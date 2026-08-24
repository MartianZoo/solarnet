# Module organization

**Status: proposal.** This document describes a desired destination, not committed behavior or an
implementation sequence. Current behavior remains documented by [ENGINE.md](ENGINE.md),
[OPTIONS.md](OPTIONS.md), and [NAMING.md](NAMING.md) where they differ from this proposal.

## Governing rule

No non-Terraforming-Mars module may depend on a Terraforming-Mars module. The generic Pets,
recording, engine, and API layers must make sense without Canon or any other Terraforming Mars code.
The boundary should be strong enough that all `tfm` modules could later move to another source tree
or repository without reversing a dependency.

Module names should normally follow their principal `dev.martianzoo.*` package. Initially prefer an
enforced boundary over minimizing module count; consider recombination only after the dependency
directions are clean.

## Proposed dependency graph

```text
pets
├── record
├── engine ──> record
│   └── api
│       └── script
├── tfm-canon
│   ├── tfm-text
│   └── tfm-api ──> api
│       └── tfm-script ──> script
```

Every module in the diagram may depend on `pets`. `tfm-api` may depend on `tfm-canon`, but neither
`pets`, `record`, `engine`, `api`, nor `script` may depend on any `tfm` module. REPL, web UI, tools,
and benchmarks are intentionally deferred; they should eventually attach as leaf applications
without weakening this rule.

## Pets: language and static semantics

`pets` owns:

- parsing, canonical source rendering, AST, and transformations;
- Class declarations, loading, types, and projected Class Tables;
- the pure `GamePremise` data model;
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

Player-chosen names should be the actual per-game Player Class Names. There need not be any
`Player1` name unless a player chooses it. A premise supplies those names and projection creates the
corresponding Player declarations; a name already owned by the selected catalog is unavailable.
Reusing a record under a different premise may require rewriting a colliding player name, just as
other changes to premise data may require rewriting the record.

### Catalog

`Catalog` is the current preferred replacement for the overloaded static meaning of `Authority`.
A catalog is a composable universe of declarations, transformations, custom implementations, and
associated content metadata from which premises select.

Examples of distinct catalogs include:

- all officially published and supported content;
- that official catalog plus a few caller-supplied Classes;
- a rebalanced replacement of official content; and
- official content plus selected fan expansions.

`PetCatalog` may be the generic interface name. `Canon` is the standard Terraforming Mars catalog,
and composition or replacement produces other catalog instances. Client permission authority is a
separate API concern and should not reuse this term.

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

## Record: durable played-game data

`record` is a proposed generic module depending only on Pets. It owns the stable data used to
round-trip a played game without importing live engine objects.

A record begins with its exact `GamePremise` and then preserves player decisions together with the
engine events they produced. The intended granularity includes facts such as:

```text
Player A prepared task B.
Player A narrowed it to C.
Changes D, E, and F resulted.
```

Clients may attach an optional human note to a recorded decision. Higher-level notions such as a
script command or “play this project” do not need to become Pets concepts. The durable semantic
decision remains the generic API operation; richer client intent can stay in the note unless a real
future requirement justifies another record layer.

Recorded and live task types are distinct:

- `TaskRecord` is immutable data used in decisions and task lifecycle events.
- Engine's live pending task is internal execution state, including preparation, continuations, and
  diagnostics.

The immediate goal is to preserve enough data faithfully. Policies for reconciling records against
changed code are deferred.

### Quiescent save boundary

A resumable record ends only after a complete API command and all user queues have drained. The
leading workflow boundary is:

1. finish the user's command and all consequences;
2. when user queues are empty, emit the generic idle signal;
3. drain anything that signal causes; and
4. if user queues are still empty, establish the save point immediately before workflow advances.

Resumption replays through that boundary and then invokes the same ordinary workflow advancement.
The record's endpoint therefore says that workflow is ready; a separate serialized workflow cursor
should be unnecessary if durable Phase state and projected topology are sufficient.

A permanently pending, lowest-priority Engine task is not the preferred representation of workflow
readiness. A Task normally means that the game is waiting for its assignee now; using one as hidden
scheduler bookkeeping weakens that meaning and forces queue persistence. If the runner ultimately
needs additional durable state, model that state explicitly rather than disguising it as an offered
task.

This conclusion remains provisional until the native workflow and idle semantics are proven
together. It is the main unresolved pressure on the claim that no task-queue snapshot is needed.

## Engine: execution and integrity

`engine` owns:

- live component, effect, task, timeline, and event state;
- preparation and lowering of Pets Instructions;
- transactional, reversible, integrity-preserving mutation;
- a flat trusted Actor-scoped workhorse;
- implementation of the Pets evaluation contract; and
- generic workflow and control-until-drain execution.

It does not own Terraforming Mars rules, client permissions, localized input, or autoexecution
policy. It receives a generic catalog and exact premise.

The workhorse may accept canonical Pets strings as convenient overloads in addition to typed Pets
values. This is harmless because the parser is generic Pets code. The meaningful boundary is that
engine must not own localized spellings, aliases, input-only synonyms, or presentation formatting.

Terraforming Mars workflow topology does not belong in a renamed `TfmWorkflow` Kotlin class. The
target remains the division in [WORKFLOW.md](WORKFLOW.md): generic workflow vocabulary in Pets,
Terraforming Mars topology in Canon declarations, and a generic runner in engine.

## API: client capabilities and policy

`api` wraps the trusted engine in interfaces that represent real differences in caller power:

- observation;
- ordinary player decisions;
- workflow control;
- referee or administrative state correction;
- optional autoexecution; and
- diagnostic access where a real client requires it.

