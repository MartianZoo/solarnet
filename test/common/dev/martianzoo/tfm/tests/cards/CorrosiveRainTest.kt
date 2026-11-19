package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CorrosiveRainTest : TfmGameplayTest() {
  @BeforeTest
  fun prepareCurrentEvent() {
    newTestGame(
        addOptions = "-QuickStartVariant, TurmoilExpansion",
        playerCount = 2,
        firstGlobalEvents = listOf("$ImmigrationToVenus", "$RedResistance", "$ScientificProgress"),
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `Player can lose money while retaining enough floaters to pay instead`() {
    kim.exMachina("$EventAnalysts, $AerialMappers, 2 Floater<$AerialMappers>")
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("19 MC") }
    stan.doTask("VenusStep! BY Admin")
    kim.doTasks("-10 MC", "ProjectCard").expect("0 Floater")
    stan.doTasks("-10 MC", "0 ProjectCard")
    kim.count("Floater<$AerialMappers>") shouldBe 2
    players.forEach { it.doTask("4 ProjectCard<Selecting>") }
  }

  @Test
  fun `Player chooses which eligible card loses two floaters instead of money`() {
    kim.exMachina(
        "$EventAnalysts, $AerialMappers, $FloatingHabs, 2 Floater<$AerialMappers>, 3 Floater<$FloatingHabs>"
    )
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("19 MC") }
    stan.doTask("VenusStep! BY Admin")
    kim.doTasks("-2 Floater<$FloatingHabs>", "ProjectCard").expect("0 MC")
    stan.doTasks("-10 MC", "0 ProjectCard")
    kim.count("Floater<$AerialMappers>") shouldBe 2
    kim.count("Floater<$FloatingHabs>") shouldBe 1
    players.forEach { it.doTask("4 ProjectCard<Selecting>") }
  }
}
