# Game hacks

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** touching one of the specifically named representations below, or when Pets meaning
> appears deliberately unlike the published game even though supported outcomes match.
>
> **Skip when:** investigating a defect with no listed representation. Search
> [`TODO.md`](../../TODO.md) and the relevant tests instead.
>
> **Status:** current inventory of deliberate representations.

This is the ranked inventory of deliberate representations whose supported outcomes match the
official game, but whose Pets meaning is technically different. Each creates an authoring rule:
content that ignores the rule can expose the representation. Known incorrect behavior and intended
repairs belong in [`TODO.md`](../../TODO.md), not here; payment allocation is documented in
[`PAYMENTS.md`](PAYMENTS.md).

## Cross-cutting representations

### 1. M€ production is stored five above the printed value

[`GrossHack`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets) gives every
player five M€-production components, then removes five M€ during each production phase. This
represents the printed -5 floor without negative component counts.

Consequently, raw `PROD[MC]` counts production assets above the minimum, not the signed
track value. This is harmless for comparisons such as Banker because adding five preserves order.
A threshold or displayed value must translate from the internal count, as Specialist does. A
universal saturating `PositiveMoneyProd` would not be safer: it would erase distinctions that
Banker must retain.

### 2. The solo opponent has replenished backing stocks, not possessions

Solo rules make neutral resources and production available whenever an attack needs them. Pets
backs that capability with resources owned by `SoloOpponent` and compensating Admin changes after
a player changes the stock. Current source still spells that Actor `Engine`.

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

### 6. Neutral solo greeneries briefly use normal greenery behavior

Solo setup places a neutral greenery without raising oxygen. Pets places a real greenery,
allowing its normal oxygen step, and then removes that step. Neutral setup must therefore finish
before user-contributed effects capable of observing oxygen changes can exist.

### 7. Colony production tracks count advances, not printed spaces

A physical colony track has seven spaces and begins on its first space. Pets starts with one
`ColonyProduction` component and caps the count at six: the count represents advances above the
starting space, not the visible space ordinal.

Any authored requirement, metric, or effect that refers to a colony marker's visible position must
translate between the one-based printed space and this zero-based advance count.
