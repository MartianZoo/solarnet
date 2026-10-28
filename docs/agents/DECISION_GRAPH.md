# Single-Action Decision Graph

> **Agent design — updated October 24, 2026; implementation remains exploratory.**
> **Selected** records direction agreed with the
> project owner. **Candidate** describes a strategy whose proof and implementation still need
> review. **Open** identifies an unsettled question. Current-code evidence does not establish
> that the proposed behavior is implemented.
>
> [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md) owns engine transactions, task priorities, and
> mandatory Admin handling. This document owns agent decision decomposition, completion analysis,
> pruning, collapsing, and automatic walking.
> The [Agent contract](AGENT_CONTRACT.md#agent-responsibilities-four-buckets) owns the broader
> responsibility inventory and the separate standing-policy direction.

This is a separate Agent project, not an acceptance checklist for the current
[engine API transition](ENGINE_TRANSITION_PLAN.md). During that transition, Agent only needs to
carry over useful behavior removed from engine so existing clients keep working.

## Purpose and governing rule

**Selected:** never close off an option that a Player might have a legitimate reason to want.
A strategy may remove an option proved unable to complete the action, or replace alternatives
proved to have the same outcome. It may not discard an alternative because it seems inefficient,
unlikely, strategically inferior, or expensive to analyze. An unknown result is not permission
to remove an option.

**Selected:** the first practical requirement is a finite set of candidate options. An abstract
task's other choices can be enumerable while an unbounded `X` prevents finite enumeration.
A finite candidate set may still contain choices that cannot complete; obtaining that set is
useful before those choices can be filtered further. This is a practical concern: the owner
reported a recent UI incident that attempted to copy the entire integer domain into a List.

A stronger goal is to prevent a client from starting down a path that later forces it to back
out. That is the goal of completion analysis, not an unconditional guarantee when information or
computation is insufficient. In those cases, preserve the option and report completion as unknown.

The agent also removes questions whose answers cannot affect the outcome. This helps the Player,
but it also reduces the work needed to analyze the remaining decisions. Simplification is part of
making completion analysis practical, not merely presentation work after analysis has finished.

## Division of responsibility

**Selected destination, October 24, 2026; not yet implemented in full:** the engine interprets an
explicit request within an identified permitting task. It combines the task and submitted input,
and can examine the entire requested transaction to establish that exactly a single concrete
narrowing succeeds. It does not execute subsequently queued tasks to decide this, including Admin
work. Task finding itself may remain a simple descriptive comparison, without trying consequences
to choose between different tasks. [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md#execute-the-clients-intended-request)
owns the precise rule and remaining implementation questions.

The agent answers different questions: what to ask when the client has not supplied a complete
request, whether to execute automatically, and whether choices can finish the later action. It can
reduce an unanswered menu partway, compose partial deductions, and investigate later tasks. The
engine's successful transaction is the beginning of that later analysis, not proof of completion.
The agent must not duplicate the engine's legality rules in a separate game interpreter.

The earlier plan to move every unique-subtype or present-dependency gain deduction into the agent
is superseded. Deductions used to interpret an explicit execution request can remain in the engine;
unsolicited narrowing and authorization of automatic execution are agent policy. An installed
policy can let the engine finish a covered selected task without consulting Agent again; this does
not move completion analysis into engine. Partial reductions remain useful even when they do not
immediately permit execution. Voluntarily retained engine narrowing remains
open because supplied information can unlock both further narrowing and evaluation.

The [THEN rule](SEQUENCING.md#reviewed-client-view-of-then) requires explicit shared bindings before
exposing the separate current instruction and continuation. Do not reconstruct X backward from a
submitted first-stage result in either module. On the experimental branch, Agent translates an
explicitly submitted resource total into individual engine calls; production tests use this for
their three money sources, while unrelated rewards stay separate. See
[the contract](ENGINE_CONTRACT.md#task-identity-optional-queries-and-conveniences--items-10-1315-and-17).
Optional transaction trials and quantity-limit reads are reasonable engine services; neither
certifies completion of later work.

Admin remains permanently automatic in the agent, through separate engine transactions. Its
scheduling follows [ENGINE_CONTRACT.md](ENGINE_CONTRACT.md#admins-choices-and-retries). Player
policy settings never disable Admin. Agent exploration must include the Admin transactions and
installed Player-policy execution that actual play would perform. Engine uniqueness deliberately
stops before subsequent task execution. Outcome comparison below also includes settled Admin work
and therefore must not be reused as the engine's
rule for accepting an ambiguous request.

[ENGINE_TRANSITION_PLAN.md](ENGINE_TRANSITION_PLAN.md) describes the autonomous experiment sequence.
The remainder of this document governs agent exploration, not a requirement to build the whole
search system before trying the engine changes.

## What the graph describes

**Selected:** the Single-Action Decision Graph (SADG) describes decisions and resulting states
involved in completing an action. An action can require several tasks, several calls, and decisions
by several Actors. An empty queue for its initiating Player does not establish completion.

The graph is implicit. We can reason about its nodes and edges without constructing or retaining
the entire graph. No graph storage model, cache, or new engine representation is selected.

A node can describe a pending decision within a task draft or an execution state with tasks left
to handle. Edges include task selection, individual narrowings, and execution with its consequences.
A view may combine several such steps for presentation or analysis, provided it preserves the
decisions that can still matter.

The selected guided decision order is:

| Kind | Question |
| --- | --- |
| TASK | Which available task should be handled next? |
| OR | Which alternative within that task? |
| TYPE | Which class, then which concrete dependencies of that class? |
| NUMBER | Which value for `X` or an optional `?` amount? |
| CARD | Which actual card identities are required? |

A class choice can reveal further dependency choices. Guided prompts should avoid representing
different question orders as different game choices. Clients may still submit a known narrowing
directly; they need not walk every prompt or enumerate its options.

Naming a played card's class can be a TYPE decision. Naming anonymous draws, moves, purchases,
or discards is the CARD concern. The precise API composition remains to be designed.

## Layers of concern

These concerns have different proof requirements and cooperate throughout exploration:

1. **Finite candidates:** establish a finite set containing every potentially completing choice.
   This is already useful even if many candidates remain invalid or unanalyzed.
2. **Completion:** establish whether an action can finish from a candidate decision. Remove
   decisions proved to lead only to dead ends.
3. **Equivalent outcomes:** remove distinctions whose alternatives cannot change the result.
   This includes task orders proved equivalent, even when every order can complete.
4. **Cost of analysis:** avoid exploring equivalent paths repeatedly, and try inexpensive
   simplifications before expensive searches. Execution proved harmless can reduce the remaining
   problem before its full graph has been explored.
5. **Automatic walking:** when the reduced graph leaves no meaningful choice at the current node,
   advance without asking the Player.

These are not mandatory sequential processing phases. In particular, constructing the whole SADG
before applying reductions would defeat an important reason for having them. Pruning and
collapsing remain useful even when a client has disabled automatic walking.

## Completion and its three possible results

**Selected:** completion analysis uses `TRUE`, `FALSE`, and `UNKNOWN` throughout its decisions.

| Result | Meaning |
| --- | --- |
| TRUE | A continuation reaching action completion has been established. |
| FALSE | No continuation can complete, established by exhaustive analysis or a sound proof. |
| UNKNOWN | Neither conclusion has been established with the available information and work. |

For alternative continuations, combine results with ternary OR: a successful continuation proves
existence; all alternatives must be false to prove impossibility. An unexamined alternative remains
unknown. A successful branch does not turn its unknown siblings into false.

The ordinary ternary operations are:

| A | B | A OR B | A AND B |
| --- | --- | --- | --- |
| TRUE | TRUE | TRUE | TRUE |
| TRUE | FALSE | TRUE | FALSE |
| TRUE | UNKNOWN | TRUE | UNKNOWN |
| FALSE | FALSE | FALSE | FALSE |
| FALSE | UNKNOWN | UNKNOWN | FALSE |
| UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN |

The omitted reversed pairs have the same results. NOT exchanges TRUE and FALSE and leaves UNKNOWN
unchanged. Use these operations only for the propositions they describe. For example, evaluating
two tasks independently in the initial state and ANDing their results does not prove that their
sequence can complete: the first task may change what the second task can do.

A transaction succeeding proves only that step succeeded. A closed instruction, an executable
task, or a successful Admin pass does not by itself establish action completion.

### Other Players and the endpoint

**Selected:** a response owed by another Player is part of the action's analysis. If Stan's
greenery opens Kim's Philares choice, Kim still has a decision to make. Apply the same rule to
her alternatives: exclude a response proved to make completion impossible; retain responses that
can complete or remain unknown. This does not authorize Stan's agent to choose Kim's meaningful
answer. No Canon example of Philares dead-ending Stan's action has been established in this
discussion; it illustrates who owns the remaining decision.

**Proposal for endpoint recognition:** an action is complete when its obligations and consequences
have finished, including removal of its `ActionScope`, dependent cleanup, and work caused by that
removal. Continue automatic game progression to the next independent decision or game end.
A new action offer may already be pending, including the same Player's second action. Executing
that new action is outside the current SADG.

`ActionScope` is a promising Canon anchor, not an engine completion concept. Its removal alone is
insufficient because removal can enqueue work. Reaching `FINALLY` is earlier still. Canon's
[scope and Baton rules](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/base/game.pets)
also show why following every recursively caused task would absorb subsequent actions indefinitely.

**Open:** recognize the endpoint from existing facts, distinguish an outstanding response from
a new independent decision, and define the corresponding scope for setup, production, and other
interactions without `ActionScope`. Do not add engine task ancestry or completion machinery merely
to make this convenient. [Sequencing's current completion convention](SEQUENCING.md#operation-completion)
can accept earlier and assumes scope removal cannot require further action choices; reconciling
that convention remains future work.

### Sources of unknown

**Selected:** unavailable card information and exhausted computation can both yield UNKNOWN.
They have the same logical treatment. A diagnostic message can explain why analysis stopped;
the result does not need separate logical variants for different causes.

The initial inventory is:

- **An unpredicted card play.** The played card could lack an assigned identity at the start of
  speculation, so its consequences cannot be analyzed from that starting knowledge.
- **A work or time allowance is exhausted.** Some relevant alternatives remain unexamined.
  Record that analysis stopped; do not turn a timeout into a dead end.
- **Propagation from unresolved alternatives.** No successful continuation has been proved,
  and an unknown alternative remains.

**Open:** enumerate further genuine sources and choose work allowances and diagnostic details.
An unexpected implementation error is not a completion verdict and must not silently become
UNKNOWN or FALSE. Recognizing unfinished exploration does not require choosing a logging API here.

[Project Eden's declaration](../../pets/src/common/dev/martianzoo/tfm/canon/bundles/prelude2/cards.json5)
combines an ocean, a city, a greenery, and discarding three project cards. Location choices,
placement consequences, card choices, and task orders can multiply the search. It is a concrete
candidate for measuring search limits; this draft does not claim a timeout is inevitable.
[DeadEndsTest](../../test/common/dev/martianzoo/tfm/tests/rules/DeadEndsTest.kt), scenario
`Project Eden must reserve the only legal city space`, also demonstrates why blindly treating its
placements as independent would be wrong.

## Cards during speculation

**Selected:** speculative exploration accepts generic faceless cards. It must not demand invented
names for anonymous draws, moves, purchases, or discards. Actual execution supplies required card
identities. Identities already known when speculation begins remain known.

Stop analysis at a card play that could use a card without an assigned identity at the start of
that exploration. Mark its completability UNKNOWN; do not invent a name or continue through the
invented card's consequences. The criterion is starting knowledge, not current location: drawing
an anonymous card and then moving it to `Hand` does not make its play predictable.

Card handling should resemble ordinary narrowing to the pruning layer as far as possible.
Faceless speculation does not establish that discarding different known cards is equivalent.
It also does not excuse omitting identities needed to perform an actual move.

Merger motivated the unknown-play case. A separate fizzle alternative, discussed using WG Project,
may establish TRUE for the existence of a continuation without resolving an unknown card play.
That can end an existence search, but cannot justify hiding or walking past the unknown alternative.
These examples, and the expected correspondence with playing from `Selecting`, still need focused
verification; they do not select card-specific engine exceptions.

## Equality of outcomes

**Selected criterion:** compare alternatives from the same starting state after their transactions
and the consequent Admin transactions have settled. Outcomes are equal when their component deltas
are equal and their task changes are equal, ignoring ordinals. The order in which those changes
occurred does not matter.

This comparison applies both to complete continuations and to shorter alternatives such as AB
versus BA before the rest of the action. At the shorter comparison point, ordinary Player tasks
can remain pending; they are part of the comparison. Applicable Admin work must have settled.

Compare the resulting component and task differences from the common start, not the ordering of
events used to produce them. Task comparison retains meaningful instruction content, multiplicity,
assignee, selection, continuation, and deferral. Incidental task IDs derived from event ordinals
do not make outcomes different. The mechanical comparison of references and cause metadata still
needs specification; ignoring ordinals does not mean ignoring meaningful task content.

The actual route still has its actual recorded history. Equivalence for analysis does not rewrite
or suppress that history. Intermediate effects must first run: equal direct gains are insufficient
if their different orders produce different triggered work.

This rule compares execution outcomes. Two unfinished client drafts are not interchangeable merely
because neither has mutated the World; their supplied choices and remaining questions can differ.
Likewise, unknown card consequences cannot establish equality by their absence from a partial trace.

## Safe eager execution and automatic walking

This section concerns Agent decisions during an interaction. The separate
[standing-policy direction](AGENT_CONTRACT.md#4-configure-autoexec-policies-in-the-engine) can
authorize engine execution in advance for covered selected tasks; preference storage and policy
mechanics remain future work. That authorization does not prove other outcomes equivalent.

### Doing harmless work early

**Selected:** task C can be done eagerly when doing it now cannot close off any possible outcome.
It is insufficient to find a successful path starting with C. Every outcome otherwise available
must remain available after doing C first.

For independent tasks gaining a plant, a heat, and a steel, if every order produces the same
settled component and task changes, the Player need not choose an order. Their six permutations
can be represented by a chosen route. This also removes redundant exploration.

That conclusion requires accounting for consequences. Project Eden's city and greenery compete
for locations in the scenario above. It would be wrong to execute an arbitrary placement merely
because that placement's immediate transaction succeeds.

A proof that particular AB and BA executions converge permits collapsing those alternatives.
It does not automatically prove that A can move ahead of every other task, every narrowing of B,
or work spawned between them. General eager-execution proofs must cover the possibilities they
claim to preserve. Cheap sufficient proofs are valuable; full enumeration is not required when
a sound narrower proof establishes the same fact.

### Choosing a representative and choosing what to analyze

**Selected:** all else equal, use FIFO task order. A randomize option may vary the choice among
proved interchangeable orders. Neither FIFO nor randomization proves interchangeability or
licenses choosing among different meaningful outcomes.

If proving X safe is cheaper than proving Y safe, analyze X first. Once X is proved safe, doing it
early may simplify the problem and make Y easier to analyze. Search priority may therefore differ
from FIFO. This is a reason to prioritize a proof, not to execute X before obtaining it.
Admin scheduling continues to follow its separate contract.

### Walking the reduced graph

**Selected:** autoexec walks the sole remaining edge after justified pruning and collapsing.
Apart from alternatives proved equivalent, every competing path must be known uncompletable.
Finding the first successful path is not sufficient.

| Remaining alternatives | Consequence |
| --- | --- |
| TRUE and FALSE | The false alternative can be removed; the surviving step can be walked. |
| TRUE and UNKNOWN | Both remain. Success elsewhere does not remove the unknown choice. |
| UNKNOWN and UNKNOWN | Both remain unless equivalence is independently proved. |
| UNKNOWN and FALSE | The false alternative can be removed. The survivor is still unknown. |
| Several proved equivalent alternatives | Choose a representative under the ordering rules above. |

A sole unknown survivor does not prove completion. Walking never supplies missing card names or
other unknown required input: the agent must obtain that input when execution needs it.
A successful existence check also does not authorize running the entire witness path; later
nodes may still contain meaningful choices.

Collapsing equal destination nodes alone can leave several outgoing edges. Removing the order
question requires also representing their proved equivalent routes by the selected representative.
How that is expressed in code remains open.

## Initial strategy catalogue

The rules above constrain every strategy. This catalogue separates promising mechanisms so they
can be reviewed and measured individually. It does not claim the list is complete or select a new
representation.

### 1. Bound X to a finite candidate range

**Candidate for exploration:** investigate whether the fully valid, action-completing values of
`X` always form a contiguous integer range in Canon. The stronger hypothesis for printed game
content is that the set is empty or `1..max`; payment allocations may instead require `min..max`.
These concern completion of the whole action, not merely immediate resource availability.

Under the `1..max` hypothesis, proving that `X = x` cannot complete immediately gives the finite
candidate range `1..x-1`. It need not be the exact valid range: smaller candidates may still fail.
Finding any such upper limit therefore solves the initial enumeration problem without first
finding the actual maximum or classifying every interior amount.

For payment ranges, a failing value below the minimum gives no upper limit. A known completing
value `y` followed by a proved failing value `x > y` does establish the upper limit `x-1` under
contiguity. A sound resource bound can also make candidates finite without a completion search.

Once the exact endpoints are known, the interval describes every valid amount; proved completable
endpoints also establish intervening values without individual searches. Validate the hypotheses
against Canon interactions before adopting them. UNKNOWN is not failure, and a work cutoff is not
an upper limit. If no finite bound has been established, do not materialize the entire integer
domain as a collection.

### 2. Canonical prompt order

**Selected direction:** ask TASK, OR, TYPE, NUMBER, CARD in the guided order above, with class
before its dependencies. Avoid exploring permutations that merely supply the same choices in
different question orders. The proof obligation is that the guided path still permits every
complete narrowing; later questions revealed by earlier answers must remain reachable.
This reduces drafting paths without executing tasks.

### 3. Reject an immediately impossible edge

**Candidate:** use a conclusive check or speculative transaction to reject a particular submitted
step that cannot succeed. Include immediate effects and final checks where they can affect the
verdict. This can avoid exploring descendants of a rejected transaction.

Failure now does not establish that the task can never be performed. Another task might supply
its resources first. Rejecting “do A now” must retain “do B, then A” when that remains possible.
Incomplete capacity checks and syntactic concreteness are not full transaction proofs.

### 4. Search for completion or prove its absence

**Selected objective; search mechanism open:** explore continuations until a successful endpoint
is found, all continuations are proved impossible, or analysis returns UNKNOWN. A successful
witness permits an early TRUE answer to the existence question. FALSE requires covering every
alternative, directly or through sound reductions.

This is the principal protection against delayed dead ends. It can be expensive, so apply the
other strategies during the search. A witness alone cannot classify all sibling choices or
authorize automatic walking.

### 5. Merge equal settled outcomes

**Selected criterion; comparison mechanism open:** apply the component-delta/task-change rule
above to alternative executions and share their remaining analysis. Keep a representative route
when the distinction itself can be removed. This saves duplicate continuation work without
discarding different outcomes.

The comparison must include Admin consequences and pending task differences. Equal resource
totals, equal immediate instructions, or different unknown traces are not sufficient evidence.

### 6. Prove an order interchangeable and execute safe work early

**Selected objective; sufficient proofs are candidates:** show that moving a task ahead preserves
all possible outcomes. A local AB/BA comparison can establish a particular commuting pair;
a general rule must also cover relevant choices and intervening work.

This can remove factorial growth from task permutations and reduce the state still needing
analysis. Different resource names are not a proof of independence: effects, gates, payments,
limits, and spawned tasks can connect them. Enumerating the useful cheap proofs is central future
work, starting with concrete Canon scenarios.

### 7. Resolve a forced narrowing

**Selected direction; enumeration mechanism open:** supply a choice when every competing answer is
proved impossible, or all retained answers are proved equivalent. This includes choosing a forced
type, dependency, or amount, as well as a task order. It reduces both question count and search.

An incomplete option list cannot prove uniqueness. A sole successful answer plus unexamined or
unknown answers is insufficient. Review which existing engine deductions establish these facts
and can be reused by Player and Admin agents. The engine transaction rule is the
starting point; deciding to supply an answer or execute without a client request is agent policy.

### 8. Reuse established analysis

**Candidate:** when exploration reaches the same relevant decision state again, reuse established
completion or equivalence results instead of repeating the work. This can be useful even without
retaining the whole graph.

Correct reuse must account for pending choices and the information available to the search, not
just component totals. UNKNOWN from exhausted work is not proof that further analysis is pointless.
No cache, persistent state key, or invalidation protocol is selected by this draft.

### 9. Prioritize cheap proofs and bound expensive exploration

**Selected policy; scheduling mechanism open:** try inexpensive, promising reductions first.
A cheap proof that C is safe may remove many orders before a harder search begins. Stop when the
configured allowance is exhausted and propagate UNKNOWN, with a diagnostic explaining the stop.

This controls work without changing what TRUE and FALSE mean. Record search work and reductions
on actual scenarios before claiming a performance improvement. Proof prioritization must not
turn “the first answer found” into “the only answer the Player may choose.”

### 10. Simplify state-dependent instructions early

**Candidate:** if a gate is true and no possible remaining work before its instruction executes
can make it false, the agent can reason over the body without the gate, even if the engine's
stored task retains it. Include consequent Admin work in that proof.

The same idea can apply to `PER` multipliers and AMAP amounts when their evaluated results are
guaranteed to stay the same. Those proofs are more involved: a quantity moving only in a particular
direction need not remain constant. Settling gates, multipliers, or amounts early could make other
pruning strategies cheaper. The classification and proof rules remain to be developed.

For each strategy's next revision, specify its exact applicability, proof obligation, preserved
outcomes, interaction with UNKNOWN, useful example, counterexample, and measured analysis cost.
Overlapping strategies may compose; they should not grow separate representations of the same facts.

## Current evidence and next work

The current implementation is not this design:

- [TaskForm](../../src/common/dev/martianzoo/agent/TaskForm.kt) exposes ALTERNATIVE, TARGET, and
  AMOUNT decisions. Task selection precedes the form, and
  [CardTracking](../../src/common/dev/martianzoo/tfm/engine/CardTracking.kt) handles card identities.
  Direct draft narrowing is also available.
- [Instructor](../../src/common/dev/martianzoo/engine/Instructor.kt) and
  [ActorEngine](../../src/common/dev/martianzoo/engine/ActorEngine.kt) currently perform some forced
  deductions inside the engine. [TASK_HANDLING.md](TASK_HANDLING.md) maps them for review.
- [AutoExecPolicy](../../src/common/dev/martianzoo/agent/AutoExecPolicy.kt) and
  [AutoExecLoop](../../src/common/dev/martianzoo/agent/AutoExecLoop.kt) use existing task policies.
  EAGER can try competing tasks in queue order without proving outcome equivalence. That behavior
  does not implement the Player walking rule above.
- [DeadEndsTest](../../test/common/dev/martianzoo/tfm/tests/rules/DeadEndsTest.kt) supplies Canon
  scenarios for delayed failure, including Project Eden's competing placements and Old Mining
  Colony needing Pluto's cards. These characterize game consequences; they do not prove a pruner
  detects those failures in advance.

[TODO.md](../../TODO.md) tracks follow-up. Priorities are to refine the strategy catalogue, settle
endpoint recognition and task comparison, inventory deductions by owner, and measure representative
searches. Preserve the distinction between a selected semantic rule and an unverified shortcut.
