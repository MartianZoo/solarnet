# Agent autoexecution and policy-relative stable points

> **Read when:** changing Agent policies, `autoExecNow`, after-completion scheduling, autonomous task
> actions, policy provenance, or proof-preserving task analysis.
>
> **Skip when:** changing authored `::` effects or explicit task semantics. Those belong in
> [ENGINE.md](ENGINE.md), [SEQUENCING.md](SEQUENCING.md), and [API.md](API.md).
>
> **Status:** selected layer ownership and forward-looking synchronous-settlement contract. Current
> code still implements autoexecution inside `:engine` through `AutoExecMode`.

## Source map

- [`Agent.kt`](../../src/common/dev/martianzoo/engine/Agent.kt),
  [`AutoExecMode.kt`](../../src/common/dev/martianzoo/engine/AutoExecMode.kt),
  [`ApiTranslation.kt`](../../src/common/dev/martianzoo/engine/ApiTranslation.kt), and
  [`Implementations.kt`](../../src/common/dev/martianzoo/engine/Implementations.kt) contain the
  current engine-owned implementation to extract.
- [API.md](API.md) owns the unique Actor-scoped Agent and its client surface.
- [SMART_AUTOEXEC.md](SMART_AUTOEXEC.md) owns optional proof guarantees for supplied policies.

## Policies are part of Agent

Every configured Game World has exactly one Agent per Actor. Every explicit and autonomous mutation
for that Actor normally enters through the same Agent. The Agent itself owns the policies that may
choose further actions; no separately named driver is required.

Agent exposes policy addition and removal plus `autoExecNow()`. Its policy storage, precedence, and
decision process remain implementation details unless an actual client needs to control them.
`autoExecNow()` asks that Agent's installed policies to act against the current game and does not
belong to the engine. If it produces a mutation, the normal shared loop then gives every Agent a
chance to respond. Adding or removing a policy invokes `autoExecNow()` before returning, because
either change can alter the remaining policies' decisions. For an Agent with no active policies,
`autoExecNow()` is a no-op.

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

1. give each Agent a chance to consult its policies;
2. allow an Agent to make at most one engine mutation;
3. if any Agent acts, discard answers computed from the old game and begin another pass; and
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
the exact installed policies: changing them may make another action available without changing
engine state. It does not mean that the global task pool is empty.

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

## The planned `slow` policy

The agent library should eventually supply `slow`: an exhaustive proof policy that spends as much
analysis as necessary to automate every command it can prove preserves the complete net-effect
decision tree. Uncertainty means no command.

The disposable World model needed for this analysis remains unplanned. The policy must enumerate
all relevant legal commands, explore their continuations, and compare normalized component/task state
at a shared semantic comparison point. A successful branch, matching headline resources, or the
absence of a known counterexample is insufficient. Event ordinals, task ids, and policy credit may
differ only when no later game rule can observe those differences.

Do not publish a cheaper heuristic under the `slow` name. Build the analysis substrate when a real
proof policy is implemented; do not add speculative public APIs ahead of it.

## Current implementation divergence

Committed code still stores `AutoExecMode` on each `Agent`, defaults it to `FIRST`, and runs the
queue drain from engine-side command and operation completion points. It does not yet provide
policy attachment or the planned Admin-first policy schedule. As a transitional
progress rule, a Player using `NONE` still drains only Engine-assigned work from the shared queue.
Treat the sections above and below as the extraction contract, not current behavior.

Admin's default may execute concrete work, select abstract work, narrow choices, and intelligently
choose among available Admin tasks. Admin is not inherently deterministic or choice-free. Its legal
powers come from game state; its autonomous behavior comes from its policy configuration.

## Required properties

- Engine and state modules contain no Agent or policy behavior.
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
