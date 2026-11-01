package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DirigiblesTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can spend floaters before paying the remaining MC`() {
    kim.exMachina("$Dirigibles, 2 Floater<$Dirigibles>")
    kim.setToExMachina(5, "MC")

    kim.inTurn {
      doTasks(
          *playCardTasks("AerialMappers"),
          "2 PayFromCard<Dirigibles> FROM Floater<Dirigibles>",
          "5 Pay<> FROM MC",
          "AerialMappers FROM ProjectCard",
      )
    }
  }

  @Test
  internal fun `A card with two Venus tags accepts floaters only once`() {
    kim.exMachina("$Dirigibles, $AerialMappers, Floater<$Dirigibles>")
    kim.setToExMachina(1, "MC")

    kim.inTurn {
      doTasks(
          *playCardTasks("VenusGovernor"),
          "PayFromCard<Dirigibles> FROM Floater<Dirigibles>",
          "Pay<> FROM MC",
          "VenusGovernor FROM ProjectCard",
          "PROD[2 MC]",
      )
    }
    kim.assertCounts(1 to "$VenusGovernor", 0 to "Floater<$Dirigibles>", 0 to "MC")
  }

  @Test
  internal fun `A card's required floater is additional to the floaters spent paying for it`() {
    kim.exMachina("$Dirigibles, 4 Floater<$Dirigibles>")
    kim.setToExMachina(3, "MC")
    kim.setToExMachina(6, "VenusStep")

    kim.inTurn {
          doTasks(
              *playCardTasks("StratosphericBirds"),
              "3 PayFromCard<Dirigibles> FROM Floater<Dirigibles>",
              "3 Pay<> FROM MC",
              "StratosphericBirds FROM ProjectCard",
          )
          doTask("-Floater")
        }
        .expect("-4 Floater<$Dirigibles>")
  }
}
