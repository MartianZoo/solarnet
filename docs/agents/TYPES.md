# Pets type system: a comprehensive walkthrough

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

This document walks through the whole Pets type system one construct at a time, in an order where
each step depends only on earlier ones. It aims to be complete rather than readable-first; see
[Type system](../type-system.md) for the friendlier tutorial. Despite the file name it is not a formal
specification: it is a worked example-driven tour of the model that
`pets/src/commonMain/kotlin/dev/martianzoo/types` exists to implement. Where the implementation and
that model disagree, the walkthrough describes the intended rule, section 15 records what today's
code does instead, and `TODO.md` carries the fix.

Nearly every example below is a real declaration, quoted (sometimes with an unrelated dependency or
effect elided) from `pets/src/commonMain/resources/pets/system.pets` or from the canon sources under
`canon/src/commonMain/resources/canon/bundles/`. Where an example is invented, it says so.

## Introduction

Pets is a programming language designed for exactly one program: the rules of the board game
*Terraforming Mars*. It has no other users and is not trying to acquire any. That focus is the
reason it looks the way it does — most of its unusual features exist because some real card, tile,
or standard project needed them, and features a general-purpose language would consider essential
(fields, identity, mutation, subtype-independent parameterization) are simply absent, because the
game never needs them.

If you have played the game once, you already know its vocabulary: players, cards, tags, tiles,
areas of the map, cubes of steel and plants and heat, production tracks, global parameters,
milestones, awards. Every one of those is a **class** in Pets, and this document uses them as its
examples throughout. If you have never played, you lose nothing: each example explains the rule it
encodes.

### A world is a multiset

The complete state of a game — everything except the queue of work still to do — is a **world**: a
multiset of **components**. A component has no identity, no fields, and no mutable state. It has
only a type. Two components of the same type are not merely equal, they are indistinguishable, and
nothing in the language can tell them apart or hold a reference to one in particular.

At the start of a game a world contains twenty components of type `TerraformRating<Player1>`,
twenty of `TerraformRating<Player2>`, one of `Tharsis_2_2` (an area of the map), sixty more like it,
and so on. There are exactly two ways to change a world: add N components of some concrete type, or
remove N of them. (Atomic transmutation, `A FROM B`, is a third, but it behaves like a paired
remove-and-add.)

Several familiar things follow immediately. There is no "out of play": a tile that has not been
placed does not exist, and a card you paid for did not move its cost anywhere — those megacredits
are gone. There is no negative quantity of anything, which eliminates a large class of bugs and
creates exactly one headache, since megacredit *production* in Terraforming Mars can legitimately go
below zero. (The workaround is a hidden class named `GrossHack`, and its declaration says
`"You didn't see this"`.) And the only question you can ask a world is a count: how many components
match this type?

### A type is a claim about what must exist

Types carry other types in angle brackets: `Plant<Player2>`, `CityTile<Tharsis_2_2>`,
`Animal<Player1, Pets<Player1>>`. This is not parameterization for convenience. Each such argument
is a **dependency**, and it means something specific:

> A component of type `A<B>` cannot exist unless a component of type `B` exists too.

A plant cube cannot exist unless there is a player to own it. A city tile cannot exist unless there
is a player to own it *and* a specific area of the map for it to sit on. An animal cube cannot exist
unless there is a card that collects animals — and that card cannot exist unless a player owns it,
which is why `Animal<Player1, Pets<Player1>>` is one coherent claim about three components rather
than a tuple of unrelated labels. Remove the card and the animals on it must go first; that is not a
special rule about cards, it is what the type already said.

So the components of a world are the vertices of a directed graph, with an edge from each dependent
to each of its targets. Because components have no identity, an edge cannot point at "that one over
there" — it points at a *concrete type*, and the type system has to guarantee that at most one
component of that type can ever exist. (Section 5 explains how; the short version is that `Area`
declares `HAS =1 This` and every card class gets `HAS MAX 1 This`.)

### What follows

Almost everything in the rest of this document is downstream of those two facts.

- Components have no fields, so a type that wants to select "cards costing at least 20" cannot
  inspect a candidate; it has to ask the world a question about it. That is a **refinement**
  (section 11), and CrediCor's `CardFront(HAS 20 CardCost)` is exactly that.
- Angle brackets mean existence, so `Production<Steel>` would be a disaster: your steel production
  would vanish the moment you spent your last steel cube. Hence **class literals** (section 4) and
  the real declaration `Production<Class<StandardResource>>`.
- An abstract type is a choice not yet made — "a tile", "an ocean somewhere". The engine must be
  able to *enumerate* the concrete choices and offer them, not merely validate one afterward, which
  requires a closed, frozen vocabulary (sections 3 and 12).
- Pets has no separate syntax for naming a pending type choice, so it says "the same one" by writing
  the same expression twice. An abstract expression in a choice-producing position therefore
  introduces an implicit **type variable** bounded by the type it denotes, which a repetition of the
  expression refers back to (section 13). That is how
  `Cardbound<CardFront<Owner>> : Owned<Owner>` manages to say "a tag belongs to whoever owns the card
  it is printed on."

## 1. Classes and nominal subtyping

A class declaration introduces a type of the same name.

```pets
CLASS Generation
```

The type `Generation` is concrete. Declaring a class inside another class's body is shorthand for
naming the enclosing class as a superclass, so these two spellings of the global-parameter classes
are equivalent:

```pets
ABSTRACT CLASS GlobalParameter {
  CLASS TemperatureStep
  CLASS OxygenStep
}
```

```pets
ABSTRACT CLASS GlobalParameter
CLASS TemperatureStep : GlobalParameter
CLASS OxygenStep : GlobalParameter
```

`GlobalParameter` is an abstract class; `TemperatureStep` and `OxygenStep` are concrete and are its
direct subclasses. Concrete classes may not have subclasses, abstract or concrete; since `CityTile`
is concrete, both of these are rejected:

```pets
CLASS CapitalTile : CityTile
ABSTRACT CLASS FancyCityTile : CityTile
```

A class may have any number of abstract classes as superclasses, separated by commas. An ocean tile
is both a tile on the map and a step along a global parameter track, and says so:

```pets
CLASS OceanTile[OT] : Tile<MarsArea>, GlobalParameter
```

As types, `TemperatureStep` is a subtype of `GlobalParameter`, written
`TemperatureStep <: GlobalParameter`. Other cases of subtyping are defined below. The subtype
relation is the reflexive, transitive closure of the relationships specified in this document.

The universal top type is `Component`. Except for `Component` itself, every class that declares no
superclass has `Component` as its implicit superclass; writing `Component` as an explicit supertype
is an error rather than a no-op.

Superclass declarations may appear in any order, so a class may name a superclass declared later in
the file, or in another file entirely: `Player` extends `Actor`, which lives in `system.pets`. What
they may not do is form a cycle: a self-extending `CLASS Tile : Tile`, or a mutual
`CLASS Tile : CityTile` / `CLASS CityTile : Tile`, both fail to load. Dependency cycles are a
different matter and are permitted (section 5).

