package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ValleyTrustTest : CardTest() {
  @Test
  internal fun `Resolves Valley Trust's starting Prelude 1 card`() {
    newGame(PreludeExpansion, retainedStartingProjects = 5)
    p1.playCorp(ValleyTrust, 5).expect("22 MC")

    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(MartianIndustries) }
        .expect("PROD[Steel, Energy]")
  }

  @Test
  internal fun `card packs control Valley Trust's draw`() {
    resolveValleyTrustPrelude(
        "PreludeExpansion, Prelude1CardPack",
        selectedPrelude = MartianIndustries,
        otherPrelude = SpaceLanes,
        otherPreludeIsAvailable = false,
    )
    resolveValleyTrustPrelude(
        "PreludeExpansion, Prelude2CardPack, -Prelude1CardPack",
        selectedPrelude = SpaceLanes,
        otherPrelude = MartianIndustries,
        otherPreludeIsAvailable = false,
    )
    resolveValleyTrustPrelude(
        "PreludeExpansion, Prelude1CardPack, Prelude2CardPack",
        selectedPrelude = SpaceLanes,
        otherPrelude = MartianIndustries,
        otherPreludeIsAvailable = true,
    )
  }

  @Test
  internal fun `Must perform required action before another standard action`() {
    newGame(PreludeExpansion, retainedStartingProjects = 5)
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Action")

    shouldThrow<RequirementException> { p1.stdProject("PowerPlantProject") }
  }

  @Test
  internal fun `An unplayable selection leaves Valley Trust free to choose another Prelude`() {
    newGame(PreludeExpansion, Prelude2CardPack, retainedStartingProjects = 5)
    val p2 = requireP2()
    p2.runOperation("PROD[-5 MC]")
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Action")
    val moneyBefore = p1.count("MC")

    shouldThrowAny {
      p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(Recession) }
    }

    p1.count("RequiredAction") shouldBe 1
    p1.count("MC") shouldBe moneyBefore
    p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(DomeFarming) }
    p1.assertCounts(0 to "RequiredAction", 1 to "$DomeFarming")
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
            ),
            retainedStartingProjects = 5,
        )
    game.classTable.isInhabited(cn("PreludePhase")) shouldBe true
    game.classTable.isInhabited(selectedPrelude) shouldBe true
    game.classTable.isInhabited(otherPrelude) shouldBe otherPreludeIsAvailable

    p1.playCorp(ValleyTrust, 5)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { p1.playPrelude(selectedPrelude) }
  }
}
