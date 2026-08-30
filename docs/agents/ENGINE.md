# Engine model

> **Read when:** changing live World construction, components, events, tasks, effects, rollback,
> recoverable dead ends, input transformation, or the current `Gameplay` surface.
>
> **Skip when:** a narrower document owns the concern. Use [TYPES.md](TYPES.md) for static types,
> [SEQUENCING.md](SEQUENCING.md) for ordering rules, and [OPTIONS.md](OPTIONS.md) for premise
> resolution.
>
> **Status:** current-model map. Follow the source pointers for exact behavior. Future facade and
> workflow directions live in [API.md](API.md) and [WORKFLOW.md](WORKFLOW.md).

## Read only the relevant sections

| If changing | Read |
| --- | --- |
| Game creation or premise activation | Game construction, then Wiring details |
| Component state, event history, rollback, or forks | Component graph; Events and timeline; Recoverable dead ends |
| Tasks, assignment, selection, narrowing, resolution, or execution | Tasks are an unordered choice pool through Execution |
| Triggered or automatic behavior | Effects; then the relevant section of [SEQUENCING.md](SEQUENCING.md) |
| Limits, refinements, AMAP, or quantification | Metrics, refinements, and limits; then [QUANTIFIERS.md](QUANTIFIERS.md) |
| Engine API or autoexecution | Current Gameplay surface; Auto-execution and workflow |
| Parsing or lowering submitted Pets | Input transformation |

## Source map

- [`World.kt`](../../src/common/dev/martianzoo/engine/World.kt) and
  [`WholeWorld.kt`](../../src/common/dev/martianzoo/engine/WholeWorld.kt) — search
  for `public interface World` and `public class WholeWorld` for the read surface and live assembly.
- [`ComponentGraph.kt`](../../src/common/dev/martianzoo/engine/ComponentGraph.kt) —
  inspect for component multiplicity and indexes.
- [`TaskQueues.kt`](../../src/common/dev/martianzoo/engine/TaskQueues.kt) and
  [`PendingTask.kt`](../../src/common/dev/martianzoo/engine/PendingTask.kt) — inspect
  only for deferred work and resolution.
- [`EventLog.kt`](../../src/common/dev/martianzoo/engine/EventLog.kt) and
  [`Timeline.kt`](../../src/common/dev/martianzoo/engine/Timeline.kt) — inspect only
  for history, atomicity, rollback, or revisions.
- [`Gameplay.kt`](../../src/common/dev/martianzoo/engine/Gameplay.kt) — search for
  `public interface Gameplay` before changing caller-facing operations.

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

`GameConfig` is unresolved user intent. Catalog-specific resolution applies defaults, selection
policy, and validation to produce an immutable `GamePremise`. The premise contains one Catalog,
selected Modules, signed class selections, seat-ordered display names, exact non-singleton types to
create once, and the source configuration when resolution began from one. Native replay export uses
that retained source rather than attempting to reverse defaults out of the resolved premise. See
[OPTIONS.md](OPTIONS.md).

A premise lazily forms and retains one immutable active `ClassTable` projection. Every World built
from that premise shares the projection and its compiled class metadata while retaining independent
component, effect, task, event, timeline, and gameplay state.

Each Catalog owns one validated master `ClassTable`. A game's table projects it: selected Classes
are active and every other Catalog-known Class is uninhabited. Occupied seats activate canonical
`Player1` through `PlayerN`; configured player names are Vocabulary aliases. Every premise Actor is
an explicit projection root. Trigger positions are observational and do not activate their
protocol Classes. Modules create the concrete standard actions and other protocols they issue; an
exact-Class invariant remains the fallback for generic families that cannot be constructed as one
concrete expression.

Module defaults, constructive active-provenance edges, and premise requirements are authored in
Pets. The Catalog resolves defaults and provenance to a fixed point; the engine checks each selected Module's premise
requirement and configuration-facing invariants against the resolved projection before creating the
World. Ambient Class ownership derives compatibility conditions from source declarations and
lowered structured data. Bundle
availability locks ambient Classes behind their owning Modules, and exact uninhabited-domain
viability checks reject impossible selected content before World construction.

