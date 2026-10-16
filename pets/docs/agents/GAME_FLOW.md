# Canon turns and game flow

> **Status:** current authored flow and separately labeled intended turn semantics.
> [Solarnet sequencing](../../../docs/agents/SEQUENCING.md) owns task dispatch, execution,
> and completion checks.

## Turns and turn offers

**Selected terminology and intended semantics, not implemented.** Use **turn** for the complete
envelope of a player's accepted opportunity and its resolution. Consecutive turns may belong to
the same player. Do not call this an action scope, move, or turn segment. This deliberately differs
from the published game's terminology: its action-phase turn of up to two actions becomes up to
two consecutive turns here. Preserve when play advances to the next player; the terminology change
does not grant additional opportunities. Tests accept the difference: `TfmGameplay.turn { }`
groups a published turn of one or two actions, which can be two turns in this sense.

### Offering, taking, and finishing a turn

- In the action phase, first offer the player a turn. Declining that first offer is passing for
  the generation: the player never took a turn and receives no turn scope.
- Accepting the offer starts the turn. Finish its work and consequences before offering the second
  turn. Declining the second offer creates no turn and has no penalty or pass status; play simply
  advances. Keep this distinction even if both offers use shared machinery.
- A turn lasts through payment, effects, placement bonuses, other players' responses, and resulting
  work and cleanup. Completion of the initiating task alone does not finish the turn.
- This first/second offer rule describes ordinary action-phase rotation. It does not make
  corporation plays, Prelude plays, or Head Start's mandatory grants optional. Keep offering the
  last unpassed player a fresh first offer after each turn, without inventing additional turn
  groupings.

The intended units are:

| Activity | Turn scope |
| --- | --- |
| Initial corporation play and its resolution | A turn |
| Each Prelude played during the Prelude phase | A separate turn |
| Each accepted action-phase opportunity | A separate turn, including the optional second |
| Each final greenery placement and its resolution | A separate turn |
| Head Start under the variant below | Its Prelude turn, followed by two additional turns |

A card played or an ability invoked while resolving an existing turn does not itself start another
turn. For example, Merger's corporation play remains inside the turn that played Merger. An
explicit grant of a further turn is different from a consequence that requires another decision.

