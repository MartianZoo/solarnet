// solarnet world export 1

// Live game begun Tue 2026-08-18. Quoted evidence is verbatim from the supplied transcripts.
auto none

// "We are playing on the Utopia Planitia board."
// "We have Preludes. We have Venus, Colonies, Promos, Milestones and Awards expansion."
// The transcript includes Briber, which Solarnet does not implement; it is not claimed in this
// partial game, so the executable pool omits it. The transcript says Enceladus twice; the
// photographed five-tile colony setup has one Enceladus.
newgame "UtopiaMap, VenusNextExpansion, PreludeExpansion, ColoniesExpansion, PromoCardPack, Ecologist, Merchant, Metallurgist, Tactician4, Hoverlord, Constructor, Excentric, Highlander, Mogul, Traveller, Venuphile, Enceladus, Miranda, Europa, Io, Pluto" Dad Ellie purple

BECOME Dad
DO tasks(2 select)
DO tasks(10 ProjectCard<Selecting>, -3 ProjectCard<Selecting>)

// board-11-00-18.jpg: initial global state, before either corporation is played.
// "I'm Point Luna... I get a titanium production." "I'm keeping seven cards."
// "So I pay 21. I have 17 money remaining."
DO playCard(PointLuna)
DO tasks(ProjectCard, 38 MC, PROD[Titanium])
DO buyCards()
BECOME Ellie
DO tasks(2 select)
DO tasks(10 ProjectCard<Selecting>, -5 ProjectCard<Selecting>)

// "I have Valley Trust. I'm keeping five cards... I have 22 money."
DO playCard(ValleyTrust)
DO tasks(37 MC, ValleyTrust_Mandate)
DO buyCards()
BECOME Dad

// "I play Biofuels... two plants, a plant production, and an energy production."
DO playCard(Biofuels)
DO tasks(PROD[Plant, Energy], 2 Plant)

// "And then I play Donation and get 21 money."
DO playCard(Donation)
DO tasks(21 MC)
BECOME Ellie

// "Supplier... four steel, and two energy production."
DO playCard(Supplier)
DO tasks(PROD[2 Energy], 4 Steel)

// "Martian Industries... six money, one steel production, and one energy production."
DO playCard(MartianIndustries)
DO tasks(PROD[Energy, Steel], 6 MC)
BECOME Dad

// "Dad pays 10 to play Pets... Miranda comes into play, and I get an animal on Pets."
DO playCard(Pets, -10 MC)
DO tasks(ProjectCard, Animal<Dad, Pets>)
DO endTurn()
BECOME Ellie
DO useAction(1, HandleMandates)

// "I use Valley Trust and I get Double Down, which I play... copy Martian Industries."
DO playCard(DoubleDown)
DO tasks(CopyPrelude<MartianIndustries>, PROD[Energy, Steel], 6 MC)

// "I spend two money to play Psychrophiles."
DO playCard(Psychrophiles, -2 MC)
BECOME Dad

// "I pay eleven for Aerial Mappers."
DO playCard(AerialMappers, -11 MC)
DO endTurn()
BECOME Ellie

// "I pay eight for Forced Precipitation."
DO playCard(ForcedPrecipitation, -8 MC)
DO endTurn()
BECOME Dad

// "I'm going to add a floater to Aerial Mappers."
DO useAction(1, AerialMappers)
DO tasks(Floater<Dad, AerialMappers>)
DO endTurn()
BECOME Ellie

// "I spend 21 to play Extractor Balloons. It gets three floaters."
DO playCard(ExtractorBalloons, -21 MC)
DO tasks(3 Floater<Ellie, ExtractorBalloons>)
DO endTurn()
BECOME Dad
DO tasks(Pass)
BECOME Ellie

// "I'm going to add a microbe to Psychrophiles."
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)

// "Remove two floaters from Extractor Balloons and raise Venus."
DO useAction(2, ExtractorBalloons, -2 Floater<Ellie, ExtractorBalloons>)
DO tasks(VenusStep, TerraformRating)

// "Then pay two money to add a floater to Forced Precipitation."
DO useAction(1, ForcedPrecipitation, -2 MC)
DO tasks(Floater<Ellie, ForcedPrecipitation>)
DO tasks(Pass)
BECOME Dad

// "Dad uses World Government Terraforming to increase oxygen."
DO tasks(OxygenStep! BY Engine)
BECOME Ellie

// board-11-09-02.jpg and both player ledgers: after Generation 1 transition, before Research.
// "I will buy four cards."
DO buyCards()
BECOME Dad
DO tasks(-ProjectCard<Selecting>)

// Dad first said two, then physically corrected the purchase to three before the action phase.
DO buyCards()
BECOME Ellie

// "I'm going to play Mining Rights... row three, column six. I get two cards and a
// titanium... and increase titanium production."
DO playCard(MiningRights, -4 Steel, -1 MC)
DO tasks(MiningRights_SpecialTile<Utopia_3_6>, 2 ProjectCard, Titanium, PROD[Titanium])

// "I play Energy Tapping... Dad loses an energy production."
DO playCard(EnergyTapping, -3 MC)
DO tasks(PROD[-Energy<Dad>, Energy])
BECOME Dad

// "I play CEO's Favorite Project... put a floater on Aerial Mappers."
DO playCard(CeosFavoriteProject, -1 MC)
DO tasks(Floater<Dad, AerialMappers>)
DO endTurn()
BECOME Ellie

// "I'm going to add a floater to Extractor Balloons."
DO useAction(1, ExtractorBalloons)
DO tasks(Floater<Ellie, ExtractorBalloons>)
DO endTurn()
BECOME Dad

// "I remove a floater from Aerial Mappers and draw a card."
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
DO endTurn()
BECOME Ellie

// "I'm adding a microbe to Psychrophiles."
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
DO endTurn()
BECOME Dad
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)
DO endTurn()
BECOME Ellie

// "I pay two and add a floater to Forced Precipitation."
DO useAction(1, ForcedPrecipitation, -2 MC)
DO tasks(Floater<Ellie, ForcedPrecipitation>)
DO endTurn()
BECOME Dad

// "I pay all 28 money and one titanium to play 16 Psyche."
DO playCard(SixteenPsyche, -Titanium, -28 MC)
DO tasks(PROD[2 Titanium], 3 Titanium)
DO endTurn()
BECOME Ellie
DO tasks(Pass)
BECOME Dad
DO tasks(Pass)
BECOME Ellie

// "I'm going to use World Government Terraforming to increase Venus."
DO tasks(VenusStep! BY Engine)
BECOME Dad
DO tasks(-3 ProjectCard<Selecting>)

// board-11-17-20.jpg and both player ledgers: after Generation 2 transition, before Research.
// "I'm going to buy one."
DO buyCards()
BECOME Ellie
DO tasks(-4 ProjectCard<Selecting>)

// Ellie corrected an initial purchase entry: "I'm buying zero cards."
DO buyCards()
BECOME Dad

// "I play Imported Hydrogen... five titanium and one money. Put two animals on Pets."
// "The ocean goes row four, column one... I get a plant and a card."
DO playCard(ImportedHydrogen, -5 Titanium, -1 MC)
DO tasks(2 Animal<Dad, Pets>, OceanTile<Utopia_4_1>, ProjectCard, TerraformRating, Plant, ProjectCard)
DO endTurn()
BECOME Ellie

// "I remove two floaters from Forced Precipitation and increase Venus."
DO useAction(2, ForcedPrecipitation, -2 Floater<Ellie, ForcedPrecipitation>)
DO tasks(VenusStep, TerraformRating)

// "I remove two floaters from Extractor Balloons and increase Venus."
DO useAction(2, ExtractorBalloons, -2 Floater<Ellie, ExtractorBalloons>)
DO tasks(VenusStep, TerraformRating, ProjectCard)
BECOME Dad

// "I pay eight for Cartel... three money production."
DO playCard(Cartel, -8 MC)
DO tasks(ProjectCard, PROD[3 MC])
DO endTurn()
BECOME Ellie

// "I play Colonizer Training Camp, paying four steel."
DO playCard(ColonizerTrainingCamp, -4 Steel)
DO endTurn()
BECOME Dad
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)
DO endTurn()
BECOME Ellie
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)

// "I play Beam from a Thorium Asteroid... two titanium and 26 money."
DO playCard(BeamFromAThoriumAsteroid, -2 Titanium, -26 MC)
DO tasks(PROD[3 Heat, 3 Energy])
BECOME Dad

