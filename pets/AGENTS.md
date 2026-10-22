# Working on Pets

Keep the static model self-contained. Runtime state, task execution, player policy, and game
replays belong to Solarnet. Do not add those dependencies to solve a local modeling problem.

- Prefer removal and composition of existing declarations to new concepts or special cases.
- A small request requiring a new representation or broad API change needs a design discussion
  before implementation. Do not preserve obsolete APIs for hypothetical clients.
- Specifications describe intended behavior; passing tests alone do not establish that contract.
  Check the relevant language/type specification and its corresponding tests before repairs.
- Tests should demonstrate meaning. Identify known incorrect behavior explicitly, and track
  unresolved work in TODO.md.
- English realization stays game-neutral. Component-specific vocabulary belongs in supplied data.
- Keep at most one top-level class-like Kotlin declaration per file. Files without classes use
  lowerCamelCase names. Do not add extension functions without a specific reason.
- Keep shared `src/common`, `src/jvm`, and `src/js` trees, with matching `test/` trees.
  Module build files select their package directories; do not introduce per-module source trees.
  Share test source only when the fixture actually belongs to both consumers; never depend on
  Solarnet test support.
- Use `./gradlew :<module>:test` for focused verification, and `./gradlew build` for changes that
  affect the complete build or cross-module contracts. Preserve JVM and browser coverage.
- Review the entire final diff for unnecessary machinery, unrelated changes, lost test coverage,
  stale documentation, and behavior changes before reporting completion.

Use [docs/agents/README.md](docs/agents/README.md) to select focused documentation; do not read
the whole collection for a narrow task. Read its testing route before editing or running tests,
and apply its final review criteria before reporting completion.

See README.md for commands, modules, and specification links. Keep local human-facing artifacts in
`_local/` and temporary agent files under `$TMPDIR`. Do not commit unless asked.
