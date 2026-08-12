# The Pets type system: walkthrough

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

This document walks through the intended Pets type model one construct at a time. It is a detailed,
example-driven companion to the friendlier [Type system](../type-system.md) tutorial and describes
the model implemented under `dev.martianzoo.types`.

## Introduction

Pets is a specification language for one purpose: faithfully modeling the rules of *Terraforming
Mars*. It has no other users and is not trying to acquire any. Most of its unusual features exist
because a real card, tile, or standard project needs them. Features that a general-purpose language
would consider essential — fields, identity, mutation, subtype-independent parameterization — are
absent because the game does not need them.

Pets also tries to preserve the game's existing iconographic grammar. A player already understands
that a brown cube means any standard resource and that a production box changes a production track.
Pets gives those familiar forms precise semantics instead of first translating them into a
conventional object model. This fidelity is both the language's organizing principle and the source
of several features that would otherwise look arbitrary.

### The component graph

A Game World is coordinated by a `Timeline` and includes a Component Graph, a Task Queue, and an
event log. The type system is chiefly concerned with the **component graph**. Its vertices are
**components**, and the graph contains a multiset of them.

A component has no identity, properties, or state beyond its type. Two components of the same type
are indistinguishable: nothing in the language can tell them apart or retain a reference to one
particular copy. At the start of a game the graph contains twenty components of type
`TerraformRating<Player1>`, twenty of `TerraformRating<Player2>`, one of `Tharsis_2_2`, and sixty
more map areas like it.

Three operations change the graph: adding components, removing components, and atomically
transmuting components with `A FROM B`. A tile that has not been placed does not exist in the graph,
and megacredits paid for a card are removed rather than moved to an "out of play" location. Counts
cannot be negative. The fundamental observation available to rules is therefore: how many
components match this type?

## 1. Components and classes

The printed map provides a simple physical component. Pets records each hex as a component and uses
classes to distinguish its kind. A small part of the map hierarchy is:

```pets
ABSTRACT CLASS Area {
  ABSTRACT CLASS MarsArea {
    ABSTRACT CLASS LandArea
  }
}
CLASS Tharsis_2_2 : LandArea
```

A `CLASS` declaration introduces a class and a type with the same name. `Tharsis_2_2` is concrete:
that map hex has exactly that type. `Area`, `MarsArea`, and `LandArea` are abstract: they group
possible components, but no component has any of them as its exact type.

Nesting a declaration is shorthand for naming the enclosing class as a direct supertype. The same
hierarchy could be written:

```pets
ABSTRACT CLASS Area
ABSTRACT CLASS MarsArea : Area
ABSTRACT CLASS LandArea : MarsArea
CLASS Tharsis_2_2 : LandArea
```

Thus `Tharsis_2_2` is a subtype of `LandArea`, written `Tharsis_2_2 <: LandArea`, and
`LandArea <: MarsArea`. Subtyping is reflexive and transitive, so `Tharsis_2_2 <: MarsArea` too. The
map-hex component has all of those supertypes.

A component has exactly one concrete type and may have any number of other types through subtyping.
Several components may share one concrete type; ten otherwise identical plant cubes are ten
components, not one component whose state is ten. A component and its concrete type can sometimes
be discussed interchangeably, but multiplicity always belongs to components.

## 2. Nominal subtyping

A class may name any number of abstract classes as direct supertypes, separated by commas. This is
useful when one physical piece participates in two parts of the rules. An ocean tile, for example,
is both a map tile and one step of a global parameter:

```pets
CLASS OceanTile : Tile<MarsArea>, GlobalParameter
```

The dependency in `Tile<MarsArea>` will be explained next. For now, the declaration directly says
which two types are supertypes of `OceanTile`; there is no separate structural inference.

Concrete classes may not have subtypes, abstract or concrete. Since `CityTile` is concrete, both of
these declarations are rejected:

```pets
CLASS CapitalTile : CityTile
ABSTRACT CLASS FancyCityTile : CityTile
```

The universal top type is `Component`. Except for `Component` itself, every class that declares no
supertype has `Component` as its implicit supertype. Writing `Component` explicitly is an error
rather than a no-op. Direct-supertype declarations may appear in any source order, but they cannot
form a cycle.

## 3. Dependencies

A plant cube is not just a plant: it belongs to a player. The resource hierarchy sits inside an
owned-component declaration:

```pets
ABSTRACT CLASS Anyone
ABSTRACT CLASS Owned<Anyone> {
  ABSTRACT CLASS Resource {
    ABSTRACT CLASS StandardResource {
      CLASS Plant
    }
  }
}
```

An individual plant cube can therefore have the type:

```text
Plant<Player2>
```

The argument is a **dependency**. It makes an existence claim:

> A component of type `Plant<Player2>` cannot exist in the component graph unless a component of
> type `Player2` exists too.

This is stronger than ordinary generic parameterization. The argument designates another vertex in
the component graph, and the dependent component has an edge to it. Removing Player 2 therefore
requires removing Player 2's plants first.

### 3.1 Declaring a dependency

The map provides the clearest declaration:

```pets
ABSTRACT CLASS Area
ABSTRACT CLASS Tile<Area>
```

`Tile` introduces one dependency bounded by `Area`. Every tile depends on the area where it sits.
Further dependencies would follow the first, separated by commas.

