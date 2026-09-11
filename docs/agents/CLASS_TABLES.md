# Catalog classes and game class views

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing `ClassTable`, Catalog-wide Class identity, a game projection, inhabitation,
> or any API that lets a `Class`/`Type` enumerate game-specific candidates.
>
> **Skip when:** changing parsing or nominal subtyping without projection-dependent
> enumeration; use [type-system-spec.md](../type-system-spec.md).
>
> **Status:** current implementation model, followed by the selected replacement direction and its
> migration plan.

## Source map

- [`ClassTable.kt`](../../src/common/dev/martianzoo/pets/types/ClassTable.kt) — search
  for `public abstract class ClassTable` to inspect master-universe and projection operations.
- [`Class.kt`](../../src/common/dev/martianzoo/pets/types/Class.kt) — read before
  adding any back-reference or universe identity to a structural value.
- [`GamePremise.kt`](../../src/common/dev/martianzoo/pets/data/GamePremise.kt) —
  search for `classTable` to see where the game projection is retained.
- [`ClassTableProjectionTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/ClassTableProjectionTest.kt)
  — read when changing inhabitation or Catalog/Class identity invariants.

## Fast rejection checks

These checks protect the current implementation while it exists. The selected replacement below
intentionally revises its identity and resolution model; do not mix half of each model in one
intermediate state.

Reject a design before implementation if it would:

- give `Class`, `Type`, or a dependency a path back to a game-specific view;
- reconstruct `Class` or `Type` identities while forming a game projection;
- make a structural operation depend on inhabitation without accepting game context explicitly; or
- mutate canonical vocabulary to represent one game's configured players or options.

## Ownership model

A Catalog owns one immutable master type universe. Within that universe there is exactly one
`Class` instance for each known Class Name. `Type` values are likewise structural values from that
master universe.

A game owns a filtered view of the master universe. The view records which catalog-known Classes
are inhabited in that game and owns any indexes that enumerate the inhabited domain. It references
the master Classes; it does not reconstruct them, their Types, their properties, or their nominal
hierarchy.

`Class` and `Type` must not provide a path back to a game-filtered `ClassTable`. They may retain an
unexposed master-universe identity so operations can reject values from different Catalogs, but
that identity is not a source of game context. In particular, removing `Class.classTable` must not
be followed by adding a differently named projection backpointer.

Consequently, inhabitation is not an intrinsic property of a `Class` or `Type`. A game class view
answers whether a Class or Type is inhabited in that game.

## Structural operations versus game-domain operations

Operations whose answers come entirely from authored declarations belong to the master universe:

- nominal subtyping and superclass relationships;
- dependencies, properties, and defaults;
- structural `glb`; and
- expression-to-Type resolution that does not inspect a live World.

Structural `glb` combines constraints in the Catalog universe. It may return a Type that is
uninhabited in a particular game. `null` means that the Catalog defines no compatible Type;
inhabitation is a separate question asked of the game view.

Operations whose answers depend on the selected game must receive that context explicitly:

- inhabited and direct-subclass enumeration;
- concrete-Type enumeration and automatic narrowing;
- deciding whether a Class literal counts or an effect is live;
- validating a Component gain, removal, or transmutation; and
- any operation involving a state-dependent refinement.

The natural context may be the game class view or a `GameReader`; a `Class` or `Type` must not find
it by reverse navigation.

## Identity and integrity

Classes and Types from different master universes are incomparable. Values from two games using
the same master universe are structurally comparable, even when their inhabited domains differ.

World mutation therefore validates both that an incoming Type belongs to the World's master
universe and that the Type is inhabited in that World's view. Projection identity must not stand in
for either check.

Unknown and uninhabited are distinct. A Catalog-known uninhabited Class resolves and keeps its
nominal relationships, but the game view gives it an empty domain. An unknown Class Name is an
error.

## Projection shape

A game projection contains only game-relative information, such as:

- the inhabited Class set;
- selected Modules and premise validation results; and
- any filtered indexes whose contents vary with that set.

It does not contain projection-local copies of master `Class` objects. `findClass` and `resolve`
delegate to the master universe, while `allClasses`, `allClassNames`, and the explicit enumeration
operations filter through the view's inhabited-name set.

The projection computes the premise's monotone activation closure but freezing it performs no Class
construction or nominal-hierarchy compilation. Master compilation performs those tasks once for the
Catalog.

## Access interface

Game runtime code receives the filtered table from `World.classTable`; it must not recover the
master through `GameReader.catalog`. Production master-table acquisition is concentrated at three
structural constraints:

- `TfmCatalog` compiles configuration and Module selection against its private `universe` handle;
- `ClassTable.forPremise` acquires the Catalog universe once to construct a filtered view; and
- canonical language metadata uses one module-private `canonClassUniverse` handle.

`Catalog.classTable` remains public even though game clients have no legitimate reason
to use it. That is an API-access gap, not permission for additional callers.

## Integrity requirements

- no `Class`, `Type`, or dependency value exposes or retains a game-projection backpointer;
- creating a game projection constructs no `Class` instances;
- all game-relative enumeration and inhabitation checks receive an explicit view or reader;
- structural operations give the same answer in every game using one master universe;
- target-World validation prevents an uninhabited Type from entering that World.

## Selected replacement: master tables, premise tables, and class universes

The current projection model above is to be replaced. Its useful observation remains: expensive
declaration-derived knowledge is common to many premises, while each game changes the type domain
only around that stable core.

- Canon has one immutable `MasterClassTable`; Canon plus Fakes has a separate immutable
  `MasterClassTable`. Sharing implementation objects between those two masters is not a goal.
- A `PremiseClassTable` contains only the generated `Premise`, configured Players, and ad-hoc test
  or custom-card declarations. It imports from exactly one master; the master cannot import from it,
  and its names cannot collide with master names.
- A `ClassUniverse` combines one master and one premise table, applies the premise's exclusions, and
  is the complete authority for hierarchy, comparison, resolution, and enumeration in one World.
- A Class cannot report its subclasses: a master can report the subclasses its own closed table
  knows, while only the combined universe can report every subclass relevant to the game.
- Classes from unrelated masters remain incomparable. Comparisons involving a premise declaration
  and its backing master must go through their shared universe.

The reusable boundary must contain supertypes, dependencies, properties, defaults, invariants,
effects, and every other fact determined solely by the backing declarations. Whether this is a
refactored master `Class` or a separate compiled definition behind a light universe Class is not yet
settled. Do not introduce two representations merely to decide whether a nominal Class object is
available for an unrealized name; the semantic model below does not depend on that choice.

### Realized and unrealized abstract Types

An abstract Class has an abstract Type. In a particular universe, that Type is **unrealized** when
it has no concrete narrowing there. “Jackalope Type” is only the informal example of this ordinary
case, not a separate type-system kind.

A master declaration that is concrete in the master can resolve as an unrealized abstract Type in
a universe that excludes it. Semantically it behaves like an abstract Class with no concrete
subclasses in that universe, while the backing declaration remains concrete and final. A Type whose
dependency is unrealized is likewise unrealized. These are structural, premise-fixed facts; a
realized Type does not become unrealized merely because the live World currently contains zero
matching Components or has no remaining capacity for another one.

All unrealized abstract Types share these rules:

- they remain distinct from an unknown name, which is an error;
- concrete enumeration and automatic narrowing produce no candidates;
- their Components, behavior, and triggered effects cannot occur;
- their counts, and the counts of their `Class<T>` literals, are zero;
- optional and AMAP changes to them are `Ok`, while mandatory changes reach `Die`; and
- nominal information may remain available for validation, subtyping, `glb`, `NOT`, and useful
  diagnostics even though the game has no concrete realization.

Counting `Class<Unrealized>` as zero establishes that there is no concrete Class representative. It
does not by itself decide whether the universe retains a nominal `Class` object for lookup and
structural reasoning. Settle that representation only after the operation inventory shows which
choice leaves one coherent source of truth.

An authored abstract Class with no realizable concrete specialization is the same unrealized case.
First verify whether premise closure already prevents such otherwise-unused abstract Classes from
mattering in current catalogs. If so, cover the invariant with tests instead of adding special
pruning machinery.

### `Die` and `Ok`

`Die` should be the canonical intentionally unrealized abstract Type. Its current concrete
declaration with `HAS MAX 0 This` is an implementation technique, not selected semantics. Once the
universe model supplies the general rule, `Die!` fails because `Die` has no concrete narrowing, and
nonmandatory `Die` changes follow the ordinary unrealized-Type rule. Every completed universe must
verify that no premise or catalog declaration gives `Die` a realizable subclass.

`Ok` is the complementary identity instruction: it denotes no change and therefore produces no
event that an effect could observe. A declaration with an `Ok:` trigger is invalid and must be
rejected rather than retained as an effect that can never fire. Preserve these paired integrity
rules together: no realizable subtype of `Die`, and no trigger on `Ok`.

## Migration plan

Do these in order; keep the current implementation and the selected semantics clearly separated
until the replacement is complete.

1. **Specify the semantic boundary.** Update the type-system specification and glossary to define
   masters, premise tables, universes, unrealized abstract Types, unknown names, comparison
   identity, class literals, and the universe-relative meaning of `NOT` and `glb`.
2. **Pin the new contracts with tests.** Cover master/premise lookup, name collisions, one-way
   references, cross-master rejection, excluded and dependency-unrealized Types, zero class-literal
   counts, hierarchy answers that include premise declarations, unrealized `Die`, and forbidden
   `Ok:` triggers.
3. **Inventory context-free operations.** Find every `Class` or `Type` operation that currently
   reaches `classTable`. Move subclass enumeration, unrelated `glb`, structural overlap, concrete
   narrowing, and their caches behind an explicit universe before changing representation.
4. **Establish the reusable compilation boundary.** Keep only facts unaffected by premise additions
   or exclusion in the master. Use the existing `Class` if it can own those facts honestly;
   otherwise extract one compiled definition without duplicating them. Ensure failed compilation
   cannot partially populate the reusable result.
5. **Introduce `ClassUniverse` behavior-preservingly.** Initially build it from today's complete
   catalog so engine callers can migrate from `ClassTable` without simultaneously changing
   realization semantics.
6. **Introduce `PremiseClassTable`.** Compile its small declaration delta against imported master
   schemas, resolve overlay references through the combined namespace, and prohibit master-to-
   premise references and duplicate names.
7. **Switch from activity to realization.** Resolve excluded master-known names as unrealized
   abstract Types, make all affected enumeration and class-literal behavior follow from that fact,
   and remove `isActive`, `findActiveClass`, and every API or comment describing an uninhabited
   Class. Decide at this point, from the simplified call sites, whether unrealized names need nominal
   Class objects.
8. **Move premise variation to the delta.** Stop composing new `TfmCatalog`s for Players and the
   generated `Premise`; remove the conventional-player catalog cache after all callers use premise
   definitions.
9. **Make `Die` and `Ok` ordinary consequences.** Replace `Die`'s concrete zero-limit encoding with
   the selected unrealized abstract semantics, reject a realizable `Die` subtype or an `Ok:` trigger,
   and remove special runtime branches only where the general rules now give the same result.
10. **Split expensive derived work.** Precompile master restriction and dependency-validation
   templates once. Let each universe merge premise deltas, apply its Class set, and perform only the
   validation whose answer can vary by premise.
11. **Migrate the runtime.** Build class representatives only for realized concrete Classes, reject
    unrealized component mutations at the boundary, and bind elaboration, transformations,
    component limits, and automatic narrowing to the universe.
12. **Delete the projection model.** Remove master/projection identity aliases, active-name masks,
    projection loaders, obsolete caches, and the superseded current-model documentation together so
    only one ontology remains.
13. **Verify reuse and savings.** Assert that repeated premises share the same master compiled
    definitions while sharing no mutable universe state. Re-run the focused card setup profiles and
    full JVM-suite category timings; retain no global cache keyed by premise shape.
