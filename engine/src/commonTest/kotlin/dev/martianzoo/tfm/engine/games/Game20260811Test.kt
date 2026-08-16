package dev.martianzoo.tfm.engine.games

import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmWorkflow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Synthetic Proton Fragment - https://terraforming-mars.herokuapp.com/the-end?id=p9d6d3ff25b39
class Game20260811Test : AbstractFullGameTest() {
  // The archived metadata specifies Hellas, Corporate Era, Prelude, promo cards, three players,
  // no Venus/Colonies/Turmoil, and the following full-random milestone and award pools.
  override val config =
      GameConfig(
          """
          HellasMapOption
          PreludeExpansion, MilestonesAwardsExpansion, PromoCardPack

          Mayor, Diversifier, Trader, Sponsor, MilestoneMM35
          Biologist, SpaceBaron, Forecaster, Botanist, Collector
          """,
          "Player1",
          "Player2",
          "Player3",
      )

  @Test
  fun game20260811() {
    TfmWorkflow.Auto(game).launch()

    // First player this generation is Mom
    // Good luck Mom!
    val mom = p1
    // Good luck Ellie!
    val ellie = p2
    // Good luck Dad!
    val dad = p3

    // Mom gets a handicap
    mom.exMachina("5 TR")

    // Generation 1
    engine.assertCounts(1 to "Generation")

    // Mom played Recyclon
    // Mom gained 1 steel production
    // Mom kept 4 project cards
    mom.playCorp("Recyclon") { buyCards(4) }.expect("PROD[S], 26")

    // Ellie played Robinson Industries
    // Ellie kept 5 project cards
    ellie.playCorp("RobinsonIndustries") { buyCards(5) }.expect("32")

    // Dad played Splice
    // Dad kept 7 project cards
    // Dad gained 2 M€ because of Splice
    // Dad gained 2 M€ because of Splice
    dad.playCorp("SpliceTacticalGenomics") {
          buyCards(7)
          doTask("2")
        }
        .expect("27")

    // Mom played Albedo Plants
    // Mom gained 1 plant production
    // Mom gained 1 plant
    // Mom gained 3 heat
    mom.playPrelude("AlbedoPlants").expect("PROD[P], P, 3 H")
    // Mom played Society Support
    // Mom lost 1 M€ production
    // Mom gained 1 plant production
    // Mom gained 1 energy production
    // Mom gained 1 heat production
    mom.playPrelude("SocietySupport").expect("PROD[-M, P, E, H]")

    // Ellie played Metals Company
    // Ellie gained 1 M€ production
    // Ellie gained 1 steel production
    // Ellie gained 1 titanium production
    ellie.playPrelude("MetalsCompany").expect("PROD[M, S, T]")
    // Ellie played Biolab
    // Ellie gained 1 plant production
    // Ellie drew 3 card(s)
    // You drew Hired Raiders,Meat Industry,Mining Rights
    ellie.playPrelude("Biolab").expect("PROD[P], 3 ProjectCard")

    // Dad played Merger
    // You drew ThorGate,Inventrix,United Nations Mars Initiative,PhoboLog
    dad.playPrelude("Merger") {
      // Dad played Inventrix
      doTask("PlayCard<Class<CorporationCard>, Class<Inventrix>>")
    }
    // Dad played Mohole Excavation
    // Dad gained 1 steel production
    // Dad gained 2 heat production
    // Dad gained 2 heat
    dad.playPrelude("MoholeExcavation").expect("PROD[S, 2 H], 2 H")

    // Mom played Deep Well Heating
    // Mom gained 1 energy production
    mom.playProject("DeepWellHeating", 13).expect("PROD[E], TR")
    // Mom passed
    mom.declineSecondAction()

    // Ellie played Earth Catapult
    ellie.playProject("EarthCatapult", 23)
    // Ellie used Robinson Industries action
    // Ellie gained 1 heat production
    ellie.cardAction1("RobinsonIndustries") {
      // NOPE: gotta pick a lower one
      shouldThrow<NarrowingException> { doTask("PROD[T]") }
      doTask("PROD[H]")
    }

    // NOPE: can't choose any other first action
    shouldThrow<RequirementException> { dad.playProject("Lichen", 7) }

    // Dad took the first action of Splice corporation
    // Discarded 14 cards Supermarkets,Asteroid Mining,Asteroid Mining Consortium,Ganymede
    // Colony,Static Harvesting,Kaguya Tech,Rover Construction,Penguins,Imported GHG,Small
    // Animals,Artificial Lake,SF Memorial,Research Outpost,Fuel Factory
    // Dad drew Ants
    // Dad took the first action of Inventrix corporation
    // Dad drew 3 card(s)
    // You drew Corporate Stronghold,Solar Logistics,Diversity Support
    dad.stdAction("HandleMandates").expect("4 ProjectCard")
    // Dad played Lichen
    // Dad gained 1 plant production
    dad.playProject("Lichen", 7).expect("PROD[P]")

    // (Mom already passed early)
    mom.pass()
    // Ellie passed
    ellie.pass()
    // Dad played Solar Logistics
    // Dad gained 2 titanium
    dad.playProject("SolarLogistics", 20).expect("2 T")
    // Dad passed
    dad.pass()

    // Generation 2
    // First player this generation is Ellie
    // Game20260811-dashboards-gen2.png was taken before cards were bought.
    mom.assertResources(m = 38, s = 1, t = 0, p = 3, e = 2, h = 4)
    mom.assertProduction(m = -1, s = 1, t = 0, p = 2, e = 2, h = 1)
    ellie.assertResources(m = 26, s = 1, t = 1, p = 1, e = 0, h = 1)
    ellie.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 0, h = 1)
    dad.assertResources(m = 23, s = 1, t = 2, p = 1, e = 0, h = 4)
    dad.assertProduction(m = 0, s = 1, t = 0, p = 1, e = 0, h = 2)
    // Ellie bought 1 card(s)
    // You bought Directed Impactors
    ellie.buyCards(1)
    // Mom bought 2 card(s)
    // You bought Magnetic Field Dome,Optimal Aerobraking
    mom.buyCards(2)
    // Dad bought 3 card(s)
    // You bought Medical Lab,AI Central,Asteroid Rights
    dad.buyCards(3)

    // Ellie played Media Group
    ellie.playProject("MediaGroup", 4)
    // Ellie played Sabotage
    ellie
        .playProject("Sabotage", 0) {
          // Mom lost 7 M€ because of Ellie
          // Ellie gained 3 M€
          doTask("-7 M<Player1>")
        }
        .expect("3 M")

    // Dad played Asteroid Rights
    // Dad added 2 Asteroid(s) to Asteroid Rights
    dad.playProject("AsteroidRights", 2, titanium = 2).expect("2 Asteroid")
    // Dad used Asteroid Rights action
    // Dad removed 1 Asteroid from Asteroid Rights to gain 2 titanium
    dad.cardAction2("AsteroidRights") { doTask("2 T") }.expect("-Asteroid")

    // Mom played Release of Inert Gases
    mom.playProject("ReleaseOfInertGases", 14).expect("2 TR")
    // Mom played Magnetic Field Dome
    // Mom gained 1 plant production
    // Mom lost 2 energy production
    mom.playProject("MagneticFieldDome", 3, steel = 1) {
          // Mom removed 2 resource(s) from Mom's Recyclon
          doTask("-2 Microbe<Recyclon> THEN PROD[P]")
        }
        .expect("PROD[2 P, -2 E]")

