# Pets type system

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing a specific Pets type-system concept. Start with Quick model, then read only
> its numbered section.
>
> **Skip when:** changing live component/task execution without changing static Type meaning; use
> [ENGINE.md](ENGINE.md).
>
> **Status:** current implementation-facing model. The human tutorial is
> [type-system.md](../type-system.md).

## Source map by concept

| Concept | Source entry point |
| --- | --- |
| Class identity and nominal hierarchy | [`Class.kt`](../../src/common/dev/martianzoo/pets/types/Class.kt), search `public class Class` |
| Ground Types and refinements | [`GroundType.kt`](../../src/common/dev/martianzoo/pets/types/GroundType.kt) and [`Type.kt`](../../src/common/dev/martianzoo/pets/types/Type.kt) |
| Dependency declarations and keys | [`Dependency.kt`](../../src/common/dev/martianzoo/pets/types/Dependency.kt) and [`DependencySet.kt`](../../src/common/dev/martianzoo/pets/types/DependencySet.kt) |
| Class loading, inheritance, defaults, and inhabitation | [`ClassLoader.kt`](../../src/common/dev/martianzoo/pets/types/ClassLoader.kt) |
| Closed-world lookup and bounds | [`ClassTable.kt`](../../src/common/dev/martianzoo/pets/types/ClassTable.kt) |
| Authored Type variables and scopes | [`TypeVariable.kt`](../../src/common/dev/martianzoo/pets/types/TypeVariable.kt), [`TypeVariableScope.kt`](../../src/common/dev/martianzoo/pets/types/TypeVariableScope.kt), and [`inferTypeVariables.kt`](../../src/common/dev/martianzoo/pets/types/inferTypeVariables.kt) |
| Class-scoped variables | [`Class.kt`](../../src/common/dev/martianzoo/pets/types/Class.kt), search `headerVariableBindings`, and [`Transformers.kt`](../../src/common/dev/martianzoo/engine/Transformers.kt), search `bindEffectVariables` |
| Trigger and Actor specialization | [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt), search `positive abstract Actor selector` and `Subscription` |
| Type-variable behavior tests | [`TypeVariableTest.kt`](../../test/common/dev/martianzoo/pets/types/TypeVariableTest.kt), [`DependencyVariableTest.kt`](../../test/common/dev/martianzoo/engine/DependencyVariableTest.kt), and [`TransformersTest.kt`](../../test/common/dev/martianzoo/engine/TransformersTest.kt) |
| Foundational declaration vocabulary | [`SystemDeclarations.kt`](../../src/common/dev/martianzoo/pets/SystemDeclarations.kt), search for the named Class |

## Quick model

- A Game World contains a multiset of concrete component Types.
- Components have no fields or instance identity. Type plus multiplicity is all state.
- Classes provide nominal subtyping. Concrete Classes cannot have subclasses.
- Type arguments are dependency edges to other unique components, not conventional generic parameters.
- `Class<X>` names a Class without depending on an X component.
- Class properties record immutable facts about a Class, not state on component occurrences.
- Refinements filter candidates through either a World requirement or a structural difference.
- Unresolved `Expression` and resolved `GroundType` are both `Specification`s. Their roots, dependencies,
  and refinements narrow compositionally, and state-aware checks use `TypeInfo`.
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
name explicitly. For example, a card instruction can gain `RequiredAction { -> 3 ProjectCard }`, or use
`CityTile<RemoteArea {}>`. Card-definition construction lowers these to declarations with
stable owner-derived names such as `Inventrix_RequiredAction` and
`PhobosSpaceHaven_RemoteArea` before building the Class Table. They have exactly the existing Class
and component semantics; there is no runtime anonymous identity.

