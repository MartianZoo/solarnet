# Deriving English from Pets

> **NOTE:** This document is used by agents to capture information for themselves to read later; a
> human didn't write it and we don't expect humans to read it. The project owner can't personally
> vouch for the information here.

> **Read when:** changing English output, renderer structure, lexical facts, card-region layout,
> refusal behavior, or generated text evidence.
>
> **Skip when:** changing Pets or game semantics without changing human rendering.

## Goal

Pets is the specification of what a game component means. English text is one derived view of that
specification, just as execution is another. The renderer should eventually describe every valid
Pets element in the loaded Terraforming Mars vocabulary without consulting a stored answer for the
card or goal that contains it. Count-only card handling currently loses some printed physical-card
procedures. `CardPrintedProcedureText.kt` is a sparse exception that owns the English wording of
those missing procedures; other card regions still derive from Pets. Remove an entry when Pets can
express its complete procedure again.

Published text is normally the proving corpus, not the production data source. The sparse physical
procedure exceptions above preserve facts absent from executable Pets. For modeled facts, published
text is evidence for meaning and good wording; incidental variation is not a rule. Prefer one clear,
consistently derived sentence for equivalent Pets.

Incomplete support must stay honest. When the renderer cannot describe a node safely, retain its
canonical Pets source in square brackets at the narrowest useful location. Losing coverage is better
than hiding a card-specific recognizer behind a general-looking API.

## Where each piece of logic belongs

> **Status: target architecture.** The code does not work this way yet. Sections below name what is
> already true and what is not. Read this before adding a renderer branch.

Rendering is two passes over one English syntax tree, separated by a hard rule about what each pass
may consult.

```text
Pets AST + Class Table
          │
          ▼
  ExpressionResolver ──────────► resolved type, keyed dependencies
          │
          ▼
  PASS 1 — lexicalize            may read Describers, ClassName, ResolvedExpression
  one EST node per Pets node     must not glue a head noun into a string
          │
          ▼
  PASS 2 — rewrite               may read the EST only
  structural paraphrase rules    must not read Describers or mention a ClassName
          │
          ▼
  EnglishText document tree
          │
          ▼
  English facade linearizes: agreement, capitalization, punctuation, regions
          │
          ▼
  English facade: standalone descriptions, card regions, goal text
```

**The decision procedure.** For any new logic, ask one question: *does this depend on which
component it is?*

- **Yes** → pass 1, as an inherited lexical fact. It is knowledge about a component.
- **No** → pass 2, as a structural rule. It automatically applies to card text, requirements,
  metrics, events, and goals alike.

That question is the thing this document exists to make answerable. When it has no clear answer, the
concept is not yet named — stop and name it rather than adding a branch to whichever renderer is
currently open.

Run a request through it before writing code. "Milestones should say *4 played event cards*" — does
"played" depend on which milestone? No. So it is a pass-2 rule, and being a pass-2 rule it applies to
card text too, where it would visibly damage Media Archives. The discipline surfaces that conflict at
design time instead of in a snapshot diff.

### Pass 1: lexicalize

One node per Pets node, built bottom-up. Exhaustive, not a nullable ladder. This is the only place
`Describers` may be touched.

The nouns it produces must be **structured, not concatenated**. `Production<Energy>` is head plus
argument in Pets, so it must be head plus attributive modifier in English — never the string
`"energy production"`. The same holds for `"$name tag"` and `"$item resource"`.

`NounPhrase` now has a structured pre-head attributive-modifier slot, and production nouns use it.
Other attributive compounds remain glued into noun strings. Structured coordination in that slot
exists specifically for the shared-head rule below; do not generalize it into another modifier API.

`lexicalizeCountedExpression` is the single total protocol from a counted `Expression` to its noun
phrase and ranking. It owns the ordered family of private recognizers and returns an opaque
phrase-level `RawPets` refusal when none accepts the expression. Those private recognizers remain
nullable deliberately: refusal must be chosen only after every applicable protocol has had a turn,
not used as a successful value inside another recognizer ladder.

`Describers` still exposes several narrower producers (`componentNounPhrase`,
`quantifiedComponentNounPhrase`, `cardResourceNounPhrase`, `playedTagPhrase`, `describedNoun`,
`plainGainNoun`, `plainGainCategoryNoun`) that each fuse a classification guard to a noun
derivation. These serve contexts that already established a narrower semantic role; do not use them
to recreate a competing general expression dispatcher.

