# JVM test performance snapshot

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** measuring JVM test throughput, changing fork count or class-model compilation, or
> choosing the next test-performance investigation.
>
> **Skip when:** running routine verification; use [TESTING.md](TESTING.md). Do not treat these
> measurements as current configuration requirements.
>
> **Status:** dated research from 2026-08-23, 2026-09-06, 2026-09-10, and 2026-09-11 on the development host.
> Treat absolute times as noisy: other JVM processes were consuming substantial CPU during the
> baseline. Relative structure and the large parallel-speedup signal are still clear.

## Configuration entry points

- [`solarnet.kotlin-base.gradle.kts`](../../gradle/build-logic/src/main/kotlin/solarnet.kotlin-base.gradle.kts)
  — search for `maxParallelForks` before changing JVM worker scaling.
- [`TfmTest.kt`](../../test/common/dev/martianzoo/tfm/tests/TfmTest.kt) — inspect
  class-level game construction only when profiling setup cost.
- [`ClassLoader.kt`](../../src/common/dev/martianzoo/pets/types/ClassLoader.kt) — read
  only when a fresh profile again points to Class-model construction.

## End-to-end baseline

Command: `./gradlew test --rerun-tasks --profile --console=plain`

- Wall time: 6m52s; the configuration cache was reused, with effectively no configuration time.
- Gradle reported 8m19s of summed task time because independent module tasks overlap.
- Compilation was not the bottleneck. Engine production and test compilation together took 6.2s.

| Test task | Task time | Reported cases |
| --- | ---: | ---: |
| `:engine:jvmTest` | 6m38.95s | 749 |
| `:script:jvmTest` | 55.50s | 55 |
| `:pets:jvmTest` | 10.46s | 141 |
| `:repl:test` | 10.03s | 12 |
| `:tools:test` | 4.44s | 5 |
| `:tfm-canon:jvmTest` | 2.05s | 16 |

Engine is the critical path and accounts for 97% of wall time.

## Engine distribution

The engine XML suites reported 397.6s total; individual methods accounted for 365.2s and
class-level setup/runner work for the remaining 32.5s.

| Area | Method time | Share |
| --- | ---: | ---: |
| Card and rule scenarios | 222.6s | 61.0% |
| TFM engine coordination | 65.2s | 17.9% |
| Generic engine | 49.9s | 13.7% |
| Whole-game scenarios | 27.4s | 7.5% |
| Type-system tests | 0.05s | <0.1% |

This cost is broad rather than dominated by a few outliers. The median engine method took 0.505s;
542 of 749 methods took 0.25–1.0s and consumed 78.8% of method time. The top ten classes accounted
for only 24.2%.

Largest classes were `ModuleSelectionTest` (18.9s), `Prelude2CardsTest` (15.5s),
`TaskNarrowingTest` (14.4s), `ClassTableProjectionTest` (7.7s), and `CoreRulesTest` (7.6s).
The single largest method was ModuleSelection's valid-configuration catalog at 15.1s.

## CPU and allocation sample

A Java Flight Recorder sample of all 25 `Prelude2CardsTest` methods ran for 19s. The single test
thread allocated an estimated 41.6GB and triggered 492 young collections. Collection wall time was
0.36s (2.36s combined collection CPU), so pauses are not the principal problem; allocation and
construction work are.

Top CPU samples were `DependencySet` construction (7.6%), dependency validation (6.5%),
`HashMap.putVal` (4.9%), dependency lookup (3.3%), and collection `addAll` (3.2%). The dominant
allocated objects were linked maps and their iterators/entries, array lists and iterators, object
arrays, linked sets, lazy holders, `Type`, and `DependencySet`. This matches the repeated
construction of a fresh World and projected type/dependency state in most card tests.

## Worker-scaling trial

The project does not configure intra-task test forks, leaving the engine task with one worker.
Clean engine-task trials changed only `maxParallelForks` through a temporary Gradle init script:

| Workers | Engine task wall time | Aggregate suite time | Parallel efficiency |
| ---: | ---: | ---: | ---: |
| 1 | 6m38.95s | 397.6s | 99.7% |
| 4 | 2m05.01s | 430.4s | 86.1% |
| 8 | 1m30.19s | 554.5s | 76.9% |

Both parallel trials passed. They discovered 765 cases in 188 classes, while the initial run's XML
contained 749 cases in 187 classes; the source of that 16-case discrepancy was not established.
Because the parallel runs performed more observed test work, it does not explain away the speedup.

