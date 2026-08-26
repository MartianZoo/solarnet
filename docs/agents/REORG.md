# Module organization

> **Read when:** moving a production package, test suite, resource, dependency, application, or
> analysis tool between Gradle modules.
>
> **Skip when:** changing behavior inside its existing owner. Use [BOUNDARIES.md](BOUNDARIES.md) only
> for a remaining generic/Terraforming Mars seam.
>
> **Status:** current ownership. The reorganization is complete; this document implies no remaining
> module, package, resource, or test moves.

## Repository entry points

- [`settings.gradle.kts`](../../settings.gradle.kts) — inspect the actual module set and centralized
  repositories.
- Each module's `build.gradle.kts` is the authority for current dependency direction; inspect only
  the source and destination modules involved in the proposed move.
- Package roots under `pets/src`, `engine/src`, `tfm-canon/src`, `tfm-engine/src`, `tfm-text/src`,
  and `tfm-tests/src` show current physical ownership.

## Governing rule

No non-Terraforming-Mars production source set may depend on a Terraforming-Mars module. The generic
Pets and engine layers must make sense without Canon or any other Terraforming Mars code.
The boundary should be strong enough that all `tfm` modules could later move to another source tree
or repository without reversing a dependency.

Module names should normally follow their principal `dev.martianzoo.*` package. Initially prefer an
enforced boundary over minimizing module count; consider recombination only after the dependency
directions are clean. Keep each Gradle module's physical source roots separate: merging them into a
shared package-shaped tree would obscure the ownership that the module split now makes explicit.
Package names express domain ownership, not merely physical Gradle ownership. A mixed leaf module may
therefore contain sibling roots such as `dev.martianzoo.script` and `dev.martianzoo.tfm.script` until
a concrete dependency split justifies separate modules; do not nest one beneath the other merely to
force a single package root.

## Current dependency graph

```text
engine     ─> pets
tfm-canon  ─> pets
tfm-engine ─> engine, pets, tfm-canon
tfm-text   ─> pets, tfm-canon
tfm-tests  ─> engine, pets, tfm-canon, tfm-engine
script     ─> engine, pets, tfm-canon, tfm-engine
repl       ─> script
web        ─> script
tools      ─> engine, pets, tfm-canon
benchmarks ─> engine, pets, tfm-canon, tfm-engine
```

Engine tests may depend on `tfm-canon` and `tfm-engine` as test-only scaffolding whenever the behavior
under test belongs to engine. Test ownership follows the behavior being proved, not the domain
vocabulary used to construct the scenario. This does not add a production dependency or change
production ownership; engine production code must never acquire the reciprocal dependency.

Generic client APIs remain packages within `engine`: every current production consumer also needs
engine implementation behavior. Terraforming Mars runtime and client-API packages similarly share
`tfm-engine`; their imports require both generic engine and Canon, but do not justify another module
between them. The leaf application modules are `script`, `repl`, `web`, `tools`, and `benchmarks`.
Their dependencies point inward to the core graph and create no reciprocal production dependency.

## Pets: language and static semantics

`pets` owns:

- parsing, canonical source rendering, AST, and transformations;
- Class declarations, loading, types, and projected Class Tables;
- the pure `GamePremise` data model;
- immutable generic task and event descriptions used both during execution and when recording what
  happened;
- generic system declarations in `system.pets`;
- a composable catalog contract for declarations and exceptional implementations; and
- the minimal host-query contract needed by world-dependent Pets evaluation.

Class Table projection is a core type-system operation. Given a catalog and exact premise, a static
program must be able to form the same projected table that engine would use. The absence of an
unselected Class is meaningful static information.

### Evaluation boundary

Requirement and Metric own the evaluation of their intrinsic tree structure. A small Pets-provided
interface supplies only state-dependent leaves positively required by current custom code, such as
type resolution and component counting. The initial interface must not expose tasks, event history,
engine mutation, or a general World handle. Workflow state is visible only if it is honestly modeled
as ordinary components.

Custom metrics query that interface. Custom instructions query it and return a Pets
`InstructionTree`, which re-enters normal engine preparation. Instruction itself remains inert Pets
syntax; engine lowers prepared instructions into its own internal executable representation.

