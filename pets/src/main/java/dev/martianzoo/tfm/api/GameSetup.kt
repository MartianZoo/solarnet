package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.Definition
import dev.martianzoo.util.toSetStrict
import dev.martianzoo.util.toStrings

/**
 * A specification of the starting conditions for game. This should determine exactly what to do to
 * prepare the game up until the point of the first player decisions.
 */
data class GameSetup(
    /** Where to pull class declarations, card definitions, etc. from. */
    val authority: Authority,

    /**
     * Which bundles of cards/milestones/maps/etc. to include. For example the officially published
     * bundles are `"B"` for base `"R"` for corporate era, `"V"` for venus next, etc. This list
     * must include `B` and exactly one map (the canon maps are `"M"` for the base map, `"H"` for
     * Hellas, and `"E"` for Elysium).
     */
    val bundles: Collection<String>,

    /** Number of players. Only 2-5 are supported for now. Solo mode will take quite some work. */
    val players: Int,

    // TODO extraCards: Set<CardDefinition>,
    // TODO removeCards: Set<CardDefinition>,
    // TODO milestones: Set<MilestoneDefinition>,
) {
  constructor(
      authority: Authority,
      /**
       * A convenience when using only one-letter bundles: pass them all in one string, like
       * `"BREVX"`.
       */
      bundles: String,
      players: Int,
  ) : this(authority, splitLetters(bundles), players)

  init {
    // TODO solo mode much later
    require(players in 2..5) { "player count not supported: $players" }
    require("B" in bundles) { "missing base: $bundles" }
    require(authority.allBundles.containsAll(bundles)) { bundles }
  }

  /** The map to use for this game. */
  val map = authority.marsMapDefinitions.single { it.bundle in bundles }

  /** All [Definition] objects to use in this game. */
  fun allDefinitions(): List<Definition> =
      authority.allDefinitions.filter { it.bundle in bundles }
}

private fun splitLetters(bundles: String) = bundles.asIterable().toStrings().toSetStrict()
