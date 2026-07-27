package dev.martianzoo.tfm.engine.cards

import kotlin.test.BeforeTest
import kotlin.test.Test

class PredatorsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    p1.manual("Predators")
    engine.phase("Action")
  }

  @Test
  fun `with an animal on a p2 card, p1 uses Predators`() {
    addBirdForP2()
    p1.cardAction1("Predators").expect("Animal<Predators>, -Animal<Player2, Birds<Player2>>")
  }

  @Test
  fun `with an animal only on Predators, p1 uses its action`() {
    p1.manual("Animal<Predators>")
    p1.cardAction1("Predators").expect("ActionUsedMarker<Predators>")
  }

  private fun addBirdForP2() {
    val p2 = requireP2()
    p2.manual("PROD[2 Plant], Birds") { doTask("PROD[-2 Plant<Player2>]") }
    p2.manual("Animal<Birds>")
  }
}
