package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
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
import dev.martianzoo.tfm.pets.ComponentDef.Dependency
import dev.martianzoo.tfm.pets.ComponentDef.RawDefaults
import dev.martianzoo.tfm.pets.PetsParser.Instructions.instruction
import dev.martianzoo.tfm.pets.PetsParser.Types.typeExpression
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.Action.Cost.Spend
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.ComplexFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.FromExpression
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Instruction.TypeInFrom
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
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.util.toSetStrict
import kotlin.reflect.KClass
import kotlin.reflect.cast

/** Parses the Petaform language. */
object PetsParser {
  /**
   * Parses the PETS expression in `source`, expecting a construct of type
   * `P`, and returning the parsed `P`. `P` can only be one of the major
   * elemental types like `Action`, not `ComponentDef`, or something smaller.
   */
  inline fun <reified P : PetsNode> parse(source: String): P = parse(P::class, source)

  /**
   * Parses the PETS source code in `source` using any accessible parser
   * instance from the properties of this object; intended for testing.
   */
  fun <T> parse(parser: Parser<T>, source: String) = parser.parseToEnd(tokenizer.tokenize(source))

  /**
   * Parses an entire PETS component defs source file.
   */
  fun parseComponents(text: String): List<ComponentDef> =
      parseRepeated(Components.nestedComponentDefs, tokenizer.tokenize(text))

  fun parseScript(text: String): Script =
      Script(parseRepeated(Scripts.line map { listOf(it) }, tokenizer.tokenize(text)))

  private val primaryParsers = // internal bookkeeping
      mutableMapOf<KClass<out PetsNode>, Parser<PetsNode>>()

  private val ignored = listOf(
      // automatically skipped
      regexToken("//[^\n]*", true),
      regexToken(" +", true)
  )

  // I simply don't want to name all of these and would rather look them up by the char itself
  private val characters = "!+,-./:;=?()[]{}<>\n".map { it to literalToken("$it") }.toMap()

  private val tokenList = (ignored + characters.values).toMutableList()

  private fun word(w: String) = regex("\\b$w\\b")
  private fun regex(r: String) = regexToken(r).also { tokenList += it }

  private val _by = word("BY")
  private val _from = word("FROM")
  private val _has = word("HAS")
  private val _max = word("MAX")
  private val _or = word("OR")
  private val _prod = word("PROD")
  private val _then = word("THEN")

  // component defs
  private val _abstract = word("abstract")
  private val _class = word("class")
  private val _CLASS = word("CLASS")
  private val _default = word("default")

  // scripts
  private val _become = word("BECOME")
  private val _count = word("COUNT")
  private val _exec = word("EXEC")
  private val _require = word("REQUIRE")

  // regexes
  private val classNameRE = regex("\\b[A-Z][a-z][A-Za-z0-9_]*\\b") // must begin UPPER Lower
  private val scalarRE = regex("\\b(0|[1-9][0-9]*)\\b")   // must be all digits
  private val customRE = regex("\\$[a-z][a-zA-Z0-9]*\\b") // must begin $ then lowerCamel
  private val metricKeyRE = regex("\\b[a-z]\\w*\\b")      // must begin lower

  private val tokenizer = DefaultTokenizer(tokenList)

  private fun char(c: Char): Token = characters[c] ?: error("add $c to `characters`")

  private fun skipChar(c: Char) = skip(char(c))

  private val nls = skip(zeroOrMore(char('\n')))

  object Types { // ------------------------------------------------------------

    internal val typeExpression: Parser<TypeExpression> = publish { whole }

    internal val className = classNameRE map { it.text }

    private val specializations = optionalList(
        skipChar('<') and commaSeparated(typeExpression) and skipChar('>')
    )
    internal val refinement = optional(parens(skip(_has) and Requirements.requirement))

    internal val whole = className and specializations and refinement map { (type, refs, reqt) ->
      TypeExpression(type, refs, reqt)
    }
  }

  val xt = Types.typeExpression

  object QEs { // --------------------------------------------------------------

    internal val qe: Parser<QuantifiedExpression> = publish { whole }

    internal val scalar: Parser<Int> = scalarRE map { it.text.toInt() }

