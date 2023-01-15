package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.SpecialClassNames.DEFAULT
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.typeExpression

data class QuantifiedExpression(
    val expression: TypeExpression = DEFAULT.type,
    val scalar: Int = 1,
) : PetNode() {
  init {
    require(scalar >= 0)
  }

  override val kind = QuantifiedExpression::class.simpleName!!

  override fun toString() = toString(false, false)

  fun toString(forceScalar: Boolean = false, forceType: Boolean = false) = when {
    !forceType && expression == DEFAULT.type -> "$scalar"
    !forceScalar && scalar == 1 -> "$expression"
    else -> "$scalar $expression"
  }

  companion object : PetParser() {
    fun parser(): Parser<QuantifiedExpression> {
      return parser {
        val scalar: Parser<Int> = _scalarRE map { it.text.toInt() }
        val implicitScalar: Parser<Int?> = optional(scalar)
        val implicitType: Parser<TypeExpression?> = optional(typeExpression)

        val qeWithScalar = scalar and implicitType map { (scalar, expr: TypeExpression?) ->
          if (expr == null) {
            QuantifiedExpression(scalar = scalar)
          } else {
            QuantifiedExpression(expr, scalar)
          }
        }
        val qeWithType = implicitScalar and typeExpression map { (scalar, expr) ->
          if (scalar == null) {
            QuantifiedExpression(expr)
          } else {
            QuantifiedExpression(expr, scalar)
          }
        }
        qeWithScalar or qeWithType
      }
    }
  }
}
