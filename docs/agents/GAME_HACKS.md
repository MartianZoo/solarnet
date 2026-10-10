# Game hacks

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** checking whether current representations compose truthfully with new cards.
>
> **Status:** historical source audit from 2026-10-02, restored 2026-10-09 at the user's request.
> Entries are investigation leads, not a fresh verification of current behavior. Fan cards below
> are hypothetical counterexamples.
> Most interactions are deductions from source. Temporary JVM probes reproduced entries 20, 21,
> 22, 24, and 25; those probes are not checked-in regression tests.

## Canon invariants checked during the build — 2026-10-09

The user named these checks **Canon invariants** and selected them for build-time characterization,
even where they describe current supported content rather than universal game rules. A failure is a
request to review the invariant and its consumers, not permission to rewrite the card until the
check passes.

[`CanonInvariantsTest`](../../test/common/dev/martianzoo/tfm/canon/CanonInvariantsTest.kt) scans the
assembled Canon, including generated cards and handwritten Pets. It runs in `:tfm-canon:jvmTest`
and therefore in the normal repository `test` and `build` checks. The former runtime card-category
audit now lives in test sources; structural Pets and per-game configuration validation remain live.

| Canon invariant | Scope and reason |
| --- | --- |
| Tags and Active/Automated classification agree with persistent behavior | The former catalog-construction audit; malformed examples remain in `CardClassTest`. |
| Animal holders have Animal tags; Animal-tagged resource cards hold Animals | Both directions hold for current Canon. This concerns storage, not a card granting Animals elsewhere. |
| Microbe holders have Microbe tags | The reverse is false: Pharmacy Union holds Disease and Vermin holds Animals. |
| Resource cards identify a concrete resource; resource-holding projects are Active | Protects the resource-holder model and the generator's inferred role. |
| ActionCard agrees with authored actions; events have no actions or resource storage | Detects roles whose behavior would be lost or misrepresented. |
| Building-card immediate instructions contain at most a single PROD box, without outer conditions, choices, scaling, actor changes or EACH bindings | Entry 12. THEN remains allowed: Mining Area/Rights copy production after their tile already exists. Both the check and CopyProductionBox use `cardProductionBoxes`: only instruction-position PROD blocks count. `Plant / PROD[Energy]` has no copyable box; its PROD is a metric. |
| Instruction and action-cost multipliers that count production capable of including MC account for its offset | Entry 3. After lowering PROD, each count must subtract its matching ProdOffset, or subtract stored production from an offset-adjusted target as Industrial Complex does. Resource and owner arguments must match; merely mentioning an offset is insufficient. New arithmetic forms fail for review. |
| Copied PROD boxes do not use This for the original card | Entry 12. The copier returns the subtree without rebinding This to its source card. |
| Prelude self-gain effects have bare This triggers and do not hide gains in automatic effects | Entry 13. Boom Town's automatic removal installs a persistent penalty that Double Down deliberately does not repeat; its gameplay test covers that distinction. |
| Global-parameter requirements have the simple counting form the adjustment helper accepts | Entry 17. Compound requirements need a helper change before they can safely use adjustments. |
| Event scoring uses bare End; event cards have no other external subscriptions | Entry 18. Played-event records only restore End effects. |
| Card declarations have no untransformed gain subscriptions capable of observing Heat | Entries 1–2. Production triggers are distinct. This does not scan indirect reactions in helper components or claim that the solo replenishment machinery observes no resource gains. |
| Authored declarations contain no same-class tile or colony transmutations | Entries 8–9. Such transfers would repeat gain-triggered placement/building rewards. This does not prove that a sequence of separate removal/gain instructions cannot do the same thing. |
| Tile gains specify placement | `GreeneryTile` gains name an area or use `DefaultGreeneryTile`. Empty `GreeneryTile<>` does not name an area. `CityTile<>` explicitly requests its placement default. |

