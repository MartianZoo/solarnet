# Sequencing

**Status: working rules and audit.** This document defines the ordering model agents should preserve.
Current known defects remain defects even when tests characterize them.

## Default: preserve the whole valid decision tree

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
does not choose a play policy. A client may deliberately trade away flexibility for convenience by
selecting work in an order the user will probably accept, or it may play safe and ask. The current
auto-execution implementation is engine-side, but that selection policy belongs on the client side
and is intended to move there. Its order must not become an engine guarantee or an authored
precedence rule.

Tests should prove only real precedence. When freedom matters, also prove that legal siblings may be
reordered.

## Recoverable dead ends are part of the model

Supporting only valid game states does not require the engine to prove in advance that every locally
valid choice has a legal completion. An encompassing operation is speculative until it commits. A
client may choose a branch whose later mandatory work cannot finish; the engine should report the
dead end and roll the whole operation back rather than commit an invalid result. See the current
transaction mechanics in [ENGINE.md](ENGINE.md#recoverable-dead-ends).

Pruning that decision tree is a client concern. A client may simulate branches, recognize common
traps, choose a likely-good route, or ask the user. Pets and engine code should not duplicate target
exclusions, add premature requirements, or impose sequencing merely to spare the client from a
recoverable mistake. The engine may reject a branch as soon as ordinary rules make its failure
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

## Choose the weakest honest mechanism

Only impose sequencing when the game rules demand it or an otherwise valid operation cannot be
modeled correctly without it.

### 1. Prefer a trigger

If every A should cause B, including future sources of A, encode the rule once as `A: B` on A or on
the component that owns the ambient rule. Use `IF` when state distinguishes which A events qualify.

Examples:

- `GlobalParameter` owns its TR reaction because every qualifying parameter step uses it.
- `GreeneryTile` owns its oxygen reaction because every greenery raises oxygen. Solo setup cancels
  the oxygen locally after each neutral placement.

Use queued `:` by default. Use automatic `::` only for a fully determined, choice-free
consequence or hidden structural maintenance that must be incorporated before player work appears.

### 2. Use source-local `A THEN B` when that source owns both stages

`THEN` is appropriate when only particular authored A operations require B, no honest trigger or
state gate distinguishes them, and that instruction conceptually owns the pair. Costs and payoffs,
or a placement and a marker identifying that selected place, are normal examples.

Completing A enqueues B in A's place. B is not immediate and receives no priority over unrelated
tasks. `A1, A2, B1, B2` may be a legal order for two `A THEN B` chains. `THEN` waits for the A
task, not every transitive consequence it causes.

`THEN` also creates one implicit Type-variable region. Mining Rights and Capital use that linkage
to carry an area or tile choice into later work; temporal order is not its only purpose.

This can force sequencing that the game rules do not independently require. It is often still the
most natural expression: one stage owns the Player's choice and the other is derived from that same
choice, so put the choice-bearing stage first and its derived continuation second. Treat this as a
linkage constraint, not evidence that unrelated work needs priority. During audit, verify both that
the choice really belongs to the first stage and that the artificial order is buying a useful,
readable linkage rather than concealing a more natural unordered model.

If every producer of A is expected to remember B, the relationship belongs in a trigger instead.

### 3. Use a narrow barrier for distributed completion

A barrier makes later work illegal until separately produced work finishes: card payment before
entry, all optional trade-track choices before fleet movement, or one delegated operation before the
next. Prefer a specific gate such as `MAX 0 TradeBarrier` to `MAX 0 Barrier`.

A barrier controls legality, not priority. Other currently legal work remains reorderable. Phase
topology and Player control-until-drain belong to [WORKFLOW.md](WORKFLOW.md), not generic barriers.

### Prefer natural unavailability when exact

Do not author sequencing when the component model already makes B impossible before A. A production
increase tied to a newly placed unique tile cannot occur before the tile exists. This keeps order
conditional on the actual World rather than encoding a universal priority.

## Automatic and queued effects

For one concrete change, the engine recursively executes all matching automatic effects before
admitting its queued effects:

```pets
A:: B
A: C
```

This can honestly ensure that choice-free B is established before C becomes player work. It is not
`B THEN C`; B and C are independent reactions to A.

Settled uses include:

- generated card tags before printed queued effects and tag reactions;
- hidden adjacency creation before area bonuses and tile reactions;
- old energy-to-heat conversion before production payouts;
- fixed card requirement/cost/payment bookkeeping before gated choices; and
- invisible marker creation that users should never execute manually.

Trade Envoys and Trading Colony deliberately create a `TradeBarrier` automatically while their
queued optional production decision later removes it.

Do not rely on registration order between two automatic effects. If one must follow the other, make
the first event trigger the second. Do not use `::` for a player choice or merely to manipulate
queue admission.

Lifecycle families using mixed modes still need audit. Card play also uses a broad barrier whose
scope may be wider than its payment transaction.

## Exploratory continuation semantics

**Aspirational hypothesis, not an approved language feature.** The action-card marker exposes three
different needs that should not be collapsed into one vague “automatic `THEN`”:

- an inline choice-free continuation immediately after selected work;
- a choice frozen from trigger-time state; and
- completion after every descendant of a delegated operation drains.

Marking an action card before `UseAction` prevents a second use but makes Viron's own marker visible
to Viron's target Requirement, producing the awkward
`ActionUsedMarker<!CardVC5>` Complement. Marking it afterward as ordinary queued work makes the
marker deferrable and can permit another use.

An author-local automatic tail might help with hidden bookkeeping, but it would not by itself solve
Viron: the queued target is prepared against the later World where the marker already exists. It
also would not solve Head Start or event cleanup, which require descendant completion. Investigate
the three semantics separately before adding syntax.

## Sequencing, immediacy, and atomicity differ

- **Sequencing:** B cannot precede A. `THEN`, a barrier, or natural unavailability can establish it.
- **Immediacy:** work cannot be postponed behind a player choice. Automatic `::` expresses this.
- **Game-rule atomicity:** no player interleaving or rule observation may split the conceptual
  operation.
- **Failure atomicity:** `Timeline.atomic` rolls implementation state back after failure.

None implies the next. An automatic effect may execute several observable component changes.
`THEN` may allow unrelated work between its stages. Timeline rollback does not decide how triggers
count or observe a physical-game operation.

When evaluating an “atomic” rule, specify separately:

1. whether player work may interleave;
2. whether intermediate component changes fire/observe effects; and
3. what multiplicity a trigger sees as one effect.

Do not infer those answers from an unanswered community post. For disputed rules, find the linked
Jacob Fryxelius ruling or preserve the uncertainty.

### Atomicity audit hypotheses

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

These current encodings are considered principled:

- Global-parameter change before TR and threshold reactions.
- Tile placement before adjacency, bonuses, and placement reactions; the reactions remain siblings.
- Action costs before payoffs through generated `THEN`.
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

- **Event cleanup:** an event must remain live through its immediate effect and tags before moving to
  the played-event pile. Current sibling cleanup can make Solar Probe lose its own science tag.
- **Head Start:** every descendant of its first granted action must finish before the second begins.
  Siblings and ordinary `THEN` are too weak; this requires a completion scope or narrow barrier.

## Open design or rules audits

- **Mars University:** current incremental `THEN` permits two discards before either draw when two
  activations trigger. Determine from authoritative evidence whether each discard/draw exchange is
  indivisible.
- **Action marker and Viron:** marker-first prevents reuse but pollutes Viron's target requirement;
  marker-last can be deferred. Distinguish frozen trigger-time choice, forced inline continuation,
  and descendant completion before adding syntax.
- **Candidate draw/select/play:** Valley Trust, Merger, and New Partner use ordinary hand cards in
  incremental chains, so candidates are neither isolated nor forced to continue. Prefer one
  operation-scoped candidate representation if a fix is selected.
- **Card-play barrier:** determine whether `MAX 0 Barrier` should be payment-specific.
- **Trade settlement:** determine whether colony-track reset can be observed before income and
  colony bonuses finish.
- **Lifecycle mixed modes:** prove setup, generation, and phase effects do not depend on automatic
  effect registration order.

## Workflow precedence

These are domain constraints even where the current workflow approximates them:

1. Starting choices precede corporation/Prelude reveal. Corporations and Preludes resolve in player
   order; starting cards are paid before Prelude play.
2. Later generations pass first player and run Research before Action. Production follows only after
   all players pass.
3. Existing energy converts before new production; production payouts are simultaneous for game
   rules unless evidence says otherwise.
4. Solar checks game end before World Government Terraforming, Colonies, or Turmoil.
5. Colonies fleet return and track advance follow World Government Terraforming. Current Canon does
   them in Production/Generation and is incomplete.
6. Solo victory is tested before final greeneries.
7. Final greenery fully drains one player before the next; scoring follows all consequences.

## Audit method

For a new A-before-B claim:

1. Identify the illegal committed result or rules violation that would occur without the ordering.
   If the only consequence is a recoverable dead end, preserve the freedom and rely on rollback.
2. Record authoritative wording and the smallest observable counterexample.
3. Ask whether A or the ambient rule owner should trigger B, possibly with `IF`.
4. If only certain authored A sources need B, ask whether each source owns `A THEN B`.
5. If `THEN` exists for Type linkage, verify that A naturally owns the choice and B is genuinely
   derived from it; do not mistake that local artificial order for broader game precedence.
6. Otherwise use the narrowest barrier or completion scope.
7. Add a precedence test and, when relevant, a freedom test.
8. Classify the result above and update `TODO.md` if work remains.
