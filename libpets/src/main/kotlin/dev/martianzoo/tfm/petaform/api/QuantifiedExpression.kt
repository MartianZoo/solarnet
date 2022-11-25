package dev.martianzoo.tfm.petaform.api

data class QuantifiedExpression(val expr: Expression, val scalar: Int = 1): PetaformObject() {
  init { require(scalar >= 0) }
  override val children = listOf(expr)
  override fun toString() = petaform()
  override val hasProd = false

  fun petaform(forceScalar: Boolean = false, forceExpression: Boolean = false) = when {
    (!forceExpression && expr == Expression.DEFAULT) -> "$scalar"
    (!forceScalar && scalar == 1) -> "$expr"
    else -> "$scalar ${expr}"
  }
}
