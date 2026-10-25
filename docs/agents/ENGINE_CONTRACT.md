# Engine contract

> **Selected destination — October 24, 2026; transition remains experimental.** The responsibility
> rules below are intended behavior, not a claim that the current implementation satisfies them.
> **Selected** records agreed direction; remaining choices are explicitly labeled.
> **Recommendation** identifies additional suggestions. Current-code observations are evidence, not approval of that behavior.
> **Investigation direction** records an experiment to try, not a completed behavior change.
> **Open** and **working presumption** retain questions and preferences that are not settled rules.
> Keep interaction terms here, not in the glossary.

## Foundations

The component state is a multiset; component changes are additions and removals. Tasks, effects,
and event history participate in the existing reversible state model. Within a live World, changes
follow a single linear timeline. This design assumes serial calls, not simultaneous calls from
different threads.

A checkpoint identifies a position to restore. Nested rollback scopes can use their own checkpoints
without a separate commit protocol: a small scope restores an individual failed change; an enclosing
scope can restore several completed changes together. These are scopes over the same recorded
history, not concurrent transactions. The existing [Timeline](../../src/common/dev/martianzoo/engine/Timeline.kt)
has navigation and restoration limits; this foundation does not promise restoration of arbitrary
application state or history that has been discarded.

The engine calculates the consequences of submitted instructions. **Selected:** exploring whether
choices can complete an entire action, and pruning choices that cannot, are agent concerns the
engine module does not and will not take on. Selected agent rules and candidate strategies belong in
[DECISION_GRAPH.md](DECISION_GRAPH.md).

## How much should the engine determine for its caller?

**Selected destination, October 24, 2026; transition not implemented.** The review has reached a
working division of responsibility. Treat the rules below as the destination for experiments,
not a claim that today's APIs satisfy them or that every implementation detail is settled.
When an experiment exposes a cost or contradiction, record it explicitly; do not quietly weaken
the rule or add machinery to make the experiment appear successful.
[TASK_HANDLING.md](TASK_HANDLING.md) inventories current behavior.
[ENGINE_TRANSITION_PLAN.md](ENGINE_TRANSITION_PLAN.md) orders the experiments and their evidence.

Preserving valid state and interpreting submitted Pets are both engine responsibilities. Their
separation in discussion never meant either was optional. The earlier suggestion that everything
beyond preventing broken state must leave the engine is too restrictive: accepting a sufficiently
specific request is also part of interpreting it. Conversely, improving the choices displayed to
a Player does not make that improvement an engine obligation.

### Execute the client's intended request

The engine serves a client that knows what it wants to do: pay an amount, play a card, or place a
tile at a location. Pending tasks express permission. Calling a task an obligation adds no
separate concept: the client need not execute it, but the game cannot advance past the pending
work. Task consumption, assignment, priority, and continuations still retain their meaning.

There are two different questions, with deliberately different amounts of required analysis:

1. **Which task permits the request?** A read-only matcher may compare the request and task
   descriptions without attempting their consequences. If several tasks match, they must be
   interchangeable under a conservative comparison, or the caller must identify the intended
   task. The exact comparison remains to be specified. Different continuations, assignments,
   priorities, or other meaningful retained task data cannot be ignored merely because the
   immediate instructions look alike. Do not execute possible tasks to select the successful one.
2. **Within that task, is the request specific enough?** Combine the task's restrictions with the
   client's input. Accept execution when exactly a single concrete narrowing can successfully
   complete the requested transaction. The engine may examine everything within that transaction
   to establish uniqueness, including dependencies, limits, and immediate effects. This analysis
   must not become a way to choose between the different tasks in the first question.

An explicit task ID remains a useful way to identify permission, but requiring IDs for most
commands is no longer the intended API principle. Exact signatures are open. Functional scenarios
use the agent layer; direct clients must also have an intelligible engine API.

For example, permission to place an ocean on a water area combined with a request to place a tile
at `Tharsis_5_5` may identify the full change. For `CapitalTile<MarsArea>`, the request
`Tile<Tharsis_6_7>` need not repeat CapitalTile. If a visible hand contains a single spade,
“remove the spade” already identifies an existing card. The earlier contrast with adding a card
remains useful for understanding what must be established, but is not a rule forbidding gain
inference. A broad CityTile request can be sufficient if CapitalTile cannot complete this
transaction and NormalCityTile is the sole surviving possibility. None of these examples assumes
that other unspecified fields magically disappear; every required choice must be accounted for.