The card generator additionally rejects multiple inferred resource-storage kinds instead of silently
dropping the ResourceCard role. `GenerateCardPetsTest` supplies a deliberately ambiguous card to
verify the diagnostic. Event classification depends on the presence of EventTag, not its position;
repeated EventTag entries remain invalid.

Do not infer that every production effect on gaining a Building-tagged card is a copyable printed
production box: Tharsis Republic's conditioned solo compensation is a counterexample (entry 5).
The syntax checks above do not prove the completeness of immediate-effect extraction.

Indirect special-tile behavior (entry 15), nested bills, watcher lifetimes, finite solo
reserves, and lost historical facts are not proven by these checks. They need further source review
and focused interaction tests; no effect-reachability analysis was added. The historical entries
below retain their original audit scope and evidence unless specifically revisited above.

## Intentional distinction: Philanthropist and Vitor

Confirmed by the user on 2026-10-09: these select different properties of a card by design.

- **Philanthropist** (`Philantropist` in Pets) uses `GainsOf<Class<VictoryPoint>>` to look for
  potentially positive VP gains in authored effect instructions. The gain can be conditional;
  the helper does not require that it will actually award VP in the current game state.
- **Vitor** uses `NonNegativeIconsOf<Class<VictoryPoint>>` as an icon proxy. It counts type
  citations in the effects returned by `cardEffects`, including triggers and conditions, rather
  than requiring a gaining instruction. Citations on the removal side of a change are excluded;
  the existing negative-only card test records that distinction.

For example, `VictoryPoint: MC` cites the VP type without gaining VP. Its different treatment by
these helpers is intentional, not evidence that the card is malformed. The Canon invariant banning
VP references outside changes has been removed. Former entries 14 and 16 conflated the selection
criteria and have been withdrawn; remaining entry numbers are preserved for existing references.

Both helpers inspect authored effects; neither difference in selected cards nor lack of indirect
runtime-effect traversal is by itself evidence of a defect in these two selectors.
See [TfmEngine.kt](../../src/common/dev/martianzoo/tfm/engine/TfmEngine.kt),
[cardEffects](../../src/common/dev/martianzoo/tfm/canon/cardClass.kt), and
[VitorTest](../../test/common/dev/martianzoo/tfm/tests/cards/VitorTest.kt).

## Inclusion rule

A **truthful** implementation makes the game changes its rules actually call for. Another card
can observe them without needing special knowledge of its implementation. Rejecting an illegal
operation by rolling back its changes, consequences, and history counts as truthful: Protected
Habitats does not leave a successful history containing the prohibited theft.

Compensating changes are different. Gaining something and later removing it leaves two real events
in a successful history. Observers can distinguish that from never gaining it.

This list covers latent composition hazards in the current supported model. Already demonstrated
incompatibilities between existing cards are outside its scope. Normal end-game VP scoring is
correct, not a hack. Coordinates, support components, and custom Kotlin are not inherently hacks
either: every entry below identifies a particular false equivalence or lost fact.

Fan-card names and rules are illustrative; costs and balancing are omitted. **Literal** means the
fan card's relevant effect is a direct gain, removal, transfer, condition, or reaction. Some entries
instead require a missing observation, or expose an assumption about how a truthful card is written.
Pets fragments are implementation sketches, not compilation claims. Related repair work remains
in [TODO.md](../../TODO.md) and the owning design documents; this inventory does not authorize fixes.

## Resource bookkeeping and narrow watchers

### 1. Supercapacitors converts retained energy into heat and back

**Hack:** energy executes `ProductionPhase:: Heat FROM This`; Supercapacitors offers
`-Energy IF ProductionPhase: Energy FROM Heat?`. Retention leaves an energy loss, heat gain,
heat loss, and energy gain in history.

**Fan card — Heat Brokerage:** “Whenever you gain any amount of heat, gain 1 M€.” **Literal:**
`X Heat: MC`. Preserving energy pays the broker even though no heat should have been gained.

**Source:** `Energy` in [resources.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/resources.pets)
and `Supercapacitors` in [promo cards](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5).
[SupercapacitorsTest](../../test/common/dev/martianzoo/tfm/tests/cards/SupercapacitorsTest.kt) verifies
retained quantities, not equivalence under heat listeners.

