# English card-text derivation

> **Agent record:** Current behavior and working rules for the incremental replacement of the
> English card-text data file.

## Direction and pace

The eventual goal is for `English` to render any instruction expressed in Pets. It now makes a
best effort without consulting whole-card text data. When a renderer cannot safely describe a Pets
node, it emits that node's canonical Pets source in square brackets at the narrowest safe structural
boundary. An incremental result containing bracketed regions is expected and honest.

The language module currently targets the JVM only. Its source and resource loading use the
ordinary JVM source sets even though the Pets and Canon dependencies remain multiplatform.

`english-card-text-goals.tsv` is fallible, reviewed target text. `english-card-text-current.tsv` is
generated characterization of what the renderer produces for canonical cards that have a
`CardDefinition`. Neither file is an answer source for production code. Run
`./gradlew :language:writeEnglishCardTextCurrent` after an intentional renderer change, then review
the current-versus-goal diff. Correct a goal row when card data or a systemic rule shows that it is
mistaken.

## Verification while replacing the data file

Do not add a test merely to prove that a newly supported shape removes brackets. Such a test would
restate the implementation boundary, and every incremental step would require another synthetic
test. The all-card comparison against the generated current snapshot is the behavioral
characterization. Review the production diff and the regenerated snapshot together to establish
that the raw boundary moved intentionally.

The explicit empty-region tests establish that structurally absent top or bottom content renders as
empty text. If a renderer gains behavior not exercised by any canonical card, add a behavioral test
for that behavior or defer the generalization.

Every active family entry point now returns total text together with explicit `Unresolved` records
for Pets retained as source. `writeEnglishCardTextCurrent` writes a ranked refusal histogram to
`english-card-text-refusals.tsv` while preserving `english-card-text-current.tsv` as the wording
characterization. Refusal reasons identify the active boundary that could not interpret the node,
such as its change frame, effect trigger, action cost, gate, or scaling form. Nullable private
matchers remain branch attempts inside those interpreters and do not create anonymous report rows.

`english-filtered-draws.tsv` is a narrow transitional supplement for information that canonical Pets
does not yet carry. It maps a card's Class name to a tag, card-resource icon, or printed-requirement
filter. An unqualified `ProjectCard` gain is an ordinary project-card draw; distinct top-card
procedures use their own described component construction.

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
`InstructionTree`, or a `Requirement`. The public instruction and action overloads that accept a
host `CardDefinition` remain, but current canonical wording does not vary by host card. Unsupported
valid Pets shapes return bracketed canonical Pets rather than failing or consulting a card row.

`English` is constructed with a complete `Map<Class, ComponentDescriber>` supplied by its client.
It has no canonical component registry or implicit Terraforming Mars description source; it only
looks up the component Class found in the Pets element being rendered. Map values are sparse
declarations: `Describers` resolves each requested fact independently through that Class's ancestry.
`TerraformingMarsDescribers` owns the canonical declarations and supplies an empty describer for
every undeclared canonical Class, keeping its map complete without copying inherited facts into
each value.

`Describers` resolves production and authored class-literal dependencies through the shared Class
Table. It uses dependency keys returned by the declaring Class to distinguish supplied arguments
from defaults; renderers do not infer owner, resource, or represented-Class roles from argument
positions. Contextual `This` still has one explicitly marked production fallback until rendering
receives the linked source needed to resolve it.
Membership in `Production` and `StandardResource` is read from the Class hierarchy and is not
duplicated in `ComponentDescriber`.

Structural renderers only identify the component Class, ask `Describers` for the applicable
inherited phrase or capability, and compose the answer. They do not name component Classes or
enumerate categories such as city tiles and colonies. In particular, requirement descriptions own
their minimum, maximum, and compound-owned wording rather than exposing a centralized
component-category value for `Describers` to switch over.

