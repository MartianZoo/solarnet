# Self-running phases

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing high-level phase progression, compiling expansion phase order, changing
> queue-drain cleanup, phase lifetime, or phase and turn continuations.
>
> **Skip when:** changing work performed inside one phase without changing how that phase begins or
> ends.
>
> **Status:** current runtime model. Once `WorkflowStarted` exists, phase-owned rules carry
> Bootstrap-to-Setup-to-Corporation, the compiled Corporation-to-Action and Solar segments, the
> recurring Production-to-Research-to-Action cycle, and the final transition from Final Greenery
> to End.
> Corporation, Prelude, Action, and Final Greenery turn order and completion follow from Pets state.
> Clients start the game with the Admin operation `WorkflowStarted`; no Kotlin runner is retained.
> `GenerationScope` and dependency-ordered idle cleanup are implemented beneath that proof.

## Purpose and scope

The high-level workflow runs through ordinary Pets components. After one explicit start, no
orchestrator inspects the current phase to decide what comes next. Each Phase owns its transition
rules and is itself the lifetime anchor for phase-local state.

`AdvancePhase<Phase>` requests advancement after pending work settles and the initiating operation
validates. It is a generic `Continuation`, not a second lifetime scope. Setup, Production, Solar,
and Research request it on entry; Corporation, Prelude, Action, and Final Greenery request it only
when their domain completion condition holds. Empty queues between player turns do not complete
those phases.

Phase transitions and within-phase turn sequencing remain separate responsibilities, both authored
in Pets. No Kotlin runner is retained.

## Current foundation

The required primitives already exist:

- [`Engine.newGame`](../../src/common/dev/martianzoo/engine/Engine.kt) completes and commits
  bootstrap before returning.
- Admin creates `BootstrapPhase` before the generated `Premise`; bootstrap begins and ends with
  that same Phase.
- [`Phase`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/game.pets) is legitimate
  Game World state, with exactly one Phase present.
- Pets Type arguments are component dependencies. Removing a dependency cascades through its
  dependents before removing the dependency itself.
- [`WorldTransaction`](../../src/common/dev/martianzoo/engine/WorldTransaction.kt) performs
  idle cleanup after an outer operation and its automatic effects have completed. Nested calls
  share the transaction. The resulting position is recorded after settlement and continuation
  cleanup, with no separate callback or callback-driven follow-up.
- A `Continuation` is removed inside that same transaction, but only after the operation validates
  complete. Its removal can therefore begin follow-up work without making that work an unfinished
  obligation of the operation that created the continuation.
- [`Engine.removeIdleComponent`](../../src/common/dev/martianzoo/engine/Engine.kt) removes
  one eligible Type from the requested cleanup class at an empty task queue, deferring Types with
  direct or indirect dependent `MustCleanUp` or that same cleanup class. `WorldTransaction` offers
  `Temporary` cleanup before validation and `Continuation` cleanup afterward, settling effects
  and rechecking the live queue and dependencies after each removal.
- `GenerationScope` is a singleton lifetime anchor created naturally by the first `Generation` and
  replaced by each later one; generation-local state depends on it instead of listening
  independently for the next Generation.
- `TemporaryScope<Parent>` is both a child `Scope` and a `Temporary`; it therefore depends on its
  parent and is mandatory cleanup removed only after its own dependent cleanup finishes.

Clients begin automatic play with `agents[ADMIN].beginOperation("WorkflowStarted")`. Phase-owned
and game-mode-owned Pets rules choose the transitions and grant player work. Lower-level explicit
phase operations remain available in the current engine, without a retained workflow object.
[COLOR_MODES.md](COLOR_MODES.md) owns the intended REPL controls and their implementation gaps.

Setup and Research are simultaneous player-work windows. Setup offers each Player two anonymous
standard corporation backs in `Selecting`. The Player moves one to `Hand` and discards the other;
with `BeginnerVariant`, they may instead take a beginner back and discard both standard offers.
Setup then creates `NewTurn`. Prelude independently gains four generic Prelude backs in `Hand` and
asks the Player to remove two. Every Player queue may remain active together. Research offers four
anonymous project backs in `Selecting`; the Player discards unwanted backs and buys all that remain.
It waits for whole-World idleness rather than imposing seat order. Corporation and Prelude phases
retain ordered turns. The client supplies exact faces when playing generic backs. Rejected offers
have modeled counts and movements, but no identities.

## Runtime model

