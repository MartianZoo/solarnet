# English card-text derivation

> **Read when:** changing English output, a renderer family, the component lexicon, card-region
> layout, refusal behavior, or review of generated card text.
>
> **Skip when:** changing Pets meaning without changing human rendering.
>
> **Status:** current behavior, settled architecture constraints, and prioritized remaining work.

## Read only the relevant sections

| Task | Read |
| --- | --- |
| Add support for one recurring Pets shape | Direction and pace; Expected renderer architecture; Transitional derivation; Review cadence |
| Change the intermediate representation, dependency resolution, or lexicon ownership | Architecture constraints; Current architecture; Prioritized architecture work; Expected renderer architecture |
| Select the next renderer architecture task | Current architecture; Prioritized architecture work; Review cadence |
| Change wording or lexical categories | Canonical wording versus rules; Component nouns and change verbs |
| Change actions, triggers, effects, requirements, or metrics | The matching part of Current derivation scope, then its renderer source below |
| Change which text appears above/below artwork | Known layout regions |
| Review a corpus-wide change | Verification while replacing the data file; Review cadence |

## Source map

- [`English.kt`](../../src/jvm/dev/martianzoo/tfm/text/English.kt) — inspect the
  public entry points and card-region assembly.
- [`EnglishSyntax.kt`](../../src/jvm/dev/martianzoo/tfm/text/EnglishSyntax.kt) —
  search for `sealed interface Clause` before changing the intermediate representation.
- [`Rendering.kt`](../../src/jvm/dev/martianzoo/tfm/text/Rendering.kt) and
  [`RenderedInstructions.kt`](../../src/jvm/dev/martianzoo/tfm/text/RenderedInstructions.kt)
  — search for `Unresolved` when changing refusal cases.
- [`ExpressionResolver.kt`](../../src/jvm/dev/martianzoo/tfm/text/ExpressionResolver.kt)
  — read when ownership defaults or dependency-by-key resolution is involved.
- [`ComponentDescriber.kt`](../../src/jvm/dev/martianzoo/tfm/text/ComponentDescriber.kt)
  and [`TerraformingMarsDescribers.kt`](../../src/jvm/dev/martianzoo/tfm/text/TerraformingMarsDescribers.kt)
  — search for `ChangeFrame` and the relevant Class Name before adding lexical facts.
- Choose one family source: [`renderActions.kt`](../../src/jvm/dev/martianzoo/tfm/text/renderActions.kt),
  [`renderChange.kt`](../../src/jvm/dev/martianzoo/tfm/text/renderChange.kt),
  [`renderEffect.kt`](../../src/jvm/dev/martianzoo/tfm/text/renderEffect.kt),
  [`renderInstructionTree.kt`](../../src/jvm/dev/martianzoo/tfm/text/renderInstructionTree.kt),
  [`renderMetric.kt`](../../src/jvm/dev/martianzoo/tfm/text/renderMetric.kt), or
  [`renderRequirement.kt`](../../src/jvm/dev/martianzoo/tfm/text/renderRequirement.kt).
- [`EnglishCardTextCurrentGenerator.kt`](../../test/jvm/dev/martianzoo/tfm/text/EnglishCardTextCurrentGenerator.kt)
  — search for `refusalRows` when changing corpus review output.

## Architecture constraints

These decisions constrain every renderer round, including ordinary coverage work.

1. The target flow is Pets → a semantic description → English realization. Keep English outside the
   Pets AST and do not add presentation categories to Pets.
2. Published cards are the golden use cases, but arbitrary valid Pets remains the public target.
   Unsupported source stays visible in brackets at the narrowest safe layer.
3. Derive structural facts from the AST and Class Table. Lexicon entries may carry language facts,
   never a second structural answer.
4. Read dependencies by `Key`. Positional recognition is transitional and must be removed only when
   the semantic replacement retains the same meaning.
5. Never recognize a whole card. A narrow Procedure or Wrapper lexical fact is acceptable only when
   it delegates the represented instruction back to a general renderer.
6. Expansion work may add lexical entries but should almost never add a renderer concept or frame.
7. Prefer consistent derivation to incidental published wording variation.
8. Change the active path in place and delete superseded machinery; do not maintain parallel
   converters.

## Current architecture

