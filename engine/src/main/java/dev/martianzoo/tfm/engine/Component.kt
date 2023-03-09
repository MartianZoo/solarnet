package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.engine.Exceptions.AbstractInstructionException
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.HasExpression
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MType

/**
 * An *instance* of some concrete [MType]; a [ComponentGraph] is a multiset of these. For any use
 * case unrelated to what instances actually exist in a game state, use [MType] instead.
 */
public data class Component private constructor(val mtype: MType) : HasExpression by mtype {

  companion object {
    public fun ofType(mtype: MType): Component = Component(mtype)
  }

  init {
    if (mtype.abstract) throw AbstractInstructionException(mtype)
  }

  /**
   * Whether this type is categorically a subtype of [thatType] for any possible game state. (In the
   * absence of refinements, this is an ordinary subtype check.)
   */
  public fun hasType(thatType: MType) = mtype.isSubtypeOf(thatType)

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [MClass.dependencies].
   */
  public val dependencyComponents = mtype.dependencies.asSet.map { ofType(it.boundType) }

  /**
   * This component's effects; while the component exists in a game state, the effects are active.
   */
  public val effects: List<Effect> by lazy {
    mtype.mclass.classEffects.map { decl ->
      // val linkages = decl.linkages
      // Transform for some "linkages" (TODO the rest, and do in more principled way)
      val effect =
          mtype.mclass.loader.transformer // ridiculoso
              .deprodify(decl.effect)
              .replaceAll(THIS.classExpression(), expression.className.classExpression())
              .replaceAll(THIS.expr, expressionFull)
      owner()?.let { effect.replaceAll(OWNER, it) } ?: effect
    }
  }

  val activeEffects: List<ActiveEffect> by lazy {
    effects.map { ActiveEffect.from(it, this) }
  }

  public fun owner(): ClassName? = mtype.dependencies.getIfPresent(Key(OWNED, 0))?.className

  override fun toString() = "[${mtype.expressionFull}]"
}
