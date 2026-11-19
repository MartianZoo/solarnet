package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class VolcanicEruptionsTest : TfmGameplayTest() {
  @BeforeTest
  fun prepareCurrentEvent() {
    newTestGame(
        addOptions = "-QuickStartVariant, TurmoilExpansion",
        playerCount = 2,
        firstGlobalEvents = listOf("$PateraBoring", "$BioengineeringBoom", "$ScientificProgress"),
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `First player chooses the threshold ocean location without earning terraforming credit`() {
    // The event's two temperature steps will cross the ocean threshold.
    admin.exMachina("13 TemperatureStep")
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("19 MC") }
    stan.doTask("VenusStep! BY Admin")
    stan
        .doTask("OceanTile<Tharsis_1_5> BY Admin")
        .expect("0 TerraformRating<Kim>, 0 TerraformRating<Stan>, 0 Plant")
    admin.count("TemperatureStep") shouldBe 15
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 0
    players.forEach { it.doTask("PROD[0 Heat]") }
    players.forEach { it.doTask("4 ProjectCard<Selecting>") }
  }
}
