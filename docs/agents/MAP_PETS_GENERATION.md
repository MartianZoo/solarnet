# Canon map Pets generation

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** editing map data, generated area declarations, map prefix naming, or the
> `generateTfmPets` tool.
>
> **Skip when:** changing tile-placement gameplay without changing map topology or metadata.
>
> **Status:** current authoring and regeneration procedure.

## Source map

- [Tharsis `maps.json5`](../../src/common/dev/martianzoo/tfm/mapdata/TharsisMap/maps.json5) — authored
  rows and its per-map legend.
- [`MapDefinition.kt`](../../src/common/dev/martianzoo/tfm/mapdata/MapDefinition.kt) — diagram and
  sigil decoding without a Pets dependency.
- [`GenerateTfmPets.kt`](../../src/jvm/dev/martianzoo/tools/GenerateTfmPets.kt) — map declaration
  construction and rendering.
- [Tharsis `maps.pets`](../../src/common/dev/martianzoo/tfm/canon/TharsisMap/maps.pets) — generated
  diagram comment and area declarations.

Map topology and bonus metadata are authored as whole row strings plus a per-map legend in
`tfm-map-data`. The first character of a cell selects the area kind; the remaining characters encode
its bonuses.
Digits before another bonus sigil multiply it; a final digit is itself a sigil, such as the `6` in
`O6` for the Hellas north-pole cost or the `4` in `LF4` for the Vastitas -4 M€ bonus.
Diagram indentation locates areas on slant-columns. The leftmost occupied slant-column is column 1;
the reader does not infer an overall map shape.
Maps whose largest row or column is at least 10 use two digits for both coordinates in every area
class name; smaller maps retain their unpadded names.

Canon loads only generated Class declarations. Each map's hand-authored map Module, milestones, and
awards remain in `classes.pets` beside `maps.pets`; premise resolution derives its default pools
from that bundle ownership.

A legend entry whose instruction is `Ok` is emitted as its own harmless `Tile<This>: Ok` effect.
Repeated sigils produce repeated effects. This preserves presentation codes such as `D` and `DD`
on the runtime Class without introducing separate display metadata; semantic bonus consumers
combine the effects while discarding the no-ops.

`./gradlew :tools:generateTfmPets` rewrites every generated `maps.pets` and `cards.pets`. For a
non-mutating comparison, pass `-PtfmPetsOutput=PATH`. Map output retains each row exactly in a
diagram comment, keeps each area declaration on one line, and separates declaration rows with a
blank line. `GenerateCardPetsTest` checks byte-for-byte drift and proves the generated map Class
names match the expanded data.
