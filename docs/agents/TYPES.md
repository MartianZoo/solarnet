# Pets type system

**Status: current model with explicit divergences in section 12.** This is the implementation-facing
reference for `dev.martianzoo.types`. The human tutorial is
[type-system.md](../type-system.md).

## Quick model

- A Game World contains a multiset of concrete component Types.
- Components have no fields or instance identity. Type plus multiplicity is all state.
- Classes provide nominal subtyping. Concrete Classes cannot have subclasses.
- Type arguments are dependency edges to other unique components, not ordinary generic parameters.
- `Class<X>` names a Class without depending on an X component.
- Class properties record immutable facts about a Class, not state on component occurrences.
- Refinements filter candidates by querying the current World.
- Complements exclude a dependency subdomain.
- Each World has one frozen closed Class Table, allowing concrete enumeration and automatic
  narrowing.
- Repeated authored abstract Expressions can form implicit Type variables inside defined regions.
- Authority-known inactive Classes are phantoms: resolvable but inert.

## 1. Components and Classes

A `CLASS` declaration introduces both a Class and its base Type:

```pets
ABSTRACT CLASS Area {
  ABSTRACT CLASS MarsArea {
    ABSTRACT CLASS LandArea
  }
}
CLASS Tharsis_2_2 : LandArea
```

Nesting is shorthand for naming the enclosing Class as a direct supertype. A component has one exact
concrete Type and every nominal supertype of that Type. Ten plant cubes are ten equal
`Plant<Player1>` components, not one object with a count field.

The graph changes only by gain, removal, and atomic transmutation `A FROM B`. Counts cannot become
negative.

### Owner-local derived Classes

A card definition can declare a component Class at its point of use without choosing its canonical
name explicitly. For example, a card instruction can gain `Mandate { -> 3 ProjectCard }`, or use
`CityTile<RemoteArea {}>`. Card-definition construction lowers these to ordinary declarations with
stable owner-derived names such as `CardB05_Mandate` and `Card021_RemoteArea` before building the
Class Table. They have exactly the existing Class and component semantics; there is no runtime
anonymous identity.

The body follows the base Class name, before any arguments on that use: for example,
`SpecialTile {}<LandArea>`. Those arguments specialize the generated Class occurrence; they do not
narrow its declared superclass.

Use this for a Class local to one definition, especially mandates, temporary effects, special tiles,
and remote areas. A shared Class, a Custom implementation, or a component with several semantic
roles should remain explicit. Multiple local Classes with the same natural suffix must be declared
explicitly rather than distinguished by an ordinal or hash.

Another expression that needs the exact derived Class may use its assigned canonical name. Writing
the unbraced superclass still means the ordinary abstract family; it does not implicitly resolve to
the local subtype. Existing implicit Type-variable rules continue to link repeated abstract
dependencies inside the derived Type.

This syntax is definition-time only. Manually submitted instructions are still parsed and validated,
then rejected with `NoNewClassDeclarationsException` because the live game's Class Table is frozen.

## 2. Nominal subtyping

Classes may have several abstract direct supertypes:

```pets
CLASS OceanTile : Tile<MarsArea>, GlobalParameter
```

Subtyping is reflexive and transitive. A concrete Class may not have any subtype. Except for
`Component`, a root declaration implicitly extends `Component`; spelling that implicit edge is
an error. Direct-supertype order in source does not permit cycles or ambiguous dependency
intersections.

Pets does not manufacture arbitrary structural intersection Classes. If game rules need one named
intersection, declare it explicitly, as `OwnedTile` does for `Owned` and `Tile`.

## 3. Dependencies

```pets
ABSTRACT CLASS Tile<Area>
ABSTRACT CLASS Owned<Anyone>
```

`Tile<Tharsis_2_2>` depends on the exact area component. `Plant<Player1>` depends on
`Player1`. A dependent component cannot exist without its targets; removing a target cascades
through its dependents.

Bounds are covariant:

```text
VolcanicArea <: LandArea
Tile<VolcanicArea> <: Tile<LandArea>
```

A Type is concrete only when its root Class and all dependency bounds are concrete. Bare `Tile`
means its declared bound `Tile<Area>`.

### Inherited and narrowed dependencies

