# Solarnet

Solarnet is a (work-in-progress) open-source game engine for the superlative 2016 board game *[Terraforming Mars](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2)*, published by [FryxGames](http://fryxgames.se).

It is not a way to *play* the game. (Good lord, a person would go mad.) For that, see the [online open-source app](http://terraforming-mars.herokuapp.com) -- and please make sure to [buy a physical copy](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2) of the game too! There is also an official app for sale in the usual places but I can't vouch for it.

This is a clean room project unrelated to those apps or to any other existing codebase.

## Disclaimer

This project is very much not ready for prime-time. If you're intrepid, you can certainly play around with it -- it does a fair amount of stuff already. But it is **not ready for prime-time** at all. It's so not ready for prime-time that I didn't even realize I said that twice.

Warnings:

* It's buggy
* A few basic things like placing a city tile don't work yet
* Error messages are criminally unhelpful
* The command-line UI is hard to figure out
* Very little of what you'd need to know is written down
* The code has many stellar examples of how not to write code

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

## The Pets language

There are several basic elements to the language.

* Instructions (`-2 Plant` means "lose two plants")
* Requirements (`MAX 4 TemperatureStep` means "is the temperature at most -22 C?")
* Metrics (`SpaceTag<!Me>` means "number of opponents' space tags", as in `PROD[1 / SpaceTag<!Me>]`)
* Effects (`EventCard: 3` means "when you play an event card, gain 3 MC")
* Actions (`Steel -> 5` means "spend 1 steel to gain 5 MC")
* Complex component type expressions (`CityTile<LandArea(HAS 2 Neighbor<CityTile<Anyone>>)>` means "a city tile on an area with at least 2 neighboring city tiles", no matter which of the above constructs it's used in)
* Production boxes (`PROD[2 Plant], Plant` means "increase plant production 2 steps and gain a plant")
* Change records (`13: OceanTile<Tharsis5_5> FOR Player3 BY Aquifer; 14: 2 Plant<Player3> FOR Player 3 BY Card023<Player3> BECAUSE 13`)

## Learning more

I have started jotting some stuff down, but the written docs leave much to be desired.

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

You can also look for the `*.pets` and `*.json5` files to see how game components get defined in the Pets language. You can change it around or attempt to add your own custom cards. **See the warnings at top!**

## Want to poke around in the implementation code?

The code isn't too pretty right now and I'm making big ugly changes to it.

It'd be better to start with the generated API doc view just because it hides the private things. It's not hosted yet, but you can `./gradlew dokkaHtmlMultiModule` and then look at `docs/api/index.html`.

It's all written in [Kotlin](https://kotlinlang.org). It's my first time using it and I'm extremely happy with that decision.

## Who are you

http://kevinb9n.github.io

I'd love to hear any thoughts on this project at kevinb9n@gmail.com.]
