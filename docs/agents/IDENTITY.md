# Context, assignment, and actor identity

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing context specialization, event Actor attribution, task assignment, `BY`,
> Admin, selection-time delegated narrowing, Philares, Admin-selected hidden cards, or the
> `Owner`/`Anyone` contextual-variable overload.
>
> **Skip when:** changing ownership as a Type dependency without task routing, attribution, or the
> contextual `Owner` spelling; read the dependency sections of [TYPES.md](TYPES.md).
>
> **Status:** current identity semantics plus the selected Engine/Admin naming direction. The
> interaction between SAFE auto-selection and cross-Player handoff remains open, as does the entry
> under Open audit.

## Source map

- [`Identities.kt`](../../src/common/dev/martianzoo/pets/data/Identities.kt) — search
  for `public sealed interface Actor` for the operation identity mechanism.
- [`Task.kt`](../../src/common/dev/martianzoo/pets/data/Task.kt) — inspect `controller`, the derived
  `assignee`, `actor`, and selection state before changing queued work.
- [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt) — search
  for `taskController` to see trigger-time routing.
- [`Transformers.kt`](../../src/common/dev/martianzoo/engine/Transformers.kt) — search for
  `fixEffectForUnownedContext` to see ownerless Effects acquire their event-Actor filter.
- [`Defaults.kt`](../../src/common/dev/martianzoo/pets/types/Defaults.kt) — search for
  `Owner also acts as a contextual variable` before changing how `Owner` resolves in defaults.
- [`EffectActorCharacterizationTest.kt`](../../test/common/dev/martianzoo/engine/EffectActorCharacterizationTest.kt)
  and [`TaskAssignmentCharacterizationTest.kt`](../../test/common/dev/martianzoo/engine/TaskAssignmentCharacterizationTest.kt)
  — read before changing current Actor or assignment semantics.
- [`ByTriggerCharacterizationTest.kt`](../../test/common/dev/martianzoo/engine/ByTriggerCharacterizationTest.kt)
  — preserve trigger-side `BY` matching and binding without treating its queue assertions as the
  delegation contract.
