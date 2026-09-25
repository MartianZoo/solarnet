# The Pets type system: a specification

This document specifies the type system of Pets: what classes and types are, what a type denotes,
and the judgments made about types — subtyping, narrowing, meets, enumeration and inhabitance. It
says what these mean. It does not say how any implementation computes them or what interface an
implementation offers; an implementation's own documentation cites the rules here that it follows.

It is meant to be readable start to finish, but it is organized so you can look one rule up and
stop.

## How to read this

The specification is divided into **sections**, numbered 1 to 13. Each section states a series of
numbered **rules**. A rule is written `T4-2` — `T` for the type system, then "section 4, rule 2" —
and it is the unit you cite. Its peer document, [the Pets language
specification](pets-language-spec.md), numbers its rules `L5-2` the same way, so one rule id belongs
to exactly one document.

Every rule is checked by conformance tests whose names begin with the same id, in
`test/common/dev/martianzoo/pets/types/`:

| Section | Test file |
| --- | --- |
| 1. Universes and identity | `Spec01UniversesTest.kt` |
| 2. Classes | `Spec02ClassesTest.kt` |
| 3. Dependencies | `Spec03DependenciesTest.kt` |
| 4. Class literals | `Spec04ClassLiteralsTest.kt` |
| 5. Types | `Spec05TypesTest.kt` |
| 6. Subtyping and narrowing | `Spec06SubtypingTest.kt` |
| 7. Greatest lower bounds | `Spec07BoundsTest.kt` |
| 8. Refinements | `Spec08RefinementsTest.kt` |
| 9. Class properties | `Spec09PropertiesTest.kt` |
| 10. Defaults | `Spec10DefaultsTest.kt` |
| 11. Enumeration and automatic narrowing | `Spec11EnumerationTest.kt` |
| 12. Inhabitance | `Spec12InhabitanceTest.kt` |
| 13. Type variables | `Spec13TypeVariablesTest.kt` |

So `grep -rn "T8-9" docs/type-system-spec.md test/common/dev/martianzoo/pets/types/` finds a rule
and everything that proves it.

Blockquoted insets are not rules. A **Non-normative example** shows a rule at work in real game
content; a **Non-normative design note** explains why an otherwise surprising provision exists; a
**Present limitation** says what Pets cannot yet express.

### Notation

| Written | Means |
| --- | --- |
| `A <: B` | A is a subtype of B (T6-1) |
| `A ⊓ B` | the greatest lower bound, or meet, of A and B (T7-1); it may be absent |
| `CLASS Foo` | Pets source for a class declaration |
| `Foo<Bar>` | Pets source for a type expression |
| `L2-10` | rule 10 of section 2 of [the PETS language specification](pets-language-spec.md) |

### Terms

- A **universe** is a closed set of classes (section 1). A **catalog universe** holds every class
  one Catalog declares. A **game universe** extends one catalog universe with the few classes a
  single game declares for itself, such as one class for each player seat.
- A **class** is a named node of the nominal hierarchy, compiled from one `CLASS` declaration.
- A **type** is a class together with a bound for each of its dependencies, and optionally a
  refinement (T5-1). `GreeneryTile<Player1, Tharsis_2_2>` is a type; `GreeneryTile` is a class,
  and written alone as an expression it also names that class's base type (T5-2).
- A **structural type** is one with no refinement anywhere in it: not on itself, and not on any
  dependency bound, recursively.
- A **ground type** is any type that is not a type variable (section 13). A ground type may carry
  refinements.
- A **component** is one occurrence of a concrete type (T5-3) in a game state, and a **state** is a
  multiset of components. This specification is about types; it mentions components only to say what
  a type means.
- A **world** is whatever can answer whether a requirement holds in one state. Most of the type
  system never consults a world. A rule that does says so.

### The examples

Examples are drawn from the Terraforming Mars catalog ("canon"), so that a fan of the game can read
them. Most of them use the following excerpt. It omits supertypes, properties, and effects that no
rule here depends on, but what it shows matches canon:

```pets
ABSTRACT CLASS Anyone
ABSTRACT CLASS Owner : Anyone
ABSTRACT CLASS Actor
ABSTRACT CLASS Player : Owner, Actor
CLASS Admin : Actor
ABSTRACT CLASS Owned<Anyone> { DEFAULT Owned<Owner> }

ABSTRACT CLASS Area {
  ABSTRACT CLASS MarsArea {
    ABSTRACT CLASS LandArea { ABSTRACT CLASS VolcanicArea }
    ABSTRACT CLASS WaterArea
  }
}
CLASS Tharsis_1_1 : LandArea
CLASS Tharsis_1_2 : WaterArea
CLASS Tharsis_2_2 : VolcanicArea
CLASS Tharsis_2_3 : LandArea

ABSTRACT CLASS AreaPiece<Area>
ABSTRACT CLASS Occupant : AreaPiece
ABSTRACT CLASS OwnedOccupant : Owned, Occupant
ABSTRACT CLASS Tile : Occupant
ABSTRACT CLASS OwnedTile : OwnedOccupant, Tile {
  CLASS GreeneryTile : Tile<MarsArea>
  ABSTRACT CLASS CityTile {
    DEFAULT +CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>
    CLASS NormalCityTile
  }
}
CLASS OceanTile : Tile<MarsArea> { DEFAULT +OceanTile<WaterArea(HAS MAX 0 Tile)> }
CLASS Neighbor<AreaPiece, MarsArea> : Custom
ABSTRACT CLASS Adjacency<Tile<Area>, Tile<Area>>
```

A two-player game universe adds `CLASS Player1 : Player` and `CLASS Player2 : Player`. Examples
that name `Player1` are therefore about a game universe. Other canon declarations are quoted where
they are used, trimmed the same way.

### What a type denotes

A universe is closed (T1-6) and every concrete class is final (T2-3), so the concrete types that
narrow a given structural type form a fixed, finite set. Call it that type's **extension**.
`Tile`'s extension pairs every concrete tile class with every area it may occupy and, for an owned
tile, every owner. `Tharsis_2_2`'s extension is `Tharsis_2_2` itself. `Player1(NOT Player1)`'s is
empty.

Two versions of this set matter, and the rules say which one they mean:

- The **structural extension** counts every concrete type the universe's classes can form.
  Subtyping, meets and differences are answerable to it.
- The **inhabited extension** keeps only the members a particular game can contain (T12-4).
  Enumeration lists it (section 11), and inhabitance asks whether it is empty (section 12). In a
  catalog universe the inhabited extension differs from the structural one only through class
  literals (T4-8).

Most of this specification is about these sets. A difference removes members (T8-4), enumeration
lists them (T11-1), inhabitance asks whether any remain (T12-4), and automatic narrowing asks
whether exactly one does (T11-3). Two properties tie the rest to them:

- **Subtyping is sound.** If `A <: B`, every member of A's extension is a member of B's.
- **A meet is exact when it exists.** When `A ⊓ B` is a type, its extension is exactly the overlap
  of theirs (T7-1).

The converses are deliberately not claimed. Subtyping is nominal (T2-4), so two types can have the
same extension without being the same type. In a Tharsis game, `MarsMap` and
`TharsisMap` have the same inhabited extension. And a meet can be absent although the extensions
overlap (T7-1). Pets recognizes the classes a Catalog declared rather than manufacturing types to
name every set, and that is what keeps a type's meaning stable as a Catalog grows.

A `HAS` refinement (section 8) filters an extension further, according to a state, and only a world
can say how.

### Refinements are types

This specification treats refinement types as genuine types. A `HAS` refinement gives its type a
denotation relative to a state, so a narrowing judgment that involves one may need a world as an
input. The world is the environment for that judgment. It does not complete an otherwise incomplete
type.

Not every type is legal in every role. A component's identity is one concrete type, which cannot
carry a refinement (T5-3), and class signatures accept only structural types (L11-4). Those
restrictions do not make refinement types a separate kind of expression.

### What this document does not cover

- **How a game's premise selects its declaration closure.** Section 12 takes that closure as part
  of the game universe and defines inhabitance relative to it. Which classes a premise selects is
  decided when the premise is constructed, not by the type system.
- **Component-count invariants**, except for the one rule the type system leans on (T3-9): a
  dependency may only target a type limited to a single copy.
- **What the rest of Pets means.** Instructions, requirements, metrics, triggers and declarations
  are the subject of [the Pets language specification](pets-language-spec.md), whose rules are cited
  here as `L2-10`. How an engine brings a change about is outside both documents.

---

## 1. Universes and identity

Types exist only relative to a universe. Compiling a Catalog produces its **catalog universe**. A
game's universe is that catalog universe extended with the few classes the game declares for
itself: a class for each player seat, a class for its premise, and any ad-hoc declarations.
Nothing in this specification is meaningful except relative to one universe.

**T1-1. One class per name.** A universe contains at most one class with a given name, and every
occurrence of that name in it means that class. A game universe contains the very classes of its
catalog universe, not copies of them (T12-2). Two separate compilations of identical source are two
different universes, and their classes and types are not equal.

**T1-2. Values belong to universes.** A class or type belongs to the universe that defines it. A
catalog value can be interpreted by its catalog universe or by any game universe built on it. A
game's own value can be interpreted only by that game's universe. A judgment that combines values
no single universe can interpret, such as values from two catalogs or from two games, is an error.
It is not a negative answer.

This matters because a false "not a subtype" would silently misroute a trigger, whereas an error
stops at the mistake.

> **Non-normative design note — Catalog isolation.** No card asks whether two universes are equal.
> This rule prevents a type retained from one compiled Catalog from silently failing to match the
> identically named trigger in another, which would look like a legal card simply doing nothing.

**T1-3. Resolution is a function of the expression and the universe.** In a given universe an
expression resolves to at most one type. Resolving it again, or resolving a different spelling of
the same type, gives an equal type:

```text
GreeneryTile<Area>  and  GreeneryTile   →  equal types (T3-4)
GreeneryTile<>      and  GreeneryTile   →  equal types (T5-2)
```

A type's own renderings (T5-4, T5-5) resolve to a type equal to it. Types are values, and equality
is the only identity they have. The same expression can denote different types in different
universes (T12-5).

**T1-4. `Component` is the root.** Every universe contains an abstract class `Component` with no
supertypes and no dependencies. Every other class has it as a supertype.

**T1-5. `Class` is the other required class.** Every universe contains the concrete class `Class`,
whose base type is `Class<Component>`. Section 4 covers it.

