package dev.martianzoo.tfm.data

import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Definition
import dev.martianzoo.data.Player
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset
import kotlin.js.JsName

/** A fully specified, non-random configuration for one game. */
data class GameSetup(
    /** The ruleset already assembled from exactly the bundles needed by [options]. */
    val ruleset: TfmRuleset,

    /** Exact semantic choices for this game. */
    val options: GameOptions,
) {
  val players: Int by options::players

  init {
    require(TERRAFORMING_MARS in options) { "missing TerraformingMars option" }
    require(ruleset.allClassNames.containsAll(options.enabled)) {
      "ruleset does not provide options: ${options.enabled - ruleset.allClassNames}"
    }
    require(players > 1 || SOLO_MODE in options) {
      "SoloMode is required for a one-player game"
    }
    val expectedColonyCount = if (players <= 2) players + 3 else players + 2
    if (COLONIES_EXPANSION in options) {
      require(options.colonyTiles.size == expectedColonyCount) {
        "ColoniesExpansion requires exactly $expectedColonyCount colony tiles"
      }
    } else {
      require(options.colonyTiles.isEmpty()) {
        "colony tiles require the ColoniesExpansion option"
      }
    }
  }

  /** The map to use for this game. */
  val map: MarsMapDefinition = ruleset.marsMapDefinitions.single()

  /** All [Definition] objects applicable to this game. */
  fun allDefinitions(): List<Definition> = ruleset.allDefinitions.toList()

  @JsName("playerList") fun players(): List<Player> = Player.players(players)

  /** All identities that receive gameplay scopes and task queues. */
  fun actors(): List<Actor> = players() + ENGINE

  val colonyTiles: Set<ColonyTileDefinition> =
      options.colonyTiles.mapTo(linkedSetOf(), ruleset::colonyTile)

  private companion object {
    val TERRAFORMING_MARS = cn("TerraformingMars")
    val COLONIES_EXPANSION = cn("ColoniesExpansion")
    val SOLO_MODE = cn("SoloMode")
  }
}
