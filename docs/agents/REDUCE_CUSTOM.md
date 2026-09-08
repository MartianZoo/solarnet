# Reducing custom Pets instructions

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** deliberately replacing a custom instruction with
> hand-authored Pets and general runtime semantics.
>
> **Skip when:** just implementing a custom operation or metric, or when no custom is being
> removed.
>
> **Status:** audit.

## Source map

- [`CustomClass.kt`](../../src/common/dev/martianzoo/pets/api/CustomClass.kt) — inspect
  the general extension interface before claiming a custom can be removed.
- [`CustomClassRuntime.kt`](../../src/common/dev/martianzoo/engine/CustomClassRuntime.kt)
  — read when the candidate performs live resolution/execution.

A custom instruction has been eliminated only when its behavior follows from ordinary declarations
and general runtime semantics. Moving the same bespoke behavior into the card generator is worse:
it keeps the custom behavior while hiding it in data conversion. The generator may construct and
render declarations, but it must not invent card-specific responders.

Custom metrics are a separate concern.

## Plausible removals

### `ColoniesSetup`

`ColoniesSetup` was removed this way: its per-player fleet loop is now
`EACH Player { TradeFleet }` in plain Pets. See [EACH.md](EACH.md).

`CreateAdjacencies` was removed after live-component fanout became available. A newly placed tile
fans out over the live neighboring tiles selected by the geometric `Neighbor` metric and creates
the two directed `Adjacency` components in plain Pets. `Neighbor` accepts any tile as its source so
the tile-owned effect remains valid for remote cities, which have no neighbors on the Mars map.

## Customs that should remain

These honestly bridge Pets to canonical metadata absent from the component graph:

- `CopyProductionBox`
- `CopyPrelude`
- `ScoreEventVps`
- `AdjustGpRequirement`
- `HandleCardTags`

Generating card-specific Pets responders would only move these to the worse generation tier.
Colony class declarations and the three resource-delay selections are hand-authored in Pets.

Highest-first `Metric.Rank` now serves both award placement and multiplayer victory, including
competition ties and lexicographic victory-point/MC comparison. Their custom declarations,
registrations, and Kotlin implementations have been removed.

Robinson Industries uses refined production instructions plus `RANK` over the other five production
counts. The five `ProdOffset<Class<MC>>` components compensate for M€ production's stored offset;
the generic resource dependency lets the rank query follow each candidate resource, so tied lowest
production follows from ordinary counts without a custom metric.

After any further removal, delete its custom declaration, registration, implementation, and
custom-interface tests while retaining end-to-end gameplay coverage.
