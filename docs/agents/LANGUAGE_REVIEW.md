# English renderer: architecture and refactoring plan

> **Status: agreed direction, not committed behavior.** Section 2 records decisions Kevin has made;
> treat those as constraints, not suggestions. Section 3 states principles derived from them.
> Sections 5–6 are the plan. Nothing here describes what the code does today except Section 4.
>
> **Audience:** the agent doing the work. Kevin skims Sections 1–3 and 7.
>
> Scope: `language/src/main/kotlin/dev/martianzoo/tfm/language`, and its relationships to `pets`,
> `canon`, and `docs/agents/LANGUAGE.md`.

---

## 1. Verdict, and what changed since the first draft

The strategy is right: derive text rather than store it; one renderer per Pets family; a per-class
fact registry with inheritance; bracket unsupported Pets at the narrowest safe boundary; a generated
snapshot as the characterization. Keep all of that.

The implementation does not carry it out. Today it is a large **idiom recognizer** — ~50 hand-written
matchers for shapes that happen to occur in today's card data — behind a **38-field record** that
restates game semantics and stores English sentence templates. Both are exactly what Kevin's
constraint forbids: they are *accidental invariants of the current data set* promoted to
architecture.

Two recommendations from the first draft are **withdrawn**:

- **Withdrawn: add role-marker classes (`Track`, `PlacedComponent`) to Pets declarations.** Kevin's
  answer to Q4 is decisive — track-vs-countable and placed-vs-added are *not game-mechanical
  distinctions*. Pets could legitimately say "gain" for every component. Putting these categories into
  Pets would leak presentation into the specification. They belong in the lexicon, on the
  language-specific side (§3.3).
- **Withdrawn: "Ontology vs Lexicon" as two registries.** There is one lexicon. What varies is where
  each fact is *grounded* (§3.1).

One recommendation is **promoted**: resolving expressions through the type system and reading
dependencies by key (Q6) moves from "worth investigating" to the second stage of work. It is the
single strongest defense against accidental invariants available, and it deletes the most fragile
code in the module.

---

## 2. Settled decisions

Recorded so later rounds don't relitigate them.

| # | Decision |
|---|---|
| D1 | **Terraforming Mars only.** No other game need ever work. But expansions will remix these elements in new and surprising ways, so no *accidental* invariant of today's data may be relied on. |
| D2 | **Target shape is two layers**: Pets → intermediate representation → language-specific renderers. Only English is being built now. The IR need not be solved up front; grow it. |
| D3 | **Real clients exist**: a game UI showing card text, plus the REPL, plus anywhere a Pets element needs to be readable. Published cards are the golden use cases, but arbitrary Pets is the target. |
| D4 | **Track / placed / countable are not Pets concepts.** They are presentation categories. Do not add them to Pets declarations. |
| D5 | **A colony is not a tile.** `ColonyTile` is the site a `Colony` is placed on. Pets class names reflect tileness accurately. |
| D6 | **Use the type system.** Resolve expressions to `Type`s and read dependencies by key. Positional argument matching is the wrong instrument. |
| D7 | **Consistency beats fidelity to the golden data.** The renderer must use the same word for the same thing every time. Variation in the published cards is *not* a requirement unless Kevin states it as one. `M€` or `megacredit` matters far less than picking one. |
| D8 | **No card-specific recognition.** Anything that would hardcode a whole card's text belongs outside this library, not inside it. The recognizers present today are early-development scaffolding, never intended to persist. |
| D9 | **A high generality bar is fine** provided the architecture lets the derivable boundary keep moving outward over time. Rate of progress matters more than today's coverage. |
| D10 | **`Mandate` and `NextCardEffect` are shells the renderer should look through.** "Mandate" contributes "As your first action," and then the *ordinary* renderer handles the contents. |
| D11 | **The refusal report is wanted** regardless of what else lands. |
| D12 | Renaming (`English`, `Describers`, `ComponentDescriber`) is cheap and can happen any time. Not a priority. |
| D13 | Whether the lexicon lives in Kotlin or in data files is not important. |
| D14 | **Card ownership context is the renderer's law.** An omitted owner on a player-owned type means the card owner; explicit `<Anyone>` is required to opt out and address every player. The renderer need not support interpreting these source expressions outside ownership context. |

---

## 3. Three principles

Everything in the plan follows from these. If a proposed change can't be justified by one of them,
don't make it.

### 3.1 Every fact must be *grounded*, and there are only two acceptable grounds

For any fact the renderer uses about a component:

