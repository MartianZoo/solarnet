# Projects: printed and generated wording

[All categories](README.md) · 426 entries

Printed text: [wording evidence](../../src/jvm/dev/martianzoo/tfm/text/english-published-wording-evidence.tsv).
Generated text comes directly from the current English renderer. See the [reading notes](README.md#reading-the-comparisons).

## Terraforming Mars

### Adaptation Technology

Class: `AdaptationTechnology`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: Your global requirements are +2 or -2 steps, your choice in each case. |
| Generated text | — | Effect: When you play a card, you may treat a global parameter requirement as if it is 2 steps lower or higher. |

Pets declaration:

```pets
CLASS AdaptationTechnology : ActiveCard {
  cost = 12
  This:: ScienceTag<This>
  CheckRequirement:: -2 Required<Class<GlobalParameter>>
  End: VictoryPoint
}
```

### Adapted Lichen

Class: `AdaptedLichen`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your plant production 1 step. | — |
| Generated text | Increase your plant production 1 step. | — |

Pets declaration:

```pets
CLASS AdaptedLichen : AutomatedCard {
  cost = 9
  This:: PlantTag<This>
  This: PROD[Plant]
}
```

### Advanced Ecosystems

Class: `AdvancedEcosystems`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires a plant tag, a microbe tag, and an animal tag. | — |
| Generated text | Requires a plant tag, a microbe tag, and an animal tag. | — |

Pets declaration:

```pets
CLASS AdvancedEcosystems : AutomatedCard {
  cost = 11
  requirement = HAS "PlantTag, MicrobeTag, AnimalTag"
  This:: PlantTag<This>, MicrobeTag<This>, AnimalTag<This>
  End: 3 VictoryPoint
}
```

### Aerobraked Ammonia Asteroid

Class: `AerobrakedAmmoniaAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 2 microbes to ANOTHER card. Increase your heat production 3 steps and your plant production 1 step. | — |
| Generated text | Add 2 microbes to another card. Increase your heat production 3 steps and your plant production 1 step. | — |

Pets declaration:

```pets
CLASS AerobrakedAmmoniaAsteroid : EventCard {
  cost = 26
  This:: SpaceTag<This>, EventTag<This>
  This: 2 Microbe, PROD[3 Heat, Plant]
}
```

### Algae

Class: `Algae`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 5 ocean tiles. Gain 1 plant and increase your plant production 2 steps. | — |
| Generated text | Requires 5 ocean tiles. Gain 1 plant. Increase your plant production 2 steps. | — |

Pets declaration:

```pets
CLASS Algae : AutomatedCard {
  cost = 10
  requirement = HAS "5 OceanTile"
  This:: PlantTag<This>
  This: Plant, PROD[2 Plant]
}
```

### Ants

Class: `Ants`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4% oxygen. 1 VP per 2 microbes on this card. | Action: Remove 1 microbe from any card to add 1 to this card. |
| Generated text | Requires 4% oxygen. 1 VP per 2 microbes on this card. | Action: Remove 1 microbe from any player's card to add 1 microbe to this card. |

Pets declaration:

```pets
CLASS Ants : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 9
  requirement = HAS "4 OxygenStep"
  This:: MicrobeTag<This>
  End: VictoryPoint / 2 Microbe<This>
  Microbe<Anyone> -> Microbe<This>
}
```

### Aquifer Pumping

Class: `AquiferPumping`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 8 M€ to place 1 ocean tile. STEEL MAY BE USED as if you were playing a building card. |
| Generated text | — | Action: Spend 8 M€ (steel may be used) to place an ocean tile. |

Pets declaration:

```pets
CLASS AquiferPumping : ActionCard, ActiveCard {
  cost = 18
  This:: BuildingTag<This>
  UseAction<This>:: Accepting<Class<Steel>>
  8 MC -> OceanTile<>
}
```

### Archaebacteria

Class: `Archaebacteria`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | It must be -18°C or colder. Increase your plant production 1 step. | — |
| Generated text | Requires -18°C or colder. Increase your plant production 1 step. | — |

Pets declaration:

```pets
CLASS Archaebacteria : AutomatedCard {
  cost = 6
  requirement = HAS "MAX 6 TemperatureStep"
  This:: MicrobeTag<This>
  This: PROD[Plant]
}
```

### Arctic Algae

Class: `ArcticAlgae`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | It must be -12°C or colder to play. Gain 1 plant. | Effect: When anyone places an ocean tile, gain 2 plants. |
| Generated text | Requires -12°C or colder. Gain 1 plant. | Effect: When any ocean tile is placed, gain 2 plants. |

Pets declaration:

```pets
CLASS ArcticAlgae : ActiveCard {
  cost = 12
  requirement = HAS "MAX 9 TemperatureStep"
  This:: PlantTag<This>
  This: Plant
  OceanTile BY Anyone: 2 Plant
}
```

### Artificial Lake

Class: `ArtificialLake`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -6°C or warmer. Place 1 ocean tile ON AN AREA NOT RESERVED FOR OCEAN. | — |
| Generated text | Requires -6°C or warmer. Place an ocean tile on a land area, or if there are 9 ocean tiles, do nothing. | — |

Pets declaration:

```pets
CLASS ArtificialLake : AutomatedCard {
  cost = 15
  requirement = HAS "12 TemperatureStep"
  This:: BuildingTag<This>
  This: OceanTile<LandArea>! OR (9 OceanTile: Ok)
  End: VictoryPoint
}
```

### Artificial Photosynthesis

Class: `ArtificialPhotosynthesis`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your plant production 1 step or your energy production 2 steps. | — |
| Generated text | Increase your plant production 1 step or your energy production 2 steps. | — |

Pets declaration:

```pets
CLASS ArtificialPhotosynthesis : AutomatedCard {
  cost = 12
  This:: ScienceTag<This>
  This: PROD[Plant OR 2 Energy]
}
```

### Asteroid

Class: `AsteroidCard`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 1 step and gain 2 titanium. Remove up to 3 plants from any player. | — |
| Generated text | Raise temperature 1 step. Gain 2 titanium. You may remove up to 3 plants from any player. | — |

Pets declaration:

```pets
CLASS AsteroidCard : EventCard {
  cost = 14
  This:: SpaceTag<This>, EventTag<This>
  This: TemperatureStep, 2 Titanium, -3 Plant<Anyone>?
}
```

### Asteroid Mining

Class: `AsteroidMining`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 2 steps. | — |
| Generated text | Increase your titanium production 2 steps. | — |

Pets declaration:

```pets
CLASS AsteroidMining : AutomatedCard {
  cost = 30
  This:: JovianTag<This>, SpaceTag<This>
  This: PROD[2 Titanium]
  End: 2 VictoryPoint
}
```

### Beam From A Thorium Asteroid

Class: `BeamFromAThoriumAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires a Jovian tag. Increase your heat production and energy production 3 steps each. | — |
| Generated text | Requires a Jovian tag. Increase your heat production and your energy production 3 steps each. | — |

Pets declaration:

```pets
CLASS BeamFromAThoriumAsteroid : AutomatedCard {
  cost = 32
  requirement = HAS "JovianTag"
  This:: JovianTag<This>, SpaceTag<This>, PowerTag<This>
  This: PROD[3 Heat, 3 Energy]
  End: VictoryPoint
}
```

### Big Asteroid

Class: `BigAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 2 steps and gain 4 titanium. Remove up to 4 plants from any player. | — |
| Generated text | Raise temperature 2 steps. Gain 4 titanium. You may remove up to 4 plants from any player. | — |

Pets declaration:

```pets
CLASS BigAsteroid : EventCard {
  cost = 27
  This:: SpaceTag<This>, EventTag<This>
  This: 2 TemperatureStep, 4 Titanium, -4 Plant<Anyone>?
}
```

### Biomass Combustors

Class: `BiomassCombustors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 6% oxygen. Decrease any plant production 1 step and increase your energy production 2 steps. | — |
| Generated text | Requires 6% oxygen. Decrease any player's plant production 1 step and increase your energy production 2 steps. | — |

Pets declaration:

```pets
CLASS BiomassCombustors : AutomatedCard {
  cost = 4
  requirement = HAS "6 OxygenStep"
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[-Plant<Anyone>, 2 Energy]
  End: -VictoryPoint
}
```

### Birds

Class: `Birds`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 13% oxygen. Decrease any plant production 2 steps. 1 VP for each animal on this card. | Action: Add an animal to this card. |
| Generated text | Requires 13% oxygen. Decrease any player's plant production 2 steps. 1 VP per animal on this card. | Action: Add 1 animal to this card. |

Pets declaration:

```pets
CLASS Birds : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 10
  requirement = HAS "13 OxygenStep"
  This:: AnimalTag<This>
  This: PROD[-2 Plant<Anyone>]
  End: VictoryPoint / Animal<This>
  -> Animal<This>
}
```

### Black Polar Dust

Class: `BlackPolarDust`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place an ocean tile. Decrease your M€ production 2 steps and increase your heat production 3 steps. | — |
| Generated text | Place an ocean tile. Decrease your M€ production 2 steps and increase your heat production 3 steps. | — |

Pets declaration:

```pets
CLASS BlackPolarDust : AutomatedCard {
  cost = 15
  This: OceanTile<>, PROD[-2 MC, 3 Heat]
}
```

### Breathing Filters

Class: `BreathingFilters`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 7% oxygen. | — |
| Generated text | Requires 7% oxygen. | — |

Pets declaration:

```pets
CLASS BreathingFilters : AutomatedCard {
  cost = 11
  requirement = HAS "7 OxygenStep"
  This:: ScienceTag<This>
  End: 2 VictoryPoint
}
```

### Bushes

Class: `Bushes`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -10°C or warmer. Increase your plant production 2 steps. Gain 2 plants. | — |
| Generated text | Requires -10°C or warmer. Increase your plant production 2 steps. Gain 2 plants. | — |

Pets declaration:

```pets
CLASS Bushes : AutomatedCard {
  cost = 10
  requirement = HAS "10 TemperatureStep"
  This:: PlantTag<This>
  This: PROD[2 Plant], 2 Plant
}
```

### Capital

Class: `Capital`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4 ocean tiles. Place this tile. Decrease your energy production 2 steps and increase your M€ production 5 steps. 1 ADDITIONAL VP FOR EACH OCEAN TILE ADJACENT TO THIS CITY TILE. | — |
| Generated text | Requires 4 ocean tiles. Place this city tile. Decrease your energy production 2 steps and increase your M€ production 5 steps. 1 VP per ocean tile next to this city tile. | — |

Pets declaration:

```pets
CLASS Capital : AutomatedCard {
  HAS MAX 1 CapitalTile<This>
  cost = 26
  requirement = HAS "4 OceanTile"
  This:: CityTag<This>, BuildingTag<This>
  This: CapitalTile<This>, PROD[-2 Energy, 5 MC]
  End: VictoryPoint / Adjacency<CapitalTile<This>, OceanTile>
}
```

### Carbonate Processing

Class: `CarbonateProcessing`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and increase your heat production 3 steps. | — |
| Generated text | Decrease your energy production 1 step and increase your heat production 3 steps. | — |

Pets declaration:

```pets
CLASS CarbonateProcessing : AutomatedCard {
  cost = 6
  This:: BuildingTag<This>
  This: PROD[-Energy, 3 Heat]
}
```

### Cloud Seeding

Class: `CloudSeeding`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 ocean tiles. Decrease your M€ production 1 step and any heat production 1 step. Increase your plant production 2 steps. | — |
| Generated text | Requires 3 ocean tiles. Decrease your M€ production 1 step and any player's heat production 1 step and increase your plant production 2 steps. | — |

Pets declaration:

```pets
CLASS CloudSeeding : AutomatedCard {
  cost = 11
  requirement = HAS "3 OceanTile"
  This: PROD[-MC, -Heat<Anyone>, 2 Plant]
}
```

### Colonizer Training Camp

Class: `ColonizerTrainingCamp`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Oxygen must be 5% or less. | — |
| Generated text | Requires 5% oxygen or less. | — |

Pets declaration:

```pets
CLASS ColonizerTrainingCamp : AutomatedCard {
  cost = 8
  requirement = HAS "MAX 5 OxygenStep"
  This:: JovianTag<This>, BuildingTag<This>
  End: 2 VictoryPoint
}
```

### Comet

Class: `Comet`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 1 step and place an ocean tile. Remove up to 3 plants from any player. | — |
| Generated text | Raise temperature 1 step. Place an ocean tile. You may remove up to 3 plants from any player. | — |

Pets declaration:

```pets
CLASS Comet : EventCard {
  cost = 21
  This:: SpaceTag<This>, EventTag<This>
  This: TemperatureStep, OceanTile<>, -3 Plant<Anyone>?
}
```

### Convoy From Europa

Class: `ConvoyFromEuropa`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place 1 ocean tile and draw 1 card. | — |
| Generated text | Place an ocean tile. Draw 1 card. | — |

Pets declaration:

```pets
CLASS ConvoyFromEuropa : EventCard {
  cost = 15
  This:: SpaceTag<This>, EventTag<This>
  This: OceanTile<>, ProjectCard
}
```

### Cupola City

Class: `CupolaCity`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Oxygen must be 9% or less. Place a city tile. Decrease your energy production 1 step and increase your M€ production 3 steps. | — |
| Generated text | Requires 9% oxygen or less. Place a city tile. Decrease your energy production 1 step and increase your M€ production 3 steps. | — |

Pets declaration:

```pets
CLASS CupolaCity : AutomatedCard {
  cost = 16
  requirement = HAS "MAX 9 OxygenStep"
  This:: CityTag<This>, BuildingTag<This>
  This: CityTile<>, PROD[-Energy, 3 MC]
}
```

### Decomposers

Class: `Decomposers`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3% oxygen. 1 VP per 3 microbes on this card. | Effect: When you play an animal, plant, or microbe tag, including this, add a microbe to this card. |
| Generated text | Requires 3% oxygen. 1 VP per 3 microbes on this card. | Effect: When you play a bio tag (including this), add 1 microbe to this card. |

Pets declaration:

```pets
CLASS Decomposers : ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 5
  requirement = HAS "3 OxygenStep"
  This:: MicrobeTag<This>
  BioTag: Microbe<This>
  End: VictoryPoint / 3 Microbe<This>
}
```

### Deep Well Heating

Class: `DeepWellHeating`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 1 step. Increase temperature 1 step. | — |
| Generated text | Increase your energy production 1 step. Raise temperature 1 step. | — |

Pets declaration:

```pets
CLASS DeepWellHeating : AutomatedCard {
  cost = 13
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[Energy], TemperatureStep
}
```

### Deimos Down

Class: `DeimosDown`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 3 steps and gain 4 steel. Remove up to 8 plants from any player. | — |
| Generated text | Raise temperature 3 steps. Gain 4 steel. You may remove up to 8 plants from any player. | — |

Pets declaration:

```pets
CLASS DeimosDown : EventCard {
  cost = 31
  autoSelectWhen = HAS "MAX 0 PromoCardPack"
  This:: SpaceTag<This>, EventTag<This>
  This: 3 TemperatureStep, 4 Steel, -8 Plant<Anyone>?
}
```

### Designed Microorganisms

Class: `DesignedMicroorganisms`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | It must be -14°C or colder. Increase your plant production 2 steps. | — |
| Generated text | Requires -14°C or colder. Increase your plant production 2 steps. | — |

Pets declaration:

```pets
CLASS DesignedMicroorganisms : AutomatedCard {
  cost = 16
  requirement = HAS "MAX 8 TemperatureStep"
  This:: ScienceTag<This>, MicrobeTag<This>
  This: PROD[2 Plant]
}
```

### Domed Crater

Class: `DomedCrater`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Oxygen must be 7% or less. Gain 3 plants and place a city tile. Decrease your energy production 1 step and increase M€ production 3 steps. | — |
| Generated text | Requires 7% oxygen or less. Gain 3 plants. Place a city tile. Decrease your energy production 1 step and increase your M€ production 3 steps. | — |

Pets declaration:

```pets
CLASS DomedCrater : AutomatedCard {
  cost = 24
  requirement = HAS "MAX 7 OxygenStep"
  This:: CityTag<This>, BuildingTag<This>
  This: 3 Plant, CityTile<>, PROD[-Energy, 3 MC]
  End: VictoryPoint
}
```

### Dust Seals

Class: `DustSeals`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 or less ocean tiles. | — |
| Generated text | Requires 3 or fewer ocean tiles. | — |

Pets declaration:

```pets
CLASS DustSeals : AutomatedCard {
  cost = 2
  requirement = HAS "MAX 3 OceanTile"
  End: VictoryPoint
}
```

### Ecological Zone

Class: `EcologicalZone`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have a greenery tile. Place this tile ADJACENT TO ANY GREENERY TILE. 1 VP per 2 animals on this card. | Effect: When you play an animal or a plant tag (including these 2), add an animal to this card. |
| Generated text | Requires that you have a greenery tile. Place this tile on a land area next to any greenery tile. 1 VP per 2 animals on this card. | Effect: When you play an animal tag or a plant tag (including this), add 1 animal to this card. |

Pets declaration:

```pets
CLASS EcologicalZone : ActiveCard, ResourceCard<Class<Animal>> {
  cost = 12
  requirement = HAS "GreeneryTile"
  This:: AnimalTag<This>, PlantTag<This>
  This: EcologicalZone_SpecialTile<LandArea(HAS Neighbor<GreeneryTile<Anyone>>)>
  AnimalTag OR PlantTag: Animal<This>
  End: VictoryPoint / 2 Animal<This>
}
```

### Energy Saving

Class: `EnergySaving`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 1 step for each city tile in play. | — |
| Generated text | Increase your energy production 1 step per city tile in play. | — |

Pets declaration:

```pets
CLASS EnergySaving : AutomatedCard {
  cost = 15
  This:: PowerTag<This>
  This: PROD[Energy / CityTile<Anyone>]
}
```

### Eos Chasma National Park

Class: `EosChasmaNationalPark`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -12°C or warmer. Add 1 animal TO ANY ANIMAL CARD. Gain 3 plants. Increase your M€ production 2 steps. | — |
| Generated text | Requires -12°C or warmer. Add 1 animal to another card. Gain 3 plants. Increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS EosChasmaNationalPark : AutomatedCard {
  cost = 16
  requirement = HAS "9 TemperatureStep"
  This:: PlantTag<This>, BuildingTag<This>
  This: Animal, 3 Plant, PROD[2 MC]
  End: VictoryPoint
}
```

### Equatorial Magnetizer

Class: `EquatorialMagnetizer`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Decrease your energy production 1 step to increase your terraform rating 1 step. |
| Generated text | — | Action: Decrease your energy production 1 step to raise your terraform rating 1 step. |

Pets declaration:

```pets
CLASS EquatorialMagnetizer : ActionCard, ActiveCard {
  cost = 11
  This:: BuildingTag<This>
  PROD[Energy] -> TerraformRating
}
```

### Extreme-Cold Fungus

Class: `ExtremeColdFungus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | It must be -10°C or colder. | Action: Gain 1 plant or add 2 microbes to ANOTHER card. |
| Generated text | Requires -10°C or colder. | Action: Gain 1 plant, or add 2 microbes to another card. |

Pets declaration:

```pets
CLASS ExtremeColdFungus : ActionCard, ActiveCard {
  cost = 13
  requirement = HAS "MAX 10 TemperatureStep"
  This:: MicrobeTag<This>
  -> Plant
  -> 2 Microbe
}
```

### Farming

Class: `Farming`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires +4°C or warmer. Increase your M€ production 2 steps and your plant production 2 steps. Gain 2 plants. | — |
| Generated text | Requires +4°C or warmer. Increase your M€ production and your plant production 2 steps each. Gain 2 plants. | — |

Pets declaration:

```pets
CLASS Farming : AutomatedCard {
  cost = 16
  requirement = HAS "17 TemperatureStep"
  This:: PlantTag<This>
  This: PROD[2 MC, 2 Plant], 2 Plant
  End: 2 VictoryPoint
}
```

### Fish

Class: `Fish`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2°C or warmer. Decrease any plant production 1 step. 1 VP for each animal on this card. | Action: Add 1 animal to this card. |
| Generated text | Requires +2°C or warmer. Decrease any player's plant production 1 step. 1 VP per animal on this card. | Action: Add 1 animal to this card. |

Pets declaration:

```pets
CLASS Fish : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 9
  requirement = HAS "16 TemperatureStep"
  This:: AnimalTag<This>
  This: PROD[-Plant<Anyone>]
  End: VictoryPoint / Animal<This>
  -> Animal<This>
}
```

### Flooding

Class: `Flooding`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place an ocean tile. IF THERE ARE TILES ADJACENT TO THIS OCEAN TILE, YOU MAY REMOVE 4 M€ FROM THE OWNER OF ONE OF THOSE TILES. | — |
| Generated text | Place an ocean tile or place an ocean tile on an area reserved for ocean next to a tile anyone owns, then you may remove up to 4 M€ from any player. | — |

Pets declaration:

```pets
CLASS Flooding : EventCard {
  cost = 7
  This:: EventTag<This>
  This: OceanTile<> OR (OceanTile<WaterArea(HAS MAX 0 Tile, HAS Neighbor<OwnedTile<Anyone>>)>! THEN -4 MC<Anyone>?)
  End: -VictoryPoint
}
```

### Food Factory

Class: `FoodFactory`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your plant production 1 step and increase your M€ production 4 steps. | — |
| Generated text | Decrease your plant production 1 step and increase your M€ production 4 steps. | — |

Pets declaration:

```pets
CLASS FoodFactory : AutomatedCard {
  cost = 12
  This:: BuildingTag<This>
  This: PROD[-Plant, 4 MC]
  End: VictoryPoint
}
```

### Fueled Generators

Class: `FueledGenerators`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 1 step and increase your energy production 1 step. | — |
| Generated text | Decrease your M€ production 1 step and increase your energy production 1 step. | — |

Pets declaration:

```pets
CLASS FueledGenerators : AutomatedCard {
  cost = 1
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[-MC, Energy]
}
```

### Fusion Power

Class: `FusionPower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 power tags. Increase your energy production 3 steps. | — |
| Generated text | Requires 2 power tags. Increase your energy production 3 steps. | — |

Pets declaration:

```pets
CLASS FusionPower : AutomatedCard {
  cost = 14
  requirement = HAS "2 PowerTag"
  This:: ScienceTag<This>, PowerTag<This>, BuildingTag<This>
  This: PROD[3 Energy]
}
```

### Ganymede Colony

Class: `GanymedeColony`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a city tile ON THE RESERVED AREA. 1 VP per Jovian tag you have. | — |
| Generated text | Place a city tile on the reserved area outside Mars. 1 VP per Jovian tag you have. | — |

Pets declaration:

```pets
CLASS GanymedeColony : AutomatedCard {
  cost = 20
  This:: JovianTag<This>, SpaceTag<This>, CityTag<This>
  This: CityTile<GanymedeColony_RemoteArea>
  End: VictoryPoint / JovianTag
}
```

### Geothermal Power

Class: `GeothermalPower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 2 steps. | — |
| Generated text | Increase your energy production 2 steps. | — |

Pets declaration:

```pets
CLASS GeothermalPower : AutomatedCard {
  cost = 11
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[2 Energy]
}
```

### GHG Factories

Class: `GhgFactories`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and increase your heat production 4 steps. | — |
| Generated text | Decrease your energy production 1 step and increase your heat production 4 steps. | — |

Pets declaration:

```pets
CLASS GhgFactories : AutomatedCard {
  cost = 11
  This:: BuildingTag<This>
  This: PROD[-Energy, 4 Heat]
}
```

### GHG Producing Bacteria

Class: `GhgProducingBacteria`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4% oxygen. | Action: Add 1 microbe to this card, or remove 2 microbes to raise temperature 1 step. |
| Generated text | Requires 4% oxygen. | Action: Add 1 microbe to this card, or spend 2 microbes from this card to raise temperature 1 step. |

Pets declaration:

```pets
CLASS GhgProducingBacteria : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 8
  requirement = HAS "4 OxygenStep"
  This:: ScienceTag<This>, MicrobeTag<This>
  -> Microbe<This>
  2 Microbe<This> -> TemperatureStep
}
```

### Giant Ice Asteroid

Class: `GiantIceAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 2 steps and place 2 ocean tiles. Remove up to 6 plants from any player. | — |
| Generated text | Raise temperature 2 steps. Place 2 ocean tiles. You may remove up to 6 plants from any player. | — |

Pets declaration:

```pets
CLASS GiantIceAsteroid : EventCard {
  cost = 36
  This:: SpaceTag<This>, EventTag<This>
  This: 2 TemperatureStep, 2 OceanTile<>, -6 Plant<Anyone>?
}
```

### Giant Space Mirror

Class: `GiantSpaceMirror`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 3 steps. | — |
| Generated text | Increase your energy production 3 steps. | — |

Pets declaration:

```pets
CLASS GiantSpaceMirror : AutomatedCard {
  cost = 17
  This:: PowerTag<This>, SpaceTag<This>
  This: PROD[3 Energy]
}
```

### Grass

Class: `Grass`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -16°C or warmer. Increase your plant production 1 step. Gain 3 plants. | — |
| Generated text | Requires -16°C or warmer. Increase your plant production 1 step. Gain 3 plants. | — |

Pets declaration:

```pets
CLASS Grass : AutomatedCard {
  cost = 11
  requirement = HAS "7 TemperatureStep"
  This:: PlantTag<This>
  This: PROD[Plant], 3 Plant
}
```

### Great Dam

Class: `GreatDam`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4 ocean tiles. Increase your energy production 2 steps. | — |
| Generated text | Requires 4 ocean tiles. Increase your energy production 2 steps. | — |

Pets declaration:

```pets
CLASS GreatDam : AutomatedCard {
  cost = 12
  requirement = HAS "4 OceanTile"
  autoSelectWhen = HAS "MAX 0 PromoCardPack"
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[2 Energy]
  End: VictoryPoint
}
```

### Greenhouses

Class: `Greenhouses`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 1 plant for each city tile in play. | — |
| Generated text | Gain 1 plant per city tile in play. | — |

Pets declaration:

```pets
CLASS Greenhouses : AutomatedCard {
  cost = 6
  This:: PlantTag<This>, BuildingTag<This>
  This: Plant / CityTile<Anyone>
}
```

### Heather

Class: `Heather`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -14°C or warmer. Increase your plant production 1 step. Gain 1 plant. | — |
| Generated text | Requires -14°C or warmer. Increase your plant production 1 step. Gain 1 plant. | — |

Pets declaration:

```pets
CLASS Heather : AutomatedCard {
  cost = 6
  requirement = HAS "8 TemperatureStep"
  This:: PlantTag<This>
  This: PROD[Plant], Plant
}
```

### Heat Trappers

Class: `HeatTrappers`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease any heat production 2 steps and increase your energy production 1 step. | — |
| Generated text | Decrease any player's heat production 2 steps and increase your energy production 1 step. | — |

Pets declaration:

```pets
CLASS HeatTrappers : AutomatedCard {
  cost = 6
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[-2 Heat<Anyone>, Energy]
  End: -VictoryPoint
}
```

### Herbivores

Class: `Herbivores`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 8% oxygen. Add 1 animal to this card. Decrease any plant production 1 step. 1 VP per 2 animals on this card. | Effect: When you place a greenery tile, add an animal to this card. |
| Generated text | Requires 8% oxygen. Add 1 animal to this card. Decrease any player's plant production 1 step. 1 VP per 2 animals on this card. | Effect: When you place a greenery tile, add 1 animal to this card. |

Pets declaration:

```pets
CLASS Herbivores : ActiveCard, ResourceCard<Class<Animal>> {
  cost = 12
  requirement = HAS "8 OxygenStep"
  This:: AnimalTag<This>
  This: Animal<This>, PROD[-Plant<Anyone>]
  GreeneryTile: Animal<This>
  End: VictoryPoint / 2 Animal<This>
}
```

### Ice Asteroid

Class: `IceAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place 2 ocean tiles. | — |
| Generated text | Place 2 ocean tiles. | — |

