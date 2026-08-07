# The Solarnet Engine module

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

The job of a game engine is to *know the rules of the game*. That is, at any given point, it knows what choices a player is allowed to make, and exactly what happens next if they do.

This module's job is to represent a world, execute instructions, and trigger effects, keeping all these activities tightly coordinated. It also has optional workflow engine that orchestrates the overall flow of a game phase by phase.

---

## Overview: The Holy Trinity of a World

The common live abstraction is `World`: a Pets component graph together with its tasks, event
history, timeline, class table, and Actor-scoped mutation API. `GamePremise` is the immutable,
reusable input—ruleset, class roots, Actors, and initial components—from which equivalent playable
worlds can be created.

`Engine.newSetupWorld` creates an independent world for collecting setup components. Once it is
idle, `Engine.newGame(setupWorld, assemble)` gains the setup ruleset's `ValidateSetup` signal,
snapshots the world as a `GamePremise`, then constructs a separate playable world. Canon supplies
its own setup ruleset, initial components, validation effects, and assembler. Callers construct a
canonical setup premise with a player count, signed `Canon.GameOptions`, and optionally selected
colony class names. Included options express user intent; exclusions mask effect-contributed
defaults. For example, `TerraformingMars` adds Corporate Era and `VenusNextExpansion` adds
`WorldGovernmentOption` unless explicitly excluded. Canon also derives `SoloMode` or
`MultiplayerMode` from player count. SoloMode defaults to `StandardSoloVariant` unless another
`SoloVariant`, such as `Tr63SoloVariant`, is selected.

Setup-world `GameOption` components are editable. Same-named gameplay declarations replace them
during assembly and inherit immutable `GameModule`, so the playable world contains only the fully
resolved, affirmative rules active at every equivalent table. Exclusion components are never
copied. Pets validation requires the base game, exactly one map, a mode consistent with player
count, and a solo variant exactly when SoloMode is active.

The `ClassTable` of active classes and authority-known inactive phantom classes is immutable. APIs
that enumerate playable classes exclude phantoms, while type resolution accepts them with zero
count. `GameReader.ruleset` is the selected ruleset from the premise. Terraforming Mars-specific
clients can use
`GameReader.tfmRuleset` to access its typed card, map, milestone, action, and colony registries.

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

`Custom` classes are never admitted to this multiset. `ComponentGraph.applyChange` enforces that
invariant at the graph boundary. A custom metric may report a virtual count for a type, but that
value does not add components, affect the total `Component` count, fire effects, satisfy
dependencies, or appear in component enumeration.

Generally an multiset isn't a "graph", but in our case, component instances themselves carry
references to their dependency components, which the component graph ensures are always present and
valid.  For example, a `GreeneryTile<Player1, Tharsis_5_6>` depends on both `Player1` and
`Tharsis_5_6` (a hex area of a map). It is always a valid *type*, but to create any *components* of
that type, a `Player1` and `Tharsis_5_6` component must already exist.

Each component has a type, which is some *concrete* PETS type extending `Component` (represented in
Kotlin as a `Type`). And... that's it! No identity, no properties. Two components are equal if
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
live effects to produce further component changes.

---

## The Event Log

Every state change and task event is appended via `EventLog` as a `GameEvent`. There are
four event types:

- `ChangeEvent` — a `StateChange` plus the performing `actor` and cause
- `TaskAddedEvent`, `TaskRemovedEvent`, `TaskEditedEvent` — task lifecycle

The event log is the basis for **rollback**: `TimelineImpl.rollBack(checkpoint)` iterates the
log backward from the current end to the checkpoint, reversing each event in turn.

`EventLog.record` and `EventLog.rollBackTo` are the single mutation/history boundary. A component
or task mutation is supplied to that boundary and must succeed before the corresponding event is
appended or removed. Each successful application or reversal also advances an opaque
`WorldRevision`. Unlike an event-count checkpoint, a revision is never reused after rollback, so a
future overlay can reliably notice any mutation of its backing world.

