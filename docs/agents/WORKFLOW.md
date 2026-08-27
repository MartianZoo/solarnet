# Native Pets workflow

> **Read when:** changing phase topology/end conditions, moving workflow into Pets, introducing a
> generic workflow runner, or deciding whether whole-world idleness is the right completion rule.
>
> **Skip when:** changing a card effect or existing workflow-created choice without
> changing phase ownership.
>
> **Status:** domain requirements are settled. Native vocabulary and runner are proposed. Committed
> `TfmWorkflow.Auto` sequences phases in Kotlin and waits for whole-world idleness.

## Source map

- [`TfmWorkflow.kt`](../../tfm-engine/src/commonMain/kotlin/dev/martianzoo/tfm/engine/TfmWorkflow.kt)
  — search for `public object TfmWorkflow` and the named phase methods for current behavior.
- [Terraforming Mars `classes.pets`](../../tfm-canon/src/commonMain/resources/canon/bundles/TerraformingMars/classes.pets)
  — search for `ABSTRACT CLASS Phase`, `CLASS Generation`, and `CLASS EndPhase` for current domain
  vocabulary.
- [`TfmWorkflowTest.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/rules/TfmWorkflowTest.kt)
  and [`EndgameRulesTest.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/rules/EndgameRulesTest.kt)
  — select only scenarios matching the changed phase/end transition.

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

After Production, the workflow checks whether any `GameEndBarrier` remains. Multiplayer begins with
one barrier for each of temperature, oxygen, and oceans; `MandatoryVenusVariant` adds
one for Venus. With no barrier, go directly to Final Greenery and End; otherwise run selected Solar
subphases and continue.

Final greenery follows final-generation player order. Each player receives an optional conversion,
and each placement may enable another after all its bonuses and effects drain. The player explicitly
chooses `Ok` to finish before the next player begins.

### Solo ending

Every solo game runs its configured generation count even if its objective is reached early. Each
`SoloGenerationsLeft` is a `GameEndBarrier`; creating a Generation removes one, so the last
generation is played with none. At the post-production check:

- if the configured objective is satisfied, run Final Greenery then End;
- otherwise abort without final greeneries or scoring and record a zero-score loss.

Standard solo checks that no active global parameter remains incomplete, while TR 63 checks the
player's then-current rating. Final greeneries cannot rescue a failed objective.

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

Native workflow needs **control-until-drain**, which neither instruction-side `BY` nor
whole-world idleness provides. Engine retains a continuation while one Player controls a turn;
nested choices may be assigned elsewhere, and a suspended parent keeps the controller's queue from
becoming empty. Engine resumes only after that control scope drains. The exact representation may
be a frame, continuation, or suspension relation. Current task queues do not provide it. See
[IDENTITY.md](IDENTITY.md).

The agreed completion handshake is an Engine-created `Idle<Player>` Signal after one controlled
queue epoch drains. Idle listeners get the first opportunity to perform automatic settlement or
enqueue more work. Workflow proceeds only when dispatch leaves the control scope drained and no
unfinished temporary state remains. This replaces the current coroutine's direct whole-World idle
wakeup; it does not make raw queue emptiness sufficient for phase advancement.

## Minimal proposed model

A generic runtime needs only:

- one selected workflow entry step;
- durable workflow-step Classes;
- span-scoped forward precedence among active steps; and
- the shared `Idle<Player>` completion handshake after a controlled queue drains.

Compile topology from active Catalog data. Precedence endpoints are weak references: a constraint
participates only when its span and both endpoint Classes are independently active. It must never
activate an endpoint.

A span has one unique next active step. Expansion constraints insert intermediate steps between
core-owned endpoints. Dynamic branches remain normal effects; for example, Solar completion may
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

"Engine-owned signal emitted after one Player's controlled queue drains"
CLASS Idle : Owned<Player>, Signal, System

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

After a controlled queue drains, Engine emits `Idle<Player>`. Automatic listeners settle transient
state and queued listeners may reopen work. Once idle dispatch reaches quiescence, the runner follows
the unique successor in the compiled span, transmutates the old Phase to the next, and waits for the
new step. No successor means termination; multiple immediate successors are invalid. Dynamic
branches may consume the idle boundary before the default successor is chosen.

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
5. Emit `Idle<Player>` after a controlled queue epoch and prove that listeners may reopen work
   before workflow advances.
6. Express Corporation, Prelude, Action, and Final Greenery turns using that control mechanism.
7. Migrate callers, then delete both Kotlin Terraforming Mars workflow variants.

Preserve current integration tests throughout. A native workflow is not successful merely because
its happy-path phase list is correct.
