# Generic and Terraforming Mars boundary audit

**Status: audit, not a mandate to support unrelated games.** The useful goal is coherent ownership
inside Solarnet. Do not perform heroic extraction for hypothetical clients.

The generic runtime is mostly reusable, but a few seams still mix Pets/engine mechanics with
Terraforming Mars or REgo application policy. `TODO.md` decides whether any seam is worth changing.

## Terraforming Mars behavior outside `tfm`

### Bare numbers mean megacredits

**Priority when boundary work is selected: P0.**

`ScaledExpression` in `pets` treats an omitted scalar expression as megacredits. That makes a
Terraforming Mars currency part of the generic AST.

A principled correction would either preserve omission in the AST until a configured transform
resolves it or supply the implicit unit through a game language profile. Do not add a general
profile system unless this seam is actually being fixed.

### Turn/action protocol is split across layers

**Priority when boundary work is selected: P0.**

Generic Pets and engine code know `Action`, `UseAction`, `WhichAction`, `NewTurn`, and turn-start
translation, while the foundational declarations live in Terraforming Mars canon. Either this is a
documented generic protocol whose declarations belong in the runtime prelude, or all of it belongs
under Terraforming Mars. The half-generic placement is the defect.

The [Pets Action model](ACTIONS.md) makes this seam more explicit: fixed and X-scaled Terraforming
Mars `StandardResource` costs use provider- and action-qualified invoices, while direct and
costless Actions keep ordinary Pets sequencing. The generic Action transformer recognizes those six
resource names directly, alongside its existing Terraforming Mars meaning for bare numbers. Treat
both leaks as one boundary debt rather than adding a broad extension framework for this rule.

### Configured marked-syntax transforms

**Resolved.**

Catalogs register handlers by the explicit marker before `[...]`; handlers that need the active
class universe are factories bound through `ClassTable`. Generic input, class-effect, and
custom-output processing use the complete resulting dispatcher without naming Terraforming Mars
syntax. Terraforming Mars registers both `PROD` and `CARDS`; its language layer selects the
registered `PROD` handler while retaining `CARDS` for structural rendering. Earlier card-source
compilation uses the same dispatcher with only the follow-mode `CARDS` handler, so unregistered
syntax is deliberately preserved for a later stage.

### The script application is mostly REgo/Terraforming Mars

**Priority when boundary work is selected: P1.**

The reusable command shell and completion framework live beside concrete Canon construction,
`TfmWorkflow`, colors, phase behavior, map views, six resources, and Terraforming Mars setup
syntax. A focused application profile or `TfmScriptSession` should own those contributions if this
area is refactored.

The REPL similarly combines reusable JLine/socket adapters with REgo construction, branding,
history, and launcher commands. Keep executable wiring application-specific; extract adapters only
when another caller needs them.

## Reusable behavior inside `tfm`

### `TfmCatalog` contains a generic Catalog implementation

**Priority when boundary work is selected: P1.**

Declaration aggregation, duplicate checking, core validation, definition lowering, indexes, custom
lookup, and test providers are generic Catalog responsibilities. Card, milestone, award, map,
standard-action, and colony registries are Terraforming Mars responsibilities. Split them when work
already touches Catalog ownership; do not redesign premise resolution at the same time.

### Workflow runner mechanics are general

**Priority when boundary work is selected: P1.**

The phase sequence and victory conditions are Terraforming Mars. Coroutine lifecycle, single launch,
queue-drained wakeup, checkpoint/rollback shutdown, and cancellation are engine mechanics. A native
workflow project should extract those mechanics while moving phase topology to the domain; see
[WORKFLOW.md](WORKFLOW.md).

### Minor presentation helpers

**Priority when boundary work is selected: P3.**

Hex-to-ANSI color rendering and half-space centering are generic helpers inside Terraforming Mars UI
classes. They are too small to drive an architecture change. Move them only with nearby work.

## Already-correct boundaries

Do not reopen these without new evidence:

- `system.pets` owns the runtime classes `Component`, `Class`, `Hidden`, `System`,
  `Temporary`, `Signal`, `Ok`, `Die`, `Engine`, `Custom`, `Atomized`, `Anyone`,
  `Owner`, and `Owned`. Ownership is generic engine vocabulary; concrete owner kinds remain
  game-specific.
- `Initializer` creates only engine/singleton baseline state; Terraforming Mars workflow creates
  `SetupPhase`.
- Class reachability roots are chosen outside `ClassLoader`; the loader only follows generic
  structural reachability.
- Runtime players use canonical seat identities; configured names are Vocabulary aliases.
- `World` is the generic live Game World and construction accepts a generic `GamePremise`.

If a boundary change is selected, prefer deleting a backward dependency or moving one whole policy
over adding adapters on both sides.

## Conditional extraction order

**Aspirational and not currently scheduled.** If the project deliberately selects a boundary
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

Do not perform this sequence merely to make an unrelated board game theoretically possible. Each
step must be independently valuable to Solarnet.
