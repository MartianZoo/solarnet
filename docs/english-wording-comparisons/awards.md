# Awards: printed and generated wording

[All categories](README.md) · 40 entries

Printed text: [wording evidence](../../src/jvm/dev/martianzoo/tfm/text/english-goal-published-wording-evidence.tsv).
Generated text comes directly from the current English renderer. See the [reading notes](README.md#reading-the-comparisons).

## Tharsis Map

### Landlord

Class: `Landlord`

| | Text |
| --- | --- |
| Printed text | Most tiles. |
| Generated text | Most tiles. |

Pets declaration:

```pets
CLASS Landlord : Award {
  metric = COUNT "OwnedTile"
}
```

### Banker

Class: `Banker`

| | Text |
| --- | --- |
| Printed text | Highest M€ production. |
| Generated text | Highest M€ production. |

Pets declaration:

```pets
CLASS Banker : Award {
  metric = COUNT "PROD[MC]"
}
```

### Scientist

Class: `Scientist`

| | Text |
| --- | --- |
| Printed text | Most science tags. |
| Generated text | Most science tags. |

Pets declaration:

```pets
CLASS Scientist : Award {
  metric = COUNT "ScienceTag"
}
```

### Thermalist

Class: `Thermalist`

| | Text |
| --- | --- |
| Printed text | Most heat resources. |
| Generated text | Most heat. |

Pets declaration:

```pets
CLASS Thermalist : Award {
  metric = COUNT "Heat"
}
```

### Miner

Class: `Miner`

| | Text |
| --- | --- |
| Printed text | Having most steel and titanium resources combined. |
| Generated text | Most titanium or steel. |

Pets declaration:

```pets
CLASS Miner : Award {
  metric = COUNT "Metal"
}
```

## Hellas Map

### Cultivator

Class: `Cultivator`

| | Text |
| --- | --- |
| Printed text | Most greeneries. |
| Generated text | Most greenery tiles. |

Pets declaration:

```pets
CLASS Cultivator : Award {
  metric = COUNT "GreeneryTile"
}
```

### Magnate

Class: `Magnate`

| | Text |
| --- | --- |
| Printed text | Most green cards. |
| Generated text | Most automated cards in play. |

Pets declaration:

```pets
CLASS Magnate : Award {
  metric = COUNT "AutomatedCard"
}
```

### Space Baron

Class: `SpaceBaron`

| | Text |
| --- | --- |
| Printed text | Most space tags. |
| Generated text | Most space tags. |

Pets declaration:

```pets
CLASS SpaceBaron : Award {
  metric = COUNT "SpaceTag"
}
```

### Excentric

Class: `Excentric`

| | Text |
| --- | --- |
| Printed text | Most resources on cards. |
| Generated text | Most card resources. |

Pets declaration:

```pets
CLASS Excentric : Award {
  metric = COUNT "CardResource"
}
```

### Contractor

Class: `Contractor`

| | Text |
| --- | --- |
| Printed text | Most building tags. |
| Generated text | Most building tags. |

Pets declaration:

```pets
CLASS Contractor : Award {
  metric = COUNT "BuildingTag"
}
```

## Elysium Map

### Celebrity

Class: `Celebrity`

| | Text |
| --- | --- |
| Printed text | Most cards that have a cost of 20 M€ or more. |
| Generated text | Most cards in play with a printed cost of 20 M€ or more. |

Pets declaration:

```pets
CLASS Celebrity : Award {
  metric = COUNT "CardFront(HAS 20 cost)"
}
```

### Industrialist

Class: `Industrialist`

| | Text |
| --- | --- |
| Printed text | Most steel and energy resources combined. |
| Generated text | Most steel and energy combined. |

Pets declaration:

```pets
CLASS Industrialist : Award {
  metric = COUNT "Steel OR Energy"
}
```

### Desert Settler

Class: `DesertSettler`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | \[OwnedTile&lt;MarsArea(HAS 6 row)&gt;\]. |

Pets declaration:

```pets
CLASS DesertSettler : Award {
  HAS ElysiumMap
  metric = COUNT "OwnedTile<MarsArea(HAS 6 row)>"
}
```

### Estate Dealer

Class: `EstateDealer`

| | Text |
| --- | --- |
| Printed text | Most tiles next to ocean. |
| Generated text | Most tiles on areas next to at least 1 ocean tile. |

Pets declaration:

```pets
CLASS EstateDealer : Award {
  metric = COUNT "OwnedTile<MarsArea(HAS Neighbor<OceanTile>)>"
}
```

### Benefactor

Class: `Benefactor`

| | Text |
| --- | --- |
| Printed text | Highest TR. If you play with this Award, count it before all other Awards and Milestones! |
| Generated text | Highest terraform rating. |

Pets declaration:

```pets
CLASS Benefactor : Award {
  metric = COUNT "TerraformRating"
}
```

## Venus Next Expansion

### Venuphile

Class: `Venuphile`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Most Venus tags. |

Pets declaration:

```pets
CLASS Venuphile : Award {
  metric = COUNT "VenusTag"
}
```

## Milestones Awards Expansion

### Administrator

Class: `Administrator`

| | Text |
| --- | --- |
| Printed text | Most cards with no tags (don’t forget the corporation and Prelude cards). |
| Generated text | Most cards in play with no tags. |

Pets declaration:

```pets
CLASS Administrator : Award {
  metric = COUNT "CardFront(HAS MAX 0 Tag)"
}
```

### Biologist

Class: `Biologist`

| | Text |
| --- | --- |
| Printed text | Most bio tags combined (plant, microbe, and animal). |
| Generated text | Most bio tags. |

Pets declaration:

```pets
CLASS Biologist : Award {
  metric = COUNT "BioTag"
}
```

### Politician

Class: `Politician`

| | Text |
| --- | --- |
| Printed text | Most Party Leaders and influence combined. |
| Generated text | \[PartyLeader OR Influence\]. |

Pets declaration:

```pets
CLASS Politician : Award {
  metric = COUNT "PartyLeader OR Influence"
  MeasureAward<This>:: EACH Measured@Influence<Anyone> { -Measured@Influence! }
  MeasureAward<This>:: EACH Player { MeasureInfluence }
}
```

### Visionary

Class: `Visionary`

| | Text |
| --- | --- |
| Printed text | Most cards in hand. |
| Generated text | Most cards in hand. |

Pets declaration:

```pets
CLASS Visionary : Award {
  metric = COUNT "ProjectCard"
}
```

## Amazonis Planitia

### Collector

Class: `Collector`

| | Text |
| --- | --- |
| Printed text | Most different types of resources, both on your player board and on your cards. |
| Generated text | Most different types of resources. |

Pets declaration:

```pets
CLASS Collector : Award {
  metric = COUNT "Class<Resource>(HAS Resource<Owner>)"
}
```

### Innovator

Class: `Innovator`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Most cards in play and cards in your event pile combined. |

Pets declaration:

```pets
CLASS Innovator : Award {
  metric = COUNT "CardFront OR PlayedEvent"
}
```

### Constructor

Class: `Constructor`

| | Text |
| --- | --- |
| Printed text | Most colonies and cities combined. |
| Generated text | Most colonies and city tiles combined. |

Pets declaration:

```pets
CLASS Constructor : Award {
  metric = COUNT "Colony OR CityTile"
}
```

### Manufacturer

Class: `Manufacturer`

| | Text |
| --- | --- |
| Printed text | Highest production of steel and heat combined. |
| Generated text | Highest steel and heat production combined. |

Pets declaration:

```pets
CLASS Manufacturer : Award {
  metric = COUNT "PROD[Steel OR Heat]"
}
```

### Physicist

Class: `Physicist`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Most science tags and space tags combined. |

Pets declaration:

```pets
CLASS Physicist : Award {
  metric = COUNT "ScienceTag OR SpaceTag"
}
```

## Vastitas Borealis

### Traveller

Class: `Traveller`

| | Text |
| --- | --- |
| Printed text | Most Jovian and Earth tags combined. |
| Generated text | Most Jovian tags and Earth tags combined. |

Pets declaration:

```pets
CLASS Traveller : Award {
  metric = COUNT "JovianTag OR EarthTag"
}
```

### Landscaper

Class: `Landscaper`

| | Text |
| --- | --- |
| Printed text | Most tiles connected together (each player counts his/her largest group of tiles). |
| Generated text | Most tiles in your largest connected group of tiles. |

Pets declaration:

```pets
CLASS Landscaper : Award {
  metric = COUNT "TileInLargestGroup"
}
```

### Highlander

Class: `Highlander`

| | Text |
| --- | --- |
| Printed text | Most tiles not adjacent to ocean. |
| Generated text | Most tiles on areas next to no ocean tiles. |

Pets declaration:

```pets
CLASS Highlander : Award {
  metric = COUNT "OwnedTile<MarsArea(HAS MAX 0 Neighbor<OceanTile>)>"
}
```

### Promoter

Class: `Promoter`

| | Text |
| --- | --- |
| Printed text | Most cards in your event pile. |
| Generated text | Most cards in your event pile. |

Pets declaration:

```pets
CLASS Promoter : Award {
  metric = COUNT "PlayedEvent"
}
```

### Blacksmith

Class: `Blacksmith`

| | Text |
| --- | --- |
| Printed text | Not transcribed |
| Generated text | Highest titanium or steel production. |

Pets declaration:

```pets
CLASS Blacksmith : Award {
  metric = COUNT "PROD[Metal]"
}
```

## Utopia Map

### Suburbian

Class: `Suburbian`

| | Text |
| --- | --- |
| Printed text | Most tiles on areas along the edges of the map. |
| Generated text | \[OwnedTile(HAS MAX 5 Neighbor)\]. |

Pets declaration:

```pets
CLASS Suburbian : Award {
  metric = COUNT "OwnedTile(HAS MAX 5 Neighbor)"
}
```

### Investor

Class: `Investor`

| | Text |
| --- | --- |
| Printed text | Most Earth tags. |
| Generated text | Most Earth tags. |

Pets declaration:

```pets
CLASS Investor : Award {
  metric = COUNT "EarthTag"
}
```

### Botanist

Class: `Botanist`

| | Text |
| --- | --- |
| Printed text | Highest plant production. |
| Generated text | Highest plant production. |

Pets declaration:

```pets
CLASS Botanist : Award {
  metric = COUNT "PROD[Plant]"
}
```

### Incorporator

Class: `Incorporator`

| | Text |
| --- | --- |
| Printed text | Most cards costing 10 M€ or less. |
| Generated text | Most active cards in play with a printed cost of 10 M€ or less and automated cards in play with a printed cost of 10 M€ or less combined. |

Pets declaration:

```pets
CLASS Incorporator : Award {
  metric = COUNT "ActiveCard(HAS MAX 10 cost) OR AutomatedCard(HAS MAX 10 cost)"
}
```

### Metropolist

Class: `Metropolist`

| | Text |
| --- | --- |
| Printed text | Most cities. |
| Generated text | Most city tiles. |

Pets declaration:

```pets
CLASS Metropolist : Award {
  metric = COUNT "CityTile"
}
```

## Cimmeria Map

### Electrician

Class: `Electrician`

| | Text |
| --- | --- |
| Printed text | Most power tags. |
| Generated text | Most power tags. |

Pets declaration:

```pets
CLASS Electrician : Award {
  metric = COUNT "PowerTag"
}
```

### Founder

Class: `Founder`

| | Text |
| --- | --- |
| Printed text | Most tiles adjacent to special tiles. |
| Generated text | Most tiles on areas next to at least 1 special tile. |

Pets declaration:

```pets
CLASS Founder : Award {
  metric = COUNT "OwnedTile<MarsArea(HAS Neighbor<SpecialTile<Anyone>>)>"
}
```

### Mogul

Class: `Mogul`

| | Text |
| --- | --- |
| Printed text | Highest production of steel, titanium, plants, energy, and heat combined (all except M€ production). |
| Generated text | Highest steel, titanium, plant, energy, and heat production combined. |

Pets declaration:

```pets
CLASS Mogul : Award {
  metric = COUNT "PROD[StandardResource(NOT MC)]"
}
```

### Zoologist

Class: `Zoologist`

| | Text |
| --- | --- |
| Printed text | Most animal and microbe resources combined. |
| Generated text | Most animals and microbes combined. |

Pets declaration:

```pets
CLASS Zoologist : Award {
  metric = COUNT "Animal OR Microbe"
}
```

### Forecaster

Class: `Forecaster`

| | Text |
| --- | --- |
| Printed text | Most cards with requirements. |
| Generated text | Most cards in play with requirements. |

Pets declaration:

```pets
CLASS Forecaster : Award {
  metric = COUNT "CardFront(HAS requirement)"
}
```
