# Pets type system

> **Read when:** changing a specific Pets type-system concept. Start with Quick model, then read only
> its numbered section; read Known divergences only when diagnosing or deliberately fixing one.
>
> **Skip when:** changing live component/task execution without changing static Type meaning; use
> [ENGINE.md](ENGINE.md).
>
> **Status:** current implementation-facing model with explicit defects in section 12. The human
> tutorial is [type-system.md](../type-system.md).

## Source map by concept

| Concept | Source entry point |
| --- | --- |
| Class identity and nominal hierarchy | [`Class.kt`](../../src/common/dev/martianzoo/pets/types/Class.kt), search `public class Class` |
| Ground Types, dependency lookup, complements | [`GroundType.kt`](../../src/common/dev/martianzoo/pets/types/GroundType.kt) and [`Type.kt`](../../src/common/dev/martianzoo/pets/types/Type.kt) |
| Dependency declarations and keys | [`Dependency.kt`](../../src/common/dev/martianzoo/pets/types/Dependency.kt) and [`DependencySet.kt`](../../src/common/dev/martianzoo/pets/types/DependencySet.kt) |
| Class loading, inheritance, defaults, and inhabitation | [`ClassLoader.kt`](../../src/common/dev/martianzoo/pets/types/ClassLoader.kt) |
| Closed-world lookup and bounds | [`ClassTable.kt`](../../src/common/dev/martianzoo/pets/types/ClassTable.kt) |
| Authored Type variables and scopes | [`TypeVariable.kt`](../../src/common/dev/martianzoo/pets/types/TypeVariable.kt), [`TypeVariableScope.kt`](../../src/common/dev/martianzoo/pets/types/TypeVariableScope.kt), and [`inferTypeVariables.kt`](../../src/common/dev/martianzoo/pets/types/inferTypeVariables.kt) |
| Class-scoped variables | [`Class.kt`](../../src/common/dev/martianzoo/pets/types/Class.kt), search `headerVariableBindings`, and [`Transformers.kt`](../../src/common/dev/martianzoo/engine/Transformers.kt), search `bindEffectVariables` |
| Trigger and Actor specialization | [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt), search `positive abstract Actor selector` and `Subscription` |
| Foundational declaration vocabulary | [`SystemDeclarations.kt`](../../src/common/dev/martianzoo/pets/SystemDeclarations.kt), search for the named Class |

## Quick model

- A Game World contains a multiset of concrete component Types.
- Components have no fields or instance identity. Type plus multiplicity is all state.
- Classes provide nominal subtyping. Concrete Classes cannot have subclasses.
- Type arguments are dependency edges to other unique components, not conventional generic parameters.
- `Class<X>` names a Class without depending on an X component.
- Class properties record immutable facts about a Class, not state on component occurrences.
- Refinements filter candidates by querying the current World.
- Unresolved `Expression` and resolved `GroundType` are both `Specification`s. Their roots, dependencies,
  and refinements narrow compositionally, and state-aware checks use `TypeInfo`.
- Complements exclude a dependency subdomain.
- Each World has one frozen closed Class Table, allowing concrete enumeration and automatic
  narrowing.
- Eligible authored abstract Expressions declare Type variables whose uses are limited to defined
  scopes.
- Catalog-known inactive Classes are uninhabited: nominally resolvable, with provably empty domains.

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
`CityTile<RemoteArea {}>`. Card-definition construction lowers these to declarations with
stable owner-derived names such as `Inventrix_Mandate` and `PhobosSpaceHaven_RemoteArea` before building the
Class Table. They have exactly the existing Class and component semantics; there is no runtime
anonymous identity.

The body follows the complete expression. For example,
`SpecialTile<LandArea(HAS Neighbor<OwnedTile>)> {}` becomes the use-site expression
`MiningArea_SpecialTile<LandArea(HAS Neighbor<OwnedTile>)>` and declares its superclass as
`SpecialTile<LandArea>`. Arguments therefore specialize both the occurrence and the generated
Class's superclass. Refinements constrain only the occurrence and are removed recursively from the
declared superclass because refinement types cannot be supertypes.

