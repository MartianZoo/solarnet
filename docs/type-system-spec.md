# The Pets type system: a specification

This document defines the type system implemented in `dev.martianzoo.pets.types`. It is meant to be
readable start to finish, but it is organized so you can look one rule up and stop.

## How to read this

The specification is divided into **sections**, numbered 1 to 13. Each section states a series of
numbered **rules**. A rule is written `T4-2` — `T` for the type system, then "section 4, rule 2" —
and it is the unit you cite. Its peer document, [the Pets language specification](pets-language-spec.md),
numbers its rules `L4-2` the same way, so one rule id belongs to exactly one document.

Every rule is checked by tests whose names begin with the same id, in
`test/common/dev/martianzoo/pets/types/`:

| Section | Test file |
| --- | --- |
| 1. Universes and identity | `Spec01UniversesTest.kt` |
| 2. Classes | `Spec02ClassesTest.kt` |
| 3. Dependencies | `Spec03DependenciesTest.kt` |
| 4. Class literals | `Spec04ClassLiteralsTest.kt` |
| 5. Types | `Spec05TypesTest.kt` |
| 6. Subtyping | `Spec06SubtypingTest.kt` |
| 7. Bounds | `Spec07BoundsTest.kt` |
| 8. Refinements | `Spec08RefinementsTest.kt` |
| 9. Class properties | `Spec09PropertiesTest.kt` |
| 10. Defaults | `Spec10DefaultsTest.kt` |
| 11. Enumeration and automatic narrowing | `Spec11EnumerationTest.kt` |
| 12. Inhabitance | `Spec12InhabitanceTest.kt` |
| 13. Type variables | `Spec13TypeVariablesTest.kt` |

So `grep -rn "T8-9" docs/type-system-spec.md test/common/dev/martianzoo/pets/types/` finds a rule and
everything that proves it. Where a rule and the implementation are known to disagree, the rule says
so and Appendix A records the departure. There is one such departure today.

Examples use real Terraforming Mars component names — `GreeneryTile`, `Tharsis_2_2`, `Plant`,
`Cardbound` — but the declarations shown are simplified. They illustrate a rule; they are not a
transcript of `tfm-canon`.

Blockquoted **Non-normative example** and **Non-normative implementation note** insets explain why
an otherwise surprising provision exists. They are evidence and orientation, not additional rules.

### Notation

| Written | Means |
| --- | --- |
| `A <: B` | every A is a B; A is a context-free structural subtype of B |
| `A ⊓ B` | the greatest lower bound (`glb`) of A and B: the most specific type below both |
| `CLASS Foo` | Pets source for a class declaration |
| `Foo<Bar>` | Pets source for a type expression |

A few terms are used precisely throughout:

- A **class** is a named node of the nominal hierarchy, compiled from one `CLASS` declaration.
- A **type** is a class together with a bound for each of its dependencies, and optionally a
  refinement. `GreeneryTile<Tharsis_2_2, Player1>` is a type; `GreeneryTile` is a class.
- A **component** is one occurrence of a concrete type in a game world. This specification is about
  types, not worlds; it mentions components only to explain what a type *means*.
- A **world** is whatever can answer "does this requirement hold right now?" — the `TypeInfo`
  interface. Most of the type system never needs one. Where a rule does, it says so.
- A **master class table** is the complete immutable class model compiled once from a Catalog's
  reusable declarations.
- A **premise class table** is one game's small declaration delta. It imports exactly one master;
  the master cannot refer back to it, and its names cannot replace master names.
- A **universe** combines one master class table with at most one premise class table and one
  game's premise-selected view.

### What a structural type denotes

A universe is closed and every concrete class is final (T2-3), so the concrete types below any
structural type form a fixed, finite set. Call it that type's **extension**. `Tile`'s extension is
every concrete tile type in the universe; `Tharsis_2_2`'s is itself; an uninhabited type's is empty.

Most of this specification is about that set, and several rules are already stated in terms of it: a
difference removes members (T8-4), enumeration lists them (T11-1), inhabitance asks whether any
remain (T12-4), and automatic narrowing asks whether exactly one does (T11-4). Two properties tie
the rest to it:

- **Subtyping is sound for it.** If `A <: B` then every member of A's extension is a member of B's.
- **A meet is exact when it exists.** When `A ⊓ B` is a type, its extension is exactly the overlap
  of theirs (T7-1).

The converses are deliberately not claimed. Subtyping is nominal (T2-4): an abstract class whose
only concrete subclass is `Foo` has the same extension as `Foo` without becoming the same type, and
`Tile ⊓ Owned` can be absent although concrete owned tiles exist. Pets recognizes the classes a
Catalog declared rather than manufacturing types to name every set, and that is what keeps a type's
meaning stable as a Catalog grows.

A `HAS` refinement (section 8) filters this set further, and only a world can say how.

### Refinements are types

This specification treats refinement types as genuine types. A `HAS` refinement gives its type a
denotation relative to a world, so a narrowing judgment involving it may need that world as an
input. `TypeInfo` supplies the environment for that judgment; it does not complete an otherwise
incomplete type. A **structural type** means the refinement-free subset of Types, recursively through
its dependencies. `GroundType` means only that a Type is not a Type variable; a Ground Type may
carry refinements.

Not every Type is legal in every role. In particular, a component needs one concrete Type as its
state-independent identity, and Class signatures accept only structural Types. Those restrictions
do not make refinement types a separate kind of expression.

### What this document does not cover

Three neighbours are deliberately out of scope:

- **How a premise selects its declaration closure.** Section 12 takes that closure as part of the
  game universe and defines inhabitance for its Types. The selection policy belongs to premise
  construction; its behavior is pinned by `ActivationTest.kt` and described in
  `docs/agents/OPTIONS.md`.
- **Component-count invariants**, except for the one rule the type system leans on (T3-9): a
  dependency may only target a type limited to a single copy.
- **What the rest of Pets means.** Instructions, requirements, metrics, triggers and declarations
  are the subject of [the Pets language specification](pets-language-spec.md), whose rules are cited
  here as `L6-9`. What an engine does about a task queue is `docs/agents/ENGINE.md`'s.

---

## 1. Universes and identity

Compiling a Catalog produces one reusable **master class table**. A game combines that master with
its small **premise class table**, which contains generated Players, its generated `Premise`, and
any ad-hoc declarations. The resulting frozen class table is that game's universe. Nothing in this
specification is meaningful except relative to one compatible master or game universe.

**T1-1. One class per name.** A Catalog compiles to exactly one reusable class object for each
master declaration. Each game universe constructs exactly one class object for each premise
declaration and reuses its master's objects. Two separately compiled masters over identical source
are different and their classes and types are not equal.

**T1-2. Values are universe-scoped.** A master value can be interpreted by that master or by one
game universe importing it. A premise value belongs only to its game universe. An operation that
combines unrelated masters, or two distinct premise universes, raises `IllegalArgumentException`.
It does not quietly answer "no". Operations whose answer can depend on premise classes take the
interpreting `ClassTable` explicitly; `ClassTable.knows(type)` is the safe question to ask first.

This matters because a false "not a subtype" would silently misroute a trigger, whereas an exception
stops the caller at the bug.

> **Non-normative implementation note — Catalog isolation.** No card asks whether two universes are
> equal. This guard prevents a type retained from one compiled Catalog from silently failing to match
> the identically named trigger in another, which would look like a legal card simply did nothing.

**T1-3. Resolution is a function of the written expression and table.** `ClassTable.resolve` maps
an `Expression` to a type. A game delegates master-only expressions to its master except when a
structural refinement must account for premise subclasses. Within one table, repeated
resolution of the same expression and different spellings of one type yield equal types:

```text
GreeneryTile<Area>  and  GreeneryTile   →  equal types
GreeneryTile        and  GreeneryTile   →  equal types
```

A type's own renderings (T5-4, T5-5) always resolve to a type equal to it. Reference identity is not
part of the contract.

> **Non-normative implementation note — caching is not semantics.** An implementation may cache or
> intern resolved types, but callers must use equality. No gameplay rule distinguishes synonymous
> spellings such as `GreeneryTile` and `GreeneryTile<Area>`.

**T1-4. `Component` is the root.** Every universe contains an abstract class `Component` with no
supertypes and no dependencies. Every other class has it as a supertype.

**T1-5. `Class` is the other required class.** Its base type is `Class<Component>`. Section 4 covers
it.

**T1-6. Enumeration requires a frozen table.** A master or combined game table is built by loading
classes and then freezing. Lookup (`findClass`, `resolve`) works during loading; anything that
enumerates the universe — `allClasses`, `allClassNames`, `allSubclasses`, `directSubclasses`, and
therefore `glb` between unrelated classes — requires the table to be frozen first.

Before returning the completed table, compilation resolves every class's structural base type.
Undeclared names are also rejected while loading. Authored expressions inside effects are resolved
when that effect is first elaborated, when its class and game context are available; invalid
expressions fail at that boundary. Judgments that require a world remain deferred.

> **Non-normative example — claiming a milestone.** `ClaimMilestoneAction` asks for a concrete
> `Milestone`. The answer is not knowable while map modules are still being loaded: Elysium may yet
> add `Legend5`, while another map supplies a different set. Freezing makes that menu a result
> of the completed Catalog instead of declaration order.