One `Tile` type exists for each admissible narrowing of that bound: `Tile<Area>`, `Tile<MarsArea>`,
`Tile<LandArea>`, `Tile<VolcanicArea>`, and `Tile<Tharsis_2_2>` are all valid. Bare `Tile` means
`Tile<Area>`. In all of them, `Tile` is the **root class**. `Tile<Player1>` is invalid because
`Player1` is not a subtype of `Area`.

Dependency bounds are covariant. Because `VolcanicArea <: LandArea`, it follows that
`Tile<VolcanicArea> <: Tile<LandArea>`. A type is concrete only when both its root class and every
dependency bound are concrete. `Tile<MarsArea>` is abstract because both parts are abstract;
`OceanTile<MarsArea>` still leaves an area to choose; `OceanTile<Tharsis_5_5>` is concrete.

### 3.2 Dependencies in the component graph

Dependencies make the multiset of components a directed graph. An edge runs from each dependent to
its target. `Animal<Player1, Pets<Player1>>`, for example, describes an animal cube on Player 1's
Pets card. The animal depends on the card, and the card depends on Player 1. The whole type is one
coherent claim about three components, not a tuple of unrelated labels.

An edge identifies its target by exact concrete type because components have no identity. Every
concrete type admitted by a dependency bound must therefore have an applicable `MAX 1` or `=1`
counting invariant. `Area` and `Player` each declare `HAS =1 This`, where `This` means the current
exact type, while every card class gets a `HAS MAX 1` invariant. Without that guarantee,
`Animal<Player1, Pets<Player1>>` could not say which copy of Pets holds the animal.

This uniqueness requirement is a rule for valid Game Worlds, not a static Type restriction. When
building a game, the engine enumerates every concrete target admitted by the game's dependencies
and checks for a counting invariant whose maximum is one. A non-counting class invariant is rejected
by the same check.

### 3.3 The same dependency belongs to every subtype

```pets
ABSTRACT CLASS Tile<Area>
CLASS CityTile : Tile
```

Every `CityTile` is a `Tile`, and every `Tile` has an area dependency, so every `CityTile` has that
same dependency. It is not copied or redeclared. For the real subtype `VolcanicArea`, covariance
gives:

```text
CityTile<VolcanicArea> <: Tile<VolcanicArea>
```

The bare supertype `Tile` means `Tile<Area>`, so `CLASS CityTile : Tile` and
`CLASS CityTile : Tile<Area>` declare the same resolved supertype.

### 3.4 Narrowing an inherited dependency

Greenery tiles can be placed on Mars but not on remote areas created by cards such as Phobos Space
Haven. The declaration states that rule directly:

```pets
CLASS GreeneryTile : Tile<MarsArea>
```

There is no special narrowing syntax or hidden inheritance step. The declaration simply names
`Tile<MarsArea>` as a supertype of `GreeneryTile`. Since dependency bounds are covariant, satisfying
that declared supertype narrows the existing area dependency from `Area` to `MarsArea`:

```text
GreeneryTile<VolcanicArea> <: Tile<VolcanicArea> <: Tile<MarsArea>
```

A further subtype could narrow an abstract bound again. An unrefined concrete bound cannot be
narrowed further by nominal subtyping or dependency narrowing.

### 3.5 Introducing another dependency

A component on a card needs both an owner and the particular card that holds it. `Cardbound` adds
that second relationship while retaining the owner relationship supplied by `Owned`:

```pets
ABSTRACT CLASS Owned<Anyone>
ABSTRACT CLASS Cardbound<CardFront<Player>> : Owned<Player>
```

`Cardbound` therefore has an inherited owner dependency, narrowed from `Anyone` to `Player`, followed
by its newly declared `CardFront<Player>` dependency. A tag on Player 1's Ants card is written
`MicrobeTag<Player1, Ants<Player1>>`, with inherited dependencies before newly declared ones.

Card resources similarly declare a `ResourceHolder<Class<CardResource>, Owner>` dependency. A
`ResourceCard` is a `ResourceHolder` as well as a `CardFront`; solo mode also provides non-card
holders owned by `SoloOpponent`. This keeps the holder's owner linked to the resource's owner
without making neutral holders into cards.

```pets
ABSTRACT CLASS Owned<Anyone> {
  ABSTRACT CLASS Resource {
    ABSTRACT CLASS CardResource<ResourceHolder<Class<CardResource>, Owner>>
  }
}
CLASS Animal : CardResource<ResourceHolder<Class<Animal>>>
```

The concrete resource declaration narrows its holder to the matching resource type. Pets collects
animals and Ants collects microbes, so `Animal<Pets>` resolves while `Animal<Ants>` is a type error.

### 3.6 Writing dependency bounds

The declarations below are enough to illustrate argument matching:

```pets
ABSTRACT CLASS Area
ABSTRACT CLASS MarsArea : Area
ABSTRACT CLASS RemoteArea : Area
ABSTRACT CLASS LandArea : MarsArea
CLASS Tharsis_2_3 : LandArea

ABSTRACT CLASS Tile<Area>
CLASS OceanTile : Tile<MarsArea>, GlobalParameter
ABSTRACT CLASS Adjacency<Tile, Tile>
```

A written argument is intersected with the dependency bound it matches:

```text
OceanTile<Area>         resolves to    OceanTile<MarsArea>
OceanTile<Tharsis_5_5>  resolves to    OceanTile<Tharsis_5_5>, which is concrete
OceanTile<RemoteArea>   error: RemoteArea and MarsArea have no common subtype
```

