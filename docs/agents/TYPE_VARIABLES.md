# Type-variable follow-ups

> **Read when:** implementing one of the remaining implicit-variable defects after first reading the
> relevant part of [TYPES.md](TYPES.md#10-authored-type-variables).
>
> **Skip when:** using existing implicit variables, changing dependencies, or considering
> Splice's generated watchers. Priority lives in [`TODO.md`](../../TODO.md).
>
> **Status:** implementation checklist for the captured-Type design specified in `TYPES.md` section
> 10.

## Source map

- [`TypeVariableScope.kt`](../../src/common/dev/martianzoo/pets/types/TypeVariableScope.kt) and
  [`inferTypeVariables.kt`](../../src/common/dev/martianzoo/pets/types/inferTypeVariables.kt) —
  inspect recognition, declaration sites, and use sites.
- [`Type.kt`](../../src/common/dev/martianzoo/pets/types/Type.kt) — inspect structural
  substitution and narrowing when the selected defect reaches resolved Types.
- [`TypeVariableTest.kt`](../../test/common/dev/martianzoo/pets/types/TypeVariableTest.kt) — read
  before changing the public inspection and binding API.

The occurrence, scope, common `Type`, and Ground-Type capture APIs are present. Specialization binds
only recorded occurrences; the broad textual and Class-name matchers have been removed.

## Scope limits

The generated `SpliceTacticalGenomicsWatcher<Player>` components used by Splice are working regression
constraints, not unfinished Type-variable infrastructure. Replacing them would be optional data and
task-assignment cleanup.

Complement narrowing and preservation have defects beyond variable recognition. Do not solve them
incidentally here. Settle the Complement domain/difference-Type model described in
[TYPES.md](TYPES.md#7-complement-bounds) and `TODO.md` before finalizing complemented variable
behavior.

## Remaining work

1. Evaluate a refined variable bound once when capturing its Ground Type, then reuse that capture
   without rerunning the Requirement.
2. Decide whether dependent captures need a variable-valued bound in the public model. The current
   ordered binding correctly specializes a Complement from its visible positive variable before
   capturing the complete expression, while `TypeVariable.bound` remains a `GroundType`.

## Direction

Use one authored-expression recognizer with explicit scope rules. Recognition occurs before
defaults and lowering, compares parsed authored Expressions exactly, and records occurrence paths.
Whitespace and parser-erased grouping are immaterial; defaults, resolved equivalence, and reordered
arguments do not change authored identity. Class-header specialization then narrows only those paths
and rejects inconsistent bindings.

Keep a class's separately declared dependency roots and sibling argument branches independent.
Choose maximal repeated abstract Expressions so a repeated `Card<Owner>` is one card choice rather
than an accidental variable over only `Owner`.

Class-header equality and Effect specialization now share the recorded header-variable model.

## Regression constraints

Preserve observable behavior for:

- Class headers such as Cardbound, Trade, and Cathedral;
- Effect specialization in Production, PlayCard, PlayedEvent, PaymentMechanic, Colony, and
  DelayedColonyTile;
- Manutech, Viral Enhancers, colony trading, Splice, Trade Envoys, and Trading Colony;
- action, transmutation, and `THEN` cases such as Flooding, Utopia Invest, Kaguya Tech, and Cyberia;
  and
- independent Adjacency branches, Market Manipulation operands, comma siblings, and `OR` arms.

Add a focused negative behavioral test for each remaining divergence before removing its old
implementation. This note is complete when the Type-variable entries in
[TYPES.md](TYPES.md#12-known-divergences) are gone.
