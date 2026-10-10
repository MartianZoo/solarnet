package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class HugeAsteroidTest : TfmSandboxTest() {
  // https://www.reddit.com/r/TerraformingMarsGame/comments/1kgksgg
  @Test
  internal fun `Remains playable at maximum temperature but still costs five MC`() {
    newTestGame("PreludeExpansion")
    kim.setToExMachina(19, "TemperatureStep")

    kim.playPrelude(HugeAsteroid)
        .expect("$HugeAsteroid, -5 MC, 0 TemperatureStep, 0 TerraformRating")
  }
}
