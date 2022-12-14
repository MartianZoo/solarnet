package dev.martianzoo.tfm.petaform

import com.github.h0tk3y.betterParse.combinators.SkipParser
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
import com.github.h0tk3y.betterParse.parser.AlternativesFailure
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.UnexpectedEof
import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import dev.martianzoo.tfm.petaform.Action.Cost
import dev.martianzoo.tfm.petaform.Action.Cost.Spend
import dev.martianzoo.tfm.petaform.Effect.Trigger
import dev.martianzoo.tfm.petaform.Effect.Trigger.Conditional
import dev.martianzoo.tfm.petaform.Effect.Trigger.Now
import dev.martianzoo.tfm.petaform.Effect.Trigger.OnGain
import dev.martianzoo.tfm.petaform.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.petaform.Instruction.ComplexFrom
import dev.martianzoo.tfm.petaform.Instruction.Custom
import dev.martianzoo.tfm.petaform.Instruction.FromExpression
import dev.martianzoo.tfm.petaform.Instruction.Gain
import dev.martianzoo.tfm.petaform.Instruction.Gated
import dev.martianzoo.tfm.petaform.Instruction.Intensity.Companion.intensity
import dev.martianzoo.tfm.petaform.Instruction.Remove
import dev.martianzoo.tfm.petaform.Instruction.SimpleFrom
import dev.martianzoo.tfm.petaform.Instruction.Transmute
import dev.martianzoo.tfm.petaform.Instruction.TypeInFrom
import dev.martianzoo.tfm.petaform.Predicate.Exact
import dev.martianzoo.tfm.petaform.Predicate.Max
import dev.martianzoo.tfm.petaform.Predicate.Min
import kotlin.reflect.KClass
import kotlin.reflect.cast

object PetaformParser {
  inline fun <reified P : PetaformNode> parse(petaform: String) = parse(P::class, petaform)


  fun <T> parse(parser: Parser<T>, petaform: String) =
      parser.parseToEnd(tokenizer.tokenize(petaform))

  fun parseComponents(arg: String): List<Component> {
    var index = 0
    val comps = mutableListOf<Component>()
    var result: ParseResult<List<Component>>? = null
    do {
      if (result is ErrorResult) throw ParseException(result)
      result = Components.componentClump.tryParse(tokenizer.tokenize(arg), index)
      if (result is Parsed) {
        comps += result.value
        index = result.nextPosition
      }
    } while (!isEOF(result!!))
    return comps.map { it.copy(complete = true) }
  }

  fun <P : PetaformNode> parse(type: KClass<P>, petaform: String): P {
    val parser: Parser<PetaformNode> = parsers[type]!!
    try {
      val pet = parse(parser, petaform)
      if (pet.countProds() > 1) {
        throw PetaformException("Can't have multiple PROD boxes")
      }
      return type.cast(pet)
    } catch (e: ParseException) {
      throw IllegalArgumentException("expecting ${type.simpleName}, input was: $petaform", e)
    }
  }

  val parsers = mutableMapOf<KClass<out PetaformNode>, Parser<PetaformNode>>()

  val literalCache: LoadingCache<String, Token> = makeCache(::literalToken)

  val regexCache: LoadingCache<String, Token> = makeCache(::regexToken)

  val ignored = listOf(
      regexToken("//[^\n]*", true),
      regexToken(" +", true))

  val prodStart = word("PROD") and char('[')
  val prodEnd = char(']')

  val className: Parser<String> = regex("\\b[A-Z][a-z][A-Za-z0-9_]*\\b") map { it.text }

  object TypeExpressions {
    val anyTypeExpr: Parser<TypeExpression> = parser { typeExpression }
    val specializations = optionalList(skipChar('<') and commaSeparated(anyTypeExpr) and skipChar('>'))
    val refinement = optional(parens(skipWord("HAS") and parser { Predicates.predicate }))

    val typeExpression = className and specializations and refinement map {
      (type, refs, pred) -> TypeExpression(type, refs, pred)
    }
  }
  val typeExpression = publish(TypeExpressions.typeExpression)

  object QEs {
    val scalar: Parser<Int> = regex("\\b(0|[1-9][0-9]*)\\b") map { it.text.toInt() }
    val implicitScalar: Parser<Int?> = optional(scalar)
    val implicitType: Parser<TypeExpression?> = optional(TypeExpressions.anyTypeExpr)

    val qeWithScalar = scalar and implicitType map { (scalar, expr) ->
      QuantifiedExpression(expr, scalar)
    }
    val qeWithType = implicitScalar and TypeExpressions.anyTypeExpr map { (scalar, expr) ->
      QuantifiedExpression(expr, scalar)
    }
    val qe = qeWithScalar or qeWithType
  }
  val qe = publish(QEs.qe)

