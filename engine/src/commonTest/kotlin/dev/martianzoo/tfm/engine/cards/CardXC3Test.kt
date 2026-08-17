package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CardXC3Test : CardTest() {
  @Test
  fun `when Splice enters play, receives both benefits for its own microbe tag`() {
    newGame(PromoCardPack)

    p1.playCorp(SpliceTacticalGenomics, 0) {
          doTask("Microbe<$SpliceTacticalGenomics>!")
        }
        .expect("46, Microbe<$SpliceTacticalGenomics>")

    engine.phase("Action")
    p1.stdAction("HandleMandates").expect("ProjectCard")
  }

  @Test
  fun `when another player plays a microbe tag, pays both players`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics") { doTask("2 Megacredit") }

    p2.manual("$Decomposers") { doTask("2 Megacredit") }
        .expect("2 Megacredit<Player1>, 2 Megacredit")
  }

  @Test
  fun `Pharmacy Union triggers Splice once for each of its two microbe tags`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics") { doTask("2 Megacredit") }
    val before = p1.count("Megacredit")

    p2.manual("$PharmacyUnion")

    p1.count("Megacredit") shouldBe before + 4
  }

  @Test
  fun `microbe-tag player can take a microbe instead of money`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics") { doTask("2 Megacredit") }

    p2.manual("$Decomposers") { doTask("Microbe<$Decomposers>!") }
        .expect("2 Megacredit<Player1>, 2 Microbe<$Decomposers>")
  }

  @Test
  fun `cannot add the microbe to a different card`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics") { doTask("2 Megacredit") }
    p2.manual("$RegolithEaters") { doTask("2 Megacredit") }
        .expect("2 Megacredit<Player1>, 2 Megacredit")

    p2.manual("$Decomposers") {
          shouldThrow<NarrowingException> { doTask("Microbe<$RegolithEaters>!") }
          doTask("Microbe<$Decomposers>!")
        }
        .expect("2 Megacredit<Player1>, 2 Microbe<$Decomposers>")
  }
}
