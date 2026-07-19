# Desired end state

We have the smallest coherent identity model that preserves the Lakefront Resorts fix, correctly
supports Philares, and leaves a narrow, clean path to `SoloOpponent`:

- Every pending task has one **assignee**, meaning the entity whose pending work contains the task
  and who is entitled to choose, prepare, and normally execute it. A prepared task must be the next
  task executed.
- Every state change records one **Actor**, meaning the entity that actually performed the change.
  Task assignment and state-change attribution remain separate concepts even while the same Player
  commonly fills both roles.
- Philares follows the ordinary rule: its Owner is assigned, narrows, and carries out the triggered
  task, while the tile placement remains attributed to the Actor that actually made it.
- Authored `BY` consistently filters the Actor of the triggering change. Contextual `Owner` binding
  is explicit and isolated instead of leaking into Actor selection.
- The implementation contains no identity machinery justified only by hypothetical Actors, World
  Government Terraforming, or a speculative generalized identity model.
- The stable identity vocabulary and invariants are easy to locate in code, tests, and maintained
  documentation, and we have done the standard cleanup checks.

`SoloOpponent`, the `Engine`-to-`Admin` rename, and an explicit language representation for
contextual Owner binding are meaningful follow-on changes, not part of this stopping point. World
Government Terraforming, a separate `Npc` hierarchy, generalized configured-identity lookup,
neutral-Actor behavior, and neutral hosting of card resources are also out of scope.

Starting commit: `aaaa6313385665941475e613bca39e099eb3d6c6`

Approval status: Approved by user on 2026-07-18

## Important decisions

### Domain glossary

- **Actor:** The entity recorded as having performed a state change. For pending work, the future
  Actor is the entity that will be recorded when the work is performed.
- **Assignee:** The entity whose pending work includes a task. The assignee chooses which of their
  tasks to prepare and normally makes the choices needed to make it concrete. Once one of their
  tasks is prepared, that task must execute next.
- **Owner:** An entity that may own game-state components. `Owner` in a card or effect is a
  contextual role that must be specialized to a particular Owner before an Owner-specific result
  can be performed.
- **Player:** A seated participant in the game. A Player is both an Owner and an Actor.
- **Admin:** The non-player Actor that performs administrative game operations. Older discussion
  and current code may call this identity `Engine`, `Npc`, or an emcee; these are not separate
  domain concepts.
- **SoloOpponent:** The passive Owner used by solo play. It is not a Player or an Actor and does not
  receive turns, tasks, or gameplay authority.

### Task assignment

- Task assignment is the same fact as whose pending work contains the task, whether the
  implementation uses separate queues or filtered views of one collection.
- An assignee may choose which of their tasks to prepare and normally makes all choices needed to
  make it concrete. A prepared task must execute next because its preparation may have read mutable
  game state.
- Do not add `controller`, `performer`, or another overlapping task role. A task may refer to its
  future Actor when a real case needs that information, but assignee and future Actor remain the
  same until a counterexample requires separate fields.
- Philares's Owner is the assignee of the triggered task. The Player whose placement created the
  adjacency remains the Actor of that placement and does not thereby become the Philares task's
  assignee.

### Authored `BY` and contextual Owner binding

- Authored `BY` is solely a restriction on the Actor of the triggering state change. It does not
  choose the assignee or specialize `Owner` in the effect.
- Only creation of an unowned component should need an authored `BY` restriction. An owned
  component's trigger should express and match the relevant Owner in the component type. Omitting
  `BY` adds no Actor restriction.
- The engine-manufactured `BY Owner` form is a temporary compatibility mechanism for binding
  contextual `Owner` occurrences in an effect. It is not an authored language rule and must not be
  evidence for broader Actor-routing machinery.
- Preserve the useful Owner/Actor/Player vocabulary split even if speculative implementation work
  is removed.

### Historical evidence

