# Agent documentation router

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

Read this page after `AGENTS.md`, but do not read every linked document. Pick the route matching the
current task, read its “Read when” note and named sections, then inspect the linked source and tests.

## Authority labels

- **Current model:** a map of committed behavior. Source and tests win when details differ.
- **Working rule:** a design constraint for new work, even if current code diverges.
- **Proposal:** unimplemented or partial direction; never permission to implement the whole idea.
- **Audit:** known gaps or suspicious ownership within the document's focused subject.
- **Research:** preserved evidence and conclusions, not product behavior.
- **Procedure:** steps to follow for a particular kind of work.

## Choose a route

### Change game behavior or engine semantics

Read [`VALUES.md`](VALUES.md), then only the row matching the concept:

| Concept being changed | Read | Authority |
| --- | --- | --- |
| World construction, components, tasks, effects, or Agent | [`ENGINE.md`](ENGINE.md) | Current model |
| Event history, recordings, checkpoints, or rollback | [`ENGINE.md`](ENGINE.md#events-and-timeline) | Current model |
| Task ordering, `THEN`, automatic effects, barriers, or completion | [`SEQUENCING.md`](SEQUENCING.md) | Working rules and audit |
| Admin, Actor attribution, task assignee, context owner, or delegated narrowing | [`IDENTITY.md`](IDENTITY.md) | Current model and selected direction |
| Gain/removal/transmutation counts, AMAP, or abstract targets | [`QUANTIFIERS.md`](QUANTIFIERS.md) | Current engine contract |
| Action costs, invoices, or action identity | [`ACTIONS.md`](ACTIONS.md) | Current model |
| Payment excess, tender value, or attribution | [`PAYMENTS.md`](PAYMENTS.md) | Audit and proposal |
| Known deliberate game representations | The matching entry in [`GAME_HACKS.md`](GAME_HACKS.md) | Current model |
| Phase topology or replacing the Kotlin workflow | [`WORKFLOW.md`](WORKFLOW.md) | Domain rules and proposal |
| Agent policies, shared autoexecution, or policy-relative stable points | [`AUTOEXEC.md`](AUTOEXEC.md) | Working direction and audit |
| Proof that an automatic task command is safe | [`SMART_AUTOEXEC.md`](SMART_AUTOEXEC.md) | Research and proposal |
| Runtime diagnostics, event metadata, or traces | [`DIAGNOSTICS.md`](DIAGNOSTICS.md) | Proposal and procedure |

### Change Pets, types, or static game construction

| Concept being changed | Read | Authority |
| --- | --- | --- |
| Classes, Types, dependencies, refinements, implicit variables, or uninhabited types | The relevant numbered section of [`TYPES.md`](TYPES.md) | Current model and type-variable working direction |
| Class-property syntax, defaults, cardinality, or property groups | The matching section of [`PROPERTIES.md`](PROPERTIES.md) | Current model and working rules |
| Catalogs, Modules, Bundles, configuration, premise resolution, or projection policy | The matching section of [`OPTIONS.md`](OPTIONS.md) | Current model and working direction |
| Master Class identity versus game-filtered enumeration | [`CLASS_TABLES.md`](CLASS_TABLES.md) | Current model |
| Proposed generic component fanout | [`EACHPLAYER.md`](EACHPLAYER.md) | Proposal |
| Eliminating a custom instruction | [`REDUCE_CUSTOM.md`](REDUCE_CUSTOM.md) | Audit |

### Change content, names, or human rendering

| Task | Read | Authority |
| --- | --- | --- |
| Add or change a card, corporation, rule component, or Pets declaration | [`NAMING.md`](NAMING.md), then topic-specific engine/type docs only as needed | Current vocabulary |
| Change English rendering, renderer architecture, or card layout | [`LANGUAGE.md`](LANGUAGE.md) | Durable goal, current understanding, and ordered direction |
| Change map diagrams or generated area declarations | [`MAP_PETS_GENERATION.md`](MAP_PETS_GENERATION.md) | Procedure |
| Model Turmoil | Select only the relevant rule family in [`TURMOIL.md`](TURMOIL.md) | Research-backed proposal |
| Design shuffle/deal, hidden cards, or chance | Select the relevant gate in [`REAL_CARDS_MODE.md`](REAL_CARDS_MODE.md) | Proposal |

### Change project structure or APIs

| Task | Read | Authority |
| --- | --- | --- |
| Move runtime layers or generic versus Terraforming Mars responsibilities | The matching division in [`RESPONSIBILITIES.md`](RESPONSIBILITIES.md) | Selected direction and audit |
| Extract the client Agent API, Agent-owned autoexecution policies, or the shared autoexecution loop | [`API.md`](API.md), [`AUTOEXEC.md`](AUTOEXEC.md), after the Agent section of [`ENGINE.md`](ENGINE.md#current-agent-surface) | Current divergence and selected direction |
| Reduce Kotlin visibility | [`VISIBILITY.md`](VISIBILITY.md) | Working rules and procedure |

### Verify a change

Read [`TESTING.md`](TESTING.md). Read [`JVM_TEST_PERFORMANCE.md`](JVM_TEST_PERFORMANCE.md) only when
measuring or changing JVM test throughput; its measurements are a dated baseline, not routine setup.

### Reconstruct a game

- For a herokuapp archive, read [`HEROKUAPP_GAME_LOGS.md`](HEROKUAPP_GAME_LOGS.md).
- For a physical game record, read [`OTB_GAME_RECORDS.md`](OTB_GAME_RECORDS.md).
- For either, also read only “Game replay tests” and “Direct state reconciliation” in
  [`TESTING.md`](TESTING.md).

### Research optimal solo play

- Read [`OPTIMAL_SOLO.md`](OPTIMAL_SOLO.md) only for the TR63 monotonicity analysis or the associated
  report tool.

## Maintain this collection

- Keep focused programs of work in the smallest owning document. Use [`TODO.md`](../../TODO.md)
  only for miscellaneous work not already covered by one of these focused plans.
- Keep current behavior, proposed behavior, and desired rules visibly separate.
- Link to production source and meaningful tests instead of copying inventories that can drift.
- For a source location, give the file and a stable search string, never a line number.
- Update the smallest owning document and avoid repeating a rule in several places.
- Delete resolved audit and migration history unless it still explains a live constraint. A decision
  to keep a cost is such a constraint: record it, with its reasoning, in the owning document and
  index it under “Dispositioned complexity findings” in [`VALUES.md`](VALUES.md) so later reviews do
  not rediscover it.
