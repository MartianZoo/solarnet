# Project Notes For Codex

## When Learning About The Project

1. Read `README.md` for project basics and `docs/packages.md` for the package layout.
2. Read `docs/language-intro.md` for the component-based representation model and
   `docs/component-types.md` for the specific component classes.
3. Read `docs/type-system.md` for the Pets type system, and `docs/syntax.md` plus
   `docs/cheat-sheet.md` for Pets syntax.
4. Read `docs/engine.md` for engine execution, queues, events, gameplay APIs, dependency injection,
   and workflow details.
5. Read `docs/game-insights.md` for obscure design decisions and `docs/faq.md` for project goals,
   non-goals, and priorities.
6. For identity vocabulary, use `glossary.md` and `docs/engine.md`. For World Government
   Terraforming work, use `plans/world-government.md`.
7. Read `docs/testing.md` before changing or running tests.

## When Running Gradle Or Tests

1. Follow the commands and suite boundaries in `docs/testing.md`.
2. Yarn's incompatible `serialize-javascript` resolution warning and its "Ignored scripts due to
   flag" warning are expected for now. The former comes from the deliberate 7.x security pin while
   Mocha still requests 6.x; the latter preserves Kotlin/JS's safer default of not running package
   lifecycle scripts.

## When Changing Public APIs

1. When changing APIs there is no need to preserve the old API for compatibility, as the project has no known clients.
