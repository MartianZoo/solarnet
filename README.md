# Solarnet

Solarnet is an open-source game engine for the board game *Terraforming Mars*.

It is not a way to *play* the game. For that, see the [online open-source app](http://terraforming-mars.herokuapp.com) (and please make sure to buy a physical copy of the game so the creators get paid!).

This project is unrelated to that app or any other codebase.

## Why another game engine?

I'm making Solarnet from scratch with a different approach. All the cards, milestones, etc. are *just data* and don't require custom programming. They express their behaviors using a custom language called Pets, which I've created just for this purpose.

I'm doing it for fun and to learn. I don't know what it will be useful for.

## Goals

My goals for Solarnet, in order:

1. Correctness -- I want to *eventually* implement the game rules with absolute unswerving fidelity. I might never get there (e.g. EcoExperts->Decomposers; that interaction is just plain weird.)
2. Completeness -- over time I want to support every single published card, milestone, award, map, component, and officially sanctioned variant. This will take a long time and is not a priority at the moment.
3. Simplicity -- my challenge is to implement it in as simple and understandable way as I can possibly imagine. I want not just for it to *work*, but for it to be clear *why* it works.
4. Composability -- I'm writing this as series of libraries that other Mars-related projects can use for other purposes; not just a single monolithic application. Currently the modules are "pets" (the core language and datatypes), "engine" (what executes the cards to update a game state), "repl" (the command-line interface), and "canon" (a data set to eventually contain all the published cards and other components).

But notice **what is not on this list**.

1. Performance -- basically, it will be slow as hell and I don't care. I can imagine profiling it every now and then when it gets too annoying. But in general, I am preferring clear and obviously-correct code over fast code every time. If I or someone builds a faster engine in the future (I would call it Aerobrake), we will have this engine to parity-test that one against, so I don't want to take any chances with its correctness.
2. Usability -- there is a command line "REPL" (read-evaluate-print loop) called REgo PLastics. It is a very, very, very bad user interface, and I plan for it to always stay that way. You cannot actually play a whole game using it.

## Learning more

I have started jotting some stuff down, but it leaves much to be desired.

* Overview of [component types](docs/component-types.md)
* Pets language [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md)
* API documentation is not hosted yet; you'd have to clone and type `./gradlew dokkaHtmlMultiModule` then look at `docs/api/index.html`.

## Interested in playing around with it?

Just clone and run `./repo` and type `help`. You can do a few things. It doesn't do very much yet. For example, you can add a Greenery tile to the board, but it won't yet trigger effects like raising oxygen. I'm getting there.

You can also look for the `*.pets` and `*.json5` files to see how the stuff is configured in the Pets language. You can change it around or add your own custom cards. But be warned: I have spent NO time on error handling, so the error messages you're gonna get will be incredibly unhelpful and frustrating. Sorry!

## Contact me?

Please feel more than welcome to send an email to kevinb9n@gmail.com if you find this interesting and want to talk about what we could do with it. 
