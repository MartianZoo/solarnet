# Corporations: printed and generated wording

[All categories](README.md) · 53 entries

Printed text: [wording evidence](../../src/jvm/dev/martianzoo/tfm/text/english-published-wording-evidence.tsv).
Generated text comes directly from the current English renderer. See the [reading notes](README.md#reading-the-comparisons).

## Terraforming Mars

### Beginner Corporation

Class: `BeginnerCorporation`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 42 M€. INSTEAD OF CHOOSING FROM 10 CARDS DURING SETUP, YOU GET 10 CARDS FOR FREE. | — |
| Generated text | Gain 42 M€. Draw 10 cards. | — |

Pets declaration:

```pets
"A beginner corporation supplies 42 M€ and all ten starting project cards for free"
ABSTRACT CLASS BeginnerCorporation : CardFront<Class<BeginnerCorporationCard>> {
  cost = 0
  This: 42 MC, 10 ProjectCard
}
```

### Beginner Corporation

Class: `BeginnerCorporation1`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Not transcribed | Not transcribed |
| Generated text | — | — |

Pets declaration:

```pets
CLASS BeginnerCorporation1 : BeginnerCorporation
```

### Beginner Corporation

Class: `BeginnerCorporation2`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Not transcribed | Not transcribed |
| Generated text | — | — |

Pets declaration:

```pets
CLASS BeginnerCorporation2 : BeginnerCorporation
```

### Beginner Corporation

Class: `BeginnerCorporation3`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Not transcribed | Not transcribed |
| Generated text | — | — |

Pets declaration:

```pets
CLASS BeginnerCorporation3 : BeginnerCorporation
```

### Beginner Corporation

Class: `BeginnerCorporation4`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Not transcribed | Not transcribed |
| Generated text | — | — |

Pets declaration:

```pets
CLASS BeginnerCorporation4 : BeginnerCorporation
```

### Beginner Corporation

Class: `BeginnerCorporation5`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | Not transcribed | Not transcribed |
| Generated text | — | — |

Pets declaration:

```pets
CLASS BeginnerCorporation5 : BeginnerCorporation
```

### CrediCor

Class: `CrediCor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 57 M€. | Effect: After you pay for a card or standard project with a basic cost of 20 M€ or more, you gain 4 M€. |
| Generated text | Gain 57 M€. | Effect: When you play a card with a printed cost of 20 M€ or more, or use a standard project with a printed cost of 20 M€ or more, gain 4 M€. |

Pets declaration:

```pets
CLASS CrediCor : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This: 57 MC
  CardFront(HAS 20 cost) OR UseAction<StandardProject(HAS 20 cost)>: 4 MC
}
```

### Ecoline

Class: `Ecoline`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 2 plant production, 3 plants, and 36 M€. | Effect: You may always pay 7 plants, instead of 8, to place 1 greenery. |
| Generated text | Gain 36 M€ and 3 plants. Increase your plant production 2 steps. | Effect: When you convert plants to greenery, you pay 1 plant less. |

Pets declaration:

```pets
CLASS Ecoline : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: PlantTag<This>
  This: 36 MC, 3 Plant, PROD[2 Plant]
  ActionBilling<ConvertPlantsAction>:: -Owed<Class<Plant>>
}
```

### Interplanetary Cinematics

Class: `InterplanetaryCinematics`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 20 steel and 30 M€. | Effect: Each time you play an event, you gain 2 M€. |
| Generated text | Gain 30 M€ and 20 steel. | Effect: When you play an event card, gain 2 M€. |

Pets declaration:

```pets
CLASS InterplanetaryCinematics : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: 30 MC, 20 Steel
  EventCard: 2 MC
}
```

### Inventrix

Class: `Inventrix`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | As your first action in the game, draw 3 cards. Start with 45 M€. | Effect: Your temperature, oxygen, and ocean requirements are +2 or -2 steps, your choice in each case. |
| Generated text | Gain 45 M€. As your first action, draw 3 cards. | Effect: When you play a card, you may treat a global parameter requirement as if it is 2 steps lower or higher. |

Pets declaration:

```pets
CLASS Inventrix : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: ScienceTag<This>
  This: 45 MC, Inventrix_RequiredAction
  CheckRequirement:: -2 Required<Class<GlobalParameter>>
}
```

### Mining Guild

Class: `MiningGuild`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 30 M€, 5 steel, and 1 steel production. | Effect: Each time you place a tile on an area with steel or titanium placement bonus, increase your steel production 1 step. |
| Generated text | Gain 30 M€ and 5 steel. Increase your steel production 1 step. | Effect: When you place a tile on an area with a titanium or steel placement bonus, increase your steel production 1 step. |

Pets declaration:

```pets
CLASS MiningGuild : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: 2 BuildingTag<This>
  This: 30 MC, 5 Steel, PROD[Steel]
  Tile<MarsArea(HAS PlacementBonus<Class<Metal>>)>: PROD[Steel]
}
```

### PhoboLog

Class: `PhoboLog`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 10 titanium and 23 M€. | Effect: Your titanium resources are each worth 1 M€ extra. |
| Generated text | Gain 23 M€ and 10 titanium. | Effect: Each titanium you pay is worth 1 M€ extra. |

Pets declaration:

```pets
CLASS PhoboLog : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: GrantedResourceValue<Class<Titanium>, This>
  This:: SpaceTag<This>
  This: 23 MC, 10 Titanium
}
```

### Tharsis Republic

Class: `TharsisRepublic`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 40 M€. As your first action in the game, place a city tile. | Effect: When any city tile is placed ON MARS, increase your M€ production 1 step. When you place a city tile, gain 3 M€. |
| Generated text | If this is a solo game, increase your M€ production 2 steps. Gain 40 M€. As your first action, place a city tile. | Effect: When any city tile is placed on Mars, increase your M€ production 1 step. When you place a city tile, gain 3 M€. |

Pets declaration:

```pets
CLASS TharsisRepublic : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: 40 MC, TharsisRepublic_RequiredAction
  CityTile<Anyone, MarsArea>: PROD[MC]
  CityTile: 3 MC
  This IF SoloMode: PROD[2 MC]
}
```

### ThorGate

Class: `ThorGate`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 1 energy production and 48 M€. | Effect: When playing a power card OR THE STANDARD PROJECT POWER PLANT, you pay 3 M€ less for it. |
| Generated text | Gain 48 M€. Increase your energy production 1 step. | Effect: When you play a power tag or use the Power Plant standard project, you pay 3 M€ less for it. |

Pets declaration:

```pets
CLASS ThorGate : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: PowerTag<This>
  This: 48 MC, PROD[Energy]
  PayingFor<Class<PowerTag>> OR ActionBilling<PowerPlantProject>:: -3 Owed
}
```

### United Nations Mars Initiative

Class: `UnitedNationsMarsInitiative`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 40 M€. | Action: If your Terraform Rating was raised this generation, you may pay 3 M€ to raise it 1 step more. |
| Generated text | Gain 40 M€. | Action: If your terraform rating has been raised this generation, spend 3 M€ to raise your terraform rating 1 step. |

Pets declaration:

```pets
CLASS UnitedNationsMarsInitiative : ActionCard<Class<StandardCorporationCard>> {
  HAS Class<TrWatcher>
  cost = 0
  This:: EarthTag<This>
  This: 40 MC
  3 MC -> HasRaisedTr: TerraformRating
}
```

## Corporate Era Expansion

### Saturn Systems

Class: `SaturnSystems`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 1 titanium production and 42 M€. | Effect: Each time any Jovian tag is put into play, including this, increase your M€ production 1 step. |
| Generated text | Gain 42 M€. Increase your titanium production 1 step. | Effect: When any Jovian tag is played (including this), increase your M€ production 1 step. |

Pets declaration:

```pets
CLASS SaturnSystems : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: JovianTag<This>
  This: 42 MC, PROD[Titanium]
  JovianTag<Anyone>: PROD[MC]
}
```

### Teractor

Class: `Teractor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 60 M€. | Effect: When playing an Earth card, you pay 3 M€ less for it. |
| Generated text | Gain 60 M€. | Effect: When you play an Earth tag, you pay 3 M€ less for it. |

