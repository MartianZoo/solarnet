# Direct corrections and inseparable consequences

> **Read when:** changing `exMachina`, `sneak`, effect suppression, or deciding which consequences
> belong to the existence of a component rather than to a gameplay event.
>
> **Status:** correction contract and canon audit, 2026-10-04. Corrections construct exact concrete
> required parts, retain recursive automatic effects, suppress queued effects, and validate all
> applicable invariants. Direct client edits exclude infrastructure targets. Remaining undeclared
> relationships and the concrete consequences of retained automatic effects are listed below.

## Scope and acceptance criteria

[COLOR_MODES.md](COLOR_MODES.md) selects a red-mode correction path that does not advance pending
gameplay. The selected-task reselection and autoexecution described below do not yet satisfy that
target; exact task-refresh semantics remain open there.

The audit covers all 29 handwritten Pets files in `tfm/canon`, all 536 definitions in its eight
`cards.json5` files, card generation (including owner-local declarations), action lowering, custom
instructions/metrics, and the inherited Pets system declarations. The seven maps contribute 457
area declarations. Repeated declarations are classified by inheritance below; this is an inventory
of consequences, not an enumeration of the unbounded concrete dependency specializations of Types.
Fake canon is outside this audit. Counts describe the audited snapshot, not future test assertions.

- A correction omits queued gameplay rewards and costs, including oxygen from a greenery.
- A card cannot exist without its printed tags and intrinsic capabilities.
- Creating, removing, and replacing state must preserve declared invariants and dependency integrity.
- After every `sneak`/`exMachina`, validate **all applicable invariants**, including requirements
  whose component is absent. Skip checks only when their irrelevance is easy to establish;
  performance is not a concern for these calls. A failed correction must roll back.
- First simplify any `::` effect that can reasonably use ordinary `:` without breaking normal play.
  Otherwise retain it during corrections, including its additional gameplay consequences.
- Prefer exact dependent invariants for required state; otherwise use automatic effects.
- Suppress queued effects throughout derived changes. Corrections do not initiate idle cleanup.
- Report concrete awkward consequences and remaining inconsistencies; do not invent another effect
  category merely to avoid them. Ordinary mandatory work can reject an illegal operation too.
- Do not implement a new mechanism merely to make an unrestricted direct-edit API work.
- Reject direct `System`, `Hidden`, and `MustCleanUp` corrections; required derived changes remain
  possible, provided they do not leave new unfinished work.
- Pharmacy Union may retain automatic starting money, including during corrections, to avoid an
  ordering framework or a special case for its own tags. Both printed tags use its ordinary effect.
- Removing St Joseph removes its action provider and scoring. Placed Cathedrals remain as markers
  depending on their cities; they have no independent scoring.
- Corrections need not obey every gameplay rule. A colony track below the colony count is allowed;
  callers changing delegates are responsible for correcting leadership when needed. Neither case
  calls for another invariant or automatic repair. Existing declared invariants still always apply.
- Pharmacy Union's money, PP's first-TR cancellation, and completed-parameter guards are accepted
  consequences of retaining automatic effects, not outstanding blockers.

## Current execution behavior

`Agent.sneak` accepts a group of fully concrete direct changes. `Instructor` applies the group,
constructs exact concrete required parts of newly gained owners, removes dependents of removed
owners, and checks every applicable count invariant against the completed World. Validation includes
positive requirements whose target is absent, not just types touched by the correction. Failure
rolls back the entire group, including events and derived effect indexes. Validation never repairs.

The correction path runs `::` and suppresses `:` throughout recursive construction and removal.
It does not settle pending tasks, perform idle cleanup, or call the gameplay completion callback.
Temporary components remain for a later gameplay interaction or explicit removal. Effect
subscriptions still follow component presence. Signals remain recorded nonpersistent changes.
Source availability, concrete types, inhabitance, non-metric targets, and dependency integrity
remain immediate preconditions.

