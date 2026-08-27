# Implicit type-variable follow-ups

> **Read when:** implementing one of the remaining implicit-variable defects after first reading the
> relevant part of [TYPES.md](TYPES.md#10-implicit-type-variables).
>
> **Skip when:** using existing implicit variables, changing dependencies, or considering
> Splice's generated watchers. Priority lives in [`TODO.md`](../../TODO.md).
>
> **Status:** focused audit; the target changes are not implemented.

## Source map

- [`TypeLinking.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/TypeLinking.kt) — search for
  `link` and `Region` only after selecting a specific divergence.
- [`Type.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/types/Type.kt) — inspect structural
  substitution and narrowing when the selected defect reaches resolved Types.
- [`DependencyLinkTest.kt`](../../engine/src/commonTest/kotlin/dev/martianzoo/engine/DependencyLinkTest.kt)
  — read before changing effect/task preservation of linked variables.

The runtime already preserves authored variables through effects, actions, task narrowing and
splitting, atomic transmutation, and incremental `THEN`. Do not replace working card mechanisms
just to begin this project.

## Scope limits

The generated `SpliceTacticalGenomicsWatcher<Player>` components used by Splice are working regression
constraints, not unfinished Type-variable infrastructure. Replacing them would be optional data and
task-assignment cleanup.

Complement narrowing and preservation have defects beyond variable recognition. Do not solve them
incidentally here. Settle the Complement domain/difference-Type model described in
[TYPES.md](TYPES.md#7-complement-bounds) and `TODO.md` before finalizing complemented variable
behavior.

## Remaining work

1. Stop nested sibling dependency branches from sharing a variable.
2. Make Class headers recognize the same maximal authored Expressions as runtime scopes, including
   arguments and refinements.
3. Propagate Class-header variables into Effects by exact authored occurrence path instead of global
   Class-Name substitution.
4. Associate explicit arguments with dependency keys before matching so unambiguous argument order
   does not matter.
5. Integrate complemented Expressions only after the complement domain/difference-type model is
   settled.

## Direction

Use one authored-expression recognizer with explicit region rules. Recognition occurs before
defaults and lowering, preserves omission/refinement/complement syntax, canonicalizes only
unambiguous dependency positions, and records occurrence paths. Class-header specialization then
narrows only those paths and rejects inconsistent bindings.

Keep a class's separately declared dependency roots and sibling argument branches independent.
Choose maximal repeated abstract Expressions so a repeated `Card<Owner>` is one card choice rather
than an accidental variable over only `Owner`.

Delete the old bare-name header matcher and broad header-to-Effect substitution only after behavior
matches.

## Regression constraints

Preserve observable behavior for:

- Class headers such as Cardbound, Trade, and Cathedral;
- Effect specialization in Production, PlayCard, PlayedEvent, PaymentMechanic, Colony, and
  DelayedColonyTile;
- Manutech, Viral Enhancers, colony trading, Splice, Trade Envoys, and Trading Colony;
- action, transmutation, and `THEN` cases such as Flooding, Utopia Invest, Kaguya Tech, and Cyberia;
  and
- independent Adjacency branches, Market Manipulation operands, comma siblings, and `OR` arms.

Add a focused negative behavioral test for each divergence before removing its old implementation.
This note is complete when the relevant divergence list in [TYPES.md](TYPES.md#12-known-divergences)
is empty.
