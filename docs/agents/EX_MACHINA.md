# Direct corrections

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing `exMachina`, `sneak`, correction validation, effect suppression, or
> required-part construction.
>
> **Status:** current correction contract. Source and tests win if details differ.

## Contract

A correction changes concrete state without pretending that normal gameplay caused the change.
It preserves structural validity, but it does not reproduce costs, rewards, placement bonuses,
history, or other queued consequences of ordinary play.

Correction facilities are available for local calculation; they are not privileges reserved for
an authenticated operator. The structural checks below specify what a correction computes, not
whether other players agree to it. [ADVERSARIAL.md](ADVERSARIAL.md) assigns acceptance of a shared
correction or takeback to the surrounding game arrangement. An internal `Audit` records a supplied
adjustment or assertion; it is not proof of player consent or an external custodian's approval.

- A correction group is failure-atomic. Rejection restores components, pending tasks, history,
  and derived indexes.
- Concrete required parts of a newly gained component are constructed before gain reactions.
- Dependents of a removed owner are removed with it.
- Recursive automatic (`::`) effects still run because they can establish inseparable state.
- Queued (`:`) effects are suppressed throughout the correction and its derived changes.
- Every applicable declared count invariant is checked against the completed World. Validation
  rejects an invalid result; it does not repair it.
- Corrections do not settle pending tasks, perform idle cleanup, or call the gameplay completion
  callback.
- A correction need not enforce game history that is not represented as an invariant. For
  example, arbitrary delegate edits need not reconstruct political leadership, and a corrected
  tile does not grant placement rewards.

Prefer ordinary `:` effects when a consequence can safely remain normal gameplay. Retain `::`
only when postponing the consequence would expose incoherent state. Do not add a new effect
category merely to make unrestricted editing convenient.

## Entry points

`Agent.sneak` is the lower-level engine correction path. It accepts fully concrete changes and
provides required-part construction, dependent removal, recursive automatic effects, and full
invariant validation.

`Agents.exMachina` is the client-facing correction path. It adds an internal `Audit` and rejects
direct changes to `System`, `Hidden`, and `MustCleanUp` types on either side of a transmutation.
Derived changes may still touch those types when required by the corrected state. It also rejects
any result that leaves additional `MustCleanUp` state, so a correction cannot start a workflow
whose ordinary completion effects were suppressed.

When a task is selected, `exMachina` restores its unresolved form, applies the correction, and
resumes selection under the existing policy. The whole sequence remains failure-atomic.
Pre-existing gameplay may then proceed normally; queued effects of the correction remain
suppressed.

## Required-part construction

Creating a component establishes a positive exact-count invariant only when its specialized target
is a concrete type directly dependent on that component. `ClassLimitTable.requiredParts` derives
those targets from inherited invariant templates by binding `This` to the complete owner type.
The engine creates only the missing count and waits for declared dependencies across the complete
structure before dispatching gain reactions.

This is deliberately not a general constraint solver:

- `HAS =1 This` constrains an owner; it does not create an absent owner.
- Minimum-only counts, abstract targets, refinements, unrelated targets, and indirect requirements
  do not imply constructors.
- Existing sufficient parts are retained, and duplicate inherited requirements collapse.
- Conflicting requirements or constructor chains beyond the execution-depth limit fail atomically.
- Removing a required part from a surviving owner fails instead of recreating it.
- Recorded playback reapplies recorded changes; it does not run construction again.

The rule supports exact attachments such as a printed card's tags and concrete card-bound
capabilities. Fanout, track synchronization, political leadership, and historical rewards remain
explicit game behavior rather than inferred correction machinery.

## Evidence

Inspect these entry points before changing the contract:

- [`exMachina.kt`](../../src/common/dev/martianzoo/agent/exMachina.kt)
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt)
- [`Limiter.kt`](../../src/common/dev/martianzoo/engine/Limiter.kt)
- [`WorldTransaction.kt`](../../src/common/dev/martianzoo/engine/WorldTransaction.kt)
- [`InvariantCompletionTest.kt`](../../test/common/dev/martianzoo/engine/InvariantCompletionTest.kt)
- [`WorldTransactionTest.kt`](../../test/common/dev/martianzoo/engine/WorldTransactionTest.kt)

Correction behavior should be proved through observable scenarios that exercise the shared
mechanism. Do not add tests that merely repeat a catalog inventory.
