# Game World model

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** creating or changing the `:state` module, deciding ownership of components,
> pending-task data, events, recordings, exported games, or recording navigation.
>
> **Skip when:** changing how an instruction is resolved or executed, how effects fire, or how an
> Agent chooses work. Those belong in [ENGINE.md](ENGINE.md), [SEQUENCING.md](SEQUENCING.md), and
> [AUTOEXEC.md](AUTOEXEC.md).
>
> **Status:** current model for state-owned world data, rich queries, recording playback, and replay
> exports.

## Current source map

- [`GameWorld.kt`](../../src/common/dev/martianzoo/state/GameWorld.kt),
  [`ComponentChange.kt`](../../src/common/dev/martianzoo/state/ComponentChange.kt), and
  [`ComponentGraph.kt`](../../src/common/dev/martianzoo/state/ComponentGraph.kt) — the passive,
  fully concrete component-state boundary.
- [`GameReaderImpl.kt`](../../src/common/dev/martianzoo/state/GameReaderImpl.kt) and
  [`CustomMetricRuntime.kt`](../../src/common/dev/martianzoo/state/CustomMetricRuntime.kt) — the
  state-owned rich Pets query adapter and Catalog-provided metric evaluation.
- [`World.kt`](../../src/common/dev/martianzoo/engine/World.kt) and
  [`WholeWorld.kt`](../../src/common/dev/martianzoo/engine/WholeWorld.kt) — the live engine facade
  over one `GameWorld`.
- [`TaskQueue.kt`](../../src/common/dev/martianzoo/state/TaskQueue.kt),
  [`TaskStore.kt`](../../src/common/dev/martianzoo/state/TaskStore.kt),
  [`TaskQueues.kt`](../../src/common/dev/martianzoo/engine/TaskQueues.kt),
  [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt), and
  [`GameEvent.kt`](../../src/common/dev/martianzoo/state/GameEvent.kt) — search for
  `internal class TaskStore`, `internal class TaskQueues`, `public data class Task`, and
  `public sealed class GameEvent` for state-owned task storage, engine-owned task construction, and
  passive recorded values.
- [`GameRecording.kt`](../../src/common/dev/martianzoo/state/GameRecording.kt),
  [`GameRecordingJson.kt`](../../src/common/dev/martianzoo/state/GameRecordingJson.kt), and
  [`recording.kt`](../../src/common/dev/martianzoo/engine/recording.kt) — immutable recording data,
  passive independent playback, opaque JSON interchange, and capture from a live engine World.
- [`ReplayExportExtension.kt`](../../test/jvm/dev/martianzoo/tfm/tests/replays/ReplayExportExtension.kt)
  and [`Main.kt`](../../src/js/dev/martianzoo/tfm/web/gameviewer/Main.kt) — automatic successful
  replay-test export and engine-free browser loading.

## One game's pocket universe

Each time people sit down to play, they create one Game World: a pocket universe with its own
immutable premise, present components, pending choices, history, and rich read model. It exists for
the whole lifetime of that game rather than denoting one snapshot.

The `:state` module owns the replayable core of that data model. A current `GameWorld` contains:

- immutable premise-derived context, including its Catalog, Class Table, and Actors;
- the `ComponentGraph`, which materializes the components present at the current position;
- one unordered queue of exact pending `Task` values;
- the complete `GameEvent` log; and
- a `GameReader` whose ordinary and custom metrics are passive queries over those components.

The component graph is the present, the task queue is the unresolved future, and the event
log is the past. They are three views of one game lifetime and must advance or reverse together.
The current component and task projections are materialized from an event-log prefix; they may
never disagree with that prefix.

Task instructions, assignment, selection state, continuations, causes, and ids are facts about the
game. Storing those facts does not give Game World any task behavior. In particular, `:state`
does not select, narrow, normalize, resolve, split, execute, or automatically process tasks.

## Dependency direction

Using “depends on” explicitly:

- `:state` depends on `:pets` for the language and static type model;
- `:engine` depends on `:state` and interprets the pending instructions it contains;
- `:agent` depends on `:engine` and owns Actor-scoped interaction and policies;
- `:tfm-engine` depends on `:engine` for Terraforming Mars behavior; and
- the game viewer depends on `:state` and `:tfm-canon`, plus `:tfm-fake` for noncanonical
  recordings, but not on `:engine` or `:tfm-engine`.

