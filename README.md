# Solarnet

## Fast facts

* Solarnet is a **work-in-progress** game engine for the amazing board game *[Terraforming Mars](https://boardgamegeek.com/boardgame/167791/terraforming-mars)*.

* It's not really for *playing* the game; for that see the excellent and unrelated [open-source app](http://github.com/terraforming-mars/terraforming-mars).

* It's a standalone library. Its only job is to *know the rules of the game* -- which means "who can do what when, and what happens if they do?" It covers the "pure logic part" of the game. You can set up game situations and see what happens ([example](https://github.com/MartianZoo/solarnet/blob/main/engine/src/test/java/dev/martianzoo/tfm/engine/cards/ExcentricSponsorTest.kt)).

* The unique behavior of a card, milestone, map area, colony tile, etc. is written in a language called Pets. These strings are ALL the game engine needs to know about a card (etc.) in order to play it correctly. Some examples:

| Class             | Example Pets syntax                                         |
|-------------------|-------------------------------------------------------------|
| `LargeConvoy`     | `This: OceanTile, 2 ProjectCard, 5 Plant OR 4 Animal`       |
| `ElectroCatapult` | `Plant OR Steel -> 7`                                       |
| `ArcticAlgae`     | `OceanTile BY Anyone: 2 Plant`                              |
| `Insulation`      | `This: PROD[X Megacredit FROM Heat]`                        |
| `EarthCatapult`   | `PlayCard: -2 Owed<Megacredit>`                             |
| `CitySP`          | `25 -> CityTile, PROD[1]`                                   |
| `TerraformRating` | `ProductionPhase: 1`; `End: VictoryPoint`                   |
| `CityTile`        | `End: VictoryPoint / Adjacency<This, GreeneryTile<Anyone>>` |

* This means you can add your own fan cards to it pretty easily and without actual "programming" -- so long as the cards don't introduce entirely new game mechanics.

* It has a crappy command-line UI (a "REPL") you can use to interact with it (see demo video below). Or you can write what you want to do as a unit test ([very long example that plays through an entire game](https://github.com/MartianZoo/solarnet/blob/main/engine/src/test/java/dev/martianzoo/tfm/engine/games/Game20230521Test.kt)).

* If you play a game IRL or on the app, you can sort of "log" it in Solarnet, and then be able to ask questions like "How much money did Advanced Alloys actually save me that game?" fairly easily. (But so far this requires writing actual code.)

* It works! See the Issues tab for exceptions. I have just about 400 cards working -- all [except these ones](https://github.com/MartianZoo/solarnet/blob/main/docs/cards-to-add.md).

* It's **not polished enough** for anyone to "just use" -- not even close! But if you're intrepid I could really use your help to *get* it into that state. If you're not in a "roll up your sleeves, dig in, ask questions" mode, you *will* find it frustrating. Sorry!

## Play around with it?

You totally can, but note: you will have a MUCH better time if you jump on the [discord](https://discord.gg/8tX5g4Yx3T) and ask lots of questions. Almost no one but me has tried to do much with it yet. It won't be self-explanatory (sorry).

It is *supposed* to be as simple to get going as:

```
git clone https://github.com/MartianZoo/solarnet.git
cd solarnet
./rego
help
```

But in these early days, you're unlikely to get far on your own. I want to improve that, but the best chance for that to happen is if YOU give things a try and tell me how it goes! Again, please do join the discord.

## Learning more

### Join the discord

Please do. [This invite](https://discord.gg/8tX5g4Yx3T) should work...

### Videos

None of this is polished or anything.

* [Aug 2023 demo](https://youtu.be/xaUOMUaWG7Q). This video is the most current. It's best for software type people who are also familiar with the game.
* [First overview and REPL demo](https://www.youtube.com/watch?v=btCLcFLvV2I). This also assumes some familiarity with the game, and also includes a demo, but it also has some introductory slides that explain a few things better. The demo part is somewhat outdated.
* [Second overview](https://www.youtube.com/watch?v=pds_Axz2T90). A presentation I gave April 2023 for an audience of more hardcore software people. Tried to make it a little more understandable for those who aren't experts in the game already.
* [Watch as I "log" a real game](https://youtu.be/se8svQH-GOE) (I explain a bunch of stuff, but it's long; watch on high-speed). 
* [Gory video](https://www.youtube.com/watch?v=jC4iZnv4UA0) of me add a brand new card (Supercapacitors) to Solarnet in about a half hour.

### Docs

I haven't written too much yet. There are too many things I could write down next, so it would really help to hear which topics you most want to know about and then I could just write about those (and *eventually* get around to everything).

* A [FAQ](docs/faq.md)
* [Cheat sheet](docs/cheat-sheet.md)
* Overview of [component types](docs/component-types.md) -- not a bad place to start
* Pets language [language intro](docs/language-intro.md) and [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md) (incomplete)
* API docs -- see below

### Browse?

Want to just [browse through](https://github.com/MartianZoo/solarnet/tree/main/canon/src/main/java/dev/martianzoo/tfm/canon) how the game components are defined?

Just breeze past all the things that don't make sense. Some of it will!

### Poke around in the implementation?

If you can generate the docs (clone, `./gradlew dokkaHtmlMultiModule`, then look at `docs/api/index.html`) that would be the ideal way to start.

I wrote it in [Kotlin](https://kotlinlang.org), which should make the whole thing usable from Java, JavaScript, and some other environments as well. Getting those working might not be trivial (?) but at least won't require a port.

## Who are you

http://kevinb9n.github.io

I'd be more than glad to hear from you at kevinb9n@gmail.com.