Every class also has a short name, or id, such as `CT` for `CityTile`, `TR` for `TerraformRating`,
`O2` for `OxygenStep`, and `SPT` for `SpaceTag`. Anywhere a class name is looked up, either spelling
works.

## 2. Components and worlds

A component is the basic unit of a world. It has several types. Exactly one is concrete and is
called its concrete type; the others are the proper supertypes of that concrete type. A single heat
cube belonging to Player 2 is a `Heat<Player2>`, and therefore also a `StandardResource<Player2>`, a
`Resource<Player2>`, an `Owned<Player2>`, and a `Component`.

A component has no identity and no state beyond its concrete type. Several components may have the
same concrete type, but those components are indistinguishable: your twenty starting
`TerraformRating<Player1>` components differ from Player 2's twenty only by type, and not from each
other at all. The components in a world therefore form a multiset.

A component and its concrete type can sometimes be treated interchangeably, but only where
multiplicity is not relevant.

## 3. The class table

Every type belongs to one **class table**: a frozen, closed set of mutually compatible classes.
Types and classes from two different tables are never comparable, and every operation that would
mix them — subtyping, `glb`, `lub`, constraint matching — fails immediately rather than answering.

A table is built by loading classes from a ruleset and then freezing it. Loading a class as
**active** also activates every class name its declaration mentions anywhere, in any position — or
tries to: an authority-known name the ruleset has no active declaration for loads as **phantom**
rather than failing the load unless the name is a structural dependency bound. An unknown name is
always an error. A custom implementation's `requiredClassNames` are activation edges too. The one
exception is the argument of a
`Class<...>` metric: counting a class literal does not force the counted class to be active, which
is what lets rules ask about optional vocabulary. The argument must still be authority-known when
the mentioning declaration loads. Freezing then sweeps up every remaining declaration the
authority knows about and installs it as phantom.

So each name falls into exactly one of three buckets:

```text
active    a class in this table, with components, effects, defaults, and subtypes
phantom   authority-known, but inactive in this table
unknown   not in the authority at all
```

This is how a game's vocabulary gets fixed at setup. A two-player game on the Tharsis map without
Venus Next activates `Player1` and `Player2` but leaves `Player3` phantom; it leaves `SoloMode`,
`SoloOpponent`, `VenusStep`, and `VenusTag` phantom as well.

An unknown name is an error in every context — as a type, as a dependency bound, or inside a
metric. In particular, counting an unknown class literal is an error rather than zero, and the
declaration that mentions it should fail to load rather than failing later, when the expression is
first resolved.

A phantom class is a valid type, and a type is phantom when its root class or any dependency bound
is phantom. A refinement is deliberately not consulted: a requirement naming an inactive class is
unsatisfiable rather than type-changing. Phantom types and their class literals count zero, and are
otherwise inert:

- they have no components, effects, defaults, invariants, or enumerated concrete subtypes;
- optional and as-many-as-possible changes to them are a quantity-zero no-op;
- a mandatory change is dead, so a choice discards that branch, while an unavoidable dead change
  fails;
- they are excluded from automatic narrowing, and cannot fire as triggers.

Counting-zero is what makes the vocabulary shrink gracefully. The Hellas milestone Diversifier is
declared once, as

```json5
{ "id": "HM1", "requirement": "8 Class<Tag>(HAS Tag<Owner>)" },
```

which counts the distinct tag classes you have in play (section 11.4). Without Venus Next,
`VenusTag` is phantom and simply contributes zero, so the same declaration correctly describes the
milestone in a game where that tag does not exist.

An active class cannot have a phantom superclass or a phantom dependency bound; both are load
errors, so a table containing one never freezes. Excluding a phantom type is fine, though, because
a complement (section 10) constrains a bound without becoming the bound:

```text
Owned<!SoloOpponent>    in a multiplayer game: not phantom; the bound is still Anyone
```

Only active classes are enumerated: the table's class and name listings skip phantoms, which is why
playable-choice generation never offers them, and why the game can list exactly the milestones of
the chosen map as available to claim, even though no `Milestone` component exists until one is.

## 4. Class literals

For every authority-known class `Foo`, the type `Class<Foo>` exists. The contents of the angle
brackets are a single class name, never a full type expression:

```text
Class<Steel>            valid
Class<Class>            valid
Class                   valid; means Class<Component>
Class<Steel<Player1>>   invalid: the argument is not a bare class name
Class<Steel, Player1>   invalid: there is only one slot
Class<Class<Steel>>     invalid: same reason as Class<Steel<Player1>>
```

The class named in the angle brackets is not a dependency. A dependency would require a component
with type `Foo` to exist; a class literal instead requires the class named `Foo` to exist. That is
the whole point: `Class<Steel>` is available on turn one, when nobody has a steel cube.

Like other angle-bracketed types, class literals are covariant:

```text
Foo <: Bar  implies  Class<Foo> <: Class<Bar>
```

A class literal is concrete exactly when the named class is concrete — its dependencies are
irrelevant, because a class literal is not parameterized by them. This is the one place where "is
`Foo` concrete?" has a different answer for the class than for the type of the same name:

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

If `Foo` is concrete, exactly one component of type `Class<Foo>` exists in every world in which
`Foo` is active. If `Foo` is abstract, `Class<Foo>` is abstract and cannot have its own
corresponding components. Counting it nevertheless counts the class-literal components of all
concrete subclasses of `Foo`, by the usual subtype-counting rule. Thus a world always contains
exactly six components matching `Class<StandardResource>` — one each for megacredits, steel,
titanium, plants, energy, and heat — and none for the abstract `StandardResource` itself.

If the authority does not know `Bar`, neither `Bar` nor `Class<Bar>` is a valid type in any
context. If it knows `Bar` but this table does not activate it, `Class<Bar>` is a phantom type that
counts zero.

## 5. Dependencies

```pets
ABSTRACT CLASS Tile<Area>
```

`Tile` introduces a dependency whose upper bound is `Area`; further dependencies would follow it,
comma-separated. Any Pets type may serve as a bound — concrete or abstract, carrying dependencies of
its own or a refinement, even a class literal.

One type exists for each admissible subtype of the bound: `Tile<Area>`, `Tile<MarsArea>`,
`Tile<Tharsis_2_2>`, and so on. In each, the type's class — also called its **root class** — is
`Tile`. Bare `Tile` is valid and means `Tile<Area>`, while `Tile<Player1>` is invalid, since
`Player1` is not a subtype of `Area`.

Dependencies are covariant by design: from `LandArea <: MarsArea` it follows that
`Tile<LandArea> <: Tile<MarsArea>`. Such a type is concrete exactly when its class and every one of
its bounds is concrete, so `Tile<MarsArea>` is abstract twice over, `OceanTile<MarsArea>` is still
abstract (which area?), and `OceanTile<Tharsis_5_5>` is concrete.

