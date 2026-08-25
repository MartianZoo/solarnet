# Context, assignment, and actor identity

> **Read when:** changing context specialization, event Actor attribution, task queue assignment,
> `BY`, preparation-time delegated narrowing, Philares, or Engine-selected hidden cards.
>
> **Skip when:** changing ownership as a Type dependency without task routing or attribution; read
> the dependency sections of [TYPES.md](TYPES.md).
>
> **Status:** “Four identities” through “Current task assignment” describe committed behavior.
> “Target delegation model” and “Proposed first slice” are unimplemented.

## Source map

- [`Identities.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/data/Identities.kt) — search
  for `public sealed interface Actor` for the operation identity boundary.
- [`Task.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/data/Task.kt) — inspect `assignee`
  and `actor` before changing queued work.
- [`LiveEffect.kt`](../../engine/src/commonMain/kotlin/dev/martianzoo/engine/LiveEffect.kt) — search
  for `assignee` to see trigger-time routing.
- [`EffectActorCharacterizationTest.kt`](../../engine/src/commonTest/kotlin/dev/martianzoo/engine/EffectActorCharacterizationTest.kt)
  and [`TaskAssignmentCharacterizationTest.kt`](../../engine/src/commonTest/kotlin/dev/martianzoo/engine/TaskAssignmentCharacterizationTest.kt)
  — read before changing current Actor or assignment semantics.
- [`BugsTest.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/cards/BugsTest.kt) —
  search for `Philares` for the characterized delegation gap.

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

This compatibility rule supports parts of the current Enceladus, Splice, Icy Impactors, and World
Government Terraforming behavior, but it is not a complete semantic model of control. In
particular, it does not implement the required Philares control sequence. It incorrectly assigns
Philares's resource choice to the corporation owner as soon as the adjacency occurs and lets the
active Player continue other work while that choice remains unresolved. Passing characterizations
of both defects live in `BugsTest`.

## Target delegation model

A task may eventually need three independently varying operational roles:

- **controller:** chooses which instruction in the surrounding operation proceeds;
- **narrower:** resolves choices inside that instruction; and
- **future Actor:** receives event attribution unless instruction-side `BY` overrides it.

The target is for a controller to retain an abstract parent task and decide exactly when to prepare
it. Preparation may discover that a different narrower is required, create a child choice in that
Actor's queue, and suspend the parent. While the child is outstanding, the controller cannot execute
other tasks in that control scope. Resolving the child supplies the parent's narrowing, removes the
block, and resumes the parent. Delegation changes narrowing authority; it does not silently replace
the future Actor.

Examples that constrain the design:

| Case | Controller | Narrower | Future Actor |
| --- | --- | --- | --- |
| Philares | Player placing the adjacent tile | Philares owner | Philares owner |
| Enceladus bonuses | Active trader orders bonuses | Each colony owner chooses their card | Each colony owner |
| Icy Impactors | Card owner | StartToken owner chooses an ocean | Card owner via explicit `BY` |
| World Government Terraforming | Engine workflow | StartToken owner | Engine via explicit `BY Engine` |
| Real-card deal | Player controlling the draw operation | Engine | Originating task's Actor |
| Steal | Attacker | Attacker | Attacker |
| Homeostasis Bureau | Surrounding operation controller | No choice | Card owner |
| Pharmacy Union | Operation that produced the Microbe tag | No choice | Pharmacy Union owner |

`!Owner` is a complement Type and has nothing to do with either form of `BY` or postfix
instruction `!`.

Philares is the primary sequencing constraint. The active Player controls a pending resource task
caused by that Player's placement and may resolve other eligible siblings before preparing it. Once
the active Player prepares that task, its resource choice is delegated to the Philares owner. The
active Player can do no more work in the scope until the Philares owner selects and receives the
resource. Assigning the reward directly to the Philares owner at trigger time is not an acceptable
approximation because it transfers control too early.

Real-card dealing uses the same boundary. A Player controls when an abstract
`ProjectCard<Player, Hand>` gain is prepared. Preparation delegates the remaining exact-face
narrowing to Engine, which derives the only lawful face from the seed and event history. The Player
cannot nominate a face, and cannot continue within the suspended scope while the delegated child is
outstanding.

Do not redesign attribution while implementing delegation. First characterize a synthetic
parent/child handoff and preserve every existing Actor test.

## Proposed first slice

**Not implemented.** The two passing Philares characterizations in `BugsTest` prove the current
incorrect behavior. Implement a synthetic parent/child handoff, invert those characterizations into
the desired behavioral suite, and preserve both forms of `BY` and future-Actor attribution. The
next design constraints are real-card dealing and Enceladus: the active trader must order two other
Players' bonuses while those Players narrow their own instructions. A solution that transfers the
whole operation to a delegate is therefore incomplete.