Do not warp the Pets class model or introduce artificial types merely to make the client spell
more concrete names. The model describes possible states; execution order and convenience should
not dictate an inferior hierarchy.

### Exactly which consequences count

For this uniqueness question, include the requested instruction, required parts, dependent
removals, recursive `::` effects, final invariant checks, and admission of resulting tasks and the
current task's continuation. Restore every unsuccessful or exploratory attempt before considering
another candidate. Successful exploration must not leave events, tasks, or selections behind.

**Do not execute the newly queued tasks to decide whether the request succeeds.** This includes
single-colon work assigned to Admin. Admin's priority does not make its subsequent transaction
part of the transaction that created it. The normal Agent call can run that work afterward; a raw
engine call need not do so. A later failure can therefore still occur. Determining whether all
later work can finish belongs to the agent.

A direct mandatory `: Die` is included: admitting that task already fails in the current
transaction. No `::` conversion is required for this case. In contrast, an ordinary Admin task
whose later execution would produce `Die` is outside this check. Canon's
`UseAction<FundAwardAction> IF 3 Award: Die` is the direct case when UseAction executes; a command
that merely enqueues UseAction is an earlier transaction. Protected Habitats similarly rejects
another Player's protected removal while its queued effect is being admitted.

This may require Canon changes where a restriction is currently enforced too late. Audit the
actual rejection point and the game rule. Use `::` when that consequence must determine acceptance
of the initiating change, not merely to make an abstract request more convenient. Conversely,
some existing `::` work may be ordinary high-priority Admin follow-up. Both directions need actual
Canon examples. See [the effect review](SEQUENCING.md#investigating-inferred-immediate-work).

### Establishing uniqueness without confusing it with preference

Start with type intersection. If it leaves ambiguity, use sound dependency and limit checks, then
complete transaction attempts where needed. This is a preferred order of work, not a required new
sequence of processing stages. The order of dependency and invariant checks is not settled.
A determined instruction must still execute and pass all its checks; early uniqueness does not
exempt it from validation. A temporary limit violation repaired by `::` is not a sound reason to
reject a candidate before those effects run.

Finding a successful candidate is insufficient: every other distinct narrowing must be excluded.
Finding two successful narrowings proves ambiguity, even if the agent might later consider their
outcomes equivalent. Outcome comparison is not a new engine disambiguation rule. If no candidate
succeeds, reject; if several succeed, require more information. Never choose by task order or by
which attempt happened to succeed first.

The engine's responsibility is defined by the requested transaction, not by an arbitrary list of
cheap checks. Nevertheless, “go to any length” does not provide a terminating algorithm for an
unbounded domain. Finite candidate generation, resource limits, and how to report an unfinished
analysis remain implementation questions. An unfinished search, missing information, or a resource
failure cannot count as proof of uniqueness or impossibility. Distinguish these from actual
game-rule rejection. Measure the hypothesis that expensive analysis is uncommon.

### Generalized auto-narrowing — items 1, 2, 7, 8, and 16

The purpose of the call now determines ownership. Unique-subtype inference, present-dependency
inference, and target feasibility can help the engine answer an explicit execution request under
the rules above. Do not move them wholesale into the agent merely because they infer information.
Do not retain separate special cases where the same transaction rule can replace them.

The agent owns improving an unanswered question without an execution request: narrowing a menu,
combining partial deductions, and deciding whether to execute on the client's behalf. Partial
narrowing is useful even if it does not immediately determine the whole instruction: a breakfast
menu is a more accurate question when only breakfast is served. Several such reductions can also
combine to determine the full change. Preserve every meaningful remaining choice.

**Still open:** whether and how the engine should retain voluntarily supplied partial narrowing.
A target choice can unlock evaluation of its AMAP amount as well as further narrowing. Client
`TaskForm` drafts are useful but are not an approved universal replacement for this capability.
Retaining supplied information is distinct from automatically executing it.

### Discovering impossibility versus simplifying known results — items 5 and 6

Keep two investigations separate: establishing that an instruction is impossible or does no work,
and simplifying a containing instruction once a `Die` or `Ok` result is known. Normalizing those
conclusions to `Die`/`Ok` is a possible representation, not a universal rewrite rule.

Attempting to execute mandatory `Die` must fail. The intended design also refuses admission of a
direct mandatory `Die`: normal in-game instructions cannot delete a task, so admission already
guarantees a stuck game. Correction APIs are not an in-game escape. This is a deliberately local
deduction, not permission to explore future tasks for dead ends.

The exact simplification rules for an unchosen OR arm, a gate, optional/AMAP work, or a later THEN
stage still need investigation. “Cannot execute now” does not mean “can never execute”: another
task may supply resources. Do not turn the direct-Die rule into general future-completion analysis.

### Selection must not quietly mean execution — item 9

**Destination for experiments:** a selection or narrowing call does not authorize task
execution. Becoming fully specified is not an execute command. An agent may separately request
execution according to its policy, and an explicitly combined execute API can perform its needed
selection internally. Keep these distinguishable in tests and in the public contract.

Start from no automatic execution and investigate every path in the
[automatic-actions inventory](TASK_HANDLING.md#automatic-actions-without-a-separate-execute-command),
including zero/Ok completion, EACH expansion, dispatch, reassignment, continuation admission, and
promotion. Whether enqueue-only structural work needs an exception remains open. The earlier
[pure-dispatch rule](SEQUENCING.md#decisions-and-open-questions) is current behavior with a reopened
interaction question, not an exception silently granted to selection.

### Task identity, optional queries, and conveniences — items 10, 13–15, and 17

Task finding follows the conservative comparison above. Explicit `canSelectTask`, `canExecuteTask`,
and `tryTask` services are provisionally acceptable; their contracts must say what they attempt
and retain. Trials within the identified execution request are now justified by its uniqueness
rule. Trials of unrelated tasks, or hidden execution during selection, need a different reason.
In particular, enforcing Admin priority must not turn task finding into consequence-based choice.

Quantity-limit reads remain useful: otherwise options may be effectively infinite. A candidate
bound is not a promise that every candidate's transaction succeeds or that an action can finish.

Combining separate resource tasks into a net total is still a candidate for test support only,
not an ordinary engine or agent obligation. Investigate removing extra queued execution from
`executeSubmittedGroup`; preserve explicit execution of submitted work without also consuming its
reactions or continuation. The [verified example](TASK_HANDLING.md#extra-execution-requested-by-executesubmittedgroup)
shows the issue. Neither convenience is required by accepting an abstract request within a task.

### Shared bindings and immediate effects — items 4 and 12

[SEQUENCING.md](SEQUENCING.md#reviewed-client-view-of-then) owns the joined/split THEN destination:
shared type/X bindings are explicit; afterward the current instruction and continuation are
separate. Narrowing the current instruction need not repeat the continuation. Do not reverse-engineer
X from `3 Foo` submitted against `X Foo THEN X Bar`, in either module. `3X Foo` makes the problem
particularly clear. Transaction uniqueness does not reopen that rejected input convention.

The same note owns [immediate-effect investigation](SEQUENCING.md#investigating-inferred-immediate-work).
NextPlayer links must be created as Players are created, without relying on incidental queued order.

### Responsibility spectrum

| Behavior | Intended owner and limit |
| --- | --- |
| Compare a request with pending task descriptions. | Engine read query; no consequence search to pick a task. |
| Combine the identified task with submitted type/location/amount information. | Engine interpretation of the request. |
| Exclude alternatives using dependencies, limits, or full requested transactions. | Engine, when necessary to establish unique execution. |
| Reject direct mandatory Die admission and simplify known results. | Engine; exact nested simplifications still need review. |
| Display a smaller unanswered choice or combine partial deductions. | Agent policy; retained voluntary engine narrowing remains open. |
| Automatically submit an execution without a new client execute command. | Agent policy; selection itself supplies no such permission. |
| Run queued Admin work after the initiating transaction. | Mandatory Admin handling in the agent; separate transactions. |
| Explore later tasks, prove action completion, or collapse equivalent outcomes. | Agent only. |

### Currently rejected alternatives

- Requiring fully concrete input even when the request and permission together identify the sole
  transaction that can succeed.
- Moving every inference to the agent, including deductions necessary to interpret an explicit
  execution request; demanding task IDs as the guiding principle of most mutations.
- Choosing among matching tasks by executing their consequences, or counting later Admin success
  as part of the initiating transaction's uniqueness test.
- Treating a successful candidate, an incomplete search, or equal outcomes as uniqueness.
- Letting a selection silently authorize execution; reconstructing shared X backward from a result.
- Changing the Pets hierarchy to conceal inconvenient execution requirements, or adding a general
  operation framework to infer whole-action completion inside the engine.

These supersede the contradictory early inventory directions. Preserve the observations and
examples above, not an earlier imperative to implement the rejected alternatives.

## Clients and transactions

Clients express the change they want; they need not repeat information already established by
the permitting task and transaction checks. The engine interprets and executes that request.
Admin is also an Actor. Agent code submits its work automatically through separate engine calls.

**Selected:** Admin always uses the eager handling described below. P1 priority for Admin tasks,
versus P2 for Player tasks, explains most scheduling differences. Other policies are conceivable
because Admin is an Actor too, but exploring or offering them has no value for this design.
Any discussion about “turning off autoexec” and configurable autoexecution levels always refers to
Players; Admin remains fully automatic, including during replay.

| Term | Meaning |
| --- | --- |
| **Task** | An instruction waiting to be handled by its assigned Actor. |
| **Transaction** | A client-initiated state change and the immediate consequences needed to make the world self-consistent again. |

Describe resolution and concreteness separately. Even an instruction that is both resolved against
the current World and concrete can fail during execution or final checks. These properties say
nothing about completion of a whole game action, a queued continuation, or tasks execution may create.

[TASK_HANDLING.md](TASK_HANDLING.md) inventories the current automatic revisions and handlings,
including their timing and differences from this contract.

## Inside a transaction

Each transaction begins with a potentially state-changing call into the engine from a client acting
for an Actor. It includes the requested change, task additions/edits/removals, dependent removals,
recursive double-colon (`::`) effects, and checks of temporarily suspended invariants. If execution
or final checks fail, the engine restores the entire transaction before returning the failure.
**Selected:** a public engine mutation must not return with an inseparable change partly applied.
Clients need not add a rollback wrapper to obtain that guarantee.

For example, placing a tile on your
[Land Claim](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/board.pets) removes your
existing cube through `::`. Cube and tile may coexist during that work; they must not both remain
when final checks run.

No competing client command may change the world during this work. The transaction's own
consequences must run, including task creation and the cube's removal. Resulting Admin tasks are
enqueued now and handled in subsequent transactions.

## Agent calls: running to quiescence and returning results

**Selected scope:** the synchronous Agent API submits the requested engine transaction, then
continues eligible work until no further automatic work can proceed without input (quiescence).
The raw engine transaction returns after its own consequences and task admission; it does not
run subsequent Admin transactions. The Agent is responsible for making those later calls. Each
Admin task is performed through a subsequent transaction, with the same rules as a Player's.
This is the intended division of responsibility; some surrounding-call machinery still lives in
the engine package today. Its location is part of the transition, not a new responsibility for
raw engine requests. No separate name or object for the enclosing call is required.

Quiescence means the mandatory Admin driver and configured Player drivers have no further work
they can perform without more client input. It does not mean every task is gone or the game action
is complete. No eligible Admin task may remain: a full pass without Admin progress is an exception,
not successful quiescence. Deferred work can become eligible during this progression and must be
considered before returning, using the promotion rules below.

For example, using AI Central starts a Player transaction, followed by Admin transactions handling
`UseAction` and its bookkeeping. With manual Player choices, the call can then return with the draw
task pending. Similarly, if Stan places greenery beside Kim's tile while Kim has
[Philares](../../test/common/dev/martianzoo/tfm/tests/cards/PhilaresTest.kt), Kim's resource choice can
remain pending when Stan's call returns.

A small task like `Pass` could potentially trigger phase completion, production, and a generation
change before the call returns. Those events were triggered by the command; they are not all
fulfillment of the original task.
Individual transactions remain smaller than the synchronous call that runs this progression.

**General result rule:** a returned `TaskResult` should cover everything that happened during that
call, including the initiating transaction, subsequent Admin work, and any other work performed
before return. Work performed by a completion callback is included too. Changes that were undone
before return are not reported as retained changes. With the current fields, `changes` includes
component changes in event order across that entire interval, and `tasksSpawned` includes tasks
added during it that remain pending at return. The event log retains the detailed event history.
A UI can present those intermediate events without requiring a separate API return for each event.

For example, if a greenery placement triggers an Admin-handled steel bonus before returning,
the result must include both the placement and the steel gain. Capturing the result immediately
after placement and omitting the subsequent bonus would violate this rule. This reporting rule
says how much the result covers; it does not by itself make the whole call failure-atomic.

## Which task acts next

Lower numbers take precedence.

| Priority | Work |
| --- | --- |
| P0 | The selected task, for any Actor |
| P1 | Ordinary Admin tasks |
| P2 | Ordinary Player tasks |
| P3 | `LATER` tasks |
| P4 | `FINALLY` tasks |

A Player cannot select a P2 task while executable P1 work remains. A selected task excludes competing client
mutations until it finishes. Admin tasks its execution creates wait until that transaction finishes.
The synchronous Agent call returns only after all currently executable Admin work finishes;
waiting and deferred Admin tasks may remain. This also rules out a callback inviting another
Player command while executable P1 work remains.

**Human clarification:** retrying an earlier failed task after later tasks succeed is existing
behavior, as when negative victory points wait for positive scoring. It does not authorize
silently returning after a completely unsuccessful pass. The current gate and Player-task
exceptions below are implementation limitations, not requirements to preserve in a model redesign.

**Open after the October 24 review:** client-held drafts are useful, but do not yet remove
engine-level partial narrowing. A voluntary narrowing can enable more deductions or evaluations;
its value depends on the engine's selected responsibilities. Committing an unfinished selection
currently excludes other task selections, which must also be considered. No transaction should
stay open while a person thinks. See [generalized auto-narrowing](#generalized-auto-narrowing--items-1-2-7-8-and-16).

When P0–P2 are empty, promote **all existing P3 tasks together**: Admin tasks become P1 and Player
tasks become P2. New `LATER` work waits for a subsequent promotion.
[Solar Probe](../../test/common/dev/martianzoo/tfm/tests/cards/SolarProbeTest.kt) shows why: its own
Science tag counts toward its draw; its deferred flip waits until the draw and other ordinary
work finish.

Promotion is checked only when a transaction completes, not while its queue is temporarily empty:
`Transactions` performs it at the end of a successful transaction, so the promoting task edits
belong to the transaction that made the work eligible. **Selected:** when P0–P3 are empty,
promote all P4 tasks together using the same Actor-based priorities.

## Admin's choices and retries

**Selected:** Admin handles eligible tasks in FIFO order. If a task cannot currently execute, move
it to the end and try later tasks. Each success starts a fresh pass. After a full pass with no
success, throw the retained rejection. Deferred P3/P4 tasks do not count until promoted.
The current exceptions for unresolved work and tasks waiting on Player work are listed below.
A successful return still does not certify completion; pending work and `MustCleanUp` are checked
at the applicable completion check.

Before executing an Admin task, establish **exactly a single fully concrete legal instruction**
that narrows it, using the engine's transaction-success rule. The agent chooses which eligible Admin
task to attempt; the engine establishes the requested task's unique successful narrowing. Complete
analysis remains unimplemented; explicit enumeration of every candidate is not the only permitted
proof technique. A failed attempt must not retain tentative selection or partial changes.

The intended meaning of **legal narrowing** is that submitting it can complete its entire transaction
successfully, including `::` consequences and final invariant checks. This excludes checking
additional spawned tasks that execute in later transactions. How to determine the complete set of
such concrete narrowings remains open.

“Fully concrete” describes the current instruction. Admin can execute the concrete System signal
[`PlaceGreeneryTile`](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/board.pets);
its consequence still asks a Player which hex receives the greenery.

An unsuccessful transaction must leave no changes behind before Admin tries another task.
Distinguish game-rule rejection, such as insufficient resources, a count limit, or `Die`, from an
unexpected implementation error. Game-rule rejection uses Martian Zoo exception types; Kotlin
implementation errors can use Kotlin's exception types. A rejection is retained while later tasks
are tried and thrown if the pass cannot progress, subject to the current exceptions below.
Implementation errors propagate immediately after restoration. A generic domain exception is not
sufficient evidence of gameplay rejection: malformed Pets, custom-code failures, and implementation
resource limits must also propagate.

## What belongs to Admin

System-component work is reassigned to Admin. **Recommendation for timing:** retain required Player
choices until the change is concrete, then assign it to Admin. Also assign concrete changes explicitly
performed `BY Admin` to Admin, including non-System changes.

For [World Government Advisor](../../test/common/dev/martianzoo/tfm/tests/cards/WorldGovernmentAdvisorTest.kt),
the Player chooses the neutral ocean's hex although Admin performs the placement. Admin's uniqueness
rule concerns tasks currently assigned to it, not everything ultimately performed `BY Admin`.

Which types are System is a design choice. The usual reason is that they represent machinery the
Player has little reason to notice, rather than tangible game concepts. Precisely distinguishing this
from being non-`Iconic` is deferred; today's type classifications do not settle that discussion.

## Failure and recovery

**Selected:** public engine mutations guarantee failure atomicity for their transaction: failure
restores its starting game state, including components, tasks, derived effects, and event history.
An implementation can make intermediate changes internally, but cannot expose them as the result
of a failed call. Safe client-controlled rollback remains useful for larger groups of calls;
it is not a prerequisite for recovering a failed engine transaction.

For example, removing an opponent's plant protected by Protected Habitats can encounter `Die`
after the internal removal. The failed engine call must restore the plant and the rest of that
transaction. **Current-code evidence:** the
[functional test](../../test/common/dev/martianzoo/engine/ActorEngineTransactionTest.kt) invokes the
raw engine mutation without an enclosing restoration block and verifies the plant and pending task
remain intact. A second test checks restoration of an enclosing batch. Arbitrary application side
effects remain outside the engine's state guarantee.

**Recommendation, still separate from transaction atomicity:** restore the enclosing synchronous
call on failure, including its completed Admin transactions. This restores more than the last failed
transaction, preventing a rejected Player command from leaving earlier changes behind. The call can
also include subsequent game progression; deciding its recovery scope does not make all that work
fulfillment of the initiating command. Reconsider the scope if it requires disproportionate machinery.

**Current-code evidence:** asking for AI Central's nonexistent second action reaches the System
`UseAction` task, whose consequence is `Die`. The existing
[Canon card test](../../test/common/dev/martianzoo/tfm/tests/cards/AiCentralTest.kt) expects rejection,
then successfully uses its real action. `Die` itself is not System; a direct mandatory `Die`
throws when the task would be created, rather than waiting in the queue. Admin restores its failed
attempt, tries other work, and propagates the `DeadEndException` if no progress is possible.
The enclosing Agent call restores the rejected action.

Still to decide, separately from the recommendations above:

- Is failure during Admin handling an ordinary rejected Player choice, a broken model, or either
  depending on the cause? Recovery scope alone does not answer that.
- Can a later failure require undoing an earlier call that already returned successfully?
  The AI Central example does not establish that need.

## Current implementation differences

These are observations to address during transition, not reasons to preserve the behavior.

- The engine has several specific inference and filtering rules, not the general proof of a unique
  successful transaction described above. Its current task matching and narrowing paths need to be
  separated and checked against their different intended responsibilities.
- Selection and narrowing can execute when they reach concrete work; exposed groups and no-ops can
  also complete a task. `executeSubmittedGroup` can execute newly queued reactions or continuation
  work. These paths are inventoried in [TASK_HANDLING.md](TASK_HANDLING.md).
- Public engine mutations restore their transaction on an `Exception`; non-`Exception` failures
  and arbitrary application side effects are outside that guarantee.
- [WorldTransaction](../../src/common/dev/martianzoo/engine/WorldTransaction.kt) ordinarily restores
  the initiating command and its Admin handling together on failure. Returned results cover the
  call's retained interval, including completion-callback work and subsequent handling. A failing
  callback still leaves the initial work in place.
- Agent decisions and queue edits handle Admin work before and after mutation, including nested
  calls. Initial task batches and bootstrap also give executable Admin work priority. Corrections
  deliberately omit queued task execution.
- The raw Player priority check uses the existing selection/execution probe, not complete legal
  candidate enumeration. It applies before a transaction changes state; work generated during a
  compound command waits until that transaction finishes. A selected task retains P0 priority.
- Admin attempts ordinary work in FIFO order and retries waiting tasks after progress. An explicit
  unmet gate may wait while ordinary Player work remains, allowing the Player to finish payment.
  Abstract work and missing external inputs also remain pending. Other unavailable concrete work
  can wait while Player work remains, as with shared research payment capacity. A retained rule
  rejection is thrown after a pass with no progress. This is not the proposed complete uniqueness
  and stalled-pass rule; those remain transition work. Transaction recording and checkpoint
  migration are completed and do not depend on this remaining analysis.
- Selection/execution probes restore their attempts after transaction completion, including deferred
  promotion. Only gameplay rejection and unresolved instructions become negative results; other
  failures propagate. A selection that hands off work does not execute the receiving Actor's task.
- Admin configuration accepts only EAGER. Player policies alone are configurable and disabled
  during replay; Admin execution is omitted from replay inputs and repeated by its mandatory driver.
- Queue edits and corrections can change the world while another task is selected, bypassing
  the proposed exclusion of competing client mutations. Partial selection can still persist.
