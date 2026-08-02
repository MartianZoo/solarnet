# TODO

Only current work belongs here; issue links provide background. Inline TODOs should be brief context pointers. Priorities descend from very soon to low.

## User Ideas and Agreed Directions

### Very Soon

- Implement [World Government Terraforming](plans/world-government.md) as an Engine operation chosen by the `StartToken` owner, preserving Solar Phase and Actor/Owner rules.
- Implement Prelude's TR 63 solo option, including Buffer Gas and the 14-generation/12-with-Prelude objective check described in `plans/native-workflow.md`.
- Give Tharsis Republic an explicit solo-setup production gain instead of observing neutral-city placement.
- Load Elysium milestones in `Game20260619Test` and claim Specialist normally.
- Give `Gain` and `Remove` convenience factories that accept an expression and count without requiring callers to construct a `ScaledExpression`.

### Soon

- Treat linkage semantics as one project ([#12](https://github.com/MartianZoo/solarnet/issues/12), [plan](plans/linkages.md)): link identical abstract expressions within one effect or `THEN`, but not across comma-separated instructions or sibling argument positions; validate every shared `X` directly against one multiplier instead of traversal-order zipping. This affects solo setup, Kaguya Tech, Flooding, Utopia Invest, Splice, Trade Envoys, Trading Colony, action-used markers, and nested dependency declarations.
- [#28: AMAP](https://github.com/MartianZoo/solarnet/issues/28) — Choose the greatest executable amount, including zero only when necessary. Apply this to optional card resources without permitting avoidable ocean placement.
- Replace `DeferredColonySelection` with setup-world choices collected before assembling the playable world; solve the corresponding solo setup choice that removes one colony tile.
- [#2: Solo mode](https://github.com/MartianZoo/solarnet/issues/2) — Support removing the opponent's card resources.
- Decide how a trigger binds its Actor into an instruction. Mons Insurance must bind both attacker and victim into its payout; `BY !Owner` supplies only the inequality test.
- Audit callers of `GameReader.getComponents()` for queries that should count a `Metric` instead, so custom metrics are not silently omitted.
- During preparation, allow a satisfied gate with an inner `Ok` to reduce to `Ok`; context-free narrowing must preserve the gate.
- Make instruction gating (`:`) bind less tightly than `OR`, simplifying `R: (A OR B)` while requiring parentheses around gated alternatives.
- Unify `OR` semantics and construction across AST families: reject duplicate authored arms; let programmatic factories deduplicate in first-occurrence order; preserve significant trigger order; reconcile `Metric.Or` syntax with execution; and allow atomized `Multi` arms so Atmoscoop can restore simultaneous track raises ([#63](https://github.com/MartianZoo/solarnet/issues/63)).
- [#30: Task refinement](https://github.com/MartianZoo/solarnet/issues/30) — Narrow tasks without repeating the full instruction.
- Determine whether gated preparation's loss of `<Anyone>` is harmless canonicalization or an invalid target; document or test the result.
- Restore colors to the interactive REPL while keeping ordinary noninteractive output plain.

### Medium Soon

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
- Follow `docs/engine-api-review.md`: simplify the existing engine into a flat, trusted workhorse by removing gameplay power layers and `godMode()`, keeping integrity-preserving mutation internal, and enforcing REPL color modes locally in `script`; design the restrictive client API separately. Then replace rollback speculation with disposable game-state forks that overlay components and live effects, copy the small task queues, and extend event history from a captured prefix.
- Reject truncated class declarations instead of treating unexpected EOF as the end of the declaration stream.
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

None currently.
