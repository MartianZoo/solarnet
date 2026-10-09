# Repository split and responsibility ownership

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** moving code across state, engine, permissions, autoexecution, generic, or
> Terraforming Mars packages; changing bare-number rejection or Action lowering; splitting Catalog
> responsibilities; preparing the Pets/Solarnet repository split; separating linguistic Pets from
> resolved types; or separating script/workflow mechanics.
>
> **Skip when:** a move follows the dependency direction already explicit in the source and Gradle
> build files, or when the only motivation is support for a hypothetical unrelated game.
>
> **Status:** agreed repository-split requirements, remaining proposals, and current runtime
> ownership. It is not a mandate to generalize Solarnet.

## Prepare the Pets/Solarnet repository split

### Agreed scope and acceptance checklist

- Reorganize the existing repository so a later repository split is straightforward. Creating the
  separate repositories is outside this effort.
- The future Pets repository owns language, types, static Terraforming Mars content and analysis,
  content compilation, human rendering, and Almanac. Solarnet owns live game state, execution,
  Agents, and runtime applications. Terraforming Mars specificity does not determine repository
  ownership: Canon belongs with Pets. See the original intent in
  [`INTRO.md`](INTRO.md) and [`pets-repo-draft.md`](../pets-repo-draft.md).
- A client using only linguistic Pets must be able to parse, inspect, and manipulate type
  expressions syntactically/literally without a loaded class table or a dependency on resolved-type
  implementation. Resolved types consume that shared syntax; do not create a second AST.
- Choose module separations and consolidations case by case, favoring consolidation over
  proliferation. No target module count or size threshold is selected.
- Do the redesign needed for the split, taking reasonably accessible opportunities for a cleaner
  design. This does not select unrelated language or gameplay changes. Existing module names,
  packages, and entry points need no compatibility preservation.
- Keep Almanac a simple web application and card compilation a build tool. Put map generation near
  card compilation; neither needs subdivision for its own sake.

Record agreements in the owning repository documents. Discuss unresolved disagreements in chat;
the module spreadsheet is not the place for answers or the authoritative design plan.

### Current implementation

