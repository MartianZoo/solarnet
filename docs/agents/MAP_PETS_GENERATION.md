# Canon map Pets generation

> **Read when:** editing a map diagram, generated area declarations, map prefix naming, or the
> `regenerateMapAreas` tool.
>
> **Skip when:** changing tile-placement gameplay without changing map topology or metadata.
>
> **Status:** current authoring and regeneration procedure.

## Source map

- [Tharsis `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TharsisMap/classes.pets)
  — search for `TharsisMap` to see an authored diagram and its generated block.
- [`MarsMapReader.kt`](../../src/common/dev/martianzoo/tfm/canon/MarsMapReader.kt)
  — inspect when changing diagram interpretation.
- [`regenerateMapAreas.kt`](../../src/jvm/dev/martianzoo/tools/regenerateMapAreas.kt) —
  search for `fun main` when changing rewrite formatting or round-trip checks.

Canon map topology and bonus metadata are authored as compact diagrams in comments within each
bundle's `classes.pets`. Each diagram is immediately followed by its generated area declarations.
The first character of a cell selects the area kind; the remaining characters encode its bonuses.
Rows use the standard nine-row Mars-map shape, so their horizontal position determines columns.

`MarsMapReader` reads those diagrams directly when Canon loads a bundle. `MapGoalSets` supplies the
explicit milestone and award Class names selected by each canonical map.

`./gradlew :tools:regenerateMapAreas` rewrites every generated area block in place. The tool keeps
map-cell centers six characters apart, centers odd-width codes on their cells, and lets even-width
codes extend one character farther right. It leaves a blank comment line between map rows. The tool
also keeps each area declaration on one line, aligns area kinds, separates declaration rows with
blank lines, and checks that the resulting Pets declarations round-trip to the definitions read
from the diagrams.
