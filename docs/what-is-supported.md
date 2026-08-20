# What is supported

Most of the published game content is working... and mostly correctly.

| Product | Corps | Projects | Preludes | Maps | Tile types | Std projects | Milestones / awards | Global params | Global events | Game phases | Other |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Terraforming Mars | 9 / 11 | 137 / 137 | - | 1 / 1 | 10 / 10 | 7 / 7 | 10 / 10 | 3 / 3 | - | 9 / 9 | - |
| Corporate Era | 2 / 2 | 70 / 71 | - | - | 4 / 4 | - | - | - | - | - | - |
| Hellas & Elysium | - | - | - | 2 / 2 | - | - | 20 / 20 | - | - | - | - |
| Venus Next | 5 / 5 | 49 / 49 | - | - | - | 1 / 1 | 2 / 2 | 1 / 1 | - | 1 / 1 | - |
| Prelude | 5 / 5 | 6 / 7 | 34 / 35 | - | - | - | - | - | - | 1 / 1 | - |
| Colonies | 3 / 5 | 49 / 49 | - | - | - | 1 / 1 | - | - | - | 1 / 1 | 11 / 11 colony tiles |
| Turmoil | 4 / 5 | 0 / 16 | - | - | - | 0 / 1 | 0 / 1 | - | 0 / 31 | 0 / 1 | 0 / 6 parties |
| Prelude 2 | 2 / 5 | 9 / 24 | 17 / 25 | - | - | - | - | - | - | - | - |
| Amazonis & Vastitas | - | - | - | 0 / 2 | - | - | 0 / 20 | 0 / 4 | - | - | - |
| Utopia & Cimmeria | - | - | - | 2 / 2 | - | - | 20 / 20 | - | - | - | - |
| Automa | - | - | - | - | 0 / 1 | - | - | - | - | - | the whole thing |
| Milestones & Awards | - | - | - | - | - | - | 11 / 16 | - | - | - | - |
| Promos through 2026-08 | 8 / 10 | 70 / 73 | 9 / 10 | - | 3 / 3 | - | - | - | 0 / 5 | - | - |

## Still to implement

| Product | Category | Item | Why |
|---|---|---|---|
| Terraforming Mars | Corporation | Beginner Corporation (`B00`) | Ready |
| Colonies | Corporation | Aridor (`CC1`) | Ready |
| Milestones & Awards | Milestone | Briber | Ready |
| Amazonis & Vastitas | Map | Amazonis Planitia | Ready |
| Amazonis & Vastitas | Map | Vastitas Borealis | Ready |
| Promos through 2026-08 | Project | Self-Replicating Robots (`210`) | Ready |
| Amazonis & Vastitas | Milestones / awards | (maybe we do have them?) | (investigate) |
| Promos through 2026-08 | Prelude | Established Methods (`X54`) | (investigate) |
| Prelude | Project | Research Coordination (`P40`) | (Works on wildtag branch) |
| Prelude | Prelude | Research Network (`P28`) | (Works on wildtag branch) |
| Terraforming Mars | Corporation | Helion (`B03`) | Payment rewrites |
| Colonies | Corporation | Stormcraft Incorporated (`CC5`) | Payment rewrites |
| Promos through 2026-08 | Corporation | Kuiper Cooperative | Payment rewrites |
| Corporate Era | Project | Land Claim (`066`) | Non-tiles on map |
| Promos through 2026-08 | Corporation | Arcadian Communities | Non-tiles on map |
| Promos through 2026-08 | Project | Mars Nomads (`X59`) | Non-tiles on map |
| Amazonis & Vastitas | Global parameter | Extended tracks | Core replacement |
| Prelude 2 | Non-Turmoil cards | 3 corporations, 4 projects, and 5 preludes | Unmodeled unusual mechanics |
| Prelude 2 | Turmoil-linked cards | 11 projects and 3 preludes | Turmoil |
| Turmoil | everything | all of it | Turmoil |
| Automa | Other | entire Automa rules | Wow that's a lot |
| Milestones & Awards | Milestone | Lobbyist | Turmoil |
| Milestones & Awards | Award | Politician | Turmoil |
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
* TharsisRepublic always gets +2 M€ production in any solo game, even if played much later in the game.
