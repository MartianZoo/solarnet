# Engine API Restructuring

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

**NOTE:** This records the current direction for preparatory engine simplification and the later
client API. It is not a requirements document or a commitment to particular type names.

The current engine API tries to express who is allowed to do what. `Gameplay` has nested power
layers and `godMode()` reveals more of them. Until recently, several live data structures also had
paired read-only and writable types. This did not produce a meaningful authority boundary: the
same implementation implements every gameplay layer, callers cast between them, and the REPL
obtains `godMode()` before hiding methods again according to its color mode.

The preparatory direction is to stop making the current engine responsible for authority. It should
be a trusted, low-level workhorse with a straightforward API that permits all supported engine
operations. The later client API is where we should design roles, permissions, capability objects,
and safe workflows deliberately.

## Decision

Separate two concerns that the current API mixes together:

1. **Engine integrity:** keep worlds structurally coherent, transactional, reversible, and
   correctly indexed even when a caller requests a rules-bypassing operation.
2. **Caller authority:** decide whether a particular caller should be offered that operation.

The current `engine` module remains responsible for the first concern and deliberately stops trying
to solve the second. For now, the `script` module owns its color-mode restrictions. A future API
layer will provide the principled authority boundary for normal clients.

This is not a claim that every internal collection should become directly mutable. A caller may be
allowed to request any *engine operation* without being allowed to corrupt the engine's bookkeeping.
For example, raw state editing may bypass Pets instruction semantics, but it must still update the
component graph and effect indexes together, log the change, and remain reversible.

## The Current Problem

### Gameplay layers do not enforce authority

`Gameplay` is currently an inheritance tower:

| Interface | Adds |
| --- | --- |
| `Gameplay` | queries, task actions, auto-exec controls, `godMode()` |
| `TurnLayer` | turns |
| `OperationLayer` | manual operations |
| `TaskLayer` | task insertion and removal |
| `GodMode` | raw state changes |

`ApiTranslation` implements the entire tower. Any holder of `Gameplay` can call `godMode()`, and the
returned object can be cast back to any layer. The layers therefore describe intended usage without
actually constraining it. They also make signatures, adapters, and Terraforming Mars helpers more
complicated.

### Read-only/writable pairs mixed two different ideas

`ComponentGraph`, `EventLog`, and `TaskQueue` formerly had separate internal writable counterparts.
They are now single concrete types whose mutation paths preserve the same invariants. The
paired hierarchy resembled the same ineffective authority model as the gameplay layers without
providing useful implementation encapsulation.

The important boundary is not whether a type's name advertises mutation. It is whether every
mutation passes through the one mechanism that preserves the object's invariants. A single concrete
service can expose only the queries required across modules while keeping other queries and
bookkeeping primitives internal, without requiring parallel read/write interfaces.

### Script color modes rebuild the layers with casts

`ScriptSession.access()` currently calls `gameplay.godMode()` for every color and `Access` casts the
result down to the desired gameplay layer. This is ceremony rather than protection. The script
session already knows its mode and is the natural temporary place to allow or reject commands.

## Target for the Existing Engine

### One flat gameplay workhorse

Replace the inheritance tower with one flat engine-facing API, retaining `Gameplay` as the
compatibility name unless a clearly better name emerges. It should expose the operations the engine
knows how to perform:

1. contextual parsing and queries;
2. task revision, preparation, execution, insertion, and removal;
3. manual operations and turns;
4. auto-execution controls;
5. raw, rules-bypassing state changes.

`godMode()` disappears because there is no hidden layer to reveal. Method names and documentation
should still distinguish ordinary ruleful operations from dangerous workhorse operations. `sneak`
and `dropTask`, for example, should be candid that they bypass normal game semantics.

Actor scoping remains meaningful even without authority layers. A gameplay object still supplies
the Actor used for contextual defaults, task assignment, execution, and event attribution.

### One integrity-preserving mutation path per structure

Remove paired read-only/writable interfaces where they do not buy implementation safety. Prefer a
single implementation type with:

1. observation methods, public only when another module needs them;
2. high-level workhorse operations, public only when direct engine clients need them; and
3. internal low-level mutation methods used to keep related state synchronized.

In particular:

1. Component changes must update the multiset and live-effect indexes together.
2. Task changes must preserve normalization, assignee validation, prepared-task rules, and event
   logging.
3. Event history must remain append-only except through rollback machinery coordinated with the
   state it describes.

The goal is to remove the writable/not-writable *API taxonomy*, not to publish mutable collections
or independent event-log append methods.

### `World` as the trusted aggregate

