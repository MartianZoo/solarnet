package dev.martianzoo.tfm.web.gameviewer.games

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.web.gameviewer.RecordedGame
import dev.martianzoo.tfm.web.gameviewer.cardnames.*

public class OtbGame20260828 : RecordedGame() {
  private val colonyTiles = listOf("Ganymede", "Io", "Luna", "Miranda", "Titan")

  protected override val config: GameConfig =
      GameConfig(
          """
          CimmeriaMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, ColoniesExpansion, PromoCardPack

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

    green.turn {
      playProject(TitanShuttles, 14, titanium = 3)
    }
    blue.turn {
      stdAction("DoRequiredActions")
      playProject(LocalShading, 4)
    }
    yellow.turn {
      playProject(RimFreighters, 1, titanium = 1)
    }
    green.turn {
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    blue.turn {
      playProject(FloaterTechnology, 7)
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    yellow.turn {
      playProject(BusinessNetwork, 4)
    }

    yellow.exMachina("PROD[MC]")

    green.pass()
    blue.turn {
      playProject(NitriteReducingBacteria, 11)
      cardAction2(LocalShading)
    }
    yellow.turn {
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
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

    blue.turn {
      cardAction2(LocalShading)
    }
    yellow.turn {
      cardAction1(BusinessNetwork) { buyCards(1) }
    }
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
    blue.turn {
      playProject(JetStreamMicroscrappers, 12)
    }
    yellow.turn {
      playProject(SolarLogistics, 5, titanium = 5)
    }
    green.turn {
      cardAction2(TitanShuttles, x = 9)
    }
    blue.turn {
      playProject(Dirigibles, 11)
    }
    yellow.turn {
      playProject(OptimalAerobraking, 7)
    }
    green.turn {
      playProject(SpaceElevator, titanium = 9)
    }
    blue.turn {
      cardAction1(NitriteReducingBacteria)
    }
    yellow.turn {
      playProject(ImportOfAdvancedGhg, 7)
    }
    green.turn {
      cardAction1(SpaceElevator)
    }
    blue.turn {
      playProject(Potatoes, 2)
    }
    yellow.pass()
    green.turn {
      playProject(ResearchOutpost, 12, steel = 3) {
        placeTile(3, 3)
        doTask("Colony<Luna>")
      }
    }
    blue.turn {
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    green.turn {
      playProject(PeroxidePower, 6)
    }
    blue.turn {
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(JetStreamMicroscrappers) }
    }
    green.pass()
    blue.turn {
      cardAction2(JetStreamMicroscrappers)
    }
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
    blue.turn {
      playProject(IoSulphurResearch, 17) { doTask("3 ProjectCard") }
    }
    yellow.turn {
      playProject(CarbonateProcessing, 6)
    }
    green.turn {
      playProject(MiningArea, 3) { placeTile(4, 3) }
      claimMilestone(cn("Landshaper"))
    }
    blue.turn {
      playProject(MarsUniversity, 8) { declineTask() }
    }
    yellow.turn {
      playProject(PowerInfrastructure, 4)
    }
    green.turn {
      cardAction1(SpaceElevator)
    }
    blue.turn {
      cardAction1(NitriteReducingBacteria)
    }
    yellow.turn {
      playProject(FusionPower, 14)
    }
    green.turn {
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    blue.turn {
      cardAction2(LocalShading)
    }
    yellow.turn {
      playProject(HousePrinting, 10)
    }
    green.turn {
      cardAction1(PalladinShipping)
    }
    blue.turn {
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
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
    blue.turn {
      cardAction2(JetStreamMicroscrappers)
    }
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
    yellow.turn {
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    yellow.exMachina("-3 MC, PROD[-MC]")
    green.turn {
      cardAction1(SpaceElevator)
    }
    blue.turn {
      cardAction1(NitriteReducingBacteria)
    }
    yellow.turn {
      playProject(LavaTubeSettlement, 15) { placeTile(6, 2) }
    }
    green.turn {
      playProject(TowingAComet, 22) { placeTile(7, 9) }
    }
    blue.turn {
      cardAction2(LocalShading)
    }
    yellow.turn {
      sellPatents(1)
    }
    green.turn {
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    blue.turn {
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    yellow.turn {
      cardAction1(PowerInfrastructure, x = 1)
    }
    green.pass()
    blue.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      playProject(WaterToVenus, titanium = 3)
    }
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

    blue.turn {
      playProject(ProtectedValley, 23) { placeTile(9, 9) }
    }
    yellow.turn {
      playProject(SubterraneanReservoir, 11) { placeTile(1, 5) }
      claimMilestone(cn("Merchant"))
    }
    green.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Titan>")
        val previousAutoExecMode = green.autoExecMode
        green.autoExecMode = NONE
        try {
          doTask("3 Floater<$TitanShuttles>")
          doTask("Floater<$TitanShuttles>")
          green.selectTask("Floater<Blue>.")
          blue.doTask("Floater<$Dirigibles>")
        } finally {
          green.autoExecMode = previousAutoExecMode
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
    green.turn {
      cardAction1(SpaceElevator)
    }
    blue.turn {
      cardAction2(NitriteReducingBacteria)
    }
    yellow.turn {
      playProject(ElectroCatapult, 13, steel = 2)
      cardAction1(ElectroCatapult)
    }
    green.turn {
      cardAction2(TitanShuttles, x = 8)
    }
    blue.turn {
      cardAction2(LocalShading)
    }
    yellow.turn {
      playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$FusionPower>") }
    }
    green.turn {
      playProject(IoMiningIndustries, 10, titanium = 10)
    }
    blue.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      convertHeat()
      convertHeat()
    }
    green.turn {
      playProject(DirectedImpactors, 7)
    }
    blue.turn {
      cardAction1(FloaterTechnology) { addCardResources(Dirigibles) }
    }
    yellow.turn {
      sellPatents(1)
      playProject(FloatingHabs, 5)
    }
    green.turn {
      playProject(ReleaseOfInertGases, 13)
    }
    blue.turn {
      cardAction1(Stratopolis) { addCardResources(Dirigibles, 2) }
    }
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
    green.turn {
      cardAction1(SpaceElevator)
    }
    blue.turn {
      playProject(RedShips, 2)
    }
    yellow.turn {
      playProject(ImportedGhg, 2, titanium = 1)
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    green.turn {
      playProject(ResearchCoordination, 3)
    }
    blue.turn {
      playProject(SnowAlgae, 12)
    }
    yellow.turn {
      cardAction1(ElectroCatapult)
      playProject(Cartel, 6)
    }
    green.turn {
      playProject(OlympusConference, 9)
    }
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
        val previousAutoExecMode = green.autoExecMode
        green.autoExecMode = NONE
        try {
          doTask("2 Floater<$TitanShuttles>")
          doTask("Floater<$TitanShuttles>")
          green.selectTask("Floater<Blue>.")
          blue.doTask("Floater<$LocalShading>")
        } finally {
          green.autoExecMode = previousAutoExecMode
        }
      }
    }
    blue.turn {
      cardAction1(NitriteReducingBacteria)
    }
    yellow.turn {
      playProject(HiredRaiders, 1) { doTask("3 M<Yellow> FROM M<Green>") }
    }
    green.turn {
      cardAction2(TitanShuttles, x = 3)
    }
    blue.turn {
      cardAction2(LocalShading)
    }
    yellow.turn {
      playProject(CuttingEdgeTechnology, 12)
    }
    green.turn {
      convertPlants { placeTile(2, 2) }
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(SkyDocks, 5, titanium = 4)
    }
    blue.turn {
      cardAction1(FloaterTechnology) { addCardResources(RedSpotObservatory) }
    }
    yellow.turn {
      playProject(IshtarExpedition, 4)
    }
    green.turn {
      playProject(InventorsGuild, 7) {
        doTask("Science<Player1, OlympusConference<Player1>>")
      }
      cardAction1(InventorsGuild) { buyCards(1) }
    }
    blue.turn {
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
    }
    yellow.exMachina("6 MC")
    yellow.turn {
      playProject(AerialMappers, 11)
    }
    green.turn {
      cardAction1(PalladinShipping)
    }
    blue.turn {
      cardAction2(RedSpotObservatory)
    }
    yellow.turn {
      playProject(VenusGovernor, 2)
    }
    green.pass()
    blue.turn {
      cardAction1(Stratopolis) { addCardResources(JetStreamMicroscrappers, 2) }
    }
    yellow.turn {
      cardAction1(PowerInfrastructure, x = 2)
    }
    blue.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      playProject(FloatingRefinery, 7)
    }
    blue.turn {
      cardAction1(Dirigibles) { addCardResources(Dirigibles) }
    }
    yellow.turn {
      convertHeat()
    }
    blue.turn {
      cardAction1(Celestic) { addCardResources(Celestic) }
    }
    yellow.turn {
      cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    }
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
    yellow.turn {
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
    }
    blue.turn {
      cardAction1(RedShips)
    }
    yellow.turn {
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    blue.pass()
    yellow.turn {
      convertHeat()
      pass()
    }

    yellow.wgt("VenusStep")
  }
}
