package dev.martianzoo.tfm.engine.games

import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Sources: _local/Game20260817/full-log-pb64886c6e682.txt and
// _local/Game20260817/player-pb64886c6e682.json. The complete Research offers come from the
// earlier clone of the same source game at /the-end?id=pe59a6b631bd6.
class SoloGame0816Test : AbstractSoloTest() {
  override val config =
      GameConfig(
          """
          ElysiumMapOption
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, TurmoilCardPack, PromoCardPack
          Tr63SoloVariant
          Ganymede, Luna, Pluto
          """,
          "Me",
      )

  // Drew and discarded Orbital Reflectors to place a 2
  // Drew and discarded Lake Marineris to place a 0
  // Drew and discarded Topsoil Contract to place a 2
  // Drew and discarded Space Station to place a 0
  override fun cityAreas(): Pair<String, String> = "Elysium_6_4" to "Elysium_8_7"

  override fun greeneryAreas(): Pair<String, String> = "Elysium_7_5" to "Elysium_9_8"

  @Test
  fun soloGame0816() {
    with(me) {
      playCorp(TerraLabsResearch) {
        buyCards(
            RadSuits,
            SoilFactory,
            FueledGenerators,
            NeptunianPowerConsultants,
            AiCentral,
            Research,
            SecurityFleet,
            ImportOfAdvancedGhg,
        )
      }
      playPrelude(BusinessEmpire).expect("PROD[6 M], -6 M")
      playPrelude(MetalsCompany).expect("PROD[M]")

      assertCounts(0 to "M")
      pass()
      doTask("VenusStep! BY Engine")
      buyCards(Solarnet)

      assertCounts(17 to "M")
      pass()
      doTask("VenusStep! BY Engine")
      buyCards(LunarBeam, VestaShipyard)

      playProject(FueledGenerators, 1).expect("PROD[-M, E]")
      playProject(Research, 11) { draw(MediaGroup, EosChasmaNationalPark) }
      playProject(AiCentral, 17, steel = 2)
      cardAction1(AiCentral) { draw(Stratopolis, RotatorImpacts) }
      // The source gives only sale counts; unused-card identities at each sale are test
      // inference.
      sellPatents(
          EosChasmaNationalPark,
          RadSuits,
          SoilFactory,
          NeptunianPowerConsultants,
          SecurityFleet,
      )
      playProject(VestaShipyard, 9, titanium = 2)

      assertCounts(0 to "M")
      pass()
      doTask("VenusStep! BY Engine")
      buyCards(ArtificialPhotosynthesis, Steelworks, SolarWindPower)

      cardAction1(AiCentral) { draw(GeothermalPower, BuildingIndustries) }
      playProject(SolarWindPower, 5, titanium = 2)
      playProject(GeothermalPower, 9, steel = 1)

      assertCounts(0 to "M")
      pass()
      doTask("TemperatureStep! BY Engine")
      buyCards(AcquiredCompany, LavaTubeSettlement, Conscription)

      cardAction1(AiCentral) { draw(ForcedPrecipitation, ProjectInspection) }
      playProject(MediaGroup, 6)
      playProject(ProjectInspection, 0) {
            doTask("UseAction1<$AiCentral>")
            draw(GreatEscarpmentConsortium, RestrictedArea)
          }
          .expect("3 M")
      playProject(RotatorImpacts, titanium = 2)
      cardAction1(RotatorImpacts) { pay(titanium = 2) }
      stdAction("TradeSA", 2) { doTask("Trade<Luna>") }.expect("13 M")
      playProject(RestrictedArea, 11) {
        doTask("RestrictedArea_SpecialTile<Elysium_3_7>")
        draw(CallistoPenalMines, Shuttles, Extremophiles)
      }
      cardAction1(RestrictedArea) { draw(LocalShading) }
      sellPatents(
          Stratopolis,
          BuildingIndustries,
          GreatEscarpmentConsortium,
          CallistoPenalMines,
          LocalShading,
      )
      playProject(Conscription, 5).expect("-2 M")
      playProject(LunarBeam, 0).expect("PROD[-2 M]")
      playProject(ArtificialPhotosynthesis, 12) { doTask("PROD[2 Energy]") }
      sellPatents(Shuttles)
      playProject(Extremophiles, 3)
      cardAction1(Extremophiles) { doTask("Microbe<$Extremophiles>") }

      assertCounts(0 to "M")
      pass()
      doTask("TemperatureStep! BY Engine")
      buyCards(SulphurEatingBacteria, IndenturedWorkers, Harvest, StaticHarvesting)

      cardAction1(RestrictedArea) { draw(SoilEnrichment) }
      cardAction1(AiCentral) { draw(TransNeptuneProbe, ProtectedGrowth) }
      cardAction2(RotatorImpacts) { draw(Worms) }
      stdAction("TradeSA", 2) {
        doTask("Trade<Pluto>")
        draw(IndustrialCenter, BigAsteroid, MethaneFromTitan, WeatherBalloons)
      }
      playProject(IndenturedWorkers, 0).expect("3 M")
      playProject(ForcedPrecipitation, 0)
      cardAction1(ForcedPrecipitation)
      sellPatents(
          TransNeptuneProbe,
          IndustrialCenter,
          MethaneFromTitan,
          WeatherBalloons,
          AcquiredCompany,
      )
      playProject(Steelworks, 11, steel = 2)
      cardAction1(Steelworks)
      playProject(ImportOfAdvancedGhg, 3, titanium = 2).expect("0 M")
      sellPatents(LavaTubeSettlement, Harvest)
      playProject(SulphurEatingBacteria, 6)
      cardAction1(SulphurEatingBacteria)
      cardAction1(Extremophiles) { doTask("Microbe<$SulphurEatingBacteria>") }

      assertCounts(0 to "M")
      pass()
      doTask("OceanTile<Elysium_3_6>! BY Engine")
      buyCards(LunaGovernor, JetStreamMicroscrappers, InvestmentLoan, TundraFarming)

      cardAction1(AiCentral) { draw(UrbanizedArea, MedicalLab) }
      cardAction1(RestrictedArea) { draw(BlackPolarDust) }
      cardAction1(Steelworks)
      stdAction("TradeSA", 2) { doTask("Trade<Ganymede>") }
      cardAction1(Extremophiles) { doTask("Microbe<$SulphurEatingBacteria>") }
      cardAction1(ForcedPrecipitation)
      playProject(InvestmentLoan, 3).expect("PROD[-M], 10 M")
      playProject(Solarnet, 7) { draw(AirScrappingExpedition, VenusGovernor) }
      cardAction2(SulphurEatingBacteria) {
            doTask("-3 Microbe<$SulphurEatingBacteria> THEN 9 M")
          }
          .expect("9 M")
      playProject(BigAsteroid, 21, titanium = 2) { doTask("Ok") }.expect("-18 M")
      cardAction1(RotatorImpacts) { pay(titanium = 2) }
      sellPatents(UrbanizedArea, MedicalLab, VenusGovernor)
      playProject(LunaGovernor, 4).expect("PROD[2 M], -4 M")

      assertCounts(2 to "M")
      pass()
      doTask("OceanTile<Elysium_2_4>! BY Engine")
      buyCards(RegolithEaters, Satellites)

      convertHeat()
      cardAction1(AiCentral) { draw(ImportedGhg, OlympusConference) }
      cardAction1(RestrictedArea) { draw(AtalantaPlanitiaLab) }
      cardAction2(ForcedPrecipitation)
      cardAction2(RotatorImpacts)
      cardAction1(Steelworks)
      stdAction("TradeSA", 2) { doTask("Trade<Luna>") }.expect("7 M")
      playProject(OlympusConference, steel = 5)
      playProject(StaticHarvesting, 5).expect("0 M")
      playProject(ProtectedGrowth, 2).expect("1 M")
      convertPlants {
        doTask("GreeneryTile<Elysium_4_8>")
      }
      playProject(AtalantaPlanitiaLab, 10) {
        draw(Trees, MinorityRefuge)
        doTask("ProjectCard FROM Science<$OlympusConference>")
        draw(TowingAComet)
      }
      playProject(ImportedGhg, 1, titanium = 2).expect("2 M")
      cardAction1(SulphurEatingBacteria)
      playProject(RegolithEaters, 13)
      cardAction1(RegolithEaters)
      cardAction1(Extremophiles) { doTask("Microbe<$RegolithEaters>") }
      sellPatents(Trees, MinorityRefuge)
      playProject(Worms, 8)
      sellPatents(JetStreamMicroscrappers, TundraFarming)

      assertCounts(2 to "M")
      pass()
      doTask("TemperatureStep! BY Engine")
      buyCards(MineralDeposit, BusinessNetwork, FuelFactory)

      convertHeat()
      cardAction1(AiCentral) { draw(TradeEnvoys, RegoPlastics) }
      cardAction1(RestrictedArea) { draw(ExtractorBalloons) }
      cardAction1(Steelworks)
      sellPatents(TradeEnvoys, RegoPlastics, Satellites)
      playProject(BusinessNetwork, 4).expect("PROD[-M], -4 M")
      cardAction1(BusinessNetwork) { buyCards(Thermophiles) }
      cardAction1(RotatorImpacts) { pay(titanium = 2) }
      playProject(AirScrappingExpedition, 13) {
            doTask("3 Floater<$ForcedPrecipitation>")
          }
          .expect("-10 M")
      cardAction2(ForcedPrecipitation)
      cardAction2(RegolithEaters)
      cardAction1(Extremophiles) { doTask("Microbe<$RegolithEaters>") }
      cardAction1(SulphurEatingBacteria)
      playProject(SoilEnrichment, 6) {
            doTask("-Microbe<$Extremophiles>")
          }
          .expect("-3 M")
      convertPlants {
        doTask("GreeneryTile<Elysium_5_8>")
      }
      sellPatents(FuelFactory)
      playProject(Thermophiles, 9)
      cardAction1(Thermophiles) { doTask("Microbe<$Thermophiles>") }

      assertCounts(0 to "M")
      pass()
      doTask("TemperatureStep! BY Engine")
      buyCards(
          DirectedHeatUsage,
          CarbonateProcessing,
          PioneerSettlement,
          InterstellarColonyShip,
      )

      convertHeat()
      convertHeat()
      cardAction1(AiCentral) { draw(Windmills, AdaptationTechnology) }
      cardAction1(RestrictedArea) { draw(SpaceMirrors) }
      cardAction1(BusinessNetwork) { buyCards(FusionPower) }
      cardAction2(RotatorImpacts)
      cardAction1(Steelworks)
      cardAction1(Extremophiles) { doTask("Microbe<$RegolithEaters>") }
      cardAction2(RegolithEaters)
      cardAction1(SulphurEatingBacteria)
      cardAction1(ForcedPrecipitation)
      cardAction1(Thermophiles) { doTask("Microbe<$Thermophiles>") }
      playProject(CarbonateProcessing, steel = 3)
      stdAction("TradeSA", 2) {
        doTask("Trade<Pluto>")
        draw(BusinessContacts, DomedCrater, LavaFlows)
      }
      sellPatents(
          AdaptationTechnology,
          SpaceMirrors,
          DomedCrater,
          DirectedHeatUsage,
          PioneerSettlement,
          InterstellarColonyShip,
      )
      playProject(BlackPolarDust, 15) {
            doTask("OceanTile<Elysium_1_3>")
            draw(BribedCommittee)
          }
          .expect("PROD[-2 M], -13 M")
      playProject(BribedCommittee, 7).expect("-4 M")
      playProject(BusinessContacts, 7) { draw(BactoviralResearch, JovianEmbassy) }.expect("-4 M")
      playProject(JovianEmbassy, steel = 7)
      playProject(MineralDeposit, 5).expect("-2 M")
      playProject(Windmills, steel = 3)

      assertCounts(6 to "M")
      pass()
      doTask("VenusStep! BY Engine")
      buyCards(Airliners)

      stdAction("TradeSA", 2) { doTask("Trade<Luna>") }.expect("7 M")
      cardAction1(RestrictedArea) { draw(ReleaseOfInertGases) }
      convertHeat()
      cardAction1(AiCentral) { draw(AtmoCollectors, InventionContest) }
      playProject(InventionContest, 2) {
            doTask("ProjectCard FROM Science<$OlympusConference>")
            draw(CityParks, IoSulphurResearch)
          }
          .expect("1 M")
      playProject(TowingAComet, 5, titanium = 6) { doTask("OceanTile<Elysium_4_6>") }.expect("0 M")
      playProject(LavaFlows, 18) { doTask("LavaFlows_SpecialTile<Elysium_3_1>") }.expect("-15 M")
      stdProject("AsteroidSP") { doTask("OceanTile<Elysium_4_7>") }.expect("-10 M")
      convertPlants {
            doTask("GreeneryTile<Elysium_5_7>")
          }
          .expect("4 M")
      cardAction1(BusinessNetwork) { buyCards(NuclearPower) }
      cardAction2(ForcedPrecipitation)
      cardAction1(RegolithEaters)
      cardAction2(Thermophiles)
      sellPatents(AtmoCollectors, CityParks, NuclearPower)
      cardAction1(RotatorImpacts) { pay(titanium = 2) }
      cardAction1(Extremophiles) { doTask("Microbe<$Thermophiles>") }
      cardAction1(SulphurEatingBacteria)
      playProject(ExtractorBalloons, 21)
      playProject(Airliners, 11) { doTask("2 Floater<$ForcedPrecipitation>") }
          .expect("PROD[2 M], -11 M")
      cardAction2(ExtractorBalloons)

      assertCounts(0 to "M")
      pass()
      doTask("OceanTile<Elysium_4_4>! BY Engine")
      buyCards(InventorsGuild)

      convertHeat()
      convertPlants {
            doTask("GreeneryTile<Elysium_5_6>")
          }
          .expect("2 M")
      convertHeat()
      convertHeat()
      cardAction1(AiCentral) { draw(MartianRails, Supercapacitors) }
      cardAction1(RestrictedArea) { draw(HydrogenToVenus) }
      cardAction1(BusinessNetwork) { buyCards(Algae) }
      cardAction2(ForcedPrecipitation)
      cardAction2(RotatorImpacts)
      cardAction1(ExtractorBalloons)
      cardAction1(Steelworks)
      cardAction1(Extremophiles) { doTask("Microbe<$RegolithEaters>") }
      cardAction2(RegolithEaters)
      cardAction1(Thermophiles) { doTask("Microbe<$SulphurEatingBacteria>") }
      playProject(IoSulphurResearch, 17) {
        doTask("3 ProjectCard")
        draw(AerosportTournament, EcologicalZone, VenusMagnetizer)
      }
      sellPatents(
          MartianRails,
          Supercapacitors,
          HydrogenToVenus,
          AerosportTournament,
          EcologicalZone,
      )
      playProject(FusionPower, 2, steel = 6) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
        draw(Advertising)
      }
      playProject(InventorsGuild, 9)
      cardAction1(InventorsGuild) { buyCards(AirRaid) }
      playProject(AirRaid, 0) {
            // The app omitted Air Raid's 5 M€ transfer from the log.
            doTask("5 Megacredit<Me> FROM Megacredit<SoloOpponent>")
          }
          .expect("8 M")
      playProject(BactoviralResearch, 10) {
        draw(Insulation)
        doTask("ProjectCard FROM Science<$OlympusConference>")
        draw(Mangrove)
        doTask("14 Microbe<$SulphurEatingBacteria>")
      }
      sellPatents(VenusMagnetizer, Advertising, Insulation)
      cardAction2(SulphurEatingBacteria) {
            doTask("-19 Microbe<$SulphurEatingBacteria> THEN 57 M")
          }
          .expect("57 M")
      playProject(ReleaseOfInertGases, 14).expect("-11 M")
      stdProject("AquiferSP") { doTask("OceanTile<Elysium_2_5>") }.expect("-14 M")
      playProject(Mangrove, 12) { doTask("GreeneryTile<Elysium_3_5>") }.expect("-4 M")
      stdProject("CitySP") { doTask("CityTile<Elysium_4_5>") }.expect("-21 M")
      playProject(Algae, 10)
      stdAction("TradeSA", 2) { doTask("Trade<Ganymede>") }
      stdProject("AquiferSP") { doTask("OceanTile<Elysium_5_4>") }.expect("-16 M")
      convertPlants {
            doTask("GreeneryTile<Elysium_5_5>")
          }
          .expect("4 M")
      convertPlants {
            doTask("GreeneryTile<Elysium_3_4>")
          }
          .expect("4 M")
      stdProject("AsteroidSP")
      assertCounts(3 to "M")
      pass()
      doTask("Ok")

      assertCardTrackingComplete()
      cardsInHand shouldBe emptySet()
      // Final state and score come from /api/player?id=pb64886c6e682.
      assertProduction(m = 3, s = 1, t = 2, p = 4, e = 11, h = 13)
      assertResources(m = 70, s = 1, t = 4, p = 4, e = 11, h = 14)
      assertCounts(64 to "TerraformRating", 0 to "ProjectCard", 18 to "PlayedEvent")
      assertSidebar(gen = 12, temp = 8, oxygen = 14, oceans = 8, venus = 30)
      has("Victory") shouldBe true

      val sum = Summarizer(game)
      sum.net("GreeneryTile", "VictoryPoint<Me>") shouldBe 7
      sum.net("CityTile", "VictoryPoint<Me>") shouldBe 4
      sum.net("Card", "VictoryPoint<Me>") shouldBe 9
      assertCounts(84 to "VictoryPoint")
    }
  }
}

