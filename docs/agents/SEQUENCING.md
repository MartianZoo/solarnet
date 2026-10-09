# Sequencing, delegated control, and completion

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** deciding task order, delegated control, `THEN`, automatic effects, or when queued
> work and its cleanup have finished.
>
> **Status:** current contracts followed by open design options. Nested priorities and exclusive
> operation scopes are alternatives under evaluation; neither is implemented or selected.
> [ENGINE.md](ENGINE.md) describes execution and the
> [Pets language specification](../pets-language-spec.md) owns authored semantics.

## The model

There is a global task pool; Actor queues are filtered views. A comma-separated group becomes
independent tasks. Stable iteration order is for reproducibility and display, not game meaning.
Current order comes from these facts:

- Effects arise after their triggering component change.
- A selected task excludes competing ordinary gameplay until that task finishes.
- `A THEN B` admits B after A's task finishes, without waiting for A's queued reactions.
- `A:: B` executes B inline before queued effects of A are evaluated.
- A component gate can make an instruction unavailable until its prerequisites hold.

There is currently no general task priority or exclusive operation spanning several tasks. An Agent
can choose among its available tasks; its policy cannot establish a missing game rule.

## The promises

- **Causality:** queued effects retain their triggering change and effect context as `Cause`.
- **Automatic coherence:** required parts exist before gain reactions; recursive automatic
  consequences run before queued effects of the originating change are evaluated.
