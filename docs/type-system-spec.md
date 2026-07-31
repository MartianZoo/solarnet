# Pets type system: concise specification

See [Type system](type-system.md) for a more human-readable tutorial.

## 1. Classes and nominal subtyping

A class declaration introduces a type of the same name.

```pets
CLASS Con
```

The type `Con` is concrete.

```pets
ABSTRACT CLASS Abs
CLASS Sub : Abs
```

`Abs` is an abstract class. `Sub` is a concrete class and a direct subclass of `Abs`.
Concrete classes may not have subclasses. A class may have any number of abstract classes as
superclasses, separated by commas.

As types, `Sub` is a subtype of `Abs`, written `Sub <: Abs`. Other cases of subtyping are
defined below. The subtype relation is the reflexive, transitive closure of the relationships
specified in this document.

The universal top type is `Component`. Except for `Component` itself, every class that declares
no superclass has `Component` as its implicit superclass.

## 2. Components and game states

A component is the basic unit of a world. It has several types. Exactly one is concrete and
is called its concrete type; the others are the proper supertypes of that concrete type.

A component has no identity and no state beyond its concrete type. Several components may have
the same concrete type, but those components are indistinguishable. The components in a game
state therefore form a multiset.

A component and its concrete type can sometimes be treated interchangeably, but only where
multiplicity is not relevant.

## 3. Class literals

For every loaded class `Foo`, the type `Class<Foo>` exists. The contents of the angle brackets
are a single class name: `Class<Foo>` is admissible, but
`Class<Foo<Bar>>` is not.

The class named in the angle brackets is not a dependency. A dependency would require a
component with type `Foo` to exist; a class literal instead requires the class named `Foo` to
exist.

Like other angle-bracketed types, class literals are covariant:

```text
Foo <: Bar  implies  Class<Foo> <: Class<Bar>
```

If `Foo` is concrete, `Class<Foo>` is concrete, and exactly one component of this type exists
in every world in which `Foo` is loaded.

If `Foo` is abstract, `Class<Foo>` is abstract and cannot have its own corresponding
components. Counting it nevertheless counts the class-literal components of all concrete
subclasses of `Foo`, by the usual subtype-counting rule.

If `Bar` is not loaded, `Class<Bar>` is not a valid type. Used as a metric, however, the
expression can still be evaluated, and its count is zero. This exception does not create a
phantom `Bar` or `Class<Bar>` type.

## 4. Dependencies

```pets
CLASS Use<Abs>
```

This class introduces a dependency whose upper bound is `Abs`. Multiple dependencies may be
declared, separated by commas. Any Pets type may be a dependency bound: it may be concrete or
abstract, may have dependencies of its own, and may even be a class literal.

The declaration introduces one type for every admissible subtype of `Abs`, including:

- `Use<Abs>`
- `Use<Sub>`
- and corresponding types for any other subtypes

In every case, the class of the type—also called its root class—is `Use`. The type expression
`Use` is valid and resolves to the same type as `Use<Abs>`. `Use<Con>` is invalid.

Dependencies are covariant by design: because `Sub <: Abs`, it follows that
`Use<Sub> <: Use<Abs>`.

A type of this form is concrete exactly when its class and all its dependency bounds are
concrete. If any are abstract, the type is abstract. Consequently `Use<Abs>` is abstract and
`Use<Sub>` is concrete.

In the component graph, components are vertices and dependencies are directed edges from
dependent components to their targets. Each edge is labeled by its dependency key. A
dependency path is a directed path in this graph; other directed-graph terminology
applies in the usual way.

The target component must exist for as long as the dependent component does. Its exact concrete
type designates it; it has no separate identity.

Because components of the same concrete type are indistinguishable, every concrete type
admitted by a dependency bound must have an applicable `MAX 1` or `=1` invariant. The invariant
may be declared on the target class or supplied by a stronger aggregate bound. When the
dependency bound is abstract, the rule applies to every concrete type it admits.

