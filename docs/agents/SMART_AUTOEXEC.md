# Proof-preserving autoexecution

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** evaluating whether an automatic task command preserves outcomes and decision
> authority, or investigating task independence.
>
> **Status:** optional policy research. No proof analyzer or certificate framework is implemented.
> [AutoExecPolicy.kt](../../src/common/dev/martianzoo/agent/AutoExecPolicy.kt) and
> [AutoExecLoop.kt](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt) define the current
> policies and shared loop.

## Scope of the current question

Start with a concrete, non-Hidden gain or removal task already present in a Player's queue during
play with current Canon. The Player must resolve every pending task; only their order and any
authored choices are at issue. Ask whether some legal way to clear the resulting work produces a
preferable outcome that is lost by doing this task first. Include tasks generated while clearing
the queue and the effects of cards already in play. Do not use tasks inserted by a test, a
hypothetical card, FakeCanon, or FakeHeadStart as counterexamples to a claim about current Canon.

`TfmWorkflow.Automatic` waits for outstanding work to drain before starting the next phase or
granting the next action. Production cannot occur midway through clearing a Player's queue. In
particular, Factorum's action is not a counterexample to immediately taking a pending Energy gain
unless a reachable Canon play actually puts both tasks in that queue. A future Canon edit that
changes a proved premise should ideally be caught by a check like `CanonInvariantsTest`; that is a
guard on Canon changes, not a reason to model imaginary cards now.

A policy chooses among engine-legal commands. It cannot establish missing scheduling rules or make
an otherwise forbidden intervention safe. In particular, delegated payment control must work with
`NONE` before a stronger policy can claim to preserve it. Scheduling and operation completion
remain open questions in
[SEQUENCING.md](SEQUENCING.md#the-missing-rule-when-an-operation-is-over).

`Agent.reader` currently exposes `GameReader`, so whole-game reads do not require a proposed scoped
reader. Analysis must leave the live World unchanged. The mechanics of exhaustive hypothetical
execution remain open.

## Source map

- [`AutoExecLoop.kt`](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt): `actOnce` implements
  current policy choices; it is not a general safety proof.
- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt): `enforceSelectLock`,
  `canSelectTask`, `prepareTaskNarrowing`, and `requireComplete` define current command constraints.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt): `resolve` and
  `executeResolved` distinguish current-state simplification from execution and consequences.
- [`Task.kt`](../../src/common/dev/martianzoo/state/Task.kt): controller, selection assignee,
  current assignee, selection, instruction, continuation, and cause all affect unfinished work.
- [`SafeAutoExecTest.kt`](../../test/common/dev/martianzoo/agent/SafeAutoExecTest.kt): current
  singleton selection, per-Actor policy, enabled-task, and rollback scenarios. The test name does
  not establish safety for arbitrary compositions.

## Formal contract

A possible strong policy contract is that its command preserves all reachable gameplay outcomes
and every Player's authority over meaningful decisions. Executing successfully is insufficient:
choosing an order can remove outcomes or force another Player to decide earlier.

For a fixed premise, let `S →a S'` denote an ordinary legal task command, including its automatic
consequences and task updates, but excluding subsequent policy choices. Corrections and deliberate
rule bypasses are outside this relation. Fix a completion criterion and an equivalence relation
that retains everything later gameplay can observe. Let `N(S)` be the successful completed outcomes
reachable under those definitions. Outcome preservation asks for:

```text
N(S) = N(a(S))
```

Since `a` is already legal, its outcomes are reachable from `S`; the difficult direction is showing
that every other outcome remains reachable after `a`. A useful argument may show that every
successful schedule has an equivalent schedule beginning with `a`.

The completion criterion is not settled for arbitrary delegated operations. An empty Actor queue,
zero debt, and whole-World idleness describe different facts. Current `requireComplete` checks
pending tasks against an allowed set and also rejects `MustCleanUp` state. That implementation is
evidence for current operations, not a definition of every future control interval.

Outcome equality alone does not preserve agency. Corresponding choices must remain with the
appropriate Actor, including the choice of when to hand work over. Equal final resources do not
justify P1 deciding for P2 or prematurely starting P2's payment. Any proof must state which decision
and observation rights it preserves. Termination, probability, and intermediate observations need
explicit treatment if the promised contract includes them.

