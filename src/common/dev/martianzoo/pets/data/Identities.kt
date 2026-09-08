package dev.martianzoo.pets.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.api.SystemClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression

/** An identity that can initiate or continue game operations. */
public sealed interface Actor : HasClassName, HasExpression {
  public companion object {
    public val ADMIN: Actor = AdminActor
  }
}

/** A runtime identity that can own game-state components. */
internal sealed interface Owner : HasClassName, HasExpression

/** One occupied seat; both an [Actor] and an [Owner]. */
public data class Player(override val className: ClassName) : Actor, Owner {
  init {
    require(className != Actor.ADMIN.className) { "Admin is not a Player" }
  }

  override val expression: Expression = className.expression

  override val expressionFull: Expression
    get() = expression

  override fun toString(): String = className.toString()

  public companion object {
    /** Returns the conventional `Player1` through `PlayerN` identities in seat order. */
    public fun players(upTo: Int): List<Player> {
      require(upTo >= 0) { "player count cannot be negative: $upTo" }
      return (1..upTo).map { Player(player(it)) }
    }

    private fun player(seat: Int) = cn("Player$seat").also { require(seat > 0) }
  }
}

private data object AdminActor : Actor {
  override val className = SystemClasses.ADMIN
  override val expression: Expression = className.expression

  override val expressionFull: Expression
    get() = expression

  override fun toString() = className.toString()
}
