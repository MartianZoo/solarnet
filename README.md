# Solarnet

Solarnet is a (work-in-progress) open-source game engine for the superlative 2016 board game *[Terraforming Mars](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2)*, published by [FryxGames](http://fryxgames.se).

It's not a way to *play* the game. For that, see the [online open-source app](http://terraforming-mars.herokuapp.com) -- and please make sure to [buy a physical copy](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2) of the game too! There is also an official app for sale in the usual places but I can't vouch for it.

This is a clean room project unrelated to those apps or to any other existing codebase.

## Intro video

This is my [first unrehearsed intro/demo](https://www.youtube.com/watch?v=btCLcFLvV2I). It'll get better.

## Disclaimers

This project is in an early-middle state of its development. It's buggy. It's not enforcing tile placement rules. Error messages are criminally unhelpful. The syntax and command-line UI are hard to figure out on your own. Much is not written yet. And the code has many stellar examples of how not to write code.

## Why another game engine?

The game engine is the part of a game implementation whose job is to *know the rules* of the game perfectly. It is really just a big calculator. Given some game state, and some choice made by a player, the engine computes the game state that results (or else determines that the choice was illegal).

Inside the open-source app mentioned above is *already* an extremely accurate (>99.9%) game engine. And inside the official app mentioned above is already a game engine. So why do we need a third?

Answer: we don't. I just wanted to see if I could do it.

I am taking a different approach, though. All the cards, milestones, etc. are *[just data](https://github.com/MartianZoo/solarnet/blob/main/canon/src/main/java/dev/martianzoo/tfm/canon/cards.json5)* and don't require custom programming (except for the ones that do). Instead these game components express their behaviors using a specification language called **Pets**. These expressions are readable by the game engine, but could also in theory be converted into natural-language instructions, or even into the iconographic language used by the printed cards. The eventual goal is that every card *must* do exactly what it says it does (in words and in icons), because it couldn't do otherwise.

But when it comes down to it, I don't know what this project will be useful for yet. Maybe it would be good for training AI players against. But maybe nothing.

## Goals

My goals for Solarnet, in descending order:

1. Correctness -- I want it to *eventually* implement the game rules with absolute unswerving fidelity. I might never get there but I'm working hard at it.
2. Completeness -- over time I want to support every single published card, milestone, award, map, component, and officially sanctioned variant. This will take a long time and adding cards is not the priority at the moment (there are 366 of them already).
3. Simplicity -- I'm trying to keep the Pets language as simple as I can.
4. Composability -- I'm writing this as series of libraries that other TfM-related projects could theoretically use for other purposes. Currently the modules are "pets" (the core language and datatypes), "engine" (what executes the cards to update a game state), "repl" (the command-line interface), and "canon" (a data set containing the officially published cards and other components).

Please notice **what is not on this list**!

1. Performance -- basically, it will be slow as hell and *I don't care*. I'm preferring the simpler, more bug-proof code over fast code every time. If I or someone builds a faster engine in the future (you might call it Aerobrake!), we will have Solarnet to parity-test it against. Therefore the best way to get to a fast engine is to first be as careful as possible with the correctness of this one.
2. Usability -- there is a command line "REPL" (read-evaluate-print loop) called REgo PLastics. Depending on your view it is either **not a user interface** or it is a **very, very bad user interface**. And I plan for it to always stay that way. You cannot actually play a whole game using it. But you'll be able to set up scenarios and find out what happens.

## What could potentially get built around this?

Who knows? But here are some thoughts I have.

* The same Pets format used by the engine should be convertible to icon grammar or to natural language. The idea of single-sourcing the data for all 3 purposes was what initially drew me into all this. It would be cool if from looking at a card you know how it *had* to behave.
* I think this library could be useful for training AI players against. It should help that the cards are introspectable -- the AI could play a card it's never seen before, and play it well, as long as it understands the different elements *on* the card.
* Logfile analysis. The logs produced by this engine have the full details on everything, in a form that should be very amenable to analysis. For example, for any card you'd be able to find out how much that card "paid out" over the course of the game.
* Parity testing against another engine.
* will add more another day

## The Pets language

There are several basic elements to the language.

* Instructions (`-2 Plant` means "lose two plants")
* Requirements (`MAX 4 TemperatureStep` means "is the temperature at most -22 C?")
* Metrics (`SpaceTag<!Me>` means "number of opponents' space tags", as in `PROD[1 / SpaceTag<!Me>]`)
* Effects (`EventCard: 3` means "when you play an event card, gain 3 MC")
* Actions (`Steel -> 5` means "spend 1 steel to gain 5 MC")
* Complex component type expressions (`CityTile<LandArea(HAS 2 Neighbor<CityTile<Anyone>>)>` means "a city tile on an area with at least 2 neighboring city tiles", no matter which of the above constructs it's used in)
* Production boxes (`PROD[2 Plant], Plant` means "increase plant production 2 steps and gain a plant")
* Change records (`13: OceanTile<Tharsis5_5> FOR Player3 BY AquiferSP; 14: 2 Plant<Player3> FOR Player3 BY ArcticAlgae<Player3> BECAUSE 13`)
* Class declarations (`CLASS TerraformRating { ProductionPhase: 1; End: VictoryPoint }`)

## Learning more

I have started jotting some stuff down, but the written docs leave much to be desired.

* [FAQ](faq.md)
* Overview of [component types](docs/component-types.md)
* Pets language [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md)
* API docs -- see below

## Interested in playing around with it?

If you have git and Java stuff working already this should be all it takes to start messing around:

```
git clone https://github.com/MartianZoo/solarnet.git
cd solarnet
./rego
```

Of course, it won't work, because nothing ever works. I'd be willing to help but I actually know very little about Build Stuff.

You can also look for the `*.pets` and `*.json5` files to see how game components get defined in the Pets language. You can change it around or attempt to add your own custom cards. **See the warnings at top!**

## Want to poke around in the implementation code?

The code isn't too pretty right now and I'm making big ugly changes to it.

It's best to start with the generated API doc view because it hides private things. It's not hosted yet, but you can `./gradlew dokkaHtmlMultiModule` and then look at `docs/api/index.html`.

It's all written in [Kotlin](https://kotlinlang.org). It's my first time using it and I'm extremely happy with that decision.

## Who are you

http://kevinb9n.github.io

I'd love to hear any thoughts or questions you have on this project at kevinb9n@gmail.com.
