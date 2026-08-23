package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SupercapacitorsTest : CardTest() {
  @Test
  internal fun `Can preserve some energy`() {
    newGame(PromoCardPack)
    p1.manual("PROD[3 Energy, 5 Heat], 3 Energy, 9 Heat, Supercapacitors")

    engine.phase("Production") { p1.doTask("Energy FROM Heat!") }

    p1.assertCounts(4 to "Energy", 16 to "Heat")
  }

  @Test
  internal fun `Can preserve no energy`() {
    newGame(PromoCardPack)
    p1.manual("PROD[3 Energy, 5 Heat], 3 Energy, 9 Heat, Supercapacitors")

    engine.phase("Production") {
      // Decline converting energy into heat.
      p1.declineTask()
    }

    p1.assertCounts(3 to "Energy", 17 to "Heat")
  }

  @Test
  internal fun `Can preserve all existing energy but not newly produced energy`() {
    newGame(PromoCardPack)
    p1.manual("PROD[3 Energy, 5 Heat], 3 Energy, 9 Heat, Supercapacitors")

    engine.phase("Production") {
      shouldThrow<NarrowingException> { p1.doTask("4 Energy FROM Heat!") }
      p1.doTask("3 Energy FROM Heat!")
    }

    p1.assertCounts(6 to "Energy", 14 to "Heat")
  }
}
