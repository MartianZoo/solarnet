package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.engine.LiveNodes.LiveEffect
import dev.martianzoo.tfm.pets.AstTransforms
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.types.Dependency.TypeDependency
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.toSetStrict

/**
 * An *instance* of some concrete [PType]; a [ComponentGraph] is a multiset of these. Any usage that
 * is not related to what instances actually exist in a game state should be using [PType] instead.
 */
public data class Component(internal val ptype: PType) {
  init {
    require(!ptype.abstract) { "Component can't be of an abstract type: ${ptype.typeExprFull}" }
  }

  public fun hasType(thatType: PType) = ptype.isSubtypeOf(thatType)

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. An empty list is returned for a
   * class component like `Class<Tile>`.
   */
  public val dependencies: List<Component> by lazy {
    val depTypes = ptype.allDependencies.types.filterIsInstance<TypeDependency>()
    depTypes.map { Component(it.ptype) }
  }

  internal fun effects(game: Game): Set<LiveEffect> {
    return ptype.pclass.classEffects
        .map {
          var fx = it
          fx = AstTransforms.replaceTypes(fx, THIS.type, ptype.typeExpr)
          // specialize for deps... owner...
          LiveNodes.from(fx, game)
        }
        .toSetStrict()
  }

  val typeExpr by ptype::typeExpr
  val typeExprFull by ptype::typeExprFull

  override fun toString() = "[$ptype]"
}
