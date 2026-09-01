# Sequencing and completion

> **Read when:** changing task eligibility/order, `THEN`, automatic effects, barriers, precursors,
> completion, recoverable dead ends, or phase precedence.
>
> **Skip when:** changing only Actor/assignee identity ([IDENTITY.md](IDENTITY.md)) or the count
> executed by one change ([QUANTIFIERS.md](QUANTIFIERS.md)).
>
> **Status:** working rules, explicit hypotheses, and audit. A passing characterization does not
> turn a known defect into intended behavior.

## Read only the relevant rule family

| Question | Read |
| --- | --- |
| Is pending work ordered at all? | Mental model; Ask which kind of ordering; Rules that deliberately impose no order |
| Which mechanism should express A-before-B? | Put facts in components; Recoverable dead ends; Choose the weakest mechanism that fits |
| Must a modifier precede the event it changes? | Model before-trigger effects with a committed precursor |
| Must reactions complete before the user sees the next choice? | Use automatic effects to preserve player-visible invariants |
| Does this concern `EACH`, continuations, controlled completion, or atomicity? | Read only the matching proposed section |
| Is this a known family, defect, or phase rule? | Settled families through Workflow precedence |
| How should a new ordering claim be researched? | Audit method |

## Source map

- [`TaskQueue.kt`](../../src/common/dev/martianzoo/engine/TaskQueue.kt) and
  [`TaskQueues.kt`](../../src/common/dev/martianzoo/engine/TaskQueues.kt) — inspect
  task pooling, narrowing, and the select-lock.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — inspect
  splitting, `THEN`, barriers, and resolved forms.
- [`Effector.kt`](../../src/common/dev/martianzoo/engine/Effector.kt) — search for
  `automatic` when changing immediate reaction ordering.
- [`AtomicOperationScope.kt`](../../src/common/dev/martianzoo/engine/AtomicOperationScope.kt)
  and [`Timeline.kt`](../../src/common/dev/martianzoo/engine/Timeline.kt) — read only
  for commit/rollback atomicity.
- [`ActionSequencingTest.kt`](../../test/common/dev/martianzoo/tfm/tests/rules/ActionSequencingTest.kt)
  — read when changing player-visible action ordering.

## Mental model: preserve the whole valid decision tree

Pending tasks are an unordered pool. Stable task ids and auto-exec iteration are implementation
conveniences, never game precedence. A player may normally interleave a card's direct effects,
persistent reactions, and already-triggered consequences.

This freedom is a major part of engine correctness, not a rare exception. Many groups of changes
may be resolved in any order. Preserve all of those orders even when no current card, component, or
test demonstrates that the choice matters. The absence of a known observable difference is not a
game rule and does not justify turning the pool into a sequence.

Causality is the baseline exception: a consequence cannot precede the event that triggered it.
Placing an ocean precedes Arctic Algae; raising production precedes Manutech. Once triggered, the
consequence joins the pool without priority.

The engine's contract is to support every rules-valid committed state and no rules-invalid one; it
does not choose a play policy. An Agent Driver may deliberately trade away flexibility for
convenience by selecting work in an order the user will probably accept, or it may play safe and
ask. This includes an Admin Driver intelligently ordering Admin's ordinary tasks. The current
auto-execution implementation is engine-side, but that policy belongs in the Agent and is intended
to move there. Its order must not become an engine guarantee or an authored precedence rule; the
generic pulse dispatcher only coordinates wake-ups.

Tests should prove only real precedence. When freedom matters, also prove that representative legal
sibling orders remain executable. Such a freedom test does not require the orders to produce the
same state; the choice of order may itself be legitimate gameplay.

## Ask which kind of ordering the rule needs

Several different concerns are easy to collapse into a vague request for work to happen “first”:

- **Sequencing:** B cannot precede A. `THEN`, a barrier, or natural unavailability can establish it.
- **Immediacy:** after A, work cannot be postponed behind a player choice. Automatic `::` expresses
  this.
- **Controlled completion:** B becomes available only after the currently controlled turn or
  delegated choice has completed, including its required settlement. The runtime mechanism remains
  open; a whole Player queue drain is not automatically the correct unit.
- **Global completion:** the whole World task pool has drained and remains empty after completion
  work.
- **Delegated completion:** a selected task may move to another Actor for narrowing while retaining
  its controller. The global select-lock prevents unrelated work until it completes; follow-up work
  returns to the controller. See [IDENTITY.md](IDENTITY.md).
