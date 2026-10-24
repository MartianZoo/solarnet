package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MartianLumberCorporationTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Plants can pay for a building card`() {
    kim.exMachina("$MartianLumberCorp")
    kim.setToExMachina(2, "Plant")

    kim.inTurn {
      doTasks(
          *playCardTasks("Mine"),
          "Pay<Class<Plant>> FROM Plant",
          "1 Pay<> FROM MC",
          "Mine FROM ProjectCard",
          "PROD[Steel]",
      )
    }
  }
}
