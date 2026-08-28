# Agent documentation router

Read this page after `AGENTS.md`, but do not read every linked document. Pick the route matching the
current task, read its “Read when” note and named sections, then inspect the linked source and tests.

## Authority labels

- **Current model:** a map of committed behavior. Source and tests win when details differ.
- **Working rule:** a design constraint for new work, even if current code diverges.
- **Proposal:** unimplemented or partial direction; never permission to implement the whole idea.
- **Audit:** known gaps or suspicious ownership. [`TODO.md`](../../TODO.md) decides priority.
- **Research:** preserved evidence and conclusions, not product behavior.
- **Procedure:** steps to follow for a particular kind of work.

## Choose a route

### Change game behavior or engine semantics

Read [`VALUES.md`](VALUES.md), then only the row matching the concept:

| Concept being changed | Read | Authority |
| --- | --- | --- |
| World construction, components, events, tasks, effects, rollback, or Gameplay | [`ENGINE.md`](ENGINE.md) | Current model |
| Task ordering, `THEN`, automatic effects, barriers, or completion | [`SEQUENCING.md`](SEQUENCING.md) | Working rules and audit |
| Actor attribution, task assignee, context owner, or delegated narrowing | [`IDENTITY.md`](IDENTITY.md) | Current model and proposal |
| Gain/removal/transmutation counts, AMAP, or abstract targets | [`QUANTIFIERS.md`](QUANTIFIERS.md) | Current engine contract |
| Action costs, invoices, or action identity | [`ACTIONS.md`](ACTIONS.md) | Current model |
| Payment excess, tender value, or attribution | [`PAYMENTS.md`](PAYMENTS.md) | Audit and proposal |
| Known deliberate game representations | The matching entry in [`GAME_HACKS.md`](GAME_HACKS.md) | Current model |
| Phase topology or replacing the Kotlin workflow | [`WORKFLOW.md`](WORKFLOW.md) | Domain rules and proposal |
| Optional client autoexecution mechanism | [`AUTOEXEC.md`](AUTOEXEC.md) | Working direction and audit |
| Proof that an automatic task command is safe | [`SMART_AUTOEXEC.md`](SMART_AUTOEXEC.md) | Research and proposal |

### Change Pets, types, or static game construction

| Concept being changed | Read | Authority |
| --- | --- | --- |
| Classes, Types, dependencies, refinements, implicit variables, or uninhabited types | The relevant numbered section of [`TYPES.md`](TYPES.md) | Current model |
| Class-property syntax, defaults, cardinality, or property groups | The matching section of [`PROPERTIES.md`](PROPERTIES.md) | Current model and working rules |
| Catalogs, Modules, Bundles, configuration, premise resolution, or projection policy | The matching section of [`OPTIONS.md`](OPTIONS.md) | Current model and working direction |
| Master Class identity versus game-filtered enumeration | [`CLASS_TABLES.md`](CLASS_TABLES.md) | Current model |
| Remaining implicit-variable defects | [`LINKAGES.md`](LINKAGES.md), after the relevant `TYPES.md` section | Focused audit |
| Proposed generic component fanout | [`EACHPLAYER.md`](EACHPLAYER.md) | Proposal |
| Eliminating a custom instruction | [`REDUCE_CUSTOM.md`](REDUCE_CUSTOM.md) | Audit |

### Change content, names, or human rendering

| Task | Read | Authority |
| --- | --- | --- |
| Add or change a card, corporation, rule component, or Pets declaration | [`NAMING.md`](NAMING.md), then topic-specific engine/type docs only as needed | Current vocabulary |
| Change English rendering, renderer architecture, or card layout | [`LANGUAGE.md`](LANGUAGE.md), selecting only the sections routed there | Current model, constraints, and prioritized design record |
| Change map diagrams or generated area declarations | [`MAP_PETS_GENERATION.md`](MAP_PETS_GENERATION.md) | Procedure |
| Change Prelude 2 scope or its unusual rules | [`PRELUDE2.md`](PRELUDE2.md) | Source and support record |
| Model Turmoil | Select only the relevant rule family in [`TURMOIL.md`](TURMOIL.md) | Research-backed proposal |
| Design shuffle/deal, hidden cards, or chance | Select the relevant gate in [`REAL_CARDS_MODE.md`](REAL_CARDS_MODE.md) | Proposal |

### Change project structure or APIs

| Task | Read | Authority |
| --- | --- | --- |
| Move generic versus Terraforming Mars responsibilities | The matching division in [`RESPONSIBILITIES.md`](RESPONSIBILITIES.md) | Audit |
| Flatten Gameplay or design a client interface | [`API.md`](API.md), after the Gameplay section of [`ENGINE.md`](ENGINE.md#current-gameplay-surface) | Proposal |
| Reduce Kotlin visibility | [`VISIBILITY.md`](VISIBILITY.md) | Working rules and procedure |

### Verify a change

Read [`TESTING.md`](TESTING.md). Read [`JVM_TEST_PERFORMANCE.md`](JVM_TEST_PERFORMANCE.md) only when
measuring or changing JVM test throughput; its measurements are a dated baseline, not routine setup.

### Reconstruct a game

- For a herokuapp archive, read [`HEROKUAPP_GAME_LOGS.md`](HEROKUAPP_GAME_LOGS.md).
- For a physical game record, read [`OTB_GAME_RECORDS.md`](OTB_GAME_RECORDS.md).
- For either, also read only “Game replay tests” and “Direct state reconciliation” in
  [`TESTING.md`](TESTING.md).

### Research AI play or optimal solo play

- Read [`AI_BACKGROUND_FOR_BOARD_GAMERS.md`](AI_BACKGROUND_FOR_BOARD_GAMERS.md) for the concise
  conceptual account; read [`AI_BACKGROUND.md`](AI_BACKGROUND.md) only when source-level AI research
  or architecture decisions are actually needed.
- Read [`OPTIMAL_SOLO.md`](OPTIMAL_SOLO.md) only for the TR63 monotonicity analysis or the associated
  report tool.

### Merge or synchronize branches

Read [`WORKTREES.md`](WORKTREES.md). It is mandatory for these operations and irrelevant otherwise.

## Maintain this collection

- Put prioritized work in [`TODO.md`](../../TODO.md); these documents must not become competing
  backlogs.
- Keep current behavior, proposed behavior, and desired rules visibly separate.
- Link to production source and meaningful tests instead of copying inventories that can drift.
- For a source location, give the file and a stable search string, never a line number.
- Update the smallest owning document and avoid repeating a rule in several places.
- Delete resolved audit and migration history unless it still explains a live constraint.
