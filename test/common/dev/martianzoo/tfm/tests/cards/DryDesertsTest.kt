package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DryDesertsTest : TfmGameplayTest() {
  @BeforeTest
  fun prepareCurrentEvent() {
    newTestGame(
        addOptions = "-QuickStartVariant, TurmoilExpansion",
        playerCount = 2,
        firstGlobalEvents = listOf("$MinimalImpactPolicy", "$RedResistance", "$ScientificProgress"),
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  fun `First player chooses which ocean disappears and each influence buys a separate resource`() {
    // Two oceans make removal a choice; Kim has two influence from Event Analysts and the chair.
    admin.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_5>")
    kim.exMachina("$EventAnalysts, Chairman FROM Chairman<Neutral>")
    stan.doTask("Pass")
    kim.doTask("Pass")
    // The Reds' first government restored both players' rating after Turmoil's reduction.
    players.forEach { it.doTask("20 MC") }
    stan.doTask("VenusStep! BY Admin")
    stan.doTask("-OceanTile<Tharsis_1_5>").expect("0 TerraformRating")
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 1
    admin.count("OceanTile<Tharsis_1_5>") shouldBe 0
    kim.doTask("EACH")
    kim.doTasks("Heat", "Plant").expect("0 Steel")
    stan.doTask("EACH") // No influence rewards for Stan.
    players.forEach { it.doTask("4 ProjectCard<Selecting>") }
  }
}
