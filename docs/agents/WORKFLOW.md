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
> Bootstrap-to-Setup-to-Corporation, the Production-to-Solar-to-Research-to-Action cycle, and the
> final transition from Final Greenery to End. `TfmWorkflow.Automatic` still wakes Action and Final
> Greenery scopes after their domain sequencing finishes, enters Prelude or Action after
> Corporation, and owns all intra-phase sequencing. `GenerationScope` and dependency-ordered idle
> cleanup are implemented beneath that proof.

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
- [`Phase`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets) is legitimate
  Game World state, with exactly one Phase present.
- Pets Type arguments are component dependencies. Removing a dependency cascades through its
  dependents before removing the dependency itself.
- [`WorldTransaction`](../../src/common/dev/martianzoo/engine/WorldTransaction.kt) performs
  idle cleanup after an outer operation and its automatic effects have completed. An Agent
  operation started synchronously by the completion callback also settles its ordinary idle
  cleanup before returning, while remaining part of the callback's recorded follow-up.
- [`Engine.removeTemporaryComponent`](../../src/common/dev/martianzoo/engine/Engine.kt) removes
  one eligible `Temporary` Type at an empty task queue, deferring Types with direct or indirect
  dependent `MustCleanUp` or `Temporary`. The transaction loop settles removal effects before
  checking the live queue and dependencies for another removal.
- `GenerationScope` is a singleton lifetime anchor replaced by each `Generation`; generation-local
  state depends on it instead of listening independently for the next Generation.

[`TfmWorkflow.Automatic`](../../src/common/dev/martianzoo/tfm/engine/TfmWorkflow.kt) still listens for
idle completions and resumes a coroutine. It does not choose Setup, Corporation, Production, Solar,
Research, Final Greenery, or End: concrete phase scopes and mode-owned Pets rules make those
decisions. The selected design removes the remaining phase-level control role without replacing the
engine primitives above.

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

"The scope belonging to an occurrence of PreludePhase"
CLASS PreludePhaseScope<PreludePhase> : PhaseScope<PreludePhase>, Temporary {
  -This:: ActionPhase FROM PreludePhase
}
```

The scope depends on the current Phase; the current Phase does not depend on the scope. Components
whose lifetime is limited to that phase depend on its `PhaseScope`.

For a compiled game in which Prelude is followed by Action, entering Prelude would create something
equivalent to:

```pets
CLASS PreludePhase : Phase {
  This:: PreludePhaseScope<This>
  // Prelude-owned work
}
```

The resulting lifecycle is entirely ordinary engine behavior:

1. Entering `PreludePhase` creates its scope and queues Prelude-owned work.
2. While any task or inner mandatory cleanup remains, idle cleanup cannot remove the scope.
3. When that work and all inner cleanup finish, idle cleanup removes the scope.
4. Scope-dependent components are removed first by the existing dependency rule.
5. `-This::` performs `ActionPhase FROM PreludePhase` while `PreludePhase` still exists.
6. The automatic effect settles as ordinary Admin work.
7. Entering `ActionPhase` creates its own scope and work.

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
  This:: BootstrapPhaseScope<BootstrapPhase>
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
order. Conceptually, base Terraforming Mars declares a Solar segment from `SolarPhase` to
`ResearchPhase`. Expansions contribute facts such as:

```text
VenusSolarPhase belongs to Solar and is after SolarPhase

