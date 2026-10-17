# Canon turns and game flow

> **Status:** current authored flow, action lifetimes, and the agreed restriction on scope-removal
> effects. Remaining design questions are labeled separately.
> [Solarnet sequencing](../../../docs/agents/SEQUENCING.md) owns task dispatch, execution,
> and completion checks.

## Turns and turn offers

`ActionScope<Player>` records an accepted action and its resolution. It is globally unique and
identifies the active player. A published action-phase turn of up to two actions contains up to
two consecutive scopes, even when both belong to the same player. `TfmGameplay.turn { }` groups
that published turn and declines an unused second opportunity.

### Offering, taking, and finishing an action

- The first action-phase offer is `StandardAction OR Pass`, or its required-action equivalent.
  Passing creates no scope and ends that player's participation for the generation.
- Accepting `StandardAction`, `DoRequiredActions`, or `PlayCard` creates a scope only when
  none exists. Generic `UseAction` does not create a scope: reactive abilities and nested card
  plays resolve within the current action.
- The scope remains through payment, effects, placement bonuses, other players' responses, and
  `LATER` cleanup. Its own `FINALLY { -This }` ends that lifetime. Scope-removal reactions schedule
  further `FINALLY` work to offer another action or pass the baton, after fixed removal
  consequences finish.
- The optional second offer lets the player accept another action or request `PassBaton`. Declining
  creates neither a scope nor `Pass`. The last unpassed player receives a fresh first offer after
  each action, including in solo play.

| Activity | ActionScope lifetime |
| --- | --- |
| Initial corporation play | Includes starting project-card payment and transfer |
| Each Prelude played during the Prelude phase | Includes its effects and nested plays |
| Each accepted action-phase opportunity | A fresh scope, including the optional second action |
| Each final greenery placement | Includes placement effects; passing the baton creates no scope |
| Fake Head Start | Its resources and both granted actions share the enclosing scope |