// "I pay four for Research Coordination."
DO playCard(ResearchCoordination, -4 MC)
DO tasks(WildTag<Dad, ResearchCoordination>)
DO endTurn()
BECOME Ellie

// "I'm going to add to Psychrophiles."
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
DO endTurn()
BECOME Dad
DO assignWildTag(VenusTag)

// "I pay four for Venus Governor... two money production."
DO playCard(VenusGovernor, -4 MC)
DO tasks(PROD[2 MC])
DO endTurn()
BECOME Ellie
DO tasks(Pass)
BECOME Dad
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
DO tasks(Pass)

// "Dad increases temperature with World Government Terraforming."
DO tasks(TemperatureStep! BY Engine)
BECOME Ellie
DO tasks(-2 ProjectCard<Selecting>)

// Both player ledgers: after Generation 3 transition, before Research.
DO buyCards()
BECOME Dad
DO tasks(-2 ProjectCard<Selecting>)
DO buyCards()
BECOME Ellie

// "I'm trading with Pluto... paying three energy, and I get three cards."
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Pluto>, FlownTradeFleet<Ellie, Pluto> FROM ReserveTradeFleet<Ellie>, 3 ProjectCard, ResetColonyProduction<Pluto>, -4 ColonyProduction<Pluto>)
DO endTurn()
BECOME Dad

// "Then I play Mars University for eight... discard one and draw one."
DO playCard(MarsUniversity, -8 MC)
DO tasks(-ProjectCard<Hand>, ProjectCard)
DO endTurn()
BECOME Ellie

// "I pay seven for Flooding... row three, column one."
DO playCard(Flooding, -7 MC)
DO tasks(OceanTile<Utopia_3_1>, 2 MC, TerraformRating, 3 Plant)

// "I use one Psychrophiles microbe to play Potatoes... lose two plants and get two money
// production."
DO playCard(Potatoes, -Microbe<Psychrophiles>)
DO tasks(-2 Plant, PROD[2 MC])
BECOME Dad
DO assignWildTag(ScienceTag)

// "I pay three for Mercurian Alloys."
DO playCard(MercurianAlloys, -3 MC)
DO endTurn()
BECOME Ellie

// "I'm just going to take my turn, and that is add to Psychrophiles."
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
DO endTurn()
BECOME Dad

// "I pay two money and two titanium for Asteroid Rights... it gets two asteroids."
DO playCard(AsteroidRights, -2 Titanium, -2 MC)
DO tasks(ProjectCard, 2 Asteroid<Dad, AsteroidRights>)
DO endTurn()
BECOME Ellie

// "I play Mine, paying two steel."
DO playCard(Mine, -2 Steel)
DO tasks(PROD[Steel])
DO endTurn()
BECOME Dad

// "I remove an asteroid and get two titanium."
DO useAction(2, AsteroidRights, -Asteroid<Dad, AsteroidRights>)
DO tasks(2 Titanium)
DO endTurn()
BECOME Ellie
DO useAction(1, ForcedPrecipitation, -2 MC)
DO tasks(Floater<Ellie, ForcedPrecipitation>)
DO useAction(1, ExtractorBalloons)
DO tasks(Floater<Ellie, ExtractorBalloons>)
BECOME Dad
DO assignWildTag(ScienceTag)

// "I pay five for Floating Habs."
DO playCard(FloatingHabs, -5 MC)
DO endTurn()
BECOME Ellie
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)

// "I spend 11 on Nitrate [sic] Reducing Bacteria."
DO playCard(NitriteReducingBacteria, -11 MC)
DO tasks(3 Microbe<Ellie, NitriteReducingBacteria>)
BECOME Dad

// "I pay two with Floating Habs and put a floater on Aerial Mappers."
DO useAction(1, FloatingHabs, -2 MC)
DO tasks(Floater<Dad, AerialMappers>)
DO endTurn()
BECOME Ellie

// "I take three microbes off Nitrate Reducing Bacteria and gain a TR."
DO useAction(2, NitriteReducingBacteria, -3 Microbe<Ellie, NitriteReducingBacteria>)
DO tasks(TerraformRating)
DO endTurn()
BECOME Dad
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
DO endTurn()
BECOME Ellie
DO tasks(Pass)
BECOME Dad
DO tasks(Pass)
BECOME Ellie

// "I'm going to increase Venus" with World Government Terraforming.
DO tasks(VenusStep! BY Engine)
BECOME Dad
DO tasks(-3 ProjectCard<Selecting>)

// board-13-20-01.jpg and both player ledgers: after Generation 4 transition, before Research.
DO buyCards()
BECOME Ellie
DO tasks(-3 ProjectCard<Selecting>)
DO buyCards()
BECOME Dad

// Point Luna tableau in board-13-46-12.jpg: "Energy Market. Cost me three."
DO playCard(EnergyMarket, -3 MC)
DO endTurn()
BECOME Ellie

// Valley Trust tableau in board-13-46-12.jpg: "Hydrogen to Venus. I spend two titanium and
// five real... add two to Forced Precipitation."
DO playCard(HydrogenToVenus, -2 Titanium, -5 MC)
DO tasks(2 Floater<Ellie, ForcedPrecipitation>, VenusStep, TerraformRating)

// User clarification: Ellie played Hermetic Order of Mars. Her ledger combines its six-M€
// gain with the following twelve-M€ Stratospheric Birds payment.
DO playCard(HermeticOrderOfMars, -10 MC)
DO tasks(PROD[2 MC], 6 MC)
BECOME Dad

// "Use my Energy Market to pay six, which gives me three energy, and then use that three
// energy to send my little boat to Io and take ten heat."
DO useAction(1, EnergyMarket, -6 MC)
DO tasks(3 Energy)
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Io>, FlownTradeFleet<Dad, Io> FROM ReserveTradeFleet<Dad>, 10 Heat, ResetColonyProduction<Io>, -5 ColonyProduction<Io>)
BECOME Ellie

// Valley Trust tableau in board-13-46-12.jpg: Stratospheric Birds. The played card consumes
// one Forced Precipitation floater.
DO playCard(StratosphericBirds, -12 MC)
DO tasks(-Floater<Ellie, ForcedPrecipitation>)

// "I spend three energy to trade with Miranda. Three animals on Stratospheric Birds."
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Miranda>, FlownTradeFleet<Ellie, Miranda> FROM ReserveTradeFleet<Ellie>, 3 Animal<Ellie, StratosphericBirds>, ResetColonyProduction<Miranda>, -5 ColonyProduction<Miranda>)
BECOME Dad

// "Big Asteroid... all titanium... overspending one... four titanium back, two temperature
// boops... remove one plant."
DO playCard(BigAsteroid, -7 Titanium)
DO tasks(-Plant<Ellie>, TemperatureStep, TerraformRating, TemperatureStep, TerraformRating, PROD[Heat], 4 Titanium)
DO endTurn()
BECOME Ellie

// Ellie's ledger records two eight-heat conversions after Big Asteroid.
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating, PROD[Heat])
BECOME Dad
DO assignWildTag(EarthTag)

// "Lunar Mining. It costs me 11... six Earth tags... six titanium production."
DO playCard(LunarMining, -11 MC)
DO tasks(ProjectCard, PROD[3 Titanium])
DO endTurn()
BECOME Ellie
DO useAction(1, StratosphericBirds)
DO tasks(Animal<Ellie, StratosphericBirds>)
DO endTurn()
BECOME Dad
DO useAction(2, AsteroidRights, -Asteroid<Dad, AsteroidRights>)
DO tasks(2 Titanium)
DO endTurn()
BECOME Ellie
DO useAction(1, ForcedPrecipitation, -2 MC)
DO tasks(Floater<Ellie, ForcedPrecipitation>)
DO useAction(1, ExtractorBalloons)
DO tasks(Floater<Ellie, ExtractorBalloons>)
BECOME Dad
DO assignWildTag(EarthTag)

// "Luna Metropolis... five titanium and one real money... seven money production."
DO playCard(LunaMetropolis, -5 Titanium, -1 MC)
DO tasks(ProjectCard, PROD[7 MC], CityTile<LunaMetropolis_RemoteArea>, Animal<Dad, Pets>)
DO endTurn()
BECOME Ellie
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
DO useAction(1, NitriteReducingBacteria)
DO tasks(Microbe<Ellie, NitriteReducingBacteria>)
BECOME Dad
DO useAction(1, FloatingHabs, -2 MC)
DO tasks(Floater<Dad, AerialMappers>)
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
BECOME Ellie
DO tasks(Pass)
BECOME Dad
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)
DO tasks(Pass)

