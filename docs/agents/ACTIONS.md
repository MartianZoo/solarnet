# Pets Actions

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing what an action is, Pets `Action` parsing or lowering, action selection,
> action availability, billing, or cards with fixed, property-scaled, or X-scaled
> standard-resource left sides.
>
> **Skip when:** changing payment allocation after an invoice has already been created; use
> [PAYMENTS.md](PAYMENTS.md) for that.
>
> **Status:** current implementation plus a working direction. The semantic lifecycle under
> “Working model” is the direction to design toward. Concrete action signals, placement and
> rewriting of the left-side instruction, and the single payment-choice loop remain proposals.

## Read only the relevant sections

| If changing | Read |
| --- | --- |
| What an action is | Working model; Actions and their syntax |
| `ActionSlot`, action identity, or `UseAction` | Concrete action identity |
| Card-action, Trade, or required-action availability | Routes and permission |
| Action lowering | The left side; Current implementation gap |
| A standard-resource left side | Terraforming Mars payment rewrite; Composition |
| Payment task shape or client payment helpers | Single payment-choice loop; Player-input staging |

## Source map

- [`Action.kt`](../../src/common/dev/martianzoo/pets/ast/Action.kt) — search for
  `public sealed class Cost` to inspect the parsed left-side forms.
- [`Transforming.kt`](../../src/common/dev/martianzoo/pets/Transforming.kt) — search for
  `actionToEffects` for current lowering.
- [`PetTransformer.kt`](../../src/common/dev/martianzoo/pets/PetTransformer.kt) — search for
  `transformAction` before changing the lowering stage.
- [Terraforming Mars `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets)
  — search for `TURN AND ACTION PROTOCOL` and `ABSTRACT CLASS Billing`.
- [`DerivedClassLowerer.kt`](../../src/common/dev/martianzoo/pets/DerivedClassLowerer.kt) — read
  before generating a Class for each authored action.
- [`VariableAmountActionsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/VariableAmountActionsTest.kt)
  — read when changing X or several actions on one component.
- [`UtopiaInvestTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/UtopiaInvestTest.kt) —
  preserves a Type variable shared across both sides of an arrow.
- [`VironTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/VironTest.kt) — preserves a direct
  action grant that bypasses the normal card-action route.

## Working model

Actions are choices supplied by the active components of the World. A pending abstract action task
grants its assignee the right to attempt one of those choices. It does not promise that the chosen
action can be completed.

The intended lifecycle is:

1. A rule creates a task to gain some abstract action type, such as a standard action or an action
   belonging to a particular card.
2. The live components and their dependencies determine the concrete choices to which that task can
   be narrowed.
3. Narrowing records which action was chosen.
4. General action machinery resolves the chosen action's left side. It may execute it directly or
   let a domain-specific transform replace it with a workflow such as Terraforming Mars billing.
5. Only after the left side has been satisfied does the machinery gain the chosen action Signal.
6. The action's right side is an ordinary effect responding to that Signal. The action machinery
   has completed its responsibility and does not inspect or manage the right side.

The Signal is therefore the successful product of action handling, not necessarily the beginning
of it. Before the Signal, an action is being selected and prepared. Once the Signal is issued, its
right side behaves exactly like every other triggered Pets effect. If a later consequence dead-ends,
the encompassing transaction rolls back, including the Signal.

This explains both the unity and the apparent split between actions and triggered effects. There is
no special kind of right-side execution. Actions need additional machinery only because a player
chooses one and because its left side is available for mediation before an ordinary trigger is
issued.

Keep the vocabulary small:

- An **action** is the concrete choice and the Signal ultimately issued for it.
- Its **provider** is the live component, if any, that makes that action a choice.
- A pending engine task represents the right to attempt an action.
- A permission component records a limited use where the game needs one.

Do not introduce separate public concepts for an offer, invocation, and execution unless the model
eventually proves that they have independent behavior.

## Actions and their syntax

An action is not defined by use of the arrow syntax. A handwritten concrete action Signal can be an
action too. It may not receive transformations that depend on metadata produced by the usual
authoring form, but it still participates in the same task and Signal protocol.

