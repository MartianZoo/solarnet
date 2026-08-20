# Engine model

**Status: current model.** This is the architectural map for committed code. Follow links to source
and tests when exact signatures or edge behavior matter. Future API and workflow directions live in
[API.md](API.md) and [WORKFLOW.md](WORKFLOW.md).

## Game construction

A live Game World is a `World` containing:

| Part | Meaning |
| --- | --- |
| `ComponentGraph` | Present state: a multiset of concrete components |
| `TaskQueues` / `TaskQueue` | Deferred work and player choices |
| `EventLog` | Applied component and task history |
| `Timeline` | Atomicity, rollback, revision, and commit floor |
| `ClassTable` | The closed vocabulary and type relationships |
| Actor-scoped `Gameplay` | The supported mutation/query facade |

`GameConfig` is unresolved user intent. Authority-specific resolution applies defaults,
implications, selection policy, and validation to produce an immutable `GamePremise`. The premise
contains one Authority, selected Modules, signed class selections, seat-ordered display names, and
exact non-singleton types to create once. See [OPTIONS.md](OPTIONS.md).

Each Authority owns one validated master `ClassTable`. A game's table projects it: selected Classes
are active and every other Authority-known Class is uninhabited. Occupied seats activate canonical
`Player1` through `PlayerN`; configured player names are Vocabulary aliases.

`Engine.newGame(premise)` wires the World, creates `Engine` and singleton components, marks
initialization complete, and commits the pre-setup baseline. It does not create a Phase.
Terraforming Mars workflow later creates `SetupPhase` as an ordinary effectful operation.

## Component graph

The component graph is only a multiset of concrete Types. Components have no fields or instance
identity. Equal Types are indistinguishable copies.

A concrete component may depend on other concrete components through its Type. Every possible
dependency target must have an applicable maximum-one invariant so the edge identifies one vertex.
Removing the last target cascades: `ComponentGraph` reports existing dependents, `Changer` removes
them first, then retries the original removal.

The only state mutation is a count plus optional source and destination. A transmutation removes
before it adds. Every successful mutation updates live-effect indexes and enters the Event Log.

`Custom` classes never enter the graph. Custom metrics report virtual non-negative counts; custom
instructions translate concrete input to ordinary instruction trees. A custom declaration may use
supertypes for dependencies and ownership, but the loader rejects inherited effects, invariants, and
ordinary instruction defaults so Kotlin translation remains its sole behavior.

## Events and timeline

The log contains `ChangeEvent`, `TaskAddedEvent`, `TaskRemovedEvent`, and `TaskEditedEvent`.
A change records its Actor and Cause. Rendered history uses `BY` for Actor, `VIA` for the
effect-bearing cause, and `BECAUSE` for causal event ordinal.

`EventLog.record` and rollback are the single history/mutation boundary: application or reversal
must succeed before the log changes. Each forward or reverse mutation advances an opaque
`WorldRevision`. Unlike an event-count checkpoint, a revision is never reused after rollback.
That distinction is intended to let a future overlay or fork detect any mutation of its backing
World even when the event count returns to the same value.

A log may capture another log as an immutable prefix in constant time. Later source events are not
part of the capture, and the source may not roll back that captured prefix while the suffix exists.

`Timeline` provides checkpoints, atomic blocks, rollback, and a commit floor. An atomic failure
reverses component state, tasks, indexes, and events. `AbortOperationException` requests the same
rollback without surfacing as a caller error. The commit floor prevents rollback into initialization
or a workflow boundary.

Failure-atomicity is not game-rule atomicity. An operation whose intermediate changes fire effects
may still be observable one change at a time.

## Tasks are an unordered choice pool

Task iteration is stable for reproducibility, but order has no game meaning. A task has:

- stable `TaskId`, derived from its original add-event ordinal;
- one task-shaped `Instruction`;
- `assignee`, whose scoped queue contains and may narrow it;
- `actor`, recorded on resulting changes unless instruction-side `BY` overrides it;
- `cause`;
- prepared flag `next`;
- optional `THEN` continuation group; and
- diagnostic `whyPending`.

A temporary 1-based display position may disambiguate equal-looking tasks. It is not an id.

`InstructionTree` is the broad AST kind. `Instruction` is one task-shaped root.
`InstructionGroup` is a normalized comma-separated batch. Queue admission splits a group into one
task per member. Selecting a grouped `OR` branch can likewise replace one task with several.

`A THEN B` stores A as current work and B as a continuation. Completing A enqueues B in its place;
B is not immediate and receives no priority over unrelated pending work. Open implicit variables can
prevent splitting until an earlier stage fixes their shared Type.

### Assignment and Actor

Assignee and Actor are different. For queued triggered work, the current compatibility rule assigns
to the Player owner of the effect-bearing component, otherwise the Player owner of the changed
component, otherwise the triggering Actor. It independently attributes the future change to the
effect's context owner when present, otherwise the triggering Actor.

