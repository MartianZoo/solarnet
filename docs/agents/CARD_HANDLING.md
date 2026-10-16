# Card handling and external tracking

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing card backs, card play, draws, searches, purchases, card-tracked replays,
> or the card-tracking game-playing API.
>
> **Status:** runtime card tracking support; the anonymous count-and-location model lives in Pets. Future
> game-playing API direction lives in the Solarnet roadmap.

## World model

See the [Pets card model](../../pets/docs/agents/CARD_MODEL.md#world-model).

## External offer procedures

See the [Pets card model](../../pets/docs/agents/CARD_MODEL.md#external-offer-procedures).

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

Missing names raise `CardTracking.NamesRequiredException`, an `AgentInputRequiredException` with
the Player, back, location, direction, additional count, and attempted change event. Its detached
`candidates` set describes the eligible source immediately before that movement, including earlier
movements within the rolled-back command and excluding names already supplied for this movement.
Null means a recognized fresh arrival requiring an external dealer; an empty set means no known
source card is eligible. Unknown transfers must not be interpreted as draws. `filter` carries the
arrival's `CardFilter` when present. The public `cardMatchesFilter` function shares the tracker's
printed-card predicate with dealers. The attempted event is diagnostic evidence, not committed
history.

The tracker retains its last successfully validated snapshot and processes only subsequent change
events. It reuses that snapshot only while its last event still survives by identity; rollback or
a different branch at the same ordinal causes reconstruction from surviving history. Failed
validation never replaces the saved snapshot. Queued names remain independent of that snapshot.

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

`hand(player, back, location)` reads a detached set at the current timeline position; Hand is the
default location, and Selecting requires the tracking option above. Seeking retains the supplied
name sequences for the future. After truncating, clients clear unused sequences through the
replacement methods. A live dealer also restores its own deck cursors; the tracker owns no decks.

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
