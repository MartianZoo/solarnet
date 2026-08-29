# Reconstructing recorded over-the-board games

> **Read when:** reconstructing a physical game from mixed evidence such as audio, photographs, and
> player-board resource records.
>
> **Skip when:** the primary evidence is a herokuapp log; use
> [HEROKUAPP_GAME_LOGS.md](HEROKUAPP_GAME_LOGS.md). For routine tests, use
> [TESTING.md](TESTING.md).
>
> **Status:** replay procedure.

## Code entry points

- [`AbstractFullGameTest.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/replays/AbstractFullGameTest.kt)
  — inspect shared chronology, checkpoint, and endgame helpers.
- [`OtbGame20260818Test.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/replays/OtbGame20260818Test.kt)
  — consult only as a syntax example after independently inventorying the new game's evidence.
- [`TestHelpers.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/TestHelpers.kt) —
  search for `exMachina` only when an evidenced physical error needs direct reconciliation.

This procedure covers live physical games reconstructed from mixed evidence such as an audio
transcript, player-board resource logs, and photographs. It supplements the shared whole-game rules
in [TESTING.md](TESTING.md). Use [HEROKUAPP_GAME_LOGS.md](HEROKUAPP_GAME_LOGS.md) instead for games
whose primary record is an application log.

## Preserve and inventory the original evidence

Before reading an existing dated test, Git history, commit message, derived reconstruction, or
another game record, inventory the original sources in `_local/replays/GameYYYYMMDD/`. Reconstruct the game
from those sources independently; old tests and plans may teach current Solarnet syntax but are
not evidence of what happened.

Keep supplied archives unchanged under `source-archives/`. Extract useful files beside them with
stable descriptive names:

- `transcript.md` for the original transcript;
- `board-HH-MM-SS.ext` for board photographs, using the local capture time when known;
- `<player>-applog-NN.ext` for each player's resource-log images in chronological order; and
- `sources.md` for original-to-local filename mappings, hashes, timestamp/timezone interpretation,
  source roles, and any transformations.

Do not overwrite an original image when rotation, perspective correction, cropping, or annotation
helps inspection. Add a sibling such as `board-HH-MM-SS-corrected.png` and record the transformation
in `sources.md`. A dated directory normally does not need its own implementation plan; keep reusable
procedure here and game-specific uncertainties in the source inventory or temporary working notes.

## Respect what each source can prove

Evidence in a physical game is complementary rather than interchangeable:

- A transcript proves the order and content of clearly spoken actions. It may omit silent
  bookkeeping, mishear card names, or faithfully record a player's mistaken arithmetic. Never
  trust a guess about who is speaking: establish the player from that player's ledger first, then
  use the transcript to interpret the ledger-backed action.
- A player-board log proves stable personal resources and production at its recorded checkpoints.
  These apps commonly merge several successive physical transactions into one net entry, omit an
  intermediate balance, or record an entry after the associated action. This is normal use of the
  app, not a discrepancy or weak evidence. Treat its rows as balance observations rather than a
  promise of one row per game operation.
- A board photograph proves visible point-in-time state: board tiles, tracks, colonies, cards,
  markers, and physical mistakes that remained on the table. Determine its exact timeline position
  from contents and timestamps rather than filename order alone.
- A raw app-log screenshot can disambiguate a ledger entry, but a machine-readable ledger is easier
  to audit when both represent the same observations.
- Repository code and other tests explain APIs and engine behavior only. Derived ledgers,
  previous guesses, excerpts, and plans are likewise secondary and never prove an event or value.

Correlate timestamps across sources. Two sources disagreeing creates a research question; it does
not authorize choosing whichever value makes the replay pass. Preserve unresolved uncertainty.

## Build an evidence map before code

Inventory setup, options, player order, corporations, preludes, colonies, milestones, awards, and
the physical map. Then map:

- transcript spans and timestamps;
- every photograph's generation, phase, and position relative to production and Research;
- every resource-log checkpoint and other stable balance;
- final board, resources, production, TR, scoring categories, total, and winner evidence; and
- corrections, apparent physical mistakes, conflicts, and gaps.

Settle the assertion contract before implementation. A useful default is:

- all six resources and all six production values at every sourced generation checkpoint, after
  automatic transition work and before the first `buyCards()`;
- `assertSidebar()` at the same checkpoint when the evidence records global state;
- intermediate balance assertions where the source records them, especially before frequently
  changing M€ becomes hard to localize;
- photo-backed tableau tags, card-front counts, tile counts, card resources, and other clearly
  visible state at each photograph's exact anchor; and
- final score assertions only where direct evidence or a clearly labeled engine summary supports
  them.

Never calculate an assertion from the replay and call it source-backed. Engine-characterization
assertions are useful only when labeled honestly. Use `assertProduction()` for complete human-value
roundups; raw M€ production component counts include the engine's internal offset.

## Translate chronology before reconciling balances

Write the chronological skeleton first: setup, corporation phase, Prelude phase, Research, turns,
passes, production, world-government and colony phases, final production, final greenery, and
scoring. Preserve every clearly ordered operation. Do not reorder actions to eliminate a mismatch.
If the sources instead show an illegal physical action cadence that the workflow cannot represent,
do not silently change ownership or invent a missing action. Isolate an extra action's evidenced
state change at its exact checkpoint when possible; otherwise make the smallest chronology distortion
and say exactly what moved and why. Record the missing general mechanism in `TODO.md`.

Follow the current full-game test style instead of copying an old revision:

- define `override val config = GameConfig(...)` with player-name arguments;
- resolve gameplay objects with `game.tfm(Player.PLAYERn)`;
- import and pass card-name constants rather than string card names;
- use named `buyCards()`/`discard()` calls when the source identifies cards, opting into
  `CardTrackingFullGameTest` only when the evidence can sustain exact hand tracking;
- use `player.turn { ... }` for routine multiplayer turns and put both Preludes in one turn block;
- once other players have passed, keep the remaining player's actions through `pass()` in one block
  when the workflow permits; and
- keep literal source comments immediately beside the operation they prove.

Preserve gameplay-bearing transcript lines verbatim beside the operations they support. Omit only
speech that provides no gameplay information; do not replace primary evidence with a paraphrase.
Retain uncertainty, corrections, and admitted mistakes. If a spoken consequence is provably wrong,
preserve the useful quote with `[sic]` and assert the stronger evidence separately. Put a
photograph's exact local filename at its timeline anchor.

## Prove actor and action identity before coding it

Make an action inventory with one row per spoken or otherwise evidenced operation: source span,
candidate card or action, actor evidence, payment, resulting changes, and later photograph or ledger
confirmation. Establish the actor from player-specific evidence before using the transcript to fill
in the action:

1. an explicit name or an unambiguous first-person reference;
2. the card in a photographed tableau, hand, or discard pile;
3. that player's resource, production, TR, or card-resource entries;
4. an owned action card and its used marker or distinctive result; and
5. only then, turn alternation as a consistency check.

An unlabeled change of voice is not actor evidence. Nor is the fact that the workflow currently
expects one player. If a card is visible in one tableau, do not assign it to the other player to
make a payment or turn work. Leave the actor unresolved until the sources decide it.

At the start of each generation, inventory both players' available action cards and standard
conversions. When a ledger delta exactly matches an action that otherwise went unused, investigate
that ordinary action before concluding that the player adjusted state directly. Likewise,
distinguish a contemplated action from a completed one: a spoken proposal needs a payment, card
exit, ledger result, marker, or later state change before it enters the replay.

## Expand app entries before declaring a discrepancy

Translate every evidenced game operation into its individual resource changes, then align that
expanded sequence with the app's observed balances. One app entry may equal the net of several
adjacent operations. Conversely, one game operation may produce several app entries. Compare the
balances, not the number or apparent granularity of rows.

When the expanded replay starts at a recorded balance, accounts for the intervening operations, and
rejoins the next recorded balance with zero residual, that is **perfect agreement**. Do not call the
unrecorded intermediate balances missing transactions, later mistakes, or reconciliations. For
example, an unlogged gain followed by a payment logged at the correspondingly smaller amount may
compact two physical changes into one net change. If the expanded replay and app land on the same
balance, no unexplained state exists there.

More generally, when test and app balances get out of step for a short sequence but quickly rejoin,
there is no mystery and nothing to investigate. The temporary difference is ordinary transaction
grouping. A mystery begins only when the balances get out of step and do not rejoin: a nonzero
residual persists across subsequent observed balances.

For a persistent residual, identify the interval of placements that would have prevented any
observed mismatch. An exact residual equal to a card's effective cost, together with no spoken or
logged payment, is direct evidence that the payment was omitted when the card was played. Later
compressed entries that perfectly rejoin the expanded replay do not move that cause forward. A
later admission such as "I miscalculated" may corroborate an earlier error, but does not date it.

When one explanation uniquely accounts for the first residual and every later balance without an
additional repair, treat it as settled unless stronger evidence contradicts it. Do not ask the user
to investigate arithmetic that the sources already decide. Place any required `exMachina()` at the
most likely supported operation.

## Turn each actual discrepancy into a constrained interval

Implement one generation or authoritative checkpoint at a time. When a checkpoint diverges and the
balances do not quickly rejoin, stop editing the chronology and make a small discrepancy worksheet
for the interval since the last matching source. Record the sourced start state, sourced end state,
every real operation that can change the affected type, the expected net change of each, and the
residual. Reconcile the interval on paper before running the replay again.

Use the state type's natural accounting rule:

| State | Account for |
| --- | --- |
| M€, steel, titanium, plants, energy, and heat | Starting stock, every gain and spend, discounts, resource valuation, rebates, and production. A phone row may aggregate several adjacent operations. |
| Production | Every persistent track change separately from resource gains. A wrong track predicts the same production error in every later generation until corrected. |
| Global parameters and TR | The physical shared track, each player's possibly delayed app copy, and the TR recipient as three separate facts. A shared-track update does not imply that both players gain TR. |
| Project cards | Starting hand plus Research purchases and draws, minus plays, discards, returns, and patent sales. Use photographed hands, tableaus, and discard piles to constrain identities as well as counts. |
| Card resources and actions | The exact holder, every add/remove effect, available action cards, used markers, and repeated actions such as Viron. |
| Tiles and other visible objects | The first photograph where each object appears, its owner and location, placement bonuses, adjacency effects, and any later replacement or removal. |

This accounting catches common causal shapes quickly. A constant resource difference suggests one
missing setup or gain. A difference that grows once per generation suggests one production-track
mistake. A late card shortage calls for a complete hand source-and-sink count, not an anonymous card
injection. Equal and opposite repairs in a short span usually mean the first repair is stale.

Only after expanding ordinary app grouping and finding a nonzero residual should explanations be
ranked. Rank them by how directly they use already-visible game mechanisms:

1. wrong actor, card name, or source anchor;
2. omitted ordinary action, standard conversion, draw, payment, discount, trigger, or placement
   bonus;
3. a wrong rule declaration, task result, or other engine behavior;
4. a physical mistake directly recorded by a source or forced by independent source constraints;
5. genuinely unsupported game behavior.

A viable explanation must account for the exact first residual, predict the relevant later
checkpoints, contradict no stronger source, and avoid a second unrelated repair. Try to falsify it
with the cheapest evidence: inspect the tableau, search the ledger, read the card, count hand exits,
or check an unused action before editing code. Actor reassignment, action reordering, invented plays,
and unexplained hand-card injection are not diagnostic theories; they change the history being
reconstructed. Only primary evidence or an explicit player clarification can justify them.

## Make focused runs answer a question

Before a diagnostic run, assert the sourced states at both ends of the interval. Add partial
`expect()` values to every real operation in the interval that can change the discrepant type,
including a typed zero where a suspected trigger should not fire. Instrument the whole causal chain
before running it, so the first failure is already localized and the next operation is already
guarded; do not add one diagnostic only after each failure.

Change one explanation at a time. If it fails its predicted delta or a later sourced checkpoint,
revert it instead of compensating elsewhere. When a better explanation works, reassess every direct
adjustment in that interval and downstream; passing state can still contain canceling mistakes.

Audit these ordinary causes before retaining a reconciliation:

1. phase transitions, passes, Research, production, world-government, colony, and final phases;
2. actor and action order;
3. card purchases, draws, discards, and research costs;
4. steel, titanium, discounts, rebates, and patent sales;
5. placement bonuses, ocean adjacency, and cross-player effects;
6. triggered, action-card, and repeated-action effects;
7. announced corrections versus persistent physical mistakes; and
8. unsupported or incorrect engine behavior.

## Use expectations as evidence and diagnostics

`TaskResult.expect()` describes a partial net delta for one completed operation. Include a type when
the transcript explicitly says it changed, when an interesting automatic or cross-player effect is
not obvious from the call, or when localizing a residual between authoritative checkpoints.

Do not restate an explicit payment argument, literal `doTask()`, or incidental movement just to
make the expectation exhaustive. Use a nearby absolute assertion when a source gives an absolute
amount. Prefer general types such as `Microbe` and `Animal` unless the destination is ambiguous, and
use typed zeroes when cancellation or the absence of a narrated result matters. Expectations are
net effects, so they may differ from spoken gross effects.

When new expectations explain a mismatch, reassess every existing manual adjustment. Better
localization often proves that a repair should move, shrink, or disappear.

Do not put an expectation on `exMachina()` simply to restate the adjustment. Start at the
reconciliation, identify the affected type, and walk backward over the real gameplay operations
that could have changed it. Put partial expectations for that type on those operations until the
remaining unexplained delta is bounded at its actual source. For example, a plant reconciliation
calls for plant expectations on the preceding card plays, placements, and actions.

## Represent physical mistakes honestly

`exMachina()` means the physical table reached an evidenced state that correct engine play
does not produce. It is not a general test-unblocking tool.

Before retaining one, verify chronology and phase transitions, reconcile the source interval, add
expectations for affected types, distinguish corrected mistakes from persistent ones, and check for
an engine or test defect. Use an explicit relative delta. Put it at the most likely causal action;
do not default to the latest source-compatible point. Keep it as a standalone timeline statement
with a comment naming the source checkpoint that requires it. Never use `sneak`, an absolute
snapshot setter, a catch-all repair, or an unrelated action lambda as a hiding place.

When an `exMachina()` represents an unsolved mystery whose cause or timing remains uncertain, its
adjacent comment must state all three of these conclusions:

1. the earliest position where inserting the adjustment would make every observed balance agree,
   so no mystery would have been reported;
2. the latest position where inserting the same adjustment would still make every observed balance
   agree, again leaving no reportable mystery; and
3. the most likely point and ordinary cause, with the evidence that makes it more likely than the
   rest of the interval.

The earliest position follows the last observed balance before the persistent residual; it cannot
precede a balance where test and app still agree. The latest position precedes the first observed
balance that would otherwise retain the residual. It is not the last moment before the player would
lack resources, violate a requirement, or encounter some other game-rule problem. A candidate
position is outside the possible interval if placing the adjustment there would leave any observed
balance mismatched.

Place the adjustment at the most likely causal point, such as the card play whose payment was
probably omitted or whose discount was probably missed. Do not place it at the latest possible
point merely because that is where a mismatch was first noticed. If one call contains effects that
cannot share one causal interval, split it or document each independently rather than implying one
event caused them all.

Do not hypothesize about a solved mistake. When direct evidence establishes what happened and when,
state that fact plainly beside the adjustment. A sourced correction likewise needs only the known
mistake and correction, not an artificial earliest/latest interval.

If Solarnet lacks a real component, represent only its known sourced consequences at the correct
point, state that support is missing, and record a reusable follow-up in `TODO.md`. Never add a
component-specific test API.

## Treat photographs and endgame as first-class evidence

Inspect every useful photograph at original resolution. Reconcile only what is actually visible:
owned cards and tags, card resources, used markers, milestones and awards, tile kind/owner/location,
global tracks, colonies, and player-board values. Resolve coordinates from the photographed board
and configured map, never by copying another test. Avoid inferring precise hidden state from a
coarse image.

Do not stop after the final action phase. Verify that all players passed, final production occurred
once, and final greenery followed player order. Submit explicit `Ok` tasks after each player's last
greenery before advancing. When sourced, assert remaining plants, full final resources and
production, TR, milestones, awards, greenery, cities, card points, totals, tie-break, and winner.
`Summarizer` can attribute categories but cannot turn a derived result into historical evidence.

## Verification and audit

Before handoff:

1. reread the original sources without consulting an old test;
2. audit every actor against the player ledger or tableau and every operation and comment against
   source order;
3. expand app entries around every suspected error and confirm that zero-residual spans are treated
   as perfect agreement rather than possible later causes;
4. recompute the source-and-sink account for project cards and every type touched by an
   `exMachina()`;
5. trace every assertion to a named source or label it as characterization;
6. justify every `exMachina()` and search once more for an ordinary game mechanism;
7. confirm endgame workflow and scoring, not just compilation;
8. run the focused JVM test, the suite required by [TESTING.md](TESTING.md), formatting, and
   `git diff --check`; and
9. inspect the final diff for game-specific helpers, accidental production changes, and stale
   repairs.

A green replay proves internal consistency. Quality means the test remains traceable to the
physical record and makes every uncertainty, real mistake, and unsupported behavior explicit.
