# Desired end state

We have the smallest coherent identity model that preserves the Lakefront Resorts fix, correctly
supports Philares, and leaves a narrow, clean path to `SoloOpponent`:

- Every pending task has one **assignee**, meaning the entity whose pending work contains the task
  and who chooses which task in that work to prepare. Assignment alone does not settle who makes
  every choice exposed by preparation.
- Every state change records one **Actor**, meaning the entity that actually performed the change.
  The Actor comes from the gameplay context executing an instruction, not from task assignment.
  Task assignment and state-change attribution remain separate concepts even while the same Player
  commonly fills both roles.
- Philares establishes two separate facts: the active Player's placement triggers work within that
  Player's operation, while the Philares Owner chooses the resulting standard resource. The exact
  queue handoff during preparation, and which gameplay ultimately executes the narrowed result,
  remain deliberately unsettled.
- Authored `BY` consistently filters the Actor of the triggering change. Contextual `Owner` binding
  is explicit and isolated instead of leaking into Actor selection.
- The implementation contains no identity machinery justified only by hypothetical Actors, World
  Government Terraforming, or a speculative generalized identity model.
- The stable identity vocabulary and invariants are easy to locate in code, tests, and maintained
  documentation, and we have done the standard cleanup checks.

`SoloOpponent`, the `Engine`-to-`Admin` rename, an explicit language representation for contextual
Owner binding, and the exact cross-owner preparation handoff are meaningful follow-on changes, not
part of this stopping point. World Government Terraforming, a separate `Npc` hierarchy,
generalized configured-identity lookup, neutral-Actor behavior, and neutral hosting of card
resources are also out of scope.

Starting commit: `aaaa6313385665941475e613bca39e099eb3d6c6`

Approval status: Original stopping-point scope approved by user on 2026-07-18; cross-owner
preparation constraints clarified by user on 2026-07-18, with their representation still deferred.

## Important decisions

### Domain glossary

- **Actor:** The entity whose gameplay context executes an instruction and which is recorded as
  having performed each resulting state change.
- **Assignee:** The entity whose pending work includes a task. The assignee chooses which of their
  tasks to prepare. Assignment does not by itself determine who makes choices exposed by
  preparation or which Actor will execute the result.
