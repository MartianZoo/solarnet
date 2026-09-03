package dev.martianzoo.tfm.web.gameviewer.games

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.web.gameviewer.RecordedGame
import dev.martianzoo.tfm.web.gameviewer.cardnames.*

public class OtbGame20260818 : RecordedGame() {
  private val colonyTiles = listOf("Enceladus", "Miranda", "Europa", "Io", "Pluto")

  protected override val config: GameConfig =
      GameConfig(
          """
          UtopiaMap
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion
          PromoCardPack

          Ecologist, Merchant, Metallurgist, Tactician, Hoverlord
          Constructor, Excentric, Highlander, Mogul, Traveller, Venuphile
          ${colonyTiles.joinToString()}
          """,
          "Green",
          "Yellow",
      )

  protected override fun play() {
    TfmWorkflow.Auto(game).launch()
    val green = game.tfm(Player.PLAYER1)
    val yellow = game.tfm(Player.PLAYER2)

    green.playCorp(PointLuna, 7)

    yellow.playCorp(ValleyTrust, 5)

    green.turn {
      playPrelude(Biofuels)

      playPrelude(Donation)
    }

    yellow.turn {
      playPrelude(Supplier)

      playPrelude(MartianIndustries)
    }

    green.turn {
      playProject(Pets, 10)
    }

    yellow.turn {
      stdAction("DoRequiredActions") {
        playPrelude(DoubleDown) { doTask("CopyPrelude<$MartianIndustries>") }
      }

      playProject(Psychrophiles, 2)
    }

    green.turn {
      playProject(AerialMappers, 11)
    }

    yellow.turn {
      playProject(ForcedPrecipitation, 8)
    }

    green.turn {
      cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    }

    yellow.turn {
      playProject(ExtractorBalloons, 21)
    }

    green.pass()
    yellow.turn {
      cardAction1(Psychrophiles)

      cardAction2(ExtractorBalloons)

      cardAction1(ForcedPrecipitation)
      yellow.pass()
    }

    green.wgt("OxygenStep")

    yellow.buyCards(4)

    green.buyCards(3)

    yellow.turn {
      playProject(MiningRights, 1, steel = 4) { placeTile(3, 6) }

      playProject(EnergyTapping, 3) { doTask("PROD[-E<Green>]") }
    }

    green.turn {
      playProject(CeosFavoriteProject, 1) { addCardResources(AerialMappers) }
    }

    yellow.turn {
      cardAction1(ExtractorBalloons)
    }

    green.turn {
      cardAction2(AerialMappers)
    }

    yellow.turn {
      cardAction1(Psychrophiles)
    }

    green.turn { sellPatents(1) }

    yellow.turn {
      cardAction1(ForcedPrecipitation)
    }

    green.turn {
      playProject(SixteenPsyche, 28, titanium = 1)
    }

    yellow.pass()
    green.pass()

    yellow.wgt("VenusStep")

    green.buyCards(1)

    yellow.buyCards(0)

    green.turn {
      playProject(ImportedHydrogen, 1, titanium = 5) {
        addCardResources(Pets)
        placeTile(4, 1)
      }
    }

    yellow.turn {
      cardAction2(ForcedPrecipitation)

      cardAction2(ExtractorBalloons)
    }

    green.turn {
      playProject(Cartel, 8)
    }

    yellow.turn {
      playProject(ColonizerTrainingCamp, steel = 4)
    }

    green.turn { sellPatents(1) }

    yellow.turn {
      sellPatents(1)

      playProject(BeamFromAThoriumAsteroid, 26, titanium = 2)
    }

    green.turn {
      playProject(ResearchCoordination, 4)
    }

    yellow.turn {
      cardAction1(Psychrophiles)
    }

    green.turn {
      playProject(VenusGovernor, 4, butFirst = assignAllWildTags("VenusTag"))
    }

    yellow.pass()
    green.turn {
      cardAction2(AerialMappers)
      green.pass()
    }

    green.wgt("TemperatureStep")

    yellow.buyCards(2)
    green.buyCards(2)

    yellow.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }
    }

    green.turn {
      playProject(MarsUniversity, 8) { doTask("-ProjectCard") }
    }

    yellow.turn {
      playProject(Flooding, 7) { placeTile(3, 1) }

      playProject(Potatoes, 0) {
        doTask("PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
      }
    }

    green.turn {
      playProject(MercurianAlloys, 3, butFirst = assignAllWildTags("ScienceTag"))
    }

    yellow.turn {
      cardAction1(Psychrophiles)
    }

    green.turn {
      playProject(AsteroidRights, 2, titanium = 2)
    }

    yellow.turn {
      playProject(Mine, steel = 2)
    }

    green.turn {
      cardAction2(AsteroidRights) { doTask("2 T") }
    }

    yellow.turn {
      cardAction1(ForcedPrecipitation)
      cardAction1(ExtractorBalloons)
    }

    green.turn {
      playProject(FloatingHabs, 5, butFirst = assignAllWildTags("ScienceTag"))
    }

    yellow.turn {
      sellPatents(1)

      playProject(NitriteReducingBacteria, 11)
    }

    green.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    yellow.turn {
      cardAction2(NitriteReducingBacteria)
    }

    green.turn { cardAction2(AerialMappers) }

    yellow.pass()
    green.pass()

    yellow.wgt("VenusStep")

    green.buyCards(1)
    yellow.buyCards(1)

    green.turn {
      playProject(EnergyMarket, 3)
    }

    yellow.turn {
      playProject(HydrogenToVenus, 5, titanium = 2) { addCardResources(ForcedPrecipitation) }

      playProject(HermeticOrderOfMars, 10)
    }

    green.turn {
      cardAction1(EnergyMarket, x = 3)
      stdAction("TradeAction", 2) { doTask("Trade<Io>") }
    }

    yellow.turn {
      playProject(StratosphericBirds, 12) { doTask("-Floater<$ForcedPrecipitation>") }

      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(StratosphericBirds)
      }
    }

    green.turn {
      playProject(BigAsteroid, titanium = 7) { doTask("-Plant<Yellow>") }
    }

    yellow.turn {
      convertHeat()
      convertHeat()
    }

    green.turn {
      playProject(LunarMining, 11, butFirst = assignAllWildTags("EarthTag"))
    }

    yellow.turn { cardAction1(StratosphericBirds) }

    green.turn { cardAction2(AsteroidRights) { doTask("2 T") } }

    yellow.turn {
      cardAction1(ForcedPrecipitation)
      cardAction1(ExtractorBalloons)
    }

    green.turn {
      playProject(LunaMetropolis, 1, titanium = 5, butFirst = assignAllWildTags("EarthTag"))
    }

    yellow.turn {
      cardAction1(Psychrophiles)
      cardAction1(NitriteReducingBacteria)
    }

    green.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
      cardAction2(AerialMappers)
    }

    yellow.pass()
    green.turn {
      convertHeat()
      pass()
    }

    green.wgt("OceanTile<Utopia_9_8>")

    green.buyCards(2)
    yellow.buyCards(1)

    yellow.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(NitriteReducingBacteria)
      }
    }

    green.turn {
      playProject(IndustrialMicrobes, 12)
      stdAction("ClaimMilestone", beforeAction = assignAllWildTags("MicrobeTag")) {
        doTask("Ecologist")
      }
    }

    green.exMachina("PROD[-S, -E]")

    yellow.turn {
      cardAction2(ForcedPrecipitation)
      cardAction2(ExtractorBalloons)
    }
    green.turn {
      playProject(ImportOfAdvancedGhg, 1, titanium = 2)

      stdAction("ClaimMilestone") { doTask("Metallurgist") }
    }
    yellow.turn { stdAction("ClaimMilestone") { doTask("Tactician") } }
    green.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
      cardAction2(AerialMappers)
    }
    yellow.turn { cardAction1(Psychrophiles) }
    green.turn {
      sellPatents(1)

      playProject(HiredRaiders, 1) { doTask("3 M<Green> FROM M<Yellow>") }
    }
    yellow.turn {
      cardAction2(NitriteReducingBacteria)
    }
    green.turn {
      cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }
    }
    yellow.turn {
      convertHeat()
    }
    green.turn {
      cardAction1(EnergyMarket, x = 1)

      convertPlants { placeTile(4, 2) }
    }

    green.exMachina("TR")

    yellow.turn {
      playProject(NoctisCity, 6, steel = 6) { placeTile(3, 2) }
    }
    green.pass()
    yellow.turn {
      cardAction1(StratosphericBirds)
    }
    yellow.pass()

    yellow.wgt("OceanTile<Utopia_6_4>")

    green.exMachina("-TR, -1 MC, PROD[S, E], S, E")

    green.buyCards(2)
    green.exMachina("6 MC")
    yellow.buyCards(3)

    green.turn {
      stdAction("FundAward") { doTask("Traveller") }
    }

    yellow.turn {
      playProject(MarketManipulation, 1) {
        doTask("ColonyProduction<Pluto> FROM ColonyProduction<Io>")
      }

      stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }
    }

    green.turn {
      playProject(MartianZoo, 12)
    }

    yellow.turn {
      playProject(IoSulphurResearch, 15) { doTask("3 ProjectCard") }

      yellow.exMachina("-2 MC")
    }

    green.turn {
      playProject(NuclearPower, 10)
    }

    yellow.turn {
      playProject(AirScrappingExpedition, 13) { addCardResources(ForcedPrecipitation) }
    }

    green.turn {
      playProject(MirandaResort, titanium = 3, butFirst = assignAllWildTags("EarthTag"))
    }

    yellow.turn {
      cardAction2(ForcedPrecipitation)
    }

    green.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    yellow.turn { cardAction1(ExtractorBalloons) }

    green.turn { cardAction2(AerialMappers) }

    yellow.turn { cardAction1(StratosphericBirds) }

    green.turn {
      playProject(BusinessContacts, 7)
    }

    yellow.turn { cardAction1(Psychrophiles) }

    green.turn {
      playProject(ImportedNitrogen, titanium = 6) { addCardResources(MartianZoo) }
    }

    green.exMachina("-Animal<$MartianZoo>")

    yellow.turn {
      cardAction2(NitriteReducingBacteria)
    }

    yellow.exMachina("-TR")

    green.turn {
      cardAction2(AsteroidRights) { doTask("PROD[1 MC]") }
    }

    yellow.turn { convertHeat() }

    green.turn {
      cardAction1(MartianZoo)
    }

    yellow.turn {
      playProject(NeutralizerFactory, 7)
    }

    green.turn {
      playProject(VenusianInsects, 5)
    }

    yellow.pass()
    green.turn {
      cardAction1(VenusianInsects)

      cardAction2(EnergyMarket)

      playProject(NitrophilicMoss, 8)
      pass()
    }

    green.wgt("VenusStep")

    green.buyCards(2)
    yellow.buyCards(1)

    green.exMachina("-6 MC, -Microbe<$VenusianInsects>")

    yellow.turn {
      playProject(IceMoonColony, 17, titanium = 2) {
        doTask("Colony<Miranda>")
        addCardResources(StratosphericBirds)
        placeTile(8, 7)
      }

      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(StratosphericBirds)
      }
    }

    green.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    yellow.turn {
      playProject(DiversitySupport, 1)
    }

    green.turn {
      playProject(Plantation, 15, butFirst = assignAllWildTags("ScienceTag")) {
        placeTile(5, 3)
      }

      playProject(KaguyaTech, 10) { doTask("CityTile<Utopia_5_3> FROM GreeneryTile<Utopia_5_3>") }
    }

    green.exMachina("-Animal<$Pets>")

    yellow.turn {
      playProject(VenusianAnimals, 13)
    }

    yellow.exMachina("-2 MC")

    green.turn {
      playProject(NitrogenRichAsteroid, 3, titanium = 7) { doTask("PROD[4 Plant]") }
    }

    yellow.turn { cardAction2(ForcedPrecipitation) }

    green.turn { cardAction2(AerialMappers) }

    yellow.turn { cardAction1(ExtractorBalloons) }

    green.turn {
      playProject(BusinessNetwork, 4)
    }

    yellow.turn { cardAction1(Psychrophiles) }

    green.turn { cardAction2(EnergyMarket) }

    yellow.turn { convertHeat() }

    green.turn {
      playProject(HeatTrappers, 2, steel = 2) { doTask("PROD[-2 H<Yellow>]") }
    }

    yellow.turn {
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-E<Green>]") }
    }

    green.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Europa>") }
    }

    yellow.turn { cardAction1(StratosphericBirds) }

    green.turn {
      playProject(ImportedGhg, 7)
    }

    yellow.turn {
      playProject(ImportedNutrients, 11, titanium = 1) { addCardResources(NitriteReducingBacteria) }
    }

    green.turn { cardAction1(VenusianInsects) }

    yellow.turn { cardAction2(NitriteReducingBacteria) }

    green.turn {
      cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }
    }

    yellow.pass()
    green.turn {
      cardAction1(MartianZoo)

      cardAction1(BusinessNetwork) { green.buyCards(1) }
      pass()
    }

    yellow.wgt("OxygenStep")

    green.exMachina("3 MC, Animal<$MartianZoo>, -ProjectCard")
    yellow.exMachina("6 MC, TR")

    green.buyCards(4)
    yellow.buyCards(4)

    green.turn {
      convertPlants { placeTile(6, 3) }

      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    yellow.turn {
      cardAction2(ExtractorBalloons)

      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(NitriteReducingBacteria)
      }
    }

    green.turn { cardAction2(AerialMappers) }

    yellow.turn {
      playProject(SponsoredAcademies, 7)

      playProject(LagrangeObservatory, 4, titanium = 1)
    }

    green.turn {
      cardAction1(BusinessNetwork) {
        green.buyCards(0)
      }
    }

    yellow.turn {
      playProject(AquiferPumping, steel = 9)
    }

    green.turn {
      playProject(AdvancedAlloys, 9) { doTask("-ProjectCard") }

      playProject(SolarLogistics, titanium = 4)
    }

    yellow.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(7, 6)
      }
    }

    green.turn {
      playProject(IceAsteroid, 3, titanium = 4) {
        doTask("OceanTile<Utopia_7_5>")
        doTask("OceanTile<Utopia_8_6>")
      }

      stdProject("AquiferSP") { placeTile(4, 5) }
    }

    yellow.turn {
      playProject(Conscription, 5)
      playProject(Capital, 10) { placeTile(6, 5) }
    }

    green.turn {
      playProject(TectonicStressPower, 9, steel = 3)
    }

    yellow.turn {
      convertHeat()

      cardAction2(NitriteReducingBacteria)
    }

    green.turn {
      cardAction2(AsteroidRights) { doTask("2 T") }

      convertPlants { placeTile(5, 2) }
    }

    yellow.turn {
      stdProject("AirScrappingSP")

      cardAction1(Psychrophiles)
    }

    green.turn {
      convertHeat()
      convertHeat()
    }

    yellow.turn { cardAction1(StratosphericBirds) }

    green.turn { cardAction2(EnergyMarket) }

    yellow.pass()
    green.turn {
      playProject(LunarExports, 2, titanium = 3) { doTask("PROD[5 MC]") }

      playProject(Solarnet, 7)

      playProject(Algae, 10)

      cardAction1(MartianZoo)

      cardAction1(VenusianInsects)
      playProject(Insects, 9, butFirst = assignAllWildTags("PlantTag"))
      pass()
    }

    green.wgt("TemperatureStep")

    green.buyCards(3)
    yellow.buyCards(3)

    yellow.turn {
      convertPlants { placeTile(5, 5) }

      stdProject("GreenerySP") { placeTile(2, 1) }
    }

    green.turn {
      stdProject("CitySP") { placeTile(4, 4) }

      playProject(EcologicalZone, 12) { placeTile(2, 2) }
    }

    yellow.turn {
      playProject(CryoSleep, 8)

      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(StratosphericBirds)
      }
    }

    green.turn {
      cardAction1(BusinessNetwork) {
        green.buyCards(0)
      }

      convertHeat()
    }

    yellow.turn {
      playProject(BactoviralResearch, 8) { addCardResources(NitriteReducingBacteria) }
    }

    green.turn {
      playProject(Herbivores, 12)
    }

    yellow.turn {
      playProject(JovianLanterns, 20) { addCardResources(JovianLanterns) }
    }

    green.turn {
      convertPlants { placeTile(4, 3) }
      convertPlants { placeTile(5, 4) }
    }

    yellow.turn {
      cardAction1(JovianLanterns)

      cardAction1(ExtractorBalloons)
    }

    green.turn {
      cardAction1(MartianZoo)
      playProject(LavaFlows, 18) { placeTile(8, 5) }
    }

    green.exMachina("-2 TR")

    yellow.turn {
      cardAction1(StratosphericBirds)

      convertHeat()
    }

    green.turn {
      playProject(FoodFactory, 3, steel = 3)
    }

    yellow.turn {
      cardAction1(Psychrophiles)
      cardAction2(NitriteReducingBacteria)
    }

    green.turn {
      sellPatents(1)
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    yellow.turn {
      playProject(Greenhouses, 0) {
        doTask("3 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
      }

      convertPlants { placeTile(6, 6) }
    }

    green.turn { cardAction2(AerialMappers) }

    yellow.pass()
    green.turn {
      cardAction2(EnergyMarket)

      sellPatents(1)
      playProject(RoboticWorkforce, 9) {
        doTask("-ProjectCard")
        doTask("CopyProductionBox<$IndustrialMicrobes>")
      }

      playProject(DawnCity, titanium = 3, butFirst = assignAllWildTags("ScienceTag"))

      cardAction1(VenusianInsects)

      sellPatents(1)
      cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }

      stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }
      pass()
    }

    yellow.wgt("OxygenStep")

    green.buyCards(1)
    yellow.buyCards(2)

    green.turn {
      convertPlants { placeTile(3, 4) }

      convertPlants { placeTile(7, 4) }
    }

    yellow.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(VenusianAnimals)
      }

      playProject(ProductiveOutpost, 0)
    }

    green.turn {
      stdAction("FundAward", which = 2) { doTask("Mogul") }

      playProject(Sabotage, 1) { doTask("-7 M<Yellow>") }
    }

    yellow.turn {
      playProject(PioneerSettlement, 13) { doTask("Colony<Pluto>") }
    }

    green.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
      cardAction2(AerialMappers)
    }

    yellow.turn { cardAction1(JovianLanterns) }

    green.turn {
      playProject(ImmigrantCity, 7, steel = 2) { placeTile(5, 6) }
    }

    yellow.turn { cardAction2(NitriteReducingBacteria) }

    green.turn {
      stdProject("CitySP") { placeTile(2, 3) }

      playProject(CommercialDistrict, 16) { placeTile(3, 3) }
    }

    green.exMachina("PROD[-M]")

    yellow.turn { cardAction1(Psychrophiles) }

    green.turn {
      playProject(RobotPollinators, 9)

      convertPlants { placeTile(2, 4) }
    }

    yellow.turn { cardAction1(StratosphericBirds) }

    green.turn {
      intentionalOverpay(2)
      playProject(MethaneFromTitan, titanium = 6)
    }

    yellow.turn { cardAction1(ExtractorBalloons) }

    green.turn { cardAction1(MartianZoo) }

    yellow.turn {
      playProject(LocalHeatTrapping, 1) { addCardResources(VenusianAnimals) }
    }

    green.turn {
      playProject(TradingColony, titanium = 4) {
        doTask("Colony<Enceladus>")
        addCardResources(VenusianInsects)
      }
    }

    yellow.turn {
      playProject(Airliners, 11) { addCardResources(JovianLanterns) }
    }

    green.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        doTask("ColonyProduction<Enceladus>")
        doTask("3 Microbe<$VenusianInsects>")
        doTask("Microbe<$VenusianInsects>")
      }
    }

    yellow.turn {
      playProject(KelpFarming, 3) {
        doTask("7 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
      }
    }

    green.turn {
      playProject(LandClaim, 1) { doTask("LandClaimMarker<Utopia_1_1>") }
    }

    yellow.turn { sellPatents(1) }

    green.turn { sellPatents(5) }

    yellow.turn { sellPatents(1) }

    green.turn { playProject(LightningHarvest, 8) }

    yellow.turn {
      sellPatents(2)

      playProject(MediaArchives, 8)
    }

    green.turn {
      cardAction1(BusinessNetwork) {
        green.buyCards(0)
      }
    }

    yellow.turn { playProject(WaterImportFromEuropa, 25) }

    green.exMachina("ProjectCard")
    green.turn {
      cardAction2(EnergyMarket)

      playProject(SubZeroSaltFish, 5) { doTask("PROD[-Plant<Yellow>]") }
    }

    green.exMachina("PROD[E]")

    yellow.turn {
      playProject(Predators, 14)
      cardAction1(Predators) { doTask("-Animal<Green, $EcologicalZone<Green>>") }
    }

    green.turn { cardAction1(SubZeroSaltFish) }

    yellow.turn {
      playProject(ArtificialLake, 3, steel = 6)
    }
    green.pass()
    yellow.pass()

    green.exMachina("4 MC, 2 TR, PROD[M, -E], -E, Animal<$Pets>")

    green.convertPlants { placeTile(1, 2) }
    green.convertPlants { placeTile(1, 3) }

    green.declineTask()

    yellow.declineTask()
  }
}
