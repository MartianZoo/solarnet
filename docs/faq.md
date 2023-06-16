# FAQ

### What are the goals of this engine?

Solarnet's goals in descending order:

1. Correctness -- I want it to *eventually* implement the game rules with absolute fidelity. It's doing pretty well so far.
2. Completeness -- over time I want to support every single published card, milestone, award, map, component, and officially sanctioned variant. This will take a long time and adding cards is not the priority at the moment (I have 397 out of 455 working already).
3. Simplicity -- I'm trying to keep the Pets language (and the engine itself) as simple and elegant as I can. This will be a constant push-and-pull, though.
4. Composability -- I'm writing this as series of libraries that other TfM-related projects could theoretically use for other purposes. Currently the modules are "pets" (the core language and datatypes), "engine" (what executes the cards to update a game state), "repl" (the command-line interface), and "canon" (a data set containing the officially published cards and other components). At some point I plan to cleave off all the actually-TfM-specific parts of it into another separate module.

Please notice **what is not on this list**!

1. Performance -- it's okay if this library is slow, as long as running the unit tests doesn't annoy me too grotesquely. We should either make sure it is *very* well-tested before adding any even-slightly-risky optimizations, or actually leave it slow and create a *second* optimized engine (call it "Aerobrake"), which we can parity-test against Solarnet. In these ways we can get performance without risking correctness. But this is not even on my mind at all right now.
2. Usability -- there is a command line "REPL" (read-evaluate-print loop) called REgo PLastics. It is a **very, very bad user interface** and I plan for it to always stay that way. It is just a small program to let you interact directly with the engine library.

### Could this engine be used for other games?

Any board game with entirely discrete mechanics *can* be implemented on Solarnet, but it wouldn't necessarily be a good fit.

It's true that the game engine itself doesn't really know anything about plants, city tiles, action cards, etc. All that comes from the `.pets` and `.json5` files. Nevertheless, everything about the engine and the Pets language has designed toward TfM's peculiarities -- the deep mechanical nature of the game. I would expect most other games would probably feel "shoehorned in". Still, it might be worth doing that shoehorning anyway; I'm really not sure. The perfect candidate would be a game that relies heavily on triggered effects and... counting things. Again, not really a high priority.

### Why is using the REPL such a pain in the ass?

You're speaking directly to the engine API, and the engine is extremely low-level. It doesn't care about being easy to use.

### Where are the rest of the cards?

There are currently 397 out of 455 cards supported. The rest are listed at [cards-to-add](cards-to-add.md).

### Why no Turmoil?

Turmoil is completely doable but will be completely gross. I'm not in any hurry for it.

### Could I add my own fan cards?

That's part of the idea, for sure! However, a couple caveats:

* There's no actual provision for how to bring fan cards into the system, so for now you would just fork the project and edit the cards.json file. We can talk about a better way to do it, for sure.
* This will work fine if your fan cards remix existing game mechanics in new ways. If they do things further out of the ordinary you'd just have to write some custom code (like we have in `custom.kt`)... but if it's *further* out of the ordinary than that you might be out of luck. I don't plan on adding a feature unless some officially published card needs it. At some point we could maintain two forks though.

### What could potentially get built around this?

_Who knows!?_ I can't wait to be surprised. But here are some thoughts I have.

* The same Pets format used by the engine should be convertible to icon grammar or to natural language. The idea of single-sourcing the data for all 3 purposes was what initially drew me into all this. It would be cool if from looking at a card you *know* how it has to work, because it could hardly do otherwise.
* Logfile analysis. Solarnet logs are a fully detailed record of the game, in a very analyzable form. For example, for any card, you could pretty easily find out how much that card actually "paid out" over the course of the game. It would be great to build a queryable database out of this stuff.
* I hope it might be good for training AI players. Since the cards are fully introspectable (i.e. code can use the Pets AST API directly), the AIs could make immediate use of cards they'd never seen before.
* Parity tests between it and any other engine out there. Whether that turns up bugs in this engine, that engine, or both, it'll be a useful activity.
* Fan cards and fan variants, of course... *eventually*. We must focus on the core stuff for now.
* The optimized engine alluded to above.

### Can we please improve the error messages? They're almost mocking me.

Yeah. I try to improve them, but so far I've been the only user. The more other people are trying to use this the more effort I'll be putting into making that a better experience.

### What do the FryxFolk think of this project?

They haven't responded yet.

### Why is it in Kotlin?

Several reasons

* I needed to learn it for my job
* It interoperates well with Java, Javascript, and other things
* It's an awesome language
* IntelliJ IDEA is an incredible product

