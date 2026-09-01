# TODO

Only current work belongs here; issue links provide background. Inline TODOs should be brief context pointers. Priorities descend from very soon to low.

## User Ideas and Agreed Directions

- Define player-decision attribution through causal ancestry. `Hidden` components may be transparent
  links between an event and its most closely connected player decision, but postpone choosing the
  exact traversal rule and how it combines `Cause` with `GameplayInputEvent`.
- Decide whether `System` should prevent Player removal as well as Player creation. Audit legitimate
  player-caused cleanup and transmutation before strengthening the current creation-only rule.
- Decide whether persistent auxiliary action providers such as `NeptunianOption` and
  `CathedralOption` should ever be hidden. Keep them visible while they are live choices offered to
  players.
- Treat meaningful owned, non-Signal, non-Temporary state as visible by default. Audit the remaining
  hidden owned bookkeeping (`SuitableInfrastructurePaid`, `MyResourceWasRemoved`, and
  `MyProductionWasDecreased`) while retaining explicit derived and engine-scaffolding exceptions.
- Decide whether `Adjacency` is a visible physical/iconographic relationship or hidden derived
  structure; distinguish it from `Neighbor`, and keep it visible until that decision is settled.
- Revisit whether `DelayedColonyTile` is `System` after settling causal and presentation semantics;
  leave it non-System for now.
- Decide whether the persistent invoice hosts `BuyCards` and `PlayCards` are `System` while the
  latter awaits its dedicated lifecycle redesign.
- Make context-specialized observational metric counts safe: after validating the general class effect,
  reduce a `Metric.Count` to zero when binding `This`, `Owner`, or an authored type variable makes its
  previously valid type expression structurally disjoint. Preserve errors for malformed expressions,
  triggers, and constructive instructions; represent disjointness explicitly instead of swallowing
  `ExpressionException`.

- Weed the vague terms `operation` and `gameplay command` out of the engine. Rename each use for
  the exact lifecycle it denotes, including atomic calls, task completion, and workflow play.
- Make the ordinary Pets lifecycle explicit and linear: elaborate every authored entry through one
  shared route, preserve Type-variable identity in typed values, distinguish contextual `Owner`
  from an intentional Owner-domain choice, preserve the icon grammar's implicit `BY Owner` and
  explicit `BY Anyone`, close component and trigger context before Task execution, and translate
  each resolved first stage to a small executable-work type while retaining later `THEN` stages as
  Pets.
- Preserve authored Pets as stable semantic data independently of executable compilation, including
  Metric- and Requirement-valued properties; reflection-like consumers may inspect or re-submit any
  authored subtree, and re-submission must use the shared elaboration route.
- Rename instruction `Intensity` to `Quantifier` throughout.
- **High priority:** Continue auditing `AutoExecPolicies.safe` against its proof obligation. It now
  acts only when the entire World has one pending task assigned to its Agent; it may select an
  abstract singleton without choosing its narrowing. Prove that execution preserves every
  continuation before broadening beyond the singleton case. Use
  [`SMART_AUTOEXEC.md`](docs/agents/SMART_AUTOEXEC.md) as the proof contract.
- **High priority:** Implement the generic `slow` autoexecution policy after disposable Worlds can
  explore every relevant legal command and compare normalized component/task continuations. It may
  spare no analysis cost, but `UNKNOWN` must stop it; do not substitute a heuristic for the promised
  proof.
- **High priority:** Try `Temporary` on `TradeBarrier`. It is suitable only if global queue
  emptiness is the real end of the selected trade operation and its removal still leaves fleet
  movement after every optional production decision. Then examine payment independently: replace
  parallel payment-method tasks with one mandatory abstract accepted-tender choice at a time, keep
  it pending and unselectable while unpaid without legal tender, and preserve invoice removal as
  the direct completion event. Keep whole-World idle cleanup separate from action-local completion
  and from cleanup of pending tasks such as `WildTagUse`.
- **High priority:** Define end-of-action completion before replacing workflow wakeup. Use one
  existing Player-turn control frame rather than a general nested-frame facility, and ensure Billing,
  action-local cleanup, and delegated tasks settle before a second-action offer or control pass.
  For Head Start, prefer using the current Prelude turn for its first immediate action and granting
  one later ordinary action turn after settlement; record that timing as a house rule if exact canon
  requires one indivisible two-action operation.
