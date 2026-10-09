# Context, assignment, and actor identity

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing context specialization, event Actor attribution, task assignment, `BY`,
> Admin, selection-time delegated narrowing, Philares, or lexical ownership.
>
> **Skip when:** changing ownership as a Type dependency without task routing, attribution, or the
> lexical `Me` binding; read sections 3, 10, and 13 of
> [type-system-spec.md](../type-system-spec.md).
>
> **Status:** current identity and lexical-ownership semantics, with unresolved operation control
> and explicit-ownership proposals distinguished below. Assignment handoffs currently last for the
> selected task, not its payment or other queued consequences.

## Source map

- [`Actor.kt`](../../src/common/dev/martianzoo/state/Actor.kt) — search
  for `public sealed interface Actor` for the operation identity mechanism.
- [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt) — inspect `controller`,
  `selectionAssignee`, current `assignee`, and `selected` before changing queued work.
- [`PendingTask.kt`](../../src/common/dev/martianzoo/engine/PendingTask.kt) — search
  for `fromEffect` to see trigger-time routing.
- [`PetElaborator.kt`](../../src/common/dev/martianzoo/pets/PetElaborator.kt) — search for
  `insertOwnedContext` to see lexical owner insertion.
- [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt) — search for
  `private fun create` to see the actor filter on unowned triggers.
- [`EffectActorCharacterizationTest.kt`](../../test/common/dev/martianzoo/engine/EffectActorCharacterizationTest.kt)
  and [`TaskAssignmentCharacterizationTest.kt`](../../test/common/dev/martianzoo/engine/TaskAssignmentCharacterizationTest.kt)
  — read before changing current Actor or assignment semantics.
- [`ByTriggerCharacterizationTest.kt`](../../test/common/dev/martianzoo/engine/ByTriggerCharacterizationTest.kt)
  — preserve trigger-side `BY` matching and binding without treating its assignment assertions as
  the handoff contract.
