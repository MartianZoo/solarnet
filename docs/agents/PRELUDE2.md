# Prelude 2 implementation record

> **Agent record:** This is implementation evidence and scope tracking, not player documentation.

## Sources and scope

The official English rulebook is stored locally at
`_local/TM_PRELUDE2_RULES_ENGi.pdf`. It was downloaded from FryxGames and visually checked after
rendering all four pages. Prelude 2 uses the original Prelude rules, keeps active Preludes in play
without treating them as blue cards, filters cards by their required expansion icons, and awards
15 M€ when a revealed Prelude cannot be fully performed.

Card identifiers, names, costs, tags, kinds, and English text were reconciled against
`terraforming-mars/terraforming-mars` commit `7dfdbb353d362f38e6f77e50096c7463e431404c`.
The English data file contains all 40 cards that do not require Turmoil. Per the selected scope, the
11 projects and 3 Preludes whose manifest compatibility includes Turmoil are omitted entirely.

## Current boundary

The executable manifest contains 30 cards: 2 corporations, 9 projects, and 19 Preludes. The
remaining 10 non-Turmoil definitions stay in `cards-dont-work.json5` so their accurate data and
specific blockers remain reviewable without loading incorrect behavior:

- `P48` Early Colonization needs an operation over every colony track.
- `P52` Industrial Complex needs production floors across all six resources.
- `P59` Recession needs one effect applied independently to every opponent.
- `P69` Cloud Tourism needs the minimum of two tag counts.
- `P78` L1 Trade Terminal needs three distinct resource-holding cards.
- `P88` Venus Orbital Survey needs reveal-and-conditionally-buy deck operations.
- `P89` Venus Shuttles needs an action cost reduced dynamically by Venus tags.
- `PC01` Nirgal Enterprises needs deferred milestone and award affordability.
- `PC03` Sagitta Frontier Services needs its printed event icon counted with tags.
- `PC05` Spire needs event-aware tag counts and standard-project-only payment.

Prelude 2 implies the original Prelude module. The ordinary Prelude phase already models the
rulebook's failed-Prelude fallback as discard plus 15 M€. Active Preludes compose with the existing
action-card machinery. `Prelude2NoImmediate` is a hidden signal for active/effect-only Preludes
whose printed immediate region is empty.

The focused card tests cover the genuinely new behavior: Wild-tag Preludes, active Prelude play,
once-per-action reset, first-TR cancellation per action phase, World Government terraforming by
Engine, and EcoTec's rewards for its own starting tags.
