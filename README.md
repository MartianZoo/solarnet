# Solarnet

## Fast facts

* Solarnet is a work-in-progress **game engine** for the amazing board game *[Terraforming Mars](https://boardgamegeek.com/boardgame/167791/terraforming-mars)*. There are a few really cool things about it.

* If you just want to *play* the game, there's an *excellent* [open-source app](http://github.com/terraforming-mars/terraforming-mars) for doing that. Solarnet is unrelated to that (but very grateful for it).

* It's "just" a standalone library. Its only job is to *know the rules of the game*: "who can do what when, and what happens if they do?" It covers the "pure logic part" of the game. You can use it to set up game situations and see what happens ([example](https://github.com/MartianZoo/solarnet/blob/main/engine/src/commonTest/kotlin/dev/martianzoo/tfm/engine/cards/ExcentricSponsorTest.kt)).

* The unique behavior of each card, milestone, map area, colony tile, etc. is written in a bespoke language called Pets. These strings are ALL the game engine needs to know about a card (etc.) in order to play it correctly. Some examples:

| Class             | Example Pets syntax                                         |
|-------------------|-------------------------------------------------------------|
| `LargeConvoy`     | `This: OceanTile, 2 ProjectCard, 5 Plant OR 4 Animal`       |
| `ElectroCatapult` | `Plant OR Steel -> 7`                                       |
| `ArcticAlgae`     | `OceanTile BY Anyone: 2 Plant`                              |
| `Insulation`      | `This: PROD[X Megacredit FROM Heat]`                        |
| `EarthCatapult`   | `PlayCard: -2 Owed<Megacredit>`                             |
| `CitySP`          | `25 -> CityTile, PROD[1]`                                   |
| `TerraformRating` | `ProductionPhase: 1`, `End: VictoryPoint`                   |
| `CityTile`        | `End: VictoryPoint / Adjacency<This, GreeneryTile<Anyone>>` |

* This means you can add your own fan cards to it pretty easily and without actual "programming" -- so long as the cards don't introduce entirely new game mechanics.

* It has a crappy command-line UI (a "REPL") you can use to interact with it (see demo video below). Or you can write what you want to do as a unit test ([very long example that plays through an entire game](https://github.com/MartianZoo/solarnet/blob/main/engine/src/commonTest/kotlin/dev/martianzoo/tfm/engine/games/Game20230521Test.kt)).

* If you play a game IRL or on the app, you can sort of "log" it in Solarnet, and then be able to ask questions like "How much money did Advanced Alloys actually save me that game?" fairly easily. For now that last part requires writing code. The other catch is that you would have to ban the expansions and individual cards from your game that Solarnet doesn't support yet.

* It works! See the Issues tab for exceptions. See [what is supported](https://github.com/MartianZoo/solarnet/blob/main/docs/what-is-supported.md).

* It's **not polished enough** for anyone to "just use". 2026 update: unless you are into agentic stuff like Codex or Claude Code; they figure it out quite easily.

## Play around with it?

`JAVA_HOME` needs to point to a JDK installation at version 17 or newer.  Then it is *supposed* to be as simple as this:

```
git clone https://github.com/MartianZoo/solarnet.git
cd solarnet
./rego
```

... and then type `help`.

You can also start a small REPL server with `./regoserve` and issue repl commands to it from the normal command line (`./rc 'repl command here'`).

## Learning more

### Videos

None of this is polished or anything.

* [Aug 2023 demo](https://youtu.be/xaUOMUaWG7Q). This video is the most current. It's best for software type people who are also familiar with the game.
* [First overview and REPL demo](https://www.youtube.com/watch?v=btCLcFLvV2I). This also assumes some familiarity with the game, and also includes a demo, but it also has some introductory slides that explain a few things better. The demo part is somewhat outdated.
* [Second overview](https://www.youtube.com/watch?v=pds_Axz2T90). A presentation I gave April 2023 for an audience of more hardcore software people. Tried to make it a little more understandable for those who aren't experts in the game already.
* [Watch as I "log" a real game](https://youtu.be/se8svQH-GOE) (I explain a bunch of stuff, but it's long; watch on high-speed). 

### Docs

I haven't written too much yet. There are too many things I could write down next, so it would really help to hear which topics you most want to know about and then I could just write about those (and *eventually* get around to everything).

* A [FAQ](docs/faq.md)
* [Cheat sheet](docs/cheat-sheet.md)
* Overview of [component types](docs/component-types.md) -- not a bad place to start
* Pets language [language intro](docs/language-intro.md) and [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md) (incomplete)
* [Testing and verification](docs/agents/TESTING.md)
* The growing project [glossary](glossary.md)
* API docs -- see below

### Browse?

Want to just [browse through](https://github.com/MartianZoo/solarnet/tree/main/canon/src/commonMain/resources/canon/bundles) how the game components are defined?

The cards are also here in [spreadsheet form](https://docs.google.com/spreadsheets/d/1A3Gt_X_0Y-6DodJNJN1C2pvy75zOnVEJgQN-dGITKDk/edit?usp=sharing) which can be easier to read.

Just breeze past all the things that don't make sense. Some of it will!

### Poke around in the implementation?

If you can generate the docs (clone, `./gradlew dokkaGenerateHtml`, then look at `docs/api/index.html`) that would be the ideal way to start. The generated site includes the API documentation for every Solarnet module.

I wrote it in [Kotlin](https://kotlinlang.org), which should make the whole thing usable from Java, JavaScript, and some other environments as well.

### Join the discord

There is a discord that I'd be happy to start regenerating invites for but there's nothing happening there currently.

## Who are you

http://kevinb9n.github.io

I'd be more than glad to hear from you at kevinb9n@gmail.com.
