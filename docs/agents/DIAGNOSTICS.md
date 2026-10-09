# Runtime diagnostics

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** investigating engine sequencing, task assignment, permissions, autoexecution,
> replay divergence, or another failure whose runtime cause is not apparent from the final World.
>
> **Skip when:** the failure is already explained by a focused assertion or source-level
> debugging.
>
> **Status:** current event notes plus proposed debug logging and investigation procedure. This
> document does not authorize adding every potentially useful detail to the event model.

## Goal

Make one small reproduction explain what the engine actually did. An investigator should normally
read captured runtime evidence before reconstructing execution from source searches.

## Current exception contract

[`Exceptions.DomainException`](../../src/common/dev/martianzoo/pets/api/Exceptions.kt) provides
unrendered `detail`, optional `sourceLocation`, and a message with a source excerpt when available.
Messages are composed where the relevant context is known. The broad categories distinguish
malformed Pets (`PetSyntaxException`), expressions that cannot be interpreted (`ExpressionException`),
faulty declarations (`InvalidPetDefinitionException`), invalid setup (`InvalidGameConfigException`),
expected gameplay rejection (`GameplayException`), unfinished choices (`NotFullySpecifiedException`),
and faulty Kotlin implementations (`CustomCodeException`). Requesting unavailable custom instruction
or metric behavior is an expression error; a crash or `TODO()` inside a supplied implementation is
a custom-code failure. Invalid Pets returned by a `CustomInstruction` implementation also identifies
the implementation, preserving the generated syntax's source span when available. Virtual
`CustomMetric` types cannot be gained or removed as components; `CustomInstruction` follows ordinary
Signal rules.

Invalid definitions may be discovered after catalog construction: invariant compilation, effect
elaboration, and property expansion still need game context. During class-effect elaboration, the
wrapper names the declaring class, retains the original cause and source span, and wraps `detail`
rather than rendering the excerpt twice. Deferred evaluations name the concrete component when an
effect fires or the selected type when an `EACH` body is bound.
A submitted property evaluation instead reports an expression error. Specialization
failures identify the concrete component and authored trigger; invalid specialized instruction
branches still become `Die` under L9-14. Gameplay subtype payloads used by recovery logic remain
intact. Missing source means no reliable authored occurrence survived; generated tasks must not
invent a declaration location.

[`PostCatalogDiagnosticsTest`](../../test/common/dev/martianzoo/engine/PostCatalogDiagnosticsTest.kt)
covers premise selection, setup, queries, effects, custom implementations, and task use after
a valid catalog exists. Named tests assert the rendered message, including its caret and source
excerpt, rather than separately asserting source offsets. Comments beside imperfect diagnostics
record the preferred span or missing related declaration. The renderer currently draws one caret,
so these expectations verify its starting position, not a highlighted range's width.

Author-facing messages use lower-case sentence fragments without a trailing period. Quote Pets
names, expressions, and keywords in backticks; quote list items individually instead of dumping
Kotlin collections or enum names. State the problem first, followed by relevant values and a
concrete correction when one is known. Parser messages use `expected ...; found ...`. Wrappers keep
the underlying explanation after a contextual prefix; messages from custom Kotlin causes retain
their original wording.

Parsing recognizes generic expression structure; resolution checks the catalog's names and bounds,
as well as the type-system restrictions on class-literal operands (T4-6) and excluded refinements
(T8-5). The resolution tests parse their inputs before entering the failure assertion so this
boundary is explicit.

Diagnostics have layer-specific homes:

- Game World `GameEvent`s remain the durable account of changes and task lifecycle. We may add a
  few optional diagnostic properties to those events, but no new event kinds simply to describe
  debugging activity. [GAMEWORLD.md](GAMEWORLD.md) owns their data and export role.
- An opt-in engine debug log records resolution, execution, effects, and rollback attempts that
  produced no event.
- Agent scoping, policies, and the shared autoexecution loop keep their own opt-in logs for caller
  provenance, engine completion, policy eligibility, and declined decisions. Those records may
  correlate with event history but do not move policy reasoning into engine diagnostics.

Keeping these separate is important. The event log says what happened; the debug log can say what
the engine considered and why it did nothing.

## Small event-schema enrichment

The current useful anchors are event ordinal, `ChangeEvent.cause`, task id, and task contents.
Prefer using and rendering those consistently before adding fields.

Every `GameEvent` has one deliberately broad annotation property:

```kotlin
public var notes: String? = null
```

This is deliberately an arbitrary string rather than a new hierarchy. It can hold sourced human
commentary or concise diagnostic information that belongs with an event but is not part of game
meaning. Card-tracked replay tests use it to correlate known physical card names with otherwise
anonymous project-card changes.

`notes`:

- is optional, mutable after the event, and has no effect on gameplay;
- is excluded from event equality and gameplay-state equivalence;
- must not become game input or a substitute for `ChangeEvent.cause`;
- appears only when a client explicitly renders it, so normal history remains readable; and
- carries no stable machine-readable format promise.

Do not put information in `notes` for convenience alone. In particular, "why did autoexec skip
this task?" cannot belong to an event when the skipped decision produced no event.

Avoid adding several typed fields speculatively. A possible later exception is making operation
correlation available on more event kinds, if real trace analysis shows it cannot be recovered
reliably from causal links. That need should be demonstrated first.

## Opt-in debug logging

Together, layer-owned debug logging should cover decision points that do not necessarily change the
World:

- tasks and policies considered by an Agent, including the reason each declined, in the Agent log;
- Actor scoping and engine mutation forwarding in the Agent log;
- engine completion, task-analysis invalidation, and fixed-point detection in the shared-loop log;
- core task-pool assignment and legality checks in the engine log;
- narrowing, resolution, and execution attempts;
- assignment, Actor, and queue choices when they are computed;
- automatic-effect ordering decisions; and
- rollback or retry paths.

The output should be written through a configurable sink, not unconditional `println` calls. Each
record should include enough correlation context to join it back to game history when applicable:
the next event ordinal or most recent event ordinal, task id, and Actor. Not every
record needs every value.

The debug log is allowed to be verbose and implementation-shaped. It must not affect scheduling,
ordering, equality, or World state. Logging should be disabled by default and
cheap when disabled.

## Investigation procedure for agents

For sequencing, delegation, autoexec, or runtime discrepancies:

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
paths. Its interface should select a test or scenario plus debug categories; it should not synchronize
Git history, change the reproduction, or silently run a broad test suite.
