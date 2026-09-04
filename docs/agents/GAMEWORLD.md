# Game World model

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** creating or changing the `:gameworld` module, deciding ownership of components,
> pending-task data, events, recordings, exported games, or recording navigation.
>
> **Skip when:** changing how an instruction is resolved or executed, how effects fire, or how an
> Agent chooses work. Those belong in [ENGINE.md](ENGINE.md), [SEQUENCING.md](SEQUENCING.md), and
> [AUTOEXEC.md](AUTOEXEC.md).
>
> **Status:** selected design with substantial current implementation divergence.

## Current source map

- [`World.kt`](../../src/common/dev/martianzoo/engine/World.kt) and
  [`WholeWorld.kt`](../../src/common/dev/martianzoo/engine/WholeWorld.kt) — search for
  `public interface World` and `internal class WholeWorld` for the currently combined object.
- [`ComponentGraph.kt`](../../src/common/dev/martianzoo/engine/ComponentGraph.kt) and
  [`GameReaderImpl.kt`](../../src/common/dev/martianzoo/engine/GameReaderImpl.kt) — search for
  `applyChange` and `internal class GameReaderImpl` for present-state storage and queries.
- [`TaskQueues.kt`](../../src/common/dev/martianzoo/engine/TaskQueues.kt),
  [`Task.kt`](../../src/common/dev/martianzoo/pets/data/Task.kt), and
  [`GameEvent.kt`](../../src/common/dev/martianzoo/pets/data/GameEvent.kt) — search for
  `internal class TaskQueues`, `public data class Task`, and `public sealed class GameEvent` for
  task storage, task behavior currently attached to its data type, and recorded deltas.
- [`EventLog.kt`](../../src/common/dev/martianzoo/engine/EventLog.kt),
  [`TimelineImpl.kt`](../../src/common/dev/martianzoo/engine/TimelineImpl.kt),
  [`GameRecording.kt`](../../src/common/dev/martianzoo/engine/GameRecording.kt), and
  [`RecordingPositions.kt`](../../src/common/dev/martianzoo/engine/RecordingPositions.kt) — search
  for `record`, `seek`, `public class GameRecording`, and `internal fun record` for the current
  history, transaction, playback, and completed-position coupling.
- [`RecordedGame.kt`](../../src/common/dev/martianzoo/tfm/web/gameviewer/RecordedGame.kt) and
  [`Main.kt`](../../src/js/dev/martianzoo/tfm/web/gameviewer/Main.kt) — search for `record()` and
  `loadSelectedGame` for the engine-backed browser replay that exported data will replace.

## One game's pocket universe

Each time people sit down to play, they create one Game World: a pocket universe with its own
immutable game context, present components, pending choices, and history. It exists for the whole
lifetime of that game rather than denoting only one snapshot.

The `:gameworld` module owns that data model. A Game World contains:

- immutable premise-derived context, including its Class Table, Actors, and Vocabulary;
- the `ComponentGraph`, which materializes the components present at the current position;
- one unordered pool of exact pending `Task` values;
- the complete `GameEvent` log; and
- the approved recording positions at which that world may be presented.

The component graph is the present, the pending-task pool is the unresolved future, and the event
log is the past. They are three views of one game lifetime and must advance or reverse together.
The current component and task projections are materialized from an event-log prefix; they may
never disagree with that prefix.

Task instructions, assignment, selection state, continuations, causes, and ids are facts about the
game. Storing those facts does not give Game World any task behavior. In particular, `:gameworld`
does not select, narrow, normalize, resolve, split, execute, or automatically process tasks.

## Dependency direction

Using “depends on” explicitly:

- `:gameworld` depends on `:pets` for the language, static type model, Catalog, and premise;
- `:engine` depends on `:gameworld` and interprets the pending instructions it contains;
- `:agent` depends on `:engine` and owns Actor-scoped interaction and policies;
- `:tfm-engine` depends on `:engine` for Terraforming Mars behavior; and
- the game viewer depends on `:gameworld` and `:tfm-canon`, but not on `:engine` or `:tfm-engine`.

The designated JVM replay exporter composes all needed layers. A browser that reads its output
does not calculate consequences and must not acquire an engine dependency indirectly.

## Passive event application

Game World accepts only changes whose meaning has already been decided:

- a fully concrete gain, removal, or transmutation with resolved Types and an exact count; or
- an exact task addition, removal, or edit.