## Compiled class-model result

`GamePremise` now retains one active `ClassTable` projection, effective inherited invariants are
cached on the Catalog-owned Classes, and each projection compiles its immutable component limits
once. Independent Worlds share that class model while each `Limiter` applies it only to the World's
live component graph.

An immediate before/after run of all 26 `Prelude2CardsTest` methods used the same focused Gradle
command. Reported suite time fell from 14.623s to 6.810s: 53.4% less time and 2.15x throughput.

The complete JVM command from the original baseline was then repeated after the change:
`./gradlew test --rerun-tasks --profile --console=plain`.

| Measurement | Before | After | Reduction |
| --- | ---: | ---: | ---: |
| Build wall time | 6m52s | 3m04.76s | 55.2% |
| `:engine:jvmTest` task | 6m38.95s | 2m58.69s | 55.2% |
| Engine XML suite total | 397.6s | 177.67s | 55.3% |
| `:script:jvmTest` task | 55.50s | 35.37s | 36.3% |

The post-change engine run passed 765 cases in 188 classes, matching the observed test count from
the earlier parallel trials. The original whole-suite baseline was taken under heavier host load,
so the focused immediate comparison is the cleaner speedup measurement; both show the same large
effect.

## Four-fork result after class-model compilation

All JVM `Test` tasks were configured with `maxParallelForks = 4`, then the complete JVM command was
run twice. Both runs passed. The first rebuilt the changed convention plugin and stored a new
configuration-cache entry; it took 1m20.62s overall and 1m05.93s for `:engine:jvmTest`. The clean
repeat reused the configuration cache and produced these measurements:

| Measurement | Serial after class-model change | Four forks | Reduction |
| --- | ---: | ---: | ---: |
| Build wall time | 3m04.76s | 1m17.09s | 58.3% |
| `:engine:jvmTest` task | 2m58.69s | 1m05.50s | 63.3% |
| Engine XML suite total | 177.67s | 209.96s | -18.2% |
| `:script:jvmTest` task | 35.37s | 26.27s | 25.7% |

The engine repeat passed 765 cases in 188 classes with no failures or errors. Its 2.73x elapsed-time
speedup corresponds to 68.2% parallel efficiency. Aggregate engine suite time increased by 18.2%,
which is the expected throughput tradeoff from running isolated test processes concurrently. The
smaller suites also paid fork and host-contention overhead, but engine remained the critical path
and the complete build still finished 1m47.67s sooner.

## Anchored lexer result

A 2026-09-06 flight recording of `Prelude2CardsTest` found that 330 of 350 regex execution-sample
stacks came from `AnchoredRegexToken`. It used `Regex.find` and rejected a result that began after
the current tokenizer position, needlessly searching the remaining input. Calling `Regex.matchAt`
expresses the token contract directly and performs no forward search.

Immediate AC-powered control and changed runs used the same 29 test methods. The single-fork JVM
suite fell from 11.539s to 7.161s and 7.116s, a 37.9–38.3% reduction. Chrome task execution fell
from 21.110s to 18.195s and 18.650s, an 11.7–13.8% reduction. The complete forced JVM and browser
suites passed after the change in 1m29s and 6m28s respectively; earlier full-suite baselines that
day were power-throttled and are not valid comparisons.

## Class-limit expression result

The same recording showed type-expression minimization beneath `ClassLimitTable` initialization.
`UnboundRestriction.bindThisTo` requested the shortest display expression for a fully resolved
type, then immediately transformed and resolved it again. Supplying the already-available full
expression preserves the resolved type while avoiding a combinatorial search through dependency
spellings.

The single-fork JVM focus changed from 7.116–7.161s to 6.997s, which is within run-to-run noise.
Chrome was consistently better: the focused task took 18.503s with the old expression and 15.748s
and 15.054s with the full expression, a 14.9–18.6% reduction. Complete forced JVM and browser suites
passed after the change in 1m38s and 5m46s.

This cost is paid once per distinct active `GamePremise` in a test process, not once per World.
The canonical master table is also initialized once per process. Build-time generation could remove
some canonical-universe startup, but it would not remove configuration-specific projection work or
support custom catalogs without another representation; measure the remaining startup cost before
considering that tradeoff.

## 2026-09-10 full-suite distribution and premise-setup profile

