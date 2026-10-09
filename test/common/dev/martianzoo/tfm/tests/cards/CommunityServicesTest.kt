package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CommunityServicesTest : TfmSandboxTest() {
  @Test
  internal fun `Does not count Ecology Experts or Decomposers as tagless`() {
    newTestGame(addOptions = "PreludeExpansion, EcologyExperts, Unsafe")
    startActionPhase()
    kim.exMachina("$EcologyExperts, $Decomposers")
    kim.setToExMachina(13, "MC")

    // The beginner corporation and Community Services itself are the two tagless cards.
    kim.playProject(CommunityServices, 13).expect("PROD[2 MC]")
  }
}
