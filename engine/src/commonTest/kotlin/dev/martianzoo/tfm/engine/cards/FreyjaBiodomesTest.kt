package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class FreyjaBiodomesTest : CardTest() {
  // FAQ: "you can still choose to take microbes"
  @Test
  fun `Freyja Biodomes can choose microbes with no eligible Venus card`() {
    val p1 = newGame("BMV", 2).tfm(PLAYER1)
    p1.sneak("PROD[Energy]")
    p1.manual("VenusianAnimals")
    p1.assertCounts(1 to "VenusTag<VenusianAnimals>", 1 to "Animal<VenusianAnimals>")

    p1.manual("FreyjaBiodomes") { doTask("Ok") }.expect("PROD[-Energy, 2 Megacredit]")

    p1.assertCounts(1 to "Animal<VenusianAnimals>")
    p1.assertProds(0 to "Energy", 2 to "Megacredit")
  }
}
