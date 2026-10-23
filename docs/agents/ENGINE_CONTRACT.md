# Engine contract

> **Human review required — October 22, 2026.** **Selected** marks direction explicitly
> confirmed during review; everything else is tentative/proposed until reviewed.
> **Recommendation** identifies additional suggestions. Current-code observations are evidence, not approval of that behavior.
> Keep interaction terms here, not in the glossary.

## Foundations

The component state is a multiset; component changes are additions and removals. Tasks, effects,
and event history participate in the existing reversible state model. Within a live World, changes
follow a single linear timeline. This design assumes serial calls, not simultaneous calls from
different threads.

A checkpoint identifies a position to restore. Nested rollback scopes can use their own checkpoints
without a separate commit protocol: a small scope restores an individual failed change; an enclosing
scope can restore several completed changes together. These are scopes over the same recorded
history, not concurrent transactions. The existing [Timeline](../../src/common/dev/martianzoo/engine/Timeline.kt)
has navigation and restoration limits; this foundation does not promise restoration of arbitrary
application state or history that has been discarded.

The engine calculates the consequences of submitted instructions. **Selected:** exploring whether
choices can complete an entire action, and pruning choices that cannot, are agent concerns the
engine module does not and will not take on. Raw material for that work belongs in
[DECISION_GRAPH.md](DECISION_GRAPH.md).

## Clients and transactions

Player clients make explicit choices, including small, tedious choices. The engine calculates their
consequences. Admin is also an Actor driven by an agent outside the engine module, much like a
Player. In ordinary games its driver handles automatic work rather than manual input.

**Selected:** Admin always uses the eager handling described below. P1 priority for Admin tasks,
versus P2 for Player tasks, explains most scheduling differences. Other policies are conceivable
because Admin is an Actor too, but exploring or offering them has no value for this design.
“Turn off autoexec” and configurable autoexecution levels always refer to Players; Admin remains
fully automatic, including during replay.

| Term | Meaning |
| --- | --- |
| **Task** | An instruction waiting to be handled by its assigned Actor. |
| **Transaction** | A client-initiated state change and the immediate consequences needed to make the world self-consistent again. |

Describe resolution and concreteness separately. Even an instruction that is both resolved against
the current World and concrete can fail during execution or final checks. These properties say
nothing about completion of a whole game action, a queued continuation, or tasks execution may create.

[TASK_HANDLING.md](TASK_HANDLING.md) inventories the current automatic revisions and handlings,
including their timing and differences from this contract.

## Inside a transaction

Each transaction begins with a potentially state-changing call into the engine from a client acting
for an Actor. It includes the requested change, task additions/edits/removals, dependent removals,
recursive double-colon (`::`) effects, and checks of temporarily suspended invariants. If execution
or final checks fail, the engine restores the entire transaction before returning the failure.
**Selected:** a public engine mutation must not return with an inseparable change partly applied.
Clients need not add a rollback wrapper to obtain that guarantee.

For example, placing a tile on your
[Land Claim](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/board.pets) removes your
existing cube through `::`. Cube and tile may coexist during that work; they must not both remain
when final checks run.

No competing client command may change the world during this work. The transaction's own
consequences must run, including task creation and the cube's removal. Resulting Admin tasks are
enqueued now and handled in subsequent transactions.

## Running to quiescence and returning results

**Selected scope:** this design focuses on the synchronous API. The client submits the specific
change it wants performed as a transaction. After that transaction finishes, the combined engine
and drivers continue eligible work until they reach quiescence, then return to the caller. Each
Admin task is performed through a subsequent transaction, with the same rules as a Player's.
This describes the existing engine-and-agent division; it does not move Player strategy into the
engine module or require a separate name or object for the enclosing call.

Quiescence means the mandatory Admin driver and configured Player drivers have no further work
they can perform without more client input. It does not mean every task is gone or the game action
is complete. No eligible Admin task may remain: a full pass without Admin progress is an exception,
not successful quiescence. Deferred work can become eligible during this progression and must be
considered before returning, using the promotion rules below.

