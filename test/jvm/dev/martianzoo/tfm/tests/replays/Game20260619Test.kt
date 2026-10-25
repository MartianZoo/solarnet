package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import kotlin.test.Test

internal class Game20260619Test : AbstractFullGameTest() {

  override val config =
      GameConfig(
          """
          VenusNextExpansion, PreludeExpansion, promos:cards
          -WorldGovernmentRule
          Specialist, Planner
          """,
          "Player1",
          "Player2",
      )

  @Test
  internal fun gameThroughGeneration5() {
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE
    admin.startGame()
    // Game id: peae6273d6b33
    // First player this generation is ER
    // Good luck ER!
    // Good luck KB!
    // Generation 1
    val ER = p1
    val KB = p2
    ER.doTask("20 TerraformRating")
    KB.doTask("20 TerraformRating")
    ER.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-4 ProjectCard<Selecting>",
    )
    KB.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-7 ProjectCard<Selecting>",
    )

    // ER played Point Luna
    // ER gained 1 titanium production
    // ER kept 6 project cards
    // ER drew 1 card(s)
    // You drew Domed Crater
    ER.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$PointLuna>>",
          "$PointLuna FROM CorporationCard",
          "38 MC",
          "PROD[Titanium]",
          "ProjectCard",
          "18 Pay<> FROM MC",
          "6 ProjectCard<Hand FROM Selecting>",
      )
    }

    // KB played Saturn Systems
    // KB gained 1 titanium production
    // KB kept 3 project cards
    // KB gained 1 M€ production because of Saturn Systems
    KB.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$SaturnSystems>>",
          "$SaturnSystems FROM CorporationCard",
          "42 MC",
          "PROD[Titanium]",
          "PROD[MC]",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }

    ER.turn {
      // ER played New Partner
      // ER gained 1 M€ production
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$NewPartner>>",
          "$NewPartner FROM PreludeCard",
          "PROD[MC]",
          "2 PreludeCard<Selecting>",
      )
      // ER played Biofuels
      // ER gained 1 plant production
      // ER gained 1 energy production
      // ER gained 2 plants
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Biofuels>, Selecting>",
          "$Biofuels FROM PreludeCard<Selecting>",
          "PROD[Plant]",
          "PROD[Energy]",
          "2 Plant",
      )

      // ER played Dome Farming
      // ER gained 2 M€ production
      // ER gained 1 plant production
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
          "$DomeFarming FROM PreludeCard",
          "PROD[Plant]",
          "PROD[2 MC]",
      )
    }

    KB.turn {
      // KB played Aquifer Turbines
      // KB gained 2 energy production
      // KB placed ocean tile at 13
      // KB drew 2 card(s)
      // You drew Physics Complex,Vesta Shipyard
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AquiferTurbines>>",
          "$AquiferTurbines FROM PreludeCard",
          "PROD[2 Energy]",
          "-3 MC",
          "OceanTile<Tharsis_2_6>",
          "TerraformRating",
          "2 ProjectCard",
      )

      // KB played Eccentric Sponsor
      // KB played Beam From A Thorium Asteroid
      // KB gained 3 energy production
      // KB gained 3 heat production
      // KB gained 1 M€ production because of Saturn Systems
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ExcentricSponsor>>",
          "$ExcentricSponsor FROM PreludeCard",
      )
      doTasks(
          "PlayCard<Class<ProjectCard>, Class<$BeamFromAThoriumAsteroid>>",
          "-25 Owed",
          "7 Pay<> FROM MC",
          "$BeamFromAThoriumAsteroid FROM ProjectCard",
          "PROD[3 Heat]",
          "PROD[3 Energy]",
          "PROD[MC]",
      )
    }

    // ER played Earth Office
    // ER drew 1 card(s)
    // You drew Restricted Area
    // ER ended turn
    ER.turn {
      doTasks(
          *playCardTasks("$EarthOffice"),
          "1 Pay<> FROM MC",
          "$EarthOffice FROM ProjectCard",
          "ProjectCard",
      )
    }

    // KB played Vesta Shipyard
    // KB gained 1 titanium production
    // KB gained 1 M€ production because of Saturn Systems
    // KB ended turn
    KB.turn {
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "15 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
          "PROD[MC]",
      )
    }

    // ER played Carbonate Processing
    // ER lost 1 energy production
    // ER gained 3 heat production
    // ER ended turn
    ER.turn {
      doTasks(
          *playCardTasks("$CarbonateProcessing"),
          "6 Pay<> FROM MC",
          "$CarbonateProcessing FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 Heat]",
      )
    }

    // KB passed
    KB.doTask("Pass")

    // ER played Subterranean Reservoir
    // ER placed ocean tile at 28
    // ER gained 2 plants
    ER.turn {
      doTasks(
          *playCardTasks("$SubterraneanReservoir"),
          "11 Pay<> FROM MC",
          "$SubterraneanReservoir FROM ProjectCard",
          "OceanTile<Tharsis_4_8>",
          "TerraformRating",
          "2 Plant",
      )

      // ER passed
      doTask("Pass")
    }

    ER.doTasks("8 MC", "-5 MC", "21 MC", "Titanium", "2 Plant", "3 Heat")
    KB.doTasks("-5 MC", "8 MC", "21 MC", "2 Titanium", "5 Energy", "3 Heat")
    ER.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")

    // Generation 2
    // First player this generation is KB
    // ER bought 3 card(s)
    // You bought Business Contacts,Lagrange Observatory,Solar Wind Power
    // KB bought 2 card(s)
    // You bought Magnetic Field Generators,Power Supply Consortium
    ER.doTasks("-ProjectCard<Selecting>", "9 Pay<> FROM MC", "3 ProjectCard<Hand FROM Selecting>")
    KB.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

    with(ER) {
      assertProduction(m = 3, s = 0, t = 1, p = 2, e = 0, h = 3)
      assertResources(m = 17, s = 0, t = 1, p = 6, e = 0, h = 3)
      assertDashMiddle(played = 7, actions = 0, tr = 21, hand = 8)
      assertTags(but = 2, spt = 1, eat = 2, plt = 1, mit = 1)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    with(KB) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 5, h = 3)
      assertResources(m = 26, s = 0, t = 2, p = 0, e = 5, h = 3)
      assertDashMiddle(played = 5, actions = 0, tr = 21, hand = 5)
      assertTags(spt = 2, pot = 2, jot = 3)
      assertDashRight(events = 0, tagless = 1, cities = 0)
    }

    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 2, venus = 0)

    // KB played Martian Survey
    // KB drew 2 card(s)
    // You drew Robotic Workforce,Ore Processor
    // KB ended turn
    KB.turn {
      doTasks(
          *playCardTasks("$MartianSurvey"),
          "9 Pay<> FROM MC",
          "$MartianSurvey FROM ProjectCard",
          "2 ProjectCard",
      )
    }

    // ER played Restricted Area
    // ER placed Restricted Area tile at 20
    // ER gained 1 steel
    // ER gained 4 M€ from 2 ocean(s)
    // ER used Restricted Area action
    // ER drew 1 card(s)
    // You drew Immigrant City
    ER.turn {
      doTasks(
          *playCardTasks("$RestrictedArea"),
          "11 Pay<> FROM MC",
          "$RestrictedArea FROM ProjectCard",
          "RestrictedArea_SpecialTile<Tharsis_3_7>",
          "Steel",
          "2 MC",
          "2 MC",
      )
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
    }

    // KB played Artificial Photosynthesis
    // KB gained 2 energy production
    // KB ended turn
    KB.turn {
      doTasks(
          *playCardTasks("$ArtificialPhotosynthesis"),
          "12 Pay<> FROM MC",
          "$ArtificialPhotosynthesis FROM ProjectCard",
          "PROD[2 Energy]",
      )
    }

    // ER played Solar Wind Power
    // ER gained 1 energy production
    // ER gained 2 titanium
    // ER ended turn
    ER.turn {
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
      )
    }

    // KB played Power Supply Consortium
    // KB stole 1 energy production from ER
    // KB ended turn
    KB.turn {
      doTasks(
          *playCardTasks("$PowerSupplyConsortium"),
          "5 Pay<> FROM MC",
          "$PowerSupplyConsortium FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Player1>]",
      )
    }

    // ER passed
    ER.doTask("Pass")

    // KB passed
    KB.doTask("Pass")

    ER.doTasks("8 MC", "-5 MC", "21 MC", "Titanium", "2 Plant", "3 Heat")
    KB.doTasks("8 MC", "-5 MC", "21 MC", "2 Titanium", "8 Energy", "3 Heat", "5 Heat FROM Energy")
    ER.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")

    // Generation 3
    // First player this generation is ER
    // KB bought 2 card(s)
    // You bought Soil Factory,Kelp Farming
    // ER bought 2 card(s)
    // You bought Natural Preserve,Nuclear Zone
    ER.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
    KB.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

    with(ER) {
      assertProduction(m = 3, s = 0, t = 1, p = 2, e = 0, h = 3)
      assertResources(m = 18, s = 1, t = 3, p = 8, e = 0, h = 6)
      assertDashMiddle(played = 9, actions = 1, tr = 21, hand = 9)
      assertTags(but = 2, spt = 2, sct = 2, pot = 1, eat = 2, plt = 1, mit = 1)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    with(KB) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 8, h = 3)
      assertResources(m = 18, s = 0, t = 4, p = 0, e = 8, h = 11)
      assertDashMiddle(played = 8, actions = 0, tr = 21, hand = 6)
      assertTags(spt = 2, sct = 1, pot = 3, jot = 3)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 2, venus = 0)

    // ER used Restricted Area action
    // ER drew 1 card(s)
    // You drew Satellites
    ER.turn {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")

      // ER used Convert Plants standard action
      // ER placed greenery tile at 19
      // ER gained 2 M€ from 1 ocean(s)
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_3_6>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
    }

    // KB used Convert Heat standard action
    // KB ended turn
    KB.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    // ER played Nuclear Zone
    // ER gained 1 heat production
    // ER drew 1 card(s)
    // You drew Nuclear Power
    // ER placed Nuclear Zone tile at 37
    // ER gained 2 plants
    // ER gained 2 M€ from 1 ocean(s)
    ER.turn {
      doTasks(
          *playCardTasks("$NuclearZone"),
          "7 Pay<> FROM MC",
          "$NuclearZone FROM ProjectCard",
          "2 TemperatureStep",
          "ProjectCard",
          "2 TerraformRating",
          "PROD[Heat]",
          "NuclearZone_SpecialTile<Tharsis_5_9>",
          "2 Plant",
          "2 MC",
      )

      // ER played Lagrange Observatory
      // ER drew 1 card(s)
      // You drew Trans-Neptune Probe
      doTasks(
          *playCardTasks("$LagrangeObservatory"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard",
      )
    }

    // KB used Power Plant:SP standard project
    // KB ended turn
    KB.turn {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }

    // ER played Business Contacts
    // ER drew 1 card(s)
    // You drew Phobos Space Haven
    // ER drew 2 card(s)
    // You drew Sponsors,ArchaeBacteria
    ER.turn {
      doTasks(
          *playCardTasks("$BusinessContacts"),
          "4 Pay<> FROM MC",
          "$BusinessContacts FROM ProjectCard",
          "4 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
          "ProjectCard",
      )

      // ER played Sponsors
      // ER gained 2 M€ production
      // ER drew 1 card(s)
      // You drew Imported GHG
      doTasks(
          *playCardTasks("$Sponsors"),
          "3 Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
          "ProjectCard",
      )
    }

    // KB passed
    KB.doTask("Pass")

    // ER played Imported GHG
    // ER gained 1 heat production
    // ER gained 3 heat
    // ER drew 1 card(s)
    // You drew Permafrost Extraction
    ER.turn {
      doTasks(
          *playCardTasks("$ImportedGhg"),
          "4 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
          "ProjectCard",
      )

      // ER passed
      doTask("Pass")
    }

    ER.doTasks("10 MC", "-5 MC", "24 MC", "Titanium", "2 Plant", "5 Heat")
    KB.doTasks("-5 MC", "8 MC", "22 MC", "2 Titanium", "9 Energy", "3 Heat", "8 Heat FROM Energy")
    ER.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")

    // Generation 4
    // First player this generation is KB
    // KB bought 2 card(s)
    // You bought Regolith Eaters,Power Plant
    // ER bought 2 card(s)
    // You bought Industrial Center,Miranda Resort
    ER.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
    KB.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

    with(ER) {
      assertProduction(m = 5, s = 0, t = 1, p = 2, e = 0, h = 5)
      assertResources(m = 25, s = 1, t = 1, p = 4, e = 0, h = 14)
      assertDashMiddle(played = 14, actions = 1, tr = 24, hand = 14)
      assertTags(but = 2, spt = 3, sct = 3, pot = 1, eat = 4, plt = 1, mit = 1)
      assertDashRight(events = 3, tagless = 1, cities = 0)
    }

    with(KB) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 9, h = 3)
      assertResources(m = 26, s = 0, t = 6, p = 0, e = 9, h = 14)
      assertDashMiddle(played = 8, actions = 0, tr = 22, hand = 8)
      assertTags(spt = 2, sct = 1, pot = 3, jot = 3)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    assertSidebar(gen = 4, temp = -24, oxygen = 1, oceans = 2, venus = 0)

    // KB played Power Plant
    // KB gained 1 energy production
    // KB ended turn
    KB.turn {
      doTasks(
          *playCardTasks("$PowerPlant"),
          "4 Pay<> FROM MC",
          "$PowerPlant FROM ProjectCard",
          "PROD[Energy]",
      )
    }

    // ER played Pets
    // ER drew 1 card(s)
    // You drew House Printing
    // ER added 1 Animal to Pets
    // ER ended turn
    ER.turn {
      doTasks(
          *playCardTasks("$Pets"),
          "7 Pay<> FROM MC",
          "$Pets FROM ProjectCard",
          "Animal<$Pets>",
          "ProjectCard",
      )
    }

    // KB claimed Specialist milestone
    // KB ended turn
    KB.turn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Specialist")
    }

    // ER played Mohole Area
    // ER gained 4 heat production
    // ER placed Mohole Area tile at 06
    // ER drew 1 card(s)
    // You drew Large Convoy
    // ER ended turn
    ER.turn {
      doTasks(
          *playCardTasks("$MoholeArea"),
          "1 Pay<Class<Steel>> FROM Steel",
          "18 Pay<> FROM MC",
          "$MoholeArea FROM ProjectCard",
          "PROD[4 Heat]",
          "MoholeArea_SpecialTile<Tharsis_1_4>",
          "ProjectCard",
      )
    }

    // KB played Ore Processor
    // KB ended turn
    KB.turn {
      doTasks(*playCardTasks("$OreProcessor"), "13 Pay<> FROM MC", "$OreProcessor FROM ProjectCard")
    }

    // ER passed
    ER.doTask("Pass")

    // KB passed
    KB.doTask("Pass")

    ER.doTasks("10 MC", "-5 MC", "24 MC", "Titanium", "2 Plant", "9 Heat")
    KB.doTasks("8 MC", "-5 MC", "22 MC", "2 Titanium", "10 Energy", "3 Heat", "9 Heat FROM Energy")
    ER.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")

    // Generation 5
    // First player this generation is ER
    // KB bought 2 card(s)
    // You bought Investment Loan,Tectonic Stress Power
    // ER bought 2 card(s)
    // You bought Micro-Mills,Lava Tube Settlement
    ER.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")
    KB.doTasks("-2 ProjectCard<Selecting>", "6 Pay<> FROM MC", "2 ProjectCard<Hand FROM Selecting>")

    with(ER) {
      assertProduction(m = 5, s = 0, t = 1, p = 2, e = 0, h = 9)
      assertResources(m = 23, s = 0, t = 2, p = 6, e = 0, h = 23)
      assertDashMiddle(played = 16, actions = 1, tr = 24, hand = 16)
      assertTags(but = 3, spt = 3, sct = 3, pot = 1, eat = 5, plt = 1, mit = 1, ant = 1)
      assertDashRight(events = 3, tagless = 1, cities = 0)
    }

    with(KB) {
      assertProduction(m = 3, s = 0, t = 2, p = 0, e = 10, h = 3)
      assertResources(m = 20, s = 0, t = 8, p = 0, e = 10, h = 26)
      assertDashMiddle(played = 10, actions = 1, tr = 22, hand = 8)
      assertTags(but = 2, spt = 2, sct = 1, pot = 4, jot = 3)
      assertDashRight(events = 1, tagless = 1, cities = 0)
    }

    assertSidebar(gen = 5, temp = -24, oxygen = 1, oceans = 2, venus = 0)

    // ER claimed Planner milestone
    // ER ended turn
    ER.turn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Planner")
    }

    // KB used Convert Heat standard action
    KB.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )

      // KB used Convert Heat standard action
      // KB gained 1 heat production
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
    }

    // ER used Restricted Area action
    // ER drew 1 card(s)
    // You drew Nitrophilic Moss
    ER.turn {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")

      // ER played ArchaeBacteria
      // ER gained 1 plant production
      doTasks(
          *playCardTasks("$Archaebacteria"),
          "6 Pay<> FROM MC",
          "$Archaebacteria FROM ProjectCard",
          "PROD[Plant]",
      )
    }

    // KB used Ore Processor action
    // KB gained 1 titanium
    // KB ended turn
    KB.turn {
      doTasks(
          *cardActionTasks("$OreProcessor"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Titanium",
          "OxygenStep",
          "TerraformRating",
      )
    }

    // ER played Miranda Resort
    // ER gained 5 M€ production
    // KB gained 1 M€ production because of Saturn Systems
    // ER ended turn
    ER.turn {
      doTasks(
          *playCardTasks("$MirandaResort"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "$MirandaResort FROM ProjectCard",
          "PROD[5 MC]",
      )
      KB.doTask("PROD[MC]")
    }

    // KB played Regolith Eaters
    // KB ended turn
    KB.turn {
      doTasks(
          *playCardTasks("$RegolithEaters"),
          "13 Pay<> FROM MC",
          "$RegolithEaters FROM ProjectCard",
      )
    }

    // ER used Convert Heat standard action
    // ER ended turn
    ER.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    // KB used Regolith Eaters action
    // KB added 1 Microbe to Regolith Eaters
    // KB ended turn
    KB.turn {
      doTasks(*cardActionTasks("$RegolithEaters"), "Microbe<$RegolithEaters>")
    }

    // ER used Convert Heat standard action
    // ER ended turn
    ER.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    // KB played Investment Loan
    // KB lost 1 M€ production
    // KB gained 10 M€
    // KB ended turn
    KB.turn {
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
    }

    // ER passed
    ER.doTask("Pass")

    // KB used Convert Heat standard action
    KB.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )

      // KB played Soil Factory
      // KB gained 1 plant production
      // KB lost 1 energy production
      // KB passed
      doTasks(
          *playCardTasks("$SoilFactory"),
          "9 Pay<> FROM MC",
          "$SoilFactory FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Plant]",
      )
      doTask("Pass")
    }
    ER.doTasks("15 MC", "-5 MC", "26 MC", "Titanium", "3 Plant", "9 Heat")
    KB.doTasks(
        "-5 MC",
        "8 MC",
        "26 MC",
        "2 Titanium",
        "9 Energy",
        "4 Heat",
        "6 Heat FROM Energy",
        "Plant",
    )
    ER.doTask("4 ProjectCard<Selecting>")
    KB.doTask("4 ProjectCard<Selecting>")
    expectPendingTasksAtEnd(4) // the next Research phase's two offers and deferred work
  }
}
