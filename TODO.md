# TODO

## Discussed

- Continue the Kotlin/JS browser-test migration by converting `canon` and then `engine` to Kotlin
  Multiplatform modules. `pets` now has a browser smoke test, but the old JVM-only Java overloads
  should move to JVM source sets if they are needed again.
- Replace `Canon`'s JVM resource loading with a KMP resource or generated-data strategy when adding
  non-JVM targets.
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
- Expose expected-token data from the BetterParse PETS parsers so REPL completion can ask for valid
  next terminals directly instead of probing candidate strings against the parser.
