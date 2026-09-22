# Self-running phase scopes

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing high-level phase progression, compiling expansion phase order, changing
> queue-drain cleanup, or introducing `GameScope`, `GenerationScope`, `PhaseScope`, `TurnScope`, or
> `ActionScope`.
>
> **Skip when:** changing work performed inside one phase without changing how that phase begins or
> ends.
>
> **Status:** working partial proof. Once `WorkflowStarted` exists, phase scopes carry
> Bootstrap-to-Setup-to-Corporation, the compiled Corporation-to-Action and Solar segments, the
> recurring Production-to-Research-to-Action cycle, and the final transition from Final Greenery
> to End.
> `TfmWorkflow.Automatic` still wakes Corporation, Prelude, and Final Greenery scopes and owns
> Action-turn rotation. The Action phase now closes itself when the last Player passes.
> `GenerationScope` and dependency-ordered idle cleanup are implemented beneath that proof.

## Purpose and scope

The high-level workflow should run because ordinary Pets components make it run. After one explicit
start, no orchestrator should inspect the current phase and decide what to do next. When a Phase is
entered, its way forward already exists as a component. That component is both the lifetime scope
for phase-local state and the continuation whose removal queues the following Phase.

Here, **workflow** means orchestration of whole Phases. Ordering actions, second actions, choices,
and other work within one Phase is **sequencing**. A phase workflow may wait for sequencing to
finish, but it must not absorb that sequencing policy.

Some scopes wake when their owned work is **drained**: every task queue is empty and no unfinished
inner cleanup remains. Others have a domain completion condition. In particular, an Action scope
must survive the ordinary empty-queue moments between turns and wake only when the Action phase is
actually complete, such as when every Player has passed. Raw `TaskQueue.isEmpty()` is therefore not
a universal definition of scope completion.

This document selects the phase-level model. It deliberately does not decide how actions within an
Action phase, second actions within a turn, or player rotation work. Those later designs must
compose through nested scopes rather than add phase-specific control to the workflow.

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
  idle cleanup after an outer operation and its automatic effects have completed. An Agent
  operation started synchronously by the completion callback also settles its ordinary idle
  cleanup before returning, while remaining part of the callback's recorded follow-up.
- A `Continuation` is removed inside that same transaction, but only after the operation validates
  complete. Its removal can therefore begin follow-up work without making that work an unfinished
  obligation of the operation that created the continuation.
- [`Engine.removeTemporaryComponent`](../../src/common/dev/martianzoo/engine/Engine.kt) removes
  one eligible `Temporary` Type at an empty task queue, deferring Types with direct or indirect
  dependent `MustCleanUp` or `Temporary`. The transaction loop settles removal effects before
  checking the live queue and dependencies for another removal.
- `GenerationScope` is a singleton lifetime anchor created naturally by the first `Generation` and
  replaced by each later one; generation-local state depends on it instead of listening
  independently for the next Generation.
- `TemporaryScope<Parent>` is both a child `Scope` and a `Temporary`; it therefore depends on its
  parent and is mandatory cleanup removed only after its own dependent cleanup finishes.

[`TfmWorkflow.Automatic`](../../src/common/dev/martianzoo/tfm/engine/TfmWorkflow.kt) still listens for
idle completions and resumes a coroutine. It does not choose Setup, Corporation, Prelude, Action,
Production, Solar, Research, Final Greenery, or End: concrete phase scopes and mode-owned Pets rules
make those decisions. The selected design removes the remaining phase-level control role without
replacing the engine primitives above.

Setup and Research are simultaneous player-work windows. Setup deals each Player's starting cards
and creates one `NewTurn`. The standard path offers the configured number of corporations, keeps
one, and gives that Player tasks for any rejected starting projects; `BeginnerVariant` instead lets
each Player choose a beginner path that skips both choices. Prelude independently adds the task to
discard exactly two Prelude cards to either path. Every Player queue may remain active together.
Research likewise offers cards to every Player and waits for whole-World idleness rather than
imposing seat order. Corporation and Prelude phases retain ordered turns for playing the cards kept
during Setup.
Normal-corporation offers explicitly exclude `BeginnerCorporation`; their standard card back already
implies the same partition, but the face restriction states the normal-path rule directly.

