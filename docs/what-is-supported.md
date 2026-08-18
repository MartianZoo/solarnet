# What is supported

Here's what we've got working so far. There may be a few bugs here and there.

Each component is attributed to whatever product introduced it first.

| Product | Corps | Projects | Preludes | Maps | Tile types | Std projects | Milestones / awards | Global params | Global events | Game phases | Other |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| Terraforming Mars | 9 / 11 | 137 / 137 | - | 1 / 1 | 10 / 10 | 7 / 7 | 10 / 10 | 3 / 3 | - | 9 / 9 | - |
| Corporate Era | 2 / 2 | 70 / 71 | - | - | 4 / 4 | - | - | - | - | - | - |
| Hellas & Elysium | - | - | - | 2 / 2 | - | - | 20 / 20 | - | - | - | - |
| Venus Next | 5 / 5 | 49 / 49 | - | - | - | 1 / 1 | 2 / 2 | 1 / 1 | - | 1 / 1 | - |
| Prelude | 5 / 5 | 6 / 7 | 34 / 35 | - | - | - | - | - | - | 1 / 1 | - |
| Colonies | 3 / 5 | 49 / 49 | - | - | - | 1 / 1 | - | - | - | 1 / 1 | 11 / 11 colony tiles |
| Turmoil | 4 / 5 | 0 / 16 | - | - | - | 0 / 1 | 0 / 1 | - | 0 / 31 | 0 / 1 | 0 / 6 parties |
| Prelude 2 | 0 / 5 | 0 / 24 | 0 / 25 | - | - | - | - | - | - | - | - |
| Amazonis & Vastitas | - | - | - | 0 / 2 | - | - | 0 / 20 | 0 / 4 | - | - | - |
| Utopia & Cimmeria | - | - | - | 2 / 2 | - | - | 20 / 20 | - | - | - | - |
| Automa | - | - | - | - | 0 / 1 | - | - | - | - | - | the whole thing |
| Milestones & Awards | - | - | - | - | - | - | 11 / 16 | - | - | - | - |
| Promos through 2026-08 | 8 / 10 | 70 / 73 | 9 / 10 | - | 2 / 3 | - | - | - | 0 / 5 | - | - |

## Supported game variant

Solarnet aims to implement Terraforming Mars faithfully. Its deliberate rule differences are very
minor edge cases, not a substantially different game, and almost every game will be unaffected by
them.

### Ecology Experts resolves its tags before its project

Ecology Experts supplies its plant and microbe tags before its player chooses the project it will
play. Reactions to those tags happen at that time. In particular, Splice Tactical Genomics can
provide money that helps pay for the project.

The selected project does not react retroactively to those tags. If Ecology Experts plays
Decomposers, Decomposers does not gain microbes from Ecology Experts itself. Making the same tags
take effect once before card selection and again after card play would add a second, exceptional
timing model for a few rare interactions.

### Every greenery raises oxygen

Every greenery placement raises oxygen, including greeneries placed during the final greenery
phase. Ordinarily this is unobservable because oxygen must already be maximized for the game to end.
It matters in variants such as TR 63 solo, which can end with oxygen below 14%.

The two neutral greeneries placed during solo setup are the sole exception: each oxygen increase is
immediately canceled. This keeps solo setup at its printed starting oxygen level without teaching
greenery tiles a phase-dependent rule.

### Robotic Workforce re-evaluates Mining Rights and Mining Area

When Robotic Workforce copies Mining Rights or Mining Area, Solarnet re-evaluates the copied
production instruction from the tile's map bonus. It does not remember whether steel or titanium
was selected when the mining card was originally played. On the rare area that provides both kinds
of bonus, Robotic Workforce may therefore choose the other production.

Remembering an otherwise invisible historical choice would add permanent state for this one edge
case. Jacob Fryxelius [declined to settle the interaction](https://boardgamegeek.com/thread/2663453/rule-opinions-mining-rights-robotic-workforce)
when it was presented to him.

### A solo loss has no score

If a solo game reaches its deadline without satisfying its victory condition, Solarnet ends the
game immediately. There are no final greeneries and no scoring; the result is a loss with a score
of zero.

This follows what we believe is the designer's intent: scoring distinguishes successful solo games,
not different degrees of failure.

## Still to implement

| Product | Category | Item | Why |
|---|---|---|---|
| Terraforming Mars | Corporation | Beginner Corporation (`B00`) | Ready |
| Colonies | Corporation | Aridor (`CC1`) | Ready |
| Milestones & Awards | Milestone | Briber | Ready |
| Amazonis & Vastitas | Map | Amazonis Planitia | Ready |
| Amazonis & Vastitas | Map | Vastitas Borealis | Ready |
| Promos through 2026-08 | Project | Self-Replicating Robots (`210`) | Ready |
| Prelude 2 | Cards | All 5 corporations, 24 projects, and 25 preludes | (investigate) |
| Amazonis & Vastitas | Milestones / awards | Both maps' 10 milestones and awards | (investigate) |
| Promos through 2026-08 | Prelude | Established Methods (`X54`) | (investigate) |
| Prelude | Project | Research Coordination (`P40`) | Wild tag |
| Prelude | Prelude | Research Network (`P28`) | Wild tag |
| Terraforming Mars | Corporation | Helion (`B03`) | Payment rewrites |
| Colonies | Corporation | Stormcraft Incorporated (`CC5`) | Payment rewrites |
| Promos through 2026-08 | Corporation | Kuiper Cooperative | Payment rewrites |
| Corporate Era | Project | Land Claim (`066`) | Non-tiles on map |
| Promos through 2026-08 | Corporation | Arcadian Communities | Non-tiles on map |
| Promos through 2026-08 | Project | Mars Nomads (`X59`) | Non-tiles on map |
| Automa | Other | entire Automa rules | Wow that's a lot |
| Amazonis & Vastitas | Global parameter | Extended temperature track | Core replacement |
| Amazonis & Vastitas | Global parameter | Extended oxygen track | Core replacement |
| Amazonis & Vastitas | Global parameter | Extended ocean track | Core replacement |
| Amazonis & Vastitas | Global parameter | Extended Venus track | Core replacement |
| Turmoil | Corporation | Septem Tribus (`TC3`) | Turmoil, Wild tag |
| Turmoil | Projects | all | Turmoil |
| Turmoil | Standard action | Lobby | Turmoil |
| Turmoil | Milestone / award | Terraformer26 | Turmoil |
| Turmoil | Global events | All 31 global events | Turmoil |
| Turmoil | Game phase | Turmoil phase | Turmoil |
| Turmoil | Other | parties | Turmoil |
| Promos through 2026-08 | Project | Political Alliance (`X09`) | Turmoil |
| Promos through 2026-08 | Global events | (five) | Turmoil |
| Milestones & Awards | Milestone | Lobbyist | Turmoil |
| Milestones & Awards | Award | Politician | Turmoil |
| Milestones & Awards | Milestone | Hydrologist | Fork |
| Milestones & Awards | Milestone | Thawer | Fork |
