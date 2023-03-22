package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.AbstractInstructionException
import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PureTransformers.replaceOwnerWith
import dev.martianzoo.tfm.pets.PureTransformers.replaceThisWith
import dev.martianzoo.tfm.pets.PureTransformers.transformInSeries
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasExpression
import dev.martianzoo.tfm.pets.ast.PetNode
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
  public fun effects(game: Game): List<ActiveEffect> {
    return mtype.mclass.classEffects.map { decl ->
      val depLinkages: Set<ClassName> = decl.depLinkages
      val xers = mtype.loader.transformers
      val blah = transformInSeries(
          Substituter(mtype.findSubstitutions(depLinkages)),
          replaceThisWith(mtype.expression),
          xers.deprodify(),
          owner()?.let { replaceOwnerWith(it) },
      ).transform(decl.effect.unprocessed) // TODO
      ActiveEffect.from(blah, this, game, decl.triggerLinkages)
    }
  }

  public fun owner(): ClassName? = mtype.dependencies.getIfPresent(Key(OWNED, 0))?.className

  override fun toString() = "[${mtype.expressionFull}]"

  fun toShortString() = "[${mtype.expressionShort}]"
}

class Substituter(private val subs: Map<ClassName, Expression>) : PetTransformer() {
  override fun <P : PetNode> transform(node: P): P {
    if (node is Expression) {
      val replacement: Expression? = subs[node.className]
      if (replacement != null) return replacement.addArgs(node.arguments) as P
    }
    return transformChildren(node)
  }
}
