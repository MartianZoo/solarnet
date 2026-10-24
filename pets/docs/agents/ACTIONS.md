# Action identity and invocation

> **Agent information:** This is an agent-maintained design note.
>
> **Read when:** changing authored actions, action selection, `UseAction`, standard actions, or
> action-specific payment identity.
>
> **Status:** absent action slots are rejected by lowered `Die` effects; the replacement
> representation is still a proposal. The [Solarnet roadmap](../../../docs/SOLARNET_ROADMAP.md#internal-design-and-game-execution)
> indexes the broader action lifecycle work.

## Current behavior

Pets lowers the nth authored action to an effect triggered by `UseAction<This, ActionN>`.
Terraforming Mars defines `Action1` through `Action3` as global slots, and its action lowerer fills
unused slots with `Die` effects. Every concrete Canon `HasActions` class has an authored action,
so lowering makes each react to all three slots without repeating the missing slots in source.
Standard actions are `HasActions` providers. The base game creates its fixed providers;
Scientists and Kelvinists policies offer their actions directly while their policies exist.
`StandardAction` imposes no minimum count, so those temporary providers can disappear normally.
For example,
`ConvertPlantsAction { 8 Plant -> PlaceGreeneryTile }` uses the same fixed-cost lowering as a
printed card action: payment completion creates the result. `UseActionOnCardAction` offers
`UseAction<@ActionCard> THEN ActionUsedMarker<@ActionCard>`, retaining the selected card across
its invocation and shared generation marker. Card play and standard projects likewise begin by
selecting their standard action, then selecting the particular card or project.

Milestone and award actions take payment before offering the choice of milestone or award.
Award pricing remains explicit Pets (8 M€ plus 6 per funded award); the player does not choose
a price or an action slot according to the current number of awards. Nirgal's free claims and
funding modify those action bills. Diversifier counts distinct tag types together with unused
wild elections in its requirement; claiming a milestone creates no manual wild-tag choices.

The current `UseAction`/slot invocation is an accepted intermediate. Removing global slots and
introducing paid standard-project signals remain separate later changes. Test command abbreviations
are governed by [Solarnet testing](../../../docs/agents/TESTING.md#command-abbreviations).

[`AiCentralTest`](../../../test/common/dev/martianzoo/tfm/tests/cards/AiCentralTest.kt) checks that
selecting AI Central's absent second action fails and leaves its real action available.
`CanonStructuralInvariantsTest` checks all concrete `HasActions` classes for all three triggers.

## Required behavior

- A player can invoke only an action actually declared by the selected component. Attempting an
  absent action fails without consuming a turn, charging a cost, or marking the component used.
- The rule applies to generated cards and handwritten Pets declarations. Keep other `HasActions`
  users, including standard projects and policies, coherent under the same selection model.
- An action's cost, variable choice, payment, effect, and `This` references remain attached to its
  originating component. All actions on a card still share its once-per-generation use limit.
- The authored action list remains the source of truth. Any derived action identity must be
  generated from that list, not maintained as a second declaration.

## Candidate direction

Derive an identity for each actual authored action, such as `Foo_Action1`, with no identity for
absent slots. A card action could route invocation to its existing card effect while the shared
use marker stays on the card. The identity must be available only when its source component offers
that action; creating class names alone would not prevent a caller from invoking an absent slot.

The exact Pets lowering and live-component relationship remain open. Check the proposal against
handwritten declarations, resource billing, variable `X`, repeated actions granted by other cards,
and non-card `HasActions` before selecting it. Prefer removing the global slot protocol and its
special cases where the derived identities can replace them coherently.
