package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.ExpressionException
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

  /** The concrete Pets type in this component's direct ownership dependency, if it has one. */
  public val owner: Type? =
      if (hasType(mtype.loader.resolve(OWNER.expression))) {
        mtype
      } else {
        mtype.typeDependencies.singleOrNull { it.key == Key(OWNED, 0) }?.boundType
      }

  /** This component's owner when that owner is a seated Player. */
  internal val playerOwner: Player? =
      owner?.className?.let { if (Player.isValid(it)) Player(it) else null }

  internal val effects: List<Effect> by lazy {
    val transformers = Transformers(mtype.loader)
    val ownerBinding = owner?.let(::replaceOwnerWith)
    val thisBinding = replaceThisExpressionsWith(expression)

    if (owner == null || playerOwner != null) {
      val checkedBinding =
          transformers.checkedSubstituter(
              mtype.root.defaultType,
              mtype,
              ownerBinding,
              thisBinding,
          )
      mtype.root.classEffects.map { effect ->
        val bound = checkedBinding.transform(effect)
        try {
          mtype.loader.checkAllTypes(bound)
          bound
        } catch (e: ExpressionException) {
          throw ExpressionException(
              "invalid component effect for ${mtype.expressionFull}: $bound",
              e,
          )
        }
      }
    } else {
      val uncheckedBinding =
          chain(transformers.substituter(mtype.root.defaultType, mtype), ownerBinding, thisBinding)
      mtype.root.classEffects.mapNotNull { effect ->
        val bound = uncheckedBinding.transform(effect)
        try {
          mtype.loader.checkAllTypes(bound)
          bound
        } catch (e: ExpressionException) {
          // An Owner-only component can inherit an effect whose output is Player-bound. The source
          // effect is valid, but it does not apply to that Owner; for example, Opponent's starting
          // tiles do not score VictoryPoint<Player> components.
          val sourceEffect =
              replaceThisExpressionsWith(mtype.root.className.expression).transform(effect)
          mtype.loader.checkAllTypes(sourceEffect)
          null
        }
      }
    }
  }

  public fun hasType(supertype: Type, info: TypeInfo? = null) =
      info?.let { mtype.narrows(supertype, it) } ?: mtype.narrows(supertype)

  private val customOutputTransformer =
      with(Transformers(mtype.loader)) {
        chain(atomizer(), insertDefaults(), owner?.let(::replaceOwnerWith))
      }

  public fun prepareCustom(reader: GameReader): Instruction {
    val implementation = requireNotNull(mtype.root.custom)
    val translated = implementation.prepare(reader, mtype)
    return customOutputTransformer.transform(translated)
  }

  override fun equals(other: Any?) = other is Component && other.mtype == mtype

  override fun hashCode() = mtype.hashCode()

  override fun toString() = "$mtype"

  public companion object {
    public fun Expression.toComponent(game: GameReader) = Component(game.resolve(this) as MType)

    public fun HasExpression.toComponent(game: GameReader) =
        this as? Component ?: expression.toComponent(game)
  }
}
