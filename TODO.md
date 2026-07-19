# TODO

Priorities appear in parentheses; no parenthetical means the default priority, **Soon**.

## Gameplay Rules Implemented Incorrectly or Incompletely

- Model the two Prelude plays as explicit first and second Prelude turns, analogous to action-phase
  turns, so turn ownership, hooks, and future workflow changes are consistent. (Somewhat soon)
- Issue #28: Flaw with "amap" quantifier and ocean tiles (perhaps other cases too?) — Define when an
  abstract AMAP instruction may narrow to `Ok`: missing dependencies must allow declining impossible
  card-resource gains, as exposed by the disabled Local Heat Trapping test, without permitting a
  player to select an occupied area and thereby evade an otherwise possible ocean placement. Revisit
  `Instructor.autoNarrowTypes` and the `ArtificialLake` `!` workaround as part of this. (Needs discussion)
- Issue #33: GreeneryTile default gain "fallback rule"

### Suspected Over-Permissiveness

- Encode Viron's "another card" restriction directly. The usual convenience path happens to select
  the reused action before adding Viron's own action-used marker, which correctly keeps Viron
  ineligible, but task order has no rule meaning; the underlying dynamic refinement would allow
  Viron to select itself if that marker task were executed first.
- Diagnose whether preparing a gated instruction incorrectly loses meaningful ownership information.
  `PrepareTest` currently turns `Plant<Anyone>` into unowned `Plant!`; establish whether this is only
  harmless canonicalization or permits an invalid target, then document or fix it with regression
  tests.
- Issue #19: Prevent overpayment? — Document the card-purchase/payment protocol, including the roles
  and invariants of `BuyCard`,
  `PlayCard`, `Owed`, `Accept`, and `Pay`. Investigate the original warning that the `-3` in
  `BuyCard` (`This: -3, ProjectCard`) "could potentially be exploited" rather than assuming a
  specific exploit; define the legal overpayment bound; account for effects such as Terralabs
  Research's `BuyCard:: 2` discount and the reason card buying must be represented as a delayed
  signal/task. (Needs discussion)
- Issue #63: Atmoscoop doesn't actually work yet — Permit an OR branch containing an atomized
  `Multi`, then restore Atmoscoop's canonical simultaneous choice and remove its temporary
  sequential-task encoding, which may expose ordering choices the card is not supposed to provide.
  (Later)
- Issue #62: I can reuse the same trade fleet?

## Expansion or Content Support Gaps

- Issue #1: Shuffle-and-deal mode
- Issue #2: Solo mode — Work out the neutral host model for card resources (for example, the
  imaginary animal that Predators may remove) without giving Opponent a playable `CardFront`.
- Issue #4: Turmoil
- Issue #5: Game configurations
- Issue #9: Community
- Issue #11: Custom requirements (a la custom instructions)
- Issue #12: Specialization across THEN (needed by UseCardAction, Flooding, UtopiaInvest)
- Issue #13: OR triggers
- Issue #15: Wild tag
- Issue #17: Requirement high jinks
- Issue #18: Auto-specializing triggers
- Issue #20: awards
- Issue #22: `ELSE`
- Issue #24: Counting tag types
- Issue #34: Consider letting component types have properties after all <sigh>
- Issue #36: More expressive defaults?
- Issue #37: The "Cardbound problem" (class signature "linkages")
- Issue #48: Trigger types should support refinements
- Issue #64: I suspect a card with `2 CityTile` would not work

## Core Instruction and Engine Semantics

- Replace or redesign the brittle `doFirstTask()` convenience. Task queue order has no domain
  meaning; callers should select by task id or an explicit match unless they have established that
  exactly one task can apply.
- Research and simplify coupled-scalar (`X`) validation for `THEN` instructions. The current
  implementation zips instructions and descendant scalars and relies on traversal order and count
  equality; explicitly test and enforce the rule that all occurrences of a shared `X` resolve to one
  consistent multiplier.
- Issue #29: Handle `THEN` better
- Issue #60: auto-narrowing instructions
- Issue #61: make sure no `Temporary` instances get littered

## Failure Safety and Error Classification

- Give failed gameplay operations precise domain exceptions. For example, Predators with no
  stealable animal currently makes its test accept any `Exception`; identify the expected
  unavailable-target error and narrow both implementation behavior and the assertion.
- Separate expected domain failures from programming and system failures throughout speculative task
  preparation and auto-execution. `canPrepareTask` and whole-game auto-exec currently treat every
  `Exception` as "cannot prepare," which can hide engine defects. Introduce an explicit result or a
  narrow exception contract, preserve useful pending reasons, and let unexpected failures surface.
  Split `autoExecNext` into understandable safe-choice, speculative-preparation, dead-end-reporting,
  and unsafe-fallback operations while doing this.
- Issue #42: See what I can get to stacktrace, and handle the problem better — Audit broad exception
  handling throughout the script layer. Narrow parsing failures in `new`,
  `mode`, and `help`; distinguish user mistakes from programming/system failures in `executeAll` and
  command dispatch; preserve useful stack traces; and decide where server-side logging belongs.
- Reject `exit` inside files executed by the `script` command so a script cannot accidentally shut
  down server mode. Parse commands with comments and whitespace handled correctly and report the
  offending line.
- Narrow the exception contract of boolean instruction/type compatibility probes.
  `Instruction.narrows` currently turns every exception from `ensureNarrows` into `false`. Determine
  which exception types mean a legitimate narrowing mismatch and which represent broken invariants
  or bugs that should propagate; this is the instruction-level counterpart of the task-preparation
  exception audit above. (Needs discussion)

