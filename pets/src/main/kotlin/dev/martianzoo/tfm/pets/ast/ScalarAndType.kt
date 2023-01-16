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

data class ScalarAndType(
    val scalar: Int = 1,
    val type: TypeExpression = DEFAULT.type,
) : PetNode() {
  init { require(scalar >= 0) }

  override val kind = ScalarAndType::class.simpleName!!

  override fun toString() = toString(false, false)

  fun toString(forceScalar: Boolean = false, forceType: Boolean = false) = when {
    !forceType && type == DEFAULT.type -> "$scalar"
    !forceScalar && scalar == 1 -> "$type"
    else -> "$scalar $type"
  }

  companion object : PetParser() {
    fun sat(scalar: Int = 1, type: TypeExpression = DEFAULT.type) =
        ScalarAndType(scalar, type)

    fun parser(): Parser<ScalarAndType> {
      return parser {
        val scalarAndOptionalType =
            scalar and optional(typeExpression) map { (scalar, expr: TypeExpression?) ->
              if (expr == null) {
                sat(scalar = scalar)
              } else {
                sat(scalar, expr)
              }
            }

        val optionalScalarAndType =
            optional(scalar) and typeExpression map { (scalar, expr) ->
              if (scalar == null) {
                sat(type = expr)
              } else {
                sat(scalar, expr)
              }
            }

        scalarAndOptionalType or optionalScalarAndType
      }
    }
  }
}
