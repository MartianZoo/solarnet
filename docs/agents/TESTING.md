# Testing and verification

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** choosing or running verification, writing/moving a test, changing build
> configuration, reconstructing a game, formatting, or benchmarking.
>
> **Skip when:** doing a read-only task that requires no build or behavioral claim.
>
> **Status:** current repository procedure; the project-card suite is actively migrating to the
> standardized fixture described below.

## Read only the needed section

| Task | Read |
| --- | --- |
| Choose commands or suite scope | Standard verification |
| Change Gradle/dependencies/source sets | Build configuration |
| Write or move a card/rule test or gameplay helper | Test design through the relevant test category; project cards also require [Standard project-card fixture migration](#standard-project-card-fixture-migration) |
| Use `TaskResult.expect()` | Expectations |
| Preserve known incorrect behavior | Known-defect tests |
| Reconstruct a whole game | Game replay tests and Direct state reconciliation, then the routed replay guide |
| Change shared multiplatform tests | Multiplatform tests |

## Test-support entry points

- [`TfmTest.kt`](../../test/common/dev/martianzoo/tfm/tests/TfmTest.kt) — inspect
  integrated setup and gameplay scopes.
- [`TestHelpers.kt`](../../test/common/dev/martianzoo/tfm/tests/TestHelpers.kt) —
  search for the named helper before spelling raw task text.
- [`CardTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/CardTest.kt) —
  read for component-focused scenario construction.
- [`ProjectCardTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/ProjectCardTest.kt) —
  use as the default base for eligible project-card functional tests.
- [`AbstractFullGameTest.kt`](../../test/common/dev/martianzoo/tfm/tests/replays/AbstractFullGameTest.kt)
  — read only for whole-game chronology.

## Standard verification

The wrapper supports and directly uses the JDK selected by `JAVA_HOME` from 17 through 26. JVM code
targets the Java 17 bytecode and API surface, while Kotlin source and standard-library APIs target
Kotlin 2.2. CI uses Temurin 25 LTS. Contributors do not need another JDK installed.

Start with the smallest test or build task that verifies the changed behavior. Expand verification
only when the change crosses a wider scope or the narrower result leaves a material risk.

- `./gradlew build` checks the whole repository: the same tests as `test`, plus all production
  JavaScript compilation and packaging. Use it only when repository-wide verification is warranted
  by the scope of the change or explicitly requested.
- `./gradlew test` runs every repository JVM test suite, every browser-specific test, and the
  `OtbGame20260828Test` replay once in a browser. The multiplatform modules' JVM test tasks are named
  `jvmTest`; their generated browser tasks are inert outside the one intentionally commented-out
  full-browser target in the root build.
- Temporarily uncomment `allBrowserTests` in the root build and run
  `./gradlew allBrowserTests --rerun-tasks` to exercise every shared and browser-specific suite,
  including all portable replay scenarios. The browser replay source set also reads the legacy
  `test/jvm/dev/martianzoo/tfm/tests/replays` directory, excluding only its JUnit file-export hook.
  JVM-only tools and filesystem/terminal tests remain outside this target. Comment the target back
  out after a successful run.
- `./gradlew :tfm-tests:jvmTest` runs the replay tests and writes one opaque JSON recording per
  successful `AbstractFullGameTest` subclass under that module's
  `generated/replay-event-logs` build directory. The browser viewer applies those recordings through
  `:state` and never runs the engine.
- `./gradlew :tfm-tests:replayTestCoverage` runs only tests in the replay package and writes HTML and
  XML production-code coverage reports under that module's `reports/jacoco/replayTestCoverage`
  build directory. Its execution data comes from the separate `replayTest` task, so card, rule,
  random-card, and browser tests do not contribute to the report.
- `./gradlew :pets:jvmTestCoverage` runs only the Pets module's JVM test suite and writes HTML and
  XML coverage reports for Pets production code under that module's
  `reports/jacoco/jvmTestCoverage` build directory.
- `./gradlew :tfm-tests:sampleRandomCards` prints randomly generated project cards as raw Pets.
  Use `-PrandomCardCount=N` and `-PrandomCardSeed=N` to control and reproduce a sample, and add
  `-PrandomCardOutput=PATH` to write it to a text file. Its weights favor nested selectors,
  refinements, sequences, gates, and per-unit metrics so the raw Pets can exercise downstream
  consumers structurally.
- `./gradlew :benchmarks:jmh` runs the separate JVM-only JMH benchmarks. Benchmark execution is not
  part of the routine test or build lifecycle, though the normal build compiles the benchmark
  sources. A benchmark error fails the task instead of producing an empty successful report.
- `./gradlew spotlessApply` formats the source tree. CI runs `spotlessCheck`, and a normal build
  also reports formatting violations.
- `./gradlew :tools:kotlinFileComplexity` writes a TSV of Detekt cyclomatic complexity for each
  production Kotlin file and each test-support file without test-case methods to
  `reports/kotlin-file-complexity.tsv` under the root build directory. The report is manual rather
  than part of `check`, and Gradle reanalyzes only added or changed files after its first run. The
  current scope excludes benchmarks and standalone tools.
- `./gradlew :tools:dumpAllExpansionsEventLogs` creates three-player and solo games with every
  supported expansion plus `FakeCardsCardPack`, completes Player 1's corporation-phase turn with
  Interplanetary Cinematics and four bought cards in each, and writes every change event (including
  `System` and `Hidden` changes, but no task events) to
  `_local/eventlogs/three-player-all-expansions-eventlog.tsv` and
  `_local/eventlogs/solo-all-expansions-eventlog.tsv`. The `actor` column is engine attribution,
  not proof of a user choice: an automatic or queued effect carried by a Player-owned component may
  attribute its derived changes to that Player. Use the cause columns to trace derivation; because
  task events are omitted, the TSV cannot by itself classify every row as chosen versus automatic.
- `./gradlew :tools:dumpOtbGame20260828EventLog` and
  `./gradlew :tools:dumpOtbGame20260912EventLog` run the JVM replay suite, read those generated
  physical-game recordings, and write every change event in the same format under
  `_local/eventlogs/`.
- `SOLARNET_RANDOM_AUTOMATIC_EFFECTS=true ./gradlew test --rerun-tasks` runs the unchanged JVM suites
  while choosing a random execution order for each batch of independent automatic-effect listeners.
  A component's own automatic Effects retain declaration order. This is a diagnostic mode for
  finding undeclared ordering dependencies; ordinary runs retain a stable diagnostic order.
  Game-state assertions pass, but the exact Advanced Alloys attribution totals in
  `Game20230521Test` and `ThermalMatterWaveTest` may fail because saturating payment reductions do
  not yet record every effect's gross contribution.

Gradle may report tests as `UP-TO-DATE`. That is usually fine. When changing a test runner,
browser configuration, resource packaging, or a locked JavaScript dependency, force the affected
tasks with `--rerun-tasks` so cached results are not mistaken for verification.

Do not run Detekt while compilation or tests are known to be failing. Restore the normal test
signal first, then review static-analysis findings.

`./gradlew dokkaGenerateHtml` generates the local API site under the root project's isolated
`build/dokka/html` directory.

JVM test tasks use at most two parallel forks with a 1 GiB maximum heap each. The repository's
`org.gradle.workers.max=2` also bounds test processes across concurrently scheduled modules, keeping
the aggregate test heap budget at 2 GiB per Gradle invocation. The separate
`org.gradle.jvmargs=-Xmx4g` setting controls the build daemon, not test workers. Concurrent Gradle
invocations each have their own budget; avoid overlapping verification runs on the same host.

Scope configuration-matrix fixtures to test instances so their Catalogs can be collected before
unrelated suites run. Deliberately shared common card-test premises retain their immutable class
models per worker. Do not use worker recycling or a larger build-daemon heap to hide unintended
retention.

Normal Gradle access to the user-level cache and configuration under `~/.gradle` is permitted.
For local builds, generated project state is isolated by account and worktree under
`~/.gradle/solarnet-builds/`. Each invocation acquires an OS-locked storage slot there: sequential
builds reuse slot zero and its caches, while overlapping invocations use distinct slots and cannot
delete each other's test results or other task outputs. A slot includes Gradle's project cache,
Kotlin's persistent data, task outputs, and build-process temporary files. CI retains the
conventional project-local paths so its artifact collection remains stable. The wrapper supplies
the isolation init script directly. For IntelliJ and other Tooling API clients, copy
`gradle/user-isolation-bootstrap.init.gradle.kts` to
`~/.gradle/init.d/solarnet-user-isolation.init.gradle.kts` once; that user-level bootstrap discovers
the checked-in script in every current and future Solarnet worktree. Gradle opens `.gradle` before
any init script runs, so also install `gradle/user-isolation.post-checkout` as the shared Git
repository's `hooks/post-checkout`; it creates an ignored `.gradle` symlink into the same
per-worktree home storage for every tracked Gradle build whenever Git creates or checks out a
worktree.
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
non-overlapping package roots from the repository-wide `src/` and `test/` trees; JavaScript-only
applications configure their targets directly. Repository-wide formatting, the Node.js version, and
Yarn policy remain in the root build.
Dependency and plugin versions are declared in `gradle/libs.versions.toml`, while dependency
repositories are declared centrally in `settings.gradle.kts`; JitPack is restricted to the pinned
better-parse fork.

All Kotlin modules use strict explicit API mode. Declarations that form a module's public API must
spell out `public` and their public types; declarations used only within one module should be
`internal` or `private`. This makes accidental API growth and signature changes visible in review.

## Test design

Do not add tests whose sole purpose is to specify what happens after `exMachina` or `sneak`.
Direct corrections are test setup, not the subject or evidence of a card test. Keep assertions
focused on the subsequent player action, card behavior, or shared engine behavior. Evidence-backed
corrections inside whole-game replays remain appropriate; the replay tests the game, not correction
semantics.

> **Recurring failure warning:** Card and rule tests operate through player-facing gameplay and
> assert observable results. They do not inspect rendered task text, causes, incidental queue order,
> or mirrored Canon data. A test-support helper must express a recurring component-independent
> operation, never one card's rule or missing engine semantics.

> **Default assertion style:** Almost every successful gameplay action that returns a `TaskResult`
> should chain `.expect(...)` directly to that action. Prefer this over later absolute resource or
> component counts: the chained expectation proves the net change came from the action under test,
> while an absolute count can pass because of setup or an earlier action. Use an absolute assertion
> only when the absolute state is itself the contract, no successful result exists to inspect, or a
> source explicitly states an absolute value.

Terraforming Mars integration tests live under `dev.martianzoo.tfm.tests`: `cards` contains
component-focused behavior, `rules` contains game-wide and cross-component behavior, and `replays`
contains whole-game chronologies. Shared integrated-test support lives directly in the parent
package. Test placement follows purpose: a test of engine behavior belongs with engine even when it
uses Terraforming Mars declarations to construct its scenario, while a test of Terraforming Mars
rules or content belongs in the Terraforming Mars suites. Test-only dependencies may cross that
direction; production dependencies may not. Small generic declarations are preferable when they
make a test clearer, but replacing domain examples is independent cleanup rather than a prerequisite
for correct ownership.

### Test categories we care about

Two migrations are in progress. Ordinary card and game-rule scenarios are moving toward full
automatic phase and turn progression, and eligible project-card classes are moving to the
standardized `ProjectCardTest` fixture described below. Existing tests have not all migrated.
Dedicated REPL mode tests and lower-level engine or bootstrap tests retain their distinct subjects.
Do not replace manual phase calls with helpers that recreate the workflow in Kotlin.

These are the repository's protected test categories. Test placement may evolve, but preserving
clear coverage of these contracts matters more than preserving every current test class:

1. **Pure Pets language tests.** Parser, preprocessing, transformation, and rendering behavior,
   exercised without Terraforming Mars content.
2. **Pure Pets type-system tests.** Class loading, type relationships, metrics, requirements, and
   related semantics, using small declarations owned by the test rather than Canon.
3. **Game World and engine-coordination tests.** Pure `:state` scenarios verify that exact
   component/task events, materialized projections, history, completed recording positions, and
   independent playback views remain coherent without firing effects. Cross-module engine
   scenarios cover consequence calculation and failure atomicity: a failed operation must restore
   present components, pending work, and recorded history together.
4. **Player-level card and game-rule tests.** `CardTest` scenarios count when they use actions and
   observations available to a player rather than internal state or implementation details.
   `CoreRulesTest` documents game-wide rules in this same style.
5. **Whole-game tests.** Long scenarios that show the workflow and many rules operate together,
   especially when reconstructed from independent game records. Every successful replay test also
   emits the recording consumed by the game viewer.
6. **Canon admissibility tests.** A compact gate confirming that the complete authority loads and
   that representative supported configurations compose into usable projected class tables and
   worlds. This is not a demand to restate the contents of every card or bundle in assertions.
7. **Known-defect scenarios.** Focused passing characterizations of important behavior known to be
   wrong, visibly quarantined in `BugsTest` until the behavior is corrected.
8. **Script-command contract tests.** Terraforming-independent checks of each command's public
   contract. These are useful interface coverage even though they are not a development priority.
9. **Cross-runtime browser coverage.** Browser-specific tests cover browser APIs, one representative
   replay checks the shared engine on JavaScript, and production JavaScript compilation and resource
   assembly cover the engine-free viewer.

This list does not itself decide which current tests should be retained. Test-deletion proposals
are a separate review.

Give each scenario variant its own named test, using a private helper for shared steps. Do not put
variants in a `for` loop inside one test: the failing test name should identify the case. Exercise
the behavior under test through gameplay. For eligible project-card tests, establish incidental
preconditions through the standardized fixture and direct corrections by default. Use authentic
gameplay instead when the way a precondition is reached can affect, or provide useful evidence for,
the behavior under test.
Use `CONCRETE` when ordering choices matter; reserve `NONE` for tests that must control otherwise
unambiguous automatic steps.

Prefer tests that exercise several pieces together. Do not mirror a production list or data object
in a test just to detect that the list changed. Test observable behavior through the normal
test-facing layer: test the card, rule, or workflow result rather than a private transformation,
exact intermediate task text, or other implementation detail.

Keep task-routing mechanism tests in the generic engine suite. Those tests may inspect task
controller, selection assignee, current assignee, selection, and event Actor because those are the
contract under test. A player-level card or rule scenario should instead demonstrate routing
through public gameplay: which Player can select or narrow, whether competing gameplay is blocked,
the resulting state, and, when necessary, an authored `BY` reaction that makes attribution
observable. Do not locate card reactions by exact rendered instruction, `Task.cause`, internal
assignment fields, or raw Event Log inspection.

For delegated payment, final resource totals do not prove continuous authority. A helper that
selects through another Actor can conceal missing engine control. Exercise separate Player
commands, including attempted intervention between payment choices, and verify legality with
autoexecution disabled where necessary. [SEQUENCING.md](SEQUENCING.md#the-missing-rule-when-an-operation-is-over)
records the unresolved operation-level rule; current task-level return-to-controller tests
characterize existing behavior, not acceptance of that proposed rule.

Keep trigger matching separate from queue routing. A `BY` characterization should show which
triggers fire and how Actor variables bind through observable changes. Do not make its continued
success depend on an incidental assignee unless that test is explicitly about delegation.

Keep gameplay and test APIs generic. Never add a Kotlin helper or DSL operation solely to represent
one card, corporation, Prelude, or other component. Use existing gameplay helpers when their
operation scopes fit. When component-specific steps must stay inside an outer operation, express
them through existing `OperationScope` primitives so any sibling task may remain pending. Add a
shared helper only for a recurring, component-independent concept that materially simplifies
several call sites. `TfmGameplay` must not repair the game model by creating or relocating rule
components, imposing order absent from Pets or the engine, or identifying work by rendered text or
cause.

Do not inspect Canon declarations or definitions and assert their exact Pets trees or rendered
strings. Do not assert card totals by bundle, deck, expansion, or other content group. Canon
admissibility is intentionally a compact loading and composition gate; card and rule behavior
belongs in player-level scenarios.

Keep scenarios minimal and legible. Project-card tests that fit the standardized fixture follow the
migration rules below. Other card tests use the smallest suitable configuration and consistently
name their gameplay objects. Use `runOperation()` when an operation's resulting setup matters and a
direct correction is not suitable. Card and rule scenarios do not call `sneak` directly; eligible
project-card scenarios use the fixture's explicit `exMachina` setup methods.
Synthetic card scenarios pass their card and supporting `ClassDeclaration`s to the `CardTest`
constructor; they are composed with Canon and selected in that test's premise.
When a custom instruction reads authored card metadata from the catalog, compose the synthetic
card into a fixture `TfmCatalog`; premise-only declarations do not populate that metadata.
Use `placeTile(row, column)`, `addCardResources(card)`, and `wgt(choice)` instead of spelling their
routine task expressions. The tile and card-resource helpers require a single matching pending
choice; card-resource matching includes the destination card, so offers for different cards can
coexist. Keep raw `doTask()` calls where multiple placements are pending.
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
entry, so these literals do not need `trimIndent()`. Solo tests conventionally use `Me` as the
canonical Player Class Name and use `Player.PLAYER1` in Kotlin. The raw-configuration
overload in `CardTest` uses the same resolution path.

### Standard project-card fixture migration

`ProjectCardTest` is the default base for project-card functional tests when its standard game can
express the scenarios in the class. It starts each test at generation 1 Action phase with:

- Tharsis, Venus Next, Colonies, and Promos;
- Corporate Era and the Beginner and Quick Start variants;
- Kim, Stan, and Rob, in that order, each using a distinct beginner corporation; and
- the ordinary beginner-corporation starting state, including 42 MC, 10 anonymous project cards,
  20 TR, and production of 1 for each standard resource.

Kim is the player exercising the subject card unless the card's behavior requires another actor.
The standard game has three players. A scenario may instead pass `playerCount` from two through
five; Maya and Nadia occupy the fourth and fifth seats. The protected `players` list exposes every
seat in order. `kim` and `stan` remain convenient non-null properties, while accessing `rob` in a
two-player game fails immediately with a fixture error.

The fixture prepares and caches an Action-phase `World` for each option-set and player-count pair,
then gives every test an independent `Engine.fork` of that prepared position. The cached World is
never exposed or mutated after preparation. The base class does not create a fork automatically. A
uniform class declares its own `@BeforeTest` method that calls `newTestGame()`. A class whose methods
need different compatible selections or player counts calls `newTestGame()` explicitly in each
method, passing arguments such as `addOptions = "CimmeriaMap"` or `playerCount = 4` where needed. Do
not share or roll back a live World between tests. The fixture leaves the default autoexecution
policy untouched: it selects Beginner mode and distinct beginner corporations, while forced setup
effects autoexecute normally.

Treat every pre-migration test as a fallible historical artifact, not as a specification of its
setup. Existing options, cards, resource grants, card plays, phase changes, autoexecution policies,
and operation order may be incidental, copied from another scenario, obsolete, or compensating for
old behavior. Begin with the behavior named by the scenario and its meaningful observable coverage.
Challenge every setup step: if removing it does not change the behavior under test, remove it. In
particular, do not preserve gameplay merely because the old test used it to reach a state that a
direct correction can express, and do not infer a configuration requirement solely from the old
test's configuration.

Review an unmigrated class for test value and structure before deciding whether it fits the fixture.
First remove declaration-only and duplicate scenarios, separate independent behaviors, and trim
each retained scenario to the setup its claim needs. Only then assess the resulting class for
migration; conversion should not preserve clutter merely because it was present when evaluation
began.

Each migrated test method should prove a single behavior. Do not carry a scenario through a series
of actions that successively test additional behavior. Split those actions into separately named
tests, and give each resulting test only the setup required for its own behavior. Several
assertions about the result of the same behavior remain appropriate.

Inline single-use and card-specific test helpers before evaluating a class. An unusual helper often
conceals inherited setup, several behaviors, lower-level intervention, or steps that no retained
scenario needs. Simplify the visible scenario first. Keep a private helper only when it makes
genuinely shared preparation clearer without hiding choices or the behavior under test.

Finish every reviewed class with a naming-consistency pass, whether or not the class migrates. Test
names should use the same voice and domain terms, state the observable behavior being proved, and
distinguish scenario variants without describing incidental setup or implementation mechanics.

Migration is also a test-value review, not a promise to preserve every method. Remove a scenario
whose only credible value is catching a literal mistake in an otherwise ordinary card declaration.
Retain tests that demonstrate non-obvious game meaning, exercise important shared semantics,
preserve sourced FAQ or defect evidence, distinguish tempting targets or choices, or show a useful
interaction among independently authored rules. Prefer representative coverage over repeating the
same semantic pattern for another card with different literals.

Every state change in a migrated test must come from real player-facing gameplay or from the two
fixture correction methods below. Do not use `runOperation`, `beginOperation`, `sneak`, manual phase
changes, or other lower-level shortcuts in a `ProjectCardTest` subclass.

Starting conditions beyond that tabula-rasa state should normally be direct, visible corrections:

- Use `setToExMachina(targetCount, type)` for a desired absolute count. It calculates the gain or
  loss from the current count, so the scenario states its intended condition rather than assuming
  the fixture's prior value.
- Use `exMachina(adjustment)` for a naturally relative change or a known absent-to-present fact.
  Prefer a concrete Type such as `NormalCityTile` when an abstract Type cannot be created directly.
- If initial play of the subject card is not being tested, install that card directly. Do not first
  satisfy its play requirement, pay its price, or reproduce its immediate instructions. Correct any
  independent state that the behavior under test actually needs.
- If initial play, payment, requirements, immediate instructions, or entry-trigger interactions are
  the subject, play the card normally and establish whatever legal setup that action requires.
- To test behavior at a completed global parameter, correct the track to its penultimate step and
  use an ordinary standard project for the final step when available. This preserves the real
  completion lifecycle, including `GpComplete`, instead of asking a correction to stand in for it.

Corrections intentionally suppress ordinary queued effects while retaining the structural work
documented in [EX_MACHINA.md](EX_MACHINA.md). Therefore, never treat the correction itself as proof
that gameplay works. Assertions should begin with the action or interaction that is the scenario's
subject, and should omit setup deltas.

Authentic setup through gameplay remains valid and is sometimes preferable. Use it deliberately
when the path into the state matters: trigger history, attribution, task ordering, payment,
requirements, card-entry effects, global-parameter completion, workflow, or interactions among
setup actions. It is also appropriate when a broader integration scenario is valuable in its own
right. Direct correction is the default for irrelevant preconditions because it keeps focused card
tests short and makes their real subject obvious; it is not a ban on authentic gameplay.

Migrate a test class only when all its scenarios fit this fixture and setup model. A scenario may
start with an additional compatible game selection, including a different map or multiplayer count.
Leave the whole class on `CardTest` when it needs solo mode, a different variant, synthetic
declarations, or another incompatible configuration. Do not add specialized fixture variants or
replace a meaningful scenario merely to increase the migrated count. This is an active,
class-by-class migration: an existing `CardTest` subclass may simply be awaiting evaluation, and its
current base class does not by itself express a preferred testing style.

### Expectations

Terraforming Mars gameplay tests provide `TaskResult.expect()`. Chain it directly to successful
gameplay calls as the normal assertion style; do not replace an available result expectation with
later `count(...) shouldBe ...` or `assertCounts(...)` checks. Expectations are partial net deltas:
name only changes that matter to the behavior under test. Unqualified owned Types are scoped to the
Player inferred from the result's ordered change events; qualify the owner explicitly when checking
another Player or an intentionally cross-player total. Do not restate costs, test setup, literal
`doTask()` choices, or every incidental resource movement. In source-backed whole-game tests,
include explicitly narrated gains/removals and interesting automatic effects, even when the
expected net differs from the narrated gross amount. Prefer a nearby absolute assertion when the
source states an absolute value or the absolute state itself is the subject. Failed and deliberately
aborted actions have no successful `TaskResult`, so assert their relevant unchanged state directly.
Use a zero scalar, such as `0 Plant` or `PROD[0 Energy]`, to assert that a particular type did not
change.

Cover meaningful interfaces, negative cases, non-targets, and option combinations rather than only
the happy path. A filtering or Type-variable test should include several tempting Components that must not
match. Preserve this coverage during refactoring.

Assert the actual exception type with `shouldThrow<ExpectedException>`; do not use `shouldThrowAny`
or a catch-all superclass. An unrelated failure must not satisfy a rejection test. Also check the
relevant unchanged state and, when useful, the diagnostic identifying the problem.

### Known-defect tests

`BugsTest` is different: its passing tests characterize known incorrect behavior, and their names
say what currently happens incorrectly. Prefer such a characterization over a disproportionate
workaround. Once the bug is fixed, move the useful scenario to its proper behavioral suite.

## Game replay tests

Whole-game tests are high-value integration coverage. When translating a supplied game log:

- `CardTrackingFullGameTest` is an opt-in full-game base for source archives that identify project
  cards. Named draw, purchase, discard, and return calls update one test-owned hand ledger and
  annotate the corresponding project-card events. Naming may happen immediately before or after the
  engine change.
  A replay with complete source data may instead override `projectCardArrivalOrder` for each Player.
  This is only the order in which cards enter that Player's modeled hand, not a claim about offers
  or physical deck order. The tracker consumes the fixture according to anonymous hand-gain counts.
  It rejects duplicate arrivals, an exhausted or partly unused fixture, and any attempt to discard
  a card that never entered the indicated Player's hand.
  Completion requires an identity label for each lasting project-card hand arrival and
  departure, including both sides of an exchange, and checks tracked hand sizes against the World.
  Temporary Hand–Revealed–Hand movements do not consume arrival names, but the strict tracker
  requires both movements to be labeled with the names of cards already held.
  When a source omits a hand card's identity, `unknownProjectCards()` supplies distinct replay-local
  `UnknownCardNN` labels for cards that did
  enter a hand; keep the source gap visible beside their use. These labels prove complete hand
  accounting, not complete source knowledge. The database-backed Herokuapp conversions use this
  base without unknown labels. A named discard is terminal unless an exact played Event later
  returns to the hand.
  Research archives that used drafting may assign each recovered post-draft four-card set as that
  player's ordinary hand arrivals when the tested engine does not support drafting. Buy only the
  evidenced count; never name cards that were not retained.
  Ordinary full-game and solo replays use `AbstractFullGameTest` and `AbstractSoloTest` without an
  external card ledger. Only the four database-backed conversions and `StinaGameTest` currently use
  `CardTrackingFullGameTest`.
  When a source gives only a discard count, an exact tracked hand requires the test to select
  names explicitly and label that selection as test inference.
- Before editing a dated whole-game test, explicitly inspect its matching
  `_local/replays/GameYYYYMMDD/` directory and read any `implementation-plan.md` there before acting. The
  repository's `_local` path may be a symlink, which `rg --files` does not traverse, so a general
  file search is not evidence that the test's local sources are absent. A plan supplies workflow,
  not game facts: establish all setup, chronology, values, and reconciliations from original sources.
- For a herokuapp archive, read `docs/agents/HEROKUAPP_GAME_LOGS.md` before implementation. Its API,
  payment-reconstruction, screenshot, counterfactual, and endgame rules supplement this section.
- For a recorded physical game, read [`_local/OTB_GAME_RECORDS.md`](../../_local/OTB_GAME_RECORDS.md)
  before implementation. Its
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
- Source-backed full-game replays enforce that assumption for resources worth more than one M€.
  Leaving an accepted full-value unit unused fails unless the player calls `intentionalUnderpay()`
  immediately before that payment. The same one-shot audit exemption covers spending an accepted
  1:1 resource while enough M€ could settle the billing. It does not waive payment legality: an
  allocation containing a unit that could be returned is rejected, while unavoidable rounding
  excess needs no marker. Explain the sourced later payment or checkpoint that requires an unusual
  allocation, and prefer correcting an unsupported allocation over declaring intent.
  For a recorded physical game, first search the transcript and player-board logs for an explicit
  payment composition; prefer that direct evidence to inference from a later balance.
- Call `requireExplicitUnusedActionCards()` on replay players when every pass should audit unused
  action cards. For those players, `pass()` asserts that no action cards are unused; otherwise use
  `pass(unused = CardA, CardB)`. The pass fails if the complete set does not match.
- The herokuapp and Solarnet map coordinates differ: herokuapp counts from the first tile whereas
  Solarnet uses slant-columns.
- Prefer supplied logs, images, and local map data over investigating another application's
  implementation. Work around unsupported engine behavior narrowly and record real follow-ups in
  `TODO.md`.

### Direct state reconciliation

- Express a sourced per-player setup rule, such as a starting handicap, on that replay's concrete
  Player Class with a `SetupPhase` effect supplied through `playerClassPets`. It is game
  setup, not a direct state reconciliation.
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