## Test Fidelity

- Load the correct map-specific milestones in logged-game fixtures. `Game20260619Test` substitutes a
  manual VP adjustment because it claims Elysium's Specialist while only Tharsis milestones are
  present; select the logged map's awards and milestones and exercise the real claim flow.

## User-Facing Behavior and Diagnostics

- Investigate parser improvements now that the project maintains better-parse itself. Include the
  duplicate validation/reporting paths in typed `parse(KClass, ...)`, the multiple `myThrow`-style
  error flatteners, structured alternative-input and source-location reporting, and opportunities to
  improve the underlying parser rather than merely consolidating local wrappers.
- Rewrite `auto` command help and validation to match the actual `none`, `safe`, and `first` modes;
  it still describes an obsolete on/off interface.
- Revisit why `MAX 0 Barrier` lacks the "currently impossible" explanation and whether the extra
  blank line in script output is intentional command separation or a formatting bug.
- Verify color behavior with a real-terminal smoke test. The current REPL still emits a mode-colored
  prompt and colored `board`/`map` output through ANSI true-color sequences, while ordinary textual
  command output remains plain as before. Confirm those escape sequences render correctly in the
  supported terminal rather than expanding this into a general structured-output project. (Needs discussion)
- Issue #30: In repl, allow reifying more concisely than repeating the entire instruction
- Issue #41: REPL: a much better `list` command — Include explicit `<Anyone>` ownership when omission
  is ambiguous, along with the hierarchy, grouping, depth, and concrete-subtype improvements in the
  issue.
- Issue #46: Bug in list command
- Issue #54: repl count issue — Correct owner-sensitive counts and display the resolved player in
  output so the result is understandable.

## Internal Design, Cleanup, and Test Convenience

- Investigate why narrowing solo setup's queued Opponent tile-placement tasks requires repeating the
  `Opponent` dependency in each concrete tile instruction; ideally the task's existing owner should
  be retained when a test supplies only the chosen map area.
- Allow a linked `X THEN X` task to be refined as a whole and then execute its concrete head and
  tail separately. Titan Shuttles and Recyclon currently repeat the combined instruction instead.
- Clarify why `Initializer` appends mandatory intensity (`!`) to every synthetic setup instruction.
  Determine whether setup really requires it or whether initialization should execute concrete
  instructions without textual rewriting.
- Issue #59: `-This` effects should get default intensity?
- Now that gameplay queue views are scoped, search tests and convenience APIs for simplifications
  where they previously expected an empty whole-game queue after each action; start with the
  `SoloGame0710Test` Head Start workaround.
- Issue #23: Actually run tests in javascript — Migrate the remaining `engine` and `repl` tests from
  Truth to cross-platform Kotest-backed assertions and ensure the relevant suite runs on JavaScript.
- Determine whether `playCorp` can be generalized to handle a corporation-selection task created by
  Merger. It does not currently appear fixed: `playCorp` always opens a new `turn`, whereas Merger's
  corporation choice is already a pending task inside `playPrelude`. If appropriate, separate the
  task-selection helper from the turn-opening convenience wrapper and update `SoloGame0710Test`,
  `SoloGame0721Test`, and `MergerTest`.
- Investigate adapting the existing PET AST random generator to Kotest property testing, especially
  whether domain-aware shrinking can produce smaller, understandable failures without replacing the
  useful recursive generation logic wholesale. (Later)
- Add Kotlin Gradle Plugin binary API validation once its Kotlin 2.2 support is mature enough. Use
  ABI snapshots for the public `pets`, `engine`, `canon`, and `script` APIs and integrate validation
  with the normal `check` lifecycle, specifically to catch accidental public API changes.
- Extract repeated `Definition`-to-`ClassDeclaration` assembly used by standard actions and related
  definition types if a shared abstraction makes the definitions materially simpler while keeping
  category-specific supertypes and effects explicit.
- Decide whether humanized Terraforming Mars readers such as temperature and oxygen accessors belong
  on `TfmGameplay` or in a presentation/reporting adapter, then update `docs/engine.md` to describe
  the chosen API boundary. (Somewhat soon)
- Improve AST transformation architecture, evaluating these concrete directions before choosing one:
  generate or centralize child traversal/copying so every new AST subtype does not enlarge
  `PetTransformer.transformChildren`; replace generic `Change` handling plus a subtype switch with
  typed `Gain`, `Remove`, and `Transmute` operations; make atomization a stateless recursive
  normalization instead of relying on mutable `ourMulti` identity during traversal; and extract
  named predicates/operations from complex owner replacement. Preserve exact tree shape and
  traversal semantics with focused tests. (Needs discussion)
- Use `docs/engine-api-review.md` to guide a gradual replacement of `godMode()` and layer casts with
  explicit player, workflow, monitor, debug, and fixture role objects. This is a large API project;
  defer the script gameplay/access-context cleanup until this direction is clearer. (Later)

## Platform Reach

- Convert `script` to Kotlin Multiplatform so its session, commands, parsing, and plain-text output
  work from JavaScript; keep JVM terminal/server launchers in `jvmMain`, and add a JS session smoke
  test. Address remaining engine JS compatibility issues as they arise. (Needs discussion)

## Performance Only

- Profile and reduce the largest type-system allocation hot spots. `MType.glb` was observed allocating
  roughly 28 MB per solo game, while `MType.narrows` consumed about 19% of runtime and over 10 MB;
  investigate canonicalization/caching and avoid repeatedly rebuilding dependency, refinement, and
  requirement objects. (Later)