- **Game-rule atomicity:** no player interleaving or rule observation may split the conceptual
  operation.
- **Failure atomicity:** `Timeline.atomic` rolls implementation state back after failure.

None implies the next. An automatic effect may execute several observable component changes.
`THEN` may allow unrelated work between its stages. Timeline rollback does not decide how triggers
count or observe a physical-game operation.

When evaluating an “atomic” rule, specify separately:

1. whether player work may interleave;
2. whether intermediate component changes fire or observe effects; and
3. what multiplicity a trigger sees as one effect.

Do not initiate rule research during routine implementation work. When the user explicitly asks
for it, do not infer these answers from an unanswered community post: find the linked Jacob
Fryxelius ruling or preserve the uncertainty.

## Put facts in components and future work in tasks

The component graph says what **is** or what **is happening**. This includes transient facts inside
an operation when their identity, cardinality, or individual changes matter. `Owed` records the
current debt and lets the event history attribute each reduction to Earth Catapult, Advanced
Alloys, a payment, or another cause. `Required` similarly records a quantitative global-parameter
shortfall. `CyberiaSystemsFirstChoice` carries one selected card identity into a later choice, and
`AwardTally` carries measured values into award comparison. Their temporary or process-local nature
does not make them task metadata.

Tasks say what activities remain available or mandatory. Keep them sealed after creation: authored
game behavior must not edit, reprioritize, cancel, or remove another task. Player narrowing remains
the deliberate exception; engine resolution and execution retain their existing mechanical roles.
A future priority is immutable task metadata assigned when work is created, not a new capability for
effects to reach into the task pool.

Signals such as `PlayCard`, `Trade`, `Accept`, and `Pay` are coherent component events: they state
what is happening and disappear before a stable World is exposed. Durable facts such as `Phase`,
`Pass`, `ActionUsedMarker`, and next-card effects likewise remain component state.

`MustCleanUp` components represent mandatory unfinished state. Each needs an honest completion event:
debt reaching zero, the end of an action turn, or another rule-specific fact. A generic Player queue
drain must not consume unrelated temporary state merely because it happens to be pending for the
same Player. The workflow may proceed only after controlled work and its required settlement leave
neither tasks nor unfinished temporary state.

The suspicious case is therefore narrower: a component whose entire payload is “some task must
wait.” `TradeBarrier` is the strongest current example. By contrast, `Owed` records gameplay
information—an amount and denomination—that other rules can inspect and change. Do not keep a
component merely as a stop sign when an existing completion event can express the same rule, but do
not replace a narrow barrier until the simpler mechanism is demonstrated.

## Recoverable dead ends are part of the model