For example, using AI Central starts a Player transaction, followed by Admin transactions handling
`UseAction` and its bookkeeping. With manual Player choices, the call can then return with the draw
task pending. Similarly, if Stan places greenery beside Kim's tile while Kim has
[Philares](../../test/common/dev/martianzoo/tfm/tests/cards/PhilaresTest.kt), Kim's resource choice can
remain pending when Stan's call returns.

Taking a plant might trigger phase completion, production, and a generation change before the call
returns. Those events were triggered by the command; they are not all fulfillment of “take a plant.”
Individual transactions remain smaller than the synchronous call that runs this progression.

**General result rule:** a returned `TaskResult` should cover everything that happened during that
call, including the initiating transaction, subsequent Admin work, and any other work performed
before return. Work performed by a completion callback is included too. Changes that were undone
before return are not reported as retained changes. With the current fields, `changes` includes
component changes in event order across that entire interval, and `tasksSpawned` includes tasks
added during it that remain pending at return. The event log retains the detailed event history.
A UI can present those intermediate events without requiring a separate API return for each event.

For example, if a greenery placement triggers an Admin-handled steel bonus before returning,
the result must include both the placement and the steel gain. Capturing the result immediately
after placement and omitting the subsequent bonus would violate this rule. This reporting rule
says how much the result covers; it does not by itself make the whole call failure-atomic.

## Which task acts next

Lower numbers take precedence.

| Priority | Work |
| --- | --- |
| P0 | The selected task, for any Actor |
| P1 | Ordinary Admin tasks |
| P2 | Ordinary Player tasks |
| P3 | `LATER` tasks |
| P4 | `FINALLY` tasks |

A Player cannot select a P2 task while P1 work remains. A selected task excludes competing client
mutations until it finishes. Admin tasks its execution creates wait until that transaction finishes.
The synchronous call returns its results only after all eligible Admin work finishes; deferred
Admin tasks may remain. This also rules out a callback inviting another Player command while P1 work remains.

**Recommendation:** gather unfinished Player choices in the client, then submit the task and its
fully concrete instruction together. Choosing a card and then a quantity can be useful UI work
without changing game state. Otherwise, committing an unfinished selection would exclude other
clients until that task finishes. No transaction should stay open while a person thinks.

When P0–P2 are empty, promote **all existing P3 tasks together**: Admin tasks become P1 and Player
tasks become P2. New `LATER` work waits for a subsequent promotion.
[Solar Probe](../../test/common/dev/martianzoo/tfm/tests/cards/SolarProbeTest.kt) shows why: its own
Science tag counts toward its draw; its deferred flip waits until the draw and other ordinary
work finish.

Promotion is checked only when a transaction completes, not while its queue is temporarily empty:
`Transactions` performs it at the end of a successful transaction, so the promoting task edits
belong to the transaction that made the work eligible. **Recommendation:** when P0–P3 are empty,
promote all P4 tasks together using the same Actor-based priorities.

## Admin's choices and retries

Admin handles eligible tasks in FIFO order. If a task cannot currently execute, move it to the end.
Each success starts a fresh pass through the remaining tasks. If eligible tasks remain but a full
pass executes none, throw an exception: the game cannot return to a Player in that state.
Deferred P3/P4 tasks do not count until promoted.

Before executing an Admin task, determine **exactly one fully concrete legal instruction** that
narrows it.
Zero or multiple instructions mean it cannot currently execute. Partial narrowings do not count.
Admin cannot leave ambiguous work pending while a Player acts to resolve it.

The intended meaning of **legal narrowing** is that submitting it can complete its entire transaction
successfully, including `::` consequences and final invariant checks. This excludes checking
additional spawned tasks that execute in later transactions. How to determine the complete set of
such concrete narrowings remains open.

“Fully concrete” describes the current instruction. Admin can execute the concrete System signal
[`PlaceGreeneryTile`](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/board.pets);
its consequence still asks a Player which hex receives the greenery.

An unsuccessful transaction must leave no changes behind before Admin tries another task.
**Recommendation:** distinguish game-rule rejection, such as insufficient resources, a count limit,
or `Die`, from an unexpected implementation error. Record game-rule rejection reasons for the
unsuccessful-pass exception; propagate implementation errors immediately.

## What belongs to Admin

