# Agent autoexecution and policy-relative stable points

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing Agent policies, `autoExecNow`, after-completion scheduling, autonomous task
> actions, policy provenance, or proof-preserving task analysis.
>
> **Skip when:** changing authored `::` effects or explicit task semantics. Those belong in
> [ENGINE.md](ENGINE.md), [SEQUENCING.md](SEQUENCING.md), and [API.md](API.md).
>
> **Status:** Agent-owned policies and the shared synchronous settlement loop are implemented.
> Stronger proof-oriented policy families remain forward-looking.

## Choice-safety check

- No active policy means no automatic choice.
- `EAGER` deliberately chooses strategy and may change the outcome. Never add it merely to make a
  card test or faithful replay proceed.
- `CONCRETE` may act only when its named proof contract shows that no legitimate continuation is lost;
  a singleton-looking task is not sufficient by itself.
- Authored `::` consequences are engine semantics, not Agent policy. Do not use autoexecution to
  compensate for missing immediacy, task identity, or completion semantics.

## Source map

- [`Agent.kt`](../../src/common/dev/martianzoo/agent/Agent.kt),
  [`AutoExecPolicy.kt`](../../src/common/dev/martianzoo/agent/AutoExecPolicy.kt),
  [`AgentImpl.kt`](../../src/common/dev/martianzoo/agent/AgentImpl.kt), and
  [`AutoExecLoop.kt`](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt) contain the current
  Agent-owned implementation.
- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt) contains the policy-free
  task commands and probes used by the loop.
- [API.md](API.md) owns the unique Actor-scoped Agent and its client surface.
- [SMART_AUTOEXEC.md](SMART_AUTOEXEC.md) owns optional proof guarantees for supplied policies.

## Policies are part of Agent

Every configured Game World has exactly one Agent per Actor. Every explicit and autonomous mutation
for that Actor normally enters through the same Agent. The Agent itself owns the policies that may
choose further actions; no separately named driver is required.

Agent exposes policy addition and removal plus `autoExecNow()`. Its policy storage, precedence, and
decision process remain implementation details unless an actual client needs to control them.
`autoExecNow()` asks the shared loop to let every Agent's installed policy act against the current
game and does not belong to the engine. Adding or removing a policy invokes that loop before
returning, because either change can alter the remaining policies' decisions. An Agent with no
active policy makes no choices, though the same loop may still settle work assigned to other
Actors whose policies are active.

Any subset of Actors may be fully autonomous. There is no separate AI-player kind: adding policies
to the same Agent progressively reduces the decisions left for a human until its policies cover every
legal choice. A normal game makes Admin fully autonomous and leaves Player autonomy to application
configuration.

An Agent policy can choose anything the same Agent could issue for an explicit client request,
including strategically bad or peculiar actions. The engine enforces validity, not strategy.
Stronger promises belong to a named policy and its tests.

## Shared autoexecution loop

The engine knows nothing about Agents, policies, or autonomy. After one engine mutation has
completely finished, including its immediate effects and task updates, it reports that the game
changed. It does not report intermediate states while effects, sibling creation, or rollback are
still in progress.

The `:agent` module then runs one plain shared loop:

1. offer queued task candidates, in queue order, to their assignee Agent's policy;
2. allow at most one engine mutation;
3. if an Agent acts, discard answers computed from the old game and begin another pass; and
4. return from the original Agent call only after one complete pass in which no Agent acts.

This loop belongs to the private wiring shared by the Agents created for one game. Each Agent may
retain the same private loop object, and no additional public game wrapper is needed. The
implementation iterates rather than recursively growing the call stack.

Agent visitation order is loop mechanics, not game precedence. No rule or policy proof may rely on
incidental Agent, policy, or task enumeration. Tests should reverse and reproducibly randomize those
enumerations.

## Policy-relative stable point

A game is at a **policy-relative stable point** when every Agent has consulted its policies against
the same completed game revision and declined to issue another mutation. “Stable” is relative to
the exact installed policies: changing them may make another action available without changing the
Game World. It does not mean that the global task queue is empty.

A fully autonomous Agent promises to decline only when it has no legal action covered by its
contract. A task may still be assigned to it when game state temporarily makes that task illegal,
for example while another Actor's selected task holds the select-lock. The generic guarantee is
therefore a fixed point of legal Agent actions, not literal queue emptiness.

Normal Admin behavior has a stronger application expectation: before a Player-facing Agent call
returns, no Admin-assigned task remains. Admin work should be admitted and sequenced so its policies
can settle it before Players observe the result. If a supposedly normal stable point retains Admin
work, first suspect incomplete Admin policy, premature task admission, or incorrect sequencing. A
genuine rule that must leave Admin blocked on a Player choice should be modeled and documented
explicitly rather than silently weakening this expectation.

## Supplied policy families

`safe` may act only when it proves that its mutation preserves every continuation named by its
contract. Because an Agent can navigate from its scoped reader to the unscoped `GameReader`, it can
prove whole-pool claims without a special engine API. Selecting an abstract task without narrowing
may be allowed when the policy proves that it does not steal another controller's decision.