The body follows the complete expression. For example,
`SpecialTile<LandArea(HAS Neighbor<OwnedTile>)> {}` becomes the use-site expression
`MiningArea_SpecialTile<LandArea(HAS Neighbor<OwnedTile>)>` and declares its superclass as
`SpecialTile<LandArea>`. Arguments therefore specialize both the occurrence and the generated
Class's superclass. Refinements constrain only the occurrence and are removed recursively from the
declared superclass because refinement types cannot be supertypes.

The local body may contain invariants, properties, effects, and actions. It may not contain
`DEFAULT` clauses or nested Class declarations. The generated Class inherits applicable defaults
from its supertypes.

Use this for a Class local to one definition, especially required actions, temporary effects,
special tiles, and remote areas. A shared Class, a Custom implementation, or a component with
several semantic roles should remain explicit. Multiple local Classes with the same natural suffix
must be declared explicitly rather than distinguished by an ordinal or hash.

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

The production invariants `HAS MAX 1 This<Player>` on `CardFront` and `Milestone` limit one concrete
card or milestone Class across owners rather than limiting each owner-specialized Type separately.
No production Effect or dependency needs general `This<...>` substitution. The smallest durable
model is therefore an invariant-only self-Class pattern, not a general expression feature. Do not
extend specialized `This` to other syntax unless another semantic use establishes one coherent
rule.

### Dependency targets must be unique

An edge identifies its target only by exact Type, so every concrete Type admitted by a dependency
bound must have an applicable `MAX 1` or `=1` counting invariant. The engine validates this when
building a game. The Type system itself does not yet encode multiplicity.

### Argument matching and forms

Written arguments match remaining dependencies greedily from left to right, using the first bound
with a non-empty intersection. Unambiguous order resolves alike:

```text
GreeneryTile<Player1, Tharsis_2_3>
GreeneryTile<Tharsis_2_3, Player1>
```

Order is meaningful when dependency bounds overlap, as in `Adjacency<Tile, Tile>`. An
unmatched extra argument is an error.

`Class.matchDependencyKeys()` exposes the key matched by each authored argument when a consumer
must retain which dependencies were supplied rather than only the fully resolved Type.

