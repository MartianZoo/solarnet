# Agent documentation router

> **Agent information:** These are focused notes for agents. They can record human decisions, but
> they are not human-authored documentation. Source, specifications, and tests win when a note
> differs.

Read only the route needed for the task. Current project direction lives in the human-facing
[`SOLARNET_ROADMAP.md`](../../SOLARNET_ROADMAP.md) and
[`PETS_ROADMAP.md`](../../PETS_ROADMAP.md); bounded loose work lives in
[`TODO.md`](../../TODO.md). Do not recreate a separate agent portfolio of future plans.

## Behavior and architecture

Read [`VALUES.md`](VALUES.md) before designing, implementing, or reviewing a behavior or
architecture change. Then choose the narrowest relevant note:

| Subject | Read |
| --- | --- |
| Live World construction, components, tasks, effects, transactions, rollback, or Agent responsibilities | [`ENGINE.md`](ENGINE.md) |
| Actor attribution, Admin, assignment, delegated narrowing, context ownership, or lexical `Me` | [`IDENTITY.md`](IDENTITY.md) |
| Task order, `THEN`, automatic effects, delegated control, or cleanup | [`SEQUENCING.md`](SEQUENCING.md) |
| Payment choices, accepted resources, billing, or settlement | [`PAYMENTS.md`](PAYMENTS.md) |
| Gain/removal/transmutation counts, AMAP, or abstract targets | [`QUANTIFIERS.md`](QUANTIFIERS.md) |
| Direct correction, `exMachina`, `sneak`, effect suppression, or correction invariants | [`EX_MACHINA.md`](EX_MACHINA.md) |
| Card backs, draws, purchases, reveals, or replay card tracking | [`CARD_HANDLING.md`](CARD_HANDLING.md) |
| Who may submit a player's decisions, hidden information, accepted game history, or agreed undo | [`ADVERSARIAL.md`](ADVERSARIAL.md) |

Solarnet calculates game consequences for the Actor whose Agent receives the request. It does not
authenticate the person or program using that Agent. Before treating caller authorization, secret
custody, or agreement on a live game as an engine gap, use the adversarial-play route above: those
responsibilities belong to the surrounding application, whose implementation remains open.

For actions, workflow, public client APIs, automatic policy, export, runtime layering, diagnostics,
or content selection, inspect the current source and tests first. Use the matching roadmap only for
selected future direction. The retired agent notes for those subjects had become duplicate design
records.

## API specifications and KDoc

For API documentation or specification-fidelity work, read
[`SPEC_FIDELITY.md`](SPEC_FIDELITY.md). It owns the nine-module priority scope, intended audience,
public API coverage, shared-rule links, and the relationship between intended contracts and
implementation defects. Inspect the relevant source, specifications, and tests as evidence.

## Pets, content, and tools

| Subject | Read |
| --- | --- |
| Classes, Types, refinements, dependencies, properties, defaults, or Type variables | The cited rule in [`type-system-spec.md`](../type-system-spec.md) and its matching `Spec*Test.kt` |
| Pets declarations, expressions, instructions, effects, actions, ownership, fanout, or elaboration | The cited rule in [`pets-language-spec.md`](../pets-language-spec.md) and its matching `Lang*Test.kt` |
| Class and display names | [`NAMING.md`](NAMING.md) |
| Generated Kotlin types for the canonical Pets vocabulary | [`PETS_TYPE_GENERATOR.md`](PETS_TYPE_GENERATOR.md) |
| Generated map-area declarations | [`MAP_PETS_GENERATION.md`](MAP_PETS_GENERATION.md) |
| Kotlin declaration visibility | [`VISIBILITY.md`](VISIBILITY.md) |

For Catalog, Bundle, Module, Content, configuration, or game-view class tables, begin with KDoc on
`Catalog`, `Bundle`, and `GamePremise`, followed by the matching tests. Completed migration ledgers
and proposed replacement models are intentionally not preserved here.

## Verification and reconstruction

- Read [`TESTING.md`](TESTING.md) before editing or running tests, Gradle, formatting, or benchmarks.
- Before staging or reporting an implementation complete, apply
  [`REVIEW_CRITERIA.md`](REVIEW_CRITERIA.md) to the entire diff.
- For a herokuapp archive, read [`HEROKUAPP_GAME_LOGS.md`](HEROKUAPP_GAME_LOGS.md), plus the replay
  sections of `TESTING.md`.
- For a physical game record, read [`_local/OTB_GAME_RECORDS.md`](../../_local/OTB_GAME_RECORDS.md),
  plus the replay sections of `TESTING.md`.
- Read [`OPTIMAL_SOLO.md`](OPTIMAL_SOLO.md) only for the retained TR63 monotonicity research or its
  report tool.

## Maintain this collection

- Keep a note only when it owns a current contract or a repeatable procedure that is not clearer in
  source, specification, KDoc, tests, a roadmap, or `TODO.md`.
- Link to source and meaningful tests instead of copying inventories that drift.
- Give source files and stable search strings rather than line numbers.
- Delete completed audits, migration ledgers, experiments, and design alternatives after their
  current conclusion has moved to the proper source of truth.
- Do not add an agent document merely to collect possible future work.
