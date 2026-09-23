<!-- Only bounded miscellaneous work not already covered anywhere in docs/agents/ belongs here. -->

# TODO

Issue links provide background. Inline TODOs should be brief context pointers.

## User Ideas and Agreed Directions

- Let English component roles default to one canonical component noun so placement, requirements,
  and metrics do not repeat singular and plural wording.
- Extend derived `including this` wording to setup operations and otherwise unsupported effects
  without adding card-specific rendering rules.
- Avoid `forEach` in tests; use cases that report failures independently or explicit assertions.
- Let a configuration select all applicable Content exposed by one bundle, without inventing a
  `CardPack` Module. Resolve narrower pool requests into individual Class choices before the game
  premise is built. Resolve eligibility before offering that choice: promo replacements still test
  `PromoCardPack`, M&A goal invariants can use
  non-simple Class metrics, and Venus cards/goals can become unviable when their supporting content
  is absent. Keep Turmoil Global Events individually selectable with hard Turmoil dependencies.
- Consider letting the Milestones & Awards bundle also provide goals identical to those supplied
  by map bundles, enforcing identical declarations across both sources. A user selecting such a
  goal individually would resolve it through Milestones & Awards provenance, not through a map
  bundle; selecting a map could still include its associated goal by default. This needs a clear
  source rule because an unqualified Class Name currently loses that provenance.
- Decide whether individually selected Content should activate otherwise Module-local vocabulary it
  structurally needs. The motivating case is a Venus card carrying `VenusTag`: logically the card
  could make the tag available without `VenusNextExpansion`, while the current bundle-availability
  lock rejects it. Catalog the analogous cases before either generalizing activation or accepting
  this as a documented selection limitation.
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
- Revisit causal ownership inside `BootstrapPhase`, moving initialization work under ordinary
  phase-caused tasks as soon as the required runtime state can express them.
- Let refinements reference their candidate explicitly, so a selector can relate a nested
  dependency to that candidate without repeating its complete expression.
- Revisit contextual `Owner` as a broad language redesign; the explicit Type-variable work leaves
  its ambient binding semantics unchanged for now.
- Give Pets a real structural conjunction, spelled something like `Tile(IS Owned)`, and retire the
  nominal `OwnedTile` class once `Landlord` and the other owned-tile rules can name the intersection
  directly. Until then a master-universe Canon test checks the nominal `OwnedOccupant` and
  `OwnedTile` relationships without depending on one active configuration.
- Decide whether compact Type expressions must be globally shortest. They currently remove each
  individually redundant argument, including T3-8 duplicates, without the subset search needed to
  prove a global minimum; search only equality-related arguments if exact minimality becomes useful.
- Separate the expression API's three intents: an object's natural available expression, a resolved
  Type's compact expression, and its full expression. Keep syntax expressions universe-independent;
  converting an arbitrary expression to either resolved form must take a `ClassTable` explicitly.
- Decouple cleanup lifetime from log visibility so player-meaningful signals such as `Pay` and
  `PayFromCard` need not inherit `Hidden` through `MustCleanUp`.
- Weed the vague terms `operation` and `gameplay command` out of the engine. Rename each use for
  the exact lifecycle it denotes, including atomic calls, task completion, and workflow play.
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
- Put promo replacement defaults on or near their replacement Content, so `GreatDamPromo` can
  identify `GreatDam` instead of the original card naming `PromoCardPack`. Preserve the three
  tested choices: original by default, promo by default with Promos, and both when the original
  is explicitly included. Deimos Down and Magnetic Field Generators follow the same pattern.
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

- Define an authored, game-neutral way for bundle compatibility inference to distinguish hard
  dependencies from references that may safely disappear when a companion bundle is absent. Start
  with characterization tests for Suitable Infrastructure, Constructor, Soil Studies, and Summit
  Logistics; decide ambiguous no-op cards explicitly, and let English consume the same modeled fact
  rather than adding a renderer-only flag.
- Move `PreludePhase` out of `tfm-text`'s `resetsForPreludeAction` recognizer once there is a
  principled bundle-supplied description of the phase/latch relationship; do not add a Prelude-only
  boolean merely to relocate the class name.
- Render the five beginner corporation copies from their inherited modeled setup semantics.
- Complete the three visible goal-text refusals only from modeled semantics: Briber's immediate
  claim instruction, Philantropist's `GainsOf` metric, and Suburbian's map-edge concept.
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
