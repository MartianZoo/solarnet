package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ExcentricSponsorTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame(addOptions = "PreludeExpansion")
  }

  @Test
  internal fun `Can apply its full discount to the next card`() {
    with(kim) {
      inTurn {
            doTasks(
                "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
                "ExcentricSponsor FROM PreludeCard",
                "PlayCard<Class<ProjectCard>, Class<NitrogenRichAsteroid>>",
            )
            selectTask("-25* Owed")
            doTask("-6 MC")
            selectTask("-X? Titanium")
            doTasks("Ok", "NitrogenRichAsteroid FROM ProjectCard")
            repeat(2) { doTask("TerraformRating") }
            selectTask("TemperatureStep*")
            selectTask("PROD[Plant] OR (3 PlantTag: PROD[4 Plant])")
            doTask("TerraformRating")
          }
          .expect("-6 MC, PROD[Plant], 3 TerraformRating")
    }
  }

  @Test
  internal fun `Can play a card costing less than its full discount`() {
    with(kim) {
      inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
            "ExcentricSponsor FROM PreludeCard",
            "PlayCard<Class<ProjectCard>, Class<GhgImportFromVenus>>",
        )
        selectTask("-25* Owed")
        doTask("GhgImportFromVenus FROM ProjectCard")
        selectTask("VenusStep*")
        doTasks("PROD[3 Heat]", "TerraformRating")
        selectTask("-X? MC")
        doTask("Ok")
        selectTask("-X? Titanium")
        doTask("Ok")
      }
    }
  }

  @Test
  internal fun `Double Down copies its project play and discount`() {
    newTestGame(addOptions = "PreludeExpansion")

    with(kim) {
      inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
            "ExcentricSponsor FROM PreludeCard",
            "PlayCard<Class<ProjectCard>, Class<DustSeals>>",
        )
        selectTask("-25* Owed")
        doTask("DustSeals FROM ProjectCard")
        selectTask("-X? MC")
        doTask("Ok")
      }
      inTurn {
            doTasks(
                "PlayCard<Class<PreludeCard>, Class<DoubleDown>>",
                "DoubleDown FROM PreludeCard",
            )
            selectOnlyTask()
            doTasks("CopyPrelude<ExcentricSponsor>", "PlayCard<Class<ProjectCard>, Class<Mine>>")
            selectTask("-25* Owed")
            doTasks("Mine FROM ProjectCard", "PROD[Steel]")
            selectTask("-X? MC")
            doTask("Ok")
            selectTask("-X? Steel")
            doTask("Ok")
          }
          .expect("$Mine, PROD[Steel], 0 MC")
    }
  }
}