A full form states every bound. A minimal form uses the smallest dependency-ordered subset of direct
arguments that greedily re-resolves to the same Type, including Type-variable equalities that let
one argument determine another. Equal-size forms prefer earlier dependencies. Rendering uses
minimal form. Difference refinements round-trip both their domain and exclusion.

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
DEFAULT +OceanTile<WaterArea(HAS MAX 0 Tile)>
DEFAULT -Required.
```

They supply omitted dependency bounds and, for gains/removals, a Quantifier. They change how an
authored Expression resolves, not which Types exist.

All-use, gain, and removal defaults are gathered separately. For one dependency and use kind, only
nearest declaring supertypes survive. Incomparable surviving bounds need one most-general common
narrowing; Quantifiers must agree.

Literal `Owner` in a default stays unresolved until a concrete owned context can bind it. In an
ownerless context it remains the abstract Class.

`Owner` is not intrinsically a contextual placeholder. As with another abstract expression, its
role follows from its authored occurrence and visible scope. An `Owner` projected from a Class
header such as `Owned<Owner>` is that Class variable; an independently authored eligible `Owner`
can be a local variable. An `Owner` inserted only by a default has no authored declaration of its
own. It retains the context expected by that default without creating a new shared choice.

Default insertion elaborates an authored Expression; it does not author another Type-variable
declaration. Existing variables retain their recorded occurrence identity as their expressions gain
default arguments. Separate inserted expressions do not become one choice just because their
text, resolved Type, declaring default, or in-memory object is shared. Contextual `Owner` is closed
by the ownership and triggering-Actor rules, not by inventing a Type variable during expansion.
Authored-variable recognition must not be rerun on the expanded syntax: that would turn elaboration
results into declarations.

Inside a refinement, an implicit default is deferred when its dependency is a direct use of a
Class-header Type variable; candidate substitution can then bind it through that occurrence.
Writing `<>` still explicitly accepts the default.

A bare dependent expression in a `HAS` refinement likewise reserves its first dependency position
that can accept the refined domain. Candidate substitution binds that position before defaults are
considered, so `Player(HAS StartToken)` tests `StartToken<p>` for each candidate `p` even though
`StartToken` inherits the contextual `Owned<Owner>` default. The explicit forms remain available:
`StartToken<Owner>` requests the contextual owner, and `StartToken<>` explicitly accepts the
default.

A gain or removal that would receive dependency bounds from its use-specific default cannot leave
its argument list implicit. It must supply at least one argument or write an empty list such as
`GreeneryTile<>` to explicitly accept those bounds. The gain and removal halves of `A FROM B` are
checked independently. This rule does not apply to all-use dependency defaults or to Quantifier
defaults. An explicit empty list is invalid when the dependency-default set for that use is empty;
it cannot serve only to give an expression a different authored spelling.

## 5a. Class properties

Class properties record immutable facts about a Class rather than state on component occurrences.
They narrow through inheritance, can be read without a component instance, and explicitly evaluate
stored Metric or Requirement syntax inside class effects. The complete current model, settled
semantic rulings, and cardinality/default/group directions are in
[PROPERTIES.md](PROPERTIES.md).

## 6. Refinements

Refinements are predicates over candidates. `HAS` tests a World requirement, while `NOT` performs a
state-independent structural exclusion. Both are abstract subtypes of their unrefined domain.

A candidate for `CardFront(HAS 20 cost)` first narrows the base Type, then the reader substitutes
that candidate into dependency positions in the Requirement and tests the current World.

Examples:

```text
CardFront(HAS requirement)
LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)
MarsArea(HAS PlacementBonus<Class<Metal>>)
```

If no dependency position accepts the candidate, the refinement fails. Satisfying a `HAS`
refinement is a state-aware relation, not static nominal subtyping.

This applies even when the written domain is wider than the compatible dependency bound. For
example, `Component(HAS StartToken)` can match only Player components because `StartToken`'s
dependency bound is `Player`; every other Component candidate fails substitution.

**Current defect: refinement substitution forgets authored dependency positions.** Resolving an
expression records the resulting dependency Types but not which dependency keys its written
arguments filled. Candidate substitution starts greedy matching from the first dependency
again and can overwrite an explicitly authored argument when several dependencies accept the same
Type. The intended behavior is for written arguments to reserve their matched dependency keys and
for the refinement candidate to specialize only a remaining compatible dependency. For example,
after that correction an area candidate in
`AreaAdjacency<MarsArea(HAS OceanTile)>` can fill the second `MarsArea` dependency without replacing
the explicitly authored first one.

### Static operations

A refinement is statically below its unrefined base. Narrowing the base while preserving the exact
predicate narrows the refinement. Different predicates do not imply one another statically.

The greatest lower bound of refined and unrefined Types keeps the refinement. Two `HAS` refinements
combine only when both are strict or both forgiving; their Requirements are conjoined. A `HAS` and
`NOT` pair has no single representable common narrowing. A common upper bound keeps a refinement
only when both operands have the exact same one.

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

## 7. Difference refinements

`Domain(NOT Excluded)` denotes the part of an explicit domain that does not overlap the excluded
Type:

```text
Owner(NOT Player1)
ActionCard(NOT Viron)
Actor(NOT Owner)
```

The excluded Type need not narrow the domain. Subtraction uses their structural intersection, so
`Actor(NOT Owner)` excludes Players, which inherit both, while retaining Admin. A candidate satisfies
`NOT` only when its entire structural domain is disjoint from the exclusion. An abstract `Player`
therefore does not satisfy `Owner(NOT Player1)`, because it still admits Player1.

Both operands are explicit and the result is a `GroundType`, so it can stand alone or
appear in a dependency, count, trigger selector, `EACH` selector, or `RANK` metric. There is no
separate dependency kind. If later structural narrowing makes the domain disjoint from the
exclusion, the redundant refinement is removed. A currently empty difference remains representable
because an excluded Type-variable use may specialize later. Concrete enumeration omits excluded
candidates, including when the difference is nested in a dependency. Automatic narrowing may
select the sole concrete candidate surviving `NOT`.

At a dependency use site, the written domain intersects the dependency's declared bound. A wider
domain is therefore safe, while a narrower one intentionally restricts the candidates further; the
domain is semantic input, not an annotation required to repeat the declaration.

The exclusion must be a refinement-free structural Type, recursively. In particular, neither
`Owner(NOT Player(HAS Marker))` nor nested `NOT` is accepted. This keeps difference checks
state-independent and prevents negating a World query. No refinement, including `NOT`, is permitted
in a Class signature; Classes continue to declare structural dependencies and supertypes only.

Two identical `NOT` refinements have a common narrowing. Different exclusions have no
single representable common narrowing because Pets has no union Type for their excluded operands.
`lub` retains a refinement only when both operands carry exactly the same predicate.

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
concrete dependency binding. `HAS` refinements are then tested against World state; `NOT`
refinements filter structurally overlapping candidates without consulting it.

Consumers that already know a smaller set of possible dependency targets may provide that set to
the Class Table's enumeration operation. Custom metrics use live component Types: because a
dependency requires its target component to exist, no omitted specialization could contribute a
nonzero count.

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

`lub` retains a refinement only when both operands have the exact same one; otherwise it falls back
to the unrefined common domain.

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
| `GroundType` | Root Class, dependencies, and optional refinement | A resolved structural Type. |
| `TypeVariable` | Its declaration and lexical scope | One captured Type constrained by its Ground-Type `bound`. |

A variable is therefore not merely an annotation beside its bound. It is a distinct Type with the
same operations. Code that only needs narrowing, dependencies, or Class information uses
the `Type` API. Code concerned with capture or substitution can inspect whether that Type is a
`TypeVariable`, then inspect its declaration and uses.

Every variable bound is a Ground Type. A dependent declaration first receives any visible capture
used inside its authored expression, then resolves that specialized expression as its own Ground-Type
bound. No authored construct requires a variable to remain open inside another variable's bound.

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

A refined declaration evaluates its Requirement when a candidate is captured. Binding consumes that
declaration refinement, so every later occurrence reuses the captured Ground Type without evaluating
the Requirement again. A distinct use-site refinement is a separate constraint.

Variables are recognized from authored syntax before defaults, marked-syntax lowering, owner
substitution, and task splitting. These phases preserve existing declaration identity and use paths
even when an occurrence's spelling changes; they do not discover new declarations from elaborated
syntax.

### Declaration and scope table

| Construct | What declares the variable | Scope and uses | What supplies its value | Representative form |
| --- | --- | --- | --- | --- |
| Class dependency | Each separately declared abstract dependency root declares one Class-scoped variable. Eligible abstract subexpressions along its nested dependency paths declare projected variables supplied by those paths. | The Class header and Effects authored in that declaration, plus inherited copies of those Effects; the structural dependency path survives subtype enumeration. | Specializing or enumerating the component Type. | `CLASS Trade<ColonyTile> ... { This: TradeBarrier<ColonyTile> ... }` |
| Repeated Class-header projection | An abstract header occurrence at one stable dependency key; a matching occurrence at the same key is a use, even through different supertypes. | The complete header and the Class-scoped effect scope above. | Intersection of the dependency bounds, then component-Type specialization. | `CLASS Cardbound<CardFront<Player>> : Owned<Player>` |
| Triggered Effect | Each maximal abstract expression in a choice-producing trigger position is a potential declaration. | That one trigger, including its `BY` and `IF` clauses, and its one instruction tree. | The concrete changed Type that matched the subscription. | `BioTag<CardFront>: Plant OR CardResource<CardFront>` |
| Positive abstract Actor selector | A simple positive abstract Actor expression after `BY`, such as `Player`. This is a binder even without repetition. | The qualified trigger and the fired instruction. Uses inside `NOT` refinements receive the Actor value before the difference is tested. | The concrete Actor recorded on the triggering event. | `-OwnedActorTrigger<Owner(NOT Player)> BY Player: Steel<Player>` |
| Action | Each maximal abstract expression in a choice-producing cost or result position is a potential declaration. | That one Action; lowering preserves it across the resulting sequence. | Narrowing the cost or result, normally the cost first. | `PROD[StandardResource] -> 4 StandardResource` |
| `THEN` | Each maximal abstract expression in a stage is a potential declaration. | All stages of that one sequence, including its continuation as it is enqueued. | Narrowing any occurrence; selecting an earlier stage carries its value into later stages. | `CopyProductionBox<CardFront> THEN CyberiaSystemsFirstChoice<CardFront>` |
| Atomic transmutation | Each maximal abstract proper subexpression in a gain or removal role is a potential declaration. | That one gain/removal pair. The complete destination and source roots are excluded. | Narrowing either role; both roles must agree. | `CityTile<LandArea> FROM GreeneryTile<LandArea>` |

Class dependencies are still dependency edges to components, not conventional generic parameters.
Their resolved targets merely provide durable values for the Class-scoped variables. Each
comma-separated dependency root is independent even when two roots have identical text. Within a
root, a dependency path follows stable keys through nesting, so `Neighbor_0.Tile_0` and
`Neighbor_1` are distinct. A key combines the Class that first declared it with its zero-based
slot, such as `Tile_0` or `Owned_0`; subtypes retain it.

For example, a concrete `Cardbound<CardFront<Player1>>` supplies both the card value and its projected
`Player1` owner value. The second `Player` in `Owned<Player>` uses that projection because both
occurrences address the same inherited owner dependency. The same value then specializes a
`Token<Player>` occurrence in a Class Effect. In contrast, independently declared roots do not
become one variable because their bounds happen to have the same spelling. A body occurrence that
could name two such declarations with different values is ambiguous and is rejected.

### Authored expression identity

Every eligible abstract occurrence in the table is semantically a potential declaration. If no
later use exists, retaining a runtime variable object has no observable effect and is optional. This
is only an implementation economy: repetition does not create the declaration. When uses do exist,
the earliest potential declaration in authored order owns them.

A local use normally requires exactly the same authored expression. Matching is structural equality of
the parsed AST, which naturally ignores whitespace and parser-erased grouping but performs no
resolution, default insertion, or dependency-order canonicalization. Omission, argument order,
and refinement syntax are meaningful authored differences:

```text
Tile != Tile<Area>
OceanTile != OceanTile<MarsArea>
Owner != Anyone
Player != Player(NOT Player1)
```

The whole authored expression is the surface name of an inferred local variable. Resolving two different
expressions to the same Ground Type does not make them uses of one variable. Recognition chooses
maximal expressions, so repeating `Card<Owner>` declares one card variable rather than also
inferring an independent variable from its nested `Owner` text.

There are two lexical extensions. First, each Class header establishes the Class-variable scope for
Effects authored in that declaration; inherited Effects carry their original scope. Text matching
one of those variables in the Class body is a use rather than a fresh declaration. A simple Class
variable may also occupy the root of an occurrence with arguments: `CardFront<Owner>` uses the
Class variable `CardFront` while constraining its owner dependency. Class-header occurrences are
identified by stable dependency paths, so projections named by a Class can be uses even when their
whole containing expressions differ.

A proper type dependency explicitly chosen in the first stage of `THEN` and repeated later belongs
to that queued choice rather than a matching Class variable. Law Suit's `MC<Player>` selects its
opponent and carries that `Player` through the gate and card movement. Outside such a choice, a rule
needing a distinct local capture must use a distinct authored expression. For example,
`ChooseOceanArea` uses `This BY Actor: OceanTile<> BY Actor` so the concrete performing Actor is
captured independently of the owned signal's owner.

Second, if `Player` is visible, the excluded operand in `Owner(NOT Player)` is a use of that
variable. Binding first specializes it to a concrete Actor, then tests the difference. The whole
`Owner(NOT Player)` expression may simultaneously declare a dependent variable under the
declaration rules. In
`-OwnedActorTrigger<Owner(NOT Player)> BY Player: Heat<Owner(NOT Player)>`, the Actor capture `T` is
bound first; the event capture `U` is then selected under `Owner(NOT T)`; and the result uses `U`.
This retains the distinction between “anyone except this Actor” and the particular other value
observed in the event.

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
| A concrete expression or `This` | There is no open choice to bind. `This` is contextual. |

These rows prevent declaration; they do not hide uses of a variable declared in an enclosing scope.
For example, a trigger declaration may have uses inside an `OR` arm or an `IF` Requirement, and a
Class-header declaration may have uses in several comma-separated Effects.

### Contextual and other binders

- `This` means the effect-bearing exact Type. `Class<This>` means its root Class without
  dependencies. Self triggers `This:` and `-This:` match only changed copies of that exact Type.
- `Owner` means the exact context owner when one exists. An ownerless triggered rule may instead
  receive the event's Player Actor under the implicit trigger rule in
  [IDENTITY.md](IDENTITY.md#implicit-trigger-owner). Otherwise it remains abstract and may be
  eligible for normal variable declaration and choice semantics. `Owner` is also a real Class, and
  `Anyone` exists only to name that bound without the contextual meaning; whether that overload
  should survive is audited in
  [IDENTITY.md](IDENTITY.md#owner-is-overloaded-as-a-class-and-as-a-contextual-variable).
- `BY Anyone` is an unrestricted Actor filter, not a declaration. A refined selector such as
  `BY Player(NOT Owner)` is a filter, not a binder. A positive simple abstract Actor subtype such as
  `BY Player` uses the explicit binder rule in the table.
- Trigger `X` and repeated `X` in `THEN` bind a scalar event count, not a Type variable.
- A refined `Class<Tag>` binds the represented candidate Class while testing its Requirement, as
  described in section 6. That represented-Class substitution is not an authored Type variable.
- `RANK Selector { ... }` owns the candidate-name scope in its Metrics. A surrounding refined
  `Class<T>` does not rewrite that subtree while specializing its represented Class;
  ranking binds the selected peer later, including inside a `NOT` exclusion.

### Lifetime and specialization

Class-scoped variables survive inheritance and enumeration. Component specialization substitutes
only their recorded uses in Effects; an unrelated occurrence of the same Class remains an ordinary
bound. Effect-local variables stay open while the effect is installed, then a matching event
specializes their trigger, condition, Actor selector, and instruction together. This is trigger
specialization, not global replacement of every occurrence of the same abstract Class.

Action and `THEN` variables survive lowering and queuing. An open variable prevents the relevant
stages from splitting into independent tasks until an earlier choice supplies its value. Within one
atomic transmutation, `Foo<Same, Here, To FROM From>` is compact syntax for
`Foo<Same, Here, To> FROM Foo<Same, Here, From>`; each unchanged argument occupies both roles and
therefore uses one atomic variable.

The [`EACH`](EACH.md) fanout makes its selector a declaration whose scope is its body. Each
enumerated concrete selector Type substitutes through the recorded use paths. Inside the body, an
Owner selection supplies contextual `Owner`; a non-Owner selection retains the enclosing contextual
owner. `This` is the effect-bearing component. The construct rejects a body with no use of the
selector.

### Implementation direction

Use one authored-occurrence model and shared matching primitives with the scope rules above. Do not
assume that Class-header specialization, trigger capture, and `THEN` continuation need one
recognition algorithm: they receive values from different events and may cleanly retain separate
policies. Consolidate a policy only when its declaration, scope, and binding rules are actually the
same.

Stable authored-occurrence paths may eventually replace the current fallbacks to expression
identity or equality after transformations. That is a possible mechanism, not an accepted next
step. No known card, rule, or normal engine operation currently demonstrates that those fallbacks
produce wrong behavior.

Do not add occurrence tokens, `Expression.Linkage`, or cross-pipeline provenance propagation from
this design description alone. First demonstrate a focused failure through normal Pets elaboration
or game execution. A synthetic test whose only contract is preserving a proposed identity
representation is not sufficient evidence. Any solution must also show why a smaller correction to
the affected construct's existing recognition policy cannot preserve the real behavior.

A rejected implementation is preserved locally as stash
`codex/type-variable-linkage-review-2026-09-02` (stash commit
`8f2c9617401d3d630097fa52209e46a586930194`). Inspect it before revisiting this mechanism. It added
217 net lines across the expression model, preprocessing, scope analysis, engine resolution, and
construct-specific lowering without establishing an observable failure. The stash records the
cost and explored failure modes, not a design to restore wholesale; because Git stashes are local,
the evidence gate above remains authoritative when the object is unavailable.

The card-owned `Splicer<SpliceTacticalGenomics>` component is a working content mechanism, not
unfinished Type-variable infrastructure. Further changes to its ownership or task assignment would
be optional content cleanup.

## 11. Uninhabited Classes and Types

The Catalog's master table establishes one nominal universe. A game projection preserves every
master Class identity in one of two states: active or uninhabited. Unknown names are errors. An
uninhabited Class retains its name, declared hierarchy, and Dependency shape so resolution and
nominal subtyping remain meaningful, but it contributes no live behavior or inhabitants.

A Type is uninhabited when its root or a dependency bound is uninhabited. A `HAS` refinement
mentioning an uninhabited Class has an unsatisfiable Requirement instead; the refined Type itself
need not be uninhabited. A `NOT` refinement does not activate its excluded Type, so excluding an
uninhabited Type leaves the active domain available.
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
Requirements. Vitor can remain active in solo while its `Class<Award>`-gated RequiredAction
and the entire Award domain remain uninhabited. Anything the analysis cannot prove unreachable remains
conservatively reachable. Known declarations outside the closure become uninhabited when the
projection freezes.

An ambient Class owned by an unavailable Bundle cannot be activated by a hard reference. After
closure, premise construction rejects selected root Classes whose `requirement` entry condition is
exactly false, and rejects selected structured content whose reachable mandatory removal targets an
uninhabited Type. See
[OPTIONS.md](OPTIONS.md#settled-projection-policy-direction).

## Appendix: Why covariance is sufficient in practice

Pets dependencies identify exact component targets. A broader dependency in an abstract Type
describes a set of concrete Types for queries and narrowing; it does not describe one component
that accepts every member of that set. Thus `ResourceValue<Class<Metal>>` counts the separate Steel
and Titanium value components, while `ResourceHolder<Class<CardResource>>` ranges over holders that
each support one concrete resource Class.

A Class literal representing an abstract Class is itself abstract. Consequently components such as
`Owed<Class<Metal>>` and `Accepting<Class<Metal>>` cannot exist without first narrowing to one
concrete denomination. Contravariant dependency matching would reverse some subtype relationships,
but would not make these broad components inhabitable or express a debt payable by a mixture of
Steel and Titanium.

Covariance also lets a broad trigger observe narrower events: `Pay<Class<Metal>>` can react to both
Steel and Titanium payments. Where persistent behavior is needed, the model creates concrete
components instead, such as one production-decrease watcher per standard resource. The practical
cost is some component fanout and no direct expression for heterogeneous family-wide payment; that
missing payment operation would need choice or conversion semantics, not dependency variance.
