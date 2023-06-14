package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn

/** A player, or [ENGINE]. */
// TODO shouldn't limit to 5
data class Player(override val className: ClassName) : HasClassName, HasExpression {
  init {
    require(isValid(className.toString())) { className }
  }

  override val expression by className::expression
  override val expressionFull by ::expression

  override fun toString() = className.toString()

  companion object {
    val regex = Regex("^(Engine|Player[1-5])$")

    val ENGINE = Player(cn("Engine"))
    val PLAYER1 = Player(player(1))
    val PLAYER2 = Player(player(2))
    val PLAYER3 = Player(player(3))
    val PLAYER4 = Player(player(4))
    val PLAYER5 = Player(player(5))

    fun players(upTo: Int): List<Player> =
        listOf(PLAYER1, PLAYER2, PLAYER3, PLAYER4, PLAYER5).subList(0, upTo) + ENGINE

    fun isValid(name: String) = name.matches(regex)
    fun isValid(name: ClassName) = isValid(name.toString())

    private fun player(seat: Int) = cn("Player$seat").also { require(seat in 1..5) }
  }
}
