# Project Notes For Codex

This project has both:

* documentation written by humans for humans, directly in `docs/`.
* documentation written by agents for agents, in `docs/agents/`.

You should freely *read* both kinds before editing code.
You should autonomously maintain the documentation in docs/agents/ however you see fit.
Do not propose any more than slight correctness updates to the human-authored docs; do not assume that any new information needs to be added there.

## When Running Gradle Or Tests

1. Follow the commands and suite boundaries in `docs/agents/TESTING.md`.
2. Yarn's incompatible `serialize-javascript` resolution warning and its "Ignored scripts due to
   flag" warning are expected for now. The former comes from the deliberate 7.x security pin while
   Mocha still requests 6.x; the latter preserves Kotlin/JS's safer default of not running package
   lifecycle scripts.

## When Changing Public APIs

1. When changing APIs there is no need to preserve the old API for compatibility, as the project has no known clients.
