# Reducing custom Pets instructions

**Status: audit.** `TODO.md` decides whether and when to act.

A custom instruction has been eliminated only when its behavior follows from hand-authored Pets and
general runtime semantics. Moving the same bespoke behavior into
`Definition.asClassDeclaration`, `extraClasses`, or Kotlin-generated Pets is worse: it keeps the
custom code and adds another representation.

Custom metrics are a separate concern.

## Plausible removals

### `ColoniesSetup`

Premise assembly now creates each selected colony tile directly. The remaining custom behavior
creates one reserve fleet per player. It could become ordinary setup signaling:

- base player setup emits `PlayerSetup<This>`; and
- the live Colonies Module responds by creating that player's `ReserveTradeFleet`.

`AddColonyTile` should remain custom because it interprets colony-definition metadata.

### `PassLeft`

A shared seat-topology model such as `LeftOf<From, To>` could let ordinary Pets move a
`StartToken`. Do this only if turn order and every seat-relative rule use the same topology. A
parallel relation invented solely to remove one custom is not an improvement.

## Customs that should remain

These honestly bridge Pets to canonical metadata absent from the component graph:

- `CopyProductionBox`
- `CopyPrelude`
- `GetEventVps`
- `CheckCardDeck`
- `CheckCardRequirement`
- `HandleCardCost`
- `TallyAward`
- `AddColonyTile`
- `CreateAdjacencies`

These perform general selections Pets cannot currently express:

- `AssignAwardPlaces`
- `MultiplayerVictoryCheck`

Revisit the last pair only if one general relational-selection facility serves both. Do not add
isolated ranking syntax to erase their Kotlin implementations.

Robinson Industries already uses ordinary refined production instructions. Its
`LowestProduction` custom metric remains the honest bridge for identifying tied lowest production.

Prototype `ColoniesSetup` first. Consider `PassLeft` only as part of a single seat model. After a
removal, delete its custom declaration, registration, implementation, and custom-boundary tests while
retaining end-to-end gameplay coverage.
