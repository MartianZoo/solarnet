# Engine API Review

**NOTE:** This is a collection of API ideas, not a requirements document.

This document reviews the engine API surface used by tests, the REPL, and future clients. It is not
trying to design the final engine internals. The near-term goal is to make the public capabilities
coherent while allowing the current internals to keep working.

The main conclusion is that the API should be designed around injectable capability objects. A
client should ask for the capabilities it needs, such as player actions, task monitoring, workflow
control, or debug fixture powers. Wiring outside the client decides whether it gets them. The client
should not navigate from `Game` to `Gameplay` to `godMode()` to casts.

## Direction

1. Keep the engine internals and public API in the same module for now.

   Eventually, most engine machinery may become `internal`, with public clients using a sibling
   `api` package. But this should wait. First prove the shape of the public capability objects,
   migrate clients to them, and only then hide internals behind module boundaries.

2. Treat dependency injection as a first-class API experience.

   The cleanest client code should look like ordinary constructor injection:

   ```kotlin
   class ReplTaskCommand(
     private val tasks: PlayerTaskActions,
     private val inbox: TaskInbox,
     private val results: ResultPresenter,
   )
   ```

   The command does not need to know where `PlayerTaskActions` came from, whether it is backed by
   `ApiTranslation`, or how it coordinates with auto-exec. This matches the current Koin direction
   and gives capability objects a concrete purpose.

3. Model auto-exec as an injected helper/agent, not as a hardwired property of every gameplay
   object.

   Auto-exec is a convenience that helps an assignee drain its own pending work after it asks the
   API to do something. It should be plugged into the command lifecycle so the `TaskResult` includes
   both the explicit command and any automatically executed follow-up work. It should not be a
   global license for one player action to drain other players' choices.

4. Keep string APIs for now.

   Pets strings are valuable in tests and the REPL. If later we want typed commands, we can add
   contextual parser objects such as `PlayerSyntax.instr("...")`. This is not the main source of
   disorder.

5. Move Terraforming Mars-specific APIs toward a separate module/facade layer.

   The generic engine should know about identities, tasks, components, and transactions. A TfM
   workflow facade may use the administrative Actor for bookkeeping. It
   should not own Terraforming Mars phase workflow,
   board projections, payment conveniences, or card-play helper DSLs.

## Current Shape

### Root Game Object

`Engine.newGame(setup)` creates a `Game`. `Game` exposes:

1. `components`: current component graph.
2. `events`: event log.
3. `tasks`: whole-game task view.
4. `timeline`: checkpoints, rollback, atomic blocks.
5. `reader`: Pets/AST-oriented state reader.
6. `setup` and `classes`.
7. `gameplay(player)`.
8. `onAtomicComplete`.

This is useful for diagnostics and tests, but it is too much for ordinary clients. It encourages
reach-through when clients want only one role-specific capability.

Near-term recommendation: keep `Game` as the aggregate and compatibility object, but start adding
role objects that clients can receive directly. Do not require clients to discover capabilities by
starting from `Game`.

### Gameplay Layers

`Gameplay` is currently a nested tower:

| Interface | Adds |
| --- | --- |
| `Gameplay` | read methods, task revision/preparation/execution, auto-exec controls, `godMode()` |
| `TurnLayer` | `startTurn`, `turn` |
| `OperationLayer` | `manual`, `beginManual`, `continueManual`, `finish` |
| `TaskLayer` | `addTasks`, `dropTask` |
| `GodMode` | `sneak` |

The original instinct is good: an object represents a set of capabilities. The problem is that
`Gameplay` includes `godMode()`, and `ApiTranslation` implements all layers at once. That turns the
capability model into a local hiding convention rather than an authority boundary.

Current clients then compensate manually:

1. The REPL obtains `gameplay.godMode()` and casts it back down for colored modes.
2. Tests call `godMode()` for fixture setup, missing-rule workarounds, and raw state edits.
3. `TfmGameplay` wraps `TurnLayer` but can still call `godMode()` because `TurnLayer` extends
   `Gameplay`.

This does not need to be fixed by immediately deleting `godMode()`. A safer path is to introduce
better-named injectable capabilities first, migrate clients, and deprecate `godMode()` only after it
is no longer the main way to express legitimate needs.

### ApiTranslation and Implementations

`Implementations` contains the instruction/task algorithms. It is internal, parsed-instruction
oriented, and mostly returns `Unit`, task ids, or raw events.

