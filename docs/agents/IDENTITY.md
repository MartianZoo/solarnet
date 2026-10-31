# Context, assignment, and Actor identity

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing on-turn identity, context specialization, event Actor attribution, task
> assignment, `BY`, Admin, delegated narrowing, or Philares. Lexical ownership is in Pets.
>
> **Status:** current semantics and active-player scope, with intended routing labeled separately.
> Currently a handoff lasts for the selected task, not its later payment or other queued consequences.

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
choices and performs the concrete instruction. `SOON Actor { body }` explicitly admits new work to
another Actor; `BY Actor` hands concrete execution to its named Actor.

## Who is on-turn, and who decides

**Current representation.** The
[action lifecycle](../../pets/docs/agents/GAME_FLOW.md#turns-and-turn-offers) is recorded by the globally
unique `ActionScope<@Player> : Scope, TaskScope<@Player>, System`. Its Player identifies the active player through the
accepted action's payment, effects, delegated responses, and cleanup. Passing, declining an
optional second action, and decisions outside an action do not create a scope. This replaces the
proposed separate `OnTurn` fact; do not maintain another active-player representation. Offers
remain addressed to their recipients while no scope exists. `Baton<Player>` records the longer
table turn, including the interval between its two actions. It does not replace the scope's
active-action identity or change task routing, and it is absent during setup and research.

**Current routing through NOW/SOON.** During an action, the active
Player chooses when generated Player work is handed to its recipient. The recipient then makes the task's choices and executes
it through their own Agent. Executing NOW must give the recipient a selected task, excluding competing
work until the recipient completes its current step. This changes neither the active Player nor
the effect's lexical owner. System work returns continuations to their former assignee; queued
effects use their own ownership or explicit recipient. ActionScope also extends generic TaskScope.
Task admission uses its owner to put NOW handoffs in the active Player's queue; it adds no second
scope component or per-task recipient field. The owner reaffirmed this game rule on October 30.

Initial-research discards and Supercapacitors' production choice need no turn of their own. Icy
Impactors, Philares, Neptunian Power Consultants, St. Joseph of Cupertino Mission, and the colony
responses likewise do not grant their recipients turns: they resolve within the existing turn,
or outside all turns when none is active. Pristar's production effect illustrates that doing work
for a player does not even require a decision, much less an on-turn player. Outside turns, retain
the appropriate recipients rather than inventing an on-turn player.

## Required active-Player handoff

Acceptance checklist for another Player's task during an active action:

- Before the handoff, the owner cannot select, narrow, or execute the waiting reward through their
  Agent. The active Player can perform other eligible work and choose when to offer this task.
- The active Player cannot supply the owner's resource, target, amount, or optional acceptance.
  Task IDs, string requests, and TaskForm submission must all preserve that restriction.
  Identifying a fixed payment by its already-determined amount is not choosing an amount: retain
  unique calculated matching without requiring callers to copy the pending metric expression.
- Handing over a task leaves it selected. Other tasks cannot interrupt before the receiving Player
  takes the current step. Narrowing alone does not execute that step.
- The owner executes through their own Agent, including already-concrete rewards. A printed BY
  can still distinguish the chooser from the final performer, as Icy Impactors requires.
- Newly queued reactions and continuations return to the active Player for ordering when the
  action is still open. The restored historical tests retain the repeated handoffs between Pluto
  draws/discards and Neptunian payment/rewards; they do not demand a lock across an entire action.
- System work must preserve the active Player and the intended recipient of downstream choices.
  Pluto's per-owner lock still prevents a second bonus's draw before the first bonus's discard.
- Outside an action, setup, research, and production work stays with its intended recipients.
  An owner response does not create another turn or action.
- Player policy may make decisions only for the Player whose Agent owns that policy. Admin remains
  automatic. No test helper may repair assignment, perform another Player's choice, or bypass it.
- Retain the restored Canon and replay handoffs from the parent of 4847065e0, including the rejected
  premature Enceladus claim, and the generic command/form/continuation checks from TaskDelegationTest.
  The synthetic fixtures express the former initiating-Player context; adapting their setup to a
  selected way of identifying the active Player is allowed, weakening their observable checks is not.

### Implementation options to compare

These were the alternatives compared before the owner selected option 2 using NOW/SOON.

1. **Record the recipient separately from the current assignee.** A Philares reward starts with
   Stan as current assignee and Kim recorded as the recipient. Stan's selection transfers the
   unchanged, selected reward to Kim. This is close to the former implementation, but does not
   require reinstating every old field: the active Player can come from the action already in
   progress. Cost: extra recorded task information must survive edits, System work, continuations,
   undo, and export. A recipient field alone does not keep a Pluto System prefix from starting early.

2. **Express the handoff in the task instruction.** Stan holds an instruction meaning "offer Kim
   this resource choice"; after the handoff Kim holds the selected resource choice. The former
   TASK dispatch was not sufficient: TaskQueues.admit performs an ordinary dispatch immediately
   and its body starts unselected. BY currently concerns concrete execution after narrowing;
   changing it to choose-and-execute would also affect Icy Impactors. This option needs an explicit
   decision about language meaning and selection versus execution, preserving existing setup and
   deferred dispatch. It could keep a whole Pluto sequence unopened until its start is chosen;
   that is the selected Pluto implementation.
   Costs include wrappers in matching/forms/recording and keeping the owner's body inaccessible
   to the active Player's narrowing.

3. **Keep the task assigned to its owner, and let the active Player release it.** Kim remains the
   recorded assignee of a Philares task throughout. While it is unselected, only Stan may select it
   for release; once selected, only Kim may narrow or execute it. This could use the existing
   active action and selected state without another per-task recipient. Cost: assignment would
   no longer identify whose Agent may select the task. Queue views, matching, probes, forms,
   policy execution, and recordings would need a consistent distinction between ordering work
   and performing it. System prefixes and BY still need explicit treatment.

All three require a game-neutral way for the engine to obtain the game's existing active-action
identity. Reading the concrete TFM class ActionScope directly in generic engine code is not the
solution. Nor is treating the most recent executing Player as active: Kim's response must not
replace Stan's ordering authority. Repeated Pluto bonuses also require resolving when the
lock-protected sequence starts; assigning its final draw correctly is insufficient. The restored
historical scenarios check the draw/discard handoffs and per-owner exclusion. The focused Pluto
scenario now also checks no lock before the start, a held lock after the draw, rejection and rollback
of a second start for that owner, and an intervening bonus for another owner.

The owner subsequently selected focused evaluation and implementation of option 2 using NOW/SOON.
The recovered October 13 design distinguishes executing a selected handoff from merely queueing
ordinary work. The implementation retains Task.then and does not add AND or rewrite THEN.
The active Player executes NOW; selection alone leaves the handoff waiting. The recipient receives
selected work and makes its own choices. Full build, 23 replay exports/reconstructions, browser
checks, and Opus/high review passed; see the October 30 implementation results.

## Admin and engine

`Admin` is the non-Player Actor and Component that performs neutral table activity. Admin may
control, receive, select, and narrow tasks. Its scheduling and requirement for a unique concrete
instruction are defined in [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md).

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
operation Actor. An author can use `SOON Actor { body }` to give subsequent choices explicitly to
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

A manually added task starts assigned to its gameplay Actor; during a TaskScope, another Player's
ordinary task is wrapped for the scope owner to schedule. Queued effects use the ownership and
event routing described above. Executing `SOON Blue { Choice }` completes that dispatch
instruction and admits `Choice` as an unresolved task for Blue.

A normalized concrete task that gains or removes a `System` type, including changes scaled by `/` or
wrapped in instruction-side `BY`, begins assigned to Admin. Abstract targets, amounts, and optional
gains or removals remain with the current Actor until narrowed; contextual selection reapplies the
assignment rule. Transmutations use the gained type for this classification after any abstract
source is selected. Gated concrete System changes also belong to Admin; their gates still have to
be satisfied before execution. A concrete `BY` remains
authoritative: naming a non-Admin performer for a gain reaches the existing `System` creation guard
and fails. Admin assignment does not determine whether the component is presented as `Iconic`.

A concrete non-System gain explicitly `BY Admin` also begins assigned to Admin. This moves the
assignment earlier; `BY` is still removed when Admin selects the task, and any continuation keeps
the usual `BY` scope. Abstract gains remain with the current Actor until narrowed.

When System assignment changes a task's assignee and it has a `THEN` continuation, the engine encodes
the former assignee in an internal `SOON Actor { ... }` continuation. For a selected NOW prefix,
a single next step instead returns through NOW and stays selected. Admin completes the System
change and dispatches the remaining work back to that Actor. Each later concrete System step
follows the same rule.

Use `System` for neutral table bookkeeping rather than a Player's game action. Fixed gains and
concrete removals must themselves be safe for eager Admin execution. A following `THEN` choice
returns to the prior assignee without an authored dispatch. An authored `SOON Actor { ... }` can
dispatch work to a different recipient. This classification is
independent of `Iconic`, which controls presentation, and `::`, which preserves an invariant within
the initiating operation.

Played-event cleanup, card-purchase, requirement, global-parameter-status, solo-countdown, and
dominance bookkeeping use `System` for their concrete changes.

`RequiredAction` is also `System`: Admin creates and removes the pending corporation-action
marker. Its Player owner still chooses `DoRequiredActions`, which remains non-System.
`PlayCard` and `UseAction` are System signals: the Player supplies unresolved choices, and Admin
executes the concrete dispatch.

### Deferred System classifications

`DoRequiredActions` remains non-System. Its trial forced standard-action callers to scan for an
internal task and commit a form just to choose the required action. Revisit it only if that choice
can remain a natural standard-action call.

Selecting a task resolves it and marks it selected. Its assignee supplies any remaining choices.
The global select lock blocks competing work until this selected task completes.

If selection or narrowing makes a top-level `BY` concrete, the engine removes the wrapper, assigns
the named Actor, and stops again at the handoff. The final assignee performs the stripped
instruction.

If resolution exposes independent children, execution completes the selected task and admits them as
unselected work for its assignee. `THEN` continuations normally go to the finishing task's
assignee; a System handoff has already encoded a return to the preceding assignee. An authored
`SOON Actor { ... }` gives the admitted work a different recipient. The lock
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

The handoff acceptance tests compare alternative representations: assert which Agent can act and
whether work stays selected, without requiring the assignee field to change at the handoff.

Generic engine tests may inspect Task Actor fields, assignee changes, selection state, and recorded
event Actor. Player-level tests should instead show who can act, that another Actor is blocked, and
an observable attribution consequence when material. Do not match rendered task text or inspect
event metadata merely to restate the mechanism.

Read `EffectActorCharacterizationTest`, `TaskAssignmentCharacterizationTest`,
`ByTriggerCharacterizationTest`, `TaskDispatchTest`, and `PhilaresTest` before changing these
contracts.
