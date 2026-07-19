package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName

/** Exact semantic choices for a game, independent of how Canon stores their ruleset data. */
data class GameOptions(
    /** Number of seated Players. */
    val players: Int,

    /** Enabled game-rule and content options, identified by their Pets class names. */
    val enabled: Set<ClassName>,

    /** Exact colony tiles to use; empty when the Colonies option is not enabled. */
    val colonyTiles: Set<ClassName> = emptySet(),
) {
  init {
    require(players in 1..5) { "player count not supported: $players" }
  }

  operator fun contains(option: ClassName): Boolean = option in enabled
}