**T1-7. Only the exact declared name resolves.** There are no abbreviations, no case folding, no
nearest-match. An unknown name raises `ExpressionException`. Master declarations are checked
against the master namespace and therefore cannot name premise classes. Premise declarations are
checked against their combined master-and-premise namespace. Duplicate premise names and collisions
with master names are rejected.

---

## 2. Classes

**T2-1. A declaration introduces a class and its base type.** `CLASS Foo` declares a concrete class;
`ABSTRACT CLASS Foo` declares an abstract one. The compiled class retains the declaration it came
from, including its docstring.

Concrete and abstract mean what they usually do: components exist only for concrete classes, and
only abstract classes can be extended (T2-3).

**T2-2. Supertypes.** A class may name any number of abstract direct supertypes. A class that names
none extends `Component` implicitly; naming `Component` explicitly is an error, because it says
nothing.

Nesting a declaration inside another is shorthand for naming the enclosing class as a supertype:

```pets
ABSTRACT CLASS Area {
  ABSTRACT CLASS MarsArea {
    ABSTRACT CLASS LandArea { CLASS Tharsis_2_2 }
  }
}
```

is exactly `ABSTRACT CLASS MarsArea : Area`, and so on.

**T2-3. A concrete class is final.** No class may extend a concrete one, so a concrete class's only
subclass is itself. This is what makes "narrow this to a concrete type" a terminating operation: a
player who chooses `GreeneryTile<Tharsis_2_2, Player1>` cannot then be asked to choose again.

**T2-4. The subclass relation.** It is reflexive, transitive, and antisymmetric, and it is *nominal*:
`LandArea` is below `MarsArea` because it says so, not because their shapes agree. `isSubtypeOf`
answers; `isSupertypeOf` is its converse; `ensureNarrows` throws `NarrowingException` instead of
returning false.

**T2-5. Cycles are rejected.** A class may not be its own supertype, directly or through a chain.

**T2-6. Declaration order is irrelevant.** A supertype may be declared after its subclass.

**T2-7. The hierarchy can be walked in both directions.** A class knows `allSuperclasses()` (itself
included). Downward traversal is table-relative: `ClassTable.allSubclasses(klass)` and
`ClassTable.directSubclasses(klass)` enumerate the subclasses visible in that table. They need a
frozen table (T1-6).

**T2-8. Greatest lower bound of two classes (`⊓`).** `ClassTable.glb(left, right)` interprets both
classes in that table. If one operand is below the other, that one is the answer. Otherwise Pets
looks in the table's complete structural universe for a *unique greatest common subclass*: a class
below both, which every other class below both is also below. If there is no such class — because the
two are disjoint, or because two rival classes combine them — the result is **absent** (`null`).

```pets
ABSTRACT CLASS Tile
ABSTRACT CLASS OwnedTile : Tile, Owned
CLASS GreeneryTile : OwnedTile
```

`Tile ⊓ Owned` is `OwnedTile`. Add `CLASS CommercialDistrictTile : Tile, Owned` and it becomes
absent: Pets does not manufacture a structural conjunction, it only recognizes a class you declared.

**T2-9. Custom classes.** A class declared `: Custom` has its behavior supplied by Kotlin instead of
Pets. A declaration and an implementation must agree: a class declared `Custom` with no
implementation is rejected, and so is an implementation for a class not declared `Custom` — including
for a root class. A `Custom` class may also not *inherit* Pets behavior: no supertype of it may
declare effects, invariants, or instruction-quantifier defaults. A load that fails these checks is
not cached as a success; loading again fails the same way.

> **Non-normative example — Robotic Workforce.** Its `CopyProductionBox` is a custom component:
> Kotlin copies the selected building card's production box. The agreement checks prevent that
> opaque implementation from being accidentally loaded as ordinary Pets behavior, or vice versa.

**T2-10. Class identity.** Within a universe, a class is identified by its name. Its `toString` is
that name.

---

## 3. Dependencies

A type argument in Pets is not a conventional generic parameter. It is a **dependency**: an edge to
one specific other component that must exist for this one to exist. `Plant<Player1>` needs
`Player1`; `GreeneryTile<Tharsis_2_2, Player1>` needs both the area and the player.

> **Non-normative design note — concrete Types are the values.** A world deliberately has no
> occurrence identity or mutable instance fields: it is a multiset whose keys are concrete Types.
> Thus `Player1` and `Tharsis_2_2` are values precisely by being concrete Types, while multiplicity
> records how many indistinguishable occurrences of a value exist. A dependency can identify a
> target only when that target Type is singleton (T3-9). This is a chosen boundary of the model, not
> an attempt to simulate object references with incomplete identity.

**T3-1. Keys.** Every dependency a class declares gets a **key**: the declaring class's name plus the
zero-based slot, written `Occupant_0`, `Owned_0`, `Adjacency_1`. The key, not the position, is the
dependency's identity.

> **Non-normative example — ocean credits.** `OceanCredit<OceanTile>` inherits its player edge from
> `Owned` and declares its ocean edge separately. Stable declaring-class keys let Hydrologist's
> watcher bind the credited player and placed ocean without inheritance order renumbering either
> fact.

**T3-2. Inheritance.** A subclass inherits every dependency under its original key, and may narrow
its bound by writing the supertype with arguments. Dependencies a subclass declares itself come
*after* the inherited ones.

```pets
ABSTRACT CLASS Occupant<Area>
ABSTRACT CLASS Tile : Occupant
CLASS GreeneryTile : Tile<MarsArea>, Owned<Owner>
```

`GreeneryTile` has keys `[Occupant_0, Owned_0]` and base type
`GreeneryTile<MarsArea, Owner>` — the area edge was narrowed, never copied or renamed.

**T3-3. Several supertypes, one key.** When more than one supertype constrains the same key, the
bounds are intersected (`⊓`, rule T7-1). Bounds with no common narrowing are an error.

> **Non-normative example — Predators.** Predators is simultaneously an action card, active card,
> and animal-resource card. Those inheritance paths converge on shared card-front dependencies;
> meeting their compatible constraints produces one physical card identity instead of three
> unrelated edges.

**T3-4. An argument intersects the bound; it never replaces it.** Writing a wider argument therefore
changes nothing, and writing one outside the bound is an error.

This is what makes `Anyone` work. `Anyone` is an ordinary class at the top of the ownership
hierarchy, so `Anyone` intersected with a narrower declared bound is that narrower bound:

```text
ProjectCard : Owned<Player>    →  ProjectCard<Anyone>  is  ProjectCard<Player>
Plant       : Owned<Owner>     →  Plant<Anyone>        is  Plant<Owner>
ProjectCard<SoloOpponent>                              →  error: not a Player
```

> **Non-normative example — Solar Logistics.** Its trigger is written `EventCard<Anyone>(HAS
> SpaceTag)`. `EventCard` is owned only by a `Player`; intersecting with `Anyone` preserves that
> bound. Replacing it would also admit a `SoloOpponent`, changing who can trigger the card.

**T3-5. Argument matching is greedy, left to right.** Each written argument takes the first
not-yet-taken dependency whose bound it can intersect. An argument that matches nothing is an error.

A useful consequence: when the bounds are disjoint, argument order does not matter.
`GreeneryTile<Tharsis_2_2, Player1>` and `GreeneryTile<Player1, Tharsis_2_2>` are the same type.
Order *is* meaningful when two bounds overlap, as in `Adjacency<Area, Area>`, and then
`Adjacency<Tharsis_2_2>` fills the first slot and leaves the second open.

> **Non-normative example — Tharsis Republic.** `CityTile<Anyone, MarsArea>` supplies owner and area
> in an order that does not mirror inherited dependency-key order. Compatibility matching puts each
> argument into the slot it can actually inhabit; positional generics would misread the trigger.

**T3-6. Which key an argument filled is recoverable.** `Class.matchDependencyKeys(arguments)` replays
the match and reports the key each authored argument took, for callers that need to remember what was
supplied rather than only the resulting type.

> **Non-normative example — Kaguya Tech.** `CityTile<MarsArea> FROM GreeneryTile<MarsArea>` replaces
> a greenery with a city in that same area. After compatibility-based argument matching, variable
> capture needs the filled keys to remember that both written `MarsArea`s name the location edge.

**T3-7. `This` in a header argument names the inheriting class.** In a declared dependency or a
supertype argument, it is rebound at each level of the hierarchy, in place, leaving every other
argument alone:

```pets
ABSTRACT CLASS Link<Class<Component>>
ABSTRACT CLASS SelfBound : Link<Class<This>>
CLASS SelfLeaf : SelfBound
```

gives `SelfLeaf<Class<SelfLeaf>>`, while writing the class name literally
(`Link<Class<SelfBound>>`) would have given `SelfLeaf<Class<SelfBound>>`.

The same rule applies within a declared dependency:

```pets
ABSTRACT CLASS Holder<Class<Component>>
ABSTRACT CLASS Held<Holder<Class<This>>>
CLASS HeldLeaf : Held
```

