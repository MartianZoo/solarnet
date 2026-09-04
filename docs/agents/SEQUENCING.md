# Sequencing and completion

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** deciding how one thing comes before another — task eligibility, `THEN`, automatic
> effects, barriers, precursors, cleanup, or "when is this operation over".
>
> **Skip when:** changing only Actor/assignee identity ([IDENTITY.md](IDENTITY.md)), the count
> executed by one change ([QUANTIFIERS.md](QUANTIFIERS.md)), or phase topology
> ([WORKFLOW.md](WORKFLOW.md)).
>
> **Status:** working rules, one selected design problem, and dispositions. [ENGINE.md](ENGINE.md)
> owns the current task lifecycle; this document does not restate it. A passing characterization
> does not turn a known defect into intended behavior.

## Read only what you need

| Question | Read |
| --- | --- |
| What is the engine actually promising about order? | The promises |
| Which mechanism should express A-before-B? | Choosing a mechanism; Audit procedure |
| Must a modifier act before the thing it modifies exists? | Committed precursors |
| Must a consequence land before the player sees anything? | Automatic effects |
| Is this really about order, or about atomicity? | Do not conflate the timing properties |
| Where does a chore with no choice in it belong? | Put choice-free work at its semantic owner |
| When is an operation over? Where does this cleanup belong? | The missing rule; Cleanup vocabulary |
| Has this idea already been rejected? | Settled |
| Is this a rules question or an engine question? | Research on file |

## Source map

- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — `executeChange` is the
  whole ordering core: automatic effects run inline and recursively, queued effects are deferred.
  `MAX_AUTOMATIC_EFFECT_DEPTH` caps runaway chains.
- [`Effector.kt`](../../src/common/dev/martianzoo/engine/Effector.kt) — `fire` selects the complete
  sibling batch; `stableAutomaticOrder` is diagnostic order only.
- [`AtomicOperationScope.kt`](../../src/common/dev/martianzoo/engine/AtomicOperationScope.kt) —
  `performIdleCleanup`, and `Engine.removeTemporaryComponents` next to it.
- [`Implementations.kt`](../../src/common/dev/martianzoo/engine/Implementations.kt) —
  `enforceSelectLock` and `requireComplete`.
- [`TaskQueues.kt`](../../src/common/dev/martianzoo/engine/TaskQueues.kt) — the class KDoc lists
  every normalization applied to a task on the way in.
- [`SystemDeclarations.kt`](../../src/common/dev/martianzoo/pets/SystemDeclarations.kt) — search for
  `MustCleanUp`, `Temporary`, `Barrier`, `Signal`.
- [Terraforming Mars `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/TerraformingMars/classes.pets)
  — `CLASS PlayCard` and `ABSTRACT CLASS Billing` for the card-play and payment latches.
- [Colonies `classes.pets`](../../src/common/dev/martianzoo/tfm/canon/ColoniesExpansion/classes.pets)
  — `CLASS Trade<ColonyTile>` for the counted-prerequisite latch.
- [`TfmGameplay.kt`](../../src/common/dev/martianzoo/tfm/engine/TfmGameplay.kt) — search for
  `isWildTagOffer` and `removeWildTagUses`; read as evidence, not as a pattern to copy.
- Tests: [`ActionSequencingTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/ActionSequencingTest.kt),
  [`AutomaticEffectOrderTest.kt`](../../test/common/dev/martianzoo/engine/AutomaticEffectOrderTest.kt),
  [`AtomicOperationScopeTest.kt`](../../test/common/dev/martianzoo/engine/AtomicOperationScopeTest.kt).

## The promises

Sequencing exists to keep these true. Everything else in this document is a means to one of them.
The third column is what actually holds the promise today, which is not always a test.

