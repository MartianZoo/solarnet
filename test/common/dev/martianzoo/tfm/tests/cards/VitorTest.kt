package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VitorTest : TfmSandboxTest() {
  @Test
  internal fun `Funds its starting award for free`() {
    newTestGame(kimCorporation = Vitor)

    kim.stdAction("RequiredActionsSignal") { doTask("Landlord") }.expect("Landlord, 0 MC")
  }

  @Test
  internal fun `In solo play its corporation turn creates no award-funding action`() {
    newTestGame(playerCount = 1, kimCorporation = Vitor, startAtCorporation = true)

    kim.playCorp(Vitor).expect("0 RequiredAction, 0 Award")
  }

  @Test
  internal fun `Rebates Search for Life before it has earned any victory points`() {
    newTestGame(kimCorporation = Vitor)
    kim.stdAction("RequiredActionsSignal") { doTask("Landlord") }

    kim.playProject(SearchForLife, 3).expect("0 MC")
  }

  @Test
  internal fun `Does not rebate a card without victory points`() {
    newTestGame(kimCorporation = Vitor)
    kim.stdAction("RequiredActionsSignal") { doTask("Landlord") }

    kim.playProject(Mine, 4).expect("-4 MC")
  }

  @Test
  internal fun `Does not rebate a card with negative victory points`() {
    newTestGame(kimCorporation = Vitor)
    kim.stdAction("RequiredActionsSignal") { doTask("Landlord") }

    kim.playProject(BribedCommittee, 7).expect("-7 MC")
  }

  // https://boardgamegeek.com/thread/2993276/article/41548927#41548927
  @Test
  internal fun `Vitor acquired through Merger funds a fourth award`() {
    newTestGame(addOptions = "PreludeExpansion, BoardOfDirectors")
    kim.playPrelude(BoardOfDirectors)
    startActionPhase()
    stan.fundAward(cn("Landlord"), 8)
    stan.fundAward(cn("Banker"), 14)
    stan.fundAward(cn("Scientist"), 20)
    kim.cardAction1(BoardOfDirectors) {
      doTask("-12 MC")
      kim.playPrelude(Merger) { kim.playCorp(Vitor) }
    }

    kim.stdAction("RequiredActionsSignal") { doTask("Thermalist") }.expect("Thermalist, 0 MC")
    admin.count("Award") shouldBe 4
    stan.setToExMachina(20, "MC")
    shouldThrow<DeadEndException> { stan.stdAction("FundAward<Class<Miner>>") }
  }
}
