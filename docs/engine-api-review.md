# Engine API Review

**NOTE: This is not necessarily documentation for humans - I just had Codex spit this out and I'm mining it for ideas.**


This document reviews the API surface used by functional tests, the REPL, and likely future UI
clients to play a game, inspect state, and occasionally bypass normal rules. It is intentionally
frank: the current model has good instincts, especially around capability-bearing objects, but too
many independent axes are collapsed into the same few interfaces.

## Executive Summary

1. The engine is currently mixing four separate concerns in one API family:

   | Concern | Examples today | Why it matters |
   | --- | --- | --- |
   | Visibility | one player's queue vs. whole-game queue, public vs. hidden state | A UI and a test harness need different read scopes. |
   | Authority | normal player action, workflow/engine action, test fixture edits, raw state surgery | Calling code should reveal what kind of permission it is using. |
   | Transaction boundary | plain mutation, `Timeline.atomic`, auto-exec, `onAtomicComplete` | Clients should not need to know which methods are already atomic. |
   | Domain | generic Pets engine vs. Terraforming Mars helpers | The core engine should not have to know TfM phase flow or payment conveniences. |

2. `Gameplay` is trying to express authority as nested capability interfaces, which is a sound
   direction. The problem is that every `Gameplay` can call `godMode()`, and the concrete
   `ApiTranslation` implements every layer at once. This makes the type system useful for hiding
   methods in a local variable, but weak as a real authority boundary.

3. `Game` exposes holistic objects directly: `components`, `events`, `tasks`, `timeline`, `reader`,
   `setup`, and `classes`. These are often legitimate read surfaces, but they also become ad hoc API
   escape hatches. Current clients reach into them for prompts, completions, task display, rollback,
   workflow synchronization, and assertions.

4. Atomicity is mostly centralized in `ApiTranslation`, but not consistently. Some public-looking
   `Gameplay` methods use the wrapper that also auto-executes and notifies `onAtomicComplete`; some
   call `Timeline.atomic` directly; some delegate to `Implementations` without an atomic wrapper.
   This is one reason the boundary between `Gameplay` and `Implementations` feels unclear.

5. `TfmGameplay` and `TfmWorkflow` are better understood as facades around the generic engine, not
   as core engine API. They provide useful domain concepts, but they also rely on casts, whole-game
   state, and `godMode()` because the generic engine does not yet expose explicit workflow/session
   capabilities.

6. The recommended target model is not "one perfect interface". It is a small set of named session
   objects, each representing one perspective plus one authority level: player play, workflow
   coordinator, test fixture, diagnostic observer, and raw state editor. A caller should receive the
   narrowest object that matches its role.

## Current API Inventory

### Entry Point and Root State

`Engine.newGame(setup)` creates a `Game`. The `Game` is a mutable aggregate with public read-only
views of the present, past, future, transaction system, reader, setup, and class table. It also
returns per-player `Gameplay` objects and exposes `onAtomicComplete` as a public workflow hook.

Important current facts:

1. `Game` documents that mutation should happen through `gameplay(player)`, but it also exposes
   low-level state views and `Timeline` directly.
2. `Game.gameplay(player)` returns `Gameplay`, not `TurnLayer` or `OperationLayer`.
3. In practice, the returned object is an `ApiTranslation`, and `ApiTranslation` implements
   `GodMode`, therefore every layer.
4. `Engine` creates one Koin scope per player. Each player gets a scoped `WritableTaskQueue`,
   `Changer`, `Instructor`, `Implementations`, and `ApiTranslation`.

This is a reasonable internal wiring model. The awkwardness is that the root `Game` is doing two
jobs:

1. It is the engine's aggregate state.
2. It is also the main client-facing session factory and diagnostic object.

Those jobs want different stability guarantees.

### Generic Read APIs

There are two main read styles:

1. `GameReader` is AST-oriented and generic:

   | Method | Input style | Role |
   | --- | --- | --- |
   | `resolve(Expression)` | Pets AST | Convert a prepared expression to a `Type`. |
   | `has(Requirement)` | Pets AST | Check whether a requirement is met. |
   | `count(Metric)` | Pets AST | Evaluate a metric. |
   | `count(Type)` / `containsAny(Type)` | resolved type | Count components matching a type. |
   | `countComponent(Type)` | concrete resolved type | Count exact component instances. |
   | `getComponents(Type)` | resolved type | Return concrete component types. |

