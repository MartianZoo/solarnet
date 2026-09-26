# Preludes: printed and generated wording

[All categories](README.md) · 71 entries

Printed text: [wording evidence](../../src/jvm/dev/martianzoo/tfm/text/english-published-wording-evidence.tsv).
Generated text comes directly from the current English renderer. See the [reading notes](README.md#reading-the-comparisons).

## Prelude 1 Card Pack

### Acquired Space Agency

Class: `AcquiredSpaceAgency`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 6 titanium. Reveal cards from the deck until you have revealed 2 space cards. Take those into hand, and discard the rest. | — |
| Generated text | Gain 6 titanium. Draw 2 space cards. | — |

Pets declaration:

```pets
CLASS AcquiredSpaceAgency : CardFront<Class<PreludeCard>> {
  cost = 0
  This: 6 Titanium, 2 SearchForCard<TagFilter<Class<SpaceTag>>>
}
```

### Allied Bank

Class: `AlliedBank`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 4 steps. Gain 3 M€. | — |
| Generated text | Increase your M€ production 4 steps. Gain 3 M€. | — |

Pets declaration:

```pets
CLASS AlliedBank : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: EarthTag<This>
  This: PROD[4 MC], 3 MC
}
```

### Aquifer Turbines

Class: `AquiferTurbines`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place an ocean tile. Increase your energy production 2 steps. Remove 3 M€. | — |
| Generated text | Place an ocean tile. Increase your energy production 2 steps. Remove 3 M€. | — |

Pets declaration:

```pets
CLASS AquiferTurbines : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: PowerTag<This>
  This: OceanTile<>, PROD[2 Energy], -3 MC
}
```

### Biofuels

Class: `Biofuels`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your plant production and energy production 1 step each. Gain 2 plants. | — |
| Generated text | Increase your plant production and your energy production 1 step each. Gain 2 plants. | — |

Pets declaration:

```pets
CLASS Biofuels : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: MicrobeTag<This>
  This: PROD[Plant, Energy], 2 Plant
}
```

### Biolab

Class: `Biolab`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your plant production 1 step. Draw 3 cards. | — |
| Generated text | Increase your plant production 1 step. Draw 3 cards. | — |

Pets declaration:

```pets
CLASS Biolab : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: ScienceTag<This>
  This: PROD[Plant], 3 ProjectCard
}
```

### Biosphere Support

Class: `BiosphereSupport`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 1 step. Increase your plant production 2 steps. | — |
| Generated text | Decrease your M€ production 1 step and increase your plant production 2 steps. | — |

Pets declaration:

```pets
CLASS BiosphereSupport : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: PlantTag<This>
  This: PROD[-MC, 2 Plant]
}
```

### Business Empire

Class: `BusinessEmpire`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 6 steps. Remove 6 M€. | — |
| Generated text | Increase your M€ production 6 steps. Remove 6 M€. | — |

Pets declaration:

```pets
CLASS BusinessEmpire : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: EarthTag<This>
  This: PROD[6 MC], -6 MC
}
```

### Dome Farming

Class: `DomeFarming`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your plant production 1 step. Increase your M€ production 2 steps. | — |
| Generated text | Increase your plant production 1 step and your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS DomeFarming : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: PlantTag<This>, BuildingTag<This>
  This: PROD[Plant, 2 MC]
}
```

### Donation

Class: `Donation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 21 M€. | — |
| Generated text | Gain 21 M€. | — |

Pets declaration:

```pets
CLASS Donation : CardFront<Class<PreludeCard>> {
  cost = 0
  This: 21 MC
}
```

### Early Settlement

Class: `EarlySettlement`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a city tile. Increase your plant production 1 step. | — |
| Generated text | Place a city tile. Increase your plant production 1 step. | — |

Pets declaration:

```pets
CLASS EarlySettlement : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: CityTag<This>, BuildingTag<This>
  This: CityTile<>, PROD[Plant]
}
```

### Ecology Experts

Class: `EcologyExperts`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your plant production 1 step. PLAY A CARD FROM HAND, IGNORING GLOBAL REQUIREMENTS | — |
| Generated text | Increase your plant production 1 step. Play a card from hand, ignoring global requirements. | — |

