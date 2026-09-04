package dev.martianzoo.tfm.web.gameviewer.games

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.web.gameviewer.RecordedGame
import dev.martianzoo.tfm.web.gameviewer.cardnames.*

public class OtbGame20260809 : RecordedGame() {

  protected override val config: GameConfig =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion
          PromoCardPack

          Coastguard, Landshaper, Mayor, Producer, Sponsor, Hoverlord
          Botanist, Founder, Landlord, Magnate, Metropolist, Venuphile
          Callisto, Luna, Triton, Miranda, Enceladus
          """,
          "Yellow",
          "Green",
      )

  protected override fun play() {
    TfmWorkflow.Auto(game).launch()
    val yellow = game.tfm(Player.PLAYER1)
    val green = game.tfm(Player.PLAYER2)

    yellow.playCorp(MonsInsurance, 6)

    green.playCorp(MorningStarInc, 4)

    yellow.turn {
      playPrelude(DomeFarming)

      playPrelude(SocietySupport)
    }

    green.turn {
      playPrelude(NitrogenShipment)

      playPrelude(GreatAquifer) {
        doTask("OceanTile<Hellas_5_6>")

        doTask("OceanTile<Hellas_4_6>")
      }
    }

    yellow.turn {
      playProject(AquiferPumping, 18)

      cardAction1(AquiferPumping) {
        pay(8)

        placeTile(5, 7)
      }
    }

    green.turn { stdAction("DoRequiredActions") }

    yellow.turn {
      sellPatents(1)
      playProject(RoboticWorkforce, 9) {
        doTask("CopyProductionBox<$DomeFarming>")
      }
    }

    green.turn {
      playProject(Moss, 4)
    }

    yellow.pass()

    green.pass()

    yellow.wgt("OxygenStep")

    green.buyCards(2)

    yellow.buyCards(2)

    green.turn { playProject(ReleaseOfInertGases, 14) }

    yellow.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(4, 7)
      }
    }

    green.turn {
      playProject(TerraformingContract, 8)
    }

    yellow.pass()

    green.pass()

    green.wgt("VenusStep")

    green.buyCards(3)

    yellow.buyCards(1)

    yellow.turn {
      cardAction1(AquiferPumping) {
        pay(8)

        placeTile(1, 1)
      }

      convertPlants {
        placeTile(3, 7)
      }
    }

    green.turn {
      playProject(GiantSolarShade, 27)
    }

    yellow.turn {
      playProject(OptimalAerobraking, 7)
      playProject(Comet, 21) {
        placeTile(5, 8)

        doTask("-3 Plant<Green>")
      }
    }

    green.turn { playProject(VenusianInsects, 5) }

    yellow.turn { playProject(Algae, 10) }

    green.turn { playProject(TopsoilContract, 8) }

    yellow.pass()

    green.turn {
      cardAction1(VenusianInsects)

      playProject(VenusGovernor, 4)

      playProject(SearchForLife, 3)

      cardAction1(SearchForLife) {
        declineTask()
      }

      pass()
    }

    yellow.wgt("OxygenStep")

    green.buyCards(3)

    yellow.buyCards(3)

    green.turn {
      cardAction1(VenusianInsects)
    }

    yellow.turn {
      playProject(ImportedHydrogen, 16) {
        doTask("3 Plant")

        placeTile(6, 8)
      }

      cardAction1(AquiferPumping) {
        pay(6, steel = 1)
        placeTile(6, 7)
      }
    }

    green.turn {
      playProject(PeroxidePower, 7)
    }

    yellow.turn {
      playProject(SponsoredAcademies, 9)
    }

    green.turn { stdProject("PowerPlantSP") }

    yellow.turn {
      convertPlants {
        placeTile(3, 6)
      }

      playProject(CorporateStronghold, 11) {
        placeTile(2, 6)
      }
    }

    green.turn {
      playProject(MartianSurvey, 9)
    }

    yellow.pass()

    green.turn {
      cardAction1(SearchForLife) {
        declineTask()
      }

      exMachina("1 MC")

      playProject(IndustrialCenter, 4) {
        placeTile(2, 5)
      }

      pass()
    }

    green.wgt("VenusStep")

    yellow.buyCards(3)

    green.buyCards(3)

    yellow.turn {
      cardAction1(AquiferPumping) {
        pay(8)

        placeTile(2, 1)
      }

      playProject(EnergyMarket, 3)
    }

    yellow.exMachina("-3 MC")

    green.turn { stdAction("TradeAction", 2) { doTask("Trade<Luna>") } }

    yellow.turn {
      cardAction1(EnergyMarket, x = 3)

      stdAction("TradeAction", 2) { doTask("Trade<Callisto>") }
    }

    yellow.exMachina("6 MC")

    green.turn { playProject(EarthCatapult, 23) }

    yellow.turn { playProject(IshtarMining, 5) }

    green.turn {
      playProject(LunarMining, 9)
    }

    yellow.turn {
      convertHeat()
      convertHeat()
    }

    green.turn { cardAction1(VenusianInsects) }

    yellow.turn { playProject(Ironworks, 11) }

    green.turn {
      cardAction1(SearchForLife) {
        declineTask()
      }
    }

    yellow.turn { cardAction1(Ironworks) }

    green.pass()

    yellow.turn {
      convertPlants {
        placeTile(1, 5)
      }

      playProject(BiomassCombustors, 4) {
        doTask("PROD[-Plant<Green>]")
      }

      pass()
    }

    yellow.wgt("VenusStep")

    yellow.buyCards(2)

    green.buyCards(0)

    green.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Triton>") }

      playProject(CupolaCity, 14) {
        placeTile(3, 3)
      }
    }

    yellow.turn {
      playProject(EcologicalZone, 12) {
        placeTile(4, 8)
      }

      stdAction("ClaimMilestone") { doTask("Landshaper") }
    }

    green.turn {
      convertPlants {
        placeTile(2, 2)
      }
    }

    yellow.turn {
      convertPlants {
        placeTile(5, 9)
      }

      convertHeat()
    }

    green.turn { playProject(RotatorImpacts, 1, titanium = 1) }

    yellow.turn {
      playProject(KelpFarming, 17)
    }

    green.turn {
      playProject(NuclearPower, 6, steel = 1)
    }

    yellow.turn {
      cardAction1(EnergyMarket, x = 2)

      cardAction1(Ironworks)
    }

    green.turn { stdAction("FundAward") { doTask("Venuphile") } }

    yellow.pass()

    green.turn {
      playProject(VenusianPlants, 11) {
        addCardResources(VenusianInsects)
      }

      cardAction1(VenusianInsects)

      cardAction1(RotatorImpacts) { pay(titanium = 2) }

      cardAction1(SearchForLife) {
        declineTask()
      }

      pass()
    }

    green.wgt("TemperatureStep")

    yellow.buyCards(3)

    green.buyCards(4)

    yellow.turn {
      stdProject("CitySP") {
        placeTile(1, 2)
      }

      convertPlants {
        placeTile(1, 3)
      }
    }

    green.turn {
      playProject(BigAsteroid, 7, titanium = 6) {
        doTask("-4 Plant<Yellow>")
      }
    }

    yellow.turn {
      playProject(NitrophilicMoss, 8)
    }

    green.turn {
      playProject(TitanFloatingLaunchPad, 16) {
        addCardResources(TitanFloatingLaunchPad)
      }
    }

    yellow.turn { cardAction1(EnergyMarket, x = 2) }

    green.turn { sellPatents(1) }

    yellow.turn { cardAction1(Ironworks) }

    green.turn { playProject(TransNeptuneProbe, 1, titanium = 1) }

    yellow.turn {
      playProject(MercurianAlloys, 3)

      playProject(SolarWindPower, 3, titanium = 2)
    }

    green.turn { cardAction2(RotatorImpacts) }

    yellow.pass()

    green.turn {
      cardAction2(TitanFloatingLaunchPad) {
        doTask("Trade<Enceladus>")
        addCardResources(VenusianInsects)
      }

      cardAction1(VenusianInsects)

      cardAction1(SearchForLife) {
        declineTask()
      }

      pass()
    }

    yellow.wgt("TemperatureStep")

    green.buyCards(2)
    yellow.buyCards(2)

    green.turn {
      playProject(Penguins, 5)

      stdAction("TradeAction", 3) {
        doTask("Trade<Miranda>")
        addCardResources(Penguins)
      }
    }

    yellow.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Callisto>") }

      cardAction1(Ironworks)
    }

    green.turn {
      playProject(StratosphericBirds, 10)
    }

    yellow.turn {
      stdProject("CitySP") {
        placeTile(3, 5)
      }

      convertPlants {
        placeTile(2, 4)
      }
    }

    green.turn {
      playProject(Birds, 8) {
        doTask("PROD[-2 Plant<Yellow>]")
      }
    }

    yellow.turn {
      convertPlants {
        placeTile(4, 5)
      }

      stdAction("ClaimMilestone") { doTask("Mayor") }
    }

    green.turn { playProject(Extremophiles, 1) }

    yellow.turn { stdAction("ClaimMilestone") { doTask("Producer") } }

    green.turn {
      cardAction1(VenusianInsects)
    }

    yellow.turn { stdAction("FundAward", which = 2) { doTask("Botanist") } }

    green.turn { playProject(Satellites, 2, titanium = 2) }

    yellow.pass()

    green.turn {
      cardAction1(Penguins)
      cardAction1(StratosphericBirds)
      cardAction1(Birds)

      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }

      cardAction1(RotatorImpacts) { pay(6) }

      cardAction1(TitanFloatingLaunchPad) { addCardResources(TitanFloatingLaunchPad) }

      cardAction1(SearchForLife) {
        declineTask()
      }

      pass()
    }

    green.wgt("TemperatureStep")

    yellow.buyCards(3)
    green.buyCards(0)

    yellow.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }

      stdProject("CitySP") { placeTile(1, 4) }
    }

    green.turn {
      playProject(EosChasmaNationalPark, 14) {
        addCardResources(Penguins)
      }

      convertPlants {
        placeTile(3, 2)
      }
    }

    yellow.turn { playProject(RegoPlastics, 10) }

    green.turn { cardAction1(VenusianInsects) }

    yellow.turn {
      playProject(IndustrialMicrobes, 0, steel = 4)
    }

    green.turn { stdAction("FundAward", which = 3) { doTask("Magnate") } }

    yellow.turn {
      playProject(MethaneFromTitan, 12, titanium = 4)
    }

    green.turn { playProject(MaxwellBase, 16) }

    yellow.turn { convertHeat() }

    green.turn { cardAction1(MaxwellBase) { addCardResources(StratosphericBirds) } }

    yellow.turn {
      playProject(NitriteReducingBacteria, 11)
    }

    green.turn { cardAction2(RotatorImpacts) }

    yellow.turn { cardAction2(NitriteReducingBacteria) }

    green.turn {
      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }
    }

    yellow.pass()

    green.turn {
      cardAction1(Penguins)
      cardAction1(StratosphericBirds)
      cardAction1(Birds)

      cardAction2(TitanFloatingLaunchPad) {
        doTask("Trade<Miranda>")
        addCardResources(Penguins)
      }

      cardAction1(SearchForLife) {
        declineTask()
      }

      convertHeat()

      pass()
    }

    yellow.wgt("TemperatureStep")

    yellow.buyCards(2)
    green.buyCards(3)

    green.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(VenusianInsects)
      }
    }

    yellow.turn {
      stdProject("CitySP") {
        placeTile(5, 5)
      }

      convertPlants {
        placeTile(6, 6)
      }
    }

    green.turn {
      playProject(RestrictedArea, 9) {
        placeTile(7, 6)
      }
    }

    yellow.turn {
      playProject(SubZeroSaltFish, 5) { doTask("PROD[-Plant<Green>]") }
    }

    green.exMachina("3 M<Yellow> FROM M<Green>")

    green.turn { cardAction1(RestrictedArea) }

    green.exMachina("2 MC")

    yellow.turn {
      playProject(MedicalLab, 1, steel = 4)
    }

    green.turn {
      playProject(AtalantaPlanitiaLab, 8)
    }

    yellow.turn {
      playProject(VenusSoils, 20) { addCardResources(NitriteReducingBacteria) }
    }

    green.turn {
      playProject(InventionContest, 0)

      playProject(LawSuit, 0) {
        doTask("3 MC<Green> FROM MC<Yellow>")
      }
    }

    yellow.turn { cardAction1(SubZeroSaltFish) }

    green.turn {
      stdProject("GreenerySP") { placeTile(4, 3) }
    }

    yellow.turn { cardAction1(NitriteReducingBacteria) }

    green.turn {
      playProject(Harvest, 2)
    }

    yellow.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(SubZeroSaltFish)
      }
    }

    green.turn { playProject(FloatingHabs, 3) }

    yellow.turn { cardAction1(EnergyMarket, x = 3) }

    green.turn {
      cardAction1(TitanFloatingLaunchPad) { addCardResources(TitanFloatingLaunchPad) }
    }

    yellow.pass()

    green.turn {
      cardAction1(VenusianInsects)

      cardAction1(Penguins)
      cardAction1(StratosphericBirds)
      cardAction1(Birds)

      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }

      cardAction1(MaxwellBase) { addCardResources(StratosphericBirds) }

      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }

      cardAction1(RotatorImpacts) { pay(titanium = 2) }

      cardAction1(SearchForLife) {
        declineTask()
      }

      pass()
    }

    green.wgt("TemperatureStep")

    green.exMachina("Plant")

    yellow.buyCards(2)
    green.buyCards(4)

    yellow.turn {
      convertHeat()
      convertHeat()
    }

    green.turn {
      cardAction1(RestrictedArea)

      stdAction("TradeAction", 2) { doTask("Trade<Triton>") }
    }

    green.exMachina("2 MC")

    yellow.turn {
      convertPlants {
        placeTile(5, 4)
      }

      playProject(Gyropolis, 11, steel = 3) { placeTile(5, 3) }
    }

    yellow.exMachina("PROD[-3 MC, 2 E]")

    green.turn {
      cardAction1(VenusianInsects)
    }

    yellow.turn { cardAction2(NitriteReducingBacteria) }

    green.turn { cardAction2(RotatorImpacts) }

    yellow.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(NitriteReducingBacteria)
      }
    }

    green.turn {
      playProject(JovianLanterns, 18) { addCardResources(JovianLanterns) }
    }

    green.exMachina("1 MC")

    yellow.turn { sellPatents(1) }

    green.turn {
      playProject(TerraformingGanymede, 10, titanium = 7)

      convertHeat()
    }

    yellow.turn { playProject(MolecularPrinting, 11) }

    yellow.exMachina("-1 MC")

    green.turn { cardAction1(JovianLanterns) }

    yellow.turn {
      stdProject("AsteroidSP")
      stdProject("AsteroidSP")
    }

    green.turn {
      playProject(FloaterLeasing, 1)
    }

    yellow.turn {
      sellPatents(2)

      playProject(EcologyResearch, 21) {
        addCardResources(SubZeroSaltFish)
        addCardResources(NitriteReducingBacteria)
      }
    }

    green.turn { cardAction1(Penguins) }

    yellow.turn {
      playProject(ProjectInspection, 0) { doTask("UseAction<$NitriteReducingBacteria, Action2>") }
    }

    green.turn { cardAction1(StratosphericBirds) }

    yellow.turn {
      convertPlants {
        placeTile(4, 2)
      }
    }

    green.turn {
      cardAction1(Birds)
      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }
    }

    yellow.turn {
      cardAction2(EnergyMarket)
    }

    green.turn {
      playProject(ViralEnhancers, 7)

      playProject(AdvancedEcosystems, 9)
    }

    yellow.pass()

    green.turn {
      convertPlants {
        placeTile(4, 4)
      }

      playProject(MiningQuota, steel = 2)

      sellPatents(1)

      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }

      cardAction1(MaxwellBase) { addCardResources(VenusianInsects) }

      cardAction1(SearchForLife) {
        declineTask()
      }

      exMachina("2 MC")

      pass()
    }

    yellow.convertPlants {
      placeTile(6, 4)
    }
    yellow.convertPlants { placeTile(5, 2) }

    yellow.declineTask()

    green.declineTask()
  }
}
