# TODO

Delete entries when they are done. Static-language and English work lives in [Pets TODO](pets/TODO.md).

- Investigate the deferred nested-placement and partial-selection experiment in
  [Make types types again](docs/agents/MAKE_TYPES_TYPES_AGAIN.md) if that proposal is selected.

- Investigate the local configuration-cache invalidation caused by the injected
  `kotlin.project.persistent.dir` property. Repeated `help` commands reproduce it on the
  pre-split baseline as well as the composite build; build separation preserves the existing
  generated-state isolation rather than redesigning it.

- Verify IDE source navigation and editing across the separate Pets and Solarnet builds.
  The command-line check proves that an unpublished Pets edit reaches a Solarnet test;
  the interactive IDE acceptance check remains unverified.

- Revisit the Playground Effects tab's temporary exclusions (Placement, UseAction, Billing,
  End, and self triggers) after reviewing their usefulness; consider an opt-in view for hidden subscriptions.
- Settle the Effects tab's Admin filter. Keep global effects visible for now: an unowned
  effect can execute for a player depending on the triggering event.
- Investigate leading class names disappearing intermittently in headless Chrome when toggling
  icons/trays in Effects. Reproduce in a normal browser before changing the shared PetsText
  renderer; recreating rows and splitting trigger/consequence text did not reliably fix it.

- Simplify wild-tag composition for Robotic Workforce and Cyberia Systems. Their current Pets
  declarations repeat an optional assignment effect and nested assigned-wild selectors. Find a
  smaller model that still allows Research Network to qualify as Building, lets copied Gyropolis
  and Medical Lab count assigned tags, and keeps assignments for the whole action. Explore choosing
  the copy target before assigning wilds while keeping `CopyProductionBox` limited to extracting
  the authored production box.

- Finish faithful coverage of the 2026-10-14 unanswered-rules scenarios where current content or
  inputs cannot express the situation. `UnknownRulesTest` covers rejecting partial Celestic and
  Morning Star search counts (question 8), but not exhaustion after finding some cards: searches
  have no deck or discard supply. Question 16 (Helion retaining 2 MC while paying partial Mons
  Insurance with heat) needs real Helion; only `FakeHelion`, explicitly lacking correct payment
  allocation, exists. Do not treat that stand-in as evidence for the Canon interaction.
  `FakeHelionTest` preserves eight unsupported intended scenarios beside passing `BUG -` tests:
  five self-payment cases, Stormcraft floaters, and two Mons Insurance payouts. Revisit them with
  real Helion rather than treating the stand-in's observed behavior as a ruling.
  Source: `/Users/dev/martianzoo/player-resources/_local/unanswered-rules-questions-2026-10-14.md`.
  Links: https://boardgamegeek.com/thread/1705209/article/37732543#37732543 and
  https://boardgamegeek.com/thread/3752654/article/48054640#48054640.

- Derive `TfmActionLowerer`'s standard-resource cost classification from Pets declarations
  instead of its hardcoded resource list, without adding a loading stage.
- Explore making each `StandardProject` a signal emitted only after its cost is paid.
  `UseStandardProject` would select the class and open the bill; the resulting project signal
  would trigger its tile/production effect and CrediCor/Standard Technology rebates. This
  would replace the persistent `HasActions` standard-project components and their `UseAction`
  path. Account for Established Methods' direct `UseAction` grants and other standard-project
  payment listeners when evaluating the replacement.
- Decide whether to add shallow/deep ELSE or instruction-level `(A)*`; candidate uses and
  restrictions are in [ELSE.md](pets/docs/agents/ELSE.md).
- Add Canon invariant tests for incompatibilities: self-heat removal outside PaymentScope, except
  Local Heat Trapping, must be declared incompatible with Stormcraft. Include abstract resource choices.
- Revisit Stormcraft plus Helion payment composition if that combination is supported.
  `Accepting` now skips alternative standard-resource offers when the payer has none; it does not
  consider possible substitutes such as Stormcraft floaters for heat.
- Verify as a Canon invariant which tasks can interleave with card play, including gaining MC
  production and other actual exceptions. Fake Head Start currently allows interleaving as a
  known approximation, not a requirement.
