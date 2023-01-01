package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.SpecialComponent.DEFAULT

data class QuantifiedExpression(val type: TypeExpression = DEFAULT.type, val scalar: Int = 1): PetsNode() {
  init { require(scalar >= 0) }
  override val kind = QuantifiedExpression::class.simpleName!!

  override val children = setOfNotNull(type)

  override fun toString() = toString(false, false)

  fun toString(forceScalar: Boolean = false, forceType: Boolean = false) =
      when {
        !forceType && type == DEFAULT.type -> "$scalar"
        !forceScalar && scalar == 1 -> "$type"
        else -> "$scalar $type"
      }
}
