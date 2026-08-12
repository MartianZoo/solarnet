# Native Pets Workflow

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

## Goal

Replace the Terraforming Mars-specific Kotlin workflow with a native workflow whose game-specific
structure and behavior come from Pets declarations. The engine may provide generic task and
workflow semantics, but Kotlin must not actively manage the Terraforming Mars phase sequence or
know which expansions insert phases.

This file is authoritative for phase ordering and terminal behavior.

## Current status

`TfmWorkflow.Auto` currently implements the complete coarse game loop in Kotlin. It starts a
Player's turn operation directly and suspends its coroutine while the **whole Game World** is not idle:
any task or temporary component keeps it waiting. This is sufficient for the current game and is
covered by workflow, card, and whole-game tests. World Government Terraforming, Philares, Splice,
Icy Impactors, and Enceladus are working behaviors; none is waiting on native workflow or a new
delegation mechanism.

The native workflow vocabulary, compiled precedence graph, and control-scope mechanism below are
not implemented. They are an architectural replacement for the Kotlin phase sequencer, not a fix
for those cards. See [IDENTITY.md](IDENTITY.md) for the exact current distinction between task
assignment, instruction-side performer overrides, and the remaining control-until-drain problem.

## Correct phase requirements

The coarse workflow is:

1. `SetupPhase`
2. `CorporationPhase`
3. `ActionPhase`
4. `ProductionPhase`
5. `SolarPhase`
6. `GenerationPhase`
7. `ResearchPhase`, which always continues to `ActionPhase`
8. `FinalGreeneryPhase`
9. `EndPhase`

This list describes the available coarse phases, not one straight-line execution. After
`ActionPhase`, play continues to `ProductionPhase`. `SolarPhase` either ends the game or continues
through the applicable Solar subphases. Normal continuation after the last applicable Solar
subphase advances `Generation`, then goes to `ResearchPhase`, then back to `ActionPhase`.

`SolarPhase` is universal. Although introduced by an expansion in the published rules, Solarnet
treats its game-end-check step as having been retroactively added to the base game's workflow.

### Generation 1

`SetupPhase` must create generation 1 before any player involvement. The first generation has no
`Generation` advance or `ResearchPhase`; setup and corporation selection already serve the relevant
purposes, and follow mode has no meaningful initial deal or research operation to perform.

Consequently the initial path is:

`SetupPhase -> CorporationPhase -> [PreludePhase] -> ActionPhase`

Later `Generation` gains implement the published Player Order phase and pass the first-player
marker. They occur only between generations, immediately before `ResearchPhase`.

### Expansion-owned insertion

Each expansion must declare its own workflow contribution without Kotlin knowing about it:

- `PreludeExpansion` inserts `PreludePhase` after `CorporationPhase`.
- `VenusNextExpansion` inserts `VenusSolarPhase` after `SolarPhase`.
- `ColoniesExpansion` inserts `SolarColoniesPhase` after every member of
  `{SolarPhase, VenusSolarPhase}` that actually exists in this game.
- `TurmoilExpansion` inserts `SolarTurmoilPhase` after every member of
  `{SolarPhase, VenusSolarPhase, SolarColoniesPhase}` that actually exists in this game.

These are precedence constraints over the phases present in one configured game. Merely
expressing a constraint must not cause an absent expansion phase class to be loaded. Thus the
resulting Solar order is always the applicable subsequence of:

`SolarPhase -> VenusSolarPhase -> SolarColoniesPhase -> SolarTurmoilPhase`

Insertion applies only to normal continuation. If `SolarPhase` ends or aborts the game, no later
Solar subphase runs.

### Multiplayer ending

After `ProductionPhase`, `SolarPhase` checks the multiplayer end condition. If it is satisfied,
the workflow continues directly to `FinalGreeneryPhase` and then `EndPhase`; otherwise it
continues through the applicable Solar subphases and into the next generation.