### 2. Stormcraft manufactures heat for Local Heat Trapping

**Hack:** this particular card bypasses heat billing. Stormcraft removes one, two, or three floaters
and creates two, four, or five heat for Local Heat Trapping to remove. Ordinary Stormcraft heat
payments do not manufacture heat.

**Fan card — Heat Brokerage:** the same literal heat-gain reward exposes this separate mechanism:
paying with floaters earns money for invented heat. A heat-removal reward would also see too much
heat spent; three floaters can supply all five heat without any real heat resources being spent.

**Source:** `StormcraftIncorporated` in [Colonies cards](../../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/cards.json5)
and [StormcraftIncorporatedTest](../../test/common/dev/martianzoo/tfm/tests/cards/colonies/StormcraftIncorporatedTest.kt).

### 3. The M€ production floor produces artificial income and losses

**Hack:** five `ProdOffset<Class<MC>>` create five ordinary production components and remove five
M€ each production phase. Counts are offset by five; income and removals compensate each other.

**Fan card — Emergency Reserves:** “For each M€ you lose during production, gain 1 heat.”
**Literal:** `-MC IF ProductionPhase: Heat`. With printed M€ production zero, it should receive
nothing from that track. Instead it sees the five offset removals and pays five heat.

Raw production thresholds also need offset-aware arithmetic, but that is the same representation,
not another entry.

**Source:** `ProdOffset` and `Production` in [resources.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/resources.pets),
and `Player` in [game.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/game.pets).

### 4. The solo opponent's unlimited backing is a finite stock

**Hack:** resources and production are initialized to 42 and replenished after player changes.
That implements ordinary attacks, but a sufficiently large single mandatory transfer must succeed
before its replenishment can happen.

**Fan card — Asset Seizure:** “Steal 10 M€ from an opponent for each Earth tag you have. In solo,
gain the full amount.” **Literal:** a scaled resource transfer, with the usual solo backing
expected to supply it. Five Earth tags require 50 M€; immediately after setup the backing stock
has only 42. An explicit solo branch would repair this fan card by knowing about the shortcut.

This example gives a determinate solo outcome rather than pretending that a neutral opponent has
an official finite wealth to count.

**Source:** `SoloStandardResourceReserve` in [solo.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/solo.pets).
[CanonClassesTest](../../test/common/dev/martianzoo/tfm/tests/rules/CanonClassesTest.kt) exercises the
backing stocks and their replenishment.

### 5. Tharsis receives a substitute payout for solo cities placed too early

**Hack:** neutral cities are placed in `SetupPhase`, before the corporation. Tharsis compensates
with `This IF SoloMode, CorporationPhase: PROD[2 MC]` instead of observing their placements.

**Fan corporation — Urban Surveyors:** “Whenever any city is placed on Mars, gain 1 steel.”
**Literal:** `CityTile<Anyone, MarsArea>: Steel`. As the original solo corporation it should see
the two neutral city placements in the intended setup order. It sees neither; only Tharsis has the
substitute payout. This is the setup-order issue already tracked in TODO, generalized to a new
corporation rather than another special payout.

**Source:** `PlaceNeutralTiles` in [solo.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/solo.pets),
`TharsisRepublic` in [base cards](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/cards.json5),
and [TharsisRepublicTest](../../test/common/dev/martianzoo/tfm/tests/cards/TharsisRepublicTest.kt).

### 7. L1 Trade Terminal's quota counts unrelated resource gains

**Hack:** `L1GiftWatcher` creates up to three `L1Gift` allowances and consumes one on **every**
`CardResource` gain while it is alive. It does not identify gains made by the Terminal's instruction.

**Fan card — Microbial Symbiosis:** “Whenever you gain a microbe, gain an animal.” **Literal:**
`Microbe: Animal`. With suitable holders, adding a Terminal microbe also adds an animal. The bonus
animal consumes another Terminal allowance. A valid three-card distribution can then dead-end,
or the player can satisfy the counter with fewer Terminal gifts than the card requires.

