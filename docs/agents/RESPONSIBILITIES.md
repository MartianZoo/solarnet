# Generic and Terraforming Mars responsibility audit

> **Read when:** moving code across generic/Terraforming Mars packages, changing bare-number rejection or
> Action lowering, splitting Catalog responsibilities, or separating script/workflow mechanics.
>
> **Skip when:** a move follows the dependency direction already explicit in the source and Gradle
> build files, or when the only motivation is support for a hypothetical unrelated game.
>
> **Status:** audit, not a mandate to generalize Solarnet.

## Source map

- [`ScaledExpression.kt`](../../src/common/dev/martianzoo/pets/ast/ScaledExpression.kt)
  — search for `denominationless` only for the parse-time rejection stage.
- [`PetTransformer.kt`](../../src/common/dev/martianzoo/pets/PetTransformer.kt) —
  search for `transformAction` only for the Action/turn division.
- [`TfmCatalog.kt`](../../src/common/dev/martianzoo/tfm/canon/TfmCatalog.kt) —
  inspect when splitting generic Catalog assembly from Terraforming Mars registries.
- [`ScriptSession.kt`](../../src/common/dev/martianzoo/script/ScriptSession.kt) —
  inspect only for the script application layer.

The generic runtime is mostly reusable, but a few interfaces still mix Pets/engine mechanics with
Terraforming Mars or REgo application policy. `TODO.md` decides whether any dependency is worth changing.

## Terraforming Mars behavior outside `tfm`

### Turn/action protocol is split across layers

Generic Pets and engine code know `Action`, `UseAction`, `WhichAction`, `NewTurn`, and turn-start
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

The REPL similarly combines reusable JLine/socket adapters with REgo construction, branding,
history, and launcher commands. Keep executable wiring application-specific; extract adapters only
when another caller needs them.

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

- `SystemDeclarations.kt` owns the runtime classes `Component`, `Class`, `Hidden`, `System`,
  `MustCleanUp`, `Temporary`, `Signal`, `Ok`, `Die`, `Engine`, `Custom`, `Atomized`, `Anyone`,
  `Owner`, and `Owned`. Ownership is generic engine vocabulary; concrete owner kinds remain
  game-specific.
- `Initializer` creates only engine/singleton baseline state; Terraforming Mars workflow creates
  `SetupPhase`.
- Class reachability roots are chosen outside `ClassLoader`; the loader only follows generic
  structural reachability.
- Runtime players use canonical seat identities; configured names are Vocabulary aliases.
- `World` is the generic live Game World and construction accepts a generic `GamePremise`.

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
5. Separate reusable REPL/server adapters from REgo branding and launcher behavior.
6. Extract generic workflow lifecycle mechanics only as part of the native-workflow project.
7. Clean up dependency directions made visible by those moves.

Do not perform this sequence solely to make an unrelated board game theoretically possible. Each
step must be independently valuable to Solarnet.
