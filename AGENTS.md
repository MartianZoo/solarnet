# Project Notes For Codex

This project has both:

* documentation written by humans for humans, directly in `docs/`.
* documentation written by agents for agents, in `docs/agents/`.

You should freely *read* both kinds before editing code.
You should autonomously maintain the documentation in docs/agents/ however you see fit.
Do not propose any more than slight correctness updates to the human-authored docs; do not assume that any new information needs to be added there.

## Other Worktrees Are Out Of Scope

- Never discover, enumerate, resolve, inspect, read, write, or otherwise access any linked or
  sibling worktree outside this project root. Do not inquire whether another worktree exists, where
  it is located, whether it is clean, or what files it contains.
- Do not run `git worktree list`, `git -C` against another worktree, filesystem searches for sibling
  worktrees, or any helper or helper mode that accesses another worktree. Do not read worktree path
  metadata from Git internals.
- Treat names such as `work1` only as local branch refs. Their committed histories and tip commits
  may be inspected and merged through the repository in this project root without locating or
  accessing any working copy.
- Merge and synchronization workflows operate only in this project root or in temporary storage
  under `$TMPDIR`. They merge committed branch tips into `main`; they never inspect uncommitted work
  elsewhere and never stage, commit, stash, discard, format, or otherwise alter it.
- Never advance, reset, or otherwise update a source branch ref after merging it into `main`. The
  branch may be checked out in an inaccessible working copy. Leave source refs and every associated
  working copy exactly as they are.
- A request to run `merge-all-work` means to perform its branch-merging purpose under these rules.
  The helper must use a branch-only workflow from this project root and must leave source refs
  unchanged.
- The ignored `_local/advance-worktrees-to-main` helper is the sole exception. Run it only when the
  user explicitly asks for that separate post-merge step. It may inspect registered `workN` working
  copies and fast-forward only those that are clean and whose branch tips are already contained in
  verified `origin/main`; it must leave dirty, missing, or divergent working copies unchanged.

## When Running Gradle Or Tests

1. Follow the commands and suite boundaries in `docs/agents/TESTING.md`.
2. Yarn's incompatible `serialize-javascript` resolution warning and its "Ignored scripts due to
   flag" warning are expected for now. The former comes from the deliberate 7.x security pin while
   Mocha still requests 6.x; the latter preserves Kotlin/JS's safer default of not running package
   lifecycle scripts.
3. Normal Gradle access to the user-level cache and configuration under `~/.gradle` is permitted
   without asking for separate approval.

## Test Fixture Reconciliations

- Never call `sneak` directly in a game fixture. Use the fixture's `exMachina` helper for an
  evidence-backed player error that requires a direct state adjustment.
- Place `exMachina` as late in the timeline as the sourced assertions allow, and precede it with a
  comment saying which later step requires the adjustment.
- Never hide a manual or other raw state reconciliation inside the body of an unrelated
  action, card play, turn, or phase merely because that body provides an executable context. Keep
  the adjustment as a standalone timeline statement at the evidence-supported boundary. If a
  prepared task prevents that, use an explicit fixture-level mechanism or fix the helper/API; do
  not make the unrelated action appear to have caused the adjustment.
- A missing consequence may be handled inside an action body only when it is genuinely caused by
  that exact action, and the comment must name that causal relationship.

## Fixture DSL Design

- Keep gameplay and fixture APIs generic. Never add a Kotlin function or DSL operation solely to
  represent one card, corporation, prelude, or other individual game component.
- Use existing gameplay helpers when their operation boundaries fit. Express component-specific
  steps directly through the existing `OperationBody` primitives when they must remain in an outer
  operation, including when a sibling task must stay pending.
- Add a shared fixture helper only for a recurring, component-independent concept that materially
  simplifies multiple call sites.

## Terraforming Mars Rule Research

- Only a post by Jacob Fryxelius is authoritative when investigating a rule question, period.
- The Terraforming Mars FAQ PDF is useful because it links to those posts; its summaries and all
  other secondary sources are not themselves authoritative.

## When Changing Public APIs

1. When changing APIs there is no need to preserve the old API for compatibility, as the project has no known clients.