| ground | what it means | verdict |
|---|---|---|
| **Structural** | Read from the Pets AST or type system: a dependency resolved by key, a subtype test against a class that exists for *engine* reasons, the shape of a refinement, a scalar, an intensity. | **Preferred.** True by construction. |
| **Declared** | Registered in the lexicon as a fact about how to *say* a class, inherited through the class hierarchy. | **Required** for anything a language knows and the engine doesn't. |
| **Recognized** | Matching an AST shape that happens to occur in today's data. | **Forbidden**, except as explicitly-labeled scaffolding with a removal plan. |

The discipline that keeps this honest:

> **A lexicon entry may never assert something the type system could have answered.**
> If `Floater` is a `CardResource`, the renderer asks the class table, not the lexicon. If the lexicon
> also said so, the two could disagree — and the lexicon would silently win.

`StandardResource`, `CardResource`, `Tag`, `Production`, `GlobalParameter`, `Tile`, `Area`, `Owned`,
`Signal`, `Barrier`, `Class` all exist as Pets classes for engine reasons. Membership in those is
structural and free. Nothing needs to be *added* to Pets to get it — which is why D4 costs nothing.

### 3.2 The IR carries structure; the lexicon carries language

Concretely, for a change to one component:

- **IR carries**: which class, resolved dependencies (owner, holder, site) *by key*, refinement,
  count, direction, modality. All structural.
- **Lexicon carries**: which verb frame the class takes in this language, and the words that frame
  needs. All declared.

This placement is deliberate and answers Kevin's open uncertainty about whether other languages carve
these categories the same way. Because the frame is chosen by the **per-language lexicon**, English
may treat `OxygenStep` as a scale and another language need not. Had the categories been pushed into
Pets, every language would have inherited English's carving. This is the main reason D4 is right
beyond the spec-purity argument.

### 3.3 Verb frames replace the 38-field record

The frame system already exists — it is written as prose at the bottom of `docs/agents/LANGUAGE.md`:

> *"Temperature, oxygen, Venus, terraform rating, and colony productions are tracks, not countable
> units. Render their gains and removals with the applicable increase/decrease or raise/lower pair
> rather than resource language. Gaining a standard resource uses `gain`; gaining a card resource
> uses `add`. The general removal verb is `remove`. An action cost paid from standard resources uses
> `spend`."*

That is a frame table. It is documentation instead of a data structure, so the code re-derives it ad
hoc from 38 booleans in every renderer. Make it the data structure:

| frame | acquire / relinquish | needs | example |
|---|---|---|---|
| **Countable** | gain / remove | noun, number forms | *gain 3 plants* |
| **Held** | add / remove | noun, **place** | *add 2 animals to this card* |
| **Scale** | raise / lower | subject, step noun | *raise oxygen 1 step* |
| **Production** | increase / decrease | owner, resource | *increase your heat production 2 steps* |
| **Positioned** | place / — | noun, article, **site** | *place a city tile on a land area* |
| **Deck** | draw / discard | noun | *draw 2 cards* |
| **Procedure** | *(imperative)* | verb + object phrase | *fund an award for free* |
| **Wrapper** | *(transparent)* | preface | *as your first action, …* |

Seven lexical frames plus one structural one. That is the replacement for 38 fields and for
`DirectChange`'s ten variants. The frame supplies **both** direction words, so verb selection stops
being scattered across `renderChange`, `renderEffect`, `renderActions`, and `renderPlacement`.

**Growth is the thing to watch.** Adding an expansion must add lexicon *entries*, never frames. A new
frame is a real language-model change and should be rare, argued for, and visible in review.

**Procedure is the honest escape hatch** (D8): a component whose behavior Pets does not express gets
one imperative phrase — not a recognizer. `Trade`, `Award`, `CopyPrelude`, `GiveColonyBonuses`
already work this way and are fine.

**Wrapper generalizes D10.** A component whose whole job is to wrap contributes a preface, then the
renderer recurses into the wrapped content with the ordinary machinery. This replaces
`directChangeForSubclasses`, `renderFirstAction`, and `renderNextPlayedCardAdjustment` with one rule.

---

## 4. What's wrong now — the evidence

Compressed; the numbers are from static reading and `grep`, accurate to about ±10%. Nothing was built
or run.

**Guards.** 549 `return null` statements across 11 files (`renderEffect.kt` 180, `renderChange.kt`
135, `renderInstructionTree.kt` 65, `renderActions.kt` 37). They are overwhelmingly the same four
tests: `refinement != null` / `.complement` (92 sites), positional `arguments ==` matching (82),
`(count as? ActualScalar)?.value` (48), `intensity != MANDATORY` (~30). Every one is an ungrounded
recognition (§3.1) or a job for the type system (D6).

**Anonymous, uneven failure.** A guard failing collapses a subtree to `[raw Pets]` with no record of
which guard or why. Granularity is accidental: `renderLoweredInstructions` brackets per instruction,
but `renderSequentialThen` and `renderAlternatives` collapse the entire `THEN`/`OR` if any child
fails.

