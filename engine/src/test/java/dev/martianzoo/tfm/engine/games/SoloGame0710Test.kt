package dev.martianzoo.tfm.engine.games

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test

class SoloGame0710Test : AbstractSoloTest() {
  override fun setup(): GameSetup {
    // There was Miranda too but I discarded it
    val colonyTiles = setOf(cn("Callisto"), cn("Ganymede"), cn("Luna"))
    return GameSetup(Canon, "BRMVPCTX", 2, colonyTiles)
  }

  override fun cityAreas(): Pair<String, String> = "Tharsis_4_1" to "Tharsis_5_8"
  override fun greeneryAreas(): Pair<String, String> = "Tharsis_5_1" to "Tharsis_5_7"

  @Test
  fun game() {
    with(me) {
      playCorp("PharmacyUnion", 10).expect("16, 11 ProjectCard")

      phase("Prelude")

      playPrelude("Merger") {
        // playCorp("Manutech", 0) - TODO this really should work
        doTask("PlayCard<Class<CorporationCard>, Class<Manutech>>")
      }

      playPrelude("HeadStart") {
        /*
         * Decline one of the actions, because the convenience API expects to see an empty queue
         * after each action. The game model does handle it fine, but it's annoying to interact with
         * and there's no real harm done in this case. Still... TODO.
         */
        doFirstTask("Ok")

        playProject("OlympusConference", 4, steel = 3).expect("Science")
      }

      phase("Action")
      playProject("StandardTechnology", 6) { doTask("ProjectCard FROM Science<OlympusConference>") }
      playProject("AdvancedAlloys", 9) {
        doTask("PlayedEvent<Class<PharmacyUnion>> FROM PharmacyUnion THEN 3 TerraformRating")
      }
      playProject("IndustrialMicrobes", 12).expect("S, E, PROD[S, E]")

      nextRound("VenusStep", 2)

      assertProduction(m = -2, s = 2, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 11, s = 3, t = 0, p = 0, e = 1, h = 1)
      assertDashMiddle(played = 8, actions = 0, vp = 20, tr = 19, hand = 10)
      assertTags(but = 3, sct = 3, eat = 1, mit = 1)

      // The app was showing tagless = 3, so... solarnet found its very first herokuapp bug!
      // https://github.com/terraforming-mars/terraforming-mars/issues/5847
      assertDashRight(events = 1, tagless = 2, cities = 0, colonies = 0)
      assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 2)

      playProject("CarbonateProcessing", steel = 2).expect("3 Heat")

      nextRound("VenusStep", 1)

      assertProduction(m = -2, s = 2, t = 0, p = 0, e = 0, h = 3)
      assertResources(m = 25, s = 3, t = 0, p = 0, e = 0, h = 8)
      assertDashMiddle(played = 9, actions = 0, vp = 20, tr = 19, hand = 10)
      assertTags(but = 4, sct = 3, eat = 1, mit = 1)
      assertDashRight(events = 1, tagless = 2, cities = 0, colonies = 0)
      assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 0, venus = 4)

      playProject("DeepWellHeating", 4, steel = 3).expect("Energy, TR")
      stdAction("ConvertHeatSA").expect("TR")
      playProject("NoctisCity", 18).expect("CityTile<Tharsis_5_3>, 2 Plant")
      playProject("FueledGenerators", 1)

      nextRound("VenusStep", 1)

      playProject("EnergySaving", 15)
      stdAction("TradeSA", 2) { doTask("Trade<Callisto, TradeFleetA>") }.expect("4 E")

      nextRound("OceanTile<Tharsis_5_5>", 1)

      assertProduction(m = 0, s = 2, t = 0, p = 0, e = 4, h = 3)
      assertResources(m = 26, s = 4, t = 0, p = 2, e = 4, h = 16)
      assertDashMiddle(played = 13, actions = 0, vp = 22, tr = 21, hand = 8)
      assertTags(but = 7, sct = 3, pot = 3, eat = 1, mit = 1, cit = 1)
      assertDashRight(events = 1, tagless = 2, cities = 1, colonies = 0)
      assertSidebar(gen = 5, temp = -26, oxygen = 0, oceans = 1, venus = 6)

      stdAction("ConvertHeatSA")
      stdAction("ConvertHeatSA")
      stdProject("BuildColonySP") { doTask("Colony<Luna>") }
      stdAction("TradeSA", 2) { doTask("Trade<Luna, TradeFleetA>") }.expect("-3 E, 15")
      playProject("GiantSolarShade", 27).expect("Card")
      playProject("GeothermalPower", 2, steel = 3)

      nextRound("VenusStep", 2)

