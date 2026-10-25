# Engine transition experiment results

Status: authorized follow-up reviewed for the user-requested commit, October 24, 2026. The selected
destination is still incomplete. This commit records the experiments and review fixes; it does not
publish them or merge them into main. The branch is
`codex/engine-transition-experiments`, at base `c14c000b23f143f0f672ba2b94ae21966ff245d7`, in
`/Users/dev/solarnet/engine-transition-experiments`. The initial work1 design documents were copied
here. During review, the user requested integrating main and committing those design documents in
work1; that documentation commit is `2b3ad9c8a`, based on main `4974858d2`. The code and its review
fixes belong to the experiment checkout; work1's commit contains documentation only.

[ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns the destination;
[ENGINE_TRANSITION_PLAN.md](ENGINE_TRANSITION_PLAN.md) owns the investigation checklist.
Evidence is in `_local/engine-transition-evidence/`. The original report is preserved there as
`first-pass-report.md`; its parked/retained labels describe the first pass only.
Two Opus/high discussions are preserved in `claude-review/`. This follow-up implements the agreed
order: 7, repaired 1b, 1a, 3 with numeric 2, then 4/5 and further evidence for 6.

## Current disposition

| Experiment | Current branch | Merge assessment |
| --- | --- | --- |
| 7 Surrounding Agent call | Application checks belong to an Agents session; cross-session behavior tested. Callback behavior characterized. | Good independent change. Does not settle callback failure recovery or FINALLY-based completion. |
| 1b Submitted groups | Executes the exact IDs returned when group members are admitted; excludes dispatch bodies, reactions, and continuations. | Good independent change; repairs the initial queue-difference approach. |
| 1a Combined scalar totals | Deleted engine arithmetic and flag; Agent translates explicit totals into separate engine calls. Production tests use totals again; unrelated rewards stay separate. | Worth merging with the Agent restoration below. |
| 3 Selection | Selection/narrowing do not execute component changes; callers explicitly execute. | Worth merging with numeric 2 and caller migration. Structural selection remains an open question. |
| 2 THEN | Shared numeric X must be supplied explicitly; no reverse inference from the requested first-stage amount. | Numeric change is suitable with 3. Broad shared-type removal remains parked. |
| 4 Transaction uniqueness | Fixed mandatory gains/removals enumerate concrete types and try complete transactions in disposable copies. | Experimental integration candidate, not complete general inference. Review cost and unsupported forms before merging. |
| 5 Admin | Uses the same unique-success rule; no later Admin work is executed to justify a candidate. | Keep together with 4. Canon declarations did not need changing for the tested cases. |
| 6 Agent narrowing | Draft composition tests and a concrete counterexample; no new automatic reduction policy. | Merge evidence tests, not the proposed general reducer. Current menus are not complete enough. |

## 7: application checks are session-owned

`AgentTransactionGuard` and `AgentInputRequiredException` moved from engine to agent. `Agents`
stores the check, `AgentImpl` and `AutoExecLoop` invoke it at the existing validation points, and
`CardTracking.start` accepts the tracked session. World no longer stores application validation.
There is no compatibility shim or global lookup.

Two independently constructed sessions over the same World now have independent validation.
The new test proves that missing input restores the tracked session's attempt, another session
can act independently, and the tracked session remains blocked until its own input is supplied.
Clients must retain the tracked session. Speculative sessions cannot install tracking checks.

`AgentCallCharacterizationTest` names existing callback behavior without approving it: a callback
that throws can leave the initial engine changes and external notification retained. Nested
callback commands and Admin rewards appear in the returned result. Moving transaction nesting,
callback timing, and FINALLY-based completion requires further decisions; no operation framework
was introduced to make this package move appear complete.

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

Selection still performs structural work such as exposing EACH/group children and handoffs.
Admission can discard literal Ok/zero and split eligible THENs. These are explicitly unresolved
exceptions to investigate, not proof that selection is now entirely inert. Partial narrowing is
retained because it enables useful subsequent AMAP evaluation.

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
