package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.tfm.types.PClass
import dev.martianzoo.tfm.types.PType

/**
 * An *instance* of some concrete [PType]; a [ComponentGraph] is a multiset of these. For any use
 * case unrelated to what instances actually exist in a game state, use [PType] instead.
 */
public data class Component(
    /** The concrete type of this component. */
    private val ptype: PType,
) {
  init {
    require(!ptype.abstract) { "Component can't be of an abstract type: ${ptype.typeExprFull}" }
  }

  /** The concrete type of this component. */
  public val type: Type by ::ptype

  /**
   * Whether this type is categorically a subtype of [thatType] for any possible game state. (In
   * the absence of refinements, this is an ordinary subtype check.
   */
  public fun alwaysHasType(thatType: PType) = ptype.isSubtypeOf(thatType)

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [PClass.allDependencyKeys].
   */
  public val dependencies: List<Component> by lazy {
    val depTypes = ptype.allDependencies.types.filterIsInstance<TypeDependency>()
    depTypes.map { Component(it.ptype) }
  }

  /**
   * This component's effects, which are active in a game state if and only if this component
   * exists in that game state.
   */
  public fun effects(): List<Effect> {
    return listOf() // TODO
  }

  override fun toString() = "[$ptype]"
}