      stdAction("ConvertHeatSA").expect("PROD[Heat]")
      stdAction("TradeSA", 2) { doTask("Trade<Ganymede, TradeFleetA>") }
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_6_3>") }.expect("-6 Plant, TR")

      playProject("MineralDeposit", 5)
      playProject("FieldCappedCity", 5, steel = 8) { doTask("CityTile<Tharsis_7_4>") }

      nextRound("OceanTile<Tharsis_6_7>", 2)

      assertProduction(m = 4, s = 2, t = 0, p = 0, e = 7, h = 5)
      assertResources(m = 40, s = 2, t = 0, p = 5, e = 7, h = 10)
      assertDashMiddle(played = 17, actions = 0, vp = 32, tr = 28, hand = 9)
      assertTags(but = 9, spt = 1, sct = 3, pot = 5, eat = 1, vet = 1, mit = 1, cit = 2)
      assertDashRight(events = 2, tagless = 2, cities = 2, colonies = 1)
      assertSidebar(gen = 7, temp = -20, oxygen = 1, oceans = 2, venus = 14)

      val AsteroidRights = "AsteroidRights"
      playProject(AsteroidRights, 10)
      cardAction2(AsteroidRights) { doTask("2 Titanium") }
      stdAction("ConvertHeatSA")
      playProject("ViralEnhancers", 9) { doTask("ProjectCard FROM Science<OlympusConference>") }
      playProject("QuantumExtractor", 13)
      playProject("SoilFactory", 3, steel = 2)

      nextRound("OxygenStep", 3)

      cardAction2(AsteroidRights) { doTask("2 Titanium") }
      stdAction("ConvertHeatSA")
      stdAction("ConvertHeatSA")
      stdAction("TradeSA", 2) { doTask("Trade<Luna, TradeFleetA>") }

      playProject("GiantIceAsteroid", 18, titanium = 4) {
        doTask("-6 Plant<Player2>")
        doFirstTask("OceanTile<Tharsis_5_4>")
        doFirstTask("OceanTile<Tharsis_5_6>")
      }
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_6_4>") }
      playProject("MagneticShield", 22)

      nextRound("OxygenStep", 3)

      assertProduction(m = 4, s = 2, t = 0, p = 1, e = 10, h = 5)
      assertResources(m = 44, s = 4, t = 0, p = 6, e = 10, h = 14)
      assertDashMiddle(played = 23, actions = 1, vp = 48, tr = 40, hand = 10)
      assertTags(but = 10, spt = 3, sct = 5, pot = 6, eat = 2, vet = 1, mit = 2, cit = 2)
      assertDashRight(events = 3, tagless = 2, cities = 2, colonies = 1)
      assertSidebar(gen = 9, temp = -10, oxygen = 4, oceans = 4, venus = 14)

      stdAction("ConvertHeatSA")
      playProject("ResearchOutpost", 6, steel = 4) {
        doTask("ProjectCard FROM Science<OlympusConference>")
        doTask("CityTile<Tharsis_8_6>")
      }
      playProject("IceMoonColony", 20) {
        doTask("Colony<Ganymede>")
        doTask("OceanTile<Tharsis_2_6>")
      }
      stdProject("AirScrappingSP").expect("-12")
      cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidRights>") }
      stdAction("TradeSA", 2) { doTask("Trade<Ganymede, TradeFleetA>") }
      sellPatents(3)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_8_7>") }
      playProject("PermafrostExtraction", 7) { doTask("OceanTile<Tharsis_9_9>") }

      nextRound("OxygenStep", 2)

      playProject("MiningExpedition", 11) { doTask("-2 Plant<Player2>") }
      stdAction("ConvertHeatSA")
      stdAction("ConvertHeatSA")
      playProject("StripMine", 12, steel = 4)
      sellPatents(1)

      val SubZeroSaltFish = "SubZeroSaltFish"
      playProject(SubZeroSaltFish, 4) {
        doTask("PROD[-Plant<Player2>]")
        doTask("Animal<$SubZeroSaltFish>") // Viral Enhancers
      }
      cardAction1(SubZeroSaltFish).expect("Animal")
      cardAction2(AsteroidRights) { doTask("2 Titanium") }

      val GhgProducingBacteria = "GhgProducingBacteria"
      playProject(GhgProducingBacteria, 7) { doTask("Plant") }
      playProject("ImportedNitrogen", 0, titanium = 5) {
        doTask("3 Microbe<$GhgProducingBacteria>")
        doTask("2 Animal<$SubZeroSaltFish>")
      }
      cardAction2(GhgProducingBacteria) { doTask("OceanTile<Tharsis_1_2>") }

