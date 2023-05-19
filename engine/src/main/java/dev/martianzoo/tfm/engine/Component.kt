package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.SpecialClassNames.OWNED
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.PetTransformer.Companion.chain
import dev.martianzoo.tfm.pets.Transforming.replaceOwnerWith
import dev.martianzoo.tfm.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MType

/**
 * An *instance* of some concrete [MType]; a [ComponentGraph] is a multiset of these. For any use
 * case unrelated to what instances actually exist in a game state, use [MType] instead.
 */
public data class Component private constructor(internal val mtype: MType) :
    HasClassName, HasExpression {
  companion object {
    private val cache: MutableMap<MType, Component> = mutableMapOf()
    public fun ofType(mtype: MType) = cache.computeIfAbsent(mtype, ::Component)

    public fun Expression.toComponent(game: GameReader) = ofType(game.resolve(this) as MType)
    public fun HasExpression.toComponent(game: GameReader) = expressionFull.toComponent(game)
  }

  init {
    if (mtype.abstract) throw Exceptions.abstractComponent(mtype)
    // check about CustomClasses? TODO
  }

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [MClass.dependencies].
   */
  public val dependencyComponents by lazy { mtype.typeDependencies.map { ofType(it.boundType) } }

  public val petEffects: List<Effect> by lazy {
    val xers = mtype.loader.transformers
    val xer =
        chain(
            xers.substituter(mtype.root.defaultType, mtype),
            xers.deprodify(),
            owner?.let(::replaceOwnerWith),
            replaceThisExpressionsWith(mtype.expression),
        )
    mtype.root.classEffects.map(xer::transform)
  }

  /**
   * This component's effects; while the component exists in a game state, the effects are active.
   */
  internal val activeEffects: List<ActiveEffect> by lazy {
    petEffects.map { ActiveEffect.from(it, this) }
  }

  public val owner: Player? by lazy {
    if (mtype.narrows(mtype.loader.resolve(OWNER.expression))) {
      Player(mtype.className)
    } else {
      val dep = mtype.dependencies.getIfPresent(Key(OWNED, 0))
      dep?.let { Player(it.className) }
    }
  }

  override val className by mtype::className
  override val expression by mtype::expression
  override val expressionFull by mtype::expressionFull

  override fun toString() = "[${mtype.expressionFull}]"

  fun hasType(supertype: MType) = mtype.narrows(supertype)

  val allowedRange: IntRange = mtype.root.componentCountRange
}
