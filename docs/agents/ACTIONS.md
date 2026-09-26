# Pets actions

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing action identity, arrow lowering, action availability, permission, or the
> boundary between an action's left side and Terraforming Mars payment.
>
> **Skip when:** changing tender selection, payment legality, or allocation after a debt exists; use
> [PAYMENTS.md](PAYMENTS.md).
>
> **Status:** selected direction with a bounded next experiment. The target lifecycle below is not
> implemented. [Current divergence](#current-divergence) records only the machinery that constrains
> the migration.

## Target model

Actions are choices supplied by the live World. A pending abstract action task grants its assignee
the right to attempt one of those choices; it does not promise that the chosen action can succeed.

The intended lifecycle is:

1. A rule creates a task for an abstract action family, such as a standard action or an action on a
   particular card.
2. Live providers and their dependencies determine the concrete action Classes available for that
   task.
3. Narrowing records one concrete action choice.
4. General action machinery obtains the chosen action's left-side `Instruction`. It executes that
   instruction directly or lets the game replace it with an equivalent process such as Terraforming
   Mars billing.
5. Successful completion of the left side gains the concrete action Signal.
6. The action's right side is an ordinary effect of that Signal. Action machinery does not inspect
   or manage it.

The Signal means “this action's left side succeeded,” not “begin trying this action.” A later
consequence may still dead-end; the enclosing transaction then rolls back the Signal and everything
it caused.

Keep the vocabulary small:

- the **action** is the concrete choice and the Signal ultimately issued for it;
- its **provider** is the live component, if any, that makes the choice available;
- the pending task is the right to attempt an action; and
- a **permission** component is additional game state limiting a route to an action.

Do not create parallel public concepts for offer, invocation, execution, and result unless they
prove to have independent behavior. Do not keep a persistent action component that merely mirrors
the provider.

## Authoring contract

The rulebook-shaped authoring form remains:

```pets
[left side] -> right side
```

Use **left side**, not cost or precondition, for its semantic role. It is an `Instruction`: it may
remove a resource, transform production, bind a choice, or be rewritten into billing. A handwritten
concrete action Signal can participate in the same runtime protocol even when no arrow generated it.

### Why the Action cost form is a product requirement

**Disposition: at peace with it.** Do not report the printed arrow form or the parallel `Cost` AST
as removable sugar. `Spend`, `Per`, and `Transform` preserve compact physical-component notation
such as `8 Plant ->` instead of `-8 Plant THEN`. The left side becoming an `Instruction` does not
require authors to write it as one; see [VALUES.md](VALUES.md#keep-pets-central).

The current language specification still defines arrow-to-`THEN` lowering and positional
`Action1`–`Action3` identity. Adopting the target model will require an explicit specification change,
not a reinterpretation of those rules.

## Concrete action identity

The selected direction is one concrete Signal Class per actual action, all beneath an abstract
`UseAction` family:

```text
pending task: UseAction
chosen Class: DirectImpactors_Action1<DirectImpactors>
action machinery: satisfy its left side
issued Signal: DirectImpactors_Action1<DirectImpactors>
ordinary effect: DirectImpactors_Action1<DirectImpactors>:: right side
```

The action Class is both identity and event. Its dependency on the provider makes it inhabitable
only while that provider is live. A task may still request an abstract family such as `UseAction`,
`StandardAction`, or `TradeAction`; narrowing chooses a concrete subtype.

Every authored arrow is one action. Position is sufficient generated identity when a provider has
several arrows, so generated names may retain `_Action1` without preserving `ActionSlot` as a
runtime dependency. Actions that rules address as one family must retain that common supertype:
Trade's three payment methods remain `TradeAction`s, and Fund Award's three prices remain one
declaring family.

One naming decision remains: a named single-action provider such as `ConvertPlantsAction` might be
the Signal itself, while arrows on cards generate dependent Signals, or every arrow might generate
one uniformly. The first prototype must make the cost of both options visible before selecting one.

## Left side and completion

Any representation of the left side must preserve these constraints:

1. General action machinery can inspect and transform it without understanding the right side.
2. The left side finishes before the concrete action Signal is issued.
3. Ordinary instructions remain valid for direct removals, production transformations,
   holder-sensitive resources, and choices.
4. A game may recognize a domain form and substitute its own equivalent workflow.
5. X, Type variables, and selected values shared across the arrow survive until the right-side
   effect runs.
6. Feasibility remains attempt-and-rollback; enumeration need not prove every action can finish.

An instruction-valued property on the concrete action Class is the leading carrier to test. It
would expose the left side to general machinery and a game transformer without inventing a second
instruction system. This is a hypothesis, not a selected property design.

Completion should use the narrowest fact the operation already provides. A direct instruction may
need only ordinary sequencing. Settled billing may provide its own completion event. Work with
transitive descendants may require the causal-completion direction in
[SEQUENCING.md](SEQUENCING.md#the-missing-rule-when-an-operation-is-over). Do not introduce
`Temporary` merely to bridge a payment representation that has not yet been selected.

## Permission

The pending abstract task is the immediate right to attempt an action. Limited use is a separate
fact and belongs on the left side of the route that consumes it.

This distinction preserves deliberate bypasses. The ordinary card-action route spends that card's
once-per-generation permission and then grants its inner action. Viron and Project Inspection grant
the inner action directly, so the route restriction must not be attached to the inner Signal.

The target card model has a positive card-scoped status with exactly one live face: available or
used. The route removes the available face; the physical used marker remains visible; generation
cleanup restores availability. One status covers a card even when the card has several arrows.

Do not force every permission into that shape. A `TradeFleet` is already positive player-owned
capacity and several fleets are fungible; Trade needs a clean way to consume or commit one fleet,
not an available/used card status.

A live `RequiredAction` should positively replace ordinary standard-action providers with the
provider of `DoRequiredActionsAction`, then restore them when the requirement disappears. The
smallest candidate is one player-owned component on which ordinary standard actions depend. Verify
its setup and gain/removal invariants before considering an action-mode sum type.

Current Canon improvises permission through the action task's Type domain, optional action tasks,
failed capped-marker gains, `TradeBarrier`, and a negative gate on `UseAction`. These are evidence
for one missing concept, not five mechanisms to preserve.

## Terraforming Mars payment boundary

Terraforming Mars must be able to replace a standard-resource left side with its own payment
process because discounts, surcharges, metal substitution, and card-held resources change how the
nominal amount is satisfied. Generic Pets must not know those resources or billing Classes.

The action contract requires only:

- one chosen action identity survives through settlement;
- adjustments finish before tender can commit;
- consequences wait for successful completion; and
- completion issues the same concrete Signal used by a direct action.

Whether the selected payment design retains `Owed`, `Billing`, or any existing offer component is
open. [PAYMENTS.md](PAYMENTS.md) owns the investigation and explicitly rejects the old one-unit
payment loop. Card play may share a debt and tender model without becoming an action.

Client helpers should recognize live domain obligation state, never arbitrary resource-removal
tasks, rendered instructions, or causal strings. A floater removal or production transformation is
ordinary left-side work unless Terraforming Mars deliberately rewrites it as payment.

## Current divergence

Today an authored arrow lowers to an effect on `UseAction<Provider, ActionSlot>`. For an ordinary
left side, gaining that Signal begins `left side THEN right side`. The Signal therefore precedes the
work whose success it is intended to denote.

`TfmActionLowerer` separately recognizes six standard-resource Classes. A fixed amount creates
`Owed` and `ActionBilling`, whose removal directly triggers the right side; an X-scaled amount keeps
the continuation in a generated sequence. This preserves current games but splits one action
lifecycle across generic lowering and Terraforming Mars billing.

The existing `THEN` tree preserves shared X and Type-variable bindings. Any delayed-Signal design
must preserve that behavior without parallel identity data. `ActionBilling<TradeAction>` listeners
also show why concrete actions still need meaningful family supertypes.

Current permission has the five improvised shapes summarized above. `UseAction<Provider,
ActionSlot>` also admits meaningless pairs such as `UseAction<ConvertPlantsAction, Action2>`. These
facts justify the redesign; they are not a migration checklist.

## Ownership

Generic Pets owns arrow syntax, the abstract action protocol, concrete action identity, left-side
handling, and the transition from successful left side to Signal.

Terraforming Mars owns standard resources, debt and tender semantics, recognition of billable left
sides, card-action permission, standard-action routes, and Trade fleets. A game transformer may
rewrite an ordinary instruction but must return to the same generic action Signal protocol.

## Next phase: constrained prototype

**Selected next step:** build a disposable, generic direct-action prototype before migrating Canon.
Use one tiny test Catalog with one live provider and one non-billing arrow.

The prototype must show that:

1. an abstract action task narrows to a provider-dependent concrete Signal Class;
2. general machinery can obtain and execute the left-side `Instruction`;
3. the Signal is gained only after that instruction succeeds;
4. the right side exists only as an ordinary effect of the Signal;
5. an absent provider removes the choice and a failed left side leaves no trace; and
6. Signal order and self-cleanup remain ordinary engine behavior.

Use it to decide whether every arrow generates a Class and whether an instruction-valued property
is the smallest honest carrier. Stop if it needs a persistent duplicate provider, parallel action
identity, a second instruction representation, or special handling of the right side.

Do not change production cards, payment, permission, or required-action availability in this
prototype. After it succeeds:

1. let the investigation in [PAYMENTS.md](PAYMENTS.md#decision-and-implementation-sequence) select
   a payment representation;
2. carry one fixed billable action through that representation to the same concrete Signal;
3. verify shared X and Type variables, multi-arrow providers, and family listeners; then
4. migrate production declarations only if the result deletes more identity and orchestration than
   it adds.

Permission and positive required-action providers remain separate work. Causal completion must
delete a real client bridge before it grows into a general scope mechanism.

## Evidence to inspect

- [`Action.kt`](../../src/common/dev/martianzoo/pets/ast/Action.kt),
  [`Transforming.kt`](../../src/common/dev/martianzoo/pets/Transforming.kt), and action rules L7 in
  the [Pets language specification](../pets-language-spec.md) define current generic lowering.
- [`TfmActionLowerer.kt`](../../src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt),
  [Terraforming Mars `actions.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/actions.pets),
  and [`payment.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/payment.pets) define
  the current domain split.
- [`VariableAmountActionsTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/VariableAmountActionsTest.kt),
  [`UtopiaInvestTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/UtopiaInvestTest.kt), and
  [`VironTest.kt`](../../test/common/dev/martianzoo/tfm/tests/cards/VironTest.kt) preserve shared
  binding, multiple-arrow, and direct-grant behavior relevant to the prototype.
