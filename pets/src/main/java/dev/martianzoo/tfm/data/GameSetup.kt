package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.api.Authority
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.util.random
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
     * bundles are `"B"` for base `"R"` for corporate era, `"V"` for venus next, etc. This list must
     * include `B` and exactly one map (the canon maps are `"M"` for the base map, `"H"` for Hellas,
     * and `"E"` for Elysium).
     */
    val bundleString: String,

    /** Number of players. Only 2-5 are supported for now. Solo mode will take quite some work. */
    val players: Int,

    /** */
    val colonyTilesDesired: Set<ClassName> = setOf(),
) {
  val bundles = splitLetters(bundleString)

  init {
    require(players in 1..5) { "player count not supported: $players" }
    require("B" in bundles) { "missing base: $bundles" }
    require(authority.allBundles.containsAll(bundles)) {
      "supported bundles are: ${authority.allBundles}"
    }
  }

  /** The map to use for this game. */
  val map = authority.marsMapDefinitions.single { it.bundle in bundles }

  /** All [Definition] objects to use in this game. */
  fun allDefinitions(): List<Definition> = authority.allDefinitions.filter { it.bundle in bundles }

  fun players(): List<Player> = Player.players(players)

  val colonyTiles: Set<ColonyTileDefinition> =
      chooseColonyTileNames().toSetStrict { authority.colonyTile(it) }

  private fun chooseColonyTileNames(): Set<ClassName> {
    if ("C" !in bundles) return colonyTilesDesired.also { require(it.none()) }
    val numTilesToUse = if (players <= 2) players + 3 else players + 2
    val need = numTilesToUse - colonyTilesDesired.size
    return when {
      // Didn't give enough? We choose the rest
      need > 0 -> {
        val all = authority.colonyTileDefinitions.classNames()
        colonyTilesDesired + random(all - colonyTilesDesired, need)
      }
      // Chose too many? We choose from those
      need < 0 -> random(colonyTilesDesired, numTilesToUse)
      else -> colonyTilesDesired
    }
  }

  private companion object {
    fun splitLetters(bundles: String) = bundles.asIterable().toStrings().sorted().toSetStrict()
  }
}
