# Context, assignment, and Actor identity

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing context specialization, event Actor attribution, task assignment, `BY`,
> Admin, delegated narrowing, Philares, or lexical ownership.
>
> **Status:** current semantics. A handoff lasts for the selected task, not its later payment or
> other queued consequences.

## Source map

- [`Actor.kt`](../../src/common/dev/martianzoo/state/Actor.kt): operation identities.
- [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt): `controller`,
  `selectionAssignee`, current `assignee`, and `selected`.
- [`PendingTask.kt`](../../src/common/dev/martianzoo/engine/PendingTask.kt): trigger-time routing;
  search for `fromEffect`.
- [`PetElaborator.kt`](../../src/common/dev/martianzoo/pets/PetElaborator.kt): lexical owner
  insertion; search for `insertOwnedContext`.
- [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt): the Actor filter on
  unowned triggers.

## Six identities

Keep these roles independent:

- **Context component:** the concrete component whose declaration supplied an Effect,
  Instruction, Requirement, or Metric.
- **Context owner:** that component's owner, or the Player scope through which an ad hoc
  instruction entered the engine.
- **Controller:** the Actor that orders work and receives independent siblings and continuations.
- **Selection assignee:** the Actor to whom selecting a task transfers it.
- **Current assignee:** the Actor through whose Agent the task may now be selected, narrowed, or
  executed.
- **Executing Actor:** the Actor whose `ActorEngine` performs the concrete change and is recorded
  on its `ChangeEvent`.

A Task stores `controller`, `selectionAssignee`, and `assignee`. It has no separate narrower or
performer field. The current assignee supplies remaining choices and performs the concrete
instruction.

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
Lower-level engine mutation also remains available for exploration, workflow, replay, and correction.

The assignment and handoff checks below preserve the meaning of game decisions and resulting
events. They do not restrict who can obtain or call an Agent. Whether a submitted decision really
came from the accepted decision-maker is handled outside Solarnet under
[ADVERSARIAL.md](ADVERSARIAL.md). Admin likewise requires no separate operator or committer;
a player's program may call the Admin Agent as part of calculating its proposed continuation.

## Context and event Actor

Lexical owner insertion happens during elaboration. A bare `Plant` in an owned card effect becomes
`Plant<Me@Owner>`, then specialization of the exact card binds `Me` to its owner. Literal `Anyone`
remains broad. This Type specialization does not assign task control or event attribution.

A queued effect's selection assignee defaults to the effect component's Player owner, then the
changed component's Player owner, then the triggering Actor. A queued effect of a `System` gain
uses the gain task's retained selection assignee before that last fallback. An automatic effect
executes inline as the effect owner when present and otherwise as its surrounding Actor.

Instruction-side `BY` remains in an abstract task until its Actor becomes concrete. It then changes
the current assignee and is removed from the executable instruction. That Actor's engine must
perform the change. Automatic effects have no task to hand off, so they cannot use instruction-side
`BY`.

A `ChangeEvent` records the Actor whose engine executed the change. Trigger-side `BY` inspects only
that Actor. It filters event attribution; it does not transfer task control.

An owned component watching an ownerless, non-System type gets an Actor filter for its Player owner
when the effect is compiled. Explicit `BY Anyone` accepts every Actor. Watching an owned type uses
its owner dependency to select whose components match.

## Task assignment and delegation

A direct task starts with its gameplay Actor as controller, selection assignee, and current
assignee. Queued work produced during Player-controlled work keeps that Player as controller. Its
selection assignee comes from the effect owner, changed component owner, then triggering Actor.
Admin-controlled reactions instead use those sources to choose the controller in the same order.

A normalized task that directly gains `System`, with or without a top-level instruction-side
`BY`, begins assigned to Admin while retaining its controller and selection assignee. Contextual
selection reapplies that rule. A concrete `BY` remains authoritative: naming a non-Admin performer
then reaches the existing `System` creation guard and fails. The Admin assignment changes neither
presentation nor scheduling.

Selecting a task resolves it, marks it selected, and moves its current assignee to its selection
assignee. If that Actor differs from the controller, selection stops at the handoff even when the
task is concrete. The new assignee narrows or executes it. The global select lock blocks competing
work until this selected task completes; `Task.controller` does not change.

If selection or narrowing makes a top-level `BY` concrete, the engine removes the wrapper, assigns
the named Actor, and stops again at the handoff. The final assignee performs the stripped
instruction.

If resolution replaces the selected task with independent siblings, they return to the controller
as unselected work and retain the selection assignee. `THEN` continuations behave the same way.
Reactions retain a Player controller; Admin-controlled reactions use the fallback described above.
The lock ends with the selected task. This is why current delegation does not cover a recipient's
later payment; see [`SEQUENCING.md`](SEQUENCING.md#the-missing-rule-when-an-operation-is-over).

Philares is the primary current scenario. The active Player controls when to select the reward;
selection then gives the Philares owner its resource choice and blocks the active Player until that
task finishes. Icy Impactors first assigns the area choice to the Start Token owner and later uses
instruction-side `BY` to return the concrete ocean task to the card owner. World Government
Terraforming assigns its concrete change to Admin.

## Lexical ownership

`Anyone` is the common supertype of `Owner` and `Actor`. `Player` is both; Admin is only an Actor;
SoloOpponent and Neutral are only Owners. Use bare `Owned` in subclass declarations unless
narrowing its bound or linking a variable. Use literal `<Anyone>` for unrestricted ownership and
trigger-side `BY Anyone` for every performer.

`Owned<Me@Owner>` gives the owner dependency an inherited lexical name. Subclasses can narrow that
same binding, such as `Me@Player`. Independently inherited bindings with the same name are
ambiguous unless they reach the same dependency. A marked `EACH Me@Player` or
`RANK Me@Player` rebinds `Me` inside its body; an unmarked selector preserves the outer binding.

`Me@` and other named references may omit their type when the visible name selects a unique
binding, as specified by language rule L1-7. The supplying occurrence still writes its type. An
`EVAL` captures the lexical `Me` from its evaluation site; ordinary Type-variable specialization
carries that capture through deferred fanout and ranking. The engine does not reconstruct an owner
from a selected component or event Actor.

A `HAS` candidate fills a compatible omitted dependency before lexical `Me` is inserted. Thus
`EACH Starter@Player(HAS StartToken) { ... }` tests each candidate's token. An incompatible
candidate leaves the owner open for lexical insertion.

## Test responsibilities

Generic engine tests may inspect Task Actor fields, assignee changes, selection state, and recorded
event Actor. Player-level tests should instead show who can act, that another Actor is blocked, and
an observable attribution consequence when material. Do not match rendered task text or inspect
event metadata merely to restate the mechanism.

Read `EffectActorCharacterizationTest`, `TaskAssignmentCharacterizationTest`,
`ByTriggerCharacterizationTest`, `TaskDelegationTest`, and `PhilaresTest` before changing these
contracts.