Pets declaration:

```pets
CLASS Teractor : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: EarthTag<This>
  This: 60 MC
  PayingFor<Class<EarthTag>>:: -3 Owed
}
```

## Venus Next Expansion

### Aphrodite

Class: `Aphrodite`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 47 M€ and 1 plant production. | Effect: Whenever Venus is terraformed 1 step, you gain 2 M€. |
| Generated text | Gain 47 M€. Increase your plant production 1 step. | Effect: When Venus is raised 1 step, gain 2 M€. |

Pets declaration:

```pets
CLASS Aphrodite : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: VenusTag<This>, PlantTag<This>
  This: 47 MC, PROD[Plant]
  VenusStep BY Anyone: 2 MC
}
```

### Celestic

Class: `Celestic`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 42 M€. As your first action, reveal cards from the deck until you have revealed 2 cards with a floater icon on it. Take those 2 cards into hand, and discard the rest. 1 VP per 3 floaters on this card. | Action: Add a floater to ANY card. |
| Generated text | Gain 42 M€. As your first action, draw 2 cards with floater icons. 1 VP per 3 floaters on this card. | Action: Add 1 floater to any card. |

Pets declaration:

```pets
CLASS Celestic : ActionCard, ResourceCard<Class<Floater>, Class<StandardCorporationCard>> {
  cost = 0
  This:: VenusTag<This>
  This: 42 MC, Celestic_RequiredAction
  End: VictoryPoint / 3 Floater<This>
  -> Floater
}
```