| Area | Current implementation | Remaining pressure |
| --- | --- | --- |
| Refusal | `Rendering<T>` carries visible output and typed `Unresolved` entries; corpus generation emits a ranked report. | Many private helpers remain nullable branch attempts. Keep refusal ownership at the family layer. |
| Expression meaning | `ExpressionResolver`, `ResolvedExpression`, `Quantity`, and `Modality` centralize much source-to-semantic conversion. | Some renderer paths still inspect source positions or lose linked identity across separately rendered stages. |
| Change lexicon | `ChangeFrame` covers countable, held, scale, positioned, deck, procedure, wrapper, and play constructions. | `ComponentDescriber` still mixes recurring language facts with family-specific facts, and one registry owns every bundle. |
| English structure | Clauses, predicates, noun phrases, coordination, and modifiers are shared by every main family. | Preassembled strings still occupy structural roles, so some factoring compares wording rather than meaning. |
| Requirements and metrics | Both produce clauses and reuse resolved expressions in important paths. | Their objects and bounds have not yet proved a shared role-bearing model. |
| Triggers and effects | `TriggerFrame` consolidates recurring card-play, tag-play, action-use, purchase, and resource-spending events; common forms compose through clause structures. | `renderEffect.kt` still contains broad effect-shape recognition and string prefaces. Action-use wording remains separate because Classes can inherit it independently of an event frame. |
| Card operations and layout | Canonical operations render from `CardOperation`; `English` assigns card regions. | Semantic rendering is not yet independent enough to name layout as its own client model. |

## Prioritized architecture work

This order reflects both value and dependency. Do not select a lower item merely to improve one or
two cards.

### 1. Give one recurring change family role-bearing semantics

Select a recurring gain, removal, or transmutation construction whose current renderer already
contains duplicated or string-shaped decisions. Replace that active path end to end; do not begin by
declaring a complete semantic schema. Introduce only the roles the selected family proves it needs,
such as lexical head, party, direct object, extent, place or oblique, modality, or adverbial
information. Keep every object's extent attached to it so coordination cannot detach a count from
its resource or production.

Replace active string-shaped decisions rather than adding a semantic record beside them. Two changes
may factor only when their invariant roles agree. Stop and report if the work needs about ten
semantic kinds, a kind serving one card, or more permanent concepts than it removes.

### 2. Finish expression meaning as required by those roles

Remove positional matching only when the resolved dependency retains the required meaning. Use
`expression.arguments`, `sourceDependency(`, and contextual placement-site `This` as the focused
audit searches. Do not replace a positional check with a Class-name check.

Revisit cross-stage linked identity only after more than one current construction needs the same
role. Flooding remains a regression constraint, not authorization for player-reference
infrastructure by itself.

### 3. Narrow lexical ownership

Move expansion-owned lexical data toward its bundle and reduce `ComponentDescriber` to facts that
recur across renderer families. Structural membership such as `CardResource`, `Tag`, or `Production`
must continue to come from the Class Table. Validate the complete lexicon once at construction and
retain vocabulary-derived default nouns for Classes without explicit entries.

Do not make registry movement a prerequisite for unrelated wording work.

### 4. Extend roles only when another family proves them

Move requirements and metrics next, then triggers and effects. Add a complement role only when a
current construction cannot be represented honestly without it. For triggers and effects, model the
finite verb group compositionally; destination is a complement, not an event kind. Replace string
prefaces only when wrappers, gates, and triggers can share one structured constituent.

### 5. Delete surviving recognizers, then name card layout

Each component-specific matcher must become a structural rule, a narrow Procedure fact, corrected
Pets, or visible unresolved source. Report lost coverage instead of hiding it. After semantic
rendering is independent of printed regions, extract their assignment as `CardLayout`; do not add
presentation meaning to Pets instructions.

Open decisions remain evidence-driven: a site dependency can replace the positioned frame only if
every applicable component should read as placed, and production should merge with scale changes
only if the role model needs no special case.

## Direction and pace

The eventual goal is for `English` to render any instruction expressed in Pets. It now makes a
best effort without consulting whole-card text data. When a renderer cannot safely describe a Pets
node, it emits that node's canonical Pets source in square brackets at the narrowest safe structural
layer. An incremental result containing bracketed regions is expected and honest.

The language module currently targets the JVM only. Its source and resource loading use the
plain JVM source sets even though the Pets and Canon dependencies remain multiplatform.

`english-card-text-goals.tsv` is fallible, reviewed target text. `english-card-text-current.tsv` is
generated characterization of what the renderer produces for canonical cards. The renderer reads
card semantics from the loaded `cards.pets` Class. Neither file is an answer source for production
code. Run
`./gradlew :tfm-text:writeEnglishCardTextCurrent` after an intentional renderer change, then review
the current-versus-goal diff. Correct a goal row when card data or a systemic rule shows that it is
mistaken.