    // Ellie played Solar Wind Power
    // Ellie gained 1 energy production
    // Ellie gained 2 titanium
    ellie.playProject("SolarWindPower", 9).expect("PROD[E], 2 T")
    // Ellie played Hired Raiders
    ellie
        .playProject("HiredRaiders", 0) {
          // Ellie stole 3 M€ from Dad
          // Ellie gained 3 M€
          doTask("3 M<Player2> FROM M<Player3>")
        }
        .expect("6 M")

    // Dad played ArchaeBacteria
    // Dad gained 1 plant production
    // Dad gained 2 M€ because of Splice
    // Dad gained 2 M€ because of Splice
    dad.playProject("Archaebacteria", 6).expect("PROD[P], -2")
    // Dad passed
    dad.declineSecondAction()
    // Mom passed
    mom.pass()

    // Ellie played Lagrange Observatory
    // Ellie drew 1 card(s)
    // You drew Cloud Seeding
    ellie.playProject("LagrangeObservatory", 7).expect("0 ProjectCard")
    // Ellie used Robinson Industries action
    // Ellie gained 1 titanium production
    ellie.cardAction1("RobinsonIndustries") { doTask("PROD[T]") }
    // (Dad already passed early)
    dad.pass()
    // Ellie played Mining Rights
    // Ellie gained 1 titanium production
    ellie
        .playProject("MiningRights", 5, steel = 1) {
          // Ellie placed Mining Rights tile at 57
          doTask("Tile067<Hellas_8_8>")
          // Ellie gained 1 titanium
        }
        .expect("PROD[T], T")
    // Ellie played Directed Impactors
    ellie.playProject("DirectedImpactors", titanium = 2)
    // Ellie used Directed Impactors action
    // Ellie added 1 Asteroid to Directed Impactors
    ellie.cardAction1("DirectedImpactors") {
      ellie.pay(titanium = 2)
      doTask("Asteroid<DirectedImpactors>")
    }
    // Ellie passed
    ellie.doTask("Pass")

    // Generation 3
    // First player this generation is Dad
    // Game20260811-dashboards-gen3.png was taken before cards were bought.
    mom.assertResources(m = 36, s = 1, t = 0, p = 7, e = 0, h = 7)
    mom.assertProduction(m = -1, s = 1, t = 0, p = 4, e = 0, h = 1)
    ellie.assertResources(m = 24, s = 1, t = 3, p = 2, e = 1, h = 2)
    ellie.assertProduction(m = 1, s = 1, t = 3, p = 1, e = 1, h = 1)
    dad.assertResources(m = 27, s = 2, t = 2, p = 3, e = 0, h = 6)
    dad.assertProduction(m = 0, s = 1, t = 0, p = 2, e = 0, h = 2)
    // Mom bought 2 card(s)
    // You bought Peroxide Power,Hospitals
    mom.buyCards(2)
    // Ellie bought 3 card(s)
    // You bought Protected Growth,GHG Factories,Soletta
    ellie.buyCards(3)
    // Dad bought 3 card(s)
    // You bought Fueled Generators,Cupola City,Designed Microorganisms
    dad.buyCards(3)

    // Dad played Fueled Generators
    // Dad lost 1 M€ production
    // Dad gained 1 energy production
    dad.playProject("FueledGenerators", 1).expect("PROD[-M, E]")
    // Dad used Asteroid Rights action
    // Dad removed 1 Asteroid from Asteroid Rights to gain 2 titanium
    dad.cardAction2("AsteroidRights") { doTask("2 T") }.expect("-Asteroid")

    // Mom played Peroxide Power
    // Mom lost 1 M€ production
    // Mom gained 2 energy production
    mom.playProject("PeroxidePower", 5, steel = 1).expect("PROD[-M, 2 E]")
    // Mom played Hospitals
    // Mom lost 1 energy production
    mom.playProject("Hospitals", 8).expect("PROD[-E]")

    // Ellie used Robinson Industries action
    // Ellie gained 1 steel production
    ellie.cardAction1("RobinsonIndustries") { doTask("PROD[S]") }
    // Ellie played GHG Factories
    // Ellie lost 1 energy production
    // Ellie gained 4 heat production
    ellie.playProject("GhgFactories", 7, steel = 1).expect("PROD[-E, 4 H]")

    // NOPE: almost there, not quite
    shouldThrow<RequirementException> {
      dad.stdAction("ClaimMilestoneSA") { doTask("Diversifier") }
    }
    // Dad played Corporate Stronghold
    // Dad gained 3 M€ production
    // Dad lost 1 energy production
    dad.playProject("CorporateStronghold", 7, steel = 2) {
          // Dad placed city tile at 11
          doTask("CityTile<Hellas_2_4>")
          // Dad gained 1 plant
          // Dad gained 1 steel
          // Mom added 1 Disease to Hospitals
        }
        .expect("PROD[3 M, -E], P, -S, Disease<Player1>")
    mom.assertCounts(7 to "P")
    // Dad claimed Diversifier milestone
    dad.stdAction("ClaimMilestoneSA") { doTask("Diversifier") }

    // Mom played Industrial Center
    mom.playProject("IndustrialCenter", 4) {
          // Mom placed Industrial Center tile at 10
          doTask("Tile123<Hellas_2_3>")
          // Mom gained 1 plant
          // Mom removed 2 resource(s) from Mom's Recyclon
          doTask("-2 Microbe<Recyclon> THEN PROD[P]")
        }
        .expect("P")
    mom.assertCounts(8 to "P")
    // Mom used Convert Plants standard action
    mom.stdAction("ConvertPlantsSA") {
          // Mom placed greenery tile at 04
          doTask("GreeneryTile<Hellas_1_2>")
          // Mom gained 2 plants
        }
        .expect("-6 P, TR")

    // Ellie used Directed Impactors action
    // Ellie added 1 Asteroid to Directed Impactors
    ellie.cardAction1("DirectedImpactors") {
      ellie.pay(titanium = 2)
      doTask("Asteroid<DirectedImpactors>")
    }
    // Ellie passed
    ellie.declineSecondAction()
    // Dad passed
    dad.pass()

    // Mom played Ecological Zone
    // Mom gained 3 heat
    // Mom added 2 Animal(s) to Ecological Zone
    mom.playProject("EcologicalZone", 12) {
          // Mom placed Ecological Zone tile at 05
          doTask("Tile128<Hellas_1_3>")
          // Mom gained 2 plants
        }
        .expect("3 H, 2 Animal, 2 P")
    // Mom used Convert Heat standard action
    mom.stdAction("ConvertHeatSA").expect("TR")
    // (Ellie already passed early)
    ellie.pass()
    // Mom passed
    mom.pass()

