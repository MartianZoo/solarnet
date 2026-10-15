# Authored payment model

> **Status:** current Canon billing and acceptance protocol.
> Client payment helpers, allocation checks, and rollback evidence belong to
> [Solarnet payment integration](../../../docs/agents/PAYMENTS.md).

## Current contract

Players pay through explicit exchanges such as `15 Pay<Class<MC>> FROM MC` and
`3 PayFromCard<Dirigibles> FROM Floater<Dirigibles>`. The removal consumes the resources;
the payment signal credits the debt in the same transaction:

- `Pay<Class<Resource>>` reduces debt in that resource's denomination.
- `ResourceValue` credits M€ debt for an explicit payment of the valued standard resource.
- A card's `PayFromCard<This>` listener credits the appropriate debt for its held resources.

`Pay` defaults its gained resource class to MC, so test commands use the shorter
`15 Pay<> FROM MC`. The amount and source resource remain explicit.

Ordinary resource losses never credit a bill, even when performed by the payer while acceptance
is open. Payment signals are player-owned and are not `System`: choosing a payment remains player
work, and `NONE` does not execute it automatically. The offered exchange fixes the payment resource
or card holder; clients cannot turn a different resource removal into payment credit.

Acceptance begins during pricing. `Owed` installs its denomination, and `PayingFor` or `UseAction`
can install alternatives before `Billing`. Removing the bill removes acceptance before the
purchased card or action results run.

`Owed` is the amount still due, including during price adjustment. `Billing` marks settlement and
carries a reason as a `Class<Component>` dependency, an action slot, and the denomination.
Cards, printed actions, conversions, milestones, and awards use this same type. For example,
`Billing<Class<Builder>>` retains the selected milestone through payment, while
`Billing<Class<Steelworks>, Action1, Class<Energy>>` identifies the printed action and its currency.
The slot defaults to `Action1` and the currency to M€. Currency-only listeners must leave the
reason broad, as in `Billing<Class<Component>, Class<Heat>>`.

`Owed`, `PayingFor`, `Billing`, `Accepting`, and `AcceptingFromCard` are `System` bookkeeping.
Fixed, concrete gains run as Admin, including scaled debt and gains inside automatic effects.
An abstract type or variable amount such as `X Owed` stays with the Player until narrowed; Admin
then performs the concrete gain. Acceptance opens explicit payment exchanges, which remain
assigned to the Player.
`Accepting` offers `X Pay<Class<Resource>> FROM Resource OR Ok` only for a resource other than
the bill's denomination, and only if the payer holds that resource when `Billing` fires. The player
chooses whether to spend, then chooses an exact positive amount. Card-held offers use
`X PayFromCard<Holder> FROM Resource<Holder> OR Ok` only when that holder has resources.
There is no optional quantifier on `X`:
choosing an amount must not leave another choice to spend less than that amount.
Empty alternatives, including card-held resources, are removed by Admin without an explicit decline.
Spending or declining an alternative finishes its acceptance through `THEN -This`; card-held
payment offers do the same.

`Billing` offers its own denomination as an exact mandatory remainder, gated until all alternative
acceptance is gone. For M€ this is `1! Pay<Class<MC>> FROM MC / Owed<Class<MC>>`, with neither
`X` nor `?`. The metric is evaluated when the gated task is selected, after alternatives have reduced
the debt. Full coverage by alternatives leaves `Ok`; positive debt cannot be partially paid or declined. Finishing
that task removes the bill, so purchased results wait for the final payment or acknowledgement.
A bill already at zero when created closes immediately. The same rule lets Stormcraft's floaters
precede an exact Heat remainder. Starting-card billing can precede the corporation's starting cash;
the exact payment remains pending until affordable. Alternative acceptance does not search for
substitutes or reopen if stock arrives later.

With player autoexecution set to `NONE`, the zero-balance acknowledgement also remains explicit.
Skipping such work may be a future autoexecution policy; it must not override `NONE`.
Both fixed and variable standard-resource action costs use billing so acceptance and price
adjustments can apply. A variable cost keeps its shared `X` in the sequence from `Owed` through
`Billing` to the action's result. Choosing `X = 4` for `X Energy -> X MC` opens a bill for 4 Energy;
only after settlement does the sequence offer 4 M€. Energy Market's `2X MC -> X Energy` can
therefore accept Heat through FakeHelion's ordinary billing listener instead of forcing an M€ loss.

## Current pieces

| Piece | Responsibility |
| --- | --- |
| `Owed<Class<Resource>>` | Remaining amount, discounts, surcharges, and non-M€ costs. |
| `PayingFor` and `PriceCard` | Concrete purchase context needed by modifiers before play. |
| `Billing` | Carries the reason Class, action slot, and currency; opens spending choices and marks settlement. |
| `Accepting` | Restricts ordinary resources by payment context. |
| `AcceptingFromCard` | Restricts spending to the correct card holder. |
| `ResourceValue` | Supplies dynamic standard-resource values. |
| `Pay` and `PayFromCard` | Identify explicit spending so only payments credit debt. |

Alternative payment offers must finish before the exact denomination payment. No unused payment
offers remain after normal settlement. Removing an effect owner still does not retract its queued
work; payment completion follows the authored acceptance lifecycle rather than task deletion.

## Source map

- [Payment declarations](../../src/common/dev/martianzoo/tfm/canon/bundles/base/payment.pets)
- [Card model](../../src/common/dev/martianzoo/tfm/canon/bundles/base/card-model.pets)
- [Action lowering](../../src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt)
