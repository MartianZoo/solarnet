package dev.martianzoo.tfm.script

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.script.OptionCodeTranslation
import dev.martianzoo.script.createGame
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow

internal object SampleGames {
  internal fun sampleGame(generations: Int): World {
    var gens = generations

    val setup = OptionCodeTranslation.setup("BRVPXT", 2)
    val game = createGame(setup)
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    TfmWorkflow.Manual(game).setupPhase()
    engine.phase("Corporation")
    p1.playCorp(cn("Manutech"), 5)
    p2.playCorp(cn("Factorum"), 4)

    engine.phase("Prelude")
    p1.playPrelude(cn("NewPartner")) { p1.playPrelude(cn("UnmiContractor")) }
    p1.playPrelude(cn("AlliedBank"))
    p2.playPrelude(cn("AcquiredSpaceAgency"))
    p2.playPrelude(cn("IoResearchOutpost"))

    engine.phase("Action")
    if (gens-- == 0) return game

    p1.playProject(cn("InventorsGuild"), 9)
    p2.playProject(cn("ArcticAlgae"), 12)
    p1.cardAction1(cn("InventorsGuild")) { doTask("BuyCard") }
    p2.cardAction1(cn("Factorum"))
    p1.stdProject("PowerPlantSP")
    p1.playProject(cn("BuildingIndustries"), 4, steel = 1)
    p2.playProject(cn("RotatorImpacts"), titanium = 2)
    p2.cardAction1(cn("RotatorImpacts")) { p2.pay(titanium = 2) }
    p2.playProject(cn("CarbonateProcessing"), 6)
    p2.playProject(cn("Archaebacteria"), 6)

    if (gens-- == 0) return game
    engine.nextGeneration(2, 2)

    p2.cardAction2(cn("Factorum"))
    p2.playProject(cn("MarsUniversity"), 6, steel = 1) {
      doTask("-ProjectCard")
    }
    p1.cardAction1(cn("InventorsGuild")) { doTask("BuyCard") }
    p1.playProject(cn("EarthOffice"), 1)
    p2.cardAction2(cn("RotatorImpacts"))
    p1.playProject(cn("DevelopmentCenter"), 1, steel = 5)
    p1.stdProject("PowerPlantSP")
    p1.cardAction1(cn("DevelopmentCenter"))
    p1.playProject(cn("InvestmentLoan"), 0)
    p1.playProject(cn("DeuteriumExport"), 11)
    p1.cardAction1(cn("DeuteriumExport"))

    if (gens-- == 0) return game
    engine.nextGeneration(2, 2)

    p1.cardAction1(cn("DevelopmentCenter"))
    p1.cardAction1(cn("InventorsGuild")) { doTask("Ok") }
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

    if (gens-- == 0) return game
    engine.nextGeneration(1, 2)

    p2.cardAction2(cn("Factorum"))
    p2.playProject(cn("AquiferPumping"), 14, steel = 2)
    p1.cardAction1(cn("DevelopmentCenter"))
    p1.cardAction1(cn("InventorsGuild")) { doTask("BuyCard") }
    p2.cardAction1(cn("AquiferPumping")) {
      p2.pay(8)
      doTask("OceanTile<Tharsis_2_6>")
    }
    p2.playProject(cn("SearchForLife"), 3) {
      doTask("-ProjectCard")
    }
    p1.cardAction1(cn("DeuteriumExport"))
    p1.playProject(cn("TectonicStressPower"), 12, steel = 3)
    p2.cardAction2(cn("RotatorImpacts"))
    p2.cardAction1(cn("SearchForLife")) { doTask("Ok") }
    p1.stdAction("ConvertHeatSA")
    p1.stdProject("AsteroidSP")
    p1.sellPatents(1)
    p1.playProject(cn("SpinInducingAsteroid"), 16)

    if (gens-- == 0) return game
    engine.nextGeneration(3, 3)

    p1.stdAction("ConvertHeatSA")
    p1.playProject(cn("SmallAsteroid"), 10) { doTask("-2 Plant<Player2>") }
    p2.cardAction2(cn("Factorum"))
    p2.playProject(cn("DirectedImpactors"), 2, titanium = 2)
    p1.cardAction1(cn("DevelopmentCenter"))
    p1.cardAction1(cn("InventorsGuild")) { doTask("Ok") }
    p2.sellPatents(2)
    p1.cardAction2(cn("DeuteriumExport"))
    p1.playProject(cn("DomedCrater"), 18, steel = 3) { doTask("CityTile<Tharsis_3_4>") }
    p2.cardAction1(cn("DirectedImpactors")) {
      p2.pay(6)
      doTask("Asteroid<RotatorImpacts>")
    }
    p2.cardAction2(cn("RotatorImpacts"))
    p1.playProject(cn("FueledGenerators"), 1)
    p2.stdAction("ConvertHeatSA")
    p2.cardAction1(cn("AquiferPumping")) {
      p2.pay(6, steel = 1)
      doTask("OceanTile<Tharsis_1_4>")
    }

    if (gens-- == 0) return game
    engine.nextGeneration(4, 2)

    p2.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_8_7>") }
    p2.cardAction2(cn("Factorum"))
    p1.cardAction1(cn("DevelopmentCenter"))
    p1.cardAction1(cn("InventorsGuild")) { doTask("BuyCard") }
    p2.playProject(cn("PowerPlantCard"), 2, steel = 1)
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
    p2.stdAction("ConvertHeatSA")
    p1.stdAction("ConvertHeatSA")
    p1.cardAction1(cn("DeuteriumExport"))
    p1.playProject(cn("LagrangeObservatory"), 6, titanium = 1) {
      doTask("ProjectCard FROM Science<OlympusConference>")
    }
    p1.playProject(cn("VenusGovernor"), 4)
    p1.sellPatents(1)
    p1.playProject(cn("Moss"), 4)

    if (gens-- == 0) return game
    engine.nextGeneration(3, 1)

    p1.stdAction("ClaimMilestoneSA") { doTask("Builder") }
    p1.cardAction1(cn("DevelopmentCenter"))
    p2.playProject(cn("EarthCatapult"), 23)
    p2.playProject(cn("InventionContest"), 0) {
      doTask("-ProjectCard")
    }
    p1.cardAction1(cn("InventorsGuild")) { doTask("Ok") }
    p1.playProject(cn("QuantumExtractor"), 13)
    p2.playProject(cn("BioPrintingFacility"), 1, steel = 2)
    p2.cardAction1(cn("BioPrintingFacility"))
    p1.cardAction2(cn("DeuteriumExport"))
    p1.playProject(cn("ProjectInspection"), 0) { doTask("UseAction1<DevelopmentCenter>") }
    p2.cardAction1(cn("Factorum"))
    p2.playProject(cn("PowerSupplyConsortium"), 3) { doTask("PROD[-E<Player1>]") }
    p1.playProject(cn("FloatingHabs"), 5)
    p1.cardAction1(cn("FloatingHabs")) { doTask("Floater<DeuteriumExport>") }
    p2.playProject(cn("TitaniumMine"), 5)
    p1.stdAction("ConvertHeatSA")
    p1.playProject(cn("StratosphericBirds"), 12)
    p1.cardAction1(cn("StratosphericBirds"))

    if (gens-- == 0) return game
    engine.nextGeneration(2, 2)

    p2.playProject(cn("AdvancedAlloys"), 7) {
      doTask("-ProjectCard")
    }
    p2.playProject(cn("AiCentral"), 13, steel = 2) {
      doTask("-ProjectCard")
    }
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
    p1.stdAction("ClaimMilestoneSA") { doTask("Terraformer") }
    p2.stdAction("ConvertHeatSA")
    p2.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_8_6>") }
    p1.cardAction1(cn("InventorsGuild")) { doTask("BuyCard") }
    p1.cardAction1(cn("DeuteriumExport"))
    p2.cardAction1(cn("BioPrintingFacility")) { doTask("2 Plant") }
    p1.stdAction("ConvertHeatSA")
    p1.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_3_5>") }
    p1.cardAction1(cn("StratosphericBirds"))
    p1.cardAction1(cn("MoholeLake")) { doTask("Animal<StratosphericBirds>") }

    if (gens == 0) return game
    engine.nextGeneration(3, 2)

    p1.cardAction1(cn("DevelopmentCenter"))
    p1.cardAction1(cn("InventorsGuild")) { doTask("BuyCard") }
    p2.playProject(cn("DeimosDown"), 9, titanium = 5) {
      p2.doTask("OceanTile<Tharsis_6_7>")
      p2.doTask("TileX31<Tharsis_2_5>")
      p2.doTask("-4 Plant<Player1>")
    }
    p2.cardAction1(cn("AiCentral"))
    p1.stdAction("ConvertHeatSA")
    p1.stdAction("ConvertHeatSA")
    p2.cardAction1(cn("AquiferPumping")) {
      p2.pay(steel = 3)
      doTask("OceanTile<Tharsis_5_6>")
    }
    p2.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_9_7>") }
    p1.playProject(cn("RegoPlastics"), 10)
    p1.playProject(cn("SfMemorial"), 1, steel = 2)
    p2.stdAction("ClaimMilestoneSA") { doTask("Gardener") }
    p2.cardAction1(cn("DirectedImpactors")) {
      p2.pay(6)
      doTask("Asteroid<RotatorImpacts>")
    }
    p1.cardAction1(cn("FloatingHabs")) { doTask("Floater<ExtractorBalloons>") }
    p1.cardAction2(cn("ExtractorBalloons"))
    p2.playProject(cn("EcologicalZone"), 10) { doTask("Tile128<Tharsis_4_5>") }
    p2.playProject(cn("Harvest"), 2)
    p1.playProject(cn("NoctisFarming"), 1, steel = 3)
    p1.cardAction2(cn("DeuteriumExport"))
    p2.cardAction1(cn("BioPrintingFacility")) { doTask("Animal<EcologicalZone>") }
    p2.cardAction2(cn("RotatorImpacts"))
    p1.cardAction1(cn("MoholeLake")) { doTask("Animal<StratosphericBirds>") }
    p1.cardAction1(cn("StratosphericBirds"))
    p2.cardAction2(cn("Factorum"))
    p2.playProject(cn("NaturalPreserve"), 1, steel = 2) {
      doTask("-ProjectCard")
      doTask("Tile044<Tharsis_3_1>")
    }
    p1.sellPatents(3)
    p1.playProject(cn("WaterToVenus"), 4, titanium = 1)
    p2.sellPatents(2)
    p2.playProject(cn("KelpFarming"), 15)
    p1.playProject(cn("Trees"), 13)
    p2.cardAction1(cn("SearchForLife")) { doTask("Ok") }
    p1.playProject(cn("VenusianInsects"), 5)
    p1.cardAction1(cn("VenusianInsects"))

    return game
  }
}