// "World Government us an ocean... nine-eight."
DO tasks(OceanTile<Utopia_9_8>! BY Engine)
DO tasks(-2 ProjectCard<Selecting>)

// Both player ledgers: after Generation 5 transition, before Research.
DO buyCards()
BECOME Ellie
DO tasks(-3 ProjectCard<Selecting>)
DO buyCards()

// "I will spend three energy to trade with Enceladus. That is five microbes going to
// Nitrate Reducing Bacteria."
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Enceladus>, FlownTradeFleet<Ellie, Enceladus> FROM ReserveTradeFleet<Ellie>, 5 Microbe<Ellie, NitriteReducingBacteria>, ResetColonyProduction<Enceladus>, -6 ColonyProduction<Enceladus>)
DO endTurn()
BECOME Dad

// "I'm going to play Industrial Microbes for full price. And now I'm going to pay eight to
// become the Ecologist."
DO playCard(IndustrialMicrobes, -12 MC)
DO tasks(PROD[Energy, Steel])
DO assignWildTag(MicrobeTag)
DO useAction(1, ClaimMilestoneSA, -8 MC)
DO tasks(Ecologist)

// Dad never narrated or logged Industrial Microbes' steel and energy production; both remain
// absent from the generation-seven photograph and ledger.
mode red
exec PROD[-Steel, -Energy]
mode purple
BECOME Ellie

// "Forced Precipitation and Extractor Balloons. Remove two off both of them to raise Venus
// by two. Oh, it is at 16, which means I get an extra TR."
DO useAction(2, ForcedPrecipitation, -2 Floater<Ellie, ForcedPrecipitation>)
DO tasks(VenusStep, TerraformRating)
DO useAction(2, ExtractorBalloons, -2 Floater<Ellie, ExtractorBalloons>)
DO tasks(VenusStep, TerraformRating, TerraformRating)
BECOME Dad

// "Import some GHG for two titanium, one real money, draw a card, get two heat production."
DO playCard(ImportOfAdvancedGhg, -2 Titanium, -1 MC)
DO tasks(ProjectCard, PROD[2 Heat])

// "For my second, let's just get this other milestone taken care of. Eight to be the
// Metallurgist."
DO useAction(1, ClaimMilestoneSA, -8 MC)
DO tasks(Metallurgist)
BECOME Ellie
DO useAction(1, ClaimMilestoneSA, -8 MC)
DO tasks(Tactician4)
DO endTurn()
BECOME Dad

// "Use Floating Habs to spend two money to put a floater on Aerial Mappers, and use that to
// draw a card."
DO useAction(1, FloatingHabs, -2 MC)
DO tasks(Floater<Dad, AerialMappers>)
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
BECOME Ellie
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
DO endTurn()
BECOME Dad
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)

// "Hired Raiders, pay one... I'm going to take three money."
DO playCard(HiredRaiders, -1 MC)
DO tasks(3 MC<Dad> FROM MC<Ellie>)
BECOME Ellie

// "Nitrate Reducing Bacteria. I will reduce the nitrates. Spend three of them to gain a TR."
// The transcript places this immediately after Hired Raiders; move it to the preceding legal
// Ellie turn rather than assigning any of Dad's photographed cards to her.
DO useAction(2, NitriteReducingBacteria, -3 Microbe<Ellie, NitriteReducingBacteria>)
DO tasks(TerraformRating)
DO endTurn()
BECOME Dad

// "Use Asteroid Rights to spend one of my three money to put an asteroid on Asteroid Rights."
DO useAction(1, AsteroidRights, -1 MC)
DO tasks(Asteroid<Dad, AsteroidRights>)
DO endTurn()
BECOME Ellie

// "Before I forget, I will heat boop."
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)
DO endTurn()
BECOME Dad

// "Use Energy Market to spend my last two money to get one energy resource."
DO useAction(1, EnergyMarket, -2 MC)
DO tasks(Energy)
DO useAction(1, ConvertPlantsSA, -8 Plant)

// "I'm going to convert plants and get in this spot where I get a plant and four money."
DO tasks(GreeneryTile<Utopia_4_2>, 2 MC, 2 MC, OxygenStep, Plant, TerraformRating)

// Dad accidentally took another TR, not realizing the app gave it to him already
mode red
exec TerraformRating
mode purple
BECOME Ellie

// "Noctis City... six steel and six real... place a city tile... on three-two."
DO playCard(NoctisCity, -6 Steel, -6 MC)
DO tasks(CityTile<Utopia_3_2>, PROD[-Energy, 3 MC], 2 MC)
BECOME Dad
DO tasks(Animal<Dad, Pets>)
BECOME Ellie
DO endTurn()
BECOME Dad
DO tasks(Pass)
BECOME Ellie

// "I stratobird. Five. I get silver stratobirds."
DO useAction(1, StratosphericBirds)
DO tasks(Animal<Ellie, StratosphericBirds>)
DO tasks(Pass)

// "I will World Government an ocean on six-four."
DO tasks(OceanTile<Utopia_6_4>! BY Engine)
BECOME Dad

// board-13-46-12.jpg and both player ledgers: after Generation 6 transition.
// When we resumed the physical game, we knew about the errors found above, so we made this
// manual correction to put things right again.
mode red
exec -TR, -1 MC, PROD[Steel, Energy], Steel, Energy
mode purple
DO tasks(-2 ProjectCard<Selecting>)
DO buyCards()

// And then I immediately screwed up and forgot to pay for my cards!
mode red
exec 6 MC
mode purple
BECOME Ellie
DO tasks(-ProjectCard<Selecting>)
DO buyCards()
BECOME Dad

// Dad: "Sure. Let's pay eight to fund Traveller. I have funded the Traveller award."
DO useAction(1, FundAwardSA, -8 MC)
DO tasks(Traveller)
DO endTurn()
BECOME Ellie

// Ellie: "I pay one for Market Manipulation. Increase the colony track one step."
// Dad: "So she's increasing Pluto." Ellie: "Yes. Decrease Io."
DO playCard(MarketManipulation, -1 MC)
DO tasks(ColonyProduction<Pluto> FROM ColonyProduction<Io>)

// Ellie: "Then I will spend three energy to trade with Pluto, which now gives me three
// cards." Dad: "Nice. Three cards free and clear."
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Pluto>, FlownTradeFleet<Ellie, Pluto> FROM ReserveTradeFleet<Ellie>, 3 ProjectCard, ResetColonyProduction<Pluto>, -4 ColonyProduction<Pluto>)
BECOME Dad

// Dad: "I guess I'll play a Martian Zoo. I believe that cost me full price. I want to pay
// 12."
DO playCard(MartianZoo, -12 MC)
DO endTurn()
BECOME Ellie

// Ellie: "Io Sulphur Research for 17. I have three Venus tags from the two floater cards and
// Strata Birds, so three cards."
DO playCard(IoSulphurResearch, -15 MC)
DO tasks(3 ProjectCard)

// She forgot her Valley Trust discount
mode red
exec -2 MC
mode purple
DO endTurn()
BECOME Dad

// Dad: "I'm going to play Nuclear Power. That cost me ten. I lose two money production. I
// gain three energy production."
DO playCard(NuclearPower, -10 MC)
DO tasks(PROD[-2 MC, 3 Energy])
DO endTurn()
BECOME Ellie

// Ellie: "Air-Scrapping Expedition for 13. Raise Venus one step, and I get a TR. Add three
// floaters to a Venus card. That'll be Forced Precipitation."
DO playCard(AirScrappingExpedition, -13 MC)
DO tasks(3 Floater<Ellie, ForcedPrecipitation>, VenusStep, TerraformRating)
DO endTurn()
BECOME Dad
DO assignWildTag(EarthTag)

// Dad: "I am going to play Miranda—Miranda Resort. I guess it costs me three titanium. And I
// get one, two, three, four, five, six, seven money production."
DO playCard(MirandaResort, -3 Titanium)
DO tasks(PROD[7 MC])
DO endTurn()
BECOME Ellie

// Ellie: "I used Forced Precipitation. Remove two floaters to increase Venus."
DO useAction(2, ForcedPrecipitation, -2 Floater<Ellie, ForcedPrecipitation>)
DO tasks(VenusStep, TerraformRating)
DO endTurn()
BECOME Dad
DO useAction(1, FloatingHabs, -2 MC)
DO tasks(Floater<Dad, AerialMappers>)
DO endTurn()
BECOME Ellie
DO useAction(1, ExtractorBalloons)
DO tasks(Floater<Ellie, ExtractorBalloons>)
DO endTurn()
BECOME Dad
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
DO endTurn()
BECOME Ellie
DO useAction(1, StratosphericBirds)
DO tasks(Animal<Ellie, StratosphericBirds>)
DO endTurn()
BECOME Dad

