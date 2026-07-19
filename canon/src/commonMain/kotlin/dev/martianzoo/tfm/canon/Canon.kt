package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.data.GameOptions
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.util.toSetStrict

/** Catalog of the official bundles and the game options they provide. */
public object Canon :
    TfmRuleset.Composite(
        StandardFormBundle("TerraformingMars", baseCustomClasses),
        StandardFormBundle("CorporateEraExpansion", corporateEraCustomClasses),
        StandardFormBundle("TharsisMap", areaShortNamePrefix = "M"),
        StandardFormBundle("HellasMap", areaShortNamePrefix = "H"),
        StandardFormBundle("ElysiumMap", areaShortNamePrefix = "E"),
        StandardFormBundle("VenusNextExpansion"),
        StandardFormBundle("PreludeExpansion", preludeCustomClasses),
        StandardFormBundle("ColoniesExpansion", coloniesCustomClasses),
        StandardFormBundle("TurmoilExpansion"),
        StandardFormBundle("PromoCardsBundle", promoCardsCustomClasses),
    ) {
  /** A minimal two-player game using the base game and Tharsis map. */
  public val SIMPLE_GAME: GameSetup by lazy { gameSetup(options("BM", 2)) }

  /** A minimal solo game using the base game, solo mode, and Tharsis map. */
  public val SIMPLE_SOLO_GAME: GameSetup by lazy { gameSetup(options("BSM", 1)) }

  /** Creates exact game options from a client's one-letter option syntax. */
  public fun options(
      optionCodes: String,
      players: Int,
      colonyTiles: Set<ClassName> = emptySet(),
  ): GameOptions {
    val codes = optionCodes.asIterable().map(Char::toString).toSetStrict()
    require(OPTIONS_BY_CODE.keys.containsAll(codes)) {
      "supported option codes are: ${OPTIONS_BY_CODE.keys}"
    }
    return GameOptions(
        players,
        codes.mapTo(linkedSetOf()) { OPTIONS_BY_CODE.getValue(it) },
        colonyTiles,
    )
  }

  /** Returns the bundle identities required to provide [options]. */
  public fun bundleNames(options: GameOptions): Set<ClassName> =
      options.enabled.mapTo(linkedSetOf()) { option ->
        OPTION_BUNDLES[option] ?: throw IllegalArgumentException("unknown game option: $option")
      }

  /** Assembles the selected ruleset and fully specified setup for [options]. */
  public fun gameSetup(options: GameOptions): GameSetup =
      GameSetup(resolve(bundleNames(options)), options)

  /** Creates a fully specified setup from one-letter option codes. */
  public fun fromOptionCodes(
      optionCodes: String,
      players: Int,
      colonyTiles: Set<ClassName> = emptySet(),
  ): GameSetup = gameSetup(options(optionCodes, players, colonyTiles))

  /** Formats known options using the client's one-letter syntax. */
  public fun optionCodes(options: GameOptions): String =
      options.enabled.mapNotNull(OPTION_CODES::get).joinToString("")

  public val supportedOptionCodes: Set<String>
    get() = OPTIONS_BY_CODE.keys

  public val mapOptionCodes: Set<String> = setOf("E", "H", "M")

  private val TERRAFORMING_MARS = cn("TerraformingMars")
  private val SOLO_MODE = cn("SoloMode")
  private val CORPORATE_ERA = cn("CorporateEraExpansion")
  private val THARSIS_MAP = cn("TharsisMap")
  private val HELLAS_MAP = cn("HellasMap")
  private val ELYSIUM_MAP = cn("ElysiumMap")
  private val VENUS_NEXT = cn("VenusNextExpansion")
  private val PRELUDE = cn("PreludeExpansion")
  private val COLONIES = cn("ColoniesExpansion")
  private val TURMOIL = cn("TurmoilExpansion")
  private val PROMOS = cn("PromoCardsBundle")

  private val OPTIONS_BY_CODE =
      linkedMapOf(
          "B" to TERRAFORMING_MARS,
          "S" to SOLO_MODE,
          "R" to CORPORATE_ERA,
          "M" to THARSIS_MAP,
          "H" to HELLAS_MAP,
          "E" to ELYSIUM_MAP,
          "V" to VENUS_NEXT,
          "P" to PRELUDE,
          "C" to COLONIES,
          "T" to TURMOIL,
          "X" to PROMOS,
      )
  private val OPTION_CODES = OPTIONS_BY_CODE.entries.associate { (code, option) -> option to code }
  private val OPTION_BUNDLES =
      mapOf(
          TERRAFORMING_MARS to TERRAFORMING_MARS,
          SOLO_MODE to TERRAFORMING_MARS,
          CORPORATE_ERA to CORPORATE_ERA,
          THARSIS_MAP to THARSIS_MAP,
          HELLAS_MAP to HELLAS_MAP,
          ELYSIUM_MAP to ELYSIUM_MAP,
          VENUS_NEXT to VENUS_NEXT,
          PRELUDE to PRELUDE,
          COLONIES to COLONIES,
          TURMOIL to TURMOIL,
          PROMOS to PROMOS,
      )
}
