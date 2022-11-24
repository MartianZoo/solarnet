package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.ClassName
import dev.martianzoo.tfm.petaform.api.Cost
import dev.martianzoo.tfm.petaform.api.Cost.Spend
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.Instruction.Gain
import dev.martianzoo.tfm.petaform.api.Instruction.Multi
import dev.martianzoo.tfm.petaform.api.Instruction.Per
import dev.martianzoo.tfm.petaform.api.Instruction.Remove
import dev.martianzoo.tfm.petaform.api.PetaformObject
import dev.martianzoo.tfm.petaform.api.Predicate
import dev.martianzoo.tfm.petaform.api.QuantifiedExpression
import dev.martianzoo.tfm.petaform.api.RootType
import dev.martianzoo.tfm.petaform.api.This
import dev.martianzoo.tfm.petaform.api.Trigger
import dev.martianzoo.tfm.petaform.api.Trigger.OnGain
import dev.martianzoo.tfm.petaform.api.Trigger.OnRemove
import kotlin.reflect.KClass
import kotlin.reflect.cast

object PetaformParser {
  @Suppress("UNCHECKED_CAST")
  inline fun <reified P : PetaformObject> getParser() = getParser(P::class) as Parser<P>

  inline fun <reified P : PetaformObject> parse(petaform: String) = parse(P::class, petaform)

  fun <P : PetaformObject> getParser(type: KClass<P>) = parsers[type]!!

  fun <P : PetaformObject> parse(type: KClass<P>, petaform: String): P {
    val parser: Parser<PetaformObject> = parsers[type]!!
    try {
      val pet = parser.parseToEnd(DefaultTokenizer(tokens).tokenize(petaform))
      return type.cast(pet)
    } catch (e: ParseException) {
      throw IllegalArgumentException("expecting ${type.simpleName}, input was: $petaform", e)
    }
  }

  private val tokens = mutableListOf<Token>()

  private val comment = regex("//[^\n]*", ignore = true)
  private val whitespace = regex("\\s+", ignore = true)
  private val arrow = literal("->")
  private val comma = literal(",")
  private val minus = literal("-")
  private val slash = literal("/")
  private val colon = literal(":")
  private val twoColons = literal("::")
  private val leftParen = literal("(")
  private val rightParen = literal(")")
  private val leftAngle = literal("<")
  private val rightAngle = literal(">")
  private val leftBracket = literal("[")
  private val rightBracket = literal("]")
  private val prod = literal("PROD")
  private val max = literal("MAX")
  private val or = literal("OR")
  private val `this` = literal("This")
  private val scalar = regex("0|[1-9][0-9]*")
  private val ident = regex("[A-Z][a-z][A-Za-z0-9_]*")

  private val parsers = mutableMapOf<KClass<out PetaformObject>, Parser<PetaformObject>>()

  val prodStart = prod and leftBracket
  val prodEnd = rightBracket

  val expression = publish(object {
    private val className: Parser<ClassName> = ident map { ClassName(it.text) } // Plant
    private val thisComponent: Parser<This> = `this` map { This } // This
    private val rootType: Parser<RootType> = thisComponent or className // Plant
    private val refinements: Parser<List<Expression>> = // <Player1, LandArea>
        skip(leftAngle) and separatedTerms(parser { expression }, comma) and skip(rightAngle)
    private val optionalRefinements = optional(refinements) map { it ?: listOf() }
    // CityTile<Player1, LandArea>
    val expression = rootType and optionalRefinements map { (type, refs) -> Expression(type, refs) }
  }.expression)

  private val explicitScalar: Parser<Int> = scalar map { it.text.toInt() } // 3
  private val implicitScalar: Parser<Int> = optional(explicitScalar) map { it ?: 1 } // "", meaning 1
  private val implicitExpression: Parser<Expression> = optional(expression) map { // "", meaning "Megacredit"
    it ?: Expression.DEFAULT
  }
  // "1" or "1 Plant"
  val qeWithExplicitScalar = explicitScalar and implicitExpression map { (scalar, expr) ->
    QuantifiedExpression(expr, scalar)
  }
  // "Plant" or "1 Plant"
  val qeWithExplicitExpression = implicitScalar and expression map { (scalar, expr) ->
    QuantifiedExpression(expr, scalar)
  }
  // "1" or "Plant" or "1 Plant" -- if both rules would match it doesn't matter which
  val quantifiedExpression = qeWithExplicitScalar or qeWithExplicitExpression

