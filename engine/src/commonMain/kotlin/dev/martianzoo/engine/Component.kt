package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.types.Class
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Type

/** An *instance* of some concrete [Type]; a [ComponentGraph] is a multiset of these. */
public class Component internal constructor(public val type: Type) : HasExpression by type {
  init {
    if (type.abstract) throw Exceptions.abstractComponent(type)
  }

  internal val isCustom: Boolean = type.rootClass.declaration.custom

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [Class.dependencies].
   */
  public val dependencyComponents: List<Component> =
      type.typeDependencies.map { it.boundType.toComponent() }

  /** The concrete Pets type in this component's direct ownership dependency, if it has one. */
  public val owner: Type? =
      if (
          type.classTable.findClass(OWNER) != null &&
              hasType(type.classTable.resolve(OWNER.expression))
      ) {
        type
      } else {
        type.typeDependencies.singleOrNull { it.key == Key(OWNED, 0) }?.boundType
      }

  /** This component's owner when that owner is a seated Player. */
  internal val playerOwner: Player? =
      owner?.className?.let { if (Player.isValid(it)) Player(it) else null }

  internal fun effects(transformers: Transformers): List<Effect> {
    val ownerBinding = owner?.let(::replaceOwnerWith)
    val thisBinding = replaceThisExpressionsWith(expression)

    return if (owner == null || playerOwner != null) {
      val checkedBinding =
          transformers.checkedSubstituter(
              type.rootClass.defaultType,
              type,
              ownerBinding,
              thisBinding,
              transformers.insertDeferredComplementDefaults(expression),
          )
      transformers.classEffects(type.rootClass).map { effect ->
        val bound = checkedBinding.transform(effect)
        try {
          type.classTable.checkAllTypes(bound)
          bound
        } catch (e: ExpressionException) {
          throw ExpressionException(
              "invalid component effect for ${type.expressionFull}: $bound",
              e,
          )
        }
      }
    } else {
      val uncheckedBinding =
          chain(
              transformers.substituter(type.rootClass.defaultType, type),
              ownerBinding,
              thisBinding,
              transformers.insertDeferredComplementDefaults(expression),
          )
      transformers.classEffects(type.rootClass).mapNotNull { effect ->
        val bound = uncheckedBinding.transform(effect)
        try {
          type.classTable.checkAllTypes(bound)
          bound
        } catch (e: ExpressionException) {
          // An Owner-only component can inherit an effect whose output is Player-bound. The source
          // effect is valid, but it does not apply to that Owner; for example, Opponent's
          // starting
          // tiles do not score VictoryPoint<Player> components.
          val sourceEffect =
              replaceThisExpressionsWith(type.rootClass.className.expression).transform(effect)
          type.classTable.checkAllTypes(sourceEffect)
          null
        }
      }
    }
  }

  /** Context-free check; throws if [supertype] has a state-dependent refinement. */
  public fun hasType(supertype: Type): Boolean = type.isSubtypeOf(supertype)

  /** State-aware check for types that may have refinements. */
  public fun hasType(supertype: Type, info: TypeInfo): Boolean = type.narrows(supertype, info)

  private val customOutputTransformer =
      with(Transformers(type.classTable)) {
        chain(atomizer(), insertDefaults(), owner?.let(::replaceOwnerWith))
      }

  public fun prepareCustom(reader: GameReader): Instruction {
    require(type.rootClass.declaration.custom)
    val implementation = reader.ruleset.customClass(type.className)
    val translated = implementation.prepare(reader, type)
    return customOutputTransformer.transform(translated)
  }

  override fun equals(other: Any?): Boolean = other is Component && other.type == type

  override fun hashCode(): Int = type.hashCode()

  override fun toString(): String = "$type"

  public companion object {
    public fun Expression.toComponent(game: GameReader): Component = Component(game.resolve(this))

    public fun HasExpression.toComponent(game: GameReader): Component =
        this as? Component ?: expression.toComponent(game)
  }
}

public fun Type.toComponent(): Component {
  if (abstract) throw Exceptions.abstractComponent(this)
  return Component(this)
}
