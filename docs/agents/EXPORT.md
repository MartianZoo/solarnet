# Human-readable game archives

> **Agent information:** Current game export/import design and migration status.
> **Read when:** changing archives, event reconstruction, or recorded card identities.

The language's priorities and accepted decisions are in the human-facing
[game archive language design](../GAME_ARCHIVE_LANGUAGE.md). This note owns implementation status.

## Single archive path

`GameArchiveExporter` and `GameArchiveImporter` live in `tfm-engine`. The exporter reads a
`GameRecording` and the original compact `GameConfig`; the importer submits explicit Pets tasks
through Agents with Player autoexecution disabled. There is no AUTO and no captured Agent-input
stream. `TaskLog`, `RecordedInput`, `GameFileImporter`, the full/short JSON codecs, and their hash
machinery have been removed. The engine still records events, and `GameRecording` still supports
in-memory passive playback. It no longer has a separate game-file codec.

Ordinary play is untracked. An optional `Cards` header records named movements and opts into
CardTracking checks. It is not required for anonymous hands. `SetupRules` preserves premise-local
Pets declarations, including custom player setup and solo neutral tile locations. The original
GameConfig keeps card pools compact rather than listing the entire resolved catalog.

`FullGameTest.completedGameArchive()` exports `.game` text. Every JVM replay run imports its
checked-in fixture through the same catalog and compares final inventory and pending tasks while
ignoring task IDs and event ordinals. The opt-in export hook additionally writes and verifies a fresh
uncommented export. `dumpEventlog` imports `.game` files before producing TSV. There is no JSON
fallback. Old JSON exports are removed when an export run refreshes the directory.

Replay scenarios have fixtures under `modules/tfm-tests/src/jvmTest/resources/games/`, named
for their Kotlin test class. Source comments, dialogue, corrections, uncertainty, and named-card
evidence are retained near the relevant entries; Kotlin-only TaskForm helper explanations remain
in the test source.
Generation methods supply headings where the source otherwise had no heading. The Kotlin tests
retain their assertions and original commentary. Normal exports never overwrite these fixtures.
When a replay changes, update its fixture deliberately and preserve the surrounding comments.

## Migration verification

`./gradlew :tfm-tests:archiveDraftTest` uses the maintained November 11 replay fixture by default.
It compares each generation and final scoring, the multiset of all inventory changes, resolved
action order and choices, and repeat-export stability. It also checks malformed input and
incomplete/incorrect tasks. Optional paths are `-ParchiveFile=/absolute/input.game` and
`-ParchiveOutput=/absolute/output.game`; supplied input uses the current archive syntax.

The user approved `VIA` for explicit source qualification through the normal task matcher.
`selectTask(...)` preserves choices such as Flooding's ocean-and-attack option. The approved
CardTracking callback supplies names from the header during validation. Invalid names still fail
and roll back the Agent command; ordinary games have no tracker. Flat nested card plays,
card-held payments, granted actions, and recipient-prefixed payments are implemented.
The replay scenarios passed both checked-in-fixture import and fresh export/re-import with
equal final component counts and pending tasks. Replays that relied on test-only New Holland were
later removed. Older solo replays explicitly submit their existing expected VP totals. Pharmacy
Union also verifies a linked choice whose System first step executes through Admin during selection;
the exporter records that selection without a second execution.
The local archive verification covered every November 11 generation, all inventory changes, action
order, stable repeated export, source qualification, invalid-source failures, payment/card-name
failure behavior, trailing turn declines, and persistent
deferred priorities. Verification command:
`./gradlew :tfm-tests:archiveDraftTest :tfm-tests:replayTest :tfm-tests:archivedReplayTest -PreplayExports`.
The full build passed after the turn-rule merge with `./gradlew build --max-workers=1`.
An initial Wasm incremental-cache failure cleared after cleaning the affected Pets build outputs; the parallel retry hit browser
timeouts. The successful serial build used the existing tests and time limits unchanged.

