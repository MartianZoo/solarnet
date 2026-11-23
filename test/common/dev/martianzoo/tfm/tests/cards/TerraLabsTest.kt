package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TerraLabsTest : TfmGameplayTest() {
  @Test
  internal fun `Buys all four research cards for four MC`() {
    newTestGame(
        addOptions = "$TerraLabsResearch",
        playerCount = 2,
        kimCorporation = TerraLabsResearch,
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("20 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTask("4 ProjectCard<Selecting>") }

    kim.doTasks(
            "Ok", // Keep all four research cards.
            "4 Pay<> FROM MC",
            "4 ProjectCard<Hand FROM Selecting>",
        )
        .expect("4 ProjectCard")
    stan.doTask("-4 ProjectCard<Selecting>")
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Acquiring Polyphemos cancels the card-purchase discount`() {
    newTestGame(
        addOptions = "PreludeExpansion, $TerraLabsResearch",
        playerCount = 2,
        kimCorporation = TerraLabsResearch,
    )
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "4 CorporationCard<Selecting>",
          "PlayCard<Class<CorporationCard>, Class<$Polyphemos>>",
          "50 MC",
          "5 Titanium",
          "PROD[5 MC]",
          "-42 MC",
      )
    }
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Donation>>",
        "21 MC",
    )
    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$SupplyDrop>>",
        "3 Titanium",
        "8 Steel",
        "3 Plant",
    )

    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$PowerGeneration>>",
        "PROD[3 Energy]",
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("25 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "4 Energy", "Heat")
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTask("4 ProjectCard<Selecting>") }

    kim.doTasks(
            "Ok", // Keep all four research cards.
            "12 Pay<> FROM MC",
            "4 ProjectCard<Hand FROM Selecting>",
        )
        .expect("4 ProjectCard")
    stan.doTask("-4 ProjectCard<Selecting>")
    admin.count("ActionPhase") shouldBe 1
  }
}
