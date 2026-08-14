package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestOption.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class CelesticTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(VenusNextExpansion)
    p1.playCorp("Celestic", 5)
    p1.manual("10 Heat")
    engine.phase("Action")
  }

  @Test
  fun `after a generation, Celestic draws two extra project cards`() {
    advanceToStartingCardDraw()
    p1.stdAction("HandleMandates").expect("2 ProjectCard")
  }

  @Test
  fun `after resolving its card draw, plays a project as Celestic`() {
    advanceToStartingCardDraw()
    p1.stdAction("HandleMandates")
    p1.playProject("Mine", 4).expect("PROD[Steel]")
  }

  @Test
  fun `before resolving its card draw, tries to play a project as Celestic`() {
    shouldThrow<RequirementException> { p1.playProject("Mine", 4) }
  }

  @Test
  fun `before resolving its card draw, tries a standard project as Celestic`() {
    shouldThrow<RequirementException> { p1.stdProject("AsteroidSP") }
  }

  @Test
  fun `before resolving its card draw, tries a standard action as Celestic`() {
    shouldThrow<RequirementException> { p1.stdAction("ConvertHeatSA") }
  }

  private fun advanceToStartingCardDraw() {
    p1.pass()
    engine.nextGeneration(2, 2)
  }
}
