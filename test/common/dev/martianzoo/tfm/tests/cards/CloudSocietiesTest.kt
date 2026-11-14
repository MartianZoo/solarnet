package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CloudSocietiesTest : TfmGameplayTest() {
  @BeforeTest
  fun prepareCurrentEvent() {
    newTestGame(
        addOptions = "TurmoilExpansion",
        playerCount = 2,
        firstGlobalEvents = listOf("TitanVenusAlliance", "RedResistance", "ScientificProgress"),
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `Players choose their own floater cards after every floater card receives a resource`() {
    // Both players have a genuine choice of recipient and influence from Event Analysts or the
    // chair.
    kim.exMachina("EventAnalysts, AerialMappers, FloatingHabs")
    stan.exMachina("Chairman FROM Chairman<Neutral>, TitanShuttles, DeuteriumExport")
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("Floater<FloatingHabs>").expect("Floater<FloatingHabs>, 0 Floater<AerialMappers>")
    stan
        .doTask("Floater<TitanShuttles>")
        .expect("Floater<TitanShuttles>, 0 Floater<DeuteriumExport>")
    kim.count("Floater<FloatingHabs>") shouldBe 2
    kim.count("Floater<AerialMappers>") shouldBe 1
    stan.count("Floater<TitanShuttles>") shouldBe 2
    stan.count("Floater<DeuteriumExport>") shouldBe 1
  }
}
