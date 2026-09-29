# Card handling and external tracking

> **NOTE:** This document is maintained for coding agents. A human did not write it.

> **Read when:** changing card backs, card play, draws, searches, purchases, card-tracked replays,
> or the card-tracking game-playing API.
>
> **Status:** current count-only engine model and selected external-tracking direction. The
> production card-tracking API remains to be designed.

## World model

The engine follows externally supplied Terraforming Mars card decisions. Before play, it records
only the number of generic card backs owned by each Player:

- `ProjectCard` is the count in that Player's hand;
- `CorporationCard` and `PreludeCard` are analogous setup/phase counts;
- a concrete `CardFront<Class<CardBack>>` is an exact face in play; and
- `PlayedEvent<Class<CardFront>>` preserves the exact face of a completed Event because published
  scoring and recovery rules query it.

There are no card-location arguments, offer or reveal pools, deck or discard Components, hidden
faces, physical-copy identities, shuffle state, or dealer policy. Playing a card consumes one
generic back and creates the concrete face supplied by the caller. Solarnet trusts that declaration.

## Count-only procedures

Prelude plays use `PlayOrFizzle`: play the chosen face, or discard its anonymous back, record an
`Audit`, and gain 15 M€. The caller verifies that the selected Prelude is unplayable. This same
signal serves the Prelude phase and additional Prelude plays granted by cards. Offered and rejected
cards remain external, including Valley Trust's two unselected Preludes.

Only cards that reach a Player's hand enter the World. An ordinary draw or an inspect-and-keep
instruction therefore gains the retained `ProjectCard` count directly. Searches skip unretained
faces entirely.
For a retained card known to have a particular printed tag,
`SearchForCard<TagFilter<Class<Tag>>>` records that externally verified criterion as a transient
audited event and adds an anonymous `ProjectCard`; the tag does not become hand state. One generic
`TagFilter` Class serves every tag, with its singleton Components supplied by the rule Module.
Its `Class<Tag>` dependency is the criterion; it has no `criteria` property because Requirement
properties do not currently specialize named header variables.
Reveal-and-test cards ask the client only for the optional
outcome. A chosen `BuyCard` count enters the normal payment workflow: it creates 3 M€ of debt per
card, card-specific modifiers adjust that debt through `PayingFor<Class<ProjectCard>>`, and settling
the one `CardPurchase` billing converts the request count into the same number of generic
`ProjectCard`s. The workflow represents no card identity or selection pool.

Do not add engine state or syntax for cards that were offered, revealed, searched past, rejected,
or left in an external deck. Do not infer hand identities from anonymous counts; known names are
supplied and tracked outside the engine.

When executable Pets omits a physical offer size, search predicate, reveal condition, or look/keep
relationship, preserve the missing fragment of the former expression beside it as a comment. Do this
only for a fact absent from the executable form; `SearchForCard<TagFilter<Class<Tag>>>` already
records its tag criterion.
The comment records an unmodeled rule fact, not dormant implementation.
Non-tag searches still use a direct `ProjectCard` gain and keep their former predicates as comments;
their old `PrintedTag` and `ReferenceTo` vocabulary was removed with real-card mode. Do not recreate
that machinery merely to make those predicates executable.

## Replay tracking

[CardTrackingFullGameTest.kt](../../test/common/dev/martianzoo/tfm/tests/replays/CardTrackingFullGameTest.kt)
maintains an exact-name ledger outside the World for stronger replay evidence. Named draws, buys,
discards, plays, and returns annotate the matching generic card-count events. An atomic
`ProjectCard FROM ProjectCard` exchange names its gained and removed cards separately; strict
tracking requires both sides. `projectCardArrivalOrder` contains only cards that actually enter the
indicated Player's hand, in arrival order. Rejected offers and searched-past cards do not appear in
the fixture. Replay-local unknown names can stand in for hand cards whose faces are absent from the
source evidence.

When enabled, strict tracking verifies that every hand-count change is named and that the external
ledger agrees with each Player's `ProjectCard` count. The focused scenarios in
[CardTrackingFullGameTestTest.kt](../../test/jvm/dev/martianzoo/tfm/tests/replays/CardTrackingFullGameTestTest.kt)
exercise named arrivals, plays, returns, discards, and tracking failures. This is implemented test
support, not yet a production game-playing API.

## Selected game-playing direction

Restore a coherent middle-ground card-tracking game-playing API, with card locations tracked
**outside the engine**, along the lines demonstrated by the test harness. Known identities of cards
that enter a hand or play matter; identities of cards that never enter either do not. The anonymous
engine counts and the external tracking must agree.

This direction does not select a public API shape, require copying the test harness literally, or
authorize restoring the old location machinery wholesale. Find the smallest coherent contract for
normal named-card play; do not reintroduce the broader real-card branch. Human-directed and
autonomous solo play should use the same game-playing capabilities.

## Deliberate boundaries

Hidden-information handling, player-specific universes, and drafting are not selected goals. Exact
hand tracking outside the engine is selected; hiding those names from other readers is not required.
Shuffle/deal policy, deck order, and identities of cards never entering a hand or play do not belong
in the engine. Do not build scaffolding for a full dealer or real-card mode as a prerequisite for
this narrower tracking API.
