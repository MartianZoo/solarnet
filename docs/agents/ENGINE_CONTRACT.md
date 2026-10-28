# Engine contract

> **Selected destination — revised October 28, 2026; transition remains experimental.** The responsibility
> rules below are intended behavior, not a claim that the current implementation satisfies them.
> **Selected** records agreed direction; remaining choices are explicitly labeled.
> **Recommendation** identifies additional suggestions. Current-code observations are evidence, not approval of that behavior.
> **Investigation direction** records an experiment to try, not a completed behavior change.
> **Open** and **working presumption** retain questions and preferences that are not settled rules.
> Keep interaction terms here, not in the glossary.

Maintain this selected design in work1 and carry updates into the experiment branch.
Implementation-status observations refer to `codex/engine-transition-experiments` at `6c4686b61`
unless they explicitly say otherwise; work1 and `main` do not yet contain that code. Record
branch-specific evidence in
[ENGINE_TRANSITION_RESULTS.md](ENGINE_TRANSITION_RESULTS.md).

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

**This project's finish line is a clean, simplified engine API.** An Agent must take over behavior
removed from the engine when clients still need that behavior. Implementing smarter Player menus,
whole-action completion search, outcome equivalence, or general Player autoexecution is a separate
project. The engine calculates submitted instructions and their immediate consequences; the later
Agent design is recorded in [DECISION_GRAPH.md](DECISION_GRAPH.md), not in this project's acceptance
criteria.
[AGENT_CONTRACT.md](AGENT_CONTRACT.md) owns the broader Agent responsibility inventory and the
separate direction for standing autoexec policies installed in engine by an Agent.

## How much should the engine determine for its caller?

**Selected destination, October 24, 2026; transition incomplete.** The review has reached a
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
   task. Different continuations are not interchangeable. Equal immediate instructions alone are
   insufficient. Whether other data such as cause distinguishes otherwise matching tasks remains
   open; current code ignores ID and cause, even though cause can affect recorded ancestry and
   application input checks. Do not execute possible tasks to select the successful one. The
   exact positive test for interchangeability remains to be specified.
2. **Within that task, is the request specific enough?** Combine the task's restrictions with the
   client's input. Accept execution when exactly a single concrete narrowing can successfully
   complete the requested transaction. The engine may examine everything within that transaction
   to establish uniqueness, including dependencies, limits, and immediate effects. This analysis
   must not become a way to choose between the different tasks in the first question.

**Working proposal from the owner:** if a submitted task string intersects exactly a single
pending task, execute as though the intersection itself had been supplied. Intersecting an
instruction and finding its sole concrete successful transaction are different steps. A client
may also identify a task by ID. A task string may express a calculated change. The owner's
illustrative wording was `1 MC / Foo` with 3 Foo present, followed by `3 Foo`; in Pets, that
expression evaluates to **3 MC**. The analogous task yielding `3 Foo` would be `Foo / MC` with
3 MC present. This calculated-amount convenience is valuable to sandbox tests and task-log import.
Whether it belongs in the final engine matcher, and how to handle several tasks resolving to the
same change, remain open. Exact API signatures remain open.

**Current-code evidence:** `ActorEngine.matchingTask` already tries intersection with a resolved
pending instruction when direct intersection fails. `TaskResolutionTest` executes a pending
`Plant / Heat` task through Agent by submitting its calculated `2 Plant` change. The engine
fallback is visible in source, but that test does not isolate a raw engine call. Neither the code
nor the test decides that resolved-value matching belongs in the final contract or establishes the
multiple-task rule.

For example, permission to place an ocean on a water area combined with a request to place
`Tile<Tharsis_5_5>` may identify the full change. The request names a tile at an existing map
area; naming `Tharsis_5_5` alone could look like creating the area. For a schematic
`CapitalTile<MarsArea>` task, `Tile<Tharsis_6_7>` illustrates a typed request without repeating
CapitalTile; it is not a claim that Canon permits Capital on that water area. If a visible hand
contains a single spade, “remove the spade” already identifies an existing card. The earlier
contrast with adding a card remains useful for understanding what must be established, but is not a rule forbidding gain
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
part of the transaction that created it. The client-facing call runs eligible Admin work afterward;
the lower-level transaction does not. A later failure can therefore still occur. Determining
whether all later work can finish is outside this engine API project.

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
unbounded domain. **No wall-clock timeout is selected.** Search must not make the same request
succeed or fail according to machine speed or incidental iteration order. Finite candidate
generation, deterministic limits or other bounded proof methods, and the public result of an
unfinished analysis remain open. An unfinished search, missing information, or resource failure
cannot count as proof of uniqueness or impossibility. Distinguish these from actual game-rule
rejection. Measure the hypothesis that expensive analysis is uncommon.