`ApiTranslation` is the public-ish adapter. It parses strings, preprocesses in player context,
wraps many calls in `Timeline.atomic`, runs auto-exec, returns `TaskResult`, and fires
`onAtomicComplete`.

That split is useful, but transaction behavior is inconsistent:

1. `manual`, `beginManual`, `continueManual`, `finish`, `startTurn`, `doTask`, `tryTask`, and
   `doFirstTask` use the adapter's `atomic` wrapper.
2. `reviseTask` and `sneak` use `timeline.atomic` directly.
3. `addTasks`, `dropTask`, `prepareTask`, and `canPrepareTask` delegate without a public
   transaction wrapper.
4. Some client code, such as the REPL `task` command, reaches for `game.timeline.atomic` to compose
   operations.

The fix is not primarily package structure. The fix is to make a single command pipeline explicit.

## Proposed Command Pipeline

Public API commands should run through one command runner:

```text
checkpoint
run explicit command body
run this actor's auto-exec policy
collect TaskResult from all activity since checkpoint
rollback on failure
publish after-command/after-commit notifications
return TaskResult
```

The command runner is the place where transaction semantics live. Internal engine operations should
mostly assume they are already inside a command/transaction.

### Command Runner Responsibilities

1. Create and own the checkpoint.
2. Roll back if the command fails.
3. Run post-command agents such as auto-exec.
4. Include all command and auto-exec activity in one `TaskResult`.
5. Notify workflow/monitor listeners after the outermost successful command.
6. Keep nested command behavior coherent.

This is close to what `ApiTranslation.atomic` already does, but it should become a named service
instead of an adapter detail.

Possible names:

1. `CommandRunner`
2. `GameCommandRunner`
3. `TransactionRunner`
4. `TurnCommandRunner`

I slightly prefer `CommandRunner` for the public API layer because it describes what clients do. The
lower-level implementation can still use `Timeline` and checkpoints.

### Who Gets Transaction Control?

Most clients should not compose arbitrary transactions. If a client has direct rollback/roll-forward
timeline access, it is already highly privileged and can be given composition tools too. Otherwise,
it should ask for named command capabilities.

That suggests:

1. Normal player clients get methods like `doTask`, `reviseTask`, `choose`, `pass`, and perhaps
   named composite commands like `reviseAndTryTask`.
2. Debug, test, and workflow clients may get `TimelineControl` or `CommandComposer`.
3. The REPL's rollback command is a privileged diagnostic command, not a normal player command.

### AutoExecutor as Agent

Auto-exec should become an agent plugged into the command runner.

Important policy choices:

1. It drains only the assignee's own pending work.
2. It runs after the initiating Actor's explicit command, before the returned `TaskResult` is finalized.
3. It may have modes such as `NONE`, `SAFE`, and `FIRST`.
4. Its state/policy can be injected per assignee or session.
5. It should not silently resolve another human player's meaningful choice.

This implies a conceptual split:

| Current concept | Proposed concept |
| --- | --- |
| `Gameplay.autoExecMode` | Session-scoped `AutoExecPolicy` or `AutoExecutor` configuration |
| `impl.autoExecNow(mode)` | Internal service used by `AutoExecutor` |
| whole-game autoexec scan | Workflow/Admin concern or special privileged policy, not default player behavior |

Administrative operations may still be attributed to a non-player Actor. Its domain name is
`Admin`; current code may call it `ENGINE`. It is not a Player, an Owner, or a separate `Npc` role.

## Capability Objects

The public API should be a set of small interfaces that can be injected independently. These
interfaces can still coordinate through shared internals.

### Core Capabilities

| Capability | Purpose |
| --- | --- |
| `GameQueries` | Player-contextual `has`, `count`, `resolve`, maybe `list`. |
| `PlayerTaskActions` | Act on tasks assigned to this player: revise, prepare, do, try. |
| `TaskInbox` | Read this player's visible tasks. |
| `TaskMonitor` | Read whole-game task state for workflow/diagnostics. |
| `OperationRunner` | Run a manual operation with command transaction semantics. |
| `TurnActions` | Start/request/complete turns where that is a public concept. |
| `TimelineControl` | Checkpoint, rollback, commit; privileged. |
| `DebugTaskEditor` | Add/drop/edit tasks for tests and debug tools. |
| `RawStateEditor` | Apply raw component changes; privileged. |
| `AutoExecutor` | Drain one assignee's pending work according to an injected policy. |

