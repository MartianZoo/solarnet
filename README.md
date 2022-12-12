# petaform

If you're here because you want to play the game, you're in the wrong place! [See this amazing app](http://github.com/bafolts/terraforming-mars), and I would also *strongly* encourage you to buy a copy of the physical game.

This project has no relationship with other apps.

## The Petaform language

Petaform is a specification language for game component behaviors in the board game Terraforming Mars, allowing components with heterogeneous behaviors (cards, milestones, maps, etc.) to be expressed as pure data.

This project is a library for parsing that language, and a data set of published cards with specifications expressed in the language.

The library is written in Kotlin, which is supposed to be able to translate to JavaScript or native code...

The name is stupid, it just comes from "1000 x teraform" and I also enjoy abbreviating it "Pets" whenever I want to.

### The goal

Petaform text can serve as the authoritative definition of a component's behaviors. It captures sufficient data that *in theory* it could do any of these things:

* Generate user-readable instruction text in a particular language (roughly matching the published text)
* Generate an iconographic depiction (for a card, map area, etc.) (roughly matching the published icons)
* Be executed correctly by an actual running game engine (goal: *exactly* matching the official rules)
* Test another game engine implementation against
* Be understood by an AI player, for whatever purposes

## What this project will eventually include

* A parser and runtime API for the Petaform language
* Data model and API for: game components, game configuration, game state, game history
* **Solarnet**, a reference engine
* **Rego Plastics**, an extremely bad command-line UI ("REPL") to Solarnet
* Potentially eventually, tools for generating text or iconographic representations from Petaform source

### Solarnet

Solarnet is a *reference engine*. A reference implementation has very different priorities from production software! These are, in order:

1. To implement the official game rules with absolute fidelity
2. To do so in as simple and understandable a manner as possible (making it hard for bugs to hide)
3. To cover as much of the official published content as possible

My goal is for Solarnet to *eventually* become a trustworthy authority on correct rules interpretation. For example, if a fan card is expressible in Petaform then this engine would provide a definitive answer on how that card should interact with canonical cards.

**But expect Solarnet to be slow af.** 

## Why do all this?

* Because it's really interesting
* Because it's really hard
* Because I think it will be cool
* Because I really love this game

## Building

`./gradlew build`