Each dependency has a **key**: the class that first declared it, plus its 0-based position in that
class's own dependency list. The one above is keyed `Tile_0`; the owner dependency that
`ABSTRACT CLASS Owned<Anyone>` introduces is keyed `Owned_0`. Keys, not positions within a written
expression, identify dependencies, and a subclass that narrows an inherited dependency reuses its
key (section 6). Class literals are the exception to one-key-per-declaration: every literal slot uses
`Class_0`, because `Class` is the only class with such a slot and no other class may declare one.
That is why production is spelled

```pets
CLASS Production<Class<StandardResource>> : AutoLoad {
  ProductionPhase: StandardResource
}
```

rather than by giving `Production` a class-literal dependency of its own.

A **dependency path** is a list of keys addressing one bound inside a nested type. In the real
declaration

```pets
CLASS Neighbor<Tile<MarsArea>, MarsArea> [NBR]
```

the inner `MarsArea` sits at `Neighbor_0.Tile_0` and the outer one at `Neighbor_1`. Paths are how
the positions sharing one type variable are tracked and rewritten (section 13).

Dependencies also form a graph, with components as vertices and dependencies as directed edges from
dependent to target, each labeled by its key; ordinary directed-graph terminology applies. A target
must exist for as long as its dependent does, and is designated by its exact concrete type, having
no separate identity. Because same-typed components are indistinguishable, every concrete type
admitted by a bound must have an applicable `MAX 1` or `=1` invariant — declared on the target class
or supplied by a stronger aggregate bound. When the bound is abstract, this holds for every concrete
type it admits. The canon satisfies this deliberately: `Area` declares `HAS =1 This`, `Player`
declares `HAS =1 This`, and every card class is generated with `HAS MAX 1 ThatCard`. Without that
guarantee, `Animal<Player1, Pets<Player1>>` could not say *which* copy of Pets the cube is sitting
on, because there would be more than one.

This is a rule about worlds, not about types, and the type system does not enforce it: a table
violating it loads and freezes without complaint. The engine checks it once, when a game is built,
by enumerating every concrete type the bounds of every *active* class admit and requiring each to
have at least one applicable counting invariant whose maximum is 1. A class invariant that is not a
counting requirement is rejected at the same moment.

Unlike superclass cycles, dependency cycles between classes are legal, and the canon has one:

```pets
ABSTRACT CLASS Area {
  HAS =1 This
  HAS MAX 1 Tile<This>                                 // circular reference is un-ideal, but...
}

ABSTRACT CLASS Tile<Area>
```

An `Area` constrains the `Tile` that sits on it, and a `Tile` depends on an `Area`. Such a table
loads fine. What it costs is finiteness: operations that enumerate concrete types (section 12)
terminate only when the table's dependency closure is finite.

## 6. A dependency belongs to all subtypes

```pets
ABSTRACT CLASS Tile<Area>
CLASS CityTile : Tile
```

We do not say that `CityTile` "inherits" the `<Area>` dependency, because in Pets there is no
separate inheritance mechanism or choice involved: every `CityTile` is a `Tile`, and every `Tile`
has that dependency, so every `CityTile` has it too. It is the same dependency, with the same key
`Tile_0`, not a copy, redeclaration, or override. For every `T <: Area`:

```text
CityTile<T> <: Tile<T>
```

An inherited `This` specialization remains bound to the current subclass, and the canon leans on
this to express one of the game's fiddlier rules — that a resource cube can only go on a card that
collects *that kind* of resource:

```pets
ABSTRACT CLASS ResourceCard<Class<CardResource>>

ABSTRACT CLASS CardResource[CR] : Resource, Cardbound<ResourceCard<Class<This>>>
```

`CLASS Animal : CardResource` therefore has the bound `ResourceCard<Class<Animal>>`, and
`CLASS Microbe : CardResource` has `ResourceCard<Class<Microbe>>`, from that one declaration. Since
the Pets card is generated as `CLASS Pets : ActiveCard, ResourceCard<Class<Animal>>` and Ants as
`CLASS Ants : ActiveCard, ResourceCard<Class<Microbe>>`, the type `Animal<Pets>` resolves and
`Animal<Ants>` is a type error.

An explicitly written class literal is a fixed literal and remains unchanged in subclasses: had
`CardResource` been written as `Cardbound<ResourceCard<Class<CardResource>>>`, every subclass would
share the one bound instead of specializing it, and textual equality after resolving `This` does not
erase that distinction. This holds at any depth, and independently per position. No canon class
needs the deepest form, so here it is with invented names:

```pets
ABSTRACT CLASS Pair<Class<Left>, Class<Right>>
ABSTRACT CLASS Wrapper<Pair<Class<Left>, Class<Right>>>
ABSTRACT CLASS Mixed : Left, Right, Wrapper<Pair<Class<This>, Class<Mixed>>>
CLASS MixedLeaf : Mixed
```

```text
MixedLeaf      resolves to      MixedLeaf<Pair<Class<MixedLeaf>, Class<Mixed>>>
```

Since `Tile` and `Tile<Area>` are the same type, either gives `CityTile` the same resolved
supertype. Their authored spellings remain distinct for the repetition matching of section 13.

## 7. Narrowing a dependency bound

```pets
CLASS GreeneryTile[GT] : Tile<MarsArea>
```

Every `GreeneryTile` is a `Tile`, so it has the same dependency that `Tile` introduced. The
declaration narrows that dependency's upper bound from `Area` to `MarsArea`; it does not introduce,
copy, or override a dependency. This is the type system carrying a real rule: greenery tiles go on
Mars, never on a `RemoteArea` — of which the canon has several, each brought into being by the card
that needs it, such as the `CLASS Area021 : RemoteArea` that comes with Phobos Space Haven. For
every `T <: MarsArea`:

```text
GreeneryTile<T> <: Tile<T>
```

If the narrowed bound is abstract, a subclass may narrow it again. The owner dependency introduced
by `Owned<Anyone>` is narrowed to `Owner` by `Cardbound` and to `Player` by `Card`, and since
`Player` is still abstract, a subclass of either could narrow it further. If the bound is an
unrefined concrete type, it cannot be narrowed further by nominal subtyping or dependency narrowing.

When a class has several supertypes that each narrow the same inherited dependency, the class's own
bound is the greatest lower bound of theirs (section 12). `CardResource` is exactly this case:

```pets
ABSTRACT CLASS Owned<Anyone> {
  ABSTRACT CLASS Resource[RES]
}

ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>

ABSTRACT CLASS CardResource[CR] : Resource, Cardbound<ResourceCard<Class<This>>>
```

`Resource` leaves `Owned_0` at `Anyone`, while `Cardbound` narrows it to `Owner`, so a
`CardResource` gets `glb(Anyone, Owner)`, which is `Owner`. That is deliberate: card resources must
be ownable by the non-player `SoloOpponent`, but not by anything wider. If that bound did not
exist, the class could not be loaded.

## 8. Introducing another dependency

```pets
ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>
```

`Cardbound` has two dependencies: the same dependency required by being an `Owned`, keyed `Owned_0`
and narrowed to `Owner`, and the new dependency introduced by `Cardbound`, keyed `Cardbound_0` and
bounded by `CardFront`. The keys determine canonical declaration order — inherited dependencies
first, in supertype order, then newly declared ones — so a microbe cube on Ants writes out as
`Microbe<Player1, Ants<Player1>>`, owner first.

