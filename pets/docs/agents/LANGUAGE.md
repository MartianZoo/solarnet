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
card or goal that contains it. Card procedures must derive from Pets' anonymous card locations,
offers, and filters; incomplete interpretation remains visible as bracketed Pets.

Published text is normally the proving corpus, not the production data source. It is evidence for
meaning and good wording; incidental variation is not a rule. Prefer one clear, consistently derived
sentence for equivalent Pets.

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

An enclosing clause can also keep a recognized head while a nested metric or `HAS` refinement
remains unresolved. The `PER` modifier retains its rendered instruction and brackets only the metric;
filtered component and location nouns bracket only an unrecognized refinement. This is a nested
lexicalization point after the applicable refinement recognizers have declined it.
An exact positive card invariant can likewise state its quantity as a gain while leaving the
component noun in brackets. The bracket retains the full Pets expression, including its arguments.
Vocabulary can supply a literal sentence for a specific card invariant when its printed meaning
needs explanation beyond the component count. The Terraforming Mars vocabulary supplies the wild
tag's printed explanation, placing it below the artwork with the card's play-time text. Its
`tagAssignmentHolder` fact also enables generic `EACH` wording for assigning unused holders and
card filters accepting a tag either directly or through an assignment holder. The same tag
criterion is retained across a card-copy sequence; concrete holder identities stay in vocabulary data.
When a card selection accepts a named tag directly or through an assigned printed wild, it uses
the named tag in English. A marked card selected through the corresponding gate uses that wording.

`Describers` still exposes several narrower producers (`componentNounPhrase`,
`quantifiedComponentNounPhrase`, `cardResourceNounPhrase`, `playedTagPhrase`, `describedNoun`,
`plainGainNoun`, `plainGainCategoryNoun`) that each fuse a classification guard to a noun
derivation. These serve contexts that already established a narrower semantic role; do not use them
to recreate a competing general expression dispatcher.

`cardCriterionModifier` owns the shared tag, property-presence, and icon filter wording for card
searches and metrics. Callers retain their context-specific restrictions and choice of an attributive
tag name versus a `with` modifier. `renderPropertyThreshold` shares numeric property comparisons
between metrics and event triggers; action-use triggers retain their positive-value shorthand.
Payment effects and persistent value grants share `renderExtraResourceValue` after their separate
recognizers establish the resource and value. The best-effort qualifier is `Modifier.BEST_EFFORT`;
modifier ordering must use that shared value rather than independently spelling its text.

Production resource filters can match placement bonuses at a described site; occupant filters and
spatial owner variables retain the tile and player relationships. A site occupant described as
"this tile" must name a concrete class gained directly by the current card. A `PER` count of a concrete type
whose compiled component limit is at most 1 uses the existing conditional wording. Abstract counts
remain quantities: a limit on each specialization does not cap their combined count.

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
live under `src/common/dev/martianzoo/tfm/canon/bundles/`, in each bundle's
`english/english.kt`, and are compiled by `text`. `Describers`
owns Class resolution; absent Classes fall back to structural interpretation and default naming.
For an opaque `Custom` requirement, `requirementKind` supplies only its English name: standalone uses say
the player must meet that named requirement, rather than duplicating its hidden gameplay definition.

The module name `text` is aspirational: its production code still depends on `tfm-canon`, including
card helpers and production constants, and contains named Mars model assumptions beyond those
imports. Moving the vocabulary into Canon requires removing that dependency first so Canon can
supply the vocabulary without a cycle. Pets TODO tracks this separation; the module rename does
not claim that it is complete. Common card/resource interpretation and English grammar should
remain in text, without introducing a parallel semantic model.

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
- Prefer structural rendering and sparse vocabulary facts. On 2026-10-14 the user requested an
  experiment without the exact-expression phrase table. Its instruction, effect, metric, and
  requirement substitutions are removed from the active renderer; incomplete composition remains
  visible as bracketed Pets. Do not restore complete rule strings to improve snapshot agreement.
  A named procedure's vocabulary still needs review when its declared behavior changes.
