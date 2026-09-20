# The live engine

> **NOTE:** This document is maintained for agents. Source and meaningful tests remain
> authoritative.
>
> **Read when:** changing construction of a live `World`, component mutation, events, transactions,
> tasks, effect execution, rollback, or the engine-facing part of `Agent`.
>
> **Status:** current-model map. It explains the engine's durable boundaries and causal path, not
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
is unfinished future work. `GameReader` supplies higher-level queries over the same state. The
`ClassTable` and premise are immutable for the life of the World.

Narrower documents own adjacent subjects:

- [GAMEWORLD.md](GAMEWORLD.md) owns passive state, recordings, and the selected extraction boundary.
- [SEQUENCING.md](SEQUENCING.md) owns ordering, `THEN`, barriers, and completion scopes.
- [IDENTITY.md](IDENTITY.md) owns controller, assignee, Actor, Owner, Admin, and attribution roles.
- [QUANTIFIERS.md](QUANTIFIERS.md) owns instruction counts and limit behavior.
- [type-system-spec.md](../type-system-spec.md) and
  [pets-language-spec.md](../pets-language-spec.md) own static Types and authored Pets semantics;
  [life-of-an-effect.md](../life-of-an-effect.md) follows the transformation pipeline in detail.
- [RESPONSIBILITIES.md](RESPONSIBILITIES.md#selected-runtime-dependency-direction) owns the selected
  dependency direction among the runtime layers.
- [AUTOEXEC.md](AUTOEXEC.md), [API.md](API.md), and [WORKFLOW.md](WORKFLOW.md) own their selected
  directions; do not mistake them for current engine behavior.

## From a premise to a ready World

`GameConfig` is unresolved intent. Catalog resolution produces an immutable `GamePremise`: the
selected Catalog and Modules, seated Player Classes, class selections, and exact initial component
Types. The premise retains one immutable game `ClassTable` view sharing its Catalog's compiled
master structure; separate Worlds from that premise share compiled class facts but no mutable game
state. See
[OPTIONS.md](OPTIONS.md).

`Engine.newGame` wires one `GameWorld` to its reader, timeline, task services, effect index,
limiter, instructor, changer, and Actor Engines. Initialization then crosses three conceptual
boundaries:

1. Structural construction installs the included concrete `Class<T>` representatives and runtime
   indexes. This is not an Actor mutation and creates no Change Events.
2. The initializer directly creates `Admin`, the minimum concrete Actor state needed to receive
   ordinary work.
3. Admin Tasks create `BootstrapPhase` and the premise component. Ordinary effects of the premise,
   Modules, and Players create the remaining selected state.

Bootstrap drains choice-free Tasks through their assigned Actor Engines. An abstract instruction is
acceptable only when normal resolution leaves exactly one legal concrete result; initialization
must not silently choose among alternatives. Successful initialization requires no pending Tasks,
all exact premise components, and all applicable positive lower bounds, then commits the timeline.

Bootstrap does not change ordinary Pets meaning to force progress: it must not turn queued `:`
effects into automatic `::` effects or discard a change's `?`, `.`, or `!` quantifier. Starting
state that requires a Player choice remains exact premise state which opens that choice during
ordinary setup; bootstrap does not make it on the Player's behalf.

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
gains. A direct Signal is represented as a self-transmutation so both trigger directions can observe
it without changing resting multiplicity. `Custom` Classes never become components: their Kotlin
implementations calculate metrics or translate instructions.

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

`Timeline` adds checkpoints, failure-atomic blocks, rollback, and a commit floor. An atomic failure
restores components, Tasks, events, and derived indexes together.
`Exceptions.AbortTransactionException` requests that restoration without surfacing as a caller
failure. The commit floor prevents rollback across initialization or a committed workflow boundary.

Transaction atomicity is not a game rule saying intermediate changes are invisible. Effects can
observe the individual changes that compose a successful operation.

## Tasks are choices, not a program counter

There is one semantic Task pool. Actor-specific queues are filtered views. Iteration is stable for
reproducibility, but its order carries no rule meaning.

A Task retains:

- a `TaskId` derived from its original add-event ordinal and preserved across edits;
- one task-shaped `Instruction`;
- the controlling Actor for the surrounding operation;
- the contextual Actor that normally performs its eventual change;
- `UNSELECTED`, `SELECTED`, or `DELEGATED` state;
- an optional `THEN` continuation; and
- its cause.

The assignee is derived from selection state: controller before delegation, contextual Actor after
delegation. These identities are independent; use [IDENTITY.md](IDENTITY.md) before changing them.

Queue admission normalizes an `InstructionGroup` into one Task per independent member. `A THEN B`
stores A as current work and B as a continuation; completing A admits B as ordinary work with no
priority over unrelated Tasks. If an open linked variable prevents safe splitting, normalization can
defer the split until narrowing binds it.

An Actor advances work by selecting a Task or narrowing one of its remaining choices. Selection
locks the entire World against competing mutation until that Task finishes. It is an ordering
promise, not a timeline commit.

Unselected Task narrowing is intentionally weaker: it may discard options only by compositional,
immutable class facts and does not execute. Selected narrowing is state-aware. Each accepted
choice is stored by a `TaskEditedEvent`, so a client need not maintain a parallel record of partial
choices.

Choice enumeration should use the same compositional granularity: expose useful narrowings for one
remaining sub-Specification, record that choice, resolve again, and only then expose the next. Do
not make a client choose from a Cartesian product of fully concrete Instructions when their parts
can be narrowed independently.

After selection, resolution repeatedly evaluates state-dependent structure until the first stage is
executable. It handles metrics and gates, eliminates impossible `OR` arms, specializes abstract
Types when unique, resolves quantifiers and limits, and translates concrete custom instructions.
Resolution may leave a genuine Player choice abstract. If it exposes independent instructions, the
selected structural Task is replaced by ordinary unselected siblings rather than transferring its
selection to one arbitrarily.

The executable first-stage forms are deliberately small: no-op, fully concrete change, an Actor
override around executable work, or `THEN` whose first stage is executable. Later `THEN` stages stay
as Pets until earlier state changes have happened. Execution removes the completed Task, admits its
continuation, and routes changes through the ordinary mutation/effect path. `Gated`, `Per`, `Or`,
gains of `Custom` Classes, marked transforms, and instruction groups cannot reach execution as a
first stage; resolution must consume, translate, choose, or split them first.

## One instruction becomes events in stages

Pets moves toward executable data without pretending every stage has a separate Kotlin type:

1. **Authored Pets** retains the rule-shaped AST, including omitted defaults, marked syntax, and
   contextual names.
2. **Elaborated Pets** applies defaults, action lowering, atomization, and registered marked-syntax
   transformations while preserving linked variable identity.
3. **Context-closed Pets** binds the Class, component, trigger, and Player-owned values supplied by
   the surrounding rule. It can still contain intentional choices.
4. **Resolved first-stage work** reads the selection-locked World and produces the small executable
   algebra described above.

`PetElaborator` owns the shared authored-to-elaborated transformations. Class effect compilation,
session input, stored metric expansion, and custom-instruction output must reenter through the
appropriate elaborator entry point rather than reproducing part of the pipeline.

Loaded declarations deliberately retain authored effects and actions separately from executable
effects. Consumers may inspect authored syntax as metadata or resubmit it as rule data, but
resubmitted data must traverse normal elaboration again. Do not treat an already specialized effect
as the declaration's source form.

`This` is contextual declaration syntax, not a free-standing runtime Type. By component-effect
compilation, ordinary `This` occurrences have become the exact effect-bearing Type. Contextual
`Owner` should likewise be bound when a scope supplies one; an intentionally open Owner choice is a
different meaning and must not arise accidentally from a missed binding.

String input submitted through a Player Agent resolves names, atomizes, supplies dependency
defaults, binds that Player as contextual Owner, and applies Catalog-registered syntax transforms.
It may remain abstract because a Task can intentionally ask for a later choice.

## Effects are derived behavior

The engine compiles each active component's elaborated effects into live subscriptions and indexes
them with the component's multiplicity. Trigger matching supplies event-linked Type values, counts,
and any Actor match before producing an instruction.

Self triggers respond to the changed copies of their exact effect-bearing Type; pre-existing equal
copies do not multiply the response. Other subscriptions multiply by the number of live
effect-bearing components. Normal triggers scale with the matching change count, while `X` means
one response to any positive count.

An owned effect listening to an unowned event defaults to its Owner unless it explicitly says
`BY Anyone`. Trigger-side `BY` filters the triggering Actor. Instruction-side `BY` changes the Actor
recorded on resulting work.

Queued `:` effects produce pending Tasks. Automatic `::` effects execute recursively before queued
effects from the same change are admitted. The recursion limit is eight nested automatic effects;
exceeding it fails the enclosing operation atomically. Read [SEQUENCING.md](SEQUENCING.md) before
using `:` versus `::` to force an order.

## Queries, invariants, and dead ends

`GameReader` evaluates component counts, requirements, refinements, and custom metrics against the
current graph. A metric union is a multiset union: for each concrete Type it retains the greatest
matching multiplicity rather than double-counting overlapping arms. Custom metrics over abstract
dependencies normally specialize only through live dependency targets, not the full structural
cross-product.

The game `ClassTable` view compiles inherited invariants into immutable per-Class limits. Each
World's `Limiter` combines those facts with live multiplicity. An invariant constrains legal state
but does not create its required components or choose concrete Types.

Completed-state validation instantiates a self-count for every inhabited concrete specialization.
A dependent count containing `This` is instantiated only for live declaring Types, so an absent
owner does not itself require dependent state.

Upper bounds are checked on gains and lower bounds on removals. Initialization verifies applicable
positive lower bounds, but an exact-one declaration alone does not prove that every intermediate or
resting state is repaired. Prefer atomic transmutation when two faces share a stable invariant, and
do not invent a second marker representation merely to bridge a transient absence. Until scoped
completion can validate repaired state, use maximum one for lifecycle state where zero is a valid
in-progress condition; reserve exact one for state whose creator and current execution path already
make absence non-resting. See [QUANTIFIERS.md](QUANTIFIERS.md) for the count contract and
[SEQUENCING.md](SEQUENCING.md#selected-direction-scoped-completion) for the proposed validation
boundary.

A locally legal choice can discover later that its remaining work cannot complete. This is a
recoverable dead end: `DeadEndException` rolls the encompassing transaction back so another branch
can be tried. A broad choice is not a correctness bug merely because one branch fails late; it is a
bug if an illegal result can commit or no branch can reach a legal result.

### The metric operators are intentional

`Metric.Max`, saturating `Metric.Subtract`, and non-double-counting `Metric.Or` have few authored
uses, but they express different operations and are not redundant special cases. In particular,
union cannot substitute for arithmetic addition. Treat the small number of clients as evidence that
the metric algebra is still sparse, not as a reason to collapse these operators. `Metric.Rank`
likewise replaces game-specific ranking instructions with reusable comparison semantics.

### Content must not compensate for an engine gap

Do not represent one compact rule with parallel marker Classes, a watcher for every resource, or
duplicated Effect branches merely because a general Type or trigger operation is missing. Repair the
general mechanism or leave marginal content unsupported with an explicit blocker.

The promo attack model is the reference case: one generic victim-owned watcher records removed
resources and decreased production while retaining the attacked Type and Actor. Crash Site Cleanup
and Mons Insurance consume those records; prevention rules remain direct removal triggers. Do not
replace that shape with an `AttackKind` hierarchy and per-resource watchers.

## Actor Engines and Agents

Each World retains one policy-free `ActorEngine` per Actor. It owns attributed task and state
commands but shares the World's changer, instructor, timeline, and Task services. `Agents(world)`
constructs the corresponding normal client objects and prevents pairing an Agent with an unrelated
World.

All ordinary Agent mutations use the shared outer transaction-completion path. `runOperation`
admits new work, lets the body finish it, runs configured autoexecution, preserves unrelated
pre-existing unselected Tasks, and rejects newly unfinished Tasks or `MustCleanUp` state. It cannot
start while a pre-existing selected Task holds the World lock. `sneak` remains an engine cheat: it
applies fully concrete changes through the timeline and graph but skips normal instruction
resolution and effects.

Current autoexecution lives in `:agent`, not in the core engine. Policy selects legal Task commands;
it does not alter their semantics. Direct engine primitives remain available to trusted workflow,
replay-correction, test, and cheat code. Preventing those callers from reaching the primitives is
not a current requirement.

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
- [`PetElaborator.kt`](../../src/common/dev/martianzoo/pets/PetElaborator.kt): shared input and
  effect transformation.
- [`Agent.kt`](../../src/common/dev/martianzoo/agent/Agent.kt) and
  [`AgentImpl.kt`](../../src/common/dev/martianzoo/agent/AgentImpl.kt): current client surface and
  transaction completion.
