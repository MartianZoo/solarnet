package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class VitorTest : CardTest() {
  @Test
  fun `in solo mode, plays Vitor`() {
    newGame("PreludeExpansion,SoloMode", players = 1)
    p1.playCorp("Vitor", 5).expect("5 ProjectCard, 33")
    p1.assertCounts(0 to "Mandate")
  }

  @Test
  fun `with Vitor in solo mode, adds a card with a positive VP`() {
    initializeVitor()
    p1.manual("SearchForLife").expect("3")
  }

  @Test
  fun `with Vitor in solo mode, adds a card without VP`() {
    initializeVitor()
    p1.count("Megacredit") shouldBe 48
    p1.manual("Mine")
    p1.count("Megacredit") shouldBe 48
  }

  @Test
  fun `with Vitor in solo mode, adds a card with negative VP`() {
    initializeVitor()
    p1.count("Megacredit") shouldBe 48
    p1.manual("BribedCommittee")
    p1.count("Megacredit") shouldBe 48
  }

  private fun initializeVitor() {
    newGame("PreludeExpansion,SoloMode", players = 1)
    p1.manual("Vitor")
  }
}
