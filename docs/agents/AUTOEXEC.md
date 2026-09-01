# Agent Drivers and policy-relative stable points

> **Read when:** changing Agent Drivers, policy installation, settled-state pulses, autonomous task
> actions, policy provenance, or proof-preserving task analysis.
>
> **Skip when:** changing authored `::` effects, explicit task semantics, or Actor access. Those
> belong in [ENGINE.md](ENGINE.md), [SEQUENCING.md](SEQUENCING.md), and [API.md](API.md).
>
> **Status:** selected layer ownership and forward-looking synchronous-settlement contract. Current
> code still implements autoexecution inside `:engine` through `AutoExecMode`.

## Source map

- [`Agent.kt`](../../src/common/dev/martianzoo/engine/Agent.kt),
  [`AutoExecMode.kt`](../../src/common/dev/martianzoo/engine/AutoExecMode.kt),
  [`ApiTranslation.kt`](../../src/common/dev/martianzoo/engine/ApiTranslation.kt), and
  [`Implementations.kt`](../../src/common/dev/martianzoo/engine/Implementations.kt) contain the
  current engine-owned implementation to extract.
- [API.md](API.md) owns the unique Agent and its private `ActorAccess` conduit.
- [SMART_AUTOEXEC.md](SMART_AUTOEXEC.md) owns optional proof guarantees for supplied policies.

## Agent Driver

Every configured Game World has exactly one Agent per Actor. Every explicit and autonomous mutation
for that Actor enters through the same Agent. The Agent owns an `AgentDriver`, the working name for
the part that autonomously chooses further actions according to installed policies.

An Agent need not expose its policies, their storage shape, or their precedence. It merely accepts
explicit requests, receives wake-up pulses, and acts. Policy installation configures that behavior;
the Agent handles any interaction among installed policies internally.

Any subset of Actors may be fully autonomous. There is no separate AI-player kind: adding policies
to the same Agent progressively reduces the decisions left for a human until its Driver covers every
legal choice. A normal game makes Admin fully autonomous and leaves Player autonomy to application
configuration.

The initial access layer is maximally permissive. A Driver can therefore choose anything that the
same Agent could issue for an explicit client request, including strategically bad or peculiar
actions. Engine and access layers enforce their own validity rules, not strategy. Stronger promises
belong to a named policy and its tests.

## Generic settled-state pulse

The engine knows nothing about Agents, Drivers, policies, or autonomy. After any outermost mutation
finishes, it emits a generic pulse identifying the coherent committed World revision. Task insertion
does not emit early pulses while effects, normalization, sibling creation, or rollback are still in
progress.

A session-level dispatcher fans that pulse out to Agents. The dispatcher is generic coordination,
not an autoexec component: it does not inspect tasks, choose actions, or know why an Agent responds.
It queues nested pulses and drains them iteratively rather than recursively:

1. one explicit or autonomous Agent mutation reaches coherent engine completion;
2. the dispatcher marks every Agent ready to inspect the new revision;
3. a woken Agent may issue at most one further mutation through its private `ActorAccess`;
4. any resulting revision invalidates all earlier observations and schedules a fresh pass; and
5. the outermost Agent call returns only after every woken Agent declines from the same revision.

Agent visitation order is dispatcher mechanics, not game precedence. An Agent owns its policy and
task-choice strategy. No rule or policy proof may rely on incidental Agent, policy, or task
enumeration. Tests should reverse and reproducibly randomize those enumerations.

## Policy-relative stable point

A configured session is at a **policy-relative stable point** when every Agent Driver has inspected
the same World revision and declined to issue another mutation. “Stable” is relative to the exact
installed policies: changing them may make another action available without changing engine state.
It does not mean that the global task pool is empty.

A fully autonomous Agent promises to decline only when it has no legal action covered by its
contract. A task may still be assigned to it when game state temporarily makes that task illegal,
for example while another Actor's selected task holds the select-lock. The generic guarantee is
therefore a fixed point of legal Agent actions, not literal queue emptiness.

Normal Admin behavior has a stronger application expectation: before a Player-facing Agent call
returns, no Admin-assigned task remains. Admin work should be admitted and sequenced so its Driver
can settle it before Players observe the result. If a supposedly normal stable point retains Admin
work, first suspect incomplete Admin policy, premature task admission, or incorrect sequencing. A
genuine rule that must leave Admin blocked on a Player choice should be modeled and documented
explicitly rather than silently weakening this expectation.

## Supplied policy families

`safe` may act only when it proves that its mutation preserves every continuation named by its
contract. Because initial `ActorAccess` may inspect the whole World, it can prove whole-pool claims
without a special engine API. Selecting an abstract task without narrowing may be allowed when the
policy proves that it does not steal another controller's decision.

`first` is intentionally choice-making. It may issue any currently legal mutation, makes no FIFO or
fairness promise, and may change the outcome. Generic and Terraforming Mars applications may each
supply one; the engine does not.

`slow` is a future exhaustive proof policy. It may use disposable Worlds only through a permitted
hypothetical-analysis facility and must decline on uncertainty. [SMART_AUTOEXEC.md](SMART_AUTOEXEC.md)
defines that optional guarantee. A caller may instead install a policy with no such promise.

Admin's default may execute concrete work, select abstract work, narrow choices, and intelligently
choose among available Admin tasks. Admin is not inherently deterministic or choice-free. Its legal
powers come from game state; its autonomous behavior comes from its Driver configuration.

## Provenance and replay

Actor identifies who acts in the game. Agent, Driver, and policy identity are optional diagnostic
provenance on a mutation; they do not change assignee, controller, narrower, performer, or semantic
state.

The REgo spellings `AUTO NONE`, `AUTO SAFE`, and `AUTO FIRST` remain application configuration, not
engine modes. Serialized replay disables autonomous Drivers and issues every recorded meaningful
Actor mutation explicitly through the corresponding Agent, including Admin mutations. Generic
pulses may still occur, but disabled Drivers decline. Authored `::` consequences remain engine
semantics and are never replayed as autonomous actions.

## Current implementation divergence

Current code stores `AutoExecMode` on engine-owned Agents, defaults it to `FIRST`, scans the global
pool from whichever Agent completed a call, and drains within a shared recursively re-enterable
atomic scope. It has no access layer, private Agent Driver, or generic session pulse dispatcher.
Treat that behavior as migration input, not target ownership.

## Required properties

- Engine and state modules contain no Agent, Driver, access, or policy behavior.
- A configured Game World has exactly one Agent per Actor.
- Every explicit and autonomous Actor mutation enters through that Agent and private `ActorAccess`.
- The engine emits only a generic coherent-revision pulse.
- The session dispatcher queues nested pulses and reaches a policy-relative fixed point before the
  outermost Agent call returns.
- Every accepted mutation invalidates all prior Agent analysis.
- An Agent, not the dispatcher, owns policy and task-choice strategy.
- Policy, Agent, and task enumeration order have no game meaning.
- A normal Player-facing stable point contains no Admin-assigned work.
- Disabling autonomous Drivers leaves explicit Agent mutation semantics unchanged.
- Serialized replay records meaningful Actor mutations explicitly and does not guess them.
