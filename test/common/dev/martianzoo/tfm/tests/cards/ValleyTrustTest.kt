package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ValleyTrustTest : CardTest() {
  @Test
  internal fun `Valley Trust requires a Prelude deck`() {
    shouldThrow<IllegalArgumentException> {
      newGame(GameConfig("ValleyTrust", "Player1", "Player2"))
    }
  }

  @Test
  internal fun `Resolves Valley Trust's starting prelude`() {
    newGame(PreludeExpansion)
    p1.playCorp(ValleyTrust, 5).expect("5 ProjectCard, 22 MC")

    engine.phase("Action")
    p1.stdAction("HandleMandates") { p1.playPrelude(MartianIndustries) }
        .expect("PROD[Steel, Energy]")
  }

  @Test
  internal fun `Valley Trust uses an already selected Prelude generation`() {
    val game =
        newGame(
            GameConfig(
                "ValleyTrust, Prelude2Deck, -Prelude1Deck",
                "Player1",
                "Player2",
            )
        )

    game.classTable.isActive(cn("PreludePhase")) shouldBe true
    game.classTable.isActive(cn("AppliedScience")) shouldBe true
    game.classTable.isActive(MartianIndustries) shouldBe false
  }

  @Test
  internal fun `Must resolve mandate before another standard action`() {
    newGame(PreludeExpansion)
    p1.playCorp(ValleyTrust, 5)
    engine.phase("Action")

    shouldThrow<RequirementException> { p1.stdAction("PowerPlantSP") }
  }
}
