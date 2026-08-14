# Authorities, Modules, and game premises

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

> **Status:** This document describes the current Authority, Module, configuration, and premise
> model.

## Authority

An **Authority** is one coherent source of everything Solarnet may know about a game. It provides:

- every directly authored Rule-Class Declaration in its namespace;
- structured Definitions that generate Content-Class Declarations, such as cards, milestones, awards, maps, standard actions, and colony tiles;
- vocabulary and other descriptive metadata;
- declarative premise defaults and validity rules;
- the exceptional custom metric and instruction implementations that cannot be expressed as data.

An Authority is almost entirely a data provider, even though much of that data describes behavior. Custom Kotlin implementations are the deliberate exception, not a reason for the Authority to make live game decisions.

Published Terraforming Mars is one Authority. A versioned rebalance or a variant with changed fundamental behavior is another. Authorities may reuse a Class Name because their namespaces never mix.

Almost every operation uses exactly one Authority. An Authority may be assembled internally from several providers, but callers and playable games still see one Authority. Provider composition is an implementation detail. Identical Declarations may coalesce, but a Module must have exactly one bundle owner; ambiguous Module ownership is invalid.

The reusable API exposes `Authority`. `TfmAuthority` extends it with typed registries for cards, milestones, awards, maps, standard actions, and colony tiles. Generic class loading and engine operation depend only on `Authority`.

## One universal class catalog

Within one Authority, every Class Name has exactly one Declaration and one meaning. Identical Declarations from separate providers coalesce; differing Declarations for the same Class Name are an error. A replacement has its own Class Name, and the replaced Definition remains known but can be inactive. The published Deimos Down Definitions are therefore `Card039` and `CardX31`.

The complete Authority catalog must load and validate together. Each playable game receives a projection of that catalog: selected Classes are active and every other Authority-known Class is present as a behaviorless phantom. Rule Class versus Content Class records declaration provenance only; Class Loading erases that distinction. No game projection obtains Declarations or behavior from another provider.

The universal catalog is a schema, not a playable Game World. It is not instantiated because doing so would create mutually exclusive maps, modes, and alternative Singleton Components together.

## Module

A **Module** is an affirmative, immutable singleton component that carries ambient behavior for one realized game choice. Base behavior, expansions, maps, modes, and variants are all Modules. The exact set of Module components is the complete statement of the game's general behavior choices.

Each Module declares the classes it activates or deactivates. The Authority stores these selections directly by Module class name; there is no second Kotlin Module object duplicating that identity. A selection can be conditional on the complete configuration, which supports definitions such as a map milestone that exists only when a particular expansion is also selected. Structural activation may pull in dependencies, but it may not activate an unselected Module or reactivate an explicitly excluded class.

## GamePremise

A `GamePremise` is the complete immutable input needed to construct equivalent playable Game Worlds. It contains only:

1. one Authority;
2. the selected Module class names;
3. signed selections for other individual classes;
4. exact concrete non-singleton types for which initialization creates one instance each.

Actors derive from positively selected player classes plus the administrative Engine actor. Active and phantom classes derive from the Authority, Modules, and signed selections. Initial state is not an unrestricted list of Pets instructions.

Availability and initial existence are separate. With Colonies active, all eligible colony-tile classes are active so later effects can select them, while setup creates only the chosen starting tile components. An eligible but initially unchosen tile has count zero.

`GameConfig` is the unresolved expression of user intent. Its included and excluded class-name sets have no ordering semantics. Defaults, implications, selection policies, and validation convert it into a `GamePremise`. The Terraforming Mars resolver currently accepts Modules, players, and initial colony tiles. It deliberately does not support adding or excluding individual cards.

## Bundles

A **Bundle** is an internal unit of file ownership, provenance, distribution, and loading. It may contribute declarations, structured definitions, premise metadata, and custom implementations. It is not selected directly by a game and never becomes a live component.

Bundle provenance normally has no semantic effect. The one deliberate selection facility is that a Module may activate a whole category from a named bundle, such as all cards, milestones, awards, maps, standard actions, colony tiles, or auto-loaded classes. This supports relationships such as an expansion adding its milestone definitions to a selected map without exposing arbitrary per-definition selection. When no explicit mapping is supplied, a Module selects all categories from its own bundle.

## Required invariants

- A game and almost every related operation use exactly one Authority.
- Every Class Name has one meaning within an Authority; identical Declarations coalesce and differing Declarations are invalid.
- The complete Authority class catalog loads and validates together.
- Every game table is a projection of that catalog, with inactive known classes represented as phantoms.
- A premise contains only the Authority, Modules, signed individual class selections, and exact initial concrete non-singleton types.
- Bundles affect organization except for a Module's explicit bundle-wide category selections.
- Eligible class availability and initial component existence are separate facts.
- Structural activation cannot select an unrequested Module or defeat an exclusion.
- Multiple simultaneously selected replacements for one definition are invalid.
- Given one Authority, the ambient behavior is a deterministic function of the exact Module components present.
