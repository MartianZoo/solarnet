# The live engine

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing construction of a live `World`, component mutation, events, transactions,
> tasks, effect execution, rollback, or the engine-facing part of `Agent`.
>
> **Status:** current-model map. It explains the engine's responsibilities and causal path, not
> every Pets operator or Terraforming Mars rule.

## The model to keep in mind

A live game has three layers with deliberately different responsibilities:

| Layer | Owns |
| --- | --- |
| `:state` | Replayable facts: concrete components, exact pending Tasks, and the event sequence |
| `:engine` | Interpretation: task resolution, limits, effects, attribution, and transactions |
| `:agent` | The normal Actor-scoped client API and optional autoexecution policy |

`GameWorld` is the passive center. It can apply or reverse already-decided events, but it cannot
interpret an Instruction or calculate a consequence. Engine services surround one `GameWorld` and
derive the events to apply. One stable `ActorEngine` per Actor supplies the policy-free mutation
boundary. `Agent` is the ordinary application-facing wrapper around that boundary.

This separation makes a recording replayable without running the engine again: playback reapplies
the recorded facts instead of rediscovering effects.

Within a `World`, the component graph is the present, the event log is the past, and the task pool
is unresolved work. `GameReader` supplies higher-level queries over the same state. The
`ClassTable` and premise are immutable for the life of the World.

Narrower notes own adjacent subjects:

- [SEQUENCING.md](SEQUENCING.md) owns ordering, `THEN`, barriers, and completion scopes.
- [IDENTITY.md](IDENTITY.md) owns assignee, Actor, Owner, Anyone, Admin, and attribution roles.
- [QUANTIFIERS.md](QUANTIFIERS.md) owns instruction counts and limit behavior.
- [type-system-spec.md](../../pets/docs/type-system-spec.md) and
  [pets-language-spec.md](../../pets/docs/pets-language-spec.md) own static Types and authored Pets semantics.

For proposed changes to runtime layering, public APIs, policy, or game flow, use the Solarnet roadmap
for direction and inspect current source and tests for the implemented contract.

## From a premise to a ready World

Catalog resolution supplies an immutable `GamePremise` and its compiled class facts. See
[Catalogs and game premises](../../pets/docs/agents/CATALOG.md) for configuration, selection,
and effect compilation before a World exists. Worlds share those facts, but no mutable state.

`Engine.newGame` first validates its supplied Kotlin custom-Class implementations against the
Catalog's derived custom declarations. Games without custom declarations may use the empty default;
game-specific entry points such as `TfmEngine.newGame` supply their complete set. The engine then
wires one `GameWorld` carrying those bindings to its reader, timeline, task services, effect index,
limiter, instructor, changer, and Actor Engines. Initialization crosses three conceptual stages:

1. Structural construction installs the included concrete `Class<T>` representatives and runtime
   indexes. This is not an Actor mutation and creates no Change Events.
2. The initializer creates `Admin`, the minimum concrete Actor state needed to receive
   ordinary work.
3. Admin Tasks create `BootstrapPhase` and the premise component. Ordinary effects of the premise,
   Modules, and Players create the remaining selected state.

Bootstrap drains choice-free Tasks through their assigned Actor Engines. An abstract instruction is
acceptable only when normal resolution leaves exactly one legal concrete result; initialization
must not silently choose among alternatives. Successful initialization requires no pending Tasks,
all exact premise components, and all applicable positive lower bounds, then commits the timeline.
Before a ready World or fork is returned, the engine also verifies every positive signed premise
selection representing an inhabited concrete Class has a live `Class<T>` component, and every
negative selection lacks one.

Bootstrap does not change ordinary Pets meaning to force progress: it must not turn queued `:`
effects into automatic `::` effects or discard a change's `?`, `*`, or `!` quantifier. Starting
state that requires a Player choice remains exact premise state which opens that choice during
ordinary setup; bootstrap does not make it on the Player's behalf.

An automatic effect narrows an abstract gain when live component limits eliminate every candidate
except one. If several legal concrete targets remain, the gain stays abstract. User-selected tasks
do not use capacity as an implicit choice.

`Engine.fork` constructs an independently mutable live World only at a completed gameplay
position. It shares the immutable premise and Class Table, copies the passive component,
dependency, task, event, completed-position, and live-effect-index collections, and builds fresh
engine services around them. Immutable compiled live effects are shared, while index storage is
independent and retains source registration order. The fork point is the new timeline commit floor,
so the fork cannot roll back into source history. Application callbacks, component listeners,
transactions, and Agents remain fresh.

