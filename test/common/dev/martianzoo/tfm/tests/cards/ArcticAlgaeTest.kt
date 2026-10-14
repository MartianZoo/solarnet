package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ArcticAlgaeTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Gains plants when an opponent places an ocean`() {
    kim.exMachina("$ArcticAlgae")

    stan.inTurn {
      doTasks(
          "UseStandardProject<AquiferProject>",
          "-18 MC",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
      )
      kim.doTask("2 Plant")
      doTask("2 Steel")
    }
  }

  @Test
  internal fun `Gains plants before an asteroid removes them when oceans resolve first`() {
    stan.exMachina("$ArcticAlgae, 2 Plant")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<GiantIceAsteroid>>",
          "-36 MC",
          "GiantIceAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TerraformRating",
          "TerraformRating",
      )
      shouldThrow<TaskException> { stan.doTask("2 Plant") }
      doTask("OceanTile<Tharsis_1_2>")
      doTasks("TerraformRating", "2 Steel")
      stan.doTask("2 Plant")
      doTask("OceanTile<Tharsis_1_4>")
      doTasks("TerraformRating", "ProjectCard")
      stan.doTask("2 Plant")
      doTask("-6 Plant<Stan>")
      doTask("Ok" /* No Titanium payment. */)
      doTask("PlayedEvent<Class<GiantIceAsteroid>> FROM GiantIceAsteroid")
    }
  }

  @Test
  internal fun `Gains plants after an asteroid removes them when the attack resolves first`() {
    stan.exMachina("$ArcticAlgae, 2 Plant")

    kim.inTurn {
      doTasks("PlayProject<Class<GiantIceAsteroid>>", "-36 MC")
      doTasks(
          "Ok" /* No Titanium payment. */,
          "GiantIceAsteroid FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTask("TemperatureStep")
      doTasks(
          "TerraformRating",
          "TerraformRating",
          "-2 Plant<Stan>",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
      )
      stan.doTask("2 Plant")
      doTasks("2 Steel", "OceanTile<Tharsis_1_4>", "TerraformRating")
      stan.doTask("2 Plant")
      doTask("ProjectCard<Kim, Hand>")
      doTask("PlayedEvent<Class<GiantIceAsteroid>> FROM GiantIceAsteroid")
    }
  }
}
