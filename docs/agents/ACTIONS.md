# Pets actions

> **NOTE:** This document is agent-maintained; source and tests take precedence.
>
> **Read when:** discussing action identity, arrow lowering, action availability, permission, or
> the relationship between an action's left side and Terraforming Mars payment.
>
> **Skip when:** discussing payment allocation or legality after debt exists; use
> [PAYMENTS.md](PAYMENTS.md).
>
> **Status:** current lowering plus the previously selected concrete-action Signal direction.
> That replacement remains unimplemented. Its representation, payment integration, and scheduling
> questions remain open; this document is not an implementation plan.

## Current divergence

An authored arrow currently lowers to an effect on `UseAction<Provider, ActionSlot>`. For an ordinary
left side, gaining that Signal begins `left side THEN right side`; the Signal precedes the work.
See [`Transforming.kt`](../../src/common/dev/martianzoo/pets/Transforming.kt), search
`actionListToEffects`, and action rules L7 in the
[Pets language specification](../pets-language-spec.md).

[`TfmActionLowerer.kt`](../../src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt), search
`actionToEffects`, separately recognizes six standard-resource classes. A fixed amount creates
`Owed` followed by `ActionBilling`; billing removal triggers the right side. An X-scaled amount
keeps its continuation in a generated sequence gated on billing removal. This splits the action
lifecycle between generic lowering and Terraforming Mars payment.

The `THEN` tree preserves shared X and Type-variable bindings. `ActionBilling<TradeAction>`
listeners require a meaningful action family. At the same time, the provider-and-slot type admits
meaningless pairs such as `UseAction<ConvertPlantsAction, Action2>`.

## Selected direction: concrete action Signals

The recorded direction uses a concrete Signal class for each actual action, beneath an abstract
`UseAction` family. A pending abstract task grants permission to attempt an available action. The
Signal is issued after its left side succeeds; an ordinary effect supplies the right side.

```text
pending task: UseAction
chosen class: DirectImpactors_Action1<DirectImpactors>
left side succeeds
issued Signal: DirectImpactors_Action1<DirectImpactors>
ordinary effect: DirectImpactors_Action1<DirectImpactors>: right side
```

The action class supplies both identity and the resulting event. A provider dependency makes the
choice available only while that provider is live. Abstract families such as `StandardAction` and
`TradeAction` remain meaningful; positional generated names can distinguish several arrows on the
same provider without retaining `ActionSlot` as a runtime dependency.

Enumeration need not prove the entire action feasible. Pets can attempt an operation and roll back
the enclosing transaction on a dead end. That does not provide rollback across previously committed
interactive choices or prove that unrelated tasks may safely intervene during the left side.

## Authoring contract

The rulebook-shaped form remains:

```pets
[left side] -> right side
```

The left side can remove resources, transform production, or bind a choice. General machinery needs
an executable instruction form that a game can replace with its payment process. It must preserve
X, Type variables, and selected values shared with the right side. A handwritten concrete action
Signal should be able to participate without requiring an authored arrow.

### Why the Action cost form is a product requirement