This preserves an important semantic boundary: custom code can derive Pets behavior from current
game state, but cannot make task scheduling or diagnostic history part of the game rules.

The completed consumer audit found that current custom implementations need type resolution and
narrowing, Metric and component counts, matching-component enumeration, the Actor list for seat
order, and the selected Catalog for composable card/map metadata and canonical declaration order.
`GameReader` is the single interface for those needs. It exposes no tasks, history, mutation, or
World handle. The former `containsAny` query was needed only by engine preparation and was removed
from this public boundary.

### Config and premise

Config and premise are different data:

- A Terraforming Mars config captures user intent. It may contain unresolved choices such as asking
  for milestones to be selected from a pool.
- A generic Pets `GamePremise` contains the resolved outcome. The entire initial World is a
  deterministic function of that exact premise under the current implementation.

`tfm-canon` resolves its config into a premise. Both engine construction and static analysis consume
the resulting premise. Versioning of config resolution, Canon, and engine behavior is deliberately
out of scope for this design.

### Catalog

`Catalog` replaces the overloaded former static meaning of `Authority`. A catalog is a composable
universe of declarations, transformations, custom implementations, and associated content metadata
from which premises select.

Examples of distinct catalogs include:

- all officially published and supported content;
- that official catalog plus a few caller-supplied Classes;
- a rebalanced replacement of official content; and
- official content plus selected fan expansions.

The generic interface is named `Catalog`, not `PetCatalog`: Pets is the language, not every piece of
generic game data. `Canon` is the standard Terraforming Mars catalog. Client permission authority is
a separate concern and does not reuse this term.

The generic contract is deliberately one interface with five responsibilities:

| Responsibility | `Catalog` members |
| --- | --- |
| Declaration aggregation | `explicitClassDeclarations`, `allClassDeclarations`, `allClassNames`, `classDeclaration` |
| Marked-syntax transformation | `transformHandlerFactories` |
| Custom implementation lookup | `customClasses`, `customClass`, `customMetric` |
| Projection inputs | `classTable`, `modules`, `classAvailabilityModules` |
| Language metadata | `displayNamesByLanguage`, `derivedPetsNameClassNames` |

Terraforming Mars content selection is absent from this interface. Bundles, card and map registries,
replacement policy, and config-to-premise resolution remain on `TfmCatalog`. There is only one
production Catalog assembler, and its declaration assembly still incorporates transitional card
and map lowering. Extracting a generic base implementation would not let any current consumer shed
`tfm-canon`; it would add a second abstraction without establishing a new dependency boundary.

## Terraforming Mars Canon

`tfm-canon` owns Terraforming Mars content and static interpretation:

- Canon and catalog composition;
- cards, maps, milestones, awards, Modules, and expansions;
- Pets and JSON resource loading;
- current structured card and map models while they still exist;
- `PROD`, `CARDS`, and other Terraforming Mars transforms;
- config-to-premise resolution; and
- custom instruction and metric implementations required by the content.

`TfmCatalog`, `Bundle`, content selection, `CardDefinition`, `MarsMapDefinition`, and their peers
live under `dev.martianzoo.tfm.canon`. A later caller-facing Terraforming Mars API must not absorb
this static catalog ownership merely because both are domain-specific.

Canon depends on Pets, never engine. A static program can load Canon, resolve a config, project its
Class Table, and inspect all authored structure. Live Metric values remain unknown because the
program has chosen not to execute a World.

Localized bundle-name resources stay with Canon; no current packaging need justifies another owner.

There is no separate `record` module in this plan. The fact that generic task and event data can
record game history does not by itself justify splitting that data away from `pets`.

## Engine and client API

`engine` owns:

- live component, effect, task, timeline, and event state;
- preparation and lowering of Pets Instructions;
- transactional, reversible, integrity-preserving mutation;
- a flat trusted Actor-scoped workhorse;
- implementation of the Pets evaluation contract; and
- generic workflow and control-until-drain execution.

It also owns the generic client-facing API packages. Packages should distinguish trusted
runtime machinery from caller-facing operations, but a package distinction does not require a
Gradle-module boundary. Split an `api` module only if real consumers or dependencies demonstrate
that the separation buys something.

