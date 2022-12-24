package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import dev.martianzoo.tfm.pets.Action.Cost
import dev.martianzoo.tfm.pets.Action.Cost.Spend
import dev.martianzoo.tfm.pets.ComponentDef.Defaults
import dev.martianzoo.tfm.pets.ComponentDef.Dependency
import dev.martianzoo.tfm.pets.Effect.Trigger
import dev.martianzoo.tfm.pets.Effect.Trigger.Now
import dev.martianzoo.tfm.pets.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.pets.Instruction.ComplexFrom
import dev.martianzoo.tfm.pets.Instruction.Custom
import dev.martianzoo.tfm.pets.Instruction.FromExpression
import dev.martianzoo.tfm.pets.Instruction.Gain
import dev.martianzoo.tfm.pets.Instruction.Gated
import dev.martianzoo.tfm.pets.Instruction.Intensity.Companion.intensity
import dev.martianzoo.tfm.pets.Instruction.Remove
import dev.martianzoo.tfm.pets.Instruction.SimpleFrom
import dev.martianzoo.tfm.pets.Instruction.Transmute
import dev.martianzoo.tfm.pets.Instruction.TypeInFrom
import dev.martianzoo.tfm.pets.Requirement.Exact
import dev.martianzoo.tfm.pets.Requirement.Max
import dev.martianzoo.tfm.pets.Requirement.Min
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
  inline fun <reified P : PetsNode> parse(source: String): P =
      parse(P::class, source)

  /**
   * Parses the PETS source code in `source` using any accessible parser
   * instance from the properties of this object; intended for testing.
   */
  fun <T> parse(parser: Parser<T>, source: String) =
      parser.parseToEnd(tokenizer.tokenize(source))

  /**
   * Parses an entire PETS component defs source file.
   */
  fun parseComponents(arg: String): List<ComponentDef> =
    Components.componentFile.parseRepeated(tokenizer.tokenize(arg))

  private val parsers =                                                         // internal bookkeeping
      mutableMapOf<KClass<out PetsNode>, Parser<PetsNode>>()
  private val literalCache = makeCache(::literalToken)
  private val regexCache = makeCache(::regexToken)

  private val ignored = listOf(                                                 // automatically skipped
      regexToken("//[^\n]*", true),
      regexToken(" +", true))

  val prodStart = word("PROD") and char('[')
  val prodEnd = char(']')

  object Types { // ------------------------------------------------------------

    internal val typeExpression: Parser<TypeExpression> = publish { whole }

    internal val className =
        regex("\\b[A-Z][a-z][A-Za-z0-9]*\\b") map { it.text }

    private val specializations =
        optionalList(skipChar('<') and
        commaSeparated(typeExpression) and
        skipChar('>'))
    internal val refinement =
        optional(parens(skipWord("HAS") and
        Requirements.requirement))

    internal val whole = className and specializations and refinement map {
      (type, refs, reqt) -> TypeExpression(type, refs, reqt)
    }
  }
  val xt = Types.typeExpression

  object QEs { // --------------------------------------------------------------

    internal val qe: Parser<QuantifiedExpression> = publish { whole }

    internal val scalar: Parser<Int> =
        regex("\\b(0|[1-9][0-9]*)\\b") map { it.text.toInt() }

    private val implicitScalar: Parser<Int?> = optional(scalar)
    private val implicitType: Parser<TypeExpression?> =
        optional(Types.typeExpression)

    internal val qeWithScalar = scalar and implicitType map {
      (scalar, expr) -> QuantifiedExpression(expr, scalar)
    }
    internal val qeWithType = implicitScalar and Types.typeExpression map {
      (scalar, expr) -> QuantifiedExpression(expr, scalar)
    }
    private val whole = qeWithScalar or qeWithType
  }
  val xq = QEs.qe

  object Requirements { // -----------------------------------------------------

    internal val requirement: Parser<Requirement> = publish { whole }

    internal val min = QEs.qe map ::Min
    internal val max = skipWord("MAX") and QEs.qeWithScalar map ::Max
    private val exact = skipChar('=') and QEs.qeWithScalar map ::Exact
    private val prod = prod(requirement) map Requirement::Prod

    // These are things that we basically can't have any precedence worries about
    internal val atom = min or max or exact or prod or parens(requirement)

    private val orReqt = separatedTerms(atom, word("OR")) map Requirement::or
    private val whole = commaSeparated(orReqt) map Requirement::and
  }
  val xr = Requirements.requirement

  object Instructions { // -----------------------------------------------------

    val instruction: Parser<Instruction> = publish { whole }

    private val anyGroup = parens(instruction)

    internal val intensity = optional(regex("[!.?]")) map {
      intensity(it?.text)
    }
    private val gain = QEs.qe and intensity map {
      (qe, intens) -> Gain(qe, intens)
    }
    private val remove = skipChar('-') and QEs.qe and intensity map {
      (qe, intens) -> Remove(qe, intens)
    }

    private val simpleFrom =
        Types.typeExpression and
        skipWord("FROM") and
        Types.typeExpression map {
      (to, from) -> SimpleFrom(to, from)
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
    private val typeInFrom = Types.typeExpression map { TypeInFrom(it) }

    // A list of one or more but where exactly one is of a certain kind
    private val fromElements: Parser<List<FromExpression>> =
        zeroOrMore(typeInFrom and skipChar(',')) and
        from and
        zeroOrMore(skipChar(',') and typeInFrom) map {
      (before, from, after) -> before + from + after
    }

    private val transmute = optional(QEs.scalar) and from and intensity map {
      (scal, fro, intens) -> Transmute(fro, scal, intens)
    }

    private val perable = transmute or parens(transmute) or gain or remove

    private val maybePer =
        perable and
        optional(skipChar('/') and QEs.qeWithType) map {
      (instr, qe) -> if (qe == null) instr else Instruction.Per(instr, qe)
    }

    private val maybeProd = maybePer or (prod(instruction) map Instruction::Prod)
    internal val custom =
        regex("\\$[a-z][a-zA-Z0-9]*\\b") and
        parens(separatedTerms(Types.typeExpression, char(','), acceptZero = true)) map {
      (name, args) -> Custom(name.text.substring(1), args)
    }
    internal val atom = anyGroup or maybeProd or custom

    internal val gated =
        optional(Requirements.atom and skipChar(':')) and atom map {
      (one, two) -> if (one == null) two else Gated(one, two)
    }
    internal val orInstr =
        separatedTerms(gated, word("OR")) map Instruction::or
    internal val then =
        separatedTerms(orInstr, word("THEN")) map Instruction::then

    private val whole = commaSeparated(then) map Instruction::multi
  }
  val xi = Instructions.instruction

  object Actions { // ----------------------------------------------------------

    private val cost: Parser<Cost> = publish { wholeCost }
    internal val action: Parser<Action> = publish { whole }

    private val groupedCost = parens(cost)

    private val spend = QEs.qe map ::Spend
    private val maybeProd = spend or (prod(cost) map Cost::Prod)

    private val perCost =
        maybeProd and
        optional(skipChar('/') and
        QEs.qeWithType) map {
      (cost, qe) -> if (qe == null) cost else Cost.Per(cost, qe)
    }

    private val orCost =
        separatedTerms(perCost or groupedCost, word("OR")) map Cost::or

    private val wholeCost = commaSeparated(orCost or groupedCost) map Cost::and

    private val whole =
        optional(wholeCost) and
        skip(literal("->")) and
        Instructions.instruction map {
      (c, i) -> Action(c, i)
    }
  }
  val xa = Actions.action

  object Effects { // ----------------------------------------------------------
    internal val trigger: Parser<Trigger> = publish { wholeTrigger }
    internal val effect: Parser<Effect> = publish { whole }

    private val onGain = Types.typeExpression map ::OnGain
    private val onRemove = skipChar('-') and Types.typeExpression map ::OnRemove
    private val atom = onGain or onRemove

    private val prod = prod(atom) map Trigger::Prod or atom
    private val now = skipWord("NOW") and Requirements.requirement map ::Now

    private val wholeTrigger = prod or now

    private val colons = skipChar(':') and optional(char(':')) map {
      it != null
    }
    internal val whole =
        wholeTrigger and
        colons and
        maybeGroup(Instructions.instruction) map {
      (trig, immed, instr) -> Effect(trig, instr, immed)
    }
  }
  val xe = Effects.effect

  private data class Signature(
      val className: String,
      val dependencies: List<Dependency>,
      val refinement: Requirement?,
      val supertypes: List<TypeExpression>)

  object Components { // -------------------------------------------------------
    private val nls = -zeroOrMore(char('\n'))

    private val isAbstract = optional(word("abstract")) and skipWord("class") map { it != null }
    private val dependency =
        optional(word("CLASS")) and Types.typeExpression map { (classDep, type) ->
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
        skipChar('+') and Types.typeExpression and Instructions.intensity map { (type, intens) ->
          Defaults(gainType = type, gainIntensity = intens)
        }
    private val removeDefault =
        skipChar('-') and Types.typeExpression and Instructions.intensity map { (type, intens) ->
          Defaults(removeType = type, removeIntensity = intens)
        }

    private val typeDefault = Types.typeExpression map {
      Defaults(typeExpression = it)
    }

    private val default: Parser<Defaults> =
        skipWord("default") and (gainDefault or removeDefault or typeDefault)

    private val bodyElement = default or Actions.action or Effects.effect

    private val oneLineBodyContents = separatedTerms(bodyElement, char(';'))
    private val oneLineBody = skipChar('{') and oneLineBodyContents and skipChar('}')
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
    internal val componentFile = incompleteComponentDefs map { it.map { it.getDef() } }

    class ComponentDefInProcess(
        private val def: ComponentDef,
        private val isComplete: Boolean
    ) {

      fun getDef() = if (isComplete) def else fixSupertypes()

      fun fillInSuperclass(s: String) =
          if (isComplete || def.supertypes.any { it.className == s }) {
            this
          } else {
            val altered = def.copy(supertypes = def.supertypes + TypeExpression(s))
            ComponentDefInProcess(altered, true)
          }

      private fun fixSupertypes(): ComponentDef {
        val sups = def.supertypes
        return when {
          def.name == rootName -> {
            require(sups.isEmpty())
            def
          }

          sups.isEmpty() -> {
            def.copy(supertypes = setOf(rootEx))
          }

          else -> {
            require(rootEx !in sups)
            def
          }
        }
      }
    }

    private fun createCcd(
        abst: Boolean,
        sig: Signature,
        contents: List<Any> = listOf()
    ): List<ComponentDefInProcess> {
      val defs = contents.filterIsInstance<Defaults>().toSetStrict()
      val acts = contents.filterIsInstance<Action>().toSetStrict()
      val effs = contents.filterIsInstance<Effect>().toSetStrict()
      val subs = contents.filterIsInstance<List<ComponentDefInProcess>>().toSetStrict()

      val comp = ComponentDef(
          name = sig.className,
          abstract = abst,
          supertypes = sig.supertypes.toSetStrict(),
          dependencies = sig.dependencies,
          effs + acts.withIndex().map { (i, act) -> actionToEffect(act, i) },
          Defaults().merge(defs)
      )
      return listOf(ComponentDefInProcess(comp, false)) + subs.flatten()
          .map { it.fillInSuperclass(sig.className) }
    }
  }
  val xc = Components.oneLineComponentDef

  // PRIVATE HELPERS -----------------------------------------------------------

  private fun literal(l: String) = literalCache.get(l)
  private fun char(c: Char) = literal("$c")
  private fun regex(r: String) = regexCache.get(r)
  private fun word(w: String) = regex("\\b$w\\b")

  private fun skipChar(c: Char) = -char(c)
  private fun skipWord(w: String) = -word(w)

  private val tokenizer by lazy {
    DefaultTokenizer(
        ignored +
        literalCache.asMap().entries.sortedBy { -it.key.length }.map { it.value } +
        regexCache.asMap().values)
  }

  private inline fun <reified T> optionalList(parser: Parser<List<T>>) =
      optional(parser) map { it ?: listOf() }

  private inline fun <reified T> prod(parser: Parser<T>) =
      skipWord("PROD") and
      skipChar('[') and
      parser and
      skipChar(']')

  private inline fun <reified P> commaSeparated(p: Parser<P>) =
      separatedTerms(p, char(','))

  private inline fun <reified T> parens(contents: Parser<T>) =
      skipChar('(') and contents and skipChar(')')

  private inline fun <reified T> maybeGroup(contents: Parser<T>) =
      contents or parens(contents)

  private inline fun <reified P : PetsNode> publish(noinline parser: () -> Parser<P>) =
      publish(P::class, parser)

  private fun <P : PetsNode> publish(type: KClass<P>, parser: () -> Parser<P>) =
      parser(parser).also { parsers[type] = it }

  private fun makeCache(loader: (String) -> Token) =
      CacheBuilder.newBuilder().build(CacheLoader.from(loader))

  /** Non-reified version of `parse(source)`. */
  fun <P : PetsNode> parse(
      expectedType: KClass<P>, source: String): P {
    require(expectedType in parsers) { expectedType }
    val parser: Parser<PetsNode> = parsers[expectedType]!!
    val pet = try {
      parse(parser, source)
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