## 5. A dependency belongs to all subtypes

```pets
ABSTRACT CLASS Base<Abs>
CLASS Child : Base
```

We do not say that `Child` "inherits" the `<Abs>` dependency, because in Pets there is no
separate inheritance mechanism or choice involved: every `Child` is a `Base`, and every `Base`
has that dependency, so every `Child` has it too. It is the same dependency, not a copy,
redeclaration, or override. For every `T <: Abs`:

```text
Child<T> <: Base<T>
```

An inherited `This` specialization remains bound to the current subclass. Thus if `SelfBound`
extends `Link<Class<This>>`, then `SelfLeaf : SelfBound` has the bound `Class<SelfLeaf>`.
An explicitly written `Class<SelfBound>` is a fixed class literal and remains unchanged in
subclasses; textual equality after resolving `This` does not erase that distinction.

Since `Base` and `Base<Abs>` are the same type, either could be written as `Child`'s supertype
with the same effect.

## 6. Narrowing a dependency bound

```pets
ABSTRACT CLASS Special : Base<Sub>
```

Every `Special` is a `Base`, so it has the same dependency that `Base` introduced. The
declaration narrows that dependency's upper bound from `Abs` to `Sub`; it does not introduce,
copy, or override a dependency. For every `T <: Sub`:

```text
Special<T> <: Base<T>
```

If `Sub` is abstract, a subclass may narrow the same bound again. If `Sub` is an unrefined
concrete type, it cannot be narrowed further by nominal subtyping or dependency narrowing.

## 7. Introducing another dependency

```pets
ABSTRACT CLASS Second
CLASS Double<Second> : Base<Abs>
```

`Double` has two dependencies: the same dependency required by being a `Base`, bounded by
`Abs`, and the new dependency introduced by `Double`, bounded by `Second`. Their dependency
keys determine canonical declaration order. Writing the supertype as `Base` would be equivalent
to writing `Base<Abs>`.

Dependencies and supertypes are each written as comma-separated lists. In a type
expression, dependency bounds equal to their upper bounds may be omitted, and the remaining
bounds need not appear in canonical order. The rules that match written bounds to dependency
keys, including ambiguity and several supertypes, are specified separately.

## 8. Refinement types

For a non-refinement type `T` and requirement `R`, `T(HAS R)` is a refinement type. `T` is its
unrefined type, and `R` is its refinement. A refinement type is always abstract and is a subtype
of its unrefined type.

A refinement type selects the components of its unrefined type that satisfy the given
requirement. Since components have no distinguishing features beyond their concrete type, this
might initially appear to be of little use. However, `R` may act on any part of the world after
the candidate type has been substituted according to the dependency-binding rules. Whether a
candidate satisfies the refinement therefore depends on the world and is decided outside the
static type system; satisfying concrete types are not subtypes of the refinement type.

The subtypes of `T(HAS R)` are the types `S(HAS R)` for `S <: T`. The refinement itself may not
vary: `A(HAS 3 B)` is not a subtype of `A(HAS 2 B)`, even though the former requirement implies
the latter.

Whether a concrete type satisfies a refinement is consequently a game-state-dependent narrowing
operation, not a subtype judgment. First, the concrete type must be a subtype of `T`; then the
world must satisfy the substituted `R`. This operation allows a refinement type to be
counted or narrowed to a concrete choice without asserting a static relationship that the type
system cannot establish by itself.

The unrefined type `T` need not be abstract; even a concrete `T` may be refined. This creates a
proper abstract subtype of `T`, but it does not violate the finality of concrete classes: the
refinement introduces no subclass and has no exact component instances of its own.

`T(HAS? R)` is a forgiving refinement. A candidate satisfies it when the candidate satisfies the
ordinary `T(HAS R)` refinement, or when the current world contains no component satisfying
`T(HAS R)`. The second condition is its escape clause; it does not change the static subtype rules
above.

