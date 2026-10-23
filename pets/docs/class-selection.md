# Which classes belong to a configured game?

A class can be **known** to Canon, **selected** in a game's class table, and **inhabited**
(have a usable concrete type). These are different questions. None means that a card has been
drawn or a component created during play.

For example, the default game knows `VenusStep` but does not select it. A solo game explicitly
selecting `Award` includes that abstract class without any concrete awards. Explicitly selecting
`Camp` includes the resource class without making it inhabited in the absence of its card.

`Content` identifies concrete authored game items. Supporting classes such as `Camp`, card backs,
and map areas are not Content. A `MarsMap` is Content; its areas are selected with it through
existing map selections, without an additional classification. See
[Catalogs and game premises](agents/CATALOG.md) for how
selection works and [the Pets roadmap](../../docs/PETS_ROADMAP.md) for unfinished policy work.

## Reading and checking the examples

Configurations below use two players unless stated otherwise. **Default** means an empty
configuration. `-Name` excludes an item; `name:cards` selects a card pack. “Absent” means
unselected, not unknown to Canon. An invalid configuration is rejected rather than producing a
game in which its requested content is absent.

[ClassTableSelectionTest](../../test/common/dev/martianzoo/tfm/tests/rules/ClassTableSelectionTest.kt)
is the executable scenario inventory, grouped in the same order as this guide. It checks concrete
configurations and keeps distinct positive, negative, and override cases. The tables explain the
rules through representative examples rather than repeat every assertion or catalog entry.

## Players and variants

| Scenario | Selected | Absent |
| --- | --- | --- |
| Default solo | `SoloOpponent`, `StandardSoloObjective` | `ClaimMilestone`, `Player2`, awards and scoring places |
| Default two-player | `Player2`, `ClaimMilestone`, `FirstPlace` | `SoloOpponent`, `SecondPlace` |
| Three-player | `FirstPlace`, `SecondPlace` | — |
| Solo with `Tr63SoloObjective` | `Tr63SoloObjective` | `StandardSoloObjective` |
| Default Corporate Era | `CopyProductionBox` | `QuickStartVariant` |
| `-CorporateEraExpansion` | `QuickStartVariant` | `CopyProductionBox` |
| Default beginner setting | `StandardOption` | `BeginnerOption`, `BeginnerCorporation1`, `BeginnerCard` |
| `BeginnerVariant` | `BeginnerOption`, `BeginnerCorporation1`, `BeginnerCard` | — |
| Default solo without colony selection | — | `SelectedColonyTile`, `SoloColoniesSetup` |
| Solo with `ColoniesExpansion, Callisto` | `SelectedColonyTile`, `SoloColoniesSetup` | — |
| Multiplayer with `ColoniesExpansion, Callisto` | — | `SoloColoniesSetup` |

The suite also checks that multiplayer with Prelude and Venus does not bring in solo classes.

## Maps, goals, and replacements

The suite checks every map: the map is Content, its areas are not, all its areas are selected,
and other maps' areas are omitted. The Hellas example separately checks direct area selection.

| Rule | Example and checked contrast |
| --- | --- |
| A map supplies its areas; an area can also be selected directly. | `Hellas_1_1` is absent by default, present with `HellasMap`, and present when explicitly selected. |
| Amazonis replaces the standard parameter tracks. | `VenusNextExpansion, AmazonisMap` selects `ExtendedGlobalParametersRule` and `ExtendedVenusTrackRules`, omitting both standard track rules. Default Mars and Venus games retain their respective standard rules. |
| A goal may need both a map and an expansion. | `Lobbyist` requires Amazonis and Turmoil together; either alone omits it. |
| Explicit goals and their support follow the chosen pool. | `Briber` is absent until chosen. Choosing Builder, Legend, and Merchant omits Hydrologist, Diversifier, and their support; Hellas includes Diversifier and its watcher. |
| Replacements omit the superseded item. | The promo pack selects `DeimosDownPromo` instead of `DeimosDown`; Elysium with Quick Start selects `Generalist2` instead of `Generalist`. Both original configurations are also checked. |

## Eligibility across expansions

These cases test automatic eligibility, separately from rejecting an explicitly requested item
whose required module is missing.