Here `HeldLeaf` has the bound `Holder<Class<HeldLeaf>>`.

> **Non-normative implementation note — presently general-purpose.** Canonical Terraforming Mars
> uses the related `Class<This>` rule (T4-9), but no current canonical class needs bare `This` in a
> header argument. T3-7 states the general rebinding rule rather than a card-specific exception.

**T3-8. One header variable in two positions forces them to agree.** When the same type variable
occupies two dependency paths of a class header (section 13 defines what that means), resolving a
type of that class propagates a choice from either position into the other:

```pets
ABSTRACT CLASS CardFront : Owned<Owner>
ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner> { CLASS Animal }
```

The `Owner` inside `CardFront<Owner>` and the `Owner` of `Owned<Owner>` are the same variable, so
an animal's owner is necessarily the owner of the card it lives on:

```text
Animal<Player1>             →  Animal<Player1, CardFront<Player1>>
Animal<Pets<Player1>>       →  Animal<Player1, Pets<Player1>>
Animal<Player1, Pets<Player2>>  →  error
```

Two dependency *roots* spelled alike stay independent: `Adjacency<Area, Area>` constrains nothing.

> **Non-normative example — Pets.** An `Animal<Pets<Player1>>` must also be owned by Player 1.
> Without propagation between the two appearances of the header's owner variable, an animal on
> Player 1's Pets card could acquire Player 2 as its independent owner.

**T3-9. A dependency may only target a type limited to one copy.** An edge names its target by exact
type alone, so a type admitting two identical components could not say which one it meant. Every
concrete type a dependency bound admits must therefore carry an applicable `MAX 1` or `=1` invariant.
This is checked when a game's component-limit table is built, and it is the only place
component-count invariants enter this specification.

`Signal` and every subtype of `Signal`, as well as `Die`, are never valid dependency targets,
regardless of component-count limits. A Signal is a point event removed immediately after firing,
and `Die` has no legal occurrence; neither can anchor the existence of another Component.

> **Non-normative example — action-used markers.** `ActionUsedMarker<ActionCard>` means the marker on
> one exact owned action card. If two indistinguishable components of that target type could exist,
> the dependency would identify neither one, and Project Inspection could offer the wrong card for
> its additional use.

**T3-10. Dependency sets.** A type's dependencies form a keyed set: `get(key)`, `getIfPresent(key)`,
`keys` in declaration order. Equality is key-wise and ignores order. `flatten()` walks nested paths
(`Cardbound_0.Owned_0`), and `at(path)` reads one. `narrowedDependencies` reports only what a type
narrowed below its own class's base type.

**T3-11. Cycles are rejected.** Two classes may refer to each other freely, but a genuine cycle of
dependency *bounds* — `CLASS Foo<Bar>` with `CLASS Bar<Foo>`, or `CLASS Foo<Foo>` — has no finite
answer and raises `PetException` when the bounds are computed. A one-way chain is fine.

---

## 4. Class literals

A dependency asserts that a component exists. `Class<X>` instead names a class *as data*:
`Production<Class<Steel>>` is a steel production, and needs no steel cube to exist.

One `Class<Foo>` component exists for each concrete Class whose base Type is inhabited, so
`Class<X>` can also be the target of an ordinary dependency without violating T3-9.

**T4-1. Form.** `Class<X>` takes exactly one bare class name. The `Class` class's own base type is
`Class<Component>`, so bare `Class` means "some class".

**T4-2. Concreteness depends only on the class named.** Not on that class's dependencies:

```text
CityTile          is abstract  — its area has not been chosen
Class<CityTile>   is concrete  — `CityTile` is one specific class
Class<Metal>      is abstract  — `Metal` is not
```

Like every concrete Type, a concrete class literal can still be uninhabited in a particular
universe (T12-4).

> **Non-normative example — the project-card deck.** A bare `ProjectCard` type is abstract because
> its location and owner are open, yet `Class<ProjectCard>` is the one concrete representative of
> that category. Card-front declarations depend on the representative, not a card in one player's
> hand.

**T4-3. Class literals are covariant.** `Class<Steel> <: Class<Metal> <: Class<StandardResource>`,
and this carries into dependency positions:
`Production<Class<Steel>> <: Production<Class<Metal>>`.

> **Non-normative example — Manutech.** Its
> `PROD[StandardResource AS SR]: SR` trigger must observe a steel-production
> increase and pay steel. That works because `Class<Steel>` narrows
> `Class<StandardResource>` through the same covariance as ordinary dependencies.

**T4-4. `representedClass`** returns the named class, and is absent for every type that is not a class
literal.

**T4-5. Greatest lower bounds follow the class hierarchy.** `Class<Metal> ⊓ Class<Steel>` is
`Class<Steel>`; the `glb` of literals for disjoint classes is absent.

**T4-6. The operand is one bare, existing class name.** All of these are errors:

```text
Class<Steel, Plant>            two operands
Class<Steel<Player1>>          the operand has arguments
Class<Class<Steel>>            nested literals
Class<Jackalope>               no such class
```

`Class<Class>` is fine — `Class` is itself a class, and a concrete one. The named class must exist
even where a declaration merely counts one (`HAS MAX 0 Class<Jackalope>`). Finally, an effect may
not *gain* a class representative: the one component per concrete class is fixed before any effect
runs.

> **Non-normative example — Collector.** Collector uses `Class<Resource>(HAS Resource<Owner>)` to
> count fixed resource-kind representatives. Allowing an argument-bearing operand such as
> `Class<Resource<Player1>>` would turn the supposedly global representative into stateful,
> player-specific data with no defined component identity.

**T4-7. The slot inside a literal is not a component dependency.** It holds a class, so it does not
appear among a type's `typeDependencies`. A class that *declares* a dependency bounded by a class
literal — `CLASS Production<Class<StandardResource>>` — has an ordinary dependency whose bound
happens to be a class literal.

> **Non-normative example — Mine.** Mine grants steel production without granting any steel cubes.
> If `Class<Steel>` were treated as a dependency on a steel component, that ordinary card effect
> would be ill-typed whenever the player had no steel in stock.

**T4-8. Enumeration.** The concrete narrowings of `Class<Metal>` are `Class<Steel>` and
`Class<Titanium>` — one per concrete subclass whose base Type is inhabited. A literal whose
represented Class has no such subclass enumerates nothing.

> **Non-normative example — the solo opponent.** Solo setup uses `EACH Class<StandardResource>` and
> `EACH Class<CardResource>` to create one reserve per concrete resource kind. Enumerating stock
> components instead would omit zero-stock kinds and duplicate kinds with several cubes.

**T4-9. `Class<This>`** follows rule T3-7: it names the inheriting class. This is how a card resource
knows which holder can contain it:

```pets
ABSTRACT CLASS ResourceHolder<Class<CardResource>>
ABSTRACT CLASS CardResource<ResourceHolder<Class<This>>> { CLASS Animal, Microbe }
CLASS Fish : ResourceHolder<Class<Animal>>
```

`Animal`'s holder bound becomes `ResourceHolder<Class<Animal>>`, so `Animal<Fish>` resolves while a
holder specialized for microbes does not.

> **Non-normative example — played events.** `EventCard` removes itself into
> `PlayedEvent<Class<This>>`. When Asteroid is played, rebinding records `Class<Asteroid>`; retaining
> the abstract `EventCard` class would lose which event face was played and break per-card scoring.

---

## 5. Types

**T5-1. What a type is.** A type is a root class, one bound for each of that class's dependency keys,
and optionally a refinement (section 8). Two types are equal when those three agree — however each
was written.

**T5-2. A bare class name means that class's base type**, which supplies every dependency's declared
bound. An explicit empty argument list, `GreeneryTile<>`, means the same type. (The two spellings
differ elsewhere in Pets: in an instruction, writing `<>` says "I accept this use's defaults on
purpose". That rule belongs to instructions, not to types; see L3-2 and L12-5 of
[the Pets language specification](pets-language-spec.md).)

> **Non-normative example — Capital.** Capital deliberately says `CityTile<> THEN
> CapitalMarker<CityTile<>>`: use the ordinary city-placement default, and mark that same city. At
> the type level `CityTile` and `CityTile<>` must therefore denote the same base type even though the
> instruction spelling records that the default was intentional.

**T5-3. Abstractness.** A type is abstract if its root class is abstract, **or** any dependency bound
is abstract, **or** it carries a refinement. Only a concrete type can describe a component.

```text
GreeneryTile                      abstract — no area, no owner
GreeneryTile<Tharsis_2_2>         abstract — no owner
GreeneryTile<Tharsis_2_2, Player1>  concrete
GreeneryTile<Tharsis_2_2, Player1>(HAS Neighbor)  abstract — a refinement is a question
```

This is the source of a common confusion: the *class* `OceanTile` is concrete, while the *type*
`OceanTile` — short for `OceanTile<WaterArea>` — is not.

Abstractness is structural and never consults a world.

Abstractness and inhabitance are different questions. A concrete Type can be uninhabited because
its root Class is outside a game's declaration closure or one of its dependency domains is empty.
An abstract Type is inhabited when its universe contains at least one concrete narrowing, and
uninhabited when it contains none (T12-4).

