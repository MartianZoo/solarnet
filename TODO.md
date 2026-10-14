<!-- Only bounded miscellaneous work not already covered by a roadmap or focused note belongs here. -->

# TODO

Keep tasks brief; linked notes and tests carry the background.

## Card tracking

- [ ] Revisit player-owned card locations together: `Selecting`, `Hand`, and played events.
  The shared `Selecting` pool currently needs per-player turn cleanup because another player's
  corporation-phase project offer may still be there.
- [ ] Decide whether to verify card identities and claim filters for deck flips outside tracked hands
  and Selecting offers, such as Search for Life and Asteroid Deflection System.

## Standard actions

- [ ] Fix missing-action selection and remove the action-slot dependency from general-purpose
  `Billing`. See [action identity and invocation](docs/agents/ACTIONS.md) for the AI Central defect,
  required behavior, and proposed direction.

- [ ] Keep gated System work with Admin when its gate becomes available. Simplify the extra player
  selections in `Game20230521Test` and `HeadStartTest` through general task handling.

- [ ] Let Admin settle its available work before a nested cross-Actor Agent call returns; the Pluto
  scenario in `ColoniesRulesTest` uses separate top-level calls instead. Draining every nested call
  changed source-backed replay outcomes, so first determine which calls form complete decisions.

## Task dispatch

- [ ] Separate task selection from execution: with autoexecution disabled, selecting a concrete task
  currently executes it. Revisit Agent contracts, handoffs, continuations, and replay `CHOOSE`
  commands together.
- [ ] When a Canon card needs an open `TASK` recipient, make the recipient and the body's `Me` one
  shared choice, reusing existing Type-variable machinery if it can serve.
- [ ] Decide whether Agent task submissions must give concrete amounts (`-8 Heat`) rather than
  arithmetic over `Owed`.

## Game flow

