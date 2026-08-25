package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SpliceTacticalGenomicsTest : CardTest() {
  @Test
  internal fun `Splice pays itself because it is not a microbe card`() {
    newGame(PromoCardPack)

    p1.playCorp(SpliceTacticalGenomics, 0).expect("48")

    engine.phase("Action")
    p1.stdAction("HandleMandates").expect("ProjectCard")
  }

  @Test
  internal fun `When another player plays a microbe tag, Splice pays both players`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics")

    p2.manual("$Decomposers") { doTask("2 Megacredit") }
        .expect("2 Megacredit<Player1>, 2 Megacredit")
  }

  @Test
  internal fun `Pharmacy Union triggers Splice twice`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics")
    val before = p1.count("Megacredit")

    p2.manual("$PharmacyUnion")

    p1.count("Megacredit") shouldBe before + 4
  }

  @Test
  internal fun `Can take a microbe instead of megacredits`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics")

    p2.manual("$Decomposers") { addCardResources(Decomposers) }
        .expect("2 Megacredit<Player1>, 2 Microbe<$Decomposers>")
  }

  @Test
  internal fun `Must add the microbe to the card just played`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics")
    p2.manual("$RegolithEaters") { doTask("2 Megacredit") }
        .expect("2 Megacredit<Player1>, 2 Megacredit")

    p2.manual("$Decomposers") {
          shouldThrow<NarrowingException> { doTask("Microbe<$RegolithEaters>!") }
          addCardResources(Decomposers)
        }
        .expect("2 Megacredit<Player1>, 2 Microbe<$Decomposers>")
  }
}