The generated premise is executable output of configuration resolution. Live initialization does
not reconsider Module defaults or assemble a second representation of the premise. Required state
should arise from its earliest honest owner: premise construction supplies exact configuration,
while Module, map, mode, and Player effects supply the state whose meaning they own.

## Concrete state and its history

The component graph is a multiset of concrete `Type` values. Components have no fields or stable
instance identity; equal Types are indistinguishable copies.

Dependencies are part of a Type. A concrete dependency must identify at most one live target.
Removing the final target cascades through existing dependents before the requested removal is
retried. The graph owns the reverse-dependency index, while the engine owns the decision to cascade.

Every live mutation is an exact gain, removal, or transmutation. A transmutation removes before it
gains. A transmutation must change its concrete Type. A Signal gain without transmutation uses a paired
gain/removal event without requiring an existing source; that event represents a signal, not a self-transmutation.
`CustomMetric` types never become components. `CustomInstruction` classes are ordinary Signals:
their gains are recorded, their Pets effects run, and Kotlin supplies one additional queued effect.

The event log contains:

- `ChangeEvent`, including Actor, concrete change, and optional causal event/context;
- `TaskAddedEvent`;
- `TaskRemovedEvent`; and
- `TaskEditedEvent`.

`GameWorld.apply` is the sole forward event/projection boundary. `GameWorld.rollBackTo` reverses the
same projections while removing their events. Engine-side derived indexes, notably live effects,
must observe both directions rather than owning another copy of game state.

Ordinary game mechanics react to the current World and the event being processed, not by querying
arbitrary event history. Event ordinals and Task ids are causal and diagnostic identities, not game
resources or presentation order.

`Timeline` owns the positions between caller-facing Player Agent methods, its retained future when
seeking backward, failure-atomic blocks, and a commit floor. Public seeking accepts only completed
positions at or after that floor; stepping moves between the same positions. Seeking preserves later
events and restores components, Tasks, and derived indexes in either direction. Gameplay cannot
mutate the World while a future is retained; `truncate()` deliberately discards it before new play.
A Player Agent method records a position after its work finishes, including an `OperationScope`
method inside an enclosing client block. Admin calls do not automatically create positions. The
enclosing block remains failure-atomic; it does not hide positions between its nested Player calls.
Seeking inside an atomic call cannot cross that call's start. An atomic failure instead discards
that interaction's events and restores its starting state.
`Exceptions.AbortTransactionException` requests that restoration without surfacing as a caller
failure. The commit floor prevents navigation across initialization or a fork point. Gameplay
commits nothing, so seeking may cross turn and phase changes.

These commits are local timeline mechanics, not acceptance of a shared game's Git history. They
do not grant or deny a takeback between players. [ADVERSARIAL.md](ADVERSARIAL.md) places that
agreement outside the engine; reconstructing an agreed earlier position is a separate operation
from rolling back an individual local transaction.

Transaction atomicity is not a game rule saying intermediate changes are invisible. Effects can
observe the individual changes that compose a successful operation.

## Tasks are choices, not a program counter

There is one semantic Task pool. Actor-specific queues are filtered views. Iteration is stable for
reproducibility, but its order carries no rule meaning.

A Task retains:

- a `TaskId` derived from its original add-event ordinal and preserved across edits;
- one task-shaped `Instruction`;
- its current assignee;
- whether it is selected;
- an optional `THEN` continuation; and
- its cause.

An unselected task normally starts assigned to the Actor that admitted it. A normalized task
containing an elementary gain of a `System` Component, alone or under top-level instruction-side
`BY`, instead starts assigned to Admin. Contextual selection reapplies that rule before a concrete
`BY` performs its later handoff. If that Admin assignment interrupts a `THEN` sequence, the engine
encodes the previous assignee in a `TASK Actor { body }` continuation. Admin executes the System
step, then dispatches the remaining work back to that Actor. An authored `TASK Actor { body }`
likewise admits unresolved body instructions for its named Actor as soon as the work holding it is
ordinary; a dispatch is never a task of its own.
See [IDENTITY.md](IDENTITY.md).

