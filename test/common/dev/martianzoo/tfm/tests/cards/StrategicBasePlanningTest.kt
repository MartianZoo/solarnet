package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StrategicBasePlanningTest : TfmSandboxTest() {
  @Test
  internal fun `Strategic Base Planning must avoid a city bonus requiring a second colony`() {
    newTestGame(
        addOptions = "PreludeExpansion, CimmeriaMap, Luna, Ceres, Triton, Ganymede, Callisto"
    )
    kim.exMachina("Colony<Luna>, Colony<Ceres>, Colony<Triton>, Colony<Ganymede>")
    kim.setToExMachina(8, "MC")
    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$StrategicBasePlanning>>",
            "-3 MC",
            "CityTile<Cimmeria_3_3>",
            "CimmeriaPlacementBonus!",
            "-5 MC",
            "Colony<Callisto>",
            "PROD[Energy]",
        )
        // Resolve the remaining colony offer to expose the exhausted colony choices.
        selectTask("Colony<>")
      }
    }
    kim.count("CityTile") shouldBe 0
    kim.count("Colony") shouldBe 4
    kim.count("MC") shouldBe 8

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$StrategicBasePlanning>>",
          "-3 MC",
          "CityTile<Cimmeria_3_4>",
          "Colony<Callisto>",
          "PROD[Energy]",
      )
    }
  }
}
