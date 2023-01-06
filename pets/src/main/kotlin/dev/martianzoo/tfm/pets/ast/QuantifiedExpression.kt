package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.SpecialComponent.DEFAULT

data class QuantifiedExpression(val expression: TypeExpression = DEFAULT.type, val scalar: Int = 1): PetsNode() {
  init { require(scalar >= 0) }
  override val kind = QuantifiedExpression::class.simpleName!!

  override fun toString() = toString(false, false)

  fun toString(forceScalar: Boolean = false, forceType: Boolean = false) =
      when {
        !forceType && expression == DEFAULT.type -> "$scalar"
        !forceScalar && scalar == 1 -> "$expression"
        else -> "$scalar $expression"
      }
}