    // Generation 4
    // First player this generation is Mom
    // Game20260811-dashboards-gen4.png was taken before cards were bought.
    mom.assertResources(m = 30, s = 1, t = 0, p = 9, e = 1, h = 3)
    mom.assertProduction(m = -2, s = 1, t = 0, p = 5, e = 1, h = 1)
    ellie.assertResources(m = 25, s = 2, t = 4, p = 3, e = 0, h = 8)
    ellie.assertProduction(m = 1, s = 2, t = 3, p = 1, e = 0, h = 5)
    dad.assertResources(m = 24, s = 2, t = 4, p = 6, e = 0, h = 8)
    dad.assertProduction(m = 2, s = 1, t = 0, p = 2, e = 0, h = 2)
    // Ellie bought 3 card(s)
    // You bought Mine,Bribed Committee,Public Plans
    ellie.buyCards(3)
    // Mom bought 2 card(s)
    // You bought Toll Station,Natural Preserve
    mom.buyCards(2)
    // Dad bought 2 card(s)
    // You bought Lunar Beam,Weather Balloons
    dad.buyCards(2)

    // Mom used Aquifer standard project
    mom.stdProject("AquiferSP") {
          // Mom placed ocean tile at 08
          doTask("OceanTile<Hellas_2_1>")
          // Mom gained 2 plants
        }
        .expect("TR, 2 P")
    // Mom used Convert Plants standard action
    mom.stdAction("ConvertPlantsSA") {
          // Mom placed greenery tile at 16
          doTask("GreeneryTile<Hellas_3_3>")
          // Mom gained 1 steel
        }
        .expect("S, TR")

    // Ellie used Convert Heat standard action
    // Ellie gained 1 heat production
    ellie.stdAction("ConvertHeatSA").expect("PROD[H], TR")
    // Ellie played Public Plans
    ellie
        .playProject("PublicPlans", 5) {
          // Ellie gained 6 M€ because of Public Plans
          doTask("6")
          // Ellie revealed Meat Industry,Cloud Seeding,Protected Growth,Soletta,Mine,Bribed
          // Committee
          // Ellie gained 3 M€
        }
        .expect("4")

    // Dad played Heat Trappers
    // Dad gained 1 energy production
    // Ellie lost 2 heat production because of Dad
    dad.playProject("HeatTrappers", 2, steel = 2) {
          // NOPE: Mom had only 1 heat production, so Dad could not choose her instead.
          shouldThrow<LimitsException> { doTask("PROD[-2 H<Player1>]") }
          shouldThrow<NarrowingException> { doTask("PROD[-H<Player1>]") }
          doTask("PROD[-2 H<Player2>]")
        }
        .expect("PROD[E]")
    // Dad played Robotic Workforce
    dad.playProject("RoboticWorkforce", 9) {
          // Dad copied Heat Trappers production with Robotic Workforce
          // Dad gained 1 energy production
          doTask("CopyProductionBox<HeatTrappers>")
          // Ellie lost 2 heat production because of Dad
          doTask("PROD[-2 Heat<Player2>]")
        }
        .expect("PROD[E]")

    // Mom played Natural Preserve
    // Mom gained 1 M€ production
    mom.playProject("NaturalPreserve", 5, steel = 2) {
          // Mom placed Natural Preserve tile at 20
          doTask("Tile044<Hellas_3_7>")
          // Mom gained 1 plant
          // Mom drew 1 card(s)
          // You drew Psychrophiles
        }
        .expect("PROD[M], P, 0 ProjectCard")
    // Mom used Sell Patents standard project
    // Mom sold 1 patents
    mom.sellPatents(1)

    // Ellie played Protected Growth
    // Ellie gained 1 plant
    // Ellie gained 3 M€
    ellie.playProject("ProtectedGrowth", megacredits = 0).expect("P, 3")
    // Ellie played Soletta
    // Ellie gained 7 heat production
    ellie.playProject("Soletta", 21, titanium = 4).expect("PROD[7 H]")

    // Dad used Asteroid Rights action
    // Dad added 1 Asteroid to Asteroid Rights
    dad.cardAction1("AsteroidRights") { doTask("Asteroid<AsteroidRights>") }
    // Dad played Mercurian Alloys
    dad.playProject("MercurianAlloys", 3)

    // Mom used Hospitals action
    // Mom removed 1 resource(s) from Mom's Hospitals
    // Mom gained 1 M€
    mom.cardAction1("Hospitals").expect("-Disease, 1")
    // Mom played Psychrophiles
    mom.playProject("Psychrophiles", 2) {
          // Mom added 1 Microbe to Psychrophiles
          doTask("Microbe<Psychrophiles>!")
          // Dad gained 2 M€ because of Splice
        }
        .expect("2 M<Player3>")

    // Ellie played Mine
    // Ellie gained 1 steel production
    ellie.playProject("Mine", steel = 1).expect("PROD[S]")
    // Ellie passed
    ellie.declineSecondAction()
    // Dad passed
    dad.pass()

    // Mom used Psychrophiles action
    // Mom added 1 Microbe to Psychrophiles
    mom.cardAction1("Psychrophiles").expect("Microbe")
    mom.declineSecondAction()
    // (Ellie already passed early)
    ellie.pass()
    // Mom passed
    mom.pass()

    // Generation 5
    // First player this generation is Ellie
    // Game20260811-dashboards-gen5.png was taken before cards were bought.
    mom.assertResources(m = 33, s = 1, t = 0, p = 9, e = 1, h = 5)
    mom.assertProduction(m = -1, s = 1, t = 0, p = 5, e = 1, h = 1)
    ellie.assertResources(m = 24, s = 4, t = 3, p = 5, e = 0, h = 9)
    ellie.assertProduction(m = 1, s = 3, t = 3, p = 1, e = 0, h = 9)
    dad.assertResources(m = 27, s = 1, t = 4, p = 8, e = 2, h = 10)
    dad.assertProduction(m = 2, s = 1, t = 0, p = 2, e = 2, h = 2)
    // Mom bought 1 card(s)
    // You bought Bio Printing Facility
    mom.buyCards(1)
    // Ellie bought 3 card(s)
    // You bought Hermetic Order of Mars,Mining Expedition,Lava Flows
    ellie.buyCards(3)
    // Dad bought 3 card(s)
    // You bought Anti-Gravity Technology,Hackers,Callisto Penal Mines
    dad.buyCards(3)

    // NOPE: almost there, not quite
    shouldThrow<RequirementException> {
      ellie.stdAction("ClaimMilestoneSA") { doTask("Tycoon") }
    }
    // Ellie played Hermetic Order of Mars
    // Ellie gained 2 M€ production
    // Ellie gained 6 M€
    ellie.playProject("HermeticOrderOfMars", 8).expect("PROD[2 M], -2")
    // Ellie claimed Tycoon10 milestone
    ellie.stdAction("ClaimMilestoneSA") { doTask("Tycoon") }

    // Dad played Ants
    // Dad gained 2 M€ because of Splice
    // Dad gained 2 M€ because of Splice
    dad.playProject("Ants", 9) { doTask("2") }.expect("-5")
    // Dad used Ants action
    dad.cardAction1("Ants") {
          // Dad removed 1 resource(s) from Mom's Recyclon
          doTask("-Microbe<Player1, Recyclon<Player1>>")
          // Dad added 1 Microbe to Ants
        }
        .expect("Microbe<Ants>")

    // Mom used Psychrophiles action
    // Mom added 1 Microbe to Psychrophiles
    mom.cardAction1("Psychrophiles").expect("Microbe")
    // Mom used Power Plant:SP standard project
    mom.stdProject("PowerPlantSP").expect("PROD[E]")