2. `Gameplay` read methods are string-oriented and player-contextual:

   | Method | Input style | Role |
   | --- | --- | --- |
   | `has(String)` | Pets text | Parse/preprocess in player context, then ask `GameReader`. |
   | `count(String)` | Pets text | Same for metrics. |
   | `list(String)` | Pets text | Higher-level listing/grouping. |
   | `resolve(String)` | Pets text | Parse/preprocess in player context, then resolve. |
   | `parseInternal(...)` | Pets text | Public extension point for typed parsing. |

The distinction is useful: a UI wants player-contextual text queries; internal algorithms and test
helpers often want AST or `Type` values. However, the names do not make the distinction obvious.
`reader` sounds like the universal read API, while `Gameplay` also reads. A future UI will probably
need a first-class `PlayerView` that combines scoped reads, visible tasks, visible history, and legal
actions without exposing mutation methods by accident.

### Mutation and Capability Layers

`Gameplay` is currently layered as:

| Interface | Adds | Intended integrity level |
| --- | --- | --- |
| `Gameplay` | task revision, preparation, execution, auto-exec | Game integrity, assuming workflow controls task creation. |
| `TurnLayer` | `startTurn()`, `turn { ... }` | Turn integrity. |
| `OperationLayer` | `manual`, `beginManual`, `continueManual`, `finish` | Operation integrity. |
| `TaskLayer` | `addTasks`, `dropTask` | Task integrity only. |
| `GodMode` | `sneak` | Change integrity only; bypasses instruction machinery. |

This layering has the right shape for a capability-object design. If a variable has static type
`OperationLayer`, the caller cannot call `sneak`. The issue is that every `Gameplay` exposes
`godMode()`, and `godMode()` returns the same object with every capability.

That makes the current security model voluntary:

1. The REPL's colored modes create narrower wrappers, but they pass `gameplay.godMode()` into those
   wrappers and then cast back down to the desired layer.
2. Tests often call `godMode()` directly for fixture setup or workarounds.
3. `TfmGameplay` is a `TurnLayer` wrapper, but it can call `godMode()` because `TurnLayer` extends
   `Gameplay`.

For tests and the local REPL, this is acceptable as a transitional mechanism. For a web UI, it is
not a good boundary. A player-facing session should simply not have a method that obtains raw edit
authority.

### ApiTranslation vs. Implementations

The division is:

1. `Implementations` is internal, instruction-oriented, and mostly returns `Unit` or raw task ids.
   It expects already-parsed `Instruction` values and performs the actual task/operation algorithms.

2. `ApiTranslation` is the public adapter. It:

   1. Parses strings.
   2. Applies player-context preprocessors.
   3. Wraps many calls in `Timeline.atomic`.
   4. Produces `TaskResult`.
   5. Runs auto-exec after wrapped calls.
   6. Fires `onAtomicComplete` at outermost completion.

This split is useful, but it is leaky. Current examples:

1. `manual`, `beginManual`, `continueManual`, `finish`, `startTurn`, `doTask`, `tryTask`, and
   `doFirstTask` go through `ApiTranslation.atomic`, so they also trigger auto-exec and
   `onAtomicComplete`.
2. `reviseTask` uses `timeline.atomic` directly, not `ApiTranslation.atomic`, so it gets rollback
   but skips the adapter's auto-exec/on-complete path.
3. `sneak` also uses `timeline.atomic` directly.
4. `addTasks`, `dropTask`, `prepareTask`, and `canPrepareTask` delegate without an atomic wrapper.
   Some of these are plausibly intended as lower-level operations, but the public API does not say
   that clearly.
5. `OperationBody.autoExecNow()` calls `atomic {}` through the adapter, while `Gameplay.autoExecNow()`
   does the same. This works, but the semantic action is indirect.

The recommendation is to make transaction behavior part of the type contract. A public client
method should either be a command that returns a `TaskResult`, or be explicitly a lower-level
primitive with a name that says it must run inside an existing transaction. Mixing those under the
same interface makes clients guess.

### Task Queue Visibility

Task queues currently have both global and scoped views. `Game.tasks` is a global read-only view.
Each player scope gets a `WritableTaskQueue` filtered by owner. This is a strong direction.