> **Non-normative example — Research Outpost.** Its city must occupy
> `LandArea(HAS MAX 0 Neighbor)`. Even if exactly one board space currently satisfies that query, the
> refined type itself remains abstract; the world may narrow it to a concrete area, but cannot turn
> a state-dependent question into component identity.

**T5-4. Full form.** `expressionFull` writes every dependency, in key order. For the
`GreeneryTile` of rule T3-2 that is `GreeneryTile<MarsArea, Owner>`; had `Owned` been inherited
first, the same type would be written `GreeneryTile<Owner, MarsArea>`.

**T5-5. Compact form.** `expression` — also what `toString` shows — writes a round-tripping argument
list in dependency-key order, whatever order the arguments were supplied in. It first retains an
argument when one of these holds:

- its bound differs from what the class already declares; or
- omitting it would leave its slot free to swallow a later argument under T3-4.

It then repeatedly omits the first retained argument whose removal still resolves to the same Type,
including through T3-8 equality propagation. Each retained bound is itself written in compact form,
and a refinement is always written.

Thus `GreeneryTile<MarsArea, Player1>` is written `GreeneryTile<Player1>`: the area slot rejects
`Player1`, so leaving it out cannot misdirect the owner. But `Neighbor`'s two slots both accept an
`Area`, so `Neighbor<Area, Tharsis_2_2>` must keep both arguments while `Neighbor<Tharsis_2_2, Area>`
drops the second. `Animal<Player1, Pets<Player1>>` becomes `Animal<Pets<Player1>>`, because the
card-bound owner determines the direct owner through T3-8.

> **Non-normative implementation note — why not search.** The direct pass handles declared bounds
> and greedy slot capture. A semantic pass then tries individual omissions against ordinary Type
> resolution, avoiding a second implementation of T3-8. Finding the globally shortest subsequence
> can require subset search; compact form promises only that no remaining argument can individually
> be removed while preserving the Type.

**T5-6. Both forms round-trip.** Resolving either form of a type yields that same type.

**T5-7. Building a type directly.** `Class.withAllDependencies(deps)` needs a bound for every one of
that class's own keys; a missing one is an error. A bound for a key the class does *not* have is
ignored, which is exactly what projects a type onto each of its supertypes — a greenery tile's owner
is not part of what an `Occupant` is. `Class.specialize(arguments)` applies arguments to the base
type, using the matching rule of T3-5. Every supplied bound is intersected with the Class's declared
bound; direct construction cannot create a Type outside the Class declaration.

> **Non-normative example — greenery placement.** A concrete greenery carries both its board area
> and owner. Projecting it to `Occupant` must retain the area while dropping the unrelated ownership
> key; rejecting extra keys would make ordinary multiple-inheritance subtyping impossible.

**T5-8. `Type` has two forms.** A `GroundType` is an ordinary resolved, non-variable type,
optionally carrying a refinement. Its resolved interpretation is itself, and its `typeVariable` is
absent. The other form is a type variable; see section 13. Code that only needs classes,
dependencies or narrowing can treat both uniformly through their resolved interpretation.

---

## 6. Subtyping

Pets uses two related judgments. **Subtyping** is context-free: `A <: B` says every A is a B from
the Types alone. **Narrowing** is contextual: it additionally asks whether A is an acceptable way to
settle B in one world, including whether a concrete candidate satisfies B's `HAS` refinement. Every
subtype is a narrowing; a narrowing that needs live-state evidence is not thereby a subtype.

**T6-1. The two judgments.** `narrows(that, info)` answers contextual narrowing;
`ensureNarrows(that, info)` is its throwing form. `isSubtypeOf` and `isSupertypeOf` answer
context-free subtyping. They raise `IllegalStateException` rather than guess when a comparison asks
for live-state evidence (T8-8).

> **Non-normative implementation note — refusing a plausible lie.** A world-free comparison cannot
> decide whether a concrete area satisfies `HAS Neighbor`. Throwing exposes a caller that chose the
> structural API; returning false would incorrectly report a legal placement as impossible in some
> worlds.

**T6-2. The shared structural rule.** A narrows B when

1. A's root class is a subclass of B's root class, and
2. for every dependency key B constrains, A's bound for that key narrows B's, and
3. B's refinement, if any, accepts A (section 8).

A key B does not have is not checked, which is what lets `GreeneryTile<Tharsis_2_2, Player1>` narrow
`Tile<Tharsis_2_2>` regardless of owner.

**T6-3. Dependencies are covariant.** `Occupant<Tharsis_2_2> <: Occupant<LandArea> <: Occupant<Area>`,
and narrowing the class and a dependency compose freely.

Covariance is the right choice here precisely because a dependency identifies an exact target. A
broad dependency describes a *set of possible* concrete types to query or narrow, not one component
that would accept any member of the set. `ResourceValue<Class<Metal>>` counts the separate steel and
titanium value components; it is not one component that accepts either.

**T6-4. Shape of the relation.** Over structural types it is reflexive, transitive, and
antisymmetric — two types that narrow each other are the same type. With a world in hand and
refinements involved, antisymmetry is the casualty: in a world where every land area has a neighbour,
`LandArea` and `LandArea(HAS Neighbor)` narrow each other while remaining distinct types. Narrowing
with a world is a preorder, not an order.

> **Non-normative example — Hermetic Order of Mars.** Its reward queries empty Mars areas adjacent
> to owned tiles. A particular board state can make that refined set coincide with an unrefined set,
> but the query must not become the same type: placing a tile can separate them again immediately.

**T6-5. Cross-universe comparisons are rejected**, per T1-2.

**T6-6. Constrained narrowing.** `ClassTable.matchesConstraint(candidate, constraint, domain, info)`
asks whether a candidate satisfies a constraint expression *read inside a domain*. The constraint is
first intersected with the domain, then the candidate is tested against the result. This is how a
trigger's `BY` selector other than the unrestricted `Anyone` is applied: with domain `Actor`, the
constraint `Player` accepts `Player1` and rejects `Admin`, and `Actor(NOT Player1)` accepts both
`Player2` and `Admin`. A constraint that cannot meet the domain at all simply answers false.

> **Non-normative example — actor constraints.** A `BY Player` trigger uses this judgment to accept a
> Player and reject Admin. `BY Anyone` is instead the icon-grammar spelling for removing the usual
> Actor restriction altogether, so Aphrodite also reacts to a World Government increase performed by
> Admin. That wildcard is handled before constrained narrowing; it is not the ownership class
> `Anyone` intersected with `Actor`.

---

## 7. Bounds

**T7-1. Greatest lower bound (`⊓`, `ClassTable.glb`).** In the explicitly supplied table, the most
general type below both operands, or **absent** when there is none. It is computed componentwise: the
root classes by T2-8, each shared dependency key by `⊓` again, and refinements by T8-9. The selected
root Class contributes its complete declared dependency set, including keys neither operand had and
bounds narrower than either operand stated.

```text
Tile<Tharsis_2_2>  ⊓  Owned<Player1>       =  OwnedTile<Tharsis_2_2, Player1>
GreeneryTile<Tharsis_2_2>  ⊓  GreeneryTile<Player1>  =  GreeneryTile<Tharsis_2_2, Player1>
Tile<Tharsis_2_2>  ⊓  Tile<Tharsis_2_3>    =  absent
GreeneryTile  ⊓  OceanTile                 =  absent
```

Absent covers two different situations, and every caller that treats it as an error should know
which one it is looking at:

- **The operands are disjoint.** `Tile<Tharsis_2_2> ⊓ Tile<Tharsis_2_3>` is absent because no
  component could be both; their extensions do not overlap. Writing an argument outside a bound
  (T3-4) is this case, and rejecting it is a genuine type error.
- **The overlap is not writable.** `Tile ⊓ Owned`, once two rival classes each extend both, is
  absent although concrete owned tiles exist. This is a limit of naming, not a claim about
  components; a Catalog that needs the intersection declares a class for it, which is why canon
  declares `OwnedTile`.

Where a result does exist it narrows both operands, and no other type below both is outside it.

> **Non-normative example — Protected Valley.** It places a greenery on a water area. The selected
> component must satisfy both the greenery's inherited Mars-area bound and the written `WaterArea`
> constraint; their meet is the legal special placement, not a replacement of one by the other.

**T7-2. `glb` is idempotent and commutative.** Refinement clauses form a set (T8-9), so two meets
that write their clauses in different orders are equal Types even though their renderings may differ.

**T7-3. Cross-universe bounds are rejected**, per T1-2.

---

## 8. Refinements

A refinement turns a type into a filtered version of itself: `LandArea(HAS MAX 0 Tile)` is "an empty
land area". It is a non-empty conjunction of comma-separated clauses. Every clause repeats its
keyword, as in `ActionCard(HAS ActionUsedMarker, NOT Viron)`. There are two kinds of clause, and they
behave very differently.

- `HAS R` asks a **world** whether requirement `R` holds of the
  candidate. This is the only place in the type system that consults game state.
- `NOT X` performs a **structural** exclusion, decided entirely from the class hierarchy.