    private val implicitScalar: Parser<Int?> = optional(scalar)
    private val implicitType: Parser<TypeExpression?> = optional(Types.typeExpression)

    private val qeWithScalar = scalar and implicitType map { (scalar, expr) ->
      QuantifiedExpression(expr, scalar)
    }
    private val qeWithType = implicitScalar and Types.typeExpression map { (scalar, expr) ->
      QuantifiedExpression(expr, scalar)
    }
    private val whole = qeWithScalar or qeWithType
  }

  val xq = QEs.qe

  object Requirements { // -----------------------------------------------------

    internal val requirement: Parser<Requirement> = publish { whole }

    internal val min = QEs.qe map ::Min
    internal val max = skip(_max) and QEs.qe map ::Max
    private val exact = skipChar('=') and QEs.qe map ::Exact
    private val prod = prod(requirement) map Requirement::Prod

    internal val atom =
        min or max or exact or prod or parens(requirement)      // can have no precedence worries

    private val orReq = separatedTerms(atom, _or) map {
      val set = it.toSet()
      if (set.size == 1) set.first() else Requirement.Or(set)
    }
    private val whole = commaSeparated(orReq) map {
      if (it.size == 1) it.first() else Requirement.And(it)
    }
  }

  val xr = Requirements.requirement

  object Instructions { // -----------------------------------------------------

    val instruction: Parser<Instruction> = publish { whole }

    private val anyGroup = parens(instruction)

    internal val intensity = optional(
        (char('!') map { Intensity.MANDATORY }) or
        (char('.') map { Intensity.AMAP }) or
        (char('?') map { Intensity.OPTIONAL })
    )

    private val gain = QEs.qe and intensity map { (qe, intens) ->
      Gain(qe, intens)
    }
    private val remove = skipChar('-') and QEs.qe and intensity map { (qe, intens) ->
      Remove(qe, intens)
    }

    private val simpleFrom =
        Types.typeExpression and skip(_from) and Types.typeExpression map { (to, from) ->
          SimpleFrom(to, from)
        }

    private val complexFrom =
        Types.className and skipChar('<') and parser { fromElements } and skipChar('>') and Types.refinement map { (name, specs, refins) ->
          ComplexFrom(name, specs, refins)
        }
    private val from = simpleFrom or complexFrom
    private val typeInFrom = Types.typeExpression map { TypeInFrom(it) }

    private val fromElements: Parser<List<FromExpression>> =
        zeroOrMore(typeInFrom and skipChar(',')) and from and zeroOrMore(skipChar(',') and typeInFrom) map { (before, from, after) ->
          before + from + after
        }

    private val transmute =
        optional(QEs.scalar) and from and intensity map { (scal, fro, intens) ->
          Transmute(fro, scal, intens)
        }

    private val perable = transmute or parens(transmute) or gain or remove

    private val maybePer = perable and optional(skipChar('/') and QEs.qe) map { (instr, qe) ->
      if (qe == null) instr else Instruction.Per(instr, qe)
    }

    private val maybeProd = maybePer or (prod(instruction) map Instruction::Prod)

    val arguments = separatedTerms(Types.typeExpression, char(','), true)
    internal val custom = customRE and parens(arguments) map { (name, args) ->
      Custom(name.text.substring(1), args)
    }
    internal val atom = anyGroup or maybeProd or custom

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

    private val whole = commaSeparated(then) map {
      if (it.size == 1) it.first() else Instruction.Multi(it)
    }
  }

  val xi = Instructions.instruction

  object Actions { // ----------------------------------------------------------

    private val cost: Parser<Cost> = publish { wholeCost }
    internal val action: Parser<Action> = publish { whole }

    private val groupedCost = parens(cost)

    private val spend = QEs.qe map ::Spend
    private val maybeProd = spend or (prod(cost) map Cost::Prod)

    private val perCost = maybeProd and optional(
        skipChar('/') and QEs.qe
    ) map { (cost, qe) ->
      if (qe == null) cost else Cost.Per(cost, qe)
    }

    private val orCost = separatedTerms(perCost or groupedCost, _or) map {
      val set = it.toSet()
      if (set.size == 1) set.first() else Cost.Or(set)
    }

