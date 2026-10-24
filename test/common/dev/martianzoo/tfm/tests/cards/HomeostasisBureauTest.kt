package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.HomeostasisBureau
import dev.martianzoo.tfm.tests.standardProjectTasks
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class HomeostasisBureauTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Does not pay when an opponent raises temperature`() {
    kim.exMachina("$HomeostasisBureau")

    stan
        .inTurn {
          doTasks(*standardProjectTasks("AsteroidProject"), "14 Pay<> FROM MC")
          doTask("TemperatureStep")
          doTask("TerraformRating")
        }
        .expect("0 MC<Kim>")
  }
}
