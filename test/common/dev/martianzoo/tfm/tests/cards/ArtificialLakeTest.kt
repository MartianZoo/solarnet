package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.canon.ApiUtils.mapDefinition
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ArtificialLakeTest : CardTest() {
  @Test
  internal fun `Cannot be played when every land area is occupied`() {
    startTerraforming(startingMc = 1_500)
    raiseTemperatureTo(12)
    connectedLandAreas().forEach { area ->
      p1.stdProject("GreenerySP") { placeTile(area.row, area.column) }
    }

    shouldThrow<NotNowException> { p1.playProject(ArtificialLake, 15) }
  }

  @Test
  internal fun `Can be played with eight oceans`() {
    startTerraforming()
    raiseTemperatureTo(12)
    placeOceans(8)

    p1.playProject(ArtificialLake, 15) { placeTile(2, 3) }.expect("Tile")
  }

  @Test
  internal fun `Can be played with nine oceans without placing another ocean`() {
    startTerraforming()
    raiseTemperatureTo(12)
    placeOceans(9)

    p1.playProject(ArtificialLake, 15).expect("0 OceanTile")
  }

  @Test
  internal fun `Cannot place its ocean on a water area`() {
    startTerraforming()
    raiseTemperatureTo(12)

    p1.playProject(ArtificialLake, 15) {
      shouldThrow<NarrowingException> { doTask("OceanTile<Tharsis_1_2>") }
      placeTile(2, 3)
    }
  }

  @Test
  internal fun `Cannot be played below -6 °C`() {
    startTerraforming()
    raiseTemperatureTo(11)

    shouldThrow<RequirementException> { p1.playProject(ArtificialLake, 15) }
  }

  private fun startTerraforming(startingMc: Int = 500) {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase(startingMc = startingMc)
    p1.turn {
      stdProject("AsteroidSP")
      stdProject("AsteroidSP")
    }
    requireP2().pass()
  }

  private fun raiseTemperatureTo(step: Int) {
    repeat(step - 2) { p1.stdProject("AsteroidSP") }
  }

  private fun placeOceans(count: Int) {
    p1.list("WaterArea").take(count).forEach { area ->
      p1.stdProject("AquiferSP") { doTask("OceanTile<$area>") }
    }
  }

  private fun connectedLandAreas(): List<AreaDefinition> {
    val grid = mapDefinition(p1.reader).areas
    val landAreaNames =
        (p1.list("LandArea") + p1.list("VolcanicArea")).mapTo(mutableSetOf()) { it.toString() }
    val remaining = grid.filter { it.className.toString() in landAreaNames }.toMutableSet()
    val pending = mutableListOf(remaining.first())
    remaining.remove(pending.first())

    return buildList {
      var next = 0
      while (next < pending.size) {
        val area = pending[next++]
        add(area)
        grid.hexNeighbors(area.row, area.column).forEach { neighbor ->
          if (remaining.remove(neighbor)) pending.add(neighbor)
        }
      }
      check(remaining.isEmpty())
    }
  }
}