These do not need to form one inheritance tower. A role receives whichever capabilities make sense.

### Example Role Bundles

| Role | Typical injected capabilities |
| --- | --- |
| Player UI/session | `GameQueries`, `TaskInbox`, `PlayerTaskActions`, maybe `TurnActions` |
| REPL green mode | `GameQueries`, `TaskInbox`, `OperationRunner`, `TurnActions` |
| REPL red mode | green mode plus `RawStateEditor` and `DebugTaskEditor` |
| Workflow/Admin | `TaskMonitor`, `OperationRunner`, `TimelineControl`, task-drain notifications |
| Test fixture | `DebugTaskEditor`, `RawStateEditor`, `TimelineControl`, high-level fixture helpers |
| Board renderer | `GameQueries`, maybe a TfM read model |

This is where DI becomes valuable. A client can depend on disconnected-looking interfaces, but the
composition root wires all of them to the same game/session.

For example:

```kotlin
class TfmBoardPresenter(
  private val queries: GameQueries,
  private val tfmReadModel: TfmReadModel,
)

class TfmWorkflowAgent(
  private val adminOps: OperationRunner,
  private val tasks: TaskMonitor,
  private val timeline: TimelineControl,
)

class CardTestFixture(
  private val raw: RawStateEditor,
  private val taskEditor: DebugTaskEditor,
  private val player: PlayerTaskActions,
)
```

None of these classes should need to call `game.gameplay(player).godMode() as Something`.

### Tokens

Token objects are still available if a specific problem calls for them. They should not be the
default model.

Good token use cases:

1. A rare method on a mostly-normal interface needs extra proof of authority.
2. A workflow lease should expire or be scoped tightly.
3. A test-only operation should be hard to call accidentally.

But most authority should be expressed by injected capabilities, because that is clearer for client
code and easier to wire with Koin.

## Task Visibility

The current task queue model is halfway to the right place. Player-scoped `WritableTaskQueue` views
already exist internally, while `Game.tasks` is a global read-only view.

The API should make that distinction explicit:

1. `TaskInbox`: this assignee's pending tasks.
2. `TaskMonitor`: whole-game task state, for workflow, REPL diagnostics, and tests.
3. `DebugTaskEditor`: privileged task mutation.

Whole-game concepts such as "is any queue empty?" and "which task is globally prepared?" should not
be methods on every player inbox. They are monitor/engine concerns.

One subtlety remains: `Task.next` is currently a global lock because preparing a task reads game
state. That invariant can stay internal. The player action API can still say "prepare my task"; the
engine can reject it if another prepared task has the lock.

## Terraforming Mars Layer

Terraforming Mars-specific helpers should move toward a separate module/facade layer. They can be
extension functions when that makes the call site natural.

Good extension-function candidates:

```kotlin
fun PlayerTaskActions.playProject(...)
fun PlayerTaskActions.stdProject(...)
fun GameQueries.temperatureC(): Int
fun GameQueries.oxygenPercent(): Int
fun RawStateEditor.giveTfmResourcesForTest(...)
```

The rule of thumb: an extension should attach to the capability it actually needs.

Examples:

1. `playProject` should extend a normal player action capability only if it can be implemented as a
   normal player operation.
2. `giveResourcesForTest` should extend a fixture/debug capability, not a player capability.
3. `phase` and `nextGeneration` should belong to a TfM workflow/Admin facade, not ordinary player
   actions.
4. Board display helpers should depend on a TfM read model or query capability, not on full `Game`.

This structure also lets the generic engine stop knowing Terraforming Mars workflow. It only needs
to support actors, commands, tasks, transactions, and read models well enough for TfM to build on.

## Strings and Parsing

String methods are not the urgent problem. Keep them where they help tests and the REPL.

If they become awkward, introduce contextual syntax services rather than global parsers:

```kotlin
val instruction = playerSyntax.instr("Plant")
val requirement = playerSyntax.reqt("MAX 0 Temporary")
```

Parsing is contextual because defaults, aliases, `Owner`, production sugar, and atomization depend
on the acting player and loaded classes. A global `instr("...")` would hide that context.

The command APIs can gradually accept typed `Instruction`, `Requirement`, and `Metric` values while
convenience overloads remain for strings.

## Recommended Refactoring Sequence

### 1. Introduce CommandRunner

First make transaction semantics explicit without changing package structure.

