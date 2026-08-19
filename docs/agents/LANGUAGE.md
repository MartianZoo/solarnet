# English card-text derivation

> **Agent record:** Current behavior and working rules for the incremental replacement of the
> English card-text data file.

## Direction and pace

The eventual goal is for `English` to render any instruction expressed in Pets, without depending
on whole-card text data. That is a direction, not a near-term completeness requirement. Progress
slowly, one well-bounded instruction shape at a time, while retaining the data file as an oracle and
fallback. Compare every affected canonical card with the oracle before expanding the supported
shape. An incremental approach that leaves most shapes data-backed is expected and acceptable.

## Verification while replacing the data file

Do not add a test merely to prove that a newly supported shape bypasses the fallback. Such a test
would restate the implementation boundary without protecting card behavior, and every incremental
step would require another synthetic fixture. The existing all-card comparison is the behavioral
check: a derivation expansion is valid when every affected canonical card still renders the oracle
text. Review the production diff to establish that the fallback boundary actually moved; the code
is clearer evidence of that progress than a change-detector test.

The two explicit fallback tests for absent regions remain useful because emptiness is not represented
by a row's wording: they establish that a structurally empty region succeeds without any card-text
record. New derivation shapes do not need analogous tests while their canonical examples are covered
by the all-card comparison. If a renderer gains behavior not exercised by any canonical card, add a
behavioral test for that behavior or defer the generalization; do not add a test whose only assertion
is that the data file was not consulted.

Keep every structurally supported End-scoring sentence canonical in the card-text data even when
another part of that card keeps the whole region data-backed. The oracle should already contain the
complete scoring text before an unrelated instruction shape becomes derivable.

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
`InstructionTree`, or a `Requirement`. An instruction or action can also be described with its host
`CardDefinition`, which supplies context such as whether an unqualified card-resource gain may go
on this card. Unsupported valid Pets shapes currently fail rather than falling back to a whole-card
row.

`English` is constructed with a complete `Map<Class, ComponentDescriber>` supplied by its client.
It has no canonical component registry or implicit Terraforming Mars description source; it only
looks up the describer mapped to a component Class found in the Pets element being rendered.
`TerraformingMarsDescribers` currently owns the canonical sparse declarations and resolves their
inheritance before constructing `English`.

The injected map establishes the dependency direction, but `ComponentDescriber` still exposes
centralized category values and `Describers` still switches over some of them. That is transitional,
not the intended component boundary. Structural renderers should only identify the component Class,
look up its describer, ask that instance for the applicable phrase or capability, and compose the
answer. They must not name component Classes or enumerate categories such as city tiles and
colonies.

Each current sparse fact is inherited independently: a declaration on a more
specific Class overrides the same fact from its superclass, facts from incomparable branches
compose, and differing values for the same fact from incomparable nearest providers are rejected.
Equal values from those providers coalesce. This keeps structural rendering closed over Pets AST
shapes while allowing a newly loaded component Class to reuse the descriptions of its supertypes.

Produce strings directly while that remains clear. If agreement, conjunction, scope, and
punctuation begin to couple otherwise independent renderers, introduce only the smallest useful
phrase representation rather than a general English grammar framework. A growing collection of
whole-card shape tests in `English` is a signal to extract the appropriate family renderer, not an
acceptable final architecture.

Instruction rendering retains its ordered clauses until the enclosing instruction or action chooses
whether to make them separate sentences or coordinate them under one action cost. This small
representation keeps punctuation out of structural decisions without attempting to model general
English grammar.

Instruction and requirement rendering lower `PROD[...]` through the shared Terraforming Mars Pets
transformer before inspection. Only `Describers` interprets the resulting ordinary `Production`
expressions, so production boxes do not need parallel renderers for every Pets wrapper and the
structural renderers do not know the production component's representation.

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

Treat the data text as an oracle for meaning, not for incidental wording. A textual difference is
not evidence of a rules distinction by itself. When equivalent instructions vary only in style,
choose one clear form that is easy to derive consistently rather than adding code to reproduce
each variation. For example, `Gain 1 steel and 1 titanium` and `Gain 1 plant. Gain 1 energy` do not
establish that conjunction and separate sentences have different semantics.

Verify that apparent variants really do express the same instruction before canonicalizing them.
Preserve the existing order of subclauses within each individual action, effect, or requirement;
wording and punctuation may be standardized without reordering those subclauses. Canonicalization
may happen alongside derivation or in focused data-cleanup passes, whichever keeps the work
clearest and the implementation simplest.

Corporation definitions must author starting money before their other immediate instructions so
the ordinary authored-order renderer puts that gain first. Correct the canonical card definition
when this order is wrong; do not teach the renderer to reorder corporations.

## Current derivation boundary

Above the artwork, `English` now composes a card's action region from its `Action` list when the card
has no immediate instruction or behavior-bearing extra declaration. It supports no-cost actions and
actions that spend a concrete amount of one standard resource, provided the result uses the
supported instruction shapes below. Multiple authored actions render as alternatives. Non-End
effects are not yet structurally rendered, so they keep the whole top region data-backed.

