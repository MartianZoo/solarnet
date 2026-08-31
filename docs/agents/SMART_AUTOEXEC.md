# Proof-preserving autoexecution

> **Read when:** defining what makes an automatic task command safe, designing a smart policy,
> proving task independence or confluence, or compiling Catalog facts for autoexecution.
>
> **Skip when:** changing where policies run or how they call `Agent`; those mechanism questions
> belong in [AUTOEXEC.md](AUTOEXEC.md).
>
> **Status:** research and proposed proof rules. None of the candidate analyses is committed
> behavior.

## Source map

- [`AutoExecPolicies.kt`](../../src/common/dev/martianzoo/engine/AutoExecPolicies.kt) and
  [`Implementations.kt`](../../src/common/dev/martianzoo/engine/Implementations.kt) — inspect the
  current conservative policy, selection probes, and locking semantics.
- [`Instructor.kt`](../../src/common/dev/martianzoo/engine/Instructor.kt) — search
  for `resolve` and `executeResolved` for state reads, execution, and effect creation.
- [`Effector.kt`](../../src/common/dev/martianzoo/engine/Effector.kt) — search for
  `candidatesFor` and `registryOrder` before defining EGS equality.
- [`Task.kt`](../../src/common/dev/martianzoo/pets/data/Task.kt) and
  [`TaskQueue.kt`](../../src/common/dev/martianzoo/engine/TaskQueue.kt) — inspect
  selected state, continuations, causes, normalization, and id-only ordering.

## Read only the relevant sections

| Question | Read |
| --- | --- |
| What does “safe” mean? | Mission; Formal contract; EGS equality obligations |
| Are the proposed starter rules sound? | Assessment of the initial rules |
| Which proofs should be built first? | Measure first; Working proof rules |
| Which proofs are plausible but unearned? | Deferred proof rules |
| What can be compiled from a Catalog? | Catalog and premise analysis |
| How can this become fast and trustworthy? | Performance shape; Validation strategy |

## Mission

A supplied smart policy should perform a task command only when it proves that the command removes
no reachable gameplay outcome and changes no Player's control over a decision. This is the right
foundation for a powerful default: fidelity may create many independent pieces of pending work
without forcing clients to resolve meaningless orders by hand.

The word *proves* is essential. A successful speculative execution, a pattern that held throughout
one replay, or the absence of a known counterexample yields `UNKNOWN`, not permission to act. The
policy may be incomplete. It must be sound.

