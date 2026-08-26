# Game hacks

> **Read when:** touching one of the specifically named representations below, or when Pets meaning
> appears deliberately unlike the published game even though supported outcomes match.
>
> **Skip when:** investigating an ordinary defect with no listed representation. Search
> [`TODO.md`](../../TODO.md) and the relevant tests instead.
>
> **Status:** current inventory of deliberate representations plus one known payment defect.

This is the ranked inventory of deliberate representations whose supported outcomes match the
official game, but whose Pets meaning is technically different. Each creates an authoring rule:
content that ignores the rule can expose the representation. Known incorrect behavior and intended
repairs belong in [`TODO.md`](../../TODO.md), not here.

## Known incorrect behavior

### Payment allocation is not yet auditable

`Pay` and `PayFromCard` remove the selected resources immediately. Their automatic effects then
remove as much `Owed` as remains. A same-denomination `Pay` removes one per resource; substitute
rules provide their exchange values, such as two for Steel and three for Titanium; and Advanced
Alloys, Phobolog, Psychrophiles, and Kuiper Cooperative supply further value. Once the debt reaches
zero, later value simply has no component to remove. The invoice settles successfully even when the
selected resource combination therefore spends more value than the rules permit.

The event history retains the resource spend and each actual `Owed` removal, but it contains no
explicit amount of excess value and no gameplay state that can reject the complete allocation.
`TfmGameplay.pay` rejects common excess payments before submitting them, but direct task selection
can bypass that client check. The known Space Elevator scenario in `BugsTest` demonstrates this.

Attribution is also unreliable at the final units of debt. A signal's self-effect, where applicable,
is handled before effects owned by other components; multiple exchange-value effects receive only
whatever `Owed` remains when each happens to execute. Automatic-effect registration order is not a
game rule. A future payment model should record every contribution in full and validate the
completed allocation before consuming the exact debt. The concerns and candidate designs are
developed in [`PAYMENTS.md`](PAYMENTS.md).

Inspect [`TfmGameplay.kt`](../../tfm-engine/src/commonMain/kotlin/dev/martianzoo/tfm/engine/TfmGameplay.kt)
at `fun pay` for the current client check, and
[`PaymentSpecializationTest.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/rules/PaymentSpecializationTest.kt)
for systemic payment behavior.

## Cross-cutting representations

### 1. M€ production is stored five above the printed value

[`GrossHack`](../../tfm-canon/src/commonMain/resources/canon/bundles/TerraformingMars/classes.pets) gives every
player five M€-production components, then removes five M€ during each production phase. This
represents the printed -5 floor without negative component counts.

Consequently, raw `PROD[MC]` counts production assets above the minimum, not the signed
track value. This is harmless for comparisons such as Banker because adding five preserves order.
A threshold or displayed value must translate from the internal count, as Specialist does. A
universal saturating `PositiveMoneyProd` would not be safer: it would erase distinctions that
Banker must retain.

### 2. The solo opponent has replenished backing stocks, not possessions

Solo rules make neutral resources and production available whenever an attack needs them. Pets
backs that capability with resources owned by `SoloOpponent` and compensating Engine changes after
a player changes the stock.

Removing, stealing, and converting from this stock are supported: their player-facing results are
real. Content must not enumerate or score the solo opponent's resource or production components,
subscribe to its gains or removals, or assume a finite capacity. This restriction does not cover
neutral tiles, which are genuine board state.

### 3. Stormcraft converts floaters into temporary heat for Local Heat Trapping

The official Stormcraft ability spends floaters as heat. Local Heat Trapping does not use the
ordinary payment machinery, so Pets removes the chosen floaters and creates the equivalent heat
inside that operation before the card consumes it.

While Stormcraft is loaded, authored content must not react to heat gained or removed during Local
Heat Trapping. Such an observer would see bookkeeping that the physical game never performs.

### 4. Victory points are a scoring ledger, not gameplay events

End-game effects materialize `VictoryPoint` components so all score sources can be totaled through
one mechanism. The physical game instead calculates those points from their sources.

Content may count the completed ledger, but must not trigger on victory-point gains or removals.
Scores derived from other scores also require an explicit completed-scoring phase; reaction or
task enumeration order is not a game rule.

## Deliberate vocabulary differences

### 5. A copyable production box must be one `PROD[...]` subtree

Robotic Workforce and Cyberia Systems copy a card's printed production box. The Kotlin-backed
`CopyProductionBox` finds exactly one `PROD` subtree in the card's immediate instruction and
rejects zero or multiple matches.

Any new card eligible to be copied must encode its entire printed production box as one `PROD[...]`
group, even when equivalent independent production instructions would otherwise be valid Pets.

## Initialization and coordinates

### 6. Neutral solo greeneries briefly use ordinary greenery behavior

Solo setup places a neutral greenery without raising oxygen. Pets places an ordinary greenery,
allowing its normal oxygen step, and then removes that step. Neutral setup must therefore finish
before user-contributed effects capable of observing oxygen changes can exist.

### 7. Colony production tracks count advances, not printed spaces

A physical colony track has seven spaces and begins on its first space. Pets starts with one
`ColonyProduction` component and caps the count at six: the count represents advances above the
starting space, not the visible space ordinal.

Any authored requirement, metric, or effect that refers to a colony marker's visible position must
translate between the one-based printed space and this zero-based advance count.
