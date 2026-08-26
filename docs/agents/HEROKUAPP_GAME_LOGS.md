# Translating herokuapp game logs

> **Read when:** reconstructing a game whose primary evidence is the herokuapp API log, end page,
> and screenshots.
>
> **Skip when:** reconstructing a physical game from audio/photos; use
> [OTB_GAME_RECORDS.md](OTB_GAME_RECORDS.md). For ordinary card or rule tests, use
> [TESTING.md](TESTING.md).
>
> **Status:** replay procedure. It intentionally contains no game-specific answers.

## Code entry points

- [`AbstractFullGameTest.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/replays/AbstractFullGameTest.kt)
  — search for `abstract class AbstractFullGameTest` for shared replay chronology and assertions.
- [`TestHelpers.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/TestHelpers.kt) —
  search for `exMachina` only when evidence proves a direct reconciliation is required.
- [`TfmWorkflow.kt`](../../tfm-engine/src/commonMain/kotlin/dev/martianzoo/tfm/engine/TfmWorkflow.kt)
  — read only when the archive chronology crosses setup, phase, or endgame boundaries that the
  replay helper does not explain.

This guide intentionally contains no setup, action, balance, coordinate, or scoring answer for a
particular archived game. Reconstruct each test independently from its archive. An existing dated
test, Git history, commit message, or previous agent summary is not evidence about what happened.

## Discover and preserve the evidence first

Do not begin translating actions until the source archive has been inventoried. For a dated test,
inspect `_local/GameYYYYMMDD/` explicitly, including any source inventory; `_local` may be a symlink
that a repository-wide file search does not traverse. Read an `implementation-plan.md` only for its
general workflow, never for game-specific answers.

Given any player ID from an endgame or player URL, collect the machine-readable sources before editing:

1. Fetch `/api/player?id=<player-id>`. It contains the game options, selected milestones and awards,
   map state, final values and scoring for every player, and the selected player's initial and final
   details. Record every player ID and the internal game ID it reveals.
2. Fetch `/api/game/logs?id=<player-id>&full=true` for the complete plain-text log.
3. Capture the rendered `/the-end?id=<player-id>` page. It provides a useful visual record of the
   score table, score progression, global-parameter contributions, final log entries, and final board.
4. Inspect every supplied screenshot at original resolution and determine its exact phase from the UI,
   not from its timestamp or directory order.
5. Save useful sources under `_local/GameYYYYMMDD/` with stable descriptive names. Keep the raw JSON,
   full log, end-page capture, and original screenshots so later work does not depend on a live archive.

Write the exact local screenshot filename in the test comment. A later rename should be reflected in
the test so the assertion stanza remains traceable to its source.

Before coding, make a compact evidence inventory covering:

- board, expansions, variants, drafting, included and banned cards;
- player order, colors, corporations, preludes, handicaps, and secondary corporations;
- randomized milestone and award pools and the goals actually funded or claimed;
- every screenshot's generation and position relative to Research purchases;
- final resources, production, TR, tableau, board, score breakdown, winner, and tiebreak facts.

Different player IDs can refer to the same game. Prefer the ID whose player record exposes the most
useful setup or final-state details, while retaining all IDs as provenance.

Record which configured components are missing from Solarnet before replay work begins. If an unused
milestone or award is unsupported, a clearly labeled same-role substitute can keep setup moving. If a
component is actually played, claimed, funded, scored, or otherwise affects the archive, prefer
implementing its general rule. When that would be disproportionate, encode only its sourced
consequences at the right boundary, explain the limitation, and add a reusable `TODO.md` item. Never
add a test DSL operation for one named component.

## Treat sources according to what they prove

The full log is nearly a gold standard for action identity, order, choices, and narrated consequences.
During reconstruction, copied log lines are useful for auditing the translation line by line. Before
removing them, preserve the complete raw log under `_local/GameYYYYMMDD/` and compare the replay with
it for action identity, order, choices, and consequences. Delete the copied lines once a complete
replay is working and that audit is complete; retain them in an incomplete replay where they still
support reconstruction. When the test disagrees, first suspect our chronology, payment allocation,
card implementation, workflow, or engine. Override a logged claim only with stronger independent
evidence and say why.

