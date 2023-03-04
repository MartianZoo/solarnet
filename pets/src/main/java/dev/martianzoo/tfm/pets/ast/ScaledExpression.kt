package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.api.SpecialClassNames.MEGACREDIT
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetParser

data class ScaledExpression(
    val scalar: Int = 1,
    val expression: Expression = MEGACREDIT.expr,
) : PetNode() {
  override fun visitChildren(visitor: Visitor) = visitor.visit(expression)

  override fun toString() = toString(forceScalar = false, forceExpression = false)
  fun toFullString() = toString(forceScalar = true, forceExpression = true)

  fun toString(forceScalar: Boolean = false, forceExpression: Boolean = false) =
      when {
        !forceExpression && expression == MEGACREDIT.expr -> "$scalar"
        !forceScalar && scalar == 1 -> "$expression"
        else -> "$scalar $expression"
      }

  init {
    require(scalar >= 0)
  }

  override val kind = ScaledExpression::class.simpleName!!

  companion object : PetParser() {
    fun scaledEx(scalar: Int? = null, expression: Expression? = null) =
        ScaledExpression(scalar ?: 1, expression ?: MEGACREDIT.expr)

    fun scaledEx(expression: Expression) = ScaledExpression(1, expression)

    fun scaledEx(text: String) = Parsing.parse(parser(), text)

    fun parser(): Parser<ScaledExpression> {
      return parser {
        val scalarAndOptionalEx = scalar and optional(Expression.parser())
        val optionalScalarAndEx = optional(scalar) and Expression.parser()

        scalarAndOptionalEx or optionalScalarAndEx map {
          (scalar, expr) -> scaledEx(scalar, expr)
        }
      }
    }
  }
}
