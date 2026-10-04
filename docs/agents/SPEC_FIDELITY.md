# Pets specification fidelity audit

> **Read when:** auditing or repairing agreement among the Pets language specification, type-system
> specification, conformance tests, Kotlin KDoc/API, and implementation.
>
> **Status:** audit procedure and unfinished-work handoff, updated 2026-10-04. The audit is not
> complete. The latest audit repair is `1d96e7a55` on `work4`; the handoff also includes the later
> main merge `54e4b8c5f`.

## What success means

The objective is fidelity across four surfaces that must agree:

| Surface | Responsibility | Evidence |
| --- | --- | --- |
| [Language spec](../pets-language-spec.md) and [type spec](../type-system-spec.md) | Define syntax, terms, mathematical concepts, and specified meaning without knowing Kotlin's APIs | Meaningful `Lang*Test` and `types/Spec*Test` witnesses |
| Kotlin KDoc | Explain an API using those terms and cite the owning spec rules; state any additional API-only contract honestly | Appropriate Pets API or conformance tests |
| Tests | Demonstrate the stated behavior and distinguish tempting incorrect implementations | Observable assertions, including negative cases where they matter |
| Implementation | Supply the specified behavior with justified complexity | Tests that exercise and establish the purpose of the behavior, not merely execute its lines |

No behavior described in either spec or KDoc should lack an appropriate witness. Conversely, a test
must not silently turn an unspecified incidental behavior into a requirement. A passing test suite
does not by itself establish agreement, and an old test may protect an accident rather than the
user's intent. Inspect assertion strength and isolate the condition a rejection test really rejects.

Use simplified examples from `tfm-canon` wherever useful. Preserve the relevant rule and remove
irrelevant game details; label adaptations rather than presenting synthetic examples as exact canon.
Do not initiate external game-rule research merely because an example mentions a card.

Other documentation is downstream of this audit. Leave it aside apart from this handoff, the
smallest navigation updates, and required tracking. Do not spend the audit rewriting tutorials or
engine design documents.

## The language describes specifications

A Pets expression is itself a specification. Pets captures information that state and engine must
be able to consume; the language spec describes what the construct specifies they are supposed to
do. Prefer that wording over promises about how a consumer executes, schedules, queries, or stores
it. Neither mathematical specification should depend on Kotlin classes, methods, nullability,
exception types, collection implementations, or allocation identity.

Pets tests have no dependency on state or engine. Keep that boundary. Their query stubs may supply
observations, but must not grow counterfeit evaluators, fanout engines, state stores, or ranking
algorithms. Pets witnesses should prove that constraints, bindings, syntax, and other necessary
information survive. Existing state/engine tests may establish actual execution obligations; the
user explicitly accepted those witnesses living outside Pets and did not ask for a runtime audit.

An API-only contract can live solely in KDoc. There is no required new API-conformance hierarchy:
keep a test under a spec section when it honestly witnesses that section; otherwise put it in an
appropriate API test class. Machine integer limits, traversal APIs, and factory behavior are examples
of API concerns, not mathematical language laws.

## The repair loop

1. Choose one discrepancy. Read the owning spec rule, KDoc, implementation, and relevant tests.
   State what disagrees and what assertion would distinguish the correct behavior.
2. Prefer a small witness that fails for the actual mismatch. Do not accept a parser error as proof
   of a type error, a round trip as proof of binding, or an assertion that repeats its fixture.
3. Make the smallest coherent repair across the affected surfaces. Neither implementation nor old
   tests automatically outrank the intended concepts. Where intent is underdetermined, choose the
   coherent interpretation that strongly avoids growing complexity.
4. Try a bounded simplification when it is an expedient fidelity repair. Simplification is an
   option, not the project's goal. Remove the suspect machinery experimentally, run the full suite,
   and inspect failures briefly. Fix a small sensible consequence; do not pursue expanding ripple
   effects. When necessary behavior survives that examination, specify it and give it a meaningful
   witness instead of treating it as an unexplained exception.
5. If a discrepancy repair becomes unexpectedly difficult, add a passing characterization to
   [Pets BugsTest](../../test/common/dev/martianzoo/pets/BugsTest.kt), record the intended rule and
   deferred decision, and move on. Existing BugsTest defects are not mandatory repairs for this
   project. Its growth can be useful evidence of discrepancies discovered.
6. Validate the bounded final change and obtain one independent review. The requested reviewer is
   local Claude, model Opus, high effort. Evaluate feedback critically without expanding scope;
   when disagreeing with Claude, follow the global instruction to use one additional turn to seek
   agreement. Apply [REVIEW_CRITERIA.md](REVIEW_CRITERIA.md) to the complete final diff.
