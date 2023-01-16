package dev.martianzoo.tfm.api

import dev.martianzoo.util.onlyElement
import dev.martianzoo.util.toSetStrict

data class GameSetup(
    val authority: Authority,
    val bundles: Collection<String>,
    val players: Int,
    // TODO extraCards: Set<CardDefinition>,
    // TODO removeCards: Set<CardDefinition>,
    // TODO milestones: Set<MilestoneDefinition>,
) {
  constructor(authority: Authority, bundles: String, players: Int) :
      this(authority, splitLetters(bundles), players)

  init {
    // TODO solo mode much later
    require(players in 2..5) { "player count not supported: $players" }
    require("B" in bundles) { "missing base: $bundles" }
    require(authority.allBundles.containsAll(bundles)) { bundles }
  }

  val map = authority.marsMapDefinitions
      .filter { it.bundle in bundles }
      .onlyElement()
}

private fun splitLetters(bundles: String) =
    bundles.asIterable().map { "$it" }.toSetStrict()
