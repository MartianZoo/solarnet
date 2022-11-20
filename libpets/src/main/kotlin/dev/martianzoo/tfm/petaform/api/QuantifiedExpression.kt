package dev.martianzoo.tfm.petaform.api

data class QuantifiedExpression(val expr: Expression, val scalar: Int = 1): PetaformObject {
  override val asSource = when {
    expr == Expression.DEFAULT -> "$scalar"
    scalar == 1 -> expr.asSource
    else -> "$scalar ${expr.asSource}"
  }

  fun asSourceWithMandatoryScalar() = when {
    expr == Expression.DEFAULT -> "MAX $scalar"
    else -> "MAX $scalar ${expr.asSource}"
  }
}
