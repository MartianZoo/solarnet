# TODO

Only current work belongs here; issue links provide background. Inline TODOs should be brief context pointers. Priorities descend from very soon to low.

## User Ideas and Agreed Directions

### Soon

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
- Stop nested bounds in sibling branches of one `<...>` list from linking, so a declaration like `Adjacency<Tile<MarsArea>, Tile<MarsArea>>`, or one repeating the same class literal in two slots, resolves with differing arguments; only a class's own repeated writing of a bound at distinct positions of the same inherited dependency should link, and the shared `Class_0` key makes the class-literal case easy to miss (`docs/agents/TYPES.md` §15.1).
- Make `Type.lub` honor refinements: with exactly one refined side, `A(HAS R) lub A` yields `A(HAS R)`, which `A` does not narrow; with two refined sides, even `A(HAS R) lub A(HAS R)` drops the shared refinement and is not least (`docs/agents/TYPES.md` §15.2 currently records only the first case).
- Tighten complement narrowing so a candidate whose relevant dependency is still abstract is rejected; `SpaceTag` counts as narrowing `SpaceTag<!Player1>` today even though it admits `SpaceTag<Player1>` (`docs/agents/TYPES.md` §15.3).
- Give subtype checks a static shortcut when the wider side is refined, so `A(HAS R) <: A(HAS R)` decides without a world instead of raising under `NoGameState` (`docs/agents/TYPES.md` §15.4).
- Make the minimal written form round-trip for same-bounded dependencies; `Adjacency<Tile, CityTile>` prints `Adjacency<CityTile>`, which re-resolves to `Adjacency<CityTile, Tile>`, and re-enable the commented-out `TypeTest.roundTrip` cases (`docs/agents/TYPES.md` §15.5).
- Give class signatures the same linkage recognition every other scope uses, so a repeated bound carrying arguments, a refinement, or a `!` links as a unit rather than only its innermost bare class names, and so only abstract occurrences link; the same shared mechanism should subsume the sibling-branch fix above (`docs/agents/TYPES.md` §15.6, §15.1).
- Replace signature-to-effect class-name substitution with real linkage recognition, so effects narrow only at the expressions a linkage names, and a name mapping to two replacements reports a disagreement instead of being silently skipped (`docs/agents/TYPES.md` §15.7).
- Make `Type.singleConcreteSubtype` respect refinements and complements, so automatic narrowing cannot return an abstract type or a concrete type the refinement excludes, and so incompatible bounds report no such subtype rather than throwing `NullPointerException` (`docs/agents/TYPES.md` §15.8).
- Make `Refinement.join` return no greatest lower bound when the two refinements disagree about `HAS?`, rather than ORing the flag and letting the escape clause discard a strict requirement; having no glb is an ordinary outcome and preferable to teaching glb more about refinements (`docs/agents/TYPES.md` §15.10).
- Stop a `Class<...>` metric argument from being activated, so counting a class literal never drags the counted class into the table, and make an authority-unknown name there fail when the mentioning declaration loads rather than when the metric is evaluated (`docs/agents/TYPES.md` §15.11, §3).
- Detect a phantom dependency bound while the class loads instead of inside the lazily computed dependency set, without giving up the laziness that dependency cycles need (`docs/agents/TYPES.md` §15.12).
- Rethink complement types properly rather than patching them. Treating `!X` as a bound-plus-exclusion that is "simpler than a difference type" does not hold up: the domain is dropped from both written forms so a `glb`- or linkage-narrowed complement does not survive re-resolution (§15.9); narrowing accepts candidates whose relevant dependency is still abstract (§15.3); two complements combine only on exact excluded-type equality; `Owned<!Anyone>` resolves but `Owned<Anyone, !Anyone>` has no glb; `ComplementDependency.ensureNarrows` throws `ClassCastException` when the other side is also a complement; a root-marked complement cannot be a linkage source outside a class signature, while a composite expression containing a nested complement still can (§15.13). Decide whether these are genuine difference types before fixing them one at a time.
- Consider modeling multiplicity in the type system itself. The rule that every concrete type a dependency bound admits must have an applicable `MAX 1` or `=1` invariant is what makes a dependency edge designate one component (`docs/agents/TYPES.md` §5), yet the type system never checks it; `Limiter` does, once, at game construction, over active classes only. A type system that knew about multiplicity could reject such a table at load and could express `Atomized` and the `HAS =1 This` idiom directly.
- Complete `docs/agents/TYPES.md`'s coverage of the current type API: document dependency-level `lub` (including complement behavior), custom implementations' `requiredClassNames` activation edges, exact-root concrete-type enumeration, and the pre-freeze lifecycle restrictions on whole-table and subclass enumeration.

### Medium Soon

- Support selecting milestone and award sets independently of the map, including flexible combinations for logged games.
- Revamp workflow behavior. Head Start must allow any first action and then grant an additional second action; move Colonies fleet return and colony-track advancement from Production into its Solar subphase after the game-end check.
- Consolidate exception cleanup ([#42](https://github.com/MartianZoo/solarnet/issues/42)): catch only expected script/domain failures, preserve defects and stack traces, use precise MartianZoo exceptions at domain boundaries, narrow `Instruction.narrows`, and start unavailable-operation coverage with Predators lacking a target.
- [#54: Owner-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54) — Resolve contextual ownership correctly and display the resolved player.
- Reorganize Kotlin packages so each Gradle module owns a strong, recognizable package subtree; once ownership is unambiguous, consider merging physical source directories into shared package-shaped trees.
- Model Prelude plays as explicit first and second turns.
- Rewrite removals as payments, covering standard projects and Kuiper Cooperative, Stormcraft, Water Import from Europa, and similar mechanics.
- Rethink task selection holistically; queue order has no domain meaning, so require an id or explicit match unless exactly one task applies.
- Rethink autoexec as a coherent project: distinguish expected domain failures from defects and simplify `autoExecNext`.
- Finish separating Canon selectors/providers from selected rulesets; never read an unselected bundle's payload.
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
- Give component and task mutations a single event-application boundary so primary state, derived indexes, and event history cannot be updated independently.
- Explain or remove `Initializer`'s synthetic mandatory intensity.
- Split `Instructor.prepareChange` into narrowing, custom translation, and limit-checking stages.
- Document the `BuyCard`/payment protocol and verify delayed 3 M€ payment cannot be exploited.
- Move PET AST generation to Kotest property tests only if domain-aware shrinking improves failures.

## Autonomous Follow-ups

- Separate `Instructor`'s preparation-only capability from execution so `Changer`, `Effector`, and
  the default Actor do not remain nullable solely for `PrepareTest`.
- Canonicalize unambiguous authored dependency arguments by key before implicit-variable matching, so equivalent argument orders share a variable as intended (`docs/agents/TYPES.md` §15.14).
- Replace `World.onAtomicComplete`'s mutable single callback with scoped listener registration once
  multiple workflow or monitoring observers need to coexist.
