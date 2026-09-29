# What is supported

Most of the published game content is working... and mostly correctly.[^heroku-settings]

[^heroku-settings]: When changing the supported cards or modes documented here, update `herokuapp_settings.json` and `herokuapp_settings_solo.json` too.

| Product | Corps | Projects | Preludes | Maps | Tile types | Std projects | Milestones | Awards | Global params | Global events | Game phases | Other |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| TOTALS | 45 / 48 | 423 / 426 | 66 / 71 | 7 / 7 | 17 / 18 | 10 / 10 | 49 / 50 | 40 / 40 | 8 / 8 | 36 / 36 | 13 / 13 | 17 / 17 named items; no Automa |
| Terraforming Mars | 10 / 11 | 137 / 137 | - | 1 / 1 | 10 / 10 | 7 / 7 | 5 / 5 | 5 / 5 | 3 / 3 | - | 9 / 9 | - |
| Corporate Era | 2 / 2 | 71 / 71 | - | - | 4 / 4 | - | - | - | - | - | - | - |
| Hellas & Elysium | - | - | - | 2 / 2 | - | - | 10 / 10 | 10 / 10 | - | - | - | - |
| Venus Next | 5 / 5 | 49 / 49 | - | - | - | 1 / 1 | 1 / 1 | 1 / 1 | 1 / 1 | - | 1 / 1 | - |
| Prelude | 5 / 5 | 6 / 7 | 34 / 35 | - | - | - | - | - | - | - | 1 / 1 | - |
| Colonies | 5 / 5 | 49 / 49 | - | - | - | 1 / 1 | - | - | - | - | 1 / 1 | 11 / 11 colony tiles |
| Turmoil | 4 / 5 | 15 / 16 | - | - | - | 1 / 1 | 1 / 1 | - | - | 31 / 31 | 1 / 1 | 6 / 6 parties |
| Prelude 2 | 5 / 5 | 24 / 24 | 23 / 25 | - | - | - | - | - | - | - | - | - |
| Amazonis & Vastitas | - | - | - | 2 / 2 | - | - | 10 / 10 | 10 / 10 | 4 / 4 | - | - | - |
| Utopia & Cimmeria | - | - | - | 2 / 2 | - | - | 10 / 10 | 10 / 10 | - | - | - | - |
| Automa | - | - | - | - | 0 / 1 | - | - | - | - | - | - | the whole thing |
| Milestones & Awards | - | - | - | - | - | - | 34 / 35 | 35 / 35 | - | - | - | - |
| Promos through 2026-08 | 9 / 10 | 72 / 73 | 9 / 11 | - | 3 / 3 | - | - | - | - | 5 / 5 | - | - |

Totals count each distinct published goal definition once. Product rows count the contents of that
product, including goals reprinted from another product.

## Still to implement

| Product | Category | Item | Why |
|---|---|---|---|
| Terraforming Mars | Corporation | Helion (`B03`) | Payment rewrites |
| Prelude | Project | Research Coordination | Wild tag |
| Prelude | Prelude | Research Network | Wild tag |
| Turmoil | Corporation | Septem Tribus | Wild tag |
| Turmoil | Project | Banned Delegate | `FakeBannedDelegate` does not update the Party Leader or Dominant party after removing a delegate |
| Prelude 2 | Prelude | Applied Science, Nobel Prize | Wild tags |
| Automa | Other | entire Automa rules | Wow that's a lot |
| Milestones & Awards | Milestone | Thawer | `FakeThawer` retains temperature credits when global events reduce temperature |
| Promos through 2026-08 | Project | Self-Replicating Robots (`210`) | Printed tags while staged |
| Promos through 2026-08 | Prelude | Established Methods (`X54`) | (investigate) |
| Promos through 2026-08 | Prelude | Head Start | Immediate-action sequencing |

The Fakes above require FakeCanon and `FakeStuffBundle`; they are not included in the canonical
support totals or the shared Heroku card settings. `FakeThawer` counts successful player-attributed
temperature increases and can be selected explicitly as a milestone.

## Solarnet's supported variant

Teeechnically what Solarnet implements is a variant rule set. The differences are extremely minor, though.

### Incompatibilities

| Content | Incompatible with | What goes wrong | Severity |
| --- | --- | --- | --- |
| Fake wild-tag assignments | Point Luna | Using a wild tag as Earth for a tag count wrongly draws an extra Point Luna card. | Medium |
| Fake Thawer | Snow Cover | Snow Cover lowers the temperature, but the fake milestone keeps counting the temperature steps that were lost. | Medium |
| Double Down | Board of Directors | Copying Board of Directors fails with an error. It should succeed without giving any directors. | Medium |
| Constructor (award) | Games without Colonies | Game setup rejects the award even when you explicitly choose it. | Low |
| Amazonis Planitia | Mining Rights, Mining Area, Mining Guild | Mining Rights and Mining Area cannot use wild-bonus spaces. Choosing steel or titanium there does not give Mining Guild its extra steel production. | Medium |
| Ecology Experts | Viral Enhancers, Ecological Zone, Decomposers, GMO Contract | The card it plays misses rewards from Ecology Experts' own plant and microbe tags. Decomposers gets 1 microbe instead of 3. GMO Contract is expected to miss 4 M€; that case has not been separately tested. | Medium |
| Preservation Program | Terraforming Deal | The TR increase that Preservation Program prevents still earns 2 M€. | Medium |
| Preservation Program | Reds ruling policy | Reds still charges 3 M€ for the prevented TR increase, and can block a legal gain when you cannot pay. | High |
| Landshaper | Capital | Capital counts as both a city and a special tile, so adding one greenery is enough to claim the milestone. We expect three distinct tiles to be required, but that ruling remains unconfirmed. | Medium; suspected |

To implement Preservation Program, Solarnet gives you the first Action-phase TR increase and then
immediately takes it back. Terraforming Deal and Reds still react to that temporary gain, so these
two pairings must never be used in the same game.

UNMI, Pristar, Valley Trust, Board of Directors, Double Down, Pharmacy Union, and World Government
Advisor remain compatible with Preservation Program. The shared Heroku presets omit it because
they include Terraforming Deal and Turmoil. Custom game selection must enforce these two
exclusions; the engine currently allows the combinations so `BugsTest` can characterize their
incorrect behavior.

### Our interpretations

We don't think these interpretations are wrong, but we don't know for certain.

* We follow the convention that the "X" icon and the phrase "any number" *exclude* zero as a choice, but the phrase "up to" *includes* zero as a valid choice. Exception: a STEAL effect is not allowed to "steal zero". If you are the only player with money, you can't play Air Raid. If your hand is empty you can't play Public Plans.
* If your MiningRights tile is somewhere with both steel and titanium bonuses, and then you RoboticWorkforce it, the game doesn't "remember" your original choice; you get to choose again.
* In a solo game, TharsisRepublic gets +2 M€ production no matter when it is played.
* Without Corporate Era, Producer requires 22 combined production. The printed threshold of 16 does
  not account for the 6 production provided by the beginner setup, and we interpret the milestone as
  requiring the same production progress in either setup.
