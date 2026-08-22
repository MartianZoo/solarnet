# TODO

Only current work belongs here; issue links provide background. Inline TODOs should be brief context pointers. Priorities descend from very soon to low.

## User Ideas and Agreed Directions

- **Medium priority:** Reorganize the automated tests around the explicitly valued suites. Move
  `CoreRulesTest` out of the `cards` package when the final functional-test layout is settled, and
  keep core-rule scenarios at the player-action and observation boundary.
- Derive `CardDefinition.resourceType` from the card's authored content instead of storing it as
  independent imported data.
- **Low priority:** Extend premise viability beyond exact uninhabited-domain facts; Law Suit being
  unviable in solo because no opponent-dependent attack record can inhabit the projected Type
  universe is the canonical stronger proof.
- **Medium priority:** Settle and prototype the generic `EACH Type { ... }` fanout proposed in
  [`docs/agents/EACHPLAYER.md`](docs/agents/EACHPLAYER.md), keeping delegation and distributed
  completion separate.
- **High priority:** Implement preparation-time delegated narrowing. The controller chooses when to
  prepare a parent task, the delegate alone narrows its child, and the controller remains blocked
  until that child completes. Fix Philares first, then prove Engine narrowing for real-card deals
  and Player delegation for Enceladus.
- **Low priority:** Develop the class-property cardinality, abstract-default, RequirementGroup, and `Instruction*`
  directions recorded in [`docs/agents/PROPERTIES.md`](docs/agents/PROPERTIES.md).
- **Low priority:** Support requirement adjustment when one part of a compound card requirement is
  a global-parameter requirement.
- **Medium-high priority:** Complete the Pets Action semantics settled in
  [`docs/agents/ACTIONS.md`](docs/agents/ACTIONS.md): standard-resource costs now become invoices;
  lower costless and direct-cost Actions through their provider- and action-qualified
  `CostPaid` signals. Keep Head Start completion scopes and action-use marker behavior separate.
- Convert card purchases to the payment workflow so BuyCard price modifiers adjust Owed instead of
  creating separate money changes.