    // Ellie used Convert Heat standard action
    ellie.stdAction("ConvertHeatSA").expect("TR")
    // Ellie used Directed Impactors action
    // Ellie removed 1 Asteroid from Directed Impactors to raise temperature 1 step
    // Ellie gained 1 heat production
    ellie.cardAction2("DirectedImpactors").expect("-Asteroid, TemperatureStep, PROD[H], TR")

    // Dad played Weather Balloons
    // Dad drew 1 card(s)
    // You drew Jovian Embassy
    dad.playProject("WeatherBalloons", 11).expect("0 ProjectCard")
    // Dad used Weather Balloons action
    // Dad added 1 Floater to Weather Balloons
    dad.cardAction1("WeatherBalloons").expect("Floater")

    // Mom played Toll Station
    // Mom gained 7 M€ production
    mom.playProject("TollStation", 12).expect("PROD[7 M]")
    // Mom used Convert Plants standard action
    mom.stdAction("ConvertPlantsSA") {
          // Mom placed greenery tile at 24
          doTask("GreeneryTile<Hellas_4_4>")
          // Mom gained 2 steel
        }
        .expect("2 S, TR")

    // Ellie played Bribed Committee
    // Ellie gained 3 M€
    ellie.playProject("BribedCommittee", 5).expect("-2")
    // Ellie used Sell Patents standard project
    // Ellie sold 1 patents
    ellie.sellPatents(1)

    // Dad played Diversity Support
    dad.playProject("DiversitySupport", 1)
    // Dad used Asteroid Rights action
    // Dad removed 1 Asteroid from Asteroid Rights to gain 2 titanium
    dad.cardAction2("AsteroidRights") { doTask("2 T") }.expect("-Asteroid")

    // Mom used Industrial Center action
    // Mom gained 1 steel production
    mom.cardAction1("IndustrialCenter").expect("PROD[S]")
    // Mom passed
    mom.declineSecondAction()

    // Ellie used Robinson Industries action
    // Ellie gained 1 energy production
    ellie.cardAction1("RobinsonIndustries").expect("PROD[E]")
    // Ellie passed
    ellie.declineSecondAction()

    // Dad played Callisto Penal Mines
    // Dad gained 3 M€ production
    dad.playProject("CallistoPenalMines", titanium = 6).expect("PROD[3 M]")
    // Dad used Convert Heat standard action
    dad.stdAction("ConvertHeatSA").expect("TR")

    // (Mom already passed early)
    mom.pass()
    // (Ellie already passed early)
    ellie.pass()
    // Dad used Convert Plants standard action
    dad.stdAction("ConvertPlantsSA") {
          // Dad placed greenery tile at 06
          doTask("GreeneryTile<Hellas_1_4>")
          // Dad gained 1 plant
          // Dad gained 1 steel
        }
        .expect("-7 P, S, TR")
    // Dad passed
    dad.doTask("Pass")

    // Generation 6
    // First player this generation is Dad
    // Game20260811-dashboards-gen6.png was taken before cards were bought.
    mom.assertResources(m = 40, s = 5, t = 0, p = 6, e = 2, h = 7)
    mom.assertProduction(m = 6, s = 2, t = 0, p = 5, e = 2, h = 1)
    ellie.assertResources(m = 28, s = 7, t = 6, p = 6, e = 1, h = 11)
    ellie.assertProduction(m = 3, s = 3, t = 3, p = 1, e = 1, h = 10)
    dad.assertResources(m = 29, s = 3, t = 0, p = 3, e = 2, h = 6)
    dad.assertProduction(m = 5, s = 1, t = 0, p = 2, e = 2, h = 2)
    // Mom bought 2 card(s)
    // You bought Imported Nutrients,Protected Valley
    mom.buyCards(2)
    // Dad bought 2 card(s)
    // You bought Fusion Power,Viral Enhancers
    dad.buyCards(2)
    // Ellie bought 3 card(s)
    // You bought Rad-Chem Factory,Saturn Surfing,Io Mining Industries
    ellie.buyCards(3)

    // Dad played Cupola City
    // Dad gained 3 M€ production
    // Dad lost 1 energy production
    dad.playProject("CupolaCity", 10, steel = 3) {
          // Dad placed city tile at 23
          doTask("CityTile<Hellas_4_3>")
          // Dad gained 1 steel
          // Mom added 1 Disease to Hospitals
        }
        .expect("PROD[3 M, -E], -2 S, Disease<Player1>")
    // Dad used Ants action
    dad.cardAction1("Ants") {
          // Dad removed 1 resource(s) from Mom's Psychrophiles
          doTask("-Microbe<Player1, Psychrophiles<Player1>>")
          // Dad added 1 Microbe to Ants
        }
        .expect("Microbe<Ants>")

    // Mom claimed Trader milestone
    mom.stdAction("ClaimMilestoneSA") { doTask("Trader") }
    // Mom removed 2 resource(s) from Mom's Psychrophiles
    // Mom played Protected Valley
    // Mom gained 2 M€ production
    // Mom gained 3 heat
    // Mom added 1 Animal to Ecological Zone
    mom.playProject("ProtectedValley", 9, steel = 5) {
          doTask("-2 Microbe<Psychrophiles> THEN -4 Owed")
          // Mom placed greenery tile at 03
          doTask("GreeneryTile<Hellas_1_1>")
          // Mom gained 2 plants
          // Mom gained 2 M€ from 1 ocean(s)
        }
        .expect("PROD[2 M], 3 H, Animal, TR, 2 P, -7")

    // Ellie played Lava Flows
    ellie
        .playProject("LavaFlows", 16) {
          // Ellie placed Lava Flows tile at 09
          doTask("Tile140<Hellas_2_2>")
          // Ellie gained 2 plants
          // Ellie gained 2 M€ from 1 ocean(s)
          // Ellie gained 3 M€
        }
        .expect("2 TR, 2 P, -11")
    // Ellie used Convert Plants standard action
    ellie
        .stdAction("ConvertPlantsSA") {
          // Ellie placed greenery tile at 56
          doTask("GreeneryTile<Hellas_8_7>")
          // Ellie gained 2 heat
        }
        .expect("2 H, TR")

    // Dad used Asteroid Rights action
    // Dad added 1 Asteroid to Asteroid Rights
    dad.cardAction1("AsteroidRights") { doTask("Asteroid<AsteroidRights>") }
    // Dad played Hackers
    // Dad gained 2 M€ production
    // Dad lost 1 energy production
    // Dad stole 2 M€ production from Ellie
    dad.playProject("Hackers", 3) { doTask("PROD[-2 M<Player2>]") }.expect("PROD[2 M, -E]")

    // Mom used Convert Heat standard action
    mom.stdAction("ConvertHeatSA").expect("TR")
    // Mom used Aquifer standard project
    mom.stdProject("AquiferSP") {
          // Mom placed ocean tile at 27
          doTask("OceanTile<Hellas_4_7>")
          // Mom gained 1 plant
        }
        .expect("TR, P")

