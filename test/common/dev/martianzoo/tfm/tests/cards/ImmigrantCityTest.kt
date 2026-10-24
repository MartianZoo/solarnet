package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ImmigrantCityTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(kimCorporation = Manutech)

  // At -4 M€ production, the reduction is unavailable until the city trigger raises production.
  @Test
  internal fun `Can be played at the production floor when Manutech offsets its loss`() {
    kim.setToExMachina(0, "PROD[MC]")
    kim.exMachina("PROD[-4 MC]")
    kim.setToExMachina(13, "MC")

    kim.inTurn {
      doTasks(*playCardTasks("ImmigrantCity"))
      doTask("Ok") // Keep the steel.
      doTask("13 Pay<> FROM MC")
      doTasks(
          "ImmigrantCity FROM ProjectCard",
          "PROD[-Energy]",
          "CityTile<Tharsis_7_4>",
          "PROD[MC]",
          "PROD[-2 MC]",
          "MC",
      )
    }
  }
}