| Promise | Meaning | Held by |
| --- | --- | --- |
| **Causality** | A consequence never precedes the change that caused it. | Structural: effects fire only from a recorded `ChangeEvent`. |
| **Freedom** | Every rules-legal ordering of the pending pool stays executable. | Nothing systematic. Scenario tests only. |
| **Coherence** | No World is exposed to any Actor with an automatic consequence outstanding. | Structural: `Instructor.executeChange` runs `::` inline before returning. |
| **Snapshot** | Every trigger-side condition in one automatic batch is tested against the World as it was before any sibling in that batch ran. | Structural: `Effector.fire` evaluates all `checkForHit` calls first. No test pins it. |
| **Sibling indifference** | No automatic sibling order carries game meaning. | `SOLARNET_RANDOM_AUTOMATIC_EFFECTS`, run manually, with one known payment-attribution exception. |
| **All-or-nothing** | A speculative operation that reaches a dead end leaves no trace. | `Timeline.atomic` and `EventLog.rollBackTo`. Tested. |
| **Sealed tasks** | No authored game behavior edits, reprioritizes, cancels, or removes another task. | Structural: Pets has no instruction that can name a task. |
| **No hidden ordering state** | No ordering guarantee depends on runtime state that rollback does not restore. | `AutomaticEffectOrderTest`. |
| **Scope hygiene** | No `MustCleanUp` component outlives the operation that created it. | `requireComplete`, at the `manual` and `finish` boundaries only. |

Freedom and Snapshot are the two weakest rows, and they are the two that matter most: Freedom is
most of what "correct sequencing" means here, and Snapshot is the rule every future change to
automatic effects will be tempted to break. See Verification we owe.

Freedom is not a rare exception to be traded away for convenience. Many groups of pending changes
may be resolved in any order, and the absence of a card that demonstrates a difference is not a game
rule. Causality is the baseline exception and, once triggered, a consequence joins the pool with no
priority. The engine's contract is to support every rules-valid committed state and no invalid one;
it does not choose a play policy. Policies live in the Agent ([AUTOEXEC.md](AUTOEXEC.md)) and no
policy order may harden into an engine guarantee.

Concretely:

- A card's direct Effects are freely reorderable, and persistent reactions — rebates, tag reactions,
  Mars University, Olympus Conference — may be resolved before, after, or between them once
  triggered, subject only to ordering inside one Effect.
- Separate activations of one Effect remain separate tasks. Whether work inside one activation may
  be split around another activation is still open; see Live agenda.
- Trade income and each colony bonus are separate siblings controlled by the active trader, so Pluto
  may use trade-income cards before taking its own draw/discard bonus.

One live fragility to watch: gaining a `Colony` fires an automatic colony-track adjustment and a
queued placement bonus from the same change, so the bonus always lands after the track has moved.
That order is a consequence of `::` versus `:`, not an authored rule, and it is safe only while no
placement bonus reads the track. If one ever does, make the dependency explicit instead of relying
on the automatic pass running first.

### Do not conflate the timing properties

Five properties collapse easily into a vague "first", and none of them implies another:

- **Sequencing** — B cannot precede A. `THEN`, a latch, or natural unavailability establishes it.
- **Immediacy** — after A, work cannot be postponed behind a player choice. `::` expresses this.
- **Operation completion** — B becomes available only once some operation and its required
  settlement have finished.
- **Game-rule indivisibility** — no player interleaving or rule observation may split the conceptual
  operation.
- **Failure atomicity** — `Timeline.atomic` rolls implementation state back after a failure.

An automatic effect may perform several observable changes. `THEN` may allow unrelated work between
its stages. Rollback decides nothing about how triggers count or observe a physical-game operation.
When a rule is called "atomic", make it say which of these it means; see Research on file for the
three questions to answer separately.

Any engine-owned completion or cleanup must run inside the enclosing atomic transaction before it
commits, so a failure cannot expose partially settled state. Client autoexecution policy is a
separate concern and must never define when engine settlement occurs.

## The missing rule: when an operation is over

This is the one design problem this document owns. Everything unresolved below is a face of it.

Solarnet can say what **is** (components), what **may or must still happen** (tasks), and what
**just happened** (`Signal`). It can even say that something **is under way**: `Trade<ColonyTile>`,
`End`, and a live `EventCard` are all intervals. What it cannot say is when an interval **ends**,
except by the single coarsest rule available — the whole World went idle.

The substitutes in use, and where each fails:

