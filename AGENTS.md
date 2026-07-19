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
6. For identity-related work, use `plans/identity-transition.md` for the domain glossary and current
   stopping-point plan. Treat older audits and code as evidence, not as proof of a user requirement.

## When Running Gradle Or Tests

1. Solarnet's preferred JDK is 21. Use JDK 21 for local Gradle work; its current Homebrew path is
   `/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home`.
2. The machine's default Java 26 can fail before project code runs, with misleading early
   Gradle/Kotlin DSL errors. A bare error mentioning `26.0.1` is a toolchain failure, not a project
   test failure.
3. Use `./gradlew build` for normal repository-wide verification. It covers all modules and the
   routine JVM and JavaScript tests.
4. The routine engine suite is `:engine:allTests`, which skips slow browser tests by default. Use
   `:engine:allTestsIncludingSlow` or `:engine:allTests -PincludeSlowTests=true` only when the slow
   engine browser tests are intentionally required.

## When Working On Kotlin/JS Tests

1. Shared `kotlin.test` test classes and methods must not be private; use internal visibility unless
   they need to be public. JUnit/JVM can accept private test containers that Kotlin/JS will not
   discover correctly.
2. Keep compact Pets forms such as `6T`, `4E`, and `3H` in cross-platform parser coverage. Regex word
   boundaries can behave differently on JVM and JavaScript around a digit followed by an all-caps
   token.
3. Diagnose long browser-test failures by timeout layer. Mocha controls the per-test timeout, while
   Karma controls browser activity, disconnect, and reconnect timeouts. Long synchronous game tests
   can block the browser event loop long enough to hit either one.
4. Do not assume a Node test target is a drop-in substitute for browser tests. Canon's JavaScript
   resource loader currently uses browser `XMLHttpRequest`; Node support needs a different resource
   loading strategy.

## When Translating Herokuapp Game Logs Into Tests

1. Use `.expect()` only for interesting, partial, net deltas. Do not mechanically list every cost or
   resource movement.
2. Screenshot-based assertions often need timing adjustment rather than test restructuring. If
   asserting after Research from a pre-buy screenshot, adjust only the affected money and hand
   counts.
3. Do not chase the Terraforming Mars app repo unless explicitly asked. Prefer local tests, local map
   data, supplied logs, and supplied images.
4. Herokuapp-to-Solarnet map coordinates are a recurring source of mistakes. Use existing tests and
   local map data carefully when translating them.
5. When the log omits a card's payment mix, preserve cash by spending eligible steel or titanium
   where practical. A locally valid arbitrary payment can make later logged actions unaffordable.
6. Work around unsupported engine behavior in the smallest local way that preserves the real flow,
   and record genuine follow-ups in `TODO.md`.

## When Working With Git Branches Or Remotes

1. Fetch before deciding that a branch is already merged or safe to delete, and compare `main`
   against the remote-tracking ref. A local branch may be stale even when newly pushed commits exist
   on its remote counterpart.
2. Push problems in this workspace are more likely to be credentials, sandbox networking, or Codex
   app state than a missing `origin`.
3. The repo is intended to use SSH remotes with the OS agent/keychain for noninteractive Git
   operations.