The local body may contain invariants, properties, effects, and actions. It may not contain
`DEFAULT` clauses or nested Class declarations. The generated Class inherits applicable defaults
from its supertypes.

Use this for a Class local to one definition, especially mandates, temporary effects, special tiles,
and remote areas. A shared Class, a Custom implementation, or a component with several semantic
roles should remain explicit. Multiple local Classes with the same natural suffix must be declared
explicitly rather than distinguished by an ordinal or hash.

Another expression that needs the exact derived Class may use its assigned canonical name. Writing
the superclass without a local body still means the whole abstract family; it does not implicitly
resolve to the local subtype. Existing implicit Type-variable rules continue to interpret repeated
abstract dependencies inside the derived Type as uses of one declaration.

This syntax is available only in card-definition expressions. Ordinary Class declarations reject
it. Manually submitted instructions are still parsed and validated, then rejected with
`NoNewClassDeclarationsException` because the live game's Class Table is frozen.

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
`Owned_0` and adds the card dependency. Both `Player` occurrences use one class-scoped Type variable,
described in section 10, so the card and owner must agree.

Bare `This` denotes the fully bound context Type, while `Class<This>` denotes only its root Class.
Current replacement also accepts `This` with explicit arguments: it substitutes the context root
Class but keeps the authored arguments. Thus `This<Player>` in a `Birds<Owner>` context becomes
`Birds<Player>`, not `Birds<Owner, Player>`.

The only production use with `This` as the root and explicit arguments is the inherited invariant
`HAS MAX 1 This<Player>` on `CardFront`; it limits the card Class across owners rather than limiting
each owner-specialized Type separately. No production Effect or dependency needs general
`This<...>` substitution. The smallest durable model is therefore an invariant-only self-Type
pattern, not a general expression feature. Do not extend specialized `This` to other syntax unless a
second semantic use establishes one coherent rule.

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

`Class.matchDependencyKeys()` exposes the key matched by each authored argument when a consumer
must retain which dependencies were supplied rather than only the fully resolved Type.

A full form states every bound. A minimal form uses the smallest dependency-ordered subset of direct
arguments that greedily re-resolves to the same Type, including Type-variable equalities that let
one argument determine another. Equal-size forms prefer earlier dependencies. Rendering uses
minimal form. A Complement's unwritten domain is the known exception to round-tripping; see section
7 and divergence 12.5.

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
The resolved `Type.representedClass` exposes that Class directly.

## 5. Defaults

Defaults preserve omitted physical-game context:

```pets
DEFAULT Owned<Owner>
DEFAULT +OceanTile<WaterArea>
DEFAULT -Required.
DEFAULT Tag<CardFront>:
```

They supply omitted dependency bounds and, for gains/removals, a Quantifier. They change how an
authored Expression resolves, not which Types exist.

All-use, gain, removal, and trigger defaults are gathered separately. For one dependency and use
kind, only nearest declaring supertypes survive. Incomparable surviving bounds need one
most-general common narrowing; Quantifiers must agree.

Literal `Owner` in a default stays unresolved until a concrete owned context can bind it. In an
ownerless context it remains the abstract Class.

Inside a refinement, an implicit default is deferred when its dependency is a direct use of a
Class-header Type variable; candidate substitution can then bind it through that occurrence.
Writing `<>` still explicitly accepts the default.

A gain, removal, or trigger that would receive dependency bounds from its use-specific default
cannot leave its argument list implicit. It must supply at least one argument or write an empty list
such as `GreeneryTile<>` or `ScienceTag<>` to explicitly accept those bounds. The gain and removal
halves of `A FROM B` are checked independently. This rule does not apply to all-use dependency
defaults or to Quantifier defaults; `<>` has the same Type meaning as a bare expression after
defaults are inserted.

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
satisfies the strict refinement. Greenery placement uses this to fall back to any empty land area
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

Every Type belongs to one immutable Catalog-wide master universe. Values from different master
universes are not comparable. Master compilation resolves the hierarchy and compiles nominal
subtype masks once.

Each game owns an explicit filtered Class Table view over that master. The view reuses the master
Classes and Types and records the inhabited names selected by the premise's activation closure. A
name has one of three states:

- **active:** full behavior in this game;
- **uninhabited:** nominally known to the Catalog, with an empty domain here; or
- **unknown:** an error in every context.