The request-directed matching work was checked with all 643 engine and 85 Agent JVM tests,
the full TFM JVM suite (1,368 cases, 26 existing skips), and then the final 10 focused archive
cases after adding the exporter total check. Fixture imports, fresh export/re-imports, local archive
checks, and the active Web browser tests also passed. Engine, Agent, and TFM JavaScript compiled.
The generated engine/Agent browser tasks are intentionally inert;
they are not counted as executed browser tests.

### Current draft syntax and ownership

`Setup` and `Research` are phase-checked groups of explicit tasks; no discard counts are inferred.
`useAction1/2(Name)` is for action cards. It uses an already granted card action when present,
otherwise the existing card-action opener; standard projects follow the same rule.
`action(Name[, number])` invokes a modeled StandardAction; the action number defaults to 1.
For example, `action(TradeAction, 2)` chooses the Energy trade action. A policy action can name
its concrete provider, such as `ScientistsPolicy`, or the current `UsePolicyAction` provider.
Costs and availability remain model rules. Archives emit the recorded provider's name.
`exMachina { ... }` submits a correction group together.
`selectTask(...)` passes its whole Pets expression to `Agent.selectTask`. It takes no body.
A linked choice is recorded before its separately executed steps, for example:

```text
selectTask(OceanTile<Amazonis_05_10>! THEN -3 MC<Blue>)
OceanTile<Amazonis_05_10>
-3 MC<Blue>
```

The exporter obtains these choices from task edits that introduce a selected THEN continuation.
Action invocations already handled by `PlayCard`/`UseAction` macros do not get duplicate selection
lines. Player autoexecution remains disabled; normal Admin handling still applies.
The importer no longer accepts the superseded research-count, unqualified setup-discard, or
card-only action spellings. Blank lines group turns visually; they have no execution semantics.
When a different player's action begins while the previous player still holds the Baton, the
importer submits `Ok` to decline that player's optional second action. Baton completion itself is
Admin work. The exporter omits that decline only when the next recorded step is another player's
action; a trailing decline or a decline before intervening work remains explicit. Final greenery
uses explicit `Ok`, never player-submitted `PassBaton`.

Research is rendered as a single block per player per generation, containing the draw, discard,
payment, and purchase transfer. Explicit corrections during research stay nested as ExMachina in
that player's block. This intentionally collects each player's research even when the recording
interleaves players' draws and purchases; source comments retain the original observations and
timing. The importer needs no special handling for this grouping. Fixture imports and fresh
programmatic exports verify the grouped research; the transformation is not a general claim that
arbitrary pending tasks commute.

### Request-directed preparation

The importer inherits EACH/group expansion and OR inference from the shared `TaskMatcher`; it
has no retry loop that opens arbitrary pending work. The exporter's passive World supplies the
same custom metric implementations as live TFM (including Neighbor). It snapshots state before
structural choices and asks the matcher whether the next exact request identifies the recorded
root and chosen continuation. It omits that preparation only when the match agrees and no
intervening component changes or Admin work would move its evaluation. That request is kept
separate from later amount aggregation so the proof still describes the text actually emitted.

Group children are exported as the actual performed steps, not hidden behind a whole-group
instruction that would execute unperformed siblings in a partial game. The exporter retains
SelectTask for different continuations with an equal first step, or to distinguish an exposed
task from a compatible task behind EACH. Selection refinement without intervening changes is
rendered as the final choice. Export allows a stop between actions with the usual action offer
still pending. It rejects an unfinished action, payment, or selected task.

`GameArchiveInferenceTest` covers shorter generated EACH/group/linked-choice output, partial
group execution, explicit ambiguous choices, and evaluation timing. `TaskPreparationTest` covers
shared matching, combined-total ambiguity, source qualification, deferral, selected tasks,
restoration, and refusing to bypass newly exposed Admin work. The exporter remains conservative:
it does not collapse a chain of multiple preparation steps, infer a total spread across hidden
children, or remove preparation across state changes.

