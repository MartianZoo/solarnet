# Autoexecution and pruning

> **Agent information:** This is an agent-maintained information-tracking document, written by
> agents for agents. It can record human decisions, but it is not human-authored documentation.
>
> **Read when:** changing autoexecution policy or the shared loop; designing a pruner or a
> preference; judging whether an automatic command preserves outcomes and decision authority;
> changing how Admin settles its work.
>
> **Status:** The current model is the three-value
> [`AutoExecPolicy`](../../src/common/dev/martianzoo/agent/AutoExecPolicy.kt) and the shared
> [`AutoExecLoop`](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt). The owner set a new
> direction on 2026-10-14 (D1–D6); none of it is implemented. This version replaces a note that
> treated the subject as proving individual commands safe. The parts of that analysis that still
> hold are folded in below.

## The idea in brief

A Player's interaction is a walk through a decision graph: which task to select, how to narrow it,
which task next. Many paths end in the same state; taking a Plant and then a Steel is the same as
the reverse order. **Pruning** removes edges that can only lead to dead ends. **Collapsing** groups
edges that lead to equivalent outcomes. When pruning and collapsing leave exactly one live choice,
taking it closes nothing off, and that is all autoexecution should do. Any automatic choice among
several live choices is a strategic preference. It is not autoexecution, and it should be labeled
as a preference.

Pruning is the hard part. Autoexecution only walks the path that pruning leaves.

## Owner direction (2026-10-14)

- **D1. Admin is greedy and fixed.** Admin executes its work in the order received. A task that
  fails is retried later in the same request. A full pass over the queue that executes nothing is
  an error. Whether Admin's Agent and the Players' Agents are the same kind of object is open.
- **D2. Policies compose.** Autoexecution is not an enum. An Agent attaches any set of small
  policies, in any order, from a library of specialized policies.
- **D3. Autoexecution is walking what pruning leaves.** Pruning removes edges that lead only to dead
  ends, and collapses edges whose outcomes are functionally identical. A next step that pruning
  leaves as the only edge is safe to take. An automatic pick among several live edges with possibly
  different outcomes is itself pruning, and usually poor pruning.
- **D4. Pruning is expensive** in this game. Expect complicated analysis.
- **D5. Never close a choice by default.** Autoexecution should preferably never remove a choice
  from its user. Some players will still want convenience that does.
- **D6. The model comes first** (2026-10-13). Do not argue for a Pets or engine design from
  autoexecution behavior. Policy adapts to the model.

## The decision graph

These definitions give the rules below precise terms.

- **G1. Decision point.** A World position together with an Actor who has pending work. Its edges
  are that Actor's legal Agent commands: select one of its pending tasks, or, for a selected task,
  choose one option of the current `TaskForm` decision (an `OR` alternative, a target, or an
  amount). A `doTask` call is a path of such edges.
