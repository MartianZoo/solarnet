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
true in the settled selection after disregarding its own target. This selection is resolved before
class projection. Initialization
creates sources before their constructively selected targets, while still creating Modules needed
to evaluate a source's gates first. Thus the declaration of A, rather than B or a central registry,
owns “A causes B.” Other structural reachability may
activate dependencies, but it may not activate an unselected Module or defeat an explicit
exclusion.

Module premise policy is authored with ordinary Requirement-valued Pets properties.
`autoSelectWhen` selects an unmentioned Module when its condition holds; automatic selections
resolve to an order-independent fixed point and an explicit exclusion wins. Each candidate's
condition is evaluated without counting that candidate itself, and an automatic selection is
retracted when later selections make its condition false. A nonconverging set of defaults is
invalid. By contrast, an explicit exclusion that contradicts an active constructive provenance
edge makes the configuration invalid.
`premiseRequirement` is checked against the completed projection when that Module is selected.
Module invariants provide the ordinary exact-count rules that are also meaningful in the live
World. `Class<T>` representatives describe that already-fixed projection: required representatives
are declared with invariants, not created by triggered instructions.

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
so effects can select them, while premise construction creates only the chosen starting selection
representations. In solo play four are selected; setup asks the player to remove one
`ColonyTileSelection` before continuing.

Defaults and active provenance are evaluated against the growing Module selection. Naming a
competing choice can make a default condition false; an explicit exclusion defeats it. In
multiplayer, each map selects the concrete members of its printed milestone and award pool
superclasses. Venus Next selects its single published milestone and award directly as conditional
bundle content. Explicitly naming any milestones or awards makes that category an exact pool, so
named goals replace only their own category. Selecting colony tiles also requests their initial
components. Solo Colonies uses three tiles, two-player Colonies uses five, and games with at least
three players use two more tiles than players.

Each concrete `MarsMap` is itself a Module. `TharsisMap`, `HellasMap`, and the other map names
therefore identify both the immutable premise choice and the live board component; there is no
parallel map option component. `TerraformingMars` selects `TharsisMap` only when no map is already
selected. Creating the selected map creates all of its Areas through the map instruction.

`PreludeExpansion` supplies the Prelude rules and phase. It selects `Prelude1Deck` by default and
requires at least one `PreludeDeck`. The original deck may therefore be explicitly excluded only
when another deck such as `Prelude2Expansion` is selected. `Prelude2Expansion` supplies its card
pool and constructively selects `PreludeExpansion`; it therefore cannot be configured without the
Prelude rules, while the phase and solo generation adjustment still come only from
`PreludeExpansion`.

## Bundle

A **Bundle** is an internal unit of ownership, provenance, distribution, and loading. It may provide
declarations, definitions, and custom implementations. It is not selected directly and never
becomes a live component.

A Module named for its owning Bundle selects that Bundle's ordinary cards and colony tiles. A map
Module selects its own map definition, areas, and the concrete milestone and award Classes under
the pool superclasses named by its map metadata. Exceptional cross-Bundle or narrowed selections
remain expressible, but Canon's ordinary expansions do not require a central registry to restate
their ownership.

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

**Status: current.** Constructive reachability, bundle-derived ambient compatibility,
exact-uninhabited premise viability, and explicit Module ownership of externally issued protocols
are implemented.

### Goals

Projection is premise semantics, not dead-code optimization. It must simultaneously provide:

1. **Isolation.** A Class unnecessary or forbidden in one game contributes no Components, behavior,
   singleton, or subtype choice there.
2. **Optional reference.** An active declaration may observe a concept that is uninhabited in this
   game without importing the feature that introduced it.
3. **Derived content compatibility.** Content Classes should not repeat expansion prerequisites
   already implied by the Classes they reference, whether their declarations come directly from
   Pets or from structured card/map data.
4. **Faithful content.** Explicit selection must not bypass an expansion dependency and leave a
   card executable but materially unlike itself.
5. **Early explanation.** Premise construction should distinguish content that is incompatible from
   content that is merely impossible to instantiate.

Uninhabited does not mean merely "currently unavailable." It means that the nominal concept is
known but its domain is provably empty in this game. Because that is genuine premise meaning, using
Active Classes to enumerate the selected milestone or award pool is principled rather than an
optimization leak.

### Bundle-derived ambient compatibility

A Bundle with a same-named Module makes its ambient declarations available through that Module. A
map Bundle without a same-named Module uses the disjunction of its map Modules. A data-only Bundle
with neither has no implicit availability boundary. This convention covers ordinary expansions,
single-map Bundles, and the two-map Hellas/Elysium and Utopia/Cimmeria Bundles without per-Class
properties.

Availability is not activation. Selecting `TerraformingMars` makes its ambient vocabulary,
including `MultiplayerMode`, available, while the selected player-count Module still decides which
mode is active. A nonconstructive gate or Trigger may mention an available but uninhabited Class and
go silent intentionally. This keeps mode-conditional cards such as Vitor available in Solo;
compatibility does not promise that every conditional branch executes in every game.