Arguments match dependencies greedily from left to right. Each takes the first remaining
dependency with which it has a non-empty intersection. Order among unambiguous arguments therefore
does not matter: `GreeneryTile<Player1, Tharsis_2_3>` and
`GreeneryTile<Tharsis_2_3, Player1>` resolve alike. Order matters when bounds are alike:

```text
Adjacency                    resolves to    Adjacency<Tile, Tile>
Adjacency<CityTile>          resolves to    Adjacency<CityTile, Tile>
Adjacency<Tile, CityTile>    resolves to    Adjacency<Tile, CityTile>
```

An argument matching no remaining dependency is an error:

```text
OceanTile<Tharsis_5_5, Tharsis_5_6>    error: only one dependency remains
Tharsis_2_3<Player1>                    error: an area has no dependency
```

Every type has a **full form**, which states every bound, and a **minimal form**, which omits bounds
equal to the root class's own bounds except where retaining such a placeholder is necessary to make
greedy argument matching round-trip:

```text
full                          minimal
Tile<Area>                    Tile
OceanTile<MarsArea>           OceanTile
Adjacency<CityTile, Tile>     Adjacency<CityTile>
Adjacency<Tile, CityTile>     Adjacency<Tile, CityTile>
```

Types print in minimal form, and resolving that form yields the same type except where a
complement's unwritten domain makes even the full form lossy (section 7).

## 4. Class literals

Dependencies create a problem for production tracks. A steel-production component must exist even
when its owner has no steel cubes. `Production<Steel>` would say the opposite: because `Steel` would
be a dependency, the production component could exist only while a steel component existed.

The game needs to name the *kind* of resource without depending on an instance of it. That motivates
a new type form, the **class literal**:

```pets
CLASS Production<Class<StandardResource>> {
  ProductionPhase: StandardResource
}
```

`Class<Steel>` names the class `Steel`. It does not depend on a `Steel` component, so it exists on
turn one when nobody owns a steel cube. Production can therefore be written
`Production<Class<Steel>>`.

The angle brackets of a class literal contain one bare class name. These forms are valid:

```text
Class<Steel>
Class<Class>
Class                    means Class<Component>
```

These forms are rejected:

```text
Class<Steel<Player1>>    the argument is not a bare class name
Class<Steel, Player1>    there is only one slot
Class<Class<Steel>>      the argument is not a bare class name
```

Class literals are covariant just as their named classes are:

```text
Steel <: StandardResource
Class<Steel> <: Class<StandardResource>
```

A class literal is concrete exactly when the named class is concrete. Dependencies of that class
are irrelevant because the literal is not parameterized by them. This is the one place where the
class and the type with the same name can differ in concreteness:

```pets
ABSTRACT CLASS Area
ABSTRACT CLASS Tile<Area>
CLASS CityTile : Tile
```

```text
CityTile                abstract type (its Area dependency is abstract)
Class<CityTile>         concrete: CityTile is a concrete class
Class<Tile>             abstract: Tile is an abstract class
```

Exactly one `Class<CityTile>` component exists in a game whose vocabulary includes `CityTile`.
Abstract `Class<Tile>` has no exact component, but counting it includes the class-literal components
of every concrete tile class. Likewise `Class<StandardResource>` counts six: megacredits, steel,
titanium, plants, energy, and heat.

## 5. Default bounds

The physical game often leaves an obvious choice unstated. A resource icon printed on one of your
cards means your resource; an ocean icon means an available ocean area. Pets preserves those
omissions with `DEFAULT` declarations rather than forcing every rule to spell out context that the
icon grammar already supplies.

```pets
DEFAULT Owned<Owner>                       // all uses
DEFAULT +OceanTile<WaterArea>              // gains
DEFAULT -Owed<Class<Megacredit>>.          // removals
```

A default supplies dependency bounds that an author omits. It changes what a written expression
means without changing which resolved types exist or how they relate. The first declaration makes an
unqualified owned component refer to the current owner. The second turns an instruction to gain an
`OceanTile` into a choice among water areas. The third supplies the currency for a payment.

`Owner` here is contextual. Within a rule belonging to an owned component, it becomes that
component's exact owner: on Player 1's card, `Owned<Owner>` becomes `Owned<Player1>`. Where no
owning component supplies the context, `Owner` remains the ordinary abstract class.

All-use, gain-only, and remove-only defaults are gathered independently. Gain and removal defaults
also carry a Quantifier (`!`, `?`, or `.`). For one Dependency and one kind, only Declarations
on the nearest supertypes survive; a declaration on a proper supertype is superseded by one on a
narrower type. Bounds from incomparable surviving supertypes must have one most general common
narrowing, which is used. Conflicting Quantifiers from those supertypes are an error.

A literal `Owner` written in a default is deliberately preserved until the expression is used.
Normalizing it immediately to a narrower class bound would leave nothing for the owning component's
context to replace.

## 6. Refinement types

CrediCor gives a discount when its owner plays a card costing at least 20 megacredits. A
`CardFront` component has no cost field for the rule to inspect: components have only types. The
game instead represents printed cost with a component-graph query and selects qualifying cards
with:

```text
CardFront(HAS 20 CardCost)
```

This is a **refinement type**. The unrefined `CardFront` says what kind of component may qualify;
the `HAS` requirement says what the component graph must report about a candidate.

More generally, adding `(HAS requirement)` to an unrefined type produces an abstract subtype of
that type. The base need not itself be abstract. Refining a concrete type creates a proper abstract
subtype without introducing a class or an exact component.

