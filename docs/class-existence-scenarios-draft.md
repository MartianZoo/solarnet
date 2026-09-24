# Draft: When does a class exist in a game?

This is a guide to **tested class-selection boundaries**, not an inventory of the catalog. A class *exists* here when it is included in a configured game's class table. That does not mean a card has been drawn or a component has been created. A class may be known to the catalog while absent from a particular game.

Examples use two players unless they say **solo**. An empty configuration means the default game. A minus sign, as in `-RefugeeCamps`, explicitly excludes something. Each group names the test that checks its statements.

## 1. The starting game and its players

### Player count

Test: [ClassDefinitionBoundaryTest, “solo rules and player seats follow the number of players”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- In a **solo** default game, `SoloOpponent` exists.
- In a **two-player** default game, `SoloOpponent` does not exist.
- In a **solo** default game, `ClaimMilestoneAction` does not exist.
- In a **two-player** default game, `ClaimMilestoneAction` exists.
- In a **solo** default game, `Player2` does not exist.
- In a **two-player** default game, `Player2` exists.

### The solo victory objective

Test: [ClassDefinitionBoundaryTest, “the TR 63 solo objective replaces the standard objective”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- In a **solo** default game, `StandardSoloObjective` exists.
- In a **solo** default game, `Tr63SoloObjective` does not exist.
- In a **solo** game configured with `Tr63SoloObjective`, `Tr63SoloObjective` exists.
- In a **solo** game configured with `Tr63SoloObjective`, `StandardSoloObjective` does not exist.

### Corporate Era and Quick Start

Test: [ClassDefinitionBoundaryTest, “Quick Start replaces Corporate Era rules when Corporate Era is excluded”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- In the default game, `CopyProductionBox` exists.
- In the default game, `QuickStartVariant` does not exist.
- With `-CorporateEraExpansion`, `CopyProductionBox` does not exist.
- With `-CorporateEraExpansion`, `QuickStartVariant` exists.

## 2. Maps, areas, and tracks

### An ordinary map area

Test: [ClassDefinitionBoundaryTest, “a map defines its areas and an area can be selected on its own”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt). `Hellas_1_1` represents the ordinary map-area shape; the test does not repeat it for every area.

- In the default game, `Hellas_1_1` does not exist.
- With `HellasMap`, `Hellas_1_1` exists.
- With `Hellas_1_1` chosen directly, `Hellas_1_1` exists.

### Mars and Venus parameter tracks

Test: [ClassDefinitionBoundaryTest, “Amazonis changes the rules for the Mars and Venus tracks”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- In the default game, `StandardGpTrackRules` exists.
- In the default game, `ExtendedGlobalParametersRule` does not exist.
- With `VenusNextExpansion`, `StandardVenusTrackRules` exists.
- With `VenusNextExpansion`, `ExtendedVenusTrackRules` does not exist.
- With `VenusNextExpansion, AmazonisMap`, `ExtendedGlobalParametersRule` exists.
- With `VenusNextExpansion, AmazonisMap`, `StandardGpTrackRules` does not exist.
- With `VenusNextExpansion, AmazonisMap`, `ExtendedVenusTrackRules` exists.
- With `VenusNextExpansion, AmazonisMap`, `StandardVenusTrackRules` does not exist.

## 3. Packs, cards, and card backs

### A pack with a card that needs another expansion

Test: [ClassDefinitionBoundaryTest, “Prelude 2 cards needing Venus wait for Venus Next”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- In the default game, `Ecotec` does not exist.
- With `Prelude2CardPack`, `Ecotec` exists.
- With `Prelude2CardPack`, `AtmosphericEnhancers` does not exist.
- With `Prelude2CardPack, VenusNextExpansion`, `AtmosphericEnhancers` exists.

### A chosen card brings the right card back

Test: [ClassDefinitionBoundaryTest, “a Prelude card brings its card back without the Prelude expansion”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- With `AcquiredSpaceAgency` chosen directly, `PreludeCard` exists.
- With `CheungShingMars` chosen directly, `PreludeCard` does not exist.

### Beginner corporations

Test: [ClassDefinitionBoundaryTest, “the beginner variant includes beginner corporations and their card back”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- In the default game, `BeginnerCorporation1` does not exist.
- In the default game, `BeginnerCorporationCard` does not exist.
- With `BeginnerVariant`, `BeginnerCorporation1` exists.
- With `BeginnerVariant`, `BeginnerCorporationCard` exists.

### A promo replacement

Test: [ClassDefinitionBoundaryTest, “the promo pack selects the new Deimos Down instead of the original”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- In the default game, `DeimosDown` exists.
- In the default game, `DeimosDownPromo` does not exist.
- With `PromoCardPack`, `DeimosDown` does not exist.
- With `PromoCardPack`, `DeimosDownPromo` exists.

## 4. Modules and individual content

### Prelude rules can be separated from their cards

Test: [ClassTableSelectionTest, “Content packs select only Content usable without their module” and “a module can exclude its associated card Content pack”](../test/common/dev/martianzoo/tfm/tests/rules/ClassTableSelectionTest.kt).

- With `Prelude1CardPack` alone, `Vitor` exists.
- With `Prelude1CardPack` alone, `PreludePhase` does not exist.
- With `PreludeExpansion, -Prelude1CardPack`, `PreludePhase` exists.
- With `PreludeExpansion, -Prelude1CardPack`, `Vitor` does not exist.

### Expansion rules and compatible standalone cards

Test: [ClassTableSelectionTest, “modules normally select their intrinsic rules and associated Content” and “explicitly included compatible Content needs no module or bundle selection”](../test/common/dev/martianzoo/tfm/tests/rules/ClassTableSelectionTest.kt).

- With `VenusNextExpansion`, `VenusStep` exists.
- With `Manutech` chosen directly, `Manutech` exists.
- With `ColoniesExpansion`, `TradeAction` exists.
- With `ColoniesExpansion`, `Callisto` exists.
- With `TurmoilExpansion`, `Party` exists.
- With `LakefrontResorts` chosen directly, `LakefrontResorts` exists.

### Content that requires its expansion

Test: [ClassTableSelectionTest, “explicitly included dependent Content requires its module” and “Turmoil global events are individual Content with a hard Module dependency”](../test/common/dev/martianzoo/tfm/tests/rules/ClassTableSelectionTest.kt). Choosing a dependent class without its required module is tested as an **invalid configuration**. There is no valid game table in which to call it “absent.”

- With `ColoniesExpansion, CryoSleep`, `CryoSleep` exists.
- With `TurmoilExpansion, AerialLenses`, `AerialLenses` exists.
- With `TurmoilExpansion, -AquiferReleasedByPublicCouncil`, `AquiferReleasedByPublicCouncil` does not exist.
- With `TurmoilExpansion, -AquiferReleasedByPublicCouncil`, `GlobalEvent` exists.

### A card's private resource

Test: [ClassDefinitionBoundaryTest, “Refugee Camps brings its Camp resource unless the card is excluded”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- With `ColoniesExpansion`, `Camp` exists.
- With `ColoniesExpansion, -RefugeeCamps`, `Camp` does not exist.
- With `Camp` chosen directly, `Camp` exists.

### Support supplied by individual promo cards

Tests: [ClassTableSelectionTest, “shared promo card resource follows either card without the pack”, “card local instruction follows its card without the pack”, and “Mars Nomads marker follows its card without the pack”](../test/common/dev/martianzoo/tfm/tests/rules/ClassTableSelectionTest.kt).

- With either `PharmacyUnion` or `Hospitals` chosen directly, `Disease` exists.
- With `IcyImpactors` chosen directly, `ChooseOceanArea` exists.
- With `MarsNomads` chosen directly, `NomadsMarker` exists.
- With `PromoCardPack, -MarsNomads`, `NomadsMarker` does not exist.

### References across packs do not turn on whole expansions

Test: [ClassTableSelectionTest, “cross-bundle prelude Content selects its card back without Prelude rules” and “cross-bundle Venus classes stay unselected without Venus Next”](../test/common/dev/martianzoo/tfm/tests/rules/ClassTableSelectionTest.kt).

- With `PromoCardPack, UtopiaMap, -CorporateEraExpansion`, `PreludeCard` exists.
- With `PromoCardPack, UtopiaMap, -CorporateEraExpansion`, `PreludePhase` does not exist.
- With `PromoCardPack, CimmeriaMap, -CorporateEraExpansion`, `VenusStep` does not exist.

## 5. Milestones and other goals

### A map milestone needing Turmoil

Test: [ClassDefinitionBoundaryTest, “a map milestone needing Turmoil waits for both options”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- With `AmazonisMap` alone, `Lobbyist` does not exist.
- With `TurmoilExpansion` alone, `Lobbyist` does not exist.
- With `AmazonisMap, TurmoilExpansion`, `Lobbyist` exists.

### An extra milestone chosen directly

Test: [ClassDefinitionBoundaryTest, “an extra milestone is absent until chosen”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- In the default game, `Briber` does not exist.
- With `Briber` chosen directly, `Briber` exists.

## 6. Solo setup for a chosen colony tile

Test: [ClassDefinitionBoundaryTest, “choosing a colony tile enables its solo setup”](../test/common/dev/martianzoo/tfm/tests/rules/ClassDefinitionBoundaryTest.kt).

- In a **solo** `ColoniesExpansion` game with no named tile, `SelectedColonyTile` does not exist.
- In a **solo** `ColoniesExpansion` game with no named tile, `SoloColoniesSetup` does not exist.
- In a **solo** game with `ColoniesExpansion, Callisto`, `SelectedColonyTile` exists.
- In a **solo** game with `ColoniesExpansion, Callisto`, `SoloColoniesSetup` exists.
- In a **two-player** game with `ColoniesExpansion, Callisto`, `SoloColoniesSetup` does not exist.

## 7. Known, selected, and usable are separate questions

Test: [ClassTableSelectionTest, “known selected and inhabited are different questions”](../test/common/dev/martianzoo/tfm/tests/rules/ClassTableSelectionTest.kt).

- In the default two-player game, `VenusStep` does not exist in the game's class table, although the catalog knows it.
- In a **solo** game with `Award` chosen directly, `Award` exists in the game's class table but is uninhabited.

Test: [ClassTableSelectionTest, “guarded mode references do not force unavailable classes into selection”](../test/common/dev/martianzoo/tfm/tests/rules/ClassTableSelectionTest.kt).

- In a **solo** game with `Prelude1CardPack`, `FirstPlace` does not exist.
- In a **solo** game with `Prelude1CardPack`, `SecondPlace` does not exist.