7. Commit that repair, then select the next one. The user explicitly authorized incremental audit
   commits. Keep the working tree to one reviewable issue; a related implementation/API migration
   may legitimately touch several files. Do not accumulate a large unreviewed batch while waiting
   for a reviewer.

On 2026-10-04, Opus hit a session limit reporting a reset at 12:20 p.m. Pacific. The user authorized
independent agent reviews until around noon. This is temporary fallback permission, not a permanent
replacement for the requested reviewer. Check availability on resumption; a preparer should not be
the sole reviewer of their own proposal. Do not restart implementation merely because the quota
resets: the latest user instruction was to stop at this handoff.

Optimizations have a specific exception to removal: validate that bypassing caches/fast paths
preserves answers, then **retain them**, adding `// TODO: benchmark evidence needed.` where needed.
Do not turn allocation reuse, cache identity, or incidental iteration order into public guarantees.
Class identity within a universe and Type allocation identity are different; preserve meaningful
identity contracts and discard only accidental ones.

Periodically compare the current branch with local `main` and merge when behind, as requested by
the user. Preserve the current repair safely first if necessary. Revalidate changes whose owners
changed during that merge; do not blindly reapply an old snapshot. Do not inspect or alter sibling
working copies, and do not treat this as permission to merge other branch tips, advance worktrees,
or push.

## Verification and record keeping

Follow [TESTING.md](TESTING.md). Run focused tests first. `./gradlew :pets:jvmTest` checks the Pets
suite; `./gradlew test` runs repository JVM suites and the configured browser checks. Formatting
tasks are root tasks: `./gradlew spotlessKotlinApply` / `spotlessKotlinCheck`. Do not launch concurrent
Gradle builds from this working copy. Broaden tests when a change or unresolved concern warrants it;
do not repeat an unchanged passing suite mechanically.

Use named tests for distinct meaningful cases. A Kotlin API check can supplement a spec witness,
but tests should not just mirror implementation branches. Coverage reports can point to questions;
line hits cannot prove that complexity is necessary. Distinguish source inspection, an unrun
proposal, a failed experiment, a focused test pass, and a full-suite pass in the handoff.

Store temporary experiments under `$TMPDIR`. Preserve human-useful local patches and evidence in
`_local/`. Update this document as issues are resolved; keep [TODO.md](../../TODO.md) as a pointer
rather than duplicating the audit plan. Keep the acceptance checklist below visible through review.

## Committed checkpoint

Seven repair commits are complete. The first six received Opus/high review; the seventh received
independent agent review under the user's temporary authorization.

| Commit | Result |
| --- | --- |
| `83678c127` | Replaced the vacuous deferred-property witness with an Award/Landlord example |
| `fad18b246` | Framed specs as consumer obligations; removed fake union-count algorithms from tests |
| `81cdf3e95` | Removed incidental enumeration/allocation promises and public descendant-count machinery |
| `6e924739c` | Strengthened dependency equality, property-bound rejection, and cascading inhabitance witnesses |
| `72a14ad86` | Removed redundant compact-form validation and unused representation paths; added semantic witnesses |
| `8677cc776` | Strengthened dependency/default/cycle/class-literal/scope witnesses and clarified API boundaries |
| `1d96e7a55` | Removed the represented-class guard that prevented a class-literal meet from narrowing both operands |

The branch includes main's class-loading/construction changes and `529d7c94b` (via `37e8d7d82`),
which replaced contextual ownership with lexical `Me`. The complete suite passed after that merge
and the final meet repair: `./gradlew test`, 3m18s. The working tree was clean at that implementation
checkpoint. No further prepared repair has been applied.

During handoff, main advanced again: `54e4b8c5f` merges `5db20fd44`, including `50f38120c`
(component invariants at operation completion). This adds runtime-oriented spec notes, changes
`ClassLimitTable`'s required-limit caching, and changes `CardPetsGenerator`. These incoming changes
have not received this audit's fidelity review. Revisit their spec wording and optimization evidence
without expanding into an engine audit. The full suite passed on this final integrated checkpoint:
`./gradlew test`, 4m58s. Documentation links and `git diff --check` also passed; the handoff received
independent agent review. These checks do not complete the fidelity audit.

An earlier, larger prepared snapshot also passed the full suite, but was deliberately taken out of
the working tree to restore incremental review. That pass predates lexical `Me` and does **not**
validate reapplying it to the current code.

## Saved material and what it does not prove

