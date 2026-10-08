package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class InsulationTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame()
    admin.phase("Action")
    p1.runOperation("2 MC, ProjectCard, PROD[-1 MC, 3 Heat]")
  }

  @Test
  internal fun `Can convert two of three heat production`() {
    p1.playProject(Insulation, 2) { doTask("PROD[2 MC FROM Heat]") }.expect("PROD[2 MC, -2 Heat]")
  }

  @Test
  internal fun `Cannot skip its production conversion`() {
    shouldThrow<NarrowingException> {
      p1.playProject(Insulation, 2) { doTask("Ok") }
    }
  }
}
