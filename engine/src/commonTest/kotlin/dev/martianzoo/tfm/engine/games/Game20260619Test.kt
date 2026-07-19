package dev.martianzoo.tfm.engine.games

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmWorkflow
import kotlin.test.Test

class Game20260619Test : AbstractFullGameTest() {

  override fun setup() = Canon.fromOptionCodes("BRMVPXT", 2)

  @Test
  fun gameThroughGeneration5() {
    val workflow = TfmWorkflow.Auto(game, setup()).launch()

    // Game id: peae6273d6b33
    // First player this generation is ER
    // Good luck ER!
    // Good luck KB!
    // Generation 1
    val ER = p1
    val KB = p2

    // ER played Point Luna
    // ER gained 1 titanium production
    // ER kept 6 project cards
    // ER drew 1 card(s)
    // You drew Domed Crater
    ER.playCorp("PointLuna", 6).expect("7 ProjectCard")

    // KB played Saturn Systems
    // KB gained 1 titanium production
    // KB kept 3 project cards
    // KB gained 1 M€ production because of Saturn Systems
    KB.playCorp("SaturnSystems", 3).expect("PROD[1]")

    with(ER) {
      // ER played New Partner
      // ER gained 1 M€ production
      playPrelude("NewPartner") {
        // ER played Biofuels
        // ER gained 1 plant production
        // ER gained 1 energy production
        // ER gained 2 plants
        playPrelude("Biofuels")
      }

      // ER played Dome Farming
      // ER gained 2 M€ production
      // ER gained 1 plant production
      playPrelude("DomeFarming")
    }

    with(KB) {
      // KB played Aquifer Turbines
      // KB gained 2 energy production
      // KB placed ocean tile at 13
      // KB drew 2 card(s)
      // You drew Physics Complex,Vesta Shipyard
      playPrelude("AquiferTurbines") { doTask("OceanTile<Tharsis_2_6>") }
          .expect("2 ProjectCard, TR")

      // KB played Eccentric Sponsor
      // KB played Beam From A Thorium Asteroid
      // KB gained 3 energy production
      // KB gained 3 heat production
      // KB gained 1 M€ production because of Saturn Systems
      playPrelude("ExcentricSponsor") { playProject("BeamFromAThoriumAsteroid", 7) }
          .expect("PROD[1], -7")
    }

    // ER played Earth Office
    // ER drew 1 card(s)
    // You drew Restricted Area
    // ER ended turn
    ER.playProject("EarthOffice", 1) // net 0 ProjectCard
    ER.declineSecondAction()

    // KB played Vesta Shipyard
    // KB gained 1 titanium production
    // KB gained 1 M€ production because of Saturn Systems
    // KB ended turn
    KB.playProject("VestaShipyard", 15).expect("PROD[1]")
    KB.declineSecondAction()

    // ER played Carbonate Processing
    // ER lost 1 energy production
    // ER gained 3 heat production
    // ER ended turn
    ER.playProject("CarbonateProcessing", 6)
    ER.declineSecondAction()

    // KB passed
    KB.pass()

    // ER played Subterranean Reservoir
    // ER placed ocean tile at 28
    // ER gained 2 plants
    ER.playProject("SubterraneanReservoir", 11) { doTask("OceanTile<Tharsis_4_8>") }
        .expect("TR, 2 Plant")
    ER.declineSecondAction()

    // ER passed
    ER.pass()

    // Generation 2
    // First player this generation is KB
    // ER bought 3 card(s)
    // You bought Business Contacts,Lagrange Observatory,Solar Wind Power
    // KB bought 2 card(s)
    // You bought Magnetic Field Generators,Power Supply Consortium
    ER.doFirstTask("3 BuyCard")
    KB.doFirstTask("2 BuyCard")

    with(ER) {
      assertProduction(m = 3, s = 0, t = 1, p = 2, e = 0, h = 3)
      assertResources(m = 17, s = 0, t = 1, p = 6, e = 0, h = 3)
      assertDashMiddle(played = 7, actions = 0, vp = 21, tr = 21, hand = 8)
      assertTags(but = 2, spt = 1, eat = 2, plt = 1, mit = 1)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    with(KB) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 5, h = 3)
      assertResources(m = 26, s = 0, t = 2, p = 0, e = 5, h = 3)
      assertDashMiddle(played = 5, actions = 0, vp = 23, tr = 21, hand = 5)
      assertTags(spt = 2, pot = 2, jot = 3)
      assertDashRight(events = 0, tagless = 1, cities = 0)
    }

    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 2, venus = 0)

    // KB played Martian Survey
    // KB drew 2 card(s)
    // You drew Robotic Workforce,Ore Processor
    // KB ended turn
    KB.playProject("MartianSurvey", 9)
    KB.declineSecondAction()

    // ER played Restricted Area
    // ER placed Restricted Area tile at 20
    // ER gained 1 steel
    // ER gained 4 M€ from 2 ocean(s)
    // ER used Restricted Area action
    // ER drew 1 card(s)
    // You drew Immigrant City
    ER.playProject("RestrictedArea", 11) { doTask("RaTile<Tharsis_3_7>") }.expect("-7, Steel")
    ER.cardAction1("RestrictedArea")

    // KB played Artificial Photosynthesis
    // KB gained 2 energy production
    // KB ended turn
    KB.playProject("ArtificialPhotosynthesis", 12) { doTask("PROD[2 Energy]") }
    KB.declineSecondAction()

    // ER played Solar Wind Power
    // ER gained 1 energy production
    // ER gained 2 titanium
    // ER ended turn
    ER.playProject("SolarWindPower", 8, titanium = 1)
    ER.declineSecondAction()

    // KB played Power Supply Consortium
    // KB stole 1 energy production from ER
    // KB ended turn
    KB.playProject("PowerSupplyConsortium", 5) { doTask("PROD[-E<P1>]") }
    KB.declineSecondAction()

    // ER passed
    ER.pass()

    // KB passed
    KB.pass()

    // Generation 3
    // First player this generation is ER
    // KB bought 2 card(s)
    // You bought Soil Factory,Kelp Farming
    // ER bought 2 card(s)
    // You bought Natural Preserve,Nuclear Zone
    ER.doFirstTask("2 BuyCard")
    KB.doFirstTask("2 BuyCard")

    with(ER) {
      assertProduction(m = 3, s = 0, t = 1, p = 2, e = 0, h = 3)
      assertResources(m = 18, s = 1, t = 3, p = 8, e = 0, h = 6)
      assertDashMiddle(played = 9, actions = 1, vp = 21, tr = 21, hand = 9)
      assertTags(but = 2, spt = 2, sct = 2, pot = 1, eat = 2, plt = 1, mit = 1)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    with(KB) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 8, h = 3)
      assertResources(m = 18, s = 0, t = 4, p = 0, e = 8, h = 11)
      assertDashMiddle(played = 8, actions = 0, vp = 24, tr = 21, hand = 6)
      assertTags(spt = 2, sct = 1, pot = 3, jot = 3)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 2, venus = 0)

    // ER used Restricted Area action
    // ER drew 1 card(s)
    // You drew Satellites
    ER.cardAction1("RestrictedArea")

    // ER used Convert Plants standard action
    // ER placed greenery tile at 19
    // ER gained 2 M€ from 1 ocean(s)
    ER.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_3_6>") }

    // KB used Convert Heat standard action
    // KB ended turn
    KB.stdAction("ConvertHeatSA")
    KB.declineSecondAction()

    // ER played Nuclear Zone
    // ER gained 1 heat production
    // ER drew 1 card(s)
    // You drew Nuclear Power
    // ER placed Nuclear Zone tile at 37
    // ER gained 2 plants
    // ER gained 2 M€ from 1 ocean(s)
    ER.playProject("NuclearZone", 7) { doTask("NzTile<Tharsis_5_9>") }

    // ER played Lagrange Observatory
    // ER drew 1 card(s)
    // You drew Trans-Neptune Probe
    ER.playProject("LagrangeObservatory", titanium = 3)

    // KB used Power Plant:SP standard project
    // KB ended turn
    KB.stdProject("PowerPlantSP")
    KB.declineSecondAction()

    // ER played Business Contacts
    // ER drew 1 card(s)
    // You drew Phobos Space Haven
    // ER drew 2 card(s)
    // You drew Sponsors,ArchaeBacteria
    ER.playProject("BusinessContacts", 4)

    // ER played Sponsors
    // ER gained 2 M€ production
    // ER drew 1 card(s)
    // You drew Imported GHG
    ER.playProject("Sponsors", 3)

    // KB passed
    KB.pass()

    // ER played Imported GHG
    // ER gained 1 heat production
    // ER gained 3 heat
    // ER drew 1 card(s)
    // You drew Permafrost Extraction
    ER.playProject("ImportedGhg", 4)
    ER.declineSecondAction()

    // ER passed
    ER.pass()

    // Generation 4
    // First player this generation is KB
    // KB bought 2 card(s)
    // You bought Regolith Eaters,Power Plant
    // ER bought 2 card(s)
    // You bought Industrial Center,Miranda Resort
    ER.doFirstTask("2 BuyCard")
    KB.doFirstTask("2 BuyCard")

    with(ER) {
      assertProduction(m = 5, s = 0, t = 1, p = 2, e = 0, h = 5)
      assertResources(m = 25, s = 1, t = 1, p = 4, e = 0, h = 14)
      assertDashMiddle(played = 14, actions = 1, vp = 24, tr = 24, hand = 14)
      assertTags(but = 2, spt = 3, sct = 3, pot = 1, eat = 4, plt = 1, mit = 1)
      assertDashRight(events = 3, tagless = 1, cities = 0)
    }

    with(KB) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 9, h = 3)
      assertResources(m = 26, s = 0, t = 6, p = 0, e = 9, h = 14)
      assertDashMiddle(played = 8, actions = 0, vp = 25, tr = 22, hand = 8)
      assertTags(spt = 2, sct = 1, pot = 3, jot = 3)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    assertSidebar(gen = 4, temp = -24, oxygen = 1, oceans = 2, venus = 0)

    // KB played Power Plant
    // KB gained 1 energy production
    // KB ended turn
    KB.playProject("PowerPlantCard", 4)
    KB.declineSecondAction()

    // ER played Pets
    // ER drew 1 card(s)
    // You drew House Printing
    // ER added 1 Animal to Pets
    // ER ended turn
    ER.playProject("Pets", 7)
    ER.declineSecondAction()

    // KB claimed Specialist milestone
    // KB ended turn
    // TODO: Specialist is an Elysium milestone, but this setup only loads Tharsis milestones.
    // Stop automatic turn enforcement at this intentionally raw substitute for the logged action.
    workflow.shutdown()
    KB.godMode().manual("-8, 5 VictoryPoint")

    // ER played Mohole Area
    // ER gained 4 heat production
    // ER placed Mohole Area tile at 06
    // ER drew 1 card(s)
    // You drew Large Convoy
    // ER ended turn
    ER.playProject("MoholeArea", 18, steel = 1) { doTask("MaTile<Tharsis_1_4>") }

    // KB played Ore Processor
    // KB ended turn
    KB.playProject("OreProcessor", 13)

    // ER passed
    ER.pass()

    // KB passed
    KB.pass()

    // Generation 5
    // First player this generation is ER
    // KB bought 2 card(s)
    // You bought Investment Loan,Tectonic Stress Power
    // ER bought 2 card(s)
    // You bought Micro-Mills,Lava Tube Settlement
    engine.nextGeneration(2, 2)

    with(ER) {
      assertProduction(m = 5, s = 0, t = 1, p = 2, e = 0, h = 9)
      assertResources(m = 23, s = 0, t = 2, p = 6, e = 0, h = 23)
      assertDashMiddle(played = 16, actions = 1, vp = 24, tr = 24, hand = 16)
      assertTags(but = 3, spt = 3, sct = 3, pot = 1, eat = 5, plt = 1, mit = 1, ant = 1)
      assertDashRight(events = 3, tagless = 1, cities = 0)
    }

    with(KB) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 10, h = 3)
      assertResources(m = 20, s = 0, t = 8, p = 0, e = 10, h = 26)
      assertDashMiddle(played = 10, actions = 1, vp = 30, tr = 22, hand = 8)
      assertTags(but = 2, spt = 2, sct = 1, pot = 4, jot = 3)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    assertSidebar(gen = 5, temp = -24, oxygen = 1, oceans = 2, venus = 0)

    // ER claimed Planner milestone
    // ER ended turn
    ER.stdAction("ClaimMilestoneSA") { doTask("Planner") }

    // KB used Convert Heat standard action
    KB.stdAction("ConvertHeatSA")

    // KB used Convert Heat standard action
    // KB gained 1 heat production
    KB.stdAction("ConvertHeatSA")

    // ER used Restricted Area action
    // ER drew 1 card(s)
    // You drew Nitrophilic Moss
    ER.cardAction1("RestrictedArea")

    // ER played ArchaeBacteria
    // ER gained 1 plant production
    ER.playProject("Archaebacteria", 6)

    // KB used Ore Processor action
    // KB gained 1 titanium
    // KB ended turn
    KB.cardAction1("OreProcessor")

    // ER played Miranda Resort
    // ER gained 5 M€ production
    // KB gained 1 M€ production because of Saturn Systems
    // ER ended turn
    ER.playProject("MirandaResort", 6, titanium = 2)

    // KB played Regolith Eaters
    // KB ended turn
    KB.playProject("RegolithEaters", 13)

    // ER used Convert Heat standard action
    // ER ended turn
    ER.stdAction("ConvertHeatSA")

    // KB used Regolith Eaters action
    // KB added 1 Microbe to Regolith Eaters
    // KB ended turn
    KB.cardAction1("RegolithEaters")

    // ER used Convert Heat standard action
    // ER ended turn
    ER.stdAction("ConvertHeatSA")

    // KB played Investment Loan
    // KB lost 1 M€ production
    // KB gained 10 M€
    // KB ended turn
    KB.playProject("InvestmentLoan", 3)

    // ER passed
    ER.pass()

    // KB used Convert Heat standard action
    KB.stdAction("ConvertHeatSA")

    // KB played Soil Factory
    // KB gained 1 plant production
    // KB lost 1 energy production
    // KB passed
    KB.playProject("SoilFactory", 9)
    KB.pass()
  }
}
