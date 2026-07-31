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
import dev.martianzoo.tfm.data.GameOptions

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
            gameOptionClassNames = setOf(cn("TharsisMap")),
        ),
        StandardFormBundle(
            "HellasMap",
            gameOptionClassNames = setOf(cn("HellasMap")),
        ),
        StandardFormBundle(
            "ElysiumMap",
            gameOptionClassNames = setOf(cn("ElysiumMap")),
        ),
        StandardFormBundle(
            "TerraCimmeriaMap",
            gameOptionClassNames = setOf(cn("TerraCimmeriaMap")),
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
      listOf("CorporateEraExpansion", "TharsisMap")

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
    val ruleset = resolve(bundleNames)
    val setupOptions = setupWorld.getComponents("GameOption").elements
    val selectedColonies = setupWorld.getComponents("SelectedColonyTile").elements
    val initialComponents = (setupOptions + selectedColonies).map { it.expression.toString() }
    val componentRoots = (setupOptions + selectedColonies).mapTo(linkedSetOf()) { it.className }
    val roots = componentRoots + ruleset.allDefinitions.classNames() + cn("TerraformingMars")
    return GamePremise(ruleset, roots, Player.players(players) + ENGINE, initialComponents)
  }

  /** A minimal two-player game using the base game and Tharsis map. */
  public val SIMPLE_GAME: GamePremise by lazy {
    gamePremise(GameOptions(2, setOf(TERRAFORMING_MARS, THARSIS_MAP)))
  }

  /** A minimal solo game using the base game, solo mode, and Tharsis map. */
  public val SIMPLE_SOLO_GAME: GamePremise by lazy {
    gamePremise(GameOptions(1, setOf(TERRAFORMING_MARS, SOLO_MODE, THARSIS_MAP)))
  }

  /** Returns the bundle identities required to provide [options]. */
  public fun bundleNames(options: GameOptions): Set<ClassName> =
      options.enabled.mapTo(linkedSetOf()) { option ->
        OPTION_BUNDLES[option] ?: throw IllegalArgumentException("unknown game option: $option")
      }

  /** Resolves exact Terraforming Mars options into generic engine construction inputs. */
  public fun gamePremise(options: GameOptions): GamePremise {
    require(TERRAFORMING_MARS in options) { "missing TerraformingMars option" }
    require(options.players > 1 || SOLO_MODE in options) {
      "SoloMode is required for a one-player game"
    }
    val expectedColonyCount = requiredColonyTileCount(options.players)
    if (COLONIES in options) {
      if (options.deferredColonySelection) {
        require(options.colonyTiles.isEmpty()) {
          "deferred colony selection cannot also specify colony tiles"
        }
      } else {
        require(options.colonyTiles.size == expectedColonyCount) {
          "ColoniesExpansion requires exactly $expectedColonyCount colony tiles"
        }
      }
    } else {
      require(options.colonyTiles.isEmpty()) {
        "colony tiles require the ColoniesExpansion option"
      }
      require(!options.deferredColonySelection) {
        "deferred colony selection requires the ColoniesExpansion option"
      }
    }

    val ruleset = resolve(bundleNames(options))
    ruleset.marsMapDefinitions.single()
    val selectionClassNames = options.colonyTiles.mapTo(linkedSetOf()) { cn("${it}Selected") }
    val configurationRoots =
        options.enabled +
            selectionClassNames +
            setOf(cn("DeferredColonySelection")).filter { COLONIES in options }
    val initialComponents =
        options.enabled.map(ClassName::toString) +
            selectionClassNames.map(ClassName::toString) +
            listOf("DeferredColonySelection").filter { options.deferredColonySelection }
    return GamePremise(
        ruleset,
        configurationRoots + ruleset.allDefinitions.classNames(),
        Player.players(options.players) + ENGINE,
        initialComponents,
    )
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
  private val SOLO_MODE = cn("SoloMode")
  private val CORPORATE_ERA = cn("CorporateEraExpansion")
  private val THARSIS_MAP = cn("TharsisMap")
  private val HELLAS_MAP = cn("HellasMap")
  private val ELYSIUM_MAP = cn("ElysiumMap")
  private val TERRA_CIMMERIA_MAP = cn("TerraCimmeriaMap")
  private val VENUS_NEXT = cn("VenusNextExpansion")
  private val PRELUDE = cn("PreludeExpansion")
  private val COLONIES = cn("ColoniesExpansion")
  private val TURMOIL_CARD_PACK = cn("TurmoilCardPack")
  private val PROMO_CARD_PACK = cn("PromoCardPack")
  private val TURMOIL_BUNDLE = cn("TurmoilExpansion")
  private val PROMO_CARDS_BUNDLE = cn("PromoCardsExpansion")

  private val OPTION_BUNDLES =
      mapOf(
          TERRAFORMING_MARS to TERRAFORMING_MARS,
          SOLO_MODE to TERRAFORMING_MARS,
          CORPORATE_ERA to CORPORATE_ERA,
          THARSIS_MAP to THARSIS_MAP,
          HELLAS_MAP to HELLAS_MAP,
          ELYSIUM_MAP to ELYSIUM_MAP,
          TERRA_CIMMERIA_MAP to TERRA_CIMMERIA_MAP,
          VENUS_NEXT to VENUS_NEXT,
          PRELUDE to PRELUDE,
          COLONIES to COLONIES,
          TURMOIL_CARD_PACK to TURMOIL_BUNDLE,
          PROMO_CARD_PACK to PROMO_CARDS_BUNDLE,
      )
}
