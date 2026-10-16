## module PETS

### <a href="pets/dev.martianzoo.pets.api/index.html">dev.martianzoo.pets.api</a>

Basic interfaces that everything else needs to share.

### <a href="pets/dev.martianzoo.pets.data/index.html">dev.martianzoo.pets.data</a>

Static declaration data shared by the Pets compiler, Catalogs, and consumers.

### <a href="pets/dev.martianzoo.pets/index.html">dev.martianzoo.pets</a>

The Pets language: parsers for elements and class declarations, utilities for transforming what they
produce, and elaboration. Specified by the
[Pets language specification](https://github.com/MartianZoo/solarnet/blob/main/pets/docs/pets-language-spec.md).

Sections 1 and 2 (source, declarations and names), 10 (transform blocks), 11 (owner-local classes)
and 12 (elaboration) are owned here; the elements themselves live in `dev.martianzoo.pets.ast`.

### <a href="pets/dev.martianzoo.pets.ast/index.html">dev.martianzoo.pets.ast</a>

The abstract syntax tree the Pets language is parsed to: the six element types — Expression,
Requirement, Metric, Instruction, Effect and Action — plus the ancillary nodes they are built from.

The Kotlin API for the concepts and rules in sections 3 through 9 of the
[Pets language specification](https://github.com/MartianZoo/solarnet/blob/main/pets/docs/pets-language-spec.md).

### <a href="pets/dev.martianzoo.pets.types/index.html">dev.martianzoo.pets.types</a>

The Kotlin API for the concepts and rules in the
[Pets type-system specification](https://github.com/MartianZoo/solarnet/blob/main/pets/docs/type-system-spec.md).

### <a href="pets/dev.martianzoo.pets.util/index.html">dev.martianzoo.pets.util</a>

Various non-Terraforming-specific helpers.

## module CATALOG

### <a href="catalog/dev.martianzoo.catalog/index.html">dev.martianzoo.catalog</a>

Static Catalog composition, configuration, premise resolution, filtered Class Tables, and display
names. It depends only on Pets and can be used without runtime game state or execution.

## module TEXT

### <a href="text/dev.martianzoo.tfm.text/index.html">dev.martianzoo.tfm.text</a>

English text for Terraforming Mars cards.

## module TFM-CANON

### <a href="tfm-canon/dev.martianzoo.tfm.canon/index.html">dev.martianzoo.tfm.canon</a>

Contains the Terraforming Mars static catalog model, data, and officially published cards, maps,
etc. Executable custom implementations and live-game helpers belong to `tfm-engine`.

## module TFM-FAKE

### <a href="tfm-fake/dev.martianzoo.tfm.fake/index.html">dev.martianzoo.tfm.fake</a>

Contains noncanonical Terraforming Mars declarations for tests, replays, and support tools.

## module ALMANAC

### <a href="almanac/dev.martianzoo.tfm.web.almanac/index.html">dev.martianzoo.tfm.web.almanac</a>

Pets Almanac, a searchable viewer for Canon's normalized Pets declarations, type information, and
derived card text.

## module JUNKDRAWER

### dev.martianzoo.tfm.petstools

Standalone tools for generating and analyzing Pets and Canon data.