    // Ellie used Convert Heat standard action
    ellie.stdAction("ConvertHeatSA").expect("TR")
    // Ellie used Robinson Industries action
    // Ellie gained 1 plant production
    ellie.cardAction1("RobinsonIndustries") { doTask("PROD[P]") }

    // NOPE: missed my chance and didn't even realize it!
    shouldThrow<RequirementException> {
      dad.playProject("DesignedMicroorganisms", 9)
    }
    // Dad played Viral Enhancers
    // Dad gained 1 plant because of Viral Enhancers
    // Dad gained 2 M€ because of Splice
    // Dad gained 2 M€ because of Splice
    dad.playProject("ViralEnhancers", 9).expect("P, -5")
    // Dad used Weather Balloons action
    // Dad removed 1 resource(s) from Dad's Weather Balloons
    // Dad gained 2 M€
    dad.cardAction2("WeatherBalloons").expect("-Floater, 2")

    // Mom used Psychrophiles action
    // Mom added 1 Microbe to Psychrophiles
    mom.cardAction1("Psychrophiles").expect("Microbe")
    // Mom used Convert Plants standard action
    mom.stdAction("ConvertPlantsSA") {
          // Mom placed greenery tile at 19
          doTask("GreeneryTile<Hellas_3_6>")
          // Mom gained 2 plants
          // Mom gained 2 M€ from 1 ocean(s)
        }
        .expect("-6 P, 2, TR")

    // Ellie used Directed Impactors action
    // Ellie removed 1 Asteroid from Directed Impactors to raise temperature 1 step
    ellie.cardAction2("DirectedImpactors").expect("-Asteroid, TemperatureStep, TR")
    // Ellie played Rad-Chem Factory
    // Ellie lost 1 energy production
    ellie.playProject("RadChemFactory", megacredits = 0, steel = 3).expect("PROD[-E], 2 TR")

    // Dad passed
    dad.pass()
    // Mom passed
    mom.pass()
    // Ellie passed
    ellie.pass()

    // Generation 7
    // First player this generation is Mom
    // We have no screencap for generation 7, so these only assert what the actual values happen to
    // be.
    mom.assertResources(m = 49, s = 2, t = 0, p = 8, e = 2, h = 5)
    mom.assertProduction(m = 8, s = 2, t = 0, p = 5, e = 2, h = 1)
    ellie.assertResources(m = 37, s = 7, t = 9, p = 2, e = 0, h = 16)
    ellie.assertProduction(m = 1, s = 3, t = 3, p = 2, e = 0, h = 10)
    dad.assertResources(m = 39, s = 2, t = 0, p = 6, e = 0, h = 10)
    dad.assertProduction(m = 10, s = 1, t = 0, p = 2, e = 0, h = 2)
    // Ellie bought 3 card(s)
    // You bought Dusk Laser Mining,Asteroid,Methane From Titan
    ellie.buyCards(3)
    // Mom bought 1 card(s)
    // You bought Supercapacitors
    mom.buyCards(1)
    // Dad bought 2 card(s)
    // You bought Algae,Bactoviral Research
    dad.buyCards(2)

    // Mom used City standard project
    mom.stdProject("CitySP") {
          // Mom placed city tile at 25
          doTask("CityTile<Hellas_4_5>")
          // Mom gained 1 steel
          // Mom added 1 Disease to Hospitals
        }
        .expect("S, Disease")
    // Mom used Convert Plants standard action
    mom.stdAction("ConvertPlantsSA") {
          // Mom placed greenery tile at 33
          doTask("GreeneryTile<Hellas_5_5>")
        }
        .expect("2 TR")

    // Ellie used Robinson Industries action
    // Ellie gained 1 energy production
    ellie.cardAction1("RobinsonIndustries").expect("PROD[E]")
    // Ellie played Dusk Laser Mining
    // Ellie gained 1 titanium production
    // Ellie lost 1 energy production
    // Ellie gained 4 titanium
    ellie.playProject("DuskLaserMining", 6).expect("PROD[T, -E], 4 T")

    // Dad used Ants action
    dad.cardAction1("Ants") {
          // Dad removed 1 resource(s) from Mom's Recyclon
          doTask("-Microbe<Player1, Recyclon<Player1>>")
          // Dad added 1 Microbe to Ants
        }
        .expect("Microbe<Ants>")
    // Dad used Asteroid Rights action
    // Dad removed 1 Asteroid from Asteroid Rights to gain 2 titanium
    dad.cardAction2("AsteroidRights") { doTask("2 T") }.expect("-Asteroid")

    // Mom used Psychrophiles action
    // Mom added 1 Microbe to Psychrophiles
    mom.cardAction1("Psychrophiles").expect("Microbe")
    // Mom played Bio Printing Facility
    mom.playProject("BioPrintingFacility", 3, steel = 2).expect("Microbe")

    // Ellie funded Space Baron award
    ellie.stdAction("FundAwardSA") { doTask("SpaceBaron") }
    // Ellie played Asteroid
    // Ellie gained 2 titanium
    // Dad drew 1 card(s)
    // You drew Sterling Vents
    ellie
        .playProject("AsteroidCard", 6, titanium = 2) {
          // Dad lost 3 plants because of Ellie
          doTask("-3 P<Player3>")
          // Ellie gained 3 M€
        }
        .expect("0 T, -ProjectCard, ProjectCard<Player3>, -3")
    // Dad used Asteroid:SP standard project
    dad.stdProject("AsteroidSP").expect("TR")
    // Dad used Convert Heat standard action
    dad.stdAction("ConvertHeatSA") {
          // Dad placed ocean tile at 14
          doTask("OceanTile<Hellas_3_1>")
          // Dad gained 1 plant
          // Dad gained 2 M€ from 1 ocean(s)
        }
        .expect("2 TR, P, 2")

    // Mom used Bio Printing Facility action
    // Mom added 1 Animal to Ecological Zone
    mom.cardAction1("BioPrintingFacility") { doTask("Animal<EcologicalZone>") }
    // Mom played Imported Nutrients
    // Mom gained 4 plants
    // Dad drew 1 card(s)
    // You drew Grass
    mom.playProject("ImportedNutrients", 14) {
          // Mom added 4 Microbe(s) to Recyclon
          doTask("4 Microbe<Recyclon>")
        }
        .expect("4 P, -ProjectCard, ProjectCard<Player3>")
    // Ellie used Convert Heat standard action
    ellie.stdAction("ConvertHeatSA")
    // Ellie used Convert Heat standard action
    ellie.stdAction("ConvertHeatSA")

    // Dad played Sterling Vents
    // Dad gained 2 energy production
    // Dad lost 2 heat production
    dad.playProject("SterlingVents", 1, steel = 2).expect("PROD[2 E, -2 H]")
    // Dad played Algae
    // Dad gained 2 plant production
    // Dad gained 1 plant
    // Dad gained 1 plant because of Viral Enhancers
    dad.playProject("Algae", 10).expect("PROD[2 P], 2 P")

    // Mom played Supercapacitors
    // Mom gained 1 M€ production
    // Mom removed 2 resource(s) from Mom's Recyclon
    // The later dashboard requires Supercapacitors' unimplemented production and payment.
    mom.exMachina("-2, -S, PROD[M, P], -2 Microbe<Recyclon>")
    // Mom used Hospitals action
    // Mom removed 1 resource(s) from Mom's Hospitals
    // Mom gained 3 M€
    mom.cardAction1("Hospitals").expect("-Disease, 3")
    mom.declineSecondAction()