Other phase grouping may collect each player's tasks that were already eligible at the start of that
block. Newly created tasks and work with a later priority begin a new block. This keeps a later
step behind the other players' earlier work. Amount grouping checks all recorded eligible tasks,
including tasks in subsequent blocks; it cannot emit a partial total because another player acted
between the matching gains. `VIA`, granted-action detection, and shared total matching use current
eligibility, since LATER/FINALLY priorities now persist until selection.

An ambiguous raw task can name its source: `Animal<Pets> VIA Pets` submits
`agent.doTask("Animal<Pets>", cn("Pets"))`. The ordinary matcher restricts candidates to that
context and applies its usual matching and total rules. A wrong source fails; the importer does
not try other sources or perform a separate match. VIA cannot qualify archive macros or blocks;
recipient-prefixed VIA responses are not implemented. The exporter conservatively adds a source
when other pending work could overlap. Combined complete totals deliberately remain unqualified.
A zero-result `BY Admin` request keeps that wrapper and its original request instead of becoming
an unqualified `Ok`.

Typed zero requests such as `-0 Plant<Green>` pass unchanged to the shared matcher. So does
`BY Admin`. Whether those qualifications may be omitted is a shared task-matching decision that
must apply equally to programmatic calls and archives, not a syntax exception in this importer.

### Strict prototype input

Recognized commands reject unused arguments and unexpected bodies, including empty braces on raw
Pets lines. Setup/research/production/solar labels and `exMachina` require nonempty bodies.
Argument lists reject empty members and mismatched delimiters. Config and GlobalEventDeck must
be flat blocks in their defined positions; player names and listed event identities are checked.
`pay` parses each argument separately and requires exact positive integer amounts of supported
resources. It must not silently discard an Ok, zero amount, optional quantity, or extra statement.

The shared runtime accepts a distinct recorded global-event prefix, validates every listed name,
and throws when a draw would exceed it. The importer passes the prefix unchanged; it neither pads
the deck nor invents events. The failed Agent call rolls back, while earlier archive entries remain.

`pay` submits allocations in file order. Just before paying the exact-denomination remainder it
closes unused optional offers; it also closes the zero remainder after alternatives cover the bill.
For example, `pay(4 Titanium, 2 MC)` reduces the bill before paying its MC remainder.
Card-held resources use their ordinary Pets identity, e.g. `pay(2 Microbe<Psychrophiles>, 5 MC)`.
A recipient can pay inside another player's action, e.g. `Blue: pay(3 Heat, 2 MC)`. The payment
releases compatible NOW offers through the sending Agent and performs allocations and declines
through Blue's Agent. Failure restores both players' pending tasks and all spent resources.
A free play with no positive allocation retains an explicit zero payment task.
Card placement is an immediate model consequence once payment and requirement preparation have
finished. Player autoexecution is not required for that movement.
Nested card plays are flat: `playCard(SpaceElevator)`, the explicit Excentric Sponsor discount,
`pay(2 MC)`, and production effects can all appear in the enclosing Prelude body.
`pay(2 MC, 4 Titanium)` fails without spending either resource. No reordering or independence proof
is performed. Executable archive directives delegate to the same [Kotlin routines](ROUTINES.md)
used by the REPL; the importer owns headers, grouping, and source-line errors.

A nested recipient response such as `Pink: 4 MC` uses shared
`Agent.doTask(text, releasedBy = activePlayer)`. The shared matcher can combine Pink's existing tasks
and Green's pending NOW bodies addressed to Pink when their eligible amounts sum exactly to four.
It uses every matching amount, never searches for a subset, and excludes newly triggered rewards.
Releases and responses execute through their respective Players; failure restores the entire call.

An ordinary response matches the recipient's own tasks and eligible NOW bodies together before
anything is released. For example, a request for Pink's pending Plant leaves an unrelated NOW
release for Pink's MC in Green's queue.

