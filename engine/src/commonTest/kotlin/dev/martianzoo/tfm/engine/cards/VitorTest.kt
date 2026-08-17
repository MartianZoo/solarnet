package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class VitorTest : CardTest() {
  @Test
  fun `funds an award for free in multiplayer`() {
    val game = newGame(PreludeExpansion, players = 2)
    val p1 = game.tfm(PLAYER1)

    p1.playCorp(Vitor, 5) { doTask("MandatePC5") }.expect("5 ProjectCard, 33")
    p1.phase("Action")
    p1.assertCounts(0 to "Award", 33 to "Megacredit")

    p1.stdAction("HandleMandates") { doTask("Landlord") }
    p1.assertCounts(1 to "Landlord", 33 to "Megacredit")
  }

  @Test
  fun `in solo mode, plays Vitor without award funding`() {
    newGame(PreludeExpansion, players = 1)
    p1.playCorp(Vitor, 5).expect("5 ProjectCard, 33")
    p1.assertCounts(0 to "Award")
  }

  @Test
  fun `with Vitor in solo mode, adds a card with a positive VP`() {
    initializeVitor()
    p1.manual("$SearchForLife").expect("3")
  }

  @Test
  fun `with Vitor in solo mode, adds a card without VP`() {
    initializeVitor()
    p1.count("Megacredit") shouldBe 48
    p1.manual("$Mine")
    p1.count("Megacredit") shouldBe 48
  }

  @Test
  fun `with Vitor in solo mode, adds a card with negative VP`() {
    initializeVitor()
    p1.count("Megacredit") shouldBe 48
    p1.manual("$BribedCommittee")
    p1.count("Megacredit") shouldBe 48
  }

  private fun initializeVitor() {
    newGame(PreludeExpansion, players = 1)
    p1.manual("$Vitor")
  }
}