`english-published-wording-evidence.tsv` records published English card text. Its preferred source
is columns B, AP, and AQ of the `Cards` tab in
`https://docs.google.com/spreadsheets/d/12FF6VyIKr8HArRR9zjkaIR-PEUql6QnNBbngvI3Fzjo/edit?gid=5502005#gid=5502005`.
Canonical cards absent from that sheet retain the wording previously collected from the deployed
Terraforming Mars card catalog or supplied directly. Empty regions generally mean that the
published card communicates that region only through icons. This file is wording evidence, not
semantic authority or a production answer source.

## Verification while replacing the data file

Do not add a test just to prove that a newly supported shape removes brackets. Such a test would
restate the implementation scope, and every incremental step would require another synthetic
test. The all-card comparison against the generated current snapshot is the behavioral
characterization. Review the production diff and the regenerated snapshot together to establish
that the raw scope moved intentionally.

The explicit empty-region tests establish that structurally absent top or bottom content renders as
empty text. If a renderer gains behavior not exercised by any canonical card, add a behavioral test
for that behavior or defer the generalization.

Every active family entry point now returns total text together with explicit `Unresolved` records
for Pets retained as source. `writeEnglishCardTextCurrent` writes a ranked refusal histogram to
`english-card-text-refusals.tsv` while preserving `english-card-text-current.tsv` as the wording
characterization. Refusal reasons identify the unsupported construct that the renderer could not interpret,
such as its change frame, effect trigger, action cost, gate, or scaling form. Nullable private
matchers remain branch attempts inside those interpreters and do not create anonymous report rows.

Canonical `CARDS` transforms carry the structure of filtered draws, card selection and purchase,
reveal-and-test operations, and event recovery. The renderer decodes those structural families
directly; card identity does not supply hidden rendering data. Standard card-handling conventions
supply the discard of cards that were revealed or inspected but not drawn. An unqualified
`ProjectCard` gain remains an ordinary project-card draw.

Keep every structurally supported End-scoring sentence canonical in the goal data even when
another part of that card remains bracketed. The goal row should already contain
the complete scoring text before an unrelated instruction shape becomes derivable.

## Expected renderer architecture

Natural-language rendering should remain outside the Pets AST. Use one compositional renderer per
major sealed Pets family, such as instructions, requirements, metrics, triggers, and action costs.
Each renderer should recursively dispatch with a localized exhaustive `when`; do not replace those
dispatchers with English-aware methods on Pets nodes or a classic typed Visitor. The existing
`PetNode` visitor is appropriate for structure-insensitive descendant traversal, but rendering is
structure-sensitive: a parent determines whether a child is a noun phrase, clause, condition, cost,
or conjunction.

`English` should remain the card-layout facade. It decides which requirements, actions, effects,
and immediate instructions belong in each printed region, while the family renderers realize those
elements compositionally. Keep lexical policy, such as component nouns and change verbs, narrower
than the structural rendering rules.

`English.describe` exposes the same family renderers for one `Effect`, a list of `Action`s, an
`InstructionTree`, or a `Requirement`. Standalone rendering needs no host card because all Class
relationships come from the complete loaded Class map. Unsupported valid Pets shapes return
bracketed canonical Pets instead of failing or consulting a card row.

`English` is constructed with a complete `Map<Class, ComponentDescriber>` supplied by its client.
That map is a real lexical-data seam, not an alternate structural universe: expression resolution
intentionally uses the canonical Terraforming Mars Class Table. Map values are sparse declarations;
`Describers` resolves each requested fact independently through that Class's ancestry.
`TerraformingMarsDescribers` owns the canonical declarations and supplies an empty describer for
every undeclared canonical Class, keeping its map complete without copying inherited facts into
each value.

`ExpressionResolver` resolves expressions through the shared Class Table into `ResolvedExpression`: the
resolved `Type` plus authored dependency expressions indexed by the keys returned by the declaring
Class. Production and class-literal interpretation consume those keys rather than inferring owner,
resource, or represented-Class roles from argument positions. In card ownership context, an
otherwise-unresolvable explicit `Anyone` or complemented `Owner` is resolved through the applicable
player domain while its authored expression remains indexed by dependency key. A contextual `This`
used as a card-resource holder resolves against the holder dependency's declared Type while retaining
the authored `This` at that key. Contextual placement sites still cannot always resolve without their
linked source; the remaining positional recognition stays visible until rendering receives it.
Family renderers do not inspect `Expression.arguments`; dependency matching and the explicitly
documented contextual-production fallback are confined to expression-role resolution.
Membership in `Production`, `StandardResource`, `CardResource`, `Tag`, `PlanetTag`, `Player`,
`Generational`, and `End` is read from the Class hierarchy and is not duplicated in
`ComponentDescriber`. Card-resource nouns and number forms remain lexical data; whether a noun
contains the word `resource` is not a separate renderer category.
Pets scalars are resolved once into fixed or variable `Quantity` values, and intensities into
required, best-effort, or optional `Modality` values. Family renderers do not inspect the Pets
scalar or intensity variants.

