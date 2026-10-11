package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class VironTest : TfmSandboxTest() {
  @Test
  internal fun `Can repeat an action used earlier in the generation`() {
    newTestGame(kimCorporation = Viron)
    kim.exMachina("$AtmoCollectors")
    kim.inTurn { doTasks("UseCardAction<$AtmoCollectors, Action1>", "Floater<$AtmoCollectors>") }

    kim.inTurn {
      doTasks(
          "UseCardAction<$Viron, Action1>",
          "UseAction<$AtmoCollectors, Action1>",
          "Floater<$AtmoCollectors>",
      )
    }
  }

  @Test
  internal fun `Can choose a different action on the previously used card`() {
    initializeGame()

    kim.inTurn {
      doTask("UseCardAction<AtmoCollectors, Action1>")
      selectOnlyTask()
    }

    kim.inTurn {
          doTasks("UseCardAction<Viron, Action1>", "UseAction<AtmoCollectors, Action2>")
          selectOnlyTask()
          doTask("2 Titanium")
        }
        .expect("-Floater")
  }

  @Test
  internal fun `Cannot use Viron to repeat Viron's own action`() {
    initializeGame()
    kim.inTurn {
      doTask("UseCardAction<AtmoCollectors, Action1>")
      selectOnlyTask()
    }
    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTask("UseCardAction<$Viron, Action1>")
        selectOnlyTask()
        doTask("UseAction<$Viron, Action1>")
      }
    }
  }

  @Test
  internal fun `Cannot choose an action card that has not been used`() {
    initializeGame()
    kim.exMachina("$ExtractorBalloons")
    kim.inTurn {
      doTask("UseCardAction<AtmoCollectors, Action1>")
      selectOnlyTask()
    }

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTask("UseCardAction<$Viron, Action1>")
        selectOnlyTask()
        doTask("UseAction<$ExtractorBalloons, Action1>")
      }
    }
  }

  @Test
  internal fun `Cannot repeat another player's action`() {
    newTestGame(kimCorporation = Viron)
    kim.exMachina("$ExtractorBalloons")
    stan.exMachina("$AtmoCollectors, Floater<$AtmoCollectors>")
    kim.inTurn {
      doTask("UseCardAction<ExtractorBalloons, Action1>")
      selectOnlyTask()
    }
    stan.inTurn {
      doTask("UseCardAction<AtmoCollectors, Action1>")
      selectOnlyTask()
    }

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTask("UseCardAction<$Viron, Action1>")
        selectOnlyTask()
        doTask("UseAction<$AtmoCollectors<Stan>, Action1>")
      }
    }
  }

  @Test
  internal fun `Repeats an action on another corporation`() {
    newTestGame(kimCorporation = Viron)
    kim.exMachina("$Celestic")
    kim.inTurn {
      doTask("UseCardAction<Celestic, Action1>")
      selectOnlyTask()
      doTask("Floater<Celestic>.")
    }

    kim.inTurn {
          doTasks("UseCardAction<Viron, Action1>", "UseAction<Celestic, Action1>")
          selectOnlyTask()
          doTask("Floater<Celestic>.")
        }
        .expect("Floater<$Celestic>")
  }

  private fun initializeGame() {
    newTestGame(kimCorporation = Viron)
    kim.exMachina("$AtmoCollectors, Floater<$AtmoCollectors>")
  }
}