- Prototype representing the one active task controller as a singleton World Component gained when
  workflow grants control and retained through delegation and settlement. If every consequence can
  derive its queue from that fact, remove `Task.controller` and its propagation; choose the final
  Pets name and shape only after proving the invariant.
- Keep `TfmGameplay` completion cleaning up `WildTagUse?` tasks when they are the acting Player's
  only remaining work. Give `WildTagUse` automatic action-slot lifecycle cleanup once scoped
  completion can express “after its tag choice or decline,” then remove both temporary cleanup
  bridges.
- **High priority:** Extend Distant Pressure Mass's exact located-card follow mode to other
  source-complete full-game replays: track every known project-card deck exit through temporary
  areas, hand, play, event pile, or terminal disappearance.
- Model `InitialResearchPhase` so starting project-card rejections happen there rather than being
  recorded beside corporation play.
- Decide whether the gameplay payment helper should accept arbitrary resources instead of its
  fixed standard-resource parameter list.
- **High priority:** Finish eliminating the Definition/Class split: make Pets Classes the sole
  runtime authority for cards and maps, get away from JSON entirely for their definitions, then
  restrict remaining JSON-backed records to offline generation and category-specific metadata.
- Discard the uncommitted typed custom-metric/code-generation experiment; it was evaluated and
  considered an unsuccessful direction.
- Decide whether card-action helpers need a separate optional payment override, analogous to
  `playProject`, while preserving actions with no cost and actions whose cost is authored.
- Implement abstract class-property defaults, beginning with `CardFront.cost = Number DEFAULT 0`;
  let the project-card families clear that default while retaining the `Number` bound so every
  project card must still state its cost explicitly, including zero.
- Try moving the automatic `This:: EventTag<This>` gain from every generated event card onto
  `EventCard`; ensure class-backed tag inspection sees the inherited authored behavior.
