# TODO

## Discussed

- Revisit structured/colorized interactive output now that the script layer is plain text and
  JLine-free.

- Convert `script` to Kotlin Multiplatform so its session, commands, parsing, and plain-text output
  work from JavaScript; keep the JVM terminal/server launchers in `jvmMain`, and add a JS session
  smoke test. `engine` is already Kotlin Multiplatform with a JS target, so address its remaining
  JS-compatibility issues as they arise rather than repeating that conversion.
- Revisit `engine`'s Koin dependency before targeting Kotlin/Wasm; the pinned Koin metadata has
  JVM/JS/Native variants but no Wasm variant.
- Once #63 permits narrowing an OR branch whose other arm is a Multi, restore Atmoscoop's canonical
  simultaneous choice and remove its temporary sequential-task encoding.
- Now that gameplay queue views are scoped, search tests and convenience APIs for simplifications
  where they previously expected an empty whole-game queue after each action; start with the
  `SoloGame0710Test` workaround.
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
