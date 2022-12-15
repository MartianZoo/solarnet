# PETS

PETS is a simple specification language for representing game component behaviors in _Terraforming Mars_. It lets components with heterogeneous behaviors (cards, milestones, maps, etc.) be expressed as pure data. The idea is that we could generate both the *iconographic* and *natural-language* instructions on a card from the same single source. Nifty huh?

Here's Freyja Biodomes. Okay, it looks like a block of JSON data, yeah, but the parts inside the immediate/effects/requirement sections are each written in PETS:

```json
{
    "id": "227",
    "bundle": "V",
    "deck": "PROJECT",
    "tags": [ "VenusTag", "PlantTag" ],
    "immediate": [
        "2 Microbe<CardFront(HAS VenusTag)> OR 2 Animal<CardFront(HAS VenusTag)>",
        "PROD[-Energy, 2]"
    ],
    "effects": [ "End: VictoryPoint" ],
    "requirement": "5 VenusStep",
    "cost": 14,
    "projectKind": "AUTOMATED"
},
```

I've got the parser parsing [350 cards](https://github.com/MartianZoo/pets/blob/main/pets/src/main/kotlin/dev/martianzoo/tfm/canon/cards.json5) so far.

## So where do you convert that to text or images then

Okay so I haven't done that part. Because what happened was:

I instead became obsessed with the idea that it should be possible to write a **game engine** that knows how to **actually play the cards** correctly based on this data alone. That would be cool for a bunch of reasons, but in particular, without that, I can't really prove that the PETS language is expressive enough. Who's to say whether Freyja Biodomes up there is even written correctly or not? But with an engine, I can write tests!

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

Of course, the nerd alpha move is to make an engine that *doesn't even know anything about Terraforming Mars*. Obviously *all* the game components should be written in PETS, not just the cards!

Here's `CityTile` in PETS

```
class CityTile {
    default CityTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>
    End: VictoryPoint / Adjacency(HAS This, GreeneryTile<Anyone>)
}
```

Here's your turn actions

```
abstract class StandardAction(HAS =1 This) {
    class PlayCardFromHand   { -> PlayCard<CardFront> }
    class UseStandardProject { -> UseAction<StandardProject> }
    class UseCardAction      { -> UseAction<CardFront> THEN ActionUsed<CardFront> }
    class ConvertPlants      { 8 Plant -> GreeneryTile }
    class ConvertHeat        { 8 Heat -> TemperatureStep }
    class ClaimMilestone     { 8 -> Milestone }
    class FundAward          { 8, 6 / Award -> Award }
    class TradeAction        { 3 Energy OR 3 Titanium OR 9 -> Trade }
    class SendDelegate       { LobbyDelegate -> Delegate; 5 -> Delegate }
}
```

I don't know why but the simplicity of `TerraformRating` brings a special joy

```
class TerraformRating : Owned<Player> {
    ProductionPhase: 1
    End: VictoryPoint
}
```

Note that *why* these formulations work will take more explaining. I believe PETS is relatively simple, but it definitely takes a minute to twist your brain into its way of thinking. I'll work on explaining everything as time permits...

## Why do all this?

* Because it's interesting
* Because it's really hard
* Because I think it will be cool
* Because for some reason I just love this game that much

## Why the name PETS

Who doesn't love pets?
