# TODO

## Discussed

- Finish the follow-up rename after the initial `interactive` extraction: current `repl` should
  become `script`, and `interactive` should become the new `repl`. Update Gradle modules, launch
  scripts, docs, package names, public API names, and test names in that pass.
- Revisit structured/colorized interactive output now that the script layer is plain text and
  JLine-free.

- Continue the Kotlin/JS browser-test migration by converting `engine` to a Kotlin Multiplatform
  module.
- Revisit `engine`'s Koin dependency before targeting Kotlin/Wasm; the pinned Koin metadata has
  JVM/JS/Native variants but no Wasm variant.
- Teach task selection to choose the Venus branch of Atmoscoop's `2 TemperatureStep OR 2 VenusStep`
  without requiring a manual state adjustment in `Game20230521Test`.
- Now that gameplay queue views are scoped, search tests for simplifications where they previously
  hacked around all queues being merged.
- Use `docs/engine-api-review.md` to guide a gradual replacement of `godMode()`/layer casts with
  explicit player, workflow, monitor, debug, and fixture role objects.

## Not Yet Discussed

- Upgrade or replace the deprecated Gradle Enterprise plugin before moving to Gradle 9.
- Implement `TfmWorkflow.Auto` final greenery handling instead of leaving final greenery placement to
  test/manual code after the final production phase.
- Replace the `Gain.copy(...)` call in `Transformers.atomizeChanges()` before Kotlin 2.2 turns the
  exposed-copy-visibility warning into an error.
- Migrate the remaining `engine` and `repl` tests from Truth to Kotest-backed assertions.
- Revisit the `pets` common-test Kotest version after upgrading the project Kotlin plugin; Kotest
  6.2.1 pulls Kotlin 2.2.21 metadata that Kotlin/JS 2.1.20 cannot compile against.
- Extract the duplicated Karma canon-resource serving setup if more Kotlin/JS browser-test modules
  need access to canon data files.
