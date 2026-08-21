# Auto-execution audit

**Status: audit of current behavior and measured structural cost.** The design direction below is
proposed, not implemented.

## Current behavior

`Implementations.autoExecNow` calls `autoExecNext` until it stops making progress. Unless a task is
already prepared, every pass scans every task id and calls `canPrepareAnyTask`. Each probe derives a
prepared instruction from the current World and treats any thrown `Exception` as “not preparable.”
`SAFE` stops when more than one candidate succeeds; `FIRST` tries successful candidates in stable
iteration order.

The scan is global even when the command that provoked it is Actor-local. Abstract tasks which
remain pending are reconsidered on later passes and later commands.

## Measured repeated work

JFR method tracing of `Game20260730Test` after immediate execution stopped using the reversible
execution preview recorded these invocation counts:

| Method or entry path | Calls |
| --- | ---: |
| `Implementations.autoExecNext` from `autoExecNow` | 3,158 |
| `Implementations.canPrepareAnyTask` from `autoExecNext` | 5,413 |
| `ApiTranslation.atomic` in total | 1,272 |
| `ApiTranslation.atomic` from explicit `doTask` | 565 |
| `ApiTranslation.atomic` from operation-body `autoExecNow` | 269 |
| `ApiTranslation.atomic` from `beginManual` / `finish` / `continueManual` / `manual` | 436 |

Recording changes elapsed time, but these call counts expose the control flow. There were more than
five auto-exec passes and nine candidate-preparation probes per explicit task selection on average.

## Why it repeats

There is no single owner of the automatic drain:

- `ApiTranslation.atomic` runs auto-exec after every nested facade call, not only the outermost
  logical command. `AtomicOperationBoundary` uses nesting depth only for completion notification.
- `Implementations.continueManual` drains both before and after its body, even though the enclosing
  `ApiTranslation.atomic` drains again.
- Terraforming Mars helpers call `autoExecNow` inside operations whose nested `doTask` calls and
  enclosing operation boundary already drain.
- Each drain starts eligibility discovery from the global task pool again. It does not retain what
  the preceding pass learned about tasks that remain unchanged.

The negative path is also suspicious. `canPrepareAnyTask` catches every `Exception`, so expected
ineligibility and engine defects are indistinguishable there. In `FIRST`, failed execution attempts
may edit `whyPending`; a changed diagnostic is a real task event and World revision. That means a
future revision-keyed eligibility cache must not invalidate itself merely because it recorded its
own diagnostic.

Explicit task matching can independently call `Instructor.prepare` while looking for a candidate.
If eligibility is eventually retained, matching should consume the same result rather than create
another preparation path.

## Smallest promising direction

1. Give one outer logical command boundary ownership of auto-execution. Nested task and helper calls
   should participate in that command without starting their own drain.
2. Remove overlapping pre-body, post-body, helper, and facade drains while preserving the deliberate
   differences among `NONE`, `SAFE`, and `FIRST`.
3. Measure again before adding a scheduler or index.
4. Only if scanning remains material, retain an eligibility snapshot for an unchanged task and
   gameplay revision. Keep diagnostic bookkeeping outside its invalidation key.

This fits the proposed command runner in [API.md](API.md). It should not turn stable iteration order
into game precedence; [SEQUENCING.md](SEQUENCING.md) still places automatic selection policy at the
client boundary eventually.
