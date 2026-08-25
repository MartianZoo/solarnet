# JVM test performance snapshot

> **Read when:** measuring JVM test throughput, changing fork count or class-model compilation, or
> choosing the next test-performance investigation.
>
> **Skip when:** running routine verification; use [TESTING.md](TESTING.md). Do not treat these
> measurements as current configuration requirements.
>
> **Status:** dated research from 2026-08-23 on the development host. Treat absolute times as
> noisy: other JVM processes were consuming substantial CPU during the baseline. Relative structure
> and the large parallel-speedup signal are still clear.

## Configuration entry points

- [`solarnet.kotlin-base.gradle.kts`](../../gradle/build-logic/src/main/kotlin/solarnet.kotlin-base.gradle.kts)
  — search for `maxParallelForks` before changing JVM worker scaling.
- [`TfmTest.kt`](../../tfm-tests/src/commonTest/kotlin/dev/martianzoo/tfm/tests/TfmTest.kt) — inspect
  class-level game construction only when profiling setup cost.
- [`ClassLoader.kt`](../../pets/src/commonMain/kotlin/dev/martianzoo/pets/types/ClassLoader.kt) — read
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
| `:tfm-text:test` | 1.98s | 4 |

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
`TaskRevisionTest` (14.4s), `ClassTableProjectionTest` (7.7s), and `CoreRulesTest` (7.6s).
The single largest method was ModuleSelection's valid-configuration catalog at 15.1s.

## CPU and allocation sample

A Java Flight Recorder sample of all 25 `Prelude2CardsTest` methods ran for 19s. The single test
thread allocated an estimated 41.6GB and triggered 492 young collections. Collection wall time was
only 0.36s (2.36s combined collection CPU), so pauses are not the principal problem; allocation and
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

## Priorities suggested by the data

1. Preserve the compiled class-model reuse. It removed over half of measured JVM test time without
   sharing live World state.
2. Keep the four-fork bound unless memory-constrained CI evidence shows it is too aggressive. It
   retained a large elapsed-time win after class-model compilation without an unbounded host-based
   worker count.
3. Treat isolated slow-test cleanup as secondary. Whole-game scenarios are not the main cost, and
   even deleting the single 15.1s outlier would save under 4% of engine CPU.
4. Do not prioritize Gradle configuration, compilation, or merely increasing worker heap from this
   evidence. They are not driving elapsed time, and collection pauses are small.
