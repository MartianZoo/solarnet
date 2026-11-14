package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DryDesertsTest : TfmGameplayTest() {
  @BeforeTest
  fun prepareCurrentEvent() {
    newTestGame(
        addOptions = "TurmoilExpansion",
        playerCount = 2,
        firstGlobalEvents = listOf("MinimalImpactPolicy", "RedResistance", "ScientificProgress"),
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `First player chooses which ocean disappears and each influence buys a separate resource`() {
    // Two oceans make removal a choice; Kim has two influence from Event Analysts and the chair.
    admin.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_5>")
    kim.exMachina("EventAnalysts, Chairman FROM Chairman<Neutral>")
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    stan.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
    stan.doTask("-OceanTile<Tharsis_1_5>").expect("0 TerraformRating")
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 1
    admin.count("OceanTile<Tharsis_1_5>") shouldBe 0
    kim.doTasks("Heat", "Plant").expect("Heat, Plant, 0 Steel")
  }
}
