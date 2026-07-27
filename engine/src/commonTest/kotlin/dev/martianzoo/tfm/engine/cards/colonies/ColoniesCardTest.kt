package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.cards.CardTest
import dev.martianzoo.util.toSetStrict
import kotlin.test.BeforeTest

abstract class ColoniesCardTest : CardTest() {
  protected val colonyTiles: Set<ClassName> =
      setOf("Luna", "Io", "Triton", "Europa", /*delayed*/ "Titan").toSetStrict(::cn)

  @BeforeTest
  fun initializeGame() {
    newGame("BMC", colonyTiles = colonyTiles)
    engine.phase("Action")
  }
}
