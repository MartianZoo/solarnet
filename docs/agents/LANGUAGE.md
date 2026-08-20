# English card-text derivation

> **Agent record:** Current behavior and working rules for the incremental replacement of the
> English card-text data file.

## Direction and pace

The eventual goal is for `English` to render any instruction expressed in Pets, without depending
on whole-card text data. That is a direction, not a near-term completeness requirement. Progress
slowly, one well-bounded instruction shape at a time, while retaining the data file as a golden
characterization and fallback. The file is fallible, not authoritative: compare every affected
canonical card with it, but correct a row when the card data or a systemic rule shows that the row
is mistaken. An incremental approach that leaves most shapes data-backed is expected and acceptable.

## Verification while replacing the data file

Do not add a test merely to prove that a newly supported shape bypasses the fallback. Such a test
would restate the implementation boundary without protecting card behavior, and every incremental
step would require another synthetic test. The existing all-card comparison is the behavioral
check: a derivation expansion is valid when every affected canonical card renders the reviewed
golden text. A mismatch is a review prompt, not an instruction to preserve the row. Review the
production diff to establish that the fallback boundary actually moved; the code is clearer
evidence of that progress than a change-detector test.

The two explicit fallback tests for absent regions remain useful because emptiness is not represented
by a row's wording: they establish that a structurally empty region succeeds without any card-text
record. New derivation shapes do not need analogous tests while their canonical examples are covered
by the all-card comparison. If a renderer gains behavior not exercised by any canonical card, add a
behavioral test for that behavior or defer the generalization; do not add a test whose only assertion
is that the data file was not consulted.

Keep every structurally supported End-scoring sentence canonical in the card-text data even when
another part of that card keeps the whole region data-backed. The golden row should already contain
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
valid Pets shapes currently fail rather than falling back to a whole-card row.

`English` is constructed with a complete `Map<Class, ComponentDescriber>` supplied by its client.
It has no canonical component registry or implicit Terraforming Mars description source; it only
looks up the component Class found in the Pets element being rendered. Map values are sparse
declarations: `Describers` resolves each requested fact independently through that Class's ancestry.
`TerraformingMarsDescribers` owns the canonical declarations and supplies an empty describer for
every undeclared canonical Class, keeping its map complete without copying inherited facts into
each value.

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

The `textNeutralSubclasses` capability is deliberately direct rather than inherited. It permits an
extra component declaration only when its one exact superclass opts in and the subclass is concrete
and declares no dependency, invariant, effect, default, property, refinement, or other supertype.
This replaces category-name exemptions without allowing a behavior-bearing intermediate class to
inherit permission accidentally.

