package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer

/** The main part of a "transmute" instruction, without the scalar or intensity. */
public data class FromExpression(
    val toExpression: Expression,
    val fromExpression: Expression,
) : PetNode() {
  override val kind = FromExpression::class

  override fun visitChildren(visitor: Visitor) = visitor.visit(toExpression, fromExpression)

  override fun toString() = "$toExpression FROM $fromExpression"

  internal companion object : PetTokenizer() {
    fun parser(): Parser<FromExpression> {
      return parser {
        Expression.parser() and
            skip(_from) and
            Expression.parser() map
            { (toExpression, fromExpression) ->
              FromExpression(toExpression, fromExpression)
            }
      }
    }
  }
}