**T8-1. A refined type is abstract and lies below its unrefined domain.** Narrowing the domain while
keeping the same predicate narrows the refined type: `Tharsis_2_2(HAS Neighbor) <: LandArea(HAS
Neighbor)`.

**T8-2. `HAS` asks the world about the candidate.** Testing whether candidate `c` narrows
`D(HAS R)` substitutes `c` into `R` and asks the world the resulting question. Testing
`Tharsis_2_2` against `LandArea(HAS Neighbor<CityTile>)` asks
`Neighbor<CityTile<Area, Owner>, Tharsis_2_2>`.

> **Non-normative example — the standard greenery action.** `DefaultGreeneryTile` asks for a land
> area adjacent to one of the acting player's tiles. Substituting each candidate area into
> `Neighbor<OwnedTile>` turns that printed condition into the concrete board query that decides
> whether the space is legal.

**T8-3. How the candidate is substituted.** Each outermost expression inside `R` receives the
candidate — not the expressions nested in its own arguments, which say what that expression is about
rather than which candidate is being tested. The candidate takes the first compatible dependency
whose bound it *strictly* narrows, so a slot already holding exactly that type is left alone and the
candidate moves on to one it can still narrow. If it strictly narrows none of the compatible
dependencies, it takes the first compatible one. A bare class property receives it as its receiver,
so `CardFront(HAS MAX 9 cost)` tested against `Ants` asks `MAX 9 Ants.cost`.

If no expression in `R` can accept the candidate, the refinement fails without asking the world at
all. This is not an error; it is the answer. `Component(HAS StartToken)` can only ever match a
player, because that is what a `StartToken` depends on — and testing a rock against it is simply
false.

A written argument *constrains* the candidate in the slot it occupies rather than reserving that
slot away from it. In `Player(HAS MAX 0 This<Anyone>)`, the candidate narrows the written `Anyone`
argument, asking whether that candidate owns `This`.

> **Non-normative examples — CrediCor and Viron.** For CrediCor, substituting a candidate card into
> bare `cost` reads that card's concrete printed cost. For Viron, the candidate must merge into the
> already-written `ActionCard(NOT Viron)` inside `ActionUsedMarker`; treating written arguments as
> occupied slots would make its “another card's action” choice fail.

**T8-4. `NOT` is a structural difference.** `D(NOT X)` is the part of `D` that cannot overlap `X`. A
candidate satisfies it only when its **entire** structural domain avoids `X`:

```text
Player2 <: Owner(NOT Player1)     yes
Player1 <: Owner(NOT Player1)     no
Player  <: Owner(NOT Player1)     no — abstract `Player` still admits Player1
```

The exclusion need not narrow the domain; subtraction goes through their structural intersection.
`Actor(NOT Owner)` excludes players, who inherit both, and retains `Admin`. Overlap is detected even
where the two have no unique greatest common subclass (T2-8): if two rival classes each extend both
`Occupant` and `Owned`, `Occupant(NOT Owned)` still excludes them. The test never consults a world,
and works the same inside a dependency: `Marker<Player(NOT Player1)>`.

> **Non-normative example — Protected Habitats.** The card cancels plant, animal, or microbe removal
> by `Player(NOT Owner)`. That must mean a structurally different player even when the world happens
> to contain no attempted removal; asking game state whether the negation holds would be circular.

**T8-5. The excluded operand must be refinement-free, recursively.** Neither
`Owner(NOT Player(HAS Marker))` nor a nested `NOT` is accepted. This keeps the difference decidable
without a world and prevents negating a world query.

> **Non-normative implementation note — a deliberate language boundary.** No canonical card needs
> to negate a `HAS` query. Rejecting that form keeps `NOT` usable for ownership and actor exclusions
> without introducing closed-world negation (“not currently found” versus “cannot exist”).

**T8-6. A difference that cannot bite is dropped.** If the domain and the exclusion cannot overlap in
the first place, the refinement disappears: `Player1(NOT Player2)` *is* `Player1`.

> **Non-normative example — Philares.** Its neighbouring-tile trigger contains
> `OwnedTile<Anyone(NOT Owner)>`. After the two owners specialize to different concrete players, the
> now-irrelevant exclusion must disappear so the otherwise concrete adjacency can match normally.

**T8-7. A difference that excludes everything is still a type.** `Player1(NOT Player1)` keeps its
refinement, is abstract, and enumerates nothing. It stays representable because an excluded type
variable may be specialized later, making the difference non-empty again.

> **Non-normative example — resource-removal watchers.** Their generic victim is
> `Owner(NOT Player)` until the acting `Player` variable is captured. Discarding an apparently empty
> difference early would erase the opponent restriction before `Player` becomes, say, `Player1`.

**T8-8. Refinements in narrowing.** In order:

- A refined type always narrows its own unrefined domain.
- Every target clause must be satisfied.
- If the narrower type includes an identical clause, that clause is accepted with no world
  consulted.
- If the narrower type's `HAS` clauses include all of a target clause's requirement conjuncts, it
  already guarantees that clause: `LandArea(HAS Neighbor, HAS Occupant) <: LandArea(HAS Neighbor)`.
- Otherwise a refined type never satisfies an unrelated `HAS` clause, and this is decided without a
  world. Different predicates do not imply one another.
- An *unrefined* type tested against a `HAS` target needs a world; asked with none it raises
  `IllegalStateException` rather than guessing.
- A `NOT` target is satisfied by an identical source clause or by the structural test of T8-4.
- These comparisons read the two predicates *as written*, which is only meaningful when both types
  substitute the same candidate into them. Two class literals for different classes do not (T8-10),
  so neither shortcut applies to them: `Class<BuildingTag>(HAS Tag)` does not narrow
  `Class<Tag>(HAS Tag)`, because for the target the predicate asks about the candidate's own class.

> **Non-normative example — Cyberia Systems.** Its second production-box choice is a building card
> with no `CyberiaSystemsFirstChoice` marker. That refined choice must still satisfy the broader
> “building card” constraint without another world query, while the extra conjunct prevents choosing
> the first card twice.

**T8-9. `glb` of refinements.** A refinement the other operand lacks is kept. Refinement clauses form
a set: duplicates collapse and clause order does not affect Type equality. Rendering retains the
order in which distinct clauses were first encountered. Thus both
`LandArea(HAS Neighbor, NOT Tharsis_2_2)` and
`Area(NOT Tharsis_2_2, NOT WaterArea)` are writable results below their two operands.

> **Non-normative implementation note — combining independent restrictions.** Two independently
> inferred constraints can each contribute a clause to one candidate. Their meet retains every
> restriction as a separate clause, producing one writable refinement below both operands.

**T8-10. Refined class literals.** A refinement on `Class<X>` tests the class the candidate names:
references to `X` inside the requirement are rewritten to the candidate's class. Testing
`Class<BuildingTag>` against `Class<Tag>(HAS Tag<Player1>)` asks `BuildingTag<Player1>` — counting
tag classes, not tag components.

> **Non-normative example — Diversifier.** Its milestone requirement counts
> `Class<Tag>(HAS Tag<Owner>)`: distinct tag kinds the player has, not the number of tag components.
> Testing the represented class is what makes five Earth tags count as one kind.

**T8-11. Refinements inside dependencies** behave like any other, and survive rendering and
re-resolution.

> **Non-normative example — Capital.** Its adjacency scoring embeds the marked capital city inside
> `Adjacency<CityTile(HAS CapitalMarker), OceanTile>`. Losing that nested refinement during rendering
> would turn every city–ocean adjacency into Capital points.

---

## 9. Class properties

A **class property** records an immutable fact about a class — a card's cost, a milestone's
requirement, an award's metric. It is not state on a component: every component of one concrete type
necessarily agrees about it.

**T9-1. A property is declared either as a bound or as a value.** The bounds are `Number`, `Metric`,
`Requirement` and `Requirement?`; the values are a literal number, a metric expression, or a
requirement.

```pets
ABSTRACT CLASS CardFront {
  cost = Number
  requirement = Requirement?
}
CLASS Ants : CardFront { cost = 9 }
```

`Requirement?` is the one optional bound: a concrete class may leave it unfilled, and then the
property reads as absent.

> **Non-normative example — Tactician.** Tactician counts cards with a printed requirement via
> `CardFront(HAS requirement)`. Most cards legitimately omit one, so `requirement` must be optional
> without making those concrete card classes incomplete.

**T9-2. A subclass may narrow, never override.** `Metric` may narrow to `Number`, `Number` to a
literal, `Requirement` to a requirement. Overriding a value already fixed by a supertype is an error,
and so is a "narrowing" that is not one.

**T9-3. A concrete class must fix every property it inherits** — except an unfilled `Requirement?`
(T9-1). A concrete class with an unfixed property is rejected at load.

> **Non-normative example — playing any card.** `PlayCard` bills `CardFront.cost`. Requiring every
> concrete card face to fix `cost` catches a missing printed price when the Catalog loads, instead of
> failing halfway through a purchase.

**T9-4. Inheriting one property along several paths.** The same fact arriving twice is one fact. When
one path narrowed further than another, the narrower fact wins — provided the two lie on one chain of
narrowings. Two properties with the same name from unrelated origins are an error, and so are two
divergent narrowings of one property.

