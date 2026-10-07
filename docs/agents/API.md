# Agent API

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing `Agent`, `World.actorEngine`, task-command authority, task forms,
> script access modes, or client-visible state.
>
> **Status:** current Agent/engine contract, with unresolved API and policy directions listed
> separately. Persistent delegated operation control is not implemented.

## Source map

- [`Agent.kt`](../../src/common/dev/martianzoo/agent/Agent.kt) declares the Actor-scoped client API.
- [`AgentImpl.kt`](../../src/common/dev/martianzoo/agent/AgentImpl.kt) owns contextual parsing and
  operation coordination; search `atomic`, `commitForm`, and `autoExecPolicy`.
- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt) owns policy-free task
  validation; search `enforceSelectLock`, `replace1WithN`, and `executeSelectedTask`.
- [`TaskForm.kt`](../../src/common/dev/martianzoo/agent/TaskForm.kt) owns provisional choices;
  search `decisions`, `options`, `choose`, and `commit`.
- [`Agents.kt`](../../src/common/dev/martianzoo/agent/Agents.kt) pairs a World with its stable Agents.
- [`Access.kt`](../../src/common/dev/martianzoo/tfm/script/Access.kt) supplies script access modes.
  [COLOR_MODES.md](COLOR_MODES.md) owns their intended contracts; current access checks do not
  implement those contracts throughout.
- [GAMEWORLD.md](GAMEWORLD.md) owns passive task data and recording navigation;
  [AUTOEXEC.md](AUTOEXEC.md) owns policy; [CARD_HANDLING.md](CARD_HANDLING.md) owns the separate
  Terraforming Mars card-tracking direction.

## Core mutation surface

`ActorEngine` has no Agent or autoexecution dependency. Ordinary task commands validate the
acting Actor against the task's current assignee and enforce the global selection lock.
Assignment and eligibility are game semantics, independent of whether a human or policy chose
that command. The global task pool has Actor-filtered views; display order is not task identity.

`selectTask` resolves current work and executes it if concrete. Otherwise the selected task
remains for narrowing, possibly assigned to its contextual Actor. `narrowTask` validates that a
submission only removes choices; the Agent method requires an already selected task, while a form
can commit by task id. A selected group can become independent unselected siblings. `THEN`
continuations retain the original controller. Triggered work retains a Player controller;
Admin-controlled work uses the ownership fallback in [IDENTITY.md](IDENTITY.md).
The current lock ends when the selected task finishes, including structural replacement.

This is insufficient for an opponent-controlled payment process. A client must not manufacture
continuous control by chasing payment tasks across Agents. The unresolved engine choices are
recorded in [delegated operations and scheduling options](SEQUENCING.md#delegated-operations-and-scheduling-options).
Any eventual rule must govern selection, narrowing, form commits, and explicit and automatic
execution consistently.

`doTask` and `tryTask` compose identification, selection, and narrowing. Submitted constraints
intersect the pending instruction, so each can fill choices left open by the other. Every distinct
intersecting task participates in ambiguity detection; a strict match does not hide another
intersection. Fully identical tasks are interchangeable. The intersection must preserve fixed
structure and compatible counts and quantifiers. Omitted quantifiers can retain a stronger pending
quantifier; explicit incompatible quantifiers fail. `narrowTask` itself remains strict.

The `TaskId` overloads identify exact tasks. `doTask` also accepts the exact effect-context Class
for source-facing disambiguation. No command accepts a presentation index. `tryTask` leaves work
pending when incomplete or temporarily unavailable, but does not conceal invalid selection,
invalid narrowing, or dead ends.

The current Agent additionally exposes task addition/removal, concrete corrections, and operation
and turn conveniences. These are deliberate powers, separate from ordinary task authority.
Internal task edits remain engine bookkeeping; the public Agent has no arbitrary task replacement
or bulk-removal command. [EX_MACHINA.md](EX_MACHINA.md) owns correction behavior.

## Committed narrowing and Agent forms

An accepted partial narrowing edits the selected task in game state. A concrete result executes
before the command returns. Committing a restriction is a legitimate Actor decision even when it
removes otherwise legal options.

`fillInTask(taskId)` instead creates a caller-held `TaskForm` for a currently assigned task.
`form.narrow` and `form.choose` keep provisional choices locally. Reading `form.instruction`
rechecks the task and World; `form.commit()` submits through normal engine validation, selecting
if necessary. Discarding the form makes no event. Forms are absent from the task pool and recording,
and cannot retain authority after their task changes assignee.

`form.decisions()` identifies the next supported choice. Target choices proceed through the root
Class and then open dependencies, avoiding a Cartesian product of complete instructions. Shared
Type and amount variables preserve their relationships. Class literals enumerate represented
Classes without requiring component instances. Amount bounds follow target selection. Optional
changes may retain absent-dependency choices because a later zero choice can still be legal.

This is choice assistance, not proof that the entire operation can succeed. Broad partial choices
must not be rejected merely because they cannot yet resolve. Broad target options can include
self-transmutations that selection rejects. Unsupported abstract shapes fail
explicitly; current gaps include `EACH`, optional changes inside unresolved `PER`, and certain
state-dependent refinements. The implementation and focused Agent tests own the detailed supported
shapes, rather than a duplicated inventory here.

An unselected form holds no lock, so intervening work can make it stale. Selected forms still
revalidate before commitment. Tentative choices remain client state; privacy requires the
application to control access to its Agents.

## Agent

`Agents(world)` constructs a stable Agent per Actor, including Admin, sharing an autoexecution loop.
Applications keep that pairing. Ordinary explicit and autonomous task commands use the same
Actor-attributed engine validation; workflow, tests, and corrections may deliberately use the
lower-level engine API.

`Agent.reader` currently exposes the unscoped `GameReader`. Actor context comes from Agent parsing:
for a Player, bare owned input such as `Plant` receives that Player through lexical ownership
insertion. This does not establish hidden-information views or a separate player universe.

Each Agent currently has an `AutoExecPolicy` enum setting and `autoExecNow()`. Changing the setting
runs the shared loop. Editing a provisional form does not. Defaults and policy limitations belong
in [AUTOEXEC.md](AUTOEXEC.md).

## Layer responsibility

Agent depends on engine; engine does not depend on Agent. The engine decides whether a command is
legal without judging strategy. Policies choose among legal commands and own any stronger promise
about preserving choices.

Recording navigation belongs to the passive Game World view, not the Agent command surface.
There are no known external clients requiring obsolete aliases. Remove obsolete APIs when the
model improves; treat user-visible script syntax as a separate contract requiring deliberate change.

## Admin

Admin is a real Pets Actor and Component, distinct from the engine. Its Agent may receive abstract
work and choose among legal outcomes. Autonomy comes from policy configuration, not from an
assumption that neutral work is choice-free. Assignment to Admin does not itself grant precedence
over Player work.

## Current implementation divergence

The Agent/engine dependency direction and shared Agent loop are implemented. An Actor-scoped
reader, a narrower ordinary client surface, and configurable policy attachment remain proposed.
There is no implemented `ScopedGameReader` or policy-addition/removal API. These possible API
changes do not establish task priority, payment control, or operation completion.

Whether admission should perform additional provably forced narrowing remains open. Existing
caller-held forms do not require that decision, and changing it must preserve the distinction
between draft assistance and a committed Actor choice.
