# Runtime diagnostics

> **Read when:** investigating engine sequencing, task assignment, permissions, autoexecution,
> replay divergence, or another failure whose runtime cause is not apparent from the final World.
>
> **Skip when:** the failure is already explained by a focused assertion or ordinary source-level
> debugging.
>
> **Status:** proposal and investigation procedure. This document does not authorize adding every
> potentially useful detail to the event model.

## Goal

Make one small reproduction explain what the engine actually did. An investigator should normally
read captured runtime evidence before reconstructing execution from source searches.

Diagnostics have layer-specific homes:

- Existing `GameEvent`s remain the durable account of changes, task lifecycle, and successful
  gameplay input. We may add a few optional diagnostic properties to those events, but no new event
  kinds merely to describe debugging activity.
- An opt-in engine debug log records resolution, execution, effects, and rollback attempts that
  produced no event.
- Actor access, Agent Drivers, and generic pulse dispatch keep their own opt-in logs for Actor
  binding, caller provenance, wake-ups, policy eligibility, and declined decisions. Those layers may
  correlate with engine revisions and events but do not move policy reasoning into engine
  diagnostics.

Keeping these separate is important. The event log says what happened; the debug log can say what
the engine considered and why it did nothing.

## Small event-schema enrichment

The current useful anchors are event ordinal, `ChangeEvent.cause`, task id and task contents,
`GameplayInputEvent.operationStartOrdinal`, and optional `GameEvent.agent` provenance. Prefer using
and rendering those consistently before adding fields.

The first additional property to consider is:

```kotlin
public var diagnostics: String? = null
```

This is deliberately an arbitrary string rather than a new hierarchy. It can hold a concise dump
of information that belongs with an event but is not part of game meaning, such as a resolution
explanation or relevant pre-transition context.

Like `agent`, `diagnostics` should:

- be optional and have no effect on gameplay;
- be excluded from event equality and gameplay-state equivalence;
- not become a replay input or a substitute for `ChangeEvent.cause`;
- appear only in explicitly diagnostic rendering, so normal history remains readable; and
- carry no stable machine-readable format promise.

Do not put information in `diagnostics` merely because it is convenient. In particular, "why did
autoexec skip this task?" cannot belong to an event when the skipped decision produced no event.

Avoid adding several typed fields speculatively. A possible later exception is making operation
correlation available on more event kinds, if real trace analysis shows it cannot be recovered
reliably from the existing operation-start ordinal and causal links. That need should be
demonstrated first.

## Opt-in debug logging

Together, layer-owned debug logging should cover decision points that do not necessarily change the
World:

- tasks and policies considered by an Agent Driver, including the reason each declined, in the
  Driver log;
- `ActorAccess` binding and mutation forwarding in the access log;
- pulse delivery, revision invalidation, and fixed-point detection in the dispatcher log;
- core task-pool assignment and legality checks in the engine log;
- narrowing, resolution, and execution attempts;
- assignment, Actor, and queue choices when they are computed;
- automatic-effect ordering decisions; and
- rollback or retry paths.

The output should be written through a configurable sink, not unconditional `println` calls. Each
record should include enough correlation context to join it back to game history when applicable:
the next event ordinal or most recent event ordinal, World revision, operation-start ordinal, task
id, Actor, and Agent. Not every record needs every value.

The debug log is allowed to be verbose and implementation-shaped. It must not affect scheduling,
ordering, equality, replay, or exported World state. Logging should be disabled by default and
cheap when disabled.

## Investigation procedure for agents

For sequencing, delegation, autoexec, or replay discrepancies:

1. Create the smallest reliable reproduction.
2. Enable diagnostic event rendering and the relevant debug-log categories.
3. Write the rendered event history and debug log to adjacent files under
   `_local/traces/<short-name>/`. Preserve the exact command and input used to generate them.
4. Analyze event ordinals, task ids, operation correlation, and causal links from those files before
   inferring execution from the implementation.
5. Use targeted source inspection to explain the observed trace, not to invent a hypothetical one.
6. In the result, cite the decisive trace records and clearly separate observed behavior from the
   proposed fix.

The eventual helper command should perform steps 2 and 3 in one invocation and print the artifact
paths. Its interface should select a test or replay plus debug categories; it should not synchronize
Git history, change the reproduction, or silently run a broad test suite.
