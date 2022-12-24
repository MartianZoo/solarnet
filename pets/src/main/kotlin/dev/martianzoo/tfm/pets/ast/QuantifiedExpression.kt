package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.pets.SpecialComponent.MEGACREDIT

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

  fun explicit() = copy(typeExpression ?: MEGACREDIT.type, scalar ?: 1)
}
