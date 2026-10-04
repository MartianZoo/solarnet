# Agent API

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing the core mutation surface, `Agent`, `World.actorEngine`, task-command
> authority, script access modes, or client-visible state.
>
> **Status:** selected layering direction with the Agent/engine dependency reversal implemented.
> The scoped-reader and policy-system portions remain forward-looking.

## Source map

- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt) is the policy-free,
  Actor-attributed core mutation API.
- [`Agent.kt`](../../src/common/dev/martianzoo/agent/Agent.kt) is the current fully permissive,
  Actor-scoped client API in `:agent`.
- [`AgentImpl.kt`](../../src/common/dev/martianzoo/agent/AgentImpl.kt) translates string input and
  coordinates operations over `ActorEngine`; [`AutoExecLoop.kt`](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt)
  owns the preserved legacy queue drain.
- [`World.kt`](../../src/common/dev/martianzoo/engine/World.kt) returns stable ActorEngines;
  [`Agents.kt`](../../src/common/dev/martianzoo/agent/Agents.kt) pairs one World with its Agents.
- [`TaskStore.kt`](../../src/common/dev/martianzoo/state/TaskStore.kt) stores one global task set;
  [`TaskQueue.kt`](../../src/common/dev/martianzoo/state/TaskQueue.kt) is its filtered read view, and
  [`TaskQueues.kt`](../../src/common/dev/martianzoo/engine/TaskQueues.kt) constructs normalized task
  events.
- [`Access.kt`](../../src/common/dev/martianzoo/tfm/script/Access.kt) implements current script-only
  access modes.
