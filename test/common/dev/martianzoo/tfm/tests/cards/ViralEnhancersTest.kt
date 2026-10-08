package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ViralEnhancersTest : CardTest() {
  // Resolved FAQ: the uncollectible animal option permits taking nothing on a microbe card.
  @Test
  internal fun `Can choose the animal option to take nothing on a microbe collector`() {
    newGame(CorporateEraExpansion)
    p1.runOperation("22 MC, 2 ProjectCard")
    admin.phase("Action")
    p1.playProject(ViralEnhancers, 9) { doTask("Plant") }

    // The uncollectible animal branch resolves to Ok, selected through declineTask().
    p1.playProject(RegolithEaters, 13) { declineTask() }
        .expect("0 Microbe<$RegolithEaters>, 0 Plant, 0 Animal")
  }

  @Test
  internal fun `When Viral Enhancers enters play, adds a plant`() {
    newGame()
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }.expect("Plant")
  }

  @Test
  internal fun `Triggers once for each bio tag on a card`() {
    newGame()
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }

    p1.runOperation("$AdvancedEcosystems") { repeat(3) { doTask("Plant") } }.expect("3 Plant")
  }

  @Test
  internal fun `Each Ecological Zone tag may choose a different Viral Enhancers bonus`() {
    newGame(CorporateEraExpansion)
    p1.runOperation("21 MC, 2 ProjectCard, GreeneryTile<Tharsis_4_4>")
    admin.phase("Action")
    p1.playProject(ViralEnhancers, 9) { doTask("Plant") }

    p1.playProject(EcologicalZone, 12) {
          placeTile(4, 5)
          doTask("Plant")
          doTask("Animal")
        }
        // One plant from Viral Enhancers and two from the Tharsis 4,5 placement bonus.
        .expect("3 Plant, 3 Animal<$EcologicalZone>")
  }

  @Test
  internal fun `Viral Enhancers plant can pay for Moss played with no plants`() {
    newGame(CorporateEraExpansion)
    admin.runOperation("OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")
    p1.runOperation("13 MC, 2 ProjectCard")
    admin.phase("Action")
    p1.playProject(ViralEnhancers, 9) { doTask("Plant") }
    p1.runOperation("-Plant")

    p1.playProject(Moss, 4) { doTask("Plant") }.expect("PROD[Plant], 0 Plant")
  }

  @Test
  internal fun `Cannot add a microbe to a different card`() {
    initializeExistingMicrobeCard()
    shouldThrow<NarrowingException> {
      p1.runOperation("$RegolithEaters") {
        doTask("Microbe<$NitriteReducingBacteria>")
      }
    }
  }

  @Test
  internal fun `Adds the chosen microbe to the entering card`() {
    initializeExistingMicrobeCard()
    p1.runOperation("$RegolithEaters") { addCardResources(RegolithEaters) }
        .expect("Microbe<$RegolithEaters>")
  }

  private fun initializeExistingMicrobeCard() {
    newGame()
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }
    p1.runOperation("$NitriteReducingBacteria") { doTask("Plant") }
  }

  @Test
  internal fun `Viral Enhancers responds to both Pharmacy Union tags`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp(CrediCor, 0)
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }
    admin.phase("Prelude")
    p1.runOperation("PreludeCard")

    p1.playPrelude(Merger) {
          p1.playCorp(PharmacyUnion) {
            doTask("Plant")
            doTask("Plant")
          }
        }
        .expect("2 Plant, 2 Disease<$PharmacyUnion>")
    p1.count("Plant") shouldBe 3
  }

  @Test
  internal fun `Viral Enhancers cannot add a disease to Pharmacy Union`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp(CrediCor, 0)
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }
    admin.phase("Prelude")
    p1.runOperation("PreludeCard")

    shouldThrow<TaskException> {
      p1.playPrelude(Merger) {
        p1.playCorp(PharmacyUnion) { doTask("Disease<$PharmacyUnion>") }
      }
    }
  }

  @Test
  internal fun `Viral Enhancers may choose a plant for a card that cannot hold animals or microbes`() {
    newGame(CorporateEraExpansion)
    p1.runOperation("21 MC, 2 ProjectCard")
    admin.phase("Action")
    p1.playProject(ViralEnhancers, 9) { doTask("Plant") }
    p1.playProject(IndustrialMicrobes, 12) { doTask("Plant") }.expect("PROD[Energy, Steel], Plant")
  }

  // Resolved FAQ: choosing an uncollectible animal or microbe may yield nothing.
  @Test
  internal fun `Viral Enhancers may choose nothing even when a plant is available`() {
    newGame(CorporateEraExpansion)
    p1.runOperation("21 MC, 2 ProjectCard")
    admin.phase("Action")
    p1.playProject(ViralEnhancers, 9) { doTask("Plant") }
    // Choose the uncollectible bonus instead of the available plant.
    p1.playProject(IndustrialMicrobes, 12) { declineTask() }
        .expect("PROD[Energy, Steel], 0 Plant, 0 CardResource")
  }
}