- The original [identity issue](https://github.com/MartianZoo/solarnet/issues/7) is evidence of
  intent, especially for the named cards, but its proposed representation details (including a
  `Me` type) are not requirements.
- Use `420396b911c530d9b7dda8c1435fbdd853a1bcd2`, the parent of the first Owner/Player
  differentiation commit, as the comparison point when identifying speculative migration
  machinery. Later code and documentation are evidence, not proof of a requirement.

## Ordered implementation steps

1. Lock down the protected behavior before changing the identity model. Keep the Lakefront Resorts
   regression unchanged in purpose and passing. Establish a change-detecting inventory of every
   authored canonical `BY` effect and direct positive/rejection tests appropriate to each selector.
   Establish a corresponding inventory for every engine-manufactured `BY Owner` path, with
   generated or parameterized binding coverage plus a focused explanatory test for each distinct
   mechanism. Complete the Philares matrix: exactly one Philares-owned tile is required; either
   Owner may place the second tile; and the Philares Owner is assigned, narrows, and executes the
   resulting task while the placement keeps its actual Actor. This is the first meaningful
   implementation slice after the plan-relocation commit.
2. Rename the task's transitional `actor` identity to `assignee` throughout the data model and
   queue APIs. Make scoped queue views derive from that field, preserve assignee choice and the
   prepared-task invariant, and remove wording that calls queue membership actorship. Do not add a
   separate task Actor.
3. Compare the post-safety-net implementation with the historical baseline and remove identity
   lookup by guessed names plus other generalization needed only by hypothetical Actors or World
   Government. Prefer the earlier ownership/default behavior wherever the newer type-system work
   is not required by the protected tests, Philares, or the narrow `SoloOpponent` seam.
4. Put the remaining contextual Owner-binding compatibility behavior behind one clearly named
   engine operation. Keep the manufactured `BY Owner` form internal, isolated, and plainly
   documented as a compatibility representation rather than a new Pets rule.
5. Run the focused identity tests and the full build. Reconcile the glossary and maintained project
   documentation with the implementation, delete stale migration commentary instead of duplicating
   it, record any deliberately deferred improvements in `TODO.md`, and perform the standard diff
   and cleanup review. Stop once the model is intentionally boring and contains no unused identity
   abstractions.

## Deferred decisions

- **Decision:** Introduce `SoloOpponent` as an Owner-only identity with the fixed removable stock
  needed by solo play.
  - **Why deferred:** It is the next concrete feature after this stopping point and should validate
    the seam rather than shape the cleanup speculatively.
  - **Evidence that will resolve it:** Focused solo setup and stock-removal tests showing that it
    needs no seat, turn, task queue, gameplay scope, corporation, hand, or ordinary Player assets.
- **Decision:** Rename the administrative runtime identity from `Engine` to `Admin`.
  - **Why deferred:** The vocabulary is settled, but a broad rename would obscure the behavioral
    and ownership changes in this transition.
  - **Evidence that will resolve it:** Completion of the stopping-point cleanup with all remaining
    administrative-identity call sites identified and no competing `Engine` domain meaning.
- **Decision:** Give contextual Owner binding an explicit representation instead of manufacturing
  `BY Owner`.
  - **Why deferred:** The compatibility mechanism must first be fully inventoried, tested, and
    isolated so the replacement has a precise contract.
  - **Evidence that will resolve it:** The protected Owner-binding test matrix and one isolated
    operation defining all behavior the new representation must preserve.
- **Decision:** Separate a task's future Actor from its assignee.
  - **Why deferred:** No current rule requires them to differ, and adding the field now would create
    overlapping identity concepts.
  - **Evidence that will resolve it:** A concrete game rule whose correct assignee cannot also be
    the Actor recorded when the prepared work executes.
- **Decision:** Model neutral hosting of card resources.
  - **Why deferred:** It is a distinct design problem and must not turn `SoloOpponent` into a
    Player.
  - **Evidence that will resolve it:** A concrete neutral-hosting rule and tests that expose the
    exact ownership and lifecycle requirements.

## Already done

- The approved identity plan was moved from `docs/` to `plans/` in
  `9b64b5b6fb323383c4cd1619176046c031035aab`; repository links now follow its maintained location.
- The Owner, Actor, Player, Admin, task-assignment, and SoloOpponent concepts and the intended
  stopping point were consolidated in the identity handoff at starting commit
  `aaaa6313385665941475e613bca39e099eb3d6c6`.
- The Lakefront Resorts Actor/Owner regression was fixed and protected in
  `1b8fc651fa8c2ce51dc8d523d476e98572c4d614`.
- Initial characterization coverage exists for `BY Anyone`, `BY Player`, `BY Owner`, repeated Owner
  binding, assignee choice, and the central Philares assignment case; the ordered safety-net step
  deliberately strengthens this into complete, change-detecting coverage before model changes.