An event log may capture another log as an immutable prefix. Capturing retains the source and its
current size and setup checkpoint directly, so creating the suffix is constant-time in the length
of the existing history. Later source events are excluded; the captured prefix must not be rolled
back while the suffix exists.

Change events render the performing Actor with `BY` and the effect-bearing causal component with
`VIA`, followed by the causal event ordinal: `+OxygenStep BY Player2 VIA GreeneryTile<...> BECAUSE
448`.

---

## The Task Queue

The internal task queue manager is `TaskQueues`, which owns the set of `Task` objects and all task
mutation. Task order has no game meaning: stable `TaskId` iteration only makes arbitrary API and
auto-exec choices reproducible. Public readers and gameplay operation bodies see scoped `TaskQueue`
objects.
Those views may be scoped; for example, gameplay for an assignee exposes only that assignee's tasks,
while `World.tasks` remains a global view for diagnostics and workflow checks. Query and mutation
operations live on the same `TaskQueue` type; mutations still delegate to `TaskQueues`, which owns
normalization, storage, and event logging. Only queries used across module boundaries are public;
the engine's mutation and bookkeeping operations remain internal.

Each task has:

- `id` — a `TaskId` wrapping the numeric ordinal of its original `TaskAddedEvent`, stable through edits
- `instruction` — the Pets instruction still to be carried out (may be abstract)
- `assignee` — whose pending work contains the task and whose scoped gameplay may narrow it
- `cause` — what originally triggered this task (a `Cause` linking to a prior event)
- `next` — boolean marking the task as "prepared" (below)
- `then` — some tasks carry a follow-up instruction to automatically enqueue when they finish
- `whyPending` — diagnostic string set when autoexec can't resolve a task

Triggered work is represented as `PendingTask` until it is actually admitted to a queue, so work
handled inline never receives a task id. A split instruction and a `THEN` tail create new tasks and
therefore receive the ordinals of their own add events.

For nonautomatic effects, `Effector` currently assigns a `PendingTask` first to the Player owning
the effect-bearing component, otherwise to the Player owning the changed component, and otherwise
to the triggering Actor. Contextual `Owner` specialization is calculated separately. This broad
compatibility policy is observable behavior: it gives Player2 Philares's resource choice when
Player1 creates the adjacency, distributes Enceladus card-resource choices to colony owners, and
helps the generated per-Player Splice watchers give each tag owner their choice. These cases are
covered by integration tests and are not known gameplay failures.

Instruction-side `BY` does not change this assignment. It is only a performer override. World
Government Terraforming therefore works because the StartToken Owner is already the task assignee
while `BY Engine` attributes the selected increase to Engine. Icy Impactors uses the same separation
between the Player choosing a task and the Actor performing its ocean placement.

The script module separately assigns letter selection handles only when it presents tasks to a user
or offers task completions. A handle remains attached while its task is pending. Once no
lettered task remains pending, the next requested handle starts again at `A`; tasks completed by
autoexec before presentation consume no letters. Label assignments are checkpointed with the event
timeline so rollback restores both prior handles and tasks that had not yet received a handle.

Task assignment and queue membership are the same fact: an assignee's scoped view contains that
assignee's tasks. This remains true if the physical implementation is one collection with filtered
views. Do not introduce another identity field on every task merely to model temporary workflow
control.

There is currently no queue-suspension graph or parent/child control-scope state. A globally
prepared task locks out preparation of every competing task, which protects isolated cross-Actor
choices but does not describe a complete delegated turn. `TfmWorkflow.Auto` instead starts each
Player operation directly and waits for whole-world idleness. Native workflow will need an explicit
control-scope completion mechanism before it can retain an Engine continuation while Player work
remains active; see [IDENTITY.md](IDENTITY.md) and [WORKFLOW.md](WORKFLOW.md).

