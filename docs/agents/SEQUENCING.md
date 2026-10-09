# Sequencing, delegated control, and completion

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing task order, delegated control, `THEN`, automatic effects, or cleanup.
>
> **Status:** current contracts and a verified missing completion rule. Replacement designs belong
> in [`SOLARNET_ROADMAP.md`](../../SOLARNET_ROADMAP.md) or a focused investigation.

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
calculations acceptable. The roadmap records the remaining design concern.

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