Unknown or incomplete analysis means no automatic choice. A bound on search cost is acceptable
only when exceeding it produces uncertainty rather than a safety claim. After a committed command,
analysis based on the previous World must be reconsidered.

## State equivalence questions

Equal visible resource counts are too weak. A candidate state comparison must account for:

- exact component Types, multiplicities, dependencies, and ownership;
- pending instructions and continuations, including unresolved linked choices;
- controller, selection assignee, current assignee, and selection state;
- any cause relationships that the compared operations or client helpers consult; and
- scheduling eligibility and operation membership, if a future model adds them.

Components have no instance identity, but that does not permit arbitrarily renaming Actors,
providers, or dependencies. Task ids and event ordinals can be ignored or renamed only when the
relevant relationships and every subsequent legal command are preserved. Current payment helpers
consult causes, and a proposed causal scope could depend on ancestry, so excluding history needs
an argument appropriate to the actual contract.

Iteration order and derived indexes should not create gameplay meaning. If a custom implementation,
workflow, or external card-identity record can affect later play, either account for it or state
that the proof does not cover that execution path. Do not claim whole-game safety from an engine
projection that omits such facts.

## Why immediate commutation is insufficient

Suppose executing A creates queued work C; C changes a metric or opens a gate used by B. Immediate
orders A,B and B,A can leave the same components with C pending, while the legal interleaving A,C,B
has a different result. A comparison limited to the original queue misses it.

Consequently, equal immediate results for A,B and B,A do not prove either command safe to automate.
Possible sufficient conditions include a closed batch with no new interleavable work, or a proof
that A can move before every command that could precede it, including created tasks. The latter
must preserve availability as well as results: a command cannot commute through a state in which
it becomes illegal. Automatic effects belong to the command's transition, while queued effects and
`THEN` continuations can create additional interleavings.

“No matching trigger” is useful evidence, but does not establish independence. Commands can share
a cap, consume dependencies, alter a metric or gate, cascade removals, or install an effect that
changes later behavior without immediately creating work.

## Candidate proof ideas

These are hypotheses to assess against a defined engine contract, not an implementation sequence.

- **Selected concrete work:** executing it may be forced while the selection lock excludes other
  ordinary commands. The argument still needs concrete to mean that no distinct legal narrowing
  remains, and must account for the task's completion and consequences.
- **Forced narrowing:** a partial restriction is safe when every viable completion already obeys
  it. Enumeration must be complete for that claim. An unselected task additionally needs the fact
  to survive other legal work that could happen before selection.
- **Interchangeable tasks:** equal text is insufficient; authority, continuations, and relevant
  causes must also support the claimed equivalence.
- **Independent additions:** exact gains may commute when limits, dependencies, effects, metrics,
  and future availability cannot make their order matter. A resource Class allowlist does not
  prove those conditions.

A sole pending task or sole successful selection probe does not by itself establish all these
premises. Resolution can prune alternatives, evaluate metrics, split work, and admit continuations;
a proof must cover the choices those transformations preserve. Nor does a task need to be concrete
for an explicit narrowing to make it executable.

## Catalog and premise analysis

Existing class tables, declared effects, and live subscriptions may help bound possible
interactions. Useful questions include what an instruction reads, what it changes or removes
through dependencies, what work it can create, and which subscriptions it can install or remove.
These facts must cover inherited and self effects, Actor constraints, and custom implementations.

Conservative overestimation can decline useful automation; missing a possible interaction can
invalidate the proof. An absent listener or unchanged metric in the current World remains useful
only if intervening legal work cannot change that fact. Cycles and opaque custom behavior need
explicit uncertainty unless a sound argument covers them.

No summary compiler, cache structure, certificate format, or separate proof kernel is selected.
Their permanent complexity must be justified by useful automation that simpler analysis cannot
provide.

## Validation strategy

Tests can falsify a claimed proof rule and check its premises; passing examples do not establish
a general theorem. For this question, use queues reached through current Canon play. Compare
complete legal queue clearings before and after the proposed command, retaining both outcomes and
decision ownership.

Distinguishing cases include spawned work, installed listeners, shared caps, mutable gates and
metrics, dependency cascades, optional declines, `THEN`, custom behavior, and cross-Actor control.
Delegated payment cases must first establish engine legality with autoexecution disabled, including
P1 intervention attempts between P2's payment commands. A policy test cannot supply that missing
rule by draining the queue quickly.

