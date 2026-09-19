# FAQ

### What are the goals of this engine?

The goals fall into tiers rather than a strict ranking within each tier.

The defining tier is exceptional software and language design: a small, precise semantic model;
ordinary game meaning expressed through Pets rather than card-specific orchestration; and one source
capable of driving execution, natural language, iconography, and analysis. Exact source-backed
replays are indispensable evidence that this model actually works across a whole game. The most
pressing current design problem is making actions and payments one intelligible lifecycle.

I also value an independent executable conformance suite, excellent diagnostics for incorrect Pets,
generative testing, material performance improvements, and a polished explanation of the project.
These are not where I intend to spend substantial time now.

Broad official-card completeness, general reuse, autonomous physical-deck play, fan material, AI
players, and a comprehensive analytics product would all be nice. I do not care enough about them
to distort the core design or make them current projects.

The REgo PLastics command-line interface is intentionally just a direct way to interact with the
low-level engine; turning it into a polished player interface is not a goal.

### Could this engine be used for other games?

Any board game with entirely discrete mechanics *can* be implemented on Solarnet, but it wouldn't necessarily be a good fit.

It's true that the game engine itself doesn't really know anything about plants, city tiles, action cards, etc. All that comes from the `.pets` and `.json5` files. Nevertheless, everything about the engine and the Pets language has designed toward TfM's peculiarities -- the deep mechanical nature of the game. I would expect most other games would probably feel "shoehorned in". Still, it might be worth doing that shoehorning anyway; I'm really not sure. The perfect candidate would be a game that relies heavily on triggered effects and... counting things. Again, not really a high priority.

### Why is using the REPL such a pain in the ass?

You're speaking directly to the engine API, and the engine is extremely low-level. It doesn't care about being easy to use.

### Where are the rest of the cards?

There are currently over 450 cards supported. The full inventory and remaining card and non-card components are listed at [what is supported](what-is-supported.md).

### Does Solarnet use any house rules?

Only for a few exceptionally minor edge cases. They are listed under the [supported game variant](what-is-supported.md#supported-game-variant).

### Why no Turmoil?

Turmoil is completely doable but will be completely gross. I'm not in any hurry for it.

### Could I add my own fan cards?

That's part of the idea, for sure! However, a couple caveats:

* There's no user-facing provision for bringing fan cards into the system, so for now you would fork the project and edit a bundle's `cards.json5` file. The build generates the corresponding Pets declarations. We can talk about a better way to do it, for sure.
* This will work fine if your fan cards remix existing game mechanics. Further-out behavior may
  need a bounded custom instruction, metric, or class after a genuine attempt to express it in
  ordinary Pets. A new general feature is not justified merely because one published or fan card
  needs it.

### What could potentially get built around this?

_Who knows!?_ I can't wait to be surprised. The first item is central to Solarnet itself; the rest
are lower-priority possibilities around it.

* The same semantic source should drive execution, icon grammar, natural language, and analysis.
  This single-sourcing was what initially drew me into all this. It would be cool if from looking at
  a card you *know* how it has to work, because it could hardly do otherwise.
* Logfile analysis. Solarnet logs are a fully detailed record of the game, in a very analyzable form. For example, for any card, you could pretty easily find out how much that card actually "paid out" over the course of the game. It would be great to build a queryable database out of this stuff.
* I hope it might be good for training AI players. Since the cards are fully introspectable (i.e. code can use the Pets AST API directly), the AIs could make immediate use of cards they'd never seen before.
* Parity tests between it and any other engine out there. Whether that turns up bugs in this engine, that engine, or both, it'll be a useful activity.
* Fan cards and fan variants, of course... *eventually*. We must focus on the core stuff for now.
* The optimized engine alluded to above.

### Can we please improve the error messages? They're almost mocking me.

Yes. A parser's responsibility is not merely to accept correct Pets; it should precisely highlight
why incorrect Pets is incorrect, especially for commonly encountered mistakes. I value that highly,
but it is not a current program of work.

### What do the FryxFolk think of this project?

They haven't responded yet.

### Why is it in Kotlin?

Several reasons

* I needed to learn it for my job
* It interoperates well with Java, Javascript, and other things
* It's an awesome language
* IntelliJ IDEA is an incredible product
