# FAQ

### What are the goals of this engine?

My goals for Solarnet, in descending order:

1. Correctness -- I want it to *eventually* implement the game rules with absolute unswerving fidelity. I might never get there but I'm working hard at it.
2. Completeness -- over time I want to support every single published card, milestone, award, map, component, and officially sanctioned variant. This will take a long time and adding cards is not the priority at the moment (there are 365 of them already).
3. Simplicity -- (I'm trying hard to keep the Pets language as simple as I can. That may be surprising as you try to learn it.)
4. Composability -- I'm writing this as series of libraries that other TfM-related projects could theoretically use for other purposes. Currently the modules are "pets" (the core language and datatypes), "engine" (what executes the cards to update a game state), "repl" (the command-line interface), and "canon" (a data set containing the officially published cards and other components).

Please notice **what is not on this list**!

1. Performance -- this library is allowed to be slow as hell, and I don't care! At every turn I'm preferring more readable, more bug-proof code over fast code. I hope that with help we'll also have a faster engine one day (Aerobrake?), but if we do, we'll have Solarnet to parity-test it against. The best way to get the fast engine is to first be very very careful with the correctness of the slow engine.
2. Usability -- there is a command line "REPL" (read-evaluate-print loop) called REgo PLastics. Depending on your view it is either **not a user interface** or it is a **very, very bad user interface**. And I plan for it to always stay that way. You cannot actually play a whole game using it. But you'll be able to set up scenarios and find out what happens. It is just a small program to let you interact directly with the engine library.

### Could this engine be used for other games?

Maybe, but I'm not sure it would be a good fit.

It's true that the game engine itself doesn't really know anything about plants, city tiles, action cards, etc. All that comes from `components.pets` and `player.pets`. There is a lot of custom code for converting data in the .json files into class declarations, but you just wouldn't use that.

Nevertheless, everything about the engine and the Pets language is designed toward TfM's peculiarities. The deep mechanical nature of the game. I would expect most any other game would feel shoehorned in. Except perhaps _TfM: Ares Expedition_.

The perfect candidate would be a game that relies heavily on triggered effects and... counting things. However, I still wouldn't recommend trying this anytime soon. Maybe in 2024.

### Why is using the REPL such a pain in the ass?

You're speaking directly to the engine API, and the engine is extremely low-level. It doesn't care about being easy to use.

### Where are the rest of the cards?

There are only about 365 cards supported. There should be issues filed about most of the rest.

### Why no Colonies?

It actually looks surprisingly easy to add. We just have bigger problems, is all.

### Why no Turmoil?

It'll be a bit of a monster to add. But totally doable.

### Could I add my own fan cards?

That's part of the idea, for sure! However, a couple caveats:

* There's no actual provision for how to bring fan cards into the system, so for now you would just fork the project and edit the cards.json file. We can talk about a better way to do it, for sure.
* If your fan card mixes existing game mechanics in different ways you'll probably be fine, but if it does things far enough out of the ordinary that the engine/language doesn't support it, I'm not going to be inclined to add the features you want. It's so much more important to focus on getting the published cards working.

### What could potentially get built around this?

_Who knows!?_ I can't wait to be surprised. But here are some thoughts I have.

* The same Pets format used by the engine should be convertible to icon grammar or to natural language. The idea of single-sourcing the data for all 3 purposes was what initially drew me into all this. It would be cool if from looking at a card you *know* how it has to work, because it could hardly do otherwise.
* Logfile analysis. Solarnet logs are a fully detailed record of the game, in a very analyzable form. For example, for any card, you could pretty easily find out how much that card actually "paid out" over the course of the game. It would be great to build a queryable database out of this stuff.
* I hope it might be good for training AI players. Since the cards are fully introspectable (i.e. code can use the Pets AST API directly), the AIs could make immediate use of cards they'd never seen before.
* Parity tests between it and any other engine out there. Whether that turns up bugs in this engine, that engine, or both, it'll be a useful activity.
* Fan cards and fan variants, of course... *eventually*. We must focus on the core stuff for now.
* The optimized engine alluded to above.

### Can we please improve the error messages? They're almost mocking me.

Yeah. The more interest I hear in people messing around with this thing, the more effort I'll put into those error messages.

### Why do I see code doing things in such absurdly slow ways?

Right now, as long as I can type stuff into the REPL and not be annoyed at the slowness of the response, there's no problem to solve. I also care a lot about keeping the code as easy to understand as possible, since the whole thing is so damned complicated by nature.

### What do the FryxFolk think of this project?

I'll let you know if they respond.

### Why is it in Kotlin?

Several reasons

* I needed to learn it for my job
* It interoperates well with Java, Javascript, and other things
* It's an awesome language
* IntelliJ IDEA is an incredible product
* 
