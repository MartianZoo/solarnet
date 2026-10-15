# Payment model

> **Agent information:** This is an agent-maintained note for agents.
>
> **Read when:** changing payment choices, accepted resources, debt, billing, resource values, or
> payment APIs.
>
> **Status:** current contract and verified gaps. Proposed replacements belong in the Solarnet
> roadmap or a focused investigation, not here.

## Current contract

The [authored payment model](../../pets/docs/agents/PAYMENTS.md#current-contract) owns debt,
acceptance, payment signals, and settlement. The runtime executes those declarations.

The linked `X` choice still appears after `UseCardAction` fires, so `ActionUsedMarker` may exist
while the choice is open. The intended later design would let a client set `X` on the printed
action before committing its use, then perform an ordinary concrete action. That requires action
invocation and its consequences to share the same choice. Inferring `X` backwards when a caller
tries to perform `3 Foo` against `X Foo` is optional and does not justify extra machinery by itself.
A client that already knows `X = 5` can submit `selectTask(task.bindXTo(5))` directly. `TaskForm`
remains optional for discovering open choices. This still selects the pending consequence after
`UseCardAction`; moving the choice to the printed action before invocation remains future work.

## Ordinary resource losses

Pharmacy Union's disease losses and Flooding's self-inflicted loss leave an open bill unchanged.
Their sandbox tests resolve those losses before the explicit payment and verify the full cost.
Pharmacy Union's starting 54 MC is queued behind a zero-disease gate; its microbe reaction places
disease before offering the capped 4 MC loss. Choosing disease before starting cash can leave setup
unable to finish, requiring undo. This is allowed; the model need not make every available choice
lead to completion. No special starting-resource phase or autoexecution exception is used.

## Interleaving and rollback

Accepting Neptunian's offer does not prevent the player playing Flooding from resolving its loss
while the purchase remains unpaid. This ordering is allowed. On 2026-10-14 the owner explicitly
confirmed that making the purchase unaffordable this way is a voluntarily dead-ended turn, not a
bug: the player playing Flooding must roll back.

`FloodingTest` covers losses both before payment and after a separate Steel payment. The loss
never credits the bill. A failed cash payment leaves the earlier Steel payment intact; rolling
back the enclosing turn restores both players' resources and removes the debt. These scenarios do
not require a lock around the purchase or automatic rollback of earlier payment commands.

Payment offers are routed by their effect owner, changed component owner, or triggering Actor.
The `pay` helper selects through each task's current assignee. Calling those Agents is legitimate
calculator use, independently of who operates the external caller.

## Current pieces

Kotlin payment helpers submit explicit exchanges, decline unused alternatives, and settle
the exact remainder. The [model pieces](../../pets/docs/agents/PAYMENTS.md#current-pieces)
own pricing and acceptance.

The sandbox suite exercises this ordering with explicit submissions. Gameplay helpers suspend
player autoexecution while submitting a specified payment allocation. Under `CONCRETE` or `EAGER`,
a cash-only bill may otherwise finish automatically; callers must not submit that payment again.
Custom allocations belong in the helper's payment callback, before the purchased result.
The script `tfm_pay` command uses the same allocation helper; `tfm_play` and `tfm_action` pause
player autoexecution while submitting a written payment, then restore the configured policy.

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
engine tasks do not apply the same whole-allocation check. The physical-game
rule for the characterized mixed-metal case is unverified; do not describe current behavior as a
settled rules defect.

## Evidence

Start with:

- [`payment.pets`](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/payment.pets)
- [`card-model.pets`](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/card-model.pets)
- [`TfmActionLowerer.kt`](../../pets/src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt)
- `PaymentSpecializationTest`, `DirigiblesTest`, `PsychrophilesTest`,
  `KuiperCooperativeTest`, `StormcraftIncorporatedTest`, `BoomTownTest`, and
  `StandardTechnologyTest`

Preserve observable card/action outcomes, discounts, values, source restrictions, rollback, and
settlement before consequences. Do not create mirrored allocation state or card-specific client
orchestration to address a local symptom.
