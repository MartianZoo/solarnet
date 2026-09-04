# Runtime and Terraforming Mars responsibility audit

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

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
- [`ScriptSession.kt`](../../src/common/dev/martianzoo/script/ScriptSession.kt) —
  inspect only for the script application layer.
- [`Agent.kt`](../../src/common/dev/martianzoo/engine/Agent.kt) and
  [`AutoExecMode.kt`](../../src/common/dev/martianzoo/engine/AutoExecMode.kt) — current engine-owned
  APIs that the selected layering direction will extract.

The generic runtime is mostly reusable, but a few interfaces still mix Pets/engine mechanics with
Terraforming Mars or REgo application policy. Any selected change must follow the focused directions
below; their presence does not schedule it.

## Selected runtime dependency direction

The target runtime has three library responsibilities with one-way dependencies:

1. **State:** owns `GameWorld`, the data of one game. It stores concrete components and supports
   readable projections plus only fully concrete gain, removal, and transmutation. It has no
   `Instruction`, task, selection, resolution, effect, Agent, or autoexecution concept.
2. **Engine:** depends on state. It owns instructions, the unordered task pool, assignment,
   selection, narrowing, resolution, execution, effects, and atomic game operations. Once an
   instruction has become a fully concrete state change, the engine asks `GameWorld` to apply it and
   reacts to the resulting change. The engine exposes its basic mutation primitives directly for
   workflows, replay correction, cheats, and tests; it does not try to prevent clients from using
   them.
3. **Agent:** depends on engine and is the normal client API. It creates exactly one Agent per Actor,
   gives each Agent an Actor-scoped reader with deliberate access to the unscoped reader, and keeps
   task selection and narrowing small. Each Agent owns its optional autoexecution policies. Shared
   wiring repeatedly gives all Agents a chance to act after an engine mutation until none does.

Applications compose those libraries and add game-specific workflow and presentation. Agent
construction returns an immutable Actor-to-Agent map; its shared loop remains private wiring rather
than another public game wrapper. A separate passive Actor-access abstraction is not currently
justified.

The exact state-to-engine notification mechanism remains open until extraction. Prefer returning a
neutral concrete-change result when that preserves required ordering. If state must invoke a
synchronous callback, the callback is supplied by its caller and speaks only in state vocabulary;
`GameWorld` still has no knowledge of effects or the engine.

The concrete-change value is owned by state and contains resolved component Types and counts. It is
not an `Instruction` subtype and does not carry task continuation, Actor attribution, cause, or
effect behavior. Because one engine operation can change both `GameWorld` and the task pool, the
engine retains coordination of atomic rollback and combined history across both.

Task assignment remains an engine-enforced game rule. Preventing a caller from choosing the direct
engine API is out of scope. The engine is intentionally indifferent to why an Actor or trusted
caller chose one legal mutation instead of another.

**Current divergence:** there is no `:state` or `:agent` module. Current `World` combines component
state, tasks, events, timeline, and Agent lookup, while `Agent`, `AutoExecMode`, queue draining, and
client-facing string translation all live in `:engine`. `TaskQueues` already stores one task set and
creates assignee-filtered `TaskQueue` views, so task extraction changes ownership rather than task
semantics.

Do not create empty Gradle modules ahead of the extraction. First settle the direct core mutation
surface, the concrete state-change contract, the sole-issuer Agent lifetime, and the plain shared
autoexecution loop; then move one coherent dependency slice at a time.

## Terraforming Mars behavior outside `tfm`

### Turn/action protocol is split across layers

Generic Pets and engine code know `Action`, `UseAction`, `ActionSlot`, `NewTurn`, and turn-start
translation, while the foundational declarations live in Terraforming Mars canon. Either this is a
documented generic protocol whose declarations belong in the runtime prelude, or all of it belongs
under Terraforming Mars. The half-generic placement is the defect.

The [Pets Action model](ACTIONS.md) makes this division more explicit: fixed and X-scaled Terraforming
Mars `StandardResource` costs use provider- and action-qualified invoices, while direct and
costless Actions keep normal Pets sequencing. The generic Action transformer recognizes those six
resource names directly. Treat that leak as layering debt instead of adding a broad extension
framework for this rule.

### The script application is mostly REgo/Terraforming Mars

The reusable command shell and completion framework live beside concrete Canon construction,
`TfmWorkflow`, colors, phase behavior, map views, six resources, and Terraforming Mars setup
syntax. A focused application profile or `TfmScriptSession` should own those contributions if this
area is refactored.

The REPL similarly combines its JLine adapter with REgo construction, branding, history, and
launcher behavior. Keep executable wiring application-specific; extract the adapter only when
another caller needs it.

## Reusable behavior inside `tfm`

### `TfmCatalog` contains a generic Catalog implementation

System-declaration aggregation, duplicate checking, core declaration validation, Class loading,
display-name merging, and custom implementation composition are generic Catalog assembly tasks.
Card, milestone, award, map, standard-action, and colony registries are Terraforming Mars
responsibilities.

The module-organization audit found no useful implementation split today. The generic contract
already lives in `pets`, while Terraforming Mars content selection is absent from it. There is only
one production assembler, and its declaration assembly still incorporates transitional card and map
lowering. Do not introduce a generic base implementation until a real second implementation or a
completed declaration-authority cutover reveals a coherent reusable unit. Do not redesign premise
resolution as part of that extraction.

### Workflow runner mechanics are general

The phase sequence and victory conditions are Terraforming Mars. Coroutine lifecycle, single launch,
queue-drained wakeup, checkpoint/rollback shutdown, and cancellation are engine mechanics. A native
workflow project should extract those mechanics while moving phase topology to the domain; see
[WORKFLOW.md](WORKFLOW.md).

### Minor presentation helpers

Hex-to-ANSI color rendering and half-space centering are generic helpers inside Terraforming Mars UI
classes. They are too small to drive an architecture change. Move them only with nearby work.

## Already-correct dependencies

Do not reopen these without new evidence:

- `SystemDeclarations.kt` owns the generic runtime vocabulary. In the target model that includes a
  concrete `Admin : Actor` Class and Component, while Kotlin `Engine` names only the passive
  mutation-processing mechanism. Current source still calls that Actor and Component `Engine`.
- Terraforming Mars workflow creates `SetupPhase`. The target bootstrap should reach ordinary
  Admin task execution as early as the state model honestly permits; the exact pre-task seed state
  remains to be selected.
- Class reachability roots are chosen outside `ClassLoader`; the loader only follows generic
  structural reachability.
- Runtime players use canonical seat identities; configured names are Vocabulary aliases.

If a dependency change is selected, prefer deleting a backward dependency or moving one whole policy
over adding adapters on both sides.

## Conditional extraction order

**Aspirational and not currently scheduled.** If the project deliberately selects a dependency
cleanup, the dependencies suggest this order:

1. Decide whether bare-number currency is preserved in the AST or supplied by one small
   game-specific language profile.
2. Decide whether turn/action signaling is a generic protocol or Terraforming Mars behavior, and
   move the narrow standard-resource lowering with it.
3. Split generic Catalog assembly/validation from Terraforming Mars registries.
4. Separate the reusable script command shell from Terraforming Mars application wiring.
5. Separate the reusable JLine adapter from REgo branding and launcher behavior.
6. Extract generic workflow lifecycle mechanics only as part of the native-workflow project.
7. Clean up dependency directions made visible by those moves.

Do not perform this sequence solely to make an unrelated board game theoretically possible. Each
step must be independently valuable to Solarnet.