Pets declaration:

```pets
CLASS EcologyExperts : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: PlantTag<This>, MicrobeTag<This>
  This: PROD[Plant], PlayCard THEN -Required / Required
}
```

### Excentric Sponsor

Class: `ExcentricSponsor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | PLAY A CARD FROM HAND, REDUCING ITS COST BY 25 M€ | — |
| Generated text | Play a card from hand, reducing its cost by 25 M€. | — |

Pets declaration:

```pets
CLASS ExcentricSponsor : CardFront<Class<PreludeCard>> {
  cost = 0
  This: PlayCard<Class<ProjectCard>> THEN -25 Owed
}
```

### Experimental Forest

Class: `ExperimentalForest`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a greenery tile and increase oxygen 1 step. Reveal cards from the deck until you have revealed 2 plant-tag cards. Take these into your hand, and discard the rest. | — |
| Generated text | Place a greenery tile. Draw 2 plant cards. | — |

Pets declaration:

```pets
CLASS ExperimentalForest : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: PlantTag<This>
  This: DefaultGreeneryTile, 2 SearchForCard<TagFilter<Class<PlantTag>>>
}
```

### Galilean Mining

Class: `GalileanMining`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 2 steps. Remove 5 M€. | — |
| Generated text | Increase your titanium production 2 steps. Remove 5 M€. | — |

Pets declaration:

```pets
CLASS GalileanMining : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: JovianTag<This>
  This: PROD[2 Titanium], -5 MC
}
```

### Great Aquifer

Class: `GreatAquifer`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place 2 ocean tiles. | — |
| Generated text | Place 2 ocean tiles. | — |

Pets declaration:

```pets
CLASS GreatAquifer : CardFront<Class<PreludeCard>> {
  cost = 0
  This: 2 OceanTile<>
}
```

### Huge Asteroid

Class: `HugeAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 3 steps. Remove 5 M€. | — |
| Generated text | Raise temperature 3 steps. Remove 5 M€. | — |

Pets declaration:

```pets
CLASS HugeAsteroid : CardFront<Class<PreludeCard>> {
  cost = 0
  This: 3 TemperatureStep, -5 MC
}
```

### Io Research Outpost

Class: `IoResearchOutpost`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 1 step. Draw 1 card. | — |
| Generated text | Increase your titanium production 1 step. Draw 1 card. | — |

Pets declaration:

```pets
CLASS IoResearchOutpost : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: ScienceTag<This>, JovianTag<This>
  This: PROD[Titanium], ProjectCard
}
```

### Loan

Class: `Loan`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 2 steps. Gain 30 M€. | — |
| Generated text | Decrease your M€ production 2 steps. Gain 30 M€. | — |

Pets declaration:

```pets
CLASS Loan : CardFront<Class<PreludeCard>> {
  cost = 0
  This: PROD[-2 MC], 30 MC
}
```

### Martian Industries

Class: `MartianIndustries`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production and steel production 1 step each. Gain 6 M€. | — |
| Generated text | Increase your energy production and your steel production 1 step each. Gain 6 M€. | — |

Pets declaration:

```pets
CLASS MartianIndustries : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: PROD[Energy, Steel], 6 MC
}
```

### Metal-Rich Asteroid

Class: `MetalRichAsteroid`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 1 step. Gain 4 titanium, and 4 steel. | — |
| Generated text | Raise temperature 1 step. Gain 4 titanium and 4 steel. | — |

Pets declaration:

```pets
CLASS MetalRichAsteroid : CardFront<Class<PreludeCard>> {
  cost = 0
  This: TemperatureStep, 4 Titanium, 4 Steel
}
```

### Metals Company

Class: `MetalsCompany`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production, steel production, and titanium production 1 step each. | — |
| Generated text | Increase your M€ production, your steel production, and your titanium production 1 step each. | — |

Pets declaration:

```pets
CLASS MetalsCompany : CardFront<Class<PreludeCard>> {
  cost = 0
  This: PROD[MC, Steel, Titanium]
}
```

