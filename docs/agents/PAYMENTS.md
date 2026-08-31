# Payment allocation

> **Read when:** fixing excess payment, recording tender value, attributing payment contributions,
> or evaluating Helion/Stormcraft implications for one auditable allocation.
>
> **Skip when:** changing Action-to-invoice lowering without changing allocation; use
> [ACTIONS.md](ACTIONS.md).
>
> **Status:** verified defect plus uncommitted design candidates. The rule interpretation still
> requires confirmation from an original Jacob Fryxelius post.

## Source map

- [Terraforming Mars `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets)
  — search separately for `CLASS Pay`, `ABSTRACT CLASS Owed`, and `ABSTRACT CLASS Billing` to inspect
  the current distributed protocol.
- [Colonies `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/classes.pets)
  — search for `Stormcraft` only when evaluating source attribution.
- [`TfmGameplay.kt`](../../src/common/dev/martianzoo/tfm/engine/TfmGameplay.kt)
  — search for `fun pay` for the current client-side rejection stage.
- [`TerraformingMarsRoutines.kt`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMarsRoutines.kt)
  — search for `fun OperationBody.settle` for Routine payment ordering and the remaining-debt M€
  check.
- [`PaymentSpecializationTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/PaymentSpecializationTest.kt)
  and [`BugsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/BugsTest.kt)
  — read before choosing a repair.

## What must be fixed