Dependencies and supertypes are each written as comma-separated lists. Section 9 gives the rules
that match written bounds to dependency keys.

## 9. Writing dependency bounds

```pets
ABSTRACT CLASS Area
ABSTRACT CLASS MarsArea[MA] : Area
ABSTRACT CLASS RemoteArea[RA] : Area
ABSTRACT CLASS LandArea[LA] : MarsArea
CLASS Tharsis_2_3 : LandArea                 // generated from the map data

ABSTRACT CLASS Tile<Area>
CLASS OceanTile[OT] : Tile<MarsArea>, GlobalParameter
ABSTRACT CLASS Adjacency<Tile, Tile>
```

A written argument does not have to be narrower than the current bound. Each argument is
*intersected* with the bound it matches, and the result is the intersection:

```text
OceanTile<Area>         resolves to    OceanTile<MarsArea>
OceanTile<Tharsis_5_5>  resolves to    OceanTile<Tharsis_5_5>, which is concrete
OceanTile<RemoteArea>   error: RemoteArea and MarsArea have no common subtype
```

Arguments are matched to dependencies greedily, left to right: each argument takes the first
dependency, in canonical order, that it has a non-empty intersection with and that no earlier
argument already took. Order among unambiguous arguments therefore does not matter — a greenery tile
may be written `GreeneryTile<Player1, Tharsis_2_3>` or `GreeneryTile<Tharsis_2_3, Player1>` — but
among same-bounded dependencies it does. `Adjacency` has two dependencies both bounded by `Tile`:

```text
Adjacency                    resolves to    Adjacency<Tile, Tile>
Adjacency<CityTile>          resolves to    Adjacency<CityTile, Tile>
Adjacency<Tile, CityTile>    resolves to    Adjacency<Tile, CityTile>
```

An argument that matches no remaining dependency is an error, which covers both supplying too many
arguments and supplying any at all to a class with no dependencies:

```text
OceanTile<Tharsis_5_5, Tharsis_5_6>    error: only one dependency to match
Generation<Player1>                    error: Generation has no dependencies
```

Every type therefore has two written forms. The **full form** states every bound explicitly; the
**minimal form** omits bounds that equal the root class's own bounds, except where retaining such a
placeholder is necessary to make greedy argument matching round-trip:

```text
full                          minimal
Tile<Area>                    Tile
OceanTile<MarsArea>           OceanTile
Adjacency<CityTile, Tile>     Adjacency<CityTile>
Adjacency<Tile, CityTile>     Adjacency<Tile, CityTile>
```

The minimal form is what types print as, and re-resolving it yields the same type except where a
complement's unwritten domain makes even the full form lossy (section 15.9).

### 9.1 Default bounds

A `DEFAULT` declaration supplies bounds for the arguments an author *omits*. It is a rule about
expressions, not a rule about types: it changes what a written `CityTile` means, and never changes
which types exist or how they relate.

```pets
DEFAULT Owned<Owner>                                                // all usages
DEFAULT +CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>   // gains only
DEFAULT -Owed<Class<Megacredit>>.                                   // removals only
```

The three kinds — all-usages, gain-only, remove-only — are gathered independently, and the gain and
remove kinds additionally carry an intensity (`!`, `?`, `.`). A default of a given kind for a given
dependency key is inherited from the *nearest* superclasses that declare one: any declarer that is a
proper superclass of another declarer drops out, and whatever survives is combined by `glb` for
bounds. Intensities are not combined; two surviving incomparable superclasses declaring different
intensities is an error.

One special case matters, because it interacts with contextual `Owner` (section 13.1). Writing
`Owner` in a default's arguments keeps the literal `Owner` bound rather than normalizing it to the
class's own narrower bound for that key, so that the contextual binding still has something to
replace when a component is created.

## 10. Complement bounds

A written argument may be complemented with `!`, which excludes one narrower type from a bound
without replacing it. This is how the game says "your opponents":

```pets
ABSTRACT CLASS Anyone[ANY] {
  ABSTRACT CLASS Owner {
    ABSTRACT CLASS Player : Actor {
      CLASS Player1[P1], Player2[P2], Player3[P3], Player4[P4], Player5[P5]
    }
  }
}
ABSTRACT CLASS Owned<Anyone>
```

Toll Station reads "increase your MC production 1 step for each space tag your *opponents* have",
and is declared:

```json5
// Toll Station
{ "id": "099", "cost": 12, "tags": [ "SpaceTag" ],
  "immediate": "PROD[1 / SpaceTag<!Owner>]" },
```

Inside a card's effects, `Owner` stands for whoever owns the card, so on Player 1's copy this
becomes `SpaceTag<!Player1>`: every space tag whose owner is not Player 1. Philares uses the same
device to mean "next to someone else's tile":

```json5
"Adjacency<OwnedTile<Owner>, OwnedTile<!Owner>> BY Anyone: StandardResource",
```

and Viron, which lets you use a card action a second time, uses it to mean "any action card other
than Viron itself":

```json5
"actions": [ "-> UseAction<ActionCard(HAS ActionUsedMarker<!Viron>)>" ],
```

A complement has a **domain** — the bound it constrains, which is `Owner` for `SpaceTag` and
`Anyone` for `OwnedTile`, since owned tiles may belong to the non-player `SoloOpponent` — and an
**excluded type**, which must be a subtype of that domain. `Owned<!CityTile>` is an error because
`CityTile` does not narrow `Anyone`. Excluding the whole domain (`Owned<!Anyone>`) is accepted and
simply admits nothing.

A complemented type is always abstract, even when only one concrete type survives, and enumeration
of its concrete subtypes filters the excluded ones out:

```text
SpaceTag<!Player1>      in a two-player game: concrete subtypes are Player 2's space tags
```

A complement never stands alone. `!Player1` has no type of its own and cannot be resolved as one; a
complement is only meaningful against a domain. Where the engine needs to test a candidate against
a complement outside a type expression, it supplies the domain explicitly, and the test is: the
candidate narrows the domain, and does not narrow the excluded type.

Two complemented bounds combine only when they exclude the very same type:

```text
Owned<!Player1> glb Card<!Player1>      Card<!Player1>
Owned<!Player1> glb Owned<!Player2>     no common subtype
```

Within a class signature a complemented bound can occupy a type variable's positions like any other
bound, and narrowing is applied before exclusion, so a complement that excludes something the
variable has already ruled out simply disappears (section 13.3):

```text
Cardbound<Player1, !CardFront<Player2>>    resolves to    Cardbound<Player1>
```

In every other scope the opposite holds: an expression written with `!` neither introduces a
variable nor refers to one, so two occurrences of `OwnedTile<!Owner>` in one effect are two
independent choices.

A complement's domain is not written out. Both written forms of a complemented bound show only the
`!`-marked exclusion, so when a `glb` or a type variable has narrowed the domain below what the
printed root class implies, that narrowing is lost on re-resolution; section 15 records this.