`ChangeFrame` is the one axis that got this right: a closed classification, dispatched exhaustively,
declared as data. Treat it as the template, not as a finished job.

### Pass 2: rewrite

Bottom-up structural rules over the EST. A rule matches tree shape and rewrites it.

The named functions in `rewriteEnglishSyntax.kt` are the current pass-2 seam. They factor shared
heads and adjacent predicates, distribute a shared step count, and attach one purpose to coordinated
costs. Their signatures accept only EST types. This is deliberately a handful of ordinary functions,
not a general rewrite engine.

`EnglishText` is the document-level continuation of the EST: sentences, sequences, and labeled card
regions remain structured while renderers compose them. Only the `English` facade asks that tree for
a `String`. Refusal is an EST leaf (`Clause.RawPets` or `NounPhrase.rawPets`), so visible fallback and
typed refusal evidence have one recursive source of truth. Family boundaries totalize nullable
recognizers exactly once; internal recognizers do not manufacture refusal values.

Two hard constraints, both checkable by review:

1. **A pass-2 rule may not mention a `ClassName` or read `Describers`.** If a rule needs to know it
   is Media Archives, it is not a rule.
2. **A rule must be a meaning-preserving paraphrase.** This is what buys order-independence for
   correctness; ordering then only selects which paraphrase you get, which is a wording question.

The two rules the current output most obviously wants:

- **Factor a shared head across coordination.** `Coord(N(head=P, attr=A), N(head=P, attr=B))` becomes
  `N(head=P, attr=Coord(A, B))`, turning "energy production and heat production combined" into
  "energy and heat production combined". This rule is implemented for structurally matching noun
  phrases; it does not know that the current proving case is production.
- **Drop a contextually recoverable possessor.** Build "you have" unconditionally in pass 1 and let a
  rule delete it where context supplies it. This replaces the `possessorEstablished` flag threaded
  through ten functions in `renderMetric.kt`, and settles the "Requires that you have 3 city tiles"
  versus "Requires 3 city tiles" split that branch ordering currently decides.

A rewrite engine is the easiest possible place to hide special cases — easier than the ladders it
replaces. Constraint 1 is not a preference.

### This is not a semantic Description layer

An earlier review proposed inserting a general semantic representation between interpretation and
clauses, and that proposal was correctly rejected. This is a different claim. Nothing here adds a
representation of *meaning*; it consolidates an `Expression → NounPhrase` operation that already
exists in eight partial copies, and gives the paraphrase rules a place to live. If a proposed layer
carries meaning that Pets already carries, it is the rejected idea again.

## The lexicon

`ComponentDescriber` holds sparse, inheritable component-specific facts: nouns, verbs, value roles,
and constructions the type system cannot answer. It must not restate structural membership such as
"is a tag," "is production," or "is a card resource" — derive those.

Facts inherit independently. A more specific Class overrides one fact from an ancestor; unrelated
facts from incomparable ancestors compose; conflicting nearest providers for one fact are rejected at
construction. This multiple inheritance is part of the design. When a fact should not reach
subclasses, say so in the data with a `forSubclasses` flag resolved through `fact()` — do not add a
second, non-inheriting accessor for one field.

`TerraformingMarsDescribers` composes authored facts keyed by `ClassName`. Bundle-specific facts
live in that bundle's `english/` directory, in `english.kt`, and are compiled by `tfm-text`. `Describers`
owns Class resolution; absent Classes fall back to structural interpretation and default naming.
For an opaque `Custom` requirement, `requirementKind` supplies only its English name: direct uses say
the player must meet that named requirement, rather than duplicating its hidden gameplay definition.

### Choosing the shape of a new fact

| Shape | Use when | Composition rule | Example |
| --- | --- | --- | --- |
| Frame | Alternatives are mutually exclusive constructions | One channel, one exhaustive dispatch | `ChangeFrame` |
| Orthogonal fact | Meanings can coexist on one Class | Separate inherited channels | `ActionCard` has played-card *and* action-use wording |
| Protocol | Meaning spans several elements or a sequence | One named cross-element interpretation | Payment, across billing, actions, and effects |

Before adding or combining lexical data:

