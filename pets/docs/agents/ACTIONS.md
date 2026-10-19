# Action identity and invocation

> **Agent information:** This is an agent-maintained design note.
>
> **Read when:** changing authored actions, action selection, `UseAction`, `UseCardAction`, or
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
`UseCardAction` forwards the selected slot to `UseAction` before creating the card's
`ActionUsedMarker`.

[`AiCentralTest`](../../../test/common/dev/martianzoo/tfm/tests/cards/AiCentralTest.kt) checks that
selecting AI Central's absent second action fails and leaves its real action available.
`CanonInvariantsTest` checks all concrete `HasActions` classes for all three triggers.

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
