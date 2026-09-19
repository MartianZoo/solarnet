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
> **Status:** selected replacement in progress. The reusable master and premise-local declaration
> delta, table-relative subclass enumeration, general Type-inhabitance query, public activity-API
> removal, runtime inhabitance boundaries, and the `Die`/`Ok` terminal invariants are implemented.
> The exact remaining work and its first decision gate are under
> [Remaining work to finish](#remaining-work-to-finish). Do not describe this program as bounded
> until that gate has shown whether a separate runtime representation deletes more complexity than
> it adds.

## Source map

- [`ClassTable.kt`](../../src/common/dev/martianzoo/pets/types/ClassTable.kt) — search
  for `public abstract class ClassTable` to inspect master-universe and projection operations.
- [`Class.kt`](../../src/common/dev/martianzoo/pets/types/Class.kt) — read before
  adding any back-reference or universe identity to a structural value.
- [`GamePremise.kt`](../../src/common/dev/martianzoo/pets/data/GamePremise.kt) —
  search for `classTable` to see where the game projection is retained.
- [`ClassTableProjectionTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/ClassTableProjectionTest.kt)
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
universe and that the Type is inhabited in that World's view. Projection identity must not stand in
for either check.

Unknown and uninhabited are distinct. A known Class resolves and keeps its nominal relationships
even when its base Type is uninhabited in this game view. An unknown Class Name is an error.

## Projection shape

A game projection contains:

- its premise-local Classes and Types;
- the premise-selected Class set and its derived inhabitance answers;
- selected Modules and premise validation results; and
- any filtered indexes whose contents vary with that set.

It does not contain projection-local copies of master `Class` objects. `findClass` and `resolve`
delegate master-only cases, while combined expressions and enumeration include the premise delta.

The projection constructs and freezes its complete structural universe, including every premise
Class, before computing the premise's monotone inclusion closure. The closure can therefore use
table-relative `glb` and concrete-domain enumeration without relaxing the frozen-table boundary.
Only the included-name set and caches derived from it grow during this internal phase; the completed
projection is immutable when returned. Master compilation performs all reusable construction and
hierarchy compilation once for the Catalog.

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

- no master declaration or master compiled value depends on a premise table;
- creating a game projection constructs only premise-local `Class` instances;
- a mixed master/premise `Type` retains the combined table needed to interpret it;
- sibling premise values and unrelated master values are rejected at comparison boundaries;
- all game-relative enumeration and inhabitation checks receive an explicit view or reader;
- master-only resolution reuses the identical master objects in every game;
- target-World validation prevents an uninhabited Type from entering that World.

## Selected replacement: master tables, premise tables, and class universes

The reusable master and premise-delta boundary is now present. `ClassTable` still serves both the
combined-universe and premise-view roles while the later universe separation is unfinished.
Expensive declaration-derived knowledge is common to many premises, while each game changes the
type domain only around that stable core.

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
effects, and every other fact determined solely by the backing declarations. The current design
retains one nominal Class object for every known master name, even when its base Type is uninhabited
in a particular universe. Do not introduce a second representation for that distinction.

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

The semantic migration is substantially complete:

- [`type-system-spec.md`](../type-system-spec.md#1-universes-and-identity) and
  [its inhabitance section](../type-system-spec.md#12-inhabitance) define master identity,
  premise-local declarations, game universes, unknown versus uninhabited Types, comparison, and
  view-relative enumeration.
- `PremiseClassTable` is the small declaration delta; games reuse their Catalog's master `Class` and
  `Type` objects rather than recompiling them.
- Public activity queries are gone. Inhabitance controls enumeration, class literals, effects, and
  mutation admission, while excluded known names retain their nominal meaning.
- Runtime component limits, elaboration, transformation, and narrowing use the game table, and
  unrelated masters and sibling premise universes are rejected.
- The specification tests cover the identity and inhabitance contract, and the measured premise-
  delta change produced the intended large setup-speed improvement.

This foundation is not to be rebuilt while finishing the program.

## Remaining work to finish

Do these in order. The first item is a decision gate; do not begin a representation split before it
answers whether that split reduces the permanent model.

1. **Audit the remaining roles and callers.** Inventory `Catalog.classTable`,
   `GamePremise.premiseClassTable`, `GamePremise.classTable`, `ClassTable.masterTable`, and every
   game-sensitive operation reached through `Class.classTable` or `Type.classTable`. Classify each
   use as one of:
   - reusable master structure and compiled facts;
   - premise-local declaration compilation;
   - one game's complete structural namespace; or
   - one game's selected, inhabited enumeration view.

   Record which answers can differ between the last two roles. The audit is complete only when no
   caller is classified merely as “needs a `ClassTable`.”

2. **Choose the smallest honest API from that audit.** The required semantic result is one reusable
   master, one premise declaration delta, and one explicit game-relative authority for every answer
   affected by premise Classes or selection. A Kotlin type named `MasterClassTable` or
   `ClassUniverse` is not itself a requirement. Introduce role-specific interfaces or concrete
   types only if they delete dual-role conditionals, unsafe access, or duplicated caches; otherwise
   keep one implementation and make the roles explicit in its API and names. Do not retain both an
   old and new public path.

3. **Close the access gap.** `Catalog.classTable` is currently public even though normal game
   clients need the game view. Narrow master acquisition to Catalog construction, premise
   construction, and canonical metadata. Expose the premise declaration delta only to code that
   compiles the game universe. Runtime and client code must receive its game-relative authority
   directly, never recover the master through a Catalog.

4. **Remove superseded projection machinery and vocabulary.** Replace or delete
   `ClassLoader.projection`, `masterSource`/`masterTable` identity conditionals, mutable
   `includedClassNames`/inclusion-cache invalidation, and `activation` terminology to the extent the
   selected API makes them obsolete. Premise construction may still compute a monotone declaration
   closure; that policy is not a second type ontology and should be named as premise selection, not
   runtime activity. Rename projection-oriented tests and documentation with the implementation.

5. **Finish the reusable derived-work boundary.** Keep master-only hierarchy, restrictions,
   dependency validation, properties, defaults, invariants, and effects compiled once. A game may
   merge premise declarations, apply its selected Class set, and calculate only answers that can
   vary by premise. Remove obsolete caches rather than adding a global cache keyed by premise shape.

6. **Verify the final surface and delete the transition.** The focused specification and projection
   suites must still prove shared master identity, premise-only Class construction, sibling-universe
   rejection, uninhabited behavior, and runtime mutation rejection. Add a direct assertion that
   repeated premises share master compiled objects but no mutable game-view state. Run the complete
   JVM suite and repeat the focused setup measurement from
   [`JVM_TEST_PERFORMANCE.md`](JVM_TEST_PERFORMANCE.md#2026-09-12-premise-delta-reuse-result). Delete
   superseded APIs, aliases, caches, tests, and documentation in the same program.

### Completion criteria

The Class-universe model is finished when:

- the four roles in the audit have explicit owners and no API silently alternates between them;
- every game-relative operation receives one compatible game authority explicitly;
- master declarations and compiled facts are constructed once and never depend on a premise;
- a game constructs only premise-local Classes and retains no mutable state shared with another
  game;
- normal runtime and client code cannot accidentally use the unfiltered master where a game view is
  required;
- the implementation and documentation no longer describe the same object alternately as a master,
  projection, activation view, and universe; and
- the identity, inhabitance, runtime-boundary, full-suite, and reuse/performance checks above pass.

### Not required for completion

- Do not add `MasterClassTable` or `ClassUniverse` merely to make the implementation nouns match
  this document; role clarity and net deletion decide the representation.
- Do not remove a `Class` or `Type` link to its master identity when that link serves only reusable
  structural meaning. The forbidden shortcut is recovering one game's selected domain implicitly.
- Do not serialize or clone master declarations, support unrelated Catalogs in one universe, add a
  global premise cache, or optimize beyond preserving the demonstrated reuse benefit.
