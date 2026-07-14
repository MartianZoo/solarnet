# TODO

Priorities appear in parentheses; no parenthetical means the default priority, **Soon**.

## Actor / Owner / Player Model — Intended Sequence

### Constraints and Useful Findings So Far

1. Effects belonging to an `Owned` component—and effects belonging to a `Player` component
   itself—have an intentional form of “actorship usurpation”: the effect owner can become the Actor
   responsible for resulting deferred work even when another Actor caused the triggering change.
   The existing context-owner / changed-component-owner / event-actor selection is therefore
   approximately necessary, not merely legacy naming to simplify away.
2. Abstract deferred effects can split control across Actors. The active Actor may control when the
   work is prepared within the current operation, while the other Owner whose effect fired controls
   how the abstract instruction is refined. Philares is the canonical regression case for this
   interaction and must remain working throughout the migration.
3. Double-colon effects execute immediately inside the same operation as their triggering change
   and are performed by the same Actor. They do not usurp actorship merely because their effect
   context has another Owner.
4. A task's primary Actor is the Actor the task is waiting on and authorizes to act, but provenance
   may need additional fields such as `onBehalfOf`. Prefer adding such fields during the migration
   and removing proven redundancies later over prematurely forcing queue ownership, authorization,
   triggering provenance, preparation timing, and refinement authority into one value.
5. `Task` remains the common representation for triggered work. An unidentified task is work that
   has not yet received its queue id; no parallel “triggered instruction” representation is
   currently justified.
6. Event logs should name the performing Actor with `BY` and the effect-bearing causal component
   with `VIA`.
7. For neutral global-parameter raises, an explicit Player restriction is preferred. Allowing an
   attempted `TerraformRating.` gain to collapse harmlessly through corrected AMAP behavior is a
   fallback, not the primary design.
8. `Owned<Owner>` remains the ideal declaration, but the existing default, substitution, and
   specialization machinery is delicate. The eventual enforcement layer is still an open question.
   Do not introduce `Verb<Actor>` or reclassify the existing signals without a demonstrated need.

### Implementation Order

1. Write characterization tests before changing the model. Cover the existing effect-actor
   selection order, immediate versus deferred effects, the `BY Owner`/`BY Anyone`/`BY Player` cases,
   repeated-type specialization within one effect, unidentified tasks before queue insertion,
   ordinary actor-scoped execution, whole-game auto-execution, and current event-log output. Add
   focused Philares tests for the active Actor's preparation timing and the effect owner's
   refinement authority. Use these tests to distinguish intentional rules from compatibility
   behavior instead of simplifying the code from inspection alone.
2. Fix and document the vocabulary and invariants. A `Player` is always exactly one of the 1–5
   people or bots playing Terraforming Mars. An `Owner` can own noun-like game-state components. An
   `Actor` can initiate or continue game operations. `Player` extends both; `SoloOpponent` is only
   an Owner; `Npc` and `Engine` are only Actors.
3. Add the minimal Pets `Actor` hierarchy and corresponding system class constant, making `Player`
   a subtype of both `Owner` and `Actor`, and making `Engine` an Actor. Do not introduce a `Verb`
   hierarchy or reclassify action signals unless a concrete type-safety or substitution problem
   later demonstrates that it is necessary.
4. Rename the current broad Kotlin execution identity from `Player` to `Actor`, then introduce or
   retain a genuinely player-only API type for seat order, turn order, player-count rules, and
   Terraforming Mars helpers. Replace the current regex-based “Player or Engine” meaning without
   broadening `Player` beyond the five actual participants.
5. Rename generic execution-facing properties and parameters consistently: `Gameplay.player`,
   `Changer.player`, scope maps, and `ChangeEvent.owner` become actor terminology. Rename
   `Task.owner` to `Task.actor`: a task is waiting on that Actor and authorizes that Actor to revise
   and execute it, subject to the characterized multi-Actor preparation/refinement rules. Do not
   call this role an assignee.
6. Add nullable Actor provenance to `Task` where the migration needs it, beginning with a candidate
   such as `onBehalfOf`. Keep this distinct from `cause`, which identifies the effect context and
   triggering event rather than delegated authority. Carry provenance through splitting, `THEN`,
   queue insertion, editing, rollback, and event logging; defer deciding which fields are redundant
   until Philares, Engine delegation, and Npc behavior are all expressible.
7. Keep `Task` as the common representation for triggered work. Characterize and document the
   lifecycle in which an unidentified task receives a real id when it is inserted into an Actor's
   queue. For double-colon effects, ensure the temporary task value executes inline under the
   triggering Actor and operation; do not introduce a parallel internal representation unless the
   existing one causes a demonstrated problem.
8. Split configured identities into actual `players` and all execution-capable `actors`. Actor
   scopes and task queues are created for Players, Engine, and configured Npcs; passive Owners such
   as SoloOpponent receive neither gameplay scopes nor task queues. Remove the current convention
   that `Player.players(...)` includes Engine.
9. Make cross-Actor execution an explicit capability. Ordinary Actors operate only on tasks that
   are waiting on and authorized for them. Determine whether whole-game auto-execution is needed
   only during initialization or elsewhere; where genuinely required, model it as an Engine
   privilege to execute *as* another Actor. Preserve both the performing Actor and any Engine
   delegation in the task/event provenance instead of deciding prematurely that only one identity
   matters.