1. Extract the standard command lifecycle out of `ApiTranslation.atomic`.
2. Make `ApiTranslation` delegate public commands to `CommandRunner`.
3. Keep the existing whole-game auto-exec policy intact at first; move auto-exec behind an
   `AutoExecutor` or `AutoExecAgent` only after the runner has made the current behavior explicit.
4. Add characterization tests for:
   1. rollback on failure,
   2. returned `TaskResult`,
   3. auto-exec inclusion in the result,
   4. after-command notifications.

This is the highest-leverage step because it clarifies atomicity and autoexec before any larger API
split.

### 2. Add Capability Interfaces Beside Existing Gameplay

Do not remove `Gameplay` yet. Add new interfaces that wrap/delegate to it:

1. `GameQueries`
2. `PlayerTaskActions`
3. `TaskInbox`
4. `TaskMonitor`
5. `OperationRunner`
6. `TimelineControl`
7. `DebugTaskEditor`
8. `RawStateEditor`

Wire them through Koin for each player/session. The first implementation can be thin adapters around
existing `Gameplay`, `Game.tasks`, `Game.timeline`, and `Game.reader`.

### 3. Move REPL to Injected Capabilities

The REPL is the best early client because its colored modes map directly to capabilities.

1. Replace `gameplay.godMode()` plus casts in `Access` with injected/retrieved capability bundles.
2. Keep visible behavior unchanged.
3. Move direct `game.timeline.atomic` usage in `TaskCommand` into a named command capability, such
   as `reviseAndTryTask`.
4. Keep rollback as a privileged command using `TimelineControl`.

### 4. Move Tests Toward Fixture Capabilities

Tests should still be able to cheat, but the kind of cheating should be named.

Add fixture helpers such as:

1. `giveResourcesForTest`
2. `forcePhaseForTest`
3. `placeTileForTest`
4. `dropTaskForTest`
5. `applyRawChangesForTest`

Then migrate repeated `godMode().manual` and `godMode().sneak` patterns when it improves clarity.
Do not attempt a mechanical replacement all at once.

### 5. Split TfM Facades

After generic capabilities exist, split `TfmGameplay` conceptually:

1. `TfmPlayerActions`: card play, standard projects, pass, payment.
2. `TfmGameFlow`: phases, generations, and administrative actions.
3. `TfmFixture`: test setup and rule-bypass helpers.
4. `TfmReadModel`: board/player/global-parameter projections.

These can be implemented as extension functions or small injected services. Prefer whichever keeps
dependencies honest.

### 6. De-escalate Gameplay

Only after the above migrations:

1. Deprecate `Gameplay.godMode()`.
2. Replace remaining legitimate uses with explicit debug/workflow/fixture capabilities.
3. Reconsider whether the old nested layer interfaces are still needed.

### 7. Hide Internals Later

Once clients use role/capability APIs:

1. Move public API types to an `api` package if helpful.
2. Mark low-level engine classes/methods `internal` where possible.
3. Consider moving TfM-specific API/extensions/workflow to a separate module.

This is intentionally late. Hiding internals before the replacement API is proven would make
refactoring harder, not easier.

## Small Starting Refactorings

After rereading the engine overview and current code, the safest small moves are narrower than a
full capability split. The current implementation has a few constraints worth respecting:

1. `ApiTranslation.atomic` is already the command lifecycle in miniature: it opens a timeline
   atomic block, runs the explicit command, runs auto-exec, returns activity since the checkpoint,
   and fires `onAtomicComplete` only for the outermost command.
2. `Implementations.autoExecNow` intentionally scans the whole-game task view today, even though
   assignee views are scoped. Existing workflows rely on this behavior.
3. `Task.next` is still a whole-game lock. An assignee may prepare only their own task,
   but it must still reject cutting in front of a prepared task elsewhere.
4. The REPL is currently the clearest client smell: `ScriptSession.access()` obtains `godMode()` and
   `Access` casts it back down to colored power levels, while `TaskCommand` reaches directly for
   `game.timeline.atomic` to compose a revise-and-try operation.
5. `TfmGameplay` and `TfmWorkflow` show two different kinds of Terraforming Mars convenience mixed
   into engine-facing types: player helpers such as `playProject` and payment, and Admin/workflow
   helpers such as phases and generations.

That suggests this concrete order:

1. Add command lifecycle characterization tests before moving code.

   Cover rollback on failure, `AbortOperationException`, returned `TaskResult` contents, inclusion
   of auto-executed follow-up work, and single outermost `onAtomicComplete` notification. These
   tests should describe current behavior, including whole-game auto-exec.

