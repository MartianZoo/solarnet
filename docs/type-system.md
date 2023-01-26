# Type system

The PETS type system is probably pretty unique.

For now, I'll address this to the reader who is already familiar with the Java type system. In the future perhaps I can make something more accessible.

## First

All component instances are immutable. None of them ever change. The game state is a mutable multiset of those component instances. They are only added, removed, and transmuted.

## Classes and types

As you would expect, named classes are introduced through a class declaration. Each class defines a base type which can have various subtypes (even with the same root type, the class).

PETS classes have fields/properties/attributes per se, only dependencies and effects.

Two component instances are distinguishable if and only if their types are distinguishable. Type is everything, because why not?

Classes can be abstract or not (concrete). Multiple inheritance is okay; effects are just added together.

## Generic

PETS types are generic types. We have Energy<Player2> and CityTile<VolcanicArea> etc.

It is a simplified generic type system.

* There are no type variables; or, you might say that in the type `Energy<Player>`, `Player` is *both* a type and a type variable. It of course expresses the upper bound for the type that goes in that position. But also, any effect that mentions `Player` is treated as a placeholder. In the more specific type `Energy<Player2>`, those placeholders change from `Player` to `Player2`. So it's fair to say `Player` is a type variable.

* Types are always covariant. CityTile<VolcanicArea> is a subtype of CityTile<LandArea> is a subtype of OwnedTile<LandArea> etc.

* The types in the <> are not just type parameters; they are *dependencies*. For an instance of `Foo<Bar>` to exist, a particular *instance* of `Bar` must exist that it depends on.

* There are also class literals like `Class<Plant>`. I discussed these a bit in [syntax].



