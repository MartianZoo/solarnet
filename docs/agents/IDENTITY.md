# Identity, Task Assignment, and Control Scopes

> **Agent record:** This is not user documentation, just an agent record written neither by humans nor for humans.

## Purpose

Record the identity distinctions the engine implements today, the cross-owner behaviors already
protected by tests, and the narrower control-scope problem that remains for native workflow. The
working cards described here are regression constraints, not evidence that task delegation must be
redesigned.

## Current identity model

- **Actor** is who performs an instruction and is recorded on resulting change events.
- **Owner** says whose components they are. Changing another Owner's components does not make that
  Owner the Actor.
- **Player** is both an Actor and an Owner. **Engine** is an Actor but not an Owner.
  **SoloOpponent** is an Owner but not an Actor.
- **Assignee** says whose task view contains a task and whose scoped gameplay may revise, prepare,
  and execute it. Assignment and performance are intentionally separate.
- Trigger-side `BY` filters the Actor recorded on the triggering change.
- Instruction-side `BY` is a **performer override**. `A BY Player2` performs `A` as Player2 without
  changing the enclosing Task's Assignee or transferring its choices.
- Resolving an owned component effect binds contextual `Owner` from the component carrying the
  effect. A later performer override does not rebind already-specialized owned types.

Mons Insurance, Crash Site Cleanup, Protected Habitats, and Helion all depend on the difference
between the Actor causing a change and the Owner affected by it. The current model preserves that
difference in event attribution and trigger matching.

## Current triggered-task assignment

`Effector` currently chooses the assignee for nonautomatic triggered work in this order:

1. the Player owning the component whose effect fired;
2. otherwise the Player owning the changed component;
3. otherwise the Actor of the triggering change.

Contextual `Owner` is calculated separately, although it currently uses the same first two sources
before falling back to a triggering Player. Keeping these calculations separate is important:
authored trigger-side `BY`, instruction-side performer overrides, contextual ownership, and task
assignment are different facts.

This assignment rule is deliberately labeled a compatibility policy in `Effector`. It is broad,
but it is also established behavior on which current rules and tests rely. Do not replace it merely
because a more explicit model seems attractive. First identify a rule whose required behavior it
cannot express, protect that behavior, and preserve the cases below.

Automatic effects are executed inline by the Instructor handling the triggering change rather than
being exposed as selectable tasks. Whole-game autoexec has another current caveat: the Actor of
the gameplay context invoking autoexec performs a selected task even when that task has a different
assignee.

## Proven cross-owner behavior

These behaviors work today and have focused integration coverage.

### Philares

When Player1 creates the qualifying adjacency beside Player2's Philares tile, the effect-bearing
component is owned by Player2. The compatibility assignment policy puts the abstract standard-
resource task in Player2's queue. Player2 chooses and performs that gain, while the initiating tile
placement remains attributed to Player1. Tests also cover same-owner adjacency and nonqualifying
adjacency.

Philares does not currently use instruction-side `BY` and does not require a queue-suspension
mechanism to produce the tested result. `PhilaresTest` is the primary regression suite.

### Splice

Splice is modeled with two cooperating effects. The corporation's ordinary effect pays its Owner,
while one generated `CardXC03FWatcher<Player>` component per Player gives the owner of each gained Microbe
tag the choice between money and a Microbe on the exact tagged card. Tests cover both recipients,
multiple tags, both choices, correct card binding, and rejection of a different card.

Linked trigger specialization works in this representation. A more direct single-effect spelling
could be a worthwhile data simplification someday, but it would also need an explicit way for the
trigger-bound Player to become the task assignee. Instruction-side `BY` cannot do that: it changes
only the performer. The generated watcher is therefore a working representation, not a known
behavioral defect. `CardXC3Test` covers the representation through the card-facing API.

### World Government Terraforming and Icy Impactors

These rules demonstrate the existing separation between choice and performance. The StartToken
Owner receives WGT's `GlobalParameter! BY Engine` task and chooses the parameter, but Engine is the
performer. Icy Impactors similarly lets the StartToken Owner choose an ocean performed by the card
owner. Tests protect assignee, performer attribution, Actor-sensitive effects, completed-parameter
rejection, option handling, and the prepared-task lock during the cross-player choice.
`WorldGovernmentTerraformingTest` and `NewPromoCardsTest` contain the focused scenarios.

### Enceladus

Colony-tile effects, colony ownership, and the current assignment policy already distribute the
placement and trade card-resource choices to the correct colony owners. The integration test has
different Players choose Microbe cards for their respective Enceladus benefits and verifies the
resulting counts. This scenario lives in `ColoniesBasicRulesTest`.

## What is not implemented

There is no general queue-suspension or parent/child control-scope model. `TaskQueues` stores tasks
by assignee, and one globally prepared task prevents any competing task from being prepared. That
global lock is enough to protect isolated cross-player choices such as Icy Impactors, but it does
not represent why one Actor temporarily has control or when a larger delegated scope is complete.

The current Kotlin `TfmWorkflow.Auto` does not need such a model. It directly starts a Player's
turn operation and waits until the whole Game World is idle. A future native Pets Workflow cannot leave
an Engine continuation as an ordinary pending task and then wait for global idleness: that
continuation itself would keep the Game World non-idle. Native Workflow therefore needs a way to say:

1. Engine retains a continuation that is temporarily not actionable;
2. a Player controls all work produced within one turn scope;
3. nested cross-owner choices may temporarily block that Player without completing the turn; and
4. Engine resumes when that particular Player scope drains, not merely when an arbitrary queue or
   the whole Game World happens to be empty.

Call this **control-until-drain** for now. Queue suspension with explicit parent/child relationships
is one possible implementation, not a settled requirement. An execution-frame or continuation
model may express the same semantics more directly.

## Remaining work

1. Define native workflow's control-scope completion semantics with a small synthetic workflow
   example before choosing its Pets syntax or storage model.
2. Decide whether control scopes are represented by queue suspension, execution frames, explicit
   continuations, or another mechanism. Preserve rollback, nested choices, and prepared-task
   authority.
3. Add behavioral tests for the native Engine-to-Player handoff and nested return path. Keep all
   existing Philares, Splice, WGT, Icy Impactors, and Enceladus tests unchanged as regressions.
4. Separately decide how a trigger binds its Actor into an instruction when the rule needs both the
   attacker and victim, as Mons Insurance does. Trigger-side `BY !Owner` is only a predicate; it
   does not itself make the matching Actor available as an output binding.
5. Revisit the broad effect-assignment policy only if a concrete rule or native control-scope test
   demonstrates that effect ownership and choice authority must diverge. Rover Construction is a
   useful characterization candidate because its concrete payout needs no choice from the card
   owner.
6. Treat a direct single-effect Splice declaration as optional cleanup after assignment from a
   trigger binding is supported, not as a prerequisite for correct gameplay.