Pets declaration:

```pets
CLASS IceAsteroid : EventCard {
  cost = 23
  This:: SpaceTag<This>, EventTag<This>
  This: 2 OceanTile<>
}
```

### Ice Cap Melting

Class: `IceCapMelting`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires +2°C or warmer. Place 1 ocean tile. | — |
| Generated text | Requires +2°C or warmer. Place an ocean tile. | — |

Pets declaration:

```pets
CLASS IceCapMelting : EventCard {
  cost = 5
  requirement = HAS "16 TemperatureStep"
  This:: EventTag<This>
  This: OceanTile<>
}
```

### Immigrant City

Class: `ImmigrantCity`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and decrease your M€ production 2 steps. Place a city tile. | Effect: Each time a city tile is placed, including this, increase your M€ production 1 step. |
| Generated text | Decrease your energy production 1 step and your M€ production 2 steps. Place a city tile. | Effect: When any city tile is placed, increase your M€ production 1 step. |

Pets declaration:

```pets
CLASS ImmigrantCity : ActiveCard {
  cost = 13
  This:: CityTag<This>, BuildingTag<This>
  This: PROD[-Energy, -2 MC], CityTile<>
  CityTile<Anyone>: PROD[MC]
}
```

### Immigration Shuttles

Class: `ImmigrationShuttles`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 5 steps. 1 VP for every 3rd city in play. | — |
| Generated text | Increase your M€ production 5 steps. 1 VP per 3 city tiles in play. | — |

Pets declaration:

```pets
CLASS ImmigrationShuttles : AutomatedCard {
  cost = 31
  This:: EarthTag<This>, SpaceTag<This>
  This: PROD[5 MC]
  End: VictoryPoint / 3 CityTile<Anyone>
}
```

### Imported GHG

Class: `ImportedGhg`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your heat production 1 step and gain 3 heat. | — |
| Generated text | Increase your heat production 1 step. Gain 3 heat. | — |

Pets declaration:

```pets
CLASS ImportedGhg : EventCard {
  cost = 7
  This:: EarthTag<This>, SpaceTag<This>, EventTag<This>
  This: PROD[Heat], 3 Heat
}
```

### Imported Hydrogen

Class: `ImportedHydrogen`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 3 plants, or add 3 microbes or 2 animals to ANOTHER card. Place an ocean tile. | — |
| Generated text | Gain 3 plants, add 3 microbes to another card, or add 2 animals to another card. Place an ocean tile. | — |

Pets declaration:

```pets
CLASS ImportedHydrogen : EventCard {
  cost = 16
  This:: EarthTag<This>, SpaceTag<This>, EventTag<This>
  This: 3 Plant OR 3 Microbe OR 2 Animal, OceanTile<>
}
```

### Imported Nitrogen

Class: `ImportedNitrogen`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 1 step and gain 4 plants. Add 3 microbes to ANOTHER card and 2 animals to ANOTHER card. | — |
| Generated text | Raise your terraform rating 1 step. Gain 4 plants. Add 3 microbes to another card. Add 2 animals to another card. | — |

Pets declaration:

```pets
CLASS ImportedNitrogen : EventCard {
  cost = 23
  This:: EarthTag<This>, SpaceTag<This>, EventTag<This>
  This: TerraformRating, 4 Plant, 3 Microbe, 2 Animal
}
```

### Import Of Advanced GHG

Class: `ImportOfAdvancedGhg`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your heat production 2 steps. | — |
| Generated text | Increase your heat production 2 steps. | — |

Pets declaration:

```pets
CLASS ImportOfAdvancedGhg : EventCard {
  cost = 9
  This:: EarthTag<This>, SpaceTag<This>, EventTag<This>
  This: PROD[2 Heat]
}
```

### Industrial Microbes

Class: `IndustrialMicrobes`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production and your steel production 1 step each. | — |
| Generated text | Increase your energy production and your steel production 1 step each. | — |

Pets declaration:

```pets
CLASS IndustrialMicrobes : AutomatedCard {
  cost = 12
  This:: MicrobeTag<This>, BuildingTag<This>
  This: PROD[Energy, Steel]
}
```

### Insects

Class: `Insects`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 6% oxygen. Increase your plant production 1 step for each plant tag you have. | — |
| Generated text | Requires 6% oxygen. Increase your plant production 1 step per plant tag you have. | — |

Pets declaration:

```pets
CLASS Insects : AutomatedCard {
  cost = 9
  requirement = HAS "6 OxygenStep"
  This:: MicrobeTag<This>
  This: PROD[Plant / PlantTag]
}
```

### Insulation

Class: `Insulation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your heat production any number of steps and increase your M€ production the same number of steps. | — |
| Generated text | Decrease your heat production 1 or more steps and increase your M€ production the same number of steps. | — |

Pets declaration:

```pets
CLASS Insulation : AutomatedCard {
  cost = 2
  This: PROD[X MC FROM Heat]
}
```

### Ironworks

Class: `Ironworks`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 4 energy to gain 1 steel and increase oxygen 1 step. |
| Generated text | — | Action: Spend 4 energy to gain 1 steel and raise oxygen 1 step. |

Pets declaration:

```pets
CLASS Ironworks : ActionCard, ActiveCard {
  cost = 11
  This:: BuildingTag<This>
  4 Energy -> Steel, OxygenStep
}
```

### Kelp Farming

Class: `KelpFarming`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 6 ocean tiles. Increase your M€ production 2 steps and your plant production 3 steps. Gain 2 plants. | — |
| Generated text | Requires 6 ocean tiles. Increase your M€ production 2 steps and your plant production 3 steps. Gain 2 plants. | — |

Pets declaration:

```pets
CLASS KelpFarming : AutomatedCard {
  cost = 17
  requirement = HAS "6 OceanTile"
  This:: PlantTag<This>
  This: PROD[2 MC, 3 Plant], 2 Plant
  End: VictoryPoint
}
```

### Lake Marineris

Class: `LakeMarineris`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 0°C or warmer. Place 2 ocean tiles. | — |
| Generated text | Requires 0°C or warmer. Place 2 ocean tiles. | — |

Pets declaration:

```pets
CLASS LakeMarineris : AutomatedCard {
  cost = 18
  requirement = HAS "15 TemperatureStep"
  This: 2 OceanTile<>
  End: 2 VictoryPoint
}
```

### Large Convoy

Class: `LargeConvoy`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place an ocean tile and draw 2 cards. Gain 5 plants, or add 4 animals to ANOTHER card. | — |
| Generated text | Place an ocean tile. Draw 2 cards. Gain 5 plants or add 4 animals to another card. | — |

Pets declaration:

```pets
CLASS LargeConvoy : EventCard {
  cost = 36
  This:: EarthTag<This>, SpaceTag<This>, EventTag<This>
  This: OceanTile<>, 2 ProjectCard, 5 Plant OR 4 Animal
  End: 2 VictoryPoint
}
```

### Lava Flows

Class: `LavaFlows`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 2 steps and place this tile ON EITHER THARSIS THOLUS, ASCRAEUS MONS, PAVONIS MONS OR ARSIA MONS. | — |
| Generated text | Raise temperature 2 steps. Place this tile on a volcanic area if using a board that has one, otherwise place it normally. | — |

Pets declaration:

```pets
CLASS LavaFlows : EventCard {
  cost = 18
  This:: EventTag<This>
  This: 2 TemperatureStep, LavaFlows_SpecialTile<VolcanicArea> OR (MAX 0 VolcanicArea: LavaFlows_SpecialTile<>)
}
```

### Lichen

Class: `Lichen`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -24°C or warmer. Increase your plant production 1 step. | — |
| Generated text | Requires -24°C or warmer. Increase your plant production 1 step. | — |

Pets declaration:

```pets
CLASS Lichen : AutomatedCard {
  cost = 7
  requirement = HAS "3 TemperatureStep"
  This:: PlantTag<This>
  This: PROD[Plant]
}
```

### Livestock

Class: `Livestock`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 9% oxygen. Decrease your plant production 1 step and increase your M€ production 2 steps. 1 VP for each animal on this card. | Action: Add 1 animal to this card. |
| Generated text | Requires 9% oxygen. Decrease your plant production 1 step and increase your M€ production 2 steps. 1 VP per animal on this card. | Action: Add 1 animal to this card. |

Pets declaration:

```pets
CLASS Livestock : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 13
  requirement = HAS "9 OxygenStep"
  This:: AnimalTag<This>
  This: PROD[-Plant, 2 MC]
  End: VictoryPoint / Animal<This>
  -> Animal<This>
}
```

### Local Heat Trapping

Class: `LocalHeatTrapping`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Spend 5 heat to either gain 4 plants, or to add 2 animals to ANOTHER card. | — |
| Generated text | Remove 5 heat. Gain 4 plants or add 2 animals to another card. | — |

Pets declaration:

```pets
CLASS LocalHeatTrapping : EventCard {
  cost = 1
  This:: EventTag<This>
  This: -5 Heat, 4 Plant OR 2 Animal
}
```

### Lunar Beam

Class: `LunarBeam`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 2 steps and increase your heat production and energy production 2 steps each. | — |
| Generated text | Decrease your M€ production 2 steps and increase your heat production and your energy production 2 steps each. | — |

Pets declaration:

```pets
CLASS LunarBeam : AutomatedCard {
  cost = 13
  This:: EarthTag<This>, PowerTag<This>
  This: PROD[-2 MC, 2 Heat, 2 Energy]
}
```

### Magnetic Field Dome

Class: `MagneticFieldDome`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 2 steps and increase your plant production 1 step. Raise your terraform rating 1 step. | — |
| Generated text | Decrease your energy production 2 steps and increase your plant production 1 step. Raise your terraform rating 1 step. | — |

Pets declaration:

```pets
CLASS MagneticFieldDome : AutomatedCard {
  cost = 5
  This:: BuildingTag<This>
  This: PROD[-2 Energy, Plant], TerraformRating
}
```

### Magnetic Field Generators

Class: `MagneticFieldGenerators`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 4 steps and increase your plant production 2 steps. Raise your TR 3 steps. | — |
| Generated text | Decrease your energy production 4 steps and increase your plant production 2 steps. Raise your terraform rating 3 steps. | — |

Pets declaration:

```pets
CLASS MagneticFieldGenerators : AutomatedCard {
  cost = 20
  autoSelectWhen = HAS "MAX 0 PromoCardPack"
  This:: BuildingTag<This>
  This: PROD[-4 Energy, 2 Plant], 3 TerraformRating
}
```

### Mangrove

Class: `Mangrove`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires +4°C or warmer. Place a Greenery tile ON AN AREA RESERVED FOR OCEAN and raise oxygen 1 step. Disregard normal placement restrictions for this. | — |
| Generated text | Requires +4°C or warmer. Place a greenery tile on an area reserved for ocean. | — |

Pets declaration:

```pets
CLASS Mangrove : AutomatedCard {
  cost = 12
  requirement = HAS "17 TemperatureStep"
  This:: PlantTag<This>
  This: GreeneryTile<WaterArea>
  End: VictoryPoint
}
```

### Martian Rails

Class: `MartianRails`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 1 energy to gain 1 M€ for each city tile ON MARS. |
| Generated text | — | Action: Spend 1 energy to gain 1 M€ per city tile in play on Mars. |

Pets declaration:

```pets
CLASS MartianRails : ActionCard, ActiveCard {
  cost = 13
  This:: BuildingTag<This>
  Energy -> MC / CityTile<Anyone, MarsArea>
}
```

### Methane From Titan

Class: `MethaneFromTitan`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2% oxygen. Increase your heat production 2 steps and your plant production 2 steps. | — |
| Generated text | Requires 2% oxygen. Increase your heat production and your plant production 2 steps each. | — |

Pets declaration:

```pets
CLASS MethaneFromTitan : AutomatedCard {
  cost = 28
  requirement = HAS "2 OxygenStep"
  This:: JovianTag<This>, SpaceTag<This>
  This: PROD[2 Heat, 2 Plant]
  End: 2 VictoryPoint
}
```

### Micro-Mills

Class: `MicroMills`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your heat production 1 step. | — |
| Generated text | Increase your heat production 1 step. | — |

Pets declaration:

```pets
CLASS MicroMills : AutomatedCard {
  cost = 3
  This: PROD[Heat]
}
```

### Mining Expedition

Class: `MiningExpedition`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise oxygen 1 step. Remove 2 plants from any player. Gain 2 steel. | — |
| Generated text | Raise oxygen 1 step. You may remove up to 2 plants from any player. Gain 2 steel. | — |

Pets declaration:

```pets
CLASS MiningExpedition : EventCard {
  cost = 12
  This:: EventTag<This>
  This: OxygenStep, -2 Plant<Anyone>?, 2 Steel
}
```

### Mining Rights

Class: `MiningRights`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place this tile on an area with a steel or titanium placement bonus. Increase that production 1 step. | — |
| Generated text | Place this tile, then if this tile is on a land area with a steel placement bonus, increase your steel production 1 step, or if this tile is on a land area with a titanium placement bonus, increase your titanium production 1 step. | — |

Pets declaration:

```pets
CLASS MiningRights : AutomatedCard {
  cost = 9
  This:: BuildingTag<This>
  This: MiningRights_SpecialTile<> THEN PROD[(LandArea(HAS MiningRights_SpecialTile, HAS PlacementBonus<Class<Steel>>): Steel) OR (LandArea(HAS MiningRights_SpecialTile, HAS PlacementBonus<Class<Titanium>>): Titanium)]
}
```

### Mohole Area

Class: `MoholeArea`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your heat production 4 steps. Place this tile ON AN AREA RESERVED FOR OCEAN. | — |
| Generated text | Increase your heat production 4 steps. Place this tile on an area reserved for ocean. | — |

Pets declaration:

```pets
CLASS MoholeArea : AutomatedCard {
  cost = 20
  This:: BuildingTag<This>
  This: PROD[4 Heat], MoholeArea_SpecialTile<WaterArea>
}
```

### Moss

Class: `Moss`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 ocean tiles and that you lose 1 plant. Increase your plant production 1 step. | — |
| Generated text | Requires 3 ocean tiles. Increase your plant production 1 step. Remove 1 plant. | — |

Pets declaration:

```pets
CLASS Moss : AutomatedCard {
  cost = 4
  requirement = HAS "3 OceanTile"
  This:: PlantTag<This>
  This: PROD[Plant], -Plant
}
```

### Natural Preserve

Class: `NaturalPreserve`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Oxygen must be 4% or less. Place this tile NEXT TO NO OTHER TILE. Increase your M€ production 1 step. | — |
| Generated text | Requires 4% oxygen or less. Place this tile on a land area next to no other tile. Increase your M€ production 1 step. | — |

Pets declaration:

```pets
CLASS NaturalPreserve : AutomatedCard {
  cost = 9
  requirement = HAS "MAX 4 OxygenStep"
  This:: ScienceTag<This>, BuildingTag<This>
  This: NaturalPreserve_SpecialTile<LandArea(HAS MAX 0 Neighbor)>, PROD[MC]
  End: VictoryPoint
}
```

### Nitrite Reducing Bacteria

Class: `NitriteReducingBacteria`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 3 microbes to this card. | Action: Add 1 microbe to this card, or remove 3 microbes to increase your TR 1 step. |
| Generated text | Add 3 microbes to this card. | Action: Add 1 microbe to this card, or spend 3 microbes from this card to raise your terraform rating 1 step. |

Pets declaration:

```pets
CLASS NitriteReducingBacteria : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 11
  This:: MicrobeTag<This>
  This: 3 Microbe<This>
  -> Microbe<This>
  3 Microbe<This> -> TerraformRating
}
```

### Nitrogen-Rich Asteroid

Class: `NitrogenRichAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your terraform rating 2 steps and temperature 1 step. Increase your plant production 1 step, or 4 steps if you have 3 plant tags. | — |
| Generated text | Raise your terraform rating 2 steps and temperature 1 step. Increase your plant production 1 step, or if you have 3 plant tags, increase your plant production 4 steps. | — |

Pets declaration:

```pets
CLASS NitrogenRichAsteroid : EventCard {
  cost = 31
  This:: SpaceTag<This>, EventTag<This>
  This: 2 TerraformRating, TemperatureStep, PROD[Plant OR (3 PlantTag: 4 Plant)]
}
```

### Nitrophilic Moss

Class: `NitrophilicMoss`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 ocean tiles and that you lose 2 plants. Increase your plant production 2 steps. | — |
| Generated text | Requires 3 ocean tiles. Increase your plant production 2 steps. Remove 2 plants. | — |

Pets declaration:

```pets
CLASS NitrophilicMoss : AutomatedCard {
  cost = 8
  requirement = HAS "3 OceanTile"
  This:: PlantTag<This>
  This: PROD[2 Plant], -2 Plant
}
```

### Noctis City

Class: `NoctisCity`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and increase your M€ production 3 steps. Place a city tile ON THE RESERVED AREA, disregarding normal placement restrictions. | — |
| Generated text | Decrease your energy production 1 step and increase your M€ production 3 steps. Place a city tile on the reserved area if using a board that has one, otherwise place it normally. | — |

Pets declaration:

```pets
CLASS NoctisCity : AutomatedCard {
  cost = 18
  This:: CityTag<This>, BuildingTag<This>
  This: PROD[-Energy, 3 MC], CityTile<NoctisArea> OR (MAX 0 NoctisArea: CityTile<>)
}
```

### Noctis Farming

Class: `NoctisFarming`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -20°C or warmer. Increase your M€ production 1 step and gain 2 plants. | — |
| Generated text | Requires -20°C or warmer. Increase your M€ production 1 step. Gain 2 plants. | — |

Pets declaration:

```pets
CLASS NoctisFarming : AutomatedCard {
  cost = 10
  requirement = HAS "5 TemperatureStep"
  This:: PlantTag<This>, BuildingTag<This>
  This: PROD[MC], 2 Plant
  End: VictoryPoint
}
```

### Nuclear Power

Class: `NuclearPower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 2 steps and increase your energy production 3 steps. | — |
| Generated text | Decrease your M€ production 2 steps and increase your energy production 3 steps. | — |

Pets declaration:

```pets
CLASS NuclearPower : AutomatedCard {
  cost = 10
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[-2 MC, 3 Energy]
}
```

### Nuclear Zone

Class: `NuclearZone`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place this tile and raise temperature 2 steps. | — |
| Generated text | Place this tile. Raise temperature 2 steps. | — |

Pets declaration:

```pets
CLASS NuclearZone : AutomatedCard {
  cost = 10
  This:: EarthTag<This>
  This: NuclearZone_SpecialTile<>, 2 TemperatureStep
  End: -2 VictoryPoint
}
```

### Open City

Class: `OpenCity`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 12% oxygen. Decrease your energy production 1 step and increase your M€ production 4 steps. Gain 2 plants and place a city tile. | — |
| Generated text | Requires 12% oxygen. Decrease your energy production 1 step and increase your M€ production 4 steps. Gain 2 plants. Place a city tile. | — |

Pets declaration:

```pets
CLASS OpenCity : AutomatedCard {
  cost = 23
  requirement = HAS "12 OxygenStep"
  This:: CityTag<This>, BuildingTag<This>
  This: PROD[-Energy, 4 MC], 2 Plant, CityTile<>
  End: VictoryPoint
}
```

### Optimal Aerobraking

Class: `OptimalAerobraking`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you play a space event, you gain 3 M€ and 3 heat. |
| Generated text | — | Effect: When you play a space event card, gain 3 M€ and 3 heat. |

Pets declaration:

```pets
CLASS OptimalAerobraking : ActiveCard {
  cost = 7
  This:: SpaceTag<This>
  EventCard(HAS SpaceTag): 3 MC, 3 Heat
}
```

### Ore Processor

Class: `OreProcessor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 4 energy to gain 1 titanium and increase oxygen 1 step. |
| Generated text | — | Action: Spend 4 energy to gain 1 titanium and raise oxygen 1 step. |

Pets declaration:

```pets
CLASS OreProcessor : ActionCard, ActiveCard {
  cost = 13
  This:: BuildingTag<This>
  4 Energy -> Titanium, OxygenStep
}
```

### Permafrost Extraction

Class: `PermafrostExtraction`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -8°C or warmer. Place 1 ocean tile. | — |
| Generated text | Requires -8°C or warmer. Place an ocean tile. | — |

Pets declaration:

```pets
CLASS PermafrostExtraction : EventCard {
  cost = 8
  requirement = HAS "11 TemperatureStep"
  This:: EventTag<This>
  This: OceanTile<>
}
```

### Peroxide Power

Class: `PeroxidePower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 1 step and increase your energy production 2 steps. | — |
| Generated text | Decrease your M€ production 1 step and increase your energy production 2 steps. | — |

Pets declaration:

```pets
CLASS PeroxidePower : AutomatedCard {
  cost = 7
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[-MC, 2 Energy]
}
```

### Pets

Class: `Pets`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 1 animal to this card. 1 VP per 2 animals here. | Effect: When any city tile is placed, add an animal to this card. ANIMALS MAY NOT BE REMOVED FROM THIS CARD |
| Generated text | Add 1 animal to this card. 1 VP per 2 animals on this card. | Effect: When any city tile is placed, add 1 animal to this card. Animals may not be removed from this card. |

Pets declaration:

```pets
CLASS Pets : ActiveCard, ResourceCard<Class<Animal>> {
  cost = 10
  This:: EarthTag<This>, AnimalTag<This>
  This: Animal<This>
  CityTile<Anyone>: Animal<This>
  -Animal<This>:: Die
  End: VictoryPoint / 2 Animal<This>
}
```

### Phobos Space Haven

Class: `PhobosSpaceHaven`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 1 step and place a city tile ON THE RESERVED AREA. | — |
| Generated text | Increase your titanium production 1 step. Place a city tile on the reserved area outside Mars. | — |

Pets declaration:

```pets
CLASS PhobosSpaceHaven : AutomatedCard {
  cost = 25
  This:: SpaceTag<This>, CityTag<This>
  This: PROD[Titanium], CityTile<PhobosSpaceHaven_RemoteArea>
  End: 3 VictoryPoint
}
```

### Plantation

Class: `Plantation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 science tags. Place a greenery tile and raise oxygen 1 step. | — |
| Generated text | Requires 2 science tags. Place a greenery tile. | — |

Pets declaration:

```pets
CLASS Plantation : AutomatedCard {
  cost = 15
  requirement = HAS "2 ScienceTag"
  This:: PlantTag<This>
  This: DefaultGreeneryTile
}
```

### Power Grid

Class: `PowerGrid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 1 step for each power tag you have, including this. | — |
| Generated text | Increase your energy production 1 step per power tag you have (including this). | — |

Pets declaration:

```pets
CLASS PowerGrid : AutomatedCard {
  cost = 18
  This:: PowerTag<This>
  This: PROD[Energy / PowerTag]
}
```

### Power Plant

Class: `PowerPlant`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 1 step. | — |
| Generated text | Increase your energy production 1 step. | — |

Pets declaration:

```pets
CLASS PowerPlant : AutomatedCard {
  cost = 4
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[Energy]
}
```

### Predators

Class: `Predators`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 11% oxygen. 1 VP per animal on this card. | Action: Remove 1 animal from any card and add it to this card. |
| Generated text | Requires 11% oxygen. 1 VP per animal on this card. | Action: Remove 1 animal from any player's card to add 1 animal to this card. |

Pets declaration:

```pets
CLASS Predators : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 14
  requirement = HAS "11 OxygenStep"
  This:: AnimalTag<This>
  End: VictoryPoint / Animal<This>
  Animal<Anyone> -> Animal<This>
}
```

### Protected Valley

Class: `ProtectedValley`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 2 steps. Place a greenery tile ON AN AREA RESERVED FOR OCEAN, disregarding normal placement restrictions, and increase oxygen 1 step. | — |
| Generated text | Increase your M€ production 2 steps. Place a greenery tile on an area reserved for ocean. | — |

Pets declaration:

```pets
CLASS ProtectedValley : AutomatedCard {
  cost = 23
  This:: PlantTag<This>, BuildingTag<This>
  This: PROD[2 MC], GreeneryTile<WaterArea>
}
```

### Rad-Chem Factory

Class: `RadChemFactory`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step. Raise your terraform rating 2 steps. | — |
| Generated text | Decrease your energy production 1 step. Raise your terraform rating 2 steps. | — |

Pets declaration:

```pets
CLASS RadChemFactory : AutomatedCard {
  cost = 8
  This:: BuildingTag<This>
  This: PROD[-Energy], 2 TerraformRating
}
```

### Regolith Eaters

Class: `RegolithEaters`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Add 1 microbe to this card, or remove 2 microbes from this card to raise oxygen level 1 step. |
| Generated text | — | Action: Add 1 microbe to this card, or spend 2 microbes from this card to raise oxygen 1 step. |

Pets declaration:

```pets
CLASS RegolithEaters : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 13
  This:: ScienceTag<This>, MicrobeTag<This>
  -> Microbe<This>
  2 Microbe<This> -> OxygenStep
}
```

### Release Of Inert Gases

Class: `ReleaseOfInertGases`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your terraform rating 2 steps. | — |
| Generated text | Raise your terraform rating 2 steps. | — |

Pets declaration:

```pets
CLASS ReleaseOfInertGases : EventCard {
  cost = 14
  This:: EventTag<This>
  This: 2 TerraformRating
}
```

### Research Outpost

Class: `ResearchOutpost`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a city tile NEXT TO NO OTHER TILE. | Effect: When you play a card, you pay 1 M€ less for it. |
| Generated text | Place a city tile on a land area next to no other tile. | Effect: When you play a card, you pay 1 M€ less for it. |

Pets declaration:

```pets
CLASS ResearchOutpost : ActiveCard {
  cost = 18
  This:: ScienceTag<This>, CityTag<This>, BuildingTag<This>
  This: CityTile<LandArea(HAS MAX 0 Neighbor)>
  PayingFor<Class<CardFront>>:: -Owed
}
```

### Rover Construction

Class: `RoverConstruction`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When any city tile is placed, gain 2 M€. |
| Generated text | — | Effect: When any city tile is placed, gain 2 M€. |

Pets declaration:

```pets
CLASS RoverConstruction : ActiveCard {
  cost = 8
  This:: BuildingTag<This>
  CityTile<Anyone>: 2 MC
  End: VictoryPoint
}
```

### Search For Life

Class: `SearchForLife`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Oxygen must be 6% or less. 3 VPs if you have one or more science resources here. | Action: Spend 1 M€ to reveal and discard the top card of the draw deck. If that card has a microbe tag, add a science resource here. |
| Generated text | Requires 6% oxygen or less. 3 VPs if you have 1 or more science resources on this card. | Action: \[MC -&gt; Science&lt;This&gt;?\]. |

Pets declaration:

```pets
CLASS SearchForLife : ActionCard, ActiveCard, ResourceCard<Class<Science>> {
  cost = 3
  requirement = HAS "MAX 6 OxygenStep"
  This:: ScienceTag<This>
  End IF Science<This>: 3 VictoryPoint
  MC -> Science<This>?
}
```

### Shuttles

Class: `Shuttles`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 5% oxygen. Decrease your energy production 1 step and increase your M€ production 2 steps. | Effect: When you play a space card, you pay 2 M€ less for it. |
| Generated text | Requires 5% oxygen. Decrease your energy production 1 step and increase your M€ production 2 steps. | Effect: When you play a space tag, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS Shuttles : ActiveCard {
  cost = 10
  requirement = HAS "5 OxygenStep"
  This:: SpaceTag<This>
  This: PROD[-Energy, 2 MC]
  PayingFor<Class<SpaceTag>>:: -2 Owed
  End: VictoryPoint
}
```

