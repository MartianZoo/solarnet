# TODO

Only current work belongs here; issue links provide background. Inline TODOs should be brief context pointers.

## User Ideas and Agreed Directions

### Known Incorrect Behavior

- [#12: Linked specialization](https://github.com/MartianZoo/solarnet/issues/12) — Link identical abstract expressions within one effect or `THEN`, but not across comma-separated instructions. This affects solo setup, Kaguya Tech, Flooding, Utopia Invest, Splice, Trade Envoys, Trading Colony, and action-used markers.
- Make Head Start finish its first selected action before selecting the second.
- [#28: AMAP](https://github.com/MartianZoo/solarnet/issues/28) — Choose the greatest executable amount, including zero only when necessary. Apply this to optional card resources without permitting avoidable ocean placement.
- [#63: Atmoscoop](https://github.com/MartianZoo/solarnet/issues/63) — Allow atomized `Multi` instructions inside `OR`, then restore simultaneous track raises. (Later)
- Prevent Solar Probe's event cleanup from preempting its card draw.

### Gameplay and Content

- Implement Terra Cimmeria's MSL Curiosity bonus once optional bundle vocabulary can be phantom: pay 5 M€ and place a colony only with Colonies enabled.
- [#2: Solo mode](https://github.com/MartianZoo/solarnet/issues/2) — Support removing the opponent's card resources.
- [#48: Refinement-typed triggers](https://github.com/MartianZoo/solarnet/issues/48) — Honor trigger refinements, including card cost and requirement metrics. Uh but apparently this works already?
- [#64: Multiple tiles](https://github.com/MartianZoo/solarnet/issues/64) — Decompose `2 CityTile` into two placement choices; consider making `Tile` atomized.

### Language and Engine Semantics

- Audit callers of `GameReader.getComponents()` for queries that should count a `Metric` instead, so custom metrics are not silently omitted.
- Consider letting custom metrics query typed, read-only event history for facts such as `HasRaisedTr` and this-generation attacks, avoiding permanent watchers; preserve semantic generation boundaries and rollback/replay determinism.
- [#22: `ELSE`](https://github.com/MartianZoo/solarnet/issues/22) — Use the fallback only when no complete narrowing of the first branch works; target WGT and Pharmacy Union first.
- Replace rollback speculation with disposable `GameState` overlays spanning components, tasks, events, and active effects.
- [#24: Distinct classes](https://github.com/MartianZoo/solarnet/issues/24) — Define a generic owner-associated distinct-class metric, then replace `DistinctTagType` and `DistinctResourceType` if it is sound.
- [#29: Incremental `THEN`](https://github.com/MartianZoo/solarnet/issues/29) — Narrow linked/coupled instructions together, execute the concrete head, then enqueue an abstract tail. Maybe we did all this already?
- [#60: Auto-narrowing](https://github.com/MartianZoo/solarnet/issues/60) — Define a small set of rules for unique choices without removing real choices.
- [#61: Temporary cleanup](https://github.com/MartianZoo/solarnet/issues/61) — Enforce cleanup at an engine boundary, not only in the convenience layer.
- [#59: `-This` intensity](https://github.com/MartianZoo/solarnet/issues/59) — Decide whether self-removal should default to mandatory. (Needs discussion)
- Decide whether a whole gated instruction may narrow to `Ok`; restore the disabled `ReifyTest` case if so.

### User-Facing Behavior

- [#42: Failure reporting](https://github.com/MartianZoo/solarnet/issues/42) — Catch only expected script errors; preserve defects, stack traces, and appropriate server logging.
- [#30: Task refinement](https://github.com/MartianZoo/solarnet/issues/30) — Narrow tasks without repeating the full instruction.
- [#41: `list`](https://github.com/MartianZoo/solarnet/issues/41) — Improve hierarchy/dependency descent, grouping, depth, concrete subtypes, and explicit `<Anyone>` display.
- [#46: Card identity in `list`](https://github.com/MartianZoo/solarnet/issues/46) — Do not collapse concrete cards into one abstract row.
- [#54: Owner-sensitive `count`](https://github.com/MartianZoo/solarnet/issues/54) — Resolve contextual ownership correctly and display the resolved player.

### Platform Reach

- Implement [World Government Terraforming](plans/world-government.md) as an Engine operation chosen by the `StartToken` owner, preserving Solar Phase and Actor/Owner rules. (Somewhat soon)

## Autonomous Follow-ups

### Gameplay Correctness and Test Fidelity

- Test the Hellas south-pole ocean payout funding its mandatory 6 M€ payment.
- Restore Aridor's new-tag production gain without another one-off metric, then enable it.
- Model Prelude plays as explicit first and second turns. (Somewhat soon)
- Give Tharsis Republic an explicit solo-setup production gain instead of observing neutral-city placement. (Later)
- Decide whether standard projects should use `Owed`, allowing Kuiper Cooperative asteroids only on Aquifer and Asteroid. (Needs discussion)
- Determine whether gated preparation's loss of `<Anyone>` is harmless canonicalization or an invalid target; document or test the result.
- Give unavailable gameplay operations precise domain exceptions; start with Predators lacking a target.
- Load Elysium milestones in `Game20260619Test` and claim Specialist normally.

### Engine Safety and Maintainability

- Make abstract custom metrics automatically enumerate concrete dependency specializations satisfying their refinements, invoke the custom implementation for each fully concrete type, and sum the results.
- Give custom implementations an explicit declaration of the types they require so loading the custom class also loads those types.
- Replace or constrain `doFirstTask()`; require an id/match unless exactly one task applies.
- Validate every shared `X` in a `THEN` against one multiplier without traversal-order zipping.
- Add `setXTo(n)`-style test/task refinement. And `setTo(from, to)`.
- Add a duplicate-free OR metric for Red Ships' `CityTile OR SpecialTile` count.
- Narrow speculative preparation/autoexec failures to domain errors; surface defects and simplify `autoExecNext`.
- Narrow boolean compatibility probes so `Instruction.narrows` does not hide broken invariants. (Needs discussion)
- Decide whether turn/action signals are generic engine protocol or Terraforming Mars rules, then colocate declarations and interpretation.
- Make only choice-free setup effects automatic; consider immediate `Photosynthesis` creation. (Later)
- Finish separating Canon selectors/providers from selected rulesets; never read an unselected bundle's payload.
- Preserve `Opponent` when narrowing only a queued solo tile's area.
- Explain or remove `Initializer`'s synthetic mandatory intensity.
- Remove whole-game-empty queue assumptions from Head Start helpers/tests.
- Separate corporation-task selection from `playCorp` for Merger and logged-game tests.
- Extract shared `Definition`-to-`ClassDeclaration` assembly without hiding category-specific behavior.
- Decide whether humanized Terraforming Mars readers belong on `TfmGameplay`; document the boundary. (Somewhat soon)
- Simplify AST transforms: centralize child copying, type `Change` operations, make atomization stateless, and name owner replacement. (Needs discussion)
- Follow `docs/engine-api-review.md`: replace `godMode()` and layer casts with explicit roles; defer script access cleanup. (Later)
- Simplify and document default-dependency normalization, especially contextual `Owner` handling.
- Narrow `MutableGrid` bounds handling instead of catching every exception.
- Document the `BuyCard`/payment protocol and verify delayed 3 M€ payment cannot be exploited.

### Diagnostics and Tooling

- Make `task <id> Ok` notify workflow when it removes the final task.
- Diagnose the production Kotlin/JS `Atomized` parse failure; add a production-distribution smoke test.
- Improve better-parse errors with one typed validation path, structured alternatives, and source locations.
- Explain missing “currently impossible” text for `MAX 0 Barrier`; decide whether the extra output blank line is intentional.
- Extend the real-terminal smoke test to cover prompt and `board`/`map` colors while keeping ordinary output plain. (Needs discussion)
- Move PET AST generation to Kotest property tests only if domain-aware shrinking improves failures. (Later)
- Add binary API validation for public `pets`, `engine`, `canon`, and `script` APIs.
- Replace synchronous browser XHR with asynchronous startup behind one Pets/Canon resource abstraction.
- Decide whether browser completion needs a grouped, described selection menu.

### Performance

- Reduce type-system allocation without risking correctness; profile `MType.glb`, `narrows`, and repeated dependency/refinement construction. (Later)
