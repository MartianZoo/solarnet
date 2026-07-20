package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class ArtificialLakeTest : CardTest() {
  @BeforeTest
  fun setUp() {
    newGame(Canon.SIMPLE_GAME)
    player1.phase("Action")
  }

  @Test
  fun `temperature requirement`() {
    prepare("11 TemperatureStep")
    shouldThrow<RequirementException> { player1.playProject("ArtificialLake", 15) }
  }

  @Test
  fun `the artificial ocean must go on land`() {
    prepare("12 TemperatureStep")

    player1.playProject("ArtificialLake", 15) {
      shouldThrow<NarrowingException> { doTask("OceanTile<Tharsis_1_2>!") }
      doTask("OceanTile<Tharsis_2_3>!")
    }
  }

  @Test
  fun `with eight oceans it must still place the ninth`() {
    prepare("12 TemperatureStep", oceanTiles(8))

    player1.playProject("ArtificialLake", 15) { doTask("OceanTile<Tharsis_2_3>!") }.expect("Tile")
  }

  @Test
  fun `with all oceans placed it plays without placing another`() {
    prepare("12 TemperatureStep", oceanTiles(9))

    player1.playProject("ArtificialLake", 15) {
      shouldThrow<LimitsException> { doTask("OceanTile<Tharsis_2_3>!") }
      doTask("Ok")
    }

    player1.assertCounts(9 to "OceanTile", 1 to "ArtificialLake")
  }

  @Test
  fun `cannot be played when every land area is occupied`() {
    val landAreas =
        player1.list("LandArea").filterNot { it.toString() == "VolcanicArea" } +
            player1.list("VolcanicArea")
    prepare("12 TemperatureStep", landAreas.joinToString { "GreeneryTile<$it>" })

    shouldThrow<IllegalArgumentException> { // TODO DeadEndException?
      player1.playProject("ArtificialLake", 15)
    }
  }

  private fun prepare(vararg components: String) =
      player1.sneak((listOf("100, 5 ProjectCard") + components).joinToString())

  private fun oceanTiles(count: Int) =
      player1.list("WaterArea").take(count).joinToString { "OceanTile<$it>" }
}
