# English renderer: guidebook

> Supersedes `LANGUAGE_REVIEW.md`. That document diagnosed a problem and proposed a direction; the
> direction is now proven in code, so this one is about finishing.
>
> The settled decisions (D1–D13) in `LANGUAGE_REVIEW.md` §2 still hold. Nothing here overrides them.
>
> Paths below use the current layout: `src/jvm/dev/martianzoo/tfm/text/`.

---

## 0. How to use this

**First, spend five minutes measuring.** Every number below was read from the source on a specific
date and may already be stale. Run the counts yourself before relying on any of them:

```
lines · return null · NounPhrase.text · describer fields · frame variants · unresolved nodes
```

Where this document and the code disagree about a *fact*, the code is right — correct the document.
Where they disagree about a *direction*, the disagreement is worth surfacing rather than silently
resolving.

**Then read §3.** It is the picture of where this ends up, and it is the only section that matters if
you read nothing else. §1 and §2 are context; §4–§6 are working discipline.

---

## 1. Where you are

*Measured 2026-08-22, after the `tfm-text` rename.*

**The central bet paid off.** `ChangeFrame` works. `renderChangeOrNull` became one exhaustive `when`
over eight frames instead of a nine-step probe ladder, `DirectChange`'s ten one-off variants are
gone, and `ComponentDescriber` went from 39 fields to 27. That is the design working, and it is the
template for everything that remains.

Three other things landed well and deserve saying plainly:

- **`ExpressionResolver`** does the real thing — `classTable.resolve`, dependency lookup by `Key`,
  subtype tests against named constants. Positional `.arguments` matching went from ~80 sites to
  around a dozen. This was the highest-risk item in the plan and it is essentially done.
- **`changeRefusalReason` as a separate classifier** is better than what the review proposed. Keeping
  the refusal taxonomy out of the render path lets reasons be specific without every guard carrying a
  label. Reuse this pattern in the other families.
- **`Quantity` and `Modality` were adopted completely**, not partially — `ActualScalar` went from 48
  mentions to 2. Half-adopted abstractions are worse than none; you avoided that.

| | start | r1 | r2 | r3 | **now** |
|---|---|---|---|---|---|
| lines | 4,657 | 4,942 | 5,387 | 5,175 | **5,999** |
| `return null` | 549 | 562 | 639 | 590 | **660** |
| `ComponentDescriber` fields | 39 | 33 | 33 | 27 | **27** |
| `NounPhrase.text` | ~47 | 47 | 49 | 38 | **49** |
| `.arguments` | ~80 | 70 | 9 | 9 | **13** |
| unresolved nodes | — | 67 | 61 | 75 | **66** |
| frame families | 0 | 0 | 0 | 1 | **1** |

**Read that table honestly.** Coverage improved this round (75 → 66 unresolved nodes), which is real
progress. But it cost +824 lines, +70 `return null`, and +11 string escapes — `NounPhrase.text` is
back to its worst level ever. The module is now 29% larger than when this work began, and one of four
families has a frame.

§6 says: if a round adds lines *and* adds `return null` *and* adds frame variants, stop and say so.
Two of those three fired this round. That is the signal to change what you are doing, not to push
harder on it.

---

## 2. What is still wrong

*Same date. If a section here is already fixed, delete it rather than working around it.*

### 2.1 Effects were never refactored, and are now most of the remaining problem

`renderEffect.kt` is **1,240 lines and 50 functions** — up 191 lines this round, and by far the
largest file. Its entry point is still an eight-deep `?:` ladder, and its contents are the
single-card recognizers D8 rules out:

```
renderCardResourcePaymentValue      renderAcceptedResourcePayment
renderAcceptedCardResourcePayment   renderAcceptedPaymentResource
renderBarrierSequencedTrackChoice   renderLinkedPlayedTagResourceChoice
renderLinkedProductionReward        renderRequirementFlexibility
renderPurchaseAdjustment            renderBillingPaymentDiscountTrigger
```

Three new files appeared alongside it — `renderCardOperation.kt` (271), `CardCriterion.kt` (64),
`BillingEvent.kt` (52) — 387 lines of new machinery. `CardCriterion`'s own doc comment reads *"the
printed card fact used by one canonical card operation."* A type whose stated purpose is serving one
operation is the shape this refactor exists to eliminate. `Billing` likewise grew into a
free-standing concept rather than folding into a frame.

