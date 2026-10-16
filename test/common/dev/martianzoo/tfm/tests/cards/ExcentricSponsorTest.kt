package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ExcentricSponsorTest : TfmSandboxTest() {

  private fun initializeGame() {
    newTestGame(addOptions = "PreludeExpansion")
  }

  @Test
  internal fun `Can apply its full discount to the next card`() {
    initializeGame()
    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
              "ExcentricSponsor FROM PreludeCard",
              "PlayCard<Class<ProjectCard>, Class<NitrogenRichAsteroid>>",
          )
          // Hand the discount to Admin before the remaining payment is offered.
          selectTask("-25* Owed")
          doTask("6 Pay<> FROM MC")
          doTasks(
              "NitrogenRichAsteroid FROM ProjectCard",
              "2 TerraformRating",
          )
          doTask("TemperatureStep")
          doTask("PROD[Plant]")
          doTask("TerraformRating")
        }
        .expect("-6 MC, PROD[Plant], 3 TerraformRating")
  }

  @Test
  internal fun `Can play a card costing less than its full discount`() {
    initializeGame()
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
          "ExcentricSponsor FROM PreludeCard",
          "PlayCard<Class<ProjectCard>, Class<GhgImportFromVenus>>",
      )
      // Hand the discount to Admin before the remaining payment is offered.
      selectTask("-25* Owed")
      doTask("Ok") // The discount covers the full cost.
      doTasks(
          "GhgImportFromVenus FROM ProjectCard",
      )
      doTask("VenusStep")
      doTasks("PROD[3 Heat]", "TerraformRating")
    }
  }

  @Test
  internal fun `Double Down copies its project play and discount`() {
    newTestGame(addOptions = "PreludeExpansion")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
          "ExcentricSponsor FROM PreludeCard",
          "PlayCard<Class<ProjectCard>, Class<DustSeals>>",
      )
      // Hand the discount to Admin before the remaining payment is offered.
      selectTask("-25* Owed")
      doTask("Ok") // The discount covers the full cost.
      doTask("DustSeals FROM ProjectCard")
    }
    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<DoubleDown>>",
              "DoubleDown FROM PreludeCard",
          )
          doTasks("CopyPrelude<ExcentricSponsor>", "PlayCard<Class<ProjectCard>, Class<Mine>>")
          // Hand the discount to Admin before the remaining payment is offered.
          selectTask("-25* Owed")
          doTask("Ok") // The discount covers the full cost.
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("$Mine, PROD[Steel], 0 MC")
  }
}
