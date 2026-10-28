# Agent contract

> **Agent information:** This document records selected responsibilities and unresolved design
> questions. It is not a claim that the corresponding implementation is complete.
>
> **Read when:** designing client assistance, delegated choices, interaction services, or autoexec
> policies installed in the engine by an agent.

[ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns engine request interpretation, transactions,
priorities, and mandatory Admin handling. This document owns the agent's client-facing
responsibilities and its configuration of standing engine policies. The detailed completion and
choice-analysis rules remain in [DECISION_GRAPH.md](DECISION_GRAPH.md).

## Agent responsibilities: four buckets

**Selected organization — October 28, 2026.** Separate helping the client choose, choosing for
it during an interaction, other interaction responsibilities, and configuring standing autoexec
policies in the engine. Assistance remains useful with Player automatic decisions disabled.
These are responsibilities, not a proposal for four classes, modules, or processing stages.
The inventory assumes the selected engine destination for design discussion; it does not claim
that the transition or the services below are implemented.

**Selected** identifies agreed responsibility or direction. **Recommendation** identifies an
additional proposed service. **Open** identifies details still requiring a decision.
**Deferred** identifies future work deliberately excluded from the present design scope.
[DECISION_GRAPH.md](DECISION_GRAPH.md) owns the detailed analysis and proof rules; this document
owns the overall inventory and the standing-policy direction.

### 1. Help the client choose

**Selected responsibility:** help the client understand and supply its own answer. Disabling
Player automatic decisions must not disable guidance, filtering, or analysis.

- **Identify pending decisions.** Explain which tasks need attention and which Actor must answer.
  Stan's greenery placement can succeed while Kim's Philares resource choice remains pending.
  Distinguish that outstanding response from a new independent action.
- **Guide construction of a request.** Break an abstract task into useful questions about an
  alternative, class, dependencies, amount, and card identities. Retain partial answers and allow
  revision without requiring live game changes. A client that already knows its answer can submit
  it directly; a form or an enumeration of options must never be mandatory.
- **Provide useful candidates.** Reduce unanswered questions and combine partial deductions while
  preserving every potentially completing choice. Choosing a target may establish a useful amount
  range. Represent large ranges without materializing every integer. A candidate menu need not
  certify that each option can complete, and an incomplete menu cannot prove its sole answer forced.
- **Analyze later consequences.** Investigate whether the action can finish after the requested
  transaction and its subsequent tasks. Project Eden must leave space for its required city.
  Preserve TRUE/FALSE/UNKNOWN; missing card information or unfinished analysis is not proof of
  impossibility. Use engine execution for game consequences rather than a second rule interpreter.
- **Establish which distinctions matter.** Prove when alternatives or task orders have equivalent
  outcomes. Explaining that equivalence helps the client; submitting a representative answer is
  a separate act in bucket 2.

**Recommendation:** offer previews and explanations based on that analysis: established
consequences and unresolved choices before submission, and retained changes, remaining choices,
and reasons for rejection or uncertainty afterward. Distinguish established results from outcomes
dependent on later answers.
The exact information and presentation contract remains **open**; no new explanation API is selected.

**Open:** complete candidate generation, work limits, action-completion recognition across Actors,
and the relationship between local drafts and voluntarily retained engine narrowing. The existing
ordered-decision and completion discussions remain in DECISION_GRAPH; this inventory does not
resolve them merely by assigning the work to Agent.

### 2. Choose for the client during the current interaction

**Selected responsibility:** under the client's applicable policy, the agent may supply and submit
an answer while handling the current situation. Establishing an answer and being authorized to act
on it are separate requirements.

- **Advance through forced decisions.** After sound pruning leaves no meaningful choice, an
  authorized agent can submit the remaining answer and continue to the next decision. Finding a
  successful continuation does not authorize executing all of its later choices.
- **Choose among proved equivalent alternatives.** The agent can select a representative or an
  interchangeable task order under the decision-graph rules. Executability of both tasks alone
  does not show that their order is harmless; their consequences must be considered.
- **Respect each Actor's decisions.** Analysis of Kim's Philares response does not let Stan's
  agent pick Kim's resource. A policy authorizing Kim's answer must come from her agent's configured
  authority. This is Actor attribution and client policy, not caller authentication.

**Open:** the controls and defaults for granting this authority, and migration from current Player
policies. No new default follows from this inventory. **Deferred:** broader strategic delegation
where an agent actively chooses between meaningfully different outcomes. It would belong here,
but is not required for assistance or automatic handling of proved unnecessary questions.

### 3. Carry out and maintain the interaction

**Selected responsibilities:** these services remain useful when the client supplies every
meaningful answer.

- **Translate convenient expressions of intent.** A submitted production total of 21 MC can
  execute the separate 6 MC production, 20 MC Terraform Rating income, and -5 MC offset tasks.
  Preserve each task's effects and continuation rather than replacing them with a net engine
  mutation. The client has supplied the intended total; Agent translates it into separate calls.
  Keep unrelated rewards, such as Investment Loan and Media Group, separate in scenario inputs.
- **Coordinate the surrounding call.** Submit the requested work, ensure mandatory Admin work
  is handled, and coordinate further execution allowed by Player policies. Account for work
  performed under installed engine policies too. Stop when further input is needed without
  presenting that stopping point as proof that the whole action has finished. Quiescence includes
  applicable installed engine policies as well as mandatory Admin and configured Player drivers.
- **Support grouping and recovery.** Organize several engine calls when the client requests a
  grouped submission, using the engine's restoration capabilities. Transaction restoration and
  restoring an entire interaction are different promises; preserve that distinction.
- **Manage external information.** Coordinate actual card identities when execution requires
  them. During speculation, allow anonymous cards where appropriate and stop with UNKNOWN when
  an unknown identity prevents analysis. Session-level input checks remain distinct from engine
  game-rule validation; this assigns no secret-custody or authentication responsibility.
- **Report and record the interaction.** Return the retained consequences across the whole call,
  including subsequent automatic work, and record decisions for reconstruction. The input record
  and the engine's detailed event history serve different purposes. Existing mandatory Admin
  execution is repeated automatically during replay; recording new installed-policy execution
  needs its own explicit decision below.

**Open:** recovery across callbacks and already-returned calls; recognition of action completion;
and the precise client contracts for previews and diagnostics. Arbitrary external application
side effects are not covered by engine rollback. Existing operation helpers are implementation
evidence, not a requirement to retain their lifecycle or completion assumptions.

### 4. Configure autoexec policies in the engine

**Selected direction:** an agent installs policies directly in the engine for selected tasks
assigned to its Actor. When such a task matches a policy, the engine can completely handle it
without consulting that agent or its client again. The agent decides what to authorize in advance;
the engine carries out that standing instruction under the ordinary game rules.

**Selected example:** Kim's agent installs a policy saying to take her Arctic Algae plants. Stan
places an ocean and selects the resulting plant task assigned to Kim. The engine completes that
task using Kim's installed policy, so Stan does not have to wait for Kim or her agent to respond.
This example selects the interaction to support; it does not prescribe a card-specific engine
recognizer or an API for selecting another Actor's task.

This changes the earlier blanket placement of automatic submissions in Agent. A separate Agent
call is unnecessary for covered tasks, but deciding the policy remains an agent responsibility.
Selection alone supplies no new execution permission: the installed policy supplies it beforehand.
Without such authorization, the rule against execution merely because selection or narrowing
makes an instruction fully specified remains. Covering a selected task does not by itself authorize
choosing among unrelated pending tasks or making another Actor's unanswered choices.

The engine's transaction-uniqueness proof still excludes execution of subsequently queued tasks.
Installing a policy does not turn later policy-driven execution into evidence for accepting an
earlier transaction, and it does not move whole-action search or outcome comparison into engine.
Mandatory Admin handling remains automatic independently of Player settings; this decision does
not select a new placement or configurable policy for the Admin driver.

**Deferred preference example:** a client tells its agent, "For my Philares bonuses, always take
titanium." The agent would translate that preference into a standing engine policy, authorizing
a meaningful choice in advance. This belongs in bucket 4 when the engine applies the installed
answer, whereas an agent evaluating the current bonus and submitting an answer uses bucket 2.
Preference storage, editing, and conflict resolution are future work, not current requirements.

**Open implementation questions:**

- How policies describe matching tasks and supplied answers, and how they are installed, updated,
  removed, and associated with the Actor, World, or agent session. No representation or API is chosen.
- How a caller selects another Actor's covered task while preserving its attribution and ordinary
  priority rules; how policy handling interacts with retained partial narrowing and handoffs.
- What happens when a matched task needs missing information, its policy does not determine an
  answer, or execution is rejected. A policy must not silently invent a choice or suppress failure.
- How finishing the selected task relates to newly queued work, and how resulting transactions,
  returned results, and restoration fit the surrounding call. Do not infer whole-action completion.
- How replay captures these decisions without executing them twice, and how previews/speculation
  apply installed policies consistently. Existing Player-policy replay behavior does not settle this.

## Current implementation evidence

Current [AutoExecPolicy](../../src/common/dev/martianzoo/agent/AutoExecPolicy.kt) and
[AutoExecLoop](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt) configure and run Player policy
in Agent. They do not implement the newly selected engine-installed policies. The inventory records
intended ownership; it does not claim that those policies or the full guidance services exist.
The engine transition and this agent design remain separate implementation work.