There are still whole-game concepts embedded in player operations:

1. `Task.next` is a global lock. `Implementations` uses a whole-game queue view to prevent a player
   from preparing one task while another task is already prepared.
2. Auto-exec scans all queues because existing workflows rely on one player's operation draining
   automatic tasks elsewhere.
3. `TaskQueue.areAllQueuesEmpty()` and `requireAllQueuesEmpty()` are exposed on every queue view,
   even scoped views, and they inspect the whole task set.

This is defensible internally, but the API name `TaskQueue` no longer means one thing. Sometimes it
means "tasks visible to this actor"; sometimes it includes whole-game checks. A cleaner model would
separate:

1. `TaskInbox`, scoped to an actor and safe to show to that actor.
2. `TaskMonitor`, whole-game and intended for workflow/diagnostic clients.
3. `TaskEditor`, privileged and only available to fixture/debug tools.

### Terraforming Mars Facades

`TfmGameplay` wraps a `TurnLayer` and adds domain verbs:

1. `playCorp`, `playProject`, `stdAction`, `stdProject`, `cardAction1/2`, `pass`.
2. `nextGeneration`, `phase`.
3. `pay`.
4. Resource/production/global-parameter reads.

These helpers make tests readable, but they also reveal missing generic capabilities:

1. `pay` escalates to `godMode().continueManual` so it can keep working inside an existing payment
   sub-protocol.
2. `phase` uses `asPlayer(ENGINE).godMode().manual(...)` to drive phase changes.
3. `asPlayer` can mint a wrapper for any player from a wrapper for one player.

This is fine for tests, but a real client API should distinguish:

1. "Act as this player because this session is authorized for that player."
2. "Drive the game workflow as the engine."
3. "Conveniently fabricate another player's session for a test."

`TfmWorkflow` is explicitly a coordinator facade. It should probably live around the engine, not in
the engine core. It needs a whole-game task monitor, transaction checkpoints, rollback, commits, and
an event/hook that fires when tasks drain. Those are workflow capabilities, not player capabilities.

## How Current Clients Use the API

### REPL

The REPL has a `ReplSession` containing a full `Game` plus the current `TurnLayer`. It uses the
full `Game` for prompt state, task display, log display, completions, rollback, and class lookup.

The colored modes are implemented in `Access`:

| Mode | Current implementation |
| --- | --- |
| Purple | Holds `Gameplay`; denies phase/new-turn/exec. |
| Blue | Casts to `TurnLayer`; can start turns and, via another cast, phases. |
| Green | Casts to `OperationLayer`; can begin manual operations. |
| Yellow | Casts to `TaskLayer`; can drop tasks. |
| Red | Casts to `GodMode`; `exec` becomes `sneak`. |

Two important observations:

1. The REPL mode model is conceptually good. It is already a user-facing version of the capability
   model.
2. The implementation gets capabilities by always calling `gameplay.godMode()` and then narrowing
   by cast. This proves the interfaces are not currently doing the authority-granting themselves;
   the REPL wrapper is.

The `task` command shows another leak. When revising and trying a task in one command, it calls
`game.timeline.atomic { gameplay.reviseTask(...); gameplay.tryTask(...) }` directly. That means a
client command had to compose public gameplay commands by reaching for the raw timeline. This should
be an engine command, not a REPL responsibility.

### Functional Tests

The tests use three patterns:

1. Low-level engine behavior tests directly inspect `Game.tasks`, `Game.events`, and
   `Game.timeline`, and use `TaskLayer.addTasks` to create controlled task states.
2. Terraforming Mars card/game tests mostly use `TfmGameplay` helpers and `TaskResult.expect`.
3. Tests frequently use `godMode().manual`, `godMode().sneak`, and `dropTask` for fixture setup,
   unsupported-rule workarounds, or focused assertions.

The frequent use of `godMode()` is not itself a failure. Tests need fixture powers. The failure is
that different reasons for privilege all look identical:

1. Give a player enough money/resources to set up a test.
2. Skip an unsupported rule.
3. Simulate an opponent board.
4. Drive the official phase workflow.
5. Delete a task because a feature is incomplete.
6. Bypass requirements intentionally to test a later effect.

Those should be different test/facade verbs, even if they ultimately delegate to the same internal
engine primitives.

### Future Web UI

