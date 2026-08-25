## module PETS

### <a href="pets/dev.martianzoo.api/index.html">dev.martianzoo.api</a>

Basic interfaces that everything else needs to share.

### <a href="pets/dev.martianzoo.data/index.html">dev.martianzoo.data</a>

Basic data types shared by the engine and game-specific code.

### <a href="pets/dev.martianzoo.pets/index.html">dev.martianzoo.pets</a>

The Pets language. Parsers for elements and class declarations, the objects it parses those into, utilities for transforming those objects, and so on.

### <a href="pets/dev.martianzoo.pets.ast/index.html">dev.martianzoo.pets.ast</a>

Pets element types, like Requirement, Action, Instruction, Expression, and so on. The abstract syntax tree the Pets language is parsed to.

### <a href="pets/dev.martianzoo.tfm.data/index.html">dev.martianzoo.tfm.data</a>

Data types for cards, milestones, maps, etc., and support for parsing these from JSON.

### <a href="pets/dev.martianzoo.tfm.api/index.html">dev.martianzoo.tfm.api</a>

Terraforming Mars-specific definitions and, in the aspirational API, the `TfmAuthority` catalog
interface.

### <a href="pets/dev.martianzoo.types/index.html">dev.martianzoo.types</a>

The Pets type system.

### <a href="pets/dev.martianzoo.util/index.html">dev.martianzoo.util</a>

Various non-Terraforming-specific helpers.

## module LANGUAGE

### <a href="tfm-text/dev.martianzoo.tfm.text/index.html">dev.martianzoo.tfm.text</a>

English text for Terraforming Mars cards.

## module CANON

### <a href="tfm-canon/dev.martianzoo.tfm.canon/index.html">dev.martianzoo.tfm.canon</a>

Contains the data and custom instructions for officially published cards, maps, etc.

## module ENGINE

### <a href="engine/dev.martianzoo.engine/index.html">dev.martianzoo.engine</a>

The engine knows how to modify a Game World by executing card Instructions.

### <a href="engine/dev.martianzoo.tfm.engine/index.html">dev.martianzoo.tfm.engine</a>

Terraforming Mars-specific gameplay and workflow code.

### <a href="engine/dev.martianzoo.analysis/index.html">dev.martianzoo.analysis</a>

Getting summary statistics from a played game.

## module SCRIPT

### <a href="script/dev.martianzoo.script/index.html">dev.martianzoo.script</a>

The command and session layer shared by the REPL applications.

## module REPL

### <a href="repl/dev.martianzoo.repl/index.html">dev.martianzoo.repl</a>

REgo PLastics, an extremely bad command-line UI to the engine.

## module WEB

### <a href="web/dev.martianzoo.web/index.html">dev.martianzoo.web</a>

An early rough browser version of REgo PLastics.

## module TOOLS

### dev.martianzoo.tools

Standalone command-line tools built from Solarnet's data.
