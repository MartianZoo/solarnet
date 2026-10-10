# Sequencing, delegated control, and completion

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing task order, delegated control, `THEN`, automatic effects, cleanup, task
> priority, or how later work waits for earlier work.
>
> **Status:** current contracts, a verified missing completion rule, and the task-priority working
> direction. Other replacement designs belong in [`SOLARNET_ROADMAP.md`](../../SOLARNET_ROADMAP.md)
> or a focused investigation.

## Current model

There is a global task pool; Actor queues are filtered views. A comma-separated group becomes
independent tasks. Stable iteration order supports reproducibility and display but carries no game
meaning.

- Effects arise after their triggering component change.
- A selected task excludes competing ordinary gameplay until that task finishes.
- `A THEN B` admits B after A's task finishes, without waiting for A's queued reactions.
- `A:: B` executes B inline before queued effects of A are evaluated.
- A component gate can make an instruction unavailable until its prerequisites hold.

There is no general task priority or exclusive interaction spanning several tasks. An Agent may
choose among its available tasks; policy cannot supply a missing game rule.
[Task priority](#task-priority-working-direction) is the working direction for deferred work.

## Required promises

- **Causality:** queued effects retain their triggering change and effect context as `Cause`.
- **Automatic coherence:** required parts exist before gain reactions; recursive automatic
  consequences run before queued effects of the originating change are evaluated.
- **Count invariants:** declared bounds hold after an initiating change and its recursive automatic
  consequences.
- **Trigger snapshot:** `Effector.fire` materializes an automatic listener batch against the same
  post-change World before executing it.
- **Failure atomicity:** failure restores components, tasks, history, and derived indexes to the
  enclosing transaction checkpoint.
- **Selection integrity:** at most a single task is selected across the World. Authored Pets cannot
  edit, cancel, or reprioritize another task.

Legal sibling orders remain available even when they produce different outcomes. Independent
automatic listeners must not acquire meaning from registration or diagnostic sort order. A
component's own automatic effects retain authored declaration order.

## Before adding order

Identify the game rule that makes a result or task order invalid. A caller using another Player's
Agent is not itself an invalid engine operation: attribution through the assigned Agent is enough.
Authentication and acceptance of that caller's submission belong to
[ADVERSARIAL.md](ADVERSARIAL.md), not to task locks or a new delegated-session mechanism.

A choice that fails within the same transaction can often be rejected through ordinary Pets and
rollback. That does not cover an interaction spread across committed commands: rejecting a later
payment step does not undo earlier partial payment.

Distinguish precedence, immediate consequences, exclusive control, completion, and atomicity.

| Mechanism | Meaning |
| --- | --- |
| No extra ordering | Availability and rollback already express the rule. |
| `A: B` | A creates ordinary selectable B work. |
| `A:: B` | B restores coherence before queued player work can proceed. |
| `A THEN B` | A's task must finish before B is admitted. |
| Committed precursor | A modifier needs an event before the final result exists. |
| Specific latch | Known prerequisites must finish before later work becomes legal. |

A fixed outcome does not imply automatic execution or Admin ownership. Conversely, implementation
housekeeping should not become a Player choice merely because it currently occupies a task. Do not
repair ordering through `TfmGameplay`, policy settings, rendered-task matching, or pool order.

## What `THEN` does

Task normalization stores A as current work and B as its continuation. Shared unresolved Type
variables can keep the stages together until narrowing supplies their value. When A finishes, B
becomes ordinary pending work with no priority over siblings or A's reactions. The selected-task
lock ends with A.

`EACH Player { A THEN B }` gives each branch a continuation. `EACH Player { A } THEN B` does not
join the branches or wait for their descendants; language rule L9-1 owns fanout semantics.

## Automatic effects

Required component parts are constructed before gain reactions. Parts' automatic reactions
precede their owners' reactions, and construction events' automatic work precedes queued matching.
For each change, the engine materializes the matching `::` batch, executes it recursively, then
evaluates queued `:` effects. An automatic reaction never asks an Actor to select it.

Use `::` for determined consequences whose delay would expose incoherent state. If automatic
listeners depend on each other, express the dependency through resulting events instead of batch
order or retries. Use trigger-side `IF` for a condition decided by the triggering event; put a gate
in queued work when intervening changes should decide availability.

An automatic `Die` can reject a forbidden component change and roll it back. It cannot generally
police task selection because selecting or narrowing an abstract task can change task state without
producing a component event. Trigger-side `BY` matches the event Actor; instruction-side `BY`
reassigns concrete queued work.

## Committed precursors

`PayingFor<Class<Component>>` lets modifiers react before a purchase or card play produces its
final component. A precursor represents commitment to that result, carries required context, and
precedes settlement. Ordinary reactions still belong on the final event.

## The missing rule: when an operation is over

The engine has no general completion rule for an interaction that spans queued work.

| Existing fact | What it measures |
| --- | --- |
| `THEN` | Completion of its current task. |
| `Temporary` | Emptiness of the entire task pool. |
| `Owed`, `Billing`, `TradeBarrier` | Prerequisites represented by a particular lifecycle. |
| An Actor's empty filtered view | All currently assigned work, including possibly unrelated work. |
| A client task search | A recognized implementation pattern. |

These are not the same completion point. `Barrier` and `MustCleanUp` are checks, not locks against
intervening selection.

Neptunian Power Consultants demonstrates the interaction. P1 may hand an optional decision to P2,
but the selected-task lock ends when that task finishes. Payment descendants return to P1's controller,
and P1 can resume unrelated work while P2's payment remains unfinished. `FloodingTest` contains current
observable characterizations, including resource changes that can make the payment fail;
[`PAYMENTS.md`](PAYMENTS.md#verified-gaps) describes the payment-specific evidence.

Separate the questions before selecting a replacement. The engine must attribute each selection,
narrowing, and execution to its current assignee and calculate the consequences of the supplied
order. Any additional restriction on intervening work needs a game-rule reason, not a requirement
to stop the caller from using P1's or P2's Agent. Helpers that call both Agents are legitimate;
their success alone does not establish that the task routing or calculated outcome is correct.

Completion and cleanup still need coherent game semantics. A smaller model should remove client
searches and overlapping lifecycle machinery rather than assume exclusive control across an entire
operation is required. Local failure restores the enclosing transaction; agreement to retract
already shared decisions or proceed after a disclosure belongs to [ADVERSARIAL.md](ADVERSARIAL.md).
The external arrangement handles that agreement without making incorrect payment or cleanup
calculations acceptable. The roadmap records the remaining design concern;
[task priority](#operation-completion) proposes a whole-World answer.

## Cleanup

| Class | Current meaning |
| --- | --- |
| `MustCleanUp` | State forbidden by operations that declare themselves complete. |
| `Barrier` | `MustCleanUp` state removed by its owning game rule. |
| `Signal` | An unscoped point event that removes itself automatically. |
| `Temporary` | State removed when the entire task pool is empty. |
| `TemporaryScope<Parent>` | A parent-dependent `Scope`, both `Temporary` and `MustCleanUp`. |

After policy settlement, an empty global task pool lets the engine remove a concrete `Temporary`
Type with no direct or indirect `Temporary` or `MustCleanUp` dependent. It settles resulting work
and rechecks before further cleanup. The workflow completion callback follows this process.

Plain `Temporary` is not `MustCleanUp`: unrelated tasks can keep it alive across a narrower manual
interaction. Event cards become `PlayedEvent` through idle cleanup, not an explicit end-of-turn
task.

## Task priority (working direction)

**Working direction, not implemented.** The owner expects that one small priority concept may
replace the deferred-work devices listed below; the proving cases decide whether it is adopted. An
August 2026 version of this direction (commit `b074f54dd`) was replaced within days by
`Idle<Player>` and `Yield<Player>` signal designs, then by whole-World `Temporary` cleanup. Each
replacement moved deferred work back into components. Do not reintroduce a signal, marker, or
continuation component for deferred work without the owner's direction.

### Facts in components, future work in tasks

The component graph records what is, or what is happening. Work that should happen later belongs in
a task. A component that exists so that its removal can start later work is a scheduling device, not
a game fact; do not add new ones. The `workflow` branch's `Continuation` class makes that device
generic.

Current devices:

| Device | Work it defers |
| --- | --- |
| `EventCard` as `Temporary` | The move to `PlayedEvent`, at an empty task pool |
| `TradeBarrier` | `FinishTrade`, until every barrier the Trade received is removed |
| `TurmoilSolarOperation` | `FormGovernment THEN ChangingTimes`, after TR revision and the current global event |
| `FinalScoringPending`, `MeasureAward` | Award placement and `Victory`, after final-scoring work |
| `TfmWorkflow.Automatic` with `World.onTransactionComplete` | The next phase or turn, from Kotlin, at World idle |

Where the component also records a real fact, keep it and change only the timing. A played
`EventCard` is in play, and its tags count, until it is flipped; only the flip becomes deferred
work.

### Bands

| Band | Meaning |
| --- | --- |
| P0 | The selected task: the only task that may advance. |
| P1 | Ordinary pending work: any P1 task may become the next P0. |
| P2 | Settlement: work that completes the current operation after its ordinary consequences. |
| P3 | Workflow: the next turn or phase. |

P0 and P1 are the current model. P2 and P3 are new.

- **TP1.** No P2 task may be selected while any P0 or P1 task exists. No P3 task may be selected
  while any P0, P1, or P2 task exists.
- **TP2.** When no P0 or P1 task exists, every P2 task becomes P1 at once. When no P0, P1, or P2
  task exists, every P3 task becomes P1 at once.
- **TP3.** Work created by executing any task enters P1 unless its instruction is authored as P2 or
  P3. With TP1 and TP2, deferred work created while a promoted batch runs waits for that whole
  batch and its consequences.
- **TP4.** A task's band is set when the task is created. Only promotion changes it; authored Pets
  cannot change another task's band.
- **TP5.** Bands restrict selection only. They apply equally to explicit commands and
  autoexecution, and they do not order tasks within a band: legal sibling orders stay available.
- **TP6.** Bands apply to the whole World task pool, not to one Actor's queue.
- **TP7.** Promotion changes task state. It rolls back with its transaction and must be
  reproducible from the event history.

P3 is below P2 because settlement belongs to the operation that caused it. Playing Solar Probe
creates its effects as P1 work and its flip as P2 work. Its own science tag therefore counts for
every consequence of its effects, and it reaches the played-event pile before any P3 turn handoff.
Entering Research creates each Player's research choices as P1 work and
`ActionPhase FROM ResearchPhase` as P3 work. Action begins only after every Player has finished and
their settlement has run.

TP2's alternative leaves bands fixed and makes the highest occupied band selectable. The two differ
only when a promoted task creates P1 work while others from its batch still wait. Under TP2 those
others stay selectable beside the new work; under the alternative they wait for it. No Terraforming
Mars case is known to distinguish them. TP2 is preferred because "a settlement batch becomes
ordinary work" is the simpler statement.

Priority is neither `THEN` nor a gate. `A THEN B` admits B when A's own task finishes, without
waiting for A's reactions; a P2 task waits for all P0 and P1 work in the World. `Owed` and
`Required` remain components because they record quantities, and their survival still makes an
operation incomplete.

### Operation completion

Under these bands, an operation is over when no P0, P1, or P2 task remains, and P3 work begins the
next operation. That gives [the missing rule](#the-missing-rule-when-an-operation-is-over) a
whole-World answer. It does not settle delegated control within one operation: in the Neptunian
case, P1 can still select unrelated P1 work while P2's payment remains.

### Open questions

1. **Who runs promoted P3 work, and in which transaction.** If the operation whose completion
   released it runs it during settlement, the next turn or phase becomes part of that operation, for
   example a Player's final Pass. P3 work probably runs as its own Admin operation with its own
   recorded position. P2 work stays inside the operation it settles.
2. **Pets spelling.** How an instruction is authored as P2 or P3. The August version preferred a
   few named bands over author-chosen numbers.
3. **`Temporary`.** Each Canon `Temporary` could become its component plus a P2 task that removes
   it; `L1GiftWatcher`, which has no removal effect, would need only the removal task. Determine
   whether the `Temporary` policy, `TemporaryScope`, and the engine's idle-cleanup loop can then be
   deleted. While idle cleanup remains, it must disregard P3 tasks, or a pending P3 task stalls it.
4. **Late evaluation.** Queued work already resolves metrics and gates against the live World after
   selection ([ENGINE.md](ENGINE.md)). Confirm the same for fanout, so a turn handoff finds the next
   eligible Player as of when it runs.
5. **Manual phase control.** Phase-entry rules would create P3 tasks in sandbox tests too. Stepping
   becomes executing the pending P3 task, and jumping to another phase must discard it. Decide
   whether manual phase changes remain.
6. **Scope.** TP6 starts global. The August `Idle<Player>` design was per Player, so that one
   Player's empty queue could not flip another Player's event card. Narrow the scope only for a
   Terraforming Mars case that needs it.

### Proving order

1. The `EventCard` flip as P2 work; `SolarProbeTest` already checks that Solar Probe counts its own
   science tag.
2. `FinishTrade` as P2 work, deleting `TradeBarrier`, including L1 Trade Terminal's second barrier.
3. `TurmoilSolarOperation`, `MeasureAward`, and `FinalScoringPending`.
4. P3 phase transitions and turn handoffs, deleting `TfmWorkflow.Automatic` and
   `World.onTransactionComplete`.

Judge each step by what it deletes.

## Source evidence

- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt): task replacement,
  selection, and execution.
- [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt): controller, assignees, selection, and
  cause.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) and
  [`Effector.kt`](../../src/common/dev/martianzoo/engine/Effector.kt): automatic and queued effects.
- [`WorldTransaction.kt`](../../src/common/dev/martianzoo/engine/WorldTransaction.kt) and
  [`Engine.kt`](../../src/common/dev/martianzoo/engine/Engine.kt): settlement and cleanup.
- `TaskDelegationTest`, `PhilaresTest`, `TemporaryCleanupTest`, and the Neptunian cases in
  `FloodingTest`: current observable contracts and gaps.
