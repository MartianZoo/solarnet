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

/**
 * A specification of the starting conditions for a game. This is configuration rather than the
 * expanded runtime meaning of that configuration; [ruleset] is the separately resolved source the
 * game will use.
 */
data class GameSetup(
    /** The complete source in which [selectedBundles] are resolved. */
    val availableRuleset: TfmRuleset,

    /** Full identities of the user-selected bundles. */
    val selectedBundles: Set<ClassName>,

    /** Number of seated Players. */
    val players: Int,
    val colonyTilesDesired: Set<ClassName> = setOf(),
) {
  /** Compatibility constructor for clients that use canonical one-letter bundle codes. */
  constructor(
      availableRuleset: TfmRuleset,
      bundleString: String,
      players: Int,
      colonyTilesDesired: Set<ClassName> = setOf(),
  ) : this(
      availableRuleset,
      selectLegacyBundles(availableRuleset, bundleString, players),
      players,
      colonyTilesDesired,
  )

  private val structuredBundles = availableRuleset.bundleRulesets.any()

  /** The single resolved source used for all declarations, definitions, and custom classes. */
  val ruleset: TfmRuleset =
      if (structuredBundles) availableRuleset.resolve(selectedBundles) else availableRuleset

  /** Selected one-letter codes, retained as a client convenience during migration. */
  val bundles: Set<String> =
      if (structuredBundles) {
        selectedBundleRulesets().mapNotNull { it.legacyCode }.toSetStrict()
      } else {
        selectedBundles.map { it.toString() }.toSetStrict()
      }

  /** Selected one-letter codes concatenated in stable order. */
  val bundleString: String = bundles.sorted().joinToString("")

  init {
    require(players in 1..5) { "player count not supported: $players" }
    if (structuredBundles) {
      val availableNames = availableRuleset.bundleRulesets.map { it.bundleName }.toSet()
      require(availableNames.containsAll(selectedBundles)) {
        "supported bundles are: $availableNames"
      }
      require(TERRAFORMING_MARS in selectedBundles) { "missing TerraformingMars" }
      require(players > 1 || SOLO_MODE in selectedBundles) {
        "SoloMode is required for a one-player game"
      }
    } else {
      require("B" in bundles) { "missing base: $bundles" }
      require(availableRuleset.allBundles.containsAll(bundles)) {
        "supported bundles are: ${availableRuleset.allBundles}"
      }
    }
  }

  /** The map to use for this game. */
  val map: MarsMapDefinition =
      if (structuredBundles) ruleset.marsMapDefinitions.single()
      else ruleset.marsMapDefinitions.single { it.bundle in bundles }

  /** All [Definition] objects applicable to this game. */
  fun allDefinitions(): List<Definition> =
      if (structuredBundles) ruleset.allDefinitions.toList()
      else ruleset.allDefinitions.filter { it.bundle in bundles }

  @JsName("playerList") fun players(): List<Player> = Player.players(players)

  /** All identities that receive gameplay scopes and task queues. */
  fun actors(): List<Actor> = players() + ENGINE

  val colonyTiles: Set<ColonyTileDefinition> =
      chooseColonyTileNames().toSetStrict { ruleset.colonyTile(it) }

  private fun selectedBundleRulesets(): List<TfmRuleset.Bundle> {
    return availableRuleset.bundleRulesets.filter { it.bundleName in selectedBundles }
  }

  private fun chooseColonyTileNames(): Set<ClassName> {
    if (COLONIES_EXPANSION !in selectedBundles && "C" !in bundles) {
      return colonyTilesDesired.also { require(it.none()) }
    }
    val numTilesToUse = if (players <= 2) players + 3 else players + 2
    val need = numTilesToUse - colonyTilesDesired.size
    return when {
      need > 0 -> {
        val all = ruleset.colonyTileDefinitions.classNames()
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
      val codes = splitLetters(bundleString)
      if (ruleset.bundleRulesets.none()) return codes.map(::cn).toSetStrict()

      val byCode =
          ruleset.bundleRulesets
              .mapNotNull { bundle ->
                bundle.legacyCode?.let { it to bundle.bundleName }
              }
              .toMap()
      require(byCode.keys.containsAll(codes)) { "supported bundle codes are: ${byCode.keys}" }
      val selected = codes.map { byCode.getValue(it) }.toMutableSet()
      if (players == 1) selected += SOLO_MODE
      return selected.toSet()
    }

    fun splitLetters(bundles: String): Set<String> =
        bundles.asIterable().toStrings().sorted().toSetStrict()
  }
}