Player-only blocks such as `Green { 92 VictoryPoint }` submit ordinary Pets requests. VictoryPoint
is now non-System, so final scoring waits for each Player's explicit total. The shared matcher
executes the individual scoring gains and losses, including zero-valued city scores; it does not
assign a balance. Winner determination waits until this work finishes. The prototype has no
scoring calculation or scoring autoexecution.

### Arithmetic inventory direction

The selected eventual goal is to read all changes to user-facing inventories from the file and
compute their totals without executing game rules. The owner has not selected `Iconic` versus
non-`System` as the exact criterion. Ruling bonuses remain deferred by the owner; explicit player
scoring is now implemented. Vermin queues its deductions directly to the affected Players.

The current model does not satisfy either blanket criterion:

- `Resource`, `Production` and `TerraformRating` are Iconic and non-System. Turmoil ruling
  payouts nevertheless execute through Admin because their unowned Party effects inherit that
  Actor. `TurmoilPhase` immediately applies the annual TR loss; `Party.FormGovernment` immediately
  applies chairman TR. These are real omissions from the arithmetic view.
- All six ruling bonuses should be considered together in the next model step. Ordinary explicit
  player dispatch is the promising mechanism; the exact sequencing must retain Reds' eligibility
  before chairman TR. Changing assignment or introducing a wait must not re-evaluate eligibility
  after the chairman gain. Existing programmatic replays must adapt to the shared rule.
- `PartyLeader` placement immediately demotes the former leader. That changes Iconic, non-System
  delegate inventories even when the original instruction is submitted by a Player. Player task
  assignment alone does not make every consequence explicit. Card construction similarly creates
  printed `Tag` parts without separate tasks.
- `VictoryPoint` is now Iconic and non-System; `ActionUsedMarker` remains both Iconic and System.
  Excluding System omits visible action-usage information; including every Iconic component includes derived tags and markers.
  Neither label currently stands for “all changes the human must explicitly execute.”
- Quantity and destination visibility is a separate requirement: `pay(8 Plant)` and
  `Heat FROM Energy` state concrete removals/gains (the omitted scalar is a single unit).
  `4* Microbe` requires state to determine the actual amount and holder. A finished archive must
  resolve those details if it claims arithmetic readability. `playCard(Name)` already names its
  inferred card movement, but does not enumerate the card's printed tags.

The relevant declarations are in Pets' `base/resources.pets`, `base/card-model.pets`,
`base/scoring.pets`, and `turmoil/classes.pets`. Do not compensate by manufacturing inventory
lines in the importer, adding hidden `exMachina` adjustments, or enabling player autoexecution.
Routing and automatic-effect changes belong in the shared model/engine; task matching belongs in
the shared matcher. Admin's automatic operation remains required for work still assigned to it.

## Regenerating archives

Run `./gradlew :tfm-tests:replayTest :tfm-tests:archivedReplayTest -PreplayExports` to regenerate
fresh exports for the active and archived replay suites. Omit `archivedReplayTest` to run only
the active suite.
A specific replay may be selected with `--tests 'fully.qualified.TestClass'`. Files are under the
`tfm-tests` module's `generated/replay-event-logs` build directory, relocated beneath
`~/.gradle/solarnet-builds/` locally. Successful export runs must also pass live re-import checks.
Ordinary replay runs verify the checked-in fixtures and leave generated exports untouched.
Fresh exports are a starting point for fixture updates, not permission to discard authored comments.

Regenerate after changing rules, replay choices, configuration, corrections, card identities, or
archive behavior. Preserve original source evidence separately; generated archives are not new
independent evidence. Comments are supported in authored archives, but generated exports omit them.
State-dependent requests such as `4* Microbe` still prevent a complete arithmetic inventory;
spelling an actual resolved amount is a shared matcher/model issue, not permission for an import
correction. REPL work and AUTO remain deferred.
