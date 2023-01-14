@file:Suppress("ObjectPropertyName")

package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.Action.Cost.Spend
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.pets.ast.FromExpression
import dev.martianzoo.tfm.pets.ast.FromExpression.ComplexFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.TypeInFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Script.ScriptCommand
import dev.martianzoo.tfm.pets.ast.Script.ScriptCounter
import dev.martianzoo.tfm.pets.ast.Script.ScriptLine
import dev.martianzoo.tfm.pets.ast.Script.ScriptPragmaPlayer
import dev.martianzoo.tfm.pets.ast.Script.ScriptRequirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.Debug
import dev.martianzoo.util.ParserGroup
import kotlin.reflect.KClass
import kotlin.reflect.cast

/** Parses the Petaform language. */
internal object ElementParsers : PetTokenizer() {
  private val pgb = ParserGroup.Builder<PetNode>()

  init { Debug.d ("start $this") }

  /**
   * Parses the PETS source code in `source` using any accessible parser
   * instance from the properties of this object; intended for testing.
   */
  internal fun <T> parse(parser: Parser<T>, source: String): T {
    val tokens = tokenize(source)
    Debug.d(tokens.filterNot { it.type.ignored }.joinToString(" ") {
      it.type.name?.replace("\n", "\\n") ?: "NULL"
    })
    return parser.parseToEnd(tokens)
  }

  internal val nls = zeroOrMore(char('\n'))

  internal object Types { // ------------------------------------------------------------
    init { Debug.d("start $this") }

    private val typeExpression: Parser<TypeExpression> = parser { whole }

    // internal val classShortName = allCapsWordRE map { ClassName(it.text) }
    val classFullName = upperCamelRE map { ClassName(it.text) }
    val className = classFullName // or classShortName -- why does that break everything?

    private val classType = className and skipChar('.') and skip(_class) map ::ClassLiteral

    private val specializations =
        optionalList(skipChar('<') and commaSeparated(typeExpression) and skipChar('>'))

    val optlRefinement = optional(parens(
        skip(_has) and parser { requirement }
    ))

    val genericType: Parser<GenericTypeExpression> =
        parser { className and specializations and optlRefinement map { (type, specs, ref) ->
          GenericTypeExpression(type, specs, ref)
        } }

    internal val whole = classType or genericType

    init { Debug.d("end $this") }
  }
  val typeExpression = pgb.publish(Types.whole)
  val genericType = pgb.publish(Types.genericType)

  internal object QEs { // --------------------------------------------------------------
    init { Debug.d("start $this") }

    internal val scalar: Parser<Int> = scalarRE map { it.text.toInt() }

    private val implicitScalar: Parser<Int?> = optional(scalar)
    private val implicitType: Parser<GenericTypeExpression?> = optional(parser { Types.genericType })

    private val qeWithScalar = scalar and implicitType map { (scalar, expr: TypeExpression?) ->
      if (expr == null) {
        QuantifiedExpression(scalar = scalar)
      } else {
        QuantifiedExpression(expr, scalar)
      }
    }
    private val qeWithType = implicitScalar and Types.genericType map { (scalar, expr) ->
      if (scalar == null) {
        QuantifiedExpression(expr)
      } else {
        QuantifiedExpression(expr, scalar)
      }
    }
    internal val whole = qeWithScalar or qeWithType

    init { Debug.d("end $this") }
  }
  val qe = pgb.publish(QEs.whole)

  internal object Requirements { // -----------------------------------------------------
    init { Debug.d("start $this") }

    private val requirement: Parser<Requirement> = parser { whole }

    private val min = qe map ::Min
    private val max = skip(_max) and qe map ::Max
    private val exact = skipChar('=') and qe map ::Exact
    private val transform = transform(requirement) map {
      (node, type) -> Requirement.Transform(node, type)
    }

    internal val atom =
        parser { min or max or exact or transform or parens(requirement) }

    private val orReq = separatedTerms(atom, _or) map {
      val set = it.toSet()
      if (set.size == 1) set.first() else Requirement.Or(set)
    }
    internal val whole = commaSeparated(orReq) map {
      if (it.size == 1) it.first() else Requirement.And(it)
    }

    init { Debug.d("end $this") }
  }
  val requirement = pgb.publish(Requirements.whole)

  internal object Instructions { // -----------------------------------------------------
    init { Debug.d ("start $this") }

    private val instruction: Parser<Instruction> = parser { whole }

    internal val intensity = optional( // TODO
        (char('!') map { Intensity.MANDATORY }) or
        (char('.') map { Intensity.AMAP }) or
        (char('?') map { Intensity.OPTIONAL })
    )

    private val gain = qe and intensity map {
      (qe, intens) -> Gain(qe, intens)
    }
    private val remove = skipChar('-') and qe and intensity map {
      (qe, intens) -> Remove(qe, intens)
    }

    private val simpleFrom = Types.genericType and skip(_from) and Types.genericType map {
      (to, from) -> SimpleFrom(to, from)
    }

    private val complexFrom =
        Types.className and
        skipChar('<') and
        parser { fromElements } and
        skipChar('>') and
        Types.optlRefinement map {
      (name, specs, refins) -> ComplexFrom(name, specs, refins)
    }
    private val from = simpleFrom or complexFrom
    private val typeInFrom = parser { Types.genericType } map { TypeInFrom(it) }

