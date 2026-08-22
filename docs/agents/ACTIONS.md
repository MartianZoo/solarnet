# Pets Actions

**Status: settled design direction with unresolved ownership.** This document describes the desired
meaning of the Pets `Action` node. The current
[transformer](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/ast/Action.kt) still lowers most
Actions by putting the cost removal and result together after `UseAction`; the rules below are not a
description of current behavior and do not prescribe a migration sequence.

## Scope and vocabulary

A **Pets Action** is the declaration node written `[cost] -> result`. Use that phrase here because
“action” also means the larger Terraforming Mars operation between one empty user task queue and
the next. This document is about the declaration node, not that operation boundary, turn structure,
Head Start, or action-use markers.

One component may declare up to three Pets Actions. Their identities remain explicit throughout the
protocol: `UseAction1`, `UseAction2`, and `UseAction3` begin them, and `CostPaid1`, `CostPaid2`, and
`CostPaid3` respectively complete their costs. The separate numbered `CostPaid` Classes are an
intentional part of the model; collapsing them would save vocabulary only by making action identity
implicit elsewhere.

## The semantic normal form

The arrow separates two different roles:

- the left side is an obligation that must be satisfied; and
- the right side is the result unlocked by satisfying it.

The result of Action N should therefore respond to `CostPaidN<This>`, not directly to
`UseActionN<This>`. `UseActionN` selects the offer and starts cost satisfaction. `CostPaidN` is the
single bridge saying that this activation's obligation has been satisfied. A truly costless Action
satisfies its empty obligation immediately and therefore emits `CostPaidN` immediately.

This gives `CostPaidN` one honest meaning across all cost families. In particular, a direct cost
must not emit `CostPaidN` first and put both its removal and payoff on that signal: at that moment
the cost would not yet have been paid. Its conceptual shape is instead:

```text
UseActionN -> satisfy cost THEN CostPaidN
CostPaidN  -> declared result
```

This is a semantic normal form, not a required intermediate AST or a commitment about which
compiler pass constructs it. The signal should retain the acting owner and Action provider needed
to distinguish the activation; its exact type signature is an implementation question.

“Costless” here is semantic, not merely syntactic. Several current declarations omit the left side
but manually construct `Owed`, `Payment`, or direct removal on the right. Those are existing
lowering workarounds, not genuinely free Actions. Normalizing Water Import from Europa to a nominal
12 M€ left side is one example of recovering the real obligation before applying these rules.

`CostPaidN` establishes causality only. It does not by itself make the result immediate, atomic,
higher-priority than unrelated tasks, or the completion boundary of the encompassing Terraforming
Mars operation. Those questions remain governed by [sequencing and completion](SEQUENCING.md).

## There is one invoice family, not one workflow for every resource

An **invoice** is exact debt in a standard resource. It is appropriate when the cost is a fixed or
chosen-X-scaled amount of `StandardResource`:

- the resource Class and Action owner are enough to identify what is owed;
- discounts naturally reduce the amount owed; and
- accepted substitutes such as Steel, Titanium, or a resource held on a card are methods of
  settling that same debt.

This reframes the apparent parallel payment workflows. A card resource used as alternate tender is
not itself the invoice. It is one way to reduce an invoice denominated in a standard resource.
Conversely, an Action whose printed cost is a card resource should not acquire an invoice merely
because card resources can sometimes be tender elsewhere.

For a fixed standard-resource cost, `UseActionN` opens the exact nominal invoice. Its modifiers and
accepted tenders may change how that invoice is settled. Final settlement emits one
`CostPaidN<This>` event, and only that event unlocks the declared result.

This family includes both Electro Catapult choices, represented as two Pets Actions: one
invoices one Plant and the other invoices one Steel. Both unlock the same payoff through their own
numbered `CostPaid` signal. The authored `OR` is not valuable enough to preserve as a special Action
cost mechanism when it is the sole corpus case and obscures which invoice is being opened.

Venus Shuttles points toward the same normalization. Its nominal cost can be an invoice for 12 M€,
while a separate Action-specific rule discounts that debt per Venus tag. Water Import from Europa
can likewise declare its nominal M€ invoice while a separate rule says that Titanium is accepted.
The Action string need not grow syntax for every discount or alternate tender.

These rules need a reliable point after the exact invoice exists and before payment settles where
Action-specific modifiers can act. Whether that point is `UseActionN`, a distinct invoice-opened
signal, or another existing component event is deliberately unresolved. The important rule is that
such effects modify an actual obligation and cannot announce payment or the result prematurely.

## Direct costs stay direct

Costs in card resources, cards, production, and other holder- or transformation-sensitive
components do not become invoices. `Class<Floater>` plus the Action owner does not identify which
card's Floaters can be removed; the direct Pets instruction already carries the holder or target
selection that the operation needs. `PROD[...]` is likewise a transformation of production state,
not fungible tender for a debt.

