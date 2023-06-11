package dev.martianzoo.tfm.engine.games

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.tfm.api.Exceptions.DependencyException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.data.TaskResult
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestHelpers.assertNetChanges
import dev.martianzoo.tfm.engine.TestHelpers.assertProds
import dev.martianzoo.tfm.engine.TestHelpers.nextGeneration
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.Timeline.AbortOperationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class Game20230521Test {
  // @Test // for profiling
  fun games() = repeat(10) { game() }

  @Test
  fun game() {
    val game = Engine.newGame(GameSetup(Canon, "BRMVPXT", 2))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    /*
        The // comments below are log messages taken directly from the herokuapp.
        After every generation is a giant block of assertions, which use the
        numbers I see in the herokuapp game. So this is basically a parity test.

        I had to cheat in a couple places; search for TODO or godMode().
    */

    fun TaskResult.expect(string: String) = assertNetChanges(this, engine, string)

    // Good luck Player1!
    // Good luck Player2!
    // Generation 1
    engine.phase("Corporation")

    // Player1's steel production increased by 1
    // Player1 played Manutech
    // Player1 kept 5 project cards
    p1.playCorp("Manutech", 5).expect("PROD[S], 20, S, 5 ProjectCard")

    // Player2's steel production increased by 1
    // Player2 played Factorum
    // Player2 kept 4 project cards
    p2.playCorp("Factorum", 4).expect("PROD[S], 25, 4 ProjectCard")

    engine.phase("Prelude")

    with(p1) {
      // Player1 played New Partner
      // Player1's megacredits production increased by 1
      // You drew UNMI Contractor and Corporate Archives
      playPrelude("NewPartner") {
        // Player1 played UNMI Contractor
        // Player1 drew 1 card(s)
        // You drew Ganymede Colony
        playPrelude("UnmiContractor")
      }.expect("PROD[1], 1, ProjectCard, 3 TR")

      // Player1 played Allied Bank
      // Player1's megacredits production increased by 4
      // Player1's megacredits amount increased by 3
      playPrelude("AlliedBank").expect("PROD[4], 7, EarthTag")
    }

    with(p2) {
      // Player2 played Acquired Space Agency
      // Player2's titanium amount increased by 6
      // Player2 drew Rotator Impacts and Atmoscoop
      playPrelude("AcquiredSpaceAgency")
      // Player2 played Io Research Outpost
      // Player2's titanium production increased by 1
      // Player2 drew 1 card(s)
      // You drew Physics Complex
      playPrelude("IoResearchOutpost")
    }

    engine.phase("Action")

    // Player1 played Inventors' Guild
    // Player1 ended turn
    p1.playProject("InventorsGuild", 9)

    // Player2 played Arctic Algae
    // Player2's plants amount increased by 1
    // Player2 ended turn
    p2.playProject("ArcticAlgae", 12).expect("-12, Plant, PlantTag")

    // Player1 used Inventors' Guild action
    p1.cardAction1("InventorsGuild") {
      // Player1 bought 1 card(s)
      // You drew Corporate Stronghold
      doTask("BuyCard")
    }
    // Player1 ended turn

    // Player2 used Factorum action
    // Player2's energy production increased by 1
    p2.cardAction1("Factorum").expect("PROD[E]")
    // Player2 ended turn

    // Player1 used Power Plant:SP standard project
    p1.stdProject("PowerPlantSP")
    // Player1 played Building Industries
    // Player1's steel production increased by 2
    // Player1's energy production decreased by 1
    p1.playProject("BuildingIndustries", 4, steel = 1)

    // Player2 played Rotator Impacts
    p2.playProject("RotatorImpacts", titanium = 2)
    // Player2 used Rotator Impacts action
    p2.cardAction1("RotatorImpacts") {
      doTask("2 Pay<Class<T>> FROM T")
      // Player2 added 1 asteroid(s) to Rotator Impacts
      doFirstTask("Ok") // take care of the Accept<Megacredit>, yuck TODO
    }

    // Player1 passed
    p1.pass()

    // Player2 played Carbonate Processing
    // Player2's energy production decreased by 1
    // Player2's heat production increased by 3
    p2.playProject("CarbonateProcessing", 6)
    // Player2 played ArchaeBacteria
    // Player2's plants production increased by 1
    p2.playProject("Archaebacteria", 6)
    // Player2 passed
    p2.pass()

    // Generation 2
    // Player1 bought 2 card(s)
    // You drew Investment Loan and Deuterium Export
    // Player2 bought 2 card(s)
    // You drew Mars University and Steelworks
    engine.nextGeneration(2, 2)

    with(p1) {
      assertProds(5 to "M", 3 to "S", 0 to "T", 0 to "P", 0 to "E", 0 to "H")
      assertCounts(23 to "M", 5 to "S", 0 to "T", 0 to "P", 0 to "E", 1 to "H")
      assertDashMiddle(played = 6, actions = 1, vp = 23, tr = 23, hand = 7)
      assertTags(2 to "BUT", 1 to "SCT", 2 to "EAT")
      assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    with(p2) {
      assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 0 to "E", 3 to "H")
      assertCounts(15 to "M", 1 to "S", 3 to "T", 2 to "P", 0 to "E", 3 to "H")
      assertDashMiddle(played = 7, actions = 2, vp = 20, tr = 20, hand = 5)
      assertTags(2 to "BUT", 1 to "SPT", 1 to "SCT", 1 to "POT", 1 to "JOT", 1 to "PLT", 1 to "MIT")
      assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 0)

    // Player2 used Factorum action
    // Player2 drew Gyropolis
    p2.cardAction2("Factorum")
    // Player2 played Mars University
    // Player2 is using their Mars University effect to draw a card by discarding a card.
    // You discarded Physics Complex
    // Player2 drew 1 card(s)
    // You drew Virus
    p2.playProject("MarsUniversity", 6, steel = 1)

    // Player1 used Inventors' Guild action
    p1.cardAction1("InventorsGuild") {
      // Player1 bought 1 card(s)
      // You drew Development Center
      doFirstTask("BuyCard")
    }
    // Player1 played Earth Office
    p1.playProject("EarthOffice", 1)

    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.cardAction2("RotatorImpacts").expect("VenusStep, TR<P2>")
    // Player2 ended turn

    // Player1 played Development Center
    p1.playProject("DevelopmentCenter", 1, steel = 5)
    // Player1 used Power Plant:SP standard project
    p1.stdProject("PowerPlantSP")

    // Player2 passed
    p2.pass()

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Optimal Aerobraking
    p1.cardAction1("DevelopmentCenter").expect("-E, ProjectCard")
    // Player1 played Investment Loan
    // Player1's megacredits production decreased by 1
    // Player1's megacredits amount increased by 10
    p1.playProject("InvestmentLoan", 0).expect("PROD[-1], 10")
    // Player1 played Deuterium Export
    p1.playProject("DeuteriumExport", 11)
    // Player1 used Deuterium Export action
    p1.cardAction1("DeuteriumExport")
    // Player1 passed
    p1.pass()

    // Generation 3
    // Player1 bought 2 card(s)
    // You drew Spin-Inducing Asteroid and Imported GHG
    // Player2 bought 2 card(s)
    // You drew Asteroid and Trans-Neptune Probe
    engine.nextGeneration(2, 2)

    with(p1) {
      assertProds(4 to "M", 3 to "S", 0 to "T", 0 to "P", 1 to "E", 0 to "H")
      assertCounts(27 to "M", 3 to "S", 0 to "T", 0 to "P", 1 to "E", 1 to "H")
      assertDashMiddle(played = 10, actions = 3, vp = 23, tr = 23, hand = 7)
      assertTags(3 to "BUT", 1 to "SPT", 2 to "SCT", 1 to "POT", 3 to "EAT", 1 to "VET")
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    with(p2) {
      assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 0 to "E", 3 to "H")
      assertCounts(21 to "M", 1 to "S", 4 to "T", 3 to "P", 0 to "E", 6 to "H")
      assertDashMiddle(played = 8, actions = 2, vp = 22, tr = 21, hand = 7)
      assertTags(3 to "BUT", 1 to "SPT", 2 to "SCT", 1 to "POT", 1 to "JOT", 1 to "PLT", 1 to "MIT")
      assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 0, venus = 2)

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Venus Waystation
    p1.cardAction1("DevelopmentCenter")
    // Player1 used Inventors' Guild action
    p1.cardAction1("InventorsGuild") {
      // Player1 bought 0 card(s)
      // You drew no cards
      doFirstTask("Ok")
    }

    // Player2 used Factorum action
    // Player2's energy production increased by 1
    p2.cardAction1("Factorum").expect("PROD[E<P2>]")
    // Player2 played Asteroid
    // Player2's titanium amount increased by 2
    p2.playProject("AsteroidCard", 2, steel = 0, titanium = 4) {
      doFirstTask("Ok") // TODO couldn't steal from anyone anyway...
    }

    // Player1 played Corporate Stronghold
    // Player1's megacredits production increased by 3
    // Player1's energy production decreased by 1
    // Player1 placed city tile on row 4 position 6
    // Player1's plants amount increased by 1
    p1.playProject("CorporateStronghold", 5, steel = 3) { doTask("CityTile<Tharsis_4_6>") }
        .expect("PROD[3, -E], -2, Plant<P1>")
    // Player1 played Optimal Aerobraking
    p1.playProject("OptimalAerobraking", 7)

    // Player2 played Trans-Neptune Probe
    // Player2 is using their Mars University effect to draw a card by discarding a card.
    // You discarded Virus
    // Player2 drew 1 card(s)
    // You drew Local Heat Trapping
    p2.playProject("TransNeptuneProbe", 0, titanium = 2)
    // Player2 used Rotator Impacts action
    p2.cardAction1("RotatorImpacts") {
      doFirstTask("6 Pay<Class<M>> FROM M")
      // Player2 added 1 asteroid(s) to Rotator Impacts
      doFirstTask("Ok") // titanium, ugh
    }

    // Player1 used Deuterium Export action
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    // Player1's energy production increased by 1
    p1.cardAction2("DeuteriumExport").expect("PROD[E]")
    // Player1 played Imported GHG
    // Player1's heat production increased by 1
    // Player1's heat amount increased by 3
    // Player1's megacredits amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    p1.playProject("ImportedGhg", 4).expect("7 Heat<P1>, PlayedEvent<P1>")

    // Player2 passed
    p2.pass()

    // Player1 passed
    p1.pass()

    // Generation 4
    // Player1 bought 1 card(s)
    // You drew Tectonic Stress Power
    // Player2 bought 2 card(s)
    // You drew Search For Life and Greenhouses
    engine.nextGeneration(1, 2)

    with(p1) {
      assertProds(7 to "M", 3 to "S", 0 to "T", 0 to "P", 1 to "E", 1 to "H")
      assertCounts(44 to "M", 3 to "S", 0 to "T", 1 to "P", 1 to "E", 10 to "H")
      assertDashMiddle(played = 13, actions = 3, vp = 21, tr = 23, hand = 6)
      assertTags(4 to "BUT", 2 to "SPT", 2 to "SCT", 1 to "POT", 3 to "EAT", 1 to "VET", 1 to "CIT")
      assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 1 to "CityTile")
    }

    with(p2) {
      assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 1 to "E", 3 to "H")
      assertCounts(29 to "M", 2 to "S", 1 to "T", 4 to "P", 1 to "E", 9 to "H")
      assertDashMiddle(played = 10, actions = 2, vp = 24, tr = 22, hand = 7)
      assertTags(3 to "BUT", 2 to "SPT", 3 to "SCT", 1 to "POT", 1 to "JOT", 1 to "PLT", 1 to "MIT")
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(gen = 4, temp = -28, oxygen = 0, oceans = 0, venus = 2)

    // Player2 used Factorum action
    // Player2 drew Jovian Embassy
    p2.cardAction2("Factorum")
    // Player2 played Aquifer Pumping
    p2.playProject("AquiferPumping", 14, steel = 2)

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Phobos Space Haven
    p1.cardAction1("DevelopmentCenter")
    // Player1 used Inventors' Guild action
    p1.cardAction1("InventorsGuild") {
      // Player1 bought 1 card(s)
      // You drew Olympus Conference
      doTask("BuyCard")
    }

    // Player2 used Aquifer Pumping action
    p2.cardAction1("AquiferPumping") {
      doTask("8 Pay<Class<M>> FROM M")
      // Player2 placed ocean tile on row 2 position 6
      // Player2 drew 2 card(s)
      // You drew Deimos Down:promo and Kelp Farming
      // Player2 gained 2 plants from Arctic Algae
      doTask("OceanTile<Tharsis_2_6>")
      doTask("Ok") // stupid steel
    }
    // Player2 played Search For Life
    // Player2 is using their Mars University effect to draw a card by discarding a card.
    // You discarded Jovian Embassy
    // Player2 drew 1 card(s)
    // You drew Local Shading
    p2.playProject("SearchForLife", 3)

    // Player1 used Deuterium Export action
    p1.cardAction1("DeuteriumExport")
    // Player1 played Tectonic Stress Power
    // Player1's energy production increased by 3
    p1.playProject("TectonicStressPower", 12, steel = 3)

    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.cardAction2("RotatorImpacts")
    // Player2 used Search For Life action
    p2.cardAction1("SearchForLife") {
      // Player2 revealed and discarded Cartel
      doTask("Ok")
    }.expect("-1")

    // Player1 used Convert Heat standard action
    p1.stdAction("ConvertHeatSA")
    // Player1 used Asteroid:SP standard project
    // Player1's heat production increased by 1
    p1.stdProject("AsteroidSP")

    // Player2 passed
    p2.pass()

    // Player1 used Sell Patents standard project
    // Player1 sold 1 patents
    p1.sellPatents(1).expect("-ProjectCard, 1")
    // Player1 played Spin-Inducing Asteroid
    // Player1 drew 1 card(s)
    // You drew Lagrange Observatory
    // Player1's megacredits amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    p1.playProject("SpinInducingAsteroid", 16).expect("3 Heat, -13")

    // Player1 passed
    p1.pass()

    // Generation 5
    // Player1 bought 3 card(s)
    // You drew Small Asteroid, Fueled Generators and Domed Crater
    // Player2 bought 3 card(s)
    // You drew Power Supply Consortium, Directed Impactors and Power Plant
    engine.nextGeneration(3, 3)

    with(p1) {
      assertProds(7 to "M", 3 to "S", 0 to "T", 0 to "P", 4 to "E", 2 to "H")
      assertCounts(28 to "M", 3 to "S", 0 to "T", 1 to "P", 4 to "E", 11 to "H")
      assertDashMiddle(played = 15, actions = 3, vp = 26, tr = 27, hand = 9)
      assertTags(5 to "BUT", 2 to "SPT", 2 to "SCT", 2 to "POT", 3 to "EAT", 1 to "VET", 1 to "CIT")
      assertCounts(3 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 1 to "CityTile")
    }

    with(p2) {
      assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 1 to "E", 3 to "H")
      assertCounts(15 to "M", 1 to "S", 2 to "T", 7 to "P", 1 to "E", 13 to "H")
      assertDashMiddle(played = 12, actions = 4, vp = 26, tr = 24, hand = 11)
      assertTags(4 to "BUT", 2 to "SPT", 4 to "SCT", 1 to "POT", 1 to "JOT", 1 to "PLT", 1 to "MIT")
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(gen = 5, temp = -24, oxygen = 0, oceans = 1, venus = 8)

    checkSummaryAfterGen4(game)

    // Player1 used Convert Heat standard action
    p1.stdAction("ConvertHeatSA")
    // Player1 played Small Asteroid
    // Player1's heat production increased by 1
    // Player1's megacredits amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    p1.playProject("SmallAsteroid", 10) {
          // Player2's plants amount decreased by 2 by Player1
          doTask("-2 Plant<P2>")
        }
        .expect("TemperatureStep, -2 Plant<P2>")

    // Player2 used Factorum action
    // 1 card(s) were discarded
    // Player2 drew AI Central
    p2.cardAction2("Factorum")
    // Player2 played Directed Impactors
    p2.playProject("DirectedImpactors", 2, titanium = 2)

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Project Inspection
    p1.cardAction1("DevelopmentCenter")
    // Player1 used Inventors' Guild action
    p1.cardAction1("InventorsGuild") {
      // Player1 bought 0 card(s)
      // You drew no cards
      doTask("Ok")
    }

    // Player2 used Sell Patents standard project
    // Player2 sold 1 patents
    p2.sellPatents(1)
    // Player2 used Sell Patents standard project
    // Player2 sold 1 patents
    p2.sellPatents(1)

    // Player1 used Deuterium Export action
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    // Player1's energy production increased by 1
    p1.cardAction2("DeuteriumExport")
    // Player1 played Domed Crater
    // Player1's megacredits production increased by 3
    // Player1's energy production decreased by 1
    // Player1's plants amount increased by 3
    p1.playProject("DomedCrater", 18, steel = 3) {
      // Player1 placed city tile on row 3 position 4
      doTask("CityTile<Tharsis_3_4>")
    }

    // Player2 used Directed Impactors action
    p2.cardAction1("DirectedImpactors") {
      // Player2 added 1 asteroid(s) to Rotator Impacts
      doTask("6 Pay<Class<M>> FROM M")
      doTask("Ok") // titanium
      doTask("Asteroid<RotatorImpacts>")
    }
    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.cardAction2("RotatorImpacts")

    // Player1 played Fueled Generators
    // Player1's megacredits production decreased by 1
    // Player1's energy production increased by 1
    p1.playProject("FueledGenerators", 1).expect("PROD[-1, E], E")
    // Player1 ended turn

    // Player2 used Convert Heat standard action
    p2.stdAction("ConvertHeatSA")

    // Player2 used Aquifer Pumping action
    // Player2 placed ocean tile on row 1 position 4
    // Player2 drew 1 card(s)
    // You drew Bushes
    // Player2 gained 2 plants from Arctic Algae
    p2.cardAction1("AquiferPumping") {
      doTask("6 Pay<Class<M>> FROM M")
      doTask("Pay<Class<S>> FROM S")
      doTask("OceanTile<Tharsis_1_4>")
    }.expect("ProjectCard<P2>, 2 Plant<P2>")

    // Player1 passed
    p1.pass()

    // Player2 passed
    p2.pass()

    // Generation 6
    // Player1 bought 4 card(s)
    // You drew Sister Planet Support, Miranda Resort, Solarnet and Dusk Laser Mining
    // Player2 bought 2 card(s)
    // You drew Bio Printing Facility and Earth Catapult
    engine.nextGeneration(4, 2)

    with(p1) {
      assertProds(9 to "M", 3 to "S", 0 to "T", 0 to "P", 5 to "E", 3 to "H")
      assertCounts(31 to "M", 3 to "S", 0 to "T", 4 to "P", 5 to "E", 15 to "H")
      assertDashMiddle(played = 18, actions = 3, vp = 29, tr = 29, hand = 11)
      assertTags(7 to "BUT", 2 to "SPT", 2 to "SCT", 3 to "POT", 3 to "EAT", 1 to "VET", 2 to "CIT")
      assertCounts(4 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProds(0 to "M", 1 to "S", 1 to "T", 1 to "P", 1 to "E", 3 to "H")
      assertCounts(21 to "M", 1 to "S", 1 to "T", 8 to "P", 1 to "E", 9 to "H")
      assertDashMiddle(played = 13, actions = 5, vp = 29, tr = 27, hand = 12)
      assertTags(4 to "BUT", 3 to "SPT", 4 to "SCT", 1 to "POT", 1 to "JOT", 1 to "PLT", 1 to "MIT")
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(gen = 6, temp = -18, oxygen = 0, oceans = 2, venus = 10)

    // TODO this *should* work, but doesn't, so we have to fake it.
    assertThrows<DependencyException> { p2.stdAction("ConvertPlantsSA") }
    with(p2.godMode()) {
      // Player2 used Convert Plants standard action
      // Player2 placed greenery tile on row 8 position 4
      // Player2 drew 1 card(s)
      // You drew Medical Lab
      manual("-8 Plant THEN GreeneryTile<Tharsis_8_7>").expect("ProjectCard") // r-5+c
    }
    // Player2 used Factorum action
    // 3 card(s) were discarded
    // Player2 drew Mine
    p2.cardAction2("Factorum").expect("ProjectCard")

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Large Convoy
    p1.cardAction1("DevelopmentCenter")

    // Player1 used Inventors' Guild action
    p1.cardAction1("InventorsGuild") {
      // Player1 bought 1 card(s)
      // You drew Mining Quota
      doTask("BuyCard")
    }

    // Player2 played Power Plant
    // Player2's energy production increased by 1
    p2.playProject("PowerPlantCard", 2, steel = 1)
    // Player2 used Aquifer Pumping action
    p2.cardAction1("AquiferPumping") {
      doTask("8 Pay<Class<M>> FROM M")
      doTask("Ok") // stupid steel
      // Player2 placed ocean tile on row 1 position 5
      // Player2 gained 2 plants from Arctic Algae
      doTask("OceanTile<Tharsis_1_5>")
    }.expect("-4, 2 Plant") // 4 back from adjacent oceans

    // Player1 played Olympus Conference
    p1.playProject("OlympusConference", 1, steel = 3).expect("Science<OlympusConference>")
    // Player1 played Sister Planet Support
    // Player1's megacredits production increased by 3
    p1.playProject("SisterPlanetSupport", 4).expect("PROD[3], -1")

    // Player2 used Directed Impactors action
    p2.cardAction1("DirectedImpactors") {
      doTask("3 Pay<Class<M>> FROM M")
      doTask("1 Pay<Class<T>> FROM T")
      // Player2 added 1 asteroid(s) to Rotator Impacts
      doTask("Asteroid<RotatorImpacts>")
    }
    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.cardAction2("RotatorImpacts").expect("VenusStep, TR<P2>")

    // Player1 played Dusk Laser Mining
    // Player1's titanium production increased by 1
    // Player1's energy production decreased by 1
    // Player1's titanium amount increased by 4
    p1.playProject("DuskLaserMining", 8).expect("PROD[T, -E], 5 T")
    // Player1 played Miranda Resort
    // Player1's megacredits production increased by 5
    p1.playProject("MirandaResort", titanium = 4).expect("PROD[5], 5")

    // Player2 played Mine
    // Player2's steel production increased by 1
    p2.playProject("Mine", 4)
    // Player2 used Search For Life action
    p2.cardAction1("SearchForLife") {
      // Player2 revealed and discarded Comet
      doTask("Ok")
    }

    // Player1 played Solarnet
    // Player1 drew 2 card(s)
    // You drew Security Fleet and Outdoor Sports
    p1.playProject("Solarnet", 7).expect("ProjectCard") // gained 2 but removed 1!
    // Player1 played Mining Quota
    // Player1's steel production increased by 2
    p1.playProject("MiningQuota", 5)

    // Player2 used Convert Heat standard action
    p2.stdAction("ConvertHeatSA")
    // Player2 passed
    p2.pass()

    // Player1 used Convert Heat standard action
    p1.stdAction("ConvertHeatSA")
    // Player1 used Deuterium Export action
    p1.cardAction1("DeuteriumExport")
    // Player1 played Lagrange Observatory
    // Player1 drew 1 card(s)
    // You drew Venus Governor
    // Player1 removed 1 resource(s) from Player1's Olympus Conference
    // Player1 drew 1 card(s)
    // You drew Power Infrastructure
    p1.playProject("LagrangeObservatory", 6, titanium = 1) {
      doTask("ProjectCard FROM Science<OlympusConference>") // I don't have to choose the card
    }.expect("ProjectCard<P1>") // -1 played, +1 from card itself, +1 from olympus
    // Player1 played Venus Governor
    // Player1's megacredits production increased by 2
    p1.playProject("VenusGovernor", 4).expect("2 VenusTag<P1>")
    // Player1 used Sell Patents standard project
    // Player1 sold 1 patents
    p1.sellPatents(1)
    // Player1 played Moss
    // Player1's plants production increased by 1
    p1.playProject("Moss", 4).expect("-4 Resource") // TODO actually trying to check `0 Plant`
    // Player1 passed
    p1.pass()

    // Generation 7
    // Player1 bought 3 card(s)
    // You drew Stratospheric Birds, Media Archives and Trees
    // Player2 bought 1 card(s)
    // You drew Invention Contest
    engine.nextGeneration(3, 1)

    with(p1) {
      assertProds(19 to "M", 5 to "S", 1 to "T", 1 to "P", 4 to "E", 3 to "H")
      assertCounts(40 to "M", 7 to "S", 1 to "T", 5 to "P", 4 to "E", 14 to "H")
      assertDashMiddle(played = 27, actions = 3, vp = 34, tr = 30, hand = 10)
      assertTags(
          9 to "BUT", 5 to "SPT", 4 to "SCT", 3 to "POT", 5 to "EAT", 1 to "JOT",
          4 to "VET", 1 to "PLT", 2 to "CIT")
      assertCounts(4 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProds(0 to "M", 2 to "S", 1 to "T", 1 to "P", 2 to "E", 3 to "H")
      assertCounts(32 to "M", 2 to "S", 1 to "T", 3 to "P", 2 to "E", 5 to "H")
      assertDashMiddle(played = 15, actions = 5, vp = 34, tr = 31, hand = 13)
      assertTags(6 to "BUT", 3 to "SPT", 4 to "SCT", 2 to "POT", 1 to "JOT", 1 to "PLT", 1 to "MIT")
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(gen = 7, temp = -14, oxygen = 1, oceans = 3, venus = 12)

    // Player1 claimed Builder milestone
    p1.stdAction("ClaimMilestoneSA") {
      doTask("Builder") // TODO sort of expected it to autopick
    }.expect("-8, Milestone")
    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Quantum Extractor
    p1.cardAction1("DevelopmentCenter")

    // Player2 played Earth Catapult
    p2.playProject("EarthCatapult", 23)
    // Player2 played Invention Contest
    // Player2 is using their Mars University effect to draw a card by discarding a card.
    // You discarded Gyropolis
    // Player2 drew 1 card(s)
    // You drew Titanium Mine
    // Player2 drew 1 card(s)
    // You drew Aerial Mappers
    p2.playProject("InventionContest", 0).expect("1 Card, 1 PlayedEvent") // no hand or table cards

    // Player1 used Inventors' Guild action
    p1.cardAction1("InventorsGuild") {
      // Player1 bought 0 card(s)
      // You drew no cards
      doTask("Ok")
    }
    // Player1 played Quantum Extractor
    // Player1's energy production increased by 4
    p1.playProject("QuantumExtractor", 13).expect("-13, PROD[4E], 4E")

    // Player2 played Bio Printing Facility
    p2.playProject("BioPrintingFacility", 1, steel = 2)
    // Player2 used Bio Printing Facility action
    // Player2's plants amount increased by 2
    p2.cardAction1("BioPrintingFacility").expect("-2E, 2P")

    // Player1 used Deuterium Export action
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    // Player1's energy production increased by 1
    p1.cardAction2("DeuteriumExport")
    // Player1 played Project Inspection
    // Player1 used Development Center action with Project Inspection
    // Player1 drew 1 card(s)
    // You drew Floating Habs
    p1.playProject("ProjectInspection", 0) {
      doTask("UseAction1<DevelopmentCenter>")
    }.expect("PlayedEvent, Card, -E")

    // Player2 used Factorum action
    // Player2's energy production increased by 1
    p2.cardAction1("Factorum").expect("PROD[E]")
    // Player2 played Power Supply Consortium
    p2.playProject("PowerSupplyConsortium", 3) {
      // Player1's energy production decreased by 1 stolen by Player2
      doTask("PROD[-E<P1>]")
    }

    // Player1 played Floating Habs
    p1.playProject("FloatingHabs", 5)
    // Player1 used Floating Habs action
    p1.cardAction1("FloatingHabs") {
      // Player1 added 1 floater(s) to Deuterium Export
      doTask("Floater<DeuteriumExport>")
    }.expect("-2, Floater")

    // Player2 played Titanium Mine
    // Player2's titanium production increased by 1
    p2.playProject("TitaniumMine", 5).expect("PROD[T], BuildingTag")
    // Player2 passed
    p2.pass().expect("Pass")

    // Player1 used Convert Heat standard action
    p1.stdAction("ConvertHeatSA").expect("-8H, TEMP, TR")
    // Player1 played Stratospheric Birds
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    p1.playProject("StratosphericBirds", 12).expect("-Floater<DeuteriumExport>")
    // Player1 used Stratospheric Birds action
    p1.cardAction1("StratosphericBirds").expect("Animal<StratosphericBirds>")
    // Player1 passed
    p1.pass()

    // Generation 8
    // Player1 bought 2 card(s)
    // You drew Sulphur Exports and Mohole Lake
    // Player2 bought 2 card(s)
    // You drew Advanced Alloys and Natural Preserve
    engine.nextGeneration(2, 2)

    with(p1) {
      assertProds(19 to "M", 5 to "S", 1 to "T", 1 to "P", 8 to "E", 3 to "H")
      assertCounts(44 to "M", 12 to "S", 2 to "T", 6 to "P", 8 to "E", 16 to "H")
      assertDashMiddle(played = 31, actions = 5, vp = 41, tr = 31, hand = 10)
      assertTags(
          9 to "BUT", 5 to "SPT", 5 to "SCT", 4 to "POT", 5 to "EAT", 1 to "JOT",
          6 to "VET", 1 to "PLT", 1 to "ANT", 2 to "CIT")
      assertCounts(5 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProds(0 to "M", 2 to "S", 2 to "T", 1 to "P", 4 to "E", 3 to "H")
      assertCounts(25 to "M", 2 to "S", 3 to "T", 6 to "P", 4 to "E", 8 to "H")
      assertDashMiddle(played = 20, actions = 6, vp = 36, tr = 31, hand = 11)
      assertTags(8 to "BUT", 3 to "SPT", 4 to "SCT", 3 to "POT", 1 to "EAT", 1 to "JOT",
          1 to "PLT", 1 to "MIT")
      assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(gen = 8, temp = -12, oxygen = 1, oceans = 3, venus = 12)

    // Player2 played Advanced Alloys
    // Player2 is using their Mars University effect to draw a card by discarding a card.
    // You discarded Medical Lab
    // Player2 drew 1 card(s)
    // You drew Aerosport Tournament
    p2.playProject("AdvancedAlloys", 7).expect("-1 ProjectCard")
    // Player2 played AI Central
    // Player2's energy production decreased by 1
    // Player2 is using their Mars University effect to draw a card by discarding a card.
    // You discarded Aerosport Tournament
    // Player2 drew 1 card(s)
    // You drew Ishtar Mining
    p2.playProject("AiCentral", 13, steel = 2)

    // Player1 played Extractor Balloons
    p1.playProject("ExtractorBalloons", 21).expect("-21")
    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Noctis Farming
    p1.cardAction1("DevelopmentCenter")

    p1.assertCounts(23 to "M")

    // Player2 used AI Central action
    // Player2 drew 2 card(s)
    // You drew Beam From A Thorium Asteroid and Harvest
    p2.cardAction1("AiCentral").expect("2 Card<P2>")
    // Player2 used Directed Impactors action
    p2.cardAction1("DirectedImpactors") {
      doTask("1 Pay<Class<T>> FROM T")
      doTask("2 Pay<Class<M>> FROM M")
      // Player2 added 1 asteroid(s) to Rotator Impacts
      doTask("Asteroid<RotatorImpacts>")
    }

    engine.assertCounts(6 to "VenusStep")

    // Player1 played Sulphur Exports
    // Player1's megacredits production increased by 8
    p1.playProject("SulphurExports", 13, titanium = 2).expect("PROD[8], -5, VenusStep")
    // Player1 used Extractor Balloons action
    // Player1 removed 2 resource(s) from Player1's Extractor Balloons
    // Player1 raised the Venus scale 1 step(s)
    p1.cardAction2("ExtractorBalloons").expect("2 TR<P1>")

    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.cardAction2("RotatorImpacts").expect("-Asteroid, VenusStep, TR<P2>")
    // Player2 played Ishtar Mining
    // Player2's titanium production increased by 1
    p2.playProject("IshtarMining", 3)

    // Player1 played Mohole Lake
    // Player1's plants amount increased by 3
    // Player1 placed ocean tile on row 5 position 5
    // Player1's plants amount increased by 2
    // Player2 gained 2 plants from Arctic Algae
    p1.playProject("MoholeLake", 7, steel = 12) {
      doTask("OceanTile<Tharsis_5_5>")
    }.expect("7 Plant, TEMP, 2 TR<P1>, -7")
    // Player1 claimed Terraformer milestone
    p1.stdAction("ClaimMilestoneSA") { doTask("Terraformer") }.expect("-8")

    // Player2 used Convert Heat standard action
    p2.stdAction("ConvertHeatSA")
    // Player2 used Convert Plants standard action
    // Player2 placed greenery tile on row 8 position 3
    // Player2 drew 1 card(s)
    // You drew Herbivores
    p2.stdAction("ConvertPlantsSA") {
      doTask("GreeneryTile<Tharsis_8_6>") // r+c-5
    }.expect("-8 Plant, Card")

    // Player1 used Inventors' Guild action
    p1.cardAction1("InventorsGuild") {
      // Player1 bought 1 card(s)
      // You drew Imported Nitrogen
      doTask("BuyCard")
    }
    // Player1 used Deuterium Export action
    p1.cardAction1("DeuteriumExport").expect("Floater")

    // Player2 used Bio Printing Facility action
    // Player2's plants amount increased by 2
    p2.cardAction1("BioPrintingFacility") {
      // TODO how would I choose 0 animals instead? Senseless move, but legal.
      doTask("2 Plant")
    }.expect("2 Plant, -2 E")
    // Player2 passed
    p2.pass()

    // Player1 used Convert Heat standard action
    p1.stdAction("ConvertHeatSA")
    // Player1 used Convert Plants standard action
    p1.stdAction("ConvertPlantsSA") {
      // Player1 placed greenery tile on row 3 position 5
      doTask("GreeneryTile<Tharsis_3_5>")
    }

    // Player1 used Stratospheric Birds action
    p1.cardAction1("StratosphericBirds").expect("Animal")
    // Player1 used Mohole Lake action
    // Player1 added 1 animal(s) to Stratospheric Birds
    p1.cardAction1("MoholeLake") {
      doTask("Animal<StratosphericBirds>") // TODO should this have autoexecuted?
    }.expect("Animal<StratosphericBirds>")
    // Player1 passed
    p1.pass()

    // Generation 9
    // Player1 bought 3 card(s)
    // You drew Rego Plastics, SF Memorial and Water to Venus
    // Player2 bought 2 card(s)
    // You drew Atalanta Planitia Lab and Mining Expedition
    engine.nextGeneration(3, 2)

    with(p1) {
      assertProds(27 to "M", 5 to "S", 1 to "T", 1 to "P", 8 to "E", 3 to "H")
      assertCounts(56 to "M", 5 to "S", 1 to "T", 4 to "P", 8 to "E", 18 to "H")
      assertDashMiddle(played = 34, actions = 7, vp = 58, tr = 38, hand = 12)
      assertTags(10 to "BUT", 6 to "SPT", 5 to "SCT", 4 to "POT", 5 to "EAT",
          1 to "JOT", 8 to "VET", 1 to "PLT", 1 to "ANT", 2 to "CIT")
      assertCounts(5 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProds(0 to "M", 2 to "S", 3 to "T", 1 to "P", 3 to "E", 3 to "H")
      assertCounts(28 to "M", 2 to "S", 5 to "T", 3 to "P", 3 to "E", 5 to "H")
      assertDashMiddle(played = 23, actions = 7, vp = 41, tr = 34, hand = 13)
      assertTags(9 to "BUT", 3 to "SPT", 6 to "SCT", 3 to "POT", 1 to "EAT",
          1 to "JOT", 1 to "VET", 1 to "PLT", 1 to "MIT")
      assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(gen = 9, temp = -6, oxygen = 3, oceans = 4, venus = 18)

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Venusian Insects
    p1.cardAction1("DevelopmentCenter")
    // Player1 used Inventors' Guild action
    p1.cardAction1("InventorsGuild") {
      // Player1 bought 1 card(s)
      // You drew Urbanized Area
      doTask("BuyCard")
    }

    // Player2 played Deimos Down:promo
    // Player2's steel amount increased by 4
    p2.playProject("DeimosDownPromo", 9, titanium = 5) {
      // Player2 placed ocean tile on row 6 position 6
      // Player2's plants amount increased by 1
      p2.doTask("OceanTile<Tharsis_6_7>")
      // Player2 placed Deimos Down tile on row 2 position 5
      p2.doTask("DdTile<Tharsis_2_5>")
      // Player1's plants amount decreased by 4 by Player2
      p2.doTask("-4 Plant<P1>")
      // Player2 gained 2 plants from Arctic Algae
    }
    // Player2 used AI Central action
    // Player2 drew 2 card(s)
    // You drew Ecological Zone and Biomass Combustors
    p2.cardAction1("AiCentral")

    // Player1 used Convert Heat standard action
    p1.stdAction("ConvertHeatSA")
    // Player1 used Convert Heat standard action
    p1.stdAction("ConvertHeatSA")

    // Player2 used Aquifer Pumping action
    p2.cardAction1("AquiferPumping") {
      doTask("3 Pay<Class<S>> FROM S")
      doTask("Ok")
      // Player2 placed ocean tile on row 5 position 6
      // Player2's plants amount increased by 2
      // Player2 gained 2 plants from Arctic Algae
      doTask("OceanTile<Tharsis_5_6>")
    }
    // Player2 used Convert Plants standard action
    p2.stdAction("ConvertPlantsSA") {
      // Player2 placed greenery tile on row 9 position 3
      doTask("GreeneryTile<Tharsis_9_7>")
    }

    // Player1 played Rego Plastics
    p1.playProject("RegoPlastics", 10)
    // Player1 played SF Memorial
    // Player1 drew 1 card(s)
    // You drew Advanced Ecosystems
    p1.playProject("SfMemorial", 1, steel = 2)

    // Player2 claimed Gardener milestone
    p2.stdAction("ClaimMilestoneSA") { doTask("Gardener") }
    // Player2 used Directed Impactors action
    p2.cardAction1("DirectedImpactors") {
      doTask("6 Pay<Class<M>> FROM M")
      // Player2 added 1 asteroid(s) to Rotator Impacts
      doTask("Asteroid<RotatorImpacts>")
      doTask("Ok")
    }

    // Player1 used Floating Habs action
    p1.cardAction1("FloatingHabs") {
      // Player1 added 1 floater(s) to Extractor Balloons
      doTask("Floater<ExtractorBalloons>")
    }
    // Player1 used Extractor Balloons action
    // Player1 removed 2 resource(s) from Player1's Extractor Balloons
    // Player1 raised the Venus scale 1 step(s)
    p1.cardAction2("ExtractorBalloons").expect("-2 Floater")

    // Player2 played Ecological Zone
    // Player2 added 2 animal(s) to Ecological Zone
    p2.playProject("EcologicalZone", 10) {
      // Player2 placed Ecological Zone tile on row 4 position 5
      // Player2's plants amount increased by 2
      doTask("EzTile<Tharsis_4_5>")
    }.expect("2 Animal, 2 Plant")

    // Player2 played Harvest
    // Player2's megacredits amount increased by 12
    // Player2 added 1 animal(s) to Ecological Zone
    p2.playProject("Harvest", 2).expect("10, Animal, PlayedEvent")

    // Player1 played Noctis Farming
    // Player1's megacredits production increased by 1
    // Player1's plants amount increased by 2
    p1.playProject("NoctisFarming", 1, steel = 3).expect("PROD[1], 2P")
    // Player1 used Deuterium Export action
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    // Player1's energy production increased by 1
    p1.cardAction2("DeuteriumExport").expect("-Floater, PROD[E]")

    // Player2 used Bio Printing Facility action
    p2.cardAction1("BioPrintingFacility") {
      // Player2 added 1 animal(s) to Ecological Zone
      doTask("Animal<EcologicalZone>")
    }
    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.cardAction2("RotatorImpacts")

    // Player1 used Mohole Lake action
    p1.cardAction1("MoholeLake") {
      // Player1 added 1 animal(s) to Stratospheric Birds
      doTask("Animal<StratosphericBirds>")
    }.expect("Animal")
    // Player1 used Stratospheric Birds action
    p1.cardAction1("StratosphericBirds").expect("Animal")

    // Player2 used Factorum action
    // 1 card(s) were discarded
    // Player2 drew Protected Valley
    p2.cardAction2("Factorum").expect("Card")
    // Player2 played Natural Preserve
    // Player2's megacredits production increased by 1
    // Player2 is using their Mars University effect to draw a card by discarding a card.
    // You discarded Herbivores
    // Player2 drew 1 card(s)
    // You drew Thermophiles
    p2.playProject("NaturalPreserve", 1, steel = 2) {
      // Player2 placed Natural Preserve tile on row 3 position 1
      // Player2 drew 1 card(s)
      // You drew Black Polar Dust
      doTask("NpTile<Tharsis_3_1>")
    }

    // Player1 used Sell Patents standard project
    // Player1 sold 3 patents
    p1.sellPatents(3)
    // Player1 played Water to Venus
    // Player1's megacredits amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    p1.playProject("WaterToVenus", 4, titanium = 1)

    // Player2 used Sell Patents standard project
    // Player2 sold 2 patents
    p2.sellPatents(2)
    // Player2 played Kelp Farming
    // Player2's megacredits production increased by 2
    // Player2's plants production increased by 3
    // Player2's plants amount increased by 2
    // Player2 added 1 animal(s) to Ecological Zone
    p2.playProject("KelpFarming", 15).expect("5 Production, 2 Plant, Animal")

    // Player1 played Trees
    // Player1's plants production increased by 3
    // Player1's plants amount increased by 1
    p1.playProject("Trees", 13)
    // Player1 funded Banker award
    p1.godMode().sneak("-8, 5 VP") // TODO

    // Player2 used Search For Life action
    p2.cardAction1("SearchForLife") {
      // Player2 revealed and discarded Fusion Power
      doTask("Ok")
    }
    // Player2 passed
    p2.pass()

    // Player1 played Venusian Insects
    p1.playProject("VenusianInsects", 5)
    // Player1 used Venusian Insects action
    p1.cardAction1("VenusianInsects")
    // Player1 funded Venuphile award
    p1.godMode().sneak("-14, 5 VP") // TODO
    // Player1 passed
    p1.pass()

    // Generation 10
    // Player1 bought 2 card(s)
    // You drew Nitrogen-Rich Asteroid and Lava Tube Settlement
    // Player2 bought 3 card(s)
    // You drew Mercurian Alloys, Hired Raiders and Nuclear Power
    engine.nextGeneration(2, 3)

    with(p1) {
      assertProds(28 to "M", 5 to "S", 1 to "T", 4 to "P", 9 to "E", 3 to "H")
      assertCounts(66 to "M", 5 to "S", 1 to "T", 10 to "P", 9 to "E", 16 to "H")
      assertDashMiddle(played = 40, actions = 8, vp = 78, tr = 42, hand = 8)
      assertTags(
          13 to "BUT", 6 to "SPT", 5 to "SCT", 4 to "POT", 5 to "EAT", 1 to "JOT",
          9 to "VET", 3 to "PLT", 1 to "MIT", 1 to "ANT", 2 to "CIT")
      assertCounts(6 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProds(3 to "M", 2 to "S", 3 to "T", 4 to "P", 3 to "E", 3 to "H")
      assertCounts(36 to "M", 3 to "S", 3 to "T", 10 to "P", 3 to "E", 9 to "H")
      assertDashMiddle(played = 28, actions = 7, vp = 58, tr = 41, hand = 13)
      assertTags(10 to "BUT", 3 to "SPT", 7 to "SCT", 3 to "POT", 1 to "EAT",
          1 to "JOT", 1 to "VET", 3 to "PLT", 1 to "MIT", 1 to "ANT")
      assertCounts(4 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    engine.assertSidebar(gen = 10, temp = 4, oxygen = 4, oceans = 6, venus = 24)

    val summ = Summarizer(game)
    assertThat(summ.net("Manutech", "Resource")).isEqualTo(92)
    assertThat(summ.net("Production<P2>", "Resource<P2>")).isEqualTo(129)

    assertThat(summ.net("EarthOffice", "Owed")).isEqualTo(-12)
    assertThat(summ.net("AdvancedAlloys<P2>", "Owed")).isEqualTo(-12)
    assertThat(summ.net("EarthCatapult<P2>", "Owed")).isEqualTo(-24)
    assertThat(summ.net("QuantumExtractor", "Owed")).isEqualTo(-4) // oof

    assertThat(summ.net("AquiferPumping", "OceanTile")).isEqualTo(4)
    assertThat(summ.net("ArcticAlgae", "Plant")).isEqualTo(13)
    assertThat(summ.net("OptimalAerobraking", "Resource")).isEqualTo(24)
    assertThat(summ.net("SearchForLife", "Megacredit")).isEqualTo(-3)
    assertThat(summ.net("SearchForLife", "Science")).isEqualTo(0)

    // This is just silly
    assertThat(summ.net("TR<P1>", "Megacredit<P1>")).isEqualTo(266)
    assertThat(summ.net("TR<P1>", "Megacredit")).isEqualTo(266)
    assertThat(summ.net("TR", "Megacredit<P1>")).isEqualTo(266)
    assertThat(summ.net("TR<P2>", "Megacredit<P2>")).isEqualTo(251)
    assertThat(summ.net("TR<P2>", "Megacredit")).isEqualTo(251)
    assertThat(summ.net("TR", "Megacredit<P2>")).isEqualTo(251)

    assertThat(summ.net("TR", "Megacredit")).isEqualTo(517)

    assertThat(summ.net("TR<P1>", "Megacredit<P2>")).isEqualTo(0)
    assertThat(summ.net("TR<P2>", "Megacredit<P1>")).isEqualTo(0)

    // Player2 played Hired Raiders
    // Player1's steel amount decreased by 2 stolen by Player2
    // Player2 used Convert Heat standard action
    // Player1 used Convert Heat standard action
    // Player1 used City standard project
    // Player1 placed city tile on row 7 position 4
    // Player2 used Convert Plants standard action
    // Player2 placed greenery tile on row 9 position 2
    // Player2's steel amount increased by 2
    // Player2 used AI Central action
    // Player2 drew 2 card(s)
    // You drew Energy Tapping and Wave Power
    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Energy Saving
    // Player1 used Inventors' Guild action
    // Player1 bought 0 card(s)
    // You drew no cards
    // Player2 played Mercurian Alloys
    // Player2 played Aerial Mappers
    // Player1 played Lava Tube Settlement
    // Player1's megacredits production increased by 2
    // Player1's energy production decreased by 1
    // Player1 placed city tile on row 2 position 2
    // Player1's steel amount increased by 1
    // Player1 played Urbanized Area
    // Player1's megacredits production increased by 2
    // Player1's energy production decreased by 1
    // Player1 placed city tile on row 2 position 3
    // Player2 played Atmoscoop
    // Player2 added 2 floater(s) to Aerial Mappers
    // Player2 used Aerial Mappers action
    // Player2 removed 1 resource(s) from Player2's Aerial Mappers
    // Player2 drew 1 card(s)
    // You drew Magnetic Field Generators:promo
    // Player1 played Nitrogen-Rich Asteroid
    // Player1's plants production increased by 4
    // Player1's megacredits amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    // Player1 used Convert Plants standard action
    // Player1 placed greenery tile on row 3 position 3
    // Player2 used Bio Printing Facility action
    // Player2 added 1 animal(s) to Ecological Zone
    // Player2 used Directed Impactors action
    // Player2 added 1 asteroid(s) to Rotator Impacts
    // Player1 used Venusian Insects action
    // Player1 used Stratospheric Birds action
    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    // Player2 used Aquifer Pumping action
    // Player2 placed ocean tile on row 9 position 5
    // Player2's titanium amount increased by 2
    // Player2 gained 2 plants from Arctic Algae
    // Player1 played Power Infrastructure
    // Player1 used Power Infrastructure action
    // Player1's megacredits amount increased by 8
    // Player2 used Factorum action
    // Player2 drew Electro Catapult
    // Player2 used Sell Patents standard project
    // Player2 sold 2 patents
    // Player1 used Deuterium Export action
    // Player1 used Extractor Balloons action
    // Player1 added 1 floater(s) to Extractor Balloons
    // Player2 played Bushes
    // Player2's plants production increased by 2
    // Player2's plants amount increased by 2
    // Player2 added 1 animal(s) to Ecological Zone
    // Player2 played Energy Tapping
    // Player1's energy production decreased by 1 stolen by Player2
    // Player1 used Floating Habs action
    // Player1 added 1 floater(s) to Floating Habs
    // Player1 used Mohole Lake action
    // Player1 added 1 animal(s) to Stratospheric Birds
    // Player2 played Nuclear Power
    // Player2's megacredits production decreased by 2
    // Player2's energy production increased by 3
    // Player2 played Biomass Combustors
    // Player2's energy production increased by 2
    // Player1's plants production decreased by 1 by Player2
    // Player1 passed
    // Player2 used Search For Life action
    // Player2 revealed and discarded Geothermal Power
    // Player2 passed
    // Generation 11
    // Player1 bought 2 card(s)
    // You drew Business Network and Gene Repair
    // Player2 bought 1 card(s)
    // You drew Towing A Comet
    // Player1 played Imported Nitrogen
    // Player1's plants amount increased by 4
    // Player1's megacredits amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    // Player1 added 3 microbe(s) to Venusian Insects
    // Player1 added 2 animal(s) to Stratospheric Birds
    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Peroxide Power
    // Player2 used AI Central action
    // Player2 drew 2 card(s)
    // You drew Media Group and Cloud Seeding
    // Player2 used Factorum action
    // 9 card(s) were discarded
    // Player2 drew Deep Well Heating
    // Player1 used Convert Plants standard action
    // Player1 placed greenery tile on row 2 position 4
    // Player1 used Inventors' Guild action
    // Player1 bought 0 card(s)
    // You drew no cards
    // Player2 played Media Group
    // Player2 played Mining Expedition
    // Player2's steel amount increased by 2
    // Player1's plants amount decreased by 2 by Player2
    // Player1 used Power Infrastructure action
    // Player1's megacredits amount increased by 5
    // Player1 used Extractor Balloons action
    // Player1 added 1 floater(s) to Extractor Balloons
    // Player2 used Bio Printing Facility action
    // Player2 added 1 animal(s) to Ecological Zone
    // Player2 used Aquifer Pumping action
    // Player2 placed ocean tile on row 5 position 4
    // Player2's plants amount increased by 2
    // Player2 gained 2 plants from Arctic Algae
    // Player1 played Business Network
    // Player1's megacredits production decreased by 1
    // Player1 used Business Network action
    // Player1 bought 1 card(s)
    // You drew Standard Technology
    // Player2 used City standard project
    // Player2 placed city tile on row 8 position 2
    // Player2 used Convert Plants standard action
    // Player2 placed greenery tile on row 8 position 1
    // Player2's steel amount increased by 2
    // Player1 used Deuterium Export action
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    // Player1's energy production increased by 1
    // Player1 used Floating Habs action
    // Player1 added 1 floater(s) to Floating Habs
    // Player2 used Convert Plants standard action
    // Player2 placed greenery tile on row 9 position 1
    // Player2's steel amount increased by 1
    // Player2 used Aerial Mappers action
    // Player2 removed 1 resource(s) from Player2's Aerial Mappers
    // Player2 drew 1 card(s)
    // You drew Penguins
    // Player1 used Stratospheric Birds action
    // Player1 used Mohole Lake action
    // Player1 added 1 animal(s) to Stratospheric Birds
    // Player2 played Magnetic Field Generators:promo
    // Player2's plants production increased by 2
    // Player2's energy production decreased by 4
    // Player2 placed Magnetic Field Generators tile on row 6 position 5
    // Player2's plants amount increased by 1
    // Player2 played Towing A Comet
    // Player2's plants amount increased by 2
    // Player2 placed ocean tile on row 6 position 7
    // Player2's plants amount increased by 1
    // Player2 gained 2 plants from Arctic Algae
    // Player1 used Venusian Insects action
    // Player1 played Standard Technology
    // Player1 removed 1 resource(s) from Player1's Olympus Conference
    // Player1 drew 1 card(s)
    // You drew Zeppelins
    // Player2 played Atalanta Planitia Lab
    // Player2 drew 2 card(s)
    // You drew House Printing and Robot Pollinators
    // Player2 is using their Mars University effect to draw a card by discarding a card.
    // You discarded Cloud Seeding
    // Player2 drew 1 card(s)
    // You drew Corroder Suits
    // Player2 used Sell Patents standard project
    // Player2 sold 3 patents
    // Player1 played Large Convoy
    // Player1 drew 2 card(s)
    // You drew Water Splitting Plant and Martian Survey
    // Player1's megacredits amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    // Player1 added 4 animal(s) to Stratospheric Birds
    // Player1 played Water Splitting Plant
    // Player2 played Robot Pollinators
    // Player2's plants production increased by 1
    // Player2's plants amount increased by 4
    // Player2 used Convert Plants standard action
    // Player2 placed greenery tile on row 7 position 2
    // Player1 played Media Archives
    // Player1's megacredits amount increased by 16
    // Player1 used Greenery standard project
    // Player1's megacredits amount increased by 3
    // Player1 placed greenery tile on row 5 position 7
    // Player1's plants amount increased by 2
    // Player2 played Greenhouses
    // Player2's plants amount increased by 6
    // Player2 added 1 animal(s) to Ecological Zone
    // Player2 used Convert Plants standard action
    // Player2 placed greenery tile on row 9 position 4
    // Player1 funded Thermalist award
    // Player1 used Convert Plants standard action
    // Player1 placed greenery tile on row 4 position 4
    // Player1's plants amount increased by 1
    // Player2 used Sell Patents standard project
    // Player2 sold 3 patents
    // Player2 played Penguins
    // Player2 added 1 animal(s) to Ecological Zone
    // Player1 played Advanced Ecosystems
    // Player1 used Sell Patents standard project
    // Player1 sold 4 patents
    // Player2 used Penguins action
    // Player2 passed
    // Player1 played Gene Repair
    // Player1's megacredits production increased by 2
    // Player1 passed
    // Final greenery placement
    // Player1 placed greenery tile on row 6 position 4
    // Player1's plants amount increased by 1
    // Player2 placed greenery tile on row 8 position 5
    // This game id was gf386a4cd5de1
  }

  private fun checkSummaryAfterGen4(game: Game) {
    val summer = Summarizer(game)

    // AA's effect has triggered once, plus the immediate plant
    assertThat(summer.net("ArcticAlgae", "Plant")).isEqualTo(3)

    // Blue has done 16 card buys: 5 initial, 8 in research, and 3 from inventors guild
    assertThat(summer.net("BuyCard<P1>", "Card<P1>")).isEqualTo(16)

    // DeuteriumExport produced a net of 1 floaters (made, consumed, made)
    assertThat(summer.net("DeuteriumExport", "Floater")).isEqualTo(1)
    assertThat(summer.net("DeuteriumExport", "Production<Class<Energy>>")).isEqualTo(1)

    // EarthOffice has saved blue 6 money (InvestmentLoan, ImportedGhg)
    assertThat(summer.net("EarthOffice", "Owed<P1>")).isEqualTo(-6)

    // Manutech has delivered! 1 MC with NewPartner, 4 with AlliedBank, 3 with CorporateStronghold
    // ... plus of course 35 at game start
    assertThat(summer.net("Manutech", "Megacredit<P1>")).isEqualTo(43)

    // Purple got 63 MC from TR (at production phases they had 20, 21, 22, and 24 TR)
    assertThat(summer.net("TerraformRating", "Megacredit<P2>")).isEqualTo(87)
    assertThat(summer.net("TerraformRating<P2>", "Megacredit")).isEqualTo(87)
    assertThat(summer.net("TerraformRating<P2>", "Megacredit<P2>")).isEqualTo(87)
    assertThat(summer.net("TerraformRating", "Megacredit")).isEqualTo(183)

    // Blue has raised temp 2 & venus 2, purple did temp & venus2 & ocean
    assertThat(summer.net("GlobalParameter", "TR<P1>")).isEqualTo(4)
    assertThat(summer.net("GlobalParameter", "TR<P2>")).isEqualTo(4)

    // This is ludicrous
    // assertThat(summer.net("Component", "Component")).isEqualTo(163)
  }

  private fun TfmGameplay.assertTags(vararg pair: Pair<Int, String>) {
    assertCounts(*pair)
    assertThat(this.count("Tag")).isEqualTo(pair.toList().sumOf { it.first })
  }

  private fun TfmGameplay.assertSidebar(gen: Int, temp: Int, oxygen: Int, oceans: Int, venus: Int) {
    assertCounts(gen to "Generation")
    assertThat(temperatureC()).isEqualTo(temp)
    assertThat(oxygenPercent()).isEqualTo(oxygen)
    assertCounts(oceans to "OceanTile")
    assertThat(venusPercent()).isEqualTo(venus)
  }

  private fun TfmGameplay.assertDashMiddle(played: Int, actions: Int, vp: Int, tr: Int, hand: Int) {
    assertCounts(hand to "ProjectCard", tr to "TR", played to "CardFront + PlayedEvent")
    assertActions(actions)
    assertVps(vp)
  }

  private fun TfmGameplay.assertVps(expected: Int) {
    phase("End") {
      assertCounts(expected to "VP")
      throw AbortOperationException()
    }
  }

  private fun TfmGameplay.assertActions(expected: Int) {
    assertThat(this.count("ActionCard") - this.count("ActionUsedMarker")).isEqualTo(expected)
  }
}