A subtype retains every inherited dependency and its stable dependency key. It may narrow the bound
through a declared supertype:

```pets
ABSTRACT CLASS Tile<Area>
CLASS GreeneryTile : Tile<MarsArea>
```

`GreeneryTile<Tharsis_2_2>` remains a `Tile<Tharsis_2_2>`. The `Area` edge was not copied or
redeclared.

New dependencies follow inherited ones. `Cardbound<CardFront<Player>> : Owned<Player>` retains
`Owned_0` and adds the card dependency. The repeated `Player` links card and owner through an
implicit variable described in section 10.

### Dependency targets must be unique

An edge identifies its target only by exact Type, so every concrete Type admitted by a dependency
bound must have an applicable `MAX 1` or `=1` counting invariant. The engine validates this when
building a game. The Type system itself does not yet encode multiplicity.

### Argument matching and forms

Written arguments match remaining dependencies greedily from left to right, using the first bound
with a non-empty intersection. Unambiguous order therefore resolves alike:

```text
GreeneryTile<Player1, Tharsis_2_3>
GreeneryTile<Tharsis_2_3, Player1>
```

Order remains meaningful when dependency bounds overlap, as in `Adjacency<Tile, Tile>`. An
unmatched extra argument is an error.

A full form states every bound. A minimal form omits bounds equal to the root Class defaults while
retaining placeholders needed for greedy matching to round-trip. Rendering uses minimal form. A
Complement's unwritten domain is the known exception to round-tripping; see section 7 and divergence
12.5.

## 4. Class literals

A dependency asserts that an instance exists. `Class<X>` instead names a Class as data:

```pets
CLASS Production<Class<StandardResource>>
```

`Production<Class<Steel>>` does not require a steel cube. Exactly one `Class<ConcreteClass>`
component exists for each active concrete Class.

The literal accepts one bare Class Name. `Class<Steel<Player1>>` and nested Class literals are
invalid. Class literals are covariant. Their concreteness depends only on the represented Class, not
that Class's dependencies: `Class<CityTile>` is concrete even when bare `CityTile<Area>` is not.

## 5. Defaults

Defaults preserve omitted physical-game context:

```pets
DEFAULT Owned<Owner>
DEFAULT +OceanTile<WaterArea>
DEFAULT -Owed<Class<Megacredit>>.
```

They supply omitted dependency bounds and, for gains/removals, a Quantifier. They change how an
authored Expression resolves, not which Types exist.

All-use, gain, and removal defaults are gathered separately. For one dependency and use kind, only
nearest declaring supertypes survive. Incomparable surviving bounds need one most-general common
narrowing; Quantifiers must agree.

Literal `Owner` in a default stays unresolved until a concrete owned context can bind it. In an
ownerless context it remains the abstract Class.

## 5a. Class properties

Class properties record immutable facts about a Class rather than state on component occurrences.
They narrow through inheritance, can be read without a component instance, and explicitly evaluate
stored Metric or Requirement syntax inside class effects. The complete current model, settled
semantic rulings, and cardinality/default/group directions are in
[PROPERTIES.md](PROPERTIES.md).

## 6. Refinements

`CardFront(HAS 20 cost)` is an abstract subtype filtered by a Requirement. A candidate first
narrows the base Type, then the reader substitutes that candidate into dependency positions in the
Requirement and tests the current World.

Examples:

```text
CardFront(HAS requirement)
LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)
MarsArea(HAS MapBonus<Class<Metal>>)
```

If no dependency position accepts the candidate, the refinement fails. Satisfying a refinement is a
state-aware relation, not static nominal subtyping.

### Static operations

A refinement is statically below its unrefined base. Narrowing the base while preserving the exact
Requirement narrows the refinement. Different Requirements do not imply one another statically.

The greatest lower bound of refined and unrefined Types keeps the refinement. Two refinements combine
only when both are strict or both forgiving; their Requirements are conjoined. A common upper bound
keeps a refinement only when both operands have the exact same one.

### Forgiving `HAS?`

A forgiving refinement accepts a candidate when its Requirement holds or when no candidate anywhere
satisfies the ordinary refinement. Greenery placement uses this to fall back to any empty land area
when no empty area adjacent to the owner exists.

