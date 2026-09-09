# Turmoil implementation record

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing Turmoil politics, policies, Solar sequencing, or Global Events. Read only
> the relevant section and then inspect its named source and tests.
>
> **Status:** implemented. The linked Pets and functional tests are authoritative; this document
> records the stable model and source interpretation.

## Source and implementation map

Primary rule evidence:

- [Local Turmoil rulebook](../../_local/rulebooks/turmoil.pdf)
- [Local FAQ 1.8](../../_local/faq/terraforming-mars-faq-v1.8.pdf)
- [Official Turmoil rules](https://fryxgames.se/wp-content/uploads/2023/07/TM_TURMOIL_ENG_RULESi.pdf)
- [Global Event data](https://docs.google.com/spreadsheets/d/1w-n2oUlVg_YBiMzsRjWEVBg01nkqwt5CCDP2M0mRrFg/edit?gid=0#gid=0)

Owning implementation:

- [`TurmoilExpansion/classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/classes.pets)
  owns setup, delegates, political state, influence, government, policies, event movement, and the
  Solar operation.
- [`TurmoilExpansion/cards.pets`](../../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/cards.pets)
  owns the expansion's supported cards and 31 base events.
- [`PromoCardPack/cards.pets`](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.pets)
  owns the five promotional events associated with Venus Next or Colonies. Their semantic
  references make premise projection require the promo pack, Turmoil, and the applicable companion
  expansion without a special module.
- [`turmoilExpansionBundle.kt`](../../src/common/dev/martianzoo/tfm/canon/turmoilExpansionBundle.kt)
  is a convention-backed bundle with no custom runtime behavior.

Test ownership:

- [`TurmoilRulesTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilRulesTest.kt)
  covers setup, lobbying, finite reserves, ranks, and influence.
- [`TurmoilGovernmentTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilGovernmentTest.kt)
  covers ruling bonuses, chairman succession, delegate return, dominance, and Lobby refill.
- [`TurmoilPoliciesTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilPoliciesTest.kt)
  covers every ruling policy and policy lifetime.
- [`TurmoilEventsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilEventsTest.kt)
  covers all 36 event effects, promo and companion-expansion projection, FAQ edge cases, ties, and
  solo formulas.
- [`TurmoilSolarPhaseTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilSolarPhaseTest.kt)
  covers ordered Solar integration and unfinished event choices.
- [`SyntheticPlasmaCurrentTest.kt`](../../test/jvm/dev/martianzoo/tfm/tests/replays/SyntheticPlasmaCurrentTest.kt)
  replays a preserved twelve-generation solo game through outward gameplay APIs.

## Political components

`TurmoilExpansion` creates one `TurmoilPlayer`, one Lobby delegate, and six reserve delegates for
each player. Neutral begins with thirteen reserve delegates, the chair, and the two printed setup
delegates introduced by the first Coming and Distant events. Greens are the initial ruling party.
Delegates are finite components: sending or returning one always transmutates a Lobby, reserve,
party, or chairman component rather than creating an unlimited marker.

`PartyDelegate<Party, Owner>` records committee membership. `PartyLeader<Party, Owner>`,
`Dominant<Party>`, `Ruling<Party>`, and `Chairman<Owner>` are separate roles:

- The incumbent leader wins a tied delegate count. A strictly larger delegation replaces it.
- The incumbent dominant party wins a tied count during ordinary delegate movement.
- After government forms, tied dominance is selected clockwise from the new ruling party.
- Neutral uses the same owner model as players, but only player-owned leaders receive the chairman
  rating increase and can contribute player influence.

Rank metrics include the incumbent role or a temporary clockwise priority. This lets ordinary
friendly ranking express both tie rules without custom Kotlin or a mirrored numeric state.

## Influence and government

Influence is a fresh, generational snapshot measured immediately before the Current event. A player
can receive at most one each from:

- holding the chair;
- leading the dominant party;
- having one or more non-leader delegates in the dominant party.

Each eligible delegate attempts an AMAP gain of the capped `DelegateInfluence` subtype. This is the
settled multiplicity model: all genuine providers respond, while the invariant consolidates them
to one existential contribution. AMAP is not used to conceal a missing player owner.

`FormGovernment` is supplied by the dominant party and performs the complete ordered operation:

1. Make that party ruling and apply its one-time bonus.
2. Return the old chairman and the ruling party's non-leader delegates to their owners' reserves.
3. Move the ruling party leader into the chair; grant one rating if that owner is a player.
4. Remove its leader role, select the next dominant party clockwise, and refill empty Lobby seats
   from finite player reserves.

Ruling bonuses count only the current player's owned icons or production. Mars First, Scientists,
Unity, Greens, and Kelvinists pay from Building, Science, planetary, bio, and heat-production
counts.
Reds awards the lowest-rating player with friendly ties in multiplayer, or the player at rating 20
or less in solo play.

## Ruling policies

A policy component exists only during an Action phase in which its party rules. It owns its effect
and removes itself when that phase ends:

- Mars First: a player placing a tile on Mars gains one steel.
- Scientists: pay 10 M€ to draw three cards, once per generation.
- Unity: titanium is worth one additional M€ for every player.
- Greens: a player placing greenery gains 4 M€.
- Reds: every player-attributed rating increase costs 3 M€ per step.
- Kelvinists: pay 10 M€ for one heat and one energy production.

Scientists and Kelvinists use ordinary action slots. The permanent `UseTurmoilPolicySA` doorway
delegates to whichever of those temporary policies is active; it does not duplicate either cost or
effect. Making the policy itself a `StandardAction` would incorrectly require it to remain present
after the Action phase.

## Solar sequence and event positions

The ordinary Solar workflow performs World Government Terraforming before the Turmoil operation.
`TurmoilSolarOperation` then performs:

1. Every player loses one rating.
2. Influence is measured and the Current event resolves.
3. Government forms after all event choices and consequences settle.
4. Changing Times removes Current, moves Coming to Current and Distant to Coming, and requests the
   next concrete Distant event.

The temporary operation is a completion latch for event choices; it is not a new workflow phase.
`Current`, `Coming`, and `Distant` are typed positions with at most one occupant. A concrete event
owns its printed delegates and effect while its position component changes. Reveal barriers request
one concrete catalog event. For now, callers explicitly complete those Admin tasks with the event
supplied by their shuffled deck or source record. This is a temporary integration compromise, not a
game decision assigned to Admin. The selected direction is an installable Admin autoexecution
policy that pulls the next exact event from an ordered list.

Admin-authored global-parameter changes grant no rating or player placement bonuses. If Admin raises
temperature through 0°C, `AdminOceanPlacement` gives the required ocean-placement choice to the
first player while retaining Admin attribution. This is a general base-game rule used by both
World Government Terraforming and Turmoil events.

## Global Event catalog

The 31 base cards are:

Aquifer Released by Public Council, Asteroid Mining, Celebrity Leaders, Diversity, Dry Deserts,
Eco Sabotage, Election, Generous Funding, Global Dust Storm, Homeworld Support, Improved Energy
Templates, Interplanetary Trade, Miners on Strike, Mud Slides, Pandemic, Paradigm Breakdown,
Productivity, Red Influence, Revolution, Riots, Sabotage, Scientific Community, Snow Cover, Solar
Flare, Solarnet Shutdown, Spin-Off Products, Sponsored Projects, Strong Society, Successful
Organisms, Volcanic Eruptions, and War on Earth.

Promotional content is selected by `PromoCardPack` and projected only when its observed domains
exist:

- Venus Next: Venus Infrastructure.
- Colonies: Jovian Tax Rights and Microgravity Health Problems.
- Venus Next plus Colonies: Cloud Societies and Corrosive Rain.

Event arithmetic follows the FAQ consistently. A printed count is capped before influence is added
to a gain or subtracted from a penalty. Influence uses the same printed rate as the main gain.
Forced losses use AMAP when a player may have fewer resources than the computed loss. Owned tags,
tiles, colonies, cards, production, and card resources never count another player's holdings; Wild
tags are not treated as printed tags.

The less mechanical FAQ cases have explicit functional coverage: neutral ranking participation and
friendly ties; solo Election and Revolution; Mud Slides coastal-tile uniqueness; Sabotage's
independent production losses; Admin temperature and ocean decisions; independent Dry Deserts
resource choices; Sponsored Projects and Cloud Societies including empty compatible cards; and
Corrosive Rain requiring two floaters on one card before offering that alternative.

## Modeling rule

Keep behavior on the component that makes it exist: parties own ruling bonuses, policies own their
temporary capabilities, event cards own their delegates and effects, and event positions own only
occupancy. Prefer existing Pets effects, metrics, ranks, signals, barriers, and module projection.
The implemented expansion requires no Turmoil-specific Kotlin instruction, metric, workflow branch,
or persistent duplicate of political or event state.