Structural renderers only identify the component Class, ask `Describers` for the applicable
inherited phrase or capability, and compose the answer. Change rendering uses one inherited
`ChangeFrame`: countable, held, scale, positioned, deck, procedure, wrapper, or play. Production is
identified structurally. The frame contains the construction and its lexical words rather than a
second classification field or a component-shaped renderer variant. Structural renderers do not
name component Classes or
enumerate categories such as city tiles and colonies. Requirement descriptions supply only nouns,
value formats, and threshold subjects. The requirement AST determines minimum versus maximum and
therefore `higher` versus `lower`, while Class membership and the keyed owner dependency determine
player-owned versus unrestricted wording.

Restricted placements follow the same rule. A placement-site fact marks an expression argument as
the placed component's site and supplies its noun and optional definite article, while a
spatial-relation fact supplies the relation phrase and its implicit target noun. The placement
renderer reads owner and site dependencies by key, independent of authored argument order, and
interprets strict counting refinements on that site structurally; neither fact contains a card or a
complete instruction.
A placement site may decline inheritance when its specialized sites have different printed
semantics; those subclasses remain unsupported until they furnish their own site description.

The same spatial-relation fact can instead mark a component as a counted pair. Metric and
requirement renderers then obtain both participant nouns, articles, and ownership from those
participants' placement and requirement facts. Refined participants remain unsupported until their
refinement itself has a structural description.

Each nullable language fact is inherited independently: a declaration on a more
specific Class overrides the same fact from its superclass, facts from incomparable branches
compose, and differing values for the same fact from incomparable nearest providers are rejected.
Equal values from those providers coalesce. This keeps structural rendering closed over Pets AST
shapes while allowing a newly loaded component Class to reuse the descriptions of its supertypes.

A wrapper frame may interpret a gained concrete direct subclass of its declaring superclass. The
subclass must be a plain concrete declaration with one supported effect; the instruction
renderer handles that effect and the wrapper contributes only its preface. `Mandate` currently uses
this construction for `as your first action`. More specialized temporary effects remain bracketed.

Instruction changes and requirements are retained as internal clauses, predicates, noun phrases,
modifiers, and coordinations until their enclosing sentence or action has made its structural
decisions. One linearizer owns capitalization, punctuation, number agreement, and final text
assembly. This is a small renderer representation rather than a general English grammar framework.

Instruction rendering retains its ordered clauses until the enclosing instruction or action chooses
whether to make them separate sentences or coordinate them under one action cost. This small
representation keeps punctuation out of structural decisions without attempting to model general
English grammar.

Instruction, requirement, and effect rendering lower `PROD[...]` through the shared Terraforming
Mars Pets transformer before inspection. Their family renderers interpret the resulting
`Production` expressions using passive component facts, so production boxes do not need parallel
renderers for every Pets wrapper. `Describers` is limited to lookup and lexical access; it does not
render Pets families.

## Transitional derivation

Intermediate solutions may derive most of an instruction structurally while looking up one narrow
wording fragment. Prefer a granular table for the irregular fragment over either a
whole-card special case or new gameplay concepts. For example, city-placing cards might share the
normal placement derivation while a small table supplies only how each card describes the allowed
city location. Such tables are acceptable stepping stones toward broader structural derivation;
they need not solve the general case immediately.

Do not automatically populate such a table from card regions keyed only by the Pets element. Equal
syntax trees currently occur in card rows with context-specific variants such as “including this.”
Either derive such wording from explicit host context or canonicalize the redundant variant away. A
granular raw scope must remain valid independently of whichever whole-card row happened to teach it.

## Canonical wording versus rules

Treat the data text as fallible evidence for meaning, not as authority or as a source of incidental
wording. A textual difference is not evidence of a rules distinction by itself. When equivalent
instructions vary only in style, choose one clear form that is easy to derive consistently instead of adding code to reproduce each variation. For example, `Gain 1 steel and 1 titanium` and `Gain
1 plant. Gain 1 energy` do not establish that conjunction and separate sentences have different
semantics.

Verify that apparent variants really do express the same instruction before canonicalizing them.
Preserve the existing order of subclauses within each individual action, effect, or requirement;
wording and punctuation may be standardized without reordering those subclauses. Canonicalization
may happen alongside derivation or in focused data-cleanup passes, whichever keeps the work
clearest and the implementation simplest.