## Runtime model

A Phase remains the visible statement of where the game is. A `PhaseScope` is the lifetime anchor
for work belonging to that occurrence of the Phase. It is parameterized by the Phase it belongs to,
not by its successor. The successor is behavior on the concrete scope, so conditional successors
remain ordinary `-This IF ...` effects instead of becoming part of type identity. The declaration
can be expressed approximately as follows; final syntax may differ:

```pets
"The lifetime anchor for the current Phase"
ABSTRACT CLASS PhaseScope<Phase> : System {
  HAS MAX 1 PhaseScope
}

"The scope belonging to an occurrence of SolarPhase"
CLASS SolarPhaseScope : PhaseScope<SolarPhase>, Temporary {
  -This:: ResearchPhase FROM SolarPhase
}
```

The scope depends on the current Phase; the current Phase does not depend on the scope. Components
whose lifetime is limited to that phase depend on its `PhaseScope`.

For a compiled game without optional Solar phases, entering Solar creates something equivalent to:

```pets
CLASS SolarPhase : Phase {
  This:: SolarPhaseScope
  // Solar-owned work
}
```

The resulting lifecycle is entirely ordinary engine behavior:

1. Entering `SolarPhase` creates its scope and queues Solar-owned work.
2. While any task or inner mandatory cleanup remains, idle cleanup cannot remove the scope.
3. When that work and all inner cleanup finish, idle cleanup removes the scope.
4. Scope-dependent components are removed first by the existing dependency rule.
5. `-This::` performs `ResearchPhase FROM SolarPhase` while `SolarPhase` still exists.
6. The automatic effect settles as ordinary Admin work.
7. Entering `ResearchPhase` creates its own scope and work.

The queued transmutation preserves the exactly-one-Phase rule. `End` is terminal and creates no
successor scope.

Scope-dependent removal must not release unordered deferred work that can race the phase
transition. Teardown is either automatic, or represented by a deeper scope whose completion keeps
the Phase scope from becoming eligible.

### Why the Phase itself is not the scope

It would be attractive for `ActionPhase` itself to own phase-local components and have
`-This:: ProductionPhase`. The exactly-one-`Phase` invariant creates an insertion problem: removing
`ActionPhase` alone is invalid, while its removal effect cannot run until after that invalid removal.
The legal transition is the atomic `ProductionPhase FROM ActionPhase` transmutation.

A separate scope resolves this without a special engine operation. It depends on `ActionPhase` and
is removed while `ActionPhase` still exists. Its removal effect can therefore queue the legal
transmutation. The engine already removes the scope's dependents before retrying the scope removal,
so the same component is useful throughout its life rather than being only a continuation token.

### Waking scopes

There is no reason every Phase scope must extend `Temporary` or share one wake policy:

- a setup-like scope may be eligible when its queue and inner cleanup drain;
- an Action scope is explicitly removed when every Player has passed;
- a scope with multiple successors selects exactly one using `-This IF ...`; and
- a terminal scope may remove itself without creating a successor only when termination is explicit.

This resembles a stack of queue-empty triggers, but it should not become a second runtime stack.
Dependencies already represent the nesting. At a quiescent point the innermost eligible scope is
removed, its effects settle, and only then can an enclosing scope become eligible. A wake condition
chooses when to request removal; dependency ordering chooses what must finish first.

## Bootstrap and the one explicit start

Bootstrap must remain quiescent after initialization. It therefore does not create a temporary
phase scope just by existing. Starting a configured workflow is one explicit Pets operation.
Creating `WorkflowStarted` creates the Bootstrap continuation:

```pets
CLASS WorkflowStarted : System {
  This:: BootstrapPhaseScope
}
```

Without `WorkflowStarted`, `Engine.newGame` ends at the committed `BootstrapPhase`. With it, cleanup
removes `BootstrapPhaseScope` and enters Setup. `SetupPhaseScope` then survives until setup work
drains and enters Corporation. The automatic workflow issues only that explicit start; it does not
perform either phase transition.