The escape test covers the whole refined Requirement. Put occupancy inside it when occupied adjacent
spaces should not prevent fallback.

### Refined Class literals

A refinement on `Class<Tag>` tests instances of the represented candidate Class:

```text
Class<Tag>(HAS Tag<Owner>)
```

Testing `Class<SpaceTag>` substitutes `SpaceTag` into the Requirement. This counts distinct live
tag Classes rather than tag component multiplicity. A refined Class literal auto-narrows when one
represented concrete Class satisfies it.

This represented-Class binding is separate from implicit variable recognition.

## 7. Complement bounds

A prefixed dependency argument excludes one Type from that dependency's domain:

```text
SpaceTag<!Player1>
OwnedTile<!Owner>
```

The domain comes from the constrained dependency: `Owner` for `SpaceTag`, `Anyone` for
`OwnedTile`. The exclusion must narrow the domain. A Complement never stands alone.

A candidate matches when it narrows the domain and does not narrow the exclusion. Complemented Types
remain abstract even if one concrete candidate survives. Two Complements have a common narrowing
only when they exclude the same Type.

The written form records the exclusion but not an independently narrowed domain. Current
consequences and defects are listed in section 12; do not patch individual symptoms before deciding
whether Complements are genuine difference Types.

## 8. Class Tables

Every Type belongs to one frozen Class Table. Values from different tables are not comparable.
Freezing compiles nominal subtype masks and sparse active-subclass indexes.

The Authority owns a master catalog. Each game projects it. A name has one of three states:

- **active:** full behavior in this game;
- **phantom:** known to the Authority but inactive here; or
- **unknown:** an error in every context.

A game's table is closed. No later declaration may change its hierarchy or set of concrete choices.

## 9. Closed-world operations

### Enumeration and automatic narrowing

Enumeration combines each active concrete root Class below an abstract Type with every admissible
concrete dependency binding. Refinements are then tested against World state; Complements filter
excluded candidates.

Automatic narrowing is stricter than “one refinement match”: the root and every dependency must
each have one structural concrete choice, and every refinement must accept it. The result is concrete
or absent. This avoids silently making a choice that a later consumer should see.

Whole-table and subclass enumeration require a frozen table.

### Greatest lower bound

`glb` finds the most general Type below both operands. Concrete incompatible dependencies have no
result. Between Classes, Pets requires one nominal common subclass above every other common subclass;
it does not synthesize an intersection.

Multiple direct supertypes that narrow one dependency use the same operation and are invalid without
one unique result.

### Upper bound

`lub` returns a common supertype and falls back to `Component`. Multiple nominal inheritance can
produce incomparable minimal candidates, so the implementation uses a heuristic rather than
promising a mathematical least upper bound.

Dependency `lub` retains a Complement only when the other bound already satisfies it or both
exclude the same Type; otherwise it falls back to the unexcluded domain.

## 10. Implicit Type variables

Repeated unspecified icons in one game rule usually mean one shared choice:

```text
PROD[StandardResource]: StandardResource
```

An eligible authored abstract Expression introduces an implicit variable in a choice-producing
region. Repeating the same authored Expression in a related region refers to that variable.
Narrowing one occurrence substitutes the same concrete Type at all occurrence paths.

Variables are inferred from authored syntax before defaults and lowering. They do not add a new kind
of choice; they link existing choices.

### Dependency keys and paths

A dependency key combines the Class that first declared it with its zero-based slot:
`Tile_0`, `Owned_0`, `Adjacency_0`. Subtypes retain the key. A dependency path follows keys
through nesting, so `Neighbor_0.Tile_0` differs from `Neighbor_1`.

Every Class literal slot uses `Class_0`. This makes sibling literal branches especially vulnerable
to accidental linkage in the current implementation.

### Authored matching

Intended matching canonicalizes names and associates written arguments with dependency keys.
Whitespace, redundant parentheses, and unambiguous argument order do not matter. Omission,
refinement, and Complement syntax remain meaningful authored differences:

```text
Tile != Tile<Area>
OceanTile != OceanTile<MarsArea>
Owner != Anyone
```

Only an abstract Expression can introduce a variable. Choose eligible maximal repeated Expressions;
do not also infer variables from smaller repeated subexpressions inside them.

