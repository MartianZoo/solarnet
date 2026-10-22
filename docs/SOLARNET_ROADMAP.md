# Solarnet roadmap

Solarnet is the runtime side of the planned repository split: Game World, the engine, Agents,
Terraforming Mars gameplay, recordings and replays, and applications that execute or
explore a game. The [Pets roadmap](PETS_ROADMAP.md) owns the static language and type model, Canon,
derived English and iconography, and the Almanac. Goals that require both static meaning and live
execution belong in both roadmaps, with each document describing its part and linking to the other.

This roadmap indexes priorities, status, and the owning notes in [`docs/agents/`](agents/README.md).
Detailed design decisions belong in those individual notes, including decisions not yet implemented.

The central runtime problem is not missing features. It is the accumulated machinery between
authored rules, the engine, gameplay helpers, and functional tests. Work is successful when a
player action follows a small, intelligible path through ordinary rules and general runtime
semantics. A new helper, stage, identity, or facade is progress only when it lets the project delete
more permanent machinery than it adds.

Sections and items are ordered by current best judgment, not by an agreed implementation schedule.
The owner and project still need a dedicated priority discussion. **Active** means substantial work
exists on a development branch or in a preserved experiment and is still intended for integration
after review. **Selected** means the direction is chosen while some design remains open.
**Exploratory** means the idea still has to justify its lasting conceptual cost.

## Internal design and game execution

[ENGINE_CONTRACT.md](agents/ENGINE_CONTRACT.md) owns client interaction requirements, distinguishing
selected direction from remaining proposals, including Admin handling. New interaction terminology
stays there while the contract is developed. [DECISION_GRAPH.md](agents/DECISION_GRAPH.md) collects
raw notes for future agent exploration and pruning; it is not a finished design.
[ORDINAL_REWORK.md](agents/ORDINAL_REWORK.md) proposes transaction grouping and checkpoint migration;
its implementation choices remain under review.
[TASK_HANDLING.md](agents/TASK_HANDLING.md) drafts the complete runtime task-handling inventory,
with timing and current differences from the interaction contract.

