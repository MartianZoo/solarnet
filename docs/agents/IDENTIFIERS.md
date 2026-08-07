# Terraforming Mars identifier audit

This document defines and audits the globally unique identifiers used by Solarnet for cards, corporations, milestones, awards, and other ID-bearing canonical definitions.

## Settled decisions

- Every identifier is globally unique across object kinds and collections. Category context is never required to disambiguate an ID.
- A printed official identifier wins unchanged. Invented prefixes must never replace or decorate a printed ID.
- Conventional IDs use an origin indicator only where necessary to remain globally unique.
- A conventional origin letter identifies the bundle, not a game option within that bundle. `B` means the base family, `H` means Hellas & Elysium, `U` means Utopia & Cimmeria, and `V` means Venus Next. Future Amazonis & Vastitas objects use `A`.
- `B` is used for base-family corporations and base-family milestones/awards because those objects have no printed IDs; it does not specifically mean Tharsis.
- Venus Next conventional objects use `VC#`, `VM#`, and `VA#`. Printed Venus project cards remain in their official numeric sequence.
- Corporation ordinals are assigned by release group and then English alphabetical order. Milestone and award ordinals preserve their published board or rulebook order, including game-option group order.
- Ordinals are unpadded unless a series exceeds nine. Ten-item milestone and award series use the single digits `0` through `9`; shorter series start at `1`. The base-corporation and promo-corporation series use two digits.
- Assigned conventional ordinals are never recycled.

The canonical JSON currently contains 534 ID fields and no duplicate identifier strings.

## Identifier grammar

| Object | Form | Example |
| --- | --- | --- |
| Published card | Printed ID | `001`, `P01`, `C01`, `T01`, `X01` |
| Base-family corporation | `B##` | `B01` |
| Expansion corporation | origin + `C` + ordinal | `PC1`, `VC1`, `XC01` |
| Tharsis milestone/award | `B` + kind + ordinal | `BM1`, `BA1` |
| Other milestone/award | origin + kind + ordinal | `HM0`, `UM0`, `VM1` |
| Follow-mode variant | base ID + `F` | `X14F`, `VC2F` |

The milestone kind letter is `M`; the award kind letter is `A`. The future Amazonis & Vastitas bundle will use `AM0` through `AM9` and `AA0` through `AA9`, following its published game-option and component order.

## Follow-mode suffix

`F` identifies a follow-mode-specific definition whose card-specific behavior relies on an external game record or human handling. It remains part of the Solarnet identifier but is removed when comparing against a printed card ID.

Ordinary draws do not automatically require a second definition. The suffix is reserved for material whose own rule is intentionally incomplete or different until distinct card backs and their locations are modeled.

| Corrected implementation | ID |
| --- | --- |
| Valley Trust | `PC4F` |
| Morning Star Inc. | `VC4F` |
| Splice Tactical Genomics | `XC03F` |
| Asteroid Deflection System | `X14F` |
| Astra Mechanica | `X51F` |
| Public Plans | `X77F` |

There are 21 implemented follow-mode card IDs. Sell Patents remains `SELL`; its generic operation may work in both modes once card backs are atomized.

## Physical authentication

A legible official published physical card is gold. When it prints the identifier both normally and in binary:

1. Transcribe both forms independently.
2. Convert the binary numeral to an integer and compare it with the numeric part of the ordinary ID.
3. Compare integer values rather than leading-zero width; authenticate the alphabetic prefix from the ordinary form.
4. Record edition/language, provenance, both readings, and check date.

A mismatch is an edition, transcription, or reproduction anomaly and is not gold until resolved. A title match or plausible artwork does not authenticate an image.

## Other evidence grades

