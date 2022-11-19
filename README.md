# petaform

(If you're here because you want to play the game, you're in the wrong place! [See this amazing app](http://github.com/bafolts/terraforming-mars).)

## The Petaform language

Petaform is a specification language for game component behaviors in the board game Terraforming Mars, allowing components with heterogeneous behaviors (cards, milestones, maps, etc.) to be expressed as pure data.

It is currently in a very early stage of development.

### The goal

Petaform text can serve as the authoritative definition of a component's behaviors. It captures sufficient data to -- in theory! -- do any of these things and more:

* Generate user-readable instruction text in a particular language (roughly matching the published text)
* Generate an iconographic depiction (for a card, map area, etc.) (roughly matching the published icons)
* Be executed correctly by an actual running game engine (exactly matching the published rules)
* Test another game engine implementation against
* Be understood by an AI player

### Expressions supported

* Instructions (`2 Plant` means "gain two plants")
* Predicates (`MAX 4 OxygenStep` means "is oxygen is at most 4%?")
* Effects (`SpaceTag: 2` means "when you play a space tag, gain 2 MC")
* Actions (`Steel -> 5` means "as an action, spend 1 steel to gain 5 MC")
* Complex component types (`Tile<MarsArea(neighbors HAS This)>` means "a tile on an area neighboring this one")

## What this project will include

* Parser and runtime API for the Petaform language
* Data model and API for: game components, game configuration, game state, game history
* Potentially, tools for generating text or iconographic representations from Petaform source
* **Solarnet**, a reference engine
* **Rego Plastics**, a very bad command-line UI ("REPL") to the reference engine

### Solarnet

Solarnet is a *reference engine*. A reference implementation has very different priorities from production software! Its priorities are, in descending order:

1. To implement the official game rules with absolute fidelity
2. To do so in as simple and understandable a manner as possible (making it hard for bugs to hide)
3. To cover as much of the official published content as possible

The long-term dream is that Solarnet becomes useful as a trustworthy authority on correct rules interpretation.

**But expect it to be slow af.** An optimized engine with good performance could potentially follow at some point. By parity-testing itself against the reference engine, we can prove that it is just as correct. If someone wants to make this, please name it Aerobrake. :-)

## Why?

Why are we doing all this?

* Because it's hard
* Because it's fun
* Because we're learning stuff
* Because the game is just that good

## Building

`./gradlew build`
