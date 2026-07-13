## module PETS

### <a href="pets/dev.martianzoo.api/index.html">dev.martianzoo.api</a>

Basic interfaces that everything else needs to share.

### <a href="pets/dev.martianzoo.data/index.html">dev.martianzoo.data</a>

### <a href="pets/dev.martianzoo.pets/index.html">dev.martianzoo.pets</a>

The Pets language. Parsers for elements and class declarations, the objects it parses those into, utilities for transforming those objects, and so on.

### <a href="pets/dev.martianzoo.pets.ast/index.html">dev.martianzoo.pets.ast</a>

Pets element types, like Requirement, Action, Instruction, Expression, and so on. The abstract syntax tree the Pets language is parsed to.

### <a href="pets/dev.martianzoo.tfm.api/index.html">dev.martianzoo.tfm.api</a>

### <a href="pets/dev.martianzoo.tfm.data/index.html">dev.martianzoo.tfm.data</a>

Data types for cards, milestones, maps, etc., and support for parsing these from JSON.

### <a href="pets/dev.martianzoo.util/index.html">dev.martianzoo.util</a>

Various non-Terraforming-specific helpers.

## module CANON

### <a href="canon/dev.martianzoo.tfm.canon/index.html">dev.martianzoo.tfm.canon</a>

Contains the data and custom instructions for officially published cards, maps, etc.

## module ENGINE

### <a href="engine/dev.martianzoo.analysis/index.html">dev.martianzoo.analysis</a>

### <a href="engine/dev.martianzoo.types/index.html">dev.martianzoo.types</a>

The Pets type system.

### <a href="engine/dev.martianzoo.engine/index.html">dev.martianzoo.engine</a>

The engine knows how to modify a game state by actually executing card instructions, etc.

### <a href="engine/dev.martianzoo.tfm.engine/index.html">dev.martianzoo.tfm.engine</a>

## module Script

### <a href="script/dev.martianzoo.script/index.html">dev.martianzoo.script</a>

The plain-text command/session layer for driving the engine from scripts or server clients.

## module REPL

### <a href="repl/dev.martianzoo.repl/index.html">dev.martianzoo.repl</a>

REgo PLastics, an extremely bad command-line UI to the engine.
