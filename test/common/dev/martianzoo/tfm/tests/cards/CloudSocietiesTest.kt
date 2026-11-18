package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CloudSocietiesTest : TfmGameplayTest(NONE) {
  @BeforeTest
  fun prepareCurrentEvent() {
    newTestGame(
        addOptions = "-QuickStartVariant, TurmoilExpansion",
        playerCount = 2,
        firstGlobalEvents = listOf("TitanVenusAlliance", "RedResistance", "ScientificProgress"),
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `Players choose their own floater cards after every floater card receives a resource`() {
    // Both players have a genuine choice of recipient and influence from Event Analysts or the
    // chair.
    kim.exMachina("EventAnalysts, AerialMappers, FloatingHabs")
    stan.exMachina("Chairman FROM Chairman<Neutral>, TitanShuttles, DeuteriumExport")
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("19 MC") }
    stan.doTask("VenusStep! BY Admin")
    // Open the mandatory gains to every floater card before the players choose their extras.
    stan.doTask("EACH")
    kim.doTask("Floater<FloatingHabs>").expect("0 Floater<AerialMappers>")
    stan.doTask("Floater<TitanShuttles>").expect("0 Floater<DeuteriumExport>")
    kim.count("Floater<FloatingHabs>") shouldBe 2
    kim.count("Floater<AerialMappers>") shouldBe 1
    stan.count("Floater<TitanShuttles>") shouldBe 2
    stan.count("Floater<DeuteriumExport>") shouldBe 1
    players.forEach { it.doTask("4 ProjectCard<Selecting>") }
  }
}
