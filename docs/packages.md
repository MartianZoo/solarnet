## module PETS

### <a href="pets/dev.martianzoo.pets.api/index.html">dev.martianzoo.pets.api</a>

Basic interfaces that everything else needs to share.

### <a href="pets/dev.martianzoo.pets.data/index.html">dev.martianzoo.pets.data</a>

Catalog, premise, configuration, and Actor data shared by state and game-specific code.

### <a href="pets/dev.martianzoo.pets/index.html">dev.martianzoo.pets</a>

The Pets language: parsers for elements and class declarations, utilities for transforming what they
produce, and elaboration. Specified by the
[Pets language specification](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md).

Sections 1 and 2 (source, declarations and names), 10 (transform blocks), 11 (owner-local classes)
and 12 (elaboration) are owned here; the elements themselves live in `dev.martianzoo.pets.ast`.

### <a href="pets/dev.martianzoo.pets.ast/index.html">dev.martianzoo.pets.ast</a>

The abstract syntax tree the Pets language is parsed to: the six element types — Expression,
Requirement, Metric, Instruction, Effect and Action — plus the ancillary nodes they are built from.

The Kotlin API for the concepts and rules in sections 3 through 9 of the
[Pets language specification](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md).

### <a href="pets/dev.martianzoo.pets.types/index.html">dev.martianzoo.pets.types</a>

The Kotlin API for the concepts and rules in the
[Pets type-system specification](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md).

### <a href="pets/dev.martianzoo.pets.util/index.html">dev.martianzoo.pets.util</a>

Various non-Terraforming-specific helpers.

## module TFM-TEXT

### <a href="tfm-text/dev.martianzoo.tfm.text/index.html">dev.martianzoo.tfm.text</a>

English text for Terraforming Mars cards.

## module TFM-CANON

### <a href="tfm-canon/dev.martianzoo.tfm.canon/index.html">dev.martianzoo.tfm.canon</a>

Contains the Terraforming Mars catalog model, data, custom instructions, and officially published
cards, maps, etc.

## module TFM-FAKE

### <a href="tfm-fake/dev.martianzoo.tfm.fake/index.html">dev.martianzoo.tfm.fake</a>

Contains noncanonical Terraforming Mars declarations for tests, replays, and support tools.

## module STATE

### <a href="state/dev.martianzoo.state/index.html">dev.martianzoo.state</a>

The replayable state of one game: components, pending tasks, exact event history, rich queries,
passive event application, immutable recordings, and opaque recording serialization.

## module ENGINE

### <a href="engine/dev.martianzoo.engine/index.html">dev.martianzoo.engine</a>

The engine knows how to modify a Game World by executing card Instructions.

## module TFM-ENGINE

### <a href="tfm-engine/dev.martianzoo.tfm.engine/index.html">dev.martianzoo.tfm.engine</a>

Terraforming Mars-specific gameplay, workflow, and shared presentation rules.

## module SCRIPT

### <a href="script/dev.martianzoo.tfm.script/index.html">dev.martianzoo.tfm.script</a>

The Terraforming Mars command and session layer shared by the REPL applications.

## module REPL

### <a href="repl/dev.martianzoo.repl/index.html">dev.martianzoo.repl</a>

REgo PLastics, an extremely bad command-line UI to the engine.

## module WEB

### <a href="web/dev.martianzoo.tfm.web.webrepl/index.html">dev.martianzoo.tfm.web.webrepl</a>

An early rough browser version of REgo PLastics. The same module also supplies Pets Almanac, a
searchable viewer for Canon's normalized Pets declarations and detailed type information.

## module GAME-VIEWER

### <a href="game-viewer/dev.martianzoo.tfm.web.gameviewer/index.html">dev.martianzoo.tfm.web.gameviewer</a>

An engine-free browser viewer that discovers generated replay-test recordings and navigates their
event-log timelines through passive state playback. Both browser applications use assets owned by
`dev/martianzoo/tfm/web/shared`.

## module TOOLS

### dev.martianzoo.tfm.tools

Standalone command-line tools built from Solarnet's data.