Local continuation material is in
[`_local/pets-audit-2026-10-04-work4/`](../../_local/pets-audit-2026-10-04-work4/).
It is optional supporting evidence, not tracked repository content; the next steps below must remain
usable without it.

- `NOTES.txt` explains provenance. `committed-checkpoint-validation.log` is the post-Me full-suite
  result. `final-integration-validation.log` covers the final main merge.
  `full-suite-validation.log` belongs to the older aggregate snapshot.
- `all-prepared-repairs.patch` and `02-remaining-prepared-repairs.patch` are historical recovery
  artifacts against `f8d099ba2`, not proposed commits. **Do not apply the aggregate.**
- `next/` contains three individually adapted patches described below. They were apply-checked
  against the Me tree but have not been tested or reviewed there. After the final main merge, the
  first patch needs its `CardPetsGenerator` hunk rebased; the other two still apply cleanly.
- `proposals/` preserves scratch patches and inventories, including committed, superseded, and
  unrun ideas. Their existence is not a recommendation to apply them. Older coverage data and
  inventories predate several repairs and the Me migration.

In particular, discard `represented-class-predicate-meaning.patch`: Opus found its proposed guard
witness vacuous. The committed meet witness is the replacement. The dependency-aware candidate-search
experiment was also deliberately reverted; a passing Pets-only run is not approval to restore it.

Drop the old EVAL patch in favor of its adapted successor. Drop the old
`owner-context-information-preservation.patch`: automatic contextual Owner behavior in `EACH` and
`RANK` is obsolete. Do not resurrect the old `Defaults` contextual-Owner exception or obsolete Each
KDoc from a combined patch. Main also renamed the inline-class test files.

## Next repairs, in priority order

### 1. Preserve authored supertype order

`ClassDeclaration.supertypes` is a Set even though T3-2 makes inherited dependency-key order
meaningful. Its renderer and `CardPetsGenerator` sort supertypes, so rendering/reparsing can change
where a positional argument binds. Declaration equality can also overlook this semantic difference.

Use `Adjacency : Occupant, Neighbor`, with both parents depending on Area: after rendering and
reloading, `Adjacency<Tharsis_2_2>` must still specialize Occupant's key, leaving Neighbor's key at
Area. Contributions with reversed parent order must conflict instead of comparing equal.

`next/01-supertype-source-order.patch` changes the representation to List, preserves rendering
order, adjusts construction sites, and adds those two witnesses. It deliberately retains existing
duplicate-supertype rejection; idempotence is a separate experiment. The old combined repair passed
before Me; this narrower adapted patch still needs its generator hunk rebased after `50f38120c`,
focused tests, full-suite verification, and review.

### 2. Correct property-evaluation deferral contracts

L9-1/L9-12 and `evaluateProperties` KDoc still overstate receiver concreteness. Fixed syntax may
expand on an abstract receiver. Partial class-effect elaboration defers a property that is only a
bound, syntax awaiting a concrete `This`, or syntax awaiting lexical `Me`. Direct expansion rejects
a bound-only property; selection bodies/keys preserve their evaluation context.

`next/02-eval-deferral.patch` updates spec/KDoc and adds simplified Award/Ants witnesses plus API
tests. It incorporates the new Me behavior. `engine/DeferredPropertyBindingTest` already owns live
trigger/fanout/rank realization; do not reproduce that engine in Pets tests. Test and review the
adapted patch before adopting its exact wording.

### 3. Replace the stale two-stage-specialization limitation

T13 still claims that an abstract header specialization cannot subsequently capture a narrower
event type. A prepared witness specializes a ResourceWatcher header from Resource to
StandardResource, then captures Steel from the trigger and substitutes Steel into the reward.
That witness passed before Me and requires no ownership syntax changes.

`next/03-header-specialization.patch` places the conceptual bound/identity rule under T13-5 and
removes the stale limitation. Run it on the current tree, then review and commit. No implementation
change is proposed.

### 4. Preserve bounded-discrepancy findings

These observations are not all present in the committed BugsTest. Reproduce them on current code;
retain the intended rule and record a passing characterization when repair pressure persists.

- **Dependency-aware meets (T7-1):** `CardFront` intersected with
  `ResourceHolder<Class<Animal>>` should select Pets when the only rival card, Ants, holds Microbe.
  The root-first meet returned no result. A small candidate-search experiment passed Pets tests but
  exposed unresolved handling of conflicting header equalities. It was reverted rather than grown
  into a general feasibility search. The saved characterization and conceptual T7 prose need review.
