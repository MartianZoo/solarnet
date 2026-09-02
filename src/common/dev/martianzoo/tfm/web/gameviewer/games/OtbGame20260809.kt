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
          "Ellie",
          "Dad",
      )

  protected override fun play() {
    TfmWorkflow.Auto(game).launch()
    val ellie = game.tfm(Player.PLAYER1)
    val dad = game.tfm(Player.PLAYER2)

    ellie.playCorp(MonsInsurance, 6)

    dad.playCorp(MorningStarInc, 4)

    ellie.turn {
      playPrelude(DomeFarming)

      playPrelude(SocietySupport)
    }

    dad.turn {
      playPrelude(NitrogenShipment)

      playPrelude(GreatAquifer) {
        doTask("OceanTile<Hellas_5_6>")

        doTask("OceanTile<Hellas_4_6>")
      }
    }

    ellie.turn {
      playProject(AquiferPumping, 18)

      cardAction1(AquiferPumping) {
        pay(8)

        placeTile(5, 7)
      }
    }

    dad.turn { stdAction("HandleMandates") }

    ellie.turn {
      sellPatents(1)
      playProject(RoboticWorkforce, 9) {
        doTask("CopyProductionBox<$DomeFarming>")
      }
    }

    dad.turn {
      playProject(Moss, 4)
    }

    ellie.pass()

    dad.pass()

    ellie.wgt("OxygenStep")

    dad.buyCards(2)

    ellie.buyCards(2)

    dad.turn { playProject(ReleaseOfInertGases, 14) }

    ellie.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(4, 7)
      }
    }

    dad.turn {
      playProject(TerraformingContract, 8)
    }

    ellie.pass()

    dad.pass()

    dad.wgt("VenusStep")

    dad.buyCards(3)

    ellie.buyCards(1)

    ellie.turn {
      cardAction1(AquiferPumping) {
        pay(8)

        placeTile(1, 1)
      }

      convertPlants {
        placeTile(3, 7)
      }
    }

    dad.turn {
      playProject(GiantSolarShade, 27)
    }

    ellie.turn {
      playProject(OptimalAerobraking, 7)
      playProject(Comet, 21) {
        placeTile(5, 8)

        doTask("-3 Plant<Dad>")
      }
    }

    dad.turn { playProject(VenusianInsects, 5) }

    ellie.turn { playProject(Algae, 10) }

    dad.turn { playProject(TopsoilContract, 8) }

    ellie.pass()

    dad.turn {
      cardAction1(VenusianInsects)

      playProject(VenusGovernor, 4)

      playProject(SearchForLife, 3)

      cardAction1(SearchForLife) {
        declineTask()
      }

      pass()
    }

    ellie.wgt("OxygenStep")

    dad.buyCards(3)

    ellie.buyCards(3)

    dad.turn {
      cardAction1(VenusianInsects)
    }

    ellie.turn {
      playProject(ImportedHydrogen, 16) {
        doTask("3 Plant")

        placeTile(6, 8)
      }

      cardAction1(AquiferPumping) {
        pay(6, steel = 1)
        placeTile(6, 7)
      }
    }

    dad.turn {
      playProject(PeroxidePower, 7)
    }

    ellie.turn {
      playProject(SponsoredAcademies, 9)
    }

    dad.turn { stdProject("PowerPlantSP") }

    ellie.turn {
      convertPlants {
        placeTile(3, 6)
      }

      playProject(CorporateStronghold, 11) {
        placeTile(2, 6)
      }
    }

    dad.turn {
      playProject(MartianSurvey, 9)
    }

    ellie.pass()

    dad.turn {
      cardAction1(SearchForLife) {
        declineTask()
      }

      exMachina("1 MC")

      playProject(IndustrialCenter, 4) {
        placeTile(2, 5)
      }

      pass()
    }

    dad.wgt("VenusStep")

    ellie.buyCards(3)

    dad.buyCards(3)

    ellie.turn {
      cardAction1(AquiferPumping) {
        pay(8)

        placeTile(2, 1)
      }

      playProject(EnergyMarket, 3)
    }

    ellie.exMachina("-3 MC")

    dad.turn { stdAction("TradeAction", 2) { doTask("Trade<Luna>") } }

    ellie.turn {
      cardAction1(EnergyMarket, x = 3)

      stdAction("TradeAction", 2) { doTask("Trade<Callisto>") }
    }

    ellie.exMachina("6 MC")

    dad.turn { playProject(EarthCatapult, 23) }

    ellie.turn { playProject(IshtarMining, 5) }

    dad.turn {
      playProject(LunarMining, 9)
    }

    ellie.turn {
      convertHeat()
      convertHeat()
    }

    dad.turn { cardAction1(VenusianInsects) }

    ellie.turn { playProject(Ironworks, 11) }

    dad.turn {
      cardAction1(SearchForLife) {
        declineTask()
      }
    }

    ellie.turn { cardAction1(Ironworks) }

    dad.pass()

    ellie.turn {
      convertPlants {
        placeTile(1, 5)
      }

      playProject(BiomassCombustors, 4) {
        doTask("PROD[-Plant<Dad>]")
      }

      pass()
    }

    ellie.wgt("VenusStep")

    ellie.buyCards(2)

    dad.buyCards(0)

    dad.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Triton>") }

      playProject(CupolaCity, 14) {
        placeTile(3, 3)
      }
    }

    ellie.turn {
      playProject(EcologicalZone, 12) {
        placeTile(4, 8)
      }

      stdAction("ClaimMilestone") { doTask("Landshaper") }
    }

    dad.turn {
      convertPlants {
        placeTile(2, 2)
      }
    }

    ellie.turn {
      convertPlants {
        placeTile(5, 9)
      }

      convertHeat()
    }

    dad.turn { playProject(RotatorImpacts, 1, titanium = 1) }

    ellie.turn {
      playProject(KelpFarming, 17)
    }

    dad.turn {
      playProject(NuclearPower, 6, steel = 1)
    }

    ellie.turn {
      cardAction1(EnergyMarket, x = 2)

      cardAction1(Ironworks)
    }

    dad.turn { stdAction("FundAward") { doTask("Venuphile") } }

    ellie.pass()

    dad.turn {
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

    dad.wgt("TemperatureStep")

    ellie.buyCards(3)

    dad.buyCards(4)

    ellie.turn {
      stdProject("CitySP") {
        placeTile(1, 2)
      }

      convertPlants {
        placeTile(1, 3)
      }
    }

    dad.turn {
      playProject(BigAsteroid, 7, titanium = 6) {
        doTask("-4 Plant<Ellie>")
      }
    }

    ellie.turn {
      playProject(NitrophilicMoss, 8)
    }

    dad.turn {
      playProject(TitanFloatingLaunchPad, 16) {
        addCardResources(TitanFloatingLaunchPad)
      }
    }

    ellie.turn { cardAction1(EnergyMarket, x = 2) }

    dad.turn { sellPatents(1) }

    ellie.turn { cardAction1(Ironworks) }

    dad.turn { playProject(TransNeptuneProbe, 1, titanium = 1) }

    ellie.turn {
      playProject(MercurianAlloys, 3)

      playProject(SolarWindPower, 3, titanium = 2)
    }

    dad.turn { cardAction2(RotatorImpacts) }

    ellie.pass()

    dad.turn {
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

    ellie.wgt("TemperatureStep")

    dad.buyCards(2)
    ellie.buyCards(2)

    dad.turn {
      playProject(Penguins, 5)

      stdAction("TradeAction", 3) {
        doTask("Trade<Miranda>")
        addCardResources(Penguins)
      }
    }

    ellie.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Callisto>") }

      cardAction1(Ironworks)
    }

    dad.turn {
      playProject(StratosphericBirds, 10)
    }

    ellie.turn {
      stdProject("CitySP") {
        placeTile(3, 5)
      }

      convertPlants {
        placeTile(2, 4)
      }
    }

    dad.turn {
      playProject(Birds, 8) {
        doTask("PROD[-2 Plant<Ellie>]")
      }
    }

    ellie.turn {
      convertPlants {
        placeTile(4, 5)
      }

      stdAction("ClaimMilestone") { doTask("Mayor") }
    }

    dad.turn { playProject(Extremophiles, 1) }

    ellie.turn { stdAction("ClaimMilestone") { doTask("Producer") } }

    dad.turn {
      cardAction1(VenusianInsects)
    }

    ellie.turn { stdAction("FundAward", which = 2) { doTask("Botanist") } }

    dad.turn { playProject(Satellites, 2, titanium = 2) }

    ellie.pass()

    dad.turn {
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

    dad.wgt("TemperatureStep")

    ellie.buyCards(3)
    dad.buyCards(0)

    ellie.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }

      stdProject("CitySP") { placeTile(1, 4) }
    }

    dad.turn {
      playProject(EosChasmaNationalPark, 14) {
        addCardResources(Penguins)
      }

      convertPlants {
        placeTile(3, 2)
      }
    }

    ellie.turn { playProject(RegoPlastics, 10) }

    dad.turn { cardAction1(VenusianInsects) }

    ellie.turn {
      playProject(IndustrialMicrobes, 0, steel = 4)
    }

    dad.turn { stdAction("FundAward", which = 3) { doTask("Magnate") } }

    ellie.turn {
      playProject(MethaneFromTitan, 12, titanium = 4)
    }

    dad.turn { playProject(MaxwellBase, 16) }

    ellie.turn { convertHeat() }

    dad.turn { cardAction1(MaxwellBase) { addCardResources(StratosphericBirds) } }

    ellie.turn {
      playProject(NitriteReducingBacteria, 11)
    }

    dad.turn { cardAction2(RotatorImpacts) }

    ellie.turn { cardAction2(NitriteReducingBacteria) }

    dad.turn {
      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }
    }

    ellie.pass()

    dad.turn {
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

    ellie.wgt("TemperatureStep")

    ellie.buyCards(2)
    dad.buyCards(3)

    dad.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(VenusianInsects)
      }
    }

    ellie.turn {
      stdProject("CitySP") {
        placeTile(5, 5)
      }

      convertPlants {
        placeTile(6, 6)
      }
    }

    dad.turn {
      playProject(RestrictedArea, 9) {
        placeTile(7, 6)
      }
    }

    ellie.turn {
      playProject(SubZeroSaltFish, 5) { doTask("PROD[-Plant<Dad>]") }
    }

    dad.exMachina("3 M<Ellie> FROM M<Dad>")

    dad.turn { cardAction1(RestrictedArea) }

    dad.exMachina("2 MC")

    ellie.turn {
      playProject(MedicalLab, 1, steel = 4)
    }

    dad.turn {
      playProject(AtalantaPlanitiaLab, 8)
    }

    ellie.turn {
      playProject(VenusSoils, 20) { addCardResources(NitriteReducingBacteria) }
    }

    dad.turn {
      playProject(InventionContest, 0)

      playProject(LawSuit, 0) {
        doTask("3 MC<Dad> FROM MC<Ellie>.")
      }
    }

    ellie.turn { cardAction1(SubZeroSaltFish) }

    dad.turn {
      stdProject("GreenerySP") { placeTile(4, 3) }
    }

    ellie.turn { cardAction1(NitriteReducingBacteria) }

    dad.turn {
      playProject(Harvest, 2)
    }

    ellie.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(SubZeroSaltFish)
      }
    }

    dad.turn { playProject(FloatingHabs, 3) }

    ellie.turn { cardAction1(EnergyMarket, x = 3) }

    dad.turn {
      cardAction1(TitanFloatingLaunchPad) { addCardResources(TitanFloatingLaunchPad) }
    }

    ellie.pass()

    dad.turn {
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

    dad.wgt("TemperatureStep")

    dad.exMachina("Plant")

    ellie.buyCards(2)
    dad.buyCards(4)

    ellie.turn {
      convertHeat()
      convertHeat()
    }

    dad.turn {
      cardAction1(RestrictedArea)

      stdAction("TradeAction", 2) { doTask("Trade<Triton>") }
    }

    dad.exMachina("2 MC")

    ellie.turn {
      convertPlants {
        placeTile(5, 4)
      }

      playProject(Gyropolis, 11, steel = 3) { placeTile(5, 3) }
    }

    ellie.exMachina("PROD[-3 MC, 2 E]")

    dad.turn {
      cardAction1(VenusianInsects)
    }

    ellie.turn { cardAction2(NitriteReducingBacteria) }

    dad.turn { cardAction2(RotatorImpacts) }

    ellie.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(NitriteReducingBacteria)
      }
    }

    dad.turn {
      playProject(JovianLanterns, 18) { addCardResources(JovianLanterns) }
    }

    dad.exMachina("1 MC")

    ellie.turn { sellPatents(1) }

    dad.turn {
      playProject(TerraformingGanymede, 10, titanium = 7)

      convertHeat()
    }

    ellie.turn { playProject(MolecularPrinting, 11) }

    ellie.exMachina("-1 MC")

    dad.turn { cardAction1(JovianLanterns) }

    ellie.turn {
      stdProject("AsteroidSP")
      stdProject("AsteroidSP")
    }

    dad.turn {
      playProject(FloaterLeasing, 1)
    }

    ellie.turn {
      sellPatents(2)

      playProject(EcologyResearch, 21) {
        addCardResources(SubZeroSaltFish)
        addCardResources(NitriteReducingBacteria)
      }
    }

    dad.turn { cardAction1(Penguins) }

    ellie.turn {
      playProject(ProjectInspection, 0) { doTask("UseAction<$NitriteReducingBacteria, Action2>") }
    }

    dad.turn { cardAction1(StratosphericBirds) }

    ellie.turn {
      convertPlants {
        placeTile(4, 2)
      }
    }

    dad.turn {
      cardAction1(Birds)
      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }
    }

    ellie.turn {
      cardAction2(EnergyMarket)
    }

    dad.turn {
      playProject(ViralEnhancers, 7)

      playProject(AdvancedEcosystems, 9)
    }

    ellie.pass()

    dad.turn {
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

    ellie.convertPlants {
      placeTile(6, 4)
    }
    ellie.convertPlants { placeTile(5, 2) }

    ellie.declineTask()

    dad.declineTask()
  }
}
