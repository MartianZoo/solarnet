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
> **Status:** selected design direction, not implemented. Committed `TfmWorkflow.Auto` is still a
> Kotlin coroutine that chooses phases and waits for whole-World idleness.

## Purpose and scope

The high-level workflow should run because ordinary Pets components make it run. After one explicit
start, no orchestrator should inspect the current phase and decide what to do next. A phase owns a
lifetime scope; draining the work inside that scope removes it; removing it queues the next phase.

Here, **drained** means that every task queue is empty and no unfinished inner cleanup remains. It
does not mean only that `TaskQueue.isEmpty()` happens to be true.

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
- [`AtomicOperationScope`](../../src/common/dev/martianzoo/engine/AtomicOperationScope.kt) performs
  idle cleanup only after an outer operation and its automatic effects have completed.
- [`Engine.removeTemporaryComponents`](../../src/common/dev/martianzoo/engine/Engine.kt) removes
  `Temporary` components when every task queue is empty. Their removal effects may create more
  work, which Admin autoexecution can settle normally.

Committed [`TfmWorkflow.Auto`](../../src/common/dev/martianzoo/tfm/engine/TfmWorkflow.kt) supplies
the missing phase decisions from Kotlin. It listens for idle completions, resumes a coroutine, and
calls the next phase operation. The selected design replaces that continuing control role, not the
engine primitives above.

## Runtime model

A Phase remains the visible statement of where the game is. A `PhaseScope` is the lifetime anchor
for work belonging to that occurrence of the Phase. A simple continuation scope can be expressed
approximately as follows; the final declaration syntax may differ:

```pets
"The lifetime anchor for the current Phase"
ABSTRACT CLASS PhaseScope<Phase> : Temporary, System {
  HAS MAX 1 PhaseScope
}

"A Phase scope whose removal queues one fixed successor"
CLASS NextPhaseScope<Class<NextPhase>, Phase> : PhaseScope<Phase> {
  -This: NextPhase FROM Phase
}
```

The scope depends on the current Phase; the current Phase does not depend on the scope. Components
whose lifetime is limited to that phase depend on its `PhaseScope`.

For a compiled game in which Prelude is followed by Action, entering Prelude would create something
equivalent to:

```pets
CLASS PreludePhase : Phase {
  This:: NextPhaseScope<Class<ActionPhase>, This>
  // Prelude-owned work
}
```

The resulting lifecycle is entirely ordinary engine behavior:

1. Entering `PreludePhase` creates its scope and queues Prelude-owned work.
2. While any task or inner mandatory cleanup remains, idle cleanup cannot remove the scope.
3. When that work and all inner cleanup finish, idle cleanup removes the scope.
4. Scope-dependent components are removed first by the existing dependency rule.
5. `-This:` queues `ActionPhase FROM PreludePhase` while `PreludePhase` still exists.
6. Admin autoexecution performs that ordinary task.
7. Entering `ActionPhase` creates its own scope and work.

The queued transmutation preserves the exactly-one-Phase rule. `End` is terminal and creates no
successor scope.

Scope-dependent removal must not release unordered deferred work that can race the phase
transition. Teardown is either automatic, or represented by a deeper scope whose completion keeps
the Phase scope from becoming eligible.

## Bootstrap and the one explicit start

Bootstrap must remain quiescent after initialization. It therefore does not create a temporary
phase scope merely by existing. Starting a configured workflow is one explicit Pets operation that
creates the Bootstrap continuation, conceptually:

```pets
CLASS StartWorkflow : Signal, System {
  This:: NextPhaseScope<Class<SetupPhase>, BootstrapPhase>
}
```

Without `StartWorkflow`, `Engine.newGame` ends at the committed `BootstrapPhase`. With it, ordinary
cleanup removes the new scope and queues `SetupPhase FROM BootstrapPhase`. From that point onward,
the generated scopes sustain phase progression themselves. An application API may provide a typed
convenience for issuing `StartWorkflow`, but it owns no continuing runner.

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
produce one unique linear order, and emits the immediate `NextPhaseScope` wiring into Pets Classes.
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
orders phases that exist; ordinary Pets requirements select a path whose answer depends on current
World state.

For example, the scope completing Production may have generated removal effects shaped like:

```pets
-This IF GameEndBarrier: SolarPhase FROM ProductionPhase
-This IF MAX 0 GameEndBarrier: FinalGreeneryPhase FROM ProductionPhase
```

The actual endgame conditions still need to preserve multiplayer and solo rules, but their home is
requirement-gated Pets attached to scope completion, not a Terraforming-Mars switch statement in a
runner. Applicable branches must be exclusive and complete. Terminal outcomes must be explicit;
silently finding no continuation is not how an active workflow ends.

## Scope hierarchy

The `GenerationScope` experiment points toward one compositional family of lifetime anchors:

```text
GameScope
└── GenerationScope
    └── PhaseScope
        └── TurnScope
            └── ActionScope
```

Each child depends on its parent, and the Phase scope also depends on the current Phase. Ordinary
state depends on the narrowest scope matching its true lifetime: an action-local invoice belongs to
the Action scope; a passed marker belongs to the Generation scope; phase-local control belongs to
the Phase scope.

This hierarchy exposes one necessary change to current cleanup. Today all `Temporary` components
are removed in one sweep. Nested scopes instead require dependency-ordered cleanup:

- at an empty queue, remove only Temporary components having no dependent `MustCleanUp`;
- settle any work their removal creates;
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
first inner scope. A Phase is complete only when its Phase scope becomes the innermost removable
Temporary.

The intended coarse Terraforming Mars shape remains:

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

## First proof

Prove the model narrowly before migrating the whole game:

1. Characterize dependency-ordered Temporary cleanup with two nested test scopes.
2. Add a tiny Pets-only chain covering committed Bootstrap, explicit start, Setup, and Corporation.
3. Show that queued work pauses scope removal and that its final completion resumes the chain.
4. Compile Prelude's weak ordering contribution and verify both active and inactive cases.
5. Compile the Solar constraints and verify every combination of base, Venus, and Colonies phases.
6. Add one requirement-gated branch at scope removal.

Do not design Action-turn rotation as part of this proof. If the narrow model needs phase-specific
Kotlin or a second representation of the next phase, stop and reconsider it rather than expanding
the machinery.
