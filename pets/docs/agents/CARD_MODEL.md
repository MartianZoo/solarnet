# Card model and offers

> **Status:** current anonymous card counts, locations, and authored offer protocols.
> Runtime identity tracking is documented in [Solarnet card handling](../../../docs/agents/CARD_HANDLING.md).

## World model

The engine follows externally supplied Terraforming Mars card decisions. Before play, it records
generic card backs by Player and location, never by printed identity:

- `ProjectCard<Hand>` is the count in that Player's hand; `Hand` is the default card location in
  instructions and queries;
- `CorporationCard<Hand>` and `PreludeCard<Hand>` are analogous setup/phase counts;
- `BeginnerCard<Hand>` is the separate back used only by the beginner variant;
- `Selecting` temporarily holds anonymous backs for project-card look-and-keep effects and the
  modeled corporation and Prelude offers. Retained project cards move to `Hand`; selected Prelude
  and Merger corporation cards are played from `Selecting`. Turn cleanup removes any remainder;
- `Revealed` temporarily holds an already owned card for Public Plans, or one anonymous card
  revealed from outside the World for Asteroid Deflection System and Search for Life;
- a concrete `CardFront<Class<CardBack>>` is an exact face in play; and
- `PlayedEvent<Class<CardFront>>` preserves the exact face of a completed Event because published
  scoring and recovery rules query it. It is also `System`: Admin performs ordinary event cleanup
  after the event's other work finishes. Optional flips, such as Pharmacy Union's, and recipient
  choices, such as Law Suit's, still require Player selection before Admin executes the transfer.

Modeled reveal effects explicitly empty their temporary locations. Retained
starting project cards rest in `Selecting` after setup until their Corporation-phase purchase is
paid and transferred. Project cards selected for a purchase also stay there while payment or the
transfer task is outstanding. Each `ActionScope` schedules Admin cleanup under `LATER`: after the
action's ordinary work, Admin discards remaining card backs in that Player's `Selecting` before
scope removal. Research schedules the same cleanup for project backs after every Player's purchase
work and before its phase handoff. Setup selections still wait for the corporation phase. An
isolated sandbox action receives scope cleanup even when it has no seat-order continuation.

Fixed-size project-card purchase offers enter `Selecting` first. Searches intentionally have no
selection pool: the matching card enters the hand, and skipped cards have no relevant
count or movement.
There is no deck or discard Component, hidden face, physical-copy identity, shuffle state, or
dealer policy. Playing a card consumes one generic back from its stated location and creates the
concrete face supplied by the caller. Solarnet trusts that declaration.

That trust is deliberate calculator behavior, not an unfinished authentication feature. A named
play through FooPlayer's Agent is FooPlayer's declaration for the calculation. Whether the player
actually held that face, who may learn a draw, and whether reported discards are valid are checked
outside the engine as described in [ADVERSARIAL.md](../../../docs/agents/ADVERSARIAL.md). Counts, locations, and the
consequences of the declared face remain game rules for Solarnet to calculate.

## Cards stored on Self-Replicating Robots

`StagedProject<Class<CardFront>>` is both a `CardLocation` outside play and a
`ResourceHolder<Class<RobotUnit>>`. Storing moves a project back from Hand into that location and
adds two `RobotUnit` resources. It requires a printed Building or Space tag; `PriceAspectCount`
checks those printed tags without gaining the card's face. The stored project contributes no live
tags, actions, play effects, or printed resource capacity.

`PlayCardAction` offers play from Hand or a `StagedProject`. The stored location rejects a face
that differs from its recorded card class. The stored back stays there through payment, while the
holder's robot count reduces `Owed` during `PayingFor`. Playing the
face consumes the back from its staged location; removing that back removes the holder and its
resources. Ordinary play requirements and payment choices remain in force. In optional
`CardTracking` mode, the staging transfer names the departure from Hand; playing from the staged
location needs no temporary Hand arrival. Other stored projects stay in place.