### Small Animals

Class: `SmallAnimals`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 6% oxygen. Decrease any plant production 1 step. 1 VP per 2 animals on this card. | Action: Add 1 animal to this card. |
| Generated text | Requires 6% oxygen. Decrease any player's plant production 1 step. 1 VP per 2 animals on this card. | Action: Add 1 animal to this card. |

Pets declaration:

```pets
CLASS SmallAnimals : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 6
  requirement = HAS "6 OxygenStep"
  This:: AnimalTag<This>
  This: PROD[-Plant<Anyone>]
  End: VictoryPoint / 2 Animal<This>
  -> Animal<This>
}
```

### Soil Factory

Class: `SoilFactory`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and increase your plant production 1 step. | — |
| Generated text | Decrease your energy production 1 step and increase your plant production 1 step. | — |

Pets declaration:

```pets
CLASS SoilFactory : AutomatedCard {
  cost = 9
  This:: BuildingTag<This>
  This: PROD[-Energy, Plant]
  End: VictoryPoint
}
```

### Solar Power

Class: `SolarPower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 1 step. | — |
| Generated text | Increase your energy production 1 step. | — |

Pets declaration:

```pets
CLASS SolarPower : AutomatedCard {
  cost = 11
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[Energy]
  End: VictoryPoint
}
```

### Solar Wind Power

Class: `SolarWindPower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 1 step and gain 2 titanium. | — |
| Generated text | Increase your energy production 1 step. Gain 2 titanium. | — |

Pets declaration:

```pets
CLASS SolarWindPower : AutomatedCard {
  cost = 11
  This:: ScienceTag<This>, SpaceTag<This>, PowerTag<This>
  This: PROD[Energy], 2 Titanium
}
```

### Soletta

Class: `Soletta`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your heat production 7 steps. | — |
| Generated text | Increase your heat production 7 steps. | — |

Pets declaration:

```pets
CLASS Soletta : AutomatedCard {
  cost = 35
  This:: SpaceTag<This>
  This: PROD[7 Heat]
}
```

### Space Mirrors

Class: `SpaceMirrors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 7 M€ to increase your energy production 1 step. |
| Generated text | — | Action: Spend 7 M€ to increase your energy production 1 step. |

Pets declaration:

```pets
CLASS SpaceMirrors : ActionCard, ActiveCard {
  cost = 3
  This:: PowerTag<This>, SpaceTag<This>
  7 MC -> PROD[Energy]
}
```

### Special Design

Class: `SpecialDesign`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | The next card you play this generation is +2 or -2 in global requirements, your choice. | — |
| Generated text | You may treat the global parameter requirement of the next card you play this generation as if it is 2 steps lower or higher. | — |

Pets declaration:

```pets
CLASS SpecialDesign : EventCard {
  cost = 4
  This:: ScienceTag<This>, EventTag<This>
  This: SpecialDesign_NextCardEffect
}
```

### Steelworks

Class: `Steelworks`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 4 energy to gain 2 steel and increase oxygen 1 step. |
| Generated text | — | Action: Spend 4 energy to gain 2 steel and raise oxygen 1 step. |

Pets declaration:

```pets
CLASS Steelworks : ActionCard, ActiveCard {
  cost = 15
  This:: BuildingTag<This>
  4 Energy -> 2 Steel, OxygenStep
}
```

### Strip Mine

Class: `StripMine`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 2 steps. Increase your steel production 2 steps and your titanium production 1 step. Raise oxygen 2 steps. | — |
| Generated text | Decrease your energy production 2 steps and increase your steel production 2 steps and your titanium production 1 step. Raise oxygen 2 steps. | — |

Pets declaration:

```pets
CLASS StripMine : AutomatedCard {
  cost = 25
  This:: BuildingTag<This>
  This: PROD[-2 Energy, 2 Steel, Titanium], 2 OxygenStep
}
```

### Subterranean Reservoir

Class: `SubterraneanReservoir`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place 1 ocean tile. | — |
| Generated text | Place an ocean tile. | — |

Pets declaration:

```pets
CLASS SubterraneanReservoir : EventCard {
  cost = 11
  This:: EventTag<This>
  This: OceanTile<>
}
```

### Symbiotic Fungus

Class: `SymbioticFungus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -14°C or warmer. | Action: Add a microbe to ANOTHER card. |
| Generated text | Requires -14°C or warmer. | Action: Add 1 microbe to another card. |

Pets declaration:

```pets
CLASS SymbioticFungus : ActionCard, ActiveCard {
  cost = 4
  requirement = HAS "8 TemperatureStep"
  This:: MicrobeTag<This>
  -> Microbe
}
```

### Tectonic Stress Power

Class: `TectonicStressPower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 science tags. Increase your energy production 3 steps. | — |
| Generated text | Requires 2 science tags. Increase your energy production 3 steps. | — |

Pets declaration:

```pets
CLASS TectonicStressPower : AutomatedCard {
  cost = 18
  requirement = HAS "2 ScienceTag"
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[3 Energy]
  End: VictoryPoint
}
```

### Towing A Comet

Class: `TowingAComet`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 2 plants. Raise oxygen level 1 step and place an ocean tile. | — |
| Generated text | Gain 2 plants. Raise oxygen 1 step. Place an ocean tile. | — |

Pets declaration:

```pets
CLASS TowingAComet : EventCard {
  cost = 23
  This:: SpaceTag<This>, EventTag<This>
  This: 2 Plant, OxygenStep, OceanTile<>
}
```

### Trees

Class: `Trees`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -4°C or warmer. Increase your plant production 3 steps. Gain 1 plant. | — |
| Generated text | Requires -4°C or warmer. Increase your plant production 3 steps. Gain 1 plant. | — |

Pets declaration:

```pets
CLASS Trees : AutomatedCard {
  cost = 13
  requirement = HAS "13 TemperatureStep"
  This:: PlantTag<This>
  This: PROD[3 Plant], Plant
  End: VictoryPoint
}
```

### Tundra Farming

Class: `TundraFarming`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -6°C or warmer. Increase your plant production 1 step and your M€ production 2 steps. Gain 1 plant. | — |
| Generated text | Requires -6°C or warmer. Increase your plant production 1 step and your M€ production 2 steps. Gain 1 plant. | — |

Pets declaration:

```pets
CLASS TundraFarming : AutomatedCard {
  cost = 16
  requirement = HAS "12 TemperatureStep"
  This:: PlantTag<This>
  This: PROD[Plant, 2 MC], Plant
  End: 2 VictoryPoint
}
```

### Underground City

Class: `UndergroundCity`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a city tile. Decrease your energy production 2 steps and increase your steel production 2 steps. | — |
| Generated text | Place a city tile. Decrease your energy production 2 steps and increase your steel production 2 steps. | — |

Pets declaration:

```pets
CLASS UndergroundCity : AutomatedCard {
  cost = 18
  This:: CityTag<This>, BuildingTag<This>
  This: CityTile<>, PROD[-2 Energy, 2 Steel]
}
```

### Underground Detonations

Class: `UndergroundDetonations`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 10 M€ to increase your heat production 2 steps. |
| Generated text | — | Action: Spend 10 M€ to increase your heat production 2 steps. |

Pets declaration:

```pets
CLASS UndergroundDetonations : ActionCard, ActiveCard {
  cost = 6
  This:: BuildingTag<This>
  10 MC -> PROD[2 Heat]
}
```

### Urbanized Area

Class: `UrbanizedArea`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and increase your M€ production 2 steps. Place a city tile ADJACENT TO AT LEAST 2 OTHER CITY TILES. | — |
| Generated text | Decrease your energy production 1 step and increase your M€ production 2 steps. Place a city tile on a land area next to any two city tiles. | — |

Pets declaration:

```pets
CLASS UrbanizedArea : AutomatedCard {
  cost = 10
  This:: CityTag<This>, BuildingTag<This>
  This: PROD[-Energy, 2 MC], CityTile<LandArea(HAS 2 Neighbor<CityTile<Anyone>>)>
}
```

### Water Import From Europa

Class: `WaterImportFromEuropa`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | 1 VP for each Jovian tag you have. | Action: Pay 12 M€ to place an ocean tile. TITANIUM MAY BE USED as if playing a space card. |
| Generated text | 1 VP per Jovian tag you have. | Action: Spend 12 M€ (titanium may be used) to place an ocean tile. |

Pets declaration:

```pets
CLASS WaterImportFromEuropa : ActionCard, ActiveCard {
  cost = 25
  This:: JovianTag<This>, SpaceTag<This>
  UseAction<This>:: Accepting<Class<Titanium>>
  End: VictoryPoint / JovianTag
  12 MC -> OceanTile<>
}
```

### Water Splitting Plant

Class: `WaterSplittingPlant`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 ocean tiles. | Action: Spend 3 energy to raise oxygen 1 step. |
| Generated text | Requires 2 ocean tiles. | Action: Spend 3 energy to raise oxygen 1 step. |

Pets declaration:

```pets
CLASS WaterSplittingPlant : ActionCard, ActiveCard {
  cost = 12
  requirement = HAS "2 OceanTile"
  This:: BuildingTag<This>
  3 Energy -> OxygenStep
}
```

### Wave Power

Class: `WavePower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 ocean tiles. Increase your energy production 1 step. | — |
| Generated text | Requires 3 ocean tiles. Increase your energy production 1 step. | — |

Pets declaration:

```pets
CLASS WavePower : AutomatedCard {
  cost = 8
  requirement = HAS "3 OceanTile"
  This:: PowerTag<This>
  This: PROD[Energy]
  End: VictoryPoint
}
```

### Windmills

Class: `Windmills`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 7% oxygen. Increase your energy production 1 step. | — |
| Generated text | Requires 7% oxygen. Increase your energy production 1 step. | — |

Pets declaration:

```pets
CLASS Windmills : AutomatedCard {
  cost = 6
  requirement = HAS "7 OxygenStep"
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[Energy]
  End: VictoryPoint
}
```

### Worms

Class: `Worms`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4% oxygen. Increase your plant production 1 step for every 2 microbe tags you have, including this. | — |
| Generated text | Requires 4% oxygen. Increase your plant production 1 step per 2 microbe tags you have (including this). | — |

Pets declaration:

```pets
CLASS Worms : AutomatedCard {
  cost = 8
  requirement = HAS "4 OxygenStep"
  This:: MicrobeTag<This>
  This: PROD[Plant / 2 MicrobeTag]
}
```

### Zeppelins

Class: `Zeppelins`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 5% oxygen. Increase your M€ production 1 step for each city tile ON MARS. | — |
| Generated text | Requires 5% oxygen. Increase your M€ production 1 step per city tile in play on Mars. | — |

Pets declaration:

```pets
CLASS Zeppelins : AutomatedCard {
  cost = 13
  requirement = HAS "5 OxygenStep"
  This: PROD[MC / CityTile<MarsArea, Anyone>]
  End: VictoryPoint
}
```

## Corporate Era Expansion

### Acquired Company

Class: `AcquiredCompany`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 3 steps. | — |
| Generated text | Increase your M€ production 3 steps. | — |

Pets declaration:

```pets
CLASS AcquiredCompany : AutomatedCard {
  cost = 10
  This:: EarthTag<This>
  This: PROD[3 MC]
}
```

### Advanced Alloys

Class: `AdvancedAlloys`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: Each titanium you have is worth 1 M€ extra. Each steel you have is worth 1 M€ extra. |
| Generated text | — | Effect: Each titanium or steel you pay is worth 1 M€ extra. |

Pets declaration:

```pets
CLASS AdvancedAlloys : ActiveCard {
  cost = 9
  This:: GrantedResourceValue<Class<Titanium>, This>, GrantedResourceValue<Class<Steel>, This>
  This:: ScienceTag<This>
}
```

### AI Central

Class: `AiCentral`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 science tags to play. Decrease your energy production 1 step. | Action: Draw 2 cards. |
| Generated text | Requires 3 science tags. Decrease your energy production 1 step. | Action: Draw 2 cards. |

Pets declaration:

```pets
CLASS AiCentral : ActionCard, ActiveCard {
  cost = 21
  requirement = HAS "3 ScienceTag"
  This:: ScienceTag<This>, BuildingTag<This>
  This: PROD[-Energy]
  End: VictoryPoint
  -> 2 ProjectCard
}
```

### Anti-Gravity Technology

Class: `AntiGravityTechnology`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 7 science tags. | Effect: When you play a card, you pay 2 M€ less for it. |
| Generated text | Requires 7 science tags. | Effect: When you play a card, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS AntiGravityTechnology : ActiveCard {
  cost = 14
  requirement = HAS "7 ScienceTag"
  This:: ScienceTag<This>
  PayingFor<Class<CardFront>>:: -2 Owed
  End: 3 VictoryPoint
}
```

### Asteroid Mining Consortium

Class: `AsteroidMiningConsortium`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have titanium production. Decrease any titanium production 1 step and increase your own 1 step. | — |
| Generated text | Requires that you have titanium production. Decrease any player's titanium production 1 step and increase your titanium production 1 step. | — |

Pets declaration:

```pets
CLASS AsteroidMiningConsortium : AutomatedCard {
  cost = 13
  requirement = HAS "PROD[Titanium]"
  This:: JovianTag<This>
  This: PROD[-Titanium<Anyone>, Titanium]
  End: VictoryPoint
}
```

### Bribed Committee

Class: `BribedCommittee`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your terraform rating 2 steps. | — |
| Generated text | Raise your terraform rating 2 steps. | — |

Pets declaration:

```pets
CLASS BribedCommittee : EventCard {
  cost = 7
  This:: EarthTag<This>, EventTag<This>
  This: 2 TerraformRating
  End: -2 VictoryPoint
}
```

### Building Industries

Class: `BuildingIndustries`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and increase your steel production 2 steps. | — |
| Generated text | Decrease your energy production 1 step and increase your steel production 2 steps. | — |

Pets declaration:

```pets
CLASS BuildingIndustries : AutomatedCard {
  cost = 6
  This:: BuildingTag<This>
  This: PROD[-Energy, 2 Steel]
}
```

### Business Contacts

Class: `BusinessContacts`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | LOOK AT THE TOP 4 CARDS FROM THE DECK. TAKE 2 OF THEM INTO HAND AND DISCARD THE OTHER 2 | — |
| Generated text | Look at 4 project cards. Draw 2 of them. | — |

Pets declaration:

```pets
CLASS BusinessContacts : EventCard {
  cost = 7
  This:: EarthTag<This>, EventTag<This>
  This: 2 ProjectCard
}
```

### Business Network

Class: `BusinessNetwork`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 1 step. | Action: LOOK AT THE TOP CARD AND EITHER BUY IT OR DISCARD IT |
| Generated text | Decrease your M€ production 1 step. | Action: Look at 1 project card. You may buy it. |

Pets declaration:

```pets
CLASS BusinessNetwork : ActionCard, ActiveCard {
  cost = 4
  This:: EarthTag<This>
  This: PROD[-MC]
  -> BuyCard?
}
```

### Callisto Penal Mines

Class: `CallistoPenalMines`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 3 steps. | — |
| Generated text | Increase your M€ production 3 steps. | — |

Pets declaration:

```pets
CLASS CallistoPenalMines : AutomatedCard {
  cost = 24
  This:: JovianTag<This>, SpaceTag<This>
  This: PROD[3 MC]
  End: 2 VictoryPoint
}
```

### Caretaker Contract

Class: `CaretakerContract`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 0°C or warmer. | Action: Spend 8 heat to increase your terraform rating 1 step. |
| Generated text | Requires 0°C or warmer. | Action: Spend 8 heat to raise your terraform rating 1 step. |

Pets declaration:

```pets
CLASS CaretakerContract : ActionCard, ActiveCard {
  cost = 3
  requirement = HAS "15 TemperatureStep"
  8 Heat -> TerraformRating
}
```

### Cartel

Class: `Cartel`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step for each Earth tag you have, including this. | — |
| Generated text | Increase your M€ production 1 step per Earth tag you have (including this). | — |

Pets declaration:

```pets
CLASS Cartel : AutomatedCard {
  cost = 8
  This:: EarthTag<This>
  This: PROD[MC / EarthTag]
}
```

### CEO's Favorite Project

Class: `CeosFavoriteProject`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | ADD 1 RESOURCE TO A CARD WITH AT LEAST 1 RESOURCE ON IT | — |
| Generated text | Add 1 resource to a card with 1 or more resources on it. | — |

Pets declaration:

```pets
CLASS CeosFavoriteProject : EventCard {
  cost = 1
  This:: EventTag<This>
  This: CardResource<CardFront(HAS CardResource)>
}
```

### Commercial District

Class: `CommercialDistrict`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and increase your M€ production 4 steps. Place this tile. 1 VP PER ADJACENT CITY TILE. | — |
| Generated text | Decrease your energy production 1 step and increase your M€ production 4 steps. Place this tile. 1 VP per city tile in play next to this tile. | — |

Pets declaration:

```pets
CLASS CommercialDistrict : AutomatedCard {
  cost = 16
  This:: BuildingTag<This>
  This: PROD[-Energy, 4 MC], CommercialDistrict_SpecialTile<>
  End: VictoryPoint / Adjacency<CommercialDistrict_SpecialTile, CityTile<Anyone>>
}
```

### Corporate Stronghold

Class: `CorporateStronghold`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and increase your M€ production 3 steps. Place a city tile. | — |
| Generated text | Decrease your energy production 1 step and increase your M€ production 3 steps. Place a city tile. | — |

Pets declaration:

```pets
CLASS CorporateStronghold : AutomatedCard {
  cost = 11
  This:: CityTag<This>, BuildingTag<This>
  This: PROD[-Energy, 3 MC], CityTile<>
  End: -2 VictoryPoint
}
```

### Development Center

Class: `DevelopmentCenter`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 1 energy to draw a card. |
| Generated text | — | Action: Spend 1 energy to draw 1 card. |

Pets declaration:

```pets
CLASS DevelopmentCenter : ActionCard, ActiveCard {
  cost = 11
  This:: ScienceTag<This>, BuildingTag<This>
  Energy -> ProjectCard
}
```

### Earth Catapult

Class: `EarthCatapult`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: when you play a card, you pay 2 M€ less for it. |
| Generated text | — | Effect: When you play a card, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS EarthCatapult : ActiveCard {
  cost = 23
  This:: EarthTag<This>
  PayingFor<Class<CardFront>>:: -2 Owed
  End: 2 VictoryPoint
}
```

### Earth Office

Class: `EarthOffice`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you play an Earth tag, you pay 3 M€ less for it. |
| Generated text | — | Effect: When you play an Earth tag, you pay 3 M€ less for it. |

Pets declaration:

```pets
CLASS EarthOffice : ActiveCard {
  cost = 1
  This:: EarthTag<This>
  PayingFor<Class<EarthTag>>:: -3 Owed
}
```

### Electro Catapult

Class: `ElectroCatapult`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Oxygen must be 8% or less. Decrease your energy production 1 step. | Action: Spend 1 plant or 1 steel to gain 7 M€. |
| Generated text | Requires 8% oxygen or less. Decrease your energy production 1 step. | Action: Spend 1 plant to gain 7 M€, or spend 1 steel to gain 7 M€. |

Pets declaration:

```pets
CLASS ElectroCatapult : ActionCard, ActiveCard {
  cost = 17
  requirement = HAS "MAX 8 OxygenStep"
  This:: BuildingTag<This>
  This: PROD[-Energy]
  End: VictoryPoint
  Plant -> 7 MC
  Steel -> 7 MC
}
```

### Energy Tapping

Class: `EnergyTapping`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease any energy production 1 step and increase your own 1 step. | — |
| Generated text | Decrease any player's energy production 1 step and increase your energy production 1 step. | — |

Pets declaration:

```pets
CLASS EnergyTapping : AutomatedCard {
  cost = 3
  This:: PowerTag<This>
  This: PROD[-Energy<Anyone>, Energy]
  End: -VictoryPoint
}
```

### Fuel Factory

Class: `FuelFactory`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and increase your titanium and your M€ production 1 step each. | — |
| Generated text | Decrease your energy production 1 step and increase your titanium production and your M€ production 1 step each. | — |

Pets declaration:

```pets
CLASS FuelFactory : AutomatedCard {
  cost = 6
  This:: BuildingTag<This>
  This: PROD[-Energy, Titanium, MC]
}
```

### Gene Repair

Class: `GeneRepair`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 science tags. Increase your M€ production 2 steps. | — |
| Generated text | Requires 3 science tags. Increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS GeneRepair : AutomatedCard {
  cost = 12
  requirement = HAS "3 ScienceTag"
  This:: ScienceTag<This>
  This: PROD[2 MC]
  End: 2 VictoryPoint
}
```

### Great Escarpment Consortium

Class: `GreatEscarpmentConsortium`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have steel production. Decrease any steel production 1 step and increase your own 1 step. | — |
| Generated text | Requires that you have steel production. Decrease any player's steel production 1 step and increase your steel production 1 step. | — |

Pets declaration:

```pets
CLASS GreatEscarpmentConsortium : AutomatedCard {
  cost = 6
  requirement = HAS "PROD[Steel]"
  This: PROD[-Steel<Anyone>, Steel]
}
```

### Hackers

Class: `Hackers`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step and any M€ production 2 steps. Increase your M€ production 2 steps. | — |
| Generated text | Decrease your energy production 1 step and any player's M€ production 2 steps and increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS Hackers : AutomatedCard {
  cost = 3
  This: PROD[-Energy, -2 MC<Anyone>, 2 MC]
  End: -VictoryPoint
}
```

### Hired Raiders

Class: `HiredRaiders`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Steal up to 2 steel, or 3 M€ from any player. | — |
| Generated text | You may steal up to 2 steel or up to 3 M€ from any player. | — |

Pets declaration:

```pets
CLASS HiredRaiders : EventCard {
  cost = 1
  This:: EventTag<This>
  This: 2 Steel<Owner FROM Anyone>? OR 3 MC<Owner FROM Anyone>?
}
```

### Indentured Workers

Class: `IndenturedWorkers`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | The next card you play this generation costs 8 M€ less. | — |
| Generated text | When you play the next card this generation, you pay 8 M€ less for it. | — |

Pets declaration:

```pets
CLASS IndenturedWorkers : EventCard {
  cost = 0
  This:: EventTag<This>
  This: IndenturedWorkers_NextCardEffect
  End: -VictoryPoint
}
```

### Industrial Center

Class: `IndustrialCenter`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place this tile ADJACENT TO A CITY TILE. | Action: Spend 7 M€ to increase your steel production 1 step. |
| Generated text | Place this tile on a land area next to any city tile. | Action: Spend 7 M€ to increase your steel production 1 step. |

Pets declaration:

```pets
CLASS IndustrialCenter : ActionCard, ActiveCard {
  cost = 4
  This:: BuildingTag<This>
  This: IndustrialCenter_SpecialTile<LandArea(HAS Neighbor<CityTile<Anyone>>)>
  7 MC -> PROD[Steel]
}
```

### Interstellar Colony Ship

Class: `InterstellarColonyShip`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 5 science tags. | — |
| Generated text | Requires 5 science tags. | — |

Pets declaration:

```pets
CLASS InterstellarColonyShip : EventCard {
  cost = 24
  requirement = HAS "5 ScienceTag"
  This:: EarthTag<This>, SpaceTag<This>, EventTag<This>
  End: 4 VictoryPoint
}
```

### Invention Contest

Class: `InventionContest`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | LOOK AT THE TOP 3 CARDS FROM THE DECK. TAKE 1 OF THEM INTO HAND AND DISCARD THE OTHER 2 | — |
| Generated text | Look at 3 project cards. Draw one of them. | — |

Pets declaration:

```pets
CLASS InventionContest : EventCard {
  cost = 2
  This:: ScienceTag<This>, EventTag<This>
  This: ProjectCard
}
```

### Inventors' Guild

Class: `InventorsGuild`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: LOOK AT THE TOP CARD AND EITHER BUY IT OR DISCARD IT |
| Generated text | — | Action: Look at 1 project card. You may buy it. |

Pets declaration:

```pets
CLASS InventorsGuild : ActionCard, ActiveCard {
  cost = 9
  This:: ScienceTag<This>
  -> BuyCard?
}
```

### Investment Loan

Class: `InvestmentLoan`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 1 step. Gain 10 M€. | — |
| Generated text | Decrease your M€ production 1 step. Gain 10 M€. | — |

Pets declaration:

```pets
CLASS InvestmentLoan : EventCard {
  cost = 3
  This:: EarthTag<This>, EventTag<This>
  This: PROD[-MC], 10 MC
}
```

### Io Mining Industries

Class: `IoMiningIndustries`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 2 steps and your M€ production 2 steps. 1 VP per Jovian tag you have. | — |
| Generated text | Increase your titanium production and your M€ production 2 steps each. 1 VP per Jovian tag you have. | — |

Pets declaration:

```pets
CLASS IoMiningIndustries : AutomatedCard {
  cost = 41
  This:: JovianTag<This>, SpaceTag<This>
  This: PROD[2 Titanium, 2 MC]
  End: VictoryPoint / JovianTag
}
```