For these Actions, using the Action creates the ordinary removal or transformed-cost task. When
that task completes, `THEN` emits the corresponding `CostPaidN`; the result remains a separate
effect subscribed to that signal. This preserves the current direct cost mechanism without
pretending it is a second kind of invoice.

This distinction also prevents the payment model from expanding simply because the grammar can
express an unusual left side. Gates, holder choices, card selections, and production changes can
retain their ordinary Pets meaning. They need special Action treatment only when something must be
carried across the `CostPaidN` boundary.

## X is chosen before an invoice exists

An X-scaled invoice cannot begin by asking the payment workflow to discover X from whatever the
player happens to pay. The Action chooses X first; that choice determines an exact invoice. Payment
methods may settle or reduce the resulting debt, but they cannot redefine the Action's X.

For the illustrative Action:

```text
2X Foo -> 3X Bar, 1 Steel
```

where Foo is a standard resource, choosing X = 5 creates an invoice for 10 Foo. Settlement then
emits one grouped effect containing `5 CostPaid1<ThatCard>`. The result has the conceptual
subscription:

```text
X CostPaid1<ThatCard>: 3X Bar, 1 Steel
```

Here X on the trigger observes and binds the group's multiplicity. It runs the right side once with
X = 5: the result is 15 Bar and 1 Steel, not five Steel. The five `CostPaid1` components encode the
selected scalar; they are not five independently completed Actions or five payment installments.

This is why a fixed cost emits one `CostPaidN` regardless of the number of resources paid, while an
X-scaled Action emits the selected X multiplicity. The same value must survive discounts,
substitutions, and split tender.

An X-scaled direct cost can obtain X from its own removal or selection task instead of opening an
invoice. It should still emit X `CostPaidN` as one group when that direct cost completes so the same
result subscription works. No additional payment abstraction is needed for that family.

## Why the boundary is narrow

The production [Canon corpus](../../canon/src/commonMain/resources/canon/bundles) supports a small
rule rather than a universal cost calculus. Most Pets
Actions either have no written cost or use a direct cost. Fixed nonstandard-resource costs in the
current corpus are card resources; production and card costs form small, readily expressed direct
families. Fixed standard-resource costs are the substantial reusable family, and the only
alternative standard-resource cost is Electro Catapult, which is better expressed as two Actions.

The useful generalization is therefore not “all left sides are payments.” It is:

1. every Pets Action separates cost satisfaction from its result with numbered `CostPaid`;
2. exact standard-resource debts use the invoice/payment protocol; and
3. everything else satisfies its cost through ordinary Pets instructions.

If future corpus evidence does not fit those rules cleanly, reconsider the boundary from that real
case rather than adding speculative kinds now.

## The ownership problem remains real

`Action` is currently a Pets AST concept, but `StandardResource`, the numbered `UseAction` protocol,
and the invoice declarations are Terraforming Mars concepts. Recognizing standard-resource costs
during lowering therefore cannot be presented as a wholly generic Pets transformation. Unlike
`PROD[...]` lowering, the invoice conversion is not merely like-for-like syntax expansion; it adds
game-specific cost semantics and extension points for discounts and tender.

The desired semantics do not settle which package owns that knowledge. Plausible boundaries
include a narrow Authority/application-supplied Action lowering policy or moving the whole
numbered Action protocol under Terraforming Mars. A general plugin framework, a generic
`StandardResource` concept, and duplicate generic/domain Action protocols would all be larger than
the demonstrated need. Resolve ownership as one boundary decision when this behavior is selected
for implementation; see [BOUNDARIES.md](BOUNDARIES.md#turnaction-protocol-is-split-across-layers).

## Deliberately separate questions

This direction does not decide:

- the compiler phase, runtime component shape, or payment barrier that realizes the normal form;
- the name of the pre-payment extension point for discounts and accepted tender;
- task priority or whether any cost step should be automatic;
- the completion scope of a whole Terraforming Mars operation, including Head Start; or
- action-use marker and Viron behavior.

Those questions may interact with Pets Actions, but folding them into cost lowering would recreate
the overgrown “action model” this separation is intended to avoid.

## Current implementation foothold

`StandardActionDefinition` can attach ordinary Effects to the Class it generates. The claim
milestone, fund award, convert plants, and convert heat definitions now use that ability to keep
their invoice opening, payment barrier, and result on the standard-action Class instead of placing
part of the workflow on the global `TerraformingMars` Module. In particular,
`ClaimMilestoneSA` once again contains its `Milestone` instruction.

This is a locality correction, not the semantic lowering described above. These definitions still
construct `Owed` and `Payment` explicitly and still use the existing unnumbered `CostPaid` signal.
`Owed<>` accepts the default M€ debt type for gains and removals; non-M€ occurrences remain
explicit, while bare `Owed` in triggers and requirements remains resource-generic.
The numbered protocol and standard-resource Action lowering therefore remain unresolved work. In
particular, `8 Plant -> ...` and `8 Heat -> ...` should eventually lower to the manual conversion
invoices now present in the standard-action definitions.
