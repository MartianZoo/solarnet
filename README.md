# Solarnet

Solarnet is a work-in-progress open-source **game engine** for the superlative 2016 board game *[Terraforming Mars](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2)*, published by [FryxGames](http://fryxgames.se).

It's not a way to *play* the game. For that, see the [online open-source app](http://terraforming-mars.herokuapp.com) (please [buy a physical copy](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2) too!). There is also an official app for sale in the usual places, but I can't vouch for it.

This is a clean room project unrelated to those apps or to any other codebase.

## Intro video

This is my [first unrehearsed intro/demo](https://www.youtube.com/watch?v=btCLcFLvV2I). Try watching it on 2x!

## Why another game engine?

The game engine is the part of a game implementation whose job is to *know the rules* of the game perfectly. It is really just a big calculator. Given some game state, and some choice made by a player, the engine computes the game state that results (or else determines that the choice was illegal). It's the "pure logic" part of the game.

Inside the open-source app mentioned above is *already* an extremely accurate (>99.9%) game engine. And inside the official app mentioned above is already a game engine. So why do we need a third?

We don't! I just wanted to challenge myself to build one in a different way.

And that is: all the game components that have heterogeneous behaviors -- that is, cards, milestones, map areas, etc. -- are *[just data](/MartianZoo/solarnet/blob/main/canon/src/main/java/dev/martianzoo/tfm/canon/cards.json5)* and don't require custom programming (except for the ones that do). Instead, components are described using a specification language called **Pets**. Pets code can -- in concept, that is -- be converted into natural-language instructions, or even into the iconographic language you see on the printed cards. But, more relevant: these Pets strings are all Solarnet needs to read in order to actually execute the card correctly (and this part basically works today).

## Goals

My goals for Solarnet, in descending order:

1. Correctness -- I want it to *eventually* implement the game rules with absolute unswerving fidelity. I might never get there but I'm working hard at it.
2. Completeness -- over time I want to support every single published card, milestone, award, map, component, and officially sanctioned variant. This will take a long time and adding cards is not the priority at the moment (there are 365 of them already).
3. Simplicity -- (I'm trying hard to keep the Pets language as simple as I can. That may be surprising as you try to learn it.)
4. Composability -- I'm writing this as series of libraries that other TfM-related projects could theoretically use for other purposes. Currently the modules are "pets" (the core language and datatypes), "engine" (what executes the cards to update a game state), "repl" (the command-line interface), and "canon" (a data set containing the officially published cards and other components).

Please notice **what is not on this list**!

1. Performance -- this library is allowed to be slow as hell, and I don't care! At every turn I'm preferring more readable, more bug-proof code over fast code. I hope that with help we'll also have a faster engine one day (Aerobrake?), but if we do, we'll have Solarnet to parity-test it against. The best way to get the fast engine is to first be very very careful with the correctness of the slow engine.
2. Usability -- there is a command line "REPL" (read-evaluate-print loop) called REgo PLastics. Depending on your view it is either **not a user interface** or it is a **very, very bad user interface**. And I plan for it to always stay that way. You cannot actually play a whole game using it. But you'll be able to set up scenarios and find out what happens. It is just a small program to let you interact directly with the engine library.

## What could potentially get built around this?

_Who knows!?_ I can't wait to be surprised. But here are some thoughts I have.

* The same Pets format used by the engine should be convertible to icon grammar or to natural language. The idea of single-sourcing the data for all 3 purposes was what initially drew me into all this. It would be cool if from looking at a card you *know* how it has to work, because it could hardly do otherwise.
* Logfile analysis. Solarnet logs are a fully detailed record of the game, in a very analyzable form. For example, for any card, you could pretty easily find out how much that card actually "paid out" over the course of the game. It would be great to build a queryable database out of this stuff.
* I hope it will be good for training AI players. Since the cards are fully introspectable (i.e. code can use the Pets AST API directly), the AIs could even play games using cards they'd never seen before.
* Parity tests between it and any other engine out there. Whether that turns up bugs in this engine, that engine, or both, it'll be a useful activity.
* Fan cards and fan variants, of course... *eventually*. We must focus on the core stuff for now.
* The optimized engine alluded to above.

## The Pets language

There are several basic elements to the language.

* Instructions (`-2 Plant` means "lose two plants")
* Requirements (`MAX 4 TemperatureStep` means "is the temperature at most -22 C?")
* Metrics (`SpaceTag<!Me>` means "number of opponents' space tags", as in `PROD[1 / SpaceTag<!Me>]`) (well, the `!Me` is not yet implemented)
* Effects (`EventCard: 3` means "when you play an event card, gain 3 MC")
* Actions (`Steel -> 5` means "spend 1 steel to gain 5 MC")
* Complex component type expressions (`CityTile<LandArea(HAS 2 Neighbor<CityTile<Anyone>>)>` means "a city tile on a land area next to >=2 city tiles", no matter which of the above element kinds it appears in)
* ChangeEvents, aka game log entries (`469: +OxygenStep FOR Player2 BY GreeneryTile<Player2, Tharsis_5_5> BECAUSE 448`)
* Class declarations (`CLASS TerraformRating { ProductionPhase: 1; End: VictoryPoint }`)

## Learning more

I have started jotting some stuff down, but the written docs leave much to be desired.

* [FAQ](faq.md)
* Overview of [component types](docs/component-types.md) -- a good place to start
* Pets language [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md)
* API docs -- see below

## Interested in playing around with it?

If you have git and Java stuff working already, this should be all it takes to start messing around:

```
git clone https://github.com/MartianZoo/solarnet.git
cd solarnet
./rego
```

Of course, it won't work, because nothing ever works. I'd be *willing* to help, but... I actually know almost nothing about Build Stuff.

You can also browse the [Pets source files](/MartianZoo/solarnet/tree/main/canon/src/main/java/dev/martianzoo/tfm/canon) to see where all the game components come from. You can change it around or attempt to add your own custom cards.

## Want to poke around in the implementation code?

Note that the code contains many great examples of how not to code. It'll get better eventually.

It's best to start with the generated API doc view because it hides private things. It's not hosted yet, but you can `./gradlew dokkaHtmlMultiModule` and then look at `docs/api/index.html`.

It's all written in [Kotlin](https://kotlinlang.org). Trying Kotlin was a great decision.

## Who are you

http://kevinb9n.github.io

I'd really love to hear your thoughts and questions at kevinb9n@gmail.com or by any other means.