**The record.** 38 fields on `ComponentDescriber`; 12 used at exactly one call site, 25 at two or
fewer, only 5 at five or more. Half of them (`standardResource`, `cardResource`, `production`, `tag`,
…) restate Pets classes that already exist. `ThresholdSyntax` and `CountSyntax` store ten *English
sentence templates* per component class — the four `CountSyntax` constants are just
`(min|max) × (owned|global)`, information the interpreter already has. Under D7 these collapse
outright; there is nothing to preserve.

**No middle layer.** `Clause`/`Predicate`/`NounPhrase` exist but are routinely bypassed —
`NounPhrase.text("up to $count $noun")` puts assembled English into the exact slot that factoring
needs to inspect, so `coordinateClauseObjects` can only factor clauses whose wording matches as
strings. A half-built IR already exists in the wrong place: `renderEffect.kt`'s private `EventKind`
enum, which is a semantic trigger IR with the verbs baked in and destination mistaken for kind.

**Registry placement.** `canon` is organized as twelve per-expansion bundles, each already shipping a
`language/en.json5` consumed by `Vocabulary`. Meanwhile all descriptions for all expansions live in
one 526-line `internal object` in a different module — so shipping an expansion means editing a core
file. Two sources currently disagree about the same nouns (`en.json5` says `"Megacredit":
"megacredit"`; the describer says `M€`), which is precisely the inconsistency D7 targets.

**The contract, already measured.** Colonies exists. Adding it cost six lexicon-shaped entries — fine
— *plus* two new `DirectChange` variants, three new renderer functions, and a 52-line effect-pair
recognizer. The instructive detail: `TradeBarrier : Barrier` cost **zero**, because it inherits its
role through the class hierarchy. `ColonyProduction` has no supertype at all, so "a track you move
steps along" — a frame the renderer already implements four times — needed bespoke code. Under §3.3
it needs one lexicon entry naming the **Scale** frame.

---

## 5. Stages

Each is independently shippable. `LANGUAGE_REFACTOR.md`'s rule holds throughout: **change the active
path in place; never build a parallel converter.**

### Stage 1 · Refusal report *(no output change)*

Make every interpreter total. `f(node): T?` becomes `f(node): T` where refusal produces
`Unresolved(node, reason)`; each `return null` names the guard that declined
(`NON_LITERAL_SCALAR`, `REFINED_EXPRESSION`, `UNKNOWN_FRAME`, `UNRESOLVED_DEPENDENCY`, …). Extend the
snapshot generator to emit a ranked histogram of refusals across all cards.

*Done when:* the snapshot is byte-identical and `writeEnglishCardTextCurrent` also emits a ranked
refusal report.
*Why first:* it changes nothing, is trivially reviewable, and every later stage is prioritized by its
output. It converts 549 anonymous nulls into a work queue.

### Stage 2 · Types, not positions

Resolve every `Expression` to a `Type` through the class table. Read owner, holder, and site as
**dependencies by key**. Delete positional `arguments` matching, `anyoneExpression`/`ownerExpression`
/`thisExpression` comparison, and the ad-hoc scalar and intensity tests — replace the last two with
single `Quantity` and `Modality` resolutions.

*Done when:* no renderer mentions `expression.arguments`, `ActualScalar`, or `Intensity` outside the
resolution helpers.
*Snapshot:* expect it to **improve** — cases refused today for arriving in an unexpected argument
order or nesting will now resolve. Every diff must be explainable; unexplained ones are bugs.
*Note:* this is the largest single deletion in the plan and the strongest move against accidental
invariants (D1, D6).

**Ownership context, verified during implementation.** Renderer inputs use source syntax but are
interpreted as attached to a card. Apply declared ownership defaults before rendering: an omitted
owner on a player-owned type means the card owner, while explicit `<Anyone>` is the opt-out. The
resolver may retain that explicit source dependency by `Key` when type normalization erases the
distinction. This does not authorize positional matching or inspecting source class/resource/site
arguments when the resolved dependency retains their meaning.

### Stage 3 · Verb frames

Replace the 38 fields and the ten `DirectChange` variants with the frame table (§3.3). Derive
structural membership by subtype test against existing engine classes; declare only what a language
knows. Implement **Wrapper** as a general rule and delete `directChangeForSubclasses`,
`renderFirstAction`, `renderNextPlayedCardAdjustment`. Delete `ThresholdSyntax`/`CountSyntax`
(D7). Rebuild the registry per bundle while it is being rebuilt anyway, and reconcile the
`Megacredit` conflict deliberately. Unregistered class ⇒ default noun via `Vocabulary`, never an
exception. Validate the whole registry once at construction, not per lookup.

