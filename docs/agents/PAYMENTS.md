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
acceptance, payment signals, and payment completion. The runtime executes those declarations.

The linked `X` choice still appears after `UseCardAction` fires, so `ActionUsedMarker` may exist
while the choice is open. The intended later design would let a client set `X` on the printed
action before committing its use, then perform an ordinary concrete action. That requires action
invocation and its consequences to share the same choice. Inferring `X` backwards when a caller
tries to perform `3 Foo` against `X Foo` is optional and does not justify extra machinery by itself.
A client that already knows `X = 5` can submit `selectTask(task.bindXTo(5))` directly. `TaskForm`
remains optional for discovering open choices. This still selects the pending consequence after
`UseCardAction`; moving the choice to the printed action before invocation remains future work.

## Ordinary resource losses

Pharmacy Union's disease losses and Flooding's self-inflicted loss leave an open payment unchanged.
Their sandbox tests resolve those losses before the explicit payment and verify the full cost.
Pharmacy Union's starting 54 MC is queued behind a zero-disease gate; its microbe reaction places
disease before offering the capped 4 MC loss.

Payment offers are routed by their effect owner, changed component owner, or triggering Actor.
Calling an offer's assigned Agent is legitimate calculator use, independently of who operates
the external caller.

## Current pieces

The [model pieces](../../pets/docs/agents/PAYMENTS.md#current-pieces) own pricing and acceptance.
Sandbox tests and migrated replays submit resource exchanges directly before the purchased result.

The shared `TfmRoutines` `pay` routine submits the stated standard or card-held resources in the
caller's order. It declines unused optional offers before paying the exact-denomination remainder
and acknowledges an offered zero remainder after alternatives cover the bill. These choices use
the offered instructions, not their causes.

`pay` temporarily pauses the participating Players' effective automatic-execution policies while
submitting that complete allocation. Mandatory Admin processing remains active. It then restores
the enclosing policies and runs configured work before returning. Failure restores the whole
invocation, including automatic work; a previously returned `playCard` invocation remains pending.
The importer and REPL both call this implementation. See [routines](ROUTINES.md).

## Allocation validation

Pets splits an instruction group into independent tasks. Shared variables can link a `THEN`
sequence, but they do not give a task separately chosen quantities of cash, Steel, Titanium, and
card-held resources. A complete allocation task would require new task semantics or a domain
instruction; it is not a spelling change.

Engine tasks and the `pay` routine do not check whether a selected resource unit could be returned
while the remaining allocation still covers the debt. Card-held resources are submitted separately.

## Evidence

Start with:

- [`payment.pets`](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/payment.pets)
- [`card-model.pets`](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/card-model.pets)
- [`TfmActionLowerer.kt`](../../pets/src/common/dev/martianzoo/tfm/canon/TfmActionLowerer.kt)
- `PaymentSpecializationTest`, `DirigiblesTest`, `PsychrophilesTest`,
  `KuiperCooperativeTest`, `StormcraftIncorporatedTest`, `BoomTownTest`, and
  `StandardTechnologyTest`

Preserve observable card/action outcomes, discounts, values, source restrictions, and
payment completion before consequences. Do not create mirrored allocation state or card-specific client
orchestration to address a local symptom.
