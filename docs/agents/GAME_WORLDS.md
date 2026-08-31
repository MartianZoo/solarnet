# Game Worlds, recordings, and speculative overlays

> **Read when:** designing event identity, checkpoints, scrolling, rollback, saved recordings,
> state rematerialization, forks, overlays, or the division between state and engine modules.
>
> **Skip when:** changing component, task, effect, or command semantics without changing their
> history or restoration model; use [ENGINE.md](ENGINE.md) instead.
>
> **Status:** working rules for recordings, event identity, and checkpoints; proposal for the
> state-module extraction and later engine overlays. The current implementation differs where noted.

## Core model

Keep three concepts distinct:

| Concept | Contract |
| --- | --- |
| Live Game World | Mutable engine state, always positioned at the tip of its own history. |
| Recording | Independent, persistable state with immutable history and a freely movable read cursor. |
| Overlay Game World | A later engine-only speculative branch represented by a base state plus an event suffix. |

A Recording has much of the read surface of a live Game World: a readable component graph, tasks,
event history, and `GameReader`. It has no command-execution surface. Copying a Recording from a
live Game World does not freeze, constrain, or share mutable state with that World. The source may
continue, roll back, or disappear while the Recording remains stable.

Scrolling and rollback are different operations. A Recording may scroll to every event cursor,
including a cursor inside the immediate consequences of one request. A live Game World may roll
back only to a Checkpoint. Rollback physically discards the suffix and makes the selected
Checkpoint the new tip; forward navigation through the discarded suffix is then impossible in
that World. An independent Recording may still retain it.

## Checkpoints

A Checkpoint is a coherent state at which the engine has finished one incoming request and is ready
to accept the next. Pending Player or Engine tasks may remain; a Checkpoint does not mean idle.

An incoming request may come from a human-facing client, script, workflow driver, Player
autoexecution policy, Engine Agent policy, test, or game-playing agent. Once the request begins:

1. no other client or policy may issue another request;
2. the engine records the submitted input and carries out its true-immediate synchronous rule
   consequences;
3. true-immediate `::` effects, normalization, immediate task changes, and other synchronous
   engine-owned settlement finish;
4. the complete segment either commits or disappears on failure; and
5. the engine marks the resulting state as the next Checkpoint before any autoexecution
   policy gets an opportunity to act.

An autoexecution command is therefore not hidden continuation of the preceding request. A policy
observes a Checkpoint and may submit one ordinary next request, which produces its own segment and
next Checkpoint. An authored `::` effect is not autoexecution and remains inside the request
that triggered it.

A Task assigned to Engine's queue is not true-immediate merely because Engine can perform it
automatically. It may remain pending at the Checkpoint. If Engine's Agent policy chooses to perform
it, that policy submits the next incoming request, whose synchronous consequences lead to another
Checkpoint.

Conceptually:

```text
checkpoint 3
  request from one client or policy
  input event and true-immediate consequences
checkpoint 4
  next client or policy may now act
```

A failed or deliberately aborted request leaves no events and creates no Checkpoint. A
successful request creates the next point even when its only retained history is diagnostic input.
The commit floor remains a separate restriction: a structurally safe point can still be too old for
one live session to roll back to.

## Checkpoint, savepoint, and event identity

`Checkpoint` is the domain term for an engine-minted position safe for later mutable resumption.
`Savepoint` is better reserved for a temporary transactional mark used for failure rollback inside
one request. An arbitrary `EventCursor` is neither: it is always safe to inspect but may be unsafe
for mutation.

The current `Timeline.Checkpoint` is a bare event-count position and does not satisfy the target
Checkpoint contract. It should eventually become an internal transaction mark or be replaced by
the history-specific Checkpoint type described here.

Only Checkpoints receive one simple sequential ordinal. Individual events receive a
composite identity consisting of:

- the Checkpoint preceding their request segment; and
- their zero-based index within that segment.

For example, the display form `3.0014` can mean the fifteenth event after Checkpoint 3, in the
segment that eventually produces Checkpoint 4. The composite applies to every logged entry,
including input and task events, not only component changes. Causes and task identities may refer to
this typed event identity.

The exact punctuation and padding are unsettled presentation details. Serialization and APIs should
store a pair, not parse a decimal-shaped number. A cursor is a position between events and must also
distinguish “before” from “after” an identified event.

Checkpoint values must carry history identity in addition to their displayed ordinal. A live
World accepts a point captured by a Recording only when the live history still contains that exact
prefix. A point from another or already-diverged history is invalid even if its number matches.

## Recordings

