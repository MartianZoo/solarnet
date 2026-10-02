<!-- Only bounded miscellaneous work not already covered anywhere in docs/agents/ belongs here. -->

# TODO

Issue links provide background. Inline TODOs should be brief context pointers.

## User Ideas and Agreed Directions

- Consider a shared party-leader recalculation helper for the ranking repeated on delegate gains
  and Banned Delegate. Keep it only if it simplifies the Pets model overall and preserves leadership
  through Recruitment's neutral-delegate transfer.
- Place the neutral solo tiles immediately after the original corporation play, then remove Tharsis
  Republic's solo-only +2 M€ production effect. Its normal city-placement effect should grant the
  two production steps; verify that neutral greeneries still do not raise oxygen and that a
  Tharsis acquired later through Merger receives no setup bonus.
- Find realistic setups for impossible original first actions of Tharsis, Philares, Arcadian
  Communities, and Aridor; only Poseidon has a focused characterization. Do not fill a map
  synthetically just to manufacture the failure.
- Clarify how Thawer markers behave when Snow Cover lowers a marked temperature step and that step
  is raised again. A generic count of temperature increases cannot prove which physical step is marked.
- Fix Point Luna's played-Earth-tag response so a temporary assigned tag still counts for Cartel
  but does not cause a card draw. First try the existing card-holder distinction; do not bypass
  trigger execution. Full wild-tag assignment remains deferred until a clean rule representation is
  available; historical replays still use explicit adjustments for unsupported choices.
- Keep Fake Self-Replicating Robots permanently in `:tfm-fake`. Move its bug characterizations,
  including the SRR cases mixed into `cards/BugsTest.kt`, into that module and consolidate duplicate
  Sponsored Projects coverage. Reuse existing test-support sources before considering a fixture API.
- Follow through on the FAQ-audit defects characterized in
  [`cards/BugsTest.kt`](test/common/dev/martianzoo/tfm/tests/cards/BugsTest.kt):
  Mining Rights/Area and Mining Guild on wild placement bonuses; Sponsored Projects adding
  resources to SRR's hosted cards; and Constructor remaining usable without Colonies.
  Constructor's combined city/colony metric is treated as a hard Colonies dependency by the shared
  content-compatibility filter. Preserve the external-card-selection boundary when addressing
  copied Merger commitment after new information is revealed.
- Investigate whether production-box copying can preserve enclosing bindings and conditions so
  Industrial Complex's former `EACH ... { PROD[...] }` spelling would work. For now, copying
  rejects a `PROD` nested inside `EACH` with an authoring diagnostic; the working form puts
  `EACH` inside `PROD`.
- Reconcile `OtbGame20260912Test` with the original physical-game evidence. Summit Logistics lacks
  a printed Colonies dependency icon; model its inclusion without enabling unused Colonies gameplay
  while allowing the smaller payout. Verify the reported extra Prelude per player and Blue's
  three-TR handicap, then express the evidenced setup in the correct order.
- Audit Prelude-card draws without `PreludeExpansion` for Valley Trust, Board of Directors,
  WG Project, and New Partner. In particular, selecting WG Project must make the Prelude 1 card
  pool available for its draw even when that pool otherwise mostly sits unused; do not infer that
  selecting the pool starts the Prelude phase. Check explicit pool exclusions separately.