    private val fromElements: Parser<List<FromExpression>> =
        zeroOrMore(typeInFrom and skipChar(',')) and
        from and
        zeroOrMore(skipChar(',') and typeInFrom) map {
      (before, from, after) -> before + from + after
    }

    private val transmute =
        optional(QEs.scalar) and
        from and
        intensity map {
      (scal, fro, intens) -> Transmute(fro, scal, intens)
    }

    private val perable = transmute or parens(transmute) or gain or remove

    private val maybePer = perable and optional(skipChar('/') and qe) map { (instr, qe) ->
      if (qe == null) instr else Instruction.Per(instr, qe)
    }

    private val maybeTransform = maybePer or (transform(instruction) map {
      (node, type) -> Instruction.Transform(node, type)
    })

    private val arguments = separatedTerms(typeExpression, char(','), true)
    private val custom = skipChar('$') and lowerCamelRE and parens(arguments) map {
      (name, args) -> Instruction.Custom(name.text, args)
    }
    private val atom = parens(instruction) or maybeTransform or custom

    private val gated = optional(Requirements.atom and skipChar(':')) and atom map { (one, two) ->
      if (one == null) two else Gated(one, two)
    }
    private val orInstr = separatedTerms(gated, _or) map {
      val set = it.toSet()
      if (set.size == 1) set.first() else Instruction.Or(set)
    }
    private val then = separatedTerms(orInstr, _then) map {
      if (it.size == 1) it.first() else Instruction.Then(it)
    }

    internal val whole = commaSeparated(then) map {
      if (it.size == 1) it.first() else Instruction.Multi(it)
    }
    init { Debug.d ("end $this") }
  }
  internal val instruction = pgb.publish(Instructions.whole)

  internal object Actions { // ----------------------------------------------------------
    init { Debug.d("start $this") }

    private val cost: Parser<Cost> = parser { wholeCost }
    private val groupedCost = parens(cost)

    private val spend = qe map ::Spend
    private val maybeTransform = spend or (transform(cost) map  {
      (node, type) -> Cost.Transform(node, type)
    })

    private val perCost = maybeTransform and optional(skipChar('/') and qe) map { (cost, qe) ->
      if (qe == null) cost else Cost.Per(cost, qe)
    }

    private val orCost = separatedTerms(perCost or groupedCost, _or) map {
      val set = it.toSet()
      if (set.size == 1) set.first() else Cost.Or(set)
    }

    val wholeCost = commaSeparated(orCost or groupedCost) map {
      if (it.size == 1) it.first() else Cost.Multi(it)
    }

    val whole = parser {
      optional(wholeCost) and
          skip(arrow) and
          instruction map { (c, i) ->
        Action(c, i)
      }
    }
    init { Debug.d("end $this") }
  }
  internal val cost = pgb.publish(Actions.wholeCost)
  internal val action = pgb.publish(Actions.whole)

  internal object Effects { // ----------------------------------------------------------
    init { Debug.d ("start $this") }

    private val onGain = parser { Types.genericType } map ::OnGain
    private val onRemove = skipChar('-') and Types.genericType map ::OnRemove
    private val atom = onGain or onRemove

    private val transform = transform(atom) map { (node, type) -> Trigger.Transform(node, type) }

    val trigger = transform or atom

    private val colons = doubleColon or char(':') map { it.text == "::" }

    val whole =
        trigger and
        colons and
        maybeGroup(instruction) map {
      (trig, immed, instr) -> Effect(trigger = trig, automatic = immed, instruction = instr)
    }
    init { Debug.d ("end $this") }
  }
  internal val trigger = pgb.publish(Effects.trigger)
  internal val effect = pgb.publish(Effects.whole)

  internal object Scripts { // ----------------------------------------------------------
    init { Debug.d ("start $this") }
    private val command: Parser<ScriptCommand> =
        skip(_exec) and
        instruction and
        optional(skip(_by) and
        Types.genericType) map {
      (instr, by) -> ScriptCommand(instr, by)
    }

    private val req: Parser<ScriptRequirement> =
        skip(_require) and requirement map ::ScriptRequirement

    private val counter: Parser<ScriptCounter> =
        skip(_count) and
        Types.genericType and
        skip(arrow) and
        lowerCamelRE map {
      (type, key) -> ScriptCounter(key.text, type)
    }

    private val player: Parser<ScriptPragmaPlayer> =
        skip(_become) and Types.genericType map ::ScriptPragmaPlayer

    val line: Parser<ScriptLine> =
        skip(nls) and (command or req or counter or player)

    init { Debug.d("end $this") }
  }

  val scriptLine = pgb.publish(Scripts.line)

  /** Non-reified version of `parse(source)`. */
  fun <P : PetNode> parsePets(expectedType: KClass<P>, source: String): P {
    val pet = try {
      pgb.parse(expectedType, tokenize(source))
    } catch (e: ParseException) {
      throw IllegalArgumentException("""
          Expecting ${expectedType.simpleName} ...
          Input was:
          $source
      """.trimIndent(), e)
    }
    return expectedType.cast(pet)
  }
}