- **High priority:** Allow a partial instruction to narrow the matching portion of exactly one
  pending task while preserving the task's untouched structure
  ([#30](https://github.com/MartianZoo/solarnet/issues/30)).
- **High priority:** Make task queues semantically unordered: remove positional task selection and
  stable-order autoexec precedence, remove `FIRST`, require an id or unambiguous instruction match,
  and run tests under reverse and reproducibly randomized enumeration to expose hidden ordering
  dependencies. Autoexecution policy belongs outside the engine as specified in
  [`docs/agents/AUTOEXEC.md`](docs/agents/AUTOEXEC.md).
- **Medium priority:** Explore immutable task priority, starting with Trade and PlayCard: tasks may prepare only at the
  highest occupied priority in their control scope, without task-targeting effects or mutation.
  Test whether Trade can delete its pure scheduling barrier and whether PlayCard can directly create
  reduced-priority card-entry and event-cleanup work while preserving auditable `Owed` and
  `Required` components. Keep this distinct from `THEN`, state gates, and scoped drain.
- **Medium-high priority:** Finish replacing the legacy “linkage” terminology and machinery with the
  Type-variable model, including unifying Class-header Type-variable recognition with ordinary
  scopes: bind whole abstract Expressions, keep sibling argument branches independent, propagate
  variables into Effects only at their named Expressions, and reject conflicting replacements
  (`docs/agents/TYPES.md` §12.1, §12.3, §12.4).
- **Medium-high priority:** Heavily revamp the `TfmGameplay` and test-helper APIs: move test-only
  actions such as `playCorp` and `playProject` out of production, remove or replace `SampleGames`,
  and give benchmarks explicit harness utilities rather than inheriting the test convenience
  surface.
- **Medium priority:** Finish disposable Game World forks and overlays: overlay components and
  live effects, copy the small task queues, extend event history from a captured prefix, and
  preserve one clear revision boundary for prepared tasks.
- **Medium priority:** Move more expansion-specific knowledge out of Kotlin and into Module/Pets
  data, starting with workflow phase insertion and Terraforming Mars registries that enumerate
  expansions directly.
- **Low priority:** Consider compiling Pets during the build into validated runtime artifacts, but
  only if one compiler can replace runtime parsing/validation without creating a second semantic
  model.
- **High priority:** Replace projection-local Class and Type ownership with the Authority-wide identities and explicit
  game-filtered views proposed in
  [`docs/agents/CLASS_TABLES.md`](docs/agents/CLASS_TABLES.md); eliminate reverse navigation from
  `Class` and `Type` to a game `ClassTable` and stop rebuilding every Class for every game.
- **Low priority:** Investigate why the oxygen steps created by SoloOpponent's setup greeneries do not award it TR,
  and whether adding and then removing those steps has any other observable consequences.

### Making Solarnet AI-player-ready

- **Medium-high priority:** Move beyond follow-along mode by modeling shuffled decks, deals, draws, and actual private hands. Include the bidirectional represented-family link from [`docs/agents/REAL_CARDS_MODE.md`](docs/agents/REAL_CARDS_MODE.md): `CardBack` carries its exact `Class<CardFront>`, while `CardFront` carries its `Class<CardBack>` family.
- **Low priority:** Provide one strict player-relative observation and visible-history interface that cannot expose opponents' cards, hidden deck order, or private events; use that same boundary for training, evaluation, and live play.
- **Low priority:** Provide a stable, machine-learning-friendly action interface that enumerates or scores complete legal choices while preserving the relationship among the engine's lower-level card, payment, target, quantity, and placement tasks.
- Support parallel or batched game simulation for AI search and training.
- Add a replay format or dataset pipeline.
- Compare players across identical random seeds so evaluation does not confuse luck with strength.
- **Low priority:** Complete the rules/content needed by the chosen research configurations, especially unsupported unusual mechanics, Turmoil, and the remaining Prelude 2 cards; maintain explicit supported-content manifests so experiments cannot silently use incomplete games.

### Soon

- **Medium priority:** Continue the unresolved sequencing work identified by the canon effect-mode
  audit: settle the action-marker/Viron tension, event and Mandate context lifetime, scoring
  completion, same-trigger action-cost dependencies, and the remaining mixed phase triggers,
  including existing automatic effects triggered only by Engine workflow events. Before inventing
  an automatic `THEN`, distinguish inline continuation, frozen trigger-time choice, and
  descendant-completion semantics.
- **High priority:** Make Artificial Lake's concrete legal ocean placement refine and execute
  normally, without the solo whole-game test's mandatory `!` override.
- **Low priority:** When Helion is implemented, settle whether AMAP for a Mons Insurance payment considers heat before determining the payable amount; do not allow payment substitution to short the victim while preserving M€.
- **Medium priority:** Model the solo setup choice that selects four colony tiles and removes one
  before assembling the playable Game World.

### Medium Soon

- **Low priority:** Rethink Complement Types as one design problem, including domain preservation, abstract-candidate
  narrowing, Complement combination, and nested-variable behavior, before patching the individual
  failures (`docs/agents/TYPES.md` §12.2, §12.5, §12.6).
- Generalize corporation-play support so Merger can play its second corporation without also
  assuming corporation-phase card buying or a separate full turn; then remove the raw
  `PlayCard<CorporationCard>` calls from the solo whole-game tests.
- Finish unifying `OR` semantics and construction across the non-Metric AST families: reject
  duplicate authored arms; let programmatic factories deduplicate in first-occurrence order; and
  preserve significant trigger order. Metrics now reject duplicate authored arms and non-component
  unions while their factory deduplicates in first-occurrence order
  ([#63](https://github.com/MartianZoo/solarnet/issues/63)).
- Reduce the remaining custom Pets instructions only where behavior can become hand-authored Pets; start with the candidates and constraints in [`docs/agents/REDUCE_CUSTOM.md`](docs/agents/REDUCE_CUSTOM.md).
- Revamp workflow behavior. Head Start must allow any first action and then grant an additional second action; move Colonies fleet return from Generation into the Solar phase.
- **Medium-high priority:** Consolidate exception cleanup
  ([#42](https://github.com/MartianZoo/solarnet/issues/42)): catch only expected script/domain
  failures, preserve defects and stack traces, use precise MartianZoo exceptions at domain
  boundaries, and narrow `Instruction.narrows`.
- **Low priority:** [#54: Owner-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54)
  — Resolve contextual ownership correctly and display the resolved player.
- Reorganize Kotlin packages so each Gradle module owns a strong, recognizable package subtree; once ownership is unambiguous, consider merging physical source directories into shared package-shaped trees.
- **Medium priority:** Model Prelude plays as explicit first and second turns.
- Move autoexecution out of the engine into optional clients of `Gameplay`: remove implicit drains,
  replace modes with named policies, record the issuing agent, and initially provide only policies
  that prove they make no gameplay sacrifice; see
  [`docs/agents/AUTOEXEC.md`](docs/agents/AUTOEXEC.md).
- **Medium priority:** Separate Authority data from premise resolution, and split `TfmAuthority`'s
  generic declaration aggregation/validation into `Authority` from the Terraforming Mars registries
  in `TfmAuthority`.
- Extract shared `Definition`-to-`ClassDeclaration` assembly without hiding category-specific behavior.
- Follow `docs/agents/API.md`: simplify the existing engine into a flat, trusted workhorse by removing gameplay power layers and `godMode()`, keeping integrity-preserving mutation internal, and enforcing REPL color modes locally in `script`; design the restrictive client API separately.
- Install and configure Kotlin ABI/binary API validation for public `pets`, `engine`, `canon`, and `script` APIs.
- Profile and reduce type-system allocation in `Type.glb`, `narrows`, and repeated dependency/refinement construction without risking correctness.

### Medium Priority

- **Medium-high priority:**
  [#60: Auto-narrowing](https://github.com/MartianZoo/solarnet/issues/60) — Define small,
  independently selectable autoexecution policies that can prove and submit forced task narrowings
  without making raw preparation search through player choices.

### Low Priority

- **Very low priority:** Fix Public Plans so “any number” cannot be zero and the card is unavailable
  when the player has no other card to reveal; keep the current wrong behavior characterized in
  `BugsTest` until fixed.
- Prevent Solar Probe's event cleanup from preempting its card draw.
- Implement the standard-game rule that starts every production at 1 when Corporate Era is disabled; this rounds out game modes and demonstrates replacements. Until then, keep canonical requirements at their printed values.
- [#64: Multiple tiles](https://github.com/MartianZoo/solarnet/issues/64) — Decompose `2 CityTile` into two placement choices; consider making `Tile` atomized.
- Model `StateChange` as a sealed gain/remove/transmute algebra so invalid nullable combinations are unrepresentable.
- [#22: `ELSE`](https://github.com/MartianZoo/solarnet/issues/22) — Use the fallback only when no complete narrowing of the first branch works; target WGT and Pharmacy Union first.
- After `OverlayWorld`, consider retaining standalone Task preparation's successful speculative
  event suffix so later execution can fast-forward it when the backing Game World has not changed.
- [#59: `-This` Quantifier](https://github.com/MartianZoo/solarnet/issues/59) — Decide whether self-removal should default to mandatory.
- [#41: `list`](https://github.com/MartianZoo/solarnet/issues/41) — Improve hierarchy/dependency descent, grouping, depth, concrete subtypes, and explicit `<Anyone>` display.
- Explain or remove `Initializer`'s synthetic mandatory Quantifier.
- Split `Instructor.prepareChange` into narrowing, custom translation, and limit-checking stages.
- Document the `BuyCard`/payment protocol and verify delayed 3 M€ payment cannot be exploited.
- Move Pets AST generation to Kotest property tests only if domain-aware shrinking improves failures.

## Autonomous Follow-ups

- Decide whether `Summarizer` should attribute invoice settlement back through `Payment` and
  `Owed` to the Action provider; standard-resource Action costs are currently attributed to the
  payment machinery rather than cards such as Search for Life.
- Let invoice lowering compose a fixed base with a per-component increment as one exact debt;
  `FundAwardSA` must retain its gated 8/14/20 gains until separate `Owed` gains cannot expose
  payment and `CostPaid` between invoice parts.
- Preserve and enforce the existing `GameReader` boundary that prevents game mechanics, including
  custom Classes, from reading `EventLog`; add an architectural check so event history remains
  diagnostic and gameplay-state equivalence can depend only on the `ComponentGraph` and
  gameplay-relevant `TaskQueues`.
- Decide whether `Ok` narrows a gated instruction; `Gated.ensureIsNarrowedBy` currently throws a
  `ClassCastException` instead of expressing the semantic result.
- Retain projection-decision provenance so premise diagnostics can explain automatic filtering and
  complete hard-reference paths, rather than only the selected Definition or immediate source.
- Implement Established Methods' unaffordable-second-project fallback, then replace the deliberately
  incomplete substitute used by Game20260819.
- Decide how source-backed physical-game turn-order violations or transcript gaps should be
  represented without reordering or inventing actions; Game20260818 currently uses a standalone
  reconciliation for a patent sale taken beyond the normal action allowance.
- Render conditional `End IF` scoring effects compositionally; they are now classified as bottom
  text and fall back honestly, with Search for Life as the canonical example.
- Represent the printed region for immediate instruction groups explicitly enough to distinguish
  Stratospheric Birds (removal above the artwork beside its action) from cards such as Potatoes
  (the whole immediate group below) before expanding English card-resource removal derivation.
- Replace the implementation's `phantom` vocabulary with game-view inhabitation queries as part of
  [`docs/agents/CLASS_TABLES.md`](docs/agents/CLASS_TABLES.md); avoid a standalone rename if that
  ownership change is underway.
- Investigate the intermittent Kotlin/Karma reporter crash during the unfiltered engine browser
  suite: targeted browser suites and the normal smoke test pass, but the reporter can lose a
  successful spec's console result and terminate the full run.
- Present other pre-payment resource refunds, especially reduced trade costs, as player-facing
  discounts once their action effects become structurally derivable.
- Keep the `Award` base class's scoring effect inherited while avoiding loading its scoring
  machinery in solo games, where no concrete Award definitions are active.
- Break `PetTransformer.transformChildren` into focused rebuild helpers; its instruction-tree
  support has made the existing cyclomatic-complexity suppression increasingly costly to maintain.
- Complete the unsupported Milestones & Awards goals: Briber's special claim cost, Hydrologist and
  Thawer's player-attributed global-parameter steps, and the Turmoil-dependent Lobbyist and
  Politician rules.
- Simplify `LiveEffect` actor binding by threading a binding context through subscription matching
  instead of maintaining parallel `Subscription.transform()` implementations and `Hit.before()`.
- Make class-literal parameters specialize inherited generic effects through a subclass, so solo
  resource helpers can share replenishment behavior without a holder/resource dependency cycle.
- Define the `script` command's relative-path policy and correct its help text, which currently
  promises paths relative to the repository while `File(args)` actually uses the process working
  directory.
- Separate `Instructor`'s preparation-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `PrepareTest`.
- Canonicalize unambiguous authored dependency arguments by key before implicit-variable matching, so equivalent argument orders share a variable as intended (`docs/agents/TYPES.md` §12.7).
- Replace `World.onAtomicComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.