### Mining Operations

Class: `MiningOperations`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your steel production 2 steps. Gain 4 steel. | — |
| Generated text | Increase your steel production 2 steps. Gain 4 steel. | — |

Pets declaration:

```pets
CLASS MiningOperations : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: PROD[2 Steel], 4 Steel
}
```

### Mohole

Class: `Mohole`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your heat production 3 steps. Gain 3 heat. | — |
| Generated text | Increase your heat production 3 steps. Gain 3 heat. | — |

Pets declaration:

```pets
CLASS Mohole : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: PROD[3 Heat], 3 Heat
}
```

### Mohole Excavation

Class: `MoholeExcavation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your steel production 1 step, and your heat production 2 steps. Gain 2 heat. | — |
| Generated text | Increase your steel production 1 step and your heat production 2 steps. Gain 2 heat. | — |

Pets declaration:

```pets
CLASS MoholeExcavation : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: PROD[Steel, 2 Heat], 2 Heat
}
```

### Nitrogen Shipment

Class: `NitrogenShipment`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your terraform rating 1 step. Increase your plant production 1 step. Gain 5 M€. | — |
| Generated text | Raise your terraform rating 1 step. Increase your plant production 1 step. Gain 5 M€. | — |

Pets declaration:

```pets
CLASS NitrogenShipment : CardFront<Class<PreludeCard>> {
  cost = 0
  This: TerraformRating, PROD[Plant], 5 MC
}
```

### Orbital Construction Yard

Class: `OrbitalConstructionYard`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium produciton 1 step. Gain 4 titanium. | — |
| Generated text | Increase your titanium production 1 step. Gain 4 titanium. | — |

Pets declaration:

```pets
CLASS OrbitalConstructionYard : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: SpaceTag<This>
  This: PROD[Titanium], 4 Titanium
}
```

### Polar Industries

Class: `PolarIndustries`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place 1 ocean tile. Increase your heat production 2 steps. | — |
| Generated text | Place an ocean tile. Increase your heat production 2 steps. | — |

Pets declaration:

```pets
CLASS PolarIndustries : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: OceanTile<>, PROD[2 Heat]
}
```

### Power Generation

Class: `PowerGeneration`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 3 steps. | — |
| Generated text | Increase your energy production 3 steps. | — |

Pets declaration:

```pets
CLASS PowerGeneration : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: PowerTag<This>
  This: PROD[3 Energy]
}
```

### Self-Sufficient Settlement

Class: `SelfSufficientSettlement`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a city tile. Increase your M€ production 2 steps. | — |
| Generated text | Place a city tile. Increase your M€ production 2 steps. | — |

Pets declaration:

```pets
CLASS SelfSufficientSettlement : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: CityTag<This>, BuildingTag<This>
  This: CityTile<>, PROD[2 MC]
}
```

### Smelting Plant

Class: `SmeltingPlant`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise oxygen 2 steps. Gain 5 steel. | — |
| Generated text | Raise oxygen 2 steps. Gain 5 steel. | — |

Pets declaration:

```pets
CLASS SmeltingPlant : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: 2 OxygenStep, 5 Steel
}
```

### Society Support

Class: `SocietySupport`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Decrease your M€ production 1 step. Increase your plant production, energy production, and heat production 1 step each. | — |
| Generated text | Decrease your M€ production 1 step and increase your plant production, your energy production, and your heat production 1 step each. | — |

Pets declaration:

```pets
CLASS SocietySupport : CardFront<Class<PreludeCard>> {
  cost = 0
  This: PROD[-MC, Plant, Energy, Heat]
}
```

### Supplier

Class: `Supplier`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 2 steps. Gain 4 steel. | — |
| Generated text | Increase your energy production 2 steps. Gain 4 steel. | — |

Pets declaration:

```pets
CLASS Supplier : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: PowerTag<This>
  This: PROD[2 Energy], 4 Steel
}
```

### Supply Drop

Class: `SupplyDrop`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 3 titanium, 8 steel, and 3 plants. | — |
| Generated text | Gain 3 titanium, 8 steel, and 3 plants. | — |

Pets declaration:

```pets
CLASS SupplyDrop : CardFront<Class<PreludeCard>> {
  cost = 0
  This: 3 Titanium, 8 Steel, 3 Plant
}
```

### UNMI Contractor

Class: `UnmiContractor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your terraform rating 3 steps. Draw 1 card. | — |
| Generated text | Raise your terraform rating 3 steps. Draw 1 card. | — |

