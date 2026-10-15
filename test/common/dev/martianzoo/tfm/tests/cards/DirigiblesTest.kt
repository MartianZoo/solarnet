package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
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
          "PlayProject<Class<AerialMappers>>",
          "2 PayFromCard<Dirigibles> FROM Floater<Dirigibles>",
          "5 Pay<> FROM MC",
          "AerialMappers FROM ProjectCard",
      )
    }
  }

  @Test
  internal fun `A card's required floater is additional to the floaters spent paying for it`() {
    kim.exMachina("$Dirigibles, 4 Floater<$Dirigibles>")
    kim.setToExMachina(3, "MC")
    kim.setToExMachina(6, "VenusStep")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<StratosphericBirds>>",
              "3 PayFromCard<Dirigibles> FROM Floater<Dirigibles>",
              "3 Pay<> FROM MC",
              "StratosphericBirds FROM ProjectCard",
          )
          doTask("-Floater")
        }
        .expect("-4 Floater<$Dirigibles>")
  }
}
