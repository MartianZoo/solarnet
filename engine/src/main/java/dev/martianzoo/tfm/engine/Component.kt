package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.Effect
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
  constructor(pclass: PClass) : this(pclass.baseType)

  init {
    require(!ptype.abstract) { "Component can't be of an abstract type: ${ptype.expressionFull}" }
  }

  /** The concrete type of this component. */
  public val type: Type by ::ptype

  /**
   * Whether this type is categorically a subtype of [thatType] for any possible game state. (In the
   * absence of refinements, this is an ordinary subtype check.)
   */
  public fun alwaysHasType(thatType: PType) = ptype.isSubtypeOf(thatType)

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [PClass.allDependencyKeys].
   */
  public val dependencies: List<Component> by lazy {
    ptype.dependencies.realDependencies.map { Component(it.bound) }
  }

  /**
   * This component's effects, which are active in a game state if and only if this component exists
   * in that game state.
   */
  public fun effects(): List<Effect> {
    return listOf() // BIGTODO
    //   return ptype.pclass.effects.map { effDecl ->
    //     val links = effDecl.
    //   }
  }

  override fun toString() = "[$ptype]"
}
