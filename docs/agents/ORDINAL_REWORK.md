# Transaction records and checkpoints

> **Partly implemented — October 22, 2026.** Steps 1 through 3 have landed and their sections
> describe current behavior; steps 4 and 5 are still proposed. **Selected** identifies the human
> decisions recorded below. [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns the transaction,
> priority, and result rules; this document plans the history changes needed to support them.

## What this change should accomplish

A full event log should show which consecutive events came from the same transaction and why that
transaction executed at that moment. A client checkpoint should identify the state immediately
before a transaction, rather than an arbitrary event offset. Visiting that state can leave Admin
work pending: it is a useful place to inspect consequences, and the P1 priority rule prevents a
Player from acting before Admin finishes.

Keep two different numbers with different jobs:

| Number | Meaning and use |
| --- | --- |
| Event ordinal | Position of an individual change or task event. Keep it for causal links, task IDs, log queries, and internal restoration. |
| Transaction serial | Position of a transaction in retained history. Use it for client checkpoints, stepping, and truncation. |

**Selected:** the low-level restoration routine can continue accepting an `Int` event ordinal.
Client-callable restoration accepts `Checkpoint`, whose meaning becomes the transaction position.
Caching the corresponding event offset inside a checkpoint is fine; it does not permit clients to
construct checkpoints in the middle of a transaction.

## Decisions incorporated into this plan

- **Results cover the entire call.** Every method returning `TaskResult` reports all retained work
  performed during that call, including subsequent Admin handling and any retained callback work.
  A batch needs no different result rule. An Agent convenience may invoke several engine mutations;
  its result aggregates them all. How many transactions occurred is a separate history question.
- **No retained events means no transaction record.** A successful no-op creates no new position
  and advances no transaction serial. Failed or probing work restored before completion likewise
  leaves no record. A task edit still counts even if no component changed. Additions followed by
  removals that remain in the event log also count; zero net component change is not an empty log.
- **Unsupported callback navigation need not survive.** Remove the ability to seek or truncate
  history from a completion callback during a gameplay call, together with tests that preserve that
  behavior. Keep ordinary client navigation after a call returns. See the specific cleanup below.
- **Periodically vary effect order in tests.** Use the existing randomized `::` mode to find
  accidental ordering dependencies. Investigate doing the same for Admin execution order as a
  diagnostic test mode. FIFO remains the required ordinary Admin behavior; randomized testing does
  not silently change that contract or prove that every order gives the same result.

No separate concept named “request” is needed. A synchronous call can perform a transaction, run
eligible work to quiescence, and return a result spanning several transactions.

## History representation

There is no list of transaction records. Each event carries the
[Transaction](../../src/common/dev/martianzoo/state/Transaction.kt) that produced it — its serial,
the initiating Actor, and why it executed now — and every event one transaction owns carries that
same value. [EventLog](../../src/common/dev/martianzoo/state/EventLog.kt) derives a transaction's
nonempty event range from the events, so nothing has to be kept synchronized with them. Step 3
records why. No marker events were added to separate ranges; that would have changed the ordinals
used by existing causal links and task IDs.

For example, suppose a Player places greenery on a hex whose bonus grants steel:

| Transaction | Events it owns | Why it executed now |
| --- | --- | --- |
| 12 | Tile placement, immediate effects, task edits, and enqueueing the bonus | Client request |
| 13 | Admin executes the bonus and removes its task | Admin eager policy |

The result of the Player's synchronous call includes both transactions, which it now does.
`Checkpoint(13)` will mean
before the bonus transaction: the tile is present and its bonus is pending. `Checkpoint(14)` is
the current end, before a possible next transaction. That last position exists without fabricating
an empty transaction record, since `EventLog.transactionStarts` ends with the current event count.
Those checkpoint meanings are step 5; numbers here are illustrative.

Execution motivation stays separate from `Cause`: the placement rule authorizes the bonus, while
Admin's eager policy explains why its task executed now. Drivers supply the motivation at the
engine call, and the engine does not infer it from the task's cause. `Motivation` is one of client
submission, Admin eager policy, or the Actor's own policy, with the Actor supplying identity.
Future policy choices are outside this plan. No public API was renamed to introduce another
interaction concept; `ActorEngine.transaction(motivation)` is the one addition, and it names the
concept this document is about.

[TaskLog](../../src/common/dev/martianzoo/agent/TaskLog.kt) is a compacted replay stream. It omits
some intermediate submissions and cannot reconstruct the complete transaction grouping. Transaction
records explain exact retained execution; they need not duplicate the entire replay command format.

Serial rules, as implemented: serials start at zero after engine initialization, whose event
prefix carries no transaction and whose end is the first client position. Serials are positions
within retained history, not globally unique execution IDs. Truncating before serial 13 removes
transaction 13 and its successors, and new work reuses 13 because the next serial is derived from
the last retained event. Checkpoints into discarded future history must be discarded by clients.
No branch identities or generation counters were added.

## Implementation order

### 1. Transaction entry points (inventory)

This inventory records current behavior in this checkout. It is evidence for the recording work
below, not approval of any entry point's contract.

Three different mechanisms already bracket work today, and none of them identifies a transaction:

- `TimelineImpl.atomic` is a plain rollback scope. Probes, corrections, `tryTask`, and each
  automatic-handling iteration use it, and it nests. A block that restores everything it did is not
  a transaction, so `atomic` cannot define the unit.
- `WorldTransaction.run` (reached as `ActorEngine.transact`) brackets an Agent call. Its outermost
  invocation also runs task handling, completion validation, and the completion callback, so it
  spans several transactions by the proposed definition.
- `AutoExecLoop.actOnce` wraps each individual automatic task in its own `atomic` block. This is
  the one place whose existing granularity already matches a transaction.

The transaction unit must therefore be marked explicitly where a mutation enters the engine. It
cannot be inferred from the rollback scopes.

| Engine entry | Retains events? | Own restoration | Notes |
| --- | --- | --- | --- |
| `sneak` | yes | `worldTransaction.correct` | Runs `::` effects and final checks; no queued `:` effects, task handling, or completion callback |
| `addTasks` | yes | none | Queue edit; can promote deferred work before the enclosing call finishes creating its own work |
| `dropTask` | yes | none | Queue edit; can also promote deferred work |
| `restoreTask` | yes | none | Task replacement without ordinary selection checks |
| `selectTask` (either overload) | yes | none | May resolve, reassign, and execute when concrete |
| `narrowTask` | yes | none | May execute when the narrowing becomes concrete |
| `doTask(taskId)` | yes | none | |
| `doTask(narrowing, …)` | yes | none | `executeSubmittedGroup` also attempts newly added tasks, and `combineScalars` consumes several existing tasks, inside this one call |
| `tryTask` (either overload) | on success only | `atomic` + abort | An unfinished or unavailable attempt returns having retained nothing |
| `trySelectedTask` | yes | none | |
| `canSelectTask`, `canExecuteTask` | never | `atomic`, always rolled back | Probes; they retain no events and so earn no record |
| `requireComplete`, `prepareTaskNarrowing`, `changeLimit` | no | n/a | Read-only; not entry points |

Consequences for recording:

1. One engine mutation is one transaction, including the compound conveniences. `doTask` with
   `executeSubmittedGroup`, and `executeScalarSum`, each remain a single record. Their existing
   multi-task behavior is recorded here, not changed; removing either needs its own justification
   and its own change.
2. A task edit with no component change still retains events, so `addTasks`, `dropTask`, and
   `restoreTask` each produce a record.
3. Probes and failed attempts retain no events and advance no serial.
4. `AgentImpl.addInitialTasks` performs `engine.addTasks` and then one `engine.doTask` per admitted
   task, so one Agent operation contains several engine mutations in sequence. These are the real
   gaps between successive mutations that the priority rule must close; the outermost coordinator
   is not the only place to inspect.
5. Each `AutoExecLoop.actOnce` success is its own transaction, which already matches the proposed
   Admin rule.
6. Engine initialization is distinct. `Initializer.executeSeed` and `executeAdminTask` each use
   `atomic`, while `drainBootstrapTasks` selects tasks in a bare loop with no per-task scope and no
   regard for assignee priority, and `initialize` ends with `markSetupStart` plus `commit`. Its
   events form a prefix that precedes the first client transaction.

### 2. Event cursors and whole-call results (landed)

`Checkpoint` no longer appears where an arbitrary event offset was meant. `EventLog.entriesSince`,
`EventLog.changesSince`, `EventLog.markSetupStart`, `GameWorld.activitySince`,
`GameWorld.markSetupStart`, `Limiter.checkInvariantsSince`, and `ActorEngine.requireComplete`'s
`lastDecision` all take plain `Int` event ordinals, and `EventLog.nextOrdinal` is the public
accessor that supplies one. `GameEvent.ordinal`, `Cause.triggerEvent`, and `TaskId` stay
event-based. No compatibility layer was kept.

Code that still holds a navigation `Checkpoint` and wants that position's events extracts
`.ordinal` explicitly. Those sites are where step 5 must resolve a transaction position to an
event offset instead.

`WorldTransaction.run` now reports the retained events of its own whole interval, captured at call
entry and computed after its finishing steps. An outermost call therefore includes its completion
callback's changes and the task handling that follows the callback; nested and batch calls each
report their own interval. This is reporting coverage only: a failing callback still leaves the
initial work in place.

Unsupported callback navigation is gone. `TimelineImpl` refuses `seek` and `truncate` while the
completion callback runs, including from gameplay calls the callback starts, so the World is always
at the end of its history when the callback returns and the final handling no longer needs an
`atEnd` test. `TimelineTest.completionCallbackCanSeekAfterANestedPlayerCallReturns` and
`completionCallbackMayLeaveTheWorldAtARecordedPastPosition` were removed with it. Ordinary client
navigation after a call returns, and navigation from inside an operation body, both remain.
Internal restoration of failed work is unaffected.

Admin handling through quiescence is not yet part of every call's coverage: `addTasks` and
`dropTask` still route through a path that deliberately skips task handling. That is step 4's work,
not a reporting gap.

### 3. Transaction records (landed as event grouping)

The grouping is recorded **on the events**, not in a parallel list of records.
[Transaction](../../src/common/dev/martianzoo/state/Transaction.kt) holds a serial, the initiating
Actor, and a [Motivation], and every event one transaction owns carries that same value.

This was chosen to keep rollback simple. A transaction's events are contiguous and serials never
decrease along ordinals, so:

1. Removing a suffix of events removes exactly the transactions those events carried. Rollback,
   truncation, fork, overlay reset, and import need no second structure to keep synchronized.
2. The next serial is derived, not counted: one more than whatever the last retained event carries,
   or zero when only the initialization prefix remains. That is why discarding history frees its
   serials for reuse, with nothing to reset.
3. An event names its transaction with no lookup, which is what "a transaction can be found from
   each of its events" requires.
4. A serial's event range comes from `EventLog.transactionRange`, a binary search bounded below by
   the serial itself, since transaction *n* cannot begin before ordinal *n*. Interpolation search or
   a derived serial-to-ordinal index would both be faster, and neither is warranted: step 5 caches
   a checkpoint's event offset, so navigation resolves once and the remaining callers scan anyway.

There is no submitted-task field. The events a transaction owns already record which tasks and
components the submission touched, so storing it again was a second representation of the same
fact — and the one that made a reconstructed history unequal to its source, because the compacted
input stream does not always record which task a form commit was submitted against.

`Motivation` is `CLIENT_REQUEST`, `ADMIN_EAGER_POLICY`, or `PLAYER_POLICY`, with the Actor supplying
identity. Every public `ActorEngine` mutation opens a `CLIENT_REQUEST` transaction;
`ActorEngine.transaction(motivation)` lets a driver state otherwise, and `AutoExecLoop` uses it for
automatic handling. Internal engine work joins the open transaction, so one submitted mutation and
the consequences it needs share one record, including the compound `doTask` conveniences.
[Transactions](../../src/common/dev/martianzoo/engine/Transactions.kt) holds the open transaction
and refuses an event produced outside one, which is how a missed entry point surfaces.

Engine initialization records no transaction: its events form a prefix whose end is the first
client position, and the first gameplay transaction is serial 0. `EventLog.append` enforces the
rest — serials start at zero, never decrease, advance by one, a prefix event cannot follow a
transaction, and an event continuing a transaction carries that transaction's own value.

`EventLogJson` stores a transaction on the first event that carries it and lets the following
events inherit it, so a file is no larger per event; the game-file format is now
`solarnet-game-2`. Reconstruction from recorded inputs reproduces event content and grouping
boundaries but not motivation, because a Player's recorded choice replays as a submission even when
the original game performed it through that Player's own policy. That is the doc's provenance rule,
and `TaskLogTest` compares such histories through a test-support helper that drops the grouping.

Not done here: deriving navigation stops from `EventLog.transactionStarts` and stepping a passive
playback between transactions are step 5; moving deferred promotion into the transaction that makes
it eligible is step 4.

### 4. Make intermediate states safe to resume

**Landed: Admin's full automation as an engine invariant (Selected, October 22, 2026).**
`AutoExecLoop` treats Admin as `EAGER` whatever policy is registered, so no configured or
overridden `AutoExecPolicy` can disable Admin's handling — which is what
[AutoExecPolicy](../../src/common/dev/martianzoo/agent/AutoExecPolicy.kt) already said it must
mean. With it, **Admin's work is always attempted**: a selection probe that rejects an Admin task
no longer keeps it from being tried, so a pending Player task can no longer hide Admin's rejection.
That alone fixed AI Central: `AiCentralTest.Cannot use an absent action while another Player task
remains` is no longer `@Ignore`d, and the `BUG -` characterization that described the old behavior
is gone.

Replay adapted with it. `TaskLog.replay` disables Player policies only, and `TaskLog.inputs()`
leaves Admin's automatic handling out of the recorded stream, because reconstruction performs it
again on its own and a recorded Admin command would not find its task.

**Still open, in the order they unblock each other:**

1. **Admin eligibility: an unmet gate is not a stall.** This blocks both the stalled-pass exception
   and the P0/P1/P2 gate, and nothing else here moves until it is settled. A trial exception that
   threw whenever a call would return with Admin's eligible work undone failed 316 tests; the
   distribution of the Admin tasks it reported is the evidence:

   | Kind | Instances | Example |
   | --- | --- | --- |
   | Gated work a Player unblocks | ~35 | `MAX 0 PaymentScope<Kim>: PlaceGreeneryTile<Kim>!` — Canon, converting plants; the placement waits for the player's payment scope to close |
   | Abstract work needing a choice | 318 | `GlobalEvent! (THEN RevealDistantEvent FROM RevealComingEvent!)` (210, Turmoil) and `CityTile<SoloOpponent, LandArea(…)>! THEN GreeneryTile<…>` (108, solo neutral tiles) |
   | Genuinely impossible | 1 | `UseAction<Kim, AiCentral<Kim>, Action2>!`, the case this contract cites |

   For the first kind, throwing is wrong: returning to the Player is what satisfies the gate. The
   P0/P1/P2 gate has the same defect in reverse — it would block the very Player whose action
   unblocks Admin. The third kind is already handled by the attempt guarantee above.

2. **Accumulation is checked at operation completion, not at quiescence.** `runOperation` and
   `completeOperation` capture the tasks that existed beforehand and call
   `ActorEngine.requireComplete`, which rejects any task created during the operation that is still
   pending, and any surviving `MustCleanUp`. Gates are usually `Barrier`/`Signal`, both
   `MustCleanUp`, so a gate cannot be left behind either. **Recommendation:** before adding a new
   exception, evaluate whether the contract's stalled-Admin rule belongs at that existing boundary
   rather than at quiescence, where it cannot tell a gate from a dead end. `beginOperation`,
   `continueOperation`, and `doTasks` do not call it, which is the real hole.

3. **Reduce the abstract Admin work rather than excusing it.** 210 of the 318 are one case: which
   `GlobalEvent` is revealed. `GlobalEvent` is an `ABSTRACT CLASS` with one concrete subtype per
   event, so this is a choice only because nothing makes it. A `CustomInstruction` that takes the
   next event off the deck would make the instruction concrete and leave Admin nothing to decide;
   the machinery already exists (`ScoreEventVps` in `scoring.pets`). The 108 solo neutral tile
   placements may be reducible the same way by fixing the locations in the game premise — check the
   solo rules first, since `BootstrapLifecycleTest.soloNeutralCitiesCannotBePlacedNextToEachOther`
   reads like a constraint on a real choice.

4. **Make Admin's policy an error, not merely ignored.** Admin's `autoExecPolicy` is still settable
   and silently disregarded, which is a lie in the API. Rejecting a non-`EAGER` policy for Admin
   surfaces the 13 test files that set one on purpose; `MassiveGammaZoneTest`'s toggles are already
   known to be no-ops, since every replay passes with them ignored.

5. **FIFO retries**, **the gaps between successive engine mutations** (`AgentImpl.addInitialTasks`
   and `Initializer`'s bootstrap loop), and **step 5's live navigation** all remain. Step 5's
   passive half does not depend on the gate and could proceed on its own.

**On this document's shape:** steps 1 through 3 are about ordinals and checkpoints and are done.
This step is ENGINE_CONTRACT's scheduling model — priorities, mandatory Admin, FIFO, replay
semantics — which arrived here only because a checkpoint cannot be exposed as a *live* position
unless resuming there is safe. It would sit more honestly in
[ENGINE_CONTRACT.md](ENGINE_CONTRACT.md), leaving this document steps 1 through 3 and 5.

### 5. Switch client checkpoints and remove redundant positions

Have `Checkpoint` identify the pre-transaction position, optionally caching its event offset.
Derive navigation stops from transaction starts plus the current end. Remove `recordedPositions`,
manual `recordPosition` bookkeeping, and recording fields that duplicate that list. Adapt Timeline,
recording playback, TaskLog selection positions, script rollback commands, and other consumers.

A live seek restores the recorded state; it need not immediately execute pending Admin tasks.
The next gameplay progression must handle those tasks before allowing Player work. A passive
viewer may inspect these positions freely. Forks must copy only the selected retained history,
including matching transaction records; inspect today's end-only fork restriction before promising
that a caller can fork a seeked position without first truncating its future.

Update the game-file format and regenerate affected exports. Exact files need transaction records;
short replay files must reflect mandatory Admin execution. If these changes ship separately, each
incompatible format change needs its own version change. Do not infer missing transaction ranges
from old `positions` or silently reinterpret old event ordinals as transaction serials.

## Why current narrowing is not enough

A concrete Player example uses Canon's Protected Habitats. Suppose Stan can remove a plant from
Kim or Pat, both have plants, and only Kim has Protected Habitats. Presence and quantity checks
see two possible targets. Executing removal from Kim encounters the card's `Die` consequence;
removal from Pat can complete. Only the latter is a legal narrowing under the proposed rule.
The rejection occurs in that transaction, even though the protection is written with a single
colon: mandatory `Die` throws when the effect tries to enqueue it.

This illustrates the check missing from the current narrowing deductions; it does **not** establish
that Canon assigns this choice to Admin. Protected Habitats specifically reacts to removal by
another Player, so substituting Admin as the removing Actor would invalidate the example.

The Admin-related Canon evidence is AI Central: its System `UseAction` work for Action2 fails,
whereas Action1 works. The [card test](../../test/common/dev/martianzoo/tfm/tests/cards/AiCentralTest.kt)
exercises that failure. It does not demonstrate a currently queued Admin task choosing between the
action slots. Before claiming that Admin uniqueness is implemented, test candidate enumeration and
whole-transaction rejection together. Count fully specified candidates that actually complete their
transaction, not partial choices or targets that merely pass capacity/dependency filters. Do not
execute additional spawned tasks to decide legality. The full narrowing implementation can proceed
separately from passive transaction grouping, but remains required for the complete Admin contract.

## Verification that matters

Use functional scenarios to verify observable behavior, with small synthetic cases only where
needed to isolate history mechanics:

- Greenery placement and its Admin steel bonus produce separate transaction groups, while the
  enclosing result includes both. Restore between them: the bonus is pending, Player execution is
  rejected, and Admin can complete it. Passive stepping fires no effects again.
- Removing a plant protected by Protected Habitats leaves neither partial changes nor a completed
  record on failure. Restoring an enclosing scope also removes earlier completed records in it.
- A no-op and a restored probe leave the serial unchanged; a retained task-only edit creates a
  record. An ordinary multi-call Agent batch returns all retained changes without merging its
  transactions in history.
- AI Central's nonexistent action cannot be hidden by unrelated pending Player work. Initialization
  and initial task batches still work when early tasks enqueue Admin consequences.
- Export/import, replay, rollback, truncation, and fork preserve matching event ranges and metadata.
  A transaction can be found from each of its events; no client checkpoint exposes a partial change.
  Motivation text is provenance, not a new requirement that live replay choose the same policy label.

Periodically run `SOLARNET_RANDOM_AUTOMATIC_EFFECTS=true ./gradlew test --rerun-tasks`, following
[TESTING.md](TESTING.md), including its known attribution caveat. Today
[Effector](../../src/common/dev/martianzoo/engine/Effector.kt) shuffles independent `::` listeners
only; a component's own effects retain their authored order. It does not randomize Admin scheduling.
The Admin-order diagnostic follow-up is tracked in [TODO.md](../../TODO.md).

## Review status and remaining choices

Two Opus/xhigh review rounds supported the preparation sequence conditionally, not an
implementation-ready design. Neither review verified runtime behavior. This revision incorporates
subsequent human answers about whole-call results, no-op records, callback cleanup, and randomized
testing; it has not had another Opus review.

Human answers recorded since: results cover the whole call, a no-op leaves no record, callback
navigation is removed, randomized effect order is used periodically, FIFO restoration promises no
retry order, and the complete Admin narrowing check is **explicitly out of scope** for this pass —
the existing selection probe stays as its approximation, and the gap stays recorded as a known
difference. The entry-point audit is done and recorded in step 1.

Steps 1 through 3 have landed. Determining Admin's complete legal candidate set remains the open
design task, and public intermediate live navigation still waits for the step 4 priority work.