Payment **sequencing** and payment **allocation evidence** are separate. The proposed sequencing
direction in [ACTIONS.md](ACTIONS.md#proposed-single-payment-choice-loop) offers one required
abstract tender choice at a time and removes Billing directly when no matching debt remains. That
removes parallel tender tasks, explicit rejection of unused methods, and client-side cleanup scans.
It does not reveal gross value hidden by saturated `Owed` removals and therefore does not by itself
repair the allocation defect below.

The payment system must eventually distinguish three facts:

1. which indivisible resources the player chose to spend;
2. the full value contributed by each resource after every applicable rule; and
3. how much of the combined value was needed to settle the invoice.

Today it preserves the first fact but not the other two. `Pay` and `PayFromCard` remove the selected
resource. Automatic effects then remove `Owed`, stopping harmlessly when no matching debt remains.
For example, one Steel removes two M€ of debt through the Steel rule and possibly another
through Advanced Alloys. (`Pay` itself handles payment whose resource matches the debt denomination.)
If the earlier removals exhaust the debt, the later effect records no contribution. The result
depends on automatic-effect execution order, although that order is not a game rule and must not
decide which card receives credit.

Consequently the engine cannot tell the difference between value that was never offered and value
that was offered but unnecessary. Direct task execution can therefore settle an invoice after an
illegal selection. `BugsTest.Space Elevator incorrectly accepts payment that wastes one steel`
captures the known failure. `TfmGameplay.pay` prevents some such selections in advance, but its
per-resource check normally forbids even legal rounding excess. Its explicit escape hatch disables
that excess check for the payment call rather than proving the complete mixed allocation legal,
and direct task callers can bypass the helper altogether. Catalog Routines reject M€ beyond the
remaining debt but otherwise share the engine's incomplete non-money validation.

Exact payment cannot replace the real rule. Reconstructed games contain legitimate payments whose
resource values do not sum exactly to the price, so forbidding every excess would reject sourced
play.

## Working statement of the rule

For debt `D`, let the selected indivisible payment units have effective values `v1 ... vn`, let
`P` be their sum, and let excess `E = P - D`. The discussed rule is:

```text
P >= D and E < vi for every selected unit i
```

Equivalently, removing any one selected unit would make the payment insufficient. This
"nothing can be returned" formulation is useful because it avoids calculating the minimum value
as a separate concept. It still requires the system evaluating the allocation to know or reproduce
each unit's complete effective contribution.

The precise official rule and its treatment of unusual payment conversions remain to be verified
from an original Jacob ruling before this becomes committed behavior.

## Attribution is related but not identical

We want the history to show when Advanced Alloys, Phobolog, Psychrophiles, and similar rules
contributed and by how much. That requires recording gross contributions before debt consumption
saturates. It does not necessarily yield one objectively correct allocation of the consumed debt.
When several bonuses contribute to a payment containing excess, saying which bonus was "needed"
may require a reporting convention or a counterfactual definition. Registration order is not an
acceptable convention.

The durable facts are the chosen source units, every conversion or bonus they caused, their gross
terminal values, the debt consumed, and the resulting excess. Analytics can derive a stated form of
credit from those facts without making that choice part of payment execution.

## Candidate designs

### Validate in a client

A client library can assemble a complete payment, calculate its effective unit values, and submit
only legal selections. It can use the equivalent test: remove each selected unit in turn and reject
the allocation if any reduced selection still covers the invoice.

This is a legitimate division of responsibility in a follow-mode engine, especially when the
engine exposes low-level choices rather than owning the player's whole move. It also has the lowest
engine cost. The present model nevertheless supplies too little evidence for a general client to do
this reliably: saturated `Owed` removals conceal gross value, and payment conversions may be
transitive. Client validation becomes credible only if payment evaluation or its event history
exposes the full contribution of each source unit. Raw task callers would still be able to create
an illegal history, and that limitation must remain explicit.

### Give the last, least-valued unit AMAP settlement

Require every earlier payment unit to contribute its full value, then let one final unit remove as
much remaining debt as possible. This is mathematically complete if that final unit has minimum
value. If the final unit has value `v` and excess is legal, all preceding units total
`D + E - v < D`, so positive debt necessarily remains for it.

This approach is small operationally but does not discover which unit is least-valued. A trusted
client could order that unit last; unrestricted engine tasks could choose the wrong final unit. A
rule that identifies the last unit inside the engine needs the same effective-value information as
direct validation.

### Record an allowed excess reserve

The invoice could create `AllowedOverpayment` units. Payment effects would remove plain `Owed`
first and the reserve second, failing if their complete value could remove neither. This records
excess and prevents silent saturation. It does not explain how much reserve to create: the legal
amount depends on the least-valued selected unit, which is not known when the invoice is created.
Making every payment source add its own allowance is wrong because the allowance is a minimum, not
a sum.

### Temporarily lend debt and take the unused part back

Before accepting payment, the engine could add artificial debt `K`. Against real debt `D`, a
payment worth `D + E` leaves `K - E` of the artificial debt. Removing that unused debt afterward
leaves `E` as an explicit record. This lets every contributing effect execute fully without knowing
its value in advance.

The trick discovers excess but cannot decide whether that excess is legal without learning the
least unit value or attempting the return test. It also needs a safe `K` and must keep artificial
debt out of attribution. It is therefore useful mainly as evidence that excess can be surfaced by
reversible bookkeeping, not yet as a complete design.

### Escrow sources and try returning each one

Selected resources could remain in payment escrow until the invoice has been evaluated. The engine
would reject the selection if any single source unit could be returned while the remainder still
covers the debt. This expresses the working rule directly and supports strange conversion chains.

It is complete, but reversing one source unit requires retaining that unit's entire causal group of
base value, bonuses, and conversions. Implemented properly, this is a per-allocation payment ledger
or counterfactual evaluator. That may ultimately be the right model, but it is not a small trick.

### Produce tender or credit before consuming debt

Instead of having each payment effect remove `Owed` immediately, spending could produce a payment
signal, conversions and bonuses could produce further payment value, and only the terminal value
would settle the invoice. Each signal would remain associated with both its source unit and its
invoice. This would preserve gross value and attribution and could support either engine or client
validation.

This is the most coherent systemic direction found so far, but it adds an intermediate concept and
may require payment units to remain grouped. It should not be adopted solely to make one failing
test pass. A prototype would need to demonstrate that it simplifies the existing Steel, Titanium,
card-resource, and modifier rules as a whole.

## What Helion and Stormcraft reveal

Helion support is low priority and is not part of the current payment-fix scope. The combination is
still a useful test of whether a proposed model composes.

Stormcraft currently responds to a Heat `Billing` by offering `PayFromCard<Stormcraft>`, then that
signal directly removes two `Owed<Heat>`. If Helion makes Heat acceptable for an M€ invoice, simply
having Stormcraft react to `Accept<Heat>` would expose the floater choice but would not complete the
conversion: `PayFromCard<Stormcraft>` would still seek Heat debt while the invoice contains M€ debt.
The signal also does not retain that the floater represents two Heat.

The designer-ruling premise supplied for this discussion requires the conceptual chain:

```text
Stormcraft floater -> two Heat payment units -> Helion conversion -> M€ invoice value
```

A future model can support that either by making conversions composable or by evaluating the whole
chain in a client. It must also associate the chain with one invoice so the same Heat value cannot
settle unrelated Heat and M€ debts. This example favors recording payment production separately
from debt consumption, but its low product priority means it is a design check, not a reason to
implement Helion now.

## Present direction

First replace the distributed parallel-task lifecycle with the single payment-choice loop while
preserving the exact `Pay` and `PayFromCard` history needed by later allocation work. Complete
Billing directly from debt reaching zero. Do not add a separate `Paid` component: invoice removal
remains the completion event.

Do not repair Space Elevator by prohibiting all excess or by relying on automatic-effect order.
First seek the smallest way to expose complete per-source payment value. Once that evidence exists,
client-side return testing is an acceptable initial enforcement point. A larger in-engine ledger is
justified only if it makes the existing payment rules materially clearer rather than adding a
second machinery alongside them.
