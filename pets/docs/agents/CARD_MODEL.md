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
  scoring and recovery rules query it.

Modeled reveal effects explicitly empty their temporary locations. Retained
starting project cards rest in `Selecting` after setup until their Corporation-phase purchase is
paid and transferred. Project cards selected for a purchase also stay there while payment or the
transfer task is outstanding. Each `NewTurn` schedules Admin cleanup under `LATER`: after that turn's
ordinary work, Admin discards remaining card backs in that Player's `Selecting` before `TurnOver`.
Research schedules the same cleanup for project backs after every
Player's purchase work and before its phase handoff. Setup selections still wait for the corporation
phase. These rules do not run for isolated sandbox operations that bypass turn and phase flow.

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
and Septem Tribus create a
`WildTag` that is itself a `TagHolder`. Assigning `EarthTag<WildTag<ResearchCoordination>>` changes
real state; ordinary tag metrics include it. `MAX 1 Tag<This>` keeps that meaning fixed, and the
wild's `Tag<This>: LATER Admin { -Tag<This>* }` removes the assignment when ordinary work is finished.
The printed wild survives. There is no additional task-scheduling mechanism.

Canon's `Tag` now declares `DEFAULT Tag<CardFront>:`. Printed-tag listeners explicitly accept
that default with `EarthTag<>:` or name a holder themselves. This affects gain/removal triggers,
not metrics or gains. Point Luna therefore reacts to Cartel's printed Earth tag, but not Earth
assigned to a wild. `cardTags` also reports the printed wild icon as card metadata.

Generated `LikesTag<Class<FooTag>>` roles describe positive immediate per-tag benefits and tag
gates. The rule on each unused wild assigns that preferred tag before queued benefits. Requirements
are excluded from this analysis. Equivalent sibling benefits choose a stable representative:
Gyropolis uses Earth, as directed by the owner. The same analysis emits `UseAction` allocation
for action benefits (Orbital Cleanup and Kuiper Cooperative). Venus Shuttles declares its allocation
on `UseAction` explicitly so its Venus count is ready before the cost is calculated.
Robotic Workforce and Cyberia Systems do not yet support wilds when selecting or counting copied
production; see [Solarnet TODO](../../../docs/TODO.md) and their card-test pairs. Blanket Building allocation was removed because
it prevented wilds from contributing Earth to copied Gyropolis production.
Corroder Suits and Maxwell Base use the analogous Venus filter and allocation on play or action use,
allowing them to add science to Applied Science. Applied Science itself adds resources only to its
owner's stocked cards, as directed by the owner.

Authored player choices:

- Interplanetary Trade offers `EACH @WildTag { Tag<@WildTag>? }` alongside its benefit.
- Diversifier's `CardWatcher` offers the same optional choices when claiming it, including when
  Diversifier is selected on another map. Its ordinary requirement still checks that eight distinct
  types actually exist. Planetologist uses the shared minimum allocator.

Nobel Prize uses `SearchForCard<RequirementFilter>` rather than unrestricted draws.
English describes the printed wild's rule but omits allocation instructions.

Requirement allocation and Cloud Tourism's allocator are implemented in
[Solarnet](../../../docs/agents/ENGINE.md#wild-tags).