**Source:** `L1TradeTerminal` and its `L1GiftWatcher` component in
[Prelude 2 cards](../../src/common/dev/martianzoo/tfm/canon/Prelude2CardPack/cards.json5),
and the L1 scenarios in [L1TradeTerminalTest](../../test/common/dev/martianzoo/tfm/tests/cards/L1TradeTerminalTest.kt).

## Acquisition events standing in for particular game actions

The next three cases share a design assumption, but affect distinct existing rule owners. Their fan
cards change ownership while keeping the object in play. Ownership transfer is the intended new
rule; replaying construction or immediate benefits is the unintended consequence.

### 8. Changing a tile's owner looks like placing it again

**Hack:** the new owner's tile component is gained, and tile gains trigger placement bonuses and
intrinsic placement effects. The model has no distinction here between a new physical tile and an
existing tile with a different owner.

**Fan card — Land Purchase:** “Take ownership of an opponent's greenery. Leave it on its hex;
this is not a tile placement.” **Literal:** transfer only the greenery's owner dependency. With
oxygen still incomplete, `GreeneryTile.This` raises oxygen again; the area's tile listener also
issues another `Placement` for an already occupied hex.

**Source:** `MarsArea`, `Tile`, and `GreeneryTile` in
[board.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/board.pets).

### 9. Changing a colony's owner looks like building it again

**Hack:** colony-tile placement benefits listen to `Colony<This>` gains, including gains caused by
an ownership transfer.

**Fan card — Colonial Acquisition:** “Take ownership of an opponent's colony without building a
new colony or receiving its placement bonus.” **Literal:** transfer the `Colony` owner. Transferring
a Ceres colony nevertheless grants steel production; Europa offers another ocean. Poseidon's
`Colony<Anyone>` listener also treats this as a newly built colony.

**Source:** `Ceres`, `Europa`, and `Colony` in
[colonies.pets](../../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/colonies.pets), and `Poseidon`
in [Colonies cards](../../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/cards.json5).

### 10. Changing a played card's owner replays its immediate instructions

**Hack:** an immediate instruction is an effect on gaining the card component. Acquiring an
already-played card therefore executes it again.

**Fan card — Patent Acquisition:** “Take ownership of an opponent's green card. Do not replay its
immediate effects; existing production is unchanged.” **Literal:** transfer an `AutomatedCard` owner.
Acquiring Mine grants the new owner another steel production, despite the fan card explicitly
leaving production unchanged. This is more than another card reacting to the acquisition: Mine's
own immediate instruction repeats.

**Source:** `immediateToEffect` in [Transforming.kt](../../src/common/dev/martianzoo/pets/Transforming.kt),
`CardFront` in [card-model.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets),
and `Mine` in [Corporate Era cards](../../src/common/dev/martianzoo/tfm/canon/CorporateEraExpansion/cards.json5).

### 11. An action-use event occurs before its cost succeeds

**Hack:** `UseAction` starts the cost and result, rather than recording successful payment of the
cost. Another listener can provide the very resources needed to make the attempted action legal.
Rollback does not help when those resources turn the whole operation into a successful one.

**Fan card — Microbial Recovery:** “After you use an action on a microbe card, add one microbe to
that card.” Its reward is a **literal resource gain**, but the available `UseAction` hook is early.
With only one microbe on Regolith Eaters, the fan reward can run before the two-microbe cost of its
oxygen action. That action should be unavailable; the refund should arrive only after paying.

