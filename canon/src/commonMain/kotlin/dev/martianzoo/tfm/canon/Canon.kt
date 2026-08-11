package dev.martianzoo.tfm.canon

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GamePremise
import dev.martianzoo.data.Player
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.TfmRuleset
import dev.martianzoo.tfm.data.AwardDefinition

/** Catalog of the official bundles and the game options they provide. */
public object Canon :
    TfmRuleset.Composite(
        StandardFormBundle(
            "TerraformingMars",
            baseCustomClasses,
            setOf(
                cn("TerraformingMars"),
                cn("SoloMode"),
                cn("MultiplayerMode"),
                cn("StandardSoloVariant"),
                cn("Tr63SoloVariant"),
            ),
        ),
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
            "UtopiaCimmeriaExpansion",
            gameOptionClassNames =
                setOf(cn("UtopiaPlanitiaMapOption"), cn("TerraCimmeriaMapOption")),
        ),
        StandardFormBundle(
            "VenusNextExpansion",
            gameOptionClassNames = setOf(cn("VenusNextExpansion"), cn("WorldGovernmentOption")),
        ),
        StandardFormBundle(
            "PreludeExpansion",
            preludeCustomClasses,
            setOf(cn("PreludeExpansion")),
        ),
        StandardFormBundle(
            "ColoniesExpansion",
            coloniesCustomClasses,
            setOf(cn("ColoniesExpansion")),
        ),
        StandardFormBundle(
            "TurmoilExpansion",
            gameOptionClassNames = setOf(cn("TurmoilCardPack")),
        ),
        StandardFormBundle(
            "PromoCardsExpansion",
            promoCardsCustomClasses,
            setOf(cn("PromoCardPack")),
        ),
    ) {
  /** One positive or negative statement in a canonical game-options expression. */
  public sealed interface OptionSelection

  /** A canonical game option, named identically to its Pets component class. */
  public enum class Option : OptionSelection {
    TerraformingMars,
    SoloMode,
    MultiplayerMode,
    StandardSoloVariant,
    Tr63SoloVariant,
    CorporateEraExpansion,
    TharsisMapOption,
    HellasMapOption,
    ElysiumMapOption,
    UtopiaPlanitiaMapOption,
    TerraCimmeriaMapOption,
    VenusNextExpansion,
    PreludeExpansion,
    ColoniesExpansion,
    TurmoilCardPack,
    PromoCardPack,
    WorldGovernmentOption,
    // MandatoryVenusVariant,
    // OfferBeginnerCorpsVariant,
    ;

    public val className: ClassName = cn(name)

    public companion object {
      /** The options used when a caller does not make an explicit selection. */
      public val DEFAULTS: Set<Option> = setOf(TerraformingMars, TharsisMapOption)
    }
  }

  /** An explicit mask for defaults contributed by [option]. Prefer the [exclude] factory. */
  public data class Exclude(public val option: Option) : OptionSelection

  /** User intent: positive selections plus explicit masks for effect-contributed defaults. */
  public data class GameOptions(
      public val included: Set<Option> = Option.DEFAULTS,
      public val excluded: Set<Option> = emptySet(),
  ) {
    public constructor(
        selections: Iterable<OptionSelection>
    ) : this(
        included = selections.filterIsInstance<Option>().toSet(),
        excluded = selections.filterIsInstance<Exclude>().mapTo(linkedSetOf(), Exclude::option),
    )

    init {
      require(included.intersect(excluded).isEmpty()) {
        "game options cannot be both included and excluded"
      }
    }
  }

  /** An unconfigured canonical setup world, ready to receive a Pets setup instruction. */
  public fun emptySetupWorldDefinition(): GamePremise =
      GamePremise(
          CanonSetupRuleset,
          CanonSetupRuleset.explicitClassDeclarations.mapTo(linkedSetOf()) { it.className },
          listOf(ENGINE),
          emptyList(),
      )

  /** Definition used to construct an independent canonical setup world. */
  public fun setupWorldDefinition(
      players: Int,
      options: GameOptions = GameOptions(),
      selectedColonies: Set<ClassName> = emptySet(),
  ): GamePremise {
    require(players in 1..5) { "player count must be between 1 and 5" }
    val mode = if (players == 1) Option.SoloMode else Option.MultiplayerMode
    val initialComponents = buildList {
      add("$players Player")
      options.excluded.mapTo(this) { "Exclude<Class<${it.className}>>" }
      (options.included + mode).mapTo(this) { it.className.toString() }
      selectedColonies.mapTo(this) { "${it}Selected" }
    }
    return GamePremise(
        CanonSetupRuleset,
        CanonSetupRuleset.explicitClassDeclarations.mapTo(linkedSetOf()) { it.className },
        listOf(ENGINE),
        initialComponents,
    )
  }

  /** Convenience for callers whose selections contain no explicit default masks. */
  public fun setupWorldDefinition(
      players: Int,
      options: Set<Option>,
      selectedColonies: Set<ClassName> = emptySet(),
  ): GamePremise = setupWorldDefinition(players, GameOptions(options), selectedColonies)

  /** Snapshots a validated canonical setup world for an independent playable game. */
  public fun assemble(setupWorld: GameReader): GamePremise {
    require(setupWorld.ruleset === CanonSetupRuleset) { "not a canonical setup world" }
    val players = setupWorld.getComponents("Player").size

    val enabledOptions =
        setupWorld.getComponents("GameOption").elements.mapTo(linkedSetOf()) { it.className }

    val bundleNames =
        enabledOptions.mapTo(linkedSetOf(cn("TerraformingMars"))) { option ->
          setupOptionBundles[option]
              ?: throw IllegalArgumentException("unknown game option: $option")
        }
    val selectedRuleset = resolve(bundleNames, setupWorld)
    val ruleset: TfmRuleset =
        if (SOLO_MODE in enabledOptions) WithoutAwards(selectedRuleset) else selectedRuleset
    val setupOptions = setupWorld.getComponents("GameOption").elements
    val selectedColonies = setupWorld.getComponents("SelectedColonyTile").elements
    val setupComponents = setupOptions + selectedColonies
    val initialComponents = setupComponents.map { it.expression.toString() }
    val componentRoots = setupComponents.mapTo(linkedSetOf()) { it.className }
    val gameplayClassRoots =
        setupWorld.getComponents("GameplayClassRoot").elements.mapTo(linkedSetOf()) { it.className }
    val roots =
        componentRoots +
            gameplayClassRoots +
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
    val result = buildMap {
      bundles.forEach { bundle ->
        bundle.gameOptionClassNames.forEach { option ->
          require(put(option, bundle.bundleName) == null) {
            "multiple setup bundles provide $option"
          }
        }
      }
    }
    require(result.keys == Option.entries.mapTo(linkedSetOf()) { it.className }) {
      "canonical option enum does not match the available setup options"
    }
    result
  }

  private val TERRAFORMING_MARS = cn("TerraformingMars")
  private val SOLO_MODE = cn("SoloMode")

  private class WithoutAwards(ruleset: TfmRuleset) : TfmRuleset.Composite(ruleset) {
    override val awardDefinitions: Set<AwardDefinition> = emptySet()
  }
}

/** Counteracts defaults for [option] in the same expression as positive options. */
public fun exclude(option: Canon.Option): Canon.OptionSelection = Canon.Exclude(option)