- [GAMEWORLD.md](GAMEWORLD.md) owns task data and recording navigation;
  [RESPONSIBILITIES.md](RESPONSIBILITIES.md#selected-runtime-dependency-direction) owns the target
  dependency direction; [AUTOEXEC.md](AUTOEXEC.md) owns Agent policies and stable points.
- [CARD_HANDLING.md](CARD_HANDLING.md#selected-game-playing-direction) owns the selected Terraforming
  Mars card-tracking API direction outside the engine; that API is not implemented by the generic
  Agent contract described here.

## Core mutation surface

The core engine has no `Agent`, permissions policy, or autoexecution concept. It offers a few
distinct mutation methods, validates each call against one live World, and returns its atomic
result. A task retains one assignee in the global unordered task queue. Ordinary task calls name the
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
narrowing, and error handling. `runOperation`, turn, and phase conveniences compose ex-machina task
addition with ordinary task action. None justifies a universal request type or
`engine.submit(actor, request)`.

`doTask` and `tryTask` normally match the submitted narrowing semantically. `doTask` can additionally
require the exact Class name of the effect-context component that caused the task, which keeps
source-facing calls readable when otherwise matching consequences have distinct origins. Their
`TaskId` overloads remain the explicit identity escape hatch; no engine API accepts a presentation
index.

The current flat Agent exposes narrowing only for its selected task and one explicit ex-machina task
removal. It has no arbitrary task replacement or bulk task-removal command. Internal task-data edits
remain engine bookkeeping, including restoration around an evidenced replay correction.

## Committed narrowing and Agent drafts

Narrowing is allowed to discard options. That is a legitimate Actor decision, not a defect. A
candidate is valid only when the engine proves it narrows the stored task and cannot introduce an
option the task did not already permit.

**Current behavior:** Selection establishes the promise to act next and the select-lock before
narrowing resolves live World facts. `Agent.narrowTask(narrowing)` applies only to the selected task;
callers that need to identify a task first use `selectTask(taskId)`. Each accepted partial narrowing
edits the engine's task. A partial result remains selected, while a concrete result executes before
the call returns. `Agent.taskDraft(taskId)` creates an independent, caller-held `TaskDraft`.
`draft.narrow(narrowing)` keeps a provisional instruction in that object; `draft.instruction`
revalidates it against the current task and World; `draft.commit()` submits it to the engine,
selecting the task if necessary. Dropping the object forgets the draft. Drafting itself makes no
game event. There is no choice enumeration yet.

**Selected direction:** A caller may keep a disposable draft narrowing for one of its Actor's tasks
without submitting it. This works for selected and unselected tasks. Future read-only choice
analysis can use that draft and the current World to offer another narrowing, while the engine
still stores the last committed task instruction. A draft is the client's chosen restriction, not a
resolved instruction: state-dependent results such as `3 Plant.` becoming `2 Plant!` are derived
for display or execution and need not narrow the original instruction.

The client calls `draft.commit()` when the next game action needs the narrowed task, such as a real
split or execution. The engine validates the submission against its current task, then records any
task edits and consequences. It remains free to accept earlier partial submissions or perform sound
simplifications itself, but progressive Agent assistance must not depend on the engine recording
every intermediate step.

An unselected draft does not select or lock its task. Other actions may change the World, the task,
or its assignee; the draft rechecks against them before reading its instruction or submitting it.
If the task is reassigned to another Actor, the former assignee's draft has no game effect and can
be discarded.

A selected task's World lock makes its draft more stable, but the engine still validates submission.
Drafts may be discarded when stale, on rollback, or when the caller drops them. They are absent from
the shared task queue, event log, and recordings. This keeps tentative choices outside the public
game record; actual opponent privacy also requires the application to restrict access to each Agent.

## Agent

A configured Game World has exactly one Agent per Actor, including Admin. Every ordinary mutation
chosen autonomously or explicitly requested by an interactive client enters through that Actor's
Agent. Replay correction, tests, and workflows may deliberately use the lower-level engine API.
The Agent serializes its requests and calls the engine's Actor-attributed methods directly. It can
create caller-held drafts without retaining them or making them game state or an autoexecution
policy. A separate passive access object is not planned.

`Agent.reader` is a `ScopedGameReader`. In Player scope, contextual input such as `Plant` is
interpreted as `Plant<that Player>`, matching the current contextual `Owner` substitution.
`agent.reader.unscoped` returns the underlying `GameReader` so callers can deliberately inspect the
whole game without leaving the Agent API. This scoping supplies contextual input; it does not
require hidden-information views or player-specific universes.

An Agent owns the policies that may autonomously choose further actions for its Actor. This makes
human and artificial players one model: a human-directed Agent may have no active policies, while
installing enough policies can make the same Agent fully autonomous. Public `autoExecNow` and
policy addition/removal belong on Agent. Policy ordering and implementation remain internal unless
a concrete client need requires more control. [AUTOEXEC.md](AUTOEXEC.md) owns the policy and shared
autoexecution-loop contract.

`Agents(world)` constructs one Agent per Actor for an engine game, all sharing the same
autoexecution loop, and holds them alongside the World they act on. Applications retain that one
object and pass it wherever both a World and its Agents are needed; it carries no gameplay of its
own, so it is a pairing rather than another public game wrapper.

## Layer responsibility

Agent depends on engine; engine does not depend on Agent. Every explicit and autonomous
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
`runOperation`, resumable-operation, turn, and completion conveniences may remain as engine test helpers
while tests are migrated; they do not define the player-facing Agent contract.

Recording navigation belongs to an independent Game World view and does not belong on the Agent
command surface. See [GAMEWORLD.md](GAMEWORLD.md).

## Admin

A configured N-Player game has N seated Player Actors plus one Admin Actor. `Admin` is a real Pets
Component extending `Actor`, not another name for the engine mechanism. The application creates an
Agent for every Actor and normally gives Admin enough policies to be fully autonomous.

Admin can receive abstract tasks and make choices. Dice, neutral setup, and similar rules may assign
or delegate narrowing to Admin. Which legal strategy an Admin policy uses is not an engine concern.

## Remaining question

- Whether the engine performs a provably forced narrowing during task admission remains open. It
  is not required for progressive Agent drafts. Agent assistance with an unambiguous draft step
  does not itself commit an Actor mutation or need an autoexecution policy.

## Current implementation divergence

`:agent` now depends on `:engine`; engine source has no Agent or autoexecution dependency.
`World.actorEngine(actor)` returns one stable policy-free engine per Actor, and applications retain
one `Agents(world)`. That type is the unit every client passes: it holds the World and one stable
Agent per Actor, so no API takes a World and its Agents as separate arguments that could disagree.
Parsing, operation conveniences, policy state, and the shared legacy drain live in `:agent`.

`TfmTest` fixtures hold the `Agents` for their current World. Standalone engine integration tests
still use `testAgents.kt`'s one-World cache so a World's Agent identity survives repeated lookups.
Those tests also depend upward on `:agent` and `:tfm-engine`, because they exercise the Agent API
rather than the engine independently. These are accepted costs, not the target state. Gradually
replace Canon-backed premises with focused declarations where that makes the generic engine
contract clearer; the cleanup is desirable but not urgent.

The current Agent is still fully permissive and exposes an unscoped `GameReader`, operation and
turn conveniences, and ex-machina mutation. `AutoExecPolicy` is still the legacy three-value
setting rather than the planned attachable policy system. Remaining extraction work should:

1. add the selected Actor-scoped reader without duplicating World state;
2. replace public many-queue language with one Game World task queue plus Agent-filtered views;
3. reduce the normal Agent surface while keeping direct engine cheats explicit; and
4. replace the legacy global queue drain with the policy-relative shared loop in [AUTOEXEC.md](AUTOEXEC.md).

Choice enumeration and read-only resolution of drafts still need implementation without copying
engine legality rules.

Do not retain obsolete aliases simply to preserve the current public API. User-visible script
syntax must be migrated deliberately.
