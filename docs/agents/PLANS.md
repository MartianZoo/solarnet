# Major plans

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** choosing a substantial next project, comparing programs of work, or deciding where
> a newly discovered large change belongs.
>
> **Status:** portfolio index without a definitive execution order. The owner documents linked
> below remain authoritative about current behavior, decisions, prerequisites, and acceptance
> criteria.

[`VALUES.md`](VALUES.md) says what outcomes matter. This page says which major changes could pursue
them. [`TODO.md`](../../TODO.md) is reserved for bounded miscellaneous work that does not belong to
one of these programs.

## Priority

[VALUES.md](VALUES.md#current-major-plan-priority) owns the current emphasis. No definitive sequence
has been selected among card tracking, decision export/import, autoexec improvement, payment
simplification, and the card trifecta, or between these and the other programs below. The order of
this index does not establish one. Preserving existing replay evidence is a proof obligation for
changes, not a competing program.

## Selected programs

### Reach the single-source card trifecta

Bring roughly 300 cards to correct behavior, good English instructions, and good iconographic
depictions from one semantic source. This is a concrete milestone; individual rendering work and
current card coverage must not be mistaken for its completion.

See [VALUES.md](VALUES.md#tier-1-defining-and-worth-active-investment).

### Restore a coherent middle-ground card-tracking game-playing API

Track card locations outside the engine, building on the approach demonstrated by the card-tracking
test harness. Keep engine card counts and avoid restoring the full real-card mode. End-to-end solo
play should use the same capabilities for a human or a computer; the public API design remains open.

See [CARD_HANDLING.md](CARD_HANDLING.md#selected-game-playing-direction).

### Make decision export and import faithful and clean

Establish confidence that the format accurately represents player decisions without incidental
engine implementation detail. Reconstructing a played game must not rely on choices omitted from
the file. Saving and resuming partial games is not a priority or an acceptance gate.

See [EXPORT.md](EXPORT.md#goal-and-the-three-views-of-one-game).

### Make actions, payments, and completion one intelligible lifecycle

Reduce distributed payment offers, reconstructed action identity, global idle waits, and client
task searches. P1 must be able to order P2's Neptunian offer, then yield control throughout P2's
accepted payment. Current delegation lasts only for the selected task; payment helpers conceal
the missing authority rule by selecting through other Actors.

Nested priority groups and exclusive operation scopes are the live scheduling alternatives.
Neither is selected. The unresolved questions include inherited control, nested choices, operation
membership, and deferred cleanup before caller or Admin progression. Payment representation and
action identity remain related design work; there is no prescribed prototype or migration order.

See [`ACTIONS.md`](ACTIONS.md#open-design-questions),
[`PAYMENTS.md`](PAYMENTS.md#design-options), and
[`SEQUENCING.md`](SEQUENCING.md#delegated-operations-and-scheduling-options).

### Express phase progression through Pets

Phase progression should follow authored game rules without a mirrored Kotlin sequence. Current
phase components do not accomplish that: the coroutine still chooses the order and waits for global
idleness. How phase transitions, Player decisions, event cleanup, and Admin work share the proposed
scheduling model remains open.

See [`WORKFLOW.md`](WORKFLOW.md) and
[`RESPONSIBILITIES.md`](RESPONSIBILITIES.md#workflow-progression-and-task-scheduling).

### Align REPL color modes and migrate manual gameplay tests

Purple provides full-game workflow; blue grants an action slot, green initiates an arbitrary
operation, yellow permits task abandonment, and red applies corrections without advancing gameplay.
Blue and green reject overlapping initiation, isolate their new work from existing game work, and
must not restart automatic progression. Red preserves pending tasks. Pending-work disposal for
blue/green, other transitions, and yellow's exact atomicity guarantee remain open.
Migrate ordinary gameplay tests to the full workflow while preserving dedicated mode coverage.

See [`COLOR_MODES.md`](COLOR_MODES.md) for the selected contracts and unresolved decisions.

### Complete the Agent boundary and policy system

Keep passive Game World data below a policy-free engine and an Actor-scoped Agent above it.
Scheduling eligibility and delegated authority belong in the engine; Agent policy chooses only
among legal commands. Reader scoping, the ordinary Agent surface, and policy composition remain
design questions. More powerful automatic execution must preserve outcomes and agency.

See [`API.md`](API.md#current-implementation-divergence),
[`AUTOEXEC.md`](AUTOEXEC.md#current-implementation), and
[`SMART_AUTOEXEC.md`](SMART_AUTOEXEC.md#validation-strategy). Admin-routing questions are recorded in
[`TASK_ROUTING_EXPERIMENT.md`](TASK_ROUTING_EXPERIMENT.md).

## Other indexed programs

These directions are substantial enough not to masquerade as small TODOs. Their detailed status and
prerequisites differ; listing them here does not resolve their priority relative to the current
emphasis above.

### Make bundle content selection generic and inspectable

Replace Terraforming-Mars-specific discovery of cards, colony tiles, map areas, milestones, and
awards with the proposed `.pets`/`.content.pets` source convention. Same-named Modules supply their
bundle's content roots by a documented generic rule; authored structural dependencies and
`premiseRequirement`, checked by generic premise validation, replace bundle-derived availability.
Exact pools and live setup choices remain separate configuration concerns.

See
[`CONTENT_SELECTION.md`](CONTENT_SELECTION.md#proposed-generic-bundlecontent-convention).

### Consolidate public contracts and failure boundaries

Install Kotlin binary-API validation for the public `pets`, `engine`, `agent`, `tfm-canon`, and
`script` libraries, consolidate exception handling so domain failures remain precise while defects
retain their stack traces, and finish removing vague runtime terms such as “operation” and
“gameplay command” in favor of the exact lifecycle meant. Treat these as contract work, not
compatibility preservation; there are no known clients requiring obsolete APIs.

See [`API.md`](API.md#layer-responsibility), [`VISIBILITY.md`](VISIBILITY.md), and issue
[#42](https://github.com/MartianZoo/solarnet/issues/42).

### Simplify Pets and runtime semantics

- Give refinements an explicit candidate when nested dependencies must relate to it, and add a real
  structural conjunction so rules can name intersections such as owned tiles without nominal proxy
  Classes.
- Support honest nested fanout where the rule genuinely iterates two domains, with Quick Start's
  players-by-standard-resources production as the concrete proof case.
- Separate the expression API's natural, compact resolved, and full resolved intents, with an
  explicit `ClassTable` for resolution.
- Replace implicit ownership shorthand with explicit `OWN[...]`, including whole-effect transforms
  and automatic card/map marks. Study removal of runtime ownership inference in the same work;
  [`IDENTITY.md`](IDENTITY.md#future-direction) records the authoring direction and unresolved runtime
  inference. Ordinary `Owned`/`Owner` declarations are the longer-term goal. Separately, divide
  `Instructor`'s resolution capability
  from execution.
- Represent direct point-event `Signal`s honestly rather than as self-transmutations, preserving
  their paired gain/removal triggers while keeping authored self-transmutations forbidden; and
  separate cleanup lifetime from log visibility.

The specifications own the final semantics. Start with [`IDENTITY.md`](IDENTITY.md#lexical-ownership-model),
[`TYPES.md`](TYPES.md), [`PROPERTIES.md`](PROPERTIES.md#design-constraints-for-future-extensions),
and the relevant rules in [`type-system-spec.md`](../type-system-spec.md) and
[`pets-language-spec.md`](../pets-language-spec.md).

### Reassess performance only where it changes what work is possible

Review the committed `OverlayWorld` and query-performance work on `perf`, then integrate only
coherent measured improvements. Profile Type allocation in `ClassTable.glb`, `narrows`, and repeated
dependency/refinement construction, and evaluate candidate-selection hooks for expensive
`CustomMetric` refinements. Compiling Pets at build time remains conditional on one compiler
replacing runtime work without creating a second semantic model.

See [`JVM_TEST_PERFORMANCE.md`](JVM_TEST_PERFORMANCE.md).

### Retain claims through shared occupancy

Keep Land Claim and Arcadian Communities: the shared `Area` occupancy limit and automatic removal
of an owner's claim express their placement rules without dedicated engine machinery. `Community`,
the `Occupant`/`OwnedOccupant` roles, and the opposing-occupant check in `DefaultGreeneryTile` earn
their cost here. The positive reward trigger and a shorter greenery expression remain in
[`TODO.md`](../../TODO.md); revisit the retained cost if a smaller coherent model emerges.

### Delete machinery justified only by marginal content

Desupport Mons Insurance, Crash Site Cleanup, and Law Suit; express
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
  [`RESPONSIBILITIES.md`](RESPONSIBILITIES.md).
- **Optimal-solo analysis:** the TR63 monotonicity work is retained research, not an implemented or
  currently scheduled optimizer. See [`OPTIMAL_SOLO.md`](OPTIMAL_SOLO.md).
- **Perfect diagnostics and causal analysis:** improve them when they cheaply support Tier 1 work;
  they do not justify a parallel semantic history model. See [`DIAGNOSTICS.md`](DIAGNOSTICS.md) and
  [`VALUES.md`](VALUES.md#priority-tiers).
