# Deriving English from Pets

> **Read when:** changing English output, renderer structure, lexical facts, card-region layout,
> refusal behavior, or generated card-text evidence.
>
> **Skip when:** changing Pets or game semantics without changing human rendering.
>
> **Status:** durable goal, current architectural understanding, and ordered working direction.

## Goal

Pets is the specification of what a game component means. English card text should be one derived
view of that specification, just as execution is another. The renderer should eventually describe
every valid Pets element in the loaded Terraforming Mars vocabulary without consulting a stored
answer for the card that contains it.

Published cards are the proving corpus, not the production data source. Their text is evidence for
meaning and good wording; incidental variation is not a rule. Prefer one clear, consistently
derived sentence for equivalent Pets.

Incomplete support must remain honest. When the renderer cannot describe a node safely, retain its
canonical Pets source in square brackets at the narrowest useful location. Losing coverage is
better than hiding a card-specific recognizer behind a general-looking API.

The intended flow is:

```text
Pets AST + canonical Class Table
              │
              ▼
interpret structure and semantic roles ◄── inherited English lexicon
              │
              ▼
Clause / Predicate / NounPhrase / Modifier / Coordination
              │
              ▼
final linearization stage: wording, agreement, capitalization, punctuation
              │
              ▼
English facade: standalone descriptions and card-region layout
```

English stays out of the Pets AST. Pets supplies structure; the Class Table supplies type meaning;
the lexicon supplies words; the syntax model retains decisions until final realization.

## What belongs where

### Structural meaning

Derive structural facts from the AST and canonical Class Table:

- resolve expressions and subtype relationships rather than recognizing Class names;
- identify dependency roles by `Dependency.Key`, not argument position;
- resolve quantities and intensities once into `Quantity` and `Modality`;
- preserve linked identity when later wording depends on two occurrences denoting the same choice;
- keep instruction, requirement, metric, trigger, and action structure visible to their family
  interpreters.

`ExpressionResolver` intentionally uses the canonical Terraforming Mars Class Table. The map passed
to `English` is a real lexical-data seam, not an alternate structural universe.

### Lexical meaning

`ComponentDescriber` contains sparse, inheritable English facts: nouns, verbs, value formats, and
constructions that the type system cannot answer. It must not restate structural membership such as
“is a tag,” “is production,” or “is a card resource.”

Facts inherit independently. A more specific Class overrides one fact from an ancestor; unrelated
facts from incomparable ancestors compose; conflicting nearest providers for the same fact are
rejected at construction. This multiple-inheritance behavior is part of the design, not an
implementation inconvenience.

### English structure

Clauses, predicates, noun phrases, modifiers, and coordinations carry realizable structure. A
literal lexical leaf such as `NounPhrase.text("M€")` is fine. A string such as
`NounPhrase.text("up to $count $noun")` is not: it has hidden quantity and modality decisions from
later composition.

Track assembled strings, not raw string-leaf calls. Current audit searches include interpolated or
concatenated `NounPhrase.text`, `Clause.Prefaced(String)`, and
`renderGateCondition(): String?`. The goal is for semantic decisions to remain inspectable until
final linearization, not for the renderer to contain no text literals.

## Choose the right modeling shape

The renderer has three recurring shapes. Confusing them caused both the original scattered record
and the later, incorrect proposal to collapse `ComponentDescriber` to five fields.

| Shape | Use when | Composition rule | Current example |
| --- | --- | --- | --- |
| Frame | Alternatives are mutually exclusive constructions | One channel and one exhaustive dispatch | `ChangeFrame`; the exclusive event categories in `TriggerFrame` |
| Orthogonal fact | Meanings can coexist on one Class | Separate inherited channels | A card can have played-card wording and independently inherit action-use wording |
| Protocol | Meaning spans several elements or a sequence | One named cross-element interpretation | Payment across Billing, Owed, Accept, Barrier, actions, and effects |

Use this test before adding or combining lexical data:

1. Can one Class legitimately have both facts? If yes, they are not alternatives in one frame.
2. Are the alternatives exclusive and stable across expansions? If yes, a closed frame may fit.
3. Does the meaning belong to a relationship, sequence, or lifecycle rather than one Class? Model a
   protocol.
