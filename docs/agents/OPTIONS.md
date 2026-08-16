# Authorities, Modules, and game premises

**Status: current model.**

## Authority

An **Authority** is one coherent namespace containing everything Solarnet may know about a game:

- authored Rule-Class declarations;
- structured definitions that generate Content-Class declarations;
- vocabulary and descriptive metadata;
- premise defaults, implications, and validity rules; and
- the exceptional custom metrics and instructions that cannot be expressed as data.

An Authority is principally a data provider. It does not make live game-state decisions. Published
Terraforming Mars is one Authority; a variant that changes fundamental meanings is another.
Authorities may reuse Class Names because their namespaces never mix.

An Authority may be assembled from internal bundles, but callers and playable games still use
exactly one Authority. Identical declarations may coalesce. Conflicting declarations for one Class
Name or ambiguous ownership of a Module are invalid.

## One class catalog, projected per game

Within an Authority, every Class Name has one meaning. The Authority loads and validates one master
`ClassTable`. A playable game receives a projection backed by that master:

- selected classes are active;
- Authority-known inactive classes are behaviorless phantoms; and
- unknown names remain errors.

The universal catalog is a schema, not a playable Game World. It is never instantiated because it
contains mutually exclusive maps, modes, and replacement classes.

## Module

A **Module** is an affirmative, immutable singleton component carrying ambient behavior for one
realized choice. Base rules, expansions, maps, modes, and variants are Modules. The exact live Module
set is the complete statement of a game's general rules.

Each Module selects classes to activate or deactivate. Selection may depend on the complete
configuration. Structural reachability may activate dependencies, but it may not activate an
unselected Module or defeat an explicit exclusion.

## Configuration and premise

`GameConfig` is unresolved user intent: unordered included and excluded class-name sets plus player
names in seat order. Defaults, implications, selection policy, and validation resolve it to a
`GamePremise`.

A `GamePremise` is the complete immutable input needed to construct equivalent Game Worlds. It
contains only:

1. one Authority;
2. selected Module Class Names;
3. signed selections for other Authority classes;
4. user-facing player names in seat order; and
5. exact concrete non-singleton types to instantiate once.

Occupied seats activate canonical `Player1` through `PlayerN`. Configured player names are
Vocabulary aliases, not Class identities. Initial state is not an unrestricted Pets script.

Availability and existence are distinct. With Colonies active, eligible colony classes are active
so effects can select them, while setup creates only the chosen starting colony components.

Defaults are evaluated against explicit inclusions. Naming a competing choice suppresses its
default; an explicit exclusion defeats defaults and implications. Selecting named milestones or
awards chooses that exact pool. Selecting colony tiles also requests their initial components.

## Bundle

A **Bundle** is an internal unit of ownership, provenance, distribution, and loading. It may provide
declarations, definitions, premise metadata, and custom implementations. It is not selected directly
and never becomes a live component.

A Module may select a whole content category from a named bundle. This supports an expansion adding
its cards or milestones without exposing arbitrary bundle selection. Otherwise bundle provenance has
no game semantics.

## Invariants

- One game uses one Authority.
- One Class Name has one meaning inside that Authority.
- Every game table projects the Authority's validated master table.
- Modules, signed class selections, seats, and exact requested initial types fully determine the
  premise.
- Structural activation cannot select an unrequested Module or override an exclusion.
- Eligible availability and initial existence remain separate.
- Multiple active replacements for one definition are invalid.
- Given an Authority, ambient behavior is a deterministic function of the live Module components.