### Lagrange Observatory

Class: `LagrangeObservatory`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 1 card. | — |
| Generated text | Draw 1 card. | — |

Pets declaration:

```pets
CLASS LagrangeObservatory : AutomatedCard {
  cost = 9
  This:: ScienceTag<This>, SpaceTag<This>
  This: ProjectCard
  End: VictoryPoint
}
```

### Land Claim

Class: `LandClaim`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | PLACE YOUR MARKER ON A NON-RESERVED AREA. ONLY YOU MAY PLACE A TILE HERE | — |
| Generated text | Place a community marker on a land area with no occupant. | — |

Pets declaration:

```pets
CLASS LandClaim : EventCard {
  cost = 1
  This:: EventTag<This>
  This: Community<LandArea(HAS MAX 0 Occupant)>
}
```

### Lightning Harvest

Class: `LightningHarvest`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 science tags. Increase your energy production and your M€ production 1 step each. | — |
| Generated text | Requires 3 science tags. Increase your energy production and your M€ production 1 step each. | — |

Pets declaration:

```pets
CLASS LightningHarvest : AutomatedCard {
  cost = 8
  requirement = HAS "3 ScienceTag"
  This:: PowerTag<This>
  This: PROD[Energy, MC]
  End: VictoryPoint
}
```

### Mars University

Class: `MarsUniversity`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you play a science tag, including this, you may discard a card from hand to draw a card. |
| Generated text | — | Effect: When you play a science tag (including this), you may discard 1 card to draw 1 card. |

Pets declaration:

```pets
CLASS MarsUniversity : ActiveCard {
  cost = 8
  This:: ScienceTag<This>, BuildingTag<This>
  ScienceTag: (-ProjectCard THEN ProjectCard) OR Ok
  End: VictoryPoint
}
```

### Mass Converter

Class: `MassConverter`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 5 science tags. Increase your energy production 6 steps. | Effect: When you play a space card, you pay 2 M€ less for it. |
| Generated text | Requires 5 science tags. Increase your energy production 6 steps. | Effect: When you play a space tag, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS MassConverter : ActiveCard {
  cost = 8
  requirement = HAS "5 ScienceTag"
  This:: ScienceTag<This>, PowerTag<This>
  This: PROD[6 Energy]
  PayingFor<Class<SpaceTag>>:: -2 Owed
}
```

### Media Archives

Class: `MediaArchives`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 1 M€ for each event EVER PLAYED by all players. | — |
| Generated text | Gain 1 M€ per card in all players' event piles. | — |

Pets declaration:

```pets
CLASS MediaArchives : AutomatedCard {
  cost = 8
  This:: EarthTag<This>
  This: MC / PlayedEvent<Anyone>
}
```

### Media Group

Class: `MediaGroup`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: After you play an event card, you gain 3 M€. |
| Generated text | — | Effect: When you play an event card, gain 3 M€. |

Pets declaration:

```pets
CLASS MediaGroup : ActiveCard {
  cost = 6
  This:: EarthTag<This>
  EventCard: 3 MC
}
```

### Medical Lab

Class: `MedicalLab`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step for every 2 building tags you have, including this. | — |
| Generated text | Increase your M€ production 1 step per 2 building tags you have (including this). | — |

Pets declaration:

```pets
CLASS MedicalLab : AutomatedCard {
  cost = 13
  This:: ScienceTag<This>, BuildingTag<This>
  This: PROD[MC / 2 BuildingTag]
  End: VictoryPoint
}
```

### Mine

Class: `Mine`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your steel production 1 step. | — |
| Generated text | Increase your steel production 1 step. | — |

Pets declaration:

```pets
CLASS Mine : AutomatedCard {
  cost = 4
  This:: BuildingTag<This>
  This: PROD[Steel]
}
```

### Mineral Deposit

Class: `MineralDeposit`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 5 steel. | — |
| Generated text | Gain 5 steel. | — |

Pets declaration:

```pets
CLASS MineralDeposit : EventCard {
  cost = 5
  This:: EventTag<This>
  This: 5 Steel
}
```

### Mining Area

Class: `MiningArea`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place this tile on an area with a steel or titanium placement bonus, adjacent to another of your tiles. Increase your production of that resource 1 step. | — |
| Generated text | Place this tile on a land area next to a tile you own, then if this tile is on a land area with a steel placement bonus, increase your steel production 1 step, or if this tile is on a land area with a titanium placement bonus, increase your titanium production 1 step. | — |

Pets declaration:

```pets
CLASS MiningArea : AutomatedCard {
  cost = 4
  This:: BuildingTag<This>
  This: MiningArea_SpecialTile<LandArea(HAS Neighbor<OwnedTile>)> THEN PROD[(LandArea(HAS MiningArea_SpecialTile, HAS PlacementBonus<Class<Steel>>): Steel) OR (LandArea(HAS MiningArea_SpecialTile, HAS PlacementBonus<Class<Titanium>>): Titanium)]
}
```

### Miranda Resort

Class: `MirandaResort`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step for each Earth tag you have. | — |
| Generated text | Increase your M€ production 1 step per Earth tag you have. | — |

Pets declaration:

```pets
CLASS MirandaResort : AutomatedCard {
  cost = 12
  This:: JovianTag<This>, SpaceTag<This>
  This: PROD[MC / EarthTag]
  End: VictoryPoint
}
```

### Olympus Conference

Class: `OlympusConference`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | When you play a science tag, including this, either add a science resource to this card, or remove a science resource from this card to draw a card. |
| Generated text | — | Effect: When you play a science tag (including this), add 1 science resource to this card or remove 1 science resource from this card to draw 1 card. |

Pets declaration:

```pets
CLASS OlympusConference : ActiveCard, ResourceCard<Class<Science>> {
  cost = 10
  This:: ScienceTag<This>, EarthTag<This>, BuildingTag<This>
  ScienceTag: Science<This> OR (ProjectCard FROM Science<This>)
  End: VictoryPoint
}
```

### Physics Complex

Class: `PhysicsComplex`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | 2 VP for each science resource on this card. | Action: Spend 6 energy to add a science resource to this card. |
| Generated text | 2 VPs per science resource on this card. | Action: Spend 6 energy to add 1 science resource to this card. |

Pets declaration:

```pets
CLASS PhysicsComplex : ActionCard, ActiveCard, ResourceCard<Class<Science>> {
  cost = 12
  This:: ScienceTag<This>, BuildingTag<This>
  End: 2 VictoryPoint / Science<This>
  6 Energy -> Science<This>
}
```

### Power Infrastructure

Class: `PowerInfrastructure`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend any amount of energy to gain that amount of M€. |
| Generated text | — | Action: Spend 1 or more energy to gain that amount of M€. |

Pets declaration:

```pets
CLASS PowerInfrastructure : ActionCard, ActiveCard {
  cost = 4
  This:: PowerTag<This>, BuildingTag<This>
  X Energy -> X MC
}
```

### Power Supply Consortium

Class: `PowerSupplyConsortium`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 power tags. Decrease any energy production 1 step and increase your own 1 step. | — |
| Generated text | Requires 2 power tags. Decrease any player's energy production 1 step and increase your energy production 1 step. | — |

Pets declaration:

```pets
CLASS PowerSupplyConsortium : AutomatedCard {
  cost = 5
  requirement = HAS "2 PowerTag"
  This:: PowerTag<This>
  This: PROD[-Energy<Anyone>, Energy]
}
```

### Protected Habitats

Class: `ProtectedHabitats`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: OPPONENTS MAY NOT REMOVE YOUR (Plant) (Animal) (Microbe) |
| Generated text | — | Effect: Opponents may not remove your plants, animals, or microbes. |

Pets declaration:

```pets
CLASS ProtectedHabitats : ActiveCard {
  cost = 5
  -Plant OR -Animal OR -Microbe BY Player(NOT Owner):: Die
}
```

### Quantum Extractor

Class: `QuantumExtractor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4 science tags. Increase your energy production 4 steps. | Effect: When you play a space card, you pay 2 M€ less for it. |
| Generated text | Requires 4 science tags. Increase your energy production 4 steps. | Effect: When you play a space tag, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS QuantumExtractor : ActiveCard {
  cost = 13
  requirement = HAS "4 ScienceTag"
  This:: ScienceTag<This>, PowerTag<This>
  This: PROD[4 Energy]
  PayingFor<Class<SpaceTag>>:: -2 Owed
}
```

### Rad-Suits

Class: `RadSuits`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 cities in play. Increase your M€ production 1 step. | — |
| Generated text | Requires 2 city tiles in play. Increase your M€ production 1 step. | — |

Pets declaration:

```pets
CLASS RadSuits : AutomatedCard {
  cost = 6
  requirement = HAS "2 CityTile<Anyone>"
  This: PROD[MC]
  End: VictoryPoint
}
```

### Research

Class: `Research`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Counts as playing 2 science cards. Draw 2 cards. | — |
| Generated text | Draw 2 cards. | — |

Pets declaration:

```pets
CLASS Research : AutomatedCard {
  cost = 11
  This:: 2 ScienceTag<This>
  This: 2 ProjectCard
  End: VictoryPoint
}
```

### Restricted Area

Class: `RestrictedArea`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place this tile. | Action: Spend 2 M€ to draw a card. |
| Generated text | Place this tile. | Action: Spend 2 M€ to draw 1 card. |

Pets declaration:

```pets
CLASS RestrictedArea : ActionCard, ActiveCard {
  cost = 11
  This:: ScienceTag<This>
  This: RestrictedArea_SpecialTile<>
  2 MC -> ProjectCard
}
```

### Robotic Workforce

Class: `RoboticWorkforce`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Duplicate only the production box of one of your building cards. | — |
| Generated text | Copy the immediate production box of a building card. | — |

Pets declaration:

```pets
CLASS RoboticWorkforce : AutomatedCard {
  cost = 9
  This:: ScienceTag<This>
  This: CopyProductionBox<CardFront(HAS BuildingTag)>
}
```

### Sabotage

Class: `Sabotage`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Remove up to 3 titanium from any player, or 4 steel, or 7 M€. | — |
| Generated text | You may remove up to 3 titanium, up to 4 steel, or up to 7 M€ from any player. | — |

Pets declaration:

```pets
CLASS Sabotage : EventCard {
  cost = 1
  This:: EventTag<This>
  This: -3 Titanium<Anyone>? OR -4 Steel<Anyone>? OR -7 MC<Anyone>?
}
```

### Satellites

Class: `Satellites`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step for each space tag you have, including this. | — |
| Generated text | Increase your M€ production 1 step per space tag you have (including this). | — |

Pets declaration:

```pets
CLASS Satellites : AutomatedCard {
  cost = 10
  This:: SpaceTag<This>
  This: PROD[MC / SpaceTag]
}
```

### Security Fleet

Class: `SecurityFleet`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | 1 VP for each fighter resource on this card. | Action: Spend 1 titanium to add 1 fighter resource to this card. |
| Generated text | 1 VP per fighter resource on this card. | Action: Spend 1 titanium to add 1 fighter resource to this card. |

Pets declaration:

```pets
CLASS SecurityFleet : ActionCard, ActiveCard, ResourceCard<Class<Fighter>> {
  cost = 12
  This:: SpaceTag<This>
  End: VictoryPoint / Fighter<This>
  Titanium -> Fighter<This>
}
```

### Space Elevator

Class: `SpaceElevator`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 1 step. | Action: Spend 1 steel to gain 5 M€. |
| Generated text | Increase your titanium production 1 step. | Action: Spend 1 steel to gain 5 M€. |

Pets declaration:

```pets
CLASS SpaceElevator : ActionCard, ActiveCard {
  cost = 27
  This:: SpaceTag<This>, BuildingTag<This>
  This: PROD[Titanium]
  End: 2 VictoryPoint
  Steel -> 5 MC
}
```

### Space Station

Class: `SpaceStation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you play a space card, you pay 2 M€ less for it. |
| Generated text | — | Effect: When you play a space tag, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS SpaceStation : ActiveCard {
  cost = 10
  This:: SpaceTag<This>
  PayingFor<Class<SpaceTag>>:: -2 Owed
  End: VictoryPoint
}
```

### Sponsors

Class: `Sponsors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 2 steps. | — |
| Generated text | Increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS Sponsors : AutomatedCard {
  cost = 6
  This:: EarthTag<This>
  This: PROD[2 MC]
}
```

### Standard Technology

Class: `StandardTechnology`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: After you pay for a standard project, except selling patents, you gain 3 M€. |
| Generated text | — | Effect: When you pay for a standard project with a positive printed cost, gain 3 M€. |

Pets declaration:

```pets
CLASS StandardTechnology : ActiveCard {
  cost = 6
  This:: ScienceTag<This>
  -ActionBilling<StandardProject(HAS cost)>: 3 MC
}
```

### Tardigrades

Class: `Tardigrades`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | 1 VP per 4 microbes on this card. | Action: Add 1 microbe to this card. |
| Generated text | 1 VP per 4 microbes on this card. | Action: Add 1 microbe to this card. |

Pets declaration:

```pets
CLASS Tardigrades : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 4
  This:: MicrobeTag<This>
  End: VictoryPoint / 4 Microbe<This>
  -> Microbe<This>
}
```

### Technology Demonstration

Class: `TechnologyDemonstration`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 2 cards. | — |
| Generated text | Draw 2 cards. | — |

Pets declaration:

```pets
CLASS TechnologyDemonstration : EventCard {
  cost = 5
  This:: ScienceTag<This>, SpaceTag<This>, EventTag<This>
  This: 2 ProjectCard
}
```

### Terraforming Ganymede

Class: `TerraformingGanymede`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 1 step for each Jovian tag you have, including this. | — |
| Generated text | Raise your terraform rating 1 step per Jovian tag you have (including this). | — |

Pets declaration:

```pets
CLASS TerraformingGanymede : AutomatedCard {
  cost = 33
  This:: JovianTag<This>, SpaceTag<This>
  This: TerraformRating / JovianTag
  End: 2 VictoryPoint
}
```

### Titanium Mine

Class: `TitaniumMine`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 1 step. | — |
| Generated text | Increase your titanium production 1 step. | — |

Pets declaration:

```pets
CLASS TitaniumMine : AutomatedCard {
  cost = 7
  This:: BuildingTag<This>
  This: PROD[Titanium]
}
```

### Toll Station

Class: `TollStation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step for each space tag your OPPONENTS have. | — |
| Generated text | Increase your M€ production 1 step per space tag your opponents have. | — |

Pets declaration:

```pets
CLASS TollStation : AutomatedCard {
  cost = 12
  This:: SpaceTag<This>
  This: PROD[MC / SpaceTag<Player(NOT Owner)>]
}
```

### Trans-Neptune Probe

Class: `TransNeptuneProbe`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | — |
| Generated text | — | — |

Pets declaration:

```pets
CLASS TransNeptuneProbe : AutomatedCard {
  cost = 6
  This:: ScienceTag<This>, SpaceTag<This>
  End: VictoryPoint
}
```

### Tropical Resort

Class: `TropicalResort`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your heat production 2 steps and increase your M€ production 3 steps. | — |
| Generated text | Decrease your heat production 2 steps and increase your M€ production 3 steps. | — |

Pets declaration:

```pets
CLASS TropicalResort : AutomatedCard {
  cost = 13
  This:: BuildingTag<This>
  This: PROD[-2 Heat, 3 MC]
  End: 2 VictoryPoint
}
```

### Vesta Shipyard

Class: `VestaShipyard`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 1 step. | — |
| Generated text | Increase your titanium production 1 step. | — |

Pets declaration:

```pets
CLASS VestaShipyard : AutomatedCard {
  cost = 15
  This:: JovianTag<This>, SpaceTag<This>
  This: PROD[Titanium]
  End: VictoryPoint
}
```

### Viral Enhancers

Class: `ViralEnhancers`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you play a plant, microbe, or an animal tag, including this, gain 1 plant or add 1 resource TO THAT CARD. |
| Generated text | — | Effect: When you play a bio tag (including this), gain 1 plant or add 1 resource to that card. |

Pets declaration:

```pets
CLASS ViralEnhancers : ActiveCard {
  cost = 9
  This:: ScienceTag<This>, MicrobeTag<This>
  BioTag<@CardFront>: Plant OR CardResource<@CardFront>!
}
```

### Virus

Class: `Virus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Remove up to 2 animals or 5 plants from any player. | — |
| Generated text | You may remove up to 2 animals or up to 5 plants from any player. | — |

Pets declaration:

```pets
CLASS Virus : EventCard {
  cost = 1
  This:: MicrobeTag<This>, EventTag<This>
  This: -2 Animal<Anyone>? OR -5 Plant<Anyone>?
}
```

## Venus Next Expansion

### Aerial Mappers

Class: `AerialMappers`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Add 1 floater to ANY card, or spend 1 floater here to draw a card. |
| Generated text | — | Action: Add 1 floater to any card, or spend 1 floater from this card to draw 1 card. |

Pets declaration:

```pets
CLASS AerialMappers : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 11
  This:: VenusTag<This>
  End: VictoryPoint
  -> Floater
  Floater<This> -> ProjectCard
}
```

### Aerosport Tournament

Class: `AerosportTournament`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have 5 floaters. Gain 1 M€ for each city tile in play. | — |
| Generated text | Requires that you have 5 floaters. Gain 1 M€ per city tile in play. | — |

Pets declaration:

```pets
CLASS AerosportTournament : EventCard {
  cost = 7
  requirement = HAS "5 Floater"
  This:: EventTag<This>
  This: MC / CityTile<Anyone>
  End: VictoryPoint
}
```

### Air-Scrapping Expedition

Class: `AirScrappingExpedition`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 1 step. Add 3 floaters to ANY VENUS CARD. | — |
| Generated text | Raise Venus 1 step. Add 3 floaters to a Venus card. | — |

Pets declaration:

```pets
CLASS AirScrappingExpedition : EventCard {
  cost = 13
  This:: VenusTag<This>, EventTag<This>
  This: VenusStep, 3 Floater<CardFront(HAS VenusTag)>
}
```

### Atalanta Planitia Lab

Class: `AtalantaPlanitiaLab`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 science tags. Draw 2 cards. | — |
| Generated text | Requires 3 science tags. Draw 2 cards. | — |

Pets declaration:

```pets
CLASS AtalantaPlanitiaLab : AutomatedCard {
  cost = 10
  requirement = HAS "3 ScienceTag"
  This:: VenusTag<This>, ScienceTag<This>
  This: 2 ProjectCard
  End: 2 VictoryPoint
}
```

### Atmoscoop

Class: `Atmoscoop`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 science tags. Either raise the temperature 2 steps, or raise Venus 2 steps. Add 2 floaters to ANY card. | — |
| Generated text | Requires 3 science tags. Raise temperature 2 steps or Venus 2 steps. Add 2 floaters to another card. | — |

Pets declaration:

```pets
CLASS Atmoscoop : AutomatedCard {
  cost = 22
  requirement = HAS "3 ScienceTag"
  This:: JovianTag<This>, SpaceTag<This>
  This: 2 TemperatureStep OR 2 VenusStep, 2 Floater
  End: VictoryPoint
}
```

### Comet For Venus

Class: `CometForVenus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 1 step. Remove up to 4 M€ from a player WITH A VENUS TAG IN PLAY. | — |
| Generated text | Raise Venus 1 step. You may remove up to 4 M€ from a player with a Venus tag in play. | — |

Pets declaration:

```pets
CLASS CometForVenus : EventCard {
  cost = 11
  This:: SpaceTag<This>, EventTag<This>
  This: VenusStep, -4 MC<Anyone(HAS VenusTag)>?
}
```

### Corroder Suits

Class: `CorroderSuits`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 2 steps. Add 1 resource to ANY VENUS CARD. | — |
| Generated text | Increase your M€ production 2 steps. Add 1 resource to a Venus card. | — |

Pets declaration:

```pets
CLASS CorroderSuits : AutomatedCard {
  cost = 8
  This:: VenusTag<This>
  This: PROD[2 MC], CardResource<CardFront(HAS VenusTag)>
}
```

### Dawn City

Class: `DawnCity`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4 science tags. Decrease your energy production 1 step. Increase your titanium production 1 step. Place a city tile ON THE RESERVED AREA. | — |
| Generated text | Requires 4 science tags. Decrease your energy production 1 step and increase your titanium production 1 step. Place a city tile on the reserved area outside Mars. | — |

Pets declaration:

```pets
CLASS DawnCity : AutomatedCard {
  cost = 15
  requirement = HAS "4 ScienceTag"
  This:: SpaceTag<This>, CityTag<This>
  This: PROD[-Energy, Titanium], CityTile<DawnCity_RemoteArea>
  End: 3 VictoryPoint
}
```

### Deuterium Export

Class: `DeuteriumExport`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Add 1 floater to this card, or spend 1 floater here to increase your energy production 1 step. |
| Generated text | — | Action: Add 1 floater to this card, or spend 1 floater from this card to increase your energy production 1 step. |

Pets declaration:

```pets
CLASS DeuteriumExport : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 11
  This:: VenusTag<This>, PowerTag<This>, SpaceTag<This>
  -> Floater<This>
  Floater<This> -> PROD[Energy]
}
```

### Dirigibles

Class: `Dirigibles`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Add 1 floater to ANY card. Effect: When playing a Venus tag, floaters here may be used as payment, and are worth 3 M€ each. |
| Generated text | — | Action: Add 1 floater to any card. / Effect: When you play a Venus tag, floaters on this card may be used as 3 M€ each. |

Pets declaration:

```pets
CLASS Dirigibles : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 11
  This:: VenusTag<This>
  PayingFor<Class<VenusTag>>:: AcceptingFromCard<This>
  PayFromCard<This>:: -3 Owed
  -> Floater
}
```

### Extractor Balloons

Class: `ExtractorBalloons`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 3 floaters to this card. | Action: Add 1 floater to this card, or remove 2 floaters here to raise Venus 1 step. |
| Generated text | Add 3 floaters to this card. | Action: Add 1 floater to this card, or spend 2 floaters from this card to raise Venus 1 step. |

Pets declaration:

```pets
CLASS ExtractorBalloons : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 21
  This:: VenusTag<This>
  This: 3 Floater<This>
  -> Floater<This>
  2 Floater<This> -> VenusStep
}
```

### Extremophiles

Class: `Extremophiles`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 science tags. 1 VP per 3 microbes on this card. | Action: Add 1 microbe to ANY card. |
| Generated text | Requires 2 science tags. 1 VP per 3 microbes on this card. | Action: Add 1 microbe to any card. |

Pets declaration:

```pets
CLASS Extremophiles : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 3
  requirement = HAS "2 ScienceTag"
  This:: VenusTag<This>, MicrobeTag<This>
  End: VictoryPoint / 3 Microbe<This>
  -> Microbe
}
```

### Floating Habs

Class: `FloatingHabs`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 science tags. 1 VP per 2 floaters on this card. | Action: Spend 2 M€ to add 1 floater to ANY card. |
| Generated text | Requires 2 science tags. 1 VP per 2 floaters on this card. | Action: Spend 2 M€ to add 1 floater to any card. |

Pets declaration:

```pets
CLASS FloatingHabs : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 5
  requirement = HAS "2 ScienceTag"
  This:: VenusTag<This>
  End: VictoryPoint / 2 Floater<This>
  2 MC -> Floater
}
```

### Forced Precipitation

Class: `ForcedPrecipitation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 2 M€ to add a floater to this card, or spend 2 floaters here to increase Venus 1 step. |
| Generated text | — | Action: Spend 2 M€ to add 1 floater to this card, or spend 2 floaters from this card to raise Venus 1 step. |

Pets declaration:

```pets
CLASS ForcedPrecipitation : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 8
  This:: VenusTag<This>
  2 MC -> Floater<This>
  2 Floater<This> -> VenusStep
}
```

### Freyja Biodomes

Class: `FreyjaBiodomes`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 10%. Add 2 microbes or 2 animals TO ANOTHER VENUS CARD. Decrease your energy production 1 step, and increase your M€ production 2 steps. | — |
| Generated text | Requires Venus 10%. Add 2 microbes or 2 animals to a Venus card. Decrease your energy production 1 step and increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS FreyjaBiodomes : AutomatedCard {
  cost = 14
  requirement = HAS "5 VenusStep"
  This:: VenusTag<This>, PlantTag<This>
  This: 2 Microbe<CardFront(HAS VenusTag)> OR 2 Animal<CardFront(HAS VenusTag)>, PROD[-Energy, 2 MC]
  End: 2 VictoryPoint
}
```

### GHG Import From Venus

Class: `GhgImportFromVenus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 1 step. Increase your heat production 3 steps. | — |
| Generated text | Raise Venus 1 step. Increase your heat production 3 steps. | — |

Pets declaration:

```pets
CLASS GhgImportFromVenus : EventCard {
  cost = 23
  This:: VenusTag<This>, SpaceTag<This>, EventTag<This>
  This: VenusStep, PROD[3 Heat]
}
```

### Giant Solar Shade

Class: `GiantSolarShade`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 3 steps. | — |
| Generated text | Raise Venus 3 steps. | — |

Pets declaration:

```pets
CLASS GiantSolarShade : AutomatedCard {
  cost = 27
  This:: VenusTag<This>, SpaceTag<This>
  This: 3 VenusStep
}
```

### Gyropolis

Class: `Gyropolis`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 2 steps. Increase your M€ production 1 step for each Venus and Earth tag you have. Place a city tile. | — |
| Generated text | Decrease your energy production 2 steps. Increase your M€ production 1 step per Venus tag you have. Increase your M€ production 1 step per Earth tag you have. Place a city tile. | — |

Pets declaration:

```pets
CLASS Gyropolis : AutomatedCard {
  cost = 20
  This:: CityTag<This>, BuildingTag<This>
  This: PROD[-2 Energy, MC / VenusTag, MC / EarthTag], CityTile<>
}
```

### Hydrogen To Venus

Class: `HydrogenToVenus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 1 step. Add 1 floater to A VENUS CARD for each Jovian tag you have. | — |
| Generated text | Raise Venus 1 step. Add 1 floater to a Venus card per Jovian tag you have. | — |

Pets declaration:

```pets
CLASS HydrogenToVenus : EventCard {
  cost = 11
  This:: SpaceTag<This>, EventTag<This>
  This: VenusStep, Floater<CardFront(HAS VenusTag)> / JovianTag
}
```

### Io Sulphur Research

Class: `IoSulphurResearch`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 1 card, or draw 3 cards if you have at least 3 Venus tags. | — |
| Generated text | Draw 1 card, or if you have 3 Venus tags, draw 3 cards. | — |

Pets declaration:

```pets
CLASS IoSulphurResearch : AutomatedCard {
  cost = 17
  This:: ScienceTag<This>, JovianTag<This>
  This: ProjectCard OR (3 VenusTag: 3 ProjectCard)
  End: 2 VictoryPoint
}
```

