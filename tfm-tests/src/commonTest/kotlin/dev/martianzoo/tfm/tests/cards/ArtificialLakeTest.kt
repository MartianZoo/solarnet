package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ArtificialLakeTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    engine.phase("Action")
  }

  @Test
  internal fun `Cannot be played when every land area is occupied`() {
    val landAreas =
        p1.list("LandArea").filterNot { it.toString() == "VolcanicArea" } + p1.list("VolcanicArea")
    seedGame(
        "12 TemperatureStep",
        landAreas.joinToString { "GreeneryTile<$it>" },
    )

    shouldThrow<NotNowException> { p1.playProject(ArtificialLake, 15) }
  }

  @Test
  internal fun `Can be played with eight oceans`() {
    seedGame("12 TemperatureStep", oceanTiles(8))
    p1.playProject(ArtificialLake, 15) { placeTile(2, 3) }.expect("Tile")
  }

  @Test
  internal fun `Can be played with nine oceans without placing another ocean`() {
    seedGame("12 TemperatureStep", oceanTiles(9))
    p1.playProject(ArtificialLake, 15)
    p1.assertCounts(9 to "OceanTile")
  }

  @Test
  internal fun `Cannot place its ocean on a water area`() {
    seedGame("12 TemperatureStep")
    p1.playProject(ArtificialLake, 15) {
      shouldThrow<NarrowingException> { doTask("OceanTile<Tharsis_1_2>") }
      placeTile(2, 3)
    }
  }

  @Test
  internal fun `Cannot be played below -6 °C`() {
    seedGame("11 TemperatureStep")
    shouldThrow<RequirementException> { p1.playProject(ArtificialLake, 15) }
  }

  private fun seedGame(vararg components: String) =
      p1.manual((listOf("15 MC, ProjectCard") + components).joinToString())

  private fun oceanTiles(count: Int) =
      p1.list("WaterArea").take(count).joinToString { "OceanTile<$it>" }
}
