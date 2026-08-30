package dev.martianzoo.tfm.web.gameviewer.games

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
          "Dad",
          "Joanna",
          "Ellie",
      )

  protected override fun play() {
    TfmWorkflow.Auto(game).launch()
    val dad = game.tfm(Player.PLAYER1)
    val joanna = game.tfm(Player.PLAYER2)
    val ellie = game.tfm(Player.PLAYER3)

    dad.playCorp(PalladinShipping, 4)
    joanna.playCorp(Celestic, 5)
    ellie.playCorp(PointLuna, 5)

    dad.turn {
      playPrelude(Biofuels)
      playPrelude(Supplier)
    }
    joanna.turn {
      playPrelude(GreatAquifer) {
        doTask("OceanTile<Cimmeria_2_1>")
        doTask("OceanTile<Cimmeria_9_5>")
      }
      playPrelude(AtmosphericEnhancers) { doTask("2 VenusStep") }
    }
    ellie.turn {
      playPrelude(OrbitalConstructionYard)
      playPrelude(EarlyColonization) { doTask("Colony<Luna>") }
    }

    dad.turn {
      playProject(TitanShuttles, 14, titanium = 3)
    }
    joanna.turn {
      stdAction("HandleMandates")
      playProject(LocalShading, 4)
    }
    ellie.turn {
      playProject(RimFreighters, 1, titanium = 1)
    }
    dad.turn {
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    joanna.turn {
      playProject(FloaterTechnology, 7)
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    ellie.turn {
      playProject(BusinessNetwork, 4)
    }

    ellie.exMachina("PROD[MC]")

    dad.pass()
    joanna.turn {
      playProject(NitriteReducingBacteria, 11)
      cardAction2(LocalShading)
    }
    ellie.turn {
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    joanna.turn {
      cardAction2(NitriteReducingBacteria)
      cardAction1(Celestic) { addCardResources(LocalShading) }
    }
    ellie.turn {
      stdAction("TradeSA", 2) { doTask("Trade<Luna>") }
      playProject(FueledGenerators, 1)
    }
    joanna.pass()
    ellie.pass()

    dad.wgt("OxygenStep")

    joanna.buyCards(2)
    ellie.buyCards(2)
    dad.buyCards(3)

    joanna.turn {
      cardAction2(LocalShading)
    }
    ellie.turn {
      cardAction1(BusinessNetwork) { buyCards(1) }
    }
    dad.turn {
      playProject(MinorityRefuge, 2, titanium = 1) {
        doTask("Colony<Titan>")
        addCardResources(TitanShuttles)
      }
      stdAction("TradeSA", 2) {
        doTask("Trade<Titan>")
        addCardResources(TitanShuttles, 3)
        addCardResources(TitanShuttles)
      }
    }
    joanna.turn {
      playProject(JetStreamMicroscrappers, 12)
    }
    ellie.turn {
      playProject(SolarLogistics, 5, titanium = 5)
    }
    dad.turn {
      cardAction2(TitanShuttles, x = 9)
    }
    joanna.turn {
      playProject(Dirigibles, 11)
    }
    ellie.turn {
      playProject(OptimalAerobraking, 7)
    }
    dad.turn {
      playProject(SpaceElevator, titanium = 9)
    }
    joanna.turn {
      cardAction1(NitriteReducingBacteria)
    }
    ellie.turn {
      playProject(ImportOfAdvancedGhg, 7)
    }
    dad.turn {
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      playProject(Potatoes, 2)
    }
    ellie.pass()
    dad.turn {
      playProject(ResearchOutpost, 12, steel = 3) {
        placeTile(3, 3)
        doTask("Colony<Luna>")
      }
    }
    joanna.turn {
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    dad.turn {
      playProject(PeroxidePower, 6)
    }
    joanna.turn {
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(JetStreamMicroscrappers) }
    }
    dad.pass()
    joanna.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    joanna.pass()

    joanna.wgt("VenusStep")

    ellie.buyCards(3)
    dad.buyCards(2)
    joanna.buyCards(1)

    ellie.turn {
      stdAction("TradeSA", 3) { doTask("Trade<Io>") }
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    dad.turn {
      stdAction("TradeSA", 2) { doTask("Trade<Ganymede>") }
      convertPlants { placeTile(3, 2) }
    }
    joanna.turn {
      playProject(IoSulphurResearch, 17) { doTask("3 ProjectCard") }
    }
    ellie.turn {
      playProject(CarbonateProcessing, 6)
    }
    dad.turn {
      playProject(MiningArea, 3) { placeTile(4, 3) }
      claimMilestone(cn("Landshaper"))
    }
    joanna.turn {
      playProject(MarsUniversity, 8) { declineTask() }
    }
    ellie.turn {
      playProject(PowerInfrastructure, 4)
    }
    dad.turn {
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      cardAction1(NitriteReducingBacteria)
    }
    ellie.turn {
      playProject(FusionPower, 14)
    }
    dad.turn {
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    joanna.turn {
      cardAction2(LocalShading)
    }
    ellie.turn {
      playProject(HousePrinting, 10)
    }
    dad.turn {
      cardAction1(PalladinShipping)
    }
    joanna.turn {
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    ellie.turn {
      convertHeat()
      convertHeat()
    }
    dad.pass()
    joanna.turn {
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(JetStreamMicroscrappers) }
    }
    ellie.pass()
    joanna.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    joanna.pass()

    ellie.wgt("VenusStep")

    dad.buyCards(3)
    joanna.buyCards(3)
    ellie.buyCards(1)

    dad.turn {
      playProject(ResearchColony, 16, titanium = 1) { doTask("Colony<Luna>") }
      stdAction("TradeSA", 2) { doTask("Trade<Luna>") }
    }
    joanna.turn {
      playProject(IceMoonColony, 17, titanium = 2) {
        placeTile(8, 9)
        doTask("Colony<Titan>")
        addCardResources(JetStreamMicroscrappers, 3)
      }
    }
    ellie.turn {
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    ellie.exMachina("-3 MC, PROD[-MC]")
    dad.turn {
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      cardAction1(NitriteReducingBacteria)
    }
    ellie.turn {
      playProject(LavaTubeSettlement, 15) { placeTile(6, 2) }
    }
    dad.turn {
      playProject(TowingAComet, 22) { placeTile(7, 9) }
    }
    joanna.turn {
      cardAction2(LocalShading)
    }
    ellie.turn {
      sellPatents(1)
    }
    dad.turn {
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    joanna.turn {
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    ellie.turn {
      cardAction1(PowerInfrastructure, x = 1)
    }
    dad.pass()
    joanna.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    ellie.turn {
      playProject(WaterToVenus, titanium = 3)
    }
    joanna.turn {
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(Dirigibles) }
    }
    ellie.pass()
    joanna.pass()

    dad.wgt("OceanTile<Cimmeria_1_1>")

    joanna.buyCards(2)
    ellie.buyCards(3)
    dad.buyCards(3)

    joanna.turn {
      playProject(ProtectedValley, 23) { placeTile(9, 9) }
    }
    ellie.turn {
      playProject(SubterraneanReservoir, 11) { placeTile(1, 5) }
      claimMilestone(cn("Merchant"))
    }
    dad.turn {
      stdAction("TradeSA", 2) {
        doTask("Trade<Titan>")
        addCardResources(TitanShuttles, 3)
        addCardResources(TitanShuttles)
        joanna.addCardResources(Dirigibles)
      }
    }
    joanna.turn {
      playProject(Stratopolis, 16) {
        doTask("2 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
      }
    }
    ellie.turn {
      stdAction("TradeSA", 2) { doTask("Trade<Luna>") }
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    dad.turn {
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      cardAction2(NitriteReducingBacteria)
    }
    ellie.turn {
      playProject(ElectroCatapult, 13, steel = 2)
      cardAction1(ElectroCatapult)
    }
    dad.turn {
      cardAction2(TitanShuttles, x = 8)
    }
    joanna.turn {
      cardAction2(LocalShading)
    }
    ellie.turn {
      playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$FusionPower>") }
    }
    dad.turn {
      playProject(IoMiningIndustries, 10, titanium = 10)
    }
    joanna.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    ellie.turn {
      convertHeat()
      convertHeat()
    }
    dad.turn {
      playProject(DirectedImpactors, 7)
    }
    joanna.turn {
      cardAction1(FloaterTechnology) { addCardResources(Dirigibles) }
    }
    ellie.turn {
      sellPatents(1)
      playProject(FloatingHabs, 5)
    }
    dad.turn {
      playProject(ReleaseOfInertGases, 13)
    }
    joanna.turn {
      cardAction1(Stratopolis) { addCardResources(Dirigibles, 2) }
    }
    ellie.turn {
      sellPatents(2)
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    dad.pass()
    joanna.turn {
      playProject(
          Extremophiles,
          payment = {
            doTask("PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          },
      )
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
    }
    ellie.pass()
    joanna.turn {
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

    joanna.wgt("OxygenStep")

    joanna.buyCards(2)
    ellie.buyCards(4)
    dad.buyCards(4)

    ellie.turn {
      claimMilestone(cn("Engineer"))
      stdAction("TradeSA", 2) { doTask("Trade<Luna>") }
    }
    dad.turn {
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      playProject(RedShips, 2)
    }
    ellie.turn {
      playProject(ImportedGhg, 2, titanium = 1)
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    dad.turn {
      playProject(ResearchCoordination, 3)
    }
    joanna.turn {
      playProject(SnowAlgae, 12)
    }
    ellie.turn {
      cardAction1(ElectroCatapult)
      playProject(Cartel, 6)
    }
    dad.turn {
      playProject(OlympusConference, 9)
    }
    joanna.turn {
      playProject(RedSpotObservatory, 17) {
        doTask("-ProjectCard")
      }
    }
    ellie.turn {
      playProject(StratosphericExpedition, titanium = 4) {
        addCardResources(FloatingHabs, 2)
      }
    }
    dad.turn {
      stdAction("TradeSA", 2) {
        doTask("Trade<Titan>")
        addCardResources(TitanShuttles, 2)
        addCardResources(TitanShuttles)
        joanna.addCardResources(LocalShading)
      }
    }
    joanna.turn {
      cardAction1(NitriteReducingBacteria)
    }
    ellie.turn {
      playProject(HiredRaiders, 1) { doTask("3 M<Ellie> FROM M<Dad>") }
    }
    dad.turn {
      cardAction2(TitanShuttles, x = 3)
    }
    joanna.turn {
      cardAction2(LocalShading)
    }
    ellie.turn {
      playProject(CuttingEdgeTechnology, 12)
    }
    dad.turn {
      convertPlants { placeTile(2, 2) }
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(SkyDocks, 5, titanium = 4)
    }
    joanna.turn {
      cardAction1(FloaterTechnology) { addCardResources(RedSpotObservatory) }
    }
    ellie.turn {
      playProject(IshtarExpedition, 4)
    }
    dad.turn {
      playProject(InventorsGuild, 7) {
        doTask("Science<Player1, OlympusConference<Player1>>")
      }
      cardAction1(InventorsGuild) { buyCards(1) }
    }
    joanna.turn {
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
    }
    ellie.exMachina("6 MC")
    ellie.turn {
      playProject(AerialMappers, 11)
    }
    dad.turn {
      cardAction1(PalladinShipping)
    }
    joanna.turn {
      cardAction2(RedSpotObservatory)
    }
    ellie.turn {
      playProject(VenusGovernor, 2)
    }
    dad.pass()
    joanna.turn {
      cardAction1(Stratopolis) { addCardResources(JetStreamMicroscrappers, 2) }
    }
    ellie.turn {
      cardAction1(PowerInfrastructure, x = 2)
    }
    joanna.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    ellie.turn {
      playProject(FloatingRefinery, 7)
    }
    joanna.turn {
      cardAction1(Dirigibles) { addCardResources(Dirigibles) }
    }
    ellie.turn {
      convertHeat()
    }
    joanna.turn {
      cardAction1(Celestic) { addCardResources(Celestic) }
    }
    ellie.turn {
      cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    }
    joanna.turn {
      playProject(
          IshtarMining,
          2,
          payment = {
            pay(2)
            doTask("PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          },
      )
    }
    ellie.turn {
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
    }
    joanna.turn {
      cardAction1(RedShips)
    }
    ellie.turn {
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    joanna.pass()
    ellie.turn {
      convertHeat()
      pass()
    }

    ellie.wgt("VenusStep")
  }
}
