# Building and publishing Pets

Pets is an independent Gradle build. It owns the language, Catalog, static Terraforming Mars
content, English, Almanac, and static generation/analysis tools. Runtime state, execution, Agents,
replays, and Playground belong to Solarnet.

## Everyday use

Run these commands from the Pets repository root (the `pets/` directory in a combined checkout):

```sh
./gradlew build
./gradlew :pets:test
./gradlew :text:test
./gradlew :almanac:jsBrowserDevelopmentRun
```

Sources share `src/{common,jvm,js,wasmJs}` and corresponding `test/` directories across modules.
Search and its model dependencies publish Wasm/JS variants as well as JVM and JS. Build files under
`modules/` select their packages. Open this Gradle project alone for static-model work.
See [TESTING.md](TESTING.md) for focused checks, browser coverage, and generators.

Search source compilation runs `:codegen:generatePetsXml` to generate its common Kotlin AST field
visitor from the compiled Pets declarations. The existing JVM `codegen` module owns the reflection
dependency; Search has no runtime dependency on that tool. See [Canon XPath](canon-xpath.md).

## Publication

`gradle.properties` records the publication version. Publish to an absolute path for an isolated
local Maven repository:

```sh
./gradlew publish -PpetsRepository=/absolute/path/to/maven
```

The publications use group `dev.martianzoo.pets`. Almanac publishes a ZIP of built web files, with a
runtime outgoing variant for composite consumers. Consumers need neither source nor intermediate
build directories to use these artifacts. Standalone Almanac accepts optional artwork through
`-PlocalImagesDir=/absolute/path` or its own `_local/images`.

Hosted releases and version policy remain future work. Solarnet's
[build integration](../../../docs/agents/BUILDS.md) documents source selection, version pinning,
artifact consumption, and its isolated producer/consumer verification script.

## Build independence

Pets uses ordinary project-local Gradle outputs and a standard wrapper. It has no copy of
Solarnet's account/worktree isolation scripts. When invoked through Solarnet's wrapper, including
`./gradlew -p pets`, it inherits that wrapper's output isolation in a separate pool for each Gradle
build. Its convention build has distinct plugin IDs and owns its version catalog and compilation
policy.

The previous Detekt setup used default source locations while static modules lived elsewhere,
so it reported NO-SOURCE. Pets does not carry that unused plugin/configuration. Compiler warnings,
formatting, JVM tests, and browser tests remain enforced; choosing useful static-analysis coverage
is tracked in [TODO.md](../../TODO.md).
