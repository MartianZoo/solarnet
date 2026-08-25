# Native Pets workflow

**Status:** Phase requirements below are settled domain behavior. The native vocabulary and runner
are a proposal. Committed `TfmWorkflow.Auto` still sequences phases in Kotlin and waits for
whole-world idleness.

This project is an architectural replacement, not a prerequisite for current Splice, Icy
Impactors, Enceladus, or World Government Terraforming behavior. Philares is not a working
precedent: its required preparation-time delegation remains a known gap described in
[IDENTITY.md](IDENTITY.md).

## Domain requirements

The coarse phase vocabulary is:

```text
Setup -> Corporation -> [Prelude] -> Action -> Production -> Solar
      -> [Venus Solar] -> [Colonies Solar] -> [Turmoil Solar]
      -> Generation -> Research -> Action ...
Solar -> Final Greenery -> End
```

This is topology, not one unconditional line.

- Setup creates generation 1. The first generation has no Generation or Research phase.
- Prelude inserts its phase after Corporation.
- Applicable Solar subphases preserve Venus, Colonies, then Turmoil order.
- Merely naming a precedence constraint must not activate an absent expansion class.
- Expansion Modules own their insertions; base Kotlin must not enumerate expansion phases.
- An explicit terminal transition from Solar suppresses all later Solar subphases.

### Multiplayer ending

After Production, Solar checks temperature, oxygen, and oceans. Venus is not part of the multiplayer
end condition. If complete, go directly to Final Greenery and End; otherwise run selected Solar
subphases and continue.

Final greenery follows final-generation player order. Each player receives an optional conversion,
and each placement may enable another after all its bonuses and effects drain. The player explicitly
chooses `Ok` to finish before the next player begins.

### Solo ending

Every solo game runs its configured generation count even if its objective is reached early. At the
Solar phase after final production:

- if the configured objective is satisfied, run Final Greenery then End;
- otherwise abort without final greeneries or scoring and record a zero-score loss.

Ordinary solo checks its required parameters, Venus solo includes Venus, and TR 63 checks rating 63.
Final greeneries cannot rescue a failed objective.

### Other required precedence

- Later Generation phases pass the first-player marker immediately before Research.
- Existing energy converts to heat before new production.
- Solar checks end before World Government Terraforming, colony production, or Turmoil.
- Colonies fleet return and track advancement belong in its Solar subphase; the current earlier/later
  approximation remains a known gap.
- Scoring begins only after all final greeneries and their consequences finish.

## State and completion

A Phase component is legitimate Game World state. Readiness, pending work, and waiting belong to
Tasks or execution control; do not mirror them as marker components.

Native workflow needs **control-until-drain**, not instruction-side `BY` and not whole-world
idleness. Engine retains a continuation while one Player controls a turn; nested choices may be
assigned elsewhere; Engine resumes only after that control scope drains. The exact representation
may be a frame, continuation, or suspension relation. Current task queues do not provide it. See
[IDENTITY.md](IDENTITY.md).

## Minimal proposed model

A generic runtime needs only:

- one selected workflow entry step;
- durable workflow-step Classes;
- span-scoped forward precedence among active steps; and
- a transient completion signal after a step's control scope drains.

Compile topology from active Catalog data. Precedence endpoints are weak references: a constraint
participates only when its span and both endpoint Classes are independently active. It must never
activate an endpoint.

A span has one unique next active step. Expansion constraints insert intermediate steps between
core-owned endpoints. Dynamic branches remain ordinary effects; for example, Solar completion may
transmute directly to Final Greenery, overriding normal span advancement.

Do not add a persistent “ready” component, a second next-phase field, a separate workflow expression
language, or a Kotlin registry that repeats domain topology.

### Proposed Pets vocabulary

**Aspirational syntax; none of these Classes or runner semantics is implemented.**

```pets
"Selects the first step of a native workflow"
ABSTRACT CLASS Workflow<Class<WorkflowStep>> : System

"One durable state in a native workflow"
ABSTRACT CLASS WorkflowStep : System

"Signal emitted after one workflow step's control scope drains"
CLASS StepComplete<WorkflowStep> : Signal, System

"One contiguous part of a workflow, including its endpoints"
ABSTRACT CLASS WorkflowSpan<Class<WorkflowStep>, Class<WorkflowStep>> : System

"A forward ordering constraint inside one span"
ABSTRACT CLASS WorkflowPrecedence<
    Class<WorkflowSpan>, Class<WorkflowStep>, Class<WorkflowStep>> : System
```

Terraforming Mars would select `SetupPhase` as its entry point and describe core spans separately:

```pets
CLASS TerraformingMars : Module {
  HAS =1 Class<TerraformingMarsWorkflow>
}

CLASS TerraformingMarsWorkflow : Workflow<Class<SetupPhase>>

CLASS AfterCorporation :
    WorkflowSpan<Class<CorporationPhase>, Class<ActionPhase>>
```

Prelude would insert its phase without replacing the core span:

```pets
CLASS CorporationBeforePrelude : WorkflowPrecedence<
    Class<AfterCorporation>, Class<CorporationPhase>, Class<PreludePhase>>

CLASS PreludeBeforeAction : WorkflowPrecedence<
    Class<AfterCorporation>, Class<PreludePhase>, Class<ActionPhase>>
```

These references are intentionally weak. The precedence declarations participate only if the span
and both endpoint Classes were activated independently. The runner compiles them from active
Catalog data; it does not create precedence components in the World.

After a step and its delegated control scope drain, the runner emits
`StepComplete<current-step>`. An ordinary Effect may consume that signal to make a dynamic branch.
Otherwise the runner follows the unique successor in the compiled span, transmutates the old Phase
to the next, and waits for the new step. No successor means termination; multiple immediate
successors are invalid.

The transient signal is intended to avoid a second persistent readiness state while preserving the
exactly-one-Phase invariant. Unordered component fanout is the separate [`EACH`](EACHPLAYER.md)
proposal. Player-controlled work still requires the delegation model in
[IDENTITY.md](IDENTITY.md); fanout alone does not transfer control.

## Implementation gates

1. Prove a generic runner with a synthetic linear span, one inactive/active insertion, one
   requirement-selected branch, and termination.
2. Extract only lifecycle, checkpoint, cancellation, and wakeup mechanics from `TfmWorkflow.Auto`.
3. Move coarse Terraforming Mars topology and expansion insertions into Catalog/Pets data.
4. Implement Player control-until-drain with parent/child assignment tests.
5. Express Corporation, Prelude, Action, and Final Greenery turns using that control mechanism.
6. Migrate callers, then delete both Kotlin Terraforming Mars workflow variants.

Preserve current integration tests throughout. A native workflow is not successful merely because
its happy-path phase list is correct.
