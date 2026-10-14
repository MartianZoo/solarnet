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

    shouldThrow<NotFullySpecifiedException> {
      kim.inTurn {
        doTask("UseCardAction<PowerInfrastructure, Action1>")
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

      // Bind the shared amount before Admin opens the bill for this action.
      selectOnlyTask(x = 3)
      doTask("-6 MC")
      doTask("3 Energy")
    }
  }

  @Test
  internal fun `Energy Market accepts Heat instead of MC for FakeHelion`() {
    // Its starting purchase offers Heat payment, which this test declines explicitly.
    newTestGameBeforeCorporationPlay(addOptions = "fakes:cards")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<FakeHelion>>",
          "FakeHelion FROM CorporationCard",
          "42 MC",
          "PROD[3 Heat]",
      )
      // Other optional tasks also accept Ok; identify the Heat payment first.
      selectTask("-X? Heat")
      doTasks("Ok" /* No Heat payment. */, "-15 MC", "5 ProjectCard<Hand FROM Selecting>")
    }
    advanceUntilActionPhase()
    kim.exMachina("$EnergyMarket")
    kim.setToExMachina(6, "Heat")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTask("UseCardAction<EnergyMarket, Action1>")

          // Bind the shared amount before Admin opens the bill for this action.
          selectOnlyTask(x = 3)
          doTask("-6 Heat")
          doTask("Ok" /* No MC payment. */)
          doTask("3 Energy")
        }
        .expect("0 MC, 3 Energy")
  }

  @Test
  internal fun `Hi-Tech Lab cannot finish with no energy and no amount chosen`() {
    newTestGame()
    kim.exMachina("$HiTechLab")

    shouldThrow<NotFullySpecifiedException> {
      kim.inTurn {
        doTask("UseCardAction<HiTechLab, Action1>")
      }
    }
  }

  @Test
  internal fun `Hi-Tech Lab keeps a card from its selection`() {
    newTestGame()
    kim.exMachina("$HiTechLab")
    kim.setToExMachina(3, "Energy")

    kim.inTurn {
          doTask("UseCardAction<HiTechLab, Action1>")

          // Bind the shared amount before Admin opens the bill for this action.
          selectOnlyTask(x = 3)
          doTask("-3 Energy")
          // Expand the paid draw-and-keep sequence before choosing the cards.
          selectOnlyTask()
          doTask("3 ProjectCard<Kim, Selecting>")
          doTask("ProjectCard<Hand FROM Selecting>")
        }
        .expect("-3 Energy, ProjectCard")
    kim.assertCounts(0 to "ProjectCard<Selecting>")
  }

  @Test
  internal fun `Tycho Magnetics keeps a card from its selection`() {
    newTestGame()
    kim.exMachina("$TychoMagnetics")
    kim.setToExMachina(4, "Energy")

    kim.inTurn {
          doTask("UseCardAction<TychoMagnetics, Action1>")

          // Bind the shared amount before Admin opens the bill for this action.
          selectOnlyTask(x = 4)
          doTask("-4 Energy")
          // Expand the paid draw-and-keep sequence before choosing the cards.
          selectOnlyTask()
          doTask("4 ProjectCard<Kim, Selecting>")
          doTask("ProjectCard<Hand FROM Selecting>")
        }
        .expect("-4 Energy, ProjectCard")
    kim.assertCounts(0 to "ProjectCard<Selecting>")
  }

  @Test
  internal fun `Sell Patents cannot choose zero cards`() {
    newTestGame()

    shouldThrow<PetSyntaxException> {
      kim.inTurn {
        doTask("UseStandardProject<SellPatentsProject>")
        doTask("0 MC FROM ProjectCard!")
      }
    }
  }
}