Finally, as noted in section 3, excluding a phantom type does not make the containing type phantom;
only the domain counts.

## 11. Refinement types

For a non-refinement type `T` and requirement `R`, `T(HAS R)` is a refinement type: `T` is its
unrefined type and `R` is its refinement. The refinement type selects the components of `T` that
satisfy `R`, and is always abstract and always a subtype of `T`.

This is how the game's placement rules and conditional effects are written. A few real ones:

```text
CardFront(HAS 20 CardCost)                       CrediCor: a card costing 20 or more
CardFront(HAS CardRequirement)                   Tactician: a card with a printed requirement
LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)   where a city may be placed
MarsArea(HAS 8 MarsRow)                          Polar Explorer: row 8 or 9, the polar rows
MarsArea(HAS MapBonus<Class<Steel>>
         OR MapBonus<Class<Titanium>>)           Mining Guild: an area with a steel or titanium bonus
```

`T` itself need not be abstract. Refining a concrete type creates a proper abstract subtype of it
without violating the finality of concrete classes: no subclass is introduced, and the refinement
type has no exact components of its own.

### 11.1 Testing a candidate

Components have no features beyond their concrete type, so a refinement that could only inspect the
candidate would be useless. Instead the candidate is substituted into `R`, which may then ask
anything of the world. Substitution follows the bound-matching rules of section 9: every type
expression in `R` is offered the candidate as a written argument.

Take the city-placement rule, which is declared as the default way to gain a `CityTile`:

```pets
CLASS CityTile[CT] {
  DEFAULT +CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>
}
```

Testing the candidate area `Tharsis_2_3` offers it to `Neighbor<CityTile<Anyone>>`. Recall
`CLASS Neighbor<Tile<MarsArea>, MarsArea>`: greedy matching gives the written `CityTile<Anyone>` the
`Neighbor_0` slot and the appended candidate `Neighbor_1` — which is where it would have landed
regardless, since an area has no intersection with `Tile<MarsArea>`. The requirement asked of the
world is therefore

```text
MAX 0 Neighbor<CityTile<Anyone>, Tharsis_2_3>
```

"no city, belonging to anyone, is adjacent to this area" — the actual rule, arrived at by ordinary
argument matching.

An expression with no room for the candidate fails the whole test, so `LandArea(HAS 8 BuildingTag)`
is satisfied by nothing: a building tag's dependencies are an `Owner` and a `CardFront`, and an area
is neither. Within a class literal's refinement, unmatched expressions are left alone instead — that
is what makes the represented-type reference of section 11.4 work.

Satisfaction is therefore a world-dependent narrowing operation in two steps: the candidate must be
a subtype of `T`, and the world must satisfy the substituted `R`. This lets a refinement type be
counted, or narrowed to a concrete choice, without asserting a static relationship that the type
system cannot establish by itself.

### 11.2 Static rules

- The subtypes of `T(HAS R)` are the types `S(HAS R)` for `S <: T`. The refinement itself may not
  vary: `CardFront(HAS 20 CardCost)` is not a subtype of `CardFront(HAS 10 CardCost)`, even though
  the former requirement implies the latter.
- Concrete types that satisfy `R` are *not* subtypes of `T(HAS R)`; satisfaction is decided outside
  the static type system.
- A narrower type carrying the exact same refinement is decided statically. A different refinement
  is statically unrelated. Testing an unrefined candidate against a refined wider type asks the
  world whether the candidate satisfies the requirement; a context-free check raises if structural
  matching cannot already reject it.
- Intersection preserves refinements: a greatest lower bound conjoins two refinements, or keeps the
  only one present. It conjoins them only when they agree about forgiveness (section 11.3), since
  `HAS R1` and `HAS? R2` have no common conjunction — `HAS? (R1 AND R2)` would let the escape
  clause discard the strict `R1`, and `HAS (R1 AND R2)` would discard the escape clause. Like any
  other pair of types with nothing below them, they simply have no greatest lower bound.
- An upper bound retains a refinement only when both operands carry that exact refinement. If only
  one side is refined, or the refinements differ, their common upper bound is unrefined.

### 11.3 Forgiving refinements

A candidate satisfies `T(HAS? R)` when it satisfies the ordinary `T(HAS R)`, or when the world
contains no component satisfying `T(HAS R)` at all. That is, it tests

```text
R  OR  MAX 0 T(HAS R)
```

The second arm is its escape clause; it does not change the static rules above. The greenery
placement rule needs exactly this, since "adjacent to one of your own tiles" has to give way when
you own no tiles at all:

```pets
CLASS GreeneryTile[GT] : Tile<MarsArea> {
  // HAS? specifies a requirement with an "escape clause": the requirement is upheld as long as
  // there is at least one component that satisfies it. If none do, it's dropped.
  DEFAULT +GreeneryTile<LandArea(HAS? Neighbor<OwnedTile>, MAX 0 Tile)>
}
```

Note that `MAX 0 Tile` — no tile is there already — sits *inside* the forgiving refinement rather
than beside it. Areas are limited to one tile anyway, but including it here means the escape clause
considers occupancy too: the rule that gets dropped is "adjacent to your tile *and* empty", so a
player whose only adjacent areas are full may place anywhere empty.

### 11.4 The represented-type reference

A class literal's `HAS` requirement refers back to the class it represents. When testing candidate
`Class<S>` against `Class<T>(HAS R)`, occurrences rooted at `T` within `R` are narrowed to `S`. Thus
`Class<SpaceTag>` satisfies `Class<Tag>(HAS Tag<Player2>)` exactly when the world contains
`SpaceTag<Player2>`, and counting that metric yields the number of distinct tag classes Player 2
has. That is precisely what the Diversifier milestone needs:

```json5
{ "id": "HM1", "requirement": "8 Class<Tag>(HAS Tag<Owner>)" },
```

and Diversity Support asks the same question about resource types:

```json5
"requirement": "9 Class<Resource>(HAS Resource<Owner>)",
```

The class-token components themselves remain unowned; the requirement is tested against instances of
the represented component type. Ordinary dependency constraints, including complements, remain
available — Aridor's "a tag you don't already have", which ignores event cards, is authored as

```json5
"effects": [ "Class<Tag>(HAS Tag<Owner, !EventCard>): PROD[1]" ],
```

(Aridor is on the not-yet-working list for an unrelated reason: it needs refined class-metric
triggers.)

## 12. Closed-world type operations

A frozen class table makes the following operations decidable in ways that an open world could not.
All of them are confined to one table; see section 3.

**Enumeration.** For any type `T`, Pets can list

```text
concreteSubtypes(T) = { U | U is concrete and U <: T }
```

by choosing every concrete root class below `T`'s root and every admissible combination of concrete
bounds, as constrained by any complement or type variable. The result may be large, but is finite
whenever the table's dependency closure is. A game engine uses it to *generate* legal choices rather
than merely validate one already made: an instruction that says `OceanTile` picks up the class's
`DEFAULT +OceanTile<WaterArea>`, and enumerating that yields one concrete type per water area on the
map — the placements to offer. A refinement is dropped rather than applied: by section 11.2 a
refinement type has no concrete subtypes, so a state-aware consumer enumerates the unrefined type
and then filters with the separate refinement test.

