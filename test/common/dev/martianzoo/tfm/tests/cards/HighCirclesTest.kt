package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HighCirclesTest : TfmSandboxTest() {
  @Test
  internal fun `Its delegates enable a Scientists project through Excentric Sponsor during Preludes`() {
    newTestGame("PreludeExpansion, $HighCircles, TurmoilExpansion", playerCount = 2)
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$HighCircles>>",
          "$HighCircles FROM PreludeCard",
          "TerraformRating",
          "SearchForCard<ReferenceFilter<Class<Ruling>>>!",
          "PartyLeader<Scientists>",
          "PartyFollower<Scientists>",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$ExcentricSponsor>>",
              "$ExcentricSponsor FROM PreludeCard",
              "PlayCard<Class<ProjectCard>, Class<$SupportedResearch>>",
          )
          // Hand the discount to Admin before the remaining payment is offered.
          selectTask("-25* Owed")
          doTask("Ok") // The discount covers the full cost.
          doTasks(
              "$SupportedResearch FROM ProjectCard",
              "2 ProjectCard<Hand>",
          )
        }
        .expect("ProjectCard")
  }

  @Test
  internal fun `Its delegates enable a Scientists project through Ecology Experts during Preludes`() {
    newTestGame(
        "PreludeExpansion, $HighCircles, TurmoilExpansion, $EcologyExperts, Unsafe",
        playerCount = 2,
    )
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$HighCircles>>",
          "$HighCircles FROM PreludeCard",
          "TerraformRating",
          "SearchForCard<ReferenceFilter<Class<Ruling>>>!",
          "PartyLeader<Scientists>",
          "PartyFollower<Scientists>",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
              "$EcologyExperts FROM PreludeCard",
              "PROD[Plant]",
              "PlayCard<Class<ProjectCard>, Class<$SupportedResearch>>",
          )
          // Execute the requirement waiver before playing the project.
          doTasks(
              "-Required* / Required",
              "3 Pay<> FROM MC",
              "$SupportedResearch FROM ProjectCard",
              "2 ProjectCard<Hand>",
          )
        }
        .expect("ProjectCard")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Its influence adds to the chairmans Global Event payout`() {
      newTestGame(
          "$HighCircles, TurmoilExpansion, -QuickStartVariant, -ColoniesExpansion",
          playerCount = 2,
          firstGlobalEvents =
              listOf("$FreeAcademiaTreaty", "$ExploreFirstDirective", "$MoralMovement"),
      )
      // The card's play and the earlier election are incidental to its continuing influence.
      kim.exMachina("$HighCircles, Chairman<Kim> FROM Chairman<Neutral>")
      agents.correct(
          "$DemocraticReform, Current<$DemocraticReform>, " +
              "PendingGlobalEffect<Current<$DemocraticReform>, Class<AquiferReleasedByPublicCouncil>>"
      )
      kim.doTask("Pass")
      stan.doTask("Pass")
      players.forEach { it.doTask("20 MC") }
      kim.doTask("VenusStep! BY Admin")
      kim.count("Influence") shouldBe 2
      stan.count("Influence") shouldBe 0
      kim.doTasks(
          "OceanTile<Tharsis_1_5> BY Admin",
          "2 Plant",
          "2 Steel",
      )
      stan.doTasks("0 Plant", "0 Steel")
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    }
  }
}