- IndustrialComplex should top up to 2 production if in QuickStartVariant (add to BugsTest anytime).
- Continue English renderer architecture work only through the prioritized plan in
  [`LANGUAGE.md`](docs/agents/LANGUAGE.md#prioritized-architecture-work). Classify effects only when
  one small model can replace the entry-point matcher chain and delete recurring recognizers; do not
  force the cross-family payment protocol into that model. First give conditions and counts
  role-bearing structure, using frames only for verified exclusive alternatives; replace assembled
  condition and noun strings at the same time. Then decompose event realization and audit surviving
  effect and card-operation recognizers individually. Do not add concepts for an isolated card.
- Try making `VictoryPoint` depend on the scoring `Component` for the fun of it.
- See if two wild tags on one card is feasible to support, else add to limitations doc.
- Replace the hard-coded First/Second/Third selector lists in Pets lowering and `TfmGameplay` with
  the concrete `WhichAction` universe, including support for a declared `Fourth` selector.
- Complete the master-Class-universe access interface: remove public `Catalog.classTable` after
  replacing the three deliberate structural acquisition points with narrower internal capabilities.
- Make an omitted concrete `CardResource` holder bound specialize to that resource class; today
  `CLASS Animal : CardResource` leaves the holder generic, so declarations must repeat
  `CardResource<ResourceHolder<Class<Animal>>>` to prevent animals from inhabiting other resource
  cards.
- **Medium priority:** Opportunistically replace Canon-backed engine characterizations with small
  generic declarations when the substitution is straightforward; keep core-rule scenarios at the
  player-action and observation interface.
- **Low priority:** Replace incidental Terraforming Mars vocabulary in generic Pets tests with small
  synthetic declarations where straightforward. Move a test only when its actual purpose is to verify
  Terraforming Mars behavior; domain words used as test data do not decide ownership by themselves.
- Decide whether generic script and Terraforming Mars script packages eventually warrant separate
  Gradle modules. Preserve their sibling domain package roots until that split has concrete value.
- **Low priority:** Extend premise viability beyond exact uninhabited-domain facts; Law Suit being
  unviable in solo because no opponent-dependent attack record can inhabit the projected Type
  universe is the canonical stronger proof.
- Replace the artificial persistent `PlayCards` invoice host in a later, dedicated lifecycle
  redesign. Distinguish the early card-play attempt from the later live payable card play; keep
  tag enumeration internal and let generic card behavior respond to the live operation without
  naming its `Class<CardFront>` data.
- In card and action behavior tests that can leave optional or delegated work behind, explicitly
  verify that the operation completes and the game returns to idle instead of checking only the
  resulting components.
- Restrict `TfmGameplay.stdAction` to actual `StandardAction` providers; give other live
  `HasActions` components a correctly named gameplay operation instead.
- **Medium-low priority:** Represent a payment as one auditable allocation: record every tender and
  every rule's full value, reject excess that is not smaller than every payment-unit value used,
  and only then consume the exact `Owed` amount. Confirm the precise excess-payment rule from an
  authoritative Jacob Fryxelius ruling before committing that legality test; do not derive
  attribution from automatic
  effect order. See [`PAYMENTS.md`](docs/agents/PAYMENTS.md) for the concerns and candidate designs.
- **Medium priority:** Settle and prototype the generic `EACH Type { ... }` fanout proposed in
  [`docs/agents/EACHPLAYER.md`](docs/agents/EACHPLAYER.md), keeping delegation and distributed
  completion separate.
- Replace the custom `ColoniesSetup` instruction with ordinary per-player setup signaling, then
  delete its custom declaration, registration, and Kotlin implementation.
- **Low priority:** Develop the class-property cardinality, abstract-default, RequirementGroup, and `Instruction*`
  directions recorded in [`docs/agents/PROPERTIES.md`](docs/agents/PROPERTIES.md).
- **Low priority:** Support requirement adjustment when one part of a compound card requirement is
  a global-parameter requirement.
- Prototype the bidirectional represented-family link in
  [`docs/agents/REAL_CARDS_MODE.md`](docs/agents/REAL_CARDS_MODE.md): `CardBack` carries its exact
  `Class<CardFront>`, while `CardFront` carries its `Class<CardBack>` family.
- Decide whether the administrative `Engine` Actor should instead be named `Npc` or `Admin`.
- **High priority:** Identify the signal Classes that workflows or APIs can create directly even
  though no selected Module activates them. Make their owning Modules activate them explicitly,
  then remove the `ClassLoader` rule that activates every reachable Trigger root.
- **High priority:** Allow a partial instruction to narrow the matching portion of the selected task
  while preserving the task's untouched structure
  ([#30](https://github.com/MartianZoo/solarnet/issues/30)).
- **High priority:** Make task queues semantically unordered: remove positional task selection and
  stable-order autoexec precedence, give the client-supplied `first` policy no ordering promise,
  require an id or unambiguous instruction match for explicit commands, and run tests under reverse
  and reproducibly randomized enumeration to expose hidden ordering dependencies. Autoexecution
  policy belongs outside the engine as specified in
  [`docs/agents/AUTOEXEC.md`](docs/agents/AUTOEXEC.md).
- **Medium-high priority:** Heavily revamp the `TfmGameplay` and test-helper APIs: move test-only
  actions such as `playCorp` and `playProject` out of production, remove or replace `SampleGames`,
  and give benchmarks explicit harness utilities rather than inheriting the test convenience
  surface.
- Improve `GameRecording` so viewer navigation does not mutate or seal its source World. Keep this
  as an in-process viewer model until a separately reviewed persistence design exists.
- Do not intern every structurally possible Type without a retention policy; families such as
  `Neighbor` can produce a very large domain. If repeated type algebra remains expensive, first
  measure whether a World-scoped or otherwise bounded cache can help without becoming state.
- **Medium priority:** Move more expansion-specific knowledge out of Kotlin and into Module/Pets
  data, starting with workflow phase insertion and Terraforming Mars registries that enumerate
  expansions directly.
- Replace negative `ClassSelection` exclusions for definition replacements with one positive
  systemic rule: selecting a replacement chooses that provider for the definition slot while the
  Catalog continues to retain every known variant.
- **Low priority:** Consider compiling Pets during the build into validated runtime artifacts, but
  only if one compiler can replace runtime parsing/validation without creating a second semantic
  model.
- **Low priority:** Investigate why the oxygen steps created by SoloOpponent's setup greeneries do not award it TR,
  and whether adding and then removing those steps has any other observable consequences.
- **Low priority:** Investigate why `GlobalParameter`'s `This: TerraformRating` silently produces no
  task for World Government Terraforming. Decide whether the authored rule should explicitly say
  `This BY Player: TerraformRating`, whether World Government should otherwise fail for its missing
  Player-bound destination, and whether AMAP is a coherent alternative. Preserve the settled AMAP
  distinction between an existing destination with zero invariant headroom and a missing
  dependency; see [`TURMOIL.md`](docs/agents/TURMOIL.md#multiplicity-and-amap).
- Complete `Game20260820Test` beyond its current partial generation-6 checkpoint using the preserved
  log, player data, and eight later screenshots; keep every new checkpoint independently sourced.

### Making Solarnet AI-player-ready

- **Medium-high priority:** Move beyond follow-along mode by modeling shuffled decks, deals, draws, and actual private hands. Include the bidirectional represented-family link from [`docs/agents/REAL_CARDS_MODE.md`](docs/agents/REAL_CARDS_MODE.md): `CardBack` carries its exact `Class<CardFront>`, while `CardFront` carries its `Class<CardBack>` family. Replace the temporary follow-mode neutralizer with real-mode lowering for the canonical `CARDS` operation.
- **Low priority:** Provide one strict player-relative observation and visible-history interface that cannot expose opponents' cards, hidden deck order, or private events; use that same interface for training, evaluation, and live play.
- **Low priority:** Provide a stable, machine-learning-friendly action interface that enumerates or scores complete legal choices while preserving the relationship among the engine's lower-level card, payment, target, quantity, and placement tasks.
- Support parallel or batched game simulation for AI search and training.
- Compare players across identical random seeds so evaluation does not confuse luck with strength.
- **Low priority:** Complete the rules/content needed by the chosen research configurations, especially unsupported unusual mechanics, Turmoil, and the remaining Prelude 2 cards; maintain explicit supported-content manifests so experiments cannot silently use incomplete games.

### Hypothetical Card Behavior

- Make `VictoryPoint` depend on the scoring `Component`, and define a scoring-completion phase
  if a future score depends on another score rather than directly on game state.
- Give multiple wild tags on one card distinct occurrences if a future card has two wild tags, so
  both can be assigned either the same tag or different tags for one action.
- Decompose a future card's `2 CityTile` instruction into two placement choices; consider making
  `Tile` atomized ([#64](https://github.com/MartianZoo/solarnet/issues/64)).
- Give players 20 TR in multiplayer setup and 14 TR in solo setup directly if a future card can
  observe the current 20-then-minus-6 solo sequence.

### Soon

- Preserve Merger's printed pay-after-play order when its real-card operation becomes executable;
  the follow-mode declaration still keeps the 42 M€ payment as an independent sibling to preserve
  current behavior.
- **Medium priority:** Continue the unresolved sequencing work identified by the canon effect-mode
  audit: settle the action-marker/Viron tension, event and Mandate context lifetime, scoring
  completion, same-trigger action-cost dependencies, and the remaining mixed phase triggers,
  including existing automatic effects triggered only by Engine workflow events. Before inventing
  an automatic `THEN`, distinguish inline continuation, frozen trigger-time choice, and
  descendant-completion semantics.
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
- In the Colonies solar phase, return every trade fleet before advancing every colony track, and
  prove that exact order.
- **Medium-high priority:** Consolidate exception cleanup
  ([#42](https://github.com/MartianZoo/solarnet/issues/42)): catch only expected script/domain
  failures, preserve defects and stack traces, use precise MartianZoo exceptions at domain
  layers, and narrow `Instruction.narrows`.
- **Low priority:** [#54: Owner-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54)
  — Resolve contextual ownership correctly and display the resolved player.
- **Medium priority:** Separate Catalog data from premise resolution, and split `TfmCatalog`'s
  generic declaration aggregation/validation into `Catalog` from the Terraforming Mars registries
  in `TfmCatalog`.
- Install and configure Kotlin ABI/binary API validation for public `pets`, `engine`, `tfm-canon`, and `script` APIs.
- Profile and reduce type-system allocation in `Type.glb`, `narrows`, and repeated dependency/refinement construction without risking correctness.

### Medium Priority

- **Medium-high priority:**
  [#60: Auto-narrowing](https://github.com/MartianZoo/solarnet/issues/60) — Define small,
  independently selectable autoexecution policies that can prove and submit forced task narrowings
  without making raw resolution search through player choices.

### Low Priority

- Model `StateChange` as a sealed gain/remove/transmute algebra so invalid nullable combinations are unrepresentable.
- [#59: `-This` Quantifier](https://github.com/MartianZoo/solarnet/issues/59) — Decide whether self-removal should default to mandatory.
- [#41: `list`](https://github.com/MartianZoo/solarnet/issues/41) — Improve hierarchy/dependency descent, grouping, depth, concrete subtypes, and explicit `<Anyone>` display.
- Explain or remove `Initializer`'s synthetic mandatory Quantifier.
- Split `Instructor.resolveChange` into narrowing, custom translation, and limit-checking stages.
- Move Pets AST generation to Kotest property tests only if domain-aware shrinking improves failures.

## Autonomous Follow-ups

- Reduce recorded-game viewer loading allocation, starting with repeated `DependencySet`
  iteration/lookups and abstract `ComponentGraph` count queries; validate changes with
  `SavedGameReplayBenchmark`.
- Serve copied Canon resources from the game-viewer Karma configuration; the resources reach the
  test package, but `:game-viewer:jsBrowserTest` currently gets a 404 for
  `canon/resource-index.txt`.
- Model Mars Nomads' moving non-tile marker, adjacency and reservation rules, and destination
  placement bonuses, then remove the replay's test-only stand-in and sourced reconciliations.
- Move Terraforming Mars payment/action lowering out of generic Pets; its remaining string-level
  implementation no longer creates a module dependency, but it is still domain ownership in the
  wrong layer.
- Contract the temporary public engine and TFM-engine test interfaces after Canon-dependent lower-layer
  tests use self-contained declarations and return to their owning modules.
- Contract the temporary public Pets-to-Canon construction and lowering stages around the eventual
  generic `Catalog` interface.
- Contract the temporary public `EventLog.entryAt` method after `tfm-engine` receives a narrow
  event query for identifying the origin of an action-phase second-action offer.

- Teach the English renderer to consolidate repeated identical optional card transfers, so Astra
  Mechanica's two independent choices render as “return up to 2” rather than two sentences.
- Restrict the remaining structured map records to offline generation and category-specific
  metadata.

- Replace the remaining colony-specific premise plumbing (`COLONY_TILES` and initial tile discovery)
  only when one general configured-starting-component model can preserve both selected starting
  tiles and unselected tiles available for mid-game addition.

- Make `FollowModeNeutralizer` consume the shared `CardOperation` semantic view; it currently keeps
  a separate recognizer because it also handles transformed card metrics and requirements.
- Make copied card effects use their executable follow-mode form; copying Head Start currently
  replays its authored card-area operation and tries to resolve inactive `Hand`.
- If real content ever references a helper belonging only to the other map in a two-map Bundle,
  split that Bundle at the selection point instead of adding per-Class availability metadata.
- Bring the JVM English renderer under Detekt through the planned intermediate-representation
  decomposition; avoid mechanical helper extraction or blanket suppression of the 47 legacy
  complexity findings.
- Resolve contextual placement-site `This` in the English renderer through its type-variable source,
  then delete its remaining positional recognition.
- Remove the context-free concrete-Type enumeration family from `Type`, `Class`, `Dependency`, and
  `DependencySet`; route structural and game-filtered enumeration through an explicit `ClassTable`
  so there is one implementation and one source of domain context.
- Preserve and enforce the existing `GameReader` constraint that prevents game mechanics, including
  custom Classes, from reading `EventLog`; add an architectural check so event history remains
  diagnostic and gameplay-state equivalence can depend only on the `ComponentGraph` and
  gameplay-relevant `TaskQueues`.
- Filter inactive gated provenance from `Initializer` source ordering so false mutual gains cannot
  create a bootstrap cycle absent from the selected configuration.
- Retain projection-decision provenance so premise diagnostics can explain automatic filtering and
  complete hard-reference paths, rather than only the selected content Class or immediate source.
- Decide how source-backed physical-game turn-order violations or transcript gaps should be
  represented without reordering or inventing actions; Game20260818 currently uses a standalone
  reconciliation for a patent sale taken beyond the normal action allowance.
- Represent the printed region for immediate instruction groups explicitly enough to distinguish
  Stratospheric Birds (removal above the artwork beside its action) from cards such as Potatoes
  (the whole immediate group below) before expanding English card-resource removal derivation.
- Investigate the intermittent Kotlin/Karma reporter crash during the unfiltered engine browser
  suite: targeted browser suites and the normal smoke test pass, but the reporter can lose a
  successful spec's console result and terminate the full run.
- Derive ocean-reserved greenery placements only after the language model can express the waiver of
  a placed component's normal restrictions as a relationship between that component and its site;
  do not attach a generic "disregard restrictions" phrase to `WaterArea`.
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
- Separate `Instructor`'s resolution-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `InstructionResolutionTest`.
- Replace `World.onAtomicComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.
