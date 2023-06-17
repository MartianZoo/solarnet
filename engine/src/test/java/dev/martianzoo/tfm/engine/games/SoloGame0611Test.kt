package dev.martianzoo.tfm.engine.games

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import org.junit.jupiter.api.Test

class SoloGame0611Test : AbstractSoloTest() {
  // @Test // for profiling
  fun ten() {
    repeat (10) {
      commonSetup()
      letsPlay()
    }
  }

  @Test
  fun letsPlay() {
    engine.phase("Corporation")
    opponent.godMode().sneak("CityTile<H51>, GreeneryTile<H62>")
    opponent.godMode().sneak("CityTile<H84>, GreeneryTile<H95>")

    with(me) {
      playCorp("ValleyTrust", 5).expect("5 ProjectCard")

      assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 22, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertDashMiddle(played = 1, actions = 0, vp = 14, tr = 14, hand = 5)
      assertTags(eat = 1) // ...
      assertDashRight(events = 0, tagless = 0, cities = 0)
      assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 0, venus = 0)

      engine.phase("Prelude")

      playPrelude("Biolab").expect("3 Card")
      playPrelude("NewPartner") { playPrelude("BusinessEmpire") }.expect("PROD[7]")

      engine.phase("Action")

      stdAction("HandleMandates") { playPrelude("GalileanMining") }.expect("PROD[2 T]")
      playProject("IndenturedWorkers", 0)
      playProject("IndustrialMicrobes", 4).expect("PROD[S, E], MicrobeTag")
      pass()

      engine.nextGeneration(2, 0)

      assertProduction(m = 7, s = 1, t = 2, p = 1, e = 1, h = 0)
      assertResources(m = 22, s = 1, t = 2, p = 1, e = 1, h = 0)
      assertDashMiddle(played = 7, actions = 0, vp = 13, tr = 14, hand = 8)
      assertTags(but = 1, sct = 1, eat = 2, jot = 1, mit = 1) // ...
      assertDashRight(events = 1, tagless = 1, cities = 0)
      assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 0)

      playProject("AcquiredCompany", 10).expect("EarthTag, PROD[3]")
      pass()

      engine.nextGeneration(2, 0)

      val ForcedPrecipitation = "ForcedPrecipitation"
      playProject("AsteroidCard", 2, titanium = 4) { doTask("-2 Plant<P2>") }.expect("TEMP, TR")
      playProject("PeroxidePower", 3, steel = 2)
      playProject(ForcedPrecipitation, 8)
      cardAction1(ForcedPrecipitation).expect("-2, Floater")
      playProject("Solarnet", 7).expect("2 Card")
      pass()

      engine.nextGeneration(1, 0)

      assertProduction(m = 9, s = 1, t = 2, p = 1, e = 3, h = 0)
      assertResources(m = 29, s = 1, t = 4, p = 3, e = 3, h = 2)
      assertDashMiddle(played = 12, actions = 1, vp = 15, tr = 15, hand = 8)
      assertTags(but = 2, sct = 1, pot = 1, eat = 3, jot = 1, vet = 1, mit = 1) // ...
      assertDashRight(events = 2, tagless = 2, cities = 0)
      assertSidebar(gen = 4, temp = -28, oxygen = 0, oceans = 0, venus = 0)

      val RegolithEaters = "RegolithEaters"
      val SubCrustMeasurements = "SubCrustMeasurements"
      playProject(RegolithEaters, 11)
      cardAction1(RegolithEaters)
      cardAction1(ForcedPrecipitation).expect("-2, Floater")
      playProject(SubCrustMeasurements, 16, steel = 1)
      cardAction1(SubCrustMeasurements).expect("Card")
      pass()

      engine.nextGeneration(2, 0)

      cardAction1(SubCrustMeasurements).expect("Card")
      cardAction2(ForcedPrecipitation).expect("TR")
      cardAction1(RegolithEaters)
      playProject("SmallAsteroid", 1, titanium = 3) { doTask("-2 Plant<P2>") }.expect("TR")
      stdProject("AsteroidSP").expect("PROD[H]")
      playProject("MagneticFieldDome", 3, steel = 1).expect("TR, PROD[P, -2 E], AutomatedCard")