Pets declaration:

```pets
CLASS UnmiContractor : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: EarthTag<This>
  This: 3 TerraformRating, ProjectCard
}
```

## Promo Card Pack

### Albedo Plants

Class: `AlbedoPlants`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase plant production 1 step. Gain 1 plant. | Effect: When you play a plant tag, including this, gain 3 heat. |
| Generated text | Increase your plant production 1 step. Gain 1 plant. | Effect: When you play a plant tag (including this), gain 3 heat. |

Pets declaration:

```pets
CLASS AlbedoPlants : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: PlantTag<This>
  This: PROD[Plant], Plant
  PlantTag: 3 Heat
}
```

### Anti Desertification Techniques

Class: `AntiDesertificationTechniques`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your plant and steel production 1 step each. Gain 3 M€. | — |
| Generated text | Gain 3 M€. Increase your plant production and your steel production 1 step each. | — |

Pets declaration:

```pets
CLASS AntiDesertificationTechniques : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: MicrobeTag<This>, PlantTag<This>
  This: 3 MC, PROD[Plant, Steel]
}
```

### Boom Town

Class: `BoomTown`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a city tile ON A STEEL OR TITANIUM BONUS. Increase titanium production 2 steps. | Effect: Your titanium is worth 1 M€ less. |
| Generated text | Place a city tile on a land area with a titanium or steel placement bonus. Increase your titanium production 2 steps. | Effect: Your titanium is worth 1 M€ less. |

Pets declaration:

```pets
CLASS BoomTown : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: -BaseResourceValue<Class<Titanium>>
  This:: BuildingTag<This>, CityTag<This>
  This: CityTile<LandArea(HAS PlacementBonus<Class<Metal>>, HAS MAX 0 Neighbor<CityTile<Anyone>>)>, PROD[2 Titanium]
  -This:: BaseResourceValue<Class<Titanium>>
}
```

### Corporate Archives

Class: `CorporateArchives`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 13 M€. LOOK AT THE TOP 7 CARDS FROM THE DECK. TAKE 2 OF THEM INTO HAND AND DISCARD THE OTHER 5 | — |
| Generated text | Gain 13 M€. Look at 7 project cards. Draw 2 of them. | — |

Pets declaration:

```pets
CLASS CorporateArchives : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: ScienceTag<This>
  This: 13 MC, 2 ProjectCard
}
```

### Double Down

Class: `DoubleDown`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | COPY YOUR OTHER PRELUDE'S DIRECT EFFECT | — |
| Generated text | Copy your other Prelude's direct effect. | — |

Pets declaration:

```pets
CLASS DoubleDown : CardFront<Class<PreludeCard>> {
  cost = 0
  This: CopyPrelude
}
```

### Giant Solar Collector

Class: `GiantSolarCollector`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 2 steps. Raise Venus 1 step. | — |
| Generated text | Increase your energy production 2 steps. Raise Venus 1 step. | — |

Pets declaration:

```pets
CLASS GiantSolarCollector : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: PowerTag<This>, SpaceTag<This>
  This: PROD[2 Energy], VenusStep
}
```

### Merger

Class: `Merger`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 4 corporation cards. Play one of them and discard the other 3. Then pay 42 M€. | — |
| Generated text | Draw 4 corporation cards, then discard 3 corporation cards, then play a corporation card. Remove 42 M€. | — |

Pets declaration:

```pets
CLASS Merger : CardFront<Class<PreludeCard>> {
  cost = 0
  This: StandardCorporationCard THEN PlayCard<Class<StandardCorporationCard>, Class<CardFront>(NOT Class<BeginnerCorporation>)>, -42 MC
}
```

### New Partner

