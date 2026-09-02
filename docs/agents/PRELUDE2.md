# Prelude 2 implementation record

> **Read when:** changing Prelude 2 supported scope, card data, compatibility filtering, active
> Preludes, the failed-Prelude fallback, or a card named below.
>
> **Skip when:** changing the standard Prelude phase with no Prelude 2-specific consequence.
>
> **Status:** current support and primary-source record.

## Source map

- [`prelude2ExpansionBundle.kt`](../../src/common/dev/martianzoo/tfm/canon/prelude2ExpansionBundle.kt)
  — inspect bundle/module composition and source resources.
- [Prelude 2 `cards.pets`](../../src/common/dev/martianzoo/tfm/canon/Prelude2Expansion/cards.pets)
  — search for the specific card Class before changing behavior.
- [`cards-dont-work.json5`](../../src/common/dev/martianzoo/tfm/canon/Prelude2Expansion/cards-dont-work.json5)
  — inspect only when changing the support scope.
- [`Prelude2CardsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/Prelude2CardsTest.kt)
  — read for the focused supported behavior, not as card-data authority.

## Sources and scope

The official English rulebook is stored locally at
`_local/rulebooks/prelude-2.pdf`. It was downloaded from FryxGames and visually checked after
rendering all four pages. Prelude 2 uses the original Prelude rules, keeps active Preludes in play
without treating them as blue cards, filters cards by their required expansion icons, and awards
15 M€ when a revealed Prelude cannot be fully performed.

Card identifiers, names, costs, tags, kinds, and English text were reconciled against
`terraforming-mars/terraforming-mars` commit `7dfdbb353d362f38e6f77e50096c7463e431404c`.
The source set contains 40 cards that do not require Turmoil. Per the selected scope, Preservation
Program is unsupported, and the 11 projects and 3 Preludes whose manifest compatibility includes
Turmoil are omitted entirely.

[Jacob Fryxelius's Recession ruling](https://boardgamegeek.com/thread/3334230/article/44565901#44565901)
confirms that its player performs both losses: Mons Insurance pays each other victim 3 M€ for the
resource loss and another 3 M€ for the production decrease. Each opponent loses up to 5 M€, but
must be able to decrease M€ production one step; an opponent already at the production floor makes
Recession unplayable.

## Current scope

The executable manifest contains 38 cards: 5 corporations, 12 projects, and 21 Preludes. The one
remaining non-Turmoil definition stays in `cards-dont-work.json5` so its accurate data and specific
blocker remain reviewable without loading incorrect behavior:

- `P78` L1 Trade Terminal needs a clean way to select up to three distinct resource-bearing cards.

Spire uses the shared card-resource payment protocol: starting a standard project offers payment
from Spire, and each resulting payment signal removes 2 M€ of debt. Other payments never receive
that offer.

Prelude and Prelude 2 are independent modules. Each owns the shared Prelude setup and phase protocol,
so selecting either activates it once; selecting both changes only the eligible card pool. The
standard Prelude phase already models the rulebook's failed-Prelude fallback as discard plus 15 M€.
Active Preludes compose with the existing action-card machinery, and active/effect-only Preludes
may naturally omit an immediate instruction.
Comments ending in `[F]` identify cards whose hidden filtered draw or reveal result must be supplied
by a follow-mode client. Their canonical card data now retains that printed procedure in
`CARDS`; executable follow-mode declarations neutralize it to the former client-supplied result.

The focused card tests cover the genuinely new behavior, including World Government option
combinations, production floors, all-colony-track advancement, minimum
tag metrics, the derived `EventTag`, Recession losses authored by its player, and the two follow-mode
Venus Orbital Survey outcomes.