The Venus parameter is not part of the multiplayer end condition, even when Venus Next is in use.
If a later Solar subphase completes the multiplayer end condition, the current Solar phase still
finishes normally and the condition is detected at the next `SolarPhase` game-end check.

### Solo ending and victory

Every solo game plays its full configured generation count. Reaching the victory objective early
does not end the game, including in the TR 63 variant. The terminal condition and the victory
condition are distinct:

- Before the final generation has ended, `SolarPhase` continues regardless of whether the solo
  victory objective has already been achieved.
- At the `SolarPhase` following the final generation's production, the game is terminal and no
  later Solar subphase runs.
- If the configured solo victory objective is satisfied at that point, continue to
  `FinalGreeneryPhase` and then `EndPhase`.
- If the objective is not satisfied, abort the game. Do not run `FinalGreeneryPhase` or
  `EndPhase`, do not perform scoring, and define the result as a loss with score zero.

This intentionally prevents a player who has probably lost from switching to VP optimization, and
prevents final greenery placement from appearing to satisfy a solo objective after the permitted
time has expired.

The solo victory predicate depends on the configured variant. Ordinary solo uses its required
global parameters, Venus solo includes the Venus parameter, and TR 63 solo uses Terraform Rating 63. These predicates affect victory at the terminal check, never the number of generations played.

## Workflow-state principle

Phases or subphases may be game components because they represent the game's current temporal and
rules context. Pending work, readiness, and waiting belong to Tasks and should not be duplicated as
components merely to tell the engine that something remains to be done.

When a future native workflow gives a Player a turn, it needs control-until-drain semantics.
Instruction-side `BY` cannot express this: it changes only who performs an instruction, without
changing the task assignee or creating a control scope. The intended result is that Engine retains
a temporarily nonactionable continuation, consequences of the Player's work remain within the
Player's turn scope, and Engine resumes after that scope and any nested cross-owner choice drain.
The native Workflow must therefore not use whole-world idleness as its completion condition.

Queue suspension is only one candidate representation. The required semantics may instead be
implemented with execution frames or explicit continuations; current task queues have no
suspension relationship.

## Proposed Pets shape

The generic runtime should contribute a small workflow vocabulary:

```pets
"Selects the first step of a native workflow"
ABSTRACT CLASS Workflow<Class<WorkflowStep>> : System

"One durable state in a native workflow"
ABSTRACT CLASS WorkflowStep : System

"A transient signal emitted after one workflow step's control scope drains"
CLASS StepComplete<WorkflowStep> : Signal, System

"A normally contiguous part of a workflow, including its two endpoints"
ABSTRACT CLASS WorkflowSpan<Class<WorkflowStep>, Class<WorkflowStep>> : System

"A forward ordering constraint within one workflow span"
ABSTRACT CLASS WorkflowPrecedence<
    Class<WorkflowSpan>, Class<WorkflowStep>, Class<WorkflowStep>> : System
```

Terraforming Mars selects its entry point, keeps phases as ordinary components, and declares
normal ordering separately and uniformly:

```pets
CLASS TerraformingMarsWorkflow : Workflow<Class<SetupPhase>>, AutoLoad

ABSTRACT CLASS Phase : WorkflowStep, AutoLoad, System {
  HAS =1 Phase

  CLASS SetupPhase
  CLASS CorporationPhase
  CLASS ActionPhase
}

CLASS SetupToCorporation :
    WorkflowSpan<Class<SetupPhase>, Class<CorporationPhase>>

CLASS AfterCorporation :
    WorkflowSpan<Class<CorporationPhase>, Class<ActionPhase>>

CLASS ActionToProduction :
    WorkflowSpan<Class<ActionPhase>, Class<ProductionPhase>>
```

