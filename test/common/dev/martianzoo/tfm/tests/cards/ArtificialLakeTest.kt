package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.ArtificialLake
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ArtificialLakeTest : TfmSandboxTest() {
  @Test
  internal fun `Cannot be played on a full land board while the ocean track is incomplete`() {
    newTestGame()
    kim.setToExMachina(12, "TemperatureStep")
    kim.exMachina(kim.list("WaterArea").take(8).joinToString { "OceanTile<$it>" })
    val land = (kim.list("LandArea") + kim.list("VolcanicArea")).distinct()
    kim.exMachina(land.joinToString { "GreeneryTile<Kim, $it>" })

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<ArtificialLake>>", "-15 MC")
        doTask("ArtificialLake FROM ProjectCard")
        selectOnlyTask()
        doTask("OceanTile<Tharsis_2_3>")
      }
    }

    kim.assertCounts(42 to "MC", 10 to "ProjectCard", 0 to "$ArtificialLake", 8 to "OceanTile")
  }

  @Test
  internal fun `Places the final ocean on land when eight oceans are present`() {
    newTestGame()
    kim.setToExMachina(12, "TemperatureStep")
    kim.exMachina(kim.list("WaterArea").take(8).joinToString { "OceanTile<$it>" })

    kim.inTurn {
      doTasks("PlayProject<Class<ArtificialLake>>", "-15 MC")
      doTasks(
          "ArtificialLake FROM ProjectCard",
          "OceanTile<Tharsis_2_3>",
          "TerraformRating",
          "2 MC",
      )
    }
  }

  @Test
  internal fun `Can be played without placing an ocean after the ocean track is complete`() {
    newTestGame()
    kim.setToExMachina(12, "TemperatureStep")
    kim.exMachina(kim.list("WaterArea").take(8).joinToString { "OceanTile<$it>" })
    stan.inTurn {
      doTasks(
          "UseStandardProject<AquiferProject>",
          "-18 MC",
          "OceanTile<Tharsis_6_7>",
          "TerraformRating",
          "Plant",
          "2 MC",
      )
    }

    kim.inTurn {
          doTasks("PlayProject<Class<ArtificialLake>>", "-15 MC")
          doTasks("ArtificialLake FROM ProjectCard")
          selectOnlyTask()
        }
        .expect("0 OceanTile, 0 TerraformRating")
  }

  @Test
  internal fun `Cannot decline placement after nine oceans on Amazonis`() {
    newTestGame(addOptions = "AmazonisMap")
    kim.setToExMachina(12, "TemperatureStep")
    kim.exMachina(kim.list("WaterArea").take(9).joinToString { "OceanTile<$it>" })

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayProject<Class<ArtificialLake>>", "-15 MC")
        doTask("ArtificialLake FROM ProjectCard")
        selectOnlyTask()
        doTask("Ok")
      }
    }
    kim.assertCounts(42 to "MC", 10 to "ProjectCard", 9 to "OceanTile")
  }
}
