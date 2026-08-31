# Pets Actions

> **Read when:** changing Pets `Action` parsing/lowering, action selection, billing, or cards with
> fixed, property-scaled, or X-scaled standard-resource costs.
>
> **Skip when:** changing an instruction that is not an `Action`, or changing payment
> allocation after an invoice has already been created; use [PAYMENTS.md](PAYMENTS.md) for that.
>
> **Status:** current model except where the single payment-choice loop is explicitly marked as a
> proposed direction. Standard-resource costs use billing; direct and costless Actions retain
> normal Pets sequencing.

## Source map

- [`Action.kt`](../../src/common/dev/martianzoo/pets/ast/Action.kt) — search for
  `public sealed class Cost` to inspect the parsed cost forms.
- [`PetTransformer.kt`](../../src/common/dev/martianzoo/pets/PetTransformer.kt) —
  search for `transformAction` before changing the lowering stage.
- [Terraforming Mars `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets)
  — search for `ABSTRACT CLASS Billing` to see the runtime protocol.
- [`VariableAmountActionsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/VariableAmountActionsTest.kt)
  — read when changing X costs or the identity of several Actions on one component.

## Meaning and scope

A **Pets Action** is the declaration written `[cost] -> result`. It is distinct from the larger
Terraforming Mars operation between one empty user task queue and the next.

One component may declare up to three Pets Actions. `First`, `Second`, and `Third` identify them.
`UseAction` and `Invoice` retain both the actual provider and this selector, so two Actions on one
card cannot settle one another. Invoice providers are live `HasActions` components, not their
Class tokens. `HasActions` is singleton-shaped because a dependency must identify one component.

## Standard-resource costs

A standard-resource Action has this lifecycle:

1. `UseAction` creates its exact `Owed<Resource>` amount.
2. Creating debt installs inert `Accept<Resource>` markers. More debt may safely accumulate before
   payment becomes available.
3. The Action creates one provider- and action-qualified `Invoice<Provider, Selector, Resource>`.
   That concrete event implements the common `Billing` protocol, which exposes ordinary and
   card-based payment choices.
4. Payment removes `Owed`. When the invoice's resource debt reaches zero, the invoice removes
   itself.
5. The Action result responds to that `-Invoice` event.

The invoice is therefore both the conclusion of one obligation and its completion event.
There is no separate payment-completed signal.

Only invoice creation needs to state its denomination, and M€ is the creation default. Effects
normally respond to `Invoice<Provider, Selector>` because a particular Action has one nominal
denomination. A non-M€ creation retains `Class<Resource>` as accurate data naming the fungible debt;
it does not substitute a Class token for the live behavior provider.

On removal, bare `-Owed` deliberately leaves the denomination generic; `-Owed<>` explicitly accepts
the M€ dependency default. Thus one `Invoice<TradeSA>` reaction can discount every Trade action
without enumerating its three payment resources.

Discounts and surcharges modify `Owed` before the invoice exists. Accepted substitutes such as
Steel, Titanium, or resources held by cards are ways to reduce the same invoice; they are not
parallel kinds of cost.

Fixed costs lower to two effects: one creates the debt and invoice, and the other responds to the
invoice's removal. X-scaled costs keep their result as a local continuation gated on absence of the
invoice, preserving the chosen X without encoding it in invoice multiplicity.

### Proposed single payment-choice loop

Today each accepted tender kind creates its own optional payment task. Paying with one kind can
leave the unused tasks behind, so callers must decline or clean them up.

Replace those parallel offers with exactly one required task meaning “pay one accepted unit.” Its
concrete refinements are the currently legal `Accept<Resource>` and
`AcceptFromCard<Holder>` choices. Spending one unit creates a common payment Signal. After its
automatic value effects finish, Billing creates one replacement payment-choice task if matching
`Owed` remains.

If debt remains but no accepted tender is available, the payment task remains present but cannot be
selected. The queue therefore cannot drain and the encompassing operation dead-ends. Do not add
a separate `MAX 0 Owed: Ok` completion task: after successful payment it would compete with stale
unused-tender tasks and recreate the need to decline or clean them.

