# Payment model and simplification investigation

> **Read when:** changing payment choices, accepted resources, debt, billing, resource values, or
> payment APIs. For direct action costs, also see [ACTIONS.md](ACTIONS.md).
>
> **Status:** current model followed by proposals. Source and tests are authoritative. Investigation
> priorities are not requirements to preserve every existing concept or build every alternative.

## Current contract

Players complete ordinary removals such as `-13 MC` and `-3 Floater<Dirigibles>`. There are no `Pay`
or `PayFromCard` signals. A loss performed by the payer is the payment event:

- `Accepting` reduces debt in the resource's own denomination.
- `ResourceValue` reduces M€ debt when that standard resource is currently accepted.
- A card's loss listener reduces the appropriate debt while `AcceptingFromCard<This>` exists.

Every payment-loss trigger uses `BY Me@`: the event's Actor must be the payer who owns the accepted
resource. This is separate from who controls the surrounding operation or delegates the task.
An opponent's removal cannot pay the victim's bill; a delegated payer's own removal still counts.

Acceptance makes the payer's loss count as payment. It starts during pricing: `Owed` installs its
denomination's acceptance, and `PayingFor` or `UseAction` can install alternatives before `Billing`.
An additional `IF Billing` condition on an acceptance component's own listener is unnecessary for
the ordinary payment sequence, but acceptance's actual lifetime is broader than settlement.
Persistent value components and cards still check acceptance. Removing the bill removes acceptance
before the purchased card or action results run.

`Owed` is the amount still due, including during price adjustment. `Billing` marks settlement and
its completion event; it contains only the denomination. `ActionBilling` additionally carries the
provider and action slot used by action-specific rules. `CardBilling` has no card identity or dummy
action identity: the pending play instruction already retains the card. There is no `CardPlay`
singleton.

`BY Me@` distinguishes the performer, not the purpose of the loss. A confirmed remaining defect is
Flooding targeting its own player while that player's Neptunian bill is open: the chosen 4 M€ loss
incorrectly reduces the 5 M€ bill to 1 M€. The owner explicitly deferred this case before committing;
[`BugsTest`](../../test/common/dev/martianzoo/tfm/tests/cards/BugsTest.kt) records the incorrect debt
and completed bonus payment. The explicit exchange and external-policy investigation below remains
open; do not add identity or transaction machinery for hypothetical concurrent invoices.

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

[`BugsTest`](../../test/common/dev/martianzoo/tfm/tests/cards/BugsTest.kt) characterizes both
interrupting immediately after acceptance and interrupting after a separate Steel payment.
The latter also verifies that the opponent's loss does not clear the remaining debt; failed cash
payment leaves the previously spent Steel consumed. These are intentionally deferred scheduling
defects, separate from payer-only recognition of resource losses.

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

## What the remaining pieces actually provide

| Piece | Actual consumer or responsibility | What is still questionable |
| --- | --- | --- |
| `Owed<Class<Resource>>` | Discounts, surcharges, remaining payment, non-M€ costs such as heat conversion and Trade. | It is also a `Barrier`, duplicating some completion state with `Billing`. |
| `PayingFor` and `PriceCard` | Price modifiers need the concrete card and printed tags before play. Purchases trigger modifiers per bought card. | Keep the context, but do not assume every aspect needs a separately recorded signal forever. |
| `Billing` | Opens choices after pricing; zero-cost and discounted-to-zero cases finish without spending; removal clears eligibility. | This is a stage and completion hook, not another amount. Whether that stage needs a component remains open. |
| `ActionBilling` | Ecoline, Thorgate, Cryo-Sleep, Rim Freighters, and the milestone/award corporation modify particular costs. Generated action results and Standard Technology react to settlement. | Provider/slot identity is needed by the current lowering, not necessarily by a better action lifecycle. |
| `CardBilling` | Concrete specialization sharing the billing rules. | Its only distinction is card-payment lifetime; no rule consumes the played card's identity here. |
| `Accepting` | Restricts ordinary resources by payment context, such as steel for building cards. | Also manufactures an optional task and leaves its cleanup to clients. |
| `AcceptingFromCard` | Restricts spending to the correct holder, such as Dirigibles rather than any floater card. | Duplicates the standard offer's task/lifetime shape. |
| `ResourceValue` | Dynamic values shared by execution and the client check: base metals, alloys, PhoboLog, Boom Town, Unity, and plant substitution. | A separate effect contribution per granting component is useful for existing summaries, but not required by the payment rule. |
| Kotlin payment helpers | Submit choices, handle controller delegation, reject some excess, and advance unfinished billing work. | Much of their task searching and policy manipulation compensates for how the choices are represented. |

