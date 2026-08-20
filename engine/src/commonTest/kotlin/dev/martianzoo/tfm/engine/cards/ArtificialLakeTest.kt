package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class ArtificialLakeTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    engine.phase("Action")
  }

  @Test
  fun `without an empty land area, Artificial Lake is unavailable`() {
    val landAreas =
        p1.list("LandArea").filterNot { it.toString() == "VolcanicArea" } + p1.list("VolcanicArea")
    seedGame(
        "12 TemperatureStep",
        landAreas.joinToString { "GreeneryTile<$it>" },
    )

    shouldThrow<NotNowException> { p1.playProject(ArtificialLake, 15) }
  }

  @Test
  fun `with eight oceans, plays Artificial Lake`() {
    seedGame("12 TemperatureStep", oceanTiles(8))
    p1.playProject(ArtificialLake, 15) { doTask("OceanTile<Tharsis_2_3>!") }.expect("Tile")
  }

  @Test
  fun `with nine oceans, plays Artificial Lake`() {
    seedGame("12 TemperatureStep", oceanTiles(9))
    p1.playProject(ArtificialLake, 15)
    p1.assertCounts(9 to "OceanTile")
  }

  @Test
  fun `with a water area selected, places the Artificial Lake ocean`() {
    seedGame("12 TemperatureStep")
    p1.playProject(ArtificialLake, 15) {
      shouldThrow<NarrowingException> { doTask("OceanTile<Tharsis_1_2>!") }
      doTask("OceanTile<Tharsis_2_3>!")
    }
  }

  @Test
  fun `below twelve temperature steps, tries to play Artificial Lake`() {
    seedGame("11 TemperatureStep")
    shouldThrow<RequirementException> { p1.playProject(ArtificialLake, 15) }
  }

  private fun seedGame(vararg components: String) =
      p1.manual((listOf("15, ProjectCard") + components).joinToString())

  private fun oceanTiles(count: Int) =
      p1.list("WaterArea").take(count).joinToString { "OceanTile<$it>" }
}
