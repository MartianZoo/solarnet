# Integrating `work1` AMAP semantics with `wildtag`

> **Branch integration record:** Delete this file after the integration is committed and verified.

This records settled conflict resolutions between `work1` commit `7a69b7948` and `wildtag` commit
`07902ff92`. If either tip has moved, rerun an in-memory `git merge-tree` simulation from the new
tips before merging. Never inspect another working copy.

## Design decision

Keep `work1`'s quantifier model from [QUANTIFIERS.md](QUANTIFIERS.md). Keep `wildtag`'s independent
`TagHolder`, `WildTagUse`, and printed-tag trigger model. These designs compose; the earlier
`wildtag` dependency fallback does not.

In particular, do not retain this `wildtag` rule:

```kotlin
catch (e: DependencyException) {
  if (intensity == MANDATORY) throw e else NoOp
}
```

It turns every dependency-blocked nonmandatory change into `Ok`. That incorrectly lets Local Heat
Trapping select absent Fish while Pets can receive animals, makes concrete AMAP targets implicit
weak references, and weakens nonmandatory transmutations. `work1` instead distinguishes target
selection, invariant capacity, and missing dependencies; validates positive abstract AMAP targets;
locks early selections; and handles `PER` before quantification.

The shared `CardResource<...> : Owned<Owner>` correction is the same on both branches and should be
kept alongside `wildtag`'s tag hierarchy.

## Known conflicts and resolutions

The simulation at the recorded tips reports nine conflicts.

| Path | Resolution |
| --- | --- |
| `TODO.md` | Union unrelated `wildtag` work. Remove AMAP issue #28 and every Pharmacy Union stale-dependency TODO resolved by `work1`. |
| `PromoCardsExpansion/cards.json5` | Keep `wildtag`'s printed-tag trigger Types and `work1`'s explicit Pharmacy Union alternatives; use the Pets below. |
| `docs/agents/ENGINE.md` | Keep `wildtag`'s current wild-tag model. Keep `work1`'s link to `QUANTIFIERS.md`; do not restore the blanket dependency-no-op sentence. |
| `Instructor.kt` | Keep `work1`'s AMAP feasibility, dependency distinctions, early-selection validation, and `PER` handling. Reapply any unrelated later `wildtag` edits around them. |
| `PrepareTest.kt` | Keep the union, using `work1`'s quantifier cases where the additions overlap. |
| `CanonEffectsTest.kt` | Keep `wildtag`'s printed-tag trigger shapes such as `ScienceTag<Owner, CardFront<Owner>>`; keep the owner-linked card-resource forms such as `Animal<This>`. |
| `BugsTest.kt` | Union unrelated `wildtag` characterizations, but do not restore the resolved Local Heat Trapping, Predators, or Artificial Lake bugs. |
| `LocalHeatTrappingTest.kt` | Keep `work1`'s three boundaries: no holder becomes `Ok`; a positive holder receives animals; an absent or full holder cannot evade another positive holder. |
| `PharmacyUnionTest.kt` | Keep `work1`'s scenarios, including two simultaneous science tags and the explicit pending-disease fallback. Adapt only trigger syntax required by `wildtag`. |

The combined Pharmacy Union effects are:

```pets
MicrobeTag<Anyone, CardFront<Anyone>>: (Disease<This>! OR (MAX 0 This: Ok)), -4.
ScienceTag<>: TerraformRating FROM Disease<This>! OR (MAX 0 Disease: (PlayedEvent<Class<This>> FROM This THEN 3 TerraformRating) OR Ok)
```

`Implementations.kt`, `Limiter.kt`, `TaskRevisionTest.kt`, and `QUANTIFIERS.md` merged automatically
at the recorded tips. Preserve them; automatic textual merging is not permission to discard their
behavior during nearby conflict cleanup.

## Verification

After resolving the merge:

1. Review the final diff for conflict-marker residue and accidental restoration of the blanket
   dependency fallback.
2. Run the focused `PrepareTest`, `TaskRevisionTest`, `LocalHeatTrappingTest`,
   `PharmacyUnionTest`, `PredatorsTest`, and wild-tag tests.
3. Run `./gradlew build` and `git diff --check`.
4. Delete this integration record in the verified merge commit.