| Substitute | Unit it actually measures | Why it is wrong here |
| --- | --- | --- |
| Whole-World idle (`Temporary`) | Every queue in the game is empty | Waits for unrelated players' work. This is exactly why Trade cannot use it. |
| `THEN` | One task finished | Too fine. Ignores the consequences that task caused. |
| Counted latch (`TradeBarrier`, `Owed`, `Billing`) | Prerequisites this lifecycle enumerated for itself | Correct but hand-rolled. Each new lifecycle re-invents it. |
| Player queue drain | One Actor has nothing left | Combines unrelated work; queue cardinality has no gameplay meaning. |
| Client bridge (`TfmGameplay`) | A string match on instruction text or `cause.context` | Not a rule at all. |

The strongest evidence that the concept is missing is the last row. `TfmGameplay` identifies tasks
by `it.instruction.toString().startsWith(...)` or by `cause?.context?.className == cn("Accepting")` in
roughly a dozen places, and `removeWildTagUses` then reaches in and deletes components. A public
convenience API is reconstructing operation scope by pattern matching because the engine will not
tell it. `UseAction` is the clearest case: it is a `Signal`, an instant, so nothing at all
represents the action that is under way.

### Selected direction: scoped completion

Make the end of an interval a derived fact instead of a hand-built one. A component may declare that
it is **scoped**: it is removed when no live task descends from it.

Descent is already recorded and needs no new state. Every triggered task carries
`Cause(context, triggerEvent)`; every `ChangeEvent` carries its own `Cause`; so a live task's
ancestry is a backward walk through event ordinals, ending either at the scoped component's own gain
event or at a null cause. Because it is derived from the event log, rollback restores it for free —
which the No-hidden-ordering-state promise requires and a cached field would not.