Static Catalog assembly is independent of runtime state in `:catalog`, which depends only on
`:pets`. Canon supplies static custom-Class dependency metadata; `TfmEngine` supplies executable
Terraforming Mars implementations. `:tfm-state` contains read-only game projections, not custom
execution. Canon, fake content, Almanac, and codegen have no runtime module dependency. See
[static assembly and runtime ownership](#static-game-assembly-lives-in-catalog-runtime-apis-live-in-state)
for the current contracts.

### Remaining work

**Deferred by the owner: linguistic Pets versus resolved types.** The client capability above is
agreed; its implementation is deferred. The proposed starting point is to keep parsing,
declarations, literal expressions, and lexical binding together, moving class-table-dependent
operations into the resolved-type layer. Inspect `Expression`, `PetElement`, `InstructionTree`,
and `InstructionIntersection` under `src/common/dev/martianzoo/pets/ast`, plus `TypeInfo`:
moving `pets/types` alone would leave circular dependencies. Verify a client can use only the
linguistic dependency with unresolved names. The [optional parser extraction](#pets-source-input-and-model-construction)
is a separate concern: allowing model clients to omit a parser does not let linguistic clients omit
the resolved-type implementation.

**Proposed: consolidate content tooling.** These moves remain unimplemented:

- Move map generation beside card compilation. The current
  [`regenerateMapAreas.kt`](../../src/jvm/dev/martianzoo/tfm/tools/regenerateMapAreas.kt) imports
  `MarsMapReader` and `MarsMapDefinition` from Canon, while Canon's build already depends on
  `:tfm-card-generator`. Move shared map inputs upstream first to avoid a build cycle. Consider
  broadening/renaming `:tfm-card-data` rather than adding a tiny map module.
- Move Canon loading and Terraforming Mars output grouping out of
  [`PetsTypeGenerator.kt`](../../src/jvm/dev/martianzoo/codegen/PetsTypeGenerator.kt) into static
  content tooling. Its `generateCanonicalPetsTypes` entry point loads Canon; the generator itself
  assumes `Card`, `Area`, `Milestone`, and `Award` Classes.
- Move the [random-card generator](../../test/jvm/dev/martianzoo/tfm/randomcards/RandomCardGenerator.kt)
  and its tests from `:tfm-tests` into static tools, accounting for its existing test-library helper.
- Separate static commands from live-game commands in the mixed `:tools` module, without creating
  a module for every command. Choose further mergers from actual dependencies, not module size.

Move meaningful tests with their responsibilities. Verify the resulting dependency graph and the
affected language, content generation, and runtime scenarios; compilation alone is insufficient.
Preserve observable language and game behavior while changing ownership.

## Source map

- [`ScaledExpression.kt`](../../src/common/dev/martianzoo/pets/ast/ScaledExpression.kt)
  — search for `denominationless` only for the parse-time rejection stage.
- [`PetTransformer.kt`](../../src/common/dev/martianzoo/pets/PetTransformer.kt) —
  search for `transformAction` only for the Action/turn division.
- [`TfmCatalog.kt`](../../src/common/dev/martianzoo/tfm/canon/TfmCatalog.kt) —
  inspect when splitting generic Catalog assembly from Terraforming Mars registries.
- [`ScriptSession.kt`](../../src/common/dev/martianzoo/tfm/script/ScriptSession.kt) —
  inspect only for the script application layer.
- [`Agent.kt`](../../src/common/dev/martianzoo/agent/Agent.kt) and
  [`AutoExecPolicy.kt`](../../src/common/dev/martianzoo/agent/AutoExecPolicy.kt) — current
  application-facing APIs owned by the `:agent` module.
- [GAMEWORLD.md](GAMEWORLD.md) owns the selected Game World data, playback, and export model.

The generic runtime is mostly reusable, but a few interfaces still mix Pets/engine mechanics with
Terraforming Mars or REgo application policy. Any selected change must follow the focused directions
below; their presence does not schedule it.

## Selected runtime dependency direction

The target runtime has three library responsibilities with one-way dependencies:

1. **Game World:** `:state` owns the replayable data of one game: immutable context,
   concrete components, exact pending tasks, event history, readable projections, and approved
   recording positions. Instructions are inert task data here. Game World can apply and replay only
   already-decided component and task events; it has no instruction interpretation, task behavior,
   effects, Agent, or autoexecution concept. [GAMEWORLD.md](GAMEWORLD.md) owns the full model.
2. **Engine:** depends on Game World. It owns task construction, assignment rules, selection,
   narrowing, resolution, execution, effects, live transaction coordination, and atomic game
   operations. Once a consequence is exact, the engine asks `GameWorld` to apply it and then reacts
   explicitly. The engine exposes its basic mutation primitives directly for workflows, replay
   correction, cheats, and tests; it does not try to prevent clients from using them.
3. **Agent:** depends on engine and is the normal client API. It creates exactly one Agent per Actor,
   gives each Agent an Actor-scoped reader with deliberate access to the unscoped reader, and keeps
   task selection and narrowing small. It creates caller-held drafts of an Actor's unsubmitted
   task choices and uses read-only engine validation to continue them; the engine still validates
   every submitted narrowing. Each Agent owns its optional autoexecution policies. Shared wiring
   repeatedly gives all Agents a chance to act after an engine mutation until none does.

Applications compose those libraries and add game-specific workflow and presentation. Agent
construction returns one `Agents`, pairing a World with its immutable set of Agents; its shared loop
remains private wiring rather than another public game wrapper. A separate passive Actor-access
abstraction is not currently justified.

Game World returns the exact applied event after its own data is coherent. It does not call back
into the engine while applying an event. Engine `Changer` explicitly updates its derived effect
index, so `GameWorld` has no knowledge of effects or the engine and recording playback cannot fire
rules.

The concrete-change value is owned by Game World and contains resolved component Types and counts.
It is not an `Instruction` subtype and carries no effect behavior. Exact task events may carry task
instructions, assignment, Actor attribution, continuations, and cause as passive data. The engine
retains coordination of live atomic rollback across every Game World event produced by one
operation.

Task assignment remains an engine-enforced game rule. Preventing a caller from choosing the direct
engine API is out of scope. The engine is intentionally indifferent to why an Actor or trusted
caller chose one legal mutation instead of another.

`:state` owns passive component and task storage, exact event history, the rich `GameReader`, custom
metric evaluation, and immutable recording navigation. Task construction and execution, effects,
live transactions, and the decision that an operation has reached a viewer-safe position remain in
`:engine`. The game viewer consumes state recordings and has no engine dependency.

## Terraforming Mars behavior outside `tfm`

### Turn/action protocol is split across layers

Generic Pets and engine code know `Action`, `UseAction`, `ActionSlot`, `NewTurn`, and turn-start
translation, while the foundational declarations live in Terraforming Mars canon. The generic
action syntax and identity protocol are deliberate. The generic Agent's `startTurn` and `inTurn`
conveniences remain layering debt: move them to `TfmGameplay` so the Agent no longer knows
`NewTurn`.

The [Pets Action model](ACTIONS.md) makes this division more explicit: fixed and X-scaled Terraforming
Mars `StandardResource` costs use provider- and action-qualified billing components, while direct and
costless Actions keep normal Pets sequencing. Generic Pets performs only the ordinary arrow-to-effect
lowering. `TfmCatalog` applies the standard-resource billing rewrite to its own declarations before
class loading, without global transformer registration.

The REgo command/session implementation now lives under `dev.martianzoo.tfm.script`; no reusable
script application abstraction has been extracted without another application requiring one.

The REPL similarly combines its JLine adapter with REgo construction, branding, history, and
launcher behavior. Keep executable wiring application-specific; extract the adapter only when
another caller needs it.

## Reusable behavior inside `tfm`

### Catalog assembly and configuration live in `catalog`

[`Catalog`](../../src/common/dev/martianzoo/catalog/Catalog.kt) is the concrete, extensible static
Catalog
implementation. It aggregates system and contributed declarations, checks duplicate names, loads
and validates the master table, derives its custom-Class requirements, composes custom-Class
dependencies and display names, and adds concrete Player Classes. Construct `Catalog(first, second)`
to combine generic contributions; construct `TfmCatalog(first, second)` to apply Terraforming Mars
policies to the combined declarations.

[`GamePremiseBuilder`](../../src/common/dev/martianzoo/catalog/GamePremiseBuilder.kt) is the protected
Catalog-subclass resolution context. It resolves explicit signed Class names, additive
setup-component adjustments, premise-local Player declarations, and convergent Module defaults.
The finished `GamePremise` stores Modules and individual Content in the same Class-selection set.
A game-specific Catalog adjusts content selections and setup effects through
`Catalog.configurePremise`; callers receive the completed premise rather than building it manually.
This working configuration never replaces or recompiles the Catalog's master table.

`TfmCatalog` owns card validation and action lowering, bundle provenance, card/map/colony registries,
expansion compatibility, milestone and award pools, and the Terraforming Mars bootstrap signals.
Player effects create seat-order Components. Its Module registry is derived from the assembled
declarations and bundle content; bundles need not compile independently. Generic Catalog composition
combines explicit Module maps.

Generic assembly and configuration tests live in `:catalog`; generic setup execution is covered in
`:engine`. Terraforming Mars content selection and full-game scenarios remain in their domain suites.

### Workflow progression and task scheduling

Phase order, player rotation, and victory conditions belong to Terraforming Mars. The engine owns
the general rules that decide which pending work may execute and what must finish before suspended
work resumes. An Agent policy may choose among eligible tasks; it cannot supply missing game
exclusion merely by always running some tasks first.

Current workflow uses a Kotlin coroutine and global idleness. Extracting its launch, wakeup, and
cancellation machinery into a generic runner would preserve that orchestration rather than make
the game run through Pets. [WORKFLOW.md](WORKFLOW.md) records the current behavior and unresolved
completion questions.

The delegated-payment requirement makes the distinction concrete: P1 must remain on turn while P2
controls a payment, and internal Admin work may occur within that payment. Turn state, task
assignment, and event Actor cannot stand in for each other. If priorities or operation groups are
introduced, their passive recorded data belongs in Game World and their eligibility rules belong
in the engine; domain rules
still determine when the relevant work is requested. The alternatives remain open in
[SEQUENCING.md](SEQUENCING.md#delegated-operations-and-scheduling-options).

### Minor presentation helpers

Hex-to-ANSI color rendering and half-space centering are generic helpers inside Terraforming Mars UI
classes. They are too small to drive an architecture change. Move them only with nearby work.

### Static game assembly lives in `:catalog`; runtime APIs live in `:state`

Pets owns source, declarations, types, requirements, metrics, instructions, narrowing, effects,
actions, transform blocks, owner-local Classes, and elaboration. `:catalog` owns `Catalog`,
`GameConfig`, `GamePremise`, `ClassSelection`, premise resolution, and Catalog-based presentation
names. These are static: player seats are Class Names, and a premise contains no Actor, Component,
task, event, reader, or executable custom implementation. `:state` turns the configured player
names into runtime Actor/Player identities and owns `GameReader` and the Kotlin custom
metric/instruction APIs. A `GameWorld` passively carries any supplied bindings and uses them for
custom metric queries. `Engine.newGame` validates the complete set needed for live play;
game-specific engines such as `TfmEngine` own and supply that set. [NAMING.md](NAMING.md) owns
presentation naming policy.

`GameRecording.open()` creates a passive world without custom implementations. Recorded component
queries and seeking work without them; querying a custom metric fails when no implementation is
available. The contract is exercised by `passivePlaybackNeedsNoCustomImplementationsUntilTheirMetricsAreQueried`
in [`GameRecordingTest.kt`](../../test/common/dev/martianzoo/engine/GameRecordingTest.kt).
Accepting custom bindings through recording playback is not part of the current API or a selected
follow-up.

`:tfm-state` owns Terraforming Mars-specific read-only projections over `GameReader`, including
the checked `TfmCatalog` view and map or production lookups. Both `:tfm-engine` and passive playback
clients may use it; it contains no execution behavior.

The loading interface accepts data and callbacks supplied by Catalog and GamePremise; it has no
dependency on either. `TypeInfo` supplies the active class table without a `GameReader` downcast.
[CLASS_TABLES.md](CLASS_TABLES.md#game-view-shape) owns those construction contracts.
`PremiseViability` stays with static game assembly and uses Pets' public `InhabitanceInterpreter`
for empty-domain facts. `:catalog` depends only on `:pets`; `:state` depends on both. Static
providers such as `:tfm-canon`, `:tfm-fake`, and `:almanac` have no dependency on `:state`.

Pure language and type tests construct tables directly. Catalog and configuration tests live with
`:catalog`; runtime identity and Kotlin implementation tests live with `:state`.

Runtime `Task`, `GameEvent`, and `TaskResult` data have moved to `:state`; their instruction-bearing
values remain inert there, while task construction and normalization stay in `:engine`.

### Pets source input and model construction

`Parsing`, `DerivedClassLowerer`, and the parsed `systemClassDeclarations` provider own source
input and owner-local lowering. The selected extraction puts these in an optional parser module
that depends on the Pets model; the model must not depend on that source reader or better-parse.
The Gradle modules have not yet been split.

`ClassBody` and `SourceExpression` are internal source-reader implementation details. A source
expression carries its authored local body until extraction emits an ordinary `ClassDeclaration`
and replaces the occurrence with an ordinary `Expression`. The model has no class-body attachment
or copying policy. `Expression` permits source-only subtypes; its structural operations remain
final, and equality distinguishes their runtime classes.

The grammar builds raw nodes. Completion extracts local declarations before selector binding and
implicit `RANK` domain binding can copy expressions. Generated headers retain markers shared by
several header positions or used by the generated body's own scope. This decision follows selector
binding, which can shadow a header name, and precedes settlement binding, which a class-header
variable supersedes. Class-header resolution and syntax validation finish the ordinary AST.
One internal declaration traversal serves extraction and both normalization passes.

Local roots cannot carry a type-variable marker or use the `This` placeholder. A `DEFAULT` root
names its declaring class and cannot declare another one, though its argument occurrences may.
WithoutEnclosingClass entry points reject local bodies directly instead of inventing a `Submitted` owner.
Separate model/parser compilation succeeds with no better-parse dependency in the model and no
friend paths. The real Gradle extraction remains follow-up work.

Public AST constructors, source-location setters, and scope-resolution functions support
independently compiled parsers. `GameReader` accepts `Expression` or resolved `Type` queries;
callers own any conversion from text.

Canonical content still parses Pets at runtime, including the system-declaration provider and some
generated Kotlin initializers. Parser-free analysis of supplied objects does not require changing
that content pipeline; a parser-free canonical-content executable would require prebuilt typed
content as a separate change.

## Already-correct dependencies

Do not reopen these without new evidence:

- `SystemDeclarations.kt` owns the generic runtime vocabulary, including the concrete
  `Admin : Actor` Class and Component, while Kotlin `Engine` names only the passive
  mutation-processing mechanism.
- Direct bootstrap creates only Admin. BootstrapPhase, the generated Premise, and fallback premise
  components use ordinary Admin tasks; workflow later replaces BootstrapPhase with `SetupPhase`.
- Class reachability roots are chosen outside `ClassLoader`; the loader only follows generic
  structural reachability.
- Runtime players use the configured concrete Player Class identities supplied by Catalog
  composition.

If a dependency change is selected, prefer deleting a backward dependency or moving one whole policy
over adding adapters on both sides.