Queue admission normalizes an `InstructionGroup` into one Task per independent member. `A THEN B`
stores A as current work and B as a continuation; completing A admits B as ordinary work with no
priority over unrelated Tasks. If the first stage uses an open shared variable, normalization
defers its split until narrowing selects that value. Unrelated earlier stages can split, retaining
the shared scope in their continuation.

The current assignee advances work by selecting a Task, narrowing its remaining choices, or
executing it once concrete. Selection blocks competing ordinary task commands until that Task
finishes. It is an ordering promise, not a timeline commit or a lock over the task's entire
consequence set. Correction APIs remain deliberately available through their own contracts.

The current engine admits committed narrowing of a selected Task. It resolves that proposal against
the live World, and each accepted partial choice becomes a `TaskEditedEvent`. It also validates a
proposed narrowing without changing an unselected or selected Task, and can commit a narrowing by
TaskId, selecting that Task if necessary. `Agent.narrowTask` still requires an already selected Task.
`Agent.selectTask(instruction)` finds a unique compatible task and commits the supplied narrowing
while selecting it. An exact instruction takes priority over other compatible tasks. If selecting
the task alone transfers it to another Actor, a submitted choice fails instead of crossing the
handoff. Clients with a task id can pass a copy of that `Task` to `selectTask`; `Task.bindXTo(4)`
provides such a copy with its linked `X` choice set to 4.
If contextual selection splits the original task before its supplied narrowing can be applied,
the request fails without changing the World. Select the original task first, then narrow the
resulting tasks; neither `selectTask` nor a form commit may silently discard that choice.

An Agent-created `TaskForm` keeps a disposable narrowing draft before or after task selection.
Its choice analysis makes no game mutation and claims no selection or assignment. The form
rechecks the current task and World before submission; the engine's stored instruction remains
authoritative. If committing an unchanged form selects a task that changes assignee, the command
performs only that handoff. A form containing a choice instead fails, because the former assignee's
draft cannot cross it.

`TaskForm.options` lists all included concrete subclasses directly, then chooses remaining
dependencies separately. It filters invalid narrowings, concrete unmet gates, and mandatory
concrete changes without enough source quantity or required dependencies. It examines only the
current stage: unresolved choices, automatic effects, and later `THEN` stages are not searched.
Optional zero choices and AMAP no-ops remain valid. Known hand identities belong to the client;
the playground additionally filters card-playing choices using `CardTracking`.

**Intended client API rule:** No game choice or task operation may require `TaskForm`. Clients may
use it to query options and prepare a choice, but must also be able to make every known choice and
complete every task through `Agent` commands. If a replay must construct a form or enumerate
its decisions merely to submit a known choice, that exposes an `Agent` API gap. A
Terraforming Mars-specific helper that hides the form does not close the gap.

After selection, resolution repeatedly evaluates state-dependent structure until it reaches a
remaining choice or the first stage is executable. It handles metrics and gates, eliminates `OR`
arms whose execution prerequisites fail, narrows an abstract gain when present dependencies
identify one target, resolves quantities, and translates concrete custom instructions. Mandatory
resolution checks physical availability and dependencies; AMAP and optional quantities also use
current count bounds. This may settle a location as well as a class, or an existing dependency
whose declared maximum is one. The same World-based gain narrowing applies when a selected Task
receives an explicit narrowing. `ActorEngine` separately executes a selected Task once its
instruction is concrete.

Task admission discards literal zero changes. Counts calculated while selecting an existing task
retain their Types through matching and execute without component events.
Typed-zero submissions cannot identify work solely through an `Ok` intersection, including an
unrelated alternative at the start of a sequence.

Resolution may leave a genuine Player choice abstract. The task's current assignee alone may narrow
it. If resolution exposes independent instructions, the selected structural Task completes and
admits ordinary unselected children without transferring its selection to a child.

Engine task execution leaves a handed-off task selected for its receiving Actor. `Agent.doTask`
runs configured autoexecution and requires that selected task to finish; otherwise it rolls back
the call. `selectTask` and `narrowTask` can instead leave work pending for the receiving Actor.
`doTask` can identify a gated task by its body, and a `/` task by its calculated action, without
repeating the gate or metric in the submitted instruction. The task still checks those conditions.
When matching a task by its body, a failed gate does not compete with available matches. If no
available task matches, the failed gate can identify the task so execution reports the unmet
requirement.