ColoniesSolarPhase belongs to Solar and is after SolarPhase
ColoniesSolarPhase is after VenusSolarPhase
```

Only active Phase Classes participate. A constraint mentioning an inactive optional Phase is weak:
it contributes no edge and does not activate that Phase. Thus the active orders are naturally:

```text
Solar -> Research
Solar -> Venus Solar -> Research
Solar -> Colonies Solar -> Research
Solar -> Venus Solar -> Colonies Solar -> Research
```

Prelude similarly contributes that `PreludePhase` belongs to the Corporation-to-Action segment and
comes after `CorporationPhase`. Membership in that segment already places it before the segment's
`ActionPhase` endpoint.

A topology compiler gathers the active members and constraints of each segment, proves that they
produce one unique linear order, and emits concrete Phase-scope removal effects into Pets Classes.
Runtime execution neither sorts phases nor interprets precedence. The expansion constraints are
authoring input and do not also become live Components; the compiled scopes and continuations are
the one runtime representation. Reconsider live constraint Components only if a real rule needs to
observe or alter phase topology during a game.

The compiler must reject:

- a cycle within a segment;
- two incomparable phases that could both be next;
- an absent required segment endpoint;
- a phase placed in incompatible segments; and
- any compiled nonterminal path lacking exactly one continuation.

Whether compilation specializes one Game Premise or emits requirement-gated Pets for every Module
combination is an implementation choice. The resulting World behavior and Pets model must be the
same, and Kotlin must not retain a second topology registry.

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
the Action scope; a passed marker belongs to the Action-phase scope; a genuinely per-generation
marker belongs to the Generation scope; phase-local control belongs to the Phase scope.

`Signal` is the zero-duration edge of this model. It carries no lifetime-scope dependency, triggers
its effects, and removes itself immediately. Do not introduce a live `NoScope` sentinel: absence of
a `Scope` dependency already states that the event owns no interval.

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
ActionPhaseScope removal -> ProductionPhase
ProductionPhaseScope removal -> SolarPhase, while a GameEndBarrier exists
ProductionPhaseScope removal -> CheckGameEnd, otherwise
SolarPhaseScope removal -> ResearchPhase
ResearchPhaseScope removal -> ActionPhase
CheckGameEnd -> FinalGreeneryPhase, when the active GameMode's end condition succeeds
FinalGreeneryPhaseScope removal -> End
```

`ProductionPhaseScope`, `SolarPhaseScope`, and `ResearchPhaseScope` are Temporary. Pending phase
work keeps cleanup from removing them; when it drains, their automatic removal effects cascade.
This includes optional Production work such as Supercapacitors. `ActionPhaseScope` and
`FinalGreeneryPhaseScope` are deliberately not Temporary because their queues drain between
players. `TfmWorkflow.Automatic` removes them only after the existing player sequencing observes phase
completion. The resulting phase decisions remain Pets effects: Kotlin neither checks
`GameEndBarrier` nor directly enters Production, Solar, Research, Final Greenery, or End.

An Agent operation begun synchronously by the atomic-completion callback now receives its own idle
cleanup before returning. That generic rule lets task-free terminal Production settle exactly like
Production with queued work; no workflow-specific wakeup is needed. Rolling back the Action scope
removal restores the Action phase and scope while removing the entire automatic continuation.

Phase-scope continuations remain inert without `WorkflowStarted`. Replay VP snapshots temporarily
remove the marker before constructing a hypothetical Production/End state, then restore the live
workflow through rollback.

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
- Exactly one Phase and at most one active Phase scope exist throughout committed play.
- Optional phases appear only when their Classes are active.
- Expansion-owned precedence composes without base code naming expansion phases.
- Queue drain cannot remove an outer scope before its dependent mandatory cleanup.
- Rollback restores scopes, their dependents, and the resulting continuation naturally from the
  event log.
- No coroutine, callback-owned phase switch, runtime topology interpreter, or mirrored Kotlin
  sequence remains.
- Phase-internal turn design can be added through nested scopes without changing these phase-level
  rules.

## Remaining demonstrations

The next workflow migration is not yet selected. The known candidates are:

- compile Prelude's weak ordering contribution and verify both active and inactive cases; and
- compile the Solar constraints and verify every combination of base, Venus, and Colonies phases.

Do not redesign Action-turn rotation as part of the Action-to-Production proof; that is sequencing.
If the narrow model needs phase-specific Kotlin, a literal runtime stack, or a second representation
of the next phase, stop and reconsider it rather than expanding the machinery.
