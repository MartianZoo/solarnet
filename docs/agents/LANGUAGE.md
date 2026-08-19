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

Produce strings directly while that remains clear. If agreement, conjunction, scope, and
punctuation begin to couple otherwise independent renderers, introduce only the smallest useful
phrase representation rather than a general English grammar framework. A growing collection of
whole-card shape tests in `English` is a signal to extract the appropriate family renderer, not an
acceptable final architecture.

## Transitional derivation

Intermediate solutions may derive most of an instruction structurally while looking up one narrow
wording fragment. Prefer a granular table for the genuinely irregular fragment over either a
whole-card special case or new gameplay concepts. For example, city-placing cards might share the
normal placement derivation while a small table supplies only how each card describes the allowed
city location. Such tables are acceptable stepping stones toward broader structural derivation;
they need not solve the general case immediately.

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

`English` derives an empty region when the card definition has no element printed there. It derives
minimum and maximum oxygen, temperature, ocean-count, and Venus requirements, plus minimum concrete-tag
requirements, same-category groups of one-count tags, minimum TR and owned-greenery requirements, and
a requirement that the player have a standard-resource production. It also derives a minimum colony
requirement. It derives bottom text when every immediate instruction is one of: a concrete mandatory
gain or removal of a standard resource; an optional removal of up to a concrete number of standard
resources from any player; a gain of one reserve Trade Fleet; a concrete mandatory gain of a card
resource on the played card; a group of concrete
mandatory standard-resource production gains or decreases; a city-tile, colony, or ocean-tile
placement; one plain greenery-tile placement; or a concrete mandatory
temperature, oxygen, Venus-step, or TR gain or removal. A production decrease may target any player.
Supported instructions are rendered in authored order, with adjacent standard-resource gains coalesced
into one sentence. A concrete fixed VP gain or penalty triggered by `End` is also derived, as is one
VP for each simple tag the player owns, for each card resource on the scoring card, or for each
complete concrete group of one card-resource type on the scoring card.

Use an indefinite article rather than the numeral `1` for one placed object: `a city tile` and `an
ocean tile`. Counts above one remain numeric. Resource quantities and track or production steps
remain numeric even when the count is one. Attach a step count to every production named; do not
move a shared count after several productions with `each`.

An unsupported requirement, unsupported End-triggered scoring effect, behavior-bearing extra component declaration, or
unsupported immediate-instruction shape keeps the whole region data-backed. Component declarations can
encode printed setup behavior that is absent from `immediate`, so deriving only that group could omit
bottom text. A card's generated declaration of its ordinary card-resource type is not behavior-bearing
and does not prevent derivation. Search for Life remains data-backed because its conditional
science-resource score is absent from Pets. Actions and non-End effects are top elements and do not
prevent bottom derivation.

## Known layout boundaries

Potatoes' plant removal and production increase are both immediate instructions printed below the
artwork. The former split across regions in the data file was a data error, not evidence that the
layout facade must divide one immediate group.

Continue treating cards with behavior-bearing extra component declarations as data-backed. Mons
Insurance shows why: its component declarations encode printed setup behavior that is absent from
`immediate`. Likewise,
do not infer a generic draw sentence from a `ProjectCard` gain. The same Pets shape backs both plain
draws and cards whose printed procedure selects from or filters viewed cards, so the current data is
not structurally sufficient.

A plain mandatory placement of one greenery tile renders its implicit oxygen increase. Restricted
greenery expressions such as `GreeneryTile<WaterArea>` remain data-backed, as does Experimental Forest
because its accompanying `ProjectCard` gain does not express the printed plant-tag filter.

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
- Temperature, oxygen, Venus, TR, and colony productions are tracks, not countable units. Render
  their gains and removals with the applicable `increase`/`decrease` or `raise`/`lower` pair rather
  than resource language.
- Gaining a standard resource uses `gain`; gaining a card resource uses `add`. Standard-resource
  gains use `Megacredit` -> `M€`, singular/plural `Plant` -> `plant`/`plants`, and a lowercased,
  un-camel-cased component Class Name by default.
- The general removal verb is `remove`. An action cost paid from standard resources uses `spend`.
  Do not use `lose`.
