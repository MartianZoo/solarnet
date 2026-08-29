# Catalog Routines and replay records

> **Read when:** implementing or reviewing catalog-contributed Routines, direct Routine calls in the
> REPL, replacement of `tfm_` commands, or the Routine half of a saved-game format.
>
> **Status:** proposal plus a narrow `DO`-command prototype. The mature surface and saved-game
> model described here remain the target.

## Goal

A **Routine** is custom Kotlin knowledge for selecting and narrowing one or more pending tasks. It
expresses a familiar game interaction without reproducing an `InstructionTree` walkthrough at every
call site. Supporting a game or expansion includes contributing its useful Routines.

Routine calls have two related uses:

1. concise, readable interaction in the REPL and replay tests; and
2. the player-input stream of a saved game.

A saved game retains both the Routine stream and the concrete state changes it originally produced.
Those support two intentionally different restore modes:

1. reproduce the earlier net component state, issuing adjustments from the recorded changes as
   needed; or
2. replay the fixed player choices through the current engine, accepting that changed rules or
   implementation may produce a different game.

The Routine stream is compact input, not a durable encoding of its consequences.

Pure-Pets Routine implementations are a distant possibility, not an initial goal. The first
implementations are custom Kotlin.

The prototype uses the current Gameplay operation scopes described in
[ENGINE.md](ENGINE.md#current-gameplay-surface). Any later client-facing Routine API should follow
the command lifecycle and temporary script policy in [API.md](API.md#command-scopes).

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
DO playCard(Pets, -10 MC, Animal<Dad, Pets>, ProjectCard)

BECOME Ellie
DO playCard(Psychrophiles, -2 MC)
```

`BECOME` persistently changes the current Actor. Prefer it whenever the recorded actor changes, so
one heading owns the following run of calls. `AS <Actor> <full command>` remains useful for an
isolated exception and restores the prior Actor. A serializer may omit repeated `BECOME` commands
and should keep the current actor across a long sole-player run. Do not introduce `AS Actor { ... }`
blocks.

Restore-oriented execution forces player-task autoexecution to `NONE`; a future stepwise mode may
use `SLOW`. Interactive Routine calls may eventually respect the session's current setting and
report a task already handled automatically as skipped, but the prototype need not support that.
Set `AUTO NONE` before starting a purple restore so setup tasks are not consumed before the first
Routine call. Each `DO` also makes that setting sticky for the remainder of the session.

## Signatures and parsing

A Routine name follows Kotlin's lower-camel convention, such as `playCard` or `assignWildTag`.
A catalog supplies named Routine signatures and Kotlin implementations. A signature has typed
selector parameters followed, where appropriate, by an `InstructionGroup` parameter. Initial
shapes are:

```text
playCard(card, choices...)
useAction(actionNumber, provider, choices...)
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

A call records one player decision and the further player choices exposed by that decision. It does
not record fixed bookkeeping. The first selector chooses the headline task; trailing arguments are
then consumed in written order against the live stages created by that task. A stage may consume no
arguments when it exposes no player choice.

For `playCard` and `useAction`, a live `Billing` stage always settles before consequence choices.
While that stage exists, leading arguments select its tender tasks. Once Billing closes, remaining
arguments select the queued consequences. Do not classify an argument as payment merely because it
is a resource removal: the live operation stage supplies that meaning. Payment and consequences
therefore cannot be interleaved, and direct card-resource costs remain ordinary consequences when
there is no Billing stage.

There is no general Player autoexecution in the first version. Work proceeds only through:

- task selections made explicitly by Routine Kotlin;
- inline `::` effects; and
- deterministic workflow work explicitly owned by Engine.

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
from the selected card and narrows an available `PlayCard` task to that card. The remaining
arguments cover that play's Billing stage first and then its queued player choices:

```text
playCard(Pets, -10 MC, Animal<Dad, Pets>, ProjectCard)
playCard(DoubleDown, CopyPrelude<MartianIndustries>, PROD[Energy, Steel], 6 MC)
```

Payment choices may use standard resources or a card's payment resource:

```text
playCard(MiningRights, -4 Steel, -1 MC)
playCard(Potatoes, -Microbe<Psychrophiles>)
useAction(2, TradeSA, -3 Energy)
```

The call does not advance to consequence arguments while its invoice retains `Owed`. No payment
argument is required for a zero-cost invocation. Valid intentional overpayment remains possible.
A supplied instruction that was not used is an error.

`playCard` does not run the workflow that might later expose its task. For example, Valley Trust's
mandate and card-selection tasks are recorded first; only once they have exposed the queued card
choice does `playCard(DoubleDown)` select it.

Direct card-resource removals and production transformations are ordinary Pets action costs, not
billing. `AerialMappers` declares `Floater<This> -> ProjectCard`, which lowers to a removal followed
by its result. Because its Billing stage is empty, the same Routine proceeds directly to those
choices:

```text
useAction(2, AerialMappers, -Floater<Dad, AerialMappers>, ProjectCard)
```

Do not route that removal through settlement handling. See `Direct and costless Actions` in
`ACTIONS.md` and search for `CLASS AerialMappers` in the Venus Next card declarations.

Generic `useAction` is preferred over thin aliases:

```text
useAction(2, TradeSA, -3 Energy, Trade<Pluto>, 3 ProjectCard)

useAction(1, ConvertHeatSA, -8 Heat, TemperatureStep, TerraformRating)

useAction(1, ConvertPlantsSA, -8 Plant, GreeneryTile<Utopia_4_2>, OxygenStep, Plant,
    TerraformRating)
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
playCard(PointLuna, ProjectCard, 38 MC, PROD[Titanium])
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
selects that optional `SecondAction` offer's `Ok` arm. It does not finish the current action, settle
wild tags, clear temporary state, pass for the generation, or advance workflow. Those are
completion or Engine responsibilities, and there is no `endCurrentAction` Routine.

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
3. attach cleanup to the owning completion scope when it must wait for descendants, or to
   `UntilYield<Player>` when it belongs at the next controlled Player queue drain.

A Routine may consume such work only as a temporary last resort. Doing so conceals a misplaced
effect, owner, or completion rule and must be documented as model debt. In particular, scaffolding
signals such as a mandate pulse should not become player-facing Routine arguments merely because a
current implementation queues them.

Production, colony advancement, research offers, trade bookkeeping, and workflow pulses should be
Engine-selected deterministic work. Player-owned types and effect context must remain intact even
when Engine selects the task. Assignee, execution Actor, context owner, and the owner encoded in a
changed Type must not be collapsed; see `IDENTITY.md`.

Parity checks belong at quiescent player points, when player queues are empty and the workflow is
about to advance. Compare the complete component graph there. If it matches, ordering differences
among individual changes and incidental task-selection events do not matter.

The recorded concrete changes include engine work and support exact-state restoration. The Routine
stream is not a replacement for that record.

## Evidence

`OtbGame20260818Test` is the design replay. The local pre-prototype six-generation mockup,
automatic-execution trace, and current runnable `.rego` replay are stored under `_local/routines/`.
`DoCommandTest` follows all six mocked generations in executable `DO` syntax and compares the
complete component graph incrementally.

## Open implementation choices

- Exact declaration format for catalog signatures and their Kotlin implementation registry.
- Saved-file envelope, versioning, premise encoding, and Event Log encoding.
- Concrete-change encoding and the adjustment algorithm for exact-state restoration.
- Multiline Routine calls and completion behavior in the interactive REPL.
- Whether interactive Routine calls respect current autoexecution and report already-completed
  tasks as skipped.
- Whether Routine names use vocabulary aliases or only canonical names.

Do not add exhaustive residual execution or pure-Pets Routines in the first implementation.