    private val wholeCost = commaSeparated(orCost or groupedCost) map {
      if (it.size == 1) it.first() else Cost.Multi(it)
    }

    private val whole =
        optional(wholeCost) and skipChar('-') and skipChar('>') and Instructions.instruction map { (c, i) ->
          Action(c, i)
        }
  }

  val xa = Actions.action

  object Effects { // ----------------------------------------------------------
    internal val trigger: Parser<Trigger> = publish { wholeTrigger }
    internal val effect: Parser<Effect> = publish { whole }

    private val onGain = Types.typeExpression map ::OnGain
    private val onRemove = skipChar('-') and Types.typeExpression map ::OnRemove
    private val atom = onGain or onRemove

    private val wholeTrigger = prod(atom) map Trigger::Prod or atom

    private val colons = skipChar(':') and optional(char(':')) map {
      it != null
    }
    internal val whole =
        wholeTrigger and colons and maybeGroup(Instructions.instruction) map { (trig, immed, instr) ->
          Effect(trig, instr, immed)
        }
  }

  val xe = Effects.effect

  object Scripts { // ----------------------------------------------------------
    private val command: Parser<ScriptCommand> =
        skip(_exec) and instruction and optional(skip(_by) and typeExpression) map { (instr, by) ->
          ScriptCommand(instr, by)
        }

    private val req: Parser<ScriptRequirement> =
        skip(_require) and Requirements.requirement map ::ScriptRequirement

    private val counter: Parser<ScriptCounter> =
        skip(_count) and typeExpression and skip(char('-') and char('>')) and metricKeyRE map {
          (type, key) -> ScriptCounter(key.text, type)
        }

    private val player: Parser<ScriptPragmaPlayer> =
        skip(_become) and typeExpression map ::ScriptPragmaPlayer

    internal val line: Parser<ScriptLine> =
        nls and (command or req or counter or player) and skipChar('\n')
  }

  val xs = Scripts.line

  private data class Signature(
      val className: String,
      val dependencies: List<Dependency>,
      val refinement: Requirement?,
      val supertypes: List<TypeExpression>
  )

  object Components { // -------------------------------------------------------
    private val isAbstract = optional(_abstract) and skip(_class) map { it != null }
    private val dependency = optional(_CLASS) and Types.typeExpression map { (classDep, type) ->
      Dependency(type, classDep != null)
    }
    private val dependencies = optionalList(
        skipChar('<') and commaSeparated(dependency) and skipChar('>')
    )
    private val supertypes = optionalList(skipChar(':') and commaSeparated(Types.typeExpression))

    private val signature =
        Types.className and dependencies and Types.refinement and supertypes map { (c, d, r, s) ->
          Signature(c, d, r, s)
        }

    private val moreSignatures: Parser<List<Signature>> =
        skipChar(',') and separatedTerms(signature, char(','))

    private val gainDefault =
        skipChar('+') and Types.typeExpression and Instructions.intensity map { (type, int) ->
          RawDefaults(gainDefault = oneDefault(type), gainIntensity = int)
        }

    private val typeDefault = Types.typeExpression map {
      RawDefaults(allDefault = oneDefault(it))
    }

    private val defaultStmt: Parser<RawDefaults> = skip(_default) and (gainDefault or typeDefault)

    private val bodyElement = defaultStmt or Actions.action or Effects.effect

    private val oneLineBody =
        skipChar('{') and separatedTerms(bodyElement, char(';')) and skipChar('}')

    internal val oneLineComponentDef: Parser<ComponentDef> =
        isAbstract and signature and optionalList(oneLineBody) map { (abs, sig, body) ->
          createCcd(abs, sig, body).first().getDef()
        }

    private val blockBodyContents = separatedTerms(
        parser { incompleteComponentDefs } or bodyElement,
        oneOrMore(char('\n')),
        acceptZero = true
    )
    private val blockBody: Parser<List<Any>> =
        skipChar('{') and nls and blockBodyContents and nls and skipChar('}')

