# Solarnet

## Fast facts

* Solarnet is a work-in-progress game engine for the amazing board game *[Terraforming Mars](https://boardgamegeek.com/boardgame/167791/terraforming-mars)*.

* It's not a way to *play* the game; for that see the excellent [open-source app](http://github.com/terraforming-mars/terraforming-mars) (which this is unrelated to).

* It's a work in progress! Try to look at it for what it could become rather than what it is right now. :-)

* It's a standalone library. Its only job is to *know the rules of the game*: who can do what when, and what happens if they do? It covers the "pure logic part" of the game. You can set up game situations and see what happens. (I do not blame you if it's quite incomprehensible what this could possibly useful for. :-))

* Card behaviors etc. are written in a language called Pets that closely resembles the game's existing iconographic language. These strings are ALL the game engine needs to know about a card in order to play it correctly. Examples:

| Class | Example Pets syntax |
| ----- | ------------------- |
| `LargeConvoy` | `This: OceanTile, 2 ProjectCard, 5 Plant OR 4 Animal` |
| `ElectroCatapult` | `Plant OR Steel -> 7` |
| `ArcticAlgae` | `OceanTile BY Anyone: 2 Plant` |
| `Insulation` | `This: PROD[X Megacredit FROM Heat]` |
| `EarthCatapult` | `PlayCard: -2 Owed<Megacredit>` |
| `CitySP` | `25 -> CityTile, PROD[1]` |
| `TerraformRating` | `ProductionPhase: 1`; `End: VictoryPoint` |
| `CityTile` | `End: VictoryPoint / Adjacency<This, GreeneryTile<Anyone>>` |

* This means that you can easily add your own fan cards to it, without actual "programming", as long as they don't require entire new mechanics. (The code is not yet arranged to make this *convenient* though.)

* It has a crappy command-line UI (a "REPL") you can use to interact with it (see demo video below). Or you can write what you want to do as a unit test ([example](https://github.com/MartianZoo/solarnet/blob/main/engine/src/test/java/dev/martianzoo/tfm/engine/games/Game20230521Test.kt)).

* The engine has some smarts. It lets you perform your turn effects in any order (per the game rules), but it can tell if there's only one task you could do next, and it can tell when you actually need to make a choice about something and when you don't.

* If you play a game IRL or on the app, you can sort of "log" it in Solarnet, and then be able to ask all kinds of questions like "How much money did AdvancedAlloys actually save me that game?"

* It works! See the Issues tab for exceptions. I'm closing in on having 400 cards working, including most of each expansion but Turmoil. (Turmoil is totally feasible, just gnarly, and I'd like to put it off for a while.)

* It's been a 1-person project for about three years now, but I'd love to change that.

* What's the point? There is no point. It's just fun.

## Messing around with it?

You totally can, but note: you will have a MUCH better time if you jump on the [discord](https://discord.com/invite/3vpKDktmde) and ask lots of questions. Almost no one but me has tried to do much with it yet. It won't be self-explanatory (sorry).

It is *supposed* to be as simple to get going as:

```
git clone https://github.com/MartianZoo/solarnet.git
cd solarnet
./rego
help
```

But in these early days, you're unlikely to get far on your own. I want to improve that, but the best chance for that to happen is if YOU give things a try and tell me how it goes!

## Learning more

### Videos

None of this is polished or anything.

* [First overview/demo](https://www.youtube.com/watch?v=btCLcFLvV2I). For this video it's best if you know the game pretty well.
* [Second overview](https://www.youtube.com/watch?v=pds_Axz2T90). This one was for an audience of more hardcore software people; it still helps to know the game, but maybe less crucially than for the previous video.
* [Watch as I "log" a real game](https://youtu.be/se8svQH-GOE) (I explain stuff, but it's long; watch on at least 1.5x). 
* [Gory video](https://www.youtube.com/watch?v=jC4iZnv4UA0) of me add a brand new card (Supercapacitors) to Solarnet in about a half hour.

### Docs

I haven't written too much yet. It will help to understand what kind of docs you would like to see next; from where I sit there are just too many different things I could write about to choose.

* A [FAQ](docs/faq.md)
* [Cheat sheet](docs/cheat-sheet.md)
* Overview of [component types](docs/component-types.md) -- not a bad place to start
* Pets language [language intro](docs/language-intro.md) and [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md) (incomplete)
* API docs -- see below

### Browse?

Want to just [browse through](https://github.com/MartianZoo/solarnet/tree/main/canon/src/main/java/dev/martianzoo/tfm/canon) how the game components are defined?

It should be interesting, just don't expect everything to make sense right away. Try to just breeze over the things that don't.

### Poke around in the implementation?

Start with the generated API doc view (because it hides private things). I don't know how to host it properly, so you'd have to `./gradlew dokkaHtmlMultiModule` and then look at `docs/api/index.html`. That would help give you an idea of which source files you want to dig further into.

It's all written in [Kotlin](https://kotlinlang.org), which should in theory make the libraries usable from Java, JavaScript, and some other environments. I don't know how trivial or not that will be to do, but it certainly won't require porting the whole thing.

## Who are you

http://kevinb9n.github.io

Your thoughts and questions are welcome at kevinb9n@gmail.com or by whatever other means.
