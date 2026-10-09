# Agent autoexecution

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing Agent policies, `autoExecNow`, synchronous settlement, or proof-oriented
> task analysis.
>
> **Status:** enum-based Agent policies and a shared synchronous loop are implemented.
> Configurable policies and stronger proof guarantees remain proposals.

## Choice-safety check

- `NONE` makes no automatic choices for that Actor.
- `EAGER` chooses task order and can change the outcome. Do not enable it merely to make a card
  scenario or faithful replay proceed.
- `CONCRETE` uses current selectable-task ambiguity; it is not a general proof that all legitimate
  continuations survive. Inspect the actual decision before relying on it.
- Authored `::` effects, assignment, and scheduling legality belong to engine semantics.
  Autoexecution cannot supply missing control, immediacy, or completion rules.

A delegated payment must keep the waiting player blocked even when every Agent uses `NONE`.
The current selected-task lock provides that protection only until the selected task completes.
An eager drain may hide the later gap by immediately handling descendants; it cannot repair it.
[SEQUENCING.md](SEQUENCING.md#delegated-operations-and-scheduling-options) owns the unresolved options.

## Source map

- [`AutoExecPolicy.kt`](../../src/common/dev/martianzoo/agent/AutoExecPolicy.kt) declares `NONE`,
  `CONCRETE`, and `EAGER`.
- [`AgentImpl.kt`](../../src/common/dev/martianzoo/agent/AgentImpl.kt) owns the policy setting and
  completion calls; search `autoExecPolicy`, `autoExecNow`, and `atomic`.
- [`AutoExecLoop.kt`](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt) owns the shared loop;
  search `actOnce`, `candidateCounts`, and `activeCandidates`.
- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt) owns policy-free command
  validation and probes; search `enforceSelectLock` and `canSelectTask`.
- [API.md](API.md) owns the Actor-scoped Agent surface.
- [SMART_AUTOEXEC.md](SMART_AUTOEXEC.md) records optional proof research.

## Current implementation

`Agents(world)` creates a stable Agent per Actor, all registered with the same private loop.
Every Agent currently defaults to `EAGER`. Its setting belongs to Agent; the core engine has no
policy dependency. Changing the setting invokes the loop, as does `autoExecNow()` and ordinary
Agent transaction settlement. `autoExecNow(policy)` instead uses that policy for the invoking Agent
only during the synchronous run and leaves its configured setting unchanged. Other Agents continue
using their configured policies. Direct correction operations have their own contracts.

The loop gives an existing selected task exclusive attention. Otherwise it considers pending tasks
in stable queue order, probing availability when several exist. A sole pending task skips that
probe; actual selection still validates it inside the transaction. Candidate counts are computed
per assignee. `CONCRETE` accepts a candidate only when its assignee has no competing selectable
task; `EAGER` can choose among candidates. The candidate's current assignee determines which
Agent's policy and engine apply. Another Agent cannot automatically select work assigned to an
Actor using `NONE`.

After a successful execution the loop starts over against the changed World. Selecting an abstract
task may instead leave a choice for its assignee, including a cross-Player handoff. Current policies
do not constitute a general strategy for filling arbitrary abstract choices. The loop ends when
its policies cannot advance available work, or throws when it classifies the state as a dead end.

Queue order can affect an `EAGER` strategy; it cannot establish a game rule. New eligibility rules
must constrain explicit commands and policy commands alike. Neither Agent visitation order nor
an Admin-first preference substitutes for engine scheduling.

## Policy-relative stable point

The useful contract is a **policy-relative stable point**: the installed policies cannot advance
any currently legal work they cover. This does not imply an empty task pool. A human choice can
remain pending, and work assigned to another Actor can remain temporarily ineligible.

The current loop approximates this contract through its candidate checks. Its dead-end fallback
can attempt an active Agent's unavailable task when an Actor using `NONE` has selectable work that
could enable it. This limitation is visible in `AutoExecLoop.actOnce`; do not present the loop as
an exhaustive legality or completion proof.

Assignment to Admin supplies neither priority nor proof that its choices are harmless. An
application may configure Admin autonomy, but deferred Admin work can legitimately wait for a
Player choice. Whether a future scheduler admits that work later or retains it as ineligible is
part of scheduling design, not a reason to require every returned state to have an empty Admin
queue.

## Proposed policy directions

Attachable policies could replace the enum and progressively automate an ordinary Agent.
Their ordering, mutation interface, and public configuration are unresolved. No separate AI-player
kind is needed merely to automate the same legal commands a human can request.

A proof-oriented policy could select or narrow work only when it establishes the preservation
contract named by that policy. A choice-making policy may deliberately choose strategy instead.
Neither should be presented as the other. A future exhaustive `slow` policy is research in
[SMART_AUTOEXEC.md](SMART_AUTOEXEC.md), not a required scheduler or an implemented capability.

Read-only task-form assistance is independent of policy: refining an unsubmitted form makes no
mutation. Committing it uses ordinary Actor authority and can trigger settlement afterward.

## Replay identity preservation

**Proposal:** a Terraforming Mars replay policy could preserve source-known card identities by
leaving anonymous `ProjectCard<Hand>` removals for explicit named replay actions. A named card play
already preserves its identity through transmutation. The selected external card-tracking direction
is owned by [CARD_HANDLING.md](CARD_HANDLING.md#selected-game-playing-direction).

This is a reason to investigate game-specific policies, not to add card names to generic
`AutoExecLoop` or use policy as gameplay ordering. The policy would need to decline before acquiring
the selection lock and inspect typed instructions. Exact replay annotations still belong with the
sourced action and recording, rather than retrospective matching across unrelated work.

## Required properties

- Policy-free engine validation governs explicit and automatic commands alike.
- Assignment and scheduling eligibility remain distinct from the Agent's willingness to act.
- Every accepted mutation invalidates policy analysis based on the prior World.
- An Actor using `NONE` retains the same explicit command semantics and scheduling protections.
- A paused policy loop does not imply operation completion or whole-World idle cleanup.
- No policy claims a preservation proof merely because it found an executable task.
