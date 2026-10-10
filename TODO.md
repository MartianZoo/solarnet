<!-- Only bounded miscellaneous work not already covered by a roadmap or focused note belongs here. -->

# TODO

Issue links provide background. Inline TODOs should be brief context pointers.

## Standard actions

- [ ] Remove the action-slot dependency from general-purpose `Billing`. Investigate generated
  `Foo_Action1` and `Foo_Action2` classes as the actions' identities, preserving authored `This`
  references to the source card and the card's shared once-per-generation use.

- [ ] Keep gated System work with Admin when the gate becomes available. With player autoexecution
  disabled, `MAX 0 Billing: DefaultGreeneryTile` currently needs the player to select it after
  payment before Admin performs the signal; `Game20230521Test` shows the extra selections.

## Consensus top five project concerns — 2026-10-08

Ranked after independent Codex and Opus/xhigh reviews and three debate rounds, reviewing
`work3` at `8efcc32abb94493bd6e7ae35f1ecccf6a1f00d68`. This is the holistic priority ranking;
the older bug-only report below has a narrower scope.
These concerns remain open; the ranking does not select replacement designs.

1. **Operation completion and delegated control lack a coherent rule.** Per-task locking and
   cleanup at global queue exhaustion do not reliably describe an action and all its delegated
   work. Neptunian and Head Start cases expose the consequences. Resolve the underlying lifecycle
   rule; see [sequencing](docs/agents/SEQUENCING.md) and the selected
   [turn terminology, lifecycle, and Head Start direction](docs/agents/SEQUENCING.md#turns-and-turn-offers).
2. **Payment validation differs between execution paths.** The gameplay helper rejects paying
   11 M€ for the 10 M€ Olympus Conference, while the REPL accepts and spends all 11. Separately,
   the payer's own resource loss for another purpose can settle an open bill. Make validation
   consistent and tie settlement to the intended payment; see [payments](docs/agents/PAYMENTS.md).
3. **Kotlin orchestration owns game-flow rules that belong in Pets.** Phase order, expansion
   participation, and turn progression remain in Kotlin. Callback failure guarantees also need
   attention: a completion callback can throw after a successful mutation. Actual coroutine/Admin
   exception propagation was not verified. See the
   [Solarnet roadmap](SOLARNET_ROADMAP.md#internal-design-and-game-execution).
4. **The player-choice API cannot navigate all valid task shapes, and availability checks hide
   code faults.** Some valid forms throw `UnsupportedOperationException`; an injected custom-code
   fault makes availability queries return `false` while direct execution reports the fault.
   Improve choice assistance and preserve meaningful errors without requiring exhaustive legal-move
   enumeration. Named-card tracking remains a separate capability with ownership outside the engine.
   See the `TaskForm` follow-up below and
   [the live-engine note](docs/agents/ENGINE.md#actor-engines-and-agents).
5. **Specified Pets features fail when combined.** Defaulted variable references and local-class
   specialization have concrete defect characterizations. Repair these composition failures;
   implementation complexity alone does not justify a rewrite. See
   [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt) and the
   [Pets roadmap](PETS_ROADMAP.md#code-clarity-and-confidence).

## Pets specification fidelity audit

- [ ] Continue the specification-fidelity audit from the
  [Pets roadmap](PETS_ROADMAP.md#code-clarity-and-confidence), reassessing every lead against the
  current specifications, tests, KDoc, and implementation.

## User Ideas and Agreed Directions

- Continue the remaining `::` audit without treating current sequencing dependencies as permanent
  justifications. Preserve intrinsic card-tag construction, old Energy conversion before
  production, and Pharmacy Union's starting money before its tag penalties.

- [ ] Extend the build-time Canon invariant checks to indirect special-tile gains behind
  signals or supporting components (`GainsOf`; entry 15 of
  [GAME_HACKS](docs/agents/GAME_HACKS.md)). Current checks cover direct authored shapes and do not
  establish that the introspection helpers see every printed tile-placement effect. Keep dynamic
  payment and watcher interactions in gameplay tests rather than treating syntax scans as proof.

- [ ] Write and review clear public API specifications and KDoc for the
  [nine priority modules](docs/agents/SPEC_FIDELITY.md#priority-scope), following the documented
  audience and intended-contract standards. Preserve useful Pets documentation and fill gaps;
  track implementation defects separately. This standards update does not complete the module
  documentation or conformance audits.
- [ ] Extend the fixtures' solo-map placements when a scenario needs maps beyond Tharsis, Hellas,
  and Elysium; reassess sandbox phase shortcuts only from concrete needs. See the
  [fixture notes](docs/agents/TESTING.md#remaining-fixture-development).
- [ ] Investigate automatic attack-history effects separately; test migration does not justify
  changing the effects used by Law Suit and Crash Site Cleanup.
- [ ] Investigate enforcing the workflow's offered turn order in `TfmGameplayTest`. `Agent.inTurn`
  currently creates a turn whenever the acting player has no pending task, even while another
  player's turn is pending. Preserve authentic gameplay without adding a second test-side game
  driver; discuss the effect on sandbox and legacy callers before changing the shared helpers.
- Distinguish counting Venus tags from carrying a Venus tag in card availability. Io Sulphur
  Research should work without Venus Next because it merely counts Venus tags, but the catalog
  currently rejects it. Cards that carry Venus tags, such as Corroder Suits and Dirigibles, must
  continue to require the expansion. Keep this catalog change separate from the test-only batch.
- [ ] Continue the [adversarial-play design discussion](docs/agents/ADVERSARIAL.md): settle acceptance,
  trusted card custody, information release, and simultaneous choices before selecting an
  implementation. Work through draw/discard, Icy Impactors handoff, accepted work that cannot
  finish, and agreed undo examples. Preserve the meaning of accepted prefixes; keep Git
  coordination and hidden-information enforcement outside the engine.
- Revisit `RepeatPlacementBonus` now that premise-specialized effects remove its runtime condition
  check. Determine whether it can duplicate `AreaDefinition.bonus` directly again without reading
  the area's generated Class effect, while still omitting unavailable delegate bonuses.
- Resolve the nested self-transmutation case characterized in `pets/BugsTest`: an abstract Box
  currently permits identical concrete shared arguments. Preserve shared abstract arguments and
  rejection of dropping their markers; clarify when an open nested shared transmutation itself
  should be rejected.
- Reject declarations of new type variables in `OR` triggers. Use separate effects when each
  trigger must bind a variable.
- Revisit Mons Insurance self-compensation only if an authoritative ruling supports it. The
  selected provisional behavior excludes its owner from compensation triggers.
- [ ] Revisit
  [payment simplification and automatic-execution policy](SOLARNET_ROADMAP.md#internal-design-and-game-execution).
  Compare the exchanges plus a small, general policy adjustment against the current payment
  machinery; automatic execution is tunable outside the pure engine model.
- Find a simple, natural way for the presence of `Class<Aridor>` to bring all unused
  `Class<ColonyTile>` definitions into the game. Without Aridor present, only the explicitly
  selected colony tiles should be defined. This should depend on Aridor's Class being present,
  not on anyone playing the corporation; the implementation remains open.
- Consider extending `Name@` shorthand to represented-Class applications such as `Chosen@<Player>`.
  Inferring types at supplying occurrences (especially `EACH` and `RANK` domains) and retaining
  short spelling after resolution also remain deferred; supported shorthand references one
  uniquely named typed binding.
- Let compact same-class transmutations retain an explicit lexical `This` dependency before
  matching defaults. `CorporationCard<This, Hand FROM Selecting>` currently tries to resolve
  `This` as a class, while omitting it changes the pending task identity.
- Make `ComponentGraph.listenToCount` update only subscriptions affected by a component change,
  instead of recounting every watched Type after every change. Preserve immediate initial delivery,
  notifications only when the count changes, cancellation, and correct subtype/refinement handling
  through gains, removals, transmutations, and rollback. Prefer existing indexes and a small design;
  callers should continue receiving changes without polling.
- When preparing the [PETS repository draft](docs/pets-repo-draft.md) for publication, decide
  whether to include Sponsored Academies, currently retained as a draft note after the gallery.
- After the repository split, rename `:pets-tools` to `:tools` in the Pets repository.
- Before separating Git repositories, choose the Pets publishing host and version convention,
  automate tagged releases, and verify Solarnet against a hosted release. This follows build
  separation; see the [release workflow](PETS_ROADMAP.md#release-workflow-before-the-git-split).
- Add scenario saving and reloading to [Mars Playground](PLAYGROUND.md) after the initial version.
- Add shareable URLs for [Mars Playground](PLAYGROUND.md#technology-and-experience) later. First
  decide what restoring the same view includes: scenario, history position, camera, and windows.
- [ ] Remove the viewer's current saved-games feature completely: delete `SavedGame`,
  `SavedGames`, generated `games/index.txt`, packaged-replay dropdown discovery/loading, and their
  tests. Do not preserve a placeholder API or design the replacement during removal. Reintroduce
  recording selection only when there is a concrete, useful workflow to replace it.
- Review derived lookups on delegated Catalogs: `classDeclaration` and `allClassNames` use the
  delegate's properties even when the wrapper overrides them. Keep this existing issue separate
  from the class-loading cleanup.
- Try to simplify Flooding and Artificial Lake's ocean instructions without engine prediction.
  Preserve full-track no-placement behavior (including Amazonis), Artificial Lake's required
  placement below the cap, and Flooding's linked placement and victim choice.
- Shorten `DefaultGreeneryTile` using existing Pets. Keep it independent of `Community`, preserve
  own and opposing claims' different effects on fallback, and do not treat unaffordable placement
  consequences as permitting fallback. This is a local expression simplification, not an `ELSE`
  language or execution-search project.
- Fix greenery fallback when Mars Nomads blocks the last adjacent land area. `MarsNomadsTest` records
  that both the blocked placement and a distant placement currently fail. Keep promo-specific
  names out of the core greenery rule.
- Prefer the positive Arcadian Communities reward trigger `Tile<LandArea(HAS Community)>: 3 MC`
  when it can observe the claim before automatic ejection while keeping the reward queued.
  Currently ordinary trigger matching sees the area after ejection; making the reward automatic
  would also pay during corrections. Until a small solution exists, retain
  `-Community<LandArea(HAS Tile)>: 3 MC`.
- Look for a small way to evaluate the existing `CardFilter` criteria against a card Class in Pets.
  Replay tracking currently checks them in Kotlin; avoid adding engine card identities or a new
  processing stage just for this. Named-header specialization of Requirement properties and
  inspecting authored references are the current obstacles (see
  [card handling](docs/agents/CARD_HANDLING.md#external-offer-procedures)).
- Make L1 Trade Terminal's resource allowance count only its own gifts, not resource gains from
  other cards reacting to them. First reproduce the interaction with a literal `Microbe: Animal`
  fan-card effect, then find the smallest correction that preserves the Terminal's required
  distribution across eligible cards.
- Find a small, exact way for Ecology Experts' plant and microbe tags to trigger a newly played
  bio listener without replaying those tags or rewarding an older copy of that listener. Double
  Down copies the Prelude's immediate instruction but not its effects, so the solution must also
  cover that path. Until then, keep the four affected combinations Unsafe-only; `EcologyExpertsTest`
  pairs the intended and current Viral Enhancers and Ecological Zone outcomes.
- Consider a shared party-leader recalculation helper for the ranking repeated on delegate gains
  and Banned Delegate. Replacement cleanup belongs to `PartyLeader`; share the remaining winner
  selection only if it simplifies the model overall and preserves Recruitment's transfer semantics.
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
  available; the intended lifetime is the accepted [turn](docs/agents/SEQUENCING.md#uses-of-the-scope).
  Historical replays still use explicit adjustments for unsupported choices.
- Keep Fake Self-Replicating Robots permanently in `:tfm-fake`. Move its paired defect scenarios
  from `SelfReplicatingRobotsTest` into that module. Reuse existing test-support sources before
  considering a fixture API.
- Follow through on the FAQ-audit defects characterized in
  `MiningGuildTest`, `SelfReplicatingRobotsTest`, and
  [`MiningRightsTest.kt`](test/common/dev/martianzoo/tfm/tests/cards/MiningRightsTest.kt):
  Mining Rights/Area and Mining Guild on wild placement bonuses; Sponsored Projects adding
  resources to SRR's hosted cards. Preserve the external-card-selection boundary when addressing
  copied Merger commitment after new information is revealed.
- Investigate whether production-box copying can preserve enclosing bindings and conditions so
  Industrial Complex's former `EACH ... { PROD[...] }` spelling would work. For now, copying
  rejects a `PROD` nested inside `EACH` with an authoring diagnostic; the working form puts
  `EACH` inside `PROD`.
- Reconcile `OtbGame20260912Test` with the original physical-game evidence. Verify the reported
  extra Prelude per player and Blue's three-TR handicap, then express the evidenced setup in the
  correct order.
- Check Double Down outside the Prelude expansion: it should depend on whether a Prelude was
  actually played rather than on the expansion switch alone. Keep this case out of the
  [value-dependency inventory](https://docs.google.com/spreadsheets/d/13WRf7ljJLuy3iwTr5caQgKhTPhNugPKJuGx1ikALshY/edit?gid=0#gid=0)
  for now.
- Implement individual Turmoil party and whole-map selection described in the
  [Pets roadmap](PETS_ROADMAP.md#canon-and-game-rule-modeling).
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
  then move its effects, including closing the `AfterMe` cycle, and update its lifecycle tests and
  the bootstrap account in `ENGINE.md` and the Solarnet roadmap.
- Replace `FinalScoringPending` with a real `FinalScoringPhase`. Today `End` creates the temporary
  marker, `MeasureAward` depends on it, and marker removal assigns `Victory`; instead final-scoring
  effects should belong to the new phase, whose phase scope drains into terminal `End`, where victory
  is assigned. Coordinate this with `TfmWorkflow` and the runtime design in the Solarnet roadmap; do not
  merely rename the completion marker into a phase.
- Decide whether `Milestone`'s per-player uniqueness constraint should use
  `HAS MAX 1 This<Player>` or a clearer way to express one instance of the concrete milestone per
  player.
- Replace the duplicated `TemperatureStep BY Player`/`BY Admin` threshold-ocean triggers and the
  synthetic `AdminOceanPlacement` signal with one rule that separates who chooses the tile from
  whose action the placement is attributed to, shared by the standard and extended tracks.
- Reassess classifying `CheckRequirement` as `System` now that scaled direct gains route to Admin.
  Verify requirement failures and downstream choices before changing its classification.
- Investigate whether Player identity can survive Player → Admin → Player task chains without
  making bookkeeping classes `Owned` solely to carry that Player through. Preserve real ownership,
  cross-player effects, explicitly named recipients, and phases with no unique on-turn Player; do
  not replace those distinct cases with a guessed current player. Current identity roles and
  routing are recorded in [IDENTITY.md](docs/agents/IDENTITY.md).
- Revisit causal ownership inside `BootstrapPhase`, moving initialization work under ordinary
  phase-caused tasks as soon as the required runtime state can express them.
- Let refinements reference their candidate explicitly, so a selector can relate a nested
  dependency to that candidate without repeating its complete expression.
- Make `PROD[@StandardResource]` retain its represented-Class marker through lowering; Utopia
  Invest currently writes `Production<Class<@StandardResource>>` in its action for this reason.
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
- Weed the vague terms `operation` and `gameplay command` out of the engine. Rename each use for
  the exact lifecycle it denotes, including atomic calls, task completion, and workflow play.
- **Low priority:** [#54: ownership-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54)
  — Resolve contextual ownership correctly and display the resolved player.
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
- Replace the [archived Life of an Effect walkthrough](docs/archive/life-of-an-effect.md) with a
  current account of lexical `Me` and the transformation pipeline.
- Give Admin an installable autoexecution policy for Global Events that pulls exact cards from an
  ordered list; until then callers explicitly complete reveal tasks.
- Reconsider Turmoil's `PartyLeader` representation and name. It currently supplements the actual
  `PartyDelegate` as a non-`Delegate` role; decide whether a clearer role name or a true delegate
  subtype can express leadership without representing or counting the physical marker twice.
- Find a clean way to make Turmoil's `ApplyRulingBonus` player-owned without complicating the Reds
  tied-lowest-player selection. It currently remains one global signal whose party effect fans out
  over the players.
- Make Preservation Program compatible with Turmoil's Reds without making the card require Turmoil.
  A trial `3 MC / Ruling<Reds>` rebate on its automatic first-TR correction removed the observed
  Reds overcharge and affordability failures, but the catalog treats that reference as a hard
  Turmoil dependency even when a separate rebate trigger has `IF TurmoilExpansion`. Investigate an
  optional count reference or a Turmoil-owned rule without adding a card-specific gameplay helper.
  If resolved, update the premise compatibility gate, `ModuleSelectionTest`, and
  `docs/what-is-supported.md`; Terraforming Deal's payout for reversed TR remains a separate defect.
- Investigate whether the three self-handling signals `CimmeriaPlacementBonus`,
  `PlaceNeutralTiles`, and `StageForReplicatedProject` can avoid named helper Classes without
  requiring authored references to generated names. Preserve Cimmeria map generation,
  `PlaceNeutralTiles`'s system-only ownership, and SRR's explicit card-Class selection.
- Consider rejecting `@` markers on concrete types, such as `Class<@BuildingTag>`, where the
  represented class is already fixed. Decide whether this should be an authoring error.
- Carry resource/file names through Pets parsing and generated catalog inputs so diagnostic spans
  identify the original file as well as the submitted text, line, and column.
- Preserve authored provenance when runtime narrowing and task normalization synthesize new trees.
  Definition, query, and direct-change diagnostics retain available spans; some generated tasks and
  failures computed solely from component Types still have no authored location.
- Preserve the empty-intersection reason when rejecting a partial task submission. For an
  unavailable Type, the current fallback can misleadingly blame an omitted dependency instead.
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

- Extend instruction intersection to preserve unresolved shared aliases and unnamed Type unions.
  These currently require a more specific submitted choice; keep task matching conservative and
  never substitute `Ok` for an unrepresentable nonempty intersection.
- Complete `TaskForm` decision and option enumeration for `EACH` and nested instruction shapes.
  Decide how a form should offer `Ok` for an optional change
  inside `PER`, whose current narrowing rule requires a change child until state resolution removes
  the wrapper. In the Generation 4 replay, Power Infrastructure's `X` task exposes an `Anyone`
  target decision before its amount even though the existing action helper can bind `X`; settle how
  a form recognizes a forced type resolution before offering the amount.
- Express card-face/card-back compatibility in the selectable `PlayCard` specification so generic
  forms can restrict Prelude faces without interpreting later card-playing effects. Its independent
  class-literal parameters currently leave `CardFront` options broader than the selected card back.
- If clients need options proved completable, add a separate analysis of the same one-step options.
  Report inconclusive searches explicitly; keep broad option enumeration independent of speculative
  engine resolution.
- Extend the Mining Rights/Area wild-resource regression after placement is fixed: Robotic
  Workforce and Cyberia Systems may choose either originally available metal production even if
  the placement awarded a nonmetal resource. No remembered resource choice is required.

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

- Find a principled way for narrower dependency defaults to retain compatible refinements from
  wider defaults, so `Tile` can own area occupancy once while its subclasses select their kinds of
  areas and add placement rules.
- Simplify `LiveEffect` actor binding by threading a binding context through subscription matching
  instead of maintaining parallel `Subscription.transform()` implementations and `Hit.before()`.
- Separate `Instructor`'s resolution-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `InstructionResolutionTest`.
- Replace `World.onTransactionComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.

## Ranked bug report — 2026-10-02

Review of `work1` at `9b26e816c`, using source, tests, open issues, historical audits, and temporary
reproduction probes. This ranking weights foundational semantics and reliable replay evidence above
isolated card coverage, following [VALUES.md](docs/agents/VALUES.md). It excludes fakecanon-only bugs,
Unsafe-only incompatible pairings, simple over-permissiveness that records the expected `Audit`,
attribution, features, and unresolved rule questions. Related symptoms are grouped.

1. **Defaulted type variables reject valid references.** A supplier accepting `<>` incorrectly
   makes its bare references demand defaults too. This breaks gains, removals, transmutations, and
   forward references—a basic compositionality defect. See
   [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).

2. **Owner-local classes fail when specialization fixes a dependency.** Mars First's inline
   `Policy<This> { Tile<MarsArea>: Steel }` produces `MarsFirst_Policy<This>` extending
   `Policy<MarsFirst>`. The argument survives after its dependency position disappears, so
   elaboration rejects the gain. Reproduced during review. See
   [DerivedClassLowerer](src/common/dev/martianzoo/pets/DerivedClassLowerer.kt) and the owner-local
   specialization characterization in [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).

3. **Autoexecution throws instead of waiting for another player.** An automated player's currently
   blocked task causes an exception even when a nonautomated player has a legal task that enables
   it. Reproduced generically; no normal Mars scenario was identified. See the zero-options fallback
   in [AutoExecLoop](src/common/dev/martianzoo/agent/AutoExecLoop.kt) and the waiting characterization
   in [Agent BugsTest](test/common/dev/martianzoo/agent/BugsTest.kt).

4. **Expansion compatibility rejects usable content.** Constructor, Summit Logistics, and Molecular
   Printing are blocked without Colonies; Suitable Infrastructure is blocked without Prelude rules
   despite its surviving standard-action effect. Configuration probes confirmed these rejections.
   Constructor's combined city/colony metric is treated as a hard Colonies dependency. This also
   forces an inaccurate Colonies setting into `OtbGame20260912Test`: Summit Logistics lacks a
   printed Colonies dependency icon and should allow its smaller payout without enabling unused
   Colonies gameplay. See
   the eligibility direction in the
   [Pets roadmap](PETS_ROADMAP.md#canon-and-game-rule-modeling).
   Keep Suitable Infrastructure out of the
   [value-dependency inventory](https://docs.google.com/spreadsheets/d/13WRf7ljJLuy3iwTr5caQgKhTPhNugPKJuGx1ikALshY/edit?gid=0#gid=0)
   for now.

5. **Corporations acquired after Prelude defer their mandatory first action.** Board of Directors
   → Merger → Tharsis leaves the city placement for another action instead of resolving it
   immediately. This changes action timing and available intervening choices. Implement the resolved
   FAQ behavior: an impossible first action must invalidate the Prelude play. See the Board/Merger/
   Tharsis pair in [MergerTest](test/common/dev/martianzoo/tfm/tests/cards/MergerTest.kt).

6. **Prelude-drawing content can be selected without a usable Prelude pool.** WG Project, Valley
   Trust, Board of Directors, and New Partner don't bring in the required pool. Configuration probes
   confirmed the missing default pool. Selecting WG Project must make the Prelude 1 pool available
   for its draw even when that pool otherwise mostly sits unused; selecting the pool must not start
   the Prelude phase. Check explicit pool exclusions separately. The WG Project draw is
   covered by the pair in [WgProjectTest](test/common/dev/martianzoo/tfm/tests/cards/WgProjectTest.kt).

7. **`DEFAULT` silently discards a root type-variable marker.** For example,
   `DEFAULT +@Piece<First>` becomes an ordinary default without reporting the invalid marker.
   Reproduced during review. Reject the marker when recording the declaring class and argument
   specs; keep this diagnostic change separate from owner-local declaration extraction. See
   [Parsing](src/common/dev/martianzoo/pets/Parsing.kt), `rejectInvalidDefaultRoot`, and the marker
   characterization in [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).

8. **Parser errors can identify the wrong character.** `Foo<~ Bar>` blames `<` rather than the
   invalid `~`, misleading the author about what needs fixing. Reproduced during review. The
   better-parse completion analyzer drops `NoMatchingToken` failures; address that diagnostic
   separately from grammar organization. The misplaced diagnostic is characterized in
   [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).
