package dev.martianzoo.tfm.data

import dev.martianzoo.data.Actor
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Definition
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.util.random
import dev.martianzoo.util.toSetStrict
import dev.martianzoo.util.toStrings
import kotlin.js.JsName

/** A specification of the starting conditions for a game. */
data class GameSetup(
    /** The complete source from which this game's content will be selected. */
    val ruleset: TfmRuleset,

    /** Full identities of the selected bundles. */
    val bundles: Set<ClassName>,

    /** Number of seated Players. */
    val players: Int,
    val colonyTilesDesired: Set<ClassName> = setOf(),
) {
  /** Compatibility constructor for clients that use canonical one-letter bundle codes. */
  constructor(
      ruleset: TfmRuleset,
      bundleString: String,
      players: Int,
      colonyTilesDesired: Set<ClassName> = setOf(),
  ) : this(
      ruleset,
      selectLegacyBundles(ruleset, bundleString, players),
      players,
      colonyTilesDesired,
  )

  /** Selected one-letter codes concatenated in stable order. */
  val bundleString: String =
      selectedBundleRulesets().mapNotNull { it.legacyCode }.sorted().joinToString("")

  init {
    require(players in 1..5) { "player count not supported: $players" }
    val availableNames = ruleset.bundles.map { it.bundleName }.toSet()
    require(availableNames.containsAll(bundles)) { "supported bundles are: $availableNames" }
    require(TERRAFORMING_MARS in bundles) { "missing TerraformingMars" }
    require(players > 1 || SOLO_MODE in bundles) {
      "SoloMode is required for a one-player game"
    }
  }

  /** The map to use for this game. */
  val map: MarsMapDefinition = resolvedRuleset().marsMapDefinitions.single()

  /** All [Definition] objects applicable to this game. */
  fun allDefinitions(): List<Definition> = resolvedRuleset().allDefinitions.toList()

  @JsName("playerList") fun players(): List<Player> = Player.players(players)

  /** All identities that receive gameplay scopes and task queues. */
  fun actors(): List<Actor> = players() + ENGINE

  val colonyTiles: Set<ColonyTileDefinition> =
      chooseColonyTileNames().toSetStrict { resolvedRuleset().colonyTile(it) }

  private fun resolvedRuleset(): TfmRuleset = ruleset.resolve(bundles)

  private fun selectedBundleRulesets(): List<TfmRuleset.Bundle> =
      ruleset.bundles.filter { it.bundleName in bundles }

  private fun chooseColonyTileNames(): Set<ClassName> {
    if (COLONIES_EXPANSION !in bundles) {
      return colonyTilesDesired.also { require(it.none()) }
    }
    val numTilesToUse = if (players <= 2) players + 3 else players + 2
    val need = numTilesToUse - colonyTilesDesired.size
    return when {
      need > 0 -> {
        val all = resolvedRuleset().colonyTileDefinitions.classNames()
        colonyTilesDesired + random(all - colonyTilesDesired, need)
      }
      need < 0 -> random(colonyTilesDesired, numTilesToUse)
      else -> colonyTilesDesired
    }
  }

  private companion object {
    val TERRAFORMING_MARS = cn("TerraformingMars")
    val COLONIES_EXPANSION = cn("ColoniesExpansion")
    val SOLO_MODE = cn("SoloMode")

    fun selectLegacyBundles(
        ruleset: TfmRuleset,
        bundleString: String,
        players: Int,
    ): Set<ClassName> {
      val codes = bundleString.asIterable().toStrings().sorted().toSetStrict()
      val byCode =
          ruleset.bundles
              .mapNotNull { bundle -> bundle.legacyCode?.let { it to bundle.bundleName } }
              .toMap()
      require(byCode.keys.containsAll(codes)) { "supported bundle codes are: ${byCode.keys}" }
      return codes
          .mapTo(mutableSetOf()) { byCode.getValue(it) }
          .apply {
            if (players == 1) add(SOLO_MODE)
          }
    }
  }
}
