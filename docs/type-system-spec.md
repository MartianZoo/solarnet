# The Pets type system: a specification

This document defines the type system implemented in `dev.martianzoo.pets.types`. It is meant to be
readable start to finish, but it is organized so you can look one rule up and stop.

## How to read this

The specification is divided into **sections**, numbered 1 to 13. Each section states a series of
numbered **rules**. A rule is written `4-2`, meaning "section 4, rule 2", and it is the unit you
cite.

Every rule is checked by tests whose names begin with the same number, in
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

So `grep -rn "8-9" docs/type-system-spec.md test/common/dev/martianzoo/pets/types/` finds a rule and
everything that proves it. One known departure from these rules has a passing characterization in
`BugsTest.kt`; it is flagged where it belongs and listed again in the appendix.

Examples use real Terraforming Mars component names — `GreeneryTile`, `Tharsis_2_2`, `Plant`,
`Cardbound` — but the declarations shown are simplified. They illustrate a rule; they are not a
transcript of `tfm-canon`.

### Notation

| Written | Means |
| --- | --- |
| `A <: B` | every A is a B; A *narrows* B |
| `A ⊓ B` | the greatest lower bound (`glb`) of A and B: the most specific type below both |
| `A ⊔ B` | the least upper bound (`lub`) of A and B: a common supertype |
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
- A **universe** is one Catalog's complete, immutable set of classes and types.

### What this document does not cover

Three neighbours are deliberately out of scope:

- **How a game decides which classes it contains.** Section 12 defines what an *uninhabited* class
  means; the activation-closure policy that decides which classes end up uninhabited belongs to
  premise construction. Its behavior is pinned by `ActivationTest.kt` and described in
  `docs/agents/OPTIONS.md`.
- **Component-count invariants**, except for the one rule the type system leans on (3-9): a
  dependency may only target a type limited to a single copy.
- **What happens at runtime** — instructions, triggers, tasks, the task queue. See
  `docs/agents/ENGINE.md`.

---

## 1. Universes and identity

Compiling a Catalog produces one **master class table**: a complete, frozen universe of classes and
the types built from them. Nothing in this specification is meaningful except relative to one such
universe.

**1-1. One class per name, one universe per Catalog.** A Catalog compiles to exactly one class
object for each declared name. Two separately compiled tables over identical source are *different*
universes whose classes and types are not equal to each other.

**1-2. Values are universe-scoped.** Every class, type and dependency belongs to one master
universe. An operation that compares values from two universes — subtyping, `glb`, `lub`, subclass
enumeration, constraint matching — raises `IllegalArgumentException`. It does not quietly answer
"no". `ClassTable.knows(type)` is the safe question to ask first.

This matters because a false "not a subtype" would silently misroute a trigger, whereas an exception
stops the caller at the bug.

**1-3. Resolution is a function of the written expression.** `ClassTable.resolve` maps an
`Expression` to a type. The same expression always yields the identical object; different spellings
of one type yield *equal* types that need not be identical:

```text
GreeneryTile<Area>  and  GreeneryTile   →  equal types
GreeneryTile        and  GreeneryTile   →  the identical object
```

A type's own renderings (5-4, 5-5) always resolve back to it.

**1-4. `Component` is the root.** Every universe contains an abstract class `Component` with no
supertypes and no dependencies. Every other class has it as a supertype.

**1-5. `Class` is the other required class.** Its base type is `Class<Component>`. Section 4 covers
it.

**1-6. Enumeration requires a frozen table.** A class table is built by loading classes and then
freezing. Lookup (`findClass`, `resolve`) works during loading; anything that enumerates the
universe — `allClasses`, `allClassNames`, `allSubclasses`, `directSubclasses`, and therefore
`glb` between unrelated classes — requires the table to be frozen first.

**1-7. Only the exact declared name resolves.** There are no abbreviations, no case folding, no
nearest-match. An unknown name raises `ExpressionException`.

---

## 2. Classes

**2-1. A declaration introduces a class and its base type.** `CLASS Foo` declares a concrete class;
`ABSTRACT CLASS Foo` declares an abstract one. The compiled class retains the declaration it came
from, including its docstring.

Concrete and abstract mean what they usually do: components exist only for concrete classes, and
only abstract classes can be extended (2-3).

**2-2. Supertypes.** A class may name any number of abstract direct supertypes. A class that names
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

**2-3. A concrete class is final.** No class may extend a concrete one, so a concrete class's only
subclass is itself. This is what makes "narrow this to a concrete type" a terminating operation: a
player who chooses `GreeneryTile<Tharsis_2_2, Player1>` cannot then be asked to choose again.

**2-4. The subclass relation.** It is reflexive, transitive, and antisymmetric, and it is *nominal*:
`LandArea` is below `MarsArea` because it says so, not because their shapes agree. `isSubtypeOf`
answers; `isSupertypeOf` is its converse; `ensureNarrows` throws `NarrowingException` instead of
returning false.

**2-5. Cycles are rejected.** A class may not be its own supertype, directly or through a chain.

**2-6. Declaration order is irrelevant.** A supertype may be declared after its subclass.

**2-7. The hierarchy can be walked in both directions.** A class knows `allSuperclasses()` (itself
included), `allSubclasses()` and `directSubclasses()`. The downward ones need a frozen table (1-6).