// Dad: "I'm going to play Business Contactos, which cost me seven of my nine money. I look at
// the top four cards and I pick two of them. Then, because I played an Earth tag, I draw a
// card and I get a little aminal on Martian Zoo."
DO playCard(BusinessContacts, -7 MC)
DO tasks(ProjectCard, Animal<Dad, MartianZoo>, 2 ProjectCard)
DO endTurn()
BECOME Ellie
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
DO endTurn()
BECOME Dad

// Dad: "I'm going to import some Nitrogen. I'm going to slightly overspend by spending six
// titanium. I will draw the card for the Earth tag. I'll get a TR. I'll get four plants. I
// don't have a microbe card. And I think I'm going to take two animals on Martian Zoo."
DO playCard(ImportedNitrogen, -6 Titanium)
DO tasks(2 Animal<Dad, MartianZoo>, ProjectCard, Animal<Dad, MartianZoo>, TerraformRating, 4 Plant, 3 Microbe)
DO endTurn()

// I forgot the extra animal from MZ's effect
mode red
exec -Animal<MartianZoo>
mode purple
BECOME Ellie

// Ellie: "Nitrite Reducing Bacteria. I remove three and get a TR."
DO useAction(2, NitriteReducingBacteria, -3 Microbe<Ellie, NitriteReducingBacteria>)
DO tasks(TerraformRating)
DO endTurn()

// It looks like she forgot to take her TR
mode red
exec -TR
mode purple
BECOME Dad

// Dad: "Now I'm going to use Asteroid Rights to remove an asteroid from Asteroid Rights. And
// honestly, I think I'll take the money production."
DO useAction(2, AsteroidRights, -Asteroid<Dad, AsteroidRights>)
DO tasks(PROD[1 MC])
DO endTurn()
BECOME Ellie
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)
DO endTurn()
BECOME Dad

// Dad: "I'll take the Martian Zoo action to take three money."
DO useAction(1, MartianZoo)
DO tasks(3 MC)
DO endTurn()
BECOME Ellie

// Ellie: "Neutralizer Factory. Pay seven. We've definitely met the ten percent Venus
// requirement. Increase Venus one step."
DO playCard(NeutralizerFactory, -7 MC)
DO tasks(VenusStep, TerraformRating)
DO endTurn()
BECOME Dad

// Dad: "Venusian Insects is the card that I played and spent five on."
DO playCard(VenusianInsects, -5 MC)
DO endTurn()
BECOME Ellie
DO tasks(Pass)
BECOME Dad

// Dad: "Now I'm taking its action, putting a microbe on it. On it like a bonnet."
DO useAction(1, VenusianInsects)
DO tasks(Microbe<Dad, VenusianInsects>)

// Dad: "I think I will use Energy Market to reduce my energy production by one and get eight
// money."
DO useAction(2, EnergyMarket)
DO tasks(PROD[-Energy], 8 MC)

// Dad: "Then I have enough money for Nitrophilic Moss. We do have the three-ocean
// requirement. And I will lose two plants and gain two plant production. And that costs the
// eight money."
DO playCard(NitrophilicMoss, -8 MC)
DO tasks(PROD[2 Plant], -2 Plant)
DO tasks(Pass)

// Dad uses World Government Terraforming to increase Venus.
DO tasks(VenusStep! BY Engine)
DO tasks(-2 ProjectCard<Selecting>)

// Both player ledgers: after Generation 7 transition, before Research.
DO buyCards()
BECOME Ellie
DO tasks(-3 ProjectCard<Selecting>)
DO buyCards()
BECOME Dad

// I suddenly realized my mistake last turn when I forgot to pay for my cards!
// So I pay for them now, and realize I wouldn't have afforded VI last round.
// I let it stay, but reason that it should lose a microbe that I wouldn't have had the
// chance to play.
mode red
exec -6 MC, -Microbe<VenusianInsects>
mode purple
BECOME Ellie

// Ellie: "Ice Moon Colony. I will pay two of my three titanium for six money, and then 17
// real. I will place on Miranda. And it gives me an animal to my Strata Birds. I place an
// ocean tile." Dad: "It's row eight." Ellie: "Four. Eight-four."
DO playCard(IceMoonColony, -2 Titanium, -17 MC)
DO tasks(Colony<Ellie, Miranda>, Animal<Ellie, StratosphericBirds>, OceanTile<Utopia_8_7>, 2 MC, TerraformRating, 2 Plant)
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Miranda>, FlownTradeFleet<Ellie, Miranda> FROM ReserveTradeFleet<Ellie>, 2 Animal<Ellie, StratosphericBirds>, ProjectCard, ResetColonyProduction<Miranda>, -2 ColonyProduction<Miranda>)
BECOME Dad

// Ellie: "And for my second action, three energy to trade with Miranda. I get two aminals and
// a card."
DO useAction(1, FloatingHabs, -2 MC)
DO tasks(Floater<Dad, AerialMappers>)
DO endTurn()
BECOME Ellie

// Ellie: "I remembered why I left a titanium, and that is so I can play Diversity Support.
// I've got all six standard resources and microbes, animals, floaters. So pay one money, get
// one TR."
DO playCard(DiversitySupport, -1 MC)
DO tasks(TerraformRating)
DO endTurn()
BECOME Dad
DO assignWildTag(ScienceTag)

// Dad: "It'll involve the playing of Plantation, which I can do because I have a science tag
// and a wild tag. I've gotten so much use out of this wild tag. So that costs me 15 entire
// money. And then I place a greenery tile, which I'm going to place at five-three."
DO playCard(Plantation, -15 MC)
DO tasks(GreeneryTile<Utopia_5_3>, 2 MC, OxygenStep, TerraformRating)

// Dad: "Then I'm going to Kaguya its ass. I'm playing Kaguya Tech for ten full money. I get
// two money production. I get a card. I swap this greenery tile. I flip it, basically."
DO playCard(KaguyaTech, -10 MC)
DO tasks(CityTile<Utopia_5_3> FROM GreeneryTile<Utopia_5_3>, PROD[2 MC], ProjectCard, 2 MC, Animal<Dad, Pets>)

// The Generation 9 photograph still has five animals on Pets, so Dad missed the animal caused
// by Kaguya Tech's city placement.
mode red
exec -Animal<Pets>
mode purple
BECOME Ellie

// Ellie: "Right. Venusian Animals." Dad: "Oh my god." Ellie: "Yep. It immediately adds an
// animal to itself."
DO playCard(VenusianAnimals, -13 MC)
DO tasks(Animal<Ellie, VenusianAnimals>)
DO endTurn()

// Again forgot her Valley Trust discount.
mode red
exec -2 MC
mode purple
BECOME Dad

// Dad: "I'm gonna go ahead and use three real and seven titanium. And I'm going to get four
// plant production, two TR plus another TR for raising temp to minus 12."
DO playCard(NitrogenRichAsteroid, -7 Titanium, -3 MC)
DO tasks(PROD[4 Plant], 2 TerraformRating, TemperatureStep, TerraformRating)
DO endTurn()
BECOME Ellie
DO useAction(2, ForcedPrecipitation, -2 Floater<Ellie, ForcedPrecipitation>)
DO tasks(VenusStep, TerraformRating)
DO endTurn()
BECOME Dad
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
DO endTurn()
BECOME Ellie
DO useAction(1, ExtractorBalloons)
DO tasks(Floater<Ellie, ExtractorBalloons>)
DO endTurn()
BECOME Dad

// Dad: "Business Network. Cost me four. I lose a money production. Aw, I only have 19 money
// production now. And I get an animal on Martian Zoo and a card."
DO playCard(BusinessNetwork, -4 MC)
DO tasks(ProjectCard, Animal<Dad, MartianZoo>, PROD[-MC])
DO endTurn()
BECOME Ellie
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
DO endTurn()
BECOME Dad
DO useAction(2, EnergyMarket)
DO tasks(PROD[-Energy], 8 MC)
DO endTurn()
BECOME Ellie
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)
DO endTurn()
BECOME Dad

// Dad: "I lose two steel and I get four money back. All right, so in the end what happened is
// I paid two steel and two real. I gain an energy production and you lose two heat
// production."
DO playCard(HeatTrappers, -2 Steel, -2 MC)
DO tasks(PROD[-2 Heat<Ellie>, Energy])
DO endTurn()
BECOME Ellie