**T1-6. A universe is closed.** A universe's set of classes is fixed: nothing adds a class to it or
removes one. Every judgment that depends on which classes exist is therefore well defined, and is
defined only for a complete universe. That covers the subclasses of a class (T2-7), a meet (T2-8,
T7-1), a difference (T8-4), enumeration (section 11) and inhabitance (section 12). Finding a class
by its name depends on no other class, so it is defined even while a universe is still being
assembled.

A universe is well formed only if its declarations obey the rules of this specification that apply
to them: every name they write is declared (T1-7), every class's dependencies and base type are
consistent (sections 2 to 4), and its properties and defaults are consistent (sections 9 and 10). A
type written inside an effect or action is also checked
against its argument positions (T3-5). That check happens when the effect is elaborated (L9-13),
where its class and game context are known, and a failure there invalidates that effect. Nothing
that needs a world is part of well-formedness.

> **Non-normative example — claiming a milestone.** `ClaimMilestoneAction` offers a choice among
> the concrete `Milestone` types. That menu is well defined because the universe is closed: every
> milestone of every map the Catalog knows is already in it, and nothing can add Elysium's
> `Legend5` afterward. Section 12 then restricts the menu to the milestones of the selected map.

**T1-7. Only the exact declared name resolves.** There are no abbreviations, no case folding and no
nearest match. An unknown name is an error wherever a declaration or expression writes it. A
catalog's declarations may name only catalog classes. A game's own declarations may name catalog
classes and the game's own classes. A game may not declare a name its catalog already declares, and
may not declare one name twice.

---

## 2. Classes

**T2-1. A declaration introduces a class and its base type.** `CLASS Foo` declares a concrete
class; `ABSTRACT CLASS Foo` declares an abstract one.

Concrete and abstract mean what they usually do. Every component's type has a concrete root class
(T5-3), and only abstract classes can be extended (T2-3).

**T2-2. Supertypes.** A class may name any number of abstract direct supertypes. A class that names
none extends `Component` implicitly. Naming `Component` explicitly is an error, because it says
nothing.

Nesting a declaration inside another is shorthand for naming the enclosing class as a supertype,
ahead of any supertypes the nested declaration writes itself:

```pets
ABSTRACT CLASS Area {
  ABSTRACT CLASS MarsArea {
    ABSTRACT CLASS LandArea { ABSTRACT CLASS VolcanicArea }
  }
}
```

is exactly `ABSTRACT CLASS MarsArea : Area`, `ABSTRACT CLASS LandArea : MarsArea`, and so on.

**T2-3. A concrete class is final.** No class may extend a concrete one, so a concrete class's only
subclass is itself. This is what makes "narrow this to a concrete type" a terminating operation: a
player who chooses `GreeneryTile<Player1, Tharsis_2_2>` cannot then be asked to choose again.

**T2-4. The subclass relation.** It is reflexive, transitive and antisymmetric, so it is a partial
order. It is also *nominal*: `LandArea` is below `MarsArea` because it says so, not because their
shapes agree. A class is a supertype of every class that is a subclass of it.

**T2-5. Cycles are rejected.** A class may not be its own supertype, directly or through a chain.

**T2-6. Declaration order is irrelevant.** A supertype may be declared after its subclass.

**T2-7. Superclasses are intrinsic; subclasses belong to the universe.** A class's superclasses are
itself, the classes it names, and theirs, up to `Component`. Its own declaration and its ancestors'
declarations determine them, so they are the same in every universe that contains it. Its subclasses
are itself and the classes of the universe that name it, directly or through a chain; its **direct**
subclasses are those that name it themselves. Because a universe is closed (T1-6), that set is well
defined. A game universe may hold subclasses its catalog universe lacks (T12-2).

`LandArea`'s superclasses are `LandArea`, `MarsArea`, `Area` and `Component` in every universe. Its
subclasses are itself, `VolcanicArea`, and every land area of every map the universe knows.

**T2-8. Greatest lower bound of two classes (`⊓`).** If one operand is a subclass of the other,
the subclass is the answer. Otherwise the answer is the unique **greatest common subclass**: a class
below both, which every other class below both is also below. If there is no such class, because
the two are disjoint or because two rival classes each extend both, the result is **absent**.

```pets
ABSTRACT CLASS Occupant : AreaPiece
ABSTRACT CLASS OwnedOccupant : Owned, Occupant
ABSTRACT CLASS Tile : Occupant
ABSTRACT CLASS OwnedTile : OwnedOccupant, Tile
```

`Tile ⊓ Owned` is `OwnedTile`: greeneries, cities and special tiles are all below it. `Occupant ⊓
Owned` is `OwnedOccupant`, which is above `OwnedTile` and above Land Claim's `Community` cube as
well. If a catalog also declared a class extending both `Tile` and `Owned` outside `OwnedTile`,
`Tile ⊓ Owned` would be absent. Pets does not manufacture a conjunction class; it only recognizes
one a catalog declared.

**T2-9. Custom classes.** A class with `Custom` among its supertypes is a **custom class**. No
component of it ever exists. Instead, the Catalog's host code supplies what gaining it means (an
instruction) or what counting it means (a metric). A Catalog must supply that meaning for exactly
its custom classes. A custom class without it is rejected, and so is host meaning supplied for a
class that is not custom, `Component` and `Class` included. A custom class may not inherit Pets
behavior: no superclass of it other than `Component` may declare effects, invariants, or default
quantifiers (T10-2).

> **Non-normative example — Robotic Workforce.** It gains
> `CopyProductionBox<CardFront(HAS BuildingTag)>`, a custom instruction whose host code copies the
> chosen building card's production box. `Neighbor` is a custom metric: the board's geometry, not
> any component, says which areas are adjacent. The agreement checks keep opaque host meaning from
> being loaded as ordinary Pets behavior, and the reverse.

---

## 3. Dependencies

A type argument in Pets is not a conventional generic parameter. It is a **dependency**: an edge to
one specific other component, which must exist for this one to exist. `Plant<Player1>` needs
`Player1`, and `GreeneryTile<Player1, Tharsis_2_2>` needs both the player and the area.

> **Non-normative design note — concrete types are the values.** A state deliberately has no
> occurrence identity or mutable instance fields: it is a multiset whose keys are concrete types.
> `Player1` and `Tharsis_2_2` are values precisely by being concrete types, and multiplicity records
> how many indistinguishable occurrences of a value exist. A dependency can identify its target only
> when that target type is limited to one copy (T3-9). This is a chosen boundary of the model, not
> an attempt to simulate object references.

**T3-1. Keys.** Every dependency a class declares gets a **key**: the declaring class's name plus
the zero-based position, written `AreaPiece_0`, `Owned_0` or `Adjacency_1`. The key, not the
position in any particular class's list, is the dependency's identity.

> **Non-normative example — ownership.** Tiles, resources, cards and markers inherit their owner
> edge from `Owned`, at different positions among their other dependencies. `Owned_0` names that
> edge in every one of them, so a rule about ownership needs no knowledge of the class it is
> reading.

**T3-2. Inheritance.** A subclass inherits every dependency under its original key, and may narrow
its bound by writing the supertype with arguments. The inherited keys come in the order the direct
supertypes are written, each contributing its own keys in its own order; a key already present keeps
its first position. Keys a class declares itself come after the inherited ones.

With the excerpt's declarations, `GreeneryTile` has keys `[Owned_0, AreaPiece_0]` and base type
`GreeneryTile<Anyone, MarsArea>`. Its first supertype is its enclosing `OwnedTile` (T2-2), which has
`Owned_0` before `AreaPiece_0` because it names `OwnedOccupant` first, and `OwnedOccupant` names
`Owned` first. `Tile<MarsArea>` then narrows the area edge; nothing copies or renames it.
`CLASS OceanCredit<OceanTile> : Owned<Player>` has keys `[Owned_0, OceanCredit_0]`.

**T3-3. Several supertypes, one key.** When more than one supertype constrains the same key, the
bounds are intersected (`⊓`, T7-1). Bounds with no common narrowing are an error.

> **Non-normative example — Predators.** Predators is at once an active card, an action card and an
> animal-resource card. `ActiveCard` constrains the card-front key `CardFront_0` to
> `Class<ProjectCard>`, while `ActionCard` leaves it at `Class<CardBack>`. Their meet gives
> Predators one card identity with the back `Class<ProjectCard>`, not three unrelated edges.

**T3-4. An argument intersects an open bound; it never replaces it.** Writing an argument for a
dependency intersects the argument with the bound already there. If the intersection is empty, the
argument is an error. A dependency whose bound is already concrete in the class's base type is
**fixed** by that class. It remains a dependency of every type of the class (T5-1), but it is not an
argument position. `Pets` fixes both its card back and the resource it holds, so its one argument
position is its owner: `Pets<Player1>`.

This is what makes `Anyone` work. `Anyone` is an ordinary class at the top of the ownership
hierarchy, so `Anyone` intersected with a narrower bound is that narrower bound:

```text
ProjectCard : Owned<Player>        →  ProjectCard<Anyone>     is  ProjectCard<Player>
GreeneryTile : Tile<MarsArea>      →  GreeneryTile<Area>      is  GreeneryTile
                                      GreeneryTile<WaterArea> is  GreeneryTile<Anyone, WaterArea>
ProjectCard<SoloOpponent>                                     →   error: not a Player
```

> **Non-normative example — Solar Logistics.** It draws a card whenever any player plays a space
> event, and its trigger is `EventCard<Anyone>(HAS SpaceTag)`. Bare `EventCard` would receive the
> contextual-owner default and see only its own owner's events (L9-4). `Anyone` removes that
> restriction, and intersecting it with `EventCard`'s `Player` bound gives `EventCard<Player>`.
> Replacing the bound would make the trigger a type its class cannot have (T5-7).

> **Non-normative example — Protected Valley.** It places `GreeneryTile<WaterArea>`. The written
> `WaterArea` meets the greenery's `MarsArea` bound, and the result is the legal special placement.
> Neither constraint replaces the other.

**T3-5. Argument matching is greedy, left to right across open positions.** Each written argument
takes the first open dependency not already taken whose current bound it can intersect. Fixed
dependencies are skipped. An argument that matches no remaining dependency is an error.

A useful consequence is that argument order does not matter when the bounds are disjoint:
`GreeneryTile<Player1, Tharsis_2_2>` and `GreeneryTile<Tharsis_2_2, Player1>` are the same type.
Order *is* meaningful when two bounds overlap. In `Adjacency<Tile<Area>, Tile<Area>>`,
`Adjacency<GreeneryTile<Player1, Tharsis_2_2>>` fills the first position and leaves the second
open.

