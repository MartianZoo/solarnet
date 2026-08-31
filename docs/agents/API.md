# Agent API and later client permissions

> **Read when:** changing `Agent`, `World.agent`, command atomicity, script access modes, or a
> future restricted client API.
>
> **Status:** the flat Agent is current. A permissions model is deliberately deferred.

## Source map

- [`Agent.kt`](../../src/common/dev/martianzoo/engine/Agent.kt) is the public Actor-scoped command
  surface.
- [`ApiTranslation.kt`](../../src/common/dev/martianzoo/engine/ApiTranslation.kt) translates its
  string-facing commands into engine operations.
- [`World.kt`](../../src/common/dev/martianzoo/engine/World.kt) returns each Actor's stable Agent.
- [`Access.kt`](../../src/common/dev/martianzoo/script/Access.kt) enforces REPL color-mode choices
  locally.
- [`AUTOEXEC.md`](AUTOEXEC.md) owns optional policy attachment and scheduling.

## Current rule

A World has exactly one stable `Agent` per Actor. `World.agent(actor)` always returns that object.
Every ordinary mutation attributed to the Actor passes through it, including commands submitted by
an attached autoexecution policy.

The Agent is intentionally fully permissive. It combines:

- component and type queries;
- task inspection, selection, narrowing, execution, insertion, and removal;
- turn and manual-operation lifecycle commands;
- direct `sneak` changes;
- parsing; and
- autoexecution policy attachment.

There are no power subinterfaces, casts, or escape method revealing a stronger object. This is a
trusted engine workhorse, not a claim that every caller should receive every method.

## Command semantics

Mutating Agent calls are failure-atomic. After a successful outer command has completed its inline
effects, attached policies run according to [AUTOEXEC.md](AUTOEXEC.md). A nested call participates
in the surrounding transaction; operation-body task commands expose deliberate policy completion
points between statements.

`manual()` seeds an operation, permits its body to finish the resulting work, and rejects newly
leftover tasks or temporary components. Pre-existing unselected tasks may remain; a pre-existing
selected task prevents a new manual operation.

`beginManual()`, `continueManual()`, and `finish()` expose the resumable form used by workflows and
domain helpers. `addTasks()`, `editTask()`, `dropTask()`, `dropTasks()`, and `sneak()` are equally
public, trusted Agent commands and use the same atomic completion path.

Task selection begins in the assignee's Agent. When resolution leaves selected work abstract, the
task moves to its narrower's Agent while its controller remains stable and receives ensuing work.
An instruction-level `BY` changes the performer credited on state changes, not the Agent authorized
to select or narrow the task.

## Script modes

Red, yellow, green, blue, and purple script modes are client workflow choices. `Access` decides
which Agent commands a REPL command may invoke. They are not engine types and cannot be used as a
permissions guarantee.

This preserves the useful interactive modes without rebuilding the former type hierarchy in a new
shape.

## Later permissions model

Restricted clients, hidden information, roles, and safe workflows need a separate design. That
design may sit before Agent as a client-facing command service, after Agent as controlled access to
trusted capabilities, or use both where information visibility and mutation authorization differ.

Do not weaken Agent or split it into capability interfaces while exploring that work. First name a
real caller and the exact authority or visibility it needs. The permissions model should translate
approved intent into Agent commands and remain independently testable.

Important later questions include:

- whether read visibility and mutation authority require separate objects;
- how a Player-relative view hides hands, decks, and private history;
- how roles are represented without leaking trusted mutation primitives;
- whether a submitted command is revalidated when World state changes; and
- how policy identity and human/session identity remain diagnostic rather than game rules.

## Public API policy

There are no known external clients requiring obsolete aliases. Rename or remove public APIs when
the model improves instead of keeping compatibility wrappers. Script or replay syntax is a separate
user-visible contract: call out any needed change before adopting it.

[GAME_WORLDS.md](GAME_WORLDS.md) separately owns immutable recordings, live checkpoints, and
future disposable Worlds. Independent recording navigation is read-only and does not belong on
the trusted Agent command surface.
