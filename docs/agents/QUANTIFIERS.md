# Instruction quantifiers

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing counts on gain/removal/transmutation, AMAP, abstract target choice,
> missing dependencies, zero limits, or composition of a quantifier with `OR`, gates, and `PER`.
>
> **Skip when:** changing ordering between separate instructions; use
> [SEQUENCING.md](SEQUENCING.md).
>
> **Status:** quantity semantics and current implementation rules. Ownership and deduction timing
> are being revised toward the [selected engine destination](ENGINE_CONTRACT.md).

Preserve authored quantity meaning, including optional and AMAP zero results. For an explicit
execution request, determining the sole narrowing whose transaction succeeds can require target
feasibility and limit analysis in the engine. Automatically reducing an unanswered choice belongs
to the agent. The current special cases below are not requirements to preserve unchanged. A
quantity-limit read can bound candidates without proving their transactions succeed; temporary
invariant violations repaired by immediate effects must not be rejected by an over-eager filter.

## Source map

- [`Instruction.kt`](../../pets/src/common/dev/martianzoo/pets/ast/Instruction.kt) — search
  for `enum class Quantifier` and the change instruction types.
- [`Limiter.kt`](../../src/common/dev/martianzoo/engine/Limiter.kt) — inspect for
  concrete limits and invariant headroom.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — search
  for `Quantifier` and `abstract` to see resolution of quantified changes.
- [`InstructionResolutionTest.kt`](../../test/common/dev/martianzoo/engine/InstructionResolutionTest.kt) and
  [`TaskResolutionTest.kt`](../../test/common/dev/martianzoo/engine/TaskResolutionTest.kt)
  — select scenarios matching the changed resolution phase.

## Before writing an explicit quantifier