> **Non-normative example — Law Suit.** The removal record its trigger matches is written
> `MyResourceWasRemoved<Owner, Attacker@Player>`. The record's dependencies are the victim
> (`Owned_0`, bound `Owner`), the resource class, the attacker (bound `Player`) and the generation.
> `Owner` takes the victim position. The attacker skips the resource-class position, which cannot
> hold a player, and takes the attacker position. Written `MyResourceWasRemoved<Attacker@Player,
> Owner>`, the attacker would take the victim position, which accepts any owner, and the two parties
> would swap.

> **Non-normative example — Turmoil.** Party rules write
> `PartyDelegate<This, DelegateOwner@Anyone>`, party first, although the inherited owner key
> precedes the party key. Because the two bounds are disjoint, each argument lands in the one
> position it can occupy.

**T3-6. The key an argument fills is part of the expression's meaning.** Matching (T3-5) assigns
each written argument to one dependency key. The resulting type does not always show that
assignment, but rules about written expressions depend on it. In particular, a variable occurrence
is captured along the key its argument filled (T13-11).

> **Non-normative example — Kaguya Tech.** It replaces one of your greeneries with a city in the
> same area: `CityTile<@MarsArea> FROM GreeneryTile<@MarsArea>`. The markers are what make the two
> areas one choice (T13-7). To capture the chosen greenery's area, the lone argument of
> `GreeneryTile<@MarsArea>` must be known to fill `AreaPiece_0`, the greenery's *second* key.
> Reading by position would read its owner.

**T3-7. `This` in a class header names the inheriting class.** In a declared dependency bound or a
supertype argument, `This` is rebound at each level of the hierarchy to the class that inherits the
header. The rebinding happens in place and leaves every other argument alone. A literal class name
in the same place is not rebound.

Canon's only use of `This` in a header is `Class<This>` in a dependency bound, described in T4-9.

> **Non-normative design note — presently general-purpose.** No canonical class writes bare `This`
> in a header, or `This` in a supertype argument. T3-7 states the general rebinding rule rather
> than an exception shaped to the one canonical use.

**T3-8. One header variable in two positions forces them to agree.** When one type variable occupies
two dependency paths of a class header (T13-2), resolving a type of that class propagates a choice
from either position into the other:

```pets
ABSTRACT CLASS ResourceHolder<Class<CardResource>> : Owned<Owner>
ABSTRACT CLASS CardResource<ResourceHolder<Class<This>, @Owner>> : Owned<@Owner> {
  CLASS Animal
  CLASS Microbe
}
```

The marker `@Owner` makes the holder's owner and the resource's own owner one variable. An animal's
owner is therefore necessarily the owner of the card it lives on:

```text
Animal<Player1>                 →  Animal<Player1, ResourceHolder<Player1, Class<Animal>>>
Animal<Pets<Player1>>           →  Animal<Player1, Pets<Player1>>
Animal<Player1, Pets<Player2>>  →  error
```

Without a marker, each occurrence declares its own variable and the positions constrain nothing.
The two tiles of `Adjacency<Tile<Area>, Tile<Area>>` are independent. `Adjacency<@Tile, @Tile>`
would require them to be the same tile.

> **Non-normative example — Pets.** An animal on Player 1's Pets card must also be owned by
> Player 1. Without propagation between the two occurrences of `@Owner`, it could acquire Player 2
> as its independent owner, and Player 2 could spend it.

**T3-9. A dependency may only target a type limited to one copy.** An edge names its target by
exact type alone, so a type that admitted two identical components could not say which one it
meant. Every concrete type a dependency bound admits must therefore carry an applicable `MAX 1` or
`=1` invariant. A game universe that breaks this is ill-formed. This is the only place
component-count invariants enter this specification.

`Signal` and all its subclasses, and `Die`, are never valid dependency targets, whatever their
invariants. A signal is a point event removed immediately after it happens, and `Die` has no legal
occurrence. Neither can anchor the existence of another component.

> **Non-normative example — action-used markers.** `ActionUsedMarker` is `Cardbound<ActionCard>`:
> `ActionUsedMarker<Player1, Predators<Player1>>` marks one exact card. It can do so because
> `CardFront` declares `HAS MAX 1 This<Player>`, so at most one Predators card is ever in play.
> Otherwise the marker would identify no card in particular, and Project Inspection could reuse the
> action of the wrong one.

**T3-10. Dependency maps and paths.** A type's dependencies form a finite map from keys to bounds.
Two dependency maps are equal when they map the same keys to equal bounds, whatever order the keys
were declared in. A bound that is itself a type has dependencies of its own, so a key **path**
reaches any nested dependency. In `Animal<Player1, Pets<Player1>>`, the path
`CardResource_0.Owned_0` reaches the owner of the card the animal is on. A type's **narrowed keys**
are those whose bound differs from the bound in its root class's base type.

**T3-11. Cycles are rejected.** Two classes may refer to each other freely. A genuine cycle of
dependency *bounds* has no finite answer and is an error: `CLASS Foo<Bar>` with `CLASS Bar<Foo>`, or
`CLASS Foo<Foo>`. A one-way chain is fine.

---

## 4. Class literals

A dependency asserts that a component exists. `Class<X>` instead names a class *as data*:
`Production<Class<Steel>>` is a steel production, and needs no steel cube to exist.

Every state holds exactly one component of `Class<X>` for each concrete class `X` whose base type is
inhabited (T12-4). A class literal can therefore also be the target of an ordinary dependency
without violating T3-9.

**T4-1. Form.** `Class<X>` takes exactly one bare class name. The class `Class` has the base type
`Class<Component>`, so bare `Class` means "some class".

Where a construct permits a type-variable marker, `Class<@X>` or `Class<Name@X>` marks the
represented class itself. Another occurrence with the same marker denotes that class's base type,
and `@X<dependencies>` applies ordinary dependency arguments to it (T13-1). Neither form denotes the
`Class<X>` component.

**T4-2. Concreteness depends only on the class named,** and not on that class's dependencies:

```text
GreeneryTile         is abstract  — its owner and area are open
Class<GreeneryTile>  is concrete  — GreeneryTile is one concrete class
Class<Metal>         is abstract  — Metal is not a concrete class
```

Like every concrete type, a concrete class literal can still be uninhabited in a particular
universe (T12-4).

> **Non-normative example — the project-card deck.** The type `ProjectCard` is abstract because
> its owner is open, yet `Class<ProjectCard>` is the one concrete representative of that category.
> A card face records which kind of back it has by depending on that representative, not on a card
> in someone's hand.

**T4-3. Class literals are covariant.** `Class<Steel> <: Class<Metal> <: Class<StandardResource>`,
and this carries into dependency positions: `Production<Class<Steel>> <:
Production<Class<Metal>>`.

> **Non-normative example — Manutech.** Its effect `PROD[@StandardResource]: @StandardResource`
> gains a resource whenever its owner's production of that resource increases. A steel-production
> increase matches the trigger because `Class<Steel>` narrows `Class<StandardResource>` through the
> same covariance as any other dependency.

**T4-4. A class literal represents one class.** `Class<X>` represents `X`. A type that is not a
class literal represents no class. Refined class literals (T8-10) and represented-class variables
(T13-1) are defined in terms of this.

**T4-5. Meets follow the class hierarchy.** `Class<Metal> ⊓ Class<Steel>` is `Class<Steel>`. The
meet of literals for disjoint classes is absent.

**T4-6. The operand is one bare, existing class name.** All of these are errors:

```text
Class<Steel, Plant>            two operands
Class<Steel<Player1>>          the operand has arguments
Class<Class<Steel>>            nested literals
Class<Oxygen>                  no such class (the class is OxygenStep)
```

`Class<Class>` is fine, because `Class` is itself a class, and a concrete one. The named class must
exist even where an expression merely counts one, as in `HAS MAX 0 Class<Oxygen>`. An instruction
may not gain a class literal: its components are fixed by the universe, not created by play.

> **Non-normative example — the Collector award.** It counts `Class<Resource>(HAS Resource<Owner>)`,
> the resource kinds a player holds. Allowing an argument-bearing operand such as
> `Class<Resource<Player1>>` would turn the global representative of a kind into player-specific
> data with no defined component identity.

**T4-7. A class literal has no component dependencies.** The slot inside `Class<X>` holds a class,
not a type, so `Class<Steel>` depends on no steel component. A class that *declares* a dependency
bounded by a class literal, such as `CLASS Production<Class<StandardResource>>`, has an ordinary
dependency whose bound happens to be a class literal.

> **Non-normative example — Mine.** Mine grants steel production without granting any steel. If
> `Class<Steel>` were a dependency on a steel component, that ordinary card would be ill-typed
> whenever its player had no steel.

**T4-8. The extension of a class literal.** The concrete narrowings of `Class<Metal>` are
`Class<Steel>` and `Class<Titanium>`: one for each concrete subclass of `Metal`. A member is
inhabited only when its represented class's base type is inhabited (T12-4). In a catalog universe,
which has no player classes, `Class<Tag>` therefore has no inhabited member, since every tag needs
a player to own it. A literal whose represented class has no inhabited concrete subclass enumerates
nothing.

> **Non-normative example — the solo opponent.** Solo setup runs
> `EACH @Class<StandardResource> { SoloStandardResourceReserve<@Class> }` to create one reserve per
> standard-resource kind. Enumerating resource components instead would miss a kind with none in
> play and repeat a kind with several.

**T4-9. `Class<This>` in a header** follows T3-7: it names the inheriting class. This is how a card
resource knows which cards can hold it:

```pets
ABSTRACT CLASS ResourceHolder<Class<CardResource>> : Owned<Owner>
ABSTRACT CLASS CardResource<ResourceHolder<Class<This>, @Owner>> : Owned<@Owner> {
  CLASS Animal
  CLASS Microbe
}
```

`Animal`'s holder bound becomes `ResourceHolder<Class<Animal>>` and `Microbe`'s becomes
`ResourceHolder<Class<Microbe>>`. Fish is a `ResourceCard<Class<Animal>>` and Ants a
`ResourceCard<Class<Microbe>>`, so `Animal<Fish>` and `Microbe<Ants>` resolve while `Microbe<Fish>`
and `Animal<Ants>` are errors. Writing `ResourceHolder<Class<CardResource>>` literally would put
microbes on Fish.

