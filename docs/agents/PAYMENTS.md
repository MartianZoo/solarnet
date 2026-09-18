# Payment simplification plan

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.
>
> **Read when:** changing card or action billing, payment choices, tender value, excess-payment
> legality, or the player-facing payment APIs.
>
> **Skip when:** changing a direct non-payment action cost; use [ACTIONS.md](ACTIONS.md).
>
> **Status:** investigation-first simplification plan. Freeze new payment capabilities until the
> decision gates below are settled.

## Objective

Payment should be one intelligible operation:

1. establish debt and apply every cost adjustment;
2. let the payer choose one legal complete tender; and
3. continue the card play or action only after settlement.

The common exact-M€ case must remain visibly common. Steel, Titanium, card-held resources, and
discounts must not turn every payment into a distributed protocol that a client reconstructs from
tasks and causes.

Success means fewer permanent concepts and less event and task churn, not merely shorter Kotlin.
Do not add an aggregate-payment representation alongside the current protocol.

## Established findings

### Current complexity is not all forced

[Terraforming Mars `payment.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/payment.pets)
currently distributes payment across `Owed`, `Billing`, `ActionBilling`, `CardBilling`,
`Accepting`, `AcceptingFromCard`, `Pay`, `PayFromCard`, and `ResourceValue`.
[`TfmGameplay.pay`](../../src/common/dev/martianzoo/tfm/engine/TfmGameplay.kt) changes autoexecution
policy and searches the task pool for billing, offers, and cause-related follow-up work.
[`TfmPayCommand`](../../src/common/dev/martianzoo/tfm/script/commands/TfmPayCommand.kt) submits each
tender kind separately and declines unused offers.

That client choreography is not a game rule. Cash-only payment still creates and removes `Owed`,
`Accepting`, `Billing`, and `Pay`; a zero-cost card still creates and removes `CardBilling`. These
are primary simplification probes.

### The older model was smaller but not sufficient

The earlier `Owed`/`Accept`/`Pay` model proves that today's identity hierarchy is not inherent. It
still created parallel optional offers, required cause-based cleanup, exposed payment before all
adjustments necessarily settled, concealed excess through saturating debt removal, and kept
card-held tender outside one checked allocation. Recover its conceptual scale, not its defects.

### A whole tender is not one current task

One task cannot currently bind independently chosen quantities of M€, Steel, Titanium, and
card-held resources. Instruction groups split into separate tasks, and `THEN` remains one task only
to preserve shared `X` or an abstract Type variable. One engine task for a complete tender therefore
requires a new representation or task capability.

That capability is necessary only if raw engine tasks must themselves accept and validate complete
tenders. Current player-facing clients already receive several ordinary tender kinds in one call
and execute their separate task choices inside one atomic transaction. They do not represent every
card-held source inside that same checked allocation, so a trusted-boundary design must extend the
client tender without creating a second game model. The enforcement boundary is an open product and
ownership decision, not a technical conclusion.

### The real requirements are narrower than the current model

| Requirement | What it establishes | What it does not establish |
| --- | --- | --- |
| Finalized debt | Discounts and surcharges need one amount and denomination to modify. | The current billing identity hierarchy. |
| Post-adjustment choice boundary | Tender cannot be committed before automatic adjustments finish. | A persistent billing component. |
| Complete-tender validation | Excess legality depends on every selected indivisible unit. | A one-unit task loop. |
| Queryable effective value | Boom Town and value-increasing cards make value dynamic data. | One component per M€ or attribution in execution state. |
| Contextual modifier hook | Rules need card Class, action family, denomination, or settlement timing. | `CardPlay`, `ActionSlot`, or meaningless provider/slot pairs. |
| Completion before consequences | Cards and action results must wait for successful settlement. | Billing removal as the only possible latch. |

The current working excess rule for debt `D`, selected unit values `v1 ... vn`, and total `P` is:

```text
P >= D and P - D < vi for every selected unit i
```

It permits unavoidable rounding excess but rejects a tender from which one unit can be returned.
The authoritative rule remains unverified.

## Constraints on the investigation

- Do not implement the proposed one-unit loop in [ACTIONS.md](ACTIONS.md). It multiplies task cycles
  for cash, cannot validate the complete tender by itself, and still needs completion machinery.
- Defer Helion, composable conversion chains, and allocation attribution.
- Do not assume `Billing`, `ActionBilling`, `CardBilling`, `CardPlay`, `ActionSlot`, `Accepting`,
  `PayFromCard`, or per-M€ `ResourceValue` components survive.
- Do not build a generic engine feature unless a prototype shows a clear game-independent contract
  and substantial net deletion.
- Preserve the known Space Elevator defect as a visible `BugsTest` until the rule and enforcement
  boundary are selected.

## Investigations before selecting a design

### 1. Inventory live requirements

Classify every production listener of `Billing`, `ActionBilling`, `CardBilling`, `Accepting`,
`Pay`, `PayFromCard`, and `ResourceValue` as one of:

- pre-payment debt adjustment;
- accepted tender source;
- tender value;
- post-settlement reward;
- continuation or cleanup; or
- reporting only.

For each distinct rule shape, record its minimum context and whether an existing card-play or action
event can own it. Listener count alone does not justify today's identity shape.

### 2. Establish concurrency and ownership needs

Use minimal scenarios to answer:

- Can one payer legally have two live obligations at once?
- Can payment create required work assigned to another Actor?
- Must two actions from one provider remain distinguishable while both obligations are live?
- Is payer plus denomination enough for any useful subset of the corpus?

Do not retain identity for hypothetical concurrency.

### 3. Verify legality evidence

Confirm the excess-payment rule from authoritative evidence before building engine enforcement
around it. Inventory reconstructed payments with unavoidable excess so exact payment is not adopted
as a false simplification.

### 4. Measure representative operations

Record event and task counts, transient components, and client calls for:

- exact-M€ card payment;
- one discount;
- mixed M€/metal with a value modifier;
- one card-held source such as Psychrophiles;
- the illegal Space Elevator tender;
- a non-M€ obligation such as Trade; and
- a cross-Actor payment consequence if one exists.

Use the measurements as design diagnostics, not brittle production assertions.

### 5. Prototype four bounded alternatives

Each prototype covers only exact cash, one discount, one mixed tender, and one card-held source.
Do not migrate the corpus during comparison.

**A. Trusted whole-tender settlement.** Keep complete-tender submission at the player-facing
boundaries, but replace per-method optional tasks and cleanup with the smallest domain-owned
settlement operation. Determine whether Pets can continue to own accepted sources and values while
the client only chooses them. Include a card-held source in the submitted allocation without
mirroring holders or values in a client model. Record the raw-task legality gap explicitly.

**B. One whole-tender engine task.** Add the smallest way for one task to bind several independent
quantities and validate them together. Decide whether this is honest generic task semantics or a
Terraforming Mars instruction. Reject it unless the resulting deletions decisively outweigh the new
cross-module concept.

**C. Sequential tender choices.** Select one source at a time, in batched quantities rather than
necessarily one physical unit. Determine how the complete allocation accumulates, how the player
finishes or corrects it, how excess is validated, and what resumes the enclosing operation. Reject
any version that creates a parallel ledger or leaves cash comparably noisy.

**D. Owner-scoped escrow.** Transfer selected resources temporarily to an
`Escrow<Payer, Obligation>` Owner, then validate and consume the collected tender or return it on
cancellation. This may let existing single-resource choices assemble a complete allocation without
a multi-quantity task.

Escrow must answer:

- Can standard resources be owned by Escrow without firing Player resource rules against the wrong
  owner?
- Can card-held resources move there without breaking the rule that their Owner follows their
  holder, or would escrow require duplicate tender tokens?
- Do deposits fire loss, gain, conversion, or payment effects before acceptance?
- Can invalid completion restore resources and consequences when deposits came from separately
  committed tasks?
- Can escrow retain payer, obligation, source, and effective value without becoming a second
  ledger?

Reject escrow if standard and card-held resources need different shadow forms, if returning a
deposit differs observably from never spending it, or if its lifecycle rivals the present protocol.

Compare all four alternatives by permanent concepts, affected modules, event/task counts, client
knowledge, raw-task legality, and card-specific rules.

### 6. Minimize identity independently

Prototype the smallest event or interval satisfying the listener inventory. Test a late card-play
event, concrete action identity, payer plus denomination, and scoped operation. Do not wait for the
allocation design to remove identity no rule needs.

## Decision and implementation sequence

1. Complete the listener, concurrency, legality, and measurement investigations.
2. Settle the enforcement boundary with the project owner.
3. Run the four small prototypes and review deletion as carefully as addition.
4. Select one representation; retain no compatibility path or parallel model.
5. Implement one vertical slice covering exact cash, a discount, mixed tender, card-held tender,
   and one post-settlement rule.
6. Apply the complexity budget before migrating remaining cards and replays.
7. Delete obsolete billing Classes, offer cleanup, cause scans, and client policy manipulation as
   part of the same program.
8. Reconsider attribution and unusual conversions only after execution is simple and stable.

## Acceptance criteria

- One debt source of truth and one completion rule.
- One complete allocation decision, even if several removals execute internally.
- No unused offers or cleanup for exact cash.
- No player-facing search through `cause.context`, rendered instructions, or the whole task pool to
  discover payment scope.
- Adjustments finish before tender can commit.
- Complete-tender validation at the selected boundary.
- Ordinary and card-held resources follow one allocation rule.
- Effective value is queryable without speculative World mutation and rollback.
- No dependence on automatic-listener order.
- A net reduction in concepts and cross-module knowledge.

## Settled project constraints

- Simplifying the action and payment lifecycle is a current priority ahead of a broader Pets
  conformance program.
- Keep complete replays working throughout the redesign; this appears feasible and temporary replay
  breakage is not a useful default plan. This does not prevent a separate, deliberate decision to
  drop a card whose support imposes disproportionate permanent complexity.
- Do not complicate the payment model to achieve perfect causal attribution. Preserve enough event
  facts for current traceability and post-process the logs when a richer explanation is wanted.

## Open questions for the project owner

1. **Enforcement boundary:** Must raw engine tasks reject an illegal complete tender, or is legality
   at `TfmGameplay` and script boundaries sufficient for now?
2. **Core-engine budget:** Is a multi-quantity task capability acceptable only if a prototype shows
   substantial net deletion and a clean game-independent contract?
3. **Fidelity timing:** Should the excess-payment rule be researched before implementation, or
   should simplification provisionally preserve the current client rule?
4. **Common-case budget:** Should exact-M€ payment have no payment-option components, or is one
   explicit payment-window component acceptable if it materially simplifies modifiers?