> **Non-normative example — Predators.** It inherits card-front properties through `ActionCard`,
> `ActiveCard`, and `ResourceCard`. Those paths all lead back to the same `cost` and `requirement`
> declarations; treating them as three competing properties would reject this real triple-role card.

**T9-5. Reading a property.** A type exposes the values of its root class:
`getNumberPropertyValue`, `getMetricPropertyValue`, `getRequirementPropertyValue` (which returns
`null` for an absent optional). Reading a property that is still a bound, or one that does not exist,
is a programming error and throws.

**T9-6. Properties take no part in type identity or subtyping.** They are facts about the class, not
dependencies. Two cards with different costs are different types because they are different classes,
not because of the costs.

> **Non-normative example — Sponsor.** Sponsor asks for three `CardFront(HAS 20 cost)`. The cost is
> a queryable fact used to filter card classes, not part of their dependency identity; otherwise a
> price change would manufacture a different kind of card component.

---

## 10. Defaults

A `DEFAULT` clause records context that a physical game leaves implicit — that a tile goes on a land
area, that a resource belongs to the current player. It changes how an authored expression *resolves*;
it never changes which types exist.

**T10-1. Three separate sets.** Defaults are gathered independently for all uses (`DEFAULT Foo<...>`),
for gains (`DEFAULT +Foo<...>`) and for removals (`DEFAULT -Foo<...>`). A class's
`defaultExpression` is its authored template with the all-uses defaults applied; its `defaultType`
is that expression's valid, declared-bound-respecting interpretation. The gain and removal sets are
consumed by instructions, not by resolution.

```pets
ABSTRACT CLASS Tile<Area> : Owned<Owner> {
  DEFAULT +Tile<LandArea>
}
```

The gain default says a tile placed without saying where goes on land. `GreeneryTile<Tharsis_1_1>`
still resolves — a water area — because a default is not a bound.

> **Non-normative example — Aquifer.** Gaining `OceanTile<>` should invoke the gain default and ask
> for an empty water area. Merely querying or removing an ocean must not synthesize a future
> placement target, which is why gain, removal, and ordinary-use defaults cannot be one set.

**T10-2. Quantifiers.** A gain or removal default may also carry a quantifier (`!` mandatory, `.`
as-much-as-possible, `?` optional). `Component` supplies `!` for both, so every class inherits
something. Gain and removal quantifiers are inherited independently, and supertypes that disagree
about one are an error.

> **Non-normative example — paying `Owed`.** Creating a debt is mandatory, while removing it defaults
> to as-much-as-possible so mixed payment sources can discharge portions safely. One inherited
> quantifier for both directions would make either debt optional or partial payment illegal.

**T10-3. A `DEFAULT` clause must name the class that declares it.**

> **Non-normative implementation note — defaults have one owner.** No card needs a class declaration
> to install another class's default. Permitting it would make the meaning of bare `OceanTile`, for
> example, depend on whichever unrelated module happened to declare a remote default.

**T10-4. Inheriting dependency defaults.** For one dependency key and one use kind, only the nearest
declaring superclasses survive: anything a nearer superclass overrode is discarded. What survives is
intersected (`⊓`), and survivors with no common narrowing are an error. Each inherited default is also
intersected with the inheriting class's own bound, so a default can only ever get narrower. A default
that merely restates the declared bound records nothing at all.

> **Non-normative example — Ants.** `CardResource` supplies a general resource-holder default, but
> `Microbe` narrows the inherited class-literal bound to `Class<Microbe>`. Intersecting the default
> with that bound prevents a bare microbe gain from selecting an animal-only holder merely because
> both holders accept card resources.

**T10-5. `Owner` in a default stays as written.** This is the one deliberate exception to T10-4: a
literal `Owner` written in a default is *not* intersected with the class's bound, so it can later be
replaced by whichever player supplies the context.

The visible consequence is that a class's `defaultExpression` may sit outside its own base type:

```pets
ABSTRACT CLASS Card : Owned<Player> { DEFAULT Card<Owner> }
```

gives base type and default type `Card<Player>`, but default expression `Card<Owner>`. The expression
is intentionally a template awaiting a context, while every constructed type still respects the
class's declared bound.

> **Non-normative example — CrediCor.** Its setup effect says simply `57 MC`. The bare resource must
> retain contextual `Owner` until the corporation's owner is known; normalizing it early to generic
> `Player` would lose which player receives the starting money.

---

## 11. Enumeration and automatic narrowing

Because a class table is closed once frozen, Pets can list the concrete possibilities below an
abstract type — the operation behind "which area do you want?" and behind narrowing a choice
automatically when only one exists.

These operations come in two flavours. Asked of a **type** (`someType.allConcreteSubtypes()`) they
range over the whole master universe. Asked of a **class table**
(`table.allConcreteSubtypes(someType)`) they range over the concrete narrowings inhabited in that
table. For a game view, the premise-selected declaration closure determines that domain (T12-3).
The rules below describe the shape of the operation; section 12 says which universe answers.

**T11-1. Enumerating concrete narrowings.** `allConcreteSubtypes()` pairs every concrete subclass of
the root class with every admissible concrete binding of every dependency:

```text
Tile              →  GreeneryTile<Tharsis_2_2>, GreeneryTile<Tharsis_2_3>, OceanTile<Tharsis_1_1>
Tile<Tharsis_2_2> →  GreeneryTile<Tharsis_2_2>
```

A concrete type enumerates only itself. A type with no concrete narrowing enumerates nothing.

> **Non-normative example — using a standard project.** `UseStandardProjectAction` starts from the
> abstract `StandardProject` and must offer Sell Patents, Power Plant, Asteroid, Aquifer, Greenery,
> and City as concrete choices. Pairing subclass choice with dependency choice is what generalizes
> that menu operation to tiles and other dependent components.

**T11-2. Refinements during enumeration.** Every `NOT` clause filters the candidates, since it can be
decided structurally. `HAS` clauses are **not** applied: enumeration is world-free, and the caller
tests the survivors. So `LandArea(HAS Neighbor)` enumerates every concrete land area.

> **Non-normative example — Red Ships.** Its value depends on tiles in Mars areas adjacent to an
> ocean. Enumeration must first retain every structurally possible area, then let the live board
> evaluate `HAS Neighbor<OceanTile>`; baking today's board into the universe would make the type
> table change after every placement.

**T11-3. Same-class enumeration.** `concreteSubtypesSameClass()` holds the root class fixed and varies
only the dependencies. An abstract root class yields nothing.

> **Non-normative example — solo reserves.** `SoloStandardResourceReserve<Class<StandardResource>>`
> must fan out to one reserve of the same root class for each resource kind. Whole-hierarchy
> enumeration could instead wander into unrelated subclasses of a broader system component.

**T11-4. Automatic narrowing.** `singleConcreteSubtype(info)` returns the one concrete narrowing when
there is exactly one, and `null` otherwise. It is stricter than "one candidate matched the
refinement": the root class and *every* dependency must each have a single concrete choice, and the
refinement must then accept the result. Any remaining choice, anywhere, blocks it — deliberately, so
the engine never silently makes a decision a player should have made.

Within those limits it is thorough: it sees through a `NOT`; it finds a subclass that already fixes
the dependency (`Tile<LandArea>` narrows to `GreeneryTile` when that is the only land tile, even
where an `OceanTile` class also exists — a concrete class incompatible with the requested type is
not one of the choices); and it reports nothing at all when the requested narrowing is incompatible
with every concrete class. Both flavours answer alike about the same universe. A
`HAS` refinement can decide between candidates only where enumeration happens anyway, as with a
refined class literal (T8-10).

This is an under-approximation, and knowingly so: a type is narrowed automatically when the
*universe* leaves one candidate, not when the current board does. An ocean placement asking for
`WaterArea(HAS MAX 0 Tile)` is never settled automatically, however few empty water areas remain,
because the map declares many water areas and only a world could say which are empty. Asking a world
about every candidate on every resolution would cost more than it saves, and the answer would change
under the player's feet; a player choosing between two spaces they could already see is not a
decision the engine should be making anyway.

> **Non-normative example — Aquifer.** On a map with two water areas, an ocean gain stays an
> ordinary choice even when one is occupied. What automatic narrowing does settle is the case where
> the universe itself is down to one candidate — a map with a single water area, or a resource kind
> with only one concrete class.

**T11-5. Caller-supplied targets.** `ClassTable.allConcreteSubtypes(type, dependencyTargets)`
enumerates using a caller's smaller set of possible dependency targets instead of the full structural
domain. A custom metric that already knows which components exist uses this: a dependency requires its
target to exist, so no omitted specialization could contribute.

> **Non-normative example — Estate Dealer.** Its award metric follows the custom `Neighbor` relation
> while counting owned tiles adjacent to oceans. Supplying the dependency targets that actually
> exist avoids expanding every hypothetical tile–area pairing in the Catalog, none of which could
> contribute to the live count.

---

## 12. Inhabitance

One Catalog is compiled once into a master table. A game view combines that table with a small
premise table and the declaration closure selected by the premise. That closure determines which
concrete narrowings the universe contains. **Inhabitance** is the resulting semantic property of a
Type, independent of the live World.

