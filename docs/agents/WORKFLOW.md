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
> **Status:** working partial proof. Once `WorkflowStarted` exists, phase scopes now carry the
> Production-to-Solar-to-Research-to-Action cycle. `TfmWorkflow.Auto` still starts and wakes that
> cycle and still owns setup, corporation, Prelude, final greenery, and all intra-phase sequencing.

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
- [`AtomicOperationScope`](../../src/common/dev/martianzoo/engine/AtomicOperationScope.kt) performs
  idle cleanup only after an outer operation and its automatic effects have completed.
- [`Engine.removeTemporaryComponents`](../../src/common/dev/martianzoo/engine/Engine.kt) removes
  `Temporary` components when every task queue is empty. Their removal effects may create more
  work, which Admin autoexecution can settle normally.

[`TfmWorkflow.Auto`](../../src/common/dev/martianzoo/tfm/engine/TfmWorkflow.kt) still listens for
idle completions and resumes a coroutine. It no longer chooses Production, Solar, or Research:
concrete phase scopes make those decisions. The selected design removes the remaining phase-level
control role without replacing the engine primitives above.

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
phase scope just by existing. Starting a configured workflow is one explicit Pets operation that
creates the Bootstrap continuation, conceptually:

```pets
CLASS StartWorkflow : Signal, System {
  This:: BootstrapPhaseScope<BootstrapPhase>
}
```

Without `StartWorkflow`, `Engine.newGame` ends at the committed `BootstrapPhase`. With it,
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

The September 6 `GenerationScope` experiment in `work1` provides concrete evidence for this model.
It replaced `Generational` inheritance with dependencies on a singleton component recreated each
Generation. Removal cascaded through scoped components, and focused tests plus the complete JVM
suite verified cleanup, recreation, bootstrap causality, and rollback. It was then deliberately
parked at the user's request as stash commit `d8a94cc1c`; it was not rejected.

That experiment points toward one compositional family of lifetime anchors:

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

This hierarchy exposes one necessary change to current cleanup. Today all `Temporary` components
are removed in one sweep as soon as all task queues are empty. Making every Phase scope Temporary
would therefore remove an Action scope between turns and advance far too early. Nested drain-woken
scopes instead require dependency-ordered cleanup:

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
first inner scope. A Phase is complete only when its Phase scope's own wake condition is satisfied
and no inner work prevents its removal.

## September 8 proof notes

A deliberately small draft tried to move only Action-to-Production out of `TfmWorkflow.Auto`:

- entering `ActionPhase` created a scope;
- the scope's removal effect queued `ProductionPhase FROM ActionPhase`;
- the final `Pass` removed the scope; and
- the coroutine no longer explicitly entered Production.

The draft first made the continuation type depend on both source and destination. That was needless:
the destination can be fixed or conditional behavior on the concrete source scope. A second draft
used `PhaseScope<Phase>` and `ActionPhaseScope<ActionPhase>`, which is the preferred shape.

The second draft made `Pass` directly depend on `ActionPhaseScope`. Existing bare `Pass` tasks then
reached `Die` in all four focused workflow tests. Changing the all-players-passed requirement did not
fix it. That experiment failed; the scope model did not. The proof now leaves Pass and turn rotation
alone: after the existing Action sequencing observes that every Player has passed, it removes the
already-present `ActionPhaseScope`. Its automatic removal effect enters Production.

The implemented phase tail is:

```text
ActionPhaseScope removal -> ProductionPhase
ProductionPhaseScope removal -> SolarPhase, while a GameEndBarrier exists
ProductionPhaseScope removal -> CheckGameEnd, otherwise
SolarPhaseScope removal -> ResearchPhase
ResearchPhaseScope removal -> ActionPhase
```

`ProductionPhaseScope`, `SolarPhaseScope`, and `ResearchPhaseScope` are Temporary. Pending phase
work keeps cleanup from removing them; when it drains, their automatic removal effects cascade.
This includes optional Production work such as Supercapacitors. `ActionPhaseScope` is deliberately
not Temporary because queues drain between turns.

`WorkflowStarted` is the explicit opt-in marker. It makes phase entry create scopes and enables
their continuations; manual phase operations remain inert without it. Replay VP snapshots
temporarily remove the marker before constructing a hypothetical Production/End state, then restore
the live workflow through rollback.

One lifecycle seam remains in Kotlin. `TfmWorkflow.Auto` removes `ActionPhaseScope` when existing
Action sequencing finishes. At terminal Production it also removes an otherwise task-free
`ProductionPhaseScope`, because this work can occur reentrantly before the outer operation gets its
ordinary Temporary-cleanup pass. If Production created player work, that work drains first and
normal cleanup removes the scope. This is a wakeup-mechanics issue, not a phase decision.

The full JVM suite passes with this proof, including complete replays, rollback-oriented workflow
tests, optional Production tasks, solo win/loss, and multiplayer final greenery.

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

## First demonstration

Continue the narrow proof before migrating the whole game:

1. Characterize and remove the reentrant terminal-Production wakeup seam generically.
2. Verify rollback of a scope removal together with its automatic phase continuation.
3. Characterize dependency-ordered drain cleanup with two nested test scopes.
4. Add a tiny Pets-only chain covering committed Bootstrap, explicit start, Setup, and Corporation.
5. Compile Prelude's weak ordering contribution and verify both active and inactive cases.
6. Compile the Solar constraints and verify every combination of base, Venus, and Colonies phases.
7. Move final greenery and End onto explicit terminal continuations.

Do not redesign Action-turn rotation as part of the Action-to-Production proof; that is sequencing.
If the narrow model needs phase-specific Kotlin, a literal runtime stack, or a second representation
of the next phase, stop and reconsider it rather than expanding the machinery.
