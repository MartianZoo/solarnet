# Canonical definition identifiers

**Status: current convention and evidence audit.** Production data is the inventory. This document
does not duplicate corporation, milestone, or award tables.

## Invariants

- Identifiers are globally unique across all canonical definition kinds and collections. Category
  context must never be needed to disambiguate an id.
- A printed official identifier wins unchanged.
- Invented prefixes supply origin only when needed for global uniqueness.
- Origin denotes a bundle family, not a game option within it: `B` base, `H` Hellas & Elysium,
  `U` Utopia & Cimmeria, `V` Venus Next, `M` Milestones & Awards, and `A` for future Amazonis
  & Vastitas material.
- `B` identifies the base family, not specifically Tharsis.
- Conventional ordinals are never recycled.

Canon loading does not yet enforce global uniqueness across every definition kind. Until that TODO
is complete, audit the whole data set whenever adding or changing an identifier.

## Grammar

| Definition | Form | Example |
| --- | --- | --- |
| Published card | printed id | `001`, `P01`, `C01`, `T01`, `X01` |
| Base-family corporation | `B##` | `B01` |
| Expansion corporation | origin + `C` + ordinal | `PC1`, `VC1`, `XC01` |
| Tharsis milestone/award | `B` + kind + ordinal | `BM1`, `BA1` |
| Other milestone/award | origin + kind + ordinal | `HM0`, `UM0`, `VM1` |
| Milestones & Awards goal | `M` + kind + two digits | `MM02`, `MA01` |
| Follow-mode variant | base id + `F` | `X14F`, `VC2F` |

`M` is the milestone kind and `A` the award kind. Venus conventional definitions use `VC#`,
`VM#`, and `VA#`; printed Venus projects keep their printed numeric sequence.

**Reserved future convention:** if Amazonis & Vastitas enters canonical scope, its ten milestones
use `AM0` through `AM9` and its ten awards use `AA0` through `AA9`, following published
option/component order. This reserves names; it does not claim that the content is implemented.

Corporation ordinals are assigned within release group in English alphabetical order. Milestone and
award ordinals preserve printed board/rulebook order, including option-group order. Use unpadded
ordinals unless a series exceeds nine. Ten-item map series use `0` through `9`; longer series use
two digits. Short series start at `1`.

## Follow-mode `F`

The suffix marks a definition whose client supplies a reveal, selection, discard, or draw outcome.
It is a distinct Solarnet identifier but is removed when comparing with a printed card id. A future
real-card mode uses the non-`F` identity when Solarnet authenticates physical cards and owns hidden
locations and chance.

Do not suffix ordinary draws automatically. Use `F` only when the state transition materially
depends on trusting client-supplied card identity. Standard actions such as `SellPatents` use
semantic Class Names rather than card identifiers.

## Evidence

A legible official physical component is the strongest evidence for printed identity. When a card
shows both text and binary forms:

1. transcribe each independently;
2. compare the binary integer with the numeric text, ignoring leading-zero width;
3. authenticate any alphabetic prefix from the text; and
4. record edition/language, provenance, readings, and date.

A mismatch remains unresolved until the edition or reproduction anomaly is explained.

Other evidence, strongest first:

1. owner/manual confirmation of an official component;
2. [Terraforming Mars: the spreadsheet (2023-04-25)][catalog];
3. corroborated catalogs, scans, wikis, or community implementations; and
4. unresolved or conflicting evidence.

The herokuapp implementation and ssimeonoff are useful leads, not authorities. Known wrong values
include Solar Logistics and Teslaract in ssimeonoff. Automa MarsBot numbers identify Automa
components, not the underlying corporations.

[catalog]: https://docs.google.com/spreadsheets/d/12FF6VyIKr8HArRR9zjkaIR-PEUql6QnNBbngvI3Fzjo/edit?gid=5502005#gid=5502005

The following stems have direct owner/manual confirmation and should not be reopened from weaker
sources:

- `039`, `044`, `064`, `067`, `085`, `097`, `123`, `128`, `136`, `140`, `142`,
  `165`, and `199`;
- `P10` and `P28`; and
- `X14`, `X17`, `X31`–`X33`, `X44`, `X49`, `X53`, `X55`, `X56`, `X60`–`X64`,
  and `X66`.

## Current open printed stems

These were still unresolved in the 2026-08 audit because the trusted catalog did not settle them:
`X65`, `X67`–`X79`, and `XM1` (Floyd Continuum).

Floyd Continuum needs a deliberate scope and collision decision: its published Dutch Open image
prints `007`, already assigned to Martian Rails, while the catalog has no Floyd Continuum entry.
Production currently uses provisional `XM1`. Keep the item in `TODO.md` until resolved.

For later promos, release provenance can establish existence and grouping but not printed identity.
Do not promote a shop page or community post to physical authentication.
