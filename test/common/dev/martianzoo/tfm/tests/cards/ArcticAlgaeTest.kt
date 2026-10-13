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
      selectTask("2! Plant<Kim>")
      kim.selectTask("2! Plant")
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
      selectTask("2 Plant<Stan>")
      stan.doTask("2 Plant")
      doTask("OceanTile<Tharsis_1_4>")
      doTasks("TerraformRating", "ProjectCard")
      selectTask("2 Plant<Stan>")
      stan.doTask("2 Plant")
      doTask("-6 Plant<Stan>")
      selectTask("-X? Titanium")
      doTask("Ok")
    }
  }

  @Test
  internal fun `Gains plants after an asteroid removes them when the attack resolves first`() {
    stan.exMachina("$ArcticAlgae, 2 Plant")

    kim.inTurn {
      doTasks("PlayProject<Class<GiantIceAsteroid>>", "-36 MC")
      selectTask("-X? Titanium")
      doTasks("Ok", "GiantIceAsteroid FROM ProjectCard")
      selectTask("TemperatureStep*")
      selectTask("TemperatureStep*")
      doTasks(
          "TerraformRating",
          "TerraformRating",
          "-2 Plant<Stan>",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
      )
      selectTask("2! Plant<Stan>")
      stan.selectTask("2! Plant")
      doTasks("2 Steel", "OceanTile<Tharsis_1_4>", "TerraformRating")
      selectTask("2! Plant<Stan>")
      stan.selectTask("2! Plant")
      selectTask("ProjectCard<Kim, Hand>!")
    }
  }
}
