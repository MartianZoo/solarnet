# English renderer architecture direction

> **Read when:** changing the renderer's intermediate representation, refusal reporting,
> dependency resolution, lexicon ownership, or removing recognizers across a whole Pets family.
>
> **Skip when:** adding ordinary coverage or changing wording within the current architecture; use
> [LANGUAGE.md](LANGUAGE.md). Do not read this as a description of committed output.
>
> **Status:** settled constraints plus remaining architecture work. Completed migration history has
> been removed.

## Source map

- [`EnglishSyntax.kt`](../../tfm-text/src/main/kotlin/dev/martianzoo/tfm/text/EnglishSyntax.kt) —
  search for `sealed interface Clause` to see the current partial intermediate representation.
- [`Rendering.kt`](../../tfm-text/src/main/kotlin/dev/martianzoo/tfm/text/Rendering.kt) — search for
  `enum class RefusalReason` for the implemented total-result boundary.
- [`ExpressionResolver.kt`](../../tfm-text/src/main/kotlin/dev/martianzoo/tfm/text/ExpressionResolver.kt)
  — inspect dependency-by-key resolution and retained source dependencies.
- [`ComponentDescriber.kt`](../../tfm-text/src/main/kotlin/dev/martianzoo/tfm/text/ComponentDescriber.kt)
  — search for `sealed interface ChangeFrame` before extending the lexicon model.
- [`TerraformingMarsDescribers.kt`](../../tfm-text/src/main/kotlin/dev/martianzoo/tfm/text/TerraformingMarsDescribers.kt)
  — inspect only when moving lexical ownership or measuring remaining centralized facts.
- [`EnglishCardTextCurrentGenerator.kt`](../../tfm-text/src/test/kotlin/dev/martianzoo/tfm/text/EnglishCardTextCurrentGenerator.kt)
  — search for `refusalRows` when changing corpus review output.

## Settled constraints

These decisions constrain future rounds; ordinary work should not relitigate them.

1. The renderer is Terraforming Mars-specific, but expansions may recombine familiar meanings in
   shapes absent from the current card set. Do not promote an accident of today's data to a rule.
2. The target flow is Pets → a semantic description → English realization. Only English is being
   built, and the semantic layer should grow one evidenced family at a time.
3. Published cards are the golden use cases, but arbitrary Pets remains the public target. Unsupported
   source stays visible in brackets at the narrowest safe boundary.
4. Facts already present in the AST or Type system must be derived there. A lexicon entry may carry
   language facts, never a second answer to a structural question.
5. Track, placed, held, countable, and similar categories are presentation distinctions. Do not add
   them to Pets solely for English rendering.
6. Resolve expressions through the Class Table and read dependencies by `Key`. Positional argument
   recognition is transitional. In card context, an omitted owner means the card owner; explicit
   `Anyone` opts out.
7. Consistent derivation matters more than matching stylistic variation in published card text.
8. Never recognize a whole card. An exceptional component may declare one narrow lexical Procedure;
   a Wrapper contributes a preface and delegates its contents to the ordinary renderer.
9. Adding an expansion may add lexical entries. It should almost never add a new frame or renderer
   concept.
10. Canonical `CARDS[...]` operations carry their printed hidden-card procedure. Do not restore a
    separate filtered-draw supplement or card-specific context channel.
11. Card layout is a client of Pets rendering, not the semantic renderer's core responsibility.
12. Change the active path in place and delete superseded machinery. Do not maintain a parallel
    converter.

## Current implementation boundary

This table is for routing only. [LANGUAGE.md](LANGUAGE.md) owns exact current output coverage.

