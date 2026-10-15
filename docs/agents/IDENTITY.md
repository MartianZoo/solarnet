# Context, assignment, and Actor identity

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing on-turn identity, context specialization, event Actor attribution, task
> assignment, `BY`, Admin, delegated narrowing, or Philares. Lexical ownership is in Pets.
>
> **Status:** current semantics plus the proposed on-turn fact and intended routing in their own
> section. Currently a handoff lasts for the selected task, not its later payment or other queued
> consequences.

## Source map

- [`Actor.kt`](../../src/common/dev/martianzoo/state/Actor.kt): operation identities.
- [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt): current `assignee` and `selected`.
- [`PendingTask.kt`](../../src/common/dev/martianzoo/engine/PendingTask.kt): trigger-time routing;
  search for `fromEffect`.
- [`PetElaborator.kt`](../../pets/src/common/dev/martianzoo/pets/PetElaborator.kt): lexical owner
  insertion; search for `insertOwnedContext`.
- [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt): the Actor filter on
  unowned triggers.

## Identity roles

Keep these roles independent:

- **Context component:** the concrete component whose declaration supplied an Effect,
  Instruction, Requirement, or Metric.
- **Context owner:** that component's owner, or the Player scope through which an ad hoc
  instruction entered the engine.
- **Current assignee:** the Actor through whose Agent the task may now be selected, narrowed, or
  executed.
- **Executing Actor:** the Actor attributed to the concrete change and recorded on its
  `ChangeEvent`.

A Task stores only its current `assignee` as an Actor identity. That Actor supplies remaining
choices and performs the concrete instruction. `TASK Actor { body }` explicitly admits new work to
another Actor; `BY Actor` hands concrete execution to its named Actor.

## Who is on-turn, and who decides