A fresh forced JVM run passed 1,463 tests in 4m44s while other JVM work contended for the host. The
absolute duration is therefore not comparable to the earlier clean runs, but its test-effort
distribution answers where suite-wide savings remain:

| Area | Share |
| --- | ---: |
| Terraforming Mars card tests | 31.66% |
| Terraforming Mars rule tests | 20.24% |
| Generic engine tests | 14.25% |
| Script and REPL tests | 14.14% |
| Other JVM tests | 10.16% |
| Replay tests | 4.51% |
| Pets module tests | 2.76% |
| Random-card tests | 2.28% |

Card and rule tests together consume 51.9% of observed suite effort. Replays are not the dominant
cost.

A focused flight recording covered 27 `Prelude2CardsTest` methods and 28 calls to
`CardTest.newGame`. Application samples attributed 50.2% of CPU and 54.1% of allocation to setup.
Premise construction used 6.4% of CPU, `Engine.newGame` used 44.1%, and `ClassLimitTable`
construction alone used 33.2% of CPU and 37.3% of allocation. Setup allocated an estimated 36.42GB
of the 67.28GB application total. The hottest stacks beneath limit construction repeatedly compiled
class headers, dependency closures, variable bindings, and dependency equalities from the
premise-specific master table.

This selects the direction in [CLASS_TABLES.md](CLASS_TABLES.md#selected-replacement-master-tables-premise-tables-and-class-universes):
compile one immutable master for Canon and one for Canon plus Fakes, then limit each premise to its
small declaration overlay, realized-Type domain, and genuinely premise-dependent validation. The
goal is reuse by explicit ownership and bounded lifetimes, not a global cache that grows with every
premise shape.

## Browser replay selection result

Only the extensive three-player `OtbGame20260828Test` full-game replay remains in shared test
sources; the other replay implementations are JVM-only. A direct browser-suite run continues to
exercise all 158 shared Terraforming Mars test classes, including that replay and one partial-game
test. Its Chrome task fell from 5m36.15s to 4m43.65s, a 15.6% reduction. The JVM suite still found
all 21 moved test classes and passed. There is no property or alternate task that adds the JVM-only
replays back to a browser run.

## 2026-09-11 heap-retention diagnosis

A normal JVM run failed in `:tfm-tests:jvmTest` with `Java heap space`. The two suites absent from
its XML reports passed alone in a single 512 MiB worker. Running the whole Terraforming Mars suite
in that worker grew to 504 MiB after full collection; it was stopped after 487 full collections.

The captured heap established this retention path:

`Catalog -> authored Effect -> TypeVariableScope -> TypeVariable.bound -> ClassTable -> Catalog`

`Class.interpretTypeVariablesIn` annotated the shared source Effect in place. Source declarations
outlived the games that interpreted them and retained those games' compiled universes. It now copies
the Effect before attaching the resolved scope. The `Spec13TypeVariablesTest` shared-source
regression fails before the fix because a second Catalog overwrites the first interpretation's
scope; it also checks that source declarations retain no resolved variables.

`ClassTableProjectionTest` additionally scopes compiled fixtures to test instances. The normal
worker budget is two 1 GiB heaps, bounded across test tasks by `org.gradle.workers.max=2`; total
maximum test heap remains 2 GiB per invocation. The build daemon's separate 4 GiB heap is unchanged.

Both complete JVM runs with the new worker budget passed. With the shared-source mutation still
present, the two Terraforming Mars workers peaked at 744 and 612 MiB immediately after collection.
After the Effect copy fix, they peaked at 274 and 278 MiB, with no full collections. The final run
passed all 1,462 tests in the current JVM modules in 4m57s. These are GC-log heap measurements, not
whole-process resident memory; the timings include compilation and host contention.

## Priorities suggested by the data

1. Preserve the compiled class-model reuse. It removed over half of measured JVM test time without
   sharing live World state.
2. Keep the bounded worker and heap policy in [TESTING.md](TESTING.md). Check retained memory as
   well as elapsed time before increasing parallelism.
3. Treat isolated slow-test cleanup as secondary. Whole-game scenarios are not the main cost, and
   even deleting the single 15.1s outlier would save under 4% of engine CPU.
4. Trace increasing retained heap to its owners before changing memory limits. Throughput samples
   from short tests do not establish the memory needs of a long-lived suite worker.
