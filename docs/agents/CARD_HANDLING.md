# Count-only card handling

> **NOTE:** This document is maintained for coding agents. A human did not write it.

> **Read when:** changing card backs, card play, draws, searches, purchases, or card-tracked
> replays.
>
> **Status:** current model and deliberate boundary.

## World model

Solarnet follows an external Terraforming Mars game. It records only the number of generic card
backs owned by each Player:

- `ProjectCard` is the count in that Player's hand;
- `CorporationCard` and `PreludeCard` are analogous setup/phase counts;
- a concrete `CardFront<Class<CardBack>>` is an exact face in play; and
- `PlayedEvent<Class<CardFront>>` preserves the exact face of a completed Event because published
  scoring and recovery rules query it.

There are no card-location arguments, offer or reveal pools, deck or discard Components, hidden
faces, physical-copy identities, shuffle state, or dealer policy. Playing a card consumes one
generic back and creates the concrete face supplied by the caller. Solarnet trusts that declaration.

## Count-only procedures

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

Do not add state or syntax for cards that were offered, revealed, searched past, rejected, or left
in an external deck. Do not infer a face before play.

When executable Pets omits a physical offer size, search predicate, reveal condition, or look/keep
relationship, preserve the missing fact in `CardPrintedProcedureText.kt` if it is needed to render
the printed English procedure. Otherwise preserve the missing fragment of the former expression
beside Pets as a comment. Do this only for a fact absent from the executable form;
`SearchForCard<TagFilter<Class<Tag>>>` already records its tag criterion.
The authored text or comment records an unmodeled rule fact, not dormant implementation.
Non-tag searches still use a direct `ProjectCard` gain and retain their former predicates in the
printed-procedure data or, when no authored procedure needs them, in comments;
their old `PrintedTag` and `ReferenceTo` vocabulary was removed with real-card mode. Do not recreate
that machinery merely to make those predicates executable.

## Replay tracking

`CardTrackingFullGameTest` may maintain an exact-name ledger outside the World for stronger replay
evidence. Named draws, buys, discards, plays, and returns annotate the matching generic card-count
events. `projectCardArrivalOrder` contains only cards that actually enter the indicated Player's
hand, in arrival order. Rejected offers and searched-past cards do not appear in the fixture.

Strict tracking verifies that every hand-count change is named and that the external ledger agrees
with each Player's `ProjectCard` count. This test-only evidence does not change production semantics.

## Deliberate non-goal

Solarnet does not plan to become the card dealer or model real-card mode. Shuffle/deal, deck order,
hidden information, and unplayed face identity remain the responsibility of the external game or
client. Reintroducing any of them requires a new explicit design decision; do not preserve or add
scaffolding for that hypothetical mode.