Tasks are fundamentally a **unit of assignee choice**, in two ways. First, the assignee gets to
choose which of their tasks to prepare (a very interesting feature of this particular game's rules).
But also, whenever a player needs to pick something (which card to play, which tile location, which
option of an Or, how many of something to steal, etc.), that manifests as a task whose instruction
is *abstract*. The instruction needs to be refined to a concrete instruction (as the player makes
their choices) before it is executed.

There are several ways for a task to be abstract: its component type is abstract, it has an
"intensity" other than `!`, it includes an `OR` instruction, etc. Anything that makes it less than
fully specified.

Sometimes refining a task from concrete to abstract depends on reading the world. For example any
"as much as possible" (AMAP) quantifier has to read how much is, well, possible. Once that resolution
happens, the task is marked as "prepared", meaning that it *must* be the next one executed.  If we
let any other task jump ahead, it could change the world that was already read.

---

## Instructions and How They Execute

`Instructor` is the thing that executes Pets instructions. These take various forms:

- `Change` — the core: gain N of X, remove N of Y, or transmute N of Y into X
- `Then` — sequential composition: do A, then B
- `Or` — a choice (player must revise to pick one branch)
- `Gated` — conditional: only execute if a requirement is met
- `Per` — scaled: multiply the inner instruction by a metric count
- `By` — perform the inner instruction as a specified concrete Actor without changing its task assignee
- `Multi` — parallel splits (used by atomization; see below)
- `NoOp` — does nothing

The process has two stages.

### 1. Prepare

`instructor.prepare(instruction)` evaluates the *current world* to simplify an instruction
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

- `Change` actually calls `changer.change(...)`, which calls `components.applyChange(...)`, then logs
  the event via `EventLog`. As noted this informs `Effector`; automatic effects execute inline
  (recursively), while queued effects are returned as new tasks.
- `Then` recursively executes each sub-instruction
- `NoOp` does nothing
- `Per`, `Gated`, `Or` -- these would cause an error as the instruction was never prepared.

The return value of `execute` is a list of `PendingTask` objects produced by queued effects.

---

## Metrics and Custom Metrics

`GameReader.count(Metric)` evaluates metric ASTs used by requirements, `/` instructions, awards,
and the REPL `count` command. An ordinary `Metric.Count` delegates to the component graph. If the
resolved expression's root is a `Custom` class with a `CustomMetric` implementation, the reader
instead asks that implementation for its non-negative virtual count.

`Metric.Or` evaluates the multiset union of ordinary component-count alternatives. For each exact
component type it keeps the greatest matching multiplicity, so overlapping alternatives do not
double-count components. Custom metrics cannot be alternatives because their virtual results have
no component identities to union.

The custom class and each dependency argument passed to its Kotlin implementation must be concrete.
The engine rejects unsupported abstract queries before invoking the implementation, so plugins do
not need to repeat that validation. Custom metrics may still decide how absent components and
refinements affect their answers.

A ruleset registers custom classes by capability. One Kotlin object may supply both instruction
and metric implementations, or two objects with the same Pets class name may supply the
capabilities separately. By default, each implementation's Pets class name is its Kotlin class's
simple name.

Canon uses virtual-property metrics for printed card cost, printed standard-project cost, presence
of a card requirement, map row, and placement bonus. Distinct tag and resource type counts instead
use the represented-type linkage in refinements such as `Class<Tag>(HAS Tag<Owner>)`.
`ClassCardRequirement` is the class-token
counterpart of `CardRequirement`; it is used when the value being refined is a `Class<CardFront>`
dependency such as the one on `PlayCard`.

Card play checks a printed requirement directly first. If an unmet requirement is a simple count
of a global parameter, Canon converts the difference into temporary `Required` barriers. Effects
such as Inventrix and Adaptation Technology remove a limited number of those barriers, while
Ecology Experts removes the entire shortfall for its selected card. The final card
conversion remains gated on having no barriers, so an unadjusted shortfall still rejects the play.
Non-global and compound requirements continue through ordinary requirement evaluation.

Ecology Experts adds its printed tags only after its selected card enters play. The selected card
therefore observes those tag gains through ordinary effect dispatch.

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
`ComponentGraph.applyChange` throws `ExistingDependentsException`. `Changer.change` catches
this and recursively removes the dependents first (via `removeAll`), then retries the original
remove. This cascades as needed. Note that `changer.change` returns `(event, done)` where `done`
is false if it had to stop to remove a dependent — the `Instructor` loops until `done` is true.

---

## The Effector: From Live Effects to Triggered Instructions

The same authored behavior has several representations during its lifecycle. A **source effect** is
transformed into a loaded **class effect**, bound to a concrete component as a **component
effect**, and registered at runtime as a **live effect**. Matching a live effect against a
particular change produces a **triggered instruction**. See the [glossary](../../glossary.md) for the
precise definitions.

The `Effector` maintains a registry of all **live effects** (one per component-effect pair,
counted by how many of that component exist). When a `ChangeEvent` fires, the effector is asked
`fire(triggerEvent, automatic)` and returns a list of `Task` objects to execute.

Each effect has a `Trigger` which is one of:

| Trigger | Fires when... |
|---------|--------------|
| `WhenGain` | the component carrying this effect is itself gained |
| `WhenRemove` | the component carrying this effect is itself removed |
| `OnGainOf<X>` | any component of type X is gained anywhere in the game |
| `OnRemoveOf<X>` | any component of type X is removed |
| `Or` | any one of two or more alternative triggers matches |

These can be wrapped:
- `ByTrigger` — only fires if the Actor recorded on the `ChangeEvent` matches its `BY` selector
- `IfTrigger` — only fires if some condition is currently met
- `XTrigger` — triggers that can match multiple times at once (e.g. Manutech, if raising production
  5 steps, get 5 resources, without processing those as individual state changes)

An effect belonging to an `Owned` component responds only to its Owner when it subscribes to an
unowned component and has no authored `BY`. This is the Pets form of the published game's ordinary
(non-red-outline) trigger icon. An authored `BY Anyone` is unrestricted. Unowned `System`
components represent engine-only machinery, so the Actor that caused their occurrence does not
implicitly restrict their subscribers; Players are also prohibited from creating them. `Hidden`
is separate: it controls whether an implementation component normally appears in user-facing
output. `System` extends `Hidden`, while `Signal` is hidden without necessarily being engine-only.
These rules affect subscription matching and do not rewrite the authored effect.

Pets spells the self triggers `This:` and `-This:`. They fire only for the occurrences of the
effect-bearing concrete type changed by this event; they are not ordinary subscriptions to that
type. Adding or removing `N` copies scales the instruction by `N` once, regardless of how many
other copies already exist. For other triggers (`OnGainOf<X>`/`OnRemoveOf<X>`), the effector checks
all registered live effects against each new change event, so their live multiplicity does
matter.

Creating a component effect applies the same checked narrowing to its instruction: an invalid
atomic consequence becomes `Die`, allowing an enclosing `OR` to discard it. Every remaining type
is then validated; an invalid trigger, gate, or other expression is a class-modeling error and fails
with the component and bound effect in the error message. The deliberate exception is an effect
declared by a supertype and applied to a passive, non-Player Owner when that effect requires
Player-bound output; that inapplicable effect is omitted.

When a concrete change narrows an abstract trigger, the same narrowing is applied to the exact
source expressions linked across that trigger and its instruction. Other occurrences of the same
class remain independent. If narrowing makes an atomic change's type invalid, that change becomes
`Die`; an enclosing `OR` discards the impossible branch, and fails if no branch remains.

When a live effect fires, if the effect is **automatic** (double-colon in Pets syntax), the
`Instructor` executes its triggered instruction inline in the same change loop. If the effect is
**queued** (single colon), its triggered instruction becomes a new `Task` appended to the
queue.

The `Task.assignee` field records whose queue contains deferred work and whose scoped gameplay may
narrow it. When a gameplay context executes the task, that context's Actor normally performs the
resulting state changes and receives their `ChangeEvent.actor` attribution. An instruction-level
`BY` explicitly overrides that performer without changing who owns or may narrow the task.
Trigger-level `BY` independently matches the Actor on the triggering `ChangeEvent`.

For automatic effects the temporary `PendingTask` still carries routing metadata in its `assignee` field,
but execution remains inline through the triggering Actor's `Instructor` and `Changer`, so
resulting change events retain the triggering Actor.

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

A checkpoint is an event ordinal and may be reached again after rollback. `WorldRevision` instead
advances on both forward and reverse event application; it identifies whether the live world has
remained untouched, not merely whether it currently has the same number of events.

---

## Preprocessing: The Transformers Pipeline

Before any instruction string reaches the engine, `ApiTranslation` runs it through a chain of
`PetTransformer` passes (assembled once per player scope):

1. **`canonicalize(vocabulary)`** — resolves localized Pets names and client-configured input-only synonyms to canonical class names
2. **`useFullNames()`** — resolves transitional structured-definition ids to canonical class names
3. **`atomizer()`** — expands `3 Heat` (where Heat is `Atomized`) into `Multi(Heat, Heat, Heat)`,
   so that each unit triggers effects individually
4. **`insertDefaults()`** — fills in omitted dependency arguments using the class's declared
   defaults (e.g., a bare `Plant` inside Player1's instruction becomes `Plant<Player1>`)
5. **`replaceOwnerWith(player)`** — replaces the `Owner` placeholder with the actual acting player
6. **`Prod.deprodify()`** — unwraps `PROD[...]` notation into actual production-component instructions

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
      TaskLayer  ← addTasks(), dropTask(), dropTasks()
        GodMode  ← sneak() (bypasses instruction machinery)
```

- **Normal callers** use `Gameplay` directly — they can revise abstract tasks, prepare them, and
  execute them, selecting by either generated task id or current instruction. Selecting by current
  instruction also accepts multiple tasks that are otherwise identical, since those tasks are
  interchangeable.
- **`OperationLayer`** is for structured operations: `manual()` adds the instruction as tasks and
  runs those new tasks to completion (including autoexec), while preserving tasks that were already
  pending but unprepared. A prepared task remains a global lock and prevents `manual()` from
  starting. An abstract initial instruction remains pending for the operation body to narrow.
  Before returning, `manual()` verifies that no new tasks or `Temporary` components remain.
- **`OperationBody.tasks`** is the assignee's scoped read-only queue view.
- **`TaskLayer`** lets you inject arbitrary tasks and remove one task or every task assigned to the
  caller for any reason.
- **`GodMode`** lets you make raw changes to the component graph, bypassing instruction
  preparation and effect firing (`sneak()`).

The concrete implementation is `ApiTranslation`, which wraps most command-style methods in
`atomic` and invokes `autoExecNow` at the end of each outermost adapter atomic block. Some methods,
including task preparation/editing and raw debug changes, still have mixed transaction semantics;
see `docs/agents/API.md` for the current API cleanup direction.

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
- `production(resource)`, `oxygenPercent()`, `temperatureC()`, etc. for reading the world
  translated to human terms (TODO: do these belong?)

### `TfmWorkflow`

Orchestrates the full game loop using Kotlin coroutines. The workflow coroutine runs the complete
game sequence (Setup → Corp → [Prelude] → Action → Production → Solar → Generation → Research →
repeat) as straight-line sequential code. Solar checks for `LastCall` before the workflow proceeds
to Generation. The workflow suspends whenever it calls `awaitTasksDrained()`, which commits the
current state (preventing rollback past this point) and then waits on a rendezvous channel.

When `WorldGovernmentOption` is active, its `VenusSolarPhase` effect automatically creates a transient
`WorldGovernmentTerraforming` signal unless the game is ending or every global parameter is complete.
The `StartToken` component responds by assigning its Owner the
mandatory `GlobalParameter! BY Engine` choice. Thus the first player narrows the task, maxed tracks
are illegal choices, and Engine remains the performer for effect matching and event attribution.

The hook `game.onAtomicComplete` is wired to send a signal on this channel whenever the task
queue drains after an atomic operation completes. Since the channel is `RENDEZVOUS`, signals
fired when no one is waiting (e.g., during automatic engine phases) are silently dropped.

Action and final-greenery turn order begins with the player who owns `StartToken`. Creating each
generation after the first passes that token one seat left, so the workflow reads turn order from
the world rather than maintaining a separate generation counter. During the action phase, players
receive an optional second action only while at least two players have not passed.

In multiplayer, a rules component watches the temperature, oxygen, and ocean `GpComplete` markers
and creates `LastCall` when all three exist; Venus completion is deliberately irrelevant. Solo
creates the same marker upon entering its final configured generation. The workflow consults only
that shared fact. After the final solo production, `SoloVictoryCheck` awards `Victory<Player1>` only
if the selected solo objective is complete. The standard variant checks the applicable global
parameters; TR 63 checks for 63 terraform rating and adds the 16 M€ Buffer Gas standard project.
The workflow enters final greenery and scoring only when that victory component exists. Multiplayer
end scoring first measures awards and assigns their places,
then `End` pays out ordinary and award victory points. Once `FinalScore` exists,
`MultiplayerVictoryCheck` awards `Victory` to the highest final score with megacredits as the
tiebreaker.

This design means:
- The game flow reads naturally (setupPhase, corporationPhase, then preludePhase, then action loop,
  etc.)
- Player turns are just `beginManual(instruction)` followed by `awaitTasksDrained()`
- The workflow never needs to poll — it wakes up exactly when the queue empties

---

## Wiring it all together

`Engine.newGame()` delegates construction to `Engine.Wiring`, the engine's manual dependency-injection
composition root. Each world owns a locale-specific `Vocabulary`; configured input-only synonyms
are canonicalized there before type resolution and are not part of the `ClassTable`. Game-level objects (`ClassTable`, `Effector`, `EventLog`, `ComponentGraph`, etc.)
are shared across all players. Each configured Actor gets its own
`Changer`, `Instructor`, `Implementations`, and `ApiTranslation` (the `Gameplay` implementation).
The Engine Actor also supplies the `Initializer` used during bootstrap.

Components expose their concrete Owner as a resolved Pets type. Kotlin runtime identities retain
separate `Actor` and `Owner` roles where code needs an entity to participate directly; `Player` is
their current intersection. Only Actors receive gameplay scopes and task queues. A passive Pets
Owner such as `SoloOpponent` has no corresponding Kotlin identity and receives neither capability.

The `Effector` takes a `GameReader` provider to break a bootstrapping cycle: the game's reader isn't
available until after the effector exists, but the effector needs the reader to fire effects.

After per-Actor gameplay is constructed and passed into the `World`, `Initializer.initialize()` runs for `ENGINE`.
It creates the administrative `ENGINE` component and all singleton-type components directly
through `Instructor`, without manufacturing a task for each top-level instruction. Automatic
effects still execute inline and queued effects still add ordinary tasks. Singleton types whose
dependencies do not exist yet are retried in progress-based rounds; a round with no progress
reports the unresolved types and dependencies.

Initialization then marks `initializationFinished()` and commits that pre-setup baseline. The
returned `World` therefore has no current Phase. `TfmWorkflow.Manual.setupPhase()` or
`TfmWorkflow.Auto` creates `SetupPhase` as an ordinary, fully effectful game operation. Automatic
workflow waits for any resulting setup tasks before entering `CorporationPhase`.

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
            → components.applyChange(count, g, r) (update component multiset)
            → changeLogger.addChangeEvent(…) (append to event log)
            → effector.fire(event, auto=true)
              → for each matching live effect → instructor.execute(...)  (recurse)
            → effector.fire(event, auto=false) → new Tasks appended to queue
        → handleTask(id)                     (remove from queue, enqueue task.then)
      → new tasks from execute → tasks.addTasks(...)
    → impl.autoExecNow(mode)                 (repeat until stable)
  → onAtomicComplete()                       (TfmWorkflow may wake up)
```