  object Predicates {
    val anyPredicate: Parser<Predicate> = parser { predicate }

    val min = qe map ::Min
    val max = skipWord("MAX") and QEs.qeWithScalar map ::Max
    val exact = skipChar('=') and QEs.qeWithScalar map ::Exact
    val prod = prodBox(anyPredicate) map Predicate::Prod

    // These are things that we basically can't have any precedence worries about
    val atom = min or max or exact or prod or parens(anyPredicate)

    val orPred = separatedTerms(atom, word("OR")) map Predicate::or
    val predicate = commaSeparated(orPred) map Predicate::and
  }
  val predicate = publish(Predicates.predicate)

  object Instructions {
    val anyInstr: Parser<Instruction> = parser { instruction }
    val anyGroup = parens(anyInstr)

    val intensity = optional(regex("[!.?]")) map { intensity(it?.text) }
    val gain = qe and intensity map { (qe, intens) -> Gain(qe, intens) }
    val remove = skipChar('-') and qe and intensity map { (qe, intens) -> Remove(qe, intens) }

    val simpleFrom = typeExpression and skipWord("FROM") and typeExpression map { (to, from) ->
      SimpleFrom(to, from)
    }
    val complexFrom =
        className and
        skipChar('<') and
        parser { fromElements } and
        skipChar('>') and
        TypeExpressions.refinement map {
      (name, specs, refins) -> ComplexFrom(name, specs, refins)
    }
    val from = simpleFrom or complexFrom
    val typeInFrom = typeExpression map { TypeInFrom(it) }

    // A list of one or more but where exactly one is of a certain kind
    val fromElements: Parser<List<FromExpression>> =
        zeroOrMore(typeInFrom and skipChar(',')) and
        from and
        zeroOrMore(skipChar(',') and typeInFrom) map { (a, b, c) -> a + b + c }
    val transmute = optional(QEs.scalar) and from and intensity map { (scal, fro, intens) ->
      Transmute(fro, scal, intens)
    }

    val perable = transmute or parens(transmute) or gain or remove

    val maybePer = perable and optional(skipChar('/') and QEs.qeWithType) map { (instr, qe) ->
      if (qe == null) instr else Instruction.Per(instr, qe)
    }

    val maybeProd = maybePer or (prodBox(anyInstr) map Instruction::Prod)
    val custom = regex("\\$[a-z][a-zA-Z0-9]*\\b") and parens(commaSeparated(typeExpression)) map {
      (name, args) -> Custom(name.text.substring(1), args)
    }
    val atom = anyGroup or maybeProd or custom

    val gated = optional(Predicates.atom and skipChar(':')) and atom map { (one, two) ->
      if (one == null) two else Gated(one, two)
    }
    val orInstr = separatedTerms(gated, word("OR")) map Instruction::or
    val then = separatedTerms(orInstr, word("THEN")) map Instruction::then
    val instruction = commaSeparated(then) map Instruction::multi
  }
  val instruction = publish(Instructions.instruction)

  object Actions {
    val anyCost: Parser<Cost> = parser { cost }
    val anyGroup = parens(anyCost)

    val spend = qe map ::Spend
    val maybeProd = spend or (prodBox(anyCost) map Cost::Prod)
    val perCost = maybeProd and optional(skipChar('/') and QEs.qeWithType) map { (cost, qe) ->
      if (qe == null) cost else Cost.Per(cost, qe)
    }
    val orCost = separatedTerms(perCost or anyGroup, word("OR")) map Cost::or
    val cost = publish(commaSeparated(orCost or anyGroup) map Cost::and)

    val action = publish(
        optional(cost) and skip(literal("->")) and instruction map { (c, i) ->
          Action(c, i)
        }
    )
  }
  val action = publish(Actions.action)

  object Effects {
    val onGain = typeExpression map ::OnGain
    val onRemove = skipChar('-') and typeExpression map ::OnRemove
    val atom = onGain or onRemove
    val prod = prodBox(atom) map Trigger::Prod or atom

    val now = skipWord("NOW") and predicate map ::Now
    val condit = (prod or now) and optional(skipWord("IF") and predicate) map { (a, b) ->
      if (b == null) a else Conditional(a, b)
    }
    val trigger = publish(condit or now)

    val colons = skipChar(':') and optional(char(':')) map { it != null }
    val effect = publish(
        trigger and colons and maybeGroup(instruction) map { (trig, immed, instr) ->
          Effect(trig, instr, immed)
        }
    )
  }
  val effect = publish(Effects.effect)

  object Components {
    data class Count(val min: Int, val max: Int?)
    data class Signature(val expr: TypeExpression, val sups: List<TypeExpression>)

    val nls: SkipParser = skip(zeroOrMore(char('\n')))

