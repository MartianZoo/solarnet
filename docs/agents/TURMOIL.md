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
- [`PartyDistance.kt`](../../src/common/dev/martianzoo/tfm/canon/PartyDistance.kt) computes forward
  distance through the party relation for government-formation tie breaking.
- [`TurmoilExpansion/cards.pets`](../../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/cards.pets)
  owns its four supported corporations, all sixteen project cards, and 31 base events.
- [`PromoCardPack/cards.pets`](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.pets)
  owns the five promotional events associated with Venus Next or Colonies. Their semantic
  references make premise projection require the promo pack, Turmoil, and the applicable companion
  expansion without a special module.
- [`turmoilExpansionBundle.kt`](../../src/common/dev/martianzoo/tfm/canon/turmoilExpansionBundle.kt)
  is the convention-backed bundle and registers the party-distance metric.

Test ownership:

- [`TurmoilRulesTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilRulesTest.kt)
  covers setup, lobbying, delegate capacity, ranks, and influence.
- [`TurmoilGovernmentTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilGovernmentTest.kt)
  covers ruling bonuses, chairman succession, delegate return, dominance, and Lobby refill.
- [`TurmoilPoliciesTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilPoliciesTest.kt)
  covers every ruling policy and policy lifetime.
- [`TurmoilEventsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilEventsTest.kt)
  covers all 36 event effects, promo and companion-expansion projection, FAQ edge cases, ties, and
  solo formulas.
- [`TurmoilSolarPhaseTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/TurmoilSolarPhaseTest.kt)
  covers ordered Solar integration and unfinished event choices.
- [`TurmoilProjectCardsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/TurmoilProjectCardsTest.kt)
  covers the project cards whose delegate, influence, and triggered effects need focused evidence.
- [`SyntheticPlasmaCurrentTest.kt`](../../test/jvm/dev/martianzoo/tfm/tests/replays/SyntheticPlasmaCurrentTest.kt)
  replays a preserved twelve-generation solo game with the canonical project cards.

## Political components

`TurmoilExpansion` queues creation of its parties, actions, and Neutral owner as ordinary Module
work. During Setup it creates one `TurmoilPlayer` and one Lobby-availability marker for each player.
Neutral begins with the chair and the two printed setup delegates introduced by the first Coming and
Distant events. Greens are the initial ruling party.

`Delegate<Owner>` is the common supertype of `PartyDelegate` and `Chairman`. Only placed delegates
are components; there is no second representation for off-board delegates, just as there is none for
unplaced tiles. Each owner's available supply is therefore derived from a limit on placed delegates.

Neutral is itself the owner, so it states its limit directly as the invariant
`MAX 14 Delegate<This>`; the chairman is one of those fourteen while occupying the chair. A player's
seven cannot be written the same way, and the `TurmoilPlayer` component exists to hold the trigger
that enforces it. Contextual `Owner` is substituted only where it is written bare inside an effect:
an invariant on `Delegate` sees the abstract `Owner` class and so counts every owner's delegates at
once, and a refined `Owner(NOT Neutral)` is a Type variable rather than the bearer's owner. A
trigger on a per-player component is the only place the bearer's own owner is available.

`LobbyActionAvailable` separately records whether the free Lobby placement remains available and
removes itself when its owner places a seventh delegate. Using that placement also consumes the
marker, and returning a delegate does not restore it. Any player placement beyond seven is
impossible, and the neutral invariant makes a Global Event placement an AMAP no-op once all fourteen
neutral delegates are placed.

`PartyDelegate<Party, Owner>` records committee membership. `PartyLeader<Party, Owner>`,
`Dominant<Party>`, `Ruling<Party>`, and `Chairman<Owner>` are separate roles:

- The incumbent leader wins a tied delegate count. A strictly larger delegation replaces it.
- The incumbent dominant party wins a tied count during ordinary delegate movement.
- After government forms, tied dominance follows the committee's party order from the new ruling
  party.
- Neutral uses the same owner model as players, but only a player taking the chair receives the
  rating increase, and only players contribute influence.

Turmoil creates the six Parties in committee order. Each arrival after the first creates a permanent
`AfterParty<Party, Party>` edge from its predecessor, and the last Party also closes the ring. Each
Party permits at most one incoming and outgoing edge. `PartyDistance<Party, Party>` is a passive
custom metric that walks those declared edges and returns a forward distance from zero through
five. Government formation ranks delegate count and then `PartyDistance<Party, This>`: measuring
back to the outgoing Party gives its immediate successor the greatest distance. Ordinary delegate
movement instead ranks delegate count and the
incumbent `Dominant` role. Both cases use the same ordinary rank and transmutation mechanisms, and
no temporary priority state is created.

`PartyLeader` ends when its owner's last delegate leaves that party, then reruns the ordinary leader
rank over the remaining delegations. No caller has to retract or replace the role separately.

## Influence and government

`Terraformer26` is supplied by the Turmoil bundle; other Terraformer versions remain selectable.
`Lobbyist` counts owned party delegates and the chairman, without double-counting party-leader roles.
The modular `Politician` award refreshes influence from the final political position during its
ordinary award measurement. It does not run a Global Event or form a new government.

