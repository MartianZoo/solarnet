<!-- Only bounded miscellaneous work not already covered anywhere in docs/agents/ belongs here. -->

# TODO

Issue links provide background. Inline TODOs should be brief context pointers.

## User Ideas and Agreed Directions

- Derive when a card-resource or tag reference includes the current card so English can add
  `including this` reliably; keep the current wording until the representation supplies that fact.
- Avoid `forEach` in tests; use cases that report failures independently or explicit assertions.
- Decide whether `Milestone`'s per-player uniqueness constraint should use
  `HAS MAX 1 This<Player>` or a clearer way to express one instance of the concrete milestone per
  player.
- Decide whether `NoctisArea` belongs with the Noctis City card instead of the core board model.
- Replace the duplicated `TemperatureStep BY Player`/`BY Admin` threshold-ocean triggers and the
  synthetic `AdminOceanPlacement` signal with one rule that separates who chooses the tile from
  whose action the placement is attributed to, shared by the standard and extended tracks.
- Discard the uncommitted typed custom-metric/code-generation experiment; it was evaluated and
  considered an unsuccessful direction.
- **Low priority:** [#54: Owner-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54)
  — Resolve contextual ownership correctly and display the resolved player.
- **Low priority:** Investigate why the oxygen steps created by SoloOpponent's setup greeneries do
  not award it TR, and whether adding and then removing those steps has any other observable
  consequences.
- Consider requirement-gated action costs, using United Nations Mars Initiative to make
  `HasRaisedTr` a prerequisite to paying its 3 M€ rather than a gate around the result.
- Derive selected singleton card watchers without explicit support-Class invariants. The current
  sites are United Nations Mars Initiative and Pristar retaining `TrWatcher`, and Hydrologist
  retaining `HydrologistWatcher`.
- Reverse replacement-card references so optional packs identify what they replace rather than
  base cards naming optional packs. Deimos Down, Great Dam, and Magnetic Field Generators currently
  use `autoSelectWhen = MAX 0 PromoCardPack`.
- Check whether Early Colonization's two `AdvanceColonyTracks` changes should be explicitly
  mandatory.
- **Low priority:** [#41: `list`](https://github.com/MartianZoo/solarnet/issues/41) — Improve
  hierarchy/dependency descent, grouping, depth, concrete subtypes, and explicit `<Anyone>` display.
- [#59: `-This` Quantifier](https://github.com/MartianZoo/solarnet/issues/59) — Decide whether
  self-removal should default to mandatory.
- Give Admin an installable autoexecution policy for Global Events that pulls exact cards from an
  ordered list; until then callers explicitly complete reveal tasks.
- Reconsider Turmoil's `PartyLeader` representation and name. It currently supplements the actual
  `PartyDelegate` as a non-`Delegate` role; decide whether a clearer role name or a true delegate
  subtype can express leadership without representing or counting the physical marker twice.
- Investigate whether the three self-handling signals `CimmeriaPlacementBonus`,
  `PlaceNeutralTiles`, and `StageForReplicatedProject` can avoid named helper Classes without
  requiring authored references to generated names. Preserve Cimmeria map generation,
  `PlaceNeutralTiles`'s system-only ownership, and SRR's explicit card-Class selection.
- Investigate why semantic validation during parsing throws `IllegalArgumentException` for malformed
  Pets, and whether those paths should use a parser-specific exception before translation.

### Hypothetical Card Behavior

- Make `VictoryPoint` depend on the scoring `Component`, and define a scoring-completion phase if a
  future score depends on another score rather than directly on game state.
- Decompose a future card's `2 CityTile` instruction into two placement choices; consider making
  `Tile` atomized ([#64](https://github.com/MartianZoo/solarnet/issues/64)).

## Autonomous Follow-ups

- Complete the three visible goal-text refusals only from modeled semantics: Briber's immediate
  claim instruction, Philantropist's `GainsOf` metric, and Suburbian's map-edge concept.
- Add canonical Prelude 2 definitions for Corridors of Power, Envoys from Venus, Special Permit,
  Red Tourism Wave, and Frontier Town, then remove their source-specific replay fixtures.
- Repair the two declared Pets conformance gaps without adding a second representation of type
  identity: L7-8 lets `Tile<> THEN Tile<>` stages diverge after defaults, and T8-3 can substitute a
  refinement candidate into the wrong one of several compatible dependency slots while existing
  cards still require candidate/argument merging.
- Find a principled way for narrower dependency defaults to retain compatible refinements from
  wider defaults, so `Tile` can own area occupancy once while its subclasses select their kinds of
  areas and add placement rules.
- Model L1 Trade Terminal's three-distinct-card resource choice, then replace `FakeL1TradeTerminal`
  with the canonical card.
- Replace FakeThawer's persistent temperature credits with credits that also account for
  global events reducing temperature.
- Simplify `LiveEffect` actor binding by threading a binding context through subscription matching
  instead of maintaining parallel `Subscription.transform()` implementations and `Hit.before()`.
- Separate `Instructor`'s resolution-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `InstructionResolutionTest`.
- Replace `World.onTransactionComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.
