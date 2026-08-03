## module PETS

### <a href="pets/dev.martianzoo.api/index.html">dev.martianzoo.api</a>

Basic interfaces that everything else needs to share.

### <a href="pets/dev.martianzoo.data/index.html">dev.martianzoo.data</a>

### <a href="pets/dev.martianzoo.pets/index.html">dev.martianzoo.pets</a>

The Pets language. Parsers for elements and class declarations, the objects it parses those into, utilities for transforming those objects, and so on.

### <a href="pets/dev.martianzoo.pets.ast/index.html">dev.martianzoo.pets.ast</a>

Pets element types, like Requirement, Action, Instruction, Expression, and so on. The abstract syntax tree the Pets language is parsed to.

Public AST construction favors compact companion factories such as `cn`, `scaledEx`, `gain`, and
`remove`. These factories may canonicalize their result without promising a particular concrete
node type. Raw constructors are non-public for nodes where canonicalization is meaningful.

### <a href="pets/dev.martianzoo.types/index.html">dev.martianzoo.types</a>

The Pets type system.

### <a href="pets/dev.martianzoo.tfm.api/index.html">dev.martianzoo.tfm.api</a>

### <a href="pets/dev.martianzoo.tfm.data/index.html">dev.martianzoo.tfm.data</a>

Data types for cards, milestones, maps, etc., and support for parsing these from JSON.
Milestone and award JSON may group definition lists under `groups`. A group's `setupRequirement`
is inherited by every definition in that group and conjoined with requirements on enclosing groups
or individual definitions. Canon's two-map expansions use this to keep one milestone file and one
award file while activating only the classes belonging to the selected map option.

### <a href="pets/dev.martianzoo.util/index.html">dev.martianzoo.util</a>

Various non-Terraforming-specific helpers.

## module CANON

### <a href="canon/dev.martianzoo.tfm.canon/index.html">dev.martianzoo.tfm.canon</a>

Contains the data and custom instructions for officially published cards, maps, etc. Canon also
provides an independent setup-world vocabulary and converts completed setup worlds into reusable
`GamePremise` values.

## module ENGINE

### <a href="engine/dev.martianzoo.analysis/index.html">dev.martianzoo.analysis</a>

### <a href="engine/dev.martianzoo.engine/index.html">dev.martianzoo.engine</a>

The engine knows how to modify a world by actually executing card instructions, etc.

### <a href="engine/dev.martianzoo.tfm.engine/index.html">dev.martianzoo.tfm.engine</a>

## module Script

### <a href="script/dev.martianzoo.script/index.html">dev.martianzoo.script</a>

The Kotlin Multiplatform plain-text command/session layer for driving the engine. Its command,
completion, and Terraforming Mars behavior runs on JVM and JavaScript. Host applications can add
commands explicitly without changing the shared session's behavior by compilation target.

## module REPL

### <a href="repl/dev.martianzoo.repl/index.html">dev.martianzoo.repl</a>

The JVM REgo PLastics application: its interactive JLine UI, filesystem script support, TCP server,
and executable entry point.

## module WEB

### <a href="web/dev.martianzoo.web/index.html">dev.martianzoo.web</a>

The browser-hosted REgo PLastics application. It adapts the shared Script session and completion
engine to a browser terminal, with browser-local command history and no server-side game process.

## module TOOLS

### dev.martianzoo.tools

Standalone JVM command-line utilities built from Solarnet's canonical data. The `solo-placement`
application accepts a map and four project-card class names and reports the neutral solo setup tile
locations. Its optional `--compatibility` mode rejects zero-cost cards and uses one-based counting.
Standard mode accepts zero-cost cards, uses zero-based counting, and assigns the four cards to city
1, city 2, greenery 1, and greenery 2. Compatibility mode instead places and assigns cards to city
1, greenery 1, city 2, and greenery 2. All placement counts skip nonexistent, reserved, and
occupied areas.
