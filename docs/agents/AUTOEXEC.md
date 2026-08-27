# Autoexecution policies

> **Read when:** moving autoexecution out of the engine, changing `AutoExecMode`, proving a task safe
> to execute automatically, or designing an optional client policy.
>
> **Skip when:** changing authored `::` automatic effects or explicit task execution; those
> are engine semantics covered by [ENGINE.md](ENGINE.md) and [SEQUENCING.md](SEQUENCING.md).
>
> **Status:** settled design direction plus current-implementation audit. The policy model is not
> committed behavior.

## Source map

- [`AutoExecMode.kt`](../../engine/src/commonMain/kotlin/dev/martianzoo/engine/AutoExecMode.kt) —
  inspect the current mode vocabulary before proposing policy objects.
- [`Implementations.kt`](../../engine/src/commonMain/kotlin/dev/martianzoo/engine/Implementations.kt)
  — search for `autoExec` to find the current engine coupling and queue drain.
- [`TfmWorkflow.kt`](../../tfm-engine/src/commonMain/kotlin/dev/martianzoo/tfm/engine/TfmWorkflow.kt)
  — search for `game.isIdle()` when changing the current workflow's queue-drained wakeup.

## Read only the relevant sections

| Task | Read |
| --- | --- |
| Distinguish engine semantics from client policy | Core distinction; Preparation is not a policy |
| Design a policy interface or driver | Policy pool and driver through Agent provenance |
| Add analysis, speculative Worlds, or performance guarantees | Analysis and disposable Worlds; Performance contract |
| Plan or review the extraction from engine | Broad implementation direction; First implemented split; Current implementation divergence |
| State the acceptance contract | Required invariants |

## Core distinction

The engine must be fully usable with no autoexecution enabled. A client can play a complete game by
issuing explicit gameplay commands. Disabling every policy must perform no autoexecution analysis,
queue scan, speculative preparation, or other hidden work.

Autoexecution is a pool of optional client policies plugged into the same `Gameplay` API. A
policy may select, prepare, narrow, or execute tasks on an Actor's behalf. It has no mutation power
that an explicit client call lacks, and the resulting gameplay operation must be identical to the
same call made directly by that client.

This distinction excludes several engine responsibilities:

- A `::` effect executes inline because the authored game rule makes it automatic.
- A `:` effect creates a task because the authored rule requires later activity by its assignee.
- Validation and recognition of a proven dead end apply whether a client or policy chose the path.
- Preparing a particular task performs the state-based evaluation required by preparation.

The engine owns those semantics. It does not own a policy for deciding which offered gameplay
command should happen.

## Preparation is not a policy

The engine prepares only the task a caller identifies. Preparation evaluates facts whose meaning is
already fixed by the current World, especially gates and metrics, and performs the validation and
normalization needed to represent that task. This evaluation is conceptually related to narrowing,
but it is not a search through the player's choices and it does not initiate background work over
the task pool.

Forced narrowing is distinct. It examines a choice and proves that some candidates are impossible
or that only one answer remains. The engine may expose reusable read-only analysis that supports
such a proof, but preparation is not contractually required to discover every forced
narrowing. An optional policy may request the analysis and submit the resulting revision through
the same gameplay command a client would use.

This distinction keeps raw preparation small and predictable. Turning every policy off removes
policy-driven task-pool searches and optional choice-pruning work while leaving gates, metrics,
validation, local preparation normalization, and explicit gameplay functional.

Selection, preparation, narrowing, and execution remain separate possible commands. A policy may
automate any subset for which it has an adequate reason; becoming prepared or concrete does not by
itself execute a task.

## Policy pool and driver

Each policy should be small, named, independently selectable, and explicit about the reasoning it
owns. A configured profile is a pool of those policies, not one aggressiveness enum. Many narrowly
capable agents can be installed, selected, credited, and scheduled without merging their reasoning
into one omnibus policy.

An application-level driver repeatedly:

1. reads current gameplay and task state;
2. asks the selected policies for a gameplay command;
3. performs at most one proposed command through the relevant assignee's `Gameplay` API; and
4. discards every prior conclusion and reads the resulting state again.

