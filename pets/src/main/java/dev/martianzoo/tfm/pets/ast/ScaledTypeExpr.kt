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

data class ScaledTypeExpr(
    val scalar: Int = 1,
    val typeExpr: TypeExpr = MEGACREDIT.type,
) : PetNode() {
  override fun visitChildren(visitor: Visitor) = visitor.visit(typeExpr)

  override fun toString() = toString(false, false)

  fun toString(forceScalar: Boolean = false, forceType: Boolean = false) =
      when {
        !forceType && typeExpr == MEGACREDIT.type -> "$scalar"
        !forceScalar && scalar == 1 -> "$typeExpr"
        else -> "$scalar $typeExpr"
      }

  init {
    require(scalar >= 0)
  }

  override val kind = ScaledTypeExpr::class.simpleName!!

  companion object : PetParser() {
    fun scaledType(scalar: Int? = null, typeExpr: TypeExpr? = null) =
        ScaledTypeExpr(scalar ?: 1, typeExpr ?: MEGACREDIT.type)

    fun scaledType(typeExpr: TypeExpr) = ScaledTypeExpr(1, typeExpr)

    fun scaledType(text: String) = Parsing.parse(parser(), text)

    fun parser(): Parser<ScaledTypeExpr> {
      return parser {
        val scalarAndOptionalType = scalar and optional(TypeExpr.parser())
        val optionalScalarAndType = optional(scalar) and TypeExpr.parser()

        scalarAndOptionalType or optionalScalarAndType map { (scalar, expr) -> scaledType(scalar, expr) }
      }
    }
  }
}