- [`TaskDelegationTest.kt`](../../test/common/dev/martianzoo/engine/TaskDelegationTest.kt) and
  [`PhilaresTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/PhilaresTest.kt) — inspect
  the generic handoff and its primary game scenario.

## Identity roles

Pets behavior is interpreted in context. Keep these roles separate:

- **Context component:** the concrete component whose declaration supplied an Effect, Instruction,
  Requirement, or Metric.
- **Context owner:** the owner of that component, or the Player scope through which an ad hoc
  instruction entered the engine.
- **Controller:** the Actor that orders the work and receives its independent siblings and
  continuations.
- **Selection assignee:** the Actor to whom selecting the task transfers it. Effect ownership and
  trigger context determine this value when queued work is created.
- **Current assignee:** the Actor with exclusive authority to advance the task now: select it while
  unselected, narrow it while abstract, or execute it when concrete.
- **Executing Actor:** the Actor whose `ActorEngine` performs the concrete change and is recorded on
  its `ChangeEvent`.

A Task stores exactly three Actor identities: `controller`, `selectionAssignee`, and `assignee`.
It has no separate narrower or performer field. The current assignee supplies any remaining choice;
when the instruction is ready, the current assignee's engine performs it. Icy Impactors uses
instruction-side `BY` to make its final assignee differ from its selection assignee.

## Admin and engine

`Admin` is the concrete non-Player Actor and Component that performs neutral table activity. An
N-Player game has those N seated Player Actors plus Admin. Admin may control, receive, select, and
narrow tasks, including abstract choices such as a die result. No identity rule
requires Admin's decisions to be deterministic or outcome-preserving.

Kotlin `Engine` is different: it is the passive mechanism that validates an Actor mutation and
calculates the resulting state transition. It is not an Actor, Component, task assignee, narrower,
or event performer.

Core engine state records a Task's current assignee and whether it is selected, and enforces that
task mutations come through that Actor's engine. The Actor's unique Agent binds normal client calls
to that Actor, presents a convenient filtered view of the global task queue, and issues both
explicit and policy-chosen mutations. Lower-level engine mutation remains available for deliberate
workflow, replay, cheat, and test use.

## Context specialization

Lexical owner insertion happens during elaboration. A bare `Plant` in an owned card effect becomes
`Plant<Me@Owner>`, then specialization of the exact card component binds `Me` to its owner. An
ownerless effect may declare `Me@Player` in an owned trigger or a `BY` selector. Literal `Anyone`
remains broad. This Type specialization is independent of task routing and Actor attribution.

## Actor

A queued effect's selection assignee defaults to the effect component's Player owner, then the
changed component's Player owner, then the triggering Actor. A queued effect of a `System` gain
inserts the gain task's retained selection assignee before that final Actor fallback. An automatic
effect instead executes inline as the effect owner when present and otherwise as its surrounding
Actor. A queued task normally starts assigned to its controller. If its normalized instruction
directly gains a `System` Component, alone or under a top-level instruction-side `BY`, it starts
assigned to Admin instead, before its add event is recorded.

Instruction-side `BY` is not a request to record an invented performer. It remains in an abstract
task until the instruction becomes concrete, then changes the current assignee and is removed from
the executable instruction. That Actor's engine must perform the change. Automatic `::` effects
have no task to hand off and therefore cannot use instruction-side `BY`; trigger-side `BY` remains
independent actor matching.

A `ChangeEvent` records the Actor whose engine executed the change. Trigger-side `BY` inspects only
that event Actor. This is why stealing a victim's heat is still an action by the attacker.

## Trigger actor filter

An owned component watching an ownerless, non-System type gets an actor filter for its Player
owner when the effect is compiled. An explicit `BY Anyone` accepts every Actor instead. Watching an
owned type uses its owner dependency to say whose components match. This is trigger matching,
not task attribution; language rule L6-9 owns its syntax.

## Triggered task assignment and delegation

A direct task starts with its gameplay Actor as controller, selection assignee, and current
assignee. Queued work triggered during a Player-controlled operation keeps that Player as
controller, regardless of which component owns the effect. Its selection assignee is the Player
owner of the effect-bearing component, then the Player owner of the changed component, then the
triggering Actor. When the surrounding controller is Admin, the effect owner, changed component
owner, and triggering Actor instead supply the controller in that order. An unselected task's
current assignee is normally its controller. A normalized task that directly gains `System`, with
or without a top-level instruction-side `BY`, is assigned to Admin immediately while retaining its
controller and selection assignee. Contextual selection reapplies that rule so it does not pass the
System gain back to the selection assignee. A concrete instruction-side `BY` then remains
authoritative; naming a non-Admin performer reaches the `System` creation guard and fails. A queued
effect of the System gain uses the retained selection assignee only after neither the effect nor
the changed component supplies a Player owner. Automatic effects retain their ordinary Actor rule.

Start-player requests locate the token's Player with an explicitly named `EACH` selector and gain a
request signal owned by that Player. `EACH` binds lexical `Me`; the signal's own effect supplies the
selection assignee. Icy Impactors separately captures the signal event's Actor and uses
instruction-side `BY` so the card owner receives the concrete ocean task after the start player
chooses its area. World Government Advisor instead gains its owned request directly, so the card
owner chooses before Admin receives the concrete global-parameter task.

Selecting any task resolves it, sets `selected`, and changes its current assignee to its selection
assignee. If that Actor differs from the controller, selection stops at the handoff even when the
task is already concrete. The new assignee's engine narrows an abstract task or executes a concrete
task. The global select-lock prevents the controller or any other Actor from advancing competing
work until the selected task completes. `Task.controller` does not change.

When selection or later narrowing makes a top-level `BY` instruction concrete, the engine resolves
its participating Actor, removes the `BY` wrapper, and changes the current assignee again. Execution
again stops at the handoff. Only the final assignee's engine can execute the stripped instruction,
and that actual Actor is recorded on resulting changes.

If resolution or narrowing replaces the selected task with independent siblings, those siblings
return to the controller as unselected work and retain the task's selection assignee. `THEN`
continuations do the same. Reactions retain a Player controller; Admin-controlled reactions use the
fallback above. The lock ends with the selected task. This is current behavior, not a rule that the
original controller should reclaim every descendant.

Neptunian Power Consultants exposes the gap: P1 may choose when to offer P2 the optional ocean
response, but accepting it creates payment work that must remain under P2's control while P1 waits.
Current delegation covers the initial choice only. Descendant routing, exclusion of unrelated work,
and the end of this control interval remain unresolved; see
[delegated operations and scheduling options](SEQUENCING.md#delegated-operations-and-scheduling-options).
Instruction-side `BY` cannot provide that wider authority because its assignment lasts only for its
selected task.

The constraining cases are:

| Case | Controller | Assignee on selection | Assignee when concrete |
| --- | --- | --- | --- |
| Philares | Player placing the adjacent tile | Philares owner | Philares owner |
| Enceladus bonuses | Active trader | Colony owner | Colony owner |
| Icy Impactors | Card owner | Start Token owner | Card owner via `BY` |
| World Government Terraforming | Start Token owner | Start Token owner | Admin via `BY` |
| Steal | Attacker | Attacker | Attacker |
| Homeostasis Bureau | Surrounding Player | Card owner | Card owner |
| Pharmacy Union | Tag-producing Player | Pharmacy Union owner | Pharmacy Union owner |

`Player(NOT Me@Owner)` filters an event Actor Type; it neither assigns task control nor makes an
instruction mandatory.

Philares is the primary sequencing scenario. The active Player controls a pending resource task
caused by that Player's placement and may select other eligible siblings before it. Once the active
Player selects that task, resolution delegates its resource choice to the Philares owner. The active
Player cannot select competing work until the Philares owner narrows the choice and receives the
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

Generic engine tests may inspect all three Task Actor fields, assignee changes, selection state,
and recorded event Actor to cover the mechanism once. Player-level card and rule tests must stay functional:
they show who can select or narrow through gameplay calls, that the controller is blocked through
rejected gameplay, and attribution through a visible trigger-side `BY` consequence when
attribution is material. They should not filter tasks by cause or Actor, match exact internal task
strings, or read the Event Log to restate engine metadata.

`ByTriggerCharacterizationTest` owns trigger matching and Actor-variable binding; it does not prove
delegated operation control. Autoexecution must be disabled in scenarios intended to prove that the
engine itself excludes the waiting player throughout payment.

## Open policy questions

- **Unique Philares reward under CONCRETE:** P1 creates an adjacency and, after immediate consequences,
  the Philares resource choice is the only selectable P1-controlled task. CONCRETE can currently select
  that task on P1's behalf, so it is already selected and assigned to P2 before an explicit P1
  command.
  Decide whether that is a legitimate controller auto-selection policy or whether every
  cross-Player handoff requires an explicit controller selection. When multiple sibling tasks are
  available, CONCRETE already leaves the ordering to P1.

## Lexical ownership model

`Anyone` is the ordinary common supertype of `Owner` and `Actor`. `Owner` names who can own a
component; `Actor` names who can perform an operation. `Player` is both; Admin is only an Actor;
SoloOpponent and Neutral are only Owners. All are Anyone identities. This hierarchy belongs to
Pets; Kotlin represents operation participants with `Actor` and derives ownership from
`Component.owner` and `Component.owningPlayer`.

Use bare `Owned` in subclass declarations unless narrowing its bound or linking a variable. Keep
actor-specific dependency bounds precise. Prefer literal `<Anyone>` for unrestricted ownership
references and trigger-side `BY Anyone` for every performer. These use ordinary intersections with the declared Owner or Actor domain;
`Anyone` never undergoes contextual substitution and does not make Admin an owner or passive owners
actors. Instruction-side `BY` still requires one concrete participating Actor. Standalone owner
fanout and ranking use `Owner`, since `EACH Anyone` and `RANK Anyone` also include Admin. Where
multiple identity dependencies make the role unclear, retain the precise role or a named variable.

`Owned<Me@Owner>` gives its owner dependency an inherited lexical name. That name is visible to
subclasses without redeclaration and specializes with the exact component Type. A use such as
`Me@Player` may narrow inherited `Me@Owner` while naming the same binding. Independent parents
that give `Me` to distinct dependencies remain ambiguous; repeated paths to one dependency agree.
An explicitly marked `EACH Me@Player` or `RANK Me@Player` selector rebinds `Me` within its body or
metrics. An unmarked `EACH` or `RANK` preserves the outer binding. A named rank selector evaluates
each peer's comparison keys in that peer's ownership context.
Names such as `Starter@Player` and
`Other@Player` express a different selected role without rebinding `Me`.

`Me@` and other `Name@` references may omit the bound type when the visible name selects one
binding, as specified by [L1-7](../pets-language-spec.md#1-expressions). The supplying occurrence
still writes its type; short references resolve to that binding, including inherited header names
and locally rebound selectors. In a `THEN` sequence, only observing references may precede their
supplier. Remaining shorthand extensions are tracked in [TODO.md](../../TODO.md).

The current elaborator inserts the nearest lexical `Me` in bare `Owned` expressions, including
represented-Class references such as `@StandardResource` when they produce an owned component.
An omitted owner on a marked declaration receives lexical `Me`; a reference
to that declaration retains its selected Type. For an effect with no inherited `Me`, elaboration introduces
one shared `Me@Player` declaration in an owned
trigger when possible, or in `BY` for an ownerless trigger. An owned component's unowned watched
type also receives an explicit actor filter during effect compilation. Submitted instructions use
the submitting Player as the insertion context. This Owned-specific insertion is provisional syntax;
the selected direction is to request it explicitly through `OWN[...]`, including whole effects.
General `DEFAULT` is not part of the ownership rule.

Task controller, selection assignee, current assignee, and event Actor remain separate roles. An
`EVAL` captures the lexical `Me` from its evaluation site, rendering it as `EVAL<Me>`. Ordinary
Type-variable specialization carries that capture through deferred fanout and rank evaluation. The
engine does not reconstruct an owner from the selected component or event Actor. A property in an
ownerless effect without a lexical binding cannot acquire it from an unrelated event.

A `HAS` candidate fills a compatible omitted dependency, including the owner or a dependency that
determines it, before lexical `Me` is inserted. Thus `EACH Starter@Player(HAS StartToken) { ... }`
tests each candidate's token. An incompatible candidate leaves the owner open for lexical `Me`.

## Future direction

**Selected authoring direction, unimplemented:** explicit `OWN[...]` requests ownership shorthand,
including whole effects, while card and map authoring retain their compact inputs. This concerns elaboration;
it does not itself solve task control across payment or other queued consequences.

The type system already handles ownership dependencies and inherited variables through ordinary
rules. The remaining special treatment lives mainly in `PetElaborator.insertOwnedContext`,
`LiveEffect.create`, `LiveEffect.specialize`, `PendingTask.fromEffect`, and
`Component.owner` / `owningPlayer`. Moving those operations behind a transform would not establish
simplification unless inference, repeated traversal, or special cases actually disappear.

### Selection-assignee proposal

An explicit Effect selector could supply the selection assignee currently inferred by
`PendingTask.fromEffect`. Syntax and fallback behavior remain open. Ordinary instruction `BY`
cannot replace this inference: Philares separates initial ordering from the recipient's choice,
while Icy Impactors uses `BY` only after the start player has chosen.

Ownerless effects also differ: Enceladus derives its chooser from an owned bonus signal, while
map bonuses use placement attribution. Automatic effects omit the changed component owner from
their Actor fallback, and Admin-controlled queued work has a separate controller fallback. A shared
selector must preserve or explicitly reconsider these differences.

The remaining ownership questions are whether explicit marks can preserve deferred property
bindings, selector shadowing, represented resource variables, and copied custom-instruction syntax
without a parallel environment; and whether ordinary Pets can express passive-owner applicability
without suppressing valid effects. Neither an ownership rewrite nor declaration relocation is a
prerequisite for deciding delegated operation control.