`directChangeForSubclasses` is likewise a direct opt-in by one exact superclass. It permits the
superclass's direct-change construction to inspect a gained concrete direct subclass. Each such
construction must reject any subclass declaration whose behavior it does not completely account
for; the next-played-card discount construction, for example, accepts exactly one automatic
played-card payment reduction and obtains the amount and resource from that Effect.

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
granular fallback must remain valid independently of whichever whole-card row happened to teach it.

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
costs share one verb only when their verb and modifiers agree; other mixes remain data-backed rather
than risking a change in scope. An action may also link an `X`-scaled standard-resource or
card-resource cost to one `X`-scaled concrete standard-resource gain; the action renderer retains
the shared quantity when it says the same number, that amount, or an explicit multiple. An action
may instead link an abstract standard-resource production cost to a fixed gain of the same abstract
resource type; the renderer retains the shared type as “resources of that kind.” A Describer may
also identify the deferred-payment protocol: a fixed standard-resource amount owed, one accepted
alternate standard resource, and a payment barrier guarding a supported result. This renders as
an ordinary fixed-cost action with the alternate resource noted parenthetically. A Describer may
also supply the object phrase for gaining one chosen concrete member of an abstract component
category. A supported action may also invoke a component described as the optional top-card purchase
procedure.
That description supplies narrow component-level knowledge for behavior absent from the Pets
change; it is not inferred from an ordinary optional gain. Supported non-End effects include a
fixed M€ discount triggered by
playing a card or one concrete tag, and a supported instruction consequence triggered by playing
concrete tags or one described tag group,
placing a supported tile, including on a described site with a described placement bonus, raising a
supported track, or adding a concrete card resource. Trigger
wording preserves whether the acting player is constrained. An unrestricted trigger uses passive
voice and qualifies the event object with `any`, so it does not introduce or imply any triggering
actor; this includes non-player mechanisms such as World Government Terraforming. Other non-End
effects keep the whole top region data-backed.
Event interpretation retains the event kind and actor constraint independently; the actor
constraint selects active `you` wording or unrestricted passive wording at linearization.
Event triggers use the same structured clauses, predicates, noun phrases, and modifiers as
instructions and requirements rather than assembling a separate partial-string representation.
An abstract production-resource trigger linked to a gain of the same resource renders as a per-step
relationship. It retains the selected production type as `of that type` and makes the trigger's
natural count multiplication explicit.
An automatic removal trigger whose described consequence is a dead end renders as a prohibition
when the trigger structurally identifies resources on this card or resources an opponent removes.
A fixed M€ increase in the payment value of a standard resource or resource category is also derived
from its spent-resource trigger. A played-card trigger is derived, including a type narrowed to one
concrete tag or a described minimum or present property. A trigger on using a described standard
action or standard project composes with the same supported consequences. A pre-payment resource gain
for an explicitly described action may be presented as the discount it implements; adjacent equal
discounts combine under one trigger sentence. The action description may supply a shared counted
noun for alternative standard-resource refunds, allowing those equal amounts to combine as one
discount. Discounts triggered by playing a card retain `for it`,
while discounts triggered by playing a tag omit that pronoun because the tag is not the object being
paid for. When actions and effects share a top region, actions are rendered first and their
card-resource metrics name `this card` rather than the contextual `here`.
A component may instead describe a purchase object and destination. A fixed mandatory resource gain
or removal triggered by that component then derives the payment adjustment without assuming an
ordinary price absent from the Pets effect.

`English` derives an empty region when the card definition has no element printed there. It derives
minimum and maximum oxygen, temperature, ocean-count, and Venus requirements, plus minimum
concrete-tag requirements, same-category groups of one-count tags, minimum terraform-rating and
owned-greenery requirements, minimum owned or any-player city tiles, compound owned city-and-colony
requirements, and a requirement that the player have a standard-resource production.
It also derives minimum concrete card-resource requirements and minimum and maximum owned-colony
requirements, plus a minimum counted spatial relationship between two described placed components.
It derives bottom text when every immediate instruction is one of: a concrete
mandatory gain or removal of a standard resource; an optional removal of up to a concrete number of
standard resources or one concrete card resource from any player; an optional transfer of one
concrete standard-resource type from any player to the acting player; a gain of one reserve Trade
Fleet; a mandatory gain of a generic or concrete card resource on the played card, an unrestricted
card, or a card narrowed to one concrete tag; a group of concrete
mandatory standard-resource production gains or decreases; a mandatory one-for-one conversion of
one or more steps of the player's production from one concrete standard resource to another; a
city-tile, colony, ocean-tile, or described special-tile placement using the type's default
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
An instruction metric may cap any otherwise supported count with a parenthetical maximum.
One mandatory concrete gain may instead use an imperative verb and object phrase supplied by that
component's Describer when its procedure is absent from the Pets change itself.
Another supported direct-change construction gains a temporary component whose declaration says
that the next card played receives one fixed standard-resource payment discount.
A gained component may instead declare exactly one described first action; its consequence is
rendered through the ordinary instruction renderer, while unsupported consequences remain
data-backed.
A described production-box-copy component obtains the selected card and its concrete tag from its
expression argument; the Describer does not contain that tag or a complete instruction.
Ownership and location remain independent renderer facts. Because a component outside the game
does not exist in Solarnet's model, generated metric phrases do not say `in play`; the published
cards use that phrase to contrast any player's components with the acting player's own components.
One supported clause may be gated by a concrete minimum number of a tag the player owns.
Supported instructions are rendered in authored order. Adjacent standard-resource gains coalesce,
and adjacent production changes remain coordinated in one sentence. Separate card-resource gains
retain separate clauses because each may choose a different destination card. Alternatives factor
a shared predicate only when its subject, verb, destination, and modifiers agree; otherwise each
alternative retains its whole clause. The same predicate-object compatibility rule governs
instruction aggregation and alternative action costs. Each alternative retains its own scalar. A
concrete fixed VP gain or
penalty triggered by `End` is also derived,
either unscaled or for each simple tag the player owns, card resource on the scoring card, or
complete concrete group of one card-resource type on the scoring card, or each described component
in one counted spatial relationship. An unscaled fixed VP gain or penalty may be conditional on the
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

