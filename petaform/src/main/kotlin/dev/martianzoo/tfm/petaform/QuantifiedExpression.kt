package dev.martianzoo.tfm.petaform

data class QuantifiedExpression(val typeExpression: TypeExpression? = null, val scalar: Int? = null): PetaformNode() {
  init {
    if (scalar == null) {
      if (typeExpression == null) throw PetaformException("Must specify type or scalar")
    } else {
      require(scalar >= 0)
    }
  }
  override fun toString() = listOfNotNull(scalar, typeExpression).joinToString(" ")
  override val children = setOfNotNull(typeExpression)

  //fun petaform(forceScalar: Boolean = false, forceExpression: Boolean = false) = when {
  //  (!forceExpression && typeExpression == null) -> "$scalar"
  //  (!forceScalar && scalar == 1) -> "$typeExpression"
  //  else -> "$scalar ${typeExpression}"
  //}
}
