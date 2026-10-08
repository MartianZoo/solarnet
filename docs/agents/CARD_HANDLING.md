# Card handling and external tracking

> **NOTE:** This document is maintained for coding agents. A human did not write it.

> **Read when:** changing card backs, card play, draws, searches, purchases, card-tracked replays,
> or the card-tracking game-playing API.
>
> **Status:** current anonymous count-and-location model and selected external-tracking direction.
> The production card-tracking API remains to be designed.

## World model

The engine follows externally supplied Terraforming Mars card decisions. Before play, it records
generic card backs by Player and location, never by printed identity:

- `ProjectCard<Hand>` is the count in that Player's hand; `Hand` is the default card location in
  instructions and queries;
- `CorporationCard<Hand>` and `PreludeCard<Hand>` are analogous setup/phase counts;
- `BeginnerCard<Hand>` is the separate back used only by the beginner variant;
- `Selecting` temporarily holds anonymous backs for project-card look-and-keep effects and the
  modeled corporation and Prelude offers. Retained project cards move to `Hand`; selected Prelude
  and Merger corporation cards are played directly from `Selecting`. The effect removes the rest;
- `Revealed` temporarily holds an already owned card for Public Plans, or one anonymous card
  revealed from outside the World for Asteroid Deflection System and Search for Life;
- a concrete `CardFront<Class<CardBack>>` is an exact face in play; and
- `PlayedEvent<Class<CardFront>>` preserves the exact face of a completed Event because published
  scoring and recovery rules query it.

The modeled selection and reveal effects explicitly empty their temporary locations. Retained
starting project cards are the deliberate exception: they rest in `Selecting` after setup until
`BuySelectedCards` moves them during that Player's Corporation-phase turn. Otherwise, a card left
in `Selecting` after its selection is resolved is always a bug. The selected invariant is zero
`Selecting` backs for a Player whenever that Player has no active selection or pending setup
purchase. Setup and Prelude selections count as active selections even though they occur outside
action-phase turns. No general check enforces this invariant yet; implementing one requires a
reliable selection-completion point. The existing `MustCleanUp` check runs only for operations that
declare themselves complete. Do not treat other leftover backs as a valid resting state or silently
discard them to conceal a missing decision.

Fixed-size project-card purchase offers enter `Selecting` first. Searches intentionally have no
selection pool: the matching card enters the hand directly, and skipped cards have no relevant
count or movement.
There is no deck or discard Component, hidden face, physical-copy identity, shuffle state, or
dealer policy. Playing a card consumes one generic back from its stated location and creates the
concrete face supplied by the caller. Solarnet trusts that declaration.

## External offer procedures

Prelude plays use `PlayOrFizzle`: play the chosen face, or discard its anonymous back, record an
`Audit`, and gain 15 M€. The caller verifies that the selected Prelude is unplayable. This same
signal serves the Prelude phase and additional Prelude plays granted by cards. Offered and rejected
cards in the fixed Valley Trust, New Partner, WG Project, and Merger offers are counted in
`Selecting`; their chosen back is played directly from there and the rejected backs are removed
without names.
Each Player first commits the setup path. The standard path then counts both offered corporations
and ten project cards in `Selecting`; the chosen corporation enters `Hand`, the other is discarded,
and unwanted project cards are discarded concurrently. The beginner path creates only
`BeginnerCard`. Prelude's setup rule reacts to either committed path and owns its deal and discard.
Corporation phase plays the retained corporation or beginner card from `Hand`, then buys every
project card still in `Selecting`.
Gameplay callers choose the card face without repeating its location. `PlayCard` has no location
default: `doTask` intersects the caller's choice with the pending task, which supplies the authorized
source (`Hand` for ordinary plays, `Selecting` for direct offered plays). An explicit conflicting
source is rejected even when that location contains another card.

An ordinary draw adds `ProjectCard<Hand>` directly. Fixed-size project-card offers, including buys,
gain the full offer as `ProjectCard<Selecting>`. Look-and-keep effects move only retained backs to
`Hand` and remove the remainder. For a buy, the Player removes unwanted backs, then
`BuySelectedCards` converts every remaining selected back into a `BuyCard` payment request. Settling
the purchase billing moves each paid request to `Hand`. Zero buys leave no selected backs. Neither
the World nor the replay ledger names rejected cards. Searches create only the matching hand card;
there is no count of cards searched past. `SearchForCard<CardFilter>` records the externally
verified criterion as a transient audited event. `TagFilter`, `NoTagsFilter`, and
`ReferenceFilter` cover the supported tag, no-printed-tag, and reference criteria. The selected
card's printed properties do not become hand state. One generic `TagFilter` Class serves every
tag, with its singleton Components supplied by the rule Module. Its `Class<Tag>` dependency is the
criterion; it has no `criteria` property because Requirement properties do not currently
specialize named header variables.