**Automatic narrowing** takes the case where enumeration would yield exactly one type, and is
strict: the root class must have exactly one concrete subclass *and* every dependency must itself
have exactly one concrete narrowing.

```text
TerraformRating         solo game: Player has one concrete subclass, so this is
                        automatically TerraformRating<Player1>
TerraformRating         four-player game: four narrowings, so somebody must choose
OceanTile<WaterArea>    twelve narrowings on Tharsis, so the player chooses
```

Unlike enumeration, it hands back a single answer with nothing left for a consumer to filter, so it
cannot afford to ignore anything the type says. A complement bound narrows to whatever single
concrete type survives its exclusion, and a refinement — on the type or on any of its bounds — is
tested against the candidate, which makes automatic narrowing state-dependent wherever a
refinement is present. A candidate the refinement rejects is not a narrowing, and the result is
always concrete or absent. A refinement does not turn several structurally possible root classes
into an automatic choice; structural uniqueness is checked first.

`Type.allConcreteSubtypes()` ranges across every concrete root class below the receiver's root.
`Type.concreteSubtypesSameClass()` and `Class.concreteTypes()` instead enumerate only types whose
root class is exactly the receiver or that class. Whole-table enumeration, subclass enumeration,
and every operation built on them require the class loader to have been frozen; calling them while
the table can still grow fails rather than capturing an incomplete snapshot.

**Greatest lower bound** keeps its ordinary order-theoretic meaning, but need not exist. Two types
with no common subtype simply have none, and every operation built on `glb` — matching a written
bound, intersecting two constraints, combining narrowed supertypes — reports that as an error rather
than inventing an intersection type:

```text
OceanTile<Tharsis_5_5> glb OceanTile<Tharsis_5_6>       none
```

Between classes the bound is a nominal class or nothing; Pets never manufactures a structural
intersection type. A class qualifies as the intersection of its direct superclasses when no other
class in the table subtypes all of them without also subtyping it, and `glb` looks for the unique
such class listing both operands among its direct superclasses. `OwnedTile` is the canon's example:

```pets
ABSTRACT CLASS Tile<Area> {
  ABSTRACT CLASS OwnedTile : Owned
}
```

`OwnedTile` subtypes both `Owned` and `Tile`, and every class that subtypes both also subtypes
`OwnedTile`, so it is their greatest lower bound. This doubles as a modeling check: the Landlord
award is declared `{ "id": "MA1", "metric": "OwnedTile" }`, so a tile class that is both an `Owned`
and a `Tile` but forgets to extend `OwnedTile` would silently stop counting for Landlord. Commercial
District's `CLASS CdTile : SpecialTile` gets it right by extending `SpecialTile`, which is already
an `OwnedTile`.

**Upper bound (`lub`)**, in contrast, always returns a common upper bound — `Component` if nothing
closer does. With multiple inheritance a mathematical least upper bound need not exist: there may be
several incomparable minimal common upper bounds, and the implementation picks one by a heuristic.
Multiple inheritance is ordinary here — `OceanTile` is both a `Tile` and a `GlobalParameter` — which
is why the distinction matters.

At dependency level, `lub` lifts the same operation through ordinary type bounds. A complement is
kept only when the other bound already satisfies it, or when both complements exclude the exact
same type; otherwise the result falls back to the complement's unexcluded domain.

## 13. Implicit type variables

Sections 13 and 14 intentionally range beyond the type system into effects, instructions, and task
splitting so that one mechanism has a coherent end-to-end meaning.

An abstract type is a choice not yet made. In certain **choice-producing** positions, an authored
expression that resolves to an abstract type introduces an implicit **type variable** whose upper
bound is the type denoted by that expression. Within that variable's scope there are **eligible**
positions where an exact repetition of the same authored expression indicates the variable rather
than opening a new choice. Narrowing the variable narrows every one of its occurrences to the same
type.

This adds no new kind of choice. A lone abstract expression in a choice-producing position was
always a choice bounded by that expression; a variable is that same choice, made referrable for the
length of a scope. The reference is the new part. Pets has no separate syntax for naming type
variables, so it says "the same one" by writing the expression again.

That the game already reads repetition this way can be checked against the printed cards. The icon
grammar has one glyph for "a standard resource, unspecified", and nothing that marks one wildcard as
distinct from another: no subscript, no prime, no "a different one". Manutech uses that glyph twice,
inside a production box on the trigger side and alone on the result side:

```json5
// Manutech
"effects": [ "PROD[StandardResource]: StandardResource" ],
```

Nobody reads that as two unrelated resources, and it is hard to see what the icons *could* do to say
that they were. Where the grammar wants a wildcard constrained it spends notation on a qualifier
instead, and when it has no qualifier to spend, the card stops being expressible at all. Robinson
Industries wants "your lowest production" — a wildcard the world narrows rather than the player —
and the printed card has to fall back on words, while the canon has to fall back out of Pets
entirely:

```json5
// Robinson Industries
"actions": [ "4 -> GainLowestProduction" ],
"components": [ "CLASS GainLowestProduction : Owned, Custom" ],
```

So the grammar spends real effort on narrowing a wildcard and none at all on re-opening one.
Repetition is not available to mean "another one", which leaves it free to mean "the same one". Pets
is not inventing a coupling rule here; it is declining to break one the game already relies on. That
is what justifies the rule's real cost — that meaning comes to depend on authored spelling.

A repetition is matched on the authored expression tree, before defaults or other preprocessing
insert expressions, and never on resolved types. Two occurrences are the same expression when their
parsed trees are structurally equal, once external source translation has produced canonical class
names and each explicitly written bound has been associated with its dependency key. Whitespace,
redundant parentheses, and the order of unambiguous bounds are therefore all irrelevant, while every
other authored difference matters: omitted bounds stay omitted, and refinements and complements are
not rewritten merely because they are logically equivalent. No pair below shares a variable, even
where later resolution or context makes its two members equal:

```pets
Tile                  Tile<Area>
OceanTile             OceanTile<MarsArea>
Owner                 Anyone
```

Keeping matching this literal makes a variable something one can find by reading the source, and
stops defaults or normalization from silently coupling independent choices. The represented-type
reference of section 11.4 is a separate binding mechanism rather than repetition matching: within
the `HAS` requirement of `Class<T>`, an occurrence rooted at `T` refers to the represented class even
when it carries dependency arguments, as in `Class<Tag>(HAS Tag<Owner>)`.

Among expressions that do match, these rules decide which positions belong to one variable:

- Only an expression whose resolved type is abstract introduces one; a concrete expression denotes
  no choice.
- Within a scope (section 13.2), all eligible maximal occurrences of the same expression belong to
  one variable.
- A smaller repeated expression nested inside every occurrence of a larger one introduces no
  variable of its own: repeating `CardFront(HAS BioTag)` gives a single variable bounded by that
  whole expression rather than an independent `BioTag` choice. Where the smaller expression also
  occurs on its own, the nested occurrences belong to its variable too.