10. Change event attribution to `ChangeEvent.actor` and render it with `BY`; render the effect-bearing
   causal component with `VIA`, retaining `BECAUSE <ordinal>` for the causal link. For example:
   `+OxygenStep BY Player2 VIA GreeneryTile<Player2, Tharsis_5_5> BECAUSE 448`.
11. Isolate and name the Effector rule that chooses which Actor a deferred task waits on. Preserve
    approximately the current context-owner / changed-component-owner / event-actor fallback until
    focused examples show the exact rule. Do not apply this usurpation to double-colon effects,
    which retain the triggering Actor.
12. Characterize `BY` independently from task routing. Determine from existing cards—especially
    Philares—whether each form tests the triggering Actor, the Actor selected for deferred work, a
    contextually substituted Owner, or some combination. Permit the event Actor, task Actor, and
    `onBehalfOf` Actor to remain distinct if the rules genuinely use all three.
13. Keep `ByTrigger.by` as a `ClassName` initially. Enumerate the actual useful forms (`Anyone`,
    `Actor`, `Player`, `Owner` as a contextual reference, concrete Players, `Npc`, and `Engine`) and
    implement their semantics directly. Introduce an Actor-bounded expression only if a real need
    arises for parameterized, refined, complemented, or otherwise non-class Actor selectors.
14. Separate contextual substitutions by role. Actor-scoped parsing always substitutes the current
    Actor where Actor context is requested. Owner substitution occurs only when an Owner is actually
    available—normally because the Actor is a Player or because an effect-bearing component has an
    Owner. Ensure Engine and Npc never acquire Owner powers merely to reuse the old preprocessing
    pipeline.
15. Attempt the ideal `Owned<Owner>` bound only behind focused dependency/default/substitution tests.
    Preserve `Anyone` use-site behavior and the rule that repeated occurrences of the same type in
    an effect specialize together. If changing the declared bound destabilizes this machinery,
    retain `Owned<Anyone>` and enforce “only Owners can own nouns” through class validation or
    concrete-component validation instead. Leave the choice of enforcement layer open until these
    experiments provide evidence.
16. Make the global-parameter/TR rule an explicit Actor-model test case before choosing its final
    Pets spelling. Preserve the important coupling expressed by using `Player` in both the `BY`
    position and `TerraformRating<Player>`, and first verify whether `BY Player` is already inserted
    automatically. Prefer suppressing the effect for Npc/Engine; retain `TerraformRating.` plus the
    eventual corrected AMAP semantics from issue #28 as a fallback if the explicit restriction
    cannot be expressed cleanly.
17. Implement `Npc` and World Government Terraforming using the resulting Actor APIs, including an
    Npc task queue and truthful Npc event attribution. Then introduce `SoloOpponent` through the
    Owner APIs without giving it gameplay, a queue, or Player-only components.
18. Update engine, language, and component-model documentation and run the full JDK 21 build after
    each behavior-affecting stage. Keep migrations small enough that failures identify which Actor,
    Owner, queue, trigger, or specialization invariant was disturbed.

Current progress: the vocabulary and minimal Pets hierarchy from steps 2–3 are in place. The Kotlin
runtime still intentionally uses `Player` as its broad Player-or-Engine compatibility identity;
steps 4 and later remain pending.

## Gameplay Rules Implemented Incorrectly or Incompletely

- Model the two Prelude plays as explicit first and second Prelude turns, analogous to action-phase
  turns, so turn ownership, hooks, and future workflow changes are consistent. (Somewhat soon)
- Issue #28: Flaw with "amap" quantifier and ocean tiles (perhaps other cases too?) — Define when an
  abstract AMAP instruction may narrow to `Ok`: missing dependencies must allow declining impossible
  card-resource gains, as exposed by the disabled Local Heat Trapping test, without permitting a
  player to select an occupied area and thereby evade an otherwise possible ocean placement. Revisit
  `Instructor.autoNarrowTypes` and the `ArtificialLake` `!` workaround as part of this. (Needs discussion)
- Issue #33: GreeneryTile default gain "fallback rule"
- Issue #49: Handle `replacesId` in card/etc. data

### Suspected Over-Permissiveness

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

- Issue #58: Npc / world government terraforming — Implement the Venus Next world-government phase
  in `TfmWorkflow.Auto`, at the correct point after production and before the next generation, and
  represent its neutral actor correctly in multiplayer games. This is a missing Venus Next rule, not
  merely workflow cleanup. (Somewhat soon)
- Remove Terraforming Mars and Colonies knowledge from generic engine initialization and class
  loading. Replace the bundle-name check, `TfmAuthority` cast, colony-tile loading, trade-fleet
  creation, and other Colonies setup with extension-owned declarations or setup hooks so additional
  expansions do not require more hard-coded engine branches. (Needs discussion)
- Issue #1: Shuffle-and-deal mode
- Issue #2: Solo mode — The current follow-along solo fixtures configure Player2 as a stocked dummy
  opponent, so the number of `Owner` components cannot identify which seats should receive the
  `StartToken`. Model real participants separately from test-support actors before using the
  automatic workflow for those games. When introducing `SoloOpponent`, replace its artificial stock
  of 99/999 resources and production with demand-driven provisioning: acquiring a pending removal
  task against the opponent should materialize exactly the removable components that task needs.
  Work out the corresponding neutral host model for card resources (for example, the imaginary
  animal that Predators may remove) without giving the opponent a playable `CardFront`.
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
