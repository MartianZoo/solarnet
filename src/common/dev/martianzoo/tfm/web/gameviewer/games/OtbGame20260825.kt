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
          "Dad",
          "Ellie",
      )

  protected override fun play() {
    TfmWorkflow.Auto(game).launch()
    val dad = game.tfm(Player.PLAYER1)
    val ellie = game.tfm(Player.PLAYER2)

    dad.playCorp(TerraLabsResearch, 10)

    ellie.playCorp(Viron, 5)

    dad.turn {
      playPrelude(FocusedOrganization) { doTask("Titanium") }

      playPrelude(HeadStart) {
        doTask("UseAction<UseCardActionSA, First>", 1)
        doTask("ActionUsedMarker<$FocusedOrganization>")
        cardAction1(FocusedOrganization) {
          doTask("-MC", 2)
          doTask("Titanium", 2)
        }

        doTask("UseAction<PlayCardSA, First>")
        doTask("PlayCard<Class<ProjectCard>, Class<$Advertising>>")
        pay(4)
      }
    }

    ellie.turn {
      playPrelude(SupplyDrop)

      playPrelude(TerraformingDeal)
    }

    dad.turn {
      playProject(MineralDeposit, 5)

      playProject(SpaceElevator, 13, steel = 4, titanium = 2)
    }

    dad.exMachina("PROD[-Titanium], Titanium")
    ellie.turn {
      playProject(ArcticAlgae, 12)

      playProject(AquiferPumping, 2, steel = 8)
    }
    dad.turn {
      cardAction1(SpaceElevator)

      playProject(RoverConstruction, 8)
    }
    ellie.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(9, 7)
      }

      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, First>")
        pay(8)
        placeTile(9, 6)
      }
    }
    dad.pass()
    ellie.turn {
      convertPlants { placeTile(8, 7) }

      playProject(MiningArea, 4) { placeTile(8, 6) }
    }
    ellie.turn {
      playProject(Sponsors, 6)
      pass()
    }

    dad.wgt("OceanTile<Cimmeria_2_1>")

    ellie.buyCards(2)
    dad.buyCards(4)

    ellie.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(9, 5)
      }
    }
    dad.turn { cardAction1(SpaceElevator) }
    ellie.turn {
      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, First>")
        pay(8)
        placeTile(8, 4)
      }

      convertPlants { placeTile(8, 5) }
    }
    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Steel")
      }
    }
    ellie.turn {
      playProject(MethaneFromTitan, 13, titanium = 5)
    }
    dad.turn {
      playProject(SponsoredAcademies, 9)
    }
    ellie.pass()
    dad.pass()

    ellie.wgt("OxygenStep")

    dad.buyCards(4)
    ellie.buyCards(3)

    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Titanium")
      }
    }
    ellie.turn {
      playProject(NeptunianPowerConsultants, 14)
    }
    dad.turn { cardAction1(SpaceElevator) }
    ellie.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(9, 9)
        doTask("UseAction<NeptunianOption, First>")
        pay(5)
      }

      convertPlants { placeTile(9, 8) }
    }
    dad.turn {
      stdProject("CitySP") { placeTile(8, 8) }
    }
    ellie.turn {
      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, First>")
        pay(8)
        placeTile(8, 9)
        doTask("UseAction<NeptunianOption, First>")
        pay(5)
      }
    }
    dad.turn {
      playProject(InvestmentLoan, 3)
    }
    ellie.pass()
    dad.turn {
      playProject(OptimalAerobraking, 1, titanium = 2)

      playProject(ViralEnhancers, 9)
    }
    dad.turn {
      sellPatents(1)

      playProject(LightningHarvest, 8)
      pass()
    }

    dad.wgt("VenusStep")

    dad.buyCards(4)
    ellie.buyCards(3)

    ellie.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(7, 9)
        doTask("UseAction<NeptunianOption, First>")
        pay(5)
      }

      cardAction1(Viron) {
        doTask("UseAction<$AquiferPumping, First>")
        pay(8)
        placeTile(1, 5)
        doTask("UseAction<NeptunianOption, First>")
        pay(5)
      }
    }
    dad.turn { cardAction1(SpaceElevator) }
    ellie.turn {
      convertPlants { placeTile(7, 6) }

      convertPlants { placeTile(7, 4) }
    }
    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Titanium")
      }
    }
    ellie.turn {
      playProject(HomeostasisBureau, 12, steel = 2)
    }
    dad.turn {
      playProject(SpinInducingAsteroid, 13, titanium = 1)
    }
    ellie.pass()
    dad.turn {
      playProject(SulphurEatingBacteria, 6) { addCardResources(SulphurEatingBacteria) }
      cardAction1(SulphurEatingBacteria)
      pass()
    }

    ellie.wgt("VenusStep")

    dad.buyCards(4)
    ellie.buyCards(1)

    dad.turn {
      playProject(MeatIndustry, 5)
      playProject(Pets, 10) { addCardResources(Pets) }
    }
    ellie.turn {
      playProject(GhgProducingBacteria, 8)
    }
    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
      cardAction1(SpaceElevator)
    }
    ellie.turn {
      cardAction1(GhgProducingBacteria)
    }
    dad.turn {
      playProject(ExtremeColdFungus, 13)
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      playProject(ImportedNutrients, 2, titanium = 4) { addCardResources(GhgProducingBacteria) }

      claimMilestone(cn("Farmer"))
    }
    dad.turn {
      cardAction2(SulphurEatingBacteria) {
        doTask("-4 Microbe<$SulphurEatingBacteria> THEN 12 MC")
      }

      claimMilestone(cn("Philantropist"))
    }
    ellie.turn {
      cardAction1(Viron) {
        doTask("UseAction<$GhgProducingBacteria, Second>")
      }
    }
    dad.pass()
    ellie.turn {
      playProject(KelpFarming, 17)
      pass()
    }

    dad.wgt("VenusStep")

    dad.exMachina("PROD[Titanium], 4 Titanium")

    ellie.buyCards(2)
    dad.buyCards(4)

    ellie.turn {
      convertHeat()
      convertHeat()
    }

    ellie.exMachina("-10 MC")
    dad.turn {
      playProject(AsteroidMiningConsortium, 13) {
        doTask("PROD[-Titanium<Ellie>]")
      }
    }
    ellie.turn {
      playProject(MiningRights, 9) { placeTile(6, 9) }
    }
    dad.turn {
      playProject(Solarnet, 7)
    }
    ellie.turn {
      playProject(OreProcessor, 9, steel = 2)
    }
    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
    }
    ellie.turn {
      playProject(PowerInfrastructure, 4)
    }
    dad.turn {
      cardAction1(SpaceElevator)
    }
    ellie.turn {
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-Energy<Dad>]") }
    }
    dad.turn {
      playProject(VenusOrbitalSurvey, 6, titanium = 4)
    }
    ellie.turn {
      cardAction1(GhgProducingBacteria)
    }
    dad.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        doTask("Ok")
        dad.pay(mc = 2)
      }
    }
    ellie.turn {
      playProject(VenusMagnetizer, 7)
    }
    dad.turn {
      playProject(LawSuit, 2) { doTask("3 MC<Dad> FROM MC<Ellie>.") }
    }
    ellie.turn {
      cardAction1(VenusMagnetizer)
    }
    dad.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      convertPlants { placeTile(5, 8) }
    }
    dad.turn {
      sellPatents(1)
      playProject(Windmills, 6)
    }
    ellie.turn {
      cardAction1(OreProcessor)

      cardAction1(Viron) { doTask("UseAction<$GhgProducingBacteria, Second>") }
    }
    dad.turn {
      cardAction1(SulphurEatingBacteria)
    }
    ellie.turn {
      claimMilestone(cn("Producer"))
    }
    dad.pass()
    ellie.pass()

    ellie.wgt("TemperatureStep")

    ellie.exMachina("10 MC")

    dad.buyCards(4)
    ellie.buyCards(4)

    dad.turn {
      playProject(Atmoscoop, 16, titanium = 2) { doTask("2 VenusStep") }
    }
    ellie.turn {
      stdProject("CitySP") { placeTile(6, 3) }
      convertPlants { placeTile(6, 2) }
    }
    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
    }
    ellie.turn { fundAward(cn("Suburbian"), 8) }
    dad.turn { cardAction1(SpaceElevator) }
    ellie.turn {
      intentionalUnderpay()
      playProject(RegoPlastics, 10)
    }
    dad.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn { playProject(FusionPower, 8, steel = 2) }
    dad.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        dad.buyCards(1)
      }
    }
    ellie.turn {
      convertHeat()
    }
    dad.turn {
      cardAction2(SulphurEatingBacteria, x = 5)
    }
    ellie.turn {
      cardAction1(OreProcessor)
    }
    dad.turn { playProject(JetStreamMicroscrappers, 12) }
    ellie.turn {
      cardAction1(VenusMagnetizer)
    }
    dad.turn {
      playProject(AirScrappingExpedition, 13) {
        addCardResources(JetStreamMicroscrappers)
      }
    }
    ellie.turn {
      playProject(cn("UnexpectedApplication"), 4)
    }
    dad.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    ellie.turn {
      cardAction1(Viron) { cardAction1(VenusMagnetizer) }
    }
    dad.pass()

    ellie.turn {
      cardAction2(GhgProducingBacteria)
      playProject(CloudTourism, 11)
    }
    ellie.turn {
      cardAction1(CloudTourism)
      playProject(Mine, 4)
    }
    ellie.turn {
      playProject(Shuttles, 1, titanium = 3)
    }
    ellie.pass()
    dad.wgt("OxygenStep")

    dad.exMachina("MC")
    ellie.exMachina("-MC")

    dad.buyCards(4)
    ellie.buyCards(2)

    ellie.turn {
      cardAction1(VenusMagnetizer)
      cardAction1(Viron) { cardAction1(VenusMagnetizer) }
    }
    dad.turn {
      playProject(ImmigrantCity, 13) { placeTile(7, 5) }
    }

    ellie.turn {
      stdProject("CitySP") { placeTile(6, 6) }

      cardAction1(OreProcessor)
    }
    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-Energy")
        doTask("Steel")
      }
      cardAction1(SpaceElevator)
    }
    ellie.turn {
      stdProject("GreenerySP") { placeTile(7, 3) }

      convertPlants { placeTile(6, 7) }
    }
    dad.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      cardAction1(GhgProducingBacteria)
    }
    dad.turn {
      playProject(EarthCatapult, 23)
    }
    ellie.turn {
      cardAction1(CloudTourism)
    }
    dad.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        doTask("Ok")
        pay(2)
      }
    }
    ellie.turn {
      playProject(EcologicalZone, 12) { placeTile(7, 8) }
    }
    dad.turn {
      playProject(SymbioticFungus, 2)
      cardAction1(SymbioticFungus) { doTask("Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      playProject(MartianLumberCorp, steel = 2)
    }
    dad.turn {
      cardAction2(SulphurEatingBacteria, x = 3)
      playProject(cn("StratosphericExpedition"), 4, titanium = 2) {
        addCardResources(JetStreamMicroscrappers)
      }
    }

    ellie.pass()
    dad.turn {
      sellPatents(6)
      playProject(Predators, 12) { addCardResources(Predators) }
      cardAction1(Predators) { doTask("-Animal<Ellie, $EcologicalZone<Ellie>>") }

      sellPatents(4)
      playProject(StratosphericBirds, 10) {
        addCardResources(StratosphericBirds)
      }

      cardAction1(StratosphericBirds)

      playProject(Heather, 4)
      pass()
    }

    ellie.wgt("TemperatureStep")

    dad.buyCards(4)
    ellie.buyCards(2)

    dad.turn {
      cardAction1(Predators) { doTask("-Animal<Ellie, $EcologicalZone<Ellie>>") }
    }
    ellie.turn {
      stdProject("CitySP") { placeTile(5, 7) }

      convertPlants { placeTile(4, 7) }
    }
    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Steel")
      }
    }
    ellie.turn {
      playProject(SixteenPsyche, 17, titanium = 4)
    }
    dad.turn {
      cardAction1(SpaceElevator)
    }
    ellie.turn {
      playProject(AsteroidDeflectionSystem, 2, steel = 3)
    }

    ellie.exMachina("2 MC")

    dad.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        doTask("Ok")
        pay(2)
      }
    }
    ellie.turn {
      fundAward(cn("Manufacturer"), 14)
    }
    dad.turn {
      playProject(BusinessNetwork, 2)
      cardAction1(BusinessNetwork) { buyCards(1) }
    }
    ellie.turn {
      cardAction1(AsteroidDeflectionSystem) { doTask("Ok") }
    }
    dad.turn {
      playProject(AqueductSystems, 7)
    }
    ellie.turn {
      playProject(VenusianInsects, 5)
    }
    dad.turn {
      convertPlants { placeTile(6, 4) }
    }
    ellie.turn {
      cardAction1(CloudTourism)
    }
    dad.turn {
      playProject(BiomassCombustors, 2) { doTask("PROD[-Plant<Ellie>]") }

      playProject(FreyjaBiodomes, 12) { addCardResources(StratosphericBirds) }
    }
    ellie.turn {
      cardAction1(VenusianInsects)
    }
    dad.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      cardAction1(GhgProducingBacteria)
    }
    dad.turn {
      cardAction1(SymbioticFungus) { doTask("Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      cardAction1(Viron) { cardAction1(VenusianInsects) }
    }
    dad.turn {
      playProject(FloatingRefinery, 5)
    }
    ellie.turn {
      convertHeat()
    }
    dad.turn {
      cardAction1(StratosphericBirds)
    }

    ellie.pass()
    dad.turn {
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }

      playProject(HeatTrappers, 4)
    }
    dad.turn {
      playProject(MagneticFieldDome, 3)

      playProject(Omnicourt, 5, steel = 2)
      pass()
    }

    dad.wgt("TemperatureStep")

    ellie.buyCards(4)
    dad.buyCards(4)

    ellie.turn {
      playProject(LargeConvoy, 19, titanium = 5) {
        doTask("5 Plant")
      }

      playProject(EquatorialMagnetizer, 2, steel = 3)
    }

    dad.turn {
      playProject(Sabotage, 0) { doTask("-7 MC<Player2>") }

      playProject(Comet, 1, titanium = 6) { doTask("Ok") }
    }
    ellie.turn {
      playProject(LavaFlows, 18) { placeTile(1, 3) }

      convertPlants { placeTile(5, 6) }
    }
    dad.turn {
      cardAction1(VenusOrbitalSurvey) {
        doTask("Ok")
        doTask("Ok")
        pay(2)
      }
    }
    ellie.turn {
      cardAction2(GhgProducingBacteria)

      convertHeat()
    }
    dad.turn {
      convertHeat()

      playProject(CometForVenus, 9) { doTask("-4 MC<Ellie>") }
    }
    ellie.turn {
      cardAction1(EquatorialMagnetizer)
    }
    dad.turn {
      cardAction1(BusinessNetwork) { buyCards(1) }
    }
    ellie.turn {
      cardAction1(AsteroidDeflectionSystem) { doTask("Ok") }
    }
    dad.turn {
      cardAction2(ExtremeColdFungus) { doTask("2 Microbe<$SulphurEatingBacteria>") }
    }
    ellie.turn {
      cardAction1(CloudTourism)
    }
    dad.turn {
      stdProject("AsteroidSP")
    }

    ellie.exMachina("TerraformRating")

    ellie.turn {
      stdProject("AsteroidSP")
    }
    dad.turn {
      cardAction1(SymbioticFungus) { doTask("Microbe<$SulphurEatingBacteria>") }
      cardAction2(SulphurEatingBacteria, x = 6)
    }
    ellie.turn {
      cardAction1(PowerInfrastructure, x = 1)
    }
    dad.turn {
      cardAction1(FocusedOrganization) {
        doTask("-MC")
        doTask("Steel")
      }

      cardAction1(SpaceElevator)
    }
    ellie.turn {
      cardAction1(VenusianInsects)
    }
    dad.turn {
      playProject(RadSuits, 4)
      fundAward(cn("Magnate"), 20)
    }
    ellie.turn {
      sellPatents(1)
    }
    dad.turn {
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
    }
    ellie.turn {
      sellPatents(1)
    }
    dad.turn {
      sellPatents(1)
    }
    ellie.turn {
      sellPatents(1)
    }
    dad.turn {
      cardAction1(Predators) { doTask("-Animal<Player2, $EcologicalZone<Player2>>") }
    }
    ellie.turn {
      sellPatents(1)
    }
    dad.turn {
      sellPatents(1)
    }
    ellie.turn {
      cardAction1(Viron) { cardAction1(VenusianInsects) }
    }
    dad.turn {
      cardAction1(StratosphericBirds)
    }
    ellie.turn {
      sellPatents(1)
    }
    dad.turn {
      sellPatents(1)
    }
    ellie.turn {
      stdProject("GreenerySP") { placeTile(1, 4) }
    }
    dad.turn {
      playProject(Fish, 7) {
        doTask("PROD[-Plant<Player2>]")
        addCardResources(Fish)
      }
    }
    ellie.turn {
      playProject(LuxuryFoods, 8)
    }
    dad.turn {
      cardAction1(Fish)
    }

    ellie.pass()
    dad.turn {
      playProject(LocalHeatTrapping, 0) { addCardResources(Fish) }

      sellPatents(4)
    }

    dad.turn {
      playProject(Bushes, 8)

      playProject(RobotPollinators, 7)

      convertPlants { placeTile(7, 7) }

      playProject(HousePrinting, 8)

      sellPatents(2)
      pass()
    }

    ellie.declineTask()

    dad.convertPlants { placeTile(5, 4) }
    dad.declineTask()
  }
}