Never invent a source-style comment. A comment that reads like a log line must be traceable to that
line in the preserved log. Label deductions and assignments explicitly as test inference, user
recollection, screenshot evidence, or player-record evidence, and state uncertainty rather than making
an inference masquerade as archived text.

The log is intentionally incomplete in several respects. It commonly omits payment composition,
discount arithmetic, some automatic effects, and the internal details of an undo. Screenshots and player
records are authoritative point-in-time state, but only at their exact phase. The final player record is
authoritative for final resources, production, tableau, scoring, and board state.

An assertion without an external source is still useful as a characterization, but label it honestly.
For example, a missing generation screenshot may use a full resource/production stanza with a comment
that it merely records replay values. Never present such a stanza as source validation, and never copy
a value from an old test into a supposedly independent reconstruction.

## Establish chronology before reconciling values

Translate the complete log skeleton first: setup, purchases, turns, passes, production, final greenery,
and scoring. Do not rebalance or insert `exMachina()` while the skeleton is incomplete. A missing pass
can leave the replay in the action phase and make every later production or final-greenery value appear
wrong. Verify phase transitions before adding resources to compensate.

Check whether the archived game used fast mode. In fast-mode multiplayer games, a player must take two
actions on every turn unless passing, so translate the actions directly and do not use `player.turn {
... }` to auto-decline a second action. In games without that restriction, use `player.turn { ... }`
consistently for ordinary turns instead of scattering many `declineSecondAction()` calls through the
test. Once every other player has passed, keep the remaining player's actions through `pass()` in one
turn block when the workflow permits. Play both of a player's Preludes in the same turn block.

A Herokuapp `Player passed` log line does not always mean that the pass happened at that point in turn
order. After taking one action, a player can combine three declarations in one request: forgo the
second action, end the current turn, and commit to passing when their next turn arrives. Herokuapp logs
the pass immediately to save a later client/server round trip. When such a line follows a player's
first action, determine whether it is this early declaration before translating it. In Solarnet, let
the one-action turn end normally and call `pass()` only when that player's next turn is actually
prepared; the executable pass-call order can therefore differ from the source log order. Preserve the
verbatim log lines in their source order near the relevant boundary, but do not treat every logged
pass as an immediate gameplay event.

While copied log comments remain, keep each one directly above its representing statement. Put a
consequence inside an action lambda only when that action genuinely causes it. Do not use an unrelated
executable context as a place to hide a manual adjustment.

## Keep archive replays visually consistent

Start each replay with a comment that says whether it is complete or the exact generation boundary
through which it is implemented, followed by the archived title, internal game ID, and end-page URL.
Name a partial test method for that boundary rather than as though it covered the entire game.

Explicitly set `inputOnlySynonyms` to an empty list. Spell out resource, production, rating, and point
types in test strings.

Use consistent provenance labels for retained comments: `Player-record evidence`, `Screenshot
evidence`, `Test inference`, `Payment reconstruction`, `Chronology`, and `Unsupported component`.
Keep final assertions in source order: card-hand closure, resources, production, score breakdown,
Terraform Rating, Victory Points, then winner.

Research screenshots need special care. The UI state—not a filename timestamp—determines whether a
dashboard was captured before card purchases, after drafting choices, or after payment. Put the entire
checkpoint at that exact boundary. Do not shift it past `buyCards()` and compensate only M€ or hand size;
that loses the screenshot's power to validate the complete state.

## Reconstruct payments before inventing discrepancies

Herokuapp logs usually identify a card and its consequences but not how it was paid for. The first
assumption is that the player spent as many eligible non-money resources as possible while receiving
full value from every unit, then paid the remainder in M€.

Apply that assumption across the player's upcoming purchases, not blindly to one card in isolation. A
later Building or Space card may prove that some steel or titanium was preserved. Discounts change which
allocations receive full value. Later screenshot and final balances can often prove a unique allocation:
one additional steel, for example, may simultaneously explain two missing M€ and the final steel total.