- Consider renaming automatic (`::`) effects to “immediate.”
- Replace the archived *Life of an Effect* walkthrough with a current account of lexical `Me`
  and the transformation pipeline.
- Review `pets/docs/class-existence-scenarios-draft.md` and consolidate repeated cases in
  `ClassDefinitionBoundaryTest` and `ClassTableSelectionTest`.
- Decide whether Agent submissions must use concrete amounts instead of arithmetic over `Owed`.
- Give beginner and standard players the same ten-card `Selecting` offer; beginners keep it free,
  while standard players discard and buy their choices.
- Clarify Astra Mechanica's zero/one/two eligible-event behavior and reconcile its modeled return
  with printed and derived English.
- Separate task selection from execution when automatic execution is disabled.
- Investigate permanently locking Admin to greedy `EAGER` execution, as selected in
  [SMART_AUTOEXEC.md](docs/agents/SMART_AUTOEXEC.md). Trace replay/import and tests that currently
  disable Admin autoexecution before changing the Agent policy API.
- Investigate selecting tasks by their main class without narrowing them, including grouped
  instructions, so more sandbox `selectOnlyTask` calls can use meaningful names.
- actually plan out how exMachina should work
- Replace ordering-only `::` protocols in pricing/acceptance, wild-tag preparation, and
  Turmoil/Politician influence measurement. Decompose government formation before judging its
  individual invariant repairs; `THEN` on a signal does not wait for its triggered work.
- Remove bootstrap/client-shape reasons for `::`: default offer counts before signed adjustments,
  inferred construction order, and bundled card-search/reward/Audit work. Preserve meaningful
  correction relationships and avoid adding client steps.
- Revisit queued requirement adjustments without adding a player step: `Required` is System, but
  `-2 Required<Class<GlobalParameter>>` remains abstract until selection. Inventrix, Adaptation
  Technology, and Special Design therefore still use immediate live narrowing.

- Revisit ownership of `Selecting`, `Hand`, and played events together, including per-player
  cleanup of shared `Selecting` while other players' starting offers remain there.
- Determine which nested cross-Actor Agent calls should settle Admin work before returning;
  the Pluto scenario in `ColoniesRulesTest` currently uses separate top-level calls.

- Briber's self-claim requirement still appears as `[Briber]`; keep it separate from the direct
  `Lose 12 M€` effect. The marked-variable references now follow recorded occurrences, but the older
  interaction-trigger Anyone-to-Player normalization still supplies an implicit `that player`.
- Investigate repeated selection probes followed by execution under `EAGER`; preserve manual-task
  competition and failure handling before removing probes.
- Start using `Agents.overlay` for an actual client or agent forecast, then measure its cost and
  compare it with the existing fork path. Internal selection and execution probes still use atomic
  rollback and may need a cheaper construction path. Probes on a source World currently invalidate
  its overlays even when the source history returns to the same point.
- Investigate repeated ranking of the same field while testing each candidate. Scores must reflect
  the current state, including rollback; avoid a second persistent representation of game scores.

- Verify remaining inferred Research counts, anonymous draws, action grouping, and earlier
  adopted card identities in `Wsbg2025Test`. Jon's generation-six two-card Research keep now
  reconciles his confirmed patent sale to afford Robinson and zero ending cash; the purchase
  count itself is still inferred. All four final scores match, and no unexplained exMachina
  remains. Charlie's recorded Miranda overpayment/refund is intentional and does not need
  solving. Evidence and remaining uncertainties are in `_local/replays/Game20250919/sources.md`
  and `derived/action-inventory.md`.

- Investigate exact AST preservation when a replay contains a Robotic Workforce
  wild-tag choice. Original and reconstructed task text matches, but parsing
  `Tag<@WildTagElection>?` marks the type reference's empty arguments as
  explicitly specified. `OtbGame20260818Test` now verifies serialized event
  text, notes, and file round trips; the internal AST marker still differs.

## Mars Playground display

- [ ] Generate the Playground script URL from the build artifact hash so deployments update the
  browser cache without manually changing the query in `index.html`.
- [ ] Consider amount-specific positive MC icons in task text (`1 MC` as `MC01`, `11 MC` as
  `MC11`). Keep generic MC elsewhere and do not use negative amount icons.