CEO's Favorite Project, Applied Science, and L1 Trade Terminal select stocked `ResourceHolder`s.
Sponsored Projects does likewise across `Player` owners, excluding the neutral solo reserves.
Corroder Suits and Maxwell Base share `AddResourceToVenusCard`, which additionally accepts stored
projects whose class has a printed Venus tag; this does not create a live tag. The signal offers
mandatory additions only when an eligible destination exists, so an empty alternative cannot
make an available addition optional. Excentric counts resources held by `CardFront`s. Trader counts
distinct resource types on card fronts. Collector and Diversity Support count both standard and card
resource types, with an explicit `NOT Class<RobotUnit>` exclusion to keep those queries compact.
`SelfReplicatingRobotsTest` covers storage, resource additions, doubling, Viron, payment, and scoring.

## External offer procedures

Prelude plays use `PlayPrelude`: play the chosen face, or discard its anonymous back, record an
`Audit`, and gain 15 M€. The caller verifies that the selected Prelude is unplayable. This same
signal serves the Prelude phase and additional Prelude plays granted by cards. Offered cards in the
fixed Valley Trust, New Partner, WG Project, and Merger offers are counted in `Selecting`. Their
chosen cards are played directly from there; turn cleanup discards the others.
Each Player first gains the persistent, owned `CorporationOption` recording their chosen setup path.
Both choices are `System`: Admin records the standard choice when the variant is disabled, while
the beginner-versus-standard choice remains a Player task when `BeginnerVariant` is enabled.
The `StandardOption` path then puts both offered
corporations in `Hand` and ten project cards in `Selecting`; one corporation and any unwanted
project cards are discarded. The `BeginnerOption` path creates only `BeginnerCard`; the beginner
corporation later adds ten project cards directly to `Hand`, without moving cards from `Selecting`.
Prelude's setup rule reacts to `CorporationOption` and owns its deal and discard.
Corporation phase plays the retained corporation or beginner card from `Hand`. Gaining that
`CardFront` triggers `BuySelectedCards`, so the corporation's purchase modifiers are already live
when Admin starts the purchase. Completing the earlier `PlayCard` signal is not sufficient.
Gameplay callers choose the card face without repeating its location. `PlayCard` has no location
default: `doTask` intersects the caller's choice with the pending task, which supplies the authorized
source (`Hand` or a stored project for ordinary plays, `Selecting` for plays from a card's offer).
The ordinary `PlayCardAction` does not offer `Selecting` or `Revealed`, even when those locations
contain card backs. A stored location also checks that the chosen face matches its recorded class.

An ordinary draw adds `ProjectCard<Hand>`. Fixed-size project-card offers, including buys,
gain the full offer as `ProjectCard<Selecting>`. Look-and-keep effects move only retained backs to
`Hand` and remove the remainder. For a buy, the Player removes unwanted backs, then
`BuySelectedCards` is `System` and bills 3 M€ for each remaining selected back, applying card
purchase discounts through `PayingFor<Class<ProjectCard>>`. Completing payment offers the Player
`N ProjectCard<Hand FROM Selecting>` as an explicit transfer task. The selected backs stay in
`Selecting` until the Player performs it. Zero buys leave no selected backs. Neither the World nor
the replay ledger names rejected cards. Searches create only the matching hand card;
there is no count of cards searched past. `SearchForCard<CardFilter>` records the externally
verified criterion as a transient audited event. `TagFilter`, `NoTagsFilter`,
`ReferenceFilter`, and `RequirementFilter` cover printed tags, no printed tags, references, and
the presence of a printed play requirement. The selected
card's printed properties do not become hand state. One generic `TagFilter` Class serves every
tag, with its singleton Components supplied by the rule Module. Its `Class<Tag>` dependency is the
criterion; it has no `criteria` property because Requirement properties do not currently
specialize named header variables.

Party-requirement searches use `ReferenceFilter<Class<Ruling>>`: the current Canon cards cite
`Ruling` in their ordinary `Ruling<Party> OR 2 PartyDelegate<Party>` requirements. Playing those
cards evaluates the Pets condition directly; no custom party-requirement metric is needed.
No current Canon card cites `Ruling` outside such a requirement.

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

The World has no general check for an abandoned `Selecting` pool. Turn and research cleanup discard
unselected backs. The Player still removes unwanted purchase cards before `BuySelectedCards`.

Wild icons are included in `cardTags` metadata, but do not disqualify a card from `NoTagsFilter`.
A search examines cards outside play, where the wild has no assigned tag. Ordinary printed tags,
including Event, still disqualify a card from that search.

## Wild tags

**Canon integration.** Research Coordination, Research Network, Applied Science, Nobel Prize,
and Septem Tribus create an inert printed `WildTag`. While an `ActionScope` exists, `WildTagElection<WildTag, ActionScope>`
holds that wild's assignment. Assigning `EarthTag<WildTagElection<WildTag<ResearchCoordination>>>`
changes real state; ordinary tag metrics include it. `MAX 1 Tag<This>` keeps that meaning fixed.
Removing the action scope removes its elections and their assigned tags through ordinary dependency
cleanup; the printed wild survives. The active scope creates elections for existing and newly
played wilds, so the printed icon itself carries no behavior.

Canon's `Tag` now declares `DEFAULT Tag<CardFront>:`. Printed-tag listeners explicitly accept
that default with `EarthTag<>:` or name a holder themselves. This affects gain/removal triggers,
not metrics or gains. Point Luna therefore reacts to Cartel's printed Earth tag, but not Earth
assigned to a wild. `cardTags` also reports the printed wild icon as card metadata.

Generated `LikesTag<Class<FooTag>>` roles describe positive immediate per-tag benefits and tag
gates. The rule on each unused election assigns that preferred tag before queued benefits. Requirements
are excluded from this analysis. Equivalent sibling benefits choose a stable representative:
Gyropolis uses Earth, as directed by the owner. The same analysis emits `UseAction` allocation
for action benefits (Orbital Cleanup and Kuiper Cooperative). Venus Shuttles declares its allocation
on `UseAction` explicitly so its Venus count is ready before the cost is calculated.
Robotic Workforce and Cyberia Systems offer optional assignments for unused wilds when played with
an active wild election. Their copy selectors accept a printed Building tag or a printed wild whose
election has been assigned Building. The player assigns Research Network's wild before selecting it
for copying; an Earth assignment can likewise precede copied Gyropolis production, or a Building
assignment can precede copied Medical Lab production. The assignments last for the action, so
Cyberia's second copied box can count the Building assignment made for Research Network.
`CopyProductionBox` only extracts the selected card's authored production box.
Choosing a wild assignment before the target is selected changes task order, not the available
game outcomes: the player can decide which card to copy before submitting either choice.
Corroder Suits and Maxwell Base use the analogous Venus filter and allocation on play or action use,
allowing them to add science to Applied Science. Applied Science itself adds resources only to its
owner's stocked cards, as directed by the owner.

Authored player choices:

- Interplanetary Trade offers `EACH @WildTagElection { Tag<@WildTagElection>? }` alongside its benefit.
- Diversifier needs eight distinct tag types. Its requirement counts existing tag types plus each
  unused wild election, since each can supply a missing type. Assigned wilds count only through
  their actual tag type, so they are not counted twice. It needs no assignment choice or watcher,
  including when selected on another map. Planetologist uses the shared minimum allocator.

Nobel Prize uses `SearchForCard<RequirementFilter>` rather than unrestricted draws.
English describes the printed wild's rule but omits allocation instructions.

Requirement allocation and Cloud Tourism's allocator are implemented in
[Solarnet](../../../docs/agents/ENGINE.md#wild-tags).
