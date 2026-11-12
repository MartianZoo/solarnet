# TODO

Delete entries when they are done. Static-language and English work lives in [Pets TODO](pets/TODO.md).

## Gameplay consistency

The [project record](docs/agents/TESTING.md#gameplay-consistency-project) owns the user's decisions,
examples, verified findings, and questions. These tasks implement or investigate that direction;
the remaining audit items listed there are **not yet discussed or approved**.

- Remove non-Admin `EAGER` and as much `SOLE_TASK` use as possible in gameplay tests. Prefer visible
  noteworthy events over shorter test-writing. Preserve the scoped corporation-setup convenience;
  the scope of automatic starting-card setup still needs clarification.
- Prioritize replacing task collection/ID selection, then sole-task assumptions; explain retained
  uses.
- Inventory replay corrections and task drops, then apply the recorded correction/comment policy.
  Keep retcons to `//` comments identifying correction pairs that could safely be rewritten later;
  retain their execution and chronology for now.
- Audit replay choices and assertions against original evidence, including expectations copied
  from current engine output.
- Investigate what each "start a game" entry point has already done before consolidating setup.
- Investigate consistent completion checks for focused scenarios, full games, and partial games.
- Inventory choices computed from state (starting player, free water area, affordable purchases)
  versus literal recorded choices; decide which adaptations are useful.
- Adopt combined-total task submissions more thoroughly, preserving choices and required ordering.
- Improve consistency in checking meaningful consequences without repeating submitted tasks.
- Reduce detailed cause-accounting assertions in ordinary replays after selecting representative
  inputs for focused mechanism tests; preserve meaningful score/resource outcomes.

### Changes to review with the user

Keep this list during the autonomous cleanup. Record behavior or readability tradeoffs even when
verification and Opus/high review support committing them. Remove entries after human review.

- `PristarTest`, `TerraLabsTest`, `PreludeRulesTest`, and UNMI's nested gameplay suite now use
  `NONE`, naming Prelude benefits, Merger corporation setup, payments, production, and research.
  Existing outcome/phase assertions remain. Pristar combines its 19 MC income and 6 MC bonus
  into `25 MC`, with an explanatory comment. A separate `6 MC` would also match the fixture's
  ordinary 6 MC production task (before its -5 MC offset), so it would not reliably identify the
  bonus. Both gains belong to the same production phase. Review the extra setup detail and
  repetition; no new gameplay helper was introduced to hide these events. Some retained `.expect`
  assertions now repeat explicit inputs; review those in the planned assertion-consistency pass.
- The user-requested `EACH` shorthand identifies a unique applicable top-level `EACH` task through
  existing Agent input handling. It removes grouped-effect ID lookups without copying formulas;
  child choices remain explicit. Review whether the short label plus nearby comments is clear
  enough. It rejects multiple applicable containers, even identical ones, which is stricter than
  ordinary instruction matching's interchangeability rule. Selection or an explicit task/context
  filter can identify the intended container. No Pets grammar or engine rule changed.
- October 9's Miranda trade now submits its printed `3* Animal` reward and asserts zero animals
  gained, replacing a task drop. With no legal holder, normal execution produces nothing. Review
  this deliberate AMAP spelling: `0 Animal` cannot identify that pending task, while the printed
  maximum can. All sourced checkpoints and chronology remain intact.

- `MassiveGammaZoneTest`: replaces three dropped attack consequences with explicit reductions
  against `SoloOpponent`. This adds neutral-player events absent from the source's human log while
  preserving its recorded human position. Low risk; check that this representation reads clearly.

- `doTasks`: now retains successful earlier arguments when a later argument throws; an empty
  sequence no longer triggers automatic execution. Explicit operation blocks still restore their
  whole attempt when failure escapes. `StandardActionTest` and `SyntheticProtonFragmentTest` now
  make that restoration explicit around failed exploratory actions, retaining their assertions.
  Review whether those scopes read clearly. Operation-scope submissions also now restore their
  automatic follow-up work if it fails and the exception is caught inside the operation.
- Removed `selectOnlyTask(x = ...)` and redundant X selections in four replays and five card
  suites. Submitted resource amounts now supply X directly. FakeHelion's existing failure
  characterization uses the named `selectTask("-6 MC")`; no card behavior changed. Low risk;
  review the remaining distinction between selecting a named cost and executing it.
- `SupercapacitorsTest` now uses `NONE` and lists its card payment, production, energy-to-heat
  conversion, and Research draws. `ActionScopeTest` likewise names production and draws instead
  of using `EAGER`. Resource/phase assertions remain. The invalid two-energy request now expects
  `TaskException` instead of `NarrowingException`: with `NONE`, no earlier automatic selection
  has committed to the remaining choice. The request must still fail, and the following valid
  request must preserve the original energy/heat totals. Low risk; review this exception change
  and whether the literal production lines make the setup clear enough despite the added length.
- Non-`NONE` timing: `doTasks` now follows `doTask` literally, so it no longer runs Player automatic
  work before its first argument. A qualified `agent.doTasks` inside an operation follows nested
  `agent.doTask` timing; use the scope's own calls for its automatic work between arguments. These
  differences disappear when Player execution is explicit, but deserve review.
- Replay recording now represents `doTasks` arguments as ordinary `DO_TASK` inputs. The redundant
  `DO_TASKS` kind is removed; older derived recordings containing it must be regenerated. Review
  any saved generated files still needed outside the test suite. Known shared `_local/replays/`
  copies are `Game20261102/solarnet-replay.short.json`, `Game20261031/RemoteVacuumFlow.short.json`,
  `Game20261031/p9a4445506f19/LatentGammaFieldTest.short.json`, and
  `Game20261030/KineticCarbonState.short.json`; leave those shared copies for separate regeneration.

## Other work

- New Holland's combined city/ocean tile remains unsupported. `RemoteVacuumFlowTest`
  uses an identity-only declaration for drawing/discarding; `LatentGammaFieldTest` uses
  the user-authorized `FakeNewHolland`, which places no city, plus explicit corrections.
  Both exported games need their test catalogs. Preserve these limitations when reusing
  either replay; neither demonstrates canonical New Holland support. Source and correction
  inventory: `_local/replays/Game20261031/p9a4445506f19/README.md`.

- Decide whether to expose a final score for lost solo games. `KineticCarbonStateTest` preserves
  the archived loss at 60 TR and final resources; the website displays 90 VP, but Solarnet
  intentionally skips its scoring phase on a solo loss. Source: `_local/replays/Game20261030/`.
  Do not inject a scoring phase into the replay to manufacture the website's displayed total.

- Investigate task matching when an `EACH` request is checked against an unrelated `THEN`
  task. In `SyntheticMagnetBurstTest` generation 8, Sponsored Academies' opponent draw throws
  `PetSyntaxException: EACH cannot be followed by THEN` while Mars University's optional
  discard/draw is pending. Passing `SponsoredAcademies` as the context succeeds; the replay
  keeps that explicit context and its exported game reconstructs successfully.
  `OtbGame20260818Test` generation 10 reproduces the same failure for Robotic Workforce's
  wild-tag election while Mars University's discard/draw is pending; it also names the context.

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
  separate Agent convenience, selected for production income; they do not authorize automatic
  choice-making.

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
- Give beginner and standard players the same ten-card `Selecting` offer; beginners keep it free,
  while standard players discard and buy their choices.
- Clarify Astra Mechanica's zero/one/two eligible-event behavior and reconcile its modeled return
  with printed and derived English.
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
  Keep instructions such as "always take titanium for Philares" deferred; no policy API,
  implementation, or default migration is selected by this documentation work.
- Reconcile the global Player default and remaining clients with the
  [gameplay consistency direction](docs/agents/TESTING.md#agreed-direction-execution-and-task-selection).
  The 2026-11-12 direction makes non-Admin `EAGER` cleanup active work, superseding the earlier
  blanket deferral. A replacement global default or new smart policy remains unselected.
- Resolve order-dependent Advanced Alloys discount attribution: the randomized-effect diagnostic
  reproduced the known `ThermalMatterWaveTest` (-96 versus -92) and `Game20230521Test`
  (-33 versus -31) reporting mismatches. Game-state assertions before those checks passed.
  See [TESTING.md](docs/agents/TESTING.md#standard-verification); this reporting issue is separate
  from transaction checkpoints and remains deferred.
- Investigate a diagnostic test mode that varies Admin task execution order, alongside the existing
  randomized independent `::` listeners. Use it to expose accidental ordering dependencies; retain
  FIFO in ordinary execution. See [Admin handling](docs/agents/ENGINE_CONTRACT.md#admins-choices-and-retries).
- Refine and verify the strategy catalogue in [DECISION_GRAPH.md](docs/agents/DECISION_GRAPH.md).
  Settle action-clearance recognition and task comparison ignoring ordinals. Use the two-identical-
  `Foo?`-tasks case to specify when different task causes can be ignored; card tracking currently
  inspects some causes. Decide what, if anything, must be preserved at intra-action handoffs. Inventory which
  existing forced deductions belong in Player and Admin agents. Measure pruning and safe eager
  execution on Canon scenarios such as Project Eden, retaining UNKNOWN when analysis runs out
  of information or work. The document selects semantic rules, not an implementation.
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

[ENGINE_CONTRACT.md](docs/agents/ENGINE_CONTRACT.md) describes current behavior and keeps potential
further work in its appendix. [Implementation evidence](docs/agents/ENGINE_TRANSITION_RESULTS.md)
preserves the retired transition plan's useful decisions and experiments. Whole-action planning,
general Player exploration, installed-policy design, operation lifecycle redesign, and
correction-time recalculation remain separate work.

- Compare future generated map PNGs with the published maps for coastline shapes that the current shared-edge and opposing-water-space counts miss; add a measurable check if repeated odd shapes appear.
