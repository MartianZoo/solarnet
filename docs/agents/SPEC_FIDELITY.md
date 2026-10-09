# API specifications, KDoc, and fidelity

> **Agent information:** This is an agent-maintained note recording documentation standards and
> user decisions.
>
> **Read when:** writing or reviewing API specifications or KDoc, or examining agreement among
> specifications, tests, and implementation.
>
> **Status:** current standards for nine priority modules. Restored from the specification-fidelity
> guidance deleted in `e3fa40d08`, with the user's 2026-10-09 decisions incorporated. Historical audit
> leads and task-specific operating instructions are not current standards.

## Priority scope

The public APIs of these nine modules are the priority for clear specifications. This scope does
not imply that their documentation or conformance audits are complete.

| Module | Contracts to explain |
| --- | --- |
| `pets` | A far higher specification standard: maintained semi-formal language and type-system specifications, plus API KDoc for AST construction, parsing, elaboration, and type queries. See below. |
| `catalog` | Catalog composition, configuration, content selection and exclusion, dependency closure, validation, and resolved game premises. |
| `state` | World observations, components, actors, tasks, events, recordings, and the lifetime and mutability of returned views. |
| `engine` | World construction, execution, transactions, effects, rollback, checkpoints, and forks. |
| `agent` | Actor-scoped queries and mutations, task selection and narrowing, completion, delegated control, forms, and automatic execution policy. |
| `tfm-card-data` | Authored card fields, absent values and defaults, validation, derived card categories, and bundle lookup. |
| `tfm-canon` | Canonical vocabulary and content, bundle composition, Terraforming Mars configuration policies, and map definitions. |
| `tfm-state` | Read-only Terraforming Mars queries, including resource names, printed production, and map lookup. |
| `tfm-engine` | Terraforming Mars game construction, gameplay calls, payments, and caller responsibilities for phase progression. |

## Audience and authority

Assume readers already understand Pets and Solarnet. Explain the particular API precisely without
adding introductory tutorials. Link to prerequisite concepts and relevant specification rules where
that helps explain the contract.

Specifications state what the system should promise. When the intended contract is clear, promise
that behavior even if the implementation does not yet comply; treat the discrepancy as a bug. The
specification and KDoc do not have to mention the defect. Track it separately in `TODO.md`, an
existing issue, or a meaningful bug characterization, without weakening the contract to describe
an accident. Do not claim that writing the promise proves the implementation satisfies it.

When intent is genuinely unclear, investigate existing specifications, source, tests, and caller
needs, then ask the user if they do not settle it. Do not turn ambiguity into a confident promise.
Record general answers in this document and API-specific answers in the owning contract. Prefer the
coherent interpretation that adds the least conceptual complexity; do not promise extra behavior
merely because an implementation happens to support it.

## Pets has a far higher specification standard

We hold `pets` specifically to a far higher standard than the other eight modules. Maintain
semi-formal [`pets-language-spec.md`](../pets-language-spec.md) and
[`type-system-spec.md`](../type-system-spec.md) as the authoritative definitions of its language
and type-system semantics, independent of Kotlin. These are actively maintained specifications,
not background reading or optional supplements to KDoc.

State semantic rules precisely using the specifications' established terminology, numbered rules,
and cross-references. When language or type-system semantics are added, changed, or clarified,
update the owning `-spec.md` document and keep API KDoc and conformance tests aligned with it.
KDoc explains the Kotlin API in those terms and cites the relevant rules; it must not become the
sole home of a language or type-system rule. Kotlin API-only contracts may still live solely in
KDoc. Clearly intended rules remain normative when implementation defects are tracked separately,
as described above.

The other eight modules require excellent public API contracts and discoverable explanations of
shared rules, but do not automatically require comparable semi-formal `-spec.md` documents. Use
KDoc and add separate module documentation when the subject warrants it.

Pets expressions are themselves specifications: describe the information they preserve and what
state and engine are supposed to do with it, without prescribing those consumers' execution
machinery. Each module's contract should explain the responsibility it actually owns.

## Public API coverage and KDoc

Every effectively public declaration in these modules deserves a clear API contract, including
constructors, properties, overloads, and declarations used only by other repository modules. Do not
dismiss a declaration as an implementation detail merely because it is not a principal entry point.
If its visibility appears inappropriate, raise that as a separate design question; it does not
excuse missing documentation or authorize a visibility change during documentation work. Protected
members intended for subclass authors also need their applicable contract explained.

Class KDoc is the starting point for understanding the class's contract. Explain shared rules there,
or link directly from it to their common explanation in another class's KDoc or an owning
specification section. A reader must be able to find applicable rules without inspecting
implementation code or guessing which agent note to read. Keep a shared rule's full explanation in
a single authoritative location; member KDoc explains its application and any differences.

Write enough that a caller can predict the observable result. Where applicable, explain valid
inputs and context, returned values and their lifetime, state changes, pending work, completion,
failure conditions, and what remains changed after failure. State meaningful ordering, ownership,
and callback guarantees where the API depends on them. For data APIs, explain field meaning,
defaults, absence, and invariants. Use concrete examples for distinctions that signatures alone
cannot convey; avoid boilerplate that merely repeats names and types.

## Evidence and conformance

Every behavioral claim needs an appropriate test, and every test's expectations need a specified
basis. A passing suite or current implementation does not establish the intended contract: existing
tests can preserve accidents. During documentation work, identify missing evidence and known
mismatches without expanding the task into an implementation repair.

Tests should demonstrate meaning and observable results. A test belongs with a specification section
when it witnesses that section, and elsewhere when it is specific to the Kotlin API; do not impose
an elaborate parallel test hierarchy. Prefer simplified canonical Terraforming Mars examples with
irrelevant details removed. Pets tests can demonstrate information preservation using minimal
stubs, while state and engine tests witness execution. Do not reproduce consumers merely to test
Pets. Coverage can suggest where to look, but exercising code is weaker evidence than showing why
its behavior is required.

For an authorized fidelity audit or repair, establish discrepancies against current source and
meaningful assertions. Keep repairs bounded: do not chase a discrepancy into disproportionate
machinery or unrelated cleanup. A useful bug characterization and a tracked follow-up can be the
right result. Tests that characterize a defect must be clearly identified as such, so they do not
become a competing statement of intended behavior. Preserve good existing documentation, especially
the Pets specifications, and improve the smallest owning explanation.

Follow the current [testing guidance](TESTING.md) when editing or running tests and the
[review criteria](REVIEW_CRITERIA.md) for the complete final diff. Documentation work alone does not
authorize changes to behavior, API visibility, or commits.
