# What is supported

Most of the published game content is working... and mostly correctly.[^heroku-settings]

[^heroku-settings]: When changing the supported cards or modes documented here, update `herokuapp_settings.json` and `herokuapp_settings_solo.json` too.

| Product | Corps | Projects | Preludes | Maps | Tile types | Std projects | Milestones | Awards | Global params | Global events | Game phases | Other |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| TOTALS | 43 / 48 | 395 / 426 | 66 / 71 | 7 / 7 | 17 / 18 | 9 / 10 | 45 / 49 | 39 / 40 | 8 / 8 | 0 / 36 | 12 / 13 | 11 / 17 named items; no Automa |
| Terraforming Mars | 9 / 11 | 137 / 137 | - | 1 / 1 | 10 / 10 | 7 / 7 | 5 / 5 | 5 / 5 | 3 / 3 | - | 9 / 9 | - |
| Corporate Era | 2 / 2 | 71 / 71 | - | - | 4 / 4 | - | - | - | - | - | - | - |
| Hellas & Elysium | - | - | - | 2 / 2 | - | - | 10 / 10 | 10 / 10 | - | - | - | - |
| Venus Next | 5 / 5 | 49 / 49 | - | - | - | 1 / 1 | 1 / 1 | 1 / 1 | 1 / 1 | - | 1 / 1 | - |
| Prelude | 5 / 5 | 7 / 7 | 35 / 35 | - | - | - | - | - | - | - | 1 / 1 | - |
| Colonies | 4 / 5 | 49 / 49 | - | - | - | 1 / 1 | - | - | - | - | 1 / 1 | 11 / 11 colony tiles |
| Turmoil | 4 / 5 | 0 / 16 | - | - | - | 0 / 1 | - | 0 / 1 | - | 0 / 31 | 0 / 1 | 0 / 6 parties |
| Prelude 2 | 5 / 5 | 12 / 24 | 21 / 25 | - | - | - | - | - | - | - | - | - |
| Amazonis & Vastitas | - | - | - | 2 / 2 | - | - | 9 / 10 | 10 / 10 | 4 / 4 | - | - | - |
| Utopia & Cimmeria | - | - | - | 2 / 2 | - | - | 10 / 10 | 10 / 10 | - | - | - | - |
| Automa | - | - | - | - | 0 / 1 | - | - | - | - | - | - | the whole thing |
| Milestones & Awards | - | - | - | - | - | - | 31 / 35 | 34 / 35 | - | - | - | - |
| Promos through 2026-08 | 9 / 10 | 70 / 73 | 10 / 11 | - | 3 / 3 | - | - | - | - | 0 / 5 | - | - |

Totals count each distinct published goal definition once. Product rows count the contents of that
product, including goals reprinted from another product.

## Still to implement

| Product | Category | Item | Why |
|---|---|---|---|
| Terraforming Mars | Corporation | Beginner Corporation (`B00`) | Ready |
| Terraforming Mars | Corporation | Helion (`B03`) | Payment rewrites |
| Colonies | Corporation | Aridor (`CC1`) | Ready |
| Turmoil | everything | everything | Turmoil |
| Prelude 2 | Project | L1 Trade Terminal (`P78`) | Distinct |
| Prelude 2 | Turmoil-linked cards | 11 projects and 3 preludes | Turmoil |
| Prelude 2 | Prelude | Preservation Program (`P57`) | Fork |
| Amazonis & Vastitas | Milestone | Lobbyist | Turmoil support |
| Automa | Other | entire Automa rules | Wow that's a lot |
| Milestones & Awards | Milestone | Briber | Milestone-claim effect |
| Milestones & Awards | Milestone | Hydrologist, Thawer | Owned global parameters |
| Milestones & Awards | Milestone | Lobbyist | Turmoil support |
| Milestones & Awards | Award | Politician | Turmoil support |
| Promos through 2026-08 | Corporation | Arcadian Communities | Non-tiles on map |
| Promos through 2026-08 | Project | New Holland | Hybrid tile |
| Promos through 2026-08 | Project | Mars Nomads (`X59`) | Non-tiles on map |
| Promos through 2026-08 | Project | Self-Replicating Robots (`210`) | Fork |
| Promos through 2026-08 | Prelude | Established Methods (`X54`) | (investigate) |
| Promos through 2026-08 | Global events | All 5 | Turmoil |

## Solarnet's supported variant

Teeechnically what Solarnet implements is a variant rule set. The differences are extremely minor, though.

### Contradictions of official rules

* If EcologyExperts plays Decomposers, you get 1 microbe, not 3.

### Our interpretations

We don't think these interpretations are wrong, but we don't know for certain.

* We follow the convention that the "X" icon and the phrase "any number" *exclude* zero as a choice, but the phrase "up to" *includes* zero as a valid choice. Exception: a STEAL effect is not allowed to "steal zero". If you are the only player with money, you can't play Air Raid. If your hand is empty you can't play Public Plans.
* If your MiningRights tile is somewhere with both steel and titanium bonuses, and then you RoboticWorkforce it, the game doesn't "remember" your original choice; you get to choose again.
* In a solo game, TharsisRepublic gets +2 M€ production no matter when it is played.
* Without Corporate Era, Producer requires 22 combined production. The printed threshold of 16 does
  not account for the 6 production provided by the beginner setup, and we interpret the milestone as
  requiring the same production progress in either setup.
