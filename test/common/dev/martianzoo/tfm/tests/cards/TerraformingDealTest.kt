package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TerraformingDealTest : TfmGameplayTest() {
  @Test
  internal fun `Pays for both TR gained from a single card`() {
    newTestGame("PreludeExpansion, TerraformingDeal", playerCount = 2)
    kim.playPrelude(TerraformingDeal)
    kim.playPrelude(Donation)
    stan.playPrelude(Supplier)
    stan.playPrelude(MetalsCompany)

    kim.playProject(BribedCommittee, 7).expect("2 TerraformRating, -3 MC")
  }

  @Test
  internal fun `Pays for becoming chairman after production`() {
    newTestGame(
        "PreludeExpansion, TerraformingDeal, HighCircles, TurmoilExpansion",
        playerCount = 2,
    )
    kim.playPrelude(TerraformingDeal)
    kim.playPrelude(HighCircles) { doTask("2 PartyDelegate<Scientists>") }
    stan.playPrelude(Supplier)
    stan.playPrelude(MetalsCompany)
    kim.pass()
    stan.pass()
    admin.count("VenusSolarPhase") shouldBe 1
    val moneyAfterProduction = kim.count("MC")
    val ratingBeforeRevision = kim.count("TerraformRating")

    kim.wgt("VenusStep")

    kim.count("Chairman") shouldBe 1
    kim.count("TerraformRating") shouldBe ratingBeforeRevision
    kim.count("MC") shouldBe moneyAfterProduction + 2
  }
}
