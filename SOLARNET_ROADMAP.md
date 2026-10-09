# Solarnet roadmap

Solarnet is the runtime side of the planned repository split: Game World, the engine, Agents,
Terraforming Mars gameplay and workflow, recordings and replays, and applications that execute or
explore a game. The [Pets roadmap](PETS_ROADMAP.md) owns the static language and type model, Canon,
derived English and iconography, and the Almanac. Goals that require both static meaning and live
execution belong in both roadmaps, with each document describing its part and linking to the other.

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

1. **Make actions, payments, delegated control, and completion a single intelligible lifecycle.**
   **Selected; active design.** A player should be able to initiate an action, another player should
   be able to control a decision and its payment, and the original player should resume only after
   the relevant work and cleanup finish. Today action identity, billing, payment offers, task
   assignment, helper searches, and whole-World idleness divide that story across several systems.
   Replace this with the smallest general rule that preserves choice, rollback, and Actor authority.
   Nested priority groups and exclusive operation scopes remain alternatives rather than settled
   architecture.

2. **Make the game advance through authored rules instead of a mirrored Kotlin workflow.**
   **Active.** Phase order, player rotation, setup, expansion phases, final greenery, scoring, and
   victory should arise from live game components and ordinary engine scheduling. The `workflow`
   branch is substantial work intended to land: it moves phase transitions into Pets, compiles
   expansion-owned topology, removes the coroutine-owned phase sequence, and migrates functional
   tests to the automatic game. Finish it by integrating the coherent rule across affected phases,
   deleting superseded orchestration, and preserving full-game replay evidence.

3. **Keep Game World, engine, and Agent responsibilities exact.** **Selected.** Game World owns the
   passive record of a game: premise, components, pending tasks, history, queries, and navigable
   recordings. The engine decides and executes consequences. Actor-scoped Agents expose ordinary
   interaction and optional policies without teaching the engine strategy. Continue moving whole
   responsibilities in this direction, including a narrower client-facing Agent API and scoped
   reading, while avoiding paired adapters or a new all-purpose game wrapper.

4. **Restore a coherent card-tracking game-playing API.** **Selected.** Keep anonymous card counts
   and rule-relevant locations in Game World, while tracking known hand and played-card identities
   outside the engine. Turn the successful replay-test approach into a small production capability
   for normal named-card play. Human-directed and autonomous play must use the same capability.
   Do not revive the former full real-card model, deck simulation, drafting, or hidden-information
   architecture as prerequisites.

5. **Make explicit and automatic play use the same legal commands.** **Selected.** A human, replay,
   simple policy, or future autonomous player should inspect the same tasks, form choices, and
   submit the same Actor-authorized commands. Agent policy may choose among legal actions; it must
   never provide missing ordering, control, or completion semantics. Finish task-form support where
   real interactions need it, make policies attachable and composable only as experience demands,
   and keep proof-oriented automation distinct from strategy.

6. **Give manual experimentation honest modes.** **Selected direction.** Purple plays the full
   game; blue grants a turn; green initiates an arbitrary operation; yellow permits deliberate task
   abandonment; red applies corrections without advancing gameplay. Make those promises real in
   the REPL and reusable by Mars Playground. Mode changes must handle existing work explicitly and
   must not smuggle a second workflow into application code.

## Code clarity and confidence

1. **Collapse the engine-to-functional-test stack.** **Active.** Ordinary scenarios should express
   player-visible setup and actions through the full workflow, not rebuild phases, relocate rule
   components, chase task causes, or rely on rendered instruction text. The `workflow` branch's
   large test migration is valuable chiefly because it exposes and removes those alternate paths.
   Keep focused lower-level tests where they prove an engine contract; delete gameplay conveniences
   that exist only to compensate for missing semantics.

2. **Preserve and deepen source-backed whole-game evidence.** Full replays are the strongest proof
   that independent rules compose. Keep original logs, screenshots, and corrections visible in the
   replay tests while keeping viewer recordings compact. Finish the incomplete 2026-08-20 replay
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

