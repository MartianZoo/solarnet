package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.CometForVenus
import dev.martianzoo.tfm.tests.cards.cardnames.Dirigibles
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CometForVenusTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can remove MC from another Venus card owner`() {
    kim.setToExMachina(11, "MC")
    stan.setToExMachina(4, "MC")
    stan.exMachina("$Dirigibles")

    kim.playProject(CometForVenus, 11) { doTask("-4 MC<Stan>") }.expect("-4 MC<Stan>")
  }
}
