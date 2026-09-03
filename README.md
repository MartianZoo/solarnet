# Solarnet

## Fast facts

* Solarnet is a game engine for the amazing board game *[Terraforming Mars](https://boardgamegeek.com/boardgame/167791/terraforming-mars)* as a standalone library. It's just a toy but there are a few really cool things about it.

* If you just want to *play* the game, use the existing [open-source app](http://github.com/terraforming-mars/terraforming-mars) hosted on [herokuapp](https://terraforming-mars.herokuapp.com). It is excellent; in fact comparing its behavior to Solarnet's is how I find most of my bugs.

* It's "just" a standalone library. Its only job is to *know the rules of the game*: "who can do what when, and what happens if they do?" It covers the "pure logic part" of the game. You can use it to set up game situations and see what happens.

* The unique behavior of each card, milestone, map area, colony tile, etc. is written in a language called Pets (no reason, I just like Pets). These strings are all the game engine needs to know about a component in order to play it correctly. Some examples:

| Class             | Example Pets syntax                                         |
|-------------------|-------------------------------------------------------------|
| `LargeConvoy`     | `This: OceanTile, 2 ProjectCard, 5 Plant OR 4 Animal`       |
| `ElectroCatapult` | `Plant -> 7 MC`, `Steel -> 7 MC`                            |
| `ArcticAlgae`     | `OceanTile BY Anyone: 2 Plant`                              |
| `Insulation`      | `This: PROD[X MC FROM Heat]`                                |
| `EarthCatapult`   | `PlayCard:: -2 Owed<MC>`                              |
| `TerraformRating` | `ProductionPhase: MC`, `End: VictoryPoint`                  |
| `CitySP`        | `25 MC -> CityTile<>, PROD[MC]`                             |
| `CityTile`        | `End: VictoryPoint / Adjacency<This, GreeneryTile<Anyone>>` |

* This means you can add your own fan cards to it pretty easily and without actual "programming" -- so long as the cards don't introduce entirely new game mechanics.

* It has a crappy command-line UI (a "REPL") you can use to interact with it (see demo video below). Or you can write what you want to do as a unit test ([very long example that plays through an entire game](https://github.com/MartianZoo/solarnet/blob/main/test/common/dev/martianzoo/tfm/tests/replays/Game20230521Test.kt)).

* If you play a game IRL or on the app, you can sort of "log" it in Solarnet, and then be able to ask questions like "How much money did Advanced Alloys actually save me that game?" fairly easily. For now that last part requires writing code. The other catch is that you would have to ban the expansions and individual cards from your game that Solarnet doesn't support yet.

* It works! See [what is supported](https://github.com/MartianZoo/solarnet/blob/main/docs/what-is-supported.md).

## Play around with it?

It is *supposed* to be as simple as this:

```
git clone https://github.com/MartianZoo/solarnet.git
cd solarnet
JAVA_HOME=<home of a JDK 17 or newer>
./rego
help
```

I'll be honest, it's not suuper easy to use yet, and you'll have a LOT more success if you let Codex or Claude Code help you -- they learn it quite easily.

## Learning more

### Videos

None of this is polished or anything.

* [Aug 2023 demo](https://youtu.be/xaUOMUaWG7Q). This video is the most current. It's best for software type people who are also familiar with the game.
* [First overview and REPL demo](https://www.youtube.com/watch?v=btCLcFLvV2I). This also assumes some familiarity with the game, and also includes a demo, but it also has some introductory slides that explain a few things better. The demo part is somewhat outdated.
* [Second overview](https://www.youtube.com/watch?v=pds_Axz2T90). A presentation I gave April 2023 for an audience of more hardcore software people. Tried to make it a little more understandable for those who aren't experts in the game already.
* [Watch as I "log" a real game](https://youtu.be/se8svQH-GOE) (I explain a bunch of stuff, but it's long; watch on high-speed). 

### Docs

I haven't written too much yet.

* A [FAQ](docs/faq.md)
* [Cheat sheet](docs/cheat-sheet.md)
* Overview of [component types](docs/component-types.md) -- not a bad place to start
* Pets language [language intro](docs/language-intro.md) and [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md) (incomplete)
* The growing project [glossary](docs/glossary.md)
* API docs -- see below

There is also a `docs/agents` directory, but that's where agents capture information for themselves to read later. A human didn't write it and we don't expect humans to read it; I can't personally vouch for the information there.

### Browse?

Want to just [browse through](https://github.com/MartianZoo/solarnet/tree/main/src/common/dev/martianzoo/tfm/canon) how the game components are defined?

The cards are also here in [spreadsheet form](https://docs.google.com/spreadsheets/d/1A3Gt_X_0Y-6DodJNJN1C2pvy75zOnVEJgQN-dGITKDk/edit?usp=sharing) which can be easier to read.

Just breeze past all the things that don't make sense. Some of it will!

### Poke around in the implementation?

If you can generate the docs (clone, `./gradlew dokkaGenerateHtml`, then look at `docs/api/index.html`) that would be the ideal way to start. The generated site includes the API documentation for every Solarnet module.

I wrote it in [Kotlin](https://kotlinlang.org), which makes the whole thing equally usable from Java, JavaScript, and some other environments as well.

### Join the discord

There is a discord that I'd be happy to start regenerating invites for but there's nothing happening there currently.

## Who are you

http://kevinb9n.github.io

I'd be more than glad to hear from you at kevinb9n@gmail.com.
