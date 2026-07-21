# Pets type system: concise specification

> Working draft. This document specifies the type model compactly. For motivation, language
> introduction, and implementation-oriented explanation, see [Type system](type-system.md).

## 1. Classes and nominal subtyping

A class declaration introduces a type of the same name.

```pets
CLASS Con
ABSTRACT CLASS Abs
CLASS Sub : Abs
```

`Con` and `Sub` are concrete classes. `Abs` is an abstract class. The third declaration
establishes the nominal subtype judgment `Sub <: Abs`.

`<:` is a partial order: it is reflexive, transitive, and antisymmetric. Every type is a
subtype of `Component`, the universal top type.

A class may declare several immediate supertypes, separated by commas.

Concrete classes are final: they cannot have proper subclasses. Abstract classes may have
abstract or concrete subclasses. Thus `Sub` could instead be abstract, but `Abs` could not
be concrete while `Sub <: Abs`.

## 2. Components and game states

A component is an instance of a concrete type. Components have neither per-instance state
nor individual identity: two components of the same concrete type are indistinguishable.
Their concrete type is their complete observable description.

A game state `S` is therefore a multiset of concrete types, equivalently a function

```text
S : ConcreteType -> N
```

where `S(T)` is the multiplicity of `T`.

Gaining or removing components requires an exact concrete type. Any type may instead be
counted. The count of `T` includes the instances of every concrete subtype of `T`:

```text
count_S(T) = sum { S(U) | U is concrete and U <: T }
```

Thus gaining `Abs` is ill-formed, while counting `Abs` includes all `Sub` components.

## 3. Dependencies

```pets
CLASS Use<Abs>
```

`Use` declares one anonymous dependency slot whose upper bound is `Abs`. Any Pets type may
be a dependency bound. A specialization supplies a subtype of that bound:

```pets
Use<Sub>
```

A specialization equal to its bound may be omitted, so `Use` and `Use<Abs>` denote the
same type.

Dependencies are covariant. For every class constructor `C`:

```text
T <: U  implies  C<T> <: C<U>
```

Pets has no invariant or contravariant dependency slots.

A type is concrete exactly when its root class and all its dependencies are concrete;
otherwise it is abstract. Consequently `Use<Abs>` is abstract and `Use<Sub>` is concrete.

Dependencies also constrain valid game states. In the example:

```text
S(Use<Sub>) > 0  implies  S(Sub) > 0
```

This referential-integrity condition is motivated by the type system but belongs to the
specification of valid game states.

## 4. Inherited dependencies

```pets
ABSTRACT CLASS Base<Abs>
CLASS Child : Base
```

`Child` has the dependency slot introduced by `Base`; the slot is not redeclared or copied.
For every admissible specialization `T`:

```text
Child<T> <: Base<T>
```

## 5. Refining a dependency

```pets
ABSTRACT CLASS Special : Base<Sub>
```

`Special` retains the dependency introduced by `Base` but narrows its upper bound from
`Abs` to `Sub`. It does not introduce another slot. This is compatible with covariance.
If `Sub` is abstract, a subclass may refine the bound again. If `Sub` is concrete, it
cannot be refined further because concrete types have no proper subtypes.

## 6. Introducing another dependency

```pets
ABSTRACT CLASS Second
CLASS Double<Second> : Base<Abs>
```

`Double` has two dependency slots: the inherited slot bounded by `Abs`, followed in
canonical declaration order by the new slot bounded by `Second`. Writing the supertype as
`Base` would be equivalent to writing `Base<Abs>`.

Dependencies and supertypes are each written as comma-separated lists. In a type
expression, specializations equal to their bounds may be omitted and the remaining
specializations need not appear in canonical order. The rules that match specializations
to dependency slots, including ambiguity and multiple inheritance, are specified
separately.
