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

## Current derivation boundary

`English` derives an empty region when the card definition has no element printed there. It also
derives bottom text when that entire region consists of one or more concrete, mandatory gains of
standard resources. A requirement, an End-triggered scoring effect, or any unsupported
immediate-instruction shape keeps the whole region data-backed. Actions and non-End effects are top
elements and do not prevent bottom derivation.

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