When a checkpoint differs, reconstruct a ledger from the previous authoritative checkpoint. Check, in
order:

1. purchase timing and card-buying cost;
2. steel, titanium, microbes, floaters, discounts, and rebates used for payment;
3. ocean adjacency and placement bonuses;
4. patent sales and action costs;
5. production, energy-to-heat conversion, and TR income;
6. triggered effects from every player; in multiplayer, test both other players rather than assuming
   that a rule written for one opponent generalizes;
7. setup facts such as a handicap or second corporation;
8. omitted pass, choice, or effect statements in the test.

Look for repeated discrepancies before treating them as separate errors. A constant M€ shortfall every
generation is more likely a missing TR handicap than several unrelated gains. Equal and opposite
adjustments at consecutive checkpoints usually indicate a stale repair that should be deleted.

Treat a checkpoint mismatch as a constrained ledger problem across the whole interval. A final balance
in a non-money resource can disambiguate an earlier payment and simultaneously explain an M€ mismatch.
Do not validate one purchase in isolation when later payments draw from the same stock.

## Assert every sourced checkpoint completely

Every dashboard screenshot must have a stanza at its exact timeline position. Research screenshots that
show `drafting` or `researching` belong before card purchases. For every player, call both
`assertResources()` and `assertProduction()` with all six values even when only one differs. Put the
exact local screenshot filename and its before/after-purchase interpretation immediately above the
stanza.

Do not create an absolute setter such as `exMachinaToSnapshot()`. Absolute targets hide what the replay
got wrong and silently change meaning when earlier reconstruction improves. If an evidenced discrepancy
must be represented, use an explicit relative delta and keep the following absolute assertions.

Add other narrow checkpoints when the sources expose them: hand counts, TR, card resources, milestone
and award ownership, tiles, global parameters, and phase. At the end, use the archived player record for
full resource and production stanzas immediately before the final VP and `Victory` assertions. After
final greenery, also assert the remaining plants when the archive exposes them.

## Use expectations only for interesting results

An expectation is a partial net delta for one completed operation. A narrated log result is not by
itself a reason to assert it, but the bar should not be so high that a replay loses most of its local
behavioral evidence. Add an expectation when the result is interesting because it is:

- automatic, passive, conditional, cross-player, or otherwise non-obvious;
- zero, capped, cancelled, protected, or unexpectedly absent;
- a one-off, source-narrated combination of several distinct consequences;
- variable, threshold-dependent, or materially changed by a discount, rebate, or interacting card;
- needed to bound a nearby sourced reconciliation or uncertain payment allocation; or
- evidence for behavior that the action body cannot express directly.

An ordinary card can therefore merit an expectation when its sourced result has multiple interacting
parts or a state-dependent amount. A single fixed production or resource gain usually does not. Do not
use an expectation merely because a standard action has its ordinary result or a global-parameter
increase ordinarily grants TR. Do not restate an explicit cost, payment argument, literal `doTask()`,
milestone claim, award funding, or every repetition of the same recurring action. A late conversion
that earns zero TR is interesting; an ordinary conversion that earns one is not.

Assert only the interesting net types, not every change from the operation. Expectations are net, so
the amount can differ from the source's gross statement when a same-type cost, rebate, or passive effect
is part of what makes the result interesting. Prefer `Microbe`, `Animal`, or another readable general
type unless the destination is genuinely ambiguous. Use typed zeroes to prove cancellation, a failed
draw, or the absence of a promised result.

If the source states an absolute balance or global value, prefer a nearby absolute assertion. Around a
forced `exMachina()` reconciliation, add enough expectations for the affected types to bound the gap to
the smallest sourced interval, but do not expand unrelated expectations. When new expectations make an
old repair overcompensate, remove the repair.

## Reserve `exMachina()` for evidenced residuals

`exMachina()` is a last-resort test mechanism, not a general way to make checkpoints pass. Valid uses
include an archived setup fact the test cannot otherwise express, an out-of-band adjustment explicitly
shown by the source, or an unsupported component whose exact consequences are known. A discrepancy by
itself is not evidence that the archived application made a mistake.

