# Transaction records and checkpoints

> **Human review required — October 22, 2026.** This is a preparation plan, not an implemented
> contract. **Selected** identifies the human decisions recorded below. The implementation steps
> and representation are proposed. [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns the transaction,
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

## Proposed history representation

Keep a list of transaction records with the event history, preferably owned by
[EventLog](../../src/common/dev/martianzoo/state/EventLog.kt). Each completed record contains its
serial, a nonempty event range, the initiating Actor and submitted operation/task information,
and a short explanation of why execution happened now. Do not add marker events merely to
separate ranges: that would change the ordinals used by existing causal links and task IDs.

For example, suppose a Player places greenery on a hex whose bonus grants steel:

| Transaction | Events it owns | Why it executed now |
| --- | --- | --- |
| 12 | Tile placement, immediate effects, task edits, and enqueueing the bonus | Client request |
| 13 | Admin executes the bonus and removes its task | Admin eager policy |

The result of the Player's synchronous call includes both transactions. `Checkpoint(13)` means
before the bonus transaction: the tile is present and its bonus is pending. `Checkpoint(14)` is
the current end, before a possible next transaction. This last position exists without fabricating
an empty transaction record. Numbers here are illustrative, not a proposed constructor signature.

Keep execution motivation separate from `Cause`: the placement rule authorizes the bonus, while
Admin's eager policy explains why its task executed now. Drivers supply the motivation at the
engine call; the engine does not infer it from the task's cause. Use a small explicit value for
client submission, Admin eager policy, or an identified Player policy. A current Player policy
counts too; implementing future policy choices is outside this plan. Choose the smallest way to
pass this information after inspecting the actual entry points; do not rename public APIs to
introduce another interaction concept.

[TaskLog](../../src/common/dev/martianzoo/agent/TaskLog.kt) is a compacted replay stream. It omits
some intermediate submissions and cannot reconstruct the complete transaction grouping. Transaction
records explain exact retained execution; they need not duplicate the entire replay command format.

Proposed serial rules: start at zero after engine initialization; preserve the initialization event
prefix and use its end as the first client checkpoint. Serials describe positions within retained
history, not globally unique execution IDs. Truncating before serial 13 removes transaction 13 and
its successors; new work can reuse 13. Checkpoints into discarded future history must be discarded
by clients. Do not add branch identities or generation counters without a demonstrated need.

## Implementation order

### 1. Identify the actual transaction entry points

Inspect [ActorEngine](../../src/common/dev/martianzoo/engine/ActorEngine.kt),
[WorldTransaction](../../src/common/dev/martianzoo/engine/WorldTransaction.kt), and
[AgentImpl](../../src/common/dev/martianzoo/agent/AgentImpl.kt) together. Today's `transact` and
`WorldTransaction` can enclose several Actor mutations plus Admin work; their names do not identify
the new transaction units.

For each public mutation, identify its outermost engine entry, immediate effects, final checks, and
return. Internal helper calls belong to that same transaction. Admin work queued during it waits
until it finishes. An Agent batch calling the engine repeatedly contains multiple transactions;
a compound instruction executed within an engine mutation need not be split into separate records.
Do not invent an at-most-single-task-per-call restriction to simplify recording.

Inspect grouped/scalar execution conveniences specifically: some consume several existing tasks
inside an engine call. Record the behavior and assess it against selected-task exclusion; result
aggregation alone neither justifies nor prohibits it. Any recommendation to remove such behavior
needs its own explanation, rather than silently changing it as part of this migration.

Include queue edits, corrections, handoffs, initialization, and probes in the inventory. Distinguish
engine initialization from ordinary Actor-driven setup. Apply priority checks to competing external
calls, not to the current transaction's own task additions or other consequences.

### 2. Separate event cursors and fix whole-call results

Replace uses of `Checkpoint` that really mean an arbitrary event offset with explicitly named
`Int` event cursors. Important consumers include invariant checks, cleanup scans, internal rollback,
[CardTracking](../../src/common/dev/martianzoo/tfm/engine/CardTracking.kt), and TaskLog anchors.
Keep `GameEvent.ordinal`, `Cause.triggerEvent`, and `TaskId` event-based.

This is not entirely private cleanup: `EventLog.entriesSince`/`changesSince`,
`GameWorld.activitySince`, and `ActorEngine.requireComplete` have cross-module callers. Update their
signatures and callers deliberately; preserve no compatibility layer just for this migration.

Compute returned results over the retained events from call entry through its final Admin handling.
Nested and batch calls each report their own entire execution interval. Preserve existing field
meanings: `changes` includes retained component changes in order, and `tasksSpawned` includes newly
added tasks still pending at return. This does not promise restoration of all completed transactions
if a later transaction fails; the enclosing recovery policy remains separate.

Remove callback-driven history navigation before relying on that simple interval calculation.
The current `TimelineTest` methods `completionCallbackCanSeekAfterANestedPlayerCallReturns` and
`completionCallbackMayLeaveTheWorldAtARecordedPastPosition` exercise real engine calls, but use
synthetic ChoiceA/ChoiceB classes and establish no Canon gameplay need. No production assignment to
`onTransactionComplete` was found in this checkout. Remove the unsupported behavior and those tests;
do not preserve it just because it has tests. Review the callback API itself before adding any new
machinery for it. Internal restoration of failed work remains available.

### 3. Establish complete transactions, then record them

Follow the failure-atomic transaction direction in ENGINE_CONTRACT: finish immediate effects and
final checks before publishing a transaction record. Failed execution must not leave an unowned
suffix of partial events. If providing that guarantee for a public method proves disproportionately
difficult, revisit its contract explicitly, as the human has allowed; do not hide the problem with
misleading records.

Append a record only when a successful transaction retained events. An enclosing client rollback
removes all completed records whose events it removes. Keep events and records together through
rollback, retained future navigation, truncation, copying, fork/overlay reset, and recording import.
Restore history metadata as well as components and tasks on failure. When deferred promotion moves
to the end of the transaction that makes it eligible, include its task edits in that transaction;
do not leave promotion events outside every record.

Add passive recording support for the new ranges and metadata. Passive playback can step between
transactions without running Admin or firing effects again. Before the priority work below lands,
do not expose newly available points with pending Admin work as live gameplay positions. The
simplest rollout is to keep the public navigation migration until step 5, rather than inventing a
temporary second set of safe-position records.

### 4. Make intermediate states safe to resume

Land these related changes together before opening every transaction position to live clients:

- Enforce P0/P1/P2 precedence in the engine. A Player cannot begin ordinary work while eligible
  Admin work remains, including immediately after restoring or forking an intermediate position.
- Make Admin handling mandatory, with FIFO retries and an exception after a full unsuccessful pass.
  Player policies remain separate. Do not mistake another pending Player task for Admin progress.
- Close actual gaps between successive engine mutations. Inspect `AgentImpl.addInitialTasks` and
  nested selection loops, not just the outermost coordinator. Adapt
  [Initializer](../../src/common/dev/martianzoo/engine/Initializer.kt), whose current bootstrap loop
  ignores assignee priority. Preserve valid initialization under the new rule.
- Move deferred promotion to completed transactions as proposed in ENGINE_CONTRACT. Promote the
  whole existing P3 batch together, assigning Admin to P1 and Players to P2. Keep the proposed P4
  rule visibly provisional until reviewed.
- Adapt decision replay and capture together: Admin runs eagerly during replay rather than being
  disabled and replayed as explicit Admin commands. Preserve Admin state assertions. Audit the
  current `TaskLogTest` and `GameFileImporterTest` expectations that disable Admin.

Exactly which Admin narrowing may execute still needs implementation work; see the example below.
The existing resolver is not a substitute for the contract's complete transaction-legality check.

FIFO restoration is a separate unresolved detail. Failed attempts can rotate the queue without
leaving events. For example, after A fails and B succeeds, the remaining order can be C,A rather
than A,C. Choose explicitly whether restoration must preserve that retry order before promising
identical subsequent execution. Randomized tests can reveal order dependencies; passing them does
not settle the promise. Add no persisted retry-order representation until this choice requires it.

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

Before implementation can promise the complete behavior, resolve FIFO restoration, audit the exact
mutation/compound-call entry points, and determine how Admin's complete legal candidate set will be
obtained. These are focused remaining design tasks, not reasons to postpone independent event-cursor
cleanup or passive grouping. Public intermediate live navigation waits for the priority work.
