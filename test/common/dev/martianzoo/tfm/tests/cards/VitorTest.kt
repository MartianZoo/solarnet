package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VitorTest : CardTest() {
  @Test
  internal fun `Funds an award for free in multiplayer`() {
    val game = newGame(PreludeExpansion, players = 2)
    val p1 = game.testTfm(PLAYER1)

    p1.playCorp(Vitor, 5).expect("33 MC")
    p1.phase("Action")
    p1.assertCounts(0 to "Award", 33 to "MC")

    p1.stdAction("DoRequiredActionsAction") { doTask("Landlord") }
    p1.assertCounts(1 to "Landlord", 33 to "MC")
  }

  @Test
  internal fun `In solo mode, plays Vitor without award funding`() {
    newGame(PreludeExpansion, players = 1)
    p1.playCorp(Vitor, 5).expect("33 MC")
    p1.assertCounts(0 to "Award")
  }

  @Test
  internal fun `Rebates a card with positive victory points`() {
    initializeVitor()
    p1.runOperation("$SearchForLife").expect("3 MC")
  }

  @Test
  internal fun `Does not rebate a card without victory points`() {
    initializeVitor()
    p1.count("MC") shouldBe 48
    p1.runOperation("$Mine")
    p1.count("MC") shouldBe 48
  }

  @Test
  internal fun `Does not rebate a card with negative victory points`() {
    initializeVitor()
    p1.count("MC") shouldBe 48
    p1.runOperation("$BribedCommittee")
    p1.count("MC") shouldBe 48
  }

  private fun initializeVitor() {
    newGame(PreludeExpansion, players = 1)
    p1.runOperation("$Vitor")
  }

  // https://boardgamegeek.com/thread/2993276/article/41548927#41548927
  @Test
  internal fun `Vitor acquired through Merger funds a fourth award`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack)
    p1.playCorp(CrediCor, 0)
    val p2 = requireP2()
    p2.runOperation("100 MC")
    p1.runOperation("54 MC")
    admin.phase("Prelude")
    p1.playPrelude(BoardOfDirectors)
    admin.phase("Action")
    p2.fundAward(cn("Landlord"), 8)
    p2.fundAward(cn("Banker"), 14)
    p2.fundAward(cn("Scientist"), 20)
    p1.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      p1.playPrelude(Merger) { p1.playCorp(Vitor) }
    }

    p1.stdAction("DoRequiredActionsAction") { doTask("Thermalist") }.expect("Thermalist, 0 MC")
    admin.count("Award") shouldBe 4
    shouldThrow<RequirementException> { p2.stdAction("FundAwardAction", 3) { doTask("Miner") } }
  }
}
