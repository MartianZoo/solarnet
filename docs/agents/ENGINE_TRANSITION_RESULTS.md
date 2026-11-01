# Engine transition experiment results

This report is maintained in work1 and describes the experimental branch, not work1's current
implementation. Source names and test results below refer to that branch at the stated revision;
local evidence files remain in `/Users/dev/solarnet/engine-transition-experiments/_local/`.

The initial retained experiments were committed through `8ceb7706b`; `main` at `f1055a08b`
was subsequently merged into `codex/engine-transition-experiments` as `6c4686b61` on
October 28, 2026. Subsequent increments and final acceptance evidence are recorded below. The selected transition
is implemented within its documented finite domain; general inference remains incomplete. The experiment branch
has not been merged into main. The branch began at
`c14c000b23f143f0f672ba2b94ae21966ff245d7`, in
`/Users/dev/solarnet/engine-transition-experiments`. The initial work1 design documents were copied
here. During review, the user requested integrating main and committing those design documents in
work1; that documentation commit is `2b3ad9c8a`, based on main `4974858d2`. The code and its review
fixes belong to the experiment checkout; work1's commit contains documentation only.

[ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns the destination;
[ENGINE_TRANSITION_PLAN.md](ENGINE_TRANSITION_PLAN.md) records the implementation checklist,
settled owner decisions, and scope limits. General Player choice search in
[DECISION_GRAPH.md](DECISION_GRAPH.md) is outside this engine API miniproject.
Evidence is in the experimental checkout's `_local/engine-transition-evidence/`. The original
report is preserved there as
`first-pass-report.md`; its parked/retained labels describe the first pass only.
Two earlier Opus/high discussions are preserved in that evidence directory's `claude-review/`.
This follow-up implements the agreed
order: 7, repaired 1b, 1a, 3 with numeric 2, then 4/5 and further evidence for 6.

## Disposition after the initial experiments

| Experiment | Current branch | Merge assessment |
| --- | --- | --- |
| 7 Surrounding Agent call | Application checks belong to an Agents session; cross-session behavior tested. Operation completion checking now lives in Agent. Unused completion hook removed; request and automatic work share whole-call recovery. | Good independent change. FINALLY-based completion remains separate. |
| 1b Submitted groups | Executes the exact IDs returned when group members are admitted; excludes dispatch bodies, reactions, and continuations. | Good independent change; repairs the initial queue-difference approach. |
| 1a Combined scalar totals | Deleted engine arithmetic and flag; Agent translates explicit totals into separate engine calls. Production tests use totals again; unrelated rewards stay separate. | Worth merging with the Agent restoration below. |
| 3 Selection | Selection/narrowing do not execute component changes, and now retain `EACH` parents until execution; other structural splitting and handoffs remain. | The `EACH` correction satisfies its selected timing rule. Classify the remaining structural paths separately. |
| 2 THEN | Shared numeric X must be supplied explicitly; no reverse inference from the requested first-stage amount. | Numeric change is suitable with 3. Broad shared-type removal remains parked. |
| 4 Transaction uniqueness | Fixed mandatory gains/removals and full-form transmutations enumerate concrete types or pairs and try complete transactions in disposable copies. | Experimental integration candidate, not complete general inference. Review cost and unsupported forms before merging. |
| 5 Admin | Uses the same unique-success rule; no later Admin work is executed to justify a candidate. | Keep together with 4. Canon declarations did not need changing for the tested cases. |
| 6 Agent narrowing | Draft composition tests and a concrete counterexample; no new automatic reduction policy. | Evidence for the separate Agent choice project, not a gate for the engine API. |

### Admin and selection preserve incomplete analysis

The increment extends distinct incomplete-analysis reporting to selection probes and Admin
handling. Admin retries after other work progresses. A stalled pass reports unfinished proof
before a retained gameplay rejection, so task iteration order cannot erase the incomplete result.
Whole-call rollback removes earlier automatic work when that result escapes.

Five scenarios exercise restoration, retry, Player selection/probes, and mixed failures in either
queue order. Three synthetic manual-choice scenarios now assign the choice to a Player with policy
`NONE`, preserving their assertions about AMAP zero-capacity targets and form decisions. Admin is
not an actor whose policy can be disabled to obtain a manual choice.

Before the raw-priority review fix, JVM verification passed 628 engine tests, 73 Agent tests,
50 script tests, 1358 Canon/rule tests (26 existing skips), and all 23 active replays with full/short
export and reconstruction. After that fix, all 629 engine tests and 73 Agent tests passed.
Opus/high identified an order-dependent raw Admin-priority check: an unfinished proof could hide
a later proven-executable Admin task. The scan now retains incomplete analysis until it has looked
for a definite priority blocker, with both orders tested. Agent and selection KDoc now describe the
propagated failure. Automatic Player policy begins only after Admin has completed its pass and
limits its own attempts to concrete instructions or `EACH`; initial-operation admission deliberately
retains choices for the operation body.

### Execution queries and try calls preserve incomplete analysis

`canExecuteTask` and every Agent `tryTask` overload now propagate the existing incomplete-analysis
exception rather than converting it to false or a successful unchanged call. Known missing choices
retain their previous behavior. The Agent scenario checks each entry point against unbound numeric
work, verifies unchanged state and history, then supplies a concrete amount successfully.

Opus/high identified initialization as another execution-query caller. Its scan now retains
incomplete analysis while trying other tasks and retries after progress, including the existing
Player fallback. A stalled unfinished proof reaches `Engine.newGame` without being relabeled as an
invalid configuration. Setup scenarios cover later Admin progress, earlier Player progress, and a
stalled unknown result. This adds no public API, choice representation, or persistent state.
The full build with replay exports passed before these initialization review fixes. Final JVM
verification passed 624 engine, 73 Agent, 50 script, 1358 Canon/rule tests (26 existing skips),
and all 23 active replays with export and reconstruction. The focused setup suite passed again
after removing an unreachable throw identified by Opus. Admin gameplay scheduling, selection probes, and automatic Player
policy catches remain separate work in the transition plan.

### Assertion failures restore transactions and enclosing calls

Both raw transaction restoration and the enclosing timeline block now catch `Throwable`, restore
their game changes, and rethrow the original object. The explicit abort signal still restores and
returns an empty result normally. The new raw scenario executes a gain that queues Admin work,
throws an assertion failure, and verifies restoration plus transaction serial reuse. The Agent
scenario throws after nested calls from two Players and verifies restoration and a usable next
call. This adds no new API or stored state. External side effects and arbitrary caller metadata
remain excluded. Opus/high found no correctness issue; its test consolidation and documentation suggestions were
accepted. All 621 engine tests and 72 Agent tests passed.

### History floor changes are construction-only

The API audit found no production client of `Timeline.commit`: only initialization and
fork/overlay construction use it. The public method is removed; the internal implementation
refuses calls inside a restoring block. This removes the path that could advance the history
floor past the state restored after a failed Agent call. The overlay reset test still checks its
internal floor reset, and `TimelineTest` verifies restoration and usable navigation after the
forbidden internal call. No replacement client API or compatibility wrapper was added.
Opus/high found no blocking issue. All 620 engine tests and 72 Agent tests passed; the focused
history test was rerun after strengthening its expected failure message during review.

### Solo neutral locations are part of the resolved premise

Setup now supplies the four fixed locations through `soloTileSetup`. A selected premise-local
declaration binds the four arguments of canonical `PlaceNeutralTiles`; its ordinary Pets effects
place the two city/greenery pairs. Existing local class serialization and hashing retain it in full and short game files; no new engine input,
custom instruction, or serialized field was added. The canonical setup signal is abstract and
Admin infers its sole configured concrete subclass. Invalid location geometry is rejected before
the setup can leave Admin waiting for an impossible fixed placement.

The initial attempt put adjacency refinements on bound type variables, which Pets rejects.
Opus identified the duplicate geometry checks in the subsequent generated-instruction attempt.
The retained form validates fixed setup locations before world creation and keeps the placement
effects in canonical Pets. Kotlin emits only the configured subclass declaration. The REPL
chooses four printed-card costs at setup; fixtures and sourced replay locations remain explicit.
`AbstractSoloTest`, `StinaGameTest`, gameplay/sandbox fixtures, the benchmark, and rule/script
scenarios no longer supply incoming Admin placement calls. The focused `SoloPremiseTest` checks
all four tiles and reconstruction from both file formats. Existing invalid-city and wrong-city
greenery checks now exercise invalid resolved setup input. Opus/high reviewed the implementation
and agreed with the final parameterized form. The REPL and setup-only fixture configure their
phase behavior before enabling Player automation; Admin remains mandatory throughout.

The final `./gradlew build -PreplayExports` passed: 619 engine tests, 72 Agent tests, 50 script
tests, 1,358 card/rule tests (26 existing skips), 23 active replay tests with regenerated files and
live reconstruction, and the browser checks. Both full and short solo files are also checked by
the focused scenario. The initial broad runs exposed remaining solo fixture constructors and
setup-only phase configuration timing; those callers were corrected without weakening engine
selection rules or disabling Admin.

### Whole-call recovery and removal of the unused completion hook

The owner selected restoring the entire Agent call when later Admin work fails and removing
`World.onTransactionComplete`. The branch now deletes that hook, the post-restoration callback
pass, and its navigation lock. Agent's request, automatic work, and validation share the restoring
block. `Timeline.atomic` documents the remaining limits, including external side effects.

`AgentCallCharacterizationTest` now verifies restoration of a Player request and an earlier
successful Admin reward when a subsequent Admin step fails, nested calls across Actors, complete
returned results, and cancellation. Recording coverage retains intermediate playback positions
inside nested Agent calls. Callback-only assertions were removed from fork and atomicity tests;
their state and isolation checks remain. The owner also confirmed that otherwise identical tasks
with different IDs and causes are interchangeable. `TaskIntersectionTest` exercises distinct
causes; `TransactionUniquenessExperimentTest` retains the different-continuations refusal case.

Opus/high found no code correctness issue. Its review led to restored nested recording coverage
and clearer documentation: an exception that escapes automatic handling restores the whole call;
a waiting task whose attempted failure is handled internally does not itself fail the call.
The full `./gradlew test` passed, including active replays and browser tests. The nested recording
and continuation-ambiguity checks were then rerun after review edits.

### Finite alternatives use complete transaction trials

After the fixed-state measurements, `successfulNarrowing` now enumerates the existing finite change
domains through `OR` arms and uses the same disposable-world execution path. `Guarded OR Other`
can execute Other when Guarded's immediate effect fails. Repeated concrete requests across arms
count once, including the shared zero of optional arms. Unsupported arms preserve incomplete
analysis unless two successful requests already prove ambiguity. No group product, continuation
search, public API, or second transaction implementation was introduced.

`AlternativeTransactionTest` covers immediate-effect restoration, queued work remaining pending,
shared zero, overlapping positive domains, and incomplete alternatives. Four existing engine tests
had manually supplied choices that Admin now determines. Their explicit failure and restoration
assertions remain on raw engine calls; their Agent paths now verify automatic completion.
Opus found the empty compatible-pair case still reporting incomplete analysis under an `OR`;
complete empty domains now reject, with a focused shared-type transmutation test. Another scenario
checks that Admin reports a dead end when every alternative fails, retaining the original task and
state. No Canon rule change was needed.
The full `./gradlew test` run passed. After the review's empty-domain fix, all 617 engine tests,
72 Agent tests, and 23 active replay tests passed; every replay also exported and reconstructed its
game file with `-PreplayExports`. Existing replay source needed no instruction changes for this
increment.

### Fixed optional counts now use complete transaction trials

Following `9ab9c008e`, concrete optional resolution keeps its authored count up to physical source
availability. Current invariant room no longer discards a positive amount before its immediate
effects can run. The existing finite trial loop now tries `Ok` once and all positive counts through
that bound over the concrete type domain. It executes no later queued work during these trials.

Raw `Repair?` now requires a choice because both zero and `Repair!` succeed; the latter's immediate
effect removes Repair and restores its MAX 0 invariant. A capped optional without that repair still
has zero as its sole successful transaction. The engine suite, repository `test` task, and full
`build` passed without Canon edits. This supersedes the earlier raw optional characterization below.
The case itself remains synthetic; no Canon rule using that immediate-repair pattern was identified.

Player EAGER still executes only concrete requests, so it now waits on an unresolved capped optional
that previously resolved to zero during selection. The raw execution command can prove zero when
explicitly requested. A subsequent menu fix makes optional amount menus retain positive counts up
to physical availability; the form can now submit the successful `Repair!` choice. These options
remain candidates rather than proven legal transactions. AMAP still uses current invariant room.
Optional card discard domains can include many absent card types; measuring and filtering those
physical failures is tracked in `TODO.md`. No public API, stored state, or additional execution
stage was introduced.

### `EACH` now expands on execution

After base `661cc1eb2`, selection resolves wrappers around `EACH` but retains the parent task.
Executing that selected parent resolves its selector against the current World, admits every
child, consumes the parent, and leaves the children unselected at their ordinary priorities.
Bootstrap and the existing Player EAGER and CONCRETE policies explicitly execute a selected
`EACH`; policy `NONE` leaves it pending. The Agent operation scope now accepts a task ID for
execution, matching the existing top-level Agent call. No second fanout representation or task
queue was added.

The direct selection test proves that selection creates no children and that execution creates a
pending child before the child's component change. Existing sandbox and replay calls that meant
"expand this fanout" now submit execution; their chosen child tasks and outcome checks remain.
The engine JVM suite (597 tests), sandbox suite (853 tests, 20 skipped), gameplay suite, and
curated replay suite passed after this migration. A later full `./gradlew build` passed after the
Opus review fixes for `BY` fanout and automatic selection checkpoints. These checks do not settle
other structural selection paths or general transaction uniqueness.

The current `ActorEngine.matchingTask` already compares a request with a pending instruction and,
when direct intersection fails, with that instruction resolved against the current World.
`TaskResolutionTest` proves `2 Plant` executes an individual `Plant / Heat` task through Agent
when 2 Heat are present; it does not isolate a raw engine call. `uniqueMatchingTask` currently
treats tasks as interchangeable when their task data matches after replacing ID and cause. Whether
resolved-value matching belongs in the final engine
API and when different causes matter are owner decisions; the current behavior does not settle
them. This is separate from Agent's submitted total spanning several tasks.

## 7: application checks are session-owned

`AgentTransactionGuard` and `AgentInputRequiredException` moved from engine to agent. `Agents`
stores the check, `AgentImpl` and `AutoExecLoop` invoke it at the existing validation points, and
`CardTracking.start` accepts the tracked session. World no longer stores application validation.
There is no compatibility shim or global lookup.

Two independently constructed sessions over the same World now have independent validation.
The new test proves that missing input restores the tracked session's attempt, another session
can act independently, and the tracked session remains blocked until its own input is supplied.
Clients must retain the tracked session. Speculative sessions cannot install tracking checks.

At this earlier checkpoint, `AgentCallCharacterizationTest` characterized the then-existing callback behavior: a callback
that throws can leave the initial engine changes and external notification retained. Nested
callback commands and Admin rewards appear in the returned result. Moving transaction nesting,
callback timing, and FINALLY-based completion requires further decisions; no operation framework
was introduced to make this package move appear complete. The later whole-call recovery increment
above removes the hook and replaces these characterizations.

## 1b: group execution follows admitted IDs

The initial proposal inferred group members from queue differences and instruction matching.
That could skip a member with a continuation or include work admitted by dispatch. The repaired
implementation returns the admitted member IDs from `applyResolution`/`narrowSelectedTask` and
executes only those IDs still assigned to the submitting Actor. It stops for handoff or unfinished
selection. A group failure restores earlier members of that submission.

Tests cover a THEN first stage, identical immediate instructions with different continuations,
dispatched bodies, reactions, mixed Actors, and rollback. The outer grouped-THEN example proposed
in review is not valid Pets syntax; supported group members with individual continuations are
covered instead. Atmoscoop's duplicate VenusStep submissions were removed; its original outcome
assertion remains.

## 1a: totals belong in Agent

The first pass deleted `executeScalarSum` and `combineScalars` from engine and expanded all affected
inputs. Temporary tracing recorded 69 old-helper matches in `total-caller-migrations.tsv`; the
tracing code was removed. That pass also excluded totals from Agent. The user subsequently
rejected that restriction: restoring the three production money sources as a single submitted
amount is the priority, while rewards for unrelated reasons should remain separate in tests.

Agent now translates a matching explicit total into separate engine calls by task ID. For example,
21 MC executes 6 MC of production, 20 MC from Terraform Rating, and the -5 MC offset. Each task
retains its own effects and continuation. Gains run before removals, so the offset needs no starting
money. All matching current nonzero amounts must sum to the request with a shared quantifier;
Agent never searches subsets or includes tasks created by those executions. Selection and explicit
IDs keep the request attached to that individual task. Failure restores the whole Agent call.

This uses a private Agent helper for fixed gains/removals and `/` calculations directly wrapping
those changes, without exposing engine's general instruction evaluation. No Canon-specific
production recognizer or additional public API was introduced. Costs retained deliberately: Agent
must perform the arithmetic, and a submitted total selects gains-before-removals ordering. It does
not collapse the engine transactions or their intermediate effects into a net change.

Restored 88 production submissions across ActionPhaseTurns and five replay classes. Unrelated
rewards remain separate, including Investment Loan/Media Group and Imported GHG/Optimal
Aerobraking. Existing production totals and final resource checkpoints remain the evidence; no
new replay facts were inferred. Agent tests cover totals, task continuations, atomized changes,
rollback, selection/explicit IDs, unspecified amounts, and replaying the recorded total. Raw-engine
tests verify that the total itself still cannot consume separate tasks.

The Agent restoration passed `spotlessApply :agent:jvmTest :engine:jvmTest :tfm-tests:jvmTest
:tfm-tests:replayTest :web:jsBrowserTest` in 3m 33s. Exact suite counts are recorded in
`agent-totals-tests.tsv`; `agent-totals-verification.log` records the successful run. Optional
archived suites remain disabled. The final diff preserves the earlier unrelated-reward inputs;
only production submissions were recombined in Canon scenarios. `git diff --check` passed.

## 3 and numeric 2: choose, then execute

`selectTask` and `narrowTask` retain concrete work until execution is explicitly requested.
Bootstrap was adapted to execute its concrete tasks. Minimal TfmGameplay changes replace the
selection loops that otherwise repeat forever, explicitly finish concrete payment/corporation
work, and explicitly submit PassBaton/Ok. No new gameplay abstraction was added; these changes do
not make preserving TfmGameplay a goal.

The initial selection impact was 47 engine and five Agent failures plus a replay helper loop.
The broader run then found 43 card/rule failures, primarily formerly implicit selection and
combined totals. Adaptations preserve the scenario assertions: Public Plans, Power Infrastructure,
Sulphur-Eating Bacteria, Energy Market, Hi-Tech Lab, Tycho Magnetics, requirement waivers, Audits,
placement bonuses, Viral Enhancers, and colony production now explicitly execute selected work.

`3X Foo THEN X Bar` requires X=3 before executing 9 Foo and later 3 Bar. Replay card sales bind X
explicitly too. Shared type inference and nested THEN remain unresolved: removing the type fallback
broke action-card selection across all 23 initial replays, and the current representation cannot
store a second continuation. No extra continuation field was added.

At this earlier checkpoint, selection still exposed `EACH` and other group children and performed
handoffs. The owner clarified that enqueuing all `EACH` children and consuming the parent executes
that parent task. The later correction above moves that work to execution. Admission can still
discard literal Ok/zero and split eligible THENs; their classification remains open. Partial
narrowing remains because it enables useful subsequent AMAP evaluation.

## 4 and 5: prove a unique requested transaction

After conservative task matching and request intersection, a remaining fixed mandatory Gain or
Remove can enumerate its concrete types. Each distinct candidate runs through real engine
execution and transaction completion in a disposable World. The proof includes required parts,
dependent removals, recursive immediate effects, final invariants, continuation admission, and
deferred-task promotion. It never executes newly queued tasks, including Admin work. This finite
experiment uses the existing invariant checks after each elementary change and its immediate
effects; it does not allow a later ordinary group member to repair an earlier member's violation.

Two successes are ambiguous even if their final counts agree. Missing information or unsupported
immediate work propagates as incomplete analysis; it does not eliminate a candidate. An empty
domain does not invent a winner. Rejection diagnostics are aggregated independently of iteration
order: a mixture of currently unavailable and dead-end candidates remains currently unavailable.

The original prototype used live rollback, which could notify listeners about discarded trials
and fail to complete an enclosing transaction during proof. The follow-up uses isolated copies
with their own transaction completion. Tests verify source listeners and existing source overlays
are untouched by successful probes and rejected trials, and deferred admission is checked even
inside an enclosing request. Custom Kotlin effect objects are still shared; arbitrary external
side effects are not copied or restored.

Concrete Canon evidence:

- Protected Habitats rejects the protected victim through direct Die admission; no :: conversion.
- A second CapitalTile fails while NormalCityTile succeeds at the requested area. Community is
  removed by the immediate effect before final occupancy validation; its placement reward remains
  queued. This tests both rejection and same-transaction repair with actual Canon.
- Ocean permission and a tile request naming a water area combine without transaction search.
- NextPlayer links appear incrementally as seats are created. Queue ordering is not substituted.
- A later failing Admin task does not eliminate the current successful candidate.
- Turmoil event draws still need supplied event identities. Unique neutral-delegate appointments
  now run automatically, requiring many old explicit submissions to become state checks.

Admin priority uses unique success, not merely existence of a successful candidate. An intermediate
existential implementation caused 211 card/rule failures and was removed. Admin retries waiting
work after progress. All-failed abstract work without ordinary Player work now reports failure;
unknown choices can remain pending. Existing gate/waiting semantics still need broader review.
Player NONE/CONCRETE/EAGER retain their prior inference scope; explicit Player execution can invoke
the new engine inference. A broader Player policy is not silently enabled by this experiment.

Transmutation, compound alternatives, numeric-domain search, and unresolved immediate choices
remain outside the implementation. This is a coherent finite subset, not the complete contract.

## 6: draft composition is useful, but sole-option selection is not safe yet

Test-only reduction composes type and amount choices, retains useful partial drafts, and avoids
materializing a billion-element integer range. Committing a draft does not execute it.

A passing BUG characterization demonstrates the missing guarantee:

```pets
CLASS Repair { HAS MAX 0 This; This:: Notice, -This }
CLASS Notice
```

For `Repair?`, the current amount menu offers only Ok. Explicit `Repair!` succeeds, leaves Notice,
and removes Repair before final invariant checks. Thus choosing the menu's sole option would
silently lose a successful transaction. Fixing the amount filter alone is also insufficient:
form commit selects/evaluates before applying the concrete choice, whereas direct execution can
apply the request first. This needs a coherent account of choice completeness and evaluation.
No second legality implementation or production automatic reducer was added.

## Measured request costs

A temporary JVM measurement prepared each source World once, reused a disposable copy, reset it
outside the timed interval, warmed each workload with 40 calls, then measured 80 calls. Raw calls
used a parsed instruction and exact task ID; Agent calls included text parsing and automatic Admin
handling with Player NONE. The research payment used Canon BuySelectedCards with four selected
cards and a prepared 12 MC bill. The city case includes Community's immediate removal. Canon
workloads used three players. This is a spot measurement, not JMH or a before/after speed claim;
JIT order, tiny sample size, and concurrent compilation/testing affect the tails.

| Request | Engine median / p95, microseconds | Agent median / p95, microseconds |
| --- | ---: | ---: |
| Mandatory MC removal | 26 / 29 | 69 / 418 |
| Prepared research payment | 88 / 189 | 238 / 309 |
| Ocean placement | 429 / 565 | 542 / 752 |
| Protected Habitats removal choice | 179 / 232 | 184 / 243 |
| City choice and Community repair | 1269 / 1842 | 1316 / 1763 |
| Synthetic unique success among 2 types | 368 / 838 | 313 / 677 |
| Synthetic unique success among 8 types | 294 / 443 | 320 / 433 |
| Synthetic unique success among 32 types | 769 / 948 | 766 / 885 |
| Synthetic unique success among 128 types | 1732 / 1869 | 1657 / 1824 |

Each synthetic domain has exactly a single successful candidate, so all listed candidates must
be checked. Two successful candidates can stop ambiguity detection earlier. These measurements
support continuing the finite experiment; they do not justify arbitrary enumeration, adding caches,
or calling the full transition fast. Ordinary curated replays also completed after caller migration.
No pre-change measurement of these exact prepared requests was made. Broader workload profiling and
unsupported-form costs remain necessary before claiming general performance.

Reproduction source, exact XML, and TSV are saved as `TransitionTimingMeasurementTest.kt`,
`followup-timing.xml`, and `followup-timing.tsv` in the evidence folder. The temporary test was
removed from the routine suite. The older first-pass timings used live rollback and must not be
presented as measurements of this isolated-copy implementation.

## Review and integration cost

Before integration of the newer main/work1 commits, against the recorded base, production changes span 22 files: 327 added lines and
342 removed. Tests span 72 files: 2150 added lines and 615 removed, including formatting of expanded explicit
commands and the new experiment checks. These counts exclude documentation and are not a measure
of conceptual complexity. The substantial caller migration is the main cost of 1a/3/5.

The final source review follows REVIEW_CRITERIA: no Canon-specific logic was added to the engine,
no second effect evaluator, operation framework, compatibility wrapper, or gameplay helper was
introduced. The internal trial factory reuses existing World copies and real transaction execution.
The Player-policy check is private Agent code. Temporary tracing and timing tests are absent from
production and the routine test tree. Existing outcome checks remain; neutral-placement commands
were replaced with observed-state checks or exact net gains on their triggering event.

Recommended review units are: (1) 7 and its session-ownership tests; (2) repaired 1b;
(3) 1a with explicit caller commands; (4) 3 with numeric 2 and caller changes; (5) isolated finite
inference plus Admin handling together. Unit 5 should remain an explicitly limited experiment
until its added execution responsibility and costs are accepted. Shared-type THEN changes and a
production Agent reducer are not included. The evidence tests for 6 are useful independently.

## Additional review findings

A subsequent strict task-matching experiment preserved `cause` when comparing otherwise identical
tasks in `ActorEngine.uniqueMatchingTask` and `TaskLog.matchingTask`. Focused Agent and engine tests
passed, but `./gradlew build --continue --console=plain` exposed failures in Canon scenarios and
curated replays. Ecological Zone offered two identical animal rewards from different trigger events;
Frontier Town offered ocean placements with different causes; a World Government scenario offered
identical money gains caused by two ocean tiles; Synthetic Plasma Current had identical card tasks
from separate draws. The strict change was removed. Requiring IDs for all such calls would change
ordinary Player inputs, while ignoring cause can change event ancestry and application input checks.
The selected conservative interchangeability comparison therefore remains open; these cases are
evidence for specifying it, not approval of the old comparison. The exploratory build also found a
formatting violation in the temporary code, so it was not a passing full build.

The original temporary diff was removed without saving its patch. A reconstructed minimal production
patch is now in `_local/engine-transition-evidence/strict-cause-reconstructed.patch` (also under
`$TMPDIR`). It changes the two task comparisons described above and applies at this branch revision;
it is not a copy of the original temporary diff. With that patch applied,
`./gradlew :tfm-tests:jvmTest --tests 'dev.martianzoo.tfm.tests.cards.ViralEnhancersTest' --tests 'dev.martianzoo.tfm.tests.cards.FrontierTownTest' --console=plain`
failed 7 of 16 tests. The exact failing cases and messages are saved in
`_local/engine-transition-evidence/strict-cause-reconstructed-failures.tsv`. The patch was then
reversed, leaving the conservative comparison in place.

The retained code change sends a uniquely proven concrete instruction through
`narrowSelectedTask` instead of applying a separate direct resolution. Its disposable trial reaches
that same step through `doTask(candidate, taskId)` and `intersectTask`. The Admin priority probe now
uses the probed task's assignee engine, so the shared narrowing step also works when a Player engine
checks pending Admin work. No new inference rule was added.
The new Admin priority test failed before the probe fix and passed afterward. The focused
`TransactionUniquenessExperimentTest` and `TransactionTrialIsolationTest` run passed before the fix.
After reversing the strict-cause patch, the focused Canon tests and
`./gradlew build --console=plain --quiet` passed with the probe fix and split amount tests. These
checks do not establish uniqueness for the unsupported instruction forms listed above.

A raw-engine characterization confirms the optional-amount issue is below `TaskForm`:
`doTask(taskId)` consumes `Repair?` as zero, while an explicit `Repair!` succeeds and produces
`Notice` after the immediate removal repairs `HAS MAX 0 Repair`. An unconstrained `Notice?` remains
unresolved. Thus the selected contract would require a choice between zero and one for `Repair?`.
`Repair*` also resolves to zero, but its desired amount rule remains open. `QUANTIFIERS.md` explicitly
caps both optional and AMAP using invariant room in the current World; Canon's neutral
`PartyDelegate` placement uses mandatory gain because AMAP would gain zero before leader demotion.
The focused `AgentNarrowingExperimentTest` run passed. Characterize the Canon impact before changing
`Instructor.limitChange` or the documented amount rule.

The whole-branch review reproduced and fixed a missed form of experiment 1b: an explicit group
member `(TASK Player1 { First! } THEN Following!)` admitted First and Following, then incorrectly
executed both. Admission now distinguishes the submitted member from the tasks produced by its
normalized dispatch. Those tasks stay pending while other submitted members execute. The original
failing test and the passing ten-test group run are recorded in `review-group-before.log` and
`review-group-after.log`.

The review also corrected `narrowTask(taskId, ...)` documentation that still promised execution.
Four OtbGame20261009 production batches and Molecular Printing now retain their original expected
MC totals independently of the migrated commands' calculated amounts. Existing resource
checkpoints remain. No additional operation representation or public API was introduced.

The two "establish a required use" markers on reusable speculative state now have an answer, and it
differs by layer. `GameWorld.resetOverlay` and the engine's `SpeculativeWorld.reset` are required:
`ActorEngine.successfulNarrowing` creates one trial World and resets it once per concrete candidate
while proving a unique narrowing, so that capability cannot be removed independently of experiments
4 and 5. The agent-level session was not required: nothing in production constructed
`Agents.overlay()` or `SpeculativeAgents`, and the only callers were `WorldOverlayTest` and
`CardTrackingTest`. The transition now removes that wrapper, its recording-reset method, and
inheritance from `Agents`. Those tests construct `Engine.overlay` directly and retain their
checks for invalidation, history, reset effects, and anonymous card draws. No engine trial
capability or test scenario was removed.

The engine's `tryTask` overloads also had only Agent callers, apart from a direct restoration test.
That policy now lives in Agent: attempt `doTask` within timeline rollback, restore unfinished
handoffs, and leave unavailable work or requests needing choices pending. The engine restoration
test now checks `doTask` rejection directly; Agent tests check restoration after an immediate-effect
failure. Preliminary `/` evaluation now receives the same unavailable-work handling as execution,
removing the old discrepancy. Existing try inputs still reconstruct through Agent. The broad
`NotFullySpecifiedException` catch is retained carryover: exposing incomplete analysis distinctly
through try calls, probes, and Admin scheduling remains tracked in the transition plan.

## Fixed-state transaction measurements

`TransactionInferenceBenchmark` compares raw `ActorEngine.doTask` with the complete Agent call,
with Player policy `NONE`. JMH 1.37 on JDK 26.0.1 used two forks, two one-second warmup iterations,
and three one-second measured iterations per fork. Setup and restoration are outside the measured
call. Each invocation verifies its result and restores the same starting state.
These prepared Agent calls have no subsequent eligible Admin work; they do not measure the
additional nested priority checks caused by a queue of abstract Admin tasks.

| Scenario | Selected candidates | Expected trials | Raw ms/call | Agent ms/call |
| --- | ---: | ---: | ---: | ---: |
| Synthetic gain: 63 immediate-effect failures, one success | 64 | 64 | 1.721 ± 0.050 | 1.789 ± 0.052 |
| Optional removal, only Acquired Company present | 2 | 2 | 0.081 ± 0.002 | 0.080 ± 0.005 |
| Optional removal, Zeppelins and Worms present | 220 | 146 | 1.855 ± 0.047 | 1.950 ± 0.021 |

Errors are JMH's reported 99.9% intervals. Trial counts are derived from the current candidate loop
and fixed states, not runtime counters. The optional cases stop when zero and a positive removal
both succeed; neither measures a full sweep of the card domain. The sole-card case first benefits
from the existing present-component deduction. The broader card case remains abstract, and its
first successful card's position depends on catalog order. These results justify no universal
latency claim, search cap, or clock cutoff. Full output and the reproducible command are in
`_local/engine-transition-evidence/transaction-inference-jmh.log`; summarized output is beside it
in `transaction-inference-jmh.txt`. Opus reviewed the benchmark and confirmed these limitations.

## Integration of main and the work1 documentation commit

At the user's request, work1 first fast-forwarded to main `4974858d2`, then committed the original
design documents as `2b3ad9c8a`. The reviewed experiments were committed separately as `ca1b8d01a`.
This branch then merged work1, retaining the experiment's newer implementation descriptions and
report while adding main's replay changes and TODO. No experiment code was copied back into work1.

The merged replay inputs needed three straightforward adaptations to the removed combined-total
feature: Atmospheric Enhancers submits its two TR tasks separately; Investment Loan and Media
Group submit 10 MC and 3 MC; Imported GHG and Optimal Aerobraking submit their separate 3 Heat
gains. Source-backed final resource assertions remain intact. Main's new replay endpoint checks
are retained. These adjustments change replay commands, not engine or Canon behavior.

After integration, `:tfm-tests:replayTest :web:jsBrowserTest` passed all 23 curated replays and both
browser checks in 44 seconds. Formatting and `git diff --check` passed. Evidence:
`merge-work1-format.log`, `merge-work1-verification.log`, and `merge-work1-tests.tsv`.
The full build recorded below verified the unchanged production code before this integration;
optional archived suites remain disabled.

Main later advanced to `f1055a08b` with the Agent contract, Pets and Canon changes, and three
replay test updates. Work1 fast-forwarded to that commit. The experiment branch merged it as
`6c4686b61`, resolving four documentation conflicts while retaining the branch's findings and
main's Agent direction. No source file had a textual conflict. The merged branch passed a full
`./gradlew build` in 3m 51s: 462 tasks, including the curated replays and web browser checks.
The pre-merge documentation edits were restored afterward and are not part of that merge commit.

## Increments after the October 24 review

The user asked for one well-defined increment at a time, each verified and committed on its own.
Three landed on October 27, 2026. None advances experiments 4 and 5, and none touches
corrections.

`349676f33` closes the queue-edit half of the selected-task exclusion. `ActorEngine.addTasks` and
`ActorEngine.dropTask` now refuse while any task is selected, except dropping the selected task
itself, which is how a client abandons work it has started. A private `requireNoSelectedTask` sits
beside `requireNoOtherSelectedTask`; both methods' KDoc records the refusal. No field, exception
type, representation, or public API was added. `sneak` and `restoreTask` are unchanged because the
contract defers corrections separately, and
[ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) records the narrowed difference.

`TaskAssignmentCharacterizationTest.aSelectedTaskExcludesCompetingQueueEdits` is the only test in
the corpus that exercises a queue edit against a selected task: the whole suite passes with the
guard in place, so nothing else performs one. The guard was therefore verified by removing it. With
both guard calls removed the test fails at its `addTasks` assertion; with only `dropTask`'s guard
removed it fails at its `dropTask` assertion. Each half is independently proven, and in both runs it
is the only failure among that class's 32 tests. Those two runs are
`increment-01-guard-removed-failure.log` and
`increment-01-droptask-guard-removed-failure.log`. Their reported frame numbers are Kotlin
inline-function positions beyond the end of the 636-line source, not file lines; the assertions
themselves are adjacent.

`7962b4f84` answers the two "establish a required use" markers on reusable speculative state. The
finding is recorded under [Additional review findings](#additional-review-findings); that commit is
comments and documentation only.

Each increment passed a whole `./gradlew build`: 5m 13s and 4m 41s, 462 tasks each, no failures,
including `:tfm-tests:replayTest` and the web browser checks. `spotlessApply` changed nothing in
either. The second build's engine, Agent, and state test tasks resolved from cache because
comment-only edits recompile to identical bytecode. Logs are `increment-01-build.log` and
`increment-02-build.log`. `refs/claude/engine-transition-increments-2026-10-27` pins the branch tip
at this record, so the increments survive a later reset of the branch.

### Increment 03: remove an engine retry policy

Commit `f19079524`, based on `a0161506a`; recoverable patch:
`$TMPDIR/engine-contract-increment-03-a0161506a.patch`.
`ActorEngine.trySelectedTask()` had only one production caller, Player automatic execution in
`AutoExecLoop`. Its special results were Player policy decisions: an unfinished choice waits, while
current unavailability is reported as a dead end. The Agent loop now calls the engine's ordinary
`doTask(taskId)` and applies those decisions at that call site. `RecordedInput.Kind.TRY_SELECTED`
still identifies the automatic attempt in task recordings; its replay path already uses
`Agent.tryTask`. The public engine retry method and its inventory row were removed. No new method,
stored fact, or instruction form was added. The Agent and engine JVM suites and `spotlessCheck`
passed, followed by `./gradlew build --console=plain --quiet`, including the active replay and
browser checks. This change does not settle the separate optional amount or Admin waiting questions.

### Committed increment: operation completion belongs to Agent

Committed in `8ceb7706b` after base `f19079524`. `ActorEngine.requireComplete` was the only
engine method that decided whether an Agent operation spanning several tasks was finished. Only
`AgentImpl` called it. The check now uses the same World task and event views from inside
`AgentImpl`, and the public engine method is gone. Its existing pending-task, `MustCleanUp`, and
`FINALLY` behavior remains intact. This removes operation-completion policy from the engine API
without adding a model or another processing step.
The `FINALLY` completion convention itself remains open; moving its existing check does not approve
it as the final action-completion rule. `WorldTransaction.run` still coordinates call finishing and
callbacks. Moving that wrapper would require a new cross-module path to the World's callback and
history-navigation lock, so this experiment does not relocate it.
`./gradlew :agent:jvmTest :engine:jvmTest spotlessCheck --console=plain --quiet` and
`./gradlew :tfm-tests:jvmTest :tfm-tests:replayTest --console=plain --quiet` passed. This checks
the existing operation scenarios and curated replays after the ownership move; it does not prove
that the retained `FINALLY` rule is the intended completion rule.

### Committed increment: finite mandatory transmutation pairs

The engine's existing transaction-success trial now enumerates concrete source/destination pairs
for a fixed mandatory transmutation, just as it enumerates concrete types for fixed mandatory gains
and removals. Equal concrete types are excluded by the Pets transmutation rule. Every remaining
structurally compatible full-form pair goes through the ordinary task execution path in a reset
disposable World; shared type bindings are checked before the trial, and immediate effects during
it. Malformed concrete pairs (such as a multi-count transmutation into an Atomized type) are
excluded before trial. Compact transmutations remain unresolved. A second successful pair remains
ambiguous.
This changes explicit engine requests and mandatory Admin work; Player automation policy is still
separate. Optional/AMAP amounts, compound alternatives, and unbounded numeric choices remain open.
The pair domain can grow as the product of its source and destination domains; no general search
resource limit or incomplete-analysis result has yet been designed.

The engine scenarios show a unique legal pair despite an absent source and a forbidden
destination, two legal pairs causing ambiguity with all trials restored, a shared type choice
with both source types present, and a malformed Atomized destination that does not hide a valid
destination. The dead-end scenario ensures incompatible shared-type pairs cannot make Admin wait
for work that cannot ever succeed. A first whole-engine run exposed
invalid self-pairs in `ReflexiveTransmutationTest`; excluding equal concrete types restored the
591-test engine suite. The full `./gradlew build --console=plain --quiet` then passed, including the
curated replays and browser checks. Subsequent focused tests exposed the pair compatibility and
malformed candidate defects, which this experiment now addresses. The final verification for this
revision was a passing 12-test `AgentNarrowingExperimentTest` and a passing
`./gradlew build --console=plain --quiet`. The first full-build attempt timed out in
`:web:jsBrowserTest` with a skipped test and no assertion failure. Browser history, the selected
replay, and their combined browser task each passed on retry; a second full build then passed.
This experiment and its review fixes are committed through `8ceb7706b`.
The earlier patch against `f19079524` was a recovery snapshot before those commits, not an
additional current work item.

## Verification and recoverability

The additional review passed `./gradlew build --continue --console=plain` in 3m 55s: 462 tasks,
180 executed, 17 from cache, 265 up-to-date. Results: 585 engine tests, 64 Agent tests, 1354 card/rule
tests (26 existing skips), 23 curated replays, and two web browser checks, with no failures.
See `review-format.log`, `review-build.log`, and `review-tests.tsv`. The figures below preserve the
preceding follow-up checkpoint; the reviewed commit also includes the additional fixes above.

The formatted final branch passed `./gradlew build --continue --console=plain` in 4m 22s:
462 Gradle tasks, 182 executed, 15 from cache, and 265 up-to-date. This includes the active web
browser tests and production JavaScript packaging; unaffected Pets browser results were up-to-date.
Disabled optional suites were not enabled. The final source diff also passes `git diff --check`.

Final relevant JVM results: 584 engine tests, 64 Agent tests, 1354 card/rule tests (26 existing
skips), and all 23 curated replays, with no failures. The isolated timing run and complete card/rule
suite had already passed in `followup-timing-and-cards2.log` (2m 47s). Formatting passed in
`followup-format.log`; the final build is `followup-final-build.log`; exact result counts are in
`followup-final-tests.tsv`. Initial logs describe intermediate attempts, not final failures.

`followup-final-all.patch` contains all tracked and new documentation/source/test files against the
recorded base. `followup-final-runtime.patch` contains production and tests only. Both were checked
against the unchanged base index without staging anything. `followup-final-manifest.tsv` records
their hashes. They are complete alternatives, not patches to apply consecutively. The original
worktree was subsequently updated and its documentation committed at the user's request. These
patches precede the subsequent review fixes; the user-requested experiment commit is the complete
reviewed snapshot before integrating the newer main/work1 commits.

Recoverable follow-up snapshots include `followup-07-1b.patch`, `followup-01a-complete.patch`, and
`followup-03-02-migrated.patch`. Original numbered patches are alternative cumulative snapshots
against the base, not a sequence to apply. Do not apply them over this dirty working tree.

Payment TODOs remain: standard-project purchase events and card-play results at payment completion.
The global authored-action redesign is excluded. Completed ordinal work was not reopened.


### Retain selected groups until execution

The owner selected retaining partial engine choices and keeping the parent until execution for
selected groups and dispatches, matching `EACH`. A follow-up design review explicitly distinguished
selection from admission: literal groups still split during admission, and pure ordinary dispatches
still perform immediately at admission/promotion. An attempted removal of that dispatch rule was
reverted after reviewing the owner's earlier intentional decision and its Canon consequences.

`Task.instruction` now holds the existing `InstructionTree`; only a selected task can store a group.
Selection and narrowing edit the same task rather than consuming it, admitting children, or releasing
its continuation. Execution admits the children at ordinary priority. Explicitly submitted groups
still execute only their admitted members, excluding dispatched bodies, reactions, and continuations.
Obsolete nullable selection returns and splitting-error branches are removed. Partial target choices
continue to support AMAP evaluation; `Ok` remains selected until execution too.

Opus/high agreed with that distinction after a follow-up correcting an overstatement about deferred
`EACH`: expansion before deferral ends is wrong, but no verified Canon outcome currently requires
execution-time expansion over expansion when work becomes ordinary. The selected execution timing
remains; earlier expansion is an alternative requiring an explicit state-timing decision.

The event codec stores selected groups as arrays, preserving empty/singleton structure that printed
Pets alone would lose. Focused tests cover full/short export and replay with a selected group,
continuation release only at execution, selection priority, and retained dispatch parents.

Opus/high found a Player-policy gap: groups with abstract children must be executable structurally,
like `EACH`, without choosing their children. The policy and initialization checks now allow that.
The bootstrap scenario also exposed an older mismatch: Admin's unique-success proof could succeed
but initialization still stopped merely because the instruction was abstract. Initialization now
executes abstract Admin work only after a successful proof, while preserving named diagnostics for
ambiguity and incomplete-analysis reporting in both mixed queue orders. Opus reviewed the fixes and
reported no remaining blocker.

Verification passed 31 state tests, 75 Agent tests, 50 script tests, 1358 Canon/rule tests (26 existing
skips), and all 23 active replay exports with full/short reconstruction. The final runtime passed
all 632 engine tests; the subsequently added mixed-order bootstrap test passed in the focused
Initializer suite, and `spotlessCheck` passed. Browser history and the selected browser replay both
passed on a focused retry before the final bootstrap proof refinement. The preceding full build
reported 2h 39m elapsed, 12 Canon failures from implicit no-op completion, and a browser-process
failure without test XML. The Canon calls now explicitly execute those empty offers with all original
outcome assertions retained. The focused retry resolved the browser failure; its original cause is
unexplained. Do not describe the failed full-build invocation as a passing full build.

## Consolidate corrections as exMachina

The owner approved explicit System edits with structural invariant validation and no additional
unfinished MustCleanUp components. `Agent.exMachina` now owns the client command, Audit for Players,
input recording, application validation, and the unfinished-work check. Admin corrections are also
recorded, without an owned Audit. Elementary correction mechanics remain public across the separate
engine/Agent modules. All `sneak` callers migrated; the old Agents extension, fixture extensions,
unused attribution parameter, and arbitrary `restoreTask` command were removed.

The owner asked for an expedient consolidation rather than fixing all correction semantics.
Corrections preserve selected instructions verbatim, including partial group choices and already
calculated amounts. They do not reselect or execute pending work. This removes the history-based
restoration path that failed after further narrowing. Correction-time recalculation is not promised.

Opus/high reviewed the change and the fixes in a follow-up. Admin's owned Audit and shifted source
positions were reproduced and fixed. The reviewer accepted the cross-module visibility constraint
and found no remaining correctness blocker. Its suggested additional Player System-edit coverage
and documentation corrections were applied; moving recording-independent correction checks into
engine was optional and not needed for this consolidation.

Verification: 633 engine, 80 Agent, 50 script, and 1358 Canon/rule cases (26 existing skips) passed.
All 23 active replays passed with full/short export and reconstruction. Selected-group export tests
also perform both Player and Admin corrections before export and execute the preserved task after
replay. Focused correction checks cover immediate effects, suppressed queued effects, unfinished-work
rejection and rollback, removal of existing unfinished work, and preservation of partial choices.
The first broad attempt exposed the Audit/source-location bugs and was stopped; the corrected broad
run passed in 3m18s. Tests' gameplay assertions were retained.

## Preserve context-class decisions and grouped selections in replay

The final task-matching audit found that `doTask(instruction, contextClass)` executed correctly but
omitted the class filter from recorded inputs. A focused test reproduced failure of fresh full/short
replay with two Coin tasks carrying different continuations. Recorded commands now retain that input;
replay calls the corresponding Agent overload. This also preserves class-filtered combined totals.
When execution hands a task to another Player, recording uses the existing task selector for the
selection command, captured before its instruction and assignee change. The context field is omitted
from that rewritten selection command. Task-overload grouped selections now replay through
InstructionTree parsing, removing the obsolete individual-Instruction assumption. File format 5
prevents an older reader from silently ignoring the new context input.

Opus/high found no blocker. The unused handoff context field and prose issues were fixed. A follow-up
withdrew the suggested context-only snapshot filter: an already-selected task takes precedence over
that filter in the engine, so the broader temporary snapshot is necessary for that path.

`./gradlew build -PreplayExports` passed in 4m51s, including all browser checks and generated artifacts.
It passed 31 state, 83 Agent, 633 engine, 50 script, 1358 Canon/rule cases (26 existing skips), all 23
active replay exports/reconstructions, and both Web browser tests. The final handoff-field omission
then passed Agent/state suites and formatting checks. No assertion was weakened.

The same final review identified a remaining Admin missing-input reporting gap. The implementation
checklist marks it open; do not interpret the passing build as completion of that requirement.

## Final Admin input reporting and Canon waiting audit

The final Opus acceptance audit found that a completely stalled Admin pass swallowed missing
application input. `runAdmin` now remembers the first `AgentInputRequiredException`, retries after
other Admin progress, and reports it if the pass stalls, before Player work. A Player request and
earlier successful Admin transactions are restored together when that error escapes. No new error
class, processing phase, or policy API was introduced.

`AdminInputTest` verifies a stalled pass with and without eager Player work, retry after the client
supplies input, retry after another Admin task makes progress, and restoration after a Player
request. The last scenario reproduced silent success before the fix. Raw setup in two new tests
initially lacked explicit quantifiers; those test setup errors were corrected before verification.

The named Canon waiting cases already have concrete scenarios:

- `CoreRulesTest.Eight plants place greenery and raise oxygen and terraform rating` starts plant
  conversion under manual Player policy, rejects greenery placement before payment, accepts the
  eight-plant payment, then places greenery and raises oxygen/TR. The pending Admin machinery must
  allow the Player's payment and then resume; it must not pre-execute placement or reject the
  payment merely because Admin work remains.
- `GameFlowTest.researchMakesEveryPlayerQueueAvailableTogether` lets Player 2 draw/discard, then
  Player 1 draw/discard, before either finishes buying. `PaymentScope` has a shared maximum of one,
  so Player 1's scope waits while Player 2 pays for two cards; Player 1 then pays for a card. This
  exercises the existing temporary-capacity wait and retry after progress, with no client Admin
  driver call. Both final hand sizes are asserted.

These are gameplay waits that a Player action can resolve. They are distinct from an application
input exception that Admin must report. Both Canon scenarios, all Agent tests, and CardTracking
passed with the missing-input fix. This establishes the named cases, not every possible Admin wait.

Opus/high found no blocker in the Admin fix. Its claim that no replay supplied card names mid-operation
was disproved by the complete build: `StaticPressureStreamTest` supplied MoholeLake after requesting
its draw. The existing name input now precedes `doTask("ProjectCard FROM Science")`; no card identity
or game assertion changed. The focused replay passed with full/short reconstruction. The research
scenario also now explicitly rejects Player 1's payment before Player 2 frees the shared capacity;
that strengthened scenario passed. Optional Player policy still leaves its own input-dependent work
pending, as existing carryover; the new reporting requirement applies to Admin.


## Scoped verification — October 29

**Status correction, October 30:** this verification established the implemented coverage below;
it did not establish completion of the full selected contract. The previous final-acceptance wording
was too strong. Unsupported analysis and Admin waiting semantics still require explicit assessment.

The implementation checklist was verified within the documented finite proof domain.
`./gradlew spotlessApply build -PreplayExports` passed in 3m37s on the final runtime and replay code.
Verification includes 86 Agent tests, 31 state tests, 633 engine tests, 50 script tests, 1358
Canon/rule cases (26 existing skips), all 23 active full/short replay exports/reconstructions, and
both Web browser tests. The earlier final-build attempt failed on the now-corrected card-input
ordering; this passing invocation supersedes it. No gameplay assertion was weakened or test skipped
to obtain the result. Opus/high reviewed each significant increment and the final fixes; no code
blocker remained. Final review criteria were applied to the complete diff.

The completed transition does not claim general inference or general action-completion search.
Unsupported proof forms still return distinct incomplete analysis. Player-policy redesign,
engine-installed policies, operation-lifecycle replacement, broader waiting-case classification,
and correction-time recalculation remain documented future work. No merge or push is implied.


## Strict ordinary Admin experiment — October 30

**Status: investigated, not adopted.** The owner asked to explore requiring every nonempty pass
through ordinary Admin tasks to make progress, throwing when none succeeds. Experiments below
started at `51ab64824`. All runtime, Canon, and test changes were temporary and restored exactly.
The selected Player task still has priority; deferred Admin tasks join this rule only after promotion.

### Rule tested and observed failures

The runtime experiment removed the three exceptions that allowed unsuccessful Admin work to remain:
abstract choices, unmet gates with Player work pending, and other unavailable work with Player tasks
pending. Each failure was retained; success restarted the pass. Incomplete-analysis and missing-input
errors retained their distinct reporting. Whole-call restoration remained in place.

Command: `./gradlew :engine:jvmTest :agent:jvmTest :tfm-tests:jvmTest :tfm-tests:replayTest -PreplayExports --continue`.

| Suite | Cases | Failures | Existing skips |
| --- | ---: | ---: | ---: |
| Engine | 633 | 5 | 0 |
| Agent | 86 | 2 | 0 |
| Canon/rules | 1358 | 2 | 26 |
| Active replay export/reconstruction | 23 | 0 | 0 |

The seven synthetic failures fall into two groups:

- Admin was used as a stand-in for a Player choosing among alternatives:
  `ReflexiveTransmutationTest.independentAbstractChoicesMustNarrowToDifferentTypes`,
  `EffectAtomizationTest` queued item choices, `PerAtomizationTest` computed item choices, and
  `SafeAutoExecTest.adminDoesNotLockAnAbstractTaskWhileAManualPlayerCanAct`.
  Keep genuine choices on a Player; explicitly test that an unresolved Admin choice throws.
- Tests deliberately allowed Admin to wait for a Player prerequisite:
  `TaskLogTest.adminWaitsForTheNextRecordedPlayerTask`,
  `TaskAssignmentCharacterizationTest.eagerAdminWaitsForTheGateThenReturnsThePlayersChoice`, and
  `TransactionTrialIsolationTest.mixedFailuresWaitForPlayerWorkRegardlessOfCandidateOrder`.
  A prerequisite followed by `THEN` can create the System work after the prerequisite completes;
  raw-engine failure/isolation assertions remain useful independently of the Agent waiting policy.

Negative System amounts that wait for another Admin gain still pass: success elsewhere in the same
pass permits a retry. No change to scoring or general retry order was needed by this run.

### Research purchases: a global capacity limit

`GameFlowTest.researchMakesEveryPlayerQueueAvailableTogether` fails when the second Player finishes
choosing cards. Player 2 already has a payment open; Player 1's fixed System work tries to create
another `PaymentScope`. Its existing `HAS MAX 1 PaymentScope` limit applies across the whole table.
The old Admin loop leaves that start waiting while Player 2 pays. Strict Admin instead rejects the
call that creates the conflicting work.

Putting the research payment start in `LATER` was tested and still failed: both starts become
ordinary together. The first opens a Player payment task; the other start then has no successful
Admin action available to release the occupied capacity.

A second prototype removed the global limit from `PaymentScope` and added
`HAS MAX 1 PaymentScope<This>` to `Player`. The focused scenario passed under strict Admin:
Player 1 pays while Player 2's bill is still open, then Player 2 pays. From 20 MC each, their ending
cash is 17 and 14, and their card counts are 1 and 2. This demonstrates independent research
payments; it does not verify every cross-Player discount, alternative resource, or payment listener.

### Pluto: repeated draw-and-discard bonuses

`ColoniesRulesTest`'s repeated-Pluto scenario gives Kim two colony bonuses. Current Pets creates
`PlutoLock! THEN ProjectCard THEN -ProjectCard THEN -PlutoLock!` for each bonus. Admin starts the
first but cannot acquire Kim's second lock until Kim discards for the first. Strict Admin throws
before returning to her draw/discard work.

An owner-explicit `LATER` around each sequence was tested and still failed on `PlutoLock<Kim>`:
both sequences are promoted together. Delaying all starts does not serialize them.

A second prototype kept `PlutoLock` as System bookkeeping and removed `System` from the existing
`GainColonyBonus` signal. Each Player explicitly starts a bonus; only then does Admin acquire its
lock. The focused scenario passed under strict Admin. It additionally rejected Kim's attempt to
start her second bonus before completing the first, then allowed that start after her discard.
The original draw/discard, owner, final card-count, and lock-cleanup assertions remained.

This prototype changes all colony bonus starts and adds a Player command. `GainColonyBonus` is
currently documented as an internal signal; turning it into a Player action changes its intended
meaning as well as its assignment. It has not been validated against other colony interactions
or automatic Player policies. A Player-owned lock would expose
lock/unlock machinery directly, so it is not the preferred client interaction. The demonstrated
direction is Player control over taking a bonus, with Admin bookkeeping created after that choice;
the smallest satisfactory final interaction remains to be settled.

### Where LATER fits

Use `LATER` for work that genuinely belongs after all ordinary work, such as the existing research
cleanup of cards left in Selecting. Do not automatically defer a failed Admin task. A deferred task
may be needed by ordinary work that prevents its promotion; alternatively, several deferred tasks
may conflict as soon as their batch is promoted. Create work after its actual prerequisite when
that expresses the rule more accurately.

### Review and restoration

Opus/high confirmed that the runtime experiment implements the proposed strict-pass rule and
independently identified the two Canon capacity conflicts and the same-batch problem with `LATER`.
In the follow-up, Opus agreed to prefer exploring Player-controlled bonus starts over exposing
lock/unlock commands. It emphasized that the existing internal bonus signal would acquire a new
Player-facing meaning. It supported the per-Player payment limit as a candidate, while noting that
the focused cash-payment case does not cover alternative resources, pricing listeners, or every
setup path. Neither prototype has had full-suite verification or is ready for adoption.

The exact original runtime, Canon, and tests were restored before concluding the exploration;
`git diff --exit-code` confirmed restoration before this report was added. The original four-suite
verification then passed, reusing Gradle's cached results for the restored unchanged test inputs.
No new skip, weakened gameplay assertion, committed runtime change, or full-contract completion
claim follows from this experiment.


### Owner clarification: payment scope and the remembered Pluto handoff

The owner chose to retain the global payment limit for the current single-client workflow. Research
coverage now completes each Player's choice and payment as a batch before starting the next purchase,
retaining card counts and checking the respective 3 MC and 6 MC charges. Per-Player payment capacity
remains an unadopted prototype, not required follow-up work.

The owner expects colony response sequences to await the active Player's selection, then hand the
selected work to the colony owner before further work can proceed. Current code differs:
`PendingTask.fromEffect` routes to the effect owner or changed component owner directly, and
`TaskQueues.assignConcreteChangeToAdmin` assigns a concrete System first step to Admin at admission.
The lock gain is therefore already Admin work before any Player selects a bonus. Finishing that
step admits the draw as a new, unselected task for Kim; selection does not span a `THEN` continuation.
`PlutoLock` still prevents Kim's second draw from being offered before her first discard.

History explains the discrepancy. Commit `08ee7dcd5` (October 3) added the per-owner Pluto lock.
Commit `92f4ee520` (October 10) made `PlutoLock` System bookkeeping and removed explicit lock calls.
Commit `4847065e0` (October 12) removed the separate task controller and selection recipient,
replaced that arrangement with direct assignment/`TASK`, and removed Stan's selection before Kim's
Pluto draws and discards from the test. The intended active-Player-first arrangement remains labeled
unimplemented in `IDENTITY.md`. The earlier exploration should have surfaced this discrepancy
before recommending a new colony-bonus start interaction. The October 30 log audit found that the
owner had explicitly objected to losing the active Player's timing decision before that commit.
The required behavior is settled; the implementation remains to be chosen. No Pluto runtime change
is made by this clarification.


### Active-Player handoff test restoration — October 30

The owner requested restoring the tests before outlining implementation options. This is an
intentionally failing acceptance-test change; no runtime workaround, ignored test, expected-failure
wrapper, or test helper supplies the missing behavior. The required behavior and design options
are in [IDENTITY.md](IDENTITY.md#required-active-player-handoff).

Audit against 4847065e0 and its parent:

| Removed coverage | Restored location |
| --- | --- |
| Concrete/abstract owner responses, rejection of the active Player's choices, TaskForm handoff, impossible owner execution | TaskDelegationTest (four restored scenarios, without assignee-field transition assertions) |
| Owner automatic policy waits until the active Player releases its task | TaskDelegationTest.theOwnersAutomaticPolicyWaitsForTheActivePlayersHandoff |
| Original Player and different recipient survive System work | TaskDelegationTest (two scenarios formerly in TaskAssignmentCharacterizationTest) |
| Recipient survives System removal without an authored TASK workaround | TaskAssignmentCharacterizationTest.playerChoosesWhetherAndWhatToRemoveBeforeAdminExecutesIt |
| Active Player orders a later explicit BY stage | TaskAssignmentCharacterizationTest.resolvedOuterByRemainsOnEverySeparableThenStage |
| Owner resources and production after another Player's action | AphroditeTest, ArcticAlgaeTest, CapitalTest, EcologyExpertsTest, LakefrontResortsTest, SpliceTacticalGenomicsTest, SolarLogisticsTest, VerminTest |
| Compensation, disease, and disease losses | MonsInsuranceTest, PharmacyUnionTest |
| Active Player offers an abstract resource; owner chooses and executes it | Both affected PhilaresTest scenarios; the greenery scenario also rejects premature owner work and active-Player narrowing |
| Optional responses, payment, and later rewards | NeptunianPowerConsultantsTest, StJosephOfCupertinoMissionTest, the two retained interrupted-payment FloodingTest scenarios |
| Active Player orders the area-choice handoff, then the first Player chooses for the card owner | IcyImpactorsTest |
| Active Player retains the timing choice after Admin performs a global change | WorldGovernmentAdvisorTest |
| Premature Enceladus claim, Pluto draws/discards, Titan resource targets | ColoniesRulesTest |
| Recorded Arctic Algae, Neptunian, Pets, and city-production handoffs | Game20230521Test, OtbGame20260904Test, OtbGame20261009Test |

The old computed/raw-resource payment calls were adapted to the current explicit Pay API.
The former optional remaining-cash payment in the Flooding scenario is now mandatory; after the
attack makes it unaffordable, the restored handoff attempt itself must be rejected.
Removed field assertions and manual Admin execution were not restored. In particular, the old
Player1 selection of a concrete BY Admin continuation stays removed under the selected automatic
Admin rule. Own-player concrete selections and obsolete payment/corporation machinery in that commit are unrelated to the lost
handoff and retain today's explicit execution calls.

Verification compiled the restored tests and ran 39 engine checks, 103 card/rule scenarios,
and three replays: nine engine failures, 34 card/rule failures, and all three replay failures,
with two existing card/rule skips. The Icy Impactors handoff now uses the current ocean dependency
syntax; a focused rerun confirms its failure is the active Player's missing task. The impossible-owner
generic fixture explicitly disables Player2's automatic policy so its failure identifies the missing
handoff access rather than automatic execution. Formatting and diff whitespace checks pass.
Runtime behavior remains unchanged.
No successful export/reconstruction claim is made while the three restored replays fail.

The former ignored Flooding tests demanding a lock through an entire payment are not reinstated:
that is a separate completion rule from the current-step handoff. Their two present-day scenarios
retain the known interrupted-payment behavior and now restore the active Player's initial handoff.

Opus/high's first review found additional System-removal coverage and representation-specific
assertions that would prematurely exclude the owner-assigned design option. The SystemChoiceA
workaround was removed, tests now assert who can act instead of assignee transitions, and a new
scenario checks that the owner's EAGER policy waits for the active Player's handoff. The review also
tightened the unaffordable-payment error to NotNowException and made the Enceladus rejection name
Kim's card explicitly. Solar Logistics and World Government Advisor were restored during the audit.

Opus's follow-up agreed with retaining numeric identification of fixed payments, leaving unrelated
exception-type changes alone, and distinguishing Pluto's early lock acquisition from the restored
draw/discard checks. The second unaffordable-payment handoff was tightened too; the abstract
reaction now rejects premature owner execution of its follow-up, and TaskForm handoff explicitly
checks selected state. The suggestion to remove the EAGER-policy test was rejected: the live test
already observed the owner's policy executing before release, and AutoExecLoop runs the configured
Players' policies from the shared session after a top-level Agent call. This synthetic game has
no persistent action component or deferred cleanup keeping it non-idle.


### Focused NOW/SOON handoff implementation — October 30

The owner selected the instruction-based option after rereading the October 13 proposal. This
increment keeps THEN, Task.then, LATER/FINALLY batching, and BY. It adds NOW for a selected recipient
task and renames TASK to SOON for ordinary recipient work. Both require execution; selection alone
does not admit their body. There is no compatibility keyword or new per-task recipient field.

The existing ActionScope implements generic TaskScope. Admission gives its owner a NOW wrapper for
another Player's work. The original complete instruction goes inside that wrapper; it is not rebuilt
from a separated first step and continuation. Matching can identify an unchanged current step or a
fixed calculated payment, but cannot choose the receiving Player's resource, target, or amount.

Pluto explicitly encloses lock/draw/discard/unlock in NOW. The active Player starts the bonus;
Admin acquires its lock and returns a selected draw to the colony owner. After the draw the active
Player schedules the discard, then Admin unlocks. A second bonus for the same owner fails and rolls
back while the first lock remains; another owner's bonus can run meanwhile. The focused scenario
checks these states directly. Selected Admin failures reject the Agent call; ordinary Admin waiting
exceptions remain as before. The separate strict ordinary-Admin-pass proposal is not adopted here.

Restored card and replay handoffs now use explicit NOW execution in place of selection. Generic
fixtures declare a TaskScope to represent the active action. Synthetic unowned System fixtures now
name their recipient with NOW, or use an owned System context: the chosen design does not restore a
hidden initiating-Player field for ownerless effects. Their tests still check recipient authority,
selected handoff, and execution attribution. This adapts the earlier restoration's System fixture;
it is not a claim that an unowned effect infers a lost recipient automatically.

Opus/high's first review identified a continuation-owner risk. Admission now performs LATER/FINALLY
before reassigning work, and wraps the original full instruction for other sequences. A focused
SOON-followed-by-reward test verifies the continuation still belongs to the original recipient.
The suggested return to automatic SOON admission was rejected because execution is the chosen
handoff point. Opus's follow-up agreed and found no remaining concrete issue. Its calculated-metric
question was checked against Instructor and covered by five items yielding two rewards under a
per-two metric, with the receiving Player choosing the reward through TaskForm.

NOW's selected recipient also retains a LATER/FINALLY first step until execution. A focused test
checks the recipient acts before competing work, then the deferred body waits normally. That test
exposed a pre-existing path that attempted to create a selected deferred Task during narrowing;
that path now rejects with TaskException before constructing invalid task data.

Replay/export verification exposed the obsolete TaskLog conversion of an executed handoff to a
CHOOSE input. It is removed: replay must execute NOW to create the recipient's selected task. The
recorder now retains the original command kind and context-class filter, while recording the
recipient's execution separately. A focused NOW/EAGER-owner recording test reconstructs with Player
policies disabled; id-based BY and context-class recording coverage remains. Opus reviewed and
accepted both this removal and the deferred-narrowing guard. ActiveProtonFlowTest and Wsbg2025Test,
plus later points in OtbGame20261009Test, also needed explicit handoffs at existing reward points.
No game facts, outcome assertions, or test helpers were changed to supply missing engine behavior.

Final verification: `./gradlew build -PreplayExports` passed. The complete engine suite has 644
passing checks; Agent has 87; card/rule verification has 1,332 passing scenarios and the same 26
existing skips. All 23 active replay tests pass with full and short exports and reconstruction
(46 generated game files), including all restored replay handoffs. Both Web browser checks pass;
the root build also completed the included Pets build and browser checks. No new skip or weakened
outcome assertion was introduced.

Final-diff review against REVIEW_CRITERIA found no outstanding rejection criterion. Production
changes add no per-task recipient field, no second action-scope component, no gameplay helper,
and no THEN/AND replacement. The earlier research choose/pay edit in GameFlowTest is unchanged
and is kept separate from this handoff commit. The broader engine-contract open questions remain
in TODO; this result establishes the focused handoff rule, not general completion analysis.


### Strict Admin after NOW/SOON — October 30

Repeated the strict-pass experiment at `9835f0210`, with the existing uncommitted research test
completing each Player's choice and payment together. No Canon or test changes were made during
this experiment. The temporary AutoExecLoop patch removed the exceptions for unresolved choices,
unmet gates, and unavailable work with Player tasks pending. A successful task restarts the pass;
a complete unsuccessful pass throws. Selected Player priority, deferred-task promotion, whole-call
restoration, and distinct incomplete-analysis/application-input errors remain unchanged.

**Functional conclusion:** no remaining game-rule or replay failure was found. Pluto's active
Player controls the handoff, the owner executes through their own Agent, the lock prevents a second
unfinished bonus for that owner, and research purchases finish with the correct cards and money.
All active recorded games export and reconstruct successfully under strict Admin.

Verification under the strict rule (historical counts before the November 1 deletions):

| Suite | Cases | Failures | Existing skips |
| --- | ---: | ---: | ---: |
| Engine | 644 | 5 | 0 |
| Agent | 87 | 2 | 0 |
| Canon/rules | 1358 | 0 | 26 |
| Active replay export/reconstruction | 23 | 0 | 0 |
| Script | 50 | 4 | 0 |
| Terminal | 4 | 0 | 0 |
| Web browser | 2 | 0 | 0 |

Commands: `./gradlew :engine:jvmTest :agent:jvmTest :tfm-tests:jvmTest :tfm-tests:replayTest
-PreplayExports --continue`, then `./gradlew :script:jvmTest :repl:test :web:jsBrowserTest --continue`.

Eleven additional engine, Agent, and script checks failed because they encoded the previous Admin
behavior. They established no broken Canon rule or required client feature. The November 1 owner
clarification rejects using those checks as design constraints and directs their deletion. The
previous recommendation to preserve or rewrite every such test is withdrawn. Retain or add tests
only for a concrete functional requirement; do not recreate obsolete mechanics in replacement
fixtures merely to retain their test counts.

No additional Canon conflict was observed. Pluto's restored active-Player handoffs, owner's own
execution, and per-owner lock checks pass. Batched research retains the global payment limit and
passes its card-count and cash assertions. All 23 active replays pass with 46 full/short exported
game files and fresh reconstruction.
No additional LATER wrapper or THEN/AND redesign was needed for these tested game scenarios.
This is evidence for the active suites, not exhaustive proof for all possible game states.

The temporary runtime patch was reversed exactly; byte comparisons confirmed AutoExecLoop and the
pre-existing research test match their saved inputs. Repeating all seven suites with the restored
runtime and `-PreplayExports` passed: 2,168 cases, zero failures, and the same 26 existing skips.
At the time of that experiment, the eleven incompatible cases were not changed or skipped.
The strict rule remains an experiment; its functional results support adoption without additional
game-model changes in the tested scenarios.

Opus/high reviewed the patch and confirmed the strict-pass implementation. It noted that the rule
also rejects waiting for an automatic Player: Admin runs before Player policies. That is the
proposed P1 priority, while selected Player work retains P0. After receiving the complete results,
Opus withdrew its suggestion to test separate choice-only and prerequisite-only strict variants;
the combined run had already identified no remaining functional failure.

### Owner-directed test deletion — November 1

Deleted the eleven cases identified by the strict Admin experiment, plus setup declarations and
imports used only by those cases. This follows the owner's instruction that functional tests
establish the requirements and that obsolete implementation expectations should be deleted.
No replacement synthetic cases were added. The existing research batching edit remains separate.
The runtime is unchanged by this deletion.

Verification: affected engine, Agent, and script suites and formatting checks pass. The unchanged
functional suites also pass: 1,332 card/rule scenarios with the same 26 existing skips, plus all
23 active replay export/reconstruction checks and 46 generated game files. Gradle reused the
unchanged card/rule and replay results. Opus/high confirmed that the diff deletes exactly the
requested eleven cases and their unused setup; its formatting finding was fixed before the
successful verification. Final review found no production change or unrelated test deletion.
