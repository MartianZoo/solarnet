# Project Notes For Codex

## Tooling

- Solarnet's preferred JDK is 21. Use JDK 21 for local Gradle work; its current Homebrew path is
  `/opt/homebrew/Cellar/openjdk@21/21.0.11/libexec/openjdk.jdk/Contents/Home`.
- The machine's default Java 26 can fail before project code runs, with misleading early Gradle/Kotlin DSL errors.
- The routine engine test suite is `:engine:allTests`, which skips slow browser tests by default.
- Use `:engine:allTestsIncludingSlow` or `:engine:allTests -PincludeSlowTests=true` when you
  really want all engine tests, including slow browser tests.

## Game Log Tests

- In Herokuapp log translation tests, use `.expect()` only for interesting, partial, net deltas. Do not mechanically list every cost or resource movement.
- Screenshot-based assertions often need timing adjustment rather than test restructuring. If asserting after Research from a pre-buy screenshot, adjust only the affected money and hand counts.
- Do not chase the Terraforming Mars app repo for these tests unless explicitly asked. Prefer local tests, local map data, supplied logs, and supplied images.
- Herokuapp-to-Solarnet map coordinates are a recurring source of mistakes. Use existing tests and local map data carefully when translating them.
- Work around unsupported engine behavior in the smallest local way that preserves the real flow, and record genuine follow-ups in `TODO.md`.

## Git

- Push problems in this workspace are more likely to be credentials or Codex app state than a missing `origin`.
- The repo is intended to use SSH remotes with the OS agent/keychain for noninteractive Git operations.
