package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EventAnalystsTest : TfmGameplayTest() {
  @Test
  internal fun `Influence stacks with High Circles and Colonial Representation beyond four`() {
    newTestGame(
        "$HighCircles, TurmoilExpansion, $ColonialRepresentation, Luna, -QuickStartVariant",
        playerCount = 2,
        firstGlobalEvents =
            listOf("$FreeAcademiaTreaty", "$ExploreFirstDirective", "$MoralMovement"),
    )
    // Prepare the six independent sources; earlier purchases and elections are incidental.
    kim.exMachina(
        "$EventAnalysts, $HighCircles, $ColonialRepresentation, Colony<Luna>, " +
            "Chairman<Kim> FROM Chairman<Neutral>, " +
            "PartyLeader<Scientists> FROM PartyLeader<Scientists, Neutral>, 3 PartyFollower<Scientists>"
    )
    agents.correct(
        "$DemocraticReform, Current<$DemocraticReform>, " +
            "PendingGlobalEffect<Current<$DemocraticReform>, Class<AquiferReleasedByPublicCouncil>>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    kim.count("Influence") shouldBe 6
    stan.count("Influence") shouldBe 0
    kim.doTasks(
        "OceanTile<Tharsis_1_5> BY Admin",
        "6 Steel",
        "6 Plant",
    )
    stan.doTasks("0 Steel", "0 Plant")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }
}
