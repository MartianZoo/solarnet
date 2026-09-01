# Type-variable follow-ups

> **Read when:** implementing one of the remaining implicit-variable defects after first reading the
> relevant part of [TYPES.md](TYPES.md#10-authored-type-variables).
>
> **Skip when:** using existing implicit variables, changing dependencies, or considering
> Splice's generated watchers. Priority lives in [`TODO.md`](../../TODO.md).
>
> **Status:** current captured-Type implementation constraints and its remaining default-expansion
> follow-up.

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

Make expressions inserted by defaults inherit the recorded trigger-variable identity they would
have in the explicitly expanded form. Preserve authored distinctions such as `Player` versus
`Player<>`; default expansion must propagate occurrences, not rerun authored-expression recognition.

Dependent captures do not need variable-valued bounds. Ordered binding first specializes any
visible positive variable inside the dependent expression, then captures the complete expression
under a Ground-Type bound. Keep `TypeVariable.bound` a `GroundType` unless an authored rule proves
that this model is insufficient.

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

Add a focused negative behavioral test before changing the remaining default-expansion behavior.
This note is complete when that entry is gone from `TODO.md`.
