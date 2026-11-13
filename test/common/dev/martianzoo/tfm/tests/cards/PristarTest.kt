package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PristarTest : TfmGameplayTest(NONE) {
  @BeforeTest
  fun initializeGame() {
    newTestGame(addOptions = "Pristar", playerCount = 2, kimCorporation = Pristar)
  }

  @Test
  internal fun `Pays its production bonus when TR did not increase`() {
    kim.doTask("Pass")
    expectChanges("25 MC<Kim>, Preservation<Pristar<Kim>>") {
      stan.doTask("Pass")
      // 19 MC from production and TR, plus Pristar's 6 MC bonus.
      kim.doTasks("25 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
      kim.doTask("Preservation<Pristar>")
      stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    }
    admin.count("SolarPhase") shouldBe 1
  }

  @Test
  internal fun `An opponent's TR increase does not suppress the production bonus`() {
    kim.doTask("Pass")
    stan.completeOperation {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    stan.doTask("Pass")
    // 19 MC from production and TR, plus Pristar's 6 MC bonus.
    kim.doTasks("25 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    kim.doTask("Preservation<Pristar>")
    stan.doTasks("22 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")

    admin.count("SolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 1
  }

  @Test
  internal fun `Does not pay its production bonus after a TR increase`() {
    kim.completeOperation {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    kim.selectTask("PassBaton") // Decline the second action.
    stan.doTask("Pass")

    kim.doTask("Pass")
    kim.doTasks("20 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")

    admin.count("SolarPhase") shouldBe 1
    kim.count("Preservation<$Pristar>") shouldBe 0
  }
}
