# Catalog classes and game class views

> **Read when:** changing `ClassTable`, Catalog-wide Class identity, a game projection, inhabitation,
> or any API that lets a `Class`/`Type` enumerate game-specific candidates.
>
> **Skip when:** changing ordinary parsing or nominal subtyping without projection-dependent
> enumeration; use [TYPES.md](TYPES.md).
>
> **Status:** current model.

## Source map

- [`ClassTable.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/types/ClassTable.kt) — search
  for `public abstract class ClassTable` to inspect master-universe and projection operations.
- [`Class.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/types/Class.kt) — read before
  adding any back-reference or universe identity to a structural value.
- [`GamePremise.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/data/GamePremise.kt) —
  search for `classTable` to see where the game projection is retained.
- [`ClassTableProjectionTest.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/rules/ClassTableProjectionTest.kt)
  — read when changing inhabitation or Catalog/Class identity invariants.

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
- structural `glb` and `lub`; and
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

Classes and Types from different master universes remain incomparable. Values from two games using
the same master universe are structurally comparable, even when their inhabited domains differ.

World mutation therefore validates both that an incoming Type belongs to the World's master
universe and that the Type is inhabited in that World's view. Projection identity must not stand in
for either check.

Unknown and uninhabited remain distinct. A Catalog-known uninhabited Class resolves and keeps its
nominal relationships, but the game view gives it an empty domain. An unknown Class Name remains an
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

## Access boundary

Game runtime code receives the filtered table from `World.classTable`; it must not recover the
master through `GameReader.catalog`. Production master-table acquisition is concentrated at three
structural boundaries:

- `TfmCatalog` compiles configuration and Module selection against its private `universe` handle;
- `ClassTable.forPremise` acquires the Catalog universe once to construct a filtered view; and
- canonical language metadata uses one module-private `canonClassUniverse` handle.

`Catalog.classTable` remains public even though ordinary game clients have no legitimate reason
to use it. That is an API-boundary gap, not permission for additional callers.

## Integrity requirements

- no `Class`, `Type`, or dependency value exposes or retains a game-projection backpointer;
- creating a game projection constructs no `Class` instances;
- all game-relative enumeration and inhabitation checks receive an explicit view or reader;
- structural operations give the same answer in every game using one master universe;
- target-World validation prevents an uninhabited Type from entering that World.
