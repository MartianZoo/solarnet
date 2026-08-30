## module PETS

### <a href="pets/dev.martianzoo.pets.api/index.html">dev.martianzoo.pets.api</a>

Basic interfaces that everything else needs to share.

### <a href="pets/dev.martianzoo.pets.data/index.html">dev.martianzoo.pets.data</a>

Basic data types shared by the engine and game-specific code.

### <a href="pets/dev.martianzoo.pets/index.html">dev.martianzoo.pets</a>

The Pets language. Parsers for elements and class declarations, the objects it parses those into, utilities for transforming those objects, and so on.

### <a href="pets/dev.martianzoo.pets.ast/index.html">dev.martianzoo.pets.ast</a>

Pets element types, like Requirement, Action, Instruction, Expression, and so on. The abstract syntax tree the Pets language is parsed to.

### <a href="pets/dev.martianzoo.pets.types/index.html">dev.martianzoo.pets.types</a>

The Pets type system.

### <a href="pets/dev.martianzoo.pets.util/index.html">dev.martianzoo.pets.util</a>

Various non-Terraforming-specific helpers.

## module TFM-TEXT

### <a href="tfm-text/dev.martianzoo.tfm.text/index.html">dev.martianzoo.tfm.text</a>

English text for Terraforming Mars cards.

## module CANON

### <a href="tfm-canon/dev.martianzoo.tfm.canon/index.html">dev.martianzoo.tfm.canon</a>

Contains the Terraforming Mars catalog model, data, custom instructions, and officially published
cards, maps, etc.

## module ENGINE

### <a href="engine/dev.martianzoo.engine/index.html">dev.martianzoo.engine</a>

The engine knows how to modify a Game World by executing card Instructions.

## module TFM-ENGINE

### <a href="tfm-engine/dev.martianzoo.tfm.engine/index.html">dev.martianzoo.tfm.engine</a>

Terraforming Mars-specific gameplay, workflow, and shared presentation rules.

## module SCRIPT

### <a href="script/dev.martianzoo.script/index.html">dev.martianzoo.script</a>

The command and session layer shared by the REPL applications.

## module REPL

### <a href="repl/dev.martianzoo.repl/index.html">dev.martianzoo.repl</a>

REgo PLastics, an extremely bad command-line UI to the engine.

## module WEB

### <a href="web/dev.martianzoo.tfm.web.webrepl/index.html">dev.martianzoo.tfm.web.webrepl</a>

An early rough browser version of REgo PLastics.

## module GAME-VIEWER

### <a href="game-viewer/dev.martianzoo.tfm.web.gameviewer/index.html">dev.martianzoo.tfm.web.gameviewer</a>

A browser viewer for recorded games, with saved replays and event-log timeline navigation. Both
browser applications use assets owned by `dev/martianzoo/tfm/web/shared`.

## module TOOLS

### dev.martianzoo.tools

Standalone command-line tools built from Solarnet's data.
