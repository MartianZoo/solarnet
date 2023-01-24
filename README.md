# PETS and SolarNet

There are two separate projects in this repo, just for convenience while they are in rapid flux.

PETS is a specification language for representing game component behaviors in _Terraforming Mars_. It lets components
with heterogeneous behaviors (cards, milestones, maps, etc.) be expressed as pure data. This data can, in theory, be
used to render the card instructions as text, render them in iconographic form, or be used by an actual running game
engine.

SolarNet is a game engine that runs off of PETS specifications alone... kind of. More on this below.

## Important disclaimers!

* Think of the *potential* of what you see here and not so much the state it's already in.
* The REPL ("REgo PLastics") is a really, really, really bad user interface by design. Basically it's not trying to be a
  UI at all.
* Error handling is absolutely awful. No effort has gone into reporting errors in more useful ways. So if you are trying
  to learn the PETS language and do stuff with it, you may have a frustrating experience because the error messages
  aren't gonna help much.
* Solarnet disclaimers:

## Solarnet

Solarnet is a game engine. It's slightly more accurate to say it will be a game engine when it exists. My goals for it are, in order:

1. It'll handle any card/milestone/etc. you can write in valid PETS form
2. It'll implement the official game rules with absolute unswerving fidelity
3. It'll be implemented as simply and understandably as I can possibly make it (so it is hard for bugs to hide)

Now you might be wondering where the following goals went

4. It won't be slow as fuck
5. It won't be a gobsmacking pain in the ass to use

They don't exist because these things just aren't important right now. We can always make them better later. And the goal is NOT to make something that could be used in an actual running game. The most exciting thing to me would be if one of the existing apps would use it to parity-test against.

## Making it harder

The nerd alpha move is to make an engine that *doesn't even know anything about Terraforming Mars*. Obviously *all* the
game components should be written in PETS, not just the cards!

Here's `CityTile` in PETS

```
CLASS CityTile : OwnedTile {
    DEFAULT +CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>
    End: VictoryPoint / Adjacency<This, GreeneryTile<Anyone>>
}
```

Here's your turn actions

```
ABSTRACT CLASS StandardAction(HAS =1 This) {
    CLASS PlayCardFromHand   { -> PlayCard<ProjectCard, CardFront> }
    CLASS UseStandardProject { -> UseAction<StandardProject> }
    CLASS UseCardAction      { -> UseAction<ActionCard> THEN ActionUsedMarker<ActionCard> }
    CLASS ConvertPlants      { 8 Plant -> GreeneryTile }
    CLASS ConvertHeat        { 8 Heat -> TemperatureStep }
    CLASS SellPatents        { X ProjectCard -> X }
    CLASS ClaimMilestone     { 8 -> Milestone }
    CLASS FundAward          { 8, 6 / Award -> Award }
    CLASS TradeAction        { 3 Energy OR 3 Titanium OR 9 -> Trade }
}
```

`TerraformRating` is pretty easy to understand:

```
CLASS TerraformRating : Owned {
    ProductionPhase: 1
    End: VictoryPoint
}
```

Each `TerraformRating` instance you own will respond to the `ProductionPhase` signal by generating 1 megacredit for you,
and will also respond to the `End` phase by handing you a `VictoryPoint`. Simple as that! (Or, I could have made `TR` a
subclass of `VictoryPoint`; that would have worked too, but I decided to be a stickler about VPs not existing until the
end of the game.)

## Why are you doing this?

Purely for fun and learning. I don't have any specific ambitions for it. It's a toy.

## Why the name PETS

Who doesn't love pets?
