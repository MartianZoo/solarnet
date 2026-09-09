# Engine model

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing live World construction, components, events, tasks, effects, rollback,
> recoverable dead ends, input transformation, recordings, or the current `Agent` surface.
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
| Component state | Component graph |
| Current event, timeline, or recording implementation | Events and timeline |
| Tasks, assignment, selection, narrowing, resolution, or execution | Tasks are an unordered choice pool through Execution |
| Triggered or automatic behavior | Effects; then the relevant section of [SEQUENCING.md](SEQUENCING.md) |
| Limits, refinements, AMAP, or quantification | Metrics, refinements, and limits; then [QUANTIFIERS.md](QUANTIFIERS.md) |
| Core mutation API or current autoexecution | Current Agent surface; Current auto-execution and workflow; then [API.md](API.md) |
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
 - [`Agent.kt`](../../src/common/dev/martianzoo/engine/Agent.kt) — search for
   `public interface Agent` before changing caller-facing operations.
 - [`PetElaborator.kt`](../../src/common/dev/martianzoo/pets/PetElaborator.kt),
   [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt), and
   [`ApiTranslation.kt`](../../src/common/dev/martianzoo/engine/ApiTranslation.kt) — inspect together
   for authored elaboration, class/component specialization, and Player-scoped input.
 - [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — search for `resolve` and
   `doExecuteResolved` for the selected-task resolution and executable-first-stage contract.
- [GAMEWORLD.md](GAMEWORLD.md) owns the selected extraction of passive World data, recording
  navigation, and exports from this current implementation.

## Game construction

A live Game World is a `World` containing:

| Part | Meaning |
| --- | --- |
| `ComponentGraph` | Present state: a multiset of concrete components |
| Global task queue | Deferred work and Actor choices, with one assignee on each Task |
| `EventLog` | Applied component and task history |
| `Timeline` | Atomicity, rollback, revision, and commit floor |
| `ClassTable` | The closed vocabulary and type relationships |
| Mutation executor | Validation and atomic calculation for direct Actor-attributed calls |

The planned `:gameworld` library owns the complete replayable data of one game: component state,
exact pending tasks, event history, readable projections, and approved recording positions. It
stores task Instructions as inert data but has no task execution, effects, Agent, or autoexecution.
The engine consumes that Game World and owns task and instruction behavior. The planned `:agent`
library consumes the engine and supplies the normal Actor-scoped client API and optional policies.
See [GAMEWORLD.md](GAMEWORLD.md),
[RESPONSIBILITIES.md](RESPONSIBILITIES.md#selected-runtime-dependency-direction), and
[API.md](API.md). Current code still combines these responsibilities in `World` and `:engine`.

`GameConfig` is unresolved user intent. Catalog-specific resolution composes concrete Player
Classes named by the configuration, then applies defaults, selection policy, and validation to
produce an immutable `GamePremise`. The premise contains one Catalog, selected Modules, signed
class selections, seat-ordered Player Class Names, and exact concrete types to create once. See
[OPTIONS.md](OPTIONS.md).

A premise lazily forms and retains one immutable active `ClassTable` projection. Every World built
from that premise shares the projection and its compiled class metadata while retaining independent
component, effect, task, event, timeline, and gameplay state.

Each Catalog owns one validated master `ClassTable`. A game's table projects it: selected Classes
are active and every other Catalog-known Class is uninhabited. Each configured player name is a
concrete Player Class in the composed Catalog and an explicit projection root. Trigger positions are
observational and do not activate their protocol Classes. Modules directly create the concrete
standard actions and other protocols they issue; generic families use `EACH` over the structurally
present `Class<T>` representatives only when the family itself owns the fanout.

Module defaults and premise requirements are authored in Pets. The Catalog resolves defaults to a
fixed point; the engine checks each selected Module's premise requirement and configuration-facing
invariants against the resolved projection before creating the World. Ambient Class ownership
derives compatibility conditions from source declarations and lowered structured data. Bundle
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
`effects` contain any follow-mode compilation needed for activation and execution. Ordinary card
location movements require no compilation; the remaining `CARDS[...]` zones delegate
printed-face predicates to the client.
`TfmCatalog.card(name)` returns that loaded Class directly. Narrow card-query functions derive its
card back, tags, immediate instructions, actions, effects, cost, requirement, and card-resource type
from Pets. Concrete `CardFront` subclasses form the card registry, and each card's represented
`Class<CardBack>` determines its deck. Card resource directories preserve Module-specific card-pool
grouping and activate unreferenced non-card roots; ordinary Pets references activate the remaining
declarations. Domain components exist only when the premise or an explicit creator produces them.
Promo Card Pack contributes
three direct class exclusions for the cards its revised printings supersede; there is no general
replacement registry.

`Engine.newGame(premise)` wires the World with one structural representative for every active
concrete Class, then creates `Admin`. Admin creates `BootstrapPhase`, which creates the generated
`Premise` component. Its immediate effects create the `BaseGameModule` first, then the other
literally named Modules, seated Players in order, and the premise's exact initial components. Its
queued `ModulesReady` signal runs after that complete layer exists. The initializer then drains the
remaining queued work and performs a final drain. Completion requires an empty task queue and every
premise-required component to exist
before the initialized state is committed. Structural Class representatives are installed before
event logging and therefore produce no Change Events. By the time `newGame` returns, the World has
one Phase, every seated Player, and each Player's five `ProdOffset<Class<MC>>` components; workflow
later replaces Bootstrap with `SetupPhase` as an ordinary effectful operation.

This staging is deliberate. The generated declaration is the executable form of the already
resolved Module selection; live effects do not choose defaults from a partial World. Queued
self-effects execute only after all Modules and Players have been created, so a queued `EACH` sees
that whole layer. The component carrying the effect remains the cause of the resulting changes.
Stable drain order is diagnostic, not game meaning.

Required state should arise at its earliest honest owner. The base Module creates initial
global-parameter status, Player1 creates the first-player token, the selected map creates its Mars
areas, and Game Modes create their distinct starting ratings during Setup. This gets resting
invariants true promptly while preserving what each event means.

Bootstrap tasks must have exactly one possible concrete outcome. Their syntax may begin abstract
only when ordinary resolution proves a single concrete alternative from the initialized state; no
bootstrap task may choose among two or more legal outcomes. Choice-bearing starting state must
remain an exact premise component and open its choice during `SetupPhase` or later, as selected
Colonies do. Queued `:` and immediate `::` still have their ordinary semantics; the bootstrap drain
is not permission to replace one with the other mechanically or to discard a change's `?`, `.`, or
`!` intensity.

`drainBootstrapTasks` currently calls the Admin Agent's normal `FIRST` autoexecution policy.
`FIRST` may select the stable execution order of several concrete tasks, but it does not invent a
narrowing for an abstract task: unresolved choice remains queued and bootstrap completion fails.
Preserve that rejection, cover it with a focused multi-alternative bootstrap test, and review task
ordering separately whenever bootstrap effects can observe one another.

**Forward-looking:** Kotlin `Engine` remains the passive mechanism that calculates responses to
Actor-attributed mutations. The current administrative Actor and Component become `Admin`.
Bootstrap should create only the state that cannot yet arise from an ordinary Admin task, then hand
control to Admin as soon as possible. Discover that minimum during extraction rather than requiring
an up-front inventory. Do not prolong special initialization merely because the current
`Initializer` can create more directly.

Keep three bootstrap layers distinct:

1. **Structural construction** forms the Class Table, state indexes prepopulated with Class
   representatives, history, timeline, and passive mutation executor. There is not yet an Actor
   mutation to record.
2. **Actor bootstrap** establishes the minimum concrete state needed for Admin to exist as an Actor
   Component and receive ordinary work. Add other directly created premise state only when the
   ordinary task route proves circular.
3. **Game initialization** begins at the earliest point where history can honestly say that Admin
   is selecting, narrowing, and executing assigned tasks. Terraforming Mars now names this interval
   with `BootstrapPhase`; Module activation and Player creation should move under ordinary phase
   work wherever the model can express them without circular prerequisites.

The goal is not to call every constructor step an Admin action. It is to make the special prefix as
short and explicit as possible, then use the ordinary task lifecycle for everything after the
handoff.

In Canon, the initializer directly materializes only `Admin` and `BootstrapPhase`, then creates the
generated `Premise` with the phase as its cause. Immediate Premise effects create all selected
Modules, seated Players, and exact initial component Types; direct initialization remains only an
idempotent fallback for copied custom premises. Module and Player effects create their owned
bootstrap state. An exact `HAS =1 This` remains a live multiplicity invariant, not an initialization
instruction.

## Component graph

The component graph is only a multiset of concrete Types. Components have no fields or instance
identity. Equal Types are indistinguishable copies. The Kotlin `Component` type is therefore an
unboxed value wrapper when its use site permits, not an interned state object.

A concrete component may depend on other concrete components through its Type. Every dependency
target admitted by a concrete class must have an applicable maximum-one invariant so the edge
identifies one vertex. An abstract class may defer that proof, but each concrete subclass must
narrow or otherwise satisfy the inherited dependency bound. Removing the last target cascades:
`ComponentGraph` reports existing dependents, `Changer` removes them first, then retries the original
removal.

The only state mutation is a count plus optional source and destination. A transmutation removes
before it adds. Currently every successful mutation updates live-effect indexes and enters the
combined Event Log.
`ComponentGraph.listenToCount` observes the live count of one resolved Type, reports its initial
value immediately, and reports later changes during both forward play and recording navigation.
The caller supplies the World's `GameReader` for abstract or refined Type evaluation and can cancel
the returned subscription. Listener failures do not interrupt state mutation.

**Forward-looking:** `GameWorld` applies only a fully concrete gain, removal, or transmutation. It
does not index or fire effects. The engine invokes that operation and explicitly reacts to a neutral
description of what changed. Recording playback invokes the same passive application without an
engine, so recorded consequences are never calculated twice. [GAMEWORLD.md](GAMEWORLD.md) owns the
contract.

`sneak` therefore remains an engine cheat, not a state operation. Normal execution and `sneak`
apply the same concrete `GameWorld` mutation; the engine decides whether to process the reported
change through effects.

`Custom` classes never enter the graph. Custom metrics report virtual non-negative counts; custom
instructions translate concrete input to instruction trees. A custom declaration may use
supertypes for dependencies and ownership, but the loader rejects inherited effects, invariants, and
instruction defaults so Kotlin translation remains its sole behavior.

## Events and timeline

The log contains `ChangeEvent`, `TaskAddedEvent`, `TaskRemovedEvent`, and `TaskEditedEvent`. A change
records its Actor and Cause, with changed component Types stored as minimal round-tripping
expressions. Rendered history uses `BY` for Actor, `VIA` for the effect-bearing cause, and `BECAUSE`
for causal event ordinal.

`EventLog.record` and rollback are the single history/mutation interface: application or reversal
must succeed before the log changes. Each current event has one integer ordinal. Each forward or
reverse mutation advances an opaque `WorldRevision`; unlike the event-count checkpoint, a revision
is never reused after rollback.

A log may capture another log as an immutable prefix in constant time. Later source events are not
part of the capture, and the source may not roll back that captured prefix while the suffix exists.

`Timeline` provides event-count checkpoints, atomic blocks, rollback, and a commit floor. An atomic
failure reverses component state, tasks, event-backed indexes, and events.
`AbortOperationException` requests rollback without surfacing as a caller error. The commit floor
prevents rollback into initialization or a workflow stage.

`World.recording()` captures the event sequence and selected positions around successful outermost
Agent completion. `GameRecording.seek` currently reverses or reapplies those events on the same live
`World`, and capturing seals its public rollback surface to those positions. This coupling is
transitional. The selected model exports immutable history and opens an independent scrollable Game
World view whose public seek targets are only completed positions, never arbitrary event ordinals.
See [GAMEWORLD.md](GAMEWORLD.md).

Failure-atomicity is not game-rule atomicity. An operation whose intermediate changes fire effects
may still be observable one change at a time.

## Tasks are an unordered choice pool

Task iteration is stable for reproducibility, but order has no game meaning. A task has:

- stable `TaskId`, derived from its original add-event ordinal;
- one task-shaped `Instruction`;
- `controller`, which owns the surrounding operation and receives resulting work;
- `assignee`, who may select and narrow it;
- `narrower`, who supplies choices after selection;
- `actor`, recorded on resulting changes unless instruction-side `BY` overrides it;
- `cause`;
- selected flag;
- optional `THEN` continuation group.

Clients normally identify work by an instruction that uniquely narrows one task. Code that already
holds an exact task may use its stable `TaskId`; presentation order never identifies a task.

Semantically there is one Game World task queue. Actor-specific queues are current filtered API
views, not independent state containers. `Agent.tasks` may present the fiction of one Actor's queue
without promoting that view into the Game World storage model.

`InstructionTree` is the broad AST kind. `Instruction` is one task-shaped root.
`InstructionGroup` is a normalized comma-separated batch. Queue admission splits a group into one
task per member. Narrowing a grouped `OR` branch can likewise replace one task with several.

`A THEN B` stores A as current work and B as a continuation. Completing A enqueues B in its place;
B is not immediate and receives no priority over unrelated pending work. Open implicit variables can
prevent splitting until an earlier stage fixes their shared Type. Narrowing and resolution normalize
the task again, so the sequence splits once those shared values become concrete.

### Controller, assignment, and Actor

Task stores two Actors: the controller and contextual Actor. Queued work triggered during a
Player-controlled operation retains that controller. Its contextual Actor follows the effect owner,
changed-component owner, or triggering Actor. Admin-driven setup and workflow retain that same
routing. A three-state lifecycle distinguishes unselected work, controller-selected work, and work
delegated after selection. The assignee is the controller in the first two states and the contextual
Actor in the third. That Actor also supplies the queued task's default performer. Automatic effects
execute inline and keep the effect owner when present or the triggering Actor otherwise.

Instruction-side `BY` changes only the Actor. Trigger-side `BY` matches only the Actor on the
trigger event. Splitting, narrowing, resolution, `THEN`, and cross-scope execution preserve the
stored Actor. See [IDENTITY.md](IDENTITY.md).

Selecting an abstract task moves that same selected task to its contextual Actor's queue when
needed, while retaining its controller. One selected task globally locks selection of competitors.
Continuations, structural siblings, and triggered work return to the controller. `TfmWorkflow.Auto`
starts Player operations directly and waits for whole-world idleness instead.

### Selection, resolution, and narrowing

This is the current task lifecycle.

Actors play through two kinds of ordinary activity:

1. select one pending task, making it the work that must finish next; or
2. narrow a task by supplying one or more of its remaining choices.

Resolution and execution are engine consequences of those activities. Selection is an ordering
promise, not a Timeline commit; commit retains its transactional meaning after execution. An Actor
with neither a selectable task nor any legal task narrowing has nothing to do.

The current API permits an unselected task to be narrowed by id only when a compositional check
discards options without consulting mutable World state. It does not resolve or execute the task.
Selected-task narrowing remains state-aware and resolves again before executing a concrete result.
Arbitrary task replacement is private engine bookkeeping.

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

Execution accepts a selected Task whose first stage has already been resolved against the locked
World. The current executable-first-stage algebra is implicit in `Instructor.doExecuteResolved`:

- `NoOp`;
- a `Change` with an actual count, mandatory intensity, and concrete component Types;
- `By` around executable work, with one concrete participating Actor; or
- `Then` with an executable first stage and later Pets stages that resolve only when reached.

`Gated`, `Per`, `Or`, custom gains, marked transforms, and instruction groups cannot be the resolved
first stage. Resolution must consume, choose, translate, or split them first. Later `THEN` stages
remain Pets because their gates, metrics, limits, and linked values must observe the World produced
by earlier stages.

These facts are checked by `isAbstract`, casts, and `when` branches while the value remains a
`pets.ast.Instruction`. A dedicated executable-first-stage type could encode the same small algebra
directly, store concrete component Types and an Actor rather than Expressions, and retain later Pets
only as an explicit continuation. That would change representation, not execution semantics.

A `Change` goes through `Changer`, logging, automatic effects, and queued effects. `By` selects an
Actor. `NoOp` does nothing. Queued `THEN` tails were separated when the Task was created.

Queued effects return `PendingTask` values and receive ids only when admitted. Inline automatic
effects never receive task ids.

## Effects

### Life of an effect

The normal path is a progression toward less implicit, more directly executable data:

1. **Authored AST.** This is Pets in its convenient source form, whether it came from a declaration,
   client input, or a custom implementation returning source-shaped Pets. It may omit dependencies
   supplied by `DEFAULT`, contain marked syntax such as `PROD[...]`, and refer to contextual `This`
   or `Owner`.
2. **Elaborated Pets.** The authoring context identifies which eligible Type occurrences are uses of
   the same Type variable before rewriting can obscure that fact. Desugaring then makes defaults
   explicit, converts actions to effects, splits counted atomized gains, and replaces registered
   marked syntax with ordinary Pets. The result remains in the authorable Pets language; it is just
   less convenient to write. It may nevertheless carry typed variable identity that cannot be
   reconstructed by stringifying one subtree and parsing it without its original region context.
3. **Context-closed Pets.** Successive contexts bind the variables they own and write their values
   into the typed tree. Class/component attachment binds `This`, contextual `Owner`, and class-header
   variables. Trigger matching binds trigger-local variables and counts. A request submitted in a
   Player scope directly substitutes that Player for contextual `Owner`. For example, `Plant`
   submitted as Player 2 ultimately becomes `Plant<Player2>` and no longer needs that scope to retain
   its meaning. “Contextualization” names the operation; “context-closed” names the resulting
   independence without suggesting that information was discarded.
4. **Resolved first stage.** Selection locks the World, then state-dependent gates, metrics, limits,
   choices, custom instructions, and abstract Types are resolved as far as the first executable
   stage requires. The exact output contract is described under Execution; later `THEN` stages stay
   as Pets until their turn.

The implementation does not enforce these as different Kotlin representations. It reuses the same
`PetNode` hierarchy, and some operations combine adjacent steps. The operational effect names map
onto the progression as follows:

- A declaration retains `authoredEffects` and `authoredActions`. Its executable `effects` also
  include actions converted to effects and may contain Catalog-specific source compilation.
- `PetElaborator.classEffects` collects inherited effects for an active Class, inserts defaults,
  atomizes, lowers marked syntax, and evaluates properties as far as the Class context permits. A
  class effect may still contain context-relative or event-relative values.
- `LiveEffect.compile` specializes a class effect to one exact component Type. Apart from the
  dedicated self-trigger representation, `This` now has no contextual role: ordinary occurrences
  have become the exact component Type. A contextual `Owner` is also bound when that context has one.
  The resulting component effect is wrapped with its subscription as a live effect.
- Trigger matching supplies event-local linkage values and counts, producing the instruction that
  becomes inline automatic work or a queued Task.

Ad-hoc client input skips the class/component-effect steps. Its input pipeline desugars it and closes
over the Player context before admitting Tasks. It may remain abstract because a Player choice is
still required; context-closed does not mean concrete or resolved.

`This` is a fake Class name with no context-free Type. Outside the dedicated self-trigger form, it is
meaningful only in declaration syntax that supplies a Class or component context and it disappears
by the component-effect stage. `Class<This>` retains the root Class identity without dependencies.
Static Class construction and the current specialized `This<...>` invariant behavior are specified
in [TYPES.md](TYPES.md#inherited-and-narrowed-dependencies).

`Owner` currently conflates two roles: a contextual value to bind and the ordinary abstract `Owner`
Type, whose concrete choices include seated Players and `SoloOpponent`. Consequently, failure to
bind a contextual occurrence can silently turn it into a broad choice. A context-closed form needs
to distinguish an intentionally open Owner-domain choice from an unbound contextual variable;
merely leaving the same `Owner` Expression behind cannot prove which was intended.

### Authored form as data

Authored AST is deliberately retained rather than consumed. It is immutable parsed rule data, not a
token-preserving source tree: parsing has already normalized syntax, lowered owner-local derived
Class declarations, and identified Type-variable occurrences. Consumers are free to inspect,
analyze, copy, or re-submit it. The useful contract is therefore its stable semantic shape, not a
whitelist of permitted consumers: authored effects remain separate from action-lowered or
Catalog-compiled executable effects; omitted defaults and marked syntax remain visible; stored
Metric and Requirement values remain inert; and no component or trigger binding has been applied.

`authoredEffects` and `authoredActions` enforce this source/executable split. Property values do not
yet have parallel authored and executable slots: Catalog source compilation can rewrite the value
stored on the loaded declaration. That is a gap in the otherwise useful authored-data contract, not
a reason for consumers to depend on whichever compilation happened to run first.

Known mechanisms that return authored data to normal elaboration include:

- stored Metric- and Requirement-valued properties remain inert until `EVAL` includes them in a
  class effect;
- `CopyProductionBox` locates and returns the one authored `PROD[...]` subtree;
- `CopyPrelude` returns the copied card's authored immediate instruction; and
- `ScoreEventVps` returns authored end-game effect instructions.

Card tags, card requirements, authored actions, `GainsOf`, and `NonNegativeIconsOf` also inspect
authored syntax, but currently use it as metadata rather than re-executing it. These accesses are
reflection-like: ordinary execution moves forward through the lifecycle, while source inspection
explicitly reaches into preserved directives as data. Re-submitted data must re-enter through the
same elaboration path as any other authored Pets.

`PetElaborator` owns the shared authored-to-elaborated packages for session input, Metric input,
Class Effects, and source-shaped custom-instruction output. Its private transformers supply one
meaning for defaults, atomization, marked syntax, and property expansion. The engine chooses which
public elaboration operation applies and supplies live component, Owner, trigger, or fanout context;
it does not reconstruct those transformation chains. Reflection-like re-entry likewise returns
through the matching elaboration operation.

The `Effector` indexes live component-effect pairs with their component multiplicity.

Triggers are self-gain `This:`, self-removal `-This:`, gain/remove subscriptions to another Type,
and `OR` combinations. Wrappers add:

- trigger-side `BY` Actor matching;
- `IF` state requirements; and
- `X` “one response for any positive matching count” behavior.

Normal triggers scale their instruction by the matching change count. Self triggers respond only
to changed copies of the effect-bearing exact Type; existing equal copies do not multiply them.
Other subscriptions multiply by the number of live effect-bearing components.

An effect on an owned component listening to an unowned event defaults to matching only its Owner
unless it says `BY Anyone`. Unowned `System` components are Admin-only; `Hidden` controls
presentation instead. `Signal` is hidden but not necessarily engine-only.

A positive abstract Actor selector can bind the matching Actor for reuse elsewhere in the trigger or
instruction. Type-variable occurrence paths likewise carry a concrete trigger narrowing into linked
instruction positions without rewriting coincidental equal Class Names.

`::` effects execute inline, recursively, before queued effects from the same concrete change are
admitted. A causal chain may contain at most eight nested automatic effects; exceeding that limit
fails the operation atomically with `RunawayEffectChainException`, which carries the attempted
chain. `:` effects become tasks. Use
[SEQUENCING.md](SEQUENCING.md) before depending on that difference.

## Metrics, refinements, and limits

`GameReader.count` evaluates component counts, union metrics, and custom metrics. A union is a
multiset union: for each exact component Type, keep the greatest matching multiplicity so overlapping
arms do not double count. Its arms must be distinct component counts; capped, scaled, subtractive,
property, and virtual custom counts cannot participate because they have no component identity.
Numeric Metrics may also subtract Metrics or positive scalar operands, saturating at zero; a scalar
by itself is not a Metric. Complete-group scaling and `MAX` bind before subtraction, which binds
before union.

`RANK Selector { Metric, ... }` is a highest-first competition rank over the distinct live Types
matching `Selector`: equal score vectors receive the same rank and later ranks skip the tied places.
Multiple Metrics are compared lexicographically. Each score binds the candidate name and contextual
`Owner` as an `EACH` body does, including occurrences inside `NOT` refinements. There is no
direction keyword; a known upper cap minus a Metric can express lowest-first scoring.

An abstract custom metric specializes only over dependency targets represented by live components,
then sums the satisfying concrete implementations. This follows the ordinary dependency rule that
a dependent value cannot exist without its targets and avoids enumerating the full structural
cross-product. Kotlin metric invocations always receive concrete dependency arguments.

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

An invariant has no constructive meaning. In particular, a positive lower bound can activate its
named Classes during projection, but it neither creates the required Components nor chooses their
concrete Types. Bare `HAS requirement` is therefore the preferred presence statement when a
separate rule already guarantees uniqueness. Structural `Class<T>` representatives are the clearest
case: `HAS Class<T>` says that T must be active, while `=1` would merely repeat their structural
multiplicity.

### Multiplicity audit

Use exact one for state that must be present at every applicable resting point and has an explicit
creator. Current examples are selected Modules and Players, Areas, track-rule providers, permanent
action providers and slots, Admin, and the solo opponent and reserve providers. Use
maximum one when zero is a legitimate state: card locations and fronts, cleanup and once-per-round
markers, claimed goals and funded awards, required actions and passing, payment state, phase-local
rules, global-parameter completion state, end barriers, and setup operations. `Milestone` was the
missing maximum-one declaration found by the current Canon audit. `TradeFleet` deliberately has no
one-count limit: additional fleet components are real capacity granted by cards.

`StartToken` is exact one: the generated Premise creates it with the explicit binary `AfterMe`
ring, and each `ResearchPhase` moves it along that relation by atomic transmutation. Each Player
permits at most one incoming and one outgoing edge. Phase is likewise exact one after Admin creates
BootstrapPhase; each transition replaces the current Phase, and `End` remains as the terminal Phase.
A separate temporary
`FinalScoringPending` component supplies the completion event that assigns multiplayer victory after every
scoring task settles. A future comprehensive lower-bound validator must account for the short
construction interval before Admin creates BootstrapPhase. Bootstrap completion
verifies its required components and empty task queue; ordinary mutations continue to enforce
applicable multiplicity limits.

**Audit:** bootstrap verification checks premise Modules, Players, and exact initial component
Types, not every positive lower bound or every source-owned support component. Canon's lifecycle
tests currently prove `StartToken` and track-status initialization; the generic initializer would
not itself detect their accidental omission.

`GpIncomplete` and `GpComplete` are two faces of one status and are the strongest candidate for an
exact-one sum; expressing that honestly requires one shared status family and an atomic
transmutation. The proposed available/spent card-action status in
[ACTIONS.md](ACTIONS.md#open-questions) has the same shape.

### Exact lower bounds and transient repair

The current Limiter checks a lower bound when removing and an upper bound when gaining. It permits
the initial gain from zero, but it does not prove that every externally observable resting state
satisfies every positive lower bound. Consequently, changing a declaration from maximum one to
exact one is not yet proof that the component is always present.

The smallest promising completion is asymmetric enforcement: keep upper bounds immediate, allow a
lower bound to be temporarily false while one automatic consequence chain repairs it, and validate
lower bounds before unrelated work can proceed. Atomic transmutation needs no such allowance because
the old and new Types hold their shared invariant constant. Validation cannot occur at
every `Timeline.atomic` exit because an operation may intentionally leave Player-choice Tasks.
Nor should it wait for whole-World idleness, which can mix unrelated work. The validation point
should instead be the completion of the causal task scope described in
[SEQUENCING.md](SEQUENCING.md#selected-direction-scoped-completion). Relational invariants such as an
Event Card's exact printed event tag should be instantiated only for a live owning component; an
absent card must not require its dependent tag.

Until that completion rule exists, retain maximum one for lifecycle state and use exact one only
where the explicit creator and current execution path already make absence non-resting. Do not add
a second representation or a hidden “repairing” marker merely to permit the transient state.

**Disposition: at peace with the operator set.** `Metric.Max`, `Metric.Subtract`, and `Metric.Or`
each have only a handful of authored uses, almost all inside `Award.metric`, so a sweep for
single-client machinery flags them. The measurement is backwards: the algebra is *under*-built, not
over-built. `Subtract` saturates but there is no `Add`, which is needed for global events that add
Influence after a capped or grouped Metric. Union and sum are also genuinely different operators —
Awards need `Or`'s non-double-counting union, Turmoil needs arithmetic addition — so neither can
stand in for the other. Propose completing this algebra, not trimming it.

`Metric.Rank` removed the former custom award-placement and multiplayer-victory instructions.
Turmoil can reuse its comparison semantics, though its distinct tie-sensitive state changes remain
a separate modeling question.

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

Current Actor-scoped string input passes through this order:

1. localized Vocabulary canonicalization and input-only synonyms;
2. Class-Name resolution against the World table;
3. atomization of counted `Atomized` components;
4. dependency defaults;
5. contextual `Owner` replacement for Player scopes; and
6. marked-syntax handlers registered by the World's Catalog.

In the lifecycle terminology above, this elaborates authored input and closes it over the acting
Player. It need not yet be concrete or resolved, because the submitted work may deliberately leave
choices for Task narrowing. The current implementation performs the `Owner` substitution directly;
no separate domain context object participates.

Forward-looking access or application code may own string parsing and call direct typed engine
methods; the core engine still owns validation, contextual closing required by game semantics,
resolution, and execution. The extraction must not let one layer elaborate a different Pets
meaning from another.

A Catalog maps transform names to handlers bound to an active `ClassTable`. The generic
dispatcher traverses the AST, prevents same-kind nesting, and preserves unregistered transforms so
an earlier compilation stage can handle only the syntax it owns. Terraforming Mars registers
`PROD` lowering and follow-mode `CARDS` lowering. Card-source compilation invokes the same
dispatcher with only `CARDS`, leaving `PROD` for the active-table stage.

Source-shaped AST returned by custom implementations enters through
`PetElaborator.elaborateCustomInstruction`. Public elaboration entry points preserve their declared
AST family; instruction entry points use `InstructionTree` where cardinality may change.

## Current Agent surface

Each World retains exactly one fully permissive `Agent` per Actor. `World.agent(actor)` returns that
stable object for reads, task commands, manual operations, task insertion/removal, and direct
changes. The old power-interface hierarchy is gone. REPL color modes restrict commands in the script client
rather than changing the engine object's type. Autoexecution policy attachment is forward-looking.

All public Agent mutations share the outer atomic-completion path.

`manual()` seeds a group of new tasks, permits an operation body to finish them, runs configured
auto-exec, preserves previously pending unselected tasks, and fails if newly created Tasks or
`MustCleanUp` components remain. A pre-existing selected task prevents it from starting.
`sneak()` applies raw changes without normal instruction resolution or effects, but still uses the
timeline and graph mutation interfaces.

**Forward-looking:** `:agent` owns the normal Actor-scoped client API. Agent calls the core engine's
audited mutation families against the Game World's task queue; a separate passive access object is
not needed. Actor assignment remains engine semantics even though the resulting assignment is Game
World data. Agent is the sole issuer of ordinary explicit and policy-chosen mutations for one Actor.
Direct engine primitives remain available for workflows, replay correction, cheats, and tests;
preventing trusted callers from using them is not a current goal. Public task mutation is already
limited to checked narrowing and explicit single-task removal.

## Current auto-execution and Terraforming Mars workflow

Autoexecution currently uses `Agent.autoExecMode`: `NONE` does nothing, `SAFE` proceeds only when
one selectable option exists, and `FIRST` chooses the first selectable task in iteration order.
Scanning is global; assignee selects the queue and stored Actor controls attribution.

**Forward-looking:** core engine contains no autoexecution. An application creates one Agent per
Actor, and each Agent owns its optional policies. After one engine mutation and its immediate
consequences finish, shared Agent wiring gives every Agent a chance to act. If one acts, the wiring
starts over against the changed game; the original Agent call returns after a complete pass in
which none acts. A normal application makes Admin fully autonomous, but the engine is indifferent
to every legal policy choice. [AUTOEXEC.md](AUTOEXEC.md) owns that target and records the current
divergence.

 `TfmGameplay` adds card, payment, production, parameter, and phase conveniences around the generic
 `Agent`. Treat it as transitional; its test conveniences and player-facing domain actions need not
 remain one production wrapper.

`TfmWorkflow.Auto` runs the Terraforming Mars phase loop in a coroutine. It commits before waiting
for tasks to drain and wakes from the shared outermost atomic-completion callback. StartToken
determines turn order. Canon represents every condition currently preventing game end as a
`GameEndBarrier`; the workflow checks for those components after Production and reads the solo
`Victory` result rather than reimplementing its predicate. Exact phase requirements and known gaps are
in [WORKFLOW.md](WORKFLOW.md).

## Wiring details

`Engine.Wiring` is the current manual composition root. Class Table, Event Log, Component Graph,
Effector, Timeline, `Changer`, `Instructor`, and other World-level services are shared; `Changer` and
`Instructor` take the acting Actor as a parameter rather than holding one. Each Actor currently
receives its own `Implementations` and `ApiTranslation` scope.

The target engine composition retains only the behavior and Actor context required to calculate one
direct mutation. Game World retains Actor identities, assignment, and pending choices as data but
contains no decision-making behavior. Actor-filtered reads, unique long-lived Agents, their
policies, and the shared autoexecution loop belong in `:agent`.

Kotlin keeps `Actor` and `Owner` distinct. Current Players are both. A passive Pets Owner such as
`SoloOpponent` has no gameplay scope or task queue.

The Effector receives a GameReader provider to break its construction cycle: it must exist before
the final reader can be assembled, while firing later requires that reader.