### Contextual bindings

`This` and `Owner` are supplied by context, not normally inferred from repetition.

- `This` means the effect-bearing exact Type. `Class<This>` means its root Class without
  dependencies. Self triggers `This:` and `-This:` match only changed copies of that exact Type.
- `Owner` means the exact context owner when one exists. Otherwise it remains abstract and may
  participate in normal choice semantics.

### Regions

Variables link only across defined related regions:

- repeated matching bounds in one Class header when they refer to the same inherited dependency;
- a header variable and matching occurrences in that Class's Effects;
- trigger and instruction;
- action cost and result;
- stages of one `THEN`; and
- different roles inside one atomic instruction, except complete source and destination roots.

Comma siblings, separate `OR` arms, independent roots declared by one Class, sibling argument
branches, and requirement-only repetition do not introduce shared variables. Counted root
Expressions do not participate, though nested Expressions may. Outside Class headers, an Expression
containing a Complement should not participate.

A variable can be introduced outside an `OR` or Requirement and have occurrences inside it.

The proposed [`EACH`](EACHPLAYER.md) fanout would add one region linking its selector to repeated
authored occurrences in its body. Each enumerated concrete selector Type would substitute through
those paths. It would not bind contextual `Owner` or `This` and would reject a body with no linked
selector occurrence. This region is not implemented.

### Narrowing

A proposed subtype is substituted at every occurrence path and every containing Expression is
resolved again. Any rejecting occurrence or disagreement fails.

Variables survive inheritance and enumeration. A Class-header owner link therefore permits only
matching owner/card pairs. Outside headers, an open variable prevents task splitting across its
regions until earlier work chooses its Type.

## 11. Phantoms

Authority-known inactive Classes remain resolvable as behaviorless phantoms. A Type is phantom when
its root or a dependency bound is phantom. A refinement mentioning a phantom has an unsatisfiable
Requirement instead; the refined Type itself need not be phantom.

Phantoms:

- have no components, effects, defaults, invariants, or enumerated concrete subtypes;
- count zero;
- make optional and AMAP changes zero no-ops;
- make mandatory changes dead;
- never auto-narrow or fire triggers; and
- do not appear in listings or generated choices.

An active Class cannot have a phantom direct supertype or dependency bound. A Complement excluding a
phantom remains active over its active domain.

Loading an active declaration normally activates structurally mentioned Classes. A Class mentioned
only as the represented value of a Class-literal metric may remain phantom, but it must be known to
the Authority. Remaining known declarations become phantoms when the projection freezes.

## 12. Known divergences

Do not document these as intended semantics or “fix” them incidentally. [LINKAGES.md](LINKAGES.md)
coordinates the related variable work.

### 12.1 Sibling nested bounds can share a variable

Class-header matching uses dependency keys too broadly, so nested occurrences in sibling argument
branches may couple. Every Class literal uses `Class_0`, making repeated literal slots a subtle
case.

### 12.2 Complement narrowing accepts wider abstract candidates

The current test accepts a candidate that narrows the domain without narrowing the excluded Type.
Thus abstract `SpaceTag` counts as narrowing `SpaceTag<!Player1>` even though it still admits
Player 1. Concrete candidates behave as intended.

### 12.3 Class headers match only bare Class Names

A separate header mechanism sees only bare names. A repeated argument-carrying, refined, or
complemented Expression does not link as a unit; matching reaches inside it. Abstractness is not
consulted.

### 12.4 Header-to-Effect propagation substitutes Class Names

The implementation compares header/component bounds and rewrites matching Class Names throughout
Effects, even at unrelated source positions. Conflicting replacements for one name are silently
skipped rather than rejected.

### 12.5 Complement domains do not round-trip

Written and full forms show the exclusion but omit a separately narrowed domain. Printing and
resolving can therefore widen a Complement produced by `glb` or variable narrowing.

### 12.6 Nested Complements do not block variable matching

Outside headers, only a Complement at the candidate Expression's root is excluded from recognition.
A composite such as `OwnedTile<!Owner>` can still repeat as a variable today.

### 12.7 Matching does not canonicalize argument order

The current matcher compares raw ordered authored trees. Two unambiguous writings with reordered
dependency arguments resolve to the same Type but create separate variables.