Numeric track thresholds use `Requires that <subject> is <value> or higher/lower`, without `at`.
Minimum count requirements use a bare number above one and an indefinite article for one; `requires
N` already expresses a minimum. Maximum counts use `N or fewer`. Player ownership is implicit in a
player-owned component, while an explicit `<Anyone>` owner renders as uppercase `ANY`. A production
requirement retains `that you have`, because omitting it makes the English unnaturally terse.

Counted component descriptions declare whether a singular change uses `1` or an article. Use `1`
when the reviewed corpus quantifies that component above one in the same instruction construction,
and otherwise use the applicable article. Thus card draws and ocean placement use `1`, while city,
greenery, and colony placement use an article. This is stable component-keyed English knowledge;
production rendering does not inspect the corpus.

A singular minimum requirement always uses an article, because `Requires 1` can misleadingly imply
exactness. Compound tag requirements repeat the complete singular phrase, as in `Requires a Venus
tag, an Earth tag, and a Jovian tag`; do not collapse them to `Requires Venus, Earth, and Jovian
tags`. Non-quantified references such as `play an Earth tag` likewise use an article.

Corporation definitions must author starting money before their other immediate instructions so
the authored-order renderer puts that gain first. Correct the canonical card definition
when this order is wrong; do not teach the renderer to reorder corporations.

## Current derivation scope

Above the artwork, `English` now composes a card's action region from its `Action` list when the card
has no behavior-bearing extra declaration. Bottom-region immediate instructions do not prevent
independent action derivation. It supports no-cost actions, actions that spend a concrete amount of
one standard resource, and actions that decrease one concrete standard-resource production by a
fixed number of steps, provided the result uses the supported instruction shapes below. An action
may instead remove a concrete number of one card resource from this card, `ANY PLAYER'S CARD`, or
any of the player's own cards, as specified by the cost expression. Multiple authored actions render
as alternatives, with a comma before `or` to distinguish their operation scopes. Alternative
costs share one verb only when their verb and modifiers agree; other mixes remain bracketed rather
than risking a change in scope. An action may also link an `X`-scaled standard-resource or
card-resource cost to one `X`-scaled concrete standard-resource gain; the action renderer retains
the shared quantity when it says the same number, that amount, or an explicit multiple. An action
may instead link an abstract standard-resource production cost to a fixed gain of the same abstract
resource type; the renderer retains the shared type as “resources of that kind.” A Describer may
identify a refinement as a named selection among the player's production tracks; this renders the
selected production directly instead of inventing a concrete resource type. The selector may remain
a local custom marker when its otherwise-empty declaration and description account for its complete
printed role. A Describer may
also identify the deferred-payment protocol: a fixed standard-resource amount owed, one accepted
alternate standard resource, and a payment barrier guarding a supported result. This renders as
an ordinary fixed-cost action with the alternate resource noted parenthetically. A Describer may
also supply the object phrase for gaining one chosen concrete member of an abstract component
category. A named operation may be invoked as an
action result and recognized as an effect trigger. Two adjacent effects may use a described barrier
solely to hold that operation open for one optional increase of its selected track; the barrier is
validated and omitted from the printed effect. Supported non-End effects include a fixed M€
discount triggered by
playing a card or one concrete tag, and a supported instruction consequence triggered by playing
concrete tags or one described tag group,
placing a supported tile, including on a described site with a described placement bonus, raising a
supported track, or adding a concrete card resource. Trigger wording preserves whether the acting
player is constrained and retains a separately described
placement location. An unrestricted trigger uses passive
voice and qualifies the event object with `any`, so it does not introduce or imply any triggering
actor; this includes non-player mechanisms such as World Government Terraforming. Other non-End
effects retain bracketed Pets at the narrowest safe effect layer.
Event interpretation retains the event kind and actor constraint independently; the actor
constraint selects active `you` wording or unrestricted passive wording at linearization.
Alternative event clauses with different verbs remain coordinated as complete clauses.
When they have the same expressed subject, emit that subject once and separate their predicates
with comma-or coordination.
Event triggers use the same structured clauses, predicates, noun phrases, and modifiers as
instructions and requirements rather than assembling a separate partial-string representation.
Billing-family triggers are resolved structurally through the `Billing` dependency keys. The
resolved provider and its refinement supply the payment operation, an explicitly selected
represented resource supplies its denomination, and `CardInvoice` additionally retains the
selected played-card Class and refinement. Invoice removal describes completion of payment rather
than a pre-payment adjustment.
The same interpretation drives action and card-play discounts, purchase surcharges, accepted
alternate resources, and card-resource payment values; individual billing subclasses and cards do
not have dedicated matchers. An explicitly selected action on the current card renders as `this
action`, while canonical operation providers supply their usual action phrases through the
lexicon.
A payment-resource acceptance effect structurally linked to the first action on the current card is
rendered as a parenthetical on that action's cost, such as `Spend 8 M€ (steel may be used)`, rather
than as a separate effect sentence.
A played-tag trigger may bind its card argument to a generic card-resource result, retaining that
shared destination as `that card`.
An abstract production-resource trigger linked to a gain of the same resource renders as a per-step
relationship. It retains the selected production type as `of that type` and makes the trigger's
natural count multiplication explicit.
An automatic removal trigger whose described consequence is a dead end renders as a prohibition
when the trigger structurally identifies resources on this card or resources an opponent removes.
A fixed M€ increase in the payment value of a standard resource or resource category is also derived
from its spent-resource trigger. A played-card trigger is derived, including a type narrowed to one
concrete tag or a described minimum or present property. A trigger on using a described standard
action or standard project composes with the same supported consequences. An as-much-as-possible
removal of generated requirement shortfalls after a card play renders as permission to treat that
card's described global-parameter requirement as lower or higher by the fixed amount.
A two-branch optional linked exchange may present card resources removed from this card as a fixed
M€ payment value per resource.
Adjacent acceptance and payment effects may instead say that the card's declared resource type can
be used at one fixed M€ value each. The acceptance trigger supplies the scope and the card definition
supplies the resource noun; a variable payment value remains unsupported.
A pre-payment resource gain for an explicitly described action may be presented as the discount it
implements; adjacent equal discounts combine under one trigger sentence. The action description
may supply a shared counted
noun for alternative standard-resource refunds, allowing those equal amounts to combine as one
discount. Discounts use imperative `pay` without restating the acting player or payment object.
When actions and effects share a top region, actions are rendered first and their
card-resource metrics name `this card` rather than the contextual `here`.
A component may instead describe a purchase object. A fixed mandatory resource gain
or removal triggered by that component then derives the payment adjustment without assuming a
default price absent from the Pets effect.

