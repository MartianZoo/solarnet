package dev.martianzoo.tfm.petaform.parser

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
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Action.Cost
import dev.martianzoo.tfm.petaform.api.Action.Cost.Spend
import dev.martianzoo.tfm.petaform.api.ComponentClassDeclaration
import dev.martianzoo.tfm.petaform.api.DEFAULT_EXPRESSION
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Effect.Trigger
import dev.martianzoo.tfm.petaform.api.Effect.Trigger.Conditional
import dev.martianzoo.tfm.petaform.api.Effect.Trigger.OnGain
import dev.martianzoo.tfm.petaform.api.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.Instruction.Gain
import dev.martianzoo.tfm.petaform.api.Instruction.Gated
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity.Companion.intensity
import dev.martianzoo.tfm.petaform.api.Instruction.Remove
import dev.martianzoo.tfm.petaform.api.Instruction.Transmute
import dev.martianzoo.tfm.petaform.api.PetaformException
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.api.Predicate
import dev.martianzoo.tfm.petaform.api.QuantifiedExpression
import dev.martianzoo.tfm.petaform.parser.PetaformParser.ComponentClasses.componentClump
import kotlin.reflect.KClass
import kotlin.reflect.cast

object PetaformParser {
  inline fun <reified P : PetaformNode> parse(petaform: String) = parse(P::class, petaform)

  fun <T> parse(parser: Parser<T>, petaform: String) =
      parser.parseToEnd(this.tokenizer.tokenize(petaform))

