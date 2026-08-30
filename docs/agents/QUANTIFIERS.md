# Instruction quantifiers

> **Read when:** changing counts on gain/removal/transmutation, AMAP, abstract target choice,
> missing dependencies, zero limits, or composition of a quantifier with `OR`, gates, and `PER`.
>
> **Skip when:** changing ordering between separate instructions; use
> [SEQUENCING.md](SEQUENCING.md).
>
> **Status:** current normative engine contract.

## Source map

- [`Instruction.kt`](../../src/common/dev/martianzoo/pets/ast/Instruction.kt) — search
  for `enum class Intensity` and the change instruction types.
- [`Limiter.kt`](../../src/common/dev/martianzoo/engine/Limiter.kt) — inspect for
  concrete limits and invariant headroom.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — search
  for `Intensity` and `abstract` to see resolution of quantified changes.
- [`InstructionResolutionTest.kt`](../../test/common/dev/martianzoo/engine/InstructionResolutionTest.kt) and
  [`TaskResolutionTest.kt`](../../test/common/dev/martianzoo/engine/TaskResolutionTest.kt)
  — select scenarios matching the changed resolution phase.

A quantifier controls the count executed by one gain, removal, or transmutation. It does not choose
an `OR` arm, satisfy a gate, choose a concrete target, or determine a `PER` metric. Those operations
compose with quantification but have their own rules.

## Read only the relevant sections

| Task | Read |
| --- | --- |
| Establish terminology, evaluation time, or syntax | Vocabulary and evaluation time; Syntax and defaults |
| Change a concrete target | The matching concrete gain/removal/transmutation section |
| Change an abstract target or AMAP choice | Abstract pure gains and removals; Abstract transmutations |
| Handle absent dependencies, uninhabited types, or zero | Uninhabited and computed-zero changes |
| Combine a quantifier with `THEN`, `OR`, a gate, or `PER` | Composition |
| Evaluate real-game effects or a rejected alternative | Known Terraforming Mars consequences; the matching alternative only |

## Vocabulary and evaluation time

- The **requested count** `n` is the positive count written on the change, or one when omitted.
  A metric may later multiply it to zero.
- A **concrete target** is one fully resolved Type, including every dependency argument. An abstract
  target denotes possible concrete Types.
- A gain or transmutation destination has a **missing dependency** when a component named by its
  resolved dependency Type does not exist. This is different from an existing target having no
  invariant headroom.
- The **limit** `m` is the greatest count the concrete change can execute without violating an
  invariant. It is never negative.
- `Ok` is the resolved representation of executing no change. Pets has no zero-count `Change` node.

Instructions normally resolve against the World in which their task is selected, not the World in
which an effect originally created the task. The resolved instruction must execute next against that
same World. Quantifiers do not preserve trigger-time target existence.

## Syntax and defaults

| Syntax | Name | Contract |
| --- | --- | --- |
| `!` | mandatory | Execute exactly `n`, or be unavailable. |
| `?` | optional | Let the player choose any count from zero through `min(n, m)`. |
| `.` | AMAP | Execute `min(n, m)`; the engine, not the player, chooses the count. |

An omitted quantifier is filled from the changed Class before play. `Component` supplies mandatory
gain and removal defaults. A subclass may override either direction independently. For example,
`GlobalParameter` gains and removals default to AMAP, while `CardResource` overrides only gain to
AMAP. An explicit symbol always wins.

For `Gaining FROM Removing` with no explicit quantifier, the gain and removal defaults are combined
into the transmutation's one quantifier. Mandatory wins over AMAP, and AMAP wins over optional. Each
side still supplies its own dependency defaults before the Types are resolved.

## Concrete pure gains

The destination dependency is checked before its invariant limit. Given requested count `n`:

| Current World | mandatory | optional | AMAP |
| --- | --- | --- | --- |
| Destination dependency is missing | `DependencyException` | `Ok` | `DependencyException` |
| Dependency exists and `m = 0` | `LimitsException` | `Ok` | `Ok` |
| `0 < m < n` | `LimitsException` | `m?` | `m!` |
| `m >= n` | `n!` | `n?` | `n!` |

Thus concrete AMAP is strict about the identity of its target but forgiving about the target's
capacity. A maxed card can receive zero AMAP resources; an absent named card is not a zero-capacity
card. Optional differs because zero is intrinsically one of its authored choices.

A concrete custom-class gain does not use the optional dependency fallback or this limit table. It
first translates to plain instructions; those generated instructions carry the meaningful
quantifiers.

## Concrete pure removals

