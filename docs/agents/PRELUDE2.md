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
The source set contains 40 cards that do not require Turmoil. Per the selected scope, Preservation
Program is unsupported, and the 11 projects and 3 Preludes whose manifest compatibility includes
Turmoil are omitted entirely.

## Current boundary

The executable manifest contains 38 cards: 5 corporations, 12 projects, and 21 Preludes. The one
remaining non-Turmoil definition stays in `cards-dont-work.json5` so its accurate data and specific
blocker remain reviewable without loading incorrect behavior:

- `P78` L1 Trade Terminal needs a clean way to select up to three distinct resource-bearing cards.

Spire uses the shared card-resource payment protocol: starting a standard project offers payment
from Spire, and each resulting payment signal removes 2 M€ of debt. Other payments never receive
that offer.

Prelude and Prelude 2 are independent modules. Each owns the shared Prelude setup and phase protocol,
so selecting either activates it once; selecting both changes only the eligible card pool. The
ordinary Prelude phase already models the rulebook's failed-Prelude fallback as discard plus 15 M€.
Active Preludes compose with the existing action-card machinery, and active/effect-only Preludes
may naturally omit an immediate instruction.
Identifiers ending in `F` mark follow-mode cards whose hidden filtered draw or reveal result must be
supplied by the client.

The focused card tests cover the genuinely new behavior, including World Government option
combinations, production floors, all-colony-track advancement, minimum
tag metrics, the derived `EventTag`, opponent-authored Recession losses, and the two follow-mode
Venus Orbital Survey outcomes.
