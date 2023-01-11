package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.SpecialComponent.Default

data class QuantifiedExpression(
    val expression: TypeExpression = Default.type,
    val scalar: Int = 1,
) : PetNode() {
  init {
    require(scalar >= 0)
  }

  override val kind = QuantifiedExpression::class.simpleName!!

  override fun toString() = toString(false, false)

  fun toString(forceScalar: Boolean = false, forceType: Boolean = false) = when {
    !forceType && expression == Default.type -> "$scalar"
    !forceScalar && scalar == 1 -> "$expression"
    else -> "$scalar $expression"
  }
}
