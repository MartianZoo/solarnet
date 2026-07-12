# TODO

## Discussed

- Investigate adapting the existing PET AST random generator to Kotest property testing, especially
  whether domain-aware shrinking can produce smaller, understandable failures without replacing
  the useful recursive generation logic wholesale.
- Revisit Kotlin Gradle Plugin binary API validation after it matures beyond the experimental
  Kotlin 2.2 implementation. Evaluate ABI snapshots for the public `pets`, `engine`, `canon`, and
  `script` APIs and integration with the normal `check` lifecycle.
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

- Implement `TfmWorkflow.Auto` final greenery handling instead of leaving final greenery placement to
  test/manual code after the final production phase.
- Migrate the remaining `engine` and `repl` tests from Truth to Kotest-backed assertions.
- Extract the duplicated Karma canon-resource serving setup if more Kotlin/JS browser-test modules
  need access to canon data files.
- Recheck Kotlin/JS's Mocha dependency after the next Kotlin upgrade; Kotlin 2.2.21 currently pins
  Mocha 11.7.1, whose transitive Glob 10.5.0 emits an upstream end-of-support warning during a fresh
  Yarn install.
