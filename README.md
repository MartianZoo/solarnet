# Solarnet

Solarnet is an open-source **game engine** for the superlative 2016 board game *[Terraforming Mars](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2)*, published by [FryxGames](http://fryxgames.se).

It's not a way to *play* the game. For that, see the [online open-source app](http://terraforming-mars.herokuapp.com) (please [buy a physical copy](https://www.amazon.com/Indie-Boards-Cards-Terraforming-Board/dp/B01GSYA4K2) too!). There is also an official app for sale in the usual places, but I can't vouch for it.

This project is written from scratch, unrelated to other codebases.

## Intro video

This is my [first unrehearsed intro/demo](https://www.youtube.com/watch?v=btCLcFLvV2I).

## Why another game engine?

The game engine is the "pure logic" part of a game implementation. It's like a big calculator, and its only job is to *know the rules* of the game perfectly. Basically, who can do what when? And what happens if they do?

Inside the open-source app mentioned above is *already* an extremely accurate (>99.9%) game engine. And inside the official app mentioned above is already a game engine. So why do we need a third?

We don't! I just thought it would be fun and hard, and had some new ideas for how to do it.

What's different about solarnet is that all the game components that have heterogeneous behaviors (cards, milestones, map areas, etc.) are *[just data](/MartianZoo/solarnet/blob/main/canon/src/main/java/dev/martianzoo/tfm/canon/cards.json5)* and don't require custom programming (except for the [ones that do](https://github.com/MartianZoo/solarnet/blob/51e0f276dc3eede6cd2f00d3246a02d638c39b7a/canon/src/main/java/dev/martianzoo/tfm/canon/custom.kt#L157)). Instead, they're described using a specification language called **Pets** that's highly tailored to the particular needs of this particular game.

Pets specifications should in theory be convertible to natural-language instructions, or even into the iconographic language you see on the printed cards. But the relevant part today is that these strings are all Solarnet needs to read in order to actually execute the card correctly.

## Help is welcome

We've been a team of one for a long time, and jumping in won't be trivial. But I would love to spend time helping you get going.

Some ideas

* Hop on the [discord](https://discord.com/invite/3vpKDktmde) (it is very little-used so far) and ask whatever.
* Try reading and commenting on current [issues/feature-requests](http://github.com/MartianZoo/solarnet/issues). Some are relatively standalone.
* Grab the code, run the REPL and try messing around with it to see what it does.
* Anything that results in you asking me questions is actually helpful. There's a lot of stuff here and I don't know what to document or do a video about next.

## Learning more

### Videos

* [First overview/demo](https://www.youtube.com/watch?v=btCLcFLvV2I), not very polished. It's best if you know the game pretty well.
* [Second overview](https://www.youtube.com/watch?v=pds_Axz2T90) for an audience of hardcore software people but who might not know TfM well.
* [Gory video](https://www.youtube.com/watch?v=jC4iZnv4UA0) of me adding a brand new card (Supercapacitors) to Solarnet in real time.

### Docs

There is much unwritten, sorry.

* Overview of [component types](docs/component-types.md) -- a good place to start
* Pets language [language intro](docs/language-intro.md) and [syntax reference](docs/syntax.md)
* The Pets [type system](docs/type-system.md) (incomplete)
* A [FAQ](docs/faq.md) that should be pretty readable without technical expertise I think
* API docs -- see below

### Mess around with it?

If you have git and Java stuff working already, this *should* be all it takes to start playing around:

```
git clone https://github.com/MartianZoo/solarnet.git
cd solarnet
./rego
help
```

Of course, it won't work, because nothing ever works. I'd be *willing* to help, but sadly I actually know almost nothing about Build Stuff. Note `rego` is a shell script so I dunno if that's usable on Windows.

You can also browse the [Pets source files](/MartianZoo/solarnet/tree/main/canon/src/main/java/dev/martianzoo/tfm/canon) to see where all the game components come from. You can change it around or attempt to add your own custom cards.

### Poke around in the implementation?

Start with the generated API doc view (which hides private things). I don't know how to host it properly, so you'd have to `./gradlew dokkaHtmlMultiModule` and then look at `docs/api/index.html`. That would help give you an idea of which source files you want to dig further into.

It's all written in [Kotlin](https://kotlinlang.org), which should in theory make the libraries usable from Java, JavaScript, and some other environments. I don't know many details about this yet.

## Who are you

http://kevinb9n.github.io

Your thoughts and questions are welcome at kevinb9n@gmail.com or by whatever other means.
