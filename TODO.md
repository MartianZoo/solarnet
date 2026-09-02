<!-- Only miscellaneous work not already covered by a focused grand-plan document in docs/agents/ belongs here. -->

# TODO

Issue links provide background. Inline TODOs should be brief context pointers.

## User Ideas and Agreed Directions

- Weed the vague terms `operation` and `gameplay command` out of the engine. Rename each use for
  the exact lifecycle it denotes, including atomic calls, task completion, and workflow play.
- Discard the uncommitted typed custom-metric/code-generation experiment; it was evaluated and
  considered an unsuccessful direction.
- IndustrialComplex should top up to 2 production if in QuickStartVariant (add to BugsTest anytime).
- Complete `Game20260820Test` beyond its current partial generation-6 checkpoint using the preserved
  log, player data, and eight later screenshots; keep every new checkpoint independently sourced.
- Install and configure Kotlin ABI/binary API validation for public `pets`, `engine`, `tfm-canon`,
  and `script` APIs.
- Profile and reduce type-system allocation in `Type.glb`, `narrows`, and repeated
  dependency/refinement construction without risking correctness.
- **Medium-high priority:** Consolidate exception cleanup
  ([#42](https://github.com/MartianZoo/solarnet/issues/42)): catch only expected script/domain
  failures, preserve defects and stack traces, use precise MartianZoo exceptions at domain layers,
  and narrow `Instruction.narrows`.
- **Low priority:** [#54: Owner-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54)
  — Resolve contextual ownership correctly and display the resolved player.
- **Low priority:** Consider compiling Pets during the build into validated runtime artifacts, but
  only if one compiler can replace runtime parsing/validation without creating a second semantic
  model.
- **Low priority:** Investigate why the oxygen steps created by SoloOpponent's setup greeneries do
  not award it TR, and whether adding and then removing those steps has any other observable
  consequences.
- **Low priority:** [#41: `list`](https://github.com/MartianZoo/solarnet/issues/41) — Improve
  hierarchy/dependency descent, grouping, depth, concrete subtypes, and explicit `<Anyone>` display.
- Model `StateChange` as a sealed gain/remove/transmute algebra so invalid nullable combinations are
  unrepresentable.
- [#59: `-This` Quantifier](https://github.com/MartianZoo/solarnet/issues/59) — Decide whether
  self-removal should default to mandatory.
- Explain or remove `Initializer`'s synthetic mandatory Quantifier.
- Split `Instructor.resolveChange` into narrowing, custom translation, and limit-checking stages.
- Have the payment lowering in `Transforming.actionToEffects` receive its standard-resource Class
  names from `tfm-canon` instead of the hardcoded `standardResourceClasses` set in `pets`; that set is
  Terraforming Mars data sitting in the language core, and it is the only reason generic Action
  lowering knows any game's vocabulary.
- Delete `Task.whyPending`'s use as state. Only `"abstract"` and `"currently impossible"` are ever
  stored, and `"abstract"` is recomputed from `instruction.isAbstract(reader)` in `explainNotNow`
  anyway, yet `Implementations.requireComplete` and `TfmGameplay` branch on the string literal. Ask
  the instruction, and leave `whyPending` a pure diagnostic message.
- Delete `pets/util/PairingChecker.kt`, along with its own tokenizer and grammar. It has no
  production caller; `PairingCheckerTest` is its only reference in the repository.
### Hypothetical Card Behavior

- Make `VictoryPoint` depend on the scoring `Component`, and define a scoring-completion phase if a
  future score depends on another score rather than directly on game state.
- Give multiple wild tags on one card distinct occurrences if a future card has two wild tags, so
  both can be assigned either the same tag or different tags for one action; otherwise document the
  limitation.
- Decompose a future card's `2 CityTile` instruction into two placement choices; consider making
  `Tile` atomized ([#64](https://github.com/MartianZoo/solarnet/issues/64)).
- Give players 20 TR in multiplayer setup and 14 TR in solo setup directly if a future card can
  observe the current 20-then-minus-6 solo sequence.

## Autonomous Follow-ups

- Reduce recorded-game viewer loading allocation, starting with repeated `DependencySet`
  iteration/lookups and abstract `ComponentGraph` count queries; validate changes with
  `SavedGameReplayBenchmark`.
- Serve copied Canon resources from the game-viewer Karma configuration; the resources reach the
  test package, but `:game-viewer:jsBrowserTest` currently gets a 404 for
  `canon/resource-index.txt`.
- Model Mars Nomads' moving non-tile marker, adjacency and reservation rules, and destination
  placement bonuses, then remove the replay's test-only stand-in and sourced reconciliations.
- Investigate the intermittent Kotlin/Karma reporter crash during the unfiltered engine browser
  suite: targeted browser suites and the normal smoke test pass, but the reporter can lose a
  successful spec's console result and terminate the full run.
- Break `PetTransformer.transformChildren` into focused rebuild helpers; its instruction-tree
  support has made the existing cyclomatic-complexity suppression increasingly costly to maintain.
- Complete the unsupported Milestones & Awards goals: Briber's special claim cost, Hydrologist and
  Thawer's player-attributed global-parameter steps, and the Turmoil-dependent Lobbyist and
  Politician rules.
- Define the `script` command's relative-path policy and correct its help text, which currently
  promises paths relative to the repository while `File(args)` actually uses the process working
  directory.
- Simplify `LiveEffect` actor binding by threading a binding context through subscription matching
  instead of maintaining parallel `Subscription.transform()` implementations and `Hit.before()`.
- Separate `Instructor`'s resolution-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `InstructionResolutionTest`.
- Replace `World.onAtomicComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.
- Share the one three-valued abstract interpreter over Pets that currently exists twice. `Truth`,
  `truthOfAll`, `truthOfAny`, `isUninhabited`, `truthOf`, and `triggerReachable` appear as private
  members of both `ClassLoader` and `PremiseViability`, near-verbatim; `ClassLoader`'s copy alone
  additionally consults `configuredCount`. The two run adjacently in one pipeline —
  `ClassTable.forPremise` calls `PremiseViability.validate` right after loading — so extracting one
  implementation parameterized by the inhabitance question each caller answers is safe, and leaves
  uninhabited-Type reasoning with a single definition.
