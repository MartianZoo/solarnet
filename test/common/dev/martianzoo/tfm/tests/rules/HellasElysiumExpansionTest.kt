package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class HellasElysiumExpansionTest : TfmSandboxTest() {
  @Test
  internal fun `Specialist uses printed MC production`() {
    newTestGame(addOptions = "ElysiumMap")
    kim.setToExMachina(9, "PROD[MC]")
    shouldThrow<RequirementException> { kim.claimMilestone(cn("Specialist")) }

    kim.stdProject("CityProject") { placeTile(1, 5) }
    kim.claimMilestone(cn("Specialist")).expect("-8 MC, Specialist")
  }
}
