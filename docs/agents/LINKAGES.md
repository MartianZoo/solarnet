# Type Variable Follow-ups

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

## Scope

The evergreen type-variable model lives in
[TYPES.md](TYPES.md#10-implicit-type-variables). This plan records only the work still needed to
make Class headers, Class-header-to-Effect propagation, argument canonicalization, and Complements
conform to that model.

The general runtime already preserves authored type variables through effects, actions, task
revision and splitting, atomic transmutation, and incremental `THEN` execution. The working cards
and rules using those paths are regression constraints, not unfinished Type-variable work. In particular,
Splice works through its generated `CardXC03FWatcher<Player>` components; replacing those is optional data
cleanup involving task assignment, not a prerequisite for completing type variables.

## Remaining divergences

The precise current behaviors are cataloged in
[TYPES.md](TYPES.md#12-known-divergences). The type-variable project owns these parts:

1. **Sibling dependency branches link accidentally.** Nested occurrences in separate arguments of
   one `<...>` list can share a variable, including distinct class-literal slots that all use the
   `Class_0` key. Those branches must remain independent.
2. **Class headers recognize only bare Class Names.** A repeated authored bound carrying
   arguments, a refinement, or a complement should link as one maximal expression when abstract.
   The current Class-header-specific mechanism instead reaches inside it.
3. **Class-header-to-Effect propagation substitutes Class Names globally.** It can rewrite Expressions
   the source did not link, and it silently skips a name that maps to conflicting replacements.
   Propagation should target exact authored occurrence paths and reject disagreement.
4. **Equivalent authored argument orders do not match.** Explicit arguments must be associated with
   dependency keys before repetition matching so unambiguous reorderings share a variable.
5. **Complements interact inconsistently with recognition.** A root complement is excluded from
   ordinary matching, but an expression containing a nested complement can still link. Class
   Class headers also need complemented bounds to participate only according to the final Complement
   model.

Complement narrowing and preservation have defects beyond type-variable recognition. Do not solve
them incidentally here. Settle the complement domain/difference-type model described in `TYPES.md`
and `TODO.md` before finalizing complemented variable behavior.

## Implementation direction

1. Extract or extend one authored-expression recognizer shared by runtime scopes and class
   headers. Give each scope explicit region rules rather than inferring scope from Dependency
   keys alone.
2. Canonicalize explicitly written dependency arguments by key before comparing authored trees.
   Preserve omission, refinements, complements, and source structure introduced before defaults or
   lowering.
3. Make Class-header recognition select maximal repeated Abstract Expressions. Keep separately
   declared dependency roots and sibling argument branches independent; preserve the intended link
   only when a class repeats a bound at distinct positions of the same inherited dependency.
4. Retain the recognized occurrence paths from a Class header into its Effects. Narrow only
   those paths when a concrete component specializes the class, resolving every containing
   expression and reporting inconsistent bindings.
5. Remove the old bare-name Class-header matcher and broad Class-header-to-Effect substitution only after
   behavioral parity is established.
6. Integrate complemented expressions after the complement model is settled, then remove any
   temporary exclusions or special cases made obsolete by that model.

## Regression constraints

Preserve all existing behavior while replacing the older mechanisms:

- Class-header cases including `Cardbound`, `Trade`, and `Cathedral`;
- Class-header-to-Effect specialization for `Production`, `PlayCard`, `PlayedEvent`,
  `PaymentMechanic`, `Colony`, and `DelayedColonyTile`;
- runtime trigger cases including Manutech, Viral Enhancers, colony trading, Splice, Trade Envoys,
  and Trading Colony;
- Action, Transmutation, and incremental `THEN` Type-variable behavior, including Flooding, Utopia
  Invest, Kaguya Tech, Cyberia, and the use-card action; and
- independent choices in `Adjacency`, sibling dependency branches, Market Manipulation, comma
  siblings, and `OR` arms.

Add focused negative tests for each remaining divergence before removing its old implementation.
Tests should assert observable narrowing and execution behavior rather than internal occurrence
tables.

## Completion condition

This plan is complete when Class headers use the shared authored-Expression model, Effects
specialize only the exact variables declared by their headers, unambiguous argument order is
canonicalized, complemented variables follow the settled complement semantics, and the known
divergences relevant to type variables have been removed from `TYPES.md`.