JVM replay tests compose all needed layers while playing. A browser reads only their exported files,
does not calculate consequences, and has no `:engine`, `:agent`, or `:tfm-engine` dependency.

## Passive event application

Game World accepts only changes whose meaning has already been decided:

- a fully concrete gain, removal, or transmutation with resolved Types and an exact count; or
- an exact task addition, removal, or edit.

Applying one updates the event log and its materialized component/task projections as one
operation. Game World enforces its own structural invariants, such as concrete active component
Types, dependency integrity, exact task-event matching, and unique task ids. A transmutation may
name the same component on both sides; its net component-state change is zero.

Constructing a `GameWorld` with a complete event list applies that list in ordinal order and
reconstructs the corresponding component graph, pending tasks, and event history. It requires the
compatible Class Table because event files reference resolved Types through their full Pets
expressions rather than embedding another Catalog.

Application is mechanically inert. It never discovers or fires effects, creates follow-up work,
chooses dependent removals, interprets an Instruction, or invokes Agent policy. During live play,
the engine decides a change, asks Game World to apply it, and then explicitly calculates any
consequences. During recording playback, the same recorded change is applied with no engine
present, so no consequence can occur twice.

Dependent removal illustrates the split. Game World may reject removal while dependents remain.
The engine decides which dependent removal to perform and records that concrete change before
retrying the original removal. Playback merely reapplies those recorded events.

Runtime `Task`, `GameEvent`, and `TaskResult` values, exact task storage, event history, passive
application, rich queries, immutable recordings, and playback navigation live in `:state`. Task
construction, normalization, custom-instruction translation, and execution live in `:engine`.

## Live play and recording navigation

The engine operates against a live Game World positioned at its latest event. The engine owns live
transaction coordination, failure rollback, the commit floor, effects, and the decision that an
outer mutation has reached a coherent presentable position. It records those ordinals while live;
capture copies them into the immutable state-owned recording.

A recording is immutable exported history. Opening it creates an independent Game World with its
own component graph, task queue, reader, and cursor. Seeking passively applies or reverses exact
events and cannot alter the engine-owned world that produced the recording, the immutable
recording, or another view opened from the same recording.

Public navigation accepts only an approved recording position or its list index. An event ordinal
may be displayed in the log and used by causal metadata, but it is not a seek target. Intermediate
events inside automatic `::` consequences, cascading dependent removal, idle cleanup, or another
outer operation are replay mechanics, never worlds exposed to the viewer.

The engine's `Timeline` retains live transaction atomicity and the commit floor. Its
`RecordingPositions` records coherent completed-operation ordinals; capture copies those values
into a state-owned recording, where they become the only public seek targets.

## Serialized events and exported recordings

`EventLogJson` is the state-level opaque JSON encoding for an exact event list. Changes use full
round-tripping Type expressions; tasks retain their complete instruction, continuation, assignment,
selection, and cause, while events retain their notes. Decoding takes the compatible Class Table
and returns events that can construct a fresh `GameWorld`.

`GameRecordingJson` wraps that data in one JSON value containing only what the viewer consumes:
the positive and negative Class selections needed to recreate the same playable Class universe,
ordered Player names, approved positions, and exact events. Premise-local setup effects are
deliberately absent: playback applies their already-recorded consequences and never executes setup.
The browser supplies the matching Canon rather than loading a serialized Catalog.

Every successfully completed `AbstractFullGameTest` writes `<test-class>.json` under the
`:tfm-tests` build directory. Replay tests are JVM-only. The game viewer's resource task packages
whatever `.json` files currently exist there and generates `games/index.txt`; it does not cause
tests to run. After a clean, a viewer-only build therefore has an empty menu. When a full build also
runs JVM tests, task ordering makes their current outputs available before viewer resources are
processed. The dropdown uses each test filename without its extension.

## Verification responsibilities

Pure `:state` tests should verify:

- exact component and task events advance and reverse all projections together;
- event replay never fires effects or invents task work;
- task values round-trip without normalization or loss;
- only approved positions are seekable;
- seeking never exposes intermediate automatic-effect or dependent-removal events;
- separate views of one recording navigate independently;
- recording and event encoding round-trip exact state data.

Cross-module engine tests should continue to confirm that live failure rollback restores components,
tasks, and history together, and that the engine marks positions only after a coherent outer
mutation completes.
