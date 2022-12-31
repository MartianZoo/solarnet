package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.pets.SpecialComponent.MEGACREDIT

data class QuantifiedExpression(val type: TypeExpression? = null, val scalar: Int? = null): PetsNode() {
  init {
    if (scalar == null) {
      if (type == null) throw PetsException("Must specify type or scalar")
    } else {
      require(scalar >= 0)
    }
  }
  override fun toString() = listOfNotNull(scalar, type).joinToString(" ")

  override val children = setOfNotNull(type)
  fun explicit() = copy(type = type ?: MEGACREDIT.type, scalar = scalar ?: 1)

  override val kind = "QuantifiedExpression"
}