When no debt remains, no replacement task is created and Billing removes itself directly in
response to the final matching `Owed` removal. Existing `-Billing` effects remove the Accept
capabilities; `-Invoice` remains the single completion event for the Action result. There is still
no separate `Paid` component and payment completion does not wait for unrelated Player work.

Card play uses the same debt-zero completion. Remove the pending
`MAX 0 Barrier: CardFront FROM CardBack` task; `-CardInvoice` instead puts the card into play as soon
as its debt is settled. EventCard lifetime is handled separately by whole-World idle cleanup; see
[SEQUENCING.md](SEQUENCING.md#whole-world-idle-cleanup).

### Player-input staging

Card play and standard-resource Actions expose one operation with ordered stages, not an
unstructured bag of removals and results. The player first chooses the card or Action. If that
choice creates Billing, the operation must finish that Billing stage before accepting any of the
resulting consequence choices. Billing may require zero or more tender selections. Only after it
closes can the player select direct effects or other queued consequences.

Clients and Routines must recognize payment from the live Billing stage, not by inspecting every
resource-removal instruction. This distinction is what keeps a direct floater cost, a production
transformation, or another holder-sensitive removal in the ordinary Pets consequence stage. It
also makes written payment/consequence interleaving invalid without inventing a special payment
syntax. See [ROUTINES.md](ROUTINES.md#invocation-contract).

## Composition

Several additions to `Owed` may precede one invoice. Card buying adds 3 M€ per card, allowing
Polyphemos and Terralabs Research to alter the same debt before the invoice opens. Fund Award is
instead three Actions—8, 14, and 20 M€—whose results are gated to the corresponding
existing Award count; selecting the wrong one cannot complete and rolls back.

Card acquisition first fixes the complete selected set, then establishes all adjusted debt under
one Billing stage, and transfers the exact cards only after settlement. Starting-card acquisition
uses the same lifecycle after corporation resources exist; Business Network and later Research use
it whenever the player commits the contents of `Selecting`. The commitment time may be a player
choice, but individual debt creation and transfer bookkeeping are not.

Card play creates printed M€ debt, handles tags, then creates
`CardInvoice<Class<CardFront>>`. Generic card-play modifiers respond to its `Billing<PlayCards>`
supertype, whose persistent owner covers standard actions, preludes, and other card-play routes.
Only modifiers that inspect the selected not-yet-live card use the specialized invoice and its Card
Class. The card moves face up only after all resulting barriers are gone, including requirement
debt.

Neptunian Power Consultants creates a live owned auxiliary `HasActions` component. An ocean offers
that component's Action, whose result puts the Hydroelectric resource on Neptunian Power
Consultants.

St. Joseph of Cupertino Mission creates one unowned `CathedralAction`. Each owned Cathedral offers
that Action to its city owner, so the city owner is also the payer and recipient.

Standard projects use the same rule while retaining authored Actions such as
`1 / cost -> OceanTile<>`; Action lowering supplies the invoice workflow.

Stormcraft offers its floaters through heat `Billing`. Local Heat Trapping is the sole
direct heat payment, so Stormcraft separately lets up to three floaters become the equivalent heat
when that card enters play; LHT itself retains its immediate removal.

## Direct and costless Actions

Card-resource removals, production transformations, selections, and other nonstandard costs remain
plain Pets instructions. Their existing `THEN` chain already expresses when the result follows
the cost, without pretending that holder-sensitive components are fungible debt.

Costless Actions likewise need no invoice. This keeps the payment model limited to the family for
which debt, modifiers, and alternate tender compose.

## Ownership rules

`Action` is a Pets AST concept, while `StandardResource`, `Owed`, and `Invoice` belong to
Terraforming Mars. The generic transformer currently recognizes the six concrete standard-resource
names, just as bare numeric costs already carry Terraforming Mars currency meaning. Treat those
leaks as one layering flaw instead of introducing a policy layer for this closed set.
