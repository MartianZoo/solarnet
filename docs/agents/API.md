# Engine workhorse and client API direction

> **Read when:** flattening `Gameplay`, removing a power interface or `godMode()`, changing command
> transaction scopes, or designing a real client/observation API.
>
> **Skip when:** adding a gameplay operation without changing facade ownership; use the
> Gameplay section of [ENGINE.md](ENGINE.md#current-gameplay-surface).
>
> **Status:** proposal. Some read/write data-structure pairs have been collapsed; the `Gameplay`
> power hierarchy and `godMode()` remain committed behavior.

## Source map

- [`Gameplay.kt`](../../src/common/dev/martianzoo/engine/Gameplay.kt) — search for
  `public interface Gameplay` to see the current power hierarchy.
- [`ApiTranslation.kt`](../../src/common/dev/martianzoo/engine/ApiTranslation.kt) —
  search for `internal class ApiTranslation` before moving string/value adaptation.
- [`Implementations.kt`](../../src/common/dev/martianzoo/engine/Implementations.kt)
  — search for `internal class Implementations` before changing command lifecycle ownership.
- [`ScriptSession.kt`](../../src/common/dev/martianzoo/script/ScriptSession.kt) —
  read only when the change also touches the temporary REPL policy layer.

## Decision

Separate two responsibilities:

1. **Engine integrity:** keep component, effect, task, event, and timeline state coherent,
   transactional, reversible, and table-local, including during a rules-bypassing operation.
2. **Caller authority:** decide which operations a player, workflow, script color, test, or
   administrator may request.

The existing `engine` module should become one flat, trusted, Actor-scoped workhorse responsible
for integrity. A later client layer should own roles, permissions, visibility, and safe workflows.
The `script` module may enforce its color modes locally in the meantime.

Flattening does not mean exposing mutable collections. Every mutation must still pass through the
single mechanism that maintains its structure and history.

## Why change

The current hierarchy is nominal rather than protective:

```text
Gameplay -> TurnLayer -> OperationLayer -> TaskLayer -> GodMode
```

`ApiTranslation` implements every layer. `Gameplay.godMode()` reveals the bottom and callers cast
the same object back to intermediate layers. Script colors reconstruct policy with those casts.

This complicates signatures without enforcing authority. It also confuses API taxonomy with the
real safety mechanism: coordinated mutation.

## Target workhorse

One Actor-scoped engine facade should expose the supported trusted operations:

- contextual parsing and queries;
- task selection, narrowing, execution, insertion, and removal;
- manual operations and turns;
- read-only task analysis usable by explicit clients and optional autoexecution policies; and
- conspicuously named rules-bypassing changes.

`godMode()` and the intermediate power interfaces disappear. Keep dangerous primitives internal;
publish only integrity-preserving operations.

The workhorse must preserve:

1. rollback of every component, task, index, and event change in a failed atomic command;
2. synchronization of component multiplicity and live effects;
3. normalized, logged task mutation;
4. one coherent global select-lock;
5. one Game World's Class Table identity;
6. Actor/context semantics and event attribution; and
7. committed initialization/workflow rollback floors.

A raw change may bypass Pets and triggered effects. It may not corrupt indexes, omit history, or
become irreversible.

## Command scopes

Public command-like operations should eventually use one explicit lifecycle:

```text
checkpoint
run requested operation
collect TaskResult
rollback on failure
publish one outermost completion notification
return
```

First characterize the current transaction, result, and notification behavior of task
selection/narrowing, insertion/removal, manual operations, turns, and raw changes. Normalize only
deliberate differences. Autoexecution is an optional client of this raw command lifecycle, as
specified in [AUTOEXEC.md](AUTOEXEC.md), not a stage inside it.

## Select-and-narrow API

The public primitives expose the two ways a Player plays; internal names expose the engine
consequences.

| Name | Contract |
| --- | --- |
| `Task.selected` | This task holds the select-lock; it need not yet be concrete. |
| `TaskQueue.selectedTask()` | Returns the Selected Task, if any. |
| `Gameplay.canSelectTask(id)` | Checks whether Selection and its initial Resolution can succeed. |
| `Gameplay.selectTask(id)` | Selects and resolves one Pending Task, delegates an abstract result when needed, then executes it immediately if concrete. |
| `Gameplay.selectTask(text)` | Convenience matcher with the same Selection semantics. |
| `Gameplay.narrowTask(text)` | Narrows only the Selected Task, resolves it again, and executes if concrete; partial narrowing remains recorded. |
| `Instructor.resolve(...)` | Applies World-dependent instruction semantics without making a Player choice. |
| `Instructor.executeResolved(...)` | Executes the already-resolved first stage. |
| `CustomClassRuntime.translateInstruction(...)` | Names the custom-class responsibility directly. |

The Player-facing core should therefore reduce to:

```kotlin
fun canSelectTask(taskId: TaskId): Boolean
fun selectTask(taskId: TaskId): TaskResult
fun narrowTask(narrowing: String): TaskResult
```

The Selected Task supplies its identity and current wider Specification. String parsing and matching
remain in `ApiTranslation`. Each mutating call resolves and, once concrete, executes before
returning; a partial narrowing returns with the same task still selected, though its assignee may
have changed while its controller remains stable.

`GameReader.resolve(Expression)` and `ClassTable.resolve(Expression)` keep their names: converting a
symbolic specification into its contextual meaning is the same concept. Private helpers should
follow the public vocabulary (`canSelectAnyTask`, `executeSelectedTask`, `resolveChange`,
`resolveOr`).

`doTask(narrowing, taskNumber)` and `tryTask(...)` need not be split merely for lexical purity. They
are combined commands that match a task, select it, resolve it, apply the submitted narrowing, and
execute once concrete, in that order. Their KDoc should state that composition and they must obey the
same invariants as the primitives. An explicitly submitted group bundles execution of the resulting
siblings; a group exposed only by Preprocessing or Resolution completes its structural task and
leaves ordinary Pending Tasks. The script command for explicit Selection is `task select`.

Selection must precede narrowing, so contextual Type narrowing occurs while the select-lock protects
the World it read. Partial narrowing produces `TaskEditedEvent`s and survives subsequent client
requests, but it does not produce a `ChangeEvent`.

If Resolution or Narrowing exposes several independent instructions, the selected structural Task
completes and the instructions become ordinary Pending Tasks. No child inherits Selection.

`ApiTranslation` should remain a value/string adapter rather than the owner of all engine policy.
A focused command runner can own atomicity, result collection, agent provenance, and completion
notification.

## Temporary script policy

Until a real client API exists, `script` should check its modes explicitly in one place:

| Mode | Allowed policy |
| --- | --- |
| Purple | Resolve offered tasks while workflow stays engine-controlled |
| Blue | Enter user actions through turns |
| Green | Run arbitrary complete operations |
| Yellow | Also insert and remove tasks |
| Red | Also perform raw changes without normal triggered consequences |

These checks are REPL policy. They are neither security nor a reusable authorization framework. Test the
command/mode matrix so new commands cannot bypass it accidentally.

## Later client interface

Build the restrictive API around the flat workhorse only when real clients require it. Likely
surfaces include player-visible queries and choices, player actions, workflow monitoring,
administration, tests/diagnostics, and Terraforming Mars read models.

The dependency direction is:

```text
normal client -> policy/observation API -> trusted engine workhorse
scripts, tests, diagnostics -----------> trusted engine workhorse
```

Do not introduce capability interfaces or dependency-injected permission objects into the current
engine as speculative scaffolding.

## Proposed responsibility split

**Not implemented.** Flattening the caller-facing workhorse does not mean one implementation Class
should accumulate every policy:

- `ApiTranslation` should become only the string/value adapter for the flat surface.
- A named command runner should own atomicity, result collection, agent provenance, and the one
  outermost completion notification.
- The generically named `Implementations` should disappear or become a plainly named facade over
  focused task lifecycle, task selection, operation completion, and raw-change services.
  Terraforming Mars turn signaling does not belong in those generic services.
- `ScriptSession` should retain interactive session state and command dispatch. An injected
  application profile should contribute Terraforming Mars construction, commands, completion
  sources, workflow, vocabulary, prompt metadata, and colors.
- `TfmGameplay` should not remain a permanent bundle of player actions, workflow/phase controls,
  test conveniences, and read-model calculations. Those are distinct future clients of the
  workhorse.

This split is an aspiration to guide ownership during the mechanical flattening. It does not
authorize building the later permission API at the same time.

## Safe sequence

1. Add behavior tests for script modes, rollback, `TaskResult`, current autoexecution coupling,
   outermost notification, and reversibility.
2. Move operations onto one `Gameplay` workhorse and remove `godMode()` plus the intermediate
   interfaces without changing behavior.
3. Replace script casts with centralized checks.
4. Collapse any remaining read/write API pair one structure at a time while keeping raw mutation
   internal.
5. Extract and name the command lifecycle.
6. Remove obsolete bindings, casts, and documentation.
7. Stop. Design the safe client API separately from actual observation and workflow requirements.

Open naming and exact command-scope questions do not change this direction. Do not mix
Actor-local auto-exec, native workflow delegation, hidden-information observation, or disposable
state forks into the mechanical flattening.

## Open questions and risks

These remain intentionally unsettled:

1. Whether the flat workhorse should still be called `Gameplay`, or instead `EngineSession` or
   `ActorEngine`.
2. Which methods are complete commands and which are primitives composed inside another command.
3. Which coordinated child-object mutations should eventually become direct workhorse operations.
4. How much timeline control belongs on the root facade versus `World.timeline`.

Risks to preserve during the work:

- A flat trusted API is easy to misuse; rules-bypassing names and KDoc must be conspicuous.
- Script mode checks can drift; keep the matrix centralized and tested.
- Collapsing a read/write pair can accidentally expose a corrupting primitive; the backing
  collections and synchronization operations stay internal.
- Type flattening can accidentally change transactions; characterize behavior before normalizing it.
- The temporary REPL policy can become accidental architecture; no other client should depend on it.
