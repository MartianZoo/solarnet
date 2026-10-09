package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TharsisRepublicTest : TfmSandboxTest() {
  @Test
  internal fun `Original solo corporation gains production for the two opponent cities`() {
    newTestGame(playerCount = 1, kimCorporation = TharsisRepublic, startAtCorporation = true)

    kim.playCorp(TharsisRepublic).expect("PROD[2 MC]")
  }

  @Test
  internal fun `Does not gain the solo starting bonus when acquired through Merger`() {
    newTestGame(addOptions = "PreludeExpansion", playerCount = 1, kimCorporation = CrediCor)

    kim.playPrelude(Merger) { kim.playCorp(TharsisRepublic) }.expect("PROD[0 MC]")
  }
}