Restricted placements follow the same rule. A placement-site fact marks an expression argument as
the placed component's site and supplies its noun and optional definite article, while a
spatial-relation fact supplies the relation phrase and its implicit target noun. The placement
renderer interprets strict counting refinements on that site structurally; neither fact contains a
card or a complete instruction.
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

`directChangeForSubclasses` is likewise a direct opt-in by one exact superclass. It permits the
superclass's direct-change construction to inspect a gained concrete direct subclass. Each such
construction must reject any subclass declaration whose behavior it does not completely account
for; the next-played-card adjustment construction, for example, accepts exactly one automatic
played-card payment reduction or requirement adjustment and obtains its details from that Effect.

Instruction changes and requirements are retained as internal clauses, predicates, noun phrases,
modifiers, and coordinations until their enclosing sentence or action has made its structural
decisions. One linearizer owns capitalization, punctuation, number agreement, and final text
assembly. This is a small renderer representation rather than a general English grammar framework.

Instruction rendering retains its ordered clauses until the enclosing instruction or action chooses
whether to make them separate sentences or coordinate them under one action cost. This small
representation keeps punctuation out of structural decisions without attempting to model general
English grammar.

Instruction, requirement, and effect rendering lower `PROD[...]` through the shared Terraforming
Mars Pets transformer before inspection. Their family renderers interpret the resulting ordinary
`Production` expressions using passive component facts, so production boxes do not need parallel
renderers for every Pets wrapper. `Describers` is limited to lookup and lexical access; it does not
render Pets families.

## Transitional derivation

Intermediate solutions may derive most of an instruction structurally while looking up one narrow
wording fragment. Prefer a granular table for the genuinely irregular fragment over either a
whole-card special case or new gameplay concepts. For example, city-placing cards might share the
normal placement derivation while a small table supplies only how each card describes the allowed
city location. Such tables are acceptable stepping stones toward broader structural derivation;
they need not solve the general case immediately.

Do not automatically populate such a table from card regions keyed only by the Pets element. Equal
syntax trees currently occur in card rows with context-specific variants such as “including this.”
Either derive such wording from explicit host context or canonicalize the redundant variant away. A
granular raw boundary must remain valid independently of whichever whole-card row happened to teach it.

## Canonical wording versus rules

Treat the data text as fallible evidence for meaning, not as authority or as a source of incidental
wording. A textual difference is not evidence of a rules distinction by itself. When equivalent
instructions vary only in style, choose one clear form that is easy to derive consistently rather
than adding code to reproduce each variation. For example, `Gain 1 steel and 1 titanium` and `Gain
1 plant. Gain 1 energy` do not establish that conjunction and separate sentences have different
semantics.

Verify that apparent variants really do express the same instruction before canonicalizing them.
Preserve the existing order of subclauses within each individual action, effect, or requirement;
wording and punctuation may be standardized without reordering those subclauses. Canonicalization
may happen alongside derivation or in focused data-cleanup passes, whichever keeps the work
clearest and the implementation simplest.

For minimum thresholds, prefer `N or more` to `at least N`.

Corporation definitions must author starting money before their other immediate instructions so
the ordinary authored-order renderer puts that gain first. Correct the canonical card definition
when this order is wrong; do not teach the renderer to reorder corporations.

## Current derivation boundary

