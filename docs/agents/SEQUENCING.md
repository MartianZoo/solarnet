# Sequencing, delegated control, and completion

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** designing turns and offers, Head Start timing, task order, delegated control,
> `THEN`, automatic effects, cleanup, task priority, or how later work waits for earlier work.
>
> **Status:** current contracts, including deferred work; a verified missing completion rule; and
> selected turn semantics. This note owns sequencing design decisions; unimplemented behavior is
> labeled separately. The [roadmap](../../SOLARNET_ROADMAP.md) indexes the work.

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
The turn order and every phase change are authored rules; see [Game flow](#game-flow).
An Agent may choose among its available tasks; policy cannot supply a missing game rule.
[Deferred work](#deferred-work) owns the rules.

## Turns and turn offers

**Selected terminology and intended semantics, not implemented.** Use **turn** for the complete
envelope of a player's accepted opportunity and its resolution. Consecutive turns may belong to
the same player. Do not call this an action scope, move, or turn segment. This deliberately differs
from the published game's terminology: its action-phase turn of up to two actions becomes up to
two consecutive turns here. Preserve when play advances to the next player; the terminology change
does not grant additional opportunities. Tests accept the difference: `TfmGameplay.turn { }`
groups a published turn of one or two actions, which can be two turns in this sense.

### Offering, taking, and finishing a turn

- In the action phase, first offer the player a turn. Declining that first offer is passing for
  the generation: the player never took a turn and receives no turn scope.
- Accepting the offer starts the turn. Finish its work and consequences before offering the second
  turn. Declining the second offer creates no turn and has no penalty or pass status; play simply
  advances. Keep this distinction even if both offers use shared machinery.
- A turn lasts through payment, effects, placement bonuses, other players' responses, and resulting
  work and cleanup. Completion of the initiating task alone does not finish the turn.
- This first/second offer rule describes ordinary action-phase rotation. It does not make
  corporation plays, Prelude plays, or Head Start's mandatory grants optional. Keep offering the
  last unpassed player a fresh first offer after each turn, without inventing additional turn
  groupings.

The intended units are:

| Activity | Turn scope |
| --- | --- |
| Initial corporation play and its resolution | A turn |
| Each Prelude played during the Prelude phase | A separate turn |
| Each accepted action-phase opportunity | A separate turn, including the optional second |
| Each final greenery placement and its resolution | A separate turn |
| Head Start under the variant below | Its Prelude turn, followed by two additional turns |

A card played or an ability invoked while resolving an existing turn does not itself start another
turn. For example, Merger's corporation play remains inside the turn that played Merger. An
explicit grant of a further turn is different from a consequence that requires another decision.

The proposed `OnTurn` fact, task routing, and decisions outside turns are described in
[Identity: who is on-turn, and who decides](IDENTITY.md#who-is-on-turn-and-who-decides).

### Uses of the scope

Wild-tag allocations should belong to the accepted turn and expire when it finishes. A consecutive
turn gets fresh allocations, even for the same player; another player's response does not get a
new allocation scope. Full wild-tag assignment is still unimplemented.

Suitable Infrastructure is a likely consumer of the same scope, but its exact eligibility and
reset rules remain to be confirmed. Existing `SuitableInfrastructureTest` scenarios show separate
rewards for the two action-phase actions and a single reward for a Prelude raising multiple
production tracks. `HeadStartTest` also expects separate rewards for Head Start's two granted
actions. These examples support the direction without establishing every interaction, especially
out-of-turn production changes.

### Head Start: acceptable variant to pursue

The owner is willing to change Head Start's timing to avoid requiring nested turns:

1. Resolve its basic instructions, including steel, money, and their consequences, in its Prelude
   turn. Finish that turn with two additional turns owed to its owner.
2. Grant and completely resolve each owed turn sequentially. Each is mandatory and grants a single
   action-phase action, not the normal first/optional-second pair or an option to pass.
3. Resume the normal Prelude sequence from where it stopped.

Two tokens could record the owed turns. Their names and implementation are undecided; express the
entitlement and its consumption through ordinary rules, without teaching Kotlin about Head Start.
This is explicit permission to explore those entitlement components, not a general return to
components used only to schedule arbitrary continuations.

This is a deliberate variant, not a claim of exact published timing. Money and steel, and all
consequences of the basic instructions, must resolve before either extra turn. In particular, the
extra turns cannot sell cards before Head Start calculates its money from cards in hand. The
current fake card allows that ordering and also allows its two actions to interleave; see
[`HeadStartTest`](../../test/common/dev/martianzoo/tfm/tests/cards/HeadStartTest.kt).

The working direction is sequential turns, with no suspended parent turn or allocations to
restore. Head Start should not justify nested scopes by itself. This does not settle which work
may interleave with another player's response or payment within a turn.

### Remaining design work

- Turn completion is authored now (`TurnOver`, under [Game flow](#game-flow)), but `NewTurn` is
  still a signal, not a lasting turn scope. Define acceptance and a turn scope through authored
  rules and settlement.
- Keep turn identity separate from generic ability invocation: `UseAction` currently represents
  both normal actions and reactive offers. It must not automatically create a turn.
- Reduce overloaded action names when changing the relevant APIs. `ActionSlot` / `Action1` /
  `Action2` select declared capabilities, not the first and second turns. `Activate`, `Activatable`,
  and `AbilitySlot` were candidate replacements, not selected names; no blanket rename is agreed.
- Verify wild-tag allocation timing, Suitable Infrastructure, extra-turn ordering, and completion
  through cross-player responses before treating the model as implemented. Do not add nesting or
  a second scope representation without a concrete case that requires it.

## Game flow

The game's own rules carry play from `SetupPhase` to `VictoryPhase`. A client only starts the game
(`TfmGameplay.startGame`, which performs `SetupPhase FROM BootstrapPhase`). Nothing turns the
flow on or off.

- **Phases without turns hand off when their work is done.** `SetupPhase`, `ProductionPhase`,
  `SolarPhase`, `ResearchPhase`, and `End` each dispatch `FINALLY { Next FROM This }` as they begin.
  Production hands off to `SolarPhase` while a `GameEndBarrier` exists, and otherwise signals
  `CheckGameEnd`.
- **Phases with turns hand off from their last turn.** A turn lasts through its `LATER` cleanup, so
  its end is `FINALLY` work, and all pending `FINALLY` work becomes ordinary together. A handoff
  dispatched as the Corporation, Prelude, Action, or final greenery phase began would therefore run
  after its first turn; these phases instead say what follows each turn.
- **Selection cleanup.** Each `NewTurn` schedules Admin's `LATER` removal of any card backs still in
  that Player's `Selecting`. It runs after the turn's choices, including purchase payment and
  transfer, and before `TurnOver`. ResearchPhase schedules analogous
  project-card cleanup for all Players before its `FINALLY` handoff.
- **Turn order.** `TakeTurn<P>` only invites `P` to take a turn; whatever governs that player's
  turns answers it. The corporation, Prelude, and final greenery phases give `NewTurn<P>` and
  dispatch `FINALLY { TurnOver<P> }`, and each says what follows `TurnOver`. In the action phase
  the player's `RoundStatus` answers instead. `NextPlayer<P>` passes play to the next seat through
  `AfterMe`. Outside the action phase, reaching the start player again is `RoundOver`; with one
  player, `NextPlayer` is always `RoundOver`. These signals take a Player argument rather than an
  owner, so their work stays with Admin, as it must for Players who do not autoexecute.
- **Round status drives the action phase.** Each player has at most one `RoundStatus`: `ImAwake`
  while they may still act in the action phase, or `Pass` once they have passed. The action phase
  wakes every player who has not passed. `ImAwake` gives its owner each turn and supplies the turn's
  choices; when the turn ends it offers a second action and then `NextPlayer` while another player
  is awake, and otherwise another turn. `Pass` replaces its owner's `ImAwake` and passes play on,
  both when the passing turn ends and whenever that player's turn comes round again. Production
  begins at the first `TurnOver` with nobody awake. Play goes around the table without `RoundOver`.
- **Modules own phase order.** The corporation phase ends in `RoundOver`. `NoPreludes`, selected in
  every game without Prelude, then begins the first `ActionPhase`; `PreludeExpansion` begins
  `PreludePhase` instead, whose own `RoundOver` begins the action phase. Expansions add their solar
  work as triggers on the one `SolarPhase`, and Turmoil's waits under `LATER` for the rest.
- **A turn given outside turn order has no continuation.** `NewTurn` alone is just a turn; only
  `TakeTurn` attaches what follows. Sandbox scenarios and the REPL's manual modes take turns this
  way. They leave the flow by dropping its pending tasks once, and then place phases ex machina
  (`TfmGameplay.phase`). Placing a phase runs its automatic effects but not its queued rules, so a
  placed action phase wakes every player without giving anyone a turn.
- **Declining another final greenery is `Done<Phase>`.** A signal would vanish at once, before
  `TurnOver` could read it, so `Done` instead lasts until its phase ends.

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
calculations acceptable. The [turn design](#turns-and-turn-offers) records the intended enclosing
lifecycle; [operation completion](#operation-completion) states the whole-World rule that ends an
operation.

## Cleanup

| Class | Current meaning |
| --- | --- |
| `MustCleanUp` | State forbidden by operations that declare themselves complete. |
| `Barrier` | `MustCleanUp` state removed by its owning game rule. |
| `Signal` | An unscoped point event that removes itself automatically. |

Nothing is removed merely because the World is idle. A component whose end is future work
schedules that work as a deferred task: an event card's move to `PlayedEvent`, `L1GiftWatcher`'s
removal, and `FakeWildTagUse`'s removal after its owner's next action. The owner performs each such
task, so a test whose Players do not autoexecute performs it explicitly.

## Deferred work

`LATER` and `FINALLY` ([language rules L2-18 and L2-19](../pets-language-spec.md#2-instructions))
are the task priorities. Like `TASK` (L2-17), each dispatches its body as separate work, but that
work waits: `LATER` work until no ordinary work remains in the World, and `FINALLY` work until no
ordinary or `LATER` work remains. An August 2026 priority
design (commit `b074f54dd`) was replaced within days by `Idle<Player>` and `Yield<Player>` signal
designs, then by whole-World `Temporary` cleanup. Each replacement moved deferred work back into
components. Do not reintroduce a signal, marker, or continuation component for deferred work
without the owner's direction.

### Facts in components, future work in tasks

The component graph records what is, or what is happening. Work that should happen later belongs in
a task. A component that exists so that its removal can start later work is a scheduling device, not
a game fact; do not add new ones. The `workflow` branch's `Continuation` class makes that device
generic.

The owner's proposed Head Start entitlement tokens are a specific design direction recorded in
the [turn design](#head-start-acceptable-variant-to-pursue). They describe
turns owed to a player; evaluate them with that game meaning rather than rejecting them solely
because turn order will consume them. Their implementation remains open.

Devices replaced by deferral:

| Former device | Deferred work now |
| --- | --- |
| `EventCard` as `Temporary` | `This: LATER { PlayedEvent<Class<This>> FROM This* }`, performed by the owner |
| `TradeBarrier` | `Trade`'s `This: LATER { FinishTrade<@ColonyTile> }` |
| `TurmoilSolarOperation` | `SolarPhase: LATER { TurmoilPhase }`; `TurmoilPhase: ResolveGlobalEvent, LATER { FormGovernment THEN ChangingTimes }` |
| `L1GiftWatcher` as `Temporary` | `LATER { -This }` beside the gift fanout |
| `FakeWildTagUse` as `Temporary` | `StandardAction: LATER { -This* }` and `PlayPrelude: LATER { -This* }` |

`FinalScoringPending` and the `Temporary` `MeasureAward` were replaced by a phase instead: `End`
scores, and the terminal `VictoryPhase` names the winner after that work has finished. With these
conversions `Temporary`, `TemporaryScope`, and the engine's idle cleanup were deleted.

Where the component also records a real fact, keep it and change only the timing. A played
`EventCard` is in play, and its tags count, until it is flipped; only the flip is deferred work.

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

An operation's work is over when no new task remains, or when the World's `FINALLY` work has begun
since the operation's last decision. `FINALLY` work begins only once no ordinary or `LATER` work
remains (L2-19), so by then everything the operation started, including the cleanup it deferred,
is finished. The turn handoffs and phase changes that follow, and the offers they create, belong to
whatever comes next. `ActorEngine.requireComplete` judges cleanup components at that same point,
from the event log; the Agents record when each operation begins and decides, and automatic
execution is not a decision. That gives [the missing
rule](#the-missing-rule-when-an-operation-is-over) a whole-World answer. It does not exclude
intervening work: P1 may resolve Flooding's loss while P2's Neptunian payment remains. If that
choice makes the turn impossible to finish, P1 must roll back the turn.

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
  having passed belongs on `ImAwake`, so its presence says so. `TakeTurn` therefore has no rule of
  its own.
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
- `SolarProbeTest`, `TradeEnvoysTest`, `L1TradeTerminalTest`, `TurmoilSolarPhaseTest`,
  `AwardsTest`, `VictoryTest`, and `FakeWildTagTest`: the converted devices.

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
