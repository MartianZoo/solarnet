# Bundles

## Intended principles and invariants

### Configuration, data, and loading are distinct

`GameSetup` is a simple value describing what the user configured. It does not expand bundle
relationships, choose derived defaults, apply replacements, or otherwise represent the full
meaning of that configuration. Bundle-specific options such as desired colony tiles must fit this
role as user choices rather than resolved game data.

The abstraction is named `Ruleset`. A ruleset supplies Pets declarations, JSON-backed definitions,
and custom Kotlin implementations, and can be composed from other rulesets. Canon is the
composition of all canonical bundle rulesets.

Each game has one resolved ruleset derived from its setup. It contains only the contributions
applicable to that game after bundle selection, load requirements, and replacements. The class
loader and all runtime definition and custom-class lookups use that same resolved ruleset; there is
no separate unfiltered view inside the game.

### Bundles have one identity throughout the system

Every ordinary selected bundle has exactly one live game component instance. Its directory,
Kotlin package, bundle object, and Pets component use the same UpperCamelCase name. Bundle
membership never adds a Pets dependency or type argument to content classes.

`System` is always included and is not user-selectable. Every canonical game includes
`TerraformingMars`. Other bundles are selected by their full identities; short letter codes are
only client conveniences.

### Class loading reflects the configured game

The resolved source exposes declarations only from applicable content. Within that source, every
`AutoLoad` class is loaded, along with the roots supplied by the setup and definitions and every
class reached transitively from those roots.

We aim not to load classes that cannot be needed in the game. Class presence has rule meaning, so
bundle filtering must happen before `AutoLoad` discovery and class reachability.

Exactly identical class declarations contributed by several sources coalesce. Nonidentical
declarations sharing a class name are an error. The combined declaration retains enough provenance
to identify all contributing bundles.

### Definitions are filtered and replaced before becoming classes

JSON definitions derive their bundle membership from their containing directory; JSON no longer
contains a `bundle` attribute. Runtime definitions retain their bundle provenance.

A definition may have a Pets `loadRequirement`. The requirement is tested against the configured
game, including the presence of selected bundle components. It filters content and does not select
additional bundles. Definitions whose requirements fail are absent before replacement and class
loading.

Every replaceable definition has a stable identity scoped by definition kind. A `replacesId`
removes the same-kind target from the applicable content before either definition is indexed by
class name or converted into class declarations. Removing a definition also removes declarations
generated from it.

Replacement chains resolve transitively. The target must be known to the underlying ruleset,
although it may be absent from this setup. Cycles and two applicable replacements for the same
target are errors. Replacement does not override arbitrary Pets declarations or custom Kotlin
implementations.

### Bundle rules are ordinary game rules where practical

Selected bundle components exist before `SetupPhase`. Bundle-specific setup behavior should be
owned by the bundle and expressed through Pets effects and custom instructions where practical.
Configuration constraints should likewise be expressed as game rules when natural; for example,
exactly one map should follow from an invariant such as `HAS =1 Map`.

The bundle migration does not need to solve general compositional workflow modification. Explicit
workflow handling for Prelude and similar features can remain until a convincing general model
emerges.

### Files have bundle ownership

Every canon `.pets` file, JSON file, and custom implementation belongs to a directory under
`canon/bundles`, including unsupported or currently unread data. File ownership does not by itself
make a file active runtime content.

## Other decisions

### Canon bundles

Canon has these bundle directories:

- `System`
- `TerraformingMars`
- `CorporateEraExpansion`
- `TharsisMap`
- `HellasMap`
- `ElysiumMap`
- `VenusNextExpansion`
- `PreludeExpansion`
- `ColoniesExpansion`
- `SoloMode`
- `PromosExpansion`
- `TurmoilExpansion`

`System` owns the declarations currently in `system.pets`. It is bundle-shaped for organization
and composition, but need not have a live bundle component because `System` already has a different
meaning in Pets.

`TerraformingMars` replaces the proposed name `Base` and uses the existing `TerraformingMars`
singleton as its bundle component. `TurmoilExpansion` is a real bundle even while its only
supported content is a few cards.

### Shared declarations

Shared vocabulary need not have one exclusive bundle owner. `Floater`, for example, may be declared
identically by Venus Next and Colonies, or also by `TerraformingMars`.

### Specific bundle behavior

A likely Colonies design gives `ColoniesExpansion` the effect
`SetupPhase: ColoniesSetup`, with `ColoniesSetup` supplied as a custom class by that bundle. Using
`SetupPhase` avoids relying on singleton creation order.

`SoloMode` may be selected explicitly or inferred by a client. Its absence requires at least two
Players. Exactly where that validation is performed is not yet decided.

Venus Next adds Hoverlord as a sixth available milestone; it does not replace one of the map's five
milestones.

Double Down's `loadRequirement` is `HAS PreludeExpansion`. Because load requirements test bundle
presence rather than traversing dependencies, mutually conditional content in two selected bundles
works without special cycle handling.

### Composition details still open

We have not decided how a simple `GameSetup` stores bundle-specific options.
