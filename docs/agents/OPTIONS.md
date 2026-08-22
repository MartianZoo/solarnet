# Authorities, Modules, and game premises

**Status: current model through "Resolution order". Stronger closed-world viability proofs and
durable projection-decision explanations remain future work.**

## Authority

An **Authority** is one coherent namespace containing everything Solarnet may know about a game:

- authored Rule-Class declarations;
- structured definitions that generate Content-Class declarations;
- vocabulary and descriptive metadata;
- premise defaults and validity rules; and
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
- Authority-known inactive classes are uninhabited; and
- unknown names remain errors.

The universal catalog is a schema, not a playable Game World. It is never instantiated because it
contains mutually exclusive maps, modes, and replacement classes.

## Module

A **Module** is an affirmative, immutable singleton component carrying ambient behavior for one
realized choice. Base rules, expansions, maps, modes, content groups, and variants are Modules. The
exact live Module set is the complete statement of a game's general rules.

`Module` is an ordinary Pets superclass except where premise construction and initialization ask
whether a Class is its subtype. Its inherited rules make each concrete Module a permanent
singleton; its `autoSelectWhen` and `premiseRequirement` properties have meaning because the
Authority reads them. There is no separate Kotlin Module object or special component storage.

Each Module selects classes to activate or deactivate. Selection may depend on the complete
configuration. A constructive self-gain in an active Module is also **active provenance** for a
target Module: `A { This:: B }` lets A select B. A gated gain does so only when its Requirement is
true over the growing selection. This closure is resolved before class projection, and
initialization orders Modules observed by those gates before their sources. Thus the declaration of
A, rather than B or a central registry, owns “A causes B.” Other structural reachability may
activate dependencies, but it may not activate an unselected Module or defeat an explicit
exclusion.

Module premise policy is authored with ordinary Requirement-valued Pets properties.
`autoSelectWhen` selects an unmentioned Module when its condition holds; automatic selections
resolve to a fixed point and an explicit exclusion wins. `premiseRequirement` is checked against
the completed projection when that Module is selected. Module invariants provide the ordinary
exact-count rules that are also meaningful in the live World.

## Configuration and premise

`GameConfig` is unresolved user intent: unordered included and excluded class-name sets plus player
names in seat order. Defaults, selection policy, and validation resolve it to a
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

Defaults and active provenance are evaluated against the growing Module selection. Naming a
competing choice can make a default condition false; an explicit exclusion defeats it. Each map
constructively selects its printed milestone group and, in multiplayer, its printed award group.
Venus Next similarly adds its published milestone and award. `MilestonesAwardsExpansion` is a goal-
pool override, so those default group gains are inactive and the pool is exactly the explicitly
named milestones and awards. Explicit goal names remain valid without that option and replace only
their own category. Selecting colony tiles also requests their initial components.

Each concrete `MarsMap` is itself a Module. `TharsisMap`, `HellasMap`, and the other map names
therefore identify both the immutable premise choice and the live board component; there is no
parallel map option component. `TerraformingMars` selects `TharsisMap` only when no map is already
selected.

## Bundle

A **Bundle** is an internal unit of ownership, provenance, distribution, and loading. It may provide
declarations, definitions, and custom implementations. It is not selected directly and never
becomes a live component.

A Module named for its owning Bundle selects that Bundle's ordinary cards and colony tiles. A map
Module selects only its own map definition, even when its Bundle contains multiple maps. A named
definition-group Module selects definitions carrying that group. Exceptional cross-Bundle or
narrowed selections remain expressible, but Canon's ordinary expansions do not require a central
registry to restate their ownership.

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

## Settled projection-policy direction

**Status: current.** Constructive reachability, the two Class policies, exact-uninhabited premise
viability, and explicit Module ownership of externally issued protocols are implemented.

### Goals

Projection is premise semantics, not dead-code optimization. It must simultaneously provide:

1. **Isolation.** A Class unnecessary or forbidden in one game contributes no Components, behavior,
   singleton, or subtype choice there.
2. **Optional reference.** An active declaration may observe a concept that is uninhabited in this
   game without importing the feature that introduced it.
3. **Derived content compatibility.** Cards, milestones, awards, and similar Definitions should not
   repeat expansion prerequisites already implied by the Classes they reference.
4. **Deliberate composition.** A user may manually include automatically filtered content when it
   remains meaningful under the resulting projection.
5. **Early explanation.** Premise construction should distinguish content that is incompatible from
   content that is merely impossible to instantiate.