## Authored topology and compiled Pets

Expansion authors should state only their own relative-order requirements. They should not replace
a base transition or describe a complete ordering that includes other optional expansions.

The workflow topology is cyclic, so precedence is local to a named segment rather than one global
order. Each segment-starting Phase declares its endpoint through the optional `phaseSegment`
property. Each expansion Module uses `phaseAfter` to name its phase first and every weaker
predecessor after it:

```pets
ABSTRACT CLASS Phase : System {
  phaseSegment = Requirement?
}
CLASS SolarPhase : Phase {
  phaseSegment = HAS "SolarPhase, ResearchPhase"
}
CLASS WorldGovernmentRule : Module {
  phaseAfter = HAS "VenusSolarPhase, SolarPhase"
}
CLASS ColoniesExpansion : Module {
  phaseAfter = HAS "ColoniesSolarPhase, SolarPhase, VenusSolarPhase"
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
linear order, and emits concrete Phase-scope Classes plus mutually exclusive Module-gated removal
effects for every possible next phase. Runtime execution neither sorts phases nor interprets
precedence. The properties are authoring metadata, not live Components; the compiled scopes and
continuations are the one runtime representation. Reconsider live constraint Components only if a
real rule needs to observe or alter phase topology during a game.

The branch currently performs this lowering while `TfmCatalog` constructs its declaration set.
That is transitional. Move topology lowering into `generateCanonSources` so the emitted Pets
declarations are build artifacts that tools and humans can inspect, and so no topology compiler is
shipped as part of the game runtime.

By default the compiler emits a `Temporary` scope. A Phase whose domain sequencing becomes idle
before the Phase is complete can instead declare its own direct `PhaseScope<ThatPhase>` subtype.
The compiler adds the same continuation effects to that authored scope without changing its wake
policy. Corporation and Prelude use this path because their ordered player turns have idle gaps.

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

The scope completing Production currently takes one of two Pets paths:

```pets
-This IF GameEndBarrier:: SolarPhase FROM ProductionPhase
-This IF MAX 0 GameEndBarrier:: CheckGameEnd
```

In multiplayer, `MultiplayerMode` advances from Production to Final Greenery when it observes
`CheckGameEnd`. In solo play, the selected objective decides whether `CheckGameEnd` creates
`Victory`, and `SoloMode` advances only when that Victory exists. A solo loss deliberately remains
outside final greenery and scoring. These are requirement-gated Pets rules, not a Terraforming Mars
switch statement in the runner.

## Scope hierarchy

The implemented `GenerationScope` and nested-cleanup rule establish the first level of one
compositional family of lifetime anchors:

```text
GameScope
└── GenerationScope
    └── PhaseScope
        └── TurnScope
            └── ActionScope
