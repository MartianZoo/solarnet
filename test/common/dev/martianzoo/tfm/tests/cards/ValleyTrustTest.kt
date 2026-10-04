package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ValleyTrustTest : CardTest() {
  @Test
  internal fun `Resolves Valley Trust's starting Prelude 1 card`() {
    newGame(PreludeExpansion)
    p1.playCorp(ValleyTrust, 5).expect("22 MC")

    admin.phase("Action")
    val result =
        p1.stdAction("DoRequiredActionsAction") {
          p1.playPrelude(MartianIndustries, location = cn("Selecting"))
        }
    result.expect("PROD[Steel, Energy]")
    result.changes
        .filter { it.change.gaining?.type == p1.resolve("PreludeCard<Selecting>") }
        .sumOf { it.change.count } shouldBe 3
    p1.assertCounts(0 to "PreludeCard<Selecting>")
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
    newGame(PreludeExpansion)
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Action")

    shouldThrow<RequirementException> { p1.stdProject("PowerPlantProject") }
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
        )
    game.classTable.isInhabited(cn("PreludePhase")) shouldBe true
    game.classTable.isInhabited(selectedPrelude) shouldBe true
    game.classTable.isInhabited(otherPrelude) shouldBe otherPreludeIsAvailable

    p1.playCorp(ValleyTrust, 5)
    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") {
      p1.playPrelude(selectedPrelude, location = cn("Selecting"))
    }
  }

  @Test
  internal fun `Valley Trust fizzles an unaffordable Industrial Complex`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.playCorp(ValleyTrust, 8)
    admin.phase("Prelude")
    p1.playPrelude(PowerGeneration)
    p1.playPrelude(Biolab)
    admin.phase("Action")
    shouldThrow<LimitsException> {
      p1.stdAction("DoRequiredActionsAction") {
        p1.playPrelude(IndustrialComplex, location = cn("Selecting"))
      }
    }

    val checkpoint = game.timeline.checkpoint()
    p1.stdAction("DoRequiredActionsAction") { doTask("-PreludeCard<Selecting>") }.expect("15 MC")
    p1.assertCounts(
        28 to "MC",
        0 to "RequiredAction",
        0 to "$IndustrialComplex",
        0 to "PreludeCard",
    )
    p1.auditGainsSince(checkpoint) shouldBe 1
  }
}
