package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.types.PType

/**
 * An instance of a concrete PType; a game state is made up of a multiset of these.
 */
public data class Component(private val ptype: PType) {
  init {
    require(!ptype.abstract) { ptype }
  }

  public fun hasType(thatType: PType) = ptype.isSubtypeOf(thatType)
  public val asTypeExpr = ptype.toTypeExprFull() // TODO minimal?
  override fun toString() = "[$ptype]"
}
