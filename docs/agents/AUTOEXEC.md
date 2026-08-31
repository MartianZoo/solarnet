# Autoexecution policies

> **Read when:** changing policy attachment, scheduling, automatic task commands, policy
> provenance, or proof-preserving task analysis.
>
> **Skip when:** changing authored `::` effects or explicit task semantics; those belong in
> [ENGINE.md](ENGINE.md) and [SEQUENCING.md](SEQUENCING.md).
>
> **Status:** current mechanism plus the settled direction for stronger safe policies.

## Source map

- [`Agent.kt`](../../src/common/dev/martianzoo/engine/Agent.kt) defines `Agent.AutoExec` and its
  context.
- [`AutoExecDriver.kt`](../../src/common/dev/martianzoo/engine/AutoExecDriver.kt) owns attachment,
  scheduling, and provenance.
- [`AutoExecPolicies.kt`](../../src/common/dev/martianzoo/engine/AutoExecPolicies.kt) contains only
  generic policies the engine library is prepared to call safe.
- [`TfmAutoExecPolicies.kt`](../../src/common/dev/martianzoo/tfm/engine/TfmAutoExecPolicies.kt)
  contains Terraforming Mars client policy, including deliberate choice-making.
- [SMART_AUTOEXEC.md](SMART_AUTOEXEC.md) owns the proof obligations for stronger policies.

## Model

Each World retains exactly one `Agent` for every Actor. `Agent.AutoExec` is an optional plugin
installed on one of those Agents. A policy receives:

- the Agent it is attached to;
- the Agent whose command triggered the pass;
- the whole pending task pool for read-only ordering information.

A policy may inspect freely but may perform at most one command through the Agent to which it is
attached per `advance` call. It returns `true` exactly when that call changed the World. The driver
checks this against the event log, then throws away every earlier conclusion and restarts
scheduling. A policy has no privileged mutation path and cannot acquire another Actor's Agent.

Attaching the same policy instance twice is idempotent. Detachment is explicit. Player policies are
client choices; the generic library does not attach one for a Player.

## Completion and scheduling

After a successful outer Agent command has completed all of its authored `::` consequences, the
driver runs before the command reports completion. Operation-body task commands provide the same
completion point between statements. Re-entry caused by a policy command does not recursively
start a second driver.

At every completion point:

1. policies attached to the Engine Agent run first;
2. any successful Engine policy command restarts step 1;
3. only after every Engine policy declines does the driver nudge every Agent carrying a Player
   policy; and
4. a successful Player policy command restarts from Engine again.

Agents without eligible work simply decline. Each policy can command only its own Agent. This lets
cross-Player effects and workflow activity finish without requiring a client to invoke every Agent.

Player visitation order is not a semantic promise. Ordinary play currently assumes that competing
unselected Player decisions are not simultaneously pending, so changing visitation order must not
change the result. A future simultaneous interaction such as drafting must not expose one Player's
choice to another through this loop. It should collect private decisions independently and merge
them into one later World state.

## Supplied policies

`AutoExecPolicies.engine` is attached automatically when a World is created. It examines Engine's
first pending task and executes it when execution needs no choice. When it is the only task in the
World, the policy may instead select it even when it remains abstract; selection does not choose a
narrowing. It exhausts Engine work before Player policies run, while never performing Player work.

`AutoExecPolicies.safe` currently advances only when the entire World contains one pending task and
that task belongs to the Agent carrying the policy. It executes a concrete executable task, or
selects an unselected task that still needs a choice. The latter exposes the choice without making
it. The whole-pool singleton requirement is deliberately conservative.

The generic library does not provide `first`. `TfmAutoExecPolicies.first` is a client helper that
may execute any currently legal task belonging to its Agent, and the engine tests own an equivalent
test-only helper. Its contract makes no FIFO, fairness, or stable-order promise. It stops when no
task can proceed without another decision. Neither helper is a safety proof.

## Safe selection

When an Agent has exactly one task it can see, selecting that task is safe under the current game
model: the game is waiting on that Actor, and selection exposes rather than answers an abstract
choice. This rule relies on the absence of live simultaneous Player decisions described above.

If an Agent has several tasks and only one passes the current selection probe, choosing that task is
plausible but not yet proven safe. `canSelectTask` establishes present feasibility only. Another
task might become feasible after other state changes, so the probe alone does not prove that
selection preserves every eventual option. Keep the generic policy conservative until a systemic
rule or `slow` analysis proves the stronger claim.

## The planned `slow` policy

The engine library should eventually supply `slow`: an exhaustive proof policy that spends as much
analysis as necessary to automate every command it can prove preserves the complete net-effect
decision tree. Uncertainty means no command.

This requires disposable Worlds or an equivalent analysis facility. The policy must enumerate all
relevant legal commands, explore their continuations, and compare normalized component/task state
at a shared semantic comparison point. A successful branch, matching headline resources, or the
absence of a known counterexample is insufficient. Event ordinals, task ids, and policy credit may
differ only when no later game rule can observe those differences.

Do not publish a cheaper heuristic under the `slow` name. Build the analysis substrate when a real
proof policy is implemented; do not add speculative public APIs ahead of it.

## Provenance and replay

Actor identifies who acts in the game. Autoexec name identifies which policy issued a command.
Policy-issued Player inputs record that diagnostic name on `GameplayInputEvent`; it does not change
task assignee, effect performer, or semantic state.

The REgo spellings `AUTO NONE`, `AUTO SAFE`, and `AUTO FIRST` remain script-client configuration,
not engine modes. `FIRST` selects the Terraforming Mars client policy.

Serialized REgo replay is stricter than interactive use: every Agent, including Engine, runs with
autoexecution disabled. The replay explicitly submits every meaningful Agent task command, making
the command stream an executable expectation rather than a trace whose omissions are filled by a
policy. Authored `::` consequences remain effect semantics and are not autoexec.

The current script profile and input record do not yet fully implement this rule. Replay setup must
be able to detach the default Engine policy, and explicit Engine-attributed commands must become
representable in the recorded input stream. [ROUTINES.md](ROUTINES.md#native-world-export) owns the
serialization contract.

## Required invariants

- Engine policies exhaust their work before a Player policy is consulted.
- With all optional Player policies detached, no Player choice is selected automatically.
- Every policy command uses only the Agent carrying that policy.
- An Engine policy never performs a Player task.
- Each accepted command invalidates all prior policy analysis.
- Every generic supplied policy preserves all semantically distinct legal continuations.
- Deliberate choice-making lives in a client library, is named as such, and promises no task order.
- Disabling policies does not change authored `::` effects or explicit command semantics.
- Serialized replay disables policies for every Agent and records meaningful commands explicitly.
