# Testing and verification in Pets

Read this when editing or running tests, Gradle, generators, or formatting in Pets.
Commands below run from the Pets repository root. Solarnet's player-facing scenarios, runtime
checks, and game replays have their own [testing guide](../../../docs/agents/TESTING.md).

## Standard verification

The wrapper supports JDK 17–26 through `JAVA_HOME`; JVM code targets Java 17 and Kotlin APIs
and source target Kotlin 2.2. Start with the smallest task that verifies the changed behavior.
Use the complete build when the change affects multiple modules or their contracts.

- `./gradlew build` covers formatting, JVM tests, browser-specific tests, and production JavaScript
  compilation and packaging.
- `./gradlew test` runs the JVM suites and browser-specific Pets and Almanac tests.
- `./gradlew :text:test` runs the derived English suite.
- `./gradlew :tfm-canon:test` runs Canon loading, composition, and invariant checks.
- `./gradlew spotlessApply` formats Kotlin and Gradle scripts; `spotlessCheck` checks formatting.
- `./gradlew dokkaGenerateHtml` writes the API site under `build/dokka/html`.
- `./gradlew :pets:test` runs Pets JVM tests and its browser-specific test. The routine browser
  run excludes the shared JVM/JS suites; use the temporary full-browser target below to run those.
- `./gradlew :pets:wasmJsTest` runs the shared Pets tests in Chrome and Node. These Wasm tests
  also run during `build`; generated parser round trips have a 60-second Mocha timeout in Chrome.
- `./gradlew :pets:jvmTestCoverage` runs only the Pets module's JVM test suite and writes HTML and
  XML coverage reports for Pets production code under that module's
  `reports/jacoco/jvmTestCoverage` build directory.
- `./gradlew :junkdrawer:sampleRandomCards` prints randomly generated project cards as raw Pets.
  Use `-PrandomCardCount=N` and `-PrandomCardSeed=N` to control and reproduce a sample, and add
  `-PrandomCardOutput=PATH` to write it to a text file. The task has no dependency on the language
  module. Its weights favor nested selectors, refinements, sequences, gates, and per-unit metrics so
  the raw Pets can exercise downstream consumers structurally.
- `./gradlew :text:writeRandomCardEnglishText -PrandomCardInput=PATH
  -PrandomCardEnglishOutput=PATH` renders top and bottom English for one saved random-card report.
  Add `-PrandomCardEnglishComparisonOutput=PATH` for a compact report pairing one bracket-free card
  region with only its contributing Pets. This separate consumer keeps the random-card generator
  independent of the language module.

For every shared browser-compatible suite, temporarily uncomment `allBrowserTests` in this
build's `build.gradle.kts` and run `./gradlew allBrowserTests --rerun-tasks`; restore the comment
afterward. Routine browser runs use `BrowserPetsTest` and Almanac's English card-text test.
Other generated JavaScript browser tasks are disabled unless that full-browser target is requested.
Chrome or Chromium is required; `CHROME_BIN` can select the executable.

When changing test runners, browser configuration, resources, or locked JavaScript dependencies,
use `--rerun-tasks` for the affected tasks. An `UP-TO-DATE` result is otherwise normally sufficient.
Avoid overlapping Gradle verification runs on the same host. Test tasks use at most two forks and
1 GiB per fork; `org.gradle.workers.max=2` bounds aggregate concurrency per invocation.
Scope configuration-matrix fixtures to test instances so their Catalogs can be collected.

## Build configuration

`pets.kotlin-base`, `pets.jvm`, and `pets.kmp-jvm-js` own compilation, strict explicit API mode,
dependency alignment, documentation, and test policy. Common source packages are selected by
module build files from shared `src/` and `test/` trees. Versions live in
`gradle/libs.versions.toml`; dependency repositories live in `settings.gradle.kts`.
The JVM and multiplatform conventions use JUnit 5; multiplatform modules expose `jvmTest` as `test`.
See [BUILDS.md](BUILDS.md) for standalone outputs and publication.

All public production declarations must spell out `public` and public types. Keep implementation
helpers `internal` or `private`, while preserving the intentional
[AST and parser API](SPEC_FIDELITY.md#deliberate-public-surface).

Yarn's `serialize-javascript` resolution warning reflects the deliberate 7.x security pin while
Mocha requests 6.x. The ignored-lifecycle-scripts warning reflects Kotlin/JS policy.

## Test design

Preserve these distinct purposes:

- Pure language tests cover parsing, preprocessing, transformation, and rendering without Canon.
- Pure type-system tests use small local declarations for loading, type relationships, metrics,
  requirements, and related semantics.
- Canon admissibility tests check loading and representative configuration composition. Do not
  duplicate card lists, bundle totals, or exact authored trees merely to detect data changes.
- English tests check derived text and visible refusals; see [LANGUAGE.md](LANGUAGE.md#evidence-and-verification).
- Known-defect characterizations explicitly say that current behavior is wrong. Specifications
  establish the intended contract; passing tests alone do not.

Keep language/type conformance tests aligned with numbered specification rules. Use minimal
examples that demonstrate information preservation without reproducing engine execution.
Actual card outcomes, live-world rollback, and full games are tested by Solarnet.
Share a test fixture only when it belongs to both consumers; do not depend on Solarnet test support.
Give distinct scenario variants named tests so a failure identifies the case.

## Multiplatform tests


- Shared `kotlin.test` test classes and methods may not be private; even though JVM test runs may
  work with them, JS test runs may not.
- Mocha owns the per-test timeout; Karma owns browser activity, disconnect, and reconnect
  timeouts. A long synchronous test can block the browser event loop long enough to hit either.
- A Node test target is not a substitute for the browser suite, which verifies browser compilation
  and integration.
