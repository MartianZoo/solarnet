# Project Notes For Codex

This project has both:

* documentation written by humans for humans, directly in `docs/`.
* documentation written by agents for agents, in `docs/agents/`.

You should freely *read* both kinds before editing code.
You should autonomously maintain the documentation in docs/agents/ however you see fit.
Do not propose any more than slight correctness updates to the human-authored docs; do not assume that any new information needs to be added there.

## The Design Is The Point

Solarnet is not a feature-accumulation project. A new capability is a loss unless it follows cleanly
from a small, coherent model. Correct behavior is necessary, but an implementation that merely
works can still be a serious failure. The design is the product: we are trying to discover the
smallest set of well-specified rules from which the real game's behavior follows naturally.

Judge cost primarily by permanent conceptual complexity, not raw line count. New abstractions,
layers, APIs, requirements, exceptions, parallel mechanisms, and interactions are expensive.
Substantial self-contained data or straightforward code may be fine; a tiny engine change that
introduces a new concept may not be. Features and flexibility may be deleted when their workarounds
are adequate and the simplification is valuable; propose that tradeoff explicitly rather than
assuming feature preservation is always the goal.

### Interpret Before Acting

- A question, speculation, or design sketch is not an implementation request. Prompts such as
  “could this work?”, “is this worth trying?”, “thoughts?”, and “my thinking is fuzzy” call for
  analysis first. Do not change code until the user actually asks for the change.
- Treat the user's proposed mechanism as a hypothesis, not a specification. Examine the underlying
  goal and recommend a cleaner alternative when one exists.
- Call out contradictions, false premises, and reversals you notice, including contradictions in
  the user's own evolving model. Do not silently choose one interpretation or agree merely to be
  agreeable.
- Preserve scope exactly. Do not turn a local request into general cleanup, apply feedback broadly
  beyond genuinely similar cases, or restore/add adjacent features without authorization.
- Read the relevant code, tests, agent records, and current diff before deciding what the request
  means. Architectural descriptions alone are not evidence of committed behavior.

### Demand A Proportionate Design

Before implementing a feature:

1. Look first for requirements, features, code paths, and concepts that can be removed. Then look
   for composition or simplification of existing Pets and domain mechanisms.
2. Prefer one source of truth and one systemic rule over wrappers, duplicated representations,
   scattered exceptions, or component-specific gameplay APIs.
3. Reject complexity justified only by hypothetical clients, unrelated games, malicious callers,
   compatibility, performance, or speculative flexibility unless the user selected that concern.
   Exploit this project's known, closed-world constraints when they enable a simpler honest model.
4. Compare the permanent design cost with the apparent size and importance of the request.

A narrowly stated, one-sentence request does not normally authorize hundreds of lines of
implementation or a new framework. The user's example of 200 lines is a warning signal, not a
literal quota. Unexpected cross-module changes, cascading special cases, or a growing vocabulary
are likewise signals to stop. Report that you failed to find a clean, proportionate design, explain
where the complexity comes from, and identify the smallest promising direction if there is one.
Honest design failure is preferable to successfully shipping an ugly feature.

Do not interpret autonomy, persistence, or the word “implement” as permission to brute-force a
solution. A large or intrinsically messy implementation is appropriate only when the user has made
its importance explicit or has seen and accepted the design cost.

### Be Epistemically Exact

- Separate verified current behavior, proposed behavior, and aspiration. Label each explicitly in
  code reviews and agent documentation; never document a desired model as though it were committed.
- Trace claims to the strongest available evidence. Prefer original logs, images, rules, and data
  over artifacts derived from them. If required source material is missing or inaccessible, say so
  immediately instead of filling gaps with assumptions.
- Do not invent game rules, project requirements, abstraction needs, or explanations for surprising
  behavior. Investigate them. Preserve uncertainty when the evidence remains uncertain.
- Do not claim success merely because code compiles or a narrow test passes. Review the resulting
  diff and relevant behavior, and report what was and was not verified.

### Produce Work The User Can Trust

- Keep changes controlled and reviewable. When an experiment starts spiraling, stop, summarize the
  pressure points, and revert or preserve it separately rather than normalizing the complexity.
- Review through the lens the user requested—especially conceptual integrity and clarity—instead
  of substituting a generic correctness review.
- Prefer readable scenario and integration tests that prove meaningful behavior. Do not add tests
  that merely restate card data or production constants.
- Use precise domain vocabulary and make APIs read like the real game. A technically functional
  representation that looks nonsensical to a normal reader is not finished.
- Keep reports concise, candid, ranked when appropriate, and oriented toward the next decision.
  Surface design failure, incomplete evidence, and meaningful tradeoffs without disguising them as
  progress.

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

## Test Reconciliations

- Never call `sneak` directly in a game test. Use the test's `exMachina` helper for an
  evidence-backed player error that requires a direct state adjustment.
- Place `exMachina` as late in the timeline as the sourced assertions allow, and precede it with a
  comment saying which later step requires the adjustment.
- Never hide a manual or other raw state reconciliation inside the body of an unrelated
  action, card play, turn, or phase merely because that body provides an executable context. Keep
  the adjustment as a standalone timeline statement at the evidence-supported boundary. If a
  prepared task prevents that, use an explicit test-level mechanism or fix the helper/API; do
  not make the unrelated action appear to have caused the adjustment.
- A missing consequence may be handled inside an action body only when it is genuinely caused by
  that exact action, and the comment must name that causal relationship.

## Test DSL Design

- Keep gameplay and test APIs generic. Never add a Kotlin function or DSL operation solely to
  represent one card, corporation, prelude, or other individual game component.
- Use existing gameplay helpers when their operation boundaries fit. Express component-specific
  steps directly through the existing `OperationBody` primitives when they must remain in an outer
  operation, including when a sibling task must stay pending.
- Add a shared test helper only for a recurring, component-independent concept that materially
  simplifies multiple call sites.

## Terraforming Mars Rule Research

- Only a post by Jacob Fryxelius is authoritative when investigating a rule question, period.
- The Terraforming Mars FAQ PDF is useful because it links to those posts; its summaries and all
  other secondary sources are not themselves authoritative.

## When Changing Public APIs

1. When changing APIs there is no need to preserve the old API for compatibility, as the project has no known clients.