Real refinements express many rules that would otherwise need fields or special-purpose code:

```text
CardFront(HAS 20 CardCost)                       CrediCor: a card costing 20 or more
CardFront(HAS CardRequirement)                   Tactician: a card with a printed requirement
LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)   an area where a city may be placed
MarsArea(HAS 8 MarsRow)                          Polar Explorer: either polar row
MarsArea(HAS MapBonus<Class<Steel>>
         OR MapBonus<Class<Titanium>>)           Mining Guild: an area with either mining bonus
```

### 6.1 Testing a candidate

Because the candidate has no fields, testing a refinement substitutes the candidate into the
requirement and then asks the component graph whether that requirement holds.

The city-placement refinement appears in the default for gaining a city tile:

```pets
CLASS CityTile {
  DEFAULT +CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>
}
```

Testing `Tharsis_2_3` offers that candidate as an additional argument to every type expression in
the requirement. `Neighbor` has a tile dependency followed by an area dependency:

```pets
CLASS Neighbor<Tile<MarsArea>, MarsArea>
```

The existing `CityTile<Anyone>` argument matches the tile dependency. The candidate area cannot
match that slot, so it matches the remaining area dependency. The graph is asked whether it
satisfies:

```text
MAX 0 Neighbor<CityTile<Anyone>, Tharsis_2_3>
```

That reads "no city belonging to anyone is adjacent to this area," which is the actual placement
rule.

If an expression has no dependency position that can accept the candidate, the test fails.
`LandArea(HAS 8 BuildingTag)`, for example, selects nothing: a building tag depends on an owner and
a card front, neither of which can accept an area.

Satisfaction therefore has two parts. The candidate must narrow the unrefined type, and the graph
must satisfy the substituted requirement. A concrete type that satisfies a refinement is not thereby
a static subtype of the refinement; state-aware consumers perform the satisfaction test separately.

### 6.2 Static relationships

A refinement is always a subtype of its unrefined type. Narrowing the base while retaining exactly
the same requirement also narrows the refinement:

```text
CardFront(HAS 20 CardCost) <: Component
ActiveCard(HAS 20 CardCost) <: CardFront(HAS 20 CardCost)
```

Different requirements do not establish static subtyping, even when one logically implies another.
In particular, `CardFront(HAS 20 CardCost)` is not a static subtype of
`CardFront(HAS 10 CardCost)`.

Testing an unrefined candidate against a refined wider type asks the component graph whether the
candidate satisfies the requirement. A context-free check raises if structural matching cannot
already reject the candidate.

Intersecting a refined and an unrefined type retains the refinement. Intersecting two refinements
conjoins their requirements only when both are strict or both are forgiving. Otherwise no greatest
lower bound exists: making the result strict would discard forgiveness, while making it forgiving
could discard a strict requirement.

An upper bound retains a refinement only when both operands carry that exact refinement. If only
one side is refined, or their refinements differ, their common upper bound is unrefined.

### 6.3 Forgiving refinements

The greenery placement rule normally requires an empty area adjacent to one of your tiles. On a
player's first placement, however, there may be no owned tile to satisfy that rule. Pets writes the
exception with `HAS?`:

```pets
CLASS GreeneryTile : Tile<MarsArea> {
  DEFAULT +GreeneryTile<LandArea(HAS? Neighbor<OwnedTile>, MAX 0 Tile)>
}
```

A candidate satisfies a forgiving refinement when it satisfies the ordinary requirement, or when no
candidate satisfies that ordinary refinement anywhere:

```text
requirement OR MAX 0 unrefined-type(HAS requirement)
```

The second arm is the escape clause. Here `MAX 0 Tile` sits inside the forgiving refinement so
occupancy participates in the escape test. If every area adjacent to one of your tiles is already
occupied, the combined rule has no candidate and placement may fall back to any empty land area.

### 6.4 Refining class literals

Diversifier counts the distinct kinds of tags its owner has, not the total number of tag components:

```json5
{ "id": "HM0", "requirement": "8 Class<Tag>(HAS Tag<Owner>)" },
```

A refinement on a class literal tests the class represented by each candidate literal. When
`Class<SpaceTag>` is tested against `Class<Tag>(HAS Tag<Player2>)`, the `Tag` in the requirement
becomes `SpaceTag`. The candidate qualifies exactly when the graph contains
`SpaceTag<Player2>`.

This represented-class binding is why expressions inside a class-literal refinement are left alone
when the candidate literal itself has no ordinary dependency position for them. The class-token
components remain unowned; the requirement asks about instances of the represented class. Diversity
Support uses the same mechanism for resource kinds:

```json5
"requirement": "9 Class<Resource>(HAS Resource<Owner>)",
```

## 7. Complement bounds

Toll Station increases megacredit production once for each space tag owned by an opponent. A normal
bound can narrow `Owner` to one player, but the card needs every owner *except* one. Prefixing an
argument with `!` expresses that exclusion:

```json5
// Toll Station
{ "id": "099", "cost": 12, "tags": [ "SpaceTag" ],
  "immediate": "PROD[1 / SpaceTag<!Owner>]" },
```

On Player 1's card the contextual `Owner` becomes `Player1`, so the metric counts
`SpaceTag<!Player1>`: space tags belonging to anyone other than Player 1. Philares uses the same
form for a tile belonging to someone else:

```json5
"Adjacency<OwnedTile<Owner>, OwnedTile<!Owner>> BY Anyone: StandardResource",
```