- Never bend a lexical fact to hit a target string. If a component's noun changes, check every other
  place that noun appears before accepting it.
- Change the active path and delete superseded machinery. Do not maintain parallel converters.
- Payment values interpret explicit `Pay` and `PayFromCard` exchanges while the matching
  `Accepting` or `AcceptingFromCard` is present. Keep actor, resource, holder, and currency restrictions intact;
  an arbitrary loss is not a payment. `Billing` supplies the represented payment subject, action
  slot, and currency; `PayingFor` supplies the card or tag being paid for. Preserve the subject's
  named Type-variable references when reading its property restrictions.
- Two general paths that render equivalent Pets differently are a defect, even when both are
  general. Ordering is not a design.
- The realization layer is game-neutral. The interpretation layer may name common game concepts
  (cards, actions, resources, production, placement, payment, scoring). Concrete component identities
  and expansion-specific recognizers stay in vocabulary data.
- Do not build a general natural-language framework or support hypothetical games. This renderer is
  for Terraforming Mars.
- Treat symmetry as a question, not a mandate. Different composition rules are evidence for different
  shapes.
- Sponsored Academies' grouped player fanout remains unsupported. Card-targeted procedures can use
  a tag gate to select their card; sequential exclusions refer back through recorded Type variables.
  An observing condition can introduce a referent before the operation supplying its value. English
  therefore considers all earlier occurrences, not only occurrences following the declaration.

## Wording decisions

- Wild-tag allocation instructions are implicit and omitted from card prose, including optional
  allocation fanouts and Cloud Tourism's allocation procedure. The printed wild card's own rule
  sentence remains visible. Tag predicates and counts still render normally.

- Omit a global parameter's incomplete-track condition when it guards a single increase of that
  same parameter, whether authored as a gate or a multiplier. Its vocabulary marks this wording
  convention; conditions guarding other gains remain explicit.

- Describe starting resources as `Gain ...` and starting production as an `Increase ... production`
  instruction, rather than using the printed `You start with ...` construction.
- Describe reveal-until-matching-card searches as `Draw N [kind] cards` in card goals, consistently
  across that search family. Tag names can modify `cards` (`Draw 2 plant cards`); other
  criteria must stay explicit (`Draw 2 cards with floater icons`, never `floater cards`).
- Describe an exchange of anonymous card backs as `Discard N cards to draw N cards`.
  An unconditional choice to decline the exchange makes the complete action optional.
- Card-play instructions resolve card back, card face, and source location by dependency role.
  Cost reductions and requirement waivers modify that same rendered instruction, preserving its
  deck and source rather than assuming a card from hand. Unsupported face restrictions remain
  visible as Pets.
- `Selecting` and `Revealed` are temporary card locations. Gaining card backs into `Selecting`
  means looking at them. Moving backs from `Selecting` into the owner's hand means keeping them
  from the selection. Render these instructions independently; do not infer an immediate discard
  from the offer size. Current turn cleanup owns the unselected cards.
- Interpret explicit purchase, play, reward, and return procedures from Pets. The selected Prelude's
  play-or-discard choice and 15 M€ compensation are read from `PlayPrelude` Pets wherever that signal
  is used. The `if it cannot be played` qualifier describes a caller-verified rule; `PlayPrelude`
  Pets does not gate its discard branch on unplayability.
- A required first action can contain multiple clauses. Keep all of them under its first-action
  preface, including any narrow unresolved fragments.
- Count production categories by their concrete members, just as for refined production selections:
  `6 PROD[Metal]` means steel and titanium production combined, not a choice between the two.
- Card-resource counts restricted to an unrefined `CardFront` retain the ordinary card-resource
  noun, including distinct-kind counts. Specific holders, holder filters, and other-player ownership
  remain explicit or visibly unsupported. Vocabulary may name `implicitKindExclusions` already
  understood in its distinct-kind noun: the Terraforming Mars resource vocabulary supplies
  `RobotUnit`, preserving the established Collector and Diversity Support wording. Other exclusions
  and additional presence requirements are not silently discarded.
