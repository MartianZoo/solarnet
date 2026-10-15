package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.HomeostasisBureau
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class HomeostasisBureauTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Pays when its owner raises temperature`() {
    kim.exMachina("$HomeostasisBureau")

    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
      doTask("TemperatureStep")
      doTasks("TerraformRating", "3 MC")
    }
  }

  @Test
  internal fun `Does not pay when an opponent raises temperature`() {
    kim.exMachina("$HomeostasisBureau")

    stan
        .inTurn {
          doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
          doTask("TemperatureStep")
          doTask("TerraformRating")
        }
        .expect("0 MC<Kim>")
  }
}
