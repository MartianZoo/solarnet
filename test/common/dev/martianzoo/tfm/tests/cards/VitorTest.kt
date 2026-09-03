package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VitorTest : CardTest() {
  @Test
  internal fun `Funds an award for free in multiplayer`() {
    val game = newGame(PreludeExpansion, players = 2)
    val p1 = game.tfm(PLAYER1)

    p1.playCorp(Vitor, 5).expect("5 ProjectCard, 33 MC")
    p1.phase("Action")
    p1.assertCounts(0 to "Award", 33 to "MC")

    p1.stdAction("DoRequiredActions") { doTask("Landlord") }
    p1.assertCounts(1 to "Landlord", 33 to "MC")
  }

  @Test
  internal fun `In solo mode, plays Vitor without award funding`() {
    newGame(PreludeExpansion, players = 1)
    p1.playCorp(Vitor, 5).expect("5 ProjectCard, 33 MC")
    p1.assertCounts(0 to "Award")
  }

  @Test
  internal fun `Rebates a card with positive victory points`() {
    initializeVitor()
    p1.manual("$SearchForLife").expect("3 MC")
  }

  @Test
  internal fun `Does not rebate a card without victory points`() {
    initializeVitor()
    p1.count("MC") shouldBe 48
    p1.manual("$Mine")
    p1.count("MC") shouldBe 48
  }

  @Test
  internal fun `Does not rebate a card with negative victory points`() {
    initializeVitor()
    p1.count("MC") shouldBe 48
    p1.manual("$BribedCommittee")
    p1.count("MC") shouldBe 48
  }

  private fun initializeVitor() {
    newGame(PreludeExpansion, players = 1)
    p1.manual("$Vitor")
  }
}
