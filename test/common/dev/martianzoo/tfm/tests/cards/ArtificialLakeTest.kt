package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.MarsMapDefinition.AreaDefinition
import dev.martianzoo.tfm.state.ApiUtils.mapDefinition
import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ArtificialLakeTest : CardTest() {
  @Test
  internal fun `A full land board blocks placement while the ocean track is incomplete`() {
    startTerraforming(startingMc = 1_500)
    raiseTemperatureTo(12)
    placeOceans(8)
    connectedLandAreas().forEach { area ->
      p1.stdProject("GreeneryProject") { placeTile(area.row, area.column) }
    }
    val moneyBefore = p1.count("MC")
    val cardsBefore = p1.count("ProjectCard")

    shouldThrow<LimitsException> { p1.playProject(ArtificialLake, 15) { placeTile(2, 3) } }

    p1.count("MC") shouldBe moneyBefore
    p1.count("ProjectCard") shouldBe cardsBefore
    p1.count("$ArtificialLake") shouldBe 0
    p1.count("OceanTile") shouldBe 8
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
  internal fun `Cannot decline the next ocean after nine oceans on Amazonis`() {
    newGame(Amazonis)
    p1.runOperation("500 MC, ProjectCard, 12 TemperatureStep")
    admin.phase("Action")
    placeOceans(9)

    shouldThrow<NarrowingException> {
      p1.playProject(ArtificialLake, 15) { declineTask() }
    }
  }

  private fun startTerraforming(startingMc: Int = 500) {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase(startingMc = startingMc)
    p1.turn {
      stdProject("AsteroidProject")
      stdProject("AsteroidProject")
    }
    requireP2().pass()
  }

  private fun raiseTemperatureTo(step: Int) {
    repeat(step - 2) { p1.stdProject("AsteroidProject") }
  }

  private fun placeOceans(count: Int) {
    p1.list("WaterArea(HAS MAX 0 Tile)").take(count).forEach { area ->
      p1.stdProject("AquiferProject") { doTask("OceanTile<$area>") }
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