**T12-1. Known names retain their nominal meaning.** A known Class name resolves to its Class, with
the same hierarchy, dependencies, and declaration whether or not its base Type is inhabited in this
universe. An unknown Class name is an expression error.

Imagine a Catalog that knows `Jackalope : Rabbit`. In a game whose declaration closure omits
`Jackalope`, the name still resolves and remains a subclass of `Rabbit`, while the `Jackalope` Type
is uninhabited: the game knows that no Jackalope Component can exist.

> **Non-normative example — Venus Next.** In a base-only game, `VenusStep` is known but uninhabited;
> `VenusStap` is unknown. Treating both as “not present” would hide typos, while treating both as
> inhabited would offer a Venus track the game did not select.

**T12-2. A view reuses its master.** It shares the very same master Class and Type objects. A
premise Class may extend master or premise Classes and is visible to hierarchy queries through its
own view. A master can be combined with one of its premise values; values from sibling premise
tables are incompatible. Expressions using only master names normally resolve through the master;
`NOT` remains combined-universe-relative because premise subclasses can create structural overlap.

> **Non-normative example — shared master identity.** Base-game and Venus views resolve
> `Class<VenusStep>` using the same master Class objects. The Type is inhabited only in the view
> whose declaration closure contains a concrete Venus step.

**T12-3. Enumeration is view-relative.** A game view answers enumeration from its
premise-selected declaration closure:

- `allSubclasses` and `directSubclasses` list only Classes included by the premise closure;
- `allConcreteSubtypes` and `concreteSubtypesSameClass` list its inhabited concrete Types;
- an uninhabited Type enumerates nothing, and `Class<X>` enumerates nothing when `X` has no
  inhabited concrete specialization;
- `singleConcreteSubtype` can therefore succeed in a view where the master is undecided — if this
  game has only one milestone, `Milestone` narrows to it automatically.

> **Non-normative example — claiming a milestone.** The master Catalog knows milestones from every
> supported map, but `ClaimMilestoneAction` must enumerate only the concrete Milestone Types
> inhabited on the selected map. Otherwise a Tharsis game could offer Hellas's Polar Explorer as a
> legal claim.

**T12-4. Inhabitance is the existence of a concrete narrowing.** A Type is inhabited in a universe
exactly when it has at least one realizable concrete narrowing there. It is uninhabited when its
concrete domain is empty. The following all have empty concrete domains:

- a Type rooted in a concrete master Class outside the premise closure;
- an abstract Type with no inhabited concrete narrowing;
- a Type whose dependency has no inhabited concrete binding;
- a structural difference such as `Player1(NOT Player1)` that excludes every candidate; and
- a class literal whose represented Class has no concrete subclass with an inhabited base Type.

Inhabitance is the greatest self-consistent concrete domain satisfying these rules. Class
representatives exist structurally (section 4), so a `Class<This>` dependency (T4-9) can make its
own Class's concrete base Type inhabited; it does not require a Component of the represented Type
to exist. Ordinary dependency-bound cycles remain illegal (T3-11).

An abstract Type may therefore be inhabited, and a structurally concrete Type may be uninhabited.
Inhabitance is fixed by the universe and structural refinements. It does not change merely because
the live World currently contains zero matching Components, no remaining capacity, or no current
answer to a `HAS` refinement. A Type from another Catalog is not known in this universe (T1-2).

An uninhabited Type counts zero, contributes no concrete choices or class representative, and
cannot appear as a Component or fire a trigger. Changes to it follow L12-14. Its nominal information
remains available for resolution, subtyping, intersection, `NOT`, and diagnostics.

`Die` is distinct from this structural case. It is a concrete, final Type whose invariant gives it
zero component capacity in every World. The engine can reject a mandatory change to either kind,
but their reasons remain different: an uninhabited Type has no concrete narrowing in the universe,
while `Die` is concrete and has no legal occurrence.

> **Non-normative example — game-end barriers.** Core rules know the generic
> `GpIncomplete<Class<GlobalParameter>>`, but the specialization for `Class<VenusStep>` must remain
> uninhabited without Venus Next. Checking only the root would create a completion barrier for a
> track that cannot advance.

**T12-5. Structural meaning stays universe-wide.** A difference (T8-4) is judged against every
master and premise Class in its universe, not merely the included premise closure. If two Classes
overlap, `Left(NOT Right)` keeps its refinement and keeps rejecting bare `Left`, even when the
overlapping Class's Type is uninhabited. Enumeration under that difference remains view-relative.

> **Non-normative example — Philares.** `Player(NOT Player1)` must keep the same structural meaning
> in two-, three-, and five-player views. Letting unoccupied seats alter the difference would make
> the card's opponent selector a different Type when player count changes.

---

## 13. Type variables

An Effect can name the shared choice that connects its trigger to its instruction:

```text
PROD[StandardResource AS SR]: SR
```

"When you gain production of a resource, gain one of *that* resource." The two occurrences are one
**type variable**: one choice, used twice.

**T13-1. A variable is a kind of type.** `Type` has exactly two forms: an ordinary `GroundType`, and a
`TypeVariable` whose resolved meaning is its `bound` — itself a ground type. Every ordinary
operation (`rootClass`, `dependencies`, `narrows`, `abstract`) works on a variable through its bound,
so code that does not care about capture can ignore the distinction. Code that does care asks for
`typeVariable`, which is absent on a ground type.

A variable has one **declaration** occurrence and any number of **usage** occurrences, in authored
order. Each occurrence is itself a `Type` view of the same variable, and remembers where it was
written.

A variable's identity is its declaration and scope — never its class name. `Player` can name several
unrelated variables in different rules.

There are three sources of a shared choice: a class header declares one (T13-2 to T13-5), an Effect
names one explicitly (T13-6), or authored syntax repeats one across other places that must agree
(T13-7 to T13-9). A trigger supplies the concrete value when it matches; `BY` is one place that
value can come from.

Three properties hold of all three sources, and most of the rules below are consequences of them:

1. **An occurrence either chooses, matches, or observes.** A change's target chooses a value; a
   trigger matches one from the event it responds to; a requirement, a gate, the expression a metric
   counts, and a refinement only look at what is there. Choosing and matching occurrences can
   introduce a variable. An observing occurrence never does — it ranges over whatever matches it —
   though it happily *uses* a variable already introduced elsewhere.
2. **Repetition does not shadow.** Where a variable is already visible, a repeated spelling is one
   more occurrence of it, never a new variable hiding it. Only an explicit binder — an `EACH` or
   `RANK` selector, or the represented class inside a refined class literal — introduces a name of
   its own over an enclosing one.

   **Known departure.** A nested inferred scope currently also records such an occurrence as an
   inner variable. See Appendix A.
3. **Inheritance passes values, not names.** A subclass does not see its superclass's header
   variables by spelling; it receives their values when a component fixes them (T13-4, T13-5).

### Class-header variables

**T13-2. Each eligible abstract header expression declares one.** Eligible means: not `This`, and
resolving to an abstract type. `ABSTRACT CLASS Holder<Box<Person>>` declares two — `Box<Person>` and
the `Person` nested inside it.

Occurrences that reach the *same dependency path* are one variable, even through different
supertypes. That is what rule T3-8 is built on:

```pets
ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>
```

The `Owner` inside `CardFront<Owner>` sits at path `Cardbound_0.Owned_0`; the `Owner` of the
`Owned<Owner>` supertype sits at `Owned_0`. One path ends with the other, so they are one variable,
and the two dependency positions are forced to agree. Two header *roots* spelled alike stay
independent, as do identical nested bounds in sibling branches: `Pair<Box<Person>, Box<Person>>`
leaves the two people free to differ.

> **Non-normative example — cardbound resources.** `CardResource<ResourceHolder<..., Owner>>` and
> `Owned<Owner>` reach the same holder owner by different dependency paths. Recognizing their shared
> suffix is what prevents a resource and its physical card from acquiring different owners.

**T13-3. Uses in the class's own body.** Text in the effects authored in a class body that matches a
header variable is a *use* of it, not a new declaration. A simple header variable may also head an
occurrence that adds arguments: with header variable `Person`, an effect writing `Box<Person>` uses
it. An effect occurrence that could equally name two independent header variables is ambiguous and is
rejected.

> **Non-normative example — production.** `Production<Class<StandardResource>>` uses
> `StandardResource` in its body: during Production Phase, a steel-production component must create
> steel. Treating the body spelling as a fresh choice could produce plants from steel production.

**T13-4. Inheritance.** A subclass does not redeclare an inherited variable, and effects inherited
from a superclass keep that superclass's scope.

> **Non-normative example — `CardBilling`.** It inherits `Billing`'s cleanup effects, including the
> resource-denomination variable, while fixing that denomination to MC. Redeclaring the variable in
> the subclass would disconnect the inherited “remove when no debt remains” test from the billing
> component's actual currency.

