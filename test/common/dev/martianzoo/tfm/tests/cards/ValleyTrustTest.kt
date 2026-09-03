package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ValleyTrustTest : CardTest() {
  @Test
  internal fun `Resolves Valley Trust's starting Prelude 1 card`() {
    newGame(PreludeExpansion)
    p1.playCorp(ValleyTrust, 5).expect("5 ProjectCard, 22 MC")

    engine.phase("Action")
    p1.stdAction("HandleMandates") { p1.playPrelude(MartianIndustries) }
        .expect("PROD[Steel, Energy]")
  }

  @Test
  internal fun `card packs control Valley Trust's draw`() {
    resolveValleyTrustPrelude(
        "PreludeExpansion, Prelude1CardPack",
        selectedPrelude = MartianIndustries,
        otherPrelude = AppliedScience,
        otherPreludeIsAvailable = false,
    )
    resolveValleyTrustPrelude(
        "PreludeExpansion, Prelude2CardPack, -Prelude1CardPack",
        selectedPrelude = AppliedScience,
        otherPrelude = MartianIndustries,
        otherPreludeIsAvailable = false,
    )
    resolveValleyTrustPrelude(
        "PreludeExpansion, Prelude1CardPack, Prelude2CardPack",
        selectedPrelude = AppliedScience,
        otherPrelude = MartianIndustries,
        otherPreludeIsAvailable = true,
    )
  }

  @Test
  internal fun `Prelude 2 expansion and card pack add the same cards to Prelude 1 rules`() {
    resolveValleyTrustPrelude(
        "PreludeExpansion, Prelude2Expansion",
        selectedPrelude = AppliedScience,
        otherPrelude = MartianIndustries,
        otherPreludeIsAvailable = true,
    )
    resolveValleyTrustPrelude(
        "PreludeExpansion, Prelude2CardPack",
        selectedPrelude = AppliedScience,
        otherPrelude = MartianIndustries,
        otherPreludeIsAvailable = true,
    )
  }

  @Test
  internal fun `Must resolve mandate before another standard action`() {
    newGame(PreludeExpansion)
    p1.playCorp(ValleyTrust, 5)
    engine.phase("Action")

    shouldThrow<RequirementException> { p1.stdAction("PowerPlantSP") }
  }

  private fun resolveValleyTrustPrelude(
      preludeConfiguration: String,
      selectedPrelude: ClassName,
      otherPrelude: ClassName,
      otherPreludeIsAvailable: Boolean,
  ) {
    val game =
        newGame(
            GameConfig(
                "ValleyTrust, $preludeConfiguration",
                "Player1",
                "Player2",
            )
        )
    game.classTable.isActive(cn("PreludePhase")) shouldBe true
    game.classTable.isActive(selectedPrelude) shouldBe true
    game.classTable.isActive(otherPrelude) shouldBe otherPreludeIsAvailable

    p1.playCorp(ValleyTrust, 5)
    engine.phase("Action")
    p1.stdAction("HandleMandates") { p1.playPrelude(selectedPrelude) }
  }
}
