# Engine interaction contract

This document describes the current implementation in this checkout. It separates engine
transactions from the surrounding Agent call and from whole game actions. Historical experiments,
owner decisions, and verification evidence are retained in
[ENGINE_TRANSITION_RESULTS.md](ENGINE_TRANSITION_RESULTS.md). Possible changes appear only in the
[appendix](#appendix-further-work-under-consideration).

## Responsibility and scope

The engine interprets submitted instructions, enforces the modeled rules, and maintains reversible
game state. Agent supplies the client-facing calls, recording, application checks, mandatory Admin
processing, and configured Player automatic behavior. The engine does not authenticate callers;
[ADVERSARIAL.md](ADVERSARIAL.md) describes the surrounding application's responsibilities.

Admin has no client to supply choices. The required automatic behavior is the work that supported
Canon rules actually assign to Admin. Setup supplies information that rules cannot determine:
Turmoil's complete shuffled event order and solo neutral tile locations are in the resolved premise
and exported game files. Admin draws the next event and handles neutral delegate placement without
incoming event identities or delegate choices.

Player inference is a limited convenience, not a promise to solve every underspecified request.
Players can supply more specific instructions. Supporting hypothetical Admin tasks or general
Player choice search is not a completeness requirement. Neither an engine transaction nor an Agent
call proves that all remaining tasks of a game action can eventually finish.

## State, transactions, and calls

A World contains component counts, tasks, effects, and event history on a single reversible timeline.
Calls are serial. Checkpoints and nested restoring scopes refer to that same timeline; they are not
concurrent transactions. Restoration is limited to retained engine history and engine-owned state.
Arbitrary Kotlin side effects, application state, and external notifications are not undone.

An **engine transaction** performs a command attributed to an Actor. Its work includes task edits,
requested changes, required-part construction, dependent removals, recursive immediate (`::`)
effects, invariant checks, and admission of resulting tasks. Invariants are checked after each
elementary change and its immediate consequences; a later ordinary group member cannot repair an
earlier member's invalid result. Direct mandatory `Die` fails when admitted.

An **Agent call** runs the requested engine work, mandatory Admin work, configured Player automatic
work, and applicable application/completion checks. These may be several transactions. For example,
using AI Central can run System bookkeeping and return with its Player draw task still pending.
A pass can trigger production and the next generation before returning.

`doTask` and `doTasks` must not return successfully without completing every instruction named in
their text arguments. They reject text containing `THEN`: callers submit the current step and then
submit later steps separately. A current step can create child tasks or enqueue a `THEN`
continuation without executing that later work. For example, executing an `EACH` task can finish the
named `EACH` instruction by creating its individual child tasks.

`doTasks(a, b, c)` is a sequence of individually atomic `doTask` calls. If `c` throws, its changes
are restored, `a` and `b` remain completed, and later arguments are not attempted. The failed call
returns no result. A successful call reports the combined changes and surviving new tasks across
all steps. An empty sequence does nothing. Recording stores the successful steps as ordinary
`doTask` inputs; a failed later step does not erase them.

Each engine mutation restores its transaction on an escaping `Throwable`. Other Agent mutation
calls restore the entire call on an escaping failure, including successful earlier transactions
in that call. An enclosing operation or Agent call still restores its whole block when failure
escapes, even if it originated in `doTasks`. A task-submission failure caught inside an operation
block restores the failed step but retains earlier steps. A later failed call does not automatically undo
an earlier call that already returned successfully.
For example, requesting AI Central's nonexistent second action fails without consuming its valid
first action. [Timeline](../../src/common/dev/martianzoo/engine/Timeline.kt) specifies restoration
limits; [EX_MACHINA.md](EX_MACHINA.md) describes corrections separately.

`TaskResult` covers retained changes throughout the call, in event order, and spawned tasks still
pending at return. Changes restored before return are not reported as retained results. The event
log preserves the detailed transaction history.

## Identifying the permitted task

Task matching reads descriptions; it does not execute competing tasks to choose the successful
permission. It combines the submitted instruction with a pending task's restrictions. A task ID
identifies an exact task, and a context component's class can disambiguate otherwise matching tasks.

Calculated matching is supported when unique. Gates and metric wrappers can be omitted from the
submitted text, but the engine still checks their requirements and amounts. A failed gate can
identify the intended task so the actual unmet requirement can be reported. A request naming a
map tile is distinct from a request creating the map area itself.

Several matching tasks are interchangeable only when their functional data agree: instruction,
continuation, assignee, selection, and deferral. IDs and originating causes do not distinguish
otherwise equivalent tasks; the executed task's actual cause remains in history. Different
continuations require disambiguation even when the immediate reward is identical.

Separate pending tasks are separate permissions. Agent can translate an explicitly submitted
resource total into several permitted changes, as with production income. Each change retains its
own effects and follow-up work; the engine does not replace them with a net change or choose among
permissions by trying their outcomes.

## Selection, narrowing, and execution

Selection chooses pending work; narrowing supplies a compatible choice. Both can retain partial
choices in the selected task without executing its component changes. Choosing a target can permit
further evaluation, including its AMAP amount. No transaction stays open while a person chooses.
Evaluated and closed instructions can still fail when executed.

Groups, `EACH`, and dispatches exposed by selection retain their parent until execution. Executing
that parent admits its children and consumes it. Ordinary initial admission still normalizes literal
groups into independent tasks, removes literal zero/Ok work, and separates eligible sequences.
A group submitted for execution processes its own admitted member IDs, not unrelated reactions or
continuations. Transfer to a different Actor stops the submitting Actor from executing that work.

Ordinary task additions and dropping a different task are refused while a task is selected.
Dropping the selected task is an explicit correction facility, not an in-game way around priority.
`exMachina` remains available and preserves selected choices and already calculated amounts.

### Linked instructions

A `THEN` instruction remains joined while a shared type or amount required by its current step is
unbound. A caller may identify and narrow it by its first step alone when that identifies a unique
task. The engine carries the first step's type and X bindings into every later step. For example,
`-2X Foo THEN 3X Bar` accepts `doTask("-4 Foo")` and leaves `6 Bar` pending; `-3 Foo` cannot bind an
integer X and fails. `Task.bindXTo` can also explicitly bind X. A first-step request may omit its
gate; the engine retains that condition and checks it after shared choices are bound.

For example, a normal blue-card action is offered as use the chosen action card, then mark that same
card used. A caller can submit `UseAction<AiCentral, Action1>`; the engine binds the later
`ActionUsedMarker<AiCentral>` consistently. Supplying Inventors Guild for that later step would be
incompatible. TaskForm can construct the complete binding from a guided choice.

After binding, the task exposes its current instruction and stores its continuation separately.
The caller can then narrow or execute just that current instruction without repeating the
continuation. The continuation is admitted when the task finishes, without special priority; it
does not wait for reactions queued by that task. Nested composition follows the existing Pets
restriction that a group cannot be a left operand of `THEN`; no second continuation slot exists.
See [SEQUENCING.md](SEQUENCING.md) for authored sequencing and [TASK_HANDLING.md](TASK_HANDLING.md)
for the automatic normalization inventory.

## Determining an executable instruction

After identifying a task, the engine combines its restrictions with the request and evaluates gates,
metrics, dependencies, and supported forced deductions. When choices still need narrowing, its finite
transaction analysis covers fixed mandatory/optional gains and removals, full-form transmutation
pairs, and OR alternatives composed of those changes. Optional changes include zero and every
positive amount through the authored count. Unbound X and unsupported forms are not searched by
inventing an amount range.

Each candidate is tried in a disposable World through its complete requested transaction. This
includes immediate effects, required/dependent parts, final checks, continuation admission, and
transaction-completion promotion. Trials leave the source World and its listeners untouched.
Custom Kotlin effect objects and their external side effects are not copied.

- A complete analysis with exactly a single successful closed instruction permits execution.
- Multiple distinct successes require a more specific request, even if their resulting counts agree.
- No successful candidate in a complete analysis rejects the request.
- Unsupported or unfinished analysis reports `IncompleteAnalysisException`; it is not proof of
  impossibility or uniqueness. This is a subtype of `NotFullySpecifiedException`, so callers that
  distinguish incomplete analysis check the subtype. There is no elapsed-time cutoff.

For example, a city request may evaluate to Normal City when Capital cannot complete the requested
transaction. Land Claim's immediate cube removal participates in tile-placement validation.
Protected Habitats' direct rejection participates when its task is admitted.

**Newly queued tasks are not executed to prove a candidate.** This includes subsequent Admin work.
A candidate can succeed and then encounter a later Admin failure, which restores the enclosing
Agent call. This distinction prevents task order from becoming a way to choose the interpretation
of the original request.

The finite analysis is shared by explicit Player execution and Admin. Its implementation does not
constitute a general Player inference promise. [QUANTIFIERS.md](QUANTIFIERS.md) owns quantity rules:
AMAP uses current capacity, while mandatory and optional positive changes can be validated through
immediate effects that repair a temporary violation.

## Which task acts next

| Priority | Work |
| --- | --- |
| P0 | The selected task, for any Actor |
| P1 | Ordinary Admin tasks |
| P2 | Ordinary Player tasks |
| P3 | LATER tasks |
| P4 | FINALLY tasks |

Selected work excludes competing tasks. If the selected task becomes System work needing no narrowing or has
an explicit Admin assignment, Admin executes that same selected task. Separately created Admin
work waits for it to finish. A selected Player task takes precedence over ordinary Admin work.

### Admin's choices and retries

Agent always runs Admin; Player policy and replay settings cannot disable it. Admin attempts
ordinary tasks FIFO. After a success it starts a fresh pass, retrying earlier failures. A complete
nonempty pass with no success throws, even if Player work could make a task executable. An unfixed
choice, false gate, or unavailable change does not authorize returning control to ordinary Player
work. On a stalled pass, missing application input takes precedence, then incomplete analysis,
then a retained rule failure. Escaping failure restores the whole Agent call.

The raw engine's Player-priority check probes whether Admin can execute; it is not a replacement
for Agent's mandatory loop. Clients use Agent for the complete processing and recovery behavior.
Admin finishes when no ordinary Admin work remains or selected Player work takes priority.
Deferred Admin work does not participate until promoted.

When ordinary work is empty, all existing LATER tasks promote together to ordinary Actor priorities.
When ordinary and LATER work are empty, all FINALLY tasks promote together. Promotion happens at
transaction completion; new deferred work waits for a later promotion. Solar Probe's deferred flip,
for example, waits for its draw and other ordinary work.

### Task handoffs during an active Player's turn

NOW and SOON distinguish immediately selected recipient work from ordinary queued recipient work.
The active Player chooses when to release each selected task to its recipient. That Player then
narrows and executes through their own Agent while the active Player retains the baton. Each
release is a separate task handoff, even when a later task belongs to the same bonus. An
instruction-side `BY` change of assignee is not, by itself, this kind of handoff. The active Player
cannot supply the recipient's choice.

Pluto colony bonuses use this ordering: the active Player starts the owner's draw/discard chain;
PlutoLock protects that owner's unfinished bonus. Another bonus for the same owner cannot begin
until the first is completed. Another owner's bonus has its own lock. The active Player also
schedules the later offered handoff. Philares resource choices likewise belong to its owner.

## Agent policies, call helpers, and corrections

Each Agent belongs to an Agents session. That session supplies Player policies and application
validation; separate sessions sharing a World do not inherit each other's settings. The shared
AutoExecLoop runs Admin before each optional Player step. NONE, SOLE_TASK, and EAGER retain their
current behavior; EAGER can choose successful Player work in queue order. This is configurable
existing behavior, not a guarantee that Player choices are strategically equivalent.

The current `runOperation`/`beginOperation`/`continueOperation`/`completeOperation` helpers retain
the existing decision marker and FINALLY-based completion convention. Their grouping remains under
API review; it is not a separate game lifecycle. Nested calls share the
World's restoration depth, with outermost completion/application callbacks. Missing application
card information remains distinct from an engine choice.

`Agent.exMachina` is the correction entry point. It allows System edits, runs required construction,
dependent removal and immediate effects, suppresses queued effects, and checks final structural
validity. It rejects added unfinished MustCleanUp work. It preserves pending tasks, their selected
choices, and previously calculated amounts. There is no separate `sneak` command or completion
hook that leaves earlier changes committed after callback failure. See [EX_MACHINA.md](EX_MACHINA.md).

## Appendix: further work under consideration

These items are not claims of implemented behavior or requirements to build a general solver.

- **Canon-driven Admin analysis:** extend inference only when an actual supported Admin task needs
  it. Keep incomplete analysis distinct from a rule failure. Measure actual expensive cases before
  adding work limits. Player inference may remain limited or be reduced.
- **Player assistance:** menus that prove their choices, safe automatic policies, whole-action
  completion search, and outcome comparison belong to [DECISION_GRAPH.md](DECISION_GRAPH.md) and
  [AGENT_CONTRACT.md](AGENT_CONTRACT.md). Project Eden can place greenery on the only remaining
  legal city space and fail later; avoiding that choice requires looking at later tasks.
- **Legacy Agent call grouping:** reconsider the existing decision/FINALLY conventions and the
  remaining engine callback coordinator only against concrete callers; no replacement framework is selected.
- **Corrections and undo:** correction-time recalculation and a separate undo checkpoint for a NOW
  recipient remain open. Current recipient undo can return to before the active Player's handoff.
- **Model sequencing:** audit ordering-only immediate effects in payments, wild-tag setup, and
  government formation. Also investigate an amount evaluation that turns a THEN head into a group;
  current group admission releases its continuation, and no Canon failure has been established.
- **AMAP and diagnostics:** investigate actual Canon cases before changing capacity semantics.
  Distinguishing an Admin model defect from a rejected Player request, and whether a later failure
  ever needs to undo an earlier completed call, remain design questions.

Bounded follow-ups are recorded in [TODO.md](../../TODO.md). Historical implementation steps and
superseded open decisions are evidence in the results document, not additional requirements.
