# Testing and verification

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** choosing or running verification, writing/moving a test, changing build
> configuration, reconstructing a game, formatting, or benchmarking.
>
> **Skip when:** doing a read-only task that requires no build or behavioral claim.
>
> **Status:** current repository procedure; card and rule scenarios use the sandbox and gameplay
> fixtures described below.

## Read only the needed section

| Task | Read |
| --- | --- |
| Choose commands or suite scope | Standard verification |
| Change Gradle/dependencies/source sets | Build configuration |
| Write or move a card/rule test or gameplay helper | Test design through the relevant test category; also read [Sandbox and gameplay fixture migration](#sandbox-and-gameplay-fixture-migration) |
| Use `TaskResult.expect()` | Expectations |
| Preserve known incorrect behavior | Known-defect tests |
| Consolidate gameplay/replay styles or helpers | Gameplay consistency project; distinguish agreed work from observations not yet discussed |
| Reconstruct a whole game | Game replay tests and State reconciliation, then the routed replay guide |
| Change shared multiplatform tests | Multiplatform tests |

## Test-support entry points

- [`TestHelpers.kt`](../../test/common/dev/martianzoo/tfm/tests/TestHelpers.kt) —
  search for the named helper before spelling raw task text.
- [`TfmSandboxTest.kt`](../../test/common/dev/martianzoo/tfm/tests/TfmSandboxTest.kt) —
  focused scenarios with corrections and selected phase shortcuts.
- [`TfmGameplayTest.kt`](../../test/common/dev/martianzoo/tfm/tests/TfmGameplayTest.kt) —
  scenarios following the game's own turns and phases, including focused phase tests from corrected
  starting positions.
- [`FullGameTest.kt`](../../test/common/dev/martianzoo/tfm/tests/replays/FullGameTest.kt) —
  replay setup, functional assertions, and export on `TfmGameplayTest`.
- [`CardTrackingFullGameTest.kt`](../../test/common/dev/martianzoo/tfm/tests/replays/CardTrackingFullGameTest.kt) —
  replay setup with named card arrivals, departures, and hand checks.
- [`OtbGame20260828Test.kt`](../../test/common/dev/martianzoo/tfm/tests/replays/OtbGame20260828Test.kt) —
  the TaskForm replay style, with automatic concrete follow-up work.

## Standard verification

The wrapper supports and uses the JDK selected by `JAVA_HOME` from 17 through 26. JVM code
targets the Java 17 bytecode and API surface, while Kotlin source and standard-library APIs target
Kotlin 2.2. CI uses Temurin 25 LTS. Contributors do not need another JDK installed.

Start with the smallest test or build task that verifies the changed behavior. Expand verification
only when the change crosses a wider scope or the narrower result leaves a material risk.

- `./gradlew build` checks the repository, including the English JVM tests, plus all production
  JavaScript compilation and packaging. Run `./gradlew -p pets :text:test` for the English suite alone.
  See [Pets testing](../../pets/docs/agents/TESTING.md) for its focused commands and generators.
  Use the full build only when repository-wide verification is warranted by the scope of the change
  or explicitly requested.
- `./gradlew test` runs the active repository JVM test suites, every browser-specific test, and the
  `OtbGame20260828Test` replay once in a browser. The multiplatform modules' JVM test tasks are named
  `jvmTest`. Pets runs `BrowserPetsTest` through `:pets:jsBrowserTest`; Web runs its browser history
  tests and the selected replay through `:web:jsBrowserTest`; Almanac runs its English card-text
  browser test through `:almanac:jsBrowserTest`. Other generated browser tasks are
  inert outside the intentionally commented-out full-browser target in the root build.
- `./gradlew :tfm-tests:gameplayTest` runs every `TfmGameplayTest` subclass, including gameplay
  classes nested inside sandbox suites, without sandbox, synthetic setup, or full-game replay tests.
- `./gradlew :tfm-tests:sandboxTest` runs every `TfmSandboxTest` subclass, including nested
  sandbox cases, without gameplay or full-game replay tests. Its class selection follows the
  shared test-source declarations; the JVM-only JUnit `@Tag` cannot label shared JVM/JS tests.
  To measure a fresh run without recompiling everything, use
  `./gradlew :tfm-tests:cleanSandboxTest :tfm-tests:sandboxTest --profile`.
- Temporarily uncomment `allBrowserTests` in the root build and run
  `./gradlew allBrowserTests --rerun-tasks` to exercise every shared and browser-specific suite,
  including all portable replay scenarios. The browser replay source set also reads the legacy
  `test/jvm/dev/martianzoo/tfm/tests/replays` directory, excluding only its JUnit file-export hook.
  JVM-only tools and filesystem/terminal tests remain outside this target. Comment the target back
  out after a successful run.
- `./gradlew :tfm-tests:replayTest` runs the curated replay suite and imports its checked-in
  game fixtures, without generating new files.
  `:tfm-tests:jvmTest` excludes replays and can run filtered tests independently. Use
  `:tfm-tests:test` for both JVM suites; `allTests`, `check`, and `build` also include replays.
  The explicit `activeReplayClasses` list in `modules/tfm-tests/build.gradle.kts` selects imports
  from 2026 plus the retained `Game20230521Test`. `:tfm-tests:archivedReplayTest` runs the three older
  scenarios and their checked-in imports on request; it remains outside aggregate verification.
  The optional, commented-out `allBrowserTests` task still runs every portable replay when restored.
  Add `-PreplayExports` to write a human-readable `.game` archive per successful `FullGameTest`
  subclass and check its live re-import against the source final inventory and pending tasks.
  Generated files go under the module's `generated/replay-event-logs` build directory. Export runs
  remove old JSON files. The 30 commented fixtures live in `modules/tfm-tests/src/jvmTest/resources/games/`;
  exports never overwrite them. Ordinary runs leave generated exports untouched.
  Direct JUnit runs need `-Djunit.jupiter.extensions.autodetection.enabled=true` for fixture checks;
  add `-Dsolarnet.replayExports=true` for fresh exports as well.
  See [archive verification](EXPORT.md#migration-verification) for current migration limitations.
- `./gradlew :tfm-tests:replayTestCoverage` runs only tests in the replay package and writes HTML and
  XML production-code coverage reports under that module's `reports/jacoco/replayTestCoverage`
  build directory. Its execution data comes from the separate, always-instrumented
  `replayCoverageTest` task, so ordinary test runs do not pay for coverage, and task abbreviations
  work normally. Card, rule, random-card, and browser tests do not contribute to the report.
  It uses the same curated selection and honors `-PreplayExports` just like `replayTest`.
- `./gradlew :benchmarks:jmh` runs the separate JVM-only JMH benchmarks. Benchmark execution is not
  part of the routine test or build lifecycle, though the normal build compiles the benchmark
  sources. A benchmark error fails the task instead of producing an empty successful report.
- `./gradlew spotlessApply` formats the source tree. CI runs `spotlessCheck`, and a normal build
  also reports formatting violations.
- `./gradlew :tools:generateCardNameConstants` refreshes the checked-in
  `test/common/dev/martianzoo/tfm/tests/cards/cardnames/cardNameConstants.kt` from Canon and FakeCanon.
  It includes concrete card faces and global events, sorted by class name. Run it after adding,
  removing, or renaming those declarations; review and commit the generated diff with the source
  change. Normal compilation reads the checked-in file without running the generator.
- `./gradlew :tools:kotlinFileComplexity` writes a TSV of Detekt cyclomatic complexity for each
  production Kotlin file and each test-support file without test-case methods to
  `reports/kotlin-file-complexity.tsv` under the root build directory. The report covers source physically present in this checkout, including `pets/src/` and `pets/test/`.
  It is manual rather than part of `check`, and Gradle reanalyzes only added or changed files after its first run. The
  current scope excludes benchmarks, standalone tools, and `dev.martianzoo.tfm.text`.
- `./gradlew :tools:dumpAllExpansionsEventLogs` creates three-player and solo games with every
  supported expansion plus the `fakes:cards` pool, completes Player 1's corporation-phase turn with
  Interplanetary Cinematics and four bought cards in each, and writes every change event (including
  `System` changes regardless of `Iconic`, but no task events) to
  `_local/eventlogs/three-player-all-expansions-eventlog.tsv` and
  `_local/eventlogs/solo-all-expansions-eventlog.tsv`. The `Iconic?` column is `true` when either
  side of the change has an `Iconic` type. The `actor` column is engine attribution,
  not proof of a user choice: an automatic or queued effect carried by a Player-owned component may
  attribute its derived changes to that Player. Use the cause columns to trace derivation; because
  task events are omitted, the TSV cannot by itself classify every row as chosen versus automatic.
- `./gradlew :tools:dumpOtbGame20260828EventLog -PreplayExports` and
  `./gradlew :tools:dumpOtbGame20260912EventLog -PreplayExports` run the JVM replay suite, read the
  generated physical-game recordings, and write every change event in the same format under
  `_local/eventlogs/`.
- `SOLARNET_RANDOM_AUTOMATIC_EFFECTS=true ./gradlew test --rerun-tasks` runs the unchanged JVM suites
  while choosing a random execution order for each batch of independent automatic-effect listeners.
  A component's own automatic Effects retain declaration order. This is a diagnostic mode for
  finding undeclared ordering dependencies; ordinary runs retain a stable diagnostic order.
  Run this mode periodically, especially around effect and scheduling changes. It does not vary
  Admin task execution order; that diagnostic follow-up is tracked in [TODO.md](../../TODO.md).

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
For local Solarnet builds, generated project state is isolated by account and worktree under
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
worktree. Standalone Pets uses ordinary project-local outputs and does not carry these isolation
scripts.
Yarn's incompatible `serialize-javascript` resolution warning and “Ignored scripts due to flag”
warning are expected: the former comes from the deliberate 7.x security pin while Mocha requests
6.x, and the latter preserves Kotlin/JS's policy of not running package lifecycle scripts.

## Build configuration

Convention plugins under `gradle/build-logic` are layered by responsibility. `solarnet.kotlin-base` owns
the policy shared by every Kotlin target: compilation, explicit API mode, dependency alignment,
Detekt, Dokka, and test logging. `solarnet.jvm` adds the JVM plugin and the repository's standard
Kotlin/JUnit 5 test dependencies. `solarnet.kmp-jvm-js` configures the JVM and browser targets, adds
shared `kotlin.test`, declares the same JUnit BOM, Kotlin/JUnit 5 adapter, and platform launcher
for `jvmTest` as the JVM convention, and exposes each module's `jvmTest` as `test`.
Pets uses `pets.kotlin-base`, `pets.jvm`, and `pets.kmp-jvm-js` for the same compiler and test
policy, without the previously inert Detekt setup (see [BUILDS.md](BUILDS.md)).
The Pets module also attaches its filtered browser test to `test`; its complete shared browser
suite remains available through the temporary full-browser target.
Runtime module build scripts under `modules/` select non-overlapping package roots from the
root `src/` and `test/` trees. Static modules select package roots from shared `pets/src/` and
`pets/test/` trees in the independent Pets build. Each build owns its formatting, Node.js version, Yarn policy,
and convention plugins; combined lifecycle commands explicitly include Pets verification.
See [BUILDS.md](BUILDS.md) for source selection, publication, and isolated verification.
Dependency and plugin versions are declared in `gradle/libs.versions.toml`, while dependency
repositories are declared centrally in `settings.gradle.kts`; JitPack is restricted to the pinned
better-parse fork.

`./scripts/rego` asks `:repl:shadowJarPath` to build the fat jar and report its relocated path before each
launch, including after the `rebuild` command. A Gradle failure stops the launcher. CI also executes
the packaged jar from an empty temporary directory, creates a Canon game, checks its player count,
and exits before uploading the artifact.

All Kotlin modules use strict explicit API mode. Declarations that form a module's public API must
spell out `public` and their public types; declarations used only within one module should be
`internal` or `private`. This makes accidental API growth and signature changes visible in review.

## Test design

Do not add card or rule tests whose sole purpose is to specify what happens after `exMachina`.
Corrections are test setup, not the subject or evidence of a card test. Keep assertions
focused on the subsequent player action, card behavior, or shared engine behavior. Evidence-backed
corrections inside whole-game replays remain appropriate; the replay tests the game, not correction
semantics.

> **Recurring failure warning:** Card and rule tests operate through player-facing gameplay and
> assert observable results. They do not inspect rendered task text, causes, incidental queue order,
> or mirrored Canon data. A test-support helper must express a recurring component-independent
> operation, never one card's rule or missing engine semantics.

Do not discover a task to execute with `tasks.matching { !it.deferred }.single()` in a card or rule
test. It hides which game decision the test makes. Submit the named choice. For an `EACH`
container, use `doTask("EACH")` to expose its children, then submit their concrete choices. Keep
`selectOnlyTask()` only when selecting the sole pending task is itself the intended action.

> **Default assertion style:** Almost every successful gameplay action that returns a `TaskResult`
> should chain `.expect(...)` directly to that action in gameplay tests. Sandbox tests instead
> submit concrete expected work explicitly and add expectations only for additional consequences.
> Prefer relative expectations over later absolute resource or component counts: the chained expectation proves the net change came from the action under test,
> while an absolute count can pass because of setup or an earlier action. Use an absolute assertion
> only when the absolute state is itself the contract, no successful result exists to inspect, or a
> source explicitly states an absolute value.

Terraforming Mars integration tests live under `dev.martianzoo.tfm.tests`: `cards` contains
component-focused behavior, `rules` contains gameplay events outside playing or using a specific card,
and `replays` contains whole-game chronologies. General milestone claiming belongs in
`MilestonesTest`; general award funding and scoring belong in `AwardsTest`. Card-specific failures
and payment cases belong with their cards, even when they exercise shared engine behavior.
Do not add a separate rule scenario when a retained card or rule scenario already exposes the same
gameplay mistake. Shared integrated-test support lives in the parent package.
The `tfm-tests` module is for real gameplay situations, including setup, production, scoring,
card identities, and other events outside normal player actions. Arithmetic, classification,
generic task injection, object identity, and helper-validation probes do not belong here.
Remove pointless technical probes; do not preserve them by renaming or relocating them.
The explicitly deferred configuration audit, including `ClassTableSelectionTest`, remains separate.
Other modules retain their own engine, Agent, recording, and language contracts.

**Human direction, October 24, 2026:** functional game scenarios should go through the agent
layer. Their readability must not depend on low-level task IDs. The selected engine destination
also accepts jointly sufficient task/request information; explicit IDs are not the governing API
principle. Focused raw-engine tests must exercise task matching separately from transaction-level
uniqueness, and distinguish selection from execution. Agent tests include subsequent mandatory
Admin work; raw engine uniqueness tests must exclude those later transactions. Combined
resource-total submission belongs in Agent, not engine. Restore the single production money amount;
keep unrelated card rewards written separately. See
[the engine review](ENGINE_CONTRACT.md#identifying-the-permitted-task).

### Test categories we care about

Card and game-rule scenarios use the `TfmSandboxTest` and `TfmGameplayTest` fixtures described
below. Choose the authentic game flow where the claim depends on it; focused sandbox scenarios remain
an intended style.
Dedicated REPL mode tests and lower-level engine or bootstrap tests retain their distinct subjects.
Do not replace direct phase calls with helpers that recreate the game flow in Kotlin.

These are the repository's protected test categories. Test placement may evolve, but preserving
clear coverage of these contracts matters more than preserving every current test class:

1. **Pure Pets language tests.** Owned by [Pets testing](../../pets/docs/agents/TESTING.md#test-design).
2. **Pure Pets type-system tests.** Owned by the same static-model testing guide.
3. **Game World and engine-coordination tests.** Pure `:state` scenarios verify that exact
   component/task events, materialized projections, history, completed recording positions, and
   independent playback views remain coherent without firing effects. Cross-module engine
   scenarios cover consequence calculation and recovery: test promised failure-atomic methods
   and client-controlled rollback by checking components, pending work, and history together.
   A raw call that documents partial failure need not restore itself.
4. **Player-level card and game-rule tests.** Scenarios count when they use actions and
   observations available to a player rather than internal state or implementation details.
   Named rule suites cover gameplay events outside individual card plays.
5. **Whole-game tests.** Long scenarios that show the game flow and many rules operating together,
   especially when reconstructed from independent game records. Every successful JVM replay
   verifies its checked-in archive. `-PreplayExports` additionally writes a fresh export and verifies
   its live re-import.
6. **Game configuration coverage.** The existing configuration audit is deferred. Do not add
   mirrored class inventories or generic authoring probes to `tfm-tests` under this heading.
7. **Known-defect scenarios.** Focused passing characterizations of important behavior known to be
   wrong, explicitly identified as defects. Card tests use the paired pattern described
   under [Known-defect tests](#known-defect-tests); other suites retain `BugsTest`.
8. **Script-command contract tests.** Terraforming-independent checks of each command's public
   contract. These are useful interface coverage even though they are not a development priority.
9. **Cross-runtime browser coverage.** Browser-specific tests cover browser APIs, one representative
   replay checks the shared engine on JavaScript, and production JavaScript compilation and resource
   assembly cover the Web REPL and Almanac.

This list does not itself decide which current tests should be retained. The November 19 direction
below explicitly authorizes removing artificial gameplay probes and assertions of implementation
details; it does not require moving those probes into another suite.

Give each scenario variant its own named test, using a private helper for shared steps. Do not put
variants in a `for` loop inside one test: the failing test name should identify the case. Exercise
the behavior under test through gameplay. Prefer short, plausible real play for incidental
preparation; use corrections when authentic setup would be tedious, distracting, or artificial.
Choose direct setup and phase invocation case by case, as described under
[Sandbox and gameplay fixture migration](#sandbox-and-gameplay-fixture-migration).

Prefer tests that exercise several pieces together. Do not mirror a production list or data object
in a test just to detect that the list changed. Test observable behavior through the normal
test-facing layer: test the card, rule, or game-flow result rather than a private transformation,
exact intermediate task text, or other implementation detail.

Keep task-routing mechanism tests in the generic engine suite. Those tests may inspect the current
assignee, selection, and event Actor because those are the contract under test. A player-level card
or rule scenario should instead demonstrate routing
through public gameplay: which Player can select or narrow, whether competing gameplay is blocked,
the resulting state, and, when necessary, an authored `BY` reaction that makes attribution
observable. Do not locate card reactions by exact rendered instruction, `Task.cause`, internal
assignment fields, or raw Event Log inspection.

For delegated payment, final resource totals alone do not establish correct task routing or order.
A helper may legitimately call several Actors' Agents; this is not an authorization bypass.
Exercise separate Agent commands when needed to expose the selections and choices each requires,
and test intervening work against the intended game outcome. Do not require the engine to identify
who operates an Agent or reject a program merely
because it calls another Player's Agent. [ADVERSARIAL.md](ADVERSARIAL.md) assigns that submission
check to the external game arrangement. [SEQUENCING.md](SEQUENCING.md#the-missing-rule-when-initiated-work-has-finished)
records the remaining operation-level questions. Do not preserve former routing fields merely to
keep synthetic characterizations passing.

Keep trigger matching separate from queue routing. A `BY` characterization should show which
triggers fire and how Actor variables bind through observable changes. Do not make its continued
success depend on an incidental assignee unless that test is explicitly about delegation.

Keep gameplay and test APIs generic. Never add a Kotlin helper or DSL operation solely to represent
one card, corporation, Prelude, or other component. Use existing gameplay helpers when their
operation scopes fit. When component-specific steps must stay inside an outer operation, express
them through existing `OperationScope` primitives so any sibling task may remain pending. Add a
shared helper only for a recurring, component-independent concept that materially simplifies
several call sites. Test helpers must not repair the game model by creating or relocating rule
components, imposing order absent from Pets or the engine, or identifying work by rendered text or
cause.

Do not inspect Canon declarations or definitions and assert their exact Pets trees or rendered
strings. Do not assert card totals by bundle, deck, expansion, or other content group. Canon
admissibility is intentionally a compact loading and composition gate; card and rule behavior
belongs in player-level scenarios.

Keep scenarios minimal and legible. Project-card tests that fit the standardized fixture follow the
migration rules below. Other card tests use the smallest suitable configuration and consistently
name their gameplay objects. Use `runOperation()` when an operation's resulting setup matters and a
correction is not suitable. Eligible project-card scenarios use the fixture's explicit `exMachina` setup methods.
Generic engine checks belong in the engine suite, rather than synthetic card/rule scenarios.
Tests of a specific fixture or card-tracking contract may use the existing `canonicalPremise` and
`setUpGame` functions or assign a prepared world to the sandbox's `game`. `WildTagTest` and
`WildTagContinuationTest` cover allocation and cleanup through prepared sandbox actions.
When a custom instruction reads authored card metadata from the catalog, compose the synthetic
card into a fixture `TfmCatalog`; premise-only declarations do not populate that metadata.
Sandbox and gameplay scenarios use explicit Agent task submissions. The fixture-specific
operation and payment conventions are described below.

Rule suites use the same player-level style for events outside individual card plays. Their scenarios
should reproduce only the important preconditions observed in whole games and use explicit sandbox
tasks with expectations for additional consequences.
Full-game tests override a `config` property with a `GameConfig`, conventionally built from an
indented multiline string followed by player-name varargs. Catalog-backed premise resolution adds
`TerraformingMars` and, when no other map is named, `TharsisMap`; the parser already trims each
entry, so these literals do not need `trimIndent()`. Solo tests conventionally use `Me` as the
canonical Player Class Name and use `Player.PLAYER1` in Kotlin.

### Command abbreviations

The user permits fixed command sequences in otherwise explicit `doTasks` tests, including selecting
a standard action and then the particular card or project. `standardActionTasks.kt` returns command
arrays determined only by its arguments. The caller submits them through ordinary `doTasks`.
Prefer `doTasks` for consecutive submissions within an action, but never group separate actions
into the same call. Keep separate calls for intervening assertions, comments, and player handoffs.
These functions receive no Agent or World and must not inspect game state, pending work, resources,
policies or card data to change their commands. Do not add payment selection, retries, cleanup,
corrections or missing game effects. Keep payment choices and outcome assertions visible in tests;
representative lifecycle tests also retain literal invocation commands.

### Sandbox and gameplay fixture migration

Choose the fixture by what the scenario proves, not the kind of card:

- `TfmSandboxTest` supplies a prepared position, explicit corrections, and selected phase shortcuts.
  Use it for focused card interactions and rules that do not depend on authentic turn/phase history.
  With `newTestGame()`, its game enters the corporation phase normally; the fixture then drops
  the turn order that phase begins, so scenarios take turns ex machina. Its private phase correction places
  later phases without their queued start-of-phase rules. Use `TfmGameplayTest` when the transition
  into production, solar, or research is part of the claim. A focused rule scenario may instead
  invoke a real phase effect directly when the surrounding traversal would be incidental.
- `TfmGameplayTest` starts the game and lets its own rules offer every turn and begin every phase.
  When prior play matters to the claim, reach the scenario through that play, including production
  to obtain resources. **Human direction, November 12, 2026:** focused phase tests may instead
  prepare an incidental starting position with visible, commented `exMachina` calls. For example,
  supply plants for final greenery or arrange for the current generation to end. From that position,
  let real turns, passes, production, and phase transitions exercise the behavior under test; do not
  replace a phase or inject the consequence being asserted. A setup convenience may play a
  documented ordered selection of corporations or Preludes in turn order; convenience must not
  bypass their gameplay. The implemented default already plays the starting corporations; Prelude
  choices remain explicit.

Gameplay tests submit the turns the game offers through plain `Agent`s. Use `doTask`/`doTasks`
for explicit choices, or `completeOperation { ... }` when the whole operation must finish before
returning. A success scenario must actually complete the action or phase it claims to prove;
submitting an intermediate choice is insufficient. Negative scenarios may stop at the expected
failure. Do not use `inTurn`: it can create an out-of-turn offer when the actor has no pending
work. Use `player.turn { ... }` for complete action-phase turns. It declines an unused second
action only while opponents remain. Setup, final greenery, and generation passes stay outside it.
Focused handoff tests decline the optional offer with `doTask("Ok")` and assert the resulting turn
holder. Players never submit `PassBaton`; Baton owns completion.

`TfmGameplayTest` exercises authentic game flow. The fixture owns its `World` and `Agents`
directly and exposes Admin and all players as plain Agents.
Scenario payments state concrete amounts, including research purchases; do not use `/ Owed` or
payment helpers. Shared corporation setup alone reads the bill for its five starting projects,
so corporations such as TerraLabs use their actual purchase price.

State the steps explicitly when testing their order. Finish or decline alternative payment
offers before the exact remainder, and explain each `Ok`. Fixed command abbreviations may
shorten repeated invocation steps; payments and gameplay outcomes remain explicit. Loops remain
appropriate for seat-order traversal and long generation/endgame progressions.

Focused rule or card-tracking scenarios may instead assign `game` directly. This creates fresh
`Agents` with Player autoexecution set to `NONE` and Admin set to `EAGER`, without preparing a position or binding
`players`, `kim`, `stan`, or `rob`;
use `agents[player]` and `admin`. These subclasses also run under `:tfm-tests:sandboxTest`,
so that task measures both prepared-position scenarios and directly initialized rule scenarios.

Focused rule scenarios that only need a prepared starting position may use `setUpGame`.
It finishes incidental opening choices with scoped `EAGER`, drops setup's pending handoff,
and leaves the game in `SetupPhase`. Callers own `Agents(game) { NONE }`; do not use the older
`World.testAgent()` cache, which explicitly retains Player `EAGER` for older engine fixtures.
`startCorporationPhaseOutOfTurn` starts the corporation phase and drops its turn order.
These shortcuts do not justify a scenario with no real gameplay subject.

Sandbox and gameplay each own their plain `Agent` fixture directly. Both expose
`newTestGameBeforeCorporationPlay()` for explicit corporation plays; a normal
`newTestGame(kimCorporation = ...)` plays the corporation during setup. `kimCorporation` never
changes the game configuration; use `addOptions` to include cards outside the default bundles.
Each owns its setup; there is no intermediate shared base. Neither is restricted to project cards.
Their setup defaults remain aligned; differences follow from prepared positions versus authentic
play.
Shared rules get representative coverage using base-game content where possible: for example,
`rules/RequiredFirstActionTest` uses Inventrix rather than repeating the same rule for each
corporation with a required first action.

Both normally return at generation 1 Prelude phase when `PreludeExpansion` is selected, otherwise
Action phase. `newTestGameBeforeCorporationPlay` instead returns before any corporation is played;
Kim takes the nonbeginner setup path, and the test makes the corporation plays and their choices
explicitly. Select a card through `addOptions` when it is outside the default bundles, so it can
appear in Kim's offer. Do not add extra arguments for corporation offers or project purchases.
Select `kimCorporation` to give Kim the normal corporation path; other seats use the beginner path.
Both fixtures discard five of Kim's ten offered projects before corporation play. Normal setup
then purchases the remaining five; explicit corporation-play tests make that purchase themselves.
The default game has:

- Tharsis, Venus Next, Colonies, and Promos;
- Corporate Era and the Beginner and Quick Start variants;
- Kim, Stan, and Rob, in that order, each using a distinct beginner corporation; and
- the ordinary beginner-corporation starting state, including 42 MC, 10 anonymous project cards,
  20 TR in multiplayer (14 in solo), and production of 1 for each standard resource.

The earlier entry points intentionally do less. `TfmGameplayTest.initializeGame(premise)` only
constructs the world and binds its Agents; it does not begin Setup. `FullGameTest.commonSetup`
uses that entry point and optionally starts recording. Its card-tracked counterpart additionally
registers expected card identities and movements, without choosing or playing those cards.
Replay `startGame()` begins Setup through Admin. Ordinary replay Players still have to take their
starting TR, choose their starting cards, and play and pay for their corporations explicitly.
The TaskForm replay's selected automatic policy intentionally changes how much concrete Player
work proceeds at that entry point. These differences do not require another shared setup API.

For corporation-named suites, prefer sandbox unless the scenario needs real phase progression.
Use `kimCorporation` for the subject corporation. In sandbox tests of its initial play, call
`newTestGameBeforeCorporationPlay` and assert the result of playing it explicitly. Tests of an ongoing
effect or action can use the default setup with that corporation already played. Acquiring another
corporation through Merger instead belongs in the phase where Merger is played.

Kim is the player exercising the subject card unless the card's behavior requires another actor.
The standard game has three players. A scenario may instead pass `playerCount` from one through
five; Maya and Nadia occupy the fourth and fifth seats. The protected `players` list exposes every
seat in order. Accessing `stan` in solo or `rob` with fewer than three players fails immediately
with a fixture error. Both styles resolve neutral solo placements through the setup tasks, using
`soloTileAreas` with fixed card costs 11, 10, 12, and 8 on the selected map.

Every sandbox test receives an independent prepared World. The base class does not create a game
automatically. Gameplay creates a fresh World and
starts the game for every call. Use `@BeforeTest` when every method needs the same setup.
When some methods need a different setup, call the common setup explicitly from the methods that
need it and initialize the exceptions directly. Never prepare a game only to replace it at the
start of a test. Keep scenario-specific corrections visible in the test. Pass named arguments such
as `addOptions = "CimmeriaMap"` or `playerCount = 4` where needed. Pass `kimCorporation` only when its
live effect is part of the scenario; Kim then plays that corporation normally and resolves its setup
effects, while every
other player keeps a beginner corporation. Do not share a live World between tests.

Prefer selecting an individual card in `addOptions` when the scenario does not need its expansion's
other rules. Negative selections such as `addOptions = "-ColoniesExpansion, TitanShuttles"`
remove an expansion from the fixture defaults. For cards with a non-obvious expansion dependency,
include focused scenarios with and without that expansion. Keep assertions centered on card
behavior; configuration checks normally belong to the existing configuration tests. Do not
duplicate this coverage for every expansion card.

Use `OtbGame20261009Test`'s explicit task style. Card and rule scenarios retain the prepared position
and visible `exMachina` corrections, then use the fixture's
`inTurn { doTasks(...) }` calls in place of Terraforming Mars gameplay helpers.
Admin handling follows [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md). `inTurn` supplies the sandbox turn
and checks that the complete action leaves no
pending tasks; payments, card transfers, player effects, and any player-assigned cleanup are explicit.
The sandbox exposes its players as plain `Agent`s. For corporation scenarios that exercise the
initial play, use `newTestGameBeforeCorporationPlay` and submit the printed effects and starting-card
purchase in the test. Scenario payments finish or decline
alternative resources before submitting the exact remainder in the bill's denomination. If those
alternatives or a pending discount cover the full cost, submit `Ok` before the purchased result.
Pharmacy Union's initial-play scenarios explicitly take the gated 54 MC, place each disease and
handle its loss, then pay the full starting-card bill. Ordinary losses do not credit a bill;
see [PAYMENTS.md](PAYMENTS.md#ordinary-resource-losses).
In this style, omit `.expect()` assertions that merely repeat the submitted tasks; retain assertions
for automatic consequences, excluded outcomes, and state after rejected actions. Continue using
`setToExMachina` for desired absolute counts rather than calculating relative fixture adjustments.
Prefer compact `FROM` expressions, such as `Steel<Kim FROM Rob>` and
`ProjectCard<Hand FROM Selecting>`, whenever the transmutation shares the same Class.
Do not retain Terraforming Mars gameplay helpers in sandbox scenarios, including indirect calls
through helpers. Preserve payment and card rules through explicit tasks.
Nested `TfmGameplayTest` suites follow the real game flow.
Use `selectTask` and `selectOnlyTask` only for a choice still needing narrowing, structural expansion,
handoff, or disambiguation, never to carry out concrete player work. Every retained selection must
have a local comment explaining why it is necessary. Prefer a short named selection, such as
`selectTask("PlaceGreeneryTile")`, to an anonymous selection. String selection also narrows the
task; it is not a name-only lookup. Open an `EACH` group with `doTask("EACH")`.
Keep `selectOnlyTask` when a short instruction would discard part of a linked choice, as in Flooding's
placement and attack.
Use `doTask` for the Actor's explicit work, naming a short, meaningful instruction.
This also applies when binding X makes player work concrete: submit the chosen resource amount
directly. Do not copy full rule expressions merely to identify the sole task.
Write expected payment amounts explicitly, such as `6 Pay<> FROM MC`; never pay `/ Owed`
in a sandbox scenario. Write `kim.inTurn { ... }` directly, without redundant `with(kim)` wrappers.
Inside that block, helpers on its operation receiver do not repeat `kim.`; another actor remains explicit.
`TfmSandboxTest` supplies its own `Agent.inTurn` member extension: it uses `runOperation("OfferAction")`
when no task is pending, and otherwise `completeOperation`. This keeps the generic Agent free of
Terraforming Mars rules while sandbox scenarios retain their explicit operation scripts.
Use `doTasks` for consecutive fully specified instructions. Preserve order when it matters to the scenario.
Comment an ambiguous `Ok` to identify what is being declined.
Write scenario actions in sequence rather than loops. Small private scenario helpers may factor
repeated explicit task sequences, with varying bonuses passed at each written-out call.
Call the base fixture's `advanceUntilActionPhase()` to reach the next Action phase. From Action,
it advances a generation; the sandbox currently expires generation-scoped state without running
production or research. The base fixture owns that degree of simulation. From Corporation or
Prelude, it skips remaining setup plays and enters Action phase.
When Turmoil is selected, it uses the existing test setup's initial global-event reveals
(`DemocraticReform` and `MinimalImpactPolicy`) before entering Corporation phase.

Treat every pre-migration test as a fallible historical artifact, not as a specification of its
setup. Existing options, cards, resource grants, card plays, phase changes,
and operation order may be incidental, copied from another scenario, obsolete, or compensating for
old behavior. Begin with the behavior named by the scenario and its meaningful observable coverage.
Challenge every setup step: if removing it does not change the behavior under test, remove it. For
sandbox scenarios, do not preserve gameplay merely because the old test used it to reach a state
that a correction can express. Gameplay scenarios must exercise the behavior under test through
play; focused phase tests may correct an incidental starting position as specified in
[Sandbox and gameplay fixture migration](#sandbox-and-gameplay-fixture-migration).
Do not infer a configuration requirement solely from the old test's configuration.

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

Choose direct setup and phase invocation case by case; this does not need to be consistent across
test classes. A focused rule test may invoke a real phase or rule effect with `runOperation` or
`beginOperation` when authentic traversal would add tedious or artificial setup. Explain a
non-obvious shortcut locally. Exercise and complete the actual behavior being claimed; never
inject the outcome being asserted. When phase ordering or action legality is the subject, use
the real flow. A raw operation does not excuse a technical-only scenario.
The sandbox provides
`advanceUntilActionPhase()` to leave explicit corporation setup or skip unplayed Preludes when their play
is irrelevant; finish pending choices first. This is a sandbox shortcut, so other unplayed starting
cards need not be handled when they are irrelevant to the scenario. Gameplay leaves those choices
to the test and advances through the game's normal flow.

Prefer real setup play when it is straightforward and produces a plausible situation.
Astra Mechanica's previously played events are established by actually playing those cards.
Use visible corrections when authentic setup would be tedious, distracting, or artificial:

- Use `setToExMachina(targetCount, type)` for a desired absolute count. Admin may use it for
  System markers such as colony tracks. It calculates the gain or
  loss from the current count, so the scenario states its intended condition rather than assuming
  the fixture's prior value. Production types such as `PROD[Heat]` work the same way, including negative printed production.
- Use `exMachina(adjustment)` for a naturally relative change or a known absent-to-present fact.
  Prefer a concrete Type such as `NormalCityTile` when an abstract Type cannot be created directly.
- If initial play is incidental and reproducing it would burden the scenario, install the card
  directly and correct its necessary independent state. Do not mechanically replace a short,
  natural card play with a correction merely because the play is setup.
- If initial play, payment, requirements, direct effects, or entry-trigger interactions are
  the subject, play the card normally and establish whatever legal setup that action requires.
- To test behavior at a completed global parameter, correct the track to its penultimate step and
  use an ordinary standard project for the final step when available. This preserves the real
  completion lifecycle, including `GpComplete`, instead of asking a correction to stand in for it.
- `advanceUntilActionPhase()` expires generation-scoped state without production or research.
  A scenario that needs phase ordering belongs in gameplay. A focused rule test may invoke
  the relevant phase effect directly; do not broaden the sandbox shortcut into another game driver.

Corrections intentionally suppress ordinary queued effects while retaining the structural work
documented in [EX_MACHINA.md](EX_MACHINA.md). Therefore, never treat the correction itself as proof
that gameplay works. Assertions should begin with the action or interaction that is the scenario's
subject, and should omit setup deltas.

Authentic setup through gameplay remains valid and is sometimes preferable. Use it deliberately
when the path into the state matters: trigger history, attribution, task ordering, payment,
requirements, card-entry effects, global-parameter completion, game flow, or interactions among
setup actions. It is also appropriate when a broader integration scenario is valuable in its own
right. Short, plausible preparation through real play is preferred even when that history is
incidental. Correction is appropriate when it avoids burdensome or artificial preparation.

Review each retained scenario before selecting its fixture. A coherent card suite can use gameplay
throughout even when some methods could use sandbox; `PreservationProgramTest` follows this style.
When most methods suit sandbox and only a few need the game flow, a normal nested class may extend
`TfmGameplayTest` inside the sandbox class. It owns its own fixture and does not share the outer
instance; do not use Kotlin `inner` or JVM-only `@Nested`. See
`TurmoilDelegatesTest.Gameplay` for focused phase scenarios. When filtering by the outer class name,
include a trailing wildcard to include nested classes
as well. Do not retain synthetic declarations without an actual gameplay scenario.
Do not replace meaningful coverage just to increase the migrated count. An existing base class does
not itself express a preferred style.

#### Remaining fixture development

- Extend the neutral-placement defaults in both bases when a solo scenario needs a map other than
  Tharsis, Hellas, or Elysium. Solo alone does not select gameplay over sandbox.
- Reassess the sandbox's allowed phase shortcuts from concrete needs. A general `advanceTo(Phase)`
  is no longer presumed necessary. Do not introduce a corporation-specific base without a need
  beyond what these two styles already provide.

Investigate whether attack-history markers used by Law Suit and Crash Site Cleanup should be
created by automatic (`::`) effects. They currently use queued (`:`) effects, suppressed by
corrections. Decide from their intended meaning during corrections and ordinary play; migration
convenience alone is not justification for changing effect semantics.

For unusual injected sequences, seek a credible gameplay route. Delete a scenario if no such route
exists rather than adding fixture machinery to recreate it. A route possible only through Fake Head
Start does not by itself establish useful supported-game coverage.

When rewriting rule suites, review each retained scenario's value, setup, assertions, and name
within the batch; larger batches do not relax those checks. Track open
fixture work in `TODO.md` rather than treating these intentions as already available APIs.

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
aborted actions have no successful `TaskResult`, so assert their relevant unchanged state.
Use a zero scalar, such as `0 Plant` or `PROD[0 Energy]`, to assert that a particular type did not
change.

An "as much as possible" submission does not state its actual result. Keep an outcome expectation
when that result is what the scenario proves: `-5* MC<Rob>` can remove less than five, and
`Director<BoardOfDirectors>*` can gain zero. Check inherited defaults too: card-resource gains
and global-parameter gains/removals default to AMAP even without a written `*`; RobotUnit is a
card resource. Explicit `!` overrides that default. An existing result check or required follow-up
may already prove the gain: Topsoil Contract's microbe must actually arrive before its MC reward
can be submitted, and a player's parameter increase must happen before its TR can be submitted.
Do not remove an outcome check merely because its requested quantity appears in the task text.

A `TaskResult` includes all retained changes through the call's return, including automatic work.
Gameplay tests use `expectChanges("...") { ... }` when the observation must span several calls or
choices in the same phase. This assertion reads change events across the block without changing
the commands' behavior. Place it immediately around the relevant choices, and use
explicit player holders for cross-player effects. For example, a global-event payout may occur
before its ocean choice, so observe both World Government and that placement together. Do not
save pre-action counts and assert arithmetic against them. Retain literal absolute assertions for
phase identity, complete final scores, and unchanged state after a rejected operation.

Cover meaningful interfaces, negative cases, non-targets, and option combinations rather than only
the happy path. A filtering or Type-variable test should include several tempting Components that must not
match. Preserve this coverage during refactoring.

Absolute checks should be rare. Prefer `.expect()` for the action's relevant net effects. When
an absolute check is needed, establish a known initial amount and assert a literal result; do not
save an old count and assert `oldValue + change`. `assertCounts` remains useful for several relevant
absolute facts; prefer it to repeated `count(...) shouldBe ...` checks when possible. `assertProds`
reports signed printed production by subtracting `ProdOffset`.
Neither replaces a suitable relative expectation.

Assert the actual exception type with `shouldThrow<ExpectedException>`; do not use `shouldThrowAny`
or a catch-all superclass. An unrelated failure must not satisfy a rejection test. Also check the
relevant unchanged state when the API promises restoration or the client supplies a restoring
block. For raw engine calls that can leave partial changes, test the client's rollback path instead
of requiring the call alone to restore state. Check the diagnostic when it helps identify the failure.

### Known-defect tests

Card and rule tests use adjacent test pairs in the class for the behavior involved:

- An `@Test` with `@Ignore` states the intended behavior. Add a short comment explaining the defect
  that keeps it disabled, and preserve any existing rule-source reference beside the pair.
- An active `@Test` with a name beginning `BUG - ` states the current incorrect behavior.
- Share scenario steps through a private helper, keeping the differing expectations visible in the
  test methods. For a currently rejected action, the characterization catches the specific exception
  and checks unchanged state; the intended test calls the same helper and expects success.

Follow the standard card-test fixture conventions above. Do not add shared infrastructure for
pairing. Verify that the intended test fails for the described defect before disabling it. When
the defect is fixed, enable that test and remove the obsolete characterization; inline the helper
if it no longer serves multiple tests. `MiningRightsTest` and `EcologyExpertsTest` are compact examples.

Other packages retain `BugsTest`: its passing tests characterize known
incorrect behavior, and their names say what currently happens incorrectly. Prefer a clearly marked
characterization over a disproportionate workaround.

## Gameplay consistency project

**Status: selected migration complete, including the final November 19 cleanup. The configuration
audit is explicitly deferred.** This section records the user's
responses to the gameplay consistency audit, the implemented criteria, and the intentional remaining
differences. Proposals and observations not yet discussed remain distinct from the completed scope.
Separate future work is in [`TODO.md`](../../TODO.md#gameplay-consistency).

### Final scope clarification, November 19, 2026

- Direct effect invocation versus full game traversal does **not** need uniformity. Judge whether
  the setup is short and plausible and whether the actual claim is exercised. Production, scoring,
  and other behavior outside normal player actions are legitimate `.rules` subjects.
- Remove implicit Player `EAGER`, including standalone callers of the older `testAgent` helper.
  Admin stays automatic; scoped incidental opening and the TaskForm replay remain exceptions.
- This module tests real gameplay situations. Delete generic arithmetic, classification,
  injected-task, helper-validation, and synthetic-authoring probes rather than relocating them.
- The `ClassTableSelectionTest` configuration audit is explicitly deferred to another day.
- Prefer actual card play for easy, natural preparation. In Astra Mechanica, play the earlier
  events and flip Pharmacy Union through its science-tag effect rather than fabricating history.
- Keep ProductionPhaseTest's short two-generation energy-conversion scenario: the first production
  establishes old energy, and the second shows old energy becoming heat alongside new production.
  An extra generation is not itself a migration defect.

Implemented and verified: all retained standalone callers now own explicit-Player Agents;
`tfm-tests` has no `testAgent`/`testAgents` callers or implicit Player `EAGER` construction.
Remaining eager execution is Admin, scoped incidental opening, and the TaskForm replay.
The cleanup deleted 70 technical-only or duplicate fixture cases, including `MetricTest`, generic
task-injection and helper probes, synthetic authoring checks, and tracking input-schema probes.
Actual card identity, search, staging, ownership, and play scenarios remain. All five Astra Mechanica
cases now establish their earlier events through real plays. The configuration audit was left alone.

Final verification: `:tfm-tests:jvmTest` ran 1,261 tests: 1,235 passed, no failures or errors, and the
same 26 existing skips. `:tfm-tests:compileTestKotlinJs`, `spotlessCheck`, and `git diff --check`
passed. Production, build configuration, and replay files were unchanged in this final cleanup.

### Additional decisions, November 19, 2026

- Card and rule tests must describe gameplay a non-engineer player can understand. Remove generic
  property/metric probes and artificial scenarios without a concrete gameplay purpose; do not
  preserve them by renaming or relocating them. Keep actual milestone requirements as paid claim
  scenarios. Repository infrastructure checks still need a specific contract to justify their place.
- Do not repeat an exact submitted gain, loss, payment, or placement in an expectation.
  Keep net-result assertions where automatic consequences alter that result, excluded outcomes,
  resolved AMAP outcomes, rejected-action restoration, and source checkpoints. Action-used markers are bookkeeping;
  prove availability by actually using the card or attempting another use when that is the subject.
- The differing scopes of `assertCounts` and `assertTags` are accepted. Do not normalize their
  coverage or change omitted tag categories merely to make their notation alike.
- Prefer generated card-name constants in instructions, corrections, and assertions. Preserve
  the resulting task text. Omit an actor name when the receiving Agent already supplies it;
  preserve explicit other-player targets and ownership that disambiguates a task.
- Prefer `doTasks` for consecutive steps of the same action. Never combine separate actions into
  a call. Keep separate submissions where an intervening assertion, comment, or choice matters.
- Direct sandbox World assignment must also give Players `NONE`; automatic execution requires
  an explicit policy test. Ordinary tile placement and tracked-card play are not policy tests.
- For Supercapacitors, prepare the incidental old energy and heat directly, then execute real
  production. This scenario-specific choice does not require removing preparation generations
  from other tests; apply the setup judgment described above.

Acceptance checklist for this pass: apply these rules without changing production behavior, retain
the agreed four test styles and standard-opening exception, preserve meaningful gameplay outcomes,
and verify both the source diff and the affected JVM/replay/browser suites.

Implemented in this pass:

- Removed `PropertyTest`, `CustomMetricTest`, `PremiseEffectsTest`, `ApiUtilsTest`,
  `DeterminedZeroCostActionTest`, `GenerationScopeTest`, `ActionSequencingTest`, and Vermin's
  synthetic attribution probe. These probes were deleted rather than moved.
- Replaced raw milestone-property queries with four paid claim scenarios in
  `MilestoneRequirementsTest`. Replaced the internal `ActionScopeTest` checks with two actual
  turn scenarios in `TurnCompletionTest`: yield after two actions, and finish Philares' reward
  before proceeding. Existing card and phase scenarios cover the discarded duplicates.
- Removed expectations repeating explicit task effects and costs, plus incidental action-marker
  assertions. AI Central now demonstrates that Inventors' Guild remains usable by using it.
  Net consequences, excluded outcomes, rejected-action restoration, and replay checkpoints remain.
- Converted ordinary quoted card names to generated constants, omitted redundant actor names,
  grouped consecutive steps within actions, and split existing calls that combined distinct actions.
  A nested play granted by Merger, Ecology Experts, Viron, or Project Inspection remains part of
  its outer action.
- Made direct sandbox World assignment use explicit Player execution too. Tracked-card and tile
  scenarios now submit their actual draws, payments, gains, and placement bonuses. Supercapacitors
  starts with corrected old resources and then runs real production.

The remaining configuration-contract audit in `ClassTableSelectionTest` is recorded separately in
`TODO.md`. Generic engine, fixture, recording, and tracking tests still have their own technical
contracts; they are not substitutes for player-level card/rule scenarios.

November 19 verification: `:tfm-tests:jvmTest` ran 1,331 tests: 1,305 passed and the existing 26
intentional skips remained unchanged. All 29 active replays passed with exports enabled; their 58
full/short JSON files were byte-for-byte identical to the files saved before this cleanup. All seven
archived replays passed through a temporary Gradle task without changing normal suite selection.
`:tfm-tests:compileTestKotlinJs`, both `:web:jsBrowserTest` cases, `spotlessCheck`, and
`git diff --check` passed. No production files changed. The lower ordinary test count reflects the
explicitly selected removal of technical probes and their focused gameplay replacements, not new
skips or reduced suite selection.

The pre-commit review identified removed AMAP outcome checks and an incorrect Tactician test name.
The follow-up audit compared removed expectations with both the committed tests and the snapshot
before assertion cleanup, including inherited CardResource and GlobalParameter defaults. It restored
32 outcome terms at 31 assertion sites: resource gains, Rob's Recession loss, and Admin's parameter
changes where no remaining check proves the result. Exact commands, retained result checks, and
required consequence submissions
remain sufficient elsewhere. Tactician's scenario counts cards with requirements, including a
production requirement; its name no longer incorrectly restricts them to global requirements.
After these fixes, `:tfm-tests:jvmTest` passed with 1,305 passed and the same 26 skipped tests;
`:tfm-tests:compileTestKotlinJs` and `spotlessCheck` also passed.

The intended families remain sandbox, gameplay, gameplay with card tracking, and the separate
TaskForm style. Differences required by card tracking are not cleanup targets. `TfmGameplay.kt`
has been deleted; keep shrinking the remaining gameplay API rather than recreating its helpers.
The user does not require preservation of unused-action or underpayment/overpayment checks merely
because old replay helpers performed them. Do not restore the discarded `tfm_sample` REPL command.
These permissions do not imply removing source resource checkpoints or changing recorded payments.

### Migration acceptance criteria and completion, November 17, 2026

The user selected these criteria for completing the migration. Apply them together when revising
or reviewing each batch; the narrower historical decisions below are superseded where noted.

- Keep the four intended gameplay families. Ordinary Player work is explicit; Admin remains
  automatic, and the TaskForm replay retains its deliberately different interaction style.
- When the corporation turn is incidental, the shared fixture may use scoped `EAGER` execution
  for the entire standard opening, including starting-card setup, corporation play, and buying
  five of ten offered projects on the nonbeginner path. Eliminating otherwise valid Player
  choices is explicitly acceptable in this convenience. Tests of those choices or consequences
  must instead show them. Do not extend this exception to ordinary scenario execution.
- Use visible `exMachina` setup instead of manufacturing an implausible long gameplay history
  merely to reach the tested position. EndgameRulesTest's corrected setup replaces its former
  41 preparation generations. Exercise the subsequent action or phase through the game's real flow.
- A successful intermediate step does not prove an action succeeds. Success scenarios must
  actually complete the action or phase they claim to prove; unrelated later turns need not run.
  Negative scenarios may stop at the expected failure. Assertions may inspect intermediate state,
  but a successful path must then finish its action.
- Remove counterfactual illegal-action probes from ordinary replays now. Do not replace them
  during this migration. A future approach to testing illegal moves from recorded positions is
  separate work. This does not remove focused negative card/rule tests or corrections reproducing
  actual recorded mistakes.
- Historical authentication is a separate project. Passing tests and imports demonstrate a game
  the current model can execute; they do not by themselves establish that it is the game actually
  played. Preserve source checkpoints, discrepancy comments, and visible corrections, but do not
  block this migration on resolving historical scores or recovering missing map evidence. An
  unauthenticated discrepancy may conceal a real Solarnet defect; that uncertainty is accepted
  for this migration, not a reason to describe the record as independently verified.

These criteria do not authorize rewriting replay chronology, changing recorded payments, inventing
source evidence, or adding a new gameplay API. Keep literal submissions and meaningful consequence
assertions, and review the final diff as well as running the affected suites.

The November 17 phase-flow batch also makes starting purchases and milestone claims finish, and
removes incidental multi-generation preparation from `SolarPhaseOrderingTest`, `GoalsTest`,
`ThawerTest`, and influence-focused card scenarios. Prepared current-event positions include their
pending global-effect component explicitly; the tests then exercise real production, World
Government, event resolution, elections, and research as needed by their claim. They do not inject
the resulting payout, milestone credit, or phase transition.

`TfmGameplayTest` now defaults to `NONE`, and all ordinary gameplay subclasses use that default.
The remaining inherited-policy callers have been migrated, including all 42 global-event cases
and the 43 Preservation Program cases (five remain intentionally ignored known-defect expectations).
The latter retains real Prelude and generation history where timing is the subject, but uses
corrected tracks, card resources, and payment positions for incidental preparation. The old
state-driven `passGeneration`, temperature-preparation loop, and generic Prelude autoexecutor are gone.

The completion audit also covers scenarios that already used `NONE`: action-scope tests now finish
the newly opened action after inspecting it, and research-purchase tests complete Research. Negative
tests retain their expected failures. Known-defect pairs still distinguish intended and current
behavior; this migration changes no production rules.

The remaining execution differences are intentional:

- Sandbox scenarios use explicit player tasks inside an injected action offer; gameplay scenarios
  use the real turns and phases. Both may prepare incidental positions with visible corrections.
- The shared standard opening uses scoped `EAGER`; ordinary scenarios do not change player policy.
- Ordinary replays use explicit tasks. Card-tracked replays additionally supply card identities.
  The August 28 TaskForm replay retains `EAGER` for its separately tested form interaction style.
- Direct setup and phase invocation are intentional judgment calls. Player execution remains
  explicit in the retained scenarios. Technical-only World/Agent probes are outside this module.
- A selection may identify otherwise ambiguous work before explicit execution. For example, two
  zero-valued global-event rewards cannot both be identified by `0 MC`; `TurmoilEventsTest` selects
  the influence term and then submits `0 MC`. Linked choices, requirement failures, and Admin
  handoffs retain locally explained selections. Use `doTask` for Pharmacy Union flips.

The replay inventory finds 147 visible `exMachina` calls across eleven replay files, including the
TaskForm replay, and no remaining task drops or counterfactual illegal-action probes in replay
classes. The corrections cover recorded mistakes and later reconciliations, unsupported-card
approximations, and explicitly unresolved source discrepancies. Their chronology and source
checkpoints remain intact; historical authentication and any later retcons remain separate work.

Final verification: `:tfm-tests:jvmTest` passed 1,329 tests with the existing 26 intentional skips;
`:tfm-tests:replayTest -PreplayExports` passed all 29 curated replays and wrote 29 full/short recording
pairs. `:tfm-tests:compileTestKotlinJs`, `:web:jsBrowserTest` (the August 28 replay and browser history),
and `spotlessCheck` passed. Temporarily enabling the five Preservation Program intended-behavior
tests and Merger's intended immediate Tharsis action produced exactly their six documented failures;
their annotations were then restored. No test cases were removed in that November 17 batch:
the two incumbent-seat variants
in `TurmoilDelegatesTest` now have separate names. The final diff changes test support, scenarios,
and these project records, with no production-rule changes.

### Replay decisions selected on 2026-11-14

- Keep ordinary replay submissions literal and inventory-checkable: submit concrete quantities,
  not calculations such as `MC / CityTile<Anyone>`. Use the existing `EACH` shorthand to open a
  container rather than copying its formula. `:tfm-tests:checkReplayLiterals` rejects `/` in ordinary
  quoted replay strings and runs with replay and check tasks. Configuration/Pets raw strings and
  the separate TaskForm replay are excluded; this rule does not ban expression-focused engine tests.
- Expected outcomes are literal too. Do not read a tag/resource count from the running game and
  calculate the expected payout from it. Use literal net changes through the existing
  `expectChanges` when an observation spans calls, or literal absolute source checkpoints.
- Keep `.expect` for interesting consequences, especially triggered or automatic work. Omit
  predictable search-card yields and printed tags that merely restate the submitted card.
- Prefer `0 Pay<> FROM MC` for an admitted zero cash remainder, and
  `N ProjectCard<Hand FROM Selecting>` for bought cards. Keep explained `Ok` for optional
  attacks, unused alternative payment resources, and other genuinely different choices.
- Use literal ASCII maps through `TfmMapRenderer` to check tile positions and owners against
  original photographs or saved-game data. Prefer an endpoint map and, when useful and sourced,
  a checkpoint near the middle of the test. Do not replace missing source evidence with current
  engine output. A latest available photograph before the endpoint belongs at that earlier
  checkpoint. Remove scattered tile counts and per-area tile assertions; retain ocean totals as
  global-parameter checks. Existing maps do not display off-Mars cities or distinguish named
  special tiles; action submissions retain those identities and placements.
- Named versus positional assertion arguments are both acceptable. The intentionally different
  TaskForm replay remains outside this cleanup.
- Combine simultaneous gains for the same holder/type where existing total matching supports it.
  Preserve intervening choices, checkpoints, handoffs, and genuinely ordered continuations.
- Remove Summarizer and its score-category, lifetime-accounting, and internal-action-count checks.
  This supersedes the earlier proposal to move those checks into focused accounting tests.
- Remove the shared export reconstruction/component comparison. Retain export generation and the
  August 18 replay's specifically authored serialization checks.
- Completed replays assert functional outcomes such as scores and Victory, not queue emptiness,
  phase internals, or GameScope removal. Partial games keep their source-position checkpoints.
- Remove incidental completeOperation wrappers from successful replay sequences, World Government
  no-TR assertions, and duplicate handwritten production-offset checks. The November 17 decision
  also removes counterfactual negative probes and their now-unneeded restoration scopes.
- Call startGame visibly in ordinary replay test bodies. Use doTask for execution; a retained
  selectTask must be followed by the explicit execution of that same work. The underlying engine
  change that would defer System assignment until execution is separate, explicitly deferred work.
- Future work may investigate illegal-action tests using positions from recorded files without
  requiring the game itself to remain a Kotlin script. November 17 explicitly permits removing
  current counterfactual replay probes without replacing that coverage in this migration.
- Separate generation methods provide navigation only; no reorganization was requested.

Verification: all 29 active JVM replay tests passed with exports enabled, and all seven archived
replays passed through a temporary test filter without changing normal suite selection. Spotless
passed. Temporarily restoring October 9's `MC / CityTile<Anyone>` submission made
`checkReplayLiterals` fail at that source line; the literal `11 MC` was then restored. The November 11
formatting changes from main commit `23f1ea417` are included. No engine behavior changed.

Follow-up verification: all 36 JVM replays passed together with exports enabled through a temporary
test filter, leaving normal suite selection unchanged. The cleanup added 36 literal maps from
original saved games and photographs; 26 of the 35 ordinary replays now have map checks. The new
Synthetic Plasma Current maps exposed swapped neutral cities/greeneries in its setup; source save 0
establishes the corrected positions, and the sourced final score still passes. Remaining board
evidence gaps are recorded in `TODO.md`. No engine behavior or TaskForm replay code changed.

### Agreed direction: execution and task selection

The user deferred further EACH-shorthand matching changes and REPL payment cleanup on 2026-11-12.
Keep those implementations unchanged until the user reopens that work. Suitable Infrastructure now
consumes its bonus helper through `::` and queues the reward with `SOON`; clients submit only `2 MC`.
The Valley Trust and Merger/Manutech scenarios use explicit submissions with Player autoexecution
disabled throughout the rewarded operations; their setup still uses automatic execution.

- Eliminate `EAGER` for non-Admin players, subject to the limited setup convenience below. Admin
  continues to execute automatically. Minimize temporary policy changes and explicit automatic
  execution used to make a test proceed. The user also selected removing `SOLE_TASK` from normal
  gameplay tests as much as possible: optimize for reading every noteworthy event, not for ease
  of writing the test. Prefer `NONE` and explicit task submissions outside the setup convenience.
  Start with test callers. Players now default to `NONE`; a new smart policy remains unselected.
  The TaskForm replay remains outside the ordinary replay cleanup. Use explicit Player
  choices when testing engine semantics independently of policy.
- A superclass setup helper may perform corporation setup with automatic execution when the caller
  only needs the corporation to exist and is not testing its turn. Keep this behavior scoped to
  that helper and make its purpose clear. Tests of corporation choices or consequences need those
  steps visible. Today `chooseStartingCards` and `playStartingCorporations` in `TfmGameplayTest`
  use bounded `autoExecNow(EAGER)` calls; they do not permanently change the configured Player
  policy. November 17 explicitly includes starting-card setup and the default five-of-ten project
  purchase in this convenience, even when `EAGER` eliminates otherwise valid choices. A replacement
  helper or new policy-switching API has not been selected.
- High priority: remove gameplay tests' inspection of task collections to choose IDs wherever
  existing named task submissions suffice. When unavoidable, explain why at the use site. Also
  reduce assumptions that the only pending task must be the intended task: they reveal less about
  the scenario than naming the intended work.
- Do not reintroduce specialized selection conveniences such as `selectOnlyTask(x = 6)`. Bind X
  through the named amount being submitted. Do not replace each variation with another specialized
  helper. First establish how existing general task selection and narrowing express the required
  choice; report any actual API gap.
- Adopt combined-total task submissions more thoroughly. For example, a single production income
  can replace separate submissions for the TR and production contributions. Keep genuinely
  different choices, owners, and ordered continuations explicit; this is not permission to merge
  unrelated game events or silently choose among targets.

### Agreed direction: sources and corrections

- Fake or substitute cards are an accepted necessity where the real card is unsupported. Their
  mere presence is not an inconsistency to eliminate. Keep limitations visible and avoid treating
  the substitute as proof of the real card's complete behavior.
- Different games inevitably have different evidence. In the separate authentication project,
  audit replay assertions and choices against the available original logs, screenshots, recordings,
  and saves to find possible mistakes; do
  not try to make the evidence formats uniform. An assertion copied from an engine run is not
  independent source confirmation. For example, `SyntheticProtonFragmentTest` explicitly says its
  generation 7 values lack a screenshot and reflect the engine's current results. Missing evidence
  and unresolved historical discrepancies do not block this test-style migration.
- Inventory every way a replay reconciles the model with its source, then simplify those uses.
  Prefer explicit `exMachina` adjustments with comments explaining the reason: a table mistake,
  an unsupported part of a fake card, or another evidenced mismatch. Ordinary task execution is
  preferable when it already reproduces the sourced position without a correction.
- Avoid dropping tasks as much as possible. A retained drop needs a comment explaining why it is
  the most feasible treatment; naming the omitted effect alone is insufficient. Do not replace
  every drop mechanically with an adjustment before investigating what the task represents.
- Reproducing a live player's mistake is legitimate. In particular, the October 9 Project Eden
  correction accounts for oxygen/TR the table omitted; that fact alone is not a defect in the test.
- Do not rewrite replay history in the current autonomous pass. When a pair of `exMachina` calls
  could correctly be eliminated by rewriting the earlier play, add a `//` comment identifying the
  pair and explaining why. Retain both adjustments and the original chronology. The user may
  select such retcons later, after reconstruction is settled; that work is not currently authorized.

For solo attack effects, ordinary submissions against `SoloOpponent` can preserve the human
player's recorded position without dropping tasks. `MassiveGammaZoneTest` and
`BinaryVectorStreamTest` demonstrate this. Absence of a neutral production reduction from the
human player's source log does not establish an application mistake.

### Agreed follow-up: setup, completion, and assertions

- The entry-point comparison is documented in
  [Sandbox and gameplay fixture migration](#sandbox-and-gameplay-fixture-migration). Keep the
  distinction between a prepared standard opening, explicit corporation play, and an unstarted
  replay; no further setup consolidation is selected.
- Completed replays use scores and Victory as their completion checks, as selected on 2026-11-14.
  Partial replays may legitimately leave tasks pending and retain their source-position checks.
  Focused success scenarios must complete the action or phase they claim to prove, as selected on
  November 17; focused negative scenarios may stop at the expected failure.
- Keep scenario choices scripted rather than locating the current starting player from live state.
  The former `PreservationProgramTest.passGeneration` driver has been removed. EndgameRulesTest now
  uses corrected positions instead of its former first-available-area and buy-while-affordable
  game drivers. It retains full generation traversal where the solo countdown is the rule under
  test. Keep traversal and gameplay whose history is itself relevant; replace artificial histories
  used merely to prepare a position with explicit corrections.
- Improve consistency in checking consequences beyond "the task call succeeded": useful automatic
  gains, effects on another player, and quantities that should stay unchanged. Preserve source
  checkpoints and final outcomes; do not add assertions that merely repeat the submitted task.
- Different assertion receivers legitimately imply different observation scopes. That fact alone
  needs no cleanup. The user asked what `expectChanges` means, not for its removal or expansion.
- Remove replay Summarizer accounting outright, as selected on 2026-11-14. Preserve ordinary final
  scores, meaningful resource checks, and source checkpoints. Production attribution is unaffected.

`expectChanges("...") { ... }` is an existing assertion helper in `TfmGameplayTest`. It remembers
an event position, runs the block, and checks the requested **net changes across the entire
block**. For example, `PristarTest` observes the last player's pass together with subsequent
production work to check Kim's 25 MC and Preservation marker. A `.expect()` on the pass alone
would observe only changes retained before that call returned. The helper does not create a
transaction, roll back the block, change automatic execution, or require all tasks to finish.

### Sequential task submissions

Use `giveTask(recipient, choice)` to release the unique pending `NOW` task for that recipient
and immediately submit their choice. When several handoffs await that recipient, identify the
offered task explicitly with `giveTask(recipient, task, choice)`. Keep separate calls when a test
checks the intermediate state or the recipient only selects or narrows work.

`doTasks` follows repeated individually atomic `doTask` calls: a later failure retains successful
earlier arguments. See [the engine contract](ENGINE_CONTRACT.md#state-transactions-and-calls) and
`Agent.doTasks` for execution, results, recording, and restoration semantics.

Use an explicit operation block when a negative probe must restore its whole attempt before the
scenario continues. Let the exception escape that block before catching it. Catching a failed step
inside the operation instead retains the successful earlier steps. Prefer `continueOperation` when
the probe may legitimately leave work pending; use `completeOperation` when finishing is part of
what the probe checks.

### Archived replay verification

Leaving slow archived games outside the routine suite is an intentional runtime concession, not
by itself a problem to fix. At the user's request, on 2026-11-12 all seven archived JVM replay
classes were executed: `FirstPartialGameTest`, `Game20260619Test`, `HellasPromoGameTest`,
`HellasPromoWithoutPreludeTest`, `SoloGame20230611Test`, `SoloGame20230710Test`, and
`SoloGame20230721Test`. Result: **7 tests, 0 failures, 0 errors, 0 skipped**.

On November 18, all seven also passed both checked-in archive import and fresh export/re-import.
Three solo replays now explicitly take their existing expected final VP totals; Pharmacy Union's
linked flip choice was preserved without repeating its Admin-executed first step. The four partial
scenarios were later removed; the archived suite now contains the three solo replays. Run `./gradlew :tfm-tests:archivedReplayTest -PreplayExports` to check those remaining
scenarios. The regular suite selects the 27 active scenarios.

### Remaining audit observations

The following preserve the remainder of the ranked audit (original items 15–29), with dispositions
where the November 14 replay decisions address them. Unresolved items are retained for discussion, not
instructions to normalize everything. The order reflects how consequential the difference is,
not how many tests use each approach. Narrower decisions above do not imply acceptance of every
related change below.

| Original rank | Difference observed | Concrete examples or question to revisit |
| --- | --- | --- |
| 15 | Negative probes and restoration checks versus successful play only | November 17: remove counterfactual replay probes now; a separate approach to illegal-action coverage is deferred. |
| 16 | How known wrong behavior is represented | `PreservationProgramTest` and `MergerTest` pair disabled intended behavior with passing `BUG -` cases; other scenarios characterize unresolved behavior directly, or replay it with a correction. Review consistency with Known-defect tests above. |
| 17 | Recording and reconstruction coverage | Ordinary replays do not record unless exports are requested. August 18 also checks serialized content, notes, and a midpoint; routine browser replay coverage mainly uses the August 28 TaskForm game. |
| 18 | Frequency, placement, and breadth of checkpoints | `BinaryVectorStreamTest` checks generations against saves; `ThermalMatterWaveTest` concentrates more at the end; physical games follow photographs. Evidence naturally differs, but assertion placement can still be reviewed. |
| 19 | What final validation proves | November 14: use final scores and Victory; remove Summarizer score categories and phase/GameScope/pending-task completion checks. |
| 20 | Extra checks of internal flow and rendered output | November 14 removes Summarizer action-signal counting. Rendered-map checks remain a separate question. |
| 21 | Scenario preparation in setup hooks versus visible test bodies | Stina starts visibly. November 19 replaces Supercapacitors' extra preparation generation with explicit initial stocks followed by real production. Shared scenario setup remains acceptable when its purpose is clear. |
| 22 | Private helpers containing a whole scenario versus inline steps | Self-Replicating Robots now keeps the relevant play and assertions together. Private helpers remain appropriate for genuinely shared scenario steps; no blanket inlining rule was selected. |
| 23 | Organization of long play sequences | Generation methods provide navigation, not gameplay behavior; no reorganization selected. Agent receiver methods versus `with(player)` remain a separate style question. |
| 24 | How broadly assertion helpers check state | November 19 explicitly accepts the difference: `assertCounts` names selected facts; `assertTags` supplies zero defaults for omitted categories. Resource/production helpers cover six amounts and production offsets. |
| 25 | Syntax used to express equivalent tasks | November 19 selects generated card constants and `doTasks` for consecutive steps of the same action. Never group distinct actions. November 14's explicit execution and literal-quantity rules still apply. |
| 26 | Naming players | Source names, colors, and fixture aliases remain intentional. November 19 removes redundant names where the receiving Agent supplies the actor; preserve explicit other-player targets and disambiguating ownership. |
| 27 | Assertion notation | `assertEquals`, Kotest matchers, `has`, and count checks; positional versus named tag arguments. Rank below differences in what is actually asserted. |
| 28 | Class/file/test organization | Nested versus top-level test classes, directory placement, method names, and visibility. Suite selection parses source declarations; which moves would change what runs? |
| 29 | Comments and presentation | Quoted source text, timestamps, database-save references, generation headings, and commented-out assertions vary. Existing replay `Ok` comments already follow a shared convention. |

## Game replay tests

`Game20230521Test` and `Wsbg2025Test` deliberately exercise the raw Agent task API.
Players submit their tasks explicitly. Admin handles fixed payment bookkeeping and expands
`ScoreEventVps`, but `VictoryPoint` is a Player task. Submit each Player's source-backed net VP
amount after final greenery. The shared matcher executes scoring gains before deductions, includes
zero-valued scoring work, and leaves winner determination waiting until scoring finishes.
Follow the game's flow through the last passes, production, final greenery, and scoring; do not
directly supply turns or phase transitions. Omit copied game-log
comments that only restate the submitted tasks.
Use fully specified instructions and `PROD[...]` in these `doTasks` calls, without AMAP dots or
unevaluated metrics. Combine the three production money amounts into their net income when those
tasks are already pending. Keep amounts from unrelated reasons separate, for example Investment
Loan's 10 MC and Media Group's 3 MC. Agent translates the production total into individual engine
calls, with gains before removals and each task's effects preserved; engine has no combined-total
command. See `Agent.doTask` for exact matching rules. Explicit `doTasks(...)` calls submit each
argument separately and retain successful earlier steps when a later step fails. See
[Sequential task submissions](#sequential-task-submissions).
Explicit-task replays extend `FullGameTest` or `CardTrackingFullGameTest`, both based on
`TfmGameplayTest` with plain Agents and Player autoexecution disabled. Call the fixture's
`startGame()` to begin setup. Have each player take their starting TR before either player handles
starting cards. Group corporations and Preludes in `with(player) { ... }`; use
`player.turn { ... }` for action-phase turns. Solo and last-player blocks may contain any number
of actions. Keep research, production, final greenery, and ordinary generation passes outside turn blocks.
Keep each action in its own task call, with the recorded dialogue beside that action. A card's
nested action (such as Viron repeating another card or Project Inspection invoking a card) remains
part of that card's action. Keep ordinary passes separate from the preceding action.
Use the existing `playCardTasks`, `cardActionTasks`, and `standardProjectTasks` abbreviations where
they exactly represent the submitted commands; do not add an extra turn action for a nested choice.
Select only the fake substitute cards used by the replay, rather than the entire `fakes:cards` pool.
Keep political changes in nearby result expectations, including the displaced leader becoming a
follower. Avoid repeating those expectations as later absolute assertions, while retaining
source-backed resource checkpoints and final board/scoring checks.

The turn wrapper checks that the actor holds the turn and that each action finishes, including
other players' responses. It declines the optional second action with `Ok` only when the actor still
holds the turn and opponents remain. It creates no offers and does not roll back previously submitted
work on failure.
Keep ordinary `doTask("Pass")` submissions outside the block. Red Appeasement's required Pass
belongs inside because it completes that card action; the wrapper then needs no extra handoff.
Keep each original task submission and its order, including cross-player responses.
Use `giveTask` for immediate `NOW` handoffs within those turns.
Use the shared card-name constants, give every `Ok` a short inline comment to its right naming
the task it dismisses (for example, `// no steel paid`). Use `.expect()` for consequences not
already evident from the preceding task submissions, including automatic resource changes and
checks that a quantity stayed unchanged. Do not repeat explicit gains/removals or their arithmetic
totals. Retain state checkpoints and final score/winner assertions. Purchase transfers into `Hand`
are explicit tasks from `Selecting`. Keep the replay's task calls instead of introducing
action or payment helpers.

`OtbGame20260828Test` exercises the separate TaskForm style on `FullGameTest(EAGER)`.
Players submit their choices through `TaskForm.choose`, `narrow`, and `commit`; the policy
executes concrete follow-up work. Its local form wrapper asserts offered choices and completed
drafts, without gameplay actions, payment helpers, or a raw `doTask` fallback. Its turn wrapper
only supplies the same unused-second-action handoff as in explicit-task replays.

Replay tests assert victory points only after the game's actual final scoring. Mid-game
checkpoints verify the live state without simulating production or scoring; partial replays
do not assert hypothetical victory points.

Replay file export is disabled by default, including in browser tests.
Complete replays check scores and Victory. Intentionally partial games retain their source-position
checks and may leave work pending. The shared queue-count check has been removed.
`ReplayExportExtension` imports each successful JVM replay's checked-in `.game` fixture through
its catalog and compares final components and pending tasks. Missing fixtures fail the test.
With `-PreplayExports`, it additionally exports and imports a fresh uncommented archive. Authored
fixtures preserve source comments; generated output never replaces them automatically.
It reads the engine's existing event history; no task-log switch or second input stream is needed.
The importer uses explicit player requests with autoexecution disabled. Replays must follow the
normal game flow and use ordinary task submissions instead of injecting operations. Partial games
may stop with decisions pending; unfinished selections need archive coverage before claiming that
all such positions are supported. The local November 11 test additionally checks every generation
and stable repeated export. See [EXPORT.md](EXPORT.md) for current migration status.

Whole-game tests are high-value integration coverage. When translating a supplied game log:

- Card-tracked replays extend
  [CardTrackingFullGameTest](../../test/common/dev/martianzoo/tfm/tests/replays/CardTrackingFullGameTest.kt),
  which starts production [CardTracking](../../src/common/dev/martianzoo/tfm/engine/CardTracking.kt)
  before gameplay. Use `cardArrivalOrder` and `cardDepartureOrder` for automatically dealt setup
  cards. Name project hand movements beside their actions with `draw` and `discard` before the
  relevant Agent operation completes, including purchased and sold cards.
  `assertCardTrackingComplete` checks unused supplied names. `cardArrivalOrder` describes hand
  entry, not offers or deck order. When a replay has names for every `Selecting` movement,
  `selectingArrivalOrder` and `selectingDepartureOrder` enable game-wide Selecting tracking.
  The four database replays and `StinaGameTest` exercise this option. The database replays use sourced setup
  and Research offers; `ErraticCarbonCurrentTest` labels two invented Invention Contest rejects.
  `StinaGameTest` also labels its invented offer assignments. If even a single modeled Selecting
  movement lacks identities, leave the option off or label a few invented assignments explicitly.
  Cards may enter a hand again after leaving play.
  Names must be concrete catalog `CardFront` classes with the matching back. When a source omits
  rejected setup-card faces, use a small number of explicitly labeled invented card assignments;
  `StinaGameTest` does this for three cards. Do not fabricate entire `Selecting` offers.
  Research archives that used drafting may assign each recovered post-draft
  four-card set as ordinary hand arrivals when the tested engine does not support drafting. Buy only
  the evidenced count and name only retained cards. The four database-backed conversions and
  `StinaGameTest` use this mode. When a source gives only a discard count, select names explicitly
  and label that selection as test inference.
- Before editing a dated whole-game test, explicitly inspect its matching
  `_local/replays/GameYYYYMMDD/` directory and read any `implementation-plan.md` there before acting. The
  repository's `_local` path may be a symlink, which `rg --files` does not traverse, so a general
  file search is not evidence that the test's local sources are absent. A plan supplies procedure,
  not game facts: establish all setup, chronology, values, and reconciliations from original sources.
- For a herokuapp archive, read `docs/agents/HEROKUAPP_GAME_LOGS.md` before implementation. Its API,
  payment-reconstruction, screenshot, and endgame rules supplement this section.
- For a recorded physical game, read [`_local/OTB_GAME_RECORDS.md`](../../_local/OTB_GAME_RECORDS.md)
  before implementation. Its
  source-preservation, mixed-evidence, photograph, reconciliation, and endgame rules supplement this
  section.
- Do not inspect an existing dated test, Git history, or previous agent summary to learn what
  happened when the task calls for an independent reconstruction. Repository code and other tests
  may teach the Solarnet API only.
- In multiplayer action phases, group each player's turn in `with(player) { ... }` and submit
  `Ok` when the player declines their optional second action. Preserve sourced fast-mode choices,
  including explicit passes and declined second actions. Once every other player has passed, the
  remaining player's actions through `Pass` may share a scope block; the game keeps offering actions.
- When an Event card's sibling tasks require explicit replay ordering, record only the gameplay
  consequences. The card's `LATER` dispatch defers its flip, and the owner performs the flip once
  no ordinary work remains. The engine performs the dispatch itself; the replay records the explicit
  `PlayedEvent<Class<X>> FROM X` step.
- Use the same turn block for a player's two Prelude plays. This also supports Prelude effects that
  immediately play another Prelude or project inside one of those plays.
- Keep the supplied machine log as a separate source artifact rather than copying its lines into the
  test solely for traceability. Periodically audit the test against that artifact for
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
- Replay payments should retain sourced allocations and the comments explaining inferred choices.
  For a recorded physical game, first search the transcript and player-board logs for an explicit
  payment composition; prefer that source evidence to inference from a later balance.
  Preserve resource checkpoints, card identities, corrections, and final outcomes.
- The herokuapp and Solarnet map coordinates differ: herokuapp counts from the first tile whereas
  Solarnet uses slant-columns.
- Prefer supplied logs, images, and local map data over investigating another application's
  implementation. Work around unsupported engine behavior narrowly and record real follow-ups in
  `TODO.md`.

### State reconciliation

See the [gameplay consistency decisions](#agreed-direction-sources-and-corrections) for the current
preference for explained corrections over task drops. For now, only comment on pairs of corrections
that could be retconned later; preserve their execution and the original history.

- Express a sourced per-player setup rule, such as a starting handicap, on that replay's concrete
  Player Class with a `SetupPhase` effect supplied through `playerClassPets`. It is game
  setup, not a state reconciliation.
- Use `Agent.exMachina` for an
  evidence-backed player error that requires a state adjustment. Place it as late in the
  timeline as the sourced assertions allow, with a comment saying which later step requires it. Add
  source-backed `.expect()` assertions for the mistake-prone types to preceding actions wherever
  practical so the remaining unexplained gap is bounded as narrowly as possible.
  Keep unexplained state reconciliations as standalone timeline statements.
  Never place an ex machina adjustment inside an unrelated action body to evade a selected-task or
  Agent-call completion restriction; use an explicit test mechanism or fix the helper/API instead.
  Nest a missing consequence only when the enclosing action caused it.

## Multiplatform tests

- Shared `kotlin.test` test classes and methods may not be private; even though JVM test runs may
  work with them, JS test runs may not.
- Mocha owns the per-test timeout; Karma owns browser activity, disconnect, and reconnect
  timeouts. A long synchronous test can block the browser event loop long enough to hit either.
- A Node test target is not a substitute for the browser suite, which verifies browser compilation
  and integration.