`first` is intentionally choice-making. It may issue any currently legal mutation, makes no FIFO or
fairness promise, and may change the outcome. Generic and Terraforming Mars applications may each
supply one; the engine does not.

`slow` is a future exhaustive proof policy. It may use disposable Worlds only through a permitted
hypothetical-analysis facility and must decline on uncertainty. [SMART_AUTOEXEC.md](SMART_AUTOEXEC.md)
defines that optional guarantee. A caller may instead install a policy with no such promise.

## Replay identity preservation

> **Proposal:** Source-backed Terraforming Mars replays need a policy that preserves choices whose
> anonymous engine representation would otherwise erase source-known card identity.

Do not lower the replay Player's whole autoexecution level for this purpose. A replay policy should
otherwise make the same eager choices as the ordinary first-choice policy, but decline a task that
would purely remove a `ProjectCard`. The replay then performs that removal explicitly with the
sourced card names and can associate those names with the exact resulting events. Playing a project
card is not such a removal: its transmutation into the named card preserves the identity needed by
the replay.

This policy belongs in Terraforming Mars replay configuration, not in the generic Agent library.
Generic policy machinery must not name `ProjectCard`, and the distinction must inspect the typed
task instruction rather than rendered text. Do not add an `EAGER_EXCEPT_PROJECT_CARD` enum value or
an independent task-filter mechanism beside Agent policies. The clean implementation depends on the
configurable-policy direction described above: install a Terraforming Mars policy in place of the
ordinary eager policy, and filter candidates before either selection or execution can acquire the
select-lock.

With the removal reserved, replay helpers should keep card names beside the source action, execute
the named discard or rejection directly, and annotate only events produced by that command. This is
intended to remove retrospective matching across unrelated event ranges, not to establish gameplay
ordering or make card identity part of the engine model. Any mutable annotation needed while a
replay is being assembled should become immutable at the recording boundary; recording ownership is
specified by [GAMEWORLD.md](GAMEWORLD.md).

## The planned `slow` policy

The agent library should eventually supply `slow`: an exhaustive proof policy that spends as much
analysis as necessary to automate every command it can prove preserves the complete net-effect
decision tree. Uncertainty means no command.

The disposable World model needed for this analysis is unplanned. The policy must enumerate
all relevant legal commands, explore their continuations, and compare normalized component/task state
at a shared semantic comparison point. A successful branch, matching headline resources, or the
absence of a known counterexample is insufficient. Event ordinals, task ids, and policy credit may
differ only when no later game rule can observe those differences.

Do not publish a cheaper heuristic under the `slow` name. Build the analysis substrate when a real
proof policy is implemented; do not add speculative public APIs ahead of it.

## Current implementation

Committed code stores `AutoExecPolicy` on each `Agent`, defaults every Agent to `EAGER`, and invokes
one shared loop from Agent-side command and operation completion points. The engine has no policy or
autoexecution dependency. The loop preserves global task-queue order while consulting the policy of
each candidate task's assignee; `NONE` therefore prevents that Actor's work from being selected by
another Agent. `CONCRETE` considers ambiguity among the selectable candidates assigned to that same
Actor. Configurable policy attachment and the planned Admin-first policy schedule remain
forward-looking.

Admin's default may execute concrete work, select abstract work, narrow choices, and intelligently
choose among available Admin tasks. Admin is not inherently deterministic or choice-free. Its legal
powers come from game state; its autonomous behavior comes from its policy configuration.

## Accepted temporary limitations

- The legacy dead-end fallback may try an active Agent's temporarily illegal task when a `NONE`
  Agent has selectable work that could enable it, rather than returning at the policy-relative
  stable point. No current Terraforming Mars scenario demonstrates this edge. Characterize a real
  case before changing dead-end classification.
- `TfmGameplay.playCorp` currently depends on `EAGER`: `CONCRETE` neither opens the retained-project
  offer nor proves the order between corporation rewards and the resulting card purchase. Do not
  repair that ordering in the gameplay helper. Revisit it when the owning Pets or engine rule can
  express why starting cash precedes payment for retained cards.
- `Game20230521Test` temporarily switches Players to `EAGER` to drain mandatory production work
  that `CONCRETE` sees as several sibling tasks. This is accepted replay infrastructure, not a
  gameplay-policy precedent. Replace it when a policy can prove those tasks mandatory; meanwhile,
  revisit the seam if production can expose any optional Player choice.

## Required properties

- `:engine` and `:state` contain no Agent or policy behavior.
- A configured Game World has exactly one Agent per Actor.
- Every ordinary explicit and autonomous Actor mutation enters through that Agent.
- The engine reports only that a complete mutation changed the game; it does not schedule Agents.
- The shared Agent loop reaches a policy-relative fixed point before the original Agent call
  returns.
- Every accepted mutation invalidates all prior Agent analysis.
- Each Agent owns its policy and task-choice strategy.
- Policy, Agent, and task enumeration order have no game meaning.
- A normal Player-facing stable point contains no Admin-assigned work.
- An Agent with no active policies retains the same explicit mutation semantics.
