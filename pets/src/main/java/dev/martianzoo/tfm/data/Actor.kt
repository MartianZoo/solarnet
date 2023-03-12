package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.HasClassName

data class Actor(override val className: ClassName) : HasClassName {
  init {
    require(className == GAME || className.toString().startsWith("Player"))
  }

  override fun toString() = className.toString()

  companion object {
    val ENGINE = Actor(GAME)
  }
}
