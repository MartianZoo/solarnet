package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.TurmoilCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class UtopiaInvestTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(TurmoilCardPack)
    p1.playCorp(UtopiaInvest, 0)
    engine.phase("Action")
  }

  @Test
  internal fun `Decreases and gains the same standard resource`() {
    p1.manual("PROD[2 Plant]")

    p1.cardAction1(UtopiaInvest) { doTask("PROD[-Plant]") }.expect("PROD[-Plant], 4 Plant")
  }
}
