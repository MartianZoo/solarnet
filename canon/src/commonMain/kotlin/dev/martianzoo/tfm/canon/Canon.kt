package dev.martianzoo.tfm.canon

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.Player
import dev.martianzoo.data.Ruleset
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset

/** Catalog of the official bundles and the game options they provide. */
public object Canon :
    TfmRuleset.Composite(
        StandardFormBundle("TerraformingMars", baseCustomClasses, setOf(cn("SoloMode"))),
        StandardFormBundle(
            "CorporateEraExpansion",
            corporateEraCustomClasses,
            setOf(cn("CorporateEraExpansion")),
        ),
        StandardFormBundle(
            "TharsisMap",
            gameOptionClassNames = setOf(cn("TharsisMapOption")),
        ),
        StandardFormBundle(
            "HellasElysiumExpansion",
            gameOptionClassNames = setOf(cn("HellasMapOption"), cn("ElysiumMapOption")),
        ),
        StandardFormBundle(
            "TerraCimmeriaMap",
            gameOptionClassNames = setOf(cn("TerraCimmeriaMapOption")),
        ),
        StandardFormBundle(
            "VenusNextExpansion",
            gameOptionClassNames = setOf(cn("VenusNextExpansion")),
        ),
        StandardFormBundle("PreludeExpansion", preludeCustomClasses, setOf(cn("PreludeExpansion"))),
        StandardFormBundle(
            "ColoniesExpansion",
            coloniesCustomClasses,
            setOf(cn("ColoniesExpansion")),
        ),
        StandardFormBundle("TurmoilExpansion", gameOptionClassNames = setOf(cn("TurmoilCardPack"))),
        StandardFormBundle(
            "PromoCardsExpansion",
            promoCardsCustomClasses,
            setOf(cn("PromoCardPack")),
        ),
    ) {
  /** The canonical type universe used by setup worlds. */
  public val setupRuleset: Ruleset = CanonSetupRuleset

  /** Classes loaded into the canonical setup world's independent type universe. */
  public val setupRootClassNames: Set<ClassName>
    get() = setupRuleset.explicitClassDeclarations.mapTo(linkedSetOf()) { it.className }

  /** Default components of a newly created canonical setup world. */
  public val setupWorldInitialComponents: List<String> =
      listOf("CorporateEraExpansion", "TharsisMapOption")

  /** Snapshots a validated canonical setup world for an independent playable game. */
  public fun assemble(setupWorld: GameReader): GamePremise {
    require(setupWorld.ruleset === setupRuleset) { "not a canonical setup world" }
    val players = setupWorld.getComponents("Player").size

    val enabledOptions =
        setupWorld.getComponents("GameOption").elements.mapTo(linkedSetOf()) { it.className }

    val bundleNames =
        enabledOptions.mapTo(linkedSetOf(cn("TerraformingMars"))) { option ->
          setupOptionBundles[option]
              ?: throw IllegalArgumentException("unknown game option: $option")
        }
    val ruleset = resolve(bundleNames, setupWorld)
    val setupOptions = setupWorld.getComponents("GameOption").elements
    val selectedColonies = setupWorld.getComponents("SelectedColonyTile").elements
    val deferredColonySelection = setupWorld.getComponents("DeferredColonySelection").elements
    val setupComponents = setupOptions + selectedColonies + deferredColonySelection
    val initialComponents = setupComponents.map { it.expression.toString() }
    val componentRoots = setupComponents.mapTo(linkedSetOf()) { it.className }
    val deferredSelectionRoots =
        setOf(cn("DeferredColonySelection")).filter { COLONIES in enabledOptions }
    val roots =
        componentRoots +
            deferredSelectionRoots +
            ruleset.allDefinitions.classNames() +
            TERRAFORMING_MARS
    return GamePremise(ruleset, roots, Player.players(players) + ENGINE, initialComponents)
  }

  /** Number of colony tiles required for a game having [players] seated players. */
  public fun requiredColonyTileCount(players: Int): Int {
    require(players in 1..5) { "player count must be between 1 and 5" }
    return when (players) {
      1 -> 3
      2 -> 5
      else -> players + 2
    }
  }

  private val setupOptionBundles by lazy {
    buildMap {
      bundles.forEach { bundle ->
        bundle.gameOptionClassNames.forEach { option ->
          require(put(option, bundle.bundleName) == null) {
            "multiple setup bundles provide $option"
          }
        }
      }
    }
  }

  private val TERRAFORMING_MARS = cn("TerraformingMars")
  private val COLONIES = cn("ColoniesExpansion")
}
