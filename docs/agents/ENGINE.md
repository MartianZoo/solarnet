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
 - [`Transformers.kt`](../../src/common/dev/martianzoo/engine/Transformers.kt),
   [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt), and
   [`ApiTranslation.kt`](../../src/common/dev/martianzoo/engine/ApiTranslation.kt) — inspect together
   for authored elaboration, class/component specialization, and Player-scoped input.
 - [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — search for `resolve` and
   `doExecuteResolved` for the selected-task resolution and executable-first-stage contract.

## Game construction

A live Game World is a `World` containing:

| Part | Meaning |
| --- | --- |
| `ComponentGraph` | Present state: a multiset of concrete components |
| Global task pool | Deferred work and Actor choices, with one assignee on each Task |
| `EventLog` | Applied component and task history |
| `Timeline` | Atomicity, rollback, revision, and commit floor |
| `ClassTable` | The closed vocabulary and type relationships |
| Mutation executor | Validation and atomic calculation for direct Actor-attributed calls |

The planned `:state` library owns a narrower `GameWorld`: concrete component data, readable
projections, and fully concrete gain, removal, and transmutation only. It has no Instructions,
tasks, effects, Agent, or autoexecution. The engine consumes that state API and owns task and
instruction semantics. The planned `:agent` library consumes the engine and supplies the normal
Actor-scoped client API and optional policies. See
[RESPONSIBILITIES.md](RESPONSIBILITIES.md#selected-runtime-dependency-direction) and
[API.md](API.md). Current code still combines these responsibilities in `World` and `:engine`.

`GameConfig` is unresolved user intent. Catalog-specific resolution applies defaults, selection
policy, and validation to produce an immutable `GamePremise`. The premise contains one Catalog,
selected Modules, signed class selections, seat-ordered display names, and exact non-singleton types
to create once. See [OPTIONS.md](OPTIONS.md).

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

`Engine.newGame(premise)` currently wires the World, creates the `Engine` Actor Component and
singleton components, marks initialization complete, and commits the pre-setup baseline. It does
not create a Phase. Terraforming Mars workflow later creates `SetupPhase` as an ordinary effectful
operation.

**Forward-looking:** Kotlin `Engine` remains the passive mechanism that calculates responses to
Actor-attributed mutations. The current administrative Actor and Component become `Admin`.
Bootstrap should create only the state that cannot yet arise from an ordinary Admin task, then hand
control to Admin as soon as possible. Discover that minimum during extraction rather than requiring
an up-front inventory. Do not prolong special initialization merely because the current
`Initializer` can create more directly.

Keep three bootstrap layers distinct:

1. **Structural construction** forms the Class Table, empty state indexes, history, timeline, and
   passive mutation executor. There is not yet an Actor mutation to record.
2. **Actor bootstrap** establishes the minimum concrete state needed for Admin to exist as an Actor
   Component and receive ordinary work. Add other directly created premise state only when the
   ordinary task route proves circular.
3. **Game initialization** begins at the earliest point where history can honestly say that Admin
   is selecting, narrowing, and executing assigned tasks. Singleton creation, Module activation,
   Player creation, and later `SetupPhase` should move into this ordinary phase wherever the model
   can express them without circular prerequisites.

The goal is not to call every constructor step an Admin action. It is to make the special prefix as
short and explicit as possible, then use the ordinary task lifecycle for everything after the
handoff.

In Canon, exact-`This` singleton bootstrapping remains appropriate for premise-selected identities,
selected data families, Class representatives, and generic specialization fanout. Initialization
materializes Modules in an order consistent with active provenance, then Module effects create the
concrete components they own.

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
does not index or fire effects. The engine invokes that operation and reacts to a neutral description
of what changed. Prefer a return value if it preserves the required ordering; if synchronous
notification is necessary, state accepts a generic callback expressed only in state vocabulary.
The callback must not name the engine, effects, tasks, or Instructions.

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
Agent completion. `GameRecording.seek` reverses or reapplies those events on the same live `World`,
and capturing seals its public rollback surface to those positions. This coupling is transitional:
recording navigation should own independent read state so seeking neither mutates nor seals its
source World. Persistence and speculative execution require separate design review.

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
- optional `THEN` continuation group; and
- diagnostic `whyPending`.

A temporary 1-based display position may disambiguate equal-looking tasks. It is not an id.

Semantically there is one World task pool. Actor-specific queues are current filtered API views,
not independent state containers. `Agent.tasks` may present the fiction of one Actor's queue
without promoting that view into the engine model.

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
- `Transformers.classEffects` collects inherited effects for an active Class, inserts defaults,
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

There is no single elaboration entry point today. `ApiTranslation`, `Transformers.classEffects`,
property evaluation, and `CustomClassRuntime` assemble overlapping transformer chains, while
`Instructor` applies marked-syntax handling after custom translation. Their shared intended
contract is one authored-to-elaborated operation, parameterized by the active Class Table, Catalog
handlers, and the contextual bindings available at that stage. Reflection-like re-entry should call
that same operation rather than reconstructing a private subset of the pipeline.

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
components such as `PlantTag<CardFront>`. In the action phase, a `WildTag` creates a distinct
`WildTagUse` holder when its owner chooses a `UseAction<StandardAction>`; choosing `Pass` creates
none. Prelude turns create the holder from `NewTurn` because they do not use the standard-action
signal. The temporary holder offers the owner `Tag<This>?`, so a chosen wild meaning is a real tag
and participates in bare tag metrics and requirements.

The holder distinction is also the trigger distinction. `Tag` has the trigger default
`Tag<CardFront>:`, so an effect that reacts only to printed tags can explicitly accept it with
`PlantTag<>:` or spell out `PlantTag<CardFront>:`. It will not see
`PlantTag<WildTagUse<...>>`; there is no dispatch filter or special change kind. Refinements can
follow the dependency graph when card identity matters. Robotic Workforce uses
`CardFront(HAS BuildingTag OR WildTagUse(HAS BuildingTag))`, which accepts only the card whose
action-scoped wild holder received the Building interpretation.

`WildTagUse` is `MustCleanUp`, so the action cannot finish while it remains. An unchosen offer is
uniquely implied end-of-action settlement: after the action's work finishes, the selected completion
hook removes the acting player's remaining uses and their dependent tags disappear through
dependency cascade. It must finish before workflow offers `SecondAction`. Until that hook exists,
the shared `TfmGameplay` completion bridge cleans up `WildTagUse?` tasks when they are the acting Player's
only remaining work, then removes the uses directly; `TfmGameplay` has an equivalent turn-helper
bridge. Both should disappear when sequencing owns end-of-action settlement.

#### Looking for a better wild-tag mechanism

**Working direction:** this representation is not settled; keep looking for a smaller one.

`WildTagUse` is the only reason the trigger-only `DEFAULT` channel exists. `DEFAULT Tag<CardFront>:`
is the single trigger default authored anywhere, in this Catalog or in `SystemDeclarations`, and it
buys a fourth `DefaultKind`, a fourth `DefaultsDeclaration` field with its merge and rendering arms,
a fourth `Defaults.DefaultSpec`, and `Transformers.insertTriggerDefaults`. A mechanism that supports
one class through a whole default channel is a candidate for replacement, not for extension.

The two facts the design must keep separate are (a) a chosen wild meaning is a real tag, countable
by bare tag metrics and refinements, and (b) an effect that reacts to printed tags must not see it.
Look for a shape that gets (b) from something already in the model rather than from a new default
kind. Candidates worth trying before anything else:

- make the printed/chosen distinction a Class distinction rather than a holder distinction, so
  ordinary nominal subtyping supplies the trigger filter;
- give `WildTag` an occurrence-per-action-slot directly, so no second holder Class is needed; or
- decide that `Tag<CardFront>` should be the ordinary `DEFAULT` for every usage, and let the two
  refinement sites that genuinely want either holder say so explicitly.

Do not settle any of these before checking it against Robotic Workforce and the
multiple-wild-tags-on-one-card entry in [`TODO.md`](../../TODO.md).

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

**Disposition: at peace with the operator set.** `Metric.Max`, `Metric.Subtract`, and `Metric.Or`
each have only a handful of authored uses, almost all inside `Award.metric`, so a sweep for
single-client machinery flags them. The measurement is backwards: the algebra is *under*-built, not
over-built. `Subtract` saturates but there is no `Add`, and
[TURMOIL.md](TURMOIL.md#open-language-and-modeling-questions) needs one for global events that add
Influence after a capped or grouped Metric. Union and sum are also genuinely different operators —
Awards need `Or`'s non-double-counting union, Turmoil needs arithmetic addition — so neither can
stand in for the other. Propose completing this algebra, not trimming it.

Separately, `AssignAwardPlaces` is a `Custom` because "rank owners by a Metric under a declared tie
rule" is inexpressible, and Turmoil's `PartyLeader` and `Dominant` maintenance want the same
primitive with different tie rules. That convergence, not the operator count, is the live design
question here.

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

AST values created inside the engine skip parsing but may use relevant transforms explicitly.
Transform entry points preserve their declared AST `kind`; a cardinality-changing caller must
request `InstructionTree`, not `Instruction`.

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
audited mutation families directly against one task pool; a separate passive access object is not
needed. Actor assignment remains engine semantics. Agent is the sole issuer of ordinary explicit
and policy-chosen mutations for one Actor. Direct engine primitives remain available for workflows,
replay correction, cheats, and tests; preventing trusted callers from using them is not a current
goal. Public task mutation is already limited to checked narrowing and explicit single-task
removal.

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
Effector, Timeline, and other World-level services are shared. Each Actor currently receives its
own `Changer`, `Instructor`, `Implementations`, and `ApiTranslation` scope.

The target engine composition retains only the Actor context required to calculate one direct
mutation. The separate state composition retains no Actor decision context. Actor-filtered reads,
unique long-lived Agents, their policies, and the shared autoexecution loop belong in `:agent`.

Kotlin keeps `Actor` and `Owner` distinct. Current Players are both. A passive Pets Owner such as
`SoloOpponent` has no gameplay scope or task queue.

The Effector receives a GameReader provider to break its construction cycle: it must exist before
the final reader can be assembled, while firing later requires that reader.