// Ellie: "Luckily I have this power tag so I can play Power Supply Consortium."
// Dad: "I lose an energy production." Ellie: "I pay five and I gain an energy production."
DO playCard(PowerSupplyConsortium, -5 MC)
DO tasks(PROD[-Energy<Dad>, Energy])
DO endTurn()
BECOME Dad
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Europa>, FlownTradeFleet<Dad, Europa> FROM ReserveTradeFleet<Dad>, PROD[Plant], ResetColonyProduction<Europa>, -6 ColonyProduction<Europa>)
DO endTurn()
BECOME Ellie

// Dad: "I guess that means if I want to trade, I better do it now. I'll trade for three
// energy and I'll do Europa. So I get a plant production."
DO useAction(1, StratosphericBirds)
DO tasks(Animal<Ellie, StratosphericBirds>)
DO endTurn()
BECOME Dad

// Dad: "All right, I am going to import some zhuzh. This time it's just Imported Zhuzh. I'm
// going to pay seven real for it. I get one heat production, three heat. I get a silver
// animal on Martian Zoo. I get a card."
DO playCard(ImportedGhg, -7 MC)
DO tasks(ProjectCard, Animal<Dad, MartianZoo>, PROD[Heat], 3 Heat)
DO endTurn()
BECOME Ellie

// Ellie: "Imported Nutrients. I pay a titanium and 11 real, gain four plants, and add four
// microbes to Nitrite-Reducing Bacteria." Dad: "Man, you're just churning that thing."
DO playCard(ImportedNutrients, -Titanium, -11 MC)
DO tasks(4 Microbe<Ellie, NitriteReducingBacteria>, 4 Plant)
DO endTurn()
BECOME Dad
DO useAction(1, VenusianInsects)
DO tasks(Microbe<Dad, VenusianInsects>)
DO endTurn()
BECOME Ellie
DO useAction(2, NitriteReducingBacteria, -3 Microbe<Ellie, NitriteReducingBacteria>)
DO tasks(TerraformRating)
DO endTurn()
BECOME Dad
DO useAction(1, AsteroidRights, -1 MC)
DO tasks(Asteroid<Dad, AsteroidRights>)
DO endTurn()
BECOME Ellie
DO tasks(Pass)
BECOME Dad

// Dad: "I will use Martian Zoo to take five money."
DO useAction(1, MartianZoo)
DO tasks(5 MC)

// Dad: "It appears I never used Business Network. So I'm going to use Business Network to
// look at a card. I think that makes me want to take it more, so I will pay three for it."
DO useAction(1, BusinessNetwork)
DO buyCards()
DO tasks(Pass)
BECOME Ellie

// Ellie uses World Government Terraforming to increase oxygen.
DO tasks(OxygenStep! BY Engine)
BECOME Dad

// board-21-13-43.jpg, board-21-14-23.jpg, and both player ledgers: after Generation 8
// transition.
// Pets scores one VP per two animals; this count does not mean five VP.
// Before Generation 9 Research, both players reconciled the physical table against their
// resource logs. Dad took three M€ and an animal on Martian Zoo and discarded a card; Ellie
// took six M€ and one TR.
mode red
exec 3 MC, Animal<MartianZoo>, -ProjectCard
mode purple
BECOME Ellie
mode red
exec 6 MC, TR
mode purple
BECOME Dad

// "You kept all your cards?" "Bada-bing. Yeah. I just hated to give them up."
DO buyCards()
BECOME Ellie
DO buyCards()
BECOME Dad
DO useAction(1, ConvertPlantsSA, -8 Plant)

// "I'm planting a forest. I'm eternally hopeful. I'm going to place on six, four to get two
// steel and two money."
DO tasks(GreeneryTile<Utopia_6_3>, 2 MC, OxygenStep, 2 Steel, TerraformRating)

// "Well, let us just go ahead and use floating hubs to put a cube on aerial mappers."
DO useAction(1, FloatingHabs, -2 MC)
DO tasks(Floater<Dad, AerialMappers>)
BECOME Ellie

// "I'm going to use my extractor balloons, spend the two floaters, increase Venus. To 28."
DO useAction(2, ExtractorBalloons, -2 Floater<Ellie, ExtractorBalloons>)
DO tasks(VenusStep, TerraformRating)

// "Then I'm going to spend three floaters, trade with Aran. This might not be the right call,
// but enchiladas, actually." "Yeah, gain three microbes."
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Enceladus>, FlownTradeFleet<Ellie, Enceladus> FROM ReserveTradeFleet<Ellie>, 3 Microbe<Ellie, NitriteReducingBacteria>, ResetColonyProduction<Enceladus>, -3 ColonyProduction<Enceladus>)
BECOME Dad

// "I will use aerial mappers to remove a floater from aerial mappers and get a card."
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
DO endTurn()
BECOME Ellie

// "Nine for sponsored academies." "I pitch a card." "And then you get three cards and I also
// get a card."
DO playCard(SponsoredAcademies, -7 MC)
DO tasks(Animal<Ellie, VenusianAnimals>, -ProjectCard<Ellie, Hand>, 2 ProjectCard, SponsoredAcademies_Signal, ProjectCard)
BECOME Dad
DO tasks(ProjectCard)
BECOME Ellie

// "La Grange Observatoire." "One titanium and four money." "I believe I get a card."
DO playCard(LagrangeObservatory, -Titanium, -4 MC)
DO tasks(Animal<Ellie, VenusianAnimals>, ProjectCard)
BECOME Dad

// "I am going to use my business network to look at this card." "I will not buy this card."
DO useAction(1, BusinessNetwork)

// Decline buying the revealed card.
DO tasks(-ProjectCard<Selecting>)
DO buyCards()
DO endTurn()
BECOME Ellie

// "May as well. Spend all nine of my steel on aquifer pumping."
DO playCard(AquiferPumping, -9 Steel)
DO endTurn()
BECOME Dad

// "I'm gonna play advanced alloys." "It costs me nine." "No discounts."
DO playCard(AdvancedAlloys, -9 MC)
DO tasks(-ProjectCard<Hand>, ProjectCard)

// "I am gonna go ahead and play Solar Logistics." "But I spend four titanium on that." "I get
// two titanium from it. I get a minimal on Martian Zoo. I get a card."
DO playCard(SolarLogistics, -4 Titanium)
DO tasks(ProjectCard, Animal<Dad, MartianZoo>, 2 Titanium)
BECOME Ellie

// "I spend eight on aquifer pumping. The action, that is." "Row seven, column six, I
// believe."
// "Yes, two plants, two money."
DO useAction(1, AquiferPumping, -8 MC)
DO tasks(OceanTile<Utopia_7_6>, 2 MC, TerraformRating, 2 Plant)
DO endTurn()
BECOME Dad

// "I'm gonna play Ice Asteroid. For four titanium and three wheel. It's a space event. So I
// draw a card from solar logistics. I place two ocean tiles." "So that gets me two TR and 10
// money."
DO playCard(IceAsteroid, -4 Titanium, -3 MC)
DO tasks(OceanTile<Utopia_7_5>, OceanTile<Utopia_8_6>, ProjectCard, 2 MC, 2 MC, TerraformRating, 2 MC, 2 MC, 2 MC, TerraformRating)

// "Yeah, what the hell, let's buy a standard project, shall we?" "Aquifer." "I'm just gonna
// take two plants by placing on four, five."
DO useAction(1, AquiferSP, -18 MC)
DO tasks(OceanTile<Utopia_4_5>, TerraformRating, 2 Plant)
BECOME Ellie

// "I spend 26 money. Lose two energy productions. Gain five money productions." "Place." "It'll
// go right here. That would be row six, column five. Yeah. For six money and two plants."
// "Actually, any chance I can undo and play conscription first?" "Yeah, sure."
DO playCard(Conscription, -5 MC)
DO tasks(Conscription_NextCardEffect)
DO playCard(Capital, -10 MC)
DO tasks(CityTile<Utopia_6_5>, PROD[-2 Energy, 5 MC], 2 MC, 2 MC, 2 MC, 2 Plant, CapitalMarker<CityTile<Utopia_6_5, Ellie>>)
BECOME Dad
DO tasks(Animal<Dad, Pets>)

// "Three steel, nine MC. That gets me tectonic stress power." "And so I get my
// three energy production."
DO playCard(TectonicStressPower, -3 Steel, -9 MC)
DO tasks(PROD[3 Energy])
DO endTurn()
BECOME Ellie

