package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.Hellas
import dev.martianzoo.tfm.tests.TestOption.TurmoilCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.LakefrontResorts
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HellasMapTest : CardTest() {
  @Test
  internal fun `An unaffordable south pole remains a structurally available adjacent greenery area`() {
    newGame(Hellas)
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
  internal fun `Ocean income from the south pole bonus can fund its payment`() {
    newGame(Hellas, TurmoilCardPack)
    engine.phase("Action")
    p1.manual("$LakefrontResorts")
    p1.manual("OceanTile<Hellas_4_7>, OceanTile<Hellas_5_6>")
    p1.manual("-54 MC")

    p1.manual("GreeneryTile<Hellas_9_7>") { placeTile(5, 7) }

    p1.count("MC") shouldBe 0
  }
}
