package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AdvertisingTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Does not trigger for a 19-cost card`() {
    kim.exMachina("$Advertising")

    kim.inTurn {
      doTasks("PlayProject<Class<LunarExports>>", "19 Pay<> FROM MC")
      doTasks("LunarExports FROM ProjectCard", "PROD[5 MC]")
    }
  }

  @Test
  internal fun `Triggers for a 20-cost card`() {
    kim.exMachina("$Advertising")

    kim.inTurn {
      doTasks("PlayProject<Class<GanymedeColony>>", "20 Pay<> FROM MC")
      doTasks("GanymedeColony FROM ProjectCard")
      doTask("CityTile<Kim, GanymedeColony_RemoteArea>")
      doTask("PROD[MC]")
    }
  }
}
