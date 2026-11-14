package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CorrosiveRainTest : TfmGameplayTest() {
  @BeforeTest
  fun prepareCurrentEvent() {
    newTestGame(
        addOptions = "TurmoilExpansion",
        playerCount = 2,
        firstGlobalEvents = listOf("ImmigrationToVenus", "RedResistance", "ScientificProgress"),
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `Player can lose money while retaining enough floaters to pay instead`() {
    kim.exMachina("EventAnalysts, AerialMappers, 2 Floater<AerialMappers>")
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("-10 MC").expect("-10 MC, 0 Floater, ProjectCard")
    kim.count("Floater<AerialMappers>") shouldBe 2
  }

  @Test
  fun `Player chooses which eligible card loses two floaters instead of money`() {
    kim.exMachina(
        "EventAnalysts, AerialMappers, FloatingHabs, 2 Floater<AerialMappers>, 3 Floater<FloatingHabs>"
    )
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("-2 Floater<FloatingHabs>").expect("-2 Floater<FloatingHabs>, 0 MC, ProjectCard")
    kim.count("Floater<AerialMappers>") shouldBe 2
    kim.count("Floater<FloatingHabs>") shouldBe 1
  }
}
