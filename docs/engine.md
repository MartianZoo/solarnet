# The Solarnet Engine module

The job of a game engine is to *know the rules of the game*. That is, at any given point, it knows what choices a player is allowed to make, and exactly what happens next if they do.

This module's job is to represent game state (components and tasks), execute instructions, and trigger effects, keeping all these activities tightly coordinated. It also has optional workflow engine that orchestrates the overall flow of a game phase by phase.

---

## Overview: The Holy Trinity of Game State

The centerpiece is the `Game` object. Of course, everything you want to know about a game in progress is here.

For starters, it holds a `GameSetup` (the configuration chosen before the game began) and the `MClassTable` of loaded classes for that configuration. These are immutable. Clients read game state through a `GameReader`. 

Clients perform all mutative operations via the `Gameplay` interface. Internally, this mutable state is held in a trinity of child objects:

| Object | Metaphor | What it contains |
|--------|----------|-----------------|
| `ComponentGraph` | The **present** | A multiset of all current component instances in play |
| `TaskQueues` / `TaskQueue` | The **future** | What the engine knows each assignee must decide or do |
| `EventLog` | The **past** | The full history of changes to those things^^ |

The fourth critical piece is a `Timeline`, which coordinates atomic changes across those three child objects, and supports rollback and replay (which not only enable an "undo" feature but are actually crucial to normal engine operations).

---

## The Component Graph

The component graph is just a multiset, nothing more, nothing less. Literally all it supports is
add, remove, and count -- this is what makes rollback-and-replay so trivial!

Generally an multiset isn't a "graph", but in our case, component instances themselves carry
references to their dependency components, which the component graph ensures are always present and
valid.  For example, a `GreeneryTile<Player1, Tharsis_5_6>` depends on both `Player1` and
`Tharsis_5_6` (a hex area of a map). It is always a valid *type*, but to create any *components* of
that type, a `Player1` and `Tharsis_5_6` component must already exist.

Each component has a type, which is some *concrete* PETS type extending `Component` (represented in
Kotlin as an `MType`). And... that's it! No identity, no properties. Two components are equal if
their types are equal, period.

Being a multiset, the only mutation is

```
update(count, gaining, removing) → StateChange
```

This removes `count` copies of `removing` (if not null) and adds `count` copies of `gaining`
(if not null), in that order. Before removing, it checks whether any *other* components currently
depend on this one. If so, something slightly strange happens: it throws a particular exception
which is caught by `Changer` causing it to auto-remove the dependents first (!).

Crucially, `ComponentGraph` informs the `Effector` about every change it makes, which often causes
active effects to produce further component changes.

---

## The Event Log

Every state change and task event is appended via `WritableEventLog` as a `GameEvent`. There are
four event types:

- `ChangeEvent` — a `StateChange` plus the performing `actor` and cause
- `TaskAddedEvent`, `TaskRemovedEvent`, `TaskEditedEvent` — task lifecycle

The event log is the basis for **rollback**: `TimelineImpl.rollBack(checkpoint)` iterates the
log backward from the current end to the checkpoint, reversing each event in turn.

Change events render the performing Actor with `BY` and the effect-bearing causal component with
`VIA`, followed by the causal event ordinal: `+OxygenStep BY Player2 VIA GreeneryTile<...> BECAUSE
448`.

---

## The Task Queue

The internal task queue manager is `TaskQueues`, which owns the set of `Task` objects and all task
mutation. Task order has no game meaning: stable `TaskId` iteration only makes arbitrary API and
auto-exec choices reproducible. Public readers and gameplay operation bodies see read-only
`TaskQueue` views.
Those views may be scoped; for example, gameplay for an assignee exposes only that assignee's tasks,
while `Game.tasks` remains a global read-only view for diagnostics and workflow checks.
Internal code that mutates tasks uses `WritableTaskQueue`, following the same read-only/writable
split as the component graph and event log.