- A variable introduced by a supertype is the same variable in its subclasses. Coincidentally
  matching text elsewhere does not join it.

### 13.1 Contextual bindings

**`This`.** `This` is closely related but is not a variable inferred from repetition. It is a
reserved, explicitly named contextual binding belonging to a class declaration. One occurrence is
enough to refer to it, and every `This` occurrence in that class's signature or body refers to the
same binding. `This` occurrences are therefore excluded from repetition matching; repetition adds
no second constraint.

In an abstract class, `This` remains late-bound as the declaration applies to subclasses. For a
component whose exact concrete type is `C<Args>`, an ordinary `This` occurrence binds to `C<Args>`,
while `Class<This>` binds to `Class<C>` because a class literal contains a class name rather than a
type with dependencies. Both appear together in the event-card rule that moves a played event to
your played-events pile:

```pets
ABSTRACT CLASS EventCard {
  This: PlayedEvent<Class<This>> FROM This
}
```

For a specific event card owned by Player 1, the two ordinary `This` occurrences become that full
concrete type while `Class<This>` becomes just the card's class literal, so the pile records which
card was played without depending on a card that no longer exists. An explicitly authored
`Class<EventCard>` would remain `Class<EventCard>` in every subclass; it does not acquire `This`
semantics merely because it temporarily resolves to the same class literal.

The trigger forms `This:` and `-This:` are self-event selectors, not ordinary type subscriptions.
They respond only to the occurrences of the effect-bearing concrete type gained or removed by the
current change. A change of `N` copies scales the triggered instruction by `N`; other copies already
present do not multiply it. Thus these two hypothetical effects would not be equivalent inside
`CLASS TerraformRating`, where a player holds twenty-odd components at once:

```pets
This: VictoryPoint
TerraformRating: VictoryPoint
```

The first fires for the terraform-rating steps gained right now. Signature-level `This` paths remain
distinct from implicit variables: a variable is a choice waiting to be narrowed, while the enclosing
concrete class supplies `This`.

**`Owner`.** Within an effect belonging to a concrete owned component, `Owner` refers to that
component's exact owner. If a card belongs to `Player1`, for example, its inherited effect
`This: VictoryPoint<Owner>` becomes `This: VictoryPoint<Player1>`. One occurrence is enough, and the
binding applies independently of repetition-created variables. In class signatures and other
context-free type expressions, `Owner` remains the ordinary abstract class and can participate in
the rules below like any other expression. Effects without an owning component may leave it abstract
until execution supplies a contextual player; the engine rules for that fallback are recorded in
[Engine](ENGINE.md).

### 13.2 Scopes

Implicit variables can span only the following related source regions:

- **Within a class signature.** Two explicitly written bounds belong to one variable when they are
  the same expression *and* bind the same dependency key. This is how the canon says that a tag, or
  a resource cube, belongs to whoever owns the card it sits on:

  ```pets
  // Repeating Owner makes the Cardbound's owner and the CardFront's owner one variable.
  ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>
  ```

  There is one `Owner` choice here: both occurrences bind `Owned_0`, the nested one through
  `CardFront` and the other through the supertype. Identical spellings binding different keys stay
  separate.

  Binding the same key is the only rule this scope adds. Which occurrences are eligible, and which
  of several nested ones a variable covers, follow the same rules as everywhere else: whole authored
  expressions, maximal ones preferred, abstract ones only. `Cardbound` happens to repeat a bare
  class name, but in a signature repeating a bound that carries arguments, the variable is that
  whole bound, not the names inside it. When a subclass introduces a variable sharing a position
  with one it inherits, the two merge into a single variable over the union of their positions.
  Section 15 records that matching here is narrower than this today.
- **From a class signature to its effects.** A variable introduced in the dependency signature
  extends to the same expression in each of that class's effects, so narrowing the component through
  that dependency narrows its effects the same way. Playing a card relies on it:

  ```pets
  CLASS PlayCard<Class<CardBack>, Class<CardFront>> : Owned, Signal {
    This:: CheckCardRequirement<Class<CardFront>>
    This:: HandleCardCost<Class<CardFront>>
  }
  ```

  Choosing which card to play narrows `Class<CardFront>` once, and the requirement check and the
  cost calculation are both narrowed to that same card.

  Like the other scopes, this one narrows only at the occurrences the variable covers, and a
  disagreement among them is an error rather than a silent choice. Section 15 records that today it
  is done by class-name substitution instead.
- **From a trigger to its instruction.** A match that narrows a trigger expression applies the same
  narrowing to every occurrence of it in the instruction. Awards are scored this way, by an effect
  on `Player`:

  ```pets
  MeasureAward<Award>:: TallyAward<This, Award>
  ```

  Whichever award is being measured is the one tallied. This is the behavior historically called
  *trigger specialization*; under this terminology the trigger match narrows the variable and the
  instruction holds another of its occurrences.
- **Within one action, `THEN`, or atomic instruction.** The cost and result of an action, the stages
  of one `THEN`, and distinct operand roles of one atomic instruction are all related regions. A
  transmutation deliberately excludes its complete source and destination expressions — those are
  independent choices — but matching proper subexpressions inside them can share a variable. Compare
  two real cards. Kaguya Tech turns one of your greeneries into a city, on the same hex:

  ```json5
  // Repeated LandArea occurrences must specialize to the same concrete area.
  "immediate": "PROD[2], ProjectCard, CityTile<LandArea> FROM GreeneryTile<LandArea>",
  ```

  Market Manipulation raises one colony's trade track and lowers another's, and must *not* couple
  them:

  ```json5
  "immediate": "ColonyProduction FROM ColonyProduction",
  ```

Outside a class signature, a scope is a set of *regions* — trigger and instruction, cost and result,
the stages of a `THEN`, the two operands of a transmutation — and a repetition refers back only when
its occurrences lie in at least two different ones. Three choice-producing occurrences inside a
single `THEN` stage, or inside the trigger alone, therefore introduce three separate variables:
three writings of the same type and nothing more.

Two exclusions cut across every scope. First, the root occurrence of each entry in a class's own
dependency declaration *declares* a dependency — a keyed variable of its own — so it always
introduces a fresh one and never refers back. The two `Tile` dependencies of `Adjacency<Tile, Tile>`
therefore stay independent — adjacency relates two tiles on *different* areas — as do the top-level
and nested `MarsArea` occurrences in `Neighbor<Tile<MarsArea>, MarsArea>`, where the whole point is
that the tile is on one area and the neighbor is another. Second, a position inside a requirement or
refinement is eligible but not choice-producing: testing two facts does not assert that one witness
satisfies both, so requirement-only repetition introduces nothing. Such an occurrence may still
refer to a variable introduced by a matching occurrence outside requirements — with one further
restriction, that a *counted* expression is not an occurrence at all. The `T` of `2 T`, `MAX 0 T`,
or an award's metric neither introduces a variable nor refers to one, wherever it appears; only the
expressions nested inside it can.

