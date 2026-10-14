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
          doTask("-6 MC")
          doTasks(
              "Ok" /* No Titanium payment. */,
              "NitrogenRichAsteroid FROM ProjectCard",
              "2 TerraformRating",
          )
          doTask("TemperatureStep")
          doTask("PROD[Plant]")
          doTask("TerraformRating")
          doTask("PlayedEvent<Class<NitrogenRichAsteroid>> FROM NitrogenRichAsteroid")
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
      doTasks(
          "GhgImportFromVenus FROM ProjectCard",
      )
      doTask("VenusStep")
      doTasks("PROD[3 Heat]", "TerraformRating")
      // Other optional tasks also accept Ok; identify the MC payment first.
      selectTask("-X? MC")
      doTask("Ok" /* No MC payment. */)
      doTask("Ok" /* No Titanium payment. */)
      doTask("PlayedEvent<Class<GhgImportFromVenus>> FROM GhgImportFromVenus")
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
      doTask("DustSeals FROM ProjectCard")
      doTask("Ok" /* No MC payment. */)
    }
    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<DoubleDown>>",
              "DoubleDown FROM PreludeCard",
          )
          doTasks("CopyPrelude<ExcentricSponsor>", "PlayCard<Class<ProjectCard>, Class<Mine>>")
          // Hand the discount to Admin before the remaining payment is offered.
          selectTask("-25* Owed")
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
          // Other optional tasks also accept Ok; identify the MC payment first.
          selectTask("-X? MC")
          doTask("Ok" /* No MC payment. */)
          doTask("Ok" /* No Steel payment. */)
        }
        .expect("$Mine, PROD[Steel], 0 MC")
  }
}
