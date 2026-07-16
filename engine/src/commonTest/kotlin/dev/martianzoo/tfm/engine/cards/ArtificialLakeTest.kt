package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class ArtificialLakeTest : CardTest() {
  private lateinit var p1: TfmGameplay

  @BeforeTest
  fun setUp() {
    val game = newGame(GameSetup(Canon, "BMT", 2))
    p1 = game.tfm(PLAYER1)
    p1.phase("Action")
    p1.godMode().manual("100, 5 ProjectCard")
  }

  @Test
  fun `temperature requirement`() {
    p1.godMode().manual("11 TemperatureStep")
    shouldThrow<RequirementException> { p1.playProject("ArtificialLake", 15) }
  }

  @Test
  fun `the artificial ocean must go on land`() {
    p1.godMode().manual("12 TemperatureStep")

    p1.playProject("ArtificialLake", 15) {
      shouldThrow<NarrowingException> { doTask("OceanTile<Tharsis_1_2>!") }
      doTask("OceanTile<Tharsis_2_3>!")
    }
  }

  @Test
  fun `with eight oceans it must still place the ninth`() {
    p1.godMode().manual("12 TemperatureStep")
    placeOceans(8)

    p1.playProject("ArtificialLake", 15) { doTask("OceanTile<Tharsis_2_3>!") }.expect("Tile")

    p1.assertCounts(9 to "OceanTile")
  }

  @Test
  fun `with all oceans placed it plays without placing another`() {
    p1.godMode().manual("12 TemperatureStep")
    placeOceans(9)

    p1.playProject("ArtificialLake", 15) {
      shouldThrow<LimitsException> { doTask("OceanTile<Tharsis_2_3>!") }
      doTask("Ok")
    }

    p1.assertCounts(9 to "OceanTile", 1 to "ArtificialLake")
  }

  @Test
  fun `cannot be played when every land area is occupied`() {
    p1.godMode().manual("12 TemperatureStep")
    val landAreas =
        p1.list("LandArea").filterNot { it.toString() == "VolcanicArea" } + p1.list("VolcanicArea")
    p1.godMode().manual(landAreas.joinToString { "GreeneryTile<$it>" })

    shouldThrow< IllegalArgumentException> { // TODO DeadEndException?
      p1.playProject("ArtificialLake", 15)
    }
  }

  private fun placeOceans(count: Int) {
    p1.godMode().manual(p1.list("WaterArea").take(count).joinToString { "OceanTile<$it>" })
  }
}