**One precondition, unsettled.** "The scoped component's own gain event" is not yet well defined.
The component graph is a multiset with no instance identity, and equal Types are indistinguishable
([ENGINE.md](ENGINE.md#component-graph)). Either every scoped concrete Type must carry a
maximum-one invariant, or scoping needs an explicit operation identity. Requiring maximum-one costs
nothing today — `End` is bounded by `MAX 1 Phase`, `CardFront` by `MAX 1 This<Player>`, and
`Trade<This>` by its `ColonyTile` invariant — but it has to become a stated requirement rather than
a coincidence, and even then it does not cover a Type gained, removed, and gained again inside one
operation. `TradeBarrier` deliberately carries no such bound because it is a count, not an identity;
that distinction is worth preserving. Settle this before calling the mechanism generic or migrating
`Temporary` and `TradeBarrier`.

What this is expected to absorb rather than add to:

- whole-World idle becomes the special case where the scope is the game, so `End` is unaffected;
- `EventCard` gets *more* precise, not less: today it survives until unrelated players' work drains;
- `TradeBarrier` is a hand-maintained count of the same fact;
- the `TfmGameplay` wild-tag and `Accepting`/`AcceptingFromCard` bridges become deletable;
- Head Start stops needing nested completion frames — the first action's scope completes, then a
  second ordinary action turn is granted.

**Answer this before writing code:** does a scope wait for work it caused in another Actor's queue?
An attack creates the victim's choice. If yes, one player's action can block on another's decision.
If no, "the operation is over" is false while its consequences are outstanding. This is a game
question with an engine consequence; settle it first, from the game.

**Stopping points.** Implement the descent query and exactly one scoped class — the interval that
`UseAction` currently lacks — and delete exactly one `TfmGameplay` bridge. Only then consider
migrating `Temporary`. If the first slice needs Pets vocabulary beyond one class marker and its
removal effect, or if the descent walk turns out to need a cache on `Task`, stop and report the
design pressure here instead of proceeding.

Until that exists, keep building narrow latches — a narrow latch is the cheapest honest mechanism
while no completion event exists — but record each new one here. A fourth latch with this shape is
the signal to stop hand-building them.

## Choosing a mechanism

Only impose sequencing when the game demands it or an otherwise valid operation cannot be modeled
without it. Take the lowest rung that fits; every rung above 0 is permanent conceptual cost.

| Rung | Mechanism | Use when |
| --- | --- | --- |
| 0 | Nothing — natural unavailability | The component model already makes B impossible before A. Production tied to a tile cannot precede the tile. |
| 0 | Nothing — recoverable dead end | The only consequence of a bad order is a rollback. Let the client prune; see below. |
| 1 | Trigger `A: B` | Every A should cause B, including future sources of A. Add `IF` when state distinguishes which A qualifies. |
| 2 | Automatic `A:: B` | A-without-B is not a coherent World to expose, and B is choice-free. |
| 3 | `A THEN B` | Only particular authored A operations need B, and that instruction owns both stages — usually because A holds the choice B is derived from. |
| 4 | Committed precursor | An effect must modify the operation before the component that announces it exists. |
| 5 | Completion latch | Distributed prerequisites must all finish. Prefer a specific gate to a broad `MAX 0 Barrier`. |

Rung 0's second form is a real design position, not laziness. Supporting only valid game states does
not require proving in advance that every locally valid choice has a legal completion. Protected
Habitats is the model: an attack may narrow to a protected resource, the card reacts with `Die`, the
enclosing operation rolls back. The card states one rule and no attack needs to pre-exclude
protected targets. During audit, actively look for restrictions and ordering machinery whose only
purpose is to pre-prune such branches, and prefer rule-and-rollback whenever it still makes every
legal completion reachable and lets no illegal result commit.

Two notes on rung 3. `THEN` waits for the A *task*, not A's transitive consequences, and B receives
no priority over unrelated work — `A1, A2, B1, B2` is a legal order for two chains. `THEN` also
opens one implicit Type-variable scope, which is often the real reason to use it: Mining Rights and
Capital carry a chosen area or tile forward. That is a shared-variable constraint, not evidence that
the later work deserves precedence. When auditing one, check both that A genuinely owns the choice
and that the artificial order buys a readable variable relationship rather than hiding an unordered
model.

On rung 5, a latch controls legality, not priority; other legal work stays reorderable. Keep a
component gate when it records gameplay information — `Owed` carries an amount and denomination that
other rules inspect and change, and `Required` carries a parameter shortfall. The suspicious case is
narrower than it looks: a component whose entire payload is "some task must wait."

### Audit procedure

For a new A-before-B claim:

1. Name the illegal committed result that would occur without it. If the only consequence is a
   recoverable dead end, keep the freedom.
2. Record authoritative wording and the smallest observable counterexample.
3. Walk the ladder from rung 0 and take the first rung that fits.
4. If you reach rung 5, first ask whether the real requirement is "the operation is over" — if so it
   belongs to the section above, not to a new latch.
5. Add a precedence test, and where freedom matters a freedom test showing representative legal
   sibling orders remain executable. A freedom test need not make the orders converge; the choice
   may itself be gameplay.
6. If no committed mechanism fits, leave the case open and say so. Do not invent a completion rule
   to close one card.

## Committed precursors

Some rules must modify an operation before the component that announces its result exists. A
discount cannot wait for the card's real Tag. The canon answer is a **committed precursor**: an
earlier component P that the operation is already committed to converting into A.

`PlayTag<Class<Tag>>` is the worked example. Card play creates one `PlayTag` per printed tag before
payment settles; discounts and alternative payment effects subscribe there; successful card entry
then creates the real Tags with their printed multiplicity. If the play reaches a dead end, rollback
removes the precursor and everything it caused.

For precursor P and result A:

- create P only after the Player has selected the operation that entails A;
- make every producer of P force A before the operation can commit;
- preserve the owner, Type, and multiplicity needed to relate P to A;
- put on P only the effects that must act before A exists — ordinary reactions still subscribe to A;
- establish P-before-A with the ladder above. Being a `Signal` provides no sequencing by itself.

The P/A distinction is permanent conceptual cost, justified only when A is genuinely too late or
cannot say how it was obtained. Never let P degrade into a notification a caller may emit without
performing A, and never duplicate A's reactions onto both.

Two live families are weaker than this and should not be described as more: `UseActionN<HasActions>`
is generic action dispatch, not a promise of a later component Type; and `BuyCard` carries
multiplicity but not selected card identity, which is exact for the two existing price modifiers and
should be specialized rather than extended if a third needs more. `Pay` is not a precursor at all —
it is created in the same `FROM` instruction that removes the resource. `Accepting` is not one either;
it exposes an optional choice. Card-play and payment lifecycles belong to
[ACTIONS.md](ACTIONS.md) and [PAYMENTS.md](PAYMENTS.md); do not re-inventory them here.

## Automatic effects

For one concrete change the engine recursively executes all matching `::` effects before admitting
any queued `:` effect. An automatic reaction never enters a queue and is never selectable.

Write `A:: B` only when all three hold:

- B is a fully determined, choice-free consequence of A;
- exposing A-without-B would misrepresent the state or leave structural bookkeeping half-built; and
- the invariant must be restored before any queued player work becomes available.

The diagnostic: could the engine stop after A, show that World to the Player, and let them choose?
If yes, use `:`. `::` is not for speed, for preferred task order, for a Player choice, or as a
substitute for completion. It does not make execution unobservable — automatic effects produce real
changes and trigger further effects; it prevents queued player work from interleaving.

Settled uses: generated card tags before printed effects; hidden adjacency before area bonuses;
energy-to-heat before production payouts; fixed cost and payment bookkeeping before gated choices;
invisible markers users must never execute by hand; completion flags derived from committed changes;
and helper Signals whose only job is to fan out later work. Effects triggered by Admin workflow
events use `:` by default — the workflow already controls when they become available.

Two canon effects stay queued for implementation reasons, not because they are decisions:
action-cost adjustments wait for the base action's `Owed`, and the solo production correction waits
for production payouts. Do not make them automatic until the dependency is expressed directly.

If one automatic effect must always follow another, make the first event trigger the second. Do not
rely on registration order, and do not add a retry loop (see Settled).

### Choose condition time explicitly

```pets
A IF R: B
A: (R: B) OR Ok
```

The first tests R when A's Change Event fires: if R is false no task is created, and if true, later
changes to R do not cancel B. The second always creates a task, tests R against the later World, and
makes B optional because `Ok` remains a legal arm. Prefer trigger-side `IF` when R cannot change in
the interval or when R qualifies the original event — global-parameter threshold bonuses and trade
income measured before the track resets both deliberately freeze trigger-time state. Use the gated
form only when later sibling work is meant to decide availability and declining is legal; Pharmacy
Union is the model.

## Put choice-free work at its semantic owner

A queued Player task should exist because the Player can make a real choice: whether to act, which
alternative to take, how to narrow it, or when to perform one of several reorderable effects. Do not
expose a Player task merely because the implementation wants a pulse or a cleanup step. This is the
rule that stops new client bridges from appearing.

Place choice-free work according to what it means:

1. `::` for a fixed consequence needed to restore a coherent World before Player work;
2. an Admin-assigned queued task for neutral game or workflow activity that stays meaningfully
   observable; or
3. the exact lifecycle's completion event for cleanup that must wait. If no such event exists, leave
   the mechanism open rather than attaching cleanup to the Player's whole queue.

Admin assignment says the neutral table Actor selects or narrows the work; it does not assert that
every outcome is equivalent. A default Admin policy may act aggressively while another legal policy
chooses differently, and that strategy is not an engine concern. A helper `Signal` may likewise stay
useful causal vocabulary without becoming a Player command. Timing alone does not decide the
representation: a fixed rule consequence belongs to engine execution even when it happens to
complete before the next Actor mutation.

Action-local temporary state follows the same rule. Its settlement must complete with the action,
before workflow offers a second action. Declining that later offer is a separate turn decision and
must not double as current-action cleanup. `WildTagUse?` is the one documented exception, and it is
a bridge to delete rather than a pattern to copy.

## Cleanup vocabulary

There is one invariant here and three policies for satisfying it, and the declarations should say so
directly. Today `Temporary` is not a `MustCleanUp`, so the sweep that satisfies the invariant is not
covered by the check that enforces it.

| Concept | Meaning |
| --- | --- |
| `MustCleanUp` | The invariant: this must not outlive its operation. |
| `Signal` | Policy 1 — removes itself immediately (`This:: -This!`). |
| `Barrier` | Policy 2 — removed by the game rule that owns it. |
| `Temporary` | Policy 3 — the engine removes it when the World is idle. |

Prefer making `Temporary` a `MustCleanUp` so one check covers all three, and read the `Temporary`
sweep as one policy for satisfying the invariant rather than as a second, opposite mechanism.

`MustCleanUp` components represent mandatory unfinished state, and each needs an honest completion
event: debt reaching zero, the end of an action, or another rule-specific fact. A generic Player
queue drain must not consume unrelated temporary state merely because it is pending for the same
Player.

### Current behavior: whole-World idle cleanup

`Temporary` is a narrow component-lifetime contract. Whenever every task queue is empty, the
outermost atomic scope removes every live instance of that class before notifying workflow that the
operation completed. Removal effects may create more components or tasks, so the engine runs
ordinary automatic work again and repeats cleanup until an idle pass finds nothing left to remove.
Only that empty pass allows the workflow callback. Work the callback starts synchronously is
coalesced into one automatic follow-up step, and the same cleanup loop runs again before the
resulting position is recorded. Every pass happens inside an atomic transaction. See
`AtomicOperationScope.performIdleCleanup` and `Engine.removeTemporaryComponents`.

Two classes use it:

- **`EventCard`** — its immediate work and tag reactions finish while the live card still exists,
  and removing it creates the corresponding `PlayedEvent`. Law Suit is a deliberate exception in
  behavior, not in machinery: its authored consequence moves the card straight to `PlayedEvent`, so
  no EventCard is left for idle cleanup.
- **`End`** — the live scoring operation, and also the terminal `Phase`. Gaining it queues every
  `End` scoring reaction; Awards queue `MeasureAward<Award>`, which establishes each Player's
  `AwardTally` automatically and queues `AssignAwardPlaces<Award>`. Once those tasks and all their
  consequences drain, removing `End` leaves no live phase and queues multiplayer victory assignment.

The reusable shape is a concrete operation component whose gain creates all the work that must
precede completion, and whose automatic removal effect emits the fixed completion consequence:

```pets
CLASS Operation : Temporary {
  -This:: Completion
}
```

Use it only when whole-World idleness really is the completion fact. It deliberately waits for all
queued work, not merely the work `Operation` caused: exact for endgame scoring, too broad for a
nested action or one cleanup item sitting among unrelated Player work. That breadth is the
limitation scoped completion is meant to remove.

## Verification we owe

Both proposals target the weak rows in The promises. Neither needs new engine concepts.

- **Automatic sibling permutation, in the suite.** `SOLARNET_RANDOM_AUTOMATIC_EFFECTS` shuffles each
  batch but only when someone remembers to set it. Turn it into a seeded test that runs one
  operation under several permutations and compares normalized component state and queued-task
  multisets. Exact event order need not match. The known saturating-`Owed` attribution variance is a
  payment-evidence defect ([PAYMENTS.md](PAYMENTS.md)), not a reason to order payment effects.
- **Player task-order permutation.** Nothing systematically tests Freedom, which is the larger
  claim. Recorded games identify tasks by instruction match rather than position, so a replay can
  choose legally among pending tasks in a different order and compare committed state at the next
  stable point. Start with one recorded game and one seed.

One honest divergence to fix while nearby: presentation order is documented as non-semantic but is
load-bearing in the API. `doTask(narrowing, taskNumber)` takes a 1-based position, `autoExecNext`
falls back to `eligible.first()`, and `TfmGameplay` computes a positional `selectionTaskNumber`.
Match on instruction or cause instead of position wherever a caller has that option.

## Live agenda

Ordered. Stop and report rather than growing any of these into cross-module vocabulary.

1. Settle both blocking questions under Selected direction — cross-Actor scope and scoped-component
   identity — then land the first scoped-completion slice and delete one `TfmGameplay` bridge.
2. Add the two permutation tests above.
3. Make `Temporary` a `MustCleanUp` and collapse the two idle sweeps into one invariant with one
   check.
4. **Head Start** — its sibling tasks currently let its two actions interleave. Prefer using the
   current Prelude turn for the first action and granting a second ordinary action turn after normal
   settlement. If authoritative evidence demands one indivisible operation, record the simpler timing
   as a deliberate house rule rather than disguising it as exact fidelity.
5. **Mars University** — incremental `THEN` permits two discards before either draw when two
   activations trigger. Needs authoritative evidence on whether each discard/draw exchange is
   indivisible, and therefore whether work inside one activation may be split around another.
6. **Candidate draw/select/play** — Valley Trust, Merger, and New Partner use hand cards in
   incremental chains, so candidates are neither isolated nor forced to continue. If a fix is
   selected, prefer one operation-scoped candidate representation.

Required actions are deliberately outside this audit: the selected model removes the component
offering ordinary standard actions while a `RequiredAction` exists, making them naturally
unavailable.

These encodings are considered principled and need no re-litigation: global-parameter change before
TR and threshold reactions; tile placement before adjacency and bonuses, with the reactions as
siblings; `UseCardAction` placing the `ActionUsedMarker` before `UseAction`; trade income and
individual colony bonuses as reorderable siblings; and card-resource `THEN` chains carrying X into an
`Owed` reduction.

## Settled

Decisions already made. Overturn one by showing its reasoning wrong — new evidence, a changed
constraint, a real case — not by rediscovering the cost.

- **Generic `PRE A:: B` — rejected.** Firing after A is resolved but before it is applied makes the
  resolved A stale: B could consume A's removal target, fill its gain limit, or remove a dependency.
  Executing A anyway violates the component model; resolving again lets B react to an A that then
  changes. It would also subscribe to an intention, with no `ChangeEvent` for a Cause to name.
  A committed precursor keeps all of this inside the existing model.
- **Retrying automatic sibling batches — dropped.** The proposal was to retry temporarily unrunnable
  siblings in nested scopes until a pass makes no progress. It contradicts the Snapshot promise it
  was meant to serve: a sibling retried later executes against a World that other siblings have
  already mutated, though its trigger condition was evaluated before any of them ran. It also lets
  authors leave real causal dependencies undeclared. Keep the single pass; when one automatic effect
  must follow another, make the first trigger the second.
- **Player queue drain as the generic completion mechanism — rejected.** It combines unrelated work
  and delays local completion arbitrarily. Queue cardinality has no gameplay meaning.
- **Ordering by presentation — rejected.** Task numbers are ephemeral labels. If presentation ever
  follows authored Class, hierarchy, and Effect order, encode that as immutable provenance assigned
  at creation; never derive gameplay precedence from it, and never give effects a way to reach into
  the pool.
- **Stabilizing payment attribution by ordering effects — rejected.** Applicable `ResourceValue`
  components remove the same saturating `Owed`, so order decides who is credited with the last
  units. Reconstructed games still reach the same paid state. The repair is the payment direction in
  [PAYMENTS.md](PAYMENTS.md), not sibling precedence.
- **The `Die` produce/consume pipeline — at peace.** `Transformers.invalidChangesToDie` emits the
  marker and `Task.normalizeForTask` eliminates it: a bottom value plus its normalization, not a
  duplicated fact. `PremiseViability` runs the same reasoning statically and earns its place by
  failing a bad premise at setup. Only the three-valued interpreter it copies from `ClassLoader` is
  genuine duplication, and that is in [TODO.md](../../TODO.md).

## Research on file

**Unconfirmed hypotheses, not game rules.** The unanswered
[atomicity thread](https://boardgamegeek.com/thread/2626062/which-instructions-are-atomic) proposed
that these may be indivisible: paying for and putting a card into play; direct transfers and
exchanges; one stated amount of resource or production change; one multi-card draw or discard; and
fleet movement with trade income and track reset. It proposed that these stay observably decomposed:
multi-step global-parameter changes, so threshold bonuses cannot be skipped; the ocean and M€ loss
printed on one Hellas space; and separate Philares reactions to separate adjacencies.

When evaluating any "atomic" claim, answer three questions separately — they do not imply each
other: whether player work may interleave; whether intermediate changes fire or observe effects; and
what multiplicity a trigger counts as one effect. Designer discussion distinguishes component-level
steps from Effect identity: gaining 5 M€ may be several changes while an "if one or more" trigger
still sees one Effect, and a player cannot repartition a 7 M€ loss to multiply Mons Insurance.

Do not initiate rule research during routine implementation work. When the user asks for it, find
the linked Jacob Fryxelius ruling or preserve the uncertainty; do not infer an answer from an
unanswered community post.

## Elsewhere

Phase and turn precedence: [WORKFLOW.md](WORKFLOW.md). Current task lifecycle, selection, and
execution: [ENGINE.md](ENGINE.md). Agent policies and the autoexecution loop:
[AUTOEXEC.md](AUTOEXEC.md). Delegated narrowing and controllers: [IDENTITY.md](IDENTITY.md). Action
costs and invoices: [ACTIONS.md](ACTIONS.md). Payment evidence: [PAYMENTS.md](PAYMENTS.md).