A web UI will need much stricter separation than the REPL and tests:

1. It should receive a player-scoped state projection, not the raw `Game`.
2. It should receive legal commands for the current actor, not a general string command surface plus
   `godMode`.
3. It should have a workflow/coordinator object on the server side, not in the browser.
4. It may still use Pets strings internally, but the UI-facing contract should prefer structured
   choices and task ids so the UI can render forms/buttons without parsing arbitrary text.

## Main Design Problems

### Problem 1: Capability Escalation Is Built Into the Base Interface

The idea "if you can get the object, you can do what it does" is good. The current base object
violates it because `Gameplay` includes `godMode()`. If a caller can get ordinary gameplay, the type
system cannot prevent it from escalating.

Recommendation:

1. Remove `godMode()` from the normal player-facing base over time.
2. Introduce an explicit privileged provider, for example `Game.debug()` or `TestGameFixture`, that
   is only used by tests/REPL/debug tools.
3. Make factory methods grant the desired interface directly:

   ```kotlin
   game.playerSession(player): PlayerSession
   game.workflowSession(): WorkflowSession
   game.diagnostics(): GameDiagnostics
   game.testFixture(): GameFixture
   ```

The concrete implementation may still be one object internally, but callers should not see that.

### Problem 2: Layers Encode Too Many Axes

`TurnLayer`, `OperationLayer`, `TaskLayer`, and `GodMode` are ordered by "power", but the ordering
also smuggles in workflow concepts. For example, `TaskLayer` extends `OperationLayer`, so anyone who
can add/drop tasks can also start turns and phases. That may be fine in the current REPL colors, but
it is not a general authority lattice.

Recommendation:

1. Split capabilities by role instead of inheritance depth.
2. Prefer composition of small interfaces over one tower:

   | Proposed capability | Examples |
   | --- | --- |
   | `GameQueries` | `has`, `count`, `resolve`, visible state projection. |
   | `TaskActions` | revise/prepare/do/try existing owned tasks. |
   | `TurnActions` | request/start/end turns where valid. |
   | `OperationRunner` | run a manual operation and finish it atomically. |
   | `TaskEditor` | add/drop/edit tasks for tests/debug. |
   | `RawStateEditor` | sneak raw changes. |
   | `WorkflowControl` | phase transitions, commits, task-drain waiting. |
   | `TimelineControl` | checkpoint/rollback, probably diagnostic/test/workflow only. |

3. Then define role objects as combinations:

   | Role object | Contains |
   | --- | --- |
   | `PlayerSession` | `GameQueries`, `TaskActions`, maybe `TurnActions`. |
   | `WorkflowSession` | `WorkflowControl`, `OperationRunner`, `TaskMonitor`. |
   | `ReplDebugSession` | all normal capabilities plus `TaskEditor`, `RawStateEditor`, `TimelineControl`. |
   | `TestFixture` | fixture verbs plus raw escape hatches with explicit names. |

### Problem 3: Atomicity Is an Adapter Accident

Today, clients cannot infer from the method name or interface whether the method is:

1. Failure-atomic.
2. Auto-execing afterward.
3. Producing `TaskResult`.
4. Firing workflow callbacks.

Recommendation:

1. Make every public command method return `TaskResult` and run in the standard transaction wrapper,
   unless it is explicitly named as an internal/lower-level primitive.
2. Keep lower-level primitives internal, or group them under an explicit `TransactionScope`:

   ```kotlin
   game.transaction {
     reviseTask(id, revised)
     tryTask(id)
   }
   ```

3. Avoid letting clients call `Timeline.atomic` just to compose two public commands. Add composite
   commands such as `reviseAndTryTask(id, revised)`.
4. Decide whether `addTasks`, `dropTask`, `prepareTask`, and `sneak` should auto-exec and notify
   workflow hooks. If not, their names and containing interface should say that they are primitive
   debug/task-edit operations.

### Problem 4: Global and Player Views Are Not First-Class

The current split is implemented in `WritableTaskQueue`, but the public type does not express it
well. A player queue can answer whole-game emptiness questions, and whole-game auto-exec is hidden
inside player gameplay.

Recommendation:

