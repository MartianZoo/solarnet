# Authority classes and game class views

**Status: proposal.** This describes the type/class ownership model we are trying to reach, not
committed behavior. Current behavior remains documented in [TYPES.md](TYPES.md).

## Target model

An Authority owns one immutable master type universe. Within that universe there is exactly one
`Class` instance for each known Class Name. `Type` values are likewise structural values from that
master universe.

A game owns a filtered view of the master universe. The view records which authority-known Classes
are inhabited in that game and owns any indexes that enumerate the inhabited domain. It references
the master Classes; it does not reconstruct them, their Types, their properties, or their nominal
hierarchy.

`Class` and `Type` must not provide a path back to a game-filtered `ClassTable`. They may retain an
unexposed master-universe identity so operations can reject values from different Authorities, but
that identity is not a source of game context. In particular, removing `Class.classTable` must not
be followed by adding a differently named projection backpointer.

Consequently, inhabitation is not an intrinsic property of a `Class` or `Type`. The current
projection-relative `phantom` properties disappear. A game class view answers whether a Class or
Type is inhabited in that game.

## Structural operations versus game-domain operations

Operations whose answers come entirely from authored declarations belong to the master universe:

- nominal subtyping and superclass relationships;
- dependencies, properties, and defaults;
- structural `glb` and `lub`; and
- expression-to-Type resolution that does not inspect a live World.

Structural `glb` combines constraints in the Authority universe. It may return a Type that is
uninhabited in a particular game. `null` means that the Authority defines no compatible Type;
inhabitation is a separate question asked of the game view.

Operations whose answers depend on the selected game must receive that context explicitly:

- inhabited and direct-subclass enumeration;
- concrete-Type enumeration and automatic narrowing;
- deciding whether a Class literal counts or an effect is live;
- validating a Component gain, removal, or transmutation; and
- any operation involving a state-dependent refinement.

The natural context may be the game class view or a `GameReader`; a `Class` or `Type` must not find
it by reverse navigation.

## Identity and integrity

Classes and Types from different master universes remain incomparable. Values from two games using
the same master universe are structurally comparable, even when their inhabited domains differ.

World mutation therefore validates both that an incoming Type belongs to the World's master
universe and that the Type is inhabited in that World's view. Projection identity must not stand in
for either check.

Unknown and uninhabited remain distinct. An Authority-known uninhabited Class resolves and keeps its
nominal relationships, but the game view gives it an empty domain. An unknown Class Name remains an
error.

## Resulting projection shape

A game projection should contain only game-relative information, such as:

- the inhabited Class set;
- filtered direct/proper-subclass indexes;
- selected Modules and premise validation results; and
- any derived indexes whose contents vary with that set.

It must not contain projection-local copies of all master `Class` objects. The exact representation
of the inhabited set is an implementation choice, not part of the model.

## Current pressure

The current `ClassLoader` builds projection-local `Class` objects because `Class.classTable`,
`Type.classTable`, `phantom`, and no-context enumeration make game filtering implicit. In a traced
whole-game test, the Canon master and one game projection each represented 1,191 Classes; the
projection rebuilt all 1,191, including 395 uninhabited Classes. A traced engine JVM suite created
621 game projections through `Engine`.

Removing that repeated construction is the immediate performance motivation. The architectural
goal is stronger: type values should describe authority-defined structure, while the World should
be the explicit source of game-relative truth.

This change does not remove the need to compute the activation closure for a premise, and it does
not by itself guarantee the whole-game performance target.

## Completion criteria

The target model is reached when:

- no `Class`, `Type`, or dependency value exposes or retains a game-projection backpointer;
- creating a game projection constructs no `Class` instances;
- all game-relative enumeration and inhabitation checks receive an explicit view or reader;
- structural operations give the same answer in every game using one master universe;
- target-World validation prevents an uninhabited Type from entering that World; and
- the existing activation, uninhabited-domain, and whole-game behavior remains otherwise unchanged.
