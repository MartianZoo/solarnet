package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class LavaFlowsTest : TfmSandboxTest() {
  @Test
  internal fun `Can place its tile on Tharsis`() {
    newTestGame()
    kim.setToExMachina(18, "MC")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<LavaFlows>>",
          "18 Pay<> FROM MC",
          "LavaFlows FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTask("TemperatureStep")
      doTasks(
          "TerraformRating",
          "TerraformRating",
          "LavaFlows_SpecialTile<Tharsis_2_2>",
          "Steel",
      )
      doTask("PlayedEvent<Class<$LavaFlows>> FROM $LavaFlows")
    }
  }

  @Test
  internal fun `Can place its tile on Hellas`() {
    newTestGame(addOptions = "HellasMap")
    kim.setToExMachina(18, "MC")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<LavaFlows>>",
          "18 Pay<> FROM MC",
          "LavaFlows FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTask("TemperatureStep")
      doTasks(
          "TerraformRating",
          "TerraformRating",
          "LavaFlows_SpecialTile<Hellas_1_5>",
          "Plant",
      )
      doTask("PlayedEvent<Class<$LavaFlows>> FROM $LavaFlows")
    }
  }

  @Test
  internal fun `An occupied volcanic placement rolls back the card and temperature increase`() {
    newTestGame()
    kim.setToExMachina(18, "MC")
    kim.exMachina(
        "GreeneryTile<Tharsis_2_2>, GreeneryTile<Tharsis_3_1>, " +
            "GreeneryTile<Tharsis_4_1>, GreeneryTile<Tharsis_5_1>"
    )
    kim.setToExMachina(1, "ProjectCard")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<LavaFlows>>")

        doTasks(
            "18 Pay<> FROM MC",
            "LavaFlows FROM ProjectCard",
        )
        doTask("TemperatureStep")
        doTask("TemperatureStep")
        doTask("TerraformRating")
        doTask("TerraformRating")
        doTask("LavaFlows_SpecialTile<Tharsis_2_2>")
      }
    }

    kim.count("MC") shouldBe 18
    kim.count("ProjectCard") shouldBe 1
    kim.count("Tile<Tharsis_2_2>") shouldBe 1
    kim.count("$LavaFlows") shouldBe 0
    kim.count("TemperatureStep") shouldBe 0
  }
}