## 9. Closed-world type operations

For a fixed class table, Pets can enumerate every concrete subtype of any type `T`:

```text
concreteSubtypes(T) = { U | U is concrete and U <: T }
```

This includes choosing every concrete root class below `T`'s root and every admissible
combination of concrete dependency bounds. A complement or linkage, when present, further
constrains the enumerated combinations. Enumeration may be large but is finite whenever the
loaded class table has finite dependency closure. In that case, Pets can determine whether an
abstract type has no concrete narrowing, exactly one concrete narrowing, or a genuine choice
among several. A game engine can use this operation to generate legal choices rather than merely
validate a choice already made.

Under section 8's static subtype rules, a refinement type has no concrete subtypes. A state-aware
consumer instead enumerates the concrete subtypes of its unrefined type and applies the separate
refinement-narrowing operation to filter them.

Greatest lower bound and least upper bound retain their ordinary order-theoretic meanings whenever
the relevant bound exists uniquely. Multiple nominal inheritance does not by itself guarantee that
every pair has a unique greatest lower bound or least upper bound.

A nominal class can be such a bound without introducing structural intersection types. For
example, if `OwnedTile` is a subtype of both `Owned` and `Tile`, and every class that subtypes both
also subtypes `OwnedTile`, then `OwnedTile` is their greatest lower bound by the ordinary
definition.

## 10. Linkages

> The rules in this section are proposed target semantics. The runtime implements several
> of them through older, separate mechanisms; [the linkage plan](../plans/linkages.md)
> records the remaining discrepancies and migration work. Sections 10 and 11 intentionally
> range beyond the type system into effects, instructions, and task splitting so that
> linkage has one coherent end-to-end meaning.

A linkage is a source-declared equality constraint among two or more occurrences of one
abstract type expression. The occurrences denote one choice: narrowing any occurrence
narrows every occurrence in the linkage to the same type.

Linkages are recognized from the authored expression tree, before defaults or other
preprocessing insert expressions. Two source expressions are the same when their parsed
trees are structurally equal after any external source translation has produced canonical class
names and each explicitly written dependency bound has been associated with its dependency key.
Whitespace, redundant parentheses, and the order in which unambiguous
dependency bounds are written therefore do not matter. All other authored structure does:
omitted bounds stay omitted, and refinements and complements are not rewritten
merely because they are logically equivalent.

In particular, linkage recognition does not use resolved-type equality. No pair below
creates a linkage merely because later resolution or context makes its members equal:

```pets
Tile                  Tile<Area>
CityTile              CityTile<LandArea>
Owner                 Anyone
```

This distinction makes linkage intentional enough to inspect in source and prevents
defaults or normalization from silently coupling independent choices.

Only an expression whose resolved type is abstract can introduce a type linkage. If a
larger repeated expression introduces a linkage, occurrences nested inside those larger
occurrences do not introduce additional linkages. For example, repeating
`CardFront(HAS BioTag)` links that whole expression, not a second independent `BioTag`
choice. Within one recognition scope, all eligible maximal occurrences of the same
normalized expression form one linkage. A linkage declared by a supertype remains the same
linkage in its subclasses; coincidentally matching text elsewhere creates no relationship.

### 10.1 The contextual `This` binding

`This` is closely related but is not a linkage inferred from repetition. It is a reserved,
explicitly named contextual binding belonging to a class declaration. One occurrence is
enough to refer to it, and every `This` occurrence in that class's signature or body refers
to the same binding. `This` occurrences are therefore excluded from repeated-expression
recognition; repetition adds no second constraint.

In an abstract class, `This` remains late-bound as the declaration applies to subclasses.
For a component whose exact concrete type is `C<Args>`, an ordinary `This` occurrence binds
to `C<Args>`, while `Class<This>` binds to `Class<C>` because a class literal contains a
class name rather than a type with dependencies. An explicitly authored `Class<Base>` remains
`Class<Base>` in subclasses; it does not acquire `This` semantics merely because it
temporarily resolves to the same class literal.

