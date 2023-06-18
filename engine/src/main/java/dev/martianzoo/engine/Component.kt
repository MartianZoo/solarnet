package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.Type
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.MClass
import dev.martianzoo.types.MType

/** An *instance* of some concrete [MType]; a [ComponentGraph] is a multiset of these. */
public class Component internal constructor(private val mtype: MType) : Type {
  init {
    if (mtype.abstract) throw Exceptions.abstractComponent(mtype)
  }

  internal val isCustom: Boolean = mtype.root.custom != null

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [MClass.dependencies].
   */
  public val dependencyComponents: List<Component> =
      mtype.typeDependencies.map { it.boundType.toComponent() }

  public val owner: Player? =
      if (hasType(mtype.loader.resolve(OWNER.expression))) {
        Player(mtype.className)
      } else {
        val dep = mtype.dependencies.getIfPresent(Key(OWNED, 0))
        dep?.let { Player(dep.className) }
      }

  private val xerForEffects: PetTransformer by lazy {
    with(Transformers(mtype.loader)) {
      chain(
          substituter(mtype.root.defaultType, mtype),
          owner?.let(::replaceOwnerWith),
          replaceThisExpressionsWith(expression),
      )
    }
  }

  private val xerForCustom: PetTransformer by lazy {
    with(Transformers(mtype.loader)) {
      chain(
          standardPreprocess(),
          substituter(mtype.root.baseType, mtype),
          owner?.let(::replaceOwnerWith),
      )
    }
  }

  internal val effects: List<Effect> by lazy {
    mtype.root.classEffects.map(xerForEffects::transform)
  }

  fun hasType(supertype: Type) = mtype.narrows(supertype)

  fun hasType(supertype: Type, info: TypeInfo) = mtype.narrows(supertype, info)

  fun prepareCustom(reader: GameReader): Instruction {
    val translated = mtype.root.custom!!.prepare(reader, mtype)
    return xerForCustom.transform(translated)
  }

  // TODO this whole section should be replaceable by just adding `by mtype` after `Type` in the
  // class signature. Doing that works from inside IDEA but gets CCE from command line
  override val abstract by mtype::abstract
  override val refinement by mtype::refinement
  override val className by mtype::className
  override val expression by mtype::expression
  override val expressionFull by mtype::expressionFull
  override fun narrows(that: Type, info: TypeInfo) = mtype.narrows(that, info)

  override fun equals(other: Any?) = other is Component && other.mtype == mtype

  override fun hashCode() = mtype.hashCode()

  override fun toString() = "[${mtype.expressionFull}]"

  companion object {
    public fun Expression.toComponent(game: GameReader) = Component(game.resolve(this) as MType)
    public fun HasExpression.toComponent(game: GameReader) =
        this as? Component ?: expression.toComponent(game)
  }
}
