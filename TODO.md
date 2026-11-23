# TODO

Delete entries when they are done. Static-language and English work lives in [Pets TODO](pets/TODO.md).

- Convert Distant Isotope Link and Stable Silicon Point into source-backed replay tests,
  including export/import verification. Prepared sources: `_local/replays/Game20261112/`
  and `_local/replays/Game20261112/pab280e66a6e5/`, respectively; read each archive README.
  Stable Silicon Point is missing intermediate save 232, but its complete log and final
  state are preserved. Neither conversion has started.

## Game20261111 replay follow-ups

- Support identifying a payment transmutation by its user-facing resource removal where unique.
  In Game20261111's starting research, `-21 MC` currently reports no matching task for
  `21 Pay<> FROM MC`. Settle the omission criterion (`Iconic`, `System`, or another rule) and
  ambiguity handling as part of the task-matching work below; retain explicit `Pay` meanwhile.
- Revisit the already-queued MC remainder when titanium or steel clears `Owed`. In
  Game20261111's generation-2 Asteroid Rights play, removing the following zero-MC payment leaves
  `PaymentScope` blocking the card's `MAX 0 Barrier` requirement. The remainder was admitted
  while money was owed and later evaluates to zero; keep the explicit `0 Pay<> FROM MC` until
  the payment model can finish that scope without an extra player submission.

## Build and test speed

- Prune the `:engine` tests. Keep distinct engine contract checks and remove cases that repeat
  coverage already provided by other tests.
- Investigate why the `:tools:test` Gradle task was open for about two minutes while its three
  test cases took about one second combined. Measure worker wait separately from test execution.
- Investigate why the `:pets:pets:jvmTest` Gradle task was open for about two minutes while its
  JVM test cases took about ten seconds combined. Measure worker wait and test execution separately.
- Substantially reduce routine testing for `:pets:search`. Review how much distinct coverage its
  JVM, JS, and Wasm runs provide and retain focused coverage for differences that matter.

## Rules test cleanup

- Explicitly deferred on November 19: review the configuration-contract cases in
  `ClassTableSelectionTest`. Keep checks that establish supported configuration choices and
  rejection of incompatible selections; remove duplicated class inventories and incidental
  implementation checks.

## Human-readable game archive prototype