Uninhabited does not mean merely "currently unavailable." It means that the nominal concept is
known but its domain is provably empty in this game. Because that is genuine premise meaning, using
Active Classes to enumerate the selected milestone or award pool is principled rather than an
optimization leak.

### Two independent Class policies

"From an expansion" is not bundle provenance and cannot be one Boolean property. A Class may carry
two independent Module conditions:

- Its **automatic-selection requirement** says which Module must be selected before ordinary bundle
  policy includes content that semantically references the Class.
- Its **activation requirement** says which Module must be selected before a hard reference may make
  the Class active at all.

An activation requirement always contributes the same condition to automatic selection. The
converse does not hold: a Class can filter ordinary content selection while remaining pullable by
an explicit composition. Generated Content Classes do not acquire either condition merely from the
Bundle that stores them; otherwise individual content could never be composed deliberately.

| Class | Automatic-selection requirement | Activation requirement | Without Venus Next |
|---|---|---|---|
| `WorldGovernmentTerraforming` | none | none | A hard reference activates the shared mechanism. |
| `VenusTag` | `VenusNextExpansion` | none | Automatic content is filtered, but a manual hard reference may activate the tag. |
| `VenusStep` | `VenusNextExpansion` | `VenusNextExpansion` | It remains uninhabited; no reference may manufacture the Venus track. |

`PreludeCard` needs no authored selection property. Either published Prelude Module owns its Bundle
cards, whose deck declarations naturally activate their shared card back. Selecting Valley Trust
without a Prelude generation activates that card back through the corporation's mandate but no
concrete Prelude fronts; the resulting mandate is deliberately broken rather than silently choosing
a deck.

Concrete award Definitions have automatic- and activation requirements for `MultiplayerMode`.
Solo projection therefore leaves every concrete award class uninhabited, and explicit selection
cannot bypass that boundary.

This classification is semantic. `WorldGovernmentTerraforming` was first used by Venus but works
over whichever Global Parameters the projection supplies; Prelude 2 can therefore use the shared
mechanism. `VenusTag` is meaningful as standalone card vocabulary. `VenusStep` represents one part
of the Venus expansion's ambient game state and is not meaningful without that Module.

### Two analyses of a Definition

Automatic selection and projection closure ask different questions and must not share one coarse
"mentions" test.

For **automatic content selection**, inspect every semantic Class reference in the Definition's
lowered declaration. Conjoin the referenced Classes' automatic-selection requirements with the
Definition's authored automatic-selection condition. This derives today's repeated `VenusTag`,
`VenusStep`, and `Colony` expansion conditions. Named content groups and their source Module effects
express default-pool facts such as which map prints a milestone or award. Authored automatic-
selection conditions record only remaining nonderivable availability facts; they do not restate an
expansion condition already implied by referenced Classes.

An explicit individual inclusion overrides this automatic content filter. It does not override a
Class's activation requirement.

For **projection closure**, classify references by what execution demands:

- Structural positions such as supertypes and Dependency bounds, constructive positions such as a
  gain or transmutation destination, deck identity, and Custom implementation dependencies are hard
  references. A hard reference activates a pullable Class.
- Counts, Metrics, Requirements, Triggers, Complements, and nonconstructive changes do not by
  themselves activate their referenced Classes. An uninhabited Class contributes an exactly empty
  domain.
- Reachability matters. A hard reference beneath a Trigger or gate that is provably false because
  of uninhabited Types is harmless; execution cannot reach it. A conservative analysis may treat
  anything it cannot prove unreachable as reachable.

The loader applies this systemic role-and-reachability rule instead of treating every mention as an
activation edge. It rechecks the closure as Classes activate and proves false gates from exact zero
counts over uninhabited Types. Trigger positions are wholly nonconstructive: Terraforming Mars and
Solo mode explicitly own the protocol Classes issued by workflow and gameplay APIs. A hard
reference to a Class whose activation requirement is false rejects the premise as broken.

The important cases then fall out without card-specific rules:

| Reference in selected content | Projection consequence | Meaning if the Class stays uninhabited |
|---|---|---|
| Count, `MAX 5`, or Trigger | Does not activate it | Exact zero, true upper bound, or permanently silent Trigger; still viable |
| Positive `MIN` needed to instantiate or play the content | Does not activate it | Lawful but impossible; unviable |
| Reachable gain or other constructive use | Activates it if pullable | Broken if an activation requirement keeps it uninhabited |
| Constructive use below a provably false Trigger | Does not activate it | Unreachable and therefore harmless |

### Viable, unviable, and broken content

Manual inclusion can bypass automatic filtering, after which premise validation assigns one of
three outcomes:

- **Viable:** the Definition can participate faithfully. References to uninhabited Types may count
  zero, make a `MAX 5` Requirement true, or make a Trigger permanently silent.
- **Unviable:** the Definition does not demand forbidden state, but it can never be instantiated or
  complete a reachable mandatory entry point. A positive `MIN` play Requirement over uninhabited
  `VenusStep` is the canonical case. A reachable mandatory removal from a permanently empty domain
  has the same character.
- **Broken:** reachable behavior demands a Class whose activation requirement is unsatisfied. An
  unconditional `VenusStep` gain without `VenusNextExpansion` is the canonical case.

Premise construction rejects both unviable and broken selected content, with different diagnostics.
It must not silently activate a locked Class or defer an inevitable failure until gameplay.

The first viability analysis need only exploit exact facts about uninhabited Types. A later
closed-world extension could prove facts not directly involving them—for example, that Law Suit is
unviable in solo because no opponent-dependent attack record can ever exist. That is the same
semantic category but a substantially stronger satisfiability analysis, not a prerequisite for the
projection change.

### Resolution order

The premise pipeline is:

1. Resolve Modules, defaults, and explicit exclusions.
2. Derive automatic Definition conditions from all semantic references and filter bundle-selected
   content.
3. Apply explicit individual content inclusions and exclusions.
4. Form the active closure from roots and reachable hard references, respecting activation
   requirements and exclusions.
5. Leave every other Authority-known Class uninhabited.
6. Validate selected content for viability and report unviable and broken paths separately.

A future retained analysis result should explain every decision, including which reference derived
an automatic exclusion and the complete hard-reference path that activated or rejected a Class.
Current failures identify the selected Definition or immediate hard-reference source and the exact
false requirement.

## Non-Canon Kotlin expansion-coupling audit

**Status:** verified current production code. This inventory excludes the `canon` module, tests,
and benchmarks. Severity describes pressure on the design boundary, not a conclusion that every
named domain concept should become generic.

1. **Severe — workflow hard-coding.** `engine/.../TfmWorkflow.kt` exposes `PreludePhase`, tests for
   `PreludeExpansion`, and grants the two Prelude plays itself. This is the clearest architectural
   leak: the base Kotlin workflow knows how one optional Module changes phase topology. The native
   workflow direction in `WORKFLOW.md` should remove this dependency.
2. **Significant — Colonies is privileged throughout premise infrastructure.**
   `pets/.../TfmAuthority.kt` recognizes colony definitions during naming, configuration,
   validation, initial-Type construction, lookup, bundle selection, and Authority composition.
   `pets/.../BundleContentSelection.kt` adds `COLONY_TILES` as a dedicated content kind;
   `pets/.../ColonyTileDefinition.kt` generates Colonies-specific declarations; and
   `pets/.../JsonReader.kt` has a dedicated colony format. Some structured colony metadata is an
   honest domain boundary, but selection and premise assembly should not acquire an expansion
   exception merely because that metadata produces initial components.
3. **Moderate — expansion concepts appear in engine and card APIs.**
   `engine/.../TfmGameplay.kt` publishes `playPrelude` and `venusPercent`.
   `pets/.../CardDefinition.kt` and `pets/.../TfmClasses.kt` make Prelude a built-in deck kind and
   give it special validation. These are real dependencies, but `PreludeCard` and `VenusStep` are
   legitimate Terraforming Mars concepts; removing their names is not inherently a simplification.
4. **Low — the legacy script boundary enumerates concrete products.**
   `script/.../OptionCodeTranslation.kt` names Corporate Era, the map products, Milestones and
   Awards, Venus, Prelude, Colonies, Turmoil, and Promos. `script/.../ScriptSession.kt` and
   `script/.../commands/NewGameCommand.kt` carry a separate selected-colonies input path. Explicit
   product names are reasonable in this adapter, while that separate colony path reflects the
   deeper premise asymmetry above.
5. **Minimal — descriptions, samples, reports, and comments.**
   `language/.../TerraformingMarsDescribers.kt` describes Venus and colony concepts;
   `script/.../tfm/script/SampleGames.kt` and `script/.../tfm/script/commands/TfmSampleCommand.kt`
   demonstrate Prelude and Venus play; `tools/.../TypeStructureReport.kt` and
   `tools/.../StandardResourceMonotonicityReport.kt` deliberately select expansions for reports;
   and `pets/.../ast/Expression.kt` mentions `VenusTag` only in documentation. These do not put
   expansion policy into reusable runtime behavior.