**2-8. Greatest lower bound of two classes (`⊓`).** If one operand is below the other, that one is
the answer. Otherwise Pets looks for a *unique greatest common subclass*: a class below both, which
every other class below both is also below. If there is no such class — because the two are disjoint,
or because two rival classes combine them — the result is **absent** (`null`).

```pets
ABSTRACT CLASS Tile
ABSTRACT CLASS OwnedTile : Tile, Owned
CLASS GreeneryTile : OwnedTile
```

`Tile ⊓ Owned` is `OwnedTile`. Add `CLASS CommercialDistrictTile : Tile, Owned` and it becomes
absent: Pets does not manufacture an intersection class, it only recognizes one you declared.

**2-9. Least upper bound of two classes (`⊔`).** Always exists, falling back to `Component`. Pets
takes the common superclasses, discards any that are above another common superclass, and picks one
of what remains. Multiple inheritance can leave several equally minimal candidates; the choice
between them prefers the class carrying more dependencies, then the one with more superclasses, and
is otherwise arbitrary. **Do not depend on which of several minimal candidates comes back** — the
guarantee is only that the result is a common superclass, and a minimal one.

**2-10. Intersection classes.** `isIntersectionType()` asks whether a class *is* the intersection of
its own direct supertypes: whether every class below all of them is also below it. `OwnedTile` is
one, and the question is worth asking, because a component that is both `Owned` and a `Tile` but
forgot to extend `OwnedTile` would go uncounted by the Landlord award.

**2-11. Custom classes.** A class declared `: Custom` has its behavior supplied by Kotlin instead of
Pets. A declaration and an implementation must agree: a class declared `Custom` with no
implementation is rejected, and so is an implementation for a class not declared `Custom` — including
for a root class. A `Custom` class may also not *inherit* Pets behavior: no supertype of it may
declare effects, invariants, or instruction-intensity defaults. A load that fails these checks is not
cached as a success; loading again fails the same way.

**2-12. Class identity.** Within a universe, a class is identified by its name. Its `toString` is
that name.

---

## 3. Dependencies

A type argument in Pets is not a conventional generic parameter. It is a **dependency**: an edge to
one specific other component that must exist for this one to exist. `Plant<Player1>` needs
`Player1`; `GreeneryTile<Tharsis_2_2, Player1>` needs both the area and the player.

**3-1. Keys.** Every dependency a class declares gets a **key**: the declaring class's name plus the
zero-based slot, written `Occupant_0`, `Owned_0`, `Adjacency_1`. The key, not the position, is the
dependency's identity.

**3-2. Inheritance.** A subclass inherits every dependency under its original key, and may narrow
its bound by writing the supertype with arguments. Dependencies a subclass declares itself come
*after* the inherited ones.

```pets
ABSTRACT CLASS Occupant<Area>
ABSTRACT CLASS Tile : Occupant
CLASS GreeneryTile : Tile<MarsArea>, Owned<Owner>
```

`GreeneryTile` has keys `[Occupant_0, Owned_0]` and base type
`GreeneryTile<MarsArea, Owner>` — the area edge was narrowed, never copied or renamed.

**3-3. Several supertypes, one key.** When more than one supertype constrains the same key, the
bounds are intersected (`⊓`, rule 7-1). Bounds with no common narrowing are an error.

**3-4. An argument intersects the bound; it never replaces it.** Writing a wider argument therefore
changes nothing, and writing one outside the bound is an error.

This is what makes `Anyone` work. `Anyone` is an ordinary class at the top of the ownership
hierarchy, so `Anyone` intersected with a narrower declared bound is that narrower bound:

```text
ProjectCard : Owned<Player>    →  ProjectCard<Anyone>  is  ProjectCard<Player>
Plant       : Owned<Owner>     →  Plant<Anyone>        is  Plant<Owner>
ProjectCard<SoloOpponent>                              →  error: not a Player
```

**3-5. Argument matching is greedy, left to right.** Each written argument takes the first
not-yet-taken dependency whose bound it can intersect. An argument that matches nothing is an error.

A useful consequence: when the bounds are disjoint, argument order does not matter.
`GreeneryTile<Tharsis_2_2, Player1>` and `GreeneryTile<Player1, Tharsis_2_2>` are the same type.
Order *is* meaningful when two bounds overlap, as in `Adjacency<Area, Area>`, and then
`Adjacency<Tharsis_2_2>` fills the first slot and leaves the second open.

**3-6. Which key an argument filled is recoverable.** `Class.matchDependencyKeys(arguments)` replays
the match and reports the key each authored argument took, for callers that need to remember what was
supplied rather than only the resulting type.

**3-7. `This` in a supertype argument names the inheriting class.** It is rebound at each level of
the hierarchy, in place, leaving every other argument alone:

```pets
ABSTRACT CLASS Link<Class<Component>>
ABSTRACT CLASS SelfBound : Link<Class<This>>
CLASS SelfLeaf : SelfBound
```

gives `SelfLeaf<Class<SelfLeaf>>`, while writing the class name literally
(`Link<Class<SelfBound>>`) would have given `SelfLeaf<Class<SelfBound>>`.

**3-8. One header variable in two positions forces them to agree.** When the same type variable
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

