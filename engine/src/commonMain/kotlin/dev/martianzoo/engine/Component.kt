package dev.martianzoo.engine

import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.api.Exceptions
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Type
import kotlin.jvm.JvmInline

/** One concrete [Type] used as a value in a [ComponentGraph]. */
@JvmInline
public value class Component internal constructor(public val type: Type) : HasExpression {
  init {
    if (type.abstract) throw Exceptions.abstractComponent(type)
  }

  internal val isCustom: Boolean
    get() = type.rootClass.declaration.custom

  /**
   * The full list of dependency instances of this component; *this* component cannot exist in a
   * [ComponentGraph] unless *all* of the returned components do. Note that a class type like
   * `Class<Tile>` has an empty dependency list, despite its appearance. The list order corresponds
   * to [Class.dependencies].
   */
  internal val dependencyComponents: List<Component>
    get() = type.typeDependencies.map { it.boundType.toComponent() }

  /** The concrete Pets type in this component's direct ownership dependency, if it has one. */
  public val owner: Type?
    get() =
        if (type.rootClass.allSuperclasses().any { it.className == OWNER }) {
          type
        } else {
          type.typeDependencies.singleOrNull { it.key == Key(OWNED, 0) }?.boundType
        }

  /** This component's owner when that owner is a seated Player. */
  internal val playerOwner: Player?
    get() = owner?.className?.let(Player::fromClassNameOrNull)

  override val expression: Expression
    get() = type.expression

  override val expressionFull: Expression
    get() = type.expressionFull

  /** Context-free check; throws if [supertype] has a state-dependent refinement. */
  public fun hasType(supertype: Type): Boolean = type.isSubtypeOf(supertype)

  /** State-aware check for types that may have refinements. */
  public fun hasType(supertype: Type, info: TypeInfo): Boolean = type.narrows(supertype, info)

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
