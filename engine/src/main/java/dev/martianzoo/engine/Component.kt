package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.Type
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasExpression
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
public class Component internal constructor(private val mtype: MType) : HasExpression by mtype {
  init {
    if (mtype.abstract) throw Exceptions.abstractComponent(mtype)
  }

  internal val type by ::mtype

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

  internal val effects: List<Effect> by lazy {
    val classEffectTransformer =
        with(Transformers(mtype.loader)) {
          chain(
              substituter(mtype.root.defaultType, mtype),
              owner?.let(::replaceOwnerWith),
              replaceThisExpressionsWith(expression),
          )
        }
    mtype.root.classEffects.map(classEffectTransformer::transform)
  }

  fun hasType(supertype: Type) = mtype.narrows(supertype)

  fun hasType(supertype: Type, info: TypeInfo) = mtype.narrows(supertype, info)

  fun prepareCustom(reader: GameReader): Instruction {
    val customOutputTransformer =
        with(Transformers(mtype.loader)) {
          chain(atomizer(), insertDefaults(), owner?.let(::replaceOwnerWith))
        }
    val translated = mtype.root.custom!!.prepare(reader, mtype)
    return customOutputTransformer.transform(translated)
  }

  override fun equals(other: Any?) = other is Component && other.mtype == mtype

  override fun hashCode() = mtype.hashCode()

  override fun toString() = "$mtype"

  companion object {
    public fun Expression.toComponent(game: GameReader) = Component(game.resolve(this) as MType)
    public fun HasExpression.toComponent(game: GameReader) =
        this as? Component ?: expression.toComponent(game)
  }
}
