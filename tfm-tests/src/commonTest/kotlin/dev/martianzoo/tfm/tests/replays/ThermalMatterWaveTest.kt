package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete archive replay: Thermal Matter Wave (gafda6ee74f34)
// https://terraforming-mars.herokuapp.com/the-end?id=pccc28386ce4b
internal class ThermalMatterWaveTest : AbstractSoloTest() {
  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  override val config =
      GameConfig(
          """
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, PromoCardPack, Tr63SoloVariant
          Ceres, Io, Miranda, Triton
          """,
          "Player1",
      )

  override fun cityAreas(): Pair<String, String> = "Tharsis_4_6" to "Tharsis_6_6"

  override fun greeneryAreas(): Pair<String, String> = "Tharsis_3_6" to "Tharsis_6_5"

  @Test
  internal fun game20260730() {
    with(me) {
      doTask("-ColonyTileSelection<Class<Miranda>>")

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

      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
      playProject(IndustrialMicrobes, 12)
      playProject(SolarReflectors, 23).expect("PROD[5 Heat], -19")

      pass()
      wgt("VenusStep")
      buyCards(CorroderSuits, TowingAComet, StripMine)

      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }

      pass()
      wgt("VenusStep")
      buyCards(HousePrinting, CorporateStronghold)

      convertHeat()
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
      playProject(AdvancedAlloys, 9)
      playProject(HousePrinting, 4, steel = 2)

      pass()
      wgt("VenusStep")
      buyCards(DevelopmentCenter)

      convertHeat()
      playProject(CryoSleep, 10)
      stdAction("TradeSA", 2) { doTask("Trade<Ceres>") }.expect("-2 Energy, 6 Steel")
      playProject(StripMine, 1, steel = 8).expect("3")
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }

      pass()
      wgt("OceanTile<Tharsis_6_7>")
      buyCards(SterlingVents, DesignedMicroorganisms, ElectroCatapult)

      // This temperature step also raises heat production.
      convertHeat().expect("PROD[Heat]")
      cardAction2(TitanShuttles) { doTask("-8 Floater<$TitanShuttles> THEN 8 Titanium") }
      stdAction("TradeSA", 3) { doTask("Trade<Triton>") }
      playProject(SterlingVents, 2, steel = 1).expect("PROD[2 Energy, -2 Heat]")
      playProject(ElectroCatapult, 8, steel = 3)
      cardAction1(ElectroCatapult)
      playProject(CorroderSuits, 8)

      pass()
      wgt("OceanTile<Tharsis_6_9>")
      buyCards(RotatorImpacts)

      convertHeat()
      cardAction1(ElectroCatapult)
      intentionalOverpay()
      playProject(TowingAComet, titanium = 6) { placeTile(6, 8) }
      playProject(SaturnSurfing, 13)
      cardAction1(TitanShuttles) { addCardResources(SaturnSurfing) }
      cardAction1(SaturnSurfing).expect("-Floater, 3")
      playProject(DesignedMicroorganisms, 16)
      playProject(RotatorImpacts, 2, titanium = 1)
      cardAction1(RotatorImpacts) { pay(2, titanium = 1) }

      pass()
      wgt("OceanTile<Tharsis_4_8>")
      buyCards(Tardigrades)

