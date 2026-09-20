# Catalog classes and game class views

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing `ClassTable`, Catalog-wide Class identity, a game view, inhabitation,
> or any API that lets a `Class`/`Type` enumerate game-specific candidates.
>
> **Skip when:** changing parsing or nominal subtyping without game-dependent
> enumeration; use [type-system-spec.md](../type-system-spec.md).
>
> **Status:** complete. Catalog structure is compiled once and reused, premise declarations are a
> small delta, and every operation whose answer varies by game receives an explicit `ClassTable`.
> `Catalog.classTable` intentionally remains public for whole-Catalog clients; gameplay receives
> its table from `GamePremise` or `GameReader`.

## Source map

- [`ClassTable.kt`](../../src/common/dev/martianzoo/pets/types/ClassTable.kt) — search
  for `public abstract class ClassTable` to inspect Catalog-wide and game-view operations.
- [`Class.kt`](../../src/common/dev/martianzoo/pets/types/Class.kt) — read before
  adding any back-reference or universe identity to a structural value.
- [`GamePremise.kt`](../../src/common/dev/martianzoo/pets/data/GamePremise.kt) —
  search for `classTable` to see where the game view is retained.
- [`ClassTableSelectionTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/ClassTableSelectionTest.kt)
  — read when changing inhabitation or Catalog/Class identity invariants.
- [`Spec12InhabitanceTest.kt`](../../test/common/dev/martianzoo/pets/types/Spec12InhabitanceTest.kt)
  — the normative master/view identity, enumeration, and inhabitance scenarios.
- [`JVM_TEST_PERFORMANCE.md`](JVM_TEST_PERFORMANCE.md#2026-09-12-premise-delta-reuse-result) — the
  measured reuse result to preserve and the baseline for final verification.

## Fast rejection checks

Reject a design before implementation if it would:

- reconstruct master `Class` or `Type` identities while forming a game universe;
- let the master refer to a premise-local declaration;
- compare values from two distinct premise universes merely because their names match;
- make a structural operation depend on premise inclusion without accepting game context; or
- mutate canonical vocabulary to represent one game's configured players or options.

## Ownership model

A Catalog owns one immutable master class table with exactly one `Class` per reusable declaration.
A `GamePremise` owns a `PremiseClassTable` containing only its generated Players, generated
`Premise`, and ad-hoc declarations. That table imports exactly one master and rejects name
collisions; the master is compiled without knowledge of the delta.

A game's combined `ClassTable` reuses every master `Class` and constructs only the premise delta.
It also owns the included-name set and indexes used for game-relative enumeration. A master-only
expression normally delegates resolution to the master. A type mentioning a premise class, or a
`NOT` whose structural overlap can be affected by premise subclasses, is scoped to the combined
game table so nested resolution cannot fall back to the wrong namespace.

Premise inclusion remains a property of the combined game table, not of a master `Class` or `Type`.

Stable interpretations of component-limit invariants are compiled with the master Classes that
declare them. A combined game table realizes those templates against its inhabited Class set and
adds premise-local declarations. Dependency targets remain view-relative and are enumerated in the
combined table. It does not rebuild or retain a completed premise table by configuration shape.

## Structural operations versus game-domain operations

Intrinsic facts whose answers come entirely from reusable declarations belong to the master:

- nominal subtyping and superclass relationships;
- dependencies, properties, and defaults;
- expression-to-Type resolution that does not inspect a live World.

Downward structural questions ask an explicit table. `ClassTable.glb` and structural overlap use
every master and premise class in that table, regardless of premise inclusion; asking the master
and asking a combined game table may therefore produce different answers for the same master
operands.

Operations whose answers depend on the selected game must receive that context explicitly:

- inhabited and direct-subclass enumeration;
- concrete-Type enumeration and automatic narrowing;
- deciding whether a Class literal counts or an effect is live;
- validating a Component gain, removal, or transmutation; and
- any operation involving a state-dependent refinement.

The natural context may be the game class view or a `GameReader`; a `Class` or `Type` must not find
it by reverse navigation.

## Identity and integrity

Classes and Types from different masters are incomparable. A master value and a premise value are
comparable through that premise's combined table. Premise values from two sibling games are
incomparable even when they have the same written declaration.

World mutation therefore validates both that an incoming Type belongs to the World's master
universe and that the Type is inhabited in that World's view. Table identity must not stand in
for either check.

Unknown and uninhabited are distinct. A known Class resolves and keeps its nominal relationships
even when its base Type is uninhabited in this game view. An unknown Class Name is an error.

## Game-view shape

A game view contains:

- its premise-local Classes and Types;
- the premise-selected Class set and its derived inhabitance answers;
- selected Modules and premise validation results; and
- any filtered indexes whose contents vary with that set.

It does not contain game-local copies of master `Class` objects. `findClass` and `resolve`
delegate master-only cases, while combined expressions and enumeration include the premise delta.

The game table constructs and freezes its complete structural namespace, including every premise
Class, before computing the premise's monotone inclusion closure. The closure can therefore use
table-relative `glb` and concrete-domain enumeration without relaxing the frozen-table boundary.
Only the included-name set and caches derived from it grow during this internal phase; the completed
game view is immutable when returned. Master compilation performs all reusable construction and
hierarchy compilation once for the Catalog.

## Access interface

Game runtime code receives the selected table from `World.classTable`; it must not recover the
master through `GameReader.catalog`. `Catalog.classTable` is intentionally public for clients whose
work spans the complete Catalog, including validation, metadata, reporting, and the class viewer.
Production master-table use is concentrated at three structural constraints:

- `TfmCatalog` compiles configuration and Module selection against its private `universe` handle;
- internal premise construction acquires the Catalog table once to construct a game view; and
- canonical language metadata and the class viewer deliberately use `Catalog.classTable`.

The public access is not game authority: callers asking an inhabited-domain question must choose
the game table explicitly.

## Integrity requirements

- no master declaration or master compiled value depends on a premise table;
- creating a game view constructs only premise-local `Class` instances;
- a mixed master/premise `Type` retains the combined table needed to interpret it;
- sibling premise values and unrelated master values are rejected at comparison boundaries;
- all game-relative enumeration and inhabitation checks receive an explicit view or reader;
- master-only resolution reuses the identical master objects in every game;
- target-World validation prevents an uninhabited Type from entering that World.

## Representation choice

No role-specific table types are needed. One `ClassLoader` implementation serves a Catalog table
and each game table, while the receiver chosen by the caller supplies the authority. Expensive
declaration-derived knowledge is common to many premises; each game constructs only its premise
Classes and its selected-domain indexes.

- Each Catalog has one immutable `classTable`. A differently composed Catalog has a different one.
- `PremiseClassTable` contains only generated Players, the generated `Premise`, and ad-hoc
  declarations. It imports exactly one Catalog table and rejects name collisions.
- `GamePremise.classTable` combines those declarations with shared Catalog objects and is the one
  authority for structural questions involving premise Classes and all game-relative enumeration.
- A `Class` cannot enumerate subclasses by itself; the caller chooses the Catalog or game table.
- Classes from unrelated Catalogs and premise Classes from sibling games remain incomparable.

The reusable Catalog boundary contains supertypes, dependencies, properties, defaults, invariants,
effects, and every fact determined solely by Catalog declarations. It retains one nominal `Class`
for every known Catalog name even when its base Type is uninhabited in a game. Do not introduce a
second representation for that distinction.

### Inhabited and uninhabited Types

A Type is **inhabited** in a particular universe when it has at least one concrete narrowing there;
otherwise it is **uninhabited**. “Jackalope Type” is only the informal example of this ordinary
case, not a separate type-system kind. This broad term applies equally to excluded concrete Classes,
abstract Types with no concrete specialization, empty structural differences, and Types whose
dependencies are uninhabited.

A master declaration that is concrete in the master can have an uninhabited base Type in
a universe that excludes it. Semantically it behaves like an abstract Class with no concrete
subclasses in that universe, while the backing declaration remains concrete and final. A Type whose
dependency is uninhabited is likewise uninhabited. These are structural, premise-fixed facts; an
inhabited Type does not become uninhabited merely because the live World currently contains zero
matching Components or has no remaining capacity for another one.

All uninhabited Types share these rules:

- they remain distinct from an unknown name, which is an error;
- concrete enumeration and automatic narrowing produce no candidates;
- their Components, behavior, and triggered effects cannot occur;
- their counts, and the counts of their `Class<T>` literals, are zero;
- optional and AMAP changes to them are `Ok`, while mandatory changes reach `Die`; and
- nominal information may remain available for validation, subtyping, `glb`, `NOT`, and useful
  diagnostics even though the game has no concrete realization.

Counting `Class<Uninhabited>` as zero establishes that there is no concrete Class representative.
The universe nevertheless retains the nominal `Class` object for lookup and structural reasoning;
that is the same reusable object as in the master when the declaration is master-owned.

An authored abstract Class with no realizable concrete specialization is the same uninhabited case.
First verify whether premise closure already prevents such otherwise-unused abstract Classes from
mattering in current catalogs. If so, cover the invariant with tests instead of adding special
pruning machinery.

### `Die` and `Ok`

`Die` is concrete and therefore final, but `HAS MAX 0 This` gives it zero component capacity in
every World. That differs honestly from a structurally uninhabited Type, which has no concrete
narrowing in a particular universe. The engine may derive the same terminal result from either
fact: a mandatory gain cannot execute, while a nonmandatory gain resolves to no change. `Die`
retains named task normalization because it is the canonical impossible instruction and can be
recognized before World-relative resolution.

`Ok` is the complementary identity instruction: it denotes no change and therefore produces no
event that an effect could observe. A subscribed trigger rooted at `Ok` or any nominal supertype of
`Ok` is invalid, even when a refinement excludes `Ok`; those subscriptions are too broad to be
useful. Self triggers remain ordinary; the gain of `Ok` to which one could react is canonicalized to
no change.

## Completion ledger

The semantic migration is complete:

- [`type-system-spec.md`](../type-system-spec.md#1-universes-and-identity) and
  [its inhabitance section](../type-system-spec.md#12-inhabitance) define master identity,
  premise-local declarations, game universes, unknown versus uninhabited Types, comparison, and
  view-relative enumeration.
- `PremiseClassTable` is the small declaration delta; games reuse their Catalog's master `Class` and
  `Type` objects rather than recompiling them.
- Public activity queries are gone. Inhabitance controls enumeration, class literals, effects, and
  mutation admission, while excluded known names retain their nominal meaning.
- Component-limit templates are compiled with their master declarations. Runtime dependency-target
  validation, limits, elaboration, transformation, and narrowing use the game table, and unrelated
  masters and sibling premise universes are rejected.
- Parameterless enumeration and automatic narrowing are absent from `Type`, `GroundType`, `Class`,
  and `DependencySet`; callers name the Catalog or game table explicitly.
- `GamePremise.premiseClassTable` and its private game-table construction are internal details.
- Tests and implementation name premise policy as selection and the resulting table as a game view.
- The specification tests cover the identity and inhabitance contract, and the measured premise-
  delta change produced the intended large setup-speed improvement.

## Role-and-caller audit

The audit found four real roles. `ClassTable` and its sole implementation, `ClassLoader`,
deliberately own both game roles; explicit receiver choice and separate structural/internal versus
inhabited/public operations keep those roles distinct without another representation.

| Role | Current owner | APIs and compiled work | Callers |
| --- | --- | --- | --- |
| Reusable master structure and compiled facts | `Catalog.classTable`; a master `ClassLoader`; master `Class` and `Type` objects | Master declaration lookup and resolution; upward hierarchy and nominal subtyping; dependencies, properties, defaults, invariants, effects, and base/default/class Types; master downward indexes; transform/custom-Class metadata | `TfmCatalog` validation, configuration, cards, and colony metadata; the full Catalog class viewer; `GamePremise` construction; test Catalogs and type-specification tests |
| Premise-local declaration compilation | Internal `GamePremise.premiseClassTable` and `PremiseClassTable` | Premise declaration ownership, collision checks, and name-level subtyping used before premise Classes are compiled | `GamePremise` validation; `ClassSelection.appliesTo`; internal game-table construction; `TfmCatalog` configuration before a premise exists |
| One game's complete structural namespace | The game `ClassLoader`, its `masterTable`, every premise `Class`, and combined `GroundType.resolutionTable` values | `findClass`/`getClass`, `resolve`, `checkAllTypes`, `knows`, `commonTable`/`accepts`, every `glb`, structural subclass and concrete-Type enumeration, `NOT` overlap, and constraint interpretation | Premise-Class compilation and validation; `Class`, `GroundType`, `DependencySet`, defaults, and type-variable operations; `PetElaborator`; engine effect and instruction interpretation; event-log decoding and diagnostics |
| One game's selected, inhabited enumeration view | The same game `ClassLoader`, selected-name set, inhabitance/subclass caches, and `ClassLimitTable` | `allClasses`, `allClassNames`, `isIncluded`, `findInhabitedClass`, every `isInhabited` overload, `allInhabitedConcreteClasses`, public subclass enumeration, public concrete-Type enumeration, automatic narrowing, component limits, and view-bound transform handlers | Engine setup, mutation, limiting, narrowing, and effect admission; `ComponentGraph` and `GameReaderImpl`; TfM workflow and `Prod`; scripts, game viewer, reports, and game/specification tests |

### Named-owner inventory

- `Catalog.classTable` owns reusable compiled Catalog structure. Public whole-Catalog clients are
  legitimate: `TfmCatalog` validation and metadata, configuration, reports, the class viewer, and
  type-system tools. Gameplay instead receives `GamePremise.classTable`.
- `GamePremise.premiseClassTable` is exclusively premise-compilation state. No runtime caller needs
  it, so it is internal.
- `GamePremise.classTable` is the runtime authority, but it exposes both the complete structural
  namespace and selected-view answers through the same nominal type.
- `ClassTable.masterTable` is an implementation identity/backing link. `commonTable`, `accepts`,
  subclass combination, and `ClassLoader.forPremise` inspect it; no client uses it directly.
- `Class.classTable` is a structural identity link. Its callers perform compatibility checks,
  master/premise hierarchy combination, declaration-derived resolution and meets, or stable lookup
  of system and TfM metadata. Those callers are `ClassTable`/`ClassLoader`, `Class` and
  `GroundType`, dependency/default compilation, `Component`, and TfM's `cardClass` helpers. It has
  no legitimate selected-view enumeration caller.
- `Type.classTable` is the structural namespace needed for resolution, `NOT`, meets,
  dependency matching, and type-variable binding. Its structural callers are `ClassTable.knows`,
  `GroundType`, `Dependency`/`DependencySet`, `TypeVariable`/`TypeVariableScope`, and the
  no-`GameReader` fallbacks in `Instruction`; runtime instruction paths use `GameReader`'s table
  when one exists. It has no enumeration or automatic-narrowing operation.

`ClassLoader.loadEverything` and master `freeze` construct role 1. `GamePremise` privately bridges
roles 2--4: it freezes the combined namespace, grows the selected declaration closure, and validates
the inhabited result. No client can invoke that bridge directly. Master-only expressions resolved
by a game still reuse the equal master Type; because enumeration is table-owned, that representation
choice cannot silently choose an inhabited domain.

### Answers that differ between the structural namespace and inhabited view

This is the complete difference list. Operations not listed here must remain structural.

1. **Membership:** all known Classes and names versus `allClasses`/`allClassNames` and
   `isIncluded`. Every premise declaration is structurally known; excluded declarations are absent
   from the selected set. Inclusion and inhabitance can also differ for an included abstract empty
   domain.
2. **Downward hierarchy:** structural subclasses and direct subclasses include every master and
   premise Class; public `allSubclasses` and `directSubclasses` retain only selected Classes.
3. **Concrete Class domain:** all structurally concrete Classes versus
   `allInhabitedConcreteClasses`. Class-literal dependencies make this a greatest fixed point, not a
   simple abstract-Class filter.
4. **Type inhabitance:** `findInhabitedClass` and every `isInhabited` overload can reject a known,
   structurally meaningful Class or Type. A `Class<X>` Type can differ because the represented
   Class is uninhabited.
5. **Concrete Type enumeration:** `allConcreteSubtypes`, its caller-supplied-target overload, and
   `concreteSubtypesSameClass` filter both root Classes and recursively enumerated dependency
   targets through the inhabited domain. Their structural counterparts do not.
6. **Automatic narrowing:** `singleConcreteSubtype` can produce a unique result in a selected view
   when the complete namespace has several candidates, or no result when the sole structural
   candidate is uninhabited.
7. **Component limits:** `componentLimits` compiles invariants only from inhabited concrete Classes
   and expands restrictions through selected subclasses. A complete-namespace limit table would
   contain constraints for excluded Classes.
8. **View-bound transforms:** `transformDispatcher` passes its table to handler factories. `Prod`
   derives its resource names with inhabited lookup and selected subclass enumeration, so the
   resulting transformer can differ by view.
9. **Downstream runtime answers:** Class-representative population and counts, effect liveness,
   elaboration admission, mutation/transmutation admission, dependency shards, and automatic task
   choices differ as consequences of items 1--8. These are consumers of the selected view, not
   additional structural operations.

Lookup of a known name, resolution, upward hierarchy, nominal subtyping, `glb`, structural overlap
and `NOT`, properties/defaults/invariants/effects, compatibility, and constraint interpretation do
**not** vary with selection. They can vary between the reusable master namespace and a combined
namespace containing premise Classes, which is a separate axis from inhabitance.

### Gate decision

Completion is a bounded net simplification once whole-Catalog access is recognized as legitimate.
No representation split is required. The finish consists of deleting implicit `Type`/`Class`
enumeration, routing game-relative callers through an explicit table, internalizing the premise
construction bridge, and using selection/game-view terminology. It adds no cache, phase, table
type, or compiled object.

Preserve these final constraints:

- Catalog declarations and compiled facts are constructed once and never depend on a premise.
- A game constructs only premise-local Classes and shares no mutable view state with another game.
- Whole-Catalog clients may use `Catalog.classTable`; gameplay uses its premise or reader table.
- A `Class` or `Type` link serves structural identity only and never chooses an inhabited domain.
- Do not add role-named table types, clone Catalog declarations, or add a global premise cache.
