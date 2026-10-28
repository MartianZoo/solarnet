# Engine API transition plan

> **Working plan — October 28, 2026.** This plans remaining work on
> `codex/engine-transition-experiments`, not new initial experiments. The owner must settle the
> marked decisions before an implementer treats a proposal as a requirement.
> [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns intended semantics;
> [ENGINE_TRANSITION_RESULTS.md](ENGINE_TRANSITION_RESULTS.md) records branch evidence;
> [TASK_HANDLING.md](TASK_HANDLING.md) inventories paths in the checkout being read. Verify the
> experimental branch's source and tests before editing its code.

Maintain this plan and the selected [contract](ENGINE_CONTRACT.md) in work1, then carry their
updates into the experiment branch. The branch's code may be ahead of work1; its observations
belong in [the results](ENGINE_TRANSITION_RESULTS.md) with the branch revision named. Do not let
a branch-only implementation silently change the selected design.

## Finish line

Deliver a small, coherent **engine API** for a client's requested change. The engine identifies a
permitting task without executing competing tasks, combines that task with supplied information,
and executes only when exactly a single concrete narrowing completes the requested transaction.
That transaction includes immediate consequences and task admission, but does not execute later
queued work to justify the request. A client-facing call then gives attached Actors' automatic
behavior a chance to run, beginning with Admin, and returns at quiescence. Lower-level transaction
methods may be internal. The public shape and precise handoff behavior still need approval.

When a useful feature is removed from engine, carry it into Agent only as needed for current
clients. General Player guidance, whole-action completion search, outcome equivalence, and a smart
Player autoexecution policy are separate projects described in
[DECISION_GRAPH.md](DECISION_GRAPH.md). The
[Agent contract](AGENT_CONTRACT.md#4-configure-autoexec-policies-in-the-engine) also selects future
standing policies installed in engine. That mechanism is not an acceptance condition here;
selection tests in this plan assume no applicable installed policy. `TfmGameplay` is
for deletion; its current helpers do not set the API design.

## Verified starting point

The branch checkpoint is merge commit `6c4686b61`, which incorporates `main` at `f1055a08b`;
work1 was fast-forwarded to that same main commit. The experiment branch has not been merged
back into main. Refresh these refs before work.
Transaction recording and checkpoints already existed in `main` before this branch split; do not
list or reimplement them as transition work. The following changes are **already committed on the
experimental branch**. Their limits remain work; the completed portions are not tasks.
The merged branch passed a full `./gradlew build`, including the curated replays and web browser
checks. This plan does not authorize any further merge.

| Already on branch | Relevant limit |
| --- | --- |
| Application input checks and operation completion moved to Agent sessions. | Completion callbacks and whole-call failure semantics are unsettled. |
| Engine combined resource totals removed; Agent translates an explicitly submitted total into separate calls. Existing engine matching can identify an individual task by its calculated amount. | Decide whether to retain calculated-amount matching in the final engine API and settle multiple matches. |
| Explicit submitted groups execute their admitted member IDs, leaving reactions and continuations queued. | Structural selection and handoff still need a final rule. |
| Selection/narrowing no longer execute component changes as the selecting Actor; numeric `THEN` bindings are explicit. Ordinary add/drop queue edits now refuse while a task is selected, except dropping that task itself. | Selection still expands exposed `EACH` tasks. The owner defines that expansion as execution, so this branch behavior must move to an execute command. Voluntary partial narrowing and other structural paths need review. |
| Finite mandatory gains/removals and full-form transmutations try complete candidate transactions in disposable Worlds; Admin and Player priority use this finite uniqueness rule. | Other forms and deterministic proof limits remain open. |
| Admin remains mandatory in Agent calls, retries after progress, and preserves retained rule failures. | Stalled passes, waiting cases, and public orchestration need settlement. |

Do not call the branch a completed contract implementation. Current engine methods expose more
low-level operations than the proposed public API may need, and the engine does not yet have a
selected final task matcher or a general proof of unique transaction success.

## Decisions to settle with the owner

These are implementation gates. Continue independent work when a gate is unresolved; do not
silently choose an answer.

1. **Task identity and interchangeability.** Confirm the owner's proposed rule: if a submitted
   task string intersects exactly a single pending task for the submitting Actor, use the
   intersection as the request. Specify whether “intersects” includes current-world resolution,
   a satisfied gate, a `THEN` first stage, or a currently failed gate matched with possible facts.
   Different continuations make tasks distinct. Define the positive rule for considering other
   task data interchangeable, especially different causes and otherwise identical repeated rewards.
   Decide whether to retain the existing resolved-value match in the engine: with 3 Foo present,
   `1 MC / Foo` evaluates to `3 MC`, so may a client submit `3 MC`? (The owner's `3 Foo` wording
   reverses the result.) Decide what happens when
   several tasks resolve to the same change.
2. **Public call shape.** Decide whether to reshape the existing public `ActorEngine.transact`
   callback coordinator or place the client-facing method in Agent or a thin coordinator. `:agent`
   depends on `:engine`: an Agent-level caller leaves an engine operation public, while an
   engine-level caller needs engine-defined callbacks. Define which `Agents` session supplies
   attached Actors when independent sessions share a World. The owner accepts the existing
   shared `AutoExecLoop`; splitting it is not required now. Admin must be retried before **every**
   automatic Player step when no task is selected. Admin must do everything it can before the
   Player sees the state. A waiting Admin task does not alone block Player selection once Admin
   can make no further progress; Player work may be needed to move the game forward. If the
   current game model requires an external input, surface that need before Player work. This
   does not commit Turmoil to its present deck workaround, which is being changed in work4.
   The selected P0 task must execute before any other work. Settle how Player work is
   ordered after Admin and whether `World.onTransactionComplete` remains a separate hook. No
   production code assigns that hook; removing it would simplify callback timing but delete the
   tested notification surface. Admin cannot be disabled by Player policy or replay configuration.
3. **Proof coverage and limits.** Decide how to expose incomplete uniqueness analysis without
   claiming impossibility or picking the first success. A wall-clock timeout is not approved:
   identical game requests must not change answers according to machine speed. Specify finite
   domains, deterministic work limits or another terminating proof strategy, and the observable
   result when no proof is possible. `Repair?` is a concrete counterexample: current raw execution
   takes zero although the explicit `Repair!` branch can succeed after an immediate repair.
   **Recommendation to validate:** fully examine finite domains derived from instruction shape,
   with no count cap until measurement shows a need. Return a distinct incomplete-analysis result
   for forms outside that domain, such as unbound numeric X; use no elapsed-time cutoff.
   Verify that the Admin driver really exhausts all currently possible work before a Player sees
   the state; a queue-empty check is not the rule. Current code can leave an Admin task pending and
   checks priority through a concrete selection trial. Characterize Canon waiting tasks and
   retained failures before changing that path; a synthetic gate test alone cannot justify it.
   The current abstract `GlobalEvent` draw is a deck-model workaround, not a reason to leave other
   runnable Admin work behind. Work4 is changing it; preserve the Turmoil replay tests when that
   change is integrated, without making the old draw request an engine API requirement.
4. **Selection, structural edits, and handoff.** Decide whether a voluntary partial narrowing is
   retained by engine or must live in a client draft, especially when a target choice unlocks AMAP
   evaluation. Decide which queue edits are permitted during a
   selection-only call: group expansion other than `EACH`, pure dispatch, `Ok` elimination, deferred promotion,
   and assignment to Admin. Selection does not execute the selecting Player's components. If it
   reassigns the selected P0 task to Admin, Admin executes that same task before any other work.
   A separately created Admin task waits until P0 is executed. Reconcile structural replacement
   of a selected task with this rule before treating it as valid completion. Current
   `dropTask(selectedTaskId)` is an external correction request, outside the in-game priority rule;
   audit it with correction APIs without making it an implementation gate for P0 scheduling.
   Move `EACH` expansion out of selection/narrowing: enqueuing all children and consuming the
   parent is execution of the selected `EACH` task. After that execution, children take their
   ordinary priorities. Use Canon base `SetupPhase: EACH Me@Player { PROD[...] }` to exercise the
   distinction; selection alone must not create its production child tasks.
   Specify whether the completed public call succeeds if that later Admin transaction fails.
5. **Whole-call results and recovery.** Decide what a client-facing call returns and restores when
   a completion callback or later automatic transaction fails. Engine transaction failure already
   restores its own game state. Current `WorldTransaction` restores the initiating work together
   with ordinary Admin follow-up on failure, but invokes `World.onTransactionComplete` after that
   restoring block; a failing callback can leave earlier work retained. External application side
   effects cannot be rolled back by the engine. Decide whether this hook survives before promising
   a uniform whole-call failure rule.
   Also decide whether correction methods may edit the queue while a task is selected; the branch
   now refuses ordinary add/drop edits but still allows some correction paths.

## Consequences to validate explicitly

These follow from the proposed API or current implementation but have not been individually
approved. Raise them with the owner instead of letting an implementer infer consent.

- A public call that runs to quiescence can run configured **Player EAGER** behavior after the
  client's request. Today's shared loop may select the first successful Player option in queue
  order. Preserving that current policy as carryover does not approve it as the final Player policy;
  determine which calls can expose it and what the client sees.
- If the lower-level engine operation becomes inaccessible to ordinary clients, sandbox tests,
  importers, and direct engine consumers may have to use a session with explicit automatic-policy
  settings. Audit real callers before removing a method; do not preserve obsolete APIs merely for
  hypothetical compatibility.
- A task identified by its **calculated** amount may no longer match after a World change. A log
  saying `3 MC` records the concrete request, but not necessarily which permission supplied it.
  Multiple matching permissions with different continuations cannot be resolved by a successful
  transaction trial.
- A later Admin failure may occur inside the same public call after the initiating Player
  transaction succeeded. Whole-call restoration would undo that successful game change; retaining
  it would return or throw with a partially completed call. A completion callback may already have
  changed external state. This decision affects what a client can safely display or record.
- A deterministic incomplete-analysis result means some valid abstract requests may need a more
  specific input. The chosen public result must let a client distinguish that situation from a
  genuine rule rejection and from two successful alternatives.

## Execution order after those decisions

### 0. Refresh evidence and preserve the branch

Read [README.md](README.md), [VALUES.md](VALUES.md), the contract, this plan, results, and only
relevant source and tests. Record exact branch/main refs, working-tree status, dirty work, and
baseline test failures. Keep main and sibling worktrees untouched. No merge, push, or commit follows
merely from this plan. Maintain the results document with exact evidence and limits. Use
[TESTING.md](TESTING.md) when running tests and [REVIEW_CRITERIA.md](REVIEW_CRITERIA.md) on the
complete final diff.

### 1. Finish task identification before transaction trials

Make matching a read-only pass. Compare the supplied task string against pending descriptions and
their meaningful retained data. Do not trial-execute several tasks and choose whichever works. If
a supplied string intersects exactly a single pending task, apply the owner-approved intersection
rule to form the request.
If several match, use only the owner-approved interchangeability rule; otherwise require an
explicit task identity. A task ID remains available but is not the normal requirement.

Use concrete checks: `Tile<Tharsis_6_7>` against a Capital tile task; never treat bare
`Tharsis_6_7` as a tile. In Canon, Capital cannot go on that water area; use a legal land area
such as `Tharsis_1_1` for successful Capital execution. Exercise repeated rewards with different
causes, identical instructions with different continuations, and the existing calculated-amount
match. If retained, include two tasks that both evaluate to `3 MC`. Keep metric evaluation
read-only and distinct from the Agent's already implemented sum of separate tasks.

### 2. Complete the engine's requested-transaction rule

Reuse existing intersection, dependency, limit, transaction, and disposable-World mechanisms.
Remove special-case inference when a shared rule covers it; do not add a parallel game interpreter.
For an identified task, count concrete narrowings whose complete requested transaction succeeds.
Zero is rejection when the proof is complete; exactly a single success executes; multiple successes
are ambiguous. A partial search is neither zero nor unique. Trials must leave no components,
tasks, selection, listeners, events, or history in the source World.

Characterize unsupported forms before claiming generality: optional/AMAP amounts, alternatives,
groups, shared bindings, nested instructions, and missing external information. Use the approved
deterministic proof result for cases the engine cannot finish. Measure candidate counts, attempts,
and complete call latency on fixed states; do not invent an unbounded integer list. Do not execute
newly queued Admin tasks during this proof. Direct mandatory `: Die` may reject at admission;
later Admin failure belongs to a later transaction.
Prove the `Repair?` counterexample through the raw engine path: zero and positive count can both
complete when its immediate effect repairs the limit, so the request cannot silently select zero.

### 3. Settle selection, handoff, and Admin scheduling

Walk the [automatic-actions inventory](TASK_HANDLING.md#automatic-actions-without-a-separate-execute-command).
For each selection-only entry, record task edits and component changes separately. Check the
Player-selected placement offered by World Government Advisor, its assignment to Admin, the
subsequent Admin transaction, and the Actor recorded for each change. Cover `Ok`, empty groups,
EACH, dispatch, continuations, and deferred promotion. Check voluntary partial narrowing where
choosing a target unlocks an AMAP amount. No transaction stays open while a person chooses.

Apply the same approved proof rule to Admin work and Player P1 priority checks. Retain Admin's
mandatory priority, FIFO attempts, and retry after progress unless a concrete Canon case proves
the selected behavior differs. Characterize greenery plant payment, shared research capacity, and
solo neutral placements before claiming all Admin cases are covered. Recheck Turmoil replay tests
after integrating work4's deck-model change; the current abstract `GlobalEvent` request does not
define the engine API.
Do not let the engine's task matcher use incidental order to choose among distinct permissions.
The existing Player EAGER policy has its own queue-order behavior; this project carries it over
without deciding its final design. Test engine semantics with Player policy `NONE`.

### 4. Simplify the client-facing call

Implement the selected public shape around an operation that performs zero or a single engine
transaction. Then run attached automatic behavior until quiescence, retrying Admin before every
automatic Player step. Each follow-up
engine command is a separate transaction. Preserve existing Player policy behavior only where it
is needed by current clients; moving policy is not a license to build the separate smart Player
project. Keep Agent provision and selection of automatic behavior distinct from the engine's
calculation of a submitted command. Retain the shared loop unless the selected public call shape
demonstrates a concrete need to change it. Do not add a registration framework for future flexibility.

Move or delete remaining engine-owned caller policy only after tracing its current users. Verify
nested calls, completion callbacks, operation completion, and returned changes over the whole
retained interval. Define what is restored when later work fails; a lower-level transaction must
always restore its own failure. Do not relocate responsibilities into `TfmGameplay` or keep obsolete
public APIs for compatibility.

Inventory the actual public surface, including `transact`, `transaction`, `doTask`, `selectTask`,
`narrowTask`, `tryTask`, `canSelectTask`, `canExecuteTask`, queue edits, correction methods, and
limit reads. For each, record whether ordinary clients need it, Agent needs it across modules,
or it can be internal/deleted. The finish line is an explicit documented API list, not merely a
new wrapper beside all old entry points.

### 5. Review, verify, and report completion honestly

Keep an acceptance checklist for settled answers. Test focused engine semantics, Agent carryover,
and Canon interactions. Expand to relevant replay tests for scheduling or call-shape changes; run
a full build at a coherent cross-module checkpoint. Measure raw transactions separately from
client-facing calls, with the same states and commands. Report conceptual cost (removed and added
APIs, layers, stored facts, exceptions) as well as runtime cost. Prefer a broader diff that deletes
an old concept to a small diff that adds a duplicate route.

Acceptance requires all owner-approved cases below, no unexplained existing Canon failures, a
review of the entire diff, and an API a client can use without accidental task creation or
unnecessary raw operations. Tests of the engine's own choice behavior must use Player policy
`NONE` so current EAGER behavior does not obscure it. A passing finite subset is not proof of
full engine coverage. Leave unresolved cases clearly marked instead of declaring the transition
finished. Update the owning docs and `TODO.md` to match verified behavior; do not preserve this
plan's stale steps as future work after they are completed.

## Concrete acceptance cases

| Scenario | Required observation |
| --- | --- |
| Typed tile input | `Tile<Tharsis_6_7>` names a tile for a schematic Capital task, but Canon rejects Capital on that water area. A legal land area such as `Tharsis_1_1` must succeed when other checks allow it; a bare area is not a tile request. |
| City placement | If Capital cannot complete the transaction and Normal City is the sole success, execute Normal City. Two successes remain ambiguous. |
| Protected Habitats | Removing an unprotected Player's plant can succeed; direct mandatory removal of a protected Player's plant is rejected at the correct point. |
| Land Claim | An immediate cube removal repairs the temporary limit violation before final checks. |
| Continuations | Two tasks with the same immediate change but different future work are not interchangeable. |
| Calculated task | If retained, test `1 MC / Foo` with 3 Foo present against `3 MC`, plus multiple tasks resolving to that same change. |
| Delayed failure | A transaction that queues Admin work is judged without executing that work; the public call handles Admin afterward and reports a later failure according to the approved recovery rule. |
| Selection handoff | A Player selection that assigns concrete work to Admin does not execute as Player; Admin may then execute automatically in the same client-facing call. |
| Turmoil replay integration | After work4's deck-model change is integrated, the existing Turmoil replay tests still pass; the former abstract event request is not an engine API acceptance case. |
| Unknown analysis | A deterministic unfinished result never becomes game-rule rejection or proof of a unique choice. |
| Optional `Repair?` | Test both zero and positive candidate transactions; do not accept zero as unique when `Repair!` also succeeds. |
| Return and recovery | Failed trials retain no source changes; direct transaction failure restores its state; whole-call results and later failure match the approved rule. |

The first experiment report remains in `_local/engine-transition-evidence/first-pass-report.md`.
Historical [SMART_AUTOEXEC.md](obsolete/SMART_AUTOEXEC.md) may inform discussion of composable
autoexecutors but has no authority over the selected public API.