Applying one updates the event log and its materialized component/task projections as one
operation. Game World enforces its own structural invariants, such as concrete active component
Types, dependency integrity, exact task-event matching, and unique task ids.

Application is mechanically inert. It never discovers or fires effects, creates follow-up work,
chooses dependent removals, interprets an Instruction, or invokes Agent policy. During live play,
the engine decides a change, asks Game World to apply it, and then explicitly calculates any
consequences. During recording playback, the same recorded change is applied with no engine
present, so no consequence can occur twice.

Dependent removal illustrates the split. Game World may reject removal while dependents remain.
The engine decides which dependent removal to perform and records that concrete change before
retrying the original removal. Playback merely reapplies those recorded events.

Runtime `Task`, `GameEvent`, and recording-position values belong to `:gameworld` and should move
there; current source still places `Task` and `GameEvent` in `:pets`. Task construction and
normalization belong in `:engine`. Event and task rendering currently attached to `Vocabulary` must
move or become higher-layer extensions so `:pets` does not depend upward on runtime data.

## Live play and recording navigation

The engine operates against a live Game World positioned at its latest event. The engine owns live
transaction coordination, failure rollback, the commit floor, effects, and the decision that an
outer mutation has reached a coherent presentable position. Game World stores the resulting
approved position but does not decide when the engine is finished.

A recording is immutable exported history. Opening it creates an independent Game World view with
its own component graph, task pool, and cursor. Seeking changes only that derived view. It cannot
alter the engine-owned world that produced the recording, the immutable recording, or another
view opened from the same recording.

Public navigation accepts only an approved recording position or its list index. An event ordinal
may be displayed in the log and used by causal metadata, but it is not a seek target. Intermediate
events inside automatic `::` consequences, cascading dependent removal, idle cleanup, or another
outer operation are replay mechanics, never worlds exposed to the viewer.

A seek may internally apply or reverse many events. Observers receive the completed target
projection, not transient projections encountered while moving the cursor.

Current `Timeline` combines live transaction control with recording playback. Extraction should
leave transaction atomicity and the commit floor in `:engine`, while moving independent recording
navigation into `:gameworld`.

## Exported recordings

The initial export format is deterministic, versioned, checked-in data. It is intentionally
Canon-dependent rather than self-contained. An export contains enough information to reconstruct
the exact premise with the supplied Catalog, followed by the complete event stream and approved
positions. It includes full task values, not display-only copies.

The format records a schema version and a Canon fingerprint. A mismatch requires regeneration;
the first format does not promise migration across changed Canon definitions. This avoids embedding
a second Class Table or duplicating the Catalog in every recording.

Designated JVM full-game replay tests are the sole authored source of viewer games. A dedicated
export task runs those replays and writes deterministic files. A verification task regenerates them
outside the source tree and byte-compares them with the checked-in copies. Ordinary viewer builds
consume the checked-in files without running the engine.

Once equivalent exports exist, delete the duplicated replay programs under the game viewer. The
viewer may derive Terraforming Mars presentation facts from `GameReader` and `tfm-canon`, but pure
queries such as production counts and visible-log filtering must not pull in `TfmGameplay`.

## Verification responsibilities

Pure `:gameworld` tests should prove:

- exact component and task events advance and reverse all projections together;
- event replay never fires effects or invents task work;
- task values round-trip without normalization or loss;
- only approved positions are seekable;
- seeking never exposes intermediate automatic-effect or dependent-removal events;
- separate views of one recording navigate independently;
- observers see only the completed seek target; and
- export encoding is deterministic and rejects incompatible schema or Canon fingerprints.

Cross-module engine tests should continue to prove that live failure rollback restores components,
tasks, and history together, and that the engine marks positions only after a coherent outer
mutation completes.

## Extraction order

1. Move the passive component store and readable Game World projection behind a `:gameworld` API.
2. Split exact task storage and event replay from engine-owned task construction and behavior.
3. Separate recording navigation from live engine transaction control.
4. Add the recording schema, deterministic JVM exporter, and checked-file verification.
5. Export designated full-game replay tests and switch the viewer to those files.
6. Remove viewer replay sources and its engine dependencies.
7. Move remaining runtime data types and rendering helpers to their final owning modules once the
   dependency seam is proven by the working composition.

Do not create an empty module, a parallel display model, or a self-contained serialized Catalog as
preparatory architecture. Each step must leave one working composition and reduce an actual
dependency or ownership mismatch.
