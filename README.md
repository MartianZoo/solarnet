# Solarnet

* Solarnet is a work-in-progress game engine library for the amazing board game *[Terraforming Mars](https://boardgamegeek.com/boardgame/167791/terraforming-mars)*.

* It's not a way to *play* the game; for that see the excellent [open-source app](http://terraforming-mars.herokuapp.com) (which this is unrelated to).

* Its only job is to *know the rules of the game*: who can do what when, and what happens if they do? It covers the "pure logic part" of the game.

* Card behaviors etc. are written in a (usually-)simple language called Pets that resembles the icon language on the printed cards. Examples:

| Class | Example Pets syntax |
| ----- | ------------------- |
| `LargeConvoy` | `This: OceanTile, 2 ProjectCard, 5 Plant OR 4 Animal` |
| `ElectroCatapult` | `Plant OR Steel -> 7` |
| `ArcticAlgae` | `OceanTile BY Anyone: 2 Plant` |
| `Insulation` | `This: PROD[X Megacredit FROM Heat]` |
| `CitySP` | `25 -> CityTile, PROD[1]` |
| `TerraformRating` | `ProductionPhase: 1`; `End: VictoryPoint` |
| `CityTile` | `End: VictoryPoint / Adjacency<This, GreeneryTile<Anyone>>` |

* This means you can "easily" add your own fan cards to it, particularly if they only remix existing game mechanics in new ways. (I'd be reluctant to add features needed *only* by fan cards, but you can also "shell out" to custom code when you need to.)

* It has a crappy command-line UI (a "REPL") you can use to interact with it, or you can write what you want to do as a unit test.

* The engine has some smarts. It lets you perform your turn effects in any order (per the game rules), but it can tell if there's only one task you could do next, and it can tell when you actually need to make a choice about something and when you don't.

* If you play a game IRL or on the app, you can sort of "log" it in Solarnet, and then be able to ask all kinds of questions like "How much money did AdvancedAlloys actually save me that game?"

* It works! See the Issues tab for exceptions. At the moment there are almost 400 cards working, including most of each expansion but Turmoil. (Turmoil is totally possible, but rather gnarly and I'd like to put it off a while.)

* It's been a 1-person project for about 3 years, and I would really love to change that.

* What's the point? There is no point. It's just fun.

## Messing around with it?

You totally can, but note: you will have a MUCH better time if you jump on the [discord](https://discord.com/invite/3vpKDktmde) and ask lots of questions. VERY few people have tried it yet who aren't me. Not much will be self-explanatory yet.

It is *supposed* to be as simple to get going as:

```
git clone https://github.com/MartianZoo/solarnet.git
cd solarnet
./rego
help
```

But in these early days, you're unlikely to get far on your own. I really want to improve that, but there's no better way for that to happen than for YOU to try and tell me how it goes!

## Learning more

### Videos

None of this is polished or anything.

* [First overview/demo](https://www.youtube.com/watch?v=btCLcFLvV2I). For this video it's best if you know the game pretty well.
* [Second overview](https://www.youtube.com/watch?v=pds_Axz2T90) for an audience of more hardcore software people; it still helps to know the game but I try to cover the basics.
* ["Logging" a real game](https://youtu.be/se8svQH-GOE) (long; watch on 2x, skip around) 
* [Gory video](https://www.youtube.com/watch?v=jC4iZnv4UA0) of me trying to add a brand new card (Supercapacitors) to Solarnet in real time.

### Docs

I haven't written too much yet. It will help to understand what kind of docs you would like to see next; right now, I could just write a million things on a million topics.

* A [FAQ](docs/faq.md)
* Overview of [component types](docs/component-types.md) -- a good place to start
* Pets language [language intro](docs/language-intro.md) and [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md) (incomplete)
* API docs -- see below

### Browse?

Want to just [browse through](https://github.com/MartianZoo/solarnet/tree/main/canon/src/main/java/dev/martianzoo/tfm/canon) how the game components are defined?

Don't expect everything to make sense right away; just breeze over the things that don't.

### Poke around in the implementation?

Start with the generated API doc view (which hides private things). I don't know how to host it properly, so you'd have to `./gradlew dokkaHtmlMultiModule` and then look at `docs/api/index.html`. That would help give you an idea of which source files you want to dig further into.

It's all written in [Kotlin](https://kotlinlang.org), which should in theory make the libraries usable from Java, JavaScript, and some other environments. I don't know how trivial or not that will be to do, but it certainly won't require porting the whole thing.

## Who are you

http://kevinb9n.github.io

Your thoughts and questions are welcome at kevinb9n@gmail.com or by whatever other means.
