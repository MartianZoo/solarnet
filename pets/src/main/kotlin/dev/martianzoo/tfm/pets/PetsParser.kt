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
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.utils.Tuple2
import dev.martianzoo.tfm.pets.ClassDeclarationParser.stripLineComments
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.Action.Cost.Spend
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.pets.ast.FromExpression
import dev.martianzoo.tfm.pets.ast.FromExpression.ComplexFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.TypeInFrom
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.PetsNode
import dev.martianzoo.tfm.pets.ast.QuantifiedExpression
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Script
import dev.martianzoo.tfm.pets.ast.Script.ScriptCommand
import dev.martianzoo.tfm.pets.ast.Script.ScriptCounter
import dev.martianzoo.tfm.pets.ast.Script.ScriptLine
import dev.martianzoo.tfm.pets.ast.Script.ScriptPragmaPlayer
import dev.martianzoo.tfm.pets.ast.Script.ScriptRequirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import kotlin.reflect.KClass
import kotlin.reflect.cast

/** Parses the Petaform language. */
object PetsParser {
  /**
   * Parses the PETS expression in `source`, expecting a construct of type
   * `P`, and returning the parsed `P`. `P` can only be one of the major
   * elemental types like `Action`, not `ClassDeclaration`, or something smaller.
   */
  inline fun <reified P : PetsNode> parsePets(source: String): P = parsePets(P::class, source)

  /**
   * Parses the PETS source code in `source` using any accessible parser
   * instance from the properties of this object; intended for testing.
   */
  fun <T> parsePets(parser: Parser<T>, source: String) = parser.parseToEnd(tokenizer.tokenize(source))

  fun parseScript(scriptText: String): Script {
    val scriptLines = try {
      val tokens = tokenizer.tokenize(stripLineComments(scriptText))
      parseRepeated(scriptLine map { listOf(it) }, tokens)
    } catch (e: Exception) {
      println("Script was:\n$scriptText")
      throw e
    }
    return Script(scriptLines)
  }

  private val primaryParsers = // internal bookkeeping
      mutableMapOf<KClass<out PetsNode>, Parser<PetsNode>>()

  private val tokenList = mutableListOf<Token>(
      regexToken("\\\\\n", true), // ignore these
      regexToken(" +", true)
  )

  private val arrow = literal("->")
  private val doubleColon = literal("::")

  // I simply don't want to name all of these and would rather look them up by the char itself
  private val characters = "!+,-./:;=?()[]{}<>\n".map { it to literal("$it") }.toMap()

  internal fun literal(w: String) = literalToken(w).also { tokenList += it }
  internal fun regex(r: Regex) = regexToken(r).also { tokenList += it }
  internal fun regex(r: String) = regexToken(r).also { tokenList += it }

  internal val _by = literal("BY")
  internal val _from = literal("FROM")
  internal val _has = literal("HAS")
  internal val _max = literal("MAX")
  internal val _or = literal("OR")
  internal val _then = literal("THEN")

  // class declarations
  internal val _abstract = literal("ABSTRACT")
  internal val _class = literal("CLASS")
  internal val _default = literal("DEFAULT")

  // scripts
  internal val _become = literal("BECOME")
  internal val _count = literal("COUNT")
  internal val _exec = literal("EXEC")
  internal val _require = literal("REQUIRE")

  // regexes - could leave the `Regex()` out, but it loses IDEA syntax highlighting!
  internal val classNameRE = regex(Regex("""\b[A-Z][a-z][A-Za-z0-9_]*\b"""))
  internal val scalarRE = regex(Regex("""\b(0|[1-9][0-9]*)\b"""))
  internal val customRE = regex(Regex("""\$[a-z][a-zA-Z0-9]*\b"""))
  internal val metricKeyRE = regex(Regex("""\b[a-z]\w*\b"""))
  internal val transformStartRE = regex(Regex("""\b[A-Z]+\["""))

  internal val tokenizer = DefaultTokenizer(tokenList)

  internal fun char(c: Char): Token = characters[c] ?: error("add $c to `characters`")

  internal fun skipChar(c: Char) = skip(char(c))

  internal val nls = zeroOrMore(char('\n'))

  object Types { // ------------------------------------------------------------

    private val typeExpression: Parser<TypeExpression> = parser { whole }

    internal val className = classNameRE map { it.text }

    private val classType = className and skipChar('.') and skip(_class) map ::ClassExpression

    private val specializations =
        optionalList(skipChar('<') and commaSeparated(typeExpression) and skipChar('>'))
    internal val refinement = optional(parens(skip(_has) and parser { requirement }))

    internal val genericType: Parser<GenericTypeExpression> =
        className and specializations and refinement map { (type, specs, ref) ->
          GenericTypeExpression(type, specs, ref)
        }

    internal val whole = classType or genericType
  }

  val typeExpression = publish(Types.whole)

  object QEs { // --------------------------------------------------------------
    internal val scalar: Parser<Int> = scalarRE map { it.text.toInt() }

    private val implicitScalar: Parser<Int?> = optional(scalar)
    private val implicitType: Parser<GenericTypeExpression?> = optional(Types.genericType)

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
  }

  val qe = publish(QEs.whole)

  object Requirements { // -----------------------------------------------------
    private val requirement: Parser<Requirement> = parser { whole }

