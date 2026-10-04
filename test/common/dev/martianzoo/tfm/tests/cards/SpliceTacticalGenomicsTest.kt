package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SpliceTacticalGenomicsTest : CardTest() {
  @Test
  internal fun `Splicer depends on Splice`() {
    newGame(PromoCardPack)
    addSpliceChoosingMoney()
    p1.count("Splicer<$SpliceTacticalGenomics>") shouldBe 1

    p1.runOperation("-$SpliceTacticalGenomics")

    p1.count("Splicer") shouldBe 0
  }

  @Test
  internal fun `Splice can choose money when it cannot hold microbes`() {
    newGame(PromoCardPack)

    p1.inTurn {
          doTask("PlayCard<Class<StandardCorporationCard>, Class<$SpliceTacticalGenomics>, Hand>")
          doTask("2 MC")
          p1.buyCards(0)
        }
        .expect("48 MC")

    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction").expect("ProjectCard")
  }

  @Test
  internal fun `When another player plays a microbe tag, Splice pays both players`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    addSpliceChoosingMoney()
    val manual = p2.also { it.autoExecPolicy = NONE }
    val p1MoneyBefore = p1.count("MC")
    val p2MoneyBefore = p2.count("MC")

    manual.runOperation("$Decomposers") {
      shouldThrow<TaskException> { p1.doTask("2 MC") }
      doTask("2 MC<Player1>")
      doTask("2 MC")
      doTask("Microbe")
    }

    p1.count("MC") shouldBe p1MoneyBefore + 2
    p2.count("MC") shouldBe p2MoneyBefore + 2
  }

  @Test
  internal fun `Pharmacy Union triggers Splice twice`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    addSpliceChoosingMoney()
    val before = p1.count("MC")

    p2.runOperation("$PharmacyUnion") { repeat(2) { doTask("2 MC") } }

    p1.count("MC") shouldBe before + 4
  }

  @Test
  internal fun `Can take a microbe instead of mc`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    addSpliceChoosingMoney()

    p2.runOperation("$Decomposers") { addCardResources(Decomposers) }
        .expect("2 MC<Player1>, 2 Microbe<$Decomposers>")
  }

  @Test
  internal fun `Must add the microbe to the card just played`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    addSpliceChoosingMoney()
    p2.runOperation("$RegolithEaters") { doTask("2 MC") }.expect("2 MC<Player1>, 2 MC")

    p2.runOperation("$Decomposers") {
          shouldThrow<NarrowingException> { doTask("Microbe<$RegolithEaters>!") }
          addCardResources(Decomposers)
        }
        .expect("2 MC<Player1>, 2 Microbe<$Decomposers>")
  }

  @Test
  internal fun `Can choose a microbe for a card that cannot hold it and gain nothing`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    addSpliceChoosingMoney()

    p2.runOperation("$IndustrialMicrobes") { declineTask() }
        .expect("2 MC<Player1>, PROD[Energy, Steel], 0 Microbe, 0 MC")
  }

  private fun addSpliceChoosingMoney() {
    p1.runOperation("$SpliceTacticalGenomics") { doTask("2 MC") }
  }
}
