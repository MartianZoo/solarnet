package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ViralEnhancersTest : TfmSandboxTest() {
  // Resolved FAQ: the uncollectible animal option permits taking nothing on a microbe card.
  @Test
  internal fun `Can choose the animal option to take nothing on a microbe collector`() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")

    // The uncollectible animal branch resolves to Ok, selected through declineTask().
    kim.playProject(RegolithEaters, 13) { declineTask() }
        .expect("0 Microbe<$RegolithEaters>, 0 Plant, 0 Animal")
  }

  @Test
  internal fun `When Viral Enhancers enters play, adds a plant`() {
    newTestGame()
    kim.playProject(ViralEnhancers, 9) { doTask("Plant") }.expect("Plant")
  }

  @Test
  internal fun `Triggers once for each bio tag on a card`() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")

    kim.exMachina("$Fish, $AdaptedLichen")

    kim.playProject(AdvancedEcosystems, 11) { repeat(3) { doTask("Plant") } }.expect("3 Plant")
  }

  @Test
  internal fun `Each Ecological Zone tag may choose a different Viral Enhancers bonus`() {
    newTestGame()
    kim.exMachina("GreeneryTile<Tharsis_4_4>")
    kim.exMachina("$ViralEnhancers")

    kim.playProject(EcologicalZone, 12) {
          placeTile(4, 5)
          doTask("Plant")
          doTask("Animal")
        }
        // One plant from Viral Enhancers and two from the Tharsis 4,5 placement bonus.
        .expect("3 Plant, 3 Animal<$EcologicalZone>")
  }

  @Test
  internal fun `Viral Enhancers plant can pay for Moss played with no plants`() {
    newTestGame()
    kim.exMachina("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")
    kim.exMachina("$ViralEnhancers")

    kim.playProject(Moss, 4) { doTask("Plant") }.expect("PROD[Plant], 0 Plant")
  }

  @Test
  internal fun `Cannot add a microbe to a different card`() {
    initializeExistingMicrobeCard()
    shouldThrow<NarrowingException> {
      kim.playProject(RegolithEaters, 13) {
        doTask("Microbe<$NitriteReducingBacteria>")
      }
    }
  }

  @Test
  internal fun `Adds the chosen microbe to the entering card`() {
    initializeExistingMicrobeCard()
    kim.playProject(RegolithEaters, 13) { addCardResources(RegolithEaters) }
        .expect("Microbe<$RegolithEaters>")
  }

  private fun initializeExistingMicrobeCard() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")
    kim.exMachina("$NitriteReducingBacteria")
  }

  @Test
  internal fun `Viral Enhancers responds to both Pharmacy Union tags`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.exMachina("$ViralEnhancers")

    kim.playPrelude(Merger) {
          kim.playCorp(PharmacyUnion) {
            doTask("Plant")
            doTask("Plant")
          }
        }
        .expect("2 Plant, 2 Disease<$PharmacyUnion>")
  }

  @Test
  internal fun `Viral Enhancers cannot add a disease to Pharmacy Union`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.exMachina("$ViralEnhancers")

    shouldThrow<TaskException> {
      kim.playPrelude(Merger) {
        kim.playCorp(PharmacyUnion) { doTask("Disease<$PharmacyUnion>") }
      }
    }
  }

  @Test
  internal fun `Viral Enhancers may choose a plant for a card that cannot hold animals or microbes`() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")
    kim.playProject(IndustrialMicrobes, 12) { doTask("Plant") }.expect("PROD[Energy, Steel], Plant")
  }

  // Resolved FAQ: choosing an uncollectible animal or microbe may yield nothing.
  @Test
  internal fun `Viral Enhancers may choose nothing even when a plant is available`() {
    newTestGame()
    kim.exMachina("$ViralEnhancers")
    // Choose the uncollectible bonus instead of the available plant.
    kim.playProject(IndustrialMicrobes, 12) { declineTask() }
        .expect("PROD[Energy, Steel], 0 Plant, 0 CardResource")
  }
}