    // Ellie played Io Mining Industries
    // Ellie gained 2 M€ production
    // Ellie gained 2 titanium production
    ellie.playProject("IoMiningIndustries", 6, titanium = 11).expect("PROD[2 M, 2 T]")
    // Ellie used Directed Impactors action
    // Ellie added 1 Asteroid to Directed Impactors
    ellie.cardAction1("DirectedImpactors") {
      ellie.pay(titanium = 2)
      doTask("Asteroid<DirectedImpactors>")
    }

    // Dad used Sell Patents standard project
    // Dad sold 1 patents
    dad.sellPatents(1)
    // Dad played Grass
    // Dad gained 1 plant production
    // Dad gained 3 plants
    // Dad gained 1 plant because of Viral Enhancers
    dad.playProject("Grass", 11).expect("PROD[P], 4 P")

    // Mom passed
    mom.pass()
    // Ellie passed
    ellie.pass()
    // Dad used Convert Plants standard action
    dad.stdAction("ConvertPlantsSA") {
          // Dad placed greenery tile at 15
          doTask("GreeneryTile<Hellas_3_2>")
          // Dad gained 1 plant
          // Dad gained 4 M€ from 2 ocean(s)
        }
        .expect("-7 P, 4, TR")
    // Dad used Weather Balloons action
    // Dad added 1 Floater to Weather Balloons
    dad.cardAction1("WeatherBalloons").expect("Floater")
    // Dad passed
    dad.doTask("Pass")

    // Generation 8
    // First player this generation is Ellie
    // Game20260811-dashboards-gen8.png was taken before cards were bought.
    mom.assertResources(m = 55, s = 2, t = 0, p = 10, e = 2, h = 6)
    mom.assertProduction(m = 10, s = 2, t = 0, p = 6, e = 2, h = 1)
    ellie.assertResources(m = 39, s = 10, t = 6, p = 4, e = 0, h = 10)
    ellie.assertProduction(m = 3, s = 3, t = 6, p = 2, e = 0, h = 10)
    dad.assertResources(m = 41, s = 1, t = 2, p = 8, e = 2, h = 2)
    dad.assertProduction(m = 10, s = 1, t = 0, p = 5, e = 2, h = 0)
    dad.assertCounts(9 to "ProjectCard")
    // Ellie bought 3 card(s)
    // You bought Convoy From Europa,Vesta Shipyard,Lake Marineris
    ellie.buyCards(3)
    // Dad bought 3 card(s)
    // You bought Inventors' Guild,Comet Aiming,Equatorial Magnetizer
    dad.buyCards(3)
    // Mom bought 3 card(s)
    // You bought Energy Tapping,Invention Contest,Field-Capped City
    mom.buyCards(3)
    // Ellie used Convert Heat standard action
    ellie.stdAction("ConvertHeatSA")
    // Ellie used Directed Impactors action
    // Ellie removed 1 Asteroid from Directed Impactors to raise temperature 1 step
    ellie.cardAction2("DirectedImpactors").expect("-Asteroid, TemperatureStep, TR")
    // Dad used Ants action
    dad.cardAction1("Ants") {
          // Dad removed 1 resource(s) from Mom's Recyclon
          doTask("-Microbe<Player1, Recyclon<Player1>>")
          // Dad added 1 Microbe to Ants
        }
        .expect("Microbe<Ants>")
    // Dad funded Forecaster award
    dad.stdAction("FundAwardSA") { doTask("Forecaster") }

    // Mom used Aquifer standard project
    mom.stdProject("AquiferSP") {
          // Mom placed ocean tile at 34
          doTask("OceanTile<Hellas_5_6>")
          // Mom drew 1 card(s)
          // You drew Project Inspection
        }
        .expect("TR, ProjectCard")
    // Mom used Convert Plants standard action
    mom.stdAction("ConvertPlantsSA") {
          // Mom placed greenery tile at 42
          doTask("GreeneryTile<Hellas_6_6>")
          // Mom gained 2 M€ from 1 ocean(s)
        }
        .expect("2, TR")

    // Ellie played Lake Marineris
    ellie
        .playProject("LakeMarineris", 16) {
          // Ellie placed ocean tile at 26
          doTask("OceanTile<Hellas_4_6>")
          // Ellie gained 1 plant
          // Ellie gained 4 M€ from 2 ocean(s)
          // Ellie placed ocean tile at 35
          doTask("OceanTile<Hellas_5_7>")
          // Ellie gained 3 heat
          // Ellie gained 6 M€ from 3 ocean(s)
        }
        .expect("2 TR, P, -6, 3 H")
    // Ellie played Mining Expedition
    // Ellie gained 2 steel
    ellie
        .playProject("MiningExpedition", 10) {
          // Dad lost 2 plants because of Ellie
          doTask("-2 P<Player3>")
          // Ellie gained 3 M€
        }
        .expect("TR, 2 S, -7")

    // Dad played Ice Asteroid
    // Dad drew 1 card(s)
    // You drew Robot Pollinators
    dad.playProject("IceAsteroid", 15, titanium = 2) {
          // Dad placed ocean tile at 46
          doTask("OceanTile<Hellas_7_3>")
          // Dad gained 2 titanium
          // Dad placed ocean tile at 21
          doTask("OceanTile<Hellas_4_1>")
          // Dad gained 1 plant
          // Dad gained 2 M€ from 1 ocean(s)
        }
        .expect("0 ProjectCard, 2 TR, 0 T, P, -13")
    // Dad used Asteroid Rights action
    // Dad added 1 Asteroid to Asteroid Rights
    dad.cardAction1("AsteroidRights") { doTask("Asteroid<AsteroidRights>") }

    // Mom used Bio Printing Facility action
    // Mom added 1 Animal to Ecological Zone
    mom.cardAction1("BioPrintingFacility") { doTask("Animal<EcologicalZone>") }
    // Mom played Field-Capped City
    // Mom gained 2 M€ production
    // Mom gained 1 energy production
    // Mom gained 3 plants
    mom.playProject("FieldCappedCity", 25, steel = 2) {
          // Mom placed city tile at 41
          doTask("CityTile<Hellas_6_5>")
          // Mom added 1 Disease to Hospitals
          // Mom removed 2 resource(s) from Mom's Recyclon
          doTask("-2 Microbe<Recyclon> THEN PROD[P]")
        }
        .expect("PROD[2 M, E, P], 3 P, Disease")

    // Ellie played Convoy From Europa
    // Ellie drew 1 card(s)
    // You drew Magnetic Field Generators:promo
    // Dad drew 1 card(s)
    // You drew Local Heat Trapping
    ellie
        .playProject("ConvoyFromEuropa", 7, titanium = 2) {
          // Ellie placed ocean tile at 36
          doTask("OceanTile<Hellas_5_8>")
          // Ellie gained 4 M€ from 2 ocean(s)
          // Ellie gained 3 M€
        }
        .expect("0 ProjectCard, ProjectCard<Player3>, TR, 0 M")
    // Ellie played Vesta Shipyard
    // Ellie gained 1 titanium production
    ellie.playProject("VestaShipyard", 1, titanium = 4).expect("PROD[T]")

