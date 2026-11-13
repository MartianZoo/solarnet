package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TerraformingDealTest : TfmGameplayTest(NONE) {
  @Test
  internal fun `Pays for both TR gained from a single card`() {
    newTestGame("PreludeExpansion, TerraformingDeal", playerCount = 2)
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>",
        "$TerraformingDeal FROM PreludeCard",
    )
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Donation>>",
        "$Donation FROM PreludeCard",
        "21 MC",
    )
    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
        "$Supplier FROM PreludeCard",
        "PROD[2 Energy]",
        "4 Steel",
    )

    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
        "$MetalsCompany FROM PreludeCard",
        "PROD[MC]",
        "PROD[Steel]",
        "PROD[Titanium]",
    )

    kim.completeOperation {
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "7 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
          "4 MC",
      )
    }
  }

  @Test
  internal fun `Pays for becoming chairman after production`() {
    newTestGame(
        "PreludeExpansion, TerraformingDeal, HighCircles, TurmoilExpansion",
        playerCount = 2,
    )
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>",
        "$TerraformingDeal FROM PreludeCard",
    )
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$HighCircles>>",
          "$HighCircles FROM PreludeCard",
          "TerraformRating",
          "2 MC",
      )
      doTask("SearchForCard<Kim, ReferenceFilter<Class<Ruling>>>!")
      doTasks("PartyLeader<Scientists>", "PartyFollower<Scientists>")
    }
    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
        "$Supplier FROM PreludeCard",
        "PROD[2 Energy]",
        "4 Steel",
    )

    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
        "$MetalsCompany FROM PreludeCard",
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
      kim.doTask("VenusStep! BY Admin")
      kim.doTask("2 MC")
    }
    kim.count("Chairman") shouldBe 1
  }
}