- Check the remaining Prelude-related content outside the Prelude expansion: Suitable
  Infrastructure should retain its standard-action response when the Prelude-phase response is
  absent, and Double Down should depend on whether a Prelude was actually played rather than on
  the expansion switch alone. Keep these cases out of the
  [value-dependency inventory](https://docs.google.com/spreadsheets/d/13WRf7ljJLuy3iwTr5caQgKhTPhNugPKJuGx1ikALshY/edit?gid=0#gid=0)
  for now.
- Implement individual Turmoil party and whole-map selection as specified in
  [Content selection and expansion eligibility](docs/agents/CONTENT_SELECTION.md#roles-and-current-selection).
- Make owner-local Class arguments work when specialization fixes an inherited dependency.
  Mars First's inline `Policy<This> { Tile<MarsArea>: Steel }` lowers to a gain of
  `MarsFirst_Policy<This>` extending `Policy<MarsFirst>`; `MarsFirst` is fixed and no longer an
  argument position, so elaboration rejects the gain.
- Consider allowing owner-local Class declarations only in gain instructions. They currently also
  parse in other expression positions, including a selector's `HAS` refinement; decide the intended
  boundary and account for existing uses before restricting the syntax.
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
- Extract `Parsing`, `DerivedClassLowerer`, and the parsed system-declaration provider into an
  optional parser module. The model construction API supports independent parsers; keep the
  better-parse dependency with source input. Canonical content still needs a separate build-time
  conversion to typed declarations before its consumers can omit runtime parsing entirely.
- Report an unrecognized character inside an optional Pets production at that character:
  `Foo<~ Bar>` currently points at `<` rather than `~`. The better-parse completion analyzer drops
  `NoMatchingToken` failures; address that diagnostic separately from grammar organization.
- Reject type-variable markers on a `DEFAULT` root instead of silently discarding them when the
  clause records its declaring class and argument specs. Keep this diagnostic change separate from
  owner-local declaration extraction.
- Carry resource/file names through Pets parsing and generated catalog inputs so diagnostic spans
  identify the original file as well as the submitted text, line, and column.
- Preserve authored provenance when runtime narrowing and task normalization synthesize new trees.
  Definition, query, and direct-change diagnostics retain available spans; some generated tasks and
  failures computed solely from component Types still have no authored location.
- Improve the specific caret targets and related-source context recorded beside message assertions
  in `CatalogDiagnosticsTest` and `PostCatalogDiagnosticsTest`; consider rendering span widths as
  well as their starting positions.
- Clarify T10-4 when an inherited dependency default is disjoint from a subclass's bound. The
  compiler currently drops that default; `MoholeArea_SpecialTile` relies on this for its WaterArea
  bound versus SpecialTile's LandArea default. Keep this semantic question separate from diagnostic
  improvements.

### Hypothetical Card Behavior

- Decompose a future card's `2 CityTile` instruction into two placement choices; consider making
  `Tile` atomized ([#64](https://github.com/MartianZoo/solarnet/issues/64)).

## Autonomous Follow-ups

- Correct defaulted Type-variable references: `pets/BugsTest` shows bare references incorrectly
  inheriting their supplier's `<>`.

- Revisit aligning multiplatform JVM tests with the repository JUnit BOM. Setup overrides now
  explicitly declare `@BeforeTest`, including `ActiveVacuumCoreTest.commonSetup`. Verify lifecycle
  compatibility and the full replay suite before changing the runner.

- Revisit Dokka's transitive Jackson 2.15.3, jsoup 1.16.1, and FreeMarker 2.3.32 advisory
  matches when a stable Dokka update is available. These documentation-time dependencies remain
  unchanged to avoid maintaining unverified overrides.

- Review [the class-existence scenario draft](docs/class-existence-scenarios-draft.md) for
  clarity and coverage, then consolidate `ClassDefinitionBoundaryTest` and
  `ClassTableSelectionTest`. Keep each distinct selection boundary tested once and remove
  repetitive assertions without losing the readable scenarios or broad module/content cases.

- Add Jacob Fryxelius's ruling that moving Mars Nomads does not trigger the Mars First ruling policy.
- Find a principled way for narrower dependency defaults to retain compatible refinements from
  wider defaults, so `Tile` can own area occupancy once while its subclasses select their kinds of
  areas and add placement rules.
- Simplify `LiveEffect` actor binding by threading a binding context through subscription matching
  instead of maintaining parallel `Subscription.transform()` implementations and `Hit.before()`.
- Separate `Instructor`'s resolution-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `InstructionResolutionTest`.
- Replace `World.onTransactionComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.