It does not own Terraforming Mars rules, client permissions, localized input, or autoexecution
policy. It receives a generic catalog and exact premise.

The workhorse may accept canonical Pets strings as convenient overloads in addition to typed Pets
values. This is harmless because the parser is generic Pets code. The meaningful boundary is that
engine must not own localized spellings, aliases, input-only synonyms, or presentation formatting.

Terraforming Mars workflow topology does not belong in a renamed `TfmWorkflow` Kotlin class. The
target remains the division in [WORKFLOW.md](WORKFLOW.md): generic workflow vocabulary in Pets,
Terraforming Mars topology in Canon declarations, and a generic runner in engine.

The Terraforming Mars runtime wrappers `TfmGameplay` and `TfmWorkflow` live in `tfm-engine`.
Redesigning their API is separate from module ownership.

## Tests

`tfm-tests` owns functional tests of the integrated Terraforming Mars system: cards, rules,
workflows, and whole games. It is a leaf module depending on `tfm-engine` and Canon.

Tests genuinely about Pets, the type system, or the generic engine remain with their production
module. Prefer small declarations owned by the test when that substitution is straightforward, but
keep an engine test with engine when Canon provides nontrivial practical scaffolding; module
ownership should describe the behavior being tested rather than the purity of its setup.

## Vocabulary and human text

`Vocabulary` remains in `pets`. Its Pets transformations and static display-name operations are used
without engine, while engine and script also consume its task and event rendering. Decomposing those
responsibilities would be client/language API work, not a module-ownership correction.

`tfm-text` is a separate static module for human-language rendering of Terraforming Mars Pets and
its English evidence resources. It depends on Pets and Canon, not engine.

## Static versus dynamic analysis

The classification depends on what an analysis does, not how its input was produced:

- Static analysis inspects Pets, a projected Class Table, Canon, or already-produced event data
  without executing instructions or evaluating a live World.
- Dynamic analysis runs or simulates engine behavior.

Therefore a finished-game summarizer is static even though engine originally produced its log. It
should consume immutable Pets data, not a live World. Canon inspection, map generation, resource
monotonicity analysis, and Terraforming Mars text rendering are also static. Benchmarks and
simulation are dynamic.

`tools` currently contains static entry points plus the dynamic `TypeStructureReport`, so the leaf
module depends on engine. No current consumer or distribution needs the static subset separately;
another module would not improve a real dependency path. If that changes, this classification gives
the split: static code itself should remain free of engine imports.

## Current ownership

| Concern | Owning module |
| --- | --- |
| `dev.martianzoo.pets` and its subpackages | `pets` |
| `ClassDeclaration`, class selection, projection, pure premise | `pets` |
| `Catalog`'s generic static contract | `pets` |
| `CustomClass`, `CustomMetric`, minimal read contract | `pets` |
| Syntax/type exceptions and generic system Class Names | `pets` |
| Live Actor, task queues, preparation, World, timeline | `engine` |
| Immutable decisions, task descriptions, and events | `pets` |
| Terraforming Mars config, card/map data, JSON models, bundles | `tfm-canon` |
| Current `TfmCatalog` registries and composition | `tfm-canon` |
| Current generic API packages | `engine`; every current production consumer needs engine behavior |
| Current `TfmGameplay` and `TfmWorkflow` | `tfm-engine` |
| Current `Vocabulary` | `pets`; later decomposition belongs to client/language API work |
| Current Terraforming Mars language renderer | `tfm-text` |
| Generic synthetic execution tests | `engine` tests |
| Terraforming Mars functional tests | `tfm-tests` |

## Resource and external-dependency boundaries

- `pets/system.pets` stays with Pets.
- Canon's rules and content resources stay with `tfm-canon`; localized name data stays there until a
  real packaging need argues otherwise.
- English renderer evidence stays with `tfm-text` and should be verification-only where production
  does not read it.
- Better Parse is confined to Pets.
- Terraforming Mars JSON serialization is confined to `tfm-canon`.
- Generic task and event serialization, if retained, stays with `pets` unless a real consumer
  requires a separate boundary.
- Coroutines remain in engine only if the generic workflow runner needs them.
- JLine and browser dependencies remain confined to the `repl` and `web` leaf applications.