Aridor combines an exclusion with a refined class literal to mean a non-event tag the owner does
not already have:

```json5
"effects": [ "Class<Tag>(HAS Tag<Owner, !EventCard>): PROD[1]" ],
```

A complement has a **domain** and an **excluded type**. The domain comes from the dependency being
constrained. It is `Owner` for `SpaceTag`, but `Anyone` for `OwnedTile` because the
non-player `SoloOpponent` may own tiles. The exclusion must be a subtype of that domain, so
`Owned<!CityTile>` is an error. Excluding the entire domain, as in `Owned<!Anyone>`, is valid and
admits nothing.

A complement never stands alone: `!Player1` is meaningful only against a dependency's domain.
When testing a candidate, Pets checks that it narrows the domain and does not narrow the excluded
type. A complemented type remains abstract even when only one concrete choice survives. In a
two-player game, for example, the only concrete owner choice admitted by
`SpaceTag<!Player1>` is Player 2.

Two complemented bounds have a common narrowing only when they exclude the same type:

```text
Owned<!Player1> with Card<!Player1>      narrows to Card<!Player1>
Owned<!Player1> with Owned<!Player2>     has no common subtype
```

The written form records the exclusion but not an independently narrowed domain. If another
constraint narrows that domain below the root class's normal bound, printing and resolving the type
again loses the extra narrowing.

## 8. Class tables

An instruction to place an `OceanTile` must offer exactly the legal areas on the selected map. It
must not offer a Hellas area during a Tharsis game, a third player in a two-player game, or a Venus
global parameter when Venus Next is absent. Generating choices therefore requires a fixed vocabulary
for this particular game.

Every type belongs to one **class table**: a frozen, closed set of mutually compatible classes.
Types from different tables are never comparable. Type operations and constraint matching fail
rather than mix them.

In the aspirational Authority model, a table would be built by filtering the declarations supplied
by one Authority and then freezing it. Once frozen, its classes and subtype relationships cannot
change. A class name the table cannot resolve is an error in every context: as a type, a dependency
bound, or part of a metric.

Freezing also compiles nominal subclass tests. Only classes that actually have proper subclasses
receive bit positions; the declaration rules guarantee that all of them are abstract. Every known
class, including an inactive phantom, then stores its abstract-superclass set in an immutable,
arbitrary-width bit mask. A frozen subclass test is an identity check followed by one bit lookup;
concrete and childless abstract targets therefore require no bit at all. While the table is still
loading, the same operation falls back to the resolved superclass set.

Freezing also builds sparse reverse indexes for proper and direct active subclasses. Proper
subclass sets exclude the indexed class itself, so leaf classes share an empty stored result;
`Class.allSubclasses()` adds an active receiver back to preserve its inclusive public contract.
Inactive phantoms participate in subtype masks but are absent from subclass enumeration.

## 9. Closed-world type operations

Once the class table is frozen, Pets can answer questions that would be undecidable against an
open-ended supply of future classes.

### 9.1 Enumeration and automatic narrowing

For an abstract type, **enumeration** lists every concrete subtype present in the table. To enumerate
`OceanTile<WaterArea>`, Pets combines every concrete root class below `OceanTile` with every
admissible concrete water-area bound. The result is one type per water area on the selected map,
which is exactly the placement choice the player must see.

Complements filter excluded choices. A refinement is handled separately: enumeration starts from
its unrefined type, then a state-aware consumer filters the concrete candidates by testing the
refinement against the component graph.

**Automatic narrowing** is the special case where exactly one concrete candidate exists. It is
strict: the root must have exactly one concrete subtype, and each dependency must have exactly one
concrete narrowing.

```text
TerraformRating         solo game: narrows automatically to TerraformRating<Player1>
TerraformRating         four-player game: four choices, so a player must be chosen
OceanTile<WaterArea>    Tharsis: several water areas, so an area must be chosen
```

Automatic narrowing cannot discard information for a later consumer to check. Complements must
leave one candidate, and refinements on the type or any dependency bound must accept it. A
refinement does not turn several structurally possible root classes into an automatic choice;
structural uniqueness is checked first. The result is concrete or absent.

`Type.allConcreteSubtypes()` ranges across every concrete root class below the receiver's root.
`Type.concreteSubtypesSameClass()` and `Class.concreteTypes()` instead enumerate only types whose
root class is exactly the receiver or that class. Whole-table enumeration, subclass enumeration,
and every operation built on them require the class loader to have been frozen; calling them while
the table can still grow fails rather than capturing an incomplete snapshot.

### 9.2 Greatest lower bounds

Argument matching, overlapping constraints, and multiple direct supertypes all need an
intersection. Pets calls the most general type below both inputs their **greatest lower bound** or
`glb`. It need not exist:

```text
OceanTile<Tharsis_5_5> glb OceanTile<Tharsis_5_6>       none
```

Pets does not manufacture structural intersection types. Between classes, it searches the table for
one nominal class that is below both inputs and above every other common subtype. `OwnedTile` is
the canon's example:

```pets
ABSTRACT CLASS Tile<Area> {
  ABSTRACT CLASS OwnedTile : Owned
}
```

`OwnedTile` is below both `Owned` and `Tile`, and every class below both is also below
`OwnedTile`, so it is their greatest lower bound. This is also a modeling check. The Landlord award
counts `OwnedTile`; a tile class declared separately as both `Owned` and `Tile` without
subtyping `OwnedTile` would silently not count.