**T13-5. Capturing values.** `variableBindingsFrom(general, variables)` reads what a specialized
component type supplies for each variable. Both types must have the same root class. Specializing
`Holder<Box<Person>>` to `Holder<Box<Alice>>` supplies `Box<Person> = Box<Alice>` and
`Person = Alice`, and binding those into the class's effect turns `This: Box<Person>` into
`This: Box<Alice>`.

A value that did not change and is still abstract supplies nothing. A subclass that *fixes* the
dependency does supply one — `CLASS Leaf : Badge<Alice>` supplies `Alice` for `Badge`'s variable.

> **Non-normative example — solo steel reserve.** Specializing
> `SoloStandardResourceReserve<Class<Steel>>` must bind every body occurrence of
> `StandardResource` to steel: its initial stock, production, and mirrored player transfers. Merely
> narrowing the header would leave a supposedly steel reserve operating on arbitrary resources.

### Effect-local and inferred variables

**T13-6. An Effect names a shared choice explicitly.** `Type AS Name` declares a variable in the
matching part of an Effect's trigger. A bare `Name` elsewhere in the same Effect uses the
declaration's complete structural expression. Naming does not make an observing occurrence eligible
to declare: a requirement, metric or refinement may use a variable declared by a match, but cannot
declare one. The name has class-name syntax, including a single capital letter, but must not name any
Type in the Catalog. It is local to the Effect, must be unique there, and must be used at least once.
A reference cannot have arguments or a refinement.

Binding it substitutes at every occurrence at once:
`Production<Class<StandardResource AS R>>: R` bound to `Plant` becomes
`Production<Class<Plant>>: Plant`.

> **Non-normative example — Manutech.** The production increase is one choice region and the gained
> resource is another. Its explicit `SR` name says “that same resource”; without that
> link, increasing titanium production could reward heat.

Repeating an abstract expression without `AS` does not declare an Effect variable. When the
instruction leaves the Effect's lexical scope, its references have already been expanded to the
chosen structural Type.

**T13-7. Repetition across the choice regions of other constructs.** An abstract expression whose
identical spelling appears in at least two regions below declares one variable, and every occurrence
of that spelling — including further ones in the same region — uses it.

| Construct | Regions |
| --- | --- |
| Action | the cost; the result |
| `THEN` sequence | each stage |
| Transmutation (`A FROM B`) | the gained side; the removed side — but *not* the two whole roots |

The first two are settlement sites: parts of one rule that are settled separately, and across
which "the same one" is worth saying. An action's two regions are the two stages its arrow lowers to
(L9-2), and `X` is shared across exactly these same regions (L6-14).

A transmutation is not like that — its two sides are settled together, as one atomic pair — and its
row is here for a different reason, the same one that makes L6-12's compact form work: a
transmutation states what changes, so what it repeats is what it keeps. In
`Production<Class<X>> FROM Production<Class<X>>` the shared variable is `Class<X>`, not the whole
production, because the whole production is what the change is replacing.

> **Non-normative example — Market Manipulation.** `ColonyProduction FROM ColonyProduction` moves
> one step from one colony to another. If the two whole roots declared one variable, source and
> destination would be forced to the same track and the card would cancel itself; only repeated
> proper subexpressions are equality claims.

> **Non-normative design note — why spelling remains here.** Repetition is meaningful because the
> physical icon grammar commonly repeats one icon to mean “the same one.” Requiring the *same
> authored spelling* keeps that claim visible in the source: resolution and default insertion
> cannot silently make two differently written icons become one shared choice, and an author who
> means two independent choices can simply write them differently.

**T13-8. Where repetition does not introduce another variable.** Repetition is evidence of one
shared choice only where the occurrences can be settled by that choice. The cases below introduce no
additional variable; none of them hides a variable already visible, which a repeated spelling only
ever uses.

| Repetition | Why not |
| --- | --- |
| Occurrences confined to requirements | a requirement observes candidates, it does not choose one |
| Occurrences inside a metric | a count ranges over a domain rather than picking one member |
| An occurrence inside a refinement | a refinement tests a candidate chosen or matched outside it |
| A nested repeat inside a larger repeat | recognition prefers the largest repeated expression, so repeating `CardFront<Owner>` does not also infer an `Owner` variable |
| A different authored spelling | `Tile` and `Tile<Area>` resolve alike but are different names; likewise `Duo<Area, Person>` and `Duo<Person, Area>` |
| An `EACH` selector and any body text naming it | the fanout declares its own variable for its body |
| A concrete expression, or `This` | there is no open choice to bind |

The first three rows are the observing case of the section's first property: an occurrence that only
looks never introduces, but does use a variable whose choice is available in the same settlement
region or an earlier one. That is why the gate in
`(Eligible<Person>: Coin<Person>) THEN Receipt<Person>` speaks about the same person the stages
choose, rather than ranging over people of its own.

> **Non-normative examples — Sponsor and `EACH`.** Sponsor's metric must count three independently
> matching expensive cards, not capture the first `CardFront(HAS 20 cost)` and demand three copies
> of it. Conversely, `EACH Class<GlobalParameter>` owns an explicit fanout variable so its body uses
> the particular track selected for that iteration.

**T13-9. Actor specialization.** A `BY` selector constrains the Actor recorded on the triggering
event. A simple, positive, abstract Actor expression in that position is specialized to the concrete
Actor before the inner trigger is matched. It is a one-occurrence declaration unless it has an
explicit name; `AS` references then use the value supplied by the event Actor. With no reuse,
recording that declaration has no additional language meaning: the selector simply tests the Actor.

Other selectors do not become declarations merely because they follow `BY`. `BY Anyone` alone is the
unrestricted wildcard described after T6-6, and a refined selector alone is a constraint. Reusing
any Actor selector elsewhere in the Effect requires an explicit name, as in
`BY Player AS ActingPlayer`; repeating its Type spelling does not link it. `BY Anyone` remains a
wildcard unless it is explicitly named.

Where an actor variable is visible, an exclusion may use it, and the difference is tested only after
the actor is bound:

```text
Notice<Owner(NOT ActingPlayer) AS Other> BY Player AS ActingPlayer: Heat<Other> BY ActingPlayer
```

Binding `ActingPlayer` to `Player1` gives
`Notice<Owner(NOT Player1) AS Other> BY Player1: Heat<Other> BY Player1`, and the remaining
`Other` variable may then capture a particular other player. This keeps
"anyone but the actor" distinct from "the particular other player this event was about".

> **Non-normative examples — Hydrologist and Aphrodite.** Hydrologist says
> `OceanTile AS Ocean BY Player AS Placer: OceanCredit<Placer, Ocean>`. When Player 2 places an
> ocean, trigger specialization supplies `Player2` for `Placer`, so the credit belongs to
> the placer. Aphrodite says `VenusStep BY Anyone: 2 MC`; it does not name the wildcard, so it only
> removes the Actor restriction and the money retains Aphrodite's contextual owner.

> **Non-normative design note — `BY` supplies an explicitly named value.** `BY` identifies the event
> field that supplies the value; `AS` identifies where that value is reused. Specializing it before
> the inner trigger matters when the Actor is mentioned in a `NOT` there. With no reuse, whether an
> implementation records a one-occurrence variable is bookkeeping rather than language semantics.

### Binding

**T13-10. Binding replaces recorded occurrences only.** `TypeVariableScope.bind(bindings)` returns a
transformer that rewrites the occurrences it recorded and nothing else — a coincidental mention of
the same class elsewhere is untouched. Each occurrence keeps its own arguments while receiving the
captured value.

A refinement on the *declaration* is consumed by binding: it was already evaluated while the
candidate was captured, so later occurrences reuse the captured type without asking the world again.

> **Non-normative example — Cyberia Systems.** Once a building card satisfying the first-choice
> refinement is selected, later occurrences must reuse that captured card without re-evaluating the
> consumed board query. Replacing every similar-looking `CardFront` would also corrupt the distinct
> second-choice expression.

**T13-11. Scope queries.** A `TypeVariableScope` reports the variables visible in it (`variables`,
`isEmpty`), the current spelling of a variable or of one occurrence (`expressionsOf`,
`expressionOf`), and which variable a given syntax node uses or declares (`variableAt`,
`variableDeclaredAt`). `bindingsFrom(authored, general, specific)` captures values by walking the
dependency keys chosen while resolving the authored expression — so a candidate that lacks the path a
variable sits on captures nothing, rather than guessing from a coincidentally similar type.

> **Non-normative example — Law Suit.** The removal watchers record victim, resource class, and
> acting player in distinct authored dependency positions; Law Suit later consumes that exact
> signal. Path-aware capture prevents a coincidentally similar `Player` or `Owner` elsewhere in the
> rule from becoming the attacker.

---

## Appendix A: known departures

| Rule | Current implementation departure |
| --- | --- |
| T13-3 | When a class-header variable is repeated inside a nested inferred scope such as `THEN`, the implementation records both the use of the visible header variable and an inner variable with the same name. Class-header specialization currently binds the occurrence first, leaving the inner variable inert, but the duplicate remains observable through `TypeVariableScope`. |

## Appendix B: deliberately unspecified

- **Exception messages.** Rules name exception *types* where the type is part of the contract.
- **Evaluation order and caching.** Resolution memoizes, and several derived values are computed
  lazily; neither is observable except through T1-6.