Instruction-side `BY` changes only the Actor. Trigger-side `BY` matches only the Actor on the
trigger event. Splitting, revision, preparation, `THEN`, and cross-scope execution preserve the
stored Actor. See [IDENTITY.md](IDENTITY.md).

There is no parent/child queue suspension or delegated control scope. One prepared task globally
locks preparation of competitors. `TfmWorkflow.Auto` starts Player operations directly and waits
for whole-world idleness instead.

### Preparation

A task can be abstract because of a Type, Quantifier, `OR`, refinement, or unresolved custom
operation. Preparation reads the current World and:

- evaluates `PER` metrics;
- evaluates gates and optional no-ops;
- recursively prepares `OR` arms and removes locally impossible ones;
- narrows Types when exactly one concrete choice remains;
- resolves quantifiers and abstract choice domains as specified in
  [QUANTIFIERS.md](QUANTIFIERS.md);
- rejects limits;
- makes a reflexive nonmandatory transfer a no-op; and
- translates a valid concrete custom instruction.

Preparation may replace one task with several group members. Once preparation has read state, the
result is marked `next` and must execute before any other mutation.

### Execution

Execution accepts prepared concrete work. A `Change` goes through `Changer`, logging, automatic
effects, and queued effects. `By` selects an Actor. A normalized `Then` inside inline execution
runs its concrete stages; queued `THEN` tails were separated when the task was created. `NoOp`
does nothing. An unresolved gate, `OR`, scalar, Type, or instruction group is an error.

Queued effects return `PendingTask` values and receive ids only when admitted. Inline automatic
effects never receive task ids.

## Effects

Authored behavior progresses through source effect, loaded class effect, concrete component effect,
live effect, then triggered instruction. The `Effector` indexes live component-effect pairs with
their component multiplicity.

Triggers are self-gain `This:`, self-removal `-This:`, gain/remove subscriptions to another Type,
and `OR` combinations. Wrappers add:

- trigger-side `BY` Actor matching;
- `IF` state requirements; and
- `X` “one response for any positive matching count” behavior.

Ordinary triggers scale their instruction by the matching change count. Self triggers respond only
to changed copies of the effect-bearing exact Type; existing equal copies do not multiply them.
Other subscriptions multiply by the number of live effect-bearing components.

An effect on an owned component listening to an unowned event defaults to matching only its Owner
unless it says `BY Anyone`. Unowned `System` components are engine-only; `Hidden` controls
presentation instead. `Signal` is hidden but not necessarily engine-only.

A positive abstract Actor selector can bind the matching Actor for reuse elsewhere in the trigger or
instruction. Type-variable occurrence paths likewise carry a concrete trigger narrowing into linked
instruction positions without rewriting coincidental equal Class Names.

`::` effects execute inline, recursively, before queued effects from the same concrete change are
admitted. `:` effects become tasks. Use [SEQUENCING.md](SEQUENCING.md) before depending on that
difference.

### Terraforming Mars wild tags

`Tag` depends on `TagHolder`; `CardFront` is one such holder. Printed tags therefore remain ordinary
components such as `PlantTag<CardFront>`. A `WildTag` creates a distinct `WildTagUse` holder for
each `NewTurn` in Action or Prelude and each action-phase `SecondAction`. The temporary holder offers
the owner `Tag<This>?`, so a chosen wild meaning is an ordinary tag and participates in bare tag
metrics and requirements.

The holder distinction is also the trigger distinction. `Tag` has the trigger default
`Tag<CardFront>:`, so an effect that reacts only to printed tags can explicitly accept it with
`PlantTag<>:` or spell out `PlantTag<CardFront>:`. It will not see
`PlantTag<WildTagUse<...>>`; there is no dispatch filter or special change kind. Refinements can
follow the dependency graph when card identity matters. Robotic Workforce uses
`CardFront(HAS BuildingTag OR WildTagUse(HAS BuildingTag))`, which accepts only the card whose
action-scoped wild holder received the Building interpretation.

`WildTagUse` is `Temporary`, so the action cannot finish while it remains. `TfmGameplay` declines
unchosen offers and removes all of the acting player's uses after the action body has drained; the
dependent tags disappear through ordinary dependency cascade. A different client must perform the
same uniquely implied settlement before completing the operation.

## Metrics, refinements, and limits

`GameReader.count` evaluates component counts, union metrics, and custom metrics. A union is a
multiset union: for each exact component Type, keep the greatest matching multiplicity so overlapping
arms do not double count. Its arms must be distinct component counts; capped, scaled, subtractive,
property, and virtual custom counts cannot participate because they have no component identity.
Numeric Metrics may also subtract Metrics or positive scalar operands, saturating at zero; a scalar
by itself is not a Metric. Complete-group scaling and `MAX` bind before subtraction, which binds
before union.

An abstract custom metric enumerates satisfying concrete subtypes and sums their implementations.
Every Kotlin invocation receives concrete dependency arguments.

