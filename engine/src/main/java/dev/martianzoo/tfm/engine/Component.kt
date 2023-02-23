package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.tfm.types.PType

/**
 * An *instance* of some concrete [PType]; a [ComponentGraph] is a multiset of these. Any usage that
 * is not related to what instances actually exist in a game state should be using [PType] instead.
 */
public data class Component(internal val ptype: PType) {
  init {
    require(!ptype.abstract) { "Component can't be of an abstract type: ${ptype.typeExprFull}" }
  }

  public fun alwaysHasType(thatType: PType) = ptype.isSubtypeOf(thatType)

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. An empty list is returned for a
   * class component like `Class<Tile>`.
   */
  public val dependencies: List<Component> by lazy {
    val depTypes = ptype.allDependencies.types.filterIsInstance<TypeDependency>()
    depTypes.map { Component(it.ptype) }
  }

  public fun effects(): List<Effect> {
    return listOf() // TODO
  }

  override fun toString() = "[$ptype]"
}
