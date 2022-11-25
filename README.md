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
* Produce game logs that are ripe for statistical analysis

### Language elements

These are context-sensitive -- the same text might represent different types depending on where it appears.

* Instructions (`-2 Plant` means "lose two plants")
* Predicates (`MAX 4 OxygenStep` means "is oxygen is at most 4%?")
* Metrics (`SpaceTag<!Me>` means "number of opponents' space tags", as in `PROD[1 / SpaceTag<!Me>]`)
* Effects (`EventCard: 3` means "when you play an event card, gain 3 MC")
* Actions (`Steel -> 5` means "spend 1 steel to gain 5 MC")
* Complex component type expressions (`CityTile<LandArea(HAS 2 Neighbor<CityTile<Anyone>>)>` means "a city tile on an area with 2 neighboring city tiles", no matter which of the above constructs it's used in)
* Production boxes (`PROD[2 Plant], Plant` means "increase plant production 2 steps and gain a plant")
* Change records (`13: OceanTile<Tharsis5_5> BY Aquifer; 14: 2 Plant<Player3> BY Card023<Player3> BECAUSE 13`)

## What this project will eventually include

* A parser and runtime API for the Petaform language
* Data model and API for: game components, game configuration, game state, game history
* Potentially, tools for generating text or iconographic representations from Petaform source
* **Solarnet**, a reference engine
* **Aerobrake**, an optimized engine (parity-tested against Solarnet) (very low priority right now)
* **Rego Plastics**, an extremely bad command-line UI ("REPL") to either engine

### Solarnet

Solarnet is a *reference engine*. A reference implementation has very different priorities from production software! These are, in order:

1. To implement the official game rules with absolute fidelity
2. To do so in as simple and understandable a manner as possible (making it hard for bugs to hide)
3. To cover as much of the official published content as possible

Solarnet should eventually become a trustworthy authority on correct rules interpretation. For example, if a fan card is expressible in Petaform then this engine would provide a definitive answer on how that card should interact with canonical cards.

**But expect Solarnet to be slow af.** 

## Why do all this?

* Because it's hard
* Because it's interesting
* Because I think it will be cool
* Because I really love this game

## Building

`./gradlew build`
