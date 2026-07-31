package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class CelesticTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame("VenusNextExpansion")
    p1.playCorp("Celestic", 5)
    p1.manual("10 Heat")
    engine.phase("Action")
  }

  @Test
  fun `with a mandate, handles it as Celestic`() {
    advanceToMandate()
    p1.assertCounts(1 to "Mandate", 7 to "ProjectCard")
    p1.stdAction("HandleMandates").expect("-Mandate, 2 ProjectCard")
  }

  @Test
  fun `after handling its mandate, plays a project as Celestic`() {
    advanceToMandate()
    p1.stdAction("HandleMandates")
    p1.playProject("Mine", 4).expect("PROD[Steel]")
  }

  @Test
  fun `before handling its mandate, tries to play a project as Celestic`() {
    shouldThrow<RequirementException> { p1.playProject("Mine", 4) }
  }

  @Test
  fun `before handling its mandate, tries a standard project as Celestic`() {
    shouldThrow<RequirementException> { p1.stdProject("AsteroidSP") }
  }

  @Test
  fun `before handling its mandate, tries a standard action as Celestic`() {
    shouldThrow<RequirementException> { p1.stdAction("ConvertHeatSA") }
  }

  private fun advanceToMandate() {
    p1.pass()
    engine.nextGeneration(2, 2)
  }
}