1. **Physically or manually confirmed:** a legible official card or the owner's direct confirmation settles the printed stem.
2. **Trusted catalog:** [Terraforming Mars: the spreadsheet (2023.04.25)](https://docs.google.com/spreadsheets/d/12FF6VyIKr8HArRR9zjkaIR-PEUql6QnNBbngvI3Fzjo/edit?gid=5502005#gid=5502005) is highly trustworthy and settles a matching name/ID entry unless stronger physical evidence conflicts.
3. **Corroborated:** a community implementation, wiki, catalog, or scan is useful supporting evidence but does not settle an otherwise open stem alone.
4. **Open:** neither direct confirmation nor the trusted catalog settles the stem, or available evidence conflicts.

Neither [terraforming-mars.herokuapp.com and its codebase](https://github.com/terraforming-mars/terraforming-mars/tree/329124965b4d129db637c95772cefed0b7f8b4d3) nor [ssimeonoff](https://github.com/ssimeonoff/ssimeonoff.github.io/tree/e8b3e343530e19052644a12f4129b0d367bedadc) is authoritative. The former contains editorial identifiers, renumbering comments, and at least one typo; the latter is incomplete and has confirmed wrong identifiers for Solar Logistics and Teslaract.

The [Terraforming Mars Fandom promo table](https://terraformingmars.fandom.com/wiki/TM/Promos) is useful corroboration but remains community-editable. Third-party image databases are leads only because an image may be a reconstruction or fan item.

Automa MarsBot card numbers identify Automa components, not the underlying corporation cards. They do not override conventional corporation IDs.

## Audit result

| Category | Records | Resolved stems | Still open |
| --- | ---: | ---: | ---: |
| Project cards | 382 | 369 | 13 |
| Prelude cards | 44 | 42 | 2 |
| Corporations | 38 | convention | convention review |
| Milestones | 26 | convention | component-ID review |
| Awards | 26 | convention | component-ID review |

No unofficial image was promoted to physical authentication.

## Corporation convention

These identifiers are project-assigned rather than printed. Reserved entries preserve the established sequence.

| Release group | Corporation | ID |
| --- | --- | --- |
| Base | Credicor | B01 |
| Base | Ecoline | B02 |
| Base | Helion | B03 (reserved) |
| Base | Interplanetary Cinematics | B04 |
| Base | Inventrix | B05 |
| Base | Mining Guild | B06 |
| Base | Phobolog | B07 |
| Base | Tharsis Republic | B08 |
| Base | Thorgate | B09 |
| Base | United Nations Mars Initiative | B10 |
| Corporate Era | Saturn Systems | B11 |
| Corporate Era | Teractor | B12 |
| Venus Next | Aphrodite | VC1 |
| Venus Next | Celestic | VC2F |
| Venus Next | Manutech | VC3 |
| Venus Next | Morning Star Inc. | VC4F |
| Venus Next | Viron | VC5 |
| Prelude | Cheung Shing Mars | PC1 |
| Prelude | Point Luna | PC2 |
| Prelude | Robinson Industries | PC3 |
| Prelude | Valley Trust | PC4F |
| Prelude | Vitor | PC5 |
| Colonies | Aridor | CC1 |
| Colonies | Arklight | CC2 |
| Colonies | Polyphemos | CC3 |
| Colonies | Poseidon | CC4 |
| Colonies | Stormcraft Incorporated | CC5 |
| Turmoil | Lakefront Resorts | TC1 |
| Turmoil | Pristar | TC2 |
| Turmoil | Septem Tribus | TC3 (reserved) |
| Turmoil | Terralabs Research | TC4 |
| Turmoil | Utopia Invest | TC5 |
| Promo | Arcadian Communities | XC01 (reserved) |
| Promo | Recyclon | XC02 |
| Promo | Splice Tactical Genomics | XC03F |
| Promo | Factorum | XC04F |
| Promo | Mons Insurance | XC05 (reserved) |
| Promo | Philares | XC06 |
| Promo | AstroDrill | XC07 |
| Promo | Pharmacy Union | XC08F |
| Promo | Tycho Magnetics | XC09 |
| Promo | Kuiper Cooperative | XC10 (reserved) |
| Promo | PolderTech | XC11 |

## Milestone and award convention

Each file contains exactly 1, 5, or 10 definitions, counting commented-out definitions. Definitions retain their original board or rulebook order. In a two-map file the first game option occupies `0` through `4` and the second occupies `5` through `9`, in their original within-group order. The code and this table use the same assignments.

| Collection | Kind | Name | ID |
| --- | --- | --- | --- |
| Tharsis | Award | Landlord | BA1 |
| Tharsis | Award | Banker | BA2 |
| Tharsis | Award | Scientist | BA3 |
| Tharsis | Award | Thermalist | BA4 |
| Tharsis | Award | Miner | BA5 |
| Tharsis | Milestone | Terraformer | BM1 |
| Tharsis | Milestone | Mayor | BM2 |
| Tharsis | Milestone | Gardener | BM3 |
| Tharsis | Milestone | Builder | BM4 |
| Tharsis | Milestone | Planner | BM5 |
| Hellas | Award | Cultivator | HA0 |
| Hellas | Award | Magnate | HA1 |
| Hellas | Award | Space Baron | HA2 |
| Hellas | Award | Excentric | HA3 |
| Hellas | Award | Contractor | HA4 |
| Elysium | Award | Celebrity | HA5 |
| Elysium | Award | Industrialist | HA6 |
| Elysium | Award | Desert Settler | HA7 |
| Elysium | Award | Estate Dealer | HA8 |
| Elysium | Award | Benefactor | HA9 |
| Hellas | Milestone | Diversifier | HM0 |
| Hellas | Milestone | Tactician | HM1 |
| Hellas | Milestone | Polar Explorer | HM2 |
| Hellas | Milestone | Energizer | HM3 |
| Hellas | Milestone | Rim Settler | HM4 |
| Elysium | Milestone | Generalist | HM5 |
| Elysium | Milestone | Specialist | HM6 |
| Elysium | Milestone | Ecologist | HM7 |
| Elysium | Milestone | Tycoon | HM8 |
| Elysium | Milestone | Legend | HM9 |
| Utopia Planitia | Award | Suburbian | UA0 |
| Utopia Planitia | Award | Investor | UA1 |
| Utopia Planitia | Award | Botanist | UA2 |
| Utopia Planitia | Award | Incorporator | UA3 |
| Utopia Planitia | Award | Metropolist | UA4 |
| Terra Cimmeria | Award | Electrician | UA5 |
| Terra Cimmeria | Award | Founder | UA6 |
| Terra Cimmeria | Award | Mogul | UA7 |
| Terra Cimmeria | Award | Zoologist | UA8 |
| Terra Cimmeria | Award | Forecaster | UA9 |
| Utopia Planitia | Milestone | Manager | UM0 |
| Utopia Planitia | Milestone | Pioneer | UM1 |
| Utopia Planitia | Milestone | Trader | UM2 |
| Utopia Planitia | Milestone | Metallurgist | UM3 |
| Utopia Planitia | Milestone | Researcher | UM4 |
| Terra Cimmeria | Milestone | Planetologist | UM5 |
| Terra Cimmeria | Milestone | Architect | UM6 |
| Terra Cimmeria | Milestone | Coastguard | UM7 |
| Terra Cimmeria | Milestone | Forester | UM8 |
| Terra Cimmeria | Milestone | Fundraiser | UM9 |
| Venus Next | Award | Venuphile | VA1 |
| Venus Next | Milestone | Hoverlord | VM1 |

## Manually confirmed

- `X63` Solar Logistics and `X66` Teslaract; ssimeonoff is wrong on both.
- `039` Deimos Down, `136` Great Dam, and `165` Magnetic Field Generators.
- `X31` Deimos Down, `X32` Great Dam, `X33` Magnetic Field Generators, and `X44` Sixteen Psyche.
- `044` Natural Preserve, `064` Mining Area, `067` Mining Rights, `085` Commercial District, `097` Nuclear Zone, `123` Industrial Center, `128` Ecological Zone, `140` Lava Flows, `142` Mohole Area, and `199` Restricted Area.
- `P10` Ecology Experts and `P28` Research Network.
- `X14` Asteroid Deflection System, `X17` Crash Site Cleanup, `X53` Cyberia Systems, `X56` Hermetic Order of Mars, and `X62` Red Ships.
- `X49` Anti-Desertification Techniques, `X55` Giant Solar Collector, `X60` Martian Lumber Corp, `X61` Neptunian Power Consultants, and `X64` St. Joseph of Cupertino Mission.

## Later promo release provenance

Release provenance does not authenticate a printed identifier, but it explains how the post-`X64` promos entered circulation. Checked 2026-08-07.

| IDs | Cards | Original release | Pack or later availability |
| --- | --- | --- | --- |
| `X65` | Strategic Base Planning | More Terraforming Mars Kickstarter | [Prelude 2 Promo Pack](https://fryxgames.se/product/tm-prelude-2-promo-pack/) |
| `X67`–`X70` | Soil Enrichment; Supermarkets; Hospitals; Public Baths | The four 2024 seasonal promos | [Seasonal Promo Pack 2024](https://fryxgames.se/product/tm-seasonal-promo-pack-2024/) |
| `X71` | City Parks | Stand-alone 2024 promo | Included as the fifth card in Seasonal Promo Pack 2024 |
| `X72` | Casinos | [WSBG 2024 promo](https://boardgamegeek.com/thread/3349723); community-corroborated | Included as the stand-alone fifth card in [Seasonal Promo Pack 2025](https://fryxgames.se/product/tm-seasonal-promo-pack-2025/) |
| `X73`–`X76` | Protected Growth; Static Harvesting; Vermin; Weather Balloons | The four 2025 seasonal promos | Seasonal Promo Pack 2025 |
| `X77` | Public Plans | [WSBG 2025 promo](https://boardgamegeek.com/thread/3556036/wsbg-promo-card-2025); community-corroborated | No annual retail pack yet |
| `X78` | Albedo Plants | [Spring 2026 seasonal promo](https://fryxgames.se/product/spring-season-promo-2026-albedo-plants/), free with FryxGames shop orders from March 1 through May 31 | No annual pack yet |
| `X79` | Sterling Vents | [Summer 2026 seasonal promo](https://fryxgames.se/product/summer-2026-sterling-vents/), free with FryxGames shop orders from June 1 through August 31 | No annual pack yet |

## Cards still needing validation

These are the only 15 unresolved printed stems. Except for Floyd Continuum, each is absent from the trusted catalog and still needs direct confirmation.

- [ ] `X65` — Strategic Base Planning (Prelude)
- [ ] `X67` — Soil Enrichment
- [ ] `X68` — Supermarkets
- [ ] `X69` — Hospitals
- [ ] `X70` — Public Baths
- [ ] `X71` — City Parks
- [ ] `X72` — Casinos
- [ ] `X73` — Protected Growth
- [ ] `X74` — Static Harvesting
- [ ] `X75` — Vermin
- [ ] `X76` — Weather Balloons
- [ ] `X77` — Public Plans
- [ ] `X78` — Albedo Plants (Prelude)
- [ ] `X79` — Sterling Vents
- [ ] `XM1` — Floyd Continuum; the Dutch Open’s [published card image](https://terraformingmars.nl/floyd-continuum/) prints `007`, which the trusted catalog assigns to Martian Rails, and the catalog has no Floyd Continuum row. Resolve whether `007` is intended as this card’s identifier and whether the card belongs in canonical scope.
