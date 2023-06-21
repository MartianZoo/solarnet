package dev.martianzoo.tfm.repl

import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Game
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm

object SampleGames {
  fun sampleGame(generations: Int): Game {
    var gens = generations

    val game = Engine.newGame(GameSetup(Canon, "BRMVPXT", 2))
    val engine = game.tfm(ENGINE)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    engine.phase("Corporation")
    p1.playCorp("Manutech", 5)
    p2.playCorp("Factorum", 4)

    engine.phase("Prelude")
    p1.playPrelude("NewPartner") { p1.playPrelude("UnmiContractor") }
    p1.playPrelude("AlliedBank")
    p2.playPrelude("AcquiredSpaceAgency")
    p2.playPrelude("IoResearchOutpost")

    engine.phase("Action")
    if (gens-- == 0) return game

    p1.playProject("InventorsGuild", 9)
    p2.playProject("ArcticAlgae", 12)
    p1.cardAction1("InventorsGuild") { doTask("BuyCard") }
    p2.cardAction1("Factorum")
    p1.stdProject("PowerPlantSP")
    p1.playProject("BuildingIndustries", 4, steel = 1)
    p2.playProject("RotatorImpacts", titanium = 2)
    p2.cardAction1("RotatorImpacts") {
      doTask("2 Pay<Class<T>> FROM T")
      doFirstTask("Ok")
    }
    p2.playProject("CarbonateProcessing", 6)
    p2.playProject("Archaebacteria", 6)

    if (gens-- == 0) return game
    engine.nextGeneration(2, 2)

    p2.cardAction2("Factorum")
    p2.playProject("MarsUniversity", 6, steel = 1)
    p1.cardAction1("InventorsGuild") { doFirstTask("BuyCard") }
    p1.playProject("EarthOffice", 1)
    p2.cardAction2("RotatorImpacts")
    p1.playProject("DevelopmentCenter", 1, steel = 5)
    p1.stdProject("PowerPlantSP")
    p1.cardAction1("DevelopmentCenter")
    p1.playProject("InvestmentLoan", 0)
    p1.playProject("DeuteriumExport", 11)
    p1.cardAction1("DeuteriumExport")

    if (gens-- == 0) return game
    engine.nextGeneration(2, 2)

    p1.cardAction1("DevelopmentCenter")
    p1.cardAction1("InventorsGuild") { doFirstTask("Ok") }
    p2.cardAction1("Factorum")
    p2.playProject("AsteroidCard", 2, steel = 0, titanium = 4) { doFirstTask("Ok") }
    p1.playProject("CorporateStronghold", 5, steel = 3) { doTask("CityTile<Tharsis_4_6>") }
    p1.playProject("OptimalAerobraking", 7)
    p2.playProject("TransNeptuneProbe", 0, titanium = 2)
    p2.cardAction1("RotatorImpacts") {
      doFirstTask("6 Pay<Class<M>> FROM M")
      doFirstTask("Ok")
    }
    p1.cardAction2("DeuteriumExport")
    p1.playProject("ImportedGhg", 4)

    if (gens-- == 0) return game
    engine.nextGeneration(1, 2)

    p2.cardAction2("Factorum")
    p2.playProject("AquiferPumping", 14, steel = 2)
    p1.cardAction1("DevelopmentCenter")
    p1.cardAction1("InventorsGuild") { doTask("BuyCard") }
    p2.cardAction1("AquiferPumping") {
      doTask("8 Pay<Class<M>> FROM M")
      doTask("OceanTile<Tharsis_2_6>")
      doTask("Ok")
    }
    p2.playProject("SearchForLife", 3)
    p1.cardAction1("DeuteriumExport")
    p1.playProject("TectonicStressPower", 12, steel = 3)
    p2.cardAction2("RotatorImpacts")
    p2.cardAction1("SearchForLife") { doTask("Ok") }
    p1.stdAction("ConvertHeatSA")
    p1.stdProject("AsteroidSP")
    p1.sellPatents(1)
    p1.playProject("SpinInducingAsteroid", 16)

    if (gens-- == 0) return game
    engine.nextGeneration(3, 3)

    p1.stdAction("ConvertHeatSA")
    p1.playProject("SmallAsteroid", 10) { doTask("-2 Plant<P2>") }
    p2.cardAction2("Factorum")
    p2.playProject("DirectedImpactors", 2, titanium = 2)
    p1.cardAction1("DevelopmentCenter")
    p1.cardAction1("InventorsGuild") { doTask("Ok") }
    p2.sellPatents(2)
    p1.cardAction2("DeuteriumExport")
    p1.playProject("DomedCrater", 18, steel = 3) { doTask("CityTile<Tharsis_3_4>") }
    p2.cardAction1("DirectedImpactors") {
      doTask("6 Pay<Class<M>> FROM M")
      doTask("Ok")
      doTask("Asteroid<RotatorImpacts>")
    }
    p2.cardAction2("RotatorImpacts")
    p1.playProject("FueledGenerators", 1)
    p2.stdAction("ConvertHeatSA")
    p2.cardAction1("AquiferPumping") {
      doTask("6 Pay<Class<M>> FROM M")
      doTask("Pay<Class<S>> FROM S")
      doTask("OceanTile<Tharsis_1_4>")
    }

    if (gens-- == 0) return game
    engine.nextGeneration(4, 2)

    p2.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_8_7>") }
    p2.cardAction2("Factorum")
    p1.cardAction1("DevelopmentCenter")
    p1.cardAction1("InventorsGuild") { doTask("BuyCard") }
    p2.playProject("PowerPlantCard", 2, steel = 1)
    p2.cardAction1("AquiferPumping") {
      doTask("8 Pay<Class<M>> FROM M")
      doTask("Ok")
      doTask("OceanTile<Tharsis_1_5>")
    }
    p1.playProject("OlympusConference", 1, steel = 3)
    p1.playProject("SisterPlanetSupport", 4)
    p2.cardAction1("DirectedImpactors") {
      doTask("3 Pay<Class<M>> FROM M")
      doTask("1 Pay<Class<T>> FROM T")
      doTask("Asteroid<RotatorImpacts>")
    }
    p2.cardAction2("RotatorImpacts")
    p1.playProject("DuskLaserMining", 8)
    p1.playProject("MirandaResort", titanium = 4)
    p2.playProject("Mine", 4)
    p2.cardAction1("SearchForLife") { doTask("Ok") }
    p1.playProject("Solarnet", 7)
    p1.playProject("MiningQuota", 5)
    p2.stdAction("ConvertHeatSA")
    p1.stdAction("ConvertHeatSA")
    p1.cardAction1("DeuteriumExport")
    p1.playProject("LagrangeObservatory", 6, titanium = 1) {
      doTask("ProjectCard FROM Science<OlympusConference>")
    }
    p1.playProject("VenusGovernor", 4)
    p1.sellPatents(1)
    p1.playProject("Moss", 4)

    if (gens-- == 0) return game
    engine.nextGeneration(3, 1)

    p1.stdAction("ClaimMilestoneSA") { doTask("Builder") }
    p1.cardAction1("DevelopmentCenter")
    p2.playProject("EarthCatapult", 23)
    p2.playProject("InventionContest", 0)
    p1.cardAction1("InventorsGuild") { doTask("Ok") }
    p1.playProject("QuantumExtractor", 13)
    p2.playProject("BioPrintingFacility", 1, steel = 2)
    p2.cardAction1("BioPrintingFacility")
    p1.cardAction2("DeuteriumExport")
    p1.playProject("ProjectInspection", 0) { doTask("UseAction1<DevelopmentCenter>") }
    p2.cardAction1("Factorum")
    p2.playProject("PowerSupplyConsortium", 3) { doTask("PROD[-E<P1>]") }
    p1.playProject("FloatingHabs", 5)
    p1.cardAction1("FloatingHabs") { doTask("Floater<DeuteriumExport>") }
    p2.playProject("TitaniumMine", 5)
    p1.stdAction("ConvertHeatSA")
    p1.playProject("StratosphericBirds", 12)
    p1.cardAction1("StratosphericBirds")

    if (gens-- == 0) return game
    engine.nextGeneration(2, 2)

    p2.playProject("AdvancedAlloys", 7)
    p2.playProject("AiCentral", 13, steel = 2)
    p1.playProject("ExtractorBalloons", 21)
    p1.cardAction1("DevelopmentCenter")
    p2.cardAction1("AiCentral")
    p2.cardAction1("DirectedImpactors") {
      doTask("1 Pay<Class<T>> FROM T")
      doTask("2 Pay<Class<M>> FROM M")
      doTask("Asteroid<RotatorImpacts>")
    }
    p1.playProject("SulphurExports", 13, titanium = 2)
    p1.cardAction2("ExtractorBalloons")
    p2.cardAction2("RotatorImpacts")
    p2.playProject("IshtarMining", 3)
    p1.playProject("MoholeLake", 7, steel = 12) { doTask("OceanTile<Tharsis_5_5>") }
    p1.stdAction("ClaimMilestoneSA") { doTask("Terraformer") }
    p2.stdAction("ConvertHeatSA")
    p2.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_8_6>") }
    p1.cardAction1("InventorsGuild") { doTask("BuyCard") }
    p1.cardAction1("DeuteriumExport")
    p2.cardAction1("BioPrintingFacility") { doTask("2 Plant") }
    p1.stdAction("ConvertHeatSA")
    p1.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_3_5>") }
    p1.cardAction1("StratosphericBirds")
    p1.cardAction1("MoholeLake") { doTask("Animal<StratosphericBirds>") }

    if (gens == 0) return game
    engine.nextGeneration(3, 2)

    p1.cardAction1("DevelopmentCenter")
    p1.cardAction1("InventorsGuild") { doTask("BuyCard") }
    p2.playProject("DeimosDownPromo", 9, titanium = 5) {
      p2.doTask("OceanTile<Tharsis_6_7>")
      p2.doTask("DdTile<Tharsis_2_5>")
      p2.doTask("-4 Plant<P1>")
    }
    p2.cardAction1("AiCentral")
    p1.stdAction("ConvertHeatSA")
    p1.stdAction("ConvertHeatSA")
    p2.cardAction1("AquiferPumping") {
      doTask("3 Pay<Class<S>> FROM S")
      doTask("Ok")
      doTask("OceanTile<Tharsis_5_6>")
    }
    p2.stdAction("ConvertPlantsSA") { doTask("GreeneryTile<Tharsis_9_7>") }
    p1.playProject("RegoPlastics", 10)
    p1.playProject("SfMemorial", 1, steel = 2)
    p2.stdAction("ClaimMilestoneSA") { doTask("Gardener") }
    p2.cardAction1("DirectedImpactors") {
      doTask("6 Pay<Class<M>> FROM M")
      doTask("Asteroid<RotatorImpacts>")
      doTask("Ok")
    }
    p1.cardAction1("FloatingHabs") { doTask("Floater<ExtractorBalloons>") }
    p1.cardAction2("ExtractorBalloons")
    p2.playProject("EcologicalZone", 10) { doTask("EzTile<Tharsis_4_5>") }
    p2.playProject("Harvest", 2)
    p1.playProject("NoctisFarming", 1, steel = 3)
    p1.cardAction2("DeuteriumExport")
    p2.cardAction1("BioPrintingFacility") { doTask("Animal<EcologicalZone>") }
    p2.cardAction2("RotatorImpacts")
    p1.cardAction1("MoholeLake") { doTask("Animal<StratosphericBirds>") }
    p1.cardAction1("StratosphericBirds")
    p2.cardAction2("Factorum")
    p2.playProject("NaturalPreserve", 1, steel = 2) { doTask("NpTile<Tharsis_3_1>") }
    p1.sellPatents(3)
    p1.playProject("WaterToVenus", 4, titanium = 1)
    p2.sellPatents(2)
    p2.playProject("KelpFarming", 15)
    p1.playProject("Trees", 13)
    p2.cardAction1("SearchForLife") { doTask("Ok") }
    p1.playProject("VenusianInsects", 5)
    p1.cardAction1("VenusianInsects")

    return game
  }
}
