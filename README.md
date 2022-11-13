# petaform

A specification language for game component behaviors in the board game Terraforming Mars, allowing components with heterogeneous behaviors (cards, milestones, maps, etc.) to be expressed as pure data.

Yes, the name just comes from 10^3 x "teraform". Dumb but it stuck.

## The goal

Petaform text can serve as a single source of truth for each component, with sufficient data to (in theory) do any of these things and more:

* Generate user-readable instruction text in some particular language
* Generate an iconographic depiction (for a card, map area, etc.)
* Be executed correctly by an actual running game engine
* Test another game engine implementation against
* Be understood by an AI player

## Expressions supported

* Instructions (`"2 Plant"` used as an instruction means to gain two plants)
* Predicates (`"3 OxygenStep"` used as a predicate determines whether the oxygen is 3% or higher)
* Triggered effects (`"SpaceTag: 2"` used as an effect means "when you play a space tag, gain 2 MC")
* Actions (`"Steel -> 5"` used as an effect means "as an action, spend 1 steel to gain 5 MC")
* Complex component types (`"Tile<MarsArea(neighbors HAS This)>"` means "a tile on an area neighboring the context component")

## What this project will include

* Parser and runtime API for the Petaform language
* Data model and API for: game components, game configuration, game state, game history
* Potentially, tools that generate text or iconographic representations
* **Solarnet**, a reference engine

### Solarnet

Solarnet is a *reference engine*. This has very different priorities from production software. These are, in descending order:

1. To implement the official game rules with absolute fidelity
2. To do so in as simple and understandable a manner as possible
3. To cover as much of the official published content as possible

My hope is that in time Solarnet can eventually become a useful "authority" on correct rules interpretation.

**But it will almost certainly be slow.** An optimized engine, with identical behavior but good performance, could potentially follow at some point. It will be able to test itself against the reference engine, which is a great thing. If someone wants to make this, please consider naming it Aerobrake.

## Building

`./gradlew build`
