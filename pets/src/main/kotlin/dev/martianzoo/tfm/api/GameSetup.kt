package dev.martianzoo.tfm.api

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

  val map = authority.mapDefinitions
      .filter { it.bundle in bundles }
      .onlyElement()
}