- [ ] Reproduce the navigation freeze reported on 2026-10-15. The recording and final input are
  not yet known; navigation checks on the August 18 recording have not reproduced it.
- [ ] Investigate missed automated Play clicks when the pointer stays over the first task row as
  it changes (gain 14 TR, then draw two corporations). Headless clicks spaced 1.2 seconds apart
  sometimes missed the draw; moving away between clicks completed draw, undo, redraw, naming,
  cancellation, and discard without exceptions. Confirm in a normal browser and distinguish
  delayed rendering from hover handling before changing tooltips. Moving two pixels within the
  row did not help. Keying rows by task ID or choice label did not establish a reliable fix and
  produced `RootNodeOwner is already disposed` during cancellation/retry; that experiment was
  reverted. With the original row code, that exception appeared after reconnecting/resizing the
  automated browser with a tooltip open, but not in fresh uninterrupted sessions.

## Mars Playground solo play

- [ ] Make the live log reliable when Chrome's directory picker never appears on macOS. The game
  now starts while the picker is pending, but no file is written until Chrome grants a folder.
  Investigate a small local write path before adding another replay format or browser export flow.

- [ ] Persist enough of a live game to restore it after Chrome evicts the page from its back/forward
  cache or reloads it. The saved setup recreates the initial game only. Full game-file import still
  needs automatic card-input capture before it can reconstruct the named cards and current choices.

- [ ] Walk through Research Coordination wild-tag assignment and cleanup in Play, including
  temporary dashboard tag counts and history navigation with the restored Canon wild tags.

- [ ] Reduce the remaining first-open catalog and chosen-configuration validation pause. Menu
  candidate checks are cached and yield between candidates; constructing the first catalog and
  validating an individual premise still run synchronously in the browser.

- [ ] Extend live-session walkthroughs to Prelude plays directly from Selecting and larger
  expansion combinations. The initial review covered base-game setup, purchase/play, a complete
  losing solo run, dealer searches/transfers/reveals, cancellation, and history recovery. A follow-up
  verified neutral placement, opening draws/undo, and complete losing runs on all seven maps; this
  does not establish every expansion's task-form interaction.

## Card tracking

- [ ] Decide whether to verify card identities and claim filters for deck flips outside tracked hands
  and Selecting offers, such as Search for Life and Asteroid Deflection System. The playground
  excludes both from live games until fresh reveals can be named. Reuse tracking for known-card
  reveal/return rather than adding a second card-location model.

## Safe autoexecution

- If a single speculative engine step can still block the browser, move the completion search off
  the browser thread. The current 500 ms stop is checked between steps and cannot interrupt work
  already running inside a step.
- When adding a game mode that forbids undo back through a card draw, disable SAFE's temporary
  early-draw rule for that mode. The reported Business Empire follow-up still needs its own live
  queue check.
- Use the test-only `SafeTasks` analyzer to propose further Canon register rows, checking each
  proposal against `DecisionSearch` outcome sets with the NONE policy at real multiplayer replay
  positions. The 806-task solo sample is dominated by production income; see
  [SMART_AUTOEXEC.md](docs/agents/SMART_AUTOEXEC.md#implementation-and-remaining-research).
- Research a competing MC payment step after steel and titanium choices resolve, and card
  placement after payment. Do not enable either shape solely because a lone concrete task is
  claimable: earlier card effects can still change payment, and placement can install listeners.
  Two `TfmPlayCommandTest` cases currently fail under `auto safe`: the MC `Pay` task remains gated by
  `Accepting` while a card placement waits behind payment. Trace that payment sequence before
  adding either register row or changing the script command.
- Move forced-choice walking and dead-end hiding out of Playground's `PlaySession` so a shared
  agent can run the complete smart mode in replays and other clients.
- Add a tracked Business Network action search test: witness a completion in the fork while the
  live deck, card tracker, and World remain unchanged.
- The test-only analyzer cannot summarize some pending custom instructions or card plays.
  Investigate `CopyProductionBox` and `CopyPrelude` as row proposals only after an outcome-oracle
  check establishes the corresponding Canon queue positions.
- `:playground:wasmJsBrowserTest` disconnects (Karma ping timeout) after the first
  `RecordingStoreTest` case, which does not involve autoexecution; not yet compared with `main`.
