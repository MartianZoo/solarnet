package dev.martianzoo.tfm.petaform

import dev.martianzoo.tfm.petaform.api.DEFAULT_EXPRESSION

data class QuantifiedExpression(val expr: Expression, val scalar: Int = 1): PetaformNode() {
  init { require(scalar >= 0) }
  override val children = listOf(expr)
  override fun toString() = petaform()

  fun petaform(forceScalar: Boolean = false, forceExpression: Boolean = false) = when {
    (!forceExpression && expr == DEFAULT_EXPRESSION) -> "$scalar"
    (!forceScalar && scalar == 1) -> "$expr"
    else -> "$scalar ${expr}"
  }
}
