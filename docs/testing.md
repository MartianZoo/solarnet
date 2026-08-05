# Testing and verification

(This is a by-codex-for-codex doc.)

## Routine verification

The wrapper supports and directly uses the JDK selected by `JAVA_HOME` from 17 through 26. JVM code
targets the Java 17 bytecode and API surface, while Kotlin source and standard-library APIs target
Kotlin 2.2. Contributors do not need another JDK installed.

- `./gradlew build` is the normal repository-wide check. It runs the JVM tests and the routine
  browser tests. The engine's slow browser suite is deliberately excluded.
- `./gradlew test` runs every repository JVM test suite, including the multiplatform modules whose
  JVM test tasks are named `jvmTest`.
- `./gradlew :engine:allTests` is the routine engine suite.
- `./gradlew :engine:allTestsIncludingSlow` or
  `./gradlew :engine:allTests -PincludeSlowTests=true` includes the slow engine browser tests.
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

Shared Kotlin, static-analysis, documentation, and multiplatform browser-test resource configuration
lives in three convention plugins under `build-logic`: common Kotlin policy, JVM modules, and
JVM/JavaScript modules. Module build scripts keep only module-specific configuration; the sole
JavaScript-only application configures its target directly. Repository-wide formatting remains in
the root build so the qualified `:spotlessApply` task formats every module. Dependency and plugin
versions are declared in `gradle/libs.versions.toml`, while dependency repositories are declared
centrally in `settings.gradle.kts`; JitPack is restricted to the pinned better-parse fork.

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

`CardTest` and the full-game fixtures provide `TaskResult.expect()`. Expectations are partial net
deltas: name only changes that matter to the behavior under test. Do not restate costs, fixture
setup, or every incidental resource movement. Use a zero scalar, such as `0 Plant` or
`PROD[0 Energy]`, to assert that a particular type did not change.

Cover meaningful boundaries, negative cases, non-targets, and option combinations rather than only
the happy path. A filtering or linkage test should include several tempting components that must not
match. Preserve this coverage during refactoring.

`BugsTest` is different: its passing tests characterize known incorrect behavior, and their names
say what currently happens incorrectly. Prefer such a characterization over a disproportionate
workaround. Once the bug is fixed, move the useful scenario to its proper behavioral suite.

## Translating game logs

Whole-game tests are high-value integration evidence. When translating a supplied game log:

- Preserve supplied log lines as comments near the test actions that translate them. Assert
  selective checkpoints, summaries, and final facts rather than mechanically asserting every log
  entry.
- Treat screenshot assertions as snapshots at a particular moment. If the screenshot was taken
  before card buying but the assertion is after Research, adjust only money and hand counts.
- Logs may not indicate how much steel/titanium/etc. was used toward a purchase. A reasonable
  default assumption to start with is that they probably spent as much of it as they could get full
  value for. Later events may reveal that your assumption needs to be revised.
- The herokuapp and Solarnet map coordinates differ: herokuapp counts from the first tile whereas
  Solarnet uses slant-columns.
- Prefer supplied logs, images, and local map data over investigating another application's
  implementation. Work around unsupported engine behavior narrowly and record real follow-ups in
  `TODO.md`.

## Multiplatform tests

- Shared `kotlin.test` test classes and methods may not be private; even though JVM test runs may
  work with them, JS test runs may not.
- Mocha owns the per-test timeout; Karma owns browser activity, disconnect, and reconnect
  timeouts. A long synchronous test can block the browser event loop long enough to hit either.
- A Node test target is not a substitute for the browser suite while Canon resources are loaded
  with browser `XMLHttpRequest`.
