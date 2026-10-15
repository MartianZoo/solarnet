package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class VenusShuttlesTest : TfmSandboxTest() {
  @Test
  internal fun `Counts Venus tags including its own to reduce the action cost`() {
    newTestGame("VenusShuttles")
    kim.exMachina(
        "$VenusGovernor, $VenusWaystation, $ForcedPrecipitation, $VenusMagnetizer, $VenusShuttles"
    )

    kim.inTurn {
      doTasks("UseCardAction<VenusShuttles, Action1>", "6 Pay<> FROM MC")
      doTask("VenusStep")
      doTask("TerraformRating")
    }
  }
}
