# Sequencing and completion

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** deciding whether work is independent, choosing between `:`, `::`, and `THEN`, or
> defining when an operation has finished.
>
> **Status:** current contracts and working rules, followed by one selected but unimplemented
> direction. Source and tests are authoritative for current behavior. [ENGINE.md](ENGINE.md) owns
> task execution; the [Pets language specification](../pets-language-spec.md) owns syntax and static
> semantics.

## The model

The pending task pool represents choices still available to Actors. It is not a program counter,
stack, or implicit chronology. A comma-separated group becomes independent tasks. Stable iteration
order exists for reproducibility and display only.

Order enters the model through a small number of explicit facts:

- a consequence cannot exist before its triggering `ChangeEvent`;
- a selected task excludes competing ordinary gameplay until that task finishes;
- `A THEN B` admits B only after A's task finishes;
- `A:: B` executes B inline before queued consequences of A are admitted; and
- a component gate can keep work unavailable until the component is removed.

None of these supplies a general priority. Once ordinary work is pending, an Agent may choose among
the tasks assigned to it. Agent policy can choose an order but cannot turn that order into engine or
game semantics.

## The promises

The current implementation provides these structural guarantees:

- **Causality:** effects arise from recorded component changes, and their work carries the
  triggering event as its cause.
- **Automatic coherence:** all automatic consequences of one change run before queued effects of
  that change are evaluated.
- **Trigger snapshot:** every automatic listener in one batch decides whether it matches, including
  trigger-side conditions, against the same post-change World.
- **Failure atomicity:** an exception or dead end restores components, tasks, history, and derived
  effect indexes to the transaction checkpoint.
- **Selection integrity:** only one task may be selected across the World, and authored Pets cannot
  edit, cancel, or reprioritize another task.

Trigger snapshot is enforced by the eager construction of `Effector.fire`'s result, but no focused
regression test currently pins it.

Two broader rules are design obligations rather than mechanically proved properties:

- **Freedom:** every rules-legal ordering of pending choices should remain executable. Different
  orders may legitimately produce different outcomes.
- **Listener independence:** independent automatic listeners must not acquire game meaning from
  registration or diagnostic sort order. A component's own automatic effects retain their authored
  declaration order.

Freedom has only scenario coverage. Listener independence has a manual randomized-order diagnostic,
`SOLARNET_RANDOM_AUTOMATIC_EFFECTS=true`, not a seeded invariant test. Treat both as review
obligations, not as permission to depend on the current order.

## Before adding order

First identify the illegal **committed result** that the proposed ordering prevents. A locally legal
choice that later dead-ends is not such a result: the enclosing transaction rolls back, and another
choice can be attempted. Pre-pruning every doomed branch usually duplicates the rule that rejects
it.

Then say which property is actually required:

- **precedence:** B may not happen before A;
- **immediacy:** no player work may intervene between A and its fixed consequence;
- **completion:** B waits until a particular operation and its consequences have finished;
- **game-rule indivisibility:** the game treats several visible changes as one operation; or
- **failure atomicity:** implementation state rolls back together after failure.

These properties do not imply one another. `THEN` gives precedence but not transitive completion.
`::` gives immediacy but does not erase the changes it performs. A transaction gives rollback but
does not decide what the physical game counts or permits another player to observe.

Use the least mechanism that states the real rule:

| Mechanism | Appropriate meaning |
| --- | --- |
| Nothing | B is naturally unavailable before A, or a bad order only rolls back. |
| `A: B` | Every A creates ordinary, selectable B work. |
| `A:: B` | B is a fixed consequence required before the World may be exposed. |
| `A THEN B` | This authored instruction owns both stages, and only A's task must finish first. |
| Committed precursor | A modifier must react after an operation is committed but before its final component exists. |
| Specific latch | Several known prerequisites must finish before later work becomes legal. |

A queued Player task should represent a real Player choice: whether or when to act, which
alternative to take, or how to narrow it. Put fixed work at its semantic owner instead of giving the
Player a pulse or cleanup chore: use `::` for a coherence-restoring consequence, an Admin-assigned
task for neutral or workflow activity that should remain observable, or the exact lifecycle's
completion event for delayed cleanup. Admin assignment routes work; it does not prove that every
outcome is equivalent. If none of these fits, leave the mechanism open rather than inventing a
client bridge.