A Phase is both the visible statement of where the game is and the lifetime anchor for its work.
There is no separate `FooPhaseScope`. A component limited to a phase depends directly on that Phase.
The Phase's successor is behavior, not part of its type identity.

```pets
CLASS AdvancePhase<Phase> : Continuation, System { HAS MAX 1 This }

CLASS ResearchPhase : Phase {
  This IF WorkflowStarted:: AdvancePhase<This>
  This:: Generation
  -AdvancePhase<This> IF WorkflowStarted:: ActionPhase FROM This
}
```

The lifecycle uses ordinary engine behavior:

1. Entering Research requests advancement and queues each Player's research work.
2. While any task remains, cleanup cannot remove the advancement request.
3. Temporary cleanup settles and the initiating operation validates.
4. Continuation cleanup removes `AdvancePhase<ResearchPhase>`.
5. Research's automatic effect performs `ActionPhase FROM This`.
6. Action entry resets participation and grants the first turn.

The phase transition preserves exactly one Phase. `End` is terminal and requests no advancement.
`AdvancePhase` is needed because phases must remain present during manual play, and starting the
next phase must not become an unfinished obligation of the preceding player operation. Making
phases themselves `Temporary` or `Continuation` would remove them even without `WorkflowStarted`.

Dependent teardown must not release unordered queued work that races a phase transition. Required
inner cleanup must finish before requesting advancement, or depend on the request so the existing
cleanup rule waits for it. The continuation is a completion request, not an additional lifetime
anchor to which ordinary phase-local state should attach.

## Bootstrap and the one explicit start

Bootstrap remains quiescent after initialization. Starting automatic play is one explicit Pets
operation:

```pets
CLASS WorkflowStarted : System {
  HAS MAX 1 This
  This:: AdvancePhase<BootstrapPhase>
}
```

Without that operation, `Engine.newGame` ends at the committed `BootstrapPhase`. With it,
continuation cleanup advances Bootstrap to Setup. Setup requests advancement on entry and waits for
all setup work to settle before entering Corporation. Manual play creates neither request.

## Authored topology and compiled Pets

Expansion authors should state only their own relative-order requirements. They should not replace
a base transition or describe a complete ordering that includes other optional expansions.

The workflow topology is cyclic, so precedence is local to a named segment rather than one global
order. Each segment-starting Phase declares its endpoint through the optional `phaseSegment`
property; the declaring Phase is the start. Each expansion Module uses `phaseAfter` to name its
phase first and its required predecessors afterward. Transitive precedence is sufficient:

```pets
ABSTRACT CLASS Phase : System {
  phaseSegment = Requirement?
}
CLASS SolarPhase : Phase {
  phaseSegment = HAS "ResearchPhase"
}
CLASS WorldGovernmentRule : Module {
  phaseAfter = HAS "VenusSolarPhase, SolarPhase"
}
CLASS ColoniesExpansion : Module {
  phaseAfter = HAS "ColoniesSolarPhase, VenusSolarPhase"
}
```

Only selected Modules contribute their optional phases at runtime. Thus the applicable orders are
naturally:

```text
Solar -> Research
Solar -> Venus Solar -> Research
Solar -> Colonies Solar -> Research
Solar -> Venus Solar -> Colonies Solar -> Research
```

`CorporationPhase` similarly declares an Action endpoint, while Prelude contributes only that
`PreludePhase` comes after `CorporationPhase`. Membership in that segment already places Prelude
before the segment's `ActionPhase` endpoint.

A topology compiler gathers those declaration properties across the composed Catalog, proves one
linear order, and appends mutually exclusive Module-gated `-AdvancePhase<This>` effects directly
to each Phase for every possible next phase. Runtime execution neither sorts phases nor interprets
precedence. The properties are authoring metadata, not live Components; ordinary Phase effects and
continuations are the runtime representation. Reconsider live constraint Components only if a
real rule needs to observe or alter phase topology during a game.

Lowering currently runs while `TfmCatalog` constructs its declaration set. The deferred move into
`generateCanonSources` would make the emitted Pets declarations inspectable build artifacts and
keep the topology compiler out of the game runtime.

The compiler contributes only transitions. Authored Phase rules decide when advancement is
requested: Solar phases request it on entry, while Corporation and Prelude use settled card and
turn completion. Compilation adds no classes or cleanup policies. It runs once per Catalog,
independent of `GameConfig`, emitting all Module-gated alternatives.

The compiler must reject:

- a cycle within a segment;
- two incomparable phases that could both be next;
- an absent required segment endpoint;
- a constraint that cannot be reached from exactly one segment start; and
- any compiled nonterminal path lacking exactly one continuation.

