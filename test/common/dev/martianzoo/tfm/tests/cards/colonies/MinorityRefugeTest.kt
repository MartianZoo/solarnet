package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.cards.ProjectCardTest
import dev.martianzoo.tfm.tests.cards.cardnames.MinorityRefuge
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MinorityRefugeTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Cannot place its colony on Io at minimum MC production`() {
    kim.setToExMachina(0, "PROD[MC]")
    kim.exMachina("PROD[-5 MC]")

    shouldThrow<LimitsException> {
      kim.playProject(MinorityRefuge, 5) { doTask("Colony<Io>") }
    }
  }

  @Test
  internal fun `A Luna placement can enable its production decrease`() {
    kim.setToExMachina(0, "PROD[MC]")
    kim.exMachina("PROD[-5 MC]")

    kim.playProject(MinorityRefuge, 5) { doTask("Colony<Luna>") }.expect("Colony<Luna>, PROD[0 MC]")
  }
}
