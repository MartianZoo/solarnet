# Testing and verification

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

(This is a by-codex-for-codex doc.)

## Routine verification

The wrapper supports and directly uses the JDK selected by `JAVA_HOME` from 17 through 26. JVM code
targets the Java 17 bytecode and API surface, while Kotlin source and standard-library APIs target
Kotlin 2.2. Contributors do not need another JDK installed.

- `./gradlew build` is the normal repository-wide check. It runs every JVM test plus one
  representative multi-generation engine game in Chrome as the browser smoke suite.
- `./gradlew test` runs every repository JVM test suite, including the multiplatform modules whose
  JVM test tasks are named `jvmTest`.
- `./gradlew :engine:jsBrowserSmokeTest` runs only the representative browser smoke scenario.
- `./gradlew jsBrowserTest` runs every module's full browser suite.
  `./gradlew build -PincludeBrowserTests=true` includes those suites in the normal repository-wide
  check.
- `./gradlew :engine:allTestsIncludingBrowser` runs every engine test on both the JVM and browser.
- `./gradlew :benchmarks:jmh` runs the separate JVM-only JMH benchmarks. Benchmark execution is not
  part of the routine test or build lifecycle, though the normal build compiles the benchmark
  sources.
- `./gradlew :repl:realTerminalSmokeTest` runs the separate Expect-based real-terminal test.
- `./gradlew spotlessApply` formats the source tree. CI runs `spotlessCheck`, and a normal build
  also reports formatting violations.

Gradle may report tests as `UP-TO-DATE`. That is usually fine. When changing a test runner,
browser configuration, resource packaging, or a locked JavaScript dependency, force the affected
tasks with `--rerun-tasks` so cached results are not mistaken for verification.

Do not run Detekt while compilation or tests are known to be failing. Restore the ordinary test
signal first, then review static-analysis findings.

`./gradlew dokkaGenerateHtml` generates the local API site at `docs/api/index.html`.

## Build configuration

Convention plugins under `build-logic` are layered by responsibility. `solarnet.kotlin-base` owns
the policy shared by every Kotlin target: compilation, explicit API mode, dependency alignment,
Detekt, Dokka, and test logging. `solarnet.jvm` adds the JVM plugin and the repository's standard
Kotlin/JUnit 5 test dependencies. `solarnet.kmp-jvm-js` configures the JVM and browser targets, adds
shared `kotlin.test`, exposes each module's `jvmTest` as `test`, and stages browser-test resources.
Module build scripts keep only module-specific configuration; the JavaScript-only application
configures its target directly. Repository-wide formatting and Yarn policy remain in the root
build. Dependency and plugin versions are declared in `gradle/libs.versions.toml`, while dependency
repositories are declared centrally in `settings.gradle.kts`; JitPack is restricted to the pinned
better-parse fork.

All Kotlin modules use strict explicit API mode. Declarations that form a module's public API must
spell out `public` and their public types; declarations used only within one module should be
`internal` or `private`. This makes accidental API growth and signature changes visible in review.

## Test design

Prefer tests that exercise several pieces together. Do not mirror a production list or data object
in a test merely to detect that the list changed. Test observable behavior through the normal
test-facing layer: test the card, rule, or workflow result rather than a private transformation,
exact intermediate task text, or other implementation detail.

Keep scenarios minimal and legible. Card tests use the base game and two players by default unless
the behavior requires something else, add only relevant options and components, and consistently
name the gameplay objects `p1` and `p2`. Use `manual()` when only the resulting setup matters instead
of replaying an irrelevant play-card sequence. Avoid `sneak`: it can create impossible states.

Full-game tests override a `config` property with a `GameConfig`, conventionally built from an
indented multiline string. Authority-backed premise resolution adds `TerraformingMars` and, when
no other map is named, `TharsisMapOption`; the parser already trims each entry, so these literals do
not need `trimIndent()`. The raw-configuration overload in `CardTest` uses the same resolution path.