Refinements substitute a candidate into their requirement and query the current World. Immutable
class properties supply printed cost and requirement plus map row and column without creating live
components. Numeric properties are ordinary Metrics. Stored Metric and Requirement syntax enters a
class effect only through `EVAL`; expansion substitutes the concrete receiver for `This`, can defer
until trigger specialization, and then receives normal defaults, `Owner` binding, and `PROD`
lowering. Map bonuses and other computed metadata remain honest custom metrics. Distinct live tag or
resource kinds use refined `Class<...>` Types instead.

`Limiter` computes a maximum from class invariants. Invariants may use `This` and are compiled to a
per-class lookup after table construction. [QUANTIFIERS.md](QUANTIFIERS.md) specifies how concrete
limits, abstract domains, dependencies, and instruction composition determine the result.

## Recoverable dead ends

Task selection is speculative until the encompassing operation completes. A locally valid choice may
produce later work that cannot finish. That is a recoverable dead end: raise
`DeadEndException`, roll back to the encompassing checkpoint, and try another branch.

Do not duplicate target exclusions in Pets merely to prevent every impossible intermediate choice.
A route is a correctness bug only when an illegal result can commit or no route can produce a legal
result. Earlier impossibility detection remains desirable for choice quality and diagnostics.

Protected Habitats is the model example: an opponent may initially narrow a broad attack to a
protected resource, the resulting `Die` causes a dead end, and the atomic attack rolls back.

## Modeling constraints

Do not model one compact game concept by proliferating parallel marker Classes, one watcher per
resource, duplicated Effect branches, or another structure whose shape reflects an engine
limitation. Repair the general Type/trigger mechanism or leave the content unsupported with a clear
blocker.

The current promo attack model is the useful example. One generic promo-scoped watcher records
hostile changes as:

- `MyResourceWasRemoved<victim, Class<resource>, attacker>`; and
- `MyProductionWasDecreased<victim, Class<standard-resource>, attacker>`.

Each record belongs to the victim and preserves the attacked Type plus Actor. Crash Site Cleanup and
Mons Insurance consume those records. Rules that prevent the removal, such as Protected Habitats
and Asteroid Deflection System, remain direct removal triggers. Do not replace this with an
`AttackKind` hierarchy and separate watchers for every resource.

## Input transformation

Actor-scoped string input passes through this order:

1. localized Vocabulary canonicalization and input-only synonyms;
2. Class-Name resolution against the World table;
3. atomization of counted `Atomized` components;
4. dependency defaults;
5. contextual `Owner` replacement for Player scopes; and
6. Terraforming Mars `PROD[...]` lowering.

AST values created inside the engine skip parsing but may use relevant transforms explicitly.
Transform entry points preserve their declared AST `kind`; a cardinality-changing caller must
request `InstructionTree`, not `Instruction`.

## Current Gameplay surface

The committed facade remains a power hierarchy:

```text
Gameplay
  -> TurnLayer
    -> OperationLayer
      -> TaskLayer
        -> GodMode
```

`ApiTranslation` implements all layers and `godMode()` reveals the bottom, so this is not an
authority boundary. Normal task commands, manual operations, task edits, and `sneak` do not all
share identical atomic/auto-exec semantics. [API.md](API.md) proposes a mechanical flattening before
a separate safe client API.

`manual()` seeds a group of new tasks, permits an operation body to resolve them, runs configured
auto-exec, preserves previously pending unprepared tasks, and fails if newly created Tasks or
`Temporary` components remain. A pre-existing prepared task prevents it from starting.
`sneak()` applies raw changes without normal instruction preparation or effects, but still uses the
timeline and graph mutation boundaries.

## Auto-execution and Terraforming Mars workflow

Auto-execution modes are:

- `NONE`: do nothing;
- `SAFE`: proceed only when one preparable option exists; and
- `FIRST`: choose the first preparable task in stable iteration order.

Scanning is global. Assignee selects the queue; stored Actor controls attribution. Failed candidates
receive `whyPending`.

`TfmGameplay` adds card, payment, production, parameter, and phase conveniences around the generic
layers. Treat it as transitional; test conveniences and player-facing domain actions need not
remain one production wrapper.

`TfmWorkflow.Auto` runs the Terraforming Mars phase loop in a coroutine. It commits before waiting
for tasks to drain and wakes from the shared outermost atomic-completion callback. StartToken
determines turn order. Canon produces shared `LastCall` and `Victory` facts; the workflow reads
those facts rather than reimplementing their predicates. Exact phase requirements and known gaps are
in [WORKFLOW.md](WORKFLOW.md).

## Wiring details

`Engine.Wiring` is the manual composition root. Class Table, Event Log, Component Graph, Effector,
Timeline, and other World-level services are shared. Each Actor receives its own `Changer`,
`Instructor`, `Implementations`, and `ApiTranslation` scope.

Kotlin keeps `Actor` and `Owner` distinct. Current Players are both. A passive Pets Owner such as
`SoloOpponent` has no gameplay scope or task queue.

The Effector receives a GameReader provider to break its construction cycle: it must exist before
the final reader can be assembled, while firing later requires that reader.
