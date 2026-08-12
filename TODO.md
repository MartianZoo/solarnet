# TODO

Only current work belongs here; issue links provide background. Inline TODOs should be brief context pointers. Priorities descend from very soon to low.

## User Ideas and Agreed Directions

### Making Solarnet AI-player-ready

- Move beyond follow-along mode by modeling shuffled decks, deals, draws, and actual private hands.
- Provide one strict player-relative observation and visible-history interface that cannot expose opponents' cards, hidden deck order, or private events; use that same boundary for training, evaluation, and live play.
- Provide a stable, machine-learning-friendly action interface that enumerates or scores complete legal choices while preserving the relationship among the engine's lower-level card, payment, target, quantity, and placement tasks.
- Add reproducible randomness, cheap disposable state forks, parallel or batched simulation, and throughput benchmarks suitable for self-play and online search.
- Add a standard training-environment adapter, baseline player population, replay format or dataset pipeline, and duplicated-seed evaluation harness.
- Complete the rules/content needed by the chosen research configurations, especially unsupported unusual mechanics, Turmoil, and Prelude 2; maintain explicit supported-content manifests so experiments cannot silently use incomplete games.

### Soon

- Use [`docs/agents/SEQUENCING.md`](docs/agents/SEQUENCING.md) to audit and normalize real A-before-B rules; next settle the action-marker/Viron tension and the mixed automatic/queued phase triggers, and keep the verdict buckets current as each case is resolved. Before inventing an automatic `THEN`, distinguish inline continuation, frozen trigger-time choice, and descendant-completion semantics.
- Implement Pluto's individual colony bonus as mandatory draw-then-discard, preserving the order without making the discard an independently available task; test an initially empty hand and observation of intermediate hand state.
- Confirm whether each Mars University discard-to-draw activation is indivisible, then replace the two-discards-first characterization if the official rule permits ordering only whole effects.
- Generalize corporation-play support so Merger can play its second corporation without also assuming corporation-phase card buying or a separate full turn; then remove the raw `PlayCard<CorporationCard>` calls from the solo whole-game fixtures.
- Fix Head Start workflow and convenience-API handling so its first action can leave the additional granted action pending, instead of requiring the solo whole-game fixture to decline one action early.
- Make Artificial Lake's concrete legal ocean placement refine and execute normally, without the solo whole-game fixture's mandatory `!` override.
- Allow milestone and award sets to be selected independently of the map, so the 2026-06-19 whole-game fixture can claim Specialist normally instead of shutting down turn enforcement and manually exchanging 8 M€ for 5 VP.
- Reconstruct the omitted steel/titanium payments in the 2026-07-30 source game, or obtain a log that records payment composition, so its whole-game fixture no longer needs an 8 M€ reconciliation injection.
- [#28: AMAP](https://github.com/MartianZoo/solarnet/issues/28) — Choose the greatest executable amount, including zero only when necessary. Apply this to optional card resources without permitting avoidable ocean placement.
- Model the solo setup choice that selects four colony tiles and removes one before assembling the playable world.
- [#2: Solo mode](https://github.com/MartianZoo/solarnet/issues/2) — Support removing the opponent's card resources.
- Decide how a trigger binds its Actor into an instruction. Mons Insurance must bind both attacker and victim into its payout; `BY !Owner` supplies only the inequality test.
- Audit callers of `GameReader.getComponents()` for queries that should count a `Metric` instead, so custom metrics are not silently omitted.
- During preparation, allow a satisfied gate with an inner `Ok` to reduce to `Ok`; context-free narrowing must preserve the gate.
- Unify `OR` semantics and construction across AST families: reject duplicate authored arms; let programmatic factories deduplicate in first-occurrence order; preserve significant trigger order; and reconcile `Metric.Or` syntax with execution ([#63](https://github.com/MartianZoo/solarnet/issues/63)).
- [#30: Task refinement](https://github.com/MartianZoo/solarnet/issues/30) — Narrow tasks without repeating the full instruction.
- Determine whether gated preparation's loss of `<Anyone>` is harmless canonicalization or an invalid target; document or test the result.
- Restore colors to the interactive REPL while keeping ordinary noninteractive output plain.
- Stop nested bounds in sibling branches of one `<...>` list from linking, so a declaration like `Adjacency<Tile<MarsArea>, Tile<MarsArea>>`, or one repeating the same class literal in two slots, resolves with differing arguments; only a class's own repeated writing of a bound at distinct positions of the same inherited dependency should link, and the shared `Class_0` key makes the class-literal case easy to miss (`docs/agents/TYPES.md` §12.1).
- Tighten complement narrowing so a candidate whose relevant dependency is still abstract is rejected; `SpaceTag` counts as narrowing `SpaceTag<!Player1>` today even though it admits `SpaceTag<Player1>` (`docs/agents/TYPES.md` §12.2).
- Give class signatures the same linkage recognition every other scope uses, so a repeated bound carrying arguments, a refinement, or a `!` links as a unit rather than only its innermost bare class names, and so only abstract occurrences link; the same shared mechanism should subsume the sibling-branch fix above (`docs/agents/TYPES.md` §12.3, §12.1).
- Replace signature-to-effect class-name substitution with real linkage recognition, so effects narrow only at the expressions a linkage names, and a name mapping to two replacements reports a disagreement instead of being silently skipped (`docs/agents/TYPES.md` §12.4).
- Rethink complement types properly rather than patching them. Treating `!X` as a bound-plus-exclusion that is "simpler than a difference type" does not hold up: the domain is dropped from both written forms so a `glb`- or linkage-narrowed complement does not survive re-resolution (§12.5); narrowing accepts candidates whose relevant dependency is still abstract (§12.2); two complements combine only on exact excluded-type equality; `Owned<!Anyone>` resolves but `Owned<Anyone, !Anyone>` has no glb; `ComplementDependency.ensureNarrows` throws `ClassCastException` when the other side is also a complement; a root-marked complement cannot be a linkage source outside a class signature, while a composite expression containing a nested complement still can (§12.6). Decide whether these are genuine difference types before fixing them one at a time.
- Consider modeling multiplicity in the type system itself. The rule that every concrete type a dependency bound admits must have an applicable `MAX 1` or `=1` invariant is what makes a dependency edge designate one component (`docs/agents/TYPES.md` §5), yet the type system never checks it; `Limiter` does, once, at game construction, over active classes only. A type system that knew about multiplicity could reject such a table at load and could express `Atomized` and the `HAS =1 This` idiom directly.

### Medium Soon

- Add a diagnostic engine/test mode that chooses otherwise executable tasks in reverse or reproducibly randomized order, then run the suites under it to expose accidental queue-order dependencies.
- Reduce the remaining custom Pets instructions only where behavior can become hand-authored Pets; start with the candidates and constraints in [`docs/agents/REDUCE_CUSTOM.md`](docs/agents/REDUCE_CUSTOM.md).
- Revamp workflow behavior. Head Start must allow any first action and then grant an additional second action; move Colonies fleet return and colony-track advancement from Production into its Solar subphase after the game-end check.
- Consolidate exception cleanup ([#42](https://github.com/MartianZoo/solarnet/issues/42)): catch only expected script/domain failures, preserve defects and stack traces, use precise MartianZoo exceptions at domain boundaries, narrow `Instruction.narrows`, and start unavailable-operation coverage with Predators lacking a target.
- [#54: Owner-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54) — Resolve contextual ownership correctly and display the resolved player.
- Reorganize Kotlin packages so each Gradle module owns a strong, recognizable package subtree; once ownership is unambiguous, consider merging physical source directories into shared package-shaped trees.
- Model Prelude plays as explicit first and second turns.
- Rewrite removals as payments, covering standard projects and Kuiper Cooperative, Stormcraft, Water Import from Europa, and similar mechanics.
- Rethink task selection holistically; queue order has no domain meaning, so require an id or explicit match unless exactly one task applies.
- Rethink autoexec as a coherent project: distinguish expected domain failures from defects and simplify `autoExecNext`.
- Separate Authority data from premise resolution, and split `TfmAuthority`'s generic declaration aggregation/validation into `Authority` from the Terraforming Mars registries in `TfmAuthority`.
- Extract shared `Definition`-to-`ClassDeclaration` assembly without hiding category-specific behavior.
- Follow `docs/agents/API.md`: simplify the existing engine into a flat, trusted workhorse by removing gameplay power layers and `godMode()`, keeping integrity-preserving mutation internal, and enforcing REPL color modes locally in `script`; design the restrictive client API separately. Then replace rollback speculation with disposable game-state forks that overlay components and live effects, copy the small task queues, and extend event history from a captured prefix.
- Install and configure Kotlin ABI/binary API validation for public `pets`, `engine`, `canon`, and `script` APIs.
- Profile and reduce type-system allocation in `Type.glb`, `narrows`, and repeated dependency/refinement construction without risking correctness.

### Medium Priority

- [#60: Auto-narrowing](https://github.com/MartianZoo/solarnet/issues/60) — Define a small set of rules for unique choices without removing real choices.

### Low Priority

- Prevent Solar Probe's event cleanup from preempting its card draw.
- Implement the standard-game rule that starts every production at 1 when Corporate Era is disabled; this rounds out game modes and demonstrates replacements. Until then, keep canonical requirements at their printed values.
- [#64: Multiple tiles](https://github.com/MartianZoo/solarnet/issues/64) — Decompose `2 CityTile` into two placement choices; consider making `Tile` atomized.
- Model `StateChange` as a sealed gain/remove/transmute algebra so invalid nullable combinations are unrepresentable.
- [#22: `ELSE`](https://github.com/MartianZoo/solarnet/issues/22) — Use the fallback only when no complete narrowing of the first branch works; target WGT and Pharmacy Union first.
- After `OverlayWorld`, revisit task preparation so a prepared task remains authoritative for its world revision instead of being repeatedly prepared, executed speculatively, rolled back, and prepared again.
- [#61: Temporary cleanup](https://github.com/MartianZoo/solarnet/issues/61) — Enforce cleanup at an engine boundary, not only in the convenience layer.
- [#59: `-This` intensity](https://github.com/MartianZoo/solarnet/issues/59) — Decide whether self-removal should default to mandatory.
- [#41: `list`](https://github.com/MartianZoo/solarnet/issues/41) — Improve hierarchy/dependency descent, grouping, depth, concrete subtypes, and explicit `<Anyone>` display.
- Explain or remove `Initializer`'s synthetic mandatory intensity.
- Split `Instructor.prepareChange` into narrowing, custom translation, and limit-checking stages.
- Document the `BuyCard`/payment protocol and verify delayed 3 M€ payment cannot be exploited.
- Move PET AST generation to Kotest property tests only if domain-aware shrinking improves failures.

## Autonomous Follow-ups

- Delete or replace `ReluctantClassLoadingTest`'s exact cumulative card-count assertion; it is a
  pure content change detector and currently requires manual updates whenever a card is added.
- Make class-literal parameters specialize inherited generic effects through a subclass, so solo
  resource helpers can share replenishment behavior without a holder/resource dependency cycle.
- Define the `script` command's relative-path policy and correct its help text, which currently
  promises paths relative to the repository while `File(args)` actually uses the process working
  directory.
- Resolve Floyd Continuum's printed `007` collision with Martian Rails and decide whether the Dutch Open card belongs in canonical scope before replacing its provisional `XM1` identifier.
- Enforce global uniqueness for canonical identifiers during canon loading or in CI, across all object kinds.
- Separate `Instructor`'s preparation-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `PrepareTest`.
- Canonicalize unambiguous authored dependency arguments by key before implicit-variable matching, so equivalent argument orders share a variable as intended (`docs/agents/TYPES.md` §12.7).
- Replace `World.onAtomicComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.
