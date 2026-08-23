# What is supported

Most of the published game content is working... and mostly correctly.[^heroku-settings]

[^heroku-settings]: When changing the supported cards or modes documented here, update `herokuapp_settings.json` and `herokuapp_settings_solo.json` too.

| Product | Corps | Projects | Preludes | Maps | Tile types | Std projects | Milestones / awards | Global params | Global events | Game phases | Other |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| TOTALS | 43 / 48 | 395 / 426 | 65 / 70 | 5 / 7 | 17 / 18 | 9 / 10 | 63 / 89 | 4 / 8 | 0 / 36 | 12 / 13 | 11 / 17 named items; no Automa |
| Terraforming Mars | 9 / 11 | 137 / 137 | - | 1 / 1 | 10 / 10 | 7 / 7 | 10 / 10 | 3 / 3 | - | 9 / 9 | - |
| Corporate Era | 2 / 2 | 71 / 71 | - | - | 4 / 4 | - | - | - | - | - | - |
| Hellas & Elysium | - | - | - | 2 / 2 | - | - | 20 / 20 | - | - | - | - |
| Venus Next | 5 / 5 | 49 / 49 | - | - | - | 1 / 1 | 2 / 2 | 1 / 1 | - | 1 / 1 | - |
| Prelude | 5 / 5 | 7 / 7 | 35 / 35 | - | - | - | - | - | - | 1 / 1 | - |
| Colonies | 4 / 5 | 49 / 49 | - | - | - | 1 / 1 | - | - | - | 1 / 1 | 11 / 11 colony tiles |
| Turmoil | 4 / 5 | 0 / 16 | - | - | - | 0 / 1 | 0 / 1 | - | 0 / 31 | 0 / 1 | 0 / 6 parties |
| Prelude 2 | 5 / 5 | 12 / 24 | 21 / 25 | - | - | - | - | - | - | - | - |
| Amazonis & Vastitas | - | - | - | 0 / 2 | - | - | 0 / 20 | 0 / 4 | - | - | - |
| Utopia & Cimmeria | - | - | - | 2 / 2 | - | - | 20 / 20 | - | - | - | - |
| Automa | - | - | - | - | 0 / 1 | - | - | - | - | - | the whole thing |
| Milestones & Awards | - | - | - | - | - | - | 11 / 16 | - | - | - | - |
| Promos through 2026-08 | 9 / 10 | 70 / 73 | 9 / 10 | - | 3 / 3 | - | - | - | 0 / 5 | - | - |

## Still to implement

| Product | Category | Item | Why |
|---|---|---|---|
| Terraforming Mars | Corporation | Beginner Corporation (`B00`) | Ready |
| Colonies | Corporation | Aridor (`CC1`) | Ready |
| Milestones & Awards | Milestone | Briber | Ready |
| Amazonis & Vastitas | Map | Amazonis Planitia | Ready |
| Amazonis & Vastitas | Map | Vastitas Borealis | Ready |
| Amazonis & Vastitas | Milestones / awards | All 20 map-specific goals | Map support |
| Promos through 2026-08 | Prelude | Established Methods (`X54`) | (investigate) |
| Terraforming Mars | Corporation | Helion (`B03`) | Payment rewrites |
| Promos through 2026-08 | Corporation | Arcadian Communities | Non-tiles on map |
| Promos through 2026-08 | Project | Mars Nomads (`X59`) | Non-tiles on map |
| Promos through 2026-08 | Global events | All 5 | Turmoil |
| Prelude 2 | Project | L1 Trade Terminal (`P78`) | Distinct |
| Automa | Other | entire Automa rules | Wow that's a lot |
| Prelude 2 | Turmoil-linked cards | 11 projects and 3 preludes | Turmoil |
| Turmoil | everything | everything | Turmoil |
| Milestones & Awards | Milestone | Lobbyist | Turmoil |
| Milestones & Awards | Award | Politician | Turmoil |
| Amazonis & Vastitas | Global parameter | Extended tracks | Fork |
| Promos through 2026-08 | Project | Self-Replicating Robots (`210`) | Fork |
| Prelude 2 | Prelude | Preservation Program (`P57`) | Fork |
| Milestones & Awards | Milestone | Hydrologist | Fork |
| Milestones & Awards | Milestone | Thawer | Fork |

## Solarnet's supported variant

Teeechnically what Solarnet implements is a variant rule set. The differences are extremely minor, though.

### Contradictions of official rules

* In solo TR63 mode, during final greenery placement, oxygen raises DO still happen.
* If EcologyExperts plays Decomposers, you get 1 microbe, not 3.

### Our interpretations

We don't think these interpretations are wrong, but we don't know for certain.

* We follow the convention that the "X" icon and the phrase "any number" *exclude* zero as a choice, but the phrase "up to" *includes* zero as a valid choice. Exception: a STEAL effect is not allowed to "steal zero". If you are the only player with money, you can't play Air Raid. If your hand is empty you can't play Public Plans.
* If your MiningRights tile is somewhere with both steel and titanium bonuses, and then you RoboticWorkforce it, the game doesn't "remember" your original choice; you get to choose again.
* In a solo game, TharsisRepublic gets +2 M€ production no matter when it is played.
* Recession's losses and M€ production decreases are performed by each opponent, not by the card owner. The printed wording does not make the actor explicit; this interpretation means Mons Insurance does not compensate an opponent for their Recession loss.