Each task has:

- `id` — a monotonically increasing `TaskId`
- `instruction` — the Pets instruction still to be carried out (may be abstract)
- `assignee` — whose pending work contains the task and whose scoped gameplay may narrow it
- `cause` — what originally triggered this task (a `Cause` linking to a prior event)
- `next` — boolean marking the task as "prepared" (below)
- `then` — some tasks carry a follow-up instruction to automatically enqueue when they finish
- `whyPending` — diagnostic string set when autoexec can't resolve a task

Task assignment and queue membership are the same fact: an assignee's scoped view contains that
assignee's tasks. This remains true if the physical implementation is one collection with filtered
views. Do not introduce another queue-control identity.

Tasks are fundamentally a **unit of assignee choice**, in two ways. First, the assignee gets to
choose which of their tasks to prepare (a very interesting feature of this particular game's rules).
But also, whenever a player needs to pick something (which card to play, which tile location, which
option of an Or, how many of something to steal, etc.), that manifests as a task whose instruction
is *abstract*. The instruction needs to be refined to a concrete instruction (as the player makes
their choices) before it is executed.

There are several ways for a task to be abstract: its component type is abstract, it has an
"intensity" other than `!`, it includes an `OR` instruction, etc. Anything that makes it less than
fully specified.

Sometimes refining a task from concrete to abstract depends on reading game state. For example any
"as much as possible" (AMAP) quantifier has to read how much is, well, possible. Once that resolution
happens, the task is marked as "prepared", meaning that it *must* be the next one executed.  If we
let any other task jump ahead, it could change the game state that was already read.

---

## Instructions and How They Execute

`Instructor` is the thing that executes Pets instructions. These take various forms:

- `Change` — the core: gain N of X, remove N of Y, or transmute N of Y into X
- `Then` — sequential composition: do A, then B
- `Or` — a choice (player must revise to pick one branch)
- `Gated` — conditional: only execute if a requirement is met
- `Per` — scaled: multiply the inner instruction by a metric count
- `Multi` — parallel splits (used by atomization; see below)
- `NoOp` — does nothing

The process has two stages.

### 1. Prepare

`instructor.prepare(instruction)` evaluates the *current game state* to simplify an instruction
as much as possible without actually changing anything:

- `Per`: count the metric and actually multiply the inner instruction by that value
- `Gated`: if the gate is met, unwrap; if optional return `NoOp`; else throw
- `Or`: each option within gets recursively prepared; those that would throw `NotNowException`
  specifially get pruned out
- `Change` is "auto-narrowed": abstract types are resolved to concrete where there's only
  one valid choice; limits are checked (see Limiter below); AMAP intensity is resolved
- Custom types delegate to their `CustomClass.prepare()` to produce a replacement instruction

### 2. Execute

`instructor.execute(instruction, cause)` is called only on a prepared, concrete instruction:

- `Change` actually calls `changer.change(...)`, which calls `updater.update(...)`, then logs
  the event via `ChangeLogger`. As noted this informs `Effector`; automatic effects execute inline
  (recursively), while queued effects are returned as new tasks.
- `Then` recursively executes each sub-instruction
- `NoOp` does nothing
- `Per`, `Gated`, `Or` -- these would cause an error as the instruction was never prepared.

The return value of `execute` is a list of `Task` objects produced by queued effects.

---

## Limits and Invariants

`Limiter` enforces that component counts stay within bounds declared as `invariants` on classes.
When the `Instructor` prepares a `Change`, it calls `limiter.findLimit(gaining, removing)` to get the
maximum allowable count for this operation given the current board state.

Limits interact with instruction *intensity*:
- `!` (MANDATORY): throws `LimitsException` if the limit is less than requested
- `?` (OPTIONAL): silently reduces to the limit (possibly 0), stays optional
- `.` (AMAP, "as many as possible"): like `?`, but gets upgraded to `!` in the prepared form
  once the limit is known

Invariants can reference `This` (meaning the type being constrained, resolved per-instance),
which is how per-player limits work. The invariants are computed lazily from the class table at
game start and compiled into a per-class lookup map.

---

## Dependency Removal Cascade

When you try to remove the last instance of a component that other components depend on,
`WritableComponentGraph.update` throws `ExistingDependentsException`. `Changer.change` catches
this and recursively removes the dependents first (via `removeAll`), then retries the original
remove. This cascades as needed. Note that `changer.change` returns `(event, done)` where `done`
is false if it had to stop to remove a dependent — the `Instructor` loops until `done` is true.

---

## The Effector: From Active Effects to Triggered Instructions

The same authored behavior has several representations during its lifecycle. A **source effect** is
transformed into a loaded **class effect**, specialized for a concrete component as a **component
effect**, and registered at runtime as an **active effect**. Matching an active effect against a
particular change produces a **triggered instruction**. See the [glossary](../glossary.md) for the
precise definitions.

The `Effector` maintains a registry of all **active effects** (one per component-effect pair,
counted by how many of that component exist). When a `ChangeEvent` fires, the effector is asked
`fire(triggerEvent, automatic)` and returns a list of `Task` objects to execute.

Each effect has a `Trigger` which is one of:

| Trigger | Fires when... |
|---------|--------------|
| `WhenGain` | the component carrying this effect is itself gained |
| `WhenRemove` | the component carrying this effect is itself removed |
| `OnGainOf<X>` | any component of type X is gained anywhere in the game |
| `OnRemoveOf<X>` | any component of type X is removed |

These can be wrapped:
- `ByTrigger` — only fires if the Actor recorded on the `ChangeEvent` matches its `BY` selector
- `IfTrigger` — only fires if some condition is currently met
- `XTrigger` — triggers that can match multiple times at once (e.g. Manutech, if raising production
  5 steps, get 5 resources, without processing those as individual state changes)

For "self" triggers (`WhenGain`/`WhenRemove`), the effect fires immediately when the component
that carries it is the thing being gained/removed. For "other" triggers (`OnGainOf`/`OnRemoveOf`),
the effector checks all registered active effects against each new change event.

When an active effect fires, if the effect is **automatic** (double-colon in Pets syntax), the
`Instructor` executes its triggered instruction inline in the same change loop. If the effect is
**queued** (single colon), its triggered instruction becomes a new `Task` appended to the
queue.

The `Task.assignee` field records whose queue contains deferred work and whose scoped gameplay may
narrow it. When a gameplay context executes the task, that context's Actor performs the resulting
state changes and receives their `ChangeEvent.actor` attribution. `BY` independently matches the
Actor on the triggering `ChangeEvent`.

For automatic effects the temporary Task still carries routing metadata in its `assignee` field,
but that field does not select the executor. Execution remains inline through the triggering
Actor's `Instructor` and `Changer`, so resulting change events retain the triggering Actor.

---

## Timeline: Checkpoints, Rollback, and Atomicity

`TimelineImpl` provides:

- `checkpoint()` — a snapshot of "how many events are in the log right now" (an ordinal)
- `rollBack(checkpoint)` — reverses all events logged after that checkpoint by walking the log
  backward (reversing changes by swapping gaining/removing; reversing task events by un-adding
  or un-removing tasks)
- `commit()` — advances the "floor": rollback can't go earlier than this (used by `TfmWorkflow`
  to prevent players from undoing engine-driven phase transitions)