The proposed `OnTurn` fact, task routing, and decisions outside turns are described in
[Identity: who is on-turn, and who decides](../../../docs/agents/IDENTITY.md#who-is-on-turn-and-who-decides).

### Uses of the scope

Wild-tag allocations should belong to the accepted turn and expire when it finishes. A consecutive
turn gets fresh allocations, even for the same player; another player's response does not get a
new allocation scope. The current [wild-tag model](CARD_MODEL.md#wild-tags) implements assignments
and deferred cleanup; the accepted-turn scope described here remains a proposed redesign.

Suitable Infrastructure is a likely consumer of the same scope, but its exact eligibility and
reset rules remain to be confirmed. Existing `SuitableInfrastructureTest` scenarios show separate
rewards for the two action-phase actions and a single reward for a Prelude raising multiple
production tracks. `HeadStartTest` also expects separate rewards for Head Start's two granted
actions. These examples support the direction without establishing every interaction, especially
out-of-turn production changes.

### Head Start: acceptable variant to pursue

The owner is willing to change Head Start's timing to avoid requiring nested turns:

1. Resolve its basic instructions, including steel, money, and their consequences, in its Prelude
   turn. Finish that turn with two additional turns owed to its owner.
2. Grant and completely resolve each owed turn sequentially. Each is mandatory and grants a single
   action-phase action, not the normal first/optional-second pair or an option to pass.
3. Resume the normal Prelude sequence from where it stopped.

Two tokens could record the owed turns. Their names and implementation are undecided; express the
entitlement and its consumption through ordinary rules, without teaching Kotlin about Head Start.
This is explicit permission to explore those entitlement components, not a general return to
components used only to schedule arbitrary continuations.

This is a deliberate variant, not a claim of exact published timing. Money and steel, and all
consequences of the basic instructions, must resolve before either extra turn. In particular, the
extra turns cannot sell cards before Head Start calculates its money from cards in hand. The
current fake card allows that ordering and also allows its two actions to interleave; see
[`HeadStartTest`](../../../test/common/dev/martianzoo/tfm/tests/cards/HeadStartTest.kt).

The working direction is sequential turns, with no suspended parent turn or allocations to
restore. Head Start should not justify nested scopes by itself. This does not settle which work
may interleave with another player's response or payment within a turn.

### Remaining design work

- Turn completion is authored now (`TurnOver`, under [Game flow](#game-flow)), but `NewTurn` is
  still a signal, not a lasting turn scope. Define acceptance and a turn scope through authored
  rules and settlement.
- Keep turn identity separate from generic ability invocation: `UseAction` currently represents
  both normal actions and reactive offers. It must not automatically create a turn.
- Reduce overloaded action names when changing the relevant APIs. `ActionSlot` / `Action1` /
  `Action2` select declared capabilities, not the first and second turns. `Activate`, `Activatable`,
  and `AbilitySlot` were candidate replacements, not selected names; no blanket rename is agreed.
- Verify wild-tag allocation timing, Suitable Infrastructure, extra-turn ordering, and completion
  through cross-player responses before treating the model as implemented. Do not add nesting or
  a second scope representation without a concrete case that requires it.

## Game flow

The game's own rules carry play from `SetupPhase` to `VictoryPhase`. A client only starts the game
(`TfmGameplay.startGame`, which performs `SetupPhase FROM BootstrapPhase`). Nothing turns the
flow on or off.

- **Phases without turns hand off when their work is done.** `SetupPhase`, `ProductionPhase`,
  `SolarPhase`, `ResearchPhase`, and `End` each dispatch `FINALLY { Next FROM This }` as they begin.
  Production hands off to `SolarPhase` while a `GameEndBarrier` exists, and otherwise signals
  `CheckGameEnd`.
- **Phases with turns hand off from their last turn.** A turn lasts through its `LATER` cleanup, so
  its end is `FINALLY` work, and all pending `FINALLY` work becomes ordinary together. A handoff
  dispatched as the Corporation, Prelude, Action, or final greenery phase began would therefore run
  after its first turn; these phases instead say what follows each turn.
- **Selection cleanup.** Each `NewTurn` schedules Admin's `LATER` removal of any card backs still in
  that Player's `Selecting`. It runs after the turn's choices, including purchase payment and
  transfer, and before `TurnOver`. ResearchPhase schedules analogous
  project-card cleanup for all Players before its `FINALLY` handoff.
- **Turn order.** `TakeTurn<P>` only invites `P` to take a turn; whatever governs that player's
  turns answers it. The corporation, Prelude, and final greenery phases give `NewTurn<P>` and
  dispatch `FINALLY { TurnOver<P> }`, and each says what follows `TurnOver`. In the action phase
  the player's `RoundStatus` answers instead. `AdvancePlayer<P>` passes play to the next seat through
  `NextPlayer`. Outside the action phase, reaching the start player again is `RoundOver`; with one
  player, `AdvancePlayer` is always `RoundOver`. These signals take a Player argument rather than an
  owner, so their work stays with Admin, as it must for Players who do not autoexecute.
- **Round status drives the action phase.** Each player has at most one `RoundStatus`: `InRound`
  while they may still act in the action phase, or `Pass` once they have passed. The action phase
  wakes every player who has not passed. `InRound` gives its owner each turn and supplies the turn's
  choices; when the turn ends it offers a second action and then `AdvancePlayer` while another player
  is awake, and otherwise another turn. `Pass` replaces its owner's `InRound` and passes play on,
  both when the passing turn ends and whenever that player's turn comes round again. Production
  begins at the first `TurnOver` with nobody awake. Play goes around the table without `RoundOver`.
- **Modules own phase order.** The corporation phase ends in `RoundOver`. `NoPreludes`, selected in
  every game without Prelude, then begins the first `ActionPhase`; `PreludeExpansion` begins
  `PreludePhase` instead, whose own `RoundOver` begins the action phase. Expansions add their solar
  work as triggers on the one `SolarPhase`, and Turmoil's waits under `LATER` for the rest.
- **A turn given outside turn order has no continuation.** `NewTurn` alone is just a turn; only
  `TakeTurn` attaches what follows. Sandbox scenarios and the REPL's manual modes take turns this
  way. They leave the flow by dropping its pending tasks once, and then place phases ex machina
  (`TfmGameplay.phase`). Placing a phase runs its automatic effects but not its queued rules, so a
  placed action phase wakes every player without giving anyone a turn.
- **Declining another final greenery is `Done<Phase>`.** A signal would vanish at once, before
  `TurnOver` could read it, so `Done` instead lasts until its phase ends.


## Cleanup

| Class | Current meaning |
| --- | --- |
| `MustCleanUp` | State forbidden by operations that declare themselves complete. |
| `Barrier` | `MustCleanUp` state removed by its owning game rule. |
| `Signal` | An unscoped point event that removes itself automatically. |

Nothing is removed merely because the World is idle. A component whose end is future work
schedules that work as a deferred task: an event card's move to `PlayedEvent`, `L1GiftWatcher`'s
removal, and removal of the tag assigned to a `WildTag` after ordinary action work finishes.
Wild assignments use `LATER Admin`, so Admin performs their cleanup even when players use `NONE`.
Other deferred work remains with the actor named by its rule.


## Facts in components, future work in tasks

The component graph records what is, or what is happening. Work that should happen later belongs in
a task. A component that exists so that its removal can start later work is a scheduling device, not
a game fact; do not add new ones. The `workflow` branch's `Continuation` class makes that device
generic.

The owner's proposed Head Start entitlement tokens are a specific design direction recorded in
the [turn design](GAME_FLOW.md#head-start-acceptable-variant-to-pursue). They describe
turns owed to a player; evaluate them with that game meaning rather than rejecting them solely
because turn order will consume them. Their implementation remains open.

Devices replaced by deferral:

| Former device | Deferred work now |
| --- | --- |
| `EventCard` as `Temporary` | `This: LATER { PlayedEvent<Class<This>> FROM This* }`, performed by the owner |
| `TradeBarrier` | `Trade`'s `This: LATER { FinishTrade<@ColonyTile> }` |
| `TurmoilSolarOperation` | `SolarPhase: LATER { TurmoilPhase }`; `TurmoilPhase: ResolveGlobalEvent, LATER { FormGovernment THEN ChangingTimes }` |
| `L1GiftWatcher` as `Temporary` | `LATER { -This }` beside the gift fanout |
| Temporary wild-tag use | The printed `WildTag` holds the assignment: `Tag<This>: LATER Admin { -Tag<This>* }` |

`FinalScoringPending` and the `Temporary` `MeasureAward` were replaced by a phase instead: `End`
scores, and the terminal `VictoryPhase` names the winner after that work has finished. With these
conversions `Temporary`, `TemporaryScope`, and the engine's idle cleanup were deleted.

Where the component also records a real fact, keep it and change only the timing. A played
`EventCard` is in play, and its tags count, until it is flipped; only the flip is deferred work.