    // Dad used Weather Balloons action
    // Dad removed 1 resource(s) from Dad's Weather Balloons
    // Dad gained 4 M€
    dad.cardAction2("WeatherBalloons").expect("-Floater, 4")
    // Dad used Sell Patents standard project
    // Dad sold 1 patents
    dad.sellPatents(1)

    // Mom used Psychrophiles action
    // Mom added 1 Microbe to Psychrophiles
    mom.cardAction1("Psychrophiles").expect("Microbe")
    // Mom used Hospitals action
    // Mom removed 1 resource(s) from Mom's Hospitals
    // Mom gained 4 M€
    mom.cardAction1("Hospitals").expect("-Disease, 4")

    // Ellie used Robinson Industries action
    // Ellie gained 1 energy production
    ellie.cardAction1("RobinsonIndustries").expect("PROD[E]")
    // Ellie played Saturn Surfing
    // Ellie added 3 Floater(s) to Saturn Surfing
    ellie.playProject("SaturnSurfing", 11).expect("3 Floater")

    // Dad played Robot Pollinators
    // Dad gained 1 plant production
    // Dad gained 3 plants
    dad.playProject("RobotPollinators", 9).expect("PROD[P], 3 P")
    // Dad used Convert Plants standard action
    // Dad placed greenery tile at 22
    // Dad gained 1 plant
    // Dad gained 4 M€ from 2 ocean(s)
    dad.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Hellas_4_2>") }.expect("-7 P, 4, TR")

    // Mom played Invention Contest
    // Mom drew 1 card(s)
    // You drew Water Import From Europa
    mom.playProject("InventionContest", 2).expect("0 ProjectCard")
    // Mom played Project Inspection
    mom.playProject("ProjectInspection", megacredits = 0) {
          // Mom used Hospitals action with Project Inspection
          doTask("UseAction1<Hospitals>")
          // Mom removed 1 resource(s) from Mom's Hospitals
          // Mom gained 4 M€
        }
        .expect("-Disease, 4")

    // Ellie used Saturn Surfing action
    // Ellie gained 3 M€
    ellie.cardAction1("SaturnSurfing").expect("3")
    // Ellie passed
    ellie.declineSecondAction()
    // Dad used Sell Patents standard project
    // Dad sold 5 patents
    dad.sellPatents(5)
    // Dad played Equatorial Magnetizer
    dad.playProject("EquatorialMagnetizer", 9, steel = 1)

    // Mom played Energy Tapping
    // Mom stole 1 energy production from Dad
    mom.playProject("EnergyTapping", 3) { doTask("PROD[-E<Player3>]") }.expect("PROD[E]")
    // Mom passed
    mom.declineSecondAction()
    // (Ellie already passed early)
    ellie.pass()
    // Dad used Equatorial Magnetizer action
    // Dad lost 1 energy production
    dad.cardAction1("EquatorialMagnetizer").expect("PROD[-E], TR")
    // Dad passed
    dad.declineSecondAction()
    // (Mom already passed early)
    mom.pass()
    // (Dad already passed early)
    dad.doTask("Pass")

    // Generation 9
    // First player this generation is Dad
    // Game20260811-dashboards-gen9.png was taken before cards were bought.
    mom.assertResources(m = 62, s = 2, t = 0, p = 12, e = 4, h = 7)
    mom.assertProduction(m = 12, s = 2, t = 0, p = 7, e = 4, h = 1)
    ellie.assertResources(m = 48, s = 15, t = 7, p = 7, e = 1, h = 15)
    ellie.assertProduction(m = 3, s = 3, t = 7, p = 2, e = 1, h = 10)
    dad.assertResources(m = 41, s = 1, t = 2, p = 9, e = 0, h = 4)
    dad.assertProduction(m = 10, s = 1, t = 0, p = 6, e = 0, h = 0)
    dad.assertCounts(5 to "ProjectCard")
    // Dad bought 1 card(s)
    // You bought Technology Demonstration
    dad.buyCards(1)
    // Mom bought 3 card(s)
    // You bought Protected Habitats,Physics Complex,Adapted Lichen
    mom.buyCards(3)
    // Ellie bought 2 card(s)
    // You bought House Printing,Beam From A Thorium Asteroid
    ellie.buyCards(2)

    // Dad used Greenery standard project
    dad.stdProject("GreenerySP") {
          // Dad placed greenery tile at 12
          doTask("GreeneryTile<Hellas_2_5>")
          // Dad gained 1 plant
        }
        .expect("P, TR")
    // Dad used Convert Plants standard action
    dad.stdAction("ConvertPlantsSA") {
          // Dad placed greenery tile at 31
          doTask("GreeneryTile<Hellas_5_3>")
        }
        .expect("TR")

    // Mom used Convert Plants standard action
    mom.stdAction("ConvertPlantsSA") {
          // Mom placed greenery tile at 49
          doTask("GreeneryTile<Hellas_7_6>")
          // Mom drew 1 card(s)
          // You drew Lightning Harvest
        }
        .expect("ProjectCard, 0 TR")
    // Mom played Protected Habitats
    mom.playProject("ProtectedHabitats", 5)

    // Ellie used City standard project
    ellie
        .stdProject("CitySP") {
          // Ellie placed city tile at 50
          doTask("CityTile<Hellas_7_7>")
          // Mom added 1 Disease to Hospitals
        }
        .expect("Disease<Player1>")
    // Ellie played House Printing
    // Ellie gained 1 steel production
    ellie.playProject("HousePrinting", steel = 4).expect("PROD[S]")

    // Dad played Technology Demonstration
    // Dad drew 2 card(s)
    // You drew Martian Rails,Acquired Company
    // Dad drew 1 card(s)
    // You drew Windmills
    dad.playProject("TechnologyDemonstration", 1, titanium = 1).expect("2 ProjectCard")
    // Dad used Sell Patents standard project
    // Dad sold 1 patents
    dad.sellPatents(1)

    // Mom removed 3 resource(s) from Mom's Psychrophiles
    // Mom played Adapted Lichen
    // Mom gained 1 plant production
    // Mom gained 3 heat
    // Mom added 1 Animal to Ecological Zone
    mom.playProject("AdaptedLichen", 3) {
          doTask("-3 Microbe<Psychrophiles> THEN -6 Owed")
        }
        .expect("PROD[P], 3 H, Animal")
    // Mom used Bio Printing Facility action
    // Mom added 1 Animal to Ecological Zone
    mom.cardAction1("BioPrintingFacility") { doTask("Animal<EcologicalZone>") }

    // Ellie used Saturn Surfing action
    // Ellie gained 2 M€
    ellie.cardAction1("SaturnSurfing").expect("2")
    // Ellie played Beam From A Thorium Asteroid
    // Ellie gained 3 energy production
    // Ellie gained 3 heat production
    ellie.playProject("BeamFromAThoriumAsteroid", 9, titanium = 7).expect("PROD[3 E, 3 H]")

