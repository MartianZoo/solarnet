package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.SpecialClassNames.MEGACREDIT
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.typeExpression

data class ScalarAndType(
    val scalar: Int = 1,
    val type: TypeExpression = MEGACREDIT.type,
) : PetNode() {
  init {
    require(scalar >= 0)
  }

  override val kind = ScalarAndType::class.simpleName!!

  override fun toString() = toString(false, false)

  fun toString(forceScalar: Boolean = false, forceType: Boolean = false) =
      when {
        !forceType && type == MEGACREDIT.type -> "$scalar"
        !forceScalar && scalar == 1 -> "$type"
        else -> "$scalar $type"
      }

  companion object : PetParser() {
    fun sat(scalar: Int? = null, type: TypeExpression? = null) = ScalarAndType(scalar ?: 1, type ?: MEGACREDIT.type)

    fun sat(text: String) = Parsing.parse(parser(), text)

    fun parser(): Parser<ScalarAndType> {
      return parser {
        val scalarAndOptionalType = scalar and optional(typeExpression)
        val optionalScalarAndType = optional(scalar) and typeExpression

        scalarAndOptionalType or optionalScalarAndType map { (scalar, expr) ->
          sat(scalar, expr)
        }
      }
    }
  }
}
