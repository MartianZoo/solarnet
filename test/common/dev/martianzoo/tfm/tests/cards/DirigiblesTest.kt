package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DirigiblesTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can spend floaters before paying the remaining money`() {
    kim.exMachina("$Dirigibles, 2 Floater<$Dirigibles>")
    kim.setToExMachina(5, "MC")

    kim.playProject(
            AerialMappers,
            payment = {
              doTask("-2 Floater<$Dirigibles>")
              doTask("-5 MC")
            },
        )
        .expect("$AerialMappers")
  }

  @Test
  internal fun `A card's required floater is additional to the floaters spent paying for it`() {
    kim.exMachina("$Dirigibles, 4 Floater<$Dirigibles>")
    kim.setToExMachina(3, "MC")
    kim.setToExMachina(6, "VenusStep")

    kim.playProject(
            StratosphericBirds,
            payment = {
              doTask("-3 Floater<$Dirigibles>")
              doTask("-3 MC")
            },
        )
        .expect("$StratosphericBirds")
    kim.count("Floater<$Dirigibles>") shouldBe 0
  }
}