// "I'm just gonna heat boop." "A heat boop has been done. That means converting heat to
// temperature."
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)

// "I will use nitrate-reducing bacteria, remove three microbes, gain a TR."
DO useAction(2, NitriteReducingBacteria, -3 Microbe<Ellie, NitriteReducingBacteria>)
DO tasks(TerraformRating)
BECOME Dad

// "I will use asteroid rights to take one asteroid off of asteroid rights and give myself
// two titanium."
DO useAction(2, AsteroidRights, -Asteroid<Dad, AsteroidRights>)
DO tasks(2 Titanium)
DO useAction(1, ConvertPlantsSA, -8 Plant)

// "I will plant a greenery or plant a forest, as they like to call it on this app." "I'll
// put it next to my city for two money."
DO tasks(GreeneryTile<Utopia_5_2>, 2 MC, OxygenStep, TerraformRating)
BECOME Ellie

// "You know, it occurred to me I can probably spend 15 on a final Venus boop."
DO useAction(1, AirScrappingSP, -15 MC)
DO tasks(VenusStep, TerraformRating)

// "I'm going to psychrophile."
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
BECOME Dad

// "Well, I'm going to heat boop." "I'm going to heat boop. I didn't move it either time."
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)
BECOME Ellie

// "Anyways, I stratoburb."
DO useAction(1, StratosphericBirds)
DO tasks(Animal<Ellie, StratosphericBirds>)
DO endTurn()
BECOME Dad

// "Energy market. Reduce energy production to four, gain eight money."
DO useAction(2, EnergyMarket)
DO tasks(PROD[-Energy], 8 MC)
DO endTurn()
BECOME Ellie
DO tasks(Pass)
BECOME Dad

// "I'm going to play Lunar Exports, which costs me three titanium and four money." "And I
// get a card from Point Luna." "I'm actually going to take the money production."
DO playCard(LunarExports, -3 Titanium, -2 MC)
DO tasks(PROD[5 MC], ProjectCard, Animal<Dad, MartianZoo>)

// "Let's play Solar Net for seven real money. I draw two cards."
DO playCard(Solarnet, -7 MC)
DO tasks(2 ProjectCard)

// "Let's play Algae for ten money." "I get two plant production and one plant."
DO playCard(Algae, -10 MC)
DO tasks(Plant, PROD[2 Plant])

// "I will go ahead and use my Martian Zoo now to take eight money."
DO useAction(1, MartianZoo)
DO tasks(8 MC)

// "I will use my Venusian Insects to take a Venusian insect, which is apparently a kind of
// microbe now."
DO useAction(1, VenusianInsects)

// "Okay, here goes insects." "And I get one, two, three, four, five, five plant production."
DO tasks(Microbe<Dad, VenusianInsects>)
DO assignWildTag(PlantTag)
DO playCard(Insects, -9 MC)
DO tasks(PROD[5 Plant])
DO tasks(Pass)

// "The world government is me, and well, I'm not going to do oxygen. I do temperature up to
// minus two."
DO tasks(TemperatureStep! BY Engine)
DO tasks(-ProjectCard<Selecting>)

// board-16-19-30.jpg and both player ledgers: after Generation 9 transition.
// "Well, I'm buying three cards." "All right, Ellie buys three cards, and I'm going to stupidly
// buy two cards. You know what? I'm going to buy three cards. Talk about stupid. Three cards."
DO buyCards()
BECOME Ellie
DO tasks(-ProjectCard<Selecting>)
DO buyCards()
DO useAction(1, ConvertPlantsSA, -8 Plant)

// "I will plant forest, put my greenery on... Looks like 5-5, right?" "Okay. Two money and a
// plant."
DO tasks(GreeneryTile<Utopia_5_5>, 2 MC, OxygenStep, Plant, TerraformRating)

// "And I will greenery standard project." "But you've got two TR from one move." "Yeah, and
// an extra for being the one to get it." "Anyways, the second one goes... 2-1."
DO useAction(1, GreenerySP, -23 MC)
DO tasks(GreeneryTile<Utopia_2_1>, 2 MC, OxygenStep, TerraformRating, TemperatureStep, TerraformRating, OceanTile<WaterArea>)
BECOME Dad

// "I am feeling like I had better put a cute little city down while I can. So I paid for
// standard project." "4-4 for two money and two plants."
DO useAction(1, CitySP, -25 MC)
DO tasks(CityTile<Utopia_4_4>, PROD[MC], 2 MC, 2 Plant, Animal<Dad, Pets>)

// "Ecological zone. Cost me 12 entire." "Well, for these two, I get two animals right away."
// "Putting it on 2-2?" "Yes. For two steel."
DO playCard(EcologicalZone, -12 MC)
DO tasks(EcologicalZone_SpecialTile<Utopia_2_2>, Animal<Dad, EcologicalZone>, Animal<Dad, EcologicalZone>, 2 Steel)
BECOME Ellie

// "Eight for cryosleep because I have science tag discount. Pay eight. And I get a new
// animal."
DO playCard(CryoSleep, -8 MC)
DO tasks(Animal<Ellie, VenusianAnimals>)

// "And I spend two energy to trade with Miranda for two animals and a card."
DO useAction(2, TradeSA, -2 Energy)
DO tasks(Trade<Miranda>, FlownTradeFleet<Ellie, Miranda> FROM ReserveTradeFleet<Ellie>, 2 Animal<Ellie, StratosphericBirds>, ProjectCard, ResetColonyProduction<Miranda>, -2 ColonyProduction<Miranda>)
BECOME Dad

// "I will use my business network to look at a card. Absolutely not."
DO useAction(1, BusinessNetwork)

// Decline buying the revealed card.
DO tasks(-ProjectCard<Selecting>)
DO buyCards()

// "Heat boob." "Now your turn."
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)
BECOME Ellie

// "Back to viral research, baby. Cost you eight?" "Yes. Almost forgot. Draw one card."
// "I will choose Nitrate Reducing Bacteria." "Six microbes."
DO playCard(BactoviralResearch, -8 MC)
DO tasks(6 Microbe<Ellie, NitriteReducingBacteria>, Animal<Ellie, VenusianAnimals>, ProjectCard)
DO endTurn()
BECOME Dad

// "Herbivores, again with the full price." "I do add an animal to this card and an animal to
// Ecozone. And you lose a plant production." "Oh, shit, I don't have any plant production."
DO playCard(Herbivores, -12 MC)
DO tasks(Animal<Dad, EcologicalZone>, Animal<Dad, Herbivores>, PROD[-Plant])
DO endTurn()
BECOME Ellie

// "Jovian lanterns for 20." "Increase your TR one step." "Add two floaters to any card. I
// will
// add it to itself."
DO playCard(JovianLanterns, -20 MC)
DO tasks(2 Floater<Ellie, JovianLanterns>, TerraformRating)
DO endTurn()
BECOME Dad
DO useAction(1, ConvertPlantsSA, -8 Plant)

// "Plant boop, plant boop." "So two money and two plants." "And played greenery. So I add a
// minimal to herbivores. I add two of them."
DO tasks(GreeneryTile<Utopia_4_3>, OxygenStep, Plant, Animal<Dad, Herbivores>, TerraformRating)
DO useAction(1, ConvertPlantsSA, -8 Plant)
DO tasks(GreeneryTile<Utopia_5_4>, 2 MC, OxygenStep, Plant, Animal<Dad, Herbivores>, TerraformRating)
BECOME Ellie

// "I use Jovian Lantern, spend a titanium to add two floaters here."
DO useAction(1, JovianLanterns, -Titanium)
DO tasks(2 Floater<Ellie, JovianLanterns>)

// "Actually, I'm just going to go like add a thing to extractor balloons."
DO useAction(1, ExtractorBalloons)
DO tasks(Floater<Ellie, ExtractorBalloons>)
BECOME Dad
DO useAction(1, MartianZoo)
DO tasks(8 MC)

// "I think I'm going to use Martian Zoo to take eight money and then spend eighteen money on
// lava flows." "Two cards and four money."
DO playCard(LavaFlows, -18 MC)
DO tasks(LavaFlows_SpecialTile<Utopia_8_5>, TemperatureStep, TerraformRating, TemperatureStep, TerraformRating, 2 MC, 2 MC, 2 ProjectCard)

// Dad's ledger omitted the two TR from Lava Flows' temperature steps.
mode red
exec -2 TR
mode purple
BECOME Ellie