Above the artwork, `English` now composes a card's action region from its `Action` list when the card
has no behavior-bearing extra declaration. Bottom-region immediate instructions do not prevent
independent action derivation. It supports no-cost actions, actions that spend a concrete amount of
one standard resource, and actions that decrease one concrete standard-resource production by a
fixed number of steps, provided the result uses the supported instruction shapes below. An action
may instead remove a concrete number of one card resource from this card, `ANY PLAYER'S CARD`, or
any of the player's own cards, as specified by the cost expression. Multiple authored actions render
as alternatives, with a comma before `or` to distinguish their operation boundaries. Alternative
costs share one verb only when their verb and modifiers agree; other mixes remain bracketed rather
than risking a change in scope. An action may also link an `X`-scaled standard-resource or
card-resource cost to one `X`-scaled concrete standard-resource gain; the action renderer retains
the shared quantity when it says the same number, that amount, or an explicit multiple. An action
may instead link an abstract standard-resource production cost to a fixed gain of the same abstract
resource type; the renderer retains the shared type as “resources of that kind.” A Describer may
identify a refinement as a named selection among the player's production tracks; this renders the
selected production directly rather than inventing a concrete resource type. The selector may remain
a local custom marker when its otherwise-empty declaration and description account for its complete
printed role. A Describer may
also identify the deferred-payment protocol: a fixed standard-resource amount owed, one accepted
alternate standard resource, and a payment barrier guarding a supported result. This renders as
an ordinary fixed-cost action with the alternate resource noted parenthetically. A Describer may
also supply the object phrase for gaining one chosen concrete member of an abstract component
category. A supported action may also invoke a component described as the optional top-card purchase
procedure.
That description supplies narrow component-level knowledge for behavior absent from the Pets
change; it is not inferred from an ordinary optional gain. A named operation may be invoked as an
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
effects retain bracketed Pets at the narrowest safe effect boundary.
Event interpretation retains the event kind and actor constraint independently; the actor
constraint selects active `you` wording or unrestricted passive wording at linearization.
Alternative event clauses with different verbs remain coordinated as complete clauses.
Event triggers use the same structured clauses, predicates, noun phrases, and modifiers as
instructions and requirements rather than assembling a separate partial-string representation.
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
or removal triggered by that component then derives the payment adjustment without assuming an
ordinary price absent from the Pets effect.

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
track; a city-tile, colony, ocean-tile, or described special-tile placement using the type's default
arguments; one plain greenery-tile placement; one city, greenery, or special-tile placement on a
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
One mandatory gain may instead use an imperative verb and object phrase supplied by that
component's Describer when its procedure is absent from the Pets change itself.
Another supported direct-change construction gains a temporary component whose declaration says
that the next card played receives one fixed standard-resource payment discount or global-
requirement adjustment. Canonical cards gain their own local concrete subclass directly; the shared
`NextCardEffect` supplies the ownership and generational category, while each local class states its
next-play lifecycle and adjustment together.
A two-stage immediate instruction may instead play a card and then remove every generated global-
requirement shortfall or remove up to one fixed owed amount, rendering the requirement waiver or
card discount at the scope of that play.
A two-stage instruction may instead remove a fixed card-resource cost or discard a project card
before one supported consequence, using `to` for that exchange. Other `THEN` sequences retain
their authored order and expose unsupported stages independently.
A two-stage placement may instead link one of several described placement bonuses on the selected
site to a one-step increase of the matching production.
A gained component may instead declare exactly one described first action; its consequence is
rendered through the ordinary instruction renderer, while unsupported consequences remain
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
class renders as `resource`, while concrete card-resource subclasses retain their inherited noun
policy. Every card-resource instruction that moves a resource names a card location: `This` becomes
`this card`, a tag-narrowed holder becomes `a <name> card`, and an unqualified removal
says `from any card`. Aggregate requirements and metrics omit the redundant card location while
retaining ownership; an unqualified card-resource metric says that the player has those resources.
An unrestricted gain says `any card`. The `<name> card` contraction applies when the refinement has
a minimum threshold of one tag; wording whose meaning depends on tag cardinality retains that
cardinality explicitly.

Any-player city-tile requirements use `any` to distinguish them from the player's own tiles and name
the required tiles without `in play`; a compound owned city-and-colony requirement says that you
have those components. Solarnet components outside the game do not exist, so the published `in
play` wording adds no existence state or scope.

Use an indefinite article rather than the numeral `1` for one placed object: `a city tile` and `an
ocean tile`. Counts above one remain numeric. Resource quantities and track or production steps
remain numeric even when the count is one. Attach a step count to every production named; do not
move a shared count after several productions with `each`.