  fun parseComponentClasses(arg: String): List<ComponentClassDeclaration> {
    var index = 0
    val comps = mutableListOf<ComponentClassDeclaration>()
    var result: ParseResult<List<ComponentClassDeclaration>>? = null
    do {
      if (result is ErrorResult) throw ParseException(result)
      result = componentClump.tryParse(tokenizer.tokenize(arg), index)
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
      if (pet.countProds() > 1)
        throw PetaformException()
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

  object Expressions {
    val anyExpression: Parser<Expression> = parser { expression }
    val specializations = optionalList(skipChar('<') and commaSeparated(anyExpression) and skipChar('>'))
    val refinement = optional(parens(skipWord("HAS") and parser { Predicates.predicate }))

    val expression = className and specializations and refinement map {
      (type, refs, pred) -> Expression(type, refs, pred)
    }
  }
  val expression = publish(Expressions.expression)

  object QEs {
    val scalar: Parser<Int> = regex("\\b(0|[1-9][0-9]*)\\b") map { it.text.toInt() }
    val implicitScalar: Parser<Int> = optional(scalar) map { it ?: 1 }
    val implicitType: Parser<Expression> = optional(Expressions.anyExpression) map {
      it ?: DEFAULT_EXPRESSION
    }

    val qeWithScalar = scalar and implicitType map { (scalar, expr) ->
      QuantifiedExpression(expr, scalar)
    }
    val qeWithType = implicitScalar and Expressions.anyExpression map { (scalar, expr) ->
      QuantifiedExpression(expr, scalar)
    }
    val qe = qeWithScalar or qeWithType
  }
  val qe = QEs.qe

  object Predicates {
    val anyPredicate: Parser<Predicate> = parser { predicate }
    val anyGroup = parens(anyPredicate)

    val min = qe map Predicate::Min
    val max = skipWord("MAX") and QEs.qeWithScalar map Predicate::Max
    val exact = skipChar('=') and QEs.qeWithScalar map Predicate::Exact
    val prod = prodBox(anyPredicate) map Predicate::Prod
    val atomPredicate = min or max or exact or prod or anyGroup
    val single = separatedTerms(atomPredicate, word("OR")) map Predicate::or

    val predicate = commaSeparated(single) map Predicate::and
  }
  val predicate = publish(Predicates.predicate)

  object Instructions {
    val anyInstr: Parser<Instruction> = parser { instruction }
    val anyGroup = parens(anyInstr)

    val intensity = optional(regex("[!.?]")) map { intensity(it?.text) }
    val gain = qe and intensity map { (qe, intens) -> Gain(qe, intens) }
    val remove = skipChar('-') and qe and intensity map { (qe, intens) -> Remove(qe, intens) }
    val transmute = optional(QEs.scalar) and expression and intensity and skipWord("FROM") and expression map { (scal, to, intens, from) ->
      Transmute(to, from, scal, intens)
    }
    val perable = transmute or gain or remove

    val maybePer = perable and optional(skipChar('/') and QEs.qeWithType) map { (instr, qe) ->
      if (qe == null) instr else Instruction.Per(instr, qe)
    }

    val maybeProd = maybePer or (prodBox(anyInstr) map Instruction::Prod)

    val atomInstruction = anyGroup or maybeProd

    val gated = optional(Predicates.atomPredicate and skipChar(':')) and atomInstruction map {
      (one, two) -> if (one == null) two else Gated(one, two)
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
    val cost = commaSeparated(orCost or anyGroup) map Cost::and

    val action = publish(
        optional(cost) and skip(literal("->")) and instruction map { (c, i) ->
          Action(c, i)
        }
    )
  }
  val action = publish(Actions.action)

  object Effects {
    val onGain = expression map ::OnGain
    val onRemove = skipChar('-') and expression map ::OnRemove
    val atom = onGain or onRemove
    val prod = prodBox(atom) map Trigger::Prod
    val uncond = atom or prod
    val condit = uncond and skipWord("IF") and predicate map { (a, b) -> Conditional(a, b) }
    val trigger = publish(condit or uncond)

    val colons = skipChar(':') and optional(char(':')) map { it != null }
    val effect = publish(
        trigger and colons and maybeGroup(instruction) map { (trig, immed, instr) ->
          Effect(trig, instr, immed)
        }
    )
  }
  val effect = publish(Effects.effect)

  object ComponentClasses {
    var containing: Expression? = null

    data class Count(val min: Int, val max: Int?)
    data class Signature(val expr: Expression, val sups: List<Expression>)

    val nls: SkipParser = skip(zeroOrMore(char('\n')))

    val default: Parser<Instruction> = skipWord("default") and instruction

    val twoDots = literal("..")
    val upper = QEs.scalar or (char('*') map { null })
    val count = skipWord("count") and QEs.scalar and skip(twoDots) and upper map { (a, b) -> Count(a, b) }

    val isAbstract: Parser<Boolean> = optional(word("abstract")) and skipWord("class") map { it != null }
    val supertypes: Parser<List<Expression>> = optionalList(skipChar(':') and commaSeparated(expression))
    val signature = expression and supertypes map { (e, s) -> Signature(e, s) }
    val moreSignatures: Parser<List<Signature>> = skipChar(',') and separatedTerms(signature, char(','))

    val repeatableElement = parser { componentClump } or default or action or effect
    val repeatedElements = separatedTerms(repeatableElement, oneOrMore(char('\n')), acceptZero = true)
    val bodyContents = optional(count and nls) and repeatedElements map {
      listOfNotNull(it.t1) + it.t2
    }
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

    val interior = separatedTerms(count or default or action or effect, char(';'))
    val oneLineBody = skipChar('{') and interior and skipChar('}')
    val oneLineComponent: Parser<ComponentClassDeclaration> =
        isAbstract and signature and optionalList(oneLineBody) map {
      (abs, sig, body) -> createCcd(abs, sig, body).first().copy(complete = true)
    }

    private fun createCcd(abst: Boolean, sig: Signature, contents: List<Any> = listOf()):
        List<ComponentClassDeclaration> {
      val cnts = contents.filterIsInstance<Count>().toSet()
      val defs = contents.filterIsInstance<Instruction>().toSet()
      val acts = contents.filterIsInstance<Action>().toSet()
      val effs = contents.filterIsInstance<Effect>().toSet()
      val subs = contents.filterIsInstance<List<ComponentClassDeclaration>>().toSet()

      val count = when (cnts.size) {
        0 -> Count(0, null)
        1 -> cnts.first()
        else -> error("")
      }

      val cd = ComponentClassDeclaration(
          sig.expr, abst, sig.sups.toSet(), acts, effs, defs, count.min, count.max, complete = false)
      return listOf(cd) + subs.flatten().map {
        if (it.supertypes.any { it.className == sig.expr.className }) { // TODO
          it.copy(complete = true)
        } else {
          it.copy(supertypes = it.supertypes + Expression(sig.expr.className), complete = true)
        }
      }
    }
  }
  val oneLineComponent = publish(ComponentClasses.oneLineComponent)

  fun literal(l: String) = literalCache.get(l)
  fun char(c: Char) = literal("$c")
  fun regex(r: String) = regexCache.get(r)
  fun word(w: String) = regex("\\b$w\\b")

  fun skipChar(c: Char) = skip(char(c))
  fun skipWord(w: String) = skip(word(w))

  val tokenizer by lazy {
    // println(regexCache.asMap().keys)
    // println(literalCache.asMap().keys)
    DefaultTokenizer(ignored +
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
        is AlternativesFailure -> result.errors.any(::isEOF)
        else -> false
      }
}
