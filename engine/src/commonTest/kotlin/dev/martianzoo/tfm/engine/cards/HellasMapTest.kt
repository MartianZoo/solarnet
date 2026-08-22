package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestOption.HellasMapOption
import dev.martianzoo.tfm.engine.TestOption.TurmoilCardPack
import dev.martianzoo.tfm.engine.cardnames.LakefrontResorts
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class HellasMapTest : CardTest() {
  @Test
  fun `An unaffordable south pole remains a structurally available adjacent greenery area`() {
    newGame(HellasMapOption)
    val p2 = requireP2()
    engine.phase("Action")
    p1.manual("GreeneryTile<Hellas_9_6>")
    p2.manual("GreeneryTile<Hellas_8_6>, GreeneryTile<Hellas_8_5>, GreeneryTile<Hellas_9_5>")
    p1.manual("8 Plant")

    p1.stdAction("ConvertPlantsSA") {
      shouldThrow<NarrowingException> { doTask("GreeneryTile<Hellas_1_5>") }
      abort()
    }
  }

  @Test
  fun `Ocean income from the south pole bonus can fund its payment`() {
    newGame(HellasMapOption, TurmoilCardPack)
    engine.phase("Action")
    p1.manual("$LakefrontResorts")
    p1.manual("OceanTile<Hellas_4_7>, OceanTile<Hellas_5_6>")
    p1.manual("-54 Megacredit")

    p1.manual("GreeneryTile<Hellas_9_7>") {
      placeTile(5, 7)
    }

    p1.count("Megacredit") shouldBe 0
  }
}
