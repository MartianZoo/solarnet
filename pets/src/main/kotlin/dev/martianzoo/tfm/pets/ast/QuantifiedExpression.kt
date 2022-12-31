package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.SpecialComponent.MEGACREDIT

data class QuantifiedExpression(val type: TypeExpression = DEFAULT_TYPE, val scalar: Int = 1): PetsNode() {
  init { require(scalar >= 0) }
  override val kind = QuantifiedExpression::class.simpleName!!

  override val children = setOfNotNull(type)

  override fun toString() = toString(false, false)

  fun toString(forceScalar: Boolean = false, forceType: Boolean = false) =
      when {
        !forceType && type == DEFAULT_TYPE -> "$scalar"
        !forceScalar && scalar == 1 -> "$type"
        else -> "$scalar $type"
      }
}

val DEFAULT_TYPE = MEGACREDIT.type
