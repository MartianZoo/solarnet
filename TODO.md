# TODO

Delete entries when they are done. Static-language and English work lives in [Pets TODO](pets/TODO.md).

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
  `FakeHelionTest` preserves eight unsupported intended scenarios beside passing `BUG -` tests:
  five self-payment cases, Stormcraft floaters, and two Mons Insurance payouts. Revisit them with
  real Helion rather than treating the stand-in's observed behavior as a ruling.
  Source: `/Users/dev/martianzoo/player-resources/_local/unanswered-rules-questions-2026-10-14.md`.
  Links: https://boardgamegeek.com/thread/1705209/article/37732543#37732543 and
  https://boardgamegeek.com/thread/3752654/article/48054640#48054640.

- Derive `TfmActionLowerer`'s standard-resource cost classification from Pets declarations
  instead of its hardcoded resource list, without adding a loading stage.
- Explore making each `StandardProject` a signal emitted only after its cost is paid.
  `UseStandardProject` would select the class and open the bill; the resulting project signal
  would trigger its tile/production effect and CrediCor/Standard Technology rebates. This
  would replace the persistent `HasActions` standard-project components and their `UseAction`
  path. Account for Established Methods' direct `UseAction` grants and other standard-project
  payment listeners when evaluating the replacement.
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
- Review `pets/docs/class-existence-scenarios-draft.md` and consolidate repeated cases in
  `ClassDefinitionBoundaryTest` and `ClassTableSelectionTest`.
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
- Review the remaining proposals in [ENGINE_CONTRACT.md](docs/agents/ENGINE_CONTRACT.md).
  Align public engine mutations with the selected failure-atomic transaction guarantee, retaining
  client-controlled rollback for larger scopes. Complete transaction checks for legal narrowings,
  then align the Player/Admin priority gate, Admin FIFO retries and stalled-pass exception,
  mandatory eager Admin handling, and deferred promotion after completed transactions.
- Decide whether Admin's full automation is an engine invariant or a driver property, before
  implementing mandatory Admin handling. A trial run measured the cost: see the obstacle recorded
  under step 4 of [ORDINAL_REWORK.md](docs/agents/ORDINAL_REWORK.md).
- Continue [ORDINAL_REWORK.md](docs/agents/ORDINAL_REWORK.md) at step 4: the P0/P1/P2 gate,
  mandatory Admin handling with FIFO retries and the stalled-pass exception, deferred promotion at
  the end of the transaction that makes it eligible, and eager Admin during replay. Step 5 then
  switches `Checkpoint` to a transaction position and removes `recordedPositions`. Steps 1 to 3
  have landed; determining Admin's complete legal candidate set is still open and deliberately
  outside that sequence.
- Investigate a diagnostic test mode that varies Admin task execution order, alongside the existing
  randomized independent `::` listeners. Use it to expose accidental ordering dependencies; retain
  FIFO in ordinary execution. See [ORDINAL_REWORK.md](docs/agents/ORDINAL_REWORK.md).
- Evolve `TaskForm` and the other agent choice surfaces to the ordered decision kinds
  **TASK, OR, TYPE, NUMBER, CARD**. TASK chooses pending work; OR chooses an alternative;
  TYPE narrows classes and dependencies; NUMBER supplies `?` amounts or `X`; CARD supplies
  card identities. Current form kinds are ALTERNATIVE, TARGET, and AMOUNT; task selection
  and card-movement naming are separate. Preserve class-before-dependency ordering within TYPE,
  including dependencies revealed by a narrower class. Distinguish guided prompts from
  unrestricted draft narrowing. This records the requested evolution, not an implementation.
- Investigate whether engine-level partial narrowing is needed at all, versus retaining unfinished
  choices solely in client-held forms. Examine a choice between System and non-System instructions,
  reassignment after narrowing, and admission rewrites; identify any concrete case that needs an
  intermediate World mutation. This is an investigation, not approval to remove the API.
- Returned `TaskResult` values now cover a call's whole retained interval, including completion
  callback changes. Coverage of Admin handling through quiescence still waits on mandatory Admin
  handling: `addTasks` and `dropTask` deliberately skip task handling today.
- Review whether the block-based transaction coordinator, application guards and completion
  callbacks belong in the engine API, along with unnecessary history controls, copying and
  speculative APIs.
  Preserve safe client-controlled rollback when reviewing those controls.
  Review grouped/scalar task conveniences before retaining them. Code comments mark the current
  differences; the cleanup deliberately leaves their behavior unchanged.
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
