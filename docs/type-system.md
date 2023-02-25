# Type system

In order to express game instructions in a way that is both "executable" and also representable as icons in a way very similar to the published icon grammar, a novel type system had to be designed. This type system is of course tailored closely to what this particular game needs from it.

For now, I'll have to address this to the reader who is already familiar with the Java type system, or ones enough like it. Later I can try to write something more accessible.

## Components

A game state consists entirely of a multiset of **component** instances (plus task queues, which we can ignore for now). Each of these components has a concrete **type**. In fact, components are distinguishable by their *type alone*; there are no attributes/properties/fields in the language. Accordingly, component instances are always immutable.

For example, when the game begins there are 20 instances of `TerraformRating<Player1>`, 20 of `TerraformRating<Player2>`, etc. The first 20 are indistinguishable from each other, and only their type distinguishes them from the latter 20. I'll try to use the term "component" consistently to mean a *single* instance, a.k.a a single occurrence of a component type (e.g., there were 40 components discussed in our example).

A game state is mutable. There are only two operations: adding or removing N identical copies of a component. (Technically there are atomic transmutations as well, but these aren't that different from a remove-and-add.) Being a multiset, or "just a bag of things", you can never have a negative amount of anything -- a fact which creates exactly one headache for Terraforming Mars (megacredit production). This headache is worth suffering, though, because other than that adopting the multiset concept makes a large number of bugs impossible.

Note that components are never "out of play"; a tile that's not yet on the board simply doesn't exist, and when you pay for a card the resources just vanish. (The "transmuting" just alluded to is fairly rare; mostly "steal" cards, and energy becoming heat during a production phase.)

## Classes and types

As expected, we *declare* named classes (like `Player1`, `Animal`, or `EcologicalZone`) each class *defines* a type (by the same name), and then these types can be assembled into more complex types (like `Animal<Player1, EcologicalZone<Player1>>`).

A class is abstract or concrete. It can have any number of superclasses, which might also be abstract or concrete, without restriction (concrete extending concrete is very rare: only `CapitalTile` does it).

All classes are loaded and frozen before a game begins. So, for example, for any class we always know the complete set of its subclasses. Only the types that *might* be needed in that game are loaded. For example, without the Venus expansion there will be no such type as `VenusStep` or `VenusTag`. This also explains, for example, how we can tell which 5 milestones are available to be claimed, even though no instance of `Milestone` exists in the game state until we claim one; we just look at what classes have been loaded. (This scheme works out well in many ways, while creating just one headache, called Aridor.)

## Generic types / dependencies

Pets types are generic types. We have `Energy<Player2>` and `CityTile<VolcanicArea>` etc. The first class named is the "root type", then comes a list of "specializations" (similar to "type arguments", but we'll get to the differences).

These types express not just an arbitrary parameterization, but a *dependency*. An instance of `Energy` cannot exist without one *specific* instance of `Player` to depend on. A `CityTile` can't exist without both a `Player` and an `Area` to depend on. A component can't be removed without dealing with its dependents somehow (for example, to remove `PharmacyUnion<Player2>` from play, all the `Disease<PharmacyUnion>` components must already be gone, and the two `MicrobeTag<PharmacyUnion>` components need to be removed automatically with the card).

Using these class declarations:

```
ABSTRACT CLASS Owned<Player>

ABSTRACT CLASS Area
ABSTRACT CLASS LandArea : Area
CLASS Tharsis_5_6 : LandArea

ABSTRACT CLASS Tile<Area>
CLASS OceanTile : Tile
CLASS GreeneryTile : Owned, Tile<MarsArea>
```

We can see that:

* Any subclass of `Owned` will have a dependency on a `Player` (so, to create a `Plant` you will need to supply which player owns it). `Player` is the upper bound for this dependency, but this bound could be narrowed by a subclass.
* `LandArea` is a subclass of `Area`, but is still abstract
* `Tharsis_5_6` is one specific (concrete) subclass of `LandArea`
* Any instance of `Tile` will depend on an instance of `Area`
* `OceanTile` automatically inherits that dependency
* `GreeneryTile` automatically inherits the `Player` dependency from `Owned`, and...
* ... also inherits the `Area` dependency from `Tile`, which it *narrows* to `MarsArea` (since greenery tiles on Phobos Space Haven are not a thing)

These classes bring a multitude of types into being. A specific greenery tile might be `GreeneryTile<Player1, Tharsis_5_6>`, which is equivalently specified as `GreeneryTile<Tharsis_5_6, Player1>` (dependencies are generally not positional). When counting *all* of `Player1`'s greenery tiles we would use the type `GreeneryTile<LandArea, Player1>`, or `GreeneryTile<Area, Player1>` (same thing), or more commonly just `GreeneryTile<Player1>`. We can always omit a dependency when it would be the same as that dependency's upper bound.

### Variance

All Pets types are implicitly **covariant**. One subtype of `Tile<Area>` is `CityTile<Area>`; another is `Tile<LandArea>`, and subtyping both of *those* we have `CityTile<LandArea>`.

(The always-covariant rule creates a problem for the payment-mechanic classes `Accept` and `Owed`. If an instance of `Owed<CardResource>` exists, we might think we can pay that off using any kind of `CardResource`. But it does not work that way. Unsure yet how to deal with this.)

### Abstract vs. concrete

A type is abstract if *either* its root type (class) is abstract, *or* if any dependency type is abstract, *or* if it has a refinement. Of course, an abstract type works fine for just counting components, but in order to create or remove a component the type must be concrete. Of course, this makes sense; if I said "please add a tile to the board", you would have to ask "what kind of tile, and where exactly, and with whose player cube on it?", whereas you can answer "how many tiles are on the board?" exactly as asked.

This can be slightly confusing. The *class* `OceanTile` is a concrete class. Yet the *type* `OceanTile`, being an abbreviation for `OceanTile<MarsArea>`, is abstract (because `MarsArea` is abstract). So "is `OceanTile` concrete?" is an ambiguous question.

(Aside: a Pets instruction like `3 Animal<CardFront>` is valid even though the `Animal<CardFront>` type is abstract. It simply cannot be *executed* in that form. It must be narrowed to a concrete type first, such as `3 Animal<Player3, Predators<Player3>>`. There is no way to break it up into, say, `2 Animal<Predators>, 1 Animal<Fish>`. Conveniently, this simple constraint makes the game rules work correctly in a variety of ways. If a card says `-6 Plant<Anyone>?`, can you remove 4 from one player and 2 from another? No, you can't. There are a few automatic "narrowing" steps, but for the most part an abstract type left in an instruction allows the player to choose how they want to narrow it. For example, in `CityTile<VolcanicArea>`, the player can narrow it to `CityTile<Tharsis_2_2>`. Since that is concrete, however, they cannot choose to *further* narrow it to `CapitalTile<Tharsis_2_2>`. Okay, enough digression.)

### Effects and specialization

Consider this example (it's a bit fictional; I'll show how production is really implemented lower down):

```
CLASS PlantProduction<Player> {
    ProductionPhase: Plant<Player>
}
```

Inside a class declaration, lines of the form `<trigger>: <instruction>` are effects. They express that component's behaviors.

The type `PlantProduction<Player>` is abstract, meaning that no component of that exact type can exist. That's because, even though `PlantProduction` is concrete, `Player` is not, and *all* types seen must be concrete for the whole type to be.

But `PlantProduction<Player4>` is concrete, and one of those can exist. And (here's the important part) narrowing `Player` to `Player4` in the type expression *also* narrows it in the same way for any effects belonging to the class. So the effect that actually becomes active is not `ProductionPhase: Plant<Player>`, but `ProductionPhase: Plant<Player4>`. (And a good thing, because `Plant<Player>` is abstract and during the production phase there is no active player who would be able to choose how to narrow it!)

One possible way to think of this is that `PlantProduction<Player>` both specifies `Player` as the upper bound for that dependency, *and* names a "type variable" `Player` as well, and the effect is actually naming that *type variable* rather than the `Player` type itself. This may be a convoluted way to look at it, though.

### `This`

The effects inside a class declaration can use the special class name `This`. It is a placeholder for the *specific concrete type* of whatever component inherits it.

### Singleton types

A class may be identifiable as a singleton class (*how* is not relevant here). If it is, then every concrete type whose root type is that class or any of its subclasses is considered a singleton type. Before a game begins, one instance of each singleton type is automatically created.

For example, `Area` is a singleton abstract class. It has 61 concrete subclasses (if playing with the Elysium expansion, these are called `Elysium_1_1`, `Elysium_1_2`, etc.). Therefore at the very start of a game, the Requirement `HAS 61 Area` will already evaluate to true; one instance of each of these concrete classes has already been created. 

### Class types

The `Class` class is predefined. It accepts one "type argument" or "specialization", but works differently from all other generic types:

* This is *not* a "dependency"; `Class<Steel>` can exist even though no `Steel` instance exists yet to be depended on.
* Only a single class name can go inside the angle brackets. `Class<Steel>` works but `Class<Steel<Player2>>` does not.
* Even though the type `Steel` is abstract, and the type `AnythingElse<Steel>` would also be abstract, `Class<Steel>` is considered concrete! After all, it's as concrete as it *can* be.

`Class` is a singleton class. If you ask a game state to count the instances of the type `Class<StandardResource>` you will get the answer `6`. (Those are `Class<Megacredit>`, `Class<Titanium>`, etc. You don't get 7, including `Class<StandardResource>` itself, because `Class<StandardResource>` is abstract, and so cannot exist as a component.)

An open question is whether the differences outlined here are enough to justify using a different syntax -- for example instead of `Class<Steel>` we could use `Steel.CLASS` or `{Steel}` or something else. Currently I think there are probably enough similarities to make it worth keeping as-is, but I'm not sure.

#### What's that good for?

Production is actually defined more like this:

```
CLASS Production<Class<StandardResource>> : Owned {
    ProductionPhase: StandardResource
}
```

`Class` is a handy trick here. It would not make sense to have this as `Production<StandardResource>`, because then anytime you had no `Energy` resources you would be unable to have any `Production<Energy>` either! (Remember, these aren't generic types; they express real live dependencies.) But with this trick a game state can have `4 Production<Class<Steel>, Player2>`, `8 Production<Class<Heat>, Player2>`, and so on. The first type has an effect that is appropriately specialized from the class effect shown above; instead of just `ProductionPhase: StandardResource` is becomes `ProductionPhase: Steel<Player2>`.
