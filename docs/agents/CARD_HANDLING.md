# Card handling and external tracking

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing card backs, card play, draws, searches, purchases, card-tracked replays,
> or the card-tracking game-playing API.
>
> **Status:** current anonymous count-and-location model and replay tracking support. Future
> game-playing API direction lives in the Solarnet roadmap.

## World model

The engine follows externally supplied Terraforming Mars card decisions. Before play, it records
generic card backs by Player and location, never by printed identity:

- `ProjectCard<Hand>` is the count in that Player's hand; `Hand` is the default card location in
  instructions and queries;
- `CorporationCard<Hand>` and `PreludeCard<Hand>` are analogous setup/phase counts;
- `BeginnerCard<Hand>` is the separate back used only by the beginner variant;
- `Selecting` temporarily holds anonymous backs for project-card look-and-keep effects and the
  modeled corporation and Prelude offers. Retained project cards move to `Hand`; selected Prelude
  and Merger corporation cards are played from `Selecting`. The effect removes the rest;
- `Revealed` temporarily holds an already owned card for Public Plans, or one anonymous card
  revealed from outside the World for Asteroid Deflection System and Search for Life;
- a concrete `CardFront<Class<CardBack>>` is an exact face in play; and
- `PlayedEvent<Class<CardFront>>` preserves the exact face of a completed Event because published
  scoring and recovery rules query it.

The modeled selection and reveal effects explicitly empty their temporary locations. Retained
starting project cards rest in `Selecting` after setup until their Corporation-phase purchase is
paid and transferred. Project cards selected for a purchase also stay there while payment or the
transfer task is outstanding. Otherwise, a card left in `Selecting` after its selection is resolved
is a bug. The selected invariant is zero `Selecting` backs for a Player whenever that Player has no
active selection or outstanding purchase. Setup and Prelude selections count as active selections
even though they occur outside action-phase turns. No general check enforces this invariant yet;
implementing one requires a reliable selection-completion point. The existing `MustCleanUp` check
runs only for operations that declare themselves complete. Do not treat other leftover backs as a
valid resting state or silently discard them to conceal a missing decision.

Fixed-size project-card purchase offers enter `Selecting` first. Searches intentionally have no
selection pool: the matching card enters the hand, and skipped cards have no relevant
count or movement.
There is no deck or discard Component, hidden face, physical-copy identity, shuffle state, or
dealer policy. Playing a card consumes one generic back from its stated location and creates the
concrete face supplied by the caller. Solarnet trusts that declaration.

That trust is deliberate calculator behavior, not an unfinished authentication feature. A named
play through FooPlayer's Agent is FooPlayer's declaration for the calculation. Whether the player
actually held that face, who may learn a draw, and whether reported discards are valid are checked
outside the engine as described in [ADVERSARIAL.md](ADVERSARIAL.md). Counts, locations, and the
consequences of the declared face remain game rules for Solarnet to calculate.

## External offer procedures

Prelude plays use `PlayPrelude`: play the chosen face, or discard its anonymous back, record an
`Audit`, and gain 15 M€. The caller verifies that the selected Prelude is unplayable. This same
signal serves the Prelude phase and additional Prelude plays granted by cards. Offered and rejected
cards in the fixed Valley Trust, New Partner, WG Project, and Merger offers are counted in
`Selecting`; the rejected backs are removed without names before the chosen back is played directly
from there.
Each Player first gains the persistent `PlayerMode` recording their chosen setup path. Both modes
are `System`: Admin installs a fixed mode, while the beginner-versus-normal choice remains a
Player task when `BeginnerVariant` is enabled. The `NonBeginnerMode` path then puts both offered
corporations in `Hand` and ten project cards in `Selecting`; one corporation and any unwanted
project cards are discarded. The `BeginnerMode` path creates only `BeginnerCard`; the beginner
corporation later adds ten project cards directly to `Hand`, without moving cards from `Selecting`.
Prelude's setup rule reacts to `PlayerMode` and owns its deal and discard.
Corporation phase plays the retained corporation or beginner card from `Hand`. Gaining that
`CardFront` triggers `BuySelectedCards`, so the corporation's purchase modifiers are already live
when Admin starts the purchase. Completing the earlier `PlayCard` signal is not sufficient.
Gameplay callers choose the card face without repeating its location. `PlayCard` has no location
default: `doTask` intersects the caller's choice with the pending task, which supplies the authorized
source (`Hand` for ordinary plays, `Selecting` for plays from an offer). An explicit conflicting
source is rejected even when that location contains another card.