| Configuration change | Result |
| --- | --- |
| Add `prelude2:cards` | Includes `Ecotec`; `AtmosphericEnhancers` still needs Venus Next. |
| Omit the promo pack | Promo global events remain absent even with Turmoil, Venus, and Colonies. |
| Add promos and Turmoil, with Colonies only | `JovianColonyBoom` is available; the Venus-dependent events remain absent. |
| Add promos and Turmoil, with Venus only | `MartianProtectionism` is available; the colony-dependent events remain absent. |
| Include both Venus and Colonies with promos and Turmoil | `TitanVenusAlliance` and `ImmigrationToVenus` are available. |
| Select promos and Utopia without Corporate Era | Prelude card backs can be selected without `PreludePhase`; Colonies-only declarations remain absent. |
| Select promos and Cimmeria without Corporate Era | References to Venus do not select `VenusStep` or `VenusTag`. |

## Supporting declarations

| Support relationship | Evidence |
| --- | --- |
| A Prelude brings its card back. | `AcquiredSpaceAgency` brings `PreludeCard`; `CheungShingMars` does not. |
| A card's private resource follows its inclusion. | Colonies with Callisto includes Refugee Camps and `Camp`; excluding Refugee Camps omits both. Directly selecting `Camp` includes it but leaves it uninhabited. |
| Shared resources can follow either provider. | Either `PharmacyUnion` or `Hospitals` brings `Disease` without the promo pack. |
| Instructions and markers follow their card. | `IcyImpactors` brings `ChooseOceanArea`; `MarsNomads` brings `NomadsMarker`. Excluding Mars Nomads from promos omits its marker too. |

## Modules, packs, and explicit choices

The module matrix covers Prelude, Venus, Colonies, and Turmoil: their rules, associated content,
standalone cards, dependent cards, and private support. These are distinct selection operations:

| Operation | Representative evidence |
| --- | --- |
| Select a module normally. | Venus brings `VenusStep` and `AirScrappingProject`; Colonies brings `TradeAction` and `BuildColonyProject`; Turmoil brings parties and events. |
| Select cards without the module. | `prelude:cards` brings Vitor and Prelude cards/back, but neither `PreludeExpansion` nor `PreludePhase`. |
| Exclude a module's card pack. | `PreludeExpansion, -prelude:cards` keeps Prelude rules and its card back but omits the pack's cards. |
| Override a pack exclusion with an explicit card. | Adding Vitor, Valley Trust, Acquired Space Agency, or House Printing individually restores that card. |
| Explicitly include content already supplied by a module or pack. | Vitor remains selected alongside either `PreludeExpansion` or `prelude:cards`; `VenusNextExpansion, Manutech` covers a module without a card pack. |
| Exclude an individual card. | Vitor and `NonNegativeIconsOf` are omitted under both the Prelude module and the standalone pack. The module matrix also checks Venus, Colonies, and Turmoil exclusions. |
| Select compatible content directly. | Standalone cards such as `Manutech` and `LakefrontResorts` need no module or pack. Compatible Turmoil corporations also work as a group without Turmoil's rules. |
| Explicitly request module-dependent content. | `CryoSleep`, `AerialLenses`, and `DemocraticReform` without their modules are invalid configurations. With the required module, the tested dependent cards are selected. |
| Exclude a global event. | Excluding `DemocraticReform` or `MinimalImpactPolicy` removes their private support; Turmoil's rules remain. |
| Exclude every Turmoil card individually. | Turmoil still selects its parties and global events. |

## How selected premises simplify effects

[PremiseEffectsTest](../../test/common/dev/martianzoo/tfm/tests/rules/PremiseEffectsTest.kt)
owns effect specialization. These checks are separate from the selection inventory because they
compare executable effects with unchanged authored declarations.

- Vitor's award guard disappears in solo without pulling awards or scoring places into the game.
- Amazonis's class-literal delegate guard disappears without Turmoil and unwraps with Turmoil.
- Selected game mode and module presence simplify the `ModulesReady` and Cimmeria effects.
- Colonies' mixed condition loses its fixed module condition but keeps `SelectedColonyTile`.
- Milestone conditions and the player threshold in `MeasureAward` remain live; their original
  effect objects are retained, including in both two- and three-player games.