If the last row appears to mean “all consequences of this operation finished,” do not add another
counted barrier before reading [The missing rule](#the-missing-rule-when-an-operation-is-over).
A latch controls legality, not priority; unrelated legal work remains reorderable.

For a real precedence rule, preserve the authoritative wording and add both the smallest observable
precedence scenario and, where relevant, a representative scenario proving that legal sibling
orders remain available. Never repair sequencing in `TfmGameplay`, autoexecution policy, rendered
task matching, or incidental queue order.

## What `THEN` does

`A THEN B` relates stages of one authored instruction. Task normalization normally stores A as the
current task and B as its continuation. If an explicitly marked Type variable must remain shared,
the sequence stays together until narrowing binds it safely. When A finishes, B becomes ordinary
pending work.

B receives no priority over other tasks and does not wait for queued effects caused by A. For two
chains, `A1, A2, B1, B2` can therefore be legal. A selected A does exclude competing gameplay while
A itself is being narrowed and executed; that lock ends with the task, not with all causal
descendants.

This distinction also applies to fanout. `EACH Player { A THEN B }` creates one continuation per
branch. `EACH Player { A } THEN B` does not join the branches or wait for their descendants; see
[EACH.md](EACH.md#sequencing).

## Automatic effects

For one component change, the engine first materializes the complete matching `::` batch. It then
executes that batch recursively. Only after those automatic chains finish does it evaluate the
matching queued `:` effects for the original event. An automatic reaction never becomes a task or
asks an Actor to select it.

Use `::` only when the result is fully determined, leaving it outstanding would expose an
incoherent World, and it must happen before any queued player work. It is not a fast path, a preferred
task order, or an operation-completion mechanism. If two automatic listeners have a causal
dependency, make the first listener's resulting event trigger the second; do not rely on listener
order or retry the original batch against a mutated World.

Use trigger-side `IF` when the requirement qualifies the original event or cannot change before
resolution. A requirement inside the queued instruction is decided later, so use that form only
when intervening work is meant to decide availability. `A: (R: B) OR Ok` additionally makes B
declinable and is correct only when declining is legal. Language rule L6 owns the exact semantics.

One current fragility is worth retaining: gaining a `Colony` performs the track adjustment
automatically and queues its placement bonus. The bonus therefore sees the adjusted track only
because automatic work precedes queued work. If a placement bonus ever reads that track, express the
dependency directly instead of treating this consequence of `::` versus `:` as a new rule.

## Committed precursors

Sometimes the final event is too late for a modifier. Card discounts need printed tags before the
played face and its tags enter play. `PayingFor<Class<Component>>` is the established shape: it
is created only after the operation commits to the eventual purchase or play, carries the
multiplicity modifiers need, and is ordered before settlement and the final result. Rollback removes
the precursor and everything it caused if the operation cannot complete.

A precursor is justified only when every producer commits to the later result and that result is too
late to carry the rule. Keep ordinary reactions on the final event, and never emit the precursor as
a free-standing notification. A generic “before A” hook was rejected because B could invalidate an
already-resolved A; the precursor remains an ordinary recorded change with an honest cause.

## The missing rule: when an operation is over

The engine can represent current components, pending tasks, point events, and intervals represented
by live components. It cannot yet derive that one particular interval is finished when all work
caused by that interval is gone.

The existing approximations measure different things:

| Mechanism | What it actually waits for |
| --- | --- |
| `THEN` | One task. |
| `Temporary` | The whole World's task pool. |
| `Owed`, `Billing`, `TradeBarrier` | Prerequisites enumerated by that particular lifecycle. |
| One Actor's queue drain | All work currently assigned there, including unrelated work. |
| A client task search | An implementation pattern, not a game fact. |

Specific latches remain the smallest honest current solution when a lifecycle knows its exact
prerequisites. Do not generalize queue drain or client recognition into completion semantics.

### Selected direction: scoped completion

The selected direction is causal completion: a live operation component becomes removable when no
pending task descends from the event that created that operation. The ancestry need not be stored as
new mutable state. A task has a `Cause`, its resulting changes retain that cause, and later triggered
work names those change-event ordinals. Walking the event log derives ancestry and naturally follows
rollback.

Two questions block implementation:

1. **Operation identity.** Components form a multiset; equal Types have no instance identity. A
   scope needs either a proven maximum-one lifetime or an explicit operation identity, including a
   rule for remove-and-regain within one causal chain.
2. **Cross-Actor descent.** If an attack creates a victim's choice, honest causal completion waits
   for that choice. Confirm that game rule before allowing one Actor's operation to depend on another
   Actor's response.

After those answers, the first slice must implement the ancestry query for exactly one action
lifecycle and delete one client bridge that currently recognizes its tasks. Do not migrate
`Temporary`, add a task cache, or introduce a general scope framework in that slice. If it cannot be
done with one narrow lifetime marker and its completion consequence, stop and reassess the model.

A later benefit may be a correct validation point for positive lower bounds temporarily broken and
repaired by one causal operation. That possibility must not broaden the first slice.

## Cleanup vocabulary

The built-in Classes state two independent concerns: whether unfinished state is legal at a
completion boundary, and who removes it.

| Class | Current meaning |
| --- | --- |
| `MustCleanUp` | Completion invariant checked by operations that declare themselves complete. |
| `Barrier` | `MustCleanUp` state whose owning game rule removes it. |
| `Signal` | An unscoped point event that removes itself automatically. |
| `Temporary` | State the engine removes only when the whole task pool is empty. |
| `TemporaryScope<Parent>` | A parent-dependent `Scope` that is both `Temporary` and `MustCleanUp`. |

Plain `Temporary` is deliberately not `MustCleanUp`: it may survive a narrower manual operation
while unrelated tasks keep the World non-idle. A `Signal` is deliberately not a `Scope`; absence of
a scope dependency is its point. `TemporaryScope` combines a cleanup policy with an invariant, but
its retirement is still whole-World-idle rather than causally local.

### Current behavior: whole-World idle cleanup

At the outer transaction boundary, policy settlement runs first. If every task queue is empty, the
engine removes all instances of one concrete `Temporary` Type that has no direct or indirect
dependent matching `Temporary` or `MustCleanUp`. It then settles work caused by that removal and
rechecks the live queues and dependencies before attempting another Type. Only after this loop can
the workflow completion callback run; synchronous callback work receives the same settlement and
cleanup protocol before the final position is recorded. Every step remains inside rollback.

This policy is exact for genuinely global resting points such as final-scoring settlement. It is
too broad for action-local cleanup and currently makes an `EventCard` wait for unrelated pending
work before becoming `PlayedEvent`. Do not describe `TemporaryScope` as operation-local until causal
completion exists.

## Open evidence and rule questions

The most valuable missing checks are a direct trigger-snapshot regression, a seeded
automatic-listener permutation test comparing normalized state and task multisets, and a replay
experiment that permutes representative legal player-task orders through the next stable point.
Exact event order need not match.

Three content cases remain evidence for missing or unsettled semantics, not invitations to build
one-off machinery:

- Head Start's two action tasks can interleave; settle it through the general action lifecycle.
- Two Mars University activations can perform both discards before either draw; authoritative rules
  evidence must decide whether each exchange is indivisible.
- Candidate draw/select/play chains do not isolate or force the selected candidates; any repair
  should use one operation-scoped candidate representation.

Do not revive automatic-batch retries, player-queue drain as completion, presentation order as task
identity, or payment-effect ordering as attribution repair. Each substitutes incidental machinery
for an authored dependency or a missing semantic fact.

## Implementation evidence

- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) and
  [`Effector.kt`](../../src/common/dev/martianzoo/engine/Effector.kt) own automatic-before-queued
  execution and batch selection.
- [`TaskQueues.kt`](../../src/common/dev/martianzoo/engine/TaskQueues.kt) and
  [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt) own `THEN` normalization and continuation.
- [`WorldTransaction.kt`](../../src/common/dev/martianzoo/engine/WorldTransaction.kt),
  [`Engine.kt`](../../src/common/dev/martianzoo/engine/Engine.kt), and
  [`SystemDeclarations.kt`](../../src/common/dev/martianzoo/pets/SystemDeclarations.kt) own current
  transaction and cleanup behavior.
- [`AutomaticEffectOrderTest.kt`](../../test/common/dev/martianzoo/engine/AutomaticEffectOrderTest.kt),
  [`WorldTransactionTest.kt`](../../test/common/dev/martianzoo/engine/WorldTransactionTest.kt), and
  [`TemporaryCleanupTest.kt`](../../test/common/dev/martianzoo/engine/TemporaryCleanupTest.kt) are
  the focused regression evidence.

Phase topology belongs to [WORKFLOW.md](WORKFLOW.md), action and payment lifecycles to
[ACTIONS.md](ACTIONS.md) and [PAYMENTS.md](PAYMENTS.md), Actor and assignee identity to
[IDENTITY.md](IDENTITY.md), and Agent selection policy to [AUTOEXEC.md](AUTOEXEC.md).
