package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** One of the actual people or bots playing the game. */
data class Player(override val className: ClassName) : Actor() {
  init {
    require(isValid(className.toString())) { className }
  }

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