An unsupported requirement or unsupported fixed part of an End-triggered scoring effect renders as
one bracketed Pets element.
Within instruction groups, supported siblings remain English while each unsupported instruction is
bracketed independently. Extra component declarations do not produce text themselves, but remain
available to the renderer as structural evidence for instructions that refer to them.
Actions and non-End effects are top elements and do not prevent bottom derivation.

## Known layout boundaries

Immediate instructions are printed below the artwork. The goal rows that split Potatoes, Air
Raid, or Stratospheric Birds across regions were data errors, not evidence for a layout distinction
in `CardDefinition` or for dividing one authored immediate group.

Filtered project-card draws still require the narrow supplemental table because Pets does not carry
their filter. Tag filters render as adjective card names, while card-resource filters retain the
explicit icon wording. A mandatory transmutation can say that card resources are removed from this
card to draw a declared drawable component; a same-component transmutation instead renders the
directional transfer as stealing from or paying to its other participant. This deliberately does
not cover optional `PlayedEvent` retrieval.

A plain mandatory placement of one greenery tile renders its implicit oxygen increase. A strict
placement-site refinement can express a minimum adjacency count or the absence of adjacent tiles;
its relation target may be implicit or one described placed-component type explicitly qualified by
`Anyone`. A Describer can also identify a specialized placement site such as an area reserved for
ocean. The resulting placement is derived, but any printed waiver of normal placement restrictions
is omitted because Pets does not represent that waiver. A two-branch alternative can prefer one
described site and repeat the same consequence-free, one-component placement behind a `MAX 0` gate
for that site; it renders as placing there when using a board that has such a site and otherwise
placing normally. This board-qualified wording does not imply that occupied sites permit the
normal-placement alternative.
Filtered draws such as Experimental Forest obtain only their missing filter from the transitional
card-Class mapping. The ordinary instruction shape still supplies the count and draw operation.

An automatic effect triggered by gaining its own host component is part of that card's immediate
region rather than its persistent effect region. Its supported instruction is rendered before the
card's authored immediate instruction, matching corporation setup order.

An unrestricted gain of a concrete card resource says `any card`. Other narrowed card-resource
targets remain bracketed.

Poseidon's delayed first-action colony placement is authored as `Mandate { -> Colony }`, so a plain
`Colony` gain unambiguously means immediate placement and is derived. One uses `a colony`; counts above
one use `colonies`. A placement narrowed to a colony tile remains bracketed because Research Colony
and Space Port Colony print additional permission to reuse an occupied colony tile.

## Review cadence

Commit bounded renderer iterations autonomously. Stop autonomous rounds after accumulating roughly
25 modified cards, then provide an old-versus-new comparison roundup grouped by the
systemic wording rule that caused them. If one renderer shape would itself change materially more
than 25 cards, report that scope before regenerating current data or committing it. Snapshot changes may be
committed along the way; reconstruct the roundup from the commit-range diff rather than expecting
review of each historical commit.

## Component nouns and change verbs

- A component type can often become its ordinary noun by splitting its camel-case name. Number
  agreement is separate; `Plant` specifically becomes `plant` or `plants`.
- Temperature, oxygen, Venus, terraform rating, and colony productions are tracks, not countable
  units. Render their gains and removals with the applicable `increase`/`decrease` or `raise`/`lower`
  pair rather than resource language.
- Spell out `terraform rating` in rendered prose; `TR` remains only an input synonym for Pets.
- Gaining a standard resource uses `gain`; gaining a card resource uses `add`. Standard-resource
  gains use `Megacredit` -> `M€`, singular/plural `Plant` -> `plant`/`plants`, and a lowercased,
  un-camel-cased component Class Name by default.
- The general removal verb is `remove`. An action cost paid from standard resources uses `spend`.
  Do not use `lose`.