Supporting only valid game states does not require the engine to prove in advance that every locally
valid choice has a legal completion. An encompassing operation is speculative until it commits. A
client may choose a branch whose later mandatory work cannot finish; the engine should report the
dead end and roll the whole operation back rather than commit an invalid result. See the current
transaction mechanics in [ENGINE.md](ENGINE.md#recoverable-dead-ends).

Pruning that decision tree is a client concern. A client may simulate branches, recognize common
traps, choose a likely-good route, or ask the user. Pets and engine code should not duplicate target
exclusions, add premature requirements, or impose sequencing simply to spare the client from a
recoverable mistake. The engine may reject a branch as soon as the rules make its failure
unavoidable; it need not predict that failure through additional game concepts.

Protected Habitats is the model example. An attack may narrow its broad resource choice to a
protected Plant, Animal, or Microbe. Protected Habitats reacts to that hostile removal with `Die`,
so the enclosing atomic attack reaches a dead end and rolls back. The card can state one direct rule
about hostile removals; every attack and target selector need not know how to pre-exclude protected
resources.

During the Pets audit, actively look for restrictions and ordering machinery whose only purpose is
to pre-prune such branches. Prefer the simpler rule-and-rollback model when it still makes every
legal completion reachable, prevents every illegal result from committing, and rolls back the
entire speculative operation.

## Choose the weakest mechanism that fits

Only impose sequencing when the game rules demand it or an otherwise valid operation cannot be
modeled correctly without it.

### 1. Prefer natural unavailability when exact

Do not author sequencing when the component model already makes B impossible before A. A production
increase tied to a newly placed unique tile cannot occur before the tile exists. This keeps order
conditional on the actual World rather than encoding a universal priority.

### 2. Prefer a trigger for a systemic consequence

If every A should cause B, including future sources of A, encode the rule once as `A: B` on A or on
the component that owns the ambient rule. Use `IF` when state distinguishes which A events qualify.

Examples:

- `GlobalParameter` owns its TR reaction because every qualifying parameter step uses it.
- `GreeneryTile` owns its oxygen reaction, conditioned on `Photosynthesis`; the corporation phase
  creates that ambient state and the final-greenery phase removes it.

Use queued `:` by default. [Automatic `::`](#use-automatic-effects-to-preserve-player-visible-invariants)
is the stronger form for restoring an invariant before player work appears.

### 3. Use source-local `A THEN B` when that source owns both stages

`THEN` is appropriate when only particular authored A operations require B, no suitable trigger or
state gate distinguishes them, and that instruction conceptually owns the pair. A direct Pets
Action cost followed by its payoff, or a placement followed by a marker identifying that selected
place, are normal examples. Standard-resource Actions instead use the removal of their finalized
invoice as the completion event; see [ACTIONS.md](ACTIONS.md).

Completing A enqueues B in A's place. B is not immediate and receives no priority over unrelated
tasks. `A1, A2, B1, B2` may be a legal order for two `A THEN B` chains. `THEN` waits for the A
task, not every transitive consequence it causes.

`THEN` also creates one implicit Type-variable scope. Mining Rights and Capital use that variable
to carry an area or tile choice into later work; temporal order is not its only purpose.

This can force sequencing that the game rules do not independently require. It is often still the
most natural expression: one stage owns the Player's choice and the other is derived from that same
choice, so put the choice-bearing stage first and its derived continuation second. Treat this as a
shared-variable constraint, not evidence that unrelated work needs priority. During audit, verify both that
the choice really belongs to the first stage and that the artificial order is buying a useful,
readable Type-variable relationship rather than concealing a more natural unordered model.

If every producer of A is expected to remember B, the relationship belongs in a trigger instead.

### 4. Use a narrow barrier for distributed completion

A barrier makes later work illegal until separately produced work finishes: card payment before
entry, all optional trade-track choices before fleet movement, or one delegated operation before the
next. Prefer a specific gate such as `MAX 0 TradeBarrier` to `MAX 0 Barrier`.

A barrier controls legality, not priority. Other currently legal work remains reorderable. Retain a
component gate when it records gameplay information, as `Owed` and `Required` do. When its only
meaning is that later work must wait, compare it with a direct completion event; a narrow barrier
may still be the cheapest honest mechanism while no such event exists.

Phase topology and Player control-until-drain belong to [WORKFLOW.md](WORKFLOW.md), not generic
barriers. Whole-World drain is a workflow completion case, not a generic replacement for local
completion events.

## Model before-trigger effects with a committed precursor

Some rules must modify an operation before the component that normally announces its result exists.
A discount cannot wait for the card's real Tag, and Trade Envoys cannot wait for the fleet to have
already flown. Canon currently handles these cases by exposing an earlier Signal that effects can
subscribe to. Creating that precursor is not a prediction that the later component might appear: a
successful encompassing operation is committed to creating it. If the later mandatory work reaches
a dead end, rollback removes the precursor and all of its consequences too.

Call this a **committed precursor**, or informally a pre-trigger. For precursor P and result A:

- create P only after the Player has selected the operation that entails A;
- make every producer of P force the corresponding A before the operation can commit;
- preserve the owner, selected Type, and multiplicity needed to relate P to A;
- put on P only effects that must distinguish or modify the operation before A exists; ordinary
  reactions should still subscribe to A; and
- establish P-before-A with the mechanisms in this document. Being a Signal does not
  itself provide sequencing or completion semantics.

The distinction between P and A is permanent conceptual cost. It is justified when A is genuinely
too late, or when A alone cannot distinguish how it was obtained. It is not justified merely to give
some reactions priority. Never let P degrade into a notification that callers may emit without
performing A, or duplicate all A reactions onto both types.

Current strong examples are:

- `PlayTag<Class<Tag>>` precedes the corresponding real Tag. Card play creates one `PlayTag` per
  printed tag before payment settles; discounts and alternative payment effects subscribe there.
  Successful card entry then creates the card's real Tags automatically with their printed
  multiplicity.
- `Trade<ColonyTile>` precedes `FlownTradeFleet<ColonyTile>`. Trade Envoys and Trading Colony
  subscribe to `Trade`, establish a `TradeBarrier`, and finish their optional track decision before
  the already-selected fleet movement can occur.

Card play, standard projects, conversion actions, and card-resource tender use the same precursor
and invoice principles. Their detailed lifecycle belongs to [ACTIONS.md](ACTIONS.md); do not repeat
that inventory here.
Two related families should not be described more strongly than the implementation supports:

- `UseActionN<HasActions>` commits to that authored action instruction, and Cryo-Sleep and Sky
  Docks use the action-qualified Trade signals to supply the selected payment resource. This is
  generic action dispatch, however, not a promise of one uniform later component Type.
- `BuyCard` distinguishes a purchase from any other `ProjectCard` gain. `BuySelectedCards` first
  creates the complete base `Owed` amount, then broadcasts one `BuyCard` per remaining selected card
  so Polyphemos and Terralabs Research can adjust that established debt, and only then creates the
  single invoice hosted by the live `BuyCards` component. The invoice exposes payment and gates the
  follow-mode `ProjectCard` gain until settlement. In real-card mode, the operation moves those
  exact selected cards to `Hand` only after the invoice is paid. These three choice-free stages are
  one inline automatic continuation, so automatic-sibling enumeration cannot reorder them.

  The current `BuyCard` signal carries multiplicity but not selected card identity. That is exact
  for the two existing price modifiers. If a future modifier needs to inspect individual selected
  cards or make a choice, specialize the pricing fact or purchase operation rather than extending
  this inline continuation with identity reconstruction or player work.
- A failed printed global-parameter requirement creates a typed `Required<GlobalParameter>`
  shortfall, then emits `RequirementCheck<CardFront>`. Inventrix, Morning Star, Adaptation
  Technology, and Special Design react to that completed check stage rather than competing
  with shortfall creation as `PlayCard` siblings. The final card entry remains gated on the absence
  of `Required`.

  `Required` itself remains owner- and parameter-scoped rather than card-scoped. That is sufficient
  while one card-play attempt establishes and settles its shortfall before another can overlap. If
  future rules permit overlapping attempts, specialize the shortfall with card or operation identity
  rather than restoring sibling-order dependence.

`Pay` is a transaction marker created in the same `FROM` instruction that removes the resource,
not an earlier promise of a later removal. `FirstPlayerOcean`, `WorldGovernmentTerraforming`,
`ResetColonyProduction`, and the colony-bonus Signals have the request/continuation shape but no
current subscribers that need a before-A modification. `Accept` is not a committed precursor at
all: it exposes an optional payment choice.

Random automatic-effect order exposes a separate limitation in payment history. Steel, Titanium,
Advanced Alloys, and similar `Pay` reactions all remove the same saturating `Owed`; when their total
value exceeds the remaining debt, execution order decides which cause receives credit for the last
units. Reconstructed games still reach the same paid state, but replay summaries of individual
discounts vary. Do not stabilize those summaries by assigning an order to the sibling effects. The
better repair is the payment direction in [PAYMENTS.md](PAYMENTS.md): produce complete, source- and
invoice-qualified tender value first, then consume debt once, retaining enough evidence to validate
excess payment and attribute every contribution. Until then, this attribution variance is diagnostic
noise rather than a gameplay regression. That larger, nice-to-have repair is intentionally not
folded into the card-play sequencing fixes here.

### Do not make proposed changes triggerable

A generic `PRE A:: B` would fire after an `A` change had been concretely resolved but before it was
applied. Do not add this. Resolution is allowed to read the current World only because its result
must be the next mutation. Letting B mutate first makes the resolved A stale: B could consume A's
removal target, fill its gain limit, remove one of its dependencies, or otherwise make the exact
change impossible. Executing A anyway can violate the component model; resolving it again permits B
to happen in response to an A that then changes or disappears. Making either outcome roll back
requires a new speculative-change contract rather than ordinary Effect semantics.

PRE would also subscribe to an intention rather than a component fact. There is no earlier
`ChangeEvent` for B's Cause to name, and accurate history would need a second event kind plus rules for
listener snapshots, atomization, multiplicity, and nested PRE cycles. A committed precursor keeps
all of that in the existing model: record a real P only after the operation commits to producing A,
make A mandatory for successful completion, and roll both back if the operation reaches a dead end.
The extra P Type is visible conceptual cost, but it is narrower and more truthful than making every
resolved change observable before it exists.

## Use automatic effects to preserve player-visible invariants

For one concrete change, the engine recursively executes all matching automatic effects before
admitting its queued effects:

```pets
A:: B
A: C
```

The primary reason to write `A:: B` is that the World after A but before B is not a coherent state to
hand back to a client or Player. A may have exposed half of one conceptual fact, temporarily broken
an invariant, or created hidden structure that the rest of the game must be able to assume exists.
Running B inline restores that invariant before queued work is exposed, so the Player cannot inspect,
choose against, or act on the half-established state.

Use `A:: B` only when all of these are true:

- B is a fully determined, choice-free consequence of A;
- exposing A-without-B would misrepresent the game state or expose incomplete structural
  bookkeeping; and
- the invariant must be restored before any queued player work becomes available.

The practical diagnostic is: could the engine stop after A, show that World to the Player, and let
them choose what to do next? If yes, use queued `A: B`. If no, and B is the fixed consequence that
makes the World coherent again, use `A:: B`. The purpose of `::` is not speed or preferred task
order. It establishes what must already be true by the time control returns to the Player.

In the example, B is established before C becomes player work. This is not `B THEN C`; B and C are
independent reactions to A. Nor does `::` make the entire execution internally unobservable:
automatic effects can still produce observable component changes and trigger further effects. It
specifically prevents queued player work from interleaving with the invariant restoration.

Settled uses include:

- generated card tags before printed queued effects and tag reactions;
- hidden adjacency creation before area bonuses and tile reactions;
- old energy-to-heat conversion before production payouts;
- fixed card requirement/cost/payment bookkeeping before gated choices; and
- invisible marker creation that users should never execute manually, including the research
  `Selecting` scope before queued card offers;
- completion and last-call flags derived from already-committed state changes that players can
  cause; and
- helper Signals caused by player activity whose only purpose is to fan out later gameplay work.

Effects triggered only by Admin workflow events, such as `SetupPhase`, use queued `:` by default.
The workflow already determines when those effects become available, and there is no end-user
decision whose queue entry must be suppressed. Use `::` there only when exposing the World between
the trigger and its consequence would violate a concrete invariant.

The canon single-colon audit leaves queued effects only when their right side is a recognizable
gameplay event or choice, or when current sequencing semantics require a task transition. Several
implementation-shaped cases are intentionally still queued:

- removing a Mandate must not destroy the context that supplies its selected action;
- End and played-event scoring must remain reorderable until all score-producing work is present;
- action-cost adjustments must wait for the base action's Owed components, because sibling
  automatic effects have no order; and
- the solo production correction must wait for production payouts before removing M€.

These are limitations of the current completion model, not evidence that those operations are
meaningful user decisions. Do not turn them automatic until their required lifetime or dependency
is expressed directly.

Trade Envoys and Trading Colony deliberately create a `TradeBarrier` automatically while their
queued optional production decision later removes it.

No gameplay ordering guarantee may depend on mutable runtime state that rollback does not restore.
In particular, do not rely on registration order between two automatic effects. If one must follow
the other, make the first event trigger the second.

Ordinary execution retains self-effects before other effects for reproducible history, and orders
each group from immutable pending-work data. Neither order has gameplay meaning. The diagnostic mode
shuffles the entire eligible automatic batch and has found no remaining game-state difference.
Two whole-game assertions that assign saturated `Owed` removals to Advanced Alloys can still vary;
that known payment-history defect is described above and does not justify ordering payment effects.
Do not use `::` for a player choice, just to manipulate queue admission, or as a substitute for
controlled completion.

Lifecycle families using mixed modes still need audit. Card play also uses a broad barrier whose
reach may be wider than its payment transaction.

### Put choice-free work at its semantic owner

A queued Player task should exist because the Player can make a real choice: whether to act, which
alternative to select, how to narrow it, or when to perform one of several reorderable effects. Do
not expose a Player task merely because the implementation needs a pulse or cleanup step.

Place choice-free work according to what it means:

1. use `::` for a fixed consequence needed to restore a coherent World before Player work;
2. use an Admin-assigned queued task for neutral game or workflow activity that remains meaningfully
   observable; or
3. use the exact lifecycle's completion event for cleanup that must wait. If no such event exists,
   leave the mechanism open rather than attaching cleanup to the Player's whole queue.

Admin assignment says that the neutral table Actor, rather than a Player, selects or narrows the
work. It does not assert that every outcome is equivalent. A default Admin policy may act
aggressively, while another permitted policy may make different legal choices. That strategy is not
an engine concern. Likewise, a helper Signal may remain useful causal vocabulary without becoming a
Player command.

The separation between inline `::` effects and Admin tasks requires a dedicated audit. A fixed rule
consequence belongs to engine execution even when it completes before the next Actor mutation. Work
belongs to Admin when the game meaningfully presents it as activity or choice by the neutral table
Actor. Timing alone does not decide the representation.

The current documented exception is `WildTagUse?`: Routine completion must settle those tasks when
they are the acting Player's only remaining work, then remove the corresponding uses. Do not add
other Routine cleanup bridges merely to make a replay pass; that can hide the misplaced effect,
owner, or completion rule that made a chore player-visible.

Action-local temporary state follows the same rule. Its uniquely implied settlement must complete
with the action before workflow offers a second action. Declining that later offer is a separate
turn decision; it must not double as current-action cleanup.

### Choose condition time explicitly

These Effects test their Requirements at different times:

```pets
A IF R: B
A: (R: B) OR Ok
```

The first tests R when A's exact Change Event fires. If R is false, no task is created; if it is
true, later changes to R do not cancel B. The second always creates a task and tests R when that task
is resolved against the later World. It also makes B optional when R is true because `Ok` remains a
valid arm. The forms are therefore not interchangeable.

Prefer trigger-side `IF` when R cannot change in the interval or when R qualifies the original
event. Global-parameter threshold bonuses, trade income measured before the colony track resets,
and Recession's test for another Player all deliberately freeze trigger-time state. Use the gated
form only when later sibling work is meant to decide availability and declining B is legal.
Pharmacy Union is the current model: each queued Science-tag consequence checks the then-current
Disease count, allowing one consequence to remove the last Disease before another offers the
corporation flip.

For one automatic batch, the current engine selects every matching effect and evaluates its
trigger-side condition against the World before any instruction in that batch executes. It then
executes the resulting instructions against the evolving World. Preserve the snapshot rule while
removing execution-order guarantees; an automatic sibling must not rely on another sibling having
already mutated the World.

## Exploratory continuation semantics

**Aspirational hypothesis, not an approved language feature.** The action-card marker exposes three
different needs that should not be collapsed into one vague “automatic `THEN`”:

- an inline choice-free continuation immediately after selected work;
- a choice frozen from trigger-time state; and
- completion after every descendant of a delegated operation drains.

These are also distinct from end-of-turn settlement. A turn-completion handoff does not provide
inline execution, freeze a later choice against earlier state, or resume an individual delegated
child.

Marking an action card before `UseAction` prevents a second use but makes Viron's own marker visible
to Viron's target Requirement, producing the awkward
`ActionUsedMarker<!Viron>` Complement. Marking it afterward as queued work makes the
marker deferrable and can permit another use.

An author-local automatic tail might help with hidden bookkeeping, but it would not by itself solve
Viron: the queued target is resolved against the later World where the marker already exists. It
also would not solve Head Start, whose preferred direction uses an end-of-action workflow handoff.
Investigate those semantics separately before adding syntax.

### Existing resolution precedent

The engine already combines resolution and forced continuation in several narrower ways:

- `Instructor` resolves every instruction immediately before inline execution, including the
  instruction of an automatic effect.
- Selecting an ordinary task resolves it; selection may leave an abstract task locked against every
  other World mutation until that task finishes.
- Narrowing a selected task automatically resolves the narrowed instruction again, because the
  selected task still owns the next mutation.
- Auto-exec may select an ordinary task as a convenience policy.

These are useful machinery, but they do not currently give `::` a choice point. An automatic
instruction that remains abstract after resolution fails instead of entering the task pool. No
current rule admits an abstract automatic instruction and immediately resolves it because it was
automatic.

### A constrained deferred-automatic hypothesis

One possible extension would allow at most one abstract instruction in a dynamic chain of `::`
effects. The engine would finish every other automatic effect, admit that instruction as a task,
immediately select and resolve it, and use the select-lock to make it the only work the Player may
change or execute.

Nothing earlier remains suspended across that task transition. When the Player narrows the selected
instruction to concrete, it executes normally and its own automatic effects run normally. If one of those also
remains abstract, the chain has violated the one-choice limit.

This is substantially narrower than giving every deferred automatic task general priority. Its
meaning would be “the invariant-restoring operation needs one forced choice,” not “this queued
choice should happen sooner.” Trade Envoys could potentially express its optional track increase
this way without creating and later removing a `TradeBarrier`.

The small syntax rule does not by itself settle the runtime semantics. Before exposing the choice,
the engine must drain every other choice-free automatic consequence that can affect its resolution;
otherwise effect registration order could change the available choices. It must also reject a
second abstract instruction in the same causal chain and decide whether a selected instruction group
is one operation or illegally expands into several unlocked tasks. None of this requires retaining
already-finished automatic work as a continuation.

Keep the current choice-free rule for `::` unless this constrained model is selected and proves that
it removes more permanent machinery than it adds.

## Whole-World idle cleanup

The `Temporary` class is a narrow component lifetime contract: whenever every task queue is empty,
the engine removes every live instance of that class before notifying workflow that the operation
has completed. Removal effects may create more components or tasks. The engine runs ordinary
automatic work again and repeats cleanup until an idle pass finds nothing to remove. Only that
empty pass allows the workflow callback. All of this remains inside the enclosing atomic transaction.

`EventCard` uses this contract. Its immediate work and tag reactions therefore finish while the
live card still exists, and removing it creates the corresponding `PlayedEvent`. Law Suit is the
deliberate exception in behavior, not machinery: its authored consequence moves the card directly
to `PlayedEvent`, so there is no EventCard left for idle cleanup.

This rule neither removes pending tasks nor identifies the end of a nested action. `WildTagUse` and
other task-lifetime problems therefore still need their own exact completion rule.

## Controlled completion (unresolved)

No general completion mechanism is selected. A Player queue drain can combine unrelated work and
can delay a local completion event until too much other work has finished, so do not use it as the
generic mechanism. Queue cardinality has no gameplay meaning.

Keep the problems separate:

- **Payment:** replace parallel optional tender tasks with one required payment-choice task at a
  time. The task means “pay one currently accepted unit”; its concrete refinements name the legal
  resource or card-resource tenders. After a payment, create another such task only if matching
  `Owed` remains. Reaching zero debt removes Billing directly and immediately enables its payoff or
  card entry. Payment completion does not wait for the rest of the Player's queue.
- **Head Start:** avoid nested completion frames. Treat its first immediate action as work in the
  current Prelude turn. After normal end-of-action settlement completes, grant a second ordinary
  action turn. If authoritative evidence requires the two printed actions to form one indivisible
  operation, document this simpler timing as a deliberate house rule rather than disguising it as
  exact fidelity. The engine still needs one precise end-of-action completion hook.
- **Trade:** retain `TradeBarrier` until a simpler completion event can keep fleet movement after
  every optional production decision without losing the selected trade operation.
- **Workflow return:** use the existing Player-turn control frame as the candidate unit. Required
  action settlement must finish before the workflow offers a second action or passes control.

`WildTagUse` is a proving case for that hook: sequencing must replace its documented Routine
cleanup bridge without removing the bridge before the replacement exists.

Any future engine-owned completion hook must run inside the enclosing atomic transaction before it
commits, so failure cannot expose partially settled state. Client autoexecution policy is separate
and must not define when engine settlement occurs.

## Atomicity audit hypotheses

**Unconfirmed working hypotheses, not game rules.** The unanswered
[atomicity thread](https://boardgamegeek.com/thread/2626062/which-instructions-are-atomic)
suggested these as useful cases to test for indivisibility:

- paying for and putting a card into play, including tags and immediate effects;
- direct transfers and exchanges;
- one stated amount of resource or production change;
- one multi-card draw or discard;
- fleet movement, trade income, and track reset, while leaving colony bonuses as triggered
  siblings; and
- possibly each individual right-hand side of a triggered Effect.

The same investigation suggested these should remain observably decomposed:

- multi-step global-parameter changes, so threshold bonuses cannot be skipped;
- the ocean placement and M€ loss printed on one Hellas space; and
- separate Philares reactions to separate new adjacencies.

Designer discussion linked from that investigation also distinguished component-level steps from
Effect identity: gaining 5 M€ may create separate changes while an “if one or more” trigger still
observes one original Effect. A Player cannot repartition a 7 M€ loss into seven Effects to multiply
Mons Insurance. Any implementation must therefore test the three dimensions above separately.

## Settled families

Fixed and X-scaled standard-resource Action costs use provider- and action-qualified invoices whose
removal unlocks the payoff. Costless and direct costs retain normal Pets sequencing. See
[ACTIONS.md](ACTIONS.md).

These current encodings are considered principled:

- Global-parameter change before TR and threshold reactions.
- Tile placement before adjacency, bonuses, and placement reactions; the reactions remain siblings.
- Direct spend-to-benefit offers on Olympus Conference, Recyclon, and St. Joseph of Cupertino
  Mission.
- Neptunian Power Consultants using a named optional signal before creating its spend-to-benefit
  payment sequence.
- Card-resource payment modifiers whose `THEN` carries X into an `Owed` reduction.
- Capital, Flooding, Mining Rights, and Mining Area carrying one selected identity into a follow-up.
- Spend-enabled effects establishing `Owed`, `Accept`, and a barrier before their payoff.
- Trade income and individual colony bonuses as reorderable siblings. Do not chain all colony
  bonuses after income.

Current behavior also correctly relies on natural unavailability for Immigrant City and some Energy
Tapping states. Colony placement currently queues track adjustment and placement bonus as siblings;
this is acceptable only while nothing can observe their relative order.

## Rules that deliberately impose no order

- A card's direct Effects are normally freely reorderable.
- Rebates, tag reactions, Mars University, Olympus Conference, and other persistent reactions may
  be resolved before, after, or between direct Effects once triggered, subject to ordering inside
  one Effect.
- Trade income and each colony bonus are separate siblings controlled by the active trader. Pluto
  may use trade-income cards before its own draw/discard bonus.
- Separate activations of one Effect remain separate tasks. Whether work inside one Mars University
  activation may be split around another activation remains under audit.

## Known defects or missing rules

- **Head Start:** current sibling tasks let its actions interleave. Prefer using the current Prelude
  turn for the first action and granting a second ordinary action turn only after normal
  end-of-action settlement, without nested completion frames. Record that timing as a house rule if
  authoritative evidence requires a stricter indivisible operation.

## Open design or rules audits

- **Mars University:** current incremental `THEN` permits two discards before either draw when two
  activations trigger. Determine from authoritative evidence whether each discard/draw exchange is
  indivisible.
- **Action marker and Viron:** marker-first prevents reuse but pollutes Viron's target requirement;
  marker-last can be deferred. Distinguish frozen trigger-time choice, forced inline continuation,
  and descendant completion before adding syntax.
- **Candidate draw/select/play:** Valley Trust, Merger, and New Partner use hand cards in
  incremental chains, so candidates are neither isolated nor forced to continue. Prefer one
  operation-scoped candidate representation if a fix is selected.
- **Controlled completion:** identify the exact end-of-action hook needed by workflow, Head Start,
  second-action offers, and `WildTagUse`. Keep auditable `Owed` and `Required` facts; do not infer
  successful payment from queue drain.
- **Trade settlement:** retain `TradeBarrier` until a direct completion event can keep fleet
  movement after every optional production decision without losing the selected trade operation.

## Workflow precedence

Phase and turn precedence belongs to [WORKFLOW.md](WORKFLOW.md#domain-requirements). This document
owns only the generic sequencing mechanisms used to express those rules.

## Audit method

For a new A-before-B claim:

1. Identify the illegal committed result or rules violation that would occur without the ordering.
   If the only consequence is a recoverable dead end, preserve the freedom and rely on rollback.
2. Record authoritative wording and the smallest observable counterexample.
3. If an effect must modify A before A exists, ask whether a committed precursor P can truthfully
   promise A; audit every producer of P and keep after-A reactions on A.
4. Ask whether A or the ambient rule owner should trigger B, possibly with `IF`.
5. If A-without-B is not a coherent state to expose and B is choice-free, use automatic `A:: B`;
   otherwise prefer queued `A: B`.
6. If only certain authored A sources need B, ask whether each source owns `A THEN B`.
7. If `THEN` exists for a shared Type variable, verify that A naturally owns the choice and B is
   derived from it; do not mistake that local artificial order for broader game precedence.
8. Otherwise identify the smallest completion fact: a local condition may call for a barrier, an
   entire workflow step may call for whole-World completion, and delegated narrowing may call for
   retaining a controller across task reassignment. Do not introduce nested completion frames by
   default.
9. Use only a committed mechanism; leave the case open when it requires an exploratory completion
   rule.
10. Add a precedence test and, when relevant, a freedom test that demonstrates alternative legal
    orders. Do not require those orders to converge unless the game rule itself makes order
    irrelevant.
11. Classify the result above and update `TODO.md` if work remains.