| Area | Committed now | Remaining architecture pressure |
| --- | --- | --- |
| Refusal | `Rendering<T>` carries visible output plus typed `Unresolved` entries; the corpus generator emits a ranked report. | Many internal family helpers still return nullable partial results. Keep refusal reasons at the family boundary instead of multiplying reporting layers. |
| Expression meaning | `ExpressionResolver`, `ResolvedExpression`, `Quantity`, and `Modality` centralize much of the source-to-semantic conversion. | Some paths still inspect source argument positions or lose linked identity across separately rendered stages. |
| Change lexicon | `ChangeFrame` expresses countable, held, scale, positioned, deck, procedure, wrapper, and play constructions. | `ComponentDescriber` still carries many family-specific facts, and one central Terraforming Mars registry owns every bundle. |
| English structure | Clauses, predicates, noun phrases, coordination, and modifiers exist and are used by every main family. | Preassembled strings still occupy structural slots, so factoring sometimes depends on wording rather than semantic roles. |
| Requirements and metrics | They produce clauses and reuse resolved expressions in important paths. | Their object phrases and bound constructions have not yet proved a shared role-bearing semantic model. |
| Triggers and effects | Common triggers and effects compose through clause structures. | `renderEffect.kt` still contains broad shape recognition, trigger-specific event kinds, and string prefaces. |
| Card operations | Canonical operations render structurally from `CardOperation`. | Card-region assignment still lives in `English` rather than a named layout boundary. |

## Remaining work, in dependency order

### 1. Finish semantic expression resolution

Remove positional matching from renderer families only when the resolved dependency retains the
required meaning. Preserve authored linked variables across sequences so a later stage can refer to
the same participant structurally. Do not replace one positional check with a Class-name check.

Useful searches:

- `expression.arguments` in `tfm-text/src/main` finds remaining direct source-position reads;
- `sourceDependency(` finds intended key-based access; and
- the `Flooding` declaration and English output constrain linked participant identity.

### 2. Narrow the lexicon

Move expansion-owned lexical data toward its bundle and reduce `ComponentDescriber` to language
facts that recur across renderer families. Structural membership such as `CardResource`, `Tag`, or
`Production` must come from the Class Table.

Do not make registry movement a prerequisite for unrelated wording work. When selected, validate
the complete lexicon once at construction and retain vocabulary-derived default nouns for Classes
without explicit entries.

### 3. Give changes role-bearing semantics

For gain, removal, and transmutation, represent the meaning needed for realization: lexical head,
party, direct object, extent, place/oblique, modality, and adverbial information. Keep each change's
object and extent together so coordination cannot detach a count from its resource or production.

Two changes may factor only when their invariant roles agree. Factoring must not compare rendered
strings or require a syntax value whose unexplained purpose is to render nothing.

Stop and report if this family requires about ten distinct semantic kinds or a kind serving one
card. That would recreate the old sparse record under a new name.

### 4. Extend only when another family proves a role

Move requirements and metrics next, then triggers and effects. Add a complement role only when a
current construction cannot be represented honestly without it.

For triggers/effects, model the finite verb group compositionally: lexical head, voice, polarity,
and evidenced auxiliary/modality. Destination is a complement, not an event kind. Distinguish an
expressed subject from the understood “you” of an imperative. Replace string prefaces with a
structured fronted constituent only when wrappers, gates, and trigger forms can share it.

### 5. Delete surviving recognizers and name card layout

Each component-specific matcher must become a general structural rule, a narrow Procedure lexical
entry, corrected Pets, or visible unresolved source. Report lost coverage instead of hiding it.

After semantic rendering is independent of card regions, extract the above/below-artwork assignment
as `CardLayout`. Do not solve unresolved published layout distinctions by adding presentation
meaning to Pets instructions.

## Review discipline

Make one systemic transformation the unit of review, not an arbitrary number of cards. For each
round, report:

1. the rule that changed;
2. each distinct before→after transformation, with a count and two examples; and
3. every unexplained row individually.

Keep a review round to roughly ten distinct transformations and zero unexplained rows. The generated
snapshot is evidence for behavior change; the refusal report identifies the next recurring boundary.
Neither authorizes new machinery for one or two cards.

## Open decisions

- A site dependency may make “positioned” structurally derivable, but only if every applicable
  component should read as placed. Otherwise the frame remains lexical.
- Production may be one scale frame with an owner role, or a distinct frame if merging them needs a
  special case. Let the role-bearing change model decide.