- **G2. Completion.** A path is complete at operation completion as
  [SEQUENCING.md](SEQUENCING.md#operation-completion) defines it. This horizon is provisional
  (Q1).
- **G3. Live and dead.** An edge is *live* when some complete path continues through it, and *dead*
  when none does. Showing an edge live takes one witness path. Showing it dead takes an exhaustive
  argument.
- **G4. Equivalent edges.** Two edges are equivalent when they reach the same set of completed
  outcomes under the [state equivalence](#state-equivalence) relation, and every corresponding
  decision keeps the same Actor and the same position relative to reveals (R2, R3).
- **G5. Pruning and collapsing.** Pruning marks dead edges. Collapsing groups equivalent live
  edges into one *class*.
- **G6. Forced.** A decision point is forced when exactly one live class remains.
- **G7. Walking.** Taking a representative edge of a forced point's only class. Choosing among two
  or more live classes is a *preference*, not a walk.

Paths converge, so the structure is a DAG over states, not a tree. Edges are already exposed: the
Actor's queue lists tasks, and `TaskForm.decisions()` and `options()` list the narrowings.
`TaskForm.options` filters little, on purpose: a partial choice that looks unworkable can become
workable after a later choice. No new representation is needed to name an edge.

## Contract

- **R1. Outcome preservation.** For a fixed premise, let `S →a S'` be an ordinary legal command,
  including its automatic consequences and task updates but not later policy choices. Corrections
  and deliberate rule bypasses are outside the relation. Let `N(S)` be the completed outcomes
  reachable from `S`. A walk must satisfy

  ```text
  N(S) = N(a(S))
  ```

  `a` is legal, so its outcomes are already in `N(S)`. The hard direction is showing that every
  other outcome is still reachable after `a`. One way is to show that every successful schedule
  has an equivalent schedule that begins with `a`.

- **R2. Decision authority.** Equal outcomes are not enough. Each choice must remain with the same
  Actor, including the choice of when to hand work over. Equal final resources do not justify P1
  deciding for P2, or starting P2's payment early.

- **R3. Information order.** A walk must not move a decision from after a reveal to before it. A
  reveal is any fact supplied from outside the calculation, such as a drawn card or a revealed
  global event ([CARD_HANDLING.md](CARD_HANDLING.md), [ADVERSARIAL.md](ADVERSARIAL.md)). Two orders
  can reach equal outcome sets while one makes the Player decide with less information. R1 alone
  does not exclude that.

- **R4. Unknown means decline.** Incomplete analysis makes no choice. A search budget is
  acceptable only when exhausting it produces "unknown", never "forced".

- **R5. Re-derive after a commit.** Analysis based on an earlier World is stale once any command
  commits.

- **R6. Legal commands only.** A policy chooses among engine-legal commands. It cannot supply a
  missing ordering, exclusion, or completion rule, and it cannot make a forbidden intervention
  safe. Delegated payment must be correct under `NONE` before any policy can claim to preserve it.
  See [SEQUENCING.md](SEQUENCING.md#the-missing-rule-when-an-operation-is-over) and roadmap item 5
  in [SOLARNET_ROADMAP.md](../../SOLARNET_ROADMAP.md).

- **R7. Analysis leaves the live World unchanged.** Exploration uses rolled-back transactions
  (`Timeline.atomic` with an abort, as `ActorEngine.canSelectTask` does) or `Engine.fork`.

## Why the whole graph is never needed

The full graph from an interesting position is enormous. Autoexecution never needs it, because
its two possible answers cost very different amounts:

- **"Not forced" costs nothing to conclude.** By R4, failing to prove a point forced is a valid
  outcome that does nothing. Autoexecution never has to prove that a choice is real.
- **"Forced" is expensive.** Every other edge must be shown dead or equivalent. A search can stop
  as soon as it finds two live edges it cannot merge.

So the sources of size behave differently:

| Source | Raw size | Effect on autoexecution |
| --- | --- | --- |
| Order of `k` independent tasks | `k!` paths | The main target. Memoizing states reduces it only to `2^k` nodes (one per subset done). Getting to about `k` means not exploring alternative orders at all: *partial-order reduction* (persistent, stubborn, or sleep sets in model checking). |
| Tile spaces, holders, `X` amounts | Wide, often dozens | Mostly harmless. Distinct live outcomes end the search after two. Width costs something only when nearly every option is dead. |
| Payment splits | Moderate | Different leftover resources are real choices. Picking one is a preference, never a prune. |
| Draws (Merger, New Partner, any card draw) | Unbounded | The draw is a reveal and a leaf. R3 forbids reordering decisions across it. |

### Immediate commutation is not independence

Suppose executing A creates queued work C, and C changes a metric or opens a gate that B reads. The
orders A,B and B,A can leave the same components with C pending, while the legal interleaving A,C,B
reaches a different result. Equal results for A,B and B,A therefore show neither order safe.

Sufficient conditions include a closed batch that creates no interleavable work, or a proof that A
can move before every command that could precede it, including created tasks. This is the
persistent-set condition from partial-order reduction. The argument must preserve availability
as well as results: a command cannot commute through a state where it is illegal. Automatic effects
belong to their command's transition. Queued effects and `THEN` continuations create further
interleavings.

"No matching trigger" is useful evidence but does not establish independence. Commands can share
a cap, consume dependencies, change a metric or gate, cascade removals, or install an effect that
changes later behavior without creating work at once.

## Composing policies

**Proposal (2026-10-14), not yet accepted.** D2 says that if any attached policy wants to act, it
acts. Taken literally, the result then depends on list order, and nothing shows which policies can
close a choice. This proposal keeps the library and splits it by soundness into three parts:

- **Pruners** are sound, and their order is irrelevant.
- **The walker** is fixed and not configurable.
- **Preferences** are lossy by design, opt-in, and ordered.

Rules:

- **C1.** At a decision point, a pruner reports edges known dead and groups of edges known
  equivalent. Silence means unknown.
- **C2.** Findings combine by union. A dead edge stays dead, and equivalence groups merge
  transitively into classes.
- **C3.** The walker, not a pruner, picks each class's representative, by a fixed rule: first in
  queue order, then first offered option. Otherwise pruner A drops `x` because it equals `y`,
  pruner B drops `y` because it equals `x`, and both disappear.
- **C4.** A pruner's findings do not depend on other pruners' findings.
- **C5.** Dominance ("this outcome is better") is not a pruner finding. No ordering of outcomes is
  sound for every player, so dominance belongs to preferences.
- **C6.** The walker acts only at a forced point (G6), and only as the Actor who owns that point.
- **C7.** Preferences run only when two or more live classes remain. The first preference that
  chooses acts. Each preference choice closes options, so a client should be able to report it as
  such (D5).

The current enum maps onto this almost exactly, so the first change can be a refactor with no
change in behavior:

| Current | Equivalent composition |
| --- | --- |
| `NONE` | Walker disabled. |
| `CONCRETE` | Walker + one-step dead-end pruner (`canSelectTask`), counted per Actor. |
| `EAGER` | `CONCRETE` + a "first completing task in queue order" preference. |

Pruning has a second client besides autoexecution. A UI can grey out dead edges and merge
equivalent ones even when several live classes remain. That argues for exposing pruning as an Agent
query, with autoexecution as one caller of it.
[ENGINE.md](ENGINE.md#queries-invariants-and-dead-ends) already assigns client-facing exploration
and pruning to Agents.

## Admin

**Current model.** Admin is not locked to greedy.

- `Agents` gives every Actor, Admin included, `EAGER` by default, and any caller can change it.
- Import and `TaskLog.replay` set Admin to `NONE` and submit its recorded commands
  ([EXPORT.md](EXPORT.md)).
- [`MassiveGammaZoneTest`](../../test/jvm/dev/martianzoo/tfm/tests/replays/MassiveGammaZoneTest.kt)
  switches Admin to `CONCRETE` or `NONE` three times, each around Turmoil global-event work.
- `EffectAtomizationTest` and `PerAtomizationTest` set Admin to `NONE`.

In the loop, Admin's options come first, in queue order. When every Admin option needs input or
is temporarily unavailable, the loop falls through to Player options.
[IDENTITY.md](IDENTITY.md#admin-and-engine) says Admin "may control, receive, select, and narrow
tasks" and that "no identity rule requires Admin's decisions to be deterministic." D1 contradicts
that sentence. Settle it before implementing D1.

Today Admin holds work of three kinds:

1. **Bookkeeping**: concrete `System` changes and routine flow. Greedy execution is correct.
   IDENTITY.md already requires fixed gains and concrete removals to be safe for eager Admin
   execution.
2. **A choice a rule gives to some Player**: for example, a global event's tile placement. The
   choice should go to the Actor the rule names, using existing assignment (`TASK`, `BY`, effect
   context), leaving Admin only the concrete change.
3. **A supplied fact**: for example, which global event is revealed. It is a reveal. The dealer
   arrangement in [ADVERSARIAL.md](ADVERSARIAL.md) supplies it, as it supplies card identities.

D1 is consistent once Admin holds only the first kind. The consequences to verify:

- Admin leaves the policy map.
- Recordings omit Admin's greedy steps (the file format may change freely).
- Import stops disabling Admin.
- A pass that executes nothing becomes an error. Deferred work is excluded, because it waits by
  design.

Whether any other Admin work needs the current fall-through is exactly what the Admin census
(stage 0) answers.

## Current model (verified 2026-10-14)

- **Policy.** `AutoExecPolicy` is `NONE`, `CONCRETE`, or `EAGER`, held per Agent. A scoped override
  stack supports `autoExecNow(policy)` and `doWithAutoExecPolicy`. Assigning a non-`NONE` policy
  runs autoexecution immediately.
- **Loop.** `AutoExecLoop.actOnce`:
  - It returns without probing unless some pending, non-deferred task belongs to an Actor whose
    policy is not `NONE`.
  - Candidates are the selected task, or the only pending task, or else every pending task that
    passes the one-step `canSelectTask` probe. Manual Actors' tasks remain candidates and compete.
  - `CONCRETE` takes a task only when it is its Actor's sole candidate. `EAGER` takes any candidate.
  - Options are tried Admin first, then in queue order. The first that completes wins, and the loop
    restarts.
  - Needing input, being not fully specified, or an abstract `NotNow` is recoverable, and leaves
    the task pending. If every option fails without recovery, a `DeadEndException` is thrown. If
    no candidate passes the probe at all, the first eligible task is executed to surface its real
    error.
- **What `CONCRETE` checks.** `CONCRETE` approximates "forced": a task is its Actor's only candidate
  that passes a one-step probe. It does no lookahead and does not consider other Actors' decisions.
  The REPL's `auto` help says `concrete` preserves "every choice allowed by the game". That claim is
  untested; the explorer can test it.
- **Defaults.**
  - `Agents(world)`, and so the REPL, gives every Actor `EAGER`.
  - `TfmGameplayTest` gives Players `CONCRETE` and Admin `EAGER`.
  - Sandbox tests give Players `NONE`.
  - Import gives every Actor `NONE`.
- **Clients guarding choices from `EAGER`.** Four call sites save the policy, change it, and
  restore it:
  - `TfmGameplay.playCorp` (to `NONE`)
  - `TfmGameplay.pay` (to `CONCRETE`)
  - `TfmActionCommand` (to `NONE` while paying a written cost)
  - `TfmPayCommand` (to `NONE`)
- **Tests settling routine work.** Tests contain 430 `autoExecNow(EAGER)` calls in 31 files.
  [TESTING.md](TESTING.md) tells authors to settle competing routine effects that way, and states
  that an automatically chosen order is not evidence of a required order. Each call marks a point
  where `CONCRETE` stops and a test author judged the remaining order unimportant. That makes them
  the largest available seed for the census.

## Staged plan

**Proposal (2026-10-14).** Stages 0 and 3 are independent of the others. Stage 4's pruner order
should follow stage 1's census and stage 2's measurements.

0. **Admin census and lock.**
   - Classify every non-greedy Admin use into the three kinds above.
   - Move rule-delegated choices to the Actor the rule names.
   - Make supplied facts reveals.
   - Update IDENTITY.md, then implement D1 and its consequences.
1. **Census of choice points.** Change no behavior. Record each point where `EAGER` would act and
   `CONCRETE` would not, in replays and at the 430 `autoExecNow(EAGER)` sites. For each point,
   record the decision's edge kinds and the Canon plays that produced it. This becomes the
   real-play corpus that [validation](#validation-and-the-canon-corpus) requires.
2. **Bounded explorer** (test-only; a prototype that may be stashed). Run it on 5–10 census points,
   including a tile placement, a payment, and a Merger. Record its measurements here. See
   [The bounded explorer](#the-bounded-explorer).
3. **Composition refactor.** Replace the enum with pruners, walker, and preferences, following the
   mapping above, with no change in behavior.
4. **Pruner library.** Add one pruner at a time. Each pruner needs:
   - oracle tests at corpus points (the explorer's outcome classes do not change after the prune);
   - a `CanonInvariantsTest` guard naming any Canon property it relies on.
5. **Defaults.** Move Players to pruners only, keeping preferences opt-in (D5). Delete the four
   save/restore sites and the replay overrides.

Measures of success:

- the four save/restore sites and Admin's overrides are gone;
- the number of `autoExecNow(EAGER)` calls that become unnecessary (baseline 430);
- for each census point: forced, not forced, or unknown, with the explorer's cost.

## The bounded explorer

A test-only oracle, never run during play. Its permanent role is to check pruners (R1 made
mechanical). Its first role is to measure where size actually comes from.

- **Edges:** the acting Actor's queue and its `TaskForm` decisions (G1). Every Actor's edges are
  explored, with each decision labeled by its Actor (R2, Q2).
- **Horizon:** operation completion (G2, Q1).
- **Leaves:** reveals, and commands that need supplied information (R3).
- **Rollback:** depth-first with transactional rollback (R7); forks where parallel comparison helps.
- **Memoization:** keyed by a conservative canonical state (see below). A key that distinguishes
  too much only costs missed merges. A key that distinguishes too little makes the oracle wrong.
- **Budget:** a node limit. Exhausting it reports "unknown" (R4).
- **Report:** nodes expanded, distinct states, outcome classes, dead edges, branching by edge kind
  (task order, alternative, target, amount, payment), and whether the budget ran out.

Unverified expectation, to be replaced by measurement: task order dominates raw path counts; tile
width barely matters for forced-point detection; and most real points come out "not forced" within
a few dozen nodes.

### State equivalence

The canonical state must retain everything later play can observe. Equal visible resource counts
are too weak. A key must account for:

- exact component Types, multiplicities, dependencies, and ownership;
- pending instructions and continuations, including unresolved linked choices;
- assignee, selection, and deferral of each pending task;
- any cause relationships that operations or client helpers consult (payment helpers do); and
- operation membership and scheduling eligibility, if the model adds them.

Components have no instance identity. That does not permit arbitrarily renaming Actors, providers,
or dependencies. Task ids and event ordinals can be ignored only when the relationships and every
later legal command are preserved. Iteration order and derived indexes must not create gameplay
meaning. If a custom implementation or an external card-identity record can affect later play,
either include it or state that the result does not cover that path.

## Candidate pruners and preferences

These are candidates, listed roughly from cheapest to most expensive. Stage 1 should reorder them.

1. **Sole edge.** One out-edge. Trivially sound.
2. **One-step dead end.** An edge whose command fails now is dead *as this edge*. A later "select
   `t` after `u`" is a different edge at a different point. This is today's `canSelectTask` probe.
   Before relying on it, confirm that every probe failure means the command is illegal now, and
   not that it needs supplied information.
3. **Interchangeable tasks.** Collapse tasks with equal instructions only when assignee,
   continuation, and any consulted causes also match. Equal text alone is not enough.
4. **Forced narrowing.** At a selected task's `TaskForm` decision, every option but one is dead
   (for example, one legal space or one valid holder). The select lock excludes other work
   meanwhile. A claim made before selection must also survive any legal work that can precede
   selection.
5. **Closed-batch commutation.** Concrete changes that create no interleavable work and share no
   cap, metric, gate, dependency, or listener are equivalent in either order (the persistent-set
   condition). The premises can come from analysis of the catalog and premise: what an instruction
   reads, changes, removes through dependencies, creates, or subscribes, including inherited and
   self effects, actor constraints, and custom implementations. Overestimating interactions only
   declines automation. Missing one breaks soundness. No summary compiler, cache, or certificate
   format is selected. Each needs a measured case that simpler analysis cannot handle.
6. **Lookahead dead end.** `k`-step search with a budget (R4).

The only preference so far is **first completing task in queue order**, which is today's `EAGER`.
Add others only as real use demands them (roadmap item 5).

## Validation and the Canon corpus

Claims about current Canon need positions reached through real Canon play. Tasks inserted with
`Agent.addTasks`, hypothetical cards, FakeCanon, and FakeHeadStart are not counterexamples, and
they are not supporting evidence either.

An earlier attempt built a corpus from inserted task pairs, then from pairs of orders within single
card plays. Both encoded the wrong premise. Pairing an Energy gain with Factorum's action assumed
that a reachable play puts both in one queue. Pairing a Player's removal with an Admin production
phase ignored the fact that the game's flow starts the next phase only as `FINALLY` work, after
everything else drains ([SEQUENCING.md](SEQUENCING.md#game-flow)). Those tests were removed and
nothing from them was kept.

The test subject is a **pending concrete task at a real decision point**:

1. Pause real Canon play there.
2. Fork the World.
3. In one fork, take the candidate first; in others, clear earlier legal work first.
4. Finish all generated work, and compare outcomes, remaining choices, and decision owners.

A difference counts as a counterexample only when both orders were legal from the same reachable
position. A point where the candidate was the only command records a forced case, not an
interaction. Stage 1 supplies the positions, and the explorer automates the comparison.

Group observed tasks by exact Type, direction, owner, dependencies, and quantifier. These families
are search seeds, not claims of reachability:

| Family | Directions | Canon plays worth examining |
| --- | --- | --- |
| Six standard resources | Gain and removal of MC, Steel, Titanium, Plant, Energy, Heat | Allied Bank, Mining Operations, Supply Drop, Biofuels, Early Colonization, Imported GHG; Local Heat Trapping for Heat removal |
| Six production Types | Gain and removal of each | Mining Operations, Industrial Microbes, Business Empire, Heat Trappers, production-changing projects |
| Five card resources | Gain and removal on a named Canon holder | Imported Nitrogen, Olympus Conference, AstroDrill, Air Scrapping Expedition, cards that spend stored resources |
| Other concrete Types | Where Canon offers them | Terraform Rating and global-parameter steps; cards in Hand or Selecting; tiles, colonies, delegates |

For each observed Type and direction, cover a normal case and each *reachable* interaction that
could reverse the preferred order: another task reads its count, consumes it, changes its capacity
or dependency, changes a listener before it fires, or changes how many effects it causes. Record
directions with no reachable witness instead of manufacturing a queue.

A positive rule such as "take Steel now" also needs an audit of every enabled Canon declaration
that can observe or change Steel before the queue drains. The audit must cover inherited effects,
metrics, caps, dependencies, gates, and custom code, and state the cheap live-queue condition that
excludes each interaction. For Energy, the audit must check whether a `UseAction<Factorum>` task
can coexist with the gain; that Factorum has such an action is not enough. When a rule relies on a
property of all current Canon declarations, add a `CanonInvariantsTest` check that names the
declaration that violates it. That check guards future Canon edits; it is not a reason to model
imaginary cards now.

## Open questions

- **Q1. Horizon.** Is the end of the operation the right point at which to compare outcomes, or
  the end of the turn, or something else? The explorer needs an answer before stage 2 reports
  numbers.
- **Q2. Several Actors.** When P1 and P2 both have pending work, who owns the decision point, and
  is "P2 decides before P1's step" an observable difference? R2 says decisions keep their owners.
  Treat such orders as distinct until this is settled. `CONCRETE`'s per-Actor counting does not
  address this.
- **Q3. Reveals in practice.** Which current commands are reveals: supplied card inputs, Admin's
  global-event gains, any command needing `AgentInputRequiredException` data? R3 needs a list.
- **Q4. Admin's identity.** Settle D1 against IDENTITY.md, and decide whether Admin keeps an Agent
  at all once it holds no decisions.
- **Q5. Defaults.** Which policy should the REPL and `Agents(world)` give Players once pruners
  exist? D5 points toward pruners only, with preferences opt-in.

## Source map

- [`AutoExecPolicy.kt`](../../src/common/dev/martianzoo/agent/AutoExecPolicy.kt) and
  [`AutoExecLoop.kt`](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt): the current enum and
  loop. `actOnce` makes choices; it does not prove safety.
- [`Agents.kt`](../../src/common/dev/martianzoo/agent/Agents.kt) and
  [`AgentImpl.kt`](../../src/common/dev/martianzoo/agent/AgentImpl.kt): initial policies, the
  override stack, and the operation completion check (`requireComplete` with `lastDecision`).
- [`TaskForm.kt`](../../src/common/dev/martianzoo/agent/TaskForm.kt): narrowing edges
  (`decisions`, `options`).
- [`ActorEngine.kt`](../../src/common/dev/martianzoo/engine/ActorEngine.kt): `canSelectTask` and
  `canExecuteTask` probes, `prepareTaskNarrowing`, `enforceSelectLock`, `requireComplete`.
- [`Engine.kt`](../../src/common/dev/martianzoo/engine/Engine.kt): `Engine.fork`.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt): `resolve` and
  `executeResolved` separate current-state simplification from execution.
- [`SafeAutoExecTest.kt`](../../test/common/dev/martianzoo/agent/SafeAutoExecTest.kt): current
  policy scenarios. Its name does not establish safety for arbitrary compositions.
- [`AutoCommand.kt`](../../src/common/dev/martianzoo/tfm/script/commands/AutoCommand.kt): the REPL's
  policy switch and its help text.
