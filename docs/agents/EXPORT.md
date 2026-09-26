# Game exports and imports

> **NOTE:** This document is an agent-maintained record of the export design discussed with the
> project owner. Source and tests are stronger evidence for implemented behavior.

> **Read when:** designing a game file, recording decisions, importing a played game, comparing an
> engine replay with passive playback, or changing round-trip tests.

> **Status:** The passive event recording is implemented. The plain-text decision experiment and
> its round-trip probe are preserved in stash `3f5131bccad4709103f6dac4ce40d55f3ed068c2`,
> outside the current `work1` tree. A combined export is a design direction, not an implemented
> format. No implementation-version compatibility promise has been selected.

## Goal and the three views of one game

The goal is a **pure file** containing the necessary and sufficient information to reconstruct a
played game, given the same game declarations and implementation. It must not depend on the
exporter's live World or a replay script that supplies omitted choices. The compact decision form
must also avoid source task ids; exact event recordings do carry ids to apply their task events.
Distinguish three useful representations:

| File | Import path | What it records | Primary use |
| --- | --- | --- | --- |
| Exact event recording | Passively apply recorded events; no engine | What happened, including concrete component changes and task lifecycle events | Viewer and faithful navigation |
| Decision recording | Recreate the premise and execute each Actor decision through the engine | What Actors chose or supplied from outside the rules | Small, explanatory game reconstruction |
| Combined recording | Either of the above, independently | Both streams with one premise recipe | Viewer playback plus engine reconstruction and comparison |

The two streams in a combined file must never both be applied to the same World: that would perform
consequences twice. They are two routes from the same game, with different dependencies and useful
checks against each other. The combined envelope and any correspondence between its streams have
not been specified yet.

## Exact events: the implemented viewer export