An unsupported requirement, unsupported End-triggered scoring effect, unaccounted behavior-bearing
extra component declaration, or unsupported immediate-instruction shape keeps the whole region
data-backed. Component declarations can encode printed setup behavior that is absent from
`immediate`, so deriving only that group could omit bottom text. An exact direct-change declaration,
or an exact superclass's explicit and validated subclass construction, accounts for a gained extra
component's printed procedure. A strictly empty direct subclass of
CardResource, SpecialTile, or RemoteArea is declared text-neutral and does not prevent derivation.
Actions and non-End effects are top elements and do not prevent bottom derivation.

## Known layout boundaries

Immediate instructions are printed below the artwork. The golden rows that split Potatoes, Air
Raid, or Stratospheric Birds across regions were data errors, not evidence for a layout distinction
in `CardDefinition` or for dividing one authored immediate group.

Continue treating cards with behavior-bearing extra component declarations as data-backed. Mons
Insurance shows why: its component declarations encode printed setup behavior that is absent from
`immediate`. Likewise,
do not infer a generic draw sentence from a `ProjectCard` gain. The same Pets shape backs both plain
draws and cards whose printed procedure selects from or filters viewed cards, so the current data is
not structurally sufficient. A mandatory transmutation can say that card resources are removed from
this card to draw a declared drawable component; this deliberately does not cover optional
`PlayedEvent` retrieval.

A plain mandatory placement of one greenery tile renders its implicit oxygen increase. A strict
placement-site refinement can express a minimum adjacency count or the absence of adjacent tiles;
its relation target may be implicit or one described placed-component type explicitly qualified by
`Anyone`. A Describer can also identify a specialized placement site such as an area reserved for
ocean. The resulting placement is derived, but any printed waiver of normal placement restrictions
is omitted because Pets does not represent that waiver. A two-branch alternative can prefer one
described site and repeat the same consequence-free, one-component placement behind a `MAX 0` gate
for that site; it renders as placing there when using a board that has such a site and otherwise
placing normally. This board-qualified wording does not imply that occupied sites permit the
fallback.
Experimental Forest remains data-backed
because its accompanying `ProjectCard` gain does not express the printed plant-tag filter.

An unrestricted gain of a concrete card resource says `any card`. Other narrowed card-resource
targets remain data-backed.

Poseidon's delayed first-action colony placement is authored as `Mandate { -> Colony }`, so a plain
`Colony` gain unambiguously means immediate placement and is derived. One uses `a colony`; counts above
one use `colonies`. A placement narrowed to a colony tile remains data-backed because Research Colony
and Space Port Colony print additional permission to reuse an occupied colony tile.

## Review cadence

Commit bounded renderer iterations autonomously. Stop autonomous rounds after accumulating roughly
25 modified cards, then provide an old-versus-new comparison roundup grouped by the
systemic wording rule that caused them. If one renderer shape would itself change materially more
than 25 cards, report that scope before updating the golden data or committing it. The golden file may be
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
