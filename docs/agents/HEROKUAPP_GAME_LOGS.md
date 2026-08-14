# Translating herokuapp game logs

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

## Discover and preserve the evidence first

Do not begin translating actions until the source archive has been inventoried. For a dated fixture,
inspect `_local/GameYYYYMMDD/` explicitly, including any `implementation-plan.md`; `_local` may be a
symlink that a repository-wide file search does not traverse.

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

Write the exact local screenshot filename in the fixture comment. A later rename should be reflected in
the test so the assertion stanza remains traceable to its source.

Before coding, make a compact evidence inventory covering:

- board, expansions, variants, drafting, included and banned cards;
- player order, colors, corporations, preludes, handicaps, and secondary corporations;
- randomized milestone and award pools and the goals actually funded or claimed;
- every screenshot's generation and position relative to Research purchases;
- final resources, production, TR, tableau, board, score breakdown, winner, and tiebreak facts.

Different player IDs can refer to the same game. Prefer the ID whose player record exposes the most
useful setup or final-state details, while retaining all IDs as provenance.

## Treat sources according to what they prove

The full log is nearly a gold standard for action identity, order, choices, and narrated consequences.
Preserve relevant lines verbatim as comments beside the code that represents them. When the fixture
disagrees, first suspect our chronology, payment allocation, card implementation, workflow, or engine.
Override the log only with stronger independent evidence.

The log is intentionally incomplete in several respects. It commonly omits payment composition,
discount arithmetic, some automatic effects, and the internal details of an undo. Screenshots and player
records are authoritative point-in-time state, but only at their exact phase. The final player record is
authoritative for final resources, production, tableau, scoring, and board state.

An assertion without an external source is still useful as a characterization, but label it honestly.
For example, a missing generation screenshot may use a full resource/production stanza with a comment
that it merely records the fixture's actual values. Never present such a stanza as source validation.

## Establish chronology before reconciling values

Translate the complete log skeleton first: setup, purchases, turns, passes, production, final greenery,
and scoring. A missing pass can leave the replay in the action phase and make every later production or
final-greenery value appear wrong. Verify phase transitions before adding resources to compensate.

In multiplayer action phases, use `player.turn { ... }` for ordinary two-action turns. Once every other
player has passed, keep the remaining player's actions through `pass()` in one turn block when the
workflow permits. Play both of a player's Preludes in the same turn block.

Keep each log comment directly above its representing statement. Put a consequence inside an action
lambda only when that action genuinely causes it. Do not use an unrelated executable context as a place
to hide a manual adjustment.

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
6. triggered effects from every player, especially in the first three-player fixture;
7. setup facts such as a handicap or second corporation;
8. omitted pass, choice, or effect statements in the fixture.

Look for repeated discrepancies before treating them as separate errors. A constant M€ shortfall every
generation is more likely a missing TR handicap than several unrelated gains. Equal and opposite
adjustments at consecutive checkpoints usually indicate a stale repair that should be deleted.

## Assert every sourced checkpoint completely

Every dashboard screenshot must have a stanza at its exact timeline position. Research screenshots that
show `drafting` or `researching` belong before card purchases. Assert all six resources and all six
production values for every player:

```kotlin
// GameYYYYMMDD-dashboards-gen4.png was taken before cards were bought.
mom.assertResources(m = 30, s = 1, t = 0, p = 9, e = 1, h = 3)
mom.assertProduction(m = -2, s = 1, t = 0, p = 5, e = 1, h = 1)
ellie.assertResources(m = 25, s = 2, t = 4, p = 3, e = 0, h = 8)
ellie.assertProduction(m = 1, s = 2, t = 3, p = 1, e = 0, h = 5)
```

Do not create an absolute setter such as `exMachinaToSnapshot()`. Absolute targets hide what the replay
got wrong and silently change meaning when earlier reconstruction improves. If an evidenced discrepancy
must be represented, use an explicit relative delta and keep the following absolute assertions.

Add other narrow checkpoints when the sources expose them: hand counts, TR, card resources, milestone
and award ownership, tiles, global parameters, and phase. At the end, use the archived player record for
full resource and production stanzas immediately before the final VP and `Victory` assertions.

## Use expectations to localize errors

Add `.expect()` assertions for logged automatic and net effects throughout the fixture, especially from
the previous authoritative checkpoint to a discrepancy. Prioritize:

- card and standard-project payments;
- resource and production changes;
- placement bonuses and ocean income;
- draws, discards, and patent sales;
- card-resource spending and gains;
- cross-player losses and passive effects.

Expectations are partial net deltas. Include payment when it changes the same type as a logged gain. Use
typed zeroes to prove cancellation when useful. Do not duplicate a delta already executed explicitly by
`doTask()`.

When new expectations make an old repair overcompensate, remove the repair. Use nearby absolute counts
to distinguish a real gap from a stale paired adjustment.

## Reserve `exMachina()` for evidenced residuals

`exMachina()` is a last-resort fixture mechanism, not a general way to make checkpoints pass. Valid uses
include a sourced player mistake, an unsupported card whose exact consequences are known, or a setup fact
the fixture cannot otherwise express.

Use a relative delta, place it at the most likely causal action when the evidence supports that placement,
and otherwise place it at the latest defensible evidence boundary. Precede it with a comment naming the
later source entry or checkpoint that requires it. Never defer many discrepancies into one large repair,
and never bury raw state mutation in an unrelated action lambda.

Before retaining an `exMachina()`:

1. verify the timeline and phase;
2. maximize plausible full-value non-money payments;
3. add source-backed expectations since the prior checkpoint;
4. check configuration and persistent patterns;
5. test whether the discrepancy identifies a real card, ownership, workflow, or engine bug;
6. add a focused regression when a production defect is found;
7. record unsupported real behavior in `TODO.md`.

The three-player fixture demonstrated that an outer `<Anyone>` does not imply the same owner for nested
owned expressions. When a passive effect should match another player's nested component, make ownership
explicit in the canon data and cover both opponents in a focused regression.

## Endgame and scoring checks

Confirm that every player has actually passed and that final production completed before translating
final greenery. Preserve final-generation player order. After each player's last greenery, explicitly
submit `Ok` before advancing to the next player.

Use the end page and player records to assert the score table by category: TR, milestones, first- and
second-place awards, greenery, cities, aggregate card VP, selected variable-card VP, totals, ties, and the
winner. Cross-check final tile ownership and map locations against the archived board rather than relying
only on aggregate points.

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

Run the focused whole-game JVM test while reconstructing. When the fixture exposes a production defect,
add and run a smaller regression beside it. Before handoff, run formatting, the documented engine or
repository-wide suite appropriate to the changes, and `git diff --check`. Reinspect the local evidence
and final diff after the tests pass; a green replay proves internal consistency, not fidelity to the
archive.