A game's view is closed. No later declaration may change its inhabited set. Structural operations
such as subtyping, `glb`, and `lub` use the master universe; active subclass and concrete-Type
enumeration receive the game view explicitly. See [CLASS_TABLES.md](CLASS_TABLES.md).

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
produce incomparable minimal candidates, so the implementation uses a heuristic instead of
promising a mathematical least upper bound.

Dependency `lub` retains a Complement only when the other bound already satisfies it or both
exclude the same Type; otherwise it falls back to the unexcluded domain.

## 10. Authored Type variables

Repeated unspecified icons in one game rule usually mean one shared choice:

```text
PROD[StandardResource]: StandardResource
```

Here the `StandardResource` in the trigger declares a Type variable and the second occurrence uses
it. If the triggering component is `Production<Class<Plant>>`, the effect produces `Plant`; the two
occurrences are not independent searches below `StandardResource`.

### Type model

`Type` is the common interpretation consumed by type-system APIs. It has two forms:

| Form | Identity | Meaning |
| --- | --- | --- |
| `GroundType` | Root Class, dependencies, and optional refinement | An ordinary resolved structural Type. |
| `TypeVariable` | Its declaration and lexical scope | One captured Type constrained by its Ground-Type `bound`. |

A variable is therefore not merely an annotation beside its bound. It is a distinct Type with the
same ordinary operations. Code that only needs narrowing, dependencies, or Class information uses
the `Type` API. Code concerned with capture or substitution can inspect whether that Type is a
`TypeVariable`, then inspect its declaration and uses.

`GroundType` is preferred to `ProperType`: it says that the value contains no open capture without
suggesting that a variable is somehow an improper Type. A variable's structural constraint is its
`bound`; the shorter name is sufficient because every variable has exactly one such constraint.

### Declaration, use, and binding

An authored abstract `Expression` has one of these roles in a particular scope:

| Role | Meaning |
| --- | --- |
| Ordinary bound | Denotes all Types below the resolved expression and makes an independent choice if a consumer requires one. |
| Variable declaration | Introduces one implicit Type variable whose `bound` is the authored expression interpreted in the enclosing scope. The declaration occurrence is also a use. |
| Variable use | Denotes the value of one visible declaration, further constrained by its containing expression. It does not make another choice. |
| Contextual placeholder | Receives a value from game context under a separate rule, as `This` and usually `Owner` do. |

`TypeVariable.Declaration` and `TypeVariable.Usage` are syntax occurrences whose `type` is the same
`TypeVariable`. A `PetElement` exposes the declarations and uses visible in its own
`TypeVariableScope`. The scope retains occurrence identity through preprocessing. Ground-only
consumers may explicitly inspect a variable's `bound`, but must not thereby discard its capture
identity.

A variable's identity is its declaration and scope, not its Class Name. `Player` can therefore name
several unrelated variables in separate rules. Only a declaration changes the meaning of matching
text: an abstract expression with no declaration in scope remains an ordinary bound.

Binding chooses a Ground Type that narrows the variable's bound. Every occurrence then denotes that
same captured value; binding is not textual Class-name replacement. Each containing expression is
resolved with the captured value in its recorded dependency position, and any rejected occurrence
or inconsistent value rejects the proposed narrowing. A declaration need not be the occurrence from
which a consumer first discovers the value; declaration is a static syntax role, while binding is a
later specialization operation.

For example, specializing the `CardFront` variable of an `AiCentral` component captures
`AiCentral<Owner>`, not merely the replacement token `AiCentral`. The captured Ground Type carries
the dependencies supplied by both the occurrence and the concrete Class.

The intended rule for a refined declaration is to evaluate its Requirement when a candidate is
captured, then reuse the captured Ground Type without evaluating the Requirement again. The current
implementation does not yet guarantee this timing; see section 12.

Variables are recognized from authored syntax before defaults, marked-syntax lowering, owner
substitution, and task splitting. These phases must preserve declaration identity and use paths even
when the original declaring occurrence is transformed away.

### Declaration and scope table