### Ishtar Mining

Class: `IshtarMining`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 8%. Increase your titanium production 1 step. | — |
| Generated text | Requires Venus 8%. Increase your titanium production 1 step. | — |

Pets declaration:

```pets
CLASS IshtarMining : AutomatedCard {
  cost = 5
  requirement = HAS "4 VenusStep"
  This:: VenusTag<This>
  This: PROD[Titanium]
}
```

### Jet Stream Microscrappers

Class: `JetStreamMicroscrappers`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 1 titanium to add 2 floaters to this card, or remove 2 floaters here to raise Venus 1 step. |
| Generated text | — | Action: Spend 1 titanium to add 2 floaters to this card, or spend 2 floaters from this card to raise Venus 1 step. |

Pets declaration:

```pets
CLASS JetStreamMicroscrappers : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 12
  This:: VenusTag<This>
  Titanium -> 2 Floater<This>
  2 Floater<This> -> VenusStep
}
```

### Local Shading

Class: `LocalShading`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Add 1 floater to this card, or spend 1 floater here to raise your M€ production 1 step. |
| Generated text | — | Action: Add 1 floater to this card, or spend 1 floater from this card to increase your M€ production 1 step. |

Pets declaration:

```pets
CLASS LocalShading : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 4
  This:: VenusTag<This>
  -> Floater<This>
  Floater<This> -> PROD[MC]
}
```

### Luna Metropolis

Class: `LunaMetropolis`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step for each Earth tag you have, including this. Place a city tile ON THE RESERVED AREA. | — |
| Generated text | Increase your M€ production 1 step per Earth tag you have (including this). Place a city tile on the reserved area outside Mars. | — |

Pets declaration:

```pets
CLASS LunaMetropolis : AutomatedCard {
  cost = 21
  This:: SpaceTag<This>, EarthTag<This>, CityTag<This>
  This: PROD[MC / EarthTag], CityTile<LunaMetropolis_RemoteArea>
  End: 2 VictoryPoint
}
```

### Luxury Foods

Class: `LuxuryFoods`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus, Earth and Jovian tags. | — |
| Generated text | Requires a Venus tag, an Earth tag, and a Jovian tag. | — |

Pets declaration:

```pets
CLASS LuxuryFoods : AutomatedCard {
  cost = 8
  requirement = HAS "VenusTag, EarthTag, JovianTag"
  End: 2 VictoryPoint
}
```

### Maxwell Base

Class: `MaxwellBase`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 12%. Decrease your energy production 1 step. Place a city tile ON THE RESERVED AREA. | Action: Add 1 resource to ANOTHER VENUS CARD. |
| Generated text | Requires Venus 12%. Decrease your energy production 1 step. Place a city tile on the reserved area outside Mars. | Action: Add 1 resource to a Venus card. |

Pets declaration:

```pets
CLASS MaxwellBase : ActionCard, ActiveCard {
  cost = 18
  requirement = HAS "6 VenusStep"
  This:: VenusTag<This>, CityTag<This>
  This: PROD[-Energy], CityTile<MaxwellBase_RemoteArea>
  End: 3 VictoryPoint
  -> CardResource<CardFront(HAS VenusTag)>
}
```

### Mining Quota

Class: `MiningQuota`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus, Earth and Jovian tags. Increase your steel production 2 steps. | — |
| Generated text | Requires a Venus tag, an Earth tag, and a Jovian tag. Increase your steel production 2 steps. | — |

Pets declaration:

```pets
CLASS MiningQuota : AutomatedCard {
  cost = 5
  requirement = HAS "VenusTag, EarthTag, JovianTag"
  This:: BuildingTag<This>
  This: PROD[2 Steel]
}
```

### Neutralizer Factory

Class: `NeutralizerFactory`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 10%. Increase Venus 1 step. | — |
| Generated text | Requires Venus 10%. Raise Venus 1 step. | — |

Pets declaration:

```pets
CLASS NeutralizerFactory : AutomatedCard {
  cost = 7
  requirement = HAS "5 VenusStep"
  This:: VenusTag<This>
  This: VenusStep
}
```

### Omnicourt

Class: `Omnicourt`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus, Earth, and Jovian tags. Increase your TR 2 steps. | — |
| Generated text | Requires a Venus tag, an Earth tag, and a Jovian tag. Raise your terraform rating 2 steps. | — |

Pets declaration:

```pets
CLASS Omnicourt : AutomatedCard {
  cost = 11
  requirement = HAS "VenusTag, EarthTag, JovianTag"
  This:: BuildingTag<This>
  This: 2 TerraformRating
}
```

### Orbital Reflectors

Class: `OrbitalReflectors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 2 steps. Increase your heat production 2 steps. | — |
| Generated text | Raise Venus 2 steps. Increase your heat production 2 steps. | — |

Pets declaration:

```pets
CLASS OrbitalReflectors : AutomatedCard {
  cost = 26
  This:: VenusTag<This>, SpaceTag<This>
  This: 2 VenusStep, PROD[2 Heat]
}
```

### Rotator Impacts

Class: `RotatorImpacts`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Venus must be 14% or lower. | Action: Spend 6 M€ to add an asteroid resource to this card (TITANIUM MAY BE USED), or spend a resource from this card to increase Venus 1 step. |
| Generated text | Requires Venus 14% or lower. | Action: Spend 6 M€ (titanium may be used) to add 1 asteroid to this card, or spend 1 asteroid from this card to raise Venus 1 step. |

Pets declaration:

```pets
CLASS RotatorImpacts : ActionCard, ActiveCard, ResourceCard<Class<Asteroid>> {
  cost = 6
  requirement = HAS "MAX 7 VenusStep"
  This:: SpaceTag<This>
  UseAction<This, Action1>:: Accepting<Class<Titanium>>
  6 MC -> Asteroid<This>
  Asteroid<This> -> VenusStep
}
```

### Sister Planet Support

Class: `SisterPlanetSupport`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus and Earth tag. Increase your M€ production 3 steps. | — |
| Generated text | Requires a Venus tag and an Earth tag. Increase your M€ production 3 steps. | — |

Pets declaration:

```pets
CLASS SisterPlanetSupport : AutomatedCard {
  cost = 7
  requirement = HAS "VenusTag, EarthTag"
  This:: VenusTag<This>, EarthTag<This>
  This: PROD[3 MC]
}
```

### Solarnet

Class: `Solarnet`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus, Earth, and Jovian tags. Draw 2 cards. | — |
| Generated text | Requires a Venus tag, an Earth tag, and a Jovian tag. Draw 2 cards. | — |

Pets declaration:

```pets
CLASS Solarnet : AutomatedCard {
  cost = 7
  requirement = HAS "VenusTag, EarthTag, JovianTag"
  This: 2 ProjectCard
  End: VictoryPoint
}
```

### Spin-Inducing Asteroid

Class: `SpinInducingAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Venus must be 10% or lower. Raise Venus 2 steps. | — |
| Generated text | Requires Venus 10% or lower. Raise Venus 2 steps. | — |

Pets declaration:

```pets
CLASS SpinInducingAsteroid : EventCard {
  cost = 16
  requirement = HAS "MAX 5 VenusStep"
  This:: SpaceTag<This>, EventTag<This>
  This: 2 VenusStep
}
```

### Sponsored Academies

Class: `SponsoredAcademies`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Discard 1 card from hand and THEN draw 3 cards. All OPPONENTS draw 1 card. | — |
| Generated text | Discard 1 card to draw 3 cards. Each other player draws 1 card. | — |

Pets declaration:

```pets
CLASS SponsoredAcademies : AutomatedCard {
  cost = 9
  This:: ScienceTag<This>, EarthTag<This>
  This: -ProjectCard THEN 3 ProjectCard, EACH Player(NOT Owner) { ProjectCard }
  End: VictoryPoint
}
```

### Stratopolis

Class: `Stratopolis`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 science tags. Increase your M€ production 2 steps. Place a city tile on THE RESERVED AREA. 1 VP per 3 floaters on this card. | Action: Add 2 floaters to ANY VENUS CARD. |
| Generated text | Requires 2 science tags. Increase your M€ production 2 steps. Place a city tile on the reserved area outside Mars. 1 VP per 3 floaters on this card. | Action: Add 2 floaters to a Venus card. |

Pets declaration:

```pets
CLASS Stratopolis : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 22
  requirement = HAS "2 ScienceTag"
  This:: VenusTag<This>, CityTag<This>
  This: PROD[2 MC], CityTile<Stratopolis_RemoteArea>
  End: VictoryPoint / 3 Floater<This>
  -> 2 Floater<CardFront(HAS VenusTag)>
}
```

### Stratospheric Birds

Class: `StratosphericBirds`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 12%, and that you spend 1 floater from any card. 1 VP for each animal on this card. | Action: Add 1 animal to this card. |
| Generated text | Requires Venus 12%. Remove 1 floater from any card. 1 VP per animal on this card. | Action: Add 1 animal to this card. |

Pets declaration:

```pets
CLASS StratosphericBirds : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 12
  requirement = HAS "6 VenusStep"
  This:: VenusTag<This>, AnimalTag<This>
  This: -Floater
  End: VictoryPoint / Animal<This>
  -> Animal<This>
}
```

### Sulphur-Eating Bacteria

Class: `SulphurEatingBacteria`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 6%. | Action: Add 1 microbe to this card, or spend any number of microbes here to gain the triple amount of M€. |
| Generated text | Requires Venus 6%. | Action: Add 1 microbe to this card, or spend 1 or more microbes from this card to gain triple that amount of M€. |

Pets declaration:

```pets
CLASS SulphurEatingBacteria : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 6
  requirement = HAS "3 VenusStep"
  This:: VenusTag<This>, MicrobeTag<This>
  -> Microbe<This>
  X Microbe<This> -> 3X MC
}
```

### Sulphur Exports

Class: `SulphurExports`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase Venus 1 step. Increase your M€ production 1 step for each Venus tag you have, including this. | — |
| Generated text | Raise Venus 1 step. Increase your M€ production 1 step per Venus tag you have (including this). | — |

Pets declaration:

```pets
CLASS SulphurExports : AutomatedCard {
  cost = 21
  This:: VenusTag<This>, SpaceTag<This>
  This: VenusStep, PROD[MC / VenusTag]
}
```

### Terraforming Contract

Class: `TerraformingContract`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have at least 25 TR. Increase your M€ production 4 steps. | — |
| Generated text | Requires that you have 25 terraform rating. Increase your M€ production 4 steps. | — |

Pets declaration:

```pets
CLASS TerraformingContract : AutomatedCard {
  cost = 8
  requirement = HAS "25 TerraformRating"
  This:: EarthTag<This>
  This: PROD[4 MC]
}
```

### Thermophiles

Class: `Thermophiles`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 6%. | Action: Add 1 microbe to ANY VENUS CARD, or spend 2 microbes here to raise Venus 1 step. |
| Generated text | Requires Venus 6%. | Action: Add 1 microbe to a Venus card, or spend 2 microbes from this card to raise Venus 1 step. |

Pets declaration:

```pets
CLASS Thermophiles : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 9
  requirement = HAS "3 VenusStep"
  This:: VenusTag<This>, MicrobeTag<This>
  -> Microbe<CardFront(HAS VenusTag)>
  2 Microbe<This> -> VenusStep
}
```

### Venus Governor

Class: `VenusGovernor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 Venus tags. Increase your M€ production 2 steps. | — |
| Generated text | Requires 2 Venus tags. Increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS VenusGovernor : AutomatedCard {
  cost = 4
  requirement = HAS "2 VenusTag"
  This:: 2 VenusTag<This>
  This: PROD[2 MC]
}
```

### Venusian Animals

Class: `VenusianAnimals`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 18%. 1 VP for each animal on this card. | Effect: When you play a science tag, including this, add 1 animal to this card. |
| Generated text | Requires Venus 18%. 1 VP per animal on this card. | Effect: When you play a science tag (including this), add 1 animal to this card. |

Pets declaration:

```pets
CLASS VenusianAnimals : ActiveCard, ResourceCard<Class<Animal>> {
  cost = 15
  requirement = HAS "9 VenusStep"
  This:: VenusTag<This>, ScienceTag<This>, AnimalTag<This>
  ScienceTag: Animal<This>
  End: VictoryPoint / Animal<This>
}
```

### Venusian Insects

Class: `VenusianInsects`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 12%. 1 VP per 2 microbes on this card. | Action: Add 1 microbe to this card. |
| Generated text | Requires Venus 12%. 1 VP per 2 microbes on this card. | Action: Add 1 microbe to this card. |

Pets declaration:

```pets
CLASS VenusianInsects : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 5
  requirement = HAS "6 VenusStep"
  This:: VenusTag<This>, MicrobeTag<This>
  End: VictoryPoint / 2 Microbe<This>
  -> Microbe<This>
}
```

### Venusian Plants

Class: `VenusianPlants`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 16%. Raise Venus 1 step. Add 1 microbe or 1 animal to ANOTHER VENUS CARD. | — |
| Generated text | Requires Venus 16%. Raise Venus 1 step. Add 1 microbe or 1 animal to a Venus card. | — |

Pets declaration:

```pets
CLASS VenusianPlants : AutomatedCard {
  cost = 13
  requirement = HAS "8 VenusStep"
  This:: VenusTag<This>, PlantTag<This>
  This: VenusStep, Microbe<CardFront(HAS VenusTag)> OR Animal<CardFront(HAS VenusTag)>
  End: VictoryPoint
}
```

### Venus Magnetizer

Class: `VenusMagnetizer`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 10%. | Action: Decrease your energy production 1 step to raise Venus 1 step. |
| Generated text | Requires Venus 10%. | Action: Decrease your energy production 1 step to raise Venus 1 step. |

Pets declaration:

```pets
CLASS VenusMagnetizer : ActionCard, ActiveCard {
  cost = 7
  requirement = HAS "5 VenusStep"
  This:: VenusTag<This>
  PROD[Energy] -> VenusStep
}
```

### Venus Soils

Class: `VenusSoils`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 1 step. Increase your plant production 1 step. Add 2 microbes to ANOTHER card. | — |
| Generated text | Raise Venus 1 step. Increase your plant production 1 step. Add 2 microbes to another card. | — |

Pets declaration:

```pets
CLASS VenusSoils : AutomatedCard {
  cost = 20
  This:: VenusTag<This>, PlantTag<This>
  This: VenusStep, PROD[Plant], 2 Microbe
}
```

### Venus Waystation

Class: `VenusWaystation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you play a Venus tag, you pay 2 M€ less for it. |
| Generated text | — | Effect: When you play a Venus tag, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS VenusWaystation : ActiveCard {
  cost = 9
  This:: VenusTag<This>, SpaceTag<This>
  PayingFor<Class<VenusTag>>:: -2 Owed
  End: VictoryPoint
}
```

### Water To Venus

Class: `WaterToVenus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 1 step. | — |
| Generated text | Raise Venus 1 step. | — |

Pets declaration:

```pets
CLASS WaterToVenus : EventCard {
  cost = 9
  This:: SpaceTag<This>, EventTag<This>
  This: VenusStep
}
```

## Prelude 1 Card Pack

### House Printing

Class: `HousePrinting`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your steel production 1 step. | — |
| Generated text | Increase your steel production 1 step. | — |

Pets declaration:

```pets
CLASS HousePrinting : AutomatedCard {
  cost = 10
  This:: BuildingTag<This>
  This: PROD[Steel]
  End: VictoryPoint
}
```

### Lava Tube Settlement

Class: `LavaTubeSettlement`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step. Increase your M€ production 2 steps. Place a city tile ON A VOLCANIC AREA, same as Lava Flows, regardless of adjacent cities. | — |
| Generated text | Decrease your energy production 1 step and increase your M€ production 2 steps. Place a city tile on a volcanic area if using a board that has one, otherwise place it normally. | — |

Pets declaration:

```pets
CLASS LavaTubeSettlement : AutomatedCard {
  cost = 15
  This:: CityTag<This>, BuildingTag<This>
  This: PROD[-Energy, 2 MC], CityTile<VolcanicArea> OR (MAX 0 VolcanicArea: CityTile<>)
}
```

### Martian Survey

Class: `MartianSurvey`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Oxygen must be 4% or lower. Draw 2 cards. | — |
| Generated text | Requires 4% oxygen or less. Draw 2 cards. | — |

Pets declaration:

```pets
CLASS MartianSurvey : EventCard {
  cost = 9
  requirement = HAS "MAX 4 OxygenStep"
  This:: ScienceTag<This>, EventTag<This>
  This: 2 ProjectCard
  End: VictoryPoint
}
```

### Psychrophiles

Class: `Psychrophiles`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires temperature -20°C or colder. | Action: Add a microbe to this card. Effect: When paying for a plant card, microbes here may be used as 2 M€ each. |
| Generated text | Requires -20°C or colder. | Action: Add 1 microbe to this card. / Effect: When you play a plant tag, microbes on this card may be used as 2 M€ each. |

Pets declaration:

```pets
CLASS Psychrophiles : ActionCard, ActiveCard, ResourceCard<Class<Microbe>> {
  cost = 2
  requirement = HAS "MAX 5 TemperatureStep"
  This:: MicrobeTag<This>
  PayingFor<Class<PlantTag>>:: AcceptingFromCard<This>
  PayFromCard<This>:: -2 Owed
  -> Microbe<This>
}
```

### SF Memorial

Class: `SfMemorial`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 1 card. | — |
| Generated text | Draw 1 card. | — |

Pets declaration:

```pets
CLASS SfMemorial : AutomatedCard {
  cost = 7
  This:: BuildingTag<This>
  This: ProjectCard
  End: VictoryPoint
}
```

### Space Hotels

Class: `SpaceHotels`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 Earth tags. Increase your M€ production 4 steps. | — |
| Generated text | Requires 2 Earth tags. Increase your M€ production 4 steps. | — |

Pets declaration:

```pets
CLASS SpaceHotels : AutomatedCard {
  cost = 12
  requirement = HAS "2 EarthTag"
  This:: EarthTag<This>, SpaceTag<This>
  This: PROD[4 MC]
}
```

## Colonies Expansion

### Airliners

Class: `Airliners`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have 3 floaters. Increase your M€ production 2 steps. Add 2 floaters to ANOTHER card. | — |
| Generated text | Requires that you have 3 floaters. Increase your M€ production 2 steps. Add 2 floaters to another card. | — |

Pets declaration:

```pets
CLASS Airliners : AutomatedCard {
  cost = 11
  requirement = HAS "3 Floater"
  This: PROD[2 MC], 2 Floater
  End: VictoryPoint
}
```

### Air Raid

Class: `AirRaid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you lose 1 floater. Steal 5 M€ from any player. | — |
| Generated text | Remove 1 floater from any card. Steal 5 M€ from any player. | — |

Pets declaration:

```pets
CLASS AirRaid : EventCard {
  cost = 0
  This:: EventTag<This>
  This: -Floater, 5 MC<Owner FROM Anyone>
}
```

### Atmo Collectors

Class: `AtmoCollectors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 2 floaters to ANY card. | Action: Add 1 floater to this card, or spend 1 floater here to gain 2 titanium, or 3 energy, or 4 heat. |
| Generated text | Add 2 floaters to any card. | Action: Add 1 floater to this card, or spend 1 floater from this card to gain 2 titanium, 3 energy, or 4 heat. |

Pets declaration:

```pets
CLASS AtmoCollectors : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 15
  This: 2 Floater
  -> Floater<This>
  Floater<This> -> 2 Titanium OR 3 Energy OR 4 Heat
}
```

### Community Services

Class: `CommunityServices`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step per CARD WITH NO TAGS, including this. | — |
| Generated text | Increase your M€ production 1 step per card in play with no tags (including this). | — |

Pets declaration:

```pets
CLASS CommunityServices : AutomatedCard {
  cost = 13
  This: PROD[MC / CardFront(HAS MAX 0 Tag)]
  End: VictoryPoint
}
```

### Conscription

Class: `Conscription`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 Earth tags. The next card you play this generation costs 16 M€ less. | — |
| Generated text | Requires 2 Earth tags. When you play the next card this generation, you pay 16 M€ less for it. | — |

Pets declaration:

```pets
CLASS Conscription : EventCard {
  cost = 5
  requirement = HAS "2 EarthTag"
  This:: EarthTag<This>, EventTag<This>
  This: Conscription_NextCardEffect
  End: -VictoryPoint
}
```

### Corona Extractor

Class: `CoronaExtractor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4 science tags. Increase your energy production 4 steps. | — |
| Generated text | Requires 4 science tags. Increase your energy production 4 steps. | — |

Pets declaration:

```pets
CLASS CoronaExtractor : AutomatedCard {
  cost = 10
  requirement = HAS "4 ScienceTag"
  This:: PowerTag<This>, SpaceTag<This>
  This: PROD[4 Energy]
}
```

### Cryo-Sleep

Class: `CryoSleep`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you trade, you pay 1 less resource for it. |
| Generated text | — | Effect: When you use the Trade standard action, you pay 1 M€ less for it. |

Pets declaration:

```pets
CLASS CryoSleep : ActiveCard {
  cost = 10
  This:: ScienceTag<This>
  ActionBilling<TradeAction>:: -Owed
  End: VictoryPoint
}
```

### Earth Elevator

Class: `EarthElevator`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 3 steps. | — |
| Generated text | Increase your titanium production 3 steps. | — |

Pets declaration:

```pets
CLASS EarthElevator : AutomatedCard {
  cost = 43
  This:: EarthTag<This>, SpaceTag<This>
  This: PROD[3 Titanium]
  End: 4 VictoryPoint
}
```

### Ecology Research

Class: `EcologyResearch`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your plant production 1 step for each colony you own. Add 1 animal to ANOTHER card and 2 microbes to ANOTHER card. | — |
| Generated text | Increase your plant production 1 step per colony you own. Add 1 animal to another card. Add 2 microbes to another card. | — |

Pets declaration:

```pets
CLASS EcologyResearch : AutomatedCard {
  cost = 21
  This:: ScienceTag<This>, AnimalTag<This>, MicrobeTag<This>, PlantTag<This>
  This: PROD[Plant / Colony], Animal, 2 Microbe
  End: VictoryPoint
}
```

### Floater Leasing

Class: `FloaterLeasing`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step per 3 floaters you have. | — |
| Generated text | Increase your M€ production 1 step per 3 floaters you have. | — |

Pets declaration:

```pets
CLASS FloaterLeasing : AutomatedCard {
  cost = 3
  This: PROD[MC / 3 Floater]
}
```

### Floater Prototypes

Class: `FloaterPrototypes`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 2 floaters to ANOTHER card. | — |
| Generated text | Add 2 floaters to another card. | — |

Pets declaration:

```pets
CLASS FloaterPrototypes : EventCard {
  cost = 2
  This:: ScienceTag<This>, EventTag<This>
  This: 2 Floater
}
```

### Floater Technology

Class: `FloaterTechnology`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Add 1 floater to ANOTHER card. |
| Generated text | — | Action: Add 1 floater to another card. |

Pets declaration:

```pets
CLASS FloaterTechnology : ActionCard, ActiveCard {
  cost = 7
  This:: ScienceTag<This>
  -> Floater
}
```

### Galilean Waystation

Class: `GalileanWaystation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step for every Jovian tag in play. | — |
| Generated text | Increase your M€ production 1 step per Jovian tag in play. | — |

Pets declaration:

```pets
CLASS GalileanWaystation : AutomatedCard {
  cost = 15
  This:: SpaceTag<This>
  This: PROD[MC / JovianTag<Anyone>]
  End: VictoryPoint
}
```

### Heavy Taxation

Class: `HeavyTaxation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 Earth tags. Increase your M€ production 2 steps, and gain 4 M€. | — |
| Generated text | Requires 2 Earth tags. Increase your M€ production 2 steps. Gain 4 M€. | — |

Pets declaration:

```pets
CLASS HeavyTaxation : AutomatedCard {
  cost = 3
  requirement = HAS "2 EarthTag"
  This:: EarthTag<This>
  This: PROD[2 MC], 4 MC
  End: -VictoryPoint
}
```

### Ice Moon Colony

Class: `IceMoonColony`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place 1 colony and 1 ocean tile. | — |
| Generated text | Place a colony and an ocean tile. | — |

Pets declaration:

```pets
CLASS IceMoonColony : AutomatedCard {
  cost = 23
  This:: SpaceTag<This>
  This: Colony<>, OceanTile<>
}
```

### Impactor Swarm

Class: `ImpactorSwarm`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 Jovian tags. Gain 12 heat. Remove up to 2 plants from any player. | — |
| Generated text | Requires 2 Jovian tags. Gain 12 heat. You may remove up to 2 plants from any player. | — |

Pets declaration:

```pets
CLASS ImpactorSwarm : EventCard {
  cost = 11
  requirement = HAS "2 JovianTag"
  This:: SpaceTag<This>, EventTag<This>
  This: 12 Heat, -2 Plant<Anyone>?
}
```

### Interplanetary Colony Ship

Class: `InterplanetaryColonyShip`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a colony. | — |
| Generated text | Place a colony. | — |

Pets declaration:

```pets
CLASS InterplanetaryColonyShip : EventCard {
  cost = 12
  This:: EarthTag<This>, SpaceTag<This>, EventTag<This>
  This: Colony<>
}
```

### Jovian Lanterns

Class: `JovianLanterns`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 1 Jovian tag. Increase your TR 1 step. Add 2 floaters to ANY card. 1 VP per 2 floaters here. | Action: Spend 1 titanium to add 2 floaters here. |
| Generated text | Requires a Jovian tag. Raise your terraform rating 1 step. Add 2 floaters to any card. 1 VP per 2 floaters on this card. | Action: Spend 1 titanium to add 2 floaters to this card. |

Pets declaration:

```pets
CLASS JovianLanterns : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 20
  requirement = HAS "JovianTag"
  This:: JovianTag<This>
  This: TerraformRating, 2 Floater
  End: VictoryPoint / 2 Floater<This>
  Titanium -> 2 Floater<This>
}
```

### Jupiter Floating Station

