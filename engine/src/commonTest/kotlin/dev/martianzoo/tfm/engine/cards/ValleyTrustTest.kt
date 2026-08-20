package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class ValleyTrustTest : CardTest() {
  @Test
  fun `Resolves Valley Trust's starting prelude`() {
    newGame(PreludeExpansion)
    p1.playCorp(ValleyTrust, 5).expect("5 ProjectCard, 22")

    engine.phase("Action")
    p1.stdAction("HandleMandates") {
          p1.playPrelude(MartianIndustries)
        }
        .expect("PROD[Steel, Energy]")
  }

  @Test
  fun `Must resolve mandate before another standard action`() {
    newGame(PreludeExpansion)
    p1.playCorp(ValleyTrust, 5)
    engine.phase("Action")

    shouldThrow<AbstractException> { p1.stdAction("UseStandardProjectSA") }
  }
}
