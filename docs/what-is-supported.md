# What is supported

Almost all the published game content except Automa works.

| Product | Corps | Projects | Preludes | Maps | Tile types | Std projects | Milestones | Awards | Global params | Global events | Game phases | Other |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| TOTALS | 46 / 48 | 424 / 426 | 67 / 71 | 7 / 7 | 17 / 18 | 10 / 10 | 50 / 50 | 40 / 40 | 8 / 8 | 36 / 36 | 13 / 13 | 17 / 17 named items; no Automa |
| Terraforming Mars | 10 / 11 | 137 / 137 | - | 1 / 1 | 10 / 10 | 7 / 7 | 5 / 5 | 5 / 5 | 3 / 3 | - | 9 / 9 | - |
| Corporate Era | 2 / 2 | 71 / 71 | - | - | 4 / 4 | - | - | - | - | - | - | - |
| Hellas & Elysium | - | - | - | 2 / 2 | - | - | 10 / 10 | 10 / 10 | - | - | - | - |
| Venus Next | 5 / 5 | 49 / 49 | - | - | - | 1 / 1 | 1 / 1 | 1 / 1 | 1 / 1 | - | 1 / 1 | - |
| Prelude | 5 / 5 | 6 / 7 | 34 / 35 | - | - | - | - | - | - | - | 1 / 1 | - |
| Colonies | 5 / 5 | 49 / 49 | - | - | - | 1 / 1 | - | - | - | - | 1 / 1 | 11 / 11 colony tiles |
| Turmoil | 4 / 5 | 16 / 16 | - | - | - | 1 / 1 | 1 / 1 | - | - | 31 / 31 | 1 / 1 | 6 / 6 parties |
| Prelude 2 | 5 / 5 | 24 / 24 | 23 / 25 | - | - | - | - | - | - | - | - | - |
| Amazonis & Vastitas | - | - | - | 2 / 2 | - | - | 10 / 10 | 10 / 10 | 4 / 4 | - | - | - |
| Utopia & Cimmeria | - | - | - | 2 / 2 | - | - | 10 / 10 | 10 / 10 | - | - | - | - |
| Milestones & Awards | - | - | - | - | - | - | 35 / 35 | 35 / 35 | - | - | - | - |
| Promos through 2026-08 | 10 / 10 | 72 / 73 | 10 / 11 | - | 3 / 3 | - | - | - | - | 5 / 5 | - | - |
| Automa | - | - | - | - | 0 / 1 | - | - | - | - | - | - | the whole thing |

## Still to implement

| Product | Category | Item | Why |
|---|---|---|---|
| Terraforming Mars | Corporation | Helion (`B03`) | Payment rewrites |
| Prelude | Project | Research Coordination | Wild tag |
| Prelude | Prelude | Research Network | Wild tag |
| Turmoil | Corporation | Septem Tribus | Wild tag |
| Prelude 2 | Prelude | Applied Science, Nobel Prize | Wild tag |
| Promos | Project | Self-Replicating Robots (`210`) | Several problems |
| Promos | Prelude | Head Start | Actions within actions |
| Automa | Other | entire Automa rules | Wow that's a lot |

### Incompatibilities

The first column has precedence in normal selection. Selecting both halves explicitly rejects the
game unless `Unsafe` is selected. `Unsafe` also restores suppressed defaults, except Ecology Experts,
which must always be selected individually. Preservation Program is manual-only in Prelude 2 because
Terraforming Deal belongs to that pack's default pool; it can be selected when neither Terraforming
Deal nor Turmoil is present.

| Preferred content | Excluded content | What goes wrong if both are present |
| --- | --- | --- |
| Viral Enhancers | Ecology Experts | Played through Ecology Experts, Viral Enhancers gives 1 rather than 3 plants when plants are chosen. |
| Ecological Zone | Ecology Experts | Played through Ecology Experts, Ecological Zone gains 2 rather than 3 animals. |
| Decomposers | Ecology Experts | Played through Ecology Experts, Decomposers gains 1 rather than 3 microbes. |
| GMO Contract | Ecology Experts | Played through Ecology Experts, GMO Contract misses 4 M€ from Ecology Experts' plant and microbe tags. |
| Merger | Sagitta Frontier Services | When Merger plays Sagitta, Sagitta misses 4 M€ for Merger's lack of tags. |
| Terraforming Deal | Preservation Program | Terraforming Deal pays 2 M€ for TR that Preservation Program reverses. |
| Turmoil expansion | Preservation Program | Reds Party charges 3 M€ for TR that Preservation Program reverses. |
| Amazonis Planitia | Mining Guild | A wild-resource placement grants steel production and an Audit even when the chosen resource is not metal. |
| Amazonis Planitia | Mining Rights | Cannot place its tile on a wild-resource area. |
| Amazonis Planitia | Mining Area | Cannot place its tile on a wild-resource area. |

The Decomposers, GMO Contract, and Mining Area outcomes follow their authored effects and placement
requirements; the matching interactions have not been separately reproduced in BugsTest.
