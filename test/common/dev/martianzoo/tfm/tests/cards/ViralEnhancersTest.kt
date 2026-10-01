package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ViralEnhancersTest : CardTest() {
  @Test
  internal fun `When Viral Enhancers enters play, adds a plant`() {
    newGame()
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }.expect("Plant")
  }

  @Test
  internal fun `Can choose a resource for a bio card that cannot hold it and gain nothing`() {
    newGame()
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }
    p1.runOperation("$IndustrialMicrobes") { declineTask() }
        .expect("PROD[Energy, Steel], 0 Plant, 0 CardResource")
  }

  @Test
  internal fun `Triggers once for each bio tag on a card`() {
    newGame()
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }

    p1.runOperation("$AdvancedEcosystems") { repeat(3) { doTask("Plant") } }.expect("3 Plant")
  }

  @Test
  internal fun `Can choose a microbe when the entering card can hold it`() {
    newGame()
    p1.runOperation("$ViralEnhancers") { doTask("Plant") }
    p1.runOperation("$NitriteReducingBacteria") { addCardResources(NitriteReducingBacteria) }
        .expect("4 Microbe")
  }

  @Test
  internal fun `Cannot add a microbe to a different card`() {
    initializeExistingMicrobeCard()
    p1.runOperation("$RegolithEaters") {
      shouldThrow<NarrowingException> { doTask("Microbe<$NitriteReducingBacteria>") }
      abort()
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
  internal fun `Viral Enhancers responds twice to Pharmacy Union acquired through Merger`() {
    newGame(PreludeExpansion, PromoCardPack)
    playCorporationWithoutStartingProjects(p1, CrediCor)
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
}