**Proposed representation and intended routing, not implemented.** The
[turn lifecycle](../../pets/docs/agents/GAME_FLOW.md#turns-and-turn-offers) defines when a player is on-turn.
The owner is open to recording the current player in the component graph with:

```pets
CLASS OnTurn<Player> : System { HAS MAX 1 OnTurn }
```

This is a proposed game fact, not an implemented declaration. The maximum applies across players.
It exists during an accepted turn, remains through that turn's resolution, and is absent when no
turn is in progress, including while an ordinary turn offer is awaiting acceptance. An offer's
recipient must therefore be identifiable without `OnTurn`. Avoid maintaining a second independent
source of truth for the current player. Whether this fact also serves as the Pets lifetime anchor
for turn-scoped components remains to be designed.

During a turn, generated non-System/non-Admin tasks go first to the on-turn player for ordering.
Selecting work can hand the actual decision to another player. This does not change who is
on-turn, the effect's lexical owner, or the Actor who executes the resulting change. System/Admin
work still retains the context needed to route its downstream player choices correctly.

Initial-research discards and Supercapacitors' production choice need no turn of their own. Icy
Impactors, Philares, Neptunian Power Consultants, St. Joseph of Cupertino Mission, and the colony
responses likewise do not grant their recipients turns: they resolve within the existing turn,
or outside all turns when none is active. Pristar's production effect illustrates that doing work
for a player does not even require a decision, much less an on-turn player. Outside turns, retain
the appropriate recipients rather than inventing an on-turn player.

## Admin and engine

`Admin` is the non-Player Actor and Component that performs neutral table activity. Admin may
control, receive, select, and narrow tasks. No identity rule requires Admin's decisions to be
deterministic.

Kotlin `Engine` is different: it validates an Actor mutation and calculates the resulting state
transition. It is not an Actor, Component, assignee, narrower, or event performer.

An Actor's Agent binds normal client calls to that Actor and presents a filtered view of the global
task pool. If a request arrives through FooPlayer's Agent for work currently assigned to FooPlayer,
that establishes the Actor for the calculation. Solarnet does not authenticate the caller or demand
proof that FooPlayer's human or bot approved it. The same program may use every Actor's Agent.
Lower-level engine mutation also remains available for exploration, replay, and correction.

The assignment and handoff checks below preserve the meaning of game decisions and resulting
events. They do not restrict who can obtain or call an Agent. Whether a submitted decision really
came from the accepted decision-maker is handled outside Solarnet under
[ADVERSARIAL.md](ADVERSARIAL.md). Admin likewise requires no separate operator or committer;
a player's program may call the Admin Agent as part of calculating its proposed continuation.

## Context and event Actor

Lexical owner insertion happens during elaboration. A bare `Plant` in an owned card effect becomes
`Plant<Me@Owner>`, then specialization of the exact card binds `Me` to its owner. Literal `Anyone`
remains broad. This Type specialization does not assign task control or event attribution.

A queued effect is assigned to the effect component's Player owner, then the changed component's
Player owner, then the triggering Actor. Automatic effects use the effect owner or triggering Actor;
their instructions execute inline. A `System` gain or pure removal itself executes as Admin.
Consequences are assigned from their own effect context and event, rather than a remembered
operation Actor. An author can use `TASK Actor { body }` to give subsequent choices explicitly to
an Actor when those sources do not identify the intended recipient.

Instruction-side `BY` remains in an abstract task until its Actor becomes concrete. It then changes
the current assignee and is removed from the executable instruction. That Actor's engine must
perform the change. Automatic effects have no task to hand off, so they cannot use instruction-side
`BY`.

A `ChangeEvent` records the Actor that performed the change. Trigger-side `BY` inspects only
that Actor. It filters event attribution; it does not transfer task control.

An owned component watching an ownerless, non-System type gets an Actor filter for its Player owner
when the effect is compiled. Explicit `BY Anyone` accepts every Actor. Watching an owned type uses
its owner dependency to select whose components match.

## Task assignment and delegation

A manually added task starts assigned to its gameplay Actor. Queued effects use the ownership and
event routing described above. Executing `TASK Blue { Choice }` completes that dispatch
instruction and admits `Choice` as an unresolved task for Blue.

A normalized concrete task that gains or removes a `System` type, including changes scaled by `/` or
wrapped in instruction-side `BY`, begins assigned to Admin. Abstract targets, amounts, and optional
gains or removals remain with the current Actor until narrowed; contextual selection reapplies the
assignment rule. Transmutations use the gained type for this classification after any abstract
source is selected. Gates must resolve before this rule applies. A concrete `BY` remains
authoritative: naming a non-Admin performer for a gain reaches the existing `System` creation guard
and fails. Admin assignment does not determine whether the component is presented as `Real`.

When this assignment changes a task's assignee and it has a `THEN` continuation, the engine encodes
the former assignee in an internal `TASK Actor { ... }` continuation. Admin completes the System
change and dispatches the remaining work back to that Actor. Each later concrete System step
follows the same rule.

Player `doTask` rejects a narrowing that makes a `System` change concrete for Admin. Use `selectTask`
or `narrowTask` to submit the choice; eager Admin policy then executes the resulting concrete task.
`TaskForm` may assist with that choice but must remain optional, as specified in
[ENGINE.md](ENGINE.md#tasks-are-choices-not-a-program-counter).
The broader intended `doTask` contract is that the submitted change must actually be performed by
this Actor; apply that rule to all handoffs when the general task-command API is ready, rather than
adding further type-specific checks here.

Use `System` for neutral table bookkeeping rather than a Player's game action. Fixed gains and
concrete removals must themselves be safe for eager Admin execution. A following `THEN` choice
returns to the prior assignee without an authored `TASK`. An authored `TASK Actor { ... }` can
dispatch work to a different recipient. This classification is
independent of `Real`, which controls presentation, and `::`, which preserves an invariant within
the initiating operation.

Card-purchase, requirement, global-parameter-status, solo-countdown, and dominance bookkeeping use
`System` for their concrete changes.

### Deferred System classifications

`PlayCard` remains open. An isolated `System` trial passed corporation selection and New Partner's
nested free Prelude when the caller committed a task form and ran automatic work before continuing
its operation. The full card-play sequence, payment, free plays, shared fixtures, replays, and
`tfm_play` still need migration and verification.

`UseAction` remains separate. An isolated trial exposed Viron's nested action timing: its
downstream choice appeared too late for the enclosing operation body. Settle that sequence before
changing action callers and reactive offers.

`RequiredActionsSignal` remains non-System. Its trial forced standard-action callers to scan for an
internal task and commit a form just to choose the required action. Revisit it only if that choice
can remain a natural standard-action call.

Selecting a task resolves it and marks it selected. Its assignee supplies any remaining choices.
The global select lock blocks competing work until this selected task completes.

If selection or narrowing makes a top-level `BY` concrete, the engine removes the wrapper, assigns
the named Actor, and stops again at the handoff. The final assignee performs the stripped
instruction.

If resolution exposes independent children, the selected task completes and admits them as
unselected work for its assignee. `THEN` continuations normally go to the finishing task's
assignee; a System handoff has already encoded a return to the preceding assignee. An authored
`TASK Actor { ... }` gives the admitted work a different recipient. The lock
ends with the selected task; it does not cover later payment. See
[`SEQUENCING.md`](SEQUENCING.md#the-missing-rule-when-an-operation-is-over).

Philares routes its reward to the card owner through effect ownership. Icy Impactors assigns the
area choice to the Start Token owner through its effect context, then uses instruction-side `BY`
to give concrete ocean placement to the card owner. World Government Terraforming assigns its
concrete change to Admin.

## Lexical ownership

See [Pets lexical ownership](../../pets/docs/agents/OWNERSHIP.md). Elaboration supplies
lexical owners; task assignment and event attribution remain separate runtime responsibilities.

## Test responsibilities

Generic engine tests may inspect Task Actor fields, assignee changes, selection state, and recorded
event Actor. Player-level tests should instead show who can act, that another Actor is blocked, and
an observable attribution consequence when material. Do not match rendered task text or inspect
event metadata merely to restate the mechanism.

Read `EffectActorCharacterizationTest`, `TaskAssignmentCharacterizationTest`,
`ByTriggerCharacterizationTest`, `TaskDispatchTest`, and `PhilaresTest` before changing these
contracts.