- Normally describe variable amounts as `1 or more`, followed by `that amount`, `the same number`,
  `twice that amount`, or `triple that amount`, as appropriate. Use X notation when a multiplied
  cost must precede the base amount, as in `Spend 2X M€ to gain X energy`. A card count linked to
  an action cost uses `that many` only when the quantities match. This distinction depends
  on the quantities, not the card. Preserve genuinely zero-inclusive choices separately.
- Recognize payment value from explicit `Pay` and `PayFromCard` events. Ordinary resource-loss
  triggers do not establish payment purpose.
- Keep permission to use special payment resources concise: `Spend 8 M€ (steel may be used)`.
- Use `raise`/`lower` for global parameters and terraform rating; `increase`/`decrease` for
  production. Spell out `terraform rating`.
- On 2026-10-14 the user clarified that mechanical, structurally derived text takes priority over
  smooth goal wording. Prefer separate sequential instructions, explicit numeric conditions, and
  actual conversions over complete-card paraphrases. The current experiment removes all exact-expression idioms, including those whose ordinary
  composition is still incomplete.
- Render AMAP changes with `(to the extent possible)`. Do not describe a raise followed by a loss
  as preventing or ignoring the raise. Omit routine end-of-game timing from scoring text.
- Card-front-to-played-event changes say `move ... to ... event pile`, without explanatory reminders.
  Explicit `that card` and `that player` references follow recorded variable occurrences. The older
  unmarked Anyone-to-Player normalization for interaction triggers remains a separate implicit path.
- Equal thresholds covering every standard resource type share their amount: AND gives `each`, OR
  gives `any`. Counted Class categories use `type`; do not add `total` to an unqualified production
  count. A filtered tile count still counts tiles, with its area's adjacency restriction stated
  explicitly rather than paraphrased as map edges.
- Omit the routine oxygen reminder from greenery placement text. If a reminder is included, use
  the parenthetical form `(and raise oxygen 1 step)`.
- Use concise `Requires ...` for milestone counts, where the claimant is implicit; keep explicit
  ownership in card requirements. Render milestone requirements independently of their direct
  effects: Briber's direct effect loses 12 M€, but its requirement must never be replaced by payment
  advice.
- Begin every requirement with `Requires`. Use `Requires that you have` for player-owned state such
  as resources, production, rating, tiles, and colonies. Use a terse noun phrase for tags and global
  counts, and `Requires that` for other clauses. Describe temperature bounds as `warmer` or `colder`,
  not `higher` or `lower`.
- In reviewed card goals, spell out a party requirement as the named party ruling or the player
  having 2 delegates there.
- Delegates use the ordinary positioned-component construction; vocabulary supplies `in` for a
  party and the nouns for delegates, non-leader delegates, and party leaders. Preserve sequential
  placements and use `that party` only for a shared Type variable. Replacements retain source and
  destination ownership; `neutral` comes from the owner's vocabulary. Do not infer reserve/lobby
  choices from a bare placement instruction.
- Render a counted `NOT` refinement by composing the included and excluded count phrases with
  `excluding`, as in Lobbyist's delegates excluding lobby delegates. Preserve unsupported filters
  as bracketed Pets rather than silently omitting them.
- Use `spend` when a standard resource or one of your card resources is consumed as an action cost.
  Use `remove` for taking resources from any player's card and for standalone or involuntary
  reductions. Use `decrease` for production costs. Use `pay` for
  non-action payment constructions. Describe substitution as `may be used as`, with no payment verb.
- Join a rendered action cost to its result with `to`. Refuse a costed action whose result cannot be
  an infinitive; do not split it into separately modalized sentences. A mandatory standard-resource
  removal followed by `THEN` is a payment for its result and joins with `to`.
- Factor a shared subject and verb across adjacent instructions with distinct objects and no clause
  modifiers. Do not make independently chosen destinations look shared or collapse repeated
  operations. Keep consecutive production changes in one sentence even when their verbs differ.
