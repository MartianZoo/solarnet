# Shared Terraforming Mars routines

> **Agent information:** Current implementation and selected contract.
> **Read when:** changing named gameplay interactions, importer directives, or REPL routine calls.

`Agent.doRoutine` is the invocation API. `Agents` receives a fixed map of routine definitions;
the generic Agent layer knows no Terraforming Mars names or rules. `TfmRoutines.registered` in
`tfm-engine` supplies the Terraforming Mars definitions, all named in lowerCamelCase. They remain
outside the Pets model, which owns legality, costs, requirements, and effects. Registration has no
catalog or bundle contribution mechanism.

Kotlin callers install the definitions when creating the session:

```kotlin
val agents = Agents(world, routines = TfmRoutines.registered)
val player = agents[playerActor]

player.doRoutine("playCard", "OlympusConference")
player.doRoutine("pay", "2 Steel", "6 MC")
player.doRoutine("action", "ConvertHeatAction")
player.doRoutine("action", "TradeAction", "2")
```

`action` takes the modeled StandardAction provider's name and an optional action number, defaulting
to 1. The number must be a positive integer; the model owns which slots exist, along with costs,
eligibility, and effects. Unavailable slots fail without consuming the action. Standard projects
and card actions retain their respective invocation routines, which select their normal opening
action when needed.

The REPL accepts `playCard(OlympusConference)` and `pay(2 Steel, 6 MC)`, or the usual command
spelling `pay 2 Steel, 6 MC`. `RoutineCommand` adapts input and output; it contains no gameplay
implementation. Ordinary `task` submissions resolve card effects, placements, and choices,
including linked variable costs.

The fixed definitions own operation names, argument counts, parsing, and action classification.
The importer retains archive headers, brace bodies, recipient syntax, and line-numbered errors.
Setup, Research, Production, WorldGovernment, and GlobalEvent remain structural groups of
explicit body instructions. Their phase checks perform no phase tasks themselves. Pets types
and signals keep their modeled spelling; lowerCamelCase applies to executable routine names.

Each invocation restores all its changes if it fails, including automatic work. Earlier returned
calls remain subject to any enclosing transaction. An archive action body consists of separate
calls, so a failed pay leaves the earlier declared play and bill available for another attempt.
Routines may return with payment, choices, or ordinary effects pending when Agent policies leave
them explicit. Forks retain the definitions and use the receiving session's Agents and world;
definitions capture no source session.

playCard uses an already granted play when present and otherwise invokes the normal card-play
action. It preserves the caller's chosen source, defaulting to Hand. Card placement is the
model's `PendingCardPlay` consequence after payment and requirement preparation; there is no
Kotlin pending-card callback. Card-action and standard-project routines similarly use a granted
action or invoke its normal opener. Starting a different Player's action declines the previous
Player's unused second action through that Player's Agent, provided the earlier action finished.

pay is a complete stated allocation, preserving written order. It supports standard resources
and card-held resources, closes unused offers, and checks that no bill remains. It adds no
check for removable excess resource units. While applying the allocation, it uses scoped NONE
overrides for the paying Player and optional releasing Player; Admin remains mandatory.
It restores the enclosing effective policies and runs configured work before returning.
`Agent.doWithAutoExecPolicy` supplies the scoped override without changing the configured
setting. See [payments](PAYMENTS.md) for the model and allocation rules.

`DoRoutineTest` exercises generic invocation restoration and fork ownership. `TfmRoutinesTest`
covers explicit versus automatic effects, payment restoration and sources, granted actions,
standard projects, default action numbers, and numbered trades. `RoutineCommandTest` checks REPL
dispatch. Archive scenarios and active/archived replay round trips verify importer/exporter
composition with the same implementations.
