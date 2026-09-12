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

## Repeated-failure alerts

These are routing alarms, not substitutes for the owning documents. If the tempting next step
matches a row, read the linked section before editing.

| Tempting next step | Required response | Read |
| --- | --- | --- |
| Broaden a narrow request, add a second representation, or introduce vocabulary across modules | Stop and report the design pressure and smallest promising direction. | [`AGENTS.md`](../../AGENTS.md#try-the-simpler-approach-first), [`VALUES.md`](VALUES.md#keep-concepts-few-and-ownership-precise) |
| Add custom Kotlin, a custom instruction, or component-specific machinery to implement one card or rule | First try removal and composition of ordinary Pets. Custom code is evidence of a missing general capability, not the normal fallback. | [`VALUES.md`](VALUES.md#keep-pets-central) |
| Add a `TfmGameplay` operation or make one repair state, ordering, or task identity | Keep helpers as recurring player-facing syntax; repair the owning Pets or engine rule instead. | [`TESTING.md`](TESTING.md#test-design), [`SEQUENCING.md`](SEQUENCING.md#the-missing-rule-when-an-operation-is-over) |
| Make a card or rule test inspect task text, causes, queue order, or mirrored Canon data | Exercise player-facing actions and assert observable results. | [`TESTING.md`](TESTING.md#test-design) |
| Replace a result expectation with broad absolute-state assertions around an action | Use `.expect()` for the action's interesting partial net delta; reserve absolute assertions for sourced checkpoints. | [`TESTING.md`](TESTING.md#expectations) |
| Use `EAGER` or another autoexecution policy to make a test or replay proceed | Treat `EAGER` as a strategic choice, not settlement or test infrastructure. | [`AUTOEXEC.md`](AUTOEXEC.md#choice-safety-check) |
| Add `THEN`, `::`, a latch, priority, or pre-pruning | Begin with no extra ordering and identify the illegal committed result the new order prevents. | [`SEQUENCING.md`](SEQUENCING.md#before-adding-order) |
| Infer control, assignment, narrowing, attribution, or `Owner` from another identity role | Name all six roles independently. | [`IDENTITY.md`](IDENTITY.md#six-identities) |
| Give `Class` or `Type` a path to game-specific downward enumeration, or rebuild stable master facts for each premise | Pass the game universe explicitly and preserve its reusable master table. | [`CLASS_TABLES.md`](CLASS_TABLES.md#selected-replacement-master-tables-premise-tables-and-class-universes) |
| Add an explicit quantifier because a repeated effect changed the wrong count | Inspect the changed Class's defaults and the number of matching effect activations first. | [`QUANTIFIERS.md`](QUANTIFIERS.md#before-writing-an-explicit-quantifier) |
| Characterize behavior known to be wrong as an ordinary rule or accepted hack | Put a passing observable characterization in `BugsTest`; move it when fixed. | [`TESTING.md`](TESTING.md#known-defect-tests) |
| Add replay assertions, corrections, transcript prose, or gameviewer source | Return to original evidence; keep corrections visible and viewer recordings compact. | [`TESTING.md`](TESTING.md#game-replay-tests), the routed replay guide |
| Record migration history or agent reasoning in evergreen documentation | State the stable current model in the smallest owning document. | [Maintain this collection](#maintain-this-collection) |

## Choose a route

### Change game behavior or engine semantics

Read [`VALUES.md`](VALUES.md), then only the row matching the concept:

| Concept being changed | Read | Authority |
| --- | --- | --- |
| Game World ownership, passive component/task data, recordings, exports, or playback | [`GAMEWORLD.md`](GAMEWORLD.md) | Selected direction |
| Current World construction, components, tasks, effects, or Agent | [`ENGINE.md`](ENGINE.md) | Current model |
| Current live event, transaction, checkpoint, or rollback implementation | [`ENGINE.md`](ENGINE.md#events-and-timeline) | Current model |
| Task ordering, `THEN`, automatic effects, barriers, or completion | [`SEQUENCING.md`](SEQUENCING.md) | Working rules and selected direction |
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
| Classes, Types, dependencies, refinements, implicit variables, or uninhabited types | The cited rule of [`type-system-spec.md`](../type-system-spec.md) | Specification, checked rule-by-rule by `pets/types/Spec*Test.kt` |
| Pets syntax, declarations, instructions, effects, actions, narrowing, owner-local Classes, or elaboration | The cited rule of [`pets-language-spec.md`](../pets-language-spec.md) | Specification, checked rule-by-rule by `pets/Lang*Test.kt` |
| Type-variable lifetime in the engine | The matching section of [`TYPES.md`](TYPES.md) | Current model and working direction |
| Class-property syntax, defaults, cardinality, or property groups | The matching section of [`PROPERTIES.md`](PROPERTIES.md) | Current model and working rules |
| Catalogs, Modules, Bundles, configuration, premise resolution, or projection policy | The matching section of [`OPTIONS.md`](OPTIONS.md) | Current model and working direction |
| Master Class identity versus game-filtered enumeration | [`CLASS_TABLES.md`](CLASS_TABLES.md) | Current model |
| Generic component fanout (`EACH`) | [`EACH.md`](EACH.md) | Current model |

### Change content, names, or human rendering

| Task | Read | Authority |
| --- | --- | --- |
| Add or change a card, corporation, rule component, or Pets declaration | [`NAMING.md`](NAMING.md), then topic-specific engine/type docs only as needed | Current vocabulary |
| Change map diagrams or generated area declarations | [`MAP_PETS_GENERATION.md`](MAP_PETS_GENERATION.md) | Procedure |
| Design shuffle/deal, hidden cards, or chance | Select the relevant gate in [`REAL_CARDS_MODE.md`](REAL_CARDS_MODE.md) | Proposal |

### Change project structure or APIs

| Task | Read | Authority |
| --- | --- | --- |
| Extract the Game World or remove engine code from recording playback | [`GAMEWORLD.md`](GAMEWORLD.md) | Selected direction |
| Move runtime layers or generic versus Terraforming Mars responsibilities | The matching division in [`RESPONSIBILITIES.md`](RESPONSIBILITIES.md) | Selected direction and audit |
| Extract the client Agent API, Agent-owned autoexecution policies, or the shared autoexecution loop | [`API.md`](API.md), [`AUTOEXEC.md`](AUTOEXEC.md), after the Agent section of [`ENGINE.md`](ENGINE.md#current-agent-surface) | Current divergence and selected direction |
| Reduce Kotlin visibility | [`VISIBILITY.md`](VISIBILITY.md) | Working rules and procedure |

### Verify a change

Read [`TESTING.md`](TESTING.md). Read [`JVM_TEST_PERFORMANCE.md`](JVM_TEST_PERFORMANCE.md) only when
measuring or changing JVM test throughput; its measurements are a dated baseline, not routine setup.

### Review or finish a change

Before reporting an implementation complete or staging it for commit, apply
[`REVIEW_CRITERIA.md`](REVIEW_CRITERIA.md) to the entire final diff. This review is mandatory even
when the relevant design and testing routes were followed earlier.

### Reconstruct a game

- For a herokuapp archive, read [`HEROKUAPP_GAME_LOGS.md`](HEROKUAPP_GAME_LOGS.md).
- For a physical game record, read [`OTB_GAME_RECORDS.md`](../../_local/OTB_GAME_RECORDS.md).
- For either, also read only “Game replay tests” and “Direct state reconciliation” in
  [`TESTING.md`](TESTING.md).
- The replay test owns source evidence, commentary, and assertions. Keep any derived gameviewer
  recording compact.

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
