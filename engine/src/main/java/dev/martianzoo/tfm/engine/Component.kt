package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.engine.Game.SnReader
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MType

/**
 * An *instance* of some concrete [MType]; a [ComponentGraph] is a multiset of these. For any use
 * case unrelated to what instances actually exist in a game state, use [MType] instead.
 */
public data class Component private constructor(internal val mtype: MType) :
    HasClassName, HasExpression {
  companion object {
    public fun Expression.toComponent(game: SnReader) = Component(game.resolve(this))
    public fun HasExpression.toComponent(game: SnReader) = expressionFull.toComponent(game)
    public fun MType.toComponent() = Component(this)
  }

  init {
    if (mtype.abstract) throw Exceptions.abstractComponent(mtype)
  }

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [MClass.dependencies].
   */
  public val dependencyComponents by lazy {
    mtype.typeDependencies.map { it.boundType.toComponent() }
  }

  // TODO get rid of
  public val typeEffectsGetRidOf: List<Effect> = mtype.effects

  public val owner: Player? by mtype::owner

  override val className by mtype::className
  override val expression by mtype::expression
  override val expressionFull by mtype::expressionFull

  override fun toString() = "[${mtype.expressionFull}]"

  fun hasType(supertype: MType) = mtype.narrows(supertype)

  val allowedRange: IntRange = mtype.root.componentCountRange
}
