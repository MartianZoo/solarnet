package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.canon.Canon.Option.*
import kotlin.test.Test

class ValleyTrustTest : CardTest() {
  @Test
  fun `resolves Valley Trust's starting prelude`() {
    newGame(PreludeExpansion)
    p1.playCorp("ValleyTrust", 5).expect("5 ProjectCard, 22")

    engine.phase("Action")
    p1.stdAction("HandleMandates") {
          p1.playPrelude("MartianIndustries")
        }
        .expect("PROD[Steel, Energy]")
  }
}
