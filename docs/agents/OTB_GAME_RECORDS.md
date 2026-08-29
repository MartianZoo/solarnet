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
  Fine-grained entry grouping may reflect data-entry timing rather than action chronology.
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

## Work checkpoint to checkpoint

Implement one generation or authoritative checkpoint at a time and run the focused JVM test after
each span. Audit only the interval since the previous checkpoint when values diverge:

1. phase transitions, passes, Research, production, world-government, colony, and final phases;
2. actor and action order;
3. card purchases and research costs;
4. steel, titanium, card resources, discounts, rebates, and patent sales;
5. placement bonuses, ocean adjacency, and cross-player effects;
6. triggered and action-card effects;
7. announced corrections versus persistent physical mistakes; and
8. unsupported or incorrect engine behavior.

A constant discrepancy usually points to one missing setup or production fact. Equal and opposite
repairs at neighboring checkpoints usually mean an earlier reconciliation has become stale. Do not
accumulate guesses until a later snapshot happens to pass.

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
an engine or test defect. Use an explicit relative delta. Put it at the causal action only when
the evidence proves that placement; otherwise place it as late as the next sourced checkpoint
allows. Keep it as a standalone timeline statement with a comment naming the source checkpoint that
requires it. Never use `sneak`, an absolute snapshot setter, a catch-all repair, or an unrelated
action lambda as a hiding place.

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
2. audit operations and comments against transcript order;
3. trace every assertion to a named source or label it as characterization;
4. justify every `exMachina()` and search once more for a natural explanation;
5. confirm endgame workflow and scoring, not just compilation;
6. run the focused JVM test, the suite required by [TESTING.md](TESTING.md), formatting, and
   `git diff --check`; and
7. inspect the final diff for game-specific helpers, accidental production changes, and stale
   repairs.

A green replay proves internal consistency. Quality means the test remains traceable to the
physical record and makes every uncertainty, real mistake, and unsupported behavior explicit.
