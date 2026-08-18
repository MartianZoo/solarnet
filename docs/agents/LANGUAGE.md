# English card-text derivation

> **Agent record:** Current behavior and working rules for the incremental replacement of the
> English card-text data file.

## Direction and pace

The eventual goal is for `English` to render any instruction expressed in Pets, without depending
on whole-card text data. That is a direction, not a near-term completeness requirement. Progress
slowly, one well-bounded instruction shape at a time, while retaining the data file as an oracle and
fallback. Compare every affected canonical card with the oracle before expanding the supported
shape. An incremental approach that leaves most shapes data-backed is expected and acceptable.

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
derives bottom text when that entire region is a singleton, concrete, mandatory gain of a standard
resource. A requirement, an End-triggered scoring effect, or any unsupported immediate-instruction
shape keeps the whole region data-backed. Actions and non-End effects are top elements and do not
prevent bottom derivation.

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
