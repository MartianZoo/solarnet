package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

// This isn't really a test specific to this card, just testing task reordering
class MinorityRefugeTest : ColoniesCardTest() {
  @Test
  fun `at minimum megacredit production, tries to place its colony on Io`() {
    initializeCard()
    p1.playProject(MinorityRefuge, 5) {
      shouldThrow<LimitsException> { doTask("Colony<Io>") }
      abort()
    }
  }

  @Test
  fun `at minimum megacredit production, tries to place its colony on Triton`() {
    initializeCard()
    p1.playProject(MinorityRefuge, 5) {
      shouldThrow<LimitsException> { doTask("Colony<Triton>") }
      abort()
    }
  }

  @Test
  fun `at minimum megacredit production, places its colony on Luna`() {
    initializeCard()
    p1.playProject(MinorityRefuge, 5) { doTask("Colony<Luna>") }.expect("Colony<Luna>")
  }

  private fun initializeCard() {
    p1.manual("ProjectCard, 5, PROD[-5]")
  }
}