A removal does not require the removed Type's dependency component to exist separately: if no
matching removable component exists, its footroom is zero. It therefore uses the same limit
table:

| Limit | mandatory | optional | AMAP |
| --- | --- | --- | --- |
| `m = 0` | `LimitsException` | `Ok` | `Ok` |
| `0 < m < n` | `LimitsException` | `m?` | `m!` |
| `m >= n` | `n!` | `n?` | `n!` |

The limit includes every applicable minimum invariant. Merely finding a matching component is not
enough when removing it would cross such a minimum.

## Concrete transmutations

A transmutation is one atomic source/destination pair. Its limit is the most restrictive invariant
changed by either side. An invariant common to both sides is held constant and does not limit the
transfer.

When the destination dependency exists, the concrete limit table above applies to the pair. A
missing destination dependency makes the pair unavailable for every quantifier; optional and AMAP
do not convert it to `Ok`. If source footroom is zero, however, optional and AMAP do become `Ok`.

Transmuting a concrete Type into itself is `Ok` when optional or AMAP and is an
`ExpressionException` when mandatory. A zero AMAP transmutation can still bind linked Types in a
following `THEN`; target selection and component movement are separate consequences of that stage.

## Abstract pure gains and removals

Resolution first performs ordinary unique-Type narrowing. If the target remains abstract, the
following rules apply.

For mandatory and AMAP, the engine can search a pure change's concrete domain. A concrete gain
candidate must be active, narrow the authored Type, have all destination dependencies, and have the
required invariant headroom. A concrete removal candidate must be a matching existing component
with the required footroom.

- Mandatory uses required count `n`. If no candidate can execute all `n`, resolution throws
  `LimitsException`. Otherwise every candidate that the player selects must execute all `n`.
- AMAP uses required count one when deciding whether a useful target exists. If no candidate can
  execute one, the whole abstract change becomes `Ok`. Otherwise the task remains a choice among
  concrete targets with positive capacity. After a target is selected, it executes the greatest
  count for that target, up to `n`.
- Optional does not globally filter gain targets by positive capacity. After unique narrowing, an
  abstract optional gain remains a choice unless its authored dependency is absent, in which case
  it becomes `Ok`. Selecting a concrete target then uses the concrete optional rules. An abstract
  optional removal becomes `Ok` when no matching component exists; otherwise it remains a choice,
  including when minimum invariants make every matching component currently unremovable.

AMAP does not require selection of the candidate with the largest limit. It requires a positive
candidate, then maximizes the count for that selected target. The count is never distributed among
several targets.

An abstract pure AMAP target may be narrowed only after its task is selected and resolved. The
select-lock then prevents a client from validating the domain in one World and executing the
chosen target in another. A zero-capacity or dependency-blocked target is rejected while the
authored domain has any positive candidate.

Abstract custom gains are not searched using pure-gain feasibility. They narrow normally; after a
concrete custom target is chosen, it translates to its instructions. Custom removal and
custom transmutation are unsupported.

## Abstract transmutations

The pure-change domain search deliberately does not preflight abstract transmutations. Source and
destination Types can contain linked variables, so narrowing must choose one compatible atomic pair
before its limit is known. Once concrete, the pair follows the concrete transmutation rules.

This permits a selected nonmandatory pair to execute zero and still supply its linked Type to a
`THEN` continuation. A missing destination dependency remains unavailable rather than becoming an
AMAP zero.

## Uninhabited and computed-zero changes

An uninhabited Type has a provably empty domain in the current premise. A change touching an
uninhabited target is `Ok` when optional or AMAP and a `DeadEndException` when mandatory. It does
not auto-narrow, fire triggers, or participate in abstract feasibility.

When a `PER` metric or other scalar calculation makes the requested count zero, resolution returns
`Ok` before ordinary positive-count quantifier behavior is needed.

## Composition

- `A OR B`: each arm resolves independently against the same current World. An unavailable arm is
  discarded. `Ok` is a real surviving arm, and equal surviving arms are deduplicated. If no arm
  survives, the `OR` reports the strongest applicable failure rather than becoming `Ok`.
- `A THEN B`: only `A` resolves initially. `B` resolves against the World produced by `A`, after
  linked Types selected by `A` have been substituted.
- `A, B`: the instructions become independent sibling tasks. Each resolves against the World in
  which it is selected, and either may be selected first unless another mechanism orders them.
- `requirement: A`: the requirement is checked before `A`. Failure makes that arm unavailable; it
  does not change `A`'s quantifier.