- `atomic(block)` — runs `block()` inside a checkpoint; if any exception is thrown the entire
  operation is rolled back. Returns a `TaskResult` describing what changed. If `AbortOperationException`
  is thrown inside the block (via `OperationBody.abort()`), the rollback happens but `atomic`
  returns normally (not an error from the caller's perspective).

Every public `Gameplay` method wraps its work in `atomic`. This means all state changes within
one operation are either fully committed or fully reversed.

---

## Preprocessing: The Transformers Pipeline

Before any instruction string reaches the engine, `ApiTranslation` runs it through a chain of
`PetTransformer` passes (assembled once per player scope):

1. **`useFullNames()`** — resolves short class aliases to canonical class names
2. **`atomizer()`** — expands `3 Heat` (where Heat is `Atomized`) into `Multi(Heat, Heat, Heat)`,
   so that each unit triggers effects individually
3. **`insertDefaults()`** — fills in omitted dependency arguments using the class's declared
   defaults (e.g., a bare `Plant` inside Player1's instruction becomes `Plant<Player1>`)
4. **`replaceOwnerWith(player)`** — replaces the `Owner` placeholder with the actual acting player
5. **`Prod.deprodify()`** — unwraps `PROD[...]` notation into actual production-component instructions

This pipeline runs on every instruction string before it reaches `Implementations` or
`Instructor`. Instructions already in Pets AST form (from inside the engine) skip the string
parsing but can still go through some of these transforms as needed.

This owner-substitution pass is present only where an operation has Player ownership context.
`Engine` is not an Owner, so its instructions pass through without acquiring contextual ownership.

---

## The Gameplay API Layers

`Gameplay` is stratified into nested interfaces, each adding more power (and more risk):

```
Gameplay         ← query-only + task revision/preparation + doTask
  TurnLayer      ← startTurn(), turn()
    OperationLayer ← manual(), beginManual(), continueManual(), finish()
      TaskLayer  ← addTasks(), dropTask()
        GodMode  ← sneak() (bypasses instruction machinery)
```

- **Normal callers** use `Gameplay` directly — they can revise abstract tasks and execute
  prepared ones, but can't create tasks from scratch.
- **`OperationLayer`** is for structured operations: `manual()` requires the caller's queue is
  empty, adds the instruction as tasks, runs them to completion (including autoexec), and verifies
  the caller's queue is empty and no `Temporary` components remain.
- **`OperationBody.tasks`** is the assignee's scoped read-only queue view.
- **`TaskLayer`** lets you inject arbitrary tasks and remove them for any reason.
- **`GodMode`** lets you make raw changes to the component graph, bypassing instruction
  preparation and effect firing (`sneak()`).

The concrete implementation is `ApiTranslation`, which wraps most command-style methods in
`atomic` and invokes `autoExecNow` at the end of each outermost adapter atomic block. Some methods,
including task preparation/editing and raw debug changes, still have mixed transaction semantics;
see `docs/engine-api-review.md` for the current API cleanup direction.

---

## Auto-Execution

After each operation completes, `ApiTranslation.atomic` calls `impl.autoExecNow(mode)`. The
`AutoExecMode` determines how aggressive this is:

- **`NONE`**: Do nothing; the caller must handle all tasks manually
- **`SAFE`**: Only execute a task if it's the only preparable option right now (never removes
  a choice from the player that the rules allow)
- **`FIRST`**: Arbitrarily execute the first preparable task in the implementation's stable
  iteration order; keep going until the queue is empty or stuck (default mode)

`autoExecNow` runs in a loop calling `autoExecNext` until it returns false. It scans pending tasks
across the whole game and uses the assignee only to select the queue containing the task. The Actor
of the gameplay context calling `autoExecNow` performs and receives credit for the resulting state
changes, even when the task has another assignee. Tasks that fail are annotated with `whyPending`.
When only one option exists, it is executed. When multiple options exist, `SAFE` stops while `FIRST`
uses their stable iteration order to make an arbitrary choice.

---

## Terraforming Mars-Specific Layer

### `TfmGameplay`

A convenience wrapper around `TurnLayer` that adds Terraforming helpers:

- `playCorp(cardName, buyCards)`, `playProject(cardName, mc, steel, titanium)`, `cardAction1/2()`
- `phase(phaseName)` — executes a phase transition as `ENGINE`
- `pay(mc, steel, titanium)` — handles the payment sub-protocol (Owed/Accept tasks)
- `production(resource)`, `oxygenPercent()`, `temperatureC()`, etc. for reading game state
  translated to human terms (TODO: do these belong?)

### `TfmWorkflow`

Orchestrates the full game loop using Kotlin coroutines. The workflow coroutine runs the complete
game sequence (Corp → [Prelude] → Action → Production → Research → repeat) as straight-line
sequential code. It suspends whenever it calls `awaitTasksDrained()`, which commits the current
state (preventing rollback past this point) and then waits on a rendezvous channel.

The hook `game.onAtomicComplete` is wired to send a signal on this channel whenever the task
queue drains after an atomic operation completes. Since the channel is `RENDEZVOUS`, signals
fired when no one is waiting (e.g., during automatic engine phases) are silently dropped.

Action and final-greenery turn order begins with the player who owns `StartToken`. Creating each
generation after the first passes that token one seat left, so the workflow reads turn order from
game state rather than maintaining a separate generation counter.

This design means:
- The game flow reads naturally (corporationPhase, then preludePhase, then action loop, etc.)
- Player turns are just `beginManual(instruction)` followed by `awaitTasksDrained()`
- The workflow never needs to poll — it wakes up exactly when the queue empties

---

## Wiring it all together

The dependencies between all these things are shockingly complex and a pain to maintain manually. I
decided to adopt Koin.

`Engine.newGame()` builds a Koin DI container. The game-level singletons (`MClassLoader`,
`Effector`, `WritableEventLog`, `WritableComponentGraph`, etc.) are shared across all players.
Each configured Actor also gets a Koin scope containing `Changer`, `Instructor`, `Implementations`,
`ApiTranslation` (the `Gameplay` impl), and `Initializer`.

Runtime identity has the same two independent role axes as Pets: an `Actor` can execute operations,
and an `Owner` can own components. `Player` is their intersection. Only Actors receive these
gameplay scopes and task queues; merely being an Owner conveys neither capability.

The `Effector` takes a `Lazy<GameReader>` to break a bootstrapping cycle: the game's reader isn't
available until after the effector exists, but the effector needs the reader to fire effects.

After scopes are created, `Initializer.initialize()` runs for `ENGINE`, which:
1. Creates the administrative `ENGINE` component
2. Instantiates all singleton-type components (things with exactly one concrete subtype)
3. Runs any Colonies-specific setup (colony tiles, trade fleets)
4. Marks `initializationFinished()` in the timeline (so setup events are excluded from game logs)
5. Executes `SetupPhase`

TODO: of course, this shouldn't have TfM-specific steps embedded.

---

## Data Flow Summary

Here's a condensed picture of what happens when a player does something:

```
Player calls gameplay.doTask("3 Plant<Player2>!")
  → ApiTranslation.doTask(revised)          (wraps in atomic)
    → preprocessor.transform(parse(...))     (full names, atomize, defaults, owner, deprod)
    → impl.doTask(instruction)
      → matchingTask(instruction)            (find the right task in queue)
      → impl.prepareTask(id)
        → instructor.prepare(instruction)    (evaluate Per/Gated/Or, auto-narrow, check limits)
      → impl.doTask(id)
        → instructor.execute(instruction, cause)
          → changer.change(...)
            → updater.update(count, g, r)    (update component multiset)
            → changeLogger.addChangeEvent(…) (append to event log)
            → effector.fire(event, auto=true)
              → for each matching active effect → instructor.execute(...)  (recurse)
            → effector.fire(event, auto=false) → new Tasks appended to queue
        → handleTask(id)                     (remove from queue, enqueue task.then)
      → new tasks from execute → tasks.addTasks(...)
    → impl.autoExecNow(mode)                 (repeat until stable)
  → onAtomicComplete()                       (TfmWorkflow may wake up)
```