`English` derives an empty region when the card definition has no element printed there. It derives
minimum and maximum oxygen, temperature, ocean-count, and Venus requirements, plus minimum
concrete-tag requirements, same-category groups of one-count tags, minimum terraform-rating and
owned-greenery requirements, minimum owned or any-player city tiles, compound owned city-and-colony
requirements, and a requirement that the player have a standard-resource production.
It also derives minimum concrete card-resource requirements and minimum and maximum owned-colony
requirements, minimum counts of described distinct component kinds the player owns, plus a minimum
counted spatial relationship between two described placed components.
It derives bottom text when every immediate instruction is one of: a concrete
mandatory gain or removal of a standard resource; an optional removal of up to a concrete number of
standard resources from any player or a player with one concrete tag, or one concrete card resource
from any player; an optional transfer of one
concrete standard-resource type from any player to the acting player; a gain of one reserve Trade
Fleet; a mandatory gain of a generic or concrete card resource on the played card, an unrestricted
card, a card narrowed to one concrete tag, or a card with a concrete minimum card-resource count;
a group of concrete
mandatory standard-resource production gains or decreases; a mandatory one-for-one conversion of
one or more steps of the player's production from one concrete standard resource to another;
a mandatory transfer of a fixed number of steps between two selected instances of one described
track; one city-tile, colony, ocean-tile, greenery-tile, or described special-tile placement using
the type's default arguments; one city, greenery, or special-tile placement on a
described land site, optionally narrowed by a supported minimum or zero-maximum adjacency
refinement; one city placement on a definite described site outside Mars; a concrete
mandatory removal of a concrete card resource; a mandatory exchange of a concrete number of card
resources on this card to draw that number of a component declared as drawable; or a concrete
mandatory temperature, oxygen, Venus-step, or terraform-rating gain or removal. A production
decrease may target any player. A choice is derived when every alternative is one supported clause,
including a choice among concrete production-change clauses. A supported single-clause
instruction may be scaled by the number of a concrete tag or card resource the player owns, or by
complete groups of that tag or resource. It may also be scaled by the player's colonies, by city
tiles or colonies owned by any player, or specifically by any player's city tiles on Mars.
Tag metrics may instead count tags among all players or only tags the player's opponents have.
An instruction metric may count described distinct component kinds the player owns.
It may also count a described component refined by a strict zero maximum of another described
component.
It may count a described component collection with an explicit ownership-sensitive suffix.
An instruction metric may cap any otherwise supported count with a parenthetical literal numeric
maximum. A cap supplied by another metric remains bracketed.
One mandatory gain may instead use a procedure frame's imperative verb and optional object phrase
when its procedure is absent from the Pets change itself. Optional top-card purchase procedures,
production-box copying, and temporary next-card adjustments remain bracketed rather than being
recognized from their current declaration shapes. A `Per` whose metric is a subtraction likewise
uses the same scaling rule; production floors are not a separate instruction idiom.
A two-stage immediate instruction may instead play a card and then remove every generated global-
requirement shortfall or remove up to one fixed owed amount, rendering the requirement waiver or
card discount at the scope of that play.
A two-stage instruction may instead remove a fixed card-resource cost or discard a project card
before one supported consequence, using `to` for that exchange. Other `THEN` sequences retain
their authored order and expose unsupported stages independently.
A two-stage placement may instead link one of several described placement bonuses on the selected
site to a one-step increase of the matching production.
A gained component may instead declare exactly one described first action; its consequence is
rendered through the instruction renderer, while unsupported consequences remain
bracketed. A class-selection gate around that mandate is omitted when the selected component's
imperative already expresses the choice, as in funding an award. A no-op alternative used only to
make that selection executable is likewise omitted from the printed instruction.
A described production-box-copy component obtains the selected card and its concrete tag from its
expression argument; the Describer does not contain that tag or a complete instruction.
Ownership and location remain independent renderer facts. Because a component outside the game
does not exist in Solarnet's model, generated metric phrases do not say `in play`; the published
cards use that phrase to contrast any player's components with the acting player's own components.
A counted spatial relation may also name the event created between its two described placement
participants, retaining contextual ownership of each participant.
One supported clause may be gated by a concrete minimum number of a tag the player owns, a
described state component, the solo player count, or a described component count. A described
state inherited from a generation-scoped component adds `this generation` to its condition.
Supported instructions are rendered in authored order. Adjacent standard-resource gains coalesce,
and adjacent production changes remain coordinated in one sentence. Separate card-resource gains
retain separate clauses because each may choose a different destination card. Alternatives factor
a shared predicate only when its subject, verb, destination, and modifiers agree; otherwise each
alternative retains its whole clause. The same predicate-object compatibility rule governs
instruction aggregation and alternative action costs. Each alternative retains its own scalar. A
concrete fixed VP gain or penalty triggered by `End` is also derived. A per-metric score says
`Worth N VPs per ...`; when the metric itself is unsupported, only that metric remains bracketed.
Supported metrics include a simple tag the player owns, a card resource on the scoring card, a
complete concrete group of one card-resource type on the scoring card, or a described component in
one counted spatial relationship. An unscaled fixed VP gain or penalty may be conditional on the
player having a concrete minimum number of one resource type on the scoring card.

