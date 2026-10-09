# REPL color modes

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing REPL modes, arbitrary task initiation, manual turns, task dropping,
> corrections, or the transition between automatic gameplay and manual experimentation.
>
> **Status:** selected direction from the owner's discussion, with unresolved choices explicitly
> listed below. This document supersedes earlier color-mode guidance. The current REPL does not
> implement this contract throughout; its behavior is evidence of remaining work, not authority
> over the intended design.

## Integrity promised by each mode

| Mode | Integrity | Intended capability and obligation |
| --- | --- | --- |
| Purple | GAME | Play through the full workflow. Game rules grant turns and advance phases. |
| Blue | TURN | Grant yourself a turn, meaning an action slot or half-turn here. Its internal work follows ordinary gameplay sequencing; reject another turn while that work is unfinished. |
| Green | OPERATION | Initiate arbitrary instructions without a game-granted task. Handle the resulting work as an operation; reject another operation while that work is unfinished. |
| Yellow | Single-TASK | Execute a task with the intended all-or-nothing guarantee. Resulting tasks may be ignored or explicitly dropped. |
| Red | Direct correction | Make `exMachina` corrections regardless of pending gameplay. Corrections must not initiate or advance gameplay. |

Blue and green deliberately permit work that the game did not grant. They still need valid
internal sequencing, including payments and work delegated to another player. Completing that work
must not restart the game workflow, grant its next turn, or advance its phase.

Blue and green reject overlapping initiation; a warning is insufficient. Completion includes
relevant consequences assigned to other Actors, not just the initiating player's local queue.

For now, blue does not support `phase`. Green can initiate arbitrary instructions, but this does
not establish a replacement phase-control command or settle which explicit phase edits it permits.

Yellow is the mode for abandoning individual tasks. Dropping pending work is not an assertion that
the operation which produced it completed correctly, and does not promise to reverse effects
already applied. The exact extent of its all-or-nothing execution guarantee remains to be settled.

Red is intended to correspond to `exMachina`, not ordinary execution with fewer checks. Pending
gameplay should not prevent a correction. Existing structural constraints still matter; this is
not permission to construct arbitrary invalid state. The desired queue-neutral correction contract
is distinct from the current selected-task handling described below.

Entering red preserves existing pending tasks, including when descending from purple. Red does
not require the fresh-work isolation of blue or green: the correction leaves gameplay idle rather
than executing a new gameplay interaction alongside old work. Keeping those tasks does not settle
how their resolved forms should be refreshed or what later return to another mode permits.

## Leaving automatic gameplay

Dropping from purple into blue or green must isolate the manually initiated work from previously
pending game work. Only the new turn or operation and its consequences should participate in that
manual interaction. Existing work must be abandoned or suspended; the choice is unresolved.

Changing the command permissions alone is insufficient. Disabling future progression alone also
leaves already granted tasks present. Conversely, deleting tasks alone can empty the pool and make
continuation cleanup eligible. A transition must account for pending tasks, selected work,
continuation components, and partially completed state without accidentally advancing gameplay.

Suspension, if chosen, needs a rule for whether saved work remains meaningful after manual play or
corrections. Abandonment needs a rule for unfinished state left by already executed steps. Neither
option implies automatic rollback of earlier commands. Merely assigning old tasks a lower priority
would still be wrong if finishing the manual work releases them again.

No suspension mechanism, priority system, mode-transition algorithm, or route back into purple is
selected by this document. Compose existing semantics first; do not introduce a separate workflow
runner to implement the modes. [WORKFLOW.md](WORKFLOW.md) owns automatic phases and turns;
[SEQUENCING.md](SEQUENCING.md) owns the open internal completion and scheduling questions.

## Open decisions

1. **Pending work on descent:** abandon existing work or suspend it? What unfinished component
   state must be handled along with tasks? Is returning to purple supported initially?
2. **Entering yellow:** how should existing work and future workflow progression be treated?
   Permission to drop individual tasks does not itself specify a mode-transition policy.
3. **Queue-neutral corrections:** may the engine invalidate or refresh a selected task's resolved
   form after state changes, provided it does not execute, complete, or discard that task? Literal
   queue immutability and retaining a task resolved against outdated state need reconciliation.