Nested occurrences in sibling branches of one `<...>` list are meant to be independent as well, but
today they share a variable: a hypothetical `Adjacency<Tile<MarsArea>, Tile<MarsArea>>` would couple
its two `MarsArea` occurrences because both bind `Tile_0`, and any class written with two identical
class literal bounds would couple its two because every class literal slot binds `Class_0`. See
section 15.

Comma-separated instructions and alternative `OR` arms are not in scope for one another. A variable
introduced by an enclosing class, effect, action, or `THEN` may still have occurrences inside those
instructions or arms, and is applied before they separate.

### 13.3 Narrowing a variable

A type variable begins at the upper bound denoted by its source expression. A proposed narrowing
supplies one subtype of that bound and substitutes it at every occurrence of the variable. The
containing expressions are then resolved and validated. The narrowing is rejected if any occurrence
cannot accept it or if different occurrences would require different types.

Within a class signature, this is enforced whenever a type is formed. Every position of one variable
is intersected with every other, and the result is written back to all of them; because a position
may belong to a variable whose other positions are nested inside it, the process repeats until it
reaches a fixed point. Narrowing any one occurrence therefore narrows the rest automatically, and
disagreement is an error rather than a silent choice:

```pets
ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner> {
  ABSTRACT CLASS Tag : Atomized {
    CLASS SpaceTag[SPT]
  }
}
```

```text
SpaceTag<Player1, CardFront>            same type as SpaceTag<CardFront<Player1>>
SpaceTag<Player1, CardFront<Player2>>   error: the variable's occurrences disagree
```

Variables survive inheritance, and enumeration respects them. There is no such thing as Player 1's
space tag on Player 2's card, so enumerating `SpaceTag` in a two-player game yields one concrete
type per (owner, card-they-own) pair rather than one per (owner, any card) pair:

```text
SpaceTag<Player1, SpaceElevator<Player1>>
SpaceTag<Player2, SpaceElevator<Player2>>
...one such pair per player, per card front, never crossed
```

Outside a class signature, a variable remains unbound while more than one concrete binding is
possible. A composite instruction must not be split across a boundary crossed by an unbound
variable — Kaguya Tech's `CityTile<LandArea> FROM GreeneryTile<LandArea>` cannot be executed as two
independent halves, or the city could land on a different hex than the greenery it replaced. Once
earlier work or an enclosing binding fixes the variable, its value is substituted into all later
occurrences; ordinary task splitting may then continue. This rule couples only the shared choice,
not the execution of otherwise sequential stages.

## 14. Scalar variables

`X` is the one kind of variable Pets spells out. In a choice-producing count position, an authored
`X` introduces a **scalar variable** denoting one positive integer. Eligible repetitions in the
scopes of section 13 refer to that same variable, and a coefficient such as `3X` denotes three times
its value. Sell Patents — discard any number of cards, gain that many megacredits — is the pure case:

```json5
{ "id": "SELL", "action": "X ProjectCard -> X" },
```

Energy Market shows a coefficient, since each energy costs two megacredits:

```json5
"actions": [ "2X -> X Energy", "PROD[Energy] -> 8" ],
```

and a floater-spending discount shows one on a payment:

```json5
"PlayTag<Class<VenusTag>>: (-X Floater<This>! THEN -3X Owed.) OR Ok"
```

The scopes and splitting rules of section 13 apply unchanged, and nobody reads `X ProjectCard -> X`
as two unrelated numbers either: section 13 is that same convention applied to a position holding a
type rather than a count. A lone `X` in a count position is still a scalar variable; it simply has no
other occurrences. The `X` modifier on a trigger such as `X This:: Accept<Class<Resource>>` instead
retains its ordinary trigger meaning and introduces no scalar variable. Variable identity follows
the authored occurrences and scopes, not traversal-order pairing.

## 15. Known divergences

Each item below is current implementation behavior that this walkthrough would otherwise describe
differently. They are recorded here rather than smoothed over.

### 15.1 Sibling nested bounds share a variable

Section 13.2's same-key rule is applied to nested occurrences in sibling branches of one `<...>`
list, so a declaration like `Adjacency<Tile<MarsArea>, Tile<MarsArea>>` couples choices that were
meant to stay independent. With class literals the coupling is easy to miss, since all class
literal slots share the key `Class_0`: had `PlayCard<Class<CardBack>, Class<CardFront>>` instead
been written with the same literal twice, those two slots would share one variable. The intent is
that only a class's *own* repeated writing of a bound at distinct positions of the same inherited
dependency should share one.

### 15.3 Complement narrowing accepts wider abstract candidates

The test is "narrows the domain and does not narrow the excluded type", so `SpaceTag` — whose owner
is still the abstract `Owner` — counts as narrowing `SpaceTag<!Player1>` even though it admits
`SpaceTag<Player1>`. The rule behaves as intended for concrete candidates.

### 15.6 Class signatures match only bare class names

Section 13.2 says a signature adds only the same-key requirement to the shared matching rules; in
fact it uses a separate mechanism in which only a bare class name is an occurrence. A repeated bound
carrying arguments, a refinement, or a `!` never becomes a variable as a unit, so matching reaches
past it to the names inside. In the invented
`ABSTRACT CLASS Thing<Holder<Card<Owner>>> : Keeper<Card<Owner>>`, the two `Card<Owner>` bounds
should be one choice; instead only their two `Owner`s are coupled, leaving the two cards free to
differ. Abstractness is not consulted either, though a variable over concrete positions has no
effect. Section 15.1 is the other half of this: one shared matching mechanism would fix both.

### 15.7 Signature-to-effect narrowing is class-name substitution, not a variable

Section 13.2's second bullet describes one variable spanning signature and effects; what runs is a
substitution built by comparing every bound of the class's own type — nested bounds included — with
the component's, and rewriting every occurrence of each differing class name anywhere in the
effects, arguments and requirements alike. It therefore rewrites expressions the author never
wrote alike, and when one name would map to two different replacements it drops that substitution
silently instead of reporting the disagreement a class signature would report.

### 15.9 A complement's domain is not written out

Both written forms of a complemented bound show only the exclusion, so a domain narrowed by `glb`
or by a type variable below what the printed root class implies is lost when the printed form is
re-resolved. This affects the full form too, and it is one of several signs that a complement is not
really the simple thing section 10 presents it as.

### 15.13 A nested complement does not block repetition matching

Section 10 says an expression written with `!` neither introduces nor refers to an implicit
variable outside a class signature. The matcher checks only whether the candidate expression itself
is complemented, so the root `OwnedTile<!Owner>` remains eligible even though its argument is
complemented. Repeating that complete expression across regions therefore shares a variable today.

### 15.14 Matching does not canonicalize argument order

Section 13 matches authored expressions after associating their explicitly written bounds with
dependency keys, so two unambiguous argument orders are meant to match. Today the matcher compares
raw expression trees, whose argument lists remain ordered: `GreeneryTile<Player1, LandArea>` and
`GreeneryTile<LandArea, Player1>` therefore introduce separate variables.