4. Can the AST or type system answer it? Derive it instead of declaring it.
5. Does the proposed fact serve one call site, one card operation, or one current card? Treat it as
   a recognizer until a recurring semantic role is demonstrated.

`ActionCard` is the decisive composition example: it inherits played-card language through
`CardFront` and action-use language through `HasActions`. Combining those into one trigger fact
creates conflicting incomparable providers. A design that permits both is more truthful than one
with a lower field count.

Payment is the decisive protocol example. It spans changes, actions, triggers, and adjacent effect
sequencing. Folding it into a trigger frame would put its ownership in the wrong place.

## Current foundation

The useful architecture already present should be extended rather than replaced:

- `ExpressionResolver` and `ResolvedExpression` centralize Class resolution and keyed dependencies.
- `Quantity` and `Modality` remove Pets scalar and intensity variants from family renderers.
- `ChangeFrame` replaced scattered change probes with one closed construction family.
- `TriggerFrame` consolidates mutually exclusive event categories while action-use language remains
  orthogonal.
- `Clause` and related types preserve enough structure for coordination and final linearization.
- `Rendering<T>` carries visible fallback text together with typed `Unresolved` evidence.
- `English` remains the facade for standalone descriptions and card-region assembly.
- `Describers` validates inherited lexical facts once at construction.

Do not introduce a separate semantic `Description` layer merely because an earlier review proposed
one. Frames, orthogonal roles, protocols, and clauses may already carry everything aggregation
needs. Add another layer only when a current decision cannot be represented honestly without it.

## Prioritized architecture work

When English architecture is selected, use this dependency order.

### 1. Structure conditions and counts

Requirements and metrics still pass assembled conditions and counted phrases across the renderer.
Identify their semantic roles and remove those strings while doing so. Verify whether their
constructions are mutually exclusive before calling the result a frame; use orthogonal facts where
one Class can participate in several roles.

This round should make factoring depend on roles rather than equality of rendered verbs or
modifiers. `Clause.Prefaced` should take structured condition material, and `renderGateCondition`
should return structure.

### 2. Decompose event realization

Keep event kind, actor constraint, voice, and complements independent. A destination such as “to
this card” is a complement, not an event kind; the existing `ADD` event already follows this rule.
Continue by separating active/passive realization from semantic event kind. Do not force
action-use wording or the payment protocol into `TriggerFrame`.

### 3. Re-examine effects as interpretations

The matcher chain in `renderEffect.kt` is evidence that some concepts may still be unnamed, but it
does not prove that one frame is missing. For each recurring matcher group, decide whether it is:

- an exclusive construction suited to a frame;
- an orthogonal lexical role;
- a cross-element protocol;
- or an irreducible, honest branch in the interpreter.

Payment should be improved on its own terms. In particular, determine whether `BillingEvent` and
`renderBillingTrigger` are two representations of one protocol decision. Do not judge the result by
how many helpers disappear; judge whether ownership becomes clearer and recurring recognizers are
deleted.

### 4. Audit card-operation recognizers

`renderCardOperation` and `CardCriterion` need an explicit scope. A surviving construction must be a
structural interpretation of a recurring Pets form, a narrow lexical fact, corrected Pets, or
visible unresolved source. A type described as serving one canonical operation is presumptively a
recognizer.

### 5. Finish ownership and layout

Move expansion-owned lexical declarations toward their bundles when that work can replace the
central registry cleanly; do not make registry movement a prerequisite for unrelated rendering.
Extract a card-layout model only after semantic rendering no longer depends on printed regions.

Then re-ask whether clauses are a sufficient intermediate representation.

## Working rules

- Make one systemic transformation at a time. Name the recurring family or existing machinery it
  will replace before implementation.
- Frames are closed; lexicon entries are open. A new expansion should normally add entries, not
  frame variants or renderer branches.
- Treat symmetry as a question, not a mandate. Similar families deserve comparison, but different
  composition rules are evidence for different shapes.
- A nullable matcher ladder is a prompt to investigate, not proof of a missing frame.
- Never recognize a whole card. Narrow Procedure or Wrapper wording is acceptable only when it
  delegates represented Pets back to a general renderer.
