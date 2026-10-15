package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PredatorsTest : TfmSandboxTest() {

  private fun initializeGame() {
    newTestGame()
    kim.exMachina("$Predators")
  }

  @Test
  internal fun `Cannot act when no animal can be removed`() {
    initializeGame()
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("UseCardAction<Predators, Action1>")
        // Resolve the mandatory removal to prove that an empty target set rejects the action.
        selectTask("-Animal")
      }
    }
  }

  @Test
  internal fun `Removes exactly one of two animals on the target card`() {
    initializeGame()
    stan.exMachina("$Birds, 2 Animal<$Birds>")

    kim.inTurn {
          doTask("UseCardAction<Predators, Action1>")
          doTask("-Animal<Birds<Stan>>")
          doTask("Animal<Predators>")
        }
        .expect("-Animal<$Birds<Stan>>, Animal<$Predators>")
  }

  @Test
  internal fun `Cannot decline to remove an opponent's animal`() {
    initializeGame()
    stan.exMachina("$Birds, Animal<$Birds>")
    kim.exMachina("Animal<$Predators>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("UseCardAction<Predators, Action1>")
        doTask("Ok")
      }
    }
  }

  @Test
  internal fun `Can remove an animal from another card its player owns`() {
    initializeGame()
    kim.exMachina("$Birds")
    kim.exMachina("Animal<$Birds>")
    kim.exMachina("Animal<$Predators>")

    kim.inTurn {
          doTasks("UseCardAction<Predators, Action1>", "-Animal<Birds>")
          doTask("Animal<Predators>")
        }
        .expect("Animal<$Predators>")
  }

  @Test
  internal fun `Can remove and replace its own animal`() {
    initializeGame()
    kim.exMachina("Animal<$Predators>")
    kim.inTurn {
          doTask("UseCardAction<Predators, Action1>")
          doTask("-Animal<Predators>")
          doTask("Animal<Predators>")
        }
        .expect("0 Animal<$Predators>")
  }

  @Test
  internal fun `Predators can remove its own animal and trigger Meat Industry when replacing it`() {
    newTestGame()
    kim.exMachina("$Predators, $MeatIndustry, Animal<$Predators>")

    kim.inTurn {
          doTask("UseCardAction<Predators, Action1>")
          doTask("-Animal<Predators>")
          doTask("Animal<Predators>")
          doTask("2 MC")
        }
        .expect("0 Animal<$Predators>")
  }

  @Test
  internal fun `Takes an animal from the neutral holder in solo play`() {
    newTestGame(playerCount = 1)
    kim.exMachina("$Predators")

    kim.inTurn {
          doTask("UseCardAction<Predators, Action1>")
          doTask("-Animal<SoloCardResourceReserve<Class<Animal>>>")
          // Hand replenishment of the solo reserve to Admin after removing its animal.
          selectTask("Animal<SoloCardResourceReserve<Class<Animal>>>* BY Admin")
          doTask("Animal<Predators>")
        }
        .expect("Animal<$Predators>")
  }
}