- [`TaskDelegationTest.kt`](../../test/common/dev/martianzoo/engine/TaskDelegationTest.kt) and
  [`PhilaresTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/PhilaresTest.kt) — inspect
  the generic handoff and its primary game scenario.

## Six identities

Pets behavior is interpreted in context. Keep these roles separate:

- **Context component:** the concrete component whose declaration supplied an Effect, Instruction,
  Requirement, or Metric.
- **Context owner:** the owner of that component, or the Player scope through which an ad hoc
  instruction entered the engine.
- **Controller:** the Actor that controls the surrounding operation and receives work caused by it.
- **Assignee:** the Actor permitted by game state to select or narrow the deferred work in its
  current state.
- **Narrower:** the Actor entitled to supply choices remaining inside a selected abstract task.
- **Actor:** the default performer credited with the resulting change. Instruction-side `BY` may
  override it.

These roles often coincide. Current queued work stores its controller and contextual Actor once.
Its three-state selection lifecycle determines which of them is the assignee. The contextual Actor
supplies any remaining choice and is the default performer. Icy Impactors uses instruction-side
`BY` to separate its credited Actor. Future hidden-card selection would add a new case in which
Admin chooses without becoming the default performer.

## Admin and engine

`Admin` is the concrete non-Player Actor and Component that performs neutral table activity. An
N-Player game has those N seated Player Actors plus Admin. Admin may control, receive, select, and
narrow tasks, including abstract choices such as a dealt card face or die result. No identity rule
requires Admin's decisions to be deterministic or outcome-preserving.

Kotlin `Engine` is different: it is the passive mechanism that validates an Actor mutation and
calculates the resulting state transition. It is not an Actor, Component, task assignee, narrower,
or event performer. Current code and Pets still call the administrative Actor `Engine`; that is
migration state, not the target vocabulary.

Core engine state derives a Task's current assignee from its selection state and enforces that
ordinary task mutations name that Actor. The Actor's unique Agent binds normal client calls to that
Actor, presents a convenient filtered view of the one global task pool, and issues both explicit and
policy-chosen mutations. Lower-level engine mutation remains available for deliberate workflow,
replay, cheat, and test use.

## Context specialization

Contextual specialization happens before a `LiveEffect` exists. A bare `Plant` inherited by an
owned card becomes that card owner's plant. An ownerless class effect may retain `Plant<Owner>`
until inheritance by a concrete owned component binds it. This Type specialization is independent
of task routing and Actor attribution.

## Actor

A queued effect instruction's Actor defaults to the effect component's Player owner, then the
changed component's Player owner, then the surrounding execution Actor. An automatic effect keeps
the effect owner when present and otherwise the surrounding Actor. An ad hoc instruction defaults
to its gameplay Actor. Instruction-side `BY` explicitly overrides the performer without changing
who narrows an abstract task.

The default binds when work is produced. A pending or queued task remembers its contextual Actor in
`Task.actor`; later execution through another gameplay scope does not steal attribution. Splitting,
narrowing, resolution, and `THEN` continuation preserve it.

A `ChangeEvent` records that Actor. Trigger-side `BY` inspects only the event's Actor. This is why
stealing a victim's heat is still an action by the attacker.

## Implicit trigger Owner

The icon grammar gives an ordinary trigger on a Player-owned card an implicit Actor filter. For
example, `OceanTile` on that card means `OceanTile BY Owner`; writing `OceanTile BY Anyone`
explicitly cancels the filter. This is trigger matching, not task attribution and not an authored
Type variable.

An ownerless rule can also require the triggering Player as context for its result. The canonical
case is `GlobalParameter`:

```pets
This: TerraformRating
```

Defaults elaborate the result to `TerraformRating<Owner>`, but the Trigger contains no `Owner` to
bind. Class-Effect transformation therefore supplies `BY Owner`. Trigger matching accepts a Player
Actor, rejects Engine as an Owner, and uses that Player to close the result's contextual `Owner`.
No default-occurrence propagation can replace this rule because the value comes from the event's
Actor rather than another Type expression.

## Triggered task assignment and delegation

A direct task starts with its gameplay Actor as controller and contextual Actor. Queued work
triggered during a Player-controlled operation keeps that Player as controller, regardless of which
component owns the effect. Its contextual Actor is the Player owner of the effect-bearing component,
then the Player owner of the changed component, then the triggering Actor. Admin-driven setup and
workflow retain that routing. An unselected task's assignee is its controller.

Selecting a concrete task executes it in place. The change records the task's Actor unless an
instruction-side `BY` overrides it. Reactions caused by that execution return to the retained
controller's queue.

Selecting an abstract task resolves it first, then moves that same selected task to its contextual
Actor's queue when that Actor differs from the controller. The derived assignee narrows it without
another selection. The global select-lock prevents the controller or any other Actor from selecting
or executing competing work until the selected task completes. `Task.controller` does not change
during this handoff.

If resolution or narrowing replaces the selected task with independent siblings, those siblings
return to the controller as ordinary unselected work. `THEN` continuations and tasks triggered by
the delegated change likewise return to the controller. This keeps one task lifecycle rather than
introducing a second parent representation.

The constraining cases are:

| Case | Controller | Narrower | Future Actor |
| --- | --- | --- | --- |
| Philares | Player placing the adjacent tile | Philares owner | Philares owner |
| Enceladus bonuses | Active trader orders bonuses | Each colony owner chooses their card | Each colony owner |
| Icy Impactors | Card owner | StartToken owner chooses an ocean | Card owner via explicit `BY` |
| Steal | Attacker | Attacker | Attacker |
| Homeostasis Bureau | Surrounding operation controller | No choice | Card owner |
| Pharmacy Union | Operation that produced the Microbe tag | No choice | Pharmacy Union owner |

`!Owner` is a complement Type and has nothing to do with either form of `BY` or postfix
instruction `!`.

Philares is the primary sequencing scenario. The active Player controls a pending resource task
caused by that Player's placement and may select other eligible siblings before it. Once the active
Player selects that task, resolution delegates its resource choice to the Philares owner. The active
Player can do no more work in the scope until the Philares owner narrows the choice and receives the
resource. Assigning the reward directly to the Philares owner at trigger time would transfer control
too early.

### Enceladus

Enceladus is primarily a narrowing-authority requirement, not an attribution test. The active
trader controls the order of colony bonuses. Once the trader selects one Enceladus bonus, the owner
of that particular colony must choose which compatible card receives the microbes. With several
colonies on Enceladus, each selected bonus may therefore move to a different Player.

That privilege follows from general ownership semantics. The signal
`GainColonyBonus<ColonyTile>` is owned by the colony owner even though Enceladus itself is neutral,
so its owner becomes the narrower. Crediting the colony owner is also correct, but does not by
itself prove that the owner received narrowing authority.

## Test responsibilities

Generic engine tests may inspect `Task.controller`, derived assignee changes, selection state, and
recorded Actor to prove the mechanism once. Player-level card and rule tests must stay functional:
prove who can select or narrow through gameplay calls, prove the controller is blocked through
rejected gameplay, and prove attribution through a visible trigger-side `BY` consequence when
attribution is material. They should not filter tasks by cause or Actor, match exact internal task
strings, or read the Event Log merely to restate engine metadata.

`ByTriggerCharacterizationTest` owns trigger matching and Actor-variable binding. Task assignment is
incidental there and should be removed from those assertions or made explicit in separately named
delegation tests. Card tests for Pharmacy Union and Splice should assert their normal outcomes and
which Player can make any offered choice; generic engine coverage should carry the internal routing
proof.

## Open policy questions

- **Unique Philares reward under SAFE:** P1 creates an adjacency and, after immediate consequences,
  the Philares resource choice is the only selectable P1-controlled task. SAFE can currently select
  that task on P1's behalf, so it is already selected in P2's queue before an explicit P1 command.
  Decide whether that is a legitimate controller auto-selection policy or whether every
  cross-Player handoff requires an explicit controller selection. When multiple sibling tasks are
  available, SAFE already leaves the ordering to P1.

## Open audit

### `Owner` is overloaded as a Class and as a contextual variable

`Owner` is simultaneously a real Class (superclass of `Player` and `SoloOpponent`) and the spelling
of the contextual owner variable. `Anyone` exists only to escape the second meaning: it is an
abstract Class whose sole subclass is `Owner`, so the two denote the same set of components, and
`Foo<Anyone>` means "the `Owner` bound, but do not substitute the contextual owner."

That single overload is the common cause of a scattered set of workarounds:

- the `OWNER` carve-out in `Defaults.gatherDefaultDeps` ("Owner also acts as a contextual variable"),
  which sits directly under a `TODO: this is complex and this human doesn't understand it`;
- the `arguments.isEmpty() && refinement == null` guard in `Transforming.replaceOwnerWith`;
- `Transformers.insertDeferredComplementDefaults` and `hasDeferredOwnerComplement`; and
- five `IMPL:` comments in Catalog sources recording bounds that **cannot be written**. Three
  (`OwnedTile`, `Resource`, `Production`) say the declared `Owner` bound would erase the contextual
  binding the `Owned` default inserts; two (`MyResourceWasRemoved`, `MyProductionWasDecreased`) say
  it would lose the concrete victim while specializing a complemented `!Player`.

Those last two overlap with the Complement direction in
[TYPES.md](TYPES.md#7-complement-bounds); if contextual ownership gets its own spelling, recheck
whether the watcher sites still need a Complement at all.

The direction to investigate is giving the contextual owner a spelling distinct from the Class name,
so `Anyone` and the carve-outs can go and a class can declare `Owner` as a real bound. Confirm first
that no rule genuinely needs `Anyone` and `Owner` to be different Types.

## Future extension

Real-card dealing will need a way to name Admin as the narrower without making Admin the default
performer. A Player must control when an abstract `ProjectCard<Player, Hand>` gain is selected;
the installed Admin policy may derive the exact face from seed and event history; and the
originating task's Actor must retain attribution. Do not overload ownership or instruction-side
`BY` to encode that extra role.