`CardTest` and the full-game fixtures provide `TaskResult.expect()`. Expectations are partial net
deltas: name only changes that matter to the behavior under test. Unqualified owned Types are scoped
to the Player inferred from the result's ordered change events; qualify an Owner explicitly when
checking another Player or an intentionally cross-player total. Do not restate costs, fixture setup,
or every incidental resource movement. Use a zero scalar, such as `0 Plant` or `PROD[0 Energy]`, to
assert that a particular type did not change.

Cover meaningful boundaries, negative cases, non-targets, and option combinations rather than only
the happy path. A filtering or Type-variable test should include several tempting Components that must not
match. Preserve this coverage during refactoring.

`BugsTest` is different: its passing tests characterize known incorrect behavior, and their names
say what currently happens incorrectly. Prefer such a characterization over a disproportionate
workaround. Once the bug is fixed, move the useful scenario to its proper behavioral suite.

## Translating game logs

Whole-game tests are high-value integration evidence. When translating a supplied game log:

- Before editing a dated whole-game fixture, explicitly inspect its matching
  `_local/GameYYYYMMDD/` directory and read any `implementation-plan.md` there before acting. The
  repository's `_local` path may be a symlink, which `rg --files` does not traverse, so a general
  file search is not evidence that the fixture's local sources are absent.
- In multiplayer action phases, express each player's actions as `player.turn { ... }`. A normal
  turn block contains up to two actions and automatically declines an unused second action. Once
  every other player has passed, keep the remaining player's actions through `pass()` in one turn
  block; workflow-provided `NewTurn` tasks let that block continue across the remaining nominal
  turns.
- Use the same turn block for a player's two Prelude plays. This also supports Prelude effects that
  immediately play another Prelude or project inside one of those plays.
- Preserve supplied log lines as comments near the test actions that translate them. Assert
  selective checkpoints, summaries, and final facts rather than mechanically asserting every log
  entry.
- Treat every supplied screenshot as an authoritative snapshot at its exact point in the timeline.
  At a generation boundary, reproduce any research choices logged before that screenshot, then
  reconcile every visible resource and production discrepancy before the first action. If the
  screenshot was taken before purchases, reconcile before them. Write explicit relative `exMachina()`
  deltas; do not set absolute values or let differences accumulate until a later screenshot.
- Logs may not indicate how much steel/titanium/etc. was used toward a purchase. A reasonable
  default assumption to start with is that they probably spent as much of it as they could get full
  value for. Later events may reveal that your assumption needs to be revised.
- The herokuapp and Solarnet map coordinates differ: herokuapp counts from the first tile whereas
  Solarnet uses slant-columns.
- Prefer supplied logs, images, and local map data over investigating another application's
  implementation. Work around unsupported engine behavior narrowly and record real follow-ups in
  `TODO.md`.
- Never call `sneak` directly in a game fixture. Use the fixture's `exMachina` helper for an
  evidence-backed player error that requires a direct state adjustment. Place it as late in the
  timeline as the sourced assertions allow, with a comment saying which later step requires it. Add
  source-backed `.expect()` assertions for the mistake-prone types to preceding actions wherever
  practical so the remaining unexplained gap is bounded as narrowly as possible.
  If auto-exec has already prepared the next task, the helper rolls that preparation back and
  repeats it after the adjustment so state-dependent instructions are recalculated.
  Keep unexplained state reconciliations as standalone timeline statements.
  Never place a manual or other raw adjustment inside an unrelated action body to evade a prepared-task or
  operation-boundary restriction; use an explicit fixture mechanism or fix the helper/API instead.
  Nest a missing consequence only when the enclosing action genuinely caused it.

## Multiplatform tests

- Shared `kotlin.test` test classes and methods may not be private; even though JVM test runs may
  work with them, JS test runs may not.
- Mocha owns the per-test timeout; Karma owns browser activity, disconnect, and reconnect
  timeouts. A long synchronous test can block the browser event loop long enough to hit either.
- A Node test target is not a substitute for the browser suite while Canon resources are loaded
  with browser `XMLHttpRequest`.