    val default: Parser<Instruction> = skipWord("default") and instruction

    val twoDots = literal("..")
    val upper = QEs.scalar or (char('*') map { null })

    val isAbstract: Parser<Boolean> = optional(word("abstract")) and skipWord("class") map { it != null }
    val supertypes: Parser<List<TypeExpression>> = optionalList(skipChar(':') and commaSeparated(typeExpression))
    val signature = typeExpression and supertypes map { (e, s) -> Signature(e, s) }
    val moreSignatures: Parser<List<Signature>> = skipChar(',') and separatedTerms(signature, char(','))

    val repeatableElement = parser { componentClump } or default or action or effect
    val repeatedElements = separatedTerms(repeatableElement, oneOrMore(char('\n')), acceptZero = true)
    val bodyContents = nls and repeatedElements
    val body: Parser<List<Any>> = skipChar('{') and nls and bodyContents and nls and skipChar('}')

    val componentClump = nls and isAbstract and signature and (body or optionalList(moreSignatures)) map {
      (abs, sig, bodyOrMoreSigs) ->
        // more signatures
        if (bodyOrMoreSigs.isNotEmpty() && bodyOrMoreSigs[0] is Signature) {
          @Suppress("UNCHECKED_CAST")
          val signatures = listOf(sig) + (bodyOrMoreSigs as List<Signature>)
          signatures.flatMap { createCcd(abs, it) }

        // body
        } else {
          createCcd(abs, sig, bodyOrMoreSigs)
        }
    }

    val interior = separatedTerms(default or action or effect, char(';'))
    val oneLineBody = skipChar('{') and interior and skipChar('}')
    val oneLineComponent: Parser<Component> =
        isAbstract and signature and optionalList(oneLineBody) map {
      (abs, sig, body) -> createCcd(abs, sig, body).first().copy(complete = true)
    }

    private fun createCcd(abst: Boolean, sig: Signature, contents: List<Any> = listOf()):
        List<Component> {
      val counts = contents.filterIsInstance<Count>().toSet()
      val defs = contents.filterIsInstance<Instruction>().toSet()
      val acts = contents.filterIsInstance<Action>().toSet()
      val effs = contents.filterIsInstance<Effect>().toSet()
      val subs = contents.filterIsInstance<List<Component>>().toSet()

      val count = when (counts.size) {
        0 -> Count(0, null)
        1 -> counts.first()
        else -> error("")
      }

      val comp = Component(
          sig.expr, abst, sig.sups.toSet(), acts, effs, defs, count.min, count.max, complete = false)
      return listOf(comp) + subs.flatten().map {
        if (it.supertypes.any { it.className == sig.expr.className }) { // TODO
          it.copy(complete = true)
        } else {
          it.copy(supertypes = it.supertypes + TypeExpression(sig.expr.className), complete = true)
        }
      }
    }
  }
  @Suppress("unused")
  val oneLineComponent = publish(Components.oneLineComponent)

  fun literal(l: String) = literalCache.get(l)
  fun char(c: Char) = literal("$c")
  fun regex(r: String) = regexCache.get(r)
  fun word(w: String) = regex("\\b$w\\b")

  fun skipChar(c: Char) = skip(char(c))
  fun skipWord(w: String) = skip(word(w))

  val tokenizer by lazy {
    DefaultTokenizer(
        ignored +
        literalCache.asMap().entries.sortedBy { -it.key.length }.map { it.value } +
        regexCache.asMap().values)
  }

  inline fun <reified T> optionalList(parser: Parser<List<T>>) =
      optional(parser) map { it ?: listOf() }

  inline fun <reified T> prodBox(parser: Parser<T>) =
      skip(prodStart) and parser and skip(prodEnd)

  inline fun <reified P> commaSeparated(p: Parser<P>) = separatedTerms(p, char(','))

  inline fun <reified P, reified S> separatedMultiple(p: Parser<P>, s: Parser<S>) =
      separatedTerms(p, s) and skip(s) and p map { (list, extra) -> list + extra }

  inline fun <reified T> parens(contents: Parser<T>) = skipChar('(') and contents and skipChar(')')

  inline fun <reified T> maybeGroup(contents: Parser<T>) = contents or parens(contents)

  inline fun <reified P : PetaformNode> publish(parser: Parser<P>): Parser<P> {
    parsers[P::class] = parser
    return parser
  }

  fun makeCache(thing: (String) -> Token) = CacheBuilder.newBuilder().build(CacheLoader.from(thing))

  fun isEOF(result: ParseResult<Any>): Boolean =
      when (result) {
        is UnexpectedEof -> true
        is AlternativesFailure -> result.errors.any(PetaformParser::isEOF)
        else -> false
      }
}
