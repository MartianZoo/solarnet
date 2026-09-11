<!-- Only miscellaneous work not already covered by a focused grand-plan document in docs/agents/ belongs here. -->

# TODO

Issue links provide background. Inline TODOs should be brief context pointers.

## User Ideas and Agreed Directions

- Rewrite the agent documents that have outgrown their intended teaching or orientation role:
  - make `ENGINE.md` only a quick tour of the runtime's major pieces;
  - make `GAMEWORLD.md` a quick orientation to the intended `state`/`engine` split;
  - reduce `API.md` to its important core principles and decisions;
  - make `RESPONSIBILITIES.md` tight and focused;
  - make `SEQUENCING.md` an overview of intra-turn ordering mechanisms and ideas for improving them;
  - rewrite `IDENTITY.md` as a teaching document about the runtime roles and their uses;
  - make `QUANTIFIERS.md` a focused educational reference;
  - reduce `EACH.md` to a terse feature reference;
  - let `AUTOEXEC.md` broadly survey the ideas for improving autoexecution;
  - make `PROPERTIES.md` a focused feature reference followed by the possible future design for
    instruction-valued properties;
  - replace `OPTIONS.md` with a focused educational explanation of the features;
  - reduce `TESTING.md` to the important repository-specific guidance; and
  - rewrite `JVM_TEST_PERFORMANCE.md` holistically, incorporating the work on `perf` and keeping the
    document identical on `perf` and `main`.
- Revisit the tested `GenerationScope` lifetime model preserved in stash commit `d8a94cc1c`.
- Revisit the cleanup-vocabulary draft that removes broad `Barrier` waits, preserved in stash commit
  `db9302652`.
- Review the committed `OverlayWorld` and query-performance work on branch `perf` before integrating
  selected changes into `main`.
- Do not let `Engine.newGame` exit bootstrap until it has validated every invariant against the
  completed World, including positive lower bounds and correctly scoped dependent-component
  invariants.
- Have the normal full application build stamp its output with the current Git commit and, when
  source changes are present, a stable hash of those changes. Include that stamp in every exported
  game record so a log identifies, or can later verify, the engine source that produced it.
- Make tile placement over an owned `Community` an atomic transmutation, then enforce
  `HAS MAX 1 Occupant<This>` on every `Area` and remove card-level empty-area refinements.
- Revisit causal ownership inside `BootstrapPhase`, moving initialization work under ordinary
  phase-caused tasks as soon as the required runtime state can express them.
- Let refinements reference their candidate explicitly, so a selector can relate a nested
  dependency to that candidate without repeating its complete expression.
- Give Pets a real structural conjunction, spelled something like `Tile(IS Owned)`, and retire the
  nominal `OwnedTile` class once `Landlord` and the other owned-tile rules can name the intersection
  directly. Until then a broad active projection checks the nominal relationship; cover every legal
  configuration family systematically so a mutually exclusive option cannot evade it.
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
- Discard the uncommitted typed custom-metric/code-generation experiment; it was evaluated and
  considered an unsuccessful direction.
- Complete `Game20260820Test` beyond its current partial generation-6 checkpoint using the preserved
  log, player data, and eight later screenshots; keep every new checkpoint independently sourced.
- Install and configure Kotlin ABI/binary API validation for public `pets`, `engine`, `tfm-canon`,
  and `script` APIs.
- Profile and reduce type-system allocation in `Type.glb`, `narrows`, and repeated
  dependency/refinement construction without risking correctness.
- Let `CustomMetric` optionally provide candidate-selection hooks so `EACH` refinements such as
  tile adjacency can avoid evaluating the metric against every live component.