`English` derives an empty region when the card definition has no element printed there. It derives
minimum and maximum oxygen, temperature, ocean-count, and Venus requirements, plus minimum
concrete-tag requirements, same-category groups of one-count tags, minimum terraform-rating and
owned-greenery requirements, minimum owned or any-player city tiles, compound owned city-and-colony
requirements, and a requirement that the player have a standard-resource production.
It also derives minimum concrete card-resource requirements and minimum and maximum owned-colony
requirements. It derives bottom text when every
immediate instruction is one of: a concrete mandatory
gain or removal of a standard resource; an optional removal of up to a concrete number of standard
resources from any player; a gain of one reserve Trade Fleet; a mandatory gain of a generic or
concrete card resource on the played card, an unrestricted card, or a card narrowed to one concrete
tag; a group of concrete
mandatory standard-resource production gains or decreases; a city-tile, colony, or ocean-tile
placement using the type's default arguments; one plain greenery-tile placement; or a concrete
mandatory
temperature, oxygen, Venus-step, or terraform-rating gain or removal. A production decrease may
target any player. A choice is derived when every alternative is one supported clause, including a
choice among concrete production-change clauses. A supported single-clause instruction may be
scaled by the number of a concrete tag or card resource the player owns, or by complete groups of
that tag or resource. It may also be scaled by the player's colonies, by city tiles or colonies
owned by any player, or specifically by any player's city tiles on Mars. Ownership and location
remain independent renderer facts. Because a component outside the game does not exist in
Solarnet's model, generated metric phrases do not say `in play`; the published cards use that phrase
to contrast any player's components with the acting player's own components. One supported clause
may be gated by a concrete minimum number of a tag the player owns.
Supported instructions are rendered in authored order, with adjacent standard-resource gains
coalesced into one sentence. A concrete fixed VP gain or penalty triggered by `End` is also derived,
either unscaled or for each simple tag the player owns, card resource on the scoring card, or
complete concrete group of one card-resource type on the scoring card. An unscaled fixed VP gain or
penalty may be conditional on the player having a concrete minimum number of one resource type on
the scoring card.

A tag-narrowed card-resource destination renders as `a card with a <name> tag`, independently of
whether the played card itself qualifies. This canonical wording replaces the data file's
semantically redundant `ANY`, `ANOTHER`, and bare-article variants. The generic `CardResource`
class renders as `resource`, while concrete card-resource subclasses retain their inherited noun
policy.

The requirement renderer still emits the published `in play` wording for any-player city-tile
requirements and for one compound owned-placement form. This is transitional wording, not a
Solarnet existence state or scope; replace it in the next requirement-focused round.

Use an indefinite article rather than the numeral `1` for one placed object: `a city tile` and `an
ocean tile`. Counts above one remain numeric. Resource quantities and track or production steps
remain numeric even when the count is one. Attach a step count to every production named; do not
move a shared count after several productions with `each`.

An unsupported requirement, unsupported End-triggered scoring effect, behavior-bearing extra component declaration, or
unsupported immediate-instruction shape keeps the whole region data-backed. Component declarations can
encode printed setup behavior that is absent from `immediate`, so deriving only that group could omit
bottom text. A card's generated declaration of its ordinary card-resource type is not behavior-bearing
and does not prevent derivation. Actions and non-End effects are top elements and do not prevent
bottom derivation.

## Known layout boundaries

Potatoes' plant removal and production increase are both immediate instructions printed below the
artwork. The former split across regions in the data file was a data error, not evidence that the
layout facade must divide one immediate group.

Stratospheric Birds has the opposite whole-group placement: its immediate floater removal is
printed above the artwork beside its action, while its requirement and End scoring are below. The
current `CardDefinition` has no layout fact that distinguishes this from an immediate group printed
below. Do not infer the region from the instruction's resource type or from the presence of an
action; represent the layout distinction explicitly before deriving this card's regions.

Continue treating cards with behavior-bearing extra component declarations as data-backed. Mons
Insurance shows why: its component declarations encode printed setup behavior that is absent from
`immediate`. Likewise,
do not infer a generic draw sentence from a `ProjectCard` gain. The same Pets shape backs both plain
draws and cards whose printed procedure selects from or filters viewed cards, so the current data is
not structurally sufficient.

A plain mandatory placement of one greenery tile renders its implicit oxygen increase. Restricted
greenery expressions such as `GreeneryTile<WaterArea>` remain data-backed, as does Experimental Forest
because its accompanying `ProjectCard` gain does not express the printed plant-tag filter.

An unrestricted gain of a concrete card resource says `ANY card` when the played card can hold that
resource and `ANOTHER card` when it cannot. A narrowed card-resource target remains data-backed.

Poseidon's delayed first-action colony placement is authored as `Mandate { -> Colony }`, so a plain
`Colony` gain unambiguously means immediate placement and is derived. One uses `a colony`; counts above
one use `colonies`. A placement narrowed to a colony tile remains data-backed because Research Colony
and Space Port Colony print additional permission to reuse an occupied colony tile.

## Review cadence

Commit bounded renderer iterations autonomously. Stop autonomous rounds after accumulating roughly
ten golden-text row changes, then provide an old-versus-new comparison roundup grouped by the
systemic wording rule that caused them. If one renderer shape would itself change materially more
than ten rows, report that scope before updating the oracle or committing it. The golden file may be
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