Solarnet still needs its own theorem. Its transition vocabulary is reconstructed dynamically from
task schemas and legal narrowings, and enabledness depends on the component graph and task pool.
After that reconstruction, Antti Valmari's
[“Stubborn Sets for Reduced State Space Generation”](https://ai.dmi.unibas.ch/research/reading_group/valmari-apn1989.pdf)
supplies the directly useful lemma: a terminating execution can be permuted to begin with a
suitable transition while reaching the same terminal state. The work was presented at APN 1989 and
published in LNCS 483 in 1991. Its terminal-state result is the useful lineage here; its separate
nontermination result is outside the first contract.

## Formal contract

Fix one Game Premise and let `S` be a complete semantic engine state. Let `S →a S'` mean that an
ordinary legal, policy-free task command `a` changes `S` to `S'`. The atomic transition includes
resolution and execution work performed by that command, every inline automatic effect, and the
creation or removal of queued tasks. It does not include a subsequent policy drain. Rules-bypassing
commands such as god-mode task deletion are outside the relation.

Let `S ≈ T` mean that the two states have the same *effective game state* (EGS). For a set `P` of
tasks allowed to predate and outlive the operation, define a successful boundary as:

```text
all pending tasks are in P  and  MAX 0 Temporary
```

Whole-World idleness is the special case `P = ∅`. Let `N(S)` be the set of EGS classes reachable at
that boundary while resolving the current operation. Queue clear alone is insufficient:
`Implementations.complete` also rejects surviving `Temporary` components such as `WildTagUse`.
If later end-of-turn play depends only on boundary EGS, equality there preserves end-of-turn
outcomes too.

A legal command `a` is *outcome-safe* at `S` exactly when:

```text
N(S) = N(a(S))
```

Because `a` is itself an ordinary legal command, `N(a(S)) ⊆ N(S)` follows automatically. Every
proof rule therefore has one real obligation: show `N(S) ⊆ N(a(S))`. Informally, every successful
schedule that does something else first must have an equivalent schedule beginning with `a`.

`N` contains successful boundary states only. A dead end is a failed execution of the enclosing
atomic operation, which rolls back so another branch can be tried; it is not an outcome made safe
to commit by disappearing from `N`. This definition does not preserve event traces, the number of
schedules reaching one outcome, nontermination, probabilities, or observations. Add those to the
contract before game mechanics make them relevant.

Outcome preservation is necessary but not sufficient for a default policy. A proof witness must be
**agency-preserving**: corresponding non-forced commands remain assigned to the same assignees, and
the policy may issue `a` only with authority for its assignee. Otherwise `N` can remain equal while
the policy steals or reallocates a Player's decision. The first implementation should therefore
handle forced commands for an authorized assignee and single-assignee pools; cross-assignee ordering
needs an explicit agency-preserving certificate. Call a command *policy-safe* only when it is both
outcome-safe and agency-preserving.

Every accepted action invalidates the proof. The driver must read the new state and prove the next
command independently.

## EGS equality obligations

At a successful boundary, EGS equality must determine all later gameplay relevant to the chosen
end-of-turn contract. For memoization and proof replay, the exact obligation is:

```text
S ≈ T  implies  N(S) = N(T)
```

A merely lossy projection does not establish this. The intended omissions have narrower existing
arguments: components have no instance identity and equal Types are indistinguishable copies
([ENGINE.md](ENGINE.md#component-graph)); game mechanics may not read event history
([AUTOEXEC.md](AUTOEXEC.md#semantic-equivalence)); and opaque task ids and ordinals may be
alpha-renamed only through the semantic-symmetry rule below. At minimum the candidate EGS contains:

- the exact component multiset, including dependencies and ownership;
- the unordered task multiset, including instruction, continuation, assignee, Actor, and selected
  status; and
- cause context plus the equivalence relation describing which tasks share one cause, while the
  numeric event ordinals and task ids may be alpha-renamed.

`whyPending`, event text, policy credit, and raw ordinals may remain diagnostic only. Cause context
cannot be dropped today: `TfmGameplay` searches it and sometimes groups tasks by exact cause.

There is a current engine defect behind `registryOrder`. `Effector.candidatesFor` uses it because
earlier automatic effects may change what later effects observe, while
[SEQUENCING.md](SEQUENCING.md#use-automatic-effects-to-preserve-player-visible-invariants) says
gameplay must not rely on registration order between automatic effects. Removing the last copy of
an effect-bearing component deletes its ordinal; rollback re-adds it with a new ordinal because
`nextRegistryOrder` is monotonic and is not timeline state. Speculation on the live World therefore
changes later gameplay even when it records no lasting events. The same defect affects ordinary
failed operations and rollback-backed task-selection probes.

Do not enshrine that order in EGS. Make automatic-effect sequencing semantic or canonical as owned
by `SEQUENCING.md`, and require every speculative proof to use a disposable World overlay rather
than checkpoint-and-restore on the live World. Until that defect is fixed, component/task equality
cannot prove future equivalence.

More generally, event history can be excluded only while game mechanics and custom code cannot read
it. Derived indexes, caches, random-generator state, workflow fields, and future hidden-information
state must likewise be derived from EGS or included in it when they affect legal transitions.

## Assessment of the initial rules

### Empty queue

If there are no tasks, doing nothing is safe. This is the inert driver case, not an automatic
gameplay command.

### One task

If exactly one task exists, selecting and resolving it is safe only after establishing an engine
lemma: resolution in an unchanged World preserves every legal narrowing and continuation of that
task. Resolution may prune an `OR`, evaluate `PER`, apply limits, translate a custom Class, split a
group, reduce to `Ok`, and enqueue `THEN`; uniqueness of the queue entry alone proves none of those
transformations.

The rule becomes sound when:

1. queue resolution admits no other semantic state mutation before that task;
2. resolution succeeds; and
3. every pre-resolution completion has an equivalent post-resolution completion.

This lemma is worth making a direct engine contract and testing independently. Once it holds, every
successful-resolution path must pass through the selected representation.

### Selected concrete task

Executing a selected concrete task is safe if ordinary commands respect `enforceSelectLock` and
*concrete* means there is no remaining non-equivalent legal narrowing. `sneak` bypasses the lock but
is already outside the formal relation. The proof should cite the ordinary-command guard rather
than treat selection as an unconditional global property.

### Unique concrete narrowing

Narrowing a selected task to its only viable concrete narrowing is safe if the candidate enumeration
is complete modulo EGS equivalence. “Viable” means capable of participating in a completion, not
merely accepted by `Instruction.narrows`. A syntactically valid narrowing may immediately or later
reach a dead end.

This is stronger and more useful in the following form. If `r` is a valid partial narrowing and
every viable concrete completion of the current task also narrows `r`, applying `r` is safe. It can
bind a forced target, quantity, Actor, linked type, or `OR` arm while leaving unrelated choices
open. Unique concrete narrowing is the special case where `r` is already concrete.

### Two concrete tasks with equal immediate orders

This rule is not sound as stated. Suppose executing `A` creates queued task `C`; `C` gains `Switch`;
and `B` gains `Marker IF Switch`, or evaluates a `PER` metric changed by `C`. The immediate orders
`A,B` and `B,A` can have identical components with `C` pending, while legal order `A,C,B` reaches a
different successful boundary. Checking only whether the original tasks are concrete does not
screen this out: it resolves each original task only in the initial state and never explores the
created task.

The comparison becomes sound under either repair:

- **Closed batch:** both orders succeed, no new interleavable task is created before the original
  batch is exhausted, and the resulting EGS is equal; or
- **Persistent action:** the selected task is proved to commute with every action, including newly
  created work, that could precede it in a completion-reaching schedule.

Inline automatic effects are part of an execution transition and do not violate closedness, but all
of their component changes must participate in the equivalence comparison. Queued effects and
`THEN` continuations do violate closedness unless a further proof accounts for them.

### Preservation of every successful boundary EGS

The general claim is the definition of safety, subject to four qualifications: the EGS relation
must satisfy the equality obligation above, the selected command must be an ordinary legal command,
all relevant completion paths must be covered, and the witness must preserve agency. Under those
conditions the reverse inclusion follows because selecting `A` first was already one of the
original paths.

## Measure before expanding

Changing the default to `SAFE` left 21 of 54 script tests unfinished because independent
consequences commonly coexist ([AUTOEXEC.md](AUTOEXEC.md#first-implemented-split)). That establishes
need, but not which ambitious proof rule will pay for itself.

Before implementing a deferred rule, run a diagnostic-only policy over the replay suites and record
the queue shape plus the first failed proof premise: remaining choice, multiple assignees, possible
trigger, shared read/write domain, continuation, created task family, or proof bound. Report the
working rule that succeeds and the `UNKNOWN` reason when none does. The resulting histogram is the
gate for the deferred section.

## Working proof rules

Each rule should return `PROVEN`, `DISPROVEN`, or `UNKNOWN` plus a compact certificate tied to the
exact `WorldRevision`, not an event-count checkpoint. `UNKNOWN` does nothing. Every certificate also
checks the agency obligation.

### 1. Semantic stutter

If `a(S) ≈ S`, `a` is safe. This handles diagnostic-only changes and any task normalization that
can truly be shown to leave `N` unchanged. Equality of visible resources is not enough.

### 2. One viable successor class

If complete analysis finds one immediate successor EGS class among all successful-boundary-reaching
legal commands, any authorized command reaching that class is safe. This subsumes forced execution
and unique concrete narrowing. Several syntactic commands may belong to the same class.

### 3. Common forced narrowing

For a selected task `t`, let `C` be all viable concrete refinements. A valid narrowing `r` is safe
when every `c` in `C` narrows `r` and the narrowing relation is transitive over these forms. Compute
the strongest inexpensive common facts rather than demand that `C` contain one element. Examples
include a singleton concrete subtype while quantity remains optional, or one live `OR` arm whose
own target is still abstract.

For an unselected task, the same rule needs a stability proof: no legal preceding action can make a
discarded refinement viable or invalidate the common fact. Catalog write summaries can sometimes
prove that the relevant type domain, metric, gate, and limits cannot change.

### 4. Sole semantic progress

If `a` is the only legal command that can change EGS, it is safe. The analysis must include task
narrowing and decline commands, not only tasks that can be selected as written. A blocked task may
have a viable explicit narrowing, so “only one task is selectable” is not this proof.

A useful extension is a necessary-enabler certificate: every successful completion must perform
`a` before any other semantic transition can become enabled.

### 5. Semantic symmetry

If an automorphism exchanges tasks `A` and `B` while preserving EGS, assignee, Actor authority,
continuations, and cause grouping, they are interchangeable. Renaming opaque task ids is the basic
case. Equal instruction text is not enough.

### 6. Closed trigger-free batch

For a finite same-assignee batch of concrete deterministic tasks, prove that each task executes
exactly once, every order remains legal, no task or continuation is emitted, and the Catalog/premise
closure proves that none of the changes can trigger an effect or alter a live effect subscription.
Then pairwise commutation in every batch-reachable state connects all permutations, so any order is
safe. A direct two-order simulation is the two-task instance, but it must run in disposable Worlds.

### 7. Certified additive accumulator

A useful instance of rule 6 is exact mandatory gains into a commutative component multiset. It is
safe to reorder when the combined gains satisfy every limit in every order, neither gain changes
anything read by the other, no removal or dependency cascade occurs, no effect or continuation is
emitted, and live-effect availability cannot change.

## Deferred proof rules

These rules are mathematically plausible but add permanent machinery. Implement one only when the
coverage histogram shows a material queue family that the working rules cannot handle.

### Singleton-enabled stubborn closure

Construct a set of transition families `T` around a proposed action. Close disabled members over
sufficient enabling families and enabled members over actions that can spoil enabledness or prevent
leftward permutation. If the Solarnet-specific stubborn-set premises hold and `T` has exactly one
enabled EGS successor class, that successor is safe. Disabled task/effect families account for work
created later rather than only instances currently queued.

A stubborn set with two enabled non-equivalent actions proves only that search may be restricted to
those actions; it does not authorize either live. This is the useful specialization of Valmari's
terminal-state theorem.

### Persistent action

Choose action `a`. At every state `U` reachable without `a` while it remains pending, and for every
next action `b` on a successful-completion path, prove that `a` remains enabled, `b` remains enabled
after `a`, and `a(b(U)) ≈ b(a(U))`. Adjacent swaps then move `a` to the front. Checking only tasks
present at `S` misses created work; state-local commutation is not this proof.

### Broader closed or confluent regions

A closed deterministic batch may allow automatic closures or later branch joining, but then it
needs a termination measure and joinability proof over the full created-work region. Newman's
original [termination-plus-local-confluence result](https://doi.org/10.2307/1968867) applies only
after those hard premises are established. Prefer direct terminal-outcome comparison unless the
coverage data demonstrates reusable structure.

If bounded proof search is eventually justified, Cormac Flanagan and Patrice Godefroid's
[DPOR independence conditions](https://patricegodefroid.github.io/public_psfiles/popl2005.pdf)
provide the right warning: independent actions must preserve enabledness as well as commute. Such
search belongs in disposable Worlds and accelerates a proof; it never authorizes a command by
itself.

## Catalog and premise analysis

Catalog analysis should compile conservative *may* summaries and exact structural facts once, then
specialize them to a Game Premise and finally to the current World. The existing compiled Class
hierarchy and effect subscription index provide much of the raw structure.

This use of over-approximation has direct precedent. Valmari's
[variable/transition framework](https://ai.dmi.unibas.ch/research/reading_group/valmari-apn1989.pdf)
defines assigned test, read, and write sets by the semantic properties they must satisfy and
explicitly allows them to be larger than the smallest such sets so they remain practical to
compute. A false conflict costs automation; an omitted possible interaction invalidates the proof.

For each instruction/effect family, a useful summary contains:

- component domains it may read through gates, metrics, auto-narrowing, limits, dependencies,
  properties, and custom resolution;
- components it may gain, remove, or transitively remove as dependents;
- change events it may emit, with Actor constraints;
- automatic and queued effects those events may fire, including inherited and self effects;
- task schemas and continuations it may enqueue;
- effect-bearing components it may add or remove, changing future subscriptions; and
- an opacity marker for custom Classes or transforms lacking an audited summary.

The analysis is a fixed point over automatic-effect output. A cycle or unknown custom behavior
widens to `UNKNOWN`; it must not be truncated and labeled safe. Premise projection can remove
inactive types and impossible subscriptions. Current-World analysis can remove effects whose source
component is absent only when no preceding action in the proof region can add such a source.

Two actions have a static independence certificate when their write closures cannot intersect the
other's read, write, queue-emission, dependency, or subscription-changing closures, and both remain
enabled. Type intersection must use the active subtype/dependency model rather than Class-name
equality.

`a(b(S)) ≈ b(a(S))` at one state establishes only conditional independence. Shmuel Katz and Doron
Peled's [conditional-trace semantics](https://doi.org/10.1016/0304-3975(92)90054-J) (1992)
formalized that actions may commute in one context and conflict in another, with uniformity needed
to reuse that fact across successors. Elvira Albert et al.'s
[constrained DPOR](https://www.cs.upc.edu/~albert/papers/cav18.pdf) (2018) gives a concrete example
where two events commute initially but become dependent after a third event, and develops a weaker
transitive-uniformity condition. For autoexecution, the corresponding obligation is the quantified
frontier in the deferred persistent-action rule: the independence predicate itself must remain true
across every prefix over which the selected action is moved.

### What a no-trigger fact proves

“No effect can trigger on creation of `A` or `B`” is valuable: if it covers inherited effects,
self effects, active live effects, trigger transforms, Actor/owner bindings, and queued as well as
automatic effects, it proves those change events emit no effect work.

It does not by itself prove that `A` and `B` commute. They may share a cap, consume or enable a
dependency, alter a `PER` metric or gate, delete dependents, or install an effect that observes the
other event. No-trigger plus disjoint read/write/dependency/subscription footprints and no
continuations can prove the desired closed-batch rule.

Catalog-wide absence is the strongest and cheapest certificate. Premise-wide absence is often more
useful because inactive expansions disappear. A state-local absence is valid only with a stability
argument over the schedules being collapsed.

## Performance shape

The working policy should spend proof effort in this order:

1. constant-time locks and already-compiled forced facts;
2. queue-local unique-successor and common-narrowing checks;
3. Catalog/premise symmetry, trigger-free, and footprint certificates.

Stop at the first proof. Never fall through to stable task order. Cache immutable Catalog summaries
by Catalog identity, premise summaries by active Class table, and dynamic certificates by EGS
revision plus the exact state slices they depend on. An accepted command invalidates dynamic
analysis. Only measured need should add pairwise disposable-World diamonds or bounded frontier
search with memoization.

The trusted proof kernel should be smaller than the analyzer. The analyzer proposes a certificate;
the kernel checks subtype disjointness, footprint closure, enabledness, or compared successor EGS.
This limits the damage from an optimization bug in a large Catalog compiler.

## Research boundary

The retained sources have narrow jobs. Valmari supplies the terminal-state permutation result and
licenses conservative read/test/write summaries. Katz–Peled and Albert et al. expose the trap in
reusing state-dependent commutation. Flanagan–Godefroid and Newman appear only beside deferred rules
whose implementation is measurement-gated. The working contract and forced-narrowing rules stand
on their Solarnet-specific proofs.

Work aimed primarily at preserving temporal logic, fairness, races, or trace counts is outside the
first policy contract. Revisit it only if nontermination, observations, chance, or history become
outcomes that autoexecution must preserve.

## Failure modes for bounded permutation proofs

A proof that resolves every current task as-is, compares only their immediate permutations, and
replays one accepted order is not sound when:

- it does not explore interleavings with tasks created by the batch, so the concrete `IF`/`PER`
  form of the `A,C,B` counterexample above can pass an immediate-order comparison;
- component/task comparison omits live-effect registry order, and rollback-backed speculation can
  itself mutate that order before evaluating the next candidate;
- one immediate concrete form per original task does not cover legal narrowings; and
- composing the proof with another policy inherits every unproved assumption in that policy.

The registry defect already has a two-task witness. Let `W1` own automatic effect `A:: Flag` and
`W2` own `A:: Marker / Flag`, with one of each present. Tasks `-W1, W1` and `W1, -W1` reach the same
component multiset and empty task pool, so an immediate component/task comparator treats them as
equivalent. The first order drops and recreates `W1`'s registry ordinal behind `W2`; the second never
drops it. A later `A` therefore produces different `Marker` results.

Resolution used by analysis must remain read-only or run in a disposable World; otherwise the act
of proving can itself change later gameplay.

The size bound is a performance limitation, not the soundness defect. A bounded proof is welcome
when exceeding the bound returns `UNKNOWN`. The defect is claiming proof after omitting reachable
interleavings or gameplay-relevant state.

## Validation strategy

Tests cannot establish the theorem, but they can test that the implementation satisfies the stated
premises and expose unsound certificates.

- Build a tiny bounded state explorer for synthetic Catalogs. For every policy proposal, enumerate
  successful-boundary EGS before and after the command and assert equality plus agency preservation.
- Include adversarial cases for spawned tasks, effects installed by earlier gains, shared caps,
  gates, `PER`, dependency cascades, custom Classes, Actor differences, cause grouping, and `THEN`.
- Run queue enumeration forward, reverse, and reproducibly shuffled so order never hides a proof
  gap.
- Generate minimal counterexamples when a certificate fails against the explorer.
- Produce a Catalog coverage report: proven rule, proof cost, and `UNKNOWN` reason for each
  encountered queue shape. Replays measure usefulness; they do not weaken a rule.
- Benchmark policy invocations, footprint checks, overlay branches, memo hits, and total time at one
  application command boundary.

The first milestone is diagnostic coverage plus a small proof kernel for selected-concrete and
common-forced-narrowing commands. The first ordering milestone is the same-assignee, closed,
trigger-free batch. Let coverage data, not theoretical reach, decide whether a persistent-action or
bounded-search implementation follows.
