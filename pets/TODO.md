# TODO

Delete entries when they are done.

- Deeper shorthand RANK inference is deferred unless a new concrete use justifies it.
  Collector's `Class<@Resource>(HAS @Resource, NOT Class<RobotUnit>)` and Excentric's
  `CardResource<CardFront>` need the competitor's ownership below the outermost expression.
  Temporarily removing `Me@Player` from award scoring awarded undeserved first places for both
  and broke Mogul's production metric. Award scoring therefore retains its explicit `RANK Me@Player`
  binding. A broader implicit rule must preserve the accepted meaning of explicitly written `Me`
  as the outer owner. Do not solve this by silently rebinding that name.
  The current Canon RANK inventory supplies no further need for this deeper inference. Existing
  award metric declarations work unchanged with their shared explicit binding.

- Continue the language-complexity audit with at least two concrete Canon beneficiaries for each
  retained wrinkle. Nearest-ancestor override precedence (T10-4) still has no identified Canon
  dependency-default example; investigate whether it is necessary.

- Consider moving the remaining configuration policies in `autoSelectWhen` and
  `premiseRequirement` out of Pets properties. Pairwise implementation incompatibilities already
  live in card JSON and are checked as catalog metadata; broader policy changes are deferred.

- Finish selection of concrete `Content` subclasses: support all applicable items from a bundle,
  independent party selection, and map selection separate from its Module role. Supporting
  declarations still use file membership and category-specific availability rules; `Content`
  identifies game items, not which helpers belong to them. See `docs/agents/CATALOG.md`.

- Extend Canon XPath queries with expression-level type matching and linked variable identity when
  actual queries require them. Search exposes nominal superclass closure and variable
  spelling; it does not prove that conditional paths are achievable or calculate combined gains.
  The MC-offset query still spells out its subtraction patterns; defer broader metric normalization
  until another real query needs it rather than adding an invariant-specific pass/fail attribute.

- Isolate the Kotlin incremental-compilation failure in the generated Canon check before trying
  to re-enable it. Merge builds reported duplicate helpers despite a single generated source file;
  clean compilation succeeded. The check now compiles the complete file when inputs change.
  Simple generated-class additions and renames did not reproduce the original failure.

- Separate Mars vocabulary and card presentation from `text`, whose name is aspirational.
  `TerraformingMarsDescribers` and eight expansion lexicons currently compile in this module.
  `English`, `Describers`, and `ExpressionResolver` import Canon helpers/constants; other
  interpreters also recognize named model classes, including PreludePhase and GenerationScope.
  First remove the renderer's production dependency on Canon, keeping generic grammar and
  common card/resource interpretation in text. Then Canon can depend on text and supply its
  vocabulary and card integration without a dependency cycle. Do not introduce a second
  semantic model or move generic grammar into Canon. Keep the existing rendered-card tests
  as evidence while separating the modules.

- Continue adversarial Pets testing beyond parser nesting and nominal inheritance: investigate
  dependency/default expansion, concrete-type enumeration, and automatic-effect cascades. The
  current depth and subtraction limits are not a general execution-work budget.

- Skip equality-incompatible subclasses in concrete-type queries (T11-1, T11-3, and T12-4).
  Pets `BugsTest` shows `Pair<First, Second>` should retain `Independent<First, Second>` and omit
  `Same`, whose two arguments must agree. Enumeration and automatic narrowing instead throw while
  constructing `Same<First, Second>`; inhabitance returns true or throws depending on declaration order.

- Exclude subclasses with empty dependency differences during automatic narrowing (T11-3).
  Pets `BugsTest` shows `Tile<Area(NOT Land)>` correctly enumerates only `Ocean`, but automatic
  narrowing returns no result because it also counts `Greenery` with the impossible `Land(NOT Land)`.

- Preserve represented-class bindings when rendered types are parsed again (T5-6 and T8-10).
  Pets `BugsTest` shows a predicate that asks about `BuildingTag<Player1>` before rendering
  incorrectly asks about `Tag<Player1>` afterward. A repair must also handle candidate classes
  with fixed dependencies and nested declarations sharing the candidate's bound without losing
  their distinct bindings.

- Recognize the same represented-class predicate under a narrower bound (T8-1 and T8-10).
  Pets `BugsTest` shows `Class<@BuildingTag>(HAS @BuildingTag)` incorrectly fails to narrow
  `Class<@Tag>(HAS @Tag)`, although both predicates ask about the selected class.

- Improve the bracketed English fragments exposed by removing the exact-expression phrase table,
  using ordinary composition only where it stays small. Marker effects, distinct-type filters,
  and L1 Trade Terminal’s gift watcher remain partly unsupported. The shared Venus resource addition
  also retains bracketed Pets.
  Philantropist's class-token requirement now
  names cards in play but retains bracketed `GainsOf`. A card instance cannot fill that metric's
  `Class<CardFront>` dependency; Astra Mechanica also evaluates event classes after those cards leave play.
- Refresh the remaining `docs/english-wording-comparisons/` entries against current Canon and
  rendering. Entries touched by the delegate work are current; older entries still show previous
  invariant declarations and wording.

- Decide a useful static-analysis policy for the shared source layout. The previous Detekt
  tasks saw no source in these modules; the Pets build preserves compiler, formatting, JVM,
  and browser checks without importing that inert configuration.

- Investigate represented-tag binding before shortening
  `@Tag<WildTagElection<@WildTag, ActionScope>>` to `@Tag<This>`. The shorter spelling still fails in
  `recordTypeVariableScopes` while `This` is lexical; the explicit election holder works. See
  [represented-tag bindings](docs/agents/ELABORATION.md#represented-tag-bindings).

- Continue the selected-task English experiment using `English.describe(instruction, player)`.
  Preserve named awards and card-play targets; support other-player resource changes, variable
  payments, and remaining selection/reveal operations. Recognized Audit steps are omitted; interpret
  remaining billing obligations before hiding any additional bookkeeping. The Solarnet-local input is
  `_local/replay-selected-task-instructions.txt`; the comparison is `_local/replay-task-english.txt`.
  Do not treat bracket-free counts as UI readiness.

- If view construction remains expensive after the selection worklist change, measure the remaining
  unresolved-condition passes before adding more machinery. They still reconsider every pending
  constructive reference after each inclusion frontier; selectively waking only affected conditions
  would require tracking subclass and dependency inhabitance, not just names written in a trigger.

- Head Start remains an approximation: its resources and two granted actions can interleave and
  share the enclosing scope, so Suitable Infrastructure pays only once and wild-tag elections
  persist. Keep these limitations explicit; revisit only if existing mechanisms can model its
  timing without special turn-flow machinery. See [game flow](docs/agents/GAME_FLOW.md#head-start-restored-approximation).

- Revisit initial `Baton: OfferAction` forwarding only if it reduces the whole turn model.
  Final greenery uses it. A bounded alternative passed the turn/scope tests but needed a deferred
  initial offer, an exception in Baton's shared completion rule, and a separate first-action
  completion listener in `InRound`; it was reverted rather than exchanging repetition for exceptions.
  Consolidate corporation/beginner offers only if the result is clearer.
  A guarded starting-Preludes-in-`Selecting` experiment did not provide the hoped-for generic hand
  check: corporation cleanup discarded them, excluding Preludes from cleanup retained New Partner's
  unused offer, and purchased project cards kept `CardBack<Hand>` nonempty after corporation play.
  The experiment was reverted; any follow-up must distinguish retained cards from temporary offers
  without adding disproportionate machinery.
