# Identity Transition

This is the authoritative handoff for pausing the identity migration. It records the decisions we
intend to preserve, not an explanation of the current implementation. The original
[identity issue](https://github.com/MartianZoo/solarnet/issues/7) remains useful evidence of intent,
especially for the cards it calls out, but its proposed representation details, including a `Me`
type, are not requirements.

## Domain glossary

### Actor

The entity recorded as having performed a state change. For work that has not happened yet, the
Actor is the entity that will be recorded when the work is performed.

### Task owner

The entity whose pending work includes a task. The task owner chooses which of their tasks to
prepare and normally makes the choices needed to make it concrete. Once one of their tasks is
prepared, that task must execute next.

### Owner

An entity that may own game-state components. `Owner` in a card or effect is a contextual role that
must be specialized to a particular Owner before an Owner-specific result can be performed.

### Player

A seated participant in the game. A Player is both an Owner and an Actor.

### Admin

The non-player Actor that performs administrative game operations. Older discussions and current
code may call this identity `Engine`, `Npc`, or an emcee; these are not separate domain concepts.

### SoloOpponent

The passive Owner used by solo play. It is not a Player or an Actor and does not receive turns,
tasks, or gameplay authority.

## Desired stopping point

We want the smallest coherent model that keeps the Lakefront fix, correctly supports Philares, and
leaves a clean path to `SoloOpponent`. World Government Terraforming is not part of this work.

At that stopping point:

- A pending task has a **task owner**. Task ownership is the same fact as whose pending work the
  task appears in, regardless of whether the implementation stores separate queues or filtered
  views of one collection.
- A task owner may choose which of their tasks to prepare and normally owns all choices needed to
  make it concrete. A prepared task must be the next task executed.
- An **Actor** is who is or will be recorded as actually performing a state change. Do not add
  `controller`, `performer`, or another overlapping task role. A task may refer to its future Actor
  if a real case needs that information, but task owner and future Actor should remain the same
  until a counterexample requires two fields.
- Philares follows the ordinary rule: its Owner owns the triggered task, narrows it, and carries out
  its resulting state change. The Player whose tile placement created the adjacency remains the
  Actor of that placement; they do not thereby own the Philares task.
- `Admin` is the eventual name for the administrative Actor currently called `Engine`. `Npc`,
  emcee, and `Engine` do not represent additional identities.

## The `BY` rule

Authored `BY` is about the Actor of the triggering state change. It does not choose the task owner
or specialize `Owner` in the effect.

Only creation of an **unowned** component should need an authored `BY` restriction. If the created
component is owned, the trigger should express the relevant Owner in the created component's type
and match its ownership directly. An omitted `BY` adds no Actor restriction.

The engine also manufactures `BY Owner` in some effects. Our working interpretation is that this is
a temporary compatibility mechanism for specializing occurrences of `Owner` in the effect, not
evidence that the author intended to narrow the triggering Actor. Confirm that interpretation with
tests, then keep the mechanism isolated and plainly commented until Owner binding has a
representation of its own.

## Steps to the stopping point

1. **Lock down the behavior we are keeping.**
   - Keep the Lakefront regression test unchanged in purpose and passing.
   - Give every authored `BY` occurrence a direct test of the matching and rejection behavior
     appropriate to its selector.
   - Inventory every manufactured `BY Owner` occurrence and cover every one through generated or
     parameterized Owner-binding tests. Give each distinct mechanism a focused explanatory test.
     The inventory should fail when a new occurrence is added without coverage.
   - Expand Philares into a small matrix: a pair triggers exactly when one tile belongs to the
     Philares Owner and the other does not. Pairs with zero or two Philares-owned tiles do not
     trigger. Either the Philares Owner or the other tile's Owner may place the second tile that
     creates the adjacency. Also prove that the Philares Owner owns, narrows, and executes the
     resulting task while the placement remains attributed to its actual Actor.

2. **Make task ownership say what it means.**
   - Rename the task's current queue identity to `taskOwner` and make queue views derive from it.
   - Preserve the task-owner rights and prepared-task invariant above.
   - Remove comments and API wording that call queue membership “actorship.”
   - Do not add a separate task Actor until a concrete rule requires the future Actor to differ from
     the task owner.

3. **Backpedal speculative migration machinery.**
   - Use the parent of the first Owner/Player differentiation change as the comparison point; do not
     assume later code or documentation proves a requirement.
   - Remove identity lookup by guessed names and other generalization that exists only for
     hypothetical Actors or World Government.
   - Prefer the earlier, simpler ownership/default behavior wherever the newer type-system changes
     are not needed by the protected tests, Philares, or the narrow `SoloOpponent` seam.
   - Keep the useful vocabulary split—Owner, Actor, Player—even if some current implementation
     changes are rolled back.

4. **Quarantine the Owner-binding compatibility hack.**
   - Put manufactured `BY Owner` behind one clearly named operation that says it binds contextual
     `Owner` in an effect.
   - Do not expose the manufactured form as a new language rule or use it to justify broader Actor
     routing machinery.

5. **Stop when the model is boring.**
   - Run the focused identity tests and the full build.
   - Re-read the glossary and this handoff against the code, deleting stale migration commentary
     elsewhere rather than duplicating it.
   - Leave further identity work for a separate change once these invariants are easy to locate and
     the implementation has no unused identity abstractions.

## Direction after the stopping point

The next identity feature is `SoloOpponent`: an Owner-only identity with the fixed removable stock
needed by solo play, but no seat, turn, task queue, gameplay scope, corporation, hand, or ordinary
Player assets. Neutral hosting of card resources remains a separate design question; it should not
be solved by turning `SoloOpponent` into a Player.

After that concrete slice works, we may:

- rename the administrative identity from `Engine` to `Admin` throughout the code;
- give contextual Owner binding an explicit representation instead of manufacturing `BY Owner`;
- separate a task's future Actor from its task owner if an actual game rule proves they differ.

World Government Terraforming, a separate `Npc` hierarchy, generalized configured-identity lookup,
and other neutral-Actor behavior are explicitly deferred. They must not shape the stopping-point
design.
