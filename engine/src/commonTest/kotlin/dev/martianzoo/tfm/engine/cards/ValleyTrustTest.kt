package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import kotlin.test.Test

class ValleyTrustTest : CardTest() {
  @Test
  fun `with a mandate, uses Valley Trust`() {
    newGame("PreludeExpansion")
    p1.playCorp("ValleyTrust", 5).expect("5 ProjectCard, 22")

    engine.phase("Action")
    p1.assertCounts(1 to "Mandate", 0 to "PreludeCard")

    p1.stdAction("HandleMandates") {
          p1.assertCounts(1 to "PreludeCard")

          p1.playPrelude("MartianIndustries")
          p1.assertCounts(0 to "PreludeCard")
        }
        .expect("PROD[Steel, Energy]")
  }
}