1. Can one Class legitimately have both facts? Then they are not alternatives in one frame.
2. Are the alternatives exclusive and stable across expansions? Then a closed frame may fit.
3. Does the meaning belong to a relationship or lifecycle rather than one Class? Model a protocol.
4. Can the AST or type system answer it? Derive it instead of declaring it.
5. Does it serve one call site or one current card? Treat it as a recognizer until a recurring
   semantic role is demonstrated.

`ActionCard` is the decisive composition case: combining its two trigger facts into one frame creates
conflicting incomparable providers. Permitting both is more truthful than a lower field count.

## Working rules

- Make one systemic transformation at a time. Name the family or existing machinery it replaces
  before implementing it.
- Frames are closed; lexicon entries are open. A new expansion should add entries, not frame variants
  or renderer branches.
- Never recognize a whole card. Narrow Procedure or Wrapper wording is acceptable only when it
  delegates represented Pets back to a general renderer.
- Bracket unsupported Pets rather than buying coverage with a one-card mechanism. Losing a row to an
  honest refusal is a good trade for deleting a hardcoded answer.
- Never bend a lexical fact to hit a target string. If a component's noun changes, check every other
  place that noun appears before accepting it.
- Change the active path and delete superseded machinery. Do not maintain parallel converters.
- Two general paths that render equivalent Pets differently are a defect, even when both are
  general. Ordering is not a design.
- The realization layer is game-neutral. The interpretation layer may name common game concepts
  (cards, actions, resources, production, placement, payment, scoring). Concrete component identities
  and expansion-specific recognizers stay in vocabulary data.
- Do not build a general natural-language framework or support hypothetical games. This renderer is
  for Terraforming Mars.
- Treat symmetry as a question, not a mandate. Different composition rules are evidence for different
  shapes.
- Some cases stay unresolved on purpose: Cyberia Systems' first-choice marker and Sponsored
  Academies' grouped player fanout are each too isolated to earn permanent machinery. Reopen one
  only with a smaller general interpretation, not a protocol built for it.

## Wording decisions

- Use `raise`/`lower` for global parameters and terraform rating; `increase`/`decrease` for
  production. Spell out `terraform rating`.
- Begin every requirement with `Requires`. Use `Requires that you have` for player-owned state such
  as resources, production, rating, tiles, and colonies. Use a terse noun phrase for tags and global
  counts, and `Requires that` for other clauses. Describe temperature bounds as `warmer` or `colder`,
  not `higher` or `lower`.
- Use `spend` when a Resource is consumed as an action cost, including a resource held on a card.
  Use `remove` for taking a resource from any player's card and for standalone or involuntary
  reductions. Use `pay` for the non-action payment constructions. Describe substitution as `may be
  used as`, with no payment verb.
- Join a rendered action cost to its result with `to`. Refuse a costed action whose result cannot be
  an infinitive; do not split it into separately modalized sentences. A mandatory standard-resource
  removal followed by `THEN` is a payment for its result and joins with `to`.
- Factor a shared subject and verb across adjacent instructions with distinct objects and no clause
  modifiers. Do not make independently chosen destinations look shared or collapse repeated
  operations. Keep consecutive production changes in one sentence even when their verbs differ.
- When adjacent production changes in the same direction use the same magnitude, state the
  magnitude once and end with `each`.
- Describe spatial adjacency as `next to`, never `adjacent to`.
- Render discount effects declaratively as `you pay N M€ less`, adding `for it` when the trigger
  supplies a clear discounted object.
- Introduce a triggering event with `when`, never `each time`. Describe one event; express the
  result's multiplicity when one event produces several changes.
- Render every ratio with `per`, whatever the denominator and whether or not the result is victory
  points.
- Preserve shared implicit player identity across a trigger and its result as `that player`.
- Make optional maxima explicit as `you may ... up to`, including above one.
- Describe a card-resource location as `this card`, never `here`.
- For an unbound card-resource destination, say `another card` when the current card cannot hold
  that resource type; otherwise say `any card`.
- Use the placement frame's determiner, normally an indefinite article, for exactly one placed
  object. Use `1` for other explicit singular quantities; ordinary type references still use
  articles.
- Describe unrestricted persistent counts and requirements as being `in play`; when the counted
  objects are restricted to Mars, say `in play on Mars`. Placement triggers say only `on Mars`, and
  player-local counts do not say `in play`.
- Omit unconditional fixed victory-point adjustments from card regions; keep conditional and
  metric-based victory-point behavior.
