package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SpliceTacticalGenomicsTest : CardTest() {
  @Test
  internal fun `Splice pays itself because it is not a microbe card`() {
    newGame(PromoCardPack)

    p1.playCorp(SpliceTacticalGenomics, 0).expect("48 MC")

    engine.phase("Action")
    p1.stdAction("HandleMandates").expect("ProjectCard")
  }

  @Test
  internal fun `When another player plays a microbe tag, Splice pays both players`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics")
    val manual = p2.godMode().also { it.autoExecMode = NONE }
    val p1MoneyBefore = p1.count("MC")
    val p2MoneyBefore = p2.count("MC")

    manual.manual("$Decomposers") {
      shouldThrow<TaskException> { p1.doTask("2 MC") }
      doTask("2 MC<Player1>")
      doTask("2 MC")
      doTask("Microbe<$Decomposers>")
    }

    p1.count("MC") shouldBe p1MoneyBefore + 2
    p2.count("MC") shouldBe p2MoneyBefore + 2
  }

  @Test
  internal fun `Pharmacy Union triggers Splice twice`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics")
    val before = p1.count("MC")

    p2.manual("$PharmacyUnion")

    p1.count("MC") shouldBe before + 4
  }

  @Test
  internal fun `Can take a microbe instead of mc`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics")

    p2.manual("$Decomposers") { addCardResources(Decomposers) }
        .expect("2 MC<Player1>, 2 Microbe<$Decomposers>")
  }

  @Test
  internal fun `Must add the microbe to the card just played`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p1.manual("$SpliceTacticalGenomics")
    p2.manual("$RegolithEaters") { doTask("2 MC") }.expect("2 MC<Player1>, 2 MC")

    p2.manual("$Decomposers") {
          shouldThrow<NarrowingException> { doTask("Microbe<$RegolithEaters>!") }
          addCardResources(Decomposers)
        }
        .expect("2 MC<Player1>, 2 Microbe<$Decomposers>")
  }
}