### Engine inference and Agent carryover — items 1, 2, 7, 8, and 16

The purpose of the call now determines ownership. Unique-subtype inference, present-dependency
inference, and target feasibility can help the engine answer an explicit execution request under
the rules above. Do not move them wholesale into the agent merely because they infer information.
Do not retain separate special cases where the same transaction rule can replace them.

The Agent owns any retained convenience that improves an unanswered question without an execution
request: narrowing a menu, combining partial deductions, and deciding whether to execute on the
client's behalf. Implementing general menu reduction is outside this engine simplification
project. Preserve an existing useful capability if moving it out of the engine would otherwise
break current clients; record a separate Agent improvement rather than leaving a duplicate engine
policy behind.

**Still open for this project:** whether and how the engine should retain voluntarily supplied partial narrowing.
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

**Selected:** a Player's selection or narrowing call does not itself request that Player's task
execution. Becoming fully specified is not the Player's execute command. If narrowing makes a
System change concrete and hands it to Admin, the public call may then execute it as Admin's next
automatic transaction. That can make narrowing appear to execute from the Player's perspective;
the task's assignee and the recorded Actor distinguish the operations. An explicitly combined
execute API can perform its needed selection internally. Keep these cases distinct in tests.
An Agent may separately authorize execution in advance through an
[installed autoexec policy](AGENT_CONTRACT.md#4-configure-autoexec-policies-in-the-engine).
The current selection experiments assume no applicable installed policy; selection alone grants
no execution permission.

**Selected meaning of `EACH` execution:** executing an `EACH` task enqueues all its child tasks
and consumes the parent. That queue change is the parent task's execution, just as gaining or
removing components is the execution of a gain or removal task. The children then follow ordinary
priority; the parent does not retain P0 until all children finish. Therefore a Player's selection
or narrowing call must not expand an `EACH` task without an execution request or a previously
installed policy authorizing that execution. The experimental branch currently expands exposed
`EACH` tasks during selection, so this part of the selected contract is not implemented there.
Canon's base `SetupPhase: EACH Me@Player { PROD[MC, Steel, Titanium, Plant, Energy, Heat] }`
illustrates the task shape. Other structural forms still need their own classification.

Start from no automatic execution *by the selecting Actor* and investigate every path in the
[automatic-actions inventory](TASK_HANDLING.md#automatic-actions-without-a-separate-execute-command),
including zero/Ok completion, group expansion other than `EACH`, dispatch, reassignment,
continuation admission, and promotion. Classify those forms individually; the `EACH` rule above
is settled, while other enqueue-only structural work remains open. The earlier
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

Combining separate resource tasks into a net total belongs in Agent, not engine. The user selected
restoring this convenience for production: a submitted 21 MC can execute 6 MC of production,
20 MC from Terraform Rating, and the -5 MC offset as separate engine calls. Tests should keep
amounts from unrelated rewards separate, such as Investment Loan's 10 MC and Media Group's 3 MC.
This is a caller-writing preference, not game-specific recognition inside Agent. Agent preserves
each task's effects and continuation; it does not replace them with a single net component change.
Investigate removing extra queued execution from
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
| Preserve an existing smaller unanswered choice or combined deduction after removing its engine policy. | Agent carryover when required; new general Player guidance is outside this project. |
| Automatically submit an execution without a new client execute command. | Agent policy, applied during the call or installed in engine in advance; selection itself supplies no such permission. |
| Supply and select automatic behaviors, including mandatory Admin behavior. | Attached Agents; exact registration interface remains open. |
| Invoke attached automatic behaviors until quiescent after the initiating transaction. | Public call coordinator; whether this lives in engine or a thin facade remains open. |
| Explore later tasks, prove action completion, or collapse equivalent outcomes. | Separate Agent project. |

### Currently rejected alternatives

- Requiring fully concrete input even when the request and permission together identify the sole
  transaction that can succeed.
- Moving every inference to the agent, including deductions necessary to interpret an explicit
  execution request; demanding task IDs as the guiding principle of most mutations.
- Choosing among matching tasks by executing their consequences, or counting later Admin success
  as part of the initiating transaction's uniqueness test.
- Treating a successful candidate, an incomplete search, or equal outcomes as uniqueness.
- Treating a Player selection as that Player's execution; reconstructing shared X backward from a result.
- Changing the Pets hierarchy to conceal inconvenient execution requirements, or adding a general
  operation framework to infer whole-action completion inside the engine.

These supersede the contradictory early inventory directions. Preserve the observations and
examples above, not an earlier imperative to implement the rejected alternatives.

## Clients and transactions

Clients express the change they want; they need not repeat information already established by
the permitting task and transaction checks. The engine interprets and executes that request.
Admin is also an Actor. The client-facing operation must coordinate the requested engine work and
the automatic work supplied for all attached Actors. A lower-level operation that performs zero or
one transaction may be internal; its present public visibility is no design requirement.

**Selected:** Admin always uses the eager handling described below. P1 priority for Admin tasks,
versus P2 for Player tasks, explains most scheduling differences. Other policies are conceivable
because Admin is an Actor too, but exploring or offering them has no value for this design.
Any discussion about “turning off autoexec” and configurable autoexecution levels always refers to
Players; Admin remains fully automatic, including during replay.

| Term | Meaning |
| --- | --- |
| **Task** | An instruction waiting to be handled by its assigned Actor. |
| **Transaction** | An Actor-attributed command and the immediate consequences needed to make the world self-consistent again, whether submitted by a client or automatic handling. |
| **Client-facing call** | The requested transaction, if any, plus automatic follow-up transactions run before returning. |

Describe resolution and concreteness separately. Even an instruction that is both resolved against
the current World and concrete can fail during execution or final checks. These properties say
nothing about completion of a whole game action, a queued continuation, or tasks execution may create.

[TASK_HANDLING.md](TASK_HANDLING.md) inventories the current automatic revisions and handlings,
including their timing and differences from this contract.

## Inside a transaction

Each transaction begins with a potentially state-changing engine command for an Actor. It includes
the requested change, task additions/edits/removals, dependent removals,
recursive double-colon (`::`) effects, and checks of temporarily suspended invariants. If execution
or final checks fail, the engine restores the entire transaction before returning the failure.
**Selected:** an engine transaction must not return with an inseparable change partly applied.
Clients need not add a rollback wrapper to obtain that guarantee.

For example, placing a tile on your
[Land Claim](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/board.pets) removes your
existing cube through `::`. Cube and tile may coexist during that work; they must not both remain
when final checks run.

No competing client command may change the world during this work. The transaction's own
consequences must run, including task creation and the cube's removal. Resulting Admin tasks are
enqueued now and handled in subsequent transactions.

## Client-facing calls: running automatic work and returning results

**Owner's current API direction:** a client invokes a public engine-facing method. Its lower-level
part performs zero or one engine transaction. Before the public method returns, the call gives all
Agents attached to that call a chance to run automatic behavior, starting with Admin and continuing until
quiescence. Each automatic command executes as a separate engine transaction. The lower-level
method need not be public to ordinary clients. Whether the public method is packaged on Engine,
Agent, or a thin coordinator remains open; name and module placement must follow the simplest
coherent implementation rather than preserving current method locations.

Current `Agents` is a session that supplies an Agent for each Actor; independent sessions can share
a World while holding different application checks. “Attached” needs an exact rule for which
session's Agents and callbacks a call uses. It must not mean that every independent session over
the same World is implicitly invoked.

Agents own which automatic behaviors they provide. **Design hypothesis to validate:** an Agent
could provide any number of small autoexecutors; the public call coordinates their invocation.
Today's `Agents` session owns a single `AutoExecLoop` shared across Actors; individual Agents
register policies, not individual autoexecutors. **Owner clarification, October 28:** the shared
loop is acceptable, and Admin must have priority before every automatic Player step. Splitting
that loop is not a requirement of this engine API project. Before a Player sees a state or selects
a task, Admin must do everything it can currently complete. A pending Admin task that cannot yet
progress does not by itself prohibit Player selection; Player work may be needed to move the game
forward. Once selected, the P0 task must execute before any other work. This does
not authorize adding a general callback framework in anticipation. The historical
[SMART_AUTOEXEC.md](obsolete/SMART_AUTOEXEC.md) recorded
composable policies, but it is archaeology, not a current implementation contract.
The separate Agent design also selects standing policies that an Agent installs in engine for
covered selected tasks. Their matching, lifetime, replay, and failure rules remain open; implementing
those policies is not part of this engine API transition. This public-call design must leave room
for them without treating later policy-driven work as proof that the initiating transaction succeeds.

**Current-code and module constraint:** public `ActorEngine.transact` already accepts `afterChanges`
and `validateCompletion` callbacks; `World.onTransactionComplete` is a separate notification hook.
The `:agent` module depends on `:engine`, and Kotlin `internal` does not cross that module split.
Putting the public call in Agent would leave some engine operation public to Agent; putting it in
engine would require Agent behavior to arrive through engine-defined callbacks. The final design
must decide the fate of both current callback routes, rather than assume a fresh coordinator.
No production code currently assigns `World.onTransactionComplete`; tests use it to characterize
notification and failure timing. Deleting it is a concrete simplification candidate, not a
selected requirement.

Admin always has mandatory automatic behavior. Configuring a Player's policy or replaying Player
inputs cannot disable Admin. Before each automatic Player action when no task is selected, the
coordinator tries eligible Admin work again, including work newly admitted by the preceding Player
transaction. Providing automatic behavior and deciding its policy remain Agent responsibilities
even if its callback executes inside an engine-owned call loop. The coordinator does not invent a
Player choice. Today's configured Player EAGER policy can select a successful option in queue
order; carrying it through this engine API transition is not approval of that separate policy.

Quiescence means the mandatory Admin driver and configured Player drivers have no further work
they can perform without more client input. It does not mean every task is gone or the game action
is complete. Admin must exhaust all work it can currently perform before the Player sees the
resulting state. Waiting or unresolved Admin tasks may remain, and their mere presence does not
prevent Player selection. A full pass with a retained concrete rejection currently throws rather
than returning
success, but the general stalled-pass rule still needs approval. Deferred work can become eligible
during this progression and must be considered before returning, using the promotion rules below.

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

A Player cannot select a P2 task while Admin can still perform P1 work. There is no separate
requirement for Admin's queue to be empty: if Admin has exhausted what it can do and pending work
needs a game-state change from a Player, the Player may select a task. If Admin is waiting for a
required external input under the current game model, surface that need before the Player acts.
This does not require any particular Canon card to keep using such an input. A selected P0 task
must execute before any other task or transaction. An Admin task created separately waits. If the
selected task itself is reassigned to Admin, Admin executes that same P0 task before any other work.
`dropTask(selectedTaskId)` is an external correction request with acknowledged risk, outside the
coherent in-game model. Its existence is not a gameplay exception to selected-task priority.
Admin tasks its execution creates wait until that transaction finishes.
The synchronous client-facing call returns only after all currently executable Admin work finishes;
waiting and deferred Admin tasks may remain. This also rules out a callback inviting another
Player command while executable P1 work remains.

**Human clarification:** retrying an earlier failed task after later tasks succeed is existing
behavior, as when negative victory points wait for positive scoring. It does not authorize
silently returning after a completely unsuccessful pass. Existing gate, unavailable input, and
Player-task waiting cases are described in [the experiment results](ENGINE_TRANSITION_RESULTS.md)
and [the source inventory](TASK_HANDLING.md); their final classification remains open.
The experimental loop can retain unresolved Admin tasks, and its priority check uses a concrete
selection trial. That is implementation evidence, not proof that the driver has exhausted every
Canon task it could perform. Audit actual Canon waiting cases and retained failures against the
selected rule above. Turmoil's current abstract `GlobalEvent` draw is a deck workaround being
replaced in work4. This contract does not require that workaround or derive a general engine rule
from it; preserve the Turmoil replay results when the work4 change is integrated.

**Open after the October 24 review:** client-held drafts are useful, but do not yet remove
engine-level partial narrowing. A voluntary narrowing can enable more deductions or evaluations;
its value depends on the engine's selected responsibilities. Committing an unfinished selection
currently excludes other task selections, which must also be considered. No transaction should
stay open while a person thinks. See [engine inference and Agent carryover](#engine-inference-and-agent-carryover--items-1-2-7-8-and-16).

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
it to the end and try later tasks. Each success starts a fresh pass. Deferred P3/P4 tasks do not
count until promoted. **Open:** after a full pass without progress, which tasks may legitimately
wait for Player input or another transaction, and which retained concrete rejections must throw?
The current exceptions are in [the results](ENGINE_TRANSITION_RESULTS.md) and
[the source inventory](TASK_HANDLING.md); they are not automatically approved as final semantics.
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
are tried. When no task progresses, handle it under the approved stalled-pass rule above; do not
silently treat a concrete rejection as successful quiescence.
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

**Selected:** every engine transaction guarantees failure atomicity: failure
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
The enclosing call currently restores the rejected action.

Still to decide, separately from the recommendations above:

- Is failure during Admin handling an ordinary rejected Player choice, a broken model, or either
  depending on the cause? Recovery scope alone does not answer that.
- Can a later failure require undoing an earlier call that already returned successfully?
  The AI Central example does not establish that need.

## Current implementation status

Snapshot: `codex/engine-transition-experiments` at `6c4686b61`, after merging `main` at
`f1055a08b` on October 28, 2026. The experiment branch remains separate from `main`.
Transaction recording and checkpoints already exist in `main`; the
transition plan does not list them as future work. Detailed evidence and limits are in
[ENGINE_TRANSITION_RESULTS.md](ENGINE_TRANSITION_RESULTS.md). For current entry paths, use
[TASK_HANDLING.md](TASK_HANDLING.md) in the checkout whose code is being examined: work1 describes
main, while the experimental checkout describes its branch.

| Area | Already on the experiment branch | Still needed for the selected destination |
| --- | --- | --- |
| Moving policy out of engine | Application validation is session-owned; operation completion and selected-task retry policy live in Agent; combined resource totals are translated by Agent into individual engine calls. `ActorEngine.transact` already coordinates callbacks, while a shared `AutoExecLoop` handles all Actors in a session. | Decide and simplify the public call coordinator, both callback routes, and any remaining engine-owned caller policy. |
| Selection and execution | Selection/narrowing no longer perform component changes as the selecting Actor; submitted groups execute admitted members only; numeric `THEN` bindings must be explicit. Ordinary add/drop queue edits are refused while another task is selected. | Specify and test structural task edits, Admin handoff, voluntary partial narrowing, correction paths, and the final public API. |
| Identified task execution | Existing task-string matching intersects direct and resolved instructions; fixed mandatory gains, removals, and full-form transmutations try complete finite candidate transactions in disposable Worlds. Shared type pairs and malformed generated changes are filtered before trial. | Decide whether to retain resolved-value matching and finish interchangeability; cover unsupported instruction forms or report analysis honestly; settle deterministic search limits. |
| Admin and priority | Admin is mandatory in Agent calls, retries after progress, and uses the same finite uniqueness rule as the Player priority probe. An ambiguous Admin candidate does not become executable just because some candidate succeeds. | Resolve stalled passes and handoff timing across all relevant Canon cases; integrate Agent-provided automatic work into the chosen public API shape. |
| Recovery and result | Engine mutations restore their transaction on an `Exception`; surrounding calls usually restore the initiating command and Admin handling together. Returned results cover the retained call interval. | Decide callback failure recovery and precisely what the public call promises; arbitrary application side effects remain outside game-state rollback. |

The branch does **not** implement a complete transaction uniqueness proof or the selected final task matcher,
an arbitrary collection of Agent-provided autoexecutors, or the proposed public engine-facing
call shape. A direct raw engine method is still public today; that is current code, not a selected
API requirement. Player choice search in [DECISION_GRAPH.md](DECISION_GRAPH.md) is separate work.
