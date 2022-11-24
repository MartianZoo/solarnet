package dev.martianzoo.tfm.petaform.api

data class QuantifiedExpression(val expr: Expression, val scalar: Int = 1): PetaformObject {
  override val petaform = petaform()

  fun petaform(forceScalar: Boolean = false, forceExpression: Boolean = false) = when {
    (!forceExpression && expr == Expression.DEFAULT) -> "$scalar"
    (!forceScalar && scalar == 1) -> expr.petaform
    else -> "$scalar ${expr.petaform}"
  }
}