The boundary applies to hand-authored Class declarations and structured standard-action
declarations. Cards, maps, areas, milestones, awards, colony tiles, and card-local generated
Classes remain independently selectable content; merely residing in an expansion Bundle does not
make a Definition expansion-dependent. Module Classes are never availability-locked: premise
selection alone decides whether a Module is active.

For content compatibility, inspect every semantic Class reference in the Definition's lowered
declaration. Ordinary Module selection conjoins the owning Bundle Modules of those ambient Classes
with the Definition's authored automatic-selection condition. An explicit individual inclusion
must satisfy the derived Bundle condition and any separate non-Bundle compatibility condition, but
may still override ordinary pool-selection policy. Thus a Colonies card that only counts colonies
is just as Colonies-dependent as one that places a colony.

`VenusTag` and `VenusStep` are both ambient declarations of the Venus Next Bundle and therefore make
referencing content Venus-dependent. `WorldGovernmentTerraforming` and `FirstPlayerOcean` are shared
protocols in the base Bundle, so `WorldGovernmentOption` and non-Venus cards may use them without
enabling Venus Next. `PreludeCard` belongs to the Prelude Bundle; Valley Trust's mandate reference
therefore derives its Prelude dependency without a card property.

Concrete awards retain their authored multiplayer-only condition. Explicit selection checks that
condition too, so solo cannot bypass the boundary.

### Projection closure

For **projection closure**, classify references by what execution demands:

- Structural positions such as supertypes and Dependency bounds, constructive positions such as a
  gain or transmutation destination, deck identity, and Custom implementation dependencies are hard
  references. A hard reference activates an available Class.
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
reference to an ambient Class whose owning Bundle is unavailable rejects the premise as broken.

The important cases then fall out without card-specific rules:

| Reference in selected content | Projection consequence | Meaning if the Class stays uninhabited |
|---|---|---|
| Count, `MAX 5`, or Trigger | Does not activate it | Exact zero, true upper bound, or permanently silent Trigger; still viable |
| Positive `MIN` needed to instantiate or play the content | Does not activate it | Lawful but impossible; unviable |
| Reachable gain or other constructive use | Activates it if available | Broken if its owning Bundle keeps it unavailable |
| Constructive use below a provably false Trigger | Does not activate it | Unreachable and therefore harmless |

### Viable, unviable, and broken content

Compatible selected content can still have one of three projection outcomes:

- **Viable:** the Definition can participate faithfully. References to uninhabited Types may count
  zero, make a `MAX 5` Requirement true, or make a Trigger permanently silent.
- **Unviable:** the Definition does not demand forbidden state, but it can never be instantiated or
  complete a reachable mandatory entry point. A positive `MIN` play Requirement over uninhabited
  `VenusStep` is the canonical case. A reachable mandatory removal from a permanently empty domain
  has the same character.
- **Broken:** reachable behavior demands an ambient Class whose owning Bundle is unavailable.

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
2. Derive Definition compatibility from all semantic references and filter bundle-selected
   content.
3. Apply explicit individual content inclusions and exclusions, rejecting incompatible inclusions.
4. Form the active closure from roots and reachable hard references, respecting Bundle availability
   and exclusions.
5. Leave every other Authority-known Class uninhabited.
6. Validate selected content for viability and report unviable and broken paths separately.

A future retained analysis result should explain every decision, including which reference derived
an automatic exclusion and the complete hard-reference path that activated or rejected a Class.
Current failures identify the selected Definition or immediate hard-reference source and the
missing owning Module.

## Non-Canon Kotlin expansion-coupling audit

**Status:** verified current production code. This inventory excludes the `tfm-canon` module, tests,
and benchmarks. Severity describes pressure on the design boundary, not a conclusion that every
named domain concept should become generic.

1. **Severe — workflow hard-coding.** `engine/.../TfmWorkflow.kt` exposes `PreludePhase`, tests for
   `PreludeExpansion`, and grants the two Prelude plays itself. This is the clearest architectural
   leak: the base Kotlin workflow knows how one optional Module changes phase topology. The native
   workflow direction in `WORKFLOW.md` should remove this dependency.
2. **Moderate — Colonies remains privileged in premise infrastructure.**
   Concrete colony tiles and their immediate or delayed `ColonyTileSelection` representations are
   ordinary Pets classes. `pets/.../TfmAuthority.kt` still recognizes the `ColonyTile` hierarchy when
   constructing initial component Types; `pets/.../BundleContentSelection.kt` retains
   `COLONY_TILES` as a dedicated content kind so every available tile remains active for mid-game
   additions. These are premise responsibilities rather than a parallel class-definition format,
   but a future general model for configured starting components could remove the remaining
   expansion names.
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
