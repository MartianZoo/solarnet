package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
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
    val PLAYER1 = Player(cn("Player1"))
    val PLAYER2 = Player(cn("Player2"))
    val PLAYER3 = Player(cn("Player3"))
    val PLAYER4 = Player(cn("Player4"))
    val PLAYER5 = Player(cn("Player5"))

    fun isValid(name: String) = name.matches(regex)
  }
}