      engine.nextGeneration(2, 0)

      assertProduction(m = 9, s = 1, t = 2, p = 2, e = 1, h = 1)
      assertResources(m = 22, s = 1, t = 5, p = 6, e = 1, h = 9)
      assertDashMiddle(played = 16, actions = 3, vp = 21, tr = 19, hand = 10)
      assertTags(but = 4, sct = 3, pot = 1, eat = 4, jot = 1, vet = 1, mit = 2)
      assertDashRight(events = 3, tagless = 2, cities = 0)
      assertSidebar(gen = 6, temp = -24, oxygen = 0, oceans = 0, venus = 2)

      stdAction("ConvertHeatSA").expect("-8 Resource, TR")
      cardAction1(SubCrustMeasurements).expect("ProjectCard")
      cardAction2(RegolithEaters).expect("O2, TR")
      playProject("FueledGenerators", 1)
      playProject("EnergyTapping", 3) { doTask("PROD[-E<P2>]") }.expect("PROD[E<P1>, -E<P2>]")
      playProject("MagneticShield", 9, titanium = 5).expect("4 TR")
      cardAction1(ForcedPrecipitation)

      pass()
      nextGeneration(3, 0)

      // TODO save trouble by not using SA in the first place
      autoExecMode = NONE
      stdAction("ConvertPlantsSA") {
        doTask(game.tasks.ids().first()) // mandate
        doTask("-8 Plant")
        godMode().dropTask(game.tasks.ids().single())
      }
      autoExecMode = FIRST
      godMode().manual("GreeneryTile<H97>") { doTask("OceanTile<H56>") }.expect("2 TR, Card, -6")

      cardAction1(SubCrustMeasurements)
      cardAction1(ForcedPrecipitation).expect("-2")
      cardAction1(RegolithEaters).expect("Microbe")
      // TODO want to expect 0 ProjectCard
      playProject("ResearchOutpost", 12, steel = 2) { doTask("CityTile<H76>") }

      playProject("Cartel", 7).expect("PROD[5]")
      playProject("Supercapacitors", 3).expect("PROD[1]")
      pass()

      phase("Production") { me.doTask("1 Heat<P1> FROM Energy<P1>") }
      phase("Research") {
        me.doTask("3 BuyCard")
        opponent.doTask("Ok")
      }
      phase("Action")

      assertProduction(m = 14, s = 1, t = 2, p = 2, e = 3, h = 1)
      assertResources(m = 33, s = 1, t = 4, p = 2, e = 5, h = 5)
      assertDashMiddle(played = 22, actions = 3, vp = 29, tr = 27, hand = 14)
      assertDashRight(events = 3, tagless = 2, cities = 1)
      assertSidebar(gen = 8, temp = -22, oxygen = 2, oceans = 1, venus = 2)

      val EquatorialMagnetizer = "EquatorialMagnetizer"
      cardAction1(SubCrustMeasurements)
      playProject("AtalantaPlanitiaLab", 7)
      cardAction1(RegolithEaters)
      cardAction2(ForcedPrecipitation)
      playProject("CarbonateProcessing", 3, steel = 1)
      playProject(EquatorialMagnetizer, 10)
      cardAction1(EquatorialMagnetizer)
      playProject("VestaShipyard", 2, titanium = 4)
      sellPatents(4)
      playProject("CorporateStronghold", 10) { doTask("CityTile<H55>") }
      pass()

      phase("Production") { me.doTask("Ok") }
      phase("Research") {
        me.doTask("2 BuyCard")
        opponent.doTask("Ok")
      }
      phase("Action")

      val AiCentral = "AiCentral"
      stdAction("ConvertHeatSA")
      cardAction1(SubCrustMeasurements)
      cardAction2(RegolithEaters)
      playProject("Archaebacteria", 5)
      playProject("PowerGrid", 17)
      cardAction1(EquatorialMagnetizer)
      playProject(AiCentral, 16, steel = 1)
      cardAction1(AiCentral)
      cardAction1(ForcedPrecipitation)
      pass()

