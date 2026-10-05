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
      <img src="images/pets-repo-draft/optimal-aerobraking-top-half.png" alt="Optimal Aerobraking / Effect: When you play a space event, you gain 3 M€ and 3 heat. / " width="240">
    </td>
    <td valign="top">
      <img src="images/pets-repo-draft/energy-market-top-half.png" alt="Energy Market / Action: Spend 2X M€ to gain X energy, or decrease energy production 1 step to gain 8 M€. / " width="240">
    </td>
    <td valign="top">
      <img src="images/pets-repo-draft/earth-office-top-half.png" alt="Earth Office / Effect: When you play an Earth tag, you pay 3 M€ less for it. / " width="240">
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

*tfm-canon* is the catalog of all officially published Terraforming Mars content.

*tfm-fake* is fake versions of cards/etc. that aren't in canon because they don't really work right.

*pets-almanac* is a web app.

*tools* has random one-off crap.

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
      <img src="images/pets-repo-draft/regolith-eaters-top-half.png" alt="Regolith Eaters / Action: Add 1 microbe to this card, or remove 2 microbes from this card to raise oxygen level 1 step. / " width="240">
    </td>
    <td valign="top">
      <img src="images/pets-repo-draft/viral-enhancers-top-half.png" alt="Viral Enhancers / Effect: When you play a plant, microbe, or an animal tag, including this, gain 1 plant or add 1 resource TO THAT CARD. / " width="240">
    </td>
    <td valign="top">
      <img src="images/pets-repo-draft/predators-top-half.png" alt="Predators / Action: Remove 1 animal from any card and add it to this card. / Requires 11% oxygen. 1 VP per animal on this card." width="240">
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
      <img src="https://cards.hadronikle.com/thumbs/projects/Promo%20-%20X02%20-%20Project%20Inspection.png" alt="Project Inspection /  / USE A CARD ACTION THAT HAS ALREADY BEEN USED THIS GENERATION" width="240">
    </td>
    <td valign="top">
      <img src="https://cards.hadronikle.com/thumbs/projects/Base%20-%20152%20-%20Insulation.png" alt="Insulation /  / Decrease your heat production any number of steps and increase your M€ production the same number of steps." width="240">
    </td>
    <td valign="top">
      <img src="https://cards.hadronikle.com/thumbs/projects/Colonies%20-%20C04%20-%20Community%20Services.png" alt="Community Services /  / Increase your M€ production 1 step per CARD WITH NO TAGS, including this." width="240">
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
      <img src="https://cards.hadronikle.com/thumbs/corporations/Base%20-%20Tharsis%20Republic.png" alt="Tharsis Republic / Effect: When any city tile is placed ON MARS, increase your M€ production 1 step. When you place a city tile, gain 3 M€. / You start with 40 M€. As your first action in the game, place a city tile." width="360">
    </td>
    <td valign="top">
      <img src="https://cards.hadronikle.com/thumbs/corporations/Venus%20Next%20-%20Manutech.png" alt="Manutech / Effect: For each step you increase the production of a resource, including this, you also gain that resource. / You start with 1 steel production and 35 M€." width="360">
    </td>
  </tr>
</table>

<!--
Draft for the future standalone PETS repository; module descriptions above are
wording for that repository.

Gallery captions come from the canon cards.json5 definitions. They omit printed
requirements, VP scoring except for Predators, Tharsis Republic's solo setup
clause, and the starting effects of Tharsis Republic and Manutech. Line breaks
and indentation are for display.
Image alt text uses name / top text / bottom text from
english-corrected-wording-evidence.tsv in the english working copy.

The gallery has 11 cards: three opening examples and eight more examples at
the bottom, in three separate tables. The two corporations share the last table.
The first two tables use the upper 50% of Hadronikle's thumbnails, stored under
images/pets-repo-draft.
Nested samp elements give captions an intermediate font size under GitHub
Markdown styling without code-block copy controls.
Sponsored Academies remains a reserved candidate:
This: -ProjectCard THEN 3 ProjectCard, EACH Other@Player(NOT Me@Owner) { ProjectCard<Other@Player> }
-->