The trigger forms `This:` and `-This:` are self-event selectors, not ordinary type
subscriptions. They respond only to the occurrences of the effect-bearing concrete type
gained or removed by the current change. A change of `N` copies scales the triggered
instruction by `N`; other copies already present do not multiply it. Thus these are not
equivalent inside `CLASS A` when several `A` components exist:

```pets
This: B
A: B
```

The future runtime model may share binding identity and occurrence-path machinery between
`This` and repetition-created linkages. Their recognition and binding sources remain
distinct: repetition creates a choice to be narrowed, while the enclosing concrete
component supplies `This`.

### 10.2 Recognition scopes

Repetition creates a linkage only across the following related source regions:

- In a class signature, repeated explicitly written bounds for the same dependency key are
  linked. Thus
  `Cardbound<CardFront<Owner>> : Owned<Owner>` has one `Owner`
  choice. The root occurrence of each entry in a class's own dependency declaration
  introduces a fresh dependency and is not eligible. Consequently the two `Tile`
  dependencies of `Adjacency<Tile, Tile>` remain independent, as do the nested and top-level `MarsArea`
  occurrences in `Neighbor<Tile<MarsArea>, MarsArea>`. Repetition in sibling branches of
  one `<...>` list also does not create a linkage; for example, the two `Component`
  occurrences in `Pair<Class<Component>, Class<Component>>` are independent.
- An expression in a class dependency signature links to the same expression in each of
  that class's effects. Narrowing the component through that dependency therefore applies
  the same narrowing to its effects.
- The trigger and instruction of one effect are related regions. A match that narrows a
  linked trigger expression applies the same narrowing to the instruction. This is the
  behavior historically called *trigger specialization*; under this terminology it is
  linked trigger narrowing.
- The cost and result of one action, the stages of one `THEN`, and distinct operand roles
  of one atomic instruction are related regions. In a transmutation, the complete source
  and destination expressions are deliberately excluded: they are independent choices,
  but matching proper subexpressions inside them can link. Thus the two `LandArea`
  occurrences in `CityTile<LandArea> FROM GreeneryTile<LandArea>` link, while the two
  `ColonyProduction` operands in `ColonyProduction FROM ColonyProduction` do not.

Comma-separated instructions and alternative `OR` arms do not create linkages with one
another. A linkage established by an enclosing class, effect, action, or `THEN` may still
have occurrences inside those instructions or arms and is applied before they separate.

An occurrence inside a requirement or refinement can refer to a linkage introduced by a
matching occurrence outside requirements. Requirement-only repetition does not introduce
a linkage: testing two facts does not assert that the same witness satisfies both.

### 10.3 Applying a linkage

A type linkage begins with the upper bound denoted by its repeated source expression. A
proposed narrowing supplies one subtype of that bound and substitutes it at every linked
occurrence. The containing expressions are then resolved and validated. The narrowing is
rejected if any occurrence cannot accept it or if different occurrences would require
different types.

A linkage remains unresolved while more than one concrete binding is possible. A
composite instruction must not be split across a boundary crossed by an unresolved
linkage. Once earlier work or an enclosing binding fixes the linkage, its value is
substituted into all later occurrences; ordinary task splitting may then continue. This
rule couples only the linked choice, not the execution of otherwise sequential stages.

## 11. Scalar linkages

Two or more authored `X` occurrences in linkage-eligible regions introduce a scalar
linkage rather than a type linkage. All linked occurrences denote one positive integer,
and a coefficient such as `3X` denotes three times that integer. The recognition scopes
and splitting rules of section 10 apply unchanged. A lone `X`, including the `X` modifier
on a trigger, retains its ordinary scalar or trigger meaning without creating a linkage.
Scalar linkage is determined by occurrence identity, not by pairing `X` occurrences in
traversal order.
