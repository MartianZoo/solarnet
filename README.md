# Solarnet

Solarnet is an open-source game engine for the superlative 2016 board game *[Terraforming Mars](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2)*, published by [FryxGames](http://fryxgames.se).

It is not a way to *play* the game. For that, see the [online open-source app](http://terraforming-mars.herokuapp.com) -- and please make sure to [buy a physical copy](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2) of the game so the creators get paid! (The *official app* for the game, sadly, cannot be recommended.)

This project is unrelated to those apps or any other codebase.

## Why another game engine?

The game engine is the part of a game whose job is to *know the rules* of the game perfectly. Given a game state and a choice made by a player, it computes the game state that results (or determines that the choice was illegal).

Inside the open-source app mentioned above is *already* an *extremely* accurate game engine. And inside the official app mentioned above is already a game engine. So why do we need a third?

It would be enough for me to just say "because I found it a fun challenge". But I am trying to build this with a unique approach. All the cards, milestones, etc. are *[just data](https://github.com/MartianZoo/solarnet/blob/main/canon/src/main/java/dev/martianzoo/tfm/canon/cards.json5)* and don't require custom programming (except for the ones that do). They express their behaviors using a specification language called Pets, created just for this purpose. Expressions of card behaviors written in Pets are also suitable for conversion into natural-language instructions or even the iconographic language used by the printed cards. In theory, we can eventually get to a situation where every card *must* do exactly what it says it does (in words and in icons), because it couldn't do otherwise.

But when it comes down to it, I don't know what this project will be useful for.

## Goals

My goals for Solarnet, in descending order:

1. Correctness -- I want it to *eventually* implement the game rules with absolute unswerving fidelity. I might never get there but I'll probably only abandon the attempt by inadvertently dying early.
2. Completeness -- over time I want to support every single published card, milestone, award, map, component, and officially sanctioned variant. This will take a long time and is not the priority at the moment; I do already have about 300 cards parsing correctly.
3. Simplicity -- my challenge is to make the implementation as simple and understandable as I can possibly manage. I don't just want it to *work*, I want it to be as clear as possible about *why* it works. Of course it is absolutely unavoidable that I have to make gruesome hacks here and there, but I'll keep trying to figure out how to undo them.
4. Composability -- I'm writing this as series of libraries that other TfM-related projects could theoretically use for other purposes -- not just a single monolithic application. Currently the modules are "pets" (the core language and datatypes), "engine" (what executes the cards to update a game state), "repl" (the command-line interface), and "canon" (a data set containing the officially published cards and other components).

Please notice **what is not on this list**!

1. Performance -- basically, it will be slow as hell and *I don't care*. I'm preferring the clearer, simpler, more bug-proof code over fast code every time. If I or someone builds a faster engine in the future (I would call it Aerobrake), we will have Solarnet to parity-test it against. Therefore the best way to get to a fast engine is to take *no chances* with the correctness of this one.
2. Usability -- there is a command line "REPL" (read-evaluate-print loop) called REgo PLastics. It is a **very, very, very bad user interface**, and I plan for it to always stay that way. You cannot actually play a whole game using it. But you'll be able to set up scenarios and find out what happens.

## Learning more

I have started jotting some stuff down, but the written docs leave much to be desired.

* Overview of [component types](docs/component-types.md)
* Pets language [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md)

## Interested in playing around with it?

Just clone and run `./rego` and type `help`. You can do a few things. Not much. For example, you can add a GreeneryTile to the board, but it won't yet trigger effects like adding an `OxygenStep`. If you `desc GreeneryTile` you can see that it knows it's supposed to. It just doesn't. I'm getting there.

You can also look for the `*.pets` and `*.json5` files to see how game components get defined in the Pets language. You can change it around or attempt to add your own custom cards. But be warned: I have spent NO time on error handling, so the error messages you're gonna get will be **incredibly unhelpful and frustrating**. Sorry! I plan to improve this at some point.

## Want to poke around in the implementation code?

First get an overview from the API documentation. It's not hosted yet, but you can `./gradlew dokkaHtmlMultiModule` and then look at `docs/api/index.html`.

It's written in [Kotlin](https://kotlinlang.org). It's the first Kotlin I've written and I'm very happy with the language. If you are already comfortable in Java, it's not too hard to learn the basics (and I don't really use the most advanced features).

## Why

Why is the language called Pets? First, who doesn't love Pets? But also, it originated from me thinking I might have played the game 1000 times by now... 1000 "teraforms" would be one "petaform" and it went from there.

Why is the command line UI called REgo PLastics? Because REPL is a term and Rego Plastics is a card...

Why is the engine called Solarnet? Who knows, it sounded cool.

## Contact me?

If you find this project interesting and want to talk about it, please send mail to kevinb9n@gmail.com. I'd be more than happy to get it.
