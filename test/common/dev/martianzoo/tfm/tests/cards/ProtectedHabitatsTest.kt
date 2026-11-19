package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ProtectedHabitatsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Allows its owner to spend protected plants`() {
    kim.exMachina("$ProtectedHabitats, 8 Plant")

    kim.inTurn {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_1_1>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
      )
    }
  }

  @Test
  internal fun `Allows its owner's Predators to eat protected animals`() {
    kim.exMachina("$ProtectedHabitats, $Predators, $Fish, Animal<$Fish>")

    kim.inTurn {
          doTasks(
              *cardActionTasks("$Predators"),
              "-Animal<$Fish>",
              "Animal<$Predators>",
          )
        }
        .expect("Animal<$Predators>")
  }

  @Test
  internal fun `Allows its owner's Ants to eat protected microbes`() {
    kim.exMachina("$ProtectedHabitats, $Ants, $Tardigrades, Microbe<$Tardigrades>")

    kim.inTurn {
          doTasks(
              *cardActionTasks("$Ants"),
              "-Microbe<$Tardigrades>",
              "Microbe<$Ants>",
          )
        }
        .expect("Microbe<$Ants>")
  }

  @Test
  internal fun `Prevents an opponent's asteroid from removing protected plants`() {
    kim.exMachina("$ProtectedHabitats, Plant")

    shouldThrow<DeadEndException> {
      stan.inTurn {
        doTasks(
            *playCardTasks("$AsteroidCard"),
            "14 Pay<> FROM MC",
            "$AsteroidCard FROM ProjectCard",
            "TemperatureStep",
            "2 Titanium",
            "TerraformRating",
        )
        // Resolve the attack before probing a protected target that the offer excludes.
        selectTask("-3? Plant<Anyone>")
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
        doTasks(
            *cardActionTasks("$Predators"),
            "-Animal<Kim, $Fish<Kim>>",
        )
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
        doTasks(
            *cardActionTasks("$Ants"),
            "-Microbe<Kim, $Tardigrades<Kim>>",
        )
      }
    }
    kim.assertCounts(1 to "Microbe<$Tardigrades>")
  }
}