      cardAction1(ElectroCatapult)
      playProject(DevelopmentCenter, 2, steel = 3)
      cardAction1(DevelopmentCenter) { draw(DeimosDownPromo) }
      playProject(CorporateStronghold, 2, steel = 3) { placeTile(5, 8) }.expect("PROD[3, -Energy]")
      convertPlants { placeTile(5, 9) }
      cardAction1(TitanShuttles) { addCardResources(SaturnSurfing) }
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
            // Decline removing an opponent's plants.
            declineTask()
            placeTile(7, 8)
          }
          .expect("4 Steel, Plant")
      stdProject("PowerPlantSP")

      pass()
      wgt("VenusStep")
      buyCards(PowerSupplyConsortium, BribedCommittee)

      cardAction1(DevelopmentCenter) { draw(ReleaseOfInertGases) }
      convertPlants { placeTile(4, 7) }
      convertHeat()
      cardAction1(ElectroCatapult)
      cardAction1(TitanShuttles) { addCardResources(SaturnSurfing) }
      cardAction1(SaturnSurfing).expect("-Floater, 5")
      cardAction1(RotatorImpacts) { pay(2, titanium = 1) }
      cardAction1(Tardigrades)
      playProject(BribedCommittee, 7)
      playProject(ReleaseOfInertGases, 14)

      pass()
      wgt("OceanTile<Tharsis_5_5>")
      buyCards(ForcedPrecipitation, VenusSoils)

      cardAction1(DevelopmentCenter) { draw(InterplanetaryTrade) }
      convertHeat()
      cardAction2(ElectroCatapult)
      cardAction2(RotatorImpacts)
      cardAction1(TitanShuttles) { addCardResources(SaturnSurfing) }
      cardAction1(SaturnSurfing)
      cardAction1(Tardigrades)
      playProject(VenusSoils, 20) {
        draw(ImportedNutrients)
        addCardResources(Tardigrades)
      }
      // Payment reconstruction: using the retained titanium here avoids the earlier two-unit
      // overpayment and is required by the later archived balance.
      playProject(InterplanetaryTrade, 19, titanium = 2) { draw(IoMiningIndustries) }
          .expect("PROD[10]")
      playProject(ForcedPrecipitation, 8)
      cardAction1(ForcedPrecipitation)
      stdProject("AirScrappingSP")

      pass()
      wgt("OceanTile<Tharsis_1_5>")
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
        // Decline removing an opponent's plants.
        declineTask()
        placeTile(2, 6)
      }
      cardAction1(SaturnSurfing)
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
      cardAction2(ElectroCatapult)
      playProject(IoMiningIndustries, 41) { draw(OptimalAerobraking) }.expect("PROD[2, 2 Titanium]")
      playProject(SolarPower, 2, steel = 3)
      playProject(Gyropolis, 2, steel = 6) {
            draw(SpaceHotels)
            placeTile(3, 7)
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
      wgt("OxygenStep")
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
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
      // Payment reconstruction: the titanium retained on Rotator Impacts is worth its full value
      // here, avoiding the action's two-unit overpayment.
      playProject(MagneticShield, 8, titanium = 4) { draw(BeamFromAThoriumAsteroid) }
      playProject(BeamFromAThoriumAsteroid, 32) { draw(Research) }.expect("PROD[3 Energy, 3 Heat]")
      cardAction2(ElectroCatapult)
      playProject(Thermophiles, 9)
      convertPlants { placeTile(8, 9) }
      playProject(OptimalAerobraking, 3, titanium = 1)
      // Test inference: the archive gives only the number sold; Penguins is the unplayed card
      // available at this point that is not needed later.
      sellPatents(Penguins)
      playProject(ImportedNutrients, 14) { addCardResources(Thermophiles) }.expect("3 Heat")
      cardAction2(Thermophiles)
      playProject(EquatorialMagnetizer, 2, steel = 3)
      cardAction1(EquatorialMagnetizer)

      pass()
      wgt("TemperatureStep")
      buyCards(FloaterPrototypes, TransNeptuneProbe, ConvoyFromEuropa)

      cardAction1(DevelopmentCenter) { draw(PioneerSettlement) }
      playProject(Research, 11) {
        draw(Shuttles, Atmoscoop)
        doTask("-ProjectCard")
        doTask("-ProjectCard")
        discard(Trees, TransNeptuneProbe)
        draw(GanymedeColony, HiTechLab)
      }
      convertHeat() { placeTile(5, 6) }
      convertPlants { placeTile(5, 7) }
      playProject(Shuttles, 2, titanium = 2).expect("PROD[2, -Energy]")
      playProject(PioneerSettlement, 3, titanium = 2) { doTask("Colony<Triton>") }
      stdAction("TradeSA", 2) { doTask("Trade<Io>") }
      convertHeat()
      convertHeat()
      playProject(TitanFloatingLaunchPad, 18) { addCardResources(TitanShuttles) }
      cardAction1(TitanFloatingLaunchPad) { addCardResources(TitanShuttles) }
      cardAction1(SearchForLife) { /* Decline the science resource. */
        declineTask()
      }
      // Payment reconstruction: retain the titanium from Pioneer Settlement for Terraforming
      // Ganymede, where it receives full value.
      intentionalUnderpay()
      playProject(Atmoscoop, 8, titanium = 3) {
        draw(MediaArchives)
        doTask("2 VenusStep")
        addCardResources(TitanShuttles)
      }
      playProject(FloaterPrototypes, 2) {
        discard(AirScrappingExpedition)
        draw(AsteroidCard)
        doTask("-ProjectCard")
        addCardResources(TitanShuttles)
      }
      cardAction2(TitanShuttles) { doTask("-11 Floater<$TitanShuttles> THEN 11 Titanium") }
      // Payment reconstruction: retain one titanium for Terraforming Ganymede rather than
      // overpaying for Ganymede Colony.
      intentionalUnderpay()
      playProject(GanymedeColony, 2, titanium = 4) { draw(Ants) }
      intentionalOverpay()
      playProject(ConvoyFromEuropa, titanium = 4) {
        draw(DustSeals)
        placeTile(9, 9)
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
      playProject(AsteroidCard, 12) { /* Decline removing an opponent's plants. */
        declineTask()
      }
      cardAction1(SaturnSurfing)
      cardAction2(Thermophiles)
      cardAction1(EquatorialMagnetizer)
      playProject(ProjectInspection, 0) { cardAction1(ElectroCatapult) }
      playProject(MediaArchives, 8)
      stdProject("CitySP") { placeTile(4, 4) }
      stdProject("GreenerySP") { placeTile(4, 5) }
      convertPlants { placeTile(3, 4) }
      playProject(DawnCity, 5, titanium = 2).expect("PROD[Titanium, -Energy]")
      cardAction1(AsteroidRights) { addCardResources(RotatorImpacts) }
      cardAction2(RotatorImpacts)
      cardAction1(Tardigrades)
      playProject(Windmills, 6)
      // Test inference: the archive gives only the number sold; Ants is the remaining unplayed
      // project card.
      sellPatents(Ants)

      pass()
      // Decline the final greenery placement.
      declineTask()

      assertCardTrackingComplete()
      cardsInHand shouldBe emptySet()

      assertResources(m = 106, s = 4, t = 6, p = 4, e = 1, h = 17)
      assertProduction(m = 27, s = 4, t = 6, p = 4, e = 1, h = 9)
      assertCounts(0 to "ProjectCard", 58 to "CardFront OR PlayedEvent")
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
      sum.net("$CryoSleep", "Owed<Player1, Class<Energy>>") shouldBe -2
      sum.net("$CryoSleep", "Owed<Player1, Class<Titanium>>") shouldBe -2

      // Resources and cards gained from active cards
      sum.net("$DevelopmentCenter", "ProjectCard") shouldBe 6
      // sum.net("$ElectroCatapult", "Plant") shouldBe -6
      // sum.net("$ElectroCatapult", "Steel") shouldBe -3
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

      assertCounts(75 to "TerraformRating")
      assertCounts(131 to "VictoryPoint")
      assertCounts(1 to "Victory")
    }
  }
}
