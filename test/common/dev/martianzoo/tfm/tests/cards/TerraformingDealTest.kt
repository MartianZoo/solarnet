package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TerraformingDealTest : TfmGameplayTest() {
  @Test
  internal fun `Pays for both TR gained from a single card`() {
    newTestGame("PreludeExpansion, $TerraformingDeal", playerCount = 2)
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>",
    )
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Donation>>",
        "21 MC",
    )
    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
        "PROD[2 Energy]",
        "4 Steel",
    )

    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
        "PROD[MC]",
        "PROD[Steel]",
        "PROD[Titanium]",
    )

    kim.completeOperation {
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "7 Pay<> FROM MC",
          "2 TerraformRating",
          "4 MC",
      )
    }
  }

  @Test
  internal fun `Pays for becoming chairman after production`() {
    newTestGame(
        "PreludeExpansion, $TerraformingDeal, $HighCircles, TurmoilExpansion",
        playerCount = 2,
    )
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>",
    )
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$HighCircles>>",
          "TerraformRating",
          "2 MC",
          "SearchForCard<ReferenceFilter<Class<Ruling>>>!",
          "PartyLeader<Scientists>",
          "PartyFollower<Scientists>",
      )
    }
    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
        "PROD[2 Energy]",
        "4 Steel",
    )

    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
        "PROD[MC]",
        "PROD[Steel]",
        "PROD[Titanium]",
    )

    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("22 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("22 MC", "2 Steel", "2 Titanium", "Plant", "3 Energy", "Heat")
    admin.count("SolarPhase") shouldBe 1
    expectChanges("0 TerraformRating<Kim>") {
      kim.doTasks(
          "VenusStep! BY Admin",
          "2 MC",
      )
    }
    kim.count("Chairman") shouldBe 1
  }
}
