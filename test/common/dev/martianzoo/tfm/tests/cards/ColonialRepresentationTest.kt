package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ColonialRepresentationTest : TfmSandboxTest() {
  @Test
  internal fun `Pays for each owned colony rather than each tile or opponents colonies`() {
    newTestGame("$ColonialRepresentation, TurmoilExpansion, Luna, Io")
    kim.exMachina("2 Colony<Luna>")
    stan.exMachina("Colony<Io>")

    kim.inTurn {
          doTasks(
              *playCardTasks("$ColonialRepresentation"),
              "10 Pay<> FROM MC",
              "6 MC",
          )
        }
        .expect("-4 MC")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Its influence improves a Global Event payout even without colonies`() {
      newTestGame(
          "$ColonialRepresentation, TurmoilExpansion, -QuickStartVariant",
          playerCount = 2,
          firstGlobalEvents =
              listOf("$FreeAcademiaTreaty", "$ExploreFirstDirective", "$MoralMovement"),
      )
      // The earlier event draws are incidental; play the card and resolve the prepared event
      // normally.
      agents.correct(
          "$DemocraticReform, Current<$DemocraticReform>, " +
              "PendingGlobalEffect<Current<$DemocraticReform>, Class<AquiferReleasedByPublicCouncil>>"
      )
      kim.turn {
        doTasks(
            *playCardTasks("$ColonialRepresentation"),
            "10 Pay<> FROM MC",
            "0 MC",
        )
      }
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.doTask("20 MC") }
      kim.doTask("VenusStep! BY Admin")
      kim.count("Influence") shouldBe 1
      kim.count("Colony") shouldBe 0
      kim.doTasks(
          "OceanTile<Tharsis_1_5> BY Admin",
          "Plant",
          "Steel",
      )
      stan.doTasks("0 Plant", "0 Steel")
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    }
  }
}
