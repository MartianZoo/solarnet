# Pets specification fidelity audit

> **Read when:** examining agreement among the Pets specifications, tests, KDoc/API, and implementation.
>
> **Status:** unfinished audit. The leads below may have been resolved or overtaken by later changes.

## Aim and boundaries

The [language specification](../pets-language-spec.md) and
[type-system specification](../type-system-spec.md) define concepts independently of Kotlin. KDoc
explains the API in those terms and cites the relevant rules. Every behavioral claim needs an
appropriate test, and every test's expectations need a specified basis. Existing tests can protect
accidents; neither a passing suite nor the current implementation establishes the intended contract.

Pets expressions are themselves specifications. Pets captures information for state and engine to
consume. Describe what a construct specifies they are supposed to do, without promising their
execution machinery. Pets tests should demonstrate information preservation using minimal stubs;
actual execution witnesses can remain in state and engine. Avoid reproducing those consumers just
to test Pets, and keep this audit's attention on Pets.

API-only contracts may live solely in KDoc. Test organization need not enforce an elaborate
separation: a test belongs with a spec section when it honestly witnesses that section, and elsewhere
when it is specific to the Kotlin API. Prefer simplified `tfm-canon` examples with irrelevant details
removed. Other documentation is downstream and outside this audit unless separately requested.

## How to work

Use current source and meaningful assertions to establish each discrepancy. The purpose is fidelity,
with a strong bias against growing complexity. Simplification is useful when it is a reasonably
expedient repair, rather than a separate cleanup objective. Be reluctant to offer extra behaviors or
invariants merely because the implementation happens to support them.

Implementation complexity should have a reason demonstrated by tests. For suspect machinery, try a
bounded removal experiment and run the full suite. If something breaks, consider whether there is a
small sensible adjustment. If the behavior earns its place, specify and test it. Coverage can suggest
where to look, but executing code is weaker evidence than testing why it is needed.

Do not chase a discrepancy into disproportionate repairs or expanding ripple effects. A passing
characterization in [Pets BugsTest](../../test/common/dev/martianzoo/pets/BugsTest.kt), with the
remaining question recorded, is a useful outcome. Existing bugs need not all be fixed; discovering
more can be progress. Where the evidence leaves a choice, favor the coherent interpretation that
adds the least complexity.

For caches and optimized paths, check that bypassing them preserves answers, then retain the
optimizations with `// TODO: benchmark evidence needed.` where measurements are missing.

Deliver improvements incrementally. Each solid repair should bring the affected spec, tests, KDoc,
and implementation closer together. Ask Claude Opus/high for one review round, consider its feedback
without expanding scope, then commit and continue. Follow the current session's instructions for any
reviewer substitution. Avoid accumulating an unreviewed batch when review is unavailable. Choose
commit granularity to make each improvement coherent and reviewable.

Use the repository's current [testing guidance](TESTING.md) and
[review criteria](REVIEW_CRITERIA.md). Periodically check whether the working branch is behind `main`
and merge it when needed. Reassess assumptions as the code evolves.

## Leads worth revisiting

These are places the audit found friction, not a prescribed sequence or a claim that the problem
still exists. Establish the present behavior and decide what, if anything, deserves a change.

### If time is limited

Prioritize consequences and reach. The strongest leads from this pass were:

- **Highest expected value: preserving authored meaning.** Declaration order, round trips, and
  preservation of bindings and predicates deserve early attention. A silent change here can corrupt
  the specification handed to every consumer while leaving apparently valid Pets nodes behind.
- **Also high value: core semantic decisions and their contracts.** Type meets, inherited-default
  precedence, property evaluation, and partial binding determine which specifications are admitted
  and what information they retain. Distinguishing an implementation defect from an inaccurate spec
  claim is worthwhile even when the eventual repair is small.
- **Usually lower priority: isolated API promises and machinery cleanup.** Incidental ordering,
  redundant guards, and unused paths can wait when they do not affect meaning or obscure a core
  contract. Reconsider that ranking if current callers or tests reveal wider consequences.

These priorities concern what to investigate, not how much complexity to spend fixing it. A useful
characterization of a consequential discrepancy can be more valuable than several minor cleanups;
the bounded-repair rule still applies. Re-rank these leads using current evidence.

### Questions to explore

- **Declaration order and round trips.** Can rendering, equality, or normalization change the
  meaning of inherited dependency positions? Which ordering distinctions are semantic, and which
  are incidental? Direct supertypes now retain authored order in declaration equality and rendering;
  `Spec03DependenciesTest` checks argument binding after both source-rendering forms, and
  `ClassDeclarationTest` rejects conflicting declarations with reversed supertypes.
- **Property evaluation with free lexical `Me`.** `PetElaborator.propertyEvaluator` retains
  evaluations whose raw property syntax contains free `Me`, even before applying an evaluation's
  captured owner. The direct class-effect path needs a focused witness or bounded removal
  experiment; the `EACH` and `RANK` cases retain their bodies before reaching this guard and do not
  establish its necessity.
- **Type meets and inherited defaults.** Dependency constraints may resolve an apparently ambiguous
  nominal intersection. An incompatible nearer default raises questions about precedence and whether
  an overridden ancestor can reappear. Earlier repair attempts here encountered wider design pressure.
- **Information hidden by syntax.** Compare marker roles, represented-class predicates, and binding
  identity through copying, rendering, and re-resolution. Equal-looking expressions and equivalent
  types are not automatically the same contract.
- **Unnecessary promises.** Look for incidental enumeration order, allocation identity, parser
  restrictions, normalization guarantees, or API behavior presented as a language rule. Check that
  tests distinguish the intended behavior rather than merely round-trip a fixture or reject it for
  an unrelated reason.
- **Spec provisions without a canonical TfM witness.** The specs separate game examples from
  authoring conventions. Revisit removal-default opt-out (L9-6), header-variable default deferral
  (L9-10), competing nearest defaults and incompatible overrides (T10-4), the explicit `Anyone`
  owner-default case (T10-5), and inline local-header binding/body generality (L12-3, L12-4).
  Other unproven subcases include Signal transmutations (L2-1), all transmutation quantifier pairs
  (L9-8), and merging two separately refined operands (T8-9). Nearby examples demonstrate simpler
  capabilities, not these extra cases. Establish a real need or try bounded removal before
  strengthening promises.
- **Complexity without a witness.** Earlier questions included scope bookkeeping, transform passes,
  fallback paths, and constructor checks. Reinspect their current callers and interactions before
  deciding whether removal, a better witness, or a documented discrepancy is appropriate.

Historical notes and experiments may be useful in
[`_local/pets-audit-2026-10-04-work4/`](../../_local/pets-audit-2026-10-04-work4/).
They contain provisional and superseded ideas; the audit does not depend on their availability.
