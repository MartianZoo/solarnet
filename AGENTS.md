# Project instructions for Codex

## Start with the task, not the handbook

Do not read `docs/agents/` wholesale. Use
[`docs/agents/README.md`](docs/agents/README.md) to select only the documents and sections triggered
by the current task. Then inspect the named source and tests; agent documents are maps and design
records, not stronger evidence than committed behavior.

Human documentation lives directly in `docs/`. Read it freely, but propose at most slight
correctness edits unless the user explicitly asks for more. Agent-maintained documentation lives in
`docs/agents/`; when a change makes one inaccurate, update the smallest owning document selected by
the router.

## Always apply these principles

### Interpret the request before acting

- A question, speculation, or design sketch is not an implementation request.
- Treat a proposed mechanism as a hypothesis. Identify the underlying goal and recommend a smaller,
  cleaner mechanism when one exists.
- Call out contradictions, false premises, and reversals rather than silently selecting an
  interpretation.
- Preserve scope. Do not turn a local request into general cleanup or restore adjacent behavior
  without authorization.

### Protect the design

Solarnet seeks the smallest coherent set of rules from which the real game follows. Correct behavior
is necessary, but an implementation that merely works can still be a design failure.

- Judge cost primarily by permanent conceptual complexity: new abstractions, APIs, layers,
  exceptions, representations, and interactions.
- Look first for something that can be removed, then for composition of existing Pets and domain
  mechanisms.
- Prefer one source of truth and one systemic rule over wrappers, duplicated representations, and
  component-specific gameplay APIs.
- Do not build for hypothetical clients, unrelated games, malicious callers, compatibility, or
  speculative flexibility unless the user selected that concern.
- Stop when a narrow request unexpectedly requires broad cross-module changes or a growing
  vocabulary. Report the design pressure and the smallest promising direction instead of
  normalizing disproportionate complexity.
- Use precise domain language. An API that reads unlike the real game is not finished.

### Try the simpler approach first

Optimize for the smallest coherent design, not the first implementation that passes tests.
Simplicity is an acceptance criterion, not a later cleanup step.

- State the intended invariant in one sentence before editing.
- Begin by looking for code or concepts that can be removed, then try to compose existing
  mechanisms before adding another one.
- Set a complexity budget appropriate to the request. Treat a disproportionate diff as evidence
  against the approach even when tests pass.
- Treat a second representation of the same fact as a signal to stop and reconsider the design.
- Pause before introducing a new processing phase, representation, or cross-module protocol.
  Report the design pressure and propose the smallest promising alternative before proceeding.
- Review permanent conceptual cost and diff size before calling the work successful.
- If no clean implementation is evident, leave the TODO unresolved and explain why rather than
  hiding the uncertainty under compensating machinery.

Read [`docs/agents/VALUES.md`](docs/agents/VALUES.md) only when designing, implementing, or reviewing
a behavior or architecture change.

### Be exact about evidence

- Separate verified current behavior, proposed behavior, and aspiration.
- Prefer original logs, images, rules, and data over artifacts derived from them.
- Do not invent game rules, requirements, abstractions, or explanations for surprising behavior.
- Passing compilation or one narrow test is not enough evidence of success. Review the resulting
  diff and verify the relevant behavior.
- Prefer readable scenario and integration tests that prove meaningful behavior. Do not add tests
  that merely restate production data or constants.

## Read these only when triggered

| Circumstance | Required route |
| --- | --- |
| Editing or running tests, Gradle, formatting, or benchmarks | [`docs/agents/TESTING.md`](docs/agents/TESTING.md) |
| Reconstructing a digital or physical game | The appropriate replay guide selected by [`docs/agents/README.md`](docs/agents/README.md#reconstruct-a-game) |
| The user explicitly asks for Terraforming Mars rule research | Only a post by Jacob Fryxelius is authoritative for a disputed ruling. The FAQ PDF is useful only as an index to those posts. Do not initiate rule research merely because a task touches game behavior. |
| Changing a public API | Preserve no obsolete API for compatibility; there are no known clients. Also read the API/model route in the handbook. |
| Merging branches or synchronizing work | [`docs/agents/WORKTREES.md`](docs/agents/WORKTREES.md) |