Class: `NewPartner`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your M€ production 1 step. Immediately draw 2 Prelude cards. Play 1 of them, and discard the other. | — |
| Generated text | Increase your M€ production 1 step. Draw 2 prelude cards, then discard 1 prelude card, then play a prelude card. | — |

Pets declaration:

```pets
CLASS NewPartner : CardFront<Class<PreludeCard>> {
  cost = 0
  This: PROD[MC], PreludeCard THEN PlayCard<Class<PreludeCard>>
}
```

### Strategic Base Planning

Class: `StrategicBasePlanning`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Requires that you spend 3 M€. Place 1 colony and 1 city tile. | — |
| Generated text | Remove 3 M€. Place a city tile and a colony. | — |

Pets declaration:

```pets
CLASS StrategicBasePlanning : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: CityTag<This>, BuildingTag<This>, SpaceTag<This>
  This: -3 MC, CityTile<>, Colony<>
}
```

## Prelude 2 Card Pack

### Atmospheric Enhancers

Class: `AtmosphericEnhancers`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise temperature 2 steps, or raise oxygen 2 steps, or raise Venus 2 steps. Draw 2 cards with floater icons. | — |
| Generated text | Raise temperature 2 steps, oxygen 2 steps, or Venus 2 steps. Draw 2 cards with floater icons. | — |

Pets declaration:

```pets
CLASS AtmosphericEnhancers : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: VenusTag<This>
  This: 2 TemperatureStep OR 2 OxygenStep OR 2 VenusStep, 2 ProjectCard
}
```

### Board of Directors

Class: `BoardOfDirectors`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 4 director resources here. | Action: DRAW 1 PRELUDE CARD: EITHER DISCARD IT, OR PAY 12 M€ AND REMOVE 1 DIRECTOR RESOURCE FROM HERE TO PLAY IT |
| Generated text | Add 4 director resources to this card. | Action: Draw 1 prelude card. Discard 1 prelude card or pay 12 M€ and remove 1 director resource from this card to play a prelude card. |

Pets declaration:

```pets
CLASS BoardOfDirectors : ActionCard, ResourceCard<Class<Director>, Class<PreludeCard>> {
  cost = 0
  This:: EarthTag<This>
  This: 4 Director<This>
  -> PreludeCard, -PreludeCard OR (-12 MC THEN -Director<This> THEN PlayCard<Class<PreludeCard>>)
}
```

### Colony Trade Hub

Class: `ColonyTradeHub`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your energy production 1 step. Gain 2 titanium. | Effect: When any colony is placed, gain 2 M€. |
| Generated text | Increase your energy production 1 step. Gain 2 titanium. | Effect: When any colony is placed, gain 2 M€. |

Pets declaration:

```pets
CLASS ColonyTradeHub : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: SpaceTag<This>
  This: PROD[Energy], 2 Titanium
  Colony<Anyone>: 2 MC
}
```

### Early Colonization

Class: `EarlyColonization`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place a colony. Gain 3 energy. RAISE ALL COLONY TRACKS 2 STEPS | — |
| Generated text | Place a colony. Gain 3 energy. \[EACH @ColonyTile { 2 ColonyProduction&lt;@ColonyTile&gt; }\]. | — |

Pets declaration:

```pets
CLASS EarlyColonization : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: SpaceTag<This>
  This: Colony<>, 3 Energy, EACH @ColonyTile { 2 ColonyProduction<@ColonyTile> }
}
```

### Floating Trade Hub

Class: `FloatingTradeHub`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Action: Add 2 floaters to ANY card, or remove any number of floaters here to gain that many of one standard resource. |
| Generated text | — | Action: Add 2 floaters to any card, or spend 1 or more floaters from this card to gain the same number of one standard resource. |

Pets declaration:

```pets
CLASS FloatingTradeHub : ActionCard, ResourceCard<Class<Floater>, Class<PreludeCard>> {
  cost = 0
  This:: SpaceTag<This>
  -> 2 Floater
  X Floater<This> -> X StandardResource
}
```

### Focused Organization

