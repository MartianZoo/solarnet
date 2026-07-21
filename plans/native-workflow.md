# Native Pets Workflow

## Goal

Replace the Terraforming Mars-specific Kotlin workflow with a native workflow whose game-specific
structure and behavior come from Pets declarations. The engine may provide generic task and
workflow semantics, but Kotlin must not actively manage the Terraforming Mars phase sequence or
know which expansions insert phases.

This file is authoritative for phase ordering and terminal behavior where an earlier focused plan,
such as `world-government.md`, differs.

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
subphase goes to `GenerationPhase`, then `ResearchPhase`, then back to `ActionPhase`.

`SolarPhase` is universal. Although introduced by an expansion in the published rules, Solarnet
treats its game-end-check step as having been retroactively added to the base game's workflow.

### Generation 1

`SetupPhase` must create generation 1 before any player involvement. The first generation has no
`GenerationPhase` or `ResearchPhase`; setup and corporation selection already serve the relevant
purposes, and follow mode has no meaningful initial deal or research operation to perform.

Consequently the initial path is:

`SetupPhase -> CorporationPhase -> [PreludePhase] -> ActionPhase`

Later `GenerationPhase` instances implement the published Player Order phase: advance the
generation and pass the first-player marker. They occur only between generations, immediately
before `ResearchPhase`.

### Expansion-owned insertion

Each expansion must declare its own workflow contribution without Kotlin knowing about it:

- `PreludeExpansion` inserts `PreludePhase` after `CorporationPhase`.
- `VenusNextExpansion` inserts `SolarVenusPhase` after `SolarPhase`.
- `ColoniesExpansion` inserts `SolarColoniesPhase` after every member of
  `{SolarPhase, SolarVenusPhase}` that actually exists in this game.
- `TurmoilExpansion` inserts `SolarTurmoilPhase` after every member of
  `{SolarPhase, SolarVenusPhase, SolarColoniesPhase}` that actually exists in this game.

These are precedence constraints over the phases present in one configured game. Merely
expressing a constraint must not cause an absent expansion phase class to be loaded. Thus the
resulting Solar order is always the applicable subsequence of:

`SolarPhase -> SolarVenusPhase -> SolarColoniesPhase -> SolarTurmoilPhase`

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
global parameters, Venus solo includes the Venus parameter, and TR 63 solo uses Terraform Rating
63. These predicates affect victory at the terminal check, never the number of generations played.

## Workflow-state principle

Phases or subphases may be game components because they represent the game's current temporal and
rules context. Pending work, readiness, and waiting belong to Tasks and should not be duplicated as
components merely to tell the engine that something remains to be done.

When Engine gives a Player a turn, this is control-until-drain rather than the one-instruction
`BY` operation described in [identity-transition.md](identity-transition.md). Engine keeps its
continuation task in its suspended queue; consequences of the Player's work remain in the Player's
queue; and Engine resumes only after that queue drains and any nested delegation has returned. The
workflow must therefore not depend on Engine's queue becoming physically empty.
