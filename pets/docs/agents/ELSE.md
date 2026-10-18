# ELSE candidates

> **Status:** exploratory, not implemented. Read when considering fallback instructions.

`A ELSE B` permits B only when A is impossible. A must not contain a comma-separated
instruction group, including inside an OR.

- **Shallow:** A itself cannot complete, including immediate rejection and invariant checks.
- **Deep:** every legal completion of the turn through A fails, considering alternative targets,
  task orders, and help from other queued work.

| Case | Useful version | Benefit or restriction |
| --- | --- | --- |
| Pharmacy Union: pending disease after flipping | Shallow sufficient | **Cosmetic:** `Disease<This>! ELSE Ok` replaces the absence check; the separate money-loss task remains. |
| Pharmacy Union: science tag | Shallow | **Cosmetic:** disease-for-TR ELSE optional flip. Deep could wrongly skip disease removal because its Reds bill is unaffordable. |
| Suitable Infrastructure | Shallow sufficient | **Cosmetic:** `(-This THEN 2 MC) ELSE Ok` replaces presence/absence gates around claiming the bonus allowance. |
| L1 Trade Terminal | Shallow | **Simplification, unverified:** choose three distinct eligible recipients successively, stopping only when none remain. Could remove gift counters and watcher; no demonstrated Canon bug. |
| PlayPrelude and its callers | Deep | **Stronger validation:** replace the externally audited fizzle declaration with proof. Recession/Early Colonization can fail late; Pluto bonuses or Project Eden placement order can rescue play (`DeadEndsTest`). Requires the declared face, not anonymous backs. |
| Nitrogen-Rich Asteroid / Io Sulphur Research | Neither | **CORRECT BEHAVIOR:** deliberately taking the lesser benefit despite sufficient tags is allowed. Preserve OR. |
| Lava Flows / Lava Tube Settlement / Noctis City | Neither as a direct replacement | Fallback requires the map to lack the named area category, not merely lack successful placements. |

Instruction-level `(A)*`, meaning **do A unless impossible**, may suffice for the `ELSE Ok`
cases: pending disease, Suitable Infrastructure, and L1's recipient steps. These need shallow
semantics, not permission to decline a possible A. This differs from count-based AMAP.
Greenery's last-space case is expressed by the `PlaceGreeneryTile` area-piece query, without `ELSE`.
Prelude compensation needs a real B.

Sources: [board](../../src/common/dev/martianzoo/tfm/canon/bundles/base/board.pets),
[card play](../../src/common/dev/martianzoo/tfm/canon/bundles/base/card-model.pets),
[promos](../../src/common/dev/martianzoo/tfm/canon/bundles/promos/cards.json5),
[Prelude 2](../../src/common/dev/martianzoo/tfm/canon/bundles/prelude2/cards.json5).