Venus Orbital Survey offers two anonymous backs in `Selecting`. The caller may keep each
Venus-tagged card for free through the externally asserted `TakeSelectedCard<TagFilter>` signal,
discard any unwanted remainder, and buy every back still selected. The World records the counts
and movements but cannot inspect those cards' printed tags. Asteroid Deflection System and Search
for Life each count one anonymous card entering and leaving `Revealed`. Each card's action supplies
its filter and resource-card destination to `ClaimCardReward<CardFilter, ResourceCard>`. The claim
derives the resource type from the destination card and scales both the reward and `Audit` by the
revealed-card count, currently one. The caller verifies the printed tag externally. The `OR Ok`
branch lets the caller decline when the card lacks the tag;
once the metric can test the revealed card's printed tag, that branch can be removed. Each `BuyCard`
creates 3 M€ of debt, card-specific modifiers adjust that debt through
`PayingFor<Class<ProjectCard>>`, and settling the `CardPurchase` billing moves its selected back to
`Hand` without assigning a printed identity.

Do not add engine identities for cards that were offered, revealed from a deck, searched past, rejected,
or left in an external deck without entering a hand. Anonymous selection counts belong in Pets
when the offer size and retain choice are part of the executable rule. Do not infer hand identities
from anonymous counts; known names are supplied and tracked outside the engine.

When executable Pets omits a physical offer size or look/keep
relationship, preserve the missing fragment of the former expression beside it as a comment. Do this
only for a fact absent from the executable form; the `SearchForCard` filter records a search
criterion. The comment records an unmodeled rule fact, not dormant implementation.

The World has no general check for an abandoned `Selecting` pool. Each currently modeled effect
drains its own pool explicitly; a future operation-completion check should detect a missed decision
without silently discarding its leftovers.

## Replay tracking

[CardTrackingFullGameTest.kt](../../test/common/dev/martianzoo/tfm/tests/replays/CardTrackingFullGameTest.kt)
maintains an exact-name ledger outside the World for stronger replay evidence. Named draws, buys,
discards, plays, and returns annotate the matching generic card-count events. A discard-and-draw
sequence records separate removal and gain events; strict tracking requires both to be named.
`projectCardArrivalOrder` contains only cards that actually enter the
indicated Player's hand, in arrival order. Rejected offers and searched-past cards do not appear in
the fixture. Replay-local unknown names can stand in for hand cards whose faces are absent from the
source evidence.

Tracking verifies that every modeled project-card movement into or out of `Hand` is named and that
the external ledger agrees with each Player's `ProjectCard<Hand>` count. A temporary
`Hand`–`Revealed`–`Hand` cycle consumes no new arrival names, but strict tracking requires the caller
to name both movements of the already held cards. `Selecting` gains and rejections consume no
names; only a move from `Selecting` into `Hand` does. The focused scenarios in
[CardTrackingFullGameTestTest.kt](../../test/jvm/dev/martianzoo/tfm/tests/replays/CardTrackingFullGameTestTest.kt)
exercise named arrivals, plays, returns, discards, and tracking failures. This is implemented test
support, not yet a production game-playing API.
At strict completion, the tracker also checks the named arrivals produced by `SearchForCard` and
`TakeSelectedCard` against their filters. Its test-only matcher uses `cardTags` for printed tags
(including the event icon) and the card's Pets declaration for references to game concepts.
Search for Life and Asteroid Deflection System successes require a matching card identity supplied
with `nameFlippedCard(actionResult, cardName)`. This annotation never enters the hand ledger.
The tracker reads the filter from the recorded `ClaimCardReward` choice offered by Pets, including
when it was declined; it contains no card-specific mapping of claim names, rewards, or tags.
Failed flips may remain unnamed; when named, their card must lack the relevant tag. If the source
does not identify a successful flip, use a matching stand-in and explicitly comment that it is faked.
`CardTrackingCriteriaTest` covers valid and invalid criteria and flip outcomes.
The tracker cannot detect a missing offer or reveal event, and does not track
corporation or Prelude card identities. Its success is not proof that all physical card movements
were modeled; skipped search cards are intentionally outside the modeled movements.
The four database-backed conversions and `StinaGameTest` use this strict base. Other full-game and
solo replays use ordinary follow-mode test bases without a card ledger.

## Selected game-playing direction

Build a coherent middle-ground card-tracking game-playing API, with exact hand membership tracked
**outside the engine**, along the lines demonstrated by the test harness. Known identities of cards
that enter a hand or play matter; identities of cards that never enter either do not. The anonymous
engine counts and the external tracking must agree.

This direction does not select a public API shape, require copying the test harness literally, or
authorize restoring the old offer-location machinery wholesale. Find the smallest coherent
contract for normal named-card play; do not reintroduce the broader real-card branch.
Human-directed and autonomous solo play should use the same game-playing capabilities.

## Deliberate boundaries

Hidden-information handling, player-specific universes, and drafting are not selected goals. Exact
hand tracking outside the engine is selected; hiding those names from other readers is not required.
Shuffle/deal policy, deck order, and identities of cards never entering a hand or play do not belong
in the engine. Do not build scaffolding for a full dealer or real-card mode as a prerequisite for
this narrower tracking API.