1. Introduce separate view types:

   | View | Audience |
   | --- | --- |
   | `PlayerGameView` | UI/player: visible components, owned tasks, public log slices. |
   | `GameMonitor` | workflow/diagnostics: whole task queue, phase, checkpoint, activity. |
   | `GameSnapshot` | immutable/state-transfer object for UI rendering and tests. |

2. Keep `GameReader` as the low-level query engine, but do not make UI clients assemble their own
   player state from raw graph queries.
3. Keep whole-game task-drain logic available to workflow code through `TaskMonitor`, not through
   every `TaskQueue`.

### Problem 5: String Commands Are Both Nice and Too Low-Level

Pets strings are excellent for tests and the REPL. They are compact and expressive. They are less
ideal as the only future UI contract because a UI needs to present structured choices.

Recommendation:

1. Keep the string API as a developer/debug API.
2. Add structured task-choice APIs as the player/UI surface:

   ```kotlin
   session.tasks(): List<VisibleTask>
   session.choices(taskId): TaskChoices
   session.choose(taskId, choice: TaskChoice): TaskResult
   ```

3. Let those APIs still compile down to Pets instructions internally.
4. Continue allowing tests to use Pets strings where they improve readability.

### Problem 6: Terraforming Mars Helpers Are Doing Three Jobs

`TfmGameplay` currently acts as:

1. A player helper DSL.
2. A phase/workflow driver.
3. A test fixture convenience object.

Recommendation:

1. Split it conceptually before splitting files:

   | Proposed facade | Examples |
   | --- | --- |
   | `TfmPlayerActions` | `playProject`, `stdProject`, `pass`, `pay`. |
   | `TfmGameFlow` | `corporationPhase`, `researchPhase`, `nextGeneration`. |
   | `TfmFixture` | `giveResources`, `placeTileIgnoringFlow`, `grantProduction`, `dropTaskForTest`. |
   | `TfmReadModel` | production map, global parameters, board text data. |

2. Keep these outside the generic engine core.
3. Make `TfmWorkflow` consume workflow/monitor capabilities instead of the whole `Game`.

## Recommended Target Model

The cleanest model is capability objects plus explicit role factories:

```kotlin
interface GameHandle {
  val setup: GameSetup
  fun player(player: Player): PlayerSession
  fun monitor(): GameMonitor
}

interface PlayerSession : GameQueries, TaskActions, TurnActions {
  val player: Player
}

interface WorkflowSession : GameMonitor, OperationRunner, WorkflowControl

interface DebugSession : PlayerSession, OperationRunner, TaskEditor, RawStateEditor, TimelineControl
```

This is illustrative, not a proposed exact API. The key properties are:

1. No player session has an escalation method.
2. Whole-game state is available through monitor/diagnostic roles, not by default from every player.
3. Test/debug authority is explicit and named.
4. Transaction behavior is uniform for public command methods.
5. Domain-specific facades wrap these roles rather than replacing them.

### Token Alternative

A token-based design is also viable:

```kotlin
class TaskEditToken internal constructor()
class RawStateToken internal constructor()

fun addTasks(token: TaskEditToken, instruction: String): TaskResult
fun sneak(token: RawStateToken, changes: String): TaskResult
```

Kotlin's non-null type guarantees are useful here: if a caller has a non-null `RawStateToken`, it
really received one from somewhere. This could reduce interface proliferation.

However, tokens are best when the same object naturally has many methods but only a few require
extra authority. In this codebase, the bigger problem is perspective: player, workflow, diagnostic,
and fixture clients need different views, not just different permissions. Therefore tokens could be
useful inside a role-object model, but should not replace it entirely.

Good places for tokens:

1. `TimelineControl` operations such as rollback/commit.
2. Raw state editing in debug/test code.
3. Temporary workflow authority granted only while a workflow owns the game.

Less good places for tokens:

1. Ordinary player task execution.
2. UI state reads.
3. Domain helpers such as `playProject`.

## Refactoring Plan

These steps are ordered to keep behavior stable and make each change reviewable.

1. Document current semantics in code names and tests.

   1. Add tests or KDoc clarifying which `Gameplay` methods are standard commands and which are
      primitive/debug operations.
   2. Rename nothing yet.
   3. Add characterization tests for atomic/on-complete behavior where it matters.

