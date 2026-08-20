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

The executable manifest contains 37 cards: 3 corporations, 12 projects, and 22 Preludes. The three
remaining non-Turmoil definitions stay in `cards-dont-work.json5` so their accurate data and
specific blockers remain reviewable without loading incorrect behavior:

- `PC01` Nirgal Enterprises needs milestone and award payments to bypass the standard actions'
  up-front affordability checks.
- `PC05` Spire needs its science resources to rewrite payment for standard projects only.
- `P78` L1 Trade Terminal needs a clean way to select up to three distinct resource-bearing cards.

Prelude 2 implies the original Prelude module. The ordinary Prelude phase already models the
rulebook's failed-Prelude fallback as discard plus 15 M€. Active Preludes compose with the existing
action-card machinery, and active/effect-only Preludes may naturally omit an immediate instruction.
Identifiers ending in `F` mark follow-mode cards whose hidden filtered draw or reveal result must be
supplied by the client.

The focused card tests cover the genuinely new behavior, including multi-step TR cancellation,
World Government option combinations, production floors, all-colony-track advancement, minimum
tag metrics, event-icon tag counting, opponent-authored Recession losses, and the two follow-mode
Venus Orbital Survey outcomes.
