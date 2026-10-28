# Automatic task revisions and handling

> **Source-based inventory — work1 at `f1055a08b`, reviewed October 28, 2026.** This
> inventories the implementation in this checkout. It is an account for review, not approval of every
> behavior. [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns the selected destination and open details.
> [ENGINE_TRANSITION_PLAN.md](ENGINE_TRANSITION_PLAN.md) orders remaining transition work;
> [ENGINE_TRANSITION_RESULTS.md](ENGINE_TRANSITION_RESULTS.md) records code already changed on the
> separate experiment branch. Verify the target checkout's source before using any current-code path.

## Scope and reading order

The destination is to identify a permitting task, combine it with an explicit execution request,
establish the unique narrowing whose transaction succeeds, and execute that transaction. Today's
paths below do not yet implement that general rule. In particular, they contain execution during
selection and special inference rules; this inventory explains when those paths run, what they
change, and what can fail. It does not require callers to supply every concrete detail themselves.

“Automatic” here means performed without a separate client instruction for that step. It includes
rewrites and bookkeeping, not just double-colon effects or agent autoexecution. An instruction
being resolved and concrete does not prove successful execution or action completion.

| Moment | Work performed | Principal source |
| --- | --- | --- |
| Before admission | Elaborate client input or specialize a triggered effect; calculate its initial assignee. | [AgentImpl](../../src/common/dev/martianzoo/agent/AgentImpl.kt), [LiveEffect](../../src/common/dev/martianzoo/engine/LiveEffect.kt), [PendingTask](../../src/common/dev/martianzoo/engine/PendingTask.kt) |
| Admission and queue edits | Normalize syntax, split independent work, manage continuations, route concrete work, perform ordinary dispatches. | [newTasks](../../src/common/dev/martianzoo/engine/newTasks.kt), [TaskQueues](../../src/common/dev/martianzoo/engine/TaskQueues.kt) |
| Matching, selection, narrowing | Identify the task, resolve current-world questions, bind shared choices, record selection or handoff, split exposed groups, possibly execute immediately. | [ActorEngine](../../src/common/dev/martianzoo/engine/ActorEngine.kt), [Instructor](../../src/common/dev/martianzoo/engine/Instructor.kt) |
| Execution and consequences | Change components, remove dependents, construct required parts, run immediate effects, collect queued effects, check invariants. | Instructor, [Changer](../../src/common/dev/martianzoo/engine/Changer.kt), [Effector](../../src/common/dev/martianzoo/engine/Effector.kt), [Limiter](../../src/common/dev/martianzoo/engine/Limiter.kt) |
| Completion and subsequent work | Admit effects and continuation, remove the task, promote deferred work, let drivers handle eligible tasks. | ActorEngine, TaskQueues, [AutoExecLoop](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt), [WorldTransaction](../../src/common/dev/martianzoo/engine/WorldTransaction.kt) |

The inventory follows every case in `normalizeForTask`, `normalizeTask`, `Instructor.resolve`,
and `doExecuteResolved`, plus the paths that create, edit, remove, dispatch, or promote tasks.
Initialization, corrections, probes, and history restoration are included separately because they
do not all take the ordinary path. Static language lowering is owned by Pets; its runtime entry
points are identified here rather than redefined as another engine processing stage.

## Automatic actions without a separate execute command

**Review evidence, October 24, 2026.** The user's starting presumption is that selection should
never execute anything. This table distinguishes interpreting an instruction, editing tasks, and
changing components so that exceptions can be considered explicitly. A row describes current code,
not permission to preserve it. Ordinary execution of `::` and authored calculations remains
required when an execution command is actually submitted.

| Entry or condition | What currently happens without another execute call | Component changes? |
| --- | --- | --- |
| Task admission or replacement | Remove no-ops, reject direct mandatory `Die`, simplify alternatives, and separate eligible `THEN` continuations. | No; this can fail before a body executes. |
| Ordinary pure dispatch admitted or exposed by an edit/promotion path that admits it | Enqueue its body for its recipient and admit its continuation; do not retain a separate ordinary dispatch task. | No. |
| `selectTask` or `narrowTask` resolves to concrete work owned by the calling Actor | Execute the instruction, run immediate effects, create queued reactions, admit the continuation, and remove the selected task. | Yes for component instructions. |
| Selection/narrowing resolves to zero or `Ok` | Finish the task and admit its continuation without a component event. | No, but the task is completed. |
| Selection/narrowing exposes a group, including `EACH` branches or a gated group | Admit independent children and the original continuation, then remove the original task. Children are not automatically executed by this structural replacement. | No. |
| Selection produces a concrete `BY` or System handoff | Change the assignee; the receiving Actor executes later. | No in this step. |
| Any successful outer engine transaction, including queue edits | Promote eligible `LATER`/`FINALLY` batches and perform dispatches exposed by promotion. | No direct component changes; tasks can be created/removed. |
| `doTask(..., executeSubmittedGroup=true)` | After the matched task disappears, attempt a snapshot of tasks newly added to this Actor's queue. | Yes; see the verified example below. |
| `doTask(..., combineScalars=true)` | Choose the matching total and execute its constituent tasks, gains before removals. | Yes; each constituent retains its effects. |
| `canSelectTask` or `canExecuteTask` | Attempt selection/execution and restore recorded state afterward. Listeners can still observe the attempt. | Temporarily, depending on the task. |
| Engine uses a trial internally | Player priority uses `canSelectTask`; bootstrap uses `canExecuteTask`; `selectionHandsOff` attempts and restores selection. | Depends on the trial; these calls were not explicitly requested as trials by the caller. |
| Engine construction | Execute configured creation work and pending bootstrap tasks before returning the World. | Yes. |
| Agent call after an engine selection/narrowing | `AutoExecLoop` may submit Admin and configured Player work; the agent owns those choices. | Yes, but separate engine calls are made. |

**Selected design correction:** for `EACH`, admitting all children and consuming the parent is
execution of that parent task, even when no component changes yet. The current selection path
therefore executes `EACH` too early under [the contract](ENGINE_CONTRACT.md#selection-must-not-quietly-mean-execution--item-9).
Move that expansion to an execution request; the children then follow ordinary priority. This
inventory still describes current code, not the corrected behavior.

Task matching and `prepareTaskNarrowing` can interpret or combine instructions without changing
components or tasks. They must not be confused with the engine trials above. Similarly, narrowing
an expression in a client-held `TaskForm` does not execute the task until submission. The last row
must remain separate from engine selection behavior when testing: Player `NONE` disables Player
agent automation, not the engine's execution-on-selection and not mandatory Admin work.

Sources: ActorEngine `selectAndExecuteIfConcrete`, `narrowSelectedTask`, `applyResolution`,
`selectionHandsOff`, `requireNoOtherSelectedTask`, and `doTask`; TaskQueues `admit`, `perform`,
`promoteDeferredWork`; Transactions `perform`; Initializer `drainBootstrapTasks`; AutoExecLoop.
The sections below describe each path and the remaining direct execution and correction paths.

### Extra execution requested by executeSubmittedGroup

**Verified with focused temporary JVM tests on October 24, 2026**, using synthetic declarations:

```pets
CLASS First { This: Second }
CLASS Second
CLASS Third
```

Enqueue `First! THEN Third!` for a Player. The task's current instruction is `First!`, with
`Third!` stored as its continuation. Execute the current instruction through the raw engine:

```kotlin
engine.doTask(parse<Instruction>("First!"), executeSubmittedGroup = true)
```

The call gains First, whose queued reaction creates a Second task; completion also creates the
Third task. The flag then attempts both newly added tasks, so First, Second, and Third all have
count 1 when the call returns. With the flag false, only First is gained and the Second and Third
tasks remain pending. No agent autoexecution is involved in this comparison.

This is not the behavior of every `doTask` call. The flag defaults to false. Agent string commands
set it for submitted groups; the `doTasks` path also recognizes groups produced by elaboration.
A companion test, with Player policy NONE, submits `First!, Third!` against
`(First!, Third!) OR Other!`. It executes the submitted children but leaves their newly created
Second reaction pending: the loop iterates a snapshot, not an expanding queue. These tests prove
the convenience flag can execute extra work and that ordinary group handling does not recursively
drain all descendants. They do not establish a Canon card that relies on the extra execution.
The temporary tests were outside the repository; no production or permanent test source changed.

## 1. Before a task reaches the queue

### Client input and computed instructions

- String APIs parse and call `PetElaborator.elaborateInput` with the submitting Player context.
  Names, defaults, free `Me`, owned context, dispatcher recipients, type-variable scopes, and
  atomized gains can therefore already have changed the syntax before `ActorEngine.addTasks`
  sees it. Session instruction input rejects explicit property evaluation; triggered/custom
  instruction paths have their own elaboration context. Direct engine calls receive instruction
  trees; queue admission is not a second full source-language compiler.
- A surviving instruction `Transform` is an error in both queue normalization and resolution.
  Constructs lowered by Pets, such as production instructions, must already have runtime meaning.
- A gained `CustomInstruction` produces an additional queued self-effect. During queued-effect
  collection, its registered Kotlin implementation is invoked with the current reader and its
  dependency types. Dependencies must exist; missing implementations, unsupported argument counts,
  or failed/invalid translations throw. Its returned Pets is elaborated with the component's owner,
  multiplied by the triggering change count, and then follows ordinary effect/task admission.
  The returned instruction is not executed as part of translating it.
- `PetElaborator.atomizeGains` splits an actual gain count greater than 1 for an `Atomized`
  class into independent gains of 1. Runtime call sites include effect output, computed `PER`,
  and required-part construction. A multi-count transmutation into an Atomized type is instead
  rejected during change resolution; it is not silently split.

Sources: AgentImpl `parseTaskNarrowing`/`parseInstructionGroup`,
[PetElaborator](../../pets/src/common/dev/martianzoo/pets/PetElaborator.kt),
[CustomInstructionRuntime](../../src/common/dev/martianzoo/engine/CustomInstructionRuntime.kt),
Effector `customInstructionEffect`.

### Triggered instruction construction

Effects are compiled and specialized to their owning component before use. At a matching change:

- `This`, component dependencies, named type-variable occurrences, and matched trigger variables
  specialize the effect. Properties in the resulting instruction are evaluated at this point;
  this is distinct from evaluating a pending instruction's world metric later.
- Gain/removal/self triggers, trigger-side `BY`, and trigger-side conditions determine whether
  work is produced at all. An `OR` trigger uses its first matching alternative, not separate work
  for every matching alternative. Explicit actor variables bind before dependent trigger matching.
- Ordinary triggers multiply their result by the event's change count. An `X` trigger instead
  binds X to that count and uses multiplier 1. Repeated copies of another listening component
  additionally multiply its effect output.
- Player-owned listeners to otherwise unowned, non-System changes receive an implicit performer
  restriction during effect compilation. Passive non-Player owners can lose inherited effects
  whose Player-bound output cannot validly specialize to them.
- Initial queued-effect assignment is: effect context's Player owner, otherwise changed component's
  Player owner, otherwise the event's Actor. Immediate effects skip the changed-component-owner
  fallback. A passive owner never becomes an Actor.
- The pending work records the context and triggering event as its cause. Subsequent normalization
  preserves this cause; it does not give every rewrite a new gameplay cause.

Sources: LiveEffect `compile`, `Subscription`, `Hit`; PendingTask `fromEffect`.
[IDENTITY.md](IDENTITY.md) owns the actor/owner interpretation.

## 2. Admission and normalization

`TaskQueues.addTasks` normalizes the whole incoming group before admitting its children. Each task actually inserted receives its add-event ordinal as its ID.
It can return no tasks or several tasks, including dispatch bodies belonging to other Actors.

### Structural rewrites

| Input or condition | Automatic handling | When |
| --- | --- | --- |
| Independent group, including nested groups | Flatten to independent instructions; omit `Ok`; create separate tasks. Siblings gain no game ordering. | Admission; again when resolution exposes a group. |
| Literal zero-count change | Replace with `Ok`, so no task is admitted for it. | Admission's recursive normalization. |
| `Ok` in a group or sequence | Omit it; collapse an empty sequence to `Ok`, a singleton to its remaining instruction. | Admission and task edits. |
| `Ok OR A` or `gate: Ok` | Preserve the meaningful choice or gate. Do not erase it merely because its body does nothing. | Admission and edits. |
| Direct gain of `Die!` | Throw immediately, including inside normalized sequence/continuation bodies. | Admission and edits, before ordinary execution. |
| Direct `Die?` or `Die*` | Replace with `Ok`; surrounding structure then normalizes. | Admission and edits. |
| An `OR` arm whose normalization throws `DeadEndException` | Omit that arm. If all arms fail, throw; otherwise deduplicate equal arms and collapse a sole survivor. | Admission and edits. |
| `BY` around a normalized sequence | Put the performer wrapper on each stage. Wrapped `Ok` disappears. | Admission and edits. |
| `EACH` or dispatch with an `Ok` body | Omit the whole construct. | Admission and edits; no target enumeration needed. |
| `PER` whose normalized body is `Ok` | Omit it. If its body becomes an independent group, reject that shape. | Admission and edits. |

The `Die` rewrite here recognizes a direct `Die` gain, not an arbitrary future consequence that
might reach `Die`. A zero literal is discarded before this check during admission. Gates do not
postpone recursive normalization of their body. Normalization does not run effects or establish
transaction legality.

`editTask` normalizes the replacement but does not perform admission, ordinary dispatch execution,
or System reassignment by itself. If the normalized data equals the stored task, no edit event is
recorded. Otherwise ID and the supplied lifecycle data survive. An edit cannot store a group where
a task requires a single instruction; structural replacement belongs to the selection path below.

Sources: newTasks `taskInstructions`, `normalizeForTask`, `normalizeTask`;
[InstructionGroup](../../pets/src/common/dev/martianzoo/pets/ast/InstructionGroup.kt);
[Instruction](../../pets/src/common/dev/martianzoo/pets/ast/Instruction.kt) factories.

### `THEN`: retaining or separating stages

- Normalize stages and expand named type-variable references first.
- If the sequence has an unresolved shared X originating in its first stage, or that stage uses
  an unresolved shared type variable, retain the sequence as a single task. It needs the first
  choice to bind the later stage.
- Otherwise, when `Task.then` is empty, store the first stage as the current instruction and
  the remainder in `Task.then`. That remainder is admitted after the current task is handled.
- If an outer continuation already occupies `Task.then`, preserve the inner sequence and outer
  continuation separately. Merging them would merge independent variable scopes.
- A retained sequence containing instruction-side `BY` is rejected. This includes a `BY`
  distributed over a sequence that must remain together.
- Choosing a first stage can bind type variables and X throughout its sequence, making separation
  possible on the next normalization. A nonmandatory zero result can still carry a binding into
  the continuation.
- Separable `A THEN B` means B becomes pending after A's task, not after every queued reaction
  caused by A. When selection exposes independent children, the old continuation is also admitted;
  it does not wait for those children.

Sources: newTasks `normalizeTask`; Instruction.Then `mustRemainOneTask`,
`selectFirstStage`, `continuationAfterFirst`; ActorEngine `prepareNarrowing`, `handleTask`.

### Assignment to Admin and explicit performers

`assignConcreteChangeToAdmin` runs at admission and contextual selection. It does not operate on
every arbitrary task edit.

- Leave work already assigned to Admin alone.
- Leave an abstract current instruction with its existing assignee. Optional counts, X, alternatives,
  or unresolved targets can therefore preserve a Player's choice.
- Look through `BY`, `PER`, and gates for a change; ignore an actual zero count.
- Inspect the gained type, or the removed type for a pure removal. If it is System, assign the
  task to Admin. For transmutations this means the destination determines this check.
- Wrap a System task's existing continuation in a dispatch back to the former assignee. Admin's
  execution of the System step must not silently take the Player's later choice.
- For non-System work, admission recognizes a concrete pure gain explicitly `BY Admin` through
  `BY`/gate wrappers. It does not apply that shortcut uniformly to removals, transmutations,
  or `PER` wrappers.
- After resolution, a concrete outer `BY actor` assigns the task to that participating Actor
  and strips the wrapper. Its continuation retains the explicit performer's scope. A leftover
  `BY` that reaches Instructor execution is an error.

A concrete gated or scaled System change can be assigned before its gate or metric has been
evaluated. “Concrete” does not by itself mean “resolved,” “legal,” or “executed.”

Sources: TaskQueues `assignConcreteChangeToAdmin`; ActorEngine `applyResolution`;
Instructor `canonicalActorExpression`; [actors](../../src/common/dev/martianzoo/engine/actors.kt).

### Dispatch: `TASK`, `LATER`, `FINALLY`

A dispatch is concrete when its recipient is fixed; its body can still contain choices.

- When the normalized current instruction is a dispatch and the containing task is ordinary,
  perform it during admission without inserting a task for the dispatcher.
- Admit the body for its fixed recipient with the dispatch's deferral. Admit the dispatcher's
  continuation immediately for the dispatcher's assignee, as ordinary work.
- A recipient must resolve to a fixed, participating Actor. The engine does not choose an Actor
  merely because an open selector currently happens to match a single component.
- If the containing task itself is deferred, retain the dispatch until promotion. Promotion then
  performs it and removes the dispatcher. An inner `LATER` can therefore defer its body again.
- A dispatch exposed only after selection follows execution's pending-work path. It still gives
  the recipient a task rather than executing the recipient's body inside the dispatcher.

For example, `TASK Player2 { Choice } THEN Marker` admits Player2's choice and the continuation;
Marker does not wait for Player2 to finish choosing. `LATER { A }` admits deferred A now, but
A's world-dependent resolution waits until it is selected after promotion.

Sources: TaskQueues `admit`, `perform`; Instructor `admitted`, `doExecuteResolved`.

## 3. Matching, selection, and supplied narrowing

These methods do more than mark an existing task selected:

1. A submission can identify work by task ID or by instruction compatibility. An existing selection
   takes precedence. Matching can resolve candidate instructions without changing their stored data,
   or provisionally ignore failed world conditions to identify the intended task and report its
   actual rejection. It does not execute all candidates.
2. Matching can inherit an omitted quantifier from compatible pending work, intersect the supplied
   instruction with a pending alternative, retain a gate omitted by the caller, or recognize a
   selected first `THEN` stage. An outer submitted `PER` is evaluated before matching.
3. A typed zero submission cannot identify unrelated work merely through a shared `Ok`
   intersection. Otherwise equivalent tasks differing only in ID/cause use the first match;
   distinct compatible tasks require explicit disambiguation.
4. Ordinary selection checks competing selections globally and rejects deferred work, resolves
   against the current World with contextual gain narrowing enabled, then applies that result.
   Already-selected tasks skip the initial resolution step; supplied narrowing can resolve again.
5. A single resulting instruction is normalized, marked selected, reassigned if needed, and edited
   in place. If it is concrete and remains with the calling Actor, selection/narrowing executes it
   immediately. A partial instruction can currently remain selected across calls.
6. A group result, even a singleton group, replaces the original with ordinary unselected child
   tasks. The original's continuation is admitted and the original is removed. An empty result
   completes it structurally. Children do not inherit selection.
7. A concrete handoff records assignment and leaves the task selected for the receiving Actor.
   It does not trial-execute that Actor's instruction; execution occurs in a later transaction.

Supplying a narrowing is checked against the pending instruction and shared-variable bindings;
independent source/destination deductions cannot pick inconsistent values for the same variable.
If selection hands the task to another Actor, the original Actor cannot supply that Actor's choice.
A structural split may require a later submission naming a child.

The overloads do not all follow the same route: `doTask(narrowing)` can apply a matched narrowing
directly via `narrowSelectedTask`, while `selectTask(id)` takes the explicit selection path.
Both routes call `requireNoOtherSelectedTask`, whose Player priority check uses the existing trial
rather than complete legal-choice enumeration. Queue edits and corrections still bypass selected-task
exclusion. See ENGINE_CONTRACT's current implementation differences rather than treating every
mutation as an equivalent task command.

Sources: ActorEngine `matchingTask`, `effectiveNarrowing`, `intersectTask`,
`selectFirstStageOrNull`, `selectTask`, `narrowSelectedTask`, `applyResolution`,
`executeSelectedIfConcrete`.

## 4. Resolution against the World

Resolution returns a revised instruction tree without itself mutating the World. It can occur
during matching/probes, selection, narrowing, immediate execution, and later stages of a retained
sequence. Different entry points enable different deductions; it is not a background process
continually rewriting every pending task.

| Instruction | Resolution behavior |
| --- | --- |
| `Ok` | Remains `Ok`. |
| Change | Resolve types and quantity using the rules below; retain an unresolved choice or throw if necessary. |
| `BY` | Resolve its body; canonicalize and validate its concrete Actor expression. Actual participating-Actor lookup occurs when handing it off. |
| Dispatch | Validate/canonicalize its recipient; do not resolve its body yet. |
| `PER` | Count the metric now, multiply the inner instruction, atomize computed gains, and resolve the resulting instruction. A group remains independent work. |
| Gate | Check the requirement now; fail if false, otherwise remove the gate and resolve its body. |
| `EACH` | Require an abstract selector; enumerate matching component types now, bind each target, evaluate body properties, and resolve each branch. Return a group even for a sole branch; no matches yields an empty group. |
| `OR` | Resolve arms against the same current World; discard arms throwing `NotNowException` or `DeadEndException`. Collapse equal/sole surviving alternatives. If none survives, report dead end, all-gates failure, or other unavailability as appropriate. |
| `THEN` | Resolve only the first stage; preserve later stages for the later World. If an open shared type choice prevents resolving the first stage, retain it, with the feasibility check below. |
| Unlowered transform | Throw an expression error. |

`EACH` preserves matching-component multiplicity: two copies of the same matched component
produce two copies of its branch. The multiset maps can compute that branch once and retain its
count; group admission then exposes the repeated instructions. For queued work, the resulting
children are separate tasks. During immediate execution, an exposed group executes in place instead.
Resolution leaves grouped branches independent rather than recursively treating the group as a
sequential instruction.

For an open shared type choice in a first `THEN` change with a known nonoptional count, resolution
can test a broad domain with refinements stripped: if even that domain cannot supply the mandatory
amount, it throws; if an AMAP first stage has no useful target, it substitutes `Ok`. The later
stages survive. This is immediate feasibility, not prediction of their consequences.

### Type deductions

- Resolve expressions and refinements against current facts while retaining authored variable
  names needed for later bindings.
- For an abstract gain, reject missing declared dependencies, then try a unique concrete subtype
  from the type model.
- During contextual selection/narrowing, also try a unique gain identified by present dependencies.
  This search is specifically enabled when subclasses introduce relevant dependencies or an
  abstract dependency with a declared maximum of 1 identifies a unique present component.
  Ordinary target selection does not filter alternatives by positive count capacity.
- Inside a recursive immediate effect, also allow a unique gain with positive capacity under
  current bounds. An unresolved immediate-effect choice fails rather than asking a Player.
- For abstract removal, infer the target if the matching component multiset has a sole distinct
  element. Preserve an abstract Actor dependency for AMAP instead of inferring the Actor from
  the sole existing resource.
- A transmutation with an open named exclusion waits for binding. Independently inferred gain and
  removal types must still narrow the original paired instruction; otherwise retain the choice.

### Quantity and availability changes

[QUANTIFIERS.md](QUANTIFIERS.md) owns the detailed quantifier contract. The runtime decisions are:

- Unbound X prevents change resolution. An actual zero returns early with its types preserved.
  Such a selected task can finish without a target, dependency, change event, or System handoff.
- Uninhabited targets fail when mandatory and become `Ok` otherwise. Virtual custom metrics
  cannot be gained or removed. A resolved mandatory `Die` gain throws.
- For abstract pure gains/removals, mandatory requires a candidate able to supply the full count;
  AMAP requires a candidate with positive capacity. No candidate means mandatory failure or AMAP
  `Ok`. Optional removals become `Ok` when no matching component exists. Optional gains do not
  discard their choice merely because all capacities are zero.
- Abstract transmutations do not run that independent candidate search; source and destination
  may share a binding.
- For concrete changes, require destination dependencies and enough physical source copies.
  AMAP/optional also use current invariant headroom/footroom; mandatory changes defer those
  declared count checks so immediate effects can repair them.
- AMAP clamps to the selected target's available count and becomes mandatory. Optional clamps
  its upper bound but remains optional. Mandatory rejects an unavailable requested count.
  Limits shared by both sides of a transmutation cancel; dependency survival still limits removal.
  A concrete self-transmutation has zero capacity.
- Missing-dependency fallback to `Ok` is limited to nonmandatory pure gains that are optional
  or still abstract, excluding custom metrics. It does not make a concrete AMAP gain or a
  transmutation into a missing destination legal.

Sources: Instructor `resolve`, `resolveEach`, `resolveOr`, `resolveChange`,
`autoNarrowTypes`; Limiter `findLimit`, candidate searches.

## 5. Execution, immediate consequences, and queued reactions

### The current instruction

`executeSelectedTask` passes the resolved instruction to Instructor. A remaining abstract
instruction fails; retained `THEN` is handled stage by stage. `Ok` and an `OR` containing only
zero/no-op alternatives do nothing. A dispatch creates pending work. A leftover `BY` is rejected.

A retained sequence executes its resolved first stage, then resolves and executes each later stage
against the World produced so far. This is different from a separated `Task.then`, which creates
later pending work. Neither form waits for newly queued reactions to execute.

### Each elementary component change

1. An actual zero returns without a change event.
2. A pure Signal gain is recorded as a transmutation from the Signal to itself: it fires a point
   event without retaining a component. This is distinct from an authored self-transmutation.
3. Before changing authoritative state, compile any needed effects. Apply the event and update
   the derived subscription index before matching reactions.
4. If removal cannot proceed because dependents remain, recursively remove all copies of a
   dependent first, process that removal's consequences, and retry the original change.
   These are real events, not silent cleanup of the component map.
5. For newly present owners, construct missing required parts only for exact positive count
   declarations naming concrete types directly dependent on that owner. Abstract requirements,
   minimum-only requirements, and requirements for the owner itself remain constraints.
   Accumulate recursive construction events until the required parts exist, preferring parts
   whose dependencies are already present. Recalculate deficits so overlapping requirements do
   not duplicate parts. Construction does not invent absent unrelated dependencies.
6. For that construction batch, run immediate effects in reverse construction-event order, so
   parts' reactions precede owners' reactions. Then collect queued effects in construction order.
   The same ordering applies to a simple change as a batch containing just its event.
7. After a top-level elementary change and its complete recursive immediate-effect chain, check
   affected count invariants. The next elementary change does not get to repair a violation
   already rejected here. Required-count checks are enabled after bootstrap.
8. Instructor returns accumulated pending tasks. ActorEngine admits them before admitting the
   selected task's continuation and recording its removal.

### Effect matching and execution

For each event and effect kind, `Effector.fire` materializes the complete matching batch before
executing any member. Trigger conditions in that batch see the same post-event World; execution
of an earlier member does not retract a later member already matched. Immediate instruction
resolution can still inspect the World produced by earlier effects.

A changed component's own immediate effects retain declaration order and complete before the
other immediate listeners. Independent listeners have a stable diagnostic order, or a randomized
diagnostic order when enabled; neither order establishes a game rule. Recursive immediate work
shares a depth limit of 8, including required-part construction. System changes inside immediate
effects are attributed to Admin.

Queued `:` effects are matched after the relevant immediate work, but their instructions remain
pending. An immediate effect may itself produce changes whose queued reactions are collected
during that recursion; this is not a single snapshot of all queued conditions for the transaction.
An immediate instruction must become concrete without a client choice. Dispatch in an authored
immediate effect is rejected by Pets validation.

Sources: Instructor `executeChange`, `constructPartsAndFire`, `executeAutomaticEffect`;
Changer `change`; Effector `fire`. Required-part eligibility is defined by
[ClassLimitTable](../../pets/src/common/dev/martianzoo/pets/types/ClassLimitTable.kt).

## 6. Finishing tasks and promoting deferred work

Normal completion admits `Task.then` before removing the old task. The continuation has the old
task's current assignee and cause, is ordinary, and undergoes all admission rewrites, including
dispatch and System routing. Queued reactions from execution have already been admitted.
The continuation has no special priority over those reactions.

At successful transaction completion, TaskQueues checks the whole World's pending work:

- If any ordinary task remains, do nothing.
- Otherwise take all tasks in the earliest remaining deferral together: `LATER` before
  `FINALLY`. Record edits making that entire batch ordinary before performing its dispatches.
- Perform promoted dispatches, admit their bodies/continuations, and remove their dispatcher
  tasks. If no ordinary work remains afterward, repeat promotion.
- Newly deferred work waits while any ordinary member or consequence of the current batch
  remains. A batch belongs to the World, not a particular Player's queue.

Promotion belongs to the completed transaction that made it eligible. A temporarily empty queue
inside a compound command does not promote deferred work before that command finishes.

Sources: ActorEngine `handleTask`; TaskQueues `addTasks`, `removeTask`,
`promoteDeferredWork`. A bare `dropTask` uses removal directly and does not admit its continuation.

## 7. Other entry paths and automatic conveniences

| Entry path | Additional or different handling |
| --- | --- |
| `doTask` with `executeSubmittedGroup` | After the matched task disappears, attempt a snapshot of newly added tasks in that Actor's queue, including effects and continuations; stop when a task remains selected. This is not recursive draining of every later descendant. |
| `combineScalars` | Resolve compatible pending scalar changes; if their signed total matches the supplied total, execute gains before removals. Newly spawned work does not join that total. |
| `canSelectTask` | Trial selection, including execution if concrete and still with the original Actor, then restore after transaction completion and deferred promotion. An abstract selection or handoff can return true. Gameplay rejection and unresolved instructions become false; other failures propagate. |
| `canExecuteTask` | Trial execution and restore after transaction completion and deferred promotion; true only if the original task disappears. Later queued tasks are not executed. Gameplay rejection and unresolved instructions become false; other failures propagate. |
| `tryTask` | Execute within an atomic block; restore when choices/unavailability prevent completion, or a selection remains as specified by the overload. Other failures restore and propagate. The instruction overload evaluates an outer `PER` before that recovery block. |
| `trySelectedTask` | Attempt selected work; missing choices return false, while current unavailability is reported as a dead end. Its mutation is transaction-restoring; its public role remains under review. |
| `restoreTask` | Replace an existing task's data and normalize it. Current code can alter instruction, assignee, and selection without ordinary selection checks; it does not recreate a removed task. |
| `sneak` correction | Execute concrete elementary changes, required parts, dependent removals, and immediate effects, suppressing queued reactions. Check all invariants after the whole correction group, using a failure-restoring wrapper. No ordinary task handling or completion callback. |
| Initialization | Create the Admin seed directly, then admit configured work through ordinary Admin task commands. Retry dependency-blocked component creation; drain choice-free bootstrap tasks, reject leftover choices, audit required counts, and establish the history floor. |
| Event restoration/replay | Apply/reverse saved task and component events, synchronizing derived effect subscriptions. Do not rerun matching, immediate effects, admission rewrites, or autoexecution merely to restore a position. |

Sources: ActorEngine methods named above; [Initializer](../../src/common/dev/martianzoo/engine/Initializer.kt);
Changer `replay`/`rollBackTo`; [TimelineImpl](../../src/common/dev/martianzoo/engine/TimelineImpl.kt).
Trial restoration does not undo arbitrary observer/application side effects.

## 8. Agent automation and synchronous completion

These explain behavior seen through agent calls; they are not additional engine task rewrites.

- Agent input helpers elaborate instructions, initially attempt operation tasks, and invoke the
  shared AutoExecLoop at command/operation finishing points. TaskForm edits remain client-held
  until committed.
- AutoExecLoop respects selected-task priority and runs mandatory Admin handling before optional
  Player policies. It retries waiting Admin tasks after progress and throws a retained rejection
  after an unsuccessful pass, subject to the existing waiting exceptions. The existing probe remains an
  approximation; [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md#current-implementation-differences)
  records the eligibility rules and the remaining complete-choice-analysis gap.
- WorldTransaction runs finishing work and validation for outermost calls, with special handling
  for calls from completion callbacks. Agent decisions and queue edits also run Admin handling
  before returning from ordinary nested calls.
- Completion callbacks run after the first restoring block. Returned results include their retained
  changes and subsequent finishing work. A callback failure does not restore the earlier block.
  Callbacks cannot seek or truncate history.
- `requireComplete` does not execute tasks. It checks outstanding work and `MustCleanUp`, with
  a current exception once `FINALLY` promotion has occurred since the last decision. That check
  can precede scope removal and is not the proposed action-completion definition.

The engine does not search a Single-Action Decision Graph or prune choices by future completion.
Immediate target/quantity checks and temporary execution probes must not be mistaken for that
proposed agent analysis.

## 9. Differences to keep visible during review

These are known implementation observations, not new recommendations competing with the contract:

- Ordinary execution checks invariants per elementary change plus its immediate chain; a broad
  multi-change transaction cannot necessarily postpone those checks until its final statement.
- Direct task edits/corrections bypass some selected-task exclusions. Player priority uses the
  existing execution probe rather than complete legal-candidate enumeration.
- Concrete non-System `BY Admin` routing is narrower at admission than the intended general rule.

Reviewing these differences does not authorize implementing them, preserving them as requirements,
or expanding the partial-narrowing investigation.

## Evidence and maintenance

The source map and named methods above support this inventory. Representative existing tests below
identify concrete scenarios; source and selected assertions were inspected for this draft, but no
tests were run and no claim of complete test coverage is made.

| Area | Existing scenarios |
| --- | --- |
| Normalization and variable scopes | [TaskNormalizationTest](../../test/common/dev/martianzoo/engine/TaskNormalizationTest.kt): terminal rewrites, meaningful gated/no-op choices, edits preserving continuation scope. |
| Dispatch and handoff | [TaskDispatchTest](../../test/common/dev/martianzoo/engine/TaskDispatchTest.kt): immediate admission of recipient choices and continuations; [TaskAssignmentCharacterizationTest](../../test/common/dev/martianzoo/engine/TaskAssignmentCharacterizationTest.kt): System routing, abstract choices, performer wrappers. |
| Selection and quantities | [TaskResolutionTest](../../test/common/dev/martianzoo/engine/TaskResolutionTest.kt), [TaskNarrowingTest](../../test/common/dev/martianzoo/engine/TaskNarrowingTest.kt), [ZeroCountTaskTest](../../test/common/dev/martianzoo/engine/ZeroCountTaskTest.kt): current-world resolution, chosen-target AMAP, typed zero. |
| Deferred work | [DeferredWorkTest](../../test/common/dev/martianzoo/engine/DeferredWorkTest.kt): whole-World batches, nested deferrals, delayed metric/fanout evaluation, restoration. |
| Effects and construction | [TriggerScalingTest](../../test/common/dev/martianzoo/engine/TriggerScalingTest.kt), [AutomaticEffectOrderTest](../../test/common/dev/martianzoo/engine/AutomaticEffectOrderTest.kt), [InvariantCompletionTest](../../test/common/dev/martianzoo/engine/InvariantCompletionTest.kt): multiplicity, shared trigger snapshots, required parts, immediate repairs, per-change checks. |
| Computed gains | [PerAtomizationTest](../../test/common/dev/martianzoo/engine/PerAtomizationTest.kt), [EffectAtomizationTest](../../test/common/dev/martianzoo/engine/EffectAtomizationTest.kt): separate gains and choices. |
| Call completion and rollback | [WorldTransactionTest](../../test/common/dev/martianzoo/engine/WorldTransactionTest.kt), [ActorEngineTransactionTest](../../test/common/dev/martianzoo/engine/ActorEngineTransactionTest.kt): restoring scopes and callback behavior. |

Keep this as a runtime inventory, linking detailed language and quantifier contracts rather than
creating another normative language specification. When task handling changes, revisit the owning
section and its entry-path distinctions, especially any claim about when state is inspected.