**3-9. A dependency may only target a type limited to one copy.** An edge names its target by exact
type alone, so a type admitting two identical components could not say which one it meant. Every
concrete type a dependency bound admits must therefore carry an applicable `MAX 1` or `=1` invariant.
This is checked when a game's component-limit table is built, and it is the only place invariants
enter this specification.

**3-10. Dependency sets.** A type's dependencies form a keyed set: `get(key)`, `getIfPresent(key)`,
`keys` in declaration order. Equality is key-wise and ignores order. `flatten()` walks nested paths
(`Cardbound_0.Owned_0`), and `at(path)` reads one. `narrowedDependencies` reports only what a type
narrowed below its own class's base type.

**3-11. Cycles are rejected.** Two classes may refer to each other freely, but a genuine cycle of
dependency *bounds* — `CLASS Foo<Bar>` with `CLASS Bar<Foo>`, or `CLASS Foo<Foo>` — has no finite
answer and raises `PetException` when the bounds are computed. A one-way chain is fine.

---

## 4. Class literals

A dependency asserts that a component exists. `Class<X>` instead names a class *as data*:
`Production<Class<Steel>>` is a steel production, and needs no steel cube to exist.

One `Class<Foo>` component exists for each active concrete class, so `Class<X>` can also be the
target of an ordinary dependency without violating 3-9.

**4-1. Form.** `Class<X>` takes exactly one bare class name. The `Class` class's own base type is
`Class<Component>`, so bare `Class` means "some class".

**4-2. Concreteness depends only on the class named.** Not on that class's dependencies:

```text
CityTile          is abstract  — its area has not been chosen
Class<CityTile>   is concrete  — `CityTile` is one specific class
Class<Metal>      is abstract  — `Metal` is not
```

**4-3. Class literals are covariant.** `Class<Steel> <: Class<Metal> <: Class<StandardResource>`,
and this carries into dependency positions:
`Production<Class<Steel>> <: Production<Class<Metal>>`.

**4-4. `representedClass`** returns the named class, and is absent for every type that is not a class
literal.

**4-5. Bounds follow the class hierarchy.** `Class<Metal> ⊓ Class<Steel>` is `Class<Steel>`;
`Class<Steel> ⊔ Class<Titanium>` is `Class<Metal>`; the `glb` of literals for disjoint classes is
absent.

**4-6. The operand is one bare, existing class name.** All of these are errors:

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

**4-7. The slot inside a literal is not a component dependency.** It holds a class, so it does not
appear among a type's `typeDependencies`. A class that *declares* a dependency bounded by a class
literal — `CLASS Production<Class<StandardResource>>` — has an ordinary dependency whose bound
happens to be a class literal.

**4-8. Enumeration.** The concrete narrowings of `Class<Metal>` are `Class<Steel>` and
`Class<Titanium>` — one per concrete subclass. A literal for a class with no concrete subclass
enumerates nothing.

**4-9. `Class<This>`** follows rule 3-7: it names the inheriting class. This is how a card resource
knows which card class can hold it:

```pets
ABSTRACT CLASS ResourceCard<Class<CardResource>> : CardFront
ABSTRACT CLASS CardResource : Cardbound<ResourceCard<Class<This>>> { CLASS Animal, Microbe }
CLASS Fish : ResourceCard<Class<Animal>>
```

`Animal`'s base type becomes `Animal<Owner, ResourceCard<Owner, Class<Animal>>>`, so
`Animal<Player1, Fish>` resolves and `Animal<Ants>` — Ants holds microbes — does not.

---

## 5. Types

**5-1. What a type is.** A type is a root class, one bound for each of that class's dependency keys,
and optionally a refinement (section 8). Two types are equal when those three agree — however each
was written.

