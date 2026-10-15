This project is for fans of the superlative 2016 board game *Terraforming
Mars*, and is in no way affiliated with FryxGames AB.

PETS is a specification language for Precisely Expressing Terraforming
Semantics.

<table>
  <tr>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>EventCard(HAS SpaceTag):<br>&nbsp;&nbsp;&nbsp;&nbsp;3 MC, 3 Heat</strong></samp></samp>
    </td>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>2X MC -&gt; X Energy<br>PROD[Energy] -&gt; 8 MC</strong></samp></samp>
    </td>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>PayingFor&lt;Class&lt;EarthTag&gt;&gt;::<br>&nbsp;&nbsp;&nbsp;&nbsp;-3 Owed</strong></samp></samp>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/OptimalAerobraking.png" alt="Optimal Aerobraking / Effect: When you play a space event, you gain 3 M€ and 3 heat. / " width="240">
    </td>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/EnergyMarket.png" alt="Energy Market / Action: Spend 2X M€ to gain X energy, or decrease energy production 1 step to gain 8 M€. / " width="240">
    </td>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/EarthOffice.png" alt="Earth Office / Effect: When you play an Earth tag, you pay 3 M€ less for it. / " width="240">
    </td>
  </tr>
</table>

The game (with its expansions) is overflowing with hundreds of interesting
project and prelude cards, corporations, milestones, awards, maps, standard
projects, colony tiles, political parties, and global events, and no two are
alike. But what exactly makes one different from another? For 99% of all the
officially published content, at least: you can write that down using PETS.

You'll notice that PETS resembles the icon language on the published cards,
which is intentional. It is also a... for lack of a better term, real language,
with a generic type system and stuff like that.

So, why do we want a specification language for Terraforming Mars cards?

From this *single representation*, we can:

* Generate the card's English-language instruction text (mostly works today)
* Generate instructions in other languages too (volunteers welcome)
* Generate the iconographic representation of the card (volunteers welcome)

