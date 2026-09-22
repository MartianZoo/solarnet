<!-- Only bounded miscellaneous work not already covered anywhere in docs/agents/ belongs here. -->

# TODO

Issue links provide background. Inline TODOs should be brief context pointers.

## User Ideas and Agreed Directions

- Replace `ModulesReady` with entering `BootstrapPhase` only after the generated premise has created
  the selected modules and configured components. Moving its effects to `SetupPhase` was tested and
  fails because bootstrap validation already requires the exact-one global-parameter rule systems.
  The smallest promising direction is to reverse premise/`BootstrapPhase` creation in `Initializer`,
  then update its lifecycle tests and the bootstrap account in `ENGINE.md` and `WORKFLOW.md`.
- Replace `FinalScoringPending` with a real `FinalScoringPhase`. Today `End` creates the temporary
  marker, `MeasureAward` depends on it, and marker removal assigns `Victory`; instead final-scoring
  effects should belong to the new phase, whose phase scope drains into terminal `End`, where victory
  is assigned. Coordinate this with `TfmWorkflow` and the phase-scope design in `WORKFLOW.md`; do not
  merely rename the completion marker into a phase.
- Decide whether `Milestone`'s per-player uniqueness constraint should use
  `HAS MAX 1 This<Player>` or a clearer way to express one instance of the concrete milestone per
  player.
- Replace the duplicated `TemperatureStep BY Player`/`BY Admin` threshold-ocean triggers and the
  synthetic `AdminOceanPlacement` signal with one rule that separates who chooses the tile from
  whose action the placement is attributed to, shared by the standard and extended tracks.
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
- **Low priority:** [#41: `list`](https://github.com/MartianZoo/solarnet/issues/41) — Improve
  hierarchy/dependency descent, grouping, depth, concrete subtypes, and explicit `<Anyone>` display.
- Give Admin an installable autoexecution policy for Global Events that pulls exact cards from an
  ordered list; until then callers explicitly complete reveal tasks.
- Reconsider Turmoil's `PartyLeader` representation and name. It currently supplements the actual
  `PartyDelegate` as a non-`Delegate` role; decide whether a clearer role name or a true delegate
  subtype can express leadership without representing or counting the physical marker twice.
- Find a clean way to make Turmoil's `ApplyRulingBonus` player-owned without complicating the Reds
  tied-lowest-player selection. It currently remains one global signal whose party effect fans out
  over the players.
- Investigate whether the three self-handling signals `CimmeriaPlacementBonus`,
  `PlaceNeutralTiles`, and `StageForReplicatedProject` can avoid named helper Classes without
  requiring authored references to generated names. Preserve Cimmeria map generation,
  `PlaceNeutralTiles`'s system-only ownership, and SRR's explicit card-Class selection.
- Investigate why semantic validation during parsing throws `IllegalArgumentException` for malformed
  Pets, and whether those paths should use a parser-specific exception before translation.
- Model Established Methods as two nonzero-cost standard projects without making the second project
  mandatory when none is affordable. `StandardProject(HAS cost)` excludes Sell Patents and
  non-project actions, but does not express current affordability; making the second action optional
  would instead let a player skip it while an affordable project exists.

### Hypothetical Card Behavior

- Decompose a future card's `2 CityTile` instruction into two placement choices; consider making
  `Tile` atomized ([#64](https://github.com/MartianZoo/solarnet/issues/64)).

## Autonomous Follow-ups

- Add Jacob Fryxelius's ruling that moving Mars Nomads does not trigger the Mars First ruling policy.
- Repair the two declared Pets conformance gaps without adding a second representation of type
  identity: L7-8 lets `Tile<> THEN Tile<>` stages diverge after defaults, and T8-3 can substitute a
  refinement candidate into the wrong one of several compatible dependency slots while existing
  cards still require candidate/argument merging.
- Find a principled way for narrower dependency defaults to retain compatible refinements from
  wider defaults, so `Tile` can own area occupancy once while its subclasses select their kinds of
  areas and add placement rules.
- Model L1 Trade Terminal's three-distinct-card resource choice, then replace `FakeL1TradeTerminal`
  with the canonical card.
- Simplify `LiveEffect` actor binding by threading a binding context through subscription matching
  instead of maintaining parallel `Subscription.transform()` implementations and `Hit.before()`.
- Separate `Instructor`'s resolution-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `InstructionResolutionTest`.
- Replace `World.onTransactionComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.
