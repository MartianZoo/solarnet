package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.EnergyMarket
import dev.martianzoo.tfm.tests.cards.cardnames.HiTechLab
import dev.martianzoo.tfm.tests.cards.cardnames.PowerInfrastructure
import dev.martianzoo.tfm.tests.cards.cardnames.TychoMagnetics
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class VariableAmountActionsTest : TfmSandboxTest() {
  @Test
  internal fun `Power Infrastructure cannot finish with no energy and no amount chosen`() {
    newTestGame()
    kim.exMachina("$PowerInfrastructure")

    shouldThrow<NotFullySpecifiedException> { kim.cardAction1(PowerInfrastructure) }
  }

  @Test
  internal fun `Energy Market spends two MC per chosen Energy`() {
    newTestGame()
    kim.exMachina("$EnergyMarket, 6 MC")

    kim.cardAction1(EnergyMarket, x = 3).expect("-6 MC, 3 Energy")
  }

  @Test
  internal fun `Hi-Tech Lab cannot finish with no energy and no amount chosen`() {
    newTestGame()
    kim.exMachina("$HiTechLab")

    shouldThrow<NotFullySpecifiedException> { kim.cardAction1(HiTechLab) }
  }

  @Test
  internal fun `Hi-Tech Lab discards every card it did not keep`() {
    newTestGame()
    kim.exMachina("$HiTechLab, 3 Energy")

    kim.cardAction1(HiTechLab, x = 3).expect("-3 Energy, ProjectCard")
    kim.assertCounts(0 to "ProjectCard<Selecting>")
  }

  @Test
  internal fun `Tycho Magnetics discards every card it did not keep`() {
    newTestGame()
    kim.exMachina("$TychoMagnetics, 4 Energy")

    kim.cardAction1(TychoMagnetics, x = 4).expect("-4 Energy, ProjectCard")
    kim.assertCounts(0 to "ProjectCard<Selecting>")
  }

  @Test
  internal fun `Sell Patents cannot choose zero cards`() {
    newTestGame()

    shouldThrow<PetSyntaxException> { kim.sellPatents(0) }
  }
}