> **Non-normative example — played events.** An event card's rules move it to its owner's
> played-events pile as `PlayedEvent<Class<This>>`. That `This` is in an effect, not a header, and
> elaboration replaces it the same way (L9-2): playing Asteroid records `Class<AsteroidCard>`.
> Keeping the abstract `EventCard` class would lose which event was played.

---

## 5. Types

**T5-1. What a type is.** A type is a root class, one bound for each of that class's dependency
keys, and optionally a refinement (section 8). Two types are equal when those three agree, however
each was written.

**T5-2. A bare class name means that class's base type**, which supplies each dependency's declared
bound. An explicit empty argument list, `GreeneryTile<>`, means the same type. The two spellings
differ elsewhere in Pets: in an instruction, `<>` says "I accept this use's defaults on purpose".
That rule belongs to instructions, not to types; see L1-2 and L9-5.

> **Non-normative example — neutral solo tiles.** Their city placement is `@CityTile<> THEN
> GreeneryTile<LandArea(HAS Neighbor<@CityTile>)>`: place a city under the ordinary city-placement
> default, then place greenery next to it. The `<>` records that the default was accepted
> deliberately, but as a type, `CityTile<>` is `CityTile`.

**T5-3. Abstractness.** A type is abstract if its root class is abstract, **or** any dependency
bound is abstract, **or** it carries a refinement. Only a concrete type can be the type of a
component.

```text
GreeneryTile                                     abstract — no owner, no area
GreeneryTile<Tharsis_2_2>                        abstract — no owner
GreeneryTile<Player1, Tharsis_2_2>               concrete
GreeneryTile<Player1, Tharsis_2_2>(HAS Neighbor) abstract — a refinement is a question
```

This is the source of a common confusion: the *class* `OceanTile` is concrete, while the *type*
`OceanTile`, short for `OceanTile<MarsArea>`, is not.

Abstractness is structural and never consults a world.

Abstractness and inhabitance are different questions. A concrete type can be uninhabited because
its root class is outside a game's selected closure, or because a dependency's domain is empty. An
abstract type is inhabited when its universe contains at least one concrete narrowing, and
uninhabited when it contains none (T12-4).

> **Non-normative example — Research Outpost.** It places its city on
> `LandArea(HAS MAX 0 Neighbor)`: a land area that, at the moment of placement, is next to no other
> tile. The type stays abstract even when exactly one area qualifies at that moment. Placing the
> tile narrows it to that concrete area, and from then on the tile is
> `NormalCityTile<Player1, Tharsis_2_3>`. Its identity says nothing about neighbours, so tiles
> placed next to it later do not change which component it is.

**T5-4. Full form.** The **full form** of a type writes an argument for every open dependency, in
key order. Fixed dependencies (T3-4) remain part of the type under T5-1, but the full form omits
them because an expression cannot select them. The base type of `GreeneryTile` has the full form
`GreeneryTile<Anyone, MarsArea>`. `Pets` has three keys but only one argument position, so its full
form is `Pets<Player>`. `CardBilling`, whose three inherited billing keys are fixed, is
`CardBilling<Player, Class<CardFront>>`.

**T5-5. Compact form.** The **compact form** writes a subsequence of the full form's arguments, in
the same order. It first keeps every argument for a narrowed key (T3-10), and every argument that,
if omitted, would leave its position free to capture a later kept argument under T3-5. It then
repeatedly drops the first kept argument whose removal still resolves to the same type, as can
happen through T3-8 propagation. Each kept argument is itself written in compact form, and a
refinement is always written. Compact form is minimal only in that no single remaining argument can
be dropped. It is not promised to be the shortest spelling.

`GreeneryTile<Player1, MarsArea>` is written `GreeneryTile<Player1>`. The two positions of
`Adjacency` both accept a tile, so `Adjacency<Tile<Area>, GreeneryTile<Player1, Tharsis_2_2>>` is
written `Adjacency<Tile, GreeneryTile<Player1, Tharsis_2_2>>`. Without its first argument, the
greenery would fill the first position. `Adjacency<GreeneryTile<Player1, Tharsis_2_2>, Tile<Area>>`
drops its second argument. `Animal<Player1, Pets<Player1>>` is written `Animal<Pets<Player1>>`,
because the owner of the card determines the animal's owner through T3-8.

**T5-6. Both forms round-trip.** Resolving either form of a type yields that same type.

**T5-7. A type never escapes its class's declaration.** Every type of a class has exactly that
class's dependency keys, and each bound narrows the class's declared bound for that key. No way of
forming a type — resolution (T3-4), meets (T7-1) or variable binding (T13-10) — can produce one that
breaks this.

A type's **projection** onto one of its root class's superclasses keeps the bounds for that
superclass's keys and forgets the rest. The projection of `GreeneryTile<Player1, Tharsis_2_2>` onto
`Tile` is `Tile<Tharsis_2_2>`. It narrows `Tile<Tharsis_2_2>` for exactly that reason (T6-2).

> **Non-normative example — tile placement.** A greenery carries both an owner and an area. As an
> occupant of its area, it is seen through its projection, which keeps the area and forgets the
> owner, since an owner is not part of what an `Occupant` is. If the owner key had to be matched
> against `Occupant`'s keys, which do not include it, a greenery could never be an occupant.

---

## 6. Subtyping and narrowing

Pets uses two related judgments. **Subtyping** is context-free: `A <: B` says, from the two types
alone, that every A is a B. **Narrowing** is contextual. It asks whether A is an acceptable way to
settle B in one state, including whether A satisfies B's `HAS` refinement there.

**T6-1. The two judgments.** Every subtype pair is a narrowing in every state, but a narrowing that
depends on the state is not thereby a subtype. Subtyping is partial. When deciding it would take
evidence from a state, as when an unrefined type is tested against a `HAS` clause (T8-8), subtyping
has no answer, and asking is an error rather than a "no". Narrowing, given a world, always has an
answer.

> **Non-normative design note — refusing a plausible lie.** Without a state, nothing can decide
> whether a concrete area satisfies `HAS Neighbor`. Answering "no" would call a legal placement
> impossible in some states. The error exposes a caller that asked the wrong question.

**T6-2. The shared structural rule.** A narrows B when all of these hold:

1. A's root class is a subclass of B's root class;
2. for every dependency key B has, A's bound for that key narrows B's; and
3. B's refinement, if any, accepts A (section 8).

A key B does not have is not checked. Equivalently, A narrows B when A's projection onto B's root
class (T5-7) does. That is what lets `GreeneryTile<Player1, Tharsis_2_2>` narrow
`Tile<Tharsis_2_2>` whatever its owner.

**T6-3. Dependencies are covariant.** `Tile<Tharsis_2_2> <: Tile<LandArea> <: Tile<Area>`, and
narrowing the class and narrowing a dependency compose freely.

Covariance is the right choice precisely because a dependency identifies one exact target. A broad
dependency describes a *set of possible* concrete types to query or narrow, not one component that
would accept any member of the set. `ResourceValue<Class<Metal>>` counts the separate steel-value
and titanium-value components; it is not one component that accepts either.

**T6-4. Shape of the relations.** Over structural types, both judgments are reflexive, transitive
and antisymmetric: two structural types that narrow each other are the same type. With refinements
and a world, antisymmetry is lost. In a state where every land area has a neighbouring tile,
`LandArea` and `LandArea(HAS Neighbor)` narrow each other while remaining distinct types. Narrowing
is a preorder, not an order.

> **Non-normative example — Hermetic Order of Mars.** When it is played, it pays 1 MC for each Mars
> area that is empty and next to one of its owner's tiles *at that moment*. In a particular state
> that set can coincide with an unrefined one, but the two types must stay distinct, since the next
> tile placed can separate them.

**T6-5. Cross-universe comparisons are errors**, per T1-2.

**T6-6. A constraint read inside a domain.** Some positions constrain a value that already has a
**domain**, a type the position guarantees. The actor of a `BY` selector has the domain `Actor`. A
candidate satisfies constraint C read inside domain D when it narrows C intersected with D, the same
intersection an argument undergoes (T3-4). With domain `Actor`, the constraint `Player` accepts
`Player1` and rejects `Admin`, and `Actor(NOT Player1)` accepts both `Player2` and `Admin`. A
constraint that cannot meet the domain at all accepts nothing.

> **Non-normative example — actor constraints.** Protected Habitats forbids removal
> `BY Player(NOT Owner)`: a player, other than the card's owner, performing the change. By contrast,
> `BY Anyone` is the spelling that removes a trigger's usual actor restriction (L6-9) altogether, so
> Aphrodite's `VenusStep BY Anyone: 2 MC` also reacts to a Venus step performed by Admin. That
> wildcard is not the ownership class `Anyone` read inside `Actor`.

---

## 7. Greatest lower bounds

**T7-1. Greatest lower bound (`⊓`).** The meet of two types, in the universe that interprets them,
is the most general type below both, or **absent** when there is none. It is computed componentwise:
the root classes by T2-8, each shared dependency key by `⊓` again, and the refinements by T8-9. The
selected root class then contributes its own declared dependency set. That can add keys neither
operand had, and bounds narrower than either operand stated.

```text
Tile<Tharsis_2_2>  ⊓  Owned<Player1>                =  OwnedTile<Player1, Tharsis_2_2>
GreeneryTile<Tharsis_2_2>  ⊓  GreeneryTile<Player1> =  GreeneryTile<Player1, Tharsis_2_2>
GreeneryTile  ⊓  Tile<WaterArea>                    =  GreeneryTile<Anyone, WaterArea>
Tile<Tharsis_2_2>  ⊓  Tile<Tharsis_2_3>             =  absent
GreeneryTile  ⊓  OceanTile                          =  absent
```

Absent covers two different situations, and every rule that treats it as an error depends on which
one it is:

- **The operands are disjoint.** `Tile<Tharsis_2_2> ⊓ Tile<Tharsis_2_3>` is absent because no
  component could be both: their extensions do not overlap. Writing an argument outside a bound
  (T3-4) is this case, and rejecting it is a genuine type error.
- **The overlap has no name.** If two rival classes each extended both `Tile` and `Owned`,
  `Tile ⊓ Owned` would be absent although concrete owned tiles exist (T2-8). This is a limit of
  naming, not a claim about components. A Catalog that needs the intersection declares a class for
  it, which is why canon declares `OwnedTile`.

Where a meet exists it narrows both operands, and every type below both narrows it.

**T7-2. `⊓` is idempotent and commutative.** Refinement clauses form a set (T8-9), so two meets
that list their clauses in different orders are equal types, even though they may render
differently.

