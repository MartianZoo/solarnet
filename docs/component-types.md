# Terraforming Mars component classes

Here's an overview of the component classes that (currently) make up the core of the game.
Refer to the source code as you read:

* [components.pets](https://github.com/MartianZoo/solarnet/blob/main/canon/src/main/kotlin/dev/martianzoo/tfm/canon/components.pets)
* [player.pets](https://github.com/MartianZoo/solarnet/blob/main/canon/src/main/kotlin/dev/martianzoo/tfm/canon/player.pets)

## Communal / non-player-owned

First, `Component` is the root of the class hierarchy; *every* instance of anything in a game state is always
a `Component`.

### Global parameters

The abstract class `GlobalParameter` has four concrete subclasses: `TemperatureStep`, `OxygenStep`, `VenusStep`, and `OceanTile` (which is *also* a subclass of `Tile`). These count "steps", so for example when there are six occurrences of `TemperatureStep` in the game that means the temperature is -18 C.

You can pretty easily guess why the `TemperatureStep` class declaration includes the line `HAS MAX 19 This`. Nineteen temperature steps up from the starting point gets you to 8 degrees Celsius and there can never be any more than that.

These classes contain effects for track bonuses, but they look a little strange; for example `OxygenStep` says `This: (=8 This: TemperatureStep) OR Ok`. There's a lot going on here. First, the effect triggers every single time oxygen is raised (and we have to ensure, through some unknown means, that it is always raised one step at a time). Each time, one of the two instructions separated by `OR` will be carried out. But the left one is only possible when the requirement `=8 OxygenStep` is met. Every other time the right side will *have* to be chosen, and `Ok` is the general-purpose "do nothing" command in PETS. (Actually it's just a component type like any other, having the effect `This:: -This` so it can never actually be created.) Note that if `OR Ok` were not present then oxygen raises would be impossible, as you'd be left with a dead-end unexecutable instruction on your queue.

The definition of the GlobalParameter class includes the line `+This.`. Any gain/remove instruction in PETS can be
followed by an "intensity": either `!` meaning "mandatory", `?` meaning "optional", or `.` meaning "to the extent
possible". The `Component` class sets a default gain intensity of `!`, because most things in the game are mandatory. This line in `GlobalParameter` says that all gains of GlobalParameter instances default to "if possible". If one can't be added due to hitting the maximum limit, the instruction will still be executable and simply do nothing. As you might guess if you know the TfM rules well, `CardResource` also has the same default.

### Maps

One `MarsMap` instance will exist, such as `Hellas`, but it doesn't do much. The interesting part is the areas. Every area is its own component instance; these are singleton classes so one of each is automatically created before the game begins.

The created areas have names like `Hellas_1_1`, `Hellas_1_2`, etc. The coordinate system is easy to understand if you try the `map` command in the command-line REPL tool (`./rego`).

Each area has a supertype that is one of `RemoteArea`, `WaterArea`, `LandArea`, `VolcanicArea`, or `NoctisArea`. `VolcanicArea` itself has `LandArea` as a supertype, so volcanic areas count as land areas. `NoctisArea` is *not* a `LandArea` because it doesn't function like one.

All areas except for `RemoteArea`s have the supertype `MarsArea`, so that cards like `Martian Rails` can work, and so that tiles except `CityTile`s can be restricted to those areas.

Areas don't get created for maps you aren't using in that game. So for example if the board is Hellas then the requirement `MAX 0 VolcanicArea` evalutes to true. That's handy for Lava Tube Settlement: `CityTile<VolcanicArea> OR (MAX 0 VolcanicArea: CityTile)`.

### Tiles

`Tile` is declared as `ABSTRACT CLASS Tile<Area>` which gives it a dependency onto `Area`. This means no tile can ever exist without having a specific `Area` that it relates to. Of course, tiles that aren't on the board yet are treated as simply not existing.

Area, by the way, was declared as `Area(HAS MAX 1 Tile)`. That's our first example of an *invariant*; the game engine
will ensure that no 2 distinct Tile types can ever relate to the same Area (TODO).

As for tile subtypes, we mentioned `OceanTile`, but will get to the rest in the player-specific section below.

### Actions

Any component that makes actions available for possible selection extends the supertype `HasActions`; these includes the
abstract classes `StandardAction`, `StandardProject`, and `ActionCard`.

The first two are singleton types: each concrete subtype such as `Aquifer` automatically has an instance created before
the game starts. Therefore if the user signals `UseAction<Aquifer>` it will be able to respond, bill the user 18 money
and put an `OceanTile` instruction on the user's task queue.

### Phases

The plan is for exactly one Phase instance will exist at all times: `SetupPhase`, `CorporationPhase`, `ResearchPhase`, `ProductionPhase`, etc. A phase called `End` triggers victory point payouts (it has such a short name because it has to be written on MANY cards!). When the final phase `Shutdown` is created, the game is thereby concluded and no more state changes can happen.

## Player stuff

Concrete classes called Player1, Player2, etc. will exist. Player1 is the start player. Mapping those to players' names
is considered a UI-level task.

The abstract class these all subclass isn't called `Player`, but `Anyone`. That's just because it reads better, for
example `CityTile<Anyone>: PROD[1]`. And, actually, we have an abstract subclass of `Player` called `Owner` even though nothing else extends `Player` but that. The reasons for this are weird and subtle and not necessarily permanent.

### Owned

The `Owned` abstract type is extremely important. It has a dependency onto `Owner` (which `Player1` etc. all extend), meaning that every concrete instance of any `Owned` subclass must always know which player it belongs to. Many, many component types have `Owned` as a direct or indirect supertype.

A simple example of an owned component type is `VictoryPoint`.

`TerraformRating` is a very simple class to understand; it looks like this:

```
CLASS TerraformRating {
    ProductionPhase: 1
    End: VictoryPoint
}
```

When the `ProductionPhase` signal goes out, each occurence of `TerraformRating` generates 1 megacredit for its owner. Likewise when the `End` signal gets posted, each occurrence of `TR` generates a victory point. And that's all there is to terraform rating.

As much as possible we would like for the `Owned->Owner` dependency to be a regular component dependency just like any other in the game. However, so far we have had to treat it as special in a few ways. I'll have to get into that some other time.

### OwnedTile

An abstract class `OwnedTile` extends both `Tile` and `Owned`. There are tests that ensure that no component ever
extends both `Tile` and `Owned` without also extending `OwnedTile`. This lets us treat the latter as if it is a proper intersection type of the first two.

The three kinds of tiles are `GreeneryTile`, `CityTile`, and `SpecialTile` (the last is abstract as each specific kind
of tile extends it).

The most interesting thing about these component types is that they use *defaults*.

```
CLASS CityTile {
    DEFAULT +CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>
    ...
}
```

Any card, class declaration, etc. that writes the simple type expression `CityTile` without specifying a kind of `Area` will get this default type instead. But simply write, say, `CityTile<VolcanicArea>` or ` CityTile<LandArea(HAS 2 Neighbor<CityTile<Anyone>>)>` (as `UrbanizedArea` does), and the default is ignored.

### Resources

Resources are divided into `StandardResource` and `CardResource`. The latter has a dependency on a `CardFront`; the
resource can't exist unless the card it goes on does first.

The standard resources are mostly boring except Energy has this:

```
CLASS Energy { ProductionPhase:: Heat FROM This }
```

We discussed the `Phase` types above. As much as possible, these types do nothing by themselves; they exist only to
trigger instructions like this one. Every single Energy instance in the game will respond to that trigger by transmuting
itself into Heat. The double colon ensures that it happens first before regular production.

### Cards

It took a while to realize that `CardBack` and `CardFront` should actually be completely different, unrelated types -- just like `CityTile` and `GreeneryTile` are, despite the fact that they also are two sides of the same physical component. `CardBack` is very uninteresting; it just has the three subtypes `ProjectCard`, `CorporationCard`, and `PreludeCard`, and that's about it. (Remember these things have no attributes either.)

The most important thing to understand about cards is that the engine supports only "follow-along mode" or "magic cards
mode". This means it neither knows nor cares what cards you have in your hand. It doesn't shuffle a deck or deal random cards to anyone. During this phase of this project's evolution, it's assumed that you're actually *playing* a game IRL or on another app and just logging the moves here for testing purposes. So if you tell it that next you played `EarthCatapult`, it will believe you, and subtract one generic `ProjectCard` from your hand. I don't expect the engine to support shuffle-and-deal mode for quite a long time.

Even with this simplification, the whole play-a-card process is a bit complex to go into here and now.

Cards can have three types of things "on" them, which all share the superclass `Cardbound`. These are `Tag`
s, `CardResource`s, and `ActionUsedMarker`s. What these all have in common is that the `CardFront` must exist before
they can, and if the `CardFront` ever went away they would have to as well. This is, of course, just how dependencies
work in PETS.

`Cardbound` is an interesting case in that it is both `Owned`, and depends on a type (`CardFront`) which is also `Owned`, and we want to make a rule somehow that these two owners are always the same. We don't have that yet, so the system sees `Animal<Predators, Player2>` as abstract, and expects to see `Animal<Predators<Player2>, Player2>` to mean the concrete type. That's quite unfortunate and I hope to solve it soonish...

### PaymentMechanic

A few types are busily doing weird stuff behind the scenes to let you pay for stuff properly: `Owed`, `Accept`, `Pay`, `PlayCard`, and `PlayTag`. The best way to understand what these are for is to see how they are used on cards in `cards.json5`.