Meanwhile `EventKind` is still nine constants each carrying `activeVerb`, `passiveVerb`,
`activeModifier`, `passiveModifier` — with `ADD_TO_CARD` and `ADD_TO_THIS_CARD` as separate *kinds*
when the difference is a destination.

### 2.2 The frame idea stopped after one family

Of 27 remaining `ComponentDescriber` fields, **21 serve triggers, requirements, and metrics** —
exactly the three families with no frame:

```
triggers      playTrigger, playedCard, playedTagPhrase, usedActionTrigger, actionUse,
              spentResourceTrigger, purchase, paymentRole, implicitPaymentResource
conditions    requirement, presenceCondition, requirementKind, requirementShortfall
counts        metricCount, countNoun, distinctKinds, metricLocation, spatialRelation,
              placementSite, placementBonus, productionSelection
```

This is not a new idea needing invention. It is the same move you already made, three more times.

### 2.3 Strings still cross layer boundaries

49 `NounPhrase.text`, plus `Clause.Prefaced(preface: String)` and `renderGateCondition(): String?`,
which push whole assembled conditions ("if you have 2 Jovian tags", "if it has a requirement") into
the structured layer. While those exist, `coordinateClauseObjects` decides factoring by comparing
`verb` and `modifiers` for **string equality** — a spelling test standing in for a semantic judgment.
That is the original defect, unchanged, and this round it got worse.

### 2.4 The injection seam is decorative

`ExpressionResolver(classes: Set<Class>)` takes a parameter it uses only for a `require`, then reads
the global `canonClassUniverse`. `TerraformingMarsDescribers` reads the same global. So
`English(descriptions: Map<Class, ComponentDescriber>)` advertises an injectability that does not
exist. Making `English` and `ComponentDescriber` `internal` narrowed the blast radius but did not
resolve the contradiction.

Pick one and commit. Given D1 — Terraforming Mars only, forever — the honest choice is almost
certainly to **delete the pretense**: no constructor parameter, build from the canon registry, say so.
A decorative parameter is worse than none, because it tells the next reader a lie.

---

## 3. The destination

This is the part to hold in your head. Everything below is one shape repeated.

```
English            facade: public API + card layout
   │
   ├─ interpret    Pets AST → structural facts        (no English anywhere)
   │
   ├─ realize      frames choose words; clauses compose   (the only English)
   │
   ├─ Clause…      syntax + exactly one linearizer
   │
   └─ lexicon      per-class English facts, inherited
        └─ ExpressionResolver   structural questions about Classes and Types
```

**One frame family per Pets family. Each a small closed sealed type. Each dispatched by a single
exhaustive `when`.**

| family | frame | replaces |
|---|---|---|
| changes | `ChangeFrame` ✓ done | `DirectChange` + 12 fields |
| triggers | `TriggerFrame` | `EventKind` + 9 fields + ~10 recognizers |
| conditions | `ConditionFrame` | `Requirement.Bound` + 4 fields |
| counts | `CountFrame` | 8 fields |

When that is finished, `ComponentDescriber` is approximately:

```kotlin
internal data class ComponentDescriber(
    val noun: Noun? = null,
    val change: ChangeFrame? = null,
    val trigger: TriggerFrame? = null,
    val condition: ConditionFrame? = null,
    val count: CountFrame? = null,
)
```

Five fields, from thirty-nine. **That** is the beautiful version, and it is not speculative — it is
the shape you already built once, applied consistently.

The extensibility contract then falls out for free. Adding a component to an expansion becomes: pick a
frame per family it participates in, supply the words. No Kotlin outside the lexicon.

**One honest revision to the earlier plan.** `LANGUAGE_REVIEW.md` proposed a separate semantic
`Description` IR between interpretation and realization. Do the frames and the role-tagged complements
first, then **re-ask whether that layer is still needed**. Frames already carry "what kind of thing is
being said"; complement roles would carry "which part is which." Together those may be all that
aggregation needs — in which case `Clause` *is* the IR and a fifth layer is overengineering. Do not
build it on the review's say-so. Build it only if, afterward, you can name a decision that still
cannot be made without it.

---

## 4. Six rules

The difference between "correct" and "beautiful."

**1. A `?:` ladder is a missing frame.** Every chain of `renderX() ?: renderY() ?: renderZ()` is a
family that has not been given its vocabulary yet. Where you see one, that is the work.

