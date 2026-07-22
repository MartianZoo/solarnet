# TODO

This file tracks work that still appears applicable to the current codebase. Public issue links
identify the broader discussion; the text here describes only the part that remains.

Priorities appear in parentheses. An item without a priority has the default priority, **Soon**.

## User Ideas and Agreed Directions

### Too permissive, doesn't block Follow Mode

- [Issue #12: Linked specialization across `THEN`](https://github.com/MartianZoo/solarnet/issues/12)
  — Require entirely identical repeated type expressions whose resolved base type is abstract to
  narrow together within a single effect, including across its colon, or within a `THEN`
  instruction. Link each repeated expression as a whole without separately coupling matching
  nested fragments. Do not link separate comma-delimited instructions. Preserve the existing
  intended behavior of coupled `X` values, payments, colony bonuses, Manutech, Flooding, and Utopia
  Invest. Make each solo setup greenery neighbor the city just placed; make Splice and Viral
  Enhancers put their resource on the card that triggered the effect; make Trade Envoys and Trading
  Colony affect the colony tile just traded with; and make an `ActionUsedMarker` identify the card
  whose action was used. Enable and test the currently disabled Splice, Trade Envoys, and Trading
  Colony definitions once their complete behavior is supported. Ensure Viron can repeat an eligible
  used card action without corrupting either card's action-used marker state.
- [Issue #22: `ELSE`](https://github.com/MartianZoo/solarnet/issues/22) — Implement ordered choice by
  enumerating valid LHS narrowings and using the fallback only if none can finish the explicit LHS.
  Use it for WGT (`GlobalParameter! ELSE Ok`) and Pharmacy Union. Try it for Prelude and Established
  Methods only if their normal instructions expose failure soon enough; otherwise keep their explicit
  fallback handling. This does not solve Greenery.
- [Issue #37: Class-signature linkages](https://github.com/MartianZoo/solarnet/issues/37) — Link
  repeated dependency expressions so a `Cardbound` component and its `CardFront` necessarily share
  one owner, eliminating verbose forms such as `Animal<Predators<Player1>, Player1>`.

### Gameplay Rules and Missing Content

- Implement Terra Cimmeria's MSL Curiosity placement bonus once optional bundle vocabulary can be
  phantom: with Colonies enabled, placing a tile there costs 5 M€ and places a colony; otherwise
  the bonus is ignored.
- Make Head Start completely drain the tasks produced by its first independently selected action
  before the player may select its second independently selected action.
- Check whether Mining Area and Mining Rights have their placement rules switched. Add focused
  tests for both cards, including rejection of an area with no steel or titanium bonus.
- [Issue #28: AMAP and ocean tiles](https://github.com/MartianZoo/solarnet/issues/28) — Make existing
  `.` try amounts from the requested maximum down to zero, choosing the greatest amount that lets
  the containing action finish; if zero does not help, the action still fails. Use this for optional
  card-resource gains such as Local Heat Trapping. Artificial Lake stays
  `OceanTile<LandArea>! OR (9 OceanTile: Ok)`; its focused tests already cover the core ruling.
- [Issue #63: Atmoscoop](https://github.com/MartianZoo/solarnet/issues/63) — Permit an `OR` branch
  to contain an atomized `Multi`, restore Atmoscoop's simultaneous choice, and remove its temporary
  sequential encoding, which exposes ordering choices the card should not provide. (Later)
- [Issue #2: Remaining solo-mode modeling](https://github.com/MartianZoo/solarnet/issues/2) — Model
  a neutral host for card resources, such as the imaginary animal that Predators may remove,
  without giving `Opponent` a playable `CardFront`.
- [Issue #5: Separate available content from enabled rules](https://github.com/MartianZoo/solarnet/issues/5)
  — Represent configuration as signed class-name selections, expanding positive defaults only after
  counteractions have masked them. Resolve active definitions and option singletons independently
  of their provider bundles. Validate ordinary class names against the Authority's full catalog,
  but let behavioral references use only active classes; known-but-inactive atomic changes should
  specialize to `Die` rather than activating their provider content.
- [Issue #13: `OR` triggers](https://github.com/MartianZoo/solarnet/issues/13) — Allow one effect to
  subscribe to alternative triggers so canonical definitions no longer duplicate the same effect.
- [Issue #48: Refinements in trigger types](https://github.com/MartianZoo/solarnet/issues/48) — Make
  trigger matching honor refinements, as needed by effects based on a played card's cost or
  requirement.
- Maybe make Tile extend Atomized

### Language and Engine Semantics

- Replace rollback-based speculation with explicit `GameState` overlays spanning the component
  graph, task queues, event log, and derived active-effect index. Allow an overlay to be discarded
  or atomically promoted by replaying its events onto an unchanged base state.
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

- Implement [World Government Terraforming](plans/world-government.md), replacing the logged-game
  neutral-opponent workaround with an Engine-performed operation chosen by the `StartToken` owner.
  Preserve the plan's Actor/Owner separation and Solar Phase rules. (Somewhat soon)

## Autonomous Follow-ups

### Gameplay Correctness and Test Fidelity

- Test Giant Ice Asteroid removing plants just granted by an opponent's Arctic Algae.
- Test Nitrophilic Moss using Manutech's production rewards to pay its plant loss.
- Test Atmoscoop choosing an already-maxed Venus track over an available temperature raise.
- Test the Hellas south-pole ocean adjacency payout funding its mandatory 6 M€ payment.
- Restore Aridor's production gain for acquiring a new type of tag without adding another one-off
  custom metric, before enabling its definition.
- Model the two Prelude plays as explicit first and second Prelude turns, analogous to action-phase
  turns, so ownership, hooks, and future workflow changes are consistent. (Somewhat soon)
- Give Tharsis Republic an explicit immediate solo-setup production gain rather than treating the
  neutral cities as though Tharsis observed their placement. Decide between `SoloMode: PROD[2]`
  and `PROD[1 / CityTile<Opponent>]` without exposing irrelevant multiplayer work. (Later)
- Decide whether standard-project costs should use the same `Owed` protocol as card payments so
  Kuiper Cooperative can spend asteroids against only the Aquifer and Asteroid projects without
  converting them into unrestricted cash. (Needs discussion)
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
- Add a test/task helper such as `setXTo(n)` so tests can select a coupled scalar without repeating
  the entire reified instruction, especially for variable card-resource payments.
- Enforce coupled type specialization within one instruction: repeated occurrences of the same
  abstract component type must resolve identically, just as repeated `X` scalars do. Kaguya Tech's
  `CityTile<LandArea> FROM GreeneryTile<LandArea>` must not permit two different land areas.
- Add a union metric that counts components matching either of two types only once. Red Ships needs
  this to combine `CityTile` and `SpecialTile` without double-counting `CapitalTile`.
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
- Finish separating the Canon catalog from selected rulesets: Canon should relate signed selectors
  to their defaults and raw providers, then resolve only the bundles required by the surviving
  selection instead of inheriting from `TfmRuleset.Composite`. Preserve the invariant that resolving
  one selection never reads another bundle's payload.
- Replace semantic uses of `CardDefinition.requiredBundles` with requirements on selected option
  singletons. Keep a separate raw provider dependency only if one is genuinely needed; raw bundle
  presence must not enable game rules.
- Reconsider physical bundle names independently of semantic selector and option names; remove
  remaining code dependencies on their current coincidences.
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
- Replace the browser UI's synchronous XHR resource loading with an asynchronous startup boundary,
  so loading larger rulesets cannot block the browser's main thread. Preserve one shared resource
  abstraction across Pets and Canon rather than adding UI-specific fetching.
- Decide whether browser completion should expose `ScriptCompletion` groups and descriptions in a
  selectable menu. The first terminal adapter preserves completion values but intentionally uses
  the terminal library's simpler value-only Tab completion interface.

### Performance

- Profile and reduce type-system allocation hot spots without risking correctness. `MType.glb` was
  observed allocating roughly 28 MB per solo game; `MType.narrows` used about 19% of runtime and
  more than 10 MB. Investigate canonicalization/caching and repeated dependency, refinement, and
  requirement construction. (Later)
