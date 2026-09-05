# Pets Actions

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing Pets `Action` parsing/lowering, action selection or availability, billing,
> or cards with fixed, property-scaled, or X-scaled standard-resource costs.
>
> **Skip when:** changing an instruction that is not an `Action`, or changing payment
> allocation after an invoice has already been created; use [PAYMENTS.md](PAYMENTS.md) for that.
>
> **Status:** current model, except that "Permission" states a selected direction and the single
> payment-choice loop states a proposal. Both are marked where they begin.

## Read only the relevant sections

| If changing | Read |
| --- | --- |
| What an action *is*, or when one is available | Meaning and scope; Permission |
| `ActionSlot`, provider identity, or `UseAction` arity | Permission → Action identity |
| A cost denominated in a standard resource | Standard-resource costs; Composition |
| A card-resource, production, or selection cost | Direct and costless Actions |
| Payment task shape or client payment helpers | Proposed single payment-choice loop; Player-input staging |

## Source map

- [`Action.kt`](../../src/common/dev/martianzoo/pets/ast/Action.kt) — search for
  `public sealed class Cost` to inspect the parsed cost forms.
- [`Transforming.kt`](../../src/common/dev/martianzoo/pets/Transforming.kt) — search for
  `actionToEffects` for the lowering from `cost -> result` to triggered effects.
- [`PetTransformer.kt`](../../src/common/dev/martianzoo/pets/PetTransformer.kt) —
  search for `transformAction` before changing the lowering stage.
- [Terraforming Mars `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets)
  — search for `TURN AND ACTION PROTOCOL` for availability and `ABSTRACT CLASS Billing` for the
  payment protocol.
- [`DerivedClassLowerer.kt`](../../src/common/dev/martianzoo/pets/DerivedClassLowerer.kt) — read
  before proposing that an action get its own Class.
- [`VariableAmountActionsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/VariableAmountActionsTest.kt)
  — read when changing X costs or the identity of several Actions on one component.
- [`UtopiaInvestTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/UtopiaInvestTest.kt) —
  the one authored action sharing a Type variable across the arrow.
- [`VironTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/VironTest.kt) — the constraint
  that any availability change must satisfy.

## Meaning and scope

A **Pets Action** is the declaration written `[cost] -> result`. It is distinct from the larger
Terraforming Mars operation between one empty user task queue and the next.

One component may declare up to three Pets Actions. `Action1`, `Action2`, and `Action3` identify
them. `UseAction` and `Invoice` retain both the actual provider and this selector, so two Actions on
one card cannot settle one another. Invoice providers are live `HasActions` components, not their
Class tokens. `HasActions` is singleton-shaped because a dependency must identify one component.

`UseAction<HasActions, ActionSlot>` is a `Signal`: a player-caused *event*, gained and immediately
self-removed. An action's body is an ordinary effect keyed to that event, so nothing about an action
is a distinct engine concept. The gain obeys the ordinary dependency rule, so **the provider must be
live at the moment of use**. That single fact constrains every proposal below.

Because `UseAction` is an ordinary component, cards both subscribe to it and create it:

- subscribe — CrediCor, Kuiper Cooperative, Spire, Suitable Infrastructure, St. Joseph of Cupertino
  Mission, and every `UseAction<This, Action1>:: Accepting<...>` line;
- create — Viron, Project Inspection, Head Start, Neptunian Power Consultants, Cathedral,
  `RequiredAction`.

An action is therefore *not* under-modeled. What has no representation is the standing right to
cause the event.

### Why the Action cost form is a product requirement

**Disposition: at peace with it.** Do not report `Action` or `Cost` as removable sugar.

`Cost` is a second AST that parallels `Instruction` — `Spend`, `Per`, and `Transform`, each with its
own precedence and parser combinators — and it is fully desugared before the engine ever sees it, so
nothing under `engine/` references it. An action has at most one cost; conditions and independent
groups remain instruction concepts. On a cost/benefit ledger that reads as pure duplication, the
cost AST is bought only so an author may write `8 Plant ->` instead of `-8 Plant THEN`.

