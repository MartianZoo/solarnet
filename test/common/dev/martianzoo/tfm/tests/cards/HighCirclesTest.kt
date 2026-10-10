package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HighCirclesTest : TfmSandboxTest() {
  @Test
  internal fun `Its delegates enable a Scientists project through Excentric Sponsor during Preludes`() {
    newTestGame("PreludeExpansion, HighCircles, TurmoilExpansion", playerCount = 2)
    kim.playPrelude(HighCircles) { doTask("2 PartyDelegate<Scientists>") }

    with(kim) { playPrelude(ExcentricSponsor) { playProject(SupportedResearch, 0) } }
        .expect("ProjectCard, $SupportedResearch")
  }

  @Test
  internal fun `Its delegates enable a Scientists project through Ecology Experts during Preludes`() {
    newTestGame(
        "PreludeExpansion, HighCircles, TurmoilExpansion, EcologyExperts, Unsafe",
        playerCount = 2,
    )
    kim.playPrelude(HighCircles) { doTask("2 PartyDelegate<Scientists>") }

    with(kim) { playPrelude(EcologyExperts) { playProject(SupportedResearch, 3) } }
        .expect("ProjectCard, $SupportedResearch")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Its influence adds to the chairmans Global Event payout`() {
      newTestGame("PreludeExpansion, HighCircles, TurmoilExpansion", playerCount = 2)
      kim.playPrelude(HighCircles) { doTask("2 PartyDelegate<Scientists>") }
      kim.playPrelude(Donation)
      stan.playPrelude(Supplier)
      stan.playPrelude(MetalsCompany)
      kim.pass()
      stan.pass()
      kim.wgt("VenusStep")
      kim.count("Chairman") shouldBe 1
      admin.doTask("ExploreFirstDirective")
      kim.buyCards(0)
      stan.buyCards(0)
      stan.pass()
      kim.pass()
      val plantsAfterProduction = kim.count("Plant")
      val steelAfterProduction = kim.count("Steel")

      stan.wgt("VenusStep")
      // Aquifer Released by Public Council pays for both the card and chairman influence.
      stan.doTask("OceanTile<Tharsis_1_5> BY Admin")

      kim.count("Plant") shouldBe plantsAfterProduction + 2
      kim.count("Steel") shouldBe steelAfterProduction + 2
    }
  }
}