System-component work is reassigned to Admin. **Recommendation for timing:** retain required Player
choices until the change is concrete, then assign it to Admin. Also assign concrete changes explicitly
performed `BY Admin` to Admin, including non-System changes.

For [World Government Advisor](../../test/common/dev/martianzoo/tfm/tests/cards/WorldGovernmentAdvisorTest.kt),
the Player chooses the neutral ocean's hex although Admin performs the placement. Admin's uniqueness
rule concerns tasks currently assigned to it, not everything ultimately performed `BY Admin`.

Which types are System is a design choice. The usual reason is that they represent machinery the
Player has little reason to notice, rather than tangible game concepts. Precisely distinguishing this
from being non-`Iconic` is deferred; today's type classifications do not settle that discussion.

## Failure and recovery

**Selected:** public engine mutations guarantee failure atomicity for their transaction: failure
restores its starting game state, including components, tasks, derived effects, and event history.
An implementation can make intermediate changes internally, but cannot expose them as the result
of a failed call. Safe client-controlled rollback remains useful for larger groups of calls;
it is not a prerequisite for recovering a failed engine transaction.

For example, removing an opponent's plant protected by Protected Habitats can encounter `Die`
after the internal removal. The failed engine call must restore the plant and the rest of that
transaction. **Current-code evidence:** the
[functional test](../../test/common/dev/martianzoo/engine/ActorEngineTransactionTest.kt) currently obtains
this restoration with an enclosing `world.timeline.atomic { ... }` block. That demonstrates the
existing facility, not compliance by every engine method. Arbitrary application side effects remain
outside the engine's state guarantee.

**Recommendation, still separate from transaction atomicity:** restore the enclosing synchronous
call on failure, including its completed Admin transactions. This restores more than the last failed
transaction, preventing a rejected Player command from leaving earlier changes behind. The call can
also include subsequent game progression; deciding its recovery scope does not make all that work
fulfillment of the initiating command. Reconsider the scope if it requires disproportionate machinery.

**Current-code evidence:** asking for AI Central's nonexistent second action reaches the System
`UseAction` task, whose consequence is `Die`. The existing
[Canon card test](../../test/common/dev/martianzoo/tfm/tests/cards/AiCentralTest.kt) expects rejection,
then successfully uses its real action. `Die` itself is not System; a direct mandatory `Die`
throws when the task would be created, rather than waiting in the queue. Current code therefore
permits an invalid Player choice to fail during Admin handling.

Still to decide, separately from the recommendations above:

- Is failure during Admin handling an ordinary rejected Player choice, a broken model, or either
  depending on the cause? Recovery scope alone does not answer that.
- Can a later failure require undoing an earlier call that already returned successfully?
  The AI Central example does not establish that need.

## Current implementation differences

These are observations to address during review, not reasons to preserve the behavior.

- Some public engine mutations still need an enclosing `Timeline.atomic` block to restore a
  failed transaction. That differs from the selected failure-atomic guarantee.
- [WorldTransaction](../../src/common/dev/martianzoo/engine/WorldTransaction.kt) ordinarily restores
  the initiating command and its Admin handling together on failure. Its returned `TaskResult` now
  satisfies the general result rule for a call's own interval, including completion-callback work
  and the handling that follows it, but a failing callback still leaves the initial work in place.
  Queue edits remain outside that coverage because they skip task handling entirely.
- Queue edits and corrections can change the world while another task is selected, bypassing
  the proposed exclusion of competing client mutations.
- Player selection does not enforce the P1 gate, and selection can still return with an unfinished
  choice. Deferred promotion no longer happens during queue edits.
- Selection sometimes tests a concrete reassignment by executing the receiving Actor's instruction
  and restoring it. That can reject the current call based on work proposed for a later transaction.
- Admin handling currently follows a configurable policy. Disabling it leaves ordinary Admin work
  pending; replay currently disables it along with Player policies.
- An unrelated pending Player task can hide Admin rejection from the caller. The
  [AI Central test](../../test/common/dev/martianzoo/tfm/tests/cards/AiCentralTest.kt) demonstrates
  a call using its nonexistent second action returning with failing Admin work still pending.
  This differs from the proposed exception after a full pass without Admin progress.
