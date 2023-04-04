package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames
import dev.martianzoo.tfm.api.SpecialClassNames.player
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.HasExpression

/** A player, or [ENGINE]. */
data class Player(override val className: ClassName) : HasClassName, HasExpression {
  init {
    require(isValid(className.toString())) { className }
  }

  override val expression = className.expr
  override val expressionFull = expression

  override fun toString() = className.toString()

  companion object {
    val regex = Regex("^(${SpecialClassNames.ENGINE}|Player[1-5])$")

    val ENGINE = Player(SpecialClassNames.ENGINE)
    val PLAYER1 = Player(player(1))
    val PLAYER2 = Player(player(2))
    val PLAYER3 = Player(player(3))
    val PLAYER4 = Player(player(4))
    val PLAYER5 = Player(player(5))

    fun players(upTo: Int): List<Player> =
        listOf(PLAYER1, PLAYER2, PLAYER3, PLAYER4, PLAYER5).subList(0, upTo) + ENGINE

    fun isValid(name: String) = name.matches(regex)
  }
}