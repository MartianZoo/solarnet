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

## Test Fixture Reconciliations

- Never call `sneak` directly in a game fixture. Use the fixture's `mistake` helper for an
  evidence-backed player error that requires a direct state adjustment.
- Place `mistake` as late in the timeline as the sourced assertions allow, and precede it with a
  comment saying which later step requires the adjustment.
- Never hide a manual or other raw state reconciliation inside the body of an unrelated
  action, card play, turn, or phase merely because that body provides an executable context. Keep
  the adjustment as a standalone timeline statement at the evidence-supported boundary. If a
  prepared task prevents that, use an explicit fixture-level mechanism or fix the helper/API; do
  not make the unrelated action appear to have caused the adjustment.
- A missing consequence may be handled inside an action body only when it is genuinely caused by
  that exact action, and the comment must name that causal relationship.

## When Changing Public APIs

1. When changing APIs there is no need to preserve the old API for compatibility, as the project has no known clients.