When a top-level instruction-side `BY` becomes concrete, task processing resolves the named
participating Actor, removes the wrapper, and transfers the task to that Actor. The prior assignee's
command ends at that handoff. The final assignee's `ActorEngine` executes the stripped instruction,
so the Actor recorded on each resulting change is the Actor that actually performed it.

Task normalization distributes `BY` across `THEN` stages when they become separate tasks. A `BY`
inside a sequence that must remain a single Task is rejected; the engine has no partial-execution
state for changing assignee midway through such a Task, and Canon has no use requiring it.

The finishing task's assignee receives structural children and continuations. A System handoff
encodes the former assignee in its continuation, so Admin executes the System step and returns later
work to that Actor. Authored `TASK` dispatch can also give work to another Actor. Queued reactions
use ownership and event attribution as described in [IDENTITY.md](IDENTITY.md). Deferral by `LATER`
and `FINALLY` is the only task priority ([SEQUENCING.md](SEQUENCING.md#deferred-work)), and there
is no persistent delegated
operation in current task data. The remaining routing and completion questions are in
[SEQUENCING.md](SEQUENCING.md#the-missing-rule-when-an-operation-is-over). Any game-rule restriction
on which task may execute must apply equally to explicit commands and autoexecution. Protecting a
player from somebody else calling their Agent is instead an external submission concern covered
by [ADVERSARIAL.md](ADVERSARIAL.md).

An owned context supplies lexical `Me` to a bare nested `CityTile`; a Player-submitted narrowing
uses that Player for omitted ownership. `Cathedral<CityTile<Anyone, Tharsis_4_2>>` explicitly leaves the city owner open. A
submitted narrowing can then resolve the existing city at that area without naming its concrete
subclass, and fails when there is no city there.

The executable first-stage forms are deliberately small: no-op, zero-count change (or alternatives
consisting entirely of zero changes and no-ops), fully concrete change, or `THEN`
whose first stage is executable. Task processing consumes instruction-side `BY` before this point.
Later `THEN` stages stay as Pets until earlier state changes have happened. Execution removes the
completed Task, admits its continuation, and routes changes through the ordinary mutation/effect
path. `Gated`, `Per`, alternatives with remaining choices, gains of virtual `CustomMetric` types,
marked transforms, and instruction groups cannot reach execution as a first stage; resolution must
consume, reject, choose, or split them first.

## One instruction becomes events in stages

[Pets elaboration and specialization](../../pets/docs/agents/ELABORATION.md) supply a rule-shaped
AST with defaults and context applied, retaining intentional choices. Resolution then reads the
selection-locked World and produces the small executable algebra described above.

String input submitted through a Player Agent resolves names, atomizes, supplies dependency
defaults, inserts that Player for omitted `Owned` owners, and applies Catalog-registered syntax transforms.
It may remain abstract because a Task can intentionally ask for a later choice.

## Effects are derived behavior

The engine compiles each active component's elaborated effects into live subscriptions and indexes
them with the component's multiplicity. Trigger matching supplies event-linked Type values, counts,
and any Actor match before producing an instruction.

Self triggers respond to the changed copies of their exact effect-bearing Type; pre-existing equal
copies do not multiply the response. Other subscriptions multiply by the number of live
effect-bearing components. Normal triggers scale with the matching change count, while `X` means
one response to any positive count.

After effect scaling, `Effector` reapplies gain atomization so `Atomized` gains remain separate
even when their counts come from trigger matching or repeated live components.

An owned effect listening to an unowned event defaults to its Player owner unless the event is
`System` or the effect explicitly says `BY Anyone`. Unowned System events are necessarily performed
by Admin and are observed table-wide; Player-specific System events carry an owner. Trigger-side
`BY` filters the triggering Actor. On queued work, instruction-side `BY`
changes the task's assignee once its instruction is concrete. It does not rewrite event
attribution.

Queued `:` effects produce pending Tasks. Automatic `::` effects execute recursively before queued
effects from the same change are admitted. The recursion limit is eight nested automatic effects;
exceeding it raises the shared Pets `ResourceLimitException` and rolls back the enclosing operation.
Parser and class-loading resource limits use the same exception. Task selection and execution
probes propagate it rather than reporting an unavailable choice. Because automatic effects create
no Task, their instructions cannot use instruction-side `BY`. Read [SEQUENCING.md](SEQUENCING.md)
before using `:` versus `::` to force an order.

### Wild tags

The [Pets card model](../../pets/docs/agents/CARD_MODEL.md#wild-tags) describes the printed
wild, assignment holders, authored benefits, and player choices.

Requirement allocation is separate from benefit inference. `PlayCard` and `Milestone` first emit
`AllocateRequiredWildTags`, then run their existing requirement checks. Each unused wild election asks
`RequiredWildTag` which concrete tag to gain. That metric selects actual assignments rather than
inflating a tag count. It covers the current direct tag minima and conjunctions, sorting available
holders by expression and filling the printed requirements in source order. It recalculates the
remaining shortfalls after each assignment, so listener order does not choose the resulting
allocation. Ecologist's `BioTag` requirement uses Plant by the owner's convention.

Cloud Tourism's custom instruction queues concrete Earth/Venus gains that maximize the smaller
count, including its own printed Venus tag. It preserves existing assignments and leaves a spare
wild unused when assigning it would not increase production.

**Owner direction:** do not force players to act in their own interest. In particular, taking
Interplanetary Trade's benefit before allocating is allowed; it simply produces less. No guard
forces the beneficial task order.

See `WildTagHolderTest`, `WildTagTest`, and `WildTagContinuationTest` for allocation, ownership,
cleanup, and reuse scenarios. Replays use Canon cards and actual allocation choices without
wild-tag corrections. `CONCRETE` lets replays allocate before a competing payout; `EAGER` may
choose the payout first. Admin removes the action scope even when the player uses `NONE`;
dependency cleanup removes its elections and assigned tags.

Represented-tag binding limitations are documented under
[Pets elaboration](../../pets/docs/agents/ELABORATION.md#represented-tag-bindings).

## Queries, invariants, and dead ends

**Owner direction (2026-10-14): decision-tree pruning is the player's agent or client's
responsibility, never an engine or model obligation.** Universally assume either that the caller
diligently presents only choices known not to dead-end the turn, or that exposing dead ends does
not matter. This assumption does not depend on a pruner existing today. Task availability,
selection, narrowing, and successful intermediate commands do not promise a successful turn.

Protected Habitats is the concrete example. Its
[`-Plant OR -Animal OR -Microbe BY Player(NOT Me@): Die`](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/corpera/cards.json5)
queues a consequence of an opponent's removal. The attempted removal may happen before that
consequence prevents completion; the caller must roll back the unsuccessful path, potentially
the turn. The engine need not anticipate that consequence or hide the protected target. This is
valid modeling, not an issue to fix. The existing
[ProtectedHabitatsTest](../../test/common/dev/martianzoo/tfm/tests/cards/ProtectedHabitatsTest.kt)
expects failed attacks and retained protected resources.

`GameReader` evaluates component counts, requirements, refinements, and custom metrics against the
current graph. A metric union is a multiset union: for each concrete Type it retains the greatest
matching multiplicity rather than double-counting overlapping arms. Custom metrics over abstract
dependencies normally specialize only through live dependency targets, not the full structural
cross-product.
Resolution's component-existence checks use `Limiter` to query the graph, stopping at the
first match without constructing a result collection. Refinements still use the current World.

The game `ClassTable` view compiles inherited invariants into immutable per-Class limits. Each
World's `Limiter` combines those facts with live multiplicity. An invariant constrains the state at
operation completion. On an ordinary gain, a positive exact-count invariant also constructs its
missing parts when the required Type is concrete and directly depends on the gained component.
Inherited requirements bind `This` to that component. Construction keeps pending requirements and
gains a part only once its dependencies exist, discovering that part's requirements in turn. This
allows a nested part to depend on another branch of the same structure regardless of declaration
order. If no pending part can proceed, normal gain resolution reports the missing dependencies.
The whole required structure exists before any of its gain events dispatches
reactions, so a required listener can observe a sibling's gain regardless of invariant order.
Construction records ordinary change events, including separate `Atomized` gains. Reactions see
the assembled structure. Automatic reactions traverse construction events in reverse, so a part
reacts before its owners can dispose of it. All construction events' automatic effects finish before
matching queued effects in their original event order. Both automatic and queued owner conditions
can therefore see their parts' automatic consequences. Existing parts count toward the requirement,
including parts supplied by overlapping requirements. Construction retains each gain's automatic
ancestry to share the existing recursion guard; waiting for a dependency does not reset that budget.
Failures roll back the whole operation.
`ClassLimitTable.requiredParts` supplies these construction requirements from the same compiled
invariant templates used for validation.

Abstract or refined Types, minimum-only bounds, self-counts, and indirect or unrelated requirements
remain constraints; construction neither chooses their components nor invents missing prerequisites.
Signals do not reconstruct parts. Ordinary instruction execution and
`sneak` corrections share this construction rule; corrections suppress queued reactions.

Completed-state validation instantiates a self-count for every inhabited concrete specialization.
A dependent count containing `This` is instantiated only for live declaring Types, so an absent
owner does not itself require dependent state.

For component-count validation, a full operation is one initiating change and all its recursive
automatic effects. Intermediate counts may cross declared bounds; the completed operation must
satisfy them. Source availability and dependency integrity are checked at execution. Resolution never
executes consequences to predict a choice's outcome; failure at completion uses the existing
transaction rollback path. [QUANTIFIERS.md](QUANTIFIERS.md#invariants-at-operation-completion) owns
the bounds and choice-resolution contract.
Initialization still audits all applicable positive minimums after constructing the initial world.
Ordinary gameplay selects global minimum checks through the changed components' existing per-Class
limit index. It still checks required dependents of live affected owners, including absent parts;
unchanged global counts need no recheck. Initialization and corrections retain the full
invariant query.
Each World's `Limiter` reuses the immutable declared limits bound to a component type. Live counts
and the required dependents of affected owners are still checked against current state.

Prefer atomic transmutation when two faces share a stable invariant. Separate queued tasks and
`THEN` stages outside the initiating automatic cascade cannot repair a completed operation: lifecycle
state that may legitimately remain absent between operations still needs maximum one rather than
exact one. Completion spanning queued work remains a separate
[sequencing discussion](SEQUENCING.md#the-missing-rule-when-an-operation-is-over).

A locally legal choice can discover later that its remaining work cannot complete. This is a
recoverable dead end: `DeadEndException` rolls the encompassing transaction back so another branch
can be tried. A broad choice is not a correctness bug merely because a branch fails late; it is a
bug if a prohibited result survives successful turn completion. An offered choice need not contain a successful branch; a client
can roll back farther when the whole choice leads to dead ends.

Failure atomicity covers the encompassing transaction; it does not promise that a later failed
command automatically undoes every earlier successful command in the turn. A caller exploring
across commands must roll back far enough to abandon the unsuccessful path. Neither that need
nor the absence of lookahead is an engine/model defect. Report a separate rule failure, such as
a prohibited result surviving successful turn completion or a required legal outcome being lost.

Whether that local calculation can be accepted as shared play, and what players do when already
accepted work cannot finish, are external questions in [ADVERSARIAL.md](ADVERSARIAL.md). The engine
does not need to prove every future continuation succeeds before exposing a task, or conceal
local rollback to make a result count as an achievement.

The engine can leave a mandatory choice available even when its count invariants will fail.
Client-facing exploration and pruning belong to the player's agent or client. Engine resolution
uses current source and dependency prerequisites, authored gates, and AMAP/optional capacity,
without predicting repairs or pruning mandatory choices by count bounds.

### The metric operators are intentional

`Metric.Max`, saturating `Metric.Subtract`, and non-double-counting `Metric.Or` have few authored
uses, but they express different operations and are not redundant special cases. In particular,
union cannot substitute for arithmetic addition. Treat the small number of clients as evidence that
the metric algebra is still sparse, not as a reason to collapse these operators. `Metric.Rank`
likewise replaces game-specific ranking instructions with reusable comparison semantics.
Ranking scores the candidate once, skips comparing it with itself, and evaluates each peer's
later tiebreakers only while earlier scores remain tied. Scores are local to that rank query;
they are not retained across state changes or rollback.

### Content must not compensate for an engine gap

Do not represent one compact rule with parallel marker Classes, a watcher for every resource, or
duplicated Effect branches merely because a general Type or trigger operation is missing. Repair the
general mechanism or leave marginal content unsupported with an explicit blocker.

The promo attack model is the reference case: one generic victim-owned watcher records removed
resources and decreased production while retaining the attacked Type and Actor. Crash Site Cleanup
and Mons Insurance consume those records; prevention rules remain removal triggers. Do not
replace that shape with an `AttackKind` hierarchy and per-resource watchers.

## Actor Engines and Agents

Each World retains one policy-free `ActorEngine` per Actor. It owns attributed task and state
commands but shares the World's changer, instructor, timeline, and Task services. `Agents(world)`
constructs the corresponding normal client objects and prevents pairing an Agent with an unrelated
World. Its optional `initialPolicy` function supplies each Actor's policy at construction without
executing pending work; callers need not assign the policy afterward.

For Actor attribution, receiving the request through that Actor's Agent is sufficient. The engine
checks the task's current assignee and legal mutation, not the identity or consent of the person
or program making the call. A client may use all of the World's Agents, including Admin.

All ordinary Agent mutations use the shared outer transaction-completion path. `runOperation`
admits new work, lets the body finish it, runs configured autoexecution, preserves unrelated
pre-existing unselected Tasks, and rejects newly unfinished Tasks or `MustCleanUp` state. It cannot
start while a pre-existing selected Task holds the World lock. `sneak` applies a fully concrete
correction group, constructs exact required parts, removes dependents, and audits every applicable
count invariant. The entire group rolls back on failure. It runs automatic effects and suppresses queued effects recursively
while bypassing settlement and completion callbacks. `exMachina` rejects explicit
System and MustCleanUp targets and any correction that leaves new MustCleanUp state.
It additionally restores and resumes an existing selection within one atomic lifecycle. See
[EX_MACHINA.md](EX_MACHINA.md) for the contract and the remaining canon consistency gaps.

A World may install one `AgentTransactionGuard` before Agent commands begin. Every Agent mutation
then validates the guard before its transaction can commit. An automatic candidate that needs
unsupplied client information rolls back locally and stays pending; an explicit Agent request fails.
The Terraforming Mars hand-tracking use is described in [CARD_HANDLING.md](CARD_HANDLING.md#card-hand-tracking).

Current autoexecution lives in `:agent`, not in the core engine. Policy selects legal Task commands;
it does not alter their semantics. Before probing tasks, the loop checks whether any pending
candidate has an effective policy other than `NONE`. If none does, it returns without probes or
execution, even when an Actor with no pending work has autoexecution enabled. When automatic work
is available, manual tasks still compete with it. The shared policy loop tries eligible Admin work
before Player work, including between explicit Player steps, but continues to an executable Player option when
Admin work is abstract or temporarily unavailable. It cannot interrupt an authored `::` chain or
supply a missing scheduling rule. Engine primitives remain available to any caller for local
exploration, replay correction, tests, and cheats. Restricting access to those primitives
is not an engine responsibility. [ADVERSARIAL.md](ADVERSARIAL.md) describes how external participants
or software decide which proposed results and supplied facts to accept as shared play.

## Where to inspect

Use these as starting points, then follow the concrete collaborators and tests for the behavior at
issue:

- [`Engine.kt`](../../src/common/dev/martianzoo/engine/Engine.kt) and
  [`Initializer.kt`](../../src/common/dev/martianzoo/engine/Initializer.kt): construction and
  bootstrap.
- [`World.kt`](../../src/common/dev/martianzoo/engine/World.kt) and
  [`GameWorld.kt`](../../src/common/dev/martianzoo/state/GameWorld.kt): live facade versus passive
  replayable state.
- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt),
  [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt), and
  [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt): task lifecycle,
  resolution, and execution.
- [`Changer.kt`](../../src/common/dev/martianzoo/engine/Changer.kt),
  [`Effector.kt`](../../src/common/dev/martianzoo/engine/Effector.kt), and
  [`LiveEffect.kt`](../../src/common/dev/martianzoo/engine/LiveEffect.kt): component changes and
  consequences.
- [`Timeline.kt`](../../src/common/dev/martianzoo/engine/Timeline.kt) and
  [`GameEvent.kt`](../../src/common/dev/martianzoo/state/GameEvent.kt): atomic history.
- [`PetElaborator.kt`](../../pets/src/common/dev/martianzoo/pets/PetElaborator.kt): shared input and
  effect transformation.
- [`Agent.kt`](../../src/common/dev/martianzoo/agent/Agent.kt) and
  [`AgentImpl.kt`](../../src/common/dev/martianzoo/agent/AgentImpl.kt): current client surface and
  transaction completion.