- **Owner:** An entity that may own game-state components. `Owner` in an [effect
  representation](../glossary.md#effect-representations) is a
  contextual role that must be specialized to a particular Owner before an Owner-specific result
  can be performed. For an active effect, ownership of the component carrying the corresponding
  component effect is also evidence for who is entitled to make choices within it.
- **Player:** A seated participant in the game. A Player is both an Owner and an Actor.
- **Admin:** The non-player Actor that performs administrative game operations. Older discussion
  and current code may call this identity `Engine`, `Npc`, or an emcee; these are not separate
  domain concepts.
- **SoloOpponent:** The passive Owner used by solo play. It is not a Player or an Actor and does not
  receive turns, tasks, or gameplay authority.

### Task assignment

- Task assignment is the same fact as whose pending work contains the task, whether the
  implementation uses separate queues or filtered views of one collection.
- An assignee may choose which of their tasks to prepare. Preparation may expose a choice belonging
  to the Owner of the component whose effect produced the task. Current evidence suggests that the
  active task then remains suspended while some corresponding work is placed in that Owner's queue,
  but does not yet settle whether this is a moved task, a linked response task, or another mechanism.
- A prepared task must prevent the assignee's other work from proceeding until every choice needed
  for that task has been supplied. Do not generalize this into a whole-game identity model before
  the cross-owner handoff itself is understood.
- Do not add `controller`, `performer`, future Actor, or another field merely to give the unsettled
  responsibility a name. Conversely, do not use the absence of such a field to claim that the
  assignee necessarily owns every decision. First determine whether the responsibility belongs on
  a linked task or can be derived from the source effect recorded in `Cause`.
- Philares requires the Player whose placement created the adjacency to retain responsibility for
  their operation while the Philares Owner chooses the reward. It does not yet prove who should be
  assigned the initial triggered task, what temporary work the Owner receives, or which Actor should
  execute the narrowed result.

### Authored `BY` and contextual Owner binding

- Authored `BY` is solely a restriction on the Actor of the triggering state change. It does not
  choose the assignee or specialize `Owner` in the effect.
- Only creation of an unowned component should need an authored `BY` restriction. An owned
  component's trigger should express and match the relevant Owner in the component type. Omitting
  `BY` adds no Actor restriction.
- The engine-manufactured `BY Owner` form is a temporary class-effect encoding that preserves
  contextual `Owner` occurrences until they can be bound. It is not an authored language rule and
  must not be evidence for broader Actor-routing machinery.
- Preserve the useful Owner/Actor/Player vocabulary split even if speculative implementation work
  is removed.

### Historical evidence

- The original [identity issue](https://github.com/MartianZoo/solarnet/issues/7) is evidence of
  intent, especially for the named cards, but its proposed representation details (including a
  `Me` type) are not requirements.
- The intended Splice representation gives each Player a hidden owned component containing that
  Player's copy of the component effect. This is evidence that ownership of the effect-bearing
  component identifies an important decision responsibility independently of initial task
  assignment.
- Use `420396b911c530d9b7dda8c1435fbdd853a1bcd2`, the parent of the first Owner/Player
  differentiation commit, as the comparison point when identifying speculative migration
  machinery. Later code and documentation are evidence, not proof of a requirement.

## Ordered implementation steps

1. Compare the post-safety-net implementation with the historical baseline and remove identity
   lookup by guessed names plus other generalization needed only by hypothetical Actors or World
   Government. Prefer the earlier ownership/default behavior wherever the newer type-system work
   is not required by protected rule facts around Philares or the narrow `SoloOpponent` seam. Do
   not treat the current Philares queue topology as one of those rule facts.
2. Keep the remaining contextual Owner-binding behavior narrow and explicit at the sites that need
   it. Keep the manufactured `BY Owner` form internal and plainly documented as a temporary
   class-effect representation rather than a new Pets rule.
3. Run the focused identity tests and the full build. Reconcile the glossary and maintained project
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
  - **Why deferred:** The temporary class-effect encoding must first be fully inventoried and tested
    so the replacement has a precise contract.
  - **Evidence that will resolve it:** The protected Owner-binding test matrix and explicit binding
    call sites defining the behavior the new representation must preserve.
- **Decision:** Represent preparation of an active Player's task when an active effect carried by
  another Owner's component requires that Owner to make a choice.
  - **Why deferred:** Philares, Enceladus, and the intended Splice representation establish the
    distinct responsibilities, but not whether preparation should move work, create a linked
    response task, or use another minimal mechanism. Settling that now would complicate this
    identity cleanup unnecessarily.
  - **Evidence that will resolve it:** Focused tests in which the active Player selects a triggered
    task, the effect Owner supplies its narrowing choice through their own queue, the active
    Player's remaining work stays suspended, and resolution resumes with explicit Actor
    attribution.
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
- Every authored canonical `BY` source effect is protected by a change-detecting inventory and direct card
  behavior coverage appropriate to its selector. The Philares matrix now proves that exactly one
  tile must belong to its Owner, either Player may create the adjacency, the Philares Owner chooses
  the reward, and the placement retains its actual Actor. The matrix currently also characterizes
  the Owner as assignee and executor; those details are implementation evidence rather than settled
  requirements pending the cross-owner preparation decision.
- All 139 `BY Owner` class effects are counted, the single authored Lakefront source effect is
  distinguished from the 138 forms manufactured during class-effect transformation, and every
  manufactured class effect's instruction is exercised through Owner binding. Focused
  characterization tests explain the manufacturing mechanism and its runtime performer-binding
  behavior.
- Characterization coverage also protects `BY Anyone`, `BY Player`, `BY Owner`, repeated Owner
  binding, and current assignee-routing behavior before model changes.
- `Task.assignee` now records whose pending work contains each task, scoped queue views derive from
  it, and task APIs, diagnostics, tests, and documentation use assignment terminology for queue
  concerns. Current task APIs also grant the assignee narrowing authority; that is now explicitly a
  characterization of the implementation, not a domain invariant. Gameplay and `ChangeEvent`
  retain `actor` for execution and attribution; task lifecycle events expose their assignee instead.
