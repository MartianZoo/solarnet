# Bundles

## Intended principles and invariants

### Options, raw data, and loading are distinct

`GameOptions` contains exact semantic choices supplied by a client: player count, enabled rule
options, the map option, and exact colony tiles when applicable. Core setup never randomizes or
fills in missing choices. A limited client such as the REPL may do that before creating
`GameOptions`.

Canon is a catalog. It knows which raw bundles provide each canonical game option and computes the
bundle set needed for a `GameOptions` value. This mapping may be hardcoded while the option model is
small. In time an option may request only part of a product's content; including that product's
bundle must not thereby enable its expansion option.

A `Bundle` is one raw grouping of Pets declarations, JSON-backed definitions, and custom Kotlin
implementations. A `Ruleset` is the lazy composition of exactly the bundles Canon selected. Loading
a bundle makes its data available and carries no gameplay meaning by itself.

`GameSetup` is the complete, immutable, non-random result: the exact options and the already
selected ruleset that provides them. `GameReader.ruleset` is that same ruleset. The current first
step still activates every definition in a selected expansion bundle; a later active-content view
will allow individual content selection without changing these boundaries.

### Bundles and game options have separate identities

A bundle identity describes raw source provenance and resource location. It is never itself a live
game component. An enabled rule option may have a live component so its Pets effects implement that
option. For now several option names and bundle names remain identical as a migration convenience;
code must not rely on that coincidence.

The Pets runtime declarations in `pets/system.pets` are available to every ruleset and are not a
bundle. Every canonical game enables the `TerraformingMars` option. Short letter codes are only a
client convenience for naming options.

### Class loading reflects the configured game

The ruleset exposes declarations from the needed raw bundles. The class loader roots enabled option
components, active definitions, and every class reached transitively from those roots.

We aim not to load classes that cannot be needed in the game. Class presence has rule meaning, so
optional-bundle `AutoLoad` declarations must eventually become option/content activation roots;
including a bundle for one item must not activate its other rules.

Exactly identical class declarations contributed by several sources coalesce. Nonidentical
declarations sharing a class name are an error. The combined declaration retains enough provenance
to identify all contributing bundles.

### Definitions are filtered and replaced before becoming classes

JSON definitions derive their bundle membership from the bundle object that reads their containing
directory; JSON and runtime definition objects do not contain a `bundle` attribute.

A card or milestone currently names comma-separated `requiredBundles`. This must become a semantic
`requiredOptions` condition: raw bundle inclusion is not a game rule. Provider-bundle dependencies
are computed by Canon rather than authored in game data.
Definitions whose requirements fail are absent before replacement and class loading.

Every replaceable definition has a stable identity scoped by definition kind. Its `replaces`
value removes the same-kind target from the applicable content before either definition is indexed by
class name or converted into class declarations. Removing a definition also removes declarations
generated from it.

Replacement chains resolve transitively. The target must be known to the underlying ruleset,
although it may be absent from this setup. Cycles and two applicable replacements for the same
target are errors. Replacement does not override arbitrary Pets declarations or custom Kotlin
implementations.

### Option rules are ordinary game rules where practical

Enabled option components exist before `SetupPhase`. Option-specific setup behavior should be
provided by the option's bundle and expressed through Pets effects and custom instructions where practical.
Configuration constraints should likewise be expressed as game rules when natural; for example,
exactly one map should follow from an invariant such as `HAS =1 Map`.

The bundle migration does not need to solve general compositional workflow modification. Explicit
workflow handling for Prelude and similar features can remain until a convincing general model
emerges.

### Files have bundle ownership

Every canon `.pets` file, JSON file, and custom implementation belongs to a directory under
`canon/bundles`, including unsupported or currently unread data. File ownership does not by itself
make a file active runtime content.

Canonical directories use the `StandardFormBundle` naming contract. Every `.pets` file is loaded, and
conventionally named JSON files such as `cards.json5`, `actions.json5`, and `maps.json5` are loaded
when present. The build-generated resource index lets this work on both JVM and JavaScript and lets
the loader warn about unexpected files. Bundle identities are source provenance, not Pets classes;
directories neither declare nor synthesize bundle components.

## Other decisions

### Canon bundles

Canon has these bundle directories:

- `TerraformingMars`
- `CorporateEraExpansion`
- `TharsisMap`
- `HellasMap`
- `ElysiumMap`
- `VenusNextExpansion`
- `PreludeExpansion`
- `ColoniesExpansion`
- `PromoCardsBundle`
- `TurmoilExpansion`

The physical names will later be reconsidered independently of option names (for example a
`VenusNext` bundle can provide the `VenusNextExpansion` option). `SoloMode` is provided by the
`TerraformingMars` bundle and is not a bundle. `TurmoilExpansion` remains a raw bundle even while
its only supported content is a few cards.

### Shared declarations

Shared vocabulary need not have one exclusive bundle owner. Card-resource classes such as `Floater`
are generated on demand from the cards that use them, and identical declarations from several
bundles coalesce.

### Specific bundle behavior

A likely Colonies design gives `ColoniesExpansion` the effect
`SetupPhase: ColoniesSetup`, with `ColoniesSetup` supplied as a custom class by that bundle. Using
`SetupPhase` avoids relying on singleton creation order.

`SoloMode` is selected explicitly in exact `GameOptions`; the REPL currently infers it for a
one-player legacy command. Its absence requires at least two Players.

Venus Next adds Hoverlord as a sixth available milestone; it does not replace one of the map's five
milestones.

Double Down currently uses `requiredBundles = PreludeExpansion`; this should become
`requiredOptions = PreludeExpansion` so loading Prelude data without enabling its rules is not enough.

### Option-specific setup values

`GameOptions` stores the exact Colonies tile identities. When `ColoniesExpansion` is enabled their
count must be exactly correct; otherwise the set must be empty. The Colonies option owns the setup
behavior that interprets those identities. The REPL may select tiles randomly before constructing
the options, while follow-mode clients and logged-game tests name what actually occurred.
