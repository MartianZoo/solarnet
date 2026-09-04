# Agent API

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing the core mutation surface, `Agent`, `World.agent`, task-command
> authority, script access modes, or client-visible state.
>
> **Status:** selected layering direction with substantial current implementation divergence. The
> current flat Agent remains described here only as migration evidence.

## Source map

- [`Agent.kt`](../../src/common/dev/martianzoo/engine/Agent.kt) is the current fully permissive,
  Actor-scoped engine API.
- [`ApiTranslation.kt`](../../src/common/dev/martianzoo/engine/ApiTranslation.kt) currently combines
  parsing, atomic mutation entry, input recording, and legacy autoexecution scheduling.
- [`World.kt`](../../src/common/dev/martianzoo/engine/World.kt) currently returns stable Agents.
- [`TaskQueues.kt`](../../src/common/dev/martianzoo/engine/TaskQueues.kt) already stores one global
  task set; [`TaskQueue.kt`](../../src/common/dev/martianzoo/engine/TaskQueue.kt) is a filtered view.
- [`Access.kt`](../../src/common/dev/martianzoo/script/Access.kt) implements current script-only
  access modes.
- [GAMEWORLD.md](GAMEWORLD.md) owns task data and recording navigation;
  [RESPONSIBILITIES.md](RESPONSIBILITIES.md#selected-runtime-dependency-direction) owns the target
  dependency direction; [AUTOEXEC.md](AUTOEXEC.md) owns Agent policies and stable points.

## Core mutation surface

The core engine has no `Agent`, permissions policy, or autoexecution concept. It offers a few
distinct mutation methods, validates each call against one live World, and returns its atomic
result. A task retains one assignee in the global unordered task pool. Ordinary task calls name the
acting Actor, and the engine rejects action by anyone other than the task's current assignee. That
is game semantics, not caller permission.

The audited mutation families are:

- select a task;
- narrow a task;
- roll the live timeline back to an allowed Checkpoint;
- add one or more ex-machina tasks;
- drop one ex-machina task; and
- sneak an ex-machina state change.

Timeline commit-floor advancement and the atomic transaction wrapper are engine/workflow lifecycle
mechanics, not Actor mutations. `doTask` and `tryTask` compose task identification, selection,
narrowing, and error handling. `manual`, turn, and phase conveniences compose ex-machina task
addition with ordinary task action. None justifies a universal request type or
`engine.submit(actor, request)`.

The current flat Agent now exposes one checked id-based narrowing and one explicit ex-machina task
removal. It has no arbitrary task replacement or bulk task-removal command. Internal task-data edits
remain engine bookkeeping, including restoration around an evidenced replay correction.

## Narrowing before selection

Narrowing is allowed to discard options. That is a legitimate Actor decision, not a defect. A
candidate is valid only when the engine proves it narrows the stored task and cannot introduce an
option the task did not already permit.

An unselected task may receive a state-independent narrowing, such as replacing a Type with a
subtype established by immutable Class facts. It remains unselected and unexecuted. This operation
must not evaluate AMAP, a gate, a Metric, current viability, or any other mutable-World fact.
Selection establishes the promise to act next and the select-lock before those facts are resolved.
The current `Agent.narrowTask(taskId, narrowing)` implements this check; the selected-task overload
retains state-aware resolution and immediate execution when the result becomes concrete.

Provably permanent forced narrowing may likewise simplify an unselected task. “Probably forever”
is insufficient: the proof must use only immutable premise, Class, and task structure. Whether that
normalization belongs to engine task admission or an Agent policy remains open; both must use the
same checked narrowing relation.

## Agent

A configured Game World has exactly one Agent per Actor, including Admin. Every ordinary mutation
chosen autonomously or explicitly requested by an interactive client enters through that Actor's
Agent. Replay correction, tests, and workflows may deliberately use the lower-level engine API.
The Agent serializes its requests and calls the engine's Actor-attributed methods directly. An Agent
with no active autoexecution policy is a thin Actor-scoped client facade; a separate passive access
object would add no present responsibility and is not planned.

`Agent.reader` is a `ScopedGameReader`. In Player scope, contextual input such as `Plant` is
interpreted as `Plant<that Player>`, matching the current contextual `Owner` substitution.
`agent.reader.unscoped` returns the underlying `GameReader` so callers can deliberately inspect the
whole game without leaving the Agent API.

An Agent owns the policies that may autonomously choose further actions for its Actor. This makes
human and artificial players one model: a human-directed Agent may have no active policies, while
installing enough policies can make the same Agent fully autonomous. Public `autoExecNow` and
policy addition/removal belong on Agent. Policy ordering and implementation remain internal unless
a concrete client need requires more control. [AUTOEXEC.md](AUTOEXEC.md) owns the policy and shared
autoexecution-loop contract.

One factory constructs the complete immutable Actor-to-Agent map for an engine game so all Agents
share the same autoexecution loop. Applications retain that map or the particular Agents they need;
the factory does not introduce another public game wrapper.

## Layer responsibility

Agent depends on engine; engine does not depend on Agent. Every ordinary explicit and autonomous
action for one Actor enters through the same Agent methods and therefore uses the same validation
path.

The engine is indifferent to why an Actor chose one legal action. A policy that always chooses one
die face, a bot that plays badly, and a human strategy are equal from the engine's perspective.
Named policy quality or fairness guarantees belong to policy implementations and their tests.

There are no known external clients requiring obsolete aliases. Rename or remove public APIs when
the model improves instead of keeping compatibility wrappers. Script syntax is a separate
user-visible contract: call out any needed change before adopting it.

Direct engine mutation remains deliberately available to callers that choose the lower-level
module. This is architectural guidance, not an attempt to prevent trusted clients from cheating.
Ex-machina task addition/removal and concrete state changes belong to that engine API. The current
`manual`, resumable-operation, turn, and completion conveniences may remain as engine test helpers
while tests are migrated; they do not define the player-facing Agent contract.

Recording navigation belongs to an independent Game World view and does not belong on the Agent
command surface. See [GAMEWORLD.md](GAMEWORLD.md).

## Admin

A configured N-Player game has N seated Player Actors plus one Admin Actor. `Admin` is a real Pets
Component extending `Actor`, not another name for the engine mechanism. The application creates an
Agent for every Actor and normally gives Admin enough policies to be fully autonomous.

Admin can receive abstract tasks and make choices. Card dealing, dice, neutral setup, and similar
rules may assign or delegate narrowing to Admin. Whether Admin policies follow a seeded dealer,
choose adversarially, or use another legal strategy is not an engine concern.

## Remaining question

- Should provably permanent forced narrowing happen during engine task admission, or should an
  Agent policy record it as an Actor mutation? Decide from whether the simplification represents a
  game action or merely removes a specification that never denoted more than one possibility.

## Current implementation divergence

Today `Agent`, parsing, direct mutation powers, `autoExecMode`, and atomic completion all live in
`:engine`. `World.agent(actor)` returns one stable fully permissive object per Actor, and the Actor
is still named `Engine`. Public task mutation has been reduced to checked narrowing and explicit
single-task removal. The extraction should preserve behavior while successively:

1. reduce core entry to the audited direct mutation families;
2. create `:agent` above `:engine`, with one stable Agent per Actor and an Actor-scoped reader;
3. replace public many-queue language with one Game World task pool plus Agent-filtered views;
4. move parsing, policy ownership, and the shared autoexecution loop into `:agent`; and
5. migrate normal clients to Agent while keeping direct engine cheats and test helpers explicit.

Do not retain obsolete aliases merely to preserve the current public API. User-visible script
syntax must be migrated deliberately.
