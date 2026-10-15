# TODO

Delete entries when they are done.

- Wild tags are not yet supported when Robotic Workforce or Cyberia Systems copies production:
  Research Network cannot be selected through its wild, and copied Gyropolis does not allocate
  Earth tags for its production. Allocate only after the copy target is known; preserve Building
  counting for Medical Lab and keep each assignment for the whole action. Bug-test pairs cover
  the rejected Research Network copies.
- Investigate represented-tag binding before shortening `@Tag<WildTag<@CardFront>>` to
  `@Tag<This>` or moving the assignment rule into an inherited `LikesTag` self-effect.
  The short spelling fails while `recordTypeVariableScopes` resolves the represented-tag usage:
  `This` has not yet been replaced with the declaring holder. Explicit holder spelling works.

- Finish faithful coverage of the 2026-10-14 unanswered-rules scenarios where current content or
  inputs cannot express the situation. `UnknownRulesTest` covers rejecting partial Celestic and
  Morning Star search counts (question 8), but not exhaustion after finding some cards: searches
  have no deck or discard supply. Question 16 (Helion retaining 2 MC while paying partial Mons
  Insurance with heat) needs real Helion; only `FakeHelion`, explicitly lacking correct payment
  allocation, exists. Do not treat that stand-in as evidence for the Canon interaction.
  Source: `/Users/dev/martianzoo/player-resources/_local/unanswered-rules-questions-2026-10-14.md`.
  Links: https://boardgamegeek.com/thread/1705209/article/37732543#37732543 and
  https://boardgamegeek.com/thread/3752654/article/48054640#48054640.

- Undo the decision to hard-code the full test catalog's fingerprint in
  `GameFileJsonTest.premiseAndCatalogFingerprintsMatchAcrossTargets`. Removing the redundant `!`
  from the built-in Signal declaration required an unrelated hash update. Preserve verification
  that JVM and JavaScript compute matching fingerprints without coupling the expected value to
  evolving built-in declarations.

- Continue adversarial Pets testing beyond parser nesting and nominal inheritance: investigate
  dependency/default expansion, concrete-type enumeration, and automatic-effect cascades. The
  current depth and subtraction limits are not a general execution-work budget.

- Skip equality-incompatible subclasses in concrete-type queries (T11-1, T11-3, and T12-4).
  Pets `BugsTest` shows `Pair<First, Second>` should retain `Independent<First, Second>` and omit
  `Same`, whose two arguments must agree. Enumeration and automatic narrowing instead throw while
  constructing `Same<First, Second>`; inhabitance returns true or throws depending on declaration order.

- Exclude subclasses with empty dependency differences during automatic narrowing (T11-3).
  Pets `BugsTest` shows `Tile<Area(NOT Land)>` correctly enumerates only `Ocean`, but automatic
  narrowing returns no result because it also counts `Greenery` with the impossible `Land(NOT Land)`.

- Preserve represented-class bindings when rendered types are parsed again (T5-6 and T8-10).
  Pets `BugsTest` shows a predicate that asks about `BuildingTag<Player1>` before rendering
  incorrectly asks about `Tag<Player1>` afterward. A repair must also handle candidate classes
  with fixed dependencies and nested declarations sharing the candidate's bound without losing
  their distinct bindings.

- Recognize the same represented-class predicate under a narrower bound (T8-1 and T8-10).
  Pets `BugsTest` shows `Class<@BuildingTag>(HAS @BuildingTag)` incorrectly fails to narrow
  `Class<@Tag>(HAS @Tag)`, although both predicates ask about the selected class.

- Implement T7-1's greatest-lower-bound promise beyond the current root-first calculation.
  Pets `BugsTest` shows `CardFront` intersected with `ResourceHolder<Class<Animal>>` returns no result
  even when `Pets` is the greatest common type and the only rival, `Ants`, is excluded by its resource kind.
  A bounded candidate-search experiment was rejected: a failed dependency meet can mean an
  unnamed overlap, not disjointness, so discarding that candidate can return a false greatest
  type. `Spec07BoundsTest` preserves the competing `Broad<Second>` and `Narrow` witness.

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
- Audit remaining non-payment `::` effects; retain
  immediate execution only where a concrete gameplay need justifies it.

- Revisit ownership of `Selecting`, `Hand`, and played events together, including per-player
  cleanup of shared `Selecting` while other players' starting offers remain there.
- Determine which nested cross-Actor Agent calls should settle Admin work before returning;
  the Pluto scenario in `ColoniesRulesTest` currently uses separate top-level calls.

- Improve the bracketed English fragments exposed by removing the exact-expression phrase table,
  using ordinary composition only where it stays small. Marker effects, distinct-type filters,
  and L1 Trade Terminal’s gift watcher remain partly unsupported. Philantropist's class-token requirement now
  names cards in play but retains bracketed `GainsOf`. A card instance cannot fill that metric's
  `Class<CardFront>` dependency; Astra Mechanica also evaluates event classes after those cards leave play.
- Refresh the remaining `docs/english-wording-comparisons/` entries against current Canon and
  rendering. Entries touched by the delegate work are current; older entries still show previous
  invariant declarations and wording.
- Continue the selected-task English experiment using `English.describe(instruction, player)`.
  Preserve named awards and card-play targets; support other-player resource changes, variable
  payments, and remaining selection/reveal operations. Recognized Audit steps are omitted; interpret
  remaining billing obligations before hiding any additional bookkeeping. The local input is
  `_local/replay-selected-task-instructions.txt`; the comparison is `_local/replay-task-english.txt`.
  Do not treat bracket-free counts as UI readiness.

- Apply current-card identity checks to `CountedRelation.renderParticipant`: it can still call
  an arbitrary `SpecialTile` "this tile" in adjacency metrics. Site-occupant filters now check
  which concrete tile the current card places; no current Canon metric exposes the older gap.
- Share the `NonNegativeIconsOf` citation scan between gameplay and English; both currently walk
  authored effects independently, so a later change to the icon rule could make their answers differ.
  Briber's self-claim requirement still appears as `[Briber]`; keep it separate from the direct
  `Lose 12 M€` effect. The marked-variable references now follow recorded occurrences, but the older
  interaction-trigger Anyone-to-Player normalization still supplies an implicit `that player`.
- Finish Head Start's English setup wording: its gated action-choice signals still appear as
  bracketed Pets after removing the exact-expression substitution.
- Replace recursive task-ancestry list concatenation with linear construction, and avoid rebuilding
  the target's ancestry when comparing matching tasks. Preserve recorded task disambiguation.
- Reduce repeated parsing during game-file decoding and reuse the full export already produced
  for reconstruction instead of serializing it again.
- Investigate repeated selection probes followed by execution under `EAGER`; preserve manual-task
  competition and failure handling before removing probes.
- Investigate repeated ranking of the same field while testing each candidate. Scores must reflect
  the current state, including rollback; avoid a second persistent representation of game scores.