2. Extract `CommandRunner` as an internal service from `ApiTranslation.atomic`.

   The first extraction should be mechanical. It can still call `Timeline.atomic`, still run
   `impl.autoExecNow(autoExecMode)`, and still use the same nesting rule for `onAtomicComplete`.
   The win is that transaction semantics get a name before clients or capabilities are rearranged.

3. Route `ApiTranslation.reviseTask` and `sneak` through the named runner only if their current
   semantics are meant to be public commands.

   This is not an automatic cleanup. `reviseTask` currently returns a `TaskResult` from
   `timeline.atomic` without the normal post-command auto-exec/notification path, and `sneak` is a
   debug/raw-state operation. Decide whether each method should be a command, a debug command, or a
   lower-level primitive before changing behavior.

4. Add a named player command for the REPL's revise-and-try flow.

   `TaskCommand` currently performs:

   ```kotlin
   game.timeline.atomic {
     gameplay.reviseTask(id, rest)
     if (id in game.tasks) gameplay.tryTask(id)
   }
   ```

   A method such as `reviseAndTryTask(id, revised)` would remove public transaction composition
   from the REPL without requiring a broad API migration. If this method lives on the future
   `PlayerTaskActions` capability, it also helps prove that capability's shape.

5. Introduce capability interfaces as names first, not new architecture.

   Start with the interfaces that map cleanly to current methods:

   1. `GameQueries` for player-contextual `has`, `count`, `list`, and `resolve`.
   2. `PlayerTaskActions` for `reviseTask`, `prepareTask`, `doTask`, `tryTask`, and the new
      `reviseAndTryTask`.
   3. `OperationRunner` for `manual`, `beginManual`, `continueManual`, and `finish`.
   4. `TurnActions` for `startTurn` and `turn`.
   5. `DebugTaskEditor` for `addTasks` and `dropTask`.
   6. `RawStateEditor` for `sneak`.

   `ApiTranslation` can implement these directly at first. Koin can bind the same scoped object to
   multiple interfaces. That gives clients better types without forcing separate implementation
   classes prematurely.

6. Add compatibility accessors on `Game` only after the interfaces exist.

   Accessors such as `playerActions(player)`, `operationRunner(player)`, and `rawStateEditor(player)`
   would be a bridge for current tests and the REPL. They should return capability interfaces, not
   the old inheritance tower. Keep `gameplay(player)` until callers have migrated.

7. Move `repl.Access` to capability-shaped constructor parameters.

   The colored modes already express authority levels, so they are a good early proving ground.
   `BlueMode` should not need to cast a `Gameplay` to `TurnLayer`, and `RedMode` should receive the
   raw-state/debug capabilities it actually uses. This keeps visible REPL behavior unchanged while
   removing `godMode()` as the way to discover authority.

8. Split the obvious Terraforming Mars facades later, after generic capabilities exist.

   The first useful splits are likely:

   1. player helpers that need normal task/turn/operation capabilities,
   2. workflow/Admin helpers that need administrative operations and timeline control,
   3. fixture helpers that need raw state or task editing,
   4. read-model helpers that need only queries.

   This should not be the first refactoring because today `TfmGameplay` also acts as a convenient
   compatibility wrapper around `TurnLayer`.

## Open Design Questions

1. Should `AutoExecMode.FIRST` remain the default for all developer clients?

   It is convenient, but assignee-local autoexec may require explicit Admin workflow or task-drain
   behavior.

2. What should count as a public command?

   `prepareTask`, `addTasks`, `dropTask`, and `sneak` currently have mixed transaction semantics.
   We need to decide which are normal commands, which are debug commands, and which should become
   lower-level internals.

3. How structured should UI task choices be?

   This can wait, but a web UI should eventually receive structured choices instead of rendering raw
   Pets text as its main interaction model.

## Bottom Line

The next clean model is not "one better `Gameplay`". It is:

1. A command runner that owns transaction semantics.
2. Auto-exec as an actor-local, injected post-command agent.
3. Small injectable capability interfaces.
4. Koin wiring that gives each client only the capabilities it should have.
5. Terraforming Mars helpers/workflow as a facade layer outside the generic engine.
6. Internal/public package separation only after the capability model is already working.

This lets the code move toward principled boundaries without first tearing apart the engine
internals. The first refactorings can be additive adapters and wiring changes, with behavior kept
stable while clients stop depending on `godMode()` and casts.
