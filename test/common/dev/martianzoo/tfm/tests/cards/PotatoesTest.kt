package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PotatoesTest : TfmSandboxTest() {
  @Test
  internal fun `Viral Enhancers can supply the second plant before Potatoes loses two`() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")
    kim.setToExMachina(2, "MC")
    kim.setToExMachina(1, "Plant")

    kim.playProject(Potatoes, 2) { doTask("Plant") }.expect("-Plant, PROD[2 MC]")
  }

  @Test
  internal fun `Plant loss does not count as a requirement for Tactician`() {
    newTestGame(addOptions = "Tactician, Landlord, Banker")
    kim.exMachina("$Archaebacteria, $DustSeals, $SearchForLife, $Potatoes")

    shouldThrow<RequirementException> { kim.claimMilestone(cn("Tactician")) }
  }
}