- **Count invariants:** declared bounds hold after an initiating change and its recursive automatic
  consequences. This is the operation meant by
  [count validation](QUANTIFIERS.md#invariants-at-operation-completion).
- **Trigger snapshot:** `Effector.fire` materializes an automatic listener batch against the same
  post-change World before executing it.
- **Failure atomicity:** failure restores components, tasks, history, and derived indexes to the
  enclosing transaction's checkpoint.
- **Selection integrity:** at most a single task is selected across the World. Authored Pets cannot
  edit, cancel, or reprioritize another task.

Legal sibling orders should remain available even when they produce different outcomes. Independent
automatic listeners must not acquire meaning from registration or diagnostic sort order. A
component's own automatic effects retain their authored declaration order. These are review
obligations; the current scenario tests do not prove them for every composition.

## Before adding order

Identify the forbidden result or intervention. A choice that fails within the same transaction can
often be rejected by ordinary Pets and rollback, without prechecking every path. That argument does
not cover an interaction spread across committed commands: rejecting a later payment step does not
undo an earlier committed partial payment.

Distinguish precedence, immediate consequences, exclusive control, completion, and failure
atomicity. `THEN` supplies precedence, `::` supplies inline execution, and a transaction supplies
rollback. None alone supplies exclusive control over a family of queued tasks.

| Mechanism | Appropriate meaning |
| --- | --- |
| No extra ordering | Ordinary availability and rollback already express the rule. |
| `A: B` | A creates ordinary selectable B work. |
| `A:: B` | B restores coherence before queued player work can proceed. |
| `A THEN B` | This instruction owns both stages; only A's task must finish first. |
| Committed precursor | A modifier needs an event before the final result exists. |
| Specific latch | Known prerequisites must finish before later work becomes legal. |

A Player may legitimately choose when to take a fixed consequence. Fixed outcome does not imply
automatic execution or Admin ownership. Conversely, implementation housekeeping should not become
a Player chore merely because it currently occupies a task. Admin routing is a separate open topic
in [TASK_ROUTING_EXPERIMENT.md](TASK_ROUTING_EXPERIMENT.md).

Do not repair ordering through `TfmGameplay`, policy settings, rendered-task matching, or incidental
queue order. Evaluate both the forbidden intervention and the sibling choices that must remain
legal.

## What `THEN` does

Task normalization normally stores A as current work and B as its continuation. Shared unresolved
Type variables can keep stages together until narrowing supplies their value. When A finishes, B
becomes ordinary pending work with no priority over siblings or A's reactions. The selected-task
lock ends with A, even if B or other consequences remain.

Pluto combines `THEN` with a `PlutoLock` limited to a single copy per owner. Each bonus acquires the
lock before drawing and releases it after discarding. `ColoniesRulesTest` covers the blocked overlapping
draw and successful completion. This is a specific prerequisite, not general task-family control.

`EACH Player { A THEN B }` gives each branch a continuation. `EACH Player { A } THEN B` does not join
the branches or wait for their descendants; see [EACH.md](EACH.md#sequencing).

## Automatic effects

Required component parts are constructed before gain reactions. Parts' automatic reactions precede
their owners' reactions, and construction events' automatic work precedes queued matching.
For each change, the engine materializes the matching `::` batch, executes it recursively, then
evaluates queued `:` effects. An automatic reaction never asks an Actor to select it.

Use `::` for determined consequences whose delay would expose incoherent state. If automatic
listeners depend on each other, express that through resulting events rather than batch order or
retries. Use trigger-side `IF` for conditions decided by the triggering event; put a gate in queued
work when intervening changes should decide availability. `A: (R: B) OR Ok` also makes B declinable.

The [Pets tricks guide](../pets-tricks-hacks-cheats.md) explains how an automatic `Die` can reject a
forbidden component change and roll it back. It cannot generally police task selection: selecting
or narrowing an abstract task can change task state without producing a component event. A blanket
`BY` guard also confuses the attributed performer with the Actor issuing a task command.

## Committed precursors

`PayingFor<Class<Component>>` lets modifiers react before a purchase or card play produces its final
component. Such a precursor should represent commitment to that result, carry the needed context,
and precede settlement. Ordinary reactions still belong on the final event. An unrelated signal
sent early merely to wake listeners would give the model another fact to keep consistent.

## The missing rule: when an operation is over

The engine can represent components, tasks, and causal events, but has no general completion rule
for a particular interaction that spans queued work.

| Existing mechanism | What it measures |
| --- | --- |
| `THEN` | Completion of its current task. |
| `Temporary` | Emptiness of the entire task pool. |
| `Owed`, `Billing`, `TradeBarrier` | The prerequisites represented by that lifecycle. |
| An Actor's empty queue | All currently assigned work, possibly including unrelated work. |
| A client task search | A recognized implementation pattern. |

These facts do not identify the same completion point. In particular, `Barrier` and `MustCleanUp`
are completion checks, not locks against intervening task selection.

## Delegated operations and scheduling options

### The concrete problem

P2 has Neptunian Power Consultants; P1 plays Flooding and places an ocean. P1 should receive the
option as orderable work whose decision belongs to P2, as with Philares. P1 chooses when to hand it
over. P2 may decline, or accept and retain control throughout the resulting payment. P1 must remain
unable to resume unrelated work between P2's decisions.

The current offer is `UseAction<NeptunianOption<...>>?`; `NeptunianOption` currently names the
card-bound action provider. A future owned request could use a different representation. No rename
or new request Class is required by this discussion.

Today the selected optional task delegates to P2, but completing it releases the lock. Splits,
`THEN` continuations, and queued reactions retain P1 as controller. Paying 2 Steel toward the 5 M€
cost leaves a cash decision while P1 again controls the outstanding work. Existing payment helpers
select through other Actors and conceal this gap; see [PAYMENTS.md](PAYMENTS.md). The generic
`TaskDelegationTest` explicitly expects follow-up work to return to the original controller, so
extending delegation changes current semantics.

Two distinct requirements follow: descendants must receive the appropriate controller, and
unrelated work must remain ineligible. Changing only assignment leaves P1 free to intervene.
Keeping an arbitrary payment child selected instead would take payment-order choices away from P2.

### Option A: nested priority groups

Use selection for the current task and an active group for the work P2 may order. Handoff suspends
the caller's remaining work; newly produced work inherits the active group and its controller.
P2 may choose among eligible siblings while no task is selected. Returning control exposes the
nearest suspended group. A further handoff can nest inside this interaction.

The proposed visible categories are p0 selected, p1 selectable, and p2 blocked. Three flat global
ranks do not preserve enough information: if Flooding's flip is already p2, moving P1's unfinished
effects to p2 during payment makes both promote together afterward. Distinct suspended layers must
survive. Tasks can become eligible together within the next layer, without releasing every blocked
task in the World.

Relative ranks may be enough for strictly nested handoffs whose descendants all stay in the active
layer. Deferred cleanup adds a harder question: if cleanup belongs to the delegated operation, it
must finish before P1 resumes and cannot share a rank with suspended P1 tasks. The model needs a
rule for creating and reusing an inner deferred group. Numeric ranks, structured priorities, or
scheduling frames are possible representations; their cost and equivalence remain to be evaluated.

This option could unify delegated choices, delayed event removal, and eventual Admin progression.
It also imposes nested scheduling and changes how eligibility is represented. Ordinary same-owner
siblings and `THEN` chains should retain their existing freedom unless a game rule requires a new
constraint; selecting every task need not start an exclusive group.

### Option B: exclusive operation scope

Retain the ordinary scheduler and restrict eligibility to work belonging to the active delegated
operation. Descendants inherit its control, and its completion releases the caller. Nested
delegation must preserve the enclosing operation. This directly expresses the payment requirement
without selecting a global deferred-work scheme, but it needs membership and completion semantics.

Causal ancestry is a candidate source for membership, not an already selected implementation.
Accepting an owned request or `UseAction` creates a fresh change event that can identify its
consequences. Existing `Cause` links follow triggering change events; they do not identify every
executed task. Siblings can share a cause, and task-only splitting or continuation needs separate
consideration. Matching equal `Cause` values is not an operation-completion test.

Pets components are multisets of Types, without fields or instance identity. A live component used
as a scope needs an unambiguous lifetime; equal copies or removal and reacquisition cannot silently
be treated as distinct object instances. An event root may avoid some identity costs, but the
engine still needs to know which operation currently excludes unrelated work.

This option may be narrower for delegation. It does not automatically provide the deferred event
cleanup and Admin progression that motivated global priorities. Evaluate aggregate conceptual cost,
including machinery that remains outside the scope mechanism.

### Questions shared by both options

- **What starts exclusive control?** A delegated decision, an authored operation, or another
  explicit fact? A concrete consequence credited to P2 can still correctly be ordered by P1.
  Do not turn every different `actor` into a handoff or use instruction-side `BY` as assignment.
- **What finishes it?** Zero debt is insufficient in the current payment model: unused offers and
  queued rewards can remain. Covering the accepted option through its consequences is a plausible
  rule, but the extent of rewards and further reactions remains to be settled.
- **What if it cannot finish?** Empty task work does not prove successful settlement either:
  declining optional payment tasks can leave unpaid `Owed` or `Billing`. Which obligations must
  clear before control returns, and what happens when no remaining choice can clear them? A failed
  later command cannot undo earlier committed choices. Affordability, dead ends, and any proposed
  cancellation must have an explicit account rather than assuming every accepted group drains.
- **What inherits it?** Splits, continuations, automatically produced queued work, and later
  reactions all matter. Admit the complete consequence batch before testing for completion; a
  transient empty queue must not release the caller.
- **What nesting is legal?** Another Player's decision must not accidentally release an outer
  interaction. Attribution, lexical ownership, the on-turn Player, and temporary control remain
  distinct facts.
- **What does deferred completion mean?** Event cleanup, returning to a caller, and giving Admin
  the next phase may require different points. A single blocked category does not decide their
  order. Scope-based completion also needs an explicit account of these points.
- **What can be removed?** Prefer a design that eliminates client task searches and overlapping
  lifecycle machinery. Do not add a second task representation solely to keep a parent selected.

Engine legality must hold with autoexecution disabled and through ordinary task commands, not only
through a payment convenience method. Useful distinguishing scenarios include affordable decline,
mixed payment across separate commands, attempted P1 intervention between payments, nested
delegation, preserved sibling choices, and event cleanup before Admin advances. These are design
probes, not a prescribed implementation sequence.

## Cleanup vocabulary

| Class | Current meaning |
| --- | --- |
| `MustCleanUp` | State forbidden by operations that declare themselves complete. |
| `Barrier` | `MustCleanUp` state removed by its owning game rule. |
| `Signal` | An unscoped point event that removes itself automatically. |
| `Temporary` | State the engine removes only when the whole task pool is empty. |
| `Continuation` | State removed at global idleness after the initiating operation validates, allowing follow-up work. |
| `TemporaryScope<Parent>` | A parent-dependent `Scope` that is both `Temporary` and `MustCleanUp`. |

Plain `Temporary` is not `MustCleanUp`: unrelated tasks can keep it alive across a narrower manual
operation. `TemporaryScope` combines a dependency, a cleanup policy, and an invariant; its name
does not give it local task completion.

### Current behavior: whole-World idle cleanup

At the outer transaction boundary, policy settlement runs first. If every task queue is empty, the
engine removes all instances of one concrete `Temporary` Type that has no direct or indirect
dependent matching `Temporary` or `MustCleanUp`. It then settles work caused by that removal and
rechecks the live queues and dependencies before attempting another Type. The initiating operation
then validates, and the engine removes eligible `Continuation` components, settling policy and
Temporary cleanup after each removal. It records the final position after this work; nested calls
share the outer transaction and create no intermediate recorded positions. Every step remains
inside rollback.

Event cards currently become `PlayedEvent` through that idle cleanup, not an explicit end-of-turn
task. Parking deferred cleanup or Admin tasks in the pool would prevent the existing emptiness test
from succeeding. A priority design must account for that interaction; priorities alone do not
replace temporary-state retirement. [WORKFLOW.md](WORKFLOW.md) owns phase progression.

## Source evidence

- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt): `replace1WithN`,
  `handleTask`, and `executeSelectedTask` preserve the original controller today.
- [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt): selection, derived assignee, and cause.
- [`GameEvent.kt`](../../src/common/dev/martianzoo/state/GameEvent.kt): `Cause` links change events.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) and
  [`Effector.kt`](../../src/common/dev/martianzoo/engine/Effector.kt): automatic and queued effects.
- [`WorldTransaction.kt`](../../src/common/dev/martianzoo/engine/WorldTransaction.kt),
  [`Engine.kt`](../../src/common/dev/martianzoo/engine/Engine.kt), and
  [`SystemDeclarations.kt`](../../src/common/dev/martianzoo/pets/SystemDeclarations.kt): settlement,
  temporary cleanup, and built-in Classes.
- [`TaskDelegationTest.kt`](../../test/common/dev/martianzoo/engine/TaskDelegationTest.kt),
  [`PhilaresTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/PhilaresTest.kt), and
  [`TemporaryCleanupTest.kt`](../../test/common/dev/martianzoo/engine/TemporaryCleanupTest.kt):
  current delegation and cleanup behavior.
- [`BugsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/BugsTest.kt): search
  `Flooding incorrectly` for passing characterizations of P1 resuming after P2 accepts and
  interrupting a partial payment. Both disable autoexecution and use separate Player commands;
  the latter leaves P2 with spent Steel, no cash, and unpaid debt after a failed payment attempt.