Class: `JupiterFloatingStation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 science tags. | Action: Add 1 floater to a JOVIAN CARD, or gain 1 M€ for every floater here (MAX 4). |
| Generated text | Requires 3 science tags. | Action: Add 1 floater to a Jovian card, or gain 1 M€ per floater on this card (max 4). |

Pets declaration:

```pets
CLASS JupiterFloatingStation : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 9
  requirement = HAS "3 ScienceTag"
  This:: JovianTag<This>
  End: VictoryPoint
  -> Floater<CardFront(HAS JovianTag)>
  -> MC / Floater<This> MAX 4
}
```

### Luna Governor

Class: `LunaGovernor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 Earth tags. Increase your M€ production 2 steps. | — |
| Generated text | Requires 3 Earth tags. Increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS LunaGovernor : AutomatedCard {
  cost = 4
  requirement = HAS "3 EarthTag"
  This:: 2 EarthTag<This>
  This: PROD[2 MC]
}
```

### Lunar Exports

Class: `LunarExports`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your plant production 2 steps, or increase your M€ production 5 steps. | — |
| Generated text | Increase your plant production 2 steps or your M€ production 5 steps. | — |

Pets declaration:

```pets
CLASS LunarExports : AutomatedCard {
  cost = 19
  This:: SpaceTag<This>, EarthTag<This>
  This: PROD[2 Plant OR 5 MC]
}
```

### Lunar Mining

Class: `LunarMining`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 1 step for every 2 Earth tags you have in play, including this. | — |
| Generated text | Increase your titanium production 1 step per 2 Earth tags you have (including this). | — |

Pets declaration:

```pets
CLASS LunarMining : AutomatedCard {
  cost = 11
  This:: EarthTag<This>
  This: PROD[Titanium / 2 EarthTag]
}
```

### Market Manipulation

Class: `MarketManipulation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | INCREASE ONE COLONY TILE TRACK 1 STEP. DECREASE ANOTHER COLONY TILE TRACK 1 STEP | — |
| Generated text | Increase one colony tile track 1 step and decrease another colony tile track 1 step. | — |

Pets declaration:

```pets
CLASS MarketManipulation : EventCard {
  cost = 1
  This:: EarthTag<This>, EventTag<This>
  This: ColonyProduction(NOT Source@ColonyProduction) FROM Source@ColonyProduction
}
```

### Martian Zoo

Class: `MartianZoo`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 city tiles in play. | Effect: when you play an Earth tag, place an animal here. Action: Gain 1 M€ per animal here. |
| Generated text | Requires 2 city tiles in play. | Action: Gain 1 M€ per animal on this card. / Effect: When you play an Earth tag, add 1 animal to this card. |

Pets declaration:

```pets
CLASS MartianZoo : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 12
  requirement = HAS "2 CityTile<Anyone>"
  This:: AnimalTag<This>, BuildingTag<This>
  EarthTag: Animal<This>
  End: VictoryPoint
  -> MC / Animal<This>
}
```

### Mining Colony

Class: `MiningColony`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 1 step. Place a colony. | — |
| Generated text | Increase your titanium production 1 step. Place a colony. | — |

Pets declaration:

```pets
CLASS MiningColony : AutomatedCard {
  cost = 20
  This:: SpaceTag<This>
  This: PROD[Titanium], Colony<>
}
```

### Minority Refuge

Class: `MinorityRefuge`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 2 steps. Place a colony. | — |
| Generated text | Decrease your M€ production 2 steps. Place a colony. | — |

Pets declaration:

```pets
CLASS MinorityRefuge : AutomatedCard {
  cost = 5
  This:: SpaceTag<This>
  This: PROD[-2 MC], Colony<>
}
```

### Molecular Printing

Class: `MolecularPrinting`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 1 M€ for each city tile in play. Gain 1 M€ for each colony in play. | — |
| Generated text | Gain 1 M€ per city tile in play. Gain 1 M€ per colony in play. | — |

Pets declaration:

```pets
CLASS MolecularPrinting : AutomatedCard {
  cost = 11
  This:: ScienceTag<This>
  This: MC / CityTile<Anyone>, MC / Colony<Anyone>
  End: VictoryPoint
}
```

### Nitrogen From Titan

Class: `NitrogenFromTitan`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 2 steps. Add 2 floaters to a JOVIAN CARD. | — |
| Generated text | Raise your terraform rating 2 steps. Add 2 floaters to a Jovian card. | — |

Pets declaration:

```pets
CLASS NitrogenFromTitan : AutomatedCard {
  cost = 25
  This:: JovianTag<This>, SpaceTag<This>
  This: 2 TerraformRating, 2 Floater<CardFront(HAS JovianTag)>
  End: VictoryPoint
}
```

### Pioneer Settlement

Class: `PioneerSettlement`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have no more than 1 colony. Decrease your M€ production 2 steps. Place a colony. | — |
| Generated text | Requires that you have 1 or fewer colonies. Decrease your M€ production 2 steps. Place a colony. | — |

Pets declaration:

```pets
CLASS PioneerSettlement : AutomatedCard {
  cost = 13
  requirement = HAS "MAX 1 Colony"
  This:: SpaceTag<This>
  This: PROD[-2 MC], Colony<>
  End: 2 VictoryPoint
}
```

### Productive Outpost

Class: `ProductiveOutpost`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | GAIN ALL YOUR COLONY BONUSES | — |
| Generated text | Gain all your colony bonuses. | — |

Pets declaration:

```pets
CLASS ProductiveOutpost : AutomatedCard {
  cost = 0
  This: GainColonyBonuses
}
```

### Quantum Communications

Class: `QuantumCommunications`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4 science tags. Increase your M€ production 1 step for each colony in play. | — |
| Generated text | Requires 4 science tags. Increase your M€ production 1 step per colony in play. | — |

Pets declaration:

```pets
CLASS QuantumCommunications : AutomatedCard {
  cost = 8
  requirement = HAS "4 ScienceTag"
  This: PROD[MC / Colony<Anyone>]
  End: VictoryPoint
}
```

### Red Spot Observatory

Class: `RedSpotObservatory`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 science tags. Draw 2 cards. | Action: Add 1 floater to this card, or spend 1 floater here to draw a card. |
| Generated text | Requires 3 science tags. Draw 2 cards. | Action: Add 1 floater to this card, or spend 1 floater from this card to draw 1 card. |

Pets declaration:

```pets
CLASS RedSpotObservatory : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 17
  requirement = HAS "3 ScienceTag"
  This:: ScienceTag<This>, JovianTag<This>
  This: 2 ProjectCard
  End: 2 VictoryPoint
  -> Floater<This>
  Floater<This> -> ProjectCard
}
```

### Refugee Camps

Class: `RefugeeCamps`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | 1 VP for each camp resource on this card. | Action: Decrease your M€ production 1 step to add a camp resource to this card. |
| Generated text | 1 VP per camp resource on this card. | Action: Decrease your M€ production 1 step to add 1 camp resource to this card. |

Pets declaration:

```pets
CLASS RefugeeCamps : ActionCard, ActiveCard, ResourceCard<Class<Camp>> {
  cost = 10
  This:: EarthTag<This>
  End: VictoryPoint / Camp<This>
  PROD[MC] -> Camp<This>
}
```

### Research Colony

Class: `ResearchColony`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a colony. MAY BE PLACED WHERE YOU ALREADY HAVE A COLONY. Draw 2 cards. | — |
| Generated text | Place a colony (may be placed where you already have a colony). Draw 2 cards. | — |

Pets declaration:

```pets
CLASS ResearchColony : AutomatedCard {
  cost = 20
  This:: ScienceTag<This>, SpaceTag<This>
  This: Colony<ColonyTile>, 2 ProjectCard
}
```

### Rim Freighters

Class: `RimFreighters`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you trade, you pay 1 less resource for it. |
| Generated text | — | Effect: When you use the Trade standard action, you pay 1 M€ less for it. |

Pets declaration:

```pets
CLASS RimFreighters : ActiveCard {
  cost = 4
  This:: SpaceTag<This>
  ActionBilling<TradeAction>:: -Owed
}
```

### Sky Docks

Class: `SkyDocks`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 Earth tags. Gain 1 Trade Fleet. | Effect: When you play a card, you pay 1 M€ less for it. |
| Generated text | Requires 2 Earth tags. Gain 1 Trade Fleet. | Effect: When you play a card, you pay 1 M€ less for it. |

Pets declaration:

```pets
CLASS SkyDocks : ActiveCard {
  cost = 18
  requirement = HAS "2 EarthTag"
  This:: EarthTag<This>, SpaceTag<This>
  This: TradeFleet
  PayingFor<Class<CardFront>>:: -Owed
  End: 2 VictoryPoint
}
```

### Solar Probe

Class: `SolarProbe`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 1 card for every 3 science tags you have, including this. | — |
| Generated text | Draw 1 card per 3 science tags you have (including this). | — |

Pets declaration:

```pets
CLASS SolarProbe : EventCard {
  cost = 9
  This:: ScienceTag<This>, SpaceTag<This>, EventTag<This>
  This: ProjectCard / 3 ScienceTag
  End: VictoryPoint
}
```

### Solar Reflectors

Class: `SolarReflectors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your heat production 5 steps. | — |
| Generated text | Increase your heat production 5 steps. | — |

Pets declaration:

```pets
CLASS SolarReflectors : AutomatedCard {
  cost = 23
  This:: SpaceTag<This>
  This: PROD[5 Heat]
}
```

### Space Port

Class: `SpacePort`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 1 colony. Gain 1 Trade Fleet. Place a city tile. Decrease your energy production 1 step, and increase your M€ production 4 steps. | — |
| Generated text | Requires that you have a colony. Gain 1 Trade Fleet. Place a city tile. Decrease your energy production 1 step and increase your M€ production 4 steps. | — |

Pets declaration:

```pets
CLASS SpacePort : AutomatedCard {
  cost = 22
  requirement = HAS "Colony"
  This:: CityTag<This>, BuildingTag<This>
  This: TradeFleet, CityTile<>, PROD[-Energy, 4 MC]
}
```

### Space Port Colony

Class: `SpacePortColony`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires a colony. Place a colony. MAY BE PLACED ON A COLONY TILE WHERE YOU ALREADY HAVE A COLONY. Gain 1 Trade Fleet. 1 VP per 2 colonies in play. | — |
| Generated text | Requires that you have a colony. Place a colony (may be placed where you already have a colony). Gain 1 Trade Fleet. 1 VP per 2 colonies in play. | — |

Pets declaration:

```pets
CLASS SpacePortColony : AutomatedCard {
  cost = 27
  requirement = HAS "Colony"
  This:: SpaceTag<This>
  This: Colony<ColonyTile>, TradeFleet
  End: VictoryPoint / 2 Colony<Anyone>
}
```

### Spin-Off Department

Class: `SpinOffDepartment`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 2 steps. | Effect: WHEN PLAYING A CARD WITH A BASIC COST OF 20 M€ OR MORE, draw a card. |
| Generated text | Increase your M€ production 2 steps. | Effect: When you play a card with a printed cost of 20 M€ or more, draw 1 card. |

Pets declaration:

```pets
CLASS SpinOffDepartment : ActiveCard {
  cost = 10
  This:: BuildingTag<This>
  This: PROD[2 MC]
  CardFront(HAS 20 cost): ProjectCard
}
```

### Sub-Zero Salt Fish

Class: `SubZeroSaltFish`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires -6°C or warmer. Decrease any plant production 1 step. 1 VP per 2 animals on this card. | Action: Add 1 animal to this card. |
| Generated text | Requires -6°C or warmer. Decrease any player's plant production 1 step. 1 VP per 2 animals on this card. | Action: Add 1 animal to this card. |

Pets declaration:

```pets
CLASS SubZeroSaltFish : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 5
  requirement = HAS "12 TemperatureStep"
  This:: AnimalTag<This>
  This: PROD[-Plant<Anyone>]
  End: VictoryPoint / 2 Animal<This>
  -> Animal<This>
}
```

### Titan Air-Scrapping

Class: `TitanAirScrapping`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 1 titanium to add 2 floaters to this card, or spend 2 floaters here to increase your TR 1 step. |
| Generated text | — | Action: Spend 1 titanium to add 2 floaters to this card, or spend 2 floaters from this card to raise your terraform rating 1 step. |

Pets declaration:

```pets
CLASS TitanAirScrapping : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 21
  This:: JovianTag<This>
  End: 2 VictoryPoint
  Titanium -> 2 Floater<This>
  2 Floater<This> -> TerraformRating
}
```

### Titan Floating Launch-Pad

Class: `TitanFloatingLaunchPad`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 2 floaters to ANY JOVIAN CARD. | Action: Add 1 floater to ANY JOVIAN CARD, or spend 1 floater here to trade for free. |
| Generated text | Add 2 floaters to a Jovian card. | Action: Add 1 floater to a Jovian card, or spend 1 floater from this card to trade. |

Pets declaration:

```pets
CLASS TitanFloatingLaunchPad : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 18
  This:: JovianTag<This>
  This: 2 Floater<CardFront(HAS JovianTag)>
  End: VictoryPoint
  -> Floater<CardFront(HAS JovianTag)>
  Floater<This> -> Trade
}
```

### Titan Shuttles

Class: `TitanShuttles`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Add 2 floaters to ANY JOVIAN CARD, or spend any number of floaters here to gain the same number of titanium. |
| Generated text | — | Action: Add 2 floaters to a Jovian card, or spend 1 or more floaters from this card to gain the same number of titanium. |

Pets declaration:

```pets
CLASS TitanShuttles : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 23
  This:: JovianTag<This>, SpaceTag<This>
  End: VictoryPoint
  -> 2 Floater<CardFront(HAS JovianTag)>
  X Floater<This> -> X Titanium
}
```

### Trade Envoys

Class: `TradeEnvoys`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you trade, you may first increase that Colony Tile track 1 step. |
| Generated text | — | Effect: When you trade, \[TradeBarrier&lt;@ColonyTile&gt;\]. \[Trade&lt;@ColonyTile&gt;: ColonyProduction&lt;@ColonyTile&gt;? THEN -TradeBarrier&lt;@ColonyTile&gt;\]. |

Pets declaration:

```pets
CLASS TradeEnvoys : ActiveCard {
  cost = 6
  Trade<@ColonyTile>:: TradeBarrier<@ColonyTile>
  Trade<@ColonyTile>: ColonyProduction<@ColonyTile>? THEN -TradeBarrier<@ColonyTile>
}
```

### Trading Colony

Class: `TradingColony`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a colony. | Effect: When you trade, you may first increase that Colony Tile track 1 step. |
| Generated text | Place a colony. | Effect: When you trade, \[TradeBarrier&lt;@ColonyTile&gt;\]. \[Trade&lt;@ColonyTile&gt;: ColonyProduction&lt;@ColonyTile&gt;? THEN -TradeBarrier&lt;@ColonyTile&gt;\]. |

Pets declaration:

```pets
CLASS TradingColony : ActiveCard {
  cost = 18
  This:: SpaceTag<This>
  This: Colony<>
  Trade<@ColonyTile>:: TradeBarrier<@ColonyTile>
  Trade<@ColonyTile>: ColonyProduction<@ColonyTile>? THEN -TradeBarrier<@ColonyTile>
}
```

### Urban Decomposers

Class: `UrbanDecomposers`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have 1 city tile and 1 colony in play. Increase your plant production 1 step, and add 2 microbes to ANOTHER card. | — |
| Generated text | Requires that you have a city tile and a colony. Increase your plant production 1 step. Add 2 microbes to another card. | — |

Pets declaration:

```pets
CLASS UrbanDecomposers : AutomatedCard {
  cost = 6
  requirement = HAS "CityTile, Colony"
  This:: MicrobeTag<This>
  This: PROD[Plant], 2 Microbe
}
```

### Warp Drive

Class: `WarpDrive`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 5 science tags. | Effect: When you play a space tag, you pay 4 M€ less for it. |
| Generated text | Requires 5 science tags. | Effect: When you play a space tag, you pay 4 M€ less for it. |

Pets declaration:

```pets
CLASS WarpDrive : ActiveCard {
  cost = 14
  requirement = HAS "5 ScienceTag"
  This:: ScienceTag<This>
  PayingFor<Class<SpaceTag>>:: -4 Owed
  End: 2 VictoryPoint
}
```

## Turmoil Expansion

### Aerial Lenses

Class: `AerialLenses`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Kelvinists are ruling or that you have 2 delegates there. Remove up to 2 plants from any player. Increase your heat production 2 steps. | — |
| Generated text | Requires that you meet the party requirement for the Kelvinists party. You may remove up to 2 plants from any player. Increase your heat production 2 steps. | — |

Pets declaration:

```pets
CLASS AerialLenses : AutomatedCard {
  cost = 2
  requirement = HAS "PartyRequirement<Kelvinists>"
  This: -2 Plant<Anyone>?, PROD[2 Heat]
  End: -VictoryPoint
}
```

### Cultural Metropolis

Class: `CulturalMetropolis`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Unity is ruling or that you have 2 delegates there. Decrease your energy production 1 step and increase your M€ production 3 steps. Place a city tile. Place 2 delegates in 1 party. | — |
| Generated text | Requires that you meet the party requirement for Unity. Decrease your energy production 1 step and increase your M€ production 3 steps. Place a city tile and 2 delegates. | — |

Pets declaration:

```pets
CLASS CulturalMetropolis : AutomatedCard {
  cost = 20
  requirement = HAS "PartyRequirement<Unity>"
  This:: CityTag<This>, BuildingTag<This>
  This: PROD[-Energy, 3 MC], CityTile<>, 2 PartyDelegate
}
```

### Diaspora Movement

Class: `DiasporaMovement`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Reds are ruling or that you have 2 delegates there. Gain 1 M€ for each Jovian tag in play. | — |
| Generated text | Requires that you meet the party requirement for the Reds party. Gain 1 M€ per Jovian tag in play (including this). | — |

Pets declaration:

```pets
CLASS DiasporaMovement : AutomatedCard {
  cost = 7
  requirement = HAS "PartyRequirement<Reds>"
  This:: JovianTag<This>
  This: MC / JovianTag<Anyone>
  End: VictoryPoint
}
```

### Event Analysts

Class: `EventAnalysts`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Scientists are ruling or that you have 2 delegates there. | Effect: You have influence +1. |
| Generated text | Requires that you meet the party requirement for the Scientists party. | Effect: When influence is counted, gain 1 influence. |

Pets declaration:

```pets
CLASS EventAnalysts : ActiveCard {
  cost = 5
  requirement = HAS "PartyRequirement<Scientists>"
  This:: ScienceTag<This>
  MeasureInfluence:: EventAnalystsInfluence
}
```

### GMO Contract

Class: `GmoContract`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Greens are ruling or that you have 2 delegates there. | Effect: Each time you play a plant, animal, or microbe tag, including this, gain 2 M€. |
| Generated text | Requires that you meet the party requirement for the Greens party. | Effect: When you play a bio tag (including this), gain 2 M€. |

Pets declaration:

```pets
CLASS GmoContract : ActiveCard {
  cost = 3
  requirement = HAS "PartyRequirement<Greens>"
  This:: MicrobeTag<This>, ScienceTag<This>
  BioTag: 2 MC
}
```

### Martian Media Center

Class: `MartianMediaCenter`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Mars First is ruling or that you have 2 delegates there. Increase your M€ production 2 steps. | Action: Pay 3 M€ to add a delegate to any party. |
| Generated text | Requires that you meet the party requirement for Mars First. Increase your M€ production 2 steps. | Action: Spend 3 M€ to place a delegate. |

Pets declaration:

```pets
CLASS MartianMediaCenter : ActionCard, ActiveCard {
  cost = 7
  requirement = HAS "PartyRequirement<MarsFirst>"
  This:: BuildingTag<This>
  This: PROD[2 MC]
  3 MC -> PartyDelegate
}
```

### Parliament Hall

Class: `ParliamentHall`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Mars First is ruling or that you have 2 delegates there. Increase M€ production 1 step for every 3 building tags you have, including this. | — |
| Generated text | Requires that you meet the party requirement for Mars First. Increase your M€ production 1 step per 3 building tags you have (including this). | — |

Pets declaration:

```pets
CLASS ParliamentHall : AutomatedCard {
  cost = 8
  requirement = HAS "PartyRequirement<MarsFirst>"
  This:: BuildingTag<This>
  This: PROD[MC / 3 BuildingTag]
  End: VictoryPoint
}
```

### PR Office

Class: `PrOffice`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Unity is ruling or that you have 2 delegates there. Gain 1 TR. Gain 1 M€ for each Earth tag you have, including this. | — |
| Generated text | Requires that you meet the party requirement for Unity. Raise your terraform rating 1 step. Gain 1 M€ per Earth tag you have (including this). | — |

Pets declaration:

```pets
CLASS PrOffice : AutomatedCard {
  cost = 7
  requirement = HAS "PartyRequirement<Unity>"
  This:: EarthTag<This>
  This: TerraformRating, MC / EarthTag
}
```

### Public Celebrations

Class: `PublicCelebrations`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you are Chairman. | — |
| Generated text | Requires that you are chairman. | — |

Pets declaration:

```pets
CLASS PublicCelebrations : EventCard {
  cost = 8
  requirement = HAS "Chairman"
  This:: EventTag<This>
  End: 2 VictoryPoint
}
```

### Recruitment

Class: `Recruitment`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Exchange one NEUTRAL NON-LEADER delegate with one of your own from the reserve. | — |
| Generated text | \[Audit\]. \[PartyDelegate&lt;Party(HAS 1 (PartyDelegate&lt;Neutral&gt; - PartyLeader&lt;Neutral&gt;)), Owner FROM Neutral&gt;\]. | — |

Pets declaration:

```pets
CLASS Recruitment : EventCard {
  cost = 2
  This:: EventTag<This>
  This: Audit, PartyDelegate<Party(HAS 1 (PartyDelegate<Neutral> - PartyLeader<Neutral>)), Owner FROM Neutral>
}
```

### Red Tourism Wave

Class: `RedTourismWave`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Reds are ruling or that you have 2 delegates there. Gain 1 M€ for each EMPTY AREA ADJACENT TO YOUR TILES. | — |
| Generated text | Requires that you meet the party requirement for the Reds party. Gain 1 M€ per area on Mars with no tiles next to at least 1 tile you own. | — |

Pets declaration:

```pets
CLASS RedTourismWave : EventCard {
  cost = 3
  requirement = HAS "PartyRequirement<Reds>"
  This:: EarthTag<This>, EventTag<This>
  This: MC / MarsArea(HAS MAX 0 Tile, HAS Neighbor<OwnedTile>)
}
```

### Sponsored Mohole

Class: `SponsoredMohole`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Kelvinists are ruling or that you have 2 delegates there. Increase your heat production 2 steps. | — |
| Generated text | Requires that you meet the party requirement for the Kelvinists party. Increase your heat production 2 steps. | — |

Pets declaration:

```pets
CLASS SponsoredMohole : AutomatedCard {
  cost = 5
  requirement = HAS "PartyRequirement<Kelvinists>"
  This:: BuildingTag<This>
  This: PROD[2 Heat]
}
```

### Supported Research

Class: `SupportedResearch`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Scientists are ruling or that you have 2 delegates there. Draw 2 cards. | — |
| Generated text | Requires that you meet the party requirement for the Scientists party. Draw 2 cards. | — |

Pets declaration:

```pets
CLASS SupportedResearch : AutomatedCard {
  cost = 3
  requirement = HAS "PartyRequirement<Scientists>"
  This:: ScienceTag<This>
  This: 2 ProjectCard
}
```

### Vote Of No Confidence

Class: `VoteOfNoConfidence`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have a Party Leader in any party and that the sitting Chairman is neutral. Remove the NEUTRAL Chairman and move your own delegate (from the reserve) there instead. Gain 1 TR. | — |
| Generated text | Requires that you lead a party. Replace the neutral chairman with one of your delegates. Raise your terraform rating 1 step. | — |

Pets declaration:

```pets
CLASS VoteOfNoConfidence : EventCard {
  cost = 5
  requirement = HAS "PartyLeader"
  This:: EventTag<This>
  This: Chairman<Owner FROM Neutral>, TerraformRating
}
```

### Wildlife Dome

Class: `WildlifeDome`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Greens are ruling or that you have 2 delegates there. Place a greenery tile and raise oxygen 1 step. | — |
| Generated text | Requires that you meet the party requirement for the Greens party. Place a greenery tile. | — |

Pets declaration:

```pets
CLASS WildlifeDome : AutomatedCard {
  cost = 15
  requirement = HAS "PartyRequirement<Greens>"
  This:: AnimalTag<This>, PlantTag<This>, BuildingTag<This>
  This: GreeneryTile<>
}
```

## Promo Card Pack

### Advertising

Class: `Advertising`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you play a card with a basic cost of 20 M€ or more, increase your M€ production 1 step. |
| Generated text | — | Effect: When you play a card with a printed cost of 20 M€ or more, increase your M€ production 1 step. |

Pets declaration:

```pets
CLASS Advertising : ActiveCard {
  cost = 4
  This:: EarthTag<This>
  CardFront(HAS 20 cost): PROD[MC]
}
```

### Aqueduct Systems

Class: `AqueductSystems`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have a city next to an ocean. Draw 3 building cards. | — |
| Generated text | Requires that you have a city tile next to an ocean tile. Draw 3 building cards. | — |

Pets declaration:

```pets
CLASS AqueductSystems : AutomatedCard {
  cost = 9
  requirement = HAS "Adjacency<CityTile, OceanTile>"
  This:: BuildingTag<This>
  This: 3 SearchForCard<TagFilter<Class<BuildingTag>>>
  End: VictoryPoint
}
```

### Asteroid Deflection System

Class: `AsteroidDeflectionSystem`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease energy production 1 step. 1 VP per asteroid on this card. | Action: REVEAL AND DISCARD the top card of the deck. If it has a space tag, add an asteroid here. OPPONENTS MAY NOT REMOVE YOUR PLANTS |
| Generated text | Decrease your energy production 1 step. 1 VP per asteroid on this card. | Action: Reveal 1 project card. If it has a space tag, add 1 asteroid to this card. / Effect: Opponents may not remove your plants. |

Pets declaration:

```pets
CLASS AsteroidDeflectionSystem : ActionCard, ActiveCard, ResourceCard<Class<Asteroid>> {
  cost = 13
  This:: EarthTag<This>, SpaceTag<This>, BuildingTag<This>
  This: PROD[-Energy]
  -Plant BY Player(NOT Owner):: Die
  End: VictoryPoint / Asteroid<This>
  -> Asteroid<This>?
}
```

### Asteroid Hollowing

Class: `AsteroidHollowing`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | 1 VP per 2 asteroids on this card. | Action: Spend 1 titanium to add 1 asteroid resource here and increase M€ production 1 step. |
| Generated text | 1 VP per 2 asteroids on this card. | Action: Spend 1 titanium to add 1 asteroid to this card and increase your M€ production 1 step. |

Pets declaration:

```pets
CLASS AsteroidHollowing : ActionCard, ActiveCard, ResourceCard<Class<Asteroid>> {
  cost = 16
  This:: SpaceTag<This>
  End: VictoryPoint / 2 Asteroid<This>
  Titanium -> Asteroid<This>, PROD[MC]
}
```

### Asteroid Rights

Class: `AsteroidRights`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 2 asteroids to this card. | Action: Spend 1 M€ to add 1 asteroid to ANY card, OR spend 1 asteroid here to raise your M€ production 1 step or gain 2 titanium. |
| Generated text | Add 2 asteroids to this card. | Action: Spend 1 M€ to add 1 asteroid to any card, or spend 1 asteroid from this card to increase your M€ production 1 step or gain 2 titanium. |

Pets declaration:

```pets
CLASS AsteroidRights : ActionCard, ActiveCard, ResourceCard<Class<Asteroid>> {
  cost = 10
  This:: EarthTag<This>, SpaceTag<This>
  This: 2 Asteroid<This>
  MC -> Asteroid
  Asteroid<This> -> PROD[MC] OR 2 Titanium
}
```

### Astra Mechanica

Class: `AstraMechanica`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | CHOOSE 2 PROJECT CARDS FROM YOUR EVENT PILE AND TAKE THEM TO HAND. IT MAY NOT BE CARDS THAT PLACE SPECIAL TILES. | — |
| Generated text | You may return up to one of your played event cards to your hand. You may return up to one of your played event cards to your hand. | — |

Pets declaration:

```pets
CLASS AstraMechanica : AutomatedCard {
  cost = 7
  This:: ScienceTag<This>
  This: ProjectCard FROM PlayedEvent?, ProjectCard FROM PlayedEvent?
}
```

### Bactoviral Research

Class: `BactoviralResearch`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 1 card. Choose 1 of your played cards and add 1 microbe to it for each science tag you have, including this. | — |
| Generated text | Draw 1 card. Add 1 microbe to another card per science tag you have (including this). | — |

Pets declaration:

```pets
CLASS BactoviralResearch : AutomatedCard {
  cost = 10
  This:: MicrobeTag<This>, ScienceTag<This>
  This: ProjectCard, Microbe / ScienceTag
}
```

### Bio Printing Facility

Class: `BioPrintingFacility`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 2 energy to gain 2 plants OR to add 1 animal to ANOTHER card. |
| Generated text | — | Action: Spend 2 energy to gain 2 plants or add 1 animal to another card. |

Pets declaration:

```pets
CLASS BioPrintingFacility : ActionCard, ActiveCard {
  cost = 7
  This:: BuildingTag<This>
  2 Energy -> 2 Plant OR Animal
}
```

### Carbon Nanosystems

Class: `CarbonNanosystems`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you play a science tag, including this, add a graphene resource here. Effect: When playing a space or city tag, graphenes may be used as 4 M€ each. |
| Generated text | — | Effect: When you play a science tag (including this), add 1 graphene resource to this card. When you play a space tag or a city tag, graphene resources on this card may be used as 4 M€ each. |

Pets declaration:

```pets
CLASS CarbonNanosystems : ActiveCard, ResourceCard<Class<Graphene>> {
  cost = 14
  This:: ScienceTag<This>, BuildingTag<This>
  ScienceTag: Graphene<This>
  PayingFor<Class<SpaceTag>> OR PayingFor<Class<CityTag>>:: AcceptingFromCard<This>
  PayFromCard<This>:: -4 Owed
  End: VictoryPoint
}
```

### Casinos

Class: `Casinos`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have a city. Decrease your energy production 1 step and increase your M€ production 4 steps. | — |
| Generated text | Requires that you have a city tile. Decrease your energy production 1 step and increase your M€ production 4 steps. | — |

Pets declaration:

```pets
CLASS Casinos : AutomatedCard {
  cost = 5
  requirement = HAS "CityTile"
  This:: BuildingTag<This>
  This: PROD[-Energy, 4 MC]
}
```

### City Parks

Class: `CityParks`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have 3 city tiles. Gain 2 plants. | — |
| Generated text | Requires that you have 3 city tiles. Gain 2 plants. | — |

Pets declaration:

```pets
CLASS CityParks : AutomatedCard {
  cost = 7
  requirement = HAS "3 CityTile"
  This:: PlantTag<This>
  This: 2 Plant
  End: 2 VictoryPoint
}
```

### Comet Aiming

Class: `CometAiming`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 1 titanium to add 1 asteroid resource to ANY CARD or remove 1 asteroid here to place an ocean tile. |
| Generated text | — | Action: Spend 1 titanium to add 1 asteroid to any card, or spend 1 asteroid from this card to place an ocean tile. |

Pets declaration:

```pets
CLASS CometAiming : ActionCard, ActiveCard, ResourceCard<Class<Asteroid>> {
  cost = 17
  This:: SpaceTag<This>
  Titanium -> Asteroid
  Asteroid<This> -> OceanTile<>
}
```

### Crash Site Cleanup

Class: `CrashSiteCleanup`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | REQUIRES THAT A PLAYER REMOVED ANOTHER PLAYER'S PLANTS THIS GENERATION. Gain 1 titanium or 2 steel. | — |
| Generated text | \[MyResourceWasRemoved&lt;Anyone, Class&lt;Plant&gt;, Owner&gt;\]. Gain 1 titanium or 2 steel. | — |

Pets declaration:

```pets
CLASS CrashSiteCleanup : EventCard {
  cost = 4
  requirement = HAS "MyResourceWasRemoved<Anyone, Class<Plant>, Owner>"
  This:: EventTag<This>
  This: Titanium OR 2 Steel
  End: VictoryPoint
}
```

### Cutting Edge Technology

Class: `CuttingEdgeTechnology`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When playing a card with a requirement, you pay 2 M€ less. |
| Generated text | — | Effect: When you play a card with a requirement, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS CuttingEdgeTechnology : ActiveCard {
  cost = 12
  This:: ScienceTag<This>
  PayingFor<Class<CardFront>(HAS requirement)>:: -2 Owed
  End: VictoryPoint
}
```