2. Introduce role aliases/wrappers without changing behavior.

   1. Add `PlayerSession` as a narrow wrapper around current `Gameplay`.
   2. Add `DebugSession` or `TestFixture` as the only obvious way to obtain `TaskLayer`/`GodMode`.
   3. Add `GameMonitor` around `Game.tasks`, `Game.events`, `Game.timeline.checkpoint`, and class
      lookup needed by the REPL.

3. Move REPL to role objects.

   1. Replace just-in-time `gameplay.godMode()` plus casts with sessions handed out by the game.
   2. Keep colored mode behavior the same.
   3. Move `task <id> <revision>` composition out of raw `Timeline.atomic` and into an engine command
      such as `reviseAndTryTask`.

4. Move tests to fixture verbs where intent is clear.

   1. Keep `godMode()` initially, but add helpers such as `fixture.give`, `fixture.placeTile`,
      `fixture.forcePhase`, `fixture.dropTask`.
   2. Convert opportunistically, starting with repeated setup patterns.
   3. Keep raw `sneak` as an escape hatch with a name like `rawChangesForTest`.

5. Split task views.

   1. Introduce `TaskInbox` for player-owned tasks.
   2. Introduce `TaskMonitor` for whole-game tasks and drain checks.
   3. Keep global prepared-task locking internal.

6. Normalize public command transaction behavior.

   1. Decide which methods are commands and make them all return `TaskResult`.
   2. Move primitive methods behind debug/test interfaces or internal transaction scopes.
   3. Remove client calls to `Timeline.atomic` except from workflow/debug components.

7. De-escalate `Gameplay`.

   1. Deprecate `Gameplay.godMode()`.
   2. Replace call sites with explicit fixture/debug/workflow providers.
   3. Eventually remove `godMode()` from player-facing interfaces.

8. Separate Terraforming Mars facades.

   1. Keep `TfmGameplay` source-compatible while adding narrower wrappers.
   2. Move phase/workflow helpers to `TfmGameFlow`.
   3. Move board/resource projections to a read model.
   4. Make `TfmWorkflow` depend on workflow/monitor roles instead of `Game`.

## Suggested Names

Names matter here because the types are supposed to teach callers what authority they have.

| Current name | Possible replacement | Rationale |
| --- | --- | --- |
| `Gameplay` | `PlayerSession` or `PlayerActions` | It is player-scoped and active. |
| `TurnLayer` | `TurnActions` | Capability, not layer. |
| `OperationLayer` | `OperationRunner` | Runs an operation with task-drain semantics. |
| `TaskLayer` | `TaskEditor` | This is not ordinary play; it edits queues. |
| `GodMode` | `RawStateEditor` or `DebugStateEditor` | Says what it actually does. |
| `Game.tasks` | `game.monitor().tasks` | Whole-game diagnostic/workflow view. |
| `OperationBody` | `OperationScope` | It is the scope available inside an operation. |
| `sneak` | `applyRawChanges` | Less cute, more precise for docs/API. |

I would keep `godMode()` only as a temporary compatibility alias, then deprecate it once call sites
have more intention-revealing replacements.

## What Not To Do

1. Do not try to solve this by making one giant final interface. The current pain comes partly from
   too many meanings being attached to one object.
2. Do not remove Pets string commands from tests or the REPL. They are productive developer APIs.
3. Do not make the web UI speak raw `Game`, `TaskQueue`, and arbitrary Pets strings as its primary
   contract. It should get a projection and structured choices.
4. Do not treat all `godMode()` usage as equally bad. Test setup, missing-rule workarounds, and
   phase workflow control are different needs and should get different replacement APIs.
5. Do not move TfM workflow deeper into the generic engine. The generic engine should provide
   workflow primitives; TfM should decide what phases and turns mean.

## Bottom Line

The current API is not hopeless; it has the seed of the right model. The nested interfaces are a
reasonable early attempt at capability objects, and the scoped Koin wiring already gives each player
their own implementation context. The main correction is to stop treating "more methods on the same
object" as the authority model.

The next model should be:

1. Role objects for perspective.
2. Capability interfaces for authority.
3. Uniform transaction semantics for public commands.
4. Explicit debug/test fixtures for cheats.
5. Domain-specific facades around the generic engine, not mixed into it.

If we move gradually in that direction, most refactorings can be compatibility-preserving: add
better-named wrappers first, migrate clients second, then deprecate the old `godMode()`/cast paths
once they are no longer carrying real work.
