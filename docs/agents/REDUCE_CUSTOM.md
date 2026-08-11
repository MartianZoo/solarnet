# Reducing custom Pets instructions

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

Custom instructions are a last resort, but replacing one with Kotlin-generated Pets is not an
improvement. Generated Pets derived from JSON definitions is a lower tier than a direct custom
instruction: it retains bespoke code, obscures where the behavior comes from, and makes the
generated declaration another representation to understand and debug.

For this project, an instruction has genuinely ceased to be custom only when its behavior follows
from hand-authored Pets and general existing runtime semantics. Moving behavior into
`Definition.asClassDeclaration`, an `extraClasses` generator, or another definition-specific
lowering pass does not count. Nor should canonical facts be duplicated manually in Pets merely to
avoid an honest Kotlin implementation.

This review covers the 14 canonical `CustomClass.translate` implementations. Custom metrics are a
separate problem.

## Reasonable candidates

### `ColoniesSetup`

In the aspirational premise design, the premise initializes each exact selected colony type and the
generated `FooSelected` class family is removed. `ColoniesSetup` would then retain only the player
loop that creates reserve trade fleets. Replace that remaining translation with ordinary,
composable setup signals:

- the base player rules can emit `PlayerSetup<This>` once for every player;
- the live `ColoniesExpansion` Module can respond to each `PlayerSetup<Player>` by creating that
  player's `ReserveTradeFleet`.

`AddColonyTile` may remain custom because it still interprets colony-definition metadata.

### `PassLeft`

Represent realized seat topology explicitly with components such as `LeftOf<From, To>`. A live
relation can subscribe to `PassLeft<StartToken<From>>` and transmute the matching token to
`StartToken<To>` using ordinary trigger-to-instruction linkage.

The one- through five-player topologies are small and stable enough to author as Pets setup rules.
This is preferable to teaching Pets numeric player-name arithmetic. It is worthwhile only if the
relations also become the shared model used by turn order; they should not be parallel machinery
created solely to remove one custom instruction.

## Customs that should remain

The following implementations honestly bridge Pets to definition metadata which Pets does not
contain:

- `CopyProductionBox`
- `CopyPrelude`
- `GetEventVps`
- `CheckCardDeck`
- `CheckCardRequirement`
- `HandleCardCost`
- `TallyAward`
- `AddColonyTile`

Generating card-, award-, or colony-specific Pets responders would only move these to the worse
generation tier. Hand-authoring copies of JSON facts would create two authorities for the same
rule.

`CreateAdjacencies` should also remain. It traverses the selected map's static grid data. Generated
per-area Pets effects or a second hand-authored adjacency catalog would be less direct than the
current implementation.

The remaining three perform selections the language cannot express:

- `GainLowestProduction` computes an arg-min with ties and the megacredit-production offset;
- `AssignAwardPlaces` ranks players while applying tie and player-count rules;
- `MultiplayerVictoryCheck` performs a lexicographic arg-max and preserves ties.

A future general relational selection facility could justify revisiting all three together. Do not
add isolated `MIN`, ranking, or winner syntax merely to erase their Kotlin objects; the custom
implementations are currently the smaller and clearer mechanism.

## Direction

Prototype `ColoniesSetup` first. Consider `PassLeft` only as part of a single explicit seat-topology
model. Do not expand Pets generation infrastructure as part of this effort. After either change,
remove its custom declaration, registration, implementation, and tests that exist only for the
custom boundary while preserving end-to-end gameplay coverage.