**5-2. A bare class name means that class's base type**, which supplies every dependency's declared
bound. An explicit empty argument list, `GreeneryTile<>`, means the same type. (The two spellings
differ elsewhere in Pets: in an instruction, writing `<>` says "I accept this use's defaults on
purpose". That rule belongs to instructions, not to types; see `docs/agents/TYPES.md`.)

**5-3. Abstractness.** A type is abstract if its root class is abstract, **or** any dependency bound
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

**5-4. Full form.** `expressionFull` writes every dependency, in key order. For the
`GreeneryTile` of rule 3-2 that is `GreeneryTile<MarsArea, Owner>`; had `Owned` been inherited
first, the same type would be written `GreeneryTile<Owner, MarsArea>`.

**5-5. Minimal form.** `expression` — also what `toString` shows — writes the smallest set of
arguments that resolves back to the same type. Concretely:

- an argument equal to what the class already declares is omitted;
- arguments are written in dependency-key order, whatever order they were supplied in;
- among equally small candidate sets, the earliest dependencies are preferred;
- an argument another one already determines may be omitted (this is 3-8 at work: for
  `Animal<Player1, Pets<Player1>>` the owner follows from the card, so the minimal form is
  `Animal<Pets<Player1>>`);
- a refinement is always written.

**5-6. Both forms round-trip.** Resolving either form of a type yields that same type.

**5-7. Building a type directly.** `Class.withAllDependencies(deps)` needs a bound for every one of
that class's own keys; a missing one is an error. A bound for a key the class does *not* have is
ignored, which is exactly what projects a type onto each of its supertypes — a greenery tile's owner
is not part of what an `Occupant` is. `Class.specialize(arguments)` applies arguments to the base
type, using the matching rule of 3-5.

**5-8. `Type` has two forms.** A `GroundType` is an ordinary resolved type: it is its own
`groundType`, and its `typeVariable` is absent. The other form is a type variable; see section 13.
Code that only needs classes, dependencies or narrowing can treat both uniformly.

---

## 6. Subtyping

Narrowing is the central relation: "every component of type A is also of type B".

**6-1. Two spellings of one test.** `narrows(that, info)` returns a boolean;
`ensureNarrows(that, info)` throws `NarrowingException` with a reason. `isSubtypeOf` /
`isSupertypeOf` are the world-free spellings; they pass a sentinel world that raises
`IllegalStateException` if the comparison actually turns out to need one (8-9).

**6-2. The structural rule.** A narrows B when

1. A's root class is a subclass of B's root class, and
2. for every dependency key B constrains, A's bound for that key narrows B's, and
3. B's refinement, if any, accepts A (section 8).

A key B does not have is not checked, which is what lets `GreeneryTile<Tharsis_2_2, Player1>` narrow
`Tile<Tharsis_2_2>` regardless of owner.

**6-3. Dependencies are covariant.** `Occupant<Tharsis_2_2> <: Occupant<LandArea> <: Occupant<Area>`,
and narrowing the class and a dependency compose freely.

Covariance is the right choice here precisely because a dependency identifies an exact target. A
broad dependency describes a *set of possible* concrete types to query or narrow, not one component
that would accept any member of the set. `ResourceValue<Class<Metal>>` counts the separate steel and
titanium value components; it is not one component that accepts either.

**6-4. Shape of the relation.** Over structural types it is reflexive, transitive, and
antisymmetric — two types that narrow each other are the same type. With a world in hand and
refinements involved, antisymmetry is the casualty: in a world where every land area has a neighbour,
`LandArea` and `LandArea(HAS Neighbor)` narrow each other while remaining distinct types. Narrowing
with a world is a preorder, not an order.

**6-5. Cross-universe comparisons are rejected**, per 1-2.

**6-6. Constrained narrowing.** `ClassTable.matchesConstraint(candidate, constraint, domain, info)`
asks whether a candidate satisfies a constraint expression *read inside a domain*. The constraint is
first intersected with the domain, then the candidate is tested against the result. This is how a
trigger's `BY` selector is applied: with domain `Actor`, the constraint `Player` accepts `Player1`
and rejects `Admin`, and `Actor(NOT Player1)` accepts both `Player2` and `Admin`. A constraint that
cannot meet the domain at all simply answers false.

---

## 7. Bounds

**7-1. Greatest lower bound (`⊓`, `glb`).** The most general type below both operands, or **absent**
when there is none. It is computed componentwise: the root classes by 2-8, each shared dependency key
by `⊓` again, and refinements by 8-10.

```text
Tile<Tharsis_2_2>  ⊓  Owned<Player1>       =  OwnedTile<Tharsis_2_2, Player1>
GreeneryTile<Tharsis_2_2>  ⊓  GreeneryTile<Player1>  =  GreeneryTile<Tharsis_2_2, Player1>
Tile<Tharsis_2_2>  ⊓  Tile<Tharsis_2_3>    =  absent
GreeneryTile  ⊓  OceanTile                 =  absent
```

Absent means "Pets cannot write down a single type for this", not "no component could be both".
Where a result does exist it narrows both operands, and no other type below both is outside it.

**7-2. Least upper bound (`⊔`, `lub`).** Always exists, falling back to `Component`. The root classes
join by 2-9, and only the dependency keys *both* operands carry survive — which is automatic, since
both are below the joined class.

```text
GreeneryTile<Tharsis_2_2, Player1>  ⊔  OceanTile<Tharsis_1_1>  =  Tile<MarsArea>
```

Because 2-9 is a heuristic among incomparable candidates, `⊔` promises a common supertype, and a
minimal one, but not a canonical one.

**7-3. Both operations are idempotent, and commutative on structural types.** Two exceptions to
commutativity follow from choices this specification leaves open elsewhere, and neither changes what
the result *means*:

- when several minimal common superclasses tie, 2-9 breaks the tie by the order each operand happens
  to list its supertypes, so `A ⊔ B` and `B ⊔ A` can name different classes;
- a joined `HAS` requirement (8-10) is written in operand order, so `A ⊓ B` and `B ⊓ A` can carry
  the same conjuncts in the other order, and are then not `==`.

**7-4. Cross-universe bounds are rejected**, per 1-2.

---

## 8. Refinements

A refinement turns a type into a filtered version of itself: `LandArea(HAS MAX 0 Tile)` is "an empty
land area". There are two kinds, and they behave very differently.

- `HAS R` (and its forgiving variant `HAS? R`) asks a **world** whether requirement `R` holds of the
  candidate. This is the only place in the type system that consults game state.
- `NOT X` performs a **structural** exclusion, decided entirely from the class hierarchy.

**8-1. A refined type is abstract and lies below its unrefined domain.** Narrowing the domain while
keeping the same predicate narrows the refined type: `Tharsis_2_2(HAS Neighbor) <: LandArea(HAS
Neighbor)`.

**8-2. `HAS` asks the world about the candidate.** Testing whether candidate `c` narrows
`D(HAS R)` substitutes `c` into `R` and asks the world the resulting question. Testing
`Tharsis_2_2` against `LandArea(HAS Neighbor<CityTile>)` asks
`Neighbor<CityTile<Area, Owner>, Tharsis_2_2>`.

**8-3. How the candidate is substituted.** Every expression inside `R` receives the candidate in the
first of its dependencies that can accept it (3-5). A bare class property receives it as its
receiver, so `CardFront(HAS MAX 9 cost)` tested against `Ants` asks `MAX 9 Ants.cost`.

If no expression in `R` can accept the candidate, the refinement fails without asking the world at
all. This is not an error; it is the answer. `Component(HAS StartToken)` can only ever match a
player, because that is what a `StartToken` depends on — and testing a rock against it is simply
false.

A written argument *constrains* the candidate in the slot it occupies rather than reserving that
slot away from it. `ActionCard(HAS ActionUsedMarker<ActionCard(NOT Viron)>)` means "an action card,
other than Viron, that has been used" — the candidate merges into the written `ActionCard(NOT Viron)`
argument.

> **A known gap.** When two dependencies of one expression accept the same type, the candidate takes
> the first, which may be the one an argument was written into, leaving the intended slot open. For
> `Area(HAS Adjacency<Tharsis_2_2>)` with candidate `Tharsis_2_2`, the world is asked
> `Adjacency<Tharsis_2_2, Area>` rather than `Adjacency<Tharsis_2_2, Tharsis_2_2>`. Characterized in
> `BugsTest`. Reserving written keys is *not* the fix: real cards, including Viron and Mons
> Insurance, depend on the merging behavior above.

**8-4. `HAS?` is forgiving.** It is satisfied when its requirement holds, *or* when no candidate
anywhere satisfies the strict version. The requirement actually asked is
`R OR MAX 0 D(HAS R)`. Greenery placement uses this: next to one of your own tiles if any such area
exists, otherwise anywhere.

The escape clause covers the whole requirement, so anything that should not enable the fallback —
occupancy, for instance — belongs inside it.

**8-5. `NOT` is a structural difference.** `D(NOT X)` is the part of `D` that cannot overlap `X`. A
candidate satisfies it only when its **entire** structural domain avoids `X`:

```text
Player2 <: Owner(NOT Player1)     yes
Player1 <: Owner(NOT Player1)     no
Player  <: Owner(NOT Player1)     no — abstract `Player` still admits Player1
```

The exclusion need not narrow the domain; subtraction goes through their structural intersection.
`Actor(NOT Owner)` excludes players, who inherit both, and retains `Admin`. Overlap is detected even
where the two have no unique intersection class (2-8): if two rival classes each extend both
`Occupant` and `Owned`, `Occupant(NOT Owned)` still excludes them. The test never consults a world,
and works the same inside a dependency: `Marker<Player(NOT Player1)>`.

**8-6. The excluded operand must be refinement-free, recursively.** Neither
`Owner(NOT Player(HAS Marker))` nor a nested `NOT` is accepted. This keeps the difference decidable
without a world and prevents negating a world query.

**8-7. A difference that cannot bite is dropped.** If the domain and the exclusion cannot overlap in
the first place, the refinement disappears: `Player1(NOT Player2)` *is* `Player1`.

**8-8. A difference that excludes everything is still a type.** `Player1(NOT Player1)` keeps its
refinement, is abstract, and enumerates nothing. It stays representable because an excluded type
variable may be specialized later, making the difference non-empty again.

**8-9. Refinements in narrowing.** In order:

- A refined type always narrows its own unrefined domain.
- If the two refinements are *identical*, the narrowing is accepted with no world consulted.
- If the narrower type's refinement is a **strict** `HAS` whose conjuncts include all of the
  target's, it already guarantees the target — whether the target is strict or forgiving, since
  forgiving only adds an escape clause:
  `LandArea(HAS Neighbor, Occupant) <: LandArea(HAS Neighbor) <: LandArea(HAS? Neighbor)`.
- A **forgiving** refinement guarantees nothing but itself. Conjoining more to it does not narrow it,
  because its escape clause is relative to its own whole requirement: where some area has a
  neighbour but none is occupied, *every* area satisfies `HAS? Neighbor, Occupant` through the
  escape, while only some satisfy `HAS? Neighbor`.
- Otherwise a refined type never satisfies an unrelated `HAS` target, and this is decided without a
  world. Different predicates do not imply one another.
- An *unrefined* type tested against a `HAS` target needs a world; asked with none it raises
  `IllegalStateException` rather than guessing.
- A `NOT` target always uses the structural test of 8-5, whatever refinement the candidate carries.
- These comparisons read the two predicates *as written*, which is only meaningful when both types
  substitute the same candidate into them. Two class literals for different classes do not (8-12),
  so neither shortcut applies to them: `Class<BuildingTag>(HAS Tag)` does not narrow
  `Class<Tag>(HAS Tag)`, because for the target the predicate asks about the candidate's own class.

**8-10. `glb` of refinements.** A refinement the other operand lacks is kept, and two identical
refinements collapse to one. Otherwise:

- if **either** operand is strict, the result is the strict conjunction of both requirements. By
  8-9 that really is below both, including below a forgiving operand — so
  `LandArea(HAS Neighbor) ⊓ LandArea(HAS? Neighbor)` is `LandArea(HAS Neighbor)`.
- if **both** are forgiving and they differ, there is no single predicate that means "both", since
  each escape clause is relative to its own whole requirement. `glb` is absent.
- a `HAS` against a `NOT`, or two different `NOT`s, likewise have no single writable predicate.
  `glb` is absent.

**8-11. `lub` of refinements.** A refinement survives only when both operands carry exactly the same
one; otherwise the result is the unrefined common domain.

**8-12. Refined class literals.** A refinement on `Class<X>` tests the class the candidate names:
references to `X` inside the requirement are rewritten to the candidate's class. Testing
`Class<BuildingTag>` against `Class<Tag>(HAS Tag<Player1>)` asks `BuildingTag<Player1>` — counting
tag classes, not tag components.

**8-13. Refinements inside dependencies** behave like any other, and survive rendering and
re-resolution.

---

## 9. Class properties

A **class property** records an immutable fact about a class — a card's cost, a milestone's
requirement, an award's metric. It is not state on a component: every component of one concrete type
necessarily agrees about it.

**9-1. A property is declared either as a bound or as a value.** The bounds are `Number`, `Metric`,
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

**9-2. A subclass may narrow, never override.** `Metric` may narrow to `Number`, `Number` to a
literal, `Requirement` to a requirement. Overriding a value already fixed by a supertype is an error,
and so is a "narrowing" that is not one.

**9-3. A concrete class must fix every property it inherits** — except an unfilled `Requirement?`
(9-1). A concrete class with an unfixed property is rejected at load.

**9-4. Inheriting one property along several paths.** The same fact arriving twice is one fact. When
one path narrowed further than another, the narrower fact wins — provided the two lie on one chain of
narrowings. Two properties with the same name from unrelated origins are an error, and so are two
divergent narrowings of one property.

**9-5. Reading a property.** A type exposes the values of its root class:
`getNumberPropertyValue`, `getMetricPropertyValue`, `getRequirementPropertyValue` (which returns
`null` for an absent optional). Reading a property that is still a bound, or one that does not exist,
is a programming error and throws.

**9-6. Properties take no part in type identity or subtyping.** They are facts about the class, not
dependencies. Two cards with different costs are different types because they are different classes,
not because of the costs.

---

## 10. Defaults

A `DEFAULT` clause records context that a physical game leaves implicit — that a tile goes on a land
area, that a resource belongs to the current player. It changes how an authored expression *resolves*;
it never changes which types exist.

**10-1. Three separate sets.** Defaults are gathered independently for all uses (`DEFAULT Foo<...>`),
for gains (`DEFAULT +Foo<...>`) and for removals (`DEFAULT -Foo<...>`). A class's `defaultType` is its
base type with the all-uses defaults applied; the gain and removal sets are consumed by instructions,
not by resolution.

```pets
ABSTRACT CLASS Tile<Area> : Owned<Owner> {
  DEFAULT +Tile<LandArea>
}
```

The gain default says a tile placed without saying where goes on land. `GreeneryTile<Tharsis_1_1>`
still resolves — a water area — because a default is not a bound.

**10-2. Intensities.** A gain or removal default may also carry an intensity (`!` mandatory, `.`
as-much-as-possible, `?` optional). `Component` supplies `!` for both, so every class inherits
something. Gain and removal intensities are inherited independently, and supertypes that disagree
about one are an error.

**10-3. A `DEFAULT` clause must name the class that declares it.**

**10-4. Inheriting dependency defaults.** For one dependency key and one use kind, only the nearest
declaring superclasses survive: anything a nearer superclass overrode is discarded. What survives is
intersected (`⊓`), and survivors with no common narrowing are an error. Each inherited default is also
intersected with the inheriting class's own bound, so a default can only ever get narrower. A default
that merely restates the declared bound records nothing at all.

**10-5. `Owner` in a default stays as written.** This is the one deliberate exception to 10-4: a
literal `Owner` written in a default is *not* intersected with the class's bound, so it can later be
replaced by whichever player supplies the context.

The visible consequence is that a class's `defaultType` may sit outside its own base type:

```pets
ABSTRACT CLASS Card : Owned<Player> { DEFAULT Card<Owner> }
```

gives base type `Card<Player>` but default type `Card<Owner>`, which is not below it. That is
intended: the default is a template awaiting a context, not a type any component will have.

---

## 11. Enumeration and automatic narrowing

Because a class table is closed once frozen, Pets can list the concrete possibilities below an
abstract type — the operation behind "which area do you want?" and behind narrowing a choice
automatically when only one exists.

These operations come in two flavours. Asked of a **type** (`someType.allConcreteSubtypes()`) they
range over the whole master universe. Asked of a **class table**
(`table.allConcreteSubtypes(someType)`) they range only over what that table holds, which for a game
view means only its active classes (12-3). The rules below describe the shape of the operation;
section 12 says which universe answers.

**11-1. Enumerating concrete narrowings.** `allConcreteSubtypes()` pairs every concrete subclass of
the root class with every admissible concrete binding of every dependency:

```text
Tile              →  GreeneryTile<Tharsis_2_2>, GreeneryTile<Tharsis_2_3>, OceanTile<Tharsis_1_1>
Tile<Tharsis_2_2> →  GreeneryTile<Tharsis_2_2>
```

A concrete type enumerates only itself. A type with no concrete narrowing enumerates nothing.

**11-2. Refinements during enumeration.** A `NOT` filters the candidates, since it can be decided
structurally. A `HAS` is **not** applied: enumeration is world-free, and the caller tests the
survivors. So `LandArea(HAS Neighbor)` enumerates every concrete land area.

**11-3. Same-class enumeration.** `concreteSubtypesSameClass()` holds the root class fixed and varies
only the dependencies. An abstract root class yields nothing.

**11-4. Automatic narrowing.** `singleConcreteSubtype(info)` returns the one concrete narrowing when
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
refined class literal (8-12).

**11-5. Caller-supplied targets.** `ClassTable.allConcreteSubtypes(type, dependencyTargets)`
enumerates using a caller's smaller set of possible dependency targets instead of the full structural
domain. A custom metric that already knows which components exist uses this: a dependency requires its
target to exist, so no omitted specialization could contribute.

---

## 12. Inhabitance

One Catalog is compiled once into a master universe. A game then takes a **view** of it. The view
does not create, rename or reshape anything; it records which names this game can hold components of.

**12-1. Three states for a name.**

| State | Meaning |
| --- | --- |
| **active** | full behavior in this game |
| **uninhabited** | known to the Catalog, but with an empty domain here |
| **unknown** | an error in every context |

An uninhabited class keeps its name, its place in the hierarchy, and its dependencies. It resolves;
it is still a subclass of what it extends; `Class<It>` still names it. Imagine a catalog that knows
`Jackalope : Rabbit`. In a game where jackalopes are uninhabited, `Jackalope` is not a spelling
error and is still a rabbit — but the game knows something stronger than "we have not seen one":
there cannot be one.

**12-2. A view reuses the master universe.** It shares the very same class and type objects.
Resolution, subtyping, `glb` and `lub` therefore give the same answers in a view as in the master.
A type has one meaning, not one per game.

**12-3. What the view does change.** Everything that *enumerates*:

- `allSubclasses` and `directSubclasses` list only active classes;
- `allConcreteSubtypes` and `concreteSubtypesSameClass` list only active types;
- an uninhabited type enumerates nothing, and neither does its class literal;
- `singleConcreteSubtype` can therefore succeed in a view where the master is undecided — if this
  game has only one milestone, `Milestone` narrows to it automatically.

**12-4. Active types.** A type is active when its root class is active and every dependency bound is.
A type from another Catalog is not even *known*, let alone active (1-2).

**12-5. Structural meaning stays catalog-wide.** A difference (8-5) is judged in the master universe.
If two classes overlap in the Catalog, `Left(NOT Right)` keeps its refinement and keeps rejecting bare
`Left`, even in a game where the overlapping class is uninhabited. Enumeration under that difference is
still view-relative, so the game sees only what it can hold. This keeps a written type from meaning
different things in different games.

---

## 13. Type variables

Repeated icons in a game rule usually mean one shared choice, not two independent ones:

```text
PROD[StandardResource]: StandardResource
```

"When you gain production of a resource, gain one of *that* resource." The two occurrences are one
**type variable**: one choice, used twice.

**13-1. A variable is a kind of type.** `Type` has exactly two forms: an ordinary `GroundType`, and a
`TypeVariable` whose structural meaning is its `bound` — itself a ground type. Every ordinary
operation (`rootClass`, `dependencies`, `narrows`, `abstract`) works on a variable through its bound,
so code that does not care about capture can ignore the distinction. Code that does care asks for
`typeVariable`, which is absent on a ground type.

A variable has one **declaration** occurrence and any number of **usage** occurrences, in authored
order. Each occurrence is itself a `Type` view of the same variable, and remembers where it was
written.

A variable's identity is its declaration and scope — never its class name. `Player` can name several
unrelated variables in different rules.

There are two ways a variable comes into being: a class header declares one (13-2 to 13-5), or one is
inferred from repetition in authored syntax (13-6 to 13-9).

### Class-header variables

**13-2. Each eligible abstract header expression declares one.** Eligible means: not `This`, and
resolving to an abstract type. `ABSTRACT CLASS Holder<Box<Person>>` declares two — `Box<Person>` and
the `Person` nested inside it.

Occurrences that reach the *same dependency path* are one variable, even through different
supertypes. That is what rule 3-8 is built on:

```pets
ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>
```

The `Owner` inside `CardFront<Owner>` sits at path `Cardbound_0.Owned_0`; the `Owner` of the
`Owned<Owner>` supertype sits at `Owned_0`. One path ends with the other, so they are one variable,
and the two dependency positions are forced to agree. Two header *roots* spelled alike stay independent, as do identical nested bounds in
sibling branches: `Pair<Box<Person>, Box<Person>>` leaves the two people free to differ.

**13-3. Uses in the class's own body.** Text in the effects authored in a class body that matches a
header variable is a *use* of it, not a new declaration. A simple header variable may also head an
occurrence that adds arguments: with header variable `Person`, an effect writing `Box<Person>` uses
it. An effect occurrence that could equally name two independent header variables is ambiguous and is
rejected.

**13-4. Inheritance.** A subclass does not redeclare an inherited variable, and effects inherited
from a superclass keep that superclass's scope.

**13-5. Capturing values.** `variableBindingsFrom(general, variables)` reads what a specialized
component type supplies for each variable. Both types must have the same root class. Specializing
`Holder<Box<Person>>` to `Holder<Box<Alice>>` supplies `Box<Person> = Box<Alice>` and
`Person = Alice`, and binding those into the class's effect turns `This: Box<Person>` into
`This: Box<Alice>`.

A value that did not change and is still abstract supplies nothing. A subclass that *fixes* the
dependency does supply one — `CLASS Leaf : Badge<Alice>` supplies `Alice` for `Badge`'s variable.

### Inferred variables

**13-6. Repetition across choice regions.** An authored construct is divided into **regions** (13-7).
An abstract expression whose identical spelling appears in at least two regions declares one variable,
and every occurrence of that spelling — including further ones in the same region — uses it.

Binding it substitutes at every occurrence at once:
`Production<Class<StandardResource>>: StandardResource` bound to `Plant` becomes
`Production<Class<Plant>>: Plant`.

**13-7. The regions of each construct.**

| Construct | Regions |
| --- | --- |
| Effect | the trigger; the instruction |
| Action | the cost; the result |
| `THEN` sequence | each stage |
| Transmutation (`A FROM B`) | the gained side; the removed side — but *not* the two whole roots |

The transmutation exception matters: the source and destination of `A FROM B` are meant to differ, so
only repeated *proper subexpressions* assert equality. In
`Production<Class<X>> FROM Production<Class<X>>` the shared variable is `Class<X>`, not the whole
production.

**13-8. What does not declare a variable.** These prevent a *declaration*; they never hide a use of a
variable declared in an enclosing scope.

| Repetition | Why not |
| --- | --- |
| Occurrences confined to requirements | a requirement observes candidates, it does not choose one |
| The expression a metric counts directly | a count ranges over a domain rather than picking one member |
| A nested repeat inside a larger repeat | recognition prefers the largest repeated expression, so repeating `CardFront<Owner>` does not also infer an `Owner` variable |
| A different authored spelling | `Tile` and `Tile<Area>` resolve alike but are different names; likewise `Duo<Area, Person>` and `Duo<Person, Area>` |
| An `EACH` selector, and body text naming it | the fanout declares its own variable for its body |
| A first-stage `THEN` dependency choice | it outranks a matching class variable, and an earlier gate occurrence belongs to that same choice |
| A concrete expression, or `This` | there is no open choice to bind |

**13-9. Actor selectors.** A simple, positive, abstract Actor expression after `BY` declares a
variable *even with no repetition* — that is how a triggered rule learns who acted. `BY Anyone` is an
unrestricted filter, and a refined selector such as `BY Player(NOT Owner)` is a filter too; neither
binds.

Where an actor variable is visible, an exclusion may use it, and the difference is tested only after
the actor is bound:

```text
Notice<Owner(NOT Player)> BY Player: Heat<Owner(NOT Player)>
```

Binding `Player` to `Player1` gives
`Notice<Owner(NOT Player1)> BY Player1: Heat<Owner(NOT Player1)>`, and the remaining
`Owner(NOT Player1)` is itself a variable that may then capture a particular other player. This keeps
"anyone but the actor" distinct from "the particular other player this event was about".

### Binding

**13-10. Binding replaces recorded occurrences only.** `TypeVariableScope.bind(bindings)` returns a
transformer that rewrites the occurrences it recorded and nothing else — a coincidental mention of
the same class elsewhere is untouched. Each occurrence keeps its own arguments while receiving the
captured value.

A refinement on the *declaration* is consumed by binding: it was already evaluated while the
candidate was captured, so later occurrences reuse the captured type without asking the world again.

**13-11. Scope queries.** A `TypeVariableScope` reports the variables visible in it (`variables`,
`isEmpty`), the current spelling of a variable or of one occurrence (`expressionsOf`,
`expressionOf`), and which variable a given syntax node uses or declares (`variableAt`,
`variableDeclaredAt`). `bindingsFrom(authored, general, specific)` captures values by walking the
dependency keys chosen while resolving the authored expression — so a candidate that lacks the path a
variable sits on captures nothing, rather than guessing from a coincidentally similar type.

---

## Appendix A: known departures

One behavior contradicts the rules above. It has a passing characterization in
`test/common/dev/martianzoo/pets/types/BugsTest.kt`; the rule states the intent.

| Rule | Departure |
| --- | --- |
| 8-3 | When two dependencies of one expression accept the same type, a substituted refinement candidate takes the first, which may already hold a written argument, leaving the intended slot open. Simply reserving written keys is not the fix — real cards depend on the current merging behavior. |

## Appendix B: deliberately unspecified

- **Which minimal common superclass `lub` returns** when several are incomparable (2-9). The result
  is a common superclass and a minimal one; among ties, do not depend on the choice, and note that
  swapping the operands can change it (7-3).
- **The order of conjuncts in a joined `HAS` requirement** (8-10). The predicate means the same
  thing either way, but the two spellings are different types by `==` (7-3).
- **Exception messages.** Rules name exception *types* where the type is part of the contract.
- **Evaluation order and caching.** Resolution memoizes, and several derived values are computed
  lazily; neither is observable except through 1-6.
