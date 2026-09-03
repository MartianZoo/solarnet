package dev.martianzoo.tfm.web.gameviewer.games

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.web.gameviewer.RecordedGame
import dev.martianzoo.tfm.web.gameviewer.cardnames.*

private val fakeL1TradeTerminal = cn("FakeL1TradeTerminal")
private val fakeL1TradeTerminalDefinition =
    parseClasses(
        """
        CLASS FakeL1TradeTerminal : ActiveCard<Class<ProjectCard>> {
          cost = 25
          This:: SpaceTag<This>
          This: Floater<FloatingHabs>, Floater<AerialMappers>, Floater<FloatingRefinery>
          Trade<ColonyTile>:: TradeBarrier<ColonyTile>
          Trade<ColonyTile>: (2 ColonyProduction<ColonyTile> OR Ok) THEN -TradeBarrier<ColonyTile>
          End: 2 VictoryPoint
        }
        """,
    )
private val otbGame20260828Catalog = Canon.withNonstandardClasses(fakeL1TradeTerminalDefinition)

public class OtbGame20260828 : RecordedGame() {
  private val colonyTiles = listOf("Ganymede", "Io", "Luna", "Miranda", "Titan")
  protected override val catalog: TfmCatalog = otbGame20260828Catalog

  protected override val config: GameConfig =
      GameConfig(
          """
          CimmeriaMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, ColoniesExpansion, PromoCardPack
          FakeL1TradeTerminal

          Engineer, Fundraiser, Landshaper, Merchant, Metallurgist
          Benefactor, EstateDealer, Industrialist, Metropolist, SpaceBaron
          ${colonyTiles.joinToString()}
          """,
          "Green",
          "Blue",
          "Yellow",
      )

  protected override fun play() {
    TfmWorkflow.Auto(game).launch()
    val green = game.tfm(Player.PLAYER1)
    val blue = game.tfm(Player.PLAYER2)
    val yellow = game.tfm(Player.PLAYER3)

    green.playCorp(PalladinShipping, 4)
    blue.playCorp(Celestic, 5)
    yellow.playCorp(PointLuna, 5)

    green.turn {
      playPrelude(Biofuels)
      playPrelude(Supplier)
    }
    blue.turn {
      playPrelude(GreatAquifer) {
        doTask("OceanTile<Cimmeria_2_1>")
        doTask("OceanTile<Cimmeria_9_5>")
      }
      playPrelude(AtmosphericEnhancers) { doTask("2 VenusStep") }
    }
    yellow.turn {
      playPrelude(OrbitalConstructionYard)
      playPrelude(EarlyColonization) { doTask("Colony<Luna>") }
    }

    green.turn { playProject(TitanShuttles, 14, titanium = 3) }
    blue.turn {
      stdAction("DoRequiredActions")
      playProject(LocalShading, 4)
    }
    yellow.turn { playProject(RimFreighters, 1, titanium = 1) }
    green.turn { cardAction1(TitanShuttles) { addCardResources(TitanShuttles) } }
    blue.turn {
      playProject(FloaterTechnology, 7)
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    yellow.turn { playProject(BusinessNetwork, 4) }

    yellow.exMachina("PROD[MC]")

    green.pass()
    blue.turn {
      playProject(NitriteReducingBacteria, 11)
      cardAction2(LocalShading)
    }
    yellow.turn { cardAction1(BusinessNetwork) { buyCards(0) } }
    blue.turn {
      cardAction2(NitriteReducingBacteria)
      cardAction1(Celestic) { addCardResources(LocalShading) }
    }
    yellow.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }
      playProject(FueledGenerators, 1)
    }
    blue.pass()
    yellow.pass()

    green.wgt("OxygenStep")

    blue.buyCards(2)
    yellow.buyCards(2)
    green.buyCards(3)