The ordering of policies is a client configuration concern. For proof-preserving policies, it
chooses which valid proof receives credit, not game precedence. A disabled policy retains no right
to scan or react, and an empty profile makes the driver inert.

The driver may be a decorator, subscriber, command-loop collaborator, or another application
mechanism. Its concrete shape must preserve the essential dependency direction:

```text
autoexecution policies -> Gameplay API -> engine semantics
```

It must not become a callback from engine internals into policy code, a privileged task mutation
path, or a requirement for workflows to function.

## Policies may make decisions, but ours currently must not

The architecture permits policies that make genuine gameplay decisions. A user could deliberately
install enough decision-making agents to play an entire game from start to finish. Such agents
still use the same API, Actor authority, transactions, and event attribution as a person driving a
client.

That architectural freedom is separate from the policies Solarnet should provide in the near term.
For now, every supplied policy must prove that its action makes no gameplay sacrifice. In
particular, there is no `FIRST` policy, no stable-order fallback, and no convenience mode allowed to
pick an arbitrary viable task. Tests depending on such choices must become explicit instead of
preserving an unsafe policy for their convenience.

A proof-preserving policy may act only when it establishes that its command removes no
semantically distinct legal continuation. Useful proof families include:

- the client has already selected and fully narrowed a concrete task, leaving execution as the
  uncommitted mechanical step;
- exactly one revision of a selected task is valid;
- exactly one task can make any gameplay progress in the current state; or
- several candidate paths reach the same semantic state at the same comparison point.

The third case is stronger than observing that one call to `prepareTask` succeeds. Another task may
still accept an explicit narrowing or another command. A policy must account for every
way a client can make gameplay progress. When one task is gated on `Foo`, the World has no `Foo`,
and the only other task gains `Foo`, the gain really is forced: the gated task cannot progress
until that state change occurs. This policy family is plausible, but it may be deferred until its
proof criterion and its value to whole-game clients are both clear.

Speculative analysis may return `PROVEN`, `DISPROVEN`, or `UNKNOWN`; uncertainty always stops a
proof-preserving policy. A successful simulation is not by itself proof that no other legal path
exists.

## Semantic equivalence

For this purpose the gameplay state is the `ComponentGraph` plus the gameplay-relevant contents of
`TaskQueues`. Two paths may be equivalent even when they produce different event records, policy
credits, task ids, or other diagnostic metadata, provided those differences cannot affect later
gameplay and the component/task states are otherwise the same at the same comparison point.

This relies on a firm architectural rule: game mechanics must not read event history. Custom Class
implementations currently receive only `GameReader`, which exposes no history. Preserve and enforce
that restriction so a future custom API cannot silently turn diagnostic history into gameplay state.
Event history remains essential for explanation, provenance, rollback, replay, and presentation;
it is simply not an input to game rules.

Equivalence of component/task state is deliberately stronger than equality of headline resources
or final scores. Temporary components, prepared-task state, assignee, Actor, continuations, and any
other task data that can affect future play all count.

## Agent provenance

Every explicit gameplay command must carry diagnostic agent identity. Actor answers who acts in the
game; agent answers which client-side decision source issued the command. A direct UI, script, test
driver, named autoexecution policy, and future game-playing agent may therefore all act as the same
Player without becoming indistinguishable in history.

The command's agent identity naturally propagates through its inline `::` consequences because
they are part of that command's causal execution. When a `:` effect queues work, the task-creation
event retains the originating command's provenance; a later command that selects, narrows, or
executes that task records its own agent. Agent identity is diagnostic and does not participate in
semantic-state equivalence.

The application may group one client command with the automatic commands it provokes for display
or undo. That presentation grouping must not pretend the later policy actions were hidden engine
work or erase the identity of the policy that supplied each command.

## Analysis and disposable Worlds

Some proof policies need more than local structural inspection. Read-only analysis may use a
disposable Game World overlay: share immutable declarations, overlay component and live-effect
state, copy the small task queues, and extend event history only for diagnosis. Every branch is
discarded after analysis; the chosen command is then applied to the live World.