4. **Yellow atomicity:** does “a whole instruction happens or it doesn't” mean a submitted execution,
   a queued task, or an instruction whose choices span several commands? Do not infer rollback of
   previous committed choices from the current transaction guarantee.
5. **Manual session setup:** without `phase`, how does a fresh blue/green session reach a useful
   phase? Is it entered from an existing game, or supplied an initial scenario? Which blue turns
   should be available outside the Action phase?

Resolve the relevant question when work reaches it rather than quietly choosing semantics in
REPL glue or committing to a larger scheduling design.

## Current implementation differences

The following describes this branch's current implementation, not the mode specification:

- [`ScriptSession.kt`](../../src/common/dev/martianzoo/tfm/script/ScriptSession.kt), search
  `installGame`: purple game creation launches `TfmWorkflow.Automatic`; other game creation calls
  `TfmWorkflow.Stepwise.setupPhase`. The initial REPL access mode is green.
- [`ModeCommand.kt`](../../src/common/dev/martianzoo/tfm/script/commands/ModeCommand.kt), search
  `repl.mode`: changing color only changes access permissions. It neither manages pending work
  nor starts or stops the workflow runner.
- [`Access.kt`](../../src/common/dev/martianzoo/tfm/script/Access.kt): blue through red expose
  explicit phase and turn commands. Green and yellow both use `beginOperation` for arbitrary
  instructions; yellow and red permit task dropping. Red currently calls `Agent.sneak`.
- [`AgentImpl.kt`](../../src/common/dev/martianzoo/agent/AgentImpl.kt), search `beginOperation` and
  `startTurn`: the former rejects a nonempty global task pool; the latter directly introduces
  `NewTurn`. Existing entry points are not proof of the proposed mode guarantees.
- [`exMachina.kt`](../../src/common/dev/martianzoo/agent/exMachina.kt): the correction wrapper
  restores a selected task's unresolved form, applies the correction, reselects the task, and runs
  autoexecution. This can advance pre-existing work. The underlying correction path suppresses
  queued effects and idle cleanup, but the wrapper is not queue-neutral.
- [`WorldTransaction.kt`](../../src/common/dev/martianzoo/engine/WorldTransaction.kt) and
  [`Engine.kt`](../../src/common/dev/martianzoo/engine/Engine.kt), search `removeTemporaryComponent`:
  gameplay settlement performs cleanup when the global pool empties and reports transaction
  completion to the workflow runner. Dropping tasks through Agent entry points therefore needs
  examination even when autoexecution is disabled.

[EX_MACHINA.md](EX_MACHINA.md) owns current correction mechanics and structural restrictions.
The red-mode target here must not be mistaken for an already implemented correction guarantee.

## Test migration and acceptance

Ordinary card and game-rule scenarios should use the full automatic phase and turn workflow.
Manual progression in their existing fixtures is migration work, not justification for retaining
another Kotlin phase sequence. Dedicated mode tests should exercise deliberate manual behavior;
bootstrap and engine tests may still test their own lower-level contracts.

Migration of manual gameplay tests remains outstanding. Stop and discuss a
scenario that cannot migrate cleanly rather than adding helpers that recreate phase or turn rules.

Acceptance checks for future implementation:

- Purple still carries normal games through phases and turns using Pets rules.
- Blue grants the requested action slot without granting a subsequent workflow turn.
- Green handles the initiated operation's consequences without reviving previous game work.
- Blue rejects a new turn and green rejects a new operation while the current interaction is
  unfinished; neither merely warns and proceeds.
- Relevant delegated work participates in completion; an empty local queue is insufficient.
- Yellow can abandon tasks without falsely promising operation completion or retroactive rollback.
- Entering red preserves pending tasks. Corrections do not advance gameplay, and selected-task
  handling follows the eventual explicit queue contract.
- Mode transitions satisfy the decisions above; no pending-work policy is hidden in autoexecution.
- Ordinary gameplay tests stop relying on manually injected phases and turns, while dedicated
  mode and engine coverage remains meaningful.