**T7-3. Cross-universe meets are errors**, per T1-2.

---

## 8. Refinements

A refinement turns a type into a filtered version of itself. `LandArea(HAS MAX 0 Tile)` is a land
area that has no tile at the moment the refinement is tested. A refinement is a non-empty
conjunction of comma-separated clauses, each of which repeats its keyword, as in Viron's
`ActionCard(HAS ActionUsedMarker, NOT Viron)`. There are two kinds of clause, and they behave very
differently:

- `HAS R` asks a **world** whether requirement `R` holds of the candidate. This is the only place
  in the type system that consults a state.
- `NOT X` makes a **structural** exclusion, decided entirely from the classes of the universe.

**T8-1. A refined type is abstract and lies below its unrefined domain.** Narrowing the domain while
keeping the same predicate narrows the refined type: `Tharsis_2_2(HAS Neighbor) <: LandArea(HAS
Neighbor)`.

**T8-2. `HAS` asks the world about the candidate.** Testing whether candidate `c` narrows
`D(HAS R)` substitutes `c` into `R` (T8-3) and asks the world whether the result holds. Testing
`Tharsis_2_2` against `LandArea(HAS Neighbor<CityTile>)` asks
`Neighbor<CityTile<Anyone, Area>, Tharsis_2_2>`. When the candidate is abstract, the question is
about that abstract type as written. `LandArea` tested against `LandArea(HAS Neighbor)` asks whether
any land area has a neighbour, not whether every one does.

> **Non-normative example — the standard greenery placement.** A player placing a greenery must, if
> possible, choose a land area next to one of their own tiles:
> `GreeneryTile<LandArea(HAS Neighbor<OwnedTile>)>`. Substituting each candidate area turns that
> printed condition into a question about the board at the moment of placement.

**T8-3. How the candidate is substituted.** Each outermost expression inside `R` receives the
candidate. Expressions nested in its arguments do not, because they say what that expression is
about rather than which candidate is being tested. The candidate takes the first compatible
dependency whose bound it *strictly* narrows, so a position already holding exactly that type is
left alone. If it strictly narrows none of the compatible dependencies, it takes the first
compatible one. A bare class property receives the candidate as its receiver:
`CardFront(HAS 20 cost)` tested against `Ants<Player1>` asks `20 Ants<Player1>.cost`.

If no expression in `R` can accept the candidate, the refinement fails without asking the world.
That is not an error; it is the answer. `Component(HAS StartToken)` can only ever match a player,
since a player is what a `StartToken` depends on, so testing `Tharsis_2_2` against it is simply
false.

A written argument *constrains* the candidate in the position it occupies. It does not reserve the
position away from it. Testing `Player1` against `Player(HAS PartyLeader<MarsFirst, Player>)`
narrows the written `Player` to `Player1` and asks `PartyLeader<Player1, MarsFirst>`.

> **Non-normative examples — CrediCor and Turmoil.** CrediCor pays 4 MC after a card costing 20 or
> more, tested as `CardFront(HAS 20 cost)`. Substituting the played card into bare `cost` reads
> that card's printed cost. When a party takes power, Turmoil's rules find its leader as
> `EACH Leader@Player(HAS PartyLeader<This, Player>)`. Each candidate player must merge into the
> written `Player` argument. If written arguments reserved their positions, no candidate would have
> anywhere to go and no player could ever become chairman.

**T8-4. `NOT` is a structural difference.** `D(NOT X)` is the part of `D` that cannot overlap `X`. A
candidate satisfies it only when its **entire** structural extension avoids `X`:

```text
Player2 <: Owner(NOT Player1)     yes
Player1 <: Owner(NOT Player1)     no
Player  <: Owner(NOT Player1)     no — abstract Player still admits Player1
```

The exclusion need not narrow the domain; the difference is taken through their structural overlap.
`Actor(NOT Owner)` excludes the players, who are both actors and owners, and keeps `Admin`. Overlap
is detected even where the two have no unique greatest common subclass (T2-8), and only concrete
overlap counts: an abstract class below both is not evidence of overlap. The test never consults a
world, and works the same inside a dependency, as in `Plant<Owner(NOT Player1)>`.

> **Non-normative example — Protected Habitats.** After specialization for its owner, its selector
> is `Player(NOT Player1)`. That must mean "a player other than Player1" by type, whatever the state
> holds. Asking the state whether the negation holds would make a player's identity depend on the
> board.

**T8-5. The excluded operand must be refinement-free, recursively.** Neither
`Owner(NOT Player(HAS StartToken))` nor a nested `NOT` is accepted. This keeps the difference
decidable without a world, and prevents negating a world query.

> **Non-normative design note — a deliberate language boundary.** No canonical card needs to negate
> a `HAS` query. Rejecting that form keeps `NOT` usable for ownership and actor exclusions without
> introducing closed-world negation ("not currently found" versus "cannot exist").

**T8-6. A difference that cannot bite is dropped.** If the domain and the exclusion cannot overlap
in the first place, the refinement disappears: `Player1(NOT Player2)` *is* `Player1`.

> **Non-normative example — Philares.** It gains a resource whenever one of its owner's tiles and
> another player's tile become adjacent. Specialized for Player1, its trigger mentions
> `OwnedTile<Anyone(NOT Player1)>`. When the trigger is specialized to Player2's city placed next to
> Player1's greenery, that owner position becomes `Player2(NOT Player1)`, which must be plain
> `Player2`. Otherwise the matched adjacency would carry a refinement and be abstract (T5-3).

**T8-7. A difference that excludes everything is still a type.** `Player1(NOT Player1)` keeps its
refinement, is abstract, and enumerates nothing. It stays representable because an excluded type
variable may be specialized later, making the difference non-empty again.

> **Non-normative example — resource-removal watchers.** Their victim is
> `Victim@Owner(NOT Attacker@Player)` until the attacker is bound. Before then it excludes every
> player, and in a game with no other owners it excludes everything. Discarding it early would lose
> the restriction that the victim is someone other than the attacker (T13-9).

**T8-8. Refinements in narrowing.** When B is refined, A narrows B only if A narrows B's unrefined
domain (T6-2) and satisfies every one of B's clauses. The clauses are decided as follows:

- A refined type always narrows its own unrefined domain (T8-1).
- A clause that also appears in A's refinement is satisfied with no world consulted.
- If A's `HAS` clauses include all of a target `HAS` clause's requirement conjuncts, A already
  satisfies it: `LandArea(HAS Neighbor, HAS Occupant) <: LandArea(HAS Neighbor)`.
- Otherwise, a type that carries any refinement, even one made only of `NOT` clauses, never
  satisfies a `HAS` clause, and this is decided without a world. Different predicates do not imply
  one another.
- An *unrefined* type is tested against a `HAS` clause by asking the world (T8-2). Without a world
  there is no answer (T6-1).
- A `NOT` clause is satisfied by an identical clause in A, or by the structural test of T8-4.
- These comparisons read the two predicates *as written*. That is meaningful only when both types
  substitute the same candidate into them. Two class literals for different classes do not (T8-10),
  so neither shortcut applies to them. `Class<BuildingTag>(HAS Tag)` does not narrow
  `Class<Tag>(HAS Tag)`, because for each type the predicate is about its own represented class.

> **Non-normative example — Cyberia Systems.** It copies the production boxes of two different
> building cards: `(BuildingTag<First@CardFront>: CopyProductionBox<First@CardFront>) THEN
> CopyProductionBox<CardFront(HAS BuildingTag, NOT First@CardFront)>`. The second choice must pass
> both clauses: the world confirms its building tag, and the structural test keeps it from being the
> first card again.

**T8-9. Meets of refinements.** A refinement that one operand has and the other lacks is kept.
Refinement clauses form a set: duplicates collapse, and clause order does not affect type
equality. A rendering lists distinct clauses in the order they were first met. So
`LandArea(HAS Neighbor) ⊓ Area(NOT Tharsis_2_2)` is `LandArea(HAS Neighbor, NOT Tharsis_2_2)`, and
`Area(NOT Tharsis_2_2) ⊓ Area(NOT WaterArea)` is `Area(NOT Tharsis_2_2, NOT WaterArea)`.

**T8-10. Refined class literals.** A refinement on `Class<X>` tests the class the candidate
represents (T4-4). Within that refinement, an occurrence rooted at `X` means the represented
candidate automatically. Testing `Class<BuildingTag>` against `Class<Tag>(HAS Tag<Player1>)`
therefore asks `BuildingTag<Player1, TagHolder<Player1>>`: it counts tag classes, not tag
components. An explicit `Class<@X>(HAS @X)` remains an equivalent spelling.

> **Non-normative example — Diversifier.** The milestone requires `8 Class<Tag>(HAS Tag<Owner>)`:
> eight distinct kinds of tag the player has, not eight tags. Testing the represented class is what
> makes five Earth tags count as one kind.

**T8-11. Refinements inside dependencies** behave like any other refinement, and survive rendering
and re-resolution.

> **Non-normative example — city placement.** Its gain default requires
> `LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)`. Losing that nested refinement during rendering
> would allow a city next to another city.

---

## 9. Class properties

A **class property** records an immutable fact about a class: a card's cost, a milestone's
requirement, an award's metric. It is not state on a component. Every component of one concrete
type necessarily agrees about it.

**T9-1. A property is declared either as a bound or as a value.** The bounds are `Number`, `Metric`,
`Requirement` and `Requirement?`. The values are a literal number, a metric, or a requirement.

```pets
ABSTRACT CLASS CardFront<Class<CardBack>> : TagHolder {
  cost = Number
  requirement = Requirement?
}
CLASS Ants : ActiveCard { cost = 9 }
```

`Requirement?` is the one optional bound: a concrete class may leave it unfilled, and the property
is then absent.

> **Non-normative example — Tactician.** The milestone counts `CardFront(HAS requirement)`, the
> cards with a printed requirement. Most cards have none, so `requirement` must be optional without
> making those card classes incomplete.

**T9-2. A subclass may narrow, never override.** `Metric` may narrow to `Number`, `Number` to a
literal, and `Requirement` to a requirement. Overriding a value a superclass already fixed is an
error, and so is a "narrowing" that is not one.

**T9-3. A concrete class must fix every property it inherits**, except an unfilled `Requirement?`
(T9-1). A concrete class with an unfixed property is an error.

> **Non-normative example — playing any card.** Playing a card bills `@CardFront.cost`. Requiring
> every concrete card to fix `cost` catches a missing printed price when the Catalog is compiled,
> instead of halfway through a purchase.