Class: `FocusedOrganization`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 1 card and gain 1 standard resource. | Action: Discard 1 card and spend 1 standard resource to draw 1 card and gain 1 standard resource. |
| Generated text | Draw 1 card. Gain 1 standard resource. | Action: Discard 1 card and pay 1 standard resource to draw 1 card and gain 1 standard resource. |

Pets declaration:

```pets
CLASS FocusedOrganization : ActionCard<Class<PreludeCard>> {
  cost = 0
  This: ProjectCard, StandardResource
  -> -ProjectCard THEN -StandardResource THEN ProjectCard THEN StandardResource
}
```

### Industrial Complex

Class: `IndustrialComplex`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Lose 18 M€. INCREASE ALL YOUR PRODUCTIONS THAT ARE LOWER THAN 1, TO 1 | — |
| Generated text | Remove 18 M€. Increase each of your productions below 1 to 1. | — |

Pets declaration:

```pets
CLASS IndustrialComplex : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: -18 MC, EACH Class<@StandardResource> { PROD[@StandardResource / (Class<@StandardResource> OR QuickStartVariant OR ProdOffset<Class<@StandardResource>>) - Production<Class<@StandardResource>>] }
}
```

### Main Belt Asteroids

Class: `MainBeltAsteroids`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Lose 5 M€. 1 VP per 2 asteroids here. | Action: Gain 1 asteroid to ANY CARD. Effect: When gaining an asteroid HERE, gain 1 titanium. |
| Generated text | Remove 5 M€. 1 VP per 2 asteroids on this card. | Action: Add 1 asteroid to any card. / Effect: When you add an asteroid to this card, gain 1 titanium. |

Pets declaration:

```pets
CLASS MainBeltAsteroids : ActionCard, ResourceCard<Class<Asteroid>, Class<PreludeCard>> {
  cost = 0
  This:: SpaceTag<This>
  This: -5 MC
  Asteroid<This>: Titanium
  End: VictoryPoint / 2 Asteroid<This>
  -> Asteroid
}
```

### Old Mining Colony

Class: `OldMiningColony`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your titanium production 1 step. Place 1 colony. Discard 1 card. | — |
| Generated text | Increase your titanium production 1 step. Place a colony. Discard 1 card. | — |

Pets declaration:

```pets
CLASS OldMiningColony : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: SpaceTag<This>
  This: PROD[Titanium], Colony<>, -ProjectCard
}
```

### Planetary Alliance

Class: `PlanetaryAlliance`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 2 steps. Draw 1 Jovian card and 1 Venus card. | — |
| Generated text | Raise your terraform rating 2 steps. Draw 1 Jovian card and 1 Venus card. | — |

Pets declaration:

```pets
CLASS PlanetaryAlliance : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: EarthTag<This>, JovianTag<This>, VenusTag<This>
  This: 2 TerraformRating, SearchForCard<TagFilter<Class<JovianTag>>>, SearchForCard<TagFilter<Class<VenusTag>>>
}
```

### Project Eden

Class: `ProjectEden`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Place 1 ocean tile, 1 city tile, and 1 greenery tile. Discard 3 cards. | — |
| Generated text | Place an ocean tile, a city tile, and a greenery tile. Discard 3 cards. | — |

Pets declaration:

```pets
CLASS ProjectEden : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: CityTag<This>, PlantTag<This>
  This: OceanTile<>, CityTile<>, DefaultGreeneryTile, -3 ProjectCard
}
```

### Recession

Class: `Recession`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | EACH OPPONENT loses 5 M€ and decreases their M€ production 1 step. You gain 10 M€. | — |
| Generated text | Gain 10 M€. Each other player removes 5 M€ and decreases their own M€ production 1 step. | — |

Pets declaration:

```pets
CLASS Recession : CardFront<Class<PreludeCard>> {
  cost = 0
  This: 10 MC, EACH Player(NOT Owner) { -5 MC., PROD[-MC] }
}
```

### Soil Bacteria

Class: `SoilBacteria`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 2 microbe cards and gain 3 plants. | Effect: When playing a plant tag or a microbe tag, including this, gain 1 plant. |
| Generated text | Draw 2 microbe cards. Gain 3 plants. | Effect: When you play a plant tag or a microbe tag (including this), gain 1 plant. |

