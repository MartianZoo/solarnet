# Context, assignment, and actor identity

**Status:** The first four sections describe committed behavior. “Target delegation model” is a
proposal.

## Four identities

Pets behavior is interpreted in context. Keep these roles separate:

- **Context component:** the concrete component whose declaration supplied an Effect, Instruction,
  Requirement, or Metric.
- **Context owner:** the owner of that component, or the Player scope through which an ad hoc
  instruction entered the engine.
- **Assignee:** the Actor whose task queue contains deferred work and whose scoped gameplay may
  narrow it.
- **Actor:** the entity credited with the resulting change. Trigger-side `BY` matches this value.

A future delegation model adds a fifth role, the **controller**, which owns a surrounding operation
while another Actor makes a nested choice.

## Context specialization

Contextual specialization happens before a `LiveEffect` exists. A bare `Plant` inherited by an
owned card becomes that card owner's plant. An ownerless class effect may retain `Plant<Owner>`
until inheritance by a concrete owned component binds it. This Type specialization is independent
of task routing and Actor attribution.

## Actor

An instruction's Actor defaults to its context owner. If there is no context owner, it falls back to
the surrounding execution Actor. Instruction-side `BY` explicitly overrides the default.

The default binds when work is produced. A pending or queued task remembers its future Actor in
`Task.actor`; later execution through another gameplay scope does not steal attribution. Splitting,
revision, preparation, and `THEN` continuation preserve it.

A `ChangeEvent` records that Actor. Trigger-side `BY` inspects only the event's Actor. This is why
stealing a victim's heat is still an action by the attacker.

## Current task assignment

Committed tasks store `assignee` and `actor`. The assignee controls queue membership and narrowing
rights. The Actor is independent.

For queued triggered work, `LiveEffect` chooses the assignee in this order:

1. Player owner of the effect-bearing component;
2. Player owner of the changed component;
3. triggering Actor.

It chooses the context owner as Actor when present and otherwise keeps the triggering Actor.
Automatic effects use the same Actor but execute inline without entering a queue.

This compatibility rule supports current Philares, Enceladus, Splice, Icy Impactors, and World
Government Terraforming behavior, but it is not a complete semantic model of control.

## Target delegation model

A task may eventually need three independently varying operational roles:

- **controller:** chooses which instruction in the surrounding operation proceeds;
- **narrower:** resolves choices inside that instruction; and
- **future Actor:** receives event attribution unless instruction-side `BY` overrides it.

The target is for a controller to retain an abstract parent task. Preparing it may discover that a
different narrower is required, create a child choice in that Actor's queue, suspend the parent, and
resume it when the child becomes concrete.

Examples that constrain the design:

| Case | Controller | Narrower | Future Actor |
| --- | --- | --- | --- |
| Philares | Player placing the adjacent tile | Philares owner | Philares owner |
| Enceladus bonuses | Active trader orders bonuses | Each colony owner chooses their card | Each colony owner |
| Icy Impactors | Card owner | StartToken owner chooses an ocean | Card owner via explicit `BY` |
| World Government Terraforming | Engine workflow | StartToken owner | Engine via explicit `BY Engine` |
| Steal | Attacker | Attacker | Attacker |
| Homeostasis Bureau | Surrounding operation controller | No choice | Card owner |
| Pharmacy Union | Operation that produced the Microbe tag | No choice | Pharmacy Union owner |

`!Owner` is a complement Type and has nothing to do with either form of `BY` or postfix
instruction `!`.

Do not redesign attribution while implementing delegation. First characterize a synthetic
parent/child handoff and preserve every existing Actor test.

## Proposed first slice

**Not implemented.** Characterize preparation-time handoff with a synthetic parent/child task, then
apply it to Philares without changing either form of `BY` or future-Actor attribution. The next
design constraint is Enceladus: the active trader must order two other Players' bonuses while those
Players narrow their own instructions. A solution that handles Philares by transferring the whole
operation to its owner is therefore incomplete.
