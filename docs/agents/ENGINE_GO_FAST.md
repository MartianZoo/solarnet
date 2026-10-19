# Engine performance research

> **Agent information:** Research note through 2026-10-18. Measurements below describe their
> stated workloads; proposed structures are hypotheses, not selected architecture. Read
> [ENGINE.md](ENGINE.md) for current behavior and [TESTING.md](TESTING.md) before running benchmarks.

## The question

Can Canon's known class table and real game traffic support a much faster component graph,
especially when a player's agent searches ahead to hide choices that cannot complete? The useful
target is a substantial end-to-end improvement in replay and search, with a small, coherent data
model. Exact concrete component counts must remain authoritative. A faster derived query must
still respect dependency variance, dynamic Types, and state-dependent refinements.

The class table is immutable within a World and shared by Worlds. Its compiled inheritance facts
already use compact bits; assigning dense IDs to more facts is only useful if the surrounding graph
work becomes cheaper. The restored `Engine.overlay` now puts speculative component changes in local
deltas and leaves its source graph unchanged. Its current abstract count path computes the source
count and scans overlay deltas. A search agent may reset an overlay repeatedly. This shifts the
likely tradeoff toward doing more work on committed source writes, but no current trace measures
that tradeoff. Source Agent probes can still mutate and roll back the source, invalidating overlays;
see [ENGINE.md](ENGINE.md) and the existing overlay measurement item in [TODO.md](../../TODO.md).

## Evidence we have

A 2026-09-13 mutation-aware trace of 25 replay `WholeWorld` graphs recorded 1,177,930 component
graph queries and 269,456 component mutations, or 4.37 queries per mutation. Type counts made up
428,396 calls (36.37%); direct dependent lookups 284,731 (24.17%); exact containment 222,249
(18.87%); exact counts 186,641 (15.84%); `getAll` 33,243 (2.82%). Abstract Type counts numbered
121,291. Refined Types appeared in 0.15% of all graph calls. There were 16,635 distinct complete
Type expressions across the run; the ten hottest accounted for only 14.21% of calls. About 45.38%
of calls came from speculative task selection. This trace **predates the restored overlays and any
full pruning agent**; overlay calls were excluded. Abstract Type counts were only 10.3% of all graph
calls, and call frequency alone says nothing about their share of elapsed time. Its raw local trace
is not retained here; the methodology and results are in historical
`git show 8840b4c03:docs/agents/JVM_TEST_PERFORMANCE.md` under “Mutation-aware trace.”

An earlier replay comparison of alternative multisets and inline dependency links found no stable
large win after rerun: the leading indexed multiset was 0.7–0.9% faster, within host variation, so
the shard-owned multiset and separate dependency map stayed. That experiment is evidence against
adding graph machinery for a small isolated gain, not evidence that indexing can never help a
different workload.

The 2026-10-18 structural-supertype audit used `Canon.withPlayers(2).classTable`. It took each of
1,510 concrete Canon classes, recursively chose a deep concrete type for each dependency, and
enumerated every distinct *unrefined structural* supertype of that chosen Type, including nested
dependency combinations. The five equally sized groups of 302 classes spanned, respectively,
**2–7, 7–7, 7–16, 16–18, and 18–6,051** supertypes. Median was 10, 95th percentile 37, 99th
percentile 86; 13 classes exceeded 100, and three exceeded 1,000. These are selected stress
examples per class, neither maxima over every possible concrete Type nor traffic-weighted results.
The table also includes classes that cannot be held as graph components.

The largest example explains the danger of blindly materializing the full closure. The chosen
`ForwardAdjacency` has two tile dependencies. Each chosen `CapitalTile` has 93 structural
supertypes, 55 of which satisfy the adjacency slot's tile bound. The two slots independently admit
55 × 55 combinations under `ForwardAdjacency` and again under its parent `Adjacency`, plus
`Component`: **3,025 + 3,025 + 1 = 6,051**. The same CapitalTile in both slots is a structurally
valid stress case, not an ordinary board placement. The 2,690-supertype `RequiredWildTag` is a
custom metric and cannot itself be gained into the graph. The audit shows combinatorial potential;
it does not establish the cost of actual graph writes.

## Candidate structures to measure

1. **Full structural aggregate counters.** On gaining an exact Type, increment its count and the
   count of every structural supertype; reverse those updates on removal. Unrefined abstract counts
   become direct slot reads. The 6,051 case makes this a risky default, even with read-mostly source
   graphs. We need the *actual mutation-weighted* fanout, memory footprint, and overlay reset cost.
   State-dependent refinements cannot generally be frozen into this closure.
2. **Query-selected aggregate counters.** Register the unrefined structural Types that actual code
   queries frequently. Precompute a sparse incidence list from each changed exact Type to the
   registered query counters it affects. A write updates exact state and those counters; an
   unregistered query uses the existing filtered scan. Registration might come from authored Canon
   queries plus replay observations, with a measured policy for runtime-generated keys. Static
   source inspection alone will miss specialized Types built at runtime. Exact counts already have
   a direct path and need no second counter. This is the leading hypothesis, not an implementation
   decision: the useful number is the intersection of *observed query keys* with *observed writes*,
   not the size of the universal supertype closure.
3. **Factorized dependency index.** If the useful query set is too broad to maintain counters,
   compact postings or bitsets for root class and dependency dimensions could narrow candidates at
   read time without writing every Cartesian combination. This trades a few intersections and
   checks per query for smaller write fanout and storage. Compare it against the present shard
   filter, not merely against a full scan.

An overlay might read a source aggregate and scan its small local delta, or maintain selected local
aggregate deltas and clear only touched slots on reset. Which is faster depends on overlay size and
query repetition. `getAll`, dependent lookups, live effects, and task resolution remain separate
costs; a faster `count(Type)` cannot by itself make whole-game search fast.

## Measure before choosing

- Add opt-in, low-overhead traces for source Worlds and overlays separately. Record phase, query
  kind and Type key, repeat count, candidate work, concrete mutations, overlay delta size/reset
  frequency, and time/allocation cost. Include current replays and realistic agent choice traffic.
- Offline, join each changed exact Type with queried abstract Types. Report incidence fanout and
  storage for full closure, observed-only counters, and several popularity cutoffs, both per-event
  and traffic-weighted. Keep refinements and runtime-specialized Types visible as separate groups.
- Prototype the smallest promising graph representation behind the current API. Compare complete
  replay and search workloads, including overlay creation/reset and committed moves; use focused
  microbenchmarks only to explain a measured whole-workload result. Verify count, containment,
  dependency, and refinement semantics against the existing graph.
- Revisit the trace when a full pruning agent exists. Such an agent belongs to the player side and
  may radically change the mix of reads, writes, and resets. See [SMART_AUTOEXEC.md](SMART_AUTOEXEC.md)
  for the agent's correctness model and search questions. A budget-limited search must distinguish
  an unproven choice from a proven dead end; engine indexing should follow measured search traffic.

The database analogy is selective materialized views: maintain only aggregates whose saved reads
justify their write and storage costs. See [DynaMat](https://www.cs.umd.edu/~nick/papers/DynaMat.html)
for workload-aware view selection and the
[data cube paper](https://www.microsoft.com/en-us/research/publication/data-cube-a-relational-aggregation-operator-generalizing-group-by-cross-tab-and-sub-totals/)
for why materializing all dependency combinations can grow rapidly. Neither result selects a
structure for this graph without our own workload measurements.
