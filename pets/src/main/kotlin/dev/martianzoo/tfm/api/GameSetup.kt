package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.MapAreaDefinition
import dev.martianzoo.util.Grid
import dev.martianzoo.util.onlyElement

data class GameSetup(
    val authority: Authority,
    val players: Int,
    val bundles: Collection<String>,
    // TODO extraCards: Set<CardDefinition>,
    // TODO removeCards: Set<CardDefinition>,
    // TODO customMilestones: Set<MilestoneDefinition>,
) {
  init {
    require(players in 2..5)
    require("B" in bundles)
  }

  fun fuck(): Grid<MapAreaDefinition>? {
    val bundleCodeToGrid = authority.mapAreaDefinitions
        .values
        .associateBy { it.first().bundle }
    println(bundleCodeToGrid.keys)
    println(bundles)
    val overlap = bundleCodeToGrid.keys.intersect(bundles)
    require(overlap.size <= 1)
    return bundleCodeToGrid[overlap.firstOrNull()]
  }
  val grid: Grid<MapAreaDefinition>? = fuck()

}