`Agents.exMachina` rejects direct changes to `System`, `Hidden`, and `MustCleanUp`
subtypes, on both sides of a transmutation. Dependencies on these types remain legal. It adds an
internal `Audit` and uses the same correction path; required-part construction and dependent removal
may change hidden and system state. It also rejects and rolls back any result with additional
`MustCleanUp` state, so a correction cannot start a workflow whose ordinary completion effects were
suppressed.

`Agent.sneak` remains the lower-level tool for internal setup/corrections: it has the same required-part
construction and full invariant validation, but not these client target restrictions.

If a task was selected, `exMachina` restores that task's unresolved form, applies the correction,
and resumes the selection under the
existing policy. That whole lifecycle is failure-atomic, preserving the selected task on rejection.
Resumed pre-existing gameplay may still execute normally; queued effects of the correction are suppressed.

Shared construction and gameplay transaction coverage lives in
[InvariantCompletionTest](../../test/common/dev/martianzoo/engine/InvariantCompletionTest.kt) and
[WorldTransactionTest](../../test/common/dev/martianzoo/engine/WorldTransactionTest.kt).
Correction-only behavior is intentionally not given dedicated tests; see
[TESTING.md](TESTING.md#test-design).

Production entry points: [exMachina](../../src/common/dev/martianzoo/agent/exMachina.kt),
[Instructor](../../src/common/dev/martianzoo/engine/Instructor.kt),
[Limiter](../../src/common/dev/martianzoo/engine/Limiter.kt),
[WorldTransaction](../../src/common/dev/martianzoo/engine/WorldTransaction.kt).

## Inventory: state relationships and correction responsibilities

Here “required” means preserve the relationship, or reject the correction if it cannot be preserved.
It does not mean every gameplay relationship must be enforced during a correction; each row states
its correction responsibility. Rows apply to all
subclasses, including generated owner-local subclasses. Corrections to a dependent representation
itself must obey the same relationship as corrections to its owner.

| Types/family | Creation or replacement | Removal | Current representation and gap |
| --- | --- | --- | --- |
| Every printed `CardFront` and its `Tag`s | Install exactly its printed tag multiset. | Remove attached tags with the face; reject independent removal of a required tag. | Generator emits exact-count invariants; generic construction supplies the parts. Applies to projects, corporations, and preludes. |
| `GrantedResourceValue`, `Splicer`, `NeptunianOption` | Install the capability attached to its live card. | Remove card-dependent capability; do not reverse earlier resources spent or rewards. | Exact-count invariants and generic construction. See the complete exceptional-card list below. |
| `AridorTagWatcher` | Install one watcher per available tag Class when Aridor enters. | Remove watchers with Aridor. | Existing `This:: EACH` supplies the fanout during corrections. This is not a single concrete exact-count target. |
| `CathedralOption` | Install St Joseph's action provider. | Remove it with St Joseph; placed Cathedrals remain but no longer score. | An exact dependent invariant constructs it. The provider is unowned so another city owner can accept its offer, but depends on the live card. |
| `BoomTown`, `BaseResourceValue` | Maintain Boom Town's ongoing reduction of titanium value. | Restore the reduction when that face disappears. | Existing gain/removal `::` changes the shared baseline during corrections. |
| All `Tile`s; `ForwardAdjacency`, `BackwardAdjacency` | Create both directed adjacency components for every occupied neighboring Mars area. Moving/replacing a tile needs old adjacency removed and new adjacency installed. | Remove adjacency components that depend on the deleted tile. | Existing `Tile.This::` creates pairs; dependency cleanup handles deletion. Neighbor-dependent fanout is not one concrete exact-count target. |
| `Area`, `Tile`, `Community`, `NomadsMarker` | Preserve occupancy maxima and live area dependencies. | Remove live dependents when deleting an area. | Area has `HAS MAX 1 Occupant`, shared by tiles and communities. Own-claim consumption retains `::`, including during corrections. Placing a tile on another player's claim requires explicitly removing that claim in the correction. Nomads' ordinary `: Die` guard is skipped. |
| `OceanTile`, `TemperatureStep`, `OxygenStep`, `VenusStep`; `GpComplete`, `GpIncomplete`, `GpGameEndBarrier` | At a track maximum, synchronize completion, incompletion, and game-end eligibility. | Reject lowering a completed track. | Existing automatic completion and decrease guards run during corrections. Threshold rewards, including Admin ocean requests, use ordinary effects and are omitted. |
| `ColonyTile`, `ColonyProduction`, `Colony` | Initialize the tile track; existing automatic placement raises it to at least the number of colonies. Activate delayed selections when a qualifying resource card exists. | Removing a colony need not lower a higher track; deleting a tile removes its dependents. | Existing `::` runs during corrections, and floor increases count all owners. Directly lowering the track below that count is an accepted correction, not a missing invariant. |
| `CardBack` (all decks/locations), `TradeFleet`, `StartToken`, `AfterMe`, `ActionUsedMarker`, `ScientistsUsedMarker`, `Pass`, `SoloGenerationsLeft`, filters, and simple owned/history markers | No intrinsic gain consequence beyond inherited rules and limits; record the requested fact. | No intrinsic inverse reward; preserve dependencies, uniqueness, and any active operation using the fact. | These facts are not invitations to buy/play a card, perform a trade, rotate turns, or replay a generation. A capacity correction during an unfinished operation needs the protocol boundary below. |
| `Party`, `AfterParty`; `PartyDelegate`, `PartyLeader`, `Dominant`, `LobbyActionAvailable`, `TurmoilPlayer` | Preserve declared capacities, uniqueness, and dependencies. Existing automatic political reactions still run. | Callers must correct leadership after arbitrary delegate edits when necessary. Each concrete Party has `HAS =1 This`, so deletion fails and rolls back. | Mostly `::`, including a player cap implemented with `Die`. Partial removal/transfer tie handling belongs to the gameplay operation; corrections need not reproduce it. |
| `Ruling`, `Policy`, `UnityTitaniumValue` | Install the ruling party's policy during Action phase, whether the phase or the ruling changes. | Remove the old policy and its dependent values with its ruling or phase. | Policy depends on `Ruling<Party>` and `ActionPhase`; party `::` triggers cover both entry events. Ruling bonuses remain separate. |
| `GlobalEvent`, `Current`, `Coming`, `Distant` | Each position depends on its live event. Reveal requests choose a position; arbitrary event creation does not invent one. | Removing an event removes its position. Moving or removing a position preserves the event; normal `ChangingTimes` explicitly discards the old Current event. | Live dependencies and uniqueness invariants keep positions coherent. A correction can swap occupied positions as one group; adding a position requires its event to exist. |
| `Generation`, `GenerationScope`; all scope-dependent markers | Generation advancement needs the corresponding scope and retirement of the old scope's dependents. | Do not leave markers attached to a vanished scope. Erasing/reducing a generation is not automatically the inverse of advancing it. | Both are System and cannot be directly corrected; their existing `::` rules remain for gameplay. |
| `Player`, `ProdOffset`, `BaseResourceValue` | Preserve the production offset and starting resource-value representation of each live player. Editing an offset must preserve the intended displayed production. | Remove associated owned state or reject deletion. Offset removal must not silently change production meaning. | Player setup uses `This:`; offset gain adds raw `Production` with `::`, with no symmetric removal effect. Player identity belongs to the premise. |
| `SoloOpponent`, `SoloStandardResourceReserve`, `SoloCardResourceReserve` | Install neutral resource-holder/reserve capabilities. | Remove dependencies together or reject. | Holder installation is `::`; initial 42-unit stock is setup work. Its replenishment reactions are gameplay conventions, not dependency integrity. |
| `Module`, map and area Classes, `Class<T>`, `CardLocation`, `ActionSlot`, persistent standard actions/projects, `CardPurchase`, singleton rule/watchers | Preserve the selected world's required singletons and installed capabilities. | Reject removal of required infrastructure; do not dynamically rebuild the premise. | Mixture of initialization, `This:` setup, and count invariants. Blanket `::` cannot replace initialization. |

Sources: the owning Pets files under [TerraformingMars](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars),
[ColoniesExpansion](../../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/colonies.pets),
[TurmoilExpansion](../../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/classes.pets),
[VenusNextExpansion](../../src/common/dev/martianzoo/tfm/canon/VenusNextExpansion/venus.pets),
and [Amazonis](../../src/common/dev/martianzoo/tfm/canon/AmazonisMap/amazonis.pets).

### Complete card-attachment exceptions

Beyond printed tags, the JSON exact-count attachments are:

- PhoboLog: one titanium `GrantedResourceValue`.
- Advanced Alloys: one titanium and one steel `GrantedResourceValue`.
- Martian Lumber Corp: three plant `GrantedResourceValue`s.
- Mercurian Alloys: one titanium `GrantedResourceValue`.
- Rego Plastics: one steel `GrantedResourceValue`.
- Splice Tactical Genomics: one `Splicer`.
- Neptunian Power Consultants: one `NeptunianOption`.
- St Joseph of Cupertino Mission: one `CathedralOption` dependent on its live card.

Aridor and Boom Town are the additional explicit capability cases discussed above.
`CardWatcher` installation includes `TrWatcher` (UNMI/Pristar), `PreservationTrWatcher`,
`HydrologistWatcher`, and `ThawerWatcher`; it occurs at Corporation phase entry, not on each card gain.
Their event-history records are covered below.

Delayed resource colonies stay active after the qualifying card leaves play: activation is a
one-way board transition, not a continuously maintained equivalence with current card presence.
Likewise, a party leader's incumbency and a recorded action use contain history; consistency does
not authorize reconstructing a different past from current counts.

Generated `RequiredAction`s, `NextCardEffect`s, special tiles, and remote areas are not all card
attachments: they implement an initial action/reward, an outstanding one-use benefit, or a separate
board object. Creating the card alone need not replay those choices. A removed face need not remove
a separately represented tile. Capital's tile remains dependent on its card, so removing that card
also removes the tile. The proposed independent Capital tile and default city-subtype selection
were deliberately excluded from this correction work.
The five handwritten beginner faces have no printed tags or additional attached capabilities.

Source: [CardPetsGenerator](../../src/jvm/dev/martianzoo/tfm/cardgenerator/CardPetsGenerator.kt),
plus each bundle's `cards.json5`. Normal self-effects/actions on a live
card require no extra “activation” component: the effect index already follows component presence.

## Inventory: consequences that are not intrinsically required

These consequences can matter greatly to normal play without being necessary to represent the
corrected component. Their ordinary effects are suppressed; any remaining automatic effects run.
The following classification describes game meaning, not a separate engine filtering mechanism. Removing a source
does not retrospectively undo rewards.

| Types or trigger family | Consequences that may be skipped |
| --- | --- |
| Standard resources, card resources, `Production`, `TerraformRating` | Ordinary gain/loss reactions, production income, payouts, and scoring. Resource holders/dependencies and count bounds still apply. Includes `Disease`, `Graphene`, `Hydroelectric`, `Fighter`, `Camp`, `Director`, `Preservation`, and all resource-card specializations. |
| Card faces and tags | Printed immediate resources/production, searches/draws, mandatory-first-action offers, and other cards' tag/card rewards. Installing tags must not resume those ordinary reactions. |
| Greenery, city, special, ocean tiles | Oxygen, rating, placement bonuses, neighboring-ocean money, and card/policy rewards. Adjacency remains required. |
| `Placement` and all 457 map areas | Printed placement rewards, including Hellas/Vastitas costs, Cimmeria's colony choice, Amazonis/Vastitas delegates, and Frontier Town's repeat bonus. |
| Global parameters | Rating and threshold rewards: heat/plant production, cards, another parameter, or an ocean request, including Admin-attributed requests. |
| Colonies and colony tracks | Placement bonuses, Poseidon/rebate effects, trade payouts, colony-owner bonuses, and solar-phase advancement. A resource colony's first activation is persistent board state; its later rewards are distinct. |
| Political placements and Global Events | Neutral delegates printed on event arrows, chairman rating, party ruling bonuses, Corridors of Power's card draw, and every printed Global Event reward/loss. A revealed event's position remains structural. |
| Milestones/awards; `FirstPlace`, `SecondPlace`, `VictoryPoint`, `Victory` | Claim eligibility and Briber's extra cost, final ranking/payout execution, and award VP gains. Declared uniqueness/claim-count bounds remain. These are recorded claims/results, not a requirement that current resources still satisfy the old claim. |
| `Energy` and phase transitions | Converting old energy to heat, ordinary production/research/setup, drawing preludes, initial rating, neutral setup stock, solo-generation deductions, and world-government choices. Phase/workflow advancement needs an explicit boundary, discussed below. |
| `HasRaisedTr`, `ActionPhaseTrGain`, `OceanCredit`, `ThawerCredit`, `MyResourceWasRemoved`, `MyProductionWasDecreased` | Historical facts are not derivable from present totals. Ordinary TR, attack, ocean, and temperature history effects are skipped during corrections. Preservation Program's automatic attempt counter and cancellation still run. |
| All `Influence` subclasses | Remeasuring a previously taken snapshot when current delegates are edited. `MeasureInfluence` and Politician scoring explicitly request measurements; the snapshot is not a continuously derived count. |
| `Pets`, `ProtectedHabitats`, `AsteroidDeflectionSystem`, `NomadsMarker`, `GpComplete` guards | Attack/reservation guards use ordinary `: Die` and are skipped. The `GpComplete` decrease guard retains `::` so corrections cannot reopen a track while leaving completion state behind. |
| `NextCardEffect`, `SuitableInfrastructureBonus`, `L1GiftWatcher`/`L1Gift`, `FrontierTownBonus` | Consuming or paying a historical/operation-specific benefit solely because an unrelated correction resembles its trigger. Their owning operation must still be coherent if directly edited. |

All ordinary remaining JSON effects/actions are covered by these inherited families: they grant or
spend resources/production, alter tracks, place board objects, manipulate cards, perform actions,
or score. Support declarations without their own consequences inherit the corresponding resource,
tile, marker, signal, metric, or capability row. No remaining map area has an extra automatic effect
or additional invariant beyond its area family. All goal declarations inherit the milestone/award
row; Briber, Hydrologist, Thawer, and Politician supply the explicit exceptions above.

## Protocol boundary and remaining gaps

Some Types represent doing something rather than a lasting fact to correct:

- **Payment/card play:** `BuyCard`, `Owed`, `Billing` and its subclasses, `Accepting`,
  `AcceptingFromCard`, `PayingFor`, `PriceCard`, `PlayCard`, `Required`,
  `CheckRequirement`, and the buy/search/take/claim signals. Removing a debt may require retiring
  billing and offers, but billing removal also performs the purchased action or grants cards.
  Running all those `::` effects would execute gameplay; skipping them all can strand barriers.
- **Trades:** `Trade`, `TradeBarrier`, `FinishTrade`, `ResetColonyProduction`, bonus/advance signals,
  `PlutoLock`, and the extra barriers from Trade Envoys, Trading Colony, and L1 Trade Terminal. Keeping a record
  of an already completed trade is different from starting a trade whose choices/payout remain.
  Directly creating `Trade` with current `::` installs a barrier whose removal is queued `:` work.
- **Events and scoring:** live `EventCard` is temporary, with tags only while resolving;
  `PlayedEvent` is the lasting record. Idle removal normally creates that record. Clients correct
  `PlayedEvent` directly. A live event is accepted unless its derived state violates the correction
  boundary; it remains until ordinary idle cleanup converts it to `PlayedEvent`.
  `MeasureAward`/`FinalScoringPending` similarly cause scoring when removed through the lower-level API.
- **Workflow:** all `Phase`s, `Generation`, `NewTurn`, `SecondAction`, `Pass`, `RequiredAction`,
  `Photosynthesis`, `CheckGameEnd`, `TurmoilSolarOperation`, `FormGovernment`, `ChangingTimes`,
  reveal requests, `BannedDelegateRemoval`, and scope/temporary/cleanup components. Mutating these
  can invalidate pending tasks or the workflow outside the component graph.
- **Custom instructions:** `CopyProductionBox`, `CopyPrelude`, `ScoreEventVps`,
  `RepeatPlacementBonus`; invoking one is gameplay, not preserving a stored component.
- **Custom metrics:** `Neighbor`, `PlacementBonus`, `GpRequirementShortfall`, `PriceAspectCount`,
  `GainsOf`, `NonNegativeIconsOf`, `TileInLargestGroup`, `PartyDistance`, `PlayerDistance`,
  `PartyRequirement`. They are calculated values and cannot be directly created or removed.

Current client boundary: direct System, Hidden, and MustCleanUp edits are rejected.
This covers phases, generation scopes, debts/billing, signals, and mandatory cleanup markers.
Temporary facts alone remain permitted: replays use a temporary wild-tag holder during an adjustment;
no idle cleanup happens inside the correction.
Any required part or automatic effect that adds unfinished MustCleanUp state rejects the correction.
Creating a Trade introduces its barrier and is therefore rejected, rather than retaining an
unfinishable trade. Low-level `sneak` deliberately has no such target/result restriction.
The boundary does **not** classify every infrastructure type. Required Party singletons and live
Global Event position dependencies express their own constraints. Persistent action providers
remain editable; removing one is not by itself evidence of a broken representation. A complete explicit correction may name several related facts. Do not infer
a purchase, replay a phase, or manufacture a missing player choice.
`Ok` and `Audit` remain nonpersistent signals; they require no lasting repair.
Abstract role Classes (`Owned`, `TagHolder`, `ResourceHolder`, `HasActions`, and similar bases)
cannot themselves be concrete correction targets; their contracts apply to their concrete subtypes.

## Automatic effects simplified and retained

Pets, Protected Habitats, Asteroid Deflection System, and Nomads use ordinary mandatory
`Die` for gameplay prohibitions. Briber's payment, milestone eligibility, Corridors of Power's draw,
award-position VP, Global Event delegate arrows, UNMI/Pristar history, and the consumption/payout
of Suitable Infrastructure and Frontier Town bonuses also use ordinary effects. The Admin temperature
threshold now queues its ocean request just as other threshold rewards do. Their normal gameplay
scenarios remain the verification requirement; corrections skip them.

Promo attack-history records and solo reserve replenishment also use ordinary effects. Corrections
therefore neither qualify Law Suit or Crash Site Cleanup nor reverse changes to the solo opponent's
standard resources, production, or card resources. Normal attacks still record history, pay Mons
Insurance compensation, and replenish solo reserves. A manual client resolves the queued history
record before selecting compensation caused by that record.

Required state uses existing automatic effects for adjacency, Aridor's watcher fanout, Boom Town's
modifier, parameter completion, colony initialization/floors/activation, and political roles.
St Joseph instead uses an exact dependent invariant. Ruling policies depend on the live ruling and
phase, with automatic installation on either entry.

Retained automatic behavior has these concrete consequences:

| Correction | Additional result |
| --- | --- |
| Add Pharmacy Union | Gain 54 MC; no disease or tag-triggered losses. Normal play still ends at 46 MC. Removing the corporation does not reclaim that starting money. |
| Add TR during Action phase with Preservation Program and no prior attempt | The first corrected TR is canceled and the attempt is recorded. A subsequent TR correction succeeds. PP's initial 5 TR is ordinary and is not granted by correcting the card into play. |
| Lower a completed global parameter | The existing automatic guard rejects the change and the transaction rolls back. |
| Create a trade or another visible component that introduces unresolved barriers | `exMachina` rejects new `MustCleanUp` state; automatic execution does not manufacture the missing gameplay choices. |

These entries follow the retained authored automatic effects; they are not promises that
correcting a count rewrites its whole history.

PP's existing gain-then-remove model still has the separately recorded Terraforming Deal/Reds normal
play defects. They are not reasons to suppress PP's automatic behavior during corrections. Its
watcher/cancellation timing has not acquired another correction-specific exception.

## Ordinary history and pending bonuses

UNMI/Pristar's `HasRaisedTr` is ordinary history. Correcting TR neither sets nor clears it.
PP retains its automatic cancellation and its existing `ActionPhaseTrGain` now counts attempts,
rather than storing only presence. Ordinary history checks run after automatic effects, so a count
of two distinguishes a later gain from the first, canceled attempt without another marker or
processing stage. Earlier Prelude-phase history remains intact.

Suitable Infrastructure's ordinary production reaction consumes its bonus before paying 2 MC.
If several production gains queue reactions before any resolves, subsequent reactions see the
absent bonus and do nothing. A correction does not queue any of those reactions.
Frontier Town's ordinary city-gain reaction consumes its pending bonus before repeating that city's
placement rewards. It does not listen to an ocean placed by a placement reward. There is no separate
cleanup racing that reaction. Correcting a city while the bonus is pending leaves it available for
the actual placement.

Normal card tests and replays verify that ordinary gameplay still performs its consequences.

## Remaining boundaries

PP's separate normal-play Terraforming Deal/Reds defects remain recorded with their existing
characterizations; this correction work does not resolve them.

Direct colony-track decreases and partial delegate removals are accepted correction cases.
Callers can separately correct leadership; the engine need not reconstruct the gameplay operation.
Full validation checks every declared invariant, not every gameplay rule.

## Required-part construction

Creating a component establishes each positive exact-count invariant whose specialized target is
one concrete type directly dependent on that component. `ClassLimitTable.requiredParts` derives
these targets from inherited invariant templates, binding `This` to the complete owner type.
The engine creates only the missing count, waiting for declared dependencies across the complete
structure before dispatching gain reactions. Parts' automatic reactions precede their owners'.
The construction boundary and depth accounting are owned by
[ENGINE.md](ENGINE.md#queries-invariants-and-dead-ends).
It does not remove excess, choose abstract subtypes, or create unrelated missing dependencies.

This rule eliminates generated attachment effects from the card compiler. `cardTags` and catalog
classification read the invariant facts directly. `GrantedResourceValue` is `Cardbound`, expressing
that its Player is its granting card's Player; without that relationship its invariant target was
abstract, and the old generated effect supplied a contextual owner missing from the declaration.
Fake cards and synthetic card fixtures use the same invariant declaration for their printed tags.

- Plain `HAS =1 This` remains a constraint, never an instruction to spawn an absent owner.
- Minimum-only counts, abstract types, refinements, and indirect requirements such as
  `Attached<Bridge<This>>` are constraints without an inferred constructor. Other prerequisites
  must already exist. Existing explicit effects can establish these requirements in normal play.
- Inheritance duplicates collapse; existing sufficient parts are retained. Conflicting requirements
  or constructor chains beyond the engine's recursive execution bound fail atomically.
- The complete required structure exists before its gain reactions. Each normal part gain has
  ordinary event attribution and reactions, once. Automatic reactions can change that structure;
  unrelated siblings have no promised gameplay order.
- Corrections retain automatic part reactions and recursively suppress queued reactions. Removing a required part from a
  surviving owner fails instead of recreating it. Removing the owner removes its dependents without reversing past
  rewards. Recorded playback applies recorded changes without running construction again.

The rule covers all printed tags and the eight exact capability cases listed above. Exact parts do
not solve fanout, changing tracks, or leadership. They also do not make
`HAS =1 UnrelatedType` conditional on the declaring component: without a `This` binding, that is
a global requirement and must hold at bootstrap. Keep the remaining relationships explicit rather
than inferring a general constraint solver from this deliberately limited constructor rule.