Overlays do not authorize arbitrary selection. They are useful only when the policy checks the
complete relevant candidate set and proves forcedness or semantic equivalence. The backing World
revision must remain explicit so analysis is never applied to a changed state.

## Performance contract

The implementation historically paid for autoexecution after every nested engine/API transition. A
JFR trace of `ThermalMatterWaveTest` after immediate execution stopped using reversible execution
preview recorded 3,158 `autoExecNext` calls, 5,413 candidate-preparation probes, and 1,272 atomic API
entries. There were more than five autoexecution passes and nine preparation probes per explicit
task selection on average.

The destination removes that structural cost:

- raw `Gameplay` commands never start an autoexecution drain;
- no selected policies means no autoexecution work;
- one application driver owns policy advancement;
- each accepted proposal performs one command and invalidates earlier analysis;
- policies may subscribe to relevant task changes instead of rescanning the World; and
- caching or overlays are added only for a demonstrated proof policy and keyed by gameplay state,
  not diagnostic history.

Performance is a first-class reason for the overhaul, but it reinforces rather than defines the
division. The engine should not retain policy ownership simply because an internal loop appears
easier to optimize.

## Broad implementation direction

The current policy loop should move out of `Implementations` and `ApiTranslation`; `AutoExecMode`
and `FIRST` should disappear from `Gameplay`. The raw API should retain task commands and
gain only the read-only analysis needed by real policies. An optional application driver should own
the selected policy profile, advancement points, and diagnostic agent identity.

The first useful profile should be deliberately small: execute a concrete task whose choice is
already committed, and apply narrowly defined forced revisions. A sole-progress policy can be added
if its proof is sound and removing the current behavior causes enough legitimate client burden to
justify it. Richer equivalence and exhaustive-search policies should follow only concrete needs.

This is a broad destination, not a required migration order. Intermediate work must keep current
behavior and proposed behavior clearly labeled and must not make arbitrary autoexecution choices
look safe just to keep tests concise.

## First implemented split

Nested facade re-entry no longer starts an implicit drain. `AtomicOperationScope` now invokes
the configured autoexecution only before the outermost command completes. Explicit `OperationBody`
task commands still advance between body statements, and the operation lifecycle still has its own
pre-body and post-body drains because current completion validation depends on them.

This is not the client-policy split yet: modes and the policy loop remain in the engine. It does
establish one outer command interface, removes incidental cross-Actor re-entry as a scheduling point,
and makes the remaining operation coupling explicit enough to extract deliberately.

An attempted direct removal of `FIRST` demonstrated why that extraction must come first. With the
default changed to `SAFE`, 21 of 54 script tests failed and failures immediately spread across the
engine's card, workflow, and full-game scenarios. Independent consequences commonly coexist in the
task pool, so `SAFE` leaves operations unfinished even where ordering is semantically immaterial.
Encoding those decomposition details as explicit choices throughout clients would be the wrong
migration. A proof of semantic equivalence, or a more direct representation of independent
automatic consequences, is required before arbitrary ordering can disappear without transferring
engine internals into client scripts.

## Current implementation divergence

Committed code still stores `AutoExecMode` on `Gameplay`, defaults it to `FIRST`, invokes draining
from the outer engine-side API layer and operation lifecycle, scans pending tasks globally, and
uses stable iteration order to choose among multiple candidates. Candidate discovery catches every
`Exception`, conflating routine ineligibility with defects, and operation-level drains can still
repeat analysis.

Those facts describe debt, not compatibility requirements. The project has no known client whose
dependence on `FIRST` outweighs the core model above.

## Required invariants

The finished design should establish that:

- a complete explicit client works with an empty policy profile;
- an empty profile performs no scans, probes, or automatic task commands;
- direct and policy-issued copies of the same gameplay command differ only in diagnostic agent
  provenance, producing identical semantic state and otherwise identical events;
- automatic rules and state-based preparation behave identically with every policy disabled;
- each supplied proof-preserving policy is tested against the legal alternatives it claims to
  preserve;
- no supplied policy uses stable task order as a gameplay decision;
- Actor, assignee, cause, and agent provenance remain distinct and correct;
- game mechanics cannot read event history; and
- policy advancement is measured once per application command rather than hidden inside nested
  engine calls.
