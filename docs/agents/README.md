# Agent handbook

Read this page before using the rest of `docs/agents`. These documents have different authority:

- **Current model** describes committed behavior. Check the code and tests when a claim matters to a
  change; the document is a map, not stronger evidence than the implementation.
- **Working rules** are design constraints for new work. Do not casually violate them even when the
  current implementation does.
- **Proposal** describes unimplemented or partially implemented work. It is context for a design
  discussion, never permission to implement the whole proposal.
- **Audit** records known gaps. [`TODO.md`](../../TODO.md) decides priority.
- **Research note** preserves conclusions and sources; it does not specify product behavior.

## Start here

| Need | Document | Status |
| --- | --- | --- |
| Project priorities and review standard | [VALUES.md](VALUES.md) | Working rules |
| Current Game World and execution architecture | [ENGINE.md](ENGINE.md) | Current model |
| Pets types, dependencies, refinements, and variables | [TYPES.md](TYPES.md) | Current model, with divergences |
| Class-property values, cardinality, defaults, and groups | [PROPERTIES.md](PROPERTIES.md) | Current model plus working rules and proposals |
| Authorities, Modules, configuration, and premises | [OPTIONS.md](OPTIONS.md) | Current model |
| Task ordering, completion, `THEN`, automatic effects, and barriers | [SEQUENCING.md](SEQUENCING.md) | Working rules and audit |
| Pets `Action` costs and invoices | [ACTIONS.md](ACTIONS.md) | Current model |
| Commands and test design | [TESTING.md](TESTING.md) | Current procedure |
| Reconstructing herokuapp game logs | [HEROKUAPP_GAME_LOGS.md](HEROKUAPP_GAME_LOGS.md) | Current procedure |
| Reconstructing recorded physical games | [OTB_GAME_RECORDS.md](OTB_GAME_RECORDS.md) | Current procedure |
| Class Names, display names, and vocabulary | [NAMING.md](NAMING.md) | Current model |
| English card-text derivation | [LANGUAGE.md](LANGUAGE.md) | Working rules |

## Read only when the task touches the area

| Area | Document | Status |
| --- | --- | --- |
| Actor, assignee, context owner, and future delegation | [IDENTITY.md](IDENTITY.md) | Current model plus proposal |
| Component fanout through proposed `EACH` syntax | [EACHPLAYER.md](EACHPLAYER.md) | Proposal |
| Flattening the engine workhorse and later client boundary | [API.md](API.md) | Proposal |
| Autoexecution policy boundary, proofs, provenance, and performance | [AUTOEXEC.md](AUTOEXEC.md) | Settled design direction plus audit |
| Authority-wide Class identity and game-filtered views | [CLASS_TABLES.md](CLASS_TABLES.md) | Current model |
| Generic/Terraforming Mars package seams | [BOUNDARIES.md](BOUNDARIES.md) | Audit |
| Native Pets-driven phase workflow | [WORKFLOW.md](WORKFLOW.md) | Proposal with settled game requirements |
| Remaining implicit-variable work | [LINKAGES.md](LINKAGES.md) | Focused implementation note |
| Integrating the `work1` AMAP model with `wildtag` | [WILDTAG_INTEGRATION.md](WILDTAG_INTEGRATION.md) | Temporary branch integration record |
| Candidates for eliminating custom instructions | [REDUCE_CUSTOM.md](REDUCE_CUSTOM.md) | Audit |
| Shuffle, physical cards, hidden information, and chance | [REAL_CARDS_MODE.md](REAL_CARDS_MODE.md) | Proposal |
| Monotonicity in optimal TR63 solo play | [OPTIMAL_SOLO.md](OPTIMAL_SOLO.md) | Research note |
| AI-player research and supporting literature | [AI_BACKGROUND.md](AI_BACKGROUND.md) and [board-gamer overview](AI_BACKGROUND_FOR_BOARD_GAMERS.md) | Research notes |

## Editing discipline

- Keep current behavior, proposed behavior, and desired rules visibly separate.
- Put prioritized work in [`TODO.md`](../../TODO.md); do not turn these documents into competing
  backlogs.
- Prefer links to production data and tests over copied inventories that can drift.
- When behavior changes, update the smallest owning document. Avoid repeating the same rule in
  several files.
- Delete resolved audit history unless it still explains a live constraint.
