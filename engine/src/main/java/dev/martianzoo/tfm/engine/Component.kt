package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.types.PClass
import dev.martianzoo.tfm.types.PType

/**
 * An *instance* of some concrete [PType]; a [ComponentGraph] is a multiset of these. For any use
 * case unrelated to what instances actually exist in a game state, use [PType] instead.
 */
public data class Component
private constructor(
    /** The concrete type of this component. */
    private val ptype: PType,
) {
  companion object {
    public fun ofType(ptype: PType): Component = Component(ptype)
  }

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
  public val dependencies: List<Component> = ptype.dependencies.asSet.map { ofType(it.bound) }

  /**
   * This component's effects; while the component exists in a game state, the effects are active.
   */
  public fun effects(): List<Effect> {
    return ptype.pclass.allSuperclasses.flatMap { superclass ->
      superclass.classEffects.map { decl ->
        val linkages = decl.linkages
        val effect = decl.effect
        effect
        // BIGTODO needs translating
      }
    }
  }

  override fun toString() = "[${ptype.expressionFull}]"
}
