# Agent and Actor-access API

> **Read when:** changing the core mutation surface, `ActorAccess`, `Agent`, `World.agent`,
> task-command authority, script access modes, or client-visible state.
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
- [RESPONSIBILITIES.md](RESPONSIBILITIES.md#selected-runtime-dependency-direction) owns the target
  dependency direction; [AUTOEXEC.md](AUTOEXEC.md) owns Agent Drivers and stable points.

## Core mutation surface

The core engine has no `ActorAccess`, `Agent`, permissions policy, or autoexecution concept. It
offers a few distinct mutation methods, validates each call against one live World, and returns its
atomic result. A task retains one assignee in the global unordered task pool. Ordinary task calls
name the acting Actor, and the engine rejects action by anyone other than the task's current
assignee. That is game semantics, not caller permission.

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
normalization belongs to engine task admission or an Agent Driver remains open; both must use the
same checked narrowing relation.

## ActorAccess

`ActorAccess` is the working name for the passive, permissions-aware conduit between one Agent and
the engine. It binds direct engine calls to one Actor and may provide convenient filtered reads over
the one global task pool. It owns no queue, timeline, transaction, policy, or autonomous behavior.
Any number may exist for one Actor and World without creating competing decision-makers.

Granular permissions are explicitly postponed. The first `ActorAccess` is maximally permissive: it
may inspect the whole World and expose every mutation family, including ex-machina operations. The
engine still enforces game semantics such as assignment and legal narrowing. Later restrictions,
hidden-information projections, maintenance roles, and session authentication require evidence
from an actual higher-layer need.

## Agent

A configured Game World has exactly one Agent per Actor, including Admin. Every mutation attributed
to that Actor passes through the Agent, whether chosen autonomously or explicitly requested by an
interactive client, script, replay, test, or workflow. The Agent serializes those requests and uses
`ActorAccess`; callers never bypass it to mutate the engine.

An Agent also owns the Driver that may autonomously choose further actions for its Actor. This
makes human and artificial players one model: a human-directed Agent has uncovered choices, while
installing enough policies can make the same Agent fully autonomous. The Agent need not reveal its
policy collection, precedence, or internal decision process. [AUTOEXEC.md](AUTOEXEC.md) owns the
Driver and pulse contract.

## Layer responsibility

Actor access depends on engine; engine does not depend on access. Agent depends on Actor access and
receives no raw engine mutation object. Therefore every explicit and autonomous action uses the
same authority and validation path.

The engine is indifferent to why an Actor chose one legal action. A policy that always chooses one
die face, a bot that plays badly, and a human strategy are equal from the engine's perspective.
Named policy quality or fairness guarantees belong to policy implementations and their tests.

There are no known external clients requiring obsolete aliases. Rename or remove public APIs when
the model improves instead of keeping compatibility wrappers. Script syntax is a separate
user-visible contract: call out any needed change before adopting it.

Recording navigation is read-only and does not belong on the trusted Agent command surface.

## Admin

A configured N-Player game has N seated Player Actors plus one Admin Actor. `Admin` is a real Pets
Component extending `Actor`, not another name for the engine mechanism. The application creates an
Agent for every Actor and normally makes Admin fully autonomous.

Admin can receive abstract tasks and make choices. Card dealing, dice, neutral setup, and similar
rules may assign or delegate narrowing to Admin. Whether an Admin Driver follows a seeded dealer,
chooses adversarially, or uses another legal strategy is not an engine or access-layer concern.

## Remaining question

- Should provably permanent forced narrowing happen during engine task admission, or should an
  Agent Driver record it as an Actor mutation? Decide from whether the simplification represents a
  game action or merely removes a specification that never denoted more than one possibility.

## Current implementation divergence

Today `Agent`, parsing, direct mutation powers, `autoExecMode`, and atomic completion all live in
`:engine`. `World.agent(actor)` returns one stable fully permissive object per Actor, and the Actor
is still named `Engine`. Public task mutation has been reduced to checked narrowing and explicit
single-task removal. The extraction should preserve behavior while successively:

1. reduce core entry to the audited direct mutation families;
2. move passive Actor binding above engine as `ActorAccess`;
3. replace public many-queue language with one task pool plus filtered views;
4. make Agent the sole mutation issuer and unique Driver host for one Actor; and
5. move Driver policy and generic pulse dispatch above Actor access.

Do not retain obsolete aliases merely to preserve the current public API. User-visible script
syntax must be migrated deliberately.
