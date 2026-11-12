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
  scenarios reached through play, with the game's own rules offering turns and beginning phases.
- [`FullGameTest.kt`](../../test/common/dev/martianzoo/tfm/tests/replays/FullGameTest.kt) —
  replay setup, assertions, endpoint and export/import checks on `TfmGameplayTest`.
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
- `./gradlew :tfm-tests:replayTest` runs the curated replay suite without input recording or export.
  `:tfm-tests:jvmTest` excludes replays and can run filtered tests independently. Use
  `:tfm-tests:test` for both JVM suites; `allTests`, `check`, and `build` also include replays.
  The explicit `activeReplayClasses` list in `modules/tfm-tests/build.gradle.kts` selects imports
  from 2026 plus the retained `Game20230521Test`. All archived replays still compile; their
  `archivedReplayTest` task registration is commented out and unavailable. The optional, also
  commented-out `allBrowserTests` task still runs every portable replay when restored.
  Add `-PreplayExports` to record inputs, write full `.json` and short `.short.json` game files
  per successful `FullGameTest` subclass, and verify reconstruction in a fresh game.
  Files go under that module's `generated/replay-event-logs` build directory. Export runs remove
  JSON files for archived games. Ordinary runs
  leave existing exports untouched. Direct JUnit runs need `-Dsolarnet.replayExports=true` and
  `-Djunit.jupiter.extensions.autodetection.enabled=true`.
  See [When to regenerate replay exports](EXPORT.md#when-to-regenerate-replay-exports) before
  reusing or distributing generated recordings after rules, runtime, or replay changes.
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
test. It hides which game decision the test makes. Submit the named choice, or use `selectOnlyTask()`
when selecting the sole pending task is itself the intended action.

> **Default assertion style:** Almost every successful gameplay action that returns a `TaskResult`
> should chain `.expect(...)` directly to that action in gameplay tests. Sandbox tests instead
> submit concrete expected work explicitly and add expectations only for additional consequences.
> Prefer relative expectations over later absolute resource or component counts: the chained expectation proves the net change came from the action under test,
> while an absolute count can pass because of setup or an earlier action. Use an absolute assertion
> only when the absolute state is itself the contract, no successful result exists to inspect, or a
> source explicitly states an absolute value.

Terraforming Mars integration tests live under `dev.martianzoo.tfm.tests`: `cards` contains
component-focused behavior, `rules` contains game-wide and cross-component behavior, and `replays`
contains whole-game chronologies. Shared integrated-test support lives in the parent
package. Test placement follows purpose: a test of engine behavior belongs with engine even when it
uses Terraforming Mars declarations to construct its scenario, while a test of Terraforming Mars
rules or content belongs in the Terraforming Mars suites. Test-only dependencies may cross that
direction; production dependencies may not. Small generic declarations are preferable when they
make a test clearer, but replacing domain examples is independent cleanup rather than a prerequisite
for correct ownership.

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
   `CoreRulesTest` documents game-wide rules in this same style.
5. **Whole-game tests.** Long scenarios that show the game flow and many rules operating together,
   especially when reconstructed from independent game records. With `-PreplayExports`, every
   successful JVM replay test also emits an exact recording and verifies reconstruction.
6. **Canon admissibility tests.** A compact gate confirming that the complete authority loads and
   that representative supported configurations compose into usable projected class tables and
   worlds. This is not a demand to restate the contents of every card or bundle in assertions.
7. **Known-defect scenarios.** Focused passing characterizations of important behavior known to be
   wrong, explicitly identified as defects. Card tests use the paired pattern described
   under [Known-defect tests](#known-defect-tests); other suites retain `BugsTest`.
8. **Script-command contract tests.** Terraforming-independent checks of each command's public
   contract. These are useful interface coverage even though they are not a development priority.
9. **Cross-runtime browser coverage.** Browser-specific tests cover browser APIs, one representative
   replay checks the shared engine on JavaScript, and production JavaScript compilation and resource
   assembly cover the Web REPL and Almanac.

This list does not itself decide which current tests should be retained. Test-deletion proposals
are a separate review.

Give each scenario variant its own named test, using a private helper for shared steps. Do not put
variants in a `for` loop inside one test: the failing test name should identify the case. Exercise
the behavior under test through gameplay. For sandbox tests, establish incidental
preconditions through the standardized fixture and corrections by default. Use authentic
gameplay instead when the way a precondition is reached can affect, or provide useful evidence for,
the behavior under test.

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
Synthetic engine checks use the existing `canonicalPremise` and `setUpGame` functions, either
in standalone tests or by assigning a synthetic world to the sandbox's `game`. For example,
`VerminTest.Attribution` supplies a synthetic listener through the sandbox. `ResearchWildTagTest`
retains its synthetic assigned-tag scenarios using plain Agents; `WildTagTest` and
`WildTagContinuationTest` cover allocation and cleanup through prepared sandbox actions.
When a custom instruction reads authored card metadata from the catalog, compose the synthetic
card into a fixture `TfmCatalog`; premise-only declarations do not populate that metadata.
Sandbox and gameplay scenarios use explicit Agent task submissions. The fixture-specific
operation and payment conventions are described below.

`CoreRulesTest` uses the same player-level style to document rules that belong to the game rather
than any individual card. Its scenarios should reproduce only the important preconditions observed
in whole games and use explicit sandbox tasks with expectations for additional consequences.
Full-game tests override a `config` property with a `GameConfig`, conventionally built from an
indented multiline string followed by player-name varargs. Catalog-backed premise resolution adds
`TerraformingMars` and, when no other map is named, `TharsisMap`; the parser already trims each
entry, so these literals do not need `trimIndent()`. Solo tests conventionally use `Me` as the
canonical Player Class Name and use `Player.PLAYER1` in Kotlin.

### Command abbreviations

The user permits fixed command sequences in otherwise explicit `doTasks` tests, including selecting
a standard action and then the particular card or project. `standardActionTasks.kt` returns command
arrays determined only by its arguments. The caller submits them through ordinary `doTasks`.
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
  later phases without their queued start-of-phase rules. A scenario that needs a phase's real effects,
  such as production, a solar phase, or research, belongs in `TfmGameplayTest`.
- `TfmGameplayTest` starts the game and lets its own rules offer every turn and begin every phase.
  Reach the scenario through real play, including passing through production to obtain resources.
  Do not inject resources, replace phases, or invoke internal rule instructions. A setup
  convenience may play a documented ordered selection of corporations or Preludes in turn order;
  convenience must not bypass their gameplay. The implemented default already plays the starting
  corporations; Prelude choices remain explicit.

Gameplay tests submit the turns the game offers through plain `Agent`s. Use `doTask`/`doTasks`
for explicit choices, or `completeOperation { ... }` when the whole operation must finish before
returning. Do not use `inTurn`: it can create an out-of-turn offer when the actor has no pending
work. Decline an offered second action explicitly with `selectTask("PassBaton")`; do not insert that
choice after both actions, after a Prelude, or when all opponents have already passed.

`TfmGameplayTest` exercises authentic game flow. The fixture owns its `World` and `Agents`
directly and exposes Admin and all players as plain Agents.
Scenario payments state concrete amounts, including research purchases; do not use `/ Owed` or
payment helpers. Shared corporation setup alone reads the bill for its five starting projects,
so corporations such as TerraLabs use their actual purchase price.

State the steps explicitly when testing their order. Finish or decline alternative payment
offers before the exact remainder, and explain each `Ok`. Fixed command abbreviations may
shorten repeated invocation steps; payments and gameplay outcomes remain explicit. Loops remain
appropriate for seat-order traversal and long generation/endgame progressions.

Synthetic sandbox checks may instead assign `game` directly. This creates fresh `Agents` with
their default policies, without preparing a position or binding `players`, `kim`, `stan`, or `rob`;
use `agents[player]` and `admin`. These subclasses also run under `:tfm-tests:sandboxTest`,
so that task measures both prepared-position scenarios and synthetic sandbox checks.

Synthetic engine checks that only need setup use `setUpGame`, which starts the game and drops setup's
pending handoff (`startSetupOnly`), so the game stays in `SetupPhase`. `startCorporationPhaseOutOfTurn`
moves such a game into the corporation phase through that phase's own start, then drops the turn
order it begins.

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
Use `selectTask` and `selectOnlyTask` only for a choice still needing narrowing, structural expansion, or
handoff, never to carry out concrete player work. Every retained selection must have a local
comment explaining why it is necessary. Prefer a short named selection, such as
`selectTask("PlaceGreeneryTile")`, to an anonymous selection. String selection also narrows the
task; it is not a name-only lookup. Keep `selectOnlyTask` when a short instruction cannot identify
an `EACH` group or would discard part of a linked choice, as in Flooding's placement and attack.
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
that a correction can express. Gameplay scenarios must still reach their conditions through
play. Do not infer a configuration requirement solely from the old test's configuration.

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

Every state change in a migrated sandbox test must come from real player-facing gameplay or from
its explicit fixture methods. Do not use `runOperation`, `beginOperation`, arbitrary direct
phase changes, or other lower-level shortcuts in either fixture's subclasses. The sandbox provides
`advanceUntilActionPhase()` to leave explicit corporation setup or skip unplayed Preludes when their play
is irrelevant; finish pending choices first. This is a sandbox shortcut, so other unplayed starting
cards need not be handled when they are irrelevant to the scenario. Gameplay leaves those choices
to the test and advances through the game's normal flow.

In sandbox tests, conditions beyond that initial state should normally be visible corrections:

- Use `setToExMachina(targetCount, type)` for a desired absolute count. Admin may use it for
  System markers such as colony tracks. It calculates the gain or
  loss from the current count, so the scenario states its intended condition rather than assuming
  the fixture's prior value. Production types such as `PROD[Heat]` work the same way, including negative printed production.
- Use `exMachina(adjustment)` for a naturally relative change or a known absent-to-present fact.
  Prefer a concrete Type such as `NormalCityTile` when an abstract Type cannot be created directly.
- If initial play of the subject card is not being tested, install that card directly. Do not first
  satisfy its play requirement, pay its price, or reproduce its direct effect. Correct any
  independent state that the behavior under test actually needs.
- If initial play, payment, requirements, direct effects, or entry-trigger interactions are
  the subject, play the card normally and establish whatever legal setup that action requires.
- To test behavior at a completed global parameter, correct the track to its penultimate step and
  use an ordinary standard project for the final step when available. This preserves the real
  completion lifecycle, including `GpComplete`, instead of asking a correction to stand in for it.
- `advanceUntilActionPhase()` expires generation-scoped state without production or research.
  A scenario that needs real phase effects or their ordering belongs in gameplay; do not broaden
  the sandbox shortcut into another game driver.

Corrections intentionally suppress ordinary queued effects while retaining the structural work
documented in [EX_MACHINA.md](EX_MACHINA.md). Therefore, never treat the correction itself as proof
that gameplay works. Assertions should begin with the action or interaction that is the scenario's
subject, and should omit setup deltas.

Authentic setup through gameplay remains valid and is sometimes preferable. Use it deliberately
when the path into the state matters: trigger history, attribution, task ordering, payment,
requirements, card-entry effects, global-parameter completion, game flow, or interactions among
setup actions. It is also appropriate when a broader integration scenario is valuable in its own
right. Correction is the default for irrelevant preconditions because it keeps focused card
tests short and makes their real subject obvious; it is not a ban on authentic gameplay.

Review each retained scenario before selecting its fixture. A coherent card suite can use gameplay
throughout even when some methods could use sandbox; `PreservationProgramTest` follows this style.
When most methods suit sandbox and only a few need the game flow, a normal nested class may extend
`TfmGameplayTest` inside the sandbox class. It owns its own fixture and does not share the outer
instance; do not use Kotlin `inner` or JVM-only `@Nested`. See
`TfmSandboxTestTest.TfmGameplayTestTest` for executable discovery coverage. When filtering by the
outer class name, include a trailing wildcard to include nested classes as well. Synthetic
declarations still require separate evaluation.
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

**Status: decisions and follow-up work recorded on 2026-11-12.** This section records the user's
responses to the gameplay consistency audit. Agreed direction, current implementation, proposals,
and observations not yet discussed are distinguished below. The work list is in
[`TODO.md`](../../TODO.md#gameplay-consistency). This is not a claim that the migration is finished.

The intended families remain sandbox, gameplay, gameplay with card tracking, and the separate
TaskForm style. Differences required by card tracking are not cleanup targets. `TfmGameplay.kt`
has been deleted; keep shrinking the remaining gameplay API rather than recreating its helpers.
The user does not require preservation of unused-action or underpayment/overpayment checks merely
because old replay helpers performed them. Do not restore the discarded `tfm_sample` REPL command.
These permissions do not imply removing source resource checkpoints or changing recorded payments.

### Agreed direction: execution and task selection

- Eliminate `EAGER` for non-Admin players, subject to the limited setup convenience below. Admin
  continues to execute automatically. Minimize temporary policy changes and explicit automatic
  execution used to make a test proceed. The user also selected removing `SOLE_TASK` from normal
  gameplay tests as much as possible: optimize for reading every noteworthy event, not for ease
  of writing the test. Prefer `NONE` and explicit task submissions outside the setup convenience.
  Start with test callers; a replacement global Agent default or new smart policy has not been
  selected. Preserve the TaskForm replay as a test of forms when removing its Player `EAGER`
  dependency. Use explicit Player choices when testing engine semantics independently of policy.
- A superclass setup helper may perform corporation setup with automatic execution when the caller
  only needs the corporation to exist and is not testing its turn. Keep this behavior scoped to
  that helper and make its purpose clear. Tests of corporation choices or consequences need those
  steps visible. Today `chooseStartingCards` and `playStartingCorporations` in `TfmGameplayTest`
  use bounded `autoExecNow(EAGER)` calls; they do not permanently change the configured Player
  policy. Starting-card setup also uses automatic execution today; whether it belongs in the
  same permitted convenience remains to be clarified. Do not treat its current implementation as
  approval of that scope. A replacement helper or new policy-switching API has not been selected.
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
- Different games inevitably have different evidence. Audit replay assertions and choices against
  the available original logs, screenshots, recordings, and saves to find possible mistakes; do
  not try to make the evidence formats uniform. An assertion copied from an engine run is not
  independent source confirmation. For example, `SyntheticProtonFragmentTest` explicitly says its
  generation 7 values lack a screenshot and reflect the engine's current results.
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

- Investigate what "start a game" hides. Compare `newTestGame`,
  `newTestGameBeforeCorporationPlay`, replay `startGame`, and direct premise initialization: which
  setup, starting cards, corporation turns, and first choices have already happened at return?
  Document the useful distinctions before choosing what to consolidate.
- Establish consistent completion checks appropriate to a scenario's stopping point. Audit direct
  gameplay tests, complete replays, and intentionally partial replays, including which execution
  routes install `ReplayExportExtension`. A partial replay may legitimately have pending work;
  checking only a count does not explain which work was expected. No replacement endpoint API
  has been selected.
- Inventory scripted choices versus choices computed from current state. Concrete examples are
  locating the current starting player (`PreservationProgramTest.passGeneration`), selecting the
  first available water area (`WorldGovernmentTerraformingTest.finishGeneration`), and buying a
  standard project repeatedly while MC permits (`EndgameRulesTest`). Compare these with a replay
  that names the recorded player, location, and purchase count. Some loops only traverse players in the game's
  order; others let the scenario adapt when the engine changes. Decide which are useful rather
  than imposing a blanket ban on loops or queries.
- Improve consistency in checking consequences beyond "the task call succeeded": useful automatic
  gains, effects on another player, and quantities that should stay unchanged. Preserve source
  checkpoints and final outcomes; do not add assertions that merely repeat the submitted task.
- Different assertion receivers legitimately imply different observation scopes. That fact alone
  needs no cleanup. The user asked what `expectChanges` means, not for its removal or expansion.
- Reduce detailed lifetime/cause accounting in ordinary replays. The user's suggested destination
  is a few focused tests of the accounting mechanism using a couple of replay games as inputs.
  Choose representative coverage before removing duplicated assertions. Preserve ordinary final
  scores, meaningful resource checks, and source checkpoints; removing detailed cause accounting
  does not authorize deleting those outcomes or the accounting implementation.

`expectChanges("...") { ... }` is an existing assertion helper in `TfmGameplayTest`. It remembers
an event position, runs the block, and checks the requested **net changes across the entire
block**. For example, `PristarTest` observes the last player's pass together with subsequent
production work to check Kim's 25 MC and Preservation marker. A `.expect()` on the pass alone
would observe only changes retained before that call returned. The helper does not create a
transaction, roll back the block, change automatic execution, or require all tasks to finish.
The current Pristar block itself calls `autoExecNow(EAGER)`; removing that use is separate from
what the assertion helper observes.

### Sequential task submissions

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

A temporary Gradle init script used the replay classpath and enabled the JUnit extension; all
seven ran `assertPendingTasksAtEnd`. Normal suite selection and repository build files were
unchanged. Input exports/reconstruction were disabled. This verifies those archived scenarios
on the JVM, not the browser suite or export round trips.

### Remaining audit observations — not yet discussed with the user

The following are the remainder of the ranked audit (original items 15–29). **The user has not
reviewed these observations or selected a response.** They are retained for discussion, not
instructions to normalize everything. The order reflects how consequential the difference is,
not how many tests use each approach. Narrower decisions above do not imply acceptance of every
related change below.

| Original rank | Difference observed | Concrete examples or question to revisit |
| --- | --- | --- |
| 15 | Negative probes and restoration checks versus successful play only | `SyntheticProtonFragmentTest` tries invalid Robinson Industries choices; `ActionScopeTest` checks restoration. Other replays submit only successful choices. Decide where these checks belong. |
| 16 | How known wrong behavior is represented | `PreservationProgramTest` and `MergerTest` pair disabled intended behavior with passing `BUG -` cases; other scenarios characterize unresolved behavior directly, or replay it with a correction. Review consistency with Known-defect tests above. |
| 17 | Recording and reconstruction coverage | Ordinary replays do not record unless exports are requested. August 18 also checks serialized content, notes, and a midpoint; routine browser replay coverage mainly uses the August 28 TaskForm game. |
| 18 | Frequency, placement, and breadth of checkpoints | `BinaryVectorStreamTest` checks generations against saves; `ThermalMatterWaveTest` concentrates more at the end; physical games follow photographs. Evidence naturally differs, but assertion placement can still be reviewed. |
| 19 | What final validation proves | Some tests check totals and winners, some score categories (`DistantPressureMassTest`), and some phase/GameScope state. This is distinct from whether tasks remain pending. |
| 20 | Extra checks of internal flow and rendered output | `SoloGame20230721Test` asserts 99 counted action signals; `ThermalMatterWaveTest` notes those signals do not measure the source's 169 actions. Some replays check a rendered map. Decide which checks add useful evidence. |
| 21 | Scenario preparation in setup hooks versus visible test bodies | `SupercapacitorsTest`'s setup advances several generations; `PristarTest` creates its game in `@BeforeTest`; `StinaGameTest` starts through shared setup. The reader sees different amounts of the scenario locally. |
| 22 | Private helpers containing a whole scenario versus inline steps | `SelfReplicatingRobotsTest`'s nested gameplay tests share a helper returning three counts; other scenarios keep actions and assertions together. Review what deserves a shared helper. |
| 23 | Organization of long play sequences | Separate generation methods versus a long test body; Agent receiver methods versus `with(player)`; scope blocks that mean a turn, an action, or a continuation. |
| 24 | How broadly assertion helpers check state | `assertCounts` names selected facts; `assertTags` supplies zero defaults for omitted categories; resource/production helpers cover six amounts and production offsets. Similar-looking checkpoints can check different amounts of state. |
| 25 | Syntax used to express equivalent tasks | Constants versus literal card names, task arrays versus expanded sequences, explicit versus inferred owners, movement spellings, and `selectTask("PassBaton")` versus `doTask("PassBaton")`. Which differences are only spelling, and which depend on execution policy? |
| 26 | Naming players | Numbered players, fixture aliases such as Kim, colors, and source names; a color can map to `Player1`. Some calls redundantly name the receiver. |
| 27 | Assertion notation | `assertEquals`, Kotest matchers, `has`, and count checks; positional versus named tag arguments. Rank below differences in what is actually asserted. |
| 28 | Class/file/test organization | Nested versus top-level test classes, directory placement, method names, and visibility. Suite selection parses source declarations; which moves would change what runs? |
| 29 | Comments and presentation | Quoted source text, timestamps, database-save references, generation headings, and commented-out assertions vary. Existing replay `Ok` comments already follow a shared convention. |

## Game replay tests

`Game20230521Test` and `Wsbg2025Test` deliberately exercise the raw Agent task API.
Players submit their tasks explicitly. Admin handles fixed payment bookkeeping and final scoring.
`VictoryPoint` and `ScoreEventVps` are `System`;
players do not submit scoring tasks. A pending negative score can wait for a positive score to supply the points.
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
starting cards. Group each corporation, Prelude, and action-phase turn in `with(player) { ... }`.
A solo replay may group a whole generation in a single `with(player)` block.
When a player declines their second action, submit `doTask("PassBaton")` explicitly at the end of
that turn. A scope block itself performs no gameplay. Keep the task submissions inside each section
as raw `doTask`/`doTasks` calls.
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
drafts, without gameplay actions, payment helpers, or a raw `doTask` fallback.

Replay tests assert victory points only after the game's actual final scoring. Mid-game
checkpoints verify the live state without simulating production or scoring; partial replays
do not assert hypothetical victory points.

Replay input recording and export are disabled by default, including in browser tests.
Every successful JVM replay checks that no tasks remain at its endpoint. A replay intentionally
stopping with open work calls `expectPendingTasksAtEnd(count)` at the end of its test, naming the
offered turn or phase in a short comment. The count includes deferred Admin tasks. For a final
action that should finish before the saved position, `completeOperation { ... }` checks its effects
before the replay accepts the next turn or phase offer.
With `-PreplayExports`, `ReplayExportExtension` enables recording before test setup, so
`FullGameTest` starts `Agents.taskLog` before gameplay. After each successful JVM
replay, the extension writes full and short game files and reexecutes the full file in a
fresh game using its checked premise and recorded inputs. The check compares the component changes with their Actors and causal ancestry,
the final components, and pending tasks. Incidental ids, event ordinals, and the order of independent
changes may differ.
A round-trip failure fails the test. Replays must follow the game's normal flow and use
ordinary task submissions instead of injecting operations. Partial games may stop with decisions
pending.
The hook reports each successful round-trip in test output.

Structured inputs retain submitted syntax and record policy-executed Player work. Admin work is
performed automatically during reconstruction rather than serialized as input, so
import does not reproduce the source's policies. Batch and single-task operations retain their
distinct semantics. `CHOOSE` preserves selections and committed form instructions,
including partial choices that leave work pending. Later full form instructions replace earlier
form narrowings when they only changed its instruction; steps that selected the task remain.
Uncommitted form edits are not recorded; round-trips compare resulting state rather than requiring
identical intermediate events.
`DROP` and `EX_MACHINA` retain interventions. The file includes the premise recipe and the game's
start. See `TaskLog` and `GameFileImporter` KDoc for supported scope.

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
  payment-reconstruction, screenshot, counterfactual, and endgame rules supplement this section.
- For a recorded physical game, read [`_local/OTB_GAME_RECORDS.md`](../../_local/OTB_GAME_RECORDS.md)
  before implementation. Its
  source-preservation, mixed-evidence, photograph, reconciliation, and endgame rules supplement this
  section.
- Do not inspect an existing dated test, Git history, or previous agent summary to learn what
  happened when the task calls for an independent reconstruction. Repository code and other tests
  may teach the Solarnet API only.
- In multiplayer action phases, group each player's turn in `with(player) { ... }` and submit
  `PassBaton` when the player declines their second action. Preserve sourced fast-mode choices,
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