- Render `PlanetaryTag` as `planetary tag`.

Card rendering calls out `including this` when the entering card contributes to an immediate metric
or satisfies a supported tag or card-play trigger. It does not infer the phrase through setup
operations or effects that the renderer cannot otherwise interpret.

## Evidence and verification

Use evidence in this order:

1. Pets and the canonical Class model, for meaning.
2. Original published text, for wording evidence.
3. `english-card-text-goals.tsv` / `english-goal-text-goals.tsv`, as fallible reviewed targets.
4. `english-card-text-current.tsv` / `english-goal-text-current.tsv`, as generated characterization —
   never a production answer source.

The goals files may cover a selected proving corpus rather than every loaded card or goal. The
current snapshots cover every loaded card and concrete goal. Current refusals are recorded
mechanically in the generated `*-refusals.tsv`; do not restate them here.

After an intentional output change, run:

```text
./gradlew :tfm-text:writeEnglishCardTextCurrent
./gradlew :tfm-text:writeEnglishGoalTextCurrent
./gradlew :tfm-text:test
```

Review the production diff and the regenerated snapshots together. Group every distinct
before-to-after wording transformation, count affected rows, give representative examples, and list
unexplained changes individually. Pause before regenerating a change that would materially affect
more than roughly 25 rows.

Track as diagnostic trends, not optimization targets:

- unresolved nodes grouped by typed refusal reason;
- sites that interpolate or concatenate a semantic role into a noun;
- positional `Expression.arguments` inspection outside role resolution;
- nullable branch exits and new renderer files;
- frame variants and lexical fields that fail the modeling test above.

Raw field count and raw `NounPhrase.text` count are not design metrics. Divergence from a goals file
is not one either: deleting a hardcoded answer correctly increases it. If a round adds production
lines, nullable exits, and renderer files together, stop and explain the cost before continuing.

Do not add a test merely to prove one card lost brackets — the snapshots are the coverage
characterization. Add a focused test when a new rule is not exercised by the corpus, or when a
semantic invariant needs direct proof.

## Source map

- [`English.kt`](../../src/common/dev/martianzoo/tfm/text/English.kt) — internal facade and card-region
  assembly; `EnglishCardTextRenderer.kt` is the public card-text entry point.
- `Clause.kt`, `Predicate.kt`, `Verb.kt`, `NounPhrase.kt`, `Determiner.kt`, `Modifier.kt`,
  `Coordination.kt`, `Sentence.kt` — sentence-level English syntax.
- [`EnglishText.kt`](../../src/common/dev/martianzoo/tfm/text/EnglishText.kt) — document composition and
  the single facade-facing linearization root.
- [`rewriteEnglishSyntax.kt`](../../src/common/dev/martianzoo/tfm/text/rewriteEnglishSyntax.kt) —
  named EST-only paraphrases and structural composition helpers.
- [`ComponentDescriber.kt`](../../src/common/dev/martianzoo/tfm/text/ComponentDescriber.kt),
  [`Describers.kt`](../../src/common/dev/martianzoo/tfm/text/Describers.kt),
  [`TerraformingMarsDescribers.kt`](../../src/common/dev/martianzoo/tfm/text/TerraformingMarsDescribers.kt),
  and expansion-local `english/` directories such as
  [`TurmoilExpansion/english/english.kt`](../../src/common/dev/martianzoo/tfm/canon/TurmoilExpansion/english/english.kt)
  — lexical facts, composition, and inheritance. Each bundle directory is an explicit `tfm-text`
  source root; `tfm-canon` excludes bundle-local `english/` directories to keep module ownership
  acyclic.
- [`ExpressionResolver.kt`](../../src/common/dev/martianzoo/tfm/text/ExpressionResolver.kt) — structural
  Class and dependency roles.
- `Clause.RawPets`, `NounPhrase.rawPets`, and `EnglishText.unresolved()` — visible fallback and
  recursively derived refusal evidence.
- `renderActions.kt`, `renderChange.kt`, `renderEffect.kt`, `renderInstructionTree.kt`,
  `renderMetric.kt`, `renderRequirement.kt`, `renderGoal.kt` — family interpreters.
- [`EnglishCardTextCurrentGenerator.kt`](../../test/jvm/dev/martianzoo/tfm/text/EnglishCardTextCurrentGenerator.kt)
  — generated snapshot and refusal report.
