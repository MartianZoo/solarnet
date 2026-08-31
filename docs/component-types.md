# Terraforming Mars component classes

Here's an overview of the component classes that (currently) make up the core of the game. Refer to
Terraforming Mars [`classes.pets`](https://github.com/MartianZoo/solarnet/blob/main/src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets)
as you read.

## Communal / non-player-owned

First, `Component` is the root of the Class hierarchy; *every* instance of anything in a Game World is always a `Component`.

### Global parameters

The abstract class `GlobalParameter` has three concrete subclasses in the base game: `TemperatureStep`, `OxygenStep`, and `OceanTile` (which is *also* a subclass of `Tile`). Venus Next adds `VenusStep`. These count "steps", so for example when there are six occurrences of `TemperatureStep` in the game that means the temperature is -18 C.

You can pretty easily guess why the `TemperatureStep` class declaration includes the line `HAS MAX 19 This`. Nineteen temperature steps up from the starting point gets you to 8 degrees Celsius and there can never be any more than that.

These classes contain effects for track bonuses; for example `OxygenStep` says `This IF =8 This: TemperatureStep`. The effect is considered every single time oxygen is raised, and triggers only when oxygen reaches 8%.

The definition of the `GlobalParameter` Class includes the line `+This.`. Any gain/remove Instruction in PETS can be followed by a Quantifier: either `!` meaning "mandatory", `?` meaning "optional", or `.` meaning "to the extent possible". The `Component` Class sets a default gain Quantifier of `!`, because most things in the game are mandatory. This line in `GlobalParameter` says that all gains of `GlobalParameter` instances default to "if possible". If one can't be added due to hitting the maximum Limit, the Instruction will still be executable and simply do nothing. As you might guess if you know the TfM rules well, `CardResource` also has the same default.

### Maps

One `MarsMap` instance will exist, such as `HellasMap`, but it doesn't do much. The interesting part is the areas. Every area is its own component instance; these are singleton classes so one of each is automatically created before the game begins.

The created areas have names like `Hellas_1_1`, `Hellas_1_2`, etc. The coordinate system is easy to understand if you try the `map` command in the command-line REPL tool (`./rego`).

Each area has a supertype that is one of `RemoteArea`, `WaterArea`, `LandArea`, `VolcanicArea`, or `NoctisArea`. `VolcanicArea` itself has `LandArea` as a supertype, so volcanic areas count as land areas. `NoctisArea` is *not* a `LandArea` because it doesn't function like one.

All areas except for `RemoteArea`s have the supertype `MarsArea`, so that cards like `Martian Rails` can work, and so that tiles except `CityTile`s can be restricted to those areas.

Areas don't get created for maps you aren't using in that game. So for example if the board is Hellas then the requirement `MAX 0 VolcanicArea` evalutes to true. That's handy for Lava Tube Settlement: `CityTile<VolcanicArea> OR (MAX 0 VolcanicArea: CityTile)`.

### Tiles

`Tile` is declared as `ABSTRACT CLASS Tile<Area>` which gives it a dependency onto `Area`. This means no tile can ever exist without having a specific `Area` that it relates to. Of course, tiles that aren't on the board yet are treated as simply not existing.

Area, by the way, was declared with `HAS MAX 1 Tile<This>`. That's our first example of an *invariant*; the engine will ensure that no 2 distinct Tile instances will ever relate to the same Area.

As for tile subtypes, we mentioned `OceanTile`, but will get to the rest in the player-specific section below.

### Actions

Any component that makes actions available for possible selection extends the supertype `HasActions`; these includes the abstract classes `StandardAction`, `StandardProject`, and `ActionCard`.

Under the aspirational premise model, the first two are singleton types: each active concrete subtype in the game's class table, such as `Aquifer`, would automatically have an instance created before the game starts. Therefore if the user signals `UseAction<Aquifer>` it will be able to respond, bill the user 18 money and put an `OceanTile` instruction on the user's task queue.

### Phases

Once setup begins, exactly one Phase instance exists at all times: `SetupPhase`, `CorporationPhase`, `ResearchPhase`, `ProductionPhase`, etc. A signal called `End` triggers victory point payouts (it has such a short name because it has to be written on MANY cards!).

## Player stuff

Concrete classes called Player1, Player2, etc. will exist. The player owning the unique `StartToken` is the start player.

The abstract class these all subclass is `Player`, which is both an `Owner` and an `Actor`. `Anyone` is still useful because it reads better in the icon-grammar spelling `CityTile<Anyone>: PROD[1 MC]`. A solo opponent can be an `Owner` without being a `Player`.

### Owned

The `Owned` abstract type is extremely important. It has a dependency onto `Owner` (which `Player1` etc. all extend), meaning that every concrete instance of any `Owned` subclass must always know which owner it belongs to. Many, many component types have `Owned` as a direct or indirect supertype.

A simple example of an owned component type is `VictoryPoint`.

`TerraformRating` is a very simple class to understand; it looks like this:

```
CLASS TerraformRating {
    ProductionPhase: 1 MC
    End: VictoryPoint
}
```

When the `ProductionPhase` signal goes out, each occurence of `TerraformRating` generates 1 MC for its owner. Likewise when the `End` signal gets posted, each occurrence of `TR` generates a victory point. And that's all there is to terraform rating.

The `Owned-Owner` dependency is a regular component dependency just like any other in the game.

### OwnedTile

An abstract class `OwnedTile` extends both `Tile` and `Owned`. There are tests that ensure that no component ever extends both `Tile` and `Owned` without also extending `OwnedTile`. This lets us treat the latter as a *de facto* intersection type of the first two, which is useful.

The three kinds of tiles are `GreeneryTile`, `CityTile`, and `SpecialTile` (the last is abstract as each specific kind of tile extends it).

The most interesting thing about these component types is that they use *defaults*.

```
CLASS CityTile {
    DEFAULT +CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>
    ...
}
```

Any card, class declaration, etc. that writes the simple type expression `CityTile` without specifying a kind of `Area` will get this default type instead. But simply write, say, `CityTile<VolcanicArea>` or ` CityTile<LandArea(HAS 2 Neighbor<CityTile<Anyone>>)>` (as `UrbanizedArea` does), and the default is ignored.

### Resources

Resources are divided into `StandardResource` and `CardResource`. The latter has a dependency on a `CardFront`; the resource can't exist unless the card it goes on does first.

The standard resources are mostly boring except Energy has this:

```
CLASS Energy { ProductionPhase:: Heat FROM This }
```

We discussed the `Phase` types above. As much as possible, these types do nothing by themselves; they exist only to trigger instructions like this one. Every single Energy instance in the game will respond to that trigger by transmuting itself into Heat. The double colon ensures that it happens first before regular production.

### Cards

It took a while to realize that `CardBack` and `CardFront` should actually be completely different, unrelated types -- just like `CityTile` and `GreeneryTile` are, despite the fact that they also are two sides of the same physical component. `CardBack` is very uninteresting; the base game has the two subtypes `ProjectCard` and `CorporationCard`, and Prelude adds `PreludeCard`. That's about it. (Remember these things have no attributes either.)

The most important thing to understand about cards is that the engine supports only "follow mode". A client supplies the concrete history to process, including draws, reveals, discards, and plays, and the engine calculates the resulting state. It neither owns hidden information nor tries to authenticate that history against a separate physical or online game. Thus, if the client says that `EarthCatapult` was played, the engine applies that play and subtracts one generic `ProjectCard` from the hand. Canonical card data retains hidden search, selection, and reveal procedures in a source-level `CARDS` transform, but the current executable declarations neutralize them to the follow-mode outcomes supplied by the client.

Even with this simplification, the whole play-a-card process is a bit complex to go into here and now.

Cards can have three types of things "on" them, which all share the superclass `Cardbound`. These are `Tag`s, `CardResource`s, and `ActionUsedMarker`s. What these all have in common is that the `CardFront` must exist before they can, and if the `CardFront` ever went away they would have to as well. This is, of course, just how dependencies work in PETS.

`Cardbound` is an interesting case in that it is both `Owned`, and depends on a type (`CardFront`) which is also `Owned`. Its declaration repeats the `Owner` bound in both places, making the two owners always the same. Thus `Animal<Player2, Predators>` and `Animal<Predators<Player2>>` mean the same concrete type, while specifying different owners is invalid.

### PaymentMechanic

A few types are busily doing weird stuff behind the scenes to let you pay for stuff properly:
`Owed`, `Accept`, `Pay`, `PlayCard`, and `PlayTag`. The best way to understand what these are for is
to see how they are used in the bundle `cards.pets` files.

## TODO

* Once the aspirational premise model exists, explain how premise choices become immutable Module components.
