package dev.martianzoo.pets.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
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
    public val CLASS_NAME: ClassName = cn("Player")
    public val PLAYER1: Player = Player(player(1))
    public val PLAYER2: Player = Player(player(2))
    public val PLAYER3: Player = Player(player(3))
    private val PLAYER4: Player = Player(player(4))
    private val PLAYER5: Player = Player(player(5))
    private val conventionalPlayers: List<Player> =
        listOf(PLAYER1, PLAYER2, PLAYER3, PLAYER4, PLAYER5)

    /** Returns the conventional `Player1` through `PlayerN` identities in seat order. */
    public fun players(upTo: Int): List<Player> {
      require(upTo in 0..5) { "player count must be between 0 and 5: $upTo" }
      return conventionalPlayers.subList(0, upTo)
    }

    private fun player(seat: Int) = cn("Player$seat").also { require(seat in 1..5) }
  }
}

private data object AdminActor : Actor {
  override val className = cn("Admin")
  override val expression: Expression = className.expression

  override val expressionFull: Expression
    get() = expression

  override fun toString() = className.toString()
}