/*
 * Implementation note: complete Research deck order from the earlier source-game clone:
 *
 * RadSuits, SoilFactory, FueledGenerators, NeptunianPowerConsultants, AiCentral, Research,
 * SecurityFleet, ImportOfAdvancedGhg, UrbanDecomposers, FreyjaBiodomes, ProductiveOutpost,
 * Solarnet, InterplanetaryTrade, MolecularPrinting, LunarBeam, LawSuit, NoctisFarming,
 * VestaShipyard, SolarWindPower, ArtificialPhotosynthesis, Steelworks, Heather,
 * AcquiredCompany, LavaTubeSettlement, FoodFactory, Conscription, SulphurEatingBacteria,
 * IndenturedWorkers, Harvest, StaticHarvesting, LunaGovernor, JetStreamMicroscrappers,
 * InvestmentLoan, TundraFarming, TradingColony, RegolithEaters, Herbivores, Satellites,
 * MineralDeposit, FuelFactory, BusinessNetwork, Potatoes, DirectedHeatUsage,
 * CarbonateProcessing, PioneerSettlement, InterstellarColonyShip, Archaebacteria,
 * OutdoorSports, MiningRights, Airliners, CrashSiteCleanup, QuantumExtractor,
 * AsteroidHollowing, InventorsGuild
 */
