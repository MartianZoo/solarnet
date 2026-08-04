package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon.Option.PromoCardPack
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

class CyberiaSystemsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PromoCardPack)
    p1.manual("Mine, IndustrialMicrobes")
  }

  @Test
  fun `copies production boxes from two different building cards`() {
    p1.manual("CyberiaSystems") {
          doTask("CopyProductionBox<Mine>")
          doTask("CopyProductionBox<IndustrialMicrobes>")
        }
        .expect("PROD[3 Steel, Energy]")
    p1.count("CyberiaFirstChoice") shouldBe 0
  }

  @Test
  fun `tries to copy the same card twice`() {
    p1.manual("CyberiaSystems") {
      doTask("CopyProductionBox<Mine>")
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<Mine>") }
      abort()
    }
  }

  @Test
  fun `tries to copy itself`() {
    p1.manual("CyberiaSystems") {
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<CyberiaSystems>") }
      abort()
    }
  }
}
