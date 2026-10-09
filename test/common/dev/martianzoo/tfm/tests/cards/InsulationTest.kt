package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class InsulationTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can convert two of three heat production`() {
    kim.setToExMachina(2, "MC")
    kim.exMachina("PROD[-MC, 2 Heat]")

    kim.playProject(Insulation, 2) { doTask("PROD[2 MC FROM Heat]") }.expect("PROD[2 MC, -2 Heat]")
  }

  @Test
  internal fun `Cannot skip its production conversion`() {
    kim.setToExMachina(2, "MC")
    kim.exMachina("PROD[-MC, 2 Heat]")

    shouldThrow<NarrowingException> {
      kim.playProject(Insulation, 2) { doTask("Ok") }
    }
  }
}
