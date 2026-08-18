# English card-text derivation

> **Agent record:** Working rules for the incremental replacement of the English card-text data
> file. These rules describe the target wording model, not behavior already implemented by
> `English`.

Derive text from the Pets instruction structure while retaining the data file as an oracle and
fallback. Move one well-bounded instruction shape at a time, and compare every affected canonical
card with the oracle before expanding the supported shape.

## Component nouns and change verbs

- A component type can often become its ordinary noun by splitting its camel-case name. Number
  agreement is separate; `Plant` specifically becomes `plant` or `plants`.
- Temperature, oxygen, Venus, TR, and colony productions are tracks, not countable units. Render
  their gains and removals with the applicable `increase`/`decrease` or `raise`/`lower` pair rather
  than resource language.
- Gaining a standard resource uses `gain`; gaining a card resource uses `add`.
- The general removal verb is `remove`. An action cost paid from standard resources uses `spend`.
  Do not use `lose`.
