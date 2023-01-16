package dev.martianzoo.tfm.api

import dev.martianzoo.util.onlyElement

data class GameSetup(
    val authority: Authority,
    val bundles: Collection<String>,
    val players: Int,
    // TODO extraCards: Set<CardDefinition>,
    // TODO removeCards: Set<CardDefinition>,
    // TODO milestones: Set<MilestoneDefinition>,
) {
  constructor(authority: Authority, bundles: String, players: Int) :
      this(authority, bundles.asIterable().map { "$it" }, players)

  init {
    require(players in 2..5) // TODO solo mode much later
    require("B" in bundles)
  }

  val map = authority.marsMapDefinitions
      .filter { it.bundle in bundles }
      .onlyElement()
}