Milestone requirements and award metrics are authored as Pets properties. Map-default pools are
abstract milestone or award superclasses whose nested concrete subclasses form the selected pool.
An invariant authored directly on a goal constrains both its live usability and its automatic
selection from a pool; Quick Start goal variants use complementary Module-count invariants.
Canon derives goal names, pool membership, selection requirements, and compatibility directly from
those declarations; there are no parallel goal metadata objects.

Canonical card classes are loaded from each bundle's authored `cards.pets` alongside
`classes.pets`. A loaded card declaration retains authored actions and authored effects while its
`effects` contain the follow-mode compilation used for activation and execution. That
compilation preserves generic `CardLocation` movements, delegates printed-face predicates to the
client, and temporarily represents exact Event-pile links with `PlayedEvent`.
`TfmCatalog.card(name)` returns that loaded Class directly. Narrow card-query functions derive its
card back, tags, immediate instructions, actions, effects, cost, requirement, and card-resource type
from Pets. Concrete `CardFront` subclasses form the card registry, and each card's represented
`Class<CardBack>` determines its deck. Card resource directories preserve Module-specific card-pool
grouping and activate unreferenced non-card roots; ordinary Pets references activate the remaining
declarations. The engine alone decides which active Classes instantiate. Promo Card Pack contributes
three direct class exclusions for the cards its revised printings supersede; there is no general
replacement registry.

`Engine.newGame(premise)` wires the World, creates `Engine` and singleton components, marks
initialization complete, and commits the pre-setup baseline. It does not create a Phase.
Terraforming Mars workflow later creates `SetupPhase` as an ordinary effectful operation.

In Canon, exact-`This` singleton bootstrapping remains appropriate for premise-selected identities,
selected data families, Class representatives, and generic specialization fanout. Initialization
materializes Modules in an order consistent with active provenance, then Module effects create the
concrete components they own.

## Component graph

The component graph is only a multiset of concrete Types. Components have no fields or instance
identity. Equal Types are indistinguishable copies. The Kotlin `Component` type is therefore an
unboxed value wrapper when its use site permits, not an interned state object.

A concrete component may depend on other concrete components through its Type. Every possible
dependency target must have an applicable maximum-one invariant so the edge identifies one vertex.
Removing the last target cascades: `ComponentGraph` reports existing dependents, `Changer` removes
them first, then retries the original removal.

The only state mutation is a count plus optional source and destination. A transmutation removes
before it adds. Every successful mutation updates live-effect indexes and enters the Event Log.
`ComponentGraph.listenToCount` observes the live count of one resolved Type, reports its initial
value immediately, and reports later changes during both forward play and recording navigation.
The caller supplies the World's `GameReader` for abstract or refined Type evaluation and can cancel
the returned subscription. Listener failures do not interrupt state mutation.

`Custom` classes never enter the graph. Custom metrics report virtual non-negative counts; custom
instructions translate concrete input to instruction trees. A custom declaration may use
supertypes for dependencies and ownership, but the loader rejects inherited effects, invariants, and
instruction defaults so Kotlin translation remains its sole behavior.

## Events and timeline

The log contains `ChangeEvent`, `TaskAddedEvent`, `TaskRemovedEvent`, `TaskEditedEvent`, and the
diagnostic `GameplayInputEvent`. A change records its Actor and Cause, with changed component Types
stored as minimal round-tripping expressions. A successful Player command records its submitted
input and outer command-start ordinal so native export can recover choices without treating Cause
as input. Rendered history uses `BY` for Actor, `VIA` for the effect-bearing cause, and `BECAUSE` for
causal event ordinal.

Every event has optional diagnostic `agent` provenance. It is rendered when present but excluded
from event equality and gameplay-state equivalence. The current autoexecution bridge supplies its
mode as the agent for automatic Player inputs; older direct calls leave it absent.

`EventLog.record` and rollback are the single history/mutation interface: application or reversal
must succeed before the log changes. Each forward or reverse mutation advances an opaque
`WorldRevision`. Unlike an event-count checkpoint, a revision is never reused after rollback.
That distinction is intended to let a future overlay or fork detect any mutation of its backing
World even when the event count returns to the same value.