Use a relative delta, place it at the most likely causal action when the evidence supports that placement,
and otherwise place it at the latest defensible evidence boundary. Precede it with a comment naming the
later source entry or checkpoint that requires it. Never defer many discrepancies into one large repair,
and never bury raw state mutation in an unrelated action lambda.

Before retaining an `exMachina()`:

1. complete and verify the timeline and phase;
2. maximize plausible full-value non-money payments;
3. add source-backed expectations since the prior checkpoint;
4. check configuration and persistent patterns;
5. test whether the discrepancy identifies a real card, ownership, workflow, or engine bug;
6. add a focused regression when a production defect is found;
7. record unsupported real behavior in `TODO.md`.

In nested owned expressions, an outer `<Anyone>` does not necessarily bind an inner owned component to
the same owner. When a passive effect should match another player's nested component, make ownership
explicit in the canon data and cover every opponent in a focused multiplayer regression.

## Endgame and scoring checks

Confirm that every player has actually passed and that final production completed before translating
final greenery. Preserve final-generation player order. After each player's last greenery, explicitly
submit `Ok` before advancing to the next player. Do not try to predict whether bonuses could enable one
more greenery; the workflow intentionally asks the player to decline.

Use the end page and player records to assert the score table by category: TR, milestones, first- and
second-place awards, greenery, cities, aggregate card VP, selected variable-card VP, totals, ties, and the
winner. Assert final plants after greenery and all six final resources and production values when the
player records expose them. Aggregate flat card VP when the archive reports only an aggregate; individual
variable-card assertions are useful when directly checkable. Cross-check final tile ownership and map
locations against the archived board rather than relying only on aggregate points.

## Preserve valuable counterfactuals

A historical replay contains exact states that are expensive to recreate in a small test. Add a few
negative assertions when they prove a non-obvious boundary that was genuinely tempting at that moment:

- a requirement or milestone threshold is almost, but not yet, met;
- a target is protected, has an insufficient amount, or is excluded by narrowing;
- a second action could not legally have been performed first for a non-obvious reason.

Use the precise domain exception (`RequirementException`, `NarrowingException`, `LimitsException`,
`DeadEndException`, or another specific result). Put a choice-level failure inside the action lambda
beside the successful choice. Skip obvious cases that merely require the card or resources gained by the
preceding action, and do not turn the whole-game test into a substitute for focused rule tests.

## Map coordinates

The log's Mars area IDs are row-major physical positions. They start at `03`, scan each row from upper
left to lower right, and count only real Mars spaces. Solarnet uses row and slant-column names:

| Log IDs | Solarnet row | Solarnet columns |
| --- | --- | --- |
| 03–07 | 1 | 1–5 |
| 08–13 | 2 | 1–6 |
| 14–20 | 3 | 1–7 |
| 21–28 | 4 | 1–8 |
| 29–37 | 5 | 1–9 |
| 38–45 | 6 | 2–9 |
| 46–52 | 7 | 3–9 |
| 53–58 | 8 | 4–9 |
| 59–63 | 9 | 5–9 |

Identify the map from metadata and corroborate it with logged area bonuses before attaching a map prefix.
Random milestones and awards do not identify the map.

## Verification

Run the focused whole-game JVM test after each source-backed span. When the test exposes a production
defect, fix it only with a proportionate general design and add a smaller regression beside it.

Before handoff:

1. audit the test directly against the complete source log for chronology, choices, and consequences;
2. trace every setup fact and assertion to the raw log, screenshot, end page, or player record;
3. recheck screenshot phase placement and all six values in every dashboard stanza;
4. justify every `exMachina()` and search once more for chronology, payment, ownership, or workflow
   explanations;
5. confirm final passes, production, greenery `Ok`s, full final state, scoring categories, and winner;
6. run formatting, the documented engine or repository-wide suite appropriate to the changes, and
   `git diff --check`;
7. inspect the final diff for game-specific APIs, accidental production changes, stale repairs, and
   claims copied from an old test.

A green replay proves internal consistency, not fidelity to the archive. The test is finished only
when its chronology and assertions remain independently traceable to the preserved source set.
