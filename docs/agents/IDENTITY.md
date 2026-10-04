# Context, assignment, and actor identity

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing context specialization, event Actor attribution, task assignment, `BY`,
> Admin, selection-time delegated narrowing, Philares, or lexical ownership.
>
> **Skip when:** changing ownership as a Type dependency without task routing, attribution, or the
> lexical `Me` binding; read sections 3, 10, and 13 of
> [type-system-spec.md](../type-system-spec.md).
>
> **Status:** current identity and lexical-ownership semantics. The interaction between CONCRETE
> auto-selection and cross-Player handoff remains open.

## Source map

- [`Actor.kt`](../../src/common/dev/martianzoo/state/Actor.kt) — search
  for `public sealed interface Actor` for the operation identity mechanism.
- [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt) — inspect `controller`, the derived
  `assignee`, `actor`, and selection state before changing queued work.
- [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt) — search
  for `taskController` to see trigger-time routing.
- [`PetElaborator.kt`](../../src/common/dev/martianzoo/pets/PetElaborator.kt) — search for
  `insertOwnedContext` to see lexical owner insertion.
- [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt) — search for
  `private fun create` to see the actor filter on unowned triggers.
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
`BY` to separate its credited Actor.

## Admin and engine

`Admin` is the concrete non-Player Actor and Component that performs neutral table activity. An
N-Player game has those N seated Player Actors plus Admin. Admin may control, receive, select, and
narrow tasks, including abstract choices such as a die result. No identity rule
requires Admin's decisions to be deterministic or outcome-preserving.

Kotlin `Engine` is different: it is the passive mechanism that validates an Actor mutation and
calculates the resulting state transition. It is not an Actor, Component, task assignee, narrower,
or event performer.

Core engine state derives a Task's current assignee from its selection state and enforces that
task mutations name that Actor. The Actor's unique Agent binds normal client calls to that
Actor, presents a convenient filtered view of the one global task queue, and issues both explicit and
policy-chosen mutations. Lower-level engine mutation remains available for deliberate workflow,
replay, cheat, and test use.

## Context specialization

Lexical owner insertion happens during elaboration. A bare `Plant` in an owned card effect becomes
`Plant<Me@Anyone>`, then specialization of the exact card component binds `Me` to its owner. An
ownerless effect may declare `Me@Player` in an owned trigger or a `BY` selector. Literal `Anyone`
remains broad. This Type specialization is independent of task routing and Actor attribution.

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

## Trigger actor filter

An owned component watching an ownerless, non-System type gets an actor filter for its Player
owner when the effect is compiled. An explicit `BY Actor` accepts every Actor instead. Watching an
owned type uses its owner dependency to say whose components match. This is trigger matching,
not task attribution; language rule L6-9 owns its syntax.

## Triggered task assignment and delegation

A direct task starts with its gameplay Actor as controller and contextual Actor. Queued work
triggered during a Player-controlled operation keeps that Player as controller, regardless of which
component owns the effect. Its contextual Actor is the Player owner of the effect-bearing component,
then the Player owner of the changed component, then the triggering Actor. Admin-driven setup and
workflow retain that routing. An unselected task's assignee is its controller.

Start-player requests locate the token's Player with an explicitly named `EACH` selector and gain a request
signal owned by that Player. `EACH` binds lexical `Me`; the signal's own
effect supplies the owned-component task routing. Icy Impactors separately captures the
signal event's Actor and uses instruction-side `BY` so the card owner still performs the ocean
placement chosen by the start player. World Government Advisor instead gains its owned request
directly, so the card owner chooses regardless of who holds the Start Token.

Selecting a concrete task executes it in place. The change records the task's Actor unless an
instruction-side `BY` overrides it. Reactions caused by that execution return to the retained
controller's queue.

Selecting an abstract task resolves it first, then moves that same selected task to its contextual
Actor's queue when that Actor differs from the controller. The derived assignee narrows it without
another selection. The global select-lock prevents the controller or any other Actor from selecting
or executing competing work until the selected task completes. `Task.controller` does not change
during this handoff.

If resolution or narrowing replaces the selected task with independent siblings, those siblings
return to the controller as unselected work. `THEN` continuations and tasks triggered by
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

`Player(NOT Me@Anyone)` filters an event Actor Type; it neither assigns task control nor makes an
instruction mandatory.

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
recorded Actor to cover the mechanism once. Player-level card and rule tests must stay functional:
they show who can select or narrow through gameplay calls, that the controller is blocked through
rejected gameplay, and attribution through a visible trigger-side `BY` consequence when
attribution is material. They should not filter tasks by cause or Actor, match exact internal task
strings, or read the Event Log to restate engine metadata.

