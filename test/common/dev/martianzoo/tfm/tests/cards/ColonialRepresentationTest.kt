package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ColonialRepresentationTest : TfmSandboxTest() {
  @Test
  internal fun `Pays for each owned colony rather than each tile or opponents colonies`() {
    newTestGame("ColonialRepresentation, TurmoilExpansion, Luna, Io")
    kim.exMachina("2 Colony<Luna>")
    stan.exMachina("Colony<Io>")

    kim.playProject(ColonialRepresentation, 10).expect("-4 MC")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Its influence improves a Global Event payout even without colonies`() {
      newTestGame("ColonialRepresentation, TurmoilExpansion", playerCount = 2)
      kim.turn { playProject(ColonialRepresentation, 10) }
      stan.pass()
      kim.pass()
      kim.wgt("VenusStep")
      admin.doTask("ExploreFirstDirective")
      kim.buyCards(0)
      stan.buyCards(0)
      stan.pass()
      kim.pass()
      val plantsAfterProduction = kim.count("Plant")
      val steelAfterProduction = kim.count("Steel")

      stan.wgt("VenusStep")
      // Aquifer Released by Public Council grants resources after its neutral ocean.
      stan.doTask("OceanTile<Tharsis_1_5> BY Admin")

      kim.count("Plant") shouldBe plantsAfterProduction + 1
      kim.count("Steel") shouldBe steelAfterProduction + 1
    }
  }
}