### Cyberia Systems

Class: `CyberiaSystems`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your steel production 1 step. Copy the PRODUCTION BOXES of 2 of your cards with building tags. | — |
| Generated text | Increase your steel production 1 step. \[BuildingTag&lt;First@CardFront&gt;: CopyProductionBox&lt;First@CardFront&gt;\], then \[CopyProductionBox&lt;CardFront(HAS BuildingTag, NOT First@CardFront)&gt;\]. | — |

Pets declaration:

```pets
CLASS CyberiaSystems : AutomatedCard {
  cost = 16
  This: PROD[Steel], BuildingTag<First@CardFront>: CopyProductionBox<First@CardFront> THEN CopyProductionBox<CardFront(HAS BuildingTag, NOT First@CardFront)>
}
```

### Deimos Down\*

Class: `DeimosDownPromo`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 3 steps. Place this tile ADJACENT TO NO CITY TILE. Gain 4 steel. Remove up to 6 plants from any player. | — |
| Generated text | Raise temperature 3 steps. Place this tile on a land area next to no city tile. Gain 4 steel. You may remove up to 6 plants from any player. | — |

Pets declaration:

```pets
CLASS DeimosDownPromo : EventCard {
  cost = 31
  This:: SpaceTag<This>, EventTag<This>
  This: 3 TemperatureStep, DeimosDownPromo_SpecialTile<LandArea(HAS MAX 0 Neighbor<CityTile<Anyone>>)>, 4 Steel, -6 Plant<Anyone>?
}
```

### Directed Heat Usage

Class: `DirectedHeatUsage`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 3 heat to either gain 4 M€ or 2 plants. |
| Generated text | — | Action: Spend 3 heat to gain 4 M€ or 2 plants. |

Pets declaration:

```pets
CLASS DirectedHeatUsage : ActionCard, ActiveCard {
  cost = 1
  3 Heat -> 4 MC OR 2 Plant
}
```

### Directed Impactors

Class: `DirectedImpactors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 6 M€ to add 1 asteroid to ANY CARD (titanium may be used to pay for this), or remove 1 asteroid here to raise temperature 1 step. |
| Generated text | — | Action: Spend 6 M€ (titanium may be used) to add 1 asteroid to any card, or spend 1 asteroid from this card to raise temperature 1 step. |

Pets declaration:

```pets
CLASS DirectedImpactors : ActionCard, ActiveCard, ResourceCard<Class<Asteroid>> {
  cost = 8
  This:: SpaceTag<This>
  UseAction<This, Action1>:: Accepting<Class<Titanium>>
  6 MC -> Asteroid
  Asteroid<This> -> TemperatureStep
}
```

### Diversity Support

Class: `DiversitySupport`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have 9 different types of resources. Raise your TR 1 step. | — |
| Generated text | Requires that you have 9 different types of resources. Raise your terraform rating 1 step. | — |

Pets declaration:

```pets
CLASS DiversitySupport : EventCard {
  cost = 1
  requirement = HAS "9 Class<Resource>(HAS Resource<Owner>)"
  This:: EventTag<This>
  This: TerraformRating
}
```

### Dusk Laser Mining

Class: `DuskLaserMining`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 science tags. Decrease your energy production 1 step, and increase your titanium production 1 step. Gain 4 titanium. | — |
| Generated text | Requires 2 science tags. Decrease your energy production 1 step and increase your titanium production 1 step. Gain 4 titanium. | — |

Pets declaration:

```pets
CLASS DuskLaserMining : AutomatedCard {
  cost = 8
  requirement = HAS "2 ScienceTag"
  This:: SpaceTag<This>
  This: PROD[-Energy, Titanium], 4 Titanium
}
```

### Energy Market

Class: `EnergyMarket`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 2X M€ to gain X energy, or decrease energy production 1 step to gain 8 M€. |
| Generated text | — | Action: Spend 2X M€ to gain X energy, or decrease your energy production 1 step to gain 8 M€. |

Pets declaration:

```pets
CLASS EnergyMarket : ActionCard, ActiveCard {
  cost = 3
  This:: PowerTag<This>
  2X MC -> X Energy
  PROD[Energy] -> 8 MC
}
```

### Field-Capped City

Class: `FieldCappedCity`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 2 steps, and increase your energy production 1 step. Gain 3 plants, and place a city tile. | — |
| Generated text | Increase your M€ production 2 steps and your energy production 1 step. Gain 3 plants. Place a city tile. | — |

Pets declaration:

```pets
CLASS FieldCappedCity : AutomatedCard {
  cost = 29
  This:: PowerTag<This>, CityTag<This>, BuildingTag<This>
  This: PROD[2 MC, Energy], 3 Plant, CityTile<>
}
```

### Great Dam\*

Class: `GreatDamPromo`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4 ocean tiles. Increase your energy production 2 steps. Place this tile ADJACENT TO AN OCEAN TILE. | — |
| Generated text | Requires 4 ocean tiles. Increase your energy production 2 steps. Place this tile on a land area next to an ocean tile. | — |

Pets declaration:

```pets
CLASS GreatDamPromo : AutomatedCard {
  cost = 15
  requirement = HAS "4 OceanTile"
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[2 Energy], GreatDamPromo_SpecialTile<LandArea(HAS Neighbor<OceanTile>)>
  End: VictoryPoint
}
```

### Harvest

Class: `Harvest`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have 3 greenery tiles in play. Gain 12 M€. | — |
| Generated text | Requires that you have 3 greenery tiles. Gain 12 M€. | — |

Pets declaration:

```pets
CLASS Harvest : EventCard {
  cost = 4
  requirement = HAS "3 GreeneryTile"
  This:: PlantTag<This>, EventTag<This>
  This: 12 MC
}
```

### Hermetic Order Of Mars

Class: `HermeticOrderOfMars`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Oxygen must be 4% or lower. Increase your M€ production 2 steps. Gain 1 M€ per empty area adjacent to your tiles. | — |
| Generated text | Requires 4% oxygen or less. Increase your M€ production 2 steps. Gain 1 M€ per area on Mars with no tiles next to at least 1 tile you own. | — |

Pets declaration:

```pets
CLASS HermeticOrderOfMars : AutomatedCard {
  cost = 10
  requirement = HAS "MAX 4 OxygenStep"
  This: PROD[2 MC], MC / MarsArea(HAS MAX 0 Tile, HAS Neighbor<OwnedTile>)
}
```

### Hi-Tech Lab

Class: `HiTechLab`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend any amount of energy to draw the same number of cards. TAKE 1 INTO HAND AND DISCARD THE REST |
| Generated text | — | Action: Spend 1 or more energy to look at that many cards. Draw 1 of them. |

Pets declaration:

```pets
CLASS HiTechLab : ActionCard, ActiveCard {
  cost = 17
  This:: ScienceTag<This>, BuildingTag<This>
  End: VictoryPoint
  X Energy -> ProjectCard
}
```

### Homeostasis Bureau

Class: `HomeostasisBureau`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your heat production 2 steps. | Effect: When you raise temperature, gain 3 M€. |
| Generated text | Increase your heat production 2 steps. | Effect: When you raise temperature 1 step, gain 3 M€. |

Pets declaration:

```pets
CLASS HomeostasisBureau : ActiveCard {
  cost = 16
  This:: BuildingTag<This>
  This: PROD[2 Heat]
  TemperatureStep: 3 MC
}
```

### Hospitals

Class: `Hospitals`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 1 step. | Effect: Each time a city is placed, gain a disease here. Action: Remove a disease from ANY OF YOUR CARDS to gain 1 M€ per city in play. |
| Generated text | Decrease your energy production 1 step. | Action: Spend 1 disease resource from any of your cards to gain 1 M€ per city tile in play. / Effect: When any city tile is placed, add 1 disease resource to this card. |

Pets declaration:

```pets
CLASS Hospitals : ActionCard, ActiveCard, ResourceCard<Class<Disease>> {
  cost = 8
  This:: BuildingTag<This>
  This: PROD[-Energy]
  CityTile<Anyone>: Disease<This>
  End: VictoryPoint
  Disease -> MC / CityTile<Anyone>
}
```

### Icy Impactors

Class: `IcyImpactors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 10 M€ (titanium may be used) to add 2 asteroids here, or spend 1 asteroid here to place an ocean tile. FIRST PLAYER CHOOSES WHERE YOU MUST PLACE IT. |
| Generated text | — | Action: Spend 10 M€ (titanium may be used) to add 2 asteroids to this card, or spend 1 asteroid from this card to \[EACH Player(HAS StartToken) { ChooseOceanArea }\]. |

Pets declaration:

```pets
CLASS IcyImpactors : ActionCard, ActiveCard, ResourceCard<Class<Asteroid>> {
  cost = 15
  This:: SpaceTag<This>
  UseAction<This, Action1>:: Accepting<Class<Titanium>>
  10 MC -> 2 Asteroid<This>
  Asteroid<This> -> EACH Player(HAS StartToken) { ChooseOceanArea }
}
```

### Imported Nutrients

Class: `ImportedNutrients`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 4 plants, and add 4 microbes to ANOTHER CARD. | — |
| Generated text | Gain 4 plants. Add 4 microbes to another card. | — |

Pets declaration:

```pets
CLASS ImportedNutrients : EventCard {
  cost = 14
  This:: EarthTag<This>, SpaceTag<This>, EventTag<This>
  This: 4 Plant, 4 Microbe
}
```

### Interplanetary Trade

Class: `InterplanetaryTrade`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step per different tag you have in play, including this. | — |
| Generated text | Increase your M€ production 1 step per different tag you have (including this). | — |

Pets declaration:

```pets
CLASS InterplanetaryTrade : AutomatedCard {
  cost = 27
  This:: SpaceTag<This>
  This: PROD[MC / Class<Tag>(HAS Tag<Owner>)]
  End: VictoryPoint
}
```

### Jovian Embassy

Class: `JovianEmbassy`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 1 step. | — |
| Generated text | Raise your terraform rating 1 step. | — |

Pets declaration:

```pets
CLASS JovianEmbassy : AutomatedCard {
  cost = 14
  This:: JovianTag<This>, BuildingTag<This>
  This: TerraformRating
  End: VictoryPoint
}
```

### Kaguya Tech

Class: `KaguyaTech`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 2 steps. Draw 1 card. Remove 1 of your greenery tiles (does not affect oxygen). Place a city tile there, regardless of other restrictions. Gain placement bonuses as usual. | — |
| Generated text | Increase your M€ production 2 steps. Draw 1 card. Change a greenery tile on any area into a city tile on that area. | — |

Pets declaration:

```pets
CLASS KaguyaTech : AutomatedCard {
  cost = 10
  This:: CityTag<This>, PlantTag<This>
  This: PROD[2 MC], ProjectCard, CityTile<@MarsArea> FROM GreeneryTile<@MarsArea>
}
```

### Law Suit

Class: `LawSuit`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Steal 3 M€ from a player that REMOVED YOUR RESOURCES OR DECREASED YOUR PRODUCTION this generation. Place this card face down in THAT PLAYER'S EVENT PILE. | — |
| Generated text | \[(MyResourceWasRemoved&lt;Owner, Attacker@Player&gt; OR MyProductionWasDecreased&lt;Owner, Attacker@Player&gt;): 3 MC FROM MC&lt;Attacker@Player&gt;\], then \[PlayedEvent&lt;Attacker@Player, Class&lt;This&gt;&gt; FROM This\]. | — |

Pets declaration:

```pets
CLASS LawSuit : EventCard {
  cost = 2
  This:: EarthTag<This>, EventTag<This>
  This: (MyResourceWasRemoved<Owner, Attacker@Player> OR MyProductionWasDecreased<Owner, Attacker@Player>): 3 MC FROM MC<Attacker@Player> THEN PlayedEvent<Attacker@Player, Class<This>> FROM This
  End: -VictoryPoint
}
```

### Magnetic Field Generators\*

Class: `MagneticFieldGeneratorsPromo`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your energy production 4 steps and increase your plant production 2 steps. Raise your TR 3 steps. Place this tile. | — |
| Generated text | Decrease your energy production 4 steps and increase your plant production 2 steps. Raise your terraform rating 3 steps. Place this tile. | — |

Pets declaration:

```pets
CLASS MagneticFieldGeneratorsPromo : AutomatedCard {
  cost = 22
  This:: BuildingTag<This>
  This: PROD[-4 Energy, 2 Plant], 3 TerraformRating, MagneticFieldGeneratorsPromo_SpecialTile<>
}
```

### Magnetic Shield

Class: `MagneticShield`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 power tags. Raise your TR 4 steps. | — |
| Generated text | Requires 3 power tags. Raise your terraform rating 4 steps. | — |

Pets declaration:

```pets
CLASS MagneticShield : AutomatedCard {
  cost = 24
  requirement = HAS "3 PowerTag"
  This:: SpaceTag<This>
  This: 4 TerraformRating
}
```

### Mars Nomads

Class: `MarsNomads`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | PLACE THE NOMADS (a gold cube) on a non-reserved, empty area on the game board. | Action: Move the Nomads to an adjacent, non-reserved, empty area, and GAIN PLACEMENT BONUSES as if placing a special tile there. No tiles may be placed on the Nomad area. |
| Generated text | Place a nomads marker on a land area with no occupant. | Action: \[NomadsMarker&lt;LandArea(HAS MAX 0 Occupant, HAS Neighbor&lt;NomadsMarker&lt;Owner&gt;&gt;, NOT Source@LandArea)&gt; FROM NomadsMarker&lt;Owner, Source@LandArea&gt;\], then \[EACH Destination@MarsArea(HAS NomadsMarker) { Placement&lt;Destination@MarsArea&gt; }\]. |

Pets declaration:

```pets
CLASS MarsNomads : ActionCard, ActiveCard {
  cost = 13
  This: NomadsMarker<LandArea(HAS MAX 0 Occupant)>
  -> NomadsMarker<LandArea(HAS MAX 0 Occupant, HAS Neighbor<NomadsMarker<Owner>>, NOT Source@LandArea)> FROM NomadsMarker<Owner, Source@LandArea> THEN EACH Destination@MarsArea(HAS NomadsMarker) { Placement<Destination@MarsArea> }
}
```

### Martian Lumber Corp

Class: `MartianLumberCorp`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have 2 greenery tiles. Increase your plant production 1 step. | Effect: When playing a building tag, plants may be used as 3 M€ each. |
| Generated text | Requires that you have 2 greenery tiles. Increase your plant production 1 step. | Effect: When you play a building tag, plants may be used as 3 M€ each. |

Pets declaration:

```pets
CLASS MartianLumberCorp : ActiveCard {
  cost = 6
  requirement = HAS "2 GreeneryTile"
  This:: 3 GrantedResourceValue<Class<Plant>, This>
  This:: BuildingTag<This>, PlantTag<This>
  This: PROD[Plant]
  PayingFor<Class<BuildingTag>>:: Accepting<Class<Plant>>
}
```

### Meat Industry

Class: `MeatIndustry`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: When you gain an animal to ANY CARD, gain 2 M€. |
| Generated text | — | Effect: When you add an animal to any card, gain 2 M€. |

Pets declaration:

```pets
CLASS MeatIndustry : ActiveCard {
  cost = 5
  This:: BuildingTag<This>
  Animal: 2 MC
}
```

### Meltworks

Class: `Meltworks`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Spend 5 heat to gain 3 steel. |
| Generated text | — | Action: Spend 5 heat to gain 3 steel. |

Pets declaration:

```pets
CLASS Meltworks : ActionCard, ActiveCard {
  cost = 4
  This:: BuildingTag<This>
  5 Heat -> 3 Steel
}
```

### Mercurian Alloys

Class: `MercurianAlloys`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 science tags. | Effect: Your titanium resources are worth 1 M€ extra. |
| Generated text | Requires 2 science tags. | Effect: Each titanium you pay is worth 1 M€ extra. |

Pets declaration:

```pets
CLASS MercurianAlloys : ActiveCard {
  cost = 3
  requirement = HAS "2 ScienceTag"
  This:: GrantedResourceValue<Class<Titanium>, This>
  This:: SpaceTag<This>
}
```

### Mohole Lake

Class: `MoholeLake`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place an ocean tile. Raise temperature 1 step. Gain 3 plants. | Action: Add a microbe or animal to ANOTHER card. |
| Generated text | Place an ocean tile. Raise temperature 1 step. Gain 3 plants. | Action: Add 1 microbe or 1 animal to another card. |

Pets declaration:

```pets
CLASS MoholeLake : ActionCard, ActiveCard {
  cost = 31
  This:: BuildingTag<This>
  This: OceanTile<>, TemperatureStep, 3 Plant
  -> Microbe OR Animal
}
```

### Neptunian Power Consultants

Class: `NeptunianPowerConsultants`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | 1 VP per hydroelectric resource on this card. | Effect: When any ocean is placed, you MAY spend 5 M€ (steel may be used), to raise your energy production 1 step and add 1 hydroelectric resource here. |
| Generated text | \[NeptunianOption&lt;This&gt;\]. 1 VP per hydroelectric resource on this card. | Effect: When any ocean tile is placed, \[UseAction&lt;NeptunianOption&lt;This&gt;&gt;?\]. |

Pets declaration:

```pets
CLASS NeptunianPowerConsultants : ActiveCard, ResourceCard<Class<Hydroelectric>> {
  cost = 14
  This:: NeptunianOption<This>
  This:: PowerTag<This>
  OceanTile BY Anyone: UseAction<NeptunianOption<This>>?
  End: VictoryPoint / Hydroelectric<This>
}
```

### Orbital Cleanup

Class: `OrbitalCleanup`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 2 steps. | Action: Gain 1 M€ per science tag you have. |
| Generated text | Decrease your M€ production 2 steps. | Action: Gain 1 M€ per science tag you have. |

Pets declaration:

```pets
CLASS OrbitalCleanup : ActionCard, ActiveCard {
  cost = 14
  This:: EarthTag<This>, SpaceTag<This>
  This: PROD[-2 MC]
  End: 2 VictoryPoint
  -> MC / ScienceTag
}
```

### Outdoor Sports

Class: `OutdoorSports`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires any city adjacent to ocean. Increase your M€ production 2 steps. | — |
| Generated text | Requires a city tile in play next to an ocean tile. Increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS OutdoorSports : AutomatedCard {
  cost = 8
  requirement = HAS "Adjacency<CityTile<Anyone>, OceanTile>"
  This: PROD[2 MC]
}
```

### Penguins

Class: `Penguins`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 8 ocean. 1 VP for each animal on this card. | Action: Add 1 animal to this card. |
| Generated text | Requires 8 ocean tiles. 1 VP per animal on this card. | Action: Add 1 animal to this card. |

Pets declaration:

```pets
CLASS Penguins : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 7
  requirement = HAS "8 OceanTile"
  This:: AnimalTag<This>
  End: VictoryPoint / Animal<This>
  -> Animal<This>
}
```

### Potatoes

Class: `Potatoes`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you lose 2 plants. Increase your M€ production 2 steps. | — |
| Generated text | Remove 2 plants. Increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS Potatoes : AutomatedCard {
  cost = 2
  This:: PlantTag<This>
  This: -2 Plant, PROD[2 MC]
}
```

### Project Inspection

Class: `ProjectInspection`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | USE A CARD ACTION THAT HAS ALREADY BEEN USED THIS GENERATION | — |
| Generated text | Use an action from an action card that has an action-used marker. | — |

Pets declaration:

```pets
CLASS ProjectInspection : EventCard {
  cost = 0
  This:: EventTag<This>
  This: UseAction<ActionCard(HAS ActionUsedMarker)>
}
```

### Protected Growth

Class: `ProtectedGrowth`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Oxygen must be 7% or less. Gain 1 plant per power tag you have. | — |
| Generated text | Requires 7% oxygen or less. Gain 1 plant per power tag you have. | — |

Pets declaration:

```pets
CLASS ProtectedGrowth : EventCard {
  cost = 2
  requirement = HAS "MAX 7 OxygenStep"
  This:: PlantTag<This>, EventTag<This>
  This: Plant / PowerTag
}
```

### Public Baths

Class: `PublicBaths`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 6 oceans. Gain 6 M€. | — |
| Generated text | Requires 6 ocean tiles. Gain 6 M€. | — |

Pets declaration:

```pets
CLASS PublicBaths : AutomatedCard {
  cost = 6
  requirement = HAS "6 OceanTile"
  This:: BuildingTag<This>
  This: 6 MC
  End: VictoryPoint
}
```

### Public Plans

Class: `PublicPlans`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | REVEAL ANY NUMBER OF OTHER CARDS FROM YOUR HAND (YOUR OPPONENTS MAY INSPECT THEM). GAIN 1 M€ FOR EACH REVEALED CARD. | — |
| Generated text | Reveal any number of cards from your hand, then gain 1 M€ per revealed card. | — |

Pets declaration:

```pets
CLASS PublicPlans : EventCard {
  cost = 7
  This:: EventTag<This>
  This: MC? / ProjectCard
  End: VictoryPoint
}
```

### Red Ships

Class: `RedShips`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4% oxygen or more. | Action: Gain 1 M€ for each CITY AND SPECIAL TILE ADJACENT TO OCEAN, regardless of owner. |
| Generated text | Requires 4% oxygen. | Action: Gain 1 M€ per city tile in play on areas next to at least 1 ocean tile or special tile in play on areas next to at least 1 ocean tile. |

Pets declaration:

```pets
CLASS RedShips : ActionCard, ActiveCard {
  cost = 2
  requirement = HAS "4 OxygenStep"
  -> MC / (CityTile<Anyone, MarsArea(HAS Neighbor<OceanTile>)> OR SpecialTile<Anyone, MarsArea(HAS Neighbor<OceanTile>)>)
}
```

