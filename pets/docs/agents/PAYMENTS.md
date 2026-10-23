# Authored payment model

> **Status:** current Canon payment-scope and acceptance protocol.
> Client payment helpers and allocation checks belong to
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
work that the client explicitly submits. The offered exchange fixes the payment resource
or card holder; clients cannot turn a different resource removal into payment credit.

`Owed` records the debt but does not install acceptance. A `PaymentScope` starts after debt is
created; its creation announces card and printed-tag pricing and installs available alternative
resources. Removing the scope ends acceptance before the purchased result runs.

`Owed` is the amount still due, including during price adjustment. `PaymentScope` tracks payment
through completion and carries a subject, denomination, and operation. Cards, printed actions, conversions,
milestones, and awards use this same type. For example,
`PaymentScope<Class<Builder>>` retains the selected milestone through payment, while
`PaymentScope<Class<Steelworks>, Class<Energy>, Class<Action1>>` identifies a printed action and
its currency. The operation defaults to `Action1` and the currency to M€. Currency-only listeners
leave the subject broad, as in `PaymentScope<Class<Component>, Class<Heat>>`.

`Owed`, `PayingFor`, `PaymentScope`, `Accepting`, and `AcceptingFromCard` are `System` bookkeeping.
Fixed, concrete gains run as Admin, including scaled debt and gains inside automatic effects.
An abstract type or variable amount stays with the Player until narrowed; Admin then performs the
concrete gain. Acceptance opens explicit payment exchanges, which remain assigned to the Player.
`Accepting` offers `X Pay<Class<Resource>> FROM Resource OR Ok` only for an alternative resource,
and only if the payer holds that resource when `PaymentScope` begins. The player
chooses whether to spend, then chooses an exact positive amount. Card-held offers use
`X PayFromCard<Holder> FROM Resource<Holder> OR Ok` only when that holder has resources.
There is no optional quantifier on `X`:
choosing an amount must not leave another choice to spend less than that amount.
Empty alternatives, including card-held resources, are removed by Admin without an explicit decline.
Acceptance also removes itself when no debt remains.
Spending or declining an alternative finishes its acceptance through `THEN -This`; card-held
payment offers do the same.

`PaymentScope` offers its own denomination as an exact mandatory remainder, gated until all alternative
acceptance is gone. For M€ this is `1! Pay<Class<MC>> FROM MC / Owed<Class<MC>>`, with neither
`X` nor `?`. The metric is evaluated when the gated task is selected, after alternatives have reduced
the debt. Full coverage by alternatives leaves `Ok`; positive debt cannot be partially paid or declined. Finishing
that task removes the scope, so purchased results wait for the final payment or acknowledgement.
A scope created with zero debt closes immediately. The same rule lets Stormcraft's floaters
precede an exact Heat remainder. Starting-card payment can precede the corporation's starting cash;
the exact payment remains pending until affordable. Alternative acceptance does not search for
substitutes or reopen if stock arrives later.

With player autoexecution set to `NONE`, the client explicitly acknowledges a zero balance.
Skipping such work may be a future autoexecution policy; it must not override `NONE`.
Action-cost classification follows declared inheritance from `StandardResource`, including abstract
resource categories, in the assembled catalog before executable lowering. It does not load an extra
class table or maintain a list of resource names.
Fixed standard-resource action costs use payment scopes so acceptance and price adjustments can
apply. Variable costs use ordinary mandatory spending followed by `THEN`, preserving the shared
`X` in that instruction. For example, `X Energy -> X MC` becomes `-X! Energy THEN X MC`, and
Energy Market's `2X MC -> X Energy` becomes `-2X! MC THEN X Energy`. No debt or payment scope is
created for these actions. Supported Canon has no variable-cost action requiring alternative
payment; Helion is explicitly out of scope. `FakeHelionTest` records its unsupported Energy Market
interaction rather than making that stand-in a requirement for action lowering.

## Current pieces

| Piece | Responsibility |
| --- | --- |
| `Owed<Class<Resource>>` | Remaining amount, discounts, surcharges, and non-M€ costs. |
| `PayingFor` | Scope-linked card and printed-tag announcements needed by modifiers before play. |
| `PaymentScope` | Carries the subject, denomination, and operation; opens spending choices and tracks payment through completion. |
| `Accepting` | Restricts ordinary resources by payment context. |
| `AcceptingFromCard` | Restricts spending to the correct card holder. |
| `ResourceValue` | Supplies dynamic standard-resource values. |
| `Pay` and `PayFromCard` | Identify explicit spending so only payments credit debt. |

Alternative payment offers must finish before the exact denomination payment. No unused payment
offers remain after normal payment completion. Removing an effect owner still does not retract its queued
work; payment completion follows the scope's lifecycle rather than task deletion.

## Source map

- [Payment declarations](../../src/common/dev/martianzoo/tfm/canon/bundles/base/payment.pets)
- [Card model](../../src/common/dev/martianzoo/tfm/canon/bundles/base/card-model.pets)
- [Action lowering](../../src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt)
