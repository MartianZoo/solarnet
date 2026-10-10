package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class QuickStartVariantTest : TfmSandboxTest() {
  @Test
  internal fun `Quick Start supplies production to all five players`() {
    newTestGame(addOptions = "-CorporateEraExpansion", playerCount = 5)

    players.forEach {
      it.assertProds(
          1 to "MC",
          1 to "Steel",
          1 to "Titanium",
          1 to "Plant",
          1 to "Energy",
          1 to "Heat",
      )
    }
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
    shouldThrow<RequirementException> { kim.claimMilestone(cn("Generalist2")) }
    kim.exMachina("PROD[MC, Steel, Titanium, Plant, Heat]")
    shouldThrow<RequirementException> { kim.claimMilestone(cn("Generalist2")) }

    kim.stdProject("PowerPlantProject")
    kim.claimMilestone(cn("Generalist2")).expect("-8 MC, Generalist2")
  }

  @Test
  internal fun `Without Quick Start Generalist needs only a single production of each resource`() {
    newTestGame(addOptions = "ElysiumMap, -QuickStartVariant")
    kim.exMachina("PROD[MC, Steel, Titanium, Plant, Heat]")
    shouldThrow<RequirementException> { kim.claimMilestone(cn("Generalist")) }

    kim.stdProject("PowerPlantProject")
    kim.claimMilestone(cn("Generalist")).expect("-8 MC, Generalist")
  }
}
