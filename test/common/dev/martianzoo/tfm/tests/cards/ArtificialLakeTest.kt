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

    shouldThrow<LimitsException> { kim.playProject(ArtificialLake, 15) { placeTile(2, 3) } }

    kim.assertCounts(42 to "MC", 10 to "ProjectCard", 0 to "$ArtificialLake", 8 to "OceanTile")
  }

  @Test
  internal fun `Places the final ocean on land when eight oceans are present`() {
    newTestGame()
    kim.setToExMachina(12, "TemperatureStep")
    kim.exMachina(kim.list("WaterArea").take(8).joinToString { "OceanTile<$it>" })

    kim.playProject(ArtificialLake, 15) { placeTile(2, 3) }.expect("OceanTile, TerraformRating")
  }

  @Test
  internal fun `Can be played without placing an ocean after the ocean track is complete`() {
    newTestGame()
    kim.setToExMachina(12, "TemperatureStep")
    kim.exMachina(kim.list("WaterArea").take(8).joinToString { "OceanTile<$it>" })
    stan.stdProject("AquiferProject") {
      doTask("OceanTile<${stan.list("WaterArea(HAS MAX 0 Tile)").first()}>")
    }

    kim.playProject(ArtificialLake, 15).expect("0 OceanTile, 0 TerraformRating")
  }

  @Test
  internal fun `Cannot decline placement after nine oceans on Amazonis`() {
    newTestGame(addOptions = "AmazonisMap")
    kim.setToExMachina(12, "TemperatureStep")
    kim.exMachina(kim.list("WaterArea").take(9).joinToString { "OceanTile<$it>" })

    shouldThrow<NarrowingException> {
      kim.playProject(ArtificialLake, 15) { declineTask() }
    }
    kim.assertCounts(42 to "MC", 10 to "ProjectCard", 9 to "OceanTile")
  }
}
