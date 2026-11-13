package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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

  @Test
  internal fun `A wild science tag meets the sponsored cards requirement during Prelude`() {
    newTestGame("PreludeExpansion", kimCorporation = Inventrix)
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
            "ExcentricSponsor FROM PreludeCard",
            "PlayCard<Class<ProjectCard>, Class<FloatingHabs>>",
        )
      }
    }
    kim.exMachina("ResearchNetwork")
    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
              "ExcentricSponsor FROM PreludeCard",
              "PlayCard<Class<ProjectCard>, Class<FloatingHabs>>",
          )
          kim.count("ScienceTag<WildTagElection<WildTag<ResearchNetwork>>>") shouldBe 1
          // Hand the discount to Admin before resolving the covered bill.
          selectTask("-25* Owed")
          doTasks("Ok", "FloatingHabs FROM ProjectCard")
        }
        .expect("FloatingHabs, 0 MC")
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }
}
