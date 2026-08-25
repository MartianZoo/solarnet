# Module organization

**Status: active target.** The named module and package boundaries through `tfm-canon`,
`tfm-engine`, `tfm-text`, and `tfm-tests` are committed. Every Kotlin module now owns one recognizable
package subtree, and lower-layer tests live with the behavior they exercise, with Canon retained only
as permitted test scaffolding. The `Catalog` replacement and remaining ownership refinements are
still proposed. Current behavior remains documented by [ENGINE.md](ENGINE.md), [OPTIONS.md](OPTIONS.md),
and [NAMING.md](NAMING.md) where they differ from this target.

## Governing rule

No non-Terraforming-Mars production source set may depend on a Terraforming-Mars module. The generic
Pets and engine layers must make sense without Canon or any other Terraforming Mars code.
The boundary should be strong enough that all `tfm` modules could later move to another source tree
or repository without reversing a dependency.

Module names should normally follow their principal `dev.martianzoo.*` package. Initially prefer an
enforced boundary over minimizing module count; consider recombination only after the dependency
directions are clean. Keep each Gradle module's physical source roots separate: merging them into a
shared package-shaped tree would obscure the ownership that the module split now makes explicit.

## Initial dependency graph

```text
engine ─────> pets
tfm-canon ──> pets
tfm-engine ─> engine, tfm-canon
tfm-text ───> tfm-canon, pets
tfm-tests ──> tfm-engine, tfm-canon
```

Engine tests may use `tfm-canon` and `tfm-engine` as test-only scaffolding when the behavior under
test belongs to engine and replacing the integrated world would be substantial. This does not add a
production dependency or change production ownership.

This is the first enforced boundary, not a claim that every eventual production module is already
known. Generic client APIs remain packages within `engine` unless a concrete dependency reason
justifies separating them. Terraforming Mars runtime and client-API packages similarly share
`tfm-engine`; their imports require both generic engine and Canon, but do not justify another module
between them. Script, REPL, web UI, tools, and benchmarks are deferred leaf applications.

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

`Catalog` is the current preferred replacement for the overloaded static meaning of `Authority`.
A catalog is a composable universe of declarations, transformations, custom implementations, and
associated content metadata from which premises select.

Examples of distinct catalogs include:

- all officially published and supported content;
- that official catalog plus a few caller-supplied Classes;
- a rebalanced replacement of official content; and
- official content plus selected fan expansions.

The generic interface should be named `Catalog`, not `PetCatalog`: Pets is the language, not every
piece of generic game data. `Canon` is the standard Terraforming Mars catalog. Client permission
authority is a separate concern and should not reuse this term.

## Terraforming Mars Canon

`tfm-canon` owns Terraforming Mars content and static interpretation:

- Canon and catalog composition;
- cards, maps, milestones, awards, Modules, and expansions;
- Pets and JSON resource loading;
- current structured Definition models while they still exist;
- `PROD`, `CARDS`, and other Terraforming Mars transforms;
- config-to-premise resolution; and
- custom instruction and metric implementations required by the content.

Most current `dev.martianzoo.tfm.data` and `dev.martianzoo.tfm.api` declarations are actually Canon
types. `TfmAuthority`, `Bundle`, content selection, `CardDefinition`, `MarsMapDefinition`, and their
peers should move under `dev.martianzoo.tfm.canon` or subordinate packages. The future
`dev.martianzoo.tfm.api` must not inherit static catalog ownership from today's misleading package
name.

Canon depends on Pets, never engine. A static program can load Canon, resolve a config, project its
Class Table, and inspect all authored structure. Live Metric values remain unknown because the
program has chosen not to execute a World.

Localized bundle-name resources may stay with Canon for now. Their eventual packaging is not
important enough to constrain the core split.

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

It also owns the generic client-facing API packages for now. Packages should distinguish trusted
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

The current Terraforming Mars runtime wrappers belong in `tfm-engine`. This is an ownership move,
not a replacement-API project: `TfmGameplay` and `TfmWorkflow` move intact until a later behavior
change gives reason to redesign them.

## Tests

`tfm-tests` owns functional tests of the integrated Terraforming Mars system: cards, rules,
workflows, and whole games. It is a leaf module depending on `tfm-engine` and Canon.

Tests genuinely about Pets, the type system, or the generic engine remain with their production
module. Prefer small declarations owned by the test when that substitution is straightforward, but
keep an engine test with engine when Canon provides nontrivial practical scaffolding; module
ownership should describe the behavior being tested rather than the purity of its setup.

## Vocabulary and human text

The current `Vocabulary` combines several responsibilities. Moving it or decomposing it is deferred
until the surrounding module boundaries reveal its actual consumers; this phase does not replace it.

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

Physical organization of tools, REPL, web UI, and benchmarks is deferred. When revisited, a static
application must not acquire engine merely because it shares a module with dynamic code.

## Current ownership changes

| Current code | Destination |
| --- | --- |
| `dev.martianzoo.pets` and its subpackages | `pets` |
| `ClassDeclaration`, class selection, projection, pure premise | `pets` |
| `Authority`'s generic static contract | `pets` as a catalog contract |
| `CustomClass`, `CustomMetric`, minimal read contract | `pets` |
| Syntax/type exceptions and generic system Class Names | `pets` |
| Live Actor, task queues, preparation, World, timeline | `engine` |
| Immutable decisions, task descriptions, and events | `pets` |
| Terraforming Mars config, Definitions, JSON models, bundles | `tfm-canon` |
| Current `TfmAuthority` registries and composition | `tfm-canon` |
| Current generic API packages | remain in `engine` until a module split has concrete value |
| Current `TfmGameplay` and `TfmWorkflow` | `tfm-engine` |
| Current `Vocabulary` | destination pending consumer audit |
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
- JLine and browser dependencies remain confined to eventual leaf applications.

## Audits that decide the remaining moves

1. Enumerate the methods every current custom implementation actually calls. The generic evaluation
   boundary should expose only those needs, ideally through one interface.
2. Classify each current `Authority` responsibility as declaration aggregation, transformation,
   implementation lookup, projection, or Terraforming Mars content selection. That concrete list
   will determine the smallest useful `Catalog` contract.
3. Trace consumers of the generic client-facing packages. Keep them in `engine` unless an actual
   consumer can avoid engine implementation dependencies through a separate module.