**2. Frames are closed; entries are open.** A new expansion adds lexicon *entries*, never frame
*variants*. If you are adding a variant, stop and write down why the existing ones can't express it —
that note is worth more than the variant.

**3. Every fact is structural or declared. Never recognized.** Structural means the type system
answered it. Declared means the lexicon did, and only about *words*. Recognized means you matched a
shape that happens to occur in today's data — which is how this went wrong the first time. A type
named for one card operation is a recognizer wearing a better hat.

**4. No assembled string crosses a layer boundary.** One linearizer, at the end. The violations are
countable — `NounPhrase.text`, `Clause.Prefaced(String)`, `renderGateCondition(): String?`. Drive that
count to zero and most of the string-equality factoring problems dissolve on their own.

**5. Symmetry is the design.** What you did for changes, do identically for triggers, conditions, and
counts. If one family seems to need a different shape, that is *information* — investigate before
accepting it. Usually a concept is misplaced, not the family special.

**6. Prefer deleting a capability to keeping an ugly one.** Bracketed Pets is an honest answer. A
103-line recognizer that renders one card beautifully is not. When those conflict, bracket it and put
the card on the list.

---

## 5. Order of work

**Next: `TriggerFrame`.** Biggest file, most recognizers, most fields, pattern already proven. Fold
`EventKind` into it — voice becomes a property of the verb group, destination becomes a complement,
and nine constants collapse to a handful. Fold `Billing` in rather than letting it stand alone. Expect
`renderEffect.kt` to lose 400–500 lines. **This is the round where the module must get visibly
smaller**; if it doesn't, stop and report rather than starting the next family.

**Then `ConditionFrame` and `CountFrame` together.** Smaller and largely mechanical after triggers.
Collapse the remaining `Requirement.Bound` wording variation here — direction comes from
`Requirement.Min`/`Max` and party from the resolver, so most of what those types encode is already
known.

**Then the string holes.** Give `Modifier` a semantic role instead of a punctuation-derived variant;
make `Prefaced` take a clause; make `renderGateCondition` return structure. Then re-derive factoring
from roles rather than string equality, and drive `NounPhrase.text` to zero.

**Then the honesty pass.** Resolve §2.4 one way or the other. Move the registry per-bundle. Validate
it once at construction instead of on every `fact()` call.

**Last, the recognizer audit.** By then most will have dissolved. Whatever survives gets argued for
individually or bracketed — including `renderCardOperation` and `CardCriterion`.

**Then re-ask the IR question** (§3, final paragraph).

---

## 6. Report these every round

Seven numbers, every time, including when they move the wrong way:

```
lines · return null · NounPhrase.text · describer fields · frame families · frame variants ·
unresolved nodes
```

Plus the transformation-grouped diff: each distinct before→after wording change with a count and two
examples, and separately **any row that changed in a way the stated rule does not explain**. Those
unexplained rows are the real bug surface. Ten transformations and zero unexplained rows is the gate;
card count is not.

If a round adds lines *and* adds `return null` *and* adds new render files, that is three signals at
once — stop and say so rather than continuing. Coverage bought at that price is a loan, not income.

---

## 7. Open

1. **Does `spatialRelation` deserve its own frame family?** Relations between two placed components
   (`Adjacency`, `Neighbor`) don't obviously belong to changes, triggers, conditions, or counts — they
   appear in all four. A fifth frame may be right; so may treating a relation as a `CountFrame`
   variant. Worth ten minutes before defaulting either way.
2. **Is the payment protocol** (`paymentRole`, `implicitPaymentResource`, `Billing`, `Owed`, `Accept`,
   `Barrier`) **a frame, or a genuinely separate small concept?** It spans several components and
   several families, unlike everything else. It may be the one thing that legitimately stays its own
   named idea — but `BillingEvent` and `renderBillingTrigger` should not both exist.
3. **Is "has a site dependency" a reliable structural test for `Positioned`?** If so, that frame
   becomes derived rather than declared — strictly better.
4. **Should `ChangeFrame.Positioned` carry `unqualifiedMetricOwner` / `anyoneMetricOwner`?** Those are
   metric-rendering concerns riding along in a change frame. Probably they belong to `CountFrame`.
5. **What is `renderCardOperation` actually for?** If it is the transitional filtered-draw table
   generalized, say so and scope it. If it is a general card-fact mechanism, it needs a frame. Right
   now its comment claims one card operation, which is neither.