      // TODO: this ! really should not be necessary
      playProject("ArtificialLake", 2, steel = 4) { doTask("OceanTile<Tharsis_6_6>!") }
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_5_2>") }

      val RefugeeCamps = "RefugeeCamps"
      playProject(RefugeeCamps, 9)
      cardAction1(RefugeeCamps)
      stdAction("TradeSA", 2) { doTask("Trade<Callisto, TradeFleetA>") }

      nextRound("VenusStep", 4)

      assertProduction(m = 3, s = 4, t = 1, p = 2, e = 8, h = 5)
      assertResources(m = 55, s = 4, t = 1, p = 7, e = 8, h = 27)
      assertDashMiddle(played = 33, actions = 4, vp = 73, tr = 57, hand = 7)
      assertTags(but = 13, spt = 4, sct = 7, pot = 6, eat = 3, vet = 1, mit = 3, ant = 1, cit = 3)
      assertDashRight(events = 6, tagless = 2, cities = 3, colonies = 2)
      assertSidebar(gen = 11, temp = 0, oxygen = 10, oceans = 8, venus = 18)

      stdAction("ConvertHeatSA")
      stdAction("ConvertHeatSA")
      cardAction1(RefugeeCamps)
      playProject("IceCapMelting", 4) { doTask("OceanTile<Tharsis_1_4>") }

      stdAction("TradeSA", 2) { doTask("Trade<Luna, TradeFleetA>") }
      playProject("TransNeptuneProbe", 3) { doTask("ProjectCard FROM Science<OlympusConference>") }
      stdProject("CitySP") { doTask("CityTile<Tharsis_6_5>") }
      playProject("UrbanizedArea", steel = 3) { doTask("CityTile<Tharsis_7_5>") }
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_8_5>") }
      cardAction1(SubZeroSaltFish)
      cardAction1(GhgProducingBacteria)
      cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidRights>") }
      stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_8_4>") }
      playProject("JovianEmbassy", 4, steel = 3)
      stdAction("ConvertHeatSA")
      sellPatents(2)
      playProject("DawnCity", 8, titanium = 1)
      stdProject("AirScrappingSP")

      nextRound("VenusStep", 2)

      assertProduction(m = 5, s = 4, t = 2, p = 2, e = 6, h = 5)
      assertResources(m = 68, s = 4, t = 3, p = 2, e = 6, h = 13)
      assertDashMiddle(played = 38, actions = 4, vp = 95, tr = 65, hand = 4)
      assertTags(
          but = 15, spt = 6, sct = 8, pot = 6, eat = 3, jot = 1, vet = 1, mit = 3, ant = 1, cit = 5)
      assertDashRight(events = 7, tagless = 2, cities = 6, colonies = 2)
      assertSidebar(gen = 12, temp = 6, oxygen = 12, oceans = 9, venus = 22)

      stdAction("ConvertHeatSA")
      cardAction2(AsteroidRights) { doTask("2 Titanium") }
      cardAction1(SubZeroSaltFish)
      cardAction1(RefugeeCamps)
      cardAction1(GhgProducingBacteria) // uselessly
      playProject("MagneticFieldDome", 1, steel = 1)
      stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_9_6>") }
      playProject("InterstellarColonyShip", 1, titanium = 5)
      stdAction("TradeSA", 2) { doTask("Trade<Luna, TradeFleetA>") }
      stdProject("CitySP") { doTask("CityTile<Tharsis_9_5>") }
      playProject("SpacePort", 3, steel = 6) {
        doTask("CityTile<Tharsis_6_2>")
        doTask("TradeFleetC")
      }
      sellPatents(1)
      stdAction("TradeSA", 2) { doTask("Trade<Ganymede, TradeFleetA>") }
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_7_6>") }
      stdProject("GreenerySP") { doTask("GreeneryTile<Tharsis_7_3>") }
      stdProject("AirScrappingSP")

      pass()
      engine.phase("Production")
      engine.phase("End")

      // Check the summary data on the you-won page
      val sum = Summarizer(game)
      assertCounts(70 to "TerraformRating")
      assertThat(sum.net("GreeneryTile", "VictoryPoint<P1>")).isEqualTo(9)
      assertThat(sum.net("CityTile", "VictoryPoint<P1>")).isEqualTo(24)
      assertThat(sum.net("Card", "VictoryPoint<P1>")).isEqualTo(18)
      assertCounts(121 to "VictoryPoint")
      assertCounts(82 to "Megacredit")

      assertThat(sum.net("ActionPhase", "UseAction<P1>")).isEqualTo(93) // note UI says 106
    }
  }
}
