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
import dev.martianzoo.tfm.petaform.api.Instruction.Gated
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity
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
  private val twoColons = literal("::")

  private val comma = literal(",")
  private val minus = literal("-")
  private val slash = literal("/")
  private val colon = literal(":")
  private val bang = literal("!")
  private val dot = literal(".")
  private val questy = literal("?")
  private val leftParen = literal("(")
  private val rightParen = literal(")")
  private val leftAngle = literal("<")
  private val rightAngle = literal(">")
  private val leftBracket = literal("[")
  private val rightBracket = literal("]")

  private val prod = literal("PROD")
  private val max = literal("MAX")
  private val or = literal("OR")
  private val has = literal("HAS")
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

    private val hazzer = skip(has) and parser { predicate }
    private val predicates: Parser<List<Predicate>> = skip(leftParen) and separatedTerms(hazzer, comma) and skip(rightParen)
    private val optionalPredicates = optional(predicates) map { it ?: listOf() }

    // CityTile<Player1, LandArea>(HAS blahblah)
    val expression = rootType and optionalRefinements and optionalPredicates map {
      (type, refs, preds) -> Expression(type, refs, preds)
    }
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

  val predicates = PredicateParsers().also {
    publish(it.predicate)
  }
  class PredicateParsers {
    private val minPredicate = quantifiedExpression map Predicate::Min
    private val maxPredicate = skip(max) and qeWithExplicitScalar map Predicate::Max
    private val prodPredicate = prodBox(parser { predicate }) map Predicate::Prod
    val onePredicate: Parser<Predicate> = minPredicate or maxPredicate or prodPredicate

    // OR binds more tightly than ,
    private val bareOrPredicate = separatedMultiple(onePredicate or parser { groupedAndPredicate }, or) map Predicate::Or
    private val bareAndPredicate: Parser<Predicate.And> = separatedMultiple(bareOrPredicate or onePredicate, comma) map Predicate::And

    val groupedAndPredicate: Parser<Predicate.And> = skip(leftParen) and bareAndPredicate and skip(rightParen)
    val groupedOrPredicate: Parser<Predicate.Or> = skip(leftParen) and bareOrPredicate and skip(rightParen)
    val predicate: Parser<Predicate> = bareAndPredicate or bareOrPredicate or onePredicate
    val safePredicate = groupedAndPredicate or groupedOrPredicate or onePredicate
  }
  val predicate = predicates.predicate

  private val instruction = publish(
      object {
        private val intensity = optional(bang or dot or questy) map {
          it?.let { Intensity.forSymbol(it.text) }
        }

        private val gainInstruction = quantifiedExpression and intensity map {
          (qe, intens) -> Gain(qe, intens)
        }
        private val removeInstruction = skip(minus) and quantifiedExpression and intensity map {
          (qe, intens) -> Remove(qe, intens)
        }
        private val prodInstruction: Parser<Instruction.Prod> = prodBox(parser { instruction }) map Instruction::Prod
        private val perableInstruction = gainInstruction or removeInstruction or prodInstruction

        // for now, can't contain an Or/Multi; will have to be distributed
        private val perInstruction = perableInstruction and skip(slash) and qeWithExplicitExpression map {
          (instr, qe) -> Instruction.Per(instr, qe)
        }

        private val bareGatedInstruction = predicates.safePredicate and skip(colon) and parser { safeInstruction } map {
          (pred, instr) -> Gated(pred, instr)
        }
        private val groupedGatedInstruction = group(bareGatedInstruction)
        private val oneInstruction: Parser<Instruction> = perInstruction or perableInstruction or groupedGatedInstruction

        // OR binds more tightly than ,
        private val orInstruction = separatedMultiple(oneInstruction or parser { groupedMultiInstruction }, or) map Instruction::Or
        private val multiInstruction: Parser<Instruction.Multi> =
            separatedMultiple(orInstruction or oneInstruction, comma) map Instruction::Multi
        private val groupedMultiInstruction = skip(leftParen) and multiInstruction and skip(rightParen)
        private val groupedOrInstruction: Parser<Instruction> = skip(leftParen) and orInstruction and skip(rightParen)
        private val safeInstruction = groupedMultiInstruction or groupedOrInstruction or oneInstruction

        val instruction = multiInstruction or orInstruction or bareGatedInstruction or perInstruction or perableInstruction
      }.instruction,
  )

  val action = publish(object {
    private val spendCost = quantifiedExpression map ::Spend
    private val prodCost: Parser<Cost.Prod> = prodBox(parser { cost }) map Cost::Prod
    private val perableCost = spendCost or prodCost

    // for now, a per can't contain an Or/Multi; will have to be distributed
    private val perCost = perableCost and skip(slash) and qeWithExplicitExpression map {
      (cost, qe) -> Cost.Per(cost, qe)
    }
    private val oneCost = perCost or perableCost

    // OR binds more tightly than ,
    private val orCost = separatedMultiple(oneCost or parser { groupedMultiCost }, or) map Cost::Or
    private val multiCost: Parser<Cost.Multi> = separatedMultiple(orCost or oneCost, comma) map Cost::Multi
    private val groupedMultiCost = skip(leftParen) and multiCost and skip(rightParen)

    private val cost = multiCost or orCost or oneCost

    val action = publish(optional(cost) and skip(arrow) and instruction map { (c, i) -> Action(c, i) })
  }.action)

  val effect = publish(object {
    private val onGainTrigger = expression map { OnGain(it) }
    private val onRemoveTrigger = skip(minus) and expression map { OnRemove(it) }
    private val nonProdTrigger = onGainTrigger or onRemoveTrigger
    private val prodTrigger = prodBox(nonProdTrigger) map Trigger::Prod
    private val trigger = publish(nonProdTrigger or prodTrigger)

    private val colons = (twoColons map { true }) or (colon map { false })
    val effect = publish(trigger and colons and instruction map {
      (trig, immed, instr) -> Effect(trig, instr, immed)
    })
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

  private inline fun <reified T> group(contents: Parser<T>) = skip(leftParen) and contents and skip(rightParen)

  private inline fun <reified P : PetaformObject> publish(parser: Parser<P>): Parser<P> {
    parsers[P::class] = parser
    return parser
  }
}
