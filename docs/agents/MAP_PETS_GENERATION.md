# Canon map Pets generation

Canon map topology and bonus metadata are authored as compact diagrams in comments within each
bundle's `classes.pets`. Each diagram is immediately followed by its generated area declarations.
The first character of a cell selects the area kind; the remaining characters encode its bonuses.
Rows use the standard nine-row Mars-map shape, so their horizontal position determines columns.

`MarsMapReader` reads those diagrams directly when Canon loads a bundle. Milestone and award pools
follow the map prefix naming convention, such as `TharsisMilestone` for `TharsisMap`.

`./gradlew :tools:regenerateMapAreas` rewrites every generated area block in place. The tool keeps
map-cell centers six characters apart, centers odd-width codes on their cells, and lets even-width
codes extend one character farther right. It leaves a blank comment line between map rows. The tool
also keeps each area declaration on one line, aligns area kinds, separates declaration rows with
blank lines, and checks that the resulting Pets declarations round-trip to the definitions read
from the diagrams.
