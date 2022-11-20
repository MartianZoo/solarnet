package dev.martianzoo.tfm.petaform.api

data class QuantifiedExpression(val expr: Expression, val scalar: Int = 1): PetaformObject {
  override val petaform = when {
    expr == Expression.DEFAULT -> "$scalar"
    scalar == 1 -> expr.petaform
    else -> "$scalar ${expr.petaform}"
  }

  fun petaformWithScalar() = when {
    expr == Expression.DEFAULT -> "MAX $scalar"
    else -> "MAX $scalar ${expr.petaform}"
  }
}
