package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.Token
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
import dev.martianzoo.tfm.petaform.parser.Tokens.`this`
import dev.martianzoo.tfm.petaform.parser.Tokens.arrow
import dev.martianzoo.tfm.petaform.parser.Tokens.colon
import dev.martianzoo.tfm.petaform.parser.Tokens.comma
import dev.martianzoo.tfm.petaform.parser.Tokens.ident
import dev.martianzoo.tfm.petaform.parser.Tokens.leftAngle
import dev.martianzoo.tfm.petaform.parser.Tokens.leftBracket
import dev.martianzoo.tfm.petaform.parser.Tokens.leftParen
import dev.martianzoo.tfm.petaform.parser.Tokens.max
import dev.martianzoo.tfm.petaform.parser.Tokens.minus
import dev.martianzoo.tfm.petaform.parser.Tokens.or
import dev.martianzoo.tfm.petaform.parser.Tokens.prod
import dev.martianzoo.tfm.petaform.parser.Tokens.rightAngle
import dev.martianzoo.tfm.petaform.parser.Tokens.rightBracket
import dev.martianzoo.tfm.petaform.parser.Tokens.rightParen
import dev.martianzoo.tfm.petaform.parser.Tokens.scalar
import dev.martianzoo.tfm.petaform.parser.Tokens.slash
import dev.martianzoo.tfm.petaform.parser.Tokens.twoColons
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
      val pet = parser.parseToEnd(Tokens.tokenizer.tokenize(petaform))
      return type.cast(pet)
    } catch (e: ParseException) {
      throw IllegalArgumentException("expecting ${type.simpleName}, input was: $petaform", e)
    }
  }

  private lateinit var expression: Parser<Expression>
  private lateinit var quantifiedExpression: Parser<QuantifiedExpression>
  private lateinit var predicate: Parser<Predicate>
  private lateinit var instruction: Parser<Instruction>
  private lateinit var trigger: Parser<Trigger>
  private lateinit var effect: Parser<Effect>
  private lateinit var action: Parser<Action>

  // sigh ... TODO
  private lateinit var groupedAndPredicate: Parser<Predicate>
  private lateinit var groupedMultiInstruction: Parser<Instruction>

  private val parsers = mutableMapOf<KClass<out PetaformObject>, Parser<PetaformObject>>()

  init {
    setUpAllParsers()
    register(expression)
    register(quantifiedExpression)
    register(predicate)
    register(instruction)
    register(trigger)
    register(effect)
    register(action)
  }

  fun setUpAllParsers() {

    // Expressions (no precedence issues), no need to deal with PROD

    val className: Parser<ClassName> = ident map { ClassName(it.text) } // Plant
    val thisComponent: Parser<This> = `this` map { This } // This
    val rootType: Parser<RootType> = thisComponent or className // Plant

    val refinements: Parser<List<Expression>> = // <Player1, LandArea>
        skip(leftAngle) and separatedTerms(parser { getParser<Expression>() }, comma) and skip(rightAngle)
    val optionalRefinements = optional(refinements) map { it ?: listOf() }

    // CityTile<Player1, LandArea>
    expression = rootType and optionalRefinements map { (type, refs) -> Expression(type, refs) }

    // Quantified expressions -- require scalar or type or both
    // No precedence issues, no need to deal with PROD

    val scalar: Parser<Int> = scalar map { it.text.toInt() } // 3
    val implicitScalar: Parser<Int> = optional(scalar) map { it ?: 1 } // "", meaning 1
    val implicitExpression: Parser<Expression> = optional(expression) map { // "", meaning "Megacredit"
      it ?: Expression.DEFAULT
    }

    // "1" or "1 Plant"
    val qeWithExplicitScalar = scalar and implicitExpression map {
      (scalar, expr) -> QuantifiedExpression(expr, scalar)
    }
    // "Plant" or "1 Plant"
    val qeWithExplicitExpression = implicitScalar and expression map {
      (scalar, expr) -> QuantifiedExpression(expr, scalar)
    }
    // "1" or "Plant" or "1 Plant" -- if both rules would match it doesn't matter which
    quantifiedExpression = qeWithExplicitScalar or qeWithExplicitExpression

    // Predicates

    val minPredicate = quantifiedExpression map Predicate::Min
    val maxPredicate = skip(max) and qeWithExplicitScalar map Predicate::Max
    val prodPredicate = prodBox(parser { predicate }) map Predicate::Prod
    val onePredicate = minPredicate or maxPredicate or prodPredicate

    // OR binds more tightly than ,
    val orPredicate = separatedMultiple(onePredicate or parser { groupedAndPredicate }, or) map Predicate::Or
    val andPredicate = separatedMultiple(orPredicate or onePredicate, comma) map Predicate::And

    predicate = andPredicate or orPredicate or onePredicate
    groupedAndPredicate = skip(leftParen) and andPredicate and skip(rightParen)

    // Instructions

    val gainInstruction = quantifiedExpression map ::Gain
    val removeInstruction = skip(minus) and quantifiedExpression map ::Remove

    val prodInstruction = prodBox(parser { instruction }) map Instruction::Prod
    val perableInstruction = gainInstruction or removeInstruction or prodInstruction

    // for now, can't contain an Or/Multi; will have to be distributed
    val perInstruction = perableInstruction and skip(slash) and qeWithExplicitExpression map {
      (instr, qe) -> Per(instr, qe)
    }
    val oneInstruction = perInstruction or perableInstruction

    // OR binds more tightly than ,
    val orInstruction = separatedMultiple(oneInstruction or parser { groupedMultiInstruction }, or) map Instruction::Or
    val multiInstruction = separatedMultiple(orInstruction or oneInstruction, comma) map ::Multi

    instruction = multiInstruction or orInstruction or oneInstruction
    groupedMultiInstruction = skip(leftParen) and multiInstruction and skip(rightParen)

    // Actions - fix this

    val groupedCost = skip(leftParen) and parser { getParser<Cost>() } and skip(rightParen)
    val spendCost = quantifiedExpression map ::Spend
    val prodCost = prodBox(parser { getParser<Cost>() }) map Cost::Prod
    val atomCost = groupedCost or spendCost or prodCost
    val singleCost = separatedTerms(atomCost, or) map Cost::or
    val cost = separatedTerms(singleCost, comma) map Cost::and

    action = optional(cost) and skip(arrow) and instruction map { (c, i) -> Action(c, i) }

    // Triggers
    val onGainTrigger = expression map { OnGain(it) }
    val onRemoveTrigger = skip(minus) and expression map { OnRemove(it) }
    val nonProdTrigger = onGainTrigger or onRemoveTrigger
    val prodTrigger = prodBox(nonProdTrigger) map Trigger::Prod
    trigger = nonProdTrigger or prodTrigger

    // Effects
    val colons = colon map { false } or twoColons map { true }
    effect = trigger and colons and instruction map { (a, b, c) -> Effect(a, c, b) }
  }

  private inline fun <reified T> prodBox(parser: Parser<T>): Parser<T> {
    val prodStart = prod and leftBracket
    val prodEnd = rightBracket
    return skip(prodStart) and parser and skip(prodEnd)
  }

  private inline fun <reified T> separatedMultiple(term: Parser<T>, sep: Token): Parser<List<T>> {
    return term and skip(sep) and separatedTerms(term, sep) map { (first, rest) ->
      listOf(first) + rest
    }
  }

  private inline fun <reified P : PetaformObject> register(parser: Parser<P>) {
    parsers[P::class] = parser
  }
}
