package dev.martianzoo.tfm.web.gameviewer.games

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.web.gameviewer.RecordedGame
import dev.martianzoo.tfm.web.gameviewer.cardnames.*

public class OtbGame20260825 : RecordedGame() {
  protected override val inputOnlySynonyms: List<Pair<String, String>> =
      emptyList<Pair<String, String>>()

  protected override val config: GameConfig =
      GameConfig(
          """
          CimmeriaMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, PromoCardPack, TurmoilCardPack

          Energizer, Farmer, Philantropist, Producer, RimSettler, Hoverlord
          Magnate, Manufacturer, Metropolist, SpaceBaron, Suburbian, Venuphile
          """,
          "Green",
          "Yellow",
      )

  protected override fun play() {
    TfmWorkflow.Auto(game).launch()
    val green = game.tfm(Player.PLAYER1)
    val yellow = game.tfm(Player.PLAYER2)

    green.playCorp(TerraLabsResearch, 10)

    yellow.playCorp(Viron, 5)

    green.turn {
      playPrelude(FocusedOrganization) { doTask("Titanium") }

      playPrelude(HeadStart) {
        doTask("UseAction<UseCardAction, Action1>", 1)
        doTask("ActionUsedMarker<$FocusedOrganization>")
        cardAction1(FocusedOrganization) {
          doTask("-MC", 2)
          doTask("Titanium", 2)
        }

        doTask("UseAction<PlayCardFromHand, Action1>")
        doTask("PlayCard<Class<ProjectCard>, Class<$Advertising>>")
        pay(4)
      }
    }

    yellow.turn {
      playPrelude(SupplyDrop)

      playPrelude(TerraformingDeal)
    }

    green.turn {
      playProject(MineralDeposit, 5)

      playProject(SpaceElevator, 13, steel = 4, titanium = 2)
    }

    green.exMachina("PROD[-Titanium], Titanium")
    yellow.turn {
      playProject(ArcticAlgae, 12)

      playProject(AquiferPumping, 2, steel = 8)
    }
    green.turn {
      cardAction1(SpaceElevator)

      playProject(RoverConstruction, 8)
    }
    yellow.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(9, 7)
      }

      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, Action1>")
        pay(8)
        placeTile(9, 6)
      }
    }
    green.pass()
    yellow.turn {
      convertPlants { placeTile(8, 7) }

      playProject(MiningArea, 4) { placeTile(8, 6) }
    }
    yellow.turn {
      playProject(Sponsors, 6)
      pass()
    }

    green.wgt("OceanTile<Cimmeria_2_1>")

    yellow.buyCards(2)
    green.buyCards(4)

    yellow.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(9, 5)
      }
    }
    green.turn { cardAction1(SpaceElevator) }
    yellow.turn {
      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, Action1>")
        pay(8)
        placeTile(8, 4)
      }

      convertPlants { placeTile(8, 5) }
    }
    green.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Steel")
      }
    }
    yellow.turn {
      playProject(MethaneFromTitan, 13, titanium = 5)
    }
    green.turn {
      playProject(SponsoredAcademies, 9)
    }
    yellow.pass()
    green.pass()

    yellow.wgt("OxygenStep")

    green.buyCards(4)
    yellow.buyCards(3)

    green.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Titanium")
      }
    }
    yellow.turn {
      playProject(NeptunianPowerConsultants, 14)
    }
    green.turn { cardAction1(SpaceElevator) }
    yellow.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(9, 9)
        doTask("UseAction<NeptunianOption, Action1>")
        pay(5)
      }

      convertPlants { placeTile(9, 8) }
    }
    green.turn {
      stdProject("CitySP") { placeTile(8, 8) }
    }
    yellow.turn {
      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, Action1>")
        pay(8)
        placeTile(8, 9)
        doTask("UseAction<NeptunianOption, Action1>")
        pay(5)
      }
    }
    green.turn {
      playProject(InvestmentLoan, 3)
    }
    yellow.pass()
    green.turn {
      playProject(OptimalAerobraking, 1, titanium = 2)

      playProject(ViralEnhancers, 9)
    }
    green.turn {
      sellPatents(1)

      playProject(LightningHarvest, 8)
      pass()
    }

    green.wgt("VenusStep")

    green.buyCards(4)
    yellow.buyCards(3)

    yellow.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(7, 9)
        doTask("UseAction<NeptunianOption, Action1>")
        pay(5)
      }

      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, Action1>")
        pay(8)
        placeTile(1, 5)
        doTask("UseAction<NeptunianOption, Action1>")
        pay(5)
      }
    }
    green.turn { cardAction1(SpaceElevator) }
    yellow.turn {
      convertPlants { placeTile(7, 6) }

      convertPlants { placeTile(7, 4) }
    }
    green.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Titanium")
      }
    }
    yellow.turn {
      playProject(HomeostasisBureau, 12, steel = 2)
    }
    green.turn {
      playProject(SpinInducingAsteroid, 13, titanium = 1)
    }
    yellow.pass()
    green.turn {
      playProject(SulphurEatingBacteria, 6) { addCardResources(SulphurEatingBacteria) }
      cardAction1(SulphurEatingBacteria)
      pass()
    }

    yellow.wgt("VenusStep")

    green.buyCards(4)
    yellow.buyCards(1)

    green.turn {
      playProject(MeatIndustry, 5)
      playProject(Pets, 10) { addCardResources(Pets) }
    }
    yellow.turn {
      playProject(GhgProducingBacteria, 8)
    }
    green.turn {
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
      cardAction1(SpaceElevator)
    }
    yellow.turn {
      cardAction1(GhgProducingBacteria)
    }
    green.turn {
      playProject(ExtremeColdFungus, 13)
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    yellow.turn {
      playProject(ImportedNutrients, 2, titanium = 4) { addCardResources(GhgProducingBacteria) }

      claimMilestone(cn("Farmer"))
    }
    green.turn {
      cardAction2(SulphurEatingBacteria) {
        doTask("-4 Microbe<$SulphurEatingBacteria> THEN 12 MC")
      }

      claimMilestone(cn("Philantropist"))
    }
    yellow.turn {
      cardAction1(Viron) {
        doTask("UseAction<$GhgProducingBacteria, Action2>")
      }
    }
    green.pass()
    yellow.turn {
      playProject(KelpFarming, 17)
      pass()
    }

    green.wgt("VenusStep")

    green.exMachina("PROD[Titanium], 4 Titanium")

    yellow.buyCards(2)
    green.buyCards(4)

    yellow.turn {
      convertHeat()
      convertHeat()
    }

    yellow.exMachina("-10 MC")
    green.turn {
      playProject(AsteroidMiningConsortium, 13) {
        doTask("PROD[-Titanium<Yellow>]")
      }
    }
    yellow.turn {
      playProject(MiningRights, 9) { placeTile(6, 9) }
    }
    green.turn {
      playProject(Solarnet, 7)
    }
    yellow.turn {
      playProject(OreProcessor, 9, steel = 2)
    }
    green.turn {
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
    }
    yellow.turn {
      playProject(PowerInfrastructure, 4)
    }
    green.turn {
      cardAction1(SpaceElevator)
    }
    yellow.turn {
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-Energy<Green>]") }
    }
    green.turn {
      playProject(VenusOrbitalSurvey, 6, titanium = 4)
    }
    yellow.turn {
      cardAction1(GhgProducingBacteria)
    }
    green.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        doTask("Ok")
        green.pay(mc = 2)
      }
    }
    yellow.turn {
      playProject(VenusMagnetizer, 7)
    }
    yellow.exMachina("MC")
    green.turn {
      playProject(LawSuit, 2) { doTask("3 MC<Green> FROM MC<Yellow>") }
    }
    green.exMachina("-MC")
    yellow.turn {
      cardAction1(VenusMagnetizer)
    }
    green.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    yellow.turn {
      convertPlants { placeTile(5, 8) }
    }
    green.turn {
      sellPatents(1)
      playProject(Windmills, 6)
    }
    yellow.turn {
      cardAction1(OreProcessor)

      cardAction1(Viron) { doTask("UseAction<$GhgProducingBacteria, Action2>") }
    }
    green.turn {
      cardAction1(SulphurEatingBacteria)
    }
    yellow.turn {
      claimMilestone(cn("Producer"))
    }
    green.pass()
    yellow.pass()

    yellow.wgt("TemperatureStep")

    yellow.exMachina("10 MC")

    green.buyCards(4)
    yellow.buyCards(4)

    green.turn {
      playProject(Atmoscoop, 16, titanium = 2) { doTask("2 VenusStep") }
    }
    yellow.turn {
      stdProject("CitySP") { placeTile(6, 3) }
      convertPlants { placeTile(6, 2) }
    }
    green.turn {
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
    }
    yellow.turn { fundAward(cn("Suburbian"), 8) }
    green.turn { cardAction1(SpaceElevator) }
    yellow.turn {
      intentionalUnderpay()
      playProject(RegoPlastics, 10)
    }
    green.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    yellow.turn { playProject(FusionPower, 8, steel = 2) }
    green.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        green.buyCards(1)
      }
    }
    yellow.turn {
      convertHeat()
    }
    green.turn {
      cardAction2(SulphurEatingBacteria, x = 5)
    }
    yellow.turn {
      cardAction1(OreProcessor)
    }
    green.turn { playProject(JetStreamMicroscrappers, 12) }
    yellow.turn {
      cardAction1(VenusMagnetizer)
    }
    green.turn {
      playProject(AirScrappingExpedition, 13) {
        addCardResources(JetStreamMicroscrappers)
      }
    }
    yellow.turn {
      playProject(cn("UnexpectedApplication"), 4)
    }
    green.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      cardAction1(Viron) { cardAction1(VenusMagnetizer) }
    }
    green.pass()

    yellow.turn {
      cardAction2(GhgProducingBacteria)
      playProject(CloudTourism, 11)
    }
    yellow.turn {
      cardAction1(CloudTourism)
      playProject(Mine, 4)
    }
    yellow.turn {
      playProject(Shuttles, 1, titanium = 3)
    }
    yellow.pass()
    green.wgt("OxygenStep")

    green.exMachina("MC")
    yellow.exMachina("-MC")

    green.buyCards(4)
    yellow.buyCards(2)

    yellow.turn {
      cardAction1(VenusMagnetizer)
      cardAction1(Viron) { cardAction1(VenusMagnetizer) }
    }
    green.turn {
      playProject(ImmigrantCity, 13) { placeTile(7, 5) }
    }

    yellow.turn {
      stdProject("CitySP") { placeTile(6, 6) }

      cardAction1(OreProcessor)
    }
    green.turn {
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
      cardAction1(SpaceElevator)
    }
    yellow.turn {
      stdProject("GreenerySP") { placeTile(7, 3) }

      convertPlants { placeTile(6, 7) }
    }
    green.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    yellow.turn {
      cardAction1(GhgProducingBacteria)
    }
    green.turn {
      playProject(EarthCatapult, 23)
    }
    yellow.turn {
      cardAction1(CloudTourism)
    }
    green.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        doTask("Ok")
        pay(2)
      }
    }
    yellow.turn {
      playProject(EcologicalZone, 12) { placeTile(7, 8) }
    }
    green.turn {
      playProject(SymbioticFungus, 2)
      cardAction1(SymbioticFungus) { doTask("Microbe<$SulphurEatingBacteria>") }
    }
    yellow.turn {
      playProject(MartianLumberCorp, steel = 2)
    }
    green.turn {
      cardAction2(SulphurEatingBacteria, x = 3)
      playProject(cn("StratosphericExpedition"), 4, titanium = 2) {
        addCardResources(JetStreamMicroscrappers)
      }
    }

    yellow.pass()
    green.turn {
      sellPatents(6)
      playProject(Predators, 12) { addCardResources(Predators) }
      cardAction1(Predators) { doTask("-Animal<Yellow, $EcologicalZone<Yellow>>") }

      sellPatents(4)
      playProject(StratosphericBirds, 10) {
        addCardResources(StratosphericBirds)
      }

      cardAction1(StratosphericBirds)

      playProject(Heather, 4)
      pass()
    }

    yellow.wgt("TemperatureStep")

    green.buyCards(4)
    yellow.buyCards(2)

    green.turn {
      cardAction1(Predators) { doTask("-Animal<Yellow, $EcologicalZone<Yellow>>") }
    }
    yellow.turn {
      stdProject("CitySP") { placeTile(5, 7) }

      convertPlants { placeTile(4, 7) }
    }
    green.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Steel")
      }
    }
    yellow.turn {
      playProject(SixteenPsyche, 17, titanium = 4)
    }
    green.turn {
      cardAction1(SpaceElevator)
    }
    yellow.turn {
      playProject(AsteroidDeflectionSystem, 2, steel = 3)
    }

    yellow.exMachina("2 MC")

    green.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        doTask("Ok")
        pay(2)
      }
    }
    yellow.turn {
      fundAward(cn("Manufacturer"), 14)
    }
    green.turn {
      playProject(BusinessNetwork, 2)
      cardAction1(BusinessNetwork) { buyCards(1) }
    }
    yellow.turn {
      cardAction1(AsteroidDeflectionSystem) { doTask("Ok") }
    }
    green.turn {
      playProject(AqueductSystems, 7)
    }
    yellow.turn {
      playProject(VenusianInsects, 5)
    }
    green.turn {
      convertPlants { placeTile(6, 4) }
    }
    yellow.turn {
      cardAction1(CloudTourism)
    }
    green.turn {
      playProject(BiomassCombustors, 2) { doTask("PROD[-Plant<Yellow>]") }

      playProject(FreyjaBiodomes, 12) { addCardResources(StratosphericBirds) }
    }
    yellow.turn {
      cardAction1(VenusianInsects)
    }
    green.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    yellow.turn {
      cardAction1(GhgProducingBacteria)
    }
    green.turn {
      cardAction1(SymbioticFungus) { doTask("Microbe<$SulphurEatingBacteria>") }
    }
    yellow.turn {
      cardAction1(Viron) { cardAction1(VenusianInsects) }
    }
    green.turn {
      playProject(FloatingRefinery, 5)
    }
    yellow.turn {
      convertHeat()
    }
    green.turn {
      cardAction1(StratosphericBirds)
    }

    yellow.pass()
    green.turn {
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }

      playProject(HeatTrappers, 4)
    }
    green.turn {
      playProject(MagneticFieldDome, 3)

      playProject(Omnicourt, 5, steel = 2)
      pass()
    }

    green.wgt("TemperatureStep")

    yellow.buyCards(4)
    green.buyCards(4)

    yellow.turn {
      playProject(LargeConvoy, 19, titanium = 5) {
        doTask("5 Plant")
      }

      playProject(EquatorialMagnetizer, 2, steel = 3)
    }

    green.turn {
      playProject(Sabotage, 0) { doTask("-7 MC<Player2>") }

      playProject(Comet, 1, titanium = 6) { doTask("Ok") }
    }
    yellow.turn {
      playProject(LavaFlows, 18) { placeTile(1, 3) }

      convertPlants { placeTile(5, 6) }
    }
    green.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        doTask("Ok")
        pay(2)
      }
    }
    yellow.turn {
      cardAction2(GhgProducingBacteria)

      convertHeat()
    }
    green.turn {
      convertHeat()

      playProject(CometForVenus, 9) { doTask("-4 MC<Yellow>") }
    }
    yellow.turn {
      cardAction1(EquatorialMagnetizer)
    }
    green.turn {
      cardAction1(BusinessNetwork) { buyCards(1) }
    }
    yellow.turn {
      cardAction1(AsteroidDeflectionSystem) { doTask("Ok") }
    }
    green.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    yellow.turn {
      cardAction1(CloudTourism)
    }
    green.turn {
      stdProject("AsteroidSP")
    }

    yellow.exMachina("TerraformRating")

    yellow.turn {
      stdProject("AsteroidSP")
    }
    green.turn {
      cardAction1(SymbioticFungus) { doTask("Microbe<$SulphurEatingBacteria>") }
      cardAction2(SulphurEatingBacteria, x = 6)
    }
    yellow.turn {
      cardAction1(PowerInfrastructure, x = 1)
    }
    green.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Steel")
      }

      cardAction1(SpaceElevator)
    }
    yellow.turn {
      cardAction1(VenusianInsects)
    }
    green.turn {
      playProject(RadSuits, 4)
      fundAward(cn("Magnate"), 20)
    }
    yellow.turn {
      sellPatents(1)
    }
    green.turn {
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
    }
    yellow.turn {
      sellPatents(1)
    }
    green.turn {
      sellPatents(1)
    }
    yellow.turn {
      sellPatents(1)
    }
    green.turn {
      cardAction1(Predators) { doTask("-Animal<Player2, $EcologicalZone<Player2>>") }
    }
    yellow.turn {
      sellPatents(1)
    }
    green.turn {
      sellPatents(1)
    }
    yellow.turn {
      cardAction1(Viron) { cardAction1(VenusianInsects) }
    }
    green.turn {
      cardAction1(StratosphericBirds)
    }
    yellow.turn {
      sellPatents(1)
    }
    green.turn {
      sellPatents(1)
    }
    yellow.turn {
      stdProject("GreenerySP") { placeTile(1, 4) }
    }
    green.turn {
      playProject(Fish, 7) {
        doTask("PROD[-Plant<Player2>]")
        addCardResources(Fish)
      }
    }
    yellow.turn {
      playProject(LuxuryFoods, 8)
    }
    green.turn {
      cardAction1(Fish)
    }

    yellow.pass()
    green.turn {
      playProject(LocalHeatTrapping, 0) { addCardResources(Fish) }

      sellPatents(4)
    }

    green.turn {
      playProject(Bushes, 8)

      playProject(RobotPollinators, 7)

      convertPlants { placeTile(7, 7) }

      playProject(HousePrinting, 8)

      sellPatents(2)
      pass()
    }

    yellow.declineTask()

    green.convertPlants { placeTile(5, 4) }
    green.declineTask()
  }
}