### Manutech

Class: `Manutech`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 1 steel production and 35 M€. | Effect: For each step you increase the production of a resource, including this, you also gain that resource. |
| Generated text | Gain 35 M€. Increase your steel production 1 step. | Effect: When you increase one of your productions 1 step, gain that resource. |

Pets declaration:

```pets
CLASS Manutech : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: 35 MC, PROD[Steel]
  PROD[@StandardResource]: @StandardResource
}
```

### Morning Star Inc.

Class: `MorningStarInc`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 50 M€. As your first action, reveal cards from the deck until you have revealed 3 Venus-tag cards. Take those into hand and discard the rest. | Effect: Your Venus requirements are +/- 2 steps, your choice in each case. |
| Generated text | Gain 50 M€. As your first action, draw 3 Venus cards. | Effect: When you play a card, you may treat a Venus requirement as if it is 2 steps lower or higher. |

Pets declaration:

```pets
CLASS MorningStarInc : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: VenusTag<This>
  This: 50 MC, MorningStarInc_RequiredAction
  CheckRequirement:: -2 Required<Class<VenusStep>>
}
```

### Viron

Class: `Viron`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 48 M€. | Action: Use a blue card action that has already been used this generation. |
| Generated text | Gain 48 M€. | Action: Use an action from an action card that has an action-used marker and is not Viron. |

Pets declaration:

```pets
CLASS Viron : ActionCard<Class<StandardCorporationCard>> {
  cost = 0
  This:: MicrobeTag<This>
  This: 48 MC
  -> UseAction<ActionCard(HAS ActionUsedMarker, NOT Viron)>
}
```

## Prelude 1 Card Pack

### Cheung Shing Mars

Class: `CheungShingMars`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 44 M€ and 3 M€ production. | Effect: When you play a building tag, you pay 2 M€ less for it. |
| Generated text | Gain 44 M€. Increase your M€ production 3 steps. | Effect: When you play a building tag, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS CheungShingMars : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: 44 MC, PROD[3 MC]
  PayingFor<Class<BuildingTag>>:: -2 Owed
}
```

### Point Luna

Class: `PointLuna`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 38 M€ and 1 titanium production. | Effect: When you play an Earth tag, including this, draw a card. |
| Generated text | Gain 38 M€. Increase your titanium production 1 step. | Effect: When you play an Earth tag (including this), draw 1 card. |

Pets declaration:

```pets
CLASS PointLuna : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: EarthTag<This>, SpaceTag<This>
  This: 38 MC, PROD[Titanium]
  EarthTag: ProjectCard
}
```

### Robinson Industries

Class: `RobinsonIndustries`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 47 M€. | Action: Spend 4 M€ to increase (one of) your LOWEST PRODUCTION 1 step. |
| Generated text | Gain 47 M€. | Action: Spend 4 M€ to increase one of your lowest productions 1 step. |

Pets declaration:

```pets
CLASS RobinsonIndustries : ActionCard<Class<StandardCorporationCard>> {
  cost = 0
  This: 47 MC
  4 MC -> PROD[StandardResource(HAS =1 (RANK @Class<StandardResource> { Production<Class<StandardResource>(NOT @Class)> OR ProdOffset<@Class> }))]
}
```

### Valley Trust

Class: `ValleyTrust`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 37 M€. As your first action, draw 3 Prelude cards, and play one of them. Discard the other two. | Effect: When you play a science tag, you pay 2 M€ less for it. |
| Generated text | Gain 37 M€. As your first action, draw 3 prelude cards, then discard 2 prelude cards, then play a prelude card. | Effect: When you play a science tag, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS ValleyTrust : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: EarthTag<This>
  This: 37 MC, ValleyTrust_RequiredAction
  PayingFor<Class<ScienceTag>>:: -2 Owed
}
```