*Done when:* `ComponentDescriber` holds only frame + words; `TerraformingMarsDescribers` is gone and
`ColoniesExpansion/` owns its own English; adding a class under an existing frame needs no Kotlin
change.
*Snapshot:* a real, reviewed wording diff. This is the stage where consistency is imposed.

### Stage 4 · IR for changes, then aggregation

Introduce `Description` for `Gain`/`Remove`/`Transmute` only. The realizer owns verb, article, and
number; `NounPhrase.text` is banned in this family. Then move coalescing and `Or`-factoring onto the
IR: two changes coalesce iff direction, party, place, and modality agree — no string comparison.

*Snapshot:* should improve. Cases that currently refuse to factor will begin factoring.

### Stage 5 · Requirements and metrics onto the IR

### Stage 6 · Triggers and effects onto the IR

Fold `EventKind`/`EventActor` into the IR's trigger representation. `renderEffect.kt` (999 lines)
should roughly halve.

### Stage 7 · Delete the recognizers

Apply D8 to everything that survives. Each card-specific matcher is either replaced by a **Procedure**
lexicon entry, fixed in the Pets so it derives, or left bracketed. Report what coverage was lost and
why.

### Stage 8 · Card layout and context

Give region assignment its own name (`CardLayout`) and get it out of the renderer core — under D3 the
primary job is rendering arbitrary Pets, and card layout is one client of that. Collapse the
`CardDefinition` overloads and the `drawFilter` parameter threaded through ~15 signatures into one
explicit context, so it has a home now and deletes cleanly if Pets ever carries the filter itself.

### Where to stop

If Stages 1–3 land and the IR work stalls, the result is still much better than today: guards
consolidated, expressions resolved through the type system, one small frame table, a per-bundle
registry, and a refusal report. Stop and report if the IR passes roughly 10 kinds, or if any family
needs a kind that serves one card — that is the 38-field record returning under a new name.

---

## 6. How to batch human review

Answering Q14 directly: the 25-card cadence is the wrong unit, because it measures the *diff* rather
than the *decision*. A systemic rule that changes 200 cards identically is far easier to review than
25 unrelated changes.

**Make the transformation the unit of review.** After each stage, diff old against new snapshot and
group rows by distinct before→after transformation. Report:

1. **The rule, in one sentence** — what changed and why.
2. **Per transformation**: a count, the before/after pattern, and two examples.
3. **Unexplained rows**: anything that moved in a way the stated rule does not account for, listed
   individually.

Kevin then reviews five to ten transformations regardless of whether twenty or three hundred cards
moved, and answers each with *keep* / *revert* / *that distinction was real*. That is exactly the
feedback he described wanting — which distinctions were worth preserving and which should have been
ironed out — and it makes the feedback cheap to give.

**Proposed gate, replacing the card count:** ≤10 distinct transformations per review round, and
**zero** unexplained rows. Unexplained rows are the actual bug surface; card count is noise.

---

## 7. Still open

Short list; everything else is settled in §2.

1. **Is "placed" structural or lexical?** `Colony<ColonyTile>` and `Tile<Area>` both have a site
   dependency, so "has a site dependency ⇒ Positioned frame" may be derivable rather than declared —
   which would be strictly better under §3.1. Does that generalize, or are there components with site
   dependencies that shouldn't read as placed?
2. **Does Production collapse into Scale?** *increase your heat production 2 steps* and *raise oxygen
   1 step* may be one frame with an owner slot. Worth trying to merge; if it needs a special case,
   keep it separate and say why.
3. **What happens to `english-card-text-goals.tsv`?** Under D8 it is scaffolding. Retire it, or keep
   it purely as a review aid with no authority?
4. **Does the UI want text or structure?** D3 says a UI will show generated card text. If it will ever
   want icons or styled fragments, the realizer should emit structure and let the caller flatten. One
   sentence from Kevin settles whether to design for that now or ignore it.
5. **The filtered-draw table** (14 rows, keyed by card class, for a filter Pets cannot express) is the
   one honestly-recognized fact left. Does Pets eventually learn it, or does it stay?

---

## 8. Caveats

- Nothing was built, run, or tested. Counts come from static reading; the Colonies history is
  inferred from current code and declarations, not from a changelog.
- Stages 3 and 4 reduce derived coverage on specific cards before general rules catch up. That is
  accepted under D8/D9 but should be reported, not hidden.
- `docs/agents/LANGUAGE.md` needs to shrink substantially. About 120 of its 403 lines enumerate the
  current derivation boundary in prose; under this plan that becomes the refusal report's output.
  Maintaining both would be worse than either.