The runner reads the unique `Workflow` component to create its declared first step. After the
step's own effects and delegated control have finished, it gains `StepComplete<the current step>`.
That signal removes itself like every other `Signal`. Unless an ordinary effect performs a
conditional terminal transition, the runner advances through the current span's resolved order,
transmuting the old phase into the next phase. It then waits on the new step. If completion leaves
no successor, the workflow has terminated. More than one immediate successor is invalid.

This completion signal is preferable to a persistent readiness component or a `nextPhase` field:
it records no duplicate workflow state, it preserves the exactly-one-`Phase` invariant during a
normal transition, and requirements on ordinary effects provide branching without a separate
workflow expression language.

An expansion inserts a phase by contributing forward precedence constraints within a core-owned
span. It does not replace the span or modify either endpoint:

```pets
CLASS PreludePhase : Phase

CLASS CorporationBeforePrelude : WorkflowPrecedence<
    Class<AfterCorporation>, Class<CorporationPhase>, Class<PreludePhase>>

CLASS PreludeBeforeAction : WorkflowPrecedence<
    Class<AfterCorporation>, Class<PreludePhase>, Class<ActionPhase>>
```

The base knows only that `CorporationPhase` precedes `ActionPhase`. When Prelude is selected, the
two additional constraints make its phase the unique intermediate step. Every topology relation
points forward; no phase sometimes names a predecessor and sometimes names a successor.

Solar expansion phases use the same scheme within one `SolarPhase`-to-`GenerationPhase` span.
Venus contributes `SolarPhase < VenusSolarPhase < GenerationPhase`; Colonies contributes
`SolarPhase < SolarColoniesPhase < GenerationPhase` plus
`VenusSolarPhase < SolarColoniesPhase`; Turmoil does likewise for every earlier Solar subphase.
The applicable constraints produce the required order without the base rules naming any expansion
phase.

Names used by `WorkflowPrecedence` are **weak class references**: a constraint participates only
when its span and both endpoint phase classes were independently activated by the selected rules.
Reading the constraint must never activate an absent endpoint. This differs intentionally from an
ordinary component dependency, whose existence asserts that its dependency exists. Workflow
topology would, in the aspirational Authority model, be Authority data filtered by the premise
rather than additional Game World state; the Declarations may use ordinary Pets Class syntax, but
the runner should compile them from the active premise classes rather than create their instances
in the Game World.

Dynamic decisions remain ordinary component effects. For example, completion of `SolarPhase` may
transmute directly to `FinalGreeneryPhase` when the game-end requirement is true. Such an explicit
transition supersedes normal advancement through the Solar span. Structural insertion and dynamic
branching therefore remain separate concepts.

```pets
CLASS SolarPhase {
  StepComplete<This> IF LastCall:: FinalGreeneryPhase FROM This
}
```

`StepComplete` does not itself solve player iteration. Action turns, Prelude plays, and final
greenery still require the proposed control-until-drain operation described in `IDENTITY.md`.
Those rules should create ordinary turn work and establish a Player control scope; once that scope
drains, Engine's continuation resumes. The runner must use that completion, not whole-world queue
emptiness, as the condition for emitting `StepComplete`.

## Implementation direction

1. Add the generic workflow vocabulary and compile weak, span-scoped precedence declarations from
   the premise's Authority and active classes without activating their referenced phase classes.
2. Build a generic runner by extracting the lifecycle, checkpoint, cancellation, and wakeup
   mechanics from `TfmWorkflow.Auto`. Prove it with a synthetic linear span, an inserted phase, a
   requirement-selected branch, and termination.
3. Move the coarse Terraforming Mars phase graph into Pets, including expansion-owned precedence
   constraints and the multiplayer/solo terminal branches.
4. Define and implement control-until-drain for native Player turns, preserving the existing
   cross-owner card tests rather than rewriting those cards as prerequisites. Then express
   corporation, Prelude, action, and final-greenery turns as component behavior.
5. Migrate callers to the generic runner and ordinary manual instructions, then delete both
   `TfmWorkflow.Auto` and `TfmWorkflow.Manual`.
