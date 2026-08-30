package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class MinorityRefugeTest : ColoniesCardTest() {
  @Test
  internal fun `Cannot place its colony on Io at minimum mc production`() {
    initializeCard()
    p1.playProject(MinorityRefuge, 5) {
      shouldThrow<LimitsException> { doTask("Colony<Io>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot place its colony on Triton at minimum mc production`() {
    initializeCard()
    p1.playProject(MinorityRefuge, 5) {
      shouldThrow<LimitsException> { doTask("Colony<Triton>") }
      abort()
    }
  }

  @Test
  internal fun `Luna placement can enable Minority Refuge's production decrease`() {
    initializeCard()
    p1.playProject(MinorityRefuge, 5) {
          p1.autoExecMode = NONE
          doTask("Colony<Luna>")
          doTask("PROD[2 MC]")
          doTask("PROD[-2 MC]")
          p1.autoExecMode = FIRST
        }
        .expect("Colony<Luna>, PROD[0 MC]")
  }

  private fun initializeCard() {
    p1.manual("ProjectCard, 5 MC, PROD[-5 MC]")
  }
}