The Pets declaration `[left side] -> right side` is the main authoring form. It should remain close
to the physical component and compile into the same semantic model as an explicitly declared action
Signal. The syntax is valuable, but it is not the ontology.

### Why the Action cost form is a product requirement

**Disposition: at peace with it.** Do not report the printed arrow form as removable sugar.

The current `Cost` AST parallels `Instruction`: `Spend`, `Per`, and `Transform` have their own
precedence and parser combinators. The form exists so authors can write `8 Plant ->` instead of
`-8 Plant THEN`. That visual correspondence with the physical game is a product requirement; see
[VALUES.md](VALUES.md#keep-pets-central).

The left side becomes an `Instruction`. Calling it a cost is still too narrow: that instruction may
take a component, transform production, or bind a choice. What remains open is where the instruction
lives after parsing, when a domain may rewrite it, and what event establishes that it has completed.
Being at peace with the authoring form does not commit the implementation to today's
`Cost.toInstruction()` lowering stage.

## Concrete action identity

### Current model

One provider may declare up to three arrows. `Action1`, `Action2`, and `Action3` select among them.
`UseAction<HasActions, ActionSlot>` and `Invoice<HasActions, ActionSlot, Resource>` carry both the
live provider and the positional selector. This admits meaningless combinations such as
`UseAction<ConvertPlantsAction, Action2>`.

`UseAction<Provider, Slot>` is currently a self-removing `Signal`. Provider dependency ensures the
provider is live when it is gained. The right side, and currently also most left-side handling,
responds to that gain.

### Proposed concrete Signals

A promising replacement is for `UseAction` to be an abstract Signal supertype and for each actual
action to be a concrete subtype:

```text
pending task: UseAction
chosen Class: DirectImpactors_Action1<DirectImpactors>
action machinery: satisfy its left side
issued Signal: DirectImpactors_Action1<DirectImpactors>
ordinary effect: DirectImpactors_Action1<DirectImpactors>:: right side
```

The dependency on `DirectImpactors` makes that concrete choice inhabitable only while the card is
live. The action Class supplies identity; its gained instance is the event. No persistent second
component needs to duplicate the provider's presence.

This loses nothing essential from the generic verb if every concrete action remains a subtype of
`UseAction`. A task can still request any `UseAction`; shared rules can still listen to that
supertype; more specific supertypes such as `StandardAction` can still group actions. What changes is
that identity is expressed by the concrete Class instead of by generic arguments.

Authored arrows need not receive semantic names. Card authors normally provide only their order.
Generated names such as `RegolithEaters_Action1` accurately preserve the identity the author did
provide. Removing `ActionSlot` from the runtime relation still eliminates invalid provider/slot
pairs even if a positional suffix remains in a generated Class name.

Single-action declarations need a consistent rule. A named class such as `ConvertPlantsAction`
might itself be the concrete Signal, while a card with arrows might produce dependent derived
Signals. Alternatively every arrow could produce a derived Signal. The first avoids a duplicate
Class for standard actions; the second is more uniform. Do not implement either until this is
settled.

Every authored arrow is one action. Its position is therefore sufficient generated identity when
the containing component has several arrows.

This rule describes the Pets model, which may intentionally differ from the printing. Electro
Catapult prints one arrow with a Plant-or-Steel choice, but Pets gives it two arrows and therefore
two actions. Trade likewise has three actions for its three payment methods, and Fund Award has
three actions for its three prices. Splitting `TradeAction` itself into three public standard-action
classes would expose that implementation choice in the rulebook-shaped menu, so keeping one
declaring family with three generated action Classes is preferable.

## Routes and permission

The pending abstract task is the immediate right to attempt an action. Limited-use components are
additional game facts, not replacements for that task.

The normal card-action route illustrates the distinction. `UseActionOnCardAction` is a printed
standard action. Its left side can spend the card's once-per-generation permission; after its own
Signal, its ordinary right-side effect creates the narrower task for the selected card action.
Viron and Project Inspection instead create that inner task directly, so they bypass the normal
route without weakening the card action itself.

Conditions that regulate ordinary access belong to the route. They must not be attached to the
inner action Signal when direct grants are allowed to bypass them.

### Current availability audit

The current model says “you may do this now” in several unrelated ways:

| Case | Current encoding |
| --- | --- |
| Standard actions | domain of `UseAction<StandardAction>` |
| Neptunian Power Consultants, Cathedral | optional `UseAction<X>?` task |
| Card action, once per generation | failed attempt to gain capped `ActionUsedMarker` |
| Trade, once per fleet | `TradeBarrier` created through a count comparison, then mandatory removal |
| Required actions | a negative gate on every `UseAction` |

Card actions, Trade, and future Turmoil policy uses all express renewable permission differently.
The negative card encoding also leaks into gameplay, tests, and the viewer as subtraction from the
set of action cards.

### Selected permission direction

Permission is a component and satisfying it belongs on the left side of the route action. For a
card, use a card-scoped status with exactly one live face: available or used. Removing the available
face creates the physical used marker; generational removal of the marker restores the available
face. This preserves the cube that the physical game places on the card while making availability a
positive fact.

Conceptually, the doorway remains:

```pets
CLASS UseActionOnCardAction {
  CardActionAvailable<ActionCard> -> UseAction<ActionCard>
}
```

The exact spelling will change if concrete action Signals replace generic `UseAction` refinements.
The card-scoped status preserves one action per card per generation even when a card prints several
actions.

Do not assume card status and Trade require one permission abstraction. A card needs an
available/used status because the card remains live after its use. A `TradeFleet` is already the
positive, player-owned capacity to trade and several fleets are fungible. Trade needs a cleaner way
to consume or commit one fleet, not necessarily a second face shaped like the card marker.

`DoRequiredActionsAction` may remain a `StandardAction`. It is the deliberate model action that lets
a mandatory effect consume its action-phase slot. The unresolved issue is not its classification
but how a live `RequiredAction` replaces the components supplying ordinary standard actions with
the component supplying only this action, and restores the ordinary set when it disappears.

The smallest current candidate is one player-owned component on which every ordinary standard
action depends. Gaining `RequiredAction` removes that component; `DoRequiredActionsAction` depends
directly on the live requirement; removing the requirement restores ordinary availability. The
pending abstract standard-action task would then have exactly the intended concrete domain in both
states. Verify the gain/removal transitions and setup invariant before preferring this to a fuller
action-mode sum type.

## The left side

Use **left side** as the neutral term. Its semantic value is an `Instruction`. “Cost” is accurate for
many printed actions but not broad enough for the role. “Precondition” suggests a passive boolean
check, while the instruction may actively remove, take, transform, select, or bill.

Whatever representation is chosen must preserve these rules:

1. The general action machinery may inspect and transform the left side without understanding the
   right side.
2. The left side completes before the concrete action Signal is issued.
3. Ordinary instructions should remain usable for direct removals, production transformations,
   holder-sensitive resources, and selections.
4. Terraforming Mars may recognize standard-resource forms and replace them with `Owed` and
   `Invoice` workflow.
5. Type variables, selected values, and X shared across the arrow must survive until the right-side
   effect responds to the Signal.
6. Dynamic feasibility remains attempt-and-rollback. Enumeration need not prove that every left
   side and every later consequence will succeed.

An instruction-valued Class property is one plausible place to store it. It would let a concrete
action Class carry its left side as data and let a domain transformer mediate it before the Signal
is gained. The value type is not in question; property semantics, variable binding, and
transformation ownership still are.

Today the direct case already preserves bindings without a separate workflow object.
`Action.toInstruction()` builds one `THEN` tree from the left side and right side and carries their
Type-variable scope onto that tree. `Then.ensureIsNarrowedBy` binds those variables across its
stages and checks a shared X value. X-scaled standard-resource lowering likewise keeps debt,
invoice, and continuation in one generated sequence. A redesign should reuse this mechanism if it
can; inserting a delayed action Signal must not accidentally discard the binding environment.

### Current implementation gap

Current lowering has the opposite signal order.

- For a nonstandard left side, `actionToEffect` makes `UseAction<Provider, Slot>` trigger
  `left-side instruction THEN right side`.
- For a fixed standard-resource left side, `UseAction` creates `Owed` and `Invoice`, and
  `-Invoice` directly triggers the right side.
- X-scaled standard-resource actions keep the right side in a local continuation following invoice
  creation.

Thus the current `UseAction` Signal means “choice accepted; begin all action work.” In the working
model, it should mean “the general action machinery has successfully satisfied this action's left
side.” The desired standard-resource chain is:

```text
choose concrete action
→ create adjusted debt and invoice
→ settle invoice
→ issue concrete action Signal
→ ordinary right-side effect
```

This is a semantic change, not merely a rename. It should be proved first for a direct left side,
a fixed billable left side, an X-scaled action, and a Type variable shared across the arrow.

The remaining completion question is about sequencing, not the value of the left side. `THEN` knows
that one instruction task completed, but does not wait for all work descended from that task.
Invoice removal is already a precise completion event for payment, and a strictly sequential
payment loop may therefore need no `Temporary`. If some other left-side instruction must wait for
all work it caused, whole-World-idle `Temporary` is too broad; the scoped-completion direction in
[SEQUENCING.md](SEQUENCING.md#the-missing-rule-when-an-operation-is-over) is the relevant candidate.

## Terraforming Mars payment rewrite

Standard-resource left sides require domain machinery because discounts, surcharges, metal
substitution, and resources held by cards can change how a nominal amount is satisfied. A static
requirement cannot express that process.

The current billing facts remain useful:

1. `Owed<Resource>` records the adjusted fungible debt.
2. Debt enables `Accepting<Resource>` and card-held substitutes.
3. A qualified `Invoice` implements `Billing` and exposes payment choices.
4. Payments remove `Owed`.
5. When matching debt reaches zero, the invoice removes itself.

Under the working model, invoice removal completes the left side and causes the chosen action
Signal. The right side then responds to that Signal instead of directly to `-Invoice`. No separate
`Paid` component is needed.

Only invoice creation needs to name its denomination, and M€ is the default. Discounts and
surcharges modify `Owed` before the invoice exists. Accepted substitutes reduce that same invoice;
they are not parallel kinds of debt.

The invoice must retain enough identity to correlate with the selected action before that action's
Signal exists. If action identity becomes a concrete Class, the likely key is the action Class plus
whatever live provider dependency modifiers need. It cannot depend on the ephemeral Signal
instance because that instance is deliberately issued only after settlement.

### Single payment-choice loop

**Status: proposal.** Today each tender kind creates its own optional task. Paying with one kind can
leave stale alternatives that callers must decline or clean up.

Replace them with one required task meaning “pay one accepted unit.” Its refinements are the legal
`Accepting<Resource>` and `AcceptingFromCard<Holder>` choices. Spending one unit creates a common
payment Signal. After its automatic value effects finish, Billing creates one replacement payment
task if matching debt remains.

If debt remains with no accepted tender, the payment task has no concrete choice and the operation
dead-ends. When no debt remains, Billing removes itself; under the working model that removal lets
the action machinery issue the selected action Signal.

Card play uses the same debt-zero rule but is not necessarily an action. `-CardInvoice` can put the
card into play as soon as its debt is settled. EventCard lifetime remains owned by whole-World idle
cleanup; see [SEQUENCING.md](SEQUENCING.md#current-behavior-whole-world-idle-cleanup).

### Player-input staging

An action with billing is one operation with ordered stages. The player chooses the action, settles
its left side through zero or more payment choices, and only then receives consequences created by
the action Signal's ordinary effects.

Client helpers must recognize payment from live Billing state, not from arbitrary resource-removal
instructions. A direct floater removal, production transformation, or holder-sensitive removal is
ordinary left-side work unless Terraforming Mars deliberately rewrites it as billing.

## Composition

Several additions to `Owed` may precede one invoice. Card buying adds 3 M€ per selected card, so
Polyphemos and Terralabs Research can alter the same debt before the invoice opens.

Trade has three actions for 9 M€, 3 energy, or 3 titanium. Cryo-Sleep and Rim Freighters both write
`Invoice<TradeAction>:: -Owed`; the bare removal follows the invoice's selected denomination, so
the cards already need not name MC, energy, titanium, or an action slot. Concrete Trade action
Classes must remain members of a common `TradeAction` family so this one listener continues to
cover all three.

Fund Award has three actions—8, 14, and 20 M€—whose right sides are gated by existing Award count.
They should likewise remain one declaring family even though each arrow receives its own concrete
action identity.

Standard projects retain authored arrows such as `1 MC / cost -> OceanTile<>`; the project Class
supplies the variable price and Terraforming Mars supplies billing. `UseStandardProjectAction` is
only the standard-action doorway that delegates to this separate set.

Neptunian Power Consultants and Cathedral use auxiliary live providers so their actions exist only
when the option exists and are owned by the correct player. Concrete action Signals should preserve
that dependency-driven behavior without requiring persistent duplicate action components.

## Ownership

`Action` syntax, the abstract action protocol, concrete action identity, and the transition from a
satisfied left side to a Signal belong to generic Pets.

`StandardResource`, `Owed`, `Invoice`, card-action permission, Trade fleets, standard-action routes,
and the recognition of billable Terraforming Mars left sides belong to Terraforming Mars. The
generic transformer currently recognizes six Terraforming Mars resource names; treat that as one
existing layering flaw rather than justification for pushing billing into generic Pets.

## Program of work

Do not begin the action-identity rewrite until the first three ranked questions below have concrete
answers. Card-permission cleanup can proceed independently if it preserves direct grants.

1. Replace the negative card-action availability calculation with the available/used status, while
   preserving the physical marker and Viron behavior.
2. Prototype one concrete action Signal with a direct left side. Its right side must be an ordinary
   `This::`-style effect and the Signal must occur only after the left side succeeds.
3. Extend that prototype through standard-resource billing. Invoice completion should issue the
   Signal; the Signal should trigger the unchanged right side.
4. Prove existing `THEN` binding and X behavior can survive the inserted Signal before replacing
   `ActionSlot` generally.
5. Replace provider/slot identity only after card actions, standard projects, Fund Award, Trade,
   direct grants, generic listeners, logs, and client choice enumeration all have a coherent form.
6. Replace the required-action gate only after a positive provider transition can remove and
   restore the ordinary standard-action set without transient invalid states.

## Ranked questions

Unresolved, in decision order:

1. **Is every actual action a concrete Signal subtype of abstract `UseAction`?** This would make the
   action Class its identity and the gained component its event, with provider dependency supplying
   availability. Decide this before choosing any new `UseAction` arity or creating persistent offer
   components.
2. **Where does an action's left-side `Instruction` live, and at what stage may a domain rewrite
   it?** An instruction-valued property on the concrete action Class is plausible, but it must compose
   with normal Pets transformation rather than create a second instruction system.
3. **What exact completion causes the action Signal?** Plain `THEN` may suffice for a direct
   instruction; invoice removal may suffice for sequential payment; descendant work may require a
   scoped completion component. Determine where, if anywhere, `Temporary` still earns a role.
4. **Can the delayed Signal preserve today's binding behavior and declaring-family matching?** The
   existing `THEN` tree carries Type variables and shared X; Trade modifiers target all three arrows
   through `Invoice<TradeAction>`. A concrete-Signal design must preserve both without parallel
   identity data.
5. **How does `RequiredAction` positively replace and restore the ordinary action providers?** The
   leading shape is one removable player-owned provider for ordinary standard actions, while the
   live required component supplies `DoRequiredActionsAction`. Work out its setup and transition
   invariants before deciding whether a fuller action-mode sum type is necessary or deleting the
   current gate.