| Construct | What declares the variable | Scope and uses | What supplies its value | Representative form |
| --- | --- | --- | --- | --- |
| Class dependency | Each separately declared abstract dependency root declares one Class-scoped variable. Eligible abstract subexpressions along its nested dependency paths declare projected variables supplied by those paths. | The Class header, its Effects, inherited copies of those Effects, and subtype enumeration. | Specializing or enumerating the component Type. | `CLASS Trade<ColonyTile> ... { This: FlownTradeFleet<ColonyTile> ... }` |
| Repeated Class-header projection | An abstract header occurrence at one stable dependency key; a matching occurrence at the same key is a use, even through different supertypes. | The complete header and the Class-scoped effect scope above. | Intersection of the dependency bounds, then component-Type specialization. | `CLASS Cardbound<CardFront<Player>> : Owned<Player>` |
| Triggered Effect | Each maximal abstract expression in a choice-producing trigger position is a potential declaration. | That one trigger, including its `BY` and `IF` clauses, and its one instruction tree. | The concrete changed Type that matched the subscription. | `BioTag<CardFront>: Plant OR CardResource<CardFront>` |
| Positive abstract Actor selector | A simple positive abstract Actor expression after `BY`, such as `Player`. This is a binder even without repetition. | The qualified trigger and the fired instruction. Uses under operators, such as `!Player`, receive the Actor value before the operator is applied. | The concrete Actor recorded on the triggering event. | `-OwnedActorTrigger<!Player> BY Player: Steel<Player>` |
| Action | Each maximal abstract expression in a choice-producing cost or result position is a potential declaration. | That one Action; lowering preserves it across the resulting sequence. | Narrowing the cost or result, normally the cost first. | `PROD[StandardResource] -> 4 StandardResource` |
| `THEN` | Each maximal abstract expression in a stage is a potential declaration. | All stages of that one sequence, including its continuation as it is enqueued. | Narrowing any occurrence; selecting an earlier stage carries its value into later stages. | `Selecting THEN ProjectCard<Selecting> THEN BuySelectedCards` |
| Atomic transmutation | Each maximal abstract proper subexpression in a gain or removal role is a potential declaration. | That one gain/removal pair. The complete destination and source roots are excluded. | Narrowing either role; both roles must agree. | `CityTile<LandArea> FROM GreeneryTile<LandArea>` |

Class dependencies are still dependency edges to components, not conventional generic parameters.
Their resolved targets merely provide durable values for the Class-scoped variables. Each
comma-separated dependency root is independent even when two roots have identical text. Within a
root, a dependency path follows stable keys through nesting, so `Neighbor_0.Tile_0` and
`Neighbor_1` remain distinct. A key combines the Class that first declared it with its zero-based
slot, such as `Tile_0` or `Owned_0`; subtypes retain it.

For example, a concrete `Cardbound<CardFront<Player1>>` supplies both the card value and its projected
`Player1` owner value. The second `Player` in `Owned<Player>` uses that projection because both
occurrences address the same inherited owner dependency. The same value then specializes a
`Token<Player>` occurrence in a Class Effect. In contrast, independently declared roots do not
become one variable merely because their bounds have the same spelling. A body occurrence that
could name two such declarations with different values is ambiguous and must be rejected; divergence
12.4 describes the current silent fallback.

### Authored expression identity

Every eligible abstract occurrence in the table is semantically a potential declaration. If no
later use exists, retaining a runtime variable object has no observable effect and is optional. This
is only an implementation economy: repetition does not create the declaration. When uses do exist,
the earliest potential declaration in authored order owns them.

A local use normally requires exactly the same authored expression. Matching is structural equality of
the parsed AST, which naturally ignores whitespace and parser-erased grouping but performs no
resolution, default insertion, or dependency-order canonicalization. Omission, argument order,
refinement, and Complement syntax remain meaningful authored differences:

```text
Tile != Tile<Area>
OceanTile != OceanTile<MarsArea>
Owner != Anyone
Player != !Player
Player != Player<>
```

The whole authored expression is the surface name of an inferred local variable. Resolving two different
expressions to the same Ground Type does not make them uses of one variable. Recognition chooses
maximal expressions, so repeating `Card<Owner>` declares one card variable rather than also
inferring an independent variable from its nested `Owner` text.