- When adjacent production changes in the same direction use the same magnitude, state the
  magnitude once and end with `each`.
- State every renderable condition in a compound gate; retain a phase condition when it
  restricts an effect to that phase.
- Describe spatial adjacency as `next to`, never `adjacent to`.
- Render discount effects declaratively as `you pay N M€ less`, adding `for it` when the trigger
  supplies a clear discounted object.
- Introduce a triggering event with `when`, never `each time`. Describe one event; express the
  result's multiplicity when one event produces several changes.
- In removal triggers, describe the removed component's owner without claiming that owner
  performed the removal; `BY` is what constrains the actor.
- Render every ratio with `per`, whatever the denominator and whether or not the result is victory
  points.
- Render `EACH` as a repeated instruction prefaced by `For each ...:`; reserve `per` for
  `PER` scaling. A marked selector may supply `that` inside the body.
- For production counts over a complete resource category with one excluded type, name the
  exclusion instead of listing all remaining concrete types.
- A numeric property filter on a placement site can state its actual comparison when the property
  has a simple word name: `HAS 6 row` says `whose row is at least 6`; `HAS MAX 5 row` says
  `whose row is at most 5`. Other property names retain their Pets source unless vocabulary
  supplies a description.
- A class-token count with a matching component-presence filter may use that component's count noun;
  additional `HAS` filters remain attached and bracket individually when unsupported. This keeps
  Philantropist's class-token gameplay requirement while saying `cards in play with [GainsOf<...>]`.
- Preserve shared implicit player identity across a trigger and its result as `that player`.
  An unrestricted `BY Anyone` or `BY Actor` constrains the performer of the event; it does not
  establish the owner of resources mentioned independently in the result.
- Make optional maxima explicit as `you may ... up to`, including above one.
- Express an operation with an unconditional option to do nothing as `you may ...`. Keep the
  operation's costs and results within that permission; preserve all-or-nothing quantities and
  conditional restrictions on declining.
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
  metric-based victory-point behavior. An `End` result without a scoring meaning must retain its
  Pets text rather than appear as an immediate gain or loss.
- Render `PlanetaryTag` as `planetary tag`.

Card rendering calls out `including this` when the entering card contributes to a metric in its
direct effect
or satisfies a supported tag or card-play trigger and the reader could reasonably be unsure whether
it counts. It does not infer the phrase through setup operations or effects that the renderer cannot
otherwise interpret.

## Evidence and verification

Use evidence in this order:

1. Pets and the canonical Class model, for meaning.
2. Original published text, for wording evidence.
3. `english-card-text-goals.tsv` / `english-goal-text-goals.tsv`, as fallible reviewed targets.
4. `english-card-text-current.tsv` / `english-goal-text-current.tsv`, as generated characterization —
   never a production answer source.

