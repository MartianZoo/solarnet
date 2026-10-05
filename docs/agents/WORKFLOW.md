# Phase progression and completion

> **Read when:** changing phase progression, expansion phase order, turn control, or cleanup that
> permits the next phase to begin.
>
> **Status:** current implementation and open design questions. Pets-owned progression remains the
> goal; a particular scope hierarchy, priority representation, or topology compiler is not an
> implementation plan.

## Purpose

[COLOR_MODES.md](COLOR_MODES.md) owns the intended REPL mode contracts and supersedes earlier
color-mode guidance. The current runner described here does not yet implement those contracts.

After an explicit start, ordinary game rules should determine phase progression. The current Kotlin
runner supplies phase order, player rotation, extra action offers, and completion decisions. Moving
those responsibilities into Pets requires an account of when the relevant work has finished, not
just a different place to store the next phase.

[SEQUENCING.md](SEQUENCING.md#delegated-operations-and-scheduling-options) owns the shared scheduling
options. Phase progression must compose with delegated choices and payments: P1 may remain on turn
while P2 completes an operation, followed by P1's remaining work, event cleanup, and eventual Admin
activity. A single current-player field cannot represent those different roles.

## Current foundation

- [`Engine.newGame`](../../src/common/dev/martianzoo/engine/Engine.kt) completes bootstrap before
  returning. The game starts with `BootstrapPhase`; creating the World does not launch the workflow.
- [`game.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/game.pets), at `CLASS Phase`,
  requires a unique current Phase, including the terminal `End`.
- [`TfmWorkflow.Automatic`](../../src/common/dev/martianzoo/tfm/engine/TfmWorkflow.kt), at `runGame`,
  chooses phase order in a coroutine. `grantFirstActionTo` and `grantSecondActionTo` issue the
  player-owned action requests. The runner resumes after `game.isIdle()`.
- `GenerationScope` is a live lifetime anchor. Generation-local components depend on it, and
  replacing it removes those dependents through ordinary component dependency semantics.
- [`SystemDeclarations.kt`](../../src/common/dev/martianzoo/pets/SystemDeclarations.kt), at
  `TemporaryScope`, defines a child Scope with both `Temporary` removal policy and `MustCleanUp`
  status. This does not provide completion local to that scope.
- [`Engine.kt`](../../src/common/dev/martianzoo/engine/Engine.kt), at
  `removeTemporaryComponent`, removes eligible Temporary Types only while the entire task pool is
  empty. It waits for direct or indirect dependent Temporary or MustCleanUp components. Settlement
  after each removal can create work that stops further cleanup.

Setup and Research expose simultaneous Player work. Corporation and Prelude play use ordered
Player turns. Action rotates players, offering a second action where the current runner permits it;
Final Greenery also visits players in order. Having tasks therefore does not itself identify who is
on turn, and several phases cannot be modeled as an exclusive Player turn.

Normal-corporation play explicitly excludes `BeginnerCorporation` in addition to standard-back
typing. That is a deliberate statement of the normal path, not redundant routing machinery to
remove during workflow work.

## The completion problem

Global idleness currently combines several events that need not be equivalent: an operation has no
more work, an event card can leave play, a turn is over, and Admin may advance the phase. Event cards
are Temporary, so unrelated pending work can keep them face up. Conversely, waiting for a particular
Player's queue to drain can miss work delegated to another Player.

Simply adding delayed cleanup or Admin tasks changes the premise of current cleanup: those tasks
keep the global pool nonempty. A priority design must explain how cleanup becomes eligible; keeping
the existing empty-pool condition would stall it.

Nested priority groups are a promising shared option, not a settled representation. The innermost
group could let its tasks remain independently selectable while suspending its caller. Cleanup
included in that group must finish before the caller resumes. Assigning both inner cleanup and
suspended caller work the same numeric rank would release them together. Likewise, event cleanup and phase
advancement cannot merely share a deferred rank if their order matters.

An exclusive operation scope is another option; causal ancestry could supply membership, but alone
supplies neither control nor scheduling. The scope must include relevant work across Actors and
distinguish its cleanup from the continuation that follows it. Component dependencies already express lifetime;
they do not automatically establish that the tasks associated with a component have finished.

## Phase topology

The current runner follows this coarse shape:

```text
Bootstrap -> Setup -> Corporation -> [Prelude] -> Action
Action -> Production -> Solar -> [Solar expansions] -> Research -> Action ...
Production -> final path -> Final Greenery -> End
```

The optional Solar order is Venus, Colonies, then Turmoil. Game-end checks, solo victory handling,
and final-greenery choices also affect the actual path; the sketch is not an executable substitute
for those rules.

Expansion-owned relative-order declarations could replace the runner's expansion list. Compiling
them into ordinary Pets successors is an option; the runtime should not also retain a parallel
Kotlin topology. Such declarations would need to distinguish absent optional expansions from
missing required phases and reject ambiguous or cyclic ordering. Dynamic game-state branches would
still belong to the owning game rules.

## Questions a replacement must answer

- What identifies an operation, turn, or phase occurrence, and which existing component already
  carries that fact? A complete Game/Generation/Phase/Turn/Action hierarchy is not assumed necessary.
- What makes a group complete when choices move between Players or temporarily visit Admin?
- How do deferred work and nested cleanup preserve the intended order without serializing
  independent choices?
- Which phases have an on-turn Player, and how does that fact differ from the controller of a
  delegated operation?
- How does bootstrap remain quiescent until explicitly started, and how does `End` remain terminal?
- Which Kotlin decisions and existing cleanup rules disappear under the proposed model?

Extracting the coroutine into a generic runner would preserve the continuing orchestration that
this work seeks to remove. A replacement is valuable only when the resulting rules become smaller
and the game continues correctly through pending choices, cleanup, and rollback.