There are two lexical extensions. First, text matching a visible Class variable is a use of that
variable rather than a fresh declaration. This applies throughout the Class body. A simple Class
variable may also occupy the root of an occurrence with arguments: `CardFront<Owner>` uses the
Class variable `CardFront` while constraining its owner dependency. Class-header occurrences are
identified by stable dependency paths, so inherited projections can be uses even when their whole
containing expressions differ.

A rule needing a distinct local capture must use a distinct authored expression. For example,
`StartToken<Player>` uses `FirstPlayerOcean<Actor> BY Actor: OceanTile<> BY Actor` so the concrete
performing Actor is captured independently of the StartToken's Class variable. If a future rule
needs a fresh variable with the same structural bound, explicit empty arguments can distinguish it:
Mons Insurance and Law Suit write `Player<>` rather than their visible Class variable `Player`.
Pets will need explicit declaration syntax if that distinction stops being sufficient.

Second, if `Player` is visible, `!Player` contains a derived use of that variable. Binding first
specializes `Player`, then applies Complement, so narrowing the positive variable widens the
complemented domain. The whole `!Player` expression may simultaneously declare a dependent variable
under the ordinary declaration rules. In
`-OwnedActorTrigger<!Player> BY Player: Heat<!Player>`, the Actor capture `T` is bound first; the
event capture `U` is then selected under `!T`; and the result uses `U`. This retains the distinction
between “anyone except this Actor” and the particular non-Actor value observed in the event.

### Non-declaring repetition

| Repetition site | Why it does not declare a shared variable |
| --- | --- |
| Comma siblings in one instruction group | They are independent instructions, not successive choice regions. |
| Separate arms of one `OR` | Choosing an arm must not choose Types for another arm. |
| Occurrences only inside Requirements | A Requirement observes candidates; it does not produce the choice it tests. |
| The root expression directly counted by a Metric | A count ranges over the root domain rather than choosing one root Type. Nested choice-producing expressions may still participate. |
| Complete source and destination roots of a transmutation | The two roots may intentionally differ, as in `ColonyProduction FROM ColonyProduction`; only repeated proper subexpressions assert equality. |
| Sibling argument branches | Each branch has its own dependency path. An enclosing repeated maximal expression may still declare a variable. |
| Separate dependency roots in one Class header | Each root declares its own component dependency. Equality must come from a shared nested dependency projection. |
| A Complement with no visible declaration for its positive operand | It remains an ordinary difference bound and cannot declare that missing positive variable. |
| A concrete expression or `This` | There is no open choice to bind. `This` is contextual. |

These rows prevent declaration; they do not hide uses of a variable declared in an enclosing scope.
For example, a trigger declaration may have uses inside an `OR` arm or an `IF` Requirement, and a
Class-header declaration may have uses in several comma-separated Effects.

### Contextual and other binders

- `This` means the effect-bearing exact Type. `Class<This>` means its root Class without
  dependencies. Self triggers `This:` and `-This:` match only changed copies of that exact Type.
- `Owner` means the exact context owner when one exists. Otherwise it remains abstract and may be
  eligible for normal variable declaration and choice semantics.
- `BY Anyone` is an unrestricted Actor filter, not a declaration. `BY !Owner` and other complemented
  selectors are filters, not binders. A positive simple abstract Actor subtype such as `BY Player`
  uses the explicit binder rule in the table.
- Trigger `X` and repeated `X` in `THEN` bind a scalar event count, not a Type variable.
- A refined `Class<Tag>` binds the represented candidate Class while testing its Requirement, as
  described in section 6. That represented-Class substitution is not an authored Type variable.

### Lifetime and specialization

Class-scoped variables survive inheritance and enumeration. Component specialization substitutes
only their recorded uses in Effects; an unrelated occurrence of the same Class remains an ordinary
bound. Effect-local variables remain open while the effect is installed, then a matching event
specializes their trigger, condition, Actor selector, and instruction together. This is trigger
specialization, not global replacement of every occurrence of the same abstract Class.

Action and `THEN` variables survive lowering and queuing. An open variable prevents the relevant
stages from splitting into independent tasks until an earlier choice supplies its value. Within one
atomic transmutation, `Foo<Same, Here, To FROM From>` is compact syntax for
`Foo<Same, Here, To> FROM Foo<Same, Here, From>`; each unchanged argument occupies both roles and
therefore uses one atomic variable.

