# TODO

This file tracks work that still appears applicable to the current codebase. Public issue links
identify the broader discussion; the text here describes only the part that remains.

Priorities appear in parentheses. An item without a priority has the default priority, **Soon**.

## User Ideas and Agreed Directions

### Gameplay Rules and Missing Content

- [Issue #28: AMAP and ocean tiles](https://github.com/MartianZoo/solarnet/issues/28) — Define when
  an abstract AMAP instruction may narrow to `Ok`. Missing dependencies must allow declining an
  impossible card-resource gain, as in the disabled Local Heat Trapping test, without letting a
  player select an occupied area to evade an otherwise possible ocean placement. Revisit
  `Instructor.autoNarrowTypes` and Artificial Lake's explicit `!` workaround. (Needs discussion)
- [Issue #33: Greenery placement fallback](https://github.com/MartianZoo/solarnet/issues/33) —
  Express the rule that a greenery normally goes next to one of the player's tiles, but may go on
  any legal area when no adjacent placement exists.
- [Issue #19: Prevent overpayment](https://github.com/MartianZoo/solarnet/issues/19) — Specify and
  enforce the legal overpayment bound: the surplus must be less than the value of every resource
  type used, so using any megacredits permits no surplus. First document the existing `BuyCard`,
  `PlayCard`, `Owed`, `Accept`, and `Pay` protocol, including Terralabs Research's discount and why
  buying cards uses a delayed signal/task. (Needs discussion)
- [Issue #62: A trade fleet can be reused](https://github.com/MartianZoo/solarnet/issues/62) — Find
  why `TradeFleet`'s `HAS MAX 1 Trade<This>` invariant does not prevent the same fleet from trading
  twice in one generation, then add a regression test.
- [Issue #63: Atmoscoop](https://github.com/MartianZoo/solarnet/issues/63) — Permit an `OR` branch
  to contain an atomized `Multi`, restore Atmoscoop's simultaneous choice, and remove its temporary
  sequential encoding, which exposes ordering choices the card should not provide. (Later)
- [Issue #1: Shuffle-and-deal mode](https://github.com/MartianZoo/solarnet/issues/1) — Add real card
  identities and deck/hand/draw/discard state only when actual play mode becomes a priority; this
  would also replace follow-mode shortcuts on cards such as Search for Life. (Later)
- [Issue #2: Remaining solo-mode modeling](https://github.com/MartianZoo/solarnet/issues/2) — Model
  a neutral host for card resources, such as the imaginary animal that Predators may remove,
  without giving `Opponent` a playable `CardFront`.
- [Issue #4: Turmoil](https://github.com/MartianZoo/solarnet/issues/4) — Implement the expansion's
  rules and content. The bundle exists, but it does not yet provide the expansion. (Later)
- [Issue #5: Separate available content from enabled rules](https://github.com/MartianZoo/solarnet/issues/5)
  — Add an active-content selection between `GameOptions` and class loading. Bundle selection now
  supports expansion configurations, but including a bundle still enables all its definitions and
  the corresponding option; callers cannot yet select individual content independently.
- [Issue #9: Community](https://github.com/MartianZoo/solarnet/issues/9) — Introduce the shared
  `Occupant<Area>` model needed by Arcadian Communities and Land Claim, including same-owner
  replacement, wrong-owner rejection, and Arcadian's removal bonus.
- [Issue #12: Linked specialization across `THEN`](https://github.com/MartianZoo/solarnet/issues/12)
  — Represent the rare cases where repeated type expressions on opposite sides of `THEN` must be
  narrowed together, including Flooding and Utopia Invest.
- [Issue #13: `OR` triggers](https://github.com/MartianZoo/solarnet/issues/13) — Allow one effect to
  subscribe to alternative triggers so canonical definitions no longer duplicate the same effect.
- [Issue #15: Wild tags](https://github.com/MartianZoo/solarnet/issues/15) — Model temporary wild
  tags so they affect tag counts without firing tag-gain effects; this blocks Research Coordination,
  Research Network, and Septem Tribus.
- [Issue #17: Adjustable card requirements](https://github.com/MartianZoo/solarnet/issues/17) — Add
  a principled way for effects such as Inventrix and Adaptation Technology to modify a card's
  global-parameter requirement during payment/validation.
- [Issue #20: Awards](https://github.com/MartianZoo/solarnet/issues/20) — Implement funding,
  measurement, winner/runner-up assignment, and scoring. Map-specific award definitions are now
  selected correctly, but the gameplay mechanics remain absent.
- [Issue #22: `ELSE`](https://github.com/MartianZoo/solarnet/issues/22) — Add an instruction that
  requires its first branch whenever that branch is possible and uses the fallback only otherwise.
- [Issue #34: Component-class properties](https://github.com/MartianZoo/solarnet/issues/34) — Revisit
  properties only if remaining mechanics need values such as card cost or requirement to participate
  in ordinary class processing; custom metrics now cover several former use cases. (Later; needs discussion)
- [Issue #36: More expressive defaults](https://github.com/MartianZoo/solarnet/issues/36) — Explore
  defaults that match and rewrite an instruction, such as making removal from `Owner` mandatory but
  removal from `Anyone` optional. (Needs discussion)
- [Issue #37: Class-signature linkages](https://github.com/MartianZoo/solarnet/issues/37) — Link
  repeated dependency expressions so a `Cardbound` component and its `CardFront` necessarily share
  one owner, eliminating verbose forms such as `Animal<Predators<Player1>, Player1>`.
- [Issue #48: Refinements in trigger types](https://github.com/MartianZoo/solarnet/issues/48) — Make
  trigger matching honor refinements, as needed by effects based on a played card's cost or
  requirement.
- [Issue #64: Multiple tiles in one instruction](https://github.com/MartianZoo/solarnet/issues/64)
  — Ensure `2 CityTile` becomes two independent placement choices rather than one count-two change
  on a single area; decide whether all `Tile` types should be atomized.

### Language and Engine Semantics

- [Issue #24: Counting distinct concrete classes](https://github.com/MartianZoo/solarnet/issues/24)
  — Decide whether `Class<Tag>(OF Owner)`-style metrics should generically count distinct concrete
  classes associated with an owner. If the semantics are clean, replace Canon's current
  `DistinctTagType` and `DistinctResourceType` custom metrics.
- [Issue #29: Execute `THEN` incrementally](https://github.com/MartianZoo/solarnet/issues/29) — Keep
  a linked or coupled-scalar `THEN` instruction together long enough to narrow it consistently,
  then execute its concrete head and enqueue its still-abstract tail. Titan Shuttles and Recyclon
  currently repeat the entire combined instruction instead.
- [Issue #60: Auto-narrowing instructions](https://github.com/MartianZoo/solarnet/issues/60) — Define
  a small, predictable set of rules for resolving a unique available dependency or target without
  taking away a real player choice.
- [Issue #61: Temporary cleanup](https://github.com/MartianZoo/solarnet/issues/61) — Enforce at the
  appropriate engine boundary that `Temporary` components cannot remain after an operation, rather
  than relying only on the convenience layer's final assertion.
- [Issue #59: Default intensity for `-This`](https://github.com/MartianZoo/solarnet/issues/59) —
  Decide whether self-removal effects should be mandatory by default so canonical declarations no
  longer need repeated `-This!`. (Needs discussion)

### User-Facing Behavior

- [Issue #42: Preserve useful failures and stack traces](https://github.com/MartianZoo/solarnet/issues/42)
  — Narrow broad exception handling in the script layer. Distinguish parsing and usage errors from
  programming/system failures in `new`, `mode`, `help`, `executeAll`, and command dispatch, and
  decide where server-side logging belongs.
- [Issue #30: Concise task refinement](https://github.com/MartianZoo/solarnet/issues/30) — Let REPL
  users narrow a task without repeating the entire instruction.
- [Issue #41: Improve `list`](https://github.com/MartianZoo/solarnet/issues/41) — Add useful
  hierarchy/dependency descent, grouping and depth controls, sensible concrete-subtype handling,
  and explicit `<Anyone>` ownership where omission is ambiguous.
- [Issue #46: `list` loses concrete card identity](https://github.com/MartianZoo/solarnet/issues/46)
  — Fix listings such as `CardFront` collapsing concrete cards into one abstract `ResourceCard` row.
- [Issue #54: Owner-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54) — Correct
  counts whose refinements contain contextual ownership and display the resolved player in output.

### Platform Reach

- [Issue #23: Run applicable tests on JavaScript](https://github.com/MartianZoo/solarnet/issues/23)
  — The shared `pets`, `canon`, and `script` suites now run on JS. Decide how the intentionally slow
  engine browser suite should participate in routine verification, and keep terminal-only REPL
  tests on the JVM.
- Implement [World Government Terraforming](plans/world-government.md), replacing the logged-game
  neutral-opponent workaround with an Engine-performed operation chosen by the `StartToken` owner.
  Preserve the plan's Actor/Owner separation and Solar Phase rules. (Somewhat soon)

## Autonomous Follow-ups

### Gameplay Correctness and Test Fidelity

- Model the two Prelude plays as explicit first and second Prelude turns, analogous to action-phase
  turns, so ownership, hooks, and future workflow changes are consistent. (Somewhat soon)
- Give Tharsis Republic an explicit immediate solo-setup production gain rather than treating the
  neutral cities as though Tharsis observed their placement. Decide between `SoloMode: PROD[2]`
  and `PROD[1 / CityTile<Opponent>]` without exposing irrelevant multiplayer work. (Later)
- Encode Viron's "another card" restriction directly. It currently rejects itself only because
  convenience code selects the reused action before adding Viron's own action-used marker; queue
  order has no rules meaning.
- Determine whether preparing a gated instruction incorrectly discards meaningful ownership.
  `PrepareTest` turns `Plant<Anyone>` into unowned `Plant!`; establish whether that is harmless
  canonicalization or an invalid-target hole, then document or fix it with a regression test.
- Give failed gameplay operations precise domain exceptions. Predators with no stealable animal
  currently accepts any `Exception`; identify the expected unavailable-target failure and narrow
  the assertion and implementation contract.
- Load the logged map's milestones in `Game20260619Test` and exercise the real Specialist claim
  instead of shutting down workflow and substituting a raw five-VP adjustment.

### Engine Safety and Maintainability

- Replace or constrain `doFirstTask()`. Queue order has no domain meaning; callers should select by
  task id or explicit match unless they have established that exactly one task can apply.
- Simplify coupled-scalar (`X`) validation for `THEN`. The current implementation zips instructions
  and descendant scalars by traversal order and count; directly test and enforce that every use of
  a shared `X` resolves to one multiplier.
- Separate expected domain failures from programming/system failures during speculative task
  preparation and auto-execution. `canPrepareTask` still catches every `Exception`. Introduce a
  narrow result/exception contract, preserve pending reasons, let defects surface, and split
  `autoExecNext` into comprehensible safe-choice, preparation, dead-end, and unsafe-fallback steps.
- Narrow the exception contract of boolean instruction/type compatibility probes.
  `Instruction.narrows` currently converts every exception from `ensureNarrows` into `false`;
  legitimate mismatches should be distinguished from broken invariants. (Needs discussion)
- Decide whether `NewTurn`, `SecondAction`, `Pass`, and `UseAction1..3` are a generic engine
  protocol or Terraforming Mars behavior, then colocate their Pets declarations with the Kotlin
  code that interprets them.
- Audit setup effects and make only non-choice consequences automatic (`::`). In particular,
  consider creating `Photosynthesis` as an immediate consequence of removing `SetupPhase`, while
  leaving genuine setup choices queued. (Later)
- Finish separating the Canon catalog from selected rulesets: Canon should own bundle locators and
  resolve only requested bundles instead of inheriting from `TfmRuleset.Composite`. Preserve the
  invariant that resolving one selection never reads another bundle's payload.
- Decide whether an abstract custom root should remain invalid or gain explicit aggregation
  semantics. Do not infer aggregation through a Cartesian product of concrete dependencies; define
  dependency presence, multiplicity, and refinement behavior first.
- Preserve a queued solo-setup tile task's existing `Opponent` dependency when a test narrows only
  its area; concrete tile instructions should not have to repeat `Opponent`.
- Clarify why `Initializer` appends mandatory intensity (`!`) to every synthetic bootstrap
  instruction and whether concrete initialization can execute without textual rewriting.
- Simplify tests and helpers that still assume the whole game's queue must be empty after each
  action now that gameplay queue views are assignee-scoped; start with Head Start in
  `SoloGame0710Test`.
- Separate corporation-task selection from the turn-opening `playCorp` wrapper so Merger's pending
  corporation choice can use the same helper. Update `SoloGame0710Test`, `SoloGame0721Test`, and
  `MergerTest` if the abstraction holds.
- Extract repeated `Definition`-to-`ClassDeclaration` assembly used by standard actions and related
  definitions when a shared abstraction can keep category-specific supertypes and effects explicit.
- Decide whether humanized Terraforming Mars readers such as temperature and oxygen belong on
  `TfmGameplay` or in a presentation/reporting adapter, then document the chosen boundary in
  `docs/engine.md`. (Somewhat soon)
- Improve AST transformations while preserving exact tree shape and traversal semantics: centralize
  child traversal/copying, replace generic `Change` subtype switches with typed operations, make
  atomization stateless, and extract named owner-replacement operations. (Needs discussion)
- Use `docs/engine-api-review.md` to replace `godMode()` and layer casts gradually with explicit
  player, workflow, monitor, debug, and fixture roles. Defer script access-context cleanup until
  that direction is clearer. (Later)

### Diagnostics and Tooling

- Improve parser errors in the maintained better-parse fork where appropriate: consolidate typed
  `parse(KClass, ...)` validation and local `myThrow` flatteners, and add structured alternatives
  and source locations rather than merely rearranging wrappers.
- Rewrite `auto` help for the actual `none`, `safe`, and `first` modes; its usage and validation are
  current, but its prose still describes obsolete on/off behavior.
- Explain why `MAX 0 Barrier` is not labeled "currently impossible" and decide whether the extra
  blank line in script output is intentional command separation or a formatting bug.
- Extend the existing real-terminal smoke test to assert that the mode-colored prompt and colored
  `board`/`map` ANSI true-color sequences render as intended. Ordinary textual output should remain
  plain. (Needs discussion)
- Reject `exit` inside files executed by `script` so a file cannot shut down server mode. Handle
  comments and whitespace correctly and report the offending line.
- Adapt the PET AST random generator to Kotest property testing if domain-aware shrinking can
  produce smaller failures without replacing its useful recursive generation logic. (Later)
- Add Kotlin binary API validation for the public `pets`, `engine`, `canon`, and `script` APIs and
  integrate ABI snapshot checks with the normal `check` lifecycle.

### Performance

- Profile and reduce type-system allocation hot spots without risking correctness. `MType.glb` was
  observed allocating roughly 28 MB per solo game; `MType.narrows` used about 19% of runtime and
  more than 10 MB. Investigate canonicalization/caching and repeated dependency, refinement, and
  requirement construction. (Later)