1. **Make actions, payments, delegated control, and completion a single intelligible lifecycle.**
   **Selected; active design.** Unify the lifecycle while preserving choice and Actor
   authority. Action identity now rejects absent action slots, including AI Central's nonexistent
   second action. See
   [action identity and invocation](../pets/docs/agents/ACTIONS.md),
   [turns and turn offers](../pets/docs/agents/GAME_FLOW.md#turns-and-turn-offers), including
   the Head Start variant, [on-turn identity and routing](agents/IDENTITY.md#who-is-on-turn-and-who-decides),
   and [deferred work](agents/SEQUENCING.md#deferred-work). Ordering delegated
   responses and their payments remains open; see [completion](agents/SEQUENCING.md#the-missing-rule-when-an-operation-is-over)
   and [payment gaps](agents/PAYMENTS.md#allocation-validation).

2. **Make the game advance through authored rules instead of a mirrored Kotlin workflow.**
   **Selected.** Phase order, player rotation, setup, expansion phases, final greenery, scoring, and
   victory should arise from live game components and ordinary engine scheduling.
   [Deferred work](agents/SEQUENCING.md#deferred-work) handles work within an operation, and
   `FINALLY` work, which waits for everything else including `LATER` work, carries the next turn or
   phase. The [game flow](../pets/docs/agents/GAME_FLOW.md#game-flow) is now authored this way and
   `TfmWorkflow` is deleted; accepted actions now use `ActionScope`. The `workflow` branch removed the
   coroutine-owned phase sequence but is not intended to land. It measured the cost of representing
   deferred work as components: per-rule mode guards, continuation components, and a compiler for
   expansion phase order. Preserve full-game replay evidence through the change.

3. **Keep Game World, engine, and Agent responsibilities exact.** **Selected.** Game World owns the
   passive record of a game: premise, components, pending tasks, history, queries, and navigable
   recordings. The engine decides and executes consequences. Actor-scoped Agents expose ordinary
   interaction through explicit task commands. Continue moving whole
   responsibilities in this direction, including a narrower client-facing Agent API and scoped
   reading, while avoiding paired adapters or a new all-purpose game wrapper.

4. **Restore a coherent card-tracking game-playing API.** **Selected.** Keep anonymous card counts
   and rule-relevant locations in Game World, while tracking known hand and played-card identities
   outside the engine. Turn the successful replay-test approach into a small production capability
   for normal named-card play. Clients submit named-card play through the same capability.
   Do not revive the former full real-card model, deck simulation, drafting, or hidden-information
   architecture as prerequisites.

5. **Give manual experimentation honest modes.** **Selected direction.** Purple plays the full
   game; blue grants a turn; green initiates an arbitrary operation; yellow permits deliberate task
   abandonment; red applies corrections without advancing gameplay. Make those promises real in
   the REPL and reusable by Mars Playground. Mode changes must handle existing work explicitly and
   must not smuggle a Kotlin game driver into application code.

## Code clarity and confidence

1. **Collapse the engine-to-functional-test stack.** **Active.** Ordinary scenarios should express
   player-visible setup and actions through the full game flow, not rebuild phases, relocate rule
   components, chase task causes, or rely on rendered instruction text. Keep focused lower-level
   tests where they prove an engine contract; delete gameplay conveniences that exist only to
   compensate for missing semantics.

2. **Preserve and deepen source-backed whole-game evidence.** Full replays are the strongest proof
   that independent rules compose. Keep original logs, screenshots, and corrections visible in the
   replay tests while keeping generated recordings compact. Finish the incomplete 2026-08-20 replay
   from its preserved evidence, retain the recovered final-greenery scenarios on `work1`, and use
   the preserved TFMBot games as additional integration evidence where their source decisions can
   be represented honestly.

3. **Make runtime contracts small, typed, and plainly named.** Consolidate the public surfaces of
   Game World, engine, Agent, Terraforming Mars gameplay, and script support after their ownership
   is clear. Remove obsolete aliases, reduce accidental visibility, install binary-API checks, and
   replace vague words such as “operation” or “gameplay command” with the actual lifecycle
   meant.
   Domain input should produce precise domain failures while implementation defects retain useful
   stack traces. There are no compatibility clients that justify preserving a worse API.

4. **Strengthen records and diagnostics without inventing another semantic model.** Stamp full
   application outputs with their producing revision and source-state hash. Preserve authored
   provenance where runtime transformation can report it cheaply, and add opt-in engine traces for
   difficult investigations. Rich causal explanation is useful when derived from existing events;
   it does not justify mirrored state, mandatory debug metadata, or a separate history ontology.

5. **Pursue performance only when it enables qualitatively different work.** Profile expensive
   type construction, limiting, and queries before redesigning them. Integrate measured branch work
   only when it remains coherent after current architectural changes. Small percentage gains,
   speculative caches, and micro-benchmarks do not earn roadmap priority.

## Game records and reconstruction

1. **Make full and faithful export/import round-trip.** **Active.** `work4` has a shared full/short
   JSON format, hashes of Catalog data and the resolved premise, state-only exact import, and
   Agent replay through recorded commands. Replay tests now emit both forms and compare
   component changes and pending work without requiring matching task IDs or event ordinals.
   Finish production card-input capture and replay with the uncommitted `work1` tracker, then
   connect Playground's upload path. A public-database ingester and friendlier discrepancy flow
   follow later. [Export design](agents/EXPORT.md) owns the details.

2. **Keep exact event recordings independently useful.** **Implemented foundation.** A recording
   must reconstruct components, pending tasks, history, and approved navigation points without an
   engine, firing effects, or needing the original live World. Continue treating this exact stream
   and the decision stream as separate views of the same game. The combined file permits passive
   playback and independent Agent reconstruction, then compares their results.

3. **Make game provenance durable.** Record enough premise and implementation identity to know
   which rules produced a game and to diagnose mismatches later. Saving and resuming an unfinished
   live game is not a priority; intermediate replay positions matter for verification and
   exploration, not as an accidental persistence product requirement.

## Applications that need Solarnet

1. **Make Mars Playground the showpiece.** **Selected.** Build the
   [interactive rules laboratory](PLAYGROUND.md) in Compose Multiplatform for desktop and tablet
   browsers. A user configures or directly constructs a scenario, manipulates cards, resource piles,
   tiles, and spaces on a zoomable tabletop, then enters play mode and steps through real pending
   tasks while watching the event log. Game World remains the source of game facts; presentation
   owns camera, windows, selection, and animation continuity. Begin with a complete tile
   interaction, a resource pile, and a card. History browsing belongs in the initial experience;
   scenario files and shareable URLs come later.

2. **Turn replay viewing and reconstruction into a compelling way to explore games.** Use Mars
   Playground to browse exact recordings, coherent history positions, useful queries, and trustworthy
   provenance. Pair it eventually with the compact decision format so a game can be read as both an
   exact evolving World and an explanatory sequence of choices. Broader causal analytics remain
   conditional on being derived cheaply from these foundations.

3. **Continue the dual-engine parity experiment.** **Active exploratory branch.** The
   `heroku-experiment` branch proves a Kotlin/JS facade that can accept semantic moves, drive a
   complete restricted game, and project a substantial Solarnet state snapshot. Continue toward
   shadowing accepted inputs from the open-source Terraforming Mars app and comparing both engines
   at stable points. The app should continue to own UI, persistence, shuffled cards, and hidden
   information; Solarnet should receive player intent, execute ordinary tasks, and expose
   disagreements. The app-side projector, comparator, transcript persistence, and broader move
   families remain before this becomes a complete parity proof.

4. **Keep REgo as the current experimental workbench.** The command-line interface remains the
   fastest place to inspect state, submit tasks, apply corrections, and test the color-mode model.
   Improve it where that exposes genuine runtime capabilities—especially task forms, mode
   integrity, and state exploration—without mistaking terminal polish or the low-priority `list`
   command for a major product program.

## Shared outcomes with Pets

- **Reach the single-source card trifecta.** Pets must provide correct modeled meaning, strong
  derived English, and strong iconography for roughly 300 cards; Solarnet must execute that same
  meaning correctly through realistic interactions and whole games. Neither side completes the
  milestone alone. See
  [Pets roadmap: Derived applications](PETS_ROADMAP.md#derived-applications-of-the-static-model).
- **Separate the Gradle builds before splitting Git repositories.** Solarnet will consume Pets
  libraries by artifact coordinates, with local composite substitution for combined development.
  The [repo-split next step](PETS_ROADMAP.md#independent-gradle-builds) owns the implementation
  scope and acceptance checks, including building an isolated Solarnet copy against freshly built
  Pets artifacts without Pets source. Developer comfort is a completion condition: prove explicit
  release/source selection, independent worktree pairings, IDE editing across both builds, and
  familiar combined checks and application startup before separating Git repositories. The same
  plan owns the lightweight release workflow and coordination of changes spanning both projects.
- **Let independent builders succeed.** Pets supplies an understandable semantic library;
  Solarnet supplies a trustworthy executable World and game-playing surface. Mars Playground,
  parity work, and future outside clients should pressure those contracts constructively without
  turning hypothetical consumers into frameworks.

## Deliberately outside this roadmap

- Individual card defects and old open issues belong in focused tests, issues, or `TODO.md` unless
  they reveal a systemic runtime flaw or invalidate a major project claim.
- A stash is evidence, not a commitment. Active development branches and explicitly preserved
  strategic experiments matter; abandoned implementation attempts do not become roadmap items.
- Full deck simulation, hidden-information security, drafting, and saving unfinished games are not
  prerequisites for the selected gameplay and export directions.
- Broad official-content coverage, support for every awkward card, and a polished owner-built
  player product remain lower priority than a small coherent model and the Playground showpiece.
- A separate optimized engine, comprehensive causal-analytics product, speculative support for
  unrelated games, and small performance improvements remain conditional.
- Pets-only syntax, type-system, Canon, English, iconography, and Almanac work belongs in
  [PETS_ROADMAP.md](PETS_ROADMAP.md), except where a shared outcome explicitly requires runtime
  work.

This roadmap synthesizes current priorities in
[`VALUES.md`](agents/VALUES.md), [`TODO.md`](../TODO.md), [`PLAYGROUND.md`](PLAYGROUND.md), the
focused runtime contracts under
`docs/agents/`, open issues, recent mainline work, the `workflow` branch, the exploratory
`heroku-experiment` branch, and preserved decision-import and TFMBot experiments.