An unplayable Prelude's removal also starts a scope when none exists, covering its compensation.
Merger's corporation and New Partner's further Prelude remain inside the scope already in progress.
Research, production, and solar-phase decisions do not acquire scopes merely because a player
receives work. [Identity](../../../docs/agents/IDENTITY.md#who-is-on-turn-and-who-decides) separates
this active-player fact from task assignment and the still-proposed routing changes.

### Scope removal must not require choices

Removing `ActionScope`, or any component that expires with it, must not directly or indirectly
create player choices belonging to the completed action. Finish those choices while the scope
exists, including choices made by another player. Wild-tag elections and Suitable
Infrastructure bonuses have already disappeared when removal effects run.

Removal may perform fixed effects, clean up, and advance game flow. Offering the next action or
phase is allowed: those decisions belong to subsequent play. Accepting the next action creates its fresh
scope before that action's consequences begin.

This is a Canon authoring rule, not a restriction on the generic `Scope` mechanism.
`CanonInvariantsTest.actionScopeExpiryOnlyPerformsReviewedHandoffs` scans removal listeners for
scope and baton removals, their possible indirect dependents including
`SecondAction`, and `PassBaton` listeners. It accepts fixed turn bookkeeping and next-play
instructions regardless of the listener's class name; optional bookkeeping (including defaults), arbitrary helper gains,
and new listener forms fail for review. This deliberately rejects new fixed rewards too, since
their reactions could require a choice. It checks instruction structure, not snapshots of Canon
declarations, and is not a general analysis of all downstream effects after a flow handoff.

Check indirect effects as well as direct removal listeners; an Agent automatically selecting a
choice does not make the declaration comply. Do not extend the old action past scope removal to
accommodate a new choice. See [operation completion](../../../docs/agents/SEQUENCING.md#operation-completion)
for the engine's separate operation check.

### Baton, action offers, and rotation

`Baton<Player>` records whose table turn is in progress. It stays with the player across both
ordinary actions, across all their Preludes, or across repeated final greeneries. `ActionScope`
records the shorter lifetime of an accepted action. There is no baton during setup, research,
production, or solar work. Baton needs no phase dependency: the current phase supplies the rules,
and its final baton is removed before the phase changes.

`StartToken` responds to entry into any `TurnBasedPhase` by giving its owner the baton.
Corporation, Prelude, Action, and Final Greenery are the phases in that family. The baton itself
requests an `OfferAction` when gained; the phase or `InRound` supplies the choices. During the
action phase, a passed player has no `InRound`, so that request offers no action and `Pass`
requests `PassBaton`. When another player is still in the round, `InRound` grants a baton-dependent
`SecondAction` allowance. Completing the first action consumes that allowance and offers
`StandardAction OR PassBaton`, or its required-action equivalent. Completing the second action
requests `PassBaton`. A player already carrying `Pass` passes a received baton onward; no action
scope is opened. When only a single player remains, there is no second-action allowance: after
each action, the baton cycles back with another first offer, which includes passing. Solo play
keeps the baton and requests another `OfferAction` instead of removing and regaining it.

`NextPlayer` responds to `PassBaton` with a deferred `Baton<Next@ FROM Previous@>` transmutation.
It removes the baton instead when nobody remains in the action round, or when the next seat is
the start player outside the action phase. Solo rules likewise remove the baton when its phase
finishes. Phase-ending listeners use `-Baton IF MAX 0 Baton`: a transfer leaves a baton in play,
whereas the final removal leaves none. These rules have no lexical `Me`, and their phase-changing
instructions need no player recipient, so bare `Baton` counts every player's baton. `RoundOver`
is no longer needed.

Red Appeasement's pass removes `InRound` immediately, but passes the baton only after the current
action finishes. Its event flip and rewards therefore finish before
the next player receives an offer. A pass during Prelude resolution does not interrupt that
player's remaining Preludes, and generation-scoped `Pass` does not skip final greenery.

`OfferAction` is a request for the phase's next activity, not another record of the turn holder.
The phase or `InRound` supplies the choice. A request submitted without a baton still opens an
accepted action's scope and cleanup, but finishing it does not rotate seats. Sandbox scenarios
and manual REPL modes use this distinction without changing the game's rules. The script's `turn`
command calls `beginOperation("OfferAction")` directly outside purple mode. `TfmGameplay.inTurn`
requests an offer only when its actor has no pending task; generic Agent APIs know only operations.
This signal was previously called `NewTurn`; the new name distinguishes a single offer from the full
baton turn.

The two remaining flow signals have separate purposes: `OfferAction` requests a choice, while
`PassBaton` requests a handoff or the final removal. Modules select the next phase when no baton
remains. This replaces `RoundOver` without increasing the number of flow signals.

### Uses of the scope

The printed `WildTag` is inert and persists with its card. `WildTagElection<WildTag, ActionScope>`
holds its chosen `Tag` during the action; removing the scope removes both election and assignment.
The next action starts with fresh elections. A newly played wild also receives an election during
an existing scope, preserving current same-action availability. Another player's response does
not create elections for that player. See [wild tags](CARD_MODEL.md#wild-tags).

`SuitableInfrastructureBonus` depends on its card and its owner's scope. The card grants the
helper at scope creation; a production gain consumes it for the bonus, and unused helpers expire
with the scope. This gives separate rewards for consecutive actions and a single reward when a
Prelude raises several production tracks. It does not establish a disputed ruling for every
out-of-turn production interaction; those eligibility questions remain separate from the selected
lifetime model.

### Head Start: restored approximation

`FakeHeadStart` directly grants its steel, money, and two mandatory action choices on entry.
Each choice allows a standard action when no required action remains, or requires
`DoRequiredActions` otherwise. These tasks finish within the enclosing action scope before
ordinary turn flow resumes; there is no extra-action counter or special completion handler.

This deliberately restores the simpler approximation rather than exact published timing.
Resources and both actions may interleave, and the money calculation can occur after selling
cards. The granted actions share the enclosing scope, so Suitable Infrastructure's bonus is
available only once and wild-tag elections persist across them. `HeadStartTest` records the
known limitations and verifies that both mandatory actions finish before ordinary play resumes.

### Remaining design work

- `ActionSlot` / `Action1` / `Action2` select declared capabilities, not first and second action
  opportunities. `Activate`, `Activatable`, and `AbilitySlot` remain unselected naming ideas.
- The active scope does not implement the proposed on-turn-first task routing in
  [Identity](../../../docs/agents/IDENTITY.md#who-is-on-turn-and-who-decides).

`ActionScopeTest`, `SuitableInfrastructureTest`, `WildTagTest`, `WildTagContinuationTest`, and
`HeadStartTest` cover the relevant gameplay scenarios. Compilation or isolated scope examples do
not replace those interactions and full replay reconstruction when changing this model.

## Generated action-phase diagram

Run `./scripts/action-turn-diagram` from the Solarnet root to rebuild the teaching walkthrough and
poster in `_local/diagrams/action-phase/`. The command requires Java, Node, and npm; pinned Graphviz
Wasm and SVG rendering packages are installed only in a temporary directory. It produces HTML with
a clickable overview and detailed event destinations, SVG, PNG, combined and per-panel DOT, and a
source evidence listing. Overview arrows are possible authored paths; their guards and choices are
shown in the linked panels.

`ActionTurnDiagram` in the Pets junkdrawer parses the current working-tree `.pets` files. Panel
order and titles are editorial; guards, instruction trees, rule owners, source lines, and destination
links come from the AST. Event matching follows declared supertypes, so a listener on
`TurnBasedPhase` appears when `ActionPhase` begins. Its documented slice covers action-phase control
rules, without expanding card/payment work or runtime dispatch. `rules.md` records the slice and input SHA-256 hashes.
For DOT alone, run `./gradlew -p pets :junkdrawer:actionTurnDiagram` with
`-PactionTurnDiagramOutput=<output-directory>`.

## Game flow

The game's own rules carry play from `SetupPhase` to `VictoryPhase`. A client starts the game with
`TfmGameplay.startGame`, which performs `SetupPhase FROM BootstrapPhase`.

- **Phases without turns** dispatch their `FINALLY` handoff as they begin. Production proceeds to
  `SolarPhase` while a `GameEndBarrier` exists and otherwise signals `CheckGameEnd`.
- **Phases with turns** advance by passing the baton after accepted scopes finish, or
  through the offered pass/decline path. A phase-wide final handoff would run after its first
  action, so it cannot replace those rules.
- **Selection cleanup** begins with `ActionScope`: Admin removes remaining card backs in that
  player's `Selecting` under `LATER`, after purchase payment and transfer and before scope removal.
  Research schedules analogous project-card cleanup for all players before its phase handoff.
- **Seat order** follows `PassBaton` through `NextPlayer`, with the solo equivalent on `SoloMode`.
  Its `FINALLY` handoff transfers the baton or removes it at phase end.
- **Modules own phase order.** Final corporation baton removal enters `ActionPhase` through
  `NoPreludes`, or `PreludePhase` through `PreludeExpansion`. Final Prelude baton removal enters
  the action phase.
  Expansions add solar work to `SolarPhase`; Turmoil uses `LATER` to follow the other solar work.
- **Final greenery** offers another placement after each completed scope. Declining requests
  `PassBaton`; no separate `Done` marker is needed. With no baton, the request has no effect,
  so a standalone offer can also finish.

## Cleanup

| Class | Current meaning |
| --- | --- |
| `MustCleanUp` | State forbidden by operations that declare themselves complete |
| `Barrier` | `MustCleanUp` state removed by its owning game rule |
| `Signal` | An unscoped point event that removes itself automatically |
| `Scope` | A lifetime anchor whose removal also removes dependent components |

Nothing disappears merely because the World is idle. Event-card flips, trade completion, and gift
watcher removal remain authored deferred tasks. `ActionScope` similarly schedules its own removal
under `FINALLY`; dependency removal then clears its wild elections and Suitable Infrastructure
helpers. `ActionScope` is not `MustCleanUp`: the current completion check runs before its removal.

## Facts in components, future work in tasks

Components record what exists or is happening. `ActionScope` records the current accepted action;
`Baton` records the current table turn and `SecondAction` its remaining second-action allowance.
Deferred tasks determine when those facts change. Do not add a component whose only meaning is
an arbitrary continuation waiting to run.

Event cards remain in play until their `LATER` flip; `Trade` uses deferred `FinishTrade`, Turmoil
uses deferred phase work, and `L1GiftWatcher` defers its removal. These do not require the deleted
`Temporary`, `TemporaryScope`, or engine idle cleanup. `End` performs final scoring, then hands off
to the terminal `VictoryPhase`.
