package dev.martianzoo.tfm.script

import dev.martianzoo.agent.Agents
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

/** Scripted two-player games, played in the game's own turn order, for the REPL to show. */
internal object SampleGames {
  /**
   * Plays the sample game through [generations] generations; 0 stops when the first action phase
   * begins. Each generation ends with the start player's World Government Terraforming and research
   * purchases.
   */
  internal fun sampleGame(generations: Int): Agents {
    var gens = generations

    val setup = OptionCodeTranslation.setup("BRVPX", 2)
    val agents = Agents(createGame(setup))
    val (p1, p2) = agents.world.actors.filterIsInstance<Player>().map { agents.tfm(it) }

    agents.tfm(ADMIN).startGame()
    p1.keepStartingProjects(5)
    p2.keepStartingProjects(4)
    p1.playCorp(cn("Manutech"))
    p2.playCorp(cn("Factorum"))

    p1.playPrelude(cn("NewPartner")) { p1.playPrelude(cn("UnmiContractor")) }
    p1.playPrelude(cn("AlliedBank"))
    p2.playPrelude(cn("AcquiredSpaceAgency"))
    p2.playPrelude(cn("IoResearchOutpost"))

    if (gens-- == 0) return agents

    // Generation 1, starting with Player1
    p1.playProject(cn("InventorsGuild"), 9)
    p1.cardAction1(cn("InventorsGuild")) { p1.buyCards(1) }
    p2.playProject(cn("ArcticAlgae"), 12)
    p2.cardAction1(cn("Factorum"))
    p1.stdProject("PowerPlantProject")
    p1.playProject(cn("BuildingIndustries"), 4, steel = 1)
    p2.playProject(cn("RotatorImpacts"), titanium = 2)
    p2.cardAction1(cn("RotatorImpacts")) { p2.pay(titanium = 2) }
    p1.pass()
    p2.playProject(cn("CarbonateProcessing"), 6)
    p2.playProject(cn("Archaebacteria"), 6)
    p2.pass()

    if (gens-- == 0) return agents
    p1.doTask("VenusStep! BY Admin")
    p1.buyCards(2)
    p2.buyCards(2)

    // Generation 2, starting with Player2
    p2.cardAction2(cn("Factorum"))
    p2.playProject(cn("MarsUniversity"), 6, steel = 1) { doTask("-ProjectCard") }
    p1.cardAction1(cn("InventorsGuild")) { p1.buyCards(1) }
    p1.playProject(cn("EarthOffice"), 1)
    p2.cardAction2(cn("RotatorImpacts"))
    p2.declineSecondAction()
    p1.playProject(cn("DevelopmentCenter"), 1, steel = 5)
    p1.stdProject("PowerPlantProject")
    p2.pass()
    p1.cardAction1(cn("DevelopmentCenter"))
    p1.playProject(cn("InvestmentLoan"), 0)
    p1.playProject(cn("DeuteriumExport"), 11)
    p1.cardAction1(cn("DeuteriumExport"))
    p1.pass()

    if (gens-- == 0) return agents
    p2.doTask("VenusStep! BY Admin")
    p1.buyCards(2)
    p2.buyCards(2)

    // Generation 3, starting with Player1
    p1.cardAction1(cn("DevelopmentCenter"))
    p1.cardAction1(cn("InventorsGuild")) { p1.buyCards(0) }
    p2.cardAction1(cn("Factorum"))
    p2.playProject(cn("AsteroidCard"), 2, steel = 0, titanium = 4) { doTask("Ok") }
    p1.playProject(cn("CorporateStronghold"), 5, steel = 3) { doTask("CityTile<Tharsis_4_6>") }
    p1.playProject(cn("OptimalAerobraking"), 7)
    p2.playProject(cn("TransNeptuneProbe"), 0, titanium = 2) {
      doTask("-ProjectCard")
    }
    p2.cardAction1(cn("RotatorImpacts")) { p2.pay(6) }
    p1.cardAction2(cn("DeuteriumExport"))
    p1.playProject(cn("ImportedGhg"), 4)
    p2.pass()
    p1.pass()

    if (gens-- == 0) return agents
    p1.doTask("VenusStep! BY Admin")
    p1.buyCards(1)
    p2.buyCards(2)

    // Generation 4, starting with Player2
    p2.cardAction2(cn("Factorum"))
    p2.playProject(cn("AquiferPumping"), 14, steel = 2)
    p1.cardAction1(cn("DevelopmentCenter"))
    p1.cardAction1(cn("InventorsGuild")) { p1.buyCards(1) }
    p2.cardAction1(cn("AquiferPumping")) {
      p2.pay(8)
      doTask("OceanTile<Tharsis_2_6>")
    }
    p2.playProject(cn("SearchForLife"), 3) { doTask("-ProjectCard") }
    p1.cardAction1(cn("DeuteriumExport"))
    p1.playProject(cn("TectonicStressPower"), 12, steel = 3)
    p2.cardAction2(cn("RotatorImpacts"))
    p2.cardAction1(cn("SearchForLife")) { doTask("Ok") }
    p1.convertHeat()
    p1.stdProject("AsteroidProject")
    p2.pass()
    p1.sellPatents(1)
    p1.playProject(cn("SpinInducingAsteroid"), 16)
    p1.pass()

    if (gens-- == 0) return agents
    p2.doTask("VenusStep! BY Admin")
    p1.buyCards(3)
    p2.buyCards(3)

    // Generation 5, starting with Player1
    p1.convertHeat()
    p1.playProject(cn("SmallAsteroid"), 10) { doTask("-2 Plant<Player2>") }
    p2.cardAction2(cn("Factorum"))
    p2.playProject(cn("DirectedImpactors"), 2, titanium = 2)
    p1.cardAction1(cn("DevelopmentCenter"))
    p1.cardAction1(cn("InventorsGuild")) { p1.buyCards(0) }
    p2.sellPatents(2)
    p2.cardAction1(cn("DirectedImpactors")) {
      p2.pay(6)
      doTask("Asteroid<RotatorImpacts>")
    }
    p1.cardAction2(cn("DeuteriumExport"))
    p1.playProject(cn("DomedCrater"), 18, steel = 3) { doTask("CityTile<Tharsis_3_4>") }
    p2.cardAction2(cn("RotatorImpacts"))
    p2.convertHeat()
    p1.playProject(cn("FueledGenerators"), 1)
    p1.declineSecondAction()
    p2.cardAction1(cn("AquiferPumping")) {
      p2.pay(6, steel = 1)
      doTask("OceanTile<Tharsis_1_4>")
    }
    p2.declineSecondAction()
    p1.pass()
    p2.pass()

    if (gens-- == 0) return agents
    p1.doTask("VenusStep! BY Admin")
    p1.buyCards(4)
    p2.buyCards(2)

    // Generation 6, starting with Player2
    p2.convertPlants { doTask("GreeneryTile<Tharsis_8_7>") }
    p2.cardAction2(cn("Factorum"))
    p1.cardAction1(cn("DevelopmentCenter"))
    p1.cardAction1(cn("InventorsGuild")) { p1.buyCards(1) }
    p2.playProject(cn("PowerPlant"), 2, steel = 1)
    p2.cardAction1(cn("AquiferPumping")) {
      p2.pay(8)
      doTask("OceanTile<Tharsis_1_5>")
    }
    p1.playProject(cn("OlympusConference"), 1, steel = 3)
    p1.playProject(cn("SisterPlanetSupport"), 4)
    p2.cardAction1(cn("DirectedImpactors")) {
      p2.pay(3, titanium = 1)
      doTask("Asteroid<RotatorImpacts>")
    }
    p2.cardAction2(cn("RotatorImpacts"))
    p1.playProject(cn("DuskLaserMining"), 8)
    p1.playProject(cn("MirandaResort"), titanium = 4)
    p2.playProject(cn("Mine"), 4)
    p2.cardAction1(cn("SearchForLife")) { doTask("Ok") }
    p1.playProject(cn("Solarnet"), 7)
    p1.playProject(cn("MiningQuota"), 5)
    p2.convertHeat()
    p2.declineSecondAction()
    p1.convertHeat()
    p1.cardAction1(cn("DeuteriumExport"))
    p2.pass()
    p1.playProject(cn("LagrangeObservatory"), 6, titanium = 1) {
      doTask("ProjectCard FROM Science<OlympusConference>")
    }
    p1.playProject(cn("VenusGovernor"), 4)
    p1.sellPatents(1)
    p1.playProject(cn("Moss"), 4)
    p1.pass()

    if (gens-- == 0) return agents
    p2.doTask("TemperatureStep! BY Admin")
    p1.buyCards(3)
    p2.buyCards(1)

    // Generation 7, starting with Player1
    p1.stdAction("ClaimMilestone<Class<Builder8>>")
    p1.cardAction1(cn("DevelopmentCenter"))
    p2.playProject(cn("EarthCatapult"), 23)
    p2.playProject(cn("InventionContest"), 0) { doTask("-ProjectCard") }
    p1.cardAction1(cn("InventorsGuild")) { p1.buyCards(0) }
    p1.playProject(cn("QuantumExtractor"), 13)
    p2.playProject(cn("BioPrintingFacility"), 1, steel = 2)
    p2.cardAction1(cn("BioPrintingFacility")) { doTask("2 Plant") }
    p1.cardAction2(cn("DeuteriumExport"))
    p1.playProject(cn("ProjectInspection"), 0) {
      doTask("UseAction<DevelopmentCenter, Action1>")
      doTask("-Energy")
    }
    p2.cardAction1(cn("Factorum"))
    p2.playProject(cn("PowerSupplyConsortium"), 3) { doTask("PROD[-Energy<Player1>]") }
    p1.playProject(cn("FloatingHabs"), 5)
    p1.cardAction1(cn("FloatingHabs")) { doTask("Floater<DeuteriumExport>") }
    p2.playProject(cn("TitaniumMine"), 5)
    p2.declineSecondAction()
    p1.convertHeat()
    p1.playProject(cn("StratosphericBirds"), 12)
    p2.pass()
    p1.cardAction1(cn("StratosphericBirds"))
    p1.pass()

    if (gens-- == 0) return agents
    p1.doTask("TemperatureStep! BY Admin")
    p1.buyCards(2)
    p2.buyCards(2)

    // Generation 8, starting with Player2
    p2.playProject(cn("AdvancedAlloys"), 7) { doTask("-ProjectCard") }
    p2.playProject(cn("AiCentral"), 13, steel = 2) { doTask("-ProjectCard") }
    p1.playProject(cn("ExtractorBalloons"), 21)
    p1.cardAction1(cn("DevelopmentCenter"))
    p2.cardAction1(cn("AiCentral"))
    p2.cardAction1(cn("DirectedImpactors")) {
      p2.pay(2, titanium = 1)
      doTask("Asteroid<RotatorImpacts>")
    }
    p1.playProject(cn("SulphurExports"), 13, titanium = 2)
    p1.cardAction2(cn("ExtractorBalloons"))
    p2.cardAction2(cn("RotatorImpacts"))
    p2.playProject(cn("IshtarMining"), 3)
    p1.playProject(cn("MoholeLake"), 7, steel = 12) { doTask("OceanTile<Tharsis_5_5>") }
    p1.stdAction("ClaimMilestone<Class<Terraformer35>>")
    p2.convertHeat()
    p2.convertPlants { doTask("GreeneryTile<Tharsis_8_6>") }
    p1.cardAction1(cn("InventorsGuild")) { p1.buyCards(1) }
    p1.cardAction1(cn("DeuteriumExport"))
    p2.cardAction1(cn("BioPrintingFacility")) { doTask("2 Plant") }
    p2.declineSecondAction()
    p1.convertHeat()
    p1.convertPlants { doTask("GreeneryTile<Tharsis_3_5>") }
    p2.pass()
    p1.cardAction1(cn("StratosphericBirds"))
    p1.cardAction1(cn("MoholeLake")) { doTask("Animal<StratosphericBirds>") }
    p1.pass()

    if (gens == 0) return agents
    p2.doTask("OxygenStep! BY Admin")
    p1.buyCards(3)
    p2.buyCards(2)

    return agents
  }
}
