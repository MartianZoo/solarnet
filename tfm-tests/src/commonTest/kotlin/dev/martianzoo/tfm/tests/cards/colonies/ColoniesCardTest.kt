package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.CardTest
import kotlin.test.BeforeTest

internal abstract class ColoniesCardTest : CardTest() {
  private val colonyTiles: Set<ClassName> =
      setOf(
              "Luna",
              "Io",
              "Triton",
              "Europa", /*delayed*/
              "Titan",
          )
          .toSetStrict(::cn)

  @BeforeTest
  fun initializeGame() {
    newGame(ColoniesExpansion, colonyTiles = colonyTiles)
    engine.phase("Action")
  }
}