A mandatory removal of concrete project cards renders as discarding cards. A supported effect may
also be triggered specifically by adding a concrete card resource to the effect's own card or by a
directly described operation such as trading.

A tag-narrowed card-resource destination renders as `a <name> card`, independently of
whether the played card itself qualifies. This canonical wording replaces the data file's
semantically redundant `ANY`, `ANOTHER`, and bare-article variants. The generic `CardResource`
class renders as `resource`; each concrete card-resource noun independently declares forms such as
`animal` or `science resource`. Card-resource holders and owners are read by dependency key,
independent of authored argument order. Every card-resource instruction that moves a resource names
a card location: `This` becomes
`this card`, a tag-narrowed holder becomes `a <name> card`, and an unqualified removal
says `from any card`. Aggregate requirements and metrics omit the redundant card location while
retaining ownership; an unqualified card-resource metric says that the player has those resources.
An unrestricted gain says `any card`. The `<name> card` contraction applies when the refinement has
a minimum threshold of one tag; wording whose meaning depends on tag cardinality retains that
cardinality explicitly.

Concrete tag triggers derive their phrase from the concrete tag name before consulting inheritable
category wording. Inherited phrases such as `a planet tag` and `a bio tag` apply only when the
selected tag type is itself abstract; they do not mask `Earth`, `Venus`, or other concrete names.

Explicit-any-player city-tile requirements use uppercase `ANY` and name the required tiles without
`in play`, while unqualified player-owned requirements omit the owner. A compound owned
city-and-colony requirement follows the same ownership rule. Solarnet components outside the game
do not exist, so the published `in play` wording adds no existence state or scope.

Singular placed objects follow the component-keyed change rule above; singular minimum requirements
always use articles. Resource quantities and track or production steps remain numeric even when the
count is one. Attach a step count to every production named; do not move a shared count after
several productions with `each`.

An unsupported requirement or unsupported fixed part of an End-triggered scoring effect renders as
one bracketed Pets element.
Within instruction groups, supported siblings remain English while each unsupported instruction is
bracketed independently. Extra component declarations do not produce text themselves, but remain
available to the renderer as structural evidence for instructions that refer to them.
Actions and non-End effects are top elements and do not prevent bottom derivation.

## Known layout regions