[`GameRecordingJson.kt`](../../src/common/dev/martianzoo/state/GameRecordingJson.kt) writes a
premise recipe (Class selections and Player names), approved viewer positions, and exact events.
Events include resolved component gains, removals, and transmutations **and** task additions,
edits, and removals. The compatible Canon and Class Table are reconstructed externally; the file
does not embed game code or a second Catalog. [`GameRecording.kt`](../../src/common/dev/martianzoo/state/GameRecording.kt)
opens an independent Game World and navigates completed positions by applying or reversing those
events. Playback does not resolve instructions, fire effects, run autoexec, or need the engine.
[`GAMEWORLD.md`](GAMEWORLD.md#serialized-events-and-exported-recordings) owns the detailed current
model.

A component-change-only stream would reconstruct the component graph, but it would not recover
pending tasks or their selection and cause data. The existing viewer recording includes task events
for that reason. It is an exact account of results, not a decision file: it cannot by itself say
which choice led the engine to a result.

## Decisions: desired small text file

The tentative human-readable syntax is one line per Actor instruction or correction:

```text
Pink: CopyProductionBox<Mine>
Pink: Production<Class<Steel>>
Purple: Plant
Pink: CHOOSE 2 VenusStep
Pink EX MACHINA: 1 MC
Admin: MudSlides
```

These lines illustrate syntax, not one actual game's chronology. `EX MACHINA` identifies an
external correction or exceptional adjustment. Manual Admin choices must be represented when the
rules and policies cannot deduce them. Deterministic Admin work can be regenerated; the experiment
does not export every automatic Admin step. Player tasks carried out by autoexec must be recorded
just like tasks carried out on a client request. Whether an Agent has a live client connection must
not affect its registered autoexec notifications.

The named Actor is the Agent that supplies the task action. A selected task whose context owner is
Px must ultimately be done by Px's Agent. Another player's Agent may select it first, then Px's
autoexec may perform a concrete task; if Px disables autoexec, the operation can wait for Px. The
task's context owner comes from the Player owner of the component that housed the instruction,
otherwise the triggering component's Player owner, otherwise the triggering Player Actor, and
finally Admin. Keep controller, context owner, assignee, and change performer distinct as in
[`IDENTITY.md`](IDENTITY.md#six-identities).

An ordinary line is a self-sufficient command for **one task stage**. It may leave tasks produced by
that stage for subsequent lines. `CHOOSE` is explicit when a line only selects or narrows a pending
task and does not complete the named stage. In particular, a line such as `2 VenusStep` must never
silently mean “only choose this; execute VenusStep again later.”

Export component-change instructions after resolution, at the successful execution boundary.
For a custom translation, export its concrete input when that stage completes. Do not export a
pre-resolution `Per`, gated instruction, unresolved `.` or `?`, or an abstract template merely
because a command began with one. A `CHOOSE` line may name an unresolved pending task, but its
quantifier is matching context, not a command to execute an unresolved child. The file omits
mandatory `!`; the reader restores it directly, without consulting component defaults. The file
may omit an Actor's own `<Pink>` argument only when reparsing in Pink's context restores exactly
the same instruction. These are syntax reductions, not lost choices.

Card tracking was deliberately deferred in the first card-tracked-game experiment. Later, any card
identity or other external outcome that the unchanged engine cannot infer must be supplied by the
file. A compact decision stream still needs a premise recipe and a workflow-start boundary; the
stashed experiment's text stream relies on its caller to supply those. An unstarted World is not
equivalent to one whose workflow has launched.

### Custom instruction boundary

The selected direction is to stop when resolving a custom instruction replaces it with concrete
work. For example, performing `CopyProductionBox<Mine>` translates that task into
`Production<Class<Steel>>`; it must leave production pending. The owner's autoexec policy may then
execute production, or a later explicit line may do it. A policy of `NONE` must leave it waiting.
This is why the plain two-line example above is preferable to `CHOOSE CopyProductionBox<Mine>`:
the first line actually performs the custom translation, while the second performs its result.

The current engine does not yet honor that boundary: selecting or narrowing a task can immediately
execute its concrete replacement. A systemic repair should make selection and narrowing stop at
the produced task and make `doTask` complete no more than the stage it was asked to perform. Do not
special-case Robotic Workforce. Recording must use those semantic stage boundaries; inspecting
all events produced before an outer command returns is unreliable because autoexec may already
have performed a second stage by then.

### Importing without task ids in the file

The importer may speculatively select a pending task, attempt the line, and roll back a failed
attempt. It must consider **all** legal task matchups and interpretations across the full file,
not commit the first locally successful match. In a real replay, two `-ProjectCard` tasks accepted
the same line; choosing the wrong one left a later `3 ProjectCard` line impossible. Backtracking
across earlier lines is therefore required. Workflow phase transitions commit timeline positions,
so rebuilding from the premise and replaying a different candidate path is a safe initial search
strategy; caching can wait for measured need.

Do not add task-origin fields merely because two pending tasks have different ids or causes.
[`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt) already treats tasks
that differ only by id and cause as interchangeable in ordinary matching. `::` trigger matching
uses the concrete change, its Actor, and the live World; it does not inspect hidden task-cause
metadata to decide which effects fire. Tasks with different controllers or `THEN` continuations
are different work, and later lines may distinguish those matchups. No example has yet shown two
distinct successful complete imports of the same file. Demonstrate one before adding a verbose
disambiguator.

## Round-trip evidence and remaining boundaries

The stashed opt-in probe starts capturing immediately after World creation, encodes and reparses
the decision text, creates a second World from the same premise, replays the lines, and compares
the complete component graph plus the multiset of pending tasks, including each task's cause
ancestry. A changed pending-task list must fail the round trip. One focused fixture also compares
Actor-attributed changes. Exact event order is not currently part of that decision test.

At the last measured point, the probe passed 16 of 29 actual replay games; ten additional support
tests passed without exercising the probe. All 29 games encoded, but 13 of the 39 package tests
failed the opt-in import or comparison. This is evidence for the stashed experiment, not for the
current clean branch. Important remaining causes were:

- Custom translation and immediate execution could erase the `CopyProductionBox` card choice and
  leave only the derived `Production` line. Retaining both under current execution semantics would
  execute production twice.
- Greedy task matching could accept an early line against the wrong pending task and fail later.
- Autoexec could select an abstract Admin Global Event before the imported Player decisions,
  despite a different source ordering. One source test had never launched its workflow, while the
  probe always launched one.
- Several remaining games failed on scoring, `Ok`, payment, or tile tasks and need their first
  divergence investigated individually. Resumable End scoring was one proven fix when Player
  autoexec was disabled.

For the next experiment, temporarily report the unlaunched-game and Admin-scheduling cases as
explicit exclusions while repairing task stages and backtracking. Do not count exclusions as
successful imports. A completed-game-only importer may require every queue to be empty, but tests
of a partial game must compare full pending tasks rather than accept matching components alone.
Direct `sneak` and task injection still need an explicit boundary if a source test uses them.

The combined format should be tested by opening its exact events without an engine and,
independently, rebuilding the game from its decisions. Compare the two results at defined coherent
positions, including pending work when a position is not terminal. This comparison should expose
missing decisions rather than let the event stream repair a failed decision import.
