package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.petaform.api.*
import dev.martianzoo.tfm.petaform.api.Predicate.MaxPredicate
import dev.martianzoo.tfm.petaform.api.Predicate.MinPredicate
import dev.martianzoo.tfm.petaform.parser.ExpressionGrammar.expression
import dev.martianzoo.tfm.petaform.parser.Tokens.comma
import dev.martianzoo.tfm.petaform.parser.Tokens.ident
import dev.martianzoo.tfm.petaform.parser.Tokens.leftAngle
import dev.martianzoo.tfm.petaform.parser.Tokens.leftParen
import dev.martianzoo.tfm.petaform.parser.Tokens.max
import dev.martianzoo.tfm.petaform.parser.Tokens.or
import dev.martianzoo.tfm.petaform.parser.Tokens.rightAngle
import dev.martianzoo.tfm.petaform.parser.Tokens.rightParen
import dev.martianzoo.tfm.petaform.parser.Tokens.scalar
import dev.martianzoo.tfm.petaform.parser.Tokens.thiss

object PredicateGrammar {
  // Quantified expressions -- require scalar or type or both
  val explicitScalar: Parser<Int> = scalar map { it.text.toInt() }

  val impliedScalar: Parser<Int> =
      optional(explicitScalar) map { it ?: 1 }
  val impliedExpression: Parser<Expression> =
      optional(expression) map { it ?: Expression.DEFAULT }

  val qeWithExprRequired = impliedScalar and expression
  val qeWithScalarRequired = explicitScalar and impliedExpression
  val quantifiedExpression = qeWithExprRequired or qeWithScalarRequired

  // Predicates
  val groupedPredicate: Parser<Predicate> =
      skip(leftParen) and parser { predicate } and skip(rightParen)

  val minPredicate = quantifiedExpression map {
    (scalar, expr) -> MinPredicate(expr, scalar)
  }
  val maxPredicate = skip(max) and qeWithScalarRequired map {
    (scalar, expr) -> MaxPredicate(expr, scalar)
  }
  val atomPredicate = groupedPredicate or minPredicate or maxPredicate
  val singlePredicate = separatedTerms(atomPredicate, or) map Predicate::or
  val predicate: Parser<Predicate> =
      separatedTerms(singlePredicate, comma) map Predicate::and
}