**T9-4. Inheriting one property along several paths.** The same fact arriving twice is one fact.
When one path narrowed further than another, the narrower fact wins, provided the two lie on one
chain of narrowings. Two properties with one name from unrelated origins are an error, and so are
two divergent narrowings of one property.

> **Non-normative example — Predators.** It inherits card properties through `ActionCard`,
> `ActiveCard` and `ResourceCard`. Those paths all lead back to the same `cost` and `requirement`
> declarations on `CardFront`. Treating them as three competing properties would reject a real
> three-role card.

**T9-5. A type's property values are its root class's.** A property has a value for a type exactly
when the type's root class fixes it, with an unfilled `Requirement?` having the value "absent".
Asking for the value of a property that is still only a bound, or that the class does not have, is
an error.

**T9-6. Properties take no part in type identity or subtyping.** They are facts about a class, not
dependencies. Two cards with different costs are different types because they are different
classes, not because of the costs.

> **Non-normative example — Sponsor.** The milestone requires three `CardFront(HAS 20 cost)`. Cost
> is a queryable fact used to filter card classes, not part of a card's identity. If it were, a
> printed price change would create a different kind of card component.

---

## 10. Defaults

A `DEFAULT` clause records context that the physical game leaves implicit: that a tile goes on a
land area, or that a resource belongs to the player in context. It changes how an authored
expression *resolves*. It never changes which types exist.

**T10-1. Three separate sets.** Defaults are gathered independently for all uses
(`DEFAULT Foo<...>`), for gains (`DEFAULT +Foo<...>`) and for removals (`DEFAULT -Foo<...>`). A
class's all-uses defaults give it a **default template**, its base type written with those
defaults filled in, and a **default type**, the type that template denotes. The two differ only as
T10-5 says. Instructions consume the gain and removal sets (L9-5); resolution does not.

```pets
ABSTRACT CLASS SpecialTile : Tile<MarsArea> {
  DEFAULT +SpecialTile<LandArea>
}
```

The gain default says that a special tile placed without saying where goes on a land area.
`SpecialTile<Tharsis_1_2>` still resolves, to a water area, because a default is not a bound.

> **Non-normative example — Aquifer.** Gaining `OceanTile<>` accepts the gain default and asks for a
> water area that is empty at the moment of placement. Merely counting oceans, or removing one, must
> not invent a placement target. That is why the gain, removal and all-uses defaults cannot be one
> set.

**T10-2. Quantifiers.** A gain or removal default may also carry a quantifier: `!` mandatory, `.`
as much as possible, or `?` optional. `Component` supplies `!` for both, so every class inherits
one. Gain and removal quantifiers are inherited independently, and supertypes that disagree about
one are an error.

> **Non-normative example — paying `Owed`.** Creating a debt is mandatory, while removing one
> defaults to as-much-as-possible (`DEFAULT -Owed<Class<MC>>.`), so a debt can be paid off in parts
> from mixed sources. A single inherited quantifier for both directions would make either the debt
> optional or partial payment illegal.

**T10-3. A `DEFAULT` clause must name the class that declares it.**

> **Non-normative design note — defaults have one owner.** No card needs one class's declaration to
> install another class's default. Permitting it would let the meaning of bare `OceanTile<>` depend
> on whichever unrelated module happened to declare a remote default.

**T10-4. Inheriting dependency defaults.** For one dependency key and one use kind, only the
nearest declaring superclasses supply a default: anything a nearer superclass overrode is
discarded. The survivors are intersected (`⊓`), and survivors with no common narrowing are an
error. Each inherited default is also intersected with the inheriting class's own bound, so a
default can only get narrower. A default that a class declares, and that merely restates that
class's own bound for the key, records nothing at all.

> **Non-normative example — Lava Flows.** Its tile is declared plainly as
> `LavaFlows_SpecialTile : SpecialTile`. On a map without volcanic areas the card places
> `LavaFlows_SpecialTile<>`, which accepts the land-area gain default inherited from `SpecialTile`.
> Had the tile been declared `SpecialTile<VolcanicArea>`, the inherited default would have narrowed
> to `VolcanicArea` with it.

**T10-5. `Owner` in a default stays as written.** This is the one deliberate exception to T10-4. A
literal `Owner` written in a default is *not* intersected with the class's bound, so it can later
be replaced by whichever player supplies the context (L9-3).

The visible consequence is that a class's default template may lie outside its own bounds. `Owned`
declares `DEFAULT Owned<Owner>`, and `TerraformRating : Owned<Player>` inherits it. The base type
and the default type are both `TerraformRating<Player>`, but the default template is
`TerraformRating<Owner>`. The template deliberately awaits a context, while every type formed from
it still respects the class's bounds (T5-7).

> **Non-normative example — setting up terraform rating.** Multiplayer setup runs
> `EACH Player { 20 TerraformRating }`, and inside that body `Owner` is the player selected for the
> branch (L9-3). If the default were normalized early to `TerraformRating<Player>`, the body would
> name "some player's" rating instead of the selected player's.

---

## 11. Enumeration and automatic narrowing

Because a universe is closed, the concrete possibilities below an abstract type can be listed. That
is the operation behind "which area do you want?", and behind settling a choice automatically when
only one possibility exists.

Both operations are relative to a universe and, in a game universe, to the game's selected closure
(T12-3). A catalog type does not determine them by itself, because many game universes share it.

**T11-1. Enumerating a type.** The **enumeration** of a type lists its inhabited extension: every
inhabited concrete subclass of its root class, paired with every inhabited concrete binding of each
dependency that both the subclass and the type admit. In a two-player Tharsis game:

```text
GreeneryTile<Tharsis_2_2>  →  GreeneryTile<Player1, Tharsis_2_2>, GreeneryTile<Player2, Tharsis_2_2>
Tile<Tharsis_2_2>          →  NormalCityTile<Player1, Tharsis_2_2>, NormalCityTile<Player2, Tharsis_2_2>, ...
                              one per owned tile class and player, and OceanTile<Tharsis_2_2>
Class<Metal>               →  Class<Steel>, Class<Titanium>
```

A concrete inhabited type enumerates only itself. A type with no inhabited concrete narrowing
enumerates nothing.

> **Non-normative example — using a standard project.** `UseStandardProjectAction` starts from the
> abstract `StandardProject` and must offer Sell Patents, Power Plant, Asteroid, Aquifer, Greenery
> and City as concrete choices. Pairing subclass choice with dependency choice extends the same
> operation to tiles and other dependent components.

**T11-2. Refinements during enumeration.** Every `NOT` clause filters the candidates, because it can
be decided structurally. `HAS` clauses are **not** applied: enumeration is world-free, and whoever
enumerates tests the survivors against a state. So `LandArea(HAS Neighbor<OwnedTile>)` enumerates
every land area.

> **Non-normative example — the standard greenery placement.** The legal areas are the land areas
> next to one of the player's tiles at the moment of placement. Enumeration lists every land area,
> and the current board then filters them. An enumeration that consulted the board would make the
> universe's answer change with every tile placed.

**T11-3. Automatic narrowing.** A type **narrows automatically** to a concrete type when that type
is its only choice in every respect. Its root class must have exactly one inhabited concrete
subclass compatible with it, every dependency must in turn have exactly one concrete choice, and the
type's refinement must accept the result. This is stricter than "one candidate satisfies the
refinement". Any remaining choice, anywhere, prevents it, deliberately, so that a decision a player
should make is never made silently.

Within those limits it is thorough:

- It sees through a `NOT`. In a two-player game, `Owner(NOT Player1)` narrows automatically to
  `Player2`; in a three-player game it does not.
- It skips a concrete class whose bounds are incompatible with the requested type. That class is
  not one of the choices. `CardResource<Pets<Player1>>` narrows to `Animal<Player1, Pets<Player1>>`,
  because a microbe cannot live on Pets.
- It yields nothing when the requested type is incompatible with every concrete class.

A `HAS` clause can decide between candidates only where enumeration happens anyway, as with a
refined class literal (T8-10).

The engine can also use the current World to finish a gain after its Task is selected. That later
step can exclude a subtype whose added dependency is absent or whose gain limit is full. If only
one executable concrete gain remains, it may also settle the gain's area. This does not change the
world-free type rule.

This is an under-approximation, and knowingly so. A type narrows automatically when the *universe*
leaves one candidate, not when the current state does. An ocean placement asking for
`WaterArea(HAS MAX 0 Tile)` never narrows automatically, however few empty water areas remain,
because the map declares many water areas and only a state can say which of them are empty. Asking
a world about every candidate on every resolution would cost more than it saves, and the answer
would change under the player's feet.

> **Non-normative example — Aquifer.** Late in a game only one ocean area may still be empty, and
> Aquifer's ocean is still an ordinary choice. What automatic narrowing does settle is a choice the
> universe itself has reduced to one: `MarsMap` in a game on one map (T12-3), or which of two
> players is "the other one".

---

## 12. Inhabitance

A game universe comes with a **selected closure**: the declarations the game's premise includes.
The closure determines which concrete types the game can contain. **Inhabitance** is the resulting
property of a type. It is fixed for the game, and does not depend on any state.

**T12-1. Known names keep their nominal meaning.** A class name the universe knows resolves to its
class, with the same hierarchy, dependencies and declaration, whether or not the game can contain
components of it. An unknown class name is an error (T1-7).

In a base game, `VenusStep` still resolves and is still a `GlobalParameter`, while the type
`VenusStep` is uninhabited: the game knows that no Venus step can exist.

> **Non-normative example — Venus Next.** In a base game, `VenusStep` is known but uninhabited and
> `VenusStap` is unknown. Treating both as "not present" would hide typos. Treating both as
> inhabited would offer a Venus track the game did not select.

**T12-2. A game universe contains its catalog universe.** It shares the catalog's very classes and
types, with the same equality (T1-1). A game's own classes may extend catalog classes, as
`Player1 : Player` does, and are then among those classes' subclasses in the game universe (T2-7).
The catalog universe never refers to a game's classes. Values of two different games are
incompatible (T1-2).

> **Non-normative example — shared catalog identity.** A base game and a Venus game resolve
> `Class<VenusStep>` to the same catalog type. It is inhabited only in the game whose closure
> contains a concrete Venus step.

**T12-3. Enumeration follows the closure.** In a game universe:

- only classes in the selected closure count among a class's subclasses for enumeration;
- enumeration (T11-1) lists only inhabited concrete types;
- an uninhabited type enumerates nothing, and `Class<X>` enumerates nothing when `X` has no
  inhabited concrete subclass;
- automatic narrowing (T11-3) can therefore succeed in a game where the catalog is undecided. The
  catalog knows seven maps, but in a Tharsis game `MarsMap` narrows automatically to `TharsisMap`.

> **Non-normative example — claiming a milestone.** The Catalog knows the milestones of every map,
> but `ClaimMilestoneAction` must offer only those on the selected map. Otherwise a Tharsis game
> could offer Hellas's Polar Explorer as a legal claim.

**T12-4. Inhabitance is the existence of a realizable concrete narrowing.** A concrete type is
**realizable** in a game when its root class is in the selected closure, every dependency bound is
itself a realizable concrete type, and, for a class literal `Class<X>`, the base type of `X` is
inhabited. A type is **inhabited** when its extension contains at least one realizable type, and
**uninhabited** when it contains none. Each of these is uninhabited:

- a type rooted in a concrete catalog class outside the closure;
- an abstract type with no realizable concrete narrowing;
- a type with a dependency that has no realizable concrete binding;
- a structural difference, such as `Player1(NOT Player1)`, that excludes every candidate; and
- a class literal whose represented class has no concrete subclass with an inhabited base type.

Realizability is the greatest self-consistent assignment satisfying these conditions. Class
representatives exist structurally (section 4), so a `Class<This>` dependency (T4-9) can make its
own class's concrete base type inhabited; that needs no component of the represented type. Ordinary
dependency-bound cycles remain illegal (T3-11).

An abstract type may therefore be inhabited, and a concrete type may be uninhabited. Inhabitance is
fixed by the game universe and its structural refinements. It does not change because a state
happens to contain no matching components, has no remaining capacity, or answers a `HAS` refinement
in the negative.

An uninhabited type counts zero, contributes no concrete choices and no class representative, and
cannot be the type of a component or fire a trigger. L9-14 says what becomes of a change to one.
Its nominal meaning remains available for resolution, subtyping, meets, differences and diagnostics.

`Die` is a different case. It is a concrete, final class whose invariant allows it no component in
any state. A mandatory change to either kind cannot happen, but the reasons differ: an uninhabited
type has no realizable narrowing in the universe, while `Die` is concrete and has no legal
occurrence.

> **Non-normative example — game-end barriers.** The core rules create
> `GpIncomplete<Class<GlobalParameter>>` for each global parameter, but the one for
> `Class<VenusStep>` must be uninhabited without Venus Next. Otherwise the game would wait on a
> track that can never advance.

**T12-5. Differences are judged against the whole universe.** Whether a difference can bite (T8-4,
T8-6) depends on every class of the universe that resolves it: the game's own classes, and every
catalog class whether or not the game selected it. It never depends on the selected closure alone.
If two classes overlap only through a class the game did not select, `Left(NOT Right)` still keeps
its refinement and still rejects bare `Left`. Only enumeration under it is restricted to the
closure (T12-3).

A game universe adds classes, so a difference can bite in a game where it could not in the catalog.
The catalog declares no concrete player class. In the catalog universe, `Owner(NOT Player)` cannot
bite and resolves to plain `Owner` (T8-6); in a game universe it keeps its refinement. An expression
that writes a difference means what the universe resolving it says (T1-3).

> **Non-normative example — Viron.** Its action reuses another card's action:
> `ActionCard(HAS ActionUsedMarker, NOT Viron)`. Whether a game selected Venus Next must not change
> what that expression means. If the difference followed the selected closure, it would vanish in
> every game without Venus Next, and one expression would denote different types in games that know
> exactly the same classes.

---

## 13. Type variables

An effect can mark one open type choice and use it again elsewhere:

```pets
PROD[@StandardResource]: @StandardResource
```

This is Manutech's effect: "when your production of a standard resource increases, gain one of that
resource". The anonymous marker `@StandardResource` makes its two occurrences one **type variable**,
a single choice shared by both.

**T13-1. A variable is a kind of type.** Every type is either a ground type or a type variable. A
variable's meaning, for every purpose of sections 1 to 12, is its **bound**, a ground type. Its
root class, dependencies, abstractness and every judgment involving it are its bound's. What a
variable adds is identity: its occurrences are one choice. A ground type is not a variable, and its
meaning is itself.

A variable has one **supplying** occurrence and any number of other occurrences. The marker syntax
does not say which is which, and does not require the supplying occurrence to come first; the
enclosing construct decides (T13-2, T13-7). Each occurrence is a type in its own right: the variable
as seen at one written position.

A variable supplied by the bare operand in `Class<@X>` or `Class<Name@X>` is a
**represented-class variable**. It shares the selected class rather than an already parameterized
type, so an occurrence may apply dependency arguments to it. `SoloCardResourceReserve` is declared
`ResourceHolder<Class<@CardResource>>` and gains `42 @CardResource<This>`; once `@CardResource` is
bound to `Microbe`, that occurrence is `Microbe<This>`. The applied expression follows the ordinary
rules: arguments match keys under T3-5, intersect bounds under T3-4, and must agree with
dependencies the selected class already fixes. An explicit empty list, `@X<>`, keeps its normal
meaning of accepting that occurrence's defaults (L1-2). Only a represented-class variable may vary
its argument list between occurrences, and an occurrence that does not supply the variable may not
add a refinement.

A variable's identity comes from its supplying declaration and its scope. Matching explicit markers
join occurrences; neither a class name nor a bound class alone identifies a variable.

There are two sources of variables. An eligible occurrence in a class header supplies one (T13-2
to T13-5), and matching markers join occurrences within a local construct (T13-6, T13-7). A
trigger supplies a concrete value when it matches, and an explicitly marked `BY` selector is one
place that value can come from (T13-9).

Three properties hold of every variable, and most of the rules below follow from them:

1. **An occurrence chooses, matches, or observes.** A change's target chooses a value. A trigger
   matches one from the event it responds to. A requirement, a gate, the expression a metric counts,
   and a refinement only look at what is there. Choosing and matching occurrences can supply a
   variable. An observing occurrence never does, since it ranges over whatever matches it, though it
   may be written before the occurrence that supplies its value.
2. **Marker identity is scope-local.** An anonymous marker identifies the sole explicitly marked
   variable of its bound class in the nearest enclosing scope that declares one. A name
   distinguishes several variables with one bound class. One scope cannot declare both anonymous and
   named variables of one bound class. The same name may independently identify variables with
   different bound classes, or in nested scopes. An `EACH` or `RANK` selector exposes its selected
   value through a marker. A refined class literal supplies its represented candidate to matching
   roots in its own predicate automatically (T8-10).
3. **Inheritance passes values, not names.** Inherited effects keep their superclass's variable
   scope. A subclass supplies values for those variables when it or a component fixes them (T13-4,
   T13-5).

### Class-header variables

**T13-2. Each eligible header occurrence supplies a variable.** An occurrence in a class header is
**eligible** when it is not `This` and it resolves to an abstract type. Each unmarked eligible
occurrence supplies its own variable, and matching marked occurrences join one shared variable.
`ABSTRACT CLASS Adjacency<Tile<Area>, Tile<Area>>` declares four variables: each `Tile<Area>`, and
the `Area` nested inside each. An abstract bound is enough; only the variables the class body uses
need a marker (T13-3).

A class literal written as a declared dependency supplies one variable, not two. It is the
represented class, as `StandardResource` in `CLASS Production<Class<@StandardResource>>`. If the
whole literal is marked instead, as in `CLASS Owed<@Class<StandardResource>>`, it is the literal
itself. A class literal written anywhere else in a header, such as inside another dependency or in a
supertype argument, is an ordinary eligible occurrence, and so is its operand.

A header variable needs a marker when the class's own body refers to it, or when two dependency
positions must agree (T3-8):

```pets
ABSTRACT CLASS Cardbound<CardFront<@Player>> : Owned<@Player>
```

`@Player` makes the owner inside `CardFront` and the owner supplied to `Owned` one variable, so an
action-used marker belongs to the player whose card it marks.

> **Non-normative example — separate parties.** Law Suit's removal record is
> `CLASS MyResourceWasRemoved<Class<Resource>, Player, GenerationScope> : Owned<Owner>`. Its owner
> is the victim and its `Player` dependency the attacker. They are separate variables, as they must
> be: the record exists precisely because two different players are involved.

**T13-3. Occurrences in the class's own body are marked explicitly.** A marker on an eligible header
occurrence and matching marked occurrences in the class's authored effects or actions share one
variable. An unmarked type in the body is an ordinary expression, not an occurrence of a header
variable. A marker is anonymous or has a class-name-shaped local name (L1-7), and every marked
header variable must recur. A represented-class header variable follows T13-1, so the body of a
class declaring `Class<@CardResource>` may write `@CardResource<This>`.

> **Non-normative example — production.** `CLASS Production<Class<@StandardResource>>` says
> `ProductionPhase: @StandardResource`: during the production phase, a steel production creates
> steel. An unmarked `StandardResource` there would be an independent type, satisfied by any
> resource.

**T13-4. Inheritance.** A subclass does not redeclare an inherited variable, and effects inherited
from a superclass keep that superclass's scope.

An explicit supertype argument supplies the value of an inherited variable. If that value is still
abstract and the subclass's own body uses it, the argument must mark it, as
`SoloCardResourceReserve : ResourceHolder<Class<@CardResource>>` does so that its body can gain
`@CardResource<This>`. An unmarked `CardResource` in the body would be a separate, ordinary choice.
A concrete value needs no variable to be spelled. Pets is a `ResourceCard<Class<Animal>>`, and its
body can simply write `Animal<This>`.

> **Non-normative example — `CardBilling`.** It inherits `Billing`'s cleanup effects, which use
> `Billing`'s resource-denomination variable, while fixing that denomination to MC. Redeclaring the
> variable in the subclass would disconnect the inherited "remove when no debt remains" test from
> the currency the billing is actually in.

**T13-5. Specialization supplies values.** When a type narrows its class's base type, it supplies a
value to each header variable whose position it narrowed. That value is the variable's position
read in the narrower type. Specializing `SoloStandardResourceReserve` to
`SoloStandardResourceReserve<Class<Steel>>` supplies `Steel` for `@StandardResource`, and binding it
into the class's effects turns `SetupPhase: 42 @StandardResource` into `SetupPhase: 42 Steel`.

