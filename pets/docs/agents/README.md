# Pets agent documentation router

These notes record current contracts, selected design directions, and explicitly labeled proposals.
Source, specifications, and meaningful tests establish behavior. Read only the route needed for the
task; [AGENTS.md](../../AGENTS.md) supplies repository instructions.

Before behavior or architecture changes, read [VALUES.md](VALUES.md). Keep bounded unfinished work
in [TODO.md](../../TODO.md). Never turn an exposed dead end into a model defect merely because a
client has not implemented pruning.

## Language, types, and Catalogs

| Subject | Read |
| --- | --- |
| Classes, types, refinements, dependencies, properties, defaults, or variables | Relevant rules in [type-system-spec.md](../type-system-spec.md) and matching `Spec*Test.kt` |
| Expressions, instructions, effects, actions, fanout, or elaboration | Relevant rules in [pets-language-spec.md](../pets-language-spec.md) and matching `Lang*Test.kt` |
| Static elaboration and effect specialization | [ELABORATION.md](ELABORATION.md) |
| Lexical `Me`, ownership inference, and saved ownership experiments | [OWNERSHIP.md](OWNERSHIP.md) |
| Gain/removal quantifier spelling and defaults | [QUANTIFIERS.md](QUANTIFIERS.md) |
| Catalogs, content selection, configuration, premises, and game class tables | [CATALOG.md](CATALOG.md), then KDoc and tests for `Catalog`, `Bundle`, and `GamePremise` |
| Public API contracts, KDoc, conformance, or deliberate AST/parser visibility | [SPEC_FIDELITY.md](SPEC_FIDELITY.md) |

## Canon and derived views

| Subject | Read |
| --- | --- |
| Authored actions, action identity, or missing action slots | [ACTIONS.md](ACTIONS.md) |
| Turn offers, Head Start, phases, and authored game flow | [GAME_FLOW.md](GAME_FLOW.md) |
| Card backs, locations, offers, and searches | [CARD_MODEL.md](CARD_MODEL.md) |
| Payment signals, pricing, billing, acceptance, or action-cost lowering | [PAYMENTS.md](PAYMENTS.md) |
| Proposed fallback instructions | [ELSE.md](ELSE.md) |
| Canon invariants and historical composition hazards | [GAME_HACKS.md](GAME_HACKS.md) |
| Class names and localized display names | [NAMING.md](NAMING.md) |
| English rendering, vocabulary, wording, or card layout | [LANGUAGE.md](LANGUAGE.md); [NLG_REVIEW.md](NLG_REVIEW.md) is a review snapshot |
| Generated Kotlin vocabulary | [PETS_TYPE_GENERATOR.md](PETS_TYPE_GENERATOR.md) |
| Generated map areas | [MAP_PETS_GENERATION.md](MAP_PETS_GENERATION.md) |
| Static resource monotonicity report | [MONOTONICITY.md](MONOTONICITY.md) |

## Verification and maintenance

Read [TESTING.md](TESTING.md) before editing or running tests, Gradle, generators, or formatting.
[BUILDS.md](BUILDS.md) covers the standalone build and publication. Apply
[REVIEW_CRITERIA.md](REVIEW_CRITERIA.md) before reporting work complete or staging a commit.

Keep each design decision in its owning note, link to source and meaningful tests, and distinguish
intended behavior from verified implementation. Prefer stable source names and search strings to
line numbers. Register new subject notes here; do not create competing explanations of shared rules.

Runtime tasks, Actor assignment, consequence calculation, automatic policy, rollback, and replays
belong to the [Solarnet router](../../../docs/agents/README.md). Gameplay evidence can live there
without becoming a dependency of the static build.