**Source:** `actionToEffect` in [Transforming.kt](../../src/common/dev/martianzoo/pets/Transforming.kt),
`RegolithEaters` in [base cards](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/cards.json5),
and the current-divergence account in [SEQUENCING.md](SEQUENCING.md#the-missing-rule-when-an-operation-is-over).

## Inferring printed rules from a limited Pets spelling

Here the exposing card can behave literally on its own. The old helper misreads its declaration.
These are authoring restrictions, not claims that every card using custom code is fraudulent.

### 12. Robotic Workforce / Cyberia require exactly one production subtree

**Hack:** `CopyProductionBox` extracts exactly one immediate `PROD` subtree and loses surrounding
conditions; it rejects multiple subtrees and `PROD` nested inside `EACH`.

**Fan card — Adaptive Factories:** a Building card whose production box says “If you have at least
three Building tags, increase steel production 1; otherwise increase energy production 1.”
**Literal spelling:** gated alternatives containing `PROD[Steel]` and `PROD[Energy]`. Playing the
card can follow the printed box exactly, but copying finds two `PROD` nodes and fails. Rewriting the
condition inside one `PROD[...]` is an accommodation to the copier.

**Source:** `CopyProductionBox` in [TfmEngine.kt](../../src/common/dev/martianzoo/tfm/engine/TfmEngine.kt).
[CopyProductionBoxTest](../../test/common/dev/martianzoo/tfm/tests/rules/CopyProductionBoxTest.kt) makes
one of the required spelling restrictions explicit.

### 13. Double Down recognizes only nonautomatic immediate effects

**Hack:** `CopyPrelude` uses `cardImmediate`, which selects bare `This` effects only when they are
not automatic. A choice-free immediate benefit written with `::` disappears from the copy.

**Fan Prelude — Guaranteed Funds:** “Gain 18 M€ and 2 steel.” **Literal spelling:**
`This:: 18 MC, 2 Steel`. The Prelude really gives those resources, but Double Down finds no immediate
instruction to copy. The same gain written with `This:` would work. The fan rule is ordinary;
which colon the author used is not a printed distinction.

**Source:** `cardImmediate` in [cardClass.kt](../../src/common/dev/martianzoo/tfm/canon/cardClass.kt)
and `CopyPrelude` in [TfmEngine.kt](../../src/common/dev/martianzoo/tfm/engine/TfmEngine.kt).

### 15. Astra Mechanica only detects directly authored special-tile gains

**Hack:** `GainsOf` traverses the card's own authored instructions. It does not follow a signal's
consequences to discover that the card places its printed special tile.

**Fan event — Memorial Mission:** “Place this card's special tile. Gain 2 plants.” A **truthful
composed implementation** invokes a placement signal whose effect gains the actual special tile,
and gains two actual plants. Astra Mechanica sees the signal gain but no `SpecialTile` gain on the
event declaration, so it can recover an event its special-tile restriction should exclude.

**Source:** `GainsOf` in [TfmEngine.kt](../../src/common/dev/martianzoo/tfm/engine/TfmEngine.kt) and `AstraMechanica` in
[promo cards](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5).

### 17. Requirement adjustments only understand one simple parameter requirement

**Hack:** `GpRequirementShortfall` expects one counting requirement with one count metric. It
returns zero for a compound requirement, even if part of it is an ordinary global-parameter minimum.

**Fan card — Temperate Laboratory:** “Requires oxygen 8% and two Science tags. Increase energy
production 2.” **Literal:** a conjunction of the two requirements followed by `PROD[2 Energy]`.
With Inventrix, oxygen 6% and two Science tags should suffice. Ordinary evaluation rejects the
printed conjunction, and the shortfall helper cannot expose its adjustable oxygen part.

**Source:** `GpRequirementShortfall` in [TfmEngine.kt](../../src/common/dev/martianzoo/tfm/engine/TfmEngine.kt),
`PlayCard` in [card-model.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets),
and `Inventrix` in [base cards](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/cards.json5).

### 18. Played-event scoring recognizes only an exactly bare End trigger

**Hack:** played events leave play and become `PlayedEvent` records. `ScoreEventVps` restores only
instructions whose trigger equals parsed `End`; a conditioned End trigger is a different object.

**Fan event — Wetlands Treaty:** “Gain 3 plants. At game end, score 2 VP if you own at least three
greeneries.” **Literal scoring declaration:** `End IF 3 GreeneryTile: 2 VictoryPoint`. Even when the
condition is satisfied at game end, this effect is absent from the scoring helper's selected list.
Writing `End: (3 GreeneryTile: 2 VictoryPoint)` would accommodate the helper instead.

**Source:** `ScoreEventVps` in [TfmEngine.kt](../../src/common/dev/martianzoo/tfm/engine/TfmEngine.kt),
`EventCard` and `PlayedEvent` in [card-model.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets),
and conditional-trigger structure in [Effect.kt](../../src/common/dev/martianzoo/pets/ast/Effect.kt).
This is a limitation of restoring an event's scoring rule, not a criticism of scoring at End.

## Deliberately omitted card information

### 19. Anonymous card locations omit printed traits before play

**Hack/boundary:** the World records anonymous counts and movements through `Hand`, `Selecting`,
and `Revealed`, but does not know an unplayed card's identity or printed traits. Public Plans emits
a reveal-from-hand movement that other rules can observe. Fixed offers count rejected backs; searches
do not count cards skipped before finding the matching card. External claims of printed traits can
produce the intended result, but another rule cannot independently inspect those traits.

**Fan card — Exobiology Contract:** “Reveal an Animal-tagged card from your hand to gain 2 M€.”
The hand count and locations cannot establish eligibility, even when the named card was previously
selected by an Animal-tag search. External verification could implement the rule, but the World does
not retain the fact needed to compose it internally.

This illustrates the remaining information loss for unplayed cards.

**Source:** `PublicPlans` in [promo cards](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5),
`CardBack` and `SearchForCard` in [card-model.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets),
and the owning design in [CARD_HANDLING.md](CARD_HANDLING.md).

## Watcher scope, political state, and payment boundaries

### 20. Frontier Town triples the first intervening placement

**Hack:** `FrontierTownBonus` watches the next `Placement`, rather than the placement of Frontier
Town's city. The watcher exists before the energy-production decrease and deletes itself on its
first match. Another card's reaction can consume it before the city is placed.

**Fan card — Decommissioning Survey:** “Whenever your energy production decreases, place an
ocean.” **Literal:** `PROD[-Energy]: OceanTile<>`. Resolve that ocean after Frontier Town's
production decrease but before its city. The ocean receives the extra two copies of its placement
bonus, and the city receives only its ordinary bonus.

A distinguishing scenario uses the two-steel ocean space Tharsis (1,2) and the one-titanium city
space (8,9): the shortcut produces **6 steel and 1 titanium**, rather than **2 steel and 3 titanium**.

**Source:** `FrontierTown` and `FrontierTownBonus` in
[Prelude 2 cards](../../src/common/dev/martianzoo/tfm/canon/Prelude2CardPack/cards.json5),
`RepeatPlacementBonus` in [TfmEngine.kt](../../src/common/dev/martianzoo/tfm/engine/TfmEngine.kt), and
[Tharsis map](../../src/common/dev/martianzoo/tfm/canon/TharsisMap/tharsis.pets).

### 21. Delegate removal relies on Banned Delegate to repair leadership

**Hack:** ordinary delegate removal clears a party leader only when that owner has no delegates
left in the party. Replacing a leader who still has delegates but has lost the majority is handled
inside `BannedDelegateRemoval`, rather than by the shared delegate rule. Dominance is recalculated
on general removals; leadership is not fully recalculated there.

**Fan card — Political Retreat:** “Action: Return two of your delegates from one party to your
reserve to gain 4 M€.” **Literal:** remove the two placed delegates, then gain the money. Start
with three of your delegates and two opposing delegates in that party. After the action you have
one, your opponent has two, but you incorrectly remain party leader. That also affects subsequent
leadership benefits and scoring.

**Source:** `PartyDelegate` and `PartyLeader` in
[Turmoil classes](../../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/classes.pets), and
`BannedDelegateRemoval` in Banned Delegate's `components` in
[Turmoil cards.json5](../../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/cards.json5).

### 22. Political influence is a snapshot, not a live quantity

**Hack:** `ChairmanInfluence`, `PartyLeaderInfluence`, and the other influence components are
created on `MeasureInfluence`. They do not continuously follow the political position. The snapshot
survives until generation cleanup. Each `MeasureInfluence` clears that player's previous snapshot
before influence providers rebuild it; Politician requests this refresh for every player at scoring.

**Fan card — Policy Consultants:** “Action: Gain 2 M€ for each influence you currently have.”
**Literal reward:** `-> 2 MC / Influence`. In an action phase before measurement, the chairman can
have no `Influence` components and receive zero. Reusing a previous snapshot would instead risk
paying for a political position the player no longer holds. The fan card would need to reconstruct
current influence or know to invoke the snapshot machinery.

This concerns current political influence, not victory points existing before final scoring.

**Source:** `MeasureInfluence`, `Influence`, and its providers in
[Turmoil classes](../../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/classes.pets), and
`Politician` in [goals.pets](../../src/common/dev/martianzoo/tfm/canon/MilestonesAwardsExpansion/goals.pets).

### 23. A funded award retains no funder

**Hack/boundary:** `Award` is unowned. Funding creates the award after charging the player, but
retains no queryable relation between the award and the player who funded it. The event log can
supply historical evidence to a human; ordinary card conditions and counts cannot query it.

**Fan card — Civic Patronage:** “Gain 4 M€ for each award you have funded.” The monetary gain is
**literal**, but playing the card after awards were funded requires a fact missing from the game
components. Counting all `Award` components would pay for other players' funding too. A watcher
installed when Civic Patronage enters play cannot recover the earlier funding.

**Source:** `Award` in [scoring.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/scoring.pets)
and `FundAward` in [actions.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/actions.pets).

### 24. Nested bills share debt and accepted resources

**Hack:** `Owed<Class<MC>>` and `Accepting` belong to the player, not a particular bill. Distinct
billing components can coexist, but their debts accumulate in the same balance, and the same
accepted resources can settle it. This assumes payment cannot open another differently restricted
payment before the first finishes.

**Fan card — Smelter Gardens:** “Whenever you pay with any amount of steel, you may pay 1 M€ to
gain a plant.” A **literal** reaction offers a separate `1 MC -> Plant` payment, as existing optional
paid effects do. Activate it while paying for a Building card with steel: the additional M€ debt
joins the card's debt, where steel is already accepted. The fee can therefore be covered with steel
even though the fan effect grants no such permission. The probe bought Mine and gained the plant
using three steel and zero M€: one steel toward Mine, then two steel against the combined balance.

**Source:** `Owed`, `Billing`, `Accepting`, and `ResourceValue` in
[payment.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/payment.pets), and action
billing in [TfmActionLowerer.kt](../../src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt).

## Audit limits

The full canon pass covered all **536 definitions in the eight card-data files**, the accompanying
Pets declarations, all seven maps and their milestones/awards, solo and beginner setup, colonies,
Turmoil and global events, card introspection and custom instructions, payments, action lowering,
and the phase/turn workflow. Shared change and effect dispatch were checked where the examples
depend on them. Repeated ordinary resource/production instructions and coordinate/bonus data were
reviewed as families, not counted as separate hacks.

This is a source-backed inventory of latent hazards, not a proof that every other card is truthful.
Most fan interactions remain source-derived predictions. Five temporary JVM scenarios, run through
`:tfm-tests:jvmTest`, reproduced the failures in entries **20, 21, 22, and 24**, plus a since-corrected
inherited-tag pricing issue (former entry 25). The final probe
run passed all five characterizations: wrong placement bonus, stale party leader, absent current
influence, steel paying the nested M€ fee, and inherited tags missing from pricing. These demonstrate
the shortcuts at the time of the audit; that audit changed no gameplay code or permanent tests.

Source links and the scenarios identify each shortcut so a later regression test can distinguish
the intended outcome from the predicted failure. Do not promote a
suspicious implementation into this list without a reasonable exposing card and a concrete mismatch;
do not keep an entry here once an existing-card incompatibility already establishes the defect.
