package dev.martianzoo.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression

/** An identity that can initiate or continue game operations. */
public sealed interface Actor : HasClassName, HasExpression {
  public companion object {
    public val ENGINE: Actor = EngineActor
  }
}

/** A runtime identity that can own game-state components. */
internal sealed interface Owner : HasClassName, HasExpression

/** One canonical occupied seat; both an [Actor] and an [Owner]. */
public class Player private constructor(override val className: ClassName) : Actor, Owner {
  override val expression: Expression by lazy { className.expression }
  override val expressionFull: Expression by ::expression

  override fun toString(): String = className.toString()

  public companion object {
    public val PLAYER1: Player = Player(player(1))
    public val PLAYER2: Player = Player(player(2))
    public val PLAYER3: Player = Player(player(3))
    private val PLAYER4: Player = Player(player(4))
    private val PLAYER5: Player = Player(player(5))
    private val allPlayers: List<Player> = listOf(PLAYER1, PLAYER2, PLAYER3, PLAYER4, PLAYER5)
    private val playersByClassName: Map<ClassName, Player> =
        allPlayers.associateBy(Player::className)

    /** Returns the canonical Player for [className]. */
    public operator fun invoke(className: ClassName): Player =
        requireNotNull(fromClassNameOrNull(className)) {
          "not a canonical Player class: $className"
        }

    /** Returns the canonical Player for [className], or null when it does not name a Player. */
    public fun fromClassNameOrNull(className: ClassName): Player? = playersByClassName[className]

    /** Returns the canonical `Player1` through `Player5` identities in seat order. */
    public fun players(upTo: Int): List<Player> {
      require(upTo in 0..5) { "player count must be between 0 and 5: $upTo" }
      return allPlayers.subList(0, upTo)
    }

    /** Whether [name] is one of the canonical `Player1` through `Player5` class names. */
    public fun isValid(name: String): Boolean = allPlayers.any { it.className.toString() == name }

    /** Whether [name] is one of the canonical `Player1` through `Player5` class names. */
    public fun isValid(name: ClassName): Boolean = fromClassNameOrNull(name) != null

    private fun player(seat: Int) = cn("Player$seat").also { require(seat in 1..5) }
  }
}

private data object EngineActor : Actor {
  override val className = cn("Engine")
  override val expression by lazy { className.expression }
  override val expressionFull by ::expression

  override fun toString() = className.toString()
}
