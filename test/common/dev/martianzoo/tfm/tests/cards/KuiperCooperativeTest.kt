package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.AstroDrill
import dev.martianzoo.tfm.tests.cards.cardnames.KuiperCooperative
import dev.martianzoo.tfm.tests.cards.cardnames.SpaceStation
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class KuiperCooperativeTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame(kimCorporation = KuiperCooperative)
    kim.setToExMachina(33, "MC")
  }

  @Test
  internal fun `Action adds one asteroid per space tag`() {
    kim.exMachina("$SpaceStation")
    stan.exMachina("$AstroDrill")

    kim.inTurn {
          doTask("UseCardAction<KuiperCooperative, Action1>")
          doTask("3 Asteroid<KuiperCooperative>")
        }
        .expect("3 Asteroid<$KuiperCooperative>")
  }

  @Test
  internal fun `Asteroids can help pay for an asteroid standard project`() {
    kim.exMachina("2 Asteroid<$KuiperCooperative>")

    kim.inTurn {
      doTasks(
          "UseStandardProject<AsteroidProject>",
          "-2 Asteroid<KuiperCooperative>",
          "-12 MC",
      )
      doTask("TemperatureStep")
      doTask("TerraformRating")
    }
  }

  @Test
  internal fun `Asteroids can help pay for an aquifer standard project`() {
    kim.exMachina("2 Asteroid<$KuiperCooperative>")

    kim.inTurn {
          doTasks(
              "UseStandardProject<AquiferProject>",
              "-2 Asteroid<KuiperCooperative>",
              "-16 MC",
              "OceanTile<Tharsis_1_2>",
              "TerraformRating",
              "2 Steel",
          )
        }
        .expect("-16 MC, OceanTile")
  }

  @Test
  internal fun `Asteroids cannot pay for another standard project`() {
    kim.exMachina("2 Asteroid<$KuiperCooperative>")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTask("UseStandardProject<PowerPlantProject>")
        // Select the payment to test the outstanding bill independently of the pending attack.
        selectTask("-X? MC")
        doTask("-2 Asteroid<KuiperCooperative>")
      }
    }
  }

  @Test
  internal fun `An asteroid on another card cannot make a Kuiper payment`() {
    kim.exMachina("$AstroDrill, Asteroid<$AstroDrill>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks("UseStandardProject<AsteroidProject>", "-Asteroid<AstroDrill>")
      }
    }
  }
}
