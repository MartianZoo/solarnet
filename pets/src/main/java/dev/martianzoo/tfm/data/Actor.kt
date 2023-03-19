package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.HasExpression

data class Actor(override val className: ClassName) : HasClassName, HasExpression {
  init {
    require(isValid(className.toString())) { className }
  }

  override val expression = className.expr
  override val expressionFull = expression

  override fun toString() = className.toString()

  companion object {
    val regex = Regex("^(${SpecialClassNames.ENGINE}|Player[1-5])$")

    val ENGINE = Actor(SpecialClassNames.ENGINE)
    val PLAYER1 = Actor(cn("Player1"))
    val PLAYER2 = Actor(cn("Player2"))
    val PLAYER3 = Actor(cn("Player3"))
    val PLAYER4 = Actor(cn("Player4"))
    val PLAYER5 = Actor(cn("Player5"))

    fun isValid(name: String) = name.matches(regex)
  }
}