    internal val min = qe map ::Min
    internal val max = skip(_max) and qe map ::Max
    private val exact = skipChar('=') and qe map ::Exact
    private val transform = transform(requirement) map {
      (node, type) -> Requirement.Transform(node, type)
    }

    internal val atom =
        min or max or exact or transform or parens(requirement)      // can have no precedence worries

    private val orReq = separatedTerms(atom, _or) map {
      val set = it.toSet()
      if (set.size == 1) set.first() else Requirement.Or(set)
    }
    internal val whole = commaSeparated(orReq) map {
      if (it.size == 1) it.first() else Requirement.And(it)
    }
  }

  val requirement = publish(Requirements.whole)

  object Instructions { // -----------------------------------------------------
    private val instruction: Parser<Instruction> = parser { whole }
    private val anyGroup = parens(instruction)

    internal val intensity = optional(
        (char('!') map { Intensity.MANDATORY }) or
        (char('.') map { Intensity.AMAP }) or
        (char('?') map { Intensity.OPTIONAL })
    )

    private val gain = qe and intensity map { (qe, intens) ->
      Gain(qe, intens)
    }
    private val remove = skipChar('-') and qe and intensity map { (qe, intens) ->
      Remove(qe, intens)
    }

    private val simpleFrom =
        Types.genericType and skip(_from) and Types.genericType map { (to, from) ->
          SimpleFrom(to, from)
        }

    private val complexFrom =
        Types.className and
        skipChar('<') and
        parser { fromElements } and
        skipChar('>') and
        Types.refinement map {
      (name, specs, refins) -> ComplexFrom(name, specs, refins)
    }
    private val from = simpleFrom or complexFrom
    private val typeInFrom = Types.genericType map { TypeInFrom(it) }

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
    internal val custom = customRE and parens(arguments) map { (name, args) ->
      Custom(name.text.substring(1), args)
    }
    internal val atom = anyGroup or maybeTransform or custom

    internal val gated = optional(Requirements.atom and skipChar(':')) and atom map { (one, two) ->
      if (one == null) two else Gated(one, two)
    }
    internal val orInstr = separatedTerms(gated, _or) map {
      val set = it.toSet()
      if (set.size == 1) set.first() else Instruction.Or(set)
    }
    internal val then = separatedTerms(orInstr, _then) map {
      if (it.size == 1) it.first() else Instruction.Then(it)
    }

    internal val whole = commaSeparated(then) map {
      if (it.size == 1) it.first() else Instruction.Multi(it)
    }
  }

  val instruction = publish(Instructions.whole)

  object Actions { // ----------------------------------------------------------
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

    internal val wholeCost = commaSeparated(orCost or groupedCost) map {
      if (it.size == 1) it.first() else Cost.Multi(it)
    }

    internal val whole =
        optional(wholeCost) and
        skip(arrow) and
        instruction map {
      (c, i) -> Action(c, i)
    }
  }

  val cost = publish(Actions.wholeCost)
  val action = publish(Actions.whole)

  object Effects { // ----------------------------------------------------------
    private val onGain = Types.genericType map ::OnGain
    private val onRemove = skipChar('-') and Types.genericType map ::OnRemove
    private val atom = onGain or onRemove

    internal val transform = transform(atom) map { (node, type) -> Trigger.Transform(node, type) }
    internal val wholeTrigger = transform or atom

    private val colons = doubleColon or char(':') map { it.text == "::" }

    internal val whole =
        wholeTrigger and
        colons and
        maybeGroup(instruction) map {
      (trig, immed, instr) -> Effect(trigger = trig, automatic = immed, instruction = instr)
    }
  }

  val trigger = publish(Effects.wholeTrigger)
  val effect = publish(Effects.whole)

  object Scripts { // ----------------------------------------------------------
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
        metricKeyRE map {
      (type, key) -> ScriptCounter(key.text, type)
    }

    private val player: Parser<ScriptPragmaPlayer> =
        skip(_become) and Types.genericType map ::ScriptPragmaPlayer

    internal val line: Parser<ScriptLine> =
        skip(nls) and (command or req or counter or player)
  }

  val scriptLine = Scripts.line

  // PRIVATE HELPERS -----------------------------------------------------------

  internal inline fun <reified T> optionalList(parser: Parser<List<T>>) =
      optional(parser) map { it ?: listOf() }

  private inline fun <reified T> transform(interior: Parser<T>) =
      transformStartRE and interior and skipChar(']') map {
        (trans, inter) -> Tuple2(inter, trans.text.removeSuffix("["))
      }

  internal inline fun <reified P> commaSeparated(p: Parser<P>) = separatedTerms(p, char(','))

  private inline fun <reified T> parens(contents: Parser<T>) =
      skipChar('(') and contents and skipChar(')')

  private inline fun <reified T> maybeGroup(contents: Parser<T>) = contents or parens(contents)

  private inline fun <reified P : PetsNode> publish(parser: Parser<P>) = publish(P::class, parser)

  private fun <P : PetsNode> publish(type: KClass<P>, parser: Parser<P>) =
      parser.also { primaryParsers[type] = it }

  /** Non-reified version of `parse(source)`. */
  fun <P : PetsNode> parsePets(expectedType: KClass<P>, source: String): P {
    require(expectedType in primaryParsers) { expectedType }
    val parser: Parser<PetsNode> = primaryParsers[expectedType]!!
    val pet = try {
      parsePets(parser, source)
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