Immediate instructions are printed below the artwork. The goal rows that split Potatoes, Air
Raid, or Stratospheric Birds across regions were data errors, not evidence for dividing one authored
immediate group.

Continue treating cards with behavior-bearing extra component declarations as data-backed. Mons
Insurance shows why: its component declarations encode printed setup behavior that is absent from
`immediate`. Do not infer a generic draw sentence from a plain `ProjectCard` gain. `CARDS`
distinguishes printed filtering, selection, reveal, purchase, and event-recovery procedures through
its inner instruction tree in canonical card data. The renderer interprets that source
transform rather than the follow-mode-neutralized executable declarations.

A plain mandatory placement of one greenery tile does not restate its automatic oxygen increase,
consistent with other systemic placement consequences. A strict placement-site refinement can
express a minimum adjacency count or the absence of adjacent tiles;
its relation target may be implicit or one described placed-component type explicitly qualified by
`Anyone`. A Describer can also identify a specialized placement site such as an area reserved for
ocean. The resulting placement is derived, but any printed waiver of normal placement restrictions
is omitted because Pets does not represent that waiver. A two-branch alternative can prefer one
described site and repeat the same consequence-free, one-component placement behind a `MAX 0` gate
for that site; it renders as placing there when using a board that has such a site and otherwise
placing normally. This board-qualified wording does not imply that occupied sites permit the
normal-placement alternative.
Experimental Forest's filtered draw is rendered from its `CARDS` transform as drawing two plant
cards.

An automatic effect triggered by gaining its own host component is part of that card's immediate
region rather than its persistent effect region. Its supported instruction is rendered before the
card's authored immediate instruction, matching corporation setup order.

An unrestricted gain of a concrete card resource says `any card`. Other narrowed card-resource
targets remain bracketed.

Poseidon's delayed first-action colony placement is authored as `Mandate { -> Colony }`, so a plain
`Colony` gain unambiguously means immediate placement and is derived. One uses `a colony`; multiple
placements use a numeric plural. A placement narrowed to a colony tile remains bracketed because
Research Colony and Space Port Colony print additional permission to reuse an occupied colony tile.

## Review cadence

Treat corpus coverage as evidence, not authorization for new machinery. Do not add structural
renderer APIs, Class-hierarchy analysis, or new grammar representations to improve only one or two
canonical cards. A systemic iteration should simplify the model or cover a meaningfully recurring
family; otherwise retain the bracketed Pets and wait for a smaller general rule. In
particular, do not distribute an `OR` metric into repeated instructions by proving its alternatives
disjoint. Render the metric composition directly when it has a compact general representation.

Before implementation, name the recurring family or the existing machinery the change will delete.
If it has neither, stop and report the unsupported card instead. Judge the trade primarily by new
production concepts and context passed between families; tests, snapshots, and documentation do not
make disproportionate production machinery cheaper.

Make one systemic transformation the unit of review. For each round, report the rule, every distinct
before→after transformation with a count and two examples, and every unexplained row individually.
Keep a round to roughly ten distinct transformations and zero unexplained rows.

When asking Kevin to judge possible wording canonicalizations, use concrete sentence pairs rather
than architectural descriptions. For each candidate, show two actual current outputs, then show both
sentences after applying one proposed consistency rule. Keep each item to one visible transformation
so Kevin can mark a winner, reject it, or refine its scope directly. Carry those decisions into
the next pair or implementation; do not translate them back into abstract questions unless the
implementation reveals a genuine design choice.

Commit bounded renderer iterations autonomously. Stop autonomous rounds after accumulating roughly
25 modified cards, then provide an old-versus-new comparison roundup grouped by the
systemic wording rule that caused them. If one renderer shape would itself change materially more
than 25 cards, report that scope before regenerating current data or committing it. Snapshot changes may be
committed along the way; reconstruct the roundup from the commit-range diff instead of expecting
review of each historical commit.

## Component nouns and change verbs

- A component type can often become its everyday noun by splitting its camel-case name. Number
  agreement is separate; `Plant` specifically becomes `plant` or `plants`.
- Temperature, oxygen, Venus, terraform rating, and colony productions are tracks, not countable
  units. Render their gains and removals with the applicable `increase`/`decrease` or `raise`/`lower`
  pair rather than resource language.
- Spell out `terraform rating` in rendered prose; `TR` remains only an input synonym for Pets.
- Gaining a standard resource uses `gain`; gaining a card resource uses `add`. Standard-resource
  gains use `MC` -> `M€`, singular/plural `Plant` -> `plant`/`plants`, and a lowercased,
  un-camel-cased component Class Name by default.
- The general removal verb is `remove`. An action cost paid from standard resources uses `spend`.
  Do not use `lose`.
