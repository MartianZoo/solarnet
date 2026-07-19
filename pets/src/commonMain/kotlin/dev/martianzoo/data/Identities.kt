package dev.martianzoo.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** An identity that can initiate or continue game operations. */
sealed interface Actor : HasClassName, HasExpression {
  companion object {
    val ENGINE: Actor = EngineActor
  }
}

/** A runtime identity that can own game-state components. */
sealed interface Owner : HasClassName, HasExpression {
  companion object {
    /** Returns the supported runtime Owner represented by [className]. */
    fun fromClassName(className: ClassName): Owner =
        when {
          Player.isValid(className) -> Player(className)
          else -> error("not a supported Owner identity: $className")
        }
  }
}

/** One of the actual people or bots playing the game; both an [Actor] and an [Owner]. */
data class Player(override val className: ClassName) : Actor, Owner {
  init {
    require(isValid(className.toString())) { className }
  }

  override val expression by lazy { className.expression }
  override val expressionFull by ::expression

  override fun toString() = className.toString()

  companion object {
    val regex = Regex("^Player[1-5]$")

    val PLAYER1 = Player(player(1))
    val PLAYER2 = Player(player(2))
    val PLAYER3 = Player(player(3))
    val PLAYER4 = Player(player(4))
    val PLAYER5 = Player(player(5))

    fun players(upTo: Int): List<Player> =
        listOf(PLAYER1, PLAYER2, PLAYER3, PLAYER4, PLAYER5).subList(0, upTo)

    fun isValid(name: String) = name.matches(regex)

    fun isValid(name: ClassName) = isValid(name.toString())

    private fun player(seat: Int) = cn("Player$seat").also { require(seat in 1..5) }
  }
}

private data object EngineActor : Actor {
  override val className = cn("Engine")
  override val expression by lazy { className.expression }
  override val expressionFull by ::expression

  override fun toString() = className.toString()
}