### Rego Plastics

Class: `RegoPlastics`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: Your steel resources are worth 1 M€ extra. |
| Generated text | — | Effect: Each steel you pay is worth 1 M€ extra. |

Pets declaration:

```pets
CLASS RegoPlastics : ActiveCard {
  cost = 10
  This:: GrantedResourceValue<Class<Steel>, This>
  This:: BuildingTag<This>
  End: VictoryPoint
}
```

### Robot Pollinators

Class: `RobotPollinators`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4% oxygen. Increase your plant production 1 step. Gain 1 plant for every plant tag you have. | — |
| Generated text | Requires 4% oxygen. Increase your plant production 1 step. Gain 1 plant per plant tag you have. | — |

Pets declaration:

```pets
CLASS RobotPollinators : AutomatedCard {
  cost = 9
  requirement = HAS "4 OxygenStep"
  This: PROD[Plant], Plant / PlantTag
}
```

### Saturn Surfing

Class: `SaturnSurfing`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 1 floater here for every Earth tag you have, including this. | Action: Spend 1 floater from here to gain 1 M€ for each floater here, INCLUDING THE PAID FLOATER (max 5). |
| Generated text | Add 1 floater to this card per Earth tag you have (including this). | Action: Spend 1 floater from this card to gain 1 M€ per floater on this card (max 4) and gain 1 M€. |

Pets declaration:

```pets
CLASS SaturnSurfing : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 13
  This:: JovianTag<This>, EarthTag<This>
  This: Floater<This> / EarthTag
  End: VictoryPoint
  Floater<This> -> MC / Floater<This> MAX 4, MC
}
```

### 16 Psyche

Class: `SixteenPsyche`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase titanium production 2 steps. Gain 3 titanium. | — |
| Generated text | Increase your titanium production 2 steps. Gain 3 titanium. | — |

Pets declaration:

```pets
CLASS SixteenPsyche : AutomatedCard {
  cost = 31
  This:: SpaceTag<This>
  This: PROD[2 Titanium], 3 Titanium
  End: 2 VictoryPoint
}
```

### Small Asteroid

Class: `SmallAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase temperature 1 step. Remove up to 2 plants from any player. | — |
| Generated text | Raise temperature 1 step. You may remove up to 2 plants from any player. | — |

Pets declaration:

```pets
CLASS SmallAsteroid : EventCard {
  cost = 10
  This:: SpaceTag<This>, EventTag<This>
  This: TemperatureStep, -2 Plant<Anyone>?
}
```

### Snow Algae

Class: `SnowAlgae`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 oceans. Increase your plant production and your heat production 1 step each. | — |
| Generated text | Requires 2 ocean tiles. Increase your plant production and your heat production 1 step each. | — |

Pets declaration:

```pets
CLASS SnowAlgae : AutomatedCard {
  cost = 12
  requirement = HAS "2 OceanTile"
  This:: PlantTag<This>
  This: PROD[Plant, Heat]
}
```

### Soil Enrichment

Class: `SoilEnrichment`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Spend 1 microbe from ANY of your cards to gain 5 plants. | — |
| Generated text | Remove 1 microbe from any card. Gain 5 plants. | — |

Pets declaration:

```pets
CLASS SoilEnrichment : EventCard {
  cost = 6
  This:: MicrobeTag<This>, PlantTag<This>, EventTag<This>
  This: -Microbe, 5 Plant
}
```

### Solar Logistics

Class: `SolarLogistics`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 2 titanium. | Effect: When you play an Earth tag, you pay 2 M€ less. Effect: When any player plays a space event, draw 1 card. |
| Generated text | Gain 2 titanium. | Effect: When you play an Earth tag, you pay 2 M€ less for it. When any space event card is played, draw 1 card. |

Pets declaration:

```pets
CLASS SolarLogistics : ActiveCard {
  cost = 20
  This:: EarthTag<This>, SpaceTag<This>
  This: 2 Titanium
  PayingFor<Class<EarthTag>>:: -2 Owed
  EventCard<Anyone>(HAS SpaceTag): ProjectCard
  End: VictoryPoint
}
```

### Stanford Torus

Class: `StanfordTorus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a city tile IN SPACE, outside and separate from the planet. | — |
| Generated text | Place a city tile on the reserved area outside Mars. | — |

Pets declaration:

```pets
CLASS StanfordTorus : AutomatedCard {
  cost = 12
  This:: CityTag<This>, SpaceTag<This>
  This: CityTile<StanfordTorus_RemoteArea>
  End: 2 VictoryPoint
}
```

### Static Harvesting

Class: `StaticHarvesting`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 or less ocean tiles. Increase your energy production 1 step. Gain 1 M€ per building tag you have. | — |
| Generated text | Requires 3 or fewer ocean tiles. Increase your energy production 1 step. Gain 1 M€ per building tag you have. | — |

Pets declaration:

```pets
CLASS StaticHarvesting : AutomatedCard {
  cost = 5
  requirement = HAS "MAX 3 OceanTile"
  This:: PowerTag<This>
  This: PROD[Energy], MC / BuildingTag
}
```

### Sterling Vents

Class: `SterlingVents`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease heat production 2 steps. Increase energy production 2 steps. | — |
| Generated text | Decrease your heat production 2 steps and increase your energy production 2 steps. | — |

Pets declaration:

```pets
CLASS SterlingVents : AutomatedCard {
  cost = 5
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[-2 Heat, 2 Energy]
  End: VictoryPoint
}
```

### St Joseph Of Cupertino Mission

Class: `StJosephOfCupertinoMission`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | 1 VP per cathedral in play. | Action: Spend 5 M€ (steel may be used) to place a cathedral (silver cube) on a city tile. Max 1 per city. THE CITY OWNER MAY PAY 2 M€ TO DRAW 1 CARD. |
| Generated text | \[CathedralOption\]. 1 VP per cathedral in play. | Action: Spend 5 M€ (steel may be used) to \[Cathedral&lt;CityTile&lt;Anyone&gt;&gt;\]. |

Pets declaration:

```pets
CLASS StJosephOfCupertinoMission : ActionCard, ActiveCard {
  cost = 7
  This: CathedralOption
  UseAction<This>:: Accepting<Class<Steel>>
  End: VictoryPoint / Cathedral
  5 MC -> Cathedral<CityTile<Anyone>>
}
```

### Sub-Crust Measurements

Class: `SubCrustMeasurements`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 science tags. | Action: Draw a card. |
| Generated text | Requires 2 science tags. | Action: Draw 1 card. |

Pets declaration:

```pets
CLASS SubCrustMeasurements : ActionCard, ActiveCard {
  cost = 20
  requirement = HAS "2 ScienceTag"
  This:: EarthTag<This>, ScienceTag<This>, BuildingTag<This>
  End: 2 VictoryPoint
  -> ProjectCard
}
```

### Supercapacitors

Class: `Supercapacitors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase M€ production 1 step. | Effect: CONVERTING ENERGY TO HEAT DURING PRODUCTION IS OPTIONAL FOR EACH ENERGY RESOURCE |
| Generated text | Increase your M€ production 1 step. | Effect: \[-Energy IF ProductionPhase: Energy FROM Heat?\]. |

Pets declaration:

```pets
CLASS Supercapacitors : ActiveCard {
  cost = 4
  This:: PowerTag<This>, BuildingTag<This>
  This: PROD[MC]
  -Energy IF ProductionPhase: Energy FROM Heat?
}
```

### Supermarkets

Class: `Supermarkets`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 city tiles in play. Increase your M€ production 2 steps. | — |
| Generated text | Requires 2 city tiles in play. Increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS Supermarkets : AutomatedCard {
  cost = 9
  requirement = HAS "2 CityTile<Anyone>"
  This: PROD[2 MC]
  End: VictoryPoint
}
```

### Teslaract

Class: `Teslaract`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 1 step. | Action: Decrease 1 energy production to increase your plant production 1 step. |
| Generated text | Raise your terraform rating 1 step. | Action: Decrease your energy production 1 step to increase your plant production 1 step. |

Pets declaration:

```pets
CLASS Teslaract : ActionCard, ActiveCard {
  cost = 14
  This:: PowerTag<This>, BuildingTag<This>
  This: TerraformRating
  PROD[Energy] -> PROD[Plant]
}
```

### Topsoil Contract

Class: `TopsoilContract`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 3 plants. | Effect: When you gain a microbe to ANY CARD, also gain 1 M€. |
| Generated text | Gain 3 plants. | Effect: When you add a microbe to any card, gain 1 M€. |

Pets declaration:

```pets
CLASS TopsoilContract : ActiveCard {
  cost = 8
  This:: EarthTag<This>, MicrobeTag<This>
  This: 3 Plant
  Microbe: MC
}
```

### Vermin

Class: `Vermin`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Each player, including you, gets -1 VP per city they have IF THERE ARE AT LEAST 10 ANIMALS HERE. | Effect: When any city is placed, add 1 animal here. Action: Add 1 animal here, or add 1 microbe to ANOTHER card. |
| Generated text | \[End IF 10 Animal&lt;This&gt;: EACH Player { -VictoryPoint / CityTile }\]. | Action: Add 1 animal to this card or add 1 microbe to another card. / Effect: When any city tile is placed, add 1 animal to this card. |

Pets declaration:

```pets
CLASS Vermin : ActionCard, ActiveCard, ResourceCard<Class<Animal>> {
  cost = 8
  This:: MicrobeTag<This>, AnimalTag<This>
  CityTile<Anyone>: Animal<This>
  End IF 10 Animal<This>: EACH Player { -VictoryPoint / CityTile }
  -> Animal<This> OR Microbe
}
```

### Weather Balloons

Class: `WeatherBalloons`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 1 card. | Action: Add 1 floater here, or spend 1 floater here to gain 1 M€ per city ON MARS. |
| Generated text | Draw 1 card. | Action: Add 1 floater to this card, or spend 1 floater from this card to gain 1 M€ per city tile in play on Mars. |

Pets declaration:

```pets
CLASS WeatherBalloons : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 11
  This:: ScienceTag<This>
  This: ProjectCard
  -> Floater<This>
  Floater<This> -> MC / CityTile<MarsArea, Anyone>
}
```

### Political Alliance

Class: `PoliticalAlliance`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you have 2 Party Leaders. Gain 1 TR. | — |
| Generated text | Requires that you lead 2 parties. Raise your terraform rating 1 step. | — |

Pets declaration:

```pets
CLASS PoliticalAlliance : EventCard {
  cost = 4
  requirement = HAS "2 PartyLeader"
  This:: EventTag<This>
  This: TerraformRating
}
```

## Prelude 2 Card Pack

### Ceres Tech Market

Class: `CeresTechMarket`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 2 M€ per colony you own. | Action: Discard any number of cards from your hand to gain 2 M€ for each discarded card. |
| Generated text | Gain 2 M€ per colony you own. | Action: Discard 1 or more cards to gain twice that amount of M€. |

Pets declaration:

```pets
CLASS CeresTechMarket : ActionCard, ActiveCard {
  cost = 12
  This:: ScienceTag<This>, SpaceTag<This>
  This: 2 MC / Colony
  End: VictoryPoint
  X ProjectCard -> 2X MC
}
```

### Cloud Tourism

Class: `CloudTourism`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step per set of Earth and Venus tags you have. 1 VP per 3 floaters here. | Action: Add 1 floater here. |
| Generated text | Increase your M€ production 1 step per pair of Earth and Venus tags you have. 1 VP per 3 floaters on this card. | Action: Add 1 floater to this card. |

Pets declaration:

```pets
CLASS CloudTourism : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 11
  This:: JovianTag<This>, VenusTag<This>
  This: PROD[MC / EarthTag MAX VenusTag]
  End: VictoryPoint / 3 Floater<This>
  -> Floater<This>
}
```

### Floating Refinery

Class: `FloatingRefinery`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 1 floater here for each Venus tag you have. | Action: Add 1 floater here, or remove 2 floaters from ANY CARD to gain 1 titanium and 2 M€. |
| Generated text | Add 1 floater to this card per Venus tag you have (including this). | Action: Add 1 floater to this card, or spend 2 floaters from any of your cards to gain 1 titanium and 2 M€. |

Pets declaration:

```pets
CLASS FloatingRefinery : ActionCard, ActiveCard, ResourceCard<Class<Floater>> {
  cost = 7
  This:: VenusTag<This>
  This: Floater<This> / VenusTag
  -> Floater<This>
  2 Floater -> Titanium, 2 MC
}
```

### Ishtar Expedition

Class: `IshtarExpedition`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires Venus 10%. Gain 3 titanium and draw 2 Venus cards. | — |
| Generated text | Requires Venus 10%. Gain 3 titanium. Draw 2 Venus cards. | — |

Pets declaration:

```pets
CLASS IshtarExpedition : EventCard {
  cost = 6
  requirement = HAS "5 VenusStep"
  This:: VenusTag<This>, EventTag<This>
  This: 3 Titanium, 2 SearchForCard<TagFilter<Class<VenusTag>>>
}
```

### Microgravity Nutrition

Class: `MicrogravityNutrition`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 1 step for each colony you have. | — |
| Generated text | Increase your M€ production 1 step per colony you own. | — |

Pets declaration:

```pets
CLASS MicrogravityNutrition : AutomatedCard {
  cost = 11
  This:: MicrobeTag<This>, PlantTag<This>
  This: PROD[MC / Colony]
  End: VictoryPoint
}
```

### Soil Studies

Class: `SoilStudies`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that temperature is -4°C or colder. Gain 1 plant for each Venus tag, plant tag (including this), and colony you have. | — |
| Generated text | Requires -4°C or colder. Gain 1 plant per Venus tag you have, plant tag you have (including this), or colony you own. | — |

Pets declaration:

```pets
CLASS SoilStudies : EventCard {
  cost = 13
  requirement = HAS "MAX 13 TemperatureStep"
  This:: MicrobeTag<This>, PlantTag<This>, EventTag<This>
  This: Plant / (VenusTag OR PlantTag OR Colony)
}
```

### Stratospheric Expedition

Class: `StratosphericExpedition`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 2 floaters to ANY CARD. Draw 2 Venus cards. | — |
| Generated text | Add 2 floaters to another card. Draw 2 Venus cards. | — |

Pets declaration:

```pets
CLASS StratosphericExpedition : EventCard {
  cost = 12
  This:: VenusTag<This>, SpaceTag<This>, EventTag<This>
  This: 2 Floater, 2 SearchForCard<TagFilter<Class<VenusTag>>>
  End: VictoryPoint
}
```

### Unexpected Application

Class: `UnexpectedApplication`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Discard 1 card to terraform Venus 1 step. | — |
| Generated text | Discard 1 card to raise Venus 1 step. | — |

Pets declaration:

```pets
CLASS UnexpectedApplication : EventCard {
  cost = 4
  This:: VenusTag<This>, EventTag<This>
  This: -ProjectCard THEN VenusStep
}
```

### Venus Allies

Class: `VenusAllies`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 2 steps. Gain 4 M€ per colony you have. | — |
| Generated text | Raise Venus 2 steps. Gain 4 M€ per colony you own. | — |

Pets declaration:

```pets
CLASS VenusAllies : AutomatedCard {
  cost = 30
  This:: VenusTag<This>, SpaceTag<This>
  This: 2 VenusStep, 4 MC / Colony
  End: 2 VictoryPoint
}
```

### Venus Orbital Survey

Class: `VenusOrbitalSurvey`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: REVEAL THE TOP 2 CARDS, TAKE ANY VENUS CARDS TO HAND FOR FREE. ANY OTHER CARD YOU EITHER BUY OR DISCARD |
| Generated text | — | Action: Reveal 2 project cards. Draw any Venus cards for free. You may buy each other card. |

Pets declaration:

```pets
CLASS VenusOrbitalSurvey : ActionCard, ActiveCard {
  cost = 18
  This:: VenusTag<This>, SpaceTag<This>
  -> SearchForCard<TagFilter<Class<VenusTag>>> OR BuyCard?, SearchForCard<TagFilter<Class<VenusTag>>> OR BuyCard?
}
```

### Venus Shuttles

Class: `VenusShuttles`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 2 floaters to ANY VENUS CARD. | Action: Spend 12 M€ to raise Venus 1 step. The cost of this action is REDUCED BY 1 FOR EACH VENUS TAG you have. |
| Generated text | Add 2 floaters to a Venus card. | Action: Spend 12 M€ to raise Venus 1 step. This cost is reduced by 1 M€ per Venus tag you have. |

Pets declaration:

```pets
CLASS VenusShuttles : ActionCard, ActiveCard {
  cost = 9
  This:: VenusTag<This>
  This: 2 Floater<CardFront(HAS VenusTag)>
  MC / 12 - VenusTag -> VenusStep
}
```

### Venus Trade Hub

Class: `VenusTradeHub`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 Venus tags. | Effect: When you trade, gain 3 M€. |
| Generated text | Requires 2 Venus tags. | Effect: When you trade, gain 3 M€. |

Pets declaration:

```pets
CLASS VenusTradeHub : ActiveCard {
  cost = 12
  requirement = HAS "2 VenusTag"
  This:: VenusTag<This>, SpaceTag<This>
  Trade: 3 MC
  End: VictoryPoint
}
```

### Colonial Envoys

Class: `ColonialEnvoys`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Unity is ruling or that you have 2 delegates there. Place 1 delegate for each colony you have. YOU MAY PLACE THEM IN SEPARATE PARTIES. | — |
| Generated text | Requires that you meet the party requirement for Unity. Place a delegate per colony you own. | — |

Pets declaration:

```pets
CLASS ColonialEnvoys : EventCard {
  cost = 4
  requirement = HAS "PartyRequirement<Unity>"
  This:: EventTag<This>
  This: EACH Colony<Owner> { PartyDelegate }
}
```

### Colonial Representation

Class: `ColonialRepresentation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 3 M€ per colony you have. | Effect: You have influence +1. |
| Generated text | Gain 3 M€ per colony you own. | Effect: When influence is counted, gain 1 influence. |

Pets declaration:

```pets
CLASS ColonialRepresentation : ActiveCard {
  cost = 10
  This: 3 MC / Colony
  MeasureInfluence:: ColonialRepresentationInfluence
}
```

### Envoys From Venus

Class: `EnvoysFromVenus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 3 Venus tags. Place 2 delegates in 1 party. | — |
| Generated text | Requires 3 Venus tags. Place 2 delegates. | — |

Pets declaration:

```pets
CLASS EnvoysFromVenus : EventCard {
  cost = 1
  requirement = HAS "3 VenusTag"
  This:: VenusTag<This>, EventTag<This>
  This: 2 PartyDelegate
}
```

### Frontier Town

Class: `FrontierTown`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Mars First is ruling or that you have 2 delegates there. Decrease your energy production 1 step. Place a city tile. GAIN THE PRINTED PLACEMENT BONUS 2 ADDITIONAL TIMES. | — |
| Generated text | Requires that you meet the party requirement for Mars First. Decrease your energy production 1 step. Place a city tile and gain its placement bonus twice. | — |

Pets declaration:

```pets
CLASS FrontierTown : AutomatedCard {
  cost = 11
  requirement = HAS "PartyRequirement<MarsFirst>"
  This:: CityTag<This>, BuildingTag<This>
  This: FrontierTownBonus, PROD[-Energy], CityTile<> THEN -FrontierTownBonus.
}
```

### GHG Shipment

Class: `GhgShipment`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that the Kelvinists are ruling or that you have 2 delegates there. Increase your heat production 1 step and gain 1 heat for each floater you have. | — |
| Generated text | Requires that you meet the party requirement for the Kelvinists party. Increase your heat production 1 step. Gain 1 heat per floater you have. | — |

Pets declaration:

```pets
CLASS GhgShipment : EventCard {
  cost = 3
  requirement = HAS "PartyRequirement<Kelvinists>"
  This:: SpaceTag<This>, EventTag<This>
  This: PROD[Heat], Heat / Floater
}
```

### Jovian Envoys

Class: `JovianEnvoys`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 Jovian tags. Place 2 delegates in 1 party. | — |
| Generated text | Requires 2 Jovian tags. Place 2 delegates. | — |

Pets declaration:

```pets
CLASS JovianEnvoys : EventCard {
  cost = 2
  requirement = HAS "2 JovianTag"
  This:: EventTag<This>
  This: 2 PartyDelegate
}
```

### Red Appeasement

Class: `RedAppeasement`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Reds are ruling or that you have 2 delegates there, AND THAT NO OTHER PLAYER HAS PASSED. Increase M€ production 2 steps. THIS COUNTS AS PASSING; you get no more turns this generation. | — |
| Generated text | Requires that you meet the party requirement for the Reds party and no player has passed. Increase your M€ production 2 steps. Pass. | — |

Pets declaration:

```pets
CLASS RedAppeasement : EventCard {
  cost = 0
  requirement = HAS "PartyRequirement<Reds>, MAX 0 Pass<Anyone>"
  This:: EventTag<This>
  This: PROD[2 MC], Pass
}
```

### Special Permit

Class: `SpecialPermit`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Greens are ruling or that you have 2 delegates there. Steal 4 plants from any player. | — |
| Generated text | Requires that you meet the party requirement for the Greens party. Steal 4 plants from any player. | — |

Pets declaration:

```pets
CLASS SpecialPermit : EventCard {
  cost = 5
  requirement = HAS "PartyRequirement<Greens>"
  This:: PlantTag<This>, EventTag<This>
  This: 4 Plant<Owner FROM Anyone>
}
```

### Sponsoring Nation

Class: `SponsoringNation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 4 Earth tags. Raise TR 3 steps and place 2 delegates. | — |
| Generated text | Requires 4 Earth tags. Raise your terraform rating 3 steps. Place 2 delegates. | — |

Pets declaration:

```pets
CLASS SponsoringNation : AutomatedCard {
  cost = 21
  requirement = HAS "4 EarthTag"
  This:: EarthTag<This>
  This: 3 TerraformRating, 2 PartyDelegate
}
```

### Summit Logistics

Class: `SummitLogistics`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that Scientists are ruling or that you have 2 delegates there. Gain 1 M€ per planet tag and colony you have. Draw 2 cards. | — |
| Generated text | Requires that you meet the party requirement for the Scientists party. Gain 1 M€ per planetary tag you have or colony you own. Draw 2 cards. | — |

Pets declaration:

```pets
CLASS SummitLogistics : AutomatedCard {
  cost = 10
  requirement = HAS "PartyRequirement<Scientists>"
  This:: BuildingTag<This>, SpaceTag<This>
  This: MC / (PlanetaryTag OR Colony), 2 ProjectCard
}
```

### WG Project

Class: `WgProject`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you are chairman. DRAW 3 PRELUDE CARDS AND PLAY 1 OF THEM. Discard the other 2. | — |
| Generated text | Requires that you are chairman. Draw 3 prelude cards, then play one of them, then discard the other 2. | — |

Pets declaration:

```pets
CLASS WgProject : AutomatedCard {
  cost = 9
  requirement = HAS "Chairman"
  This:: EarthTag<This>
  This: PreludeCard THEN PlayCard<Class<PreludeCard>>
}
```

## Replay-only models

### Research Coordination

Class: `FakeResearchCoordination`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | After being played, when you perform an action, the wild tag counts as any tag of your choice. | — |
| Generated text | \[FakeWildTag&lt;This&gt;\]. | — |

Pets declaration:

```pets
CLASS FakeResearchCoordination : AutomatedCard {
  cost = 4
  This:: FakeWildTag<This>
}
```

### Banned Delegate

Class: `FakeBannedDelegate`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you are Chairman. Remove any NON-LEADER delegate. | — |
| Generated text | Requires that you are chairman. \[Audit\]. \[FakeBannedDelegateRemoval&lt;Party, Anyone&gt;. / (PartyDelegate&lt;Party, Anyone&gt; - PartyLeader&lt;Party, Anyone&gt;) MAX 1\]. | — |

Pets declaration:

```pets
"Banned Delegate without immediate party-leader and Dominant-party changes"
CLASS FakeBannedDelegate : EventCard {
  cost = 0
  requirement = HAS "Chairman"
  This:: EventTag<This>
  This:: Audit
  This: FakeBannedDelegateRemoval<Party, Anyone>. / (PartyDelegate<Party, Anyone> - PartyLeader<Party, Anyone>) MAX 1
}
```

### L1 Trade Terminal

Class: `FakeL1TradeTerminal`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add a resource to 3 different cards that already have resources. | Effect: When you trade, you may first increase that colony track 2 steps. |
| Generated text | \[Floater&lt;FloatingHabs&gt;\]. \[Floater&lt;AerialMappers&gt;\]. \[Floater&lt;FloatingRefinery&gt;\]. | Effect: When you trade, \[TradeBarrier&lt;@ColonyTile&gt;\]. \[Trade&lt;@ColonyTile&gt;: (2 ColonyProduction&lt;@ColonyTile&gt; OR Ok) THEN -TradeBarrier&lt;@ColonyTile&gt;\]. |

Pets declaration:

```pets
"L1 Trade Terminal with the replay's three resource destinations fixed"
CLASS FakeL1TradeTerminal : ActiveCard {
  cost = 25
  This:: SpaceTag<This>
  This: Floater<FloatingHabs>, Floater<AerialMappers>, Floater<FloatingRefinery>
  Trade<@ColonyTile>:: TradeBarrier<@ColonyTile>
  Trade<@ColonyTile>: (2 ColonyProduction<@ColonyTile> OR Ok) THEN -TradeBarrier<@ColonyTile>
  End: 2 VictoryPoint
}
```

### Self-Replicating Robots

Class: `FakeSelfReplicatingRobots`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires 2 science tags. | Action: Reveal and place a SPACE OR BUILDING card here from hand, and place 2 resources on it, OR double the resourses on a card here. Effect: Cards here may be played as if from hand with its cost reduced by the number of resources on it. |
| Generated text | Requires 2 science tags. | Action: \[StageForReplicatedProject&lt;Class&lt;CardFront&gt;&gt;\], or \[ReplicateForStagedProject&lt;Class&lt;CardFront&gt;(HAS RobotUnit&lt;Class&lt;CardFront&gt;&gt;)&gt;\]. / Effect: \[PlayCard&lt;Class&lt;CardBack&gt;, Class&lt;@CardFront&gt;(HAS RobotUnit&lt;Class&lt;@CardFront&gt;&gt;)&gt;: ProjectCard\]. |

Pets declaration:

```pets
"Self-Replicating Robots with printed-tag selection delegated in follow mode"
CLASS FakeSelfReplicatingRobots : ActionCard, ActiveCard {
  cost = 7
  requirement = HAS "2 ScienceTag"
  PlayCard<Class<CardBack>, Class<@CardFront>(HAS RobotUnit<Class<@CardFront>>)>: ProjectCard
  -> StageForReplicatedProject<Class<CardFront>>
  -> ReplicateForStagedProject<Class<CardFront>(HAS RobotUnit<Class<CardFront>>)>
}
```