`ByTriggerCharacterizationTest` owns trigger matching and Actor-variable binding. Task assignment is
incidental there and should be removed from those assertions or made explicit in separately named
delegation tests. Card tests for Pharmacy Union and Splice should assert their normal outcomes and
which Player can make any offered choice; generic engine coverage should carry the internal routing
coverage.

## Open policy questions

- **Unique Philares reward under CONCRETE:** P1 creates an adjacency and, after immediate consequences,
  the Philares resource choice is the only selectable P1-controlled task. CONCRETE can currently select
  that task on P1's behalf, so it is already selected in P2's queue before an explicit P1 command.
  Decide whether that is a legitimate controller auto-selection policy or whether every
  cross-Player handoff requires an explicit controller selection. When multiple sibling tasks are
  available, CONCRETE already leaves the ordering to P1.

## Lexical ownership model

The selected language model treats `Anyone` as an ordinary Class. `Actor` names who performs an
operation; `Anyone` names who can own a component. `Player` is both; Admin is only an Actor;
SoloOpponent is only an `Anyone`. Use literal `<Anyone>` for an unrestricted ownership dependency
and `BY Actor` for an unrestricted trigger Actor. `Anyone` never undergoes contextual substitution.

`Owned<Me@Anyone>` gives its owner dependency an inherited lexical name. That name is visible to
subclasses without redeclaration and specializes with the exact component Type. A use such as
`Me@Player` may narrow inherited `Me@Anyone` while naming the same binding. Independent parents
that give `Me` to distinct dependencies remain ambiguous; repeated paths to one dependency agree.
An explicitly marked `EACH Me@Player` or `RANK Me@Player` selector rebinds `Me` within its body or
metrics. An unmarked `EACH` or `RANK` preserves the outer binding. A named rank selector evaluates
each peer's comparison keys in that peer's ownership context.
Names such as `Starter@Player` and
`Other@Player` express a different selected role without rebinding `Me`.

The current elaborator inserts the nearest lexical `Me` in bare `Owned` expressions, including
represented-Class references such as `@StandardResource` when they produce an owned component.
An omitted owner on a marked declaration receives lexical `Me`; a reference
to that declaration retains its selected Type. For an effect with no inherited `Me`, elaboration introduces
one shared `Me@Player` declaration in an owned
trigger when possible, or in `BY` for an ownerless trigger. An owned component's unowned watched
type also receives an explicit actor filter during effect compilation. Submitted instructions use
the submitting Player as the insertion context. This Owned-specific insertion is provisional syntax;
`OWN[...]` may eventually request it explicitly around a whole effect or action. General `DEFAULT`
is not part of the ownership rule.

The lexical model removes the old ownership default, its special preservation, effect-time
contextual substitution, and special handling of an unbound ownership `BY`.
Task controller, assignee, and Actor remain separate roles. An `EVAL` captures the lexical `Me`
from its evaluation site, rendering it as `EVAL<Me>`. Ordinary Type-variable specialization carries
that capture through deferred fanout and rank evaluation. The engine does not reconstruct an owner
from the selected component or event Actor. A property in an ownerless effect without a lexical
binding cannot acquire one from an unrelated event. The corpus and full replay suite exercise passive solo owners,
represented resources, ranked metrics, and action costs.

A `HAS` candidate fills a compatible omitted dependency, including the owner or a dependency that
determines it, before lexical `Me` is inserted. Thus `EACH Starter@Player(HAS StartToken) { ... }`
tests each candidate's token. An incompatible candidate leaves the owner open for lexical `Me`.

## Future direction

An explicit `OWN[...]` transform may replace the current implicit Owned-specific insertion while
leaving compact card JSON nearly unchanged. Before selecting it, check whether the resulting
`.pets` authoring improves and whether whole-effect wrapping expresses both trigger and result
without extra owner machinery. Keep the current lexical model coherent on its own, including the
player versus passive-owner boundary and property evaluation, rather than preserving old runtime
substitution as an unnoticed dependency.

`Me@` without a written bound would be useful for references whose visible name selects one
binding. The current parser requires a Class name to construct every Expression, so this needs a
real unresolved-reference form and scope resolution that detects ambiguous inherited or local
names. Treating every `Me@` as `Me@Anyone` would misread a locally rebound `Me@Player`.

The class elaborator still records scopes around its post-dispatch ownership insertion; remove
further passes only when transformed variable occurrences continue to specialize correctly.
