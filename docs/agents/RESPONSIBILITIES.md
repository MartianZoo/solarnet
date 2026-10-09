# Runtime and Terraforming Mars responsibility audit

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** moving code across state, engine, permissions, autoexecution, generic, or
> Terraforming Mars packages; changing bare-number rejection or Action lowering; splitting Catalog
> responsibilities; or separating script/workflow mechanics.
>
> **Skip when:** a move follows the dependency direction already explicit in the source and Gradle
> build files, or when the only motivation is support for a hypothetical unrelated game.
>
> **Status:** selected runtime dependency direction plus an audit of remaining generic/domain
> placement. It is not a mandate to generalize Solarnet.

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

### Catalog assembly and configuration live in `state`

[`Catalog`](../../src/common/dev/martianzoo/state/Catalog.kt) is the concrete, extensible Catalog
implementation. It aggregates system and contributed declarations, checks duplicate names, loads
and validates the master table, derives its custom-Class requirements, composes custom-Class
dependencies and display names, and adds concrete Player Classes. Construct `Catalog(first, second)`
to combine generic contributions; construct `TfmCatalog(first, second)` to apply Terraforming Mars
policies to the combined declarations.

[`GamePremiseBuilder`](../../src/common/dev/martianzoo/state/GamePremiseBuilder.kt) resolves explicit
signed Class names, additive setup-component adjustments, premise-local Player declarations, and
convergent Module defaults. The finished `GamePremise` stores Modules and individual Content in the
same Class-selection set. A game-specific Catalog can adjust content selections and setup effects
before `build()` creates its ordinary Pets initialization declaration. This working configuration
never replaces or recompiles the Catalog's master table.

`TfmCatalog` owns card validation and action lowering, bundle provenance, card/map/colony registries,
expansion compatibility, milestone and award pools, and the Terraforming Mars bootstrap signals.
Player effects create seat-order Components. Its Module registry is derived from the assembled
declarations and bundle content; bundles need not compile independently. Generic Catalog composition
combines explicit Module maps.

Generic assembly and configuration tests live in `:state`; generic setup execution is covered in
`:engine`. Terraforming Mars content selection and full-game scenarios remain in their domain suites.

### Workflow progression and task scheduling

Phase order, player rotation, and victory conditions belong to Terraforming Mars. The engine owns
the general rules that decide which pending work may execute and what must finish before suspended
work resumes. An Agent policy may choose among eligible tasks; it cannot supply missing game
exclusion merely by always running some tasks first.

Pets phases and turn continuations own progression. Clients issue the initial Admin operation
and perform pending choices; they retain no Kotlin workflow runner. Generic transaction settlement
and continuation cleanup remain in the engine. [WORKFLOW.md](WORKFLOW.md) records this behavior
and the remaining completion questions.

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

### Game assembly and runtime APIs belong to `:state`

Pets owns source, declarations, types, requirements, metrics, instructions, narrowing, effects,
actions, transform blocks, owner-local Classes, and elaboration. `:state` owns `Catalog`,
`GameConfig`, `GamePremise`, `ClassSelection`, runtime Actor/Player identities, `GameReader`, and the
Kotlin custom metric/instruction APIs. A `GameWorld` passively carries any supplied bindings and uses
them for custom metric queries. `Engine.newGame` validates the complete set needed for live play;
game-specific engines such as `TfmEngine` own and supply that set. Its `displayNames.kt` supplies
Catalog-based presentation names; [NAMING.md](NAMING.md) owns naming policy.

The loading interface accepts data and callbacks supplied by Catalog and GamePremise; it has no
dependency on either. `TypeInfo` supplies the active class table without a `GameReader` downcast.
[CLASS_TABLES.md](CLASS_TABLES.md#game-view-shape) owns those construction contracts.
`PremiseViability` stays with game assembly and uses Pets' public `InhabitanceInterpreter` for
empty-domain facts. Providers of Catalogs and custom runtime behavior depend on `:state`, which
in turn depends on `:pets`. Pets production code and tests have no dependency on `:state`.

Pure language and type tests construct tables directly. Catalog, configuration, runtime identity,
and Kotlin implementation tests live with `:state`; their game-assembly fixtures are not compiled
into the Pets test module.

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
