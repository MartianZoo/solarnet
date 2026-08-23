package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.data.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ValleyTrustTest : CardTest() {
  @Test
  fun `Valley Trust requires a Prelude deck`() {
    shouldThrow<IllegalArgumentException> {
      newGame(GameConfig("ValleyTrust", "Player1", "Player2"))
    }
  }

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
  fun `Valley Trust uses an already selected Prelude generation`() {
    val game =
        newGame(
            GameConfig(
                "ValleyTrust, Prelude2Expansion, -Prelude1Deck",
                "Player1",
                "Player2",
            )
        )

    game.classTable.isActive(cn("PreludePhase")) shouldBe true
    game.classTable.isActive(cn("AppliedScience")) shouldBe true
    game.classTable.isActive(MartianIndustries) shouldBe false
  }

  @Test
  fun `Must resolve mandate before another standard action`() {
    newGame(PreludeExpansion)
    p1.playCorp(ValleyTrust, 5)
    engine.phase("Action")

    shouldThrow<AbstractException> { p1.stdAction("UseStandardProjectSA") }
  }
}