When several direct supertypes narrow the same dependency, their bounds are combined with this same
operation. The class is invalid if those constraints have no unique greatest lower bound.

### 9.3 Upper bounds

The **upper bound** operation, `lub`, returns a common supertype, using `Component` when nothing
closer exists. Multiple nominal supertypes mean a mathematical least upper bound may not be unique.
For example, `OceanTile` is both `Tile<MarsArea>` and `GlobalParameter`; there can be several
incomparable minimal common supertypes, so the implementation selects one by a heuristic.

At dependency level, `lub` lifts the same operation through ordinary type bounds. A complement is
kept only when the other bound already satisfies it, or when both complements exclude the exact
same type; otherwise the result falls back to the complement's unexcluded domain.

## 10. Implicit type variables

Manutech gives its owner one resource whenever that resource's production rises. The printed card
uses the same unspecified-resource icon in the production box and in the result. Pets preserves that
repetition:

```json5
// Manutech
"effects": [ "PROD[StandardResource]: StandardResource" ],
```

Nobody reads the two icons as unrelated resource choices. In a **choice-producing** position, an
authored abstract expression introduces an implicit **type variable** bounded by that expression.
Repeating the same authored expression in a related position refers to the same variable. Narrowing
one occurrence then narrows all of them to the same concrete type.

This adds no new kind of choice. A lone `StandardResource` was already a choice among resource
types; repetition merely makes that choice referable for the duration of a rule. Pets has no syntax
for naming the variable because the game's icon grammar has none. Robinson Industries demonstrates
the boundary: its printed card falls back to words for "your lowest production," while Pets uses a
named custom operation.

### 10.1 Dependency keys and paths

Repetition matching sometimes needs to distinguish two identical-looking dependency positions.
`Adjacency<Tile, Tile>`, for example, relates two independently chosen tiles. Pets identifies a
dependency by a **key** made from the class that first declared it and its zero-based position in
that class's own dependency list.

```text
Tile<Area>                              area dependency: Tile_0
Owned<Anyone>                           owner dependency: Owned_0
Adjacency<Tile, Tile>                   tile dependencies: Adjacency_0, Adjacency_1
```

Every subtype retains the same key for the same dependency. Narrowing `GreeneryTile` from
`Tile<Area>` to `Tile<MarsArea>` changes the bound but not the key `Tile_0`. In canonical
argument order, inherited dependencies come first in direct-supertype order, followed by newly
declared dependencies.

A **dependency path** follows keys into a nested type. In:

```pets
CLASS Neighbor<Tile<MarsArea>, MarsArea>
```

the `MarsArea` inside `Tile` is at `Neighbor_0.Tile_0`; the separate outer `MarsArea` is at
`Neighbor_1`. Paths let a variable narrow one nested occurrence without confusing it with an
identically spelled occurrence elsewhere. Every class-literal slot uses `Class_0`: `Class` is
the sole class that declares that special slot, and no ordinary class can declare another.

### 10.2 Matching authored expressions

Variables are found in the authored expression tree, before defaults or other preprocessing insert
anything. Two occurrences match when their parsed trees are structurally equal after names are
canonicalized and each written argument is associated with its dependency key. Whitespace,
redundant parentheses, and the order of unambiguous arguments do not matter. Omitted bounds,
refinements, and complements remain authored differences.

These pairs do not match, even when resolution or context later makes their members equal:

```pets
Tile                  Tile<Area>
OceanTile             OceanTile<MarsArea>
Owner                 Anyone
```

This literal rule keeps shared choices visible in the source and prevents defaults from coupling
otherwise independent choices. The represented-class binding in a refined class literal is a
separate mechanism: `Class<Tag>(HAS Tag<Owner>)` binds the `Tag` in the requirement to the class
represented by the candidate literal, not to a variable inferred from repetition.

Among matching expressions:

- Only an expression resolving to an abstract type can introduce a variable.
- Within one scope, eligible maximal occurrences of that expression belong to one variable.
- A smaller expression nested inside every occurrence of a larger repeated expression does not
  introduce a second variable. Repeating `CardFront(HAS BioTag)` chooses one refined card front,
  not both a card front and an independent bio tag.
- A variable introduced by a direct supertype remains the same variable in its subtypes.
  Coincidentally matching source elsewhere does not join it.

### 10.3 Contextual bindings

Two reserved names bind from context rather than repetition.

**`This`.** One occurrence is enough to refer to the exact type supplied by the current class
declaration. In an abstract class, it remains late-bound for each concrete subtype. For an exact
component type such as `Pets<Player1>`, ordinary `This` becomes that full type, while
`Class<This>` becomes `Class<Pets>` because a class literal represents a class without its
dependencies.

Event cards use both forms when they move from play to the played-events pile:

```pets
ABSTRACT CLASS EventCard {
  This: PlayedEvent<Class<This>> FROM This
}
```

The two ordinary occurrences identify the exact card component being transmuted. The class literal
records which card was played without depending on the removed component. An explicitly authored
`Class<EventCard>` would remain fixed; it would not specialize merely because it happened to
resolve like `Class<This>` at some point.

The trigger forms `This:` and `-This:` are self-event selectors. They respond only to copies of
the effect-bearing exact type added or removed by the current change. A change of several copies
scales the instruction by that number; other copies already in the graph do not multiply it. Inside
`TerraformRating`, these hypothetical effects therefore differ:

```pets
This: VictoryPoint
TerraformRating: VictoryPoint
```