See [Pets quantifier guidance](../../pets/docs/agents/QUANTIFIERS.md#before-writing-an-explicit-quantifier).

## Read only the relevant sections

| Task | Read |
| --- | --- |
| Establish terminology, evaluation time, or syntax | Vocabulary and evaluation time; Syntax and defaults |
| Change when count invariants are enforced | Invariants at operation completion |
| Change a concrete target | The matching concrete gain/removal/transmutation section |
| Change an abstract target or AMAP choice | Abstract pure gains and removals; Abstract transmutations |
| Handle absent dependencies, uninhabited types, or zero | Uninhabited and computed-zero changes |
| Combine a quantifier with `THEN`, `OR`, a gate, or `PER` | Composition |
| Evaluate real-game effects or a rejected alternative | Known Terraforming Mars consequences; the matching alternative only |

## Vocabulary and evaluation time

- The **requested count** `n` is the nonnegative count written on the change, or one when omitted.
  A metric may later multiply it to zero.
- A **concrete target** is one fully resolved Type, including every dependency argument. An abstract
  target denotes possible concrete Types.
- A gain or transmutation destination has a **missing dependency** when a component named by its
  resolved dependency Type does not exist. This is different from an existing target having no
  invariant headroom.
- The **limit** `m` is the concrete change's capacity under the current invariant bounds and
  physical source count. AMAP and optional quantities use it. Mandatory quantities require physical
  sources immediately and satisfy count bounds at operation completion. Negative capacity is
  clamped to zero.
- Zero-count changes retain their gained/removed Types through resolution and task matching.
  Execution emits no component changes. `Ok` remains an untyped no-op and can narrow a zero change.

Instructions normally resolve against the World in which their task is selected, not the World in
which an effect originally created the task. The resolved instruction must execute next against that
same World. Quantifiers do not preserve trigger-time target existence.

## Invariants at operation completion

Declared component-count invariants must hold when a full operation completes. For this check, an
operation consists of one initiating component change and its entire recursive `::` cascade,
including `THEN` stages within that automatic work. Mandatory changes may cross count bounds in
intermediate states. Before completing the operation, the engine validates affected bounds,
including positive minimums activated by newly gained components. Bootstrap audits all required counts
after constructing its initial world.

Creation also establishes positive exact-count concrete parts directly dependent on the newly
created component, recursively, before its own effects. This is construction from the invariant,
not repair during validation. Abstract/minimum-only requirements and unrelated prerequisites do not
construct anything. A correction group instead validates all applicable invariants once after its
explicit changes, required construction, and automatic effects, with queued effects suppressed; see [EX_MACHINA.md](EX_MACHINA.md).

Physical source availability and dependency integrity are checked immediately. AMAP and optional
quantity resolution also consult invariant bounds in the current World; negative headroom or
footroom becomes zero. These quantities cannot assume that later consequences will make room.

Completion determines when count invariants are enforced, not when changes become observable.
Trigger batches keep their existing snapshots, and nested effects can observe intermediate counts.
Authors must express causal dependencies through the events that actually cause their consequences.

Queued tasks, `THEN` stages outside the initiating automatic cascade, and separate initiating changes
each belong to separate operations for this check. They cannot repair an earlier operation. A game
action can contain several such operations; its completion does not postpone their invariant checks.

A family can own replacement with `This:: -Family(NOT This).` and a family-wide maximum of one.
The initiating gain must be mandatory to cross that maximum. Repeating the same concrete value
still fails: `NOT This` excludes both copies, and adding `HAS MAX 1 This` would only restate the
rejection. Turmoil's `Dominant` uses this pattern after its caller selects the new value; the cleanup
does not choose the winner. Its `PartyLeader` uses the same allowance to demote the previous leader
into a `PartyFollower` before checking the party's maximum of a single leader. Therefore neutral
event placement uses a mandatory `PartyDelegate` gain guarded by the neutral reserve count: AMAP
would see the occupied leader seat and gain zero before demotion could make room.

Turmoil's player supply check is a queued `Delegate` gain effect, after automatic consequences.
A placement can temporarily make eight delegates when the unused lobby delegate is still present;
`LobbyDelegate` automatically removes itself before the queued supply check. With no lobby delegate,
the eighth delegate violates the supply rule. `PartyDelegate` is not `Atomized`: Canon authors
individual placements explicitly, using a shared party variable when required, and Canon invariants
reject bulk delegate gains.

## Syntax and defaults

See [Pets quantifier guidance](../../pets/docs/agents/QUANTIFIERS.md#syntax-and-defaults).

## Concrete pure gains

For positive requested count `n`, the destination dependency is checked before its invariant limit:

| Current World | mandatory | optional | AMAP |
| --- | --- | --- | --- |
| Destination dependency is missing | `DependencyException` | `Ok` | `DependencyException` |
| Dependency exists and `m = 0` | `n!`, validated at operation completion | `0?` | `0!` |
| `0 < m < n` | `n!`, validated at operation completion | `m?` | `m!` |
| `m >= n` | `n!` | `n?` | `n!` |

Thus concrete AMAP is strict about the identity of its target but forgiving about the target's
capacity. A maxed card can receive zero AMAP resources; an absent named card is not a zero-capacity
card. Optional differs because zero is intrinsically one of its authored choices.

A concrete custom-class gain does not use the optional dependency fallback or this limit table. It
first translates to plain instructions; those generated instructions carry the meaningful
quantifiers.

## Concrete pure removals

For a positive requested count, a removal does not require the removed Type's dependency component
to exist separately: if no
matching removable component exists, its footroom is zero. A mandatory removal requires all `n`
physical copies immediately and validates declared minimums at operation completion. Optional and
AMAP removals use the invariant-limited footroom:

| Limit | mandatory | optional | AMAP |
| --- | --- | --- | --- |
| `m = 0` | `n!` if physically available, validated at operation completion | `0?` | `0!` |
| `0 < m < n` | `n!` if physically available, validated at operation completion | `m?` | `m!` |
| `m >= n` | `n!` | `n?` | `n!` |

The AMAP/optional limit includes every applicable minimum invariant. A mandatory removal may cross
that minimum only if its automatic consequences restore it by operation completion.

## Concrete transmutations

A transmutation is one atomic source/destination pair. Its limit is the most restrictive invariant
changed by either side. An invariant common to both sides is held constant and does not limit the
transfer.

When the destination dependency exists, the concrete quantifier rules above apply to the pair. A
missing destination dependency makes the pair unavailable for every quantifier; optional and AMAP
do not convert it to `Ok`. If source footroom is zero, however, optional and AMAP retain a
zero-count transmutation.

Transmuting a concrete Type into itself is invalid for every quantifier, including when a trigger,
class header, or `EACH` binding makes the Types equal. Content must exclude those bindings, as Mons
Insurance and StartToken do. A self-target selected by automatic narrowing of an open transfer has
zero capacity instead: mandatory resolution fails, while optional and AMAP can become `Ok`.
Signal gains without transmutation remain valid point events (L2-1), not self-transmutation instructions.

A zero AMAP transmutation can still bind Type Variables in a
following `THEN`; target selection and component movement are separate consequences of that stage.

## Abstract pure gains and removals

Resolution first performs unique-Type narrowing. If the target remains abstract, the
following rules apply.

For mandatory and AMAP, the engine searches the concrete domain using current prerequisites. A gain
candidate must be active, narrow the authored Type, and have all destination dependencies. A removal
candidate must be a matching existing component with enough physical copies. Mandatory availability
search ignores declared count bounds; AMAP requires at least one candidate with positive capacity
under those bounds. Target narrowing uses identity and present dependencies, without filtering by
capacity. Neither search executes consequences or predicts the result of an operation.

- Mandatory uses required count `n`. If no candidate meets the execution prerequisites for all `n`,
  resolution throws `LimitsException`. A selected candidate must execute all `n` and satisfy
  invariants at operation completion. It can fail then even when resolution left it available.
- AMAP uses required count one when deciding whether a useful target exists. If no candidate can
  execute one, the whole abstract change becomes `Ok` rather than presenting a meaningless choice.
  Otherwise Type narrowing chooses the target. Capacity does not filter that Type domain.
  After a target is selected, it executes the greatest count for that target, up to `n`, which may
  be zero even when a different eligible target has positive capacity.
- Optional does not globally filter gain targets by positive capacity. After unique narrowing, an
  abstract optional gain remains a choice unless its authored dependency is absent, in which case
  it becomes `Ok`. Selecting a concrete target then uses the concrete optional rules. An abstract
  optional removal becomes `Ok` when no matching component exists; otherwise it stays a choice,
  including when minimum invariants make every matching component currently unremovable.

Target eligibility belongs to the authored Type and its refinements. AMAP does not require selection
of the candidate with the largest limit and does not distribute one count among several targets.
A missing concrete dependency remains unavailable because it does not name an existing target;
zero capacity on an existing eligible target is different and resolves to `Ok`.

Abstract `CustomInstruction` gains use ordinary pure-gain feasibility. After a concrete target is
gained, its event queues the Kotlin-produced instruction tree. Virtual `CustomMetric` types cannot
be gained, removed, or transmuted.

## Abstract transmutations

The pure-change domain search deliberately does not preflight abstract transmutations. Source and
destination Types can use the same variable, so narrowing must choose one compatible atomic pair
before its limit is known. Once concrete, the pair follows the concrete transmutation rules.

This permits a selected nonmandatory pair to execute zero and still supply its Type Variable to a
`THEN` continuation. A missing destination dependency remains unavailable rather than becoming an
AMAP zero.

## Uninhabited and computed-zero changes

An uninhabited Type has a provably empty domain in the current premise. A change touching an
uninhabited target is `Ok` when optional or AMAP and fails when mandatory. It does
not auto-narrow, fire triggers, or participate in abstract feasibility.

When a `PER` metric or other scalar calculation makes the requested count zero, resolution retains
the zero-count change before positive-count quantifier behavior is needed. Concrete limit
calculations likewise retain the Types at zero. Such changes need no target selection, existing
dependency, or System handoff to execute; they emit no change events. `0 Foo` can therefore identify
a zero-valued `Foo / metric` task without identifying a zero-valued `Bar / metric` task.
Task matching also rejects an intersection consisting only of `Ok` for a typed-zero submission:
`0 Foo` cannot identify unrelated work through an explicit or resolved `Ok` alternative.
This is a task-identification rule; Pets intersection remains symmetric, and a submitted `Ok` can
still complete a zero-count task. Batch submissions through `doTasks` retain their signed-total
semantics: a zero total may execute offsetting positive gains and removals, including their effects.
Task admission discards literal zero changes as no-ops; a count calculated during selection stays
typed until the submitted narrowing has been checked and the selected task executes.

Unavailable abstract targets, uninhabited Types, and optional missing-dependency fallbacks still
produce `Ok`. Extending retained counts to those paths remains open; their lost Type information
means a typed zero cannot identify those resolved outcomes. They still require `Ok`.

## Composition

- `A OR B`: each arm resolves independently against the same current World. Failed gates, missing
  physical sources or dependencies, and mandatory uninhabited changes can eliminate arms. Declared
  count bounds do not eliminate mandatory arms, even when no automatic consequence will repair
  them. A surviving arm still has to satisfy the execution checks. `Ok` is a real
  surviving arm, and equal surviving arms are deduplicated. An alternative containing only zero
  changes and `Ok` requires no decision, while retaining its Types for matching. If no arm survives,
  the `OR` reports the strongest applicable failure rather than becoming `Ok`. An automatic effect
  must still resolve without a choice: use an authored condition if one arm is intended, or queued work if an Actor
  should choose. Admin can make such ordinary queued choices too.
- `A THEN B`: only `A` resolves initially. `B` resolves against the World produced by `A`, after
  Type Variables selected by `A` have been substituted. `B` still follows when nonmandatory `A`
  resolves to `Ok`; author `A` as mandatory when the continuation requires a positive change.
- `A, B`: the instructions become independent sibling tasks. Each resolves against the World in
  which it is selected, and either may be selected first unless another mechanism orders them.
- `requirement: A`: the requirement is checked before `A`. Failure makes that arm unavailable; it
  does not change `A`'s quantifier.
- `A / metric`: the metric is evaluated first and multiplies `A`'s requested count. Zero retains
  the change and its Types; a positive gain of an `Atomized` type splits into independent gains of
  one before resolving their choices and limits. Other changes follow the normal rules.
- `A BY actor`: `BY` changes the performer, not the target domain, count, or limit.
- `PROD[A]`: production lowering changes the component Types first; the resulting changes
  then follow this specification.

A concrete AMAP gain with a missing dependency therefore fails as a standalone instruction but is
discarded when it is an arm of `OR`. The fallback comes from `OR`, not from AMAP.

## Known Terraforming Mars consequences

See [Pets quantifier guidance](../../pets/docs/agents/QUANTIFIERS.md#known-terraforming-mars-consequences).
