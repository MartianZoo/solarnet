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
      doTasks("PlayProject<Class<LunarExports>>", "-19 MC")
      selectTask("-X Titanium?")
      doTasks("Ok", "LunarExports FROM ProjectCard", "PROD[5 MC]")
    }
  }

  @Test
  internal fun `Triggers for a 20-cost card`() {
    kim.exMachina("$Advertising")

    kim.inTurn {
      doTasks("PlayProject<Class<GanymedeColony>>", "-20 MC")
      selectTask("-X Titanium?")
      doTasks("Ok", "GanymedeColony FROM ProjectCard")
      selectTask("CityTile<Kim, GanymedeColony_RemoteArea>!")
      doTask("PROD[MC]")
    }
  }
}