Influence is a fresh, generational snapshot measured immediately before the Current event. A player
can receive at most one each from:

- holding the chair;
- leading the dominant party;
- having one or more non-leader delegates in the dominant party.

Each eligible delegate attempts an AMAP gain of the capped `DelegateInfluence` subtype. This is the
settled multiplicity model: all genuine providers respond, while the invariant consolidates them
to one existential contribution. AMAP is not used to conceal a missing player owner.

`FormGovernment` is supplied by the dominant party and performs the complete ordered operation:

1. Move the ruling marker to that party and apply its one-time bonus.
2. Return the old chairman and the ruling party's non-leader delegates to their owners' available
   supply by removing their placed components.
3. Move the ruling party leader into the chair, which ends that leader role.
4. Grant one rating if the new chairman is a player, select the next dominant party in committee
   order, and restore free lobbying for players with fewer than seven placed delegates.

`ApplyRulingBonus` stays a separate signal rather than triggering on the `Ruling` marker, because
Setup places `Ruling<Greens>` without paying the Greens bonus.

Ruling bonuses count only the current player's owned icons or production. Mars First, Scientists,
Unity, Greens, and Kelvinists pay from Building, Science, planetary, bio, and heat-production
counts.
Reds awards the lowest-rating player with friendly ties in multiplayer, or the player at rating 20
or less in solo play.

## Ruling policies

A policy component exists only during an Action phase in which its party rules. Its `ActionPhase`
dependency removes it when that phase ends; Unity's extra titanium-value components in turn depend
on its policy:

- Mars First: a player placing a tile on Mars gains one steel.
- Scientists: pay 10 M€ to draw three cards, once per generation.
- Unity: titanium is worth one additional M€ for every player.
- Greens: a player placing greenery gains 4 M€.
- Reds: every player-attributed rating increase costs 3 M€ per step.
- Kelvinists: pay 10 M€ for one heat and one energy production.

Scientists and Kelvinists use ordinary action slots. The permanent `UseTurmoilPolicyAction` doorway
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

`TurmoilSolarPhase` orders this work after the active Venus and Colonies Solar phases. The temporary
operation inside it is a completion latch for event choices: it keeps government formation from
racing consequences created by the current event; a pending task is enough to hold it open, so the
choices themselves need no `Barrier`. `Current`, `Coming`, and `Distant` are typed positions with at
most one occupant, each keyed by `Class<GlobalEvent>`; each live event also limits its exact class to
one position across the three. The card class is the key because
`ChangingTimes` must name one event on both sides of a transmutation, and only an `EACH` selector
spelling — here `Class<GlobalEvent>` — binds a concrete Type inside its body. Making the position a
dependency of the event instead would leave the destination of `Coming FROM Distant` abstract.
The event component itself is created once, at reveal, and discarded when it leaves Current, so its
printed reveal-corner delegate is simply a `This::` effect. Reveal barriers request one concrete
catalog event. For now, callers explicitly complete those Admin tasks with the event supplied by
their shuffled deck or source record. This is a temporary integration compromise, not a game
decision assigned to Admin. The selected direction is an installable Admin autoexecution policy
that pulls the next exact event from an ordered list.

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
The bounded `PartyDistance` metric supplies the one graph operation Pets lacks. The expansion
requires no Turmoil-specific instruction, workflow branch, or persistent duplicate of political or
event state.

Three habits are worth naming, because each one removed a class that looked necessary:

- A named Signal earns its keep only when it marks a moment the game itself has — `FormGovernment`,
  `ApplyRulingBonus`, `ResolveGlobalEvent`. A Signal that exists only to carry a concrete Type into
  a place that could have named it is redundant: an `EACH` selector already binds its own spelling
  in the body, so `EACH ResourceCard<Anyone> { CardResource<ResourceCard<Anyone>> }` needs no helper.
- An owned Signal is the way to route a choice to one player, because `EACH` cannot; but it needs to
  be a `Barrier` only if something must wait on more than the resulting task. The Solar operation is
  held open by the pending task itself.
- A printed threshold belongs in the payout metric, not in a ladder of gated cases. Diversity is
  `10 MC / (9 (Class<Tag>(HAS Tag<Owner>) OR Influence) MAX 1)`; a gate inside an `EACH` body would
  fail the branch rather than pay nothing.

Two pairs of similar declarations remain separate for language reasons. `GePartyDistant` and
`GePartyCurrent` give the two printed party arrows distinct dependency paths and spellings; two
bare `Party` dependencies on `GlobalEvent` would be independent but indistinguishable in its body.
`RevealComingEvent` bootstraps setup by becoming `RevealDistantEvent` only after its event choice
completes, while each later `RevealDistantEvent` simply finishes. A position-parameterized reveal
would admit the unsupported Current position and still need these position-specific behaviors.

`TurmoilExpansion` declares no `HAS Class<...>` activation list. Every class it needs is reached by
a gain, which is a hard reference that projection closure already activates
([OPTIONS.md](OPTIONS.md#projection-closure)).
