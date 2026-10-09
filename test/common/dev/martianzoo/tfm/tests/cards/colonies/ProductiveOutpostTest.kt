package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.cards.ProjectCardTest
import dev.martianzoo.tfm.tests.cards.cardnames.ProductiveOutpost
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ProductiveOutpostTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Pays no bonuses without colonies`() {
    kim.playProject(ProductiveOutpost, 0).expect("0 MC")
  }

  @Test
  internal fun `Pays each bonus for colonies the player owns`() {
    kim.exMachina("Colony<Luna>, Colony<Io>, Colony<Triton>")

    kim.playProject(ProductiveOutpost, 0).expect("2 MC, 2 Heat, Titanium")
  }

  @Test
  internal fun `Pays once per colony, not once per colony tile`() {
    kim.exMachina("2 Colony<Luna>")

    kim.playProject(ProductiveOutpost, 0).expect("4 MC")
  }
}
