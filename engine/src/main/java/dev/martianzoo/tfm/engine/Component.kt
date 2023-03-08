package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.HasExpression
import dev.martianzoo.tfm.types.Dependency
import dev.martianzoo.tfm.types.PClass
import dev.martianzoo.tfm.types.PType

/**
 * An *instance* of some concrete [PType]; a [ComponentGraph] is a multiset of these. For any use
 * case unrelated to what instances actually exist in a game state, use [PType] instead.
 */
public data class Component
private constructor(val ptype: PType) : HasExpression by ptype {

  companion object {
    public fun ofType(ptype: PType): Component = Component(ptype)
  }

  init {
    require(!ptype.abstract) { "Component can't be of an abstract type: ${ptype.expressionFull}" }
  }

  /**
   * Whether this type is categorically a subtype of [thatType] for any possible game state. (In the
   * absence of refinements, this is an ordinary subtype check.)
   */
  public fun hasType(thatType: PType) = ptype.isSubtypeOf(thatType)

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [PClass.dependencies].
   */
  public val dependencyComponents = ptype.dependencies.asSet.map { ofType(it.boundType) }

  /**
   * This component's effects; while the component exists in a game state, the effects are active.
   */
  public fun effects(): List<Effect> {
    return ptype.pclass.allSuperclasses.flatMap { superclass ->
      superclass.classEffects.map { decl ->
        // val linkages = decl.linkages
        var effect = ptype.pclass.loader.transformer.deprodify(decl.effect) // ridiculoso

        // Transform for some "linkages" (TODO the rest, and do in more principled way)
        effect = effect.replaceAll(THIS.expr, expressionFull)
        val owner = ptype.dependencies.getIfPresent(Dependency.Key(OWNED, 0)) ?: return@map effect
        effect.replaceAll(OWNER, owner.className)
      }
    }
  }

  override fun toString() = "[${ptype.expressionFull}]"
}
