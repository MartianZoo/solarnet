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
internal sealed interface Owner : HasClassName, HasExpression {
  public companion object {
    /** Returns the supported runtime Owner represented by [className]. */
    internal fun fromClassName(className: ClassName): Owner =
        when {
          Player.isValid(className) -> Player(className)
          else -> error("not a supported Owner identity: $className")
        }
  }
}

/** One of the actual people or bots playing the game; both an [Actor] and an [Owner]. */
public data class Player(override val className: ClassName) : Actor, Owner {
  init {
    require(isValid(className.toString())) { className }
  }

  override val expression: Expression by lazy { className.expression }
  override val expressionFull: Expression by ::expression

  override fun toString(): String = className.toString()

  public companion object {
    internal val regex = Regex("^Player[1-5]$")

    public val PLAYER1: Player = Player(player(1))
    public val PLAYER2: Player = Player(player(2))
    public val PLAYER3: Player = Player(player(3))
    private val PLAYER4 = Player(player(4))
    private val PLAYER5 = Player(player(5))

    public fun players(upTo: Int): List<Player> =
        listOf(PLAYER1, PLAYER2, PLAYER3, PLAYER4, PLAYER5).subList(0, upTo)

    public fun isValid(name: String): Boolean = name.matches(regex)

    public fun isValid(name: ClassName): Boolean = isValid(name.toString())

    private fun player(seat: Int) = cn("Player$seat").also { require(seat in 1..5) }
  }
}

private data object EngineActor : Actor {
  override val className = cn("Engine")
  override val expression by lazy { className.expression }
  override val expressionFull by ::expression

  override fun toString() = className.toString()
}