Pets declaration:

```pets
CLASS SoilBacteria : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: MicrobeTag<This>
  This: 2 SearchForCard<TagFilter<Class<MicrobeTag>>>, 3 Plant
  PlantTag OR MicrobeTag: Plant
}
```

### Space Lanes

Class: `SpaceLanes`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 3 titanium. | Effect: When playing a planet tag, you pay 2 M€ less. |
| Generated text | Gain 3 titanium. | Effect: When you play a planetary tag, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS SpaceLanes : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: SpaceTag<This>
  This: 3 Titanium
  PayingFor<Class<PlanetaryTag>>:: -2 Owed
}
```

### Suitable Infrastructure

Class: `SuitableInfrastructure`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 5 steel. | Effect: Once per action you take, gain 2 M€ if you increase any production(s). |
| Generated text | Gain 5 steel. | Effect: Once per action you take, gain 2 M€ if you increase any production. |

Pets declaration:

```pets
CLASS SuitableInfrastructure : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: 5 Steel
  UseAction<StandardAction>:: SuitableInfrastructureBonus<This>.
  NewTurn IF PreludePhase:: SuitableInfrastructureBonus<This>.
}
```

### Terraforming Deal

Class: `TerraformingDeal`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | — | Effect: Each step your TR is raised, you gain 2 M€. |
| Generated text | — | Effect: When you raise your terraform rating 1 step, gain 2 M€. |

Pets declaration:

```pets
CLASS TerraformingDeal : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: EarthTag<This>
  TerraformRating: 2 MC
}
```

### Venus Contract

Class: `VenusContract`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 1 Venus card. Raise your TR 1 step. | Effect: Each step you raise Venus, gain 3 M€. |
| Generated text | Draw 1 Venus card. Raise your terraform rating 1 step. | Effect: When you raise Venus 1 step, gain 3 M€. |

Pets declaration:

```pets
CLASS VenusContract : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: VenusTag<This>
  This: SearchForCard<TagFilter<Class<VenusTag>>>, TerraformRating
  VenusStep: 3 MC
}
```

### Venus L1 Shade

Class: `VenusL1Shade`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise Venus 3 steps. | — |
| Generated text | Raise Venus 3 steps. | — |

Pets declaration:

```pets
CLASS VenusL1Shade : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: SpaceTag<This>
  This: 3 VenusStep
}
```

### World Government Advisor

Class: `WorldGovernmentAdvisor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 2 steps. Draw 1 card. | Action: RAISE 1 GLOBAL PARAMETER WITHOUT GETTING ANY TR OR OTHER BONUSES |
| Generated text | Raise your terraform rating 2 steps. Draw 1 card. | Action: Raise 1 global parameter without gaining terraform rating or other bonuses. |

Pets declaration:

```pets
CLASS WorldGovernmentAdvisor : ActionCard<Class<PreludeCard>> {
  cost = 0
  This:: EarthTag<This>
  This: 2 TerraformRating, ProjectCard
  -> WorldGovernmentTerraforming
}
```

### Corridors Of Power