**Disposition: at peace with it.** The printed arrow form and parallel `Cost` AST preserve compact
notation such as `8 Plant ->`. `Spend`, `Per`, and `Transform` need not disappear merely because
execution uses an `Instruction`; see [VALUES.md](VALUES.md#keep-pets-central).

The current language specification defines arrow-to-`THEN` lowering and positional `Action1`–`Action3`
identity. The selected direction would require a specification change, not a reinterpretation of
those current rules.

## Left side and completion

The left side must finish before the proposed action Signal is issued. General action machinery
must be able to inspect and transform it without understanding or managing the right side. Direct
removals, production transformations, holder-sensitive resources, and ordinary choices remain valid
left-side instructions.

An instruction-valued property on the concrete action class is a possible carrier. It is not a
selected property design. A parallel instruction system or a persistent component mirroring the
provider would add costs that this direction is meant to remove.

Completion and exclusion are distinct. Existing `THEN` waits for its preceding task, not all work
that task causes. Existing billing supplies a payment-specific completion event. Neither generally
keeps another Player in control through a multi-task action.

Neptunian Power Consultants exposes this distinction: selecting the option delegates its decision;
accepting it creates payment choices whose current controller is still the active Player.
[PAYMENTS.md](PAYMENTS.md#delegated-payment-exposes-a-control-gap) records that source behavior and
the payment helper's cross-assignee selections. A successful helper call does not prove uninterrupted
control through interactive payment.

**Nested priority groups** and an **exclusive operation scope** are unresolved alternatives for
keeping delegated work under its decision owner's control and delaying unrelated work. Their
completion rule, treatment of nested delegation, and relationship to cleanup belong in
[SEQUENCING.md](SEQUENCING.md#delegated-operations-and-scheduling-options). The concrete-action
Signal direction does not choose between them, and a delayed Signal alone does not solve scheduling.

## Permission

The pending action task is the immediate right to attempt an action. Limited use belongs to the
route granting that task. The ordinary card-action route consumes the card's once-per-generation
permission; Viron and Project Inspection grant its inner action directly. Putting the restriction
on the inner Signal would break those bypasses.

The recorded permission direction uses a card-scoped available/used status, shared by all its arrows,
with generation cleanup restoring availability. Trade fleets are already positive player-owned
capacity, so their permission need not use that same representation.

Required-action availability has a related question: can the live requirement replace ordinary
standard-action providers with `DoRequiredActionsAction`, then restore them when resolved? A shared
player-owned provider dependency is a candidate, not an established runtime contract.

Current permission uses action task types, optional action tasks, capped markers, `TradeBarrier`,
and a gate on `UseAction`. Their differing roles must survive any simplification; their existing
representations need not. See
[`actions.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/actions.pets), search
`UseActionOnCardAction`, `DoRequiredActionsAction`, and `RequiredAction`.

## Terraforming Mars payment

Terraforming Mars can replace a standard-resource left side because discounts, surcharges, metal
substitution, and card-held resources change how the nominal amount is paid. Generic Pets should
not know those resources or billing classes.

The selected action direction requires the chosen action identity and shared bindings to survive
payment, cost adjustments to finish before payment commits, and successful settlement to issue the
same Signal as a direct left side. It does not require retaining `Owed`, `Billing`, or optional
payment-offer components. Card play can share a payment model without becoming an action.

Payment representation remains open in [PAYMENTS.md](PAYMENTS.md#design-options). Arbitrary resource
removals, rendered tasks, and causal strings are not an adequate domain payment interface. A floater
removal or production transformation remains ordinary left-side work unless the game deliberately
rewrites it as payment.

## Open design questions

- Does every arrow generate a dependent class, or can a named single-action provider itself be
  the Signal? Both must preserve provider availability and meaningful action families.
- What is the smallest executable representation of the left side that supports ordinary Pets
  and game-specific payment substitution while preserving shared bindings?
- What finishes a delegated left side, and when may the enclosing Player resume? Settlement,
  action results, and all their reactions need not denote the same point.
- Which permission representations remove current special cases without making ordinary action
  availability harder to express?

## Evidence to inspect

- [`Action.kt`](../../src/common/dev/martianzoo/pets/ast/Action.kt), search `Cost`, owns the current
  authoring representation; `Transforming.actionListToEffects` owns generic lowering.
- `TfmActionLowerer.actionToEffects` and Terraforming Mars `actions.pets` and `payment.pets` own
  the current game-specific split.
- [`VariableAmountActionsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/VariableAmountActionsTest.kt),
  [`UtopiaInvestTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/UtopiaInvestTest.kt), and
  [`VironTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/VironTest.kt) exercise shared
  quantities, linked resource choices, and direct-grant behavior that a replacement must preserve.