The proposed [`EACH`](EACHPLAYER.md) fanout would make its selector a declaration whose scope is its
body. Each enumerated concrete selector Type would substitute through the recorded use paths. It
would not bind contextual `Owner` or `This` and would reject a body with no use of the selector. This
construct is not implemented.

## 11. Uninhabited Classes and Types

The Catalog's master table establishes one nominal universe. A game projection preserves every
master Class identity in one of two states: active or uninhabited. Unknown names remain errors. An
uninhabited Class retains its name, declared hierarchy, and Dependency shape so resolution and
nominal subtyping remain meaningful, but it contributes no live behavior or inhabitants.

A Type is uninhabited when its root or a dependency bound is uninhabited. A refinement mentioning
an uninhabited Class has an unsatisfiable Requirement instead; the refined Type itself need not be
uninhabited. A Complement excluding an uninhabited Type remains active over its active domain.
Here *uninhabited* is the permanent classification produced by the game projection, not a claim
that every Type with no candidates in one current World receives that classification.

Uninhabited Classes and Types:

- have no Components, Effects, defaults, Invariants, or enumerated concrete subtypes;
- count zero, as do Class literals representing uninhabited Classes;
- make optional and AMAP changes zero no-ops;
- make mandatory changes dead;
- never auto-narrow or fire triggers; and
- do not appear in listings or generated choices.

Thus an uninhabited `Jackalope : Rabbit` remains resolvable and nominally below `Rabbit`, while both
`Jackalope` and `Class<Jackalope>` count zero and enumeration below `Rabbit` or `Class<Rabbit>` omits
it. A declaration guarded by a Trigger over an uninhabited Type is unreachable rather than
malformed. A mandatory change that actually reaches an uninhabited Type is dead.

An active Class cannot have an uninhabited direct supertype or dependency bound.

### Current activation policy

Loading an active declaration activates structural supertypes, dependency and default Types,
explicit ownership roots, Custom implementation dependencies, and destinations of reachable gains
and transmutations. A positive Class invariant activates the inhabitants it explicitly requires;
observational Requirements and Trigger roots do not. Modules explicitly own protocol
Classes issued by workflows or gameplay APIs. A Trigger with an uninhabited argument or false gate
remains dormant. The loader rechecks every active declaration as the closure grows, so activating a
Trigger domain can make its constructive body reachable later.

Reachability currently proves exact facts from uninhabited Count domains through `AND` and `OR`
Requirements. Thus Vitor can remain active in solo while its `Class<Award>`-gated Mandate and the
entire Award domain remain uninhabited. Anything the analysis cannot prove unreachable remains
conservatively reachable. Known declarations outside the closure become uninhabited when the
projection freezes.

An ambient Class owned by an unavailable Bundle cannot be activated by a hard reference. After
closure, premise construction rejects selected root Classes whose `requirement` entry condition is
exactly false, and rejects selected structured content whose reachable mandatory removal targets an
uninhabited Type. See
[OPTIONS.md](OPTIONS.md#settled-projection-policy-direction).

## 12. Known divergences

Do not document these as intended semantics or fix them incidentally. The
[Type-variable audit](TYPE_VARIABLES.md) coordinates the related work.

### 12.1 Complement narrowing accepts wider abstract candidates

The current test accepts a candidate that narrows the domain without narrowing the excluded Type.
Thus abstract `SpaceTag` counts as narrowing `SpaceTag<!Player1>` even though it still admits
Player 1. Concrete candidates behave as intended.

### 12.2 Complement domains do not round-trip

Written and full forms show the exclusion but omit a separately narrowed domain. Printing and
resolving can therefore widen a Complement produced by `glb` or Type-variable narrowing.

### 12.3 Refined captures can be evaluated more than once

Binding records a Ground Type and realizes it only at the declared occurrence positions. However,
a repeated refined spelling can still leave the same Requirement on more than one realized
occurrence, so a changing World may evaluate it at different times. Capturing must consume the
declaration's refinement once and make later uses refer only to that result.
