package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.types.Class
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Type

/** An *instance* of some concrete [Type]; a [ComponentGraph] is a multiset of these. */
public class Component internal constructor(public val type: Type) : HasExpression by type {
  init {
    if (type.abstract) throw Exceptions.abstractComponent(type)
    if (type.phantom) throw ExpressionException("inactive type has no components: $type")
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
      if (type.classTable.isActive(OWNER) && hasType(type.classTable.resolve(OWNER.expression))) {
        type
      } else {
        type.typeDependencies.singleOrNull { it.key == Key(OWNED, 0) }?.boundType
      }

  /** This component's owner when that owner is a seated Player. */
  internal val playerOwner: Player? = owner?.let(Player::fromType)

  /** Context-free check; throws if [supertype] has a state-dependent refinement. */
  public fun hasType(supertype: Type): Boolean = type.isSubtypeOf(supertype)

  /** State-aware check for types that may have refinements. */
  public fun hasType(supertype: Type, info: TypeInfo): Boolean = type.narrows(supertype, info)

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
