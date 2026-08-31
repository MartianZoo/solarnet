# Catalog Routines and replay records

> **Read when:** implementing or reviewing catalog-contributed Routines, direct Routine calls in the
> REPL, replacement of `tfm_` commands, or the Routine half of a saved-game format.
>
> **Status:** proposal plus a catalog-contributed `DO`-command prototype. The mature surface and
> saved-game model described here remain the target. “Native World export” is an agreed working
> rule for its named miniproject.

## Goal

A **Routine** is custom Kotlin knowledge for selecting and narrowing one or more pending tasks. It
expresses a familiar game interaction without reproducing an `InstructionTree` walkthrough at every
call site. Supporting a game or expansion includes contributing its useful Routines.

Routine calls have two related uses:

1. concise, readable interaction in the REPL and replay tests; and
2. the Player-facing portion of a saved game's explicit Agent-command stream.

A saved game retains both the Routine stream and the concrete state changes it originally produced.
Those support two intentionally different restore modes:

1. reproduce the earlier net component state, issuing adjustments from the recorded changes as
   needed; or
2. replay the fixed player choices through the current engine, accepting that changed rules or
   implementation may produce a different game.

The Routine stream is compact input, not a durable encoding of its consequences.

The native World-export miniproject uses that Routine stream as its readable replay format. The
engine owns the format and `World.export()` API; `script` is only its current importer. This does
not turn a low-level task or concrete-change dump into an acceptable Routine export. The target
format is specified below under “Native World export.”

Pure-Pets Routine implementations are a distant possibility, not an initial goal. The first
implementations are custom Kotlin.

The current prototype models Routine contribution as the engine-level `RoutineProvider` capability,
separate from the base data-oriented `Catalog` interface. `TfmCatalog` implements that capability;
each bundle may contribute a name-to-implementation map, and composite catalogs reject duplicate
names. The core Terraforming Mars bundle owns the initial implementations. `DO` parses a call,
looks it up through the active World's Catalog, and supplies the live World and Actor-scoped
Agent in a `RoutineContext`.

Routine implementations belong to the Catalog module and depend only on the generic engine and
Pets surfaces. They must not call `TfmGameplay`; the transitional `TfmGameplay` facade instead
dispatches overlapping operations through the Catalog's `RoutineProvider` capability.