Class: `CorridorsOfPower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 1 step and gain 4 M€. | Effect: Each time you become party leader, draw 1 card. |
| Generated text | Raise your terraform rating 1 step. Gain 4 M€. | Effect: When you become a party leader, draw 1 card. |

Pets declaration:

```pets
CLASS CorridorsOfPower : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: EarthTag<This>
  This: TerraformRating, 4 MC
  PartyLeader<Party>:: ProjectCard
}
```

### High Circles

Class: `HighCircles`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 1 step and draw 1 card with a PARTY REQUIREMENT. Place 2 delegates in one party. | Effect: You have +1 influence. |
| Generated text | Raise your terraform rating 1 step. Draw 1 card with a party requirement. Place 2 delegates. | Effect: When influence is counted, gain 1 influence. |

Pets declaration:

```pets
CLASS HighCircles : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: EarthTag<This>
  This: TerraformRating, ProjectCard, 2 PartyDelegate
  MeasureInfluence:: HighCirclesInfluence
}
```

### Rise To Power

Class: `RiseToPower`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Increase your M€ production 3 steps and place 3 delegates. YOU MAY PLACE THEM IN SEPARATE PARTIES. | — |
| Generated text | Increase your M€ production 3 steps. Place a delegate. Place a delegate. Place a delegate. | — |

Pets declaration:

```pets
CLASS RiseToPower : CardFront<Class<PreludeCard>> {
  cost = 0
  This: PROD[3 MC], PartyDelegate, PartyDelegate, PartyDelegate
}
```

## Replay-only models

### Research Network

Class: `FakeResearchNetwork`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Draw 3 cards, and increase your M€ production 1 step. After being played, when you perform an action, the wild tag is any tag of your choice. | — |
| Generated text | \[FakeWildTag&lt;This&gt;\]. Increase your M€ production 1 step. Draw 3 cards. | — |

Pets declaration:

```pets
CLASS FakeResearchNetwork : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: FakeWildTag<This>
  This: PROD[MC], 3 ProjectCard
}
```

### Head Start

Class: `FakeHeadStart`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | GAIN 2 STEEL. GAIN 2 M€ PER PROJECT CARD YOU HAVE IN HAND. IMMEDIATELY TAKE 2 ACTIONS. | — |
| Generated text | \[Audit\]. Gain 2 steel. Gain 2 M€ per card in hand. Use an action. Use an action. | — |

Pets declaration:

```pets
"Head Start with independently interleavable immediate actions"
CLASS FakeHeadStart : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: Audit
  This: 2 Steel, 2 MC / ProjectCard, UseAction<StandardAction>!, UseAction<StandardAction>!
}
```

### Applied Science

Class: `FakeAppliedScience`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Add 6 science resources here. | Action: Remove 1 science resource here to either add 1 resource to ANY CARD WITH A RESOURCE or gain 1 standard resource. |
| Generated text | \[FakeWildTag&lt;This&gt;\]. Add 6 science resources to this card. | Action: Spend 1 science resource from this card to add 1 resource to a card with 1 or more resources on it or gain 1 standard resource. |

Pets declaration:

```pets
CLASS FakeAppliedScience : ActionCard, ResourceCard<Class<Science>, Class<PreludeCard>> {
  cost = 0
  This:: FakeWildTag<This>
  This: 6 Science<This>
  Science<This> -> CardResource<CardFront(HAS CardResource)> OR StandardResource
}
```

### Nobel Prize

Class: `FakeNobelPrize`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 5 M€. Draw 2 cards with requirements. | — |
| Generated text | \[FakeWildTag&lt;This&gt;\]. Gain 5 M€. Draw 2 cards. | — |

Pets declaration:

```pets
CLASS FakeNobelPrize : CardFront<Class<PreludeCard>> {
  cost = 0
  This:: FakeWildTag<This>
  This: 5 MC, 2 ProjectCard
  End: 2 VictoryPoint
}
```

### Preservation Program

Class: `FakePreservationProgram`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Raise your TR 5 steps. | Effect: SKIP THE FIRST TR YOU GAIN IN EACH GENERATION'S ACTION PHASE |
| Generated text | Raise your terraform rating 5 steps. | Effect: \[TerraformRating IF ActionPhase:: FakePreservationTrLost&lt;This&gt;.\]. |

Pets declaration:

```pets
"Preservation Program that reverses the first action-phase TR gain instead of preventing it"
CLASS FakePreservationProgram : CardFront<Class<PreludeCard>> {
  cost = 0
  This: 5 TerraformRating
  TerraformRating IF ActionPhase:: FakePreservationTrLost<This>.
}
```

### Established Methods

Class: `FakeEstablishedMethods`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Gain 30 M€. Then immediately pay and perform 2 standard projects. | — |
| Generated text | Gain 30 M€. Use an action. Use an action. | — |

Pets declaration:

```pets
"Established Methods without its unaffordable-second-project rule"
CLASS FakeEstablishedMethods : CardFront<Class<PreludeCard>> {
  cost = 0
  This: 30 MC, UseAction<StandardAction>!, UseAction<StandardAction>!
}
```
