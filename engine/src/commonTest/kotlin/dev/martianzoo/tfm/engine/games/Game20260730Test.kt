package dev.martianzoo.tfm.engine.games

import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete archive replay: Thermal Matter Wave (gafda6ee74f34)
// https://terraforming-mars.herokuapp.com/the-end?id=pccc28386ce4b
class Game20260730Test : AbstractSoloTest() {
  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  override val config =
      GameConfig(
          """
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, PromoCardPack, Tr63SoloVariant
          Ceres, Io, Triton
          """,
          "Player1",
      )

  override fun cityAreas(): Pair<String, String> = "Tharsis_4_6" to "Tharsis_6_6"

  override fun greeneryAreas(): Pair<String, String> = "Tharsis_3_6" to "Tharsis_6_5"

  @Test
  fun game20260730() {
    with(me) {
      playCorp(CrediCor) {
        buyCards(
            CryoSleep,
            SolarReflectors,
            TerraformingGanymede,
            AdvancedAlloys,
            TitanShuttles,
            IndustrialMicrobes,
            SaturnSurfing,
        )
      }

      playPrelude(SocietySupport).expect("PROD[-1, Plant, Energy, Heat]")
      playPrelude(ExcentricSponsor) {
        // CrediCor still pays its rebate when Excentric Sponsor pays the project cost.
        playProject(TitanShuttles, 0).expect("4")
      }

      cardAction1(TitanShuttles) {
        doTask("2 Floater<$TitanShuttles>")
      }
      playProject(IndustrialMicrobes, 12)
      playProject(SolarReflectors, 23).expect("PROD[5 Heat], -19")

      pass()
      doTask("VenusStep! BY Engine")
      buyCards(CorroderSuits, TowingAComet, StripMine)

      cardAction1(TitanShuttles) {
        doTask("2 Floater<$TitanShuttles>")
      }

      pass()
      doTask("VenusStep! BY Engine")
      buyCards(HousePrinting, CorporateStronghold)

      convertHeat()
      cardAction1(TitanShuttles) {
        doTask("2 Floater<$TitanShuttles>")
      }
      playProject(AdvancedAlloys, 9)
      playProject(HousePrinting, 4, steel = 2)

      pass()
      doTask("VenusStep! BY Engine")
      buyCards(DevelopmentCenter)

      convertHeat()
      playProject(CryoSleep, 10)
      stdAction("TradeSA", 2) {
            doTask("Trade<Ceres>")
          }
          .expect("-2 Energy, 6 Steel")
      playProject(StripMine, 1, steel = 8).expect("3")
      cardAction1(TitanShuttles) {
        doTask("2 Floater<$TitanShuttles>")
      }

      pass()
      doTask("OceanTile<Tharsis_6_7>! BY Engine")
      buyCards(SterlingVents, DesignedMicroorganisms, ElectroCatapult)

      // This temperature step also raises heat production.
      convertHeat().expect("PROD[Heat]")
      cardAction2(TitanShuttles) {
        doTask("-8 Floater<$TitanShuttles> THEN 8 Titanium")
      }
      stdAction("TradeSA", 3) { doTask("Trade<Triton>") }
      playProject(SterlingVents, 2, steel = 1).expect("PROD[2 Energy, -2 Heat]")
      playProject(ElectroCatapult, 8, steel = 3)
      cardAction1(ElectroCatapult)
      playProject(CorroderSuits, 8)

      pass()
      doTask("OceanTile<Tharsis_6_9>! BY Engine")
      buyCards(RotatorImpacts)

      convertHeat()
      cardAction1(ElectroCatapult) { doTask("-Plant") }
      intentionalOverpay()
      playProject(TowingAComet, titanium = 6) {
        doTask("OceanTile<Tharsis_6_8>")
      }
      playProject(SaturnSurfing, 13)
      cardAction1(TitanShuttles) { doTask("2 Floater<$SaturnSurfing>") }
      cardAction1(SaturnSurfing).expect("-Floater, 3")
      playProject(DesignedMicroorganisms, 16)
      playProject(RotatorImpacts, 2, titanium = 1)
      cardAction1(RotatorImpacts) { pay(2, titanium = 1) }

      pass()
      doTask("OceanTile<Tharsis_4_8>! BY Engine")
      buyCards(Tardigrades)

      cardAction1(ElectroCatapult) { doTask("-Plant") }
      playProject(DevelopmentCenter, 2, steel = 3)
      cardAction1(DevelopmentCenter) { draw(DeimosDownPromo) }
      playProject(CorporateStronghold, 2, steel = 3) {
            doTask("CityTile<Tharsis_5_8>")
          }
          .expect("PROD[3, -Energy]")
      convertPlants {
        doTask("GreeneryTile<Tharsis_5_9>")
      }
      cardAction1(TitanShuttles) { doTask("2 Floater<$SaturnSurfing>") }
      cardAction1(SaturnSurfing)
      cardAction2(RotatorImpacts) { draw(SpinOffDepartment) }
      stdAction("TradeSA", 3) { doTask("Trade<Io>") }.expect("-2 Titanium, 13 Heat")
      convertHeat()
      convertHeat()
      playProject(SpinOffDepartment, 4, steel = 2)
      playProject(Tardigrades, 4)
      cardAction1(Tardigrades)
      // Payment reconstruction: retain one titanium for Interplanetary Trade; that allocation is
      // required to reproduce the later archived balance without a state adjustment.
      intentionalUnderpay()
      playProject(DeimosDownPromo, 23, titanium = 2) {
            draw(DawnCity)
            doTask("Ok")
            doTask("DeimosDownPromo_SpecialTile<Tharsis_7_8>")
          }
          .expect("4 Steel, Plant")
      stdProject("PowerPlantSP")

      pass()
      doTask("VenusStep! BY Engine")
      buyCards(PowerSupplyConsortium, BribedCommittee)

      cardAction1(DevelopmentCenter) { draw(ReleaseOfInertGases) }
      convertPlants {
        doTask("GreeneryTile<Tharsis_4_7>")
      }
      convertHeat()
      cardAction1(ElectroCatapult) { doTask("-Plant") }
      cardAction1(TitanShuttles) { doTask("2 Floater<$SaturnSurfing>") }
      cardAction1(SaturnSurfing).expect("-Floater, 5")
      cardAction1(RotatorImpacts) { pay(2, titanium = 1) }
      cardAction1(Tardigrades)
      playProject(BribedCommittee, 7)
      playProject(ReleaseOfInertGases, 14)

      pass()
      doTask("OceanTile<Tharsis_5_5>! BY Engine")
      buyCards(ForcedPrecipitation, VenusSoils)

      cardAction1(DevelopmentCenter) { draw(InterplanetaryTrade) }
      convertHeat()
      cardAction1(ElectroCatapult) { doTask("-Steel") }
      cardAction2(RotatorImpacts)
      cardAction1(TitanShuttles) { doTask("2 Floater<$SaturnSurfing>") }
      cardAction1(SaturnSurfing)
      cardAction1(Tardigrades)
      playProject(VenusSoils, 20) {
        draw(ImportedNutrients)
        doTask("2 Microbe<$Tardigrades>")
      }
      // Payment reconstruction: using the retained titanium here avoids the earlier two-unit
      // overpayment and is required by the later archived balance.
      playProject(InterplanetaryTrade, 19, titanium = 2) { draw(IoMiningIndustries) }
          .expect("PROD[10]")
      playProject(ForcedPrecipitation, 8)
      cardAction1(ForcedPrecipitation)
      stdProject("AirScrappingSP")

      pass()
      doTask("OceanTile<Tharsis_1_5>! BY Engine")
      buyCards(Penguins, MarsUniversity, MedicalLab, Gyropolis)

      cardAction1(DevelopmentCenter) { draw(OutdoorSports) }
      playProject(MarsUniversity, 2, steel = 2) {
        discard(OutdoorSports)
        draw(Comet)
        doTask("-ProjectCard")
      }
      stdAction("TradeSA", 1) { doTask("Trade<Triton>") }
      playProject(Comet, 1, titanium = 5) {
        draw(SolarPower, Predators, EquatorialMagnetizer)
        doTask("Ok")
        doTask("OceanTile<Tharsis_2_6>")
      }
      cardAction1(SaturnSurfing)
      cardAction1(TitanShuttles) { doTask("2 Floater<$TitanShuttles>") }
      cardAction1(ElectroCatapult) { doTask("-Steel") }
      playProject(IoMiningIndustries, 41) { draw(OptimalAerobraking) }.expect("PROD[2, 2 Titanium]")
      playProject(SolarPower, 2, steel = 3)
      playProject(Gyropolis, 2, steel = 6) {
            draw(SpaceHotels)
            doTask("CityTile<Tharsis_3_7>")
          }
          .expect("PROD[4, -2 Energy]")
      // Payment reconstruction: the steel retained on Solar Power is worth its full value here.
      playProject(MedicalLab, 1, steel = 4) {
        discard(Predators)
        draw(AsteroidRights)
        doTask("-ProjectCard")
      }
      playProject(AsteroidRights, 10)
      cardAction2(AsteroidRights) { doTask("2 Titanium") }
      cardAction1(RotatorImpacts) { pay(2, titanium = 1) }
      cardAction1(Tardigrades)
      playProject(PowerSupplyConsortium, 5)
      cardAction1(ForcedPrecipitation)

      pass()
      doTask("OxygenStep! BY Engine")
      buyCards(Thermophiles, TitanFloatingLaunchPad, MagneticShield, SixteenPsyche)

      cardAction1(DevelopmentCenter) { draw(SearchForLife) }
      playProject(SearchForLife, 3) {
        discard(SpaceHotels)
        draw(AirScrappingExpedition)
        doTask("-ProjectCard")
      }
      cardAction2(AsteroidRights) { doTask("2 Titanium") }
      cardAction1(SaturnSurfing)
      // Payment reconstruction: retain one titanium for Magnetic Shield, where it avoids the
      // Rotator Impacts overpayment and receives full value.
      intentionalUnderpay()
      playProject(SixteenPsyche, 11, titanium = 5) { draw(Trees) }.expect("PROD[2 Titanium]")
      cardAction1(SearchForLife) { doTask("Science<$SearchForLife>") }
      cardAction1(Tardigrades)
      cardAction2(ForcedPrecipitation)
      cardAction2(RotatorImpacts)
      convertHeat()
      cardAction1(TitanShuttles) { doTask("2 Floater<$TitanShuttles>") }
      // Payment reconstruction: the titanium retained on Rotator Impacts is worth its full value
      // here, avoiding the action's two-unit overpayment.
      playProject(MagneticShield, 8, titanium = 4) { draw(BeamFromAThoriumAsteroid) }
      playProject(BeamFromAThoriumAsteroid, 32) { draw(Research) }.expect("PROD[3 Energy, 3 Heat]")
      cardAction1(ElectroCatapult) { doTask("-Steel") }
      playProject(Thermophiles, 9)
      convertPlants {
        doTask("GreeneryTile<Tharsis_8_9>")
      }
      playProject(OptimalAerobraking, 3, titanium = 1)
      // Test inference: the archive gives only the number sold; Penguins is the unplayed card
      // available at this point that is not needed later.
      sellPatents(Penguins)
      playProject(ImportedNutrients, 14) {
            doTask("4 Microbe<$Thermophiles>")
          }
          .expect("3 Heat")
      cardAction2(Thermophiles)
      playProject(EquatorialMagnetizer, 2, steel = 3)
      cardAction1(EquatorialMagnetizer)

      pass()
      doTask("TemperatureStep! BY Engine")
      buyCards(FloaterPrototypes, TransNeptuneProbe, ConvoyFromEuropa)

      cardAction1(DevelopmentCenter) { draw(PioneerSettlement) }
      playProject(Research, 11) {
        draw(Shuttles, Atmoscoop)
        doTask("-ProjectCard")
        doTask("-ProjectCard")
        discard(Trees, TransNeptuneProbe)
        draw(GanymedeColony, HiTechLab)
      }
      convertHeat() { doTask("OceanTile<Tharsis_5_6>") }
      convertPlants {
        doTask("GreeneryTile<Tharsis_5_7>")
      }
      playProject(Shuttles, 2, titanium = 2).expect("PROD[2, -Energy]")
      playProject(PioneerSettlement, 3, titanium = 2) { doTask("Colony<Triton>") }
      stdAction("TradeSA", 2) { doTask("Trade<Io>") }
      convertHeat()
      convertHeat()
      playProject(TitanFloatingLaunchPad, 18) {
        doTask("2 Floater<$TitanShuttles>")
      }
      cardAction1(TitanFloatingLaunchPad) { doTask("Floater<$TitanShuttles>") }
      cardAction1(SearchForLife) { doTask("Ok") }
      // Payment reconstruction: retain the titanium from Pioneer Settlement for Terraforming
      // Ganymede, where it receives full value.
      intentionalUnderpay()
      playProject(Atmoscoop, 8, titanium = 3) {
        draw(MediaArchives)
        doTask("2 VenusStep")
        doTask("2 Floater<$TitanShuttles>")
      }
      playProject(FloaterPrototypes, 2) {
        discard(AirScrappingExpedition)
        draw(AsteroidCard)
        doTask("-ProjectCard")
        doTask("2 Floater<$TitanShuttles>")
      }
      cardAction2(TitanShuttles) {
        doTask("-11 Floater<$TitanShuttles> THEN 11 Titanium")
      }
      // Payment reconstruction: retain one titanium for Terraforming Ganymede rather than
      // overpaying for Ganymede Colony.
      intentionalUnderpay()
      playProject(GanymedeColony, 2, titanium = 4) { draw(Ants) }
      intentionalOverpay()
      playProject(ConvoyFromEuropa, titanium = 4) {
        draw(DustSeals)
        doTask("OceanTile<Tharsis_9_9>")
      }
      // Payment reconstruction: the titanium retained on Pioneer Settlement and Ganymede Colony
      // is worth its full value here, avoiding three units of combined overpayment.
      playProject(TerraformingGanymede, 7, titanium = 6) { draw(ProjectInspection) }
      playProject(HiTechLab, 5, steel = 4) {
        discard(DustSeals)
        draw(Windmills)
        doTask("-ProjectCard")
      }
      cardAction1(ElectroCatapult)
      playProject(AsteroidCard, 12) { doTask("Ok") }
      cardAction1(SaturnSurfing)
      cardAction2(Thermophiles)
      cardAction1(EquatorialMagnetizer)
      playProject(ProjectInspection, 0) {
        doTask("UseAction1<$ElectroCatapult>")
      }
      playProject(MediaArchives, 8)
      stdProject("CitySP") { doTask("CityTile<Tharsis_4_4>") }
      stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_4_5>") }
      convertPlants {
        doTask("GreeneryTile<Tharsis_3_4>")
      }
      playProject(DawnCity, 5, titanium = 2).expect("PROD[Titanium, -Energy]")
      cardAction1(AsteroidRights) { doTask("Asteroid<$RotatorImpacts>") }
      cardAction2(RotatorImpacts)
      cardAction1(Tardigrades)
      playProject(Windmills, 6)
      // Test inference: the archive gives only the number sold; Ants is the remaining unplayed
      // project card.
      sellPatents(Ants)

      pass()
      has("Victory") shouldBe true
      doTask("Ok")

      assertCardTrackingComplete()
      cardsInHand shouldBe emptySet()

      assertProduction(m = 27, s = 4, t = 6, p = 4, e = 1, h = 9)
      assertResources(m = 106, s = 4, t = 6, p = 4, e = 1, h = 17)
      assertCounts(0 to "ProjectCard", 75 to "TerraformRating", 58 to "CardFront OR PlayedEvent")
      assertDashRight(events = 10, tagless = 3, cities = 5, colonies = 1)
      assertSidebar(gen = 12, temp = 8, oxygen = 10, oceans = 9, venus = 30)
      assertTags(15, 16, 10, 5, eat = 3, jot = 8, vet = 4, plt = 1, mit = 4, ant = 0, cit = 4)

      val sum = Summarizer(game)

      // Best current match for the app's reported action count: turns offered plus passes,
      // excluding the final-greenery offer.
      (-sum.net("NewTurn", "NewTurn<Player1>") + sum.net("ActionPhase", "Pass<Player1>") -
          1) shouldBe 168

      // Discounts earned
      sum.net("$AdvancedAlloys", "Owed<Player1>") shouldBe -92
      sum.net("$Shuttles", "Owed<Player1>") shouldBe -14

      // Resources and cards gained from active cards
      sum.net("$CryoSleep", "Energy") shouldBe 2
      sum.net("$CryoSleep", "Titanium") shouldBe 2
      sum.net("$DevelopmentCenter", "ProjectCard") shouldBe 6
      sum.net("$ElectroCatapult", "Plant") shouldBe -6
      sum.net("$ElectroCatapult", "Steel") shouldBe -3
      sum.net("$OptimalAerobraking", "Heat") shouldBe 9
      sum.net("$SpinOffDepartment", "ProjectCard") shouldBe 12
      sum.net("$TitanShuttles", "Titanium") shouldBe 19
      sum.net("$TitanShuttles", "Floater<$SaturnSurfing>") shouldBe 8

      // Terraforming gains
      sum.net("$EquatorialMagnetizer", "TerraformRating") shouldBe 2
      sum.net("$ForcedPrecipitation", "VenusStep") shouldBe 1
      sum.net("$RotatorImpacts", "VenusStep") shouldBe 4
      sum.net("$Thermophiles", "VenusStep") shouldBe 2

      // Puntos
      sum.net("GreeneryTile", "VictoryPoint") shouldBe 6
      sum.net("CityTile", "VictoryPoint") shouldBe 7
      sum.net("Card", "VictoryPoint") shouldBe 43
      assertCounts(131 to "VictoryPoint")
    }
  }
}
