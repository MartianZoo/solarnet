package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.RAW
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.ClassDeclaration.EffectDeclaration
import dev.martianzoo.tfm.pets.PetTransformer.Companion.transformInSeries
import dev.martianzoo.tfm.pets.PureTransformers.replaceOwnerWith
import dev.martianzoo.tfm.pets.PureTransformers.replaceThisWith
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.HasExpression
import dev.martianzoo.tfm.pets.ast.TransformNode
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MType

/**
 * An *instance* of some concrete [MType]; a [ComponentGraph] is a multiset of these. For any use
 * case unrelated to what instances actually exist in a game state, use [MType] instead.
 */
public data class Component private constructor(val mtype: MType) : HasClassName, HasExpression {
  // TODO can mtype be private?
  companion object {
    public fun ofType(mtype: MType): Component = Component(mtype)
  }

  init {
    if (mtype.abstract) throw UserException.abstractComponent(mtype)
  }

  public fun hasType(type: Type): Boolean = mtype.isSubtypeOf(type)

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [MClass.dependencies].
   */
  public val dependencyComponents = mtype.typeDependencies.map { ofType(it.boundType) }

  public val petEffects: List<EffectDeclaration> by lazy {
    mtype.root.classEffects.map { fxDecl ->
      val fx =
          transformInSeries(
                  mtype.loader.transformers.deprodify(),
                  Substituter(mtype.findSubstitutions(fxDecl.depLinkages)),
                  owner()?.let { replaceOwnerWith(it) },
                  replaceThisWith(mtype.expression),
                  TransformNode.unwrapper(RAW),
              )
              .transform(fxDecl.effect)
      fxDecl.copy(effect = fx, depLinkages = setOf())
    }
  }

  /**
   * This component's effects; while the component exists in a game state, the effects are active.
   */
  internal fun activeEffects(game: Game): List<ActiveEffect> {
    return petEffects.map { ActiveEffect.from(it.effect, this, game, it.triggerLinkages) }
  }

  public fun owner(): ClassName? = mtype.dependencies.getIfPresent(Key(OWNED, 0))?.className

  override val className by mtype::className
  override val expression by mtype::expression
  override val expressionFull by mtype::expressionFull

  override fun toString() = "[${mtype.expressionFull}]"

  val allowedRange: IntRange = mtype.root.componentCountRange
}

