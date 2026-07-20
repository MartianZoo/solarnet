package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class SaturnSurfingTest : CardTest() {
  @Test
  fun `the action pays at most four for its remaining floaters plus one until empty`() {
    val game = newGame(Canon.fromOptionCodes("BMRX", 2))
    with(game.tfm(PLAYER1)) {
      phase("Action")
      godMode()
          .manual("Teractor, EarthOffice, AcquiredCompany, MediaGroup, Cartel, SaturnSurfing")
          .expect("6 Floater")

      useActionExpecting(payout = 5)
      useActionExpecting(payout = 5)
      useActionExpecting(payout = 4)
      useActionExpecting(payout = 3)
      useActionExpecting(payout = 2)
      useActionExpecting(payout = 1, finalUse = true)
    }
  }

  private fun TfmGameplay.useActionExpecting(payout: Int, finalUse: Boolean = false) {
    cardAction1("SaturnSurfing").expect("-Floater, $payout Megacredit")
    if (!finalUse) manual("-ActionUsedMarker<SaturnSurfing>")
  }
}