A log may capture another log as an immutable prefix in constant time. Later source events are not
part of the capture, and the source may not roll back that captured prefix while the suffix exists.

`Timeline` provides checkpoints, atomic blocks, rollback, and a commit floor. An atomic failure
reverses component state, tasks, event-backed indexes, and events. The current live-effect
`registryOrder` is an exception: remove/re-add rollback can assign a new ordinal, as audited in
[SEQUENCING.md](SEQUENCING.md#open-design-or-rules-audits). `AbortOperationException` requests
rollback without surfacing as a caller error. The commit floor prevents rollback into
initialization or a workflow stage.

`World.recording()` captures the current event sequence and positions after each successful
outermost gameplay operation. It also captures the position after the operation-completion callback,
so synchronously resumed workflow activity is one automatic follow-up step rather than many internal
task selections. A `GameRecording` can seek backward or forward only among those positions and the
final state. Seeking mechanically reverses or reapplies captured component and task events; it does
not run gameplay operations again. Capturing seals the World's public Timeline rollback surface to
the same positions; internal atomic failure recovery remains able to restore its operation
checkpoint.

Failure-atomicity is not game-rule atomicity. An operation whose intermediate changes fire effects
may still be observable one change at a time.

## Tasks are an unordered choice pool

Task iteration is stable for reproducibility, but order has no game meaning. A task has:

- stable `TaskId`, derived from its original add-event ordinal;
- one task-shaped `Instruction`;
- `assignee`, whose scoped queue contains it and who may select and narrow it;
- `actor`, recorded on resulting changes unless instruction-side `BY` overrides it;
- `cause`;
- selected flag;
- optional `THEN` continuation group; and
- diagnostic `whyPending`.

A temporary 1-based display position may disambiguate equal-looking tasks. It is not an id.

`InstructionTree` is the broad AST kind. `Instruction` is one task-shaped root.
`InstructionGroup` is a normalized comma-separated batch. Queue admission splits a group into one
task per member. Narrowing a grouped `OR` branch can likewise replace one task with several.

`A THEN B` stores A as current work and B as a continuation. Completing A enqueues B in its place;
B is not immediate and receives no priority over unrelated pending work. Open implicit variables can
prevent splitting until an earlier stage fixes their shared Type. Narrowing and resolution normalize
the task again, so the sequence splits once those shared values become concrete.

### Assignment and Actor

Assignee and Actor are different. For queued triggered work, the current compatibility rule assigns
to the Player owner of the effect-bearing component, otherwise the Player owner of the changed
component, otherwise the triggering Actor. It independently attributes the future change to the
effect's context owner when present, otherwise the triggering Actor.

Instruction-side `BY` changes only the Actor. Trigger-side `BY` matches only the Actor on the
trigger event. Splitting, narrowing, resolution, `THEN`, and cross-scope execution preserve the
stored Actor. See [IDENTITY.md](IDENTITY.md).

There is no parent/child queue suspension or delegated control scope. One selected task globally
locks selection of competitors. `TfmWorkflow.Auto` starts Player operations directly and waits
for whole-world idleness instead.

This is a known correctness gap for Philares. The current trigger-time assignment gives the
Philares owner its resource choice immediately and does not let the active Player choose when to
select that reward. The target selection-time handoff and its blocking requirement are specified
in [IDENTITY.md](IDENTITY.md); `BugsTest` preserves the two current incorrect behaviors until that
handoff exists.

### Selection, resolution, and narrowing

This is the current task lifecycle.

Players play through exactly two kinds of activity:

1. select one pending task, making it the work that must finish next; or
2. narrow the selected task by supplying one or more of its remaining choices.

Resolution and execution are engine consequences of those activities. Selection is an ordering
promise, not a Timeline commit; commit retains its transactional meaning after execution. A Player
with neither a selectable task nor narrowing authority over the selected task has nothing to do.

`InstructionTree` and its narrowable parts implement `Specification`: `isAbstract` reports whether
an externally supplied choice remains, while `narrows` and `ensureNarrows` compare two
specifications compositionally. This is independent of resolution. An unresolved gate, `PER`, or
AMAP instruction can already be non-abstract, and an unresolved instruction can be narrowed while
preserving its gate, metric, or refinement.

Selecting a task causes the engine to resolve its state-dependent parts against the current World.
Resolution repeats after each narrowing and:

- evaluates `PER` metrics;
- evaluates gates and optional no-ops;
- recursively resolves `OR` arms and removes locally impossible ones;
- narrows Types when exactly one concrete choice remains;
- resolves quantifiers and abstract choice domains as specified in
  [QUANTIFIERS.md](QUANTIFIERS.md);
- rejects limits;
- makes a reflexive nonmandatory transfer a no-op; and
- translates a valid concrete custom instruction.

Narrowing may be partial. Each accepted narrowing is recorded as Task state so the client
does not need a parallel memory of earlier sub-Specification choices. It is a Task Event, not a State
Change. Because the select-lock prevents intervening World mutation, later contextual checks,
including refinement narrowing, observe the same World. Resolution may winnow the options for every
choice that remains.

Choice enumeration should follow that same decomposition. Expose useful legal narrowings for one
sub-Specification, retain the Player's choice, resolve again, and then enumerate the next remaining
choice. Do not require a client to choose from the Cartesian product of every fully concrete
Instruction when its parts can be narrowed compositionally.

Once resolution has read state, an abstract task remains selected and must finish before any
competing mutation. A concrete result executes as part of the same command. A Selected Task retains
its resolved first stage rather than deriving it again; later linked stages resolve when reached,
against the state produced by earlier stages.

If resolution or narrowing exposes several independent instructions, the selected structural Task
completes and is replaced by ordinary Pending Tasks. No child inherits selection; choosing which
sibling comes next is a new Selection.

### Execution

Execution accepts resolved concrete work without resolving its first stage again. A `Change` goes
through `Changer`, logging, automatic effects, and queued effects. `By` selects an Actor. A
normalized `Then` inside inline execution runs its concrete stages; queued `THEN` tails were
separated when the task was created. `NoOp` does nothing. An unresolved gate, `OR`, scalar, Type, or
instruction group is an error.

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

Normal triggers scale their instruction by the matching change count. Self triggers respond only
to changed copies of the effect-bearing exact Type; existing equal copies do not multiply them.
Other subscriptions multiply by the number of live effect-bearing components.

An effect on an owned component listening to an unowned event defaults to matching only its Owner
unless it says `BY Anyone`. Unowned `System` components are engine-only; `Hidden` controls
presentation instead. `Signal` is hidden but not necessarily engine-only.

A positive abstract Actor selector can bind the matching Actor for reuse elsewhere in the trigger or
instruction. Type-variable occurrence paths likewise carry a concrete trigger narrowing into linked
instruction positions without rewriting coincidental equal Class Names.

`::` effects execute inline, recursively, before queued effects from the same concrete change are
admitted. A causal chain may contain at most eight nested automatic effects; exceeding that limit
fails the operation atomically with `RunawayEffectChainException`, which carries the attempted
chain. `:` effects become tasks. Use
[SEQUENCING.md](SEQUENCING.md) before depending on that difference.

### Terraforming Mars wild tags

`Tag` depends on `TagHolder`; `CardFront` is one such holder. Printed tags therefore remain ordinary
components such as `PlantTag<CardFront>`. A `WildTag` creates a distinct `WildTagUse` holder for
each `NewTurn` in Action or Prelude and each action-phase `SecondAction`. The temporary holder offers
the owner `Tag<This>?`, so a chosen wild meaning is a real tag and participates in bare tag
metrics and requirements.

The holder distinction is also the trigger distinction. `Tag` has the trigger default
`Tag<CardFront>:`, so an effect that reacts only to printed tags can explicitly accept it with
`PlantTag<>:` or spell out `PlantTag<CardFront>:`. It will not see
`PlantTag<WildTagUse<...>>`; there is no dispatch filter or special change kind. Refinements can
follow the dependency graph when card identity matters. Robotic Workforce uses
`CardFront(HAS BuildingTag OR WildTagUse(HAS BuildingTag))`, which accepts only the card whose
action-scoped wild holder received the Building interpretation.

`WildTagUse` is `Temporary`, so the action cannot finish while it remains. An unchosen offer is
uniquely implied action-scoped settlement: after the action body and all descendants drain, scoped
completion removes the acting player's remaining uses and their dependent tags disappear through
dependency cascade. It must finish before workflow offers `SecondAction`; a client Routine and the
later decision to decline that second action must not own this cleanup. `TfmGameplay` currently
performs the equivalent settlement directly and should migrate when scoped completion exists.

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
components. Numeric properties are Metrics. Stored Metric and Requirement syntax enters a
class effect only through `EVAL`; expansion substitutes the concrete receiver for `This`, can defer
until trigger specialization, and then receives normal defaults, `Owner` binding, and `PROD`
lowering. Map bonuses and other computed metadata remain justified custom metrics. Distinct live tag or
resource kinds use refined `Class<...>` Types instead.

Each `Class` retains its effective inherited invariants, and each active `ClassTable` projection
compiles them once into an immutable per-class component-limit lookup. A World's `Limiter` combines
that shared lookup with the live component graph to compute current headroom and footroom.
[QUANTIFIERS.md](QUANTIFIERS.md) specifies how concrete limits, abstract domains, dependencies, and
instruction composition determine the result.

## Recoverable dead ends

Task selection is speculative until the encompassing operation completes. A locally valid choice may
produce later work that cannot finish. That is a recoverable dead end: raise
`DeadEndException`, roll back to the encompassing checkpoint, and try another branch.

Do not duplicate target exclusions in Pets simply to prevent every impossible intermediate choice.
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
6. marked-syntax handlers registered by the World's Catalog.

A Catalog maps transform names to handlers bound to an active `ClassTable`. The generic
dispatcher traverses the AST, prevents same-kind nesting, and preserves unregistered transforms so
an earlier compilation stage can handle only the syntax it owns. Terraforming Mars registers
`PROD` lowering and follow-mode `CARDS` lowering. Card-source compilation invokes the same
dispatcher with only `CARDS`, leaving `PROD` for the active-table stage.

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
authority model. Normal task commands, manual operations, task edits, and `sneak` do not all
share identical atomic/auto-exec semantics. [API.md](API.md) proposes a mechanical flattening before
a separate safe client API.

`manual()` seeds a group of new tasks, permits an operation body to finish them, runs configured
auto-exec, preserves previously pending unselected tasks, and fails if newly created Tasks or
`Temporary` components remain. A pre-existing selected task prevents it from starting.
`sneak()` applies raw changes without normal instruction resolution or effects, but still uses the
timeline and graph mutation interfaces.

## Auto-execution and Terraforming Mars workflow

Auto-execution modes are:

- `NONE`: do nothing;
- `SAFE`: proceed only when one selectable option exists; and
- `FIRST`: choose the first selectable task in stable iteration order.

Scanning is global. Assignee selects the queue; stored Actor controls attribution. Failed candidates
receive `whyPending`. [AUTOEXEC.md](AUTOEXEC.md) records the measured duplication in the current
scheduling points and the proposed direction; it does not describe committed behavior.

`TfmGameplay` adds card, payment, production, parameter, and phase conveniences around the generic
layers. Treat it as transitional; test conveniences and player-facing domain actions need not
remain one production wrapper.

`TfmWorkflow.Auto` runs the Terraforming Mars phase loop in a coroutine. It commits before waiting
for tasks to drain and wakes from the shared outermost atomic-completion callback. StartToken
determines turn order. Canon represents every condition currently preventing game end as a
`GameEndBarrier`; the workflow checks for those components after Production and reads the solo
`Victory` result rather than reimplementing its predicate. Exact phase requirements and known gaps are
in [WORKFLOW.md](WORKFLOW.md).

## Wiring details

`Engine.Wiring` is the manual composition root. Class Table, Event Log, Component Graph, Effector,
Timeline, and other World-level services are shared. Each Actor receives its own `Changer`,
`Instructor`, `Implementations`, and `ApiTranslation` scope.

Kotlin keeps `Actor` and `Owner` distinct. Current Players are both. A passive Pets Owner such as
`SoloOpponent` has no gameplay scope or task queue.

The Effector receives a GameReader provider to break its construction cycle: it must exist before
the final reader can be assembled, while firing later requires that reader.