... and there is also [Solarnet](https://github.com/MartianZoo/solarnet), a
completely working game engine that can basically play any card correctly as
long as it's written in this format.

If everything is single-sourced from the PETS representation, then what a card
does basically can't be different from what it *says* it does, because there
aren't two different representations of that behavior to get out of sync with
each other.

## What's here

This is a Kotlin multiplatform project, so it can be run on a JVM, or as
javascript in a browser, or in yet other ways I haven't verified yet.

*pets* has the parser, the AST library it parses into, the class loader and type system.

*catalog* assembles declarations into selectable static game catalogs.

*tfm-card-data* reads the external card database, and *tfm-compiler* turns it into Pets.

*tfm-canon* is the catalog of all officially published Terraforming Mars content.

*tfm-fake* is fake versions of cards/etc. that aren't in canon because they don't really work right.

*text* derives English from the declarations; its Mars-specific vocabulary still needs separating.

*almanac* is a web app for exploring the resulting catalog.

*codegen* can generate typed Kotlin access to a Pets class table.

*junkdrawer* has random one-off generation and analysis crap.

<table>
  <tr>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>-&gt; Microbe&lt;This&gt;<br>2 Microbe&lt;This&gt; -&gt; OxygenStep</strong></samp></samp>
    </td>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>BioTag&lt;@CardFront&gt;: Plant<br>&nbsp;&nbsp;&nbsp;&nbsp;OR Animal&lt;@CardFront&gt;<br>&nbsp;&nbsp;&nbsp;&nbsp;OR Microbe&lt;@CardFront&gt;</strong></samp></samp>
    </td>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>Animal&lt;Anyone&gt; -&gt; Animal&lt;This&gt;<br>End: VictoryPoint / Animal&lt;This&gt;</strong></samp></samp>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/RegolithEaters.png" alt="Regolith Eaters / Action: Add 1 microbe to this card, or remove 2 microbes from this card to raise oxygen level 1 step. / " width="240">
    </td>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/ViralEnhancers.png" alt="Viral Enhancers / Effect: When you play a plant, microbe, or an animal tag, including this, gain 1 plant or add 1 resource TO THAT CARD. / " width="240">
    </td>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/Predators.png" alt="Predators / Action: Remove 1 animal from any card and add it to this card. / Requires 11% oxygen. 1 VP per animal on this card." width="240">
    </td>
  </tr>
</table>

<table>
  <tr>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>This: UseAction&lt;ActionCard(<br>&nbsp;&nbsp;&nbsp;&nbsp;HAS ActionUsedMarker)&gt;</strong></samp></samp>
    </td>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>This: PROD[X MC FROM Heat]</strong></samp></samp>
    </td>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>This: PROD[1 MC /<br>&nbsp;&nbsp;&nbsp;&nbsp;CardFront(HAS MAX 0 Tag)]</strong></samp></samp>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/ProjectInspection.png" alt="Project Inspection /  / USE A CARD ACTION THAT HAS ALREADY BEEN USED THIS GENERATION" width="240">
    </td>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/Insulation.png" alt="Insulation /  / Decrease your heat production any number of steps and increase your M€ production the same number of steps." width="240">
    </td>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/CommunityServices.png" alt="Community Services /  / Increase your M€ production 1 step per CARD WITH NO TAGS, including this." width="240">
    </td>
  </tr>
</table>

<table>
  <tr>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>CityTile&lt;Anyone, MarsArea&gt;: PROD[1 MC]<br>CityTile: 3 MC</strong></samp></samp>
    </td>
    <td valign="top" nowrap="nowrap">
      <samp><samp><strong>PROD[@StandardResource]: @StandardResource</strong></samp></samp>
    </td>
  </tr>
  <tr>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/TharsisRepublic.png" alt="Tharsis Republic / Effect: When any city tile is placed ON MARS, increase your M€ production 1 step. When you place a city tile, gain 3 M€. / You start with 40 M€. As your first action in the game, place a city tile." width="360">
    </td>
    <td valign="top">
      <img src="https://martianzoo.dev/images/classes-pubtext/Manutech.png" alt="Manutech / Effect: For each step you increase the production of a resource, including this, you also gain that resource. / You start with 1 steel production and 35 M€." width="360">
    </td>
  </tr>
</table>

<!--
Gallery captions come from the canon cards.json5 definitions. They omit printed
requirements, VP scoring except for Predators, Tharsis Republic's solo setup
clause, and the starting effects of Tharsis Republic and Manutech. Line breaks
and indentation are for display.
Image alt text uses name / top text / bottom text from
english-corrected-wording-evidence.tsv in the english working copy.

The gallery has 11 cards: three opening examples and eight more examples at
the bottom, in three separate tables. The two corporations share the last table.
Card images use the published-text images hosted at martianzoo.dev/images/classes-pubtext.
Nested samp elements give captions an intermediate font size under GitHub
Markdown styling without code-block copy controls.
Sponsored Academies remains a reserved candidate:
This: -ProjectCard THEN 3 ProjectCard, EACH Other@Player(NOT Me@Owner) { ProjectCard<Other@Player> }
-->

---

# Pets

Pets describes game rules as classes, types, instructions, and effects. This repository contains
its language implementation and static tools, with Terraforming Mars as its working vocabulary.
It can load and inspect declarations, generate canonical content, and derive English without
starting a game.

## What's here

| Module | Responsibility |
| --- | --- |
| `pets` | Parsing, elaboration, types, refinements, and instructions |
| `catalog` | Bundles of declarations and content selection |
| `tfm-card-data` | Published card data, independent of the Pets language |
| `tfm-compiler` | Converts card data into canonical Pets declarations |
| `tfm-canon` | Canonical Terraforming Mars vocabulary and content |
| `tfm-fake` | Explicit alternate content used in experiments and tests |
| `text` | English derived from the declarations; still uses Mars vocabulary |
| `almanac` | Browser for the catalog, types, and derived English |
| `junkdrawer` | Static analyses and content generators |
| `codegen` | Experimental generation of typed Kotlin vocabulary |

Sources share `src/common`, `src/jvm`, and `src/js`, with matching directories under `test/`.
The build files under `modules/` select the packages belonging to each module. Game state,
execution, player agents, game replays, and Playground are maintained by Solarnet.

The name `text` describes the intended responsibility. Its current implementation still depends on
`tfm-canon`, supplies Mars-specific English vocabulary, and recognizes parts of the Mars model.
Moving those facts into Canon is tracked in TODO.md; the rename does not complete that separation.

## Build

Use JDK 17–26. The Gradle wrapper downloads the required tools; browser tests need Chrome or Chromium
(`CHROME_BIN` can select the executable).

```sh
./gradlew build                 # JVM tests, browser tests, JS compilation and packaging
./gradlew :pets:test            # Language and type tests, including the browser smoke test
./gradlew :text:test            # Derived English tests
./gradlew spotlessApply         # Format Kotlin and Gradle scripts
./gradlew :almanac:jsBrowserDevelopmentRun
```

The [documentation index](docs/README.md) links the guides, Canon notes, English comparisons,
and [agent documentation router](docs/agents/README.md).

The [language specification](docs/pets-language-spec.md) and
[type-system specification](docs/type-system-spec.md) describe the intended contracts.
Their corresponding tests live in the `pets` module. Known mismatches are tracked in [TODO.md](TODO.md).

## Use from Solarnet

Solarnet can include this directory as a Gradle composite build, or consume the published JVM and
JavaScript libraries under `dev.martianzoo.pets`. See its build-selection instructions for choosing
a local checkout. No publication is needed while editing through a composite.

For an isolated local publication, use an absolute path to an empty Maven repository:

```sh
./gradlew publish -PpetsRepository=/absolute/path/to/repository
```

`gradle.properties` records the publication version. Almanac also publishes a ZIP of its built web
files, so consumers do not need access to its source or intermediate build directories.

This is an independent fan project, not affiliated with or endorsed by FryxGames AB.
