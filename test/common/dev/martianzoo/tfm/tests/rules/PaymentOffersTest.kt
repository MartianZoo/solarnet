package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.tfm.tests.TfmSandboxTest
import kotlin.test.Test

internal class PaymentOffersTest : TfmSandboxTest() {
  @Test
  internal fun `A card with both metal tags needs no metal decisions when only another player has metals`() {
    newTestGame()
    stan.exMachina("Steel, Titanium")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpaceElevator>>",
          "-27 MC",
          "SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Steel remains usable when there is no titanium`() {
    newTestGame()
    kim.exMachina("Steel")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpaceElevator>>",
          "-Steel",
          "-25 MC",
          "SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Titanium can pay the whole price without an empty steel decision`() {
    newTestGame()
    kim.exMachina("9 Titanium")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
      doTask("PlayProject<Class<SpaceElevator>>")
      selectTask("-X? MC")
      doTask("Ok")
      doTasks(
          "-9 Titanium",
          "SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
  }
}