1. **Make decision export and import faithful and clean.** **Selected; preserved experiment.** The
   desired small file records what Actors decided or supplied from outside the rules, without task
   ids or incidental engine choreography, and can reconstruct the same game using the same
   declarations and implementation. The earlier round-trip experiment established useful syntax
   and failure cases but is not finished: card identities, workflow start, backtracking across
   ambiguous task matches, Admin scheduling, and several replay divergences remain. Resume that
   work rather than replacing it with consequences disguised as decisions.

2. **Keep exact event recordings independently useful.** **Implemented foundation.** A recording
   must reconstruct components, pending tasks, history, and approved navigation points without an
   engine, firing effects, or needing the original live World. Continue treating this exact stream
   and the decision stream as separate views of the same game. A future combined file should allow
   passive playback and independent engine reconstruction, then compare their results rather than
   letting either stream repair the other.

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

2. **Enable complete human and autonomous play through the same capabilities.** **Selected
   direction.** End-to-end solo and multiplayer operation is desirable, including computer-driven
   play, but it must not split into separate gameplay architectures. Card tracking, task forms,
   Agent policies, workflow, and decision records should compose into a usable game-playing surface.
   The preserved TFMBot replay work is evidence and a future corpus, not permission to create a
   special bot engine or to confuse eager execution with strategy.

3. **Turn replay viewing and reconstruction into a compelling way to explore games.** Extend the
   existing engine-free viewer around exact recordings, coherent history positions, useful queries,
   and trustworthy provenance. Pair it eventually with the compact decision format so a game can
   be read as both an exact evolving World and an explanatory sequence of choices. Broader causal
   analytics remain conditional on being derived cheaply from these foundations.

4. **Continue the dual-engine parity experiment.** **Active exploratory branch.** The
   `heroku-experiment` branch proves a Kotlin/JS facade that can accept semantic moves, drive a
   complete restricted game, and project a substantial Solarnet state snapshot. Continue toward
   shadowing accepted inputs from the open-source Terraforming Mars app and comparing both engines
   at stable points. The app should continue to own UI, persistence, shuffled cards, and hidden
   information; Solarnet should receive player intent, execute ordinary tasks, and expose
   disagreements. The app-side projector, comparator, transcript persistence, and broader move
   families remain before this becomes a complete parity proof.

5. **Keep REgo as the immediate experimental workbench.** The command-line interface remains the
   fastest place to inspect state, submit tasks, apply corrections, and test the color-mode model.
   Improve it where that exposes genuine runtime capabilities—especially task forms, mode
   integrity, and state exploration—without mistaking terminal polish or the low-priority `list`
   command for a major product program.

6. **Retain optimal-solo analysis as later research.** **Exploratory.** The TR63 monotonicity work
   shows how Solarnet's executable rules and closed catalog could support principled search and
   conditional proofs. Preserve the research and pursue it when the runtime can expose a tractable
   decision surface without a separate optimized engine. Large-scale strategy research remains
   below the design, Playground, gameplay, and reconstruction programs.

## Shared outcomes with Pets

- **Reach the single-source card trifecta.** Pets must provide correct modeled meaning, strong
  derived English, and strong iconography for roughly 300 cards; Solarnet must execute that same
  meaning correctly through realistic interactions and whole games. Neither side completes the
  milestone alone. See
  [Pets roadmap: Derived applications](PETS_ROADMAP.md#derived-applications-of-the-static-model).
- **Complete the static/runtime separation.** Pets should build and explain declarations without
  runtime state. Solarnet should consume the resulting model through narrow capabilities and keep
  Catalog assembly, Game Premise, Game World, engine, and Agent ownership clear. Cross-repository
  work should remove reverse dependencies rather than create matching adapters.
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
[`VALUES.md`](docs/agents/VALUES.md), [`PLANS.md`](docs/agents/PLANS.md),
[`TODO.md`](TODO.md), [`PLAYGROUND.md`](PLAYGROUND.md), the runtime design records under
`docs/agents/`, open issues, recent mainline work, the active `workflow` branch, the exploratory
`heroku-experiment` branch, and preserved decision-import and TFMBot experiments.
