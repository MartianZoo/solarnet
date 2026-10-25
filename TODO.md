# TODO

Delete entries when they are done. Static-language and English work lives in [Pets TODO](pets/TODO.md).

- Continue converting replay tests that use Player autoexecution or `TfmGameplay` actions to
  explicit Player `doTask`/`doTasks` submissions. Keep source-backed state checkpoints and
  expectations for effects that are not evident from those submissions. The archived
  `HellasPromoWithoutPreludeTest` is a verified small example; `OtbGame20260828Test` has a
  deliberate TaskForm interaction style and should be assessed separately.

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
- actually plan out how exMachina should work
- Replace ordering-only `::` protocols in pricing/acceptance, wild-tag preparation, and
  Turmoil/Politician influence measurement. Decompose government formation before judging its
  individual invariant repairs; `THEN` on a signal does not wait for its triggered work.
- Remove bootstrap/client-shape reasons for `::`: default offer counts before signed adjustments,
  inferred construction order, and bundled card-search/reward/Audit work. Preserve meaningful
  correction relationships.

- Revisit ownership of `Selecting`, `Hand`, and played events together, including per-player
  cleanup of shared `Selecting` while other players' starting offers remain there.
- Fully migrate task/instruction preparation terminology across Pets and Solarnet APIs,
  implementation names, diagnostics, tests, specifications, glossary, and agent documentation.
  Use the abandoned `codex/pets-open-closed-lexicon` branch (terminology commit `3c89d711c`)
  as a scope reference, adapting it to this agreement rather than restoring it wholesale.
  **Open** means narrowing or evaluation remains before the immediate instruction is fully
  determined; **closed** means neither remains. Narrowing supplies a permitted choice;
  evaluation interprets against the current World (gates, metrics, amounts, forced deductions)
  without executing, and can leave the task open. Multiple narrowings and evaluations may
  interleave before closure. **Execution** performs the instruction and its consequences;
  closed does not mean executed or guaranteed to succeed. Use this preparation terminology
  consistently, including in ENGINE_CONTRACT.md. Preserve **abstract/concrete for classes**, including their declarations and classification APIs, for now. Both evaluation
  results and the validity of narrowings involving refinements can depend on the World;
  distinguish a fully specified instruction becoming invalid from an unresolved choice.
  Cover AMAP target selection leaving the task open until its amount is evaluated, and
  evaluation followed by further narrowing. This TODO records the migration, not its completion.
- Investigate the selected destination in [ENGINE_CONTRACT.md](docs/agents/ENGINE_CONTRACT.md)
  using the [transition plan](docs/agents/ENGINE_TRANSITION_PLAN.md).
  Complete transaction-success analysis and Admin uniqueness remain unfinished. Admin retains the
  existing retries after progress and throws a retained rejection after a completely unsuccessful
  pass. Existing gate and Player-task waiting exceptions still need model-level review; do not
  generalize them into suppressing all stalled-pass failures. Handoff does not trial-execute the
  receiving Actor's task. Establish the intended model for Turmoil event draws and solo neutral
  placements when implementing complete enumeration. Keep whole-action exploration in agents,
  separate from the legality of an individual transaction.
- Migrate Players away from the EAGER default, which can close legitimate gameplay choices.
  The user deliberately deferred the migration cleanup for now, hoping to move to a suitable smart
  policy rather than first migrating to NONE or CONCRETE. Leave the current default unchanged in
  this pass. Use explicit Player choices when testing engine semantics independently of policy.
- Resolve order-dependent Advanced Alloys discount attribution: the randomized-effect diagnostic
  reproduced the known `ThermalMatterWaveTest` (-96 versus -92) and `Game20230521Test`
  (-33 versus -31) reporting mismatches. Game-state assertions before those checks passed.
  See [TESTING.md](docs/agents/TESTING.md#standard-verification); this reporting issue is separate
  from transaction checkpoints and remains deferred.
- Investigate a diagnostic test mode that varies Admin task execution order, alongside the existing
  randomized independent `::` listeners. Use it to expose accidental ordering dependencies; retain
  FIFO in ordinary execution. See [Admin handling](docs/agents/ENGINE_CONTRACT.md#admins-choices-and-retries).
- Refine and verify the strategy catalogue in [DECISION_GRAPH.md](docs/agents/DECISION_GRAPH.md).
  Settle action-completion recognition and task comparison ignoring ordinals; inventory which
  existing forced deductions belong in Player and Admin agents. Measure pruning and safe eager
  execution on Canon scenarios such as Project Eden, retaining UNKNOWN when analysis runs out
  of information or work. The document selects semantic rules, not an implementation.
- Evolve `TaskForm` and the other agent choice surfaces to the ordered decision kinds
  **TASK, OR, TYPE, NUMBER, CARD**. TASK chooses pending work; OR chooses an alternative;
  TYPE narrows classes and dependencies; NUMBER supplies `?` amounts or `X`; CARD supplies
  card identities. Current form kinds are ALTERNATIVE, TARGET, and AMOUNT; task selection
  and card-movement naming are separate. Preserve class-before-dependency ordering within TYPE,
  including dependencies revealed by a narrower class. Distinguish guided prompts from
  unrestricted draft narrowing. This records the requested evolution, not an implementation.
- Investigate moving the code that runs several engine calls, checks supplied card identities,
  reports completion to application code, and decides whether an action has finished out of the
  engine package. Preserve transaction validity, rollback, and complete returned results. The
  [transition plan](docs/agents/ENGINE_TRANSITION_PLAN.md#experiment-7--the-surrounding-agent-call)
  explains each responsibility and the separate recovery questions.
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

Follow [ENGINE_TRANSITION_PLAN.md](docs/agents/ENGINE_TRANSITION_PLAN.md) for the ordered branch
experiments, acceptance cases, and cost reports. [ENGINE_CONTRACT.md](docs/agents/ENGINE_CONTRACT.md)
owns the selected destination: simple permitting-task matching; unique successful narrowing within
the requested transaction; no subsequent Admin execution in that proof; no execution implied by
selection. This is intended behavior, not a completed migration.

The plan covers combined totals, extra queued execution, explicit THEN bindings, partial narrowing,
transaction-level inference, Die/Ok simplification, immediate-effect timing, the surrounding Agent
call, and generalized agent narrowing. Earlier instructions to move all inference into agents or
require task IDs for most mutations are superseded. Record evidence from rejected experiments as
well as retained changes. Whole-action exploration remains agent work.
