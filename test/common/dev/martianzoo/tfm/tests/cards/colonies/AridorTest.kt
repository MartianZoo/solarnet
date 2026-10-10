package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class AridorTest : TfmSandboxTest() {
  @Test
  internal fun `First action adds a selected colony tile`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", kimCorporation = Aridor)

    kim.stdAction("RequiredActionsSignal") {
          selectTask("ColonyTileSelection")
          narrowTask("Europa")
        }
        .expect("Europa, ColonyProduction, -RequiredAction")
  }

  @Test
  internal fun `A delayed colony activates immediately if its resource card is already in play`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", kimCorporation = Aridor)
    kim.exMachina("$TitanShuttles, Floater<$TitanShuttles>")

    kim.stdAction("RequiredActionsSignal") {
          selectTask("ColonyTileSelection")
          narrowTask("DelayedTitan")
        }
        .expect("Titan, ColonyProduction, 0 DelayedTitan")
  }

  @Test
  internal fun `New tag classes count for its owner even if another player already has them`() {
    newTestGame(kimCorporation = Aridor)
    kim.stdAction("RequiredActionsSignal") {
      selectTask("ColonyTileSelection")
      narrowTask("Europa")
    }
    stan.exMachina("$Mine")
    kim.setToExMachina(11, "MC")

    kim.playProject(DevelopmentCenter, 11).expect("PROD[2 MC]")
  }

  @Test
  internal fun `Acquiring Aridor does not reward existing tag classes`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.exMachina("$PharmacyUnion, $EarthCatapult")

    kim.playPrelude(Merger) { kim.playCorp(Aridor) }.expect("PROD[0 MC]")
  }

  @Test
  internal fun `A microbe tag lost when Pharmacy Union flips can be rewarded again`() {
    newTestGame(kimCorporation = Aridor)
    kim.stdAction("RequiredActionsSignal") {
      selectTask("ColonyTileSelection")
      narrowTask("Europa")
    }
    kim.exMachina("$PharmacyUnion, 3 OxygenStep")
    kim.setToExMachina(16, "MC")
    kim.playProject(CryoSleep, 10) { doTask("PlayedEvent FROM $PharmacyUnion") }

    kim.playProject(Decomposers, 5).expect("PROD[MC]")
  }

  @Test
  internal fun `A surviving microbe tag prevents a fresh reward after Pharmacy Union flips`() {
    newTestGame(kimCorporation = Aridor)
    kim.stdAction("RequiredActionsSignal") {
      selectTask("ColonyTileSelection")
      narrowTask("Europa")
    }
    kim.exMachina("$PharmacyUnion, $Decomposers, $Mine")
    kim.setToExMachina(23, "MC")
    kim.playProject(CryoSleep, 10) { doTask("PlayedEvent FROM $PharmacyUnion") }

    kim.playProject(IndustrialMicrobes, 12).expect("PROD[0 MC]")
  }

  @Test
  internal fun `An event's printed tags do not reward production`() {
    newTestGame(kimCorporation = Aridor)
    kim.stdAction("RequiredActionsSignal") {
      selectTask("ColonyTileSelection")
      narrowTask("Europa")
    }

    kim.playProject(BribedCommittee, 7).expect("PROD[0 MC]")
  }

  @Test
  internal fun `Two copies of a new tag on Pharmacy Union reward production only once`() {
    newTestGame(addOptions = "PreludeExpansion", kimCorporation = Aridor)

    kim.playPrelude(Merger) { kim.playCorp(PharmacyUnion) }.expect("PROD[MC]")
  }

  @Test
  internal fun `A Venus tag can introduce a new tag class`() {
    newTestGame(kimCorporation = Aridor)
    kim.stdAction("RequiredActionsSignal") {
      selectTask("ColonyTileSelection")
      narrowTask("Europa")
    }

    kim.setToExMachina(11, "MC")
    kim.playProject(AerialMappers, 11).expect("PROD[MC]")
  }
}