`english-global-event-published-wording-evidence.tsv` records the 31 Turmoil and five promotional
Global Events in spreadsheet order. Its effect text comes from the
[Global Events sheet](https://docs.google.com/spreadsheets/d/12FF6VyIKr8HArRR9zjkaIR-PEUql6QnNBbngvI3Fzjo/edit?gid=418375937#gid=418375937),
visually checked against all 36 [Hadronikle scans](https://tm.hadronikle.com/) on 2026-09-25.
Line wrapping and the outer reminder-text parentheses are omitted; internal punctuation is retained,
including the scan's comma in Riots' `(max 5, then reduced by influence)`. Names use the spreadsheet's
capitalization and spelling (the Successful Organisms scan misspells its title `SUCESSFUL`). Upper
titles, flavor text, icons, and spreadsheet notes are outside this effect-text corpus; Dry Deserts'
upper title is `Minimal Impact Policy` on the scan, not the sheet's `Free Academia Treaty`.

Card text reads persistent resource-value grants from exact invariants. Other exact, non-tag
invariants remain visible as unresolved Pets in the effect region until their meaning is supported.
Tags remain card metadata. Lexical `Me` references mean the current owner; a named player fanout
must explicitly target its selected player, while `EACH Me@Player` rebinds ownership in its body.
Event attribution uses `Actor`, independently of ownership.

Standalone `English.describe(instruction, player)` optionally addresses a concrete player as
`you`. `playerRelativeInstruction` rewrites the AST before interpretation, omitting that player's
ownership only when the remaining arguments keep their dependency roles. Compact transfers are
expanded to preserve both endpoints; `BY` retains its actor and can say `performed by you`.
Instructions already containing a lexical `Me` are left unchanged to avoid capturing a fanout's
owner. The public `EnglishInstructionTextRenderer` exposes this rendering with the loaded class
table and task assignee; callers use null for neutral wording such as Admin instructions.
Concrete card selectors, placement sites, named card-resource holders, and card action providers
retain their names; explicit action slots remain visible. For named content targets (cards, goals, colony tiles, parties, and similar selected items),
use literal Class Names directly. The owner explicitly authorized this policy; do not add per-item
describers merely to supply display names. Procedure `targetRelation` also
supports named placement sites (such as trading with a colony tile). Explicit `Hand` gains/removals
use the ordinary draw/discard wording; discards from `Selecting` and `Revealed` name their source.
Typed resource destinations use "any card" when their resource type matches the change, retaining
extra restrictions such as a science tag; removals retain "your" ownership. Optional changes
preserve the quantity and target. Only mandatory discards are phrased as costs for a later result;
optional discards retain sequential wording so the reward remains unconditional. Explicit
resource-payment transmutations say "spend", using the existing payment-event interpretation;
actions and standard projects retain their provider and explicit slot. A sequence inside an OR
branch is parenthesized, and coordination must not flatten AND objects into OR alternatives.
Readable output is not proof of complete task rendering: named award/card-play targets, variable
payments, and additional internal bookkeeping remain follow-ups in `TODO.md`. Recognized `Audit`
gains are omitted from instruction groups/sequences with the owner's approval; payment obligations,
gates, and unknown System work are not suppressed.

The once-per-action production reward recognizer reads a marker gained when its owner's action
scope begins. The vocabulary identifies the scope as an action; the marker must depend on that same
scope. Its unit limit, production trigger, and consume-on-reward structure determine the rendered
rule.

The card goals cover the same corpus as the card current and corrected files. That corpus follows
the published wording evidence, including replay-only cards and the abstract Beginner Corporation.
Retain supported printed constraints in reviewed goals even when Pets does not yet enforce them;
record the model gap separately. Such a target is not permission to invent a renderer special case.
The goal-text goals may cover a selected proving corpus; the goal current snapshot covers every
concrete goal. Current refusals are recorded mechanically in the generated `*-refusals.tsv`; do not
restate them here. Card and goal snapshot assertions collect wording differences across the entire
corpus in each run, rather than stopping at the first mismatched component.

Global Event resolution rendering is paused after its lower-half `GlobalEffect` declarations moved
to player-owned signals. The published wording evidence remains available for a later renderer.
`EnglishGoalTextCurrentGenerator` writes goal snapshots and refusal reports.

The category reports in [`docs/english-wording-comparisons/`](../english-wording-comparisons/README.md)
compare the published evidence with live renderer output and serialized authored Pets declarations.
Regenerate them with `./gradlew :text:writeEnglishTextComparisons`. They enumerate every canonical
card and concrete goal, plus evidence-backed replay card models and replay-only goals; missing
printed transcriptions and replay provenance remain explicit. These reports currently cover cards
and goals; no saved current-text snapshot is used as input.

After an intentional output change, run:

```text
./gradlew :text:writeEnglishCardTextCurrent
./gradlew :text:writeEnglishGoalTextCurrent
./gradlew :text:test
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
semantic invariant needs a focused check.

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
  [`turmoil/english/english.kt`](../../src/common/dev/martianzoo/tfm/canon/bundles/turmoil/english/english.kt)
  — lexical facts, composition, and inheritance. Each bundle directory is an explicit `text`
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