A Recording owns:

- the immutable premise and static type information required for reads;
- a complete event sequence or an explicit initial snapshot followed by a complete sequence;
- Checkpoint markers;
- an independent current cursor; and
- replaceable materialized read state for that cursor.

Restoring a Recording means loading this independent object, not creating a live engine. Seeking
mechanically applies or reverses recorded component and task events. It never resolves
instructions, fires effects, invokes autoexecution, or asks game rules what should have happened;
the recorded consequences are already authoritative.

Any event cursor is valid for inspection. Only a cursor marked as a Checkpoint may be used to
construct a mutable Game World or as the target of destructive rollback. This lets a viewer inspect
transient states without claiming that the engine could safely resume from them.

The current `GameRecording` is transitional. It seeks by reversing and replaying events on the
original live `World`, exposes that mutable `World`, limits navigation to recorded positions, and
seals the source timeline. None of those properties belong to the target Recording contract.

## Two saved forms

The same game may be exported with two restoration contracts:

1. **Request replay.** Store the premise or source configuration plus the ordered incoming requests,
   their issuing agents, and required format/algorithm versions. Loading creates a real engine World
   and submits those requests again. This is concise and readable, but reconstruction is execution
   under the selected engine version. [ROUTINES.md](ROUTINES.md) owns the readable Routine form.
2. **State recording.** Store the request replay plus the complete resulting event log and
   Checkpoints. Loading rematerializes state mechanically and requires no engine. It preserves the
   exact recorded outcome and can optionally validate a fresh engine replay against that outcome.

Both forms should use one versioned container with the complete event stream optional rather than
unrelated formats. A state recording must identify the exact immutable declarations needed to
interpret its types. Whether those declarations are embedded or selected by a versioned artifact is
a transport decision, not a change to the state model.

## State and engine ownership

The intended module division is:

| `dev.martianzoo.state` owns | `dev.martianzoo.engine` owns |
| --- | --- |
| Event and Checkpoint identities | Accepting and validating incoming requests |
| Immutable event sequences and Recording I/O | Instruction resolution and execution |
| Mechanical event application and reversal | Effects and task creation |
| Readable components, tasks, and `GameReader` state | Atomic command execution and live rollback |
| Recording cursors and materialization caches | Autoexecution-policy integration and overlays |

The engine depends on state and produces state events. A pure replay viewer depends on state plus
the relevant static game declarations and presentation code, not on the engine. Read-only custom
metrics required by `GameReader` must consequently be available to the state-side reader without
bringing instruction execution with them.

The state module does not need `OverlayWorld`. It can copy, save, restore, scroll, and materialize a
Recording, but it has no engine with which to explore an alternative continuation.

## Materialization

Event history is authoritative; materialization is only a performance cache. A Recording or later
Overlay may retain materialized state through an internal cursor and fold the remaining suffix when
answering a query. It may advance, discard, or rebuild that cache without changing any answer.

Client control over how far state is materialized, if useful for profiling or memory management, is
an internal tuning hook rather than public Game World semantics. No serialized result or legal
command may depend on it.

An event suffix being the complete semantic representation does not require every query to perform
a literal full scan. Net-count maps, task indexes, dependency indexes, and snapshots are all valid
replaceable accelerators. Deleting them and folding the same history must reproduce the same reads.

## Later Overlay Game Worlds

Recordings come first. An Overlay Game World later supports explicit hypothetical engine execution:

- its base is an exact live-World revision or immutable Recording Checkpoint;
- its only authoritative difference from that base is its event suffix;
- component, task, and live-effect views derive from the base plus that suffix;
- materialized deltas and indexes are caches;
- every mutation and query targets the Overlay explicitly; and
- discarding the Overlay has no observable effect on its base.

An event suffix can remain the whole overlay representation so long as every fact that can affect
future gameplay is immutable or derivable from recorded state. Random outcomes, hidden information,
workflow state, and future caches must obey that rule or become recorded state. Stable automatic
effect ordering must derive from immutable work data, never from mutation history outside the log.

`WholeWorld` and `OverlayWorld` should be separate engine implementations over a common state read
model. A stable `World` object must not silently swap its interior between them: callers can retain
its components, tasks, reader, events, and Actor-scoped command objects independently, and two
speculative branches must be able to coexist.

The first overlay use is disposable analysis. A policy explores alternatives in overlays, discards
them, verifies that the live base revision is unchanged, and then submits the selected ordinary
command to the live World. Reusing a successful speculative suffix as a live commit is a later
optimization, not part of the initial semantic contract.
