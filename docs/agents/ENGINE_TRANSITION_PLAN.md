# Engine and agent transition experiments

> **Master plan — October 24, 2026.** Intended for a coding agent commissioned to investigate on
> its own branch. This document is a plan, not a report that experiments have run. Preparing it
> does not begin implementation or authorize merging experimental changes.
>
> [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns the selected destination and open details.
> [DECISION_GRAPH.md](DECISION_GRAPH.md) owns agent choice and completion-analysis rules.
> [TASK_HANDLING.md](TASK_HANDLING.md) maps current behavior; inspect source before relying on it.
> Here “coding agent” means the investigator; `:agent` means the runtime module.

## Objective and successful outcome

Find the smallest coherent implementation of the intended engine responsibility, and discover its
real costs. A useful outcome can include successful changes, rejected changes with evidence, and
specific unresolved questions. The goal is not to accumulate the largest mergeable diff or to make
every experiment pass by adding compensating machinery.

The engine serves a client with an intended change. It first identifies permission through a
conservative task comparison, then determines whether the request and that task admit exactly a
single narrowing whose transaction succeeds. That transaction includes immediate consequences and
task admission; it excludes executing subsequently queued work, including Admin work. The agent
owns automatic submissions and reasoning about later tasks. Selection does not itself authorize
execution. These are the experiment's starting principles, not a claim that transition will be easy.

Proceed through the experiments without seeking approval for routine choices inside their stated
scope once commissioned. Reevaluate the order when evidence warrants it, recording why. If an
experiment needs an unsettled semantic decision or disproportionate new machinery, preserve the
finding, leave that experiment unresolved, and continue independent experiments. Do not silently
change the selected contract to unblock code. Ask only when the remaining useful work actually
depends on the owner's answer. Repository stop rules for growing architecture still apply.

## Branch, checkpoints, and evidence

1. Read checkout instructions and refresh status, current source, and the active design docs.
   Create an isolated worktree on `codex/engine-transition-experiments` or an unused suffixed name.
   Record the exact base revision. Do not modify another checkout's dirty work or assume its branch
   tip contains the latest uncommitted design docs. Copy the explicitly relevant current docs into
   the experiment checkout when needed and record that starting difference.
2. Keep the original checkout untouched. Changes on the experiment branch must be attributable to
   this plan. Do not merge, publish, or push experiments without separate authorization. If commits
   are authorized by the commissioning request, make small experiment checkpoints. Otherwise keep
   recoverable patches and exact base references under `$TMPDIR`; lack of commit permission need
   not stop investigation. Human-facing findings belong in the experiment checkout's docs.
3. Maintain `docs/agents/ENGINE_TRANSITION_RESULTS.md` on that branch with a compact status table:
   experiment, hypothesis, base/checkpoint or patch, outcome, commands, and recommendation. Include
   a reproducible counterexample for a rejected approach. Save an unsuccessful attempt before
   removing only its own changes. Keep an independently useful experiment independent of a failed
   predecessor wherever possible; do not make unrelated work depend on knowingly broken code.
4. After each experiment, compare both against its starting point and against the original baseline.
   Record what was deleted, what was added, and what moved. Distinguish test adaptation from new
   production responsibilities. A passing test obtained by hiding work in `TfmGameplay` is failure.
5. At the end, report which changes form coherent candidates for review, which should stay as
   evidence only, and which questions still need a decision. The recommended integration set may
   be empty. Do not label the destination implemented while acceptance cases remain unresolved.

## Acceptance checklist used throughout

Use this checklist before each revision and when reviewing the final experimental diff. Add newly
settled requirements as the investigation proceeds; do not lose earlier requirements to later fixes.

- Task finding is read-only and does not try consequences to pick between distinct matching tasks.
  Interchangeability must include meaningful task data, not just matching immediate changes.
- Information may be supplied jointly by the identified task and request. Do not require redundant
  concrete names or change the class hierarchy to avoid valid abstract requests.
- Unique execution means exactly a single concrete narrowing completes the requested transaction.
  First success, equivalent outcomes, and incomplete analysis do not establish uniqueness.
- Temporary invariant violations repaired by immediate effects remain possible candidates. Errors
  caused by missing information, resource limits, or implementation defects are not game-rule
  rejections that eliminate candidates.
- Newly queued Admin work is not executed during that proof. Direct mandatory Die admission can
  reject now; later failure remains later. Trying a candidate leaves no retained state or history.
- Selecting or narrowing does not authorize execution. Explicit combined execution is
  different. Classify structural task changes separately and expose unresolved exceptions.
- Joined THEN retains a shared unbound type/X; explicit binding exposes current work and a separate
  continuation. Do not infer X backward from a submitted first-stage result in either module.
- Partial deductions can be useful without permitting execution. Voluntary stored partial
  narrowing remains an experiment, not a deletion decided in advance.
- Admin is always automatic through the agent, including replay. Player policy settings do not
  disable it. Raw engine clients remain possible and preserve transaction validity independently.
- Public engine failures restore their transaction. Broader Agent-call recovery, application side
  effects, and history inspection remain separate questions. Preserve completed history semantics.
- Whole-action exploration and outcome equivalence remain in the agent. UNKNOWN never licenses
  removing a potentially meaningful choice. Finite candidate generation must not allocate an
  unbounded integer domain.
- Functional game scenarios use agents; focused engine tests exercise raw engine promises. Preserve
  actual Canon behavior, not incidental queue order, helper assumptions, or obsolete APIs.

## Baseline and measurements

Before changing behavior, inspect `ActorEngine`, `Instructor`, `newTasks`, `TaskQueues`, `Limiter`,
`Effector`, `Initializer`, `WorldTransaction`, `AgentTransactionGuard`, `AgentImpl`, `AutoExecLoop`,
and `TaskForm` only for the experiment at hand. Follow the source map in TASK_HANDLING rather than
assuming method names describe all their effects. Inventory actual production and test callers.

Read [TESTING.md](TESTING.md). Start with the relevant existing engine and agent JVM suites and a
small Canon scenario set. Record preexisting failures separately. Use filtered card/rule tests via
`:tfm-tests:jvmTest`; full-game cases use `:tfm-tests:replayTest`, whose class filter must actually
include the requested replay. Expand to the curated replay suite when scheduling or runtime
semantics change. Run a full build at coherent cross-module checkpoints, not after each tiny edit.
Run browser checks when changed public surfaces reach browser clients. Do not enable intentionally
disabled optional suites as a side effect of this plan.

Choose a fixed representative workload before timing changes: ordinary resource payment, a
placement with location choice, a direct protected removal, a request requiring immediate-effect
validation, and replay segments with multiple tasks and Admin follow-up. Measure raw engine calls
and complete Agent calls separately. Use the same starting states, commands, environment, and warmup
for comparisons. For each request report candidates after type intersection, candidates excluded
by sound cheap checks, transaction attempts, and whether later Admin work was executed. For Agent
calls report that later work separately. Do not claim complete smart-policy coverage from timing
only the search function; exercise the actual path submitting choices and handling Admin.

Cost reports must cover:

| Cost | Evidence to retain |
| --- | --- |
| Conceptual | Added or removed concepts, public methods, stored facts, exceptional cases, and cross-module dependencies. |
| Code | Production and test diff sizes separately; moved code separately from new code; duplicated rules eliminated or introduced. |
| Gameplay | Named Canon card/rule, before/after choices and effects, meaningful failing and passing scenarios. State when no Canon reliance is known. |
| Usability | How a direct engine request and an Agent request change; how much test setup became more explicit. |
| Runtime | Candidate and attempt counts, complete measured latency, slow cases, and amount of unresolved analysis. |
| Recovery | Components, tasks, selection, events, and transaction positions after failure or speculative attempts; observer limitations where relevant. |

Prefer temporary instrumentation to permanent metrics APIs. A wider diff can be worthwhile if it
removes a concept. Conversely, few lines that introduce a second interpreter or a lasting exception
can be expensive. Do not optimize an invented worst case before measuring the representative cases.

## Experiment 1 — remove conveniences that execute more than requested

Run these as separate, recoverable changes; neither requires the complete uniqueness implementation.

**Combined totals:** inventory `combineScalars` / `executeScalarSum` callers. Try removing the engine
facility and expressing the actual separate tasks in test support where needed. Preserve each
task's individual effects and real ordering choices. Do not replace it with a normal agent feature
unless a real production need is demonstrated. Explain any former gains-before-removals reliance.

**Extra queued work:** remove or narrow `executeSubmittedGroup` so explicit submitted work can
execute without also consuming reactions or continuations merely because they appeared in the
queue. Reproduce the documented First/Second/Third case: executing First should leave its Second
reaction and Third continuation pending in the raw engine. Also exercise an explicitly submitted
group, so removing the convenience does not make ordinary group submission impossible.

Use focused engine tests for these mechanics and Agent tests for affected input forms. Name any
Canon scenario that actually relies on the convenience. A synthetic test preserving the old flag
is not sufficient reason to retain it. Report whether the removal simplifies transaction scope.

## Experiment 2 — explicit shared bindings and the THEN client view

Try the smallest existing binding mechanism before adding an API. Show joined work only while a
shared type/X needs a value; after explicit binding, expose current instruction and continuation.
Exercise `X Foo THEN X Bar`, `3X Foo THEN X Bar`, shared types, nested scopes, and an already occupied
continuation. A client binding X to 3 must mean 9 Foo then 3 Bar in the scaled case. Supplying only
3 Foo must not secretly infer X. Once split, narrowing the current half must not repeat the future
half. Test failure restoration as well as successful binding.

Trace every affected Canon case before treating a changed test as gameplay loss. Adapt consumers to
the selected meaning; do not add a second continuation representation to preserve an old shortcut.
If a new representation appears necessary, record the concrete obstacle and park that approach
before building it. Keep this experiment independent of generalized agent search.

## Experiment 3 — selection and narrowing without execution

Walk every row of the automatic-actions inventory. First remove execution merely because selection
or narrowing reaches a fully specified instruction. Compare raw engine selection, explicit engine
execution, and an Agent command under Player NONE with Admin still automatic. An apparent Agent
execution may be a legitimate separate submission; trace it rather than inferring from final state.

Investigate zero/Ok completion, EACH/group expansion, pure dispatch, handoff, continuation admission,
and deferred promotion individually. Start with no implicit execution; compare any proposed
structural exception against that version. State exactly what task edits it permits and why it is
needed. Do not retain exceptions simply because they make old tests pass. Check World Government
Advisor's Player-chosen placement and subsequent Admin work where relevant.

Retain voluntary partial narrowing for the first attempt. Exercise a target choice unlocking AMAP
evaluation. A separate comparison may use client drafts; measure lost evaluations, extra APIs, and
revalidation costs before recommending removal of stored narrowing. No transaction stays open
while a human chooses. This experiment may expose code the agent must explicitly call; count that
cost instead of concealing it in another engine method named select.

## Experiment 4 — transaction-success uniqueness for explicit requests

This is the central harder experiment. Establish the matcher and its tests before changing how an
identified task is executed. Keep exact task IDs available to bypass matching; do not require them
as the only intelligible way to use the engine.

For matching, test several apparently compatible tasks with different continuations or assignees.
Reject ambiguity without trying their transactions. Define a conservative comparison from actual
task data; do not reuse the agent's equality-of-outcomes rule.

Within the identified task, compose existing type intersection, dependency queries, limits, and
rollback support. Start with the finite domains already present in the source; identify anything
their enumeration misses before treating the result as complete. Reuse the real execution path for
hard cases instead of implementing another approximation of effects. Cover this acceptance matrix:

| Case | Required observation |
| --- | --- |
| Ocean permission plus Tile at Tharsis_5_5; CapitalTile plus supplied area. | Joint information is accepted when it fully determines a legal change. |
| Remove the sole matching existing component. | No demand to repeat its concrete name. |
| CityTile with Capital impossible in this transaction. | Accept NormalCityTile only if it is the sole full narrowing that succeeds. |
| Remove another Player's plant: protected Kim versus unprotected Pat. | With both plants present and Protected Habitats only for Kim, direct Die admission eliminates Kim's removal. Keep the real Player attribution; Admin would change the rule. |
| Land Claim placement with immediate cube removal. | Final invariant checks include the repair; an early capacity filter does not reject it. |
| Two successful concrete narrowings. | Ambiguity, even if resource deltas happen to match. |
| No successful narrowing; or an unfinished analysis. | Distinct failures; neither produces a choice or retained trial changes. |
| Candidate succeeds now but creates Admin work that later fails. | Raw engine acceptance depends only on now; Agent handling separately exposes the later failure. |
| Direct Die versus gated/optional Die or a later THEN failure. | Document admission/simplification meaning; do not generalize a direct rejection without proof. |

Reconcile unique-subtype gain and present-dependency special cases under the general rule. They are
not automatically moved out. Separate discovering impossibility/no-op from algebra on known
Die/Ok results. Report whether current automatic effects use the same interpretation and what extra
requirements truly immediate work imposes.

Count how often cheap intersection suffices, how often transaction attempts are required, and the
largest candidate domains. If the implementation cannot establish uniqueness, preserve that fact
explicitly; do not silently execute the first success or treat a time cutoff as game illegality.
The public way to report incomplete analysis remains a recommendation to evaluate, not a promise
that the destination is already met for every Pets instruction.

## Experiment 5 — Admin handling and Canon rejection timing

Apply the same transaction-success rule to each Admin task that the agent attempts. Keep FIFO and
retries after progress; preserve and explicitly investigate the existing waiting cases instead of
inventing success after a stalled pass. Use greenery's plant payment and shared research capacity
to distinguish legitimate waiting from a permanently stuck task. Check missing external input,
Turmoil event draws, and solo neutral placements before claiming complete Admin coverage.

Inventory how ordinary Player priority, handoff, and bootstrap currently try tasks. Keep the
priority obligation separate from task matching: a priority check cannot silently select the task
the client meant. Independently requested trial APIs can remain. Demonstrate no unrelated work is
retained by a check and no receiving Actor's task executes just to complete a handoff.

For each Canon timing candidate, record the triggering change, immediate checks, tasks admitted,
first possible rejection, and the state visible between transactions. Use a TSV if the inventory
outgrows a readable table. Include direct `: Die`, ordinary Admin work that later fails, and existing
`::` effects that infer a target. Try conversion in either direction only when the rule demands it.
NextPlayer links must form as Players are created, including the final closing link; do not queue
all links and rely on their incidental order. Test corrections where changing `::` affects them.

Never broaden engine uniqueness to run Admin simply because a Canon model becomes awkward. Report
that modeling cost. Keep payment redesign and global authored-action replacement out of this
experiment unless separately commissioned. The existing standard-project paid-event and card-play
completion TODOs remain; AI Central's absent Action2 is evidence about delayed failure, not
permission to start the deferred global-slot redesign.

## Experiment 6 — generalized agent narrowing and explicit automation

Use the engine's transaction rule as the source of immediate legality. Try composing partial
reductions for type, dependency, and amount choices rather than adding separate card-specific
rules. Keep narrowing a displayed question distinct from deciding to submit execution. Preserve
both guided choices and direct input from a client that already knows the desired change.

Measure partial reductions that stay partial as well as chains that determine a full instruction.
Honor explicit THEN bindings. Bound number choices safely without constructing every integer.
Retain missing-card and unexamined alternatives; do not choose the sole *tested* success. Show raw
engine, manual Player Agent, and automatic Player Agent behavior side by side, with mandatory
Admin handling in both Agent modes.

Only then test the connection to later-task exploration from DECISION_GRAPH. Project Eden's
competing placements and Old Mining Colony's needed cards are useful delayed-failure cases.
Immediate transaction success must not be presented as action completion. Full completion search,
outcome collapsing, and safe eager execution can be evaluated independently; a full graph system
is not a prerequisite for reporting the engine experiments. Keep UNKNOWN when endpoint recognition,
card identity, or analysis limits prevent a conclusion. Do not silently change the default Player
policy; that migration remains separately deferred.

## Experiment 7 — the surrounding Agent call

This can begin after the baseline independently of experiments 4–6. Investigate each responsibility
separately before proposing a package move or replacement API:

| Current code or feature | Responsibility to evaluate in plain terms |
| --- | --- |
| `WorldTransaction.run` / `ActorEngine.transact` | Runs the client's commands, calls more task handling afterward, checks completion, calls application code, and handles work that application code added. Several engine transactions can happen before this call returns. |
| `AgentTransactionGuard` | Checks application-supplied facts such as which anonymous card moved. Raw engine calls bypass it; it is not the check that component counts obey game rules. |
| `World.onTransactionComplete` | Notifies application code after surrounding work; that code can issue more commands. Its failures and external side effects need a defined treatment. |
| `ActorEngine.requireComplete` and operation tracking | Decides whether work across several tasks has finished, using event positions, pending work, and cleanup state. This is a different question from whether the last transaction succeeded. |
| Timeline, fork, overlay, and correction APIs | Restore or inspect game state, or provide a branch for trying commands. Judge each independent capability on its actual use; support for agent speculation alone is not a reason to remove it. |
| Initial construction | Builds a valid World and currently performs bootstrap work. Determine what must remain engine construction versus automatic handling by a caller, without requiring a new construction framework. |

Try moving or deleting the smallest coherent piece that belongs to the agent/application. Preserve
engine transaction restoration and safe client-controlled rollback. Keep the distinction between
returning all retained changes and promising that failure undoes an entire multi-transaction call.
A callback currently can fail after earlier work survived; demonstrate that case before recommending
its treatment. Do not quietly promise rollback of arbitrary external application state.

Re-run a raw Protected Habitats rejection, an Agent call that completes several transactions, an
application card-input failure, and nested calls with completion notifications. Verify each returned
result covers its actual retained interval. Keep navigation and transaction grouping intact; the
completed ordinal/checkpoint work is not a new migration dependency. If deleting a helper requires
a broad operation framework, record the cost and stop that approach. `TfmGameplay` is for deletion,
not a destination for responsibilities removed from engine.

## Review and final handoff

After each coherent experiment run its focused tests, review the complete diff using
[REVIEW_CRITERIA.md](REVIEW_CRITERIA.md), and update the acceptance checklist and results. When
changes affect scheduling, payments, or deferred work, broaden to relevant Canon interactions and
curated replays. Preserve original replay choices; changing old implementation-shaped commands is
different from inventing a new game history. Record tests that were filtered out, skipped, or failed.

The final report should let a reviewer answer, for every experiment: what changed, what it taught
us, whether it obeyed the destination, what unexpectedly became harder, and what should happen
next. Include reproduction commands and exact checkpoints or patches, summarize code and runtime
costs, and distinguish measured facts from hypotheses. Identify dependent changes that cannot be
reviewed or adopted separately. Leave the branch recoverable with no undocumented failures.

Update current owning docs only for verified behavior on that experiment branch or newly accepted
direction. Preserve the destination as intended until its acceptance cases are met. Do not turn an
experiment's compromise into policy merely because it passed tests. Report completion of the
investigation separately from completion of the transition; neither implies approval to merge.