The first fires for terraform-rating steps gained now. The second is an ordinary subscription to
matching terraform-rating changes.

**`Owner`.** Within an effect belonging to a concrete owned component, `Owner` refers to that
component's exact owner. On Player 1's card, `This: VictoryPoint<Owner>` becomes
`This: VictoryPoint<Player1>`. In context-free expressions, `Owner` remains the ordinary abstract
class and may introduce or refer to a repeated choice. Effects with no owning component may remain
abstract until execution supplies a contextual player; [Engine](ENGINE.md) defines that fallback.

`This` and contextual `Owner` are excluded from repetition inference when context has already
bound them. They are supplied values, while an implicit variable is a choice waiting to be narrowed.

### 10.4 Variable scopes

A repeated expression shares a variable only across related source regions.

**Within a Class header.** Two written bounds share a variable when the Expressions match and
bind the same dependency key. This says that a cardbound component belongs to whoever owns its
card:

```pets
ABSTRACT CLASS Cardbound<CardFront<Player>> : Owned<Player>
```

Both `Player` occurrences bind `Owned_0`, one through the nested `CardFront` and one through the
declared supertype. They are one owner choice. Matching text bound to different dependency keys stays
independent. A repeated bound carrying arguments is matched as a whole expression, not merely by
names nested inside it. If a subtype adds a variable sharing a position with an inherited variable,
the positions merge into one variable.

**From a Class header to its Effects.** A variable introduced in the header extends to
matching occurrences in that class's effects:

```pets
CLASS PlayCard<Class<CardBack>, Class<CardFront>> : Owned, Signal {
  This:: CheckCardRequirement<Class<CardFront>>
  This:: HandleCardCost<Class<CardFront>>
}
```

Choosing a card front once narrows both its requirement check and cost calculation to that card.

**From a trigger to its instruction.** A trigger match narrows matching occurrences in the
instruction. Award scoring relies on this rule:

```pets
MeasureAward<Award>:: TallyAward<This, Award>
```

The award being measured is the award tallied.

**Across regions of one action, `THEN`, or atomic instruction.** Costs and results, stages of one
`THEN`, and different operand roles of one atomic instruction are related regions. Kaguya Tech
turns one greenery into a city on the same area:

```json5
"immediate": "PROD[2], ProjectCard, CityTile<LandArea> FROM GreeneryTile<LandArea>",
```

The repeated `LandArea` is one area choice. Complete source and destination expressions of a
transmutation are deliberately independent, so Market Manipulation may raise one colony track and
lower another:

```json5
"immediate": "ColonyProduction FROM ColonyProduction",
```

Outside a Class header, repetition refers back only when matching occurrences lie in at least two
regions. Three copies of one abstract expression within a single `THEN` stage remain three choices.
Comma-separated instructions and alternative `OR` arms are also independent, although a variable
introduced by an enclosing region may have occurrences inside them.

Several exclusions preserve intended independence:

- Each root expression in a class's own dependency declaration introduces a fresh keyed dependency.
  The two `Tile` roots of `Adjacency<Tile, Tile>` are independent, as are the nested and outer
  `MarsArea` roots in `Neighbor<Tile<MarsArea>, MarsArea>`.
- Sibling branches of one angle-bracket list are independent.
- A requirement position may refer to a variable introduced outside the requirement, but repetition
  found only within requirements introduces none. Testing two facts does not claim one witness for
  both.
- A counted expression — the resource in `2 StandardResource`, for example — neither introduces nor
  refers to a variable. Expressions nested inside it remain eligible.
- Outside Class headers, an Expression written with a Complement neither introduces nor refers
  to a variable. Two writings of `OwnedTile<!Owner>` are therefore independent choices.

Within a Class header, a complemented bound may occupy a variable's positions. Narrowing applies
to the shared domain before the exclusion; an exclusion already ruled out by the shared narrowing
then disappears.

### 10.5 Narrowing a variable

A type variable begins at the upper bound denoted by its source expression. A proposed subtype is
substituted at every occurrence, and every containing expression is resolved again. Narrowing fails
if an occurrence cannot accept the proposed type or if the occurrences disagree.

A Class header enforces this whenever a Type is formed. Intersections propagate through all
positions of one variable until no bound changes. The tag hierarchy provides a real example:

```pets
ABSTRACT CLASS Cardbound<CardFront<Player>> : Owned<Player> {
  ABSTRACT CLASS Tag : Atomized {
    CLASS SpaceTag
  }
}
```

```text
SpaceTag<Player1, CardFront>            same type as SpaceTag<CardFront<Player1>>
SpaceTag<Player1, CardFront<Player2>>   error: the owner occurrences disagree
```

Variables survive through subtyping, and enumeration respects them. A two-player game admits space
tags only in matching owner/card pairs:

```text
SpaceTag<Player1, SpaceElevator<Player1>>
SpaceTag<Player2, SpaceElevator<Player2>>
...one pair per player and card front, never crossed
```

Outside a Class header, a variable remains open while several concrete bindings are possible. A
composite instruction cannot split across a boundary crossed by an open variable:
`CityTile<LandArea> FROM GreeneryTile<LandArea>` must choose the area before its source and
destination can separate. Once earlier work fixes that area, Pets substitutes it at every later
occurrence and ordinary task splitting can continue.

## 11. Optional vocabulary and phantom classes

The closed-table model is sufficient until rules shared across optional expansions enter the
picture. Diversifier, for example, is declared once to count eight distinct tag classes:

