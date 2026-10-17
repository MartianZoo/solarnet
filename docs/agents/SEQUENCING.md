# Sequencing, delegated control, and completion

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** designing turns and offers, Head Start timing, task order, delegated control,
> `THEN`, automatic effects, cleanup, task priority, or how later work waits for earlier work.
>
> **Status:** current contracts, including deferred work; a verified missing completion rule; and
> selected turn semantics. This note owns sequencing design decisions; unimplemented behavior is
> labeled separately. The [roadmap](../SOLARNET_ROADMAP.md) indexes the work.

## Current model

There is a global task pool; Actor queues are filtered views. A comma-separated group becomes
independent tasks. Stable iteration order supports reproducibility and display but carries no game
meaning.

- Effects arise after their triggering component change.
- A selected task excludes competing ordinary gameplay until that task finishes.
- `A THEN B` admits B after A's task finishes, without waiting for A's queued reactions.
- `A:: B` executes B inline before queued effects of A are evaluated.
- A component gate can make an instruction unavailable until its prerequisites hold.
- `TASK R { B }` dispatches B as R's work; `LATER { B }` and `FINALLY { B }` dispatch B as work
  that waits until no ordinary work, or no ordinary and `LATER` work, remains in the World.

Deferral is the only task priority, and there is no exclusive interaction spanning several tasks.
The turn order and every phase change are authored rules; see [Game flow](../../pets/docs/agents/GAME_FLOW.md#game-flow).
An Agent may choose among its available tasks; policy cannot supply a missing game rule.
[Deferred work](#deferred-work) owns the rules.

## Turns and turn offers

See [Canon turns and game flow](../../pets/docs/agents/GAME_FLOW.md#turns-and-turn-offers).

## Game flow

See [Canon turns and game flow](../../pets/docs/agents/GAME_FLOW.md#game-flow).

## Required promises

- **Causality:** queued effects retain their triggering change and effect context as `Cause`.
- **Self effects first:** a changed component's own automatic effects, including recursive automatic
  consequences, finish before other listeners in the same batch execute. Trigger matching and
  trigger-side conditions for the entire batch are decided before any of these effects runs.
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

Distinguish precedence, synchronous consequences, exclusive control, completion, and atomicity.

| Mechanism | Meaning |
| --- | --- |
| No extra ordering | Availability and rollback already express the rule. |
| `A: B` | A creates ordinary selectable B work. |
| `A:: B` | B restores coherence before queued player work can proceed. |
| `A THEN B` | A's task must finish before B is admitted. |
| `A: LATER { B }` | B waits until no ordinary work remains in the World. |
| `A: FINALLY { B }` | B waits until no ordinary or `LATER` work remains in the World. |
| Committed precursor | A modifier needs an event before the final result exists. |
| Specific latch | Known prerequisites must finish before later work becomes legal. |

A fixed outcome does not imply automatic execution or Admin ownership. Conversely, implementation
housekeeping should not become a Player choice merely because it currently occupies a task. Do not
repair ordering through `TfmGameplay`, policy settings, rendered-task matching, or pool order.

## What `THEN` does

Task normalization stores A as current work and B as its continuation. Shared unresolved Type
variables can keep the stages together until narrowing supplies their value. When A finishes, B
becomes ordinary pending work with no priority over siblings or A's reactions. The selected-task
lock ends with A. If A is transferred to Admin because it is a concrete System change, the engine
dispatches B back to the Actor who had the task before that transfer. Work that must also wait for
A's reactions is deferred instead; it waits for all other work in the World
([deferred work](#deferred-work)).

`EACH Player { A THEN B }` gives each branch a continuation. `EACH Player { A } THEN B` is invalid:
completing the `EACH` task admits independent branches and does not wait for them to finish.

## Automatic effects

Required component parts are constructed before gain reactions. Parts' automatic reactions
precede their owners' reactions, and construction events' automatic work precedes queued matching.
For each change, the engine materializes the matching `::` batch, executes it recursively, then
evaluates queued `:` effects. An automatic reaction never asks an Actor to select it.

Require a semantic reason for the consequence to belong to the initiating change. Strong cases
include repairing a declared count invariant before validation (Land Claim's cube replacement),
maintaining an inseparable stored relationship through corrections, and preserving actual event
history (the directed adjacency of a newly placed tile). First check whether required-part
construction, trigger-side conditions, or existing event context can express the relationship.
See [correction semantics](EX_MACHINA.md#contract): queued effects are suppressed there, but an
effect's current execution during corrections does not itself justify preserving it.

`A:: B` alongside `A: C` is not an acceptable way to express only "B before C". Nor does calling B
an event-time snapshot establish a stronger reason when the actual requirement is simply to
measure before a later authored step. Reds' bonus before chairman TR, influence before Politician
scoring, wild-tag allocation before requirements, and price/acceptance adjustments before Billing
are current sequencing dependencies to refactor, not certified reasons for `::`.

Choose scope by the actual dependency: a pricing or government-formation sequence can contain
individually valid invariant repairs without justifying an immediate wrapper around the whole
procedure. `THEN` waits for its current task, not the reactions that task triggers, so replacing an
immediate preparation signal with `Preparation THEN Consumer` does not by itself establish that
preparation has completed.

Fixed bookkeeping can use concrete System tasks without another player choice. Initial offer
counts preceding signed premise adjustments, generic requirement removals needing live narrowing,
and card-claim effects retained for their client task shape are implementation limitations, not
semantic justifications. Earlier failure of an invalid choice is also insufficient; judge completed
legal outcomes and preserve eligibility at the relevant event.

If automatic listeners depend on each other, express the dependency through resulting events
instead of batch order or retries. Use trigger-side `IF` for a condition decided by the triggering
event; put a gate in queued work when intervening changes should decide availability.

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
| `LATER` | No pending work in the World except deferred work. |
| `Owed`, `Billing` | Prerequisites represented by a particular lifecycle. |
| An Actor's empty filtered view | All currently assigned work, including possibly unrelated work. |
| A client task search | A recognized implementation pattern. |

These are not the same completion point. `Barrier` and `MustCleanUp` are checks, not locks against
intervening selection.

Neptunian Power Consultants allows P1 to hand an optional decision to P2, then resume other work
while P2's payment remains unfinished. On 2026-10-14 the owner confirmed that Flooding making that
purchase unaffordable is an allowed dead end: P1 voluntarily made their own turn impossible to
finish and must roll it back. This is not evidence that delegated work needs exclusion or a join.
`FloodingTest` covers these orders and rollback;
[`PAYMENTS.md`](PAYMENTS.md#interleaving-and-rollback) describes the payment-specific evidence.

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
calculations acceptable. The [turn design](../../pets/docs/agents/GAME_FLOW.md#turns-and-turn-offers) records the intended enclosing
lifecycle; [operation completion](#operation-completion) states the whole-World rule that ends an
operation.

## Cleanup

See [Canon game flow](../../pets/docs/agents/GAME_FLOW.md#cleanup).

## Deferred work

`LATER` and `FINALLY` ([language rules L2-18 and L2-19](../../pets/docs/pets-language-spec.md#2-instructions))
are the task priorities. Like `TASK` (L2-17), each dispatches its body as separate work, but that
work waits: `LATER` work until no ordinary work remains in the World, and `FINALLY` work until no
ordinary or `LATER` work remains. An August 2026 priority
design (commit `b074f54dd`) was replaced within days by `Idle<Player>` and `Yield<Player>` signal
designs, then by whole-World `Temporary` cleanup. Each replacement moved deferred work back into
components. Do not reintroduce a signal, marker, or continuation component for deferred work
without the owner's direction.

### Facts in components, future work in tasks

See [Canon game flow](../../pets/docs/agents/GAME_FLOW.md#facts-in-components-future-work-in-tasks).

### Engine obligations

The language rules state what deferral means. A task's deferral records which dispatch admitted it:
`TASK`, `LATER`, or `FINALLY`. A selected task is always ordinary. Beyond those rules:

- The engine performs a dispatch as soon as the work holding it is ordinary, as it releases a
  `THEN` continuation: the body is admitted with the dispatch's deferral, and no task ever consists
  of a dispatch alone. A dispatch in deferred work is performed when that work is promoted, before
  any later deferral could be. Only promotion changes a task's deferral afterward; authored Pets
  cannot defer or promote another task.
- Deferral restricts selection only. It applies equally to explicit commands and autoexecution, and
  it does not order tasks with the same deferral: legal sibling orders stay available.
- Promotion edits the task's deferral. It rolls back with its transaction and is reproduced from
  the event history.

Playing Solar Probe creates its effects as ordinary work and its flip as `LATER` work. Its own
science tag therefore counts for every consequence of its effects, and it reaches the played-event
pile before the game offers another turn.

L2-18 promotes a whole batch at once. The alternative leaves deferral fixed and makes deferred work
selectable only while no ordinary work exists. The two differ only when a promoted task creates
ordinary work while others from its batch still wait: under L2-18 those others stay selectable
beside the new work; under the alternative they wait for it. No Terraforming Mars case is known to
distinguish them. Batch promotion is preferred because "a deferred batch becomes ordinary work" is
the simpler statement.

Deferral is neither `THEN` nor a gate. `A THEN B` admits B when A's own task finishes, without
waiting for A's reactions; deferred work waits for all ordinary work in the World. `Owed` and
`Required` remain components because they record quantities, and their survival still makes an
operation incomplete.

### Why `FINALLY`

A `LATER` nested in another defers its body again when it runs. That body then waits for the
ordinary work pending at that moment, but not for work deferred by the same batch: if a trade
completion deferred more work, that work and the inner body would become ordinary together. Making
a nested `LATER` mean "later than later" would need a depth rule. `FINALLY` states the ordering
directly: final work follows every consequence of the work before it, including work that `LATER`
work defers. It exists for turn and phase handoffs.

### Operation completion

`ActorEngine.requireComplete` currently accepts an operation when no new task remains, or when
World `FINALLY` work has begun since the operation's last decision. The first final promotion
occurs after all preceding ordinary and `LATER` work, and the cleanup-component check uses that
same point in the event log. Agents record each operation's beginning and decisions; automatic
execution is not a decision.

This check precedes `ActionScope` removal, which runs as final work. Canon's
[scope-removal rule](../../pets/docs/agents/GAME_FLOW.md#scope-removal-must-not-require-choices)
requires every choice belonging to the action to finish while its scope exists. Removal and
dependent expiry may perform fixed effects and cleanup and advance to subsequent play;
they must not generate further choices for the completed action. This is an authoring restriction,
not a new engine completion rule. Do not mark the finally-removed scope `MustCleanUp`, since that
check runs before removal.

Completion still does not exclude intervening work: P1 may resolve Flooding's loss while P2's
Neptunian payment remains. If that choice makes the turn impossible to finish, P1 must roll back
the turn. See [the interaction discussion](#the-missing-rule-when-an-operation-is-over).

### Decisions and open questions

- **Dispatch is not a step.** On 2026-10-12 the owner ruled that a pure dispatch should never sit in
  a queue as current work; the engine performs it, as it releases a `THEN` continuation. This
  replaced the earlier rule that each dispatch was a task of its own.
- **`FINALLY` for game flow.** On 2026-10-11 the owner ruled out a band below `LATER`. On
  2026-10-12, once nested `LATER` proved to need either a depth rule or a lowest band, he chose
  `FINALLY`, so that turns and phases can advance through authored rules. Keeping
  Terraforming Mars-specific Kotlin such as `TfmWorkflow` was out of the question; it is deleted.
- **Present components answer.** On 2026-10-13 the owner asked that active behavior come from
  components that are present rather than from conditions: an effect that relies on a player not
  having passed belongs on `InRound`, so its presence says so. The baton itself does not decide
  which action to offer: `InRound` or the phase answers, while `Pass` releases it.
- **Game flow.** On 2026-10-12 the owner ruled that the Pets model must never be shaped by turning
  the flow on or off, that tests be either clear sandbox or clear gameplay scenarios, and that
  modules contribute phase order without a central orchestrator. He proposed `NoPreludes` so that
  exactly one module owns each handoff out of the corporation phase.
- **Exclusion is not deferral.** Deferral orders work; it cannot keep other work out while an
  interaction is under way. The Flooding–Neptunian dead ends above are allowed and do not require
  exclusion or a join.
- **Scope.** Deferral is global (L2-18). The August `Idle<Player>` design was per Player, so that
  one Player's empty queue could not flip another Player's event card. Narrow the scope only for a
  Terraforming Mars case that needs it.

### Evidence

- `DeferredWorkTest`: selection, promotion batches, every Actor's work, late evaluation of metrics
  and fanouts, groups, nesting, final work, and rollback.
- `Lang02InstructionsTest`: L2-18 and L2-19 syntax; `TaskDispatchTest`: dispatch recipients.
- `SolarProbeTest`, `TradeEnvoysTest`, `L1TradeTerminalTest`, `SolarPhaseOrderingTest`,
  `AwardsTest`, `VictoryTest`, `WildTagTest`, and `WildTagContinuationTest`: the converted devices.
- `SolarPhaseOrderingTest` reaches solar phases through gameplay and observes ocean removal,
  TR brackets, influence payouts, ruling bonuses, delegate availability, dominance ties, optional
  payment affordability, and next-generation corporation eligibility. It does not assert execution
  traces or a complete prescribed sequence. Forecast-information timing remains untested without
  a hidden global-event deck.

## Source evidence

- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt): task replacement,
  selection, and execution.
- [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt): assignee, selection, deferral, and
  cause.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) and
  [`Effector.kt`](../../src/common/dev/martianzoo/engine/Effector.kt): automatic and queued effects.
- [`WorldTransaction.kt`](../../src/common/dev/martianzoo/engine/WorldTransaction.kt) and
  [`Engine.kt`](../../src/common/dev/martianzoo/engine/Engine.kt): settlement.
- [`TaskQueues.kt`](../../src/common/dev/martianzoo/engine/TaskQueues.kt): admission and
  promotion of deferred work.
- `TaskDispatchTest`, `PhilaresTest`, `DeferredWorkTest`, and the Neptunian cases in
  `FloodingTest`: current observable contracts, including allowed dead ends and rollback.