Compilation emits requirement-gated Pets for every Module combination in the Catalog. Kotlin does
not retain a runtime topology registry.

## Dynamic paths remain Pets behavior

Static ordering and a game-state-dependent branch are different problems. The topology compiler
orders phases that exist; Pets requirements select a path whose answer depends on current
World state.

Production responds to completion of its advancement request with one of two Pets paths:

```pets
-AdvancePhase<This> IF WorkflowStarted, GameEndBarrier:: SolarPhase FROM This
-AdvancePhase<This> IF WorkflowStarted, MAX 0 GameEndBarrier:: CheckGameEnd
```

In multiplayer, `MultiplayerMode` advances from Production to Final Greenery when it observes
`CheckGameEnd`. In solo play, the selected objective decides whether `CheckGameEnd` creates
`Victory`, and `SoloMode` advances only when that Victory exists. A solo loss deliberately remains
outside final greenery and scoring. These are requirement-gated Pets rules, not a Terraforming Mars
switch statement in the runner.

## Lifetime and participation

`GenerationScope` anchors generation-local state. A Phase directly anchors phase-local state;
no separate phase-scope component is required. Further turn and action lifetime scopes remain
separate sequencing design work, not an implemented universal hierarchy.

`Pass` is the only participation state. Players without Pass remain eligible; the turn rules count
`Player(HAS MAX 0 Pass)` when they need the number still playing. Pass is unique per Player and
persists through Production and Research. Entering the next Action phase removes all Pass
components. It deliberately has no ActionPhase dependency: the representation change does not
change its lifetime or manual availability.

Gaining the final Player's Pass requests Action advancement. Turn continuations stop the rotation
when no eligible Player remains. `ActionTurn` is itself a Continuation and depends directly on
ActionPhase. This permits either eligible cleanup order at the last pass: retiring the turn first
or advancing the phase and removing the turn as a dependent. The old turn cannot survive into the
next Action phase.

`Signal` carries a momentary event and leaves no persistent state. `Temporary` and `Continuation`
remain generic cleanup policies; neither should be confused with phase identity. Cleanup currently
waits for the whole World's task pool to drain, not merely one operation's descendants.

## Current phase proof

`WorkflowStarted` is the explicit opt-in marker. Its Bootstrap advancement request enters Setup;
Setup requests its own advancement after setup work drains.

The implemented recurring and terminal paths are:

```text
Corporation completion -> PreludePhase when selected, or ActionPhase
Prelude completion -> ActionPhase
last Pass gain -> AdvancePhase<ActionPhase> -> ProductionPhase
Production completion -> SolarPhase, while a GameEndBarrier exists
Production completion -> CheckGameEnd, otherwise
Solar completion -> first selected optional Solar phase, or ResearchPhase
Each optional Solar completion -> next selected optional phase, or ResearchPhase
Research completion -> ActionPhase
CheckGameEnd -> FinalGreeneryPhase, when the active GameMode's end condition succeeds
Final Greenery completion -> End
```

Production, Solar, and Research request advancement on entry. Pending phase work keeps their
continuations alive, including optional Production work such as Supercapacitors. Corporation,
Prelude, Action, and Final Greenery wait for domain completion because their queues drain between
players.

Corporation begins with the Start Token owner. Removing a corporation-card back creates a
continuation through the existing `AfterMe` seat relation. Once the choice and consequences settle,
it grants the next Player's turn or requests phase advancement if no corporation backs remain.
Prelude also starts with the Start Token owner. Its continuation grants another turn while that
Player retains a Prelude, advances through `AfterMe` otherwise, and requests advancement when no
Prelude cards remain. Waiting for settlement covers zero-card setups and cards that grant another
Prelude.

Within Action, each first- or second-action marker is a Continuation. It waits for the task pool to
empty and the current operation to validate, then directly starts the next slot or creates
`NextActionTurn` to walk `AfterMe` through passed Players. No intermediate second-action-start
component is needed. A sole active Player receives successive first-action slots, preserving the
one-action turn rule.

Final Greenery begins with the Start Token owner. Its `NewTurn` offers plant conversion or
`FinishFinalGreenery<Owner>`. Choosing conversion creates `FinalGreeneryTurn<Owner>` automatically.
Both are ordinary owned continuations: conversion effects settle and the operation validates before
another turn is granted. Finishing
walks `AfterMe` to the next player; when that seat holds the Start Token, the phase requests advancement to
End. The conversion rule itself supplies costs and bonuses, so sequencing neither counts tiles nor
predicts how many conversions a player can afford. `FinishFinalGreenery` is the explicit player
choice to stop, including when another conversion would be affordable.