```json5
{ "id": "HM0", "requirement": "8 Class<Tag>(HAS Tag<Owner>)" },
```

The Venus Next expansion adds `VenusTag`. A game without that expansion must neither count the tag
nor require a different Diversifier declaration. At the same time, `Class<Tag>` must keep ranging
over all tag classes that this particular game includes.

The aspirational Authority model handles that distinction by giving each game exactly one
**Authority**: the full, uniquely named catalog of class declarations it is allowed to load. A
class known to the Authority but not active in the current game would be installed in the frozen
table as **phantom**. Names therefore have three states in that target model:

```text
active    installed in this table for this game
phantom   known to the authority but inactive in this table
unknown   not known to the authority
```

In a two-player base game, `Player3`, `SoloMode`, `SoloOpponent`, `VenusStep`, and `VenusTag` are
phantom. An unknown name remains an error in every context, including inside a class literal.

A type is phantom when its root class or any dependency bound is phantom. A refinement mentioning a
phantom class is instead an unsatisfiable requirement; it does not turn the refined type phantom.
Phantom types and their class literals count zero and are otherwise inert:

- They have no components, effects, defaults, invariants, or enumerated concrete subtypes.
- Optional and as-many-as-possible changes to them are quantity-zero no-ops.
- A mandatory change is dead. A choice discards that branch; an unavoidable dead change fails.
- They are never inferred as the only legal choice and cannot fire as triggers.

Thus `VenusTag` contributes zero to Diversifier without requiring a second milestone declaration.

An active class cannot have a phantom direct supertype or phantom dependency bound. Either makes the
table invalid. A complement excluding a phantom type is not itself phantom, because its active
domain remains the dependency bound:

```text
Owned<!SoloOpponent>    in a multiplayer game: active; the bound is still Anyone
```

Only active classes appear in class and name listings or generated choices. This is also why the
game lists exactly the milestones belonging to its selected map even though an unclaimed milestone
has no ordinary `Milestone` component.

Loading a declaration as active normally activates every class name it mentions. A structural
dependency bound must resolve to an active class, while another authority-known inactive name loads
as phantom. A custom implementation's `requiredClassNames` are activation edges too. A class name
used only as the represented class of a `Class<...>` metric is the exception: counting it does not
by itself make that class active, but the name must still be authority-known when the mentioning
declaration loads. When the table freezes, every remaining authority-known declaration is installed
as phantom.

## 12. Known divergences

Each item below is current implementation behavior that this walkthrough would otherwise describe
differently. They are recorded here rather than smoothed over.

### 12.1 Sibling nested bounds share a variable

The same-key rule for Class headers is applied to nested occurrences in sibling branches of one
`<...>` list, so a declaration like `Adjacency<Tile<MarsArea>, Tile<MarsArea>>` couples choices
that were meant to stay independent. With class literals the coupling is easy to miss, since all
class literal slots share the key `Class_0`: had
`PlayCard<Class<CardBack>, Class<CardFront>>` instead been written with the same literal twice,
those two slots would share one variable. The intent is that only a class's own repeated writing of
a bound at distinct positions of the same inherited dependency should share one.

### 12.2 Complement narrowing accepts wider abstract candidates

The test is "narrows the domain and does not narrow the excluded type", so `SpaceTag` — whose owner
is still the abstract `Owner` — counts as narrowing `SpaceTag<!Player1>` even though it admits
`SpaceTag<Player1>`. The rule behaves as intended for concrete candidates.

### 12.3 Class headers match only bare Class Names

The intended Class-header rule adds only the same-key Requirement to the shared matching rules;
in fact it uses a separate mechanism in which only a bare Class Name is an occurrence. A repeated
bound carrying arguments, a refinement, or a `!` never becomes a variable as a unit, so matching
reaches past it to the names inside. Two repeated `Card<Owner>` bounds should represent one choice;
instead only their nested `Owner` occurrences are coupled, leaving the card choices free to differ.
Abstractness is not consulted either, though a variable over concrete positions has no effect.
Section 12.1 is the other half of this: one shared matching mechanism would fix both.

### 12.4 Class-header-to-Effect narrowing is Class-Name substitution, not a variable

The intended rule makes one variable span a Class header and its Effects; what runs is a substitution
built by comparing every bound of the class's own type — nested bounds included — with the
component's, and rewriting every occurrence of each differing class name anywhere in the effects,
arguments, and requirements alike. It therefore rewrites expressions the author never wrote alike,
and when one name would map to two different replacements it drops that substitution silently
instead of reporting the disagreement a Class header would report.

### 12.5 A complement's domain is not written out

Both written forms of a complemented bound show only the exclusion, so a domain narrowed by `glb`
or by a type variable below what the printed root class implies is lost when the printed form is
resolved again. This affects the full form too.

### 12.6 A nested complement does not block repetition matching

An expression written with `!` should neither introduce nor refer to an implicit variable outside a
Class header. The matcher checks only whether the candidate Expression itself is complemented,
so the root `OwnedTile<!Owner>` remains eligible even though its argument is complemented. Repeating
that complete expression across regions therefore shares a variable today.

### 12.7 Matching does not canonicalize argument order

The intended matcher compares expressions after associating their explicitly written bounds with
dependency keys, so two unambiguous argument orders are meant to match. Today the matcher compares
raw expression trees, whose argument lists remain ordered: `GreeneryTile<Player1, LandArea>` and
`GreeneryTile<LandArea, Player1>` therefore introduce separate variables.
