# TODO

## Discussed

- Teach task selection to choose the Venus branch of Atmoscoop's `2 TemperatureStep OR 2 VenusStep`
  without requiring a manual state adjustment in `Game20230521Test`.
- Now that gameplay queue views are scoped, search tests for simplifications where they previously
  hacked around all queues being merged.
- Use `docs/engine-api-review.md` to guide a gradual replacement of `godMode()`/layer casts with
  explicit player, workflow, monitor, debug, and fixture role objects.

## Not Yet Discussed

- Implement `TfmWorkflow.Auto` final greenery handling instead of leaving final greenery placement to
  test/manual code after the final production phase.
- Replace the `Gain.copy(...)` call in `Transformers.atomizeChanges()` before Kotlin 2.2 turns the
  exposed-copy-visibility warning into an error.
- Expose expected-token data from the BetterParse PETS parsers so REPL completion can ask for valid
  next terminals directly instead of probing candidate strings against the parser.