Sources: [payment.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/payment.pets),
[card-model.pets](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/card-model.pets),
[TfmActionLowerer](../../src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt), and their production
listeners in each card bundle.

## Requirements we must not invent

- **A single engine task for the entire allocation is not required.** Batched removals from several
  sources already express the user's desired interaction. Do not introduce a multi-quantity task,
  escrow, or parallel allocation ledger merely to combine them.
- **Perfect attribution is not required for execution.** Preserve observable results and sourced
  summaries while practical; richer explanations can be reconstructed from recorded events. The
  current resource-value attribution already varies under randomized independent listener order.
- **Every payment need not support every currency.** Keep the eligible sources and denomination
  explicit. Do not add conversion chains or new Helion support during simplification.
- **Payment does not require general causal completion.** Debt reaching zero after pricing is an
  actual domain completion fact. A global operation-scope redesign must earn its cost independently.
- **Current excess validation is not universal.** `TfmGameplay.pay` checks the standard-resource
  amounts supplied in that call. Card-held amounts are submitted separately; `TfmPayCommand` and
  raw tasks do not perform the same complete-allocation check. The existing system must not be
  defended as if it already enforces something stronger.
- **A source-reconstruction audit is not a game rule.** `requireExplicitPaymentChoices` and
  `intentionalUnderpay` ask replay authors to acknowledge unusual allocations; ordinary players
  need no such opt-in. These belong with replay tooling if moved out of `TfmGameplay`.

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


## Why unused payment tasks survive

`Billing: -X Resource?` creates an independent task. Removing the `Accepting` component unregisters
its effects, but does not retract work already queued by them. Consequently a settled bill can
leave several optional removal tasks behind. `TfmGameplay` and `TfmPayCommand` search their causes
and explicitly choose `Ok`.

A live requirement around the removal is not sufficient by itself. It can make further spending
unavailable, but the task still needs resolution. Under `CONCRETE`, several independently selectable
no-op tasks can still prevent progress. This describes the current external policy, not a limit
of the engine model. A different policy may simplify the flow; evaluate its contract and the total
machinery it replaces instead of treating `CONCRETE` as a fixed payment requirement.

The general engine must also retain ordinary consequences after their originating card disappears;
therefore blanket deletion of tasks whose effect owner disappeared is wrong. A revocable offer and
an already-triggered obligation have different lifetimes. Any generic offer-lifetime feature needs
that distinction and a substantial demonstrated deletion, not just a payment-specific shortcut.

## Explicit exchanges and continuation cost

An offer such as `(-X Graphene<CarbonNanosystems> THEN -4X Owed) OR Ok` can carry its own
exchange rate instead of observing resource losses. Existing first-stage narrowing binds X in the
continuation, so the player can still submit just the resource removal. Declining must skip the
whole exchange.

However, `THEN` does not execute the exchange together. Spending creates an independent concrete
debt-reduction task. With other accepted choices still pending, `CONCRETE` can stop before crediting
the bill. The helper consequently sees outstanding debt and skips settlement cleanup. Later card
choices can then compete with unused payment offers. Moving card offers to their providers also
invalidates the current cleanup's dependence on `AcceptingFromCard` as the task's cause context.

