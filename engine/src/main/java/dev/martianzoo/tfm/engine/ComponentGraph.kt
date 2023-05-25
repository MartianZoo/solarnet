package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.TypeInfo
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset

/**
 * A multiset of [Component] instances; the "present" state of a game in progress. It is a plain
 * multiset, but called a "graph" because these component instances have references to their
 * dependencies which are also stored in the multiset.
 */
public interface ComponentGraph {

  /**
   * Does at least one instance of [component] exist currently? (That is, is [countComponent]
   * nonzero?
   */
  operator fun contains(component: Component): Boolean

  /** How many instances of the exact component [component] currently exist? */
  fun countComponent(component: Component): Int

  /** How many total component instances have the type [parentType] (or any of its subtypes)? */
  fun count(parentType: MType, info: TypeInfo): Int

  /**
   * Returns all component instances having the type [parentType] (or any of its subtypes), as a
   * multiset. The size of the returned collection will be `[count]([parentType])` . If [parentType]
   * is `Component` this will return the entire component multiset.
   */
  fun getAll(parentType: MType, info: TypeInfo): Multiset<Component>

  /**
   * An *instance* of some concrete [MType]; a [ComponentGraph] is a multiset of these. For any use
   * case unrelated to what instances actually exist in a game state, use [MType] instead.
   */
  public data class Component private constructor(internal val mtype: MType) :
      HasClassName, HasExpression {
    companion object {
      public fun Expression.toComponent(game: GameReader) = Component(game.resolve(this) as MType)
      public fun HasExpression.toComponent(game: GameReader) = expressionFull.toComponent(game)
      public fun MType.toComponent() = Component(this)
    }

    init {
      if (mtype.abstract) throw Exceptions.abstractComponent(mtype)
    }

    /**
     * The full list of dependency instances of this component; *this* component cannot exist in a
     * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
     * `Class<Tile>` has an empty dependency list, despite its appearance. The list order
     * corresponds to [MClass.dependencies].
     */
    public val dependencyComponents by lazy {
      mtype.typeDependencies.map { it.boundType.toComponent() }
    }


    public val owner: Player? by lazy {
      if (mtype.narrows(mtype.loader.resolve(OWNER.expression))) {
        Player(className)
      } else {
        mtype.dependencies.getIfPresent(Key(OWNED, 0))?.className?.let(::Player)
      }
    }

    // TODO understand why these are different

    val xerForEffects: PetTransformer =
        with(mtype.loader.transformers) {
          chain(
              substituter(mtype.root.defaultType, mtype),
              deprodify(),
              owner?.let(::replaceOwnerWith),
              replaceThisExpressionsWith(expression),
          )
        }

    val xerForCustom: PetTransformer =
        with(mtype.loader.transformers) {
          chain(
              standardPreprocess(),
              substituter(mtype.root.baseType, mtype),
              owner?.let(::replaceOwnerWith),
          )
        }

    public val effects: List<Effect> by lazy {
      mtype.root.classEffects.map(xerForEffects::transform)
    }

    override val className by mtype::className
    override val expression by mtype::expression

    override val expressionFull by mtype::expressionFull

    override fun toString() = "[${mtype.expressionFull}]"

    fun hasType(supertype: Type) = mtype.narrows(supertype)

    val allowedRange: IntRange = mtype.root.componentCountRange

    fun prepareCustom(reader: GameReader): Instruction {
      val translated = mtype.root.custom!!.prepare(reader, mtype)
      return xerForCustom.transform(translated)
    }
  }
}
