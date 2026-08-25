package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ViralEnhancersTest : CardTest() {
  @Test
  internal fun `When Viral Enhancers enters play, adds a plant`() {
    newGame()
    p1.manual("$ViralEnhancers").expect("Plant")
  }

  @Test
  internal fun `Forces a plant when a bio card cannot hold resources`() {
    newGame()
    p1.manual("$ViralEnhancers")
    p1.manual("$IndustrialMicrobes").expect("Plant")
  }

  @Test
  internal fun `Triggers once for each bio tag on a card`() {
    newGame()
    p1.manual("$ViralEnhancers")

    p1.manual("$AdvancedEcosystems").expect("3 Plant")
  }

  @Test
  internal fun `Can choose a microbe when the entering card can hold it`() {
    newGame()
    p1.manual("$ViralEnhancers")
    p1.manual("$NitriteReducingBacteria") { addCardResources(NitriteReducingBacteria) }
        .expect("4 Microbe")
  }

  @Test
  internal fun `Cannot add a microbe to a different card`() {
    initializeExistingMicrobeCard()
    p1.manual("$RegolithEaters") {
      shouldThrow<NarrowingException> { doTask("Microbe<$NitriteReducingBacteria>") }
      abort()
    }
  }

  @Test
  internal fun `Adds the chosen microbe to the entering card`() {
    initializeExistingMicrobeCard()
    p1.manual("$RegolithEaters") { addCardResources(RegolithEaters) }
        .expect("Microbe<$RegolithEaters>")
  }

  private fun initializeExistingMicrobeCard() {
    newGame()
    p1.manual("$ViralEnhancers")
    p1.manual("$NitriteReducingBacteria") { doTask("Plant") }
  }
}