That reading is wrong. The `cost -> result` form is how the physical game prints an action, many
cards use it, and Pets reading like the printed card is a product requirement rather than a
convenience — see [VALUES.md](VALUES.md#keep-pets-central). The parallel AST is the accepted price
of that requirement.

What would still be worth reporting is a `Cost` form with no authored user, or a `Cost` construct
that survives into the engine instead of desugaring.

## Permission

**Status: selected direction.** The audit below is current model; the shape and the steps are not
yet implemented.

### The audit: one rule, five improvisations

Nothing in the model says "you may do this now." Availability is currently expressed five different
ways, three of them by arranging for an attempt to *fail*:

| Case | How availability is expressed | Polarity |
| --- | --- | --- |
| Standard actions | domain of the abstract task `UseAction<StandardAction>` | positive |
| Neptunian Power Consultants, Cathedral | an optional task `UseAction<X>?` | positive |
| Card action, once per generation | failure to gain `ActionUsedMarker`, capped `HAS MAX 1 This` | negative |
| Trade, once per fleet | `MAX 0 (Trade - TradeFleet): TradeBarrier<ColonyTile>` creates the barrier only when within the limit; a mandatory removal then fails when it was not created | negative |
| A `RequiredAction` blocking other actions | the gate task `This IF RequiredAction: MAX 0 RequiredAction: Ok` on `UseAction` | negative |

Rows three and four are the same rule — *N uses per generation* — solved twice, differently.
[TURMOIL.md](TURMOIL.md#policies) is poised to solve it a third way with `ScientistsUsedMarker`. By
the standard in [VALUES.md](VALUES.md#model-the-game-honestly), a repeated content-shaped workaround
means a general concept is missing.

The negative encoding also leaks upward. Three separate places compute availability by subtraction
rather than reading it: `TfmGameplay.pass` filters `ActionCard` by
`count("ActionUsedMarker<...>") == 0`, `AbstractFullGameTest` asserts
`count("ActionCard") - count("ActionUsedMarker")`, and `gameviewer/playedCards.hasActionUsedMarker`
inverts it again for display.

### What is *not* wrong

Three findings survived review and should not be re-litigated:

1. **The used marker is a real game component.** In the physical game a cube is placed on the card.
   The viewer displays it correctly and it must keep a component of its own. What is wrong is using
   its *absence* as the permission mechanism, not its existence.
2. **The doorway is the printed rule.** `UseCardAction` is a `StandardAction` because the player
   board lists "use a card action" among the standard actions, and
   [TURMOIL.md](TURMOIL.md#parties-dominance-and-ruling) reaches the same conclusion for `UsePartyActionSA`.
   Do not flatten the standard-action menu into one undifferentiated list of every available action;
   the `StandardAction` subclasses are exactly what the rulebook prints, and restoring
   `UseStandardProject` would make that correspondence more exact, not less.
3. **`UseAction` must stay unconditioned.** Viron, Project Inspection, and Head Start all hand a
   player a use that bypasses the normal route. Any condition attached to `UseAction` itself breaks
   them. **Conditions belong on the route, never on the act.** The routes are standard-action
   doorways, triggered offers, and direct grants.

Provider lifetime as availability is already the selected model where the provider is free to come
and go: `RequiredAction` is created, used, and destroyed; `NeptunianOption` and `CathedralOption`
exist so that an action can exist; and [TURMOIL.md](TURMOIL.md#policies) states it outright — a
policy's actions "disappear automatically when their policy components disappear." `ActionCard` is
the lone outlier, because its provider is permanent while its availability is not.

### The shape

Separate two facts that `ActionUsedMarker` currently conflates.

**Permission is a component, and spending it is a cost.** Represent the card's once-per-generation
right as a sum type over a card-scoped status — an abstract class with exactly one live concrete
face, where removing either face creates the other. Making the used face `Generational` then
restores the available face at generation turn with no host writing a renewal effect. The precedents
are `GpIncomplete`/`GpComplete` and the `CardLocation` subclasses.

The doorway then reads as the printed rule, and the permission genuinely is the cost:

```pets
CLASS UseCardAction { CardActionAvailable<ActionCard> -> UseAction<ActionCard> }
```

Viron and Project Inspection are unchanged, textually as well as behaviorally: `HAS
ActionUsedMarker` now reads the other face of the same fact instead of the absence of a blocker.
The permission is card-scoped, not slot-scoped, which is what preserves "one action per card per
generation" for a card that declares two.

Sharing the `ActionCard` Type variable across the arrow is supported and exercised: `UtopiaInvest`
writes `PROD[StandardResource] -> 4 StandardResource` and `UtopiaInvestTest` pins it.

**Action identity is a Class, not a pair.** `UseAction<HasActions, ActionSlot>` admits meaningless
pairs such as `UseAction<ConvertPlants, Action2>`, and that pair travels together through lowering,
`Invoice`, task instructions, and the event log. The direction is one live component per available
action, so `UseAction` takes one argument and invalid pairs become unrepresentable. Multi-action
classes would lower to derived offer Classes carrying the declaring Class as a dependency;
`DerivedClassLowerer` already performs exactly that lowering for `RequiredAction { -> ... }`, and its
current restriction is one *unnamed* derived Class per base per owner.

The offer Class lives as long as its provider. It is not the permission and is never withdrawn on
use — that is what keeps Viron able to name a spent action, and it is why the two facts must stay
separate rather than collapsing into one "offer" component.

**Enumerating the options is an engine gap, not a model gap.** "What can I do right now" is the
domain of the pending abstract `UseAction` task. `ENGINE.md` already specifies compositional choice
enumeration, and `ClassTable.allConcreteSubtypes` exists but is consumed only by `Limiter`,
`TypeDescription`, and `CustomClassRuntime` — never to offer a player their choices. The permission
change is what makes such an enumeration honest, because afterward the domain is a set of live
components instead of a set that must be probed against a limit.

Keep the boundary explicit. **Structural availability** — is this option present at all — should be
component presence, exactly enumerable. **Feasibility** — can the cost be met and the result legally
produced — stays attempt-and-roll-back per
[ENGINE.md](ENGINE.md#recoverable-dead-ends). Do not let the second leak into the first.

### Steps

Each step is independently valuable and independently revertible.

1. **Sum-type the card-action status.** Pets only; no engine change. Wins the positive rule, deletes
   the three subtraction sites, and keeps the physical cube. Verify against `VironTest`,
   `OtbGame20260825Test`, and `AbstractFullGameTest`.
2. **Apply the same shape to Trade.** `TradeFleet` splits into available and used faces;
   `TradeBarrier` and the `MAX 0 (Trade - TradeFleet)` comparison both go away. This is the step
   that shows whether renewable permission is a real primitive or a card-shaped coincidence — and it
   answers `ScientistsUsedMarker` before Turmoil lands. Do not extract a shared `Renewable`
   declaration until at least these two exist.
3. **Restore `UseStandardProject`.** Independent and small; recovers the rulebook's own list.
4. **Collapse `ActionSlot` into offer Classes.** The only step with engine cost: lowering, `Invoice`
   arity, `UseAction` arity, the six authored `UseAction<This, Action1>` triggers, and the REPL and
   test helpers. Sequence it after step 1, which removes the marker bookkeeping that most entangles
   the pair today.

## Standard-resource costs

A standard-resource Action has this lifecycle:

1. `UseAction` creates its exact `Owed<Resource>` amount.
2. Creating debt installs inert `Accepting<Resource>` markers. More debt may safely accumulate before
   payment becomes available.
3. The Action creates one provider- and action-qualified `Invoice<Provider, Selector, Resource>`.
   That concrete event implements the common `Billing` protocol, which exposes ordinary and
   card-based payment choices.
4. Payment removes `Owed`. When the invoice's resource debt reaches zero, the invoice removes
   itself.
5. The Action result responds to that `-Invoice` event.

The invoice is therefore both the conclusion of one obligation and its completion event.
There is no separate payment-completed signal.

Note that this cost is *not* a condition on the trigger: `actionToEffects` keys the effect to an
unconditional `UseAction<This, Action1>` and makes the cost the first stage of the result. It has to
be, because affordability is dynamic — discounts, metal substitutes, and card-held resources all
modify `Owed` before the invoice opens, so no static `Requirement` could express it. A permission
token is the one kind of cost that is a static condition, which is why it reads correctly as one.

Only invoice creation needs to state its denomination, and M€ is the creation default. Effects
normally respond to `Invoice<Provider, Selector>` because a particular Action has one nominal
denomination. A non-M€ creation retains `Class<Resource>` as accurate data naming the fungible debt;
it does not substitute a Class token for the live behavior provider.

On removal, bare `-Owed` deliberately leaves the denomination generic; `-Owed<>` explicitly accepts
the M€ dependency default. Thus one `Invoice<TradeAction>` reaction can discount every Trade action
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
concrete refinements are the currently legal `Accepting<Resource>` and
`AcceptingFromCard<Holder>` choices. Spending one unit creates a common payment Signal. After its
automatic value effects finish, Billing creates one replacement payment-choice task if matching
`Owed` remains.

If debt remains but no accepted tender is available, the payment task remains present but cannot be
selected. The queue therefore cannot drain and the encompassing operation dead-ends. Do not add
a separate `MAX 0 Owed: Ok` completion task: after successful payment it would compete with stale
unused-tender tasks and recreate the need to decline or clean them.

When no debt remains, no replacement task is created and Billing removes itself directly in
response to the final matching `Owed` removal. Existing `-Billing` effects remove the Accepting
capabilities; `-Invoice` remains the single completion event for the Action result. There is still
no separate `Paid` component and payment completion does not wait for unrelated Player work.

Card play uses the same debt-zero completion. Remove the pending
`MAX 0 Barrier: CardFront FROM CardBack` task; `-CardInvoice` instead puts the card into play as soon
as its debt is settled. EventCard lifetime is handled separately by whole-World idle cleanup; see
[SEQUENCING.md](SEQUENCING.md#current-behavior-whole-world-idle-cleanup).

### Player-input staging

Card play and standard-resource Actions expose one operation with ordered stages, not an
unstructured bag of removals and results. The player first chooses the card or Action. If that
choice creates Billing, the operation must finish that Billing stage before accepting any of the
resulting consequence choices. Billing may require zero or more tender selections. Only after it
closes can the player select direct effects or other queued consequences.

Client helpers must recognize payment from the live Billing stage, not by inspecting every
resource-removal instruction. This distinction is what keeps a direct floater cost, a production
transformation, or another holder-sensitive removal in the ordinary Pets consequence stage. It
also makes written payment/consequence interleaving invalid without inventing a special payment
syntax.

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
`CardInvoice<Class<CardFront>>`. Generic card-play modifiers respond to its `Billing<CardPlay>`
supertype, whose persistent owner covers standard actions, preludes, and other card-play routes.
Only modifiers that inspect the selected not-yet-live card use the specialized invoice and its Card
Class. The card moves face up only after all resulting barriers are gone, including requirement
debt.

Neptunian Power Consultants creates a live owned auxiliary `HasActions` component. An ocean offers
that component's Action, whose result puts the Hydroelectric resource on Neptunian Power
Consultants.

St. Joseph of Cupertino Mission creates one unowned `CathedralOption`. Each owned Cathedral offers
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

Permission is likewise a Terraforming Mars concept. `HasActions`, `UseAction`, and any permission
component belong to Canon; the generic layer owns only `Action`, its lowering, and the task
lifecycle that offers a choice.

## Open questions

Unresolved. Do not treat any answer below as decided.

1. **Where does the `=1` invariant on a card-action status live, and does it survive card entry?**
   `HAS =1 This` on the abstract status must not be violated in the instant between a card entering
   play and its available face existing. `Area` gets away with the same shape because its instances
   are created once at setup.
2. **Does a spent permission belong to the card or to the player?** Card-scoped is what preserves
   one-action-per-card. But Trade's permission is player-scoped and fungible, so step 2 will have to
   answer whether "renewable permission" is one primitive or two shapes that merely rhyme.
3. **Ordering between `Generational` removal and sum-type restoration.** `Generational` fires
   `Generation:: -This.`; the restoration is a `-This::` effect on the used face. Confirm this is one
   inline automatic chain and not two generation-boundary passes whose order matters.
4. **Can offer Classes avoid index-shaped generated names?** `DerivedClassLowerer` produces
   `Owner_Base`, and a card printing two unnamed arrows has nothing else to name them by. If the
   generated name is `RegolithEaters_Action1`, the slot has changed shape rather than disappeared —
   the invalid-pair win is real, but the naming win may not be.
5. **Is `RequiredAction`'s exclusivity expressible positively at all?** Obligation suppresses other
   options by nature. Trading `MAX 0 RequiredAction: Ok` for `NewTurn IF MAX 0 RequiredAction: ...`
   relocates the negation without removing it. It may simply be true that capability and obligation
   are different modalities and only the first is presence-shaped.
6. **Does Viron's "already used" reading hold?** The model requires the target to carry a used
   marker (`ActionCard(HAS ActionUsedMarker<!Viron>)`). Under the sum type that becomes a query on
   the other face, which is faithful to whatever the rule is — but the rule itself has not been
   verified against a Fryxelius ruling, and no one should verify it during unrelated work.