    blue.turn { cardAction2(LocalShading) }
    yellow.turn { cardAction1(BusinessNetwork) { buyCards(1) } }
    green.turn {
      playProject(MinorityRefuge, 2, titanium = 1) {
        doTask("Colony<Titan>")
        addCardResources(TitanShuttles)
      }
      stdAction("TradeAction", 2) {
        doTask("Trade<Titan>")
        addCardResources(TitanShuttles, 3)
        addCardResources(TitanShuttles)
      }
    }
    blue.turn { playProject(JetStreamMicroscrappers, 12) }
    yellow.turn { playProject(SolarLogistics, 5, titanium = 5) }
    green.turn { cardAction2(TitanShuttles, x = 9) }
    blue.turn { playProject(Dirigibles, 11) }
    yellow.turn { playProject(OptimalAerobraking, 7) }
    green.turn { playProject(SpaceElevator, titanium = 9) }
    blue.turn { cardAction1(NitriteReducingBacteria) }
    yellow.turn { playProject(ImportOfAdvancedGhg, 7) }
    green.turn { cardAction1(SpaceElevator) }
    blue.turn { playProject(Potatoes, 2) }
    yellow.pass()
    green.turn {
      playProject(ResearchOutpost, 12, steel = 3) {
        placeTile(3, 3)
        doTask("Colony<Luna>")
      }
    }
    blue.turn { cardAction1(FloaterTechnology) { addCardResources(LocalShading) } }
    green.turn { playProject(PeroxidePower, 6) }
    blue.turn {
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(JetStreamMicroscrappers) }
    }
    green.pass()
    blue.turn { cardAction2(JetStreamMicroscrappers) }
    blue.pass()

    blue.wgt("VenusStep")

    yellow.buyCards(3)
    green.buyCards(2)
    blue.buyCards(1)

    yellow.turn {
      stdAction("TradeAction", 3) { doTask("Trade<Io>") }
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    green.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Ganymede>") }
      convertPlants { placeTile(3, 2) }
    }
    blue.turn { playProject(IoSulphurResearch, 17) { doTask("3 ProjectCard") } }
    yellow.turn { playProject(CarbonateProcessing, 6) }
    green.turn {
      playProject(MiningArea, 3) { placeTile(4, 3) }
      claimMilestone(cn("Landshaper"))
    }
    blue.turn { playProject(MarsUniversity, 8) { declineTask() } }
    yellow.turn { playProject(PowerInfrastructure, 4) }
    green.turn { cardAction1(SpaceElevator) }
    blue.turn { cardAction1(NitriteReducingBacteria) }
    yellow.turn { playProject(FusionPower, 14) }
    green.turn { cardAction1(TitanShuttles) { addCardResources(TitanShuttles) } }
    blue.turn { cardAction2(LocalShading) }
    yellow.turn { playProject(HousePrinting, 10) }
    green.turn { cardAction1(PalladinShipping) }
    blue.turn { cardAction1(FloaterTechnology) { addCardResources(LocalShading) } }
    yellow.turn {
      convertHeat()
      convertHeat()
    }
    green.pass()
    blue.turn {
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(JetStreamMicroscrappers) }
    }
    yellow.pass()
    blue.turn { cardAction2(JetStreamMicroscrappers) }
    blue.pass()

    yellow.wgt("VenusStep")

    green.buyCards(3)
    blue.buyCards(3)
    yellow.buyCards(1)

    green.turn {
      playProject(ResearchColony, 16, titanium = 1) { doTask("Colony<Luna>") }
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }
    }
    blue.turn {
      playProject(IceMoonColony, 17, titanium = 2) {
        placeTile(8, 9)
        doTask("Colony<Titan>")
        addCardResources(JetStreamMicroscrappers, 3)
      }
    }
    yellow.turn { cardAction1(BusinessNetwork) { buyCards(0) } }
    yellow.exMachina("-3 MC, PROD[-MC]")
    green.turn { cardAction1(SpaceElevator) }
    blue.turn { cardAction1(NitriteReducingBacteria) }
    yellow.turn { playProject(LavaTubeSettlement, 15) { placeTile(6, 2) } }
    green.turn { playProject(TowingAComet, 22) { placeTile(7, 9) } }
    blue.turn { cardAction2(LocalShading) }
    yellow.turn { sellPatents(1) }
    green.turn { cardAction1(TitanShuttles) { addCardResources(TitanShuttles) } }
    blue.turn { cardAction1(FloaterTechnology) { addCardResources(LocalShading) } }
    yellow.turn { cardAction1(PowerInfrastructure, x = 1) }
    green.pass()
    blue.turn { cardAction2(JetStreamMicroscrappers) }
    yellow.turn { playProject(WaterToVenus, titanium = 3) }
    blue.turn {
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(Dirigibles) }
    }
    yellow.pass()
    blue.pass()

    green.wgt("OceanTile<Cimmeria_1_1>")

    blue.buyCards(2)
    yellow.buyCards(3)
    green.buyCards(3)

    blue.turn { playProject(ProtectedValley, 23) { placeTile(9, 9) } }
    yellow.turn {
      playProject(SubterraneanReservoir, 11) { placeTile(1, 5) }
      claimMilestone(cn("Merchant"))
    }
    green.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Titan>")
        doWithoutAutoExec(green) {
          doTask("3 Floater<$TitanShuttles>")
          doTask("Floater<$TitanShuttles>")
          green.selectTask("Floater<Blue>.")
          blue.doTask("Floater<$Dirigibles>")
        }
      }
    }
    blue.turn {
      playProject(Stratopolis, 16) {
        doTask("2 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
      }
    }
    yellow.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    green.turn { cardAction1(SpaceElevator) }
    blue.turn { cardAction2(NitriteReducingBacteria) }
    yellow.turn {
      playProject(ElectroCatapult, 13, steel = 2)
      cardAction1(ElectroCatapult)
    }
    green.turn { cardAction2(TitanShuttles, x = 8) }
    blue.turn { cardAction2(LocalShading) }
    yellow.turn { playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$FusionPower>") } }
    green.turn { playProject(IoMiningIndustries, 10, titanium = 10) }
    blue.turn { cardAction2(JetStreamMicroscrappers) }
    yellow.turn {
      convertHeat()
      convertHeat()
    }
    green.turn { playProject(DirectedImpactors, 7) }
    blue.turn { cardAction1(FloaterTechnology) { addCardResources(Dirigibles) } }
    yellow.turn {
      sellPatents(1)
      playProject(FloatingHabs, 5)
    }
    green.turn { playProject(ReleaseOfInertGases, 13) }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Dirigibles, 2) } }
    yellow.turn {
      sellPatents(2)
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    green.pass()
    blue.turn {
      playProject(
          Extremophiles,
          payment = {
            doTask("PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          },
      )
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
    }
    yellow.pass()
    blue.turn {
      cardAction1(Celestic) { addCardResources(Dirigibles) }
      cardAction1(Dirigibles) { addCardResources(Dirigibles) }
      playProject(
          VenusTradeHub,
          payment = {
            doTask("4 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          },
      )
      pass()
    }

    blue.wgt("OxygenStep")

    blue.buyCards(2)
    yellow.buyCards(4)
    green.buyCards(4)

    yellow.turn {
      claimMilestone(cn("Engineer"))
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }
    }
    green.turn { cardAction1(SpaceElevator) }
    blue.turn { playProject(RedShips, 2) }
    yellow.turn {
      playProject(ImportedGhg, 2, titanium = 1)
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    green.turn { playProject(ResearchCoordination, 3) }
    blue.turn { playProject(SnowAlgae, 12) }
    yellow.turn {
      cardAction1(ElectroCatapult)
      playProject(Cartel, 6)
    }

    yellow.exMachina("6 MC")

    green.turn { playProject(OlympusConference, 9) }
    blue.turn {
      playProject(RedSpotObservatory, 17) {
        doTask("-ProjectCard")
      }
    }
    yellow.turn {
      playProject(StratosphericExpedition, titanium = 4) {
        addCardResources(FloatingHabs, 2)
      }
    }
    green.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Titan>")
        doWithoutAutoExec(green) {
          doTask("2 Floater<$TitanShuttles>")
          doTask("Floater<$TitanShuttles>")
          green.selectTask("Floater<Blue>.")
          blue.doTask("Floater<$LocalShading>")
        }
      }
    }
    blue.turn { cardAction1(NitriteReducingBacteria) }
    yellow.turn { playProject(HiredRaiders, 1) { doTask("3 M<Yellow> FROM M<Green>") } }
    green.turn { cardAction2(TitanShuttles, x = 3) }
    blue.turn { cardAction2(LocalShading) }
    yellow.turn { playProject(CuttingEdgeTechnology, 12) }
    green.turn {
      convertPlants { placeTile(2, 2) }
      playProject(SkyDocks, 5, titanium = 4, butFirst = assignAllWildTags("EarthTag"))
    }
    blue.turn { cardAction1(FloaterTechnology) { addCardResources(RedSpotObservatory) } }
    yellow.turn { playProject(IshtarExpedition, 4) }
    green.turn {
      playProject(InventorsGuild, 7) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
      cardAction1(InventorsGuild) { buyCards(1) }
    }
    blue.turn { cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) } }
    yellow.turn { playProject(AerialMappers, 11) }
    green.turn { cardAction1(PalladinShipping) }
    blue.turn { cardAction2(RedSpotObservatory) }
    yellow.turn { playProject(VenusGovernor, 2) }
    green.pass()
    blue.turn { cardAction1(Stratopolis) { addCardResources(JetStreamMicroscrappers, 2) } }
    yellow.turn { cardAction1(PowerInfrastructure, x = 2) }
    blue.turn { cardAction2(JetStreamMicroscrappers) }
    yellow.turn { playProject(FloatingRefinery, 7) }
    blue.turn { cardAction1(Dirigibles) { addCardResources(Dirigibles) } }
    yellow.turn { convertHeat() }
    blue.turn { cardAction1(Celestic) { addCardResources(Celestic) } }
    yellow.turn { cardAction1(AerialMappers) { addCardResources(AerialMappers) } }
    blue.turn {
      playProject(
          IshtarMining,
          2,
          payment = {
            pay(2)
            doTask("PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          },
      )
    }
    yellow.turn { cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") } }
    blue.turn { cardAction1(RedShips) }
    yellow.turn { cardAction1(FloatingHabs) { addCardResources(FloatingHabs) } }
    blue.pass()
    yellow.turn {
      convertHeat()
      pass()
    }

    yellow.wgt("VenusStep")

    yellow.exMachina("-6 MC")
    green.buyCards(3)
    yellow.buyCards(3)
    blue.buyCards(1)

    green.turn {
      playProject(MarketManipulation, 0) {
        doTask("ColonyProduction<Luna FROM Titan>")
      }
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }
    }
    blue.turn {
      playProject(IndenturedWorkers, 0)
      playProject(GiantIceAsteroid, 25, titanium = 1) {
        doTask("-Plant<Green>")
        placeTile(9, 6)
        placeTile(9, 7)
      }
    }
    yellow.turn {
      cardAction1(BusinessNetwork) { buyCards(1) }
      playProject(TechnologyDemonstration, 2, titanium = 1)
    }
    green.turn {
      playProject(InventionContest, 0) {
        declineTask()
      }
    }
    blue.turn { cardAction2(NitriteReducingBacteria) }
    yellow.turn { playProject(AsteroidCard, 2, titanium = 4) { doTask("-3 Plant<Blue>") } }
    green.turn {
      playProject(QuantumExtractor, 11) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    blue.turn { cardAction1(FloaterTechnology) { addCardResources(LocalShading) } }
    yellow.turn { cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") } }
    green.turn { cardAction1(PalladinShipping) }
    blue.turn { cardAction2(LocalShading) }
    yellow.turn {
      intentionalOverpay(4)
      playProject(fakeL1TradeTerminal, 13, titanium = 4)
    }
    green.turn { cardAction1(SpaceElevator) }
    blue.turn { cardAction1(Stratopolis) { addCardResources(JetStreamMicroscrappers, 2) } }
    yellow.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Io>")
        doTask("2 ColonyProduction<Io>")
      }
    }
    green.turn { cardAction1(InventorsGuild) { buyCards(0) } }
    blue.turn { cardAction2(JetStreamMicroscrappers) }
    yellow.turn { cardAction2(ElectroCatapult) }
    green.turn {
      playProject(MolecularPrinting, 9) {
        declineTask()
      }
    }
    blue.turn { cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) } }
    yellow.turn { cardAction2(AerialMappers) }
    green.turn {
      playProject(AntiGravityTechnology, 12, butFirst = assignAllWildTags("ScienceTag")) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    blue.turn { playProject(Grass, 11) }
    yellow.turn { playProject(CometForVenus, 11) { doTask("-4 MC<Blue>") } }
    green.turn { cardAction1(TitanShuttles) { addCardResources(TitanShuttles, 2) } }
    blue.turn { cardAction1(RedSpotObservatory) }
    yellow.turn {
      playProject(SisterPlanetSupport, 3)
    }
    green.turn { playProject(LunarMining, 7, butFirst = assignAllWildTags("EarthTag")) }
    blue.turn { cardAction1(Dirigibles) { addCardResources(Celestic) } }
    yellow.turn { playProject(LunaGovernor, 0) }
    green.turn {
      playProject(Atmoscoop, 13, titanium = 1) {
        doTask("2 TemperatureStep")
        addCardResources(TitanShuttles, 2)
      }
      convertHeat { placeTile(2, 6) }
    }
    blue.turn { cardAction1(Celestic) { addCardResources(Celestic) } }
    yellow.turn {
      convertHeat()
      convertHeat()
    }
    green.pass()
    blue.turn { cardAction1(RedShips) }
    yellow.turn {
      convertHeat()
      convertHeat()
    }
    blue.pass()
    yellow.turn {
      cardAction1(PowerInfrastructure, x = 2)
      playProject(EnergyMarket, 3)
      cardAction2(EnergyMarket)
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      playProject(CloudTourism, 11)
      cardAction1(CloudTourism)
    }

    yellow.pass()
    green.wgt("VenusStep")

    blue.buyCards(1)
    green.buyCards(2)
    yellow.buyCards(1)

    blue.turn {
      cardAction1(Stratopolis) { addCardResources(JetStreamMicroscrappers, 2) }
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      cardAction1(BusinessNetwork) { buyCards(0) }
      playProject(AtalantaPlanitiaLab, 8)
    }
    green.turn {
      fundAward(cn("Industrialist"), 8)
      fundAward(cn("SpaceBaron"), 14)
    }
    blue.turn { playProject(GeneRepair, 12) { declineTask() } }
    yellow.turn {
      playProject(CryoSleep, 10)
      intentionalOverpay(4)
      playProject(SolarProbe, 3, titanium = 2)
    }
    green.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }
      stdAction("TradeAction", 2) { doTask("Trade<Ganymede>") }
    }
    blue.turn { playProject(OrbitalCleanup, 11, titanium = 1) }
    yellow.turn { cardAction2(AerialMappers) }
    green.turn {
      playProject(Bushes, 6)
      convertPlants { placeTile(3, 4) }
    }
    blue.turn {
      playProject(BactoviralResearch, 10) {
        doTask("-ProjectCard")
        addCardResources(NitriteReducingBacteria)
      }
    }
    yellow.turn { cardAction2(ElectroCatapult) }
    green.turn { cardAction1(SpaceElevator) }
    blue.turn { playProject(TitanAirScrapping, 21) }
    yellow.turn { cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") } }
    green.turn { playProject(QuantumCommunications, 4) }
    blue.turn { cardAction2(NitriteReducingBacteria) }
    yellow.turn { cardAction1(PowerInfrastructure, x = 2) }
    green.turn { cardAction1(InventorsGuild) { buyCards(0) } }
    blue.turn { cardAction1(FloaterTechnology) { addCardResources(LocalShading) } }
    yellow.turn { cardAction1(CloudTourism) }
    green.turn { playProject(WaterSplittingPlant, 8) }
    blue.turn { cardAction2(LocalShading) }
    yellow.turn { playProject(Mangrove, 10) { placeTile(8, 4) } }
    green.turn { cardAction1(WaterSplittingPlant) }
    blue.turn { cardAction1(Dirigibles) { addCardResources(TitanAirScrapping) } }
    yellow.turn {
      playProject(TundraFarming, 14)
      playProject(Livestock, 11)
    }
    green.turn {
      playProject(BreathingFilters, 7) {
        declineTask()
      }
    }
    blue.turn { cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) } }
    yellow.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        doTask("2 ColonyProduction<Miranda>")
        addCardResources(Livestock)
      }
    }
    green.turn {
      playProject(TransNeptuneProbe, 0) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    blue.turn { cardAction1(OrbitalCleanup) }
    yellow.turn {
      playProject(MineralDeposit, 5)
      playProject(MiningRights, 1, steel = 4) { placeTile(8, 6) }
    }
    green.turn { cardAction1(TitanShuttles) { addCardResources(TitanShuttles, 2) } }
    blue.turn { cardAction1(Celestic) { addCardResources(TitanAirScrapping) } }
    yellow.turn {
      playProject(Ants, 7)
      cardAction1(Ants)
    }
    green.pass()
    blue.turn { cardAction2(TitanAirScrapping) }
    yellow.turn { cardAction1(Livestock) }
    blue.turn { cardAction2(RedSpotObservatory) }
    yellow.turn { cardAction1(FloatingHabs) { addCardResources(FloatingHabs) } }
    blue.turn { playProject(Algae, 10) }
    yellow.turn {
      cardAction2(EnergyMarket)
      playProject(AsteroidMiningConsortium, 11) {
        doTask("PROD[-Titanium<Blue>]")
      }
    }
    blue.turn { convertPlants { placeTile(8, 8) } }
    yellow.pass()
    blue.turn {
      cardAction1(RedShips)
      sellPatents(3)
      playProject(Lichen, 7)
    }

    blue.pass()
    blue.wgt("OxygenStep")
    yellow.buyCards(1)
    green.buyCards(1)
    blue.buyCards(0)

    yellow.turn {
      stdProject("GreenerySP") { placeTile(7, 3) }
      stdProject("GreenerySP") { placeTile(7, 4) }
    }
    green.turn {
      cardAction1(WaterSplittingPlant)
      playProject(ImmigrantCity, 9) { placeTile(9, 8) }
    }
    blue.turn {
      stdProject("CitySP") { placeTile(8, 5) }
      stdProject("CitySP") { placeTile(7, 8) }
    }
    yellow.turn {
      playProject(KaguyaTech, 10) {
        doTask("CityTile<Cimmeria_7_3> FROM GreeneryTile<Cimmeria_7_3>")
      }
      cardAction2(EnergyMarket)
    }
    green.turn { playProject(OpenCity, 19) { placeTile(5, 3) } }
    green.exMachina("PROD[-MC]")
    blue.turn { convertPlants { placeTile(7, 7) } }
    yellow.turn {
      cardAction1(BusinessNetwork) { buyCards(0) }
      playProject(TopsoilContract, 6)
    }
    green.turn { cardAction1(SpaceElevator) }
    blue.turn { cardAction2(NitriteReducingBacteria) }
    yellow.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        doTask("2 ColonyProduction<Miranda>")
        addCardResources(Livestock)
      }
    }
    green.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }
      stdAction("TradeAction", 2) { doTask("Trade<Ganymede>") }
    }
    blue.exMachina("Floater<$RedSpotObservatory>")
    blue.turn {
      cardAction2(RedSpotObservatory)
      sellPatents(1)
    }
    yellow.turn {
      cardAction1(Ants) {
        doTask("-Microbe<Player2, $NitriteReducingBacteria<Player2>>")
      }
    }
    green.turn { convertPlants { placeTile(6, 4) } }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) } }
    blue.exMachina("-2 Floater<$Stratopolis>, 2 Floater<$TitanAirScrapping>")
    yellow.turn { cardAction1(PowerInfrastructure, x = 1) }
    green.turn { cardAction1(InventorsGuild) { buyCards(0) } }
    blue.turn { cardAction2(TitanAirScrapping) }
    yellow.turn { cardAction1(Livestock) }
    green.turn { playProject(InterstellarColonyShip, titanium = 6) }
    green.exMachina("-Titanium")
    blue.turn { cardAction1(OrbitalCleanup) }
    yellow.turn { cardAction1(CloudTourism) }
    green.turn { cardAction2(TitanShuttles, x = 6) }
    blue.turn { fundAward(cn("Benefactor"), 20) }
    yellow.turn { cardAction1(FloatingRefinery) }
    green.turn {
      playProject(AiCentral, 13, steel = 2) {
        declineTask()
      }
      cardAction1(AiCentral)
    }
    blue.turn { cardAction1(Dirigibles) { addCardResources(Celestic) } }
    yellow.turn { cardAction1(FloatingHabs) { addCardResources(FloatingHabs) } }
    yellow.exMachina("2 MC")
    green.turn { stdProject("GreenerySP") { placeTile(8, 7) } }
    blue.turn { cardAction1(FloaterTechnology) { addCardResources(Celestic) } }
    yellow.turn { sellPatents(1) }
    green.turn { sellPatents(5) }
    blue.turn { cardAction1(Celestic) { addCardResources(Celestic) } }
    yellow.turn { convertPlants { placeTile(7, 6) } }
    green.pass()
    blue.turn { cardAction1(RedShips) }
    yellow.turn { cardAction2(ElectroCatapult) }
    blue.turn { cardAction1(LocalShading) }
    yellow.turn { playProject(RobotPollinators, 7) }
    blue.turn { cardAction1(Extremophiles) { addCardResources(Extremophiles) } }
    yellow.turn { playProject(Insects, 7) }
    blue.pass()
    yellow.turn {
      playProject(StanfordTorus, titanium = 4)
      sellPatents(5)
      playProject(CarbonNanosystems, 6, steel = 4)
      playProject(VestaShipyard, 2, titanium = 3) {
        doTask("PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
      }
      sellPatents(1)
    }
    yellow.pass()

    yellow.convertPlants { placeTile(6, 3) }
    yellow.declineTask()
    green.declineTask()
    blue.convertPlants { placeTile(6, 8) }
    blue.declineTask()
  }
}
