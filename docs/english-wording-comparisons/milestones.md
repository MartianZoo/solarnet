# Milestones: printed and generated wording

[All categories](README.md) · 51 entries

Printed text: [wording evidence](../../src/jvm/dev/martianzoo/tfm/text/english-goal-published-wording-evidence.tsv).
Generated text comes directly from the current English renderer. See the [reading notes](README.md#reading-the-comparisons).

## Tharsis Map

### Terraformer

Class: `Terraformer35`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 35 terraform rating. |

Pets declaration:

```pets
CLASS Terraformer35 : Milestone {
  requirement = HAS "35 TerraformRating"
}
```

### Mayor

Class: `Mayor`

| | Text |
| --- | --- |
| Printed text | 3 cities. |
| Generated text | Requires that you have 3 city tiles. |

Pets declaration:

```pets
CLASS Mayor : Milestone {
  requirement = HAS "3 CityTile"
}
```

### Gardener

Class: `Gardener`

| | Text |
| --- | --- |
| Printed text | 3 greeneries. |
| Generated text | Requires that you have 3 greenery tiles. |

Pets declaration:

```pets
CLASS Gardener : Milestone {
  requirement = HAS "3 GreeneryTile"
}
```

### Builder

Class: `Builder8`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires 8 building tags. |

Pets declaration:

```pets
CLASS Builder8 : Milestone {
  requirement = HAS "8 BuildingTag"
}
```

### Planner

Class: `Planner`

| | Text |
| --- | --- |
| Printed text | 16 cards in hand (when claiming this milestone). |
| Generated text | Requires that you have 16 cards in hand. |

Pets declaration:

```pets
CLASS Planner : Milestone {
  requirement = HAS "16 ProjectCard"
}
```

## Hellas Map

### Diversifier

Class: `Diversifier`

| | Text |
| --- | --- |
| Printed text | 8 different tags. |
| Generated text | Requires 8 different tags. |

Pets declaration:

```pets
CLASS Diversifier : Milestone {
  requirement = HAS "8 Class<Tag>(HAS Tag<Owner>)"
}
```

### Tactician

Class: `Tactician5`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 5 cards in play with requirements. |

Pets declaration:

```pets
CLASS Tactician5 : Milestone {
  requirement = HAS "5 CardFront(HAS requirement)"
}
```

### Polar Explorer

Class: `PolarExplorer`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | \[3 OwnedTile&lt;MarsArea(HAS 8 row)&gt;\]. |

Pets declaration:

```pets
CLASS PolarExplorer : Milestone {
  requirement = HAS "3 OwnedTile<MarsArea(HAS 8 row)>"
}
```

### Energizer

Class: `Energizer`

| | Text |
| --- | --- |
| Printed text | 6 energy production. |
| Generated text | Requires that you have 6 energy production. |

Pets declaration:

```pets
CLASS Energizer : Milestone {
  requirement = HAS "PROD[6 Energy]"
}
```

### Rim Settler

Class: `RimSettler`

| | Text |
| --- | --- |
| Printed text | 3 Jovian tags. |
| Generated text | Requires 3 Jovian tags. |

Pets declaration:

```pets
CLASS RimSettler : Milestone {
  requirement = HAS "3 JovianTag"
}
```

## Elysium Map

### Generalist

Class: `Generalist`

| | Text |
| --- | --- |
| Printed text | 1 production of each resource. (If you play without the Corporate Era, you need 2 production of each resource instead.) |
| Generated text | Requires that you have 1 M€ production, 1 steel production, 1 titanium production, 1 plant production, 1 energy production, and 1 heat production. |

Pets declaration:

```pets
CLASS Generalist : Milestone {
  autoSelectWhen = HAS "MAX 0 QuickStartVariant"
  requirement = HAS "PROD[1 (MC - ProdOffset<Class<MC>>), Steel, Titanium, Plant, Energy, Heat]"
}
```

### Generalist

Class: `Generalist2`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 2 M€ production, 2 steel production, 2 titanium production, 2 plant production, 2 energy production, and 2 heat production. |

Pets declaration:

```pets
CLASS Generalist2 : Milestone {
  autoSelectWhen = HAS "QuickStartVariant"
  requirement = HAS "PROD[2 (MC - ProdOffset<Class<MC>>), 2 Steel, 2 Titanium, 2 Plant, 2 Energy, 2 Heat]"
}
```

### Specialist

Class: `Specialist`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | \[PROD\[10 (MC - ProdOffset&lt;Class&lt;MC&gt;&gt;) OR 10 Steel OR 10 Titanium OR 10 Plant OR 10 Energy OR 10 Heat\]\]. |

Pets declaration:

```pets
CLASS Specialist : Milestone {
  requirement = HAS "PROD[10 (MC - ProdOffset<Class<MC>>) OR 10 Steel OR 10 Titanium OR 10 Plant OR 10 Energy OR 10 Heat]"
}
```

### Ecologist

Class: `Ecologist`

| | Text |
| --- | --- |
| Printed text | 4 bio tags. |
| Generated text | Requires 4 bio tags. |

Pets declaration:

```pets
CLASS Ecologist : Milestone {
  requirement = HAS "4 BioTag"
}
```

### Tycoon

Class: `Tycoon15`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 15 active cards in play and automated cards in play combined. |

Pets declaration:

```pets
CLASS Tycoon15 : Milestone {
  requirement = HAS "15 (ActiveCard OR AutomatedCard)"
}
```

### Legend

Class: `Legend5`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 5 cards in your event pile. |

Pets declaration:

```pets
CLASS Legend5 : Milestone {
  requirement = HAS "5 PlayedEvent"
}
```

## Venus Next Expansion

### Hoverlord

Class: `Hoverlord`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 7 floaters. |

Pets declaration:

```pets
CLASS Hoverlord : Milestone {
  requirement = HAS "7 Floater"
}
```

## Turmoil Expansion

### Terraformer

Class: `Terraformer26`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 26 terraform rating. |

Pets declaration:

```pets
CLASS Terraformer26 : Milestone {
  requirement = HAS "26 TerraformRating"
}
```

## Milestones Awards Expansion

### Briber

Class: `Briber`

| | Text |
| --- | --- |
| Printed text | Pay 12 M€ to receive this milestone, in addition to the normal claim cost of 8 M€ (so, 20 M€ total). |
| Generated text | \[Briber\]. |

Pets declaration:

```pets
CLASS Briber : Milestone {
  requirement = HAS "This"
  This:: -12 MC
}
```

### Builder

Class: `Builder`

| | Text |
| --- | --- |
| Printed text | 7 building tags. |
| Generated text | Requires 7 building tags. |

Pets declaration:

```pets
CLASS Builder : Milestone {
  requirement = HAS "7 BuildingTag"
}
```

### Hydrologist

Class: `Hydrologist`

| | Text |
| --- | --- |
| Printed text | Having placed 4 oceans. (Place owner markers on each ocean until claimed. This owner marker does not make the ocean tile yours, though.) |
| Generated text | \[4 OceanCredit\]. |

Pets declaration:

```pets
CLASS Hydrologist : Milestone {
  HAS Class<HydrologistWatcher>
  requirement = HAS "4 OceanCredit"
}
```

### Legend

Class: `Legend`

| | Text |
| --- | --- |
| Printed text | 4 event cards. |
| Generated text | Requires that you have 4 cards in your event pile. |

Pets declaration:

```pets
CLASS Legend : Milestone {
  requirement = HAS "4 PlayedEvent"
}
```

### Merchant

Class: `Merchant`

| | Text |
| --- | --- |
| Printed text | 2 of each standard resource (after paying the claim cost). |
| Generated text | Requires that you have 2 M€, 2 steel, 2 titanium, 2 plants, 2 energy, and 2 heat. |

Pets declaration:

```pets
CLASS Merchant : Milestone {
  requirement = HAS "2 MC, 2 Steel, 2 Titanium, 2 Plant, 2 Energy, 2 Heat"
}
```

### Philantropist

Class: `Philantropist`

| | Text |
| --- | --- |
| Printed text | 5 cards with non-negative VP. (Cards that count 1 VP per 2 microbes or similar cards also count.) |
| Generated text | \[5 CardFront(HAS GainsOf&lt;Class&lt;VictoryPoint&gt;&gt;)\]. |

Pets declaration:

```pets
CLASS Philantropist : Milestone {
  requirement = HAS "5 CardFront(HAS GainsOf<Class<VictoryPoint>>)"
}
```

### Pioneer

Class: `Pioneer`

| | Text |
| --- | --- |
| Printed text | 4 colonies. |
| Generated text | Requires that you have 4 colonies. |

Pets declaration:

```pets
CLASS Pioneer : Milestone {
  requirement = HAS "4 Colony"
}
```

### Producer

Class: `Producer`

| | Text |
| --- | --- |
| Printed text | Combined total production of at least 16. Not all types need to be represented, and negative M€ production subtracts from the total. |
| Generated text | Requires that you have 16 standard resource production. |

Pets declaration:

```pets
CLASS Producer : Milestone {
  autoSelectWhen = HAS "MAX 0 QuickStartVariant"
  requirement = HAS "16 (PROD[StandardResource] - ProdOffset)"
}
```

### Producer

Class: `Producer22`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 22 standard resource production. |

Pets declaration:

```pets
CLASS Producer22 : Milestone {
  autoSelectWhen = HAS "QuickStartVariant"
  requirement = HAS "22 (PROD[StandardResource] - ProdOffset)"
}
```

### Tactician

Class: `Tactician`

| | Text |
| --- | --- |
| Printed text | 4 cards with requirements. |
| Generated text | Requires that you have 4 cards in play with requirements. |

Pets declaration:

```pets
CLASS Tactician : Milestone {
  requirement = HAS "4 CardFront(HAS requirement)"
}
```

### Terraformer

Class: `Terraformer`

| | Text |
| --- | --- |
| Printed text | 29 TR. |
| Generated text | Requires that you have 29 terraform rating. |

Pets declaration:

```pets
CLASS Terraformer : Milestone {
  requirement = HAS "29 TerraformRating"
}
```

### Tycoon

Class: `Tycoon`

| | Text |
| --- | --- |
| Printed text | 10 blue and green cards combined. |
| Generated text | Requires that you have 10 active cards in play and automated cards in play combined. |

Pets declaration:

```pets
CLASS Tycoon : Milestone {
  requirement = HAS "10 (ActiveCard OR AutomatedCard)"
}
```

## Amazonis Planitia

### Terran

Class: `Terran`

| | Text |
| --- | --- |
| Printed text | 5 Earth tags. |
| Generated text | Requires 5 Earth tags. |

Pets declaration:

```pets
CLASS Terran : Milestone {
  requirement = HAS "5 EarthTag"
}
```

### Landshaper

Class: `Landshaper`

| | Text |
| --- | --- |
| Printed text | 1 city, 1 greenery, and 1 special tile. |
| Generated text | Requires that you have 1 city tile, 1 greenery tile, and 1 special tile. |

Pets declaration:

```pets
CLASS Landshaper : Milestone {
  requirement = HAS "CityTile, GreeneryTile, SpecialTile"
}
```

### Merchant

Class: `Merchant3`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 3 M€, 3 steel, 3 titanium, 3 plants, 3 energy, and 3 heat. |

Pets declaration:

```pets
CLASS Merchant3 : Milestone {
  requirement = HAS "3 MC, 3 Steel, 3 Titanium, 3 Plant, 3 Energy, 3 Heat"
}
```

### Sponsor

Class: `Sponsor`

| | Text |
| --- | --- |
| Printed text | 3 cards with a cost of 20 M€ or more. |
| Generated text | Requires that you have 3 cards in play with a printed cost of 20 M€ or more. |

Pets declaration:

```pets
CLASS Sponsor : Milestone {
  requirement = HAS "3 CardFront(HAS 20 cost)"
}
```

### Lobbyist

Class: `Lobbyist`

| | Text |
| --- | --- |
| Printed text | Having all your 7 delegates in parties (Party Leaders and Chairman also count). |
| Generated text | \[7 Delegate\]. |

Pets declaration:

```pets
CLASS Lobbyist : Milestone {
  requirement = HAS "7 Delegate"
}
```

## Vastitas Borealis

### Agronomist

Class: `Agronomist`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires 4 plant tags. |

Pets declaration:

```pets
CLASS Agronomist : Milestone {
  requirement = HAS "4 PlantTag"
}
```

### Engineer

Class: `Engineer`

| | Text |
| --- | --- |
| Printed text | 10 energy production and heat production combined. |
| Generated text | Requires that you have 10 energy and heat production combined. |

Pets declaration:

```pets
CLASS Engineer : Milestone {
  requirement = HAS "10 PROD[Energy OR Heat]"
}
```

### Spacefarer

Class: `Spacefarer`

| | Text |
| --- | --- |
| Printed text | 4 space tags. |
| Generated text | Requires 4 space tags. |

Pets declaration:

```pets
CLASS Spacefarer : Milestone {
  requirement = HAS "4 SpaceTag"
}
```

### Geologist

Class: `Geologist`

| | Text |
| --- | --- |
| Printed text | 3 tiles on, or adjacent to, volcanic areas (marked with bold text on the maps). If playing with a map without volcanic areas, replace this milestone. |
| Generated text | Requires that you have 3 tiles on volcanic areas and tiles next to at least 1 volcanic area combined. |

Pets declaration:

```pets
CLASS Geologist : Milestone {
  requirement = HAS "3 (OwnedTile<VolcanicArea> OR OwnedTile(HAS Neighbor<VolcanicArea>))"
}
```

### Farmer

Class: `Farmer`

| | Text |
| --- | --- |
| Printed text | 5 animal and microbe resources combined. |
| Generated text | Requires that you have 5 animals and microbes combined. |

Pets declaration:

```pets
CLASS Farmer : Milestone {
  requirement = HAS "5 (Animal OR Microbe)"
}
```

## Utopia Map

### Manager

Class: `Manager`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 3 special tiles. |

Pets declaration:

```pets
CLASS Manager : Milestone {
  requirement = HAS "3 SpecialTile"
}
```

### Pioneer

Class: `Pioneer3`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires that you have 3 colonies. |

Pets declaration:

```pets
CLASS Pioneer3 : Milestone {
  requirement = HAS "3 Colony"
}
```

### Trader

Class: `Trader`

| | Text |
| --- | --- |
| Printed text | 3 different types of resources on cards. |
| Generated text | Requires that you have 3 different types of card resources. |

Pets declaration:

```pets
CLASS Trader : Milestone {
  requirement = HAS "3 Class<CardResource>(HAS CardResource<Owner>)"
}
```

### Metallurgist

Class: `Metallurgist`

| | Text |
| --- | --- |
| Printed text | 6 steel production and titanium production combined. |
| Generated text | Requires that you have 6 titanium or steel production. |

Pets declaration:

```pets
CLASS Metallurgist : Milestone {
  requirement = HAS "6 PROD[Metal]"
}
```

### Researcher

Class: `Researcher`

| | Text |
| --- | --- |
| Printed text | 4 science tags. |
| Generated text | Requires 4 science tags. |

Pets declaration:

```pets
CLASS Researcher : Milestone {
  requirement = HAS "4 ScienceTag"
}
```

## Cimmeria Map

### Planetologist

Class: `Planetologist`

| | Text |
| --- | --- |
| Printed text | 2 Earth tags, 2 Venus tags, and 2 Jovian tags. |
| Generated text | Requires 2 Earth tags, 2 Venus tags, and 2 Jovian tags. |

Pets declaration:

```pets
CLASS Planetologist : Milestone {
  requirement = HAS "2 EarthTag, 2 VenusTag, 2 JovianTag"
}
```

### Architect

Class: `Architect`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Requires 3 city tags. |

Pets declaration:

```pets
CLASS Architect : Milestone {
  requirement = HAS "3 CityTag"
}
```

### Coastguard

Class: `Coastguard`

| | Text |
| --- | --- |
| Printed text | 3 tiles adjacent to ocean. |
| Generated text | Requires that you have 3 tiles on areas next to at least 1 ocean tile. |

Pets declaration:

```pets
CLASS Coastguard : Milestone {
  requirement = HAS "3 OwnedTile<MarsArea(HAS Neighbor<OceanTile>)>"
}
```

### Forester

Class: `Forester`

| | Text |
| --- | --- |
| Printed text | 3 plant production. |
| Generated text | Requires that you have 3 plant production. |

Pets declaration:

```pets
CLASS Forester : Milestone {
  requirement = HAS "PROD[3 Plant]"
}
```

### Fundraiser

Class: `Fundraiser`

| | Text |
| --- | --- |
| Printed text | 12 M€ production. |
| Generated text | Requires that you have 12 M€ production. |

Pets declaration:

```pets
CLASS Fundraiser : Milestone {
  requirement = HAS "PROD[12 (MC - ProdOffset<Class<MC>>)]"
}
```

## Replay-only models

### Thawer

Class: `FakeThawer`

| | Text |
| --- | --- |
| Printed text | Having raised temperature 5 times. (Mark with your player marker on the temperature track each time you raise temperature until claimed.) |
| Generated text | \[5 FakeTemperatureCredit\]. |

Pets declaration:

```pets
"Thawer whose temperature credits survive temperature reductions"
CLASS FakeThawer : Milestone {
  requirement = HAS "5 FakeTemperatureCredit"
}
```