All 34 remaining replay scenarios have commented `.game` fixtures and pass checked-in import and
fresh export/re-import. See [archive verification](docs/agents/EXPORT.md#migration-verification).

- Pursue the [arithmetic inventory direction](docs/agents/EXPORT.md#arithmetic-inventory-direction):
  move all six ruling bonuses to explicit player tasks, preserving Reds' eligibility before chairman
  TR; also audit annual TR loss, chairman TR, immediate delegate changes, and constructed card tags.
  Select the inventory criterion before claiming complete coverage. Ruling-bonus model changes
  remain deferred.
- Treat omitted actor qualifications and typed-zero matching as shared task-matcher concerns,
  never archive-only conveniences. Typed zeros in the current draft already work unchanged.
- Add a direct archive round-trip for Self-Replicating Robots' staged card source. Tracked games,
  plays from Selecting, and card-held payments now have replay coverage. REPL work is deferred.
- Resolve how an archive should state the actual zero outcome of `4* Microbe`: changing its limit
  to `0 Microbe` currently fails shared task matching. Do not hide this with an importer-only rule.
- Leave AUTO unimplemented until it can prove identical outcomes across all allowed execution
  orders. Current archives list player effects explicitly.
- Request-directed EACH/group expansion and OR inference now share a matcher with exporter
  omission checks. Keep explicit preparation for chains not yet proved, intervening state changes,
  newly exposed Admin work, and totals spread across hidden children. Extend these only when a
  concrete Canon/archive example justifies it and the exporter can use the same proof.

## Gameplay consistency

The [project record](docs/agents/TESTING.md#gameplay-consistency-project) owns the user's decisions,
examples, verified findings, and questions. The November 19 batch implements and verifies the user's
additional gameplay, assertion, and notation decisions;
the remaining audit items listed there are **not yet discussed or approved**.

The selected migration now uses explicit player execution in all ordinary gameplay tests, keeps
the scoped standard-opening and TaskForm exceptions, replaces artificial preparation histories,
and completes focused success paths. The owning testing document records the retained differences
and replay correction inventory. The following are separate future work:

- Investigate selection and narrowing that leave execution explicitly pending, including concrete
  System choices such as UseAction. Today selecting an alternative can transfer it to Admin and
  execute it before returning. The user selected separate engine work; this replay cleanup must
  not change the engine/Agent or TaskForm contract.

## Replay authentication and future coverage

These are separate from completing the test-style migration. A working replay proves a game the
current model can execute; authenticating it against the actual historical game can still expose
a Solarnet defect. Preserve that uncertainty without treating it as a migration blocker.

- Audit replay choices and assertions against original evidence, including expectations copied
  from current engine output and unresolved historical score discrepancies.
- Add source-backed ASCII maps to the remaining older replays when independent board evidence is
  recovered: `FirstPartialGameTest`, `Game20230521Test`, `Game20260619Test`,
  `HellasPromoGameTest`, `HellasPromoWithoutPreludeTest`, `SoloGame20230611Test`,
  `SoloGame20230710Test`, `SoloGame20230721Test`, and `StinaGameTest`. No suitable original
  board dataset was located in this audit; generated recordings are not independent evidence.
  The August 9, August 18, and November 11 physical replays now check their middle and latest
  photographed boards. Their latest photographs precede later placements; establish endpoint
  maps from additional photographs or a separately verified continuation of the original
  transcripts. Keep those earlier checkpoints at their actual positions.
- Investigate illegal-action coverage using positions loaded from recorded games. Current
  counterfactual replay probes may be removed without replacement. A possible future direction is
  separate functional tests that load a position, attempt an illegal move, and verify rejection
  and restoration, while the replay itself remains an import/export file. No replacement design
  is selected.

## Other work

- New Holland's combined city/ocean tile remains unsupported. The two replays that relied on
  test-only versions of New Holland were removed from the suite. Source and correction inventory:
  `_local/replays/Game20261031/p9a4445506f19/README.md`.

- Decide whether to expose a final score for lost solo games. `KineticCarbonStateTest` preserves
  the archived loss at 60 TR and final resources; the website displays 90 VP, but Solarnet
  intentionally skips its scoring phase on a solo loss. Source: `_local/replays/Game20261030/`.
  Do not inject a scoring phase into the replay to manufacture the website's displayed total.

- Clarify `THEN` after an instruction whose amount resolution atomizes it into a group. The current
  engine releases the continuation when that group is admitted, while Pets forbids an authored
  group or `EACH` before `THEN`. No Canon consequence is established; keep this separate from
  selection timing.
- Extend transaction analysis only for an actual supported Canon Admin task that cannot complete
  without client choices. Player inference remains a limited convenience and may be reduced.
  Preserve the distinct incomplete-analysis result; unsupported hypothetical forms are not a
  general completeness backlog. Measure actual expensive cases before adding search limits.
- Review whether a NOW recipient needs a separate undo checkpoint before their choice. Current
  selection checkpoints restore to before the active Player's handoff, as earlier BY handoffs did.
  The focused NOW/SOON fix preserves that behavior; no separate per-recipient undo policy is selected.
- Complete Agent choice analysis before adopting automatic form reduction. `Repair?` now offers
  both `Ok` and its positive amount, and form submission can execute the immediate repair.
  Menus still list candidates without proving their complete transactions. `Repair*`
  still resolves to zero under the documented current-headroom rule in
  `QUANTIFIERS.md`, but its intended amount rule remains open. Canon's neutral `PartyDelegate`
  placement deliberately uses mandatory gain for this reason. Characterize affected Canon amounts
  before changing the rule. Reconcile form commit selecting/evaluating before applying a narrowing
  with direct execution applying the request first.
  A sole displayed option is not proof of a sole successful transaction. Do not broaden Player
  automatic choice-making until this is resolved; the selected reduction of automatic execution
  is tracked in the gameplay consistency project above. Explicit combined-total requests are a
  separate Agent convenience, selected for production income and other simultaneous rewards; they do
  not authorize automatic choice-making.

- Investigate the deferred nested-placement and partial-selection experiment in
  [Make types types again](docs/agents/MAKE_TYPES_TYPES_AGAIN.md) if that proposal is selected.

- Investigate the local configuration-cache invalidation caused by the injected
  `kotlin.project.persistent.dir` property. Repeated `help` commands reproduce it on the
  pre-split baseline as well as the composite build; build separation preserves the existing
  generated-state isolation rather than redesigning it.

- Verify IDE source navigation and editing across the separate Pets and Solarnet builds.
  The command-line check proves that an unpublished Pets edit reaches a Solarnet test;
  the interactive IDE acceptance check remains unverified.

- Simplify wild-tag composition for Robotic Workforce and Cyberia Systems. Their current Pets
  declarations repeat an optional assignment effect and nested assigned-wild selectors. Find a
  smaller model that still allows Research Network to qualify as Building, lets copied Gyropolis
  and Medical Lab count assigned tags, and keeps assignments for the whole action. Explore choosing
  the copy target before assigning wilds while keeping `CopyProductionBox` limited to extracting
  the authored production box.

- Finish faithful coverage of the 2026-10-14 unanswered-rules scenarios where current content or
  inputs cannot express the situation. `UnknownRulesTest` covers rejecting partial Celestic and
  Morning Star search counts (question 8), but not exhaustion after finding some cards: searches
  have no deck or discard supply. Question 16 (Helion retaining 2 MC while paying partial Mons
  Insurance with heat) needs real Helion; only `FakeHelion`, explicitly lacking correct payment
  allocation, exists. Do not treat that stand-in as evidence for the Canon interaction.
  `FakeHelionTest` preserves nine unsupported intended scenarios beside passing `BUG -` tests:
  five self-payment cases, Energy Market, Stormcraft floaters, and two Mons Insurance payouts. Revisit
  them with real Helion rather than treating the stand-in's observed behavior as a ruling.
  Source: `/Users/dev/martianzoo/player-resources/_local/unanswered-rules-questions-2026-10-14.md`.
  Links: https://boardgamegeek.com/thread/1705209/article/37732543#37732543 and
  https://boardgamegeek.com/thread/3752654/article/48054640#48054640.

- Give standard projects a meaningful paid event. Paying for the Greenery standard project
  should emit an event saying that project was bought; its placement effect, Standard Technology's
  rebate, and CrediCor's rebate should respond to that event instead of payment bookkeeping
  disappearing. Preserve Established Methods' granted uses. Treat this as the next substantial,
  reasonably contained payment improvement; it can work independently of the card-play work below.
- Make card play create its result when payment finishes instead of queuing it behind
  `MAX 0 Barrier`: choose the card, satisfy its requirements, pay, then create the play result.
  Retain the selected card and its source through those steps, including Self-Replicating Robots'
  stored cards. Preserve supported requirement timing and temporary/stored card sources without
  introducing a general-purpose operation framework. Investigate this harder case separately
  from standard projects.
- Simplify the stored-card face/location relationship. `PlayCardAction` now offers only Hand or
  StagedProject, but the latter still checks the face separately against its recorded class.
  Sharing a selected type across those arguments in a single offered instruction is not supported
  by the current local-variable rules. Do not add a preparation step or gameplay helper just to
  replace this check.
- Add Canon invariant tests for incompatibilities: self-heat removal outside PaymentScope, except
  Local Heat Trapping, must be declared incompatible with Stormcraft. Include abstract resource choices.
- Revisit Stormcraft plus Helion payment composition if that combination is supported.
  `Accepting` now skips alternative standard-resource offers when the payer has none; it does not
  consider possible substitutes such as Stormcraft floaters for heat.
- Verify as a Canon invariant which tasks can interleave with card play, including gaining MC
  production and other actual exceptions. Fake Head Start currently allows interleaving as a
  known approximation, not a requirement.
- Consider renaming automatic (`::`) effects to “immediate.”
- Replace the archived *Life of an Effect* walkthrough with a current account of lexical `Me`
  and the transformation pipeline.
- Decide whether Agent submissions must use concrete amounts instead of arithmetic over `Owed`.
- Replace the starting-project purchase listener on gaining a corporation with an explicit
  continuation after actual corporation play. `PlayCard THEN BuySelectedCards` currently bills
  before the corporation and its purchase modifiers enter play. The TerraLabs experiment and
  retained test are recorded in [card handling](pets/docs/agents/CARD_MODEL.md#external-offer-procedures).
- Investigate selecting tasks by their main class without narrowing them, including grouped
  instructions, so more sandbox `selectOnlyTask` calls can use meaningful names.
- Replace ordering-only `::` protocols in pricing/acceptance, wild-tag preparation, and
  Turmoil/Politician influence measurement. Decompose government formation before judging its
  individual invariant repairs; `THEN` on a signal does not wait for its triggered work.
- Remove bootstrap/client-shape reasons for `::`: default offer counts before signed adjustments,
  inferred construction order, and bundled card-search/reward/Audit work. Preserve meaningful
  correction relationships.

- Revisit ownership of `Selecting`, `Hand`, and played events together, including per-player
  cleanup of shared `Selecting` while other players' starting offers remain there.
- Decide whether callers need a public `isOpen` check for tasks or instructions. The current
  `needsNarrowing` check answers only whether an immediate elective part is unfixed; it does not
  inspect World-dependent evaluation. A task with `PER`, a gate, or an AMAP amount may therefore
  need evaluation after `needsNarrowing` becomes false. Define the evaluation state and the
  caller's use before adding a second predicate. Preserve abstract/concrete for Types.
- Extend transaction inference beyond the finite mandatory/optional changes, OR alternatives, and
  full-form transmutation pairs implemented by the [implementation evidence](docs/agents/ENGINE_TRANSITION_RESULTS.md).
  Preserve distinct incomplete-analysis results for unsupported groups, AMAP, unbound numeric X,
  and broader nested forms; no wall-clock cutoff. Broader Admin waiting-case classification and
  general Player completion search remain separate work. The approved matching, selection, Agent
  call, rollback, correction, premise-owned Turmoil/solo, and replay changes are implemented; do not
  repeat them as future work.
- Design engine-installed autoexec policies for selected tasks assigned to an Actor, configured
  by that Actor's agent. Use Arctic Algae collection without waiting for the receiving agent as
  the acceptance example. Resolve matching, lifetime, selection and priority, failure, transaction,
  replay, and speculation details in the
  [Agent contract](docs/agents/AGENT_CONTRACT.md#4-configure-autoexec-policies-in-the-engine).
  Keep instructions such as "always take titanium for Philares" deferred; no engine-installed
  policy API or implementation is selected.
- Continue removing Player `EAGER` from remaining clients and older engine test fixtures where
  explicit task submissions make their behavior clear. Players now default to `NONE`; Admin remains
  automatic. Preserve the scoped opening and TaskForm exceptions in the
  [gameplay consistency direction](docs/agents/TESTING.md#agreed-direction-execution-and-task-selection).
  A new smart policy remains unselected.
- Resolve order-dependent Advanced Alloys discount attribution: the randomized-effect diagnostic
  reproduced the known `ThermalMatterWaveTest` (-96 versus -92) and `Game20230521Test`
  (-33 versus -31) reporting mismatches. Game-state assertions before those checks passed.
  Those replay accounting checks were removed on 2026-11-14. See the
  [attribution accounting analysis](docs/agents/ATTRIBUTION.md#capped-effects-make-per-provider-totals-order-dependent);
  this reporting issue is separate from transaction checkpoints and remains deferred.
- Investigate a diagnostic test mode that varies Admin task execution order, alongside the existing
  randomized independent `::` listeners. Use it to expose accidental ordering dependencies; retain
  FIFO in ordinary execution. See [Admin handling](docs/agents/ENGINE_CONTRACT.md#admins-choices-and-retries).
- Refine and verify the strategy catalogue in [DECISION_GRAPH.md](docs/agents/DECISION_GRAPH.md).
  Settle action-clearance recognition and task comparison ignoring ordinals. Use the two-identical-
  `Foo?`-tasks case to specify when different task causes can be ignored; card tracking currently
  inspects some causes. Verify that comparisons at intra-action handoffs
  preserve the recipient's meaningful choices. Inventory which
  existing forced deductions belong in Player and Admin agents. Measure pruning and safe early
  task completion on Canon scenarios such as Project Eden. Investigate unexpected UNKNOWN results
  without pruning options that lack a viability proof. The document selects semantic rules, not an
  implementation.
- Design the planned interaction API as a Platonic UI for information, decisions, navigation, and
  undo. Specify shared interaction scripts with a direct driver and a UI driver, and research
  established test patterns before choosing interfaces. No UI or driver implementation is selected.
- Specify fuzzy task matching for client requests: settle which omitted details may still identify
  a pending task, how ambiguity is reported, and how this relates to explicit task IDs and
  context-class disambiguation. Preserve useful narrowing errors when candidate matching fails
  without rejecting valid gated requests. Review the current descriptive rule in
  [ENGINE_CONTRACT.md](docs/agents/ENGINE_CONTRACT.md#identifying-the-permitted-task) before changing behavior.
- Evolve `TaskForm` and the other agent choice surfaces to the ordered decision kinds
  **TASK, OR, TYPE, NUMBER, CARD**. TASK chooses pending work; OR chooses an alternative;
  TYPE narrows classes and dependencies; NUMBER supplies `?` amounts or `X`; CARD supplies
  card identities. Current form kinds are ALTERNATIVE, TARGET, and AMOUNT; task selection
  and card-movement naming are separate. Preserve class-before-dependency ordering within TYPE,
  including dependencies revealed by a narrower class. Distinguish guided prompts from
  unrestricted draft narrowing. This records the requested evolution, not an implementation.
- Reconsider the remaining engine callback coordinator and legacy Agent `*Operation` helpers
  only against actual callers. Session-owned checks, full-call results, and rollback
  are implemented; external application side effects are outside restoration. See the
  [contract appendix](docs/agents/ENGINE_CONTRACT.md#appendix-further-work-under-consideration).
- Clarify System classification versus non-Iconic presentation separately. Review when concrete
  System work and explicit BY Admin work change assignee.

- Briber's self-claim requirement still appears as `[Briber]`; keep it separate from the direct
  `Lose 12 M€` effect. The marked-variable references now follow recorded occurrences, but the older
  interaction-trigger Anyone-to-Player normalization still supplies an implicit `that player`.
- Investigate repeated ranking of the same field while testing each candidate. Scores must reflect
  the current state; avoid a second persistent representation of game scores.

- Verify remaining inferred Research counts, anonymous draws, action grouping, and earlier
  adopted card identities in `Wsbg2025Test`. Jon's generation-six two-card Research keep now
  reconciles his confirmed patent sale to afford Robinson and zero ending cash; the purchase
  count itself is still inferred. All four final scores match, and no unexplained exMachina
  remains. Charlie's recorded Miranda overpayment/refund is intentional and does not need
  solving. Evidence and remaining uncertainties are in `_local/replays/Game20250919/sources.md`
  and `derived/action-inventory.md`.

- Investigate exact AST preservation when a replay contains a Robotic Workforce
  wild-tag choice. Original and reconstructed task text matches, but parsing
  `Tag<@WildTagElection>?` marks the type reference's empty arguments as
  explicitly specified. `OtbGame20260818Test` now verifies serialized event
  text, notes, and file round trips; the internal AST marker still differs.

## Engine contract investigations

- Make mandatory Admin processing inseparable from Player task execution, rather than requiring
  callers such as `AgentImpl.combineAmounts` to remember `runAdmin()` between raw engine calls.
  Deferred at the user's request. Investigate engine task execution as the owner, including
  whether direct `ActorEngine` calls should also finish Admin work automatically. Preserve selected-Player priority,
  failure rollback, and the distinction between executing a task and probing its interpretation.
  The current loop also retries Agent application-input failures (`AdminInputTest`), so moving it
  requires an explicit treatment of that session-owned validation. Remove superseded scheduling
  calls rather than adding another optional wrapper. Verify the combined-TR case and the shared
  task-execution behavior once the ownership change is implemented.

[ENGINE_CONTRACT.md](docs/agents/ENGINE_CONTRACT.md) describes current behavior and keeps potential
further work in its appendix. [Implementation evidence](docs/agents/ENGINE_TRANSITION_RESULTS.md)
preserves the retired transition plan's useful decisions and experiments. Whole-action planning,
general Player exploration, installed-policy design, operation lifecycle redesign, and
correction-time recalculation remain separate work.

- Compare future generated map PNGs with the published maps for coastline shapes that the current shared-edge and opposing-water-space counts miss; add a measurable check if repeated odd shapes appear.