```

Each child depends on its parent, and the Phase scope also depends on the current Phase. Ordinary
state depends on the narrowest scope matching its true lifetime: an action-local invoice belongs to
the Action scope; a genuinely per-generation marker belongs to the Generation scope; phase-local
control belongs to the Phase scope. The exact-one `ActionPhaseStatus` is persistent Player state:
every Player is either `HaveNotPassed` or `Pass`, and entering Action resets all passed Players. It
can eventually depend on `GameScope`, but it cannot depend on the shorter Action scope while
remaining a globally valid exact-one sum.

`Signal` is the zero-duration edge of this model. It carries no lifetime-scope dependency and leaves
no persistent state. Do not introduce a live `NoScope` sentinel: absence of a `Scope` dependency
already states that no enclosing interval is needed.

The engine applies dependency-ordered cleanup needed by this hierarchy:

- at an empty queue, remove one Temporary Type with no direct or indirect dependent `MustCleanUp` or `Temporary`;
- settle any work its removal creates and recheck the live dependencies;
- repeat only if the queue remains empty; and
- never remove an outer scope while a live inner scope or other mandatory cleanup depends on it.

This is a generic cleanup rule, not workflow scheduling. It allows an Action scope to finish while
its Turn, Phase, Generation, and Game scopes remain. The exact creation and continuation rules for
Action and Turn scopes are intentionally deferred. It also aligns with the cleanup simplification
in [`SEQUENCING.md`](SEQUENCING.md#cleanup-vocabulary): `Temporary` is a removal policy for one kind
of `MustCleanUp`, not a competing statement of completion.

## Phase ownership

The workflow mechanism knows nothing about setup choices, player order, actions, production,
expansion rules, or scoring. Entering a Phase must create either all of that Phase's work or its
first inner scope. A Phase is complete only when its Phase scope's own wake condition is satisfied
and no inner work prevents its removal.

## Current phase proof

`WorkflowStarted` is the explicit opt-in marker. It creates a temporary Bootstrap scope whose
removal enters Setup. Setup creates its own temporary scope; starting-project and other setup work
keeps that scope alive until the World is idle, when its removal enters Corporation.

The implemented recurring and terminal paths are:

```text
CorporationPhaseScope removal -> PreludePhase when selected, or ActionPhase
PreludePhaseScope removal -> ActionPhase
last Pass -> ActionPhaseComplete; completion removal -> ProductionPhase
ProductionPhaseScope removal -> SolarPhase, while a GameEndBarrier exists
ProductionPhaseScope removal -> CheckGameEnd, otherwise
SolarPhaseScope removal -> first selected optional Solar phase, or ResearchPhase
Each compiled optional Solar scope -> next selected optional phase, or ResearchPhase
ResearchPhaseScope removal -> ActionPhase
CheckGameEnd -> FinalGreeneryPhase, when the active GameMode's end condition succeeds
FinalGreeneryPhaseScope removal -> End
```

`ProductionPhaseScope`, every compiled Solar scope, and `ResearchPhaseScope` are Temporary. Pending
phase work keeps cleanup from removing them; when it drains, their automatic removal effects
cascade. This includes optional Production work such as Supercapacitors. Corporation, Prelude,
Action, and Final Greenery scopes are deliberately not Temporary because their queues drain between
players. The last `HaveNotPassed -> Pass` transition changes `ActionPhaseScope` into the
`ActionPhaseComplete` continuation. The engine removes it after the passing operation validates,
and its Pets removal effect enters Production. Kotlin still rotates Action turns, but no longer
decides that the phase is complete or removes its scope. Corporation, Prelude, and Final Greenery
remain explicitly woken by their sequencing.

An Agent operation begun synchronously by the atomic-completion callback now receives its own idle
cleanup before returning. That generic rule lets task-free terminal Production settle exactly like
Production with queued work; no workflow-specific wakeup is needed. Rolling back the Action scope
completion restores the Action phase and open scope while removing the entire automatic
continuation.

Phase-scope continuations do not advance without `WorkflowStarted`. An Action scope and its
participation states still exist during stepwise play, but the last Pass does not create the
completion continuation. Replay VP snapshots temporarily remove the marker before constructing a
hypothetical Production/End state, then restore the live workflow through rollback.

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
- Starting once produces Setup and then every later phase through Pets scopes and effects.
- Exactly one Phase and at most one live Phase scope exist throughout committed play.
- Optional phases appear only when their owning Modules are selected.
- Expansion-owned precedence composes without base code naming expansion phases.
- Queue drain cannot remove an outer scope before its dependent mandatory cleanup.
- Rollback restores scopes, their dependents, and the resulting continuation naturally from the
  event log.
- No coroutine, callback-owned phase switch, runtime topology interpreter, or mirrored Kotlin
  sequence remains.
- Phase-internal turn design can be added through nested scopes without changing these phase-level
  rules.

## Remaining demonstrations

Segment compilation is implemented for Corporation-to-Action with optional Prelude and for
Solar-to-Research with optional Venus and Colonies phases. Action-to-Production now closes from its
Pets-owned exact-one participation states. The remaining Kotlin role is intra-phase player
sequencing and explicit wakeup of Corporation, Prelude, and Final Greenery.

Do not redesign Action-turn rotation as part of the Action-to-Production proof; that is sequencing.
If the narrow model needs phase-specific Kotlin, a literal runtime stack, or a second representation
of the next phase, stop and reconsider it rather than expanding the machinery.
