package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DemocraticReformTest : TfmGameplayTest(NONE) {
  @BeforeTest
  fun prepareCurrentEvent() {
    newTestGame(
        addOptions = "-QuickStartVariant, TurmoilExpansion",
        playerCount = 2,
        firstGlobalEvents = listOf("DemocraticReform", "BioengineeringBoom", "ScientificProgress"),
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `First player chooses the ocean location without earning TR or placement resources`() {
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.doTask("19 MC") }
    stan.doTask("VenusStep! BY Admin")
    stan
        .doTask("OceanTile<Tharsis_1_2> BY Admin")
        .expect("0 TerraformRating<Kim>, 0 TerraformRating<Stan>, 0 Plant, 0 ProjectCard")
    admin.count("OceanTile<Tharsis_1_5>") shouldBe 0
    players.forEach { it.doTasks("0 Plant", "0 Steel") }
    players.forEach { it.doTask("4 ProjectCard<Selecting>") }
  }
}
