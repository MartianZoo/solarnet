package dev.martianzoo.tfm.pets

data class QuantifiedExpression(val typeExpression: TypeExpression? = null, val scalar: Int? = null): PetsNode() {
  init {
    if (scalar == null) {
      if (typeExpression == null) throw PetsException("Must specify type or scalar")
    } else {
      require(scalar >= 0)
    }
  }
  override fun toString() = listOfNotNull(scalar, typeExpression).joinToString(" ")
  override val children = setOfNotNull(typeExpression)

  fun explicit() = copy(typeExpression ?: te("Megacredit"), scalar ?: 1)

  //fun pets(forceScalar: Boolean = false, forceExpression: Boolean = false) = when {
  //  (!forceExpression && typeExpression == null) -> "$scalar"
  //  (!forceScalar && scalar == 1) -> "$typeExpression"
  //  else -> "$scalar ${typeExpression}"
  //}
}
