package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import dev.martianzoo.tfm.petaform.api.ByName
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.PetaformObject
import dev.martianzoo.tfm.petaform.api.Predicate
import dev.martianzoo.tfm.petaform.api.QuantifiedExpression
import dev.martianzoo.tfm.petaform.api.RootType
import dev.martianzoo.tfm.petaform.api.This
import kotlin.reflect.KClass
import kotlin.reflect.cast

object PetaformParser {
  internal inline fun <reified P : PetaformObject> Parser<P>.register() =
      this.also { register(P::class, it) }

  internal fun <P : PetaformObject> register(type: KClass<P>, parser: Parser<P>) {
    parsers[type] = parser
  }

  @Suppress("UNCHECKED_CAST")
  inline fun <reified P : PetaformObject> getParser() = getParser(P::class) as Parser<P>

  fun <P : PetaformObject> getParser(type: KClass<P>) = parsers[type]!!

  inline fun <reified P : PetaformObject> parse(petaform: String) = parse(P::class, petaform)

  fun <P : PetaformObject> parse(type: KClass<P>, petaform: String): P {
    val parser: Parser<PetaformObject> = parsers[type]!!
    try {
      val pet = parser.parseToEnd(Tokens.tokenizer.tokenize(petaform))
      return type.cast(pet)
    } catch (e: ParseException) {
      throw IllegalArgumentException("expecting ${type.simpleName}, input was: $petaform", e)
    }
  }

  private val parsers = mutableMapOf<KClass<out PetaformObject>, Parser<PetaformObject>>()
  init { registerAll() }

  fun registerAll() {

    val ctypeName: Parser<ByName> = Tokens.ident map { ByName(it.text) }
    val rootType: Parser<RootType> = Tokens.thiss map { This } or ctypeName

    val refinements: Parser<List<Expression>> =
        skip(Tokens.leftAngle) and
            separatedTerms(parser { getParser<Expression>() }, Tokens.comma) and
            skip(Tokens.rightAngle)

    val expression: Parser<Expression> = (rootType and optional(refinements) map { (type, refs) ->
      Expression(type, refs ?: listOf())
    }).register()

    // Quantified expressions -- require scalar or type or both

    val explicitScalar: Parser<Int> = Tokens.scalar map { it.text.toInt() }

    val impliedScalar: Parser<Int> = optional(explicitScalar) map { it ?: 1 }
    val impliedExpression: Parser<Expression> = optional(expression) map {
      it ?: Expression.DEFAULT
    }

    val qeWithExprRequired = impliedScalar and expression map { (scalar, expr) ->
      QuantifiedExpression(expr, scalar)
    }
    val qeWithScalarRequired = explicitScalar and impliedExpression map { (scalar, expr) ->
      QuantifiedExpression(expr, scalar)
    }
    val quantifiedExpression = (qeWithExprRequired or qeWithScalarRequired).register()

    // Predicates

    val groupedPredicate: Parser<Predicate> =
        skip(Tokens.leftParen) and
            parser { getParser<Predicate>() } and
            skip(Tokens.rightParen)

    val minPredicate = quantifiedExpression map Predicate::MinPredicate
    val maxPredicate = skip(Tokens.max) and qeWithScalarRequired map Predicate::MaxPredicate

    val atomPredicate = groupedPredicate or minPredicate or maxPredicate
    val singlePredicate = separatedTerms(atomPredicate, Tokens.or) map Predicate::or

    val predicate = (separatedTerms(singlePredicate, Tokens.comma) map Predicate::and).register()
  }
}
