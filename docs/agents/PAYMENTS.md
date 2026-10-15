# Payment model

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing payment choices, accepted resources, debt, billing, resource values, or
> payment APIs.
>
> **Status:** current contract and verified gaps. Proposed replacements belong in the Solarnet
> roadmap or a focused investigation, not here.

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

The linked `X` choice still appears after `UseCardAction` fires, so `ActionUsedMarker` may exist
while the choice is open. The intended later design would let a client set `X` on the printed
action before committing its use, then perform an ordinary concrete action. That requires action
invocation and its consequences to share the same choice. Inferring `X` backwards when a caller
tries to perform `3 Foo` against `X Foo` is optional and does not justify extra machinery by itself.
A client that already knows `X = 5` can submit `selectTask(task.bindXTo(5))` directly. `TaskForm`
remains optional for discovering open choices. This still selects the pending consequence after
`UseCardAction`; moving the choice to the printed action before invocation remains future work.

## Ordinary resource losses

Pharmacy Union's disease losses and Flooding's self-inflicted loss leave an open bill unchanged.
Their sandbox tests resolve those losses before the explicit payment and verify the full cost.
Pharmacy Union's starting 54 MC is queued behind a zero-disease gate; its microbe reaction places
disease before offering the capped 4 MC loss. Choosing disease before starting cash can leave setup
unable to finish, requiring undo. This is allowed; the model need not make every available choice
lead to completion. No special starting-resource phase or autoexecution exception is used.

## Interleaving and rollback

Accepting Neptunian's offer does not prevent the player playing Flooding from resolving its loss
while the purchase remains unpaid. This ordering is allowed. On 2026-10-14 the owner explicitly
confirmed that making the purchase unaffordable this way is a voluntarily dead-ended turn, not a
bug: the player playing Flooding must roll back.

`FloodingTest` covers losses both before payment and after a separate Steel payment. The loss
never credits the bill. A failed cash payment leaves the earlier Steel payment intact; rolling
back the enclosing turn restores both players' resources and removes the debt. These scenarios do
not require a lock around the purchase or automatic rollback of earlier payment commands.

Payment offers are routed by their effect owner, changed component owner, or triggering Actor.
The `pay` helper selects through each task's current assignee. Calling those Agents is legitimate
calculator use, independently of who operates the external caller.

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
| Kotlin payment helpers | Submit explicit exchanges, decline unused alternatives, and settle the exact remainder. |

Alternative payment offers must finish before the exact denomination payment. No unused payment
offers remain after normal settlement. Removing an effect owner still does not retract its queued
work; payment completion follows the authored acceptance lifecycle rather than task deletion.

The sandbox suite exercises this ordering with explicit submissions. Gameplay helpers suspend
player autoexecution while submitting a specified payment allocation. Under `CONCRETE` or `EAGER`,
a cash-only bill may otherwise finish automatically; callers must not submit that payment again.
Custom allocations belong in the helper's payment callback, before the purchased result.
The script `tfm_pay` command uses the same allocation helper; `tfm_play` and `tfm_action` pause
player autoexecution while submitting a written payment, then restore the configured policy.

## Allocation validation

Pets splits an instruction group into independent tasks. Shared variables can link a `THEN`
sequence, but they do not give a task separately chosen quantities of cash, Steel, Titanium, and
card-held resources. A complete allocation task would require new task semantics or a domain
instruction; it is not a spelling change.

For positive M€ debt, `TfmGameplay.pay` rejects an allocation when a selected unit could be
returned while the remainder still covers the debt. With debt `D`, unit values `v1 ... vn`, and
total `P`, the test is:

```text
P - D < vi for every selected unit i
```

Full payment separately requires `P >= D`. Card-held resources are submitted separately, and raw
engine tasks do not apply the same whole-allocation check. The physical-game
rule for the characterized mixed-metal case is unverified; do not describe current behavior as a
settled rules defect.

## Evidence

Start with:

- [`payment.pets`](../../src/common/dev/martianzoo/tfm/canon/bundles/base/payment.pets)
- [`card-model.pets`](../../src/common/dev/martianzoo/tfm/canon/bundles/base/card-model.pets)
- [`TfmActionLowerer.kt`](../../src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt)
- `PaymentSpecializationTest`, `DirigiblesTest`, `PsychrophilesTest`,
  `KuiperCooperativeTest`, `StormcraftIncorporatedTest`, `BoomTownTest`, and
  `StandardTechnologyTest`

Preserve observable card/action outcomes, discounts, values, source restrictions, rollback, and
settlement before consequences. Do not create mirrored allocation state or card-specific client
orchestration to address a local symptom.
