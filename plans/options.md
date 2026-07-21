# Game options and content selection

This records the intended configuration model, not a detailed implementation plan. Some names in
the examples are prospective canonical names rather than names already implemented.

## Configuration is a signed list of class names

The REPL, text UI, and files should share one small configuration language. A plain class name
selects something; a leading `-` counteracts a selection:

```
Player1
CorporateEra
ElysiumMap
ElysiumMilestones
Tr63Mode
PreludeExpansion
VenusNextExpansion
-Hoverlord
SkipWorldGovernment
Io
Enceladus
Pluto
SelfReplicatingRobots
-TradeEnvoys
-UndergroundDetonations
```

This signed selection is the primary representation. Rich Kotlin objects closely mirroring every
official option are not. Kotlin APIs may expose convenient derived views of the resolved setup.
Legacy clients may also provide compact option codes or make random choices, but those are client
conveniences that produce the same exact selection before core setup begins.

Canon interprets each selected name according to what it denotes. It may be a pure game option, a
player or map choice, an individual definition such as a card or colony tile, or a named content
group such as a map's milestones. Selecting a definition makes it active content; it does not mean
gaining that component into game state. A pure game option is represented in game state by a
singleton of the same name before `SetupPhase` so that its rules can be ordinary Pets rules. Player
entries determine the player count; solo-only selections are valid only with one player.

## Counteractions mask defaults before expansion

Selections may contribute other selections as defaults. `TerraformingMars`, for example, should
select `CorporateEra` by default. An explicit `-CorporateEra` masks that selection before its
defaults are expanded, so neither its singleton nor the large set of cards it would contribute is
selected. Counteractions take precedence independently of file order; they are not cleanup applied
after all default content has already been loaded.

An independently named item may still be selected from a counteracted group. Its own semantic
requirements must still hold. For example, naming one card supplied by the Corporate Era raw bundle
need not re-enable the `CorporateEra` option or the rest of its default card set.

The same mechanism handles smaller adjustments. Venus may contribute Hoverlord in addition to a
map's five milestones, while `-Hoverlord` removes just that contribution. Card, milestone, award,
and colony selections use stable identities scoped by definition kind.

## Available bundles, chosen options, and selected bundles

Raw bundles available to Canon determine which selector names and defaults are available. The
resolved signed selection then determines the minimal raw bundles needed to provide the surviving
options and content. Thus available bundles determine the options available, while chosen options
determine the bundles selected; these are two different stages, not a cycle.

A bundle is only a raw grouping of Pets declarations, JSON definitions, and custom Kotlin
implementations. Its identity describes provenance and resource location and has no gameplay
meaning. A provider bundle may be loaded for one selected item without enabling a same-named
expansion option or its other content. Option, content-group, and bundle names may temporarily
coincide, but code must not depend on that coincidence.

Canon is the catalog that relates selectors to defaults and raw providers. A ruleset is the lazy
composition of the selected bundle payloads. `GameSetup` is the immutable resolved result: the
signed choices, their exact active content, and the selected ruleset. `GameReader.ruleset` is that
same ruleset.

## Filtering, replacement, and class loading

Semantic requirements on definitions refer to selected game options, not to raw bundle presence.
Canon determines provider-bundle needs. Definitions whose requirements fail and definitions masked
by counteractions are absent before replacement, class indexing, and class loading.

A replacement removes its same-kind target before either definition becomes a class. Replacement
chains are transitive; targets must be known to the catalog even when absent from this setup; cycles
and two applicable replacements for one target are errors. Replacement does not override arbitrary
Pets declarations or custom implementations.

The class loader roots the surviving option singletons and active definitions, then follows their
transitive class references. Merely loading a provider bundle must not make unrelated `AutoLoad`
declarations active. Identical declarations from multiple selected sources coalesce and retain all
contributing bundle provenance; nonidentical declarations with the same class name are errors.

## Setup rules and source ownership

Configuration constraints should be Pets rules where that is natural, such as requiring exactly
one map. Option-specific setup behavior should likewise come from the option's provider through
Pets effects and custom instructions. For example, the Colonies option interprets the exact selected
colony names, validates their number, and owns the `SetupPhase: ColoniesSetup` behavior. General
compositional workflow modification is not part of this design; explicit workflow handling may
remain where needed.

Every canonical `.pets` file, JSON file, and custom implementation has raw bundle ownership,
including unsupported data. The Pets runtime declarations in `pets/system.pets` are shared runtime
vocabulary and are not a bundle. JSON definitions derive their provenance from the bundle that
reads their directory rather than carrying raw bundle attributes. Canon's current raw directories
are `TerraformingMars`,
`CorporateEraExpansion`, `TharsisMap`, `HellasMap`, `ElysiumMap`, `VenusNextExpansion`,
`PreludeExpansion`, `ColoniesExpansion`, `PromoCardsBundle`, and `TurmoilExpansion`; `SoloMode` is
provided by `TerraformingMars` rather than being a raw bundle.

Canonical directories follow the `StandardFormBundle` resource contract: all `.pets` files and
supported conventionally named JSON files are discovered through the generated JVM/JavaScript
resource index, while unexpected files are reported. A directory neither declares nor synthesizes
a live bundle component merely by existing.
