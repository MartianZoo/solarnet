# Notes for future introductions

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** planning introductory material for programmers who know *Terraforming Mars*, or
> choosing topics and ordering for that material. Read only the numbered topic group being planned.
>
> **Skip when:** looking for a current introduction to Pets or Solarnet; this is an editorial
> backlog, not published human documentation.

This is not an introduction to Pets or Solarnet. It holds notes for creating better introductory
material later. The intended reader of that material knows programming and *Terraforming Mars* well,
but knows nothing about these projects.

The headings follow the planned repository split: Pets, Canon, and Almanac in a static-model
repository; state, engine, agent, script, and REPL in a runtime repository. The split has not yet
happened. [`RESPONSIBILITIES.md`](RESPONSIBILITIES.md#prepare-the-petssolarnet-repository-split)
owns the agreed preparation requirements and current implementation status. Each numbered item
identifies a focused subject that could support a short article. The order ranks what seems most
useful for this reader to remember, not the order in which articles must appear. Before publication,
check claims against the current code and distinguish implemented behavior from goals.

## Pets, Canon, and Almanac

1. Pets as a precise specification language for *Terraforming Mars* rules.
2. The relationship between Pets notation and the game's printed icon language.
3. Canonical rules as inspectable data without a game state or card execution.
4. The single-source goal for execution, English wording, icons, and analysis.
5. Instructions, requirements, metrics, effects, and actions as a shared rule vocabulary.
6. Classes and typed dependencies for ownership, attachment, location, and targets.
7. Refinements and type variables for legal choices and context-dependent meaning.
8. Quantities, alternatives, and abstract targets specified without choosing them.
9. Class literals and rules that inspect kinds of components.
10. Published cards as compositions of general mechanisms rather than special cases.
11. The same declarations describing cards, maps, milestones, awards, and global rules.
12. Cataloged content, modules, and the universe selected for a game.
13. Fan cards made by composing existing mechanics.
14. Language and type specifications checked by conformance tests.
15. Almanac's searchable class relationships and semantic usage index.

## Solarnet

1. A standalone rules library for legal moves and their consequences.
2. Executing Pets declarations without card-specific gameplay code.
3. Card interactions emerging from general component and effect rules.
4. Game state as typed components, counts, and pending tasks.
5. Abilities attached to live components and removed with them.
6. Player choices represented as tasks that narrow toward concrete decisions.
7. Triggered effects, optional responses, and sequencing as general semantics.
8. Source-backed full-game replays as evidence for the whole model.
9. Follow mode with printed card identities supplied externally.
10. Exact event history, atomic rollback, and passive playback.
11. Distinct roles for ownership, agency, assignment, choice, and attribution.
12. Dependencies and invariants that maintain a coherent world.
13. Agent-facing play and selectable automatic decision policies.
14. *Terraforming Mars* setup, phase, and turn rules composed with the engine.
15. Script and REPL access to the same rules engine.
