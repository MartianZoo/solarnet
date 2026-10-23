# Retired documentation passages

> **Archaeology only.** This document preserves a mix of how things were and how we wanted
> them to be around **October 22, 2026**. It is not current guidance, an implementation
> contract, or a list of work to resume. Earlier status labels and instructions below are
> historical quotations, not present requirements.

These passages were removed from active documentation during the October 22, 2026 reset.
They are grouped by original source. Their wording is preserved; headings and relative links
have been adjusted to combine the fragments. The seven complete historical documents remain
separate and are listed in [the archive index](README.md).

- [AGENTS.md](#source-agents-md)
- [TODO.md](#source-todo-md)
- [docs/PETS_ROADMAP.md](#source-docs-pets-roadmap-md)
- [docs/PLAYGROUND.md](#source-docs-playground-md)
- [docs/README.md](#source-docs-readme-md)
- [docs/SOLARNET_ROADMAP.md](#source-docs-solarnet-roadmap-md)
- [docs/agents/ADVERSARIAL.md](#source-docs-agents-adversarial-md)
- [docs/agents/CARD_HANDLING.md](#source-docs-agents-card-handling-md)
- [docs/agents/ENGINE.md](#source-docs-agents-engine-md)
- [docs/agents/EXPORT.md](#source-docs-agents-export-md)
- [docs/agents/EX_MACHINA.md](#source-docs-agents-ex-machina-md)
- [docs/agents/HEROKUAPP_GAME_LOGS.md](#source-docs-agents-herokuapp-game-logs-md)
- [docs/agents/IDENTITY.md](#source-docs-agents-identity-md)
- [docs/agents/MAKE_TYPES_TYPES_AGAIN.md](#source-docs-agents-make-types-types-again-md)
- [docs/agents/PAYMENTS.md](#source-docs-agents-payments-md)
- [docs/agents/QUANTIFIERS.md](#source-docs-agents-quantifiers-md)
- [docs/agents/README.md](#source-docs-agents-readme-md)
- [docs/agents/REVIEW_CRITERIA.md](#source-docs-agents-review-criteria-md)
- [docs/agents/SEQUENCING.md](#source-docs-agents-sequencing-md)
- [docs/agents/SPEC_FIDELITY.md](#source-docs-agents-spec-fidelity-md)
- [docs/agents/TESTING.md](#source-docs-agents-testing-md)
- [docs/agents/automatic-effects-audit.tsv](#source-docs-agents-automatic-effects-audit-tsv)
- [docs/glossary.md](#source-docs-glossary-md)
- [pets/AGENTS.md](#source-pets-agents-md)
- [pets/TODO.md](#source-pets-todo-md)
- [pets/docs/README.md](#source-pets-docs-readme-md)
- [pets/docs/agents/GAME_HACKS.md](#source-pets-docs-agents-game-hacks-md)
- [pets/docs/agents/PAYMENTS.md](#source-pets-docs-agents-payments-md)
- [pets/docs/agents/README.md](#source-pets-docs-agents-readme-md)
- [pets/docs/agents/REVIEW_CRITERIA.md](#source-pets-docs-agents-review-criteria-md)
- [pets/docs/agents/TESTING.md](#source-pets-docs-agents-testing-md)
- [pets/docs/agents/VALUES.md](#source-pets-docs-agents-values-md)
- [pets/docs/pets-tricks-hacks-cheats.md](#source-pets-docs-pets-tricks-hacks-cheats-md)
- [pets/docs/type-system.md](#source-pets-docs-type-system-md)

<a id="source-agents-md"></a>

## AGENTS.md

Original source: `AGENTS.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-agents-md--project-instructions-for-codex"></a>

### Project instructions for Codex

<a id="source-agents-md--always-apply-these-principles"></a>

#### Always apply these principles

<a id="source-agents-md--decision-tree-pruning-belongs-to-the-players-agent-or-client"></a>

##### Decision-tree pruning belongs to the player's agent or client

**Universal design assumption:** When modeling Pets or making engine decisions, assume either
(a) the player's agent or client diligently prunes the decision tree and presents only choices
known not to dead-end the turn, or (b) we do not care about exposing dead ends. Neither the engine
nor the model is responsible for making every offered choice lead to a successful turn. This
principle applies even when no such pruner has been implemented.

**Protected Habitats is a valid model:** an opponent can attempt to take a protected plant; the
card's consequence dead-ends that path, requiring rollback. There is nothing wrong with this.
The model need not prevent the initial choice or reject the removal before its consequences run.

Do not report a defect, add a TODO, or introduce engine/model machinery merely because a choice
can fail later, requires lookahead, or makes the caller roll back. A defect claim needs a separate
rule failure, such as allowing the prohibited theft to survive a successfully completed turn or
excluding a required legal outcome. Enforcing game rules remains the engine/model's job; deciding
which unsuccessful paths to hide belongs to the agent or client.
See [ENGINE.md](#source-docs-agents-engine-md--queries-invariants-and-dead-ends).

<a id="source-todo-md"></a>

## TODO.md

Original source: `TODO.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-todo-md--todo"></a>

### TODO

- Decide whether to add shallow/deep ELSE or instruction-level `(A)*`; candidate uses and
  restrictions are in [ELSE.md](pets/ELSE.md).

- Separate task selection from execution when automatic execution is disabled.

- Investigate permanently locking Admin to greedy `EAGER` execution, as selected in
  [SMART_AUTOEXEC.md](SMART_AUTOEXEC.md). Trace replay/import and tests that currently
  disable Admin autoexecution before changing the Agent policy API.

- Remove bootstrap/client-shape reasons for `::`: default offer counts before signed adjustments,
  inferred construction order, and bundled card-search/reward/Audit work. Preserve meaningful
  correction relationships and avoid adding client steps.

- Revisit queued requirement adjustments without adding a player step: `Required` is System, but
  `-2 Required<Class<GlobalParameter>>` remains abstract until selection. Inventrix, Adaptation
  Technology, and Special Design therefore still use immediate live narrowing.

- Determine which nested cross-Actor Agent calls should settle Admin work before returning;
  the Pluto scenario in `ColoniesRulesTest` currently uses separate top-level calls.

- Investigate repeated selection probes followed by execution under `EAGER`; preserve manual-task
  competition and failure handling before removing probes.

- Start using `Agents.overlay` for an actual client or agent forecast, then measure its cost and
  compare it with the existing fork path. Internal selection and execution probes still use atomic
  rollback and may need a cheaper construction path. Probes on a source World currently invalidate
  its overlays even when the source history returns to the same point.

- Investigate repeated ranking of the same field while testing each candidate. Scores must reflect
  the current state, including rollback; avoid a second persistent representation of game scores.

<a id="source-docs-pets-roadmap-md"></a>

## docs/PETS_ROADMAP.md

Original source: `docs/PETS_ROADMAP.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-pets-roadmap-md--pets-roadmap"></a>

### Pets roadmap

<a id="source-docs-pets-roadmap-md--language-and-modeling"></a>

#### Language and modeling

5. **Extend properties and defaults only when they replace special cases.** Abstract property
   defaults, instruction-valued printed facts, and richer cardinality remain promising but
   unsettled. Adopt them only after inheritance, querying, binding, and materialization have a
   coherent systemic rule and the result removes existing bridges.
   [ELSE and instruction-level `(A)*`](pets/ELSE.md) are exploratory fallback candidates
   with concrete Canon uses and distinct shallow/deep semantics.

<a id="source-docs-playground-md"></a>

## docs/PLAYGROUND.md

Original source: `docs/PLAYGROUND.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-playground-md--mars-playground"></a>

### Mars Playground

<a id="source-docs-playground-md--cards-and-gameplay"></a>

#### Cards and gameplay

- Task execution is manual, including concrete tasks; avoid autoexecution so the user can follow
  resolution step by step. Effects accumulate in a log viewer panel.

<a id="source-docs-playground-md--history-and-restarting"></a>

#### History and restarting

- Browsing does not change the live game. Explicitly roll back to the viewed point to discard the
  subsequent history and try another move.

- Return to sandbox by rolling all the way back, or reset to start over.

<a id="source-docs-readme-md"></a>

## docs/README.md

Original source: `docs/README.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-readme-md--solarnet-documentation"></a>

### Solarnet documentation

- [Historical effect walkthrough: live effects through execution](life-of-an-effect.md)

<a id="source-docs-solarnet-roadmap-md"></a>

## docs/SOLARNET_ROADMAP.md

Original source: `docs/SOLARNET_ROADMAP.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-solarnet-roadmap-md--solarnet-roadmap"></a>

### Solarnet roadmap

<a id="source-docs-solarnet-roadmap-md--internal-design-and-game-execution"></a>

#### Internal design and game execution

1. **Make actions, payments, delegated control, and completion a single intelligible lifecycle.**
   **Selected; active design.** Unify the lifecycle while preserving choice, rollback, and Actor
   authority. Fix action identity as a necessary part of this work: a nonexistent second action on
   AI Central currently succeeds and consumes its use without drawing cards. See
   [action identity and invocation](../../../pets/docs/agents/ACTIONS.md),
   [turns and turn offers](../../../pets/docs/agents/GAME_FLOW.md#turns-and-turn-offers), including
   the Head Start variant, [on-turn identity and routing](../IDENTITY.md#who-is-on-turn-and-who-decides),
   and [deferred work](../SEQUENCING.md#deferred-work). Ordering delegated
   responses and their payments remains open; see [completion](../SEQUENCING.md#the-missing-rule-when-an-operation-is-over)
   and [payment gaps](../PAYMENTS.md#allocation-validation).

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
   and keep [proof-oriented automation](SMART_AUTOEXEC.md) distinct from strategy.

6. **Give manual experimentation honest modes.** **Selected direction.** Purple plays the full
   game; blue grants a turn; green initiates an arbitrary operation; yellow permits deliberate task
   abandonment; red applies corrections without advancing gameplay. Make those promises real in
   the REPL and reusable by Mars Playground. Mode changes must handle existing work explicitly and
   must not smuggle a Kotlin game driver into application code.

<a id="source-docs-solarnet-roadmap-md--code-clarity-and-confidence"></a>

#### Code clarity and confidence

5. **Pursue performance only when it enables qualitatively different work.** Profile expensive
   type construction, limiting, and queries before redesigning them. Integrate measured branch work
   only when it remains coherent after current architectural changes. Small percentage gains,
   speculative caches, and micro-benchmarks do not earn roadmap priority.
   Current measurements and data-structure hypotheses live in
   [ENGINE_GO_FAST.md](ENGINE_GO_FAST.md).

<a id="source-docs-solarnet-roadmap-md--game-records-and-reconstruction"></a>

#### Game records and reconstruction

1. **Make full and faithful export/import round-trip.** **Active.** `work4` has a shared full/short
   JSON format, hashes of Catalog data and the resolved premise, state-only exact import, and
   Agent replay with every Actor policy disabled. Replay tests now emit both forms and compare
   component changes and pending work without requiring matching task IDs or event ordinals.
   Finish production card-input capture and replay with the uncommitted `work1` tracker, then
   connect Playground's upload path. A public-database ingester and friendlier discrepancy flow
   follow later. [Export design](../EXPORT.md) owns the details.

<a id="source-docs-solarnet-roadmap-md--applications-that-need-solarnet"></a>

#### Applications that need Solarnet

2. **Enable complete human and autonomous play through the same capabilities.** **Selected
   direction.** End-to-end solo and multiplayer operation is desirable, including computer-driven
   play, but it must not split into separate gameplay architectures. Card tracking, task forms,
   Agent policies, and decision records should compose into a usable game-playing surface.
   The preserved TFMBot replay work is evidence and a future corpus, not permission to create a
   special bot engine or to confuse eager execution with strategy.

3. **Turn replay viewing and reconstruction into a compelling way to explore games.** Use Mars
   Playground to browse exact recordings, coherent history positions, useful queries, and trustworthy
   provenance. Pair it eventually with the compact decision format so a game can be read as both an
   exact evolving World and an explanatory sequence of choices. Broader causal analytics remain
   conditional on being derived cheaply from these foundations.

4. **Continue the dual-engine parity experiment.** **Active exploratory branch.** The
   `heroku-experiment` branch proves a Kotlin/JS facade that can accept semantic moves, drive a
   complete restricted game, and project a substantial Solarnet state snapshot. Continue toward
   shadowing accepted inputs from the open-source Terraforming Mars app and comparing both engines
   at stable points. The app should continue to own UI, persistence, shuffled cards, and hidden
   information; Solarnet should receive player intent, execute ordinary tasks, and expose
   disagreements. The app-side projector, comparator, transcript persistence, and broader move
   families remain before this becomes a complete parity proof.

5. **Keep REgo as the current experimental workbench.** The command-line interface remains the
   fastest place to inspect state, submit tasks, apply corrections, and test the color-mode model.
   Improve it where that exposes genuine runtime capabilities—especially task forms, mode
   integrity, and state exploration—without mistaking terminal polish or the low-priority `list`
   command for a major product program.

6. **Retain optimal-solo analysis as later research.** **Exploratory.** The TR63 monotonicity work
   shows how Solarnet's executable rules and closed catalog could support principled search and
   conditional proofs. Preserve the research and pursue it when the runtime can expose a tractable
   decision surface without a separate optimized engine. Large-scale strategy research remains
   below the design, Playground, gameplay, and reconstruction programs.

<a id="source-docs-agents-adversarial-md"></a>

## docs/agents/ADVERSARIAL.md

Original source: `docs/agents/ADVERSARIAL.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-adversarial-md--adversarial-play-around-solarnet"></a>

### Adversarial play around Solarnet

**Every player's local engine is theirs to use freely.** A player can explore alternative moves,
make hypothetical decisions for opponents, run Admin work, try assumed hands, change resources,
and rewind repeatedly. All of that is fair local calculation. Exploring a possible opponent hand
does not entitle the player to learn the actual hand.

There is an unresolved difficulty here: an accepted partial operation might later have no legal
completion. Local transaction rollback cannot retract another player's accepted decision or make
somebody forget a revealed card. The current
[sequencing discussion](../SEQUENCING.md#the-missing-rule-when-an-operation-is-over)
already distinguishes an individual transaction from completion across delegated work. The
surrounding agreement must address this case without assuming those are the same thing.

Finding a possible successful continuation would not by itself settle the problem. That
continuation might depend on another player's choice or an unknown draw. Requiring every proposal
to prove future completion would also be a substantial new verification requirement. No such
search requirement, automatic takeback, or forfeiture rule is selected here.

4. **What should a result tell somebody who wasn't there?** Your unrestricted, mutually agreed
   undo remains intact. But should a solo victory distinguish “succeeded on the first attempt”
   from “succeeded after seeing future draws and returning to an earlier position”? Both can be
   valid achievements; the final surviving branch alone may not communicate which achievement
   occurred.

5. **What exactly must be simultaneous: acceptance, or commitment before seeing others' choices?**
   If Bob's proposal is readable before Alice submits hers, accepting both through a merge still
   lets Alice respond to Bob. Must submissions therefore remain secret until everybody's choice
   is fixed? And if somebody then refuses to finish, do the remaining players get to learn the
   submitted choices?

### Additional Admin guidance

`Admin` is an Actor in the model, not necessarily a separate person, machine, branch, or committer.
Any player's submission may include appropriate Admin work performed by their local engine. Public
rule consequences remain independently checkable. If Admin work includes an actual discretionary
choice, the players must agree who supplies it; calling it Admin does not give the submitting
player permission to decide it unilaterally. Whoever keeps the deck secret has a separate job from
the engine's Admin Actor.

<a id="source-docs-agents-card-handling-md"></a>

## docs/agents/CARD_HANDLING.md

Original source: `docs/agents/CARD_HANDLING.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-card-handling-md--card-handling-and-external-tracking"></a>

### Card handling and external tracking

<a id="source-docs-agents-card-handling-md--card-hand-tracking"></a>

#### Card-hand tracking

[CardTracking](../../../src/common/dev/martianzoo/tfm/engine/CardTracking.kt) is an optional mode
selected once per World before the first card-hand movement. Once selected, each Agent command
validates all surviving `CardBack<Hand>` gains and removals, including project, corporation,
Prelude, and beginner cards, before it can complete. The client supplies names in each Player's
per-back hand-entry order and names for otherwise anonymous departures. Supplied names must resolve
to concrete `CardFront` classes with the matching back in the catalog. A source card can be named
even when that card is excluded from this game's playable pool. An operation with missing names
rolls back; automatic execution can leave the affected card task pending for the client.
Unused supplied names may be replaced after a failed operation without changing names used by
surviving events.

World copies do not copy card tracking, so new card movements remain anonymous. A copy made before
any hand movement can opt into tracking explicitly. Speculative Worlds refuse guard installation;
card movements in an overlay remain anonymous. The source World's tracker remains attached only to
its own history.

Missing names raise `CardTracking.NamesRequiredException`, an `AgentInputRequiredException` with
the Player, back, location, direction, additional count, and attempted change event. Its detached
`candidates` set describes the eligible source immediately before that movement, including earlier
movements within the rolled-back command and excluding names already supplied for this movement.
Null means a recognized fresh arrival requiring an external dealer; an empty set means no known
source card is eligible. Unknown transfers must not be interpreted as draws. `filter` carries the
arrival's `CardFilter` when present. The public `cardMatchesFilter` function shares the tracker's
printed-card predicate with dealers. The attempted event is diagnostic evidence, not committed
history.

The tracker retains its last successfully validated snapshot and processes only subsequent change
events. It reuses that snapshot only while its last event still survives by identity; rollback or
a different branch at the same ordinal causes reconstruction from surviving history. Failed
validation never replaces the saved snapshot. Queued names remain independent of that snapshot.

<a id="source-docs-agents-engine-md"></a>

## docs/agents/ENGINE.md

Original source: `docs/agents/ENGINE.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-engine-md--the-live-engine"></a>

### The live engine

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing construction of a live `World`, component mutation, events, transactions,
> tasks, effect execution, rollback, or the engine-facing part of `Agent`.
>
> **Status:** current-model map. It explains the engine's responsibilities and causal path, not
> every Pets operator or Terraforming Mars rule.

<a id="source-docs-agents-engine-md--the-model-to-keep-in-mind"></a>

#### The model to keep in mind

| Layer | Owns |
| --- | --- |
| `:state` | Replayable facts: concrete components, exact pending Tasks, and the event sequence |
| `:engine` | Interpretation: task resolution, limits, effects, attribution, and transactions |
| `:agent` | The normal Actor-scoped client API and optional autoexecution policy |

`GameWorld` is the passive center. It can apply or reverse already-decided events, but it cannot
interpret an Instruction or calculate a consequence. Engine services surround one `GameWorld` and
derive the events to apply. One stable `ActorEngine` per Actor supplies the policy-free mutation
boundary. `Agent` is the ordinary application-facing wrapper around that boundary.

For proposed changes to runtime layering, public APIs, policy, or game flow, use the Solarnet roadmap
for direction and inspect current source and tests for the implemented contract.

<a id="source-docs-agents-engine-md--from-a-premise-to-a-ready-world"></a>

#### From a premise to a ready World

Bootstrap drains choice-free Tasks through their assigned Actor Engines. An abstract instruction is
acceptable only when normal resolution leaves exactly one legal concrete result; initialization
must not silently choose among alternatives. Successful initialization requires no pending Tasks,
all exact premise components, and all applicable positive lower bounds, then commits the timeline.
Before a ready World or fork is returned, the engine also verifies every positive signed premise
selection representing an inhabited concrete Class has a live `Class<T>` component, and every
negative selection lacks one.

`Engine.fork` constructs an independently mutable live World only at a completed gameplay
position. It shares the immutable premise and Class Table, copies the passive component,
dependency, task, event, completed-position, and live-effect-index collections, and builds fresh
engine services around them. Immutable compiled live effects are shared, while index storage is
independent and retains source registration order. The fork point is the new timeline commit floor,
so the fork cannot roll back into source history. Application callbacks, component listeners,
transactions, and Agents remain fresh.

`Engine.overlay` accepts the same completed-tip position and builds ordinary engine services over
an event-suffix `GameWorld`. Exact component counts are source counts plus local deltas; pending
Tasks read the source until the first local Task event; event history reads the source prefix and
the local suffix. Live-effect subscriptions copy individual buckets only when changed. The source
World is never changed by overlay play. Applying or rolling back a source event invalidates the
overlay, including its retained component, task, and event views; seeking backward requires
truncating the retained future before overlay creation. Source Agent selection and execution probes
can also invalidate the overlay because they apply tentative events and roll them back. Run such
probes before creating the overlay or on the overlay's own Agents. `SpeculativeWorld.reset` clears local
projections and events directly, keeping the World and its retained views ready for another trial.
`SpeculativeAgents.reset` also clears its local input recording while keeping its Agents and policy
settings. `Engine.fork` on an overlay materializes the combined history, projections, and completed
positions into an independent World. `Agents.overlay` and `Agents.fork` create the corresponding
fresh Agents with each Actor's current autoexecution policy. Agent transaction guards are not
copied by either operation, so new card movements can remain anonymous. Overlays refuse guard
installation; a full copy of an overlay keeps its anonymous event history. Completion callbacks
and task recording begin fresh.

Engine-internal selection probes still use short atomic attempts and rollback on the active World.
The overlay work does not change that path; its cost and suitability for frequent probes need a
separate measurement.

<a id="source-docs-agents-engine-md--concrete-state-and-its-history"></a>

#### Concrete state and its history

`GameWorld.apply` is the sole forward event/projection boundary. `GameWorld.rollBackTo` reverses the
same projections while removing their events. Engine-side derived indexes, notably live effects,
must observe both directions rather than owning another copy of game state.

`Timeline` owns the positions between caller-facing Player Agent methods, its retained future when
seeking backward, failure-atomic blocks, and a commit floor. Public seeking accepts only completed
positions at or after that floor; stepping moves between the same positions. Seeking preserves later
events and restores components, Tasks, and derived indexes in either direction. Gameplay cannot
mutate the World while a future is retained; `truncate()` deliberately discards it before new play.
A Player Agent method records a position after its work finishes, including an `OperationScope`
method inside an enclosing client block. Admin calls do not automatically create positions. The
enclosing block remains failure-atomic; it does not hide positions between its nested Player calls.
Seeking inside an atomic call cannot cross that call's start. An atomic failure instead discards
that interaction's events and restores its starting state.
`Exceptions.AbortTransactionException` requests that restoration without surfacing as a caller
failure. The commit floor prevents navigation across initialization or a fork point. Gameplay
commits nothing, so seeking may cross turn and phase changes.

These commits are local timeline mechanics, not acceptance of a shared game's Git history. They
do not grant or deny a takeback between players. [ADVERSARIAL.md](../ADVERSARIAL.md) places that
agreement outside the engine; reconstructing an agreed earlier position is a separate operation
from rolling back an individual local transaction.

Transaction atomicity is not a game rule saying intermediate changes are invisible. Effects can
observe the individual changes that compose a successful operation.

<a id="source-docs-agents-engine-md--tasks-are-choices-not-a-program-counter"></a>

#### Tasks are choices, not a program counter

There is one semantic Task pool. Actor-specific queues are filtered views. Iteration is stable for
reproducibility, but its order carries no rule meaning.

Engine task execution leaves a handed-off task selected for its receiving Actor. `Agent.doTask`
runs configured autoexecution and requires that selected task to finish; otherwise it rolls back
the call. `selectTask` and `narrowTask` can instead leave work pending for the receiving Actor.
`doTask` can identify a gated task by its body, and a `/` task by its calculated action, without
repeating the gate or metric in the submitted instruction. The task still checks those conditions.
When matching a task by its body, a failed gate does not compete with available matches. If no
available task matches, the failed gate can identify the task so execution reports the unmet
requirement.

The finishing task's assignee receives structural children and continuations. A System handoff
encodes the former assignee in its continuation, so Admin executes the System step and returns later
work to that Actor. Authored `TASK` dispatch can also give work to another Actor. Queued reactions
use ownership and event attribution as described in [IDENTITY.md](../IDENTITY.md). Deferral by `LATER`
and `FINALLY` is the only task priority ([SEQUENCING.md](../SEQUENCING.md#deferred-work)), and there
is no persistent delegated
operation in current task data. The remaining routing and completion questions are in
[SEQUENCING.md](../SEQUENCING.md#the-missing-rule-when-an-operation-is-over). Any game-rule restriction
on which task may execute must apply equally to explicit commands and autoexecution. Protecting a
player from somebody else calling their Agent is instead an external submission concern covered
by [ADVERSARIAL.md](../ADVERSARIAL.md).

<a id="source-docs-agents-engine-md--effects-are-derived-behavior"></a>

#### Effects are derived behavior

Queued `:` effects produce pending Tasks. Automatic `::` effects execute recursively before queued
effects from the same change are admitted. The recursion limit is eight nested automatic effects;
exceeding it raises the shared Pets `ResourceLimitException` and rolls back the enclosing operation.
Parser and class-loading resource limits use the same exception. Task selection and execution
probes propagate it rather than reporting an unavailable choice. Because automatic effects create
no Task, their instructions cannot use instruction-side `BY`. Read [SEQUENCING.md](../SEQUENCING.md)
before using `:` versus `::` to force an order.

<a id="source-docs-agents-engine-md--wild-tags"></a>

##### Wild tags

See `WildTagHolderTest`, `WildTagTest`, and `WildTagContinuationTest` for allocation, ownership,
cleanup, and reuse scenarios. Replays use Canon cards and actual allocation choices without
wild-tag corrections. `CONCRETE` lets replays allocate before a competing payout; `EAGER` may
choose the payout first. Admin removes the action scope even when the player uses `NONE`;
dependency cleanup removes its elections and assigned tags.

<a id="source-docs-agents-engine-md--queries-invariants-and-dead-ends"></a>

#### Queries, invariants, and dead ends

**Owner direction (2026-10-14): decision-tree pruning is the player's agent or client's
responsibility, never an engine or model obligation.** Universally assume either that the caller
diligently presents only choices known not to dead-end the turn, or that exposing dead ends does
not matter. This assumption does not depend on a pruner existing today. Task availability,
selection, narrowing, and successful intermediate commands do not promise a successful turn.

Protected Habitats is the concrete example. Its
[`-Plant OR -Animal OR -Microbe BY Player(NOT Me@): Die`](../../../pets/src/common/dev/martianzoo/tfm/canon/bundles/corpera/cards.json5)
queues a consequence of an opponent's removal. The attempted removal may happen before that
consequence prevents completion; the caller must roll back the unsuccessful path, potentially
the turn. The engine need not anticipate that consequence or hide the protected target. This is
valid modeling, not an issue to fix. The existing
[ProtectedHabitatsTest](../../../test/common/dev/martianzoo/tfm/tests/cards/ProtectedHabitatsTest.kt)
expects failed attacks and retained protected resources.

The game `ClassTable` view compiles inherited invariants into immutable per-Class limits. Each
World's `Limiter` combines those facts with live multiplicity. An invariant constrains the state at
operation completion. On an ordinary gain, a positive exact-count invariant also constructs its
missing parts when the required Type is concrete and directly depends on the gained component.
Inherited requirements bind `This` to that component. Construction keeps pending requirements and
gains a part only once its dependencies exist, discovering that part's requirements in turn. This
allows a nested part to depend on another branch of the same structure regardless of declaration
order. If no pending part can proceed, normal gain resolution reports the missing dependencies.
The whole required structure exists before any of its gain events dispatches
reactions, so a required listener can observe a sibling's gain regardless of invariant order.
Construction records ordinary change events, including separate `Atomized` gains. Reactions see
the assembled structure. Automatic reactions traverse construction events in reverse, so a part
reacts before its owners can dispose of it. All construction events' automatic effects finish before
matching queued effects in their original event order. Both automatic and queued owner conditions
can therefore see their parts' automatic consequences. Existing parts count toward the requirement,
including parts supplied by overlapping requirements. Construction retains each gain's automatic
ancestry to share the existing recursion guard; waiting for a dependency does not reset that budget.
Failures roll back the whole operation.
`ClassLimitTable.requiredParts` supplies these construction requirements from the same compiled
invariant templates used for validation.

For component-count validation, a full operation is one initiating change and all its recursive
automatic effects. Intermediate counts may cross declared bounds; the completed operation must
satisfy them. Source availability and dependency integrity are checked at execution. Resolution never
executes consequences to predict a choice's outcome; failure at completion uses the existing
transaction rollback path. [QUANTIFIERS.md](../QUANTIFIERS.md#invariants-at-operation-completion) owns
the bounds and choice-resolution contract.
Initialization still audits all applicable positive minimums after constructing the initial world.
Ordinary gameplay selects global minimum checks through the changed components' existing per-Class
limit index. It still checks required dependents of live affected owners, including absent parts;
unchanged global counts need no recheck. Initialization and corrections retain the full
invariant query.
Each World's `Limiter` reuses the immutable declared limits bound to a component type. Live counts
and the required dependents of affected owners are still checked against current state.

A locally legal choice can discover later that its remaining work cannot complete. This is a
recoverable dead end: `DeadEndException` rolls the encompassing transaction back so another branch
can be tried. A broad choice is not a correctness bug merely because a branch fails late; it is a
bug if a prohibited result survives successful turn completion. An offered choice need not contain a successful branch; a client
can roll back farther when the whole choice leads to dead ends.

Failure atomicity covers the encompassing transaction; it does not promise that a later failed
command automatically undoes every earlier successful command in the turn. A caller exploring
across commands must roll back far enough to abandon the unsuccessful path. Neither that need
nor the absence of lookahead is an engine/model defect. Report a separate rule failure, such as
a prohibited result surviving successful turn completion or a required legal outcome being lost.

Whether that local calculation can be accepted as shared play, and what players do when already
accepted work cannot finish, are external questions in [ADVERSARIAL.md](../ADVERSARIAL.md). The engine
does not need to prove every future continuation succeeds before exposing a task, or conceal
local rollback to make a result count as an achievement.

The engine can leave a mandatory choice available even when its count invariants will fail.
Client-facing exploration and pruning belong to the player's agent or client. Engine resolution
uses current source and dependency prerequisites, authored gates, and AMAP/optional capacity,
without predicting repairs or pruning mandatory choices by count bounds.

<a id="source-docs-agents-engine-md--the-metric-operators-are-intentional"></a>

##### The metric operators are intentional

`Metric.Max`, saturating `Metric.Subtract`, and non-double-counting `Metric.Or` have few authored
uses, but they express different operations and are not redundant special cases. In particular,
union cannot substitute for arithmetic addition. Treat the small number of clients as evidence that
the metric algebra is still sparse, not as a reason to collapse these operators. `Metric.Rank`
likewise replaces game-specific ranking instructions with reusable comparison semantics.
Ranking scores the candidate once, skips comparing it with itself, and evaluates each peer's
later tiebreakers only while earlier scores remain tied. Scores are local to that rank query;
they are not retained across state changes or rollback.

<a id="source-docs-agents-engine-md--actor-engines-and-agents"></a>

#### Actor Engines and Agents

Each World retains one policy-free `ActorEngine` per Actor. It owns attributed task and state
commands but shares the World's changer, instructor, timeline, and Task services. `Agents(world)`
constructs the corresponding normal client objects and prevents pairing an Agent with an unrelated
World. Its optional `initialPolicy` function supplies each Actor's policy at construction without
executing pending work; callers need not assign the policy afterward.

All ordinary Agent mutations use the shared outer transaction-completion path. `runOperation`
admits new work, lets the body finish it, runs configured autoexecution, preserves unrelated
pre-existing unselected Tasks, and rejects newly unfinished Tasks or `MustCleanUp` state. It cannot
start while a pre-existing selected Task holds the World lock. `sneak` applies a fully concrete
correction group, constructs exact required parts, removes dependents, and audits every applicable
count invariant. The entire group rolls back on failure. It runs automatic effects and suppresses queued effects recursively
while bypassing settlement and completion callbacks. `exMachina` rejects explicit
System and MustCleanUp targets and any correction that leaves new MustCleanUp state.
It additionally restores and resumes an existing selection within one atomic lifecycle. See
[EX_MACHINA.md](../EX_MACHINA.md) for the contract and the remaining canon consistency gaps.

A World may install one `AgentTransactionGuard` before Agent commands begin. Every Agent mutation
then validates the guard before its transaction can commit. An automatic candidate that needs
unsupplied client information rolls back locally and stays pending; an explicit Agent request fails.
The Terraforming Mars hand-tracking use is described in [CARD_HANDLING.md](../CARD_HANDLING.md#card-hand-tracking).

Current autoexecution lives in `:agent`, not in the core engine. Policy selects legal Task commands;
it does not alter their semantics. Before probing tasks, the loop checks whether any pending
candidate has an effective policy other than `NONE`. If none does, it returns without probes or
execution, even when an Actor with no pending work has autoexecution enabled. When automatic work
is available, manual tasks still compete with it. The shared policy loop tries eligible Admin work
before Player work, including between explicit Player steps, but continues to an executable Player option when
Admin work is abstract or temporarily unavailable. It cannot interrupt an authored `::` chain or
supply a missing scheduling rule. Engine primitives remain available to any caller for local
exploration, replay correction, tests, and cheats. Restricting access to those primitives
is not an engine responsibility. [ADVERSARIAL.md](../ADVERSARIAL.md) describes how external participants
or software decide which proposed results and supplied facts to accept as shared play.

<a id="source-docs-agents-export-md"></a>

## docs/agents/EXPORT.md

Original source: `docs/agents/EXPORT.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-export-md--full-and-faithful-game-exportimport"></a>

### Full and faithful game export/import

<a id="source-docs-agents-export-md--file-and-import-contracts"></a>

#### File and import contracts

`GameFileImporter` in `agent` checks hashes before constructing a live game. The caller supplies
the game-specific runtime constructor and the game's start. It disables autoexecution for **all**
Actors, including Admin, then submits every recorded command through the corresponding Agent.
A full import compares the multiset of component changes with Actor and causal ancestry, final
components, and pending tasks with their causal ancestry. Raw event ordinals, task IDs, component
change order among independent effects, and intermediate task revisions may differ. The importer
copies source change notes to corresponding live changes after checking any notes produced by the
new execution. A mismatch throws; there is no repair or discrepancy UI yet. Task-event notes stay
in the exact passive recording because compacted task revisions need not have live counterparts.

`TaskLog` captures successful Actor submissions, including Player and Admin policy actions. It
records selections, single-task and batch execution, tries, drops, and `exMachina` as distinct
operations. A `doTask` completed through an Actor handoff records the originating selection and
the receiving Actor's execution separately. Its causal task selector helps replay target repeated
tasks without storing source ordinals. Game start is mandatory and recorded by
`TfmGameplay.startGame()`; import starts the game through that same method, and the game's own rules
drive play from there.
Unique task instructions omit ancestry from their selectors. Full import compares complete causal
ancestry through shared internal signatures, so authored phase flow does not require copying the
entire preceding game for each change.
Commands produced by game effects are consequences rather than Actor submissions. Intermediate
form edits that only narrow an instruction may be omitted; selections and stateful operations stay.

`TaskLog.selectionCheckpoints()` returns positions before surviving Player task selections and
replay corrections. Automatic selections that leave an abstract task awaiting input are included;
automatically completed tasks are not. Replay exports retain these positions plus the start and
end, so passive playback can stop before each recorded decision without stepping through every
automatic command. This does not change the live Timeline's completed positions.

<a id="source-docs-agents-ex-machina-md"></a>

## docs/agents/EX_MACHINA.md

Original source: `docs/agents/EX_MACHINA.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-ex-machina-md--corrections"></a>

### Corrections

<a id="source-docs-agents-ex-machina-md--entry-points"></a>

#### Entry points

When a task is selected, `exMachina` restores its unresolved form, applies the correction, and
resumes selection under the existing policy. The whole sequence remains failure-atomic.
Pre-existing gameplay may then proceed normally; queued effects of the correction remain
suppressed.

<a id="source-docs-agents-herokuapp-game-logs-md"></a>

## docs/agents/HEROKUAPP_GAME_LOGS.md

Original source: `docs/agents/HEROKUAPP_GAME_LOGS.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-herokuapp-game-logs-md--translating-herokuapp-game-logs"></a>

### Translating herokuapp game logs

<a id="source-docs-agents-herokuapp-game-logs-md--preserve-valuable-counterfactuals"></a>

#### Preserve valuable counterfactuals

Use the precise domain exception (`RequirementException`, `NarrowingException`, `LimitsException`,
`DeadEndException`, or another specific result). Put a choice-level failure inside the action lambda
beside the successful choice. Skip obvious cases that only require the card or resources gained by the
preceding action, and do not turn the whole-game test into a substitute for focused rule tests.

<a id="source-docs-agents-identity-md"></a>

## docs/agents/IDENTITY.md

Original source: `docs/agents/IDENTITY.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-identity-md--context-assignment-and-actor-identity"></a>

### Context, assignment, and Actor identity

<a id="source-docs-agents-identity-md--task-assignment-and-delegation"></a>

#### Task assignment and delegation

`Agent.doTask` demands completion of the submitted task, including when selection or narrowing
hands it to Admin or another Actor. It runs configured autoexecution and rolls back if the selected
task remains pending. The receiving Actor's policy still controls execution. Use `selectTask`,
`narrowTask`, or an optional `TaskForm` when handing off work for later completion.
Task recordings retain the originating choice and the receiving Actor's execution separately, so
imports need no autoexecution policy.

`RequiredAction` is also `System`: Admin creates and removes the pending corporation-action
marker. Its Player owner still chooses `DoRequiredActions`, which remains non-System.
`PlayCard` and `UseAction` are System signals: the Player supplies unresolved choices, and Admin
executes the concrete dispatch. `doTask` accepts this handoff when configured policy finishes it.

### Additional Admin guidance

`Admin` is the non-Player Actor and Component that performs neutral table activity. Admin may
control, receive, select, and narrow tasks. No identity rule requires Admin's decisions to be
deterministic.

<a id="source-docs-agents-make-types-types-again-md"></a>

## docs/agents/MAKE_TYPES_TYPES_AGAIN.md

Original source: `docs/agents/MAKE_TYPES_TYPES_AGAIN.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-make-types-types-again-md--make-types-types-again"></a>

### Make types types again

<a id="source-docs-agents-make-types-types-again-md--evidence-and-acceptance"></a>

#### Evidence and acceptance

- The final design removes the old state-dependent type model rather than preserving it under a
  different name. No additional dead-end prevention or player-choice pruning is introduced.

<a id="source-docs-agents-payments-md"></a>

## docs/agents/PAYMENTS.md

Original source: `docs/agents/PAYMENTS.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-payments-md--payment-model"></a>

### Payment model

<a id="source-docs-agents-payments-md--ordinary-resource-losses"></a>

#### Ordinary resource losses

Pharmacy Union's disease losses and Flooding's self-inflicted loss leave an open payment unchanged.
Their sandbox tests resolve those losses before the explicit payment and verify the full cost.
Pharmacy Union's starting 54 MC is queued behind a zero-disease gate; its microbe reaction places
disease before offering the capped 4 MC loss. Choosing disease before starting cash can leave setup
unable to finish, requiring undo. This is allowed; the model need not make every available choice
lead to completion. No special starting-resource phase or autoexecution exception is used.

<a id="source-docs-agents-payments-md--interleaving-and-rollback"></a>

#### Interleaving and rollback

Accepting Neptunian's offer does not prevent the player playing Flooding from resolving its loss
while the purchase remains unpaid. This ordering is allowed. On 2026-10-14 the owner explicitly
confirmed that making the purchase unaffordable this way is a voluntarily dead-ended turn, not a
bug: the player playing Flooding must roll back.

`FloodingTest` covers losses both before payment and after a separate Steel payment. The loss
never credits the bill. A failed cash payment leaves the earlier Steel payment intact; rolling
back the enclosing turn restores both players' resources and removes the debt. These scenarios do
not require a lock around the purchase or automatic rollback of earlier payment commands.

<a id="source-docs-agents-payments-md--current-pieces"></a>

#### Current pieces

The sandbox suite exercises this ordering with explicit submissions. Gameplay helpers suspend
player autoexecution while submitting a specified payment allocation. Under `CONCRETE` or `EAGER`,
a cash-only bill may otherwise finish automatically; callers must not submit that payment again.
Custom allocations belong in the helper's payment callback, before the purchased result.
The script `tfm_pay` command uses the same allocation helper; `tfm_play` and `tfm_action` pause
player autoexecution while submitting a written payment, then restore the configured policy.

<a id="source-docs-agents-payments-md--evidence"></a>

#### Evidence

Preserve observable card/action outcomes, discounts, values, source restrictions, rollback, and
settlement before consequences. Do not create mirrored allocation state or card-specific client
orchestration to address a local symptom.

<a id="source-docs-agents-quantifiers-md"></a>

## docs/agents/QUANTIFIERS.md

Original source: `docs/agents/QUANTIFIERS.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-quantifiers-md--instruction-quantifiers"></a>

### Instruction quantifiers

<a id="source-docs-agents-quantifiers-md--vocabulary-and-evaluation-time"></a>

#### Vocabulary and evaluation time

Resolution checks execution prerequisites and calculates quantities; it does not predict whether
the completed operation will satisfy count invariants. Mandatory choices can lead to recoverable
dead ends. Exploring and pruning those branches belongs to the player's agent or client, not
engine resolution. Under the [universal design assumption](#source-agents-md--decision-tree-pruning-belongs-to-the-players-agent-or-client),
either the caller presents only choices known not to dead-end the turn or exposing dead ends is
acceptable; a missing pruner does not justify changing quantifier semantics.

<a id="source-docs-agents-quantifiers-md--invariants-at-operation-completion"></a>

#### Invariants at operation completion

Declared component-count invariants must hold when a full operation completes. For this check, an
operation consists of one initiating component change and its entire recursive `::` cascade,
including `THEN` stages within that automatic work. Mandatory changes may cross count bounds in
intermediate states. Before completing the operation, the engine validates affected bounds,
including positive minimums activated by newly gained components. Failure rolls the transaction
back, including components, tasks, events, and derived effects. Bootstrap audits all required counts
after constructing its initial world.

Queued tasks, `THEN` stages outside the initiating automatic cascade, and separate initiating changes
each belong to separate operations for this check. They cannot repair an earlier operation. A game
action or client transaction can contain several such operations; its wider rollback boundary does
not postpone their invariant checks.

Turmoil's player supply check is a queued `Delegate` gain effect, after automatic consequences.
A placement can temporarily make eight delegates when the unused lobby delegate is still present;
`LobbyDelegate` automatically removes itself before the queued supply check. With no lobby delegate,
the eighth delegate instead dead-ends the turn. `PartyDelegate` is not `Atomized`: Canon authors
individual placements explicitly, using a shared party variable when required, and Canon invariants
reject bulk delegate gains.

<a id="source-docs-agents-quantifiers-md--uninhabited-and-computed-zero-changes"></a>

#### Uninhabited and computed-zero changes

An uninhabited Type has a provably empty domain in the current premise. A change touching an
uninhabited target is `Ok` when optional or AMAP and a `DeadEndException` when mandatory. It does
not auto-narrow, fire triggers, or participate in abstract feasibility.

<a id="source-docs-agents-quantifiers-md--composition"></a>

#### Composition

- `A OR B`: each arm resolves independently against the same current World. Failed gates, missing
  physical sources or dependencies, and mandatory uninhabited changes can eliminate arms. Declared
  count bounds do not eliminate mandatory arms, even when no automatic consequence will repair
  them. A surviving arm can fail at execution; the transaction then rolls back. `Ok` is a real
  surviving arm, and equal surviving arms are deduplicated. An alternative containing only zero
  changes and `Ok` requires no decision, while retaining its Types for matching. If no arm survives,
  the `OR` reports the strongest applicable failure rather than becoming `Ok`. An automatic effect
  must still resolve without a choice: use an authored condition if one arm is intended, or queued work if an Actor
  should choose. Admin can make such ordinary queued choices too.

<a id="source-docs-agents-readme-md"></a>

## docs/agents/README.md

Original source: `docs/agents/README.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-readme-md--agent-documentation-router"></a>

### Agent documentation router

<a id="source-docs-agents-readme-md--behavior-and-architecture"></a>

#### Behavior and architecture

Always apply the [decision-tree principle](#source-agents-md--decision-tree-pruning-belongs-to-the-players-agent-or-client):
assume the player's agent or client presents only choices known not to dead-end the turn, or accept
that dead ends are exposed.
Do not treat missing pruning as an engine or Pets modeling issue. Protected Habitats rejecting an
attempted theft through a later dead end and rollback is valid. The engine note owns the detailed
explanation; the autoexecution note owns optional pruning policy.

| Subject | Read |
| --- | --- |
| Live World construction, components, tasks, effects, transactions, rollback, or Agent responsibilities | [`ENGINE.md`](../ENGINE.md) |
| Component graph performance, overlays under search traffic, or Canon-specific indexing research | [`ENGINE_GO_FAST.md`](ENGINE_GO_FAST.md) |
| Actor attribution, on-turn identity, Admin, assignment, or delegated narrowing | [`IDENTITY.md`](../IDENTITY.md); lexical ownership is in [Pets](../../../pets/docs/agents/OWNERSHIP.md) |
| Game flow, turns and offers, Head Start timing, task order, `THEN`, automatic effects, delegated control, cleanup, or task priority | [`SEQUENCING.md`](../SEQUENCING.md) |
| Payment choices, accepted resources, billing, or settlement | [`PAYMENTS.md`](../PAYMENTS.md) |
| Autoexecution policy, pruning, Admin's greedy settling, or whether an automatic command preserves player outcomes and choices | [`SMART_AUTOEXEC.md`](SMART_AUTOEXEC.md) |
| Gain/removal/transmutation counts, AMAP, or abstract targets | [`QUANTIFIERS.md`](../QUANTIFIERS.md) |
| Proposed ELSE or instruction-level `(A)*` fallbacks and their Canon uses | [`ELSE.md`](pets/ELSE.md) |
| Correction, `exMachina`, `sneak`, effect suppression, or correction invariants | [`EX_MACHINA.md`](../EX_MACHINA.md) |
| Card backs, draws, purchases, reveals, or replay card tracking | [`CARD_HANDLING.md`](../CARD_HANDLING.md) |
| Authored actions, action identity, invocation, or missing action slots | [`ACTIONS.md`](../../../pets/docs/agents/ACTIONS.md) |
| Who may submit a player's decisions, hidden information, accepted game history, or agreed undo | [`ADVERSARIAL.md`](../ADVERSARIAL.md) |

For public client APIs, automatic policy, export, runtime layering, diagnostics, or content
selection, inspect the current source and tests for implemented behavior. Use the matching
roadmap to locate the owning design note and understand its priority and status. Keep detailed
decisions in that note rather than duplicating them in the roadmap.

<a id="source-docs-agents-readme-md--verification-and-reconstruction"></a>

#### Verification and reconstruction

- Read [`OPTIMAL_SOLO.md`](OPTIMAL_SOLO.md) only for the retained TR63 monotonicity research; its static
  report and Catalog analysis live in [Pets](pets/MONOTONICITY.md).

<a id="source-docs-agents-review-criteria-md"></a>

## docs/agents/REVIEW_CRITERIA.md

Original source: `docs/agents/REVIEW_CRITERIA.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-review-criteria-md--final-review-criteria"></a>

### Final review criteria

<a id="source-docs-agents-review-criteria-md--do-not-mistake-dead-ends-for-defects"></a>

#### Do not mistake dead ends for defects

Apply the [decision-tree principle](#source-agents-md--decision-tree-pruning-belongs-to-the-players-agent-or-client)
before raising findings: assume the player's agent or client presents only choices known not to
dead-end the turn, or accept that dead ends are exposed. No implemented pruner is required for this design
assumption. Protected Habitats allowing an attempted theft that later dead-ends and requires
rollback is valid modeling.

Reject findings, TODOs, and proposed engine/model changes justified only by an exposed dead end,
late failure, or caller rollback. Require a separate rule failure: for example, a prohibited theft
surviving successful turn completion, or a required legal outcome being excluded. This does not
prevent work explicitly requested on agent/client pruning.

<a id="source-docs-agents-review-criteria-md--rules-and-ownership"></a>

#### Rules and ownership

- autoexecution, incidental iteration order, `THEN`, `::`, priority, or pre-pruning was used to hide
  missing choice, task identity, completion, or engine semantics. Exposing a path that must later
  be rolled back is not itself missing semantics; apply the dead-end principle above.

Use [`VALUES.md`](../../../pets/docs/agents/VALUES.md#keep-pets-central), [`TESTING.md`](../TESTING.md#test-design),
and [`SEQUENCING.md`](../SEQUENCING.md#before-adding-order) for the detailed tests. Inspect the current
Agent policy source before relying on automatic execution.

<a id="source-docs-agents-sequencing-md"></a>

## docs/agents/SEQUENCING.md

Original source: `docs/agents/SEQUENCING.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-sequencing-md--sequencing-delegated-control-and-completion"></a>

### Sequencing, delegated control, and completion

<a id="source-docs-agents-sequencing-md--current-model"></a>

#### Current model

There is a global task pool; Actor queues are filtered views. A comma-separated group becomes
independent tasks. Stable iteration order supports reproducibility and display but carries no game
meaning.

Deferral is the only task priority, and there is no exclusive interaction spanning several tasks.
The turn order and every phase change are authored rules; see [Game flow](../../../pets/docs/agents/GAME_FLOW.md#game-flow).
An Agent may choose among its available tasks; policy cannot supply a missing game rule.
[Deferred work](../SEQUENCING.md#deferred-work) owns the rules.

<a id="source-docs-agents-sequencing-md--before-adding-order"></a>

#### Before adding order

A choice that fails within the same transaction can often be rejected through ordinary Pets and
rollback. That does not cover an interaction spread across committed commands: rejecting a later
payment step does not undo earlier partial payment.

| Mechanism | Meaning |
| --- | --- |
| No extra ordering | Availability and rollback already express the rule. |
| `A: B` | A creates ordinary selectable B work. |
| `A:: B` | B restores coherence before queued player work can proceed. |
| `A THEN B` | A's task must finish before B is admitted. |
| `A: LATER { B }` | B waits until no ordinary work remains in the World. |
| `A: FINALLY { B }` | B waits until no ordinary or `LATER` work remains in the World. |
| Committed precursor | A modifier needs an event before the final result exists. |
| Specific latch | Known prerequisites must finish before later work becomes legal. |

A fixed outcome does not imply automatic execution or Admin ownership. Conversely, implementation
housekeeping should not become a Player choice merely because it currently occupies a task. Do not
repair ordering through `TfmGameplay`, policy settings, rendered-task matching, or pool order.

<a id="source-docs-agents-sequencing-md--automatic-effects"></a>

#### Automatic effects

An automatic `Die` can reject a forbidden component change and roll it back. It cannot generally
police task selection because selecting or narrowing an abstract task can change task state without
producing a component event. Trigger-side `BY` matches the event Actor; instruction-side `BY`
reassigns concrete queued work.

<a id="source-docs-agents-sequencing-md--the-missing-rule-when-an-operation-is-over"></a>

#### The missing rule: when an operation is over

Neptunian Power Consultants allows P1 to hand an optional decision to P2, then resume other work
while P2's payment remains unfinished. On 2026-10-14 the owner confirmed that Flooding making that
purchase unaffordable is an allowed dead end: P1 voluntarily made their own turn impossible to
finish and must roll it back. This is not evidence that delegated work needs exclusion or a join.
`FloodingTest` covers these orders and rollback;
[`PAYMENTS.md`](#source-docs-agents-payments-md--interleaving-and-rollback) describes the payment-specific evidence.

<a id="source-docs-agents-sequencing-md--deferred-work"></a>

#### Deferred work

<a id="source-docs-agents-sequencing-md--engine-obligations"></a>

##### Engine obligations

- Deferral restricts selection only. It applies equally to explicit commands and autoexecution, and
  it does not order tasks with the same deferral: legal sibling orders stay available.

- Promotion edits the task's deferral. It rolls back with its transaction and is reproduced from
  the event history.

<a id="source-docs-agents-sequencing-md--operation-completion"></a>

##### Operation completion

`ActorEngine.requireComplete` currently accepts an operation when no new task remains, or when
World `FINALLY` work has begun since the operation's last decision. The first final promotion
occurs after all preceding ordinary and `LATER` work, and the cleanup-component check uses that
same point in the event log. Agents record each operation's beginning and decisions; automatic
execution is not a decision.

Completion still does not exclude intervening work: P1 may resolve Flooding's loss while P2's
Neptunian payment remains. If that choice makes the turn impossible to finish, P1 must roll back
the turn. See [the interaction discussion](../SEQUENCING.md#the-missing-rule-when-an-operation-is-over).

<a id="source-docs-agents-sequencing-md--decisions-and-open-questions"></a>

##### Decisions and open questions

- **Exclusion is not deferral.** Deferral orders work; it cannot keep other work out while an
  interaction is under way. The Flooding–Neptunian dead ends above are allowed and do not require
  exclusion or a join.

<a id="source-docs-agents-sequencing-md--evidence"></a>

##### Evidence

- `DeferredWorkTest`: selection, promotion batches, every Actor's work, late evaluation of metrics
  and fanouts, groups, nesting, final work, and rollback.

<a id="source-docs-agents-sequencing-md--source-evidence"></a>

#### Source evidence

- `TaskDispatchTest`, `PhilaresTest`, `DeferredWorkTest`, and the Neptunian cases in
  `FloodingTest`: current observable contracts, including allowed dead ends and rollback.

<a id="source-docs-agents-spec-fidelity-md"></a>

## docs/agents/SPEC_FIDELITY.md

Original source: `docs/agents/SPEC_FIDELITY.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-spec-fidelity-md--api-specifications-kdoc-and-fidelity"></a>

### API specifications, KDoc, and fidelity

<a id="source-docs-agents-spec-fidelity-md--priority-scope"></a>

#### Priority scope

| Module | Contracts to explain |
| --- | --- |
| `state` | World observations, components, actors, tasks, events, recordings, and the lifetime and mutability of returned views. |
| `engine` | World construction, execution, transactions, effects, rollback, checkpoints, and forks. |
| `agent` | Actor-scoped queries and mutations, task selection and narrowing, completion, delegated control, forms, and automatic execution policy. |
| `tfm-state` | Read-only Terraforming Mars queries, including resource names, printed production, and map lookup. |
| `tfm-engine` | Terraforming Mars game construction, gameplay calls, payments, and caller responsibilities for phase progression. |

<a id="source-docs-agents-testing-md"></a>

## docs/agents/TESTING.md

Original source: `docs/agents/TESTING.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-testing-md--testing-and-verification"></a>

### Testing and verification

<a id="source-docs-agents-testing-md--test-design"></a>

#### Test design

<a id="source-docs-agents-testing-md--test-categories-we-care-about"></a>

##### Test categories we care about

Give each scenario variant its own named test, using a private helper for shared steps. Do not put
variants in a `for` loop inside one test: the failing test name should identify the case. Exercise
the behavior under test through gameplay. For sandbox tests, establish incidental
preconditions through the standardized fixture and corrections by default. Use authentic
gameplay instead when the way a precondition is reached can affect, or provide useful evidence for,
the behavior under test.
Sandbox players always use `NONE`. Gameplay players default to `CONCRETE`; suites that must
control intermediate steps request `TfmGameplayTest(NONE)`, as described below.

For delegated payment, final resource totals alone do not establish correct task routing or order.
A helper may legitimately call several Actors' Agents; this is not an authorization bypass.
Exercise separate Agent commands when needed to expose the selections and choices each requires,
and test intervening work against the intended game outcome with autoexecution disabled where
necessary. Do not require the engine to identify who operates an Agent or reject a program merely
because it calls another Player's Agent. [ADVERSARIAL.md](../ADVERSARIAL.md) assigns that submission
check to the external game arrangement. [SEQUENCING.md](../SEQUENCING.md#the-missing-rule-when-an-operation-is-over)
records the remaining operation-level questions. Do not preserve former routing fields merely to
keep synthetic characterizations passing.

<a id="source-docs-agents-testing-md--sandbox-and-gameplay-fixture-migration"></a>

##### Sandbox and gameplay fixture migration

Gameplay constructs players with `CONCRETE`; suites that exercise intermediate steps request
`TfmGameplayTest(NONE)`. Admin uses `EAGER`. These policies stay fixed: neither the fixture nor its
scenarios assign `autoExecPolicy`. Use explicit `autoExecNow(EAGER)` calls to settle competing
routine effects, production, and phase cleanup. Put them inside the observed operation or
`expectChanges` block when their effects belong to the assertion. An automatically selected order
is not evidence of a required order; scripts testing that order must state the steps explicitly.
Fixed payments can now complete under `CONCRETE` or `EAGER`; do not submit an already completed
payment again. Custom helper allocations belong in the payment callback, before the result body.
Finish or decline alternative payment offers before the exact remainder, and explain each `Ok`.
Short repeated action sequences are written out; loops remain appropriate for seat-order traversal
and long generation/endgame progressions.

The sandbox owns its plain `Agent` fixture directly and must not use `TfmGameplay`, including
through inherited helpers. Gameplay also owns its fixture directly; neither extends `TfmTest`. Both expose
`newTestGameBeforeCorporationPlay()` for explicit corporation plays; a normal
`newTestGame(kimCorporation = ...)` plays the corporation during setup. `kimCorporation` never
changes the game configuration; use `addOptions` to include cards outside the default bundles.
Each owns its setup; there is no intermediate shared base. Neither is restricted to project cards.
Their setup defaults remain aligned; differences follow from prepared positions versus authentic
play and the explicit autoexecution policies described here.
Shared rules get representative coverage using base-game content where possible: for example,
`rules/RequiredFirstActionTest` uses Inventrix rather than repeating the same rule for each
corporation with a required first action.

Both normally return at generation 1 Prelude phase when `PreludeExpansion` is selected, otherwise
Action phase. `newTestGameBeforeCorporationPlay` instead returns before any corporation is played;
Kim takes the nonbeginner setup path, and the test makes the corporation plays and their choices
explicitly. Select a card through `addOptions` when it is outside the default bundles, so it can
appear in Kim's offer. Do not add extra arguments for corporation offers or project purchases.
Select `kimCorporation` to give Kim the normal corporation path; other seats use the beginner path.
Both fixtures discard five of Kim's ten offered projects before corporation play. Normal setup
then purchases the remaining five; explicit corporation-play tests make that purchase themselves.
The sandbox settles normal corporation setup with `autoExecNow(EAGER)`, including the exact
starting-card payment offered by billing.
The default game has:

The sandbox prepares and caches up to eight recently used `World`s, keyed by option-set,
player-count, Kim corporation, and entry-point combination, then gives every test an independent
`Engine.fork` of that prepared position. A cached World is never exposed or mutated after
preparation. The base class does not create a game automatically. Gameplay creates a fresh World and
starts the game for every call. Use `@BeforeTest` when every method needs the same setup.
When some methods need a different setup, call the common setup explicitly from the methods that
need it and initialize the exceptions directly. Never prepare a game only to replace it at the
start of a test. Keep scenario-specific corrections visible in the test. Pass named arguments such
as `addOptions = "CimmeriaMap"` or `playerCount = 4` where needed. Pass `kimCorporation` only when its
live effect is part of the scenario; Kim then plays that corporation normally and resolves its setup
effects, while every
other player keeps a beginner corporation. Do not share or roll back a live World between tests.

Before setup and after forking the prepared position, the sandbox sets every player's policy to
`NONE`, following
`OtbGame20261009Test`'s explicit task style. Card and rule scenarios retain the prepared position
and visible `exMachina` corrections, then use the fixture's
`inTurn { doTasks(...) }` calls in place of Terraforming Mars gameplay helpers. Admin retains its
default policy. `inTurn` supplies the sandbox turn and checks that the complete action leaves no
pending tasks; payments, card transfers, player effects, and any player-assigned cleanup are explicit.
The sandbox exposes its players as plain `Agent`s. For corporation scenarios that exercise the
initial play, use `newTestGameBeforeCorporationPlay` and submit the printed effects and starting-card
purchase in the test. For ordinary named-corporation setup, the fixture submits the corporation
play as an Agent task and runs `autoExecNow(EAGER)` to settle its effects and exact starting-card
payment. Kim's configured policy remains `NONE` throughout. Scenario payments finish or decline
alternative resources before submitting the exact remainder in the bill's denomination. If those
alternatives or a pending discount cover the full cost, submit `Ok` before the purchased result.
Pharmacy Union's initial-play scenarios explicitly take the gated 54 MC, place each disease and
resolve its loss, then pay the full starting-card bill. Ordinary losses do not credit a bill;
see [PAYMENTS.md](../PAYMENTS.md#ordinary-resource-losses).
In this style, omit `.expect()` assertions that merely repeat the submitted tasks; retain assertions
for automatic consequences, excluded outcomes, and state after rejected actions. Continue using
`setToExMachina` for desired absolute counts rather than calculating relative fixture adjustments.
Prefer compact `FROM` expressions, such as `Steel<Kim FROM Rob>` and
`ProjectCard<Hand FROM Selecting>`, whenever the transmutation shares the same Class.
Do not retain Terraforming Mars gameplay helpers in sandbox scenarios, including tests of helper
policies. Preserve payment and card rules through explicit tasks; helper-only policy checks are
outside this suite. Nested `TfmGameplayTest` suites follow the real game flow. Tests solely
comparing autoexecution policies are omitted.
Use `selectTask` and `selectOnlyTask` only for an unresolved choice, structural expansion, or
handoff, never to carry out concrete player work. Every retained selection must have a local
comment explaining why it is necessary. Prefer a short named selection, such as
`selectTask("PlaceGreeneryTile")`, to an anonymous selection. String selection also narrows the
task; it is not a name-only lookup. Keep `selectOnlyTask` when a short instruction cannot identify
an `EACH` group or would discard part of a linked choice, as in Flooding's placement and attack.
Use `doTask` for the Actor's explicit work, naming a short, meaningful instruction.
This also applies when binding X makes player work concrete. When the bound amount instead opens
a bill through Admin, use a commented `selectOnlyTask(x = ...)`. Do not copy full rule expressions
merely to identify the sole task.
Write expected payment amounts explicitly, such as `6 Pay<> FROM MC`; never pay `/ Owed`
in a sandbox scenario. Write `kim.inTurn { ... }` directly, without redundant `with(kim)` wrappers.
Inside that block, helpers on its operation receiver do not repeat `kim.`; another actor remains explicit.
`TfmGameplay.turn` groups the published game's turn, including declining an unused second action.
`TfmSandboxTest` supplies its own `Agent.inTurn` member extension: it uses `runOperation("OfferAction")`
when no task is pending, and otherwise `completeOperation`. This keeps the generic Agent free of
Terraforming Mars rules while sandbox scenarios retain their explicit operation scripts. Cross-player
Agent calls can leave Admin work waiting within that block;
use a locally explained `autoExecNow()` only when it must settle before the next player choice.
Use `doTasks` for consecutive concrete instructions. Preserve order when it matters to the scenario.
Comment an ambiguous `Ok` to identify what is being declined.
Write scenario actions in sequence rather than loops. Small private scenario helpers may factor
repeated explicit task sequences, with varying bonuses passed at each written-out call.
Call the base fixture's `advanceUntilActionPhase()` to reach the next Action phase. From Action,
it advances a generation; the sandbox currently expires generation-scoped state without running
production or research. The base fixture owns that degree of simulation. From Corporation or
Prelude, it skips remaining setup plays and enters Action phase.
For rejection-and-rollback scenarios, put `shouldThrow` outside the operation. Do not catch the
failure inside the operation and call `abort()` before checking the restored state.
The shared fixture's own preparation still uses its existing helpers and autoexecution.
When Turmoil is selected, it uses the existing test setup's initial global-event reveals
(`DemocraticReform` and `MinimalImpactPolicy`) before entering Corporation phase.

Treat every pre-migration test as a fallible historical artifact, not as a specification of its
setup. Existing options, cards, resource grants, card plays, phase changes, autoexecution policies,
and operation order may be incidental, copied from another scenario, obsolete, or compensating for
old behavior. Begin with the behavior named by the scenario and its meaningful observable coverage.
Challenge every setup step: if removing it does not change the behavior under test, remove it. For
sandbox scenarios, do not preserve gameplay merely because the old test used it to reach a state
that a correction can express. Gameplay scenarios must still reach their conditions through
play. Do not infer a configuration requirement solely from the old test's configuration.

- Use `victoryPoints()` to query final VP totals in player-seat order for the current position.
  Finish pending choices before querying. It executes the existing final-scoring phase and rolls
  back in `finally`, including on failure. It does not advance through production or final greenery;
  arrange the position to be scored explicitly. The query preserves the live game's state and
  history for subsequent gameplay. Supply `victoryPoints { ... }` when scoring offers player
  choices, such as Vermin's per-player deductions.

<a id="source-docs-agents-testing-md--expectations"></a>

##### Expectations

A `TaskResult` includes its operation's changes, but subsequent completion callbacks can begin
production or continue a Solar phase after that result was recorded. Gameplay tests use
`expectChanges("...") { ... }` when the observation must include that follow-up work or several
choices in the same phase. This assertion reads change events across the block without changing
transaction or autoexecution behavior. Place it immediately around the relevant choices, and use
explicit player holders for cross-player effects. For example, a global-event payout may occur
before its ocean choice, so observe both World Government and that placement together. Do not
save pre-action counts and assert arithmetic against them. Retain literal absolute assertions for
phase identity, complete final scores, and unchanged state after a rejected operation.

<a id="source-docs-agents-testing-md--game-replay-tests"></a>

#### Game replay tests

`Game20230521Test` and `Wsbg2025Test` deliberately exercise the raw Agent task API.
Their players use `NONE` during the replay, while Admin retains normal autoexecution and handles
fixed payment bookkeeping and final scoring. `VictoryPoint` and `ScoreEventVps` are `System`;
players do not submit scoring tasks. A pending negative score can wait for a positive score to supply the points.
Follow the game's flow through the last passes, production, final greenery, and scoring; do not
manually supply turns or phase transitions. Omit copied game-log
comments that only restate the submitted tasks.
Use resolved, concrete instructions and `PROD[...]` in these `doTasks` calls, without AMAP dots or
unevaluated metrics. Combine amounts when the matching tasks are already pending. A scalar total
consumes all pending gains and removals of the same type and quantifier when their signed total
matches, even if an individual task has that amount. Gains execute first; effects created while
those changes run do not join the total. Explicit `doTasks(...)` calls are atomic, resumable batches.
In explicit setup, have each player take their starting TR before either player handles starting
cards. Group each corporation, Prelude, and action-phase turn in `player.turn { ... }`; the wrapper
declines an unused second action. Keep the task submissions inside each section as raw `doTasks`
calls.
Use the shared card-name constants, give every `Ok` a short inline comment to its right naming
the task it dismisses (for example, `// no steel paid`). Keep the replay free of `.expect()`
calls; retain its state checkpoints and final score/winner assertions. Purchase transfers into
`Hand` are explicit tasks from `Selecting`. Keep the replay's task calls instead of introducing
`TfmGameplay` action or payment helpers.

Replay input recording and export are disabled by default, including in browser tests.
With `-PreplayExports`, `ReplayExportExtension` enables recording before test setup, so
`AbstractFullGameTest` starts `Agents.taskLog` before gameplay. After each successful JVM
replay, the extension writes full and short game files and reexecutes the full file in a
fresh game using its checked premise and recorded inputs. Every imported Actor, including Admin,
uses `NONE`. The check compares the component changes with their Actors and causal ancestry,
the final components, and pending tasks. Incidental ids, event ordinals, and the order of independent
changes may differ.
A round-trip failure fails the test. Replays must follow the game's normal flow and use
ordinary task submissions instead of injecting operations. Partial games may stop with decisions
pending.
The hook reports each successful round-trip in test output.

- When an Event card's sibling tasks require explicit replay ordering, record only the gameplay
  consequences. The card's `LATER` dispatch defers its flip, and the owner performs the flip once
  no ordinary work remains. The engine performs the dispatch itself; a replay whose Players do not
  autoexecute records only the explicit `PlayedEvent<Class<X>> FROM X` step.

<a id="source-docs-agents-testing-md--state-reconciliation"></a>

##### State reconciliation

- Never call `sneak` directly in a game test. Use the test's `exMachina` helper for an
  evidence-backed player error that requires a state adjustment. Place it as late in the
  timeline as the sourced assertions allow, with a comment saying which later step requires it. Add
  source-backed `.expect()` assertions for the mistake-prone types to preceding actions wherever
  practical so the remaining unexplained gap is bounded as narrowly as possible.
  If auto-exec has already selected the next task, the helper rolls that selection back and repeats
  it after the adjustment so state-dependent instructions are resolved again.
  Both replay bases delegate that lifecycle to the shared `World.exMachina` implementation; do not
  fork its task-history traversal.
  Keep unexplained state reconciliations as standalone timeline statements.
  Never place a manual or other raw adjustment inside an unrelated action body to evade a selected-task or
  operation-scope restriction; use an explicit test mechanism or fix the helper/API instead.
  Nest a missing consequence only when the enclosing action caused it.

<a id="source-docs-agents-automatic-effects-audit-tsv"></a>

## docs/agents/automatic-effects-audit.tsv

Original source: `docs/agents/automatic-effects-audit.tsv` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-agents-automatic-effects-audit-tsv--retired-audit-rows"></a>

### Retired audit rows

```tsv
scope	source_file	line	occurrence_on_line	enclosing_name	current_status	assessment	family	source_finding	next_investigation_not_verified_fix	source_excerpt
Canon	pets/src/common/dev/martianzoo/tfm/canon/bundles/base/payment.pets	15	1	Billing	retained	unproven	Zero bill completion	Billing removes itself before its queued This IF This payment offer is matched. This suppresses an empty payment task; no independent state invariant requiring immediacy was found.	Review ordinary zero-balance completion and client policy instead of relying on a self-effect suppressing a sibling effect.	This IF MAX 0 Owed<Class<@StandardResource>>:: -This
```

<a id="source-docs-glossary-md"></a>

## docs/glossary.md

Original source: `docs/glossary.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-docs-glossary-md--solarnet-glossary"></a>

### Solarnet glossary

- **agent:** The unique mutation issuer for one Actor in one configured game world. Explicit client requests and autonomous decisions both pass through it; its private agent driver chooses the latter. It is not a task queue or core-engine concept.

- **agent driver:** The private part of an agent that autonomously chooses further legal actions according to installed policies.

- **atomicity:** Indivisibility with respect to a stated scope. One `FROM` is a single transmutation state change, so its removal and gain cannot be observed separately. A timeline operation is failure-atomic: all of its events commit or all are rolled back. An instruction, task, or chain of automatic effects is not thereby one indivisible gameplay event; intermediate state changes may fire effects and be observed by them. Unrelated to atomize, which splits an instruction apart.

- **autoexec:** An agent driver policy that autonomously selects or narrows tasks. It may be proof-preserving, aggressive, or deliberately peculiar; resolution and execution remain ordinary engine consequences.

- **automatic narrowing:** Narrowing performed without an explicit client choice because only one valid option is proved to remain. An unselected task may be narrowed only from immutable facts; resolution against the mutable game world waits for selection.

- **dead end:** A task-pool state from which pending work cannot finish normally. A `Die!` task is an example. An enclosing operation must roll back any route that eventually dead-ends.

- **game world:** The complete live engine state of a game: its component graph, global task queue, event log, timeline, and class table, together with the catalog and immutable premise behind them. ActorAccess, agents, agent drivers, and generic pulse dispatch are configured above that state.

- **game world revision:**

- **operation:** A unit of work with a defined completion boundary. For component-count invariants, one initiating change and all its recursive automatic (`::`) consequences form a full operation. A game action or client transaction may contain several such operations; queued work does not postpone their checks.

- **player-relative observation:**

- **policy-relative stable point:** A coherent game world revision at which every agent driver has inspected that revision and declined to issue another mutation. It depends on the installed policies and does not imply an empty global task queue.

- **resolution:** The engine's interpretation of an instruction against the current game world. It evaluates gates and metrics, checks execution prerequisites, bounds AMAP and optional quantities, translates concrete custom instructions, and performs forced narrowing without making client choices or predicting final invariant satisfaction. Resolution follows selection and repeats after each narrowing.

- **select-lock:** The rule that no competing game world mutation may invalidate the facts used to resolve a selected task before that task finishes.

- **selected task:** The task chosen to finish next. Selection sets `Task.selected`, may change its assignee, and takes the select-lock because resolution has read the current game world. The task may remain abstract and accept partial narrowing from its new assignee.

- **selection:** The client activity that chooses a pending task to finish next, resolves it, and assigns it to its recorded selection assignee. Selection is a promise about ordering, not a timeline commit; commit retains its transactional meaning after execution.

- **task queue:** The one stored, unordered set of pending tasks in a game world. Every task records its assignee; Actor-specific queues are filtered views of that set. Enumeration order has no gameplay meaning.

- **whole-world idleness:**

<a id="source-pets-agents-md"></a>

## pets/AGENTS.md

Original source: `pets/AGENTS.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-agents-md--working-on-pets"></a>

### Working on Pets

- The client may prune choices that later fail, or expose those choices. That alone is not a
  language or model defect.

<a id="source-pets-todo-md"></a>

## pets/TODO.md

Original source: `pets/TODO.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-todo-md--todo"></a>

### TODO

- Refresh the historical catalog claims in [monotonicity analysis](pets/MONOTONICITY.md),
  especially Prelude 2 producer chains and generic-resource targets. Its retained scan predates
  the current content inventory; the documentation move did not rerun that analysis.

<a id="source-pets-docs-readme-md"></a>

## pets/docs/README.md

Original source: `pets/docs/README.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-docs-readme-md--pets-documentation"></a>

### Pets documentation

<a id="source-pets-docs-readme-md--development-and-evidence"></a>

#### Development and evidence

- [Historical effect walkthrough: declarations and specialization](pets/life-of-an-effect.md)

<a id="source-pets-docs-agents-game-hacks-md"></a>

## pets/docs/agents/GAME_HACKS.md

Original source: `pets/docs/agents/GAME_HACKS.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-docs-agents-game-hacks-md--game-hacks"></a>

### Game hacks

<a id="source-pets-docs-agents-game-hacks-md--inclusion-rule"></a>

#### Inclusion rule

**An exposed dead end is not a hack or defect.** Apply the
[decision-tree principle](../../../pets/AGENTS.md):
the player's agent or client handles pruning, or we accept unsuccessful paths. Do not add an entry
merely because a choice fails later or requires rollback, even if no pruner exists. Protected
Habitats is valid as modeled; it needs no precheck to prevent an attempted plant theft.

A **truthful** implementation makes the game changes its rules actually call for. Another card
can observe them without needing special knowledge of its implementation. Rejecting an illegal
operation by rolling back its changes, consequences, and history counts as truthful: Protected
Habitats does not leave a successful history containing the prohibited theft.

<a id="source-pets-docs-agents-game-hacks-md--resource-bookkeeping-and-narrow-watchers"></a>

#### Resource bookkeeping and narrow watchers

<a id="source-pets-docs-agents-game-hacks-md--7-l1-trade-terminals-quota-counts-unrelated-resource-gains"></a>

##### 7. L1 Trade Terminal's quota counts unrelated resource gains

**Fan card — Microbial Symbiosis:** “Whenever you gain a microbe, gain an animal.” **Literal:**
`Microbe: Animal`. With suitable holders, adding a Terminal microbe also adds an animal. The bonus
animal consumes another Terminal allowance. A valid three-card distribution can then dead-end,
or the player can satisfy the counter with fewer Terminal gifts than the card requires.

<a id="source-pets-docs-agents-game-hacks-md--acquisition-events-standing-in-for-particular-game-actions"></a>

#### Acquisition events standing in for particular game actions

<a id="source-pets-docs-agents-game-hacks-md--11-an-action-use-event-occurs-before-its-cost-succeeds"></a>

##### 11. An action-use event occurs before its cost succeeds

**Hack:** `UseAction` starts the cost and result, rather than recording successful payment of the
cost. Another listener can provide the very resources needed to make the attempted action legal.
Rollback does not help when those resources turn the whole operation into a successful one.

<a id="source-pets-docs-agents-payments-md"></a>

## pets/docs/agents/PAYMENTS.md

Original source: `pets/docs/agents/PAYMENTS.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-docs-agents-payments-md--authored-payment-model"></a>

### Authored payment model

> **Status:** current Canon payment-scope and acceptance protocol.
> Client payment helpers, allocation checks, and rollback evidence belong to
> [Solarnet payment integration](../PAYMENTS.md).

<a id="source-pets-docs-agents-payments-md--current-contract"></a>

#### Current contract

Ordinary resource losses never credit a bill, even when performed by the payer while acceptance
is open. Payment signals are player-owned and are not `System`: choosing a payment remains player
work, and `NONE` does not execute it automatically. The offered exchange fixes the payment resource
or card holder; clients cannot turn a different resource removal into payment credit.

With player autoexecution set to `NONE`, the zero-balance acknowledgement also remains explicit.
Skipping such work may be a future autoexecution policy; it must not override `NONE`.
Both fixed and variable standard-resource action costs use the payment scope so acceptance and price
adjustments can apply. A variable cost keeps its shared `X` in the sequence from `Owed` through
`PaymentScope` to the action's result. Choosing `X = 4` for `X Energy -> X MC` opens a scope for 4 Energy;
only after settlement does the sequence offer 4 M€. Energy Market's `2X MC -> X Energy` can
therefore accept Heat through FakeHelion's ordinary payment-scope listener instead of forcing an M€ loss.

<a id="source-pets-docs-agents-readme-md"></a>

## pets/docs/agents/README.md

Original source: `pets/docs/agents/README.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-docs-agents-readme-md--pets-agent-documentation-router"></a>

### Pets agent documentation router

Before behavior or architecture changes, read [VALUES.md](../../../pets/docs/agents/VALUES.md). Keep bounded unfinished work
in [TODO.md](../../../pets/TODO.md). Never turn an exposed dead end into a model defect merely because a
client has not implemented pruning.

<a id="source-pets-docs-agents-readme-md--canon-and-derived-views"></a>

#### Canon and derived views

| Subject | Read |
| --- | --- |
| Authored actions, action identity, or missing action slots | [ACTIONS.md](../../../pets/docs/agents/ACTIONS.md) |
| Turn offers, Head Start, phases, and authored game flow | [GAME_FLOW.md](../../../pets/docs/agents/GAME_FLOW.md) |
| Card backs, locations, offers, and searches | [CARD_MODEL.md](../../../pets/docs/agents/CARD_MODEL.md) |
| Payment signals, pricing, billing, acceptance, or action-cost lowering | [PAYMENTS.md](../../../pets/docs/agents/PAYMENTS.md) |
| Proposed fallback instructions | [ELSE.md](pets/ELSE.md) |
| Canon invariants and historical composition hazards | [GAME_HACKS.md](../../../pets/docs/agents/GAME_HACKS.md) |
| Class names and localized display names | [NAMING.md](../../../pets/docs/agents/NAMING.md) |
| English rendering, vocabulary, wording, or card layout | [LANGUAGE.md](../../../pets/docs/agents/LANGUAGE.md); [NLG_REVIEW.md](../../../pets/docs/agents/NLG_REVIEW.md) is a review snapshot |
| Almanac columns, navigation, and images | [ALMANAC.md](../../../pets/docs/agents/ALMANAC.md) |
| Generated Kotlin vocabulary | [PETS_TYPE_GENERATOR.md](../../../pets/docs/agents/PETS_TYPE_GENERATOR.md) |
| Generated map areas | [MAP_PETS_GENERATION.md](../../../pets/docs/agents/MAP_PETS_GENERATION.md) |
| Canon XPath queries, authored/elaborated views, or CLI | [Canon XPath](../../../pets/docs/agents/canon-xpath.md); multiplatform module `search`, source and tests under `dev/martianzoo/search` |
| Static resource monotonicity report | [MONOTONICITY.md](pets/MONOTONICITY.md) |

<a id="source-pets-docs-agents-readme-md--verification-and-maintenance"></a>

#### Verification and maintenance

Runtime tasks, Actor assignment, consequence calculation, automatic policy, rollback, and replays
belong to the [Solarnet router](../README.md). Gameplay evidence can live there
without becoming a dependency of the static build.

<a id="source-pets-docs-agents-review-criteria-md"></a>

## pets/docs/agents/REVIEW_CRITERIA.md

Original source: `pets/docs/agents/REVIEW_CRITERIA.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-docs-agents-review-criteria-md--final-review-criteria-for-pets"></a>

### Final review criteria for Pets

- Do not report exposed dead ends, later failure, or caller rollback as a model defect without
  a separate game-rule failure. Protected Habitats may reject an attempted theft through its
  consequence; the client may prune that choice or expose it.

<a id="source-pets-docs-agents-testing-md"></a>

## pets/docs/agents/TESTING.md

Original source: `pets/docs/agents/TESTING.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-docs-agents-testing-md--testing-and-verification-in-pets"></a>

### Testing and verification in Pets

<a id="source-pets-docs-agents-testing-md--test-design"></a>

#### Test design

Keep language/type conformance tests aligned with numbered specification rules. Use minimal
examples that demonstrate information preservation without reproducing engine execution.
Actual card outcomes, live-world rollback, and full games are tested by Solarnet.
Share a test fixture only when it belongs to both consumers; do not depend on Solarnet test support.
Give distinct scenario variants named tests so a failure identifies the case.

<a id="source-pets-docs-agents-values-md"></a>

## pets/docs/agents/VALUES.md

Original source: `pets/docs/agents/VALUES.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-docs-agents-values-md--pets-modeling-values"></a>

### Pets modeling values

<a id="source-pets-docs-agents-values-md--model-the-game-honestly"></a>

#### Model the game honestly

**Assume client-side pruning, or accept dead ends.** Every Pets and engine design must follow
the [decision-tree principle in AGENTS.md](../../../pets/AGENTS.md).
The player's agent or client either presents only choices known to permit turn completion, or
exposing unsuccessful paths is acceptable. Missing pruning does not create a model requirement.
Protected Habitats may let an opponent attempt a plant removal and then dead-end the turn,
requiring rollback. That is valid rule enforcement, not a modeling defect or a fidelity compromise.

<a id="source-pets-docs-pets-tricks-hacks-cheats-md"></a>

## pets/docs/pets-tricks-hacks-cheats.md

Original source: `pets/docs/pets-tricks-hacks-cheats.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--pets-tricks-hacks-and-cheats"></a>

### Pets tricks, hacks, and cheats

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--6-let-an-illegal-event-happen-then-make-the-operation-dead-end"></a>

#### 6. Let an illegal event happen, then make the operation dead-end

A `Die` consequence can reject a path *after* its triggering change. An automatic `:: Die`
fails within the triggering operation; a queued `: Die` can expose the dead end later. Failure
rolls back its encompassing transaction; if earlier commands already succeeded, the caller must
roll back farther to abandon the unsuccessful path, potentially the turn.

This is valid modeling. The engine and model enforce the rule without prechecking every path
that could violate it. Universally assume either that the player's agent or client presents only
choices known not to dead-end the turn, or that exposing dead ends is acceptable. Missing pruning
does not make this a defect or require a different card model. The prohibited result must not
survive successful turn completion.

- **Protected Habitats:** [`-Plant OR -Animal OR -Microbe BY Player(NOT Me@): Die`](../../../pets/src/common/dev/martianzoo/tfm/canon/bundles/corpera/cards.json5) queues a consequence of an opponent's attack regardless of which action caused the removal. An opponent may attempt to take a plant, but that path dead-ends and requires rollback. There is nothing wrong with exposing that choice.

- **Global parameter completion:** [`-@GlobalParameter:: Die`](../../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/board.pets) rejects any later attempt to lower a completed track, no matter which card or event requested it.

The same rollback pattern protects the Mars Nomads marker through its ordinary `: Die` effect.
It depends on transaction rollback, not on `Die` somehow preventing the triggering event from starting.

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--7-use-a-zero-or-one-metric-as-an-if-without-a-branch"></a>

#### 7. Use a zero-or-one metric as an `if` without a branch

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--8-make-a-fallback-arm-available-only-when-its-exception-applies"></a>

#### 8. Make a fallback arm available only when its exception applies

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--9-use--to-opt-in-to-a-gains-hidden-placement-rule"></a>

#### 9. Use `<>` to opt in to a gain's hidden placement rule

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--10-put-an-amount-policy-on-the-class-then-omit-it-everywhere-else"></a>

#### 10. Put an amount policy on the class, then omit it everywhere else

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--11-make-a-refinements-candidate-fill-an-unspoken-dependency"></a>

#### 11. Make a refinement's candidate fill an unspoken dependency

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--12-use-each-to-turn-a-type-query-into-a-family-of-effects"></a>

#### 12. Use `EACH` to turn a type query into a family of effects

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--13-let-an-inherited-this-rule-cover-every-subclass"></a>

#### 13. Let an inherited `This` rule cover every subclass

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--14-bind-a-count-once-with-x"></a>

#### 14. Bind a count once with `X`

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--15-count-distinct-kinds-by-counting-class-components"></a>

#### 15. Count distinct kinds by counting `Class` components

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--16-repeat-a-header-variable-to-force-two-dependencies-to-agree"></a>

#### 16. Repeat a header variable to force two dependencies to agree

<a id="source-pets-docs-pets-tricks-hacks-cheats-md--17-replace-a-one-change-or-ok-with-"></a>

#### 17. Replace a one-change `OR Ok` with `?`

<a id="source-pets-docs-type-system-md"></a>

## pets/docs/type-system.md

Original source: `pets/docs/type-system.md` at `ccfd949f82cb64f291c34f8d47dffbd4b15a0054`.

<a id="source-pets-docs-type-system-md--type-system"></a>

### Type system

<a id="source-pets-docs-type-system-md--classes-and-types"></a>

#### Classes and types

<a id="source-pets-docs-type-system-md--uninhabited-types-the-jackalope-example"></a>

##### Uninhabited Types: the jackalope example

Once premise construction leaves a trigger Class uninhabited, this last rule lets declarations
mention optional concepts without importing them into every game. An instruction guarded by "if a
Jackalope ever appears" is perfectly safe: execution can never reach its body. Conversely, an
instruction that actually tries to create an uninhabited Jackalope cannot succeed. Optional and
AMAP changes to uninhabited Types become zero no-ops; mandatory changes are dead ends.

<a id="source-pets-docs-type-system-md--types-and-dependencies"></a>

#### Types and dependencies

<a id="source-pets-docs-type-system-md--invariants-and-operation-completion"></a>

##### Invariants and operation completion

A class-declared count invariant states what must be true when a full operation completes. For this
check, an operation includes one initiating component change and all its recursive automatic (`::`)
consequences. Intermediate counts may be outside the declared bounds. For example, a mandatory gain
can add a new member of a maximum-one family whose automatic effect removes the old member. The
engine validates the resulting counts before completing the operation; if they still violate an
invariant, it rolls back the transaction, including its changes, tasks, and events.

An available choice is not a promise that the operation will succeed. The engine can offer a
mandatory change that will fail its count invariants at completion; the failed attempt rolls back.
Resolution does not execute consequences to predict that result or use count bounds to remove
mandatory alternatives. Clients and agents may explore and prune these dead ends for the player.