// "Oh, I add a Strato Bird."
DO useAction(1, StratosphericBirds)
DO tasks(Animal<Ellie, StratosphericBirds>)

// "Oh, probably be smart for me to do my own heat boob."
DO useAction(1, ConvertHeatSA, -8 Heat)
DO tasks(TemperatureStep, TerraformRating)
BECOME Dad

// "It's weird, but I'm gonna play a card I've never played before in my life. Food Factory."
// "And three real gives me four money production. Takes away one of my plant production."
DO playCard(FoodFactory, -3 Steel, -3 MC)
DO tasks(PROD[-Plant, 4 MC])
DO endTurn()
BECOME Ellie

// "I will add Psychrophile and I will remove three nitrites for a TR."
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
DO useAction(2, NitriteReducingBacteria, -3 Microbe<Ellie, NitriteReducingBacteria>)
DO tasks(TerraformRating)
BECOME Dad
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)

// "I'm going to sell a patent to get a money and then spend two money on floating habs. To
// use
// floating habs to put a floater onto aerial mappers."
DO useAction(1, FloatingHabs, -2 MC)
DO tasks(Floater<Dad, AerialMappers>)
BECOME Ellie

// "I'll pay three psychrophiles for green houses." "Gain one plant for each city tile in
// play. That's one, two, three, four, five."
DO playCard(Greenhouses, -3 Microbe<Psychrophiles>)
DO tasks(5 Plant)
DO useAction(1, ConvertPlantsSA, -8 Plant)

// "And I will greenery boop." "It's six, six, sorry." "It's the last possible spot next to my
// capital for two money."
DO tasks(GreeneryTile<Utopia_6_6>, 2 MC, OxygenStep, TerraformRating)
BECOME Dad

// "I'm going to use aerial mappers to take a floater off of aerial mappers and draw a card."
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
DO endTurn()
BECOME Ellie
DO tasks(Pass)
BECOME Dad

// "And then I'm going to use energy market to reduce energy production and give myself eight
// money."
DO useAction(2, EnergyMarket)
DO tasks(PROD[-Energy], 8 MC)
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)

// "I guess I play Dawn City for three titanium." "I lose an energy production. I gain a
// titanium production." "And so when I place that, I believe I get a pet."
// "Oh shit. I don't." "Okay, since I already committed to it, what I will do is I will sell a
// patent to get one money, spend nine money on robotic workforce just to make an honest card
// out of it." "I'll copy industrial microbes." "Robotic workforce in Dawn City, we pretended
// they were in that order."
DO playCard(RoboticWorkforce, -9 MC)
DO tasks(-ProjectCard<Hand>, CopyProductionBox<Dad, IndustrialMicrobes>, ProjectCard, PROD[Energy, Steel])
DO assignWildTag(ScienceTag)
DO playCard(DawnCity, -3 Titanium)
DO tasks(PROD[-Energy, Titanium], CityTile<DawnCity_RemoteArea>, Animal<Dad, Pets>)

// "I'm gonna add a Venusian insect."
DO useAction(1, VenusianInsects)
DO tasks(Microbe<Dad, VenusianInsects>)
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)

// "I'm going to yet again, sell a patent for one money and spend that one money on asteroid
// rights to put an asteroid onto that."
DO useAction(1, AsteroidRights, -1 MC)
DO tasks(Asteroid<Dad, AsteroidRights>)

// "But I'll take the cards using three energy for the Pluto, take two cards."
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Pluto>, FlownTradeFleet<Dad, Pluto> FROM ReserveTradeFleet<Dad>, 2 ProjectCard, ResetColonyProduction<Pluto>, -3 ColonyProduction<Pluto>)
DO tasks(Pass)
BECOME Ellie

// Ellie uses World Government Terraforming to raise oxygen to 12%.
DO tasks(OxygenStep! BY Engine)
BECOME Dad
DO tasks(-3 ProjectCard<Selecting>)

// board-16-44-30.jpg and both player ledgers: after Generation 10 transition.
// "Man. Yeah. I'm buying two. I'll buy one. I'm gonna actually buy it."
DO buyCards()
BECOME Ellie
DO tasks(-2 ProjectCard<Selecting>)
DO buyCards()
BECOME Dad
DO useAction(1, ConvertPlantsSA, -8 Plant)

// "Yep. Boop, boop. Indeed. And the game will officially end this round."
// "One goes here for just two money. That is three, four." "I think I'll just take the four
// money down here."
DO tasks(GreeneryTile<Utopia_3_4>, 2 MC, OxygenStep, Animal<Dad, Herbivores>, TerraformRating)
DO useAction(1, ConvertPlantsSA, -8 Plant)

// The second placement is gesture-only in the transcript; its four-M€ ocean income locates
// it at the open land area between the row-six and row-seven oceans.
DO tasks(GreeneryTile<Utopia_7_4>, 2 MC, 2 MC, OxygenStep, Animal<Dad, Herbivores>, TerraformRating)
BECOME Ellie

// "I will... pay two energy to trade with Miranda... for one aminal and a card. This time
// I'll
// put the aminal on Venusian."
DO useAction(2, TradeSA, -2 Energy)
DO tasks(Trade<Miranda>, FlownTradeFleet<Ellie, Miranda> FROM ReserveTradeFleet<Ellie>, Animal<Ellie, VenusianAnimals>, ProjectCard, ResetColonyProduction<Miranda>, -ColonyProduction<Miranda>)

// "Productive outpost for zero... Gain all my colony bonuses, which is literally just draw a
// card."
DO playCard(ProductiveOutpost)
DO tasks(GiveColonyBonuses, ProjectCard)
BECOME Dad

// "I pay fourteen... Mogul." "Yeah. I think I got that one."
DO useAction(2, FundAwardSA, -14 MC)
DO tasks(Mogul)

// "Listen, all of y'all. It's sabotage. So... You lose... Seven money, and that's it."
DO playCard(Sabotage, -1 MC)
DO tasks(-7 MC<Ellie>)
BECOME Ellie

// "I'm going to spend thirteen money, no titanus." "Lose two money production." "Place a
// colony on Pluto. To get two cards."
DO playCard(PioneerSettlement, -13 MC)
DO tasks(Colony<Ellie, Pluto>, PROD[-2 MC], 2 ProjectCard)
DO endTurn()
BECOME Dad

// "I'm going to spend two on floating habs. To put a dingus on aerial mappers." "Use aerial
// mappers to draw a card."
DO useAction(1, FloatingHabs, -2 MC)
DO tasks(Floater<Dad, AerialMappers>)
DO useAction(2, AerialMappers, -Floater<Dad, AerialMappers>)
DO tasks(ProjectCard)
BECOME Ellie
DO useAction(1, JovianLanterns, -Titanium)
DO tasks(2 Floater<Ellie, JovianLanterns>)
DO endTurn()
BECOME Dad

// "Immigrant City, spending six worth of steel and seven rail." "I better decrease my energy
// production, decrease my money production by two and then back up one." "Five, six, on five,
// six."
DO playCard(ImmigrantCity, -2 Steel, -7 MC)
DO tasks(CityTile<Utopia_5_6>, PROD[-Energy, -2 MC], 2 MC, 2 Plant, Animal<Dad, Pets>, PROD[MC])
DO endTurn()
BECOME Ellie
DO useAction(2, NitriteReducingBacteria, -3 Microbe<Ellie, NitriteReducingBacteria>)
DO tasks(TerraformRating)
DO endTurn()
BECOME Dad

// "I'm gonna put it right here, two energy."
// The City standard project and Immigrant City each increase M€ production for this
// placement.
DO useAction(1, CitySP, -25 MC)
DO tasks(CityTile<Utopia_2_3>, PROD[MC], 2 Energy, Animal<Dad, Pets>, PROD[MC])

// "And for my second trick, commercial district, from sixteen, lose an energy production,
// gain four money production, place a shitty tile, not a shitty tile."
DO playCard(CommercialDistrict, -16 MC)
DO tasks(CommercialDistrict_SpecialTile<Utopia_3_3>, PROD[-Energy, 4 MC], Steel)

// Dad confirms he forgot Immigrant City's trigger. His ledger records only the standard
// project's one M€ production step, so remove the omitted Immigrant City step here.
mode red
exec PROD[-MC]
mode purple
BECOME Ellie
DO useAction(1, Psychrophiles)
DO tasks(Microbe<Ellie, Psychrophiles>)
DO endTurn()
BECOME Dad