  private val predicate = publish(object {
    private val minPredicate = quantifiedExpression map Predicate::Min
    private val maxPredicate = skip(max) and qeWithExplicitScalar map Predicate::Max
    private val prodPredicate = prodBox(parser { predicate }) map Predicate::Prod
    private val onePredicate: Parser<Predicate> = minPredicate or maxPredicate or prodPredicate

    // OR binds more tightly than ,
    private val orPredicate = separatedMultiple(onePredicate or parser { groupedAndPredicate }, or) map Predicate::Or
    private val andPredicate: Parser<Predicate.And> = separatedMultiple(orPredicate or onePredicate, comma) map Predicate::And

    private val groupedAndPredicate = skip(leftParen) and andPredicate and skip(rightParen)

    val predicate = andPredicate or orPredicate or onePredicate
  }.predicate)

  private val instruction = publish(object {
    private val gainInstruction = quantifiedExpression map ::Gain
    private val removeInstruction = skip(minus) and quantifiedExpression map ::Remove
    private val prodInstruction: Parser<Instruction.Prod> = prodBox(parser { instruction }) map Instruction::Prod
    private val perableInstruction = gainInstruction or removeInstruction or prodInstruction

    // for now, can't contain an Or/Multi; will have to be distributed
    private val perInstruction = perableInstruction and skip(slash) and qeWithExplicitExpression map { (instr, qe) ->
      Per(instr, qe)
    }
    private val oneInstruction = perInstruction or perableInstruction

    // OR binds more tightly than ,
    private val orInstruction = separatedMultiple(oneInstruction or parser { groupedMultiInstruction }, or) map Instruction::Or
    private val multiInstruction: Parser<Multi> = separatedMultiple(orInstruction or oneInstruction, comma) map ::Multi
    private val groupedMultiInstruction = skip(leftParen) and multiInstruction and skip(rightParen)

    val instruction = multiInstruction or orInstruction or oneInstruction
  }.instruction)

  // Actions - fix this
  val action = publish(object {
    private val groupedCost = skip(leftParen) and parser { cost } and skip(rightParen)
    private val spendCost = quantifiedExpression map ::Spend
    private val prodCost = prodBox(parser { cost }) map Cost::Prod
    private val atomCost = groupedCost or spendCost or prodCost
    private val singleCost = separatedTerms(atomCost, or) map Cost::or
    private val cost: Parser<Cost> = separatedTerms(singleCost, comma) map Cost::and

    val action = publish(optional(cost) and skip(arrow) and instruction map { (c, i) -> Action(c, i) })
  }.action)

  val effect = publish(object {
    private val onGainTrigger = expression map { OnGain(it) }
    private val onRemoveTrigger = skip(minus) and expression map { OnRemove(it) }
    private val nonProdTrigger = onGainTrigger or onRemoveTrigger
    private val prodTrigger = prodBox(nonProdTrigger) map Trigger::Prod
    private val trigger = publish(nonProdTrigger or prodTrigger)

    private val colons = colon map { false } or twoColons map { true }
    val effect = publish(trigger and colons and instruction map { (a, b, c) -> Effect(a, c, b) })
  }.effect)

  private fun regex(r: String, ignore: Boolean = false) = regexToken(r, ignore).also { tokens += it }
  private fun literal(l: String, ignore: Boolean = false) = literalToken(l, ignore).also { tokens += it }

  private inline fun <reified T> prodBox(parser: Parser<T>): Parser<T> {
    return skip(prodStart) and parser and skip(prodEnd)
  }

  private inline fun <reified T> separatedMultiple(term: Parser<T>, sep: Token): Parser<List<T>> {
    return term and skip(sep) and separatedTerms(term, sep) map { (first, rest) ->
      listOf(first) + rest
    }
  }

  private inline fun <reified P : PetaformObject> publish(parser: Parser<P>): Parser<P> {
    parsers[P::class] = parser
    return parser
  }
}
