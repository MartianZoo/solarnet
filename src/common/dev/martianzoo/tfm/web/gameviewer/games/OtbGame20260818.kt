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
          "Dad",
          "Ellie",
      )

  protected override fun play() {
    TfmWorkflow.Auto(game).launch()
    val dad = game.tfm(Player.PLAYER1)
    val ellie = game.tfm(Player.PLAYER2)

    dad.playCorp(PointLuna, 7)

    ellie.playCorp(ValleyTrust, 5)

    dad.turn {
      playPrelude(Biofuels)

      playPrelude(Donation)
    }

    ellie.turn {
      playPrelude(Supplier)

      playPrelude(MartianIndustries)
    }

    dad.turn {
      playProject(Pets, 10)
    }

    ellie.turn {
      stdAction("DoRequiredActions") {
        playPrelude(DoubleDown) { doTask("CopyPrelude<$MartianIndustries>") }
      }

      playProject(Psychrophiles, 2)
    }

    dad.turn {
      playProject(AerialMappers, 11)
    }

    ellie.turn {
      playProject(ForcedPrecipitation, 8)
    }

    dad.turn {
      cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    }

    ellie.turn {
      playProject(ExtractorBalloons, 21)
    }

    dad.pass()
    ellie.turn {
      cardAction1(Psychrophiles)

      cardAction2(ExtractorBalloons)

      cardAction1(ForcedPrecipitation)
      ellie.pass()
    }

    dad.wgt("OxygenStep")

    ellie.buyCards(4)

    dad.buyCards(3)

    ellie.turn {
      playProject(MiningRights, 1, steel = 4) { placeTile(3, 6) }

      playProject(EnergyTapping, 3) { doTask("PROD[-E<Dad>]") }
    }

    dad.turn {
      playProject(CeosFavoriteProject, 1) { addCardResources(AerialMappers) }
    }

    ellie.turn {
      cardAction1(ExtractorBalloons)
    }

    dad.turn {
      cardAction2(AerialMappers)
    }

    ellie.turn {
      cardAction1(Psychrophiles)
    }

    dad.turn { sellPatents(1) }

    ellie.turn {
      cardAction1(ForcedPrecipitation)
    }

    dad.turn {
      playProject(SixteenPsyche, 28, titanium = 1)
    }

    ellie.pass()
    dad.pass()

    ellie.wgt("VenusStep")

    dad.buyCards(1)

    ellie.buyCards(0)

    dad.turn {
      playProject(ImportedHydrogen, 1, titanium = 5) {
        addCardResources(Pets)
        placeTile(4, 1)
      }
    }

    ellie.turn {
      cardAction2(ForcedPrecipitation)

      cardAction2(ExtractorBalloons)
    }

    dad.turn {
      playProject(Cartel, 8)
    }

    ellie.turn {
      playProject(ColonizerTrainingCamp, steel = 4)
    }

    dad.turn { sellPatents(1) }

    ellie.turn {
      sellPatents(1)

      playProject(BeamFromAThoriumAsteroid, 26, titanium = 2)
    }

    dad.turn {
      playProject(ResearchCoordination, 4)
    }

    ellie.turn {
      cardAction1(Psychrophiles)
    }

    dad.turn {
      assignWildTag(ResearchCoordination, "VenusTag")
      playProject(VenusGovernor, 4)
    }

    ellie.pass()
    dad.turn {
      cardAction2(AerialMappers)
      dad.pass()
    }

    dad.wgt("TemperatureStep")

    ellie.buyCards(2)
    dad.buyCards(2)

    ellie.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }
    }

    dad.turn {
      playProject(MarsUniversity, 8) { doTask("-ProjectCard") }
    }

    ellie.turn {
      playProject(Flooding, 7) { placeTile(3, 1) }

      playProject(Potatoes, 0) {
        doTask("PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
      }
    }

    dad.turn {
      assignWildTag(ResearchCoordination, "ScienceTag")
      playProject(MercurianAlloys, 3)
    }

    ellie.turn {
      cardAction1(Psychrophiles)
    }

    dad.turn {
      playProject(AsteroidRights, 2, titanium = 2)
    }

    ellie.turn {
      playProject(Mine, steel = 2)
    }

    dad.turn {
      cardAction2(AsteroidRights) { doTask("2 T") }
    }

    ellie.turn {
      cardAction1(ForcedPrecipitation)
      cardAction1(ExtractorBalloons)
    }

    dad.turn {
      assignWildTag(ResearchCoordination, "ScienceTag")
      playProject(FloatingHabs, 5)
    }

    ellie.turn {
      sellPatents(1)

      playProject(NitriteReducingBacteria, 11)
    }

    dad.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    ellie.turn {
      cardAction2(NitriteReducingBacteria)
    }

    dad.turn { cardAction2(AerialMappers) }

    ellie.pass()
    dad.pass()

    ellie.wgt("VenusStep")

    dad.buyCards(1)
    ellie.buyCards(1)

    dad.turn {
      playProject(EnergyMarket, 3)
    }

    ellie.turn {
      playProject(HydrogenToVenus, 5, titanium = 2) { addCardResources(ForcedPrecipitation) }

      playProject(HermeticOrderOfMars, 10)
    }

    dad.turn {
      cardAction1(EnergyMarket, x = 3)
      stdAction("TradeAction", 2) { doTask("Trade<Io>") }
    }

    ellie.turn {
      playProject(StratosphericBirds, 12) { doTask("-Floater<$ForcedPrecipitation>") }

      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(StratosphericBirds)
      }
    }

    dad.turn {
      playProject(BigAsteroid, titanium = 7) { doTask("-Plant<Ellie>") }
    }

    ellie.turn {
      convertHeat()
      convertHeat()
    }

    dad.turn {
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(LunarMining, 11)
    }

    ellie.turn { cardAction1(StratosphericBirds) }

    dad.turn { cardAction2(AsteroidRights) { doTask("2 T") } }

    ellie.turn {
      cardAction1(ForcedPrecipitation)
      cardAction1(ExtractorBalloons)
    }

    dad.turn {
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(LunaMetropolis, 1, titanium = 5)
    }

    ellie.turn {
      cardAction1(Psychrophiles)
      cardAction1(NitriteReducingBacteria)
    }

    dad.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
      cardAction2(AerialMappers)
    }

    ellie.pass()
    dad.turn {
      convertHeat()
      pass()
    }

    dad.wgt("OceanTile<Utopia_9_8>")

    dad.buyCards(2)
    ellie.buyCards(1)

    ellie.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(NitriteReducingBacteria)
      }
    }

    dad.turn {
      playProject(IndustrialMicrobes, 12)
      assignWildTag(ResearchCoordination, "MicrobeTag")
      stdAction("ClaimMilestone") { doTask("Ecologist") }
    }

    dad.exMachina("PROD[-S, -E]")

    ellie.turn {
      cardAction2(ForcedPrecipitation)
      cardAction2(ExtractorBalloons)
    }
    dad.turn {
      playProject(ImportOfAdvancedGhg, 1, titanium = 2)

      stdAction("ClaimMilestone") { doTask("Metallurgist") }
    }
    ellie.turn { stdAction("ClaimMilestone") { doTask("Tactician") } }
    dad.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
      cardAction2(AerialMappers)
    }
    ellie.turn { cardAction1(Psychrophiles) }
    dad.turn {
      sellPatents(1)

      playProject(HiredRaiders, 1) { doTask("3 M<Dad> FROM M<Ellie>") }
    }
    ellie.turn {
      cardAction2(NitriteReducingBacteria)
    }
    dad.turn {
      cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }
    }
    ellie.turn {
      convertHeat()
    }
    dad.turn {
      cardAction1(EnergyMarket, x = 1)

      convertPlants { placeTile(4, 2) }
    }

    dad.exMachina("TR")

    ellie.turn {
      playProject(NoctisCity, 6, steel = 6) { placeTile(3, 2) }
    }
    dad.pass()
    ellie.turn {
      cardAction1(StratosphericBirds)
    }
    ellie.pass()

    ellie.wgt("OceanTile<Utopia_6_4>")

    dad.exMachina("-TR, -1 MC, PROD[S, E], S, E")

    dad.buyCards(2)
    dad.exMachina("6 MC")
    ellie.buyCards(3)

    dad.turn {
      stdAction("FundAward") { doTask("Traveller") }
    }

    ellie.turn {
      playProject(MarketManipulation, 1) {
        doTask("ColonyProduction<Pluto> FROM ColonyProduction<Io>")
      }

      stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }
    }

    dad.turn {
      playProject(MartianZoo, 12)
    }

    ellie.turn {
      playProject(IoSulphurResearch, 15) { doTask("3 ProjectCard") }

      ellie.exMachina("-2 MC")
    }

    dad.turn {
      playProject(NuclearPower, 10)
    }

    ellie.turn {
      playProject(AirScrappingExpedition, 13) { addCardResources(ForcedPrecipitation) }
    }

    dad.turn {
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(MirandaResort, titanium = 3)
    }

    ellie.turn {
      cardAction2(ForcedPrecipitation)
    }

    dad.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    ellie.turn { cardAction1(ExtractorBalloons) }

    dad.turn { cardAction2(AerialMappers) }

    ellie.turn { cardAction1(StratosphericBirds) }

    dad.turn {
      playProject(BusinessContacts, 7)
    }

    ellie.turn { cardAction1(Psychrophiles) }

    dad.turn {
      playProject(ImportedNitrogen, titanium = 6) { addCardResources(MartianZoo) }
    }

    dad.exMachina("-Animal<$MartianZoo>")

    ellie.turn {
      cardAction2(NitriteReducingBacteria)
    }

    ellie.exMachina("-TR")

    dad.turn {
      cardAction2(AsteroidRights) { doTask("PROD[1 MC]") }
    }

    ellie.turn { convertHeat() }

    dad.turn {
      cardAction1(MartianZoo)
    }

    ellie.turn {
      playProject(NeutralizerFactory, 7)
    }

    dad.turn {
      playProject(VenusianInsects, 5)
    }

    ellie.pass()
    dad.turn {
      cardAction1(VenusianInsects)

      cardAction2(EnergyMarket)

      playProject(NitrophilicMoss, 8)
      pass()
    }

    dad.wgt("VenusStep")

    dad.buyCards(2)
    ellie.buyCards(1)

    dad.exMachina("-6 MC, -Microbe<$VenusianInsects>")

    ellie.turn {
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

    dad.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    ellie.turn {
      playProject(DiversitySupport, 1)
    }

    dad.turn {
      assignWildTag(ResearchCoordination, "ScienceTag")
      playProject(Plantation, 15) { placeTile(5, 3) }

      playProject(KaguyaTech, 10) { doTask("CityTile<Utopia_5_3> FROM GreeneryTile<Utopia_5_3>") }
    }

    dad.exMachina("-Animal<$Pets>")

    ellie.turn {
      playProject(VenusianAnimals, 13)
    }

    ellie.exMachina("-2 MC")

    dad.turn {
      playProject(NitrogenRichAsteroid, 3, titanium = 7) { doTask("PROD[4 Plant]") }
    }

    ellie.turn { cardAction2(ForcedPrecipitation) }

    dad.turn { cardAction2(AerialMappers) }

    ellie.turn { cardAction1(ExtractorBalloons) }

    dad.turn {
      playProject(BusinessNetwork, 4)
    }

    ellie.turn { cardAction1(Psychrophiles) }

    dad.turn { cardAction2(EnergyMarket) }

    ellie.turn { convertHeat() }

    dad.turn {
      playProject(HeatTrappers, 2, steel = 2) { doTask("PROD[-2 H<Ellie>]") }
    }

    ellie.turn {
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-E<Dad>]") }
    }

    dad.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Europa>") }
    }

    ellie.turn { cardAction1(StratosphericBirds) }

    dad.turn {
      playProject(ImportedGhg, 7)
    }

    ellie.turn {
      playProject(ImportedNutrients, 11, titanium = 1) { addCardResources(NitriteReducingBacteria) }
    }

    dad.turn { cardAction1(VenusianInsects) }

    ellie.turn { cardAction2(NitriteReducingBacteria) }

    dad.turn {
      cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }
    }

    ellie.pass()
    dad.turn {
      cardAction1(MartianZoo)

      cardAction1(BusinessNetwork) { dad.buyCards(1) }
      pass()
    }

    ellie.wgt("OxygenStep")

    dad.exMachina("3 MC, Animal<$MartianZoo>, -ProjectCard")
    ellie.exMachina("6 MC, TR")

    dad.buyCards(4)
    ellie.buyCards(4)

    dad.turn {
      convertPlants { placeTile(6, 3) }

      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    ellie.turn {
      cardAction2(ExtractorBalloons)

      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(NitriteReducingBacteria)
      }
    }

    dad.turn { cardAction2(AerialMappers) }

    ellie.turn {
      playProject(SponsoredAcademies, 7)

      playProject(LagrangeObservatory, 4, titanium = 1)
    }

    dad.turn {
      cardAction1(BusinessNetwork) {
        dad.buyCards(0)
      }
    }

    ellie.turn {
      playProject(AquiferPumping, steel = 9)
    }

    dad.turn {
      playProject(AdvancedAlloys, 9) { doTask("-ProjectCard") }

      playProject(SolarLogistics, titanium = 4)
    }

    ellie.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(7, 6)
      }
    }

    dad.turn {
      playProject(IceAsteroid, 3, titanium = 4) {
        doTask("OceanTile<Utopia_7_5>")
        doTask("OceanTile<Utopia_8_6>")
      }

      stdProject("AquiferSP") { placeTile(4, 5) }
    }

    ellie.turn {
      playProject(Conscription, 5)
      playProject(Capital, 10) { placeTile(6, 5) }
    }

    dad.turn {
      playProject(TectonicStressPower, 9, steel = 3)
    }

    ellie.turn {
      convertHeat()

      cardAction2(NitriteReducingBacteria)
    }

    dad.turn {
      cardAction2(AsteroidRights) { doTask("2 T") }

      convertPlants { placeTile(5, 2) }
    }

    ellie.turn {
      stdProject("AirScrappingSP")

      cardAction1(Psychrophiles)
    }

    dad.turn {
      convertHeat()
      convertHeat()
    }

    ellie.turn { cardAction1(StratosphericBirds) }

    dad.turn { cardAction2(EnergyMarket) }

    ellie.pass()
    dad.turn {
      playProject(LunarExports, 2, titanium = 3) { doTask("PROD[5 MC]") }

      playProject(Solarnet, 7)

      playProject(Algae, 10)

      cardAction1(MartianZoo)

      cardAction1(VenusianInsects)
      assignWildTag(ResearchCoordination, "PlantTag")

      playProject(Insects, 9)
      pass()
    }

    dad.wgt("TemperatureStep")

    dad.buyCards(3)
    ellie.buyCards(3)

    ellie.turn {
      convertPlants { placeTile(5, 5) }

      stdProject("GreenerySP") { placeTile(2, 1) }
    }

    dad.turn {
      stdProject("CitySP") { placeTile(4, 4) }

      playProject(EcologicalZone, 12) { placeTile(2, 2) }
    }

    ellie.turn {
      playProject(CryoSleep, 8)

      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(StratosphericBirds)
      }
    }

    dad.turn {
      cardAction1(BusinessNetwork) {
        dad.buyCards(0)
      }

      convertHeat()
    }

    ellie.turn {
      playProject(BactoviralResearch, 8) { addCardResources(NitriteReducingBacteria) }
    }

    dad.turn {
      playProject(Herbivores, 12)
    }

    ellie.turn {
      playProject(JovianLanterns, 20) { addCardResources(JovianLanterns) }
    }

    dad.turn {
      convertPlants { placeTile(4, 3) }
      convertPlants { placeTile(5, 4) }
    }

    ellie.turn {
      cardAction1(JovianLanterns)

      cardAction1(ExtractorBalloons)
    }

    dad.turn {
      cardAction1(MartianZoo)
      playProject(LavaFlows, 18) { placeTile(8, 5) }
    }

    dad.exMachina("-2 TR")

    ellie.turn {
      cardAction1(StratosphericBirds)

      convertHeat()
    }

    dad.turn {
      playProject(FoodFactory, 3, steel = 3)
    }

    ellie.turn {
      cardAction1(Psychrophiles)
      cardAction2(NitriteReducingBacteria)
    }

    dad.turn {
      sellPatents(1)
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
    }

    ellie.turn {
      playProject(Greenhouses, 0) {
        doTask("3 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
      }

      convertPlants { placeTile(6, 6) }
    }

    dad.turn { cardAction2(AerialMappers) }

    ellie.pass()
    dad.turn {
      cardAction2(EnergyMarket)

      sellPatents(1)
      playProject(RoboticWorkforce, 9) {
        doTask("-ProjectCard")
        doTask("CopyProductionBox<$IndustrialMicrobes>")
      }

      assignWildTag(ResearchCoordination, "ScienceTag")
      playProject(DawnCity, titanium = 3)

      cardAction1(VenusianInsects)

      sellPatents(1)
      cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }

      stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }
      pass()
    }

    ellie.wgt("OxygenStep")

    dad.buyCards(1)
    ellie.buyCards(2)

    dad.turn {
      convertPlants { placeTile(3, 4) }

      convertPlants { placeTile(7, 4) }
    }

    ellie.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(VenusianAnimals)
      }

      playProject(ProductiveOutpost, 0)
    }

    dad.turn {
      stdAction("FundAward", which = 2) { doTask("Mogul") }

      playProject(Sabotage, 1) { doTask("-7 M<Ellie>") }
    }

    ellie.turn {
      playProject(PioneerSettlement, 13) { doTask("Colony<Pluto>") }
    }

    dad.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }
      cardAction2(AerialMappers)
    }

    ellie.turn { cardAction1(JovianLanterns) }

    dad.turn {
      playProject(ImmigrantCity, 7, steel = 2) { placeTile(5, 6) }
    }

    ellie.turn { cardAction2(NitriteReducingBacteria) }

    dad.turn {
      stdProject("CitySP") { placeTile(2, 3) }

      playProject(CommercialDistrict, 16) { placeTile(3, 3) }
    }

    dad.exMachina("PROD[-M]")

    ellie.turn { cardAction1(Psychrophiles) }

    dad.turn {
      playProject(RobotPollinators, 9)

      convertPlants { placeTile(2, 4) }
    }

    ellie.turn { cardAction1(StratosphericBirds) }

    dad.turn {
      intentionalOverpay(2)
      playProject(MethaneFromTitan, titanium = 6)
    }

    ellie.turn { cardAction1(ExtractorBalloons) }

    dad.turn { cardAction1(MartianZoo) }

    ellie.turn {
      playProject(LocalHeatTrapping, 1) { addCardResources(VenusianAnimals) }
    }

    dad.turn {
      playProject(TradingColony, titanium = 4) {
        doTask("Colony<Enceladus>")
        addCardResources(VenusianInsects)
      }
    }

    ellie.turn {
      playProject(Airliners, 11) { addCardResources(JovianLanterns) }
    }

    dad.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        doTask("ColonyProduction<Enceladus>")
        doTask("3 Microbe<$VenusianInsects>")
        doTask("Microbe<$VenusianInsects>")
      }
    }

    ellie.turn {
      playProject(KelpFarming, 3) {
        doTask("7 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
      }
    }

    dad.turn {
      playProject(LandClaim, 1) { doTask("LandClaimMarker<Utopia_1_1>") }
    }

    ellie.turn { sellPatents(1) }

    dad.turn { sellPatents(5) }

    ellie.turn { sellPatents(1) }

    dad.turn { playProject(LightningHarvest, 8) }

    ellie.turn {
      sellPatents(2)

      playProject(MediaArchives, 8)
    }

    dad.turn {
      cardAction1(BusinessNetwork) {
        dad.buyCards(0)
      }
    }

    ellie.turn { playProject(WaterImportFromEuropa, 25) }

    dad.exMachina("ProjectCard")
    dad.turn {
      cardAction2(EnergyMarket)

      playProject(SubZeroSaltFish, 5) { doTask("PROD[-Plant<Ellie>]") }
    }

    dad.exMachina("PROD[E]")

    ellie.turn {
      playProject(Predators, 14)
      cardAction1(Predators) { doTask("-Animal<Dad, $EcologicalZone<Dad>>") }
    }

    dad.turn { cardAction1(SubZeroSaltFish) }

    ellie.turn {
      playProject(ArtificialLake, 3, steel = 6)
    }
    dad.pass()
    ellie.pass()

    dad.exMachina("4 MC, 2 TR, PROD[M, -E], -E, Animal<$Pets>")

    dad.convertPlants { placeTile(1, 2) }
    dad.convertPlants { placeTile(1, 3) }

    dad.declineTask()

    ellie.declineTask()
  }
}
