package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.toSetStrict

/** An instance of a concrete PType; a game state is made up of a multiset of these. */
public data class Component(private val ptype: PType) {
  init {
    require(!ptype.abstract) { ptype }
  }

  public fun hasType(thatType: PType) = ptype.isSubtypeOf(thatType)
  public val asTypeExpr = ptype.toTypeExprMinimal()

  public fun dependencies(): Set<Component> =
      if (ptype.isClassType) {
        setOf()
      } else {
        ptype.allDependencies.types.map { Component((it as TypeDependency).ptype) }.toSetStrict()
      }

  override fun toString() = "[$ptype]"
}