`World` can remain the live aggregate for components, events, tasks, timeline control, readers, and
Actor-scoped gameplay. Direct engine clients are trusted and may reach all of these facilities.
There is no need to add current-engine accessors such as `playerActions`, `rawStateEditor`, or
`timelineControl` merely to ration authority.

This also means we should not introduce dependency-injected capability interfaces into the current
engine as an intermediate architecture. DI may still assemble engine internals, but it should not
simulate a client permission system that the workhorse explicitly does not promise.

## What Must Remain Protected

Flattening authority layers must not weaken these engine properties:

1. Atomic commands roll back every component, task, index, and event change on failure.
2. Every component mutation keeps dependent indexes and live effects synchronized.
3. Every task mutation is logged and reversible and preserves queue normalization.
4. The global prepared-task lock remains coherent across Actor-scoped queue views.
5. Components and parsed types belong to the world's own class table.
6. Actor context continues to control defaults, `Owner` substitution where applicable, task scope,
   and event attribution.
7. Initialization and committed workflow boundaries cannot be accidentally undone through ordinary
   rollback.

These are correctness boundaries, not caller-permission boundaries. Keeping them inside the
workhorse is compatible with letting trusted callers perform arbitrary ruleful or rules-bypassing
operations.

## Script Color Modes

Until the new API exists, the `script` module should enforce its color modes locally. This is an
intentional transitional policy shim, not a security boundary and not a reusable authorization
framework.

`Access` can hold the flat `Gameplay` object and decide which operation to invoke or reject for the
current mode. Script commands that currently bypass `Access` should either be routed through it or
perform an explicit mode check in one obvious place. No command should rely on a cast failing to
enforce its mode.

Visible behavior should remain unchanged:

| Mode | Script policy |
| --- | --- |
| Purple | workflow stays engine-controlled; the user may resolve offered tasks |
| Blue | user actions must enter through turn operations |
| Green | arbitrary complete operations are allowed |
| Yellow | task insertion/removal powers are additionally allowed |
| Red | raw state changes are allowed without normal triggered consequences |

The mode checks may be somewhat custom because the modes themselves are a REPL feature. They should
nevertheless be centralized and directly tested so a new command cannot accidentally ignore them.

## Command Pipeline

The proposed command runner remains useful, but as an integrity and lifecycle mechanism rather than
an authority object. Public command-style operations should eventually share one pipeline:

```text
checkpoint
run explicit operation
run configured auto-exec behavior
collect TaskResult from all activity since checkpoint
rollback on failure
publish outermost after-command notification
return TaskResult
```

This pipeline should make current behavioral differences explicit before normalizing them.
`reviseTask`, `prepareTask`, `addTasks`, `dropTask`, and `sneak` currently do not all have the same
transaction, auto-exec, result, or notification semantics. Flat access does not imply that every
method must trigger auto-exec, but each method's command boundary must be intentional.

The first extraction can preserve the current whole-game auto-exec behavior. Actor-local auto-exec
and workflow handoff are separate semantic changes and should not be smuggled into an API cleanup.

## Implementation Responsibility Boundaries

The flat `Gameplay` API is one caller-facing workhorse, not a requirement that one implementation
class contain every policy. `ApiTranslation` should become only the string/value adapter for that
surface. The command pipeline owns atomicity, auto-exec coordination, result collection, and
completion notification. The current generically named `Implementations` class should disappear or
become a plainly named facade over focused task lifecycle, task-selection/auto-exec, operation
completion, and raw-change collaborators; Terraforming Mars turn signaling does not belong in any
of those generic services.

`ScriptSession` should retain interactive session state and command dispatch. Terraforming Mars
game construction, command catalog contributions, and vocabulary belong in an injected application
profile, while task-list and result rendering remain presentation collaborators rather than
additional session responsibilities.

As described below, `TfmGameplay` is transitional. Its player actions, workflow/phase controls,
test-fixture conveniences, and read-model calculations should become purpose-specific clients of
the flat engine instead of surviving as one wrapper.

## The Later Client API

The future API should wrap the workhorse instead of forcing the workhorse itself to impersonate a
safe client API. That later layer can provide small role-appropriate objects such as:

1. player queries and visible task choices;
2. player actions and turn actions;
3. workflow monitoring and administrative operations;
4. test-fixture and diagnostic powers;
5. timeline or composition control for privileged tools; and
6. Terraforming Mars-specific player actions and read models.

At that point, capability objects, dependency injection, unforgeable tokens, or another authority
model can be evaluated against real client needs. The new API may expose only a small subset of the
workhorse and can translate higher-level requests into several engine operations.

The important architectural dependency points one way:

```text
normal clients -> future policy/client API -> engine workhorse
scripts (temporarily) ---------------------> engine workhorse
tests and diagnostics ---------------------> engine workhorse
```

Terraforming Mars conveniences should likewise be separated by client purpose in the later API:
player actions, game flow, fixtures, and read models should not all be one wrapper. This separation
does not need to block simplification of today's `TfmGameplay`; it can temporarily depend on the
flat workhorse.

## Refactoring Sequence

### 1. Characterize the boundaries we are about to simplify

Before changing interfaces, cover behavior that the type hierarchy currently obscures:

1. which script commands each color mode accepts and rejects;
2. rollback on command failure and `AbortOperationException`;
3. returned `TaskResult` contents;
4. inclusion of auto-executed follow-up work;
5. single outermost `onAtomicComplete` notification; and
6. raw changes and task edits remaining reversible.

These should be behavior tests spanning the actual engine and script pieces, not tests that merely
repeat interface declarations.

### 2. Flatten `Gameplay`

1. Move turn, operation, task-editing, and raw-change methods onto `Gameplay`.
2. Remove `TurnLayer`, `OperationLayer`, `TaskLayer`, and `GodMode`.
3. Remove `godMode()`.
4. Update `ApiTranslation`, operation bodies, `TfmGameplay`, workflows, and tests to use the flat
   type without casts.
5. Preserve existing behavior while changing the surface.

This is the first implementation step because it directly deletes a hierarchy that provides no
real protection.

### 3. Put script policy in `script`

1. Give each `Access` mode the flat gameplay object.
2. Replace layer casts with explicit allow/reject behavior.
3. Route mode-sensitive commands through the centralized policy.
4. Add an explicit test whenever a command receives a new mode-sensitive power.

Keep this small and local. Do not build the future authorization framework in `script`.

### 4. Collapse read/write API pairs selectively

For components, events, and tasks, remove paired interfaces where one service with internal
mutation methods is clearer. Do one structure at a time and preserve its mutation/event/rollback
tests. The likely order is:

1. event log, whose append operations are already internal service roles;
2. component graph, keeping its update primitive internal;
3. task queues, where scoped views and normalization make the change most delicate.

Do not expose backing mutable collections. If merging a pair would force unrelated code to see a
dangerous primitive, retain implementation encapsulation without treating it as caller authority.

### 5. Name the command pipeline

Extract `CommandRunner` (name tentative) from `ApiTranslation.atomic` after the flat surface has
made command boundaries easier to compare. Initially preserve current auto-exec and notification
semantics. Then decide method by method which operations use the full pipeline.

### 6. Simplify wiring and documentation

Remove DI bindings, casts, compatibility accessors, and documentation that exist only for the old
layers or paired writable types. Keep stable engine documentation honest: direct engine callers are
trusted, while structural invariants remain enforced.

### 7. Stop before designing the new API accidentally

Once the current engine is a coherent workhorse, use actual upcoming clients and workflow needs to
design the restrictive API separately. Do not preserve speculative capability interfaces in the
engine just because they might resemble that future design.

## Risks and Mitigations

1. **A flat API is easy to misuse.** This is deliberate for direct engine clients. Use conspicuous
   names and KDoc for rules-bypassing methods, and direct normal applications to the future API.
2. **Script checks can drift.** Centralize them and test the command/mode matrix.
3. **Removing writable types could expose corrupting mutation.** Collapse type pairs only while
   keeping structural mutation primitives internal and synchronized.
4. **Flattening can accidentally change transactions.** First change types mechanically; normalize
   command semantics only after characterization tests exist.
5. **The temporary script policy might become permanent.** Document that it is REPL-specific and do
   not let other clients depend on it as their authority model.

## Open Questions

1. Should the flat workhorse retain the name `Gameplay`, or would `EngineSession` or `ActorEngine`
   make its trusted, Actor-scoped nature clearer?
2. Which flat methods are complete commands and which are composable primitives inside another
   command?
3. Which child-object mutations, if any, should eventually become direct public engine operations
   rather than remaining coordinated through gameplay?
4. How much of timeline control belongs on the workhorse root versus `World.timeline`?

None of these questions changes the main boundary decision.

## Bottom Line

This is a good preparatory simplification if we are precise about what “let everyone do whatever
they want” means. The existing engine should stop rationing legitimate operations through nominal
layers. It should not stop protecting its own data-structure and transaction invariants.

The near-term model is one flat, trusted engine workhorse; localized script color-mode checks; and
no speculative client-authority architecture inside the engine. The principled role- and
capability-aware API comes afterward as a separate layer around the simplified workhorse.
