# Agent documentation router

> **Agent information:** These are focused notes for agents. They can record human decisions, but
> they are not human-authored documentation. Source, specifications, and tests establish current
> behavior; explicitly recorded design decisions may describe intended behavior not yet implemented.

Read only the route needed for the task. Capture design decisions in the individual owning
`docs/agents/` notes, distinguishing agreed intent, proposals, open questions, and current behavior.
The human-facing
[`SOLARNET_ROADMAP.md`](../SOLARNET_ROADMAP.md) and
[`PETS_ROADMAP.md`](../PETS_ROADMAP.md) index those notes and summarize priorities and status;
they should not own detailed design decisions. Bounded loose work lives in [`TODO.md`](../../TODO.md).

## Behavior and architecture

Read [`VALUES.md`](VALUES.md) before designing, implementing, or reviewing a behavior or
architecture change. Then choose the narrowest relevant note:

| Subject | Read |
| --- | --- |
| Live World construction, components, tasks, effects, or Agent responsibilities | [`ENGINE.md`](ENGINE.md) |
| Actor attribution, on-turn identity, Admin, assignment, or delegated narrowing | [`IDENTITY.md`](IDENTITY.md); lexical ownership is in [Pets](../../pets/docs/agents/OWNERSHIP.md) |
| Game flow, turns and offers, Head Start timing, task order, `THEN`, automatic effects, delegated control, cleanup, or task priority | [`SEQUENCING.md`](SEQUENCING.md) |
| Payment choices, accepted resources, billing, or payment completion | [`PAYMENTS.md`](PAYMENTS.md) |
| Client interaction, transactions, task priorities, or mandatory Admin handling | [`ENGINE_CONTRACT.md`](ENGINE_CONTRACT.md) |
| Transaction grouping, execution motivation, event ordinals, or transaction-based checkpoints | [`ORDINAL_REWORK.md`](ORDINAL_REWORK.md) — proposed preparation plan |
| Inventory of automatic task revisions, admission, resolution, execution, and their timing | [`TASK_HANDLING.md`](TASK_HANDLING.md) — source-based draft of current behavior |
| Single-action decision graphs, agent pruning, or speculative card choices | [`DECISION_GRAPH.md`](DECISION_GRAPH.md) — raw discussion notes only |
| Gain/removal/transmutation counts, AMAP, or abstract targets | [`QUANTIFIERS.md`](QUANTIFIERS.md) |
| Correction, `exMachina`, `sneak`, effect suppression, or correction invariants | [`EX_MACHINA.md`](EX_MACHINA.md) |
| Card backs, draws, purchases, reveals, or replay card tracking | [`CARD_HANDLING.md`](CARD_HANDLING.md) |
| Authored actions, action identity, invocation, or missing action slots | [`ACTIONS.md`](../../pets/docs/agents/ACTIONS.md) |
| Who may submit a player's decisions, hidden information, accepted game history, or agreed undo | [`ADVERSARIAL.md`](ADVERSARIAL.md) |

Solarnet calculates game consequences for the Actor whose Agent receives the request. It does not
authenticate the person or program using that Agent. Before treating caller authorization, secret
custody, or agreement on a live game as an engine gap, use the adversarial-play route above: those
responsibilities belong to the surrounding application, whose implementation remains open.

For public client APIs, export, runtime layering, diagnostics, or content
selection, inspect the current source and tests for implemented behavior. Use the matching
roadmap to locate the owning design note and understand its priority and status. Keep detailed
decisions in that note rather than duplicating them in the roadmap.

The [automatic-effect audit](automatic-effects-audit.tsv) inventories the 2026-10-16 source uses
of `::`, including fake cards. Paths are repository-relative. `assessment` and `source_excerpt`
record the audit finding; `current_status` distinguishes retained effects from later queued
conversions. The inventory is evidence for follow-up, not proof that a proposed conversion works.

For game export/import design, read [`EXPORT.md`](EXPORT.md), then inspect current recording,
decision-log, premise, and replay code for implemented behavior.

## API specifications and KDoc

For API documentation or specification-fidelity work, read
[`SPEC_FIDELITY.md`](SPEC_FIDELITY.md). It records the
runtime API priorities and links to the shared documentation standard maintained by Pets. Inspect the relevant source, specifications, and tests as evidence.

## Pets, content, and tools

Use the [Pets router](../../pets/docs/agents/README.md) for language/types, Catalogs, lexical
ownership, Canon modeling, naming, English rendering, and static generators. The
[Pets documentation index](../../pets/docs/README.md) also links introductory guides and comparisons.

For the exploratory proposal to remove state-dependent `HAS` from Types while retaining it in
Expressions, read [MAKE_TYPES_TYPES_AGAIN.md](MAKE_TYPES_TYPES_AGAIN.md).

Kotlin visibility auditing across runtime modules remains in [VISIBILITY.md](VISIBILITY.md).

## Verification and reconstruction

- For build separation, Pets source selection, publication, or isolated-build verification, read
  [BUILDS.md](BUILDS.md).

- Read [`TESTING.md`](TESTING.md) before editing or running tests, Gradle, formatting, or benchmarks.
- Before staging or reporting an implementation complete, apply
  [`REVIEW_CRITERIA.md`](REVIEW_CRITERIA.md) to the entire diff.
- For a herokuapp archive, read [`HEROKUAPP_GAME_LOGS.md`](HEROKUAPP_GAME_LOGS.md), plus the replay
  sections of `TESTING.md`.
- For a physical game record, read [`_local/OTB_GAME_RECORDS.md`](../../_local/OTB_GAME_RECORDS.md),
  plus the replay sections of `TESTING.md`.

## Historical material

[obsolete/README.md](obsolete/README.md) indexes material retained for archaeology only.
It supplies no current requirements. [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns the
interaction terms and distinguishes selected direction from remaining proposals; keep those terms
there until explicitly approved for the glossary. [DECISION_GRAPH.md](DECISION_GRAPH.md) holds
unfinished agent exploration notes separately.

## Maintain this collection

- Each subject's note owns its design decisions, including selected future behavior and unresolved
  design questions. Prefer updating an existing owning note; link between subjects rather than
  duplicating decisions. Keep roadmap entries to brief summaries, status, and links.
- Current contracts and repeatable procedures may also live here when not clearer in source,
  specification, KDoc, or tests. Keep miscellaneous implementation tasks in `TODO.md`.
- Link to source and meaningful tests instead of copying inventories that drift.
- Give source files and stable search strings rather than line numbers.
- Delete completed audits, migration ledgers, experiments, and design alternatives after their
  current conclusion has moved to the proper source of truth.
- Add a design note for a distinct subject when needed, not merely to collect miscellaneous future
  work. Register it in this router and link it from the relevant roadmap entry.
