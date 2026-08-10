package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon.Option.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class CardXC3Test : CardTest() {
  @Test
  fun `when Splice enters play, receives both benefits for its own microbe tag`() {
    newGame(PromoCardPack)

    p1.manual("SpliceTacticalGenomics") {
          doTask("Microbe<SpliceTacticalGenomics>!")
        }
        .expect("46 Megacredit, Mandate, MicrobeTag, Microbe<SpliceTacticalGenomics>")

    engine.phase("Action")
    p1.stdAction("HandleMandates").expect("-Mandate, ProjectCard")
  }

  @Test
  fun `when another player plays a microbe tag, pays both players`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("SpliceTacticalGenomics") { doTask("2 Megacredit") }

    p2.manual("Decomposers") { doTask("2 Megacredit") }.expect("4 Megacredit")
  }

  @Test
  fun `microbe-tag player makes a separate choice for each tag`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("SpliceTacticalGenomics") { doTask("2 Megacredit") }
    p2.manual("GhgProducingBacteria") { doTask("2 Megacredit") }.expect("4 Megacredit")
    p2.manual("-MicrobeTag<GhgProducingBacteria>").expect("-MicrobeTag")

    p2.manual("2 MicrobeTag<GhgProducingBacteria>") {
          doTask("2 Megacredit")
          doTask("Microbe<GhgProducingBacteria>!")
        }
        .expect("2 MicrobeTag, 6 Megacredit, Microbe<GhgProducingBacteria>")
  }

  @Test
  fun `abstract removal auto-narrows one concrete type regardless of its multiplicity`() {
    newGame(PromoCardPack)
    p1.manual("GhgProducingBacteria, 2 Microbe<GhgProducingBacteria>")

    p1.manual("-Microbe").expect("-Microbe<GhgProducingBacteria>")
  }

  @Test
  fun `microbe-tag player can take a microbe instead of money`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("SpliceTacticalGenomics") { doTask("2 Megacredit") }

    p2.manual("Decomposers") { doTask("Microbe<Decomposers>!") }
        .expect("2 Megacredit, 2 Microbe<Decomposers>")
  }

  @Test
  fun `cannot add the microbe to a different card`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("SpliceTacticalGenomics") { doTask("2 Megacredit") }
    p2.manual("RegolithEaters") { doTask("2 Megacredit") }.expect("4 Megacredit")

    p2.manual("Decomposers") {
          shouldThrow<NarrowingException> { doTask("Microbe<RegolithEaters>!") }
          doTask("Microbe<Decomposers>!")
        }
        .expect("2 Megacredit, 2 Microbe<Decomposers>")
  }
}