Rolling back an operation restores both its effects and the next offered turn. The Final Greenery
scenario in `FinalGreeneryPhaseTest` exercises a non-default first player, normal global-parameter
gains, production, conversion and finish rollback, seat rotation, and scoring without a runner.

Phase advancement does not run without `WorkflowStarted`. Participation states still exist during
stepwise play, but the last Pass does not request advancement. Replay VP snapshots temporarily remove the marker before constructing a
hypothetical Production/End state, then restore the live workflow through rollback.

Removing `WorkflowStarted` stops phase-local continuations from granting later turns. Already
granted tasks remain in Game World and may still be completed or declined. Removing the marker is
therefore insufficient to isolate a manually granted turn from previously pending work.
[COLOR_MODES.md](COLOR_MODES.md) supersedes the former REPL mode guidance here and owns the selected
manual-mode direction, including the removal of blue's `phase` command and unresolved task disposal.

The intended coarse Terraforming Mars shape is:

```text
Bootstrap -> Setup -> Corporation -> [Prelude] -> Action
Action -> Production -> Solar -> [Solar expansion phases] -> Research -> Action ...
Production -> final path -> Final Greenery -> End
```

Setup creates generation 1. Later Research phases create the next Generation. Those are Phase
effects, not extra workflow steps.

## Acceptance criteria

The phase workflow is successful only when all of these hold:

- `Engine.newGame` still returns a committed, task-free `BootstrapPhase`.
- Without an explicit start, the World remains there indefinitely.
- Starting once produces Setup and then every later phase through Pets continuations and effects.
- Exactly one Phase exists throughout committed play; there is no duplicate phase scope.
- Optional phases appear only when their owning Modules are selected.
- Expansion-owned precedence composes without base code naming expansion phases.
- Queue drain cannot remove an outer scope before its dependent mandatory cleanup.
- Rollback restores phases, their dependents, and resulting continuations naturally from the
  event log.
- No coroutine, callback-owned phase switch, runtime topology interpreter, or mirrored Kotlin
  sequence remains.
- Phase-internal turn design can be added through nested scopes without changing these phase-level
  rules.

## Evidence and remaining work

Segment compilation is implemented for Corporation-to-Action with optional Prelude and for
Solar-to-Research with optional Venus, Colonies, and Turmoil phases. Prelude and Corporation turn order
advance through settled card choices and the seat relation, and Action-to-Production closes from its
Player participation state. Final Greenery turn continuations carry the game through scoring.
The topology compiler still lowers declarations during catalog construction; moving that lowering
to generated Canon sources remains separate from runtime sequencing.

Operation-local causal completion remains the separate design in `SEQUENCING.md`. The current
continuations use whole-World idleness; do not describe them as operation-local completion.

Normal-corporation play explicitly excludes `BeginnerCorporation` in addition to standard-back
typing. That is a deliberate statement of the normal path, not redundant routing machinery to
remove during workflow work.

## Remaining completion questions

[SEQUENCING.md](SEQUENCING.md#delegated-operations-and-scheduling-options) owns the shared
scheduling options. These remain open alongside the implemented phase continuations.


Global idleness currently combines several events that need not be equivalent: an operation has no
more work, an event card can leave play, a turn is over, and Admin may advance the phase. Event cards
are Temporary, so unrelated pending work can keep them face up. Conversely, waiting for a particular
Player's queue to drain can miss work delegated to another Player.

Simply adding delayed cleanup or Admin tasks changes the premise of current cleanup: those tasks
keep the global pool nonempty. A priority design must explain how cleanup becomes eligible; keeping
the existing empty-pool condition would stall it.

Nested priority groups are a promising shared option, not a settled representation. The innermost
group could let its tasks remain independently selectable while suspending its caller. Cleanup
included in that group must finish before the caller resumes. Assigning both inner cleanup and
suspended caller work the same numeric rank would release them together. Likewise, event cleanup and phase
advancement cannot merely share a deferred rank if their order matters.

An exclusive operation scope is another option; causal ancestry could supply membership, but alone
supplies neither control nor scheduling. The scope must include relevant work across Actors and
distinguish its cleanup from the continuation that follows it. Component dependencies already express lifetime;
they do not automatically establish that the tasks associated with a component have finished.