- **Medium-high priority:** Consolidate exception cleanup
  ([#42](https://github.com/MartianZoo/solarnet/issues/42)): catch only expected script/domain
  failures, preserve defects and stack traces, use precise MartianZoo exceptions at domain layers,
  and narrow `Instruction.narrows`.
- **Low priority:** [#54: Owner-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54)
  — Resolve contextual ownership correctly and display the resolved player.
- **Low priority:** Consider compiling Pets during the build into validated runtime artifacts, but
  only if one compiler can replace runtime parsing/validation without creating a second semantic
  model.
- **Low priority:** Investigate why the oxygen steps created by SoloOpponent's setup greeneries do
  not award it TR, and whether adding and then removing those steps has any other observable
  consequences.
- Keep looking for a better representation of Splice Tactical Genomics.
- **Low priority:** [#41: `list`](https://github.com/MartianZoo/solarnet/issues/41) — Improve
  hierarchy/dependency descent, grouping, depth, concrete subtypes, and explicit `<Anyone>` display.
- Model `StateChange` as a sealed gain/remove/transmute algebra so invalid nullable combinations are
  unrepresentable.
- [#59: `-This` Quantifier](https://github.com/MartianZoo/solarnet/issues/59) — Decide whether
  self-removal should default to mandatory.
- Investigate whether the three self-handling signals `CimmeriaPlacementBonus`,
  `PlaceNeutralTiles`, and `StageForReplicatedProject` can avoid named helper Classes without
  requiring authored references to generated names. Preserve Cimmeria map generation,
  `PlaceNeutralTiles`'s system-only ownership, and SRR's explicit card-Class selection.
- Have the payment lowering in `Transforming.actionToEffects` receive its standard-resource Class
  names from `tfm-canon` instead of the hardcoded `standardResourceClasses` set in `pets`; that set is
  Terraforming Mars data sitting in the language core, and it is the only reason generic Action
  lowering knows any game's vocabulary.
- **Better Task Disambiguation:** let callers state the intended task without searching the task
  pool; use extra identity only when distinct tasks accept the same narrowing. Prior partial work is
  in commits `fc84e1490` and `a76bb9949`. Current use cases:
  - `TfmTest` and `RecordedGame` search tasks for tile placement, card-resource placement, qualified
    declines, and wild-tag assignment.
  - `TfmGameplay` searches for project-card offers/discards, the second-action offer, wild-tag
    offers, billing and payment tasks, and the variable-X task.
  - `TfmPlayCommand`, `TfmActionCommand`, and `TfmPayCommand` search for standard actions, action
    costs, invoices, and payment offers.
  - `TaskDelegationTest`, `PhilaresTest`, `NewPromoCardsTest`, and `PropertyTest` recover a task by
    scanning ids or instruction text before selecting or dropping it. Keep mechanism assertions
    separate from gameplay calls when designing the replacement.
  - Functional cross-player handoffs already proceed without explicit selection under `CONCRETE`
    when the handoff is the only selectable task. The remaining tests mix it with forced sibling
    work; `CONCRETE` stops because it cannot prove an order harmless. Prefer explicit sequencing or
    a narrow proof of harmless reordering over making `CONCRETE` execute an arbitrary concrete
    sibling.
  - Compare a context-component `ClassName` selector (for example, Search for Life or Big Asteroid)
    with matching the original pending instruction and with an already-held stable `TaskId`. Keep
    ordinary `doTask(concreteNarrowing)` as the default path.

### Hypothetical Card Behavior

- Make `VictoryPoint` depend on the scoring `Component`, and define a scoring-completion phase if a
  future score depends on another score rather than directly on game state.
- Decompose a future card's `2 CityTile` instruction into two placement choices; consider making
  `Tile` atomized ([#64](https://github.com/MartianZoo/solarnet/issues/64)).

## Autonomous Follow-ups

- Repair the two declared Pets conformance gaps without adding a second representation of type
  identity: L7-8 lets `Tile<> THEN Tile<>` stages diverge after defaults, and T8-3 can substitute a
  refinement candidate into the wrong one of several compatible dependency slots while existing
  cards still require candidate/argument merging.
- Find a principled way for narrower dependency defaults to retain compatible refinements from
  wider defaults, so `Tile` can own area occupancy once while its subclasses select their kinds of
  areas and add placement rules.
- Model L1 Trade Terminal's three-distinct-card resource choice, then replace `FakeL1TradeTerminal`
  with the canonical card.
- Serve copied Canon resources from the game-viewer Karma configuration; the resources reach the
  test package, but `:game-viewer:jsBrowserTest` currently gets a 404 for
  `canon/resource-index.txt`.
- Investigate the intermittent Kotlin/Karma reporter crash during the unfiltered engine browser
  suite: targeted browser suites and the normal smoke test pass, but the reporter can lose a
  successful spec's console result and terminate the full run.
- Complete the unsupported Milestones & Awards goals: Hydrologist and Thawer's player-attributed
  global-parameter steps, and the Turmoil-dependent Lobbyist and Politician rules.
- Simplify `LiveEffect` actor binding by threading a binding context through subscription matching
  instead of maintaining parallel `Subscription.transform()` implementations and `Hit.before()`.
- Separate `Instructor`'s resolution-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `InstructionResolutionTest`.
- Replace `World.onTransactionComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.