A direct replacement was tested and reverted: focused payment scenarios passed, but the full
suite exposed unfinished settlement and leftover-choice failures across card play and workflow.
The [discarded prototype and results](../../_local/investigations/payment-then-2026-10-05-work5/README.md)
retain the concrete evidence. Those failures establish incompatibility with the current policy
and orchestration, not that explicit exchanges make the model worse. Automatic execution is a
tunable policy outside the pure engine; the engine can continue exposing independent tasks while
a policy chooses to finish a selected exchange's concrete continuation. `TaskNarrowingTest`'s
first-stage X scenario and [SEQUENCING.md](SEQUENCING.md#what-then-does) describe the existing engine
contract.

- [ ] Compare explicit exchanges plus a small, general continuation policy with the current loss
  listeners and cleanup. Keep the engine's legal choices and `THEN` semantics separate from the
  policy's decision to proceed. Determine which continuations the policy should finish, preserve
  actual player choices and mixed-payment orders, and judge the combined conceptual cost and net
  deletion. Do not assume every concrete task should execute immediately or add payment-specific
  sequencing merely to preserve today's helpers. Re-run the full payment/replay scenarios under
  the proposed policy before drawing a conclusion.

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

The following candidates target the current payment machinery without selecting a replacement
representation or implementation order:

### Replace parallel optional offers with a single current spending choice

The strongest target is the unused-choice lifecycle, not resource arithmetic. Keep eligibility as
live data, offer a choice of accepted removals, accept a positive batch from the chosen source, and
ask again only if debt remains. Cash-only payment should be a single removal. Mixed payment should
need only the removals actually chosen, with no unused-method dismissal.

First test whether ordinary Pets can express the eligible resource alternatives and batched
continuation. The obstacle is concrete holder identity: `Class<Resource>` cannot carry
`Floater<Dirigibles>` because `Class` represents a class name, while an instance dependency on a
resource would incorrectly require that resource to survive spending. Do not add a general `Type`
representation just to unify the two offer classes.

If ordinary Pets cannot express the choice compactly, compare a small domain-owned
`CustomInstruction` that constructs an `OR` of ordinary removals from the existing acceptance data.
It must delete per-source tasks, unused-offer cleanup, and cause-based discovery as a coherent
replacement. It must not introduce another eligibility/value table, a per-unit loop, new core APIs,
or a second payment protocol. This is an unimplemented candidate, not a selected design.

### Make fixed pricing work automatic at its semantic owner

The fixed-cost action lowerer currently queues debt creation and billing as work the client must
advance. Those are calculations, not player decisions. The variable-cost action is different:
selecting X is a real choice and must survive into the result.

Do not blindly change the fixed effect from `:` to `::`. Other `UseAction` listeners install
accepted metals or card resources. Opening billing before those independent listeners run can miss
an offer entirely. Establish eligibility and finalized debt coherently, then open the spending
choice. A single current spending choice may remove this ordering problem without another
latch. Success must delete `openPendingBilling` and `advanceSingleConcreteTask`, not move their
knowledge into a different client helper.

### Consolidate the actual payment entry points

`TfmGameplay.pay` and `TfmPayCommand` implement different selection, pausing, cleanup, and validation
paths. Once choices have a smaller lifecycle, share the actual payment operation. Use existing
resource instructions/expressions to include card-held sources; do not create a second model of
resource holders or values. Preserve partial batch submission if it remains the interaction model.

Separately decide whether the helper's excess check is sufficient or whether a complete submitted
allocation must be checked. Do not build whole-allocation infrastructure while treating that
product choice as already settled.

### Revisit debt versus billing with action completion

`Owed` alone cannot replace `Billing` by textual substitution: discounts can reach zero during
pricing, action modifiers need context after the debt exists, and Standard Technology must reward
settlement rather than supply money to make the payment affordable. Card play already retains its
continuation; action lowering currently distributes it between `UseAction` and `-ActionBilling`.

The late action-signal direction in [ACTIONS.md](ACTIONS.md) may remove this identity duplication.
Evaluate it with a fixed cost, a variable cost, a Trade discount, and a post-payment reward. Do not
preserve dummy action identities or require an operation-scope framework in advance.

### Sum values where they are used

A standard offer could reduce debt by a queried aggregate `ResourceValue` count instead of firing
separate reduction effects. This would keep the same value data and remove effect fanout. Its
specific cost is changed per-card attribution in the existing Advanced Alloys replay summaries;
that is not an execution requirement, but sourced summaries must be deliberately adapted rather
than silently deleted.

Replacing base/value components with immutable properties is less clearly a win: Boom Town changes
base titanium value and restores it on removal, while independent cards and Unity add value. A new
positive/negative modifier hierarchy may simply redistribute the same complexity. Prioritize task
and client deletion first.

## Acceptance and evidence

- Keep player choices as ordinary resource removals.
- Preserve current card/action outcomes, discounts, value modifiers, non-M€ costs, source restrictions,
  rollback, and settlement before consequences.
- Preserve the current validation scope unless the owner explicitly selects a change.
- Add no mirrored allocation state, speculative concurrency, or card-specific client orchestration.
- Judge permanent concepts and client knowledge before line counts.
- Keep full replay coverage passing and review independent automatic-listener ordering.

Focused evidence includes `PaymentSpecializationTest`, `TfmGameplayTest`, `DirigiblesTest`,
`PsychrophilesTest`, `KuiperCooperativeTest`, `StormcraftIncorporatedTest`, `BoomTownTest`,
`StandardTechnologyTest`, variable-amount action tests, script-command tests, and the complete replay
suite. `DirigiblesTest` covers both mixed-payment orders and an additional resource cost after
settlement. `UnknownRulesTest` preserves current raw mixed-metal excess behavior.
