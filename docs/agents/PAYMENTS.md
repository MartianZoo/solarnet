# Payment model

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing payment choices, accepted resources, debt, billing, resource values, or
> payment APIs.
>
> **Status:** current contract and verified gaps. Proposed replacements belong in the Solarnet
> roadmap or a focused investigation, not here.

## Current contract

Players pay through ordinary removals such as `-13 MC` and `-3 Floater<Dirigibles>`. There are no
`Pay` or `PayFromCard` signals. A loss performed by the payer is the payment event:

- `Accepting` reduces debt in the resource's own denomination.
- `ResourceValue` reduces M€ debt when that standard resource is accepted.
- A card's loss listener reduces the appropriate debt while `AcceptingFromCard<This>` exists.

Every payment-loss trigger uses `BY Me@`: the event Actor must be the payer who owns the accepted
resource. This is independent of who controls the surrounding task. An opponent's removal cannot
pay the victim's bill; a delegated payer's own removal can.

Here “performed by the payer” means submitted through the payer's Agent. The engine does not
authenticate the caller or ask whether the real player consented to the spending. A local program
may call both players' Agents. [ADVERSARIAL.md](ADVERSARIAL.md) assigns verification of submitted
decisions to the surrounding game arrangement; payment arithmetic and Actor attribution remain
engine responsibilities.

Acceptance begins during pricing. `Owed` installs its denomination, and `PayingFor` or `UseAction`
can install alternatives before `Billing`. Removing the bill removes acceptance before the
purchased card or action results run.

`Owed` is the amount still due, including during price adjustment. `Billing` marks settlement and
contains the denomination. `ActionBilling` also carries the provider and action slot used by
action-specific rules. `CardBilling` has no card identity: the pending play instruction already
retains the card.

`Owed`, `PayingFor`, and `Billing` are `System` bookkeeping. Fixed gains run as Admin,
including scaled debt and gains inside automatic effects. A variable amount such as `X Owed`
stays with the Player until they choose the amount; Admin then performs the concrete gain.
The resulting payment choices remain assigned to the Player, whose resource removals retain
that Player as their event Actor.

## Verified gaps

`BY Me@` identifies the performer, not the purpose of a loss. If Flooding targets its own player
while that player's Neptunian bill is open, the 4 M€ loss incorrectly reduces that bill. Search
[`FloodingTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/FloodingTest.kt) for the characterized
case. Do not add transaction identity for hypothetical concurrent invoices without a selected
systemic rule.

Delegated payment also exposes a routing and completion question. P1 can hand a Neptunian decision
to P2, but current selection delegates only that selected task. Admin opens the fixed bill;
its payment offers still have P1 as controller and hand off to P2 on selection. The `pay` helper
bridges the gap by selecting through each task's current assignee. Calling those Agents is legitimate
calculator use. A helper-driven scenario can succeed without establishing which selections each
Agent requires or whether intervening work changes the
payment result; inspect those game semantics separately from authorization of the external caller.

`FloodingTest` characterizes intervention immediately after acceptance and after a separate Steel
payment. Earlier committed spending remains consumed if a later payment command fails. `Owed` and
`Billing` are `Barrier`s, but that checks completion; it does not exclude unrelated task selection.
The remaining completion and task-order questions are recorded in
[`SEQUENCING.md`](SEQUENCING.md#the-missing-rule-when-an-operation-is-over).
External acceptance can reject an unauthorized submitted decision, but cannot repair a calculation
that reduces the wrong bill or loses the distinction between spending and another resource loss.

## Current pieces

| Piece | Responsibility |
| --- | --- |
| `Owed<Class<Resource>>` | Remaining amount, discounts, surcharges, and non-M€ costs. |
| `PayingFor` and `PriceCard` | Concrete purchase context needed by modifiers before play. |
| `Billing` | Opens spending choices after pricing and marks settlement. |
| `ActionBilling` | Carries provider/action identity used by current action rules. |
| `CardBilling` | Specializes the shared billing rules for card payment lifetime. |
| `Accepting` | Restricts ordinary resources by payment context. |
| `AcceptingFromCard` | Restricts spending to the correct card holder. |
| `ResourceValue` | Supplies dynamic standard-resource values. |
| Kotlin payment helpers | Submit allocations, reject some excess, and drain unused offers. |

`Billing: -X Resource?` creates an independent optional task. Removing `Accepting` unregisters its
effects but does not retract work already queued by them. A settled bill can therefore leave unused
removal tasks, which current helpers resolve by choosing `Ok`. Blanket deletion of tasks whose
effect owner disappeared would be wrong because ordinary consequences must survive removal of
their source.

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
engine tasks and `TfmPayCommand` do not apply the same whole-allocation check. The physical-game
rule for the characterized mixed-metal case is unverified; do not describe current behavior as a
settled rules defect.

## Evidence

Start with:

- [`payment.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/payment.pets)
- [`card-model.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets)
- [`TfmActionLowerer.kt`](../../src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt)
- `PaymentSpecializationTest`, `TfmGameplayTest`, `DirigiblesTest`, `PsychrophilesTest`,
  `KuiperCooperativeTest`, `StormcraftIncorporatedTest`, `BoomTownTest`, and
  `StandardTechnologyTest`

Preserve observable card/action outcomes, discounts, values, source restrictions, rollback, and
settlement before consequences. Do not create mirrored allocation state or card-specific client
orchestration to address a local symptom.
