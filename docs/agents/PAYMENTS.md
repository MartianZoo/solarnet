# Payments: current protocol and design questions

> **NOTE:** This document is agent-maintained; source and tests take precedence.
>
> **Read when:** discussing card or action billing, payment choices, resource values, excess
> payment, or the player-facing payment APIs.
>
> **Skip when:** discussing a direct non-payment action cost; use [ACTIONS.md](ACTIONS.md).
>
> **Status:** current behavior, known limitations, and unresolved design options. No replacement
> payment representation or scheduling design has been selected here.

## Required behavior

Payment establishes the debt, applies its adjustments, lets the payer choose a legal allocation,
and resumes the card play or action after settlement. Accepted resources, effective values, and
modifiers belong to the game model. Clients should supply choices without reconstructing the
payment lifecycle from task causes or duplicating those rules.

Exact-M€ payment should remain simple. Mixed resources and card-held resources must not require
parallel debt records or a task cycle for every physical unit. Keep the existing replay evidence;
perfect causal attribution does not justify additional payment machinery.

## Current protocol

[Terraforming Mars `payment.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/payment.pets)
contains the current protocol. Search for these declarations:

- `Owed` records debt in a standard-resource denomination and creates its matching `Accepting`.
- `Billing` disappears when that denomination's debt reaches zero. `ActionBilling` identifies an
  action provider and slot; `CardBilling` identifies a card play.
- `Accepting` and `AcceptingFromCard` react to billing with separate optional payment tasks.
  Billing removal removes the offer components; existing optional tasks still need resolution.
- `Pay`, `PayFromCard`, and `ResourceValue` turn selected resource removals into debt reduction.

Card-price adjustment context is separate from billing. In
[`card-model.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets),
`PriceCard` emits `PayingFor` for the project-card class and its printed tags. `BuyCard` emits it
for project-card purchases. Corporation and Prelude plays do not use project-card pricing.
[ACTIONS.md](ACTIONS.md#current-divergence) describes standard-resource action lowering.

[`TfmGameplay.kt`](../../src/common/dev/martianzoo/tfm/engine/TfmGameplay.kt), search `pay`,
`openPendingBilling`, and `finishBilling`, changes autoexecution policy and searches the global
task pool for billing, payment offers, and follow-up work. Several standard-resource amounts can be
submitted in the same call, which executes separate choices inside an atomic interaction.
Card-held resource choices are not part of that same allocation argument.
[`TfmPayCommand.kt`](../../src/common/dev/martianzoo/tfm/script/commands/TfmPayCommand.kt), search
`withArgs` and `dismissUnusedAcceptsWhilePaused`, also submits separate choices and resolves unused
offers. These helpers are evidence of client orchestration, not a general payment contract.

## Delegated payment exposes a control gap

Suppose P2 owns Neptunian Power Consultants and P1 places Flooding's ocean. The intended interaction
is that P1 chooses when to offer P2 the decision; P2 then accepts or declines and, if accepting,
controls payment through completion while P1's unrelated work waits. The current optional action
and its permanent provider are declared together in
[Promo `cards.json5`](../../src/common/dev/martianzoo/tfm/canon/PromoCardPack/cards.json5), search
`NeptunianPowerConsultants` and `NeptunianOption`.

Current selection delegates only the selected task. Its controller remains P1; accepting the option
queues the billing work under P1 again. Billing then creates independent cash and Steel choices.
After P2 pays 2 Steel toward the 5 M€ cost, the remaining cash choice still requires selection by
P1. Structural splits, `THEN` continuations, and triggered work preserve that original controller;
[IDENTITY.md](IDENTITY.md#triggered-task-assignment-and-delegation) owns those mechanics.

The current `pay` helper bridges this gap by selecting tasks through their current assignee, even
when that assignee is another Player. Search `preparePayment` and `selectTaskForActor` in
`TfmGameplay.kt`. Consequently,
[`NewPromoCardsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/NewPromoCardsTest.kt),
search `Neptunian owner chooses and pays when an opponent places the ocean`, proves that the helper
can finish payment. It does not prove that P2 retains control between interactive payment choices
or that P1 cannot interrupt them.

`Owed` and `Billing` already inherit `Barrier`, but that means unfinished state is rejected by a
declared completion check. It does not exclude unrelated tasks while payment is pending. A
transaction likewise rolls back its own interaction, not earlier committed payment choices.

Acceptance alone does not establish affordability. Optional payment offers can be exhausted while
debt remains, so an empty task group cannot alone establish successful payment. A scheduling design
must account for unpaid obligations and failure after earlier choices have committed.

The unresolved scheduling alternatives are **nested priority groups** and an **exclusive operation
scope**. Either needs both inherited control for payment work and exclusion of unrelated work;
changing task ownership alone is insufficient. Whether settlement releases control immediately or
waits for the action's resulting choices is also unresolved. See
[delegated operations and scheduling options](SEQUENCING.md#delegated-operations-and-scheduling-options)
for the shared comparison; neither option is selected by this payment example.

## Allocation and legality

Pets currently splits an instruction group into independent tasks. Shared `X` and Type variables
can preserve a linked `THEN` choice, but do not give a task independently chosen quantities of cash,
Steel, Titanium, and card-held resources. A complete allocation as a task would therefore require
additional task semantics or a domain instruction. It is not merely different spelling of the
existing payment offers.

For positive M€ debt, the current helper's excess check rejects an allocation from which a selected
unit could be returned while still covering the debt. With debt `D`, selected unit values
`v1 ... vn`, and total value `P`, that criterion is:

```text
P - D < vi for every selected unit i
```

Full payment separately requires `P >= D`. `TfmGameplay.rejectReturnableUnit` does not by itself
require full payment, and ordinary engine payment tasks do not share its whole-allocation check.
The corresponding physical-game rule remains unverified. The current characterization lives in
[`UnknownRulesTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/UnknownRulesTest.kt), search
`Mixed-metal payment currently accepts seven steel and five titanium for Space Elevator`. Do not
present that case as a settled rules defect.

## Design options

These are alternatives for payment representation, separate from the scheduling choice above:

| Option | Potential simplification | Unresolved cost |
| --- | --- | --- |
| Complete allocation through a domain settlement operation | Replaces optional offers and client cleanup while Pets retains accepted sources and values. | Where allocation validation belongs, how card-held sources participate, and what raw engine tasks permit. |
| Complete allocation as an engine task | Keeps the payer's whole choice within the ordinary selected-task lifecycle. | Independent quantities need new semantics; the resulting deletions must justify them. |
| Sequential source choices in batches | Reuses ordinary resource choices and can work inside the delegated operation. | Recording the complete allocation, excess validation, correction, and completion without a parallel ledger. |
| Escrow represented as an owner | Ordinary transfers might accumulate an allocation before consumption. | Card-held ownership follows its holder; deposits and refunds may fire observable effects. A shadow resource model would defeat the simplification. |

The open questions are which layer must enforce complete-allocation legality, what identity actual
payment listeners require, and what ends the delegated interaction. A payer with overlapping
obligations or an action with cross-Player consequences would constrain those answers; do not assume
such concurrency solely to preserve the current billing identity hierarchy. No prototype order or
implementation sequence is implied by this comparison.
