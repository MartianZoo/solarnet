package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

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
  fun `Luna placement can enable Minority Refuge's production decrease`() {
    initializeCard()
    p1.playProject(MinorityRefuge, 5) {
          p1.autoExecMode = NONE
          doTask("Colony<Luna>")
          doTask("PROD[2 Megacredit]")
          doTask("PROD[-2 Megacredit]")
          p1.autoExecMode = FIRST
        }
        .expect("Colony<Luna>, PROD[0 Megacredit]")
  }

  private fun initializeCard() {
    p1.manual("ProjectCard, 5, PROD[-5]")
  }
}