A value that did not change and is still abstract supplies nothing. A subclass that *fixes* the
position does supply a value. `CardBilling : Billing<CardPlay, Action1, Class<MC>>` supplies
`Class<MC>` for `Billing`'s `@Class`, so the inherited `This IF MAX 0 Owed<@Class>:: -This!` tests
`Owed<Class<MC>>`.

> **Non-normative example — solo steel reserve.** Every body occurrence of `@StandardResource` in
> the steel reserve must become steel: its initial stock, its production, and the transfers that
> mirror each player's steel. Merely narrowing the header would leave a supposedly steel reserve
> operating on arbitrary resources.

### Local variables

**T13-6. An effect marks a shared choice explicitly.** Matching marked occurrences in the trigger
and the instruction are one variable. A matching trigger occurrence supplies its complete
structural expression. A requirement, metric or refinement may observe that variable but cannot
supply it, and the observing occurrence may nevertheless come first in the source. The marker is
anonymous or has a class-name-shaped local name (L1-7). At least one occurrence must be in the
instruction, since repetitions confined to the trigger do not connect the trigger's matched value to
the effect's result. Only a represented-class occurrence may apply dependency arguments (T13-1), and
an occurrence that does not supply the variable may not add a refinement.

Binding substitutes at every occurrence at once. Manutech's effect, lowered to
`Production<Class<@StandardResource>>: @StandardResource` and bound to `Plant`, becomes
`Production<Class<Plant>>: Plant`.

> **Present limitation — two-stage specialization.** When component specialization supplies an
> abstract value for a class-header variable, that value replaces the variable before trigger
> matching. Pets cannot currently both constrain an effect-local trigger variable to that header
> value and then bind it to the triggering event's more specific type for use in the instruction. A
> separate local marker can capture the event's type, but has no relationship to the header
> variable.

> **Non-normative example — Manutech.** The production increase is one choice region and the gained
> resource is another. The anonymous marker makes both regions choose the same resource, so an
> increase in titanium production rewards titanium.

Other abstract expressions in the effect remain ordinary types, settled in their own positions. By
the time the instruction leaves the effect's lexical scope, its references have been expanded to the
chosen structural type.

**T13-7. Other construct-local variables.** An action, a `THEN` sequence, a full transmutation, an
`EACH` or a `RANK` marks a shared choice with matching occurrences. The construct determines which
choosing or matching occurrence supplies the value, and that occurrence need not come first in the
source. An unmarked type belongs only to the region where it is written. A represented-class
variable may apply dependency arguments (T13-1).

Selector scopes bind before symmetric inner constructs settle their remaining markers. An `EACH` or
`RANK` marker therefore stays visible through a nested full transmutation or `THEN`, and the inner
construct declares only matching markers that are still unbound. Symmetric scopes settle inside out,
so a full transmutation outranks an enclosing sequence. A selector's refinement takes part in
filtering candidates, but the value exposed to its body or metrics is the selected concrete type,
without that refinement.

| Construct | Supplying occurrence | Other marked region |
| --- | --- | --- |
| Action | a choosing or matching marker in the cost | the result |
| `THEN` sequence | a choosing or matching marker in any stage | another stage |
| Full transmutation (`A FROM B`) | a choosing marker on either side | the other side |
| `EACH` | the marked selector | the body |
| `RANK` | the marked selector | the comparison metrics |

The first two are **settlement sites**: parts of one rule that are settled separately, across which
"the same one" is worth saying. An action's two regions are the two stages its arrow lowers to
(L7-3), and `X` is shared across exactly the same regions (L2-11).

A full transmutation is different: its sides are settled together as one atomic pair. Matching
markers on its gained and removed sides share one choice. A non-observing marker on either side can
supply that choice, so the destination may use the selected source in a refinement just as the
source may use the selected destination. Compact `FROM` has its own instruction syntax and declares
no variable (L2-4).

> **Non-normative example — Market Manipulation.**
> `ColonyProduction(NOT Source@ColonyProduction) FROM Source@ColonyProduction` moves one step to a
> different colony track. The source marker supplies the track that the destination excludes.

> **Non-normative example — Kaguya Tech.** `CityTile<@MarsArea> FROM GreeneryTile<@MarsArea>`
> replaces one of its owner's greeneries with a city in the same area. Neither marker has a
> different role from the other; together they say that the two areas vary as one. Without the
> markers, the city could go on any Mars area at all.

**T13-8. Only choosing and matching occurrences supply construct-local variables.** An ordinary
unmarked expression has no variable identity. An observing expression may use a visible variable,
but cannot introduce one.

| Occurrence | Variable rule |
| --- | --- |
| An ordinary unmarked expression in a local construct | it is settled in its own position |
| A requirement | it observes candidates rather than choosing one |
| A metric | it ranges over a domain rather than picking one member |
| A refinement | it tests a candidate chosen or matched outside it |
| An `EACH` or `RANK` selector | a marker exposes its selected value to the body or metrics |
| A represented class inside a refined `Class<T>` literal | its selected class is exposed to the refinement |
| A concrete expression, or `This` | it has no open choice to bind |

The three observing rows are the first property in T13-1: an occurrence that only looks never
introduces a variable, but may use one whose choice is available in the same settlement region or
an earlier one. That is why the gate in Cyberia Systems'
`(BuildingTag<First@CardFront>: CopyProductionBox<First@CardFront>) THEN ...` speaks about the same
card its stage chooses, rather than ranging over cards of its own.

> **Non-normative examples — Sponsor and `EACH`.** The Sponsor milestone's requirement must count
> three independently matching expensive cards, not capture the first `CardFront(HAS 20 cost)` and
> demand three copies of it. Conversely, `EACH @Class<GlobalParameter> { GpIncomplete<@Class> }`
> explicitly uses the particular track selected for each branch.

**T13-9. Actor specialization.** A `BY` selector constrains the actor recorded on the triggering
event. An unmarked selector is only a filter. A marker joins the selector to other occurrences of a
variable, whose value the event's concrete actor supplies. That value is bound before the inner
trigger is matched.

Other selectors do not become declarations merely by following `BY`. `BY Anyone` alone is the
unrestricted wildcard (T6-6), and a refined selector alone is a constraint. A selector exposes its
actor elsewhere in the effect only through an explicit marker, as `BY @Player` referenced as
`@Player`. `BY Anyone` stays a wildcard unless it is marked.

Where an actor variable is visible, an exclusion may use it, and the difference is tested only
after the actor is bound. The canonical resource-removal watcher is:

```text
-X @Resource<Victim@Owner(NOT Attacker@Player)> BY Attacker@Player::
    MyResourceWasRemoved<Victim@Owner, Class<@Resource>, Attacker@Player>.
```

When Player1 removes a resource, `Attacker@Player` is bound to `Player1`, giving
`@Resource<Victim@Owner(NOT Player1)> BY Player1`. `Victim@Owner` then captures the particular
other player whose resource it was. This keeps "anyone but the actor" distinct from "the particular
other player this event was about".

> **Non-normative examples — Hydrologist and Aphrodite.** The Hydrologist milestone's watcher says
> `@OceanTile BY @Player: OceanCredit<@Player, @OceanTile>`. When Player 2 places an ocean,
> `@Player` is bound to `Player2`, so the credit belongs to the placer. Aphrodite says
> `VenusStep BY Anyone: 2 MC`. It does not name the wildcard, so the wildcard only removes the actor
> restriction, and the money goes to Aphrodite's owner.

> **Non-normative design note — `BY` supplies an explicitly marked value.** `BY` identifies the
> event field that supplies the value, and matching markers identify where it is reused. Binding it
> before the inner trigger matters when that trigger mentions the actor in a `NOT`. An unmarked
> selector remains only an actor filter.

### Binding

**T13-10. Binding replaces the variable's occurrences and nothing else.** Binding a variable to a
value substitutes the value at each occurrence of the variable, and each occurrence keeps its own
arguments. Every unmarked expression stays outside the variable's scope, even one that resolves to
the same type. The one exception is the implicit represented root in a refined class literal
(T8-10).

An occurrence stays an occurrence when elaboration copies it or adds default arguments to it.
Neutral solo setup places `@CityTile<>`, which elaboration expands with the city's gain default.
Binding `@CityTile` replaces that expanded declaration and the use in
`GreeneryTile<LandArea(HAS Neighbor<@CityTile>)>`. An unmarked city expression written the same way
elsewhere would be untouched.

For a represented-class variable, binding `@CardResource` to the class `Microbe` makes
`@CardResource<This>` mean `Microbe<This>`. If `Microbe` already narrows the dependency an argument
matches, the two constraints intersect as usual, and a conflict is an error. Application produces an
ordinary type expression. It does not create the invalid class literal `Class<Microbe<This>>`, and
`Class<@CardResource>` written separately still denotes the literal for `Microbe`.

A refinement on the supplying occurrence is consumed by binding. It was already tested when the
value was chosen, so the other occurrences reuse the chosen type without asking the world again.

> **Non-normative example — Turmoil's new chairman.** When a party takes power, its rules run:
>
> ```text
> EACH Leader@Player(HAS PartyLeader<This, Player>) {
>   Chairman<Leader@Player> FROM PartyDelegate<This, Leader@Player>
>   THEN TerraformRating<Leader@Player>
> }
> ```
>
> The refinement is a fact about the moment the leader is selected. The body's first stage moves one
> of that leader's delegates, so asking it again at a later occurrence could give a different
> answer. Binding reuses the selected player instead.

**T13-11. Capture follows dependency paths.** Capturing a variable's value from a specialized
expression reads, for each marked occurrence, the key path its written arguments filled (T3-6,
T3-10), and takes the specialized type's bound at that path. A candidate that lacks the path a
variable sits on captures nothing, rather than a guess drawn from a coincidentally similar type. For
`Class<X>`, the represented class is that path: specializing `Class<@StandardResource>` to
`Class<Steel>` captures `Steel`. Only the paths of the variable's own occurrences are read. An
unmarked position elsewhere in the same type contributes nothing, even when its bound is the same.

> **Non-normative example — Law Suit.** The removal watchers record the victim, the resource class
> and the attacker in distinct dependency positions, and Law Suit later reads that exact record.
> Path-aware capture leaves the unrelated `Player` and `Owner` positions alone.

---

## Appendix A: deliberately unspecified

- **How an error is reported.** A rule says only that something is an error, and, where it matters,
  that an error is not a negative answer.
- **When a derived fact is computed.** Nothing here depends on whether a meet, an enumeration or a
  resolution is computed eagerly, lazily or once and remembered.
