package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ProtectedHabitatsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Allows its owner to spend protected plants`() {
    kim.exMachina("$ProtectedHabitats, 8 Plant")

    kim.inTurn {
          doTasks("ConvertPlants", "-Plant / Owed<Class<Plant>>")
          selectOnlyTask()
          doTask("GreeneryTile<Tharsis_1_1>")
          selectTask("OxygenStep.")
          doTasks("2 Steel", "TerraformRating")
        }
        .expect("-8 Plant")
  }

  @Test
  internal fun `Allows its owner's Predators to eat protected animals`() {
    kim.exMachina("$ProtectedHabitats, $Predators, $Fish, Animal<$Fish>")

    kim.inTurn {
          doTask("UseCardAction<Predators, Action1>")
          selectOnlyTask()
          selectOnlyTask()
        }
        .expect("-Animal<$Fish>, Animal<$Predators>")
  }

  @Test
  internal fun `Allows its owner's Ants to eat protected microbes`() {
    kim.exMachina("$ProtectedHabitats, $Ants, $Tardigrades, Microbe<$Tardigrades>")

    kim.inTurn {
          doTask("UseCardAction<Ants, Action1>")
          selectOnlyTask()
          selectOnlyTask()
        }
        .expect("-Microbe<$Tardigrades>, Microbe<$Ants>")
  }

  @Test
  internal fun `Prevents an opponent's asteroid from removing protected plants`() {
    kim.exMachina("$ProtectedHabitats, Plant")

    shouldThrow<DeadEndException> {
      stan.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<AsteroidCard>>", "-14 MC")
        selectTask("-X Titanium?")
        selectTask("Ok")
        doTask("AsteroidCard FROM ProjectCard")
        selectTask("TemperatureStep.")
        selectTask("2 Titanium!")
        selectTask("TerraformRating!")
        selectTask("-3 Plant<Anyone>?")
        doTask("-Plant<Kim>")
      }
    }
    kim.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Prevents an opponent's Predators from eating protected animals`() {
    kim.exMachina("$ProtectedHabitats, $Fish, Animal<$Fish>")
    stan.exMachina("$Predators, Animal<$Predators>")

    shouldThrow<DeadEndException> {
      stan.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<Predators, Action1>")
        selectOnlyTask()
        doTask("-Animal<Kim, Fish<Kim>>")
      }
    }
    kim.assertCounts(1 to "Animal<$Fish>")
  }

  @Test
  internal fun `Prevents an opponent's Ants from eating protected microbes`() {
    kim.exMachina("$ProtectedHabitats, $Tardigrades, Microbe<$Tardigrades>")
    stan.exMachina("$Ants, Microbe<$Ants>")

    shouldThrow<DeadEndException> {
      stan.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<Ants, Action1>")
        selectOnlyTask()
        doTask("-Microbe<Kim, Tardigrades<Kim>>")
      }
    }
    kim.assertCounts(1 to "Microbe<$Tardigrades>")
  }
}
