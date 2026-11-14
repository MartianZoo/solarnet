package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DemocraticReformTest : TfmGameplayTest() {
  @BeforeTest
  fun prepareCurrentEvent() {
    newTestGame(
        addOptions = "TurmoilExpansion",
        playerCount = 2,
        firstGlobalEvents = listOf("DemocraticReform", "BioengineeringBoom", "ScientificProgress"),
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `First player chooses the ocean location without earning TR or placement resources`() {
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    stan
        .doTask("OceanTile<Tharsis_1_2> BY Admin")
        .expect(
            "OceanTile<Tharsis_1_2>, 0 TerraformRating<Kim>, 0 TerraformRating<Stan>, 0 Plant, 0 ProjectCard"
        )
    admin.count("OceanTile<Tharsis_1_5>") shouldBe 0
  }
}