- [ ] Reconsider the operation-completion rule: an operation's work is over once the World's
  `FINALLY` work has begun since its last decision. It is judged from the event log against
  `AutoExecLoop.lastDecision`, which is Agent-side state shared by every Actor, so a nested
  cross-Actor call moves it. See [operation completion](docs/agents/SEQUENCING.md#operation-completion).
- [ ] Check for game end at the end of `ProductionPhase`, not as it begins. Its `IF GameEndBarrier`
  gates are evaluated when the phase is gained; the check could instead be part of the phase's own
  `FINALLY` work.
- [ ] Give the Corporation, Prelude, and final greenery phases a shared abstract phase class for
  their repeated turn-order rules (`EACH Starter@Player(HAS StartToken) { TakeTurn<Starter@> }` and
  the `TakeTurn` answer).
- [ ] Verify event tag lifetimes through deferred consequences. An event's `LATER` flip is promoted
  together with other `LATER` work and can precede consequences that work generates; the deleted
  `Temporary` sweep waited for all tasks. No Canon case is known.
- [ ] Evaluate lowering the on-turn player's waiting work during an explicitly handed-off response,
  including Admin billing and payment descendants. Compare it with a complete payment-allocation
  decision, including same-player payment.
- [ ] When implementing the [Head Start variant](docs/agents/SEQUENCING.md#head-start-acceptable-variant-to-pursue),
  give each owed turn through `TakeTurn`, so that it waits for the previous turn's `LATER` cleanup;
  nested `LATER` alone lets the next action run first.

## Game files

- [ ] Derive game-file card inputs from typed movements and `Cards:` notes; verify full and short
  imports with production `CardTracking`. See [export design](docs/agents/EXPORT.md).
- [ ] Decide whether short game files need arbitrary event commentary.

## Physical game conversion — 2026-10-09

- [ ] If new source evidence appears, resolve Pink's spoken 138 versus the replay's 139. See
  `_local/replays/Game20261009/sources.md`.

## Project priorities — 2026-10-08 review

1. Resolve operation completion and delegated control across an action and its tasks. See
   [sequencing](docs/agents/SEQUENCING.md#turns-and-turn-offers).
2. Make payment validation consistent across gameplay and REPL execution, and settle only the
   intended bill. Check `PharmacyUnionTest`'s missing disease penalties. See
   [payments](docs/agents/PAYMENTS.md).
3. Give turns a lasting scope through authored rules, now that the [game flow](docs/agents/SEQUENCING.md#game-flow)
   is in Pets; define callback failure behavior. See the [Solarnet roadmap](SOLARNET_ROADMAP.md#internal-design-and-game-execution).
4. Support valid task shapes in the player-choice API and surface code faults during availability
   checks. See [TaskForm below](#autonomous-follow-ups) and [engine notes](docs/agents/ENGINE.md#actor-engines-and-agents).
5. Repair specified Pets composition failures, including owner-local specialization. See
   [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).

## Pets specification fidelity audit

- [ ] Continue the [Pets specification-fidelity audit](PETS_ROADMAP.md#code-clarity-and-confidence).

## User Ideas and Agreed Directions

- [ ] Define a Canon guard for card effects that can remove heat outside `Billing`. Include
  `FocusedOrganization`'s `-StandardResource` choice as well as literal `-Heat`; determine whether
  Stormcraft's floaters apply to that choice before requiring every such card to be named by
  Stormcraft.

- Give beginner and standard players the same ten-card `Selecting` offer. Let the beginner
  corporation take it for free, retain `NonBeginnerMode`'s discard, and source starting TR from
  `MultiplayerMode` or `SoloMode`. Dispatch each Player's choice with `TASK`.
- Make `ResearchPhase` provide each Player's four-card offer and discard, dispatching each Player's
  choice with `TASK` and without replay-specific context matching.
- Consider renaming automatic (`::`) effects to “immediate.”

- Replace `PartyRequirement` with its Pets condition: ruling party or at least two of your delegates.
- Let English component roles default to one canonical component noun so placement, requirements,
  and metrics do not repeat singular and plural wording.
- Extend derived `including this` wording to setup operations and otherwise unsupported effects
  without adding card-specific rendering rules.
- Avoid `forEach` in tests; use cases that report failures independently or explicit assertions.
- Review remaining automatic (`::`) sequencing and payment effects, preserving card-tag
  construction, Energy conversion timing, and Pharmacy Union's starting-money timing.
- [ ] Resolve card-payment adjustment timing before queueing remaining `PayingFor` discounts.
- [ ] Decide whether two search signals offer separate card choices or a single two-card task, then
  revisit queued search and claim effects.
- [ ] Rework a colony tile's starting `ColonyProduction` marker without permanent `HAS =1`; the
  track may reset to zero. Resolve the marker task's ownership first.
- [ ] Restore Global Event English rendering for player-owned `GlobalEffect` signals and
  `This IF StartToken` clauses.

- [ ] Extend build-time Canon checks to indirect special-tile gains behind signals or `GainsOf`.
  See [GAME_HACKS](docs/agents/GAME_HACKS.md).

- [ ] Complete public API specifications and KDoc for the
  [nine priority modules](docs/agents/SPEC_FIDELITY.md#priority-scope), then audit conformance.
- [ ] Extend solo-map fixtures beyond Tharsis, Hellas, and Elysium when a scenario needs them. See
  [fixture notes](docs/agents/TESTING.md#remaining-fixture-development).
- [ ] Investigate automatic attack-history effects separately; test migration does not justify
  changing the effects used by Law Suit and Crash Site Cleanup.
- [ ] Enforce the game's offered turn order in `TfmGameplayTest`; `Agent.inTurn` can currently
  start a turn while another Player's turn is pending.
- [ ] Move remaining `.cards` gameplay tests from `TfmGameplay` helpers to explicit `Agent` tasks.
- Distinguish counting Venus tags from carrying one in card availability. Io Sulphur Research should
  work without Venus Next; cards bearing Venus tags should still require it.
- [ ] Continue the [adversarial-play design](docs/agents/ADVERSARIAL.md): settle acceptance, card
  custody, information release, simultaneous choices, and agreed undo.
- Revisit whether `RepeatPlacementBonus` can duplicate `AreaDefinition.bonus` directly while omitting
  unavailable delegate bonuses.
- Resolve nested self-transmutation with identical concrete shared arguments in `pets/BugsTest`,
  preserving shared abstract arguments.
- Reject declarations of new type variables in `OR` triggers. Use separate effects when each
  trigger must bind a variable.
- [ ] Revisit [payment simplification and automatic execution](SOLARNET_ROADMAP.md#internal-design-and-game-execution).
- Let `Class<Aridor>` bring unused `Class<ColonyTile>` definitions into the game, without requiring
  anyone to play Aridor.
- Consider `Name@` shorthand for represented-Class applications, supplier domains, and compact
  resolved spelling.
- Let compact same-class transmutations retain an explicit lexical `This` dependency before
  matching defaults. `CorporationCard<This, Hand FROM Selecting>` currently tries to resolve
  `This` as a class, while omitting it changes the pending task identity.
- Make `ComponentGraph.listenToCount` update only affected subscriptions while preserving initial
  delivery, change-only notifications, cancellation, and rollback behavior.
- When preparing the [PETS repository draft](docs/pets-repo-draft.md) for publication, decide
  whether to include Sponsored Academies, currently retained as a draft note after the gallery.
- After the repository split, rename `:pets-tools` to `:tools` in the Pets repository.
- Before the repository split, choose a Pets publishing host and version convention, automate
  releases, and verify Solarnet against a hosted version. See [release workflow](PETS_ROADMAP.md#release-workflow-before-the-git-split).
- Add scenario saving and reloading to [Mars Playground](PLAYGROUND.md) after the initial version.
- Add shareable [Mars Playground](PLAYGROUND.md#technology-and-experience) URLs after defining which
  scenario and view state they restore.
- Review derived lookups on delegated Catalogs: `classDeclaration` and `allClassNames` use the
  delegate's properties even when the wrapper overrides them. Keep this existing issue separate
  from the class-loading cleanup.
- Simplify Flooding and Artificial Lake's ocean instructions while preserving full-track behavior,
  required placement, and Flooding's victim choice.
- Shorten `DefaultGreeneryTile` with existing Pets, keeping it independent of `Community` and
  rejecting fallback when placement consequences are unaffordable.
- Fix greenery fallback when Mars Nomads blocks the last adjacent land area. `MarsNomadsTest` records
  that both the blocked placement and a distant placement currently fail. Keep promo-specific
  names out of the core greenery rule.
- Use a positive Arcadian Communities tile reward trigger if it can observe the claim before
  ejection without paying during corrections.
- Evaluate `CardFilter` criteria against card Classes in Pets if a small design works. See
  [card handling](docs/agents/CARD_HANDLING.md#external-offer-procedures).
- Make L1 Trade Terminal's allowance count only its own gifts, including when other cards react to
  them; reproduce the interaction first.
- Make Ecology Experts' tags trigger a newly played bio listener, including through Double Down,
  without rewarding an older listener. See `EcologyExpertsTest`.
- Consider a shared party-leader recalculation helper for the ranking repeated on delegate gains
  and Banned Delegate. Replacement cleanup belongs to `PartyLeader`; share the remaining winner
  selection only if it simplifies the model overall and preserves Recruitment's transfer semantics.
- Place neutral solo tiles after the original corporation play and remove Tharsis Republic's
  solo-only +2 M€ production effect. Check Merger and oxygen behavior.
- Find realistic setups for impossible original first actions of Tharsis, Philares, Arcadian
  Communities, and Aridor; only Poseidon has a focused characterization. Do not fill a map
  synthetically just to manufacture the failure.
- Clarify how Thawer markers behave when Snow Cover lowers a marked temperature step and that step
  is raised again. A generic count of temperature increases cannot prove which physical step is marked.
- Fix Point Luna's Earth-tag response so a temporary assigned tag counts for Cartel without drawing
  a card. See the accepted [turn scope](docs/agents/SEQUENCING.md#uses-of-the-scope).
- Keep Fake Self-Replicating Robots permanently in `:tfm-fake`. Move its paired defect scenarios
  from `SelfReplicatingRobotsTest` into that module. Reuse existing test-support sources before
  considering a fixture API.
- Fix FAQ-audit defects in `MiningGuildTest`, `SelfReplicatingRobotsTest`, and
  [MiningRightsTest](test/common/dev/martianzoo/tfm/tests/cards/MiningRightsTest.kt): wild placement
  bonuses, Sponsored Projects on SRR cards, and copied Merger commitment.
- Investigate whether production-box copying can preserve enclosing bindings so
  `EACH ... { PROD[...] }` works for Industrial Complex.
- Reconcile `OtbGame20260912Test` with the original physical-game evidence. Verify the reported
  extra Prelude per player and Blue's three-TR handicap, then express the evidenced setup in the
  correct order.
- Check Double Down outside the Prelude expansion: it should depend on whether a Prelude was
  actually played rather than on the expansion switch alone. Keep this case out of the
  [value-dependency inventory](https://docs.google.com/spreadsheets/d/13WRf7ljJLuy3iwTr5caQgKhTPhNugPKJuGx1ikALshY/edit?gid=0#gid=0)
  for now.
- Implement individual Turmoil party and whole-map selection described in the
  [Pets roadmap](PETS_ROADMAP.md#canon-and-game-rule-modeling).
- Follow the bundle-loading metadata move with a separate look at the legacy REPL option-code
  mapping and content-specific English describers. Both still name game content in Kotlin; neither
  determines which declarations a bundle loads or which cards its pool selects.
- Move the map-diagram area kinds and bonus sigils in `MarsMapReader` into authored map data together
  with their delegate handling; the generated area Pets and diagram reader must continue to agree.
- Derive the standard-resource action-cost set in `TfmActionLowerer` from the
  `StandardResource` Pets hierarchy after resolving when action lowering has that hierarchy.
- Revisit the expansion names in `TfmWorkflow`'s Prelude and solar-phase sequencing when moving
  game-phase rules out of Kotlin; this is separate from card-pool selection.
- Consider allowing owner-local Class declarations only in gain instructions. They currently also
  parse in other expression positions, including a selector's `HAS` refinement; decide the intended
  boundary and account for existing uses before restricting the syntax.
- Decide whether configurations need narrower card requests and how to exclude cards from automatic
  pools when ordinary compatibility checks cannot prove them impossible.
- Decide whether excluding a replacement card restores its original version. Card and goal
  replacements currently differ; cover the chosen rule in configuration tests.
- Consider allowing identical goals in both `goals` and map bundles, with a clear source rule for
  individual selection.
- Replace `ModulesReady` by entering `BootstrapPhase` after premise components exist. Revisit
  `Initializer` order and the `AfterMe` cycle; see [engine notes](docs/agents/ENGINE.md).
- Replace the duplicated `TemperatureStep BY Player`/`BY Admin` threshold-ocean triggers and the
  synthetic `AdminOceanPlacement` signal with one rule that separates who chooses the tile from
  whose action the placement is attributed to, shared by the standard and extended tracks.
- Preserve Player identity across Player → Admin → Player task chains without fake ownership of
  bookkeeping classes. See [identity notes](docs/agents/IDENTITY.md).
- Revisit causal ownership inside `BootstrapPhase`, moving initialization work under ordinary
  phase-caused tasks as soon as the required runtime state can express them.
- Let refinements and `HAS EVAL` properties reference their candidate explicitly if Canon needs it.
  See [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).
- Decide whether `RANK Me@Player` should bind `Me` inside its selector refinement like `EACH`.
  See [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).
- Revisit ownerless rule subscription inference, which can change when a result gains a bare Player
  resource. See [identity notes](docs/agents/IDENTITY.md#ownership-transition-current-code-and-saved-experiments)
  and [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).
- Specify Wildlife Dome's `GreeneryTile<>` placement explicitly and verify its card play; current
  elaboration fails without a gain dependency default.
- Add structural conjunction in Pets so owned-tile rules can drop nominal `OwnedTile`.
- Separate the expression API's three intents: an object's natural available expression, a resolved
  Type's compact expression, and its full expression. Keep syntax expressions universe-independent;
  converting an arbitrary expression to either resolved form must take a `ClassTable` explicitly.
- Weed the vague terms `operation` and `gameplay command` out of the engine. Rename each use for
  the exact lifecycle it denotes, including atomic calls, task completion, and turn-order play.
- **Low priority:** [#54: ownership-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54)
  — Resolve contextual ownership correctly and display the resolved player.
- Consider requirement-gated action costs, using United Nations Mars Initiative to make
  `HasRaisedTr` a prerequisite to paying its 3 M€ rather than a gate around the result.
- Derive selected singleton card watchers without explicit support-Class invariants. The current
  sites are United Nations Mars Initiative and Pristar retaining `TrWatcher`, and Hydrologist
  retaining `HydrologistWatcher`.
- **Low priority:** [#41: `list`](https://github.com/MartianZoo/solarnet/issues/41) — Improve
  hierarchy/dependency descent, grouping, depth, concrete subtypes, and explicit `<Anyone>` display.
- Replace the [archived Life of an Effect walkthrough](docs/archive/life-of-an-effect.md) with a
  current account of lexical `Me` and the transformation pipeline.
- Reconsider Turmoil's `PartyLeader` representation and name. It currently supplements the actual
  `PartyDelegate` as a non-`Delegate` role; evaluate the exploratory
  [Leader/Follower delegate model](docs/agents/DELEGATES.md), especially choice-free subtype
  selection and leadership after removal, before deciding whether to replace the marker.
- Find a clean way to make Turmoil's `ApplyRulingBonus` player-owned without complicating the Reds
  tied-lowest-player selection. It currently remains one global signal whose party effect fans out
  over the players.
- Make Preservation Program compatible with Turmoil's Reds without requiring Turmoil. Update the
  premise gate, `ModuleSelectionTest`, and `docs/what-is-supported.md` when resolved. Keep
  Terraforming Deal's reversed-TR payout separate.
- See whether `CimmeriaPlacementBonus`, `PlaceNeutralTiles`, and `StageForReplicatedProject` can
  avoid named helper Classes while preserving their ownership and selection behavior.
- Consider rejecting `@` markers on concrete types, such as `Class<@BuildingTag>`, where the
  represented class is already fixed. Decide whether this should be an authoring error.
- Carry resource/file names through Pets parsing and generated catalog inputs so diagnostic spans
  identify the original file as well as the submitted text, line, and column.
- Preserve authored source locations through runtime narrowing and task normalization.
- Preserve the empty-intersection reason when rejecting a partial task submission. For an
  unavailable Type, the current fallback can misleadingly blame an omitted dependency instead.
- Improve the specific caret targets and related-source context recorded beside message assertions
  in `CatalogDiagnosticsTest` and `PostCatalogDiagnosticsTest`; consider rendering span widths as
  well as their starting positions.
- Clarify T10-4 when an inherited dependency default conflicts with a subclass's bound, as in
  `MoholeArea_SpecialTile`.

## Autonomous Follow-ups

- Reconcile the Valley Trust English snapshot: `EnglishTest` expects its Prelude action text, but
  the renderer currently emits `[ValleyTrust_RequiredAction]`. Restore `:tfm-text` tests to the
  standard build once the English suite passes.
- Refresh [English wording comparisons](docs/english-wording-comparisons/README.md) from current
  Canon using `writeEnglishTextComparisons`.

- Clarify combined-production requirement wording: Metallurgist counts steel and titanium
  production together. Add a focused assertion.
- Render conditional choices between standard and required actions from shared semantics;
  `FakeHeadStart` currently shows bracketed Pets.
- Model when a Prelude may fizzle; `PlayPrelude` currently leaves that check to the caller.
- Use `spend` consistently for action costs before `->` and in `THEN`; keep `pay` for other
  payments. Check Focused Organization with Stormcraft in `FocusedOrganizationTest`.
- Extend card-play English to combine modeled face restrictions with deck and source location,
  including discounted and requirement-waived plays.
- Reconcile Astra Mechanica's printed two-card return with its `2 TakeEventCard` signal and
  zero-card behavior, without a card-specific English rule.
- Define game-neutral bundle compatibility for optional references; continue with Suitable
  Infrastructure, Constructor, and Soil Studies. Let English use the same modeled fact.
- Move `PreludePhase` out of `tfm-text`'s `resetsForPreludeAction` recognizer once there is a
  principled bundle-supplied description of the phase/latch relationship; do not add a Prelude-only
  boolean merely to relocate the class name.
- Render the five beginner corporation copies from their inherited modeled setup semantics.
- Complete the three visible goal-text refusals only from modeled semantics: Briber's direct effect
  claim instruction, Philantropist's `GainsOf` metric, and Suburbian's map-edge concept.
- Finish Global Event text from modeled semantics for ranked selections, resource-card fanout,
  and procedure bodies. Keep unsupported nodes visible.
- Fill the missing printed transcriptions identified in
  [the wording comparisons](docs/english-wording-comparisons/README.md) from verified printed evidence.
- Correct defaulted Type-variable references: `pets/BugsTest` shows bare references incorrectly
  inheriting their supplier's `<>`.
- Fix atomized gains in the first stage of `THEN` when that stage expands to a group.

- Reconcile `SoloGame20230721Test`'s 95 VP with its original record (`gf33a06d07a1c`), which
  also mentions 99 and 111.

- Preserve unresolved shared aliases and unnamed Type unions during instruction intersection.
- Preserve a combined selection and narrowing when contextual resolution splits a task into siblings.
- Complete `TaskForm` options for `EACH`, nested instructions, optional changes inside `PER`, and
  forced type resolution before a later amount.
- Express face/back compatibility in selectable `PlayCard` so generic forms restrict Prelude faces.
- Extend the Mining Rights/Area wild-resource test after placement is fixed: Robotic Workforce and
  Cyberia Systems may choose either originally available metal production.

- Revisit the multiplatform JVM test runner's alignment with the repository JUnit BOM.

- Revisit Dokka's transitive Jackson, jsoup, and FreeMarker advisories when a stable update exists.

- Review the [class-existence scenario draft](docs/class-existence-scenarios-draft.md), then
  consolidate repeated cases in `ClassDefinitionBoundaryTest` and `ClassTableSelectionTest`.

- Find a principled way for narrower dependency defaults to retain compatible refinements from
  wider defaults, so `Tile` can own area occupancy once while its subclasses select their kinds of
  areas and add placement rules.
- Simplify `LiveEffect` actor binding by threading a binding context through subscription matching
  instead of maintaining parallel `Subscription.transform()` implementations and `Hit.before()`.
- Separate `Instructor`'s resolution-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `InstructionResolutionTest`.

## Bugs from the 2026-10-02 review

- Resolve a corporation's mandatory first action immediately after Merger; an impossible action
  invalidates the Prelude play. See [MergerTest](test/common/dev/martianzoo/tfm/tests/cards/MergerTest.kt).
- Reject a type-variable marker on a `DEFAULT` root; see [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).
- Point parser diagnostics at the invalid character in `Foo<~ Bar>`; see
  [Pets BugsTest](test/common/dev/martianzoo/pets/BugsTest.kt).
