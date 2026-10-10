# Global events: printed and generated wording

[All categories](README.md) · 36 entries

Printed text: [wording evidence](../../src/jvm/dev/martianzoo/tfm/text/english-global-event-published-wording-evidence.tsv).
Generated text comes directly from the current English renderer. See the [reading notes](README.md#reading-the-comparisons).

## Turmoil Expansion

### Aquifer Released By Public Council

Class: `AquiferReleasedByPublicCouncil`

| | Text |
| --- | --- |
| Printed text | First player places an ocean tile. Gain 1 plant and 1 steel per influence. |
| Generated text | If not all oceans have been placed, the first player must place an ocean tile without gaining terraform rating or other bonuses. Gain 1 plant and 1 steel per influence. |

Pets declaration:

```pets
CLASS AquiferReleasedByPublicCouncil : GePartyDistant<MarsFirst>, GePartyCurrent<Greens> {
  ResolveGlobalEvent<Class<This>> IF GpIncomplete<Class<OceanTile>>:: EACH Starter@Player(HAS StartToken) { AdminOceanPlacement<Starter@Player> }
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { Plant / Influence, Steel / Influence }
}
```

### Asteroid Mining

Class: `AsteroidMiningGlobalEvent`

| | Text |
| --- | --- |
| Printed text | Gain 1 titanium for each Jovian tag (max 5) and influence. |
| Generated text | Gain 1 titanium per Jovian tag you have (max 5). Gain 1 titanium per influence. |

Pets declaration:

```pets
CLASS AsteroidMiningGlobalEvent : GePartyDistant<Reds>, GePartyCurrent<Unity> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { Titanium / JovianTag MAX 5, Titanium / Influence }
}
```

### Celebrity Leaders

Class: `CelebrityLeaders`

| | Text |
| --- | --- |
| Printed text | Gain 2 M€ for each event played (max 5) and influence. |
| Generated text | Gain 2 M€ per card in your event pile (max 5). Gain 2 M€ per influence. |

Pets declaration:

```pets
CLASS CelebrityLeaders : GePartyDistant<Unity>, GePartyCurrent<Greens> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { 2 MC / PlayedEvent MAX 5, 2 MC / Influence }
}
```

### Diversity

Class: `Diversity`

| | Text |
| --- | --- |
| Printed text | Gain 10 M€ if you have 9 or more different tags. Influence counts as unique tags. |
| Generated text | If you have at least 9 different tags and influence combined, gain 10 M€. |

Pets declaration:

```pets
CLASS Diversity : GePartyDistant<Scientists>, GePartyCurrent<Scientists> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { 10 MC / 9 (Class<@Tag>(HAS @Tag) OR Influence) MAX 1 }
}
```

### Dry Deserts

Class: `DryDeserts`

| | Text |
| --- | --- |
| Printed text | First player removes 1 ocean tile from the gameboard. Gain 1 standard resource per influence. |
| Generated text | If not all oceans have been placed, the first player must remove an ocean tile. \[ResolveDryDeserts\]. |

Pets declaration:

```pets
CLASS DryDeserts : GePartyDistant<Reds>, GePartyCurrent<Unity> {
  ResolveGlobalEvent<Class<This>> IF GpIncomplete<Class<OceanTile>>:: EACH Starter@Player(HAS StartToken) { RemoveOceanForGlobalEvent<Starter@Player> }
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { ResolveDryDeserts }
}
```

### Eco Sabotage

Class: `EcoSabotage`

| | Text |
| --- | --- |
| Printed text | Lose all plants except 3 + influence. |
| Generated text | Remove all plants except 3 plus influence. |

Pets declaration:

```pets
CLASS EcoSabotage : GePartyDistant<Greens>, GePartyCurrent<Reds> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -Plant! / Plant - Influence - 3 }
}
```

### Election

Class: `Election`

| | Text |
| --- | --- |
| Printed text | Count your influence plus building tags and city tiles (no limits). The player with most (or 10 in solo) gains 2 TR, the 2nd (or counting 5 in solo) gains 1 TR (ties are friendly). |
| Generated text | If this is a multiplayer game, \[EACH Me@Player(HAS =1 (RANK Ranked@Player { Influence&lt;Ranked@Player&gt; OR BuildingTag&lt;Ranked@Player&gt; OR CityTile&lt;Ranked@Player&gt; })) { 2 TerraformRating }\]. If this is a multiplayer game, \[EACH Me@Player(HAS =2 (RANK Ranked@Player { Influence&lt;Ranked@Player&gt; OR BuildingTag&lt;Ranked@Player&gt; OR CityTile&lt;Ranked@Player&gt; })) { TerraformRating }\]. If this is a solo game, raise your terraform rating 1 step per complete set of 5 influence, building tags, and city tiles combined (max 2). |

Pets declaration:

```pets
CLASS Election : GePartyDistant<Greens>, GePartyCurrent<MarsFirst> {
  ResolveGlobalEvent<Class<This>> IF MultiplayerMode:: EACH Me@Player(HAS =1 (RANK Ranked@Player { Influence<Ranked@Player> OR BuildingTag<Ranked@Player> OR CityTile<Ranked@Player> })) { 2 TerraformRating }
  ResolveGlobalEvent<Class<This>> IF MultiplayerMode:: EACH Me@Player(HAS =2 (RANK Ranked@Player { Influence<Ranked@Player> OR BuildingTag<Ranked@Player> OR CityTile<Ranked@Player> })) { TerraformRating }
  ResolveGlobalEvent<Class<This>> IF SoloMode:: EACH Me@Player { TerraformRating / 5 (Influence OR BuildingTag OR CityTile) MAX 2 }
}
```

### Generous Funding

Class: `GenerousFunding`

| | Text |
| --- | --- |
| Printed text | Gain 2 M€ for each influence and each complete set of 5 TR over 15 (max 5 sets). |
| Generated text | Gain 2 M€ per complete set of 5 steps of your terraform rating in excess of 15 (max 5). Gain 2 M€ per influence. |

Pets declaration:

```pets
CLASS GenerousFunding : GePartyDistant<Kelvinists>, GePartyCurrent<Unity> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { 2 MC / 5 (TerraformRating - 15) MAX 5, 2 MC / Influence }
}
```

### Global Dust Storm

Class: `GlobalDustStorm`

| | Text |
| --- | --- |
| Printed text | Lose all heat. Lose 2 M€ for each building tag (max 5, then reduced by influence). |
| Generated text | Remove all heat. Remove 2 M€ per building tag you have (max 5) in excess of influence, or as much as possible. |

Pets declaration:

```pets
CLASS GlobalDustStorm : GePartyDistant<Kelvinists>, GePartyCurrent<Greens> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -Heat! / Heat, -2 MC. / BuildingTag MAX 5 - Influence }
}
```

### Homeworld Support

Class: `HomeworldSupport`

| | Text |
| --- | --- |
| Printed text | Gain 2 M€ for each Earth tag (max 5) and influence. |
| Generated text | Gain 2 M€ per Earth tag you have (max 5). Gain 2 M€ per influence. |

Pets declaration:

```pets
CLASS HomeworldSupport : GePartyDistant<Reds>, GePartyCurrent<Unity> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { 2 MC / EarthTag MAX 5, 2 MC / Influence }
}
```

### Improved Energy Templates

Class: `ImprovedEnergyTemplates`

| | Text |
| --- | --- |
| Printed text | Increase energy production 1 step per 2 power tags (no limit). Influence counts as power tags. |
| Generated text | Increase your energy production 1 step per complete set of 2 power tags and influence combined. |

Pets declaration:

```pets
CLASS ImprovedEnergyTemplates : GePartyDistant<Scientists>, GePartyCurrent<Kelvinists> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { PROD[Energy / 2 (PowerTag OR Influence)] }
}
```

### Interplanetary Trade

Class: `InterplanetaryTradeGlobalEvent`

| | Text |
| --- | --- |
| Printed text | Gain 2 M€ for each space tag (max 5) and influence. |
| Generated text | Gain 2 M€ per space tag you have (max 5). Gain 2 M€ per influence. |

Pets declaration:

```pets
CLASS InterplanetaryTradeGlobalEvent : GePartyDistant<Unity>, GePartyCurrent<Unity> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { 2 MC / SpaceTag MAX 5, 2 MC / Influence }
}
```

### Miners On Strike

Class: `MinersOnStrike`

| | Text |
| --- | --- |
| Printed text | Lose 1 titanium for each Jovian tag (max 5, then reduced by influence). |
| Generated text | Remove 1 titanium per Jovian tag you have (max 5) in excess of influence, or as much as possible. |

Pets declaration:

```pets
CLASS MinersOnStrike : GePartyDistant<MarsFirst>, GePartyCurrent<Greens> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -Titanium. / JovianTag MAX 5 - Influence }
}
```

### Mud Slides

Class: `MudSlides`

| | Text |
| --- | --- |
| Printed text | Lose 4 M€ for each tile adjacent to ocean (max 5, then reduced by influence). |
| Generated text | Remove 4 M€ per tile you own on areas next to at least 1 ocean tile (max 5) in excess of influence, or as much as possible. |

Pets declaration:

```pets
CLASS MudSlides : GePartyDistant<Kelvinists>, GePartyCurrent<Greens> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -4 MC. / OwnedTile<MarsArea(HAS Neighbor<OceanTile>)> MAX 5 - Influence }
}
```

### Pandemic

Class: `Pandemic`

| | Text |
| --- | --- |
| Printed text | Lose 3 M€ for each building tag (max 5, then reduced by influence). |
| Generated text | Remove 3 M€ per building tag you have (max 5) in excess of influence, or as much as possible. |

Pets declaration:

```pets
CLASS Pandemic : GePartyDistant<Greens>, GePartyCurrent<MarsFirst> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -3 MC. / BuildingTag MAX 5 - Influence }
}
```

### Paradigm Breakdown

Class: `ParadigmBreakdown`

| | Text |
| --- | --- |
| Printed text | Discard 2 cards from hand. Gain 2 M€ per influence. |
| Generated text | Discard 2 cards, or as much as possible. Gain 2 M€ per influence. |

Pets declaration:

```pets
CLASS ParadigmBreakdown : GePartyDistant<Kelvinists>, GePartyCurrent<Reds> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -2 ProjectCard., 2 MC / Influence }
}
```

### Productivity

Class: `Productivity`

| | Text |
| --- | --- |
| Printed text | Gain 1 steel for each steel production (max 5) and influence. |
| Generated text | Gain 1 steel per steel production (max 5). Gain 1 steel per influence. |

Pets declaration:

```pets
CLASS Productivity : GePartyDistant<Scientists>, GePartyCurrent<MarsFirst> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { Steel / PROD[Steel] MAX 5, Steel / Influence }
}
```

### Red Influence

Class: `RedInfluence`

| | Text |
| --- | --- |
| Printed text | Lose 3 M€ for each set of 5 TR over 10 (max 5 sets). Increase M€ production 1 step per influence. |
| Generated text | Remove 3 M€ per complete set of 5 steps of your terraform rating in excess of 10 (max 5), or as much as possible. Increase your M€ production 1 step per influence. |

Pets declaration:

```pets
CLASS RedInfluence : GePartyDistant<Kelvinists>, GePartyCurrent<Reds> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -3 MC. / 5 (TerraformRating - 10) MAX 5, PROD[MC / Influence] }
}
```

### Revolution

Class: `Revolution`

| | Text |
| --- | --- |
| Printed text | Count Earth tags and ADD(!) influence. The player(s) with most (at least 1) loses 2 TR, and 2nd most (at least 1) loses 1 TR. SOLO: Lose 2 TR if the sum is 4 or more. |
| Generated text | If this is a multiplayer game, \[EACH Me@Player(HAS 1 (EarthTag OR Influence), HAS =1 (RANK Ranked@Player { EarthTag&lt;Ranked@Player&gt; OR Influence&lt;Ranked@Player&gt; })) { -2 TerraformRating. }\]. If this is a multiplayer game, \[EACH Me@Player(HAS 1 (EarthTag OR Influence), HAS =2 (RANK Ranked@Player { EarthTag&lt;Ranked@Player&gt; OR Influence&lt;Ranked@Player&gt; })) { -TerraformRating. }\]. If this is a solo game, \[EACH Me@Player(HAS 4 (EarthTag OR Influence)) { -2 TerraformRating. }\]. |

Pets declaration:

```pets
CLASS Revolution : GePartyDistant<Unity>, GePartyCurrent<MarsFirst> {
  ResolveGlobalEvent<Class<This>> IF MultiplayerMode:: EACH Me@Player(HAS 1 (EarthTag OR Influence), HAS =1 (RANK Ranked@Player { EarthTag<Ranked@Player> OR Influence<Ranked@Player> })) { -2 TerraformRating. }
  ResolveGlobalEvent<Class<This>> IF MultiplayerMode:: EACH Me@Player(HAS 1 (EarthTag OR Influence), HAS =2 (RANK Ranked@Player { EarthTag<Ranked@Player> OR Influence<Ranked@Player> })) { -TerraformRating. }
  ResolveGlobalEvent<Class<This>> IF SoloMode:: EACH Me@Player(HAS 4 (EarthTag OR Influence)) { -2 TerraformRating. }
}
```

### Riots

Class: `Riots`

| | Text |
| --- | --- |
| Printed text | Lose 4 M€ for each city tile (max 5, then reduced by influence). |
| Generated text | Remove 4 M€ per city tile you own (max 5) in excess of influence, or as much as possible. |

Pets declaration:

```pets
CLASS Riots : GePartyDistant<MarsFirst>, GePartyCurrent<Reds> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -4 MC. / CityTile MAX 5 - Influence }
}
```

### Sabotage

Class: `SabotageGlobalEvent`

| | Text |
| --- | --- |
| Printed text | Decrease steel and energy production 1 step each. Gain 1 steel per influence. |
| Generated text | Decrease your steel production 1 step, or as much as possible. Decrease your energy production 1 step, or as much as possible. Gain 1 steel per influence. |

Pets declaration:

```pets
CLASS SabotageGlobalEvent : GePartyDistant<Unity>, GePartyCurrent<Reds> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { PROD[-Steel., -Energy.], Steel / Influence }
}
```

### Scientific Community

Class: `ScientificCommunity`

| | Text |
| --- | --- |
| Printed text | Gain 1 M€ for each card in hand (no limit) and influence. |
| Generated text | Gain 1 M€ per card in hand. Gain 1 M€ per influence. |

Pets declaration:

```pets
CLASS ScientificCommunity : GePartyDistant<Reds>, GePartyCurrent<Scientists> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { MC / ProjectCard, MC / Influence }
}
```

### Snow Cover

Class: `SnowCover`

| | Text |
| --- | --- |
| Printed text | Decrease temperature 2 steps. Draw 1 card per influence. |
| Generated text | If temperature has not reached its maximum, lower temperature 2 steps. Draw 1 card per influence. |

Pets declaration:

```pets
CLASS SnowCover : GePartyDistant<Kelvinists>, GePartyCurrent<Kelvinists> {
  ResolveGlobalEvent<Class<This>> IF GpIncomplete<Class<TemperatureStep>>:: -2 TemperatureStep
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { ProjectCard / Influence }
}
```

### Solar Flare

Class: `SolarFlare`

| | Text |
| --- | --- |
| Printed text | Lose 3 M€ for each space tag (max 5, then reduced by influence). |
| Generated text | Remove 3 M€ per space tag you have (max 5) in excess of influence, or as much as possible. |

Pets declaration:

```pets
CLASS SolarFlare : GePartyDistant<Unity>, GePartyCurrent<Kelvinists> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -3 MC. / SpaceTag MAX 5 - Influence }
}
```

### Solarnet Shutdown

Class: `SolarnetShutdown`

| | Text |
| --- | --- |
| Printed text | Lose 3 M€ for each blue card (max 5, then reduced by influence). |
| Generated text | Remove 3 M€ per active card in play (max 5) in excess of influence, or as much as possible. |

Pets declaration:

```pets
CLASS SolarnetShutdown : GePartyDistant<Scientists>, GePartyCurrent<MarsFirst> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -3 MC. / ActiveCard MAX 5 - Influence }
}
```

### Spin-Off Products

Class: `SpinOffProducts`

| | Text |
| --- | --- |
| Printed text | Gain 2 M€ for each science tag (max 5) and influence. |
| Generated text | Gain 2 M€ per science tag you have (max 5). Gain 2 M€ per influence. |

Pets declaration:

```pets
CLASS SpinOffProducts : GePartyDistant<Greens>, GePartyCurrent<Scientists> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { 2 MC / ScienceTag MAX 5, 2 MC / Influence }
}
```

### Sponsored Projects

Class: `SponsoredProjects`

| | Text |
| --- | --- |
| Printed text | All cards with resources on them gain 1 resource. Draw 1 card for each influence. |
| Generated text | \[EACH Card@ResourceCard&lt;Anyone&gt;(HAS CardResource) { CardResource&lt;Card@ResourceCard&gt; }\]. Draw 1 card per influence. |

Pets declaration:

```pets
CLASS SponsoredProjects : GePartyDistant<Scientists>, GePartyCurrent<Greens> {
  ResolveGlobalEvent<Class<This>>:: EACH Card@ResourceCard<Anyone>(HAS CardResource) { CardResource<Card@ResourceCard> }
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { ProjectCard / Influence }
}
```

### Strong Society

Class: `StrongSociety`

| | Text |
| --- | --- |
| Printed text | Gain 2 M€ for each city tile (max 5) and influence. |
| Generated text | Gain 2 M€ per city tile you own (max 5). Gain 2 M€ per influence. |

Pets declaration:

```pets
CLASS StrongSociety : GePartyDistant<Reds>, GePartyCurrent<MarsFirst> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { 2 MC / CityTile MAX 5, 2 MC / Influence }
}
```

### Successful Organisms

Class: `SuccessfulOrganisms`

| | Text |
| --- | --- |
| Printed text | Gain 1 plant per plant production (max 5) and influence. |
| Generated text | Gain 1 plant per plant production (max 5). Gain 1 plant per influence. |

Pets declaration:

```pets
CLASS SuccessfulOrganisms : GePartyDistant<MarsFirst>, GePartyCurrent<Scientists> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { Plant / PROD[Plant] MAX 5, Plant / Influence }
}
```

### Volcanic Eruptions

Class: `VolcanicEruptions`

| | Text |
| --- | --- |
| Printed text | Increase temperature 2 steps. Increase heat production 1 step per influence. |
| Generated text | Raise temperature 2 steps. Increase your heat production 1 step per influence. |

Pets declaration:

```pets
CLASS VolcanicEruptions : GePartyDistant<Scientists>, GePartyCurrent<Kelvinists> {
  ResolveGlobalEvent<Class<This>>:: 2 TemperatureStep
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { PROD[Heat / Influence] }
}
```

### War On Earth

Class: `WarOnEarth`

| | Text |
| --- | --- |
| Printed text | Reduce TR 4 steps. Each influence prevents 1 step. |
| Generated text | Lower your terraform rating 1 step per unit by which influence falls short of 4, or as much as possible. |

Pets declaration:

```pets
CLASS WarOnEarth : GePartyDistant<MarsFirst>, GePartyCurrent<Kelvinists> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -TerraformRating. / 4 - Influence }
}
```

## Promo Card Pack

### Jovian Tax Rights

Class: `JovianTaxRights`

| | Text |
| --- | --- |
| Printed text | Increase M€ production 1 step for each colony. Gain 1 titanium for each influence. |
| Generated text | Increase your M€ production 1 step per colony you own (max 5). Gain 1 titanium per influence. |

Pets declaration:

```pets
CLASS JovianTaxRights : GePartyDistant<Scientists>, GePartyCurrent<Unity> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { PROD[MC / Colony MAX 5], Titanium / Influence }
}
```

### Microgravity Health Problems

Class: `MicrogravityHealthProblems`

| | Text |
| --- | --- |
| Printed text | Lose 3 M€ for each colony (max 5), then reduced by influence. |
| Generated text | Remove 3 M€ per colony you own (max 5) in excess of influence, or as much as possible. |

Pets declaration:

```pets
CLASS MicrogravityHealthProblems : GePartyDistant<MarsFirst>, GePartyCurrent<Scientists> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { -3 MC. / Colony MAX 5 - Influence }
}
```

### Venus Infrastructure

Class: `VenusInfrastructure`

| | Text |
| --- | --- |
| Printed text | Gain 2 M€ per Venus tag (max 5) and influence. |
| Generated text | Gain 2 M€ per Venus tag you have (max 5). Gain 2 M€ per influence. |

Pets declaration:

```pets
CLASS VenusInfrastructure : GePartyDistant<MarsFirst>, GePartyCurrent<Unity> {
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { 2 MC / VenusTag MAX 5, 2 MC / Influence }
}
```

### Cloud Societies

Class: `CloudSocieties`

| | Text |
| --- | --- |
| Printed text | Add a floater to each card that can collect floaters. Add 1 floater for each influence to a card. |
| Generated text | \[EACH @ResourceCard&lt;Class&lt;Floater&gt;, Anyone&gt; { Floater&lt;@ResourceCard&gt; }\]. \[ChooseInfluenceFloaterCard\]. |

Pets declaration:

```pets
CLASS CloudSocieties : GePartyDistant<Unity>, GePartyCurrent<Reds> {
  HAS Class<VenusTag>, Class<Colony>
  ResolveGlobalEvent<Class<This>>:: EACH @ResourceCard<Class<Floater>, Anyone> { Floater<@ResourceCard> }
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { ChooseInfluenceFloaterCard }
}
```

### Corrosive Rain

Class: `CorrosiveRain`

| | Text |
| --- | --- |
| Printed text | Lose 2 floaters from a card, or lose 10 M€. Draw 1 card for each influence. |
| Generated text | \[ResolveCorrosiveRain\]. |

Pets declaration:

```pets
CLASS CorrosiveRain : GePartyDistant<Kelvinists>, GePartyCurrent<Greens> {
  HAS Class<VenusTag>, Class<Colony>
  ResolveGlobalEvent<Class<This>>:: EACH Me@Player { ResolveCorrosiveRain }
}
```