- Change the active path and delete superseded machinery. Do not maintain parallel converters.
- Prefer canonical wording derived from meaning over incidental published variation. Preserve
  authored semantic order.
- Use `raise` and `lower` for global parameters and a player's terraform rating; use `increase` and
  `decrease` for production. Spell out `terraform rating` in card text.
- Describe a card-resource location as `this card`, never `here`.
- Introduce a triggering event with `when`, never `each time`. Describe one event; express the
  result's multiplicity when one event produces several changes.
- Render every ratio with `per`, whether its denominator is one or greater and whether or not its
  result is victory points.
- Make optional maxima explicit as `you may ... up to`, including when the maximum is greater than
  one.
- Use `pay` when standard resources are a cost for obtaining or doing something. Use `remove` for
  card-resource costs and for standalone or involuntary resource reductions, including standard
  resources. Do not use `spend` or `lose`. Describe substitution as `may be used as`, without a
  payment verb.
- Join a rendered action cost to its result with `to`. Refuse a costed action whose result cannot be
  expressed as an infinitive; do not split it into separately modalized sentences.
- Render `PlanetaryTag` as `planetary tag`.
- Bracket unsupported Pets rather than buying coverage with a one-card mechanism.
- Do not build a general natural-language framework or support hypothetical games. This renderer is
  for Terraforming Mars.

## Evidence and verification

Use evidence in this order:

1. Pets and the canonical Class model for meaning.
2. Original published card text for wording evidence.
3. `english-card-text-goals.tsv` as fallible reviewed targets.
4. `english-card-text-current.tsv` as generated characterization, never a production answer source.

After an intentional output change, run:

```text
./gradlew :tfm-text:writeEnglishCardTextCurrent
./gradlew :tfm-text:test
```

Review the production diff and generated snapshot together. Group every distinct before-to-after
wording transformation, count affected rows, give representative examples, and list unexplained
changes individually. Pause before regenerating a change that would materially affect more than
roughly 25 cards.

Track these as diagnostic trends, not optimization targets:

- unresolved nodes grouped by typed refusal reason;
- assembled-string sites that interpolate or concatenate semantic roles;
- positional `Expression.arguments` inspection outside role resolution;
- production lines, nullable branch exits, and new renderer files;
- frame variants and lexical fields that fail the modeling test above.

Raw field count and raw `NounPhrase.text` count are not design metrics. If a round adds production
lines, nullable exits, and renderer files together, stop and explain the cost before continuing.

Do not add a test merely to prove that one canonical card lost brackets. The all-card snapshot is
the characterization for corpus coverage. Add a focused behavioral test when a new rule is not
exercised by canonical cards or when a meaningful semantic invariant needs direct proof.

## Source map

- [`English.kt`](../../src/jvm/dev/martianzoo/tfm/text/English.kt) — facade and card-region assembly.
- [`EnglishSyntax.kt`](../../src/jvm/dev/martianzoo/tfm/text/EnglishSyntax.kt) — clauses and the
  linearizer.
- [`ComponentDescriber.kt`](../../src/jvm/dev/martianzoo/tfm/text/ComponentDescriber.kt),
  [`Describers.kt`](../../src/jvm/dev/martianzoo/tfm/text/Describers.kt), and
  [`TerraformingMarsDescribers.kt`](../../src/jvm/dev/martianzoo/tfm/text/TerraformingMarsDescribers.kt)
  — lexical facts and inheritance.
- [`ExpressionResolver.kt`](../../src/jvm/dev/martianzoo/tfm/text/ExpressionResolver.kt) — structural
  Class and dependency roles.
- [`Rendering.kt`](../../src/jvm/dev/martianzoo/tfm/text/Rendering.kt) — visible fallback and refusal
  evidence.
- `renderActions.kt`, `renderChange.kt`, `renderEffect.kt`, `renderInstructionTree.kt`,
  `renderMetric.kt`, and `renderRequirement.kt` in the same source directory — family interpreters.
- [`EnglishCardTextCurrentGenerator.kt`](../../test/jvm/dev/martianzoo/tfm/text/EnglishCardTextCurrentGenerator.kt)
  — generated corpus snapshot and refusal report.
