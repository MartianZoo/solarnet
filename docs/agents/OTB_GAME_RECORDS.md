# Reconstructing recorded over-the-board games

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

This procedure covers live physical games reconstructed from mixed evidence such as an audio
transcript, player-board resource logs, and photographs. It supplements the shared whole-game rules
in [TESTING.md](TESTING.md). Use [HEROKUAPP_GAME_LOGS.md](HEROKUAPP_GAME_LOGS.md) instead for games
whose primary record is an application log.

## Preserve and inventory the original evidence

Before reading an existing dated fixture, Git history, commit message, derived reconstruction, or
another game record, inventory the original sources in `_local/GameYYYYMMDD/`. Reconstruct the game
from those sources independently; old fixtures and plans may teach current Solarnet syntax but are
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
  bookkeeping, mishear card names, or faithfully record a player's mistaken arithmetic.
- A player-board log proves stable personal resources and production at its recorded checkpoints.
  Fine-grained entry grouping may reflect data-entry timing rather than action chronology.
- A board photograph proves visible point-in-time state: board tiles, tracks, colonies, cards,
  markers, and physical mistakes that remained on the table. Determine its exact timeline position
  from contents and timestamps rather than filename order alone.
- A raw app-log screenshot can disambiguate a ledger entry, but a machine-readable ledger is easier
  to audit when both represent the same observations.
- Repository code and other fixtures explain APIs and engine behavior only. Derived ledgers,
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

- all six resources and all six production values at every sourced generation boundary, after
  automatic transition work and before the first `buyCards()`;
- `assertSidebar()` at the same boundary when the evidence records global state;
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
state change at its exact boundary when possible; otherwise make the smallest chronology distortion
and say exactly what moved and why. Record the missing general mechanism in `TODO.md`.

Follow the current full-game fixture style rather than copying an old revision:

- define `override val config = GameConfig(...)` with player-name arguments;
- resolve gameplay objects with `game.tfm(Player.PLAYERn)`;
- import and pass card-name constants rather than string card names;
- use named `buyCards()`/`discard()` calls when the source identifies cards, opting into
  `CardTrackingFullGameTest` only when the evidence can sustain exact hand tracking;
- use `player.turn { ... }` for ordinary multiplayer turns and put both Preludes in one turn block;
- once other players have passed, keep the remaining player's actions through `pass()` in one block
  when the workflow permits; and
- keep literal source comments immediately beside the operation they prove.

Transcript comments may normalize filler and repetition while retaining gameplay-relevant
personality, uncertainty, corrections, and admitted mistakes. If a spoken consequence is provably
wrong, preserve the useful quote with `[sic]` and assert the stronger evidence separately. Put a
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

Do not restate an explicit payment argument, literal `doTask()`, or incidental movement merely to
make the expectation exhaustive. Use a nearby absolute assertion when a source gives an absolute
amount. Prefer general types such as `Microbe` and `Animal` unless the destination is ambiguous, and
use typed zeroes when cancellation or the absence of a narrated result matters. Expectations are
net effects, so they may differ from spoken gross effects.

When new expectations explain a mismatch, reassess every existing manual adjustment. Better
localization often proves that a repair should move, shrink, or disappear.

## Represent physical mistakes honestly

`exMachina()` means the physical table reached an evidenced state that ordinary correct engine play
does not produce. It is not a general test-unblocking tool.

Before retaining one, verify chronology and phase transitions, reconcile the source interval, add
expectations for affected types, distinguish corrected mistakes from persistent ones, and check for
an engine or fixture defect. Use an explicit relative delta. Put it at the causal action only when
the evidence proves that placement; otherwise place it as late as the next sourced checkpoint
allows. Keep it as a standalone timeline statement with a comment naming the source boundary that
requires it. Never use `sneak`, an absolute snapshot setter, a catch-all repair, or an unrelated
action lambda as a hiding place.

If Solarnet lacks a real component, represent only its known sourced consequences at the correct
boundary, state that support is missing, and record a reusable follow-up in `TODO.md`. Never add a
component-specific fixture API.

## Treat photographs and endgame as first-class evidence

Inspect every useful photograph at original resolution. Reconcile only what is actually visible:
owned cards and tags, card resources, used markers, milestones and awards, tile kind/owner/location,
global tracks, colonies, and player-board values. Resolve coordinates from the photographed board
and configured map, never by copying another fixture. Avoid inferring precise hidden state from a
coarse image.

Do not stop after the final action phase. Verify that all players passed, final production occurred
once, and final greenery followed player order. Submit explicit `Ok` tasks after each player's last
greenery before advancing. When sourced, assert remaining plants, full final resources and
production, TR, milestones, awards, greenery, cities, card points, totals, tie-break, and winner.
`Summarizer` can attribute categories but cannot turn a derived result into historical evidence.

## Verification and audit

Before handoff:

1. reread the original sources without consulting an old fixture;
2. audit operations and comments against transcript order;
3. trace every assertion to a named source or label it as characterization;
4. justify every `exMachina()` and search once more for a natural explanation;
5. confirm endgame workflow and scoring, not merely compilation;
6. run the focused JVM test, the suite required by [TESTING.md](TESTING.md), formatting, and
   `git diff --check`; and
7. inspect the final diff for game-specific helpers, accidental production changes, and stale
   repairs.

A green replay proves internal consistency. Quality means the fixture remains traceable to the
physical record and makes every uncertainty, real mistake, and unsupported behavior explicit.
