# Reducing custom Pets instructions

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** deliberately replacing `ColoniesSetup`, `PassLeft`, or another custom instruction
> with hand-authored Pets and general runtime semantics.
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
- [`ColoniesExpansion.kt`](../../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion.kt)
  — search for `ColoniesSetup` for that specific candidate.

A custom instruction has been eliminated only when its behavior follows from hand-authored Pets and
general runtime semantics. Moving the same bespoke behavior into declaration-conversion code,
`extraClasses`, or Kotlin-generated Pets is worse: it keeps the custom code and adds another
representation.

Custom metrics are a separate concern.

## Plausible removals

### `ColoniesSetup`

Premise assembly now creates each selected colony tile directly. The remaining custom behavior
creates one reserve fleet per player.

The `EACH` fanout proposed in [EACHPLAYER.md](EACHPLAYER.md) removes it outright: a prototype
replaced the whole class with `SetupPhase: EACH Player { TradeFleet }` on `ColoniesExpansion`, which
also let `SoloColoniesSetup` shrink to its discard and dropped the `IF MAX 0 SoloColoniesSetup`
gate. That prototype was measured and set aside, so this candidate is still open — but the fanout,
not setup signaling, is the shape to try first.

The signaling alternative, if the fanout is abandoned:

- base player setup emits `PlayerSetup<This>`; and
- the live Colonies Module responds by creating that player's `TradeFleet`.

### `PassLeft`

A shared seat-topology model such as `LeftOf<From, To>` could let plain Pets move a
`StartToken`. Do this only if turn order and every seat-relative rule use the same topology. A
parallel relation invented solely to remove one custom is not an improvement.

## Customs that should remain

These honestly bridge Pets to canonical metadata absent from the component graph:

- `CopyProductionBox`
- `CopyPrelude`
- `ScoreEventVps`
- `CheckCardDeck`
- `AdjustGpRequirement`
- `HandleCardTags`
- `CreateAdjacencies`

Generating card-specific Pets responders would only move these to the worse generation tier.
Colony class declarations and the three resource-delay selections are hand-authored in Pets.

These perform general selections Pets cannot currently express:

- `AssignAwardPlaces`
- `AssignMultiplayerVictory`

Revisit the last pair only if one general relational-selection facility serves both. Do not add
isolated ranking syntax to erase their Kotlin implementations.

Robinson Industries already uses refined production instructions. Its
`LowestProduction` custom metric remains the honest bridge for identifying tied lowest production.

After a removal, delete its custom declaration, registration, implementation, and custom-interface
tests while retaining end-to-end gameplay coverage.
