package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon.Option.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class ViralEnhancersTest : CardTest() {
  @Test
  fun `when Viral Enhancers enters play, adds a plant`() {
    newGame()
    p1.manual("ViralEnhancers").expect("Plant")
  }

  @Test
  fun `with Viral Enhancers, adds a bio-tag card choosing a plant`() {
    newGame()
    p1.manual("ViralEnhancers")
    p1.manual("IndustrialMicrobes").expect("Plant")
  }

  @Test
  fun `with Viral Enhancers, reacts once to each bio tag on a card`() {
    newGame()
    p1.manual("ViralEnhancers")

    p1.manual("AdvancedEcosystems").expect("3 Plant")
  }

  @Test
  fun `with Viral Enhancers, adds a microbe card choosing a microbe`() {
    newGame()
    p1.manual("ViralEnhancers")
    p1.manual("NitriteReducingBacteria") { doTask("Microbe<NitriteReducingBacteria>") }
        .expect("4 Microbe")
  }

  @Test
  fun `with Viral Enhancers, tries to add a microbe to another card`() {
    initializeExistingMicrobeCard()
    p1.manual("RegolithEaters") {
      shouldThrow<NarrowingException> { doTask("Microbe<NitriteReducingBacteria>") }
      abort()
    }
  }

  @Test
  fun `with Viral Enhancers, adds a microbe to the entering card`() {
    initializeExistingMicrobeCard()
    p1.manual("RegolithEaters") { doTask("Microbe<RegolithEaters>") }
        .expect("Microbe<RegolithEaters>")
  }

  private fun initializeExistingMicrobeCard() {
    newGame()
    p1.manual("ViralEnhancers")
    p1.manual("NitriteReducingBacteria") { doTask("Plant") }
  }
}