    // Dad played Windmills
    // Dad gained 1 energy production
    dad.playProject("Windmills", 4, steel = 1).expect("PROD[E]")
    // Dad played Bactoviral Research
    // Dad drew 1 card(s)
    // You drew Potatoes
    // Dad gained 1 plant because of Viral Enhancers
    dad.playProject("BactoviralResearch", 10) {
          // Dad added 5 Microbe(s) to Ants
          doTask("5 Microbe<Ants>")
          // Dad gained 2 M€ because of Splice
          // Dad gained 2 M€ because of Splice
        }
        .expect("0 ProjectCard, P, -6")

    // Mom used Psychrophiles action
    // Mom added 1 Microbe to Psychrophiles
    mom.cardAction1("Psychrophiles").expect("Microbe")
    // Mom used Hospitals action
    // Mom removed 1 resource(s) from Mom's Hospitals
    // Mom gained 5 M€
    mom.cardAction1("Hospitals").expect("-Disease, 5")

    // Ellie played Magnetic Field Generators:promo
    // Ellie gained 2 plant production
    // Ellie lost 4 energy production
    ellie
        .playProject("MagneticFieldGenerators", steel = 10) {
          // Ellie placed Magnetic Field Generators tile at 29
          doTask("TileX33<Hellas_5_1>")
          // Ellie drew 1 card(s)
          // You drew Asteroid Hollowing
          // Ellie gained 2 M€ from 1 ocean(s)
        }
        .expect("PROD[2 P, -4 E], 0 ProjectCard, 2 M")
    // Ellie used Sell Patents standard project
    // Ellie sold 1 patents
    ellie.sellPatents(1)

    // Dad used Ants action
    dad.cardAction1("Ants") {
          // NOPE: Mom is protected now, so this was pointless?
          shouldThrow<DeadEndException> { doTask("-Microbe<Player1, Psychrophiles<Player1>>") }
          // Dad removed 1 resource(s) from Dad's Ants
          doTask("-Microbe<Player3, Ants<Player3>>")
          // Dad added 1 Microbe to Ants
        }
        .expect("0 Microbe<Ants>")
    // Dad used Equatorial Magnetizer action
    // Dad lost 1 energy production
    dad.cardAction1("EquatorialMagnetizer").expect("PROD[-E], TR")

    // Mom used Sell Patents standard project
    // Mom sold 3 patents
    mom.sellPatents(3)
    // Mom used City standard project
    mom.stdProject("CitySP") {
          // Mom placed city tile at 07
          doTask("CityTile<Hellas_1_5>")
          // Mom gained 1 plant
          // Mom added 1 Disease to Hospitals
        }
        .expect("P, Disease")

    // Ellie used Sell Patents standard project
    // Ellie sold 1 patents
    ellie.sellPatents(1)
    // Ellie used Sell Patents standard project
    // Ellie sold 1 patents
    ellie.sellPatents(1)

    // Dad used Asteroid Rights action
    // Dad removed 1 Asteroid from Asteroid Rights to gain 2 titanium
    dad.cardAction2("AsteroidRights") { doTask("2 T") }.expect("-Asteroid")
    // Dad used Weather Balloons action
    // Dad added 1 Floater to Weather Balloons
    dad.cardAction1("WeatherBalloons").expect("Floater")

    // Mom funded Botanist award
    mom.stdAction("FundAwardSA") { doTask("Botanist") }
    // Mom used Industrial Center action
    // Mom gained 1 steel production
    mom.cardAction1("IndustrialCenter").expect("PROD[S]")

    // Ellie passed
    ellie.pass()
    // Dad used Sell Patents standard project
    // Dad sold 6 patents
    dad.sellPatents(6)
    // Dad passed
    dad.declineSecondAction()
    // Mom passed
    mom.pass()
    dad.pass()

    // Mom converted 2 units of energy to heat
    // Final greenery placement
    dad.doTask("UseAction1<ConvertPlantsSA>")
    // Dad placed greenery tile at 18
    dad.doTask("GreeneryTile<Hellas_3_5>")
    // Dad gained 2 M€ from 1 ocean(s)
    dad.doTask("Ok")
    // Mom placed greenery tile at 13
    // Mom gained 1 plant
    mom.doTask("UseAction1<ConvertPlantsSA>")
    mom.doTask("GreeneryTile<Hellas_2_6>")
    mom.doTask("Ok")
    // Ellie placed greenery tile at 51
    ellie.doTask("UseAction1<ConvertPlantsSA>")
    ellie.doTask("GreeneryTile<Hellas_7_8>")
    ellie.doTask("Ok")

    val score = Summarizer(game)

    mom.assertCounts(42 to "TR")
    ellie.assertCounts(44 to "TR")
    dad.assertCounts(34 to "TR")

    mom.assertCounts(1 to "FirstPlace<Player1>", 0 to "SecondPlace<Player1>")
    ellie.assertCounts(1 to "FirstPlace<Player2>", 1 to "SecondPlace<Player2>")
    dad.assertCounts(1 to "FirstPlace<Player3>", 2 to "SecondPlace<Player3>")

    score.net("Milestone", "VP<Player1>") shouldBe 5
    score.net("Milestone", "VP<Player2>") shouldBe 5
    score.net("Milestone", "VP<Player3>") shouldBe 5
    score.net("FirstPlace", "VP<Player1>") shouldBe 5
    score.net("SecondPlace", "VP<Player1>") shouldBe 0
    score.net("FirstPlace", "VP<Player2>") shouldBe 5
    score.net("SecondPlace", "VP<Player2>") shouldBe 2
    score.net("FirstPlace", "VP<Player3>") shouldBe 5
    score.net("SecondPlace", "VP<Player3>") shouldBe 4
    score.net("GreeneryTile", "VP<Player1>") shouldBe 9
    score.net("GreeneryTile", "VP<Player2>") shouldBe 2
    score.net("GreeneryTile", "VP<Player3>") shouldBe 6
    score.net("CityTile", "VP<Player1>") shouldBe 9
    score.net("CityTile", "VP<Player2>") shouldBe 4
    score.net("CityTile", "VP<Player3>") shouldBe 8
    score.net("Card", "VP<Player1>") shouldBe 4
    score.net("Card", "VP<Player2>") shouldBe 12
    score.net("Card", "VP<Player3>") shouldBe 5

    score.net("EcologicalZone", "VP<Player1>") shouldBe 3
    score.net("IoMiningIndustries", "VP<Player2>") shouldBe 4
    score.net("SaturnSurfing", "VP<Player2>") shouldBe 1
    score.net("Ants", "VP<Player3>") shouldBe 4

    with(mom) {
      assertResources(m = 56, s = 5, t = 0, p = 6, e = 4, h = 13)
      assertProduction(m = 13, s = 3, t = 0, p = 8, e = 4, h = 1)
    }
    with(ellie) {
      assertResources(m = 63, s = 5, t = 7, p = 3, e = 0, h = 29)
      assertProduction(m = 4, s = 4, t = 7, p = 4, e = 0, h = 13)
    }
    with(dad) {
      assertResources(m = 57, s = 1, t = 3, p = 1, e = 0, h = 4)
      assertProduction(m = 10, s = 1, t = 0, p = 6, e = 0, h = 0)
    }
    mom.assertCounts(74 to "VP", 0 to "Victory")
    ellie.assertCounts(74 to "VP", 1 to "Victory")
    dad.assertCounts(67 to "VP", 0 to "Victory")
    // This game id was g9ea8656f1c7e
  }
}
