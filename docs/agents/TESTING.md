# Testing and verification

> **Read when:** choosing or running verification, writing/moving a test, changing build
> configuration, reconstructing a game, formatting, or benchmarking.
>
> **Skip when:** doing a read-only task that requires no build or behavioral claim.
>
> **Status:** current repository procedure.

## Read only the needed section

| Task | Read |
| --- | --- |
| Choose commands or suite scope | Routine verification |
| Change Gradle/dependencies/source sets | Build configuration |
| Write or move a test | Test design through the relevant test category |
| Reconstruct a whole game | Game replay tests and Direct state reconciliation, then the routed replay guide |
| Change shared multiplatform tests | Multiplatform tests |

## Test-support entry points

- [`TfmTest.kt`](../../test/common/dev/martianzoo/tfm/tests/TfmTest.kt) — inspect
  integrated setup and gameplay scopes.
- [`TestHelpers.kt`](../../test/common/dev/martianzoo/tfm/tests/TestHelpers.kt) —
  search for the named helper before spelling raw task text.
- [`CardTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/CardTest.kt) —
  read for component-focused scenario construction.
- [`AbstractFullGameTest.kt`](../../test/common/dev/martianzoo/tfm/tests/replays/AbstractFullGameTest.kt)
  — read only for whole-game chronology.

## Routine verification

The wrapper supports and directly uses the JDK selected by `JAVA_HOME` from 17 through 26. JVM code
targets the Java 17 bytecode and API surface, while Kotlin source and standard-library APIs target
Kotlin 2.2. Contributors do not need another JDK installed.

Start with the smallest test or build task that verifies the changed behavior. Expand verification
only when the change crosses a wider scope or the narrower result leaves a material risk.

- `./gradlew build` checks the whole repository: every JVM test plus one representative
  multi-generation engine game in Chrome. Use it only when repository-wide verification is
  warranted by the scope of the change or explicitly requested.
- `./gradlew test` runs every repository JVM test suite, including the multiplatform modules whose
  JVM test tasks are named `jvmTest`.
- `./gradlew :tfm-tests:jsBrowserSmokeTest` runs only the representative browser smoke scenario.
- `./gradlew jsBrowserTest` runs every module's full browser suite.
  `./gradlew build -PincludeBrowserTests=true` includes those suites in the normal repository-wide
  check.
- `./gradlew :tfm-tests:allTestsIncludingBrowser` runs every Terraforming Mars functional test on
  both the JVM and browser.
- `./gradlew :benchmarks:jmh` runs the separate JVM-only JMH benchmarks. Benchmark execution is not
  part of the routine test or build lifecycle, though the normal build compiles the benchmark
  sources. A benchmark error fails the task instead of producing an empty successful report.
- `./gradlew :repl:realTerminalSmokeTest` runs the separate Expect-based real-terminal test.
- `./gradlew spotlessApply` formats the source tree. CI runs `spotlessCheck`, and a normal build
  also reports formatting violations.
- `SOLARNET_RANDOM_AUTOMATIC_EFFECTS=true ./gradlew test --rerun-tasks` runs the unchanged JVM suites
  while choosing a random execution order for each batch of automatic-effect siblings. This is a
  diagnostic mode for finding undeclared ordering dependencies; ordinary runs retain a stable
  diagnostic order. Game-state assertions pass, but the exact Advanced Alloys attribution totals in
  `Game20230521Test` and `ThermalMatterWaveTest` may fail because saturating payment reductions do
  not yet record every effect's gross contribution.

Gradle may report tests as `UP-TO-DATE`. That is usually fine. When changing a test runner,
browser configuration, resource packaging, or a locked JavaScript dependency, force the affected
tasks with `--rerun-tasks` so cached results are not mistaken for verification.

Do not run Detekt while compilation or tests are known to be failing. Restore the normal test
signal first, then review static-analysis findings.

`./gradlew dokkaGenerateHtml` generates the local API site under the root project's isolated
`build/dokka/html` directory.

JVM test tasks use at most four parallel forks. This keeps the dominant engine suite substantially
faster while bounding the additional CPU and memory demand from concurrent test processes.

Normal Gradle access to the user-level cache and configuration under `~/.gradle` is permitted.
For local wrapper builds, generated project state is isolated by account and worktree under
`~/.gradle/solarnet-builds/`. This includes Gradle's project cache, Kotlin's persistent data, task
outputs, and build-process temporary files. CI retains the conventional project-local paths so its
artifact collection remains stable. Use `./gradlew` rather than a directly installed `gradle` so
the checked-in isolation configuration is applied.
Yarn's incompatible `serialize-javascript` resolution warning and “Ignored scripts due to flag”
warning are expected: the former comes from the deliberate 7.x security pin while Mocha requests
6.x, and the latter preserves Kotlin/JS's policy of not running package lifecycle scripts.

## Build configuration

Convention plugins under `gradle/build-logic` are layered by responsibility. `solarnet.kotlin-base` owns
the policy shared by every Kotlin target: compilation, explicit API mode, dependency alignment,
Detekt, Dokka, and test logging. `solarnet.jvm` adds the JVM plugin and the repository's standard
Kotlin/JUnit 5 test dependencies. `solarnet.kmp-jvm-js` configures the JVM and browser targets, adds
shared `kotlin.test`, and exposes each module's `jvmTest` as `test`.
Module build scripts under `modules/` keep only module-specific configuration and select their
non-overlapping package roots from the repository-wide `src/` and `test/` trees; the JavaScript-only
application configures its target directly. Repository-wide formatting and Yarn policy remain in
the root build.
Dependency and plugin versions are declared in `gradle/libs.versions.toml`, while dependency
repositories are declared centrally in `settings.gradle.kts`; JitPack is restricted to the pinned
better-parse fork.

All Kotlin modules use strict explicit API mode. Declarations that form a module's public API must
spell out `public` and their public types; declarations used only within one module should be
`internal` or `private`. This makes accidental API growth and signature changes visible in review.

## Test design

Terraforming Mars integration tests live under `dev.martianzoo.tfm.tests`: `cards` contains
component-focused behavior, `rules` contains game-wide and cross-component behavior, and `replays`
contains whole-game chronologies. Shared integrated-test support remains directly in the parent
package. Test placement follows purpose: a test of engine behavior belongs with engine even when it
uses Terraforming Mars declarations to construct its scenario, while a test of Terraforming Mars
rules or content belongs in the Terraforming Mars suites. Test-only dependencies may cross that
direction; production dependencies may not. Small generic declarations remain preferable when they
make a test clearer, but replacing domain examples is independent cleanup rather than a prerequisite
for correct ownership.

### Test categories we care about

These are the repository's protected test categories. Test placement may evolve, but preserving
clear coverage of these contracts matters more than preserving every current test class:

1. **Pure Pets language tests.** Parser, preprocessing, transformation, and rendering behavior,
   exercised without Terraforming Mars content.
2. **Pure Pets type-system tests.** Class loading, type relationships, metrics, requirements, and
   related semantics, using small declarations owned by the test rather than Canon.
3. **GameWorld coordination tests.** Terraforming-independent scenarios proving that components,
   effects, tasks, event history, revision, checkpoints, and gameplay operations form one coherent
   world. Failure atomicity belongs here: a failed operation must restore present state, pending
   work, and recorded history together while retaining a fresh revision identity.
4. **Player-level card and game-rule tests.** `CardTest` scenarios count when they use actions and
   observations available to a player rather than internal state or implementation details.
   `CoreRulesTest` documents game-wide rules in this same style.
5. **Whole-game tests.** Long scenarios that prove the workflow and many rules operate together,
   especially when reconstructed from independent game records.
6. **Canon admissibility tests.** A compact gate proving that the complete authority loads and that
   representative supported configurations compose into usable projected class tables and worlds.
   This is not a demand to restate the contents of every card or bundle in assertions.
7. **Known-defect scenarios.** Focused passing characterizations of important behavior known to be
   wrong, visibly quarantined in `BugsTest` until the behavior is corrected.
8. **Script-command contract tests.** Terraforming-independent checks of each command's public
   contract. These are useful interface coverage even though they are not a development priority.
9. **Cross-runtime packaging smoke coverage.** One representative browser game proving that the
   JavaScript artifact, generated Canon data, and engine work together outside the JVM.
10. **Real-terminal REPL smoke coverage.** One Expect-driven scenario proving the packaged REPL can
    be launched and used through an actual terminal.

This list does not itself decide which current tests should be retained. Test-deletion proposals
are a separate review.

Prefer tests that exercise several pieces together. Do not mirror a production list or data object
in a test just to detect that the list changed. Test observable behavior through the normal
test-facing layer: test the card, rule, or workflow result rather than a private transformation,
exact intermediate task text, or other implementation detail.

Keep task-routing mechanism tests in the generic engine suite. Those tests may inspect task
controller, assignee, narrower, selection, and event Actor because those are the contract under test. A
player-level card or rule scenario should instead demonstrate routing through public gameplay:
which Player can select or narrow, whether competing gameplay is blocked, the resulting state, and,
when necessary, an authored `BY` reaction that makes attribution observable. Do not locate card
reactions by exact rendered instruction, `Task.cause`, `Task.actor`, or raw Event Log inspection.

Keep trigger matching separate from queue routing. A `BY` characterization should prove which
triggers fire and how Actor variables bind through observable changes. Do not make its continued
success depend on an incidental assignee unless that test is explicitly about delegation.

Keep gameplay and test APIs generic. Never add a Kotlin helper or DSL operation solely to represent
one card, corporation, Prelude, or other component. Use existing gameplay helpers when their
operation scopes fit. When component-specific steps must stay inside an outer operation, express
them through existing `OperationBody` primitives so any sibling task may remain pending. Add a
shared helper only for a recurring, component-independent concept that materially simplifies
several call sites.

Do not inspect Canon declarations or definitions and assert their exact Pets trees or rendered
strings. Do not assert card totals by bundle, deck, expansion, or other content group. Canon
admissibility is intentionally a compact loading and composition gate; card and rule behavior
belongs in player-level scenarios.

Keep scenarios minimal and legible. Card tests use the base game and two players by default unless
the behavior requires something else, add only relevant options and components, and consistently
name the gameplay objects `p1` and `p2`. Use `manual()` when only the resulting setup matters instead
of replaying an irrelevant play-card sequence. Avoid `sneak`: it can create impossible states.
Synthetic card scenarios pass their card and supporting `ClassDeclaration`s to the `CardTest`
constructor; they are composed with Canon and selected in that test's premise.
Use `placeTile(row, column)`, `addCardResources(card)`, `wgt(choice)`, and `assignWildTag(card, tag)`
instead of spelling their routine task expressions. The tile and card-resource helpers require a
single matching pending choice; keep raw `doTask()` calls where multiple placements are pending.
When unrelated optional tasks are pending, pass the pending instruction to `declineTask(instruction)`.
Inside an existing operation that directly offers a repeated card action, such as Project Inspection,
use `cardAction1()` or `cardAction2()`; the operation-body overload selects and pays that action
without starting the usual use-card-action wrapper.
Use `declineTask()` only when exactly one pending task accepts `Ok`, and comment what is declined.

`CoreRulesTest` uses the same player-level style to document rules that belong to the game rather
than any individual card. Its scenarios should reproduce only the important preconditions observed
in whole games and should use the standard `TfmGameplay` actions and result expectations.
Full-game tests override a `config` property with a `GameConfig`, conventionally built from an
indented multiline string followed by player-name varargs. Catalog-backed premise resolution adds
`TerraformingMars` and, when no other map is named, `TharsisMap`; the parser already trims each
entry, so these literals do not need `trimIndent()`. Solo tests conventionally give canonical
`Player1` the vocabulary alias `Me` and use `Player.PLAYER1` in Kotlin. The raw-configuration
overload in `CardTest` uses the same resolution path.

`CardTest` and the full-game tests provide `TaskResult.expect()`. Expectations are partial net
deltas: name only changes that matter to the behavior under test. Unqualified owned Types are scoped
to the Player inferred from the result's ordered change events; qualify an Owner explicitly when
checking another Player or an intentionally cross-player total. Do not restate costs, test setup,
literal `doTask()` choices, or every incidental resource movement. In source-backed whole-game
tests, include explicitly narrated gains/removals and interesting automatic effects, even when the
expected net differs from the narrated gross amount. Prefer a nearby absolute assertion when the
source states an absolute value. Use a zero scalar, such as `0 Plant` or `PROD[0 Energy]`, to assert
that a particular type did not change.

Cover meaningful interfaces, negative cases, non-targets, and option combinations rather than only
the happy path. A filtering or Type-variable test should include several tempting Components that must not
match. Preserve this coverage during refactoring.

Assert a particular exception subclass only when callers or game semantics depend on that
classification. Otherwise prove that the command is rejected, state and history remain atomic, and
the diagnostic identifies the problem. The current distinction among task, abstractness, and
narrowing exceptions is provisional and should not make an otherwise behavioral test brittle.

`BugsTest` is different: its passing tests characterize known incorrect behavior, and their names
say what currently happens incorrectly. Prefer such a characterization over a disproportionate
workaround. Once the bug is fixed, move the useful scenario to its proper behavioral suite.

## Game replay tests

Whole-game tests are high-value integration evidence. When translating a supplied game log:

- `CardTrackingFullGameTest` is an opt-in full-game base for source archives that identify project
  cards. `expectProjectCards()` assigns sourced identities to an otherwise anonymous selection;
  named draw, purchase, discard, and return calls then update one test-owned location ledger. The
  tracker reads game events only to observe named cards being played. A named discard is terminal;
  cards do not return to the deck. For source-known deck exits that the model omits, record the
  terminal exit explicitly. `discardUnselectedProjectCards()` may either close a previously named
  selection or introduce the rejected names directly; inside an operation it also resolves an
  already-open anonymous selection-removal task.
  Research archives that used drafting may assign each recovered post-draft four-card set as that
  player's ordinary deal when the tested engine does not support drafting. Express an ordinary
  research deal directly by partitioning its cards between `buyCards()` and
  `discardUnselectedProjectCards()`; do not declare the same offer first.
  `AbstractSoloTest` inherits this capability, but a solo test opts into tracking only by using
  the named calls.
  When a source gives only a discard count, an exact tracked hand requires the test to select
  names explicitly and label that selection as test inference.
- Before editing a dated whole-game test, explicitly inspect its matching
  `_local/replays/GameYYYYMMDD/` directory and read any `implementation-plan.md` there before acting. The
  repository's `_local` path may be a symlink, which `rg --files` does not traverse, so a general
  file search is not evidence that the test's local sources are absent. A plan supplies workflow,
  not game facts: establish all setup, chronology, values, and reconciliations from original sources.
- For a herokuapp archive, read `docs/agents/HEROKUAPP_GAME_LOGS.md` before implementation. Its API,
  payment-reconstruction, screenshot, counterfactual, and endgame rules supplement this section.
- For a recorded physical game, read `docs/agents/OTB_GAME_RECORDS.md` before implementation. Its
  source-preservation, mixed-evidence, photograph, reconciliation, and endgame rules supplement this
  section.
- Do not inspect an existing dated test, Git history, or previous agent summary to learn what
  happened when the task calls for an independent reconstruction. Repository code and other tests
  may teach the Solarnet API only.
- In multiplayer action phases, express each player's actions as `player.turn { ... }`. A normal
  turn block contains up to two actions and automatically declines an unused second action. The
  exception is a sourced fast-mode game, where players must take two actions unless passing; translate
  those actions directly instead of using `turn {}`, and preserve explicit passes or declined second
  actions when the source records them.
  Once every other player has passed, keep the remaining player's actions through `pass()` in one
  turn block; workflow-provided `NewTurn` tasks let that block continue across the remaining nominal
  turns.
- When an Event card's sibling tasks require explicit replay ordering, record only the gameplay
  consequences. The engine moves the card to `PlayedEvent` automatically after all queues empty.
- Use the same turn block for a player's two Prelude plays. This also supports Prelude effects that
  immediately play another Prelude or project inside one of those plays.
- Keep the supplied machine log as a separate source artifact rather than copying its lines into the
  test solely for traceability. Periodically audit the test directly against that artifact for
  chronology, choices, and consequences. Assert selective checkpoints, summaries, and final facts
  rather than mechanically asserting every log entry. Audio transcript translations may drop filler
  and repetition while retaining gameplay-relevant personality, uncertainty, corrections, and mistakes.
- Treat every supplied screenshot as an authoritative snapshot at its exact point in the timeline.
  At a generation checkpoint, reproduce any research choices logged before that screenshot, then
  reconcile every visible resource and production discrepancy before the first action. If the
  screenshot was taken before purchases, reconcile before them. Write explicit relative `exMachina()`
  deltas; do not set absolute values or let differences accumulate until a later screenshot.
- Logs may not indicate how much steel/titanium/etc. was used toward a purchase. A reasonable
  default assumption to start with is that they probably spent as much of it as they could get full
  value for. Later events may reveal that your assumption needs to be revised.
- Source-backed full-game replays enforce that assumption. A payment that leaves an accepted
  non-money resource unused despite its still receiving full value fails unless the player calls
  `intentionalUnderpay()` immediately before that payment. A payment that spends a non-money
  resource beyond the remaining owed amount likewise requires `intentionalOverpay(amountSquandered)`,
  with the exact lost monetary value. Each call is
  permission for one payment only; explain the sourced later payment or checkpoint that requires an
  underpayment, and prefer correcting an unsupported allocation over declaring intent. For a
  recorded physical game, first search the transcript and player-board logs for an explicit payment
  composition; prefer that direct evidence to inference from a later balance.
- The herokuapp and Solarnet map coordinates differ: herokuapp counts from the first tile whereas
  Solarnet uses slant-columns.
- Prefer supplied logs, images, and local map data over investigating another application's
  implementation. Work around unsupported engine behavior narrowly and record real follow-ups in
  `TODO.md`.

### Direct state reconciliation

- Never call `sneak` directly in a game test. Use the test's `exMachina` helper for an
  evidence-backed player error that requires a direct state adjustment. Place it as late in the
  timeline as the sourced assertions allow, with a comment saying which later step requires it. Add
  source-backed `.expect()` assertions for the mistake-prone types to preceding actions wherever
  practical so the remaining unexplained gap is bounded as narrowly as possible.
  If auto-exec has already selected the next task, the helper rolls that selection back and repeats
  it after the adjustment so state-dependent instructions are resolved again.
  Both replay bases delegate that lifecycle to the shared `World.exMachina` implementation; do not
  fork its task-history traversal.
  Keep unexplained state reconciliations as standalone timeline statements.
  Never place a manual or other raw adjustment inside an unrelated action body to evade a selected-task or
  operation-scope restriction; use an explicit test mechanism or fix the helper/API instead.
  Nest a missing consequence only when the enclosing action caused it.

## Multiplatform tests

- Shared `kotlin.test` test classes and methods may not be private; even though JVM test runs may
  work with them, JS test runs may not.
- Mocha owns the per-test timeout; Karma owns browser activity, disconnect, and reconnect
  timeouts. A long synchronous test can block the browser event loop long enough to hit either.
- A Node test target is not a substitute for the browser suite, which verifies browser compilation
  and integration.
