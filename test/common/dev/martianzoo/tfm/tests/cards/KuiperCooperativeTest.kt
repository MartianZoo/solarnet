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
          selectOnlyTask()
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
              "-MC / Owed",
          )
          selectTask("TemperatureStep*")
          doTask("TerraformRating")
        }
        .expect("-12 MC, TemperatureStep")
  }

  @Test
  internal fun `Asteroids can help pay for an aquifer standard project`() {
    kim.exMachina("2 Asteroid<$KuiperCooperative>")

    kim.inTurn {
          doTasks(
              "UseStandardProject<AquiferProject>",
              "-2 Asteroid<KuiperCooperative>",
              "-MC / Owed",
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
        selectOnlyTask()
        doTask("UseStandardProject<PowerPlantProject>")
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
        selectOnlyTask()
        doTasks("UseStandardProject<AsteroidProject>", "-Asteroid<AstroDrill>")
      }
    }
  }
}