      phase("Production") { me.doTask("2 Heat FROM Energy") }
      phase("Research") {
        me.doTask("Ok")
        opponent.doTask("Ok")
      }
      phase("Action")

      assertProduction(m = 17, s = 1, t = 3, p = 3, e = 3, h = 5)
      assertResources(m = 56, s = 1, t = 6, p = 7, e = 6, h = 8)
      assertDashMiddle(played = 30, actions = 5, vp = 36, tr = 32, hand = 10)
      assertDashRight(events = 3, tagless = 2, cities = 2)
      assertSidebar(gen = 10, temp = -20, oxygen = 3, oceans = 1, venus = 4)

      cardAction1(AiCentral)
      cardAction1(SubCrustMeasurements)
      cardAction1(EquatorialMagnetizer)
      stdAction("ConvertHeatSA")
      playProject("StripMine", 22, steel = 1)
      playProject("Potatoes", 1)
      playProject("MirandaResort", 2, titanium = 3)
      cardAction1(RegolithEaters)
      cardAction1(ForcedPrecipitation)
      stdProject("AquiferSP") { doTask("OceanTile<H67>") }
      sellPatents(3)
      stdProject("AirScrappingSP")
      pass()

      phase("Production") { me.doTask("3 Heat FROM Energy") }
      phase("Research") {
        me.doTask("3 BuyCard")
        opponent.doTask("Ok")
      }
      phase("Action")

      stdAction("ConvertHeatSA")
      cardAction1(AiCentral)
      cardAction1(SubCrustMeasurements)
      cardAction2(ForcedPrecipitation)
      stdAction("ConvertPlantsSA") { doTask("GT<H66>") }

      val WaterSplittingPlant = "WaterSplittingPlant"
      playProject(WaterSplittingPlant, 5, steel = 3)
      cardAction1(WaterSplittingPlant)

      playProject("BribedCommittee", 6)
      playProject("QuantumExtractor", 10)

      cardAction1(EquatorialMagnetizer)
      playProject("ImportedGhg", 4)
      playProject("NitrogenRichAsteroid", 7, titanium = 7)
      stdProject("GreenerySP") { doTask("GT<H77>") }
      cardAction1(RegolithEaters)
      pass()

      phase("Production") { me.doTask("Ok") }
      phase("Research") {
        me.doTask("Ok")
        opponent.doTask("Ok")
      }
      phase("Action")

      stdAction("ConvertHeatSA")
      cardAction1(AiCentral)
      cardAction1(SubCrustMeasurements)
      cardAction1(WaterSplittingPlant)
      cardAction1(EquatorialMagnetizer)
      cardAction2(RegolithEaters)

      playProject("Greenhouses", 1, steel = 2)
      playProject("TerraformingGanymede", 18, titanium = 4).expect("4 TR")
      sellPatents(6)
      stdProject("AirScrappingSP")
      stdProject("AirScrappingSP")
      stdProject("AirScrappingSP")
      stdProject("AirScrappingSP").expect("2 TR")
      sellPatents(3)
      playProject("TransNeptuneProbe", 1)
      stdAction("ConvertPlantsSA") { doTask("GreeneryTile<H65>") }

      assertProduction(m = 24, s = 3, t = 4, p = 4, e = 2, h = 6)
      assertResources(m = 8, s = 1, t = 0, p = 0, e = 0, h = 1)
      assertDashMiddle(played = 41, actions = 1, vp = 78, tr = 64, hand = 0)
      assertDashRight(events = 6, tagless = 2, cities = 2)
      assertSidebar(gen = 12, temp = -10, oxygen = 11, oceans = 2, venus = 16)

      pass()
      phase("Production") { me.doTask("Ok") }

      // Victory check should happen here
      assertThat(count("TR")).isAtLeast(63)

      // Final plant conversion would happen, but...
      assertCounts(4 to "Plant")
      assertCounts(12 to "Tile") // checking for the heck of it

      phase("End")
      assertCounts(78 to "VP") // wow that was not good
    }
  }
}
