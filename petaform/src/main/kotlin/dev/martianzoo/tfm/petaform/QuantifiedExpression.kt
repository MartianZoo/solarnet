package dev.martianzoo.tfm.petaform

data class QuantifiedExpression(val expr: TypeExpression, val scalar: Int = 1): PetaformNode() {
  init { require(scalar >= 0) }
  override fun toString() = petaform()
  override val children = listOf(expr)

  fun petaform(forceScalar: Boolean = false, forceExpression: Boolean = false) = when {
    (!forceExpression && expr == DEFAULT_TYPE_EXPRESSION) -> "$scalar"
    (!forceScalar && scalar == 1) -> "$expr"
    else -> "$scalar ${expr}"
  }
}
