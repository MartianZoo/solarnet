# TODO

## Discussed

- Teach task selection to choose the Venus branch of Atmoscoop's `2 TemperatureStep OR 2 VenusStep`
  without requiring a manual state adjustment in `Game20230521Test`.
- Now that gameplay queue views are scoped, search tests for simplifications where they previously
  hacked around all queues being merged.

## Not Yet Discussed

- Implement `TfmWorkflow.Auto` final greenery handling instead of leaving final greenery placement to
  test/manual code after the final production phase.
- Replace the `Gain.copy(...)` call in `Transformers.atomizeChanges()` before Kotlin 2.2 turns the
  exposed-copy-visibility warning into an error.
- Make REPL Pets-language completion parser-aware so it can suggest only syntactically plausible
  next tokens instead of the current broad live class/keyword list.