// "I'm gonna play robot pollinators for all of my money. It gives me a plant production and
// one plant per plant tag. One, two, three, four, five. Five plants."
DO playCard(RobotPollinators, -9 MC)
DO tasks(PROD[Plant], 5 Plant)
DO useAction(1, ConvertPlantsSA, -8 Plant)

// "I'm just gonna do the plant boop now." "Did not give me TR good." "This plant boop will go
// here for two energy and a card. That is two, four."
DO tasks(GreeneryTile<Utopia_2_4>, OxygenStep, 2 Energy, ProjectCard, Animal<Dad, Herbivores>)
BECOME Ellie

// "I'm gonna add a strato bird." "I'm at 15 strato birds."
DO useAction(1, StratosphericBirds)
DO tasks(Animal<Ellie, StratosphericBirds>)
DO endTurn()
BECOME Dad

// "Methane from Titan." "I'm gonna spend six titanium." "I mostly played it for the two
// points."
DO playCard(MethaneFromTitan, -6 Titanium)
DO tasks(PROD[2 Heat, 2 Plant])
DO endTurn()
BECOME Ellie

// "I add an extractor balloon."
DO useAction(1, ExtractorBalloons)
DO tasks(Floater<Ellie, ExtractorBalloons>)
DO endTurn()
BECOME Dad

// "I'll go ahead and use Martian Zoo to take eight money."
DO useAction(1, MartianZoo)
DO tasks(8 MC)
DO endTurn()
BECOME Ellie

// "I'll use local heat trapping. One money. Spend five heat. And I will add two Venusian
// animals."
DO playCard(LocalHeatTrapping, -1 MC)
DO tasks(2 Animal<Ellie, VenusianAnimals>, -5 Heat)
DO endTurn()
BECOME Dad

// "Trading colony for my four titanium." "No, no, no. I'm gonna get three microbes." "Three
// microbes which go on to Venusian insects."
DO playCard(TradingColony, -4 Titanium)
DO tasks(Colony<Dad, Enceladus>, 3 Microbe<Dad, VenusianInsects>)
DO endTurn()
BECOME Ellie

// "Airliners for 11 requires that you have three floaters." "Gain two money production, add
// two floaters to another card, which will be Jovian lanterns."
DO playCard(Airliners, -11 MC)
DO tasks(2 Floater<Ellie, JovianLanterns>, PROD[2 MC])
DO endTurn()
BECOME Dad

// "Now, I'm going to fly my little boat to Angelatus." "I get three and one." "They all four
// go on to the New Zealand insects."
DO useAction(2, TradeSA, -3 Energy)
DO tasks(Trade<Enceladus>, ColonyProduction<Enceladus>, FlownTradeFleet<Dad, Enceladus> FROM ReserveTradeFleet<Dad>, 3 Microbe<Dad, VenusianInsects>, Microbe<Dad, VenusianInsects>, ResetColonyProduction<Enceladus>, -2 ColonyProduction<Enceladus>)
DO endTurn()
BECOME Ellie

// "My seven psychrophiles and three real." "Increase money production two steps. Increase
// plant production three steps. Increase... No, gain two plants."
DO playCard(KelpFarming, -3 MC, -7 Microbe<Psychrophiles>)
DO tasks(PROD[2 MC, 3 Plant], 2 Plant)
DO endTurn()
BECOME Dad

// "Just to be funny, I'm going to play one for land claim, just so you can go there."
// The source does not identify the claimed area; Utopia_1_1 is a neutral test inference.
DO playCard(LandClaim, -1 MC)
DO tasks(LandClaimMarker<Utopia_1_1>)
DO endTurn()
BECOME Ellie

// "I sell a card for a money."
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)
DO endTurn()
BECOME Dad

// "I'll go ahead and sell five cards for five money. None of them have victory points on them."
DO useAction(1, SellPatents)
DO tasks(5 MC FROM ProjectCard<Hand>)
DO endTurn()
BECOME Ellie

// "Wait. I sell a card for a money."
DO useAction(1, SellPatents)
DO tasks(MC FROM ProjectCard<Hand>)
DO endTurn()
BECOME Dad

// "I'll spend eight on lightning harvest. One energy product, one money product, and a point."
DO playCard(LightningHarvest, -8 MC)
DO tasks(PROD[Energy, MC])
DO endTurn()
BECOME Ellie

// Ellie's ledger groups Media Archives' net thirteen-M€ gain with the twenty-five-M€ Water
// Import from Europa payment below as one twelve-M€ debit at entry 325.
// "Sell three cards for three money. Wait, actually. Hold on. Just in case that can be useful
// somehow. I'll sell two for two."
DO useAction(1, SellPatents)
DO tasks(2 MC FROM ProjectCard<Hand>)

// "I'm going to play Media Archives." "I have eight. I have 13, so that's 21 money for you."
DO playCard(MediaArchives, -8 MC)
DO tasks(21 MC)
BECOME Dad

// "Oh my god, I forgot to use my business network. Fine, I'll use that then."
DO useAction(1, BusinessNetwork)

// Decline buying the revealed card.
DO tasks(-ProjectCard<Selecting>)
DO buyCards()
DO endTurn()
BECOME Ellie

// "Finally playing. Water Import from Europa."
DO playCard(WaterImportFromEuropa, -25 MC)
DO endTurn()
BECOME Dad

// The transcript identifies Sub-Zero Salt Fish in Dad's hand, while the generic reconstructed
// hand is one card short after the sourced plays and patent sales.
mode red
exec ProjectCard
mode purple

// "Okay, now I can use Energy Market. Get all up to 12 money."
DO useAction(2, EnergyMarket)
DO tasks(PROD[-Energy], 8 MC)

// "Play Sub-Zero Salt Fish." "Now you lose plant production. I spend five on that." "It's an
// animal tag." "So I get an Ecomole."
DO playCard(SubZeroSaltFish, -5 MC)
DO tasks(PROD[-Plant<Ellie>], Animal<Dad, EcologicalZone>)

// Dad took Energy Market's eight M€ but did not record its energy-production decrease.
mode red
exec PROD[Energy]
mode purple
BECOME Ellie

// "But, well, still, for the means, I can play Predators."
// "Well, I guess you're going to take my Ecomal, then."
DO playCard(Predators, -14 MC)
DO useAction(1, Predators)
DO tasks(-Animal<Dad, EcologicalZone<Dad>>, Animal<Ellie, Predators>)
BECOME Dad

// "And then, you know, for all the good it'll do, I'll just use the action to add another
// animal."
DO useAction(1, SubZeroSaltFish)
DO tasks(Animal<Dad, SubZeroSaltFish>)
DO endTurn()
BECOME Ellie

// "Thanks to my steel, I can spend all six and three real for a point from Artificial Lake."
DO playCard(ArtificialLake, -6 Steel, -3 MC)
DO tasks(OceanTile<LandArea>! OR (9 OceanTile: Ok))
DO endTurn()
BECOME Dad
DO tasks(Pass)
BECOME Ellie
DO tasks(Pass)
BECOME Dad

// Both ledgers: after the final production phase and before final greenery placement.
// The resource apps incremented their display to 12 during final production; the engine keeps
// the completed action generation numbered 11.
// Reconcile the mistakes still present in the physical game so final greenery and scoring use
// the ordinary-rules state. This net delta is characterized by the separate exMachina-free
// replay, not by the ledgers: retain Lava Flows' two TR and Kaguya Tech's Pets animal, retain
// Immigrant City's M€-production trigger, and retain Energy Market's production decrease.
mode red
exec 4 MC, 2 TR, PROD[MC, -Energy], -Energy, Animal<Pets>
mode purple
DO useAction(1, ConvertPlantsSA, -8 Plant)

// "So I'm going to 1-2 and 1-3."
DO tasks(GreeneryTile<Utopia_1_2>, Animal<Dad, Herbivores>)
DO useAction(1, ConvertPlantsSA, -8 Plant)
DO tasks(GreeneryTile<Utopia_1_3>, 2 Energy, Animal<Dad, Herbivores>)

// Decline another final greenery placement.
DO tasks(1 Ok)
BECOME Ellie

// Decline the final greenery placement.
DO tasks(1 Ok)

// "Resource points on cards. One, four. Holy shit. Yeah. One, four, eight, and fifteen."
// The earlier explicit count, "I'm at 15 strato birds," identifies the last value.
// The spoken 118-115 tally omitted Dad's four Herbivores points and the two Lava Flows TR that
// the corrected scoring state retains. Ellie's spoken total is one point below the complete
// replay categories, which sum to 116.
