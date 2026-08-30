package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CyberiaSystemsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PromoCardPack)
    p1.manual("$Mine, $IndustrialMicrobes")
  }

  @Test
  internal fun `Copies production boxes from two different building cards`() {
    p1.manual("$CyberiaSystems") {
          doTask("CopyProductionBox<$Mine>")
          doTask("CopyProductionBox<$IndustrialMicrobes>")
        }
        .expect("PROD[3 Steel, Energy]")
  }

  @Test
  internal fun `Cannot copy the same card twice`() {
    p1.manual("$CyberiaSystems") {
      doTask("CopyProductionBox<$Mine>")
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<$Mine>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy itself`() {
    p1.manual("$CyberiaSystems") {
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<$CyberiaSystems>") }
      abort()
    }
  }
}