    private val incompleteComponentDefs =
        nls and isAbstract and signature and (blockBody or optionalList(moreSignatures)) map { (abs, sig, bodyOrSigs) ->
          if (bodyOrSigs.firstOrNull() !is Signature) { // sigs
            createCcd(abs, sig, bodyOrSigs)
          } else { // body
            @Suppress("UNCHECKED_CAST") val signatures =
                listOf(sig) + (bodyOrSigs as List<Signature>)
            signatures.flatMap { createCcd(abs, it) }
          }
        }
    internal val nestedComponentDefs =
        incompleteComponentDefs map { defs -> defs.map { it.getDef() } }

    class ComponentDefInProcess(
        private val def: ComponentDef,
        private val isComplete: Boolean
    ) {

      fun getDef() = if (isComplete) def else fixSupertypes()

      fun fillInSuperclass(name: String) =
          if (isComplete || def.supertypes.any { it.className == name }) {
            this
          } else {
            ComponentDefInProcess(
                def.copy(supertypes = (listOf(te(name)) + def.supertypes).toSetStrict()), true
            )
          }

      private fun fixSupertypes(): ComponentDef {
        val supes = def.supertypes
        return when {
          def.className == "$COMPONENT" -> def.also { require(supes.isEmpty()) }
          supes.isEmpty() -> def.copy(supertypes = setOf(COMPONENT.type))
          else -> def.also { require(COMPONENT.type !in supes) }
        }
      }
    }

    private fun createCcd(
        abst: Boolean,
        sig: Signature,
        contents: List<Any> = listOf()
    ): List<ComponentDefInProcess> {
      val defs = contents.filterIsInstance<RawDefaults>().toSetStrict()
      val acts = contents.filterIsInstance<Action>().toSetStrict()
      val effs = contents.filterIsInstance<Effect>().toSetStrict()
      val subs = contents.filterIsInstance<List<*>>().toSetStrict()

      val mergedDefaults = RawDefaults(
          allDefault = defs.firstNotNullOfOrNull { it.allDefault },
          gainDefault = defs.firstNotNullOfOrNull { it.gainDefault },
          gainIntensity = defs.firstNotNullOfOrNull { it.gainIntensity },
      )

      val comp = ComponentDef(
          className = sig.className,
          abstract = abst,
          supertypes = sig.supertypes.toSetStrict(),
          dependencies = sig.dependencies,
          effectsRaw = { effs + actionsToEffects(acts) },
          rawDefaults = mergedDefaults
      )
      return listOf(ComponentDefInProcess(comp, false)) + subs.flatten()
          .map { (it as ComponentDefInProcess).fillInSuperclass(sig.className) }
    }
  }

  val xc = Components.oneLineComponentDef

  // PRIVATE HELPERS -----------------------------------------------------------

  private inline fun <reified T> optionalList(parser: Parser<List<T>>) =
      optional(parser) map { it ?: listOf() }

  private inline fun <reified T> prod(parser: Parser<T>) =
      skip(_prod) and skipChar('[') and parser and skipChar(']')

  private inline fun <reified P> commaSeparated(p: Parser<P>) =
      separatedTerms(p, char(','))

  private inline fun <reified T> parens(contents: Parser<T>) =
      skipChar('(') and contents and skipChar(')')

  private inline fun <reified T> maybeGroup(contents: Parser<T>) =
      contents or parens(contents)

  private inline fun <reified P : PetsNode> publish(
      noinline parser: () -> Parser<P>) = publish(P::class, parser)

  private fun <P : PetsNode> publish(type: KClass<P>, parser: () -> Parser<P>) =
      parser(parser).also { primaryParsers[type] = it }

  /** Non-reified version of `parse(source)`. */
  fun <P : PetsNode> parse(expectedType: KClass<P>, source: String): P {
    require(expectedType in primaryParsers) { expectedType }
    val parser: Parser<PetsNode> = primaryParsers[expectedType]!!
    val pet = try {
      parse(parser, source)
    } catch (e: ParseException) {
      throw IllegalArgumentException(
          """
          Expecting ${expectedType.simpleName} ...
          Input was:
          $source
      """.trimIndent(),
          e
      )
    }
    return expectedType.cast(pet)
  }
}
