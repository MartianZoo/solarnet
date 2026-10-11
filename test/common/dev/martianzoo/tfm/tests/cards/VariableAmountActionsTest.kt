package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.EnergyMarket
import dev.martianzoo.tfm.tests.cards.cardnames.FakeHelion
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

    shouldThrow<NotFullySpecifiedException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<PowerInfrastructure, Action1>")
        selectOnlyTask()
      }
    }
  }

  @Test
  internal fun `Energy Market spends two MC per chosen Energy`() {
    newTestGame()
    kim.exMachina("$EnergyMarket")
    kim.setToExMachina(6, "MC")

    kim.inTurn {
          doTask("UseCardAction<EnergyMarket, Action1>")
          selectOnlyTask()
          selectOnlyTask(x = 3)
          doTask("-MC / Owed<Class<MC>>")
          selectOnlyTask()
        }
        .expect("-6 MC, 3 Energy")
  }

  @Test
  internal fun `Energy Market accepts Heat instead of MC for FakeHelion`() {
    newTestGame(addOptions = "FakeStuffBundle", kimCorporation = FakeHelion)
    kim.exMachina("$EnergyMarket")
    kim.setToExMachina(6, "Heat")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTask("UseCardAction<EnergyMarket, Action1>")
          selectOnlyTask()
          selectOnlyTask(x = 3)
          doTask("-6 Heat")
          selectTask("-X MC?")
          doTask("Ok")
          selectOnlyTask()
        }
        .expect("0 MC, 3 Energy")
  }

  @Test
  internal fun `Hi-Tech Lab cannot finish with no energy and no amount chosen`() {
    newTestGame()
    kim.exMachina("$HiTechLab")

    shouldThrow<NotFullySpecifiedException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<HiTechLab, Action1>")
        selectOnlyTask()
      }
    }
  }

  @Test
  internal fun `Hi-Tech Lab discards every card it did not keep`() {
    newTestGame()
    kim.exMachina("$HiTechLab")
    kim.setToExMachina(3, "Energy")

    kim.inTurn {
          doTask("UseCardAction<HiTechLab, Action1>")
          selectOnlyTask()
          selectOnlyTask(x = 3)
          doTask("-Energy / Owed<Class<Energy>>")
          selectOnlyTask()
          selectTask("3 ProjectCard<Kim, Selecting>!")
          selectOnlyTask()
          selectOnlyTask()
        }
        .expect("-3 Energy, ProjectCard")
    kim.assertCounts(0 to "ProjectCard<Selecting>")
  }

  @Test
  internal fun `Tycho Magnetics discards every card it did not keep`() {
    newTestGame()
    kim.exMachina("$TychoMagnetics")
    kim.setToExMachina(4, "Energy")

    kim.inTurn {
          doTask("UseCardAction<TychoMagnetics, Action1>")
          selectOnlyTask()
          selectOnlyTask(x = 4)
          doTask("-Energy / Owed<Class<Energy>>")
          selectOnlyTask()
          selectTask("4 ProjectCard<Kim, Selecting>!")
          selectOnlyTask()
          selectOnlyTask()
        }
        .expect("-4 Energy, ProjectCard")
    kim.assertCounts(0 to "ProjectCard<Selecting>")
  }

  @Test
  internal fun `Sell Patents cannot choose zero cards`() {
    newTestGame()

    shouldThrow<PetSyntaxException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseStandardProject<SellPatentsProject>")
        selectOnlyTask()
        doTask("0 MC FROM ProjectCard!")
      }
    }
  }
}