### Vitor

Class: `Vitor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 45 M€. As your first action, fund an award for free. | Effect: When you play a card with a NON-NEGATIVE VP icon, including this, gain 3 M€. |
| Generated text | As your first action, fund an award for free. Gain 45 M€. | Effect: When you play a card with a VP icon, gain 3 M€. |

Pets declaration:

```pets
CLASS Vitor : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: EarthTag<This>
  This: 45 MC
  This IF Class<Award>: Vitor_RequiredAction
  CardFront(HAS NonNegativeIconsOf<Class<VictoryPoint>>): 3 MC
}
```

## Colonies Expansion

### Aridor

Class: `Aridor`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 40 M€. As your first action, put an additional Colony Tile of your choice into play. | Effect: When you get a new type of tag in play (event cards do not count), increase your M€ production 1 step. |
| Generated text | \[EACH @Class&lt;Tag&gt; { AridorTagWatcher&lt;@Class, This&gt; }\]. Gain 40 M€. As your first action, add 1 colony tile. | — |

Pets declaration:

```pets
CLASS Aridor : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: EACH @Class<Tag> { AridorTagWatcher<@Class, This> }
  This: 40 MC, Aridor_RequiredAction
}
```

### Arklight

Class: `Arklight`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 45 M€. Increase your M€ production 2 steps. 1 VP per 2 animals on this card. | Effect: When you play an animal or plant tag, including this, add 1 animal to this card. |
| Generated text | Gain 45 M€. Increase your M€ production 2 steps. 1 VP per 2 animals on this card. | Effect: When you play an animal tag or a plant tag (including this), add 1 animal to this card. |

Pets declaration:

```pets
CLASS Arklight : ResourceCard<Class<Animal>, Class<StandardCorporationCard>> {
  cost = 0
  This:: AnimalTag<This>
  This: 45 MC, PROD[2 MC]
  AnimalTag OR PlantTag: Animal<This>
  End: VictoryPoint / 2 Animal<This>
}
```

### Polyphemos

Class: `Polyphemos`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 50 M€. Increase your M€ production 5 steps. Gain 5 titanium. | Effect: When you buy a card to hand, pay 5 M€ instead of 3 M€, including the starting hand. |
| Generated text | Gain 50 M€ and 5 titanium. Increase your M€ production 5 steps. | Effect: When you buy a card, pay 2 M€ extra. |

Pets declaration:

```pets
CLASS Polyphemos : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This: 50 MC, 5 Titanium, PROD[5 MC]
  PayingFor<Class<ProjectCard>>:: 2 Owed<>
}
```

### Poseidon

Class: `Poseidon`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 45 M€. As your first action, place a colony. | Effect: When any colony is placed, including this, raise your M€ production 1 step. |
| Generated text | Gain 45 M€. As your first action, place a colony. | Effect: When any colony is placed, increase your M€ production 1 step. |

Pets declaration:

```pets
CLASS Poseidon : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This: 45 MC, Poseidon_RequiredAction
  Colony<Anyone>: PROD[MC]
}
```

### Stormcraft Incorporated

Class: `StormcraftIncorporated`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 48 M€. | Action: Add 1 floater to ANY card. Effect: Floaters on this card may be used as 2 heat each. |
| Generated text | Gain 48 M€. | Action: Add 1 floater to any card. / Effect: You may use floaters on this card as 2 heat each. When you play a local heat trapping, you may remove 1 floater from this card to gain 2 heat, remove 2 floaters from this card to gain 4 heat, or remove 3 floaters from this card to gain 5 heat. |

Pets declaration:

```pets
CLASS StormcraftIncorporated : ActionCard, ResourceCard<Class<Floater>, Class<StandardCorporationCard>> {
  cost = 0
  This:: JovianTag<This>
  This: 48 MC
  Billing<HasActions, ActionSlot, Class<Heat>>:: AcceptingFromCard<This>
  PayFromCard<This>:: -2 Owed<Class<Heat>>
  LocalHeatTrapping: (-Floater<This> THEN 2 Heat) OR (-2 Floater<This> THEN 4 Heat) OR (-3 Floater<This> THEN 5 Heat) OR Ok
  -> Floater
}
```

## Turmoil Expansion

### Lakefront Resorts

Class: `LakefrontResorts`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 54 M€. | Effect: When any ocean is placed, increase your M€ production 1 step. Effect: Your bonus for placing adjacent to oceans is 3 M€ instead of 2 M€. |
| Generated text | Gain 54 M€. | Effect: When any ocean tile is placed, increase your M€ production 1 step. When you place a tile on Mars, gain 1 M€ per ocean tile next to that area. |

Pets declaration:

```pets
CLASS LakefrontResorts : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: 54 MC
  OceanTile BY Anyone: PROD[MC]
  Placement<@MarsArea>: MC / Neighbor<OceanTile, @MarsArea>
}
```

### Pristar

Class: `Pristar`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 53 M€. Decrease your TR 2 steps. 1 VP per preservation resource here. | Effect: During production phase, if you did not get TR so far this generation, add 1 preservation resource here and gain 6 M€. |
| Generated text | Gain 53 M€. Lower your terraform rating 2 steps. 1 VP per preservation resource on this card. | Effect: \[ProductionPhase IF MAX 0 HasRaisedTr: Preservation&lt;This&gt;, 6 MC\]. |

Pets declaration:

```pets
CLASS Pristar : ResourceCard<Class<Preservation>, Class<StandardCorporationCard>> {
  HAS Class<TrWatcher>
  cost = 0
  This: 53 MC, -2 TerraformRating
  ProductionPhase IF MAX 0 HasRaisedTr: Preservation<This>, 6 MC
  End: VictoryPoint / Preservation<This>
}
```

### TerraLabs Research

Class: `TerraLabsResearch`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 14 M€. Lower your TR 1 step. | Effect: Buying cards to hand costs 1 M€, including your starting hand. |
| Generated text | Gain 14 M€. Lower your terraform rating 1 step. | Effect: When you buy a card, you pay 2 M€ less for it. |

Pets declaration:

```pets
CLASS TerraLabsResearch : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: ScienceTag<This>, EarthTag<This>
  This: 14 MC, -TerraformRating
  PayingFor<Class<ProjectCard>>:: -2 Owed
}
```

### Utopia Invest

Class: `UtopiaInvest`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 40 M€. Increase your steel and titanium production 1 step each. | Action: Decrease any production to gain 4 resources of that kind. |
| Generated text | Gain 40 M€. Increase your steel production and your titanium production 1 step each. | Action: Decrease one of your productions 1 step to gain 4 of that resource. |

Pets declaration:

```pets
CLASS UtopiaInvest : ActionCard<Class<StandardCorporationCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: 40 MC, PROD[Steel, Titanium]
  PROD[@StandardResource] -> 4 @StandardResource
}
```

## Promo Card Pack

### Arcadian Communities

Class: `ArcadianCommunities`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 40 M€ and 10 steel. AS YOUR FIRST ACTION, PLACE A COMMUNITY (PLAYER MARKER) ON A NON-RESERVED AREA. | Action: PLACE A COMMUNITY (PLAYER MARKER) ON A NON-RESERVED AREA ADJACENT TO ONE OF YOUR TILES OR MARKED AREAS. Effect: MARKED AREAS ARE RESERVED FOR YOU. WHEN YOU PLACE A TILE THERE, GAIN 3 M€ |
| Generated text | Gain 40 M€ and 10 steel. As your first action, place a community marker on a land area with no occupant. | Action: Place a community marker on a land area with no occupant next to a tile or community you own. / Effect: When you remove a community marker, gain 3 M€. |

Pets declaration:

```pets
CLASS ArcadianCommunities : ActionCard<Class<StandardCorporationCard>> {
  cost = 0
  This: 40 MC, 10 Steel, ArcadianCommunities_RequiredAction
  -Community: 3 MC
  -> Community<LandArea(HAS MAX 0 Occupant, HAS Neighbor<OwnedOccupant>)>
}
```

### AstroDrill

Class: `AstroDrill`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 35 M€, and 3 asteroid resources here. | Action: Either add 1 asteroid to ANY CARD or gain any standard resource, OR spend 1 asteroid here to gain 3 titanium. |
| Generated text | Gain 35 M€. Add 3 asteroids to this card. | Action: Add 1 asteroid to any card or gain 1 standard resource, or spend 1 asteroid from this card to gain 3 titanium. |

Pets declaration:

```pets
CLASS AstroDrill : ActionCard, ResourceCard<Class<Asteroid>, Class<StandardCorporationCard>> {
  cost = 0
  This:: SpaceTag<This>
  This: 35 MC, 3 Asteroid<This>
  -> Asteroid OR StandardResource
  Asteroid<This> -> 3 Titanium
}
```

### Factorum

Class: `Factorum`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 37 M€. Increase your steel production 1 step. | Action: Increase your energy production 1 step IF YOU HAVE NO ENERGY RESOURCES, or spend 3 M€ to draw a building card. |
| Generated text | Gain 37 M€. Increase your steel production 1 step. | Action: If you have no energy, increase your energy production 1 step, or spend 3 M€ to draw 1 building card. |

Pets declaration:

```pets
CLASS Factorum : ActionCard<Class<StandardCorporationCard>> {
  cost = 0
  This:: PowerTag<This>, BuildingTag<This>
  This: 37 MC, PROD[Steel]
  -> MAX 0 Energy: PROD[Energy]
  3 MC -> SearchForCard<TagFilter<Class<BuildingTag>>>
}
```

### Kuiper Cooperative

Class: `KuiperCooperative`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 33 M€. Increase your titanium production 1 step. | Action: Add 1 asteroid here for every space tag you have. Effect: When paying for the ASTEROID or AQUIFER standard projects, each asteroid here may be used as 1 M€. |
| Generated text | Gain 33 M€. Increase your titanium production 1 step. | Action: Add 1 asteroid to this card per space tag you have. / Effect: When you use a standard project or a standard project, asteroids on this card may be used as 1 M€ each. |

Pets declaration:

```pets
CLASS KuiperCooperative : ActionCard, ResourceCard<Class<Asteroid>, Class<StandardCorporationCard>> {
  cost = 0
  This:: 2 SpaceTag<This>
  This: 33 MC, PROD[Titanium]
  UseAction<AsteroidProject> OR UseAction<AquiferProject>:: AcceptingFromCard<This>
  PayFromCard<This>:: -Owed
  -> Asteroid<This> / SpaceTag
}
```

### Mons Insurance

Class: `MonsInsurance`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 48 M€. Increase your M€ production 4 steps. ALL OPPONENTS DECREASE THEIR M€ PRODUCTION 2 STEPS. THIS DOES NOT TRIGGER THE EFFECT BELOW. | Effect: When a player causes another player to decrease production or lose resources, pay 3 M€ to the victim, or as much as possible. |
| Generated text | Gain 48 M€. Increase your M€ production 4 steps. Each other player decreases their own M€ production 2 steps. | Effect: When any player has their resources removed by another player, or has their production decreased by another player, pay 3 M€ to that player, or as much as possible. |

Pets declaration:

```pets
CLASS MonsInsurance : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This: 48 MC, PROD[4 MC], EACH Player(NOT Owner) { PROD[-2 MC] BY Owner }
  MyResourceWasRemoved<Victim@Anyone> OR MyProductionWasDecreased<Victim@Anyone>: 3 MC<Victim@Anyone FROM Owner>.
}
```

### Pharmacy Union

Class: `PharmacyUnion`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 54 M€. Draw a science card. | Effect: When ANY microbe tag is played, including these 2, add a disease here and lose 4 M€ or as much as possible. When you play a science tag, remove 1 disease from here and raise your TR 1 step, OR, if there are no diseases here, you may raise your TR 3 steps and place this card in your event pile. It now counts as a played event. |
| Generated text | Gain 54 M€. Draw 1 science card. | Effect: \[MicrobeTag&lt;Anyone&gt;: Disease&lt;This&gt;! OR (MAX 0 This: Ok), -4 MC.\]. \[ScienceTag: (TerraformRating FROM Disease&lt;This&gt;!) OR (MAX 0 Disease: (PlayedEvent&lt;Class&lt;This&gt;&gt; FROM This THEN 3 TerraformRating) OR Ok)\]. |

Pets declaration:

```pets
CLASS PharmacyUnion : ResourceCard<Class<Disease>, Class<StandardCorporationCard>> {
  cost = 0
  This:: 54 MC
  This:: 2 MicrobeTag<This>
  This: SearchForCard<TagFilter<Class<ScienceTag>>>
  MicrobeTag<Anyone>: Disease<This>! OR (MAX 0 This: Ok), -4 MC.
  ScienceTag: (TerraformRating FROM Disease<This>!) OR (MAX 0 Disease: (PlayedEvent<Class<This>> FROM This THEN 3 TerraformRating) OR Ok)
}
```

### Philares

Class: `Philares`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 47 M€. As your first action, place a greenery tile and raise the oxygen 1 step. | Effect: Each new adjacency between your tile and an opponent's tile gives you a standard resource of your choice (regardless of who just placed a tile). |
| Generated text | Gain 47 M€. As your first action, place a greenery tile. | Effect: When an adjacency between one of your tiles and an opponent's tile is created, gain 1 standard resource. |

Pets declaration:

```pets
CLASS Philares : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: BuildingTag<This>
  This: 47 MC, Philares_RequiredAction
  Adjacency<OwnedTile<Owner>, OwnedTile<Anyone(NOT Owner)>> BY Anyone: StandardResource
}
```

### Recyclon

Class: `Recyclon`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 38 M€ and 1 steel production. | Effect: When you play a building tag, including this, gain 1 microbe to this card, or remove 2 microbes here and raise your plant production 1 step. |
| Generated text | Gain 38 M€. Increase your steel production 1 step. | Effect: When you play a building tag (including this), add 1 microbe to this card or remove 2 microbes from this card to increase your plant production 1 step. |

Pets declaration:

```pets
CLASS Recyclon : ResourceCard<Class<Microbe>, Class<StandardCorporationCard>> {
  cost = 0
  This:: MicrobeTag<This>, BuildingTag<This>
  This: 38 MC, PROD[Steel]
  BuildingTag: Microbe<This> OR (-2 Microbe<This> THEN PROD[Plant])
}
```

### Splice Tactical Genomics

Class: `SpliceTacticalGenomics`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 44 M€. As your first action, reveal cards until you have revealed a microbe tag. Take that card into hand, and discard the rest. | Effect: Each time a microbe tag is played, including this, THAT PLAYER gains 2 M€, or adds a microbe to THAT card, and you gain 2 M€. |
| Generated text | \[Splicer&lt;This&gt;\]. Gain 44 M€. As your first action, draw 1 microbe card. | Effect: When any microbe tag is played (including this), gain 2 M€. |

Pets declaration:

```pets
CLASS SpliceTacticalGenomics : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: Splicer<This>
  This:: MicrobeTag<This>
  This: 44 MC, SpliceTacticalGenomics_RequiredAction
  MicrobeTag<Anyone>: 2 MC
}
```

### Tycho Magnetics

Class: `TychoMagnetics`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 42 M€. Increase your energy production 1 step. | Action: Spend any number of energy to draw that many cards. Keep 1 and discard the rest. |
| Generated text | Gain 42 M€. Increase your energy production 1 step. | Action: Spend 1 or more energy to look at that many cards. Draw 1 of them. |

Pets declaration:

```pets
CLASS TychoMagnetics : ActionCard<Class<StandardCorporationCard>> {
  cost = 0
  This:: PowerTag<This>, ScienceTag<This>
  This: 42 MC, PROD[Energy]
  X Energy -> ProjectCard
}
```

## Prelude 2 Card Pack

### Ecotec

Class: `Ecotec`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 42 M€. Increase your plant production 1 step. | Effect: When playing a bio tag, including these 2, gain 1 plant or add 1 microbe to ANY card. |
| Generated text | Gain 42 M€. Increase your plant production 1 step. | Effect: When you play a bio tag (including this), gain 1 plant or add 1 microbe to another card. |

Pets declaration:

```pets
CLASS Ecotec : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: MicrobeTag<This>, PlantTag<This>
  This: 42 MC, PROD[Plant]
  BioTag: Plant OR Microbe
}
```

### Nirgal Enterprises

Class: `NirgalEnterprises`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 30 M€. Increase your energy, plant, and steel production 1 step each. | Effect: AWARDS AND MILESTONES ALWAYS COST 0 M€ FOR YOU |
| Generated text | Gain 30 M€. Increase your energy production, your plant production, and your steel production 1 step each. | Effect: When you claim a milestone or fund an award, the cost is 0 M€. |

Pets declaration:

```pets
CLASS NirgalEnterprises : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: PowerTag<This>, PlantTag<This>, BuildingTag<This>
  This: 30 MC, PROD[Energy, Plant, Steel]
  ActionBilling<ClaimMilestoneAction>:: -Owed / Owed
  ActionBilling<FundAwardAction>:: -Owed / Owed
}
```

### Palladin Shipping

Class: `PalladinShipping`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 36 M€. Gain 5 titanium. | Effect: When you play a space event, gain 1 titanium. Action: Spend 2 titanium to raise temperature 1 step. |
| Generated text | Gain 36 M€ and 5 titanium. | Action: Spend 2 titanium to raise temperature 1 step. / Effect: When you play a space event card, gain 1 titanium. |

Pets declaration:

```pets
CLASS PalladinShipping : ActionCard<Class<StandardCorporationCard>> {
  cost = 0
  This:: SpaceTag<This>
  This: 36 MC, 5 Titanium
  EventCard(HAS SpaceTag): Titanium
  2 Titanium -> TemperatureStep
}
```

### Sagitta Frontier Services

Class: `SagittaFrontierServices`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 31 M€. Increase your energy production 1 step and your M€ production 2 steps. Draw a card that has no tag. | Effect: When you play a card with no tags, including this, gain 4 M€. When you play a card with EXACTLY 1 TAG, you gain 1 M€. |
| Generated text | Gain 31 M€. Increase your energy production 1 step and your M€ production 2 steps. Draw 1 card with no tags. | Effect: When you play a card with no tags (including this), gain 4 M€. When you play a card with exactly 1 tag, gain 1 M€. |

Pets declaration:

```pets
CLASS SagittaFrontierServices : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This: 31 MC, PROD[Energy, 2 MC], ProjectCard
  CardFront(HAS =0 Tag): 4 MC
  CardFront(HAS =1 Tag): MC
}
```

### Spire

Class: `Spire`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 50 M€. As your first action, draw 4 cards, then discard 3 cards from hand. | Effect: When you play a card with AT LEAST 2 tags, including this, add 1 science resource here. When you pay for a standard project, science resources here may be used as 2 M€ each. |
| Generated text | Gain 50 M€. As your first action, draw 4 cards, then discard 3 cards. | Effect: When you play a card with 2 or more tags (including this), add 1 science resource to this card. When you use a standard project with a positive printed cost, science resources on this card may be used as 2 M€ each. |

Pets declaration:

```pets
CLASS Spire : ResourceCard<Class<Science>, Class<StandardCorporationCard>> {
  cost = 0
  This:: CityTag<This>, EarthTag<This>
  This: 50 MC, Spire_RequiredAction
  CardFront(HAS 2 Tag): Science<This>
  UseAction<StandardProject(HAS cost)>:: AcceptingFromCard<This>
  PayFromCard<This>:: -2 Owed
}
```

## Replay-only models

### Helion

Class: `FakeHelion`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 3 heat production and 42 M€. | Effect: You may use heat as M€. You may not use M€ as heat. |
| Generated text | Gain 42 M€. Increase your heat production 3 steps. | Effect: \[Billing&lt;HasActions, ActionSlot, Class&lt;MC&gt;&gt; IF Owed&lt;Class&lt;MC&gt;&gt;:: Accepting&lt;Class&lt;Heat&gt;&gt;\]. |

Pets declaration:

```pets
"Helion without correct Heat payment allocation"
CLASS FakeHelion : CardFront<Class<StandardCorporationCard>> {
  cost = 0
  This:: SpaceTag<This>, BaseResourceValue<Class<Heat>>
  This: 42 MC, PROD[3 Heat]
  Billing<HasActions, ActionSlot, Class<MC>> IF Owed<Class<MC>>:: Accepting<Class<Heat>>
}
```

### Septem Tribus

Class: `FakeSeptemTribus`

| | Bottom | Top |
| --- | --- | --- |
| Printed text | You start with 36 M€. When you perform an action, the wild tag counts as any tag of your choice. | Action: Gain 2 M€ for EACH PARTY WHERE YOU HAVE AT LEAST 1 DELEGATE. |
| Generated text | Gain 36 M€. \[FakeWildTag&lt;This&gt;\]. | Action: \[2 MC / Class&lt;Party&gt;(HAS PartyDelegate&lt;Party, Owner&gt;)\]. |

Pets declaration:

```pets
"Septem Tribus with an inert wild tag"
CLASS FakeSeptemTribus : ActionCard<Class<StandardCorporationCard>> {
  cost = 0
  This: 36 MC, FakeWildTag<This>
  -> 2 MC / Class<Party>(HAS PartyDelegate<Party, Owner>)
}
```