Clients decide which capabilities to expose. This hierarchy is not an engine-internal layering
scheme.

Referee correction is a legitimate tabletop operation, not solely test machinery. It must preserve
engine integrity, carry explicit provenance, and be recorded as a correction rather than disguised
as ordinary legal play. The current `exMachina()` test helper should eventually call the same
capability.

The exact consequences of correction remain unresolved. Blindly suppressing every Effect is unsafe:
some effects may maintain structural state, such as map adjacency after tile placement. Before
choosing a correction primitive, audit which changes require automatic or queued consequences and
whether a principled distinction between them exists.

### Autoexecution

Engine owns no autoexecution policy. `api` owns the plug-in contract and driver and may supply small
generic policies. `tfm-api` supplies Terraforming Mars-specific policies. Any API client may install
its own implementations.

Script's `auto` command should normally configure available policies. Policy reasoning belongs in
script only when it genuinely depends on textual interaction rather than generic or Terraforming
Mars gameplay.

## Terraforming Mars API

`tfm-api` mirrors generic API capabilities with Terraforming Mars-shaped operations. It replaces the
useful portions of `TfmGameplay` without combining player actions, workflow control, corrections,
and test assertions into one wrapper.

The three card models require distinct, parallel APIs:

- non-tracking play does not represent exact physical cards;
- tracking play requires the client to report cards as they pass through the game; and
- assigning play will let the appropriate client assign exact cards.

Functional gameplay tests select the API matching their evidence and ordinarily do only what a real
holder of that capability could do. Referee correction is an explicit stronger capability.
Assertion-only helpers remain test support.

Information hiding and observer-specific record projections are deferred.

## Vocabulary and human text

The current `Vocabulary` is not a necessary deep abstraction. It combines display names, alternate
parseable names, player aliases, input synonyms, AST rewriting, and task/event formatting. The
likely destination deletes this central object:

- chosen player names are already canonical per-game names;
- client-side maps own alternate input spellings and display names;
- a simple `PetTransformer` performs AST name rewriting;
- script and UI own input-only synonyms and record formatting; and
- engine uses canonical Pets only.

No lower-level Vocabulary interface should be introduced unless a concrete consumer remains after
those responsibilities move outward.

`tfm-text` is a separate static module for human-language rendering of Terraforming Mars Pets and
its English evidence resources. It depends on Pets and Canon, not engine.

## Static versus dynamic analysis

The classification depends on what an analysis does, not how its input was produced:

- Static analysis inspects Pets, a projected Class Table, Canon, or an already-produced record
  without executing instructions or evaluating a live World.
- Dynamic analysis runs or simulates engine behavior.

Therefore a finished-game summarizer is static even though engine originally produced its log. It
should consume Pets plus `record`, not a live World. Canon inspection, map generation, resource
monotonicity analysis, and Terraforming Mars text rendering are also static. Benchmarks and
simulation are dynamic.

Physical organization of tools, REPL, web UI, and benchmarks is deferred. When revisited, a static
application must not acquire engine merely because it shares a module with dynamic code.

## Current ownership changes

| Current code | Destination |
| --- | --- |
| `dev.martianzoo.pets`, `.pets.ast`, `.types` | `pets` |
| `ClassDeclaration`, class selection, projection, pure premise | `pets` |
| `Authority`'s generic static contract | `pets` as a catalog contract |
| `CustomClass`, `CustomMetric`, minimal read contract | `pets` |
| Syntax/type exceptions and generic system Class Names | `pets` |
| Live Actor, task queues, preparation, World, timeline | `engine` |
| Immutable decisions, task descriptions, and events | `record` |
| Terraforming Mars config, Definitions, JSON models, bundles | `tfm-canon` |
| Current `TfmAuthority` registries and composition | `tfm-canon` |
| Current `ApiTranslation` | split between canonical engine adaptation and client API |
| Current `TfmGameplay` | decomposed into generic and Terraforming Mars capabilities |
| Current `TfmWorkflow` | replaced by Canon topology plus generic runner |
| Current `Vocabulary` | deleted and distributed to small client/Pets utilities |
| Current Terraforming Mars language renderer | `tfm-text` |
| Generic synthetic execution tests | `engine` tests |
| Terraforming Mars functional tests | `tfm-api` tests plus test support |

## Resource and external-dependency boundaries

- `pets/system.pets` stays with Pets.
- Canon's rules and content resources stay with `tfm-canon`; localized name data stays there until a
  real packaging need argues otherwise.
- English renderer evidence stays with `tfm-text` and should be verification-only where production
  does not read it.
- Better Parse is confined to Pets.
- Terraforming Mars JSON serialization is confined to `tfm-canon`.
- Record serialization is confined to `record`.
- Coroutines remain in engine only if the generic workflow runner needs them.
- JLine and browser dependencies remain confined to eventual leaf applications.

## Remaining consequential questions

1. Can the native workflow runner always resume from the exact pre-advance boundary using only
   premise, replayed components, and the rule “advance workflow now,” or is another durable workflow
   datum required?
2. Which engine consequences preserve representation integrity, and which are ordinary gameplay
   consequences that a referee correction may intentionally bypass?
3. What is the exact smallest evaluation interface required by every current custom implementation?
   Derive it from audited call sites; do not publish speculative query methods.
4. What composition and replacement rules must `PetCatalog` expose to cover official content,
   caller additions, rebalanced replacements, and fan expansions without making Terraforming Mars
   concepts generic?
5. Which current public result and event types are durable record data, and which are merely live
   engine conveniences that should remain internal?
