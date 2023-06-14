package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses
import dev.martianzoo.api.Type
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.Transforming
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.MType

/**
 * An *instance* of some concrete [MType]; a [ComponentGraph] is a multiset of these. For any use
 * case unrelated to what instances actually exist in a game state, use [MType] instead.
 */
public data class Component internal constructor(internal val mtype: MType) :
    HasClassName, HasExpression {
  companion object {
    public fun Expression.toComponent(game: GameReader) = Component(game.resolve(this) as MType)
    public fun HasExpression.toComponent(game: GameReader) = expressionFull.toComponent(game)
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
  public val dependencyComponents: List<Component> by lazy {
    mtype.typeDependencies.map { it.boundType.toComponent() } as List<Component>
  }

  public val owner: Player? by lazy {
    if (mtype.narrows(mtype.loader.resolve(SystemClasses.OWNER.expression))) {
      Player(className)
    } else {
      mtype.dependencies.getIfPresent(Key(SystemClasses.OWNED, 0))?.className?.let(::Player)
    }
  }

  private val xerForEffects: PetTransformer by lazy {
    with(Transformers(mtype.loader)) {
      PetTransformer.chain(
          substituter(mtype.root.defaultType, mtype),
          owner?.let(Transforming::replaceOwnerWith),
          Transforming.replaceThisExpressionsWith(expression),
      )
    }
  }

  private val xerForCustom: PetTransformer by lazy {
    with(Transformers(mtype.loader)) {
      PetTransformer.chain(
          standardPreprocess(),
          substituter(mtype.root.baseType, mtype),
          owner?.let(Transforming::replaceOwnerWith),
      )
    }
  }

  public val effects: List<Effect> by lazy {
    mtype.root.classEffects.map(xerForEffects::transform)
  }

  override val className by mtype::className
  override val expression by mtype::expression

  override val expressionFull by mtype::expressionFull

  override fun toString() = "[${mtype.expressionFull}]"

  fun hasType(supertype: Type) = mtype.narrows(supertype)

  fun prepareCustom(reader: GameReader): Instruction {
    val translated = mtype.root.custom!!.prepare(reader, mtype)
    return xerForCustom.transform(translated)
  }
}
