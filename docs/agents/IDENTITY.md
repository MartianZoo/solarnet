# Context and identity

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

## Status

This document separates the committed engine from a proposed identity model. The section
**Current committed implementation** is descriptive. Sections labeled **Target** are aspirational:
they record intended semantics and must not be read as behavior already provided by the committed
code.

## Pets nuggets need context

A Pets value is a nugget of behavior specification:

- an **Instruction** says what to do;
- an **Effect** says what to do in response to a trigger;
- a **Requirement** says what must be true; and
- a **Metric** says how to count.

The Pets text alone is often incomplete. Interpreting or executing it requires context.

## Current context component and target context owner

The **context component** is the concrete component whose declaration supplied the Pets nugget. A
live Effect inherited by a particular card, tile, or other component has that component as its
context component. We sometimes speak loosely of its concrete Type as the component.

If the context component is owned, its owner is the **context owner**. An
instruction entered through a Player-scoped REPL or API context likewise has that Player as its
context owner, even though it did not come from a component. Engine-scoped work may have no context
owner. The engine preserves this identity as the Actor on triggered work through queued execution.

Contextual specialization happens before a `LiveEffect` exists. For example, a bare `Plant` in an
ownerless class effect remains abstract or is represented as `Plant<Owner>` so that inheritance by
a concrete owned component can bind it. The target Types in an eventual Instruction are therefore
already settled independently of task routing and Actor attribution.

## Actor: meaning and default

The principal meaning of **Actor** is the entity credited with acting. Each `ChangeEvent` records
the Actor that performed its gain, removal, or transmutation. Trigger-side `BY` inspects only that
recorded Actor.

An instruction's Actor defaults to its context owner. Instruction-side `BY`
explicitly overrides that default. If there is no context owner, the surrounding execution Actor
is the fallback.

This default binds when work is produced: a pending or queued task remembers its **future
Actor**. Later execution by a different gameplay context must not steal the attribution. An
explicit `BY` inside the Instruction remains authoritative. `Task.actor` stores this identity.

## Target: a task has three relevant identities

These identities may all differ:

- **Controller/task owner** chooses which pending instruction proceeds next in the surrounding
  operation.
- **Narrower/decider** resolves the abstract choices inside that instruction.
- **Future Actor** is recorded on resulting changes unless instruction-side `BY` overrides it.

The committed `assignee` field conflates queue membership with narrowing authority, while
`Task.actor` independently stores the future Actor. The target model has the controller retain an
abstract parent task. Preparing it discovers whether a different narrower is required; if so,
preparation creates a child choice task in the narrower's queue, suspends the parent, and resumes
it after the child becomes concrete.

## Target examples

The following table describes desired identity assignments. It is not a table of current queue
routing or event attribution.

| Case | Controller/task owner | Narrower | Future Actor |
| --- | --- | --- | --- |
| **Philares** | The Player placing the adjacent tile | The Philares owner chooses `StandardResource` | The Philares owner, from the effect's context owner |
| **Enceladus resource colonies** | The active trader orders trade income and every colony bonus | Each colony owner chooses the card receiving their `Microbe` | Each bonus's context owner; this is independent of the trader's ordering choice |
| **Icy Impactors** | The card owner controls the action | The StartToken owner chooses the ocean location | The card owner, because the instruction explicitly says `BY` that Player |
| **World Government Terraforming** | Engine controls the solar workflow | The StartToken owner chooses the global parameter | Engine, because the instruction explicitly says `BY Engine` |
| **Homeostasis Bureau** | The surrounding operation's controller | None; the payout is concrete | The card's context owner; the unqualified trigger also defaults to matching that owner's prior change |
| **Pharmacy Union** | The operation that produced the Microbe tag | None; `-4` is concrete | The Pharmacy Union owner, from context—not from a redundant `BY Owner` |
| **Steal** | The attacker | The attacker makes any choices | The attacker. The victim owns the changed resource but is “not doing anything”; this is why Helion cannot use stolen heat as money and why Actor-sensitive protection and compensation rules work. |

`!Owner` in Philares is a complement Type bound meaning an owner other than `Owner`. It is unrelated
to either form of `BY` and unrelated to postfix instruction `!`.

## Current committed implementation

Committed tasks store an `assignee` and an `actor`. A scoped queue gives the assignee both queue
membership and revision rights. The Actor is fixed when work is produced and remains the default
for resulting changes even if another gameplay context executes the task. Instruction-side `BY`
can override that Actor.

For triggered work, `LiveEffect` chooses the assignee from the effect's Player owner, otherwise the
changed component's Player owner, otherwise the triggering Actor. It independently chooses the
effect's context owner as Actor when present and the triggering Actor otherwise. Automatic effects
use that same Actor while executing inline. There is no independent controller or narrower.

Contextual specialization of owned Types happens before the live effect fires, and trigger-side
`BY` inspects the Actor on the triggering event. These committed behaviors should survive the
identity redesign. Philares and Enceladus demonstrate why the current assignment model is not the
final semantic model.

## Next step

Characterize preparation-time handoff with a synthetic parent/child task and implement it for
Philares without changing `BY` or
future-Actor attribution. Preserve the target Enceladus behavior in which the active Player orders
two other Players' bonuses while those Players narrow their own instructions.
