package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.engine.World
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class Game20230521Test : AbstractFullGameTest() {

  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  override val config =
      GameConfig(
          """
          VenusNextExpansion, PreludeExpansion, PromoCardPack, TurmoilCardPack
          -WorldGovernmentOption
          """,
          "Player1",
          "Player2",
      )

  @Test
  internal fun game20230521() {
    TfmWorkflow.Auto(game).launch()

    // Good luck Player1!
    // Good luck Player2!
    // Generation 1

    // Player1's steel production increased by 1
    // Player1 played Manutech
    // Player1 kept 5 project cards
    p1.playCorp(Manutech, 5).expect("PROD[Steel], 20 MC, Steel, 5 ProjectCard")

    // Player2's steel production increased by 1
    // Player2 played Factorum
    // Player2 kept 4 project cards
    p2.playCorp(Factorum, 4).expect("PROD[Steel], 25 MC, 4 ProjectCard")

    p1.turn {
      // Player1 played New Partner
      // Player1's mc production increased by 1
      // You drew UNMI Contractor and Corporate Archives
      playPrelude(NewPartner) {
            // Player1 played UNMI Contractor
            // Player1 drew 1 card(s)
            // You drew Ganymede Colony
            playPrelude(UnmiContractor)
          }
          .expect("PROD[1 MC], 1 MC, ProjectCard, 3 TerraformRating")

      // Player1 played Allied Bank
      // Player1's mc production increased by 4
      // Player1's mc amount increased by 3
      playPrelude(AlliedBank).expect("PROD[4 MC], 7 MC, EarthTag")
    }

    p2.turn {
      // Player2 played Acquired Space Agency
      // Player2's titanium amount increased by 6
      // Player2 drew Rotator Impacts and Atmoscoop
      playPrelude(AcquiredSpaceAgency)
      // Player2 played Io Research Outpost
      // Player2's titanium production increased by 1
      // Player2 drew 1 card(s)
      // You drew Physics Complex
      playPrelude(IoResearchOutpost)
    }

    listOf(p1, p2).forEach { it.autoExecMode = SAFE }

    // Player1 played Inventors' Guild
    // Player1 ended turn
    p1.turn {
      playProject(InventorsGuild, 9)
    }

    // Player2 played Arctic Algae
    // Player2's plants amount increased by 1
    // Player2 ended turn
    p2.turn {
      playProject(ArcticAlgae, 12).expect("-12 MC, Plant, PlantTag")
    }

    // Player1 used Inventors' Guild action
    p1.turn {
      cardAction1(InventorsGuild) {
        // Player1 bought 1 card(s)
        // You drew Corporate Stronghold
        buyCards(1)
      }
    }
    // Player1 ended turn

    // Player2 used Factorum action
    // Player2's energy production increased by 1
    p2.turn { cardAction1(Factorum).expect("PROD[Energy]") }
    // Player2 ended turn

    // Player1 used Power Plant:SP standard project
    p1.turn {
      stdProject("PowerPlantSP")
      // Player1 played Building Industries
      // Player1's steel production increased by 2
      // Player1's energy production decreased by 1
      playProject(BuildingIndustries, 4, steel = 1) {
        doTask("PROD[-Energy]")
      }
    }

    // Player2 played Rotator Impacts
    p2.turn {
      playProject(RotatorImpacts, titanium = 2)
      // Player2 used Rotator Impacts action
      // Player2 added 1 asteroid(s) to Rotator Impacts
      cardAction1(RotatorImpacts) {
        pay(titanium = 2)
      }
    }

    // Player1 passed
    p1.pass()

    // Player2 played Carbonate Processing
    // Player2's energy production decreased by 1
    // Player2's heat production increased by 3
    p2.turn {
      playProject(CarbonateProcessing, 6) {
        doTask("PROD[-Energy]")
      }
      // Player2 played ArchaeBacteria
      // Player2's plants production increased by 1
      playProject(Archaebacteria, 6)
      // Player2 passed
      pass()
    }

    // Generation 2
    // Player1 bought 2 card(s)
    // You drew Investment Loan and Deuterium Export
    p1.buyCards(2)
    // Player2 bought 2 card(s)
    // You drew Mars University and Steelworks
    p2.buyCards(2)

    with(p1) {
      assertProduction(m = 5, s = 3, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 23, s = 5, t = 0, p = 0, e = 0, h = 1)
      assertDashMiddle(played = 6, actions = 1, vp = 23, tr = 23, hand = 7)
      assertTags(but = 2, sct = 1, eat = 2)
      assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    with(p2) {
      assertProduction(m = 0, s = 1, t = 1, p = 1, e = 0, h = 3)
      assertResources(m = 15, s = 1, t = 3, p = 2, e = 0, h = 3)
      assertDashMiddle(played = 7, actions = 2, vp = 20, tr = 20, hand = 5)
      assertTags(but = 2, spt = 1, sct = 1, pot = 1, jot = 1, plt = 1, mit = 1)
      assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 0, venus = 0)

    // Player2 used Factorum action
    // Player2 drew Gyropolis
    p2.turn {
      cardAction2(Factorum)
      // Player2 played Mars University
      playProject(MarsUniversity, 6, steel = 1) {
        // Player2 is using their Mars University effect to draw a card by discarding a card.
        // You discarded Physics Complex
        // Player2 drew 1 card(s)
        // You drew Virus
        doTask("-ProjectCard")
      }
    }

    // Player1 used Inventors' Guild action
    p1.turn {
      cardAction1(InventorsGuild) {
        // Player1 bought 1 card(s)
        // You drew Development Center
        buyCards(1)
      }
      // Player1 played Earth Office
      playProject(EarthOffice, 1)
    }

    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.turn { cardAction2(RotatorImpacts).expect("VenusStep, TerraformRating<Player2>") }
    // Player2 ended turn

    // Player1 played Development Center
    p1.turn {
      playProject(DevelopmentCenter, 1, steel = 5)
      // Player1 used Power Plant:SP standard project
      stdProject("PowerPlantSP")
    }

    // Player2 passed
    p2.pass()

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Optimal Aerobraking
    p1.turn {
      cardAction1(DevelopmentCenter).expect("-Energy, ProjectCard")
      // Player1 played Investment Loan
      // Player1's megacredits production decreased by 1
      // Player1's megacredits amount increased by 10
      playProject(InvestmentLoan, 0) {
            doTask("PROD[-MC]")
            doTask("10 MC")
          }
          .expect("PROD[-1 MC], 10 MC")
      // Player1 played Deuterium Export
      playProject(DeuteriumExport, 11)
      // Player1 used Deuterium Export action
      cardAction1(DeuteriumExport)
      // Player1 passed
      pass()
    }

    // Generation 3
    // Player1 bought 2 card(s)
    // You drew Spin-Inducing Asteroid and Imported GHG
    p1.buyCards(2)
    // Player2 bought 2 card(s)
    // You drew Asteroid and Trans-Neptune Probe
    p2.buyCards(2)

    with(p1) {
      assertProduction(m = 4, s = 3, t = 0, p = 0, e = 1, h = 0)
      assertResources(m = 27, s = 3, t = 0, p = 0, e = 1, h = 1)
      assertDashMiddle(played = 10, actions = 3, vp = 23, tr = 23, hand = 7)
      assertTags(but = 3, spt = 1, sct = 2, pot = 1, eat = 3, vet = 1)
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    with(p2) {
      assertProduction(m = 0, s = 1, t = 1, p = 1, e = 0, h = 3)
      assertResources(m = 21, s = 1, t = 4, p = 3, e = 0, h = 6)
      assertDashMiddle(played = 8, actions = 2, vp = 22, tr = 21, hand = 7)
      assertTags(but = 3, spt = 1, sct = 2, pot = 1, jot = 1, plt = 1, mit = 1)
      assertCounts(0 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 0, venus = 2)

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Venus Waystation
    p1.turn {
      cardAction1(DevelopmentCenter)
      // Player1 used Inventors' Guild action
      cardAction1(InventorsGuild) {
        // Player1 bought 0 card(s)
        // You drew no cards
        buyCards(0)
      }
    }

    // Player2 used Factorum action
    // Player2's energy production increased by 1
    p2.turn {
      cardAction1(Factorum).expect("PROD[Energy<Player2>]")
      // Player2 played Asteroid
      // Player2's titanium amount increased by 2
      playProject(
          AsteroidCard,
          2,
          steel = 0,
          titanium = 4,
      ) { /* Decline removing an opponent's plants. */
        declineTask()
        doTask("TemperatureStep")
        doTask("TerraformRating")
        doTask("2 Titanium")
      }
    }

    // Player1 played Corporate Stronghold
    // Player1's mc production increased by 3
    // Player1's energy production decreased by 1
    // Player1 placed city tile on row 4 position 6
    // Player1's plants amount increased by 1
    p1.turn {
      playProject(CorporateStronghold, 5, steel = 3) {
            doTask("PROD[3 MC]")
            doTask("3 MC")
            doTask("PROD[-Energy]")
            placeTile(4, 6)
          }
          .expect("PROD[3 MC, -Energy], -2 MC, Plant<Player1>")
      // Player1 played Optimal Aerobraking
      playProject(OptimalAerobraking, 7)
    }

    // Player2 played Trans-Neptune Probe
    p2.turn {
      playProject(TransNeptuneProbe, 0, titanium = 2) {
        // Player2 is using their Mars University effect to draw a card by discarding a card.
        // You discarded Virus
        // Player2 drew 1 card(s)
        // You drew Local Heat Trapping
        doTask("-ProjectCard")
      }
      // Player2 used Rotator Impacts action
      cardAction1(RotatorImpacts) {
        pay(6)
        // Player2 added 1 asteroid(s) to Rotator Impacts
      }
    }

    // Player1 used Deuterium Export action
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    // Player1's energy production increased by 1
    p1.turn {
      cardAction2(DeuteriumExport).expect("PROD[Energy]")
      // Player1 played Imported GHG
      // Player1's heat production increased by 1
      // Player1's heat amount increased by 3
      // Player1's mc amount increased by 3 by Optimal Aerobraking
      // Player1's heat amount increased by 3 by Optimal Aerobraking
      playProject(ImportedGhg, 4) {
            doTask("PROD[Heat]")
            doTask("Heat")
            doTask("3 Heat")
            doTask("3 MC")
            doTask("3 Heat")
          }
          .expect("7 Heat<Player1>, PlayedEvent<Player1>")
    }

    // Player2 passed
    p2.pass()

    // Player1 passed
    p1.pass()

    // Generation 4
    // Player1 bought 1 card(s)
    // You drew Tectonic Stress Power
    p1.buyCards(1)
    // Player2 bought 2 card(s)
    // You drew Search For Life and Greenhouses
    p2.buyCards(2)

    with(p1) {
      assertProduction(m = 7, s = 3, t = 0, p = 0, e = 1, h = 1)
      assertResources(m = 44, s = 3, t = 0, p = 1, e = 1, h = 10)
      assertDashMiddle(played = 13, actions = 3, vp = 21, tr = 23, hand = 6)
      assertTags(but = 4, spt = 2, sct = 2, pot = 1, eat = 3, vet = 1, cit = 1)
      assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 1 to "CityTile")
    }

    with(p2) {
      assertProduction(m = 0, s = 1, t = 1, p = 1, e = 1, h = 3)
      assertResources(m = 29, s = 2, t = 1, p = 4, e = 1, h = 9)
      assertDashMiddle(played = 10, actions = 2, vp = 24, tr = 22, hand = 7)
      assertTags(but = 3, spt = 2, sct = 3, pot = 1, jot = 1, plt = 1, mit = 1)
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    assertSidebar(gen = 4, temp = -28, oxygen = 0, oceans = 0, venus = 2)

    // Player2 used Factorum action
    // Player2 drew Jovian Embassy
    p2.turn {
      cardAction2(Factorum)
      // Player2 played Aquifer Pumping
      playProject(AquiferPumping, 14, steel = 2)
    }

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Phobos Space Haven
    p1.turn {
      cardAction1(DevelopmentCenter)
      // Player1 used Inventors' Guild action
      cardAction1(InventorsGuild) {
        // Player1 bought 1 card(s)
        // You drew Olympus Conference
        buyCards(1)
      }
    }

    // Player2 used Aquifer Pumping action
    p2.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        // Player2 placed ocean tile on row 2 position 6
        // Player2 drew 2 card(s)
        // You drew Deimos Down:promo and Kelp Farming
        // Player2 gained 2 plants from Arctic Algae
        placeTile(2, 6)
        doTask("TerraformRating")
        doTask("2 ProjectCard")
      }
      // Player2 played Search For Life
      playProject(SearchForLife, 3) {
        // Player2 is using their Mars University effect to draw a card by discarding a card.
        // You discarded Jovian Embassy
        // Player2 drew 1 card(s)
        // You drew Local Shading
        doTask("-ProjectCard")
      }
    }

    // Player1 used Deuterium Export action
    p1.turn {
      cardAction1(DeuteriumExport)
      // Player1 played Tectonic Stress Power
      // Player1's energy production increased by 3
      playProject(TectonicStressPower, 12, steel = 3)
    }

    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.turn {
      cardAction2(RotatorImpacts)
      // Player2 used Search For Life action
      cardAction1(SearchForLife) {
            // Player2 revealed and discarded Cartel
            // Decline the science resource.
            declineTask()
          }
          .expect("-1 MC")
    }

    // Player1 used Convert Heat standard action
    p1.turn {
      convertHeat()
      // Player1 used Asteroid:SP standard project
      // Player1's heat production increased by 1
      stdProject("AsteroidSP") { doTask("TerraformRating") }
    }

    // Player2 passed
    p2.pass()

    // Player1 used Sell Patents standard project
    // Player1 sold 1 patents
    p1.turn {
      sellPatents(1).expect("-ProjectCard, 1 MC")
      // Player1 played Spin-Inducing Asteroid
      // Player1 drew 1 card(s)
      // You drew Lagrange Observatory
      // Player1's mc amount increased by 3 by Optimal Aerobraking
      // Player1's heat amount increased by 3 by Optimal Aerobraking
      playProject(SpinInducingAsteroid, 16) {
            doTask("VenusStep")
            doTask("TerraformRating")
            doTask("VenusStep")
            doTask("TerraformRating")
            doTask("ProjectCard")
            doTask("3 MC")
            doTask("3 Heat")
          }
          .expect("3 Heat, -13 MC")

      // Player1 passed
      pass()
    }

    // Generation 5
    // Player1 bought 3 card(s)
    // You drew Small Asteroid, Fueled Generators and Domed Crater
    p1.buyCards(3)
    // Player2 bought 3 card(s)
    // You drew Power Supply Consortium, Directed Impactors and Power Plant
    p2.buyCards(3)
    with(p1) {
      assertProduction(m = 7, s = 3, t = 0, p = 0, e = 4, h = 2)
      assertResources(m = 28, s = 3, t = 0, p = 1, e = 4, h = 11)
      assertDashMiddle(played = 15, actions = 3, vp = 26, tr = 27, hand = 9)
      assertTags(but = 5, spt = 2, sct = 2, pot = 2, eat = 3, vet = 1, cit = 1)
      assertCounts(3 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 1 to "CityTile")
    }

    with(p2) {
      assertProduction(m = 0, s = 1, t = 1, p = 1, e = 1, h = 3)
      assertResources(m = 15, s = 1, t = 2, p = 7, e = 1, h = 13)
      assertDashMiddle(played = 12, actions = 4, vp = 26, tr = 24, hand = 11)
      assertTags(but = 4, spt = 2, sct = 4, pot = 1, jot = 1, plt = 1, mit = 1)
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    assertSidebar(gen = 5, temp = -24, oxygen = 0, oceans = 1, venus = 8)

    checkSummaryAfterGen4(game)

    // Player1 used Convert Heat standard action
    p1.turn {
      convertHeat()
      // Player1 played Small Asteroid
      // Player1's heat production increased by 1
      // Player1's mc amount increased by 3 by Optimal Aerobraking
      // Player1's heat amount increased by 3 by Optimal Aerobraking
      playProject(SmallAsteroid, 10) {
            // Player2's plants amount decreased by 2 by Player1
            doTask("-2 Plant<Player2>")
            doTask("TemperatureStep")
            doTask("3 MC")
            doTask("3 Heat")
            doTask("TerraformRating")
            doTask("PROD[Heat]")
            doTask("Heat")
          }
          .expect("TemperatureStep, -2 Plant<Player2>")
    }

    // Player2 used Factorum action
    // 1 card(s) were discarded
    // Player2 drew AI Central
    p2.turn {
      cardAction2(Factorum)
      // Player2 played Directed Impactors
      playProject(DirectedImpactors, 2, titanium = 2)
    }

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Project Inspection
    p1.turn {
      cardAction1(DevelopmentCenter)
      // Player1 used Inventors' Guild action
      cardAction1(InventorsGuild) {
        // Player1 bought 0 card(s)
        // You drew no cards
        buyCards(0)
      }
    }

    // Player2 used Sell Patents standard project
    // Player2 sold 1 patents
    p2.turn {
      sellPatents(1)
      // Player2 used Sell Patents standard project
      // Player2 sold 1 patents
      sellPatents(1)
    }

    // Player1 used Deuterium Export action
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    // Player1's energy production increased by 1
    p1.turn {
      cardAction2(DeuteriumExport)
      // Player1 played Domed Crater
      // Player1's mc production increased by 3
      // Player1's energy production decreased by 1
      // Player1's plants amount increased by 3
      playProject(DomedCrater, 18, steel = 3) {
        doTask("3 Plant")
        doTask("PROD[-Energy]")
        doTask("PROD[3 MC]")
        // Player1 placed city tile on row 3 position 4
        placeTile(3, 4)
      }
    }

    // Player2 used Directed Impactors action
    p2.turn {
      cardAction1(DirectedImpactors) {
        p2.pay(6)
        // Player2 added 1 asteroid(s) to Rotator Impacts
        addCardResources(RotatorImpacts)
      }
      // Player2 used Rotator Impacts action
      // Player2 removed 1 resource(s) from Player2's Rotator Impacts
      // Player2 removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts)
    }

    // Player1 played Fueled Generators
    // Player1's mc production decreased by 1
    // Player1's energy production increased by 1
    p1.turn {
      playProject(FueledGenerators, 1) {
            doTask("PROD[-MC]")
          }
          .expect("PROD[-1 MC, Energy], Energy")
    }
    // Player1 ended turn

    // Player2 used Convert Heat standard action
    p2.turn {
      convertHeat()

      // Player2 used Aquifer Pumping action
      // Player2 placed ocean tile on row 1 position 4
      // Player2 drew 1 card(s)
      // You drew Bushes
      // Player2 gained 2 plants from Arctic Algae
      cardAction1(AquiferPumping) {
        p2.pay(6, steel = 1)
        placeTile(1, 4)
        doTask("TerraformRating")
        doTask("ProjectCard")
      }
    }

    // Player1 passed
    p1.pass()

    // Player2 passed
    p2.pass()

    // Generation 6
    // Player1 bought 4 card(s)
    // You drew Sister Planet Support, Miranda Resort, Solarnet and Dusk Laser Mining
    p1.buyCards(4)
    // Player2 bought 2 card(s)
    // You drew Bio Printing Facility and Earth Catapult
    p2.buyCards(2)

    with(p1) {
      assertProduction(m = 9, s = 3, t = 0, p = 0, e = 5, h = 3)
      assertResources(m = 31, s = 3, t = 0, p = 4, e = 5, h = 15)
      assertDashMiddle(played = 18, actions = 3, vp = 29, tr = 29, hand = 11)
      assertTags(but = 7, spt = 2, sct = 2, pot = 3, eat = 3, vet = 1, cit = 2)
      assertCounts(4 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProduction(m = 0, s = 1, t = 1, p = 1, e = 1, h = 3)
      assertResources(m = 21, s = 1, t = 1, p = 8, e = 1, h = 9)
      assertDashMiddle(played = 13, actions = 5, vp = 29, tr = 27, hand = 12)
      assertTags(but = 4, spt = 3, sct = 4, pot = 1, jot = 1, plt = 1, mit = 1)
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    assertSidebar(gen = 6, temp = -18, oxygen = 0, oceans = 2, venus = 10)

    // Player2 used Convert Plants standard action
    p2.turn {
      convertPlants {
        // Player2 placed greenery tile on row 8 position 4
        // Player2 drew 1 card(s)
        // You drew Medical Lab
        placeTile(8, 7) // r-5 + c
        doTask("OxygenStep")
        doTask("TerraformRating")
      }
      // Player2 used Factorum action
      // 3 card(s) were discarded
      // Player2 drew Mine
      cardAction2(Factorum).expect("ProjectCard")
    }

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Large Convoy
    p1.turn {
      cardAction1(DevelopmentCenter)

      // Player1 used Inventors' Guild action
      cardAction1(InventorsGuild) {
        // Player1 bought 1 card(s)
        // You drew Mining Quota
        buyCards(1)
      }
    }

    // Player2 played Power Plant
    // Player2's energy production increased by 1
    p2.turn {
      playProject(PowerPlant, 2, steel = 1)
      // Player2 used Aquifer Pumping action
      cardAction1(AquiferPumping) {
        p2.pay(8)
        // Player2 placed ocean tile on row 1 position 5
        // Player2 gained 2 plants from Arctic Algae
        placeTile(1, 5)
        doTask("2 MC")
        doTask("2 MC")
        doTask("TerraformRating")
      }
    }

    // Player1 played Olympus Conference
    p1.turn {
      playProject(OlympusConference, 1, steel = 3).expect("Science<$OlympusConference>")
      // Player1 played Sister Planet Support
      // Player1's mc production increased by 3
      playProject(SisterPlanetSupport, 4).expect("PROD[3 MC], -1 MC")
    }

    // Player2 used Directed Impactors action
    p2.turn {
      cardAction1(DirectedImpactors) {
        p2.pay(3, titanium = 1)
        // Player2 added 1 asteroid(s) to Rotator Impacts
        addCardResources(RotatorImpacts)
      }
      // Player2 used Rotator Impacts action
      // Player2 removed 1 resource(s) from Player2's Rotator Impacts
      // Player2 removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts).expect("VenusStep, TerraformRating<Player2>")
    }

    // Player1 played Dusk Laser Mining
    // Player1's titanium production increased by 1
    // Player1's energy production decreased by 1
    // Player1's titanium amount increased by 4
    p1.turn {
      playProject(DuskLaserMining, 8) {
            doTask("4 Titanium")
            doTask("PROD[Titanium]")
            doTask("PROD[-Energy]")
          }
          .expect("PROD[Titanium, -Energy], 5 Titanium")
      // Player1 played Miranda Resort
      // Player1's mc production increased by 5
      playProject(MirandaResort, titanium = 4).expect("PROD[5 MC], 5 MC")
    }

    // Player2 played Mine
    // Player2's steel production increased by 1
    p2.turn {
      playProject(Mine, 4)
      // Player2 used Search For Life action
      cardAction1(SearchForLife) {
        // Player2 revealed and discarded Comet
        // Decline the science resource.
        declineTask()
      }
    }

    // Player1 played Solarnet
    // Player1 drew 2 card(s)
    // You drew Security Fleet and Outdoor Sports
    p1.turn {
      playProject(Solarnet, 7).expect("ProjectCard") // gained 2 but removed 1!
      // Player1 played Mining Quota
      // Player1's steel production increased by 2
      playProject(MiningQuota, 5)
    }

    // Player2 used Convert Heat standard action
    p2.turn {
      convertHeat()
    }

    // Player1 used Convert Heat standard action
    p1.turn {
      convertHeat()
      // Player1 used Deuterium Export action
      cardAction1(DeuteriumExport)
    }

    // Player2 passed
    p2.pass()
    // Player1 played Lagrange Observatory
    // Player1 drew 1 card(s)
    // You drew Venus Governor
    // Player1 removed 1 resource(s) from Player1's Olympus Conference
    // Player1 drew 1 card(s)
    // You drew Power Infrastructure
    p1.turn {
      playProject(LagrangeObservatory, 6, titanium = 1) {
            doTask(
                "ProjectCard FROM Science<$OlympusConference>"
            ) // I don't have to choose the card
          }
          .expect("ProjectCard<Player1>") // -1 played, +1 from card itself, +1 from olympus
      // Player1 played Venus Governor
      // Player1's mc production increased by 2
      playProject(VenusGovernor, 4).expect("2 VenusTag<Player1>")
      // Player1 used Sell Patents standard project
      // Player1 sold 1 patents
      sellPatents(1)
      // Player1 played Moss
      // Player1's plants production increased by 1
      playProject(Moss, 4) { doTask("-Plant") }.expect("-4 Resource")
      // Player1 passed
      pass()
    }

    // Generation 7
    // Player1 bought 3 card(s)
    // You drew Stratospheric Birds, Media Archives and Trees
    p1.buyCards(3)
    // Player2 bought 1 card(s)
    // You drew Invention Contest
    p2.buyCards(1)

    with(p1) {
      assertProduction(m = 19, s = 5, t = 1, p = 1, e = 4, h = 3)
      assertResources(m = 40, s = 7, t = 1, p = 5, e = 4, h = 14)
      assertDashMiddle(played = 27, actions = 3, vp = 34, tr = 30, hand = 10)
      assertTags(but = 9, spt = 5, sct = 4, pot = 3, eat = 5, jot = 1, vet = 4, plt = 1, cit = 2)
      assertCounts(4 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProduction(m = 0, s = 2, t = 1, p = 1, e = 2, h = 3)
      assertResources(m = 32, s = 2, t = 1, p = 3, e = 2, h = 5)
      assertDashMiddle(played = 15, actions = 5, vp = 34, tr = 31, hand = 13)
      assertTags(but = 6, spt = 3, sct = 4, pot = 2, jot = 1, plt = 1, mit = 1)
      assertCounts(1 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    assertSidebar(gen = 7, temp = -14, oxygen = 1, oceans = 3, venus = 12)

    // Player1 claimed Builder milestone
    p1.turn {
      stdAction("ClaimMilestoneSA") { doTask("Builder8") }.expect("Milestone")
      // Player1 used Development Center action
      // Player1 drew 1 card(s)
      // You drew Quantum Extractor
      cardAction1(DevelopmentCenter)
    }

    // Player2 played Earth Catapult
    p2.turn {
      playProject(EarthCatapult, 23)
      // Player2 played Invention Contest
      // Player2 drew 1 card(s)
      // You drew Aerial Mappers
      playProject(InventionContest, 0) {
            // Player2 is using their Mars University effect to draw a card by discarding a card.
            // You discarded Gyropolis
            // Player2 drew 1 card(s)
            // You drew Titanium Mine
            doTask("-ProjectCard")
            doTask("ProjectCard")
            doTask("ProjectCard")
          }
          .expect("1 Card, 1 PlayedEvent") // no hand or table cards
    }

    // Player1 used Inventors' Guild action
    p1.turn {
      cardAction1(InventorsGuild) {
        // Player1 bought 0 card(s)
        // You drew no cards
        buyCards(0)
      }
      // Player1 played Quantum Extractor
      // Player1's energy production increased by 4
      playProject(QuantumExtractor, 13) {
            // Decline spending an Olympus Conference science resource to draw a card.
            doTask("Science<$OlympusConference>")
          }
          .expect("-13 MC, PROD[4 Energy], 4 Energy")
    }

    // Player2 played Bio Printing Facility
    p2.turn {
      playProject(BioPrintingFacility, 1, steel = 2)
      // Player2 used Bio Printing Facility action
      // Player2's plants amount increased by 2
      cardAction1(BioPrintingFacility) { doTask("2 Plant") }.expect("-2 Energy, 2 Plant")
    }

    // Player1 used Deuterium Export action
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    // Player1's energy production increased by 1
    p1.turn {
      cardAction2(DeuteriumExport)
      // Player1 played Project Inspection
      // Player1 used Development Center action with Project Inspection
      // Player1 drew 1 card(s)
      // You drew Floating Habs
      playProject(ProjectInspection, 0) {
            doTask("UseAction<$DevelopmentCenter, First>")
            p1.pay(energy = 1)
            doTask("ProjectCard")
          }
          .expect("PlayedEvent, Card, -Energy")
    }

    // Player2 used Factorum action
    // Player2's energy production increased by 1
    p2.turn {
      cardAction1(Factorum).expect("PROD[Energy]")
      // Player2 played Power Supply Consortium
      playProject(PowerSupplyConsortium, 3) {
        // Player1's energy production decreased by 1 stolen by Player2
        doTask("PROD[-Energy<Player1>]")
      }
    }

    // Player1 played Floating Habs
    p1.turn {
      playProject(FloatingHabs, 5)
      // Player1 used Floating Habs action
      cardAction1(FloatingHabs) {
            // Player1 added 1 floater(s) to Deuterium Export
            addCardResources(DeuteriumExport)
          }
          .expect("-2 MC, Floater")
    }

    // Player2 played Titanium Mine
    // Player2's titanium production increased by 1
    p2.turn { playProject(TitaniumMine, 5).expect("PROD[Titanium], BuildingTag") }

    // Player1 used Convert Heat standard action
    p1.turn {
      convertHeat().expect("-8 Heat, TemperatureStep, TerraformRating")
      // Player1 played Stratospheric Birds
      // Player1 removed 1 resource(s) from Player1's Deuterium Export
      playProject(StratosphericBirds, 12).expect("-Floater<$DeuteriumExport>")
    }

    // Player2 passed
    p2.turn {
      pass().expect("Pass")
    }

    // Player1 used Stratospheric Birds action
    p1.turn {
      cardAction1(StratosphericBirds).expect("Animal<$StratosphericBirds>")
      // Player1 passed
      pass()
    }

    // Generation 8
    // Player1 bought 2 card(s)
    // You drew Sulphur Exports and Mohole Lake
    p1.buyCards(2)
    // Player2 bought 2 card(s)
    // You drew Advanced Alloys and Natural Preserve
    p2.buyCards(2)

    with(p1) {
      assertProduction(m = 19, s = 5, t = 1, p = 1, e = 8, h = 3)
      assertResources(m = 44, s = 12, t = 2, p = 6, e = 8, h = 16)
      assertDashMiddle(played = 31, actions = 5, vp = 41, tr = 31, hand = 10)
      // (but = 9, ...)
      assertTags(9, spt = 5, sct = 5, pot = 4, eat = 5, jot = 1, vet = 6, plt = 1, ant = 1, cit = 2)
      assertCounts(5 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProduction(m = 0, s = 2, t = 2, p = 1, e = 4, h = 3)
      assertResources(m = 25, s = 2, t = 3, p = 6, e = 4, h = 8)
      assertDashMiddle(played = 20, actions = 6, vp = 36, tr = 31, hand = 11)
      assertTags(but = 8, spt = 3, sct = 4, pot = 3, eat = 1, jot = 1, plt = 1, mit = 1)
      assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    assertSidebar(gen = 8, temp = -12, oxygen = 1, oceans = 3, venus = 12)

    // Player2 played Advanced Alloys
    p2.turn {
      playProject(AdvancedAlloys, 7) {
            // Player2 is using their Mars University effect to draw a card by discarding a card.
            // You discarded Medical Lab
            // Player2 drew 1 card(s)
            // You drew Aerosport Tournament
            doTask("-ProjectCard")
          }
          .expect("-1 ProjectCard")
      // Player2 played AI Central
      // Player2's energy production decreased by 1
      playProject(AiCentral, 13, steel = 2) {
        // Player2 is using their Mars University effect to draw a card by discarding a card.
        // You discarded Aerosport Tournament
        // Player2 drew 1 card(s)
        // You drew Ishtar Mining
        doTask("-ProjectCard")
        doTask("PROD[-Energy]")
      }
    }

    // Player1 played Extractor Balloons
    p1.turn {
      playProject(ExtractorBalloons, 21).expect("-21 MC")
      // Player1 used Development Center action
      // Player1 drew 1 card(s)
      // You drew Noctis Farming
      cardAction1(DevelopmentCenter)
    }

    p1.assertCounts(23 to "MC")

    // Player2 used AI Central action
    // Player2 drew 2 card(s)
    // You drew Beam From A Thorium Asteroid and Harvest
    p2.turn {
      cardAction1(AiCentral).expect("2 Card<Player2>")
      // Player2 used Directed Impactors action
      cardAction1(DirectedImpactors) {
        p2.pay(2, titanium = 1)
        // Player2 added 1 asteroid(s) to Rotator Impacts
        addCardResources(RotatorImpacts)
      }
    }

    engine.assertCounts(6 to "VenusStep")

    // Player1 played Sulphur Exports
    // Player1's mc production increased by 8
    p1.turn {
      playProject(SulphurExports, 13, titanium = 2) {
            doTask("VenusStep")
            doTask("TerraformRating")
          }
          .expect("PROD[8 MC], -5 MC, VenusStep")
      // Player1 used Extractor Balloons action
      // Player1 removed 2 resource(s) from Player1's Extractor Balloons
      // Player1 raised the Venus scale 1 step(s)
      cardAction2(ExtractorBalloons) { doTask("TerraformRating") }
          .expect("2 TerraformRating<Player1>")
    }

    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.turn {
      cardAction2(RotatorImpacts).expect("-Asteroid, VenusStep, TerraformRating<Player2>")
      // Player2 played Ishtar Mining
      // Player2's titanium production increased by 1
      playProject(IshtarMining, 3)
    }

    // Player1 played Mohole Lake
    // Player1's plants amount increased by 3
    // Player1 placed ocean tile on row 5 position 5
    // Player1's plants amount increased by 2
    // Player2 gained 2 plants from Arctic Algae
    p1.turn {
      playProject(MoholeLake, 7, steel = 12) {
            placeTile(5, 5)
            doTask("3 Plant")
            doTask("TemperatureStep")
            doTask("2 Plant<Player1>")
            doTask("TerraformRating")
            p2.doTask("2 Plant<Player2>")
          }
          .expect("5 Plant, 2 Plant<Player2>, TemperatureStep, 2 TerraformRating, -7 MC")
      // Player1 claimed Terraformer milestone
      stdAction("ClaimMilestoneSA") { doTask("Terraformer35") }.expect("-8 MC")
    }

    // Player2 used Convert Heat standard action
    p2.turn {
      convertHeat()
      // Player2 used Convert Plants standard action
      // Player2 placed greenery tile on row 8 position 3
      // Player2 drew 1 card(s)
      // You drew Herbivores
      convertPlants {
            placeTile(8, 6) // r+c-5
            doTask("OxygenStep")
            doTask("TerraformRating")
          }
          .expect("-8 Plant, Card")
    }

    // Player1 used Inventors' Guild action
    p1.turn {
      cardAction1(InventorsGuild) {
        // Player1 bought 1 card(s)
        // You drew Imported Nitrogen
        buyCards(1)
      }
      // Player1 used Deuterium Export action
      cardAction1(DeuteriumExport).expect("Floater")
    }

    // Player2 used Bio Printing Facility action
    // Player2's plants amount increased by 2
    p2.turn { cardAction1(BioPrintingFacility) { doTask("2 Plant") }.expect("2 Plant, -2 Energy") }

    // Player1 used Convert Heat standard action
    p1.turn {
      convertHeat()
      // Player1 used Convert Plants standard action
      convertPlants {
        // Player1 placed greenery tile on row 3 position 5
        placeTile(3, 5)
      }
    }

    // Player2 passed
    p2.pass()

    // Player1 used Stratospheric Birds action
    p1.turn {
      cardAction1(StratosphericBirds).expect("Animal")
      // Player1 used Mohole Lake action
      // Player1 added 1 animal(s) to Stratospheric Birds
      cardAction1(MoholeLake) { addCardResources(StratosphericBirds) }
      // Player1 passed
      pass()
    }

    // Generation 9
    // Player1 bought 3 card(s)
    // You drew Rego Plastics, SF Memorial and Water to Venus
    p1.buyCards(3)
    // Player2 bought 2 card(s)
    // You drew Atalanta Planitia Lab and Mining Expedition
    p2.buyCards(2)

    with(p1) {
      assertProduction(m = 27, s = 5, t = 1, p = 1, e = 8, h = 3)
      assertResources(m = 56, s = 5, t = 1, p = 4, e = 8, h = 18)
      assertDashMiddle(played = 34, actions = 7, vp = 58, tr = 38, hand = 12)
      // (but = 10, spt = 6, ...)
      assertTags(10, 6, sct = 5, pot = 4, eat = 5, jot = 1, vet = 8, plt = 1, ant = 1, cit = 2)
      assertCounts(5 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProduction(m = 0, s = 2, t = 3, p = 1, e = 3, h = 3)
      assertResources(m = 28, s = 2, t = 5, p = 3, e = 3, h = 5)
      assertDashMiddle(played = 23, actions = 7, vp = 41, tr = 34, hand = 13)
      assertTags(but = 9, spt = 3, sct = 6, pot = 3, eat = 1, jot = 1, vet = 1, plt = 1, mit = 1)
      assertCounts(2 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    assertSidebar(gen = 9, temp = -6, oxygen = 3, oceans = 4, venus = 18)

    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Venusian Insects
    p1.turn {
      cardAction1(DevelopmentCenter)
      // Player1 used Inventors' Guild action
      cardAction1(InventorsGuild) {
        // Player1 bought 1 card(s)
        // You drew Urbanized Area
        buyCards(1)
      }
    }

    // Player2 played Deimos Down:promo
    // Player2's steel amount increased by 4
    p2.turn {
      playProject(DeimosDownPromo, 9, titanium = 5) {
        repeat(3) { doTask("TemperatureStep") }
        // Player2 placed ocean tile on row 6 position 6
        // Player2's plants amount increased by 1
        p2.doTask("OceanTile<Tharsis_6_7>")
        // Player2 placed Deimos Down tile on row 2 position 5
        p2.placeTile(2, 5)
        // Player1's plants amount decreased by 4 by Player2
        p2.doTask("-4 Plant<Player1>")
        // Player2 gained 2 plants from Arctic Algae
        doTask("4 Steel")
        doTask("Plant<Player2>")
        doTask("2 Plant<Player2>")
        repeat(4) { doTask("TerraformRating") }
        repeat(3) { doTask("2 MC") }
      }
      // Player2 used AI Central action
      // Player2 drew 2 card(s)
      // You drew Ecological Zone and Biomass Combustors
      cardAction1(AiCentral)
    }

    // Player1 used Convert Heat standard action
    p1.turn {
      convertHeat()
      // Player1 used Convert Heat standard action
      convertHeat()
    }

    // Player2 used Aquifer Pumping action
    p2.turn {
      cardAction1(AquiferPumping) {
        p2.intentionalOverpay(1)
        p2.pay(steel = 3)
        // Player2 placed ocean tile on row 5 position 6
        // Player2's plants amount increased by 2
        // Player2 gained 2 plants from Arctic Algae
        placeTile(5, 6)
        doTask("2 MC")
        doTask("2 MC")
        doTask("TerraformRating")
        doTask("2 Plant")
      }
      // Player2 used Convert Plants standard action
      convertPlants {
        // Player2 placed greenery tile on row 9 position 3
        placeTile(9, 7)
      }
    }

    // Player1 played Rego Plastics
    p1.turn {
      // Reason 3: Rego Plastics makes the retained steel worth more for SF Memorial below.
      intentionalUnderpay()
      playProject(RegoPlastics, 10)
      // Player1 played SF Memorial
      // Player1 drew 1 card(s)
      // You drew Advanced Ecosystems
      playProject(SfMemorial, 1, steel = 2)
    }

    // Player2 claimed Gardener milestone
    p2.turn {
      stdAction("ClaimMilestoneSA") { doTask("Gardener") }
      // Player2 used Directed Impactors action
      cardAction1(DirectedImpactors) {
        p2.pay(6)
        // Player2 added 1 asteroid(s) to Rotator Impacts
        addCardResources(RotatorImpacts)
      }
    }

    // Player1 used Floating Habs action
    p1.turn {
      cardAction1(FloatingHabs) {
        // Player1 added 1 floater(s) to Extractor Balloons
        addCardResources(ExtractorBalloons)
      }
      // Player1 used Extractor Balloons action
      // Player1 removed 2 resource(s) from Player1's Extractor Balloons
      // Player1 raised the Venus scale 1 step(s)
      cardAction2(ExtractorBalloons).expect("-2 Floater")
    }

    // Player2 played Ecological Zone
    // Player2 added 2 animal(s) to Ecological Zone
    p2.turn {
      playProject(EcologicalZone, 10) {
            // Player2 placed Ecological Zone tile on row 4 position 5
            // Player2's plants amount increased by 2
            placeTile(4, 5)
            repeat(2) { doTask("Animal<$EcologicalZone>") }
            doTask("2 MC")
            doTask("2 MC")
          }
          .expect("2 Animal, 2 Plant")

      // Player2 played Harvest
      // Player2's mc amount increased by 12
      // Player2 added 1 animal(s) to Ecological Zone
      playProject(Harvest, 2) {
            doTask("Animal<$EcologicalZone>")
            doTask("12 MC")
          }
          .expect("10 MC, Animal, PlayedEvent")
    }

    // Player1 played Noctis Farming
    // Player1's mc production increased by 1
    // Player1's plants amount increased by 2
    p1.turn {
      playProject(NoctisFarming, 1, steel = 3) { doTask("2 Plant") }.expect("PROD[1 MC], 2 Plant")
      // Player1 used Deuterium Export action
      // Player1 removed 1 resource(s) from Player1's Deuterium Export
      // Player1's energy production increased by 1
      cardAction2(DeuteriumExport).expect("-Floater, PROD[Energy]")
    }

    // Player2 used Bio Printing Facility action
    p2.turn {
      cardAction1(BioPrintingFacility) {
        // Player2 added 1 animal(s) to Ecological Zone
        addCardResources(EcologicalZone)
      }
      // Player2 used Rotator Impacts action
      // Player2 removed 1 resource(s) from Player2's Rotator Impacts
      // Player2 removed an asteroid resource to increase Venus scale 1 step
      cardAction2(RotatorImpacts)
    }

    // Player1 used Mohole Lake action
    p1.turn {
      cardAction1(MoholeLake) {
            // Player1 added 1 animal(s) to Stratospheric Birds
            addCardResources(StratosphericBirds)
          }
          .expect("Animal")
      // Player1 used Stratospheric Birds action
      cardAction1(StratosphericBirds).expect("Animal")
    }

    // Player2 used Factorum action
    // 1 card(s) were discarded
    // Player2 drew Protected Valley
    p2.turn {
      cardAction2(Factorum).expect("Card")
      // Player2 played Natural Preserve
      // Player2's mc production increased by 1
      playProject(NaturalPreserve, 1, steel = 2) {
        // Player2 is using their Mars University effect to draw a card by discarding a card.
        // You discarded Herbivores
        // Player2 drew 1 card(s)
        // You drew Thermophiles
        doTask("-ProjectCard")
        // Player2 placed Natural Preserve tile on row 3 position 1
        // Player2 drew 1 card(s)
        // You drew Black Polar Dust
        placeTile(3, 1)
        doTask("ProjectCard")
        doTask("ProjectCard")
      }
    }

    // Player1 used Sell Patents standard project
    // Player1 sold 3 patents
    p1.turn {
      sellPatents(3)
      // Player1 played Water to Venus
      // Player1's mc amount increased by 3 by Optimal Aerobraking
      // Player1's heat amount increased by 3 by Optimal Aerobraking
      playProject(WaterToVenus, 4, titanium = 1) {
        doTask("VenusStep")
        doTask("TerraformRating")
        doTask("3 MC")
        doTask("3 Heat")
      }
    }

    // Player2 used Sell Patents standard project
    // Player2 sold 2 patents
    p2.turn {
      sellPatents(2)
      // Player2 played Kelp Farming
      // Player2's mc production increased by 2
      // Player2's plants production increased by 3
      // Player2's plants amount increased by 2
      // Player2 added 1 animal(s) to Ecological Zone
      playProject(KelpFarming, 15) {
            doTask("Animal<$EcologicalZone>")
            doTask("2 Plant")
            doTask("PROD[2 MC]")
          }
          .expect("5 Production, 2 Plant, Animal")
    }

    // Player1 played Trees
    // Player1's plants production increased by 3
    // Player1's plants amount increased by 1
    p1.turn {
      playProject(Trees, 13) { doTask("Plant") }
      // Player1 funded Banker award
      stdAction("FundAwardSA") { doTask("Banker") }
    }

    // Player2 used Search For Life action
    p2.turn {
      cardAction1(SearchForLife) {
        // Player2 revealed and discarded Fusion Power
        // Decline the science resource.
        declineTask()
      }
    }

    // Player1 played Venusian Insects
    p1.turn {
      playProject(VenusianInsects, 5)
      // Player1 used Venusian Insects action
      cardAction1(VenusianInsects)
    }

    // Player2 passed
    p2.pass()

    // Player1 funded Venuphile award
    p1.turn {
      stdAction("FundAwardSA", which = 2) { doTask("Venuphile") }
      // Player1 passed
      pass()
    }

    // Generation 10
    // Player1 bought 2 card(s)
    // You drew Nitrogen-Rich Asteroid and Lava Tube Settlement
    p1.buyCards(2)
    // Player2 bought 3 card(s)
    // You drew Mercurian Alloys, Hired Raiders and Nuclear Power
    p2.buyCards(3)

    with(p1) {
      assertProduction(m = 28, s = 5, t = 1, p = 4, e = 9, h = 3)
      assertResources(m = 66, s = 5, t = 1, p = 10, e = 9, h = 16)
      assertDashMiddle(played = 40, actions = 8, vp = 78, tr = 42, hand = 8)
      // (but = 13, spt = 6, sct = 5, ...)
      assertTags(13, 6, 5, pot = 4, eat = 5, jot = 1, vet = 9, plt = 3, mit = 1, ant = 1, cit = 2)
      assertCounts(6 to "PlayedEvent", 2 to "CardFront(HAS MAX 0 Tag)", 2 to "CityTile")
    }

    with(p2) {
      assertProduction(m = 3, s = 2, t = 3, p = 4, e = 3, h = 3)
      assertResources(m = 36, s = 3, t = 3, p = 10, e = 3, h = 9)
      assertDashMiddle(played = 28, actions = 7, vp = 58, tr = 41, hand = 13)
      // (but = 10, spt = 3, ...)
      assertTags(10, 3, sct = 7, pot = 3, eat = 1, jot = 1, vet = 1, plt = 3, mit = 1, ant = 1)
      assertCounts(4 to "PlayedEvent", 1 to "CardFront(HAS MAX 0 Tag)", 0 to "CityTile")
    }

    assertSidebar(gen = 10, temp = 4, oxygen = 4, oceans = 6, venus = 24)

    // Player2 played Hired Raiders
    p2.turn {
      playProject(HiredRaiders, 0) {
        // Player1's steel amount decreased by 2 stolen by Player2
        doTask("2 Steel<Player2> FROM Steel<Player1>")
      }
      // Player2 used Convert Heat standard action
      convertHeat()
    }

    // Player1 used Convert Heat standard action
    p1.turn {
      convertHeat()
      // Player1 used City standard project
      stdProject("CitySP") {
        // Player1 placed city tile on row 7 position 4
        placeTile(7, 6)
      }
    }

    // Player2 used Convert Plants standard action
    p2.turn {
      convertPlants {
            // Player2 placed greenery tile on row 9 position 2
            placeTile(9, 6)
            // Player2's steel amount increased by 2
            doTask("2 Steel")
          }
          .expect("2 Steel")

      // Player2 used AI Central action
      // Player2 drew 2 card(s)
      // You drew Energy Tapping and Wave Power
      cardAction1(AiCentral)
    }
    // Player1 used Development Center action
    // Player1 drew 1 card(s)
    // You drew Energy Saving
    p1.turn {
      cardAction1(DevelopmentCenter)
      // Player1 used Inventors' Guild action
      // Player1 bought 0 card(s)
      // You drew no cards
      cardAction1(InventorsGuild) { p1.buyCards(0) }
    }
    // Player2 played Mercurian Alloys
    p2.turn {
      playProject(MercurianAlloys, 1)
      // Player2 played Aerial Mappers
      playProject(AerialMappers, 9)
    }
    // Player1 played Lava Tube Settlement
    // Player1's mc production increased by 2
    // Player1's energy production decreased by 1
    // Player1 placed city tile on row 2 position 2
    // Player1's steel amount increased by 1
    p1.turn {
      playProject(LavaTubeSettlement, 6, steel = 3) {
            placeTile(2, 2)
            doTask("Steel")
            doTask("PROD[-Energy]")
          }
          .expect("-2 Steel")
      // Player1 played Urbanized Area
      // Player1's mc production increased by 2
      // Player1's energy production decreased by 1
      // Player1 placed city tile on row 2 position 3
      playProject(UrbanizedArea, 7, steel = 1) {
        placeTile(2, 3)
        doTask("PROD[-Energy]")
      }
    }
    // Player2 played Atmoscoop
    // Player2 added 2 floater(s) to Aerial Mappers
    p2.turn {
      playProject(Atmoscoop, 5, titanium = 3) {
        doTask("2 VenusStep")
        doTask("VenusStep")
        addCardResources(AerialMappers)
        doTask("TerraformRating")
      }

      // Player2 used Aerial Mappers action
      // Player2 removed 1 resource(s) from Player2's Aerial Mappers
      // Player2 drew 1 card(s)
      // You drew Magnetic Field Generators:promo
      cardAction2(AerialMappers)
    }
    // Player1 played Nitrogen-Rich Asteroid
    // Player1's plants production increased by 4
    // Player1's mc amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    p1.turn {
      playProject(NitrogenRichAsteroid, 26, titanium = 1) {
            doTask("PROD[4 Plant]")
            doTask("2 TerraformRating")
            doTask("TemperatureStep")
            doTask("3 MC")
            doTask("3 Heat")
            doTask("4 Plant")
          }
          .expect("3 Heat")
      // Player1 used Convert Plants standard action
      // Player1 placed greenery tile on row 3 position 3
      convertPlants {
        placeTile(3, 3)
      }
    }
    // Player2 used Bio Printing Facility action
    // Player2 added 1 animal(s) to Ecological Zone
    p2.turn {
      cardAction1(BioPrintingFacility) { addCardResources(EcologicalZone) }
      // Player2 used Directed Impactors action
      // Player2 added 1 asteroid(s) to Rotator Impacts
      cardAction1(DirectedImpactors) {
        p2.pay(6)
        addCardResources(RotatorImpacts)
      }
    }
    // Player1 used Venusian Insects action
    p1.turn {
      cardAction1(VenusianInsects)
      // Player1 used Stratospheric Birds action
      cardAction1(StratosphericBirds)
    }
    // Player2 used Rotator Impacts action
    // Player2 removed 1 resource(s) from Player2's Rotator Impacts
    // Player2 removed an asteroid resource to increase Venus scale 1 step
    p2.turn {
      cardAction2(RotatorImpacts).expect("VenusStep")
      // Player2 used Aquifer Pumping action
      // Player2 placed ocean tile on row 9 position 5
      // Player2's titanium amount increased by 2
      // Player2 gained 2 plants from Arctic Algae
      cardAction1(AquiferPumping) {
            p2.pay(2, steel = 2)
            placeTile(9, 9)
            doTask("2 Titanium")
            doTask("TerraformRating")
          }
          .expect("2 Titanium, 2 Plant")
    }
    // Player1 played Power Infrastructure
    p1.turn {
      playProject(PowerInfrastructure, 4)
      // Player1 used Power Infrastructure action
      // Player1's mc amount increased by 8
      cardAction1(PowerInfrastructure, x = 8)
    }
    // Player2 used Factorum action
    // Player2 drew Electro Catapult
    p2.turn {
      cardAction2(Factorum)
      // Player2 used Sell Patents standard project
      // Player2 sold 2 patents
      sellPatents(2)
    }
    // Player1 used Deuterium Export action
    p1.turn {
      cardAction1(DeuteriumExport)
      // Player1 used Extractor Balloons action
      // Player1 added 1 floater(s) to Extractor Balloons
      cardAction1(ExtractorBalloons)
    }
    // Player2 played Bushes
    // Player2's plants production increased by 2
    // Player2's plants amount increased by 2
    // Player2 added 1 animal(s) to Ecological Zone
    p2.turn {
      playProject(Bushes, 8) {
        doTask("Animal<$EcologicalZone>")
        doTask("2 Plant")
      }
      // Player2 played Energy Tapping
      // Player1's energy production decreased by 1 stolen by Player2
      playProject(EnergyTapping, 1) { doTask("PROD[-Energy<Player1>]") }
    }
    // Player1 used Floating Habs action
    // Player1 added 1 floater(s) to Floating Habs
    p1.turn {
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      // Player1 used Mohole Lake action
      // Player1 added 1 animal(s) to Stratospheric Birds
      cardAction1(MoholeLake) { addCardResources(StratosphericBirds) }
    }
    // Player2 played Nuclear Power
    // Player2's mc production decreased by 2
    // Player2's energy production increased by 3
    p2.turn {
      intentionalOverpay(1)
      playProject(NuclearPower, steel = 3) { doTask("PROD[-2 MC]") }
      // Player2 played Biomass Combustors
      // Player2's energy production increased by 2
      // Player1's plants production decreased by 1 by Player2
      intentionalOverpay(1)
      playProject(BiomassCombustors, steel = 1) { doTask("PROD[-Plant<Player1>]") }
    }
    // Player1 passed
    p1.pass()
    // Player2 used Search For Life action
    // Player2 revealed and discarded Geothermal Power
    p2.turn {
      cardAction1(SearchForLife) { /* Decline the science resource. */
        declineTask()
      }
      // Player2 passed
      pass()
    }
    // Generation 11
    // Player1 bought 2 card(s)
    // You drew Business Network and Gene Repair
    // Player2 bought 1 card(s)
    // You drew Towing A Comet
    p1.buyCards(2)
    p2.buyCards(1)
    // Player1 played Imported Nitrogen
    // Player1's plants amount increased by 4
    // Player1's mc amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    // Player1 added 3 microbe(s) to Venusian Insects
    // Player1 added 2 animal(s) to Stratospheric Birds
    p1.turn {
      playProject(ImportedNitrogen, 15, titanium = 1) {
        addCardResources(VenusianInsects)
        addCardResources(StratosphericBirds)
        doTask("TerraformRating")
        doTask("4 Plant")
        doTask("3 MC")
        doTask("3 Heat")
      }
      // Player1 used Development Center action
      // Player1 drew 1 card(s)
      // You drew Peroxide Power
      cardAction1(DevelopmentCenter)
    }
    // Player2 used AI Central action
    // Player2 drew 2 card(s)
    // You drew Media Group and Cloud Seeding
    p2.turn {
      cardAction1(AiCentral)
      // Player2 used Factorum action
      // 9 card(s) were discarded
      // Player2 drew Deep Well Heating
      cardAction2(Factorum)
    }
    // Player1 used Convert Plants standard action
    // Player1 placed greenery tile on row 2 position 4
    p1.turn {
      convertPlants {
        placeTile(2, 4)
        doTask("2 MC")
      }
      // Player1 used Inventors' Guild action
      // Player1 bought 0 card(s)
      // You drew no cards
      cardAction1(InventorsGuild) { p1.buyCards(0) }
    }
    // Player2 played Media Group
    p2.turn {
      playProject(MediaGroup, 4)
      // Player2 played Mining Expedition
      // Player2's steel amount increased by 2
      // Player1's plants amount decreased by 2 by Player2
      playProject(MiningExpedition, 10) {
        doTask("-2 Plant<Player1>")
        doTask("OxygenStep")
        doTask("TerraformRating")
        doTask("2 Steel")
        doTask("3 MC")
        doTask("TemperatureStep")
      }
    }
    // Player1 used Power Infrastructure action
    // Player1's mc amount increased by 5
    p1.turn {
      cardAction1(PowerInfrastructure, x = 5)
      // Player1 used Extractor Balloons action
      // Player1 added 1 floater(s) to Extractor Balloons
      cardAction1(ExtractorBalloons)
    }
    // Player2 used Bio Printing Facility action
    // Player2 added 1 animal(s) to Ecological Zone
    p2.turn {
      cardAction1(BioPrintingFacility) { addCardResources(EcologicalZone) }
      // Player2 used Aquifer Pumping action
      // Player2 placed ocean tile on row 5 position 4
      // Player2's plants amount increased by 2
      // Player2 gained 2 plants from Arctic Algae
      cardAction1(AquiferPumping) {
        pay(2, steel = 2)
        placeTile(5, 4)
        doTask("2 MC")
        doTask("TerraformRating")
        doTask("2 Plant")
      }
    }
    // Player1 played Business Network
    // Player1's mc production decreased by 1
    p1.turn {
      playProject(BusinessNetwork, 1).expect("PROD[-1 MC]")
      // Player1 used Business Network action
      // Player1 bought 1 card(s)
      // You drew Standard Technology
      cardAction1(BusinessNetwork) { p1.buyCards(1) }
    }
    // Player2 used City standard project
    // Player2 placed city tile on row 8 position 2
    p2.turn {
      stdProject("CitySP") {
        placeTile(8, 5)
      }
      // Player2 used Convert Plants standard action
      convertPlants {
            // Player2 placed greenery tile on row 8 position 1
            placeTile(8, 4)
            // Player2's steel amount increased by 2
            doTask("2 Steel")
          }
          .expect("2 Steel")
    }
    // Player1 used Deuterium Export action
    // Player1 removed 1 resource(s) from Player1's Deuterium Export
    // Player1's energy production increased by 1
    p1.turn {
      cardAction2(DeuteriumExport).expect("PROD[Energy]")
      // Player1 used Floating Habs action
      // Player1 added 1 floater(s) to Floating Habs
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    // Player2 used Convert Plants standard action
    p2.turn {
      convertPlants {
            // Player2 placed greenery tile on row 9 position 1
            placeTile(9, 5)
            // Player2's steel amount increased by 1
            doTask("Steel")
          }
          .expect("Steel")
      // Player2 used Aerial Mappers action
      // Player2 removed 1 resource(s) from Player2's Aerial Mappers
      // Player2 drew 1 card(s)
      // You drew Penguins
      cardAction2(AerialMappers)
    }
    // Player1 used Stratospheric Birds action
    p1.turn {
      cardAction1(StratosphericBirds)
      // Player1 used Mohole Lake action
      // Player1 added 1 animal(s) to Stratospheric Birds
      cardAction1(MoholeLake) { addCardResources(StratosphericBirds) }
    }
    // Player2 played Magnetic Field Generators:promo
    // Player2's plants production increased by 2
    // Player2's energy production decreased by 4
    p2.turn {
      playProject(MagneticFieldGeneratorsPromo, 2, steel = 6) {
            // Player2 placed Magnetic Field Generators tile on row 6 position 5
            placeTile(6, 6)
            // Player2's plants amount increased by 1
            doTask("PROD[-4 Energy]")
            doTask("PROD[2 Plant]")
            doTask("3 TerraformRating")
            repeat(3) { doTask("2 MC") }
          }
          .expect("PROD[-4 Energy, 2 Plant], 3 TerraformRating, Plant")
      // Player2 played Towing A Comet
      // Player2's plants amount increased by 2
      playProject(TowingAComet, 1, titanium = 4) {
            // Player2 placed ocean tile on row 6 position 7
            placeTile(6, 8)
            // Player2's plants amount increased by 1
            // Player2 gained 2 plants from Arctic Algae
            doTask("2 Plant")
            doTask("OxygenStep")
            repeat(2) { doTask("TerraformRating") }
            doTask("3 MC")
            doTask("2 MC")
            doTask("Plant")
            doTask("2 Plant")
          }
          .expect("5 Plant")
    }
    // Player1 used Venusian Insects action
    p1.turn {
      cardAction1(VenusianInsects)
      // Player1 played Standard Technology
      // Player1 removed 1 resource(s) from Player1's Olympus Conference
      // Player1 drew 1 card(s)
      // You drew Zeppelins
      playProject(StandardTechnology, 6) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    // Player2 played Atalanta Planitia Lab
    // Player2 drew 2 card(s)
    // You drew House Printing and Robot Pollinators
    p2.turn {
      playProject(AtalantaPlanitiaLab, 8) {
        // Player2 is using their Mars University effect to draw a card by discarding a card.
        // You discarded Cloud Seeding
        // Player2 drew 1 card(s)
        // You drew Corroder Suits
        doTask("-ProjectCard")
        doTask("2 ProjectCard")
      }
      // Player2 used Sell Patents standard project
      // Player2 sold 3 patents
      sellPatents(3)
    }
    engine.assertCounts(9 to "OceanTile")
    // Player1 played Large Convoy
    // Player1 drew 2 card(s)
    // You drew Water Splitting Plant and Martian Survey
    // Player1's mc amount increased by 3 by Optimal Aerobraking
    // Player1's heat amount increased by 3 by Optimal Aerobraking
    // Player1 added 4 animal(s) to Stratospheric Birds
    p1.turn {
      playProject(LargeConvoy, 31) {
            addCardResources(StratosphericBirds)
            // No ocean tile remains to place.
            doTask("Ok")
            doTask("2 ProjectCard")
            doTask("3 MC")
            doTask("3 Heat")
          }
          .expect("ProjectCard, 3 Heat, 4 Animal")
      // Player1 played Water Splitting Plant
      playProject(WaterSplittingPlant, steel = 4)
    }
    // Player2 played Robot Pollinators
    // Player2's plants production increased by 1
    // Player2's plants amount increased by 4
    p2.turn {
      playProject(RobotPollinators, 7) { doTask("PROD[Plant]") }.expect("PROD[Plant], 4 Plant")
      // Player2 used Convert Plants standard action
      // Player2 placed greenery tile on row 7 position 2
      convertPlants {
        placeTile(7, 4)
      }
    }
    // Player1 played Media Archives
    // Player1's mc amount increased by 16
    p1.turn {
      playProject(MediaArchives, 5)
      // Player1 used Greenery standard project
      // Player1's mc amount increased by 3
      stdProject("GreenerySP") {
        // Player1 placed greenery tile on row 5 position 7
        placeTile(5, 7)
        // Player1's plants amount increased by 2
        doTask("3 MC")
        repeat(3) { doTask("2 MC") }
        doTask("2 Plant")
      }
    }
    // Player2 played Greenhouses
    // Player2's plants amount increased by 6
    // Player2 added 1 animal(s) to Ecological Zone
    p2.turn {
      playProject(Greenhouses, 4) { doTask("Animal<$EcologicalZone>!") }.expect("6 Plant, Animal")
      // Player2 used Convert Plants standard action
      // Player2 placed greenery tile on row 9 position 4
      convertPlants {
        placeTile(9, 8)
        doTask("2 MC")
      }
    }
    // Player1 funded Thermalist award
    p1.turn {
      stdAction("FundAwardSA", which = 3) { doTask("Thermalist") }
      // Player1 used Convert Plants standard action
      convertPlants {
            // Player1 placed greenery tile on row 4 position 4
            placeTile(4, 4)
            // Player1's plants amount increased by 1
            doTask("Plant")
            doTask("2 MC")
            doTask("2 MC")
          }
          .expect("-7 Plant")
    }
    // Player2 used Sell Patents standard project
    // Player2 sold 3 patents
    p2.turn {
      sellPatents(3)
      // Player2 played Penguins
      // Player2 added 1 animal(s) to Ecological Zone
      playProject(Penguins, 5).expect("Animal<$EcologicalZone>")
    }
    // Player1 played Advanced Ecosystems
    p1.turn {
      playProject(AdvancedEcosystems, 11)
      // Player1 used Sell Patents standard project
      // Player1 sold 4 patents
      sellPatents(4)
    }
    // Player2 used Penguins action
    p2.turn {
      cardAction1(Penguins)
    }
    // Player1 played Gene Repair
    // Player1's megacredits production increased by 2
    p1.turn {
      playProject(GeneRepair, 12) {
            doTask("PROD[2 MC]")
            doTask("Science<$OlympusConference>")
          }
          .expect("PROD[2 MC]")
    }
    // Player2 passed
    p2.pass()
    // Player1 passed
    p1.pass()
    // Final greenery placement
    p1.convertPlants {
      // Player1 placed greenery tile on row 6 position 4
      placeTile(6, 5)
      doTask("Plant")
      doTask("2 MC")
    }
    // Player1's plants amount increased by 1
    // Decline another final greenery placement.
    p1.declineTask()
    p2.convertPlants {
      // Player2 placed greenery tile on row 8 position 5
      placeTile(8, 8)
    }
    // Decline another final greenery placement.
    p2.declineTask()
    // This game id was gf386a4cd5de1

    val summ = Summarizer(game)
    summ.net("$Manutech", "Resource") shouldBe 104
    summ.net("Production<Player2>", "Resource<Player2>") shouldBe 187

    summ.net("$EarthOffice", "Owed") shouldBe -24
    // Random automatic order may attribute fewer saturated removals here; see SEQUENCING.md.
    summ.net("$AdvancedAlloys<Player2>", "Owed") shouldBe -34
    summ.net("$EarthCatapult<Player2>", "Owed") shouldBe -55
    summ.net("$QuantumExtractor", "Owed") shouldBe -10 // oof

    summ.net("$AquiferPumping", "OceanTile") shouldBe 6
    summ.net("$ArcticAlgae", "Plant") shouldBe 19
    summ.net("$OptimalAerobraking", "Resource") shouldBe 42
    // summ.net("$SearchForLife", "MC") shouldBe -4
    summ.net("$SearchForLife", "Science") shouldBe 0

    // This is just silly
    summ.net("TerraformRating<Player1>", "MC<Player1>") shouldBe 361
    summ.net("TerraformRating<Player1>", "MC") shouldBe 361
    summ.net("TerraformRating", "MC<Player1>") shouldBe 361
    summ.net("TerraformRating<Player2>", "MC<Player2>") shouldBe 356
    summ.net("TerraformRating<Player2>", "MC") shouldBe 356
    summ.net("TerraformRating", "MC<Player2>") shouldBe 356

    summ.net("TerraformRating", "MC") shouldBe 717

    summ.net("TerraformRating<Player1>", "MC<Player2>") shouldBe 0
    summ.net("TerraformRating<Player2>", "MC<Player1>") shouldBe 0
  }

  private fun checkSummaryAfterGen4(game: World) {
    val summer = Summarizer(game)

    // AA's effect has triggered once, plus the immediate plant
    summer.net("$ArcticAlgae", "Plant") shouldBe 3

    // Blue has done 16 card buys: 5 initial, 8 in research, and 3 from inventors guild
    summer.net("BuySelectedCards<Player1>", "BuyCard<Player1>") shouldBe 16

    // DeuteriumExport produced a net of 1 floaters (made, consumed, made)
    summer.net("$DeuteriumExport", "Floater") shouldBe 1
    summer.net("$DeuteriumExport", "Production<Class<Energy>>") shouldBe 1

    // EarthOffice has saved blue 6 money (InvestmentLoan, ImportedGhg)
    summer.net("$EarthOffice", "Owed<Player1>") shouldBe -6

    // Manutech has delivered! 1 MC with NewPartner, 4 with AlliedBank, 3 with CorporateStronghold
    // ... plus of course 35 at game start
    summer.net("$Manutech", "MC<Player1>") shouldBe 43

    // Purple got 63 MC from Terraform Rating (at production phases they had 20, 21, 22, and 24
    // Terraform Rating)
    summer.net("TerraformRating", "MC<Player2>") shouldBe 87
    summer.net("TerraformRating<Player2>", "MC") shouldBe 87
    summer.net("TerraformRating<Player2>", "MC<Player2>") shouldBe 87
    summer.net("TerraformRating", "MC") shouldBe 183

    // Blue has raised temp 2 & venus 2, purple did temp & venus2 & ocean
    summer.net("GlobalParameter", "TerraformRating<Player1>") shouldBe 4
    summer.net("GlobalParameter", "TerraformRating<Player2>") shouldBe 4
  }
}