An ordinary draw adds `ProjectCard<Hand>`. Fixed-size project-card offers, including buys,
gain the full offer as `ProjectCard<Selecting>`. Look-and-keep effects move only retained backs to
`Hand` and remove the remainder. For a buy, the Player removes unwanted backs, then
`BuySelectedCards` is `System` and bills 3 M€ for each remaining selected back, applying card
purchase discounts through `PayingFor<Class<ProjectCard>>`. Settling the bill offers the Player
`N ProjectCard<Hand FROM Selecting>` as an explicit transfer task. The selected backs stay in
`Selecting` until the Player performs it. Zero buys leave no selected backs. Neither the World nor
the replay ledger names rejected cards. Searches create only the matching hand card;
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
checking that the claim or decline matches the actual revealed card belongs to the external tracker
or custodian. The recorded filter supplies the criterion; securing the declaration does not
require adding hidden faces or an engine reveal-verification stage. The same purchase billing
and Player transfer task apply to these selected backs without assigning a printed identity.

Do not add engine identities for cards that were offered, revealed from a deck, searched past, rejected,
or left in an external deck without entering a hand. Anonymous selection counts belong in Pets
when the offer size and retain choice are part of the executable rule. Do not infer hand identities
from anonymous counts; known names are supplied and tracked outside the engine.

When executable Pets omits a physical offer size or look/keep
relationship, preserve the missing fragment of the former expression beside it as a comment. Do this
only for a fact absent from the executable form; the `SearchForCard` filter records a search
criterion. The comment records an unmodeled rule fact, not dormant implementation.

The World has no general check for an abandoned `Selecting` pool. Each currently modeled effect
drains its own pool explicitly.

## Card-hand tracking

[CardTracking](../../src/common/dev/martianzoo/tfm/engine/CardTracking.kt) is an optional mode
selected once per World before the first card-hand movement. Once selected, each Agent command
validates all surviving `CardBack<Hand>` gains and removals, including project, corporation,
Prelude, and beginner cards, before it can complete. The client supplies names in each Player's
per-back hand-entry order and names for otherwise anonymous departures. Supplied names must resolve
to concrete `CardFront` classes with the matching back in the catalog. A source card can be named
even when that card is excluded from this game's playable pool. An operation with missing names
rolls back; automatic execution can leave the affected card task pending for the client.
Unused supplied names may be replaced after a failed operation without changing names used by
surviving events.

The tracker writes a `Cards:` note on every hand-change event, checks that a played card was in
that Player's hand, and compares named holdings with modeled hand counts for every back type.
`SearchForCard` and `TakeSelectedCard` gains are checked against their recorded Pets `CardFilter`
using printed tags or the card declaration. `Hand`–`Revealed`–`Hand` movements retain identity
when unambiguous; a partial return needs supplied names. A card outside a hand may enter again
after a discard or reshuffle. A hand arrival cannot reuse a card still held or played elsewhere.
`PlayedEvent` already carries its card's exact name, so moving a played face into that type needs no
supplied name or `Cards:` note. With Selecting tracking off, a direct play from `Selecting` is not
checked against previously seen cards.

`CardTracking.start(world, requireSelectingNames = true)` additionally requires every `Selecting`
movement to be named. The option applies to the whole game; it cannot mix known and unknown offers.
The default mode ignores Selecting identities and rejects supplied Selecting names. In the enabled
mode, the tracker compares named Selecting holdings with modeled counts, checks card locations, and
writes `Cards:` notes for Selecting changes. Transfers to `Hand` use the required hand-arrival names,
and direct plays carry the face in their type. Use `location = cn("Selecting")` with the same
`expectArrivals`, `expectDepartures`, and `replacePending...` methods used for Hand. Supply names
just before the matching operation or in exact event order; queued names always belong to the next
matching event. Cards rejected from `Selecting` are named in the enabled mode. Cards only passed
over during a deck search and deck flips remain outside it.

The four database-backed card-tracked replays and `StinaGameTest` now use this production mode;
[CardTrackingTest](../../test/common/dev/martianzoo/tfm/tests/CardTrackingTest.kt)
checks the focused integrity cases.

## Deliberate boundaries

Hidden-information handling, player-specific universes, and drafting are not selected engine goals.
The [adversarial-play proposal](ADVERSARIAL.md) discusses secret custody and acceptance outside
the engine; it does not select their implementation. Exact hand tracking outside the engine is
selected; the narrower tracking API does not require hiding those names from other readers.
Shuffle/deal policy, deck order, and identities of cards never entering a hand or play do not belong
in the engine. Do not build scaffolding for a full dealer or real-card mode as a prerequisite for
this narrower tracking API.