The prototype uses the current Agent lifecycle described in
[ENGINE.md](ENGINE.md#current-agent-surface). Any later client-facing Routine API should follow
the command lifecycle and temporary script policy in [API.md](API.md#command-semantics).

## REPL surface

The initial prototype exposes Routine calls through `DO`:

```text
EXEC <InstructionGroup>
DO playCard(Pets, -10 MC)
DO tasks(ProjectCard, Animal<Pets>)
```

The stored record contains the plain Routine call without `DO`. Direct top-level Routine dispatch is
the intended mature surface; the wrapper lets the prototype develop without changing general REPL
dispatch yet.

The call syntax is recognized only by REPL dispatch. It is not added to the Pets instruction or
effect AST, so class effects cannot invoke Routines.

Actor attribution is persistent in a replay:

```text
BECOME Dad
DO playCard(Pets, -10 MC)
DO tasks(ProjectCard, Animal<Dad, Pets>)

BECOME Ellie
DO playCard(Psychrophiles, -2 MC)
```

`BECOME` persistently changes the current Actor. Prefer it whenever the recorded actor changes, so
one heading owns the following run of calls. `AS <Actor> <full command>` remains useful for an
isolated exception and restores the prior Actor. A serializer may omit repeated `BECOME` commands
and should keep the current actor across a long sole-player run. Do not introduce `AS Actor { ... }`
blocks.

Every serialized Routine replay sets autoexecution to `NONE` for every Agent, including Engine. Put
`AUTO NONE` before `newgame`, and keep it in force throughout import. A Routine must not restore or
drain a prior policy setting, even temporarily. Meaningful Engine task commands therefore belong in
the replay just as meaningful Player task commands do. Assignment alone does not make an abstract
choice deterministic. A World Government Terraforming task, for example, still needs the recorded
Player narrowing. Do not reassign Player work or alter its Actor, assignee, Cause, or abstractness
merely to make it eligible for Engine execution.

## Native World export

> **Status:** agreed miniproject contract. A passing implementation must satisfy all of these gates;
> uncommitted serializer code and generated artifacts are not evidence that a gate is met.

Before acting on the 0818 export, read its dated
[implementation plan](../../_local/replays/Game20260818/implementation-plan.md).

`World.export(): String` belongs to `engine` and returns a versioned REgo script that today's
`script` module can run. Future importers may consume the same engine-owned format. The first
deliverable is the current-engine explicit-command replay, not the separate concrete-change restore
mode described in the goal.

The exported file must have the same shape as the established
`_local/routines/OtbGame20260818-generations1-6.rego` replay:

1. the exact version marker `// solarnet world export 1`, followed by `auto none`;
2. `newgame` using the exact concise unresolved `GameConfig`, not an expanded cooked `GamePremise`
   containing implicit Modules and negative default selections;
3. persistent `BECOME` headings for Actor runs;
4. `DO playCard(...)` and `DO useAction(...)` for headline decisions and their costs;
5. subsequent `DO tasks(...)` calls for ordinary choices exposed by those decisions;
6. `DO buyCards()`, `DO endTurn()`, and `DO assignWildTag(...)` where those established Routines
   express the choice; and
7. only source-backed direct corrections as brief `mode red` / `exec` / `mode purple` passages.

Do not substitute raw `task` commands for `tasks(...)`, card play, actions, purchases, or turn
declines that the Routine vocabulary already expresses. Do not expand authored `::` effects,
payment plumbing, action markers, or temporary cleanup into fake Player choices. Meaningful
Engine-owned tasks that autoexec would otherwise consume still need an explicit Engine-attributed
command or Routine. Do not switch to green mode merely to replay internal helper choreography. A
same-version round trip that reaches the right final graph only because any autoexec policy silently
chooses omitted work fails this contract.

Start from the existing Event Log and its Cause information, but interpret Cause only as ancestry.
A card or Action may cause a later task that still needs a Player selection or narrowing; that
choice remains explicit in `tasks(...)`. Omit a caused task only when no participant chose any part
of its execution. Before adding a new event kind or public API, demonstrate the exact information
that existing history cannot recover. If the readable Routine call cannot be derived without
materially widening several modules, stop and report that design pressure; do not compensate with a
low-level dump, speculative payment probes, catch-all fallbacks, or autoexecution.

Successful explicit Agent inputs are the intended replay record. The current
`GameplayInputEvent` records only Player input, so Engine-attributed replay commands require a small
input-model extension rather than inference from component changes. The optional `agent` field can
identify an automatic or other command source, is rendered as an Event Log note, and is excluded
from event equality. This provenance must not become game state or affect replay-state equality.

The engine serializer must remain generic. Obtain configuration and Routine facts from the World,
premise, and Catalog capabilities; do not embed Terraforming Mars cards, Player names, the 0818
configuration, or branches for this replay in engine code.

The six-generation replay is an executable golden oracle, not merely a style example. First prove
that it and a fully explicit reference produce the desired equivalent game state with every Agent's
autoexecution disabled on both paths. If they do, the overlapping exported command stream must
preserve the same Routine calls and every ordered, noncommutative choice after its version marker.
Arguments within one call may appear in either order only when those effects commute and the
generated order passes the same round-trip state checks. If parity fails, report the mismatch
instead of silently changing the format. The durable test compares Actors, active Classes,
complete Task queues, and the component graph after importing both the canonical source and a
fresh export.

The completed export is the colocated
`test/common/dev/martianzoo/tfm/tests/replays/OtbGame20260818-world-export.rego`
source replay.
`OtbGame20260818Test` retains the compact independent Kotlin execution path and imports that exact
REgo source through `ScriptSession`. At the idle endpoint it compares Actors, active Classes,
pending Tasks, and the complete component multiset. Incidental task-selection events and Event Log
ordering need not be identical.

## Signatures and parsing

A Routine name follows Kotlin's lower-camel convention, such as `playCard` or `assignWildTag`.
A catalog supplies named Routine signatures and Kotlin implementations. A signature has typed
selector parameters followed, where appropriate, by an `InstructionGroup` parameter. Initial
shapes are:

```text
playCard(card, costs...)
useAction(actionNumber, provider, costs...)
tasks(instructions...)
buyCards()
endTurn()
assignWildTag(tag)
```

`useAction` uses the authored one-based selector:

```text
useAction(2, ExtractorBalloons)
```

The remaining text inside `tasks(...)` is one `InstructionGroup`. For mixed signatures, selectors
are parsed according to the signature and the trailing arguments form the group. Commas nested in
Pets brackets or type arguments do not split Routine arguments.

`tasks` accepts the same task text as the existing REPL task command; Routines do not add a more
permissive task language. Run instruction arguments through the actor-scoped engine preprocessing
pipeline before Routine logic sees them. Vocabulary canonicalization, defaults, owner replacement,
and marked-syntax lowering therefore happen once and early. In particular this is valid surface
syntax:

```text
tasks(CopyPrelude<MartianIndustries>, PROD[Energy, Steel], 6 MC)
```

The `PROD[...]` transform lowers before task matching. In `OtbGame20260818Test`, Double Down copies
only Martian Industries, so `PROD[3 Energy, Steel]` at that point would correctly fail to match; the
earlier two Energy production came from Supplier.

## Invocation contract

Each Routine call is atomic. Any parse error, unmatched or ambiguous task, invalid payment,
remaining debt, unused supplied argument, or failed postcondition rolls the whole call back. Use
the engine's existing operation rollback rather than building Routine-specific recovery machinery.

A Kotlin Routine implementation should produce an ordered substitute list of task selections and
narrowings, analogous to a custom instruction producing its substitute instruction tree. Executing
that list may only select or narrow tasks already in the actor's queue. It must not initiate an
otherwise unavailable game action ex machina.

A call records one player decision. It does not record fixed bookkeeping or absorb the ordinary
tasks exposed by that decision. The first selector chooses the headline task; trailing arguments
may settle only costs. Consequence choices remain pending for a subsequent `tasks(...)` call.

For `playCard` and `useAction`, a live `Billing` stage always settles before consequences. While
that stage exists, trailing arguments select its tender tasks. `useAction` may also consume the
linked first stage of a direct Pets cost such as a card-resource removal. Once costs close, the
Routine returns and leaves every queued consequence untouched. Do not classify later removals as
payment merely because they remove a resource: the live operation stage or linked Action cost
supplies that meaning.

There is no autoexecution during replay. Work proceeds only through:

- task selections made explicitly by Routine Kotlin;
- inline `::` effects; and
- explicit commands for meaningful deterministic workflow work owned by Engine.

A Routine may return with player tasks pending for the next call. Those tasks belong in another call
only when they represent another player decision rather than a fixed continuation of the headline
decision.

### `tasks`

`tasks(group)` preprocesses the group, flattens it to instructions, and selects each member in
written order. A later member may match a task created by an earlier member. Every member must match
exactly one pending task, except that behaviorally identical tasks remain interchangeable under the
engine's existing rule.

Do not implement this by passing the whole group to current grouped `doTask`. That path narrows one
group-shaped task and has different semantics.

Production shorthand is useful here because lowering can expose several ordinary instructions:

```text
tasks(PROD[3 Energy, Steel], 6 MC)
```

This does not yet authorize aggregate matching of one authored `4 MC` against two unrelated `2 MC`
tasks. Exhaustive residual execution is also deferred.

### `playCard` and `useAction`

`playCard` replaces separate corporation, Prelude, and project-card helpers. It infers the card deck
from the selected card and narrows an available `PlayCard` task to that card. Its remaining
arguments cover only that play's Billing stage; queued choices use `tasks(...)`:

```text
playCard(Pets, -10 MC)
tasks(ProjectCard, Animal<Dad, Pets>)
playCard(DoubleDown)
tasks(CopyPrelude<MartianIndustries>, PROD[Energy, Steel], 6 MC)
```

Payment choices may use standard resources or a card's payment resource:

```text
playCard(MiningRights, -4 Steel, -1 MC)
playCard(Potatoes, -Microbe<Psychrophiles>)
useAction(2, TradeSA, -3 Energy)
```

The call does not return while its invoice retains `Owed`. No payment argument is required for a
zero-cost invocation. Valid intentional overpayment remains possible. A supplied instruction that
was not used as a cost is an error.

After card-resource and non-money tenders execute, a Routine rejects an M€ amount greater than the
remaining debt and rolls back the whole call. The engine still cannot validate every non-money
allocation from saturated `Owed` removals; that separate defect is tracked in
[PAYMENTS.md](PAYMENTS.md).

`playCard` does not run the workflow that might later expose its task. For example, Valley Trust's
mandate and card-selection tasks are recorded first; only once they have exposed the queued card
choice does `playCard(DoubleDown)` select it.

Direct card-resource removals and production transformations are ordinary Pets action costs, not
billing. `AerialMappers` declares `Floater<This> -> ProjectCard`, which lowers to a removal followed
by its result. `useAction` may perform that linked cost, but the result remains a separate call:

```text
useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
tasks(ProjectCard)
```

Do not route that removal through settlement handling. See `Direct and costless Actions` in
`ACTIONS.md` and search for `CLASS AerialMappers` in the Venus Next card declarations.

Generic `useAction` is preferred over thin aliases:

```text
useAction(2, TradeSA, -3 Energy)
tasks(Trade<Pluto>, 3 ProjectCard)

useAction(1, ConvertHeatSA, -8 Heat)
tasks(TemperatureStep, TerraformRating)

useAction(1, ConvertPlantsSA, -8 Plant)
tasks(GreeneryTile<Utopia_4_2>, OxygenStep, Plant, TerraformRating)
```

Add a named convenience only when it captures more game knowledge than these generic calls.

### Starting cards and `buyCards`

Starting-card selection, corporation play, and card purchase are three distinct decisions and must
remain distinct in the record. The player first discards any unwanted cards from `Selecting`, then
plays a corporation, then may invoke `buyCards()` after the corporation has supplied its starting
resources:

```text
tasks(2 select)
tasks(10 ProjectCard<Selecting>, -3 ProjectCard<Selecting>)
playCard(PointLuna)
tasks(ProjectCard, 38 MC, PROD[Titanium])
buyCards()
```

The numbered `select` is current REPL task-selection syntax: setup initially offers corporation play
and starting-card selection as two pending tasks, and the replay deliberately opens the latter
first. It is not a separate game decision.

`buyCards()` is deliberately argumentless: invoking it commits to buying every project card still
in the actor's `Selecting` area. The acquisition operation establishes the complete adjusted debt,
settles it, moves those exact cards to Hand only after settlement, and closes the selection. The
invocation point records when the player commits; neither `buyCards(-21 MC)` nor a nested `pay()` is
needed while the purchase offers no tender decision. If card purchase later gains a genuine tender
choice, expose that operation stage directly rather than teaching the Routine to scan arbitrary
removal instructions.

Later research and card-selection effects use the same select-then-commit lifecycle. A different
Routine is justified only when the game presents a different player decision.

### `endTurn`

`endTurn()` means exactly that the player has been offered a second action and declines it. It
selects that optional `SecondAction` offer's `Ok` arm. It does not itself finish the current action,
pass for the generation, or advance workflow, and there is no `endCurrentAction` Routine.

For now, the shared Routine completion bridge must settle `WildTagUse?` tasks when they are the
acting Player's only remaining work, then remove the corresponding `WildTagUse` components. This is
choice-free cleanup around every Routine call, not part of `endTurn()`'s player-decision meaning.
The sequencing goal is an exact end-of-action hook that performs this settlement before offering a
second action or returning control, at which point the Routine bridge should be deleted.

## Recording conventions

- Record full tile tasks such as `OceanTile<Utopia_4_1>` and `CityTile<Utopia_3_2>`; do not replace
  them with coordinate-only helpers.
- For an Event card, put its movement last among the explicitly selected consequences:
  `PlayedEvent<Class<Flooding>> FROM Flooding`.
- Record a transfer with native Pets syntax, for example `3 MC<Dad FROM Ellie>`.
- `assignWildTag(EarthTag)` records the requested concrete tag while allowing task matching to find
  the available wild tag being assigned.
- Decline an optional second-action offer with `endTurn()`; use `tasks(Pass)` for a generation pass.
- Prefer persistent `BECOME` grouping for actor attribution.
- World Government Terraforming is an ordinary task with explicit attribution, for example
  `tasks(OxygenStep! BY Engine)` or `tasks(OceanTile<Utopia_9_8>! BY Engine)`.

## Choice-free and Engine-owned work

The Routine stream contains player decisions, not chores needed to make the model continue. Repair
choice-free work at its source in this order:

1. use an inline `::` effect when the consequence must already be true before a coherent World is
   returned to a Player;
2. assign queued deterministic game or workflow work to Engine when it is meaningful work but no
   participant chooses its outcome; or
3. attach cleanup to the exact lifecycle completion event when it must wait. Leave the mechanism
   open when no such event exists instead of using the Player's whole queue as a generic signal.

A Routine may consume such work only as a temporary last resort. Doing so conceals a misplaced
effect, owner, or completion rule and must be documented as model debt. In particular, scaffolding
signals such as a mandate pulse should not become player-facing Routine arguments merely because a
current implementation queues them.

Production, colony advancement, research offers, trade bookkeeping, and workflow pulses should be
Engine-selected deterministic work. Player-owned types and effect context must remain intact even
when Engine selects the task. Assignee, execution Actor, context owner, and the owner encoded in a
changed Type must not be collapsed; see `IDENTITY.md`.

Parity checks belong at quiescent player points, when player queues are empty and the workflow is
about to advance. Compare Actors, active Classes, complete Task queues, and the component graph
there. If they match, ordering differences among individual changes and incidental task-selection
events do not matter.

The recorded concrete changes include engine work and support exact-state restoration. The Routine
stream is not a replacement for that record.

## Evidence

`OtbGame20260818-world-export.rego` is the full source replay and owns the sourced commentary.
`OtbGame20260818Test` is its compact independent Kotlin parity path. It compares both that source
replay and a fresh export of the completed Kotlin World against the same complete endpoint. The
earlier six-generation Routine oracle remains
`_local/routines/OtbGame20260818-generations1-6.rego` as design history, not duplicated test input.
Focused `DoCommandTest` cases cover command mode and parsing contracts.

## Open implementation choices

- Typed Routine signatures beyond the prototype's name-to-Kotlin-implementation registry.
- A richer saved-file envelope around the versioned native export.
- The smallest additional recorded input fact, if any, that the incremental export gates prove the
  existing Event Log cannot recover.
- The explicit input representation and Routine vocabulary needed for Engine-owned replay commands.
- Multiline Routine calls and completion behavior in the interactive REPL.
- Whether interactive Routine calls respect current autoexecution and report already-completed
  tasks as skipped.
- Whether Routine names use vocabulary aliases or only canonical names.

Do not add exhaustive residual execution or pure-Pets Routines in the first implementation.
