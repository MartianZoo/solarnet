package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.cards.cardnames.StandardTechnology
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class StandardTechnologyTest : CardTest() {
  @BeforeTest
  internal fun initializeGame() {
    newGame()
    engine.phase("Action")
    p1.manual("$StandardTechnology")
  }

  @Test
  internal fun `Rebate cannot fund the triggering standard project`() {
    p1.manual("8 MC")

    shouldThrow<LimitsException> { p1.stdProject("PowerPlantSP") }

    p1.assertCounts(8 to "MC", 0 to "Owed", 0 to "Invoice")
    p1.assertProds(0 to "Energy")
  }

  @Test
  internal fun `Awards the rebate after paying for a standard project`() {
    p1.manual("11 MC")

    p1.stdProject("PowerPlantSP").expect("-8 MC, PROD[Energy]")
  }

  @Test
  internal fun `Does not award the rebate after selling patents`() {
    p1.manual("ProjectCard<Hand>")

    p1.sellPatents(1).expect("-ProjectCard, MC")
  }
}
