package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CelesticTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion)
    p1.playCorp(Celestic, 5)
    p1.manual("10 Heat")
    engine.phase("Action")
  }

  @Test
  internal fun `Can pass the first generation and draw two cards in the second`() {
    advanceToStartingCardDraw()
    p1.stdAction("DoRequiredActions").expect("2 ProjectCard")
  }

  @Test
  internal fun `Can play a project after resolving its mandatory card draw`() {
    advanceToStartingCardDraw()
    p1.stdAction("DoRequiredActions")
    p1.playProject(Mine, 4).expect("PROD[Steel]")
  }

  @Test
  internal fun `Cannot play a project before resolving its mandatory card draw`() {
    shouldThrow<RequirementException> { p1.playProject(Mine, 4) }
  }

  @Test
  internal fun `Cannot buy a standard project before resolving its mandatory card draw`() {
    shouldThrow<RequirementException> { p1.stdProject("AsteroidSP") }
  }

  @Test
  internal fun `Cannot use a standard action before resolving its mandatory card draw`() {
    shouldThrow<RequirementException> { p1.convertHeat() }
  }

  private fun advanceToStartingCardDraw() {
    p1.pass()
    engine.nextGeneration(2, 2)
  }
}
