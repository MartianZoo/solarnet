# Context, assignment, and Actor identity

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing on-turn identity, context specialization, event Actor attribution, task
> assignment, `BY`, Admin, delegated narrowing, Philares, or lexical ownership.
>
> **Status:** current semantics plus the proposed on-turn fact and intended routing in their own
> section. Currently a handoff lasts for the selected task, not its later payment or other queued
> consequences.

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
- **Executing Actor:** the Actor attributed to the concrete change and recorded on its
  `ChangeEvent`.

A Task stores `controller`, `selectionAssignee`, and `assignee`. It has no separate narrower or
performer field. The current assignee supplies remaining choices and performs the concrete
instruction.

## Who is on-turn, and who decides

**Proposed representation and intended routing, not implemented.** The
[turn lifecycle](SEQUENCING.md#turns-and-turn-offers) defines when a player is on-turn.
The owner is open to recording the current player directly in the component graph with:

```pets
CLASS OnTurn<Player> : System, Hidden { HAS MAX 1 OnTurn }
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
changed component's Player owner, then the triggering Actor. A queued effect of a `System` gain or
pure removal uses the task's retained selection assignee before that last fallback. An automatic effect
executes inline as the effect owner when present and otherwise as its surrounding Actor, except
that a `System` gain or pure removal executes as Admin. This does not change the Player recipient of downstream
choices or the Actor of other changes in the same automatic effect.

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

A direct task starts with its gameplay Actor as controller, selection assignee, and current
assignee. Queued work produced during Player-controlled work keeps that Player as controller. Its
selection assignee comes from the effect owner, changed component owner, then triggering Actor.
Admin-controlled reactions instead use those sources to choose the controller in the same order.

A normalized task that gains `System` with a fixed scalar or removes a concrete `System` type,
including changes scaled by `/` or wrapped in instruction-side `BY`, begins assigned to Admin while
retaining its controller and selection assignee. An abstract scalar or removal target, and the choice
whether to perform an optional removal, remain with the Player until narrowed; contextual selection
reapplies the assignment rule. Transmutations use the gained type for this classification. Gates
must resolve before this rule applies. A concrete `BY` remains authoritative: naming a non-Admin
performer for a gain reaches the existing `System` creation guard and fails. The Admin assignment
changes neither presentation nor scheduling.

Use `System` for neutral table bookkeeping rather than a Player's game action. Fixed gains and
concrete removals must themselves be safe for eager Admin execution. If they open a real choice, that
choice remains downstream work for the retained Player recipient. This classification is
independent of `Hidden`, which controls presentation, and `::`, which preserves an invariant within
the initiating operation.

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

An ownerless rule may infer `Me` from an owned trigger or, for a non-System event, its Player
actor. Elaboration rejects an attempt to infer that Player from an ownerless System event;
`Phase: Plant` needs an explicit recipient when `Phase` is System. A System event with an owned
dependency can still supply the recipient through that dependency.

### Ownership transition: current code and saved experiments

The selected stopping point keeps implicit ownership and the useful fixes independent of OWN.
Current Canon does not justify general support for arbitrary transform combinations. The
`ownership` branch remains the place to pursue making Owned and Owner ordinary; its existing
prototype and `docs/agents/OWNERSHIP.md` are the starting point, not a discarded direction.
Do not merge that prototype into current code as incidental cleanup.

Making inferred Actor and recipient bindings explicit belongs to that transition, not to the
independent fixes kept here. Adding bare `BY Player` to the 16 audited rules only duplicated an
existing filter while leaving the actual binding implicit; those edits were removed. For future
work, prefer the fully explicit relationship, for example:

```pets
TemperatureStep BY Me@Player IF =3 TemperatureStep: PROD[Heat<Me@>]
```

This states both who performed the event and who receives the reward. It is a useful target for
what OWN could eventually insert, not an approved migration of current Canon.

The later experiment in supporting deferred transforms through EVAL and preserving transforms
around extracted effects/actions is saved in local annotated tags:

- `archive/transforms-before-main`: snapshot whose full build had passed before merging main.
- `archive/transforms-after-main`: snapshot with unfinished integration after merging main.

These tags point to stash commits: their main trees contain tracked files; their third parents
contain the files that were untracked. Preserve both when recovering an experiment. Neither tag
selects this machinery for current code, and the later snapshot need not be repaired for archival
purposes. The experiment exposed the cost of carrying authored transform wrappers through source
copying, class extraction, property evaluation, and consumers expecting concrete effects/actions.
Resume it only for a demonstrated need, or as part of the deliberate ownership redesign.

An October 10 audit at `fba575219` scanned all 1,581 compiled Canon classes, including generated
cards. None of its 11 distinct RANK expressions had a selector refinement; no HAS refinement
contained a Requirement.Eval. The EACH/RANK selector-scope disagreement and lost HAS-EVAL candidate
therefore had no occurrence in that catalog. Award metrics evaluated inside RANK are a different
case. The user selected leaving both questions documented without changing behavior. These are
observations about current content, not restrictions on future content.

The same temporary audit elaborated effects in solo and two-player premises across all seven maps,
with the supported expansion/card packs selected. Trigger inference is actively used by placement
bonuses, standard projects, colony rewards, phase offers, and global-parameter rewards. In
particular, `GlobalParameter`'s `This: TerraformRating` acquires `BY Me@Player`, excluding Admin
events. That dependency does not itself demonstrate incorrect gameplay; removing inference needs
a deliberate migration. The audit could not elaborate WildlifeDome's `GreeneryTile<>`, which has
no gain dependency default to accept; that separate content defect limits the elaboration sweep.
The user selected recording WildlifeDome for later rather than fixing it during this work.

**Potential later simplification, not selected for implementation:** stop supplying a missing player
from the Actor merely because an effect's instruction needs an owner. A temporary engine probe with
trigger inference disabled confirmed that `Pulse: Temperature, Plant` responds to Admin with both
tasks, leaving `Plant!` abstract. The caller can choose `Plant<Player2>` and complete it. This exposes
an unresolved recipient instead of silently filtering the whole effect, but does not itself reject
an authoring mistake. Decide separately whether Canon needs such an open choice or whether these
declarations should fail. Inference from an owned trigger's recipient is a separate question.

## Test responsibilities

Generic engine tests may inspect Task Actor fields, assignee changes, selection state, and recorded
event Actor. Player-level tests should instead show who can act, that another Actor is blocked, and
an observable attribution consequence when material. Do not match rendered task text or inspect
event metadata merely to restate the mechanism.

Read `EffectActorCharacterizationTest`, `TaskAssignmentCharacterizationTest`,
`ByTriggerCharacterizationTest`, `TaskDelegationTest`, and `PhilaresTest` before changing these
contracts.