- `A / metric`: the metric is evaluated first and multiplies `A`'s requested count. Zero becomes
  `Ok`; a positive result follows the normal rules.
- `A BY actor`: `BY` changes the performer, not the target domain, count, or limit.
- `PROD[A]`: production lowering changes the component Types first; the resulting changes
  then follow this specification.

A concrete AMAP gain with a missing dependency therefore fails as a standalone instruction but is
discarded when it is an arm of `OR`. The fallback comes from `OR`, not from AMAP.

## Known Terraforming Mars consequences

- Local Heat Trapping's `2 Animal` is abstract AMAP. If at least one animal holder has capacity,
  the player must select a positive holder and place as many of the two animals as that holder can
  accept. Absent Fish and maxed holders are not legal zero selections. With no positive animal
  holder, the whole animal choice is `Ok`.
- CEO's Favorite Project and Corroder Suits use abstract AMAP card-resource gains. They become `Ok`
  when no compatible card can receive a resource.
- An abstract AMAP tile placement cannot select an occupied area while any legal area exists. When
  no legal area exists, the whole placement is `Ok`.
- Atmoscoop authors separate concrete AMAP global-parameter arms. A maxed arm resolves to `Ok` and
  remains selectable because it is an explicit arm; Atmoscoop is not one abstract parameter domain.
- Viral Enhancers authors `Plant OR CardResource<CardFront>`. When the entering bio card cannot
  hold a resource, the concrete resource arm has a missing dependency and is discarded, forcing
  Plant. This is an engine characterization, not a claim about an official ruling.
- Pharmacy Union does not rely on AMAP treating a vanished card as zero. Its microbe effect
  explicitly chooses between adding Disease while Pharmacy Union exists and `Ok` after it has
  flipped; its independent 4 M€ loss remains pending either way.

## Alternatives considered and their card consequences

These are plausible models we deliberately did not choose. They are recorded so future fixes do
not rediscover one locally and accidentally change unrelated cards.

### Treat every missing dependency as zero for nonmandatory changes

This would make Pharmacy Union's stale Disease task disappear without special Pets, but it would
also make concrete references implicit weak references. Local Heat Trapping could select absent
Fish even while Pets can receive animals. Viral Enhancers could retain an `Ok` resource arm and let
the player avoid the otherwise forced Plant. Misspelled, stale, or incorrectly specialized targets
would silently succeed. Pharmacy Union instead states its genuine lifetime exception explicitly.

### Let AMAP choose a target first, even when that target can execute zero

This is the per-target-only interpretation of “as much as possible.” It would allow Local Heat
Trapping to choose an absent or maxed animal holder while another holder can receive animals, and
would allow an occupied area to satisfy an abstract tile placement while a legal area exists. The
chosen rule first requires some positive target in the whole abstract domain, then maximizes within
the selected positive target.

### Require the globally largest target

This stronger interpretation would force Local Heat Trapping to choose a holder that can receive
both animals over one that can receive only one. We instead preserve the printed target choice: any
positive holder is legal, and AMAP determines only the amount placed on that holder.

### Distribute one quantified change across several targets

Under this model, Local Heat Trapping could put one animal on each of two cards to reach two. That
would turn one Type choice into an allocation problem and make `-3 Animal.` remove from several
holders. The engine instead treats one change as one concrete Type; cards that distribute resources
must author several instructions or an explicit fanout mechanism.

### Filter optional domains like AMAP domains

Requiring an optional target to be positive whenever any positive target exists would prevent the
known BugsTest cases in which a player selects a zero transfer to avoid stealing or Air Raid's
attack. It would also mean `?` no longer intrinsically permits zero after target selection. Cards
such as Comet for Venus that intentionally make removal optional would need an explicit `OR Ok` to
retain a voluntary decline. This may be a worthwhile future redesign of those card Pets, but it is
not the current meaning of `?`.

### Freeze every quantified task when its effect triggers

This would not solve Pharmacy Union cleanly. Freezing the Disease branch while the corporation
exists would leave a mandatory gain whose dependency is gone when it executes, or would require
components to remain addressable after removal. It would also make every delayed sibling resolve
against stale capacities. Trigger-time snapshots, when genuinely needed, should be an explicit
sequencing feature rather than an implicit quantifier rule.

### Keep an all-zero abstract AMAP domain as a target choice

Local Heat Trapping would strand the player with animal names that cannot receive anything, and
CEO's Favorite Project or Corroder Suits could become unplayable when no compatible holder exists.
Collapsing the whole all-zero domain to `Ok` represents the printed “do as much as possible” rule
without manufacturing a meaningless target choice.
