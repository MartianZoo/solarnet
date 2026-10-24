package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class QuickStartVariantTest : TfmSandboxTest() {
  private fun assertQuickStartProduction(seat: Int) {
    players[seat].assertProds(
        1 to "MC",
        1 to "Steel",
        1 to "Titanium",
        1 to "Plant",
        1 to "Energy",
        1 to "Heat",
    )
  }

  @Test
  internal fun `Quick Start supplies production to all five players`() {
    newTestGame(addOptions = "-CorporateEraExpansion", playerCount = 5)

    assertQuickStartProduction(0)
    assertQuickStartProduction(1)
    assertQuickStartProduction(2)
    assertQuickStartProduction(3)
    assertQuickStartProduction(4)
  }

  @Test
  internal fun `Quick Start works alongside Corporate Era`() {
    newTestGame()
    kim.assertProds(
        1 to "MC",
        1 to "Steel",
        1 to "Titanium",
        1 to "Plant",
        1 to "Energy",
        1 to "Heat",
    )
  }

  @Test
  internal fun `Quick Start can be disabled independently of Corporate Era`() {
    newTestGame(addOptions = "-CorporateEraExpansion, -QuickStartVariant")
    kim.assertProds(
        0 to "MC",
        0 to "Steel",
        0 to "Titanium",
        0 to "Plant",
        0 to "Energy",
        0 to "Heat",
    )
  }

  @Test
  internal fun `Quick Start's Generalist requires two production of every resource`() {
    newTestGame(addOptions = "ElysiumMap")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("UseAction<ClaimMilestoneAction, Action1>")
        doTask("8 Pay<> FROM MC")
        doTask("Generalist2")
      }
    }
    kim.exMachina("PROD[MC, Steel, Titanium, Plant, Heat]")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("UseAction<ClaimMilestoneAction, Action1>")
        doTask("8 Pay<> FROM MC")
        doTask("Generalist2")
      }
    }

    kim.inTurn {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC")
      doTask("Generalist2")
    }
  }

  @Test
  internal fun `Without Quick Start Generalist needs only a single production of each resource`() {
    newTestGame(addOptions = "ElysiumMap, -QuickStartVariant")
    kim.exMachina("PROD[MC, Steel, Titanium, Plant, Heat]")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTask("UseAction<ClaimMilestoneAction, Action1>")
        doTask("8 Pay<> FROM MC")
        doTask("Generalist")
      }
    }

    kim.inTurn {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC")
      doTask("Generalist")
    }
  }
}