Changing task enumeration order helps expose an incidental ordering assumption. Replays can then
measure whether sound automation removes real interaction costs, but cannot weaken the safety
contract. Measure observed coverage, unresolved cases, and analysis cost from current runs.

## Canon task-order corpus to build

### Failed corpus attempt

An earlier attempt wrote tests by inserting pairs of tasks with `Agent.addTasks`. That proved only
what the engine does when given those pairs. It did not show that current Canon can put both tasks
in a Player's queue. In particular, pairing an Energy gain with Factorum's action assumed the
very scheduling fact that needed checking. Pairing a Player's removal task with an Admin production
phase was invalid for this question: the automatic workflow waits for outstanding work to clear
before starting production.

The replacement tests used real Canon card plays, but mostly compared two orders within a single
card. Agreement in those plays could not establish that doing a Type's gain or removal first is
always safe. Corporation examples also brought starting-card payment into the queue and diverted
the investigation from the candidate task. Searching card text for `Steel` or `Energy` had the
same basic limitation: a rule's mention of a resource does not show that its task can coexist with
the candidate in a reachable queue. None of those test results supports a Type-wide claim.

The task-order test files from both attempts were removed. No code from them was retained or
stashed. Forking a World to compare legal orders remains a useful test technique, but the removed
fixtures and assertions encoded the wrong premises; preserving them would invite reuse as false
evidence. Write a specific safety claim and its cheap guard first, establish the relevant Canon
queue reachability, then add tests that could falsify that claim. FakeCanon and FakeHeadStart remain
outside the evidence set.

The test subject is a **pending concrete task**, not a card or an arbitrarily inserted pair of
instructions. Pause a real Canon play when such a task is in the Player's queue. Fork that same
World. In one fork do the candidate task first; in other forks clear earlier legal work first.
Finish every task generated by those choices. Compare the resulting game state, remaining choices,
and decision owners. A test only counts as a counterexample when both orders were legal from the
same reachable position. A comparison in which the task is the only possible next command records
a forced case, not an interaction.

Generate the coverage list from *observed pending tasks*, resolving each task to its exact Type,
direction, owner, dependencies, and quantifier. Group observations under these initial families,
then retain exact holder/area identities where they matter:

| Family | Directions to seek | Canon starting plays worth examining |
| --- | --- | --- |
| Six standard resources | gain and removal of MC, Steel, Titanium, Plant, Energy, Heat | Allied Bank, Mining Operations, Supply Drop, Biofuels, Early Colonization, Imported GHG; Local Heat Trapping for Heat removal |
| Six production Types | gain and removal of each standard-resource production | Mining Operations, Industrial Microbes, Business Empire, Heat Trappers, and production-changing project cards |
| Five card resources | gain and removal of Microbe, Animal, Science, Asteroid, Floater on a named Canon holder | Imported Nitrogen, Olympus Conference, AstroDrill, Air Scrapping Expedition, and cards that spend stored resources |
| Other concrete Types | gain and removal where Canon actually offers them | Terraform Rating and global-parameter steps; cards in Hand/Selecting; placed tiles, colonies, and delegates |

These are search seeds, not assertions that every direction is reachable or that those plays create
an ordering choice. For each observed Type and direction, cover a normal case and each *reachable*
interaction that could reverse the preferred order: another task reads its count, consumes it,
changes its capacity or dependency, changes a listener before it fires, or changes the number of
effects caused by it. Include queued reactions created while clearing work. Record directions
with no reachable witness rather than manufacturing a queue to fill the table.

For a positive rule such as “take Steel now,” a finite set of passing plays is only supporting
evidence. The rule also needs a Canon audit of every enabled declaration that can observe or
change Steel before this queue drains, including inherited effects, metrics, caps, dependencies,
gates, and custom code. Write down the cheap live-queue condition under which each interaction is
excluded. The same audit for Energy must explicitly check whether a `UseAction<Factorum>` task can
coexist with its gain; the fact that Factorum has that action is not enough. When a rule relies on
a property of all current Canon declarations, add a check in `CanonInvariantsTest` that names the
new or changed declaration violating it.
