# Shared Terraforming Mars routines

> **Agent information:** Current implementation and selected contract.
> **Read when:** changing named gameplay interactions, importer directives, or REPL routine calls.

`TfmRoutines` in `tfm-engine` owns the Kotlin implementations and their fixed `registered` map.
Routines remain outside the Pets model. They translate stated decisions into the existing Agent
API; the model owns legality, costs, requirements, and effects. Registration has no catalog or
bundle contribution mechanism.

Kotlin callers share a session's Agents:

```kotlin
val routines = TfmRoutines(agents)
routines.execute(player, "PlayCard", "OlympusConference")
routines.execute(player, "Pay", "2 Steel", "6 MC")
```

The REPL accepts `PlayCard(OlympusConference)` and `Pay(2 Steel, 6 MC)`, or the usual command
spelling `Pay 2 Steel, 6 MC`. `RoutineCommand` adapts input and output; it contains no gameplay
implementation. Ordinary `task` submissions resolve card effects, placements, and choices,
including linked variable costs.

The registry owns operation names, argument counts, argument parsing, and action classification.
The importer retains archive headers, brace bodies, recipient syntax, and line-numbered errors.
Setup, Research, Production, WorldGovernment, and GlobalEvent remain phase-checked groups of
explicit body instructions. The importer's internal phase check performs no phase tasks itself.

Each executable routine restores its entire invocation if it fails, including any automatic work.
Earlier successful calls remain. An archive action body consists of separate calls, so a failed
Pay leaves the earlier declared play and bill available for another attempt. Routines may return
with payment, choices, or ordinary effects pending when the Agent's policy leaves them explicit.

PlayCard uses an already granted play when present and otherwise invokes the normal card-play
action. It preserves the caller's chosen source, defaulting to Hand. Card placement is the
model's `PendingCardPlay` consequence after payment and requirement preparation; there is no
Kotlin pending-card callback. Action routines similarly use a granted action or invoke its normal
action opener. Starting a different Player's action declines the previous Player's unused
second action through that Player's Agent, provided the earlier action has finished.

Pay is a complete stated allocation, preserving written order. It supports standard resources
and card-held resources, closes unused offers, and checks that no bill remains. It adds no
check for removable excess resource units. While applying the allocation, it uses scoped NONE
overrides for the paying Player and optional releasing Player; Admin remains mandatory.
It restores the enclosing effective policies and runs configured work before returning.
`Agent.doWithAutoExecPolicy` exposes the existing scoped override to such callers without
changing the configured setting. See [payments](PAYMENTS.md) for the model and allocation rules.

`TfmRoutinesTest` exercises direct Kotlin use, explicit versus automatic effects, allocation
restoration, card-held resources, granted actions, and standard projects. `RoutineCommandTest`
checks REPL dispatch. Archive scenarios and active/archived replay round trips verify importer
composition with the same implementations.