- **Inherited defaults (T10-4):** dropping an incompatible nearer default before choosing its
  supplier can revive an overridden ancestor default. Strict rejection broke canon loading:
  Mohole Area's water-only tile inherits a land-area default. That repair was reverted. Record the
  precedence discrepancy and leave the broader applicability decision explicit; do not change game
  declarations merely to make the strict experiment pass.
- **Possible represented-class textual round trip:** a meet may retain its hidden represented-class
  predicate in `Type.expression` while rendering/reparsing loses it. A proposed BugsTest compares
  narrowing from the retained expression and parsed text. This probe is **unrun**, not a confirmed
  defect. Try the smallest repair only after demonstrating it.

### 5. Recover the remaining fidelity witnesses as small repairs

The historical snapshot contains useful work, not a ready-to-apply batch. Rebase and review these
groups separately, discarding superseded assumptions:

- Declaration docstring placement, DEFAULT refinement boundaries, authored-versus-executable
  rendering, and duplicate-supertype/invariant idempotence experiments.
- Marker equality: equal `@StandardResource` occurrences in `PROD[@StandardResource] -> 4
  @StandardResource` differed by internal supplying/reference role. A small equality correction
  passed the older full suite; it still needs current validation and review.
- Correct `Ok` narrowing prose/witnesses, compact gained-only refinement, nested HAS conjunctions,
  grouped metric caps, first-stage shared-X ambiguity, and fixed BY/EACH wrapper intersections.
- KDoc/API witnesses for traversal, atomic replacement, Requirement.join, integer bounds, zero
  instruction scaling, direct construction, and round-trip limits. Correct canon examples without
  restoring old Owner semantics.
- Type witnesses for optional-to-required properties, inherited equality propagation, trigger-only
  actor-variable rejection, compact rendering after a failed intermediate omission, and preservation
  of nested predicates while binding dependencies.
- Remove accidental independent-variable enumeration-order promises. Do not replace the old false
  spec attribution with a new KDoc ordering guarantee. Preserve meaningful occurrence positions.
- Four unrun language witnesses remain in `pets-final-language-observation-witnesses.patch`: nested
  compact projections, IF forbidden inside a trigger transform, and supplied property/rank metric
  observations. They require no runtime simulator.

### 6. Finish bounded implementation experiments

The following are proposals, not accepted deletions. Check current callers after the Me merge and
try each independently: redundant defaulting checks; header Seed flag/usage copying; duplicate
observing-supplier validation; refined-literal exclusion in the THEN collector; unused Then factory
and empty-input behavior; broad ClassLoader resolution catch; transform rewrapping and recursive
dispatch of handler outputs; ClassSelection's candidate self-subtraction.

Important constraints discovered during preparation:

- Parser catch removal first needs `0X` validation moved into located parsing, with an authored
  offset witness. Simply deleting the outer catch leaks a programmer exception.
- `variableDeclaredAt`'s structural-equality fallback may be needed because syntax and scopes are
  copied independently. If the identity-only experiment breaks real BY behavior, retain the fallback
  and witness copy preservation; do not redesign binding to rescue the deletion.
- Transform-wrapper removal and handler-output recursion interact. Removing recursion changes the
  earlier argument that generated same-kind nesting is rejected. Reassess them separately.
- Keep TfmCatalog's separate module-default self-exclusion; the ClassSelection proposal does not
  establish that this other subtraction is redundant.
- Keep the inhabitance fixed-point iteration: an earlier single-pass mutation failed the new cascade
  witness. Keep compact rendering's failed-trial fallback and nested replacement's universe/refinement
  handling unless meaningful counterevidence appears.

Cache bypassing previously passed the Pets suite plus focused real engine dependency/selector
tests. An all-bypasses full-suite run timed out after ten minutes and was restored; it established no
full-suite equivalence. The saved optimization comments are not committed. Preserve the caches,
revalidate affected paths after Me, and record exactly what passed. Do not claim benchmark evidence.

## Completion checklist

- [ ] Every normative language/type rule and substantive example has an appropriate meaningful
  witness, or a clearly recorded discrepancy; stale promises have been removed.
- [ ] KDoc describes current API behavior and cites concepts in the specs; API-only tests are
  located honestly and do not create unnecessary guarantees.
- [ ] Implementation complexity has a test-backed reason or a documented bounded disposition;
  optimizations are retained with honest evidence limits.
- [ ] Test doubles capture Pets information and supply observations without duplicating consumers.
- [ ] Known defects are distinguished from conformance, and deferred work is explicit.
- [ ] Refreshed coverage and a final spec/KDoc/test sweep account for the Me migration and all
  accepted repairs; old inventories are not mistaken for proof of completion.
- [ ] Each repair has been reviewed, appropriately validated, and committed; the final tree is clean.
