# TODO

Delete entries when they are done.

- Continue adversarial Pets testing beyond parser nesting and nominal inheritance: investigate
  dependency/default expansion, concrete-type enumeration, and automatic-effect cascades. The
  current depth and subtraction limits are not a general execution-work budget.

- Simplify Flooding and Artificial Lake's ocean instructions, preserving full-track behavior,
  mandatory placement, and Flooding's victim choice.
- Derive `TfmActionLowerer`'s standard-resource cost classification from Pets declarations
  instead of its hardcoded resource list, without adding a loading stage.
- Decide whether to add shallow/deep ELSE or instruction-level `(A)*`; candidate uses and
  restrictions are in [ELSE.md](docs/agents/ELSE.md).
- Add Canon invariant tests for incompatibilities: self-heat removal outside Billing, except
  Local Heat Trapping, must be declared incompatible with Stormcraft. Include abstract resource choices.
- Revisit Stormcraft plus Helion payment composition if that combination is supported.
  `Accepting` now skips alternative standard-resource offers when the payer has none; it does not
  consider possible substitutes such as Stormcraft floaters for heat.
- Verify as a Canon invariant which tasks can interleave with card play: gaining MC production,
  Head Start's two actions, and any other actual exceptions. Establish the complete list.
- Share the Corporation, Prelude, and final greenery phases' turn procedure through an abstract
  phase class.
- Share the circular-distance calculation in `PlayerDistance` and `PartyDistance`, preserving
  their different handling of unseated owners.
- Consider renaming automatic (`::`) effects to “immediate.”
- Replace the archived *Life of an Effect* walkthrough with a current account of lexical `Me`
  and the transformation pipeline.
- Review `docs/class-existence-scenarios-draft.md` and consolidate repeated cases in
  `ClassDefinitionBoundaryTest` and `ClassTableSelectionTest`.
- Decide whether Agent submissions must use concrete amounts instead of arithmetic over `Owed`.
- Give beginner and standard players the same ten-card `Selecting` offer; beginners keep it free,
  while standard players discard and buy their choices.
- Move Fake Self-Replicating Robots' paired defect scenarios from `SelfReplicatingRobotsTest`
  into `tfm-fake`, reusing existing test support.
- Verify Double Down can copy an actually played Prelude without `PreludeExpansion` enabled.
- Clarify Astra Mechanica's zero/one/two eligible-event behavior and reconcile its modeled return
  with printed and derived English.
- Separate task selection from execution when automatic execution is disabled.
- Investigate selecting tasks by their main class without narrowing them, including grouped
  instructions, so more sandbox `selectOnlyTask` calls can use meaningful names.
- actually plan out how exMachina should work
- Keep gated System work with Admin when its condition becomes satisfied, removing unnecessary
  player selections such as those in Head Start scenarios.
- Audit remaining non-payment `::` effects, including Pharmacy Union's starting money; retain
  immediate execution only where a concrete gameplay need justifies it.

- Revisit ownership of `Selecting`, `Hand`, and played events together, including per-player
  cleanup of shared `Selecting` while other players' starting offers remain there.
- Determine which nested cross-Actor Agent calls should settle Admin work before returning;
  the Pluto scenario in `ColoniesRulesTest` currently uses separate top-level calls.

- Improve the bracketed English fragments exposed by removing the exact-expression phrase table,
  using ordinary composition only where it stays small. Delegate replacement,
  marker movement, distinct-type filters, and L1 Trade Terminal’s gift watcher
  remain unsupported. Philantropist's class-token requirement now names cards in play but retains
  bracketed `GainsOf`. A card instance cannot fill that metric's `Class<CardFront>` dependency;
  Astra Mechanica also evaluates event classes after those cards leave play.
- Share the `NonNegativeIconsOf` citation scan between gameplay and English; both currently walk
  authored effects independently, so a later change to the icon rule could make their answers differ.
  Briber's self-claim requirement still appears as `[Briber]`; keep it separate from the direct
  `Lose 12 M€` effect. The marked-variable references now follow recorded occurrences, but the older
  interaction-trigger Anyone-to-Player normalization still supplies an implicit `that player`.
- Finish Head Start's English setup wording: its Audit marker and action-choice signals still
  appear as bracketed Pets after removing the exact-expression substitution.
- Replace recursive task-ancestry list concatenation with linear construction, and avoid rebuilding
  the target's ancestry when comparing matching tasks. Preserve recorded task disambiguation.
- Reduce repeated parsing during game-file decoding and reuse the full export already produced
  for reconstruction instead of serializing it again.
- Investigate repeated selection probes followed by execution under `EAGER`; preserve manual-task
  competition and failure handling before removing probes.
- Investigate repeated ranking of the same field while testing each candidate. Scores must reflect
  the current state, including rollback; avoid a second persistent representation of game scores.
