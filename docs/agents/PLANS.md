# Major plans

> **NOTE:** This is an agent-maintained index of substantial changes already selected, proposed, or
> approved elsewhere. It is not a promise to implement every item and does not replace the owning
> documents.
>
> **Read when:** choosing a substantial next project, comparing programs of work, or deciding where
> a newly discovered large change belongs.
>
> **Status:** prioritized portfolio summary. The owner documents linked below remain authoritative
> about current behavior, decisions, prerequisites, and acceptance criteria.

[`VALUES.md`](VALUES.md) says what outcomes matter. This page says which major changes could pursue
them. [`TODO.md`](../../TODO.md) is reserved for bounded miscellaneous work that does not belong to
one of these programs.

## Priority

The authoritative rationale is in
[`VALUES.md`](VALUES.md#current-major-plan-priority). The practical order is:

1. make actions, payments, and completion one intelligible lifecycle;
2. replace the Kotlin phase runner with self-running Pets scopes;
3. complete the Agent boundary and policy system;
4. rewrite the internal agent handbook around the settled model;
5. consolidate public contracts and failure boundaries; and
6. simplify the remaining Pets and runtime semantics.

These are Tier 1 programs. The bounded Class-universe finish is complete and no longer appears in
the portfolio. Tier 2 begins with material measured performance work. Expanded replay
evidence/provenance and deleting marginal-card machinery follow without an ordering between them.
Preserving existing replay evidence is a proof obligation for every Tier 1 change, not a competing
program.

## Selected programs

### Make actions, payments, and completion one intelligible lifecycle

Replace positional or reconstructed action identity, distributed payment offers, whole-World waits,
and client task-pool searches with the smallest lifecycle that has one action identity, one debt and
tender decision, an honest completion event, and positive permission state where permission is real
game state. The selected next step is a discardable concrete-action-Signal prototype; payment design
remains investigation-first, and scoped completion must prove that it can delete a real
`TfmGameplay` bridge before it grows. Replace the persistent `CardPlay` billing host only when the
live late-stage card-play operation has stable identity.

Before adding another gameplay helper, review `TfmGameplay` as one whole public contract: classify
its operations, query conveniences, test-only audits, and engine/Pets repair bridges; state the
boundary in its KDoc and owning documents; and map focused contract tests plus replay evidence to
the retained surface. Prefer deleting or relocating responsibilities to splitting the same
choreography among more facades.

This program also owns the broad task-disambiguation problem: ordinary callers should state the
intended task semantically, using extra identity only when distinct tasks accept the same narrowing.
Revisit the cleanup-vocabulary draft in stash commit `db9302652` only through the scoped-completion
rules, not as authority for restoring broad `Barrier` waits.

See [`ACTIONS.md`](ACTIONS.md#next-phase-constrained-prototype),
[`PAYMENTS.md`](PAYMENTS.md#decision-and-implementation-sequence), and
[`SEQUENCING.md`](SEQUENCING.md#the-missing-rule-when-an-operation-is-over).

### Replace the Kotlin phase runner with self-running Pets scopes

After one explicit start, Phase-owned scopes and ordinary effects should advance the game. Expansion
authors contribute local ordering constraints; a compiler emits the one runtime topology, and no
coroutine or mirrored Kotlin phase sequence remains. Continue through narrow demonstrations before
migrating the game. Moving remaining bootstrap work under ordinary phase-caused tasks belongs here.

See [`WORKFLOW.md`](WORKFLOW.md#first-demonstration) and the workflow findings in
[`RESPONSIBILITIES.md`](RESPONSIBILITIES.md#workflow-runner-mechanics-are-general).

### Complete the Agent boundary and policy system

Keep passive Game World data below a policy-free engine and one Actor-scoped Agent per Actor above
it. Narrow the ordinary Agent surface, provide the selected scoped reader, replace legacy policy
levels with attachable policies, and keep shared settlement at policy-relative stable points. More
powerful automatic execution must be proved outcome- and agency-preserving; the proposed `slow`
policy and broader confluence analyses remain optional research, not engine semantics.

See [`API.md`](API.md#current-implementation-divergence),
[`AUTOEXEC.md`](AUTOEXEC.md#current-implementation), and
[`SMART_AUTOEXEC.md`](SMART_AUTOEXEC.md#validation-strategy).

## Other indexed programs

These directions are substantial enough not to masquerade as small TODOs. Their current priority is
recorded above even though their detailed status differs. The remaining Tier 1 programs appear
first in priority order. Tier 2 follows, with its final two programs alphabetized because
[`VALUES.md`](VALUES.md#current-major-plan-priority) deliberately leaves them unordered.

### Rewrite the agent handbook around the settled model

Shorten the large orientation documents into focused tours, teaching documents, feature references,
or surveys; incorporate the `perf` findings into the performance guide. Preserve stable current
models and live decisions, remove migration history and agent reasoning, and keep detailed plans in
their smallest owning documents rather than expanding this index.

Specifically: make `ENGINE.md` and `GAMEWORLD.md` quick tours; reduce `API.md` and
`RESPONSIBILITIES.md` to their core decisions; make `IDENTITY.md` educational; focus
`QUANTIFIERS.md`, `PROPERTIES.md`, `EACH.md`, and `TESTING.md` as references; let
`SEQUENCING.md` and `AUTOEXEC.md` survey their improvement directions; and rewrite
`JVM_TEST_PERFORMANCE.md` holistically and identically on `perf` and `main`. Apply
[`README.md`](README.md#maintain-this-collection) while performing the rewrite.

### Consolidate public contracts and failure boundaries

Install Kotlin binary-API validation for the public `pets`, `engine`, `agent`, `tfm-canon`, and
`script` libraries, consolidate exception handling so domain failures remain precise while defects
retain their stack traces, and finish removing vague runtime terms such as “operation” and
“gameplay command” in favor of the exact lifecycle meant. Treat these as contract work, not
compatibility preservation; there are no known clients requiring obsolete APIs.

See [`API.md`](API.md#layer-responsibility), [`VISIBILITY.md`](VISIBILITY.md), and issue
[#42](https://github.com/MartianZoo/solarnet/issues/42).

### Simplify Pets and runtime semantics

- Repair the two declared Pets conformance gaps and the compatible-refinement/default problem
  without adding a second representation of Type identity: L3-8 stage divergence after defaults and
  T8-3 substitution into the wrong compatible dependency slot.
- Give refinements an explicit candidate when nested dependencies must relate to it, and add a real
  structural conjunction so rules can name intersections such as owned tiles without nominal proxy
  Classes.
- Support honest nested fanout where the rule genuinely iterates two domains, with Quick Start's
  players-by-standard-resources production as the concrete proof case.
- Separate the expression API's natural, compact resolved, and full resolved intents, with an
  explicit `ClassTable` for resolution.
- Resolve the contextual `Owner`/ownership-Class overload and remove its Kotlin carve-outs; simplify
  `LiveEffect` Actor binding and separate `Instructor`'s resolution capability from execution.
- Represent direct point-event `Signal`s honestly rather than as self-transmutations, preserving
  their paired gain/removal triggers while authored reflexive transmutations remain invalid; and
  separate cleanup lifetime from log visibility.

The specifications own the final semantics. Start with [`IDENTITY.md`](IDENTITY.md#open-audit),
[`TYPES.md`](TYPES.md), [`PROPERTIES.md`](PROPERTIES.md#design-constraints-for-future-extensions),
and the relevant rules in [`type-system-spec.md`](../type-system-spec.md) and
[`pets-language-spec.md`](../pets-language-spec.md).

### Reassess performance only where it changes what work is possible

Review the committed `OverlayWorld` and query-performance work on `perf`, then integrate only
coherent measured improvements. Profile Type allocation in `ClassTable.glb`, `narrows`, and repeated
dependency/refinement construction, and evaluate candidate-selection hooks for expensive
`CustomMetric` refinements. Compiling Pets at build time remains conditional on one compiler
replacing runtime work without creating a second semantic model.

See [`JVM_TEST_PERFORMANCE.md`](JVM_TEST_PERFORMANCE.md) and the reusable-universe program above.

### Delete machinery justified only by marginal content

Desupport Land Claim and Arcadian Communities, then remove the reservation/occupancy machinery they
alone require. Separately desupport Mons Insurance, Crash Site Cleanup, and Law Suit; express
Hydrologist with player-owned watchers; then remove unused attack-history and Actor-value-reuse
machinery. These are deliberate applications of the project's willingness to trade minor card
coverage for a smaller honest model.

See [`VALUES.md`](VALUES.md#model-the-game-honestly) and review the affected entries in
[`ENGINE.md`](ENGINE.md#content-must-not-compensate-for-an-engine-gap) before changing behavior.

### Strengthen replay evidence and provenance

Complete `Game20260820Test` past its generation-6 checkpoint from the preserved log, player data,
and eight later screenshots, keeping every checkpoint independently sourced. Stamp full application
builds with the Git commit and a stable hash of source changes, and include the stamp in exported
recordings so their producing engine can be identified or verified. Add richer runtime diagnostics
only through the layer-owned, opt-in logs already proposed; do not turn debug metadata into game
meaning.

See [`GAMEWORLD.md`](GAMEWORLD.md#serialized-events-and-exported-recordings),
[`DIAGNOSTICS.md`](DIAGNOSTICS.md), and
[`TESTING.md`](TESTING.md#game-replay-tests).

## Deliberately later or conditional directions

No total order has been selected for the following conditional work.

- **Instruction-valued properties:** investigate one source for printed facts and live
  materialization only after group, binding, and query semantics are coherent. See
  [`PROPERTIES.md`](PROPERTIES.md#instruction-and-printed-tags).
- **Broader responsibility extraction:** further Catalog, script-shell, JLine, and generic workflow
  extraction is aspirational and must be independently valuable to Solarnet, not justified by a
  hypothetical second game. See
  [`RESPONSIBILITIES.md`](RESPONSIBILITIES.md#conditional-extraction-order).
- **Optimal-solo analysis:** the TR63 monotonicity work is retained research, not an implemented or
  currently scheduled optimizer. See [`OPTIMAL_SOLO.md`](OPTIMAL_SOLO.md).
- **Perfect diagnostics and causal analysis:** improve them when they cheaply support Tier 1 work;
  they do not justify a parallel semantic history model. See [`DIAGNOSTICS.md`](DIAGNOSTICS.md) and
  [`VALUES.md`](VALUES.md#priority-tiers).
